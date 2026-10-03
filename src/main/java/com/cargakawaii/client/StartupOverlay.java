package com.cargakawaii.client;

import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Envuelve la pantalla de carga de arranque (la de Mojang / Forge). La original sigue haciendo todo su trabajo
 * (terminar la recarga, el fundido, abrir el menu...) y encima pintamos la version kawaii con el mismo fundido.
 *
 * Los campos privados de la original se localizan POR TIPO (long, boolean, ReloadInstance), no por nombre, asi
 * que funciona aunque en produccion los nombres esten ofuscados. Si algo no se puede leer, no se envuelve nada.
 */
final class StartupOverlay extends Overlay {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Overlay inner;
    private final Field fadeOutStart;
    private final Field fadeInStart;   // puede ser null
    private final Field fadeInFlag;    // puede ser null
    private final Field reload;        // puede ser null
    private final long startedAt = Util.getMillis();
    private final int seed = new Random().nextInt(1000);
    private float shown = 0f;
    private boolean failed = false;

    private StartupOverlay(Overlay inner, Field fadeOutStart, Field fadeInStart, Field fadeInFlag, Field reload) {
        this.inner = inner;
        this.fadeOutStart = fadeOutStart;
        this.fadeInStart = fadeInStart;
        this.fadeInFlag = fadeInFlag;
        this.reload = reload;
    }

    /** @return el envoltorio, o null si no se pudo leer el estado de la pantalla original. */
    static StartupOverlay wrap(Overlay inner) {
        Field fadeOut = null, fadeIn = null, flag = null, reload = null;
        for (Class<?> c = inner.getClass(); c != null && c != Overlay.class && c != Object.class; c = c.getSuperclass()) {
            List<Field> longs = new ArrayList<>();
            Field bool = null;
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                Class<?> t = f.getType();
                if (t == long.class) longs.add(f);
                else if (t == boolean.class && bool == null) bool = f;
                else if (reload == null && ReloadInstance.class.isAssignableFrom(t)) reload = f;
            }
            // la clase mas derivada que declara longs es la que realmente se usa al dibujar
            if (fadeOut == null && !longs.isEmpty()) {
                fadeOut = longs.get(0);
                fadeIn = longs.size() > 1 ? longs.get(1) : null;
                flag = bool;
            }
        }
        if (fadeOut == null) return null;
        try {
            fadeOut.setAccessible(true);
            if (fadeIn != null) fadeIn.setAccessible(true);
            if (flag != null) flag.setAccessible(true);
            if (reload != null) reload.setAccessible(true);
            fadeOut.getLong(inner);          // comprobacion de que se puede leer
        } catch (Throwable t) {
            LOGGER.warn("Carga Kawaii: no se puede acceder a los campos de la pantalla de arranque", t);
            return null;
        }
        return new StartupOverlay(inner, fadeOut, fadeIn, flag, reload);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        inner.render(g, mouseX, mouseY, partialTick);       // puede ponerse a si misma a null al terminar
        if (failed) return;
        try {
            long now = Util.getMillis();

            // Mismo fundido que la original: 1 s a tope, luego 1 s apagandose.
            long fo = fadeOutStart.getLong(inner);
            float f = fo > -1L ? (now - fo) / 1000f : -1f;
            float alpha;
            if (f >= 1f) {
                alpha = 1f - Mth.clamp(f - 1f, 0f, 1f);
            } else if (fadeInFlag != null && fadeInStart != null && fadeInFlag.getBoolean(inner)) {
                long fi = fadeInStart.getLong(inner);
                float f1 = fi > -1L ? (now - fi) / 500f : -1f;
                alpha = Mth.clamp(f1, 0f, 1f);
            } else {
                alpha = 1f;
            }
            if (alpha <= 0f) return;

            // Progreso real de la carga (se suaviza para que los corazones no den saltos)
            float actual = -1f;
            if (reload != null) {
                Object r = reload.get(inner);
                if (r instanceof ReloadInstance ri) actual = ri.getActualProgress();
            }
            if (f >= 0f) actual = 1f;
            if (actual >= 0f) shown += (Mth.clamp(actual, 0f, 1f) - shown) * 0.08f;

            KawaiiPainter.paintOverlay(g, alpha, actual >= 0f ? shown : -1f, now - startedAt, seed);
        } catch (Throwable t) {
            failed = true;
            LOGGER.error("Carga Kawaii: error al dibujar la pantalla de arranque; vuelve la original", t);
            Minecraft mc = Minecraft.getInstance();
            if (mc.getOverlay() == this) mc.setOverlay(inner);
        }
    }

    public boolean isPauseScreen() {
        return inner.isPauseScreen();
    }
}
