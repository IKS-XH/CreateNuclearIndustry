"""ART-REACTOR-08窗口专用SVG导出与核对；不读取或写入其他R1图集。"""
from collections import Counter
from decimal import Decimal, ROUND_HALF_UP
import argparse
import hashlib
import io
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET
from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
OLD = ROOT / "tools/art-assets/reactor-ct-r1"
SOURCES = HERE / "sources/reactor_window"
GENERATED = HERE / "generated"
PANE_OPACITY = "0.25098039215686275"
PANE_ALPHA = 64
NS = "http://www.w3.org/2000/svg"
INTEGER = re.compile(r"(?:0|[1-9][0-9]*)\Z")
ROOT_ATTRS = {"width": "16", "height": "16", "viewBox": "0 0 16 16", "shape-rendering": "crispEdges"}

def transparent_pixels(mask):
    """按原Create八向上下文定位透明窗格；坐标及rect尺寸均为像素整数。"""
    hole = set()
    corners = {frozenset((0, 2)): 4, frozenset((0, 3)): 5,
               frozenset((1, 2)): 6, frozenset((1, 3)): 7}
    for y in range(16):
        for x in range(16):
            edges = ([0] if y < 4 else [1] if y >= 12 else [])
            edges += ([2] if x < 4 else [3] if x >= 12 else [])
            required = edges + ([corners[frozenset(edges)]] if len(edges) == 2 else [])
            if all(mask & (1 << bit) for bit in required):
                hole.add((x, y))
    return hole

def read_mapping():
    mapping = json.loads((HERE / "mapping.json").read_text(encoding="utf-8"))
    contexts = mapping["window_contexts"]
    fallbacks = mapping["fallback_indices"]
    if len(contexts) != 47 or len({c["mask"] for c in contexts}) != 47 or len({c["index"] for c in contexts}) != 47:
        raise ValueError("原生窗口上下文必须保持47项唯一")
    if set(c["index"] for c in contexts) | set(fallbacks) != set(range(64)) or len(fallbacks) != 17:
        raise ValueError("窗口索引必须精确分为47个有效项与17个fallback")
    pane = mapping["pane"]
    if pane != {"color": "#66696A", "alpha": 64, "fill_opacity": PANE_OPACITY,
                "scope": "只覆盖64个索引的原透明窗格（47个原生上下文及17个隔离fallback）"}:
        raise ValueError("窗格颜色或透明度偏离已批准候选")
    return mapping

def palette():
    result = json.loads((HERE / "palette.json").read_text(encoding="utf-8"))["reactor_window"]
    if set(result) != {"outline", "frame_light", "frame_mid", "frame_dark", "frame_shadow", "copper_light", "copper_shadow", "pane_lead"}:
        raise ValueError("窗口色板命名必须精确匹配")
    if result["pane_lead"] != "#66696A" or len(set(result.values())) != len(result):
        raise ValueError("窗口铅灰色值或颜色唯一性错误")
    if any(not re.fullmatch(r"#[0-9A-Fa-f]{6}", value) for value in result.values()):
        raise ValueError("色板只允许#RRGGBB")
    return result

def render_tile(text, colors):
    """仅接受SVG整数rect；唯一半透明声明是铅灰pane的alpha64/255。"""
    if re.search(r"<!DOCTYPE|<!ENTITY|<\?", text, re.I):
        raise ValueError("SVG不允许DTD、实体或处理指令")
    root = ET.fromstring(text, parser=ET.XMLParser(target=ET.TreeBuilder(insert_comments=True, insert_pis=True)))
    if root.tag != f"{{{NS}}}svg" or root.attrib != ROOT_ATTRS:
        raise ValueError("根SVG必须保持16×16 crispEdges视口")
    if root.text and root.text.strip():
        raise ValueError("SVG根不允许非空文本")
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pixels = out.load()
    painted = set()
    pane_rgb = tuple(int(colors["pane_lead"][i:i+2], 16) for i in (1, 3, 5))
    for node in root:
        if node.tag is ET.Comment:
            if node.tail and node.tail.strip():
                raise ValueError("注释尾部不允许文本")
            continue
        if node.tag != f"{{{NS}}}rect" or len(node) or (node.text and node.text.strip()):
            raise ValueError("只接受无子节点的直属rect")
        allowed = {"x", "y", "width", "height", "fill"}
        opacity = node.attrib.get("fill-opacity")
        if set(node.attrib) not in (allowed, allowed | {"fill-opacity"}):
            raise ValueError("rect属性必须为整数几何、色值及可选fill-opacity")
        if opacity is not None and (node.get("fill") != colors["pane_lead"] or opacity != PANE_OPACITY):
            raise ValueError("仅铅灰窗格可用固定alpha64/255")
        values = [node.get(k, "") for k in ("x", "y", "width", "height")]
        if any(not INTEGER.fullmatch(v) for v in values):
            raise ValueError("rect坐标/尺寸必须为非负整数")
        x, y, width, height = map(int, values)
        if width < 1 or height < 1 or x + width > 16 or y + height > 16:
            raise ValueError("rect必须完全落在16×16格内")
        fill = node.get("fill", "").upper()
        if fill not in {v.upper() for v in colors.values()}:
            raise ValueError("rect颜色必须属于窗口有限色板")
        alpha = PANE_ALPHA if opacity is not None else 255
        rgb = pane_rgb if opacity is not None else tuple(int(fill[i:i+2], 16) for i in (1, 3, 5))
        for py in range(y, y + height):
            for px in range(x, x + width):
                pos = (px, py)
                if pos in painted:
                    raise ValueError(f"rect重叠：{pos}")
                painted.add(pos)
                pixels[px, py] = (*rgb, alpha)
    if painted != {(x, y) for y in range(16) for x in range(16)}:
        raise ValueError("SVG rect必须完整覆盖16×16格，避免未定义透明区域")
    return out

def encode(image):
    data = io.BytesIO()
    image.save(data, format="PNG", optimize=False)
    return data.getvalue()

def build_images():
    mapping = read_mapping()
    colors = palette()
    original_hashes = mapping["old_source_sha256"]
    native = {c["index"]: c["mask"] for c in mapping["window_contexts"]}
    outputs = {}
    pane_pixels = 0
    frame_pixels = 0
    for i in range(64):
        name = f"tile_{i:02d}.svg"
        source_path = SOURCES / name
        original_svg = OLD / "sources/reactor_window" / name
        source_data = source_path.read_bytes()
        if hashlib.sha256(original_svg.read_bytes()).hexdigest() != original_hashes[name]:
            raise ValueError(f"旧R1源输入有漂移：{name}")
        image = render_tile(source_data.decode("utf-8"), colors)
        old_path = OLD / "generated/tiles/reactor_window" / f"tile_{i:02d}.png"
        old = Image.open(old_path).convert("RGBA")
        mask = native.get(i, 0)
        expected_panes = transparent_pixels(mask)
        actual_panes = {(x, y) for y in range(16) for x in range(16) if image.getpixel((x, y))[3] == PANE_ALPHA}
        if actual_panes != expected_panes:
            raise ValueError(f"{name}: alpha64窗格覆盖与原生mask不一致")
        old_holes = {(x, y) for y in range(16) for x in range(16) if old.getpixel((x, y))[3] == 0}
        if old_holes != expected_panes:
            raise ValueError(f"{name}: 原R1透明窗格与锁定mask不一致")
        for y in range(16):
            for x in range(16):
                now = image.getpixel((x, y))
                before = old.getpixel((x, y))
                if (x, y) in expected_panes:
                    if now[:3] != (102, 105, 106) or now[3] != PANE_ALPHA or before[3] != 0:
                        raise ValueError(f"{name}: pane像素不符{(x,y)}")
                    pane_pixels += 1
                else:
                    if now != before or now[3] != 255:
                        raise ValueError(f"{name}: 原框/角实体像素改变{(x,y)}")
                    frame_pixels += 1
        outputs[GENERATED / "tiles/reactor_window" / f"tile_{i:02d}.png"] = encode(image)
        atlas_x, atlas_y = (i % 8) * 16, (i // 8) * 16
        outputs.setdefault(GENERATED / "reactor_window_lead_ct.png", Image.new("RGBA", (128, 128)))
        outputs[GENERATED / "reactor_window_lead_ct.png"].paste(image, (atlas_x, atlas_y))
    outputs[GENERATED / "reactor_window_lead_ct.png"] = encode(outputs[GENERATED / "reactor_window_lead_ct.png"])
    return outputs, pane_pixels, frame_pixels

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="只核源、来源、导出覆盖与输出文件")
    args = parser.parse_args()
    outputs, pane_count, frame_count = build_images()
    if args.check:
        for path, data in outputs.items():
            if not path.is_file() or path.read_bytes() != data:
                raise SystemExit(f"CHECK_FAILED generated mismatch: {path.relative_to(HERE)}")
        print(f"CHECK_OK svg=64 atlas=128x128 contexts=47 fallback=17 pane_alpha64={pane_count} frame_pixels_same={frame_count} generated_files={len(outputs)}")
        return
    for path, data in outputs.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    print(f"EXPORTED svg=64 atlas=128x128 contexts=47 fallback=17 pane_alpha64={pane_count} frame_pixels_same={frame_count} generated_files={len(outputs)}")

if __name__ == "__main__":
    main()
