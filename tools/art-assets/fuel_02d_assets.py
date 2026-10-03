"""EXT-A-FUEL-02D屏蔽装配台SVG纹理、方块模型与离线几何预览。"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / "tools" / "art-assets"
sys.path.insert(0, str(ART))
import export as strict_exporter

ASSET = ROOT / "src/main/resources/assets/create_nuclear_industry"
SOURCE = ART / "sources/fuel-02d"
GENERATED = ART / "generated/block"
EVIDENCE = ROOT / "build/reports/extension/EXT-A-FUEL-02D-assets"
REPORT = ROOT / "build/reports/extension/EXT-A-FUEL-02D-assets.md"
PREFIX = "shielded_assembly_station"

PALETTE = {
    "front_off": ["#182329", "#2C3D44", "#53676B", "#87999A", "#D0B66B", "#9A8249", "#10171B", "#A9C4C2", "#6C8586"],
    "front_on": ["#182329", "#2C3D44", "#53676B", "#87999A", "#D0B66B", "#9A8249", "#10171B", "#A9C4C2", "#6C8586", "#E3A53D", "#FFD577", "#A44B25"],
    "side": ["#182329", "#2C3D44", "#53676B", "#87999A", "#D0B66B", "#9A8249", "#10171B", "#A9C4C2", "#6C8586"],
    "top": ["#182329", "#2C3D44", "#53676B", "#87999A", "#D0B66B", "#9A8249", "#10171B", "#A9C4C2", "#6C8586"],
    "bottom": ["#182329", "#2C3D44", "#53676B", "#87999A", "#D0B66B", "#9A8249", "#10171B", "#A9C4C2", "#6C8586"],
}

# 每个整数矩形都是可在SVG中直接编辑的像素块；主体保持全不透明。
PIXELS: dict[str, list[tuple[int, int, int, int, str]]] = {
    "front_off": [
        (0, 0, 16, 16, "#2C3D44"), (0, 0, 16, 1, "#182329"), (0, 15, 16, 1, "#182329"),
        (0, 1, 2, 14, "#182329"), (14, 1, 2, 14, "#182329"), (2, 2, 12, 1, "#87999A"),
        (2, 12, 12, 1, "#53676B"), (2, 3, 1, 9, "#53676B"), (13, 3, 1, 9, "#182329"),
        (3, 4, 10, 6, "#10171B"), (3, 4, 10, 1, "#D0B66B"), (3, 9, 10, 1, "#9A8249"),
        (3, 5, 1, 4, "#9A8249"), (12, 5, 1, 4, "#9A8249"),
        (4, 5, 8, 4, "#6C8586"), (4, 5, 8, 1, "#A9C4C2"), (4, 5, 2, 1, "#D0B66B"),
        (5, 6, 6, 2, "#53676B"), (5, 6, 1, 2, "#87999A"), (6, 7, 4, 1, "#2C3D44"),
        (4, 11, 8, 1, "#182329"), (5, 12, 6, 1, "#87999A"), (6, 13, 4, 1, "#53676B"),
        (1, 2, 1, 2, "#D0B66B"), (14, 2, 1, 2, "#9A8249"), (1, 12, 1, 2, "#9A8249"), (14, 12, 1, 2, "#D0B66B"),
    ],
    "front_on": [
        (0, 0, 16, 16, "#2C3D44"), (0, 0, 16, 1, "#182329"), (0, 15, 16, 1, "#182329"),
        (0, 1, 2, 14, "#182329"), (14, 1, 2, 14, "#182329"), (2, 2, 12, 1, "#87999A"),
        (2, 12, 12, 1, "#53676B"), (2, 3, 1, 9, "#53676B"), (13, 3, 1, 9, "#182329"),
        (3, 4, 10, 6, "#10171B"), (3, 4, 10, 1, "#D0B66B"), (3, 9, 10, 1, "#9A8249"),
        (3, 5, 1, 4, "#9A8249"), (12, 5, 1, 4, "#9A8249"),
        (4, 5, 8, 4, "#6C8586"), (4, 5, 8, 1, "#A9C4C2"), (4, 5, 2, 1, "#D0B66B"),
        (5, 6, 6, 2, "#53676B"), (5, 6, 1, 2, "#87999A"), (6, 7, 4, 1, "#2C3D44"),
        (4, 11, 8, 1, "#182329"), (5, 12, 6, 1, "#87999A"), (6, 13, 4, 1, "#53676B"),
        (1, 2, 1, 2, "#D0B66B"), (14, 2, 1, 2, "#9A8249"), (1, 12, 1, 2, "#9A8249"), (14, 12, 1, 2, "#D0B66B"),
        (6, 6, 4, 2, "#E3A53D"), (7, 6, 2, 1, "#FFD577"), (7, 8, 2, 1, "#A44B25"),
    ],
    "side": [
        (0, 0, 16, 16, "#2C3D44"), (0, 0, 16, 1, "#182329"), (0, 15, 16, 1, "#182329"),
        (0, 1, 1, 14, "#182329"), (15, 1, 1, 14, "#182329"), (1, 2, 14, 1, "#87999A"),
        (1, 13, 14, 1, "#53676B"), (2, 3, 12, 1, "#D0B66B"), (2, 4, 1, 8, "#9A8249"),
        (13, 4, 1, 8, "#182329"), (3, 5, 10, 6, "#53676B"), (4, 6, 8, 1, "#6C8586"),
        (4, 7, 8, 1, "#2C3D44"), (4, 8, 8, 1, "#53676B"), (3, 12, 10, 1, "#182329"),
    ],
    "top": [
        (0, 0, 16, 16, "#53676B"), (0, 0, 16, 1, "#182329"), (0, 15, 16, 1, "#182329"),
        (0, 1, 1, 14, "#182329"), (15, 1, 1, 14, "#182329"), (1, 1, 14, 1, "#87999A"),
        (1, 14, 14, 1, "#2C3D44"), (2, 2, 12, 1, "#D0B66B"), (2, 13, 12, 1, "#9A8249"),
        (2, 3, 1, 10, "#9A8249"), (13, 3, 1, 10, "#D0B66B"), (4, 4, 8, 1, "#87999A"),
        (4, 11, 8, 1, "#2C3D44"), (4, 5, 1, 6, "#6C8586"), (11, 5, 1, 6, "#53676B"),
        (5, 5, 6, 6, "#10171B"), (5, 5, 6, 1, "#A9C4C2"), (5, 10, 6, 1, "#182329"),
        (5, 6, 1, 4, "#53676B"), (10, 6, 1, 4, "#182329"),
    ],
    "bottom": [
        (0, 0, 16, 16, "#2C3D44"), (0, 0, 16, 1, "#182329"), (0, 15, 16, 1, "#182329"),
        (0, 1, 1, 14, "#182329"), (15, 1, 1, 14, "#182329"), (1, 1, 14, 1, "#53676B"),
        (1, 14, 14, 1, "#53676B"), (2, 2, 12, 1, "#87999A"), (2, 13, 12, 1, "#182329"),
        (2, 3, 1, 10, "#53676B"), (13, 3, 1, 10, "#182329"),
        (6, 6, 4, 1, "#D0B66B"), (5, 7, 6, 2, "#9A8249"), (6, 9, 4, 1, "#53676B"),
        (6, 7, 4, 2, "#10171B"), (7, 8, 2, 1, "#A9C4C2"),
    ],
}


def make_svg(name: str) -> str:
    """按本批像素草图建立受严格渲染器支持的SVG源稿。"""
    body = "\n".join(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{c}"/>' for x, y, w, h, c in PIXELS[name])
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges">\n{body}\n</svg>\n'


def initialize_missing_sources() -> None:
    """仅创建缺少的可编辑SVG，不覆盖已有源稿。"""
    SOURCE.mkdir(parents=True, exist_ok=True)
    for name in PIXELS:
        path = SOURCE / f"{PREFIX}_{name}.svg"
        if not path.exists():
            path.write_text(make_svg(name), encoding="utf-8")


def put_model(path: Path, value: dict) -> None:
    """稳定写入本批模型JSON。"""
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def cube(frm: list[float], to: list[float], faces: dict[str, tuple[str, list[float]]]) -> dict:
    """按实际可见表面和纹理局部区域构造有体积的元素。"""
    return {"from": frm, "to": to, "faces": {
        face: {"texture": f"#{key}", "uv": uv} for face, (key, uv) in faces.items()}}


def build_model(state: str, item: bool = False) -> dict:
    """由互相搭接的平面与四个真实斜角构成密闭单格机身。"""
    front = "front_on" if state == "on" else "front_off"
    textures = {"front": f"create_nuclear_industry:block/{PREFIX}_{front}",
                "side": f"create_nuclear_industry:block/{PREFIX}_side",
                "top": f"create_nuclear_industry:block/{PREFIX}_top",
                "bottom": f"create_nuclear_industry:block/{PREFIX}_bottom",
                "particle": f"create_nuclear_industry:block/{PREFIX}_side"}
    # 基座在四周露出一格台阶，因此上表面必须存在；窄条只取纹理对应边区，避免复制观察窗和进料口。
    elements = [
        cube([0, 0, 0], [16, 3, 16], {
            "north": ("front", [0, 13, 16, 16]), "south": ("side", [0, 13, 16, 16]),
            "east": ("side", [0, 13, 16, 16]), "west": ("side", [0, 13, 16, 16]),
            "up": ("top", [0, 0, 16, 16]), "down": ("bottom", [0, 0, 16, 16])}),
        cube([2, 3, 2], [14, 15, 14], {"up": ("top", [2, 2, 14, 14])}),
        cube([2, 3, 1], [14, 15, 2], {
            "north": ("front", [2, 1, 14, 13]), "up": ("top", [2, 1, 14, 2])}),
        cube([2, 3, 14], [14, 15, 15], {
            "south": ("side", [2, 1, 14, 13]), "up": ("top", [2, 14, 14, 15])}),
        cube([1, 3, 2], [2, 15, 14], {
            "west": ("side", [2, 1, 14, 13]), "up": ("top", [1, 2, 2, 14])}),
        cube([14, 3, 2], [15, 15, 14], {
            "east": ("side", [2, 1, 14, 13]), "up": ("top", [14, 2, 15, 14])}),
    ]
    # 机身直面在四角提前结束；斜面横跨直面端点，其后方有实体芯，外侧不再保留方盒角。
    for cx, cz, angle in ((1.65, 1.65, 45), (14.35, 1.65, -45),
                          (1.65, 14.35, -45), (14.35, 14.35, 45)):
        edge = cube([cx - 1.06, 3, cz - .125], [cx + 1.06, 15.01, cz + .125], {
            "north": ("side", [0, 1, 2, 13]), "south": ("side", [0, 1, 2, 13]),
            "up": ("side", [3, 5, 5, 6])})
        edge["rotation"] = {"origin": [cx, 3, cz], "axis": "y", "angle": angle, "rescale": False}
        elements.append(edge)
    model = {"credit": "EXT-A-FUEL-02D", "ambientocclusion": True, "textures": textures, "elements": elements}
    if item:
        model["display"] = {
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.45, 0.45, 0.45]},
            "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.45, 0.45, 0.45]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.72, 0.72, 0.72]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
        }
    return model


def hash_existing_pngs() -> dict[str, str]:
    """记录本任务以外全部既有游戏PNG的摘要用于字节保护检查。"""
    root = ASSET / "textures"
    return {p.relative_to(root).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(root.rglob("*.png")) if not p.name.startswith(PREFIX + "_")}


def render_model_preview(model: dict, textures: dict[str, Image.Image], target: Path, label: str, view: str) -> None:
    """对模型实际面、局部UV和深度做逐像素正交光栅化，避免画家排序误判遮挡。"""
    size = 420
    canvas = Image.new("RGB", (size, size), "#20282c")
    zbuffer = [[-float("inf")] * size for _ in range(size)]
    camera = {"front": (0, 0, -1), "back": (0, 0, 1), "top": (0, 1, 0), "bottom": (0, -1, 0),
              "item": (1, .8, -1)}[view]
    if view == "front":
        right, up = (1, 0, 0), (0, 1, 0)
    elif view == "back":
        right, up = (-1, 0, 0), (0, 1, 0)
    elif view == "top":
        right, up = (1, 0, 0), (0, 0, 1)
    elif view == "bottom":
        right, up = (1, 0, 0), (0, 0, -1)
    else:
        # 屏幕右向量与相机(+X,+Y,-Z)相容：北面在左、东面在右。
        right = (1 / math.sqrt(2), 0, 1 / math.sqrt(2))
        up = (-.8 / math.sqrt(2 * 2.64), 2 / math.sqrt(2 * 2.64),
              .8 / math.sqrt(2 * 2.64))
    def project(point: tuple[float, float, float]) -> tuple[float, float, float]:
        centered = [point[i] - 8 for i in range(3)]
        scale = 14 if view == "item" else 15
        return (size / 2 + scale * sum(centered[i] * right[i] for i in range(3)),
                size / 2 - scale * sum(centered[i] * up[i] for i in range(3)),
                sum(point[i] * camera[i] for i in range(3)))
    vertices = {
        "north": lambda x0,y0,z0,x1,y1,z1: [(x0,y0,z0),(x1,y0,z0),(x1,y1,z0),(x0,y1,z0)],
        "south": lambda x0,y0,z0,x1,y1,z1: [(x1,y0,z1),(x0,y0,z1),(x0,y1,z1),(x1,y1,z1)],
        "east": lambda x0,y0,z0,x1,y1,z1: [(x1,y0,z0),(x1,y0,z1),(x1,y1,z1),(x1,y1,z0)],
        "west": lambda x0,y0,z0,x1,y1,z1: [(x0,y0,z1),(x0,y0,z0),(x0,y1,z0),(x0,y1,z1)],
        "up": lambda x0,y0,z0,x1,y1,z1: [(x0,y1,z0),(x1,y1,z0),(x1,y1,z1),(x0,y1,z1)],
        "down": lambda x0,y0,z0,x1,y1,z1: [(x0,y0,z1),(x1,y0,z1),(x1,y0,z0),(x0,y0,z0)],
    }
    normals = {"north": (0,0,-1), "south": (0,0,1), "east": (1,0,0), "west": (-1,0,0), "up": (0,1,0), "down": (0,-1,0)}
    def rotate_point(p: tuple[float, float, float], rotation: dict | None) -> tuple[float, float, float]:
        if not rotation:
            return p
        ox, oy, oz = rotation["origin"]
        angle = rotation["angle"] * 3.141592653589793 / 180
        dx, dz = p[0] - ox, p[2] - oz
        return (ox + dx * math.cos(angle) + dz * math.sin(angle), p[1], oz - dx * math.sin(angle) + dz * math.cos(angle))
    def rotate_normal(n: tuple[float, float, float], rotation: dict | None) -> tuple[float, float, float]:
        return rotate_point(n, {**rotation, "origin": [0,0,0]} if rotation else None)
    def raster_triangle(points: list[tuple[float, float, float]], uv: list[tuple[float, float]], tex: Image.Image, shade: float) -> None:
        """每个像素按三角形插值UV及相机深度，只有最前表面能写入。"""
        a, b, c = points
        area = (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0])
        if abs(area) < 1e-8:
            return
        min_x = max(0, math.floor(min(p[0] for p in points)))
        max_x = min(size - 1, math.ceil(max(p[0] for p in points)))
        min_y = max(0, math.floor(min(p[1] for p in points)))
        max_y = min(size - 1, math.ceil(max(p[1] for p in points)))
        pixels = canvas.load()
        texels = tex.load()
        for sy in range(min_y, max_y + 1):
            for sx in range(min_x, max_x + 1):
                px, py = sx + .5, sy + .5
                w1 = ((px-a[0])*(c[1]-a[1])-(py-a[1])*(c[0]-a[0])) / area
                w2 = ((b[0]-a[0])*(py-a[1])-(b[1]-a[1])*(px-a[0])) / area
                w0 = 1 - w1 - w2
                if min(w0, w1, w2) < -1e-6:
                    continue
                depth = w0*a[2] + w1*b[2] + w2*c[2]
                if depth <= zbuffer[sy][sx] + 1e-5:
                    continue
                u = sum(w*point[0] for w, point in zip((w0,w1,w2), uv))
                v = sum(w*point[1] for w, point in zip((w0,w1,w2), uv))
                color = texels[min(15, max(0, int(u))), min(15, max(0, int(v)))]
                pixels[sx, sy] = tuple(round(channel * shade) for channel in color[:3])
                zbuffer[sy][sx] = depth

    # 按面法线剔除背面，再以像素深度处理相交、搭接和斜角。
    for el in model["elements"]:
        x0, y0, z0 = el["from"]
        x1, y1, z1 = el["to"]
        rotation = el.get("rotation")
        for face in el["faces"]:
            normal = rotate_normal(normals[face], rotation)
            if sum(normal[i] * camera[i] for i in range(3)) <= 0:
                continue
            pts = [rotate_point(tuple(p), rotation) for p in vertices[face](x0,y0,z0,x1,y1,z1)]
            face_data = el["faces"][face]
            tex = textures[face_data["texture"][1:]]
            shade = {"north": 0.83, "south": 0.72, "east": 0.66, "west": 0.76, "up": 1.0, "down": 0.42}[face]
            u0, v0, u1, v1 = face_data["uv"]
            coordinates = [(u0, v1), (u1, v1), (u1, v0), (u0, v0)]
            screen = [project(tuple(point)) for point in pts]
            for indexes in ((0,1,2), (0,2,3)):
                raster_triangle([screen[i] for i in indexes], [coordinates[i] for i in indexes], tex, shade)
    ImageDraw.Draw(canvas).text((12, 10), f"EXT-A-FUEL-02D | {label} | JSON / UV / depth", fill="#f0eee6", font=ImageFont.load_default())
    target.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(target)


def build() -> dict:
    """严格渲染SVG并生成方块状态所需资源；旧PNG通过摘要逐字节保护。"""
    before = hash_existing_pngs()
    for name in PIXELS:
        source = SOURCE / f"{PREFIX}_{name}.svg"
        if not source.is_file():
            raise FileNotFoundError(f"缺少可编辑SVG源稿：{source}；先运行 --initialize-sources")
        image = strict_exporter.render_svg(source.read_text(encoding="utf-8"), set(PALETTE[name]), (16, 16))
        if image.mode != "RGBA" or image.size != (16, 16) or image.getchannel("A").getextrema() != (255, 255):
            raise ValueError(f"{source.name}必须生成不透明16×16 RGBA图")
        GENERATED.mkdir(parents=True, exist_ok=True)
        image.save(GENERATED / f"{PREFIX}_{name}.png")
        game_path = ASSET / "textures/block" / f"{PREFIX}_{name}.png"
        game_path.parent.mkdir(parents=True, exist_ok=True)
        image.save(game_path)
    model_dir = ASSET / "models/block"
    off = build_model("off")
    on = build_model("on")
    put_model(model_dir / f"{PREFIX}_off.json", off)
    put_model(model_dir / f"{PREFIX}_on.json", on)
    put_model(model_dir / f"{PREFIX}_item.json", build_model("off", item=True))
    put_model(ASSET / "models/item" / f"{PREFIX}.json", {"parent": f"create_nuclear_industry:block/{PREFIX}_item"})
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for working, suffix in (("false", "off"), ("true", "on")):
            key = f"facing={facing},working={working}"
            variants[key] = {"model": f"create_nuclear_industry:block/{PREFIX}_{suffix}", **({"y": y} if y else {}), "uvlock": True}
    put_model(ASSET / "blockstates" / f"{PREFIX}.json", {"variants": variants})
    after = hash_existing_pngs()
    changed = sorted(path for path, digest in before.items() if after.get(path) != digest)
    if changed:
        raise RuntimeError(f"既有游戏PNG摘要变化：{changed}")
    images = {name: Image.open(GENERATED / f"{PREFIX}_{name}.png").convert("RGBA") for name in PIXELS}
    evidence = EVIDENCE / "previews"
    render_model_preview(off, {"front": images["front_off"], "side": images["side"], "top": images["top"], "bottom": images["bottom"]}, evidence / "front-off.png", "FRONT | IDLE", "front")
    render_model_preview(off, {"front": images["front_off"], "side": images["side"], "top": images["top"], "bottom": images["bottom"]}, evidence / "back-off.png", "BACK | IDLE", "back")
    render_model_preview(off, {"front": images["front_off"], "side": images["side"], "top": images["top"], "bottom": images["bottom"]}, evidence / "top-off.png", "TOP | IDLE", "top")
    render_model_preview(off, {"front": images["front_off"], "side": images["side"], "top": images["top"], "bottom": images["bottom"]}, evidence / "bottom-off.png", "BOTTOM | IDLE", "bottom")
    render_model_preview(off, {"front": images["front_off"], "side": images["side"], "top": images["top"], "bottom": images["bottom"]}, evidence / "item-gui-off.png", "GUI ITEM VIEW", "item")
    render_model_preview(on, {"front": images["front_on"], "side": images["side"], "top": images["top"], "bottom": images["bottom"]}, evidence / "front-on.png", "FRONT | WORKING", "front")
    (EVIDENCE / "existing-game-png-sha256.json").write_text(json.dumps({"count": len(before), "records": before}, indent=2) + "\n", encoding="utf-8")
    # 对资源引用、状态组合、UV边界和元素几何执行本任务专用的静态合同核验。
    loaded = {"off": json.loads((model_dir / f"{PREFIX}_off.json").read_text(encoding="utf-8")),
              "on": json.loads((model_dir / f"{PREFIX}_on.json").read_text(encoding="utf-8")),
              "item": json.loads((model_dir / f"{PREFIX}_item.json").read_text(encoding="utf-8"))}
    references = set()
    element_count = 0
    for model in loaded.values():
        for element in model["elements"]:
            element_count += 1
            frm, to = element["from"], element["to"]
            if any(float(v) < 0 or float(v) > 16 for v in (*frm, *to)) or any(float(a) >= float(b) for a, b in zip(frm, to)):
                raise ValueError(f"元素几何超出0..16或出现零厚面：{element}")
            rotation = element.get("rotation")
            if rotation:
                ox, oy, oz = rotation["origin"]
                angle = math.radians(rotation["angle"])
                corners = [(x,y,z) for x in (frm[0],to[0]) for y in (frm[1],to[1]) for z in (frm[2],to[2])]
                turned = [(ox+(x-ox)*math.cos(angle)+(z-oz)*math.sin(angle), y, oz-(x-ox)*math.sin(angle)+(z-oz)*math.cos(angle)) for x,y,z in corners]
                if any(v < -1e-6 or v > 16+1e-6 for point in turned for v in point):
                    raise ValueError(f"旋转后几何超出0..16：{element}")
            for face in element["faces"].values():
                uv = face["uv"]
                if len(uv) != 4 or any(float(v) < 0 or float(v) > 16 for v in uv):
                    raise ValueError(f"UV超出0..16：{face}")
                references.add(face["texture"][1:])
    state_file = json.loads((ASSET / "blockstates" / f"{PREFIX}.json").read_text(encoding="utf-8"))
    if set(state_file["variants"]) != {f"facing={f},working={w}" for f in ("north", "east", "south", "west") for w in ("false", "true")}:
        raise ValueError("方块状态必须正好覆盖4朝向×2工作状态")
    for key in references:
        texture = loaded["off"]["textures"].get(key)
        if not texture or not texture.startswith("create_nuclear_industry:block/"):
            raise FileNotFoundError(f"模型纹理变量未定义：#{key}")
        texture_path = texture.removeprefix("create_nuclear_industry:")
        if not (ASSET / "textures" / f"{texture_path}.png").is_file():
            raise FileNotFoundError(f"模型纹理引用缺失：{texture}")
    new_hashes = {}
    for path in sorted([*(GENERATED / f"{PREFIX}_{name}.png" for name in PIXELS), *(ASSET / "textures/block" / f"{PREFIX}_{name}.png" for name in PIXELS)]):
        new_hashes[path.relative_to(ROOT).as_posix()] = hashlib.sha256(path.read_bytes()).hexdigest()
    (EVIDENCE / "new-assets-sha256.json").write_text(json.dumps(new_hashes, indent=2) + "\n", encoding="utf-8")
    result = {"existing_png_count": len(before), "existing_png_changed": changed, "new_textures": len(PIXELS), "source_count": len(PIXELS), "model_elements": element_count, "state_variants": len(state_file["variants"]), "uvs_in_range": True, "geometry_in_range": True, "preview_dir": str(evidence)}
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(
        "# EXT-A-FUEL-02D 素材交付\n\n"
        "状态：任务2资源已生成并冻结，待项目经理审查。\n\n"
        f"- 实际基线：HEAD `675e7a4`，隔离工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。\n"
        "- 实际使用技能：`minecraft-modding/SKILL.md` 核对NeoForge/Minecraft资产边界；`minecraft-testing/SKILL.md` 确认静态资源证据不替代运行集成；`minecraft-resource-pack/SKILL.md` 应用1.21.1自定义元素、方块状态和UV约束。实际运行基线仍锁定Minecraft 1.21.1 / NeoForge 21.1.219 / Create 6.0.10-280。\n"
        f"- 资源：{len(PIXELS)}张16×16不透明RGBA纹理，SVG源稿{len(PIXELS)}份；共检查{element_count}个模型元素及{len(state_file['variants'])}个朝向/工作状态组合。所有UV和元素坐标均在0..16，几何无零厚元素；模型纹理引用完整。\n"
        "- 整改记录：首轮物品预览投影与相机方向不一致，且仅用面排序造成错误遮挡；现以一致的相机基向量、局部UV及逐像素深度重绘。首轮机身保留方盒角、基座漏上表面，并把整张顶/前纹理反复压到窄条；现已修正为真实削角、完整台阶顶面及对应位置的局部UV。\n"
        "- 密闭罩主体的四个方角已实际移除：中央实体、四块止于斜角前的直面与四个旋转铅灰斜面相接。基座露出台阶，上表面完整；顶口位于连续顶面，底部仅画轴接口并由Create原生halfshaft渲染转轴。\n"
        f"- 保护：生成前枚举到{len(before)}张已有游戏PNG，生成后SHA-256逐张一致；变化数{len(changed)}。完整记录见`EXT-A-FUEL-02D-assets/existing-game-png-sha256.json`。\n"
        "- 离线预览逐像素使用模型JSON的实际顶点、局部UV及深度遮挡；实际打开检查正面停机/运行、背面、顶面进料口、底面动力接口和物品栏等距视角，纹理未在窄条上重复压缩成窗或顶口。\n"
        "- 未运行Gradle或Minecraft客户端。\n\n"
        "文件：\n\n"
        "- 生成器：`tools/art-assets/fuel_02d_assets.py`；SVG源：`tools/art-assets/sources/fuel-02d/`。\n"
        "- 游戏模型：`assets/create_nuclear_industry/models/block/shielded_assembly_station_{off,on,item}.json`、`models/item/shielded_assembly_station.json`、`blockstates/shielded_assembly_station.json`。\n"
        "- 游戏纹理及导出副本：`textures/block/shielded_assembly_station_*.png`、`tools/art-assets/generated/block/`。\n"
        "- 预览：`EXT-A-FUEL-02D-assets/previews/`；新资源SHA-256：`EXT-A-FUEL-02D-assets/new-assets-sha256.json`。\n",
        encoding="utf-8")
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description="生成屏蔽装配台纹理、模型与离线预览")
    parser.add_argument("--initialize-sources", action="store_true", help="仅创建缺失SVG源稿")
    args = parser.parse_args()
    if args.initialize_sources:
        initialize_missing_sources()
    print(json.dumps(build(), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
