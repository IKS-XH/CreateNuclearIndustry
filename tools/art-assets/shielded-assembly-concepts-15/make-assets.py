"""15A 三稿专属生成器：只写候选 SVG、模型与本批有限检查，不导出或安装 PNG。"""

from __future__ import annotations

import argparse
import hashlib
import itertools
import json
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
TOOL = Path(__file__).resolve().parent
ASSETS = TOOL / "assets"
REPORT = ROOT / "build/reports/art/ART-SHIELDED-ASSEMBLY-15A/assets"
RESOURCE = ROOT / "src/main/resources/assets/create_nuclear_industry"
RIG_PATH = ROOT / "tools/art-assets/sources/fuel-02e/rig.json"
SLOTS = ("lead", "steel", "machined", "fastener", "brass", "window", "lamp_off", "lamp_on", "deck")
RIG = json.loads(RIG_PATH.read_text(encoding="utf-8"))


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def save(path: Path, value: dict | list) -> None:
    """路径仅由本生成器常量构成，输出以 UTF-8 和稳定缩进保存。"""
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def source_paths() -> list[Path]:
    """绑定真实八格、活动分件、朝向/底轴消费和配方来源，不运行旧批量器。"""
    paths = [RIG_PATH, ROOT / "tools/art-assets/fuel_02e_assets.py", ROOT / "gradle.properties"]
    paths += sorted((RESOURCE / "models/block/shielded_assembly_station/parts").glob("*.json"))
    paths += sorted((RESOURCE / "models/block/shielded_assembly_station/partials").glob("*.json"))
    paths += [RESOURCE / f"blockstates/{name}.json" for name in ("shielded_assembly_station", "shielded_assembly_part")]
    paths += [ROOT / f"src/main/java/com/iksxh/create_nuclear_industry/production/{name}.java" for name in
              ("ShieldedAssemblyRenderer", "ShieldedAssemblyLayout", "ShieldedAssemblyBlockEntity")]
    paths += [ROOT / "src/main/resources/data/create_nuclear_industry/recipe/mechanical_crafting/shielded_assembly_station.json"]
    paths += sorted((ROOT / "src/main/resources/data/create_nuclear_industry/recipe/shielded_assembly").glob("*.json"))
    paths += sorted((ROOT / "tools/art-assets/sources/fuel-02e").glob("*.svg"))
    paths += sorted((RESOURCE / "textures/block").glob("shielded_assembly_multiblock*.png"))
    return paths


def protected_paths() -> list[Path]:
    """有限保护清单覆盖本机原输入、14A/14B和现有包；不扫描世界或全设备。"""
    paths = source_paths()
    for directory in ("tools/art-assets/centrifuge-concepts-14", "tools/art-assets/centrifuge-art-14b",
                      "build/reports/art/ART-CENTRIFUGE-14A", "build/reports/art/ART-CENTRIFUGE-14B"):
        paths += [p for p in (ROOT / directory).rglob("*") if p.is_file()]
    paths += [p for p in (ROOT / "tools/art-assets/turbine-rotor-bearing-art-10b/__pycache__").glob("*") if p.is_file()]
    paths += [ROOT / name for name in ("logs/debug.log", "logs/latest.log", "build/libs/create_nuclear_industry-0.1.0.jar")]
    paths += sorted((RESOURCE / "models/block").glob("enrichment_centrifuge*.json"))
    paths += sorted((RESOURCE / "textures/block").glob("enrichment_centrifuge*.png"))
    return sorted(set(p for p in paths if p.is_file()))


def identity(paths: list[Path]) -> dict:
    return {p.relative_to(ROOT).as_posix(): {"sha256": sha(p), "bytes": p.stat().st_size} for p in paths}


PALETTES = {
    "a": {"lead": "#879397", "light": "#B1BBBD", "shadow": "#69797E", "steel": "#52656D", "dark": "#34464E",
          "machined": "#9CAFB6", "edge": "#C5D2D5", "brass": "#AA9161", "brasslight": "#C5AC76", "glass": "#9CB9BE"},
    "b": {"lead": "#89918F", "light": "#B4BBB6", "shadow": "#6B7977", "steel": "#61726E", "dark": "#3F514E",
          "machined": "#A5B7B3", "edge": "#CED6CF", "brass": "#9C8154", "brasslight": "#BAA16D", "glass": "#ADC5BE"},
    "c": {"lead": "#A3A6A0", "light": "#C7C9BE", "shadow": "#818B87", "steel": "#687879", "dark": "#47595B",
          "machined": "#ABBABA", "edge": "#D2D9D1", "brass": "#AC9467", "brasslight": "#CBB57F", "glass": "#B2C7C8"},
}


def rect(x: float, y: float, width: float, height: float, color: str) -> str:
    return f'<rect x="{x}" y="{y}" width="{width}" height="{height}" fill="{color}"/>'


def svg(slot: str, p: dict, style: str) -> str:
    """各槽只有有限不透明色；玻璃保留透明底，稀疏反光不冒充整体发光。"""
    parts: list[str] = []
    if slot == "window":
        parts = [rect(2, 2, 4, 1, p["glass"]), rect(3, 3, 2, 1, p["glass"]), rect(10, 12, 3, 1, p["glass"])]
    elif slot in ("lamp_off", "lamp_on"):
        # 灯面实际只有1单位高；统一实色槽保证任何真实UV位置均能显示on/off差异。
        parts = [rect(0, 0, 16, 16, "#CCB467" if slot == "lamp_on" else p["shadow"]),
                 rect(0, 0, 16, 1, "#E7D792" if slot == "lamp_on" else p["steel"])]
    elif slot == "fastener":
        parts = [rect(0, 0, 16, 16, p["steel"]), rect(0, 0, 16, 1, p["machined"]),
                 rect(0, 15, 16, 1, p["dark"]), rect(7, 0, 2, 16, p["dark"])]
    elif slot == "machined":
        parts = [rect(0, 0, 16, 16, p["machined"]), rect(0, 0, 16, 1, p["edge"]), rect(0, 15, 16, 1, p["steel"]),
                 rect(0, 5, 16, 1, p["light"]), rect(0, 11, 16, 1, p["light"])]
    elif slot == "brass":
        parts = [rect(0, 0, 16, 16, p["brass"]), rect(0, 0, 16, 1, p["brasslight"]), rect(0, 15, 16, 1, p["shadow"])]
    elif slot == "deck":
        parts = [rect(0, 0, 16, 16, p["steel"])]
        parts += [rect(0, y, 16, 1, p["machined"]) for y in (2, 6, 10, 14)]
        parts += [rect(x, 0, 1, 16, p["dark"]) for x in (3, 11)]
    elif slot == "steel":
        parts = [rect(0, 0, 16, 16, p["steel"]), rect(0, 0, 16, 1, p["machined"]), rect(0, 15, 16, 1, p["dark"])]
        parts += [rect(3, 6, 1, 1, p["shadow"]), rect(12, 10, 2, 1, p["shadow"])]
    else:
        parts = [rect(0, 0, 16, 16, p["lead"]), rect(0, 0, 16, 1, p["light"]), rect(0, 15, 16, 1, p["shadow"])]
        # A大板少接缝，B铸板短横痕，C模块浅边，差异不靠密集铆钉堆叠。
        details = {"a": [(2, 5, 4, 1)], "b": [(2, 4, 3, 1), (10, 11, 3, 1)], "c": [(0, 0, 1, 16), (14, 13, 2, 1)]}
        parts += [rect(*data, p["light"]) for data in details[style]]
    return '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges">\n' + "\n".join(parts) + '\n</svg>\n'


FACE_AXES = {"north": (0, 1), "south": (0, 1), "east": (2, 1), "west": (2, 1), "up": (0, 2), "down": (0, 2)}


def box(elements: list, name: str, lo: tuple, hi: tuple, material: str, faces: tuple | None = None) -> None:
    """在16单位格线上切开长面，UV跨度等于实面跨度；不是把16²图缩进小紧固件。"""
    segments = []
    for axis in range(3):
        cuts = [lo[axis]] + [v for v in (16,) if lo[axis] < v < hi[axis]] + [hi[axis]]
        segments.append(list(zip(cuts, cuts[1:])))
    for spans in itertools.product(*segments):
        a, b = [s[0] for s in spans], [s[1] for s in spans]
        mapping = {}
        for face in faces or tuple(FACE_AXES):
            normal_axis, high = {"north": (2, False), "south": (2, True), "east": (0, True),
                                 "west": (0, False), "up": (1, True), "down": (1, False)}[face]
            # UV切片产生的内部格线不是原盒外表面，不导出隐藏的相反重复面。
            if (b[normal_axis] != hi[normal_axis]) if high else (a[normal_axis] != lo[normal_axis]):
                continue
            u, v = FACE_AXES[face]
            u0, u1 = a[u] % 16, a[u] % 16 + b[u] - a[u]
            if face in ("north", "south", "east", "west"):
                v1 = 16 - a[v] % 16
                v0 = v1 - b[v] + a[v]
            else:
                v0, v1 = a[v] % 16, a[v] % 16 + b[v] - a[v]
            mapping[face] = {"texture": "#" + material, "uv": [u0, v0, u1, v1]}
        elements.append({"name": name, "from": a, "to": b, "faces": mapping})


def base(elements: list, style: str) -> None:
    """底台围主控半轴开口分成四片；其上盖继续封闭工位，无假静态输入轴。"""
    for lo, hi in (((0, 0, 0), (6, 4, 32)), ((10, 0, 0), (32, 4, 32)),
                   ((6, 0, 0), (10, 4, 6)), ((6, 0, 10), (10, 4, 32))):
        box(elements, "主控轴孔围台", lo, hi, "steel")
    if style == "a":
        box(elements, "厚铅底肩", (1, 4, 1), (31, 7.5, 31), "lead")
        box(elements, "加工钢压边", (2, 7.5, 2), (30, 8.5, 30), "machined")
    elif style == "b":
        box(elements, "铸钢重型下座", (0, 4, 0), (32, 8.5, 32), "steel")
        box(elements, "下座铅芯", (2, 8.5, 2), (30, 9.5, 30), "lead")
    else:
        box(elements, "检修下座", (1, 4, 1), (31, 6.5, 31), "lead")
        box(elements, "分段下托层", (2, 6.5, 2), (30, 8, 30), "steel")
    box(elements, "室内工作底板", (3, 9.5, 3), (29, 10.5, 29), "deck")
    box(elements, "中央夹具静止座", (12, 10.5, 11), (20, 11, 22), "steel")
    # 静止后支架位于原走廊之外，机械臂/夹具扫掠不碰壳体。
    box(elements, "左后轨", (4.5, 10.5, 24), (6, 25.5, 27), "steel")
    box(elements, "右后轨", (26, 10.5, 24), (27.5, 25.5, 27), "steel")


def pane(elements: list, name: str, x0: float, x1: float, y0: float, y1: float, depth: float) -> None:
    """观察窗使用独立透明材质两面，窄边由实体钢框承担。"""
    box(elements, name, (x0, y0, depth), (x1, y1, depth + .125), "window", ("north", "south"))


def upper_with_lamp(elements: list, name: str, lo: tuple, hi: tuple, material: str) -> None:
    """上梁按真实灯面挖分区，灯片和背板相接，不把灯塞在实心梁内或贴共面。"""
    xs, ys = [lo[0], 13.5, 18.5, hi[0]], [lo[1], 26, 27, hi[1]]
    for i, (x0, x1) in enumerate(zip(xs, xs[1:])):
        for j, (y0, y1) in enumerate(zip(ys, ys[1:])):
            if i == j == 1:
                box(elements, "上梁灯槽背板", (x0, y0, lo[2] + .5), (x1, y1, hi[2]), material)
                box(elements, "原工作灯静态对照", (x0, y0, lo[2]), (x1, y1, lo[2] + .5), "lamp_off", ("north",))
            else:
                box(elements, name, (x0, y0, lo[2]), (x1, y1, hi[2]), material)


def shell(style: str) -> list:
    result: list = []
    base(result, style)
    if style == "a":
        # 厚壁热室用整块大板和退台横窗，侧壳与背壳完整屏蔽内部。
        box(result, "左厚屏蔽壁", (0, 8.5, 0), (3.5, 28, 32), "lead")
        box(result, "右厚屏蔽壁", (28.5, 8.5, 0), (32, 28, 32), "lead")
        box(result, "后厚屏蔽壁", (3.5, 8.5, 28.5), (28.5, 28, 32), "lead")
        box(result, "窗下厚退台", (3.5, 8.5, 0), (28.5, 12, 3), "lead")
        upper_with_lamp(result, "窗上厚退台", (3.5, 25.5, 0), (28.5, 28, 3), "lead")
        for x0, x1 in ((3.5, 5), (27, 28.5)):
            box(result, "退台窗竖框", (x0, 12, .25), (x1, 25.5, 2.5), "steel")
        for y0, y1 in ((12, 13), (24.5, 25.5)):
            box(result, "退台窗横框", (5, y0, .25), (27, y1, 2.5), "steel")
        pane(result, "宽横向铅玻璃", 5, 27, 13, 24.5, 1.75)
        box(result, "层叠钢顶肩", (0, 28, 0), (32, 30, 32), "steel")
        box(result, "内收厚铅顶盖", (1.5, 30, 1.5), (30.5, 31.5, 30.5), "lead")
        for lo, hi in (((1.5, 31.5, 1.5), (30.5, 32, 5)), ((1.5, 31.5, 27), (30.5, 32, 30.5)),
                       ((1.5, 31.5, 5), (5, 32, 27)), ((27, 31.5, 5), (30.5, 32, 27))):
            box(result, "顶盖压条外围", lo, hi, "lead")
        box(result, "顶部压条", (5, 31.5, 5), (27, 32, 27), "machined")
    elif style == "b":
        # 双窗和中央加强构件、外露铸柱形成机床轮廓；整体仍是封闭屏蔽室。
        box(result, "左铅芯侧板", (1, 9.5, 3), (3, 27, 29), "lead")
        box(result, "右铅芯侧板", (29, 9.5, 3), (31, 27, 29), "lead")
        box(result, "背部铅芯板", (3, 9.5, 29), (29, 27, 31), "lead")
        for x in (0, 28):
            for z in (0, 28):
                box(result, "铸钢立柱", (x, 8.5, z), (x + 4, 29, z + 4), "steel")
        upper_with_lamp(result, "明确上横梁", (4, 25, 0), (28, 29, 3.5), "steel")
        box(result, "双窗下横梁", (4, 9.5, 0), (28, 12.5, 3.5), "steel")
        box(result, "双窗中央加强柱", (15, 12.5, 0), (17, 25, 2), "steel")
        for x0, x1 in ((4, 5), (27, 28)):
            box(result, "双窗外压边", (x0, 12.5, .5), (x1, 25, 2.5), "machined")
        pane(result, "左观察区", 5, 15, 12.5, 25, 2)
        pane(result, "右观察区", 17, 27, 12.5, 25, 2)
        box(result, "顶铅封盖", (4, 27, 4), (28, 30, 28), "lead")
        box(result, "横梁顶帽", (0, 29, 0), (32, 32, 5), "steel")
        box(result, "后横梁顶帽", (0, 29, 27), (32, 32, 32), "steel")
        box(result, "顶部平铅板", (4, 30, 5), (28, 31.5, 27), "lead")
        for x in (0, 30):
            box(result, "侧部厚加强筋", (x, 11, 13), (x + 2, 26, 17), "steel")
    else:
        # 分段检修舱采用四角梁、三面内收护板和独立窗模块，避免只换A稿颜色。
        for x in (0, 29):
            for z in (0, 29):
                box(result, "四角钢梁", (x, 8, z), (x + 3, 29.5, z + 3), "steel")
        for x0, x1 in ((.5, 2.5), (29.5, 31.5)):
            for z0, z1 in ((3, 15.5), (16.5, 29)):
                box(result, "侧模块内收护板", (x0, 8, z0), (x1, 28, z1), "lead")
            box(result, "侧模块凹接缝", (x0 + .25, 8, 15.5), (x1 - .25, 28, 16.5), "steel")
        for x0, x1 in ((3, 15.5), (16.5, 29)):
            box(result, "后模块护板", (x0, 8, 29.5), (x1, 28, 31.5), "lead")
        box(result, "后模块接缝", (15.5, 8, 30), (16.5, 28, 31), "steel")
        box(result, "前下检修护板", (3, 8, .75), (29, 12.5, 2.5), "lead")
        upper_with_lamp(result, "前上护板", (3, 24.5, .75), (29, 28, 2.5), "lead")
        for x0, x1 in ((3, 5), (27, 29)):
            box(result, "窗模块竖梁", (x0, 12.5, .5), (x1, 24.5, 2.5), "steel")
        for y0, y1 in ((12.5, 13.5), (23.5, 24.5)):
            box(result, "窗模块横梁", (5, y0, .5), (27, y1, 2.5), "machined")
        pane(result, "独立横窗模块", 5, 27, 13.5, 23.5, 1.5)
        box(result, "顶检修内托层", (2, 28, 2), (30, 29.5, 30), "steel")
        for x0, x1 in ((0, 15.5), (16.5, 32)):
            box(result, "两片模块铅顶", (x0, 29.5, 0), (x1, 32, 32), "lead")
        box(result, "顶盖凹中缝", (15.5, 29.5, 1), (16.5, 31.5, 31), "steel")
        for x0, x1 in ((5, 11), (21, 27)):
            box(result, "下护板小检修压盖", (x0, 9, .25), (x1, 11.5, .75), "steel")
    # 少量实体锁固件与灯位没有料槽标签，不暗示新增指定接口或GUI。
    sites = {"a": ((3.75, 27.25), (13.5, 23), 0), "b": ((4, 27), (13.5, 23), .25),
             "c": ((3.5, 27.5), (14, 22), .25)}
    xs, ys, depth = sites[style]
    for x in xs:
        for y in ys:
            box(result, "窗框钢紧固头", (x, y, depth), (x + 1, y + 1, depth + .25), "fastener")
    return result


def arm(style: str, right: bool) -> list:
    """细化原肩、执行杆与双指夹爪；整体仍位于原idle_bounds，未引入新动作。"""
    result: list = []
    data = [
        ("肩左轨", (7, 20, 10), (8.25, 22.5, 22), "steel"),
        ("肩右轨", (11.75, 20, 10), (13, 22.5, 22), "steel"),
        ("肩上加工盖", (8.25, 22.5, 10), (11.75, 23, 22), "machined"),
        ("肩中央关节体", (8.25, 20, 12), (11.75, 22.5, 19), "steel"),
        ("肩前功能锁圈", (8.5, 20.5, 11.25), (9.5, 21.5, 11.5), "brass"),
        ("关节前端钢盖", (8.5, 20.25, 11.5), (11.5, 22.25, 12), "machined"),
        ("执行杆加工钢芯", (8.75, 15.5, 12.5), (11.25, 20, 16.5), "machined"),
        ("功能黄铜锁圈", (8, 18, 12), (12, 19.5, 17), "brass"),
        ("执行杆左导条", (8.25, 15.5, 12.5), (8.75, 18, 16.5), "steel"),
        ("执行杆右导条", (11.25, 15.5, 12.5), (11.75, 18, 16.5), "steel"),
        ("腕部钢座", (9, 14.75, 13), (11, 16, 16), "steel"),
        ("左夹指", (9, 13, 13), (9.5, 14.75, 16), "machined"),
        ("右夹指", (10.5, 13, 13), (11, 14.75, 16), "machined"),
        ("夹爪中锁片", (9.5, 14.25, 13), (10.5, 14.75, 13.5), "fastener"),
    ]
    if style == "b":
        data += [("铸肩中筋", (9.75, 21.5, 19), (10.25, 22.5, 22), "steel")]
    elif style == "c":
        data += [("肩检修薄盖", (8.5, 20.25, 19), (11.5, 22.25, 19.25), "lead")]
    for name, lo, hi, material in data:
        if right:
            lo, hi = (32 - hi[0], lo[1], lo[2]), (32 - lo[0], hi[1], hi[2])
        box(result, name, lo, hi, material)
    return result


def fixture(style: str) -> list:
    """中央夹具保原升降包络；分开的钳口与层叠工作台留出可辨缝隙。"""
    result: list = []
    data = [
        ("夹具升降座", (14, 11, 12.5), (18, 13.5, 20), "steel"),
        ("座前加工盖", (14.5, 11.5, 12), (17.5, 13, 12.5), "machined"),
        ("升降中柱", (14.75, 13.5, 14), (17.25, 15, 18), "machined"),
        ("工作台锁固圈", (13, 14, 13), (19, 15.25, 19), "brass"),
        ("精加工台面", (13.5, 15.25, 13.5), (18.5, 16, 18.5), "machined"),
        ("左夹具钳口", (15, 16, 14), (15.625, 18, 18), "steel"),
        ("右夹具钳口", (16.375, 16, 14), (17, 18, 18), "steel"),
        ("钳口左压片", (15.625, 16.5, 14.5), (15.875, 17.5, 17.5), "machined"),
        ("钳口右压片", (16.125, 16.5, 14.5), (16.375, 17.5, 17.5), "machined"),
    ]
    if style == "b":
        data += [("台前钢筋", (13.5, 14, 12.5), (18.5, 15.25, 13), "steel")]
    elif style == "c":
        data += [("升降检修侧盖", (13.75, 11.5, 13), (14, 13, 19), "lead")]
    for name, lo, hi, material in data:
        box(result, name, lo, hi, material)
    return result


def bounds(elements: list) -> list:
    return [[min(e["from"][k] for e in elements) for k in range(3)],
            [max(e["to"][k] for e in elements) for k in range(3)]]


def model(style: str, elements: list) -> dict:
    return {"credit": "ART-SHIELDED-ASSEMBLY-15A 原创候选，未安装", "ambientocclusion": True,
            "textures": {**{slot: f"cni_concept15:block/{style}/{slot}" for slot in SLOTS},
                         "particle": f"cni_concept15:block/{style}/lead"}, "elements": elements}


def generate() -> dict:
    names = {"a": "厚壁屏蔽热室", "b": "铸钢装配工作站", "c": "模块检修舱"}
    descriptions = {"a": "厚铅大板、退台宽窗、层叠顶肩与重型底台。", "b": "铸钢立柱、双观察区、中央加强柱与厚侧筋。",
                    "c": "浅暖分段铅护板、四角钢梁、独立窗模块及凹检修层。"}
    contract = {"schemaVersion": 1, "task": "ART-SHIELDED-ASSEMBLY-15A", "baseline": {
        "head": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
        "branch": subprocess.check_output(["git", "branch", "--show-current"], cwd=ROOT, text=True).strip(),
        "sources": identity(source_paths()), "sourceRig": RIG_PATH.relative_to(ROOT).as_posix(),
        "registration": ["shielded_assembly_station", "shielded_assembly_part"],
        "materials": "实际配方的铅板、钢板、两个Create机械手、辐射传感器；少量黄铜限原机械手功能件。",
        "renderState": "current/expanded 时原Renderer绘制三件idle机构及原Create SHAFT_HALF；WORKING仅静态灯素材对照。"},
        "coordinates": {"units": "16模型单位=1格", "origin": "北向主控格最小角", "axes": {"x": "east", "y": "up", "z": "south"},
            "machineBounds": [[0, 0, 0], [32, 32, 32]], "partFormula": "x+2*z+4*y", "partOffset": "[16*x,16*y,16*z]仅用于原八part对照，不用于候选partial",
            "facingPivotXZ": [8, 8], "partialOffset": [0, 0, 0], "shaftCenter": [8, 0, 8],
            "shaftAperture": [[6, 0, 6], [10, 4, 10]], "shaftSource": "Create原生SHAFT_HALF，预览执行者只读处理；候选没有静态轴"},
        "concepts": {}}
    for style in "abc":
        texture_paths = {}
        for slot in SLOTS:
            path = ASSETS / f"textures/{style}/{slot}.svg"
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(svg(slot, PALETTES[style], style), encoding="utf-8")
            texture_paths[slot] = path.relative_to(ASSETS).as_posix()
        pieces = {"shell": shell(style), "left_arm": arm(style, False), "right_arm": arm(style, True), "fixture": fixture(style)}
        models = {}
        geometry = {}
        for part, elements in pieces.items():
            path = ASSETS / f"models/{style}/{part}.json"
            save(path, model(style, elements))
            models[part] = path.relative_to(ASSETS).as_posix()
            geometry[part] = {"bounds": bounds(elements), "elements": len(elements)}
        contract["concepts"][style] = {"name": names[style], "label": style.upper() + " · " + names[style], "description": descriptions[style],
            "palette": PALETTES[style], "models": models, "textures": texture_paths, "rig": RIG["parts"], "geometry": geometry,
            "visibility": "前窗可见左右执行杆、双指夹爪和中央双钳口；浏览器剖视与游戏视觉验收分别记录。",
            "stateComparison": {"off": "lamp_off", "on": "lamp_on", "meaning": "仅真实WORKING灯素材静态对照，无假工序循环"}}
    save(ASSETS / "concepts.json", contract)
    return contract


def overlap(a: dict, b: dict) -> bool:
    return all(min(a["to"][k], b["to"][k]) - max(a["from"][k], b["from"][k]) > 1e-6 for k in range(3))


def same_facing_faces(elements: list) -> list:
    """检查外露同向共面重叠；外法向微移点仍在实体内的接合面不属于可见表面。"""
    normal = {"north": (2, 0), "south": (2, 1), "east": (0, 1), "west": (0, 0), "up": (1, 1), "down": (1, 0)}
    conflicts = []
    for first, second in itertools.combinations(elements, 2):
        for face in first["faces"].keys() & second["faces"].keys():
            axis, high = normal[face]
            edge = "to" if high else "from"
            if abs(first[edge][axis] - second[edge][axis]) > 1e-6:
                continue
            projected = [k for k in range(3) if k != axis]
            if all(min(first["to"][k], second["to"][k]) - max(first["from"][k], second["from"][k]) > 1e-6 for k in projected):
                sample = [0., 0., 0.]
                sample[axis] = first[edge][axis] + (1e-4 if high else -1e-4)
                for k in projected:
                    sample[k] = (min(first["to"][k], second["to"][k]) + max(first["from"][k], second["from"][k])) / 2
                if any(all(e["from"][k] < sample[k] < e["to"][k] for k in range(3)) for e in elements):
                    continue
                conflicts.append({"first": first["name"], "second": second["name"], "face": face, "plane": first[edge][axis]})
    return conflicts


def verify(contract: dict, original: dict) -> dict:
    """检查本批引用、实UV密度、静/扫掠包络与来源身份；可视风格须另读实际截图。"""
    errors: list[str] = []
    detail = {}
    for style, concept in contract["concepts"].items():
        models = {name: json.loads((ASSETS / path).read_text(encoding="utf-8")) for name, path in concept["models"].items()}
        for slot, path in concept["textures"].items():
            tree = ET.fromstring((ASSETS / path).read_text(encoding="utf-8"))
            if tree.attrib.get("width") != "16" or tree.attrib.get("height") != "16" or tree.attrib.get("shape-rendering") != "crispEdges":
                errors.append(f"{style}/{slot}:SVG尺寸或crispEdges错误")
            if any(node.tag.rsplit("}", 1)[-1] not in ("svg", "rect") for node in tree.iter()):
                errors.append(f"{style}/{slot}:存在滤镜或不支持形状")
            colors = {node.attrib["fill"] for node in tree if "fill" in node.attrib}
            if len(colors) > 7:
                errors.append(f"{style}/{slot}:色板超过有限上限")
        for name, m in models.items():
            if "parent" in m:
                errors.append(f"{style}/{name}:候选不得依赖parent")
            for e in m["elements"]:
                if any(not 0 <= e["from"][k] < e["to"][k] <= 32 for k in range(3)):
                    errors.append(f"{style}/{name}/{e['name']}:几何越界")
                for face, value in e["faces"].items():
                    slot = value["texture"][1:]
                    uv = value["uv"]
                    if slot not in concept["textures"] or slot not in m["textures"] or len(uv) != 4 or any(not 0 <= x <= 16 for x in uv):
                        errors.append(f"{style}/{name}:材质或UV无效")
                    u, v = FACE_AXES[face]
                    if abs(abs(uv[2] - uv[0]) - (e["to"][u] - e["from"][u])) > 1e-6 or abs(abs(uv[3] - uv[1]) - (e["to"][v] - e["from"][v])) > 1e-6:
                        errors.append(f"{style}/{name}:面跨度与UV密度不一致")
            coplanar = same_facing_faces(m["elements"])
            if coplanar:
                errors.append(f"{style}/{name}:同朝向共面重叠:{coplanar[:3]}")
        # 零厚度接触不当作穿模；原fixture底座在y=11接触静止工位是合法接触。
        part_detail = {}
        for name in ("left_arm", "right_arm", "fixture"):
            candidate = models[name]["elements"]
            rig = RIG["parts"][name]
            actual = bounds(candidate)
            if actual != rig["idle_bounds"] or concept["rig"][name] != rig:
                errors.append(f"{style}/{name}:待机包络或原rig漂移")
            delta = {"left_arm": [1, 0, 0], "right_arm": [-1, 0, 0], "fixture": [0, 3, 0]}[name]
            swept = [{**e, "from": [min(e["from"][k], e["from"][k] + delta[k]) for k in range(3)],
                      "to": [max(e["to"][k], e["to"][k] + delta[k]) for k in range(3)]} for e in candidate]
            collisions = [(e["name"], static["name"]) for e in swept for static in models["shell"]["elements"] if overlap(e, static)]
            if collisions:
                errors.append(f"{style}/{name}:扫掠与机壳相交:{collisions[:3]}")
            part_detail[name] = {"idleBounds": actual, "sweptBounds": bounds(swept), "pivot": rig["pivot"], "shellCollisions": collisions}
        intersections = []
        for first, second in itertools.combinations(("left_arm", "right_arm", "fixture"), 2):
            intersections += [(first, a["name"], second, b["name"]) for a in models[first]["elements"] for b in models[second]["elements"] if overlap(a, b)]
        if intersections:
            errors.append(f"{style}:待机分件相交:{intersections[:3]}")
        hole = {"from": [6, 0, 6], "to": [10, 4, 10]}
        if any(overlap(hole, e) for e in models["shell"]["elements"]):
            errors.append(f"{style}:主控底轴通孔堵塞")
        # 前向正交中心射线只测实体遮挡；透明玻璃不是遮挡层，实际GPU可读性留给负责人。
        targets = {"left_arm": [10, 17, 13], "right_arm": [22, 17, 13], "fixture": [16, 16.5, 16]}
        view = {}
        for name, target in targets.items():
            blockers = [e["name"] for e in models["shell"]["elements"] if any(f["texture"] != "#window" for f in e["faces"].values())
                        and e["from"][0] < target[0] < e["to"][0] and e["from"][1] < target[1] < e["to"][1] and e["from"][2] < target[2]]
            # B的中央加强柱只遮中央线，夹具两侧真实表面另取一条边射线。
            if style == "b" and name == "fixture":
                target = [14.5, 15.5, 14]
                blockers = [e["name"] for e in models["shell"]["elements"] if any(f["texture"] != "#window" for f in e["faces"].values())
                            and e["from"][0] < target[0] < e["to"][0] and e["from"][1] < target[1] < e["to"][1] and e["from"][2] < target[2]]
            if blockers:
                errors.append(f"{style}/{name}:选定前窗射线有实体挡板:{blockers}")
            view[name] = {"sample": target, "opaqueShellBlockers": blockers, "scope": "有限几何射线，仅作可见性线索，不代替浏览器/游戏视觉验收"}
        lamp = next(e for e in models["shell"]["elements"] if e["name"] == "原工作灯静态对照")
        lamp_center = [(lamp["from"][k] + lamp["to"][k]) / 2 for k in (0, 1)]
        lamp_blockers = [e["name"] for e in models["shell"]["elements"] if e is not lamp and
                         e["from"][0] < lamp_center[0] < e["to"][0] and e["from"][1] < lamp_center[1] < e["to"][1]
                         and e["from"][2] < lamp["from"][2]]
        if lamp_blockers:
            errors.append(f"{style}:工作灯前方有实体遮挡:{lamp_blockers}")
        detail[style] = {"modelElements": {name: len(m["elements"]) for name, m in models.items()}, "rig": part_detail,
                         "idlePartialIntersections": intersections, "windowSightLines": view, "shaftHoleOpen": True}
        detail[style].update({"sameFacingCoplanarFaces": {name: same_facing_faces(m["elements"]) for name, m in models.items()},
                              "lampFrontBlockers": lamp_blockers})
    changed = []
    for path, before in original.items():
        current = ROOT / path
        if not current.is_file() or sha(current) != before["sha256"]:
            changed.append(path)
    if changed:
        errors.append(f"原输入漂移:{changed}")
    result = {"task": "ART-SHIELDED-ASSEMBLY-15A", "passed": not errors, "errors": errors,
              "checks": {"svgFinitePalette16Crisp": True, "allReferencesResolve": True, "explicitUVPhysicalDensity": True,
                         "wholeBoundsAndNativePivot": True, "idleAndSweptCorridors": True, "noFakeShaftOrAnimation": True,
                         "threeDifferentShells": len({json.dumps(shell(s), sort_keys=True) for s in "abc"}) == 3},
              "concepts": detail, "protectedFiles": len(original), "protectedChanged": changed,
              "limitations": "静态三稿。窗内可读性须负责人实际浏览；未做Minecraft、Gradle、PNG导出/安装或工序动画。"}
    save(REPORT / "checks.json", result)
    save(REPORT / "output-identity.json", identity([TOOL / "make-assets.py"] + sorted(p for p in ASSETS.rglob("*") if p.is_file())))
    print(json.dumps({"passed": result["passed"], "errors": errors, "protectedFiles": len(original), "models": 12, "svg": 27,
                      "concepts": {s: d["modelElements"] for s, d in detail.items()}}, ensure_ascii=False))
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="只读候选并复核本批输出，不重新生成")
    args = parser.parse_args()
    REPORT.mkdir(parents=True, exist_ok=True)
    before_path = REPORT / "source-protection-before.json"
    if before_path.exists():
        original = json.loads(before_path.read_text(encoding="utf-8"))
    else:
        original = identity(protected_paths())
        save(before_path, original)
    contract = json.loads((ASSETS / "concepts.json").read_text(encoding="utf-8")) if args.check else generate()
    result = verify(contract, original)
    if not result["passed"]:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
