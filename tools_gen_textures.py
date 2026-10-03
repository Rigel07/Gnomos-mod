# Genera la hoja de sprites de Carga Kawaii. Ejecutar: python3 tools_gen_textures.py
# Hoja 128x128 (ver KawaiiPainter.java para las coordenadas de cada sprite).
from PIL import Image
import math, os

OUT = "src/main/resources/assets/cargakawaii/textures/gui/kawaii.png"
sheet = Image.new("RGBA", (128, 128), (0, 0, 0, 0))

def canvas(w, h):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))

def ellipse(img, cx, cy, rx, ry, c):
    for y in range(img.height):
        for x in range(img.width):
            dx, dy = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
            if dx * dx + dy * dy <= 1.0:
                img.putpixel((x, y), c + (255,))

def rect(img, x, y, w, h, c):
    for i in range(x, x + w):
        for j in range(y, y + h):
            if 0 <= i < img.width and 0 <= j < img.height:
                img.putpixel((i, j), c + (255,))

def outline(img, c):
    src = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            if src.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < img.width and 0 <= ny < img.height and src.getpixel((nx, ny))[3] > 0:
                        img.putpixel((x, y), c + (255,))
                        break

SKIN = (255, 224, 200); HAIR = (255, 226, 140); HAIR_D = (240, 190, 90)
WHITE = (255, 255, 255); BLUE = (200, 228, 255); GOLD = (255, 215, 0)
EYE = (60, 40, 30); BLUSH = (255, 150, 170); MOUTH = (200, 90, 100)

# ---------------- Mao (angelito) ----------------
def mao(blink=False):
    im = canvas(32, 32)
    ellipse(im, 6, 17, 4, 7, WHITE); ellipse(im, 26, 17, 4, 7, WHITE)       # alas
    rect(im, 3, 21, 5, 2, BLUE); rect(im, 24, 21, 5, 2, BLUE)
    ellipse(im, 16, 26, 6, 4.5, WHITE); rect(im, 11, 25, 10, 1, GOLD)       # tunica y cinturon
    ellipse(im, 16, 13, 9, 8, HAIR)                                          # pelo
    ellipse(im, 16, 15, 7.5, 5.5, SKIN)                                      # cara
    ellipse(im, 16, 9, 8.5, 3.2, HAIR)                                       # flequillo
    rect(im, 15, 10, 2, 2, HAIR); rect(im, 12, 10, 1, 1, HAIR); rect(im, 19, 10, 1, 1, HAIR)
    rect(im, 7, 15, 2, 5, HAIR_D); rect(im, 23, 15, 2, 5, HAIR_D)
    if blink:
        rect(im, 10, 15, 3, 1, EYE); rect(im, 19, 15, 3, 1, EYE)
    else:
        rect(im, 11, 14, 2, 3, EYE); rect(im, 19, 14, 2, 3, EYE)
        im.putpixel((11, 14), WHITE + (255,)); im.putpixel((19, 14), WHITE + (255,))
    rect(im, 9, 17, 2, 1, BLUSH); rect(im, 21, 17, 2, 1, BLUSH)
    rect(im, 15, 18, 2, 1, MOUTH)
    # aureola
    ring = canvas(32, 32); ellipse(ring, 16, 3.5, 6, 2.5, GOLD)
    hole = canvas(32, 32); ellipse(hole, 16, 3.5, 3.6, 1.0, (0, 0, 0))
    for y in range(32):
        for x in range(32):
            if hole.getpixel((x, y))[3] > 0:
                ring.putpixel((x, y), (0, 0, 0, 0))
    im.alpha_composite(ring)
    outline(im, (150, 90, 110))
    return im

# ---------------- Flowy (florecita con cara) ----------------
def flowy(blink=False):
    im = canvas(32, 32)
    rect(im, 14, 21, 4, 10, (80, 175, 70))
    ellipse(im, 9, 26, 4.5, 2.2, (100, 195, 85)); ellipse(im, 23, 24, 4.5, 2.2, (100, 195, 85))
    for k in range(8):
        a = k * math.pi / 4
        ellipse(im, 16 + 7.3 * math.cos(a), 12 + 7.3 * math.sin(a), 3.6, 3.6, (255, 205, 40))
    ellipse(im, 16, 12, 6.6, 6.6, (250, 150, 55))
    if blink:
        rect(im, 11, 11, 3, 1, EYE); rect(im, 18, 11, 3, 1, EYE)
    else:
        rect(im, 12, 10, 2, 3, EYE); rect(im, 18, 10, 2, 3, EYE)
        im.putpixel((12, 10), WHITE + (255,)); im.putpixel((18, 10), WHITE + (255,))
    rect(im, 10, 13, 2, 1, BLUSH); rect(im, 20, 13, 2, 1, BLUSH)
    rect(im, 14, 14, 4, 1, EYE); rect(im, 15, 15, 2, 1, EYE)                  # sonrisa
    outline(im, (140, 90, 30))
    return im

# ---------------- Nutria ----------------
BROWN = (150, 100, 62); BROWN_D = (110, 70, 42); CREAM = (240, 220, 180); NOSE = (60, 40, 30)
def nutria(blink=False):
    im = canvas(32, 32)
    ellipse(im, 27, 27, 4, 2.2, BROWN)                                         # cola
    ellipse(im, 16, 24, 9, 6, BROWN); ellipse(im, 16, 25.5, 5, 4, CREAM)       # cuerpo
    ellipse(im, 11, 29.5, 2.6, 1.6, BROWN_D); ellipse(im, 21, 29.5, 2.6, 1.6, BROWN_D)
    ellipse(im, 8, 6, 2.8, 2.8, BROWN); ellipse(im, 24, 6, 2.8, 2.8, BROWN)   # orejas
    ellipse(im, 8, 6.5, 1.4, 1.4, (255, 180, 190)); ellipse(im, 24, 6.5, 1.4, 1.4, (255, 180, 190))
    ellipse(im, 16, 13, 10, 8, BROWN)                                          # cabeza
    ellipse(im, 16, 17, 6, 3.8, CREAM)                                         # hocico
    rect(im, 15, 15, 2, 2, NOSE)
    rect(im, 15, 18, 2, 1, MOUTH)
    if blink:
        rect(im, 9, 12, 3, 1, NOSE); rect(im, 20, 12, 3, 1, NOSE)
    else:
        rect(im, 10, 11, 2, 3, (35, 22, 18)); rect(im, 20, 11, 2, 3, (35, 22, 18))
        im.putpixel((10, 11), WHITE + (255,)); im.putpixel((20, 11), WHITE + (255,))
    rect(im, 7, 15, 2, 1, BLUSH); rect(im, 23, 15, 2, 1, BLUSH)
    for y in (16, 18):                                                         # bigotes
        im.putpixel((5, y), CREAM + (255,)); im.putpixel((26, y), CREAM + (255,))
    outline(im, (80, 50, 35))
    return im

# ---------------- corazones (igual que Corazon de Melon) ----------------
HEART = [
"..XXX...XXX..", ".XXXXX.XXXXX.", "XXXXXXXXXXXXX", "XXXXXXXXXXXXX", "XXXXXXXXXXXXX",
".XXXXXXXXXXX.", "..XXXXXXXXX..", "...XXXXXXX...", "....XXXXX....", ".....XXX.....", "......X......",
]
def heart(fill, dark, light):
    im = canvas(16, 16); mask = set()
    for y, row in enumerate(HEART):
        for x, c in enumerate(row):
            if c == "X": mask.add((x, y))
    for (x, y) in mask:
        edge = any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        im.putpixel((x + 1, y + 2), (dark if edge else fill) + (255,))
    for (x, y) in [(2, 3), (3, 3), (2, 4), (3, 2), (9, 3)]:
        if (x, y) in mask: im.putpixel((x + 1, y + 2), light + (255,))
    return im

def sparkle():
    im = canvas(16, 16)
    pat = ["....X....", "....X....", "...XXX...", "..XXXXX..", "XXXXXXXXX", "..XXXXX..", "...XXX...", "....X....", "....X...."]
    for y, row in enumerate(pat):
        for x, c in enumerate(row):
            if c == "X": im.putpixel((x + 3, y + 3), (255, 255, 255, 255))
    for (x, y) in [(6, 7), (7, 6), (7, 7), (7, 8), (8, 7)]:      # centro amarillito
        im.putpixel((x, y), (255, 235, 120, 255))
    return im

def cloud():
    im = canvas(32, 16)
    ellipse(im, 9, 10, 7, 5, (200, 220, 245))
    ellipse(im, 22, 10, 8, 5, (200, 220, 245))
    ellipse(im, 15, 6.5, 7, 5, (200, 220, 245))
    ellipse(im, 9, 9, 7, 5, WHITE); ellipse(im, 22, 9, 8, 5, WHITE); ellipse(im, 15, 6, 7, 5, WHITE)
    rect(im, 5, 11, 22, 3, WHITE)
    outline(im, (190, 205, 235))
    return im

def flower():
    im = canvas(16, 16)
    for k in range(5):
        a = k * 2 * math.pi / 5 - math.pi / 2
        ellipse(im, 8 + 3.3 * math.cos(a), 8 + 3.3 * math.sin(a), 2.4, 2.4, (255, 170, 200))
    ellipse(im, 8, 8, 1.8, 1.8, (255, 225, 80))
    outline(im, (200, 100, 140))
    return im

def put(img, x, y):
    sheet.alpha_composite(img, (x, y))

put(mao(), 0, 0);        put(flowy(), 32, 0);        put(nutria(), 64, 0)
put(mao(True), 0, 32);   put(flowy(True), 32, 32);   put(nutria(True), 64, 32)
put(heart((255, 105, 180), (150, 30, 90), (255, 200, 230)), 0, 64)
put(heart((80, 170, 255), (20, 70, 160), (200, 235, 255)), 16, 64)
put(heart((255, 205, 40), (170, 110, 0), (255, 245, 170)), 32, 64)
put(heart((190, 90, 255), (90, 30, 150), (235, 200, 255)), 48, 64)
put(sparkle(), 64, 64)
put(flower(), 80, 64)
put(cloud(), 0, 80)

os.makedirs(os.path.dirname(OUT), exist_ok=True)
sheet.save(OUT)
print("ok", OUT)
