"""离线导出 EXT-ART-01 的四张候选贴图，不读写游戏资源。

输入仅为本目录固定路径的 SVG 和 palette.json；输出为固定 PNG 与预览。
受限 SVG 使用整数 rect 和色板内的 #RRGGBB，按文档顺序覆盖像素。
任何不支持的元素、属性或非空文本均报错；全部源稿验证后才开始写出。
本工具没有服务端/客户端运行行为，不属于 Minecraft 资源加载器。
"""
from __future__ import annotations

import hashlib
import html
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
ASSETS = (
    ("item/lapis_dust", "LAPIS DUST", "青金石粉"),
    ("item/lead_ingot", "LEAD INGOT", "铅锭"),
    ("item/steel_plate", "STEEL PLATE", "合金钢板"),
    ("block/lead_ore", "LEAD ORE", "铅矿石"),
)
NS = "{http://www.w3.org/2000/svg}"
HEX = re.compile(r"#[0-9A-Fa-f]{6}\Z")
INTEGER = re.compile(r"(?:0|[1-9][0-9]*)\Z")


def read_palette() -> dict[str, dict[str, str]]:
    """读取每张纹理的有限色板；色值不得包含 alpha、别名或外部引用。"""
    palette = json.loads((ROOT / "palette.json").read_text(encoding="utf-8"))
    if not isinstance(palette, dict) or set(palette) != {p.split("/")[1] for p, _, _ in ASSETS}:
        raise ValueError("色板必须恰好列出四张固定资源")
    for name, colors in palette.items():
        if not isinstance(colors, dict) or not 2 <= len(colors) <= 12:
            raise ValueError(f"{name}: 色板必须包含 2 至 12 个命名实色")
        if any(not isinstance(v, str) or not HEX.fullmatch(v) for v in colors.values()):
            raise ValueError(f"{name}: 只支持 #RRGGBB 色值")
        if len({v.upper() for v in colors.values()}) != len(colors):
            raise ValueError(f"{name}: 不允许重复色值")
    return palette


def render_svg(text: str, allowed_colors: set[str]) -> Image.Image:
    """把 16×16 SVG 光栅化为 RGBA；一坐标单位对应一个像素，不做抗锯齿。

    只接受根 svg、无属性注释和直属 rect；矩形必须完全位于画布内。
    XML 实体声明、处理指令、样式、变换和其他扩展都拒绝，避免静默漏画。
    """
    if re.search(r"<!DOCTYPE|<!ENTITY|<\?", text, re.IGNORECASE):
        raise ValueError("不支持 DTD、实体声明或 XML 处理指令")
    parser = ET.XMLParser(target=ET.TreeBuilder(insert_comments=True, insert_pis=True))
    root = ET.fromstring(text, parser=parser)
    required = {"width": "16", "height": "16", "viewBox": "0 0 16 16", "shape-rendering": "crispEdges"}
    if root.tag != NS + "svg" or root.attrib != required:
        raise ValueError("根 svg 必须使用固定 16×16 属性，不能添加样式或其他属性")
    if root.text and root.text.strip():
        raise ValueError("不支持根节点文本")
    result = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pixel = result.load()
    count = 0
    for node in root:
        if node.tail and node.tail.strip():
            raise ValueError("不支持元素尾随文本")
        if node.tag is ET.Comment:
            continue
        if node.tag != NS + "rect" or set(node.attrib) != {"x", "y", "width", "height", "fill"}:
            raise ValueError("只支持直属 rect 及 x/y/width/height/fill 五个属性")
        if len(node) or (node.text and node.text.strip()):
            raise ValueError("rect 不能包含子元素或文本")
        values = [node.get(k, "") for k in ("x", "y", "width", "height")]
        if any(not INTEGER.fullmatch(v) for v in values):
            raise ValueError("矩形坐标和尺寸必须是非负十进制整数")
        x, y, width, height = map(int, values)
        if width < 1 or height < 1 or x + width > 16 or y + height > 16:
            raise ValueError("矩形必须非空且完全位于 16×16 画布")
        fill = node.get("fill", "")
        if not HEX.fullmatch(fill) or fill.upper() not in allowed_colors:
            raise ValueError("矩形填色必须是该资源色板内的 #RRGGBB")
        rgba = tuple(int(fill[i:i + 2], 16) for i in (1, 3, 5)) + (255,)
        for py in range(y, y + height):
            for px in range(x, x + width):
                pixel[px, py] = rgba
        count += 1
    if count == 0:
        raise ValueError("源稿不能没有矩形")
    return result


def validate_texture(name: str, image: Image.Image) -> None:
    """区分物品与方块的透明合同；物品边界须留空，矿石每个像素须不透明。"""
    alpha = image.getchannel("A")
    values = set(alpha.tobytes())
    if not values <= {0, 255}:
        raise ValueError(f"{name}: alpha 只能为 0/255")
    if name.startswith("block/"):
        if values != {255}:
            raise ValueError(f"{name}: 矿石必须完全不透明")
    else:
        border = [(x, y) for x in range(16) for y in range(16) if x in (0, 15) or y in (0, 15)]
        if values != {0, 255} or any(alpha.getpixel(p) for p in border):
            raise ValueError(f"{name}: 物品四周必须透明，内部必须有可见像素")


def place(canvas: Image.Image, source: Image.Image, xy: tuple[int, int], size: int, background: str) -> None:
    """预览仅用最近邻整数倍缩放；背景只用于对照，不写入贴图。"""
    tile = Image.new("RGBA", (size, size), background if background != "checker" else "#C3C7CC")
    if background == "checker":
        draw = ImageDraw.Draw(tile)
        for y in range(0, size, 10):
            for x in range(0, size, 10):
                if (x // 10 + y // 10) % 2:
                    draw.rectangle((x, y, x + 9, y + 9), fill="#E1E3E5")
    tile.alpha_composite(source.resize((size, size), Image.Resampling.NEAREST))
    canvas.alpha_composite(tile, xy)


def make_preview(images: dict[str, Image.Image], palette: dict) -> Image.Image:
    """生成可审阅的固定布局；使用 Pillow 内置字体，不读取系统字体文件。"""
    canvas = Image.new("RGBA", (1120, 870), "#151C24")
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.load_default(size=17)
    small = ImageFont.load_default(size=13)
    title = ImageFont.load_default(size=26)
    draw.text((32, 22), "MATERIAL STUDIES / 01", fill="#E6ECEC", font=title)
    draw.text((32, 63), "EXT-ART-01   /   ORIGINAL 16 x 16 SVG   /   TOP-LEFT LIGHT   /   CANDIDATE ONLY", fill="#95A9B7", font=small)
    for i, (name, label, _) in enumerate(ASSETS):
        x = 32 + i * 272
        draw.rounded_rectangle((x, 100, x + 240, 493), radius=8, fill="#212C36")
        draw.text((x + 16, 116), label, font=font, fill="#EEF1EA")
        place(canvas, images[name], (x + 16, 152), 16, "#A3A7AC")
        draw.text((x + 43, 152), "1x / 16 px", font=small, fill="#A8B9C5")
        place(canvas, images[name], (x + 40, 184), 160, "checker")
        draw.text((x + 16, 355), "10x / nearest neighbour", font=small, fill="#A8B9C5")
        place(canvas, images[name], (x + 16, 385), 80, "#EBE5DA")
        place(canvas, images[name], (x + 128, 385), 80, "#111720")
        draw.text((x + 16, 472), "LIGHT / 5x", font=small, fill="#A8B9C5")
        draw.text((x + 128, 472), "DARK / 5x", font=small, fill="#A8B9C5")
    draw.text((32, 517), "LEAD ORE / 3 x 3 TILE / 6x", fill="#EEF1EA", font=font)
    ore = images["block/lead_ore"].resize((96, 96), Image.Resampling.NEAREST)
    for y in range(3):
        for x in range(3):
            canvas.alpha_composite(ore, (32 + x * 96, 553 + y * 96))
    draw.text((360, 517), "PALETTES / SOLID COLOUR ONLY", fill="#EEF1EA", font=font)
    for i, (name, label, _) in enumerate(ASSETS):
        y = 559 + i * 68
        draw.text((360, y), label, font=small, fill="#A8B9C5")
        for j, color in enumerate(palette[name.split("/")[1]].values()):
            draw.rectangle((502 + j * 35, y - 3, 531 + j * 35, y + 24), fill=color)
    draw.text((820, 559), "SVG -> PNG", fill="#D4DED8", font=font)
    draw.text((820, 592), "Editable pixel rectangles", fill="#A8B9C5", font=small)
    draw.text((820, 617), "No gradients or bitmap", fill="#A8B9C5", font=small)
    draw.text((820, 642), "No game assets replaced", fill="#A8B9C5", font=small)
    draw.text((820, 667), "Style review pending", fill="#A8B9C5", font=small)
    return canvas


def make_html() -> str:
    """提供无需脚本、网络或外部字体的浏览器预览；1x 固定为 16 CSS 像素。"""
    cards = []
    for name, _, label in ASSETS:
        src = f"generated/{name}.png"
        cards.append(f'<article><h2>{html.escape(label)}</h2><code>{name}</code><p>原尺寸 · 16×16</p><div class="native"><img src="{src}" width="16" height="16" alt="{label}原尺寸"></div><p>10 倍 · 最近邻</p><div class="check"><img src="{src}" width="160" height="160" alt="{label}放大"></div><div class="compare"><figure class="light"><img src="{src}" width="80" height="80" alt="浅底"><figcaption>浅底 · 5 倍</figcaption></figure><figure class="dark"><img src="{src}" width="80" height="80" alt="深底"><figcaption>深底 · 5 倍</figcaption></figure></div></article>')
    tiles = '<img src="generated/block/lead_ore.png" alt="铅矿石平铺单元">' * 9
    return '''<!doctype html>
<html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>EXT-ART-01 材料样稿</title>
<style>body{margin:32px;background:#151c24;color:#e6ecec;font-family:system-ui,sans-serif}h1{margin-bottom:8px}p,code{color:#a8b9c5}main{display:flex;gap:24px;flex-wrap:wrap;margin:28px 0}article{width:208px;background:#212c36;padding:20px;border-radius:8px}h2{font-size:20px;margin:0 0 8px}img{image-rendering:pixelated;display:block}.native{height:16px}.check{width:160px;height:160px;margin:auto;background:repeating-conic-gradient(#c3c7cc 0 25%,#e1e3e5 0 50%) 0/20px 20px}.compare{display:flex;justify-content:space-between;margin-top:24px}figure{margin:0;width:80px}figure img{margin-bottom:6px}.light img{background:#ebe5da}.dark img{background:#111720}figcaption{color:#a8b9c5;font-size:13px}.tiles{display:grid;grid-template-columns:repeat(3,96px);width:288px;gap:0}.tiles img{width:96px;height:96px}</style>
<h1>材料样稿 / 01</h1><p>EXT-ART-01 · 原创 16×16 SVG · 统一左上光照 · 候选，待风格确认</p><p>浏览器缩放为 100% 时，“原尺寸”是 16 CSS 像素；截图、系统缩放与屏幕像素可能不同。</p><main>''' + "".join(cards) + '</main><h2>铅矿石 · 3×3 平铺 · 6 倍</h2><div class="tiles">' + tiles + '</div><p>未接入游戏；自动导出和本预览不能代替客户端验收。色板见 palette.json。</p></html>\n'


def main() -> None:
    """先验证全部源稿，再覆盖固定候选输出；不接受可扩散写集的命令行参数。"""
    import sys
    if len(sys.argv) != 1:
        raise ValueError("本导出器不接受参数")
    palette = read_palette()
    images = {}
    for name, _, _ in ASSETS:
        colors = {v.upper() for v in palette[name.split("/")[1]].values()}
        image = render_svg((ROOT / "sources" / (name + ".svg")).read_text(encoding="utf-8"), colors)
        validate_texture(name, image)
        images[name] = image
    preview = make_preview(images, palette)
    preview_html = make_html()
    for name, image in images.items():
        path = ROOT / "generated" / (name + ".png")
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path, format="PNG", optimize=False, compress_level=9)
        print(f"{path.relative_to(ROOT).as_posix()}  sha256={hashlib.sha256(path.read_bytes()).hexdigest()}")
    preview.save(ROOT / "preview.png", format="PNG", optimize=False, compress_level=9)
    (ROOT / "preview.html").write_text(preview_html, encoding="utf-8", newline="\n")
    print("Exported 4 RGBA textures, preview.png and preview.html")


if __name__ == "__main__":
    main()
