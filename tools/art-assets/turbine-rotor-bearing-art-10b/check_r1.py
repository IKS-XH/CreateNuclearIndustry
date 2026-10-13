"""只核R1受影响的原B色源/像素/UV、批准切分覆盖与preR1几何身份，不重跑旧完整矩阵。"""
import hashlib
import json
import math
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

from PIL import Image, ImageDraw

from check_assets import cross, dot, group_audit, parse_obj, sub
from study_r1_uv import projection

ROOT=Path(__file__).resolve().parent
REPO=ROOT.parents[2]
REPORT=REPO/"build/reports/art/ART-TURBINE-10B/assets/r1"
SNAPSHOT=REPORT/"pre-r1"
CANDIDATE=ROOT/"candidate"
MODEL=Path("assets/create_nuclear_industry/models/block/turbine/mesh")
TEXTURES=Path("assets/create_nuclear_industry/textures/block/turbine")
STEEL_COLORS={"#657173","#7e898b","#596467","#667274","#9ba5a3","#41494b","#a8b1af","#454e50","#333b3d","#4c5658","#626d6f","#424b4d","#9ba5a4","#858f91","#697476","#899395","#c8cfcd"}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def area(points,normal):
    return sum(dot(cross(sub(points[i],points[0]),sub(points[i+1],points[0])),normal)/2 for i in range(1,len(points)-1))


def inside_plane(points,old_points,normal):
    """检查新增点仍在原凸面及其边界内，不将UV切分误变成实体外包修改。"""
    for p in points:
        if abs(dot(sub(p,old_points[0]),normal))>2e-7:
            return False
        for a,b in zip(old_points,old_points[1:]+old_points[:1]):
            if dot(cross(sub(b,a),sub(p,a)),normal)<-2e-7:
                return False
    return True


def geometry_protection(name,old,new):
    old_groups,new_groups=defaultdict(list),defaultdict(list)
    for face in old["faces"]:
        old_groups[face[0]].append(face)
    for face in new["faces"]:
        new_groups[face[0]].append(face)
    assert old_groups.keys()==new_groups.keys(),f"组身份变化: {name}"
    changed_groups=0
    new_faces=0
    for group,before in old_groups.items():
        after=new_groups[group]
        if not group.startswith("blade_"):
            signature=lambda f:(f[0],f[2],f[4])
            assert list(map(signature,before))==list(map(signature,after)),f"非叶片位置/法向/面变化: {name}/{group}"
            continue
        changed_groups+=1
        sum_area=[0]*len(before)
        for _,_,points,uv,normals in after:
            assert len(points)<=4
            geometric=cross(sub(points[1],points[0]),sub(points[2],points[0]))
            length=math.sqrt(dot(geometric,geometric))
            assert length>1e-10
            normal=tuple(v/length for v in geometric)
            assert all(dot(normal,n)>.999999 for n in normals)
            matches=[i for i,f in enumerate(before) if dot(f[4][0],normal)>.999999 and inside_plane(points,f[2],f[4][0])]
            assert len(matches)==1,f"切分片离开原面: {name}/{group}"
            index=matches[0]
            value=area(points,before[index][4][0])
            assert value>1e-10
            sum_area[index]+=value
        for index,face in enumerate(before):
            expected=area(face[2],face[4][0])
            assert abs(sum_area[index]-expected)<2e-6,f"切分覆盖面积不一致: {name}/{group}/{index}"
        # 只复核新增切分后的叶片闭合；轮毂/轴/支承原闭合证据复用。
        group_audit(after)
        new_faces+=len(after)
    old_bounds=[[min(p[i] for p in old["vertices"]),max(p[i] for p in old["vertices"])] for i in range(3)]
    new_bounds=[[min(p[i] for p in new["vertices"]),max(p[i] for p in new["vertices"])] for i in range(3)]
    assert all(abs(a-b)<1e-8 for axis_a,axis_b in zip(old_bounds,new_bounds) for a,b in zip(axis_a,axis_b)),name
    return {"non_blade_geometry_exact":True,"changed_blade_groups":changed_groups,"changed_blade_faces":new_faces,"same_bounds":new_bounds,"original_face_coverage":True}


def svg_png(name,reference=None):
    source=ROOT/"sources/textures"/("turbine_10b_"+name+".svg")
    tree=ET.parse(source).getroot()
    assert tree.attrib["viewBox"]=="0 0 16 16" and tree.attrib["shape-rendering"]=="crispEdges"
    image=Image.new("RGBA",(16,16))
    draw=ImageDraw.Draw(image)
    colors=set()
    for element in tree:
        assert element.tag.endswith("}rect")
        a=element.attrib
        x,y,w,h=int(a.get("x",0)),int(a.get("y",0)),int(a["width"]),int(a["height"])
        assert all(float(a.get(key,0))==int(a.get(key,0)) for key in ("x","y","width","height"))
        assert 0<=x<x+w<=16 and 0<=y<y+h<=16
        colors.add(a["fill"])
        draw.rectangle((x,y,x+w-1,y+h-1),fill=a["fill"])
    png=Image.open(CANDIDATE/TEXTURES/(source.stem+".png"))
    assert png.mode=="RGBA" and png.size==(16,16) and png.tobytes()==image.tobytes(),name
    if reference:
        original=REPO/"tools/art-assets/turbine-rotor-bearing-concepts-10/sources/textures"/reference
        assert sha(source)==sha(original)==sha(SNAPSHOT/"reference/sources/textures"/reference)
        assert len(colors)==(12 if name=="blade" else 10)
    elif name=="steel":
        assert colors==STEEL_COLORS
    elif name=="copper":
        assert colors=={"#b27a4e"}
    return {"colors":len(colors),"source_sha256":sha(source),"png_sha256":sha(CANDIDATE/TEXTURES/(source.stem+".png")),"pixel_identity":True,"reference_identity":bool(reference)}


def main():
    result={"task":"ART-TURBINE-10B-R1","installed":False,"checks":{},"textures":{},"geometry":{}}
    for name,reference in (("blade","B_rotor.svg"),("bearing","B_bearing.svg"),("steel",None),("copper",None)):
        result["textures"][name]=svg_png(name,reference)
    assert sha(REPO/"tools/art-assets/turbine-rotor-bearing-concepts-10/preview.js")==sha(SNAPSHOT/"reference/preview.js")
    result["checks"]["original_b_reference_svg_12_10_colors_and_pure_color_png_identity"]=True
    models={}
    for path in sorted((CANDIDATE/MODEL).glob("*.obj")):
        old=parse_obj(SNAPSHOT/"candidate"/MODEL/path.name)
        new=parse_obj(path)
        result["geometry"][path.stem]=geometry_protection(path.stem,old,new)
        models[path.stem]=new
        assert all(-1e-8<=value<=1+1e-8 for face in new["faces"] for uv in face[3] for value in uv),path
    old=parse_obj(SNAPSHOT/"source/comparison/rotor_blades_d3_before.obj")
    new=parse_obj(ROOT/"comparison/rotor_blades_d3_before.obj")
    result["geometry"]["tool_comparison_thick"]=geometry_protection("comparison",old,new)
    result["checks"]["approved_coplanar_caps_side_splits_cover_old_faces_and_remain_closed"]=True
    result["checks"]["all_non_blade_positions_normals_unchanged_bounds_retained"]=True
    result["checks"]["all_actual_vt_inside_sprite_0_1"]=True
    texture_images={name:Image.open(CANDIDATE/TEXTURES/("turbine_10b_"+name+".png")) for name in ("blade","bearing","steel","copper")}
    sample=lambda material,uv:texture_images[material].getpixel((min(15,max(0,int(uv[0]*16))),min(15,max(0,int(uv[1]*16)))))[:3]
    for name,model in models.items():
        radius={"rotor_blades_d3":1.1875,"rotor_blades_d5":2.1875,"rotor_blades_d7":3.1875,"turbine_rotor_unformed":.42}.get(name)
        for group,material,points,uv,normals in model["faces"]:
            if group.startswith("blade_") and abs(normals[0][2])>.999:
                assert material=="blade"
                row=int(group.split("_")[1][-1])
                number=int(group.split("_")[2][1:])
                expected=projection(points,row,number,radius)
                for actual,raw in zip(uv,expected):
                    # seam两边可分别取0/1，但非整数坐标必须严格等于原1格投影的小数部分。
                    assert all(abs((r-a)-round(r-a))<2e-7 for a,r in zip(actual,raw)),f"原B纹理密度/朝向不同: {name}/{group}"
            elif group in {"hub_lock_ring","hub_lock_pin","fixed_lock_ring","rotor_shaft"} or group.startswith("box_support_") or group.startswith("fixed_bolt_"):
                color="#7e898b" if group=="hub_lock_ring" else "#b27a4e" if "lock" in group else "#697476" if group=="rotor_shaft" else "#899395" if group.startswith("box_support_") else "#c8cfcd"
                expected=tuple(int(color[i:i+2],16) for i in (1,3,5))
                assert all(sample(material,p)==expected for p in uv),f"原B纯色分配不同: {name}/{group}"
            elif group=="end_plate" and material=="bearing":
                assert all(abs(u-x)<1e-7 and abs(v-y)<1e-7 for (u,v),(x,y,_) in zip(uv,points))
            elif group=="end_plate" and abs(normals[0][2])>.999:
                assert all(sample(material,p)==(76,86,88) for p in uv)
            elif group=="output_shaft" and abs(normals[0][2])<.999:
                assert all(abs(v-z)<1e-7 for (_,v),(_,_,z) in zip(uv,points))
    result["checks"]["original_one_block_blade_uv_outer_plate_xy_and_pure_color_assignment"]=True
    mtl=(CANDIDATE/MODEL/"turbine_rotor_bearing_10b.mtl").read_text()
    materials={line.split()[1] for line in mtl.splitlines() if line.startswith("newmtl ")}
    assert materials=={"steel","blade","bearing","copper"}
    for model in models.values():
        assert {face[1] for face in model["faces"]}<=materials
    for line in mtl.splitlines():
        if line.startswith("map_Kd "):
            namespace,path=line.split()[1].split(":")
            assert (CANDIDATE/f"assets/{namespace}/textures"/(path+".png")).is_file()
    for path in (CANDIDATE/MODEL.parent).glob("*.json"):
        data=json.loads(path.read_text())
        if "loader" in data:
            namespace,reference=data["model"].split(":")
            assert data["loader"]=="neoforge:obj" and data["flip_v"] is False
            assert (CANDIDATE/f"assets/{namespace}"/reference).is_file()
    result["checks"]["json_obj_mtl_png_references_same_four_texture_ids_flip_v_false"]=True
    current=json.loads((ROOT/"resource-map.json").read_text())
    before=json.loads((SNAPSHOT/"source/resource-map.json").read_text())
    assert len(current["resources"])==26
    assert {r["destination"] for r in current["resources"]}=={r["destination"] for r in before["resources"]}
    changed=[]
    for row in current["resources"]:
        assert sha(CANDIDATE/row["candidate"])==row["sha256"]
        if sha(SNAPSHOT/"candidate"/row["candidate"])!=row["sha256"]:
            changed.append(row["destination"])
        assert sha(REPO/row["destination"])==sha(SNAPSHOT/"installed"/row["destination"]),"生产文件被意外修改"
    result["changed_resources"]=changed
    assert len(changed)==14 and all(path.endswith((".obj",".png")) for path in changed)
    result["mapping_sha256"]=sha(ROOT/"resource-map.json")
    result["checks"]["26_path_boundary_current_installed_pre_r1_identity_unchanged"]=True
    (REPORT/"checks.json").write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"checks":len(result["checks"]),"candidate_models":10,"split_blade_groups":sum(r["changed_blade_groups"] for r in result["geometry"].values()),"changed_resources":len(changed),"pass":True,"installed":False,"mapping_sha256":result["mapping_sha256"]}))


if __name__=="__main__":
    main()
