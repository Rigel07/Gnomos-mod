package com.cargakawaii;

import net.minecraftforge.common.ForgeConfigSpec;

/** Ajustes de cliente (config/cargakawaii-client.toml). */
public final class KawaiiConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue STARTUP_OVERLAY;
    public static final ForgeConfigSpec.BooleanValue TITLE_DECOR;
    public static final ForgeConfigSpec.BooleanValue SHOW_MASCOTS;
    public static final ForgeConfigSpec.BooleanValue SHOW_TIPS;
    public static final ForgeConfigSpec.BooleanValue FLOATING_DECOR;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("pantalla_de_carga");
        ENABLED = b.comment("Activa todo Carga Kawaii (false = vuelven las pantallas de siempre)")
                .define("enabled", true);
        STARTUP_OVERLAY = b.comment("Cubre la pantalla roja de Mojang del arranque con la version kawaii")
                .define("startupOverlay", true);
        TITLE_DECOR = b.comment("Decoracion del menu principal: corazones flotando y personajes en las esquinas")
                .define("titleDecor", true);
        SHOW_MASCOTS = b.comment("Muestra a Mao, las Flowys y las nutrias saltando")
                .define("showMascots", true);
        SHOW_TIPS = b.comment("Muestra consejitos cute abajo (en las pantallas de carga del mundo)")
                .define("showTips", true);
        FLOATING_DECOR = b.comment("Corazones, nubes y destellos flotando de fondo")
                .define("floatingDecor", true);
        b.pop();
        SPEC = b.build();
    }

    private KawaiiConfig() {}

    // La config todavia puede no estar cargada muy al principio: en ese caso, valores por defecto.
    public static boolean enabled() { return safe(ENABLED); }
    public static boolean startupOverlay() { return safe(STARTUP_OVERLAY); }
    public static boolean titleDecor() { return safe(TITLE_DECOR); }
    public static boolean showMascots() { return safe(SHOW_MASCOTS); }
    public static boolean showTips() { return safe(SHOW_TIPS); }
    public static boolean floatingDecor() { return safe(FLOATING_DECOR); }

    private static boolean safe(ForgeConfigSpec.BooleanValue value) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }
}
