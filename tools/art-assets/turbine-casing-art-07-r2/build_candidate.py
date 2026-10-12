"""从原创整数像素SVG导出R2侧壳稀疏atlas，并生成真实OBJ预览。"""

from __future__ import annotations

import argparse
import base64
import hashlib
import html
import json
import math
import struct
import zlib
from pathlib import Path
from xml.etree import ElementTree as ET

from importlib.util import module_from_spec, spec_from_file_location

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
GENERATED = HERE / "generated"
MESH = ROOT / "src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh"
OLD_TEXTURE = ROOT / "src/main/resources/assets/create_nuclear_industry/textures/block/turbine"
OLD_TOOL_PATH = ROOT / "tools/art-assets/turbine-casing-art-07/build_candidate.py"
SVG_NS = "http://www.w3.org/2000/svg"
PALETTE = {
    "D": "#28353A", "S": "#3E5058", "M": "#5B7078", "H": "#788E94",
    "L": "#A3B7B8", "W": "#D1DFDA", "C": "#A87942", "c": "#715035",
}
ATLAS_WIDTH, ATLAS_HEIGHT = 64, 32


def old_module():
    """复用07B只读OBJ解析与投影；不调用其生成或写入入口。"""
    spec = spec_from_file_location("turbine_casing_07b_readonly", OLD_TOOL_PATH)
    if spec is None or spec.loader is None:
        raise RuntimeError("无法载入07B只读OBJ解析器")
    module = module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def make_atlas() -> list[list[str]]:
    """上行为d5两相位、下行为d7三相位，其余区域铺满钢色。"""
    grid = [["M"] * ATLAS_WIDTH for _ in range(ATLAS_HEIGHT)]
    for y in range(ATLAS_HEIGHT):
        local_y = y % 16
        for x in range(ATLAS_WIDTH):
            # 深色底边和柔和侧光贯穿每格，避免形成连续高亮束带。
            color = {0: "D", 1: "S", 2: "H", 13: "S", 14: "D", 15: "D"}.get(local_y, "M")
            grid[y][x] = color

    # d5 atlas上排：偶相位为整片钢板，奇相位仅有短铜接缝和稀疏刻线。
    paint_sparse_cell(grid, col=0, row=0, copper=False, variant=0)
    paint_sparse_cell(grid, col=1, row=0, copper=True, variant=1)
    # d7 atlas下排：两相位整片钢板，仅第三相位出现短铜缝。
    paint_sparse_cell(grid, col=0, row=1, copper=False, variant=0)
    paint_sparse_cell(grid, col=1, row=1, copper=False, variant=1)
    paint_sparse_cell(grid, col=2, row=1, copper=True, variant=2)
    return grid


def paint_sparse_cell(grid: list[list[str]], col: int, row: int, copper: bool, variant: int) -> None:
    """绘制16×16单格：钢板留白为主，铜标只占短边缝的一小段。"""
    x0, y0 = col * 16, row * 16
    # 每格都是连续钢板；顶部与底部深边保持低反差，不再叠加多道亮束带。
    for y in range(16):
        for x in range(16):
            grid[y0 + y][x0 + x] = {0: "D", 1: "S", 2: "H", 13: "S", 14: "D", 15: "D"}.get(y, "M")
    if not copper:
        # 无带相位是一整块无内缝钢板，保留外周材料明暗即可。
        return
    # 带相位仅保留一像素暗缝与紧邻窄钢色，不形成强亮线。
    seam = 7 if variant % 2 == 0 else 9
    for y in range(3, 13):
        grid[y0 + y][x0 + seam] = "D"
        grid[y0 + y][x0 + seam + 1] = "S"
    # 少量短刻线，不做整圈束带；短铜嵌条标记轴向分段节奏。
    accent_y = 5 + (variant % 3) * 3
    grid[y0 + accent_y][x0 + 3] = "L"
    grid[y0 + accent_y][x0 + 4] = "H"
    grid[y0 + accent_y + 1][x0 + 3] = "S"
    if copper:
        for x in range(4, 7):
            grid[y0 + 10][x0 + x] = "C" if x == 4 else "c"
        grid[y0 + 11][x0 + 4] = "c"


def svg_text(grid: list[list[str]]) -> str:
    rows = [f'<svg xmlns="{SVG_NS}" width="64" height="32" viewBox="0 0 64 32" shape-rendering="crispEdges">',
            "<title>汽轮机中大型机壳稀疏钢板atlas</title>",
            '<desc>每个16×16子格对应一格世界长度；d5两相位在上排，d7三相位在下排。</desc>']
    for y, row in enumerate(grid):
        x = 0
        while x < ATLAS_WIDTH:
            end = x + 1
            while end < ATLAS_WIDTH and row[end] == row[x]:
                end += 1
            rows.append(f'<rect x="{x}" y="{y}" width="{end-x}" height="1" fill="{PALETTE[row[x]]}"/>')
            x = end
    rows.append("</svg>")
    return "\n".join(rows) + "\n"


def png_chunk(kind: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)


def export_png(grid: list[list[str]]) -> bytes:
    raw = b"".join(b"\x00" + b"".join(bytes.fromhex(PALETTE[key][1:]) + b"\xff" for key in row) for row in grid)
    return b"\x89PNG\r\n\x1a\n" + png_chunk(b"IHDR", struct.pack(">IIBBBBB", 64, 32, 8, 6, 0, 0, 0)) + png_chunk(b"IDAT", zlib.compress(raw, 9)) + png_chunk(b"IEND", b"")


def write_sources() -> tuple[list[list[str]], bytes]:
    grid = make_atlas()
    svg = svg_text(grid)
    (HERE / "turbine_casing_07_sparse.svg").write_text(svg, encoding="utf-8", newline="\n")
    png = export_png(grid)
    out = GENERATED / "textures/turbine_casing_07_sparse.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(png)
    return grid, png


def map_uv(uv: tuple[float, float], diameter: int, phase: int) -> tuple[float, float]:
    """按负责人锁定公式，把原壳面UV映射到atlas的16×16相位格。"""
    u, v = uv
    if diameter == 5:
        col, row = phase % 2, 0
    elif diameter == 7:
        col, row = phase % 3, 1
    else:
        return uv
    return ((col + u) / 4.0, (row + v) / 2.0)


def phase_for(world_long: int, facing: str, diameter: int) -> int:
    """模拟消费者以位置点乘朝向反方向所得的纯装饰相位。"""
    period = 2 if diameter == 5 else 3
    # 示例把front轴向坐标设为0；south朝向时点乘反方向令轴向递增为负。
    sign = {"north": 1, "east": -1, "south": -1, "west": 1}[facing]
    return (sign * world_long) % period


def instances_for(old, diameter: int, facing: str = "south"):
    """组装实际front、六个middle与rear OBJ，局部轴向映射到世界8格。"""
    by_role = {"front": [], "middle": [], "rear": []}
    for path in old.target_models():
        details = old.formed_details(path)
        if details and details[0] == diameter:
            _, role, x, y = details
            by_role[role].append((path, float(x), float(y)))
    result = []
    for role, start_z in (("front", 0), ("middle", 1), ("rear", 7)):
        copies = range(1, 7) if role == "middle" else (start_z,)
        for local_z in copies:
            for path, x, y in by_role[role]:
                result.append({"path": path, "offset": (x, y, float(local_z)), "world_long": local_z, "facing": facing})
    expected = diameter * diameter * 8 - (diameter - 2) ** 2 * 8
    if len(result) < expected:
        raise ValueError(f"d{diameter}八格预览OBJ不足：{len(result)} < {expected}")
    return result


def solve_affine(screen, uv, width: int, height: int):
    source = [(u * width, v * height) for u, v in uv]
    for i in range(1, len(source)):
        for j in range(i + 1, len(source)):
            x0, y0 = source[0]
            x1, y1 = source[i]
            x2, y2 = source[j]
            det = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
            if abs(det) < 1e-9:
                continue
            sx0, sy0 = screen[0]
            sx1, sy1 = screen[i]
            sx2, sy2 = screen[j]
            a = ((sx1 - sx0) * (y2 - y0) - (sx2 - sx0) * (y1 - y0)) / det
            c = ((x1 - x0) * (sx2 - sx0) - (x2 - x0) * (sx1 - sx0)) / det
            b = ((sy1 - sy0) * (y2 - y0) - (sy2 - sy0) * (y1 - y0)) / det
            d = ((x1 - x0) * (sy2 - sy0) - (x2 - x0) * (sy1 - sy0)) / det
            e = sx0 - a * x0 - c * y0
            f = sy0 - b * x0 - d * y0
            return a, b, c, d, e, f
    raise ValueError("预览面UV退化")


def png_size(data: bytes) -> tuple[int, int]:
    """从PNG IHDR读取实际采样尺寸，不能假设端盖也为16×16。"""
    if data[:8] != b"\x89PNG\r\n\x1a\n" or len(data) < 24:
        raise ValueError("预览贴图不是有效PNG")
    return struct.unpack(">II", data[16:24])


def render(diameter: int, title: str, assembled, new: bool, textures: dict[str, bytes]) -> str:
    old = old_module()
    faces = []
    serial = 0
    for instance in assembled:
        doc = old.parse_obj(instance["path"])
        ox, oy, oz = instance["offset"]
        phase = phase_for(int(instance["world_long"]), instance["facing"], diameter) if new and diameter in (5, 7) else 0
        for face in doc["faces"]:
            if face["material"] not in ("casing", "endcap", "edge", "inside"):
                continue
            points, uvs = [], []
            for vi, ti, _ in face["indices"]:
                x, y, z = doc["vertices"][vi - 1]
                points.append((x + ox, y + oy, z + oz))
                uv = doc["uvs"][ti - 1]
                if new and face["material"] == "casing" and diameter in (5, 7):
                    uv = map_uv(uv, diameter, phase)
                uvs.append(uv)
            faces.append({"material": face["material"], "points": points, "uvs": uvs,
                          "depth": sum(sum(p) for p in points) / len(points), "serial": serial})
            serial += 1

    scale = 62.0
    project = lambda p: ((p[0] - p[2]) * scale, ((p[0] + p[2]) * 0.5 - p[1]) * scale)
    all_pts = [project(p) for face in faces for p in face["points"]]
    min_x, max_x = min(p[0] for p in all_pts), max(p[0] for p in all_pts)
    min_y, max_y = min(p[1] for p in all_pts), max(p[1] for p in all_pts)
    width, height = math.ceil(max_x - min_x + 80), math.ceil(max_y - min_y + 100)
    dx, dy = 40 - min_x, 52 - min_y
    data_urls = {key: "data:image/png;base64," + base64.b64encode(data).decode("ascii") for key, data in textures.items()}
    rows = [f'<svg xmlns="{SVG_NS}" width="{width}" height="{height}" viewBox="0 0 {width} {height}" shape-rendering="crispEdges" preserveAspectRatio="xMidYMin meet">',
            f"<title>{html.escape(title)}</title>", '<rect width="100%" height="100%" fill="#10171b"/>',
            f'<text x="18" y="27" fill="#e5e7e3" font-family="sans-serif" font-size="17">{html.escape(title)}</text>', "<defs>"]
    transformed = []
    for face in faces:
        screen = [(x + dx, y + dy) for x, y in (project(p) for p in face["points"])]
        pts = " ".join(f"{x:.3f},{y:.3f}" for x, y in screen)
        serial_id = face["serial"]
        rows.append(f'<clipPath id="clip{serial_id}" clipPathUnits="userSpaceOnUse"><polygon points="{pts}"/></clipPath>')
        transformed.append((face, screen, pts))
    rows.append("</defs>")
    for face, screen, pts in sorted(transformed, key=lambda item: (item[0]["depth"], item[0]["serial"])):
        material = face["material"]
        if material == "casing" and new and diameter in (5, 7):
            texture_key = "atlas"
        elif material in ("casing", "endcap"):
            texture_key = material
        else:
            texture_key = "base"
        if texture_key in data_urls:
            tex_w, tex_h = png_size(textures[texture_key])
            a, b, c, d, e, f = solve_affine(screen, face["uvs"], tex_w, tex_h)
            mat = f"{a:.8f} {b:.8f} {c:.8f} {d:.8f} {e:.4f} {f:.4f}"
            rows.append(f'<g clip-path="url(#clip{face["serial"]})"><image href="{data_urls[texture_key]}" width="{tex_w}" height="{tex_h}" transform="matrix({mat})" image-rendering="pixelated"/></g>')
        else:
            rows.append(f'<polygon points="{pts}" fill="#3e5058"/>')
    rows.append("</svg>")
    return "\n".join(rows) + "\n"


def create_previews(atlas_png: bytes) -> list[str]:
    old = old_module()
    GENERATED.mkdir(parents=True, exist_ok=True)
    base_panel = (OLD_TEXTURE / "turbine_casing_panel.png").read_bytes()
    endcap = (OLD_TEXTURE / "turbine_casing_07_endcap.png").read_bytes()
    panel = (OLD_TEXTURE / "turbine_casing_07_panel.png").read_bytes()
    made = []
    for diameter in (3, 5, 7):
        items = instances_for(old, diameter)
        for variant, is_new in (("before", False), ("after", True)):
            if diameter == 3:
                # d3仍保留旧机壳和R1端盖；after卡展示未变化的边界。
                is_new = False
            name = f"d{diameter}-{variant}.svg"
            title = f"d{diameter} · {'原样保留' if diameter == 3 else ('疏线候选' if is_new else '现行B')} · 8格真实OBJ"
            textures = {"casing": atlas_png if is_new else panel, "atlas": atlas_png,
                        "endcap": endcap, "base": base_panel}
            svg = render(diameter, title, items, is_new, textures)
            (GENERATED / name).write_text(svg, encoding="utf-8", newline="\n")
            made.append(name)
    return made


def render_window_scene(title: str, cells: list[dict], textures: dict[str, bytes]) -> str:
    """按消费者实际烘焙quad/UV/alpha证据绘制窗口3D离线预览。"""
    faces = []
    serial = 0
    for cell in cells:
        ox, oy, oz = cell["offset"]
        for face in cell["faces"]:
            material = face["sprite"].rsplit("/", 1)[-1]
            if material not in ("turbine_casing_panel", "turbine_window_glass"):
                continue
            points, uvs, alphas = [], [], []
            for vertex in face["vertices"]:
                px, py, pz = vertex["position"]
                points.append((px + ox, py + oy, pz + oz))
                uvs.append(tuple(vertex["uv"]))
                alphas.append(((int(vertex["color_abgr"]) >> 24) & 0xFF) / 255.0)
            faces.append({"material": material, "points": points, "uvs": uvs,
                          "alpha": sum(alphas) / len(alphas),
                          "depth": sum(sum(point) for point in points) / len(points), "serial": serial})
            serial += 1
    if not faces:
        raise ValueError(f"窗口mesh预览没有可见quad：{title}")
    scale = 210.0
    project = lambda p: ((p[0] - p[2]) * scale, ((p[0] + p[2]) * 0.5 - p[1]) * scale)
    points = [project(point) for face in faces for point in face["points"]]
    min_x, max_x = min(p[0] for p in points), max(p[0] for p in points)
    min_y, max_y = min(p[1] for p in points), max(p[1] for p in points)
    width, height = math.ceil(max_x - min_x + 90), math.ceil(max_y - min_y + 112)
    dx, dy = 45 - min_x, 65 - min_y
    data_urls = {key: "data:image/png;base64," + base64.b64encode(data).decode("ascii") for key, data in textures.items()}
    rows = [f'<svg xmlns="{SVG_NS}" width="{width}" height="{height}" viewBox="0 0 {width} {height}" shape-rendering="crispEdges" preserveAspectRatio="xMidYMin meet">',
            f"<title>{html.escape(title)}</title>", '<rect width="100%" height="100%" fill="#10171b"/>',
            f'<text x="16" y="25" fill="#e5e7e3" font-family="sans-serif" font-size="15">{html.escape(title)}</text>', "<defs>"]
    transformed = []
    for face in faces:
        screen = [(x + dx, y + dy) for x, y in (project(point) for point in face["points"])]
        polygon = " ".join(f"{x:.4f},{y:.4f}" for x, y in screen)
        serial_id = face["serial"]
        rows.append(f'<clipPath id="window-clip-{serial_id}" clipPathUnits="userSpaceOnUse"><polygon points="{polygon}"/></clipPath>')
        transformed.append((face, screen))
    rows.append("</defs>")
    for face, screen in sorted(transformed, key=lambda item: (item[0]["depth"], item[0]["serial"])):
        texture_key = "glass" if face["material"] == "turbine_window_glass" else "casing"
        tex_w, tex_h = png_size(textures[texture_key])
        try:
            a, b, c, d, e, f = solve_affine(screen, face["uvs"], tex_w, tex_h)
        except ValueError:
            # 正投影下完全侧向的quad面积为零，不参与此视角的离线画面。
            continue
        matrix = f"{a:.8f} {b:.8f} {c:.8f} {d:.8f} {e:.4f} {f:.4f}"
        serial_id = face["serial"]
        opacity = face["alpha"]
        rows.append(f'<g clip-path="url(#window-clip-{serial_id})" opacity="{opacity:.6f}"><image href="{data_urls[texture_key]}" width="{tex_w}" height="{tex_h}" transform="matrix({matrix})" image-rendering="pixelated"/></g>')
    rows.append("</svg>")
    return "\n".join(rows) + "\n"


def create_window_previews() -> list[str]:
    """只使用消费者交付的最终实际mesh/UV/透明度证据，不重构猜测几何。"""
    evidence_path = ROOT / "build/reports/art/ART-TURBINE-07B-R2/consumer/window-mesh-glass-uv.json"
    evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
    scenes = {scene["name"]: scene for scene in evidence["scenes"]}
    single = scenes["single"]["cells"][0]
    connected = scenes["three_connected"]["cells"]
    three_original = [{"offset": [0, 0, z], "faces": single["faces"]} for z in range(3)]
    textures = {"casing": (OLD_TEXTURE / "turbine_casing_panel.png").read_bytes(),
                "glass": (OLD_TEXTURE / "turbine_window_glass.png").read_bytes()}
    views = [
        ("window-single.svg", "d5合法中段单窗 · 原模型mesh", [single]),
        ("window-three-before.svg", "d5三窗 · 三份原模型mesh未连接", three_original),
        ("window-three-connected.svg", "d5三窗 · 消费者实际连接mesh", connected),
    ]
    out = GENERATED / "windows"
    out.mkdir(parents=True, exist_ok=True)
    names = []
    for filename, title, cells in views:
        (out / filename).write_text(render_window_scene(title, cells, textures), encoding="utf-8", newline="\n")
        names.append(f"windows/{filename}")
    return names


def write_index(previews: list[str], window_previews: list[str]) -> None:
    cards = []
    for diameter in (3, 5, 7):
        cards.append(f"""<section><h2>d{diameter}：8格机壳</h2><p>轴向顺序：front → middle × 6 → rear。</p><div class="pair">
<figure><figcaption>现行B</figcaption><object data="generated/d{diameter}-before.svg" type="image/svg+xml"></object></figure>
<figure><figcaption>{'保持原样' if diameter == 3 else 'R2稀疏相位候选'}</figcaption><object data="generated/d{diameter}-after.svg" type="image/svg+xml"></object></figure>
</div></section>""")
    window_cards = """<section><h2>观察窗：单窗与三窗沿轴向连接</h2><p>依据消费者交付的实际 d5 合法中段 mesh、UV 与 alpha。仅同尺寸窗口沿轴向相邻连接；游戏侧限制为同piece、同朝向、同成型状态的合法中段窗，端盖与其他部件会截断。</p><div class="window-grid">
<figure><figcaption>1个合法中段单窗：原模型mesh</figcaption><object data="generated/windows/window-single.svg" type="image/svg+xml"></object></figure>
<figure><figcaption>3个原模型：尚未连接</figcaption><object data="generated/windows/window-three-before.svg" type="image/svg+xml"></object></figure>
<figure><figcaption>3个实际连接mesh：共享端框撤除、玻璃跨格</figcaption><object data="generated/windows/window-three-connected.svg" type="image/svg+xml"></object></figure>
</div><p>预览使用消费者离线烘焙quad证据及原PNG纹理/顶点透明度，不等于客户端GPU截图或最终游戏视觉验收。</p></section>"""
    page = """<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>汽轮机机壳R2稀疏相位预览</title><style>
body{margin:0;background:#11191d;color:#e7ece8;font:16px system-ui,sans-serif}main{max-width:1500px;margin:auto;padding:24px}h1{font-size:28px;margin:0 0 8px}p{color:#b9c6c7}.atlas{width:256px;image-rendering:pixelated;border:1px solid #697b80;background:#253238}section{margin:30px 0;padding:20px;border:1px solid #394b51;border-radius:12px;background:#182226}.pair{display:grid;grid-template-columns:1fr 1fr;gap:14px}.window-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:14px}figure{margin:0;padding:10px;background:#0f1518;border-radius:8px}figcaption{margin:0 0 8px;color:#b9c6c7}object{display:block;width:100%;height:490px;background:#10171b} @media(max-width:900px){.pair,.window-grid{grid-template-columns:1fr}object{height:400px}}
</style><main><h1>汽轮机中大型机壳疏线候选</h1><p>每档均为8格长真实OBJ组装，含front、6个middle与rear。d5周期2、d7周期3；每个atlas相位格为16×16，d3与R1端盖保持不变。</p><img class="atlas" src="generated/textures/turbine_casing_07_sparse.png" alt="64乘32稀疏钢壳atlas"><p>直接打开同目录SVG也可单独检查；稀疏纹理已安装试用，客户端视觉待复看。</p>
__CARDS__\n__WINDOW_CARDS__</main></html>
""".replace("__CARDS__", "\n".join(cards))
    page = page.replace("__WINDOW_CARDS__", window_cards if window_previews else "")
    (HERE / "index.html").write_text(page, encoding="utf-8", newline="\n")


def mapping(previews: list[str], png: bytes, window_previews: list[str]) -> dict:
    source_count, source_digest = protected_input_fingerprint()
    installed_path = ROOT / "src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_casing_07_sparse.png"
    installed = installed_path.exists() and installed_path.read_bytes() == png
    return {
        "task": "ART-TURBINE-07B-R2",
        "stage": "唯一新增PNG已安装试用，客户端视觉待复看" if installed else "候选未安装",
        "source": "原创64×32整数rect SVG，同源确定性导出PNG；旧07B脚本和正式资源只读",
        "targets": {"candidate_png": "generated/textures/turbine_casing_07_sparse.png", "formal_png_after_authorized_install": "src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_casing_07_sparse.png"},
        "atlas": {"size": [64, 32], "tile": [16, 16], "d5": {"row": 0, "columns": [0, 1], "period": 2}, "d7": {"row": 1, "columns": [0, 1, 2], "period": 3}, "unused": "其余像素为连续钢色", "uv": "((phase+u)/4,(row+v)/2)"},
        "phase": "纯装饰，floorMod(worldPos·machine_facing.getOpposite(),周期)。north:+Z/east:-X/south:-Z/west:+X。示例south朝向、front轴坐标0；移动位置会改变相位。",
        "preserved": ["d3现行quad/PNG", "R1端盖PNG/OBJ UV", "旧07B PNG/OBJ/MTL/脚本"],
        "preview_files": previews,
        "window_mesh_evidence": "build/reports/art/ART-TURBINE-07B-R2/consumer/window-mesh-glass-uv.json",
        "window_preview_files": window_previews,
        "sha256_atlas_png": hashlib.sha256(png).hexdigest(),
        "protected_input_fingerprint": {"files": source_count, "sha256": source_digest,
                                        "scope": "旧07B工具、3张旧机壳PNG及206张d3/d5/d7成型OBJ"},
    }


def protected_input_fingerprint() -> tuple[int, str]:
    """聚合核对候选依赖的旧工具、贴图与成型OBJ未发生漂移。"""
    old = old_module()
    source_hash = hashlib.sha256()
    source_count = 0
    protected = [OLD_TOOL_PATH, OLD_TEXTURE / "turbine_casing_panel.png",
                 OLD_TEXTURE / "turbine_casing_07_panel.png", OLD_TEXTURE / "turbine_casing_07_endcap.png"]
    protected.extend(path for path in old.target_models() if old.formed_details(path))
    for path in sorted(protected, key=lambda p: str(p).casefold()):
        relative = path.relative_to(ROOT).as_posix().encode("utf-8")
        source_hash.update(len(relative).to_bytes(4, "big"))
        source_hash.update(relative)
        data = path.read_bytes()
        source_hash.update(len(data).to_bytes(8, "big"))
        source_hash.update(data)
        source_count += 1
    return source_count, source_hash.hexdigest()


def read_svg_grid(svg_path: Path) -> list[list[str]]:
    root = ET.parse(svg_path).getroot()
    if root.attrib.get("width") != "64" or root.attrib.get("height") != "32" or root.attrib.get("viewBox") != "0 0 64 32":
        raise ValueError("atlas SVG画布尺寸不符")
    grid: list[list[str | None]] = [[None] * 64 for _ in range(32)]
    for node in root:
        if node.tag == f"{{{SVG_NS}}}title" or node.tag == f"{{{SVG_NS}}}desc":
            continue
        if node.tag != f"{{{SVG_NS}}}rect":
            raise ValueError("atlas SVG含非rect绘图图元")
        x, y, width, height = (int(node.attrib[key]) for key in ("x", "y", "width", "height"))
        color = node.attrib.get("fill", "")
        if color not in PALETTE.values() or min(x, y, width, height) < 0 or width < 1 or height < 1 or x + width > 64 or y + height > 32:
            raise ValueError("atlas SVG像素越界或色板无效")
        code = next(key for key, value in PALETTE.items() if value == color)
        for yy in range(y, y + height):
            for xx in range(x, x + width):
                if grid[yy][xx] is not None:
                    raise ValueError("atlas SVG像素重叠")
                grid[yy][xx] = code
    if any(pixel is None for row in grid for pixel in row):
        raise ValueError("atlas SVG有未绘制像素")
    return [[pixel for pixel in row if pixel is not None] for row in grid]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.check:
        svg_path = HERE / "turbine_casing_07_sparse.svg"
        grid = read_svg_grid(svg_path)
        if grid != make_atlas():
            raise ValueError("SVG atlas与五个已定义相位格不符")
        elements = len([node for node in ET.parse(svg_path).getroot() if node.tag == f"{{{SVG_NS}}}rect"])
        png = (GENERATED / "textures/turbine_casing_07_sparse.png").read_bytes()
        if png != export_png(grid):
            raise ValueError("候选PNG与SVG像素不一致")
        data = json.loads((HERE / "mapping.json").read_text(encoding="utf-8"))
        if hashlib.sha256(png).hexdigest() != data.get("sha256_atlas_png"):
            raise ValueError("mapping记录的候选PNG SHA不一致")
        count, digest = protected_input_fingerprint()
        expected = data.get("protected_input_fingerprint", {})
        if count != expected.get("files") or digest != expected.get("sha256"):
            raise ValueError("旧07B工具、纹理或成型OBJ输入哈希发生变化")
        preview_names = data.get("preview_files", [])
        if len(preview_names) != 6:
            raise ValueError("应有d3/d5/d7各两张预览")
        for preview_name in preview_names:
            preview_root = ET.parse(GENERATED / preview_name).getroot()
            if not preview_root.tag.endswith("svg") or int(preview_root.attrib.get("width", "0")) <= 0 or int(preview_root.attrib.get("height", "0")) <= 0:
                raise ValueError(f"实际OBJ预览SVG无效：{preview_name}")
        formal = ROOT / data["targets"]["formal_png_after_authorized_install"]
        if formal.exists() and formal.read_bytes() != png:
            raise ValueError("唯一正式PNG与候选PNG不一致")
        window_names = data.get("window_preview_files", [])
        if len(window_names) != 3:
            raise ValueError("应有单窗、三窗原模型与三窗连接mesh三张预览")
        for preview_name in window_names:
            preview_root = ET.parse(GENERATED / preview_name).getroot()
            if not preview_root.tag.endswith("svg") or int(preview_root.attrib.get("width", "0")) <= 0 or int(preview_root.attrib.get("height", "0")) <= 0:
                raise ValueError(f"实际窗口mesh预览SVG无效：{preview_name}")
        mesh_evidence = ROOT / data["window_mesh_evidence"]
        evidence = json.loads(mesh_evidence.read_text(encoding="utf-8"))
        scenes = {scene["name"]: scene for scene in evidence["scenes"]}
        if len(scenes["single"]["cells"][0]["faces"]) != 30 or sum(len(cell["faces"]) for cell in scenes["three_connected"]["cells"]) != 54:
            raise ValueError("窗口mesh quad计数与消费者证据不符")
        status = "installed_png=matches" if formal.exists() else "installed_png=absent"
        print(f"CHECK_OK svg_rects={elements} png_svg=64x32 phase_tiles=5 casing_previews=6 window_previews=3 protected_inputs={count}; {status}")
        return
    grid, png = write_sources()
    previews = create_previews(png)
    window_previews = create_window_previews()
    write_index(previews, window_previews)
    (HERE / "mapping.json").write_text(json.dumps(mapping(previews, png, window_previews), ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(f"CANDIDATE_READY svg=1 png=1 phase_tiles=5 obj_previews={len(previews)} length=8 window_mesh_previews={len(window_previews)}")


if __name__ == "__main__":
    main()
