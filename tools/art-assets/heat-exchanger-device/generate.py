"""确定性生成核换热器方块模型、纹理与审阅预览。"""

from __future__ import annotations

import json
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / "src/main/resources/assets/create_nuclear_industry"
ART = Path(__file__).resolve().parent
GENERATED = ART / "generated"
EVIDENCE = ROOT / "build/reports/extension/EXT-B-EXCHANGER-01D-ART"
BLOCK_MODELS = ASSETS / "models/block/nuclear_heat_exchanger"
TEXTURES = ASSETS / "textures/block/nuclear_heat_exchanger"

PALETTE = {
    "steel": (77, 86, 89, 255),
    "steel_light": (125, 137, 139, 255),
    "steel_dark": (38, 45, 47, 255),
    "panel": (21, 27, 29, 255),
    "copper": (190, 103, 57, 255),
    "copper_light": (231, 151, 86, 255),
    "brass": (213, 169, 69, 255),
    "brass_dark": (117, 82, 37, 255),
    "hot": (245, 106, 37, 255),
    "hot_light": (255, 208, 107, 255),
    "cold": (41, 137, 190, 255),
    "cold_light": (143, 218, 245, 255),
    "cold_outlet": (20, 31, 35, 255),
    "hot_inlet": (20, 31, 35, 255),
}


def box(name: str, start: tuple[int, int, int], end: tuple[int, int, int], material: str) -> dict:
    """建立单位格内的方盒部件，坐标使用半开区间0至16。"""
    return {"name": name, "from": list(start), "to": list(end), "material": material}


def base_parts() -> list[dict]:
    """生成Y=0..12完整深灰基座，并在北/南面标出定向流体接口。"""
    voxels: dict[tuple[int, int, int], str] = {
        (x, y, z): "steel_dark" if y < 2 else "steel"
        for x in range(16) for y in range(12) for z in range(16)
    }
    # 方向标记只重绘北/南外表面的中央像素，不挖槽、不增加几何厚度。
    for x in range(5, 11):
        for y in range(3, 9):
            voxels[(x, y, 0)] = "cold_face"
            voxels[(x, y, 15)] = "hot_face"

    # 贪心合并同材质体素为尽可能大的长方体，控制方块与物品模型元素数。
    parts: list[dict] = []
    while voxels:
        x, y, z = min(voxels, key=lambda point: (point[1], point[0], point[2]))
        material = voxels[(x, y, z)]
        end_x = x + 1
        while voxels.get((end_x, y, z)) == material:
            end_x += 1
        end_z = z + 1
        while all(voxels.get((xx, y, end_z)) == material for xx in range(x, end_x)):
            end_z += 1
        end_y = y + 1
        while all(voxels.get((xx, end_y, zz)) == material
                  for xx in range(x, end_x) for zz in range(z, end_z)):
            end_y += 1
        parts.append(box("base_directional_body", (x, y, z), (end_x, end_y, end_z), material))
        for xx in range(x, end_x):
            for yy in range(y, end_y):
                for zz in range(z, end_z):
                    del voxels[(xx, yy, zz)]
    return parts


def fin_parts() -> list[dict]:
    """生成沿Z方向排列、槽口真实敞开的七条独立铜鳍片。"""
    return [box("copper_cooling_fin", (x, 12, 2), (x + 1, 16, 14), "copper" if x % 4 else "copper_light")
            for x in (2, 4, 6, 8, 10, 12, 14)]


def lit_parts() -> list[dict]:
    """热态仅在三个鳍片槽内增加短提示条，不覆盖冷态鳍片表面。"""
    return [box("hot_fin_channel", (x, 14, 4), (x + 1, 16, 12), "hot" if x != 9 else "hot_light")
            for x in (5, 9, 13)]


def model_json(parts: list[dict]) -> dict:
    """逐面显式写入合法0至16 UV，供Minecraft模型加载器直接解析。"""
    textures = {"particle": "create_nuclear_industry:block/nuclear_heat_exchanger/steel"}
    textures.update({key: f"create_nuclear_industry:block/nuclear_heat_exchanger/{key}" for key in PALETTE})
    elements = []
    for part in parts:
        texture = "#" + part["material"]
        faces = {face: {"texture": texture, "uv": [0, 0, 16, 16]}
                 for face in ("down", "up", "north", "south", "west", "east")}
        if part["material"] in ("cold_face", "hot_face"):
            faces = {face: {"texture": "#steel", "uv": [0, 0, 16, 16]} for face in faces}
            faces["north" if part["material"] == "cold_face" else "south"]["texture"] = "#cold_outlet" if part["material"] == "cold_face" else "#hot_inlet"
        elements.append({"from": part["from"], "to": part["to"], "faces": faces})
    return {"credit": "Create: Nuclear Industry 美术团队", "ambientocclusion": True,
            "textures": textures, "elements": elements}


def texture_pixels(kind: str) -> Image.Image:
    """绘制不透明16×16金属纹理，依靠真实几何呈现凹槽与鳍片。"""
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
    elif kind.startswith("hot") and kind != "hot_inlet":
        draw.rectangle((1, 1, 14, 14), outline=PALETTE["copper"], width=2)
        for y in (4, 8, 12):
            draw.line((3, y, 12, y), fill=PALETTE["hot_light"], width=1)
    elif kind == "cold_outlet":
        draw.rectangle((1, 1, 14, 14), outline=PALETTE["cold"], width=2)
        draw.rectangle((3, 3, 12, 12), fill=PALETTE["panel"])
        draw.line((7, 10, 7, 5), fill=PALETTE["cold_light"], width=2)
        draw.line((8, 10, 8, 5), fill=PALETTE["cold"], width=1)
        draw.polygon(((4, 6), (7, 3), (10, 6)), fill=PALETTE["cold_light"])
    elif kind == "hot_inlet":
        draw.rectangle((1, 1, 14, 14), outline=PALETTE["hot"], width=2)
        draw.rectangle((3, 3, 12, 12), fill=PALETTE["panel"])
        draw.line((7, 5, 7, 10), fill=PALETTE["hot_light"], width=2)
        draw.line((8, 5, 8, 10), fill=PALETTE["hot"], width=1)
        draw.polygon(((4, 9), (7, 12), (10, 9)), fill=PALETTE["hot_light"])
    return image


def blockstate_json() -> dict:
    """按四个水平朝向旋转部件层，lit状态仅叠加不相交的槽内提示条。"""
    rotations = {"north": 0, "east": 90, "south": 180, "west": 270}
    multipart = []
    for facing, y in rotations.items():
        for layer, lit_only in (("base", False), ("fins", False), ("fins_lit", True)):
            when = {"facing": facing}
            if lit_only:
                when["lit"] = "true"
            multipart.append({"when": when, "apply": {
                "model": f"create_nuclear_industry:block/nuclear_heat_exchanger/{layer}", "y": y
            }})
    return {"multipart": multipart}


def preview_png(active: bool) -> Image.Image:
    """按部件实体体素绘制等距预览，槽内空隙来自真实几何。"""
    image = Image.new("RGBA", (840, 900), (28, 34, 40, 255))
    draw = ImageDraw.Draw(image)
    origin = (420, 650)
    sx, sy, height = 21, 10, 25

    def project(point: tuple[int, int, int]) -> tuple[int, int]:
        x, y, z = point
        return (origin[0] + (x - z) * sx, origin[1] + (x + z - 16) * sy - y * height)

    parts = base_parts() + fin_parts() + (lit_parts() if active else [])
    voxels: dict[tuple[int, int, int], str] = {}
    for part in parts:
        x0, y0, z0 = part["from"]
        x1, y1, z1 = part["to"]
        for x in range(x0, x1):
            for y in range(y0, y1):
                for z in range(z0, z1):
                    material = part["material"]
                    voxels[(x, y, z)] = "steel" if material in ("cold_face", "hot_face") else material

    faces = (
        ("top", (0, 1, 0), lambda x, y, z: ((x, y + 1, z), (x + 1, y + 1, z), (x + 1, y + 1, z + 1), (x, y + 1, z + 1))),
        ("front", (0, 0, 1), lambda x, y, z: ((x, y, z + 1), (x + 1, y, z + 1), (x + 1, y + 1, z + 1), (x, y + 1, z + 1))),
        ("right", (1, 0, 0), lambda x, y, z: ((x + 1, y, z), (x + 1, y + 1, z), (x + 1, y + 1, z + 1), (x + 1, y, z + 1))),
    )
    surfaces = []
    for (x, y, z), material in voxels.items():
        for face, normal, corners in faces:
            neighbor = (x + normal[0], y + normal[1], z + normal[2])
            if neighbor not in voxels:
                depth = x + y + z
                surfaces.append((depth, [project(p) for p in corners(x, y, z)], PALETTE[material], face))
    for _, polygon, rgba, face in sorted(surfaces, key=lambda entry: entry[0]):
        scale = {"top": 1.12, "front": 0.78, "right": 0.60}[face]
        color = tuple(min(255, int(channel * scale)) for channel in rgba[:3]) + (255,)
        draw.polygon(polygon, fill=color)
    title = "Heat Exchanger / Hot" if active else "Heat Exchanger / Cold"
    draw.text((34, 34), title, fill=(226, 235, 240, 255))
    draw.text((34, 58), "Steel base / directional inlet and outlet / separated copper fins", fill=(174, 193, 204, 255))
    return image


def directional_preview_png() -> Image.Image:
    """并排展示FACING北侧冷出与背面热入的面板方向和颜色。"""
    image = Image.new("RGBA", (720, 300), (28, 34, 40, 255))
    draw = ImageDraw.Draw(image)
    draw.text((52, 24), "FACING / NORTH: COLD LIQUID OUT", fill=PALETTE["cold"])
    draw.text((402, 24), "REAR / SOUTH: HOT LIQUID IN", fill=PALETTE["hot"])
    cold = texture_pixels("cold_outlet").resize((144, 144), Image.Resampling.NEAREST)
    hot = texture_pixels("hot_inlet").resize((144, 144), Image.Resampling.NEAREST)
    for x, texture, accent in ((126, cold, PALETTE["cold"]), (476, hot, PALETTE["hot"])):
        draw.rectangle((x - 14, 65, x + 158, 237), fill=PALETTE["steel_dark"], outline=PALETTE["steel_light"], width=5)
        image.alpha_composite(texture, (x, 79))
        draw.rectangle((x - 14, 65, x + 158, 237), outline=accent, width=3)
    draw.text((62, 260), "OUTWARD arrow / away from exchanger", fill=(196, 216, 225, 255))
    draw.text((412, 260), "INWARD arrow / into exchanger", fill=(196, 216, 225, 255))
    return image


def svg_sources() -> None:
    """输出与游戏几何一致的封闭基座、定向面板、鳍片及装配矢量稿。"""
    (ART / "sources").mkdir(parents=True, exist_ok=True)
    (ART / "sources/device-components.svg").write_text('''<svg xmlns="http://www.w3.org/2000/svg" width="760" height="420" viewBox="0 0 760 420">
<rect width="760" height="420" fill="#202a33"/>
<g transform="translate(55 55)" stroke="#11191d" stroke-width="4"><path d="M45 110h175v115H45z" fill="#4d5659"/><path d="M45 110l60-38h175l-60 38z" fill="#7d898b"/><path d="M220 110l60-38v115l-60 38z" fill="#263032"/><path d="M107 145h48v48h-48z" fill="#151b1d" stroke="#2989be"/><path d="M131 181v-23m-11 10 11-12 11 12" fill="none" stroke="#8fdaef" stroke-width="5"/><text x="32" y="270" fill="#e0e8ec" stroke="none" font-size="17">北面 FACING：蓝色冷液出口，箭头向外</text></g>
<g transform="translate(395 55)" stroke="#11191d" stroke-width="4"><path d="M45 110h175v115H45z" fill="#4d5659"/><path d="M45 110l60-38h175l-60 38z" fill="#7d898b"/><path d="M220 110l60-38v115l-60 38z" fill="#263032"/><path d="M107 145h48v48h-48z" fill="#151b1d" stroke="#f56a25"/><path d="M131 157v23m-11-10 11 12 11-12" fill="none" stroke="#ffd06b" stroke-width="5"/><text x="32" y="270" fill="#e0e8ec" stroke="none" font-size="17">南面背侧：橙色热液入口，箭头向内</text></g>
<g fill="#c06b39" stroke="#11191d" stroke-width="2"><path d="M310 100h8v105h-8z"/><path d="M323 100h8v105h-8z"/></g><text x="80" y="380" fill="#aebfca" font-size="16">左右侧与底面保持暗钢封闭；上方七条铜鳍片独立分件，坐标范围0..16。</text></svg>''', encoding="utf-8")
    (ART / "sources/pivots-and-assembly.svg").write_text('''<svg xmlns="http://www.w3.org/2000/svg" width="760" height="300" viewBox="0 0 760 300">
<rect width="760" height="300" fill="#202a33"/><g fill="#4d5659" stroke="#11191d" stroke-width="3"><path d="M65 105h180v125H65z"/><path d="M65 105l70-45h180l-70 45z"/><path d="M245 105l70-45v125l-70 45z"/></g>
<g fill="#c06b39" stroke="#11191d" stroke-width="2"><path d="M100 102h9v-38h-9z"/><path d="M122 102h9v-38h-9z"/><path d="M144 102h9v-38h-9z"/><path d="M166 102h9v-38h-9z"/><path d="M188 102h9v-38h-9z"/></g>
<g fill="#e0e8ec" font-family="sans-serif" font-size="16"><text x="370" y="62">基座：Y=0..12，封闭完整覆盖方格</text><text x="370" y="97">鳍片：Y=12..16，沿Z方向平行排列</text><text x="370" y="132">北面冷出口；南面热入口；四朝向整体旋转</text><text x="370" y="167">左右侧与底部不画可接管接口</text><text x="370" y="202">旋转枢轴：方块中心 (8,8,8)</text><text x="370" y="237">热态提示条留在开放鳍片槽内</text></g></svg>''', encoding="utf-8")


def validate_parts_and_resources() -> dict:
    """检查定向面板、体素相交、冷热槽、坐标/UV、资源引用与手持变换。"""
    base, fins, hot = base_parts(), fin_parts(), lit_parts()
    layers = {"base": base, "fins": fins, "fins_lit": hot}
    seen: dict[tuple[int, int, int], tuple[str, str]] = {}
    for layer, parts in layers.items():
        for part in parts:
            assert all(0 <= n <= 16 for n in part["from"] + part["to"]), (layer, part)
            assert all(part["from"][axis] < part["to"][axis] for axis in range(3)), (layer, part)
            for x in range(part["from"][0], part["to"][0]):
                for y in range(part["from"][1], part["to"][1]):
                    for z in range(part["from"][2], part["to"][2]):
                        key = (x, y, z)
                        if layer in ("base", "fins", "fins_lit"):
                            assert key not in seen, (key, seen.get(key), (layer, part["name"]))
                            seen[key] = (layer, part["name"])

    assert all(part["from"][1] == 12 and part["to"][1] == 16 for part in fins)
    assert sum((p["to"][0]-p["from"][0])*(p["to"][1]-p["from"][1])*(p["to"][2]-p["from"][2]) for p in base) == 16*12*16
    assert {p["material"] for p in base} >= {"cold_face", "hot_face"}
    assert all(part["from"][1] == 14 and part["to"][1] == 16 for part in hot)
    fin_x = {x for part in fins for x in range(part["from"][0], part["to"][0])}
    hot_x = {x for part in hot for x in range(part["from"][0], part["to"][0])}
    assert fin_x.isdisjoint(hot_x)
    assert {x for x in range(16) if x not in fin_x}  # 鳍片间保持真实空气槽。

    # 同一法线、同一坐标平面上的面片若有正面积重叠，会造成深度闪烁。
    face_rectangles: dict[tuple[int, int, int], list[tuple[int, int, int, int, str]]] = {}
    for layer, parts in layers.items():
        for part in parts:
            start, end = part["from"], part["to"]
            for axis in range(3):
                tangent = [value for value in range(3) if value != axis]
                rectangle = (start[tangent[0]], end[tangent[0]], start[tangent[1]], end[tangent[1]], part["name"])
                for plane, normal in ((start[axis], -1), (end[axis], 1)):
                    face_rectangles.setdefault((axis, plane, normal), []).append(rectangle)
    coplanar_overlaps = 0
    for rectangles in face_rectangles.values():
        for index, left in enumerate(rectangles):
            for right in rectangles[index + 1:]:
                if min(left[1], right[1]) > max(left[0], right[0]) and min(left[3], right[3]) > max(left[2], right[2]):
                    coplanar_overlaps += 1
    assert coplanar_overlaps == 0, coplanar_overlaps

    texture_references, checked_elements = set(), 0
    for path in BLOCK_MODELS.glob("*.json"):
        data = json.loads(path.read_text(encoding="utf-8"))
        for key, reference in data["textures"].items():
            texture_path = ASSETS / "textures" / (reference.split(":", 1)[1] + ".png")
            assert texture_path.is_file(), (path, key, reference)
            texture_references.add(reference)
        for element in data["elements"]:
            checked_elements += 1
            assert all(0 <= n <= 16 for n in element["from"] + element["to"]), (path, element)
            for face in element["faces"].values():
                assert all(0 <= n <= 16 for n in face["uv"]), (path, face)

    base_model = json.loads((BLOCK_MODELS / "base.json").read_text(encoding="utf-8"))
    cold_faces = [element["faces"] for element in base_model["elements"]
                  if element["faces"]["north"]["texture"] == "#cold_outlet"]
    hot_faces = [element["faces"] for element in base_model["elements"]
                 if element["faces"]["south"]["texture"] == "#hot_inlet"]
    assert cold_faces and hot_faces
    assert all(all(faces[face]["texture"] != "#cold_outlet" for face in ("up", "down", "south", "east", "west"))
               for faces in cold_faces)
    assert all(all(faces[face]["texture"] != "#hot_inlet" for face in ("up", "down", "north", "east", "west"))
               for faces in hot_faces)

    state = json.loads((ASSETS / "blockstates/nuclear_heat_exchanger.json").read_text(encoding="utf-8"))
    model_references = set()
    for selector in state["multipart"]:
        model = selector["apply"]["model"].split(":", 1)[1]
        assert (ASSETS / "models" / f"{model}.json").is_file(), model
        model_references.add(model)
    item = json.loads((ASSETS / "models/item/nuclear_heat_exchanger.json").read_text(encoding="utf-8"))
    parent = item["parent"].split(":", 1)[1]
    assert (ASSETS / "models" / f"{parent}.json").is_file(), parent
    model_references.add(parent)
    for texture in TEXTURES.glob("*.png"):
        image = Image.open(texture).convert("RGBA")
        assert image.size == (16, 16) and image.getchannel("A").getextrema() == (255, 255), texture
    expected_faces = {"north", "east", "south", "west"}
    selectors = state["multipart"]
    assert {entry["when"]["facing"] for entry in selectors} == expected_faces
    expected_rotations = {"north": 0, "east": 90, "south": 180, "west": 270}
    for facing, rotation in expected_rotations.items():
        entries = [entry for entry in selectors if entry["when"]["facing"] == facing]
        assert {entry["apply"]["y"] for entry in entries} == {rotation}
    assert item["display"]["firstperson_righthand"]["scale"] == [0.5, 0.5, 0.5]
    return {
        "model_element_count_checked": checked_elements,
        "blockstate_models_resolved": sorted(model_references),
        "textures_resolved": sorted(texture_references),
        "texture_dimensions": f"{len(list(TEXTURES.glob('*.png')))} RGBA PNG, 16x16, fully opaque",
        "coordinate_and_explicit_uv_bounds": [0, 16],
        "base_y": [0, 12], "fin_y": [12, 16], "fin_count": len(fins),
        "true_fin_grooves": True, "lit_channels_inside_grooves": len(hot),
        "intersecting_component_voxels": 0,
        "same_facing_coplanar_face_overlaps": coplanar_overlaps,
        "directional_interfaces": {"north": "cold outlet, outward arrow", "south": "hot inlet, inward arrow", "east_west_down": "closed steel"},
        "directional_face_texture_assignments": {"north_cold_panel_elements": len(cold_faces), "south_hot_panel_elements": len(hot_faces), "other_faces_use_directional_texture": False},
        "facing_rotations": {"north": 0, "east": 90, "south": 180, "west": 270},
        "handheld_scale": item["display"]["firstperson_righthand"]["scale"],
        "gradle_or_runtime_checks": "not run; art-only task"
    }


def main() -> None:
    GENERATED.mkdir(parents=True, exist_ok=True)
    BLOCK_MODELS.mkdir(parents=True, exist_ok=True)
    TEXTURES.mkdir(parents=True, exist_ok=True)
    for obsolete in ("shell.json", "core.json", "core_lit.json"):
        (BLOCK_MODELS / obsolete).unlink(missing_ok=True)
    for name in PALETTE:
        texture_pixels(name).save(TEXTURES / f"{name}.png")
        texture_pixels(name).save(GENERATED / f"{name}.png")

    model_layers = (("base", base_parts()), ("fins", fin_parts()), ("fins_lit", lit_parts()))
    for name, parts in model_layers:
        payload = model_json(parts)
        (BLOCK_MODELS / f"{name}.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        (GENERATED / f"{name}.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    blockstate = blockstate_json()
    (ASSETS / "blockstates/nuclear_heat_exchanger.json").write_text(json.dumps(blockstate, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    item_static = model_json(base_parts() + fin_parts())
    (BLOCK_MODELS / "item_static.json").write_text(json.dumps(item_static, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (GENERATED / "item_static.json").write_text(json.dumps(item_static, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    item = {
        "parent": "create_nuclear_industry:block/nuclear_heat_exchanger/item_static",
        "display": {
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.4, 0.4, 0.4]},
            "thirdperson_lefthand": {"rotation": [75, 225, 0], "translation": [0, 2.5, 0], "scale": [0.4, 0.4, 0.4]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.65, 0.65, 0.65]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.4, 0.4, 0.4]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.85, 0.85, 0.85]}
        }
    }
    item_path = ASSETS / "models/item/nuclear_heat_exchanger.json"
    item_path.write_text(json.dumps(item, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (GENERATED / "blockstate.json").write_text(json.dumps(blockstate, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    svg_sources()
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    for filename, active in (("heat-exchanger-preview.png", False), ("heat-exchanger-hot-preview.png", True)):
        preview_png(active).save(GENERATED / filename)
        preview_png(active).save(EVIDENCE / filename)
    directional_preview_png().save(GENERATED / "directional-interfaces-preview.png")
    directional_preview_png().save(EVIDENCE / "directional-interfaces-preview.png")
    validation = validate_parts_and_resources()
    (EVIDENCE / "resource-validation.json").write_text(json.dumps(validation, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"生成完成：基座{len(base_parts())}段、{len(fin_parts())}条铜鳍片、{len(lit_parts())}条热态提示；证据写入01D目录。")


if __name__ == "__main__":
    main()
