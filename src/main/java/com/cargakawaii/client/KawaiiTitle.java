package com.cargakawaii.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.lang.ref.WeakReference;
import java.util.List;

import static com.cargakawaii.client.KawaiiPainter.*;

/**
 * Decoracion del menu principal: corazones y destellos flotando (transparentes, no tapan los botones) y los
 * personajes de tus mods en las esquinas saludando. El fondo (panorama pastel) y las frases del menu van como
 * recursos en assets/minecraft/.
 */
final class KawaiiTitle {
    private static WeakReference<Screen> last = new WeakReference<>(null);
    private static long openedAt = 0L;

    private KawaiiTitle() {}

    static void paint(GuiGraphics g, Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int w = screen.width;
        int h = screen.height;
        long now = Util.getMillis();
        if (last.get() != screen) {
            last = new WeakReference<>(screen);
            openedAt = now;
        }
        float fade = Mth.clamp((now - openedAt - 500L) / 800f, 0f, 1f);   // entra despues del fundido del menu
        if (fade < 0.05f) return;
        float time = (now % 600000L) / 1000f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        floating(g, w, h, time, fade, 12, 12, 0.5f);

        if (w >= 460 && h >= 200) {
            List<Integer> list = mascotList();
            float s = 1.5f;
            float ground = h - 16;
            for (int i = 0; i < list.size(); i++) {
                int m = list.get(i);
                int slot = i / 2;
                float cx = (i % 2 == 0) ? 38 + slot * 54 : w - 38 - slot * 54;

                float hop = Math.abs(Mth.sin(time * 2.6f + i * 1.3f));
                float dy = -hop * 5f * s;
                float sy = 1f - 0.08f * (1f - hop);
                float sx = 1f + 0.06f * (1f - hop);
                boolean blink = ((time + i * 1.1f) % 3.8f) < 0.16f;
                int[] uv = blink ? MASCOT_BLINK_UV[m] : MASCOT_UV[m];
                draw(g, uv[0], uv[1], 32, 32, cx, ground + dy, 16, 32, sx * s, sy * s, fade);

                drawBubble(g, font, m, cx, ground - MASCOT_SIZE * s - 6f + dy, w, (time + i * 3.1f) % 10f, fade);
            }
        }
        g.setColor(1f, 1f, 1f, 1f);
    }

    /** Globito de dialogo que aparece 4 s de cada 10. */
    private static void drawBubble(GuiGraphics g, Font font, int mascot, float cx, float bottomY, int w, float cycle, float fade) {
        if (cycle >= 4f) return;
        float a = Mth.clamp(Math.min(cycle, 4f - cycle) * 3f, 0f, 1f) * fade;
        if (a < 0.1f) return;
        String text = Component.translatable("cargakawaii.hello." + mascot).getString();
        int bw = font.width(text) + 10;
        int bx = Mth.clamp((int) (cx - bw / 2f), 2, Math.max(2, w - bw - 2));
        int by = (int) bottomY - 16;
        int white = argb(0xFFFFFF, 0.95f * a);
        g.fill(bx + 1, by, bx + bw - 1, by + 14, white);
        g.fill(bx, by + 1, bx + bw, by + 13, white);
        int tx = (int) cx;
        g.fill(tx - 2, by + 14, tx + 2, by + 15, white);
        g.fill(tx - 1, by + 15, tx + 1, by + 16, white);
        g.drawString(font, text, bx + 5, by + 3, argb(0xB5427A, a), false);
    }
}
