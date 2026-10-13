"""检查10A离线设计稿的源图、引用和已确认几何边界。"""
from __future__ import annotations

import json
import math
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parent
WORKSPACE = ROOT.parents[2]
GEOMETRY = ROOT / "geometry.json"
HTML = ROOT / "index.html"
SCRIPT = ROOT / "preview.js"
TEXTURES = ROOT / "sources" / "textures"
REPORT = WORKSPACE / "build" / "reports" / "art" / "ART-TURBINE-10A"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def finite_tree(value: object, path: str = "geometry") -> None:
    """递归确保几何JSON内的数值均有限，避免非法SVG坐标。"""
    if isinstance(value, bool) or value is None or isinstance(value, str):
        return
    if isinstance(value, (int, float)):
        if not math.isfinite(value):
            fail(f"{path}含非有限数值: {value}")
        return
    if isinstance(value, list):
        for index, item in enumerate(value):
            finite_tree(item, f"{path}[{index}]")
        return
    if isinstance(value, dict):
        for key, item in value.items():
            finite_tree(item, f"{path}.{key}")


def check_svg(path: Path) -> int:
    """验证低分辨率材质只含范围内整数矩形及有限色板。"""
    root = ET.parse(path).getroot()
    if root.tag.rsplit("}", 1)[-1] != "svg":
        fail(f"根节点不是SVG: {path.name}")
    if root.attrib.get("width") != "16" or root.attrib.get("height") != "16":
        fail(f"非16×16画布: {path.name}")
    if root.attrib.get("viewBox") != "0 0 16 16" or root.attrib.get("shape-rendering") != "crispEdges":
        fail(f"像素画布约定不符: {path.name}")
    colors: set[str] = set()
    rects = 0
    for element in root:
        if element.tag.rsplit("}", 1)[-1] != "rect":
            fail(f"发现非rect绘图元素: {path.name}/{element.tag}")
        attrs = element.attrib
        for name in ("x", "y", "width", "height"):
            value = attrs.get(name, "0" if name in ("x", "y") else "")
            if not re.fullmatch(r"-?\d+", value):
                fail(f"非整数{ name }: {path.name}={value}")
        x, y = int(attrs.get("x", "0")), int(attrs.get("y", "0"))
        width, height = int(attrs["width"]), int(attrs["height"])
        if width <= 0 or height <= 0 or x < 0 or y < 0 or x + width > 16 or y + height > 16:
            fail(f"矩形越界或面积为零: {path.name}")
        fill = attrs.get("fill", "").lower()
        if not re.fullmatch(r"#[0-9a-f]{6}", fill):
            fail(f"填色不是不透明六位色: {path.name}={fill}")
        colors.add(fill)
        rects += 1
    if rects == 0 or len(colors) > 16:
        fail(f"矩形数量或色板超限: {path.name}, rect={rects}, colors={len(colors)}")
    return rects


def main() -> int:
    geometry = json.loads(GEOMETRY.read_text(encoding="utf-8"))
    finite_tree(geometry)
    designs = geometry.get("designs", {})
    sizes = geometry.get("sizes", {})
    if set(designs) != {"A", "B", "C"} or set(sizes) != {"3", "5", "7"}:
        fail("必须恰有A/B/C三稿与3/5/7三档")

    checked_rects = 0
    texture_paths = sorted(TEXTURES.glob("*.svg"))
    if len(texture_paths) != 6:
        fail(f"应有六张原创SVG源图，实际{len(texture_paths)}张")
    for path in texture_paths:
        checked_rects += check_svg(path)

    shared = geometry["shared"]
    blade_z = shared["bladeZ"]
    if blade_z != [0.13, 0.87] or shared["stationSpacing"] != 1:
        fail("轴向叶片范围或每块段距与定位合同不符")
    if not math.isclose(shared["bearingRadii"]["outer"], 0.27) or not math.isclose(shared["bearingRadii"]["inner"], 0.155):
        fail("轴承环半径与已核几何不符")
    if not shared["bearingRadii"]["inner"] > shared["shaftRadius"]:
        fail("转轴与轴承内圈无装配间隙")

    formula_cases = 0
    for size_key, size in sizes.items():
        radius = size["sweepRadius"]
        shell = size["innerShellRadius"]
        if not math.isclose(shell - radius, shared["clearance"], abs_tol=1e-9):
            fail(f"{size_key}档转子与八棱壳体余量不为0.125")
        for design_key, design in designs.items():
            ratio = radius / sizes["3"]["sweepRadius"]
            hub = design["hubRadius"] * ratio
            half_chord = design["bladeWidth"] * ratio / 2
            tip_radius = math.sqrt(max(hub * hub, radius * radius - half_chord * half_chord)) * 0.985
            corner_radius = math.hypot(tip_radius, half_chord)
            if corner_radius > radius + 1e-9 or hub * 0.94 >= tip_radius:
                fail(f"{design_key}/{size_key}扫掠几何超出真实边界或叶根失效")
            formula_cases += 1

    html = HTML.read_text(encoding="utf-8")
    script = SCRIPT.read_text(encoding="utf-8")
    if not (ROOT / "preview.js").is_file() or "preview.js" not in html:
        fail("交互脚本引用无效")
    if 'targetPrefix=target.id||"preview"' not in script or "`${targetPrefix}-uv${textureSerial++}`" not in script:
        fail("每个SVG目标未使用独立pattern ID前缀")
    if "function shaftMesh(L,angle)" not in script or "shaftMesh(s.len,angle)" not in script:
        fail("贯穿轴未接收当前示意转角")
    if 'annulus(.27,.155,faceZ,"#657173",0,24)' not in script or 'annulus(.155,.14,faceZ+.006,"#9ba5a4",angle,24)' not in script:
        fail("轴承外圈固定/内圈随转的分组输入不符")
    if "function plateFaceWithHole(z,fill)" not in script or "const h=.19" not in script or "plateFaceWithHole(faceZ,plateFill)" not in script:
        fail("端板未保留半宽.19的贯穿孔几何")
    if "diskFaces(.153,faceZ+.003" in script or "for(const m of marks)" in script or "marks=[]" in script:
        fail("伪孔或不参与深度队列的标记仍存在")
    if "function diskMark(p,r,color)" not in script or 'face(points,color,"#252b2d",.65)' not in script:
        fail("支承螺栓未加入统一面深度队列")
    if "sources/textures/${asset}.svg" not in script:
        fail("材质SVG动态引用模板缺失")
    for material in ("A", "B", "C"):
        for part in ("rotor", "bearing"):
            ref = f"sources/textures/{material}_{part}.svg"
            if not (ROOT / ref).is_file():
                fail(f"材质SVG文件缺失: {ref}")
        if f'texture:"{material}"' not in script and f'texture: "{material}"' not in script:
            fail(f"方案未绑定材质源: {material}")
    casing = WORKSPACE / "src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_casing_07_panel.png"
    if not casing.is_file() or "turbine_casing_07_panel.png" not in script:
        fail("只读机壳贴图引用缺失")
    if re.search(r"(?:href|src)\s*=\s*['\"]https?://", html + script, re.IGNORECASE):
        fail("预览不应依赖外网")
    for control in ("design", "size", "explode", "play", "slow", "speed", "viewport", "cardA", "cardB", "cardC"):
        if f'id="{control}"' not in html and f'`card${{key}}`' not in script:
            fail(f"交互控件或静态卡片缺失: {control}")
    if "requestAnimationFrame(frame)" not in script or "state.playing" not in script:
        fail("动画循环或暂停控制缺失")

    print("PASS: 6张16×16 SVG均为整数rect、crispEdges、有限不透明色板")
    print(f"PASS: {checked_rects}个矩形在像素边界内，色板均不超过16色")
    print("PASS: 几何JSON数值有限，A/B/C与3/5/7齐全")
    print(f"PASS: {formula_cases}组设计/尺寸扫掠角点未越内壳，保留0.125间隙")
    print("PASS: 叶片Z范围、段距、轴承/轴间隙及真实机壳引用符合定位")
    print("PASS: cardA/cardB/cardC/viewport的SVG pattern ID按目标唯一命名")
    print("PASS: 轴与内圈接收转角，固定外圈保持不动")
    print("PASS: 螺栓进入深度排序面队列，端板保留半宽0.19贯穿孔")
    print("PASS: HTML/本地纹理引用、暂停/速度/拆件控件存在，无外网依赖")
    print("PASS: 仅检查设计稿文件；未生成PNG、安装、构建或测试游戏")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ET.ParseError, json.JSONDecodeError) as exc:
        print(f"FAIL: 检查输入读取失败: {exc}", file=sys.stderr)
        sys.exit(1)
