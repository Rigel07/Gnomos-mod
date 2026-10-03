# Genera: textures/gui/startup.png (letras pixel para la pantalla de arranque, donde aun no hay fuentes ni idiomas)
#         textures/gui/title/background/panorama_0..5.png + panorama_overlay.png (fondo del menu principal)
# Ejecutar: python3 tools_gen_startup.py
from PIL import Image, ImageDraw
import math, os, random

BASE = "src/main/resources/assets"

# ------------------------------------------------------------------ letras pixel 5x7
GLYPHS = "CARGNDO0123456789%."          # mismo orden que KawaiiPainter.GLYPHS
FONT = {
 "A": [".###.","#...#","#...#","#####","#...#","#...#","#...#"],
 "C": [".###.","#...#","#....","#....","#....","#...#",".###."],
 "R": ["####.","#...#","#...#","####.","#.#..","#..#.","#...#"],
 "G": [".###.","#...#","#....","#.###","#...#","#...#",".###."],
 "N": ["#...#","##..#","#.#.#","#..##","#...#","#...#","#...#"],
 "D": ["####.","#...#","#...#","#...#","#...#","#...#","####."],
 "O": [".###.","#...#","#...#","#...#","#...#","#...#",".###."],
 "0": [".###.","#...#","#..##","#.#.#","##..#","#...#",".###."],
 "1": ["..#..",".##..","..#..","..#..","..#..","..#..",".###."],
 "2": [".###.","#...#","....#","...#.","..#..",".#...","#####"],
 "3": [".###.","#...#","....#","..##.","....#","#...#",".###."],
 "4": ["...#.","..##.",".#.#.","#..#.","#####","...#.","...#."],
 "5": ["#####","#....","####.","....#","....#","#...#",".###."],
 "6": [".###.","#....","#....","####.","#...#","#...#",".###."],
 "7": ["#####","....#","...#.","..#..",".#...",".#...",".#..."],
 "8": [".###.","#...#","#...#",".###.","#...#","#...#",".###."],
 "9": [".###.","#...#","#...#",".####","....#","....#",".###."],
 "%": ["##..#","##..#","...#.","..#..",".#...","#..##","#..##"],
 ".": [".....",".....",".....",".....",".....",".....","..##."],
}
CELL_W, CELL_H = 8, 10
txt = Image.new("RGBA", (160, 16), (0, 0, 0, 0))
for idx, ch in enumerate(GLYPHS):
    ox = idx * CELL_W
    px = set()
    for y, row in enumerate(FONT[ch]):
        for x, c in enumerate(row):
            if c == "#": px.add((x + 1, y + 1))
    for (x, y) in px:                                   # contorno (8 vecinos)
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                q = (x + dx, y + dy)
                if q not in px and 0 <= q[0] < CELL_W and 0 <= q[1] < CELL_H:
                    txt.putpixel((ox + q[0], q[1]), (140, 80, 130, 255))
    for (x, y) in px:
        txt.putpixel((ox + x, y), (255, 255, 255, 255))
os.makedirs(f"{BASE}/cargakawaii/textures/gui", exist_ok=True)
txt.save(f"{BASE}/cargakawaii/textures/gui/startup.png")

# ------------------------------------------------------------------ panorama del menu principal
S = 512
SKY_TOP = (205, 182, 255); SKY_MID = (255, 191, 222); HORIZON = (255, 233, 214)
FAR = (236, 204, 244); MID = (200, 232, 215); NEAR = (170, 226, 190); GROUND = (150, 214, 168)

def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def sky_color(y):
    t = y / S
    if t < 0.55: return lerp(SKY_TOP, SKY_MID, t / 0.55)
    return lerp(SKY_MID, HORIZON, min(1.0, (t - 0.55) / 0.25))

def cloud(d, cx, cy, sc):
    for dx, dy, r in [(-34, 6, 20), (-12, -6, 26), (16, -2, 24), (38, 7, 18), (0, 10, 24)]:
        d.ellipse([cx + (dx - r) * sc, cy + (dy - r) * sc, cx + (dx + r) * sc, cy + (dy + r) * sc], fill=(255, 255, 255, 235))

def heart(d, cx, cy, sc, col):
    pts = []
    for i in range(60):
        t = i / 60 * 2 * math.pi
        x = 16 * math.sin(t) ** 3
        y = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
        pts.append((cx + x * sc, cy + y * sc))
    d.polygon(pts, fill=col + (230,))
    d.ellipse([cx - 9 * sc, cy - 11 * sc, cx - 5 * sc, cy - 7 * sc], fill=(255, 255, 255, 170))

def sparkle(d, cx, cy, r, col=(255, 255, 255, 235)):
    d.polygon([(cx, cy - r), (cx + r * .25, cy - r * .25), (cx + r, cy), (cx + r * .25, cy + r * .25),
               (cx, cy + r), (cx - r * .25, cy + r * .25), (cx - r, cy), (cx - r * .25, cy - r * .25)], fill=col)

def flower(d, cx, cy, sc):
    for k in range(5):
        a = k * 2 * math.pi / 5
        x, y = cx + 7 * sc * math.cos(a), cy + 7 * sc * math.sin(a)
        d.ellipse([x - 5 * sc, y - 5 * sc, x + 5 * sc, y + 5 * sc], fill=(255, 170, 205, 255))
    d.ellipse([cx - 4 * sc, cy - 4 * sc, cx + 4 * sc, cy + 4 * sc], fill=(255, 226, 90, 255))

def hill_y(x, base, comps):
    # f(0) = f(S) = base para TODAS las caras -> las uniones entre caras quedan continuas en cualquier sentido
    return base + sum(A * S * math.sin(2 * math.pi * k * x / S + ph) - A * S * math.sin(ph) for A, k, ph in comps)

def side_face(n):
    rnd = random.Random(100 + n)
    im = Image.new("RGBA", (S, S))
    d = ImageDraw.Draw(im)
    for y in range(S): d.line([(0, y), (S, y)], fill=sky_color(y) + (255,))
    ov = Image.new("RGBA", (S, S), (0, 0, 0, 0)); od = ImageDraw.Draw(ov)
    if n == 0:                                                       # arcoiris en la cara frontal
        cols = [(255, 140, 160), (255, 190, 140), (255, 240, 150), (170, 235, 170), (150, 210, 255), (200, 170, 255)]
        for i, c in enumerate(cols):
            r = S * 0.46 - i * S * 0.02
            od.ellipse([S / 2 - r, S * 0.80 - r, S / 2 + r, S * 0.80 + r], outline=c + (230,), width=int(S * 0.02) + 1)
    for _ in range(6):                                               # destellos
        sparkle(od, rnd.uniform(.12, .88) * S, rnd.uniform(.04, .45) * S, rnd.uniform(5, 11))
    for i in range(3):                                               # corazones flotando
        heart(od, rnd.uniform(.16, .84) * S, rnd.uniform(.12, .5) * S, rnd.uniform(.6, 1.1),
              rnd.choice([(255, 120, 180), (150, 190, 255), (255, 215, 90), (200, 130, 255)]))
    for i in range(2 + n % 2):                                       # nubes
        cloud(od, rnd.uniform(.25, .75) * S, rnd.uniform(.14, .42) * S, rnd.uniform(.9, 1.5))
    im.alpha_composite(ov)
    d = ImageDraw.Draw(im)
    far = [(0.03, 2 + n % 2, 0.7), (0.015, 3 + n % 3, 2.0)]
    mid = [(0.035, 1 + n % 2, 1.2), (0.015, 4, 0.3)]
    near = [(0.03, 2, 0.5 + n), (0.012, 5, 1.1)]
    for base, comps, col in [(0.64 * S, far, FAR), (0.72 * S, mid, MID)]:
        d.polygon([(0, S)] + [(x, hill_y(x, base, comps)) for x in range(0, S + 1, 4)] + [(S, S)], fill=col + (255,))
    for y in range(int(0.78 * S), S):                                # colina cercana con degradado hasta el color del suelo
        pass
    nb = 0.80 * S
    poly = [(0, S)] + [(x, hill_y(x, nb, near)) for x in range(0, S + 1, 4)] + [(S, S)]
    d.polygon(poly, fill=NEAR + (255,))
    for y in range(int(0.84 * S), S):
        d.line([(0, y), (S, y)], fill=lerp(NEAR, GROUND, (y - 0.84 * S) / (0.16 * S)) + (255,))
    for _ in range(9):                                               # florecitas en la colina
        x = rnd.uniform(.12, .88) * S
        y = max(hill_y(x, nb, near), 0.8 * S) + rnd.uniform(10, 0.15 * S)
        flower(d, x, min(y, S - 14), rnd.uniform(.7, 1.1))
    return im.convert("RGB")

def top_face():
    im = Image.new("RGBA", (S, S), SKY_TOP + (255,))
    d = ImageDraw.Draw(im)
    for r in range(int(S * 0.5), 0, -4):                             # centro algo mas claro, borde = SKY_TOP exacto
        t = 1 - r / (S * 0.5)
        c = lerp(SKY_TOP, (235, 215, 255), t)
        d.ellipse([S / 2 - r, S / 2 - r, S / 2 + r, S / 2 + r], fill=c + (255,))
    rnd = random.Random(7)
    for _ in range(16):
        sparkle(d, rnd.uniform(.15, .85) * S, rnd.uniform(.15, .85) * S, rnd.uniform(5, 11))
    for _ in range(3):
        cloud(d, rnd.uniform(.3, .7) * S, rnd.uniform(.3, .7) * S, 1.2)
    return im.convert("RGB")

def bottom_face():
    im = Image.new("RGBA", (S, S), GROUND + (255,))
    d = ImageDraw.Draw(im)
    rnd = random.Random(9)
    for _ in range(14):
        flower(d, rnd.uniform(.14, .86) * S, rnd.uniform(.14, .86) * S, rnd.uniform(.8, 1.3))
    for _ in range(8):
        sparkle(d, rnd.uniform(.1, .9) * S, rnd.uniform(.1, .9) * S, rnd.uniform(4, 8), (255, 255, 255, 200))
    return im.convert("RGB")

out = f"{BASE}/minecraft/textures/gui/title/background"
os.makedirs(out, exist_ok=True)
for n in range(4): side_face(n).save(f"{out}/panorama_{n}.png")
top_face().save(f"{out}/panorama_4.png")
bottom_face().save(f"{out}/panorama_5.png")

ov = Image.new("RGBA", (16, 128))
for y in range(128):
    a = int(70 - 50 * y / 127)
    for x in range(16): ov.putpixel((x, y), (255, 232, 246, a))
ov.save(f"{out}/panorama_overlay.png")

# ------------------------------------------------------------------ frases del menu (reemplazan las de vanilla)
SPLASH = [
 "¡Hecho con mucho cariño!", "¡Mao te vigila (con amor)!", "¡Flowy te adora!", "¡La nutria quiere salmón!",
 "¡Sé amable hoy!", "¡Inocencia al máximo!", "¡Un abrazo virtual!", "¡Ahora 100% más cute!",
 "¡Planta flores!", "¡Nivel Santo!", "uwu", "¡Todo es mejor con corazones!", "¡Una sonrisa por bloque!",
 "¡Zanahorias doradas!", "¡Hoy toca aventura!", "¡Cuida a tus nutrias!", "¡Las Flowys flotan!",
 "¡Te quiero, humano!", "¡Corazón de Melon!", "¡Mejor con amigos!", "¡Pastel de calabaza!",
 "¡Respira y sonríe!", "¡Qué bonito día!", "¡Aureolas para todos!", "¡Con purpurina!",
]
os.makedirs(f"{BASE}/minecraft/texts", exist_ok=True)
with open(f"{BASE}/minecraft/texts/splashes.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(SPLASH) + "\n")

# previsualizacion rapida
prev = Image.new("RGB", (S * 4, S), (0, 0, 0))
for n in range(4): prev.paste(Image.open(f"{out}/panorama_{n}.png"), (S * n, 0))
prev.resize((S * 2, S // 2)).save("/tmp/panorama_prev.png")
print("ok")
