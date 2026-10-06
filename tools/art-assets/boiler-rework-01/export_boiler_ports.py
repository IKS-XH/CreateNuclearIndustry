"""为锅炉冷热端口导出SVG纹理、四向状态和检查预览。"""
from __future__ import annotations

import importlib.util
import json
import sys
from pathlib import Path

sys.dont_write_bytecode = True

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[3]
ART = Path(__file__).resolve().parent
ASSETS = ROOT / "src/main/resources/assets/create_nuclear_industry"
EVIDENCE = ROOT / "build/reports/extension/EXT-B-BOILER-REWORK-01"
IDS = (
    "high_pressure_boiler_hot_coolant_port",
    "high_pressure_boiler_cold_coolant_port",
)
EXPORT_PATH = ROOT / "tools/art-assets/export.py"

# 复用项目现有限制SVG导出器，只导出本任务两张贴图。
spec = importlib.util.spec_from_file_location("strict_svg_export", EXPORT_PATH)
if spec is None or spec.loader is None:
    raise RuntimeError("无法读取项目SVG导出器")
exporter = importlib.util.module_from_spec(spec)
spec.loader.exec_module(exporter)


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    palette = json.loads((ART / "palette.json").read_text(encoding="utf-8-sig"))
    rendered: list[Image.Image] = []
    for block_id, palette_id in zip(IDS, ("hot", "cold"), strict=True):
        svg_path = ART / f"{block_id}.svg"
        svg_text = svg_path.read_text(encoding="utf-8-sig")
        texture = exporter.render_svg(svg_text, set(palette[palette_id].values()))
        target = ASSETS / "textures/block" / f"{block_id}.png"
        target.parent.mkdir(parents=True, exist_ok=True)
        texture.save(target, format="PNG", optimize=True)
        rendered.append(texture)

        variants = {}
        for direction, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            variant = {"model": f"create_nuclear_industry:block/{block_id}"}
            if rotation:
                variant["y"] = rotation
            variants[f"facing={direction}"] = variant
        write_json(ASSETS / "blockstates" / f"{block_id}.json", {"variants": variants})

        write_json(ASSETS / "models/block" / f"{block_id}.json", {
            "parent": "minecraft:block/block",
            "textures": {
                "side": "create_nuclear_industry:block/high_pressure_boiler/casing_side",
                "front": f"create_nuclear_industry:block/{block_id}",
                "particle": "create_nuclear_industry:block/high_pressure_boiler/casing_side",
            },
            "elements": [{
                "from": [0, 0, 0], "to": [16, 16, 16],
                "faces": {
                    "north": {"texture": "#front"},
                    "south": {"texture": "#side"},
                    "east": {"texture": "#side"},
                    "west": {"texture": "#side"},
                    "up": {"texture": "#side"},
                    "down": {"texture": "#side"},
                },
            }],
        })
        write_json(ASSETS / "models/item" / f"{block_id}.json", {"parent": f"create_nuclear_industry:block/{block_id}"})

    # 用Nearest放大展示两种接口面，不经过Minecraft客户端渲染。
    scale = 12
    canvas = Image.new("RGBA", (2 * 16 * scale + 72, 16 * scale + 86), (30, 36, 40, 255))
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.load_default()
    labels = ("HOT / INLET", "COLD / OUTLET")
    for index, (texture, label) in enumerate(zip(rendered, labels, strict=True)):
        x = 24 + index * (16 * scale + 24)
        canvas.alpha_composite(texture.resize((16 * scale, 16 * scale), Image.Resampling.NEAREST), (x, 18))
        draw.text((x, 16 * scale + 30), label, fill=(230, 235, 235, 255), font=font)
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    canvas.save(EVIDENCE / "coolant-port-preview.png", optimize=True)

    manifest = {
        "source": [f"{block_id}.svg" for block_id in IDS],
        "outputs": [
            f"src/main/resources/assets/create_nuclear_industry/{kind}/{block_id}.{ext}"
            for block_id in IDS
            for kind, ext in (("blockstates", "json"), ("models/block", "json"), ("models/item", "json"), ("textures/block", "png"))
        ],
        "method": "tools/art-assets/export.py::render_svg；显式限制到palette.json中对应端口色板",
        "preview": "build/reports/extension/EXT-B-BOILER-REWORK-01/coolant-port-preview.png",
    }
    (ART / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
