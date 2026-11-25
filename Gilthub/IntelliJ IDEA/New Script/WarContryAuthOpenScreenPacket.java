package com.warcontry.auth;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WarContryAuthOpenScreenPacket {

    public WarContryAuthOpenScreenPacket() {
    }

    public static void encode(WarContryAuthOpenScreenPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
    }

    public static WarContryAuthOpenScreenPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
        return new WarContryAuthOpenScreenPacket();
    }

    public static void handle(WarContryAuthOpenScreenPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                Minecraft.getInstance().setScreen(new WarContryAuthScreen())
        ));
        context.get().setPacketHandled(true);
    }
}
