"""锅炉A横箍压力舱的SVG源稿与PNG候选导出工具。"""
from __future__ import annotations

import json
import hashlib
from pathlib import Path
import xml.etree.ElementTree as ET

from PIL import Image

ROOT = Path(__file__).resolve().parent
WORKSPACE = ROOT.parents[2]
CONCEPT = WORKSPACE / "tools/art-assets/boiler-concepts-05/01-horizontal-hoop"
REFERENCE = WORKSPACE / "tools/art-assets/reactor-ct-r1/mapping.json"
BASE = {
    "casing_side": "casing_side.svg", "casing_top": "casing_top.svg",
    "window_frame": None, "water_port_face": "water_port.svg",
    "steam_port_face": "steam_port.svg", "controller_face": "controller_face.svg",
    "exchanger_face": None, "exchanger_top": None,
    "valve_body": None, "safety_badge": None,
    "high_pressure_boiler_hot_coolant_port": "hot_port.svg",
    "high_pressure_boiler_cold_coolant_port": "cold_port.svg",
}
RECT_TARGETS = ("side", "top", "partition_side", "water_port_face", "steam_port_face",
                "controller_face", "hot_coolant_port", "cold_coolant_port")
TARGETS = {name: f"create_nuclear_industry:block/boiler_ct/{name}" for name in (*RECT_TARGETS, "window_frame")}
P = {"deep": "#252B30", "steel": "#3D4549", "mid": "#657176", "light": "#A6B0AE",
     "pale": "#D6D6C8", "brass_dark": "#7B5A3C", "brass": "#B98A62",
     "signal": "#D7AE55", "glass": "#5B8FA7", "hot": "#B9573D"}
PALETTE_HEX = set(P.values())
# R1真实索引位序：上、下、左、右、左上、右上、左下、右下。
NATIVE_BITS = ((1, 0, -1), (2, 0, 1), (4, -1, 0), (8, 1, 0),
               (16, -1, -1), (32, 1, -1), (64, -1, 1), (128, 1, 1))


def new_pixels(fill=None):
    """创建16×16 RGBA画布；空白像素保持真正透明。"""
    return [[fill for _ in range(16)] for _ in range(16)]


def rect(pixels, x, y, width, height, color, alpha=255):
    if not (0 <= x < 16 and 0 <= y < 16 and width > 0 and height > 0 and x + width <= 16 and y + height <= 16):
        raise ValueError(f"矩形越出16×16画布：{x},{y},{width},{height}")
    rgba = (*bytes.fromhex(color[1:]), alpha)
    for py in range(y, y + height):
        for px in range(x, x + width):
            pixels[py][px] = rgba


def svg_text(pixels):
    """按逐行相同像素合并为整数rect，保留透明区域和有限调色板。"""
    rows = ['<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges">']
    for y, row in enumerate(pixels):
        x = 0
        while x < 16:
            color = row[x]
            if color is None:
                x += 1
                continue
            end = x + 1
            while end < 16 and row[end] == color:
                end += 1
            r, g, b, alpha = color
            fill = f"#{r:02X}{g:02X}{b:02X}"
            opacity = "" if alpha == 255 else f' opacity="{alpha / 255:.2f}"'
            rows.append(f'<rect x="{x}" y="{y}" width="{end-x}" height="1" fill="{fill}"{opacity}/>')
            x = end
    rows.append("</svg>")
    return "\n".join(rows) + "\n"


def parse_svg(text):
    """严格读取有限色板的16×16整数rect SVG；只支持明确透明度。"""
    if "<!DOCTYPE" in text.upper() or "<!ENTITY" in text.upper() or "<?" in text:
        raise ValueError("不支持DTD、实体或处理指令")
    root = ET.fromstring(text)
    required = {"width": "16", "height": "16", "viewBox": "0 0 16 16", "shape-rendering": "crispEdges"}
    if root.tag != "{http://www.w3.org/2000/svg}svg" or root.attrib != required:
        raise ValueError("根svg必须是16×16 crispEdges画布")
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = image.load()
    count = 0
    for node in root:
        if node.tag != "{http://www.w3.org/2000/svg}rect" or set(node.attrib) - {"x", "y", "width", "height", "fill", "opacity"}:
            raise ValueError("只接受x/y/width/height/fill和可选opacity的rect")
        nums = [node.get(key, "") for key in ("x", "y", "width", "height")]
        if any(not value.isdecimal() for value in nums):
            raise ValueError("坐标与尺寸必须为非负整数")
        x, y, w, h = map(int, nums)
        if w < 1 or h < 1 or x + w > 16 or y + h > 16:
            raise ValueError("矩形必须非空且位于画布内")
        color = node.get("fill", "")
        if len(color) != 7 or not color.startswith("#"):
            raise ValueError("填色必须为#RRGGBB")
        try:
            rgb = tuple(bytes.fromhex(color[1:]))
        except ValueError as exc:
            raise ValueError("填色不是有效十六进制颜色") from exc
        if color.upper() not in PALETTE_HEX:
            raise ValueError(f"颜色不在本批有限色板：{color}")
        opacity = node.get("opacity", "1")
        if opacity not in ("1", "0.50", "0.47", "0.28"):
            raise ValueError("opacity不在本批窗口有限透明度中")
        alpha = round(float(opacity) * 255)
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                px[xx, yy] = (*rgb, alpha)
        count += 1
    if count == 0:
        raise ValueError("SVG不能没有rect")
    return image


def rectangle_tile(kind, index, contexts, base_sources):
    """按锁定Create RECTANGLE上下左右邻接状态绘制单个16×16图格。"""
    pixels = new_pixels((*bytes.fromhex(P["steel"][1:]), 255))
    context = contexts[index]
    up, down, left, right = (context["up"], context["down"], context["left"], context["right"])
    if kind == "top":
        rect(pixels, 0, 0, 16, 16, P["steel"])
        rect(pixels, 0, 3, 16, 1, P["mid"])
    elif kind == "partition_side":
        rect(pixels, 0, 4, 16, 1, P["deep"])
        rect(pixels, 0, 5, 16, 1, P["light"])
        rect(pixels, 0, 6, 16, 1, P["mid"])
        rect(pixels, 0, 7, 16, 1, P["light"])
        rect(pixels, 0, 8, 16, 1, P["pale"])
        rect(pixels, 0, 9, 16, 1, P["steel"])
        rect(pixels, 0, 10, 16, 1, P["deep"])
        rect(pixels, 0, 12, 16, 1, P["deep"])
        rect(pixels, 0, 13, 16, 1, P["light"])
    elif kind == "side":
        rect(pixels, 0, 3, 16, 1, P["deep"])
        rect(pixels, 0, 4, 16, 1, P["light"])
        rect(pixels, 0, 11, 16, 1, P["deep"])
        rect(pixels, 0, 12, 16, 1, P["light"])
        rect(pixels, 2, 7, 12, 1, P["mid"])
    elif kind in ("water_port_face", "steam_port_face", "hot_coolant_port", "cold_coolant_port", "controller_face"):
        # CT只替换外周收边，中心功能图形逐像素沿用对应基础SVG。
        source_image = parse_svg(base_sources[kind].read_text(encoding="utf-8"))
        base_pixels = source_image.load()
        pixels = [[base_pixels[x, y] for x in range(16)] for y in range(16)]
        steel = (*bytes.fromhex(P["steel"][1:]), 255)
        deep = (*bytes.fromhex(P["deep"][1:]), 255)
        light = (*bytes.fromhex(P["light"][1:]), 255)
        for y in range(16):
            for x in range(16):
                if x <= 2 or x >= 13 or y <= 2 or y >= 13:
                    pixels[y][x] = steel
        for y, color in ((3, deep), (4, light), (11, deep), (12, light)):
            for x in range(16):
                if x <= 2 or x >= 13:
                    pixels[y][x] = color
        for x, color in ((3, deep), (4, light), (11, deep), (12, light)):
            for y in range(16):
                if y <= 2 or y >= 13:
                    pixels[y][x] = color
    else:
        raise ValueError(kind)
    if not up:
        rect(pixels, 0, 0, 16, 1, P["deep"])
        rect(pixels, 0, 1, 16, 1, P["mid"])
    if not down:
        rect(pixels, 0, 15, 16, 1, P["deep"])
        rect(pixels, 0, 14, 16, 1, P["mid"])
    if not left:
        rect(pixels, 0, 0, 1, 16, P["deep"])
        rect(pixels, 1, 0, 1, 16, P["mid"])
    if not right:
        rect(pixels, 15, 0, 1, 16, P["deep"])
        rect(pixels, 14, 0, 1, 16, P["mid"])
    return pixels


def window_tile(mask):
    """按实际八向邻接绘制透光玻璃；相接边去框，缺角补法兰角。"""
    p = new_pixels()
    # 玻璃主体保持透明，只留下窄反光线，避免连窗遮挡内部视图。
    rect(p, 4, 4, 1, 8, P["pale"], 0x80)
    rect(p, 5, 4, 2, 2, P["glass"], 0x47)
    if not mask & 1:
        rect(p, 0, 0, 16, 2, P["deep"])
        rect(p, 0, 2, 16, 1, P["light"])
    if not mask & 8:
        rect(p, 14, 0, 2, 16, P["deep"])
        rect(p, 13, 0, 1, 16, P["light"])
    if not mask & 2:
        rect(p, 0, 14, 16, 2, P["deep"])
        rect(p, 0, 13, 16, 1, P["light"])
    if not mask & 4:
        rect(p, 0, 0, 2, 16, P["deep"])
        rect(p, 2, 0, 1, 16, P["light"])
    corners = ((4, 1, 16, 0, 0), (1, 8, 32, 14, 0),
               (8, 2, 128, 14, 14), (2, 4, 64, 0, 14))
    for a, b, diagonal, x, y in corners:
        if not (mask & a and mask & b and mask & diagonal):
            rect(p, x, y, 2, 2, P["deep"])
            rect(p, x + (1 if x == 0 else 0), y + (1 if y == 0 else 0), 1, 1, P["light"])
    return p


def special_base(name):
    """为既有换热器表面与阀牌绘制同色板的独立候选面。"""
    p = new_pixels((*bytes.fromhex(P["steel"][1:]), 255))
    if name == "exchanger_face":
        rect(p, 0, 0, 16, 1, P["deep"])
        rect(p, 0, 15, 16, 1, P["deep"])
        rect(p, 2, 2, 12, 12, P["deep"])
        rect(p, 3, 3, 10, 10, P["mid"])
        for x in (5, 9):
            rect(p, x, 4, 2, 8, P["brass_dark"])
            rect(p, x, 4, 1, 8, P["brass"])
        rect(p, 6, 6, 4, 1, P["signal"])
        rect(p, 6, 9, 4, 1, P["signal"])
        rect(p, 6, 7, 4, 2, P["steel"])
    elif name == "exchanger_top":
        rect(p, 0, 0, 16, 2, P["deep"])
        rect(p, 0, 2, 16, 1, P["light"])
        rect(p, 0, 14, 16, 2, P["deep"])
        rect(p, 0, 13, 16, 1, P["mid"])
        rect(p, 2, 4, 12, 8, P["deep"])
        for x in (4, 6, 8, 10, 12):
            rect(p, x, 5, 1, 6, P["steel"])
        rect(p, 3, 5, 1, 6, P["brass_dark"])
        rect(p, 3, 5, 1, 1, P["brass"])
    elif name == "safety_badge":
        rect(p, 5, 5, 6, 6, P["deep"])
        rect(p, 6, 6, 4, 4, P["signal"])
        rect(p, 7, 6, 2, 2, P["deep"])
        rect(p, 7, 9, 2, 1, P["deep"])
        rect(p, 6, 6, 4, 1, P["pale"])
    elif name == "valve_body":
        rect(p, 0, 0, 16, 16, P["steel"])
        rect(p, 1, 1, 14, 14, P["mid"])
        rect(p, 2, 2, 12, 12, P["steel"])
        rect(p, 2, 2, 1, 12, P["light"])
        rect(p, 13, 2, 1, 12, P["deep"])
        rect(p, 4, 5, 8, 1, P["mid"])
        rect(p, 4, 10, 8, 1, P["deep"])
        for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
            rect(p, x, y, 1, 1, P["brass"])
    else:
        raise ValueError(name)
    return p


def native_mapping():
    source = json.loads(REFERENCE.read_text(encoding="utf-8"))
    base_refs = {name: {
        "model_sprite": ("create_nuclear_industry:block/high_pressure_boiler/" + name
                         if not name.startswith("high_pressure_boiler_")
                         else "create_nuclear_industry:block/" + name),
        "source_svg": (f"sources/base/{name}.svg"),
    } for name in BASE}
    base_refs["high_pressure_boiler_hot_coolant_port"]["model_sprite"] = "create_nuclear_industry:block/high_pressure_boiler_hot_coolant_port"
    base_refs["high_pressure_boiler_cold_coolant_port"]["model_sprite"] = "create_nuclear_industry:block/high_pressure_boiler_cold_coolant_port"
    return {
        "source": "tools/art-assets/reactor-ct-r1/mapping.json（只读，Create 6.0.10锁定映射）",
        "source_sha256": hashlib.sha256(REFERENCE.read_bytes()).hexdigest(),
        "base_assets": base_refs,
        "connected_assets": {name: {"target_sprite": target, "ct_type": "OMNIDIRECTIONAL" if name == "window_frame" else "RECTANGLE",
                                    "atlas_png": f"generated/ct_{name}.png"}
                             for name, target in TARGETS.items()},
        "rectangle_sheet": {"type": "RECTANGLE", "tile": 16, "sheet": 4, "contexts": source["rectangle_contexts"]},
        "window_sheet": {"type": "OMNIDIRECTIONAL", "tile": 16, "sheet": 8,
                         "contexts": source["window_contexts"],
                         "fallback_indices": source["window_fallback_indices"], "fallback_source_index": 0},
    }


def png_from_svg(source):
    return parse_svg(source.read_text(encoding="utf-8"))


def save_atlas(folder, indices, side, output):
    sheet = Image.new("RGBA", (side * 16, side * 16), (0, 0, 0, 0))
    for idx in indices:
        sheet.paste(png_from_svg(folder / f"{idx:02}.svg"), ((idx % side) * 16, (idx // side) * 16))
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output, format="PNG", optimize=False, compress_level=9)
    return sheet


def composite_window(mapping, folder, cells):
    mask_to_index = {item["mask"]: item["index"] for item in mapping["window_sheet"]["contexts"]}
    width, height = max(x for x, _ in cells) + 1, max(y for _, y in cells) + 1
    image = Image.new("RGBA", (width * 16, height * 16), (0, 0, 0, 0))
    for x, y in cells:
        up = (x, y - 1) in cells
        down = (x, y + 1) in cells
        left = (x - 1, y) in cells
        right = (x + 1, y) in cells
        diagonals = (
            up and left and (x - 1, y - 1) in cells,
            up and right and (x + 1, y - 1) in cells,
            down and left and (x - 1, y + 1) in cells,
            down and right and (x + 1, y + 1) in cells,
        )
        bits = (up, down, left, right, *diagonals)
        mask = sum(1 << bit for bit, connected in enumerate(bits) if connected)
        index = mask_to_index.get(mask, 0)
        image.paste(png_from_svg(folder / f"{index:02}.svg"), (x * 16, y * 16))
    return image


def preview_html():
    return """<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>高压锅炉素材候选</title><style>body{margin:0;background:#dddcd6;color:#263238;font:16px Segoe UI,Arial,sans-serif}header{padding:20px 28px;background:#293338;color:#f5f2e8}h1{margin:0 0 6px}main{max-width:1450px;margin:20px auto;padding:18px;background:#f8f7f2}section{margin:22px 0;padding-bottom:22px;border-bottom:1px solid #c3c4be}.sprites{display:flex;flex-wrap:wrap;gap:24px}.tile{display:grid;gap:8px;justify-items:center}.large{width:128px;height:128px;image-rendering:pixelated;background-color:#c4c8c6;background-image:linear-gradient(45deg,#aeb4b1 25%,transparent 25%),linear-gradient(-45deg,#aeb4b1 25%,transparent 25%),linear-gradient(45deg,transparent 75%,#aeb4b1 75%),linear-gradient(-45deg,transparent 75%,#aeb4b1 75%);background-size:16px 16px;background-position:0 0,0 8px,8px -8px,-8px 0}.atlas{width:256px;height:256px;image-rendering:pixelated;background:#c4c8c6}.scene{width:192px;height:auto;image-rendering:pixelated;background-color:#c4c8c6;background-image:linear-gradient(45deg,#aeb4b1 25%,transparent 25%),linear-gradient(-45deg,#aeb4b1 25%,transparent 25%),linear-gradient(45deg,transparent 75%,#aeb4b1 75%),linear-gradient(-45deg,transparent 75%,#aeb4b1 75%);background-size:16px 16px;background-position:0 0,0 8px,8px -8px,-8px 0}.label{font-weight:600}small{color:#667176}img{object-fit:contain}</style><header><h1>高压锅炉 · A横箍压力舱素材候选</h1><div>SVG原创源稿与连接图集预览；尚未接入游戏</div></header><main><section><h2>基础纹理</h2><div class="sprites" id="base"></div></section><section><h2>连接图集</h2><div class="sprites" id="atlases"></div></section><section><h2>连窗边界形状</h2><div class="sprites" id="windows"></div><p><small>棋盘底色用于观察透明玻璃；示例使用原生八向邻接映射。</small></p></section><script>const base=['casing_side','casing_top','window_frame','water_port_face','steam_port_face','controller_face','exchanger_face','exchanger_top','valve_body','safety_badge','high_pressure_boiler_hot_coolant_port','high_pressure_boiler_cold_coolant_port'];const targets=['side','top','partition_side','water_port_face','steam_port_face','controller_face','hot_coolant_port','cold_coolant_port','window_frame'];function add(host,name,src,cls){let d=document.createElement('div');d.className='tile';let im=document.createElement('img');im.className=cls;im.src=src;d.appendChild(im);let label=document.createElement('span');label.className='label';label.textContent=name;d.appendChild(label);host.appendChild(d)}base.forEach(n=>add(document.getElementById('base'),n,'../generated/base_'+n+'.png','large'));targets.forEach(n=>add(document.getElementById('atlases'),n,'../generated/ct_'+n+'.png','atlas'));[['单窗','window_single.png'],['2×2连窗','window_2x2.png'],['L形缺角','window_L.png']].forEach(v=>add(document.getElementById('windows'),v[0],v[1],'scene'));</script></main></html>"""


def build():
    source_base = ROOT / "sources/base"
    source_base.mkdir(parents=True, exist_ok=True)
    for name, filename in BASE.items():
        if filename is None and name == "window_frame":
            text = svg_text(window_tile(0))
        elif filename is None:
            text = svg_text(special_base(name))
        else:
            text = (CONCEPT / filename).read_text(encoding="utf-8")
        parse_svg(text)
        (source_base / f"{name}.svg").write_text(text, encoding="utf-8", newline="\n")
    mapping = native_mapping()
    (ROOT / "mapping.json").write_text(json.dumps(mapping, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (ROOT / "palette.json").write_text(json.dumps(P, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    manifest = {
        "task": "ART-BOILER-05B",
        "design": "A横箍压力舱",
        "tile_size": 16,
        "base_asset_count": len(BASE),
        "connected_target_ids": list(TARGETS.values()),
        "source_mapping": "mapping.json",
        "palette": "palette.json",
        "source_policy": "基础造型沿用05A作者SVG，新增图格为本批整数rect SVG；只读R1真实Create索引表",
        "game_install": False,
    }
    (ROOT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    rect_dir = ROOT / "sources/ct"
    base_sources = {
        "water_port_face": source_base / "water_port_face.svg",
        "steam_port_face": source_base / "steam_port_face.svg",
        "controller_face": source_base / "controller_face.svg",
        "hot_coolant_port": source_base / "high_pressure_boiler_hot_coolant_port.svg",
        "cold_coolant_port": source_base / "high_pressure_boiler_cold_coolant_port.svg",
    }
    for target in RECT_TARGETS:
        folder = rect_dir / target
        folder.mkdir(parents=True, exist_ok=True)
        for index in range(16):
            (folder / f"{index:02}.svg").write_text(svg_text(rectangle_tile(target, index, mapping["rectangle_sheet"]["contexts"], base_sources)), encoding="utf-8", newline="\n")
    window_dir = rect_dir / "window_frame"
    window_dir.mkdir(parents=True, exist_ok=True)
    valid = {entry["index"]: entry["mask"] for entry in mapping["window_sheet"]["contexts"]}
    fallback = set(mapping["window_sheet"]["fallback_indices"])
    isolated = window_tile(0)
    for index in range(64):
        pixels = window_tile(valid[index]) if index in valid else isolated
        if index not in valid and index not in fallback:
            raise ValueError(f"原生窗口索引未登记：{index}")
        (window_dir / f"{index:02}.svg").write_text(svg_text(pixels), encoding="utf-8", newline="\n")
    generated = ROOT / "generated"
    generated.mkdir(parents=True, exist_ok=True)
    for name in BASE:
        png_from_svg(source_base / f"{name}.svg").save(generated / f"base_{name}.png", format="PNG", optimize=False, compress_level=9)
    for target in RECT_TARGETS:
        save_atlas(rect_dir / target, range(16), 4, generated / f"ct_{target}.png")
    save_atlas(window_dir, range(64), 8, generated / "ct_window_frame.png")
    preview = ROOT / "preview"
    preview.mkdir(parents=True, exist_ok=True)
    scenes = (("window_single", {(0, 0)}),
              ("window_2x2", {(0, 0), (1, 0), (0, 1), (1, 1)}),
              ("window_L", {(0, 0), (1, 0), (0, 1)}))
    for name, cells in scenes:
        composite_window(mapping, window_dir, cells).save(preview / f"{name}.png", format="PNG", optimize=False, compress_level=9)
    (preview / "index.html").write_text(preview_html(), encoding="utf-8", newline="\n")
    print(f"preview={preview / 'index.html'}")
    print(f"base_png={len(BASE)} rectangle_atlas={len(RECT_TARGETS)} window_indices=64")
    print(f"window_scenes=3; native_contexts={len(mapping['window_sheet']['contexts'])}; fallbacks={len(fallback)}")


if __name__ == "__main__":
    build()
