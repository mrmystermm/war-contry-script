
package com.warcontry.auth;

import net.minecraftforge.common.ForgeConfigSpec;

public class WarContryAuthConfig {

    private static final ForgeConfigSpec.Builder BUILDER =
            new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<String> WORKER_URL =
            BUILDER
                    .comment("URL du Worker Cloudflare War Contry")
                    .define("workerUrl",
                            "https://warcontry-accounts.mr-mystermm.workers.dev");

    public static final ForgeConfigSpec.ConfigValue<String> MINECRAFT_LINK_SECRET =
            BUILDER
                    .comment("Secret serveur utilisé pour communiquer avec le Worker. Ne jamais partager.")
                    .define("minecraftLinkSecret", "");

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
