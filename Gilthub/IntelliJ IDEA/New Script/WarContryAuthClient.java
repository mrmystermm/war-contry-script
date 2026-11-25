package com.warcontry.auth;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class WarContryAuthClient {
    public static void showStatus(String message, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof WarContryAuthScreen authScreen) {
            authScreen.setStatus(message, color);
        } else if (minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.literal(message).withStyle(style -> style.withColor(color)),
                    false
            );
        }
    }
}
