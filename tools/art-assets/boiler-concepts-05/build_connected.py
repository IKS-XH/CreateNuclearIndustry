from pathlib import Path
import html
import re
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parent
OUT = ROOT / "connected"
OLD = ROOT

STYLES = {
    "A": {
        "folder": "01-horizontal-hoop", "title": "A 横箍压力舱", "name": "横箍压力舱",
        "colors": ["#252B30", "#3D4549", "#657176", "#A6B0AE", "#D6D6C8", "#7B5A3C", "#B98A62", "#D7AE55", "#5B8FA7", "#B9573D"],
        "base": 1, "field": 2, "edge": 0, "light": 3, "warm": 6,
    },
    "B": {
        "folder": "02-refractory-jacket", "title": "B 耐火砖护套", "name": "耐火砖护套",
        "colors": ["#2A2826", "#51463E", "#796653", "#B99775", "#E4D2B2", "#C5AD8E", "#A57E5F", "#68777A", "#4F8CA4", "#B8583E"],
        "base": 3, "field": 4, "edge": 1, "light": 5, "warm": 6,
    },
    "C": {
        "folder": "03-steel-frame", "title": "C 钢骨架舱", "name": "钢骨架舱",
        "colors": ["#20272B", "#364147", "#52636A", "#8E9DA0", "#BDC6C1", "#75533A", "#B28A61", "#D3AD62", "#548CA2", "#B8533A"],
        "base": 1, "field": 2, "edge": 0, "light": 3, "warm": 6,
    },
}

NEIGHBORS = ((0, -1), (1, 0), (0, 1), (-1, 0))


def rect(x, y, width, height, color):
    return f'<rect x="{x}" y="{y}" width="{width}" height="{height}" fill="{color}"/>'


def neighbor_mask(x, y, cells):
    """根据面内相邻单元返回四边邻接掩码；空缺边由外周包边补齐。"""
    mask = 0
    for bit, (dx, dy) in enumerate(NEIGHBORS):
        if (x + dx, y + dy) in cells:
            mask |= 1 << bit
    return mask


def tile_svg(style_key, kind, mask=15, phase=0):
    """生成单块连接模块；只在面边界缺少邻块时画粗包边。"""
    style = STYLES[style_key]
    colors = style["colors"]
    base = colors[style["base"]]
    field = colors[style["field"]]
    edge = colors[style["edge"]]
    light = colors[style["light"]]
    warm = colors[style["warm"]]
    out = ['<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges">']
    out.append(rect(0, 0, 16, 16, field if kind == "window" else base))

    if kind == "side":
        if style_key == "A":
            out.extend((rect(0, 4, 16, 1, edge), rect(0, 5, 16, 1, light), rect(0, 12, 16, 1, edge), rect(0, 13, 16, 1, light)))
            if phase & 1:
                out.extend((rect(0, 2, 16, 1, edge), rect(0, 3, 16, 2, light), rect(0, 5, 16, 1, colors[1]), rect(0, 10, 16, 1, edge), rect(0, 11, 16, 2, light), rect(0, 13, 16, 1, colors[1])))
            if phase & 2:
                out.extend((rect(5, 7, 2, 3, colors[5]), rect(7, 7, 3, 3, warm), rect(10, 7, 1, 3, colors[5])))
        elif style_key == "B":
            out.extend((rect(0, 4, 16, 1, colors[5]), rect(0, 10, 16, 1, colors[5])))
            joint = 4 if not phase & 1 else 10
            out.extend((rect(joint, 1, 1, 4, colors[2]), rect(12 - joint, 6, 1, 5, colors[2]), rect(joint, 12, 1, 3, colors[2])))
            if phase & 2:
                out.extend((rect(0, 6, 16, 1, colors[1]), rect(0, 7, 16, 2, colors[7]), rect(0, 9, 16, 1, colors[1])))
        else:
            if phase:
                quadrant = phase - 1
                qx, qy = quadrant % 2, quadrant // 2
                for y in range(16):
                    run_color = None
                    run_start = 0
                    for x in range(17):
                        color = None
                        if x < 16:
                            gx, gy = qx * 16 + x, qy * 16 + y
                            d1, d2 = abs(gy - gx), abs(gy - (31 - gx))
                            distance = min(d1, d2)
                            if distance <= 2:
                                color = colors[1] if distance == 2 else colors[3]
                        if color != run_color:
                            if run_color is not None:
                                out.append(rect(run_start, y, x - run_start, 1, run_color))
                            run_start, run_color = x, color
    elif kind == "top":
        if style_key == "A":
            out.extend((rect(0, 3, 16, 2, colors[1]), rect(0, 4, 16, 1, light), rect(0, 11, 16, 2, colors[1]), rect(0, 11, 16, 1, edge)))
        elif style_key == "B":
            out.extend((rect(0, 4, 16, 1, colors[5]), rect(0, 10, 16, 1, colors[5]), rect(7 if phase else 3, 1, 1, 3, colors[2]), rect(11 if phase else 7, 5, 1, 5, colors[2])))
    elif kind == "window":
        glass = colors[8]
        out.append(rect(0, 0, 16, 16, glass))
        if phase == 1:
            out.extend((rect(3, 3, 1, 8, colors[3]), rect(4, 3, 3, 1, colors[4])))

    # 只有缺少同类相邻单元的边绘制外框；内部接缝不再画逐格框线。
    if not mask & 1:
        out.extend((rect(0, 0, 16, 2, edge), rect(0, 2, 16, 1, light)))
    if not mask & 2:
        out.extend((rect(14, 0, 2, 16, edge), rect(13, 0, 1, 16, light)))
    if not mask & 4:
        out.extend((rect(0, 14, 16, 2, edge), rect(0, 13, 16, 1, light)))
    if not mask & 8:
        out.extend((rect(0, 0, 2, 16, edge), rect(2, 0, 1, 16, light)))
    out.append("</svg>")
    return "\n".join(out) + "\n"


def symbol(style_key, kind, mask=15, phase=0):
    raw = tile_svg(style_key, kind, mask, phase)
    inner = raw[raw.index(">") + 1:raw.rfind("</svg>")]
    return f'<symbol id="{style_key}-{kind}-{mask}-{phase}" viewBox="0 0 16 16" width="16" height="16">{inner}</symbol>'


def text(x, y, value, size=18, color="#30393B", weight=700):
    safe = html.escape(value)
    return f'<text x="{x}" y="{y}" font-family="Segoe UI,Arial,sans-serif" font-size="{size}" font-weight="{weight}" fill="{color}">{safe}</text>'


def poly(points, fill, stroke="#20272B", width=1.8, tier=None):
    values = " ".join(f"{x:.2f},{y:.2f}" for x, y in points)
    mark = f' data-valve-tier="{tier}"' if tier is not None else ""
    return f'<polygon{mark} points="{values}" fill="{fill}" stroke="{stroke}" stroke-width="{width:.2f}" stroke-linejoin="round"/>'


def tile_use(style_key, kind, mask, phase, matrix, face="detail", world_y=None):
    y_mark = f' data-world-y="{world_y}"' if world_y is not None else ""
    return f'<use data-face="{face}" data-kind="{kind}" data-edge-mask="{mask}"{y_mark} href="#{style_key}-{kind}-{mask}-{phase}" width="16" height="16" transform="matrix({matrix})"/>'


def cube(cx, top, scale, style_key, width=5, depth=5, height=7):
    """用共用邻接模块构成任意尺寸墙面、合并连窗与闭合顶面。"""
    dx, dy, vertical = 30 * scale, 15 * scale, 27 * scale
    back = (cx, top)
    left_top = (cx - depth * dx, top + depth * dy)
    right_top = (cx + width * dx, top + width * dy)
    front_top = (cx + (width - depth) * dx, top + (width + depth) * dy)
    def pair(value):
        return f"{value[0]:.2f},{value[1]:.2f}"
    out = [f'<g data-model-width="{width}" data-model-depth="{depth}" data-model-height="{height}" data-center="{pair(back)}" data-back="{pair(back)}" data-left="{pair(left_top)}" data-right="{pair(right_top)}" data-front="{pair(front_top)}" data-left-tiles="{width*height}" data-right-tiles="{depth*height}" data-top-tiles="{width*depth}" data-window-world-y="4,5" data-window-screen-rows="{height-1-5},{height-1-4}" data-valve-column="{width//2}" data-valve-depth="{depth//2}" data-valve-levels="0-7,7-10,10-14,14-16">']
    win_x0 = max(1, width // 2 - 1)
    # 观察窗固定在隔层上方实际Y=4、5，再换算成从顶往下的屏幕行。
    win_rows = tuple(height - 1 - world_y for world_y in (4, 5) if world_y < height)
    if not win_rows:
        win_rows = (max(0, height - 2), max(0, height - 1))
    win_cells = {(x, y) for x in (win_x0, min(width - 1, win_x0 + 1)) for y in win_rows if y < height}

    def phase_for(kind, col, row, horizontal_size):
        if kind == "window":
            return 1 if (col, row) == (win_x0, min(win_rows)) else 0
        if style_key == "A":
            heavy = row in (0, height - 1, height - 1 - 3)
            inset = col == horizontal_size // 2 and row == 2
            return (1 if heavy else 0) | (2 if inset else 0)
        if style_key == "B":
            partition_row = height - 1 - 3
            return ((col + row) & 1) | (2 if row == partition_row else 0)
        # 交错选择2×2支撑组，每格只绘制同一条跨格斜筋/X的一段。
        return 1 + (col % 2) + 2 * (row % 2) if (col // 2 + row // 2) % 2 == 0 else 0

    # 左墙从left沿W列(+dx,+dy)铺到front，掩码按该面真实W×H邻接计算。
    left_cells = {(x, y) for x in range(width) for y in range(height)}
    left_windows = {(x, y) for x, y in win_cells if x < width}
    for col in range(width):
        for row in range(height):
            px = cx - depth * dx + col * dx
            py = top + depth * dy + col * dy + row * vertical
            kind = "window" if (col, row) in left_windows else "side"
            mask = neighbor_mask(col, row, left_windows if kind == "window" else left_cells)
            phase = phase_for(kind, col, row, width)
            matrix = f"{dx/16:.4f},{dy/16:.4f},0,{vertical/16:.4f},{px:.2f},{py:.2f}"
            out.append(tile_use(style_key, kind, mask, phase, matrix, "left", height - 1 - row if kind == "window" else None))

    # 右墙从front沿D列(+dx,-dy)铺到right，窗口掩码根据D×H独立收口。
    right_cells = {(x, y) for x in range(depth) for y in range(height)}
    right_windows = {(x, y) for x, y in win_cells if x < depth}
    for col in range(depth):
        for row in range(height):
            px = cx + (width - depth) * dx + col * dx
            py = top + (width + depth) * dy - col * dy + row * vertical
            kind = "window" if (col, row) in right_windows else "side"
            mask = neighbor_mask(col, row, right_windows if kind == "window" else right_cells)
            phase = phase_for(kind, col, row, depth)
            matrix = f"{dx/16:.4f},{-dy/16:.4f},0,{vertical/16:.4f},{px:.2f},{py:.2f}"
            out.append(tile_use(style_key, kind, mask, phase, matrix, "right", height - 1 - row if kind == "window" else None))

    top_cells = {(x, z) for x in range(width) for z in range(depth)}
    for z in range(depth):
        for x in range(width):
            px, py = cx + x * dx - z * dx, top + x * dy + z * dy
            mask = neighbor_mask(x, z, top_cells)
            phase = (x + z) & 1 if style_key == "B" else 0
            matrix = f"{dx/16:.4f},{dy/16:.4f},{-dx/16:.4f},{dy/16:.4f},{px:.2f},{py:.2f}"
            out.append(tile_use(style_key, "top", mask, phase, matrix, "top"))

    # 接口按结构实际底部Y定位，避免把上下方向误读成屏幕行号。
    port_cells = [("water_port", "left", 1, 1), ("cold_port", "left", width - 2, 3), ("steam_port", "right", depth - 2, height - 2), ("hot_port", "right", 1, 0), ("controller_face", "right", depth // 2, 1)]
    for kind, face, col, level in port_cells:
        horizontal_size = width if face == "left" else depth
        if 0 <= col < horizontal_size and 0 <= level < height:
            row = height - 1 - level
            if face == "left":
                px = cx - depth * dx + col * dx
                py = top + depth * dy + col * dy + row * vertical
                matrix = f"{dx/16:.4f},{dy/16:.4f},0,{vertical/16:.4f},{px:.2f},{py:.2f}"
            else:
                px = cx + (width - depth) * dx + col * dx
                py = top + (width + depth) * dy - col * dy + row * vertical
                matrix = f"{dx/16:.4f},{-dy/16:.4f},0,{vertical/16:.4f},{px:.2f},{py:.2f}"
            out.append(f'<g data-port="{kind}" data-face="{face}" data-world-y="{level}" data-column="{col}" data-face-width="{horizontal_size}" transform="matrix({matrix})"><use href="#{style_key}-{kind}" width="16" height="16" transform="translate(4 4) scale(0.5)"/></g>')

    left = [left_top, front_top, (front_top[0], front_top[1] + height * vertical), (left_top[0], left_top[1] + height * vertical)]
    right = [front_top, right_top, (right_top[0], right_top[1] + height * vertical), (front_top[0], front_top[1] + height * vertical)]
    top_face = [back, right_top, front_top, left_top]
    out.append(poly(left, "none", "#20272B", 3.0 * scale))
    out.append(poly(right, "none", "#20272B", 3.0 * scale))
    out.append(poly(top_face, "none", "#20272B", 3.0 * scale))
    # 顶面两条高光与侧墙包边在同一转角收口。
    out.append(f'<path d="M {left_top[0]:.2f} {left_top[1]:.2f} L {back[0]:.2f} {back[1]:.2f} L {right_top[0]:.2f} {right_top[1]:.2f}" fill="none" stroke="#A6B0AE" stroke-width="{1.6*scale:.2f}"/>')

    # 安全阀按既有四段阶梯尺寸绘成矮小立体件。
    def valve_point(x, z, y):
        center_col, center_depth = width // 2 + 0.5, depth // 2 + 0.5
        center_x = cx + (center_col - center_depth) * dx
        center_y = top + (center_col + center_depth) * dy
        return (center_x + (x - z) * dx / 16, center_y + (x + z - 16) * dy / 16 - y * vertical / 16)
    tiers = [(0, 16, 0, 16, 0, 7, "#3D4549", "#657176", "#A6B0AE"), (5, 11, 5, 11, 7, 10, "#364147", "#52636A", "#8E9DA0"), (6, 10, 6, 10, 10, 14, "#3D4549", "#657176", "#A6B0AE"), (3, 13, 3, 13, 14, 16, "#75533A", "#B28A61", "#D3AD62")]
    for tier, (x0, x1, z0, z1, y0, y1, lc, fc, tc) in enumerate(tiers, 1):
        out.append(poly([valve_point(x0,z0,y0), valve_point(x0,z1,y0), valve_point(x0,z1,y1), valve_point(x0,z0,y1)], lc, "#20272B", 0.8 * scale, tier))
        out.append(poly([valve_point(x0,z1,y0), valve_point(x1,z1,y0), valve_point(x1,z1,y1), valve_point(x0,z1,y1)], fc, "#20272B", 0.8 * scale, tier))
        out.append(poly([valve_point(x0,z0,y1), valve_point(x1,z0,y1), valve_point(x1,z1,y1), valve_point(x0,z1,y1)], tc, "#20272B", 0.8 * scale, tier))

    out.append("</g>")
    return "".join(out)


def defs(style_key):
    out = ["<defs>"]
    for kind in ("side", "top", "window"):
        for mask in range(16):
            for phase in range(5):
                out.append(symbol(style_key, kind, mask, phase))
    for port in ("water_port", "steam_port", "hot_port", "cold_port", "controller_face"):
        raw = (OLD / STYLES[style_key]["folder"] / f"{port}.svg").read_text(encoding="utf-8")
        inner = raw[raw.index(">") + 1:raw.rfind("</svg>")]
        out.append(f'<symbol id="{style_key}-{port}" viewBox="0 0 16 16" width="16" height="16">{inner}</symbol>')
    out.append("</defs>")
    return "".join(out)


def root_svg(width, height, content, label):
    return f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="{width}" height="{height}" viewBox="0 0 {width} {height}" preserveAspectRatio="xMidYMin meet" shape-rendering="crispEdges"><title>{html.escape(label)}</title>{content}</svg>\n'


def connected_texture_sources(style_key, folder):
    target = OUT / style_key
    target.mkdir(parents=True, exist_ok=True)
    for name, kind, mask, phase in (("casing_side.svg", "side", 15, 0), ("casing_side_boundary.svg", "side", 0, 0), ("casing_top.svg", "top", 15, 0), ("window_module.svg", "window", 0, 0)):
        (target / name).write_text(tile_svg(style_key, kind, mask, phase), encoding="utf-8")


def overview_svg():
    out = ["<rect width=\"1800\" height=\"1040\" fill=\"#e7e5df\"/>", text(70, 72, "锅炉连接外观", 38, "#263238"), text(72, 108, "三种外周收口与成型接缝 · 设计稿，未接入游戏", 19, "#566367", 400)]
    centers = (300, 900, 1500)
    for key, cx in zip(STYLES, centers):
        out.append(defs(key))
        out.append(text(cx - 190, 160, STYLES[key]["title"], 26, "#263238"))
        out.append(cube(cx, 220, 1.35, key, 5, 5, 7))
        out.append(text(cx - 160, 870, "5 × 5 × 7", 20, "#566367"))
        out.append(text(cx - 160, 912, {"A": "连续横箍与窄耐火嵌片", "B": "暖砖错缝与外周钢框", "C": "深凹板与疏朗整体斜筋"}[key], 18, "#566367", 400))
    out.append(f'<image href="../../../../src/main/resources/assets/create_nuclear_industry/textures/block/reactor_casing_side.png" x="1430" y="955" width="32" height="32" preserveAspectRatio="xMidYMid meet"/>')
    out.append(text(1472, 980, "现行反应堆外壳（对照）", 15, "#566367", 400))
    return root_svg(1800, 1040, "".join(out), "锅炉连接外观总览")


def flat_cluster(style_key, x, y, cell=16, columns=3, rows=2, window=False):
    out = []
    cells = {(i, j) for i in range(columns) for j in range(rows)}
    for j in range(rows):
        for i in range(columns):
            kind = "window" if window else "side"
            mask = neighbor_mask(i, j, cells)
            if window:
                phase = 1 if (i, j) == (0, 0) else 0
            elif style_key == "A":
                phase = 1 if j in (0, rows - 1) else 0
            elif style_key == "B":
                phase = ((i + j) & 1) | (2 if j == rows // 2 else 0)
            else:
                phase = 1 + (i % 2) + 2 * (j % 2) if (i // 2 + j // 2) % 2 == 0 else 0
            m = f"{cell/16:.3f},0,0,{cell/16:.3f},{x+i*cell},{y+j*cell}"
            out.append(tile_use(style_key, kind, mask, phase, m, "detail-window" if window else "detail-wall"))
    return "".join(out)


def board_svg(style_key):
    out = ["<rect width=\"1800\" height=\"1500\" fill=\"#e7e5df\"/>", text(60, 70, f"{STYLES[style_key]['title']} · 连接外观", 34, "#263238"), text(62, 108, "设计稿，未接入游戏", 18, "#566367", 400), defs(style_key)]
    out.append(text(60, 165, "5 × 5 × 7", 24, "#263238"))
    out.append(cube(320, 250, 2.0, style_key, 5, 5, 7))
    out.append(text(850, 165, "7 × 5 × 9", 24, "#263238"))
    out.append(cube(1150, 250, 2.0, style_key, 7, 5, 9))
    out.append(text(70, 1140, "连续墙面模块", 22, "#263238"))
    out.append(flat_cluster(style_key, 80, 1170, 100, 3, 2, False))
    out.append(text(550, 1140, "隔层以上 2 × 2 连窗", 22, "#263238"))
    out.append(flat_cluster(style_key, 570, 1170, 140, 2, 2, True))
    out.append(text(1020, 1140, "接口嵌入统一壳面", 22, "#263238"))
    # 展示整合后的管口：壳面不分格，接口法兰仍保留自身形状。
    out.append(f'<use href="#{style_key}-water_port" width="16" height="16" transform="translate(1040 1170) scale(2.8)"/>')
    out.append(f'<use href="#{style_key}-steam_port" width="16" height="16" transform="translate(1120 1170) scale(2.8)"/>')
    out.append(f'<use href="#{style_key}-cold_port" width="16" height="16" transform="translate(1200 1170) scale(2.8)"/>')
    out.append(f'<use href="#{style_key}-hot_port" width="16" height="16" transform="translate(1280 1170) scale(2.8)"/>')
    out.append(text(1030, 1250, "给水　蒸汽　冷却液　热液", 16, "#566367", 400))
    out.append(text(1470, 1140, "现行反应堆外壳", 18, "#566367"))
    out.append(f'<image href="../../../../../src/main/resources/assets/create_nuclear_industry/textures/block/reactor_casing_side.png" x="1480" y="1180" width="64" height="64" preserveAspectRatio="xMidYMid meet"/>')
    return root_svg(1800, 1500, "".join(out), f"{STYLES[style_key]['title']}连接外观单稿")


def index_html():
    return '''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>锅炉连接外观设计稿</title><style>body{margin:0;background:#e6e5df;color:#253036;font:15px Segoe UI,Arial,sans-serif}header{padding:14px 24px;background:#263238;color:#f7f4eb}header h1{margin:0 0 4px;font-size:22px}header p{margin:0;color:#d4dedc}.tabs{display:flex;flex-wrap:wrap;gap:8px;padding:10px 22px}.tabs button{border:1px solid #7d8889;background:#faf9f5;padding:9px 15px;border-radius:4px;font-weight:700;cursor:pointer}.tabs button.active{background:#53666c;color:white}main{margin:0 18px 18px;background:#faf9f5;border:1px solid #c4c5bf;line-height:0}object{display:block;width:100%;height:calc(100vh - 125px);min-height:760px;object-fit:contain;object-position:top center}</style><header><h1>锅炉连接外观提案</h1><p>三套SVG设计稿 · 设计稿，未接入游戏</p></header><nav class="tabs"><button class="active" data-src="connected/overview.svg">连接外观</button><button data-src="connected/A/board.svg">A 横箍压力舱</button><button data-src="connected/B/board.svg">B 耐火砖护套</button><button data-src="connected/C/board.svg">C 钢骨架舱</button><button data-src="overview.svg">原单块稿</button><button data-src="01-horizontal-hoop/board.svg">原稿 A</button><button data-src="02-refractory-jacket/board.svg">原稿 B</button><button data-src="03-steel-frame/board.svg">原稿 C</button></nav><main><object id="board" data="connected/overview.svg" type="image/svg+xml" aria-label="锅炉连接外观设计稿"></object></main><script>document.querySelectorAll('button').forEach(b=>b.addEventListener('click',()=>{document.querySelectorAll('button').forEach(x=>x.classList.remove('active'));b.classList.add('active');document.getElementById('board').data=b.dataset.src;}));</script></html>'''


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for key, style in STYLES.items():
        connected_texture_sources(key, style["folder"])
        (OUT / key / "board.svg").write_text(board_svg(key), encoding="utf-8")
    (OUT / "overview.svg").write_text(overview_svg(), encoding="utf-8")
    (ROOT / "index.html").write_text(index_html(), encoding="utf-8")
    print(f"overview={OUT / 'overview.svg'}")
    print(f"boards={sum(1 for _ in OUT.glob('*/board.svg'))}")
    print(f"connected_tiles={sum(1 for _ in OUT.glob('*/*.svg')) - sum(1 for _ in OUT.glob('*/board.svg'))}")


if __name__ == "__main__":
    main()
