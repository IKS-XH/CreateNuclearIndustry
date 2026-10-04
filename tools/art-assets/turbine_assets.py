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
EVIDENCE_DIR = REPO / "build/reports/extension/EXT-B-TURBINE-ASSETS-01"
NS = "{http://www.w3.org/2000/svg}"
PALETTE = {
    "#28353A": "外轮廓", "#3E5058": "钢影", "#5B7078": "钢灰", "#788E94": "钢身",
    "#A3B7B8": "钢亮面", "#D1DFDA": "钢高光", "#715035": "铜影", "#A87942": "铜色",
    "#D2A359": "黄铜亮面", "#F08A36": "入口橙", "#A94F27": "橙影",
    "#85CDE0": "排汽浅蓝", "#397E99": "蓝影", "#202A30": "暗孔",
}

# 六件构建物品使用专属面纹；其余两张为机身端盖和轴承支座的复用贴片。
PAINT = {
    "turbine_casing": [
        "................", ".MMMMMMMMMMMMMM.", ".MCCCCMMMMCCCCM.", ".MccccMMMMccccM.",
        ".MMMMMMMMMMMMMM.", ".MLLLLLLLLLLLLM.", ".MDDDDDDDDDDDMM.", ".MMMMMMMMMMMMMM.",
        ".MMMMMMMMMMMMMM.", ".MDDDDDDDDDDDMM.", ".MLLLLLLLLLLLLM.", ".MMMMMMMMMMMMMM.",
        ".MCCCCMMMMCCCCM.", ".MccccMMMMccccM.", ".MMMMMMMMMMMMMM.", "................",
    ],
    "turbine_rotor": [
        "................", "....DDDDDD......", "...DLLLLLDD.....", "..DLLLLMLLLD....",
        ".DLLLDDDDLLLD...", ".DLLD....DLLLD..", ".DLLD.MMMM.DLLD.", ".DLLD.MHHM.DLLD.",
        ".DLLD.MHHM.DLLD.", ".DLLD.MMMM.DLLD.", ".DLLD....DLLLD..", ".DLLLDDDDLLLD...",
        "..DLLLLMLLLD....", "...DLLLLLDD.....", "....DDDDDD......", "................",
    ],
    "turbine_controller": [
        "................", ".MMMMMMMMMMMMMM.", ".MCCCCMMMMCCCCM.", ".MDDDDDDDDDDDMM.",
        ".MDDDDDDDDDDDMM.", ".MDD...HHH...DMM", ".MDD..HLLLH..DMM", ".MDD..HLLLH..DMM",
        ".MDD...HHH...DMM", ".MDDDDDDDDDDDMM.", ".MDD.LLL.LLL.DMM", ".MDD.LLL.LLL.DMM",
        ".MDDDDDDDDDDDMM.", ".MCCCCMMMMCCCCM.", ".MMMMMMMMMMMMMM.", "................",
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
}

INK = {
    ".": None, "D": "#28353A", "d": "#3E5058", "M": "#5B7078", "m": "#788E94",
    "L": "#A3B7B8", "H": "#D1DFDA", "C": "#A87942", "c": "#715035",
    "O": "#F08A36", "o": "#A94F27", "B": "#85CDE0", "b": "#397E99",
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
        rgb = tuple(int(color[i:i + 2], 16) for i in (1, 3, 5)) + (255,)
        for py in range(y, y + height):
            for px in range(x, x + width):
                pixels[px, py] = rgb
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
    """生成纹理样张与5/8/11节机身比例草图的离线总览。"""
    canvas = Image.new("RGBA", (1180, 930), "#17212A")
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.load_default(size=17)
    small = ImageFont.load_default(size=13)
    draw.text((28, 22), "EXT-B-TURBINE-ASSETS-01 / OFFLINE ART PREVIEW", font=font, fill="#E3ECE8")
    draw.text((28, 52), "SVG texture candidates; turbine views below are schematic proportions, not final Minecraft models", font=small, fill="#A6B9C2")

    names = list(images)
    for index, name in enumerate(names):
        col, row = index % 4, index // 4
        x, y = 28 + col * 285, 92 + row * 112
        draw.rectangle((x, y, x + 74, y + 74), fill="#34414A", outline="#6A7B82")
        tile = images[name].resize((64, 64), Image.Resampling.NEAREST)
        canvas.alpha_composite(tile, (x + 5, y + 5))
        draw.text((x + 86, y + 18), name, font=small, fill="#E3ECE8")
        draw.text((x + 86, y + 42), "16x16 RGBA / SVG", font=small, fill="#A6B9C2")

    # 以长度为唯一变化项，端盖、支座、铜箍和箭头遵循已批准外观方案。
    colors = {"steel": "#788E94", "shadow": "#3E5058", "light": "#A3B7B8", "brass": "#D2A359",
              "dark": "#28353A", "orange": "#F08A36", "blue": "#85CDE0"}
    for row, length in enumerate((5, 8, 11)):
        x0, y0 = 82, 360 + row * 175
        seg_w, body_h = 72, 76
        body_x, body_y, body_w = x0 + 96, y0 + 18, length * seg_w
        draw.text((28, y0 + 39), f"{length - 2} rotors / L={length}", font=font, fill="#E3ECE8")
        # 八棱筒体侧面分段，重复节段表达配置长度可扩展。
        draw.rectangle((body_x, body_y, body_x + body_w, body_y + body_h), fill=colors["steel"], outline=colors["dark"], width=4)
        draw.polygon([(body_x, body_y + 10), (body_x + 20, body_y - 8),
                      (body_x + body_w - 20, body_y - 8), (body_x + body_w, body_y + 10),
                      (body_x + body_w - 9, body_y + 24), (body_x + 9, body_y + 24)],
                     fill=colors["light"], outline=colors["dark"])
        draw.rectangle((body_x + 10, body_y + 27, body_x + body_w - 10, body_y + 44), fill=colors["shadow"])
        draw.rectangle((body_x + 10, body_y + 47, body_x + body_w - 10, body_y + 57), fill=colors["steel"])
        for i in range(1, length):
            seam = body_x + i * seg_w
            draw.rectangle((seam - 3, body_y + 1, seam + 3, body_y + body_h - 1), fill=colors["brass"])
        # 两端端盖与支座在各长度保持相同截面。
        octagon(draw, body_x - 25, body_y - 2, 34, body_h + 4, colors["steel"], colors["dark"])
        octagon(draw, body_x + body_w - 9, body_y - 2, 34, body_h + 4, colors["steel"], colors["dark"])
        for support_x in (body_x + 34, body_x + body_w - 44):
            draw.rectangle((support_x, body_y + body_h, support_x + 12, body_y + body_h + 19), fill=colors["shadow"])
            draw.rectangle((support_x - 10, body_y + body_h + 18, support_x + 22, body_y + body_h + 24), fill=colors["brass"])
        draw.polygon([(body_x + 53, body_y + 36), (body_x + 33, body_y + 29),
                      (body_x + 33, body_y + 33), (body_x + 19, body_y + 33),
                      (body_x + 19, body_y + 40), (body_x + 33, body_y + 40),
                      (body_x + 33, body_y + 44)], fill=colors["orange"])
        out_x = body_x + body_w - 64
        draw.polygon([(out_x + 45, body_y + 34), (out_x + 25, body_y + 27),
                      (out_x + 25, body_y + 31), (out_x + 11, body_y + 31),
                      (out_x + 11, body_y + 38), (out_x + 25, body_y + 38),
                      (out_x + 25, body_y + 42)], fill=colors["blue"])
    draw.text((28, 894), "No part boundaries, UVs, collision, or blockstates are defined; final models wait for C's frozen interface.", font=small, fill="#A6B9C2")
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
    sheet = preview(images)
    preview_path = EVIDENCE_DIR / "turbine-assets-preview.png"
    sheet.save(preview_path, format="PNG", optimize=False)
    for name, image in images.items():
        image.save(EVIDENCE_DIR / f"{name}.png", format="PNG", optimize=False)
    if args.install:
        TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
        for name, image in images.items():
            target = TEXTURE_DIR / f"{name}.png"
            image.save(target, format="PNG", optimize=False)
    (EVIDENCE_DIR / "asset-preview.json").write_text(json.dumps({
        "status": "preview-only" if not args.install else "textures-installed",
        "sources_created": created,
        "source_count": len(images),
        "texture_size": "16x16 RGBA",
        "preview": preview_path.relative_to(REPO).as_posix(),
        "runtime_texture_dir": TEXTURE_DIR.relative_to(REPO).as_posix(),
        "textures_installed": bool(args.install),
        "note": "三档比例为方案示意；最终模型与blockstate等待C冻结接口。",
    }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"PASS: {len(images)} SVG textures validated; preview={preview_path.relative_to(REPO)}; install={args.install}")
    if created:
        print("Initialized missing sources: " + ", ".join(created))


if __name__ == "__main__":
    main()
