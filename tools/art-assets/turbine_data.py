"""准备并安装汽轮机配方、掉落、语言、工具标签和普通蒸汽纹理。"""
from __future__ import annotations

import argparse
import json
import shutil
import xml.etree.ElementTree as ET
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[1]
REPORT = REPO / "build/reports/extension/EXT-B-TURBINE-ASSETS-01"
STAGING = REPORT / "data_resources"
SVG_DIR = ROOT / "svg/fluid/steam"
RESOURCE = REPO / "src/main/resources"
NAMESPACE = "create_nuclear_industry"
BLOCKS = (
    "turbine_casing", "turbine_rotor", "turbine_controller",
    "turbine_output_shaft", "turbine_inlet", "turbine_exhaust",
)
REQUIRED = tuple(f"{NAMESPACE}:{block}" for block in BLOCKS)
SVG_COLORS = {"A": "#B9D9DE", "B": "#E7F4F3", "S": "#6D9AA3", ".": None}
STEAM_ROWS = {
    "steam_still": [
        "................", "..AAAAAA........", ".ABBBBBA........", ".ABAAAAB........",
        ".ABAAAAB........", ".ABBBBBA........", "..AAAAAA........", "................",
        "...........AAAA.", "..........ABBBBA", "..........ABAAAB", "..........ABAAAB",
        "..........ABBBBA", "...........AAAA.", "................", "................",
    ],
    "steam_flow": [
        "................", "..SS............", ".ABAS...........", "..ABAS..........",
        "...ABAS.........", "....ABAS........", ".....ABAS.......", "......ABAS......",
        ".......ABAS.....", "........ABAS....", ".........ABAS...", "..........ABAS..",
        "...........ABAS.", "............ABA.", "............A...", "................",
    ],
}


def svg_text(rows: list[str]) -> str:
    if len(rows) != 16 or any(len(row) != 16 for row in rows):
        raise ValueError("普通蒸汽SVG必须为16×16像素")
    rects = []
    for y, row in enumerate(rows):
        for x, color in enumerate(row):
            value = SVG_COLORS[color]
            if value:
                rects.append(f'<rect x="{x}" y="{y}" width="1" height="1" fill="{value}"/>')
    return ('<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" '
            'viewBox="0 0 16 16" shape-rendering="crispEdges">\n' +
            "\n".join(rects) + "\n</svg>\n")


def render_steam() -> tuple[dict[str, bytes], list[str]]:
    SVG_DIR.mkdir(parents=True, exist_ok=True)
    sources = []
    images = {}
    for name, rows in STEAM_ROWS.items():
        path = SVG_DIR / f"{name}.svg"
        if not path.exists():
            path.write_text(svg_text(rows), encoding="utf-8", newline="\n")
            sources.append(path.name)
        text = path.read_text(encoding="utf-8")
        if "<!DOCTYPE" in text.upper() or "<!ENTITY" in text.upper() or "<?" in text:
            raise ValueError(f"禁止在蒸汽SVG中使用外部实体或处理指令: {path}")
        root = ET.fromstring(text)
        if root.tag != "{http://www.w3.org/2000/svg}svg" or root.attrib != {
            "width": "16", "height": "16", "viewBox": "0 0 16 16", "shape-rendering": "crispEdges"
        }:
            raise ValueError(f"普通蒸汽SVG根节点或尺寸不正确: {path}")
        image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        pixels = image.load()
        for node in root:
            if node.tag != "{http://www.w3.org/2000/svg}rect" or set(node.attrib) != {"x", "y", "width", "height", "fill"}:
                raise ValueError(f"只允许普通蒸汽SVG整数像素rect: {path}")
            x, y, width, height = (int(node.get(key, "-1")) for key in ("x", "y", "width", "height"))
            fill = node.get("fill", "").upper()
            if fill not in SVG_COLORS.values() or x < 0 or y < 0 or width != 1 or height != 1 or x >= 16 or y >= 16:
                raise ValueError(f"普通蒸汽SVG像素非法: {path}")
            rgb = tuple(int(fill[i:i + 2], 16) for i in (1, 3, 5)) + (255,)
            pixels[x, y] = rgb
        if len(root) == 0 or image.getbbox() is None:
            raise ValueError(f"普通蒸汽SVG不得为空: {path}")
        images[name] = image
    files = {}
    for name, image in images.items():
        from io import BytesIO
        buffer = BytesIO()
        image.save(buffer, format="PNG", optimize=False)
        files[f"assets/{NAMESPACE}/textures/fluid/{name}.png"] = buffer.getvalue()
    preview = Image.new("RGBA", (128, 80), "#26353B")
    preview.alpha_composite(images["steam_still"].resize((48, 48), Image.Resampling.NEAREST), (8, 8))
    preview.alpha_composite(images["steam_flow"].resize((48, 48), Image.Resampling.NEAREST), (72, 8))
    REPORT.mkdir(parents=True, exist_ok=True)
    preview.save(REPORT / "ordinary-steam-preview.png", format="PNG", optimize=False)
    return files, sources


def translations() -> dict[str, dict[str, str]]:
    zh = {
        "turbine_casing": "汽轮机机壳", "turbine_rotor": "汽轮机转子",
        "turbine_controller": "汽轮机控制器", "turbine_output_shaft": "汽轮机后输出轴",
        "turbine_inlet": "汽轮机进汽口", "turbine_exhaust": "汽轮机排汽口",
        "steam": "普通蒸汽",
    }
    en = {
        "turbine_casing": "Steam Turbine Casing", "turbine_rotor": "Steam Turbine Rotor",
        "turbine_controller": "Steam Turbine Controller", "turbine_output_shaft": "Steam Turbine Rear Shaft",
        "turbine_inlet": "Steam Turbine Inlet", "turbine_exhaust": "Steam Turbine Exhaust",
        "steam": "Steam",
    }
    zh_gui = {
        "state.running": "正在运行", "state.no_steam": "入口库存不足，等待蒸汽",
        "state.exhaust_full": "排汽库存已满", "state.stock_over_capacity": "库存超过当前配置容量",
        "state.redstone": "红石信号暂停", "state.unformed": "结构未成型",
        "state.invalid_config": "汽轮机配置无效，已停机",
        "state.overlap": "机组构件与另一台已成型汽轮机重叠",
        "inspect": "%s：检查位置 %s, %s, %s",
        "issue.chunk": "区块未加载", "issue.controller": "控制器位置错误或缺失",
        "issue.length": "轴向长度不属于当前配置档位", "issue.axis": "转子轴心或轴列位置错误",
        "issue.facing": "机壳朝向不一致", "issue.port_facing": "端口朝向不匹配",
        "issue.end": "前后端部件位置错误", "issue.ring": "八棱机壳位置错误",
        "issue.inlet": "进汽口缺失或位置错误", "issue.exhaust": "排汽口缺失或位置错误",
        "issue.overlap": "此构件已属于另一台汽轮机",
        "formed": "结构已成型：%s 个转子", "wait_stock": "请检查库存容量与汽轮机配置",
        "hint": "空载仍消耗蒸汽；红石可停止汽轮机",
        "rotors_rpm": "转子数 %s；转速 %s RPM", "flow": "实际处理 %s / 额定 %s mB/tick",
        "tanks": "入口 %s/%s mB；排汽 %s/%s mB", "su": "总 %s SU（前 %s；后 %s）",
    }
    en_gui = {
        "state.running": "Running", "state.no_steam": "Waiting for inlet steam",
        "state.exhaust_full": "Exhaust stock is full", "state.stock_over_capacity": "Stock exceeds configured capacity",
        "state.redstone": "Paused by redstone", "state.unformed": "Structure is unformed",
        "state.invalid_config": "Invalid turbine configuration; stopped",
        "state.overlap": "Turbine overlaps another formed turbine",
        "inspect": "%s: inspect at %s, %s, %s",
        "issue.chunk": "Chunk is not loaded", "issue.controller": "Controller is missing or misplaced",
        "issue.length": "Axial length is not a configured tier", "issue.axis": "Rotor axis or row is misplaced",
        "issue.facing": "Casing facing does not match", "issue.port_facing": "Port facing does not match",
        "issue.end": "End component is misplaced", "issue.ring": "Octagonal casing is misplaced",
        "issue.inlet": "Inlet is missing or misplaced", "issue.exhaust": "Exhaust is missing or misplaced",
        "issue.overlap": "This part belongs to another turbine",
        "formed": "Formed with %s rotors", "wait_stock": "Check stored fluids, capacity and turbine settings",
        "hint": "Consumes steam while idle; redstone stops the turbine",
        "rotors_rpm": "Rotors: %s; Speed: %s RPM", "flow": "Processed %s / rated %s mB/tick",
        "tanks": "Inlet %s/%s mB; exhaust %s/%s mB", "su": "Total %s SU (front %s; rear %s)",
    }
    return {"zh_cn": _make_translations(zh, zh_gui), "en_us": _make_translations(en, en_gui)}


def _make_translations(blocks, gui):
    out = {f"block.{NAMESPACE}.{key}": value for key, value in blocks.items() if key.startswith("turbine_")}
    out[f"fluid_type.{NAMESPACE}.steam"] = blocks["steam"]
    out.update({f"gui.{NAMESPACE}.turbine.{key}": value for key, value in gui.items()})
    return out


def build_files() -> tuple[dict[str, bytes], list[str]]:
    files, new_svg = render_steam()
    # 复制任务卡预审过的五个工作台配方和一个21格动力合成配方。
    recipe_root = REPORT / "draft-recipes"
    for folder in ("crafting", "mechanical_crafting"):
        for path in sorted((recipe_root / folder).glob("*.json")):
            data = json.loads(path.read_text(encoding="utf-8"))
            if data["result"]["id"] not in REQUIRED:
                raise ValueError(f"配方产物不是注册汽轮机块: {path}")
            if data["type"] in ("minecraft:crafting_shaped", "create:mechanical_crafting"):
                rows = data["pattern"]
                width = len(rows[0])
                if any(len(row) != width for row in rows):
                    raise ValueError(f"有序配方图样行宽不一致: {path}")
                used = {char for row in rows for char in row if char != " "}
                if not used.issubset(data["key"]):
                    raise ValueError(f"有序配方包含未定义符号: {path}: {used - set(data['key'])}")
                if data["type"] == "create:mechanical_crafting":
                    counts = {char: sum(row.count(char) for row in rows) for char in used}
                    if len(rows) != 5 or width != 5 or sum(counts.values()) != 21:
                        raise ValueError(f"控制器动力合成必须为21格5×5布局: {path}")
                    if counts != {"S": 12, "R": 4, "P": 2, "I": 2, "A": 1}:
                        raise ValueError(f"控制器配方材料数量不符合任务方案: {path}: {counts}")
            files[f"data/{NAMESPACE}/recipe/{path.name}"] = (json.dumps(data, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
    for block in BLOCKS:
        loot = {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NAMESPACE}:{block}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        }
        files[f"data/{NAMESPACE}/loot_table/blocks/{block}.json"] = (json.dumps(loot, indent=2) + "\n").encode("utf-8")
    for tag in ("mineable/pickaxe", "needs_iron_tool"):
        path = RESOURCE / f"data/minecraft/tags/block/{tag}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        values = data["values"]
        for block_id in REQUIRED:
            if block_id not in values:
                values.append(block_id)
        if data.get("replace") is not False:
            raise ValueError(f"矿物工具标签必须保留replace=false: {path}")
        files[f"data/minecraft/tags/block/{tag}.json"] = (json.dumps(data, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
    for locale, additions in translations().items():
        path = RESOURCE / f"assets/{NAMESPACE}/lang/{locale}.json"
        existing = json.loads(path.read_text(encoding="utf-8"))
        overridable = {f"gui.{NAMESPACE}.turbine.wait_stock"}
        conflicts = {key for key, value in additions.items()
                     if key in existing and existing[key] != value and key not in overridable}
        if conflicts:
            raise ValueError(f"语言键冲突，不覆盖其他任务变更: {locale}: {sorted(conflicts)}")
        existing.update(additions)
        files[f"assets/{NAMESPACE}/lang/{locale}.json"] = (json.dumps(existing, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
    return files, new_svg


def write_files(files: dict[str, bytes], target: Path):
    for relative, data in files.items():
        path = target / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)


def verify(files: dict[str, bytes]):
    expected = {f"data/{NAMESPACE}/recipe/{block}.json" for block in BLOCKS}
    recipes = {path for path in files if "/recipe/" in path}
    assert recipes == expected, sorted(recipes)
    assert all(files[key] for key in expected)
    for path, raw in files.items():
        if path.endswith(".json"):
            json.loads(raw.decode("utf-8"))
    controller = json.loads(files[f"data/{NAMESPACE}/loot_table/blocks/turbine_controller.json"])
    assert controller["pools"][0]["entries"][0]["name"] == f"{NAMESPACE}:turbine_controller"
    return {"resource_files": len(files), "recipe_count": len(recipes), "loot_tables": len(BLOCKS),
            "language_files": 2, "tag_files": 2, "fluid_textures": 2, "json_parse": "PASS"}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--install", action="store_true")
    args = parser.parse_args()
    files, new_svg = build_files()
    summary = verify(files)
    write_files(files, STAGING)
    if args.install:
        write_files(files, RESOURCE)
    summary.update({"install": args.install, "staged_root": STAGING.relative_to(REPO).as_posix(),
                    "new_svg_sources": new_svg, "status": "PASS"})
    REPORT.mkdir(parents=True, exist_ok=True)
    (REPORT / "data-generation.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=True, indent=2))


if __name__ == "__main__":
    main()
