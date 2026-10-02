"""生成并预览EXT-A-FUEL-01D离心机JSON几何，不依赖Minecraft运行时。"""
from __future__ import annotations

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
EVIDENCE = ROOT / "build/reports/extension/EXT-A-FUEL-01D-assets"
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
    facet = 5.8
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
    return parts


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


def install_assets():
    lower = base() + drum(6,16)
    upper = drum(0,11) + upper_cap()
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
    geometry=install_assets()
    model_names={"lower":"enrichment_centrifuge","upper":"enrichment_centrifuge_upper",
                 "item":"enrichment_centrifuge_item","rotor":"enrichment_centrifuge_rotor"}
    geometry={part:json.loads((ASSET/f"models/block/{name}.json").read_text(encoding="utf-8"))["elements"]
              for part,name in model_names.items()}
    validate_assets(geometry)
    EVIDENCE.mkdir(parents=True,exist_ok=True)
    render_json(geometry["item"],EVIDENCE/"centrifuge-final-front.png",210,"EXT-A-FUEL-01D | north service hatch")
    render_json(geometry["item"],EVIDENCE/"centrifuge-final-side.png",55,"EXT-A-FUEL-01D | side manifold view")
    summary={"asset":"enrichment_centrifuge","blockstate_variants":8,
             "elements":{"lower":len(geometry["lower"]),"upper":len(geometry["upper"]),
                         "item":len(geometry["item"]),"rotor":len(geometry["rotor"])},
             "bounds":{"lower":model_bounds(geometry["lower"]),"upper":model_bounds(geometry["upper"]),
                       "whole_item":model_bounds(geometry["item"]),"rotor":model_bounds(geometry["rotor"])},
             "rotor_axis_lower_local":[8,16,8],"preview":"final JSON cuboids and rotated elements; material colors only, no game UV/light"}
    (EVIDENCE/"geometry-check.json").write_text(json.dumps(summary,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(summary,ensure_ascii=False,indent=2))


if __name__=="__main__": main()
