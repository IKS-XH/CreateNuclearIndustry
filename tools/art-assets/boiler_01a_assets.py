"""EXT-B-BOILER-01A锅炉方块素材、模型与配方的可复现导出入口。"""
from __future__ import annotations

import json
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
EVIDENCE = ROOT / "build/reports/extension/EXT-B-BOILER-01A-ASSETS"
REPORT = ROOT / "build/reports/extension/EXT-B-BOILER-01A-ASSETS.md"
NS = "create_nuclear_industry"

# 行字符是像素调色索引；每张16×16纹理均由这些明确的像素格复现。
COLORS = {
    ".": "#263238", "s": "#53676B", "m": "#788E94", "l": "#A3B7B8",
    "w": "#D1DFDA", "d": "#3E5058", "k": "#182329", "b": "#D0B66B",
    "g": "#9A8249", "o": "#C36B28", "a": "#F1A642", "c": "#79C8CC",
    "t": "#B7F4E8", "x": "#303E42", "r": "#C87942",
}
PATTERNS: dict[str, list[str]] = {
    "casing_side": [
        "kkkkkkkkkkkkkkkk", "kwwwwwwwwwwwwwwk", "kwmmmmmmmmmmmmwk", "kwmddddddddddmwk",
        "kwmd........dmwk", "kwmd........dmwk", "kwmd..kkkk..dmwk", "kwmd..kwwk..dmwk",
        "kwmd..kwwk..dmwk", "kwmd..kkkk..dmwk", "kwmd........dmwk", "kwmd........dmwk",
        "kwmddddddddddmwk", "kwmmmmmmmmmmmmwk", "kwwwwwwwwwwwwwwk", "kkkkkkkkkkkkkkkk",
    ],
    "casing_top": [
        "kkkkkkkkkkkkkkkk", "kwwwwwwwwwwwwwwk", "kwmmmmmmmmmmmmwk", "kwmddddddddddmwk",
        "kwmd............", "kwmd............", "kwmd............", "kwmd............",
        "kwmd............", "kwmd............", "kwmd............", "kwmd............",
        "kwmddddddddddmwk", "kwmmmmmmmmmmmmwk", "kwwwwwwwwwwwwwwk", "kkkkkkkkkkkkkkkk",
    ],
    "window_frame": [
        "kkkkkkkkkkkkkkkk", "kwwwwwwwwwwwwwwk", "kwmmmmmmmmmmmmwk", "kwmdkkkkkkkkdmwk",
        "kwmdkkkkkkkkdmwk", "kwmdkcccccckdmwk", "kwmdkcttttckdmwk", "kwmdkctxxckdmwk",
        "kwmdkctxxckdmwk", "kwmdkcttttckdmwk", "kwmdkcccccckdmwk", "kwmdkkkkkkkkdmwk",
        "kwmdkkkkkkkkdmwk", "kwmmmmmmmmmmmmwk", "kwwwwwwwwwwwwwwk", "kkkkkkkkkkkkkkkk",
    ],
    "water_port_face": [
        "kkkkkkkkkkkkkkkk", "kwwwwwwwwwwwwwwk", "kwmmmmmmmmmmmmwk", "kwmd........dmwk",
        "kwmd..kkkk..dmwk", "kwmd.kwwwwk.dmwk", "kwmdkwmmmmwkdmwk", "kwmdwm......wdmwk",
        "kwmdwm......wdmwk", "kwmdkwmmmmwkdmwk", "kwmd.kwwwwk.dmwk", "kwmd..kkkk..dmwk",
        "kwmd........dmwk", "kwmmmmmmmmmmmmwk", "kwwwwwwwwwwwwwwk", "kkkkkkkkkkkkkkkk",
    ],
    "steam_port_face": [
        "kkkkkkkkkkkkkkkk", "kwwwwwwwwwwwwwwk", "kwmmmmmmmmmmmmwk", "kwmd........dmwk",
        "kwmd..kkkk..dmwk", "kwmd.kwwwwk.dmwk", "kwmdkwbbbkkkdmwk", "kwmdbb....kkdmwk",
        "kwmdbb....kkdmwk", "kwmdkwbbbkkkdmwk", "kwmd.kwwwwk.dmwk", "kwmd..kkkk..dmwk",
        "kwmd........dmwk", "kwmmmmmmmmmmmmwk", "kwwwwwwwwwwwwwwk", "kkkkkkkkkkkkkkkk",
    ],
    "controller_face": [
        "kkkkkkkkkkkkkkkk", "kwwwwwwwwwwwwwwk", "kwmmmmmmmmmmmmwk", "kwmd........dmwk",
        "kwmd..bbbb..dmwk", "kwmd.bgggbb.dmwk", "kwmd.bgggbb.dmwk", "kwmd..bbbb..dmwk",
        "kwmd....a...dmwk", "kwmd..dddd..dmwk", "kwmd..dwwd..dmwk", "kwmd..dddd..dmwk",
        "kwmd........dmwk", "kwmmmmmmmmmmmmwk", "kwwwwwwwwwwwwwwk", "kkkkkkkkkkkkkkkk",
    ],
    "valve_body": [
        "................", "................", ".......kk.......", "......kwwk......",
        "......kmmk......", ".....kwmmwk.....", "....kwwmmwwk....", "....kwmd.dmwk....",
        "....kwmd.dmwk....", "....kwwmmwwk....", ".....kwmmwk.....", "......kmmk......",
        "......kwwk......", ".......kk.......", "................", "................",
    ],
    "exchanger_face": [
        "kkkkkkkkkkkkkkkk", "kwwwwwwwwwwwwwwk", "kwmmmmmmmmmmmmwk", "kwmddddddddddmwk",
        "kwmd........dmwk", "kwmd.kkkkkkkk.dmwk", "kwmd.kwwwwwwk.dmwk", "kwmd.kmxxxxmk.dmwk",
        "kwmd.kmxxxxmk.dmwk", "kwmd.kmxxxxmk.dmwk", "kwmd.kmxxxxmk.dmwk", "kwmd.kwwwwwwk.dmwk",
        "kwmd.kkkkkkkk.dmwk", "kwmddddddddddmwk", "kwmmmmmmmmmmmmwk", "kkkkkkkkkkkkkkkk",
    ],
    "exchanger_top": [
        "kkkkkkkkkkkkkkkk", "kwwwwwwwwwwwwwwk", "kwmmmmmmmmmmmmwk", "kwmddddddddddmwk",
        "kwmd.kkkkkkkk.dmwk", "kwmd.kwwwwwwk.dmwk", "kwmd.kmxxxxmk.dmwk", "kwmd.kmxxxxmk.dmwk",
        "kwmd.kmxxxxmk.dmwk", "kwmd.kmxxxxmk.dmwk", "kwmd.kmxxxxmk.dmwk", "kwmd.kmxxxxmk.dmwk",
        "kwmd.kkkkkkkk.dmwk", "kwmddddddddddmwk", "kwmmmmmmmmmmmmwk", "kkkkkkkkkkkkkkkk",
    ],
    "safety_badge": [
        "................", "................", "......bbbb......", ".....bwwwwb.....",
        "....bwwwwwwb....", "....bwwaawwgb....", "....bwwaawwgb....", "....bwwaawwgb....",
        "....bwwaawwgb....", "....bwwwwwwb....", ".....bwwwwb.....", "......bbbb......",
        "................", "................", "................", "................",
    ],
}

BLOCKS = (
    "high_pressure_boiler_casing", "high_pressure_boiler_window", "high_pressure_boiler_water_port",
    "high_pressure_boiler_steam_port", "high_pressure_boiler_controller", "boiler_safety_valve",
    "boiler_heat_exchange_section",
)


def normalize_rows(pattern: list[str]) -> list[str]:
    """将简写行以中心对齐到16像素，拒绝超出少量排版误差的输入。"""
    if len(pattern) != 16 or any(len(row) < 14 or len(row) > 18 for row in pattern):
        raise ValueError("纹理需有16行，每行长度为14至18个像素符号")
    rows = []
    for row in pattern:
        if len(row) < 16:
            left = (16 - len(row)) // 2
            row = "." * left + row + "." * (16 - len(row) - left)
        elif len(row) > 16:
            left = (len(row) - 16) // 2
            row = row[left:left + 16]
        rows.append(row)
    return rows


def svg_for(pattern: list[str]) -> str:
    """把像素格转为严格导出器支持的无样式SVG矩形。"""
    pattern = normalize_rows(pattern)
    rects = []
    for y, row in enumerate(pattern):
        for x, code in enumerate(row):
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
    for name, value in roots.items():
        write_json(ASSETS / "models/block" / f"{name}.json", value)
        write_json(ASSETS / "models/item" / f"{name}.json", {"parent": f"create_nuclear_industry:block/{name}"})
    for name in (BLOCKS[2], BLOCKS[3], BLOCKS[4]):
        variants = {f"facing={direction}": {"model": f"create_nuclear_industry:block/{name}", "y": rotation}
                    for direction, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}
        write_json(ASSETS / "blockstates" / f"{name}.json", {"variants": variants})
    for name in (BLOCKS[0], BLOCKS[1], BLOCKS[5], BLOCKS[6]):
        write_json(ASSETS / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"create_nuclear_industry:block/{name}"}}})


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
    fluid_patterns = {
        "supercritical_steam_still": [
            "................", "................", "....tttt........", "...tccccct......",
            "..tcttttct......", "...tccccct......", "....tttt........", "...........tt...",
            "..........tccct.", ".........tctttct", "..........tccct.", "...........tt...",
            "................", "................", "................", "................",
        ],
        "supercritical_steam_flow": [
            "....tt.........t", "...tccct......tc", "..tctttct....tct", "...tccct......tc",
            "....tt.........t", "........tt......", ".......tccct.....", "......tctttct....",
            ".......tccct.....", "........tt.......", "....tt...........", "...tccct.........",
            "..tctttct........", "...tccct.........", "....tt...........", "................",
        ],
    }
    for name, pattern in fluid_patterns.items():
        source = FLUID_SOURCE / f"{name}.svg"
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_text(fluid_svg_for(pattern), encoding="utf-8")
        png = strict_exporter.render_svg(source.read_text(encoding="utf-8"), set(COLORS.values()), (16, 16))
        target = ASSETS / "textures/fluid" / f"{name}.png"
        target.parent.mkdir(parents=True, exist_ok=True)
        png.save(target)
        output.append({"source": source.relative_to(ROOT).as_posix(), "texture": target.relative_to(ROOT).as_posix(), "size": "16x16 RGBA"})
    return output


def render_preview() -> Path:
    width, height = 1140, 470
    image = Image.new("RGB", (width, height), "#e5e9e9")
    draw = ImageDraw.Draw(image)
    font = ImageFont.load_default()
    draw.text((22, 18), "EXT-B-BOILER-01A | Texture and block silhouette reference | illustrative only, not a game-model render", fill="#172329", font=font)
    for index, name in enumerate(BLOCKS):
        col, row = index % 4, index // 4
        x, y = 35 + col * 278, 65 + row * 220
        draw.text((x, y), name, fill="#172329", font=font)
        face_name = {BLOCKS[0]: "casing_side", BLOCKS[1]: "window_frame", BLOCKS[2]: "water_port_face", BLOCKS[3]: "steam_port_face", BLOCKS[4]: "controller_face", BLOCKS[5]: "safety_badge", BLOCKS[6]: "exchanger_face"}[name]
        tex = Image.open(ASSETS / "textures/block/high_pressure_boiler" / f"{face_name}.png").convert("RGBA").resize((96, 96), Image.Resampling.NEAREST)
        front_x, front_y = x + 28, y + 47
        draw.polygon([(front_x, front_y), (front_x + 24, front_y - 20), (front_x + 120, front_y - 20), (front_x + 96, front_y)], fill="#9aa9aa", outline="#182329")
        draw.polygon([(front_x + 96, front_y), (front_x + 120, front_y - 20), (front_x + 120, front_y + 76), (front_x + 96, front_y + 96)], fill="#3e5058", outline="#182329")
        draw.rectangle((front_x - 1, front_y - 1, front_x + 96, front_y + 96), fill="#263238", outline="#182329")
        image.paste(tex, (front_x, front_y), tex)
        draw.text((x + 22, y + 165), "texture sample / shape diagram only", fill="#475256", font=font)
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    preview = EVIDENCE / "boiler-block-assets-preview.png"
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
    return errors


def write_report(errors: list[str], preview: Path) -> None:
    report = f"""# EXT-B-BOILER-01A 素材与配方交付

- 基线：`8bf423d`，候选工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`
- 技能：`minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack` 均已读取；本任务只提交原生资源、素材和离线静态证据。
- 内容：七个锅炉方块的 blockstate、块/物品模型、12张PNG纹理和对应SVG；超临界蒸汽 still/flow 贴图；七条配方、七张单件掉落表；镐挖掘与铁工具标签追加。
- 流体图集：`assets/minecraft/atlases/blocks.json`登记超临界蒸汽still/flow两项，并保留原铀浆料两项。
- 预览：[锅炉纹理与轮廓示意](./EXT-B-BOILER-01A-ASSETS/boiler-block-assets-preview.png)只是正面纹理和方块剪影参考，不是游戏模型渲染；导出清单：[assets.json](./EXT-B-BOILER-01A-ASSETS/assets.json)。
- 导出：`C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe tools/art-assets/boiler_01a_assets.py`。
- 静态核查：脚本检查本批JSON语法、水平朝向、模型/纹理引用、元素边界/体积交叠/外表面共面重复、显式UV范围、PNG规格、六条工作台配方材料与数量、控制器5×5格数/禁镜像及七张单件掉落表；错误数：{len(errors)}。
- 未运行 Gradle、客户端、服务端或 Create 实际配方加载。游戏内透明层表现、方块朝向、掉落及真实工作台/动力合成加载仍待设备执行者接线与人工验收。
- 控制器普通掉落仅一件，不含库存NBT；库存快照由设备实现接入。

"""
    if errors:
        report += "\n静态错误：\n\n" + "\n".join(f"- `{error}`" for error in errors) + "\n"
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(report, encoding="utf-8")


def main() -> None:
    textures = export_textures()
    generate_models()
    generate_recipes()
    generate_loot_and_tags()
    append_fluid_atlas_sources()
    preview = render_preview()
    errors = validate_assets()
    write_report(errors, preview)
    write_json(EVIDENCE / "assets.json", {"textures": textures, "preview": preview.relative_to(ROOT).as_posix(), "blocks": list(BLOCKS)})
    if errors:
        raise SystemExit("素材静态核查失败：\n" + "\n".join(errors))
    print(f"已导出 {len(textures)} 张锅炉纹理、七组模型/方块状态/配方/掉落表；预览：{preview}")


if __name__ == "__main__":
    sys.dont_write_bytecode = True
    main()
