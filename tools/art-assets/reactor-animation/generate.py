"""03R2确定性候选生成器：SVG是唯一低分辨率纹理来源，rig生成真实OBJ分件。

只生成独立工具目录；输入/几何/引用全部在内存验证完成后写出，无安装入口。
模型坐标以方块为单位，Y向上；法线采用外向右手绕序，UV与flip_v=false一致。
"""
from pathlib import Path
import argparse,importlib.util,io,json,math,re
from PIL import Image
HERE=Path(__file__).resolve().parent
NAMES=('reactor_fuel_rod','fuel_rod_glow','control_rod_shaft','control_rod_head')
TEXTURES=('fuel_rod_steel','fuel_rod_glow','control_rod')
SOURCES=tuple(n+'.svg' for n in TEXTURES)+tuple(f'coolant_{kind}/frame_{i:02d}.svg' for kind in ('cold','hot') for i in range(8))
spec=importlib.util.spec_from_file_location('animation_strict_svg',HERE.parent/'export.py');shared=importlib.util.module_from_spec(spec);spec.loader.exec_module(shared)

def encode(image):
    data=io.BytesIO();image.save(data,format='PNG',optimize=False);return data.getvalue()

def read_inputs(source_overrides=None,mapping_override=None,rig_override=None):
    """作者路径/帧数/消费ID固定，拒绝以mapping扩展写集；所有纹理先严格校验。"""
    mapping=mapping_override if mapping_override is not None else json.loads((HERE/'mapping.json').read_text(encoding='utf-8'))
    rig=rig_override if rig_override is not None else json.loads((HERE/'rig.json').read_text(encoding='utf-8'))
    palette=json.loads((HERE/'palette.json').read_text(encoding='utf-8'))
    if {p.relative_to(HERE/'sources').as_posix() for p in (HERE/'sources').rglob('*') if p.is_file()}!=set(SOURCES):raise ValueError('源路径须精确19份SVG')
    if source_overrides and not set(source_overrides).issubset(SOURCES):raise ValueError('源覆盖越过19源白名单')
    if set(palette)!=set(TEXTURES)|{'coolant_cold','coolant_hot'}:raise ValueError('色板须精确五组')
    for n,colors in palette.items():
        if not 2<=len(colors)<=16 or len(set(colors.values()))!=len(colors) or any(not re.fullmatch('#[0-9A-Fa-f]{6}',c) for c in colors.values()):raise ValueError(n+': 非法色板')
    if mapping.get('schema_version')!=1 or mapping.get('namespace')!='create_nuclear_industry' or len(mapping.get('models',[]))!=4:raise ValueError('mapping须四固定OBJ模型')
    for i,n in enumerate(NAMES):
        wanted=dict(name=n,target_model='create_nuclear_industry:block/'+('reactor_fuel_rod' if n=='reactor_fuel_rod' else 'reactor_animation/'+n),obj='create_nuclear_industry:models/block/reactor_animation/mesh/'+n+'.obj',material=TEXTURES[0] if i==0 else TEXTURES[1] if i==1 else TEXTURES[2])
        if i==1:wanted.update(radial_bias_baked_blocks=.0005,consumer_radial_scale=1)
        if mapping['models'][i]!=wanted:raise ValueError(n+': 模型/材质引用必须保持消费合同')
    if len(mapping.get('textures',[]))!=3 or len(mapping.get('fluids',[]))!=2:raise ValueError('须三模型材质与两流动帧表')
    for item,n in zip(mapping['textures'],TEXTURES):
        if item!=dict(name=n,target_sprite='create_nuclear_industry:block/reactor_animation/'+n,source='sources/'+n+'.svg',output='generated/textures/'+n+'.png'):raise ValueError('模型材质路径偏离白名单')
    for item,kind in zip(mapping['fluids'],('cold','hot')):
        n='coolant_'+kind
        wanted=dict(name=n,target_sprite='create_nuclear_industry:block/reactor_animation/'+n,source_pattern='sources/'+n+'/frame_{index:02d}.svg',sheet='generated/textures/'+n+'.png',frames=8,frame_width=16,frame_height=16,frametime_ticks=2,interpolate=True,animation_metadata={'animation':{'width':16,'height':16,'frametime':2,'interpolate':True,'frames':list(range(8))}})
        if item!=wanted:raise ValueError('流动帧表必须为固定8帧16×128及消费ID')
    if rig.get('model_units_per_block')!=16 or rig.get('fuel')!=dict(plate_xz=[0,16],bottom_y=[0,2],top_y=[14,16],centers_xz=[4,8,12],tube_diameter=3.5,tube_y=[2,14],sides=8):raise ValueError('燃料几何偏离两板九根八边管尺寸')
    c=rig.get('control',{})
    if any(c.get(k)!=v for k,v in dict(axis_xz=[8,8],shape='square',shaft_y_blocks=[0,1],shaft_side_blocks=.6,shaft_xz_blocks=[.2,.8],sides=4,head_y_units=[0,3],head_side_blocks=.65,head_xz_blocks=[.175,.825],pivot_blocks=[.5,0,.5]).items()):raise ValueError('控制棒须0.6格方杆、0.65格方箍与固定单位长度/3单位厚度')
    if rig.get('glow',{}).get('radial_bias_blocks')!=.0005 or rig.get('glow',{}).get('radial_bias_baked') is not True:raise ValueError('辉光偏置须显式锁定小径向量')
    if any(rig.get('fluid',{}).get(k)!=v for k,v in dict(frame_count=8,frame_pixels=[16,16],sheet_pixels=[16,128],phase_step_pixels=2).items()):raise ValueError('流动循环尺寸不符')
    images={}
    for key in SOURCES:
        name=key.split('/')[0].removesuffix('.svg');text=source_overrides[key] if source_overrides and key in source_overrides else (HERE/'sources'/key).read_text(encoding='utf-8')
        image=shared.render_svg(text,set(palette[name].values()));pixels=list(image.get_flattened_data())
        if len(set(pixels))>16 or any(p[3] not in (0,255) for p in pixels):raise ValueError(key+': RGBA≤16色/alpha0或255')
        if name!='fuel_rod_glow' and any(p[3]!=255 for p in pixels):raise ValueError(key+': 钢/杆/流动作者源必须全不透明')
        if name=='fuel_rod_glow' and (all(p[3]==0 for p in pixels) or all(p[3]==255 for p in pixels)):raise ValueError('辉光材质须有局部亮带与透明钢材保留区')
        images[key]=image
    # 每帧为上帧精确2px循环位移，07→00也遵守同一相位步；两方向可重复平铺。
    for kind in ('cold','hot'):
        frames=[images[f'coolant_{kind}/frame_{i:02d}.svg'] for i in range(8)]
        for i,image in enumerate(frames):
            if any(frames[(i+1)%8].getpixel((x,y))!=image.getpixel((x,(y-2)%16)) for y in range(16) for x in range(16)):raise ValueError('流动首尾相位不连续')
    return rig,mapping,images

def sub(a,b):return tuple(x-y for x,y in zip(a,b))
def cross(a,b):return (a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0])
def dot(a,b):return sum(x*y for x,y in zip(a,b))
def norm(a):
    length=math.sqrt(dot(a,a));return tuple(x/length for x in a)

class Mesh:
    """不重复反向面；每个面显式保存UV和外法线，几何可由OBJ再次独立读取。"""
    def __init__(self,name,material):self.name=name;self.material=material;self.faces=[]
    def face(self,group,points,outward,uv=None):
        uv=uv or ([(0,0),(1,0),(1,1),(0,1)] if len(points)==4 else [(0.5,0.5),(0,0),(1,0)])
        points=list(points);uv=list(uv);normal=norm(cross(sub(points[1],points[0]),sub(points[2],points[0])))
        if dot(normal,outward)<0:points.reverse();uv.reverse();normal=tuple(-v for v in normal)
        self.faces.append(dict(group=group,vertices=points,uv=uv,normal=normal))
    def box(self,group,lo,hi,uv=None):
        x0,y0,z0=lo;x1,y1,z1=hi
        for points,n in [([(x0,y0,z0),(x1,y0,z0),(x1,y1,z0),(x0,y1,z0)],(0,0,-1)),
            ([(x0,y0,z1),(x1,y0,z1),(x1,y1,z1),(x0,y1,z1)],(0,0,1)),
            ([(x0,y0,z0),(x0,y0,z1),(x0,y1,z1),(x0,y1,z0)],(-1,0,0)),
            ([(x1,y0,z0),(x1,y0,z1),(x1,y1,z1),(x1,y1,z0)],(1,0,0)),
            ([(x0,y0,z0),(x1,y0,z0),(x1,y0,z1),(x0,y0,z1)],(0,-1,0)),
            ([(x0,y1,z0),(x1,y1,z0),(x1,y1,z1),(x0,y1,z1)],(0,1,0))]:self.face(group,points,n,uv)
    def tube(self,group,x,z,y0,y1,r,uv=(0,0,1,1),caps=True):
        ring=[(x+r*math.cos(math.pi/8+i*math.pi/4),z+r*math.sin(math.pi/8+i*math.pi/4)) for i in range(8)]
        u0,v0,u1,v1=uv
        for i in range(8):
            a,b=ring[i],ring[(i+1)%8];mid=((a[0]+b[0])/2-x,0,(a[1]+b[1])/2-z)
            self.face(group,[(a[0],y0,a[1]),(b[0],y0,b[1]),(b[0],y1,b[1]),(a[0],y1,a[1])],mid,[(u0,v0),(u1,v0),(u1,v1),(u0,v1)])
            if caps:
                for y,n in ((y0,(0,-1,0)),(y1,(0,1,0))):
                    self.face(group,[(x,y,z),(a[0],y,a[1]),(b[0],y,b[1])],n,[(.5,.5),(.4,.4),(.6,.4)])
    def validate(self):
        seen=set()
        for face in self.faces:
            vertices=face['vertices'];n=norm(cross(sub(vertices[1],vertices[0]),sub(vertices[2],vertices[0])))
            if dot(n,face['normal'])<.999999:raise ValueError('OBJ绕序/法线不一致')
            if any(not 0<=value<=1 for pair in face['uv'] for value in pair):raise ValueError('OBJ UV越界')
            if any(not math.isfinite(value) for p in vertices for value in p):raise ValueError('OBJ非有限顶点')
            key=tuple(sorted(tuple(round(v,8) for v in p) for p in vertices))
            if key in seen:raise ValueError('OBJ重复/反向共面面')
            seen.add(key)
    def obj(self):
        self.validate();lines=[f'# 原创03A候选，方块单位，Y向上；外向绕序/UV/法线。',f'mtllib {self.name}.mtl',f'o {self.name}'];index=1
        for f in self.faces:
            for p in f['vertices']:lines.append('v '+' '.join(f'{v:.9f}' for v in p))
            for uv in f['uv']:lines.append('vt '+' '.join(f'{v:.9f}' for v in uv))
            for _ in f['vertices']:lines.append('vn '+' '.join(f'{v:.9f}' for v in f['normal']))
            lines += [f"g {f['group']}",f'usemtl {self.material}','f '+' '.join(f'{i}/{i}/{i}' for i in range(index,index+len(f['vertices'])))]
            index+=len(f['vertices'])
        return '\n'.join(lines)+'\n'

def meshes(rig):
    steel=Mesh('reactor_fuel_rod','fuel_rod_steel');glow=Mesh('fuel_rod_glow','fuel_rod_glow')
    # rig冻结模型单位，输出转为方块；厚板缩短管区而不挪动九个轴心。
    fuel=rig['fuel'];control=rig['control']
    steel.box('bottom_plate',(0,fuel['bottom_y'][0]/16,0),(1,fuel['bottom_y'][1]/16,1))
    steel.box('top_plate',(0,fuel['top_y'][0]/16,0),(1,fuel['top_y'][1]/16,1))
    radius=fuel['tube_diameter']/32;y0,y1=(value/16 for value in fuel['tube_y'])
    for x in fuel['centers_xz']:
        for z in fuel['centers_xz']:
            steel.tube(f'tube_{x}_{z}',x/16,z/16,y0,y1,radius,uv=(2/16,2/16,6/16,14/16))
            # 辉光只有侧面；逐管半径烘焙小径向偏置，轴心不变，消费者不再次缩放。
            glow.tube(f'glow_{x}_{z}',x/16,z/16,y0,y1,radius+rig['glow']['radial_bias_blocks'],caps=False)
    # 方柱四侧与两端独立闭合，轴线仍(.5,.5)；仅更换控制截面，燃料/辉光产物原字节保持。
    shaft=Mesh('control_rod_shaft','control_rod')
    lo,hi=control['shaft_xz_blocks'];y0,y1=control['shaft_y_blocks']
    shaft.box('absorber_unit',(lo,y0,lo),(hi,y1,hi),uv=[(0,0),(.5,0),(.5,1),(0,1)])
    head=Mesh('control_rod_head','control_rod')
    lo,hi=control['head_xz_blocks'];y0,y1=[y/16 for y in control['head_y_units']]
    head.box('brass_end_band',(lo,y0,lo),(hi,y1,hi),uv=[(.75,0),(1,0),(1,1),(.75,1)])
    return dict(zip(NAMES,(steel,glow,shaft,head)))

def candidate_bytes(rig,mapping,images):
    files={};models=meshes(rig)
    for name,mesh in models.items():
        files[HERE/f'generated/models/mesh/{name}.obj']=mesh.obj().encode('utf-8')
        files[HERE/f'generated/models/mesh/{name}.mtl']=(f'# 本项目原创候选；资源ID供后续正式安装解析。\nnewmtl {mesh.material}\nKa 0.25 0.25 0.25\nKd 1 1 1\nmap_Kd create_nuclear_industry:block/reactor_animation/{mesh.material}\n').encode('utf-8')
        model=dict(loader='neoforge:obj',model=f'create_nuclear_industry:models/block/reactor_animation/mesh/{name}.obj',textures={'particle':f'create_nuclear_industry:block/reactor_animation/{mesh.material}'},automatic_culling=False,shade_quads=name!='fuel_rod_glow',flip_v=False,emissive_ambient=name=='fuel_rod_glow')
        # 静态燃料块继承原版GUI/手持变换；动画partial保持独立世界坐标，无此parent。
        if name=='reactor_fuel_rod':model['parent']='minecraft:block/block'
        files[HERE/f'generated/models/{name}.json']=(json.dumps(model,indent=2)+'\n').encode('utf-8')
    for n in TEXTURES:files[HERE/f'generated/textures/{n}.png']=encode(images[n+'.svg'])
    for kind in ('cold','hot'):
        sheet=Image.new('RGBA',(16,128))
        for i in range(8):
            tile=images[f'coolant_{kind}/frame_{i:02d}.svg'];sheet.paste(tile,(0,16*i));files[HERE/f'generated/textures/coolant_{kind}/frame_{i:02d}.png']=encode(tile)
        files[HERE/f'generated/textures/coolant_{kind}.png']=encode(sheet)
    return files

def run(check=False,source_overrides=None,mapping_override=None,rig_override=None):
    rig,mapping,images=read_inputs(source_overrides,mapping_override,rig_override);files=candidate_bytes(rig,mapping,images)
    if any(not p.resolve().is_relative_to((HERE/'generated').resolve()) for p in files):raise ValueError('输出越过本generated')
    if check:
        if any(not p.exists() or p.read_bytes()!=data for p,data in files.items()):raise ValueError('候选与SVG/rig不一致')
        if {p for p in (HERE/'generated').rglob('*') if p.is_file()}!=set(files):raise ValueError('generated含额外产物')
        print('19SVG与33候选只读核对通过');return
    old={p:p.read_bytes() if p.exists() else None for p in files};touched=[]
    try:
        for p,data in files.items():
            if data==old[p]:continue
            p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data);touched.append(p)
    except Exception:
        for p in reversed(touched):
            if old[p] is None:p.unlink(missing_ok=True)
            else:p.write_bytes(old[p])
        raise
    print(f'19SVG→4OBJ/MTL/JSON+3材质+16帧+2帧表，改变{len(touched)}文件；无正式安装')

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');run(parser.parse_args().check)
