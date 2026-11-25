package com.warcontry.auth;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

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
                if (!packet.challengeId.isBlank()) {
                    String expectedChallenge = WarContryAuthManager.getPending2FA(player.getUUID());
                    if (expectedChallenge == null || !expectedChallenge.equals(packet.challengeId)) {
                        player.sendSystemMessage(Component.literal("§cSession de vérification invalide. Recommence la connexion."));
                        return;
                    }

                    var result = WarContryAuthAPI.authenticate2FA(packet.challengeId, packet.code);
                    if (result.has("authenticated") && result.get("authenticated").getAsBoolean()) {
                        WarContryAuthManager.authenticate(player.getUUID());
                        player.sendSystemMessage(Component.literal("§aConnexion War Contry réussie !"));
                    } else {
                        player.sendSystemMessage(Component.literal("§c" + result.get("error").getAsString()));
                    }
                    return;
                }

                if (packet.username.isBlank() || packet.password.isBlank()) {
                    player.sendSystemMessage(Component.literal("§cRemplis ton pseudo et ton mot de passe."));
                    return;
                }

                var result = WarContryAuthAPI.authenticate(packet.username, packet.password);
                if (result.has("requires2FA") && result.get("requires2FA").getAsBoolean()) {
                    String id = result.get("challengeId").getAsString();
                    WarContryAuthManager.setPending2FA(player.getUUID(), id);
                    player.sendSystemMessage(Component.literal("§eUn code de vérification a été envoyé par e-mail. Saisis-le dans l'écran de connexion."));
                } else if (result.has("authenticated") && result.get("authenticated").getAsBoolean()) {
                    WarContryAuthManager.authenticate(player.getUUID());
                    player.sendSystemMessage(Component.literal("§aConnexion War Contry réussie !"));
                } else {
                    String error = result.has("error") ? result.get("error").getAsString() : "Connexion refusée.";
                    player.sendSystemMessage(Component.literal("§c" + error));
                }
            } catch (Exception exception) {
                player.sendSystemMessage(Component.literal("§cImpossible de contacter le service d'authentification."));
                System.err.println("[War Contry Auth] Échec authentification: " + exception.getMessage());
            }
        });
        context.setPacketHandled(true);
    }
}
