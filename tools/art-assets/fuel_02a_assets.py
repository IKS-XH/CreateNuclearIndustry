"""EXT-A-FUEL-02A 专用像素素材与单格炉模型生成器。"""
from __future__ import annotations

import json
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / "tools" / "art-assets"
sys.path.insert(0, str(ART))
import export as strict_exporter

ASSET = ROOT / "src/main/resources/assets/create_nuclear_industry"
EVIDENCE = ROOT / "build/reports/extension/EXT-A-FUEL-02B"
REPORT = ROOT / "build/reports/extension/EXT-A-FUEL-02B.md"
MOD = "create_nuclear_industry:"

# 三种新纹理各自独立设色，避免改变已有全局色板和导出清单。
PALETTE = {
    "green_fuel_pellet": ["#26333A", "#53636B", "#78868A", "#A4A9A2", "#D0C7A8", "#6E6250"],
    "sintered_fuel_pellet": ["#302A26", "#594334", "#876343", "#B48755", "#D3B67C", "#D0C7A8"],
    "fuel_sintering_furnace_steel": ["#252E32", "#364247", "#4C5A5F", "#637176", "#7D8889", "#A1A39A"],
    "fuel_sintering_furnace_brass": ["#533D28", "#81603A", "#AA814A", "#D1AD69", "#E1C98A"],
    "fuel_sintering_furnace_refractory": ["#302E2A", "#49443A", "#625746", "#82715A", "#A38B6B"],
    "fuel_sintering_furnace_window_off": ["#151C20", "#26343A", "#35484D", "#536267", "#78817D"],
    "fuel_sintering_furnace_window_on": ["#3B2415", "#7D3E1B", "#C36B28", "#F1A642", "#FFE18A"],
}


def svg(name: str, rects: list[tuple[int, int, int, int, str]]) -> str:
    """将原创像素图形写成导出器支持的直属整数矩形SVG。"""
    body = "".join(
        f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{color}"/>'
        for x, y, w, h, color in rects
    )
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" '
        'viewBox="0 0 16 16" shape-rendering="crispEdges">'
        f"{body}</svg>\n"
    )


def render_texture(name: str) -> None:
    source = ART / "sources" / "fuel-02a" / f"{name}.svg"
    target_name = name.removesuffix("_off").removesuffix("_on")
    if name in {"fuel_sintering_furnace_window_off", "fuel_sintering_furnace_window_on"}:
        target_name = "fuel_sintering_furnace_" + ("window_off" if name.endswith("_off") else "window_on")
    if name.startswith("fuel_sintering_furnace_") and name not in {
        "fuel_sintering_furnace_window_off", "fuel_sintering_furnace_window_on"
    }:
        target_name = name
    if name in {"green_fuel_pellet", "sintered_fuel_pellet"}:
        target = ASSET / "textures/item" / f"{name}.png"
    else:
        target = ASSET / "textures/block" / f"{target_name}.png"
    if not source.is_file():
        raise FileNotFoundError(f"缺少可编辑SVG源稿：{source}；需要时先显式运行 --initialize-sources")
    text = source.read_text(encoding="utf-8")
    image = strict_exporter.render_svg(text, set(PALETTE[name]), (16, 16))
    target.parent.mkdir(parents=True, exist_ok=True)
    image.save(target)


def initialize_sources() -> None:
    """只为缺少的源稿写入初始像素稿，绝不覆盖已有SVG。"""
    for name, rects in {**ITEM_PIXELS, **FURNACE_TEXTURES}.items():
        source = ART / "sources" / "fuel-02a" / f"{name}.svg"
        if source.exists():
            continue
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_text(svg(name, rects), encoding="utf-8")


ITEM_PIXELS = {
    "green_fuel_pellet": [
        (6, 2, 4, 1, "#A4A9A2"), (5, 3, 6, 1, "#78868A"),
        (4, 4, 8, 1, "#53636B"), (4, 5, 8, 7, "#53636B"),
        (4, 5, 2, 7, "#78868A"), (6, 5, 1, 7, "#A4A9A2"),
        (7, 5, 4, 7, "#53636B"), (11, 5, 1, 7, "#26333A"),
        (5, 12, 6, 1, "#26333A"), (6, 13, 4, 1, "#26333A"),
        (6, 3, 2, 1, "#D0C7A8"), (9, 7, 2, 1, "#6E6250"),
    ],
    "sintered_fuel_pellet": [
        (6, 2, 4, 1, "#D3B67C"), (5, 3, 6, 1, "#B48755"),
        (4, 4, 8, 1, "#876343"), (4, 5, 8, 7, "#594334"),
        (4, 5, 2, 7, "#B48755"), (6, 5, 1, 7, "#D3B67C"),
        (7, 5, 4, 7, "#876343"), (11, 5, 1, 7, "#302A26"),
        (5, 12, 6, 1, "#302A26"), (6, 13, 4, 1, "#302A26"),
        (5, 6, 6, 1, "#D0C7A8"), (5, 10, 6, 1, "#594334"),
        (6, 3, 4, 1, "#D3B67C"),
    ],
}

FURNACE_TEXTURES = {
    "fuel_sintering_furnace_steel": [
        (0, 0, 16, 16, "#364247"), (0, 0, 16, 1, "#7D8889"),
        (0, 1, 1, 14, "#637176"), (15, 1, 1, 14, "#252E32"),
        (1, 14, 14, 1, "#252E32"), (2, 3, 1, 9, "#4C5A5F"),
        (13, 3, 1, 9, "#252E32"), (5, 2, 6, 1, "#A1A39A"),
        (5, 13, 6, 1, "#252E32"), (3, 4, 1, 1, "#A1A39A"),
    ],
    "fuel_sintering_furnace_brass": [
        (0, 0, 16, 16, "#81603A"), (0, 0, 16, 2, "#E1C98A"),
        (0, 2, 16, 2, "#D1AD69"), (0, 4, 16, 8, "#AA814A"),
        (0, 12, 16, 2, "#81603A"), (0, 14, 16, 2, "#533D28"),
        (2, 6, 2, 2, "#D1AD69"), (12, 6, 2, 2, "#533D28"),
    ],
    "fuel_sintering_furnace_refractory": [
        (0, 0, 16, 16, "#49443A"), (0, 0, 16, 2, "#82715A"),
        (0, 2, 2, 12, "#625746"), (14, 2, 2, 12, "#302E2A"),
        (2, 13, 12, 2, "#302E2A"), (3, 4, 2, 1, "#A38B6B"),
        (7, 6, 3, 2, "#625746"), (11, 10, 2, 2, "#82715A"),
    ],
    "fuel_sintering_furnace_window_off": [
        (0, 0, 16, 16, "#151C20"), (1, 1, 14, 14, "#26343A"),
        (2, 2, 2, 12, "#35484D"), (4, 2, 1, 12, "#536267"),
        (5, 2, 8, 1, "#35484D"), (5, 13, 8, 1, "#151C20"),
        (8, 4, 2, 7, "#78817D"), (9, 4, 1, 7, "#536267"),
    ],
    "fuel_sintering_furnace_window_on": [
        (0, 0, 16, 16, "#3B2415"), (1, 1, 14, 14, "#7D3E1B"),
        (2, 2, 2, 12, "#C36B28"), (4, 2, 1, 12, "#F1A642"),
        (5, 2, 8, 1, "#C36B28"), (5, 13, 8, 1, "#3B2415"),
        (7, 4, 3, 7, "#FFE18A"), (8, 4, 1, 7, "#F1A642"),
    ],
}


TEXTURES = {
    "steel": "fuel_sintering_furnace_steel",
    "brass": "fuel_sintering_furnace_brass",
    "refractory": "fuel_sintering_furnace_refractory",
    "window_off": "fuel_sintering_furnace_window_off",
    "window_on": "fuel_sintering_furnace_window_on",
}


def face(texture: str, uv: list[float] | None = None) -> dict:
    return {"texture": f"#{texture}", "uv": uv or [0, 0, 16, 16]}


def box(lo: list[float], hi: list[float], texture: str, faces: dict | None = None) -> dict:
    face_map = faces or {key: face(texture) for key in ("north", "south", "east", "west", "up", "down")}
    return {"from": lo, "to": hi, "faces": face_map}


def octagonal_band(y0: float, y1: float, material: str, outer_apothem: float = 8.0,
                   thickness: float = 0.50) -> list[dict]:
    """八片相邻面板构成八棱带，外侧面相交于同一八棱顶点。"""
    half_width = outer_apothem * math.tan(math.pi / 8)
    panels = []
    for angle in range(0, 360, 45):
        depth_center = outer_apothem - thickness / 2
        if angle in (0, 45, 315):
            element = box([8 - half_width, y0, 8 + depth_center - thickness / 2],
                          [8 + half_width, y1, 8 + depth_center + thickness / 2], material)
            rotation = {0: None, 45: 45, 315: -45}[angle]
        elif angle in (90, 270):
            center_x = 8 + depth_center if angle == 90 else 8 - depth_center
            element = box([center_x - thickness / 2, y0, 8 - half_width],
                          [center_x + thickness / 2, y1, 8 + half_width], material)
            rotation = None
        elif angle == 180:
            element = box([8 - half_width, y0, 8 - depth_center - thickness / 2],
                          [8 + half_width, y1, 8 - depth_center + thickness / 2], material)
            rotation = None
        elif angle in (135, 225):
            center_z = 8 - depth_center
            element = box([8 - half_width, y0, center_z - thickness / 2],
                          [8 + half_width, y1, center_z + thickness / 2], material)
            rotation = -45 if angle == 135 else 45
        else:
            raise ValueError(f"不支持的八棱方向: {angle}")
        if rotation is not None:
            element["rotation"] = {"origin": [8, (y0 + y1) / 2, 8], "axis": "y", "angle": rotation, "rescale": False}
        panels.append(element)
    return panels


def add_front_window(elements: list[dict], variant: str) -> None:
    win_tex = "window_on" if variant == "on" else "window_off"
    # 前壳已扩至Z=0；拆分中央北面，四周钢板、黄铜框和窗片无缝相接。
    for element in elements:
        if ("rotation" not in element
                and element["from"][1] == 2.65 and element["to"][1] == 9.35
                and element["from"][2] == 0 and element["to"][2] == 0.5
                and element["from"][0] < 8 < element["to"][0]):
            element["faces"].pop("north", None)
            break
    else:
        raise ValueError("未找到正北中段钢壳面，无法嵌入观察窗")
    panel_lo, panel_hi = 8 - 8 * math.tan(math.pi / 8), 8 + 8 * math.tan(math.pi / 8)
    for lo, hi in (
        ([panel_lo, 2.65, 0], [5.55, 9.35, 0.08]),
        ([10.45, 2.65, 0], [panel_hi, 9.35, 0.08]),
        ([5.55, 2.65, 0], [10.45, 4.25, 0.08]),
        ([5.55, 8.3, 0], [10.45, 9.35, 0.08]),
    ):
        elements.append(box(lo, hi, "steel", {"north": face("steel")}))
    for lo, hi in (
        ([5.55, 4.25, 0], [10.45, 4.8, 0.08]),
        ([5.55, 7.75, 0], [10.45, 8.3, 0.08]),
        ([5.55, 4.8, 0], [5.8, 7.75, 0.08]),
        ([10.2, 4.8, 0], [10.45, 7.75, 0.08]),
    ):
        elements.append(box(lo, hi, "brass", {"north": face("brass")}))
    elements.append(box([5.8, 4.8, 0], [10.2, 7.75, 0.08], win_tex,
                        {"north": face(win_tex, [6, 5, 10, 11])}))


def top_bottom_ports(elements: list[dict]) -> None:
    # 顶/底分别留出方形暗孔，并以四边耐火衬圈表示进料和热源接口。
    for y0, y1, hole_y0, hole_y1 in ((11.6, 11.75, 11.6, 11.61), (0.0, 0.85, 0.0, 0.05)):
        outer_lo, outer_hi = 4.0, 12.0
        inner_lo, inner_hi = 6.0, 10.0
        for lo, hi in (
            ([outer_lo, y0, outer_lo], [outer_hi, y1, inner_lo]),
            ([outer_lo, y0, inner_hi], [outer_hi, y1, outer_hi]),
            ([outer_lo, y0, inner_lo], [inner_lo, y1, inner_hi]),
            ([inner_hi, y0, inner_lo], [outer_hi, y1, inner_hi]),
        ):
            elements.append(box(lo, hi, "brass"))
        # 暗色短柱明确表示接口腔；端面与炉体表面齐平，不依赖动画或BER。
        elements.append(box([6, hole_y0, 6], [10, hole_y1, 10], "refractory"))


def furnace_model(variant: str) -> dict:
    window_texture = TEXTURES["window_on" if variant == "on" else "window_off"]
    textures = {
        "particle": MOD + "block/" + TEXTURES["steel"],
        "steel": MOD + "block/" + TEXTURES["steel"],
        "brass": MOD + "block/" + TEXTURES["brass"],
        "refractory": MOD + "block/" + TEXTURES["refractory"],
        "window_off": MOD + "block/" + TEXTURES["window_off"],
        "window_on": MOD + "block/" + TEXTURES["window_on"],
    }
    elements = [
        *octagonal_band(0, 1.8, "steel"),
        box([4, 1.2, 4], [12, 1.8, 12], "refractory"),
        *octagonal_band(1.8, 2.65, "brass"),
        *octagonal_band(2.65, 9.35, "steel"),
        *octagonal_band(9.35, 10.2, "brass"),
        *octagonal_band(10.2, 11.6, "refractory", outer_apothem=7.4, thickness=2.85),
        box([4, 10.2, 4], [12, 11.6, 12], "refractory"),
    ]
    top_bottom_ports(elements)
    add_front_window(elements, variant)
    # 将原11.75单位高度统一映射到完整方块高度；横截面由八棱外切面精确到0..16。
    height_scale = 16 / 11.75
    for element in elements:
        element["from"][1] = round(element["from"][1] * height_scale, 6)
        element["to"][1] = round(element["to"][1] * height_scale, 6)
        if "rotation" in element:
            element["rotation"]["origin"][1] = round(element["rotation"]["origin"][1] * height_scale, 6)
    return {"parent": "minecraft:block/block", "ambientocclusion": True,
            "textures": textures, "elements": elements}


DISPLAY = {
    "gui": {"rotation": [25, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.55, 0.55, 0.55]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 1.5, 0], "scale": [0.375, 0.375, 0.375]},
    "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 1.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
}


def variants() -> dict:
    result = {}
    for facing, angle in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for lit in (False, True):
            result[f"facing={facing},lit={str(lit).lower()}"] = {
                "model": f"create_nuclear_industry:block/fuel_sintering_furnace_{'on' if lit else 'off'}",
                **({"y": angle} if angle else {}),
            }
    return {"variants": result}


def write_json(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def bounds(elements: list[dict]) -> list[list[float]]:
    points = []
    for element in elements:
        lo, hi = element["from"], element["to"]
        corners = [(x, y, z) for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])]
        rotation = element.get("rotation")
        if rotation:
            angle = math.radians(rotation["angle"])
            ox, oy, oz = rotation["origin"]
            corners = [(ox + (x - ox) * math.cos(angle) + (z - oz) * math.sin(angle), y,
                        oz - (x - ox) * math.sin(angle) + (z - oz) * math.cos(angle))
                       for x, y, z in corners]
        points.extend(corners)
    return [[min(point[axis] for point in points) for axis in range(3)],
            [max(point[axis] for point in points) for axis in range(3)]]


def uv_report(model: dict) -> dict:
    faces = [definition for element in model["elements"] for definition in element["faces"].values()]
    bad = [item["uv"] for item in faces if len(item.get("uv", [])) != 4 or
           any(value < 0 or value > 16 for value in item.get("uv", []))]
    implicit = sum("uv" not in item for item in faces)
    return {"elements": len(model["elements"]), "explicit_faces": len(faces) - implicit,
            "implicit_faces": implicit, "out_of_range_faces": len(bad), "examples": bad[:3]}


def front_face_coverage_report(model: dict) -> dict:
    """以北向可见面采样确认观察窗及其周围钢壳没有漏面。"""
    scale = 16 / 11.75
    x0, x1 = 8 - 8 * math.tan(math.pi / 8), 8 + 8 * math.tan(math.pi / 8)
    y0, y1 = 2.65 * scale, 9.35 * scale
    visible = [element for element in model["elements"]
               if "rotation" not in element and "north" in element["faces"]
               and element["from"][2] <= .000001]
    nx = ny = 100
    missing = overlap = 0
    for ix in range(nx):
        x = x0 + (ix + .5) * (x1 - x0) / nx
        for iy in range(ny):
            y = y0 + (iy + .5) * (y1 - y0) / ny
            hits = sum(element["from"][0] <= x <= element["to"][0]
                       and element["from"][1] <= y <= element["to"][1]
                       for element in visible)
            if not hits:
                missing += 1
            elif hits > 1:
                overlap += 1
    return {"sample_grid": [nx, ny], "uncovered_points": missing, "overlap_points": overlap,
            "visible_north_faces": len(visible), "plane_z": 0.0}


def transformed_panel(angle: float, apothem: float, half_width: float, thickness: float) -> tuple:
    rad = math.radians(angle)
    normal = (math.sin(rad), math.cos(rad))
    tangent = (math.cos(rad), -math.sin(rad))
    center = (8 + normal[0] * (apothem - thickness / 2),
              8 + normal[1] * (apothem - thickness / 2))
    corners = [(center[0] + tangent[0] * dx + normal[0] * dz,
                center[1] + tangent[1] * dx + normal[1] * dz)
               for dx, dz in ((-half_width, thickness / 2), (half_width, thickness / 2),
                              (half_width, -thickness / 2), (-half_width, -thickness / 2))]
    return normal, center, tangent, corners


def shell_report() -> dict:
    apothem, thickness = 8.0, .5
    half_width = apothem * math.tan(math.pi / 8)
    panels = [transformed_panel(angle, apothem, half_width, thickness) for angle in range(0, 360, 45)]
    missing = 0
    rays = 4096
    for index in range(rays):
        theta = (index + .5) * (2 * math.pi / rays)
        direction = (math.sin(theta), math.cos(theta))
        delta = (theta + math.pi / 8) % (math.pi / 4) - math.pi / 8
        radius = (apothem - .001) / math.cos(delta)
        point = (8 + direction[0] * radius, 8 + direction[1] * radius)
        covered = False
        for normal, center, tangent, _ in panels:
            if normal[0] * direction[0] + normal[1] * direction[1] <= 0:
                continue
            delta = (point[0] - center[0], point[1] - center[1])
            along = delta[0] * tangent[0] + delta[1] * tangent[1]
            radial = delta[0] * normal[0] + delta[1] * normal[1]
            if abs(along) <= half_width + .002 and -.002 <= radial <= thickness + .002:
                covered = True
                break
        if not covered:
            missing += 1
    return {"sampled_rays": rays, "uncovered_rays": missing,
            "outer_apothem": apothem, "outer_bound": max(max(abs(x - 8), abs(z - 8)) for _, _, _, cs in panels for x, z in cs) + 8,
            "minimum_face_angle_degrees": 45}


def json_shell_report(model: dict) -> dict:
    """从写出的JSON几何反变换采样点，检查各接合高度的八棱外缘。"""
    scale = 16 / 11.75
    probes = tuple((round(y * scale, 6), apothem, label) for y, apothem, label in (
        (0.6, 8.0, "steel_foot"), (1.2, 8.0, "steel_foot_mid_join"),
        (1.799, 8.0, "steel_foot_upper_edge"), (1.8, 8.0, "brass_lower_band_join"),
        (2.649, 8.0, "brass_lower_band_upper_edge"), (2.65, 8.0, "steel_shell_join"),
        (5.0, 8.0, "steel_shell"), (9.35, 8.0, "brass_upper_band_join"),
        (10.199, 8.0, "brass_upper_band_upper_edge"), (10.2, 7.4, "refractory_cap_join"),
        (10.5, 7.4, "refractory_cap"), (11.6, 7.4, "top_interface_join")))
    rays = 4096
    reports = []
    for sample_y, apothem, label in probes:
        shell_elements = [element for element in model["elements"]
                          if element["from"][1] <= sample_y <= element["to"][1]]
        missing = 0
        for index in range(rays):
            theta = (index + .5) * 2 * math.pi / rays
            delta = (theta + math.pi / 8) % (math.pi / 4) - math.pi / 8
            radius = (apothem - .001) / math.cos(delta)
            point = (8 + math.sin(theta) * radius, sample_y, 8 + math.cos(theta) * radius)
            covered = False
            for element in shell_elements:
                test_point = point
                rotation = element.get("rotation")
                if rotation:
                    angle = math.radians(-rotation["angle"])
                    ox, _, oz = rotation["origin"]
                    x, y, z = point
                    test_point = (ox + (x - ox) * math.cos(angle) + (z - oz) * math.sin(angle), y,
                                  oz - (x - ox) * math.sin(angle) + (z - oz) * math.cos(angle))
                if all(element["from"][axis] - .002 <= test_point[axis] <= element["to"][axis] + .002
                       for axis in range(3)):
                    covered = True
                    break
            if not covered:
                missing += 1
        reports.append({"label": label, "sample_y": sample_y, "apothem": apothem,
                        "sampled_rays": rays, "uncovered_rays": missing,
                        "elements": len(shell_elements)})
    return {"probes": reports,
            "uncovered_rays_total": sum(probe["uncovered_rays"] for probe in reports)}


def render_model(model: dict, path: Path, azimuth: float, elevation: float, title: str) -> None:
    """直接读取模型JSON，以简化面色和深度缓冲展示三维几何。"""
    width = height = 560
    background = (236, 239, 236)
    rgb = Image.new("RGB", (width, height), background)
    zbuffer = Image.new("F", (width, height), -1e9)
    palette = {**{name: tuple(int(PALETTE[texture][2][i:i + 2], 16) for i in (1, 3, 5))
                  for name, texture in (("steel", "fuel_sintering_furnace_steel"),
                                        ("brass", "fuel_sintering_furnace_brass"),
                                        ("refractory", "fuel_sintering_furnace_refractory"),
                                        ("window_off", "fuel_sintering_furnace_window_off"),
                                        ("window_on", "fuel_sintering_furnace_window_on"))}}
    face_vertices = {
        "north": (0, 1, 2, 3), "south": (5, 4, 7, 6), "west": (4, 0, 3, 7),
        "east": (1, 5, 6, 2), "up": (3, 2, 6, 7), "down": (4, 5, 1, 0),
    }
    normals = {"north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0),
               "east": (1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}
    ca, sa = math.cos(math.radians(azimuth)), math.sin(math.radians(azimuth))
    ce, se = math.cos(math.radians(elevation)), math.sin(math.radians(elevation))
    scale, cx, cy = 21.0, width / 2, height / 2 + 8

    def project(point: tuple[float, float, float]) -> tuple[float, float, float]:
        x, y, z = point[0] - 8, point[1] - 8, point[2] - 8
        xr, zr = x * ca - z * sa, x * sa + z * ca
        screen_y = y * ce - zr * se
        depth = zr * ce + y * se
        return cx + xr * scale, cy - screen_y * scale, depth

    for element in model["elements"]:
        lo, hi = element["from"], element["to"]
        vertices = [(lo[0], lo[1], lo[2]), (hi[0], lo[1], lo[2]), (hi[0], hi[1], lo[2]), (lo[0], hi[1], lo[2]),
                    (lo[0], lo[1], hi[2]), (hi[0], lo[1], hi[2]), (hi[0], hi[1], hi[2]), (lo[0], hi[1], hi[2])]
        rotation = element.get("rotation")
        if rotation:
            angle = math.radians(rotation["angle"])
            ox, oy, oz = rotation["origin"]
            vertices = [(ox + (x - ox) * math.cos(angle) + (z - oz) * math.sin(angle), y,
                         oz - (x - ox) * math.sin(angle) + (z - oz) * math.cos(angle))
                        for x, y, z in vertices]
        for name, ids in face_vertices.items():
            if name not in element["faces"]:
                continue
            normal = normals[name]
            if rotation:
                theta = math.radians(rotation["angle"])
                normal = (normal[0] * math.cos(theta) + normal[2] * math.sin(theta), normal[1],
                          -normal[0] * math.sin(theta) + normal[2] * math.cos(theta))
            view_normal = ((normal[0] * ca - normal[2] * sa) * 0 +
                           (normal[0] * sa + normal[2] * ca) * ce + normal[1] * se)
            if view_normal <= .01:
                continue
            texture = element["faces"][name]["texture"].lstrip("#")
            base = palette.get(texture, (85, 90, 90))
            shade = min(1.25, max(.60, .82 + view_normal * .35))
            color = tuple(min(255, int(channel * shade)) for channel in base)
            points = [project(vertices[i]) for i in ids]
            min_x = max(0, int(min(p[0] for p in points))); max_x = min(width - 1, int(max(p[0] for p in points)) + 1)
            min_y = max(0, int(min(p[1] for p in points))); max_y = min(height - 1, int(max(p[1] for p in points)) + 1)
            if min_x > max_x or min_y > max_y:
                continue
            mask = Image.new("1", (max_x - min_x + 1, max_y - min_y + 1), 0)
            ImageDraw.Draw(mask).polygon([(p[0] - min_x, p[1] - min_y) for p in points], fill=1)
            matrix = [[points[i][0], points[i][1], 1.0] for i in range(3)]
            determinant = (matrix[0][0] * (matrix[1][1] - matrix[2][1]) +
                           matrix[1][0] * (matrix[2][1] - matrix[0][1]) +
                           matrix[2][0] * (matrix[0][1] - matrix[1][1]))
            if abs(determinant) < 1e-8:
                continue
            depths = [p[2] for p in points[:3]]
            # 每像素深度通过三点仿射平面求解，足以正确遮挡相邻面板。
            aa = ((depths[0] * (matrix[1][1] - matrix[2][1]) + depths[1] * (matrix[2][1] - matrix[0][1]) + depths[2] * (matrix[0][1] - matrix[1][1])) / determinant)
            bb = ((depths[0] * (matrix[2][0] - matrix[1][0]) + depths[1] * (matrix[0][0] - matrix[2][0]) + depths[2] * (matrix[1][0] - matrix[0][0])) / determinant)
            cc = depths[0] - aa * points[0][0] - bb * points[0][1]
            draw = ImageDraw.Draw(rgb)
            mask_pixels = mask.load(); depth_pixels = zbuffer.load()
            for py in range(min_y, max_y + 1):
                for px in range(min_x, max_x + 1):
                    if mask_pixels[px - min_x, py - min_y]:
                        depth = aa * (px + .5) + bb * (py + .5) + cc
                        if depth > depth_pixels[px, py]:
                            depth_pixels[px, py] = depth
                            draw.point((px, py), fill=color)
    font = preview_font(18)
    ImageDraw.Draw(rgb).text((18, 16), title, fill="#263238", font=font)
    ImageDraw.Draw(rgb).text((18, height - 28), "JSON几何预览；不代表客户端UV、光照与最终渲染", fill="#435057", font=font)
    path.parent.mkdir(parents=True, exist_ok=True)
    rgb.save(path)


def render_item_preview(path: Path) -> None:
    images = []
    for name in ("green_fuel_pellet", "sintered_fuel_pellet"):
        texture = Image.open(ASSET / "textures/item" / f"{name}.png").convert("RGBA")
        images.append(texture.resize((256, 256), Image.Resampling.NEAREST))
    canvas = Image.new("RGB", (560, 680), "#EEF0ED")
    for i, image in enumerate(images):
        canvas.paste(image, (20 + i * 270, 10), image)
    font = preview_font(18)
    draw = ImageDraw.Draw(canvas)
    draw.text((18, 274), "浅色背景：生燃料芯块", fill="#263238", font=font)
    draw.text((288, 274), "浅色背景：烧结燃料芯块", fill="#263238", font=font)
    draw.rectangle((0, 320, 559, 679), fill="#202A2E")
    for i, image in enumerate(images):
        canvas.paste(image, (20 + i * 270, 335), image)
    draw = ImageDraw.Draw(canvas)
    draw.text((18, 604), "深色背景：生燃料芯块", fill="#F0F2EF", font=font)
    draw.text((288, 604), "深色背景：烧结燃料芯块", fill="#F0F2EF", font=font)
    draw.text((18, 646), "16×16原稿按最近邻放大", fill="#CBD2D0", font=font)
    path.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(path)


def preview_font(size: int):
    for font_path in ("C:/Windows/Fonts/msyh.ttc", "C:/Windows/Fonts/simhei.ttf", "arial.ttf"):
        try:
            return ImageFont.truetype(font_path, size)
        except OSError:
            continue
    return ImageFont.load_default()


def main() -> None:
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--initialize-sources", action="store_true",
                        help="只创建缺失的本批SVG初稿，不覆盖已存在源稿")
    parser.add_argument("--geometry-only", action="store_true",
                        help="只重建本炉三种JSON模型，不导出PNG或重写其他资源")
    args = parser.parse_args()
    if args.initialize_sources:
        initialize_sources()
        return
    if not args.geometry_only:
        for name in (*ITEM_PIXELS.keys(), *FURNACE_TEXTURES.keys()):
            render_texture(name)

    off = furnace_model("off")
    on = furnace_model("on")
    write_json(ASSET / "models/block/fuel_sintering_furnace_off.json", off)
    write_json(ASSET / "models/block/fuel_sintering_furnace_on.json", on)
    item_model = furnace_model("off")
    item_model["display"] = DISPLAY
    write_json(ASSET / "models/block/fuel_sintering_furnace_item.json", item_model)
    write_json(ASSET / "models/item/fuel_sintering_furnace.json",
               {"parent": "create_nuclear_industry:block/fuel_sintering_furnace_item"})
    if not args.geometry_only:
        for item_name in ITEM_PIXELS:
            write_json(ASSET / f"models/item/{item_name}.json",
                       {"parent": "minecraft:item/generated",
                        "textures": {"layer0": MOD + "item/" + item_name}})
        write_json(ASSET / "blockstates/fuel_sintering_furnace.json", variants())

    # 预览与检查重新读取落盘JSON，避免只审查内存中的生成对象。
    off = json.loads((ASSET / "models/block/fuel_sintering_furnace_off.json").read_text(encoding="utf-8"))
    on = json.loads((ASSET / "models/block/fuel_sintering_furnace_on.json").read_text(encoding="utf-8"))
    item_model = json.loads((ASSET / "models/block/fuel_sintering_furnace_item.json").read_text(encoding="utf-8"))

    model_bounds = bounds(item_model["elements"])
    uv = {name: uv_report(model) for name, model in (("off", off), ("on", on), ("item", item_model))}
    ref_paths = {"steel": TEXTURES["steel"], "brass": TEXTURES["brass"],
                 "refractory": TEXTURES["refractory"], "window_off": TEXTURES["window_off"],
                 "window_on": TEXTURES["window_on"]}
    errors = []
    if len(variants()["variants"]) != 8:
        errors.append("方块状态不是8种 facing/lit 组合")
    if any(value < -1e-5 for value in model_bounds[0]) or any(value > 16 + 1e-5 for value in model_bounds[1]):
        errors.append(f"模型边界超出方块: {model_bounds}")
    if any(abs(value) > 1e-5 for value in model_bounds[0]) or any(abs(value - 16) > 1e-5 for value in model_bounds[1]):
        errors.append(f"模型未完整占满0..16范围: {model_bounds}")
    if any(item["implicit_faces"] or item["out_of_range_faces"] for item in uv.values()):
        errors.append("模型存在隐式或越界UV")
    element_angles = {element["rotation"]["angle"] for element in off["elements"] if "rotation" in element}
    if not element_angles <= {-45, -22.5, 0, 22.5, 45}:
        errors.append(f"方块模型元素旋转角超出1.21.1允许集合: {sorted(element_angles)}")
    actual_shell = json_shell_report(off)
    if actual_shell["uncovered_rays_total"]:
        errors.append("八面钢壳存在周向缺口")
    front_coverage = front_face_coverage_report(off)
    if front_coverage["uncovered_points"]:
        errors.append("正北观察窗或相邻钢壳存在未覆盖可见面")
    if front_coverage["overlap_points"]:
        errors.append("正北可见钢壳、窗框或窗片存在共面重叠")
    pane_center = (8.0, 6.0 * 16 / 11.75)
    if any("rotation" not in element and "north" in element["faces"]
           and abs(element["from"][2]) < 1e-6
           and element["from"][0] < pane_center[0] < element["to"][0]
           and element["from"][1] < pane_center[1] < element["to"][1]
           and element["faces"]["north"]["texture"] == "#steel"
           for element in off["elements"]):
        errors.append("观察窗中心仍被共面钢壳面遮挡")
    for key, value in ref_paths.items():
        if not (ASSET / "textures/block" / f"{value}.png").exists():
            errors.append(f"缺少模型材质PNG: {key}")
    for model_name, model in (("off", off), ("on", on), ("item", item_model)):
        for texture_name, resource in model["textures"].items():
            if texture_name == "particle":
                continue
            namespace, texture_path = resource.split(":", 1)
            resource_path = ROOT / "src/main/resources/assets" / namespace / "textures" / f"{texture_path}.png"
            if not resource_path.is_file():
                errors.append(f"{model_name}模型材质引用不存在: {resource}")
    source_files = sorted((ART / "sources" / "fuel-02a").glob("*.svg"))
    if len(source_files) != 7:
        errors.append(f"本批应有7张SVG源稿，实际为{len(source_files)}张")
    for texture_name in (*ITEM_PIXELS.keys(), *FURNACE_TEXTURES.keys()):
        texture_path = (ASSET / "textures/item" / f"{texture_name}.png" if texture_name in ITEM_PIXELS
                        else ASSET / "textures/block" / f"{texture_name}.png")
        with Image.open(texture_path) as texture:
            if texture.size != (16, 16) or texture.mode != "RGBA":
                errors.append(f"纹理必须为16×16 RGBA: {texture_path.name}")
    state_data = json.loads((ASSET / "blockstates/fuel_sintering_furnace.json").read_text(encoding="utf-8"))
    if set(state_data["variants"]) != {
        f"facing={facing},lit={str(lit).lower()}" for facing in ("north", "east", "south", "west")
        for lit in (False, True)
    }:
        errors.append("方块状态未覆盖四向和lit两态的全部组合")
    if not args.geometry_only:
        for state_variant, state_model in state_data["variants"].items():
            model_id = state_model["model"]
            namespace, model_path = model_id.split(":", 1)
            resolved_model = ROOT / "src/main/resources/assets" / namespace / "models" / f"{model_path}.json"
            if not resolved_model.is_file():
                errors.append(f"方块状态{state_variant}引用模型不存在: {model_id}")
    item_ref = json.loads((ASSET / "models/item/fuel_sintering_furnace.json").read_text(encoding="utf-8"))
    if item_ref.get("parent") != "create_nuclear_industry:block/fuel_sintering_furnace_item":
        errors.append("物品模型未指向完整三维炉体模型")
    if not args.geometry_only:
        for item_name in ITEM_PIXELS:
            item_data = json.loads((ASSET / f"models/item/{item_name}.json").read_text(encoding="utf-8"))
            if item_data.get("parent") != "minecraft:item/generated" or item_data.get("textures", {}).get("layer0") != MOD + "item/" + item_name:
                errors.append(f"{item_name}物品模型父级或贴图引用错误")
            if not (ASSET / f"textures/item/{item_name}.png").is_file():
                errors.append(f"{item_name}物品模型贴图缺失")
    if set(ITEM_PIXELS) != {"green_fuel_pellet", "sintered_fuel_pellet"}:
        errors.append("本批芯块素材白名单不匹配")

    if not args.geometry_only:
        render_item_preview(EVIDENCE / "fuel-pellet-preview.png")
    render_model(off, EVIDENCE / "furnace-north-top-angle.png", 180, 20, "冷态 / 顶口 / 北向")
    render_model(on, EVIDENCE / "furnace-bottom-angle.png", 220, -38, "热态 / 底部供热口")
    summary = {
        "asset_ids": ["green_fuel_pellet", "sintered_fuel_pellet", "fuel_sintering_furnace"],
        "new_textures": 7,
        "editable_svg_sources": len(source_files),
        "standard_export_reads_sources": True,
        "item_textures": 2,
        "block_textures": 5,
        "blockstate_variants": len(variants()["variants"]),
        "model_elements": {"off": len(off["elements"]), "on": len(on["elements"]), "item": len(item_model["elements"])},
        "bounds": model_bounds,
        "uv": uv,
        "shell_design": shell_report(),
        "shell_json": actual_shell,
        "front_face_coverage": front_coverage,
        "window": {"lit_false": ref_paths["window_off"], "lit_true": ref_paths["window_on"]},
        "display_scale": {key: value["scale"] for key, value in DISPLAY.items()},
        "unchanged_centrifuge_textures": True,
        "errors": errors,
    }
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    (EVIDENCE / "asset-check.json").write_text(json.dumps(summary, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    if errors:
        raise ValueError("; ".join(errors))


if __name__ == "__main__":
    sys.dont_write_bytecode = True
    main()
