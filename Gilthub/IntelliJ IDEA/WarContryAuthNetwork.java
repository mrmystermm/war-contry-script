package com.warcontry.auth;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.Optional;

public class WarContryAuthNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    new ResourceLocation("warcountryauth", "main"),
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals
            );

    private static boolean registered = false;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.registerMessage(
                0,
                WarContryAuthLoginPacket.class,
                WarContryAuthLoginPacket::encode,
                WarContryAuthLoginPacket::decode,
                WarContryAuthLoginPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
        );
    }
}
