package com.cargakawaii.client;

import com.cargakawaii.CargaKawaii;
import com.cargakawaii.KawaiiConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.ModList;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Dibuja la pantalla de carga kawaii.
 *  - {@link #paint}: encima de las pantallas de carga del mundo (con texto e idiomas).
 *  - {@link #paintOverlay}: encima de la pantalla de arranque, donde todavia no hay fuentes ni idiomas cargados:
 *    ahi el texto se dibuja con letras-sprite propias (startup.png).
 */
final class KawaiiPainter {
    static final ResourceLocation TEX = new ResourceLocation(CargaKawaii.MOD_ID, "textures/gui/kawaii.png");
    static final ResourceLocation TEXT_TEX = new ResourceLocation(CargaKawaii.MOD_ID, "textures/gui/startup.png");
    static final int SHEET = 128;
    static final int TEXT_W = 160, TEXT_H = 16;
    static final String GLYPHS = "CARGNDO0123456789%.";     // mismo orden que tools_gen_startup.py
    static final int GLYPH_W = 8, GLYPH_H = 10;

    // Coordenadas dentro de kawaii.png (ver tools_gen_textures.py)
    static final int MASCOT_SIZE = 32;
    static final int[][] MASCOT_UV = {{0, 0}, {32, 0}, {64, 0}};        // Mao, Flowy, Nutria
    static final int[][] MASCOT_BLINK_UV = {{0, 32}, {32, 32}, {64, 32}};
    static final String[] MASCOT_MOD = {"corazondemelon", "flowys", "nutriamod"};
    static final int[][] HEART_UV = {{0, 64}, {16, 64}, {32, 64}, {48, 64}}; // rosa, azul, dorado, violeta
    static final int[] SPARKLE_UV = {64, 64};
    static final int[] FLOWER_UV = {80, 64};
    static final int[] CLOUD_UV = {0, 80};                               // 32x16

    static final int[] WAVE_COLORS = {0xFF6FAE, 0xB78CFF, 0x6EC3FF, 0xFFA94D};

    private static final int GENERIC_TIPS = 8;
    private static final int MELON_TIPS = 7;
    private static final int FLOWY_TIPS = 5;
    private static final int NUTRIA_TIPS = 4;

    // Estado de la pantalla de carga actual
    private static WeakReference<Screen> lastScreen = new WeakReference<>(null);
    private static long openedAt = 0L;
    private static int tipSeed = 0;
    private static List<String> tipKeys = null;
    private static List<Integer> mascots = null;

    private KawaiiPainter() {}

    // ------------------------------------------------------------------ puntos de entrada

    static void paint(GuiGraphics g, Screen screen, String kind, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        if (lastScreen.get() != screen) {
            lastScreen = new WeakReference<>(screen);
            openedAt = now;
            tipSeed = new Random().nextInt(1000);
        }
        float fade = Mth.clamp((now - openedAt) / 250f, 0f, 1f);
        core(g, Minecraft.getInstance().font, screen.width, screen.height, headingFor(screen, kind), fade, -1f, now - openedAt, tipSeed);

        // La pantalla original puede tener botones (p. ej. "Cancelar" al conectar): se vuelven a pintar encima.
        for (GuiEventListener child : screen.children()) {
            if (child instanceof Renderable renderable) {
                renderable.render(g, mouseX, mouseY, partialTick);
            }
        }
    }

    /** Pantalla de arranque: sin fuentes ni idiomas todavia, asi que sin texto "normal". */
    static void paintOverlay(GuiGraphics g, float alpha, float progress, long elapsedMs, int seed) {
        core(g, null, g.guiWidth(), g.guiHeight(), null, alpha, progress, elapsedMs, seed);
    }

    // ------------------------------------------------------------------ nucleo

    /**
     * @param font     null = modo arranque (letras-sprite, sin consejitos)
     * @param progress 0..1 si se conoce el progreso real; negativo = animacion sin progreso
     */
    private static void core(GuiGraphics g, Font font, int w, int h, String heading, float fade, float progress, long elapsed, int seed) {
        if (tipKeys == null) tipKeys = buildTipKeys();
        if (mascots == null) mascots = loadedMascots();
        boolean sprite = font == null;
        long now = Util.getMillis();
        float time = (now % 600000L) / 1000f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        drawBackground(g, w, h, fade);
        if (fade < 0.05f) return;      // (el texto con alpha casi 0 se dibujaria opaco: Minecraft lo trata asi)
        if (KawaiiConfig.floatingDecor()) drawDecor(g, w, h, time, fade);

        // ---- medidas del bloque central ----
        float s = h >= 320 ? 2f : (h >= 220 ? 1.5f : 1f);       // escala de personajes
        float ts = h >= 220 ? 2f : 1.5f;                        // escala del titulo
        float hs = s >= 1.5f ? 1.5f : 1f;                       // escala de corazones de la barra

        boolean showMascots = KawaiiConfig.showMascots();
        boolean showTips = !sprite && KawaiiConfig.showTips() && h >= 170;

        int titleH = (int) ((sprite ? GLYPH_H : 9) * ts);
        int mascotH = showMascots ? (int) (MASCOT_SIZE * s + 10 * s) : 0;
        float pctScale = hs >= 1.5f ? 1.5f : 1f;
        int pctH = progress >= 0f ? (int) (GLYPH_H * pctScale) + 4 : 0;
        int barH = (int) (16 * hs) + pctH;

        List<FormattedCharSequence> tipLines = null;
        int tipW = Math.min(300, w - 40);
        int tipH = 0;
        if (showTips && !tipKeys.isEmpty()) {
            int idx = Math.floorMod(seed + (int) (elapsed / 6000L), tipKeys.size());
            tipLines = font.split(Component.translatable(tipKeys.get(idx)), tipW - 16);
            tipH = tipLines.size() * 10 + 8 + 12;                // +12 para la linea "Consejito"
        }

        int total = titleH + 8 + (showMascots ? mascotH + 8 : 0) + barH + (tipH > 0 ? 12 + tipH : 0);
        int y = Math.max(4, (h - total) / 2);

        // ---- titulo con letras ondulantes ----
        if (sprite) drawSpriteTitle(g, w, y, w < 220 ? 1f : ts, time, fade);
        else drawTitle(g, font, heading, w, y, ts, time, fade);
        y += titleH + 8;

        // ---- personajes ----
        if (showMascots) {
            drawMascots(g, w, y, s, time, fade);
            y += mascotH + 8;
        }

        // ---- barra de corazones (+ porcentaje) ----
        drawHeartBar(g, w, y, hs, time, fade, progress);
        y += (int) (16 * hs);
        if (progress >= 0f) {
            drawSpriteText(g, ((int) (Mth.clamp(progress, 0f, 1f) * 100f)) + "%", w / 2f, y + 4, pctScale, 0xD6538C, fade, true);
            y += pctH;
        }

        // ---- consejito ----
        if (tipLines != null) {
            y += 12;
            drawTip(g, font, tipLines, w, y, tipW, tipH, fade);
        }

        g.setColor(1f, 1f, 1f, 1f);
    }

    // ------------------------------------------------------------------ fondo y decoracion

    private static void drawBackground(GuiGraphics g, int w, int h, float fade) {
        int top = 0xFFD3E8, mid = 0xE6D6FF, bottom = 0xCDEBFF;
        g.fillGradient(0, 0, w, h / 2, argb(top, fade), argb(mid, fade));
        g.fillGradient(0, h / 2, w, h, argb(mid, fade), argb(bottom, fade));
    }

    private static void drawDecor(GuiGraphics g, int w, int h, float time, float fade) {
        // nubes que pasan
        for (int i = 0; i < 4; i++) {
            float speed = 5f + i * 3f;
            float x = ((time * speed + i * (w / 4f)) % (w + 100f)) - 50f;
            float cy = h * (0.08f + 0.22f * i) + Mth.sin(time * 0.5f + i) * 3f;
            draw(g, CLOUD_UV[0], CLOUD_UV[1], 32, 16, x, cy, 0, 0, 2f, 2f, 0.85f * fade);
        }
        floating(g, w, h, time, fade, 26, 20, 0.6f);
    }

    /** Destellos que titilan y corazones/florecitas que suben. Tambien lo usa el menu principal. */
    static void floating(GuiGraphics g, int w, int h, float time, float fade, int sparkles, int hearts, float heartAlpha) {
        for (int i = 0; i < sparkles; i++) {
            float x = rnd(i, 1) * w;
            float y = rnd(i, 2) * h;
            float tw = 0.5f + 0.5f * Mth.sin(time * (1.5f + rnd(i, 3) * 2f) + i * 1.7f);
            float sc = 0.4f + 0.5f * rnd(i, 4);
            draw(g, SPARKLE_UV[0], SPARKLE_UV[1], 16, 16, x, y, 8, 8, sc * (0.7f + 0.3f * tw), sc * (0.7f + 0.3f * tw), tw * 0.9f * fade);
        }
        for (int i = 0; i < hearts; i++) {
            float speed = 10f + 18f * rnd(i, 5);
            float cycle = h + 40f;
            float y = h + 20f - ((time * speed + rnd(i, 6) * cycle) % cycle);
            float x = rnd(i, 7) * w + Mth.sin(time * 1.2f + i) * 8f;
            float sc = 0.5f + 0.6f * rnd(i, 8);
            int kind = (int) (rnd(i, 9) * 5);                    // 0-3 corazones, 4 florecita
            int[] uv = kind == 4 ? FLOWER_UV : HEART_UV[kind];
            draw(g, uv[0], uv[1], 16, 16, x, y, 8, 8, sc, sc, heartAlpha * fade);
        }
    }

    // ------------------------------------------------------------------ titulo

    private static void drawTitle(GuiGraphics g, Font font, String heading, int w, int y, float ts, float time, float fade) {
        int dots = (int) (time * 2.5f) % 4;
        String full = heading + "...";
        float totalW = font.width(full) * ts;
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate((w - totalW) / 2f, y, 0);
        pose.scale(ts, ts, 1f);
        int x = 0;
        String shown = heading + ".".repeat(dots);
        for (int k = 0; k < shown.length(); k++) {
            String ch = String.valueOf(shown.charAt(k));
            int dy = Math.round(Mth.sin(time * 5f - k * 0.55f) * 2f);
            g.drawString(font, ch, x + 1, dy + 1, argb(0xFFFFFF, 0.9f * fade), false);   // contorno blanquito
            g.drawString(font, ch, x, dy, argb(WAVE_COLORS[k % WAVE_COLORS.length], fade), false);
            x += font.width(ch);
        }
        pose.popPose();
    }

    /** "CARGANDO..." con letras-sprite (arranque). */
    private static void drawSpriteTitle(GuiGraphics g, int w, int y, float ts, float time, float fade) {
        String word = "CARGANDO";
        int slots = word.length() + 3;                           // 3 huecos fijos para los puntitos
        float cell = GLYPH_W * ts;
        float x0 = (w - slots * cell) / 2f;
        int dots = (int) (time * 2.5f) % 4;
        for (int k = 0; k < slots; k++) {
            char ch = k < word.length() ? word.charAt(k) : '.';
            if (k >= word.length() && (k - word.length()) >= dots) continue;
            float dy = Mth.sin(time * 5f - k * 0.55f) * 2f * ts;
            drawGlyph(g, ch, x0 + k * cell, y + dy, ts, WAVE_COLORS[k % WAVE_COLORS.length], fade);
        }
    }

    /** Texto corto (digitos, %) con letras-sprite. */
    static void drawSpriteText(GuiGraphics g, String text, float cx, float y, float scale, int rgb, float fade, boolean center) {
        float cell = GLYPH_W * scale;
        float x = center ? cx - text.length() * cell / 2f : cx;
        for (int i = 0; i < text.length(); i++) {
            drawGlyph(g, text.charAt(i), x + i * cell, y, scale, rgb, fade);
        }
    }

    private static void drawGlyph(GuiGraphics g, char ch, float x, float y, float scale, int rgb, float alpha) {
        int idx = GLYPHS.indexOf(ch);
        if (idx < 0) return;
        blitSprite(g, TEXT_TEX, TEXT_W, TEXT_H, idx * GLYPH_W, 0, GLYPH_W, GLYPH_H, x, y, 0, 0, scale, scale,
                ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f, alpha);
    }

    private static String headingFor(Screen screen, String kind) {
        String key = switch (kind) {
            case "ReceivingLevelScreen" -> "cargakawaii.msg.terrain";
            case "LevelLoadingScreen" -> "cargakawaii.msg.world";
            case "ConnectScreen" -> "cargakawaii.msg.connect";
            case "ProgressScreen" -> "cargakawaii.msg.progress";
            default -> null;
        };
        String text;
        if (key != null) {
            text = Component.translatable(key).getString();
        } else {
            text = screen.getTitle().getString().trim();
            if (text.isEmpty()) text = Component.translatable("cargakawaii.msg.generic").getString();
        }
        return text.replaceAll("[.\\u2026\\s]+$", "");
    }

    // ------------------------------------------------------------------ personajes

    static List<Integer> mascotList() {
        if (mascots == null) mascots = loadedMascots();
        return mascots;
    }

    private static List<Integer> loadedMascots() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < MASCOT_MOD.length; i++) {
            if (ModList.get().isLoaded(MASCOT_MOD[i])) list.add(i);
        }
        if (list.isEmpty()) {            // sin ninguno de tus mods: salen los tres igualmente
            for (int i = 0; i < MASCOT_MOD.length; i++) list.add(i);
        }
        return list;
    }

    private static void drawMascots(GuiGraphics g, int w, int y, float s, float time, float fade) {
        int n = mascots.size();
        float cell = MASCOT_SIZE * s;
        float gap = 20f * s;
        float startX = (w - (n * cell + (n - 1) * gap)) / 2f;
        float ground = y + cell;                                  // pies del personaje

        for (int i = 0; i < n; i++) {
            int m = mascots.get(i);
            float cx = startX + i * (cell + gap) + cell / 2f;

            // nubecita de plataforma
            draw(g, CLOUD_UV[0], CLOUD_UV[1], 32, 16, cx, ground - 6f * s, 16, 0, 1.25f * s, 1.25f * s, 0.95f * fade);

            // salto con squash & stretch
            float phase = time * 3.2f + i * 1.1f;
            float hop = Math.abs(Mth.sin(phase));
            float dy = -hop * 7f * s;
            float sy = 1f - 0.10f * (1f - hop);
            float sx = 1f + 0.07f * (1f - hop);
            boolean blink = ((time + i * 0.9f) % 3.4f) < 0.16f;
            int[] uv = blink ? MASCOT_BLINK_UV[m] : MASCOT_UV[m];
            draw(g, uv[0], uv[1], 32, 32, cx, ground + dy, 16, 32, sx * s, sy * s, fade);

            // corazoncito que sube
            float t2 = (time * 0.8f + i * 0.37f) % 1f;
            float hx = cx + Mth.sin(time * 2f + i) * 6f * s;
            float hy = ground - cell - t2 * 18f * s;
            int[] hu = HEART_UV[(i + m) % 4];
            draw(g, hu[0], hu[1], 16, 16, hx, hy, 8, 8, 0.48f * s, 0.48f * s, (1f - t2) * fade);
        }
    }

    // ------------------------------------------------------------------ barra de corazones

    private static void drawHeartBar(GuiGraphics g, int w, int y, float hs, float time, float fade, float progress) {
        int count = 10;
        float step = 14f * hs;
        float startX = (w - count * step) / 2f;
        if (progress >= 0f) {
            float lit = Mth.clamp(progress, 0f, 1f) * count;
            for (int i = 0; i < count; i++) {
                int[] uv = HEART_UV[i % 4];
                float amount = Mth.clamp(lit - i, 0f, 1f);
                float alpha = 0.28f + 0.72f * amount;
                float sc = hs;
                if (amount > 0f && amount < 1f) sc = hs * (1f + 0.18f * Mth.sin(time * 9f));   // el que se esta llenando late
                draw(g, uv[0], uv[1], 16, 16, startX + i * step + 8f * hs, y + 8f * hs, 8, 8, sc, sc, alpha * fade);
            }
            return;
        }
        float cyc = time * 3.5f;
        int lit = (int) (cyc % 13);                              // 0..12 (los ultimos tiempos: todo encendido)
        float frac = cyc % 1f;
        for (int i = 0; i < count; i++) {
            int[] uv = HEART_UV[i % 4];
            float alpha = i < lit ? 1f : 0.28f;
            float sc = hs;
            if (i == lit - 1) sc = hs * (1f + 0.35f * (1f - frac));   // el recien encendido "pop"
            draw(g, uv[0], uv[1], 16, 16, startX + i * step + 8f * hs, y + 8f * hs, 8, 8, sc, sc, alpha * fade);
        }
    }

    // ------------------------------------------------------------------ consejito

    private static List<String> buildTipKeys() {
        List<String> keys = new ArrayList<>();
        for (int i = 1; i <= GENERIC_TIPS; i++) keys.add("cargakawaii.tip.generic." + i);
        ModList mods = ModList.get();
        if (mods.isLoaded("corazondemelon")) for (int i = 1; i <= MELON_TIPS; i++) keys.add("cargakawaii.tip.melon." + i);
        if (mods.isLoaded("flowys")) for (int i = 1; i <= FLOWY_TIPS; i++) keys.add("cargakawaii.tip.flowy." + i);
        if (mods.isLoaded("nutriamod")) for (int i = 1; i <= NUTRIA_TIPS; i++) keys.add("cargakawaii.tip.nutria." + i);
        return keys;
    }

    private static void drawTip(GuiGraphics g, Font font, List<FormattedCharSequence> lines, int w, int y, int boxW, int boxH, float fade) {
        int x1 = (w - boxW) / 2;
        int x2 = x1 + boxW;
        int y2 = y + boxH;
        int fill = argb(0xFFFFFF, 0.6f * fade);
        int border = argb(0xFFFFFF, 0.9f * fade);
        // "pildora" con esquinas redondeadas: dos rectangulos cruzados
        g.fill(x1 + 1, y, x2 - 1, y2, border);
        g.fill(x1, y + 1, x2, y2 - 1, border);
        g.fill(x1 + 2, y + 1, x2 - 2, y2 - 1, fill);
        g.fill(x1 + 1, y + 2, x2 - 1, y2 - 2, fill);

        int ty = y + 5;
        String label = Component.translatable("cargakawaii.tip.label").getString();
        g.drawString(font, label, (w - font.width(label)) / 2, ty, argb(0xD6538C, fade), false);
        ty += 12;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, (w - font.width(line)) / 2, ty, argb(0x6B3A5B, fade), false);
            ty += 10;
        }
    }

    // ------------------------------------------------------------------ utilidades

    /**
     * Dibuja un sprite de kawaii.png.
     * (ax, ay) = donde se ancla en pantalla; (ox, oy) = punto del sprite que cae sobre el ancla; sx/sy = escala.
     */
    static void draw(GuiGraphics g, int u, int v, int sw, int sh, float ax, float ay, float ox, float oy,
                     float sx, float sy, float alpha) {
        blitSprite(g, TEX, SHEET, SHEET, u, v, sw, sh, ax, ay, ox, oy, sx, sy, 1f, 1f, 1f, alpha);
    }

    static void blitSprite(GuiGraphics g, ResourceLocation tex, int texW, int texH, int u, int v, int sw, int sh,
                           float ax, float ay, float ox, float oy, float sx, float sy,
                           float r, float gr, float b, float alpha) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(ax, ay, 0);
        pose.scale(sx, sy, 1f);
        pose.translate(-ox, -oy, 0);
        g.setColor(r, gr, b, Mth.clamp(alpha, 0f, 1f));
        g.blit(tex, 0, 0, (float) u, (float) v, sw, sh, texW, texH);
        g.setColor(1f, 1f, 1f, 1f);
        pose.popPose();
    }

    static int argb(int rgb, float alpha) {
        return ((int) (Mth.clamp(alpha, 0f, 1f) * 255f) << 24) | (rgb & 0xFFFFFF);
    }

    /** Numero pseudoaleatorio estable en [0,1) para (indice, canal): no hace falta guardar estado. */
    static float rnd(int i, int k) {
        double v = Math.sin(i * 12.9898 + k * 78.233) * 43758.5453;
        return (float) (v - Math.floor(v));
    }
}
