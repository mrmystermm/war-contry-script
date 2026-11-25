
package com.warcontry.auth;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WarContryAuth.MOD_ID)
public class WarContryAuthEvents {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        WarContryAuthManager.unauthenticate(player.getUUID());

        System.out.println(
                "[War Contry Auth] Connexion détectée : "
                        + player.getGameProfile().getName()
        );
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        WarContryAuthManager.unauthenticate(player.getUUID());

        System.out.println(
                "[War Contry Auth] Déconnexion : "
                        + player.getGameProfile().getName()
        );
    }
}
