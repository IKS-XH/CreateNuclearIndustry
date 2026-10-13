#!/usr/bin/env python3
"""反应堆驱动器/换料口本批SVG初始化、PNG导出和只读检查。"""

from __future__ import annotations

import argparse
import base64
import hashlib
import io
import json
import re
import shutil
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

from PIL import Image, ImageDraw


HERE = Path(__file__).resolve().parent
SPRITES = (
    "control_rod_drive_top",
    "control_rod_drive_side",
    "reactor_refueling_port_top",
    "reactor_refueling_port_side",
)
BASE_DIR = HERE / "sources" / "base"
CT_DIR = HERE / "sources" / "ct"
OUTPUT = HERE / "generated"
PREVIEW = HERE / "preview"
COLORS = {
    "shadow": "#303638",
    "steel_dark": "#45494B",
    "steel_shadow": "#4B5351",
    "steel_mid": "#66737A",
    "steel_edge": "#77817C",
    "steel": "#A5AAA1",
    "steel_light": "#D2D1C4",
    "steel_white": "#E4E5DD",
    "bronze_shadow": "#715035",
    "bronze": "#AD7A42",
    "copper": "#D2A359",
    "copper_light": "#F0CF87",
}
PALETTE = set(COLORS.values())
REFERENCE_DIR = HERE / "references" / "original" / "base"


def canvas(color: str) -> list[list[str]]:
    """创建16×16不透明整数像素面。"""
    return [[color for _ in range(16)] for _ in range(16)]


def box(grid: list[list[str]], x: int, y: int, width: int, height: int, color: str) -> None:
    """用整数坐标填充闭区间以外的矩形像素。"""
    if min(x, y, width, height) < 0 or x + width > 16 or y + height > 16:
        raise ValueError(f"矩形越界: {x},{y},{width},{height}")
    for py in range(y, y + height):
        for px in range(x, x + width):
            grid[py][px] = color


def frame(grid: list[list[str]], x: int, y: int, width: int, height: int, color: str) -> None:
    """绘制一像素方形轮廓。"""
    box(grid, x, y, width, 1, color)
    box(grid, x, y + height - 1, width, 1, color)
    box(grid, x, y, 1, height, color)
    box(grid, x + width - 1, y, 1, height, color)


def device_design(sprite: str) -> list[list[str]]:
    """依sprite生成为独立设备身份服务的16×16像素矩阵。"""
    grid = canvas(COLORS["steel_mid"])
    shadow = COLORS["shadow"]
    steel_dark = COLORS["steel_dark"]
    steel_shadow = COLORS["steel_shadow"]
    steel_mid = COLORS["steel_mid"]
    steel_edge = COLORS["steel_edge"]
    steel = COLORS["steel"]
    light = COLORS["steel_light"]
    white = COLORS["steel_white"]
    bronze_shadow = COLORS["bronze_shadow"]
    bronze = COLORS["bronze"]
    copper = COLORS["copper"]
    copper_light = COLORS["copper_light"]

    if sprite == "control_rod_drive_top":
        # 宽钢承力台围绕方杆通道；通道周围留下清晰的机构操作面。
        box(grid, 2, 2, 12, 12, steel_dark)
        box(grid, 3, 3, 10, 10, steel_edge)
        box(grid, 4, 4, 8, 8, steel_mid)
        frame(grid, 5, 5, 6, 6, steel_dark)
        box(grid, 6, 6, 4, 4, shadow)
        box(grid, 7, 7, 2, 2, steel_dark)
        # 两条宽导轨与上下轴承桥形成驱动器的机械符号。
        box(grid, 3, 4, 2, 8, steel_shadow)
        box(grid, 4, 4, 1, 8, steel)
        box(grid, 11, 4, 2, 8, steel_dark)
        box(grid, 11, 4, 1, 8, light)
        box(grid, 5, 3, 6, 2, steel)
        box(grid, 5, 11, 6, 2, steel_shadow)
        box(grid, 6, 3, 4, 1, white)
        box(grid, 6, 12, 4, 1, steel_edge)
        # 少量黄铜锁销只作紧固件，不抢占钢色主体。
        for px, py in ((3, 3), (12, 3), (3, 12), (12, 12)):
            grid[py][px] = copper
    elif sprite == "control_rod_drive_side":
        # 侧面显示两根承载导柱、中心执行缸和横向轴承圈。
        box(grid, 2, 2, 12, 12, steel_dark)
        box(grid, 3, 3, 10, 10, steel_edge)
        box(grid, 4, 3, 2, 10, steel_dark)
        box(grid, 5, 4, 1, 8, steel)
        box(grid, 10, 4, 1, 8, light)
        box(grid, 11, 3, 2, 10, steel_shadow)
        box(grid, 6, 4, 4, 8, steel_mid)
        box(grid, 7, 5, 2, 6, shadow)
        box(grid, 7, 5, 1, 6, steel_dark)
        box(grid, 6, 4, 4, 1, light)
        box(grid, 6, 11, 4, 1, steel_shadow)
        box(grid, 4, 6, 2, 2, steel)
        box(grid, 10, 8, 2, 2, steel)
        grid[6][4] = copper
        grid[9][11] = copper
    elif sprite == "reactor_refueling_port_top":
        # 闭合铜舱盖以连续大色块、铰链和两侧锁扣区别于开口导轨。
        box(grid, 2, 2, 12, 12, steel_dark)
        box(grid, 3, 3, 10, 10, bronze_shadow)
        box(grid, 4, 4, 8, 8, bronze)
        box(grid, 4, 4, 8, 1, copper_light)
        box(grid, 4, 5, 1, 6, copper)
        box(grid, 11, 5, 1, 6, bronze_shadow)
        box(grid, 4, 11, 8, 1, bronze_shadow)
        # 中央压板是闭合面，不画成与驱动器相同的黑色方槽。
        box(grid, 6, 6, 4, 4, copper)
        box(grid, 6, 7, 4, 2, bronze)
        box(grid, 7, 7, 2, 1, copper_light)
        # 上侧双铰链与左右锁扣给出更大的暖铜轮廓。
        box(grid, 4, 2, 2, 2, steel_dark)
        box(grid, 4, 2, 1, 1, copper_light)
        box(grid, 10, 2, 2, 2, steel_dark)
        box(grid, 11, 2, 1, 1, copper_light)
        box(grid, 2, 7, 2, 2, bronze_shadow)
        box(grid, 2, 7, 1, 1, copper_light)
        box(grid, 12, 7, 2, 2, bronze_shadow)
        box(grid, 13, 7, 1, 1, copper_light)
    elif sprite == "reactor_refueling_port_side":
        # 侧面把盖板厚度、铰链座和下方锁闩表现为铜色操作组件。
        box(grid, 2, 2, 12, 12, steel_dark)
        box(grid, 3, 3, 10, 10, steel_edge)
        box(grid, 4, 4, 8, 8, bronze_shadow)
        box(grid, 5, 4, 7, 7, bronze)
        box(grid, 5, 4, 7, 1, copper_light)
        box(grid, 5, 5, 1, 5, copper)
        box(grid, 11, 5, 1, 5, bronze_shadow)
        box(grid, 5, 10, 7, 2, bronze_shadow)
        box(grid, 6, 6, 5, 3, copper)
        box(grid, 7, 7, 3, 1, copper_light)
        box(grid, 4, 3, 2, 2, steel_dark)
        box(grid, 10, 3, 2, 2, steel_dark)
        box(grid, 6, 12, 4, 1, light)
        box(grid, 7, 13, 2, 1, bronze)
    else:
        raise ValueError(f"未知sprite: {sprite}")
    return grid


def ct_context(index: int) -> tuple[bool, bool, bool, bool]:
    """按锁定RECTANGLE图集索引返回上、下、左、右连接位。"""
    col, row = index % 4, index // 4
    return row in (1, 2), row in (0, 1), col in (2, 3), col in (1, 2)


def connected_tile(base: list[list[str]], index: int) -> list[list[str]]:
    """仅在缺少邻格的外边添加钢压框，保留内侧功能图案。"""
    grid = [row[:] for row in base]
    up, down, left, right = ct_context(index)
    shadow, edge, dark = COLORS["shadow"], COLORS["steel_light"], COLORS["steel_shadow"]
    if not up:
        box(grid, 0, 0, 16, 1, shadow)
        box(grid, 0, 1, 16, 1, edge)
    if not down:
        box(grid, 0, 15, 16, 1, shadow)
        box(grid, 0, 14, 16, 1, dark)
    if not left:
        box(grid, 0, 0, 1, 16, shadow)
        box(grid, 1, 0, 1, 16, COLORS["steel_edge"])
    if not right:
        box(grid, 15, 0, 1, 16, shadow)
        box(grid, 14, 0, 1, 16, dark)
    bolts = ((1, 1, not up and not left), (14, 1, not up and not right),
             (1, 14, not down and not left), (14, 14, not down and not right))
    for x, y, visible in bolts:
        if visible:
            grid[y][x] = COLORS["steel_white"]
    return grid


def write_svg(grid: list[list[str]], path: Path, note: str) -> None:
    """将整数像素行程编码为仅含整数rect的可编辑SVG。"""
    path.parent.mkdir(parents=True, exist_ok=True)
    pieces = [
        '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges">',
        f"  <!-- {note} -->",
    ]
    for y, row in enumerate(grid):
        x = 0
        while x < 16:
            color = row[x]
            end = x + 1
            while end < 16 and row[end] == color:
                end += 1
            pieces.append(f'  <rect x="{x}" y="{y}" width="{end - x}" height="1" fill="{color}"/>')
            x = end
    pieces.append("</svg>")
    path.write_text("\n".join(pieces) + "\n", encoding="utf-8")


def render_svg(path: Path) -> Image.Image:
    """严格读取本批SVG，并将整数rect逐像素渲染为RGBA。"""
    root = ET.fromstring(path.read_text(encoding="utf-8"))
    if root.tag != "{http://www.w3.org/2000/svg}svg" or root.attrib.get("viewBox") != "0 0 16 16":
        raise ValueError(f"SVG根尺寸不符: {path}")
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    for item in root:
        if item.tag != "{http://www.w3.org/2000/svg}rect":
            raise ValueError(f"仅允许rect绘图元素: {path}")
        values = []
        for key in ("x", "y", "width", "height"):
            value = item.attrib.get(key, "")
            if not re.fullmatch(r"(?:0|[1-9][0-9]*)", value):
                raise ValueError(f"SVG坐标/尺寸必须为非负整数: {path}")
            values.append(int(value))
        x, y, width, height = values
        color = item.attrib.get("fill", "")
        if color not in PALETTE or width < 1 or height < 1 or x + width > 16 or y + height > 16:
            raise ValueError(f"SVG超出本批整数色板/范围: {path}")
        draw.rectangle((x, y, x + width - 1, y + height - 1), fill=color)
    return image


def source_paths() -> list[Path]:
    return sorted(BASE_DIR.glob("*.svg")) + sorted(CT_DIR.glob("*/tile_*.svg"))


def build_output_images() -> dict[Path, Image.Image]:
    """从已保存SVG在内存构造四基础图与四张CT图集。"""
    expected = 4 + 4 * 16
    sources = source_paths()
    if len(sources) != expected:
        raise ValueError(f"SVG数量错误: 期望{expected}，实际{len(sources)}；请先执行--seed")
    outputs: dict[Path, Image.Image] = {}
    for sprite in SPRITES:
        base_path = BASE_DIR / f"{sprite}.svg"
        base = render_svg(base_path)
        outputs[Path("base") / f"{sprite}.png"] = base

        atlas = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        for index in range(16):
            tile_path = CT_DIR / sprite / f"tile_{index:02d}.svg"
            tile = render_svg(tile_path)
            if index == 12 and tile.tobytes() != base.tobytes():
                raise ValueError(f"{sprite}孤立CT格与基础sprite不一致")
            atlas.paste(tile, ((index % 4) * 16, (index // 4) * 16))
        outputs[Path("reactor_ct") / f"{sprite}.png"] = atlas
    return outputs


def export_images() -> list[Path]:
    """从SVG写出本批四基础sprite与四张CT图集。"""
    OUTPUT.mkdir(parents=True, exist_ok=True)
    outputs = []
    for relative, image in build_output_images().items():
        path = OUTPUT / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path, format="PNG", optimize=False)
        outputs.append(path)
    return outputs


def seed_sources() -> None:
    """首次按本批手绘像素版式展开四基础SVG与64种RECTANGLE格。"""
    BASE_DIR.mkdir(parents=True, exist_ok=True)
    CT_DIR.mkdir(parents=True, exist_ok=True)
    reference_hashes = {}
    formal_base = HERE.parents[2] / "src/main/resources/assets/create_nuclear_industry/textures/block"
    REFERENCE_DIR.mkdir(parents=True, exist_ok=True)
    for sprite in SPRITES:
        snapshot = REFERENCE_DIR / f"{sprite}.png"
        if not snapshot.exists():
            shutil.copy2(formal_base / f"{sprite}.png", snapshot)
        reference_hashes[f"references/original/base/{sprite}.png"] = hashlib.sha256(snapshot.read_bytes()).hexdigest()
    for sprite in SPRITES:
        raw_design = device_design(sprite)
        base = connected_tile(raw_design, 12)
        write_svg(base, BASE_DIR / f"{sprite}.svg", f"{sprite}基础像素稿")
        for index in range(16):
            tile = connected_tile(raw_design, index)
            write_svg(tile, CT_DIR / sprite / f"tile_{index:02d}.svg",
                      f"{sprite} RECTANGLE孤立与邻接上下文 index={index:02d}")
    mapping = {
        "task": "ART-REACTOR-09",
        "source_of_truth": "四张基础16x16 SVG与64张RECTANGLE上下文SVG",
        "tile_size": 16,
        "ct_type": "RECTANGLE",
        "sheet_size": 4,
        "isolated_index": 12,
        "original_base_snapshots": reference_hashes,
        "targets": [
            {
                "name": sprite,
                "base_source": f"sources/base/{sprite}.svg",
                "base_generated": f"generated/base/{sprite}.png",
                "base_formal": f"src/main/resources/assets/create_nuclear_industry/textures/block/{sprite}.png",
                "ct_source_directory": f"sources/ct/{sprite}",
                "ct_generated": f"generated/reactor_ct/{sprite}.png",
                "ct_formal": f"src/main/resources/assets/create_nuclear_industry/textures/block/reactor_ct/{sprite}.png",
            } for sprite in SPRITES
        ],
        "installation": "未授权；仅生成本地候选",
        "read_only_inputs": [
            "tools/art-assets/sources/block",
            "tools/art-assets/generated/block",
            "tools/art-assets/reactor-ct-r1",
            "src/main/resources/assets/create_nuclear_industry/models/block",
            "src/main/resources/assets/create_nuclear_industry/data",
        ],
    }
    (HERE / "mapping.json").write_text(json.dumps(mapping, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (HERE / "palette.json").write_text(json.dumps(COLORS, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def atlas_crop(sprite: str, index: int, x: int, y: int, size: int = 16) -> str:
    """把CT格转为内嵌PNG数据，避免SVG作为img时浏览器屏蔽外链资源。"""
    col, row = index % 4, index // 4
    href = png_data_uri(OUTPUT / "reactor_ct" / f"{sprite}.png")
    return (f'<svg x="{x}" y="{y}" width="{size}" height="{size}" viewBox="{col * 16} {row * 16} 16 16" '
            f'preserveAspectRatio="none"><image href="{href}" x="0" y="0" width="64" height="64" '
            'image-rendering="pixelated"/></svg>')


def png_data_uri(path: Path) -> str:
    """将本地预览PNG编码进SVG，确保单独作为img打开时也能显示。"""
    return "data:image/png;base64," + base64.b64encode(path.read_bytes()).decode("ascii")


def preview_svg() -> str:
    """生成12×12近远混排阵列与方杆插入状态的离线矢量预览。"""
    parts = ['<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="768" height="768" viewBox="0 0 192 192" shape-rendering="crispEdges">',
             '<rect width="192" height="192" fill="#303638"/>']
    grid = []
    for z in range(12):
        row = []
        for x in range(12):
            if x in (0, 11) or z in (0, 11):
                name = "reactor_casing_top"
                source = png_data_uri(HERE.parents[2] / "src/main/resources/assets/create_nuclear_industry/textures/block/reactor_ct/reactor_casing_top.png")
                col, atlas_row = 2 * (x > 0) + (x < 11), (1 if z > 0 else 0) + (0 if z < 11 else 2)
                idx = int(col) + 4 * int(atlas_row)
                row.append("C")
                parts.append(f'<svg x="{x * 16}" y="{z * 16}" width="16" height="16" viewBox="{idx % 4 * 16} {idx // 4 * 16} 16 16" preserveAspectRatio="none"><image href="{source}" width="64" height="64" image-rendering="pixelated"/></svg>')
            else:
                name = "control_rod_drive_top" if ((x // 2 + z // 2) % 2 == 0) else "reactor_refueling_port_top"
                row.append("D" if name.startswith("control") else "R")
                # 阵列格的CT索引按同一外表面四邻接几何选择。
                up, down, left, right = z > 0, z < 11, x > 0, x < 11
                idx = (1 if up else 0) + (0 if down else 2)
                idx = (2 if left else 0) + (1 if right else 0) + 4 * idx
                parts.append(atlas_crop(name, idx, x * 16, z * 16))
        grid.append("".join(row))
    parts.append('</svg>')
    return "\n".join(parts)


def rod_preview_svg() -> str:
    """用拟定坐标对照方杆完全插入及退出后的顶面遮挡范围。"""
    drive = png_data_uri(OUTPUT / "base" / "control_rod_drive_top.png")
    def panel(x: int, inserted: bool, title: str) -> str:
        layers = [f'<text x="{x}" y="20" fill="#E4E5DD" font-family="sans-serif" font-size="12">{title}</text>',
                  f'<svg x="{x}" y="34" width="192" height="192" viewBox="0 0 16 16" shape-rendering="crispEdges"><image href="{drive}" x="0" y="0" width="16" height="16" image-rendering="pixelated"/>']
        # 仅绘制本格命中椭球在X/Z面的投影；法向半轴不显示，避免误读为矩形热点。
        layers.append('<ellipse cx="8" cy="1.4" rx="5.6" ry="1.2" fill="none" stroke="#F0CF87" stroke-width="0.25" stroke-dasharray="0.5 0.35"/>')
        if inserted:
            layers.append('<rect x="2.8" y="2.8" width="10.4" height="10.4" fill="none" stroke="#D2D1C4" stroke-width="0.45"/>')
            layers.append('<rect x="3.2" y="3.2" width="9.6" height="9.6" fill="#4B5351" stroke="#303638" stroke-width="0.35"/>')
            layers.append('<rect x="3.7" y="3.7" width="0.55" height="8.6" fill="#A5AAA1"/>')
            layers.append('<rect x="11.75" y="3.7" width="0.55" height="8.6" fill="#66737A"/>')
        layers.append('</svg>')
        return "".join(layers)
    return ('<svg xmlns="http://www.w3.org/2000/svg" width="600" height="250" viewBox="0 0 600 250">'
            '<rect width="600" height="250" fill="#252A2D"/>'
            + panel(42, True, "方杆完全插入 · 0.6格杆 / 0.65格端箍")
            + panel(348, False, "方杆完全拔出 · 通道与导轨外露")
            + '<text x="42" y="244" fill="#F0CF87" font-family="sans-serif" font-size="10">虚线椭圆：顶/底命中椭球的X/Z投影，半轴0.35/0.075，中心x=0.5,z=0.0875；法向半轴0.10未显示。</text>'
            + '</svg>')


def write_previews() -> None:
    PREVIEW.mkdir(parents=True, exist_ok=True)
    (PREVIEW / "mixed-array.svg").write_text(preview_svg(), encoding="utf-8")
    (PREVIEW / "rod-occlusion.svg").write_text(rod_preview_svg(), encoding="utf-8")
    html = """<!doctype html>
<html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>反应堆驱动器与换料口候选</title>
<style>
body{margin:24px auto;padding:0 18px;max-width:1240px;background:#202526;color:#e4e5dd;font:16px/1.5 "Segoe UI","Microsoft YaHei",sans-serif}
h1,h2{color:#f0cf87}section{margin:28px 0;padding:16px;background:#303638;border:1px solid #59615e;border-radius:8px}
.pair,.four{display:grid;grid-template-columns:repeat(auto-fit,minmax(390px,1fr));gap:16px}
img{max-width:100%;height:auto;image-rendering:pixelated;image-rendering:crisp-edges;background:#45494b}
.tile{width:192px;height:192px}.atlas{width:256px;height:256px}.arrayNear{width:min(768px,100%)}.arrayFar{width:min(384px,100%)}
.label{color:#bfc7c0;font-size:14px}.warning{border-left:3px solid #d2a359;padding-left:12px}
@media(max-width:620px){body{margin:10px;padding:0 8px}}
</style>
<body>
<h1>控制棒驱动器与反应堆换料口 · SVG候选</h1>
<p class="warning">本页为SVG/PNG离线材质与阵列示意，不是Minecraft截图或最终客户端排序。顶/底操作位置按09共享补充冻结；椭圆只显示命中椭球X/Z投影。</p>
<section><h2>基础16×16：原稿与新稿</h2><div class="pair">
<div><h3>驱动器顶面</h3><img class="tile" src="../references/original/base/control_rod_drive_top.png"><img class="tile" src="../generated/base/control_rod_drive_top.png"></div>
<div><h3>驱动器侧面</h3><img class="tile" src="../references/original/base/control_rod_drive_side.png"><img class="tile" src="../generated/base/control_rod_drive_side.png"></div>
<div><h3>换料舱盖顶面</h3><img class="tile" src="../references/original/base/reactor_refueling_port_top.png"><img class="tile" src="../generated/base/reactor_refueling_port_top.png"></div>
<div><h3>换料口侧面</h3><img class="tile" src="../references/original/base/reactor_refueling_port_side.png"><img class="tile" src="../generated/base/reactor_refueling_port_side.png"></div>
</div><p class="label">每组左侧为本批保存的原版只读快照（SHA见mapping.json），右侧为候选SVG导出。</p></section>
<section><h2>12×12密集混排与近远读性</h2><p class="label">混排含驱动器、换料舱盖和原反应堆钢外壳；外缘引用现行只读CT钢壳图。</p><img class="arrayNear" src="mixed-array.svg"><p>近景 · 4倍最近邻放大</p><img class="arrayFar" src="mixed-array.svg"><p>远景 · 2倍最近邻缩放</p></section>
<section><h2>顶面驱动器：方杆遮挡对照</h2><img src="rod-occlusion.svg" style="width:min(900px,100%)"><p class="label">方杆截面0.6格、端箍0.65格；虚线椭圆仅示顶/底本格命中椭球的X/Z投影（X半轴0.35、Z半轴0.075），法向半轴0.10不在此二维图中显示。</p></section>
<section><h2>RECTANGLE CT 4×4图集</h2><div class="four">
<div><h3>驱动器顶面</h3><img class="atlas" src="../generated/reactor_ct/control_rod_drive_top.png"></div>
<div><h3>驱动器侧面</h3><img class="atlas" src="../generated/reactor_ct/control_rod_drive_side.png"></div>
<div><h3>换料舱盖顶面</h3><img class="atlas" src="../generated/reactor_ct/reactor_refueling_port_top.png"></div>
<div><h3>换料口侧面</h3><img class="atlas" src="../generated/reactor_ct/reactor_refueling_port_side.png"></div>
</div><p class="label">索引0–15沿用Create RECTANGLE 4×4布局，孤立格index12；未安装至正式路径。</p></section>
</body></html>"""
    (PREVIEW / "index.html").write_text(html, encoding="utf-8")


def check() -> None:
    sources = source_paths()
    if len(sources) != 68:
        raise ValueError(f"SVG数量错误: {len(sources)}")
    for sprite in SPRITES:
        base = render_svg(BASE_DIR / f"{sprite}.svg")
        if base.size != (16, 16):
            raise ValueError("基础纹理尺寸错误")
        for index in range(16):
            tile = render_svg(CT_DIR / sprite / f"tile_{index:02d}.svg")
            if tile.tobytes() != render_svg(BASE_DIR / f"{sprite}.svg").tobytes() and index == 12:
                raise ValueError(f"{sprite}孤立格12与基础纹理不一致")
    expected_images = build_output_images()
    expected_outputs = {OUTPUT / relative: image for relative, image in expected_images.items()}
    pngs = sorted(OUTPUT.rglob("*.png"))
    if len(pngs) != 8 or set(pngs) != set(expected_outputs):
        raise ValueError(f"本批generated PNG目标必须恰为8项，实际{len(pngs)}")
    for path in pngs:
        image = Image.open(path).convert("RGBA")
        if image.size != expected_outputs[path].size:
            raise ValueError(f"基础PNG尺寸错误: {path}")
        expected_bytes = io.BytesIO()
        expected_outputs[path].save(expected_bytes, format="PNG", optimize=False)
        if path.read_bytes() != expected_bytes.getvalue():
            raise ValueError(f"已导出PNG与SVG渲染不一致: {path}")
        expected_size = (16, 16) if path.parent.name == "base" else (64, 64)
        if image.size != expected_size:
            raise ValueError(f"PNG尺寸错误: {path}")
    print("CHECK_OK svg=68 base_png=4 atlas_png=4 atlas_size=64x64 ct=RECTANGLE isolated=12 scope=8-local-targets")


def main() -> int:
    parser = argparse.ArgumentParser(description="反应堆驱动器与换料口SVG资产候选")
    parser.add_argument("--seed", action="store_true", help="首次写入手绘矩阵对应的68张SVG与映射")
    parser.add_argument("--check", action="store_true", help="只读检查本批SVG来源和8张本地PNG")
    parser.add_argument("--preview-only", action="store_true", help="仅重建离线预览，不写候选PNG")
    args = parser.parse_args()
    try:
        if args.seed:
            seed_sources()
            print("SEED_OK svg=68 mapping=1 palette=1")
            return 0
        if args.check:
            check()
            return 0
        if args.preview_only:
            write_previews()
            print("PREVIEW_OK svg=2 embedded-local-png=true original-snapshots=true")
            return 0
        outputs = export_images()
        write_previews()
        print(f"EXPORT_OK svg=68 png={len(outputs)} preview=2svg+html")
        return 0
    except (OSError, ValueError, ET.ParseError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
