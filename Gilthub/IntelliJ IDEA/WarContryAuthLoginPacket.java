package com.warcontry.auth;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class WarContryAuthLoginPacket {

    private final String username;
    private final String password;
    private final String challengeId;
    private final String code;

    public WarContryAuthLoginPacket(String username, String password, String challengeId, String code) {
        this.username = username == null ? "" : username;
        this.password = password == null ? "" : password;
        this.challengeId = challengeId == null ? "" : challengeId;
        this.code = code == null ? "" : code;
    }

    public static void encode(WarContryAuthLoginPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.username, 32);
        buffer.writeUtf(packet.password, 128);
        buffer.writeUtf(packet.challengeId, 80);
        buffer.writeUtf(packet.code, 16);
    }

    public static WarContryAuthLoginPacket decode(FriendlyByteBuf buffer) {
        return new WarContryAuthLoginPacket(
                buffer.readUtf(32),
                buffer.readUtf(128),
                buffer.readUtf(80),
                buffer.readUtf(16)
        );
    }

    public static void handle(WarContryAuthLoginPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || WarContryAuthManager.isAuthenticated(player.getUUID())) {
                return;
            }

            try {
                String pendingChallenge = WarContryAuthManager.getPending2FA(player.getUUID());

                if (pendingChallenge != null && !packet.code.isBlank()) {
                    String challengeToVerify = packet.challengeId.isBlank()
                            ? pendingChallenge : packet.challengeId;

                    if (!pendingChallenge.equals(challengeToVerify)) {
                        sendStatus(player, "Session de vérification invalide.", 0xFF5555);
                        return;
                    }

                    var result = WarContryAuthAPI.authenticate2FA(challengeToVerify, packet.code);
                    if (result.has("authenticated") && result.get("authenticated").getAsBoolean()) {
                        WarContryAuthManager.authenticate(player.getUUID());
                        sendStatus(player, "Connexion War Contry réussie !", 0x55FF55);
                    } else {
                        String error = result.has("error") ? result.get("error").getAsString() : "Code invalide.";
                        sendStatus(player, error, 0xFF5555);
                    }
                    return;
                }

                if (!packet.challengeId.isBlank()) {
                    sendStatus(player, "Aucune vérification en attente.", 0xFF5555);
                    return;
                }

                if (packet.username.isBlank() || packet.password.isBlank()) {
                    sendStatus(player, "Remplis ton pseudo et ton mot de passe.", 0xFF5555);
                    return;
                }

                var result = WarContryAuthAPI.authenticate(packet.username, packet.password);
                if (result.has("requires2FA") && result.get("requires2FA").getAsBoolean()) {
                    String id = result.get("challengeId").getAsString();
                    WarContryAuthManager.setPending2FA(player.getUUID(), id);
                    sendStatus(player, "Code de vérification envoyé par e-mail. Saisis-le ici.", 0xFFFF55);
                } else if (result.has("authenticated") && result.get("authenticated").getAsBoolean()) {
                    WarContryAuthManager.authenticate(player.getUUID());
                    sendStatus(player, "Connexion War Contry réussie !", 0x55FF55);
                } else {
                    String error = result.has("error") ? result.get("error").getAsString() : "Connexion refusée.";
                    sendStatus(player, error, 0xFF5555);
                }
            } catch (Exception exception) {
                sendStatus(player, "Impossible de contacter le service d'authentification.", 0xFF5555);
                System.err.println("[War Contry Auth] Échec authentification: " + exception.getMessage());
            }
        });
        context.setPacketHandled(true);
    }

    private static void sendStatus(ServerPlayer player, String message, int color) {
        WarContryAuthNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new WarContryAuthStatusPacket(message, color)
        );
    }
}
