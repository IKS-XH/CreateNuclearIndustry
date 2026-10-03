"""EXT-A-FUEL-02C五种装配材料纹理的专用生成器。"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / "tools" / "art-assets"
sys.path.insert(0, str(ART))
import export as strict_exporter

ASSET = ROOT / "src/main/resources/assets/create_nuclear_industry"
SOURCE_DIR = ART / "sources" / "fuel-02c"
GENERATED_DIR = ART / "generated" / "item"
EVIDENCE_DIR = ROOT / "build/reports/extension/EXT-A-FUEL-02C-assets"
REPORT = ROOT / "build/reports/extension/EXT-A-FUEL-02C-assets.md"

# 颜色沿用钢板的蓝灰色阶，并以既有锡铅锭的明亮顶面表现银灰合金。
PALETTE = {
    "solder_ingot": ["#28353A", "#D1DFDA", "#A3B7B8", "#788E94", "#5B7078", "#3E5058"],
    "fuel_cladding_tube": ["#28353A", "#D1DFDA", "#A3B7B8", "#788E94", "#5B7078", "#3E5058", "#151C20"],
    "steel_mesh": ["#28353A", "#D1DFDA", "#A3B7B8", "#788E94", "#5B7078", "#3E5058"],
    "steel_grate": ["#28353A", "#D1DFDA", "#A3B7B8", "#788E94", "#5B7078", "#3E5058"],
    "incomplete_steel_grate": ["#28353A", "#D1DFDA", "#A3B7B8", "#788E94", "#5B7078", "#3E5058", "#C36B28", "#F1A642"],
}

# 每个整数矩形代表可直接编辑的16×16像素区域，不使用抗锯齿或渐变。
PIXELS: dict[str, list[tuple[int, int, int, int, str]]] = {
    "solder_ingot": [
        (5, 3, 6, 1, "#28353A"), (4, 4, 1, 1, "#28353A"), (5, 4, 5, 1, "#D1DFDA"), (10, 4, 1, 1, "#A3B7B8"), (11, 4, 1, 1, "#28353A"),
        (3, 5, 1, 1, "#28353A"), (4, 5, 1, 1, "#D1DFDA"), (5, 5, 6, 1, "#A3B7B8"), (11, 5, 1, 1, "#3E5058"), (12, 5, 1, 1, "#28353A"),
        (2, 6, 1, 1, "#28353A"), (3, 6, 1, 1, "#D1DFDA"), (4, 6, 6, 1, "#A3B7B8"), (10, 6, 2, 1, "#788E94"), (12, 6, 1, 1, "#3E5058"), (13, 6, 1, 1, "#28353A"),
        (2, 7, 1, 1, "#28353A"), (3, 7, 2, 1, "#A3B7B8"), (5, 7, 6, 1, "#788E94"), (11, 7, 2, 1, "#5B7078"), (13, 7, 1, 1, "#28353A"),
        (2, 8, 1, 1, "#28353A"), (3, 8, 8, 1, "#788E94"), (11, 8, 2, 1, "#5B7078"), (13, 8, 1, 1, "#28353A"),
        (3, 9, 1, 1, "#28353A"), (4, 9, 7, 1, "#5B7078"), (11, 9, 1, 1, "#3E5058"), (12, 9, 1, 1, "#28353A"),
        (4, 10, 1, 1, "#28353A"), (5, 10, 6, 1, "#3E5058"), (11, 10, 1, 1, "#28353A"),
        (5, 11, 6, 1, "#28353A"), (6, 12, 4, 1, "#28353A"),
    ],
    "fuel_cladding_tube": [
        # 管身斜向延伸，左端的暗孔与亮色环形边缘明确表示空心截面。
        (6, 3, 6, 1, "#28353A"), (5, 4, 8, 1, "#28353A"), (8, 4, 5, 1, "#A3B7B8"),
        (4, 5, 2, 1, "#28353A"), (6, 5, 7, 1, "#D1DFDA"), (13, 5, 1, 1, "#3E5058"),
        (3, 6, 1, 1, "#28353A"), (4, 6, 2, 1, "#D1DFDA"), (6, 6, 7, 1, "#A3B7B8"), (13, 6, 1, 1, "#28353A"),
        (3, 7, 1, 1, "#28353A"), (4, 7, 1, 1, "#A3B7B8"), (5, 7, 1, 1, "#151C20"), (6, 7, 7, 1, "#788E94"), (13, 7, 1, 1, "#28353A"),
        (3, 8, 1, 1, "#28353A"), (4, 8, 1, 1, "#788E94"), (5, 8, 1, 1, "#151C20"), (6, 8, 7, 1, "#5B7078"), (13, 8, 1, 1, "#28353A"),
        (3, 9, 1, 1, "#28353A"), (4, 9, 2, 1, "#5B7078"), (6, 9, 7, 1, "#3E5058"), (13, 9, 1, 1, "#28353A"),
        (4, 10, 2, 1, "#28353A"), (6, 10, 7, 1, "#28353A"), (5, 11, 7, 1, "#28353A"), (7, 12, 5, 1, "#28353A"),
    ],
    "steel_mesh": [
        # 一像素钢丝彼此交错，未覆盖格子保持透明以展示细网孔。
        (7, 2, 2, 1, "#28353A"), (5, 3, 2, 1, "#28353A"), (7, 3, 1, 1, "#D1DFDA"), (8, 3, 2, 1, "#A3B7B8"), (10, 3, 1, 1, "#28353A"),
        (3, 4, 2, 1, "#28353A"), (5, 4, 1, 1, "#D1DFDA"), (6, 4, 2, 1, "#788E94"), (8, 4, 1, 1, "#D1DFDA"), (9, 4, 2, 1, "#5B7078"), (11, 4, 2, 1, "#28353A"),
        (2, 5, 1, 1, "#28353A"), (3, 5, 1, 1, "#D1DFDA"), (5, 5, 1, 1, "#788E94"), (7, 5, 1, 1, "#5B7078"), (9, 5, 1, 1, "#788E94"), (11, 5, 1, 1, "#A3B7B8"), (13, 5, 1, 1, "#28353A"),
        (2, 6, 1, 1, "#28353A"), (4, 6, 1, 1, "#788E94"), (6, 6, 1, 1, "#D1DFDA"), (8, 6, 1, 1, "#5B7078"), (10, 6, 1, 1, "#D1DFDA"), (12, 6, 1, 1, "#5B7078"), (14, 6, 1, 1, "#28353A"),
        (1, 7, 1, 1, "#28353A"), (3, 7, 1, 1, "#5B7078"), (5, 7, 1, 1, "#A3B7B8"), (7, 7, 1, 1, "#788E94"), (9, 7, 1, 1, "#D1DFDA"), (11, 7, 1, 1, "#788E94"), (13, 7, 1, 1, "#A3B7B8"), (15, 7, 1, 1, "#28353A"),
        (1, 8, 1, 1, "#28353A"), (3, 8, 1, 1, "#A3B7B8"), (5, 8, 1, 1, "#5B7078"), (7, 8, 1, 1, "#D1DFDA"), (9, 8, 1, 1, "#788E94"), (11, 8, 1, 1, "#5B7078"), (13, 8, 1, 1, "#D1DFDA"), (15, 8, 1, 1, "#28353A"),
        (2, 9, 1, 1, "#28353A"), (4, 9, 1, 1, "#5B7078"), (6, 9, 1, 1, "#D1DFDA"), (8, 9, 1, 1, "#788E94"), (10, 9, 1, 1, "#A3B7B8"), (12, 9, 1, 1, "#788E94"), (14, 9, 1, 1, "#28353A"),
        (2, 10, 1, 1, "#28353A"), (3, 10, 1, 1, "#A3B7B8"), (5, 10, 1, 1, "#788E94"), (7, 10, 1, 1, "#5B7078"), (9, 10, 1, 1, "#D1DFDA"), (11, 10, 1, 1, "#788E94"), (13, 10, 1, 1, "#5B7078"), (14, 10, 1, 1, "#28353A"),
        (3, 11, 2, 1, "#28353A"), (5, 11, 1, 1, "#5B7078"), (7, 11, 1, 1, "#A3B7B8"), (9, 11, 1, 1, "#788E94"), (11, 11, 1, 1, "#D1DFDA"), (12, 11, 2, 1, "#28353A"),
        (5, 12, 2, 1, "#28353A"), (7, 12, 1, 1, "#5B7078"), (8, 12, 2, 1, "#A3B7B8"), (10, 12, 1, 1, "#28353A"), (7, 13, 2, 1, "#28353A"),
    ],
    "steel_grate": [
        # 双像素外框配合两根横梁、两根纵梁，形成厚实且孔洞清楚的格架。
        (5, 2, 6, 1, "#28353A"), (3, 3, 2, 1, "#28353A"), (5, 3, 6, 1, "#D1DFDA"), (11, 3, 2, 1, "#28353A"),
        (2, 4, 1, 1, "#28353A"), (3, 4, 2, 1, "#A3B7B8"), (5, 1, 2, 1, "#28353A"), (7, 4, 2, 1, "#788E94"), (9, 4, 2, 1, "#28353A"), (11, 4, 2, 1, "#5B7078"), (13, 4, 1, 1, "#28353A"),
        (2, 5, 1, 8, "#28353A"), (3, 5, 2, 1, "#788E94"), (5, 5, 1, 8, "#28353A"), (6, 5, 1, 1, "#5B7078"), (7, 5, 1, 8, "#3E5058"), (8, 5, 1, 1, "#A3B7B8"), (9, 5, 1, 8, "#28353A"), (10, 5, 1, 1, "#788E94"), (11, 5, 2, 1, "#5B7078"), (13, 5, 1, 8, "#28353A"),
        (3, 6, 2, 1, "#5B7078"), (6, 6, 1, 1, "#788E94"), (8, 6, 1, 1, "#D1DFDA"), (10, 6, 1, 1, "#5B7078"), (11, 6, 2, 1, "#788E94"),
        (3, 7, 2, 1, "#A3B7B8"), (6, 7, 1, 1, "#D1DFDA"), (8, 7, 1, 1, "#788E94"), (10, 7, 1, 1, "#A3B7B8"), (11, 7, 2, 1, "#5B7078"),
        (3, 8, 2, 1, "#5B7078"), (6, 8, 1, 1, "#A3B7B8"), (8, 8, 1, 1, "#D1DFDA"), (10, 8, 1, 1, "#788E94"), (11, 8, 2, 1, "#A3B7B8"),
        (3, 9, 2, 1, "#788E94"), (6, 9, 1, 1, "#D1DFDA"), (8, 9, 1, 1, "#5B7078"), (10, 9, 1, 1, "#D1DFDA"), (11, 9, 2, 1, "#5B7078"),
        (3, 10, 2, 1, "#5B7078"), (6, 10, 1, 1, "#A3B7B8"), (8, 10, 1, 1, "#788E94"), (10, 10, 1, 1, "#A3B7B8"), (11, 10, 2, 1, "#788E94"),
        (3, 11, 2, 1, "#A3B7B8"), (6, 11, 1, 1, "#D1DFDA"), (8, 11, 1, 1, "#A3B7B8"), (10, 11, 1, 1, "#D1DFDA"), (11, 11, 2, 1, "#5B7078"),
        (3, 12, 2, 1, "#28353A"), (5, 12, 6, 1, "#5B7078"), (11, 12, 2, 1, "#28353A"), (4, 13, 8, 1, "#28353A"),
    ],
    "incomplete_steel_grate": [
        # 半成品仍有格架主框，只完成部分横纵梁，并用暖色端点表示尚未封合的钢材。
        (5, 2, 5, 1, "#28353A"), (3, 3, 2, 1, "#28353A"), (5, 3, 5, 1, "#D1DFDA"), (10, 3, 2, 1, "#28353A"),
        (2, 4, 1, 1, "#28353A"), (3, 4, 2, 1, "#A3B7B8"), (5, 4, 1, 1, "#28353A"), (6, 4, 4, 1, "#788E94"), (10, 4, 1, 1, "#28353A"), (11, 4, 1, 1, "#5B7078"), (12, 4, 1, 1, "#28353A"),
        (2, 5, 1, 7, "#28353A"), (3, 5, 2, 1, "#788E94"), (5, 5, 1, 6, "#28353A"), (6, 5, 1, 1, "#F1A642"), (7, 5, 1, 6, "#3E5058"), (8, 5, 1, 1, "#A3B7B8"), (9, 5, 1, 6, "#28353A"), (10, 5, 1, 1, "#C36B28"), (11, 5, 2, 1, "#5B7078"), (13, 5, 1, 5, "#28353A"),
        (3, 6, 2, 1, "#5B7078"), (8, 6, 1, 1, "#D1DFDA"), (11, 6, 2, 1, "#788E94"),
        (3, 7, 2, 1, "#A3B7B8"), (8, 7, 1, 1, "#788E94"), (11, 7, 2, 1, "#5B7078"),
        (3, 8, 2, 1, "#5B7078"), (8, 8, 1, 1, "#D1DFDA"), (11, 8, 2, 1, "#A3B7B8"),
        (3, 9, 2, 1, "#788E94"), (8, 9, 1, 1, "#5B7078"), (11, 9, 2, 1, "#5B7078"),
        (3, 10, 2, 1, "#5B7078"), (8, 10, 1, 1, "#788E94"), (11, 10, 2, 1, "#788E94"),
        (3, 11, 2, 1, "#28353A"), (5, 11, 2, 1, "#F1A642"), (7, 11, 1, 1, "#5B7078"), (8, 11, 1, 1, "#C36B28"), (9, 11, 2, 1, "#28353A"), (11, 11, 2, 1, "#5B7078"),
        (4, 12, 5, 1, "#28353A"), (9, 12, 3, 1, "#28353A"), (4, 13, 8, 1, "#28353A"),
    ],
}


def make_svg(rectangles: list[tuple[int, int, int, int, str]]) -> str:
    """把整数像素矩形序列转成严格渲染器支持的SVG子集。"""
    body = "\n".join(
        f'  <rect x="{x}" y="{y}" width="{width}" height="{height}" fill="{color}"/>'
        for x, y, width, height, color in rectangles
    )
    return ('<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" '
            'viewBox="0 0 16 16" shape-rendering="crispEdges">\n' + body + "\n</svg>\n")


def initialize_missing_sources() -> None:
    """仅为缺少的源稿建立首次像素稿，保留已有SVG编辑。"""
    SOURCE_DIR.mkdir(parents=True, exist_ok=True)
    for name, rectangles in PIXELS.items():
        source = SOURCE_DIR / f"{name}.svg"
        if not source.exists():
            source.write_text(make_svg(rectangles), encoding="utf-8")


def write_json(path: Path, data: dict) -> None:
    """以稳定缩进写出本批原生物品模型。"""
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def render_and_install() -> dict[str, tuple[int, int, int, int]]:
    """严格渲染五个SVG，再仅更新本批候选PNG和游戏PNG。"""
    result = {}
    for name in PIXELS:
        source = SOURCE_DIR / f"{name}.svg"
        if not source.is_file():
            raise FileNotFoundError(f"缺少可编辑SVG源稿：{source}；需先显式运行 --initialize-sources")
        image = strict_exporter.render_svg(source.read_text(encoding="utf-8"), set(PALETTE[name]), (16, 16))
        if image.size != (16, 16) or image.mode != "RGBA":
            raise ValueError(f"{name}渲染尺寸或色彩模式错误：{image.size} {image.mode}")
        alpha = image.getchannel("A")
        alpha_counts = {value: count for count, value in (alpha.getcolors(maxcolors=256) or [])}
        if set(alpha_counts) - {0, 255}:
            raise ValueError(f"{name}含有非二值透明度")
        GENERATED_DIR.mkdir(parents=True, exist_ok=True)
        image.save(GENERATED_DIR / f"{name}.png")
        image.save(ASSET / "textures/item" / f"{name}.png")
        write_json(ASSET / "models/item" / f"{name}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"create_nuclear_industry:item/{name}"},
        })
        result[name] = (*alpha.getbbox(), alpha_counts.get(0, 0))
    return result


def draw_checker(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], cell: int = 4) -> None:
    """绘制棋盘底，便于检查图标的透明背景。"""
    x0, y0, x1, y1 = box
    for y in range(y0, y1, cell):
        for x in range(x0, x1, cell):
            color = "#E8E8E8" if ((x - x0) // cell + (y - y0) // cell) % 2 == 0 else "#B8B8B8"
            draw.rectangle((x, y, min(x + cell - 1, x1 - 1), min(y + cell - 1, y1 - 1)), fill=color)


def create_preview() -> Path:
    """生成含原尺寸与最近邻放大图的单张并排对照预览。"""
    names = list(PIXELS)
    width, height = 1120, 340
    image = Image.new("RGB", (width, height), "#f5f5f5")
    draw = ImageDraw.Draw(image)
    font = ImageFont.load_default()
    draw.text((18, 14), "EXT-A-FUEL-02C | actual size (16px) and nearest-neighbor enlarged (8x)", fill="#202020", font=font)
    col_w = 216
    for index, name in enumerate(names):
        x = 16 + index * col_w
        draw.text((x, 48), name, fill="#202020", font=font)
        small = Image.open(GENERATED_DIR / f"{name}.png").convert("RGBA")
        draw.text((x, 76), "1x original", fill="#505050", font=font)
        draw_checker(draw, (x, 96, x + 64, 160), cell=8)
        image.paste(small, (x + 24, 120), small)
        draw.text((x, 184), "8x nearest", fill="#505050", font=font)
        draw_checker(draw, (x, 204, x + 128, 276), cell=8)
        enlarged = small.resize((128, 128), Image.Resampling.NEAREST)
        image.paste(enlarged, (x, 204), enlarged)
    EVIDENCE_DIR.mkdir(parents=True, exist_ok=True)
    target = EVIDENCE_DIR / "fuel-02c-five-items-preview.png"
    image.save(target)
    return target


def main() -> None:
    parser = argparse.ArgumentParser(description="生成燃料02C五种材料纹理与原生物品模型")
    parser.add_argument("--initialize-sources", action="store_true", help="仅建立不存在的SVG源稿")
    args = parser.parse_args()
    if args.initialize_sources:
        initialize_missing_sources()
        print(f"已初始化缺少的SVG源稿：{SOURCE_DIR}")
    bounds = render_and_install()
    preview = create_preview()
    print(f"已导出五项资源；透明像素数与非透明边界：{bounds}")
    print(f"预览：{preview}")


if __name__ == "__main__":
    main()
