package com.cargakawaii;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/** Mod solo de cliente: decora las pantallas de carga. Todo el dibujo vive en {@code client/}. */
@Mod(CargaKawaii.MOD_ID)
public class CargaKawaii {
    public static final String MOD_ID = "cargakawaii";

    public CargaKawaii() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, KawaiiConfig.SPEC);
    }
}
