"""汽轮机像素纹理导出器：SVG 是正式源稿，默认只生成离线预览。"""
from __future__ import annotations

import argparse
import json
import shutil
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

sys.dont_write_bytecode = True
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[1]
SOURCE_DIR = ROOT / "svg" / "block" / "turbine"
TEXTURE_DIR = REPO / "src/main/resources/assets/create_nuclear_industry/textures/block/turbine"
EVIDENCE_DIR = REPO / "build/reports/extension/EXT-B-TURBINE-01B-ASSETS"
NS = "{http://www.w3.org/2000/svg}"
PALETTE = {
    "#28353A": "细窄阴影", "#3E5058": "钢影", "#5B7078": "深钢灰", "#788E94": "钢灰",
    "#A3B7B8": "钢亮面", "#D1DFDA": "钢高光", "#715035": "铜影", "#A87942": "铜色",
    "#D2A359": "黄铜亮面", "#F08A36": "入口橙", "#A94F27": "橙影",
    "#85CDE0": "排汽浅蓝", "#397E99": "蓝影", "#202A30": "暗孔",
    "#74C6D088": "观察玻璃阴影", "#B7E5E888": "观察玻璃高光",
}

# 六件构建物品使用专属面纹；其余两张为机身端盖和轴承支座的复用贴片。
PAINT = {
    "turbine_casing": [
        "................", "..dddddddddddd..", ".dMMMMMMMMMMMMd.", ".dMLLLLMMMMMLLd.",
        ".dMLLLLMMMMMLLd.", ".dMMMMMMMMMMMMd.", ".dMMMMMMMMMMMMd.", ".dMMMMMMCCMMMMd.",
        ".dMMMMMMCCMMMMd.", ".dMMMMMMMMMMMMd.", ".dMMMMMMMMMMMMd.", ".dMLLLLMMMMMLLd.",
        ".dMLLLLMMMMMLLd.", ".dMMMMMMMMMMMMd.", "..dddddddddddd..", "................",
    ],
    "turbine_rotor": [
        "................", "....ddDDdd......", "...dDMLLLDd.....", "..dMLLLLMMMLd...",
        ".dMLLLLddLLMLd..", ".dMLLd..dLLMLd..", "dMLLd.MMM.dLLMd.", "dMLLd.MLHM.dLLMd",
        "dMLLd.MHLM.dLLMd", "dMLLd.MMM.dLLMd.", ".dMLLd..dLLMLd..", ".dMLLLLddLLMLd..",
        "..dMLLLLMMMLd...", "...dDMLLLDd.....", "....ddDDdd......", "................",
    ],
    "turbine_controller": [
        "................", "..dddddddddddd..", ".dMMMMMMMMMMMMd.", ".dMLLLLCCCCLLMd.",
        ".dM..........Md.", ".dM...HHHHH..Md.", ".dM..HLLLLLH.Md.", ".dM..HLLLLLH.Md.",
        ".dM..HLLLLLH.Md.", ".dM...HHHHH..Md.", ".dM...D...D..Md.", ".dM...D...D..Md.",
        ".dMLLLLCCCCLLMd.", ".dMMMMMMMMMMMMd.", "..dddddddddddd..", "................",
    ],
    "turbine_output_shaft": [
        "................", ".......DD.......", "......DMLD......", "......DMLD......",
        "......DMLD......", "......DCLC......", "......DCLC......", "......DMLD......",
        "......DMLD......", "......DMLD......", "......DCLC......", "......DCLC......",
        "......DMLD......", "......DMLD......", ".......DD.......", "................",
    ],
    "turbine_inlet": [
        "................", ".MMMMMMMMMMMMMM.", ".MCCCCMMMMCCCCM.", ".MDDDDDDDDDDDMM.",
        ".MDDDDDDDDDDDMM.", ".MDD......OODDMM", ".MDD.....OOOODMM", ".MDD.OOOOOOOOOMM",
        ".MDD.OOOOOOOOOMM", ".MDD.....OOOODMM", ".MDD......OODDMM", ".MDDDDDDDDDDDMM.",
        ".MCCCCMMMMCCCCM.", ".MMMMMMMMMMMMMM.", "................", "................",
    ],
    "turbine_exhaust": [
        "................", ".MMMMMMMMMMMMMM.", ".MCCCCMMMMCCCCM.", ".MDDDDDDDDDDDMM.",
        ".MDDDDDDDDDDDMM.", ".MDD......BBDDMM", ".MDD.....BBBBdMM", ".MDD.BBBBBBBBBMM",
        ".MDD.BBBBBBBBBMM", ".MDD.....BBBBdMM", ".MDD......BBDDMM", ".MDDDDDDDDDDDMM.",
        ".MCCCCMMMMCCCCM.", ".MMMMMMMMMMMMMM.", "................", "................",
    ],
    "turbine_casing_endcap": [
        "................", ".DDDDDDDDDDDDDD.", ".DMMMMMMMMMMMMD.", ".DMCCCCCCCCCCMD.",
        ".DMDDDDDDDDDDMD.", ".DMDLLLLLLLLDMD.", ".DMDMMMMMMMMDMD.", ".DMDMLLLMLLLDMD.",
        ".DMDMDDDDDDMDMD.", ".DMDMDDDDDDMDMD.", ".DMDMLLLMLLLDMD.", ".DMDMMMMMMMMDMD.",
        ".DMDDDDDDDDDDMD.", ".DMCCCCCCCCCCMD.", ".DMMMMMMMMMMMMD.", "................",
    ],
    "turbine_bearing_support": [
        "................", "....DDDDDDDD....", "...DMLLLLLLMD...", "..DMLDDDDDDMLD..",
        ".DMLDMMMMMMDMLD.", ".DMLDMMDDMMDMLD.", ".DMLDMMDDMMDMLD.", ".DMLDMMMMMMDMLD.",
        ".DMLDDDDDDMLD...", ".DMLLLLLLLLLMLD.", ".DMLCCCCCCCLMLD.", ".DMLcccccccLMLD.",
        ".DMLLLLLLLLLMLD.", "..DDDDDDDDDDDD..", "....MMMMMMMM....", "................",
    ],
    "turbine_window": [
        "................", "..CCCCCCCCCCCC..", ".CMMMMMMMMMMMMC.", ".CM..........MC.",
        ".CM..GGGGGG..MC.", ".CM.GggggggG.MC.", ".CM.GggggggG.MC.", ".CM.GggggggG.MC.",
        ".CM.GggggggG.MC.", ".CM..GGGGGG..MC.", ".CM..........MC.", ".CMMMMMMMMMMMMC.",
        "..CCCCCCCCCCCC..", "................", "................", "................",
    ],
    "turbine_window_glass": [
        "................", "................", "................", "....gggggggg....",
        "....gGGGGGGg....", "....gGGGGGGg....", "....gGGGGGGg....", "....gGGGGGGg....",
        "....gGGGGGGg....", "....gGGGGGGg....", "....gGGGGGGg....", "....gggggggg....",
        "................", "................", "................", "................",
    ],
    "turbine_casing_panel": [
        "mmmmmmmmmmmmmmmm", "mLLLLLLLLLLLLLLm", "mLMMMMMMMMMMMMmL", "mLMMMMMMMMMMMMmL",
        "mMMMMMMMMMMMMMMm", "mMMMMMMMMMMMMMMm", "mMMMMMMMMMMMMMMm", "mMMMMMMMMMMMMMMm",
        "mMMMMMMMMMMMMMMm", "mMMMMMMMMMMMMMMm", "mMMMMMMMMMMMMMMm", "mMMMMMMMMMMMMMMm",
        "mMMMMMMMMMMMMMMm", "mMMMMMMMMMMMMMMm", "mddddddddddddddm", "mmmmmmmmmmmmmmmm",
    ],
    "turbine_rotor_blade": [
        "mmmmmmmmmmmmmmmm", "mLLLLLLLLLLLLLLm", "mLMMMMMMMMMMMMmL", "mLMMMMMMMMMMMMmL",
        "mLMMMMMMMMMMMMmL", "mLMMMMMMMMMMMMmL", "mMMMMMMMMMMMMMMm", "mMMMMMMMMMMMMMMm",
        "mMMMMMMMMMMMMMMm", "mMMMMMMMMMMMMMMm", "mMMMMMMMMMMMMMMm", "mLMMMMMMMMMMMMmL",
        "mLMMMMMMMMMMMMmL", "mLMMMMMMMMMMMMmL", "mddddddddddddddm", "mmmmmmmmmmmmmmmm",
    ],
    "turbine_rotor_metal": [
        "MMMMMMMMMMMMMMMM", "MMLLLLLLLLLLLLMM", "MLMMMMMMMMMMMMMM", "MLMMMMMMMMMMMMMM",
        "MLMMMMMMMMMMMMMM", "MLMMMMMMMMMMMMMM", "MLMMMMMMMMMMMMMM", "MLMMMMMMMMMMMMMM",
        "MLMMMMMMMMMMMMMM", "MLMMMMMMMMMMMMMM", "MLMMMMMMMMMMMMMM", "MLMMMMMMMMMMMMMM",
        "MLMMMMMMMMMMMMMM", "MLMMMMMMMMMMMMMM", "MMdddddddddddddM", "MMMMMMMMMMMMMMMM",
    ],
}

INK = {
    ".": None, "D": "#28353A", "d": "#3E5058", "M": "#5B7078", "m": "#788E94",
    "L": "#A3B7B8", "H": "#D1DFDA", "C": "#A87942", "c": "#715035",
    "O": "#F08A36", "o": "#A94F27", "B": "#85CDE0", "b": "#397E99",
    "G": "#74C6D088", "g": "#B7E5E888",
}


def svg_text(rows: list[str]) -> str:
    """把16×16像素草图转换为严格整数矩形SVG。"""
    if len(rows) != 16 or any(len(row) != 16 for row in rows):
        raise ValueError("纹理草图必须严格为16×16")
    rects = []
    for y, row in enumerate(rows):
        for x, color in enumerate(row):
            value = INK[color]
            if value:
                rects.append(f'<rect x="{x}" y="{y}" width="1" height="1" fill="{value}"/>')
    return ('<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" '
            'viewBox="0 0 16 16" shape-rendering="crispEdges">\n' +
            "\n".join(rects) + "\n</svg>\n")


def initialize_sources() -> list[str]:
    """只创建缺少的SVG源稿，保留已经存在的手工修改。"""
    SOURCE_DIR.mkdir(parents=True, exist_ok=True)
    created = []
    for name, rows in PAINT.items():
        path = SOURCE_DIR / f"{name}.svg"
        if not path.exists():
            path.write_text(svg_text(rows), encoding="utf-8", newline="\n")
            created.append(path.name)
    return created


def render_svg(path: Path) -> Image.Image:
    """校验既有像素SVG子集并确定性渲染为16×16 RGBA。"""
    text = path.read_text(encoding="utf-8")
    if "<!DOCTYPE" in text.upper() or "<!ENTITY" in text.upper() or "<?" in text:
        raise ValueError(f"{path.name}: 不允许DTD、实体或处理指令")
    root = ET.fromstring(text)
    attrs = {"width": "16", "height": "16", "viewBox": "0 0 16 16", "shape-rendering": "crispEdges"}
    if root.tag != NS + "svg" or root.attrib != attrs:
        raise ValueError(f"{path.name}: SVG根节点或尺寸不符合16×16格式")
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pixels = image.load()
    for node in root:
        if node.tag != NS + "rect" or set(node.attrib) != {"x", "y", "width", "height", "fill"}:
            raise ValueError(f"{path.name}: 仅允许整数像素rect")
        x, y, width, height = (int(node.get(k, "-1")) for k in ("x", "y", "width", "height"))
        color = node.get("fill", "").upper()
        if color not in PALETTE or x < 0 or y < 0 or width < 1 or height < 1 or x + width > 16 or y + height > 16:
            raise ValueError(f"{path.name}: 像素越界或颜色不在汽轮机色板中")
        rgba = tuple(int(color[i:i + 2], 16) for i in (1, 3, 5))
        alpha = int(color[7:9], 16) if len(color) == 9 else 255
        rgba += (alpha,)
        for py in range(y, y + height):
            for px in range(x, x + width):
                pixels[px, py] = rgba
    if not list(root):
        raise ValueError(f"{path.name}: 源稿为空")
    return image


def octagon(draw: ImageDraw.ImageDraw, x: int, y: int, width: int, height: int, fill: str, outline: str) -> None:
    """绘制仅用于方案对照的八棱端盖示意，不生成游戏模型。"""
    cut = max(5, min(height // 4, 14))
    points = [(x + cut, y), (x + width - cut, y), (x + width, y + cut),
              (x + width, y + height - cut), (x + width - cut, y + height),
              (x + cut, y + height), (x, y + height - cut), (x, y + cut)]
    draw.polygon(points, fill=fill, outline=outline)


def preview(images: dict[str, Image.Image]) -> Image.Image:
    """生成独立纹理样张；真实机身外观由同源OBJ预览器绘制。"""
    canvas = Image.new("RGBA", (1180, 420), "#17212A")
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.load_default(size=17)
    small = ImageFont.load_default(size=13)
    draw.text((28, 22), "EXT-B-TURBINE-01B / PIXEL MATERIAL SHEET", font=font, fill="#E3ECE8")
    draw.text((28, 52), "16x16 RGBA textures from editable SVG sources; machine geometry is shown in the true-mesh preview.", font=small, fill="#A6B9C2")

    names = list(images)
    for index, name in enumerate(names):
        col, row = index % 5, index // 5
        x, y = 28 + col * 224, 96 + row * 108
        draw.rectangle((x, y, x + 74, y + 74), fill="#34414A", outline="#6A7B82")
        tile = images[name].resize((64, 64), Image.Resampling.NEAREST)
        canvas.alpha_composite(tile, (x + 5, y + 5))
        draw.text((x + 86, y + 18), name, font=small, fill="#E3ECE8")
        draw.text((x + 86, y + 42), "16x16 RGBA / SVG", font=small, fill="#A6B9C2")

    return canvas


def preview_nine_textures() -> Image.Image:
    """把机身、金属、窗口和端口九种关键像素纹理排成单页色板。"""
    selected = ("turbine_casing", "turbine_casing_panel", "turbine_casing_endcap",
                "turbine_rotor_blade", "turbine_rotor_metal", "turbine_window",
                "turbine_window_glass", "turbine_controller", "turbine_output_shaft")
    canvas = Image.new("RGBA", (1180, 460), "#17212A")
    draw = ImageDraw.Draw(canvas)
    title = ImageFont.load_default(size=20)
    label = ImageFont.load_default(size=16)
    draw.text((28, 20), "EXT-B-TURBINE-01B / NINE PIXEL TEXTURES", font=title, fill="#E3ECE8")
    for index, name in enumerate(selected):
        col, row = index % 3, index // 3
        x, y = 34 + col * 382, 66 + row * 128
        draw.rectangle((x, y, x + 86, y + 86), fill="#34414A", outline="#6A7B82")
        source = Image.open(TEXTURE_DIR / f"{name}.png").convert("RGBA")
        canvas.alpha_composite(source.resize((80, 80), Image.Resampling.NEAREST), (x + 3, y + 3))
        draw.text((x + 98, y + 28), name, font=label, fill="#E3ECE8")
        draw.text((x + 98, y + 52), "16x16 RGBA / SVG", font=label, fill="#A6B9C2")
    return canvas


def main() -> None:
    parser = argparse.ArgumentParser(description="导出汽轮机像素SVG候选并制作离线预览")
    parser.add_argument("--initialize-sources", action="store_true", help="只创建缺少的初始SVG，保留已有源稿")
    parser.add_argument("--install", action="store_true", help="将纹理PNG接入游戏资源路径（需项目经理确认验证窗口）")
    args = parser.parse_args()
    created = initialize_sources() if args.initialize_sources else []
    images = {path.stem: render_svg(path) for path in sorted(SOURCE_DIR.glob("*.svg"))}
    if set(images) != set(PAINT):
        raise ValueError("汽轮机SVG源稿集合必须与导出白名单完全一致")
    EVIDENCE_DIR.mkdir(parents=True, exist_ok=True)
    if args.install:
        TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
        for name, image in images.items():
            target = TEXTURE_DIR / f"{name}.png"
            image.save(target, format="PNG", optimize=False)
    texture_board = preview_nine_textures()
    texture_board_path = EVIDENCE_DIR / "turbine-01b-texture-sheet-3x3.png"
    texture_board.save(texture_board_path, format="PNG", optimize=False)
    (EVIDENCE_DIR / "asset-preview.json").write_text(json.dumps({
        "status": "preview-only" if not args.install else "textures-installed",
        "sources_created": created,
        "source_count": len(images),
        "texture_size": "16x16 RGBA",
        "preview": texture_board_path.relative_to(REPO).as_posix(),
        "runtime_texture_dir": TEXTURE_DIR.relative_to(REPO).as_posix(),
        "textures_installed": bool(args.install),
        "note": "01B网格与状态接口已冻结；纹理图板取自九张运行时PNG，几何预览见turbine-assets-preview.png。",
    }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"PASS: {len(images)} SVG textures validated; texture_sheet={texture_board_path.relative_to(REPO)}; install={args.install}")
    if created:
        print("Initialized missing sources: " + ", ".join(created))


if __name__ == "__main__":
    main()
