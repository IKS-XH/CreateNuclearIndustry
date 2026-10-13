"""ART-LOGO-11专属导出器：从整数矩形SVG确定性生成PNG并核对候选身份。

仅处理本批工具目录与build/reports/art/ART-LOGO-11，不安装正式资源。
所有图形由SVG输入提供；导出器不含Logo绘图坐标，放大固定使用最近邻。
"""

import argparse
import hashlib
import json
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[2]
REPORTS = REPO / "build/reports/art/ART-LOGO-11"
SVG_NS = "{http://www.w3.org/2000/svg}"
MAPPING = json.loads((ROOT / "resource-map.json").read_text(encoding="utf-8"))


def sha256(path):
    """返回文件SHA256，供SVG、候选与后续安装/打包逐字核对。"""
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read_source():
    """检查32px SVG合同并按半开像素矩形绘制RGBA图，保留0/255透明度。

    拒绝路径、文字、变换、渐变、外部引用和非整数坐标；两像素网格保证
    16px最近邻缩图具有一一对应的逻辑像素，不依赖浏览器抗锯齿。
    """
    source_path = ROOT / MAPPING["source"]
    tree = ET.parse(source_path)
    root = tree.getroot()
    if root.tag != SVG_NS + "svg":
        raise ValueError("SVG命名空间不符")
    expected = {"width": "32", "height": "32", "viewBox": "0 0 32 32", "shape-rendering": "crispEdges"}
    if root.attrib != expected:
        raise ValueError("SVG画布或根属性不符合固定合同")
    canvas = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)
    colors = set()
    rectangles = 0
    for node in root:
        if node.tag in (SVG_NS + "title", SVG_NS + "desc"):
            continue
        if node.tag != SVG_NS + "rect" or set(node.attrib) != {"x", "y", "width", "height", "fill"}:
            raise ValueError("只允许直属像素rect及说明节点")
        x, y, width, height = (int(node.attrib[key]) for key in ("x", "y", "width", "height"))
        if any(str(value) != node.attrib[key] for value, key in zip((x, y, width, height), ("x", "y", "width", "height"))):
            raise ValueError("坐标必须是十进制整数")
        if min(x, y) < 0 or min(width, height) <= 0 or x + width > 32 or y + height > 32:
            raise ValueError("矩形越界或面积为零")
        if any(value % 2 for value in (x, y, width, height)):
            raise ValueError("本批全部几何必须落在两像素网格")
        color = node.attrib["fill"]
        if not re.fullmatch(r"#[0-9A-F]{6}", color):
            raise ValueError("颜色必须为固定不透明#RRGGBB")
        colors.add(color)
        rgb = tuple(int(color[index:index + 2], 16) for index in (1, 3, 5))
        draw.rectangle((x, y, x + width - 1, y + height - 1), fill=(*rgb, 255))
        rectangles += 1
    if len(colors) > 12:
        raise ValueError("色板超过12色")
    return canvas, {"rectangles": rectangles, "palette": sorted(colors), "source_sha256": sha256(source_path)}


def save_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def make_sheet(canvas):
    """用同源PNG生成离线检查图：原生尺寸、明暗背景及标签模拟。

    下方标签尺寸只提供离线视觉构图参考，不代表Minecraft加载或GUI截图。
    """
    sheet = Image.new("RGB", (960, 520), "#ECEFF0")
    draw = ImageDraw.Draw(sheet)
    draw.text((26, 20), "ART-LOGO-11 / ORIGINAL SVG / OFFLINE PREVIEW", fill="#26313A")
    for index, (background, label) in enumerate((("#F8F6EF", "LIGHT"), ("#202930", "DARK"), (None, "TRANSPARENT GRID"))):
        left = 24 + index * 312
        draw.text((left, 54), label, fill="#26313A")
        draw.rectangle((left, 76, left + 287, 363), fill=background or "#D0D5D8")
        if background is None:
            for y in range(76, 364, 16):
                for x in range(left, left + 288, 16):
                    color = "#E8ECEE" if ((x-left)//16 + (y-76)//16) % 2 else "#CED5D8"
                    draw.rectangle((x, y, x + 15, y + 15), fill=color)
        sheet.paste(canvas.resize((256, 256), Image.Resampling.NEAREST), (left + 16, 92), canvas.resize((256, 256), Image.Resampling.NEAREST))
    for index, size in enumerate((16, 24, 32)):
        left = 26 + index * 118
        draw.text((left, 396), f"NATIVE {size}px", fill="#26313A")
        icon = canvas.resize((size, size), Image.Resampling.NEAREST)
        sheet.paste(icon, (left + 16, 423), icon)
    draw.text((408, 396), "CREATIVE TAB MOCKUP - NOT A GAME CAPTURE", fill="#26313A")
    for index, background in enumerate(("#C6C6C6", "#373C43")):
        left = 412 + index * 112
        draw.rectangle((left, 420, left + 28, 451), fill="#232A30")
        draw.rectangle((left + 1, 421, left + 27, 450), fill=background)
        draw.line((left + 1, 421, left + 26, 421), fill="#FFFFFF")
        draw.line((left + 1, 422, left + 1, 449), fill="#FFFFFF")
        icon = canvas.resize((16, 16), Image.Resampling.NEAREST)
        sheet.paste(icon, (left + 6, 428), icon)
    draw.text((26, 484), "32px source -> nearest-neighbor 32 / 128 / 256 transparent PNG. No text in icon.", fill="#465660")
    output = ROOT / "generated/preview-sheet.png"
    sheet.save(output, optimize=False, compress_level=9)


def export():
    """导出本批三种PNG与32px候选纹理，不触及src/main/resources。"""
    canvas, info = read_source()
    generated = ROOT / "generated"
    generated.mkdir(parents=True, exist_ok=True)
    for size in MAPPING["exports"]:
        canvas.resize((size, size), Image.Resampling.NEAREST).save(generated / f"mod_logo_{size}.png", optimize=False, compress_level=9)
    texture = ROOT / MAPPING["candidate_root"] / MAPPING["texture"]
    texture.parent.mkdir(parents=True, exist_ok=True)
    texture.write_bytes((generated / "mod_logo_32.png").read_bytes())
    make_sheet(canvas)
    print(f"EXPORT SVG={info['source_sha256']} rects={info['rectangles']} palette={len(info['palette'])}")
    print("EXPORT PNG=32,128,256 RGBA alpha=0/255 nearest-neighbor candidate_texture=32")
    print("EXPORT formal_install=0 gradle=0 minecraft=0")


def check():
    """读取现成输出并核对像素同源、尺寸、模型引用与正式目标未安装。"""
    canvas, info = read_source()
    checks = []
    def require(name, condition):
        checks.append({"name": name, "passed": bool(condition)})
        if not condition:
            raise AssertionError(name)
    require("SVG全部几何为2px整数网格且有限色板", info["rectangles"] > 0 and len(info["palette"]) <= 12)
    for size in MAPPING["exports"]:
        path = ROOT / f"generated/mod_logo_{size}.png"
        with Image.open(path) as actual:
            require(f"{size}px PNG为RGBA且尺寸正确", actual.mode == "RGBA" and actual.size == (size, size))
            require(f"{size}px PNG逐像素来自SVG最近邻", actual.tobytes() == canvas.resize((size, size), Image.Resampling.NEAREST).tobytes())
            require(f"{size}px透明度只有0和255", set(actual.getchannel("A").getdata()) == {0, 255})
    require("16px逻辑像素缩放可无损回到32px", canvas.resize((16, 16), Image.Resampling.NEAREST).resize((32, 32), Image.Resampling.NEAREST).tobytes() == canvas.tobytes())
    candidate = ROOT / MAPPING["candidate_root"]
    texture = candidate / MAPPING["texture"]
    model = candidate / MAPPING["model"]
    require("候选32px贴图与独立导出逐字一致", sha256(texture) == sha256(ROOT / "generated/mod_logo_32.png"))
    expected_model = {"parent": "minecraft:item/generated", "textures": {"layer0": "create_nuclear_industry:item/mod_logo"}}
    require("候选模型只使用原生item/generated和单层同名贴图", json.loads(model.read_text(encoding="utf-8")) == expected_model)
    html = (ROOT / "index.html").read_text(encoding="utf-8")
    local_references = re.findall(r'(?:href|src)="([^"]+)"', html)
    require("离线预览全部引用解析为本目录实际文件", bool(local_references) and all((ROOT / reference).is_file() for reference in local_references))
    require("两拟正式资源尚不存在", all(not (REPO / MAPPING["installed_root"] / MAPPING[key]).exists() for key in ("model", "texture")))
    report = {"task": "ART-LOGO-11", "phase": "candidate-only", "checks": checks, "svg": info,
              "nontransparent_pixels": sum(pixel[3] == 255 for pixel in canvas.getdata()),
              "alpha_values": [0, 255], "minecraft_visual_acceptance": False,
              "formal_install": False, "gradle_run": False}
    save_json(REPORTS / "phase1-verification.json", report)
    paths = sorted(path for path in ROOT.rglob("*") if path.is_file())
    manifest = {"task": "ART-LOGO-11", "phase": "candidate-only", "files": [
        {"path": path.relative_to(REPO).as_posix(), "bytes": path.stat().st_size, "sha256": sha256(path)} for path in paths]}
    save_json(REPORTS / "phase1-manifest.json", manifest)
    print(f"CHECK PASS={len(checks)}/{len(checks)} SVG_rects={info['rectangles']} palette={len(info['palette'])}")
    print(f"CHECK manifest_files={len(paths)} SVG->PNG->candidate=identical formal_targets=absent")
    print("CHECK offline_only=1 minecraft_visual_acceptance=0 gradle=0")


def main():
    parser = argparse.ArgumentParser(description="ART-LOGO-11同源导出与有限校验")
    parser.add_argument("action", choices=("export", "check"))
    args = parser.parse_args()
    {"export": export, "check": check}[args.action]()


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(f"FAIL {type(error).__name__}: {error}", file=sys.stderr)
        raise
