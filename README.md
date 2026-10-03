# Carga Kawaii 1.1.0

Mod **solo de cliente** para **Minecraft Forge 1.20.1** (Forge 47.3.0+, Java 17) que lo deja todo muy *cute*.
Sale el personaje de cada uno de tus mods si está instalado (si no hay ninguno, salen los tres):
Mao (`corazondemelon`), Flowy (`flowys`) y la nutria (`nutriamod`).

## Qué cambia

1. **Arranque del juego** (la pantalla roja de Mojang): se cubre con cielo pastel, nubes, personajes saltando,
   "CARGANDO..." con letras pixel y una barra de 10 corazones que se llenan con el progreso real y un porcentaje.
   Mantiene el mismo fundido de la original. También sale al recargar recursos (F3+T).
2. **Menú principal**: panorama pastel con arcoíris, colinas y flores; corazones y destellos flotando;
   los personajes en las esquinas con globitos de diálogo; frases del menú nuevas.
3. **Pantallas de carga del mundo**: "Cargando terreno", "Preparando tu mundo", "Guardando", "Conectando", con
   consejitos que cambian según los mods instalados.

No sustituye pantallas: pinta encima de las originales, así que su lógica no cambia.

## Lo que NO se puede cambiar

La ventana de carga de **Forge** (la de las barras de "cargando mods", que aparece primero) se dibuja antes de que
cargue ningún mod, así que un mod no puede tocarla.

## Ajustes

`config/cargakawaii-client.toml`: `enabled`, `startupOverlay`, `titleDecor`, `showMascots`, `showTips`, `floatingDecor`.
El panorama y las frases del menú son recursos: un paquete de recursos que actives encima los reemplaza.

## Compilar con GitHub (sin instalar nada)

Sube TODO el contenido (incluida la carpeta oculta `.github`) a un repositorio, espera al Build verde en Actions
y descarga el artefacto `carga-kawaii` (contiene el `.jar`). Mételo en `mods/` (solo en el cliente).

## Texturas

`tools_gen_textures.py` (sprites) y `tools_gen_startup.py` (letras, panorama, frases) con Pillow.
