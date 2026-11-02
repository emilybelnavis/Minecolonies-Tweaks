package dev.minecoloniestweaks.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue COOK_SELF_FEEDING;
    public static final ModConfigSpec.BooleanValue CUSTOM_NAMES;
    public static final ModConfigSpec.ConfigValue<String> NAME_LOCALE;

    static {
        var builder = new ModConfigSpec.Builder();
        COOK_SELF_FEEDING = builder.comment("Let restaurant cooks eat prepared meals and keep cooking when none are ready.")
                .define("cookSelfFeeding", true);
        CUSTOM_NAMES = builder.comment("Use this mod's name lists for newly generated citizens only.")
                .define("customCitizenNames", true);
        NAME_LOCALE = builder.comment("en_ca, en_us, en_gb, fr_ca, fr_fr, zh_cn_latn, or zh_tw_latn.",
                        "Override each list in config/minecoloniestweaks/names/<locale>.json.")
                .define("nameLocale", "en_ca");
        SPEC = builder.build();
    }

    private ServerConfig() {}
}
