package com.cargakawaii.client;

import com.cargakawaii.CargaKawaii;
import com.cargakawaii.KawaiiConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.Set;

/**
 * Engancha las pantallas de Minecraft y les pinta encima la version kawaii:
 *  - pantallas de carga del mundo (cargando terreno, preparando, guardando, conectando...)
 *  - el menu principal (decoracion)
 *  - la pantalla de arranque de Mojang/Forge (envolviendola desde un tick de cliente)
 * No se reemplazan pantallas: toda la logica original sigue funcionando igual.
 */
@Mod.EventBusSubscriber(modid = CargaKawaii.MOD_ID, value = Dist.CLIENT)
public final class KawaiiEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Nombres de clase (los de Mojang, iguales en desarrollo y en produccion). */
    private static final Set<String> LOADING_SCREENS = Set.of(
            "ReceivingLevelScreen",      // "Cargando terreno..." al entrar a un mundo o cambiar de dimension
            "LevelLoadingScreen",        // "Preparando zona de aparicion" al abrir un mundo
            "ProgressScreen",            // barras de progreso genericas
            "ConnectScreen",             // conectando a un servidor
            "GenericDirtMessageScreen",  // "Guardando el mundo", "Uniendose al mundo"...
            "GenericMessageScreen"
    );

    private static boolean paintFailed = false;
    private static boolean titleFailed = false;
    private static boolean startupFailed = false;
    private static Overlay rejected = null;

    private KawaiiEvents() {}

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        String kind = screen.getClass().getSimpleName();

        if (kind.equals("TitleScreen")) {
            if (titleFailed || !KawaiiConfig.enabled() || !KawaiiConfig.titleDecor()) return;
            try {
                KawaiiTitle.paint(event.getGuiGraphics(), screen);
            } catch (Throwable t) {
                titleFailed = true;
                LOGGER.error("Carga Kawaii: error al decorar el menu principal, se desactiva hasta reiniciar", t);
            }
            return;
        }

        if (paintFailed || !LOADING_SCREENS.contains(kind) || !KawaiiConfig.enabled()) return;
        try {
            KawaiiPainter.paint(event.getGuiGraphics(), screen, kind, event.getMouseX(), event.getMouseY(), event.getPartialTick());
        } catch (Throwable t) {
            // Si algo falla, dejamos de pintar para no romper el juego: se ve la pantalla normal.
            paintFailed = true;
            LOGGER.error("Carga Kawaii: error al dibujar, se desactiva hasta reiniciar", t);
        }
    }

    /** Cada vez que Minecraft pone una pantalla de arranque/recarga de recursos, la envolvemos con la kawaii. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || startupFailed) return;
        if (!KawaiiConfig.enabled() || !KawaiiConfig.startupOverlay()) return;
        Minecraft mc = Minecraft.getInstance();
        Overlay current = mc.getOverlay();
        if (current == null || current instanceof StartupOverlay || current == rejected) return;
        if (!(current instanceof LoadingOverlay)) return;
        try {
            StartupOverlay wrapper = StartupOverlay.wrap(current);
            if (wrapper != null) {
                mc.setOverlay(wrapper);
                LOGGER.info("Carga Kawaii: pantalla de arranque envuelta ({})", current.getClass().getName());
            } else {
                rejected = current;
                LOGGER.warn("Carga Kawaii: no se pudo leer el estado de {}; se deja la pantalla original", current.getClass().getName());
            }
        } catch (Throwable t) {
            rejected = current;
            startupFailed = true;
            LOGGER.error("Carga Kawaii: error al envolver la pantalla de arranque", t);
        }
    }
}
