"""为核换热器材料导出三项16像素纹理、模型与离线核对图。"""
from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[3]
ART_ROOT = ROOT / "tools" / "art-assets"
sys.path.insert(0, str(ART_ROOT))
import export as strict_exporter

ASSET_ROOT = ROOT / "src/main/resources/assets/create_nuclear_industry"
SOURCE_ROOT = Path(__file__).resolve().parent / "sources"
OUTPUT_ROOT = ROOT / "build/reports/extension/EXT-B-EXCHANGER-01A-MATERIAL"
PREVIEW_PATH = OUTPUT_ROOT / "heat-exchanger-materials-preview.png"
ITEMS = {
    "steel_pipe_blank": ["#263238", "#D1DFDA", "#A3B7B8", "#788E94", "#5B7078", "#3E5058", "#28353A", "#151C20"],
    "reinforced_steel_plate": ["#263238", "#D1DFDA", "#A3B7B8", "#788E94", "#5B7078", "#3E5058"],
    "nuclear_heat_exchange_bundle": ["#263238", "#D1DFDA", "#A3B7B8", "#788E94", "#5B7078", "#3E5058", "#28353A", "#C87942"],
}


def render_items() -> list[dict[str, object]]:
    """按严格像素SVG子集生成透明PNG、原生物品模型与校验摘要。"""
    results = []
    for name, palette in ITEMS.items():
        svg_path = SOURCE_ROOT / f"{name}.svg"
        source = svg_path.read_text(encoding="utf-8")
        image = strict_exporter.render_svg(source, set(palette), (16, 16))
        if image.size != (16, 16) or image.mode != "RGBA":
            raise ValueError(f"{name}必须导出为16×16 RGBA：{image.size} {image.mode}")
        alpha_pixels = image.getchannel("A").get_flattened_data()
        alpha_values = set(alpha_pixels)
        if not alpha_values <= {0, 255} or 0 not in alpha_values:
            raise ValueError(f"{name}透明像素合同错误：{sorted(alpha_values)}")

        texture = ASSET_ROOT / "textures/item" / f"{name}.png"
        model = ASSET_ROOT / "models/item" / f"{name}.json"
        texture.parent.mkdir(parents=True, exist_ok=True)
        model.parent.mkdir(parents=True, exist_ok=True)
        image.save(texture)
        model.write_text(json.dumps({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"create_nuclear_industry:item/{name}"},
        }, indent=2) + "\n", encoding="utf-8")
        results.append({
            "item": name,
            "source": svg_path.relative_to(ROOT).as_posix(),
            "texture": texture.relative_to(ROOT).as_posix(),
            "model": model.relative_to(ROOT).as_posix(),
            "size": list(image.size),
            "transparent_pixels": list(alpha_pixels).count(0),
            "sha256": hashlib.sha256(texture.read_bytes()).hexdigest(),
        })
    return results


def create_preview() -> None:
    """绘制明暗底原尺寸与八倍最近邻预览。"""
    width, height = 700, 330
    canvas = Image.new("RGB", (width, height), "#F2F2F2")
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.load_default()
    draw.text((16, 14), "EXT-B-EXCHANGER-01A MATERIAL | pixel art preview", fill="#202020", font=font)
    for index, name in enumerate(ITEMS):
        x = 18 + index * 226
        image = Image.open(ASSET_ROOT / "textures/item" / f"{name}.png").convert("RGBA")
        draw.text((x, 48), name, fill="#202020", font=font)
        draw.text((x, 76), "1x", fill="#505050", font=font)
        draw.rectangle((x, 96, x + 64, 160), fill="#E5E5E5")
        draw.rectangle((x + 32, 96, x + 64, 160), fill="#4B4B4B")
        canvas.paste(image, (x + 24, 120), image)
        draw.text((x, 184), "8x nearest", fill="#505050", font=font)
        draw.rectangle((x, 204, x + 128, 332), fill="#E5E5E5")
        draw.rectangle((x + 64, 204, x + 128, 332), fill="#4B4B4B")
        enlarged = image.resize((128, 128), Image.Resampling.NEAREST)
        canvas.paste(enlarged, (x, 204), enlarged)
    OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
    canvas.save(PREVIEW_PATH)


def main() -> None:
    """仅更新本批纹理、模型及其离线证据。"""
    results = render_items()
    create_preview()
    OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
    (OUTPUT_ROOT / "assets.json").write_text(json.dumps({
        "items": results,
        "preview": PREVIEW_PATH.relative_to(ROOT).as_posix(),
    }, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"已导出 {len(results)} 项核换热器材料纹理与模型：{PREVIEW_PATH}")


if __name__ == "__main__":
    main()
