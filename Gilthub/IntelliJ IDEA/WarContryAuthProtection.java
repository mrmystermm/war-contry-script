package com.warcontry.auth;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WarContryAuth.MOD_ID)
public class WarContryAuthProtection {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (WarContryAuthManager.isAuthenticated(player.getUUID())) {
            return;
        }

        player.setDeltaMovement(0, 0, 0);
        player.hurtMarked = true;
    }
    @SubscribeEvent
    public static void onBlockBreak(
            net.minecraftforge.event.level.BlockEvent.BreakEvent event
    ) {
        if (event.getPlayer() instanceof ServerPlayer player
                && !WarContryAuthManager.isAuthenticated(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(
            net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player
                && !WarContryAuthManager.isAuthenticated(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(
            net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event
    ) {
        if (!WarContryAuthManager.isAuthenticated(
                event.getEntity().getUUID()
        )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockInteract(
            net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event
    ) {
        if (!WarContryAuthManager.isAuthenticated(
                event.getEntity().getUUID()
        )) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void onAttackEntity(
            net.minecraftforge.event.entity.player.AttackEntityEvent event
    ) {
        if (!WarContryAuthManager.isAuthenticated(
                event.getEntity().getUUID()
        )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemUse(
            net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event
    ) {
        if (!WarContryAuthManager.isAuthenticated(
                event.getEntity().getUUID()
        )) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void onPlayerChat(
            net.minecraftforge.event.ServerChatEvent event
    ) {
        if (!WarContryAuthManager.isAuthenticated(
                event.getPlayer().getUUID()
        )) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void onCommand(
            net.minecraftforge.event.CommandEvent event
    ) {
        if (event.getParseResults().getContext().getSource().getEntity()
                instanceof ServerPlayer player
                && !WarContryAuthManager.isAuthenticated(player.getUUID())) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void onItemPickup(
            net.minecraftforge.event.entity.player.EntityItemPickupEvent event
    ) {
        if (!WarContryAuthManager.isAuthenticated(event.getEntity().getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemToss(
            net.minecraftforge.event.entity.item.ItemTossEvent event
    ) {
        if (event.getPlayer() instanceof ServerPlayer player
                && !WarContryAuthManager.isAuthenticated(player.getUUID())) {
            event.setCanceled(true);
        }
    }
}
