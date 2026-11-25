package com.warcontry.auth;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(WarContryAuth.MOD_ID)
public class WarContryAuth {

    public static final String MOD_ID = "warcountryauth";

    public WarContryAuth() {
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON,
                WarContryAuthConfig.SPEC
        );

        WarContryAuthNetwork.register();

        System.out.println("[War Contry Auth] Le mod est chargé !");
    }
}
