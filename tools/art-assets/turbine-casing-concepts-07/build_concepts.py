"""生成汽轮机机壳三稿的整数SVG源、展示板与轻量规格检查。"""
from __future__ import annotations

import argparse
import json
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent
PALETTE = {
    "D": "#28353A", "d": "#3E5058", "M": "#5B7078", "m": "#788E94",
    "L": "#A3B7B8", "H": "#D1DFDA", "c": "#715035", "C": "#A87942",
}
GEOMETRY = {"width": 5, "length": 8, "height": 5, "profile": "octagonal", "projection": "iso-5x8x5-v1"}
PROFILE = [(1, 0), (4, 0), (5, 1), (5, 4), (4, 5), (1, 5), (0, 4), (0, 1)]
SCHEMES = {
    "A": {"title": "分体钢铜", "short": "铜上盖 / 深钢下壳", "desc": "铜质上盖覆盖上部八棱面；深钢下壳由连续中腰检修分缝收口，并配置成组螺栓。", "claim": "上部铜盖与下部深钢沿中腰连续分体"},
    "B": {"title": "分段轮舱", "short": "钢制轮舱 / 窄铜环缝", "desc": "钢面为主，轴向每段用厚钢束带收口，并以窄铜缝标记轮舱分界。", "claim": "连续环带分段，铜只作窄识别线"},
    "C": {"title": "纵筋检修", "short": "纵向钢筋 / 凹入铜面", "desc": "深钢纵筋贯通机壳长轴，凹入铜面被亮钢边压住，间隔短横缝提示检修板。", "claim": "纵筋连续，凹入铜带构成长轴中心"},
}


def blank(fill: str = "M") -> list[list[str]]:
    return [[fill for _ in range(16)] for _ in range(16)]


def frame(g: list[list[str]]) -> None:
    """画共同的钢制外周，保证单块边缘明暗一致。"""
    for x in range(16):
        g[0][x], g[15][x] = "D", "D"
        g[1][x], g[14][x] = "L", "d"
    for y in range(16):
        g[y][0], g[y][15] = "D", "D"
        g[y][1], g[y][14] = "m", "m"


def set_rect(g: list[list[str]], x0: int, y0: int, x1: int, y1: int, color: str) -> None:
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            g[y][x] = color


def side_pattern(scheme: str) -> list[str]:
    g = blank()
    frame(g)
    if scheme == "A":
        set_rect(g, 2, 3, 13, 12, "M")
        set_rect(g, 3, 4, 12, 4, "L")
        set_rect(g, 3, 5, 12, 10, "M")
        set_rect(g, 3, 11, 12, 11, "d")
        for x in (3, 12):
            for y in (4, 7, 11):
                g[y][x] = "H"
        set_rect(g, 6, 6, 9, 6, "d")
        set_rect(g, 6, 8, 9, 8, "d")
    elif scheme == "B":
        for y in (2, 3, 12, 13):
            set_rect(g, 2, y, 13, y, "d" if y in (2, 13) else "L")
        for x in (2, 3, 12, 13):
            set_rect(g, x, 4, x, 11, "c" if x in (3, 12) else "C")
        set_rect(g, 4, 5, 11, 10, "M")
        for x in (5, 10):
            g[5][x] = g[10][x] = "H"
        set_rect(g, 6, 6, 9, 9, "m")
        for x in (0, 15):
            for y in range(4, 12):
                g[y][x] = "D"
    else:
        set_rect(g, 2, 2, 13, 3, "d")
        set_rect(g, 2, 4, 13, 4, "L")
        set_rect(g, 2, 11, 13, 11, "d")
        set_rect(g, 2, 12, 13, 13, "L")
        set_rect(g, 3, 6, 12, 9, "C")
        set_rect(g, 3, 6, 12, 6, "c")
        set_rect(g, 3, 9, 12, 9, "c")
        for x in (3, 12):
            g[5][x] = g[10][x] = "H"
        set_rect(g, 7, 7, 8, 8, "c")
    return ["".join(row) for row in g]


def top_pattern(scheme: str) -> list[str]:
    g = blank("d")
    frame(g)
    if scheme == "A":
        set_rect(g, 2, 2, 13, 13, "C")
        set_rect(g, 2, 2, 2, 13, "c")
        set_rect(g, 13, 2, 13, 13, "c")
        set_rect(g, 3, 7, 12, 7, "c")
        set_rect(g, 3, 8, 12, 8, "C")
        for x in (4, 11):
            g[3][x] = g[12][x] = "H"
    elif scheme == "B":
        set_rect(g, 2, 3, 13, 12, "M")
        for x in (3, 4, 11, 12):
            set_rect(g, x, 3, x, 12, "C" if x in (4, 11) else "c")
        for y in (4, 11):
            set_rect(g, 5, y, 10, y, "L")
    else:
        set_rect(g, 2, 3, 13, 12, "M")
        for y, color in ((4, "d"), (5, "L"), (10, "d"), (11, "L")):
            set_rect(g, 2, y, 13, y, color)
        set_rect(g, 3, 7, 12, 8, "C")
        set_rect(g, 3, 7, 12, 7, "c")
    return ["".join(row) for row in g]


def endcap_pattern(scheme: str) -> list[str]:
    g = blank(".")
    for y in range(16):
        for x in range(16):
            dx, dy = abs(x - 7.5), abs(y - 7.5)
            octo = dx <= 6 and dy <= 6 and dx + dy <= 10
            if not octo:
                continue
            radius = max(dx, dy) + (dx + dy) * 0.12
            g[y][x] = "D" if radius > 5.2 else "m" if radius > 4.2 else "M"
    if scheme == "A":
        for y in range(4, 12):
            for x in range(4, 12):
                if abs(x - 7.5) + abs(y - 7.5) <= 3:
                    g[y][x] = "C" if abs(x - 7.5) + abs(y - 7.5) > 1.2 else "D"
        g[4][7] = g[4][8] = g[11][7] = g[11][8] = "H"
    elif scheme == "B":
        for y in range(3, 13):
            for x in range(3, 13):
                if abs(x - 7.5) + abs(y - 7.5) in (4, 5):
                    g[y][x] = "C" if x in (4, 11) or y in (4, 11) else "c"
        set_rect(g, 6, 6, 9, 9, "D")
        set_rect(g, 7, 7, 8, 8, "L")
    else:
        set_rect(g, 7, 3, 8, 12, "d")
        set_rect(g, 3, 7, 12, 8, "d")
        set_rect(g, 6, 6, 9, 9, "C")
        set_rect(g, 7, 7, 8, 8, "D")
    return ["".join(row) for row in g]


def seam_pattern(scheme: str) -> list[str]:
    g = blank("M")
    for y in range(16):
        g[y][0] = g[y][15] = "D"
        g[y][1] = g[y][14] = "m"
    set_rect(g, 0, 2, 15, 3, "d")
    set_rect(g, 0, 4, 15, 11, "M")
    set_rect(g, 0, 12, 15, 13, "D")
    if scheme == "A":
        set_rect(g, 0, 6, 15, 7, "C")
        set_rect(g, 0, 8, 15, 8, "c")
    elif scheme == "B":
        set_rect(g, 0, 4, 15, 5, "L")
        set_rect(g, 0, 6, 15, 6, "C")
        set_rect(g, 0, 7, 15, 9, "d")
        set_rect(g, 0, 10, 15, 10, "c")
    else:
        set_rect(g, 0, 4, 15, 5, "L")
        set_rect(g, 0, 10, 15, 11, "L")
        set_rect(g, 4, 6, 11, 9, "C")
    return ["".join(row) for row in g]


def tile_svg(scheme: str, key: str, rows: list[str]) -> str:
    rects = []
    for y, row in enumerate(rows):
        x = 0
        while x < 16:
            color = row[x]
            end = x + 1
            while end < 16 and row[end] == color:
                end += 1
            if color != ".":
                rects.append(f'<rect x="{x}" y="{y}" width="{end-x}" height="1" fill="{PALETTE[color]}"/>')
            x = end
    title = SCHEMES[scheme]["title"] + " · " + {"side": "侧面", "top": "顶面", "endcap": "端盖", "seam": "接缝"}[key]
    return ('<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" '
            'shape-rendering="crispEdges" data-art="SVG设计稿，未接入游戏">\n'
            f'<title>{title}</title><defs><symbol id="tile" viewBox="0 0 16 16">'
            + "".join(rects) + '</symbol></defs><use href="#tile" width="16" height="16"/>\n</svg>\n')


def pstr(points: list[tuple[float, float]]) -> str:
    return " ".join(f"{x:.2f},{y:.2f}" for x, y in points)


def project(x: float, y: float, z: float) -> tuple[float, float]:
    """将5×5八棱截面与8格轴向映射为展示用屏幕坐标。"""
    # 截面x保持屏幕横向，截面y形成真实竖向厚度，轴向z横卧并略向上延伸。
    return (18 * (x - 2.5) + 24 * z,
            -24 * (y - 2.5) - 7 * z)


def use_matrix(z0: float, a: tuple[int, int], b: tuple[int, int]) -> str:
    start = project(a[0], a[1], z0)
    edge_end = project(b[0], b[1], z0)
    axis = (24 * 2 / 16, -7 * 2 / 16)
    edge = ((edge_end[0] - start[0]) / 16, (edge_end[1] - start[1]) / 16)
    return f"matrix({axis[0]:.4f} {axis[1]:.4f} {edge[0]:.4f} {edge[1]:.4f} {start[0]:.2f} {start[1]:.2f})"


def symbols(scheme: str, rows_by_key: dict[str, list[str]]) -> str:
    out = []
    for key, rows in rows_by_key.items():
        rects = []
        for y, row in enumerate(rows):
            for x, color in enumerate(row):
                if color != ".":
                    rects.append(f'<rect x="{x}" y="{y}" width="1" height="1" fill="{PALETTE[color]}"/>')
        out.append(f'<symbol id="{scheme}-{key}" viewBox="0 0 16 16" shape-rendering="crispEdges">{"".join(rects)}</symbol>')
    return "<defs>" + "".join(out) + "</defs>"


def shell_svg(scheme: str, rows_by_key: dict[str, list[str]], cx: float, cy: float, scale: float, prefix: str) -> str:
    """绘制相同八棱机壳；面片按两格轴向显式映射一张16像素贴图。"""
    out = [f'<g transform="translate({cx-96*scale:.2f} {cy+28*scale:.2f}) scale({scale:.4f})">']
    parts = []
    for index, a in enumerate(PROFILE):
        b = PROFILE[(index + 1) % len(PROFILE)]
        face_sections = [(a, b, "top" if (a[1] + b[1]) / 2 >= 3.5 else "side")]
        # A方案把两片竖直侧壁沿中腰切成铜上盖和深钢下壳。
        if scheme == "A" and a[0] == b[0] and {a[1], b[1]} == {1, 4}:
            mid = (a[0], 2.5)
            if a[1] < b[1]:
                face_sections = [(a, mid, "side"), (mid, b, "top")]
            else:
                face_sections = [(mid, b, "side"), (a, mid, "top")]
        for part_index, (pa, pb, layer) in enumerate(face_sections):
            pts = [project(*pa, 0), project(*pb, 0), project(*pb, 8), project(*pa, 8)]
            avg_y = sum(point[1] for point in pts) / 4
            parts.append((avg_y, index, part_index, pa, pb, pts, layer))
    cap_pts = [project(x, y, 0) for x, y in PROFILE]
    cap_clip = f"{prefix}-cap"
    clip_defs = []
    for _avg, index, part_index, _a, _b, pts, _layer in parts:
        clip_id = f"{prefix}-face-{index}-{part_index}"
        clip_defs.append(f'<clipPath id="{clip_id}"><polygon points="{pstr(pts)}"/></clipPath>')
    clip_defs.append(f'<clipPath id="{cap_clip}"><polygon points="{pstr(cap_pts)}"/></clipPath>')
    out.append("<defs>" + "".join(clip_defs) + "</defs>")
    for _avg, index, part_index, a, b, pts, layer in sorted(parts):
        clip_id = f"{prefix}-face-{index}-{part_index}"
        tile = f"{scheme}-{layer}"
        out.append(f'<g data-shell-layer="{layer}" clip-path="url(#{clip_id})">')
        out.append(f'<polygon points="{pstr(pts)}" fill="{PALETTE["D"]}"/>')
        for z0 in (0, 2, 4, 6):
            out.append(f'<use href="#{tile}" width="16" height="16" transform="{use_matrix(z0,a,b)}"/>')
        if layer != "top":
            out.append(f'<polygon points="{pstr(pts)}" fill="#101820" opacity="0.12"/>')
        out.append("</g>")
    if scheme == "A":
        # 中腰分缝沿两侧长壁连续延伸，并用成组螺栓压紧接合边。
        for x in (0, 5):
            p0, p1 = project(x, 2.5, 0), project(x, 2.5, 8)
            line = f"M {p0[0]:.2f},{p0[1]:.2f} L {p1[0]:.2f},{p1[1]:.2f}"
            out.append(f'<path d="{line}" fill="none" stroke="#28353A" stroke-width="3.2"/>')
            out.append(f'<path d="{line}" fill="none" stroke="#715035" stroke-width="1.1"/>')
            for z in (0.8, 2.8, 4.8, 6.8):
                bolt = project(x, 2.5, z)
                out.append(f'<circle cx="{bolt[0]:.2f}" cy="{bolt[1]:.2f}" r="2.1" fill="#A3B7B8" stroke="#28353A" stroke-width="0.8"/>')
                out.append(f'<circle cx="{bolt[0]:.2f}" cy="{bolt[1]:.2f}" r="0.7" fill="#28353A"/>')
    # 三道重复压边让轴向拼段清楚，窄铜芯仅用于B的轮舱识别。
    for z in (2, 4, 6):
        for index, a in enumerate(PROFILE):
            b = PROFILE[(index + 1) % len(PROFILE)]
            band = [project(*a, z - (0.08 if scheme != "B" else 0.14)),
                    project(*b, z - (0.08 if scheme != "B" else 0.14)),
                    project(*b, z + (0.08 if scheme != "B" else 0.14)),
                    project(*a, z + (0.08 if scheme != "B" else 0.14))]
            color = "#28353A" if scheme != "B" else "#3E5058"
            out.append(f'<polygon points="{pstr(band)}" fill="{color}"/>')
            if scheme == "B":
                thin = [project(*a, z - 0.025), project(*b, z - 0.025), project(*b, z + 0.025), project(*a, z + 0.025)]
                out.append(f'<polygon points="{pstr(thin)}" fill="#A87942"/>')
    cap_pts = [project(x, y, 0) for x, y in PROFILE]
    min_x, max_x = min(x for x, _ in cap_pts), max(x for x, _ in cap_pts)
    min_y, max_y = min(y for _, y in cap_pts), max(y for _, y in cap_pts)
    out.append(f'<g clip-path="url(#{cap_clip})"><polygon points="{pstr(cap_pts)}" fill="#28353A"/>')
    out.append(f'<use href="#{scheme}-endcap" width="16" height="16" transform="matrix({(max_x-min_x)/16:.4f} 0 0 {(max_y-min_y)/16:.4f} {min_x:.2f} {min_y:.2f})"/>')
    out.append("</g>")
    out.append(f'<polygon points="{pstr(cap_pts)}" fill="none" stroke="#D1DFDA" stroke-width="1.4"/>')
    out.append("</g>")
    return "".join(out)


def svg_text(title: str, width: int, height: int, body: str, defs: str = "") -> str:
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" '
            'preserveAspectRatio="xMidYMin meet" shape-rendering="crispEdges">'
            f'<title>{title}</title><rect width="100%" height="100%" fill="#17212A"/>{defs}{body}</svg>\n')


def tile_use(scheme: str, key: str, x: float, y: float, size: float) -> str:
    return f'<use href="#{scheme}-{key}" x="{x}" y="{y}" width="{size}" height="{size}"/>'


def sheet(scheme: str, rows_by_key: dict[str, list[str]]) -> str:
    info = SCHEMES[scheme]
    out = [f'<text x="42" y="52" class="title">方案{scheme} · {info["title"]}</text>',
           f'<text x="42" y="82" class="sub">{info["desc"]}</text>',
           '<text x="42" y="122" class="tag">同一5×8×5八棱机壳投影 · 只比较机壳贴图候选</text>',
           f'<g data-geometry="5x8x5-octagonal">{shell_svg(scheme, rows_by_key, 370, 330, 1.18, f"board-{scheme}")}</g>']
    out.append('<text x="820" y="142" class="heading">单格纹理</text>')
    labels = [("side", "侧面"), ("top", "顶面"), ("endcap", "端盖"), ("seam", "接缝")]
    for i, (key, label) in enumerate(labels):
        x = 820 + (i % 2) * 245
        y = 170 + (i // 2) * 205
        out.append(tile_use(scheme, key, x, y, 128))
        out.append(f'<text x="{x}" y="{y+151}" class="label">{label} · 16×16</text>')
    out.extend(['<text x="820" y="598" class="heading">材料色板</text>'])
    swatches = [("D", "钢影"), ("d", "钢暗面"), ("M", "深钢"), ("m", "钢灰"), ("L", "钢亮面"), ("H", "钢高光"), ("c", "铜影"), ("C", "铜面")]
    for i, (color, label) in enumerate(swatches):
        x = 820 + (i % 4) * 82
        y = 625 + (i // 4) * 54
        out.append(f'<rect x="{x}" y="{y}" width="28" height="28" fill="{PALETTE[color]}" stroke="#D1DFDA" stroke-width="1"/>')
        out.append(f'<text x="{x+34}" y="{y+19}" class="small">{label}</text>')
    out.extend(['<text x="42" y="565" class="heading">平铺与连接边界示意</text>',
                '<text x="42" y="590" class="small">左：2×2单块纹理直接平铺；右：遮去内部外框，接缝连续（设计示意，不代表已接入CT）</text>'])
    side = rows_by_key["side"]
    out.append('<text x="60" y="636" class="small">单块平铺</text>')
    out.append('<text x="360" y="636" class="small">相邻后</text>')
    size = 92
    for row in range(2):
        for col in range(2):
            out.append(tile_use(scheme, "side", 60 + col * size, 650 + row * size, size))
            connected = mask_edges(side, north=row == 0, east=col == 1, south=row == 1, west=col == 0)
            out.append(tile_svg_inline(scheme, connected, 360 + col * size, 650 + row * size, size))
    out.append('<text x="820" y="846" class="small">' + info["claim"] + '。展示保持相同钢/铜色板。</text>')
    return svg_text(f"汽轮机机壳方案{scheme}", 1320, 900, "<style>.title{font: bold 32px sans-serif;fill:#f2f5f2}.sub{font:18px sans-serif;fill:#c1d0d0}.tag,.label,.heading{font:bold 18px sans-serif;fill:#e8eee9}.small{font:14px sans-serif;fill:#bac8c8}</style>" + "".join(out), symbols(scheme, rows_by_key))


def mask_edges(rows: list[str], north: bool, east: bool, south: bool, west: bool) -> list[str]:
    """只在有同类相邻块时，把外周框颜色延续成内面色。"""
    g = [list(row) for row in rows]
    for x in range(16):
        if north:
            g[0][x] = g[2][x]
        if south:
            g[15][x] = g[13][x]
    for y in range(16):
        if west:
            g[y][0] = g[y][2]
        if east:
            g[y][15] = g[y][13]
    return ["".join(row) for row in g]


def tile_svg_inline(scheme: str, rows: list[str], x: float, y: float, size: float) -> str:
    scale = size / 16
    out = [f'<g transform="translate({x} {y}) scale({scale:.4f})">']
    for py, row in enumerate(rows):
        for px, color in enumerate(row):
            if color != ".":
                out.append(f'<rect x="{px}" y="{py}" width="1" height="1" fill="{PALETTE[color]}"/>')
    out.append("</g>")
    return "".join(out)


def overview(rows_by_scheme: dict[str, dict[str, list[str]]]) -> str:
    body = ['<text x="36" y="46" class="title">汽轮机机壳贴图三稿</text>',
            '<text x="36" y="74" class="sub">SVG设计稿，未接入游戏 · 共用5×8×5八棱外形，仅改变机壳结构纹理</text>']
    x_starts = [16, 438, 860]
    for scheme, x in zip(("A", "B", "C"), x_starts):
        info = SCHEMES[scheme]
        body.append(f'<g data-geometry="5x8x5-octagonal" data-scheme="{scheme}">')
        body.append(f'<text x="{x+20}" y="112" class="heading">{scheme} · {info["title"]}</text>')
        body.append(shell_svg(scheme, rows_by_scheme[scheme], x + 205, 225, 1.0, f"overview-{scheme}"))
        body.append(f'<text x="{x+20}" y="330" class="small">{info["short"]}</text>')
        body.append(tile_use(scheme, "side", x + 25, 358, 64))
        body.append(f'<text x="{x+103}" y="384" class="small">单格侧面</text>')
        # 同一侧面以2×2直接重复及去内框显示连接前后。
        size = 44
        for row in range(2):
            for col in range(2):
                body.append(tile_use(scheme, "side", x + 30 + col * size, 420 + row * size, size))
                connected = mask_edges(rows_by_scheme[scheme]["side"], row == 0, col == 1, row == 1, col == 0)
                body.append(tile_svg_inline(scheme, connected, x + 164 + col * size, 420 + row * size, size))
        body.append(f'<text x="{x+28}" y="528" class="small">单块平铺</text><text x="{x+164}" y="528" class="small">相邻去内框</text>')
        for i, (color, label) in enumerate((("D", "钢"), ("M", "深钢"), ("L", "亮钢"), ("c", "铜影"), ("C", "铜"))):
            sx = x + 22 + i * 68
            body.append(f'<rect x="{sx}" y="554" width="26" height="26" fill="{PALETTE[color]}" stroke="#dce7e3" stroke-width="1"/>')
            body.append(f'<text x="{sx+31}" y="573" class="tiny">{label}</text>')
        body.append(f'<text x="{x+20}" y="610" class="small">{info["claim"]}</text></g>')
    body.append('<text x="36" y="680" class="small">相邻示意只用于比较纹理边界处理；不表示已实现连接纹理。点击上方方案可打开单稿放大。</text>')
    return svg_text("汽轮机机壳贴图三稿总览", 1280, 720,
                    '<style>.title{font:bold 28px sans-serif;fill:#f2f5f2}.sub{font:15px sans-serif;fill:#bdcbcb}.heading{font:bold 18px sans-serif;fill:#e7eeea}.small{font:13px sans-serif;fill:#c0cecd}.tiny{font:11px sans-serif;fill:#c0cecd}</style>' + "".join(body),
                    "".join(symbols(s, rows) for s, rows in rows_by_scheme.items()))


def board_svg(scheme: str, rows: dict[str, list[str]]) -> str:
    return sheet(scheme, rows)


def build() -> dict[str, dict[str, list[str]]]:
    rows_by_scheme = {}
    for scheme in SCHEMES:
        rows = {"side": side_pattern(scheme), "top": top_pattern(scheme),
                "endcap": endcap_pattern(scheme), "seam": seam_pattern(scheme)}
        rows_by_scheme[scheme] = rows
        for key, pixels in rows.items():
            folder = ROOT / scheme
            folder.mkdir(parents=True, exist_ok=True)
            (folder / f"{key}.svg").write_text(tile_svg(scheme, key, pixels), encoding="utf-8")
        (ROOT / scheme / "board.svg").write_text(board_svg(scheme, rows), encoding="utf-8")
    (ROOT / "overview.svg").write_text(overview(rows_by_scheme), encoding="utf-8")
    (ROOT / "index.html").write_text(index_html(), encoding="utf-8")
    manifest = {
        "task": "ART-TURBINE-07A", "png_created": 0,
        "geometry": GEOMETRY,
        "schemes": {key: {**SCHEMES[key], "sources": [f"{key}/{name}.svg" for name in ("side", "top", "endcap", "seam")], "board": f"{key}/board.svg"} for key in SCHEMES},
        "overview": "overview.svg", "entry": "index.html",
        "reference_palette": {color: meaning for color, meaning in zip(PALETTE.values(), ("钢影", "钢暗面", "深钢", "钢灰", "钢亮面", "钢高光", "铜影", "铜面"))},
    }
    (ROOT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return rows_by_scheme


def index_html() -> str:
    links = " ".join(f'<a href="{scheme}/board.svg" target="_blank">方案{scheme}放大</a>' for scheme in SCHEMES)
    return f'''<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>汽轮机机壳贴图三稿</title>
<style>html,body{{margin:0;background:#101820;color:#e8eee9;font-family:system-ui,sans-serif}}header{{height:62px;display:flex;align-items:center;gap:22px;padding:0 20px;background:#202a30}}header strong{{font-size:18px}}a{{color:#d1dfda;text-decoration:none;border:1px solid #5b7078;padding:7px 11px;border-radius:5px}}main{{height:calc(100vh - 62px)}}object{{width:100%;height:100%;display:block}}</style></head>
<body><header><strong>汽轮机机壳贴图三稿</strong><span>SVG设计稿，未接入游戏</span>{links}</header>
<main><object data="overview.svg" type="image/svg+xml" aria-label="A、B、C三种机壳贴图设计稿总览"></object></main></body></html>\n'''


def read_source(path: Path) -> list[str]:
    root = ET.parse(path).getroot()
    if root.attrib.get("viewBox") != "0 0 16 16" or root.attrib.get("shape-rendering") != "crispEdges":
        raise ValueError(f"画布或像素边缘设置错误：{path.relative_to(ROOT)}")
    symbol = root.find("{http://www.w3.org/2000/svg}defs/{http://www.w3.org/2000/svg}symbol")
    if symbol is None or symbol.attrib.get("id") != "tile":
        raise ValueError(f"缺少tile源符号：{path.relative_to(ROOT)}")
    grid = [["." for _ in range(16)] for _ in range(16)]
    for node in symbol:
        if node.tag != "{http://www.w3.org/2000/svg}rect":
            raise ValueError(f"纹理仅允许整数像素rect：{path.relative_to(ROOT)}")
        values = [node.attrib.get(k, "") for k in ("x", "y", "width", "height")]
        if any(not value.isdigit() for value in values):
            raise ValueError(f"像素坐标必须为非负整数：{path.relative_to(ROOT)}")
        x, y, width, height = map(int, values)
        color = node.attrib.get("fill", "").upper()
        if color not in PALETTE.values() or width < 1 or height != 1 or x + width > 16 or y >= 16:
            raise ValueError(f"越界像素或未登记色板：{path.relative_to(ROOT)}")
        for px in range(x, x + width):
            if grid[y][px] != ".":
                raise ValueError(f"重复覆盖像素：{path.relative_to(ROOT)}")
            grid[y][px] = next(key for key, value in PALETTE.items() if value == color)
    return ["".join(row) for row in grid]


def check() -> None:
    expected = {f"{scheme}/{name}.svg" for scheme in SCHEMES for name in ("side", "top", "endcap", "seam")}
    actual = {p.relative_to(ROOT).as_posix() for p in ROOT.glob("[ABC]/*.svg") if p.name != "board.svg"}
    if expected != actual:
        raise ValueError(f"源稿集合不符：expected={len(expected)} actual={len(actual)}")
    rows: dict[str, dict[str, list[str]]] = {scheme: {} for scheme in SCHEMES}
    for rel in sorted(expected):
        parsed = read_source(ROOT / rel)
        scheme, filename = rel.split("/")
        key = Path(filename).stem
        expected_rows = {"side": side_pattern, "top": top_pattern, "endcap": endcap_pattern, "seam": seam_pattern}[key](scheme)
        if parsed != expected_rows:
            raise ValueError(f"源像素与设计定义不符：{rel}")
        colors = {PALETTE[pixel] for row in parsed for pixel in row if pixel != "."}
        if len(colors) > 14:
            raise ValueError(f"单张纹理色数超过14：{rel}")
        rows[scheme][key] = parsed
        print(f"SVG_OK {rel} colors={len(colors)}")
    a_upper_copper = sum(px in "cC" for row in rows["A"]["top"] for px in row)
    a_lower_copper = sum(px in "cC" for row in rows["A"]["side"] for px in row)
    b_copper = sum(px in "cC" for row in rows["B"]["side"] for px in row)
    b_copper_cols = sum(sum(row[x] in "cC" for row in rows["B"]["side"]) >= 8 for x in range(16))
    c_copper_rows = sum(sum(px in "cC" for px in row) >= 8 for row in rows["C"]["side"])
    c_steel_rails = sum(sum(px in "dDL" for px in row) >= 12 for row in rows["C"]["side"])
    if not (a_upper_copper > b_copper * 2 and a_lower_copper < b_copper / 3
            and b_copper_cols >= 2 and c_copper_rows >= 2 and c_steel_rails >= 2):
        raise ValueError(f"三稿结构差异不足：A上盖铜像素={a_upper_copper}, A下壳铜像素={a_lower_copper}, B铜像素={b_copper}, B铜纵缝={b_copper_cols}, C铜横槽={c_copper_rows}, C纵筋={c_steel_rails}")
    manifest = json.loads((ROOT / "manifest.json").read_text(encoding="utf-8"))
    if manifest.get("geometry") != GEOMETRY or manifest.get("png_created") != 0:
        raise ValueError("manifest外形或PNG统计错误")
    for scheme in SCHEMES:
        board = (ROOT / scheme / "board.svg").read_text(encoding="utf-8")
        if 'data-geometry="5x8x5-octagonal"' not in board or f"方案{scheme}" not in board:
            raise ValueError(f"单稿没有标注共用外形或方案名：{scheme}")
    overview_text = (ROOT / "overview.svg").read_text(encoding="utf-8")
    index_text = (ROOT / "index.html").read_text(encoding="utf-8")
    for scheme in SCHEMES:
        if f'data-scheme="{scheme}"' not in overview_text or f'href="{scheme}/board.svg"' not in index_text:
            raise ValueError(f"总览或放大入口缺少方案{scheme}")
    if list(ROOT.rglob("*.png")):
        raise ValueError("本批目录不应生成PNG")
    a_board = (ROOT / "A" / "board.svg").read_text(encoding="utf-8")
    if 'data-shell-layer="top"' not in a_board or 'data-shell-layer="side"' not in a_board:
        raise ValueError("A方案未分别呈现上盖和下壳贴图层")
    print(f"DISTINCT_OK A_upper_copper_pixels={a_upper_copper} A_lower_copper_pixels={a_lower_copper} B_copper_pixels={b_copper} B_copper_seams={b_copper_cols} C_copper_groove_rows={c_copper_rows} C_steel_rails={c_steel_rails}")
    print("GEOMETRY_OK 5x8x5 octagonal profile shared across A/B/C")
    print("ENTRY_OK overview.svg index.html plus A/B/C board.svg")
    print("PNG_created=0 formal_texture_write=0")
    print("RESULT PASS svg_sources=12")


def main() -> None:
    parser = argparse.ArgumentParser(description="生成并检查汽轮机机壳贴图三稿")
    parser.add_argument("--check", action="store_true", help="只读检查已生成的源稿和展示入口")
    args = parser.parse_args()
    if args.check:
        check()
    else:
        build()
        print("已生成12张16×16 SVG、3张方案板、总览与入口页。")


if __name__ == "__main__":
    main()
