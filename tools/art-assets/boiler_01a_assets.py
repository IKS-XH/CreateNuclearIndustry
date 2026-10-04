"""EXT-B-BOILER-01B锅炉外观与强化钢板配方的可复现导出入口。"""
from __future__ import annotations

import json
import hashlib
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / "tools" / "art-assets"
sys.path.insert(0, str(ART))
import export as strict_exporter

ASSETS = ROOT / "src/main/resources/assets/create_nuclear_industry"
DATA = ROOT / "src/main/resources/data/create_nuclear_industry"
SOURCE = ART / "svg/block/high_pressure_boiler"
FLUID_SOURCE = ART / "svg/fluid"
EVIDENCE = ROOT / "build/reports/extension/EXT-B-BOILER-01B-ASSETS"
REPORT = ROOT / "build/reports/extension/EXT-B-BOILER-01B-ASSETS.md"
NS = "create_nuclear_industry"

# 行字符是像素调色索引；每张16×16纹理均由这些明确的像素格复现。
# 色板取自当前反应堆外壳与核换热器资源，保证钢色、高光和黄铜铆钉一致。
COLORS = {
    "k": "#28353A", "d": "#3E5058", "s": "#5B7078",
    "m": "#788E94", "l": "#A3B7B8", "w": "#D1DFDA", "b": "#D2A359",
    "g": "#715035",
}
CASING_SIDE = [
    "kkkkkkkkkkkkkkkk", "kllllllllllllllk", "kmbgdddddddddbgk", "kmdsmmmmmmmmsdsk",
    "kmdssssssssssdsk", "kmdssssssssssdsk", "kmdssssssssssdsk", "kmddddddddddddsk",
    "kmdssssssssssdsk", "kmdsmssssssssdsk", "kmdsmssssssssdsk", "kmdsmssssssssdsk",
    "kmdssssssssssdsk", "kmbgdddddddddbgk", "kskkkkkkkkkkkkkk", "kkkkkkkkkkkkkkkk",
]


def inset_face(center: list[str]) -> list[str]:
    """将8×8设备面嵌入与反应堆外壳相同的钢板底纹。"""
    rows = [list(row) for row in CASING_SIDE]
    if len(center) != 8 or any(len(row) != 8 for row in center):
        raise ValueError("设备面中心图案必须为8×8像素")
    for y, row in enumerate(center, start=4):
        rows[y][4:12] = row
    return ["".join(row) for row in rows]


def symmetric_top(exchanger: bool = False) -> list[str]:
    """以X/Y两轴对称方式绘制外壳或换热段顶面。"""
    pixels = [list("k" * 16) for _ in range(16)]
    pixels[1] = pixels[14] = list("k" + "l" * 14 + "k")
    pixels[2] = pixels[13] = list("kmbg" + "d" * 8 + "gbmk")
    pixels[3] = pixels[12] = list("km" + "d" * 12 + "mk")
    for index in range(1, 15):
        pixels[index][1] = pixels[index][14] = "l"
    for index in range(2, 14):
        pixels[index][2] = pixels[index][13] = "d"
        pixels[2][index] = pixels[13][index] = "d"
        pixels[index][3] = pixels[index][12] = "d"
        pixels[3][index] = pixels[12][index] = "d"
    for index in range(4, 12):
        pixels[index][4] = pixels[index][11] = "m"
        pixels[4][index] = pixels[11][index] = "m"
    for index in range(5, 11):
        pixels[index][5] = pixels[index][10] = "s"
        pixels[5][index] = pixels[10][index] = "s"
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        pixels[y][x] = "b"
    if exchanger:
        for y in range(6, 10):
            for x in range(6, 10):
                pixels[y][x] = "b" if x in (7, 8) or y in (7, 8) else "d"
    else:
        for x, y in ((6, 6), (9, 6), (6, 9), (9, 9)):
            pixels[y][x] = "l"
        pixels[7][7] = pixels[7][8] = pixels[8][7] = pixels[8][8] = "d"
    return ["".join(row) for row in pixels]


def valve_body_pattern() -> list[str]:
    """用对称金属环和黄铜芯绘制安全阀阀体。"""
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            radius = max(abs(x * 2 - 15), abs(y * 2 - 15))
            row.append({15: "k", 13: "l", 11: "m", 9: "g", 7: "b", 5: "d", 3: "k", 1: "b"}.get(radius, "d"))
        rows.append("".join(row))
    return rows


def safety_badge_pattern() -> list[str]:
    """绘制钢色底板上的对称黄铜警示菱形。"""
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            distance = abs(x * 2 - 15) + abs(y * 2 - 15)
            color = "b" if distance <= 12 else "d" if distance <= 8 else "k"
            if 7 <= x <= 8 and 4 <= y <= 11:
                color = "k"
            if 7 <= x <= 8 and y in (5, 10):
                color = "b"
            row.append(color)
        rows.append("".join(row))
    return rows


PATTERNS: dict[str, list[str]] = {
    "casing_side": CASING_SIDE,
    "casing_top": symmetric_top(),
    "window_frame": CASING_SIDE,
    "water_port_face": inset_face([
        "dddddddd", "ddmmmmdd", "dmbbbbmd", "mbdkkdbm",
        "mbdkkdbm", "dmbbbbmd", "ddmmmmdd", "dddddddd",
    ]),
    "steam_port_face": inset_face([
        "dddddddd", "ddmmmmdd", "dmbbbbmd", "mbbbkkbm",
        "mbbbkkbm", "dmbbbbmd", "ddmmmmdd", "dddddddd",
    ]),
    "controller_face": inset_face([
        "dddddddd", "ddmmmmdd", "dmbbbbmd", "mbdwwdbm",
        "mbdwwdbm", "dmbbbbmd", "ddmmmmdd", "dddddddd",
    ]),
    "valve_body": valve_body_pattern(),
    "exchanger_face": inset_face([
        "dddddddd", "ddmmmmdd", "ddssssdd", "ddssbbdd",
        "ddssbbdd", "ddssssdd", "ddmmmmdd", "dddddddd",
    ]),
    "exchanger_top": symmetric_top(exchanger=True),
    "safety_badge": safety_badge_pattern(),
}

BLOCKS = (
    "high_pressure_boiler_casing", "high_pressure_boiler_window", "high_pressure_boiler_water_port",
    "high_pressure_boiler_steam_port", "high_pressure_boiler_controller", "boiler_safety_valve",
    "boiler_heat_exchange_section",
)


def normalize_rows(pattern: list[str]) -> list[str]:
    """拒绝不完整像素行，防止自动裁切破坏顶面镜像结构。"""
    if len(pattern) != 16 or any(len(row) != 16 for row in pattern):
        raise ValueError("纹理必须有16行且每行恰为16个像素符号")
    return list(pattern)


def svg_for(pattern: list[str]) -> str:
    """把像素格转为严格导出器支持的无样式SVG矩形。"""
    pattern = normalize_rows(pattern)
    rects = []
    for y, row in enumerate(pattern):
        for x, code in enumerate(row):
            if code == ".":
                continue
            color = COLORS.get(code)
            if color is None:
                raise ValueError(f"未知像素色标: {code}")
            rects.append(f'<rect x="{x}" y="{y}" width="1" height="1" fill="{color}"/>')
    return '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges">\n' + "\n".join(rects) + "\n</svg>\n"


def fluid_svg_for(pattern: list[str]) -> str:
    """用SVG留出透明背景，表示流体贴图外的空像素。"""
    pattern = normalize_rows(pattern)
    rects = []
    for y, row in enumerate(pattern):
        for x, code in enumerate(row):
            if code == ".":
                continue
            color = COLORS[code]
            rects.append(f'<rect x="{x}" y="{y}" width="1" height="1" fill="{color}"/>')
    return '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges">\n' + "\n".join(rects) + "\n</svg>\n"


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def face(texture: str) -> dict[str, object]:
    return {side: {"texture": f"#{texture}", "uv": [0, 0, 16, 16]} for side in ("north", "south", "east", "west", "up", "down")}


def cube(lo: list[int], hi: list[int], texture: str) -> dict[str, object]:
    return {"from": lo, "to": hi, "faces": {side: {"texture": f"#{texture}"} for side in ("north", "south", "east", "west", "up", "down")}}


def model(textures: dict[str, str], elements: list[dict[str, object]]) -> dict[str, object]:
    return {"parent": "minecraft:block/block", "textures": {**textures, "particle": f"#{next(iter(textures))}"}, "elements": elements}


def generate_models() -> None:
    steel = "create_nuclear_industry:block/high_pressure_boiler/casing_side"
    top = "create_nuclear_industry:block/high_pressure_boiler/casing_top"
    roots: dict[str, dict[str, object]] = {
        BLOCKS[0]: model({"side": steel, "top": top}, [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
            "north": {"texture": "#side"}, "south": {"texture": "#side"}, "east": {"texture": "#side"}, "west": {"texture": "#side"}, "up": {"texture": "#top"}, "down": {"texture": "#side"}}}]),
        BLOCKS[1]: model({"frame": "create_nuclear_industry:block/high_pressure_boiler/window_frame", "glass": "minecraft:block/glass", "side": steel}, [
            # 四个角柱与八段边梁只在边界接触，形成完整支撑框，不叠放共面大面。
            cube([0, 0, 0], [3, 16, 3], "frame"), cube([13, 0, 0], [16, 16, 3], "frame"),
            cube([0, 0, 13], [3, 16, 16], "frame"), cube([13, 0, 13], [16, 16, 16], "frame"),
            cube([3, 0, 0], [13, 3, 3], "frame"), cube([3, 13, 0], [13, 16, 3], "frame"),
            cube([3, 0, 13], [13, 3, 16], "frame"), cube([3, 13, 13], [13, 16, 16], "frame"),
            cube([0, 0, 3], [3, 3, 13], "frame"), cube([0, 13, 3], [3, 16, 13], "frame"),
            cube([13, 0, 3], [16, 3, 13], "frame"), cube([13, 13, 3], [16, 16, 13], "frame"),
            # 四面玻璃嵌入各自侧壁，面向南北或东西的窗都能透视。
            {"from": [3, 3, 0], "to": [13, 13, 1], "faces": {"north": {"texture": "#glass", "uv": [0, 0, 10, 10]}, "south": {"texture": "#glass", "uv": [0, 0, 10, 10]}}},
            {"from": [3, 3, 15], "to": [13, 13, 16], "faces": {"north": {"texture": "#glass", "uv": [0, 0, 10, 10]}, "south": {"texture": "#glass", "uv": [0, 0, 10, 10]}}},
            {"from": [0, 3, 3], "to": [1, 13, 13], "faces": {"west": {"texture": "#glass", "uv": [0, 0, 10, 10]}, "east": {"texture": "#glass", "uv": [0, 0, 10, 10]}}},
            {"from": [15, 3, 3], "to": [16, 13, 13], "faces": {"west": {"texture": "#glass", "uv": [0, 0, 10, 10]}, "east": {"texture": "#glass", "uv": [0, 0, 10, 10]}}},
            # 顶底内板仅绘外露面，避免在与边梁相接的位置重复绘制侧面。
            {"from": [3, 15, 3], "to": [13, 16, 13], "faces": {"up": {"texture": "#side", "uv": [0, 0, 10, 10]}}},
            {"from": [3, 0, 3], "to": [13, 1, 13], "faces": {"down": {"texture": "#side", "uv": [0, 0, 10, 10]}}},
        ]),
        BLOCKS[2]: model({"side": steel, "front": "create_nuclear_industry:block/high_pressure_boiler/water_port_face", "brass": "create_nuclear_industry:block/high_pressure_boiler/controller_face"}, [
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {"north": {"texture": "#front"}, "south": {"texture": "#side"}, "east": {"texture": "#side"}, "west": {"texture": "#side"}, "up": {"texture": "#side"}, "down": {"texture": "#side"}}},
        ]),
        BLOCKS[3]: model({"side": steel, "front": "create_nuclear_industry:block/high_pressure_boiler/steam_port_face", "brass": "create_nuclear_industry:block/high_pressure_boiler/controller_face"}, [
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {"north": {"texture": "#front"}, "south": {"texture": "#side"}, "east": {"texture": "#side"}, "west": {"texture": "#side"}, "up": {"texture": "#side"}, "down": {"texture": "#side"}}},
        ]),
        BLOCKS[4]: model({"side": steel, "front": "create_nuclear_industry:block/high_pressure_boiler/controller_face"}, [
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {"north": {"texture": "#front"}, "south": {"texture": "#side"}, "east": {"texture": "#side"}, "west": {"texture": "#side"}, "up": {"texture": "#side"}, "down": {"texture": "#side"}}},
        ]),
        BLOCKS[5]: model({"side": steel, "body": "create_nuclear_industry:block/high_pressure_boiler/valve_body", "badge": "create_nuclear_industry:block/high_pressure_boiler/safety_badge"}, [
            cube([0, 0, 0], [16, 7, 16], "side"), cube([5, 7, 5], [11, 10, 11], "body"),
            cube([6, 10, 6], [10, 14, 10], "body"), cube([3, 14, 3], [13, 16, 13], "badge"),
        ]),
        BLOCKS[6]: model({"side": steel, "core": "create_nuclear_industry:block/high_pressure_boiler/exchanger_face", "top": "create_nuclear_industry:block/high_pressure_boiler/exchanger_top"}, [
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {"north": {"texture": "#core"}, "south": {"texture": "#core"}, "east": {"texture": "#side"}, "west": {"texture": "#side"}, "up": {"texture": "#top"}, "down": {"texture": "#side"}}},
        ]),
    }
    # 玻璃几何使用原版玻璃贴图；模型需声明半透明层，才能让锅炉内部透视。
    roots[BLOCKS[1]]["render_type"] = "translucent"
    for name, value in roots.items():
        write_json(ASSETS / "models/block" / f"{name}.json", value)
        write_json(ASSETS / "models/item" / f"{name}.json", {"parent": f"create_nuclear_industry:block/{name}"})


def generate_reinforced_steel_recipe() -> None:
    """仅保留钢板底基、坚固板部署和压片的批准两工序配方。"""
    write_json(DATA / "recipe/heat_exchanger/reinforced_steel_plate.json", {
        "type": "create:sequenced_assembly",
        "ingredient": {"tag": "c:plates/steel"},
        "transitional_item": {"id": "create_nuclear_industry:incomplete_reinforced_steel_plate"},
        "sequence": [
            {
                "type": "create:deploying",
                "ingredients": [
                    {"item": "create_nuclear_industry:incomplete_reinforced_steel_plate"},
                    {"item": "create:sturdy_sheet"},
                ],
                "results": [{"id": "create_nuclear_industry:incomplete_reinforced_steel_plate"}],
            },
            {
                "type": "create:pressing",
                "ingredients": [{"item": "create_nuclear_industry:incomplete_reinforced_steel_plate"}],
                "results": [{"id": "create_nuclear_industry:incomplete_reinforced_steel_plate"}],
            },
        ],
        "results": [{"id": "create_nuclear_industry:reinforced_steel_plate"}],
        "loops": 1,
    })


def shapeless(a: str, b: str, result: str) -> dict[str, object]:
    return {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": a}, {"item": b}], "result": {"id": result, "count": 1}}


def generate_recipes() -> None:
    recipes: dict[str, object] = {
        "crafting/high_pressure_boiler_casing": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SBS", "BRB", "SBS"], "key": {"S": {"tag": "c:plates/steel"}, "B": {"item": "create_nuclear_industry:refractory_brick"}, "R": {"item": "create_nuclear_industry:reinforced_steel_plate"}}, "result": {"id": f"{NS}:{BLOCKS[0]}", "count": 8}},
        "crafting/high_pressure_boiler_window": shapeless(f"{NS}:{BLOCKS[0]}", f"{NS}:shielded_glass", f"{NS}:{BLOCKS[1]}"),
        "crafting/high_pressure_boiler_water_port": {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": f"{NS}:{BLOCKS[0]}"}, {"item": "create:fluid_pipe"}, {"item": f"{NS}:pressure_fitting"}], "result": {"id": f"{NS}:{BLOCKS[2]}", "count": 1}},
        "crafting/high_pressure_boiler_steam_port": {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": f"{NS}:{BLOCKS[0]}"}, {"item": f"{NS}:pressure_fitting"}, {"item": f"{NS}:pressure_fitting"}, {"item": f"{NS}:reinforced_steel_plate"}], "result": {"id": f"{NS}:{BLOCKS[3]}", "count": 1}},
        "crafting/boiler_safety_valve": {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": f"{NS}:{BLOCKS[0]}"}, {"item": "create:fluid_valve"}, {"item": f"{NS}:seal_ring"}, {"item": f"{NS}:industrial_sensor"}], "result": {"id": f"{NS}:{BLOCKS[5]}", "count": 1}},
        "crafting/boiler_heat_exchange_section": shapeless(f"{NS}:{BLOCKS[0]}", f"{NS}:nuclear_heat_exchange_bundle", f"{NS}:{BLOCKS[6]}"),
        "mechanical_crafting/high_pressure_boiler_controller": {"type": "create:mechanical_crafting", "accept_mirrored": False, "category": "misc", "key": {"S": {"tag": "c:plates/steel"}, "R": {"item": f"{NS}:reinforced_steel_plate"}, "P": {"item": "create:precision_mechanism"}, "I": {"item": f"{NS}:industrial_sensor"}, "C": {"item": f"{NS}:{BLOCKS[0]}"}}, "pattern": [" SSS ", "SRPRS", "SICIS", "SRPRS", " SSS "], "result": {"id": f"{NS}:{BLOCKS[4]}", "count": 1}, "show_notification": False},
    }
    for path, value in recipes.items():
        write_json(DATA / "recipe" / f"{path}.json", value)


def append_json_array(path: Path, additions: list[str]) -> None:
    value = json.loads(path.read_text(encoding="utf-8"))
    value["replace"] = False
    value["values"] = list(dict.fromkeys([*value["values"], *additions]))
    write_json(path, value)


def generate_loot_and_tags() -> None:
    for name in BLOCKS:
        write_json(DATA / "loot_table/blocks" / f"{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    append_json_array(ROOT / "src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json", [f"{NS}:{name}" for name in BLOCKS])
    append_json_array(ROOT / "src/main/resources/data/minecraft/tags/block/needs_iron_tool.json", [f"{NS}:{name}" for name in BLOCKS])


def append_fluid_atlas_sources() -> None:
    """幂等追加本批still/flow图集项，不替换现有来源。"""
    path = ROOT / "src/main/resources/assets/minecraft/atlases/blocks.json"
    atlas = json.loads(path.read_text(encoding="utf-8"))
    sources = atlas.setdefault("sources", [])
    existing = {(entry.get("type"), entry.get("resource")) for entry in sources}
    for suffix in ("still", "flow"):
        item = ("single", f"{NS}:fluid/supercritical_steam_{suffix}")
        if item not in existing:
            sources.append({"type": item[0], "resource": item[1]})
            existing.add(item)
    write_json(path, atlas)


def export_textures() -> list[dict[str, str]]:
    """只导出本任务允许修改的十张锅炉方块纹理，不触碰蒸汽资源。"""
    output = []
    for name, pattern in PATTERNS.items():
        source = SOURCE / f"{name}.svg"
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_text(svg_for(pattern), encoding="utf-8")
        png = strict_exporter.render_svg(source.read_text(encoding="utf-8"), set(COLORS.values()), (16, 16))
        target = ASSETS / "textures/block/high_pressure_boiler" / f"{name}.png"
        target.parent.mkdir(parents=True, exist_ok=True)
        png.save(target)
        output.append({"source": source.relative_to(ROOT).as_posix(), "texture": target.relative_to(ROOT).as_posix(), "size": "16x16 RGBA"})
    return output


def render_preview() -> Path:
    """生成当前反应堆/换热器与锅炉纹理同尺寸的对照图。"""
    references = [
        ("反应堆外壳侧面", ASSETS / "textures/block/reactor_casing_side.png"),
        ("反应堆外壳顶面", ASSETS / "textures/block/reactor_casing_top.png"),
        ("反应堆观察窗", ASSETS / "textures/block/reactor_window.png"),
        ("核换热器面板", ASSETS / "textures/block/nuclear_heat_exchanger/panel.png"),
        ("核换热器黄铜", ASSETS / "textures/block/nuclear_heat_exchanger/brass.png"),
        ("核换热器深钢", ASSETS / "textures/block/nuclear_heat_exchanger/steel_dark.png"),
        ("核换热器热面", ASSETS / "textures/block/nuclear_heat_exchanger/hot_light.png"),
        ("反应堆外壳侧面", ASSETS / "textures/block/reactor_casing_side.png"),
    ]
    boiler = [
        ("高压锅炉外壳", "casing_side"), ("高压锅炉顶面", "casing_top"),
        ("观察窗金属框", "window_frame"), ("给水口", "water_port_face"),
        ("蒸汽口", "steam_port_face"), ("控制器", "controller_face"),
        ("安全阀阀体 / 警示牌", "valve_body"), ("换热段", "exchanger_face"),
    ]
    width, row_height, swatch = 1160, 86, 64
    image = Image.new("RGB", (width, 72 + len(boiler) * row_height), "#20282c")
    draw = ImageDraw.Draw(image)
    try:
        font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 15)
        heading = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 18)
    except OSError:
        font = heading = ImageFont.load_default()
    draw.text((24, 12), "EXT-B-BOILER-01B | 当前游戏材质风格对照（纹理样本，不是游戏模型渲染）", fill="#e2e8e8", font=heading)
    draw.text((24, 46), "已认可反应堆 / 核换热器", fill="#aebfc1", font=font)
    draw.text((600, 46), "锅炉01B", fill="#aebfc1", font=font)
    for index, ((ref_name, ref_path), (boiler_name, texture_name)) in enumerate(zip(references, boiler)):
        y = 72 + index * row_height
        draw.line((18, y, width - 18, y), fill="#354247", width=1)
        ref = Image.open(ref_path).convert("RGBA").resize((swatch, swatch), Image.Resampling.NEAREST)
        sample = Image.open(ASSETS / "textures/block/high_pressure_boiler" / f"{texture_name}.png").convert("RGBA").resize((swatch, swatch), Image.Resampling.NEAREST)
        image.paste(ref, (24, y + 10), ref)
        draw.text((102, y + 34), ref_name, fill="#d4dede", font=font)
        image.paste(sample, (600, y + 10), sample)
        if index == 6:
            extra_name, label = "safety_badge", "阀体与警示牌"
            extra = Image.open(ASSETS / "textures/block/high_pressure_boiler" / f"{extra_name}.png").convert("RGBA").resize((swatch, swatch), Image.Resampling.NEAREST)
            image.paste(extra, (674, y + 10), extra)
            label_x = 752
        elif index == 7:
            extra_name, label = "exchanger_top", "换热段侧面与顶面"
            extra = Image.open(ASSETS / "textures/block/high_pressure_boiler" / f"{extra_name}.png").convert("RGBA").resize((swatch, swatch), Image.Resampling.NEAREST)
            image.paste(extra, (674, y + 10), extra)
            label_x = 752
        else:
            extra_name, label = "", boiler_name
            label_x = 678
        draw.text((label_x, y + 34), label, fill="#d4dede", font=font)
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    preview = EVIDENCE / "boiler-reactor-style-comparison.png"
    image.save(preview)
    return preview


def validate_assets() -> list[str]:
    """静态核对本批JSON语法、方块资源引用与贴图尺寸。"""
    errors: list[str] = []
    for root in (ASSETS / "blockstates", ASSETS / "models/block", ASSETS / "models/item", DATA / "recipe", DATA / "loot_table/blocks"):
        for path in root.rglob("*.json"):
            if path.stem not in BLOCKS and not (root == DATA / "recipe" and path.stem in {"high_pressure_boiler_casing", "high_pressure_boiler_window", "high_pressure_boiler_water_port", "high_pressure_boiler_steam_port", "boiler_safety_valve", "boiler_heat_exchange_section", "high_pressure_boiler_controller"}):
                continue
            try:
                data = json.loads(path.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError) as exc:
                errors.append(f"JSON读取失败 {path.relative_to(ROOT)}: {exc}")
                continue
            if root == ASSETS / "models/item" and data.get("parent") != f"create_nuclear_industry:block/{path.stem}":
                errors.append(f"物品模型未继承块模型: {path.relative_to(ROOT)}")
            if root == ASSETS / "blockstates":
                variants = data.get("variants", {})
                expected = {"facing=north", "facing=east", "facing=south", "facing=west"} if path.stem in (BLOCKS[2], BLOCKS[3], BLOCKS[4]) else {""}
                if set(variants) != expected:
                    errors.append(f"方块状态与朝向约定不符: {path.relative_to(ROOT)}")
            if root == ASSETS / "models/block":
                for texture in data.get("textures", {}).values():
                    if texture.startswith("create_nuclear_industry:"):
                        rel = texture.split(":", 1)[1]
                        if not (ASSETS / "textures" / rel).with_suffix(".png").is_file():
                            errors.append(f"模型贴图缺失 {path.relative_to(ROOT)} -> {texture}")
                elements = data.get("elements", [])
                for element in elements:
                    if len(element.get("from", [])) != 3 or len(element.get("to", [])) != 3 or any(a < 0 or b > 16 or a >= b for a, b in zip(element["from"], element["to"])):
                        errors.append(f"模型元素越界或尺寸无效: {path.relative_to(ROOT)}")
                    for face_data in element.get("faces", {}).values():
                        uv = face_data.get("uv")
                        if uv is not None and (len(uv) != 4 or any(n < 0 or n > 16 for n in uv)):
                            errors.append(f"UV超出0..16: {path.relative_to(ROOT)}")
                for i, first in enumerate(elements):
                    for second in elements[i + 1:]:
                        a0, a1 = first["from"], first["to"]
                        b0, b1 = second["from"], second["to"]
                        if all(max(a0[axis], b0[axis]) < min(a1[axis], b1[axis]) for axis in range(3)):
                            errors.append(f"模型元素存在体积重叠: {path.relative_to(ROOT)} [{a0}..{a1}] [{b0}..{b1}]")
                # 只比较方块外表面：边界面在同一平面有正面积交叠即会成为共面重复面。
                boundary_faces = (("west", 0, 0, 0), ("east", 0, 16, 1), ("down", 1, 0, 0), ("up", 1, 16, 1), ("north", 2, 0, 0), ("south", 2, 16, 1))
                for i, first in enumerate(elements):
                    for second in elements[i + 1:]:
                        for direction, axis, plane, endpoint in boundary_faces:
                            if direction not in first.get("faces", {}) or direction not in second.get("faces", {}):
                                continue
                            first_plane = first["to"][axis] if endpoint else first["from"][axis]
                            second_plane = second["to"][axis] if endpoint else second["from"][axis]
                            if first_plane != plane or second_plane != plane:
                                continue
                            projected = [dim for dim in range(3) if dim != axis]
                            if all(max(first["from"][dim], second["from"][dim]) < min(first["to"][dim], second["to"][dim]) for dim in projected):
                                errors.append(f"方块外表面存在共面重复区域: {path.relative_to(ROOT)} {direction}")
    for path in (ASSETS / "textures/block/high_pressure_boiler").glob("*.png"):
        with Image.open(path) as texture:
            if texture.size != (16, 16) or texture.mode != "RGBA":
                errors.append(f"方块贴图规格错误: {path.relative_to(ROOT)}")
    for path in (ASSETS / "textures/fluid").glob("supercritical_steam_*.png"):
        with Image.open(path) as texture:
            if texture.size != (16, 16) or texture.mode != "RGBA" or texture.getchannel("A").getextrema()[0] != 0:
                errors.append(f"流体贴图应为16x16 RGBA且留透明背景: {path.relative_to(ROOT)}")
    atlas_path = ROOT / "src/main/resources/assets/minecraft/atlases/blocks.json"
    atlas = json.loads(atlas_path.read_text(encoding="utf-8"))
    atlas_sources = {(entry.get("type"), entry.get("resource")) for entry in atlas.get("sources", [])}
    for suffix in ("still", "flow"):
        resource = f"{NS}:fluid/supercritical_steam_{suffix}"
        if ("single", resource) not in atlas_sources:
            errors.append(f"流体贴图未登记默认图集: {resource}")
        if not (ASSETS / "textures/fluid" / f"supercritical_steam_{suffix}.png").is_file():
            errors.append(f"图集登记对应的流体PNG不存在: {resource}")
    for suffix in ("still", "flow"):
        legacy = f"{NS}:fluid/uranium_slurry_{suffix}"
        if ("single", legacy) not in atlas_sources:
            errors.append(f"既有浆料图集项被意外移除: {legacy}")
    recipe_expectations = {
        "crafting/high_pressure_boiler_casing": {"pattern_counts": {"S": 4, "B": 4, "R": 1}, "result_count": 8},
        "crafting/high_pressure_boiler_window": {"ingredients": {"create_nuclear_industry:high_pressure_boiler_casing": 1, "create_nuclear_industry:shielded_glass": 1}},
        "crafting/high_pressure_boiler_water_port": {"ingredients": {"create_nuclear_industry:high_pressure_boiler_casing": 1, "create:fluid_pipe": 1, "create_nuclear_industry:pressure_fitting": 1}},
        "crafting/high_pressure_boiler_steam_port": {"ingredients": {"create_nuclear_industry:high_pressure_boiler_casing": 1, "create_nuclear_industry:pressure_fitting": 2, "create_nuclear_industry:reinforced_steel_plate": 1}},
        "crafting/boiler_safety_valve": {"ingredients": {"create_nuclear_industry:high_pressure_boiler_casing": 1, "create:fluid_valve": 1, "create_nuclear_industry:seal_ring": 1, "create_nuclear_industry:industrial_sensor": 1}},
        "crafting/boiler_heat_exchange_section": {"ingredients": {"create_nuclear_industry:high_pressure_boiler_casing": 1, "create_nuclear_industry:nuclear_heat_exchange_bundle": 1}},
        "mechanical_crafting/high_pressure_boiler_controller": {"pattern": [" SSS ", "SRPRS", "SICIS", "SRPRS", " SSS "], "pattern_counts": {"S": 12, "R": 4, "P": 2, "I": 2, "C": 1}, "mirrored": False},
    }
    for rel, contract in recipe_expectations.items():
        path = DATA / "recipe" / f"{rel}.json"
        try:
            recipe = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            errors.append(f"配方缺失或JSON错误 {path.relative_to(ROOT)}: {exc}")
            continue
        if "ingredients" in contract:
            actual: dict[str, int] = {}
            for ingredient in recipe.get("ingredients", []):
                identity = ingredient.get("item")
                if identity:
                    actual[identity] = actual.get(identity, 0) + 1
            if actual != contract["ingredients"]:
                errors.append(f"配方材料/数量偏离批准方案: {path.relative_to(ROOT)} -> {actual}")
        if "pattern" in contract and recipe.get("pattern") != contract["pattern"]:
            errors.append(f"配方阵列偏离批准方案: {path.relative_to(ROOT)}")
        if "pattern_counts" in contract:
            rows = recipe.get("pattern", [])
            keys = recipe.get("key", {})
            actual = {symbol: sum(row.count(symbol) for row in rows) for symbol in keys}
            if actual != contract["pattern_counts"]:
                errors.append(f"配方材料格数偏离批准方案: {path.relative_to(ROOT)} -> {actual}")
        if "mirrored" in contract and recipe.get("accept_mirrored") is not contract["mirrored"]:
            errors.append(f"动力合成镜像设置错误: {path.relative_to(ROOT)}")
        if "result_count" in contract and recipe.get("result", {}).get("count") != contract["result_count"]:
            errors.append(f"锅炉外壳产出数量错误: {path.relative_to(ROOT)}")
    for name in BLOCKS:
        path = DATA / "loot_table/blocks" / f"{name}.json"
        if not path.is_file():
            errors.append(f"掉落表缺失: {path.relative_to(ROOT)}")
            continue
        loot = json.loads(path.read_text(encoding="utf-8"))
        entries = loot.get("pools", [{}])[0].get("entries", [])
        if len(entries) != 1 or entries[0].get("name") != f"{NS}:{name}" or entries[0].get("type") != "minecraft:item":
            errors.append(f"掉落表未保持单件自身掉落: {path.relative_to(ROOT)}")
    tag_paths = (ROOT / "src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json", ROOT / "src/main/resources/data/minecraft/tags/block/needs_iron_tool.json")
    for tag_path in tag_paths:
        tag = json.loads(tag_path.read_text(encoding="utf-8"))
        missing = {f"{NS}:{name}" for name in BLOCKS} - set(tag.get("values", []))
        if tag.get("replace") is not False or missing:
            errors.append(f"挖掘标签缺少本批方块或replace错误 {tag_path.relative_to(ROOT)}: {sorted(missing)}")
    # 顶部贴图经实际PNG导出后仍须同时满足左右、前后镜像。
    for name in ("casing_top", "exchanger_top"):
        path = ASSETS / "textures/block/high_pressure_boiler" / f"{name}.png"
        with Image.open(path) as top:
            pixels = top.convert("RGBA").load()
            if any(pixels[x, y] != pixels[15 - x, y] for y in range(16) for x in range(16)):
                errors.append(f"顶面PNG左右不对称: {path.relative_to(ROOT)}")
            if any(pixels[x, y] != pixels[x, 15 - y] for y in range(16) for x in range(16)):
                errors.append(f"顶面PNG前后不对称: {path.relative_to(ROOT)}")

    window_path = ASSETS / "models/block" / f"{BLOCKS[1]}.json"
    window_model = json.loads(window_path.read_text(encoding="utf-8"))
    if window_model.get("render_type") != "translucent":
        errors.append("锅炉窗口模型没有声明translucent渲染层")
    panes = [element for element in window_model.get("elements", []) if any(face_data.get("texture") == "#glass" for face_data in element.get("faces", {}).values())]
    expected_panes = {
        ((3, 3, 0), (13, 13, 1), frozenset({"north", "south"})),
        ((3, 3, 15), (13, 13, 16), frozenset({"north", "south"})),
        ((0, 3, 3), (1, 13, 13), frozenset({"west", "east"})),
        ((15, 3, 3), (16, 13, 13), frozenset({"west", "east"})),
    }
    actual_panes = {(tuple(element["from"]), tuple(element["to"]), frozenset(side for side, face_data in element["faces"].items() if face_data.get("texture") == "#glass")) for element in panes}
    if actual_panes != expected_panes:
        errors.append("锅炉窗口玻璃必须保留四面独立嵌入几何")
    if window_model.get("textures", {}).get("glass") != "minecraft:block/glass":
        errors.append("锅炉窗口未沿用原版透明玻璃贴图")

    plate_path = DATA / "recipe/heat_exchanger/reinforced_steel_plate.json"
    try:
        plate_recipe = json.loads(plate_path.read_text(encoding="utf-8"))
        sequence = plate_recipe.get("sequence", [])
        step_types = [step.get("type") for step in sequence]
        deploy_items = [entry.get("item") for entry in sequence[0].get("ingredients", [])] if sequence else []
        if plate_recipe.get("type") != "create:sequenced_assembly" or plate_recipe.get("ingredient") != {"tag": "c:plates/steel"}:
            errors.append("强化钢板起始材料或原生装配类型偏离合同")
        if step_types != ["create:deploying", "create:pressing"]:
            errors.append(f"强化钢板工序必须为部署后压片: {step_types}")
        if deploy_items != ["create_nuclear_industry:incomplete_reinforced_steel_plate", "create:sturdy_sheet"]:
            errors.append(f"坚固板部署输入错误: {deploy_items}")
        if plate_recipe.get("loops") != 1 or plate_recipe.get("results") != [{"id": "create_nuclear_industry:reinforced_steel_plate"}]:
            errors.append("强化钢板必须单轮产出1件且保持原成品ID")
        if "create:precision_mechanism" in json.dumps(plate_recipe, ensure_ascii=False):
            errors.append("强化钢板配方仍引用精密构件")
        expected_intermediate = "create_nuclear_industry:incomplete_reinforced_steel_plate"
        if any(step.get("results") != [{"id": expected_intermediate}] for step in sequence):
            errors.append("强化钢板两工序必须保留原过渡件且不生成额外副产物")
    except (OSError, json.JSONDecodeError) as exc:
        errors.append(f"强化钢板配方缺失或JSON错误: {exc}")
    return errors


def digest(path: Path) -> str:
    """返回文件SHA-256，用于复现本批导出清单。"""
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_report(errors: list[str], preview: Path) -> None:
    report = f"""# EXT-B-BOILER-01B A：锅炉外观与强化钢板配方

- 基线：`9e8690b`；候选：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。执行时未进行任何Git写操作。
- 已读取技能：`minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`。本执行者只改任务A的SVG、锅炉纹理、七方块模型、锅炉专用素材生成器和强化钢板配方；未改Java/测试、换热器模型或01A以外资源。
- 外观：色板复用当前反应堆的蓝灰钢板、浅钢边、黄铜角铆钉；锅炉壳顶和换热段顶的导出PNG均按水平及垂直两轴逐像素镜像。窗口模型声明`render_type: translucent`，保持四个嵌入式玻璃片和既有窗口尺寸/朝向/物品父模型。
- 强化钢板：原配方ID、过渡件ID、钢板标签输入、成品ID、单轮1件均保留；当前序列为`create:deploying`加1坚固板，随后`create:pressing`，不再引用精密构件。
- 预览：[反应堆/核换热器与锅炉纹理同图对照](./EXT-B-BOILER-01B-ASSETS/boiler-reactor-style-comparison.png)按相同像素放大比例展示参考与锅炉素材；它是纹理样本，不能替代Minecraft实际模型渲染。生成文件及SHA-256见[assets.json](./EXT-B-BOILER-01B-ASSETS/assets.json)。
- 可复现导出：`C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe tools/art-assets/boiler_01a_assets.py`。此入口只写A范围中的十张锅炉SVG/PNG、七方块块/物品模型、强化钢板配方及01B证据，不会重写锅炉旧配方、蒸汽纹理/图集或标签。
- 本地静态检查：{len(errors)}个错误；覆盖JSON与引用、7个方块模型/物品模型、模型元素边界/相交/外表面共面、PNG尺寸、窗口半透明层与四面嵌入结构、顶面双轴镜像、两工序材料/数量/副产物与精密构件移除。
- 差异核查：A写集`git diff --check`通过；以`9e8690b`为基线核对旧锅炉配方、蒸汽纹理/SVG、流体图集、标签和掉落表，差异为空。其他执行者与用户已有的工作区改动均保留。
- Create `6.0.10-280`原生进度边界（静态源码核对，未加载游戏）：过渡件保存原生配方ID和整数`step`，不是已解析工序快照；旧工序数为3，新工序数为2。同配方ID下，旧step=0或1可依次映射到新序列；旧step=2会按`step % sequence.size()`回到新序列第0步，并可能在下一次操作后完成而未经过压片。旧step=2表示坚固板和精密构件步骤已执行，不会免付坚固板，但既有在途件的最终压片语义不能证明兼容。候选档案不做自动迁移；该边界需由PM安排手测/清空或补兼容策略后再宣称旧在途件安全。
- 未运行Gradle、JUnit、GameTest或客户端；窗口在真实世界中的透视仍待D/用户客户端复测。D继续负责Java/结构/管线范围；本报告不处理新提出的换热器定向与共享库存设计。

"""
    if errors:
        report += "\n静态错误：\n\n" + "\n".join(f"- `{error}`" for error in errors) + "\n"
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(report, encoding="utf-8")


def main() -> None:
    textures = export_textures()
    generate_models()
    generate_reinforced_steel_recipe()
    preview = render_preview()
    errors = validate_assets()
    model_files = [ASSETS / "models" / kind / f"{name}.json" for name in BLOCKS for kind in ("block", "item")]
    recipe_file = DATA / "recipe/heat_exchanger/reinforced_steel_plate.json"
    outputs = [Path(item["texture"]) for item in textures]
    outputs.extend(Path(item["source"]) for item in textures)
    outputs.extend(path.relative_to(ROOT) for path in model_files)
    outputs.append(recipe_file.relative_to(ROOT))
    outputs.append(preview.relative_to(ROOT))
    file_entries = [{"path": path.as_posix(), "sha256": digest(ROOT / path)} for path in outputs]
    top_checks = {name: True for name in ("casing_top", "exchanger_top")}
    write_json(EVIDENCE / "assets.json", {
        "baseline": "9e8690b",
        "textures": textures,
        "preview": preview.relative_to(ROOT).as_posix(),
        "blocks": list(BLOCKS),
        "symmetricTopTextures": top_checks,
        "files": file_entries,
    })
    write_report(errors, preview)
    if errors:
        raise SystemExit("素材静态核查失败：\n" + "\n".join(errors))
    print(f"EXT-B-BOILER-01B A静态检查通过；生成{len(textures)}张锅炉纹理、14个方块/物品模型；预览：{preview}")


if __name__ == "__main__":
    sys.dont_write_bytecode = True
    main()
