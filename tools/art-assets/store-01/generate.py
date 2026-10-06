"""导出 STORE-01 像素物品、干式贮存架模型与静态资源检查证据。"""
from __future__ import annotations

import hashlib
import json
import argparse
import re
import sys
import zipfile
from pathlib import Path

sys.dont_write_bytecode = True

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[3]
TOOL_ROOT = Path(__file__).resolve().parent
SOURCE_ROOT = TOOL_ROOT / "sources"
ASSET_ROOT = ROOT / "src/main/resources/assets/create_nuclear_industry"
DATA_ROOT = ROOT / "src/main/resources/data"
REPORT_ROOT = TOOL_ROOT / "evidence"

sys.path.insert(0, str(ROOT / "tools/art-assets"))
import export as strict_exporter  # noqa: E402


ITEM_PALETTES = {
    "glass_dust": {"#263238", "#83C4C8", "#D1DFDA", "#52A2AD", "#F0F2E9"},
    "vitrification_medium": {"#263238", "#5B7078", "#83C4C8", "#52A2AD", "#D1DFDA", "#367E92", "#28353A", "#F0C985", "#AD7A42", "#D2A359", "#A3B7B8", "#3E5058"},
    "lead_shielding_cask": {"#303441", "#CDD3DE", "#A6AFBF", "#7E8798", "#60687A", "#D2A359", "#AD7A42", "#464C5D"},
    "sealed_spent_fuel_cask": {"#303441", "#D1DFDA", "#7E8798", "#60687A", "#CDD3DE", "#D2A359", "#AD7A42", "#A6AFBF", "#464C5D", "#235C71", "#367E92", "#83C4C8", "#52A2AD", "#F0F2E9"},
    "dry_storage_rack_frame": {"#788E94", "#28353A", "#A3B7B8", "#5B7078", "#3E5058", "#D1DFDA"},
    "dry_storage_rack_base": {"#625F56", "#393A37", "#77766D", "#4C4A45", "#A39A83"},
    "dry_storage_rack_interior": {"#222426", "#303537", "#454B4B", "#5B6160", "#5B7078", "#3E5058"},
    "dry_storage_rack_door": {"#28353A", "#5B7078", "#367E92", "#235C71", "#163E52", "#A3B7B8", "#3E5058", "#83C4C8", "#52A2AD"},
    "dry_storage_rack_content": {"#28353A", "#D1DFDA", "#5B7078", "#367E92", "#83C4C8", "#52A2AD", "#163E52", "#235C71", "#A3B7B8"},
}

ITEM_IDS = tuple(name for name in ITEM_PALETTES if name in {
    "glass_dust", "vitrification_medium", "lead_shielding_cask", "sealed_spent_fuel_cask"
})
RACK_TEXTURES = {
    "frame": "dry_storage_rack_frame",
    "base": "dry_storage_rack_base",
    "interior": "dry_storage_rack_interior",
    "door": "dry_storage_rack_door",
    "content": "dry_storage_rack_content",
}


def face(texture: str) -> dict[str, object]:
    """返回六个闭合方位面，UV 固定在 0 至 16 的单格纹理范围内。"""
    return {direction: {"texture": texture, "uv": [0, 0, 16, 16]} for direction in (
        "down", "up", "north", "south", "west", "east"
    )}


def element(start: list[float], end: list[float], texture: str) -> dict[str, object]:
    return {"from": start, "to": end, "faces": face(texture)}


def rack_frame_elements() -> list[dict[str, object]]:
    """构造占满一格的封闭柜体、四格分隔框与混凝土底座。"""
    return [
        element([0, 0, 0], [16, 4, 16], "#base"),
        element([0, 4, 0], [2, 16, 16], "#frame"),
        element([14, 4, 0], [16, 16, 16], "#frame"),
        element([2, 14, 0], [14, 16, 16], "#frame"),
        element([2, 4, 14], [14, 14, 16], "#interior"),
        element([2, 4, 0], [14, 5, 2], "#frame"),
        element([2, 9, 0], [14, 10, 2], "#frame"),
        element([2, 13, 0], [14, 14, 2], "#frame"),
        element([2, 5, 2], [7, 6, 14], "#frame"),
        element([9, 5, 2], [14, 6, 14], "#frame"),
        element([2, 9, 2], [7, 10, 14], "#frame"),
        element([9, 9, 2], [14, 10, 14], "#frame"),
        element([2, 13, 2], [7, 14, 14], "#frame"),
        element([9, 13, 2], [14, 14, 14], "#frame"),
        element([7, 6, 2], [9, 9, 14], "#frame"),
        element([7, 10, 2], [9, 13, 14], "#frame"),
    ]


def rack_door_elements() -> list[dict[str, object]]:
    """为四个储藏显示区独立建模静态门片，便于后续动画替换。"""
    return [
        element([3, 5, 0.25], [7, 9, 1], "#door"),
        element([9, 5, 0.25], [13, 9, 1], "#door"),
        element([3, 10, 0.25], [7, 13, 1], "#door"),
        element([9, 10, 0.25], [13, 13, 1], "#door"),
    ]


def rack_occupancy_elements(level: int) -> list[dict[str, object]]:
    """按视觉占用档位点亮 0 至 4 个储藏显示区，不代表实际槽位数。"""
    bays = [
        ([4, 6, 0], [6, 8, 0.25]),
        ([10, 6, 0], [12, 8, 0.25]),
        ([4, 10.5, 0], [6, 12.5, 0.25]),
        ([10, 10.5, 0], [12, 12.5, 0.25]),
    ]
    return [element(start, end, "#content") for start, end in bays[:level]]


def make_model(textures: dict[str, str], elements: list[dict[str, object]]) -> dict[str, object]:
    return {
        "ambientocclusion": True,
        "textures": {**textures, "particle": textures.get("frame", textures.get("content", "create_nuclear_industry:block/dry_storage_rack_frame"))},
        "elements": elements,
    }


def render_textures() -> dict[str, dict[str, object]]:
    results: dict[str, dict[str, object]] = {}
    for texture_name, colors in ITEM_PALETTES.items():
        source_path = SOURCE_ROOT / f"{texture_name}.svg"
        svg = source_path.read_text(encoding="utf-8")
        image = strict_exporter.render_svg(svg, colors, (16, 16))
        if image.size != (16, 16) or image.mode != "RGBA":
            raise ValueError(f"{texture_name} 必须为 16×16 RGBA")
        alpha = list(image.getchannel("A").get_flattened_data())
        if set(alpha) - {0, 255}:
            raise ValueError(f"{texture_name} alpha 通道只允许 0 或 255")
        if texture_name in ITEM_IDS and (0 not in alpha or 255 not in alpha):
            raise ValueError(f"{texture_name} 物品图标必须同时包含透明与不透明像素")
        if texture_name not in ITEM_IDS and set(alpha) != {255}:
            raise ValueError(f"{texture_name} 方块纹理必须全不透明")
        alpha_bounds = image.getchannel("A").getbbox()
        if texture_name in ITEM_IDS and (alpha_bounds is None or alpha_bounds[0] < 1 or alpha_bounds[1] < 1 or alpha_bounds[2] > 15 or alpha_bounds[3] > 15):
            raise ValueError(f"{texture_name} 图标不透明像素必须留在 16×16 画布安全边界内：{alpha_bounds}")
        kind = "item" if texture_name in ITEM_IDS else "block"
        png_path = ASSET_ROOT / "textures" / kind / f"{texture_name}.png"
        png_path.parent.mkdir(parents=True, exist_ok=True)
        image.save(png_path)
        results[texture_name] = {
            "source": source_path.relative_to(ROOT).as_posix(),
            "texture": png_path.relative_to(ROOT).as_posix(),
            "size": [16, 16],
            "mode": "RGBA",
            "transparent_pixels": alpha.count(0),
            "opaque_pixels": alpha.count(255),
            "opaque_bounds": list(alpha_bounds) if alpha_bounds else [0, 0, 0, 0],
            "sha256": hashlib.sha256(png_path.read_bytes()).hexdigest(),
        }
    return results


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def lead_cask_recipe() -> dict[str, object]:
    """生成用户确认的有序铅桶配方，固定成本为铅板 4、钢板 1、密封环 1。"""
    return {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": [" R ", "L L", "LSL"],
        "key": {
            "L": {"tag": "c:plates/lead"},
            "S": {"tag": "c:plates/steel"},
            "R": {"item": "create_nuclear_industry:seal_ring"},
        },
        "result": {"id": "create_nuclear_industry:lead_shielding_cask", "count": 4},
    }


def write_lead_cask_recipe() -> Path:
    """只写本批铅桶配方，供局部整改时避免重导其他资源。"""
    path = DATA_ROOT / "create_nuclear_industry/recipe/crafting/lead_shielding_cask.json"
    write_json(path, lead_cask_recipe())
    return path


def verify_lead_cask_shaped_recipe(path: Path | None = None) -> dict[str, object]:
    """展开有序 pattern 字符，核对原料身份、数量和原生有序配方形状。"""
    if path is None:
        path = DATA_ROOT / "create_nuclear_industry/recipe/crafting/lead_shielding_cask.json"
    recipe = json.loads(path.read_text(encoding="utf-8"))
    expected = lead_cask_recipe()
    if recipe != expected:
        raise ValueError("铅屏蔽桶有序配方 JSON 与已确认合同不符")
    pattern = recipe["pattern"]
    symbols = [symbol for row in pattern for symbol in row if symbol != " "]
    symbol_counts = {symbol: symbols.count(symbol) for symbol in sorted(set(symbols))}
    if symbol_counts != {"L": 4, "R": 1, "S": 1}:
        raise ValueError(f"铅桶 pattern 展开数量应为 L4/R1/S1，实际为 {symbol_counts}")
    if recipe["key"]["L"] != {"tag": "c:plates/lead"} or recipe["key"]["S"] != {"tag": "c:plates/steel"}:
        raise ValueError("铅桶的铅板或钢板标签不符")
    if recipe["key"]["R"] != {"item": "create_nuclear_industry:seal_ring"}:
        raise ValueError("铅桶必须使用正式 seal_ring 物品身份")
    if recipe["category"] != "misc" or recipe["result"] != {"id": "create_nuclear_industry:lead_shielding_cask", "count": 4}:
        raise ValueError("铅桶配方类别或产量不符")
    return {
        "type": recipe["type"],
        "category": recipe["category"],
        "pattern": pattern,
        "symbol_counts": symbol_counts,
        "lead_plate_tag": recipe["key"]["L"]["tag"],
        "steel_plate_tag": recipe["key"]["S"]["tag"],
        "seal_ring_item": recipe["key"]["R"]["item"],
        "output": recipe["result"],
        "native_horizontal_mirror_allowed": True,
    }


def write_models() -> list[Path]:
    model_root = ASSET_ROOT / "models"
    models: list[Path] = []
    frame = rack_frame_elements()
    door = rack_door_elements()
    textures = {
        "frame": "create_nuclear_industry:block/dry_storage_rack_frame",
        "base": "create_nuclear_industry:block/dry_storage_rack_base",
        "interior": "create_nuclear_industry:block/dry_storage_rack_interior",
        "door": "create_nuclear_industry:block/dry_storage_rack_door",
        "content": "create_nuclear_industry:block/dry_storage_rack_content",
    }
    shared = {"frame": textures["frame"], "base": textures["base"], "interior": textures["interior"], "door": textures["door"], "content": textures["content"]}
    for name, parts in (
        ("dry_storage_rack_frame", frame),
        ("dry_storage_rack_door", door),
    ):
        path = model_root / "block" / f"{name}.json"
        write_json(path, make_model(shared, parts))
        models.append(path)

    for level in range(5):
        path = model_root / "block" / f"dry_storage_rack_occupancy_{level}.json"
        write_json(path, make_model(shared, rack_occupancy_elements(level)))
        models.append(path)

    zero_model = make_model(shared, frame + door)
    zero_model["parent"] = "minecraft:block/block"
    path = model_root / "block" / "dry_storage_rack_0.json"
    write_json(path, zero_model)
    models.append(path)

    for name in ITEM_IDS:
        path = model_root / "item" / f"{name}.json"
        write_json(path, {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"create_nuclear_industry:item/{name}"},
        })
        models.append(path)
    item_path = model_root / "item" / "dry_storage_rack.json"
    write_json(item_path, {"parent": "create_nuclear_industry:block/dry_storage_rack_0"})
    models.append(item_path)
    return models


def write_blockstate() -> Path:
    rotations = {"north": 0, "east": 90, "south": 180, "west": 270}
    multipart: list[dict[str, object]] = []
    for facing, y in rotations.items():
        multipart.append({"when": {"facing": facing}, "apply": {"model": "create_nuclear_industry:block/dry_storage_rack_frame", "y": y}})
        multipart.append({"when": {"facing": facing}, "apply": {"model": "create_nuclear_industry:block/dry_storage_rack_door", "y": y}})
        for level in range(5):
            multipart.append({
                "when": {"facing": facing, "storage_level": str(level)},
                "apply": {"model": f"create_nuclear_industry:block/dry_storage_rack_occupancy_{level}", "y": y},
            })
    path = ASSET_ROOT / "blockstates" / "dry_storage_rack.json"
    write_json(path, {"multipart": multipart})
    return path


def dry_storage_rack_recipe() -> dict[str, object]:
    """返回干式贮存架工作台配方，供定向核验与资源写入共用。"""
    return {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["S S", " C "],
        "key": {
            "S": {"tag": "c:plates/steel"},
            "C": {"item": "create_nuclear_industry:shielding_concrete"},
        },
        "result": {"id": "create_nuclear_industry:dry_storage_rack", "count": 1},
    }


def write_recipes_and_tags() -> list[Path]:
    recipes = {
        "recipe/milling/glass_dust.json": {
            "type": "create:milling",
            "ingredients": [{"item": "minecraft:glass"}],
            "processing_time": 100,
            "results": [{"id": "create_nuclear_industry:glass_dust"}],
        },
        "recipe/mixing/vitrification_medium.json": {
            "type": "create:mixing",
            "ingredients": [
                {"item": "create_nuclear_industry:glass_dust"},
                {"tag": "c:dusts/quartz"},
                {"item": "minecraft:clay_ball"},
            ],
            "heat_requirement": "heated",
            "processing_time": 100,
            "results": [{"id": "create_nuclear_industry:vitrification_medium", "count": 4}],
        },
        "recipe/crafting/lead_shielding_cask.json": {
            **lead_cask_recipe(),
        },
        "recipe/crafting/dry_storage_rack.json": dry_storage_rack_recipe(),
    }
    files: list[Path] = []
    for relative, data in recipes.items():
        path = DATA_ROOT / "create_nuclear_industry" / relative
        write_json(path, data)
        files.append(path)

    tag_data = {
        "c/tags/item/dusts/glass.json": {"replace": False, "values": ["create_nuclear_industry:glass_dust"]},
        "create_nuclear_industry/tags/item/vitrification_media.json": {"replace": False, "values": ["create_nuclear_industry:vitrification_medium"]},
        "create_nuclear_industry/tags/item/lead_shielding_casks.json": {"replace": False, "values": ["create_nuclear_industry:lead_shielding_cask"]},
    }
    for relative, data in tag_data.items():
        path = DATA_ROOT / relative
        write_json(path, data)
        files.append(path)

    dusts_path = DATA_ROOT / "c/tags/item/dusts.json"
    dusts = json.loads(dusts_path.read_text(encoding="utf-8"))
    if "#c:dusts/glass" not in dusts["values"]:
        dusts["values"].append("#c:dusts/glass")
    write_json(dusts_path, dusts)
    files.append(dusts_path)

    pickaxe_path = DATA_ROOT / "minecraft/tags/block/mineable/pickaxe.json"
    pickaxe = json.loads(pickaxe_path.read_text(encoding="utf-8"))
    block_id = "create_nuclear_industry:dry_storage_rack"
    if block_id not in pickaxe["values"]:
        pickaxe["values"].append(block_id)
    write_json(pickaxe_path, pickaxe)
    files.append(pickaxe_path)

    loot_path = DATA_ROOT / "create_nuclear_industry/loot_table/blocks/dry_storage_rack.json"
    write_json(loot_path, {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": "create_nuclear_industry:dry_storage_rack"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })
    files.append(loot_path)
    return files


def make_preview(texture_results: dict[str, dict[str, object]]) -> Path:
    """绘制透明背景物品图标及五档正面模型静态概览。"""
    scale = 8
    canvas = Image.new("RGB", (1000, 530), "#e9e7df")
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.load_default()
    draw.text((18, 14), "STORE-01 | item icons and dry storage rack states", fill="#202020", font=font)

    for index, name in enumerate(ITEM_IDS):
        x = 28 + index * 150
        draw.text((x, 48), name, fill="#202020", font=font)
        checker = Image.new("RGB", (64, 64), "#dedbd2")
        d = ImageDraw.Draw(checker)
        d.rectangle((32, 0, 63, 31), fill="#4c4c4c")
        d.rectangle((0, 32, 31, 63), fill="#4c4c4c")
        icon = Image.open(ASSET_ROOT / "textures/item" / f"{name}.png").convert("RGBA").resize((64, 64), Image.Resampling.NEAREST)
        checker.paste(icon, (0, 0), icon)
        canvas.paste(checker, (x, 68))
        draw.text((x, 140), "16x16 / 8x nearest", fill="#454545", font=font)

    draw.text((18, 190), "rack front preview | storage_level 0-4 (visual occupancy only)", fill="#202020", font=font)
    for level in range(5):
        x = 44 + level * 190
        draw.text((x, 220), f"level={level}", fill="#202020", font=font)
        draw.rectangle((x, 244, x + 120, 432), fill="#625F56", outline="#393A37", width=4)
        draw.rectangle((x + 14, 264, x + 106, 416), fill="#3e5058", outline="#28353a", width=5)
        draw.line((x + 60, 264, x + 60, 416), fill="#788e94", width=8)
        draw.line((x + 14, 340, x + 106, 340), fill="#788e94", width=8)
        bays = [(x + 28, 282), (x + 74, 282), (x + 28, 358), (x + 74, 358)]
        for bay_index, (bx, by) in enumerate(bays):
            draw.rectangle((bx, by, bx + 28, by + 40), fill="#163e52", outline="#367e92", width=3)
            if bay_index < level:
                draw.rectangle((bx + 8, by + 10, bx + 20, by + 32), fill="#52a2ad", outline="#d1dfda", width=2)
                draw.line((bx + 12, by + 13, bx + 12, by + 27), fill="#f0f2e9", width=2)
        draw.rectangle((x + 8, 440, x + 112, 466), fill="#4c4a45", outline="#393a37", width=2)
    output = REPORT_ROOT / "store-01-static-preview.png"
    output.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(output)
    return output


def inspect_model(path: Path) -> dict[str, object]:
    model = json.loads(path.read_text(encoding="utf-8"))
    elements = model.get("elements", [])
    for index, part in enumerate(elements):
        if len(part.get("from", [])) != 3 or len(part.get("to", [])) != 3:
            raise ValueError(f"{path}: 元素 {index} 坐标维数错误")
        if any(not (0 <= a <= 16 and 0 <= b <= 16 and a < b) for a, b in zip(part["from"], part["to"])):
            raise ValueError(f"{path}: 元素 {index} 超出单格或存在零体积轴")
        faces = part.get("faces", {})
        if set(faces) != {"down", "up", "north", "south", "west", "east"}:
            raise ValueError(f"{path}: 元素 {index} 缺少封闭面")
        for direction, face_data in faces.items():
            uv = face_data.get("uv", [])
            if len(uv) != 4 or any(not (0 <= value <= 16) for value in uv):
                raise ValueError(f"{path}: {index}.{direction} UV 超出 0..16")
    return {"model": path.relative_to(ROOT).as_posix(), "elements": len(elements), "closed_faces": True, "coordinates_and_uv_within_0_16": True}


def check_no_intersections(path: Path) -> None:
    parts = json.loads(path.read_text(encoding="utf-8")).get("elements", [])
    for i, left in enumerate(parts):
        for right in parts[i + 1:]:
            if all(max(left["from"][axis], right["from"][axis]) < min(left["to"][axis], right["to"][axis]) for axis in range(3)):
                raise ValueError(f"{path}: 模型元素有实体重叠或共面闪烁风险")


def verify_public_material_tags() -> dict[str, list[str]]:
    """确认现有铅板、钢板与石英粉公共标签真实包含项目材料。"""
    expected = {
        "c:plates/lead": ("c/tags/item/plates/lead.json", "create_nuclear_industry:lead_plate"),
        "c:plates/steel": ("c/tags/item/plates/steel.json", "create_nuclear_industry:steel_plate"),
        "c:dusts/quartz": ("c/tags/item/dusts/quartz.json", "create_nuclear_industry:quartz_dust"),
    }
    resolved: dict[str, list[str]] = {}
    for tag, (relative, item_id) in expected.items():
        path = DATA_ROOT / relative
        values = json.loads(path.read_text(encoding="utf-8"))["values"]
        if item_id not in values:
            raise ValueError(f"{tag} 未包含既有项目材料 {item_id}")
        resolved[tag] = values
    return resolved


def verify_recipe_contract() -> dict[str, object]:
    """核对配方工时、产量及独立铅板 ingredient 数量。"""
    root = DATA_ROOT / "create_nuclear_industry/recipe"
    milling = json.loads((root / "milling/glass_dust.json").read_text(encoding="utf-8"))
    if milling["ingredients"] != [{"item": "minecraft:glass"}] or milling["processing_time"] != 100 or milling["results"] != [{"id": "create_nuclear_industry:glass_dust"}]:
        raise ValueError("玻璃磨石配方与工时合同不符")
    mixing = json.loads((root / "mixing/vitrification_medium.json").read_text(encoding="utf-8"))
    if mixing["heat_requirement"] != "heated" or mixing["processing_time"] != 100 or mixing["results"] != [{"id": "create_nuclear_industry:vitrification_medium", "count": 4}]:
        raise ValueError("固化基材搅拌配方与参数合同不符")
    if mixing["ingredients"] != [
        {"item": "create_nuclear_industry:glass_dust"},
        {"tag": "c:dusts/quartz"},
        {"item": "minecraft:clay_ball"},
    ]:
        raise ValueError("固化基材配方输入不符")
    cask_contract = verify_lead_cask_shaped_recipe(root / "crafting/lead_shielding_cask.json")
    rack = json.loads((root / "crafting/dry_storage_rack.json").read_text(encoding="utf-8"))
    if rack != dry_storage_rack_recipe():
        raise ValueError("干式贮存架有序配方与合同不符")
    return {"glass_milling_time": 100, "mixing_time": 100, "mixing_heat": "heated", "lead_shielding_cask": cask_contract, "rack_output": 1}


def verify_rack_item_display_chain() -> dict[str, object]:
    """从锁定版本客户端 JAR 检查干式贮存架物品模型的有效 display 父链。"""
    item_path = ASSET_ROOT / "models/item/dry_storage_rack.json"
    zero_path = ASSET_ROOT / "models/block/dry_storage_rack_0.json"
    item_model = json.loads(item_path.read_text(encoding="utf-8"))
    zero_model = json.loads(zero_path.read_text(encoding="utf-8"))
    if item_model.get("parent") != "create_nuclear_industry:block/dry_storage_rack_0":
        raise ValueError("柜架物品模型没有继承零档方块模型")
    if zero_model.get("parent") != "minecraft:block/block":
        raise ValueError("零档方块模型没有继承 minecraft:block/block")
    if "display" in item_model or "display" in zero_model:
        raise ValueError("零档及物品模型不应覆盖原版 display")

    client_jar = Path.home() / ".gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar"
    if not client_jar.is_file():
        raise FileNotFoundError(f"验证原版 display 需要当前锁定版本客户端 JAR：{client_jar}")
    with zipfile.ZipFile(client_jar) as archive:
        vanilla = json.loads(archive.read("assets/minecraft/models/block/block.json"))
    display = vanilla.get("display")
    required_contexts = {"gui", "ground", "fixed", "thirdperson_righthand", "firstperson_righthand", "firstperson_lefthand"}
    if not isinstance(display, dict) or not required_contexts <= set(display):
        raise ValueError("Minecraft 1.21.1 原版 block/block 缺少预期 GUI 或手持 display")
    return {
        "chain": ["create_nuclear_industry:item/dry_storage_rack", "create_nuclear_industry:block/dry_storage_rack_0", "minecraft:block/block"],
        "resolved_vanilla_model": "assets/minecraft/models/block/block.json",
        "minecraft_client_version": "1.21.1",
        "minecraft_client_sha256": hashlib.sha256(client_jar.read_bytes()).hexdigest(),
        "effective_display_contexts": sorted(display),
        "gui_and_handheld_display_inherited": True,
    }


def verify_and_write_evidence(models: list[Path], blockstate: Path, data_files: list[Path], texture_results: dict[str, dict[str, object]], preview: Path) -> None:
    model_checks = [inspect_model(path) for path in models if path.parent.name == "block"]
    states = json.loads(blockstate.read_text(encoding="utf-8"))["multipart"]
    expected_models = {"create_nuclear_industry:block/dry_storage_rack_frame", "create_nuclear_industry:block/dry_storage_rack_door"}
    expected_models.update(f"create_nuclear_industry:block/dry_storage_rack_occupancy_{level}" for level in range(5))
    actual_models = {part["apply"]["model"] for part in states}
    if actual_models != expected_models:
        raise ValueError(f"方块状态引用不匹配：{sorted(actual_models ^ expected_models)}")
    state_pairs = set()
    for part in states:
        when = part.get("when", {})
        if "storage_level" in when:
            state_pairs.add((when["facing"], int(when["storage_level"])))
    expected_pairs = {(facing, level) for facing in ("north", "east", "south", "west") for level in range(5)}
    if state_pairs != expected_pairs:
        raise ValueError("facing × storage_level 未完整覆盖四向五档")

    # 门片和状态图标分层贴在柜体前端，各模型组合时也必须保持无体积交叠。
    component_paths = [ASSET_ROOT / "models/block/dry_storage_rack_frame.json", ASSET_ROOT / "models/block/dry_storage_rack_door.json", *(ASSET_ROOT / f"models/block/dry_storage_rack_occupancy_{n}.json" for n in range(5)), ASSET_ROOT / "models/block/dry_storage_rack_0.json"]
    for path in component_paths:
        check_no_intersections(path)
    component_boxes = [(path, json.loads(path.read_text(encoding="utf-8")).get("elements", [])) for path in component_paths[:2]]
    for level in range(5):
        occ_path = ASSET_ROOT / f"models/block/dry_storage_rack_occupancy_{level}.json"
        occ_parts = json.loads(occ_path.read_text(encoding="utf-8")).get("elements", [])
        for left_path, left_parts in component_boxes:
            for left in left_parts:
                for right in occ_parts:
                    if all(max(left["from"][axis], right["from"][axis]) < min(left["to"][axis], right["to"][axis]) for axis in range(3)):
                        raise ValueError(f"多部件模型组合交叠：{left_path.name} × {occ_path.name}")

    frame_boxes = json.loads((ASSET_ROOT / "models/block/dry_storage_rack_frame.json").read_text(encoding="utf-8"))["elements"]
    block_bounds = [min(part["from"][axis] for part in frame_boxes) for axis in range(3)] + [max(part["to"][axis] for part in frame_boxes) for axis in range(3)]
    if block_bounds != [0, 0, 0, 16, 16, 16]:
        raise ValueError(f"柜架外形未占满单格边界：{block_bounds}")

    for path in models:
        model = json.loads(path.read_text(encoding="utf-8"))
        parent = model.get("parent", "")
        if parent.startswith("create_nuclear_industry:"):
            parent_category, parent_name = parent.split(":", 1)[1].split("/", 1)
            parent_path = ASSET_ROOT / "models" / parent_category / f"{parent_name}.json"
            if not parent_path.is_file():
                raise ValueError(f"父模型引用缺失：{path} -> {parent}")
        for texture in model.get("textures", {}).values():
            if texture.startswith("create_nuclear_industry:"):
                category, name = texture.split(":", 1)[1].split("/", 1)
                png = ASSET_ROOT / "textures" / category / f"{name}.png"
                if not png.is_file():
                    raise ValueError(f"纹理引用缺失：{path} -> {texture}")

    recipe_checks = []
    for path in data_files:
        data = json.loads(path.read_text(encoding="utf-8"))
        if "recipe" in path.parts:
            recipe_checks.append({"file": path.relative_to(ROOT).as_posix(), "json": True})
    public_tags = verify_public_material_tags()
    recipe_contract = verify_recipe_contract()
    item_display_chain = verify_rack_item_display_chain()
    evidence = {
        "task": "STORE-01-ART",
        "textures": texture_results,
        "model_count": len(models),
        "models": model_checks,
        "blockstate": {
            "file": blockstate.relative_to(ROOT).as_posix(),
            "directions": ["north", "east", "south", "west"],
            "storage_levels": list(range(5)),
            "direction_level_pairs": len(state_pairs),
            "model_references_resolve": True,
        },
        "recipes_and_tags": [{"file": path.relative_to(ROOT).as_posix(), "json": True} for path in data_files],
        "recipe_files": recipe_checks,
        "public_material_tags": public_tags,
        "recipe_contract": recipe_contract,
        "rack_item_display_chain": item_display_chain,
        "rack_model_bounds": block_bounds,
        "preview": preview.relative_to(ROOT).as_posix(),
        "checks": {
            "png_dimensions": "all exported textures are 16x16 RGBA",
            "item_transparency": "binary alpha; both transparent and opaque pixels present; opaque bounds stay inside the 16x16 safety border",
            "model_bounds_and_uv": "coordinates and UV bounds are within 0..16",
            "closed_faces": "all model elements define six faces",
            "geometry_overlap": "no positive-volume intersections within or between frame, door, and occupancy components",
            "rack_bounds": "frame model spans [0,0,0] to [16,16,16]",
            "rack_item_display": "resolved item -> zero block model -> actual Minecraft 1.21.1 client block/block parent display",
            "blockstate_coverage": "all 4 facings x 5 storage levels are represented",
            "lead_cask_recipe": "shaped pattern is expanded and checked as 4 lead plates, 1 steel plate, 1 seal ring -> 4 casks",
            "minecraft_runtime_or_gradle": "not run by STORE-01-ART per task boundary",
        },
    }
    REPORT_ROOT.mkdir(parents=True, exist_ok=True)
    (REPORT_ROOT / "resource-checks.json").write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description="导出 STORE-01 像素素材与资源检查证据")
    parser.add_argument("--lead-cask-only", action="store_true", help="只写并检查铅屏蔽桶有序配方，不重导其他资源")
    args = parser.parse_args()
    if args.lead_cask_only:
        path = write_lead_cask_recipe()
        public_tags = verify_public_material_tags()
        cask_contract = verify_lead_cask_shaped_recipe(path)
        evidence_path = REPORT_ROOT / "resource-checks.json"
        evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
        evidence["recipe_contract"].pop("cask_lead_plates", None)
        evidence["recipe_contract"].pop("cask_output", None)
        evidence["recipe_contract"]["lead_shielding_cask"] = cask_contract
        evidence["public_material_tags"] = public_tags
        evidence["checks"]["lead_cask_recipe"] = "shaped pattern expanded: 4 lead plates, 1 steel plate, 1 seal ring -> 4 casks"
        evidence["lead_cask_recipe_verification"] = {
            "file": path.relative_to(ROOT).as_posix(),
            "json_schema": "minecraft:crafting_shaped",
            **cask_contract,
        }
        write_json(evidence_path, evidence)
        print(f"已写入并核对铅桶有序配方：{path.relative_to(ROOT)}")
        return
    textures = render_textures()
    models = write_models()
    blockstate = write_blockstate()
    data_files = write_recipes_and_tags()
    preview = make_preview(textures)
    verify_and_write_evidence(models, blockstate, data_files, textures, preview)
    print(f"已生成 {len(textures)} 张纹理、{len(models)} 个模型和 4 条配方；检查证据：{(REPORT_ROOT / 'resource-checks.json').relative_to(ROOT)}")


if __name__ == "__main__":
    main()
