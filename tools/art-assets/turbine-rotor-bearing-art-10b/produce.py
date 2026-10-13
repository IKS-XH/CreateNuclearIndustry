"""独立生成10B候选；仅写本工具目录，不安装正式资源、不调用旧全库producer。"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import shutil
import xml.etree.ElementTree as ET
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[2]
CANDIDATE = ROOT / "candidate"
NS = "create_nuclear_industry"
MODEL_DIR = f"assets/{NS}/models/block/turbine"
TEXTURE_DIR = f"assets/{NS}/textures/block/turbine"
MTL = "turbine_rotor_bearing_10b.mtl"
RADIUS = {3: 1.1875, 5: 2.1875, 7: 3.1875}
TEXTURES = {"steel": "turbine_10b_steel", "blade": "turbine_10b_blade",
            "bearing": "turbine_10b_bearing", "copper": "turbine_10b_copper"}
REFERENCE = REPO/"tools/art-assets/turbine-rotor-bearing-concepts-10/sources/textures"
# 纯色来自已选B稿的实际fill；UV取色块中心，避免把纹理拉满轮毂/轴圈/轴体。
STEEL_COLORS = ("#657173","#7e898b","#596467","#667274","#9ba5a3","#41494b",
                "#a8b1af","#454e50","#333b3d","#4c5658","#626d6f","#424b4d",
                "#9ba5a4","#858f91","#697476","#899395","#c8cfcd")


def dot(a, b):
    return sum(x * y for x, y in zip(a, b))


def cross(a, b):
    return (a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0])


def sub(a, b):
    return tuple(x-y for x, y in zip(a, b))


class Mesh:
    """离线OBJ容器；单位为格，中心(.5,.5)，每个命名实体的外向绕序独立闭合。"""

    def __init__(self, name):
        self.name, self.faces = name, []

    def face(self, points, material, group, target, uv=None):
        normal = cross(sub(points[1], points[0]), sub(points[2], points[0]))
        if dot(normal, target) < 0:
            points = list(reversed(points))
            normal = tuple(-x for x in normal)
            if uv is not None:
                uv = list(reversed(uv))
        length = math.sqrt(dot(normal, normal))
        if length < 1e-10:
            raise ValueError(f"零面积面: {self.name}/{group}")
        normal = tuple(x/length for x in normal)
        axis = max(range(3), key=lambda i: abs(normal[i]))
        u, v = {0:(2,1), 1:(0,2), 2:(0,1)}[axis]
        low_u, high_u = min(p[u] for p in points), max(p[u] for p in points)
        low_v, high_v = min(p[v] for p in points), max(p[v] for p in points)
        if uv is None:
            uv = [((p[u]-low_u)/max(high_u-low_u, 1e-8), (p[v]-low_v)/max(high_v-low_v, 1e-8)) for p in points]
        self.faces.append((group, material, points, uv, normal))

    def text(self):
        lines = [f"mtllib {MTL}", f"o {self.name}"]
        faces, n = [], 0
        last_group, last_material = None, None
        for group, material, points, uvs, normal in self.faces:
            if group != last_group:
                faces.append(f"g {group}")
                last_group = group
            if material != last_material:
                faces.append(f"usemtl {material}")
                last_material = material
            ids = []
            for p, uv in zip(points, uvs):
                n += 1
                lines.append("v " + " ".join(f"{x:.8f}" for x in p))
                lines.append("vt " + " ".join(f"{x:.8f}" for x in uv))
                lines.append("vn " + " ".join(f"{x:.8f}" for x in normal))
                ids.append(f"{n}/{n}/{n}")
            faces.append("f " + " ".join(ids))
        return "\n".join(lines+faces)+"\n"


def ring(mesh, outer, inner, z0, z1, material, group, center=(.5,.5), segments=24):
    """闭合圆柱或空心圈；孔壁法向朝孔内，前后盖分别朝-Z/+Z。"""
    cx, cy = center
    def p(r, i, z):
        a = i*2*math.pi/segments
        return (cx+r*math.cos(a), cy+r*math.sin(a), z)
    for i in range(segments):
        j, mid = (i+1)%segments, (i+.5)*2*math.pi/segments
        n = (math.cos(mid), math.sin(mid), 0)
        mesh.face([p(outer,i,z0),p(outer,j,z0),p(outer,j,z1),p(outer,i,z1)],material,group,n,[(i/segments,0),((i+1)/segments,0),((i+1)/segments,1),(i/segments,1)])
        if inner > 0:
            mesh.face([p(inner,i,z0),p(inner,i,z1),p(inner,j,z1),p(inner,j,z0)],material,group,tuple(-v for v in n),[(i/segments,0),(i/segments,1),((i+1)/segments,1),((i+1)/segments,0)])
        for z, sign in ((z0,-1),(z1,1)):
            points = [p(outer,i,z),p(outer,j,z),p(inner,j,z),p(inner,i,z)] if inner else [(cx,cy,z),p(outer,i,z),p(outer,j,z)]
            mesh.face(points,material,group,(0,0,sign),[((x-cx)/(outer*2)+.5,(y-cy)/(outer*2)+.5) for x,y,_ in points])


def hub(mesh, radius, inner=.155):
    """单体阶梯轮毂保留固定孔径；肩面只生成一次，避免阶梯段重叠盖。"""
    profile = [(radius*.82,.13),(radius*.82,.27),(radius,.27),(radius,.73),(radius*.82,.73),(radius*.82,.87)]
    count, group = 24, "stepped_hub"
    def p(r, i, z):
        a = i*2*math.pi/count
        return (.5+r*math.cos(a),.5+r*math.sin(a),z)
    for i in range(count):
        j, mid = (i+1)%count, (i+.5)*2*math.pi/count
        for (r0,z0),(r1,z1) in zip(profile,profile[1:]):
            target = (math.cos(mid),math.sin(mid),0) if z1 != z0 else (0,0,-1 if r1>r0 else 1)
            points=[p(r0,i,z0),p(r0,j,z0),p(r1,j,z1),p(r1,i,z1)]
            uv=[(i/count,(z0-.13)/.74),((i+1)/count,(z0-.13)/.74),((i+1)/count,(z1-.13)/.74),(i/count,(z1-.13)/.74)] if z1!=z0 else [((x-.5)/(radius*2)+.5,(y-.5)/(radius*2)+.5) for x,y,_ in points]
            mesh.face(points,"steel",group,target,uv)
        mesh.face([p(inner,i,.13),p(inner,i,.87),p(inner,j,.87),p(inner,j,.13)],"steel",group,(-math.cos(mid),-math.sin(mid),0),[(i/count,0),(i/count,1),((i+1)/count,1),((i+1)/count,0)])
        for z, sign in ((.13,-1),(.87,1)):
            points=[p(radius*.82,i,z),p(radius*.82,j,z),p(inner,j,z),p(inner,i,z)]
            mesh.face(points,"steel",group,(0,0,sign),[((x-.5)/(radius*2)+.5,(y-.5)/(radius*2)+.5) for x,y,_ in points])


def rotor(diameter, thickness=.125):
    """完整动态单格partial：24片双列错相叶片、轮毂及贯穿轴；全旋转扫掠不超R上限。"""
    mesh = Mesh(f"rotor_blades_d{diameter}")
    radius = RADIUS[diameter]
    ratio, hub_radius = radius/RADIUS[3], .52*radius/RADIUS[3]
    hub(mesh, hub_radius)
    ring(mesh,.14,0,0,1,"steel","rotor_shaft",segments=20)
    chord = .21*ratio
    half = chord/2
    start, end = hub_radius*.94, math.sqrt(radius*radius-half*half)*.985
    for row, center in enumerate((.30,.70)):
        z0, z1 = center-thickness/2, center+thickness/2
        for blade in range(12):
            angle = 2*math.pi*blade/12 + row*math.pi/12
            def p(r, side, z):
                a = angle+.09*r/radius
                return (.5+r*math.cos(a)-side*math.sin(a),.5+r*math.sin(a)+side*math.cos(a),z)
            front = [p(start,-half*.55,z0),p(end,-half,z0),p(end,half,z0),p(start,half*.55,z0)]
            back = [(x,y,z1) for x,y,_ in front]
            group = f"blade_row{row}_n{blade:02d}"
            mesh.face(front,"blade",group,(0,0,-1))
            mesh.face(back,"blade",group,(0,0,1))
            # XY截面正绕序，边界的右侧法向为实体外侧，不拿轴向法向猜边面绕序。
            for k in range(4):
                j = (k+1)%4
                edge = sub(front[j],front[k])
                mesh.face([front[k],front[j],back[j],back[k]],"steel",group,(edge[1],-edge[0],0))
    # 窄铜锁圈与偏置小锁钉随同一partial转动，不改变材料或玩法。
    ring(mesh,hub_radius*.825,hub_radius*.82,.15,.18,"copper","hub_lock_ring")
    ring(mesh,.035*ratio,0,.122,.132,"copper","hub_lock_pin",(.5+hub_radius*.62,.5+hub_radius*.18),8)
    return mesh


def plate(mesh, z0, z1):
    """厚3/16的方孔端板；分段边界匹配，孔半宽.19，四孔壁法向朝中心。"""
    h, group = .19, "end_plate"
    cuts = [-.5,-h,h,.5]
    strips = []
    for i in range(3):
        for j in range(3):
            if i == j == 1:
                continue
            x0,x1,y0,y1 = cuts[i],cuts[i+1],cuts[j],cuts[j+1]
            strips.append([(x0,y0),(x1,y0),(x1,y1),(x0,y1)])
    for strip in strips:
        for z, sign in ((z0,-1),(z1,1)):
            mesh.face([(.5+x,.5+y,z) for x,y in strip],"bearing",group,(0,0,sign),[(.5+x,.5+y) for x,y in strip])
    for side in range(4):
        for i in range(3):
            a,b = cuts[i],cuts[i+1]
            edge = [(-.5,a),(-.5,b)] if side==0 else [(.5,a),(.5,b)] if side==1 else [(a,-.5),(b,-.5)] if side==2 else [(a,.5),(b,.5)]
            target = [(-1,0,0),(1,0,0),(0,-1,0),(0,1,0)][side]
            mesh.face([(.5+x,.5+y,z) for z,x,y in [(z0,*edge[0]),(z0,*edge[1]),(z1,*edge[1]),(z1,*edge[0])]],"steel",group,target)
    hole = [(-h,-h),(h,-h),(h,h),(-h,h)]
    for i in range(4):
        a,b = hole[i],hole[(i+1)%4]
        mesh.face([(.5+x,.5+y,z) for z,x,y in [(z0,*a),(z0,*b),(z1,*b),(z1,*a)]],"steel",group,(-(a[0]+b[0])/2,-(a[1]+b[1])/2,0))


def box(mesh, low, high, material, group):
    x0,y0,z0 = low
    x1,y1,z1 = high
    points = [(x0,y0,z0),(x1,y0,z0),(x1,y1,z0),(x0,y1,z0),(x0,y0,z1),(x1,y0,z1),(x1,y1,z1),(x0,y1,z1)]
    for ids,n in [((0,1,2,3),(0,0,-1)),((4,5,6,7),(0,0,1)),((0,4,7,3),(-1,0,0)),((1,2,6,5),(1,0,0)),((0,1,5,4),(0,-1,0)),((3,7,6,2),(0,1,0))]:
        mesh.face([points[i] for i in ids],material,group,n)


def output(end, rotating=False):
    """前后端本格固定/旋转拆件；固定外座不转，旧外露轴范围0..1保持。"""
    front = end=="front"
    mesh = Mesh(f"output_shaft_{end}"+("_rotating" if rotating else ""))
    z0,z1 = (0,.1875) if front else (.8125,1)
    inner0,inner1 = (.1875,.2625) if front else (.7375,.8125)
    if rotating:
        ring(mesh,.14,0,0,1,"steel","output_shaft",segments=20)
        ring(mesh,.155,.14,inner0,inner1,"steel","rotating_inner_race")
    else:
        plate(mesh,z0,z1)
        ring(mesh,.27,.155,inner0,inner1,"steel","fixed_outer_race")
        # 箱座两条纵向支撑与小铜锁圈放在内侧；端板外面保留完整钢面及四个固定螺钉。
        for x in (.10,.90):
            box(mesh,(x-.045,.16,inner0),(x+.045,.84,inner1),"steel",f"box_support_{x}")
        copper0,copper1 = (inner1+.001,inner1+.009) if front else (inner0-.009,inner0-.001)
        ring(mesh,.272,.24,copper0,copper1,"copper","fixed_lock_ring")
        outer0,outer1 = (-.0075,0) if front else (1,1.0075)
        for i in range(4):
            a = math.pi/4+i*math.pi/2
            ring(mesh,.025,0,outer0,outer1,"steel",f"fixed_bolt_{i}",(.5+.39*math.cos(a),.5+.39*math.sin(a)),8)
    return mesh


def save(path, data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(data,encoding="utf-8",newline="\n")


def make_texture(name):
    """叶片/外板逐字复制B稿SVG；纯钢只作取色表，PNG由SVG矩形确定性栅格化。"""
    source = ROOT/"sources/textures"/(TEXTURES[name]+".svg")
    source.parent.mkdir(parents=True,exist_ok=True)
    if name in ("blade","bearing"):
        shutil.copyfile(REFERENCE/("B_rotor.svg" if name=="blade" else "B_bearing.svg"),source)
    else:
        rects=[(0,0,16,16,"#b27a4e" if name=="copper" else STEEL_COLORS[0])]
        if name=="steel":
            rects += [(i%5*3,i//5*3,3,3,color) for i,color in enumerate(STEEL_COLORS)]
            # 两个一像素取色列复现输出轴在端板厚度内银灰、内侧深钢的分区，不增加实体面。
            rects += [(14,0,1,3,"#858f91"),(14,3,1,13,"#697476"),
                      (15,0,1,13,"#697476"),(15,13,1,3,"#858f91")]
        svg = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges">\n'
        svg += "\n".join(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{color}"/>' for x,y,w,h,color in rects)+"\n</svg>\n"
        save(source,svg)
    image = Image.new("RGBA",(16,16))
    draw = ImageDraw.Draw(image)
    for element in ET.parse(source).getroot():
        a = element.attrib
        x,y,w,h = int(a.get("x",0)),int(a.get("y",0)),int(a["width"]),int(a["height"])
        draw.rectangle((x,y,x+w-1,y+h-1),fill=a["fill"])
    path = CANDIDATE/TEXTURE_DIR/(TEXTURES[name]+".png")
    path.parent.mkdir(parents=True,exist_ok=True)
    image.save(path,optimize=False)


def solid_uv(color, count):
    """每个实体面固定采样原B色块中心；无条纹密度、全圈贴图或atlas越界。"""
    if color=="#b27a4e":
        return [(.5,.5)]*count
    index=STEEL_COLORS.index(color)
    return [((index%5*3+1.5)/16,(index//5*3+1.5)/16)]*count


def blade_projection(points, group, radius):
    """原B纹理坐标：根部负弦角为原点，径向theta+.063/切向各以1格为一个16px周期。"""
    row=int(group.split("_")[1][-1])
    number=int(group.split("_")[2][1:])
    ratio=radius/1.1875
    start=.52*ratio*.94
    side=-.21*ratio/2*.55
    theta=number*2*math.pi/12+row*math.pi/12
    angle=theta+.09*start/radius
    origin=(.5+start*math.cos(angle)-side*math.sin(angle),.5+start*math.sin(angle)+side*math.cos(angle))
    radial=theta+.09*.7
    u=(math.cos(radial),math.sin(radial))
    v=(-math.sin(radial),math.cos(radial))
    result=[]
    for p in points:
        coords=[sum((p[i]-origin[i])*axis[i] for i in range(2)) for axis in (u,v)]
        result.append(tuple(round(value) if abs(value-round(value))<1e-9 else value for value in coords))
    return result


def clip_plane(polygon, group, radius, axis, value, keep_above):
    """只在原平面插值切纹理seam；顶点位置不离开原实体表面。"""
    if not polygon:
        return []
    result=[]
    coords=blade_projection(polygon,group,radius)
    for i,current in enumerate(polygon):
        previous=polygon[i-1]
        a,b=coords[i-1][axis],coords[i][axis]
        inside_a=(a>=value-1e-10) if keep_above else (a<=value+1e-10)
        inside_b=(b>=value-1e-10) if keep_above else (b<=value+1e-10)
        if inside_a!=inside_b:
            ratio=(value-a)/(b-a)
            result.append(tuple(previous[k]+ratio*(current[k]-previous[k]) for k in range(3)))
        if inside_b:
            result.append(current)
    cleaned=[]
    for p in result:
        if not cleaned or math.dist(cleaned[-1],p)>1e-9:
            cleaned.append(p)
    if len(cleaned)>1 and math.dist(cleaned[0],cleaned[-1])<1e-9:
        cleaned.pop()
    return cleaned


def blade_caps(mesh, group, points, normal, radius):
    """只跨V=0及D5/D7的U=1作共面面片；每个面片UV回到对应sprite的0..1。"""
    raw=blade_projection(points,group,radius)
    max_u=math.floor(max(p[0] for p in raw)-1e-9)
    min_v=math.floor(min(p[1] for p in raw)+1e-9)
    for tile_u in range(max_u+1):
        for tile_v in range(min_v,1):
            polygon=points
            for axis,low in ((0,tile_u),(1,tile_v)):
                polygon=clip_plane(polygon,group,radius,axis,low,True)
                polygon=clip_plane(polygon,group,radius,axis,low+1,False)
            if len(polygon)<3:
                continue
            # NeoForge OBJ烘焙最多取四个面角；五边形仅分解为同向三角形，不保留非法n-gon。
            pieces=[polygon] if len(polygon)<=4 else [[polygon[0],polygon[i],polygon[i+1]] for i in range(1,len(polygon)-1)]
            for piece in pieces:
                uv=[(min(1,max(0,u-tile_u)),min(1,max(0,v-tile_v))) for u,v in blade_projection(piece,group,radius)]
                mesh.face(piece,"blade",group,normal,uv)


def blade_side(mesh, group, points, normal, radius, color):
    """薄边只补端面seam触到的共线分点，保持原实体边闭合；各段继续采样同一纯钢色。"""
    unique=[]
    for x,y,_ in points:
        if (x,y) not in unique:
            unique.append((x,y))
    a,b=unique
    coords=blade_projection([(a[0],a[1],0),(b[0],b[1],0)],group,radius)
    cuts=[0,1]
    for axis,value in ((1,0),(0,1)):
        if min(coords[0][axis],coords[1][axis])+1e-9<value<max(coords[0][axis],coords[1][axis])-1e-9:
            cuts.append((value-coords[0][axis])/(coords[1][axis]-coords[0][axis]))
    cuts=sorted(set(round(value,12) for value in cuts))
    z0,z1=min(p[2] for p in points),max(p[2] for p in points)
    for low,high in zip(cuts,cuts[1:]):
        start=tuple(a[k]+low*(b[k]-a[k]) for k in range(2))
        end=tuple(a[k]+high*(b[k]-a[k]) for k in range(2))
        face=[(*start,z0),(*end,z0),(*end,z1),(*start,z1)]
        mesh.face(face,"steel",group,normal,solid_uv(color,4))


def apply_b_materials(source, radius=None):
    """仅替换material/UV及获准叶片共面分片；轮毂、轴、箱座和孔的原位置/法向逐面保持。"""
    target=Mesh(source.name)
    for group,material,points,uv,normal in source.faces:
        if group.startswith("blade_"):
            if abs(normal[2])>.999:
                blade_caps(target,group,points,normal,radius)
            else:
                # 用原叶片截面的四边次序识别原B端尖/正弦侧/其它深钢边，取色不靠网页重画。
                coords=blade_projection(points,group,radius)
                mid_u=sum(p[0] for p in coords)/len(coords)
                mid_v=sum(p[1] for p in coords)/len(coords)
                cap_group=[f for f in source.faces if f[0]==group and abs(f[4][2])>.999][0]
                cap_coords=blade_projection(cap_group[2],group,radius)
                high_u=max(p[0] for p in cap_coords)
                high_v=max(p[1] for p in cap_coords)
                low_v=min(p[1] for p in cap_coords)
                color="#a8b1af" if mid_u>high_u*.98 else "#454e50" if mid_v>(high_v+low_v)/2 else "#333b3d"
                blade_side(target,group,points,normal,radius,color)
            continue
        center=tuple(sum(p[i] for p in points)/len(points) for i in range(3))
        color="#657173"
        if group=="stepped_hub":
            if abs(normal[2])>.999:
                z=center[2]
                color="#7e898b" if abs(z-.13)<1e-8 else "#667274" if abs(z-.27)<1e-8 else "#9ba5a3" if abs(z-.73)<1e-8 else "#596467"
            elif dot(normal,(center[0]-.5,center[1]-.5,0))<0:
                color="#41494b"
            else:
                color="#7e898b" if center[2]<.27 else "#596467" if center[2]>.73 else "#657173"
        elif group=="rotor_shaft":
            color="#697476"
        elif group=="end_plate":
            rear=source.name.endswith("rear")
            outer=1 if rear else 0
            if abs(normal[2])>.999 and abs(center[2]-outer)<1e-8:
                target.faces.append((group,"bearing",points,[(x,y) for x,y,_ in points],normal))
                continue
            if abs(normal[2])>.999:
                color="#4c5658"
            else:
                color="#626d6f" if any(abs(p[k])<1e-8 or abs(p[k]-1)<1e-8 for p in points for k in range(2)) else "#424b4d"
        elif group=="fixed_outer_race":
            color="#657173" if abs(normal[2])>.999 else "#41494b" if dot(normal,(center[0]-.5,center[1]-.5,0))<0 else "#626d6f"
        elif group=="rotating_inner_race":
            color="#41494b" if abs(normal[2])<.999 and dot(normal,(center[0]-.5,center[1]-.5,0))<0 else "#9ba5a4"
        elif group=="output_shaft":
            if abs(normal[2])<.999:
                column=15 if "rear" in source.name else 14
                first=(points[0][0],points[0][1])
                band_uv=[((column+(.3 if (x,y)==first else .7))/16,z) for x,y,z in points]
                target.faces.append((group,"steel",points,band_uv,normal))
                continue
            color="#858f91" if abs(center[2]-(1 if "rear" in source.name else 0))<1e-8 else "#697476"
        elif group.startswith("box_support_"):
            color="#899395"
        elif group.startswith("fixed_bolt_"):
            color="#c8cfcd"
        elif group=="hub_lock_ring":
            # 10B已保留这个实体环，但原B轮毂只有偏置小铜钉；整周环恢复前段纯钢色。
            color="#7e898b"
        elif "lock" in group:
            color="#b27a4e"
        material="copper" if color=="#b27a4e" else "steel"
        target.faces.append((group,material,points,solid_uv(color,len(points)),normal))
    return target


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.parse_args()
    for name in TEXTURES:
        make_texture(name)
    materials = "\n\n".join(f"newmtl {name}\nKa 0.25 0.25 0.25\nKd 1 1 1\nmap_Kd {NS}:block/turbine/{texture}" for name,texture in TEXTURES.items())+"\n"
    save(CANDIDATE/MODEL_DIR/"mesh"/MTL,materials)
    meshes = {f"rotor_blades_d{d}":rotor(d) for d in RADIUS}
    source = meshes["rotor_blades_d3"]
    small = Mesh("turbine_rotor_unformed")
    scale = .42/RADIUS[3]
    for group,material,points,uvs,normal in source.faces:
        points = [(.5+(x-.5)*scale,.5+(y-.5)*scale,z) for x,y,z in points]
        small.faces.append((group,material,points,uvs,normal))
    meshes[small.name]=small
    for end in ("front","rear"):
        fixed,moving=output(end),output(end,True)
        meshes[fixed.name],meshes[moving.name]=fixed,moving
        unformed=Mesh("turbine_output_shaft_unformed"+("_rear" if end=="rear" else ""))
        unformed.faces=fixed.faces+moving.faces
        meshes[unformed.name]=unformed
    mapping=[]
    for name,mesh in meshes.items():
        radius=RADIUS[int(name[-1])] if name.startswith("rotor_blades_d") else .42 if name=="turbine_rotor_unformed" else None
        mesh=apply_b_materials(mesh,radius)
        save(CANDIDATE/MODEL_DIR/"mesh"/(name+".obj"),mesh.text())
        wrapper={"loader":"neoforge:obj","model":f"{NS}:models/block/turbine/mesh/{name}.obj","textures":{"particle":f"{NS}:block/turbine/{TEXTURES['blade' if 'rotor' in name else 'bearing']}"},"automatic_culling":False,"shade_quads":True,"flip_v":False,"emissive_ambient":False}
        save(CANDIDATE/MODEL_DIR/(name+".json"),json.dumps(wrapper,indent=2)+"\n")
    save(CANDIDATE/MODEL_DIR/"rotor_axle.json",json.dumps({"textures":{"particle":f"{NS}:block/turbine/{TEXTURES['steel']}"},"elements":[]},indent=2)+"\n")
    for path in sorted(CANDIDATE.rglob("*")):
        if path.is_file():
            relative=path.relative_to(CANDIDATE).as_posix()
            installed=REPO/"src/main/resources"/relative
            mapping.append({"candidate":relative,"destination":"src/main/resources/"+relative,"operation":"replace" if installed.is_file() else "add","sha256":hashlib.sha256(path.read_bytes()).hexdigest()})
    save(ROOT/"resource-map.json",json.dumps({"task":"ART-TURBINE-10B-R1","installed":False,"resources":mapping,"read_only_blockstates":["turbine_rotor","turbine_output_shaft"],"obsolete_unconsumed":["rotor_middle"]},indent=2)+"\n")
    # 只读比较稿是同一网格算法仅把真实厚度恢复为.34，绝不进入安装映射。
    comparison=apply_b_materials(rotor(3,.34),RADIUS[3])
    save(ROOT/"comparison/rotor_blades_d3_before.obj",comparison.text())
    save(ROOT/"comparison"/MTL,materials)
    print(json.dumps({"resource_count":len(mapping),"obj_count":len(meshes),"svg_count":4,"installed":False}))


if __name__=="__main__":
    main()
