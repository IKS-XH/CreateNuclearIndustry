"""仅导出强化钢板序列装配半成品的游戏纹理与物品模型。"""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools" / "art-assets"))
import export as strict_exporter

ASSET_ROOT = ROOT / "src/main/resources/assets/create_nuclear_industry"
SOURCE = Path(__file__).resolve().parent / "sources/incomplete_reinforced_steel_plate.svg"
NAME = "incomplete_reinforced_steel_plate"
PALETTE = {"#263238", "#D1DFDA", "#A3B7B8", "#788E94", "#5B7078", "#3E5058", "#28353A"}


def main() -> None:
    """使用仓库严格像素SVG渲染器生成透明16×16纹理和原生物品模型。"""
    image = strict_exporter.render_svg(SOURCE.read_text(encoding="utf-8"), PALETTE, (16, 16))
    if image.mode != "RGBA" or image.size != (16, 16):
        raise ValueError(f"半成品纹理规格不符：{image.mode} {image.size}")
    alpha_values = set(image.getchannel("A").get_flattened_data())
    if not alpha_values <= {0, 255} or 0 not in alpha_values:
        raise ValueError(f"半成品透明像素规格不符：{sorted(alpha_values)}")

    texture = ASSET_ROOT / "textures/item" / f"{NAME}.png"
    model = ASSET_ROOT / "models/item" / f"{NAME}.json"
    texture.parent.mkdir(parents=True, exist_ok=True)
    model.parent.mkdir(parents=True, exist_ok=True)
    image.save(texture)
    model.write_text(json.dumps({
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"create_nuclear_industry:item/{NAME}"},
    }, indent=2) + "\n", encoding="utf-8")
    print(f"已导出 {NAME}: {texture.relative_to(ROOT).as_posix()}")


if __name__ == "__main__":
    main()
