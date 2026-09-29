"""严格像素 SVG 渲染器及 EXT-ART-02 离线导出入口。

默认输入为 manifest 指定 SVG 和色板；默认只输出 generated 和预览。
显式 --install 通过固定白名单接入原有 51 个游戏路径，由 pipeline 管理。
受限 SVG 使用整数 rect 和色板内的 #RRGGBB，按文档顺序覆盖像素。
任何不支持的元素、属性或非空文本均报错；全部源稿验证后才开始写出。
本工具没有服务端/客户端运行行为，不属于 Minecraft 资源加载器。
"""
from __future__ import annotations

import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

from PIL import Image

ROOT = Path(__file__).resolve().parent
NS = "{http://www.w3.org/2000/svg}"
HEX = re.compile(r"#[0-9A-Fa-f]{6}\Z")
INTEGER = re.compile(r"(?:0|[1-9][0-9]*)\Z")


def read_palette() -> dict[str, dict[str, str]]:
    """读取每张纹理的有限色板；色值不得包含 alpha、别名或外部引用。"""
    palette = json.loads((ROOT / "palette.json").read_text(encoding="utf-8"))
    if not isinstance(palette, dict) or not palette:
        raise ValueError("色板必须为非空命名映射")
    for name, colors in palette.items():
        if not isinstance(colors, dict) or not 2 <= len(colors) <= 16:
            raise ValueError(f"{name}: 色板必须包含 2 至 16 个命名实色")
        if any(not isinstance(v, str) or not HEX.fullmatch(v) for v in colors.values()):
            raise ValueError(f"{name}: 只支持 #RRGGBB 色值")
        if len({v.upper() for v in colors.values()}) != len(colors):
            raise ValueError(f"{name}: 不允许重复色值")
    return palette


def render_svg(text: str, allowed_colors: set[str], size: tuple[int, int] = (16, 16)) -> Image.Image:
    """把 16×16 或 16×64 SVG 光栅化为 RGBA，一坐标单位对应一个像素。

    只接受根 svg、无属性注释和直属 rect；矩形必须完全位于画布内。
    XML 实体声明、处理指令、样式、变换和其他扩展都拒绝，避免静默漏画。
    """
    if re.search(r"<!DOCTYPE|<!ENTITY|<\?", text, re.IGNORECASE):
        raise ValueError("不支持 DTD、实体声明或 XML 处理指令")
    parser = ET.XMLParser(target=ET.TreeBuilder(insert_comments=True, insert_pis=True))
    root = ET.fromstring(text, parser=parser)
    if size not in ((16, 16), (16, 64)):
        raise ValueError("只支持清单指定的 16×16 或 16×64 画布")
    canvas_width, canvas_height = size
    required = {"width": str(canvas_width), "height": str(canvas_height), "viewBox": f"0 0 {canvas_width} {canvas_height}", "shape-rendering": "crispEdges"}
    if root.tag != NS + "svg" or root.attrib != required:
        raise ValueError("根 svg 必须与清单尺寸完全匹配，不能添加样式或其他属性")
    if root.text and root.text.strip():
        raise ValueError("不支持根节点文本")
    result = Image.new("RGBA", size, (0, 0, 0, 0))
    pixel = result.load()
    count = 0
    for node in root:
        if node.tail and node.tail.strip():
            raise ValueError("不支持元素尾随文本")
        if node.tag is ET.Comment:
            continue
        if node.tag != NS + "rect" or set(node.attrib) != {"x", "y", "width", "height", "fill"}:
            raise ValueError("只支持直属 rect 及 x/y/width/height/fill 五个属性")
        if len(node) or (node.text and node.text.strip()):
            raise ValueError("rect 不能包含子元素或文本")
        values = [node.get(k, "") for k in ("x", "y", "width", "height")]
        if any(not INTEGER.fullmatch(v) for v in values):
            raise ValueError("矩形坐标和尺寸必须是非负十进制整数")
        x, y, width, height = map(int, values)
        if width < 1 or height < 1 or x + width > canvas_width or y + height > canvas_height:
            raise ValueError("矩形必须非空且完全位于清单画布")
        fill = node.get("fill", "")
        if not HEX.fullmatch(fill) or fill.upper() not in allowed_colors:
            raise ValueError("矩形填色必须是该资源色板内的 #RRGGBB")
        rgba = tuple(int(fill[i:i + 2], 16) for i in (1, 3, 5)) + (255,)
        for py in range(y, y + height):
            for px in range(x, x + width):
                pixel[px, py] = rgba
        count += 1
    if count == 0:
        raise ValueError("源稿不能没有矩形")
    return result


if __name__ == "__main__":
    import sys
    sys.dont_write_bytecode = True
    from pipeline import main as export_all
    export_all()
