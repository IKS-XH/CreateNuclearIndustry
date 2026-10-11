"""锅炉蒸汽低分辨率帧的原创SVG、候选导出与定向检查。"""
from __future__ import annotations

import json
import math
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
SOURCES = ROOT / "sources"
GENERATED = ROOT / "generated"
MATERIALS = ("normal", "supercritical")
KINDS = ("still", "flow")
FRAMES = 8
SIZE = 16
PALETTE = {
    "normal": {1: "#A4A9A8", 2: "#D5D7D3", 3: "#F7F4EC"},
    "supercritical": {1: "#76B7BD", 2: "#B1E1E3", 3: "#F0FFFA"},
}
STILL_MOTION = ((0, 0), (1, 0), (1, 1), (0, 1), (-1, 1), (-1, 0), (-1, -1), (0, -1))
FLOW_WOBBLE = (0, 0, 1, 1, 0, -1, -1, 0)


def source_path(material: str, kind: str, index: int) -> Path:
    return SOURCES / material / kind / f"frame_{index:02d}.svg"


def centers_for(kind: str, index: int) -> list[tuple[int, int, float, float]]:
    """返回同一帧的云团中心和横纵半径，坐标单位为像素。"""
    if kind == "still":
        dx, dy = STILL_MOTION[index]
        base = ((4, 10, 3.0, 2.7), (7, 9, 3.6, 3.0), (10, 8, 2.8, 2.8),
                (6, 5, 2.5, 2.3), (10, 4, 2.0, 2.0))
        return [(x + dx, y + dy, rx, ry) for x, y, rx, ry in base]
    lift = 2 * index
    wobble = FLOW_WOBBLE[index]
    base = ((4, 14, 3.0, 2.7), (6, 10, 3.2, 3.2),
            (9, 6, 3.2, 3.2), (12, 1, 2.8, 2.7))
    return [(x + wobble, (y - lift) % SIZE, rx, ry) for x, y, rx, ry in base]


def make_levels(kind: str, index: int) -> list[list[int]]:
    """用整数像素绘出连续云团；同坐标遮罩供两种蒸汽逐像素混色。"""
    centers = centers_for(kind, index)
    result = [[0 for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            best = 99.0
            for cx, cy, rx, ry in centers:
                dy = abs(y - cy)
                if kind == "flow":
                    dy = min(dy, SIZE - dy)
                d = math.sqrt(((x - cx) / rx) ** 2 + ((dy) / ry) ** 2)
                best = min(best, d)
            if best <= 1.0:
                level = 1 if best > 0.72 else 2 if best > 0.35 else 3
                # 高光沿云团弧面断续排列，避免形成整块平色。
                if level == 2 and (x + 2 * y + index) % 7 == 0:
                    level = 3
                result[y][x] = level
    return result


def write_sources() -> None:
    """将本批原创像素云纹写为四组、每组八张SVG帧。"""
    for material in MATERIALS:
        for kind in KINDS:
            for index in range(FRAMES):
                levels = make_levels(kind, index)
                colors = PALETTE[material]
                lines = [
                    '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges">',
                ]
                for y, row in enumerate(levels):
                    for x, level in enumerate(row):
                        if level:
                            lines.append(
                                f'<rect x="{x}" y="{y}" width="1" height="1" fill="{colors[level]}"/>'
                            )
                lines.append("</svg>")
                source_path(material, kind, index).write_text(
                    "\n".join(lines) + "\n", encoding="utf-8", newline="\n"
                )


def read_svg(path: Path, material: str) -> list[list[int]]:
    """读取并约束SVG为16×16整数rect，透明背景只由未覆盖像素表示。"""
    root = ET.parse(path).getroot()
    if root.attrib.get("width") != "16" or root.attrib.get("height") != "16":
        raise ValueError(f"{path}: 画布必须为16×16")
    if root.attrib.get("viewBox") != "0 0 16 16" or root.attrib.get("shape-rendering") != "crispEdges":
        raise ValueError(f"{path}: viewBox或像素渲染约束错误")
    colors = {color: level for level, color in PALETTE[material].items()}
    grid = [[0 for _ in range(SIZE)] for _ in range(SIZE)]
    for node in root:
        if not node.tag.endswith("rect"):
            raise ValueError(f"{path}: 仅允许rect元素")
        x, y, width, height = (node.attrib.get(key, "") for key in ("x", "y", "width", "height"))
        if not all(value.isdigit() for value in (x, y, width, height)):
            raise ValueError(f"{path}: rect坐标和尺寸须为非负整数")
        x, y, width, height = map(int, (x, y, width, height))
        if width != 1 or height != 1 or x >= SIZE or y >= SIZE:
            raise ValueError(f"{path}: 每个rect必须是画布内单像素")
        if "opacity" in node.attrib or "fill-opacity" in node.attrib:
            raise ValueError(f"{path}: 液体帧alpha只能是0或255")
        color = node.attrib.get("fill")
        if color not in colors:
            raise ValueError(f"{path}: 非批准色值 {color}")
        if grid[y][x]:
            raise ValueError(f"{path}: 像素重复绘制")
        grid[y][x] = colors[color]
    if not any(any(row) for row in grid):
        raise ValueError(f"{path}: 不允许空帧")
    return grid


def image_from_grid(grid: list[list[int]], material: str) -> Image.Image:
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    pixels = image.load()
    for y, row in enumerate(grid):
        for x, level in enumerate(row):
            if level:
                color = PALETTE[material][level]
                pixels[x, y] = (int(color[1:3], 16), int(color[3:5], 16), int(color[5:7], 16), 255)
    return image


def save_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG", optimize=False, compress_level=9)


def build_outputs() -> None:
    """导出四张8帧流体atlas及32张独立内部16×16帧。"""
    for material in MATERIALS:
        for kind in KINDS:
            images = []
            for index in range(FRAMES):
                grid = read_svg(source_path(material, kind, index), material)
                images.append(image_from_grid(grid, material))
                internal = GENERATED / "internal" / "textures" / "boiler_steam" / material / f"{kind}_{index}.png"
                save_png(images[-1], internal)
            sheet = Image.new("RGBA", (SIZE, SIZE * FRAMES), (0, 0, 0, 0))
            for index, image in enumerate(images):
                sheet.paste(image, (0, index * SIZE))
            atlas = GENERATED / "atlas" / f"{'steam' if material == 'normal' else 'supercritical_steam'}_{kind}.png"
            save_png(sheet, atlas)
            meta = {
                "animation": {
                    "width": SIZE,
                    "height": SIZE,
                    "frametime": 2,
                    "interpolate": True,
                    "frames": list(range(FRAMES)),
                }
            }
            atlas.with_suffix(".png.mcmeta").write_text(
                json.dumps(meta, indent=2) + "\n", encoding="utf-8", newline="\n"
            )


def check_outputs() -> None:
    """核四组源帧、同位遮罩、atlas像素、mcmeta与32张内部候选。"""
    expected_meta = {
        "animation": {
            "width": 16,
            "height": 16,
            "frametime": 2,
            "interpolate": True,
            "frames": list(range(8)),
        }
    }
    mapping = json.loads((ROOT / "mapping.json").read_text(encoding="utf-8"))
    expected_sprites = [
        ("normal", "still", "create_nuclear_industry:fluid/steam_still"),
        ("normal", "flow", "create_nuclear_industry:fluid/steam_flow"),
        ("supercritical", "still", "create_nuclear_industry:fluid/supercritical_steam_still"),
        ("supercritical", "flow", "create_nuclear_industry:fluid/supercritical_steam_flow"),
    ]
    actual_sprites = [(item["material"], item["kind"], item["registered_sprite"]) for item in mapping["sprites"]]
    if actual_sprites != expected_sprites:
        raise AssertionError("mapping.json中的四个FluidType sprite引用与核准映射不符")
    if mapping["internal_frames"]["consumer_path_pattern"] != "textures/boiler_steam/{normal,supercritical}/{still,flow}_0..7.png":
        raise AssertionError("内部帧消费路径映射错误")
    if not (ROOT / "preview" / "index.html").is_file() or not (ROOT / "preview" / "contact-sheet.png").is_file():
        raise AssertionError("预览入口或帧总览缺失")
    for kind in KINDS:
        signatures = {
            tuple(level for row in read_svg(source_path("normal", kind, index), "normal") for level in row)
            for index in range(FRAMES)
        }
        if len(signatures) != FRAMES:
            raise AssertionError(f"{kind}: 八帧中存在重复帧")
    source_count = 0
    internal_count = 0
    for kind in KINDS:
        for index in range(FRAMES):
            grids = {material: read_svg(source_path(material, kind, index), material) for material in MATERIALS}
            if grids["normal"] != grids["supercritical"]:
                raise AssertionError(f"{kind} frame {index}: 两类型遮罩/亮度等级不一致")
            source_count += 2
    for material in MATERIALS:
        for kind in KINDS:
            atlas = GENERATED / "atlas" / f"{'steam' if material == 'normal' else 'supercritical_steam'}_{kind}.png"
            sheet = Image.open(atlas).convert("RGBA")
            if sheet.size != (16, 128):
                raise AssertionError(f"{atlas}: 尺寸不是16×128")
            for index in range(FRAMES):
                grid = read_svg(source_path(material, kind, index), material)
                expected = image_from_grid(grid, material)
                actual = sheet.crop((0, index * SIZE, SIZE, (index + 1) * SIZE))
                if list(actual.getdata()) != list(expected.getdata()):
                    raise AssertionError(f"{atlas}: 第{index}帧与SVG不一致")
                if set(actual.getchannel("A").getdata()) - {0, 255}:
                    raise AssertionError(f"{atlas}: alpha出现0/255以外值")
                internal = GENERATED / "internal" / "textures" / "boiler_steam" / material / f"{kind}_{index}.png"
                separate = Image.open(internal).convert("RGBA")
                if separate.size != (16, 16) or list(separate.getdata()) != list(expected.getdata()):
                    raise AssertionError(f"{internal}: 内部帧与同源SVG不一致")
                internal_count += 1
            meta = json.loads(atlas.with_suffix(".png.mcmeta").read_text(encoding="utf-8"))
            if meta != expected_meta:
                raise AssertionError(f"{atlas}: mcmeta不符合八帧2tick循环")
    if source_count != 32 or internal_count != 32:
        raise AssertionError("源SVG或内部PNG数量不等于32")
    print("PASS: 32/32 SVG有效；同类型配对遮罩/亮度等级16/16一致")
    print("PASS: 4/4 atlas为16x128；32/32 atlas帧与源SVG逐像素一致")
    print("PASS: 4/4 mcmeta为8帧、2tick、interpolate=true")
    print("PASS: 32/32内部帧为16x16并与同源SVG逐像素一致")
    print("PASS: 所有流体与内部纹理alpha仅为0或255")


def main() -> None:
    if len(sys.argv) != 2 or sys.argv[1] not in {"author", "build", "check"}:
        raise SystemExit("用法：pipeline.py author|build|check")
    if sys.argv[1] == "author":
        write_sources()
        print("已写出32张16×16原创SVG帧")
    elif sys.argv[1] == "build":
        build_outputs()
        print("已导出4张16×128 atlas、4份mcmeta和32张内部帧")
    else:
        check_outputs()


if __name__ == "__main__":
    main()



