"""CRAFT-SHAPED-01 定向基线采集与静态核验。"""

from __future__ import annotations

import argparse
import base64
import hashlib
import importlib.util
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any


sys.dont_write_bytecode = True


ROOT = Path(__file__).resolve().parents[3]
RECIPE_ROOT = ROOT / "src/main/resources/data/create_nuclear_industry/recipe"
EVIDENCE_ROOT = Path(__file__).resolve().parent
BASELINE_PATH = EVIDENCE_ROOT / "baseline.json"
TARGET_PATTERNS = {
    "crafting/reactor/reactor_window.json": ["G", "C"],
    "crafting/reactor/reactor_cold_port.json": ["FD", "CS"],
    "crafting/reactor/reactor_hot_port.json": ["FD", "CS"],
    "crafting/high_pressure_boiler_window.json": ["G", "C"],
    "crafting/high_pressure_boiler_water_port.json": ["PF", " C"],
    "crafting/high_pressure_boiler_steam_port.json": ["F F", " P ", " C "],
    "crafting/boiler_safety_valve.json": ["V R", " I ", " C "],
    "crafting/boiler_heat_exchange_section.json": ["H", "C"],
    "crafting/turbine_window.json": ["G", "C"],
    "turbine_inlet.json": ["P P", " R ", "SC "],
    "turbine_exhaust.json": ["P P", " R ", "SC "],
    "turbine_output_shaft.json": ["SBS", "SAS", "BC "],
    "crafting/dry_storage_rack.json": ["S S", " C "],
}
UNCHANGED_PATHS = [
    "raw_lead_from_block.json",
    "raw_tin_from_block.json",
    "raw_uranium_from_block.json",
    "crafting/materials/lead_nugget_from_ingot.json",
    "crafting/materials/tin_nugget_from_ingot.json",
    "crafting/lead_shielding_cask.json",
]


def compact_json(value: Any) -> str:
    """以稳定键序将JSON值转换成可计数的身份字符串。"""
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def capture_baseline() -> None:
    """确认指定13条配方基线后，保存原始对象与豁免文件字节。"""
    recipes: dict[str, Any] = {}
    for relative in TARGET_PATTERNS:
        raw = (RECIPE_ROOT / relative).read_bytes()
        recipe = json.loads(raw.decode("utf-8"))
        if recipe.get("type") != "minecraft:crafting_shapeless":
            raise ValueError(f"基线类型不是无序配方：{relative}")
        recipes[relative] = {
            "sha256": hashlib.sha256(raw).hexdigest(),
            "raw_base64": base64.b64encode(raw).decode("ascii"),
            "object": recipe,
        }
    if len(recipes) != 13:
        raise ValueError(f"目标基线数量错误：{len(recipes)}")
    preserved: dict[str, Any] = {}
    for relative in UNCHANGED_PATHS:
        raw = (RECIPE_ROOT / relative).read_bytes()
        preserved[relative] = {
            "sha256": hashlib.sha256(raw).hexdigest(),
            "raw_base64": base64.b64encode(raw).decode("ascii"),
        }
    BASELINE_PATH.write_text(
        json.dumps({"recipes": recipes, "preserved": preserved}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"已捕获13条配方原对象，以及6个豁免文件的原始字节：{BASELINE_PATH.relative_to(ROOT)}")


def expanded_shaped(recipe: dict[str, Any], path: str) -> Counter[str]:
    """展开有序阵列，保留item/tag身份并按格子统计数量。"""
    pattern = recipe.get("pattern")
    key = recipe.get("key")
    if not isinstance(pattern, list) or not 1 <= len(pattern) <= 3:
        raise ValueError(f"阵列高度超出1至3格：{path}")
    if not all(isinstance(row, str) and 1 <= len(row) <= 3 for row in pattern):
        raise ValueError(f"阵列行宽超出1至3格或不是字符串：{path}")
    if len({len(row) for row in pattern}) != 1:
        raise ValueError(f"阵列每行宽度不一致：{path}")
    used = {cell for row in pattern for cell in row if cell != " "}
    if not isinstance(key, dict) or used != set(key):
        raise ValueError(f"key含缺失或未使用字符：{path} used={sorted(used)} key={sorted(key or {})}")
    return Counter(compact_json(key[cell]) for row in pattern for cell in row if cell != " ")


def original_ingredients(recipe: dict[str, Any]) -> Counter[str]:
    """统计原配方ingredient对象，标签与物品身份分别保留。"""
    return Counter(compact_json(ingredient) for ingredient in recipe.get("ingredients", []))


def verify_source() -> dict[str, Any]:
    """对照基线核验材料守恒、元数据、豁免字节和剩余无序配方。"""
    baseline = json.loads(BASELINE_PATH.read_text(encoding="utf-8"))
    if set(baseline["recipes"]) != set(TARGET_PATTERNS) or len(baseline["recipes"]) != 13:
        raise ValueError("保存的目标基线不是任务卡指定13条")
    for relative, expected_pattern in TARGET_PATTERNS.items():
        original = baseline["recipes"][relative]["object"]
        current = json.loads((RECIPE_ROOT / relative).read_text(encoding="utf-8"))
        if current.get("type") != "minecraft:crafting_shaped":
            raise ValueError(f"目标配方仍不是有序配方：{relative}")
        if current.get("pattern") != expected_pattern:
            raise ValueError(f"有序阵列不符合任务卡：{relative} -> {current.get('pattern')}")
        if original_ingredients(original) != expanded_shaped(current, relative):
            raise ValueError(f"展开后的item/tag及重复数量偏离基线：{relative}")
        if current.get("result") != original.get("result"):
            raise ValueError(f"完整result对象改变：{relative}")
        if current.get("category") != original.get("category"):
            raise ValueError(f"category改变：{relative}")
        old_other = {key: value for key, value in original.items() if key not in {"type", "ingredients"}}
        new_other = {key: value for key, value in current.items() if key not in {"type", "pattern", "key"}}
        if old_other != new_other:
            raise ValueError(f"其他配方元数据改变：{relative}")
    for relative, saved in baseline["preserved"].items():
        raw = (RECIPE_ROOT / relative).read_bytes()
        if base64.b64encode(raw).decode("ascii") != saved["raw_base64"]:
            raise ValueError(f"豁免文件原字节发生变化：{relative}")
    shapeless = sorted(
        path.relative_to(RECIPE_ROOT).as_posix()
        for path in RECIPE_ROOT.rglob("*.json")
        if json.loads(path.read_text(encoding="utf-8")).get("type") == "minecraft:crafting_shapeless"
    )
    expected_shapeless = sorted(UNCHANGED_PATHS[:5])
    if shapeless != expected_shapeless:
        raise ValueError(f"src剩余无序配方不是五条拆解豁免：{shapeless}")
    verify_generators(baseline)
    return {
        "target_shaped_count": len(TARGET_PATTERNS),
        "remaining_shapeless": shapeless,
        "preserved_byte_hashes": {path: value["sha256"] for path, value in baseline["preserved"].items()},
        "patterns": TARGET_PATTERNS,
    }


def load_module(relative: str, module_name: str) -> Any:
    """从指定导出器加载纯生成函数，避免调用会写资源的入口。"""
    spec = importlib.util.spec_from_file_location(module_name, ROOT / relative)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"无法加载导出器：{relative}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def verify_generators(baseline: dict[str, Any]) -> None:
    """核对三个导出器内存生成对象与已经保存的配方合同。"""
    boiler = load_module("tools/art-assets/boiler_01a_assets.py", "crafting_shaped_boiler")
    boiler_recipes = boiler.shaped_device_recipes()
    turbine = load_module("tools/art-assets/turbine_data.py", "crafting_shaped_turbine")
    rack = load_module("tools/art-assets/store-01/generate.py", "crafting_shaped_rack")
    generated = {
        **{f"{name}.json": value for name, value in boiler_recipes.items()},
        "crafting/turbine_window.json": turbine.turbine_window_recipe(),
        "crafting/dry_storage_rack.json": rack.dry_storage_rack_recipe(),
    }
    for relative, value in generated.items():
        source = json.loads((RECIPE_ROOT / relative).read_text(encoding="utf-8"))
        if source != value:
            raise ValueError(f"导出器内存对象与资源不一致：{relative}")
        if relative in baseline["recipes"] and value != source:
            raise ValueError(f"导出器生成对象偏离原配方result/元数据：{relative}")
    if len(generated) != 7:
        raise ValueError(f"三个导出器定向生成数不为7：{len(generated)}")


def main() -> None:
    parser = argparse.ArgumentParser(description="采集或定向核验CRAFT-SHAPED-01配方")
    parser.add_argument("--capture-baseline", action="store_true", help="在改动前保存13条对象及豁免字节")
    args = parser.parse_args()
    if args.capture_baseline:
        capture_baseline()
        return
    evidence = verify_source()
    serialized = json.dumps(evidence, ensure_ascii=False, indent=2) + "\n"
    (EVIDENCE_ROOT / "verification.json").write_text(serialized, encoding="utf-8")
    print(serialized)


if __name__ == "__main__":
    main()
