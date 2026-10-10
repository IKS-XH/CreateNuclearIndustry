"""04专用资产工具：只读取冻结整数SVG，生成四个普通流体图集及元数据。"""
from pathlib import Path
from io import BytesIO
import argparse
import hashlib
import json
import re
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[3]
HOME = Path(__file__).resolve().parent
GENERATED = HOME / "generated"
SOURCE = ROOT / "tools/art-assets/reactor-animation/sources"


def read_mapping(path=HOME / "mapping.json"):
    """冻结名称、来源、规格和目标；任何错误必须在创建输出前被拒绝。"""
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    expected = {(kind, flow) for kind in ("cold", "hot") for flow in (False, True)}
    if len(data["sprites"]) != 4 or {(s["kind"], s["flow"]) for s in data["sprites"]} != expected:
        raise ValueError("必须为冷热各still/flow四项，不能增加安装目标")
    for s in data["sprites"]:
        name = ("hot_" if s["kind"] == "hot" else "") + "compound_coolant_" + ("flow" if s["flow"] else "still")
        size = 32 if s["flow"] else 16
        if s["name"] != name or s["frame_size"] != size:
            raise ValueError("图集名称或16/32帧规格不符合冻结合同")
        targets = [f"src/main/resources/assets/create_nuclear_industry/textures/{folder}/{name}.png" for folder in ("block", "fluid")]
        if s["targets"] != targets:
            raise ValueError("目标路径必须精确为已授权block/fluid双副本")
    for kind in ("cold", "hot"):
        rows = data["sources"][kind]
        expected_paths = [f"tools/art-assets/reactor-animation/sources/coolant_{kind}/frame_{i:02d}.svg" for i in range(8)]
        if len(rows) != 8 or [r["path"] for r in rows] != expected_paths:
            raise ValueError("SVG来源路径或八帧数量不符合冻结合同")
        for row in rows:
            p = ROOT / row["path"]
            if hashlib.sha256(p.read_bytes()).hexdigest() != row["sha256"]:
                raise ValueError(f"冻结SVG字节已变化：{row['path']}")
    return data


def svg_frame(path):
    """整数rect按SVG覆盖顺序栅格化；不调用旧pipeline，不修改共享renderer。"""
    root = ET.parse(path).getroot()
    if root.tag != "{http://www.w3.org/2000/svg}svg" or root.attrib.get("width") != "16" or root.attrib.get("height") != "16" or root.attrib.get("viewBox") != "0 0 16 16":
        raise ValueError("SVG必须为16×16整数rect源稿")
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    for node in root:
        if node.tag != "{http://www.w3.org/2000/svg}rect" or set(node.attrib) != {"x", "y", "width", "height", "fill"}:
            raise ValueError("SVG仅允许无透明度的整数rect")
        values = [node.attrib[k] for k in ("x", "y", "width", "height")]
        if any(not re.fullmatch(r"\d+", v) for v in values):
            raise ValueError("SVG rect必须为整数坐标")
        x, y, w, h = map(int, values)
        if w < 1 or h < 1 or x + w > 16 or y + h > 16 or not re.fullmatch(r"#[0-9A-Fa-f]{6}", node.attrib["fill"]):
            raise ValueError("SVG rect范围或RGB颜色非法")
        draw.rectangle((x, y, x + w - 1, y + h - 1), fill=node.attrib["fill"])
    if image.getchannel("A").getextrema() != (255, 255):
        raise ValueError("SVG必须全覆盖alpha255")
    return image


def inputs(data):
    return {kind: [svg_frame(ROOT / row["path"]) for row in data["sources"][kind]] for kind in ("cold", "hot")}


def metadata(size):
    return {"animation": {"width": size, "height": size, "frametime": 2, "frames": list(range(8)), "interpolate": True}}


def candidates(data, frames):
    """flow的四象限直接复制16像素帧，保留原生quarter UV的像素密度。"""
    result = {}
    for s in data["sprites"]:
        size = s["frame_size"]
        sheet = Image.new("RGBA", (size, size * 8))
        for i, frame in enumerate(frames[s["kind"]]):
            for y in range(0, size, 16):
                for x in range(0, size, 16):
                    sheet.paste(frame, (x, size * i + y))
        out = BytesIO()
        sheet.save(out, format="PNG", compress_level=9, optimize=False)
        result[s["name"] + ".png"] = out.getvalue()
        result[s["name"] + ".png.mcmeta"] = (json.dumps(metadata(size), indent=2) + "\n").encode("utf-8")
    return result


def generate(data):
    products = candidates(data, inputs(data))
    GENERATED.mkdir(parents=True, exist_ok=True)
    for name, raw in products.items():
        (GENERATED / name).write_bytes(raw)
    return {"generated_files": len(products)}


def check(data, installed=False):
    """读取实际输出验证来源/循环/四象限，不以再写一遍生成物代替检查。"""
    frames = inputs(data)
    registration = data["registration_source"]
    assert registration["path"] == "src/main/java/com/iksxh/create_nuclear_industry/content/ModFluids.java", "注册来源路径错误"
    assert hashlib.sha256((ROOT / registration["path"]).read_bytes()).hexdigest() == registration["sha256"], "实际ModFluids引用已变化"
    for kind in ("cold", "hot"):
        internal = Image.open(ROOT / f"tools/art-assets/reactor-animation/generated/textures/coolant_{kind}.png").convert("RGBA")
        assert internal.size == (16, 128), "冻结内部sheet规格错误"
        for i, frame in enumerate(frames[kind]):
            assert frame.tobytes() == internal.crop((0, i * 16, 16, (i + 1) * 16)).tobytes(), "SVG与内部sheet像素不同"
            # 每帧向下移动2像素，最后一帧同样环绕到第0帧。
            shifted = Image.new("RGBA", (16, 16))
            shifted.paste(frame.crop((0, 14, 16, 16)), (0, 0))
            shifted.paste(frame.crop((0, 0, 16, 14)), (0, 2))
            assert shifted.tobytes() == frames[kind][(i + 1) % 8].tobytes(), "2px周期或7→0错误"
    for s in data["sprites"]:
        size = s["frame_size"]
        p = GENERATED / (s["name"] + ".png")
        sheet = Image.open(p)
        assert sheet.mode == "RGBA" and sheet.size == (size, size * 8), "实际PNG尺寸/格式错误"
        assert sheet.getchannel("A").getextrema() == (255, 255), "实际PNG alpha必须全255"
        for i, frame in enumerate(frames[s["kind"]]):
            for y in range(0, size, 16):
                for x in range(0, size, 16):
                    assert sheet.crop((x, i * size + y, x + 16, i * size + y + 16)).tobytes() == frame.tobytes(), "输出帧或flow象限不同"
        assert json.loads(Path(str(p) + ".mcmeta").read_text()) == metadata(size), "元数据切帧错误"
        if installed:
            for target in s["targets"]:
                for suffix in ("", ".mcmeta"):
                    assert (ROOT / (target + suffix)).read_bytes() == Path(str(p) + suffix).read_bytes(), "双安装路径与生成物不同"
    return {"svg_frames": 16, "sprites": 4, "alpha": 255, "phase_shift_px": 2, "wrap_7_to_0": True, "flow_quadrants": "all exact", "installed_checked": installed}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=("generate", "check"))
    parser.add_argument("--mapping", type=Path, default=HOME / "mapping.json")
    parser.add_argument("--installed", action="store_true")
    args = parser.parse_args()
    data = read_mapping(args.mapping)
    print(json.dumps(generate(data) if args.action == "generate" else check(data, args.installed), ensure_ascii=False))


if __name__ == "__main__":
    main()
