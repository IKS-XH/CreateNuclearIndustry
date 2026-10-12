"""只读分析冻结叶片的原B纹理投影范围；不修改网格、UV或候选资源。"""
import json
import math
from pathlib import Path

from check_assets import parse_obj

ROOT=Path(__file__).resolve().parent
REPO=ROOT.parents[2]
REPORT=REPO/"build/reports/art/ART-TURBINE-10B/assets/r1"
SNAPSHOT=REPORT/"pre-r1/candidate/assets/create_nuclear_industry/models/block/turbine/mesh"


def projection(points, row, number, radius):
    """按原texturePattern的1格径向/切向基底求未折返UV，原点为负弦根角。"""
    ratio=radius/1.1875
    start=.52*ratio*.94
    side=-.21*ratio/2*.55
    theta=number*2*math.pi/12+row*math.pi/12
    a=theta+.09*start/radius
    origin=(.5+start*math.cos(a)-side*math.sin(a),.5+start*math.sin(a)+side*math.cos(a))
    radial=theta+.09*.7
    u=(math.cos(radial),math.sin(radial))
    v=(-math.sin(radial),math.cos(radial))
    return [(sum((p[i]-origin[i])*u[i] for i in range(2)),sum((p[i]-origin[i])*v[i] for i in range(2))) for p in points]


def main():
    result=[]
    for name,radius in (("rotor_blades_d3",1.1875),("rotor_blades_d5",2.1875),("rotor_blades_d7",3.1875),("turbine_rotor_unformed",.42)):
        faces=parse_obj(SNAPSHOT/(name+".obj"))["faces"]
        uv=[]
        cap_count=0
        crossing_caps=0
        for group,material,points,_,normals in faces:
            if not group.startswith("blade_") or abs(normals[0][2])<.999:
                continue
            row=int(group.split("_")[1][-1])
            number=int(group.split("_")[2][1:])
            coords=projection(points,row,number,radius)
            uv+=coords
            cap_count+=1
            crossing_caps+=any(math.floor(min(p[i] for p in coords)+1e-7)!=math.floor(max(p[i] for p in coords)-1e-7) for i in range(2))
        result.append({"model":name,"texture_unit_blocks":1,"cap_count":cap_count,"caps_crossing_integer_uv":crossing_caps,"u_min":min(p[0] for p in uv),"u_max":max(p[0] for p in uv),"v_min":min(p[1] for p in uv),"v_max":max(p[1] for p in uv)})
    (REPORT/"original-b-uv-study.json").write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(result))


if __name__=="__main__":
    main()
