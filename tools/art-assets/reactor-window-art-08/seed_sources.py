"""从锁定R1上下文复制窗口SVG并添加低饱和铅灰窗格，不改写旧源。"""
from pathlib import Path
import hashlib
import json
import re
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
OLD = ROOT / "tools/art-assets/reactor-ct-r1"
SOURCE = HERE / "sources/reactor_window"
ALPHA = "0.25098039215686275"
PANE = "#66696A"
NS = "http://www.w3.org/2000/svg"
ET.register_namespace("", NS)
PAIRS = None

def transparent_pixels(mask):
    """按已锁定的八向mask得到内窗裁空位置；每格坐标单位为像素。"""
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

def runs(points):
    """把逐像素透明区压成每行整数横条，保持16×16边界。"""
    for y in range(16):
        xs = sorted(x for x, py in points if py == y)
        if not xs:
            continue
        start = previous = xs[0]
        for x in xs[1:] + [None]:
            if x is not None and x == previous + 1:
                previous = x
                continue
            yield start, y, previous - start + 1
            if x is not None:
                start = previous = x

def main():
    global PAIRS
    source_mapping = json.loads((OLD / "mapping.json").read_text(encoding="utf-8"))
    window = next(a for a in source_mapping["assets"] if a["name"] == "reactor_window")
    contexts = source_mapping["window_contexts"]
    if len(contexts) != 47 or len({c["mask"] for c in contexts}) != 47 or len({c["index"] for c in contexts}) != 47:
        raise SystemExit("旧R1窗上下文不符合已锁定的47项索引")
    native = {c["index"]: c["mask"] for c in contexts}
    SOURCE.mkdir(parents=True, exist_ok=True)
    hashes = {}
    for i in range(64):
        name = f"tile_{i:02d}.svg"
        original = OLD / "sources/reactor_window" / name
        data = original.read_bytes()
        hashes[name] = hashlib.sha256(data).hexdigest()
        root = ET.fromstring(data, parser=ET.XMLParser(target=ET.TreeBuilder(insert_comments=True)))
        mask = native.get(i, 0)
        points = transparent_pixels(mask)
        children = list(root)
        insert_at = 0
        while insert_at < len(children) and children[insert_at].tag is ET.Comment:
            old_text = children[insert_at].text or ""
            children[insert_at].text = f" ART-REACTOR-08候选：原生索引{i:02d}，mask={mask}；透明窗格填充铅灰{PANE}，alpha64/255；原框/角矩形保持。 "
            insert_at += 1
        if insert_at == 0:
            root.insert(0, ET.Comment(f" ART-REACTOR-08候选：原生索引{i:02d}，mask={mask}；透明窗格填充铅灰{PANE}，alpha64/255；原框/角矩形保持。 "))
            insert_at = 1
        panes = []
        for x, y, width in runs(points):
            rect = ET.Element(f"{{{NS}}}rect", {
                "x": str(x), "y": str(y), "width": str(width), "height": "1",
                "fill": PANE, "fill-opacity": ALPHA,
            })
            panes.append(rect)
        for offset, rect in enumerate(panes):
            root.insert(insert_at + offset, rect)
        ET.indent(root, space="  ")
        ET.ElementTree(root).write(SOURCE / name, encoding="unicode", xml_declaration=False)
        with (SOURCE / name).open("a", encoding="utf-8", newline="\n") as stream:
            stream.write("\n")
    palette = {
        "reactor_window": {
            "outline": "#28353A", "frame_light": "#A3B7B8", "frame_mid": "#788E94",
            "frame_dark": "#3E5058", "frame_shadow": "#5B7078", "copper_light": "#D2A359",
            "copper_shadow": "#715035", "pane_lead": PANE
        }
    }
    (HERE / "palette.json").write_text(json.dumps(palette, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    mapping = {
        "task": "ART-REACTOR-08", "stage": "候选未安装", "source_of_truth": "纯SVG rect，窗格的fill-opacity在SVG中明确为64/255",
        "tile_size": 16, "atlas_size": [128, 128], "ct_type": window["ct_type"],
        "source_sprite": window["original_sprite"], "target_sprite": window["target_sprite"],
        "source_tiles": 64, "window_contexts": contexts,
        "fallback_indices": source_mapping["window_fallback_indices"],
        "pane": {"color": PANE, "alpha": 64, "fill_opacity": ALPHA, "scope": "只覆盖47个原生上下文中的原透明窗格"},
        "preservation": "原R1窗口SVG中全部框/角/实体像素逐像素不变；17个fallback索引保持原不透明形状",
        "old_source_sha256": hashes,
        "old_atlas": "tools/art-assets/reactor-ct-r1/generated/reactor_window_ct.png",
        "candidate_atlas": "generated/reactor_window_lead_ct.png",
        "formal_target_after_approval": "src/main/resources/assets/create_nuclear_industry/textures/block/reactor_ct/reactor_window.png"
    }
    (HERE / "mapping.json").write_text(json.dumps(mapping, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"SEEDED window_svg=64 contexts=47 fallback=17 alpha=64/255 source_hashes=64")

if __name__ == "__main__":
    main()
