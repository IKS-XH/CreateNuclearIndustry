"""生成14A离心机三稿，正式资源只读，输出只限本批SVG、JSON和资产证据。

模型坐标单位为1/16格；upper仍使用自身格坐标，rotor使用lower底面原点。
当前默认只生成R3三份lower、独立shaft和契约；上壳/R2转子/18SVG不再次导出。
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
EVIDENCE = ROOT / "build/reports/art/ART-CENTRIFUGE-14A/assets/r3"
R2_FACTOR = 1.28
MODEL_DIR = ROOT / "src/main/resources/assets/create_nuclear_industry/models/block"
TEXTURE_DIR = ROOT / "src/main/resources/assets/create_nuclear_industry/textures/block"
BASE_SLOTS = ("casing", "brass", "glass", "dark")
SLOTS = (*BASE_SLOTS, "machined", "fastener")
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
for style, colors in {"a": ("#87959B", "#B0BCC0"), "b": ("#73838A", "#9AA9AF"),
                      "c": ("#839197", "#9DACB2")}.items():
    PALETTES[style].update({"machined": colors[0], "machinedLight": colors[1]})
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
        # 新密封边占用约0.46单位孔宽；B适度放宽原孔，仍保留深钢厚壳方向。
        window = {"a": 2.2, "b": 1.9, "c": 2.7 if angle%90 == 0 else 1.5}[style]
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
        # 四边钢框突出壳面，内侧四边密封窄缘仍保留实体观察孔，不覆盖转子。
        surface = 7 + thickness/2
        for sign in (-1, 1):
            parts.append(panel(angle, low-.2, high+.2, .38, .2, "machined", sign*(window/2+.04),
                               radius=surface+.075, name="观察孔侧钢框"))
            parts.append(panel(angle, low+.16, high-.16, .14, .12, "glass", sign*(window/2-.16),
                               radius=surface+.025, name="观察孔侧密封边"))
        for y0, y1, seal_y in ((low-.2, low+.2, low+.2), (high-.2, high+.2, high-.34)):
            parts.append(panel(angle, y0, y1, window+.4, .2, "machined", radius=surface+.075,
                               name="观察孔横钢框"))
            parts.append(panel(angle, seal_y, seal_y+.14, window-.2, .12, "glass", radius=surface+.025,
                               name="观察孔横密封边"))
        if style == "b":
            # 八筋位于每面边缘，不跨过观察孔；相邻面不会叠画同一条筋。
            parts.append(panel(angle, bottom, top, .68, .9, "casing", offset=2.48,
                               radius=6.96, name="铸钢加强筋"))
        if style == "c":
            # 上下端框各只留一条短钢压条，避免面板密集线条覆盖转子。
            parts.append(panel(angle, bottom+.26, bottom+.78, facet, .2, "casing", radius=7.25,
                               name="检修分区压条"))
    return parts


def square_ring(outer, inner, y0, y1, material, name):
    """四块钢条围出真实顶孔，所有层的中央x/z=7..9孔域保持空，不加假进料面。"""
    return [box((outer, y0, outer), (16-outer, y1, inner), material, name=name),
            box((outer, y0, 16-inner), (16-outer, y1, 16-outer), material, name=name),
            box((outer, y0, inner), (inner, y1, 16-inner), material, name=name),
            box((16-inner, y0, inner), (16-outer, y1, 16-inner), material, name=name)]


def bolt(x, y, z, front=False):
    """少量实体紧固点采用垫圈/退台头，UV按真实面跨度取样，不拉满一张16²图。"""
    if front:
        washer = box((x-.3, y-.3, z), (x+.3, y+.3, z+.12), "dark", name="检修盖螺栓垫圈")
        head = box((x-.21, y-.21, z-.2), (x+.21, y+.21, z+.01), "fastener", name="检修盖退台螺栓")
    else:
        washer = box((x-.5, y, z-.5), (x+.5, y+.12, z+.5), "dark", name="顶肩螺栓垫圈")
        head = box((x-.34, y+.08, z-.34), (x+.34, y+.37, z+.34), "fastener", name="顶肩退台螺栓")
    for definition in head["faces"].values():
        uv = definition["uv"]
        definition["uv"] = [7, 7, round(7+abs(uv[2]-uv[0]), 6), round(7+abs(uv[3]-uv[1]), 6)]
    return [washer, head]


def maintenance_cover():
    """在原北向检修位置构建搭接框、后退钢片和四紧固点，中心仍是原轴承维修面。"""
    parts = [box((6.12, 2.02, .64), (9.88, 4.08, 1.04), "dark", name="检修盖凹槽背座"),
             box((6.55, 2.43, .5), (9.45, 3.67, .73), "casing", name="检修盖内凹钢片")]
    for lo, hi in (((6.18, 2.06, .34), (6.58, 4.04, .67)),
                   ((9.42, 2.06, .34), (9.82, 4.04, .67)),
                   ((6.58, 2.06, .34), (9.42, 2.46, .67)),
                   ((6.58, 3.64, .34), (9.42, 4.04, .67))):
        parts.append(box(lo, hi, "machined", name="检修盖搭接钢框"))
    parts.append(box((7.28, 2.88, .43), (8.72, 3.22, .56), "brass", name="检修盖窄金属压片"))
    for x in (6.42, 9.58):
        for y in (2.3, 3.8):
            parts.extend(bolt(x, y, .3, front=True))
    return parts


def top_flange(style):
    """铸钢顶肩与加工钢台阶分层，顶孔用小金压环嵌入，整机最高仍为upper本地16。"""
    shoulder_top = 13.35 if style == "b" else 13.15
    parts = [box((3, 12, 3), (13, shoulder_top, 13), "casing", name="铸钢顶肩"),
             box((3.32, shoulder_top, 3.32), (12.68, shoulder_top+.2, 12.68), "dark", name="顶肩退刀槽"),
             box((3.18, shoulder_top+.2, 3.18), (12.82, 14.04, 12.82), "machined", name="顶肩加工台阶")]
    parts += square_ring(4.6, 6.75, 14.04, 14.32, "dark", "顶口下层隔离圈")
    parts += square_ring(4.88, 6.8, 14.32, 15.05, "casing", "顶口钢制凸台")
    parts += square_ring(5.25, 6.85, 15.05, 15.3, "dark", "顶口层间窄槽")
    parts += square_ring(5.08, 7.0, 15.3, 15.84, "machined", "顶口加工钢法兰")
    parts += square_ring(6.25, 7.0, 15.82, 16.0, "brass", "顶孔嵌入窄金压环")
    for x in (4.12, 11.88):
        for z in (4.12, 11.88):
            parts.extend(bolt(x, 14.04, z))
    return parts


def end_details(style, half):
    """沿端座原外缘加加工边与浅槽，金属材料由三层窄带分区而非大面积金板。"""
    if half == "lower":
        ranges = [(4.6, 4.84, "dark", "端座退刀窄槽"),
                  (5.38 if style == "b" else 5.5, 5.74, "brass", "端座嵌入窄金环"),
                  (5.84, 6.04, "machined", "端座上沿加工钢边")]
    else:
        ranges = [(10.96, 11.16, "machined", "上端座下沿加工钢边"),
                  (11.28, 11.48, "brass", "上端座嵌入窄金环"),
                  (11.64, 11.88, "dark", "上端座退刀窄槽")]
    return [panel(angle, y0, y1, 6.08, .14, material, radius=7.32, name=name)
            for angle in range(0, 360, 45) for y0, y1, material, name in ranges]


def model(style: str, half: str, original: dict) -> dict:
    """正式端座和既有孔盖作为基底；三稿只改变承压外观，不安装或改变碰撞。"""
    output = copy.deepcopy(original)
    if half == "rotor":
        # 保留23元素、所有坐标和UV，只有钢叶/金色连杆的材质分区随稿变化。
        for index, element in enumerate(output["elements"]):
            if 1 <= index <= 8:
                for face in element["faces"].values():
                    face["texture"] = "#machined"
            if index == 0:
                for face in element["faces"].values():
                    face["texture"] = "#machined" if style == "a" else "#dark"
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
        if half == "lower":
            # 原检修块由分层组件替换；动力套及其中心坐标保留，不添加第二动力口。
            original_parts = original_parts[:8] + maintenance_cover()
            original_parts += [box((1.8, 2.66 if style == "b" else 2.02, 1.8),
                                   (14.2, 2.88 if style == "b" else 2.24, 14.2), "machined", name="底台加工钢肩")]
        else:
            original_parts = original_parts[:4] + top_flange(style)
        output["elements"] = original_parts + shell(style, half) + end_details(style, half)
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
    elif slot == "machined":
        # 加工面比铸钢温度偏冷、对比更整齐；稀疏横向压痕只表达金属处理。
        rect(0, 0, 16, 16, "machined")
        rect(0, 0, 16, 1, "machinedLight"); rect(0, 1, 1, 14, "machinedLight")
        rect(0, 15, 16, 1, "shade"); rect(15, 1, 1, 14, "shade")
        rect(3, 4, 10, 1, "machinedLight")
        rect(4, 11, 8, 1, "shade")
    elif slot == "fastener":
        rect(0, 0, 16, 16, "shade")
        rect(6, 6, 4, 4, "dark")
        rect(7, 7, 2, 2, "machinedLight")
        rect(7, 8, 2, 1, "machined")
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
    paths += [TEXTURE_DIR/("enrichment_centrifuge_"+slot+".png") for slot in BASE_SLOTS]
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


def make_r1() -> None:
    before = sources()
    originals = {half: json.loads((MODEL_DIR/(name+".json")).read_text(encoding="utf-8"))
                 for half, name in MODEL_NAMES.items()}
    contract = {
        "schemaVersion": 1,
        "baseline": {
            "models": {half: {"path": relative(MODEL_DIR/(name+".json")), "sha256": sha(MODEL_DIR/(name+".json")),
                              "elements": len(originals[half]["elements"])} for half, name in MODEL_NAMES.items()},
            "textures": {slot: {"path": relative(TEXTURE_DIR/("enrichment_centrifuge_"+slot+".png")),
                                "sha256": sha(TEXTURE_DIR/("enrichment_centrifuge_"+slot+".png"))} for slot in BASE_SLOTS},
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
        "revision": "r1",
        "revisionDescription": "分层钢法兰、嵌入小金环、退刀槽/加工边、北盖凹层与实体紧固点、完整钢窗框/窄密封边",
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
                "outputs": {relative(path): sha(path) for path in [ASSETS/"concepts.json",
                            *sorted(path for style in ("a", "b", "c") for path in (ASSETS/style).iterdir())] if path.is_file()}}
    write_json(EVIDENCE/"manifest.json", manifest)
    print(json.dumps({"passed": report["passed"], "sourcesUnchanged": report["sourcesUnchanged"],
                      "textures": 18, "models": 9, "contract": relative(ASSETS/"concepts.json"),
                      "elements": {ident: {half: data["elementCount"] for half, data in detail["models"].items()}
                                   for ident, detail in report["concepts"].items()}, "errors": report["errors"]},
                     ensure_ascii=False, indent=2))
    if not report["passed"]:
        raise SystemExit(1)


def rotor_r2(archived: dict) -> dict:
    """只放大其余22移动件的XZ；细端主轴保原嵌座范围，全部Y/局部角度不动。

    保留单个主轴及23元素分工，不能拆成新元素或用预览group缩放。
    侧面仅重新投影横向U，纵向V取原值；顶/底面按真实横截面重投影。
    """
    output = copy.deepcopy(archived)
    for element in output["elements"][1:]:
        for key in ("from", "to"):
            for axis in (0, 2):
                element[key][axis] = round(8+(element[key][axis]-8)*R2_FACTOR, 8)
        if "rotation" in element:
            for axis in (0, 2):
                origin = element["rotation"]["origin"]
                origin[axis] = round(8+(origin[axis]-8)*R2_FACTOR, 8)
        for face, definition in element["faces"].items():
            projected = uv_for(element, face)
            if face in ("up", "down"):
                definition["uv"] = [round(value, 8) for value in projected]
            else:
                original = definition["uv"]
                definition["uv"] = [round(projected[0], 8), original[1], round(projected[2], 8), original[3]]
    return output


def radial_interval(element: dict) -> tuple[float, float]:
    """求Y旋转矩形横截面的精确径向区间，用整圈旋转并集替代角度抽样。

    矩形是凸连通集，距离轴线的值连续；绕轴360度扫掠恰好为该径向区间的圆环。
    最大半径在角点，最小半径在边段或轴线内部；不把盒状转子伪称连续圆柱。
    """
    lo, hi = element["from"], element["to"]
    rotation = element.get("rotation", {})
    angle = math.radians(rotation.get("angle", 0))
    ox, _, oz = rotation.get("origin", [0, 0, 0])
    points = []
    for x, z in ((lo[0], lo[2]), (hi[0], lo[2]), (hi[0], hi[2]), (lo[0], hi[2])):
        dx, dz = x-ox, z-oz
        points.append((ox+dx*math.cos(angle)+dz*math.sin(angle)-8,
                       oz-dx*math.sin(angle)+dz*math.cos(angle)-8))
    crosses = []
    edge_distances = []
    for index, start in enumerate(points):
        end = points[(index+1)%4]
        dx, dz = end[0]-start[0], end[1]-start[1]
        crosses.append(dx*(-start[1])-dz*(-start[0]))
        t = max(0, min(1, -(start[0]*dx+start[1]*dz)/(dx*dx+dz*dz)))
        edge_distances.append(math.hypot(start[0]+t*dx, start[1]+t*dz))
    inside = all(value >= -1e-10 for value in crosses) or all(value <= 1e-10 for value in crosses)
    return (0 if inside else min(edge_distances), max(math.hypot(x, z) for x, z in points))


def interval_gap(a0, a1, b0, b1):
    return max(0, a0-b1, b0-a1)


def sweep_report(rotor: dict, archived: dict, lower: dict, upper: dict) -> dict:
    """逐Y区间核整圈扫掠与固定壳/窗框/端座，主轴既有嵌座接触单列。

    全角度移动件在每个Y层是圆环并集；固定元素半径区间与其Y区间决定净距。
    报告最小净距是模型实体的距离，不是制造公差、运行性能或玩家碰撞验收。
    """
    static = []
    for half, model_data, offset in (("lower", lower, 0), ("upper", upper, 16)):
        for index, element in enumerate(model_data["elements"]):
            radial = radial_interval(element)
            static.append({"half": half, "index": index, "name": element.get("name", "原端座/底台"),
                           "y": [element["from"][1]+offset, element["to"][1]+offset], "radial": radial})
    report = {"method": "每个Y区间的矩形整圈圆环并集；解析覆盖360度，不是离散角度抽样",
              "unit": "模型单位1/16格", "factorXZForElements1To22": R2_FACTOR,
              "shaftElement0Unchanged": rotor["elements"][0] == archived["elements"][0],
              "elements": [], "shaftExistingContacts": [], "yLayers": [], "errors": []}
    all_minimum = float("inf")
    radial_minimum = float("inf")
    for index, element in enumerate(rotor["elements"]):
        radius0, radius1 = radial_interval(element)
        y0, y1 = element["from"][1], element["to"][1]
        pairs = []
        for fixed in static:
            radial_gap = interval_gap(radius0, radius1, *fixed["radial"])
            y_gap = interval_gap(y0, y1, *fixed["y"])
            distance = math.hypot(radial_gap, y_gap)
            if index == 0 and distance <= 1e-8:
                report["shaftExistingContacts"].append({"half": fixed["half"], "element": fixed["index"],
                                                       "name": fixed["name"],
                                                       "overlapY": [max(y0, fixed["y"][0]), min(y1, fixed["y"][1])]})
            if index > 0 and y_gap == 0:
                radial_minimum = min(radial_minimum, radial_gap)
            pairs.append((distance, fixed))
        distance, nearest = min(pairs, key=lambda pair: pair[0])
        if index > 0:
            all_minimum = min(all_minimum, distance)
            if distance <= 1e-6:
                report["errors"].append(f"移动件{index}与{nearest['half']}元素{nearest['index']}全圈扫掠相交")
        report["elements"].append({"index": index, "y": [y0, y1],
                                   "sweepRadius": [round(radius0, 6), round(radius1, 6)],
                                   "minimumDistance": round(distance, 6),
                                   "nearestStatic": {key: nearest[key] for key in ("half", "index", "name")}})
    levels = sorted({value for element in rotor["elements"][1:] for value in (element["from"][1], element["to"][1])}
                    | {value for fixed in static for value in fixed["y"] if 7 <= value <= 24.65})
    for low, high in zip(levels, levels[1:]):
        active = [index for index, element in enumerate(rotor["elements"]) if index > 0
                  and element["from"][1] <= low and element["to"][1] >= high]
        if not active:
            continue
        candidates = []
        for index in active:
            radial = radial_interval(rotor["elements"][index])
            for fixed in static:
                if fixed["y"][0] <= low and fixed["y"][1] >= high:
                    candidates.append((interval_gap(*radial, *fixed["radial"]), index, fixed))
        if candidates:
            gap, moving, fixed = min(candidates, key=lambda item: item[0])
            report["yLayers"].append({"y": [low, high], "minimumRadialGap": round(gap, 6),
                                      "movingElement": moving, "fixedHalf": fixed["half"],
                                      "fixedElement": fixed["index"], "fixedName": fixed["name"]})
    before_diameter = 2*max(radial_interval(element)[1] for element in archived["elements"])
    after_diameter = 2*max(radial_interval(element)[1] for element in rotor["elements"])
    report.update({"sweepEnvelopeDiameterBefore": round(before_diameter, 6),
                   "sweepEnvelopeDiameterAfter": round(after_diameter, 6),
                   "shaftWidthBeforeAfter": [1.3, 1.3], "eightRodsWidthBeforeAfter": [.76, round(.76*R2_FACTOR, 6)],
                   "bladeWidthBeforeAfter": [.8, round(.8*R2_FACTOR, 6)],
                   "minimumDistanceExcludingUnchangedShaft": round(all_minimum, 6),
                   "minimumRadialGapAtOverlappingYExcludingShaft": round(radial_minimum, 6)})
    report["passed"] = not report["errors"] and report["shaftElement0Unchanged"]
    return report


def make_r2() -> None:
    """R2默认入口：仅三rotor/契约及当前证据可写，六壳/18SVG只读哈希保护。"""
    history = ASSETS/"history/pre-r2"
    evidence_history = ROOT/"build/reports/art/ART-CENTRIFUGE-14A/assets/history/pre-r2"
    protected = json.loads((evidence_history/"protected-shell-textures.json").read_text(encoding="utf-8"))
    before = sources()
    contract = json.loads((history/"concepts.json").read_text(encoding="utf-8"))
    report = {"revision": "r2", "scope": "仅转子/UV/全圈扫掠及来源保护检查", "concepts": {}, "errors": []}
    for style in ("a", "b", "c"):
        archived = json.loads((history/style/"rotor.json").read_text(encoding="utf-8"))
        enlarged = rotor_r2(archived)
        lower = json.loads((ASSETS/style/"lower.json").read_text(encoding="utf-8"))
        upper = json.loads((ASSETS/style/"upper.json").read_text(encoding="utf-8"))
        sweep = sweep_report(enlarged, archived, lower, upper)
        invalid_uv = []; invariant_errors = []
        for index, element in enumerate(enlarged["elements"]):
            previous = archived["elements"][index]
            if [element["from"][1], element["to"][1]] != [previous["from"][1], previous["to"][1]]:
                invariant_errors.append(f"元素{index}Y变化")
            if element.get("rotation", {}).get("angle", 0) != previous.get("rotation", {}).get("angle", 0):
                invariant_errors.append(f"元素{index}局部角度变化")
            for face, definition in element["faces"].items():
                uv = definition["uv"]
                if len(uv) != 4 or any(not 0 <= value <= 16 for value in uv):
                    invalid_uv.append([index, face, uv])
                if face not in ("up", "down") and uv[1::2] != previous["faces"][face]["uv"][1::2]:
                    invariant_errors.append(f"元素{index}/{face}纵向V变化")
            if "rotation" in element:
                origin = element["rotation"]["origin"]
                if origin[1] != previous["rotation"]["origin"][1]:
                    invariant_errors.append(f"元素{index}旋转原点Y变化")
                for axis in (0, 2):
                    expected = 8+(previous["rotation"]["origin"][axis]-8)*R2_FACTOR
                    if abs(origin[axis]-expected) > 1e-7:
                        invariant_errors.append(f"元素{index}旋转原点XZ未随主体放大")
        bounds = transformed_bounds(enlarged["elements"])
        if any(bounds[0][axis] < 0 or bounds[1][axis] > (32 if axis == 1 else 16) for axis in range(3)):
            invariant_errors.append("转子实际几何超出两格包络")
        if len(enlarged["elements"]) != 23:
            invariant_errors.append("23元素分工变化")
        report["concepts"][style] = {"elementCount": len(enlarged["elements"]), "invalidUV": invalid_uv,
                                      "invariantErrors": invariant_errors, "bounds": bounds,
                                      "sweep": sweep}
        if invalid_uv or invariant_errors or not sweep["passed"]:
            report["errors"].append(f"{style}转子UV/Y/整圈扫掠失败")
        write_json(ASSETS/style/"rotor.json", enlarged)
    contract["revision"] = "r2"
    contract["revisionDescription"] = "主体及八杆/叶片/连接件横向1.28倍，23元素/Y/竖轴保持；细主轴原1.3宽保留既有嵌座范围"
    contract["rotorSizing"] = {"unit": "模型单位1/16格", "xzScaleElements1To22": R2_FACTOR,
                                "shaftElement0Width": 1.3, "elementCount": 23,
                                "shaftReason": "原细轴Y5..27已嵌入闭合上下端座，不扩大既有接触；并非全23件均匀缩放",
                                "concepts": {style: {key: detail["sweep"][key] for key in
                                    ("sweepEnvelopeDiameterBefore", "sweepEnvelopeDiameterAfter", "eightRodsWidthBeforeAfter",
                                     "minimumDistanceExcludingUnchangedShaft", "minimumRadialGapAtOverlappingYExcludingShaft")}
                                    for style, detail in report["concepts"].items()}}
    write_json(ASSETS/"concepts.json", contract)
    report["sourcesUnchanged"] = before == sources() == contract["baseline"]["sources"]
    report["protected24Unchanged"] = all(sha(ROOT/path) == identity for path, identity in protected.items())
    if not report["sourcesUnchanged"] or not report["protected24Unchanged"]:
        report["errors"].append("正式来源或R1六壳/18SVG保护失败")
    report["passed"] = not report["errors"]
    write_json(EVIDENCE/"checks.json", report)
    manifest = {"revision": "r2", "generator": {"path": relative(Path(__file__)), "sha256": sha(Path(__file__))},
                "sources": before, "protectedShellTextures": protected,
                "outputs": {relative(path): sha(path) for path in [ASSETS/"concepts.json", *[ASSETS/style/"rotor.json" for style in ("a", "b", "c")]]}}
    write_json(EVIDENCE/"manifest.json", manifest)
    print(json.dumps({"passed": report["passed"], "protected24Unchanged": report["protected24Unchanged"],
                      "sourcesUnchanged": report["sourcesUnchanged"], "rotorSizing": contract["rotorSizing"],
                      "errors": report["errors"]}, ensure_ascii=False, indent=2))
    if not report["passed"]:
        raise SystemExit(1)


def lower_r3(original: dict) -> tuple[dict, dict[int, int]]:
    """仅替换原底台0/1、静态接头6/7和封住中心的加工肩23，围出6.4方孔。

    下层四角脚保留原外界与Y0，上部围孔平台从Y1.25起，露出短轴下段侧面。
    原八棱端板2..5及其余全部对象/UV原样复制，禁止扩大检修盖或实体轴孔范围。
    """
    source = original["elements"]
    bottom_top = source[0]["to"][1]
    footparts = []
    for x0, x1 in ((1, 4.8), (11.2, 15)):
        for z0, z1 in ((1, 4.8), (11.2, 15)):
            footparts.append(box((x0, 0, z0), (x1, 1.25, z1), "dark", name="底台静止角脚"))
    replacements = {
        0: footparts + square_ring(1, 4.8, 1.25, bottom_top, "dark", "底台下层围孔平台"),
        1: square_ring(2, 4.8, source[1]["from"][1], source[1]["to"][1], "casing", "底台上层静止围孔座"),
        6: square_ring(4.35, 4.8, 1.25, 1.65, "machined", "静止轴座下口加工边"),
        7: square_ring(4.2, 4.8, 1.65, 2.25, "machined", "静止轴座上阶加工边"),
        23: square_ring(1.8, 4.8, source[23]["from"][1], source[23]["to"][1], "machined", "底台围孔加工钢肩"),
    }
    output = copy.deepcopy(original)
    elements = []; old_to_new = {}
    for index, element in enumerate(source):
        if index in replacements:
            elements.extend(replacements[index])
        else:
            old_to_new[index] = len(elements)
            elements.append(copy.deepcopy(element))
    output["elements"] = elements
    return output, old_to_new


def shaft_r3(lower: dict) -> dict:
    """独立Minecraft方截面钢短轴：4×4、Y0..4，侧面/端面均按真实跨度取原加工钢槽。"""
    return {"parent": "minecraft:block/block", "ambientocclusion": True,
            "credit": "ART-CENTRIFUGE-14A R3 独立底部输入短轴，仅候选模型",
            "textures": copy.deepcopy(lower["textures"]),
            "elements": [box((6, 0, 6), (10, 4, 10), "machined", name="底部独立旋转钢短轴")]}


def shaft_clearance(shaft: dict, lower: dict, old_to_new: dict[int, int]) -> dict:
    """解析整圈角点扫掠；只允许短轴Y3.98..4.0进入原2..5端板的明确装配接触。

    其余静止孔座必须有正净距，不用轴/方孔在零相位的面间隙冒充360度间隙。
    """
    element = shaft["elements"][0]
    radial = radial_interval(element)
    y0, y1 = element["from"][1], element["to"][1]
    seats = {old_to_new[index]: index for index in (2, 3, 4, 5)}
    report = {"method": "矩形横截面精确径向区间的360度圆环并集", "unit": "模型单位1/16格",
              "shaftSection": [4, 4], "shaftY": [y0, y1], "axisXZ": [8, 8],
              "sweepRadius": [round(value, 6) for value in radial],
              "exposedLowerSideY": [0, 1.25], "squareOpening": {"xz": [4.8, 11.2], "width": 6.4},
              "assemblyContacts": [], "fixedParts": [], "errors": []}
    distances = []
    for index, fixed in enumerate(lower["elements"]):
        static_radius = radial_interval(fixed)
        y_fixed = [fixed["from"][1], fixed["to"][1]]
        radial_gap = interval_gap(*radial, *static_radius)
        y_gap = interval_gap(y0, y1, *y_fixed)
        distance = math.hypot(radial_gap, y_gap)
        label = {"lowerIndex": index, "name": fixed.get("name", "原端板/底台"),
                 "radialGap": round(radial_gap, 6), "distance": round(distance, 6)}
        if distance <= 1e-8:
            overlap = [max(y0, y_fixed[0]), min(y1, y_fixed[1])]
            if index in seats and overlap[0] >= 3.98-1e-8 and overlap[1] <= 4+1e-8:
                report["assemblyContacts"].append({**label, "sourceR2Index": seats[index], "overlapY": overlap,
                                                   "reason": "原端板闭合实体的上端装配接触，明确不声称此段净空"})
            else:
                report["errors"].append(f"短轴与静止lower元素{index}在装配范围外相交")
        else:
            distances.append((distance, label))
        report["fixedParts"].append(label)
    distance, nearest = min(distances, key=lambda pair: pair[0])
    report["minimumDistanceExcludingAssembly"] = round(distance, 6)
    report["nearestFixed"] = nearest
    report["passed"] = not report["errors"] and bool(report["assemblyContacts"])
    return report


def main() -> None:
    """R3默认入口只写lower/shaft/契约；先核原上壳/转子/材质身份，不复跑旧素材矩阵。"""
    history = ASSETS/"history/pre-r3"
    evidence_history = ROOT/"build/reports/art/ART-CENTRIFUGE-14A/assets/history/pre-r3"
    protected = json.loads((evidence_history/"protected-upper-rotor-textures.json").read_text(encoding="utf-8"))
    before = sources()
    contract = json.loads((history/"concepts.json").read_text(encoding="utf-8"))
    report = {"revision": "r3", "scope": "只核底台局部/短轴/UV/360净距及保护身份", "concepts": {}, "errors": []}
    for concept in contract["concepts"]:
        style = concept["id"]
        original = json.loads((history/style/"lower.json").read_text(encoding="utf-8"))
        lower, mapping = lower_r3(original)
        shaft = shaft_r3(lower)
        clearance = shaft_clearance(shaft, lower, mapping)
        invalid_uv = []
        for part, model_data in (("lower", lower), ("shaft", shaft)):
            for index, element in enumerate(model_data["elements"]):
                for face, definition in element["faces"].items():
                    uv = definition["uv"]
                    if len(uv) != 4 or any(not 0 <= value <= 16 for value in uv):
                        invalid_uv.append([part, index, face, uv])
        unchanged = all(original["elements"][old] == lower["elements"][new] for old, new in mapping.items())
        bounds = transformed_bounds(lower["elements"])
        bounds_ok = all(0 <= bounds[0][axis] and bounds[1][axis] <= 16 for axis in range(3))
        report["concepts"][style] = {"lowerElementCount": len(lower["elements"]), "shaftElementCount": 1,
                                      "replacedR2LowerIndices": [0, 1, 6, 7, 23],
                                      "preservedLowerElementCount": len(mapping), "preservedLowerElementsEqual": unchanged,
                                      "preservedR2ToR3Indices": mapping, "invalidUV": invalid_uv,
                                      "lowerBounds": bounds, "lowerBoundsWithinBlock": bounds_ok,
                                      "shaftBounds": transformed_bounds(shaft["elements"]), "clearance": clearance}
        if not clearance["passed"] or not unchanged or invalid_uv or not bounds_ok:
            report["errors"].append(f"{style}底轴/静座/UV/保护失败")
        write_json(ASSETS/style/"lower.json", lower)
        write_json(ASSETS/style/"shaft.json", shaft)
        concept["models"]["shaft"] = f"{style}/shaft.json"
    contract["revision"] = "r3"
    contract["revisionDescription"] = "底部独立4×4钢短轴Y0..4与原转子同相位；静止四角脚及6.4围孔轴座，上端约.02嵌入原端板单列装配接触"
    contract["shaft"] = {"coordinates": "lower-local", "axis": {"origin": [8, 16, 8], "direction": [0, 1, 0]},
                          "section": [4, 4], "y": [0, 4], "material": "machined", "phase": "same-as-rotor",
                          "staticOpeningWidth": 6.4, "minimum360ClearanceExcludingAssembly": .371573,
                          "assemblyContactY": [3.98, 4], "modelsKey": "shaft"}
    write_json(ASSETS/"concepts.json", contract)
    report["sourcesUnchanged"] = before == sources() == contract["baseline"]["sources"]
    report["protected24Unchanged"] = all(sha(ROOT/path) == identity for path, identity in protected.items())
    if not report["sourcesUnchanged"] or not report["protected24Unchanged"]:
        report["errors"].append("上壳/R2转子/18SVG或14正式来源漂移")
    report["passed"] = not report["errors"]
    write_json(EVIDENCE/"checks.json", report)
    manifest = {"revision": "r3", "generator": {"path": relative(Path(__file__)), "sha256": sha(Path(__file__))},
                "sources": before, "protectedUpperRotorTextures": protected,
                "outputs": {relative(path): sha(path) for path in [ASSETS/"concepts.json",
                            *[ASSETS/style/(part+".json") for style in ("a", "b", "c") for part in ("lower", "shaft")]]}}
    write_json(EVIDENCE/"manifest.json", manifest)
    print(json.dumps({"passed": report["passed"], "sourcesUnchanged": report["sourcesUnchanged"],
                      "protected24Unchanged": report["protected24Unchanged"], "shaft": contract["shaft"],
                      "lowerElements": {style: detail["lowerElementCount"] for style, detail in report["concepts"].items()},
                      "errors": report["errors"]}, ensure_ascii=False, indent=2))
    if not report["passed"]:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
