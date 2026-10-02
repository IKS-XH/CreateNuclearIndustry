"""生成并预览EXT-A-FUEL-01D离心机JSON几何，不依赖Minecraft运行时。"""
from __future__ import annotations

import argparse
import importlib.util
import json
import math
from pathlib import Path
import sys

from PIL import Image, ImageDraw, ImageFont
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
ASSET = ROOT / "src/main/resources/assets/create_nuclear_industry"
ART = ROOT / "tools/art-assets"
EVIDENCE = ROOT / "build/reports/extension/EXT-A-FUEL-01E-assets"
MOD = "create_nuclear_industry:block/"

spec = importlib.util.spec_from_file_location("art_export", ART / "export.py")
exporter = importlib.util.module_from_spec(spec)
assert spec and spec.loader
sys.dont_write_bytecode = True
spec.loader.exec_module(exporter)

TEXTURES = {
    "casing": "#59656A",
    "brass": "#B88945",
    "glass": "#24464C",
    "dark": "#273238",
}


def box(lo, hi, texture="casing", angle=0):
    item = {
        "from": list(lo), "to": list(hi),
        "faces": {face: {"texture": f"#{texture}"} for face in ("north", "south", "east", "west", "up", "down")},
    }
    if angle:
        if angle not in (-45,-22.5,22.5,45):
            raise ValueError(f"Minecraft元素旋转角度超出标准范围: {angle}")
        center = [(lo[i] + hi[i]) / 2 for i in range(3)]
        item["rotation"] = {"origin": center, "axis": "y", "angle": angle, "rescale": False}
    return item


def default_uv(element, face, map_y=lambda value: value):
    """复现锁定版 BlockElement.uvsByFace 的默认UV投影。"""
    lo, hi = element["from"], element["to"]
    y0, y1 = map_y(lo[1]), map_y(hi[1])
    return {
        "down": [lo[0], 16 - hi[2], hi[0], 16 - lo[2]],
        "up": [lo[0], lo[2], hi[0], hi[2]],
        "north": [16 - hi[0], 16 - y1, 16 - lo[0], 16 - y0],
        "south": [lo[0], 16 - y1, hi[0], 16 - y0],
        "west": [lo[2], 16 - y1, hi[2], 16 - y0],
        "east": [16 - hi[2], 16 - y1, 16 - lo[2], 16 - y0],
    }[face]


def make_uv_explicit(elements, map_y=lambda value: value):
    """将原版按模型坐标补出的UV显式写入各面，可选地重映射纵向坐标。"""
    for element in elements:
        for face, definition in element["faces"].items():
            definition["uv"] = default_uv(element, face, map_y)
    return elements


def panel(cx, cz, y0, y1, length, thick, material, angle):
    direction=angle%360
    rotation=0
    if direction in (0,180):
        lo=(cx-length/2,y0,cz-thick/2); hi=(cx+length/2,y1,cz+thick/2)
        face="north" if direction==0 else "south"
    elif direction in (90,270):
        lo=(cx-thick/2,y0,cz-length/2); hi=(cx+thick/2,y1,cz+length/2)
        face="east" if direction==90 else "west"
    else:
        lo=(cx-length/2,y0,cz-thick/2); hi=(cx+length/2,y1,cz+thick/2)
        if direction==45: rotation=-45; face="north"
        elif direction==135: rotation=45; face="south"
        elif direction==225: rotation=-45; face="south"
        elif direction==315: rotation=45; face="north"
        else: raise ValueError(f"离心机面板方向必须为45度步进: {direction}")
    element = box(lo,hi,material,rotation)
    # 薄板只保留朝外的一面；斜面不带cullface，紧贴机器时不会被整格剔除。
    element["faces"] = {face: {"texture": f"#{material}"}}
    return element


def drum(y0, y1):
    """八片径向侧板围成八棱转鼓，斜向四片是真实旋转几何。"""
    parts = []
    radius = 7.0
    # 按外表面半径计算八边形弦长，并留0.02模型单位窄搭接封住渲染裂隙。
    facet = 2 * (radius + .2) * math.tan(math.pi / 8) + .02
    for angle in range(0, 360, 45):
        radians = math.radians(angle)
        cx, cz = 8 + math.sin(radians)*radius, 8 - math.cos(radians)*radius
        # 八面同宽的壳板，上下保留实体，中央区域留成贯通观察孔。
        parts.append(panel(cx,cz,y0,y0+3,facet,.4,"casing",angle))
        parts.append(panel(cx,cz,y1-3,y1,facet,.4,"casing",angle))
        for offset in (-1.6375,1.6375):
            tangent_x,tangent_z=math.cos(radians)*offset,math.sin(radians)*offset
            parts.append(panel(cx+tangent_x,cz+tangent_z,y0+3,y1-3,2.525,.4,"casing",angle))
        # 暗青色窄边框强调开孔。开孔本身没有覆盖面，转子可直接显露。
        for offset in (-.46,.46):
            tangent_x,tangent_z=math.cos(radians)*offset,math.sin(radians)*offset
            parts.append(panel(cx+tangent_x,cz+tangent_z,y0+3,y1-3,.10,.13,"glass",angle))
    return parts


def base():
    return [
        box((1,0,1),(15,2,15),"dark"),
        box((2,2,2),(14,4,14),"casing"),
        box((2,4,2),(14,5,14),"brass"),
        box((5,0,5),(11,1,11),"brass"),
        box((6,1,6),(10,2,10),"casing"),
        # 北面独有的小检修盖标出轴承维修朝向，与物料出口位置无关。
        panel(8,0.78,2.2,3.8,2.8,.44,"brass",0),
        panel(8,0.52,2.45,3.55,2.05,.20,"dark",0),
        box((6.55,3.0,.38),(6.9,3.35,.72),"brass"),
        box((9.1,3.0,.38),(9.45,3.35,.72),"brass"),
    ]


def upper_cap():
    cap = [box((2,11,2),(14,12,14),"brass"), box((3,12,3),(13,14,13),"casing"),
           box((5,14,5),(11,15,11),"dark"),
           # 八边顶帽由正交与斜向压板围出，中央留有浆料进料孔。
           box((5,15,5),(7,16,11),"brass"), box((9,15,5),(11,16,11),"brass"),
           box((7,15,5),(9,16,6),"brass"), box((7,15,10),(9,16,11),"brass"),
           box((6,14,6),(10,15,7),"casing"), box((6,14,9),(10,15,10),"casing"),
           box((6,14,7),(7,15,9),"casing"), box((9,14,7),(10,15,9),"casing")]
    return cap


def rotor():
    # 轴心(8,16,8)，坐标以lower底面为原点，转子可整体绕Y轴旋转。
    parts = [box((7.35,5,7.35),(8.65,27,8.65),"dark")]
    # 八根偏心立叶靠近观察窗内侧，让静态物品模型也能看清转鼓结构。
    for angle in range(0,360,45):
        radians=math.radians(angle); x=8+math.sin(radians)*3.4; z=8-math.cos(radians)*3.4
        parts.append(box((x-.38,8,z-.38),(x+.38,24,z+.38),"brass"))
    for y in (7,15,24):
        parts.extend([box((5.7,y,7.5),(10.3,y+.65,8.5),"brass"),
                      box((7.5,y,5.7),(8.5,y+.65,10.3),"brass")])
    # 四片窄立叶让转子能从四周的实体观察孔中看见，斜叶采用±45度实体旋转。
    parts.extend([box((7.6,8,4.6),(8.4,24,5.4),"casing"),box((7.6,8,10.6),(8.4,24,11.4),"casing"),
                  box((4.6,8,7.6),(5.4,24,8.4),"casing"),box((10.6,8,7.6),(11.4,24,8.4),"casing"),
                  box((6.8,8,6.8),(7.6,24,7.6),"casing",45),box((8.4,8,8.4),(9.2,24,9.2),"casing",45),
                  box((6.8,8,8.4),(7.6,24,9.2),"casing",-45),box((8.4,8,6.8),(9.2,24,7.6),"casing",-45)])
    # 跨两格转子的侧面UV按整根转轴高度映射到一张16×16贴图。
    return make_uv_explicit(parts, lambda value: (value - 5) * (16 / 22))


def translate_y(element, amount):
    moved = {**element,
             "from": [element["from"][0], element["from"][1] + amount, element["from"][2]],
             "to": [element["to"][0], element["to"][1] + amount, element["to"][2]]}
    if "rotation" in element:
        moved["rotation"] = {**element["rotation"],
                             "origin": [element["rotation"]["origin"][0],
                                        element["rotation"]["origin"][1] + amount,
                                        element["rotation"]["origin"][2]]}
    return moved


def json_model(elements, textures=None, parent="minecraft:block/block"):
    return {"parent": parent, "ambientocclusion": True,
            "textures": {"particle": "#casing", **(textures or {k: MOD+"enrichment_centrifuge_"+k for k in TEXTURES})},
            "elements": elements}


def model_bounds(elements):
    corners=[]
    for element in elements:
        lo,hi=element["from"],element["to"]
        for x in (lo[0],hi[0]):
            for y in (lo[1],hi[1]):
                for z in (lo[2],hi[2]):
                    point=(x,y,z)
                    turn=element.get("rotation")
                    if turn:
                        ox,oy,oz=turn["origin"]; angle=math.radians(turn["angle"])
                        dx,dz=x-ox,z-oz
                        point=(ox+dx*math.cos(angle)+dz*math.sin(angle),y,oz-dx*math.sin(angle)+dz*math.cos(angle))
                    corners.append(point)
    return [[round(min(p[i] for p in corners),3) for i in range(3)],
            [round(max(p[i] for p in corners),3) for i in range(3)]]


def validate_assets(geometry):
    expected={"lower":"enrichment_centrifuge","upper":"enrichment_centrifuge_upper",
              "item":"enrichment_centrifuge_item","rotor":"enrichment_centrifuge_rotor"}
    for part,name in expected.items():
        data=json.loads((ASSET/f"models/block/{name}.json").read_text(encoding="utf-8"))
        if len(data["elements"])!=len(geometry[part]): raise ValueError(f"{name}: 几何元素数量与生成预览不一致")
        for texture in data["textures"].values():
            if texture.startswith(MOD) and not (ASSET/"textures"/(texture.split(":",1)[1]+".png")).is_file():
                raise ValueError(f"{name}: 纹理引用缺失 {texture}")
        for element in data["elements"]:
            turn=element.get("rotation")
            if turn and turn["angle"] not in (-45,-22.5,22.5,45):
                raise ValueError(f"{name}: 不支持的元素角度 {turn['angle']}")
            if any("cullface" in face for face in element["faces"].values()):
                raise ValueError(f"{name}: 不允许固定方块面剔除非整格几何")
    states=json.loads((ASSET/"blockstates/enrichment_centrifuge.json").read_text(encoding="utf-8"))
    expected_states={f"half={half},facing={facing}" for half in ("lower","upper")
                     for facing in ("north","south","east","west")}
    if set(states["variants"])!=expected_states: raise ValueError("blockstate必须恰好包含八种half/facing组合")
    item_ref=json.loads((ASSET/"models/item/enrichment_centrifuge.json").read_text(encoding="utf-8"))
    if item_ref.get("parent")!="create_nuclear_industry:block/enrichment_centrifuge_item":
        raise ValueError("物品模型必须引用完整双格模型")
    for part,limit_y in (("lower",16),("upper",16),("item",32),("rotor",32)):
        bounds=model_bounds(geometry[part])
        if any(bounds[0][axis]<0 or bounds[1][axis]>(limit_y if axis==1 else 16)
               for axis in range(3)):
            raise ValueError(f"{part}: 几何越出约定占位 {bounds}")
    for kind in TEXTURES:
        image=Image.open(ASSET/f"textures/block/enrichment_centrifuge_{kind}.png")
        if image.size!=(16,16) or image.mode!="RGBA": raise ValueError(f"{kind}: PNG必须是16×16 RGBA")


def uv_diagnostics(model_names):
    """读取磁盘模型，按原版默认投影核对隐式UV及显式UV是否越界。"""
    result = {}
    for part, name in model_names.items():
        model = json.loads((ASSET / f"models/block/{name}.json").read_text(encoding="utf-8"))
        implicit = explicit = 0
        violations = []
        for element_index, element in enumerate(model["elements"]):
            for face, definition in element["faces"].items():
                values = definition.get("uv")
                if values is None:
                    implicit += 1
                    values = default_uv(element, face)
                else:
                    explicit += 1
                if any(value < 0 or value > 16 for value in values):
                    violations.append({"element": element_index, "face": face, "uv": values})
        result[part] = {"elements": len(model["elements"]), "explicit_faces": explicit,
                        "implicit_faces": implicit, "out_of_range_faces": len(violations),
                        "examples": violations[:4]}
    return result


def rotate_y(point, rotation):
    turn = math.radians(rotation["angle"])
    ox, _, oz = rotation["origin"]
    dx, dz = point[0] - ox, point[2] - oz
    return (ox + dx * math.cos(turn) + dz * math.sin(turn), point[1],
            oz - dx * math.sin(turn) + dz * math.cos(turn))


def shell_ring_report(model_name, y0, y1):
    """从磁盘模型抽取实际八片外侧面，验证朝向、周向射线覆盖和搭接。"""
    model = json.loads((ASSET / f"models/block/{model_name}.json").read_text(encoding="utf-8"))
    panels = []
    corner_indices = {"north": (0, 1, 2, 3), "south": (5, 4, 7, 6),
                      "west": (4, 0, 3, 7), "east": (1, 5, 6, 2)}
    face_normals = {"north": (0, 0, -1), "south": (0, 0, 1),
                    "west": (-1, 0, 0), "east": (1, 0, 0)}
    for element in model["elements"]:
        lo, hi = element["from"], element["to"]
        dims = sorted((abs(hi[0] - lo[0]), abs(hi[2] - lo[2])))
        if (abs(lo[1] - y0) > 1e-5 or abs(hi[1] - y1) > 1e-5
                or abs(dims[0] - .4) > 1e-5 or dims[1] < 5.7
                or len(element["faces"]) != 1):
            continue
        face, definition = next(iter(element["faces"].items()))
        if definition["texture"] != "#casing" or face not in corner_indices:
            continue
        vertices = [(lo[0], lo[1], lo[2]), (hi[0], lo[1], lo[2]),
                    (hi[0], hi[1], lo[2]), (lo[0], hi[1], lo[2]),
                    (lo[0], lo[1], hi[2]), (hi[0], lo[1], hi[2]),
                    (hi[0], hi[1], hi[2]), (lo[0], hi[1], hi[2])]
        rotation = element.get("rotation")
        if rotation:
            vertices = [rotate_y(point, rotation) for point in vertices]
        outer = [vertices[index] for index in corner_indices[face]]
        normal = face_normals[face]
        if rotation:
            angle = math.radians(rotation["angle"])
            normal = (normal[0] * math.cos(angle) + normal[2] * math.sin(angle), 0,
                      -normal[0] * math.sin(angle) + normal[2] * math.cos(angle))
        center = (sum(point[0] for point in outer) / 4 - 8,
                  sum(point[2] for point in outer) / 4 - 8)
        if normal[0] * center[0] + normal[2] * center[1] <= 0:
            raise ValueError(f"{model_name}: {face}外壳面法线朝内")
        ends = sorted({(point[0] - 8, point[2] - 8) for point in outer})
        if len(ends) != 2:
            raise ValueError(f"{model_name}: {face}外侧轮廓退化")
        dx, dz = ends[1][0] - ends[0][0], ends[1][1] - ends[0][1]
        length = math.hypot(dx, dz)
        tangent = (dx / length, dz / length)
        half_width = length / 2
        normal_len = math.hypot(normal[0], normal[2])
        n = (normal[0] / normal_len, normal[2] / normal_len)
        plane_distance = n[0] * center[0] + n[1] * center[1]
        panels.append({"normal": n, "distance": plane_distance,
                       "tangent": tangent, "center": center, "half_width": half_width,
                       "angle": (rotation or {}).get("angle", 0)})
    if len(panels) != 8:
        raise ValueError(f"{model_name}: 应有8片外壳面，实际找到{len(panels)}片")
    minimum_margin = float("inf")
    uncovered = []
    ray_count = 4096
    for ray_index in range(ray_count):
        angle = (ray_index + .5) * (2 * math.pi / ray_count)
        direction = (math.cos(angle), math.sin(angle))
        covered = []
        for panel in panels:
            dot = panel["normal"][0] * direction[0] + panel["normal"][1] * direction[1]
            if dot <= 0:
                continue
            distance = panel["distance"] / dot
            point = (direction[0] * distance, direction[1] * distance)
            along = ((point[0] - panel["center"][0]) * panel["tangent"][0]
                     + (point[1] - panel["center"][1]) * panel["tangent"][1])
            margin = panel["half_width"] - abs(along)
            if margin >= -1e-7:
                covered.append((distance, margin))
        if not covered:
            uncovered.append(ray_index)
            continue
        minimum_margin = min(minimum_margin, max(margin for _, margin in covered))
    ordered = sorted(panels, key=lambda p: math.atan2(p["normal"][1], p["normal"][0]))
    min_normal_angle = min(math.degrees(math.acos(max(-1, min(1,
        ordered[i]["normal"][0] * ordered[(i + 1) % 8]["normal"][0]
        + ordered[i]["normal"][1] * ordered[(i + 1) % 8]["normal"][1])))) for i in range(8))
    if min_normal_angle < 44.99:
        raise ValueError(f"{model_name}: 相邻外侧面可能共面并发生z-fighting ({min_normal_angle}度)")
    largest_gap_rays = 0
    if uncovered:
        doubled = uncovered + [ray + ray_count for ray in uncovered]
        run = 0
        previous = None
        for ray in doubled:
            run = run + 1 if previous is not None and ray == previous + 1 else 1
            largest_gap_rays = max(largest_gap_rays, min(run, ray_count))
            previous = ray
    return {"outer_faces": len(panels), "outward_normals": 8,
            "sampled_azimuth_rays": ray_count, "uncovered_rays": len(uncovered),
            "largest_uncovered_span_degrees": round(largest_gap_rays * 360 / ray_count, 6),
            "minimum_overlap_margin": round(minimum_margin, 6) if math.isfinite(minimum_margin) else None,
            "minimum_adjacent_normal_angle_degrees": round(min_normal_angle, 6),
            "cullface_count": 0, "degenerate_face_count": 0}


def svg_texture(kind, color):
    # 本机SVG保持现有导出器支持的直属整数rect子集。
    patterns = {
        "casing": [(0,0,16,16,"#59656A"),(0,0,16,1,"#78858A"),(0,1,1,14,"#647277"),
                   (15,1,1,14,"#364247"),(1,14,14,1,"#465258"),(3,4,1,8,"#69777C"),
                   (12,4,1,8,"#4B585D"),(6,2,4,1,"#7C898D"),(6,13,4,1,"#3B484D")],
        "brass": [(0,0,16,16,"#72532E"),(1,1,14,3,"#D0A55D"),(1,4,14,8,"#B88945"),
                  (1,12,14,3,"#946B38"),(3,6,2,2,"#D0A55D"),(11,6,2,2,"#D0A55D")],
        "glass": [(0,0,16,16,"#17292D"),(1,1,14,14,"#24464C"),(2,2,2,12,"#32818B"),
                  (4,2,1,12,"#55A3A6"),(5,2,8,1,"#30666C"),(5,13,8,1,"#1B363B")],
        "dark": [(0,0,16,16,"#202A2E"),(1,1,14,2,"#3D4A4F"),(1,13,14,2,"#121A1D"),
                 (2,4,12,8,"#29363A"),(3,5,1,6,"#526167"),(12,5,1,6,"#182226")],
    }
    rects = patterns[kind]
    body = "".join(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{fill}"/>' for x,y,w,h,fill in rects)
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges">{body}</svg>\n'


def install_assets(write_textures=True):
    lower = base() + drum(6,16)
    upper = drum(0,11) + upper_cap()
    make_uv_explicit(lower)
    make_uv_explicit(upper)
    rotor_elements = rotor()
    item = lower + [translate_y(el, 16) for el in upper] + rotor_elements
    (ASSET/"blockstates").mkdir(parents=True,exist_ok=True)
    (ASSET/"models/block").mkdir(parents=True,exist_ok=True)
    (ASSET/"models/item").mkdir(parents=True,exist_ok=True)
    variants = {}
    for half, model in (("lower","enrichment_centrifuge"),("upper","enrichment_centrifuge_upper")):
        for facing, angle in (("north",0),("east",90),("south",180),("west",270)):
            variants[f"half={half},facing={facing}"] = {"model":f"create_nuclear_industry:block/{model}", **({"y":angle} if angle else {})}
    (ASSET/"blockstates/enrichment_centrifuge.json").write_text(json.dumps({"variants":variants},indent=2)+"\n",encoding="utf-8")
    for name, elements in (("enrichment_centrifuge",lower),("enrichment_centrifuge_upper",upper),
                           ("enrichment_centrifuge_item",item),("enrichment_centrifuge_rotor",rotor_elements)):
        model=json_model(elements)
        if name.endswith("_rotor"):
            model["display"]={"gui":{"rotation":[25,45,0],"translation":[0,0,0],"scale":[0.72,0.72,0.72]}}
        if name.endswith("_item"):
            model["display"]={
                "gui":{"rotation":[25,225,0],"translation":[0,-1,0],"scale":[0.45,0.45,0.45]},
                "ground":{"rotation":[0,0,0],"translation":[0,2,0],"scale":[0.32,0.32,0.32]},
                "fixed":{"rotation":[0,0,0],"translation":[0,0,0],"scale":[0.35,0.35,0.35]},
                "thirdperson_righthand":{"rotation":[75,45,0],"translation":[0,2,0],"scale":[0.38,0.38,0.38]},
                "firstperson_righthand":{"rotation":[0,45,0],"translation":[0,0,0],"scale":[0.38,0.38,0.38]},
            }
        (ASSET/f"models/block/{name}.json" if not name.endswith("_item") else ASSET/"models/block/enrichment_centrifuge_item.json").write_text(json.dumps(model,indent=2)+"\n",encoding="utf-8")
    item_ref={"parent":"create_nuclear_industry:block/enrichment_centrifuge_item"}
    (ASSET/"models/item/enrichment_centrifuge.json").write_text(json.dumps(item_ref,indent=2)+"\n",encoding="utf-8")
    if write_textures:
        for kind,color in TEXTURES.items():
            source=ART/f"sources/block/enrichment_centrifuge_01d_{kind}.svg"
            source.parent.mkdir(parents=True,exist_ok=True)
            svg=svg_texture(kind,color)
            source.write_text(svg,encoding="utf-8")
            image=exporter.render_svg(svg,set(__import__("re").findall(r"#[0-9A-Fa-f]{6}",svg)),(16,16))
            image.save(ASSET/f"textures/block/enrichment_centrifuge_{kind}.png")
    return {"lower":lower,"upper":upper,"item":item,"rotor":rotor_elements}


FACE_COLORS={"casing":"#59656A","brass":"#C4934B","glass":"#387780","dark":"#29363A"}
FACES=(("north",(0,0,-1)),("south",(0,0,1)),("west",(-1,0,0)),("east",(1,0,0)),("up",(0,1,0)),("down",(0,-1,0)))


def render_json(elements, path, azimuth, title):
    width,height=560,560
    bg=np.empty((height,width,3),dtype=np.uint8); bg[:]=[235,237,235]
    depth_buffer=np.full((height,width),-np.inf,dtype=np.float32)
    color_buffer=bg.copy()
    a=math.radians(azimuth); elev=math.radians(25)
    ca,sa=math.cos(a),math.sin(a); ce,se=math.cos(elev),math.sin(elev)
    scale=10.5; cx,cy=width/2,height/2+28
    def transform(point):
        x,y,z=point[0]-8,point[1]-16,point[2]-8
        xr=x*ca-z*sa; zr=x*sa+z*ca
        screen_up=y*ce-zr*se
        camera_depth=zr*ce+y*se
        return (cx+xr*scale,cy-screen_up*scale,camera_depth)
    face_indices={"north":(0,1,2,3),"south":(5,4,7,6),"west":(4,0,3,7),
                  "east":(1,5,6,2),"up":(3,2,6,7),"down":(4,5,1,0)}
    normals={"north":(0,0,-1),"south":(0,0,1),"west":(-1,0,0),
             "east":(1,0,0),"up":(0,1,0),"down":(0,-1,0)}
    colors={"casing":(89,101,106),"brass":(196,147,75),"glass":(56,119,128),"dark":(41,54,58)}
    def raster_polygon(points,color):
        minx=max(0,int(math.floor(min(p[0] for p in points)))); maxx=min(width-1,int(math.ceil(max(p[0] for p in points))))
        miny=max(0,int(math.floor(min(p[1] for p in points)))); maxy=min(height-1,int(math.ceil(max(p[1] for p in points))))
        if minx>maxx or miny>maxy: return
        mask=Image.new("1",(maxx-minx+1,maxy-miny+1),0)
        ImageDraw.Draw(mask).polygon([(p[0]-minx,p[1]-miny) for p in points],fill=1)
        inside=np.asarray(mask,dtype=bool)
        xs=np.arange(minx,maxx+1,dtype=np.float32)+.5; ys=np.arange(miny,maxy+1,dtype=np.float32)+.5
        xx,yy=np.meshgrid(xs,ys)
        matrix=np.asarray([[p[0],p[1],1.0] for p in points[:3]],dtype=np.float64)
        try: a_depth,b_depth,c_depth=np.linalg.solve(matrix,np.asarray([p[2] for p in points[:3]],dtype=np.float64))
        except np.linalg.LinAlgError:return
        depths=a_depth*xx+b_depth*yy+c_depth
        old=depth_buffer[miny:maxy+1,minx:maxx+1]; visible=inside&(depths>old+1e-5)
        old[visible]=depths[visible]
        target=color_buffer[miny:maxy+1,minx:maxx+1]
        for channel in range(3): target[:,:,channel][visible]=color[channel]
    for element in elements:
        lo,hi=element["from"],element["to"]
        verts=[(lo[0],lo[1],lo[2]),(hi[0],lo[1],lo[2]),(hi[0],hi[1],lo[2]),(lo[0],hi[1],lo[2]),
               (lo[0],lo[1],hi[2]),(hi[0],lo[1],hi[2]),(hi[0],hi[1],hi[2]),(lo[0],hi[1],hi[2])]
        rot=element.get("rotation")
        if rot:
            ox,oy,oz=rot["origin"]; angle=math.radians(rot["angle"])
            verts=[(ox+(x-ox)*math.cos(angle)+(z-oz)*math.sin(angle), y,
                    oz-(x-ox)*math.sin(angle)+(z-oz)*math.cos(angle)) for x,y,z in verts]
        for face,indices in face_indices.items():
            points=[transform(verts[i]) for i in indices]
            normal=normals[face]
            if rot:
                turn=math.radians(rot["angle"])
                normal=(normal[0]*math.cos(turn)+normal[2]*math.sin(turn),normal[1],
                        -normal[0]*math.sin(turn)+normal[2]*math.cos(turn))
            nx=normal[0]*ca-normal[2]*sa; nz=normal[0]*sa+normal[2]*ca
            visible=nz*ce+normal[1]*se
            if visible<=0.015: continue
            if face not in element["faces"]: continue
            tex=element["faces"][face]["texture"].lstrip("#")
            rgb=colors.get(tex,(89,101,106)); shade=max(.68,min(1.2,.86+visible*.30))
            rgb=tuple(min(255,int(channel*shade)) for channel in rgb)
            raster_polygon(points,rgb)
    image=Image.fromarray(color_buffer,"RGB")
    draw=ImageDraw.Draw(image)
    try: font=ImageFont.truetype("arial.ttf",18)
    except OSError: font=ImageFont.load_default()
    draw.text((18,14),title,fill="#263238",font=font)
    draw.text((18,height-30),"JSON geometry and colors only; no game UV/light",fill="#435057",font=font)
    image.save(path)


def main():
    model_names={"lower":"enrichment_centrifuge","upper":"enrichment_centrifuge_upper",
                 "item":"enrichment_centrifuge_item","rotor":"enrichment_centrifuge_rotor"}
    parser = argparse.ArgumentParser()
    parser.add_argument("--diagnose-only", action="store_true", help="只读诊断磁盘模型并保存修前证据")
    parser.add_argument("--models-only", action="store_true", help="只生成JSON模型，不重导出SVG或PNG")
    args = parser.parse_args()
    if args.diagnose_only:
        report = {"phase": "before-fix", "uv": uv_diagnostics(model_names),
                  "shell_rings": {"lower_top": shell_ring_report("enrichment_centrifuge", 13, 16),
                                  "upper_bottom": shell_ring_report("enrichment_centrifuge_upper", 0, 3)}}
        EVIDENCE.mkdir(parents=True, exist_ok=True)
        (EVIDENCE / "pre-fix-diagnostics.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        print(json.dumps(report, ensure_ascii=False, indent=2))
        return
    geometry=install_assets(write_textures=not args.models_only)
    geometry={part:json.loads((ASSET/f"models/block/{name}.json").read_text(encoding="utf-8"))["elements"]
              for part,name in model_names.items()}
    validate_assets(geometry)
    uv_report = uv_diagnostics(model_names)
    bad_uv = {part: data["out_of_range_faces"] for part, data in uv_report.items()
              if data["out_of_range_faces"] or data["implicit_faces"]}
    if bad_uv:
        raise ValueError(f"生成模型仍含隐式或越界UV: {bad_uv}")
    shell_report = {"lower_top": shell_ring_report("enrichment_centrifuge", 13, 16),
                    "upper_bottom": shell_ring_report("enrichment_centrifuge_upper", 0, 3)}
    if any(data["uncovered_rays"] for data in shell_report.values()):
        raise ValueError(f"生成模型外壳仍有周向缺口: {shell_report}")
    EVIDENCE.mkdir(parents=True,exist_ok=True)
    render_json(geometry["item"],EVIDENCE/"centrifuge-final-front.png",210,"EXT-A-FUEL-01E | north service hatch")
    render_json(geometry["item"],EVIDENCE/"centrifuge-final-side.png",55,"EXT-A-FUEL-01E | side manifold view")
    summary={"asset":"enrichment_centrifuge","blockstate_variants":8,
             "elements":{"lower":len(geometry["lower"]),"upper":len(geometry["upper"]),
                         "item":len(geometry["item"]),"rotor":len(geometry["rotor"])},
             "bounds":{"lower":model_bounds(geometry["lower"]),"upper":model_bounds(geometry["upper"]),
                       "whole_item":model_bounds(geometry["item"]),"rotor":model_bounds(geometry["rotor"])},
             "rotor_axis_lower_local":[8,16,8],"preview":"geometry colors only; UV separately checked against 1.21.1 projection and 16x16 sprite bounds",
             "uv_diagnostics":uv_report,"shell_rings":shell_report,
             "pre_fix_uv_violations":{"item":168,"rotor":76},
             "display_scales":{"gui":0.45,"ground":0.32,"fixed":0.35,
                                "thirdperson_righthand":0.38,"firstperson_righthand":0.38}}
    (EVIDENCE/"geometry-check.json").write_text(json.dumps(summary,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(summary,ensure_ascii=False,indent=2))


if __name__=="__main__": main()
