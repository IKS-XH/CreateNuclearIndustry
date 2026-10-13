"""只解析10B实际候选，检查闭合绕序、孔壁、扫掠、加载引用和SVG/PNG像素同源。"""
import hashlib
import json
import math
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[2]
CANDIDATE = ROOT/"candidate"
REPORT = REPO/"build/reports/art/ART-TURBINE-10B/assets"
MODEL = CANDIDATE/"assets/create_nuclear_industry/models/block/turbine"


def cross(a,b):
    return (a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0])


def dot(a,b):
    return sum(x*y for x,y in zip(a,b))


def sub(a,b):
    return tuple(x-y for x,y in zip(a,b))


def parse_obj(path):
    """读取生成文件自身；顶点/UV/法向的每个引用都必须真实存在。"""
    vertices,uvs,normals,faces=[],[],[],[]
    group,material,library=None,None,None
    for line in path.read_text(encoding="utf-8").splitlines():
        fields=line.split()
        if not fields:
            continue
        kind=fields[0]
        if kind=="mtllib":
            library=fields[1]
        elif kind=="g":
            group=fields[1]
        elif kind=="usemtl":
            material=fields[1]
        elif kind=="v":
            vertices.append(tuple(map(float,fields[1:])))
        elif kind=="vt":
            uvs.append(tuple(map(float,fields[1:])))
        elif kind=="vn":
            normals.append(tuple(map(float,fields[1:])))
        elif kind=="f":
            ids=[tuple(map(int,part.split("/"))) for part in fields[1:]]
            assert all(0<a<=len(vertices) and 0<b<=len(uvs) and 0<c<=len(normals) for a,b,c in ids), path
            faces.append((group,material,[vertices[a-1] for a,_,_ in ids],[uvs[b-1] for _,b,_ in ids],[normals[c-1] for _,_,c in ids]))
    assert library and (path.parent/library).is_file(),path
    return {"vertices":vertices,"faces":faces,"library":library}


def group_audit(faces):
    """坐标边的无向计数为2且有向计数相反；正体积证明闭合实体的总体外向绕序。"""
    edges,orientations,volume=Counter(),Counter(),0
    key=lambda p: tuple(round(v,7) for v in p)
    for _,_,points,uvs,normals in faces:
        geometric=cross(sub(points[1],points[0]),sub(points[2],points[0]))
        length=math.sqrt(dot(geometric,geometric))
        assert length>1e-10
        unit=tuple(v/length for v in geometric)
        assert all(dot(unit,n)>.999999 for n in normals),"OBJ法向与绕序不一致"
        assert all(-1e-7<=v<=1+1e-7 for uv in uvs for v in uv)
        for a,b in zip(points,points[1:]+points[:1]):
            a,b=key(a),key(b)
            edge=tuple(sorted((a,b)))
            edges[edge]+=1
            orientations[edge]+=1 if a<b else -1
        for i in range(1,len(points)-1):
            volume+=dot(points[0],cross(points[i],points[i+1]))/6
    assert all(n==2 for n in edges.values()),"实体边不闭合"
    assert all(n==0 for n in orientations.values()),"相邻实体面绕序不一致"
    assert volume>1e-9,"实体总体法向朝内"
    return {"faces":len(faces),"edges":len(edges),"closed":True,"signed_volume":volume}


def radius(points):
    return [math.hypot(x-.5,y-.5) for x,y,_ in points]


def near(a,b):
    return abs(a-b)<2e-7


def main():
    results={"task":"ART-TURBINE-10B","installed":False,"checks":{},"models":{}}
    parsed={}
    for path in sorted((MODEL/"mesh").glob("*.obj")):
        model=parse_obj(path)
        parsed[path.stem]=model
        groups=defaultdict(list)
        for face in model["faces"]:
            groups[face[0]].append(face)
        audits={g:group_audit(faces) for g,faces in groups.items()}
        vertices=model["vertices"]
        results["models"][path.stem]={"groups":audits,"bounds":[[min(p[i] for p in vertices),max(p[i] for p in vertices)] for i in range(3)]}
    assert len(parsed)==10
    results["checks"]["actual_obj_closed_winding_normals"]=True
    for d,limit in ((3,1.1875),(5,2.1875),(7,3.1875)):
        model=parsed[f"rotor_blades_d{d}"]
        assert max(radius(model["vertices"]))<=limit+1e-7
        groups=defaultdict(list)
        for g,_,points,_,_ in model["faces"]:
            groups[g]+=points
        assert len([g for g in groups if g.startswith("blade_")])==24
        for g,points in groups.items():
            if g.startswith("blade_"):
                row=int(g.split("_")[1][-1])
                lo,hi=min(p[2] for p in points),max(p[2] for p in points)
                assert near(hi-lo,.125) and near((hi+lo)/2,(.30,.70)[row])
        assert near(min(radius(groups["stepped_hub"])),.155)
        assert near(max(radius(groups["stepped_hub"])),.52*limit/1.1875)
        assert near(min(p[2] for p in groups["stepped_hub"]),.13)
        assert near(max(p[2] for p in groups["stepped_hub"]),.87)
        assert near(max(radius(groups["rotor_shaft"])),.14)
        # 对实际两列同号叶片坐标旋转15°后比较，避免仅检查数量却漏掉错相。
        angle=math.pi/12
        xy0={(round(.5+(x-.5)*math.cos(angle)-(y-.5)*math.sin(angle),6),round(.5+(x-.5)*math.sin(angle)+(y-.5)*math.cos(angle),6)) for x,y,_ in groups["blade_row0_n00"]}
        xy1={(round(x,6),round(y,6)) for x,y,_ in groups["blade_row1_n00"]}
        assert xy0==xy1
        results["models"][f"rotor_blades_d{d}"]["full_sweep_radius"]=max(radius(model["vertices"]))
        results["models"][f"rotor_blades_d{d}"]["sweep_limit"]=limit
        results["models"][f"rotor_blades_d{d}"]["shell_clearance_at_limit"]=.125
    results["checks"]["three_tier_sweep_thin_rows_fixed_hub_bore"]=True
    small=parsed["turbine_rotor_unformed"]["vertices"]
    original=parsed["rotor_blades_d3"]["vertices"]
    assert len(small)==len(original)
    scale=.42/1.1875
    assert all(near(a[0],.5+(b[0]-.5)*scale) and near(a[1],.5+(b[1]-.5)*scale) and near(a[2],b[2]) for a,b in zip(small,original))
    assert max(radius(small))<=.42
    assert json.loads((MODEL/"rotor_axle.json").read_text())["elements"]==[]
    results["checks"]["complete_unformed_scaled_from_d3_empty_located_base"]=True
    for end in ("front","rear"):
        fixed=parsed[f"output_shaft_{end}"]
        rotating=parsed[f"output_shaft_{end}_rotating"]
        groups=defaultdict(list)
        for g,_,points,_,_ in fixed["faces"]:
            groups[g]+=points
        assert not any(g in {"output_shaft","rotating_inner_race"} for g in groups)
        zvalues=[p[2] for p in groups["end_plate"]]
        assert near(max(zvalues)-min(zvalues),.1875)
        platefaces=[f for f in fixed["faces"] if f[0]=="end_plate"]
        for _,_,points,_,normals in platefaces:
            if all(near(p[2],min(zvalues)) for p in points):
                assert normals[0][2]<-.999
            elif all(near(p[2],max(zvalues)) for p in points):
                assert normals[0][2]>.999
            # 四个孔壁必须向中心，不能把孔壁当外壁或封掉整个孔。
            elif all(near(p[0],.31) or near(p[0],.69) for p in points) and near(max(p[0] for p in points),min(p[0] for p in points)):
                assert dot(normals[0],(.5-points[0][0],0,0))>0
            elif all(near(p[1],.31) or near(p[1],.69) for p in points) and near(max(p[1] for p in points),min(p[1] for p in points)):
                assert dot(normals[0],(0,.5-points[0][1],0))>0
        assert near(min(radius(groups["fixed_outer_race"])),.155)
        assert near(max(radius(groups["fixed_outer_race"])),.27)
        moving_groups={f[0] for f in rotating["faces"]}
        assert moving_groups=={"output_shaft","rotating_inner_race"}
        collar_points=[p for g,_,points,_,_ in rotating["faces"] if g=="rotating_inner_race" for p in points]
        assert near(min(radius(collar_points)),.14) and near(max(radius(collar_points)),.155)
        assert near(min(p[2] for p in rotating["vertices"]),0) and near(max(p[2] for p in rotating["vertices"]),1)
        old=REPO/"src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh"/(f"output_shaft_{end}.obj")
        old_points=[tuple(map(float,line.split()[1:])) for line in old.read_text().splitlines() if line.startswith("v ")]
        assert near(min(p[2] for p in old_points),0) and near(max(p[2] for p in old_points),1)
    results["checks"]["front_rear_plate_hole_normals_stationary_rotating_split"]=True
    mtl=(MODEL/"mesh/turbine_rotor_bearing_10b.mtl").read_text()
    materials={line.split()[1] for line in mtl.splitlines() if line.startswith("newmtl ")}
    for model in parsed.values():
        assert {f[1] for f in model["faces"]}<=materials
    for line in mtl.splitlines():
        if line.startswith("map_Kd "):
            asset=line.split()[1]
            namespace,path=asset.split(":")
            assert (CANDIDATE/f"assets/{namespace}/textures"/(path+".png")).is_file()
    for path in MODEL.glob("*.json"):
        data=json.loads(path.read_text())
        if "loader" in data:
            namespace,reference=data["model"].split(":")
            assert data["loader"]=="neoforge:obj"
            assert (CANDIDATE/f"assets/{namespace}"/reference).is_file()
    results["checks"]["json_obj_mtl_texture_references"]=True
    for path in sorted((ROOT/"sources/textures").glob("*.svg")):
        svg=ET.parse(path).getroot()
        assert svg.attrib["viewBox"]=="0 0 16 16" and svg.attrib["shape-rendering"]=="crispEdges"
        expected=Image.new("RGBA",(16,16))
        draw=ImageDraw.Draw(expected)
        colors=set()
        for element in svg:
            assert element.tag.endswith("}rect")
            a=element.attrib
            assert all(str(int(a[k]))==a[k] for k in ("x","y","width","height"))
            x,y,w,h=(int(a[k]) for k in ("x","y","width","height"))
            assert 0<=x<x+w<=16 and 0<=y<y+h<=16
            colors.add(a["fill"])
            draw.rectangle((x,y,x+w-1,y+h-1),fill=a["fill"])
        actual=Image.open(CANDIDATE/"assets/create_nuclear_industry/textures/block/turbine"/(path.stem+".png"))
        assert len(colors)<=8 and actual.mode=="RGBA" and actual.size==(16,16)
        assert actual.tobytes()==expected.tobytes()
    results["checks"]["four_svg_integer_rect_palette_png_pixel_identity"]=True
    mapping=json.loads((ROOT/"resource-map.json").read_text())
    assert len(mapping["resources"])==26
    for resource in mapping["resources"]:
        path=CANDIDATE/resource["candidate"]
        assert hashlib.sha256(path.read_bytes()).hexdigest()==resource["sha256"]
        assert not any(word in resource["candidate"] for word in ("casing","window","turbine.mtl","rotor_middle"))
    results["checks"]["26_exact_candidate_mapping_hashes_no_shared_or_shell_writes"]=True
    REPORT.mkdir(parents=True,exist_ok=True)
    (REPORT/"checks.json").write_text(json.dumps(results,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"checks":len(results["checks"]),"models":len(parsed),"groups":sum(len(m["groups"]) for m in results["models"].values()),"pass":True,"installed":False},ensure_ascii=False))


if __name__=="__main__":
    main()
