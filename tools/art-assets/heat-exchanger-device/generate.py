"""确定性生成核换热器方块模型、16×16纹理和审阅预览。"""

from __future__ import annotations

import json
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / "src/main/resources/assets/create_nuclear_industry"
ART = Path(__file__).resolve().parent
GENERATED = ART / "generated"
EVIDENCE = ROOT / "build/reports/extension/EXT-B-EXCHANGER-01A-ART"
BLOCK_MODELS = ASSETS / "models/block/nuclear_heat_exchanger"
TEXTURES = ASSETS / "textures/block"

PALETTE = {
    "steel": (83, 103, 119, 255),
    "steel_light": (123, 145, 159, 255),
    "steel_dark": (43, 59, 72, 255),
    "panel": (38, 53, 64, 255),
    "copper": (191, 103, 57, 255),
    "copper_light": (231, 151, 86, 255),
    "brass": (220, 176, 74, 255),
    "brass_dark": (133, 99, 43, 255),
    "hot": (255, 145, 54, 255),
    "hot_light": (255, 211, 112, 255),
}


def box(name: str, start: tuple[int, int, int], end: tuple[int, int, int], material: str) -> dict:
    """建立Minecraft单位格内的方盒部件，坐标使用0至16。"""
    return {"name": name, "from": list(start), "to": list(end), "material": material}


def chassis_parts() -> list[dict]:
    """外壳由角柱和六面窗框组成，中央开口由后缩实芯封闭。"""
    parts: list[dict] = []
    for x0, x1 in ((0, 2), (14, 16)):
        for z0, z1 in ((0, 2), (14, 16)):
            parts.append(box("corner_post", (x0, 0, z0), (x1, 16, z1), "steel"))
    # 其余十二条棱用分段钢梁封闭，端点与角柱相接但不互相穿插。
    for y0, y1 in ((0, 2), (14, 16)):
        for z0, z1 in ((0, 2), (14, 16)):
            parts.append(box("edge_rail_x", (2, y0, z0), (14, y1, z1), "steel"))
        for x0, x1 in ((0, 2), (14, 16)):
            parts.append(box("edge_rail_z", (x0, y0, 2), (x1, y1, 14), "steel"))

    # 前后窗框；横梁与侧梁相接但不占用同一体积。
    for z0, z1 in ((0, 2), (14, 16)):
        for y0, y1 in ((2, 5), (11, 14)):
            parts.append(box("side_window_rail", (2, y0, z0), (14, y1, z1), "steel_light"))
        for x0, x1 in ((2, 5), (11, 14)):
            parts.append(box("side_window_stile", (x0, 5, z0), (x1, 11, z1), "steel"))

    # 左右窗框。
    for x0, x1 in ((0, 2), (14, 16)):
        for y0, y1 in ((2, 5), (11, 14)):
            parts.append(box("side_window_rail", (x0, y0, 2), (x1, y1, 14), "steel_light"))
        for z0, z1 in ((2, 5), (11, 14)):
            parts.append(box("side_window_stile", (x0, 5, z0), (x1, 11, z1), "steel"))

    # 顶部和底部采用分块压框，顶面中央留作可识别的热交换面。
    for y0, y1 in ((0, 2), (14, 16)):
        for z0, z1 in ((2, 5), (11, 14)):
            parts.append(box("thermal_rim", (2, y0, z0), (14, y1, z1), "steel_light"))
        for x0, x1 in ((2, 5), (11, 14)):
            parts.append(box("thermal_rim", (x0, y0, 5), (x1, y1, 11), "steel"))

    # 黄铜锁扣仅占窗框表面前方的薄层，与钢件共面处只接触、不穿插。
    for x0 in (5, 10):
        for y0 in (5, 10):
            parts.append(box("brass_coupling", (x0, y0, 2), (x0 + 1, y0 + 1, 3), "brass"))
            parts.append(box("brass_coupling", (x0, y0, 13), (x0 + 1, y0 + 1, 14), "brass"))
    for z0 in (5, 10):
        for y0 in (5, 10):
            parts.append(box("brass_coupling", (2, y0, z0), (3, y0 + 1, z0 + 1), "brass"))
            parts.append(box("brass_coupling", (13, y0, z0), (14, y0 + 1, z0 + 1), "brass"))
    return parts


def core_parts() -> list[dict]:
    """独立热芯在窗框后封闭空间，盘管与顶部热面保持可静态拆分。"""
    parts = [box("sealed_core", (4, 4, 4), (12, 12, 12), "panel")]
    # 六面暗色衬板遮住内部，防止模型出现贯通透明缝。
    parts.extend([
        box("core_window_front", (5, 5, 3), (11, 11, 4), "panel"),
        box("core_window_back", (5, 5, 12), (11, 11, 13), "panel"),
        box("core_window_left", (3, 5, 5), (4, 11, 11), "panel"),
        box("core_window_right", (12, 5, 5), (13, 11, 11), "panel"),
        box("core_window_bottom", (5, 3, 5), (11, 4, 11), "panel"),
    ])
    # 盘管为六个视窗中可见的方形回路。各段不相交，避免共面重叠闪烁。
    for axis in ("front", "back"):
        z0, z1 = ((2, 3) if axis == "front" else (13, 14))
        parts.extend([
            box("coil_run", (6, 6, z0), (10, 7, z1), "copper_light"),
            box("coil_run", (6, 9, z0), (10, 10, z1), "copper"),
            box("coil_turn", (6, 7, z0), (7, 9, z1), "copper"),
            box("coil_turn", (9, 7, z0), (10, 9, z1), "copper_light"),
        ])
    for axis in ("left", "right"):
        x0, x1 = ((2, 3) if axis == "left" else (13, 14))
        parts.extend([
            box("coil_run", (x0, 6, 6), (x1, 7, 10), "copper_light"),
            box("coil_run", (x0, 9, 6), (x1, 10, 10), "copper"),
            box("coil_turn", (x0, 7, 6), (x1, 9, 7), "copper"),
            box("coil_turn", (x0, 7, 9), (x1, 9, 10), "copper_light"),
        ])
    for axis in ("bottom",):
        y0, y1 = (2, 3)
        parts.extend([
            box("coil_run", (6, y0, 6), (10, y1, 7), "copper_light"),
            box("coil_run", (6, y0, 9), (10, y1, 10), "copper"),
            box("coil_turn", (6, y0, 7), (7, y1, 9), "copper"),
            box("coil_turn", (9, y0, 7), (10, y1, 9), "copper_light"),
        ])

    # 顶部铺设连续铜色热扩散板；独立状态模型提供热鳍片和提示灯。
    parts.append(box("top_heat_spreader", (5, 13, 5), (11, 14, 11), "copper"))
    return parts


def lit_parts() -> list[dict]:
    """独立激活层，仅切换顶部视觉提示，不重复基础芯体几何。"""
    parts = []
    for z0, z1 in ((6, 7), (8, 9), (10, 11)):
        parts.append(box("top_heat_fin", (6, 14, z0), (10, 15, z1), "hot"))
    parts.append(box("heat_state_lens", (7, 15, 7), (9, 16, 9), "hot_light"))
    return parts


def model_json(parts: list[dict]) -> dict:
    """序列化为无旋转方盒模型，并逐面使用合法0至16 UV。"""
    textures = {"particle": "create_nuclear_industry:block/nuclear_heat_exchanger/steel"}
    for material in PALETTE:
        textures[material] = f"create_nuclear_industry:block/nuclear_heat_exchanger/{material}"
    elements = []
    for part in parts:
        tex = "#" + part["material"]
        faces = {
            face: {"texture": tex, "uv": [0, 0, 16, 16]}
            for face in ("down", "up", "north", "south", "west", "east")
        }
        elements.append({"from": part["from"], "to": part["to"], "faces": faces})
    return {"credit": "Create: Nuclear Industry 美术团队", "ambientocclusion": True,
            "textures": textures, "elements": elements}


def texture_pixels(kind: str) -> Image.Image:
    """绘制不透明16×16纹理；结构纹理不依赖透明层叠。"""
    image = Image.new("RGBA", (16, 16), PALETTE[kind])
    draw = ImageDraw.Draw(image)
    if kind.startswith("steel"):
        draw.line((0, 2, 15, 2), fill=PALETTE["steel_light"], width=1)
        draw.line((0, 13, 15, 13), fill=PALETTE["steel_dark"], width=1)
        for x in (2, 13):
            for y in (4, 11):
                draw.point((x, y), fill=PALETTE["brass"])
        for x in (0, 4, 8, 12):
            draw.point((x, 7), fill=PALETTE["steel_dark"])
    elif kind == "panel":
        draw.rectangle((1, 1, 14, 14), outline=PALETTE["steel_dark"])
        for y in (3, 12):
            draw.line((2, y, 13, y), fill=PALETTE["steel"])
        draw.line((3, 5, 12, 5), fill=PALETTE["copper"])
        draw.line((3, 10, 12, 10), fill=PALETTE["copper_light"])
    elif kind.startswith("copper"):
        draw.rectangle((1, 1, 14, 14), outline=PALETTE["brass_dark"])
        draw.line((3, 4, 12, 4), fill=PALETTE["copper_light"], width=2)
        draw.line((3, 7, 12, 7), fill=PALETTE["copper"], width=2)
        draw.line((3, 10, 12, 10), fill=PALETTE["copper_light"], width=2)
    elif kind.startswith("brass"):
        draw.rectangle((2, 2, 13, 13), outline=PALETTE["brass_dark"], width=2)
        draw.point((4, 4), fill=PALETTE["hot_light"])
        draw.point((11, 11), fill=PALETTE["brass_dark"])
    elif kind.startswith("hot"):
        draw.rectangle((1, 1, 14, 14), outline=PALETTE["copper"], width=2)
        for y in (4, 8, 12):
            draw.line((3, y, 12, y), fill=PALETTE["hot_light"], width=1)
    return image


def blockstate_json() -> dict:
    """使用multipart按facing旋转外壳/热芯，lit仅添加视觉状态片。"""
    rotations = {"north": 0, "east": 90, "south": 180, "west": 270}
    multipart = []
    for facing, y in rotations.items():
        for layer, lit_only in (("shell", False), ("core", False), ("core_lit", True)):
            when = {"facing": facing}
            if lit_only:
                when["lit"] = "true"
            multipart.append({"when": when, "apply": {
                "model": f"create_nuclear_industry:block/nuclear_heat_exchanger/{layer}", "y": y
            }})
    return {"multipart": multipart}


def preview_png(active: bool) -> Image.Image:
    """以正交等距投影绘制可读预览，色彩直接取自方块部件表。"""
    image = Image.new("RGBA", (840, 900), (28, 34, 40, 255))
    draw = ImageDraw.Draw(image)
    origin = (420, 650)
    sx, sy, height = 21, 10, 25

    def project(point: tuple[int, int, int]) -> tuple[int, int]:
        x, y, z = point
        return (origin[0] + (x - z) * sx, origin[1] + (x + z - 16) * sy - y * height)

    parts = chassis_parts() + core_parts() + (lit_parts() if active else [])
    voxels: dict[tuple[int, int, int], str] = {}
    for part in parts:
        x0, y0, z0 = part["from"]
        x1, y1, z1 = part["to"]
        for x in range(x0, x1):
            for y in range(y0, y1):
                for z in range(z0, z1):
                    voxels[(x, y, z)] = part["material"]

    surfaces = []
    faces = (
        ("top", (0, 1, 0), lambda x,y,z: ((x,y+1,z),(x+1,y+1,z),(x+1,y+1,z+1),(x,y+1,z+1))),
        ("front", (0, 0, 1), lambda x,y,z: ((x,y,z+1),(x+1,y,z+1),(x+1,y+1,z+1),(x,y+1,z+1))),
        ("right", (1, 0, 0), lambda x,y,z: ((x+1,y,z),(x+1,y+1,z),(x+1,y+1,z+1),(x+1,y,z+1))),
    )
    # 只绘制朝向观察者的暴露面；其余几何挡住的内部面不会进入预览。
    for (x, y, z), material in voxels.items():
        for face, normal, corners in faces:
            neighbor = (x + normal[0], y + normal[1], z + normal[2])
            if neighbor in voxels:
                continue
            depth = x + y + z
            surfaces.append((depth, [project(p) for p in corners(x, y, z)], PALETTE[material], face))
    for _, polygon, rgba, face in sorted(surfaces, key=lambda entry: entry[0]):
        scale = {"top": 1.12, "front": 0.78, "right": 0.60}[face]
        color = tuple(min(255, int(channel * scale)) for channel in rgba[:3]) + (255,)
        draw.polygon(polygon, fill=color)
    # 标题和视觉状态只出现在审阅板，不进入游戏内模型。
    title = "Heat Exchanger / Lit" if active else "Heat Exchanger / Static"
    draw.text((34, 34), title, fill=(226, 235, 240, 255))
    draw.text((34, 58), "Steel frame / copper coil / brass fittings / top heat face", fill=(174, 193, 204, 255))
    return image


def svg_sources() -> None:
    """输出可编辑的矢量分件稿和方向/枢轴说明图。"""
    (ART / "sources").mkdir(parents=True, exist_ok=True)
    (ART / "sources/device-components.svg").write_text('''<svg xmlns="http://www.w3.org/2000/svg" width="720" height="420" viewBox="0 0 720 420">
<rect width="720" height="420" fill="#202a33"/><g stroke="#1b252c" stroke-width="3">
<g transform="translate(90 105)"><rect width="160" height="190" rx="12" fill="#536777"/><rect x="34" y="38" width="92" height="112" rx="5" fill="#263540" stroke="#7b919f" stroke-width="12"/><path d="M53 68h55v18H53zm0 38h55v18H53" fill="none" stroke="#c06b43" stroke-width="8"/><circle cx="25" cy="28" r="6" fill="#dcb04a"/><circle cx="135" cy="28" r="6" fill="#dcb04a"/><text x="10" y="220" fill="#dce7ec" stroke="none" font-size="18">钢制外壳（独立）</text></g>
<g transform="translate(300 105)"><rect width="160" height="190" rx="12" fill="#263540" stroke="#536777" stroke-width="9"/><path d="M43 58h74v18H43zm0 43h74v18H43" fill="none" stroke="#e79756" stroke-width="10"/><text x="10" y="220" fill="#dce7ec" stroke="none" font-size="18">铜盘管热芯（独立）</text></g>
<g transform="translate(510 105)"><rect width="160" height="190" rx="10" fill="#536777"/><rect x="18" y="18" width="124" height="154" rx="8" fill="#263540" stroke="#7b919f" stroke-width="8"/><path d="M45 57h70v15H45zm0 32h70v15H45zm0 32h70v15H45" fill="none" stroke="#ff9136" stroke-width="9"/><rect x="65" y="13" width="30" height="9" fill="#ffd370"/><text x="15" y="220" fill="#dce7ec" stroke="none" font-size="18">完整静态组装 / lit=true</text></g></g>
<text x="38" y="375" fill="#aebfca" font-size="16">顶部热面朝上；盘管窗位于六面；水平朝向只改变外观转角。</text></svg>''', encoding="utf-8")
    (ART / "sources/pivots-and-assembly.svg").write_text('''<svg xmlns="http://www.w3.org/2000/svg" width="720" height="300" viewBox="0 0 720 300">
<rect width="720" height="300" fill="#202a33"/><g fill="#536777" stroke="#111a20" stroke-width="3"><path d="M170 75h180v150H170z"/><path d="M170 75l80-45 180 0-80 45z"/><path d="M350 75l80-45v150l-80 45z"/></g>
<path d="M208 158h96" stroke="#c06b43" stroke-width="14"/><path d="M208 190h96" stroke="#e79756" stroke-width="14"/><path d="M255 110v-44" stroke="#ffd370" stroke-width="11"/>
<g fill="#e0e8ec" font-family="sans-serif" font-size="17"><text x="32" y="35">坐标原点：方块西北下角 (0,0,0)</text><text x="460" y="85">X→东，Y→上，Z→南</text><text x="460" y="118">外壳：静态承力层</text><text x="460" y="150">热芯：铜盘管独立模型</text><text x="460" y="182">热面：Y=14..16</text><text x="460" y="214">旋转枢轴：方块中心 (8,8,8)</text><text x="460" y="246">旋转只绕Y轴；坐标与UV均在0..16</text></g></svg>''', encoding="utf-8")


def validate_parts_and_resources() -> dict:
    """检查方块坐标、UV、接缝封闭、部件相交与模型纹理引用。"""
    parts = chassis_parts() + core_parts() + lit_parts()
    for part in parts:
        assert all(0 <= n <= 16 for n in part["from"] + part["to"]), part
        assert all(part["from"][axis] < part["to"][axis] for axis in range(3)), part
    for index, left in enumerate(parts):
        for right in parts[index + 1:]:
            assert not all(max(left["from"][axis], right["from"][axis]) <
                           min(left["to"][axis], right["to"][axis]) for axis in range(3)), (left, right)

    occupied = set()
    for part in chassis_parts() + core_parts():
        for x in range(part["from"][0], part["to"][0]):
            for y in range(part["from"][1], part["to"][1]):
                for z in range(part["from"][2], part["to"][2]):
                    occupied.add((x, y, z))
    # 六个方向逐条检查穿过模型的轴向视线均能遇到实体，杜绝贯通空洞。
    for axis in range(3):
        other = [value for value in range(3) if value != axis]
        for a in range(16):
            for b in range(16):
                line = any(tuple((point if i == axis else a if i == other[0] else b)
                                 for i in range(3)) in occupied for point in range(16))
                assert line, (axis, a, b)

    texture_references = set()
    checked_elements = 0
    for path in BLOCK_MODELS.glob("*.json"):
        data = json.loads(path.read_text(encoding="utf-8"))
        for key, reference in data["textures"].items():
            assert (ASSETS / "textures" / (reference.split(":", 1)[1] + ".png")).is_file(), (path, key, reference)
            texture_references.add(reference)
        for element in data["elements"]:
            checked_elements += 1
            assert all(0 <= n <= 16 for n in element["from"] + element["to"]), (path, element)
            for face in element["faces"].values():
                assert all(0 <= n <= 16 for n in face["uv"]), (path, face)

    blockstate_path = ASSETS / "blockstates/nuclear_heat_exchanger.json"
    state = json.loads(blockstate_path.read_text(encoding="utf-8"))
    model_references = set()
    for selector in state["multipart"]:
        model = selector["apply"]["model"].split(":", 1)[1]
        assert (ASSETS / "models" / f"{model}.json").is_file(), model
        model_references.add(model)
    item = json.loads((ASSETS / "models/item/nuclear_heat_exchanger.json").read_text(encoding="utf-8"))
    parent = item["parent"].split(":", 1)[1]
    assert (ASSETS / "models" / f"{parent}.json").is_file(), parent
    model_references.add(parent)
    for texture in (TEXTURES / "nuclear_heat_exchanger").glob("*.png"):
        image = Image.open(texture).convert("RGBA")
        assert image.size == (16, 16), (texture, image.size)
        assert image.getchannel("A").getextrema() == (255, 255), texture
    return {
        "coordinate_bounds": [0, 16],
        "uv_bounds": [0, 16],
        "model_element_count_checked": checked_elements,
        "blockstate_models_resolved": sorted(model_references),
        "textures_resolved": sorted(texture_references),
        "texture_dimensions": "10 RGBA PNG, 16x16, fully opaque",
        "overlapping_component_volumes": 0,
        "axis_lines_checked": 3 * 16 * 16,
        "all_axis_lines_intersect_solid": True,
        "runtime_or_gradle_checks": "not run; art-only task"
    }


def main() -> None:
    GENERATED.mkdir(parents=True, exist_ok=True)
    BLOCK_MODELS.mkdir(parents=True, exist_ok=True)
    texture_dir = TEXTURES / "nuclear_heat_exchanger"
    texture_dir.mkdir(parents=True, exist_ok=True)
    for name in PALETTE:
        target = texture_dir / f"{name}.png"
        texture_pixels(name).save(target)
        texture_pixels(name).save(GENERATED / f"{name}.png")

    for name, parts in (("shell", chassis_parts()), ("core", core_parts()), ("core_lit", lit_parts())):
        payload = model_json(parts)
        (BLOCK_MODELS / f"{name}.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        (GENERATED / f"{name}.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    blockstate = blockstate_json()
    (ASSETS / "blockstates/nuclear_heat_exchanger.json").write_text(json.dumps(blockstate, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    item_static = model_json(chassis_parts() + core_parts())
    (BLOCK_MODELS / "item_static.json").write_text(json.dumps(item_static, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (GENERATED / "item_static.json").write_text(json.dumps(item_static, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    item = {
        "parent": "create_nuclear_industry:block/nuclear_heat_exchanger/item_static",
        "display": {
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.32, 0.32, 0.32]},
            "thirdperson_lefthand": {"rotation": [75, 225, 0], "translation": [0, 2.5, 0], "scale": [0.32, 0.32, 0.32]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.42, 0.42, 0.42]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.42, 0.42, 0.42]},
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.55, 0.55, 0.55]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.35, 0.35, 0.35]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.65, 0.65, 0.65]}
        }
    }
    item_dir = ASSETS / "models/item"
    item_dir.mkdir(parents=True, exist_ok=True)
    (item_dir / "nuclear_heat_exchanger.json").write_text(json.dumps(item, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (GENERATED / "blockstate.json").write_text(json.dumps(blockstate, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    svg_sources()
    previews = (("heat-exchanger-preview.png", False), ("heat-exchanger-lit-preview.png", True))
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    for filename, active in previews:
        image = preview_png(active)
        image.save(GENERATED / filename)
        image.save(EVIDENCE / filename)
    validation = validate_parts_and_resources()
    (EVIDENCE / "resource-validation.json").write_text(json.dumps(validation, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"生成完成：{len(PALETTE)}张纹理，3份部件模型，blockstate与物品模型；坐标及UV约束0..16。")


if __name__ == "__main__":
    main()
