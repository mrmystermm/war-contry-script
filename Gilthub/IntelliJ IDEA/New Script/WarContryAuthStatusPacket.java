package com.warcontry.auth;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;
import java.util.function.Supplier;

public class WarContryAuthStatusPacket {
    private final String message;
    private final int color;

    public WarContryAuthStatusPacket(String message, int color) {
        this.message = message == null ? "" : message;
        this.color = color;
    }

    public static void encode(WarContryAuthStatusPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.message, 256);
        buffer.writeInt(packet.color);
    }

    public static WarContryAuthStatusPacket decode(FriendlyByteBuf buffer) {
        return new WarContryAuthStatusPacket(buffer.readUtf(256), buffer.readInt());
    }

    public static void handle(WarContryAuthStatusPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> WarContryAuthClient.showStatus(packet.message, packet.color)));
        context.setPacketHandled(true);
    }
}
