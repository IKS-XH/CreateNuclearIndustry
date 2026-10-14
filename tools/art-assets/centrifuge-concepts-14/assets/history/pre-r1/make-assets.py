"""生成14A离心机三稿，正式资源只读，输出只限本批SVG、JSON和资产证据。

模型坐标单位为1/16格；upper仍使用自身格坐标，rotor使用lower底面原点。
此工具不生成PNG、不安装资源、不调用旧安装器，也不读取世界或运行状态。
"""

from __future__ import annotations

import copy
import hashlib
import json
import math
from pathlib import Path
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
ASSETS = HERE / "assets"
EVIDENCE = ROOT / "build/reports/art/ART-CENTRIFUGE-14A/assets"
MODEL_DIR = ROOT / "src/main/resources/assets/create_nuclear_industry/models/block"
TEXTURE_DIR = ROOT / "src/main/resources/assets/create_nuclear_industry/textures/block"
SLOTS = ("casing", "brass", "glass", "dark")
FACES = ("north", "south", "east", "west", "up", "down")
MODEL_NAMES = {"lower": "enrichment_centrifuge", "upper": "enrichment_centrifuge_upper",
               "rotor": "enrichment_centrifuge_rotor"}
PALETTES = {
    "a": {"steel": "#77848A", "light": "#A3ADB0", "shade": "#59666C", "dark": "#303E46",
          "gold": "#B58C51", "goldLight": "#D4AD6A", "glass": "#52717B", "mark": "#718D96"},
    "b": {"steel": "#4C5961", "light": "#7C898E", "shade": "#3B474E", "dark": "#252F36",
          "gold": "#9C7644", "goldLight": "#C09A58", "glass": "#425F68", "mark": "#6E858B"},
    "c": {"steel": "#A2A09A", "light": "#C0BDB4", "shade": "#777E7F", "dark": "#35444D",
          "gold": "#A7824F", "goldLight": "#C4A368", "glass": "#426B80", "mark": "#658B9B"},
}
NAMES = {"a": "A 精密钢筒", "b": "B 铸钢加强壳", "c": "C 分段检修机壳"}
DESCRIPTIONS = {
    "a": "中亮钢灰长窗、简洁纵向骨架与细金色端环；突出两格钢筒及窗内钢制转子。",
    "b": "深钢灰厚端座、八条宽加强筋与克制金色轴承件；突出铸钢重量和独立转子。",
    "c": "暖浅灰分段检修护板、深钢骨架与上下分区；蓝色窗缘及赭色既有顶部圈辅助辨识。",
}


def sha(path: Path) -> str:
    """返回原始字节SHA-256；来源与输出身份均不依赖文件时间。"""
    return hashlib.sha256(path.read_bytes()).hexdigest()


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def write_json(path: Path, data: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def uv_for(element: dict, face: str) -> list[float]:
    """按1.21.1默认六面投影显式计算UV；壳体各半格保持0..16纹理域。"""
    lo, hi = element["from"], element["to"]
    return {
        "down": [lo[0], 16-hi[2], hi[0], 16-lo[2]],
        "up": [lo[0], lo[2], hi[0], hi[2]],
        "north": [16-hi[0], 16-hi[1], 16-lo[0], 16-lo[1]],
        "south": [lo[0], 16-hi[1], hi[0], 16-lo[1]],
        "west": [lo[2], 16-hi[1], hi[2], 16-lo[1]],
        "east": [16-hi[2], 16-hi[1], 16-lo[2], 16-lo[1]],
    }[face]


def box(lo, hi, material="casing", angle=0, name="") -> dict:
    """构造闭合局部实体；斜向元素只用Minecraft标准±45度Y旋转。"""
    element = {"from": [round(v, 6) for v in lo], "to": [round(v, 6) for v in hi],
               "faces": {face: {"texture": "#"+material} for face in FACES}}
    if angle:
        element["rotation"] = {"origin": [round((lo[i]+hi[i])/2, 6) for i in range(3)],
                               "axis": "y", "angle": angle, "rescale": False}
    if name:
        element["name"] = name
    for face in element["faces"]:
        uv = uv_for(element, face)
        # 斜向端环的未旋转包围盒可略越过格边；平移该面的UV域，不缩放像素密度。
        # 实际几何经Y旋转仍位于八棱外缘，不能裁剪实体来掩盖纹理坐标越界。
        for axis in (0, 1):
            minimum, maximum = min(uv[axis], uv[axis+2]), max(uv[axis], uv[axis+2])
            shift = -minimum if minimum < 0 else 16-maximum if maximum > 16 else 0
            uv[axis] = round(uv[axis]+shift, 6)
            uv[axis+2] = round(uv[axis+2]+shift, 6)
        element["faces"][face]["uv"] = uv
    return element


def panel(angle, y0, y1, width, thickness, material="casing", offset=0, radius=7, name=""):
    """沿八棱面放置闭合板，offset沿切线；轴心固定x=z=8，半径不改变接口。"""
    radians = math.radians(angle)
    x = 8 + math.sin(radians)*radius + math.cos(radians)*offset
    z = 8 - math.cos(radians)*radius + math.sin(radians)*offset
    if angle in (90, 270):
        return box((x-thickness/2, y0, z-width/2), (x+thickness/2, y1, z+width/2), material, name=name)
    rotation = {0: 0, 45: -45, 135: 45, 180: 0, 225: -45, 315: 45}[angle]
    return box((x-width/2, y0, z-thickness/2), (x+width/2, y1, z+thickness/2),
               material, rotation, name)


def shell(style: str, half: str) -> list[dict]:
    """仅重构正式八棱壳的护板/观察孔；保留真实两格、无新增管嘴或功能接口。"""
    bottom, top = (6.04, 16) if half == "lower" else (0, 10.96)
    aperture = {
        "a": ((7.45, 14.65) if half == "lower" else (1.35, 9.5)),
        "b": ((8.5, 13.6) if half == "lower" else (2.4, 8.4)),
        "c": ((8.0, 14.0) if half == "lower" else (2.0, 9.0)),
    }[style]
    low, high = aperture
    thickness = {"a": .38, "b": .64, "c": .46}[style]
    facet = 2*(7+thickness/2)*math.tan(math.pi/8) + .02
    parts = []
    for angle in range(0, 360, 45):
        window = {"a": 2.0, "b": 1.45, "c": 2.7 if angle%90 == 0 else 1.2}[style]
        frame_material = "dark" if style == "c" else "casing"
        parts.extend([panel(angle, bottom, low, facet, thickness, frame_material, name="下沿实壳"),
                      panel(angle, high, top, facet, thickness, frame_material, name="上沿实壳")])
        side_width = (facet-window)/2
        side_offset = (facet+window)/4
        for sign in (-1, 1):
            if style == "c":
                # 检修板上下分段，窄暗缝后由实体深钢背板封闭；不是新增交互门。
                middle = (low+high)/2
                parts.append(panel(angle, low, high, side_width, .25, "dark", sign*side_offset,
                                   radius=6.94, name="检修板深钢背衬"))
                for y0, y1 in ((low, middle-.16), (middle+.16, high)):
                    parts.append(panel(angle, y0, y1, side_width, thickness, "casing", sign*side_offset,
                                       name="分段检修护板"))
            else:
                parts.append(panel(angle, low, high, side_width, thickness, "casing", sign*side_offset,
                                   name="纵向钢骨架"))
            parts.append(panel(angle, low, high, .18 if style == "a" else .24, .16, "glass",
                               sign*(window/2+.02), radius=7.02, name="观察孔染色窄缘"))
        if style == "b":
            # 八筋位于每面边缘，不跨过观察孔；相邻面不会叠画同一条筋。
            parts.append(panel(angle, bottom, top, .68, .9, "casing", offset=2.48,
                               radius=6.96, name="铸钢加强筋"))
        if style == "c":
            # 上下端框各只留一条短钢压条，避免面板密集线条覆盖转子。
            parts.append(panel(angle, bottom+.26, bottom+.78, facet, .2, "casing", radius=7.25,
                               name="检修分区压条"))
    return parts


def model(style: str, half: str, original: dict) -> dict:
    """正式端座和既有孔盖作为基底；三稿只改变承压外观，不安装或改变碰撞。"""
    output = copy.deepcopy(original)
    if half == "rotor":
        # 保留23元素、所有坐标和UV，只有钢叶/金色连杆的材质分区随稿变化。
        for index, element in enumerate(output["elements"]):
            if 1 <= index <= 8:
                for face in element["faces"].values():
                    face["texture"] = "#casing"
            if index == 0:
                for face in element["faces"].values():
                    face["texture"] = "#casing" if style == "a" else "#dark"
    else:
        original_parts = copy.deepcopy(original["elements"][:12] if half == "lower" else original["elements"][48:])
        # 原八棱端板由交叉片构成；保持覆盖和微错层，只将大面积金色承压板改为钢。
        cap_indices = range(2, 6) if half == "lower" else range(0, 4)
        for index in cap_indices:
            for face in original_parts[index]["faces"].values():
                face["texture"] = "#casing"
        if style == "b":
            if half == "lower":
                original_parts[0]["to"][1] = 2.65
                original_parts[1]["from"][1] = 2.65
                original_parts[1]["to"][1] = 4.42
            else:
                original_parts[4]["to"][1] = 14.42
            # 加厚底台与顶肩朝端座内部发展，整机仍止于0..32，不覆盖顶进料孔。
            for element in original_parts:
                for face, definition in element["faces"].items():
                    definition["uv"] = uv_for(element, face)
        output["elements"] = original_parts + shell(style, half)
        # 金色只保留承压端座外沿的窄环、顶口和实际北检修盖，材料与真实配方一致。
        y0, y1 = (5.48, 5.96) if half == "lower" else (11.08, 11.58)
        if style == "b":
            y0, y1 = (5.2, 6.0) if half == "lower" else (11.02, 11.82)
        if style == "c":
            y0, y1 = (5.62, 5.92) if half == "lower" else (11.15, 11.45)
        for angle in range(0, 360, 45):
            output["elements"].append(panel(angle, y0, y1, 6.08, .16, "brass", radius=7.28, name="窄金色端环"))
    output["textures"] = {"particle": "#casing", **{slot: f"create_nuclear_industry:block/centrifuge_concepts_14/{style}/{slot}" for slot in SLOTS}}
    output["credit"] = "Create Nuclear Industry 原模型派生；ART-CENTRIFUGE-14A 仅设计稿"
    return output


def svg_texture(style: str, slot: str) -> str:
    """16²有限色板纯矩形像素SVG；无滤镜/渐变/外部引用，纹理不携带假接口。"""
    palette = PALETTES[style]
    parts = ['<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges">',
             f'<title>{NAMES[style]}：{slot}，原创16×16像素纹理</title>']

    def rect(x, y, w, h, color):
        parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{palette[color]}"/>')

    if slot == "casing":
        rect(0, 0, 16, 16, "steel")
        rect(0, 0, 16, 1, "light"); rect(0, 1, 1, 14, "light")
        rect(0, 15, 16, 1, "shade"); rect(15, 1, 1, 14, "shade")
        if style == "a":
            rect(3, 2, 1, 12, "light"); rect(12, 2, 1, 12, "shade")
            for y in (4, 11):
                rect(7, y, 2, 1, "shade")
        elif style == "b":
            rect(1, 1, 2, 13, "shade"); rect(13, 1, 2, 13, "shade")
            rect(3, 1, 1, 13, "light")
            for x, y in ((5, 3), (10, 11), (8, 6)):
                rect(x, y, 1, 1, "shade")
            for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
                rect(x, y, 2, 2, "dark"); rect(x, y, 1, 1, "light")
        else:
            rect(2, 2, 12, 1, "light"); rect(2, 13, 12, 1, "shade")
            for x, y in ((2, 3), (12, 3), (2, 11), (12, 11)):
                rect(x, y, 2, 2, "shade"); rect(x, y, 1, 1, "light")
            rect(5, 8, 6, 1, "shade")
    elif slot == "brass":
        rect(0, 0, 16, 16, "gold"); rect(0, 0, 16, 2, "goldLight")
        rect(0, 14, 16, 2, "shade"); rect(0, 2, 1, 12, "goldLight")
        rect(15, 2, 1, 12, "shade")
        # 槽复用于端环/连杆/顶口，本纹理只有金属压痕，不把条纹解释为独立流体口。
        for x in (4, 11):
            rect(x, 3, 1, 10, "goldLight" if style == "a" else "shade")
    elif slot == "glass":
        rect(0, 0, 16, 16, "glass"); rect(0, 0, 16, 1, "mark")
        rect(0, 1, 1, 14, "mark"); rect(15, 1, 1, 14, "dark")
        rect(0, 15, 16, 1, "dark")
        # 基线glass槽是孔缘而非整面玻璃；保留实孔，内部转子不会被整片贴图挡住。
        rect(3, 3, 1, 5, "mark"); rect(12, 9, 1, 4, "mark")
    else:
        rect(0, 0, 16, 16, "dark"); rect(0, 0, 16, 1, "shade")
        rect(0, 1, 1, 14, "shade"); rect(15, 1, 1, 14, "dark")
        for y in (5, 10):
            rect(3, y, 10, 1, "shade")
        if style == "c":
            rect(6, 2, 4, 1, "mark")
    parts.append("</svg>")
    return "\n".join(parts) + "\n"


def sources() -> dict[str, str]:
    paths = [MODEL_DIR/(name+".json") for name in MODEL_NAMES.values()]
    paths += [TEXTURE_DIR/("enrichment_centrifuge_"+slot+".png") for slot in SLOTS]
    paths += [ROOT/"src/main/java/com/iksxh/create_nuclear_industry/production"/(name+".java")
              for name in ("CentrifugeRenderer", "CentrifugeBlock", "CentrifugeBlockEntity")]
    paths += [ROOT/"tools/art-assets/centrifuge_01d_preview.py"]
    paths += [MODEL_DIR/"enrichment_centrifuge_item.json",
              ROOT/"src/main/resources/assets/create_nuclear_industry/blockstates/enrichment_centrifuge.json",
              ROOT/"src/main/resources/data/create_nuclear_industry/recipe/mechanical_crafting/enrichment_centrifuge.json"]
    return {relative(path): sha(path) for path in paths}


def transformed_bounds(elements: list[dict]) -> list[list[float]]:
    """以Minecraft元素局部Y旋转求实际角点包络，防止未旋转框误报斜板越界。"""
    points = []
    for element in elements:
        lo, hi = element["from"], element["to"]
        rotation = element.get("rotation", {})
        angle = math.radians(rotation.get("angle", 0))
        origin = rotation.get("origin", [0, 0, 0])
        for x in (lo[0], hi[0]):
            for y in (lo[1], hi[1]):
                for z in (lo[2], hi[2]):
                    dx, dz = x-origin[0], z-origin[2]
                    points.append([origin[0]+dx*math.cos(angle)+dz*math.sin(angle), y,
                                   origin[2]-dx*math.sin(angle)+dz*math.cos(angle)])
    return [[round(min(point[axis] for point in points), 6) for axis in range(3)],
            [round(max(point[axis] for point in points), 6) for axis in range(3)]]


def check(contract: dict, originals: dict, before: dict) -> dict:
    """有限实物检查：SVG/JSON/UV/轴心/来源；不声明GPU、游戏或功能验收。"""
    report = {"schemaVersion": 1, "scope": "14A有限资产检查，不是游戏或浏览器视觉验收",
              "sourcesUnchanged": before == sources(), "concepts": {}, "errors": []}
    for concept in contract["concepts"]:
        ident = concept["id"]
        detail = {"textures": {}, "models": {}}
        for slot, path in concept["textures"].items():
            root = ET.parse(ASSETS/path).getroot()
            valid = all(root.get(key) == value for key, value in
                        (("width", "16"), ("height", "16"), ("viewBox", "0 0 16 16"), ("shape-rendering", "crispEdges")))
            colors = {node.get("fill") for node in root if node.tag.endswith("rect")}
            external = any("href" in key for node in root.iter() for key in node.attrib)
            detail["textures"][slot] = {"dimensions16": valid, "palette": sorted(colors), "externalReference": external}
            if not valid or external or not colors.issubset(set(concept["palette"].values())):
                report["errors"].append(f"{ident}/{slot}: SVG尺寸/引用/色板失败")
        for half, path in concept["models"].items():
            data = json.loads((ASSETS/path).read_text(encoding="utf-8"))
            invalid_uv = []; invalid_elements = []; invalid_slots = []
            for index, element in enumerate(data["elements"]):
                if not all(element["from"][axis] < element["to"][axis] for axis in range(3)):
                    invalid_elements.append(index)
                for face, definition in element["faces"].items():
                    uv = definition.get("uv", [])
                    if len(uv) != 4 or any(not 0 <= value <= 16 for value in uv):
                        invalid_uv.append([index, face, uv])
                    if definition["texture"].removeprefix("#") not in SLOTS:
                        invalid_slots.append([index, face])
            geometry = [{key: value for key, value in element.items() if key != "faces"}
                        for element in data["elements"]]
            original_geometry = [{key: value for key, value in element.items() if key != "faces"}
                                 for element in originals[half]["elements"]]
            rotor_preserved = half != "rotor" or geometry == original_geometry
            bounds = transformed_bounds(data["elements"])
            limit_y = 32 if half == "rotor" else 16
            bounds_valid = all(bounds[0][axis] >= 0 and bounds[1][axis] <= (limit_y if axis == 1 else 16)
                               for axis in range(3))
            detail["models"][half] = {"elementCount": len(data["elements"]), "invalidUV": invalid_uv,
                                      "invalidElements": invalid_elements, "invalidSlots": invalid_slots,
                                      "rotorGeometryPreserved": rotor_preserved,
                                      "transformedBounds": bounds, "withinTwoBlockEnvelope": bounds_valid,
                                      "localY": [min(e["from"][1] for e in data["elements"]),
                                                 max(e["to"][1] for e in data["elements"])]}
            if invalid_uv or invalid_elements or invalid_slots or not rotor_preserved or not bounds_valid:
                report["errors"].append(f"{ident}/{half}: 元素/UV/槽/转子身份失败")
        report["concepts"][ident] = detail
    if not report["sourcesUnchanged"]:
        report["errors"].append("正式来源发生变化")
    report["passed"] = not report["errors"]
    return report


def main() -> None:
    before = sources()
    originals = {half: json.loads((MODEL_DIR/(name+".json")).read_text(encoding="utf-8"))
                 for half, name in MODEL_NAMES.items()}
    contract = {
        "schemaVersion": 1,
        "baseline": {
            "models": {half: {"path": relative(MODEL_DIR/(name+".json")), "sha256": sha(MODEL_DIR/(name+".json")),
                              "elements": len(originals[half]["elements"])} for half, name in MODEL_NAMES.items()},
            "textures": {slot: {"path": relative(TEXTURE_DIR/("enrichment_centrifuge_"+slot+".png")),
                                "sha256": sha(TEXTURE_DIR/("enrichment_centrifuge_"+slot+".png"))} for slot in SLOTS},
            "rotorAxis": {"origin": [8, 16, 8], "direction": [0, 1, 0], "coordinates": "lower-local",
                          "description": "转子23元素坐标以lower底面为原点；Y5..27；实际rotateCentered绕Direction.UP，Y参考点不改变轴线。"},
            "upperOffset": [0, 16, 0],
            "interfaces": [
                {"half": "upper", "face": "up", "position": [8, 16, 8], "role": "矿浆输入/回取",
                 "geometry": "顶帽中央x=7..9,z=7..9现有孔；upper-local"},
                {"half": "lower", "face": "down", "position": [8, 0, 8], "role": "唯一动力输入",
                 "geometry": "底部中心；lower-local；Y轴"},
                {"half": "lower+upper", "faces": ["north", "east", "south", "west"], "role": "清水及两种粉末输出",
                 "geometry": "上下两段全部四水平面均有效，不假定某单独侧口或新增管嘴"},
                {"half": "lower", "face": "north", "position": [8, 3, .5], "role": "停转后轴承检修",
                 "geometry": "默认facing=north既有小盖；随方块facing旋转，不是物料出口"},
            ],
            "window": "正式glass槽为染色窄孔缘；本批保留实际观察孔，不把整机替换成透明壳。",
            "sources": before,
        },
        "concepts": [],
    }
    for style in ("a", "b", "c"):
        directory = ASSETS/style
        directory.mkdir(parents=True, exist_ok=True)
        for slot in SLOTS:
            (directory/(slot+".svg")).write_text(svg_texture(style, slot), encoding="utf-8")
        for half in MODEL_NAMES:
            write_json(directory/(half+".json"), model(style, half, originals[half]))
        contract["concepts"].append({"id": style, "name": NAMES[style], "description": DESCRIPTIONS[style],
                                     "palette": PALETTES[style],
                                     "models": {half: f"{style}/{half}.json" for half in MODEL_NAMES},
                                     "textures": {slot: f"{style}/{slot}.svg" for slot in SLOTS}})
    write_json(ASSETS/"concepts.json", contract)
    report = check(contract, originals, before)
    write_json(EVIDENCE/"checks.json", report)
    manifest = {"generator": {"path": relative(Path(__file__)), "sha256": sha(Path(__file__))},
                "sources": before,
                "outputs": {relative(path): sha(path) for path in sorted(ASSETS.rglob("*")) if path.is_file()}}
    write_json(EVIDENCE/"manifest.json", manifest)
    print(json.dumps({"passed": report["passed"], "sourcesUnchanged": report["sourcesUnchanged"],
                      "textures": 12, "models": 9, "contract": relative(ASSETS/"concepts.json"),
                      "elements": {ident: {half: data["elementCount"] for half, data in detail["models"].items()}
                                   for ident, detail in report["concepts"].items()}, "errors": report["errors"]},
                     ensure_ascii=False, indent=2))
    if not report["passed"]:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
