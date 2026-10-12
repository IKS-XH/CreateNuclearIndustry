"""从锁定Create像素生成SVG，再按SVG矩形确定性导出；只写本专项资源。"""
import argparse, hashlib, io, json, zipfile
from pathlib import Path
import xml.etree.ElementTree as ET
from PIL import Image, PngImagePlugin
ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
JAR = Path.home() / '.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280/92471e8fc5ed4e3c2279001d77ebcec6fd38bcab/create-1.21.1-6.0.10-280-slim.jar'
ASSETS = ROOT / 'src/main/resources/assets/create_nuclear_industry'
FAMILY = 'industrial_fluid_visual_01'
DIRECTIONS = ['down', 'up', 'north', 'south', 'west', 'east']
def save(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
def recolor(pixel, tone):
    r, g, b, a = pixel
    # 保持原亮度次序和alpha；钢灰为冷灰，陶瓷适度提亮，保留全部阴影与像素边线。
    luminance = (54*r + 183*g + 19*b + 128)//256
    channels = (max(0, luminance-10), luminance, min(255, luminance+8)) if tone == 'steel' else (min(255, 28+luminance*89//100), min(255, 30+luminance*88//100), min(255, 33+luminance*87//100))
    return (*channels, a)
def svg_pixels(image, name):
    width, height = image.size
    lines = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" shape-rendering="crispEdges">', '<!-- Create原像素调色派生，版权归The Create Team / The Creators of Create；来源见sources.json。 -->']
    for y in range(height):
        for x in range(width):
            r,g,b,a = image.getpixel((x,y))
            lines.append(f'<rect x="{x}" y="{y}" width="1" height="1" fill="#{r:02x}{g:02x}{b:02x}" data-alpha="{a}" fill-opacity="{a/255:.8f}"/>')
    lines.append('</svg>')
    path = HERE / 'svg' / (name + '.svg'); path.parent.mkdir(exist_ok=True)
    path.write_text('\n'.join(lines)+'\n', encoding='utf-8')
    return path
def export_svg(path):
    svg = ET.parse(path).getroot(); image = Image.new('RGBA', (int(svg.attrib['width']), int(svg.attrib['height'])))
    for rect in svg:
        if not rect.tag.endswith('rect'): continue
        color = rect.attrib['fill'][1:]; pixel = tuple(int(color[i:i+2],16) for i in (0,2,4)) + (int(rect.attrib['data-alpha']),)
        image.putpixel((int(rect.attrib['x']),int(rect.attrib['y'])),pixel)
    out = ASSETS / 'textures/block' / FAMILY / (path.stem+'.png'); out.parent.mkdir(parents=True,exist_ok=True); metadata=PngImagePlugin.PngInfo(); metadata.add_text("Copyright", "The Create Team / The Creators of Create; output_arrow authored for Create Nuclear Industry"); metadata.add_text("License", "Create-derived textures: All Rights Reserved"); metadata.add_text("Source", "https://github.com/Creators-of-Create/Create/tree/mc1.21.1-6.0.10/src/main/resources/assets/create"); image.save(out, compress_level=9, pnginfo=metadata)
    return image

PUMP_PATCH = {'region_px':[3,19,15,27], 'pixel_count':96, 'before_rgba':[0,0,8,255], 'after_rgba':[116,126,134,255], 'reason':'仅消除COG大面近黑实体孔形成的方向伪图案，alpha与其他像素保持'}
def neutralize_pump_cog_face(image):
    """像素坐标使用32×32原图单位；只改已核COG面上的96个不透明近黑点，不改其余纹理。"""
    for y in range(19,27):
        for x in range(3,15):
            assert image.getpixel((x,y)) == (0,0,8,255), '锁定Create泵补片前像素身份变化'
            image.putpixel((x,y),(116,126,134,255))
def pump_only():
    """只从本张已保存SVG导出PNG，并更新本张派生记录；其他资源和模型不生成。"""
    export_svg(HERE/'svg/steel_pump.svg')
    sources=json.loads((HERE/'sources.json').read_text(encoding='utf-8'))
    sources['textures']['steel_pump']['derived_patch']=PUMP_PATCH
    save(HERE/'sources.json',sources)
    print('steel_pump单张SVG导出完成，96点补片，其他资源未生成。')

def produce():
    sources = {'dependency':str(JAR), 'dependency_sha256':hashlib.sha256(JAR.read_bytes()).hexdigest(), 'version':'Create 6.0.10-280 / Minecraft 1.21.1', 'assets_license':'All Rights Reserved', 'copyright':'The Create Team / The Creators of Create', 'license_url':'https://github.com/Creators-of-Create/Create/blob/mc1.21.1-6.0.10/LICENSE.md', 'models':{}, 'textures':{}}
    with zipfile.ZipFile(JAR) as z:
        def read(path): return json.loads(z.read(path))
        for texture in ['pipes', 'pipes_connected', 'pump', 'fluid_valve']:
            raw = z.read('assets/create/textures/block/'+texture+'.png'); original = Image.open(io.BytesIO(raw)).convert('RGBA')
            for tone in ['steel','ceramic'] if texture != 'pump' else ['steel']:
                image = original.copy(); image.putdata([recolor(p,tone) for p in original.get_flattened_data()])
                if texture == 'pump': neutralize_pump_cog_face(image)
                name = tone+'_'+texture; actual = export_svg(svg_pixels(image,name)); assert actual.tobytes()==image.tobytes()
                assert [p[3] for p in actual.get_flattened_data()]==[p[3] for p in original.get_flattened_data()]
                sources['textures'][name]={'source':'assets/create/textures/block/'+texture+'.png','source_sha256':hashlib.sha256(raw).hexdigest(),'size':list(original.size),'alpha_preserved':True}
                if texture == 'pump': sources['textures'][name]['derived_patch']=PUMP_PATCH
        def tex(tone,name): return 'create_nuclear_industry:block/'+FAMILY+'/'+tone+'_'+name
        def wrap(original,tone):
            suffix = original.removeprefix('create:block/')
            model = read('assets/create/models/block/'+suffix+'.json')
            overrides={}
            # 变量键按上游模型实际解析；上游父模型的0/1也一并覆盖，几何和UV始终继承原件。
            for key,value in model.get('textures',{}).items():
                if value in ['create:block/pipes','create:block/pipes_connected','create:block/pump','create:block/fluid_valve']:
                    overrides[key]=tex(tone,value.split('/')[-1])
            if suffix.startswith('fluid_pipe/'):
                # 依真实父链读取0的来源；casing使用pipes，连接中央面使用pipes_connected，不能凭目录深度猜。
                effective = dict(model.get('textures', {})); ancestor = model
                while ancestor.get('parent', '').startswith('create:block/'):
                    ancestor = read('assets/create/models/' + ancestor['parent'].split(':',1)[1] + '.json')
                    effective = dict(ancestor.get('textures', {})) | effective
                if '0' in effective: overrides['0']=tex(tone,effective['0'].split('/')[-1])
                overrides.update({'1':tex(tone,'pipes'), 'particle':tex(tone,'pipes')})
            elif suffix.startswith('mechanical_pump/'): overrides['particle']=tex('steel','pump')
            else: overrides['particle']=tex(tone,'fluid_valve')
            local=FAMILY+'/'+tone+'/'+suffix
            wrapper={'parent':original,'textures':overrides}
            # 默认solid会让原纹理alpha0窗口写深度；锁定NeoForge内建minecraft层只用于阀body/item。
            if suffix in ['fluid_valve/block_vertical_open','fluid_valve/block_horizontal_open','fluid_valve/item']: wrapper['render_type']='minecraft:cutout_mipped'
            save(ASSETS/'models/block'/(local+'.json'), wrapper)
            sources['models'][local]={'parent':original,'source_sha256':hashlib.sha256(z.read('assets/create/models/block/'+suffix+'.json')).hexdigest()}
            return 'create_nuclear_industry:block/'+local
        # 两个不同轴的连接意味着弯头/分支；其余0/1/两端直线只选择原完整静止段，与原multipart/RIM互斥。
        orthogonal = {'OR':[{a:'true',b:'true'} for i,a in enumerate(DIRECTIONS) for b in DIRECTIONS[i+1:] if DIRECTIONS.index(a)//2 != DIRECTIONS.index(b)//2]}
        for name,tone in [('reinforced_fluid_pipe','steel'),('ceramic_fluid_pipe','ceramic')]:
            state=read('assets/create/blockstates/fluid_pipe.json')
            for part in state['multipart']:
                part['apply']['model']=wrap(part['apply']['model'],tone)
                part['when']={'AND':[part['when'],orthogonal]}
            for direction in DIRECTIONS:
                for family in ['rim_connector','rim']:
                    state['multipart'].append({'when':{'AND':[{direction:'true'},orthogonal]},'apply':{'model':wrap('create:block/fluid_pipe/'+family+'/'+direction,tone)}})
            complete=wrap('create:block/fluid_pipe/item',tone)
            state['multipart'].append({'when':dict.fromkeys(DIRECTIONS,'false'),'apply':{'model':complete}})
            for axis, pair, rotation in [('x',['west','east'],{'y':90}),('y',['down','up'],{'x':90}),('z',['north','south'],{})]:
                condition={'AND':[dict.fromkeys([d for d in DIRECTIONS if d not in pair],'false'),{'OR':[{d:'true'} for d in pair]}]}
                state['multipart'].append({'when':condition,'apply':{'model':complete,**rotation}})
            # 原shouldDrawCasing：存在某轴之外超过两个连接，非共面三向(one-per-axis)不补壳。
            casing_conditions=[]
            for mask in range(64):
                if any(sum(bool(mask & (1<<i)) for i,d in enumerate(DIRECTIONS) if i//2 != axis)>2 for axis in range(3)):
                    casing_conditions.append({d:str(bool(mask & (1<<i))).lower() for i,d in enumerate(DIRECTIONS)})
            state['multipart'].append({'when':{'OR':casing_conditions},'apply':{'model':wrap('create:block/fluid_pipe/casing',tone)}})
            save(ASSETS/'blockstates'/(name+'.json'),state)
            save(ASSETS/'models/block'/(name+'.json'),{'parent':complete})
            save(ASSETS/'models/block'/(name+'_arm.json'),{'parent':wrap('create:block/fluid_pipe/rim_connector/north',tone)})
            save(ASSETS/'models/item'/(name+'.json'),{'parent':complete})
        state=read('assets/create/blockstates/mechanical_pump.json')
        for variant in state['variants'].values(): variant['model']=wrap(variant['model'],'steel')
        save(ASSETS/'blockstates/industrial_fluid_pump.json',state)
        save(ASSETS/'models/block/industrial_fluid_pump.json',{'parent':wrap('create:block/mechanical_pump/block','steel')})
        save(ASSETS/'models/item/industrial_fluid_pump.json',{'parent':wrap('create:block/mechanical_pump/item','steel')})
        wrap('create:block/mechanical_pump/cog','steel')
        for name,tone in [('reinforced_fluid_valve','steel'),('ceramic_fluid_valve','ceramic')]:
            variants=[]
            # 当前只存在管轴AXIS；原horizontal模型管轴Y，原vertical模型管轴X，Z由其Y旋转得到。
            for axis,original,y in [('x','block_vertical_open',0),('y','block_horizontal_open',0),('z','block_vertical_open',90)]:
                apply={'model':wrap('create:block/fluid_valve/'+original,tone)}
                if y: apply['y']=y
                variants.append({'when':{'axis':axis},'apply':apply})
            save(ASSETS/'blockstates'/(name+'.json'),{'multipart':variants})
            save(ASSETS/'models/block'/(name+'.json'),{'parent':wrap('create:block/fluid_valve/block_vertical_open',tone)})
            save(ASSETS/'models/block'/(name+'_arm.json'),{'parent':wrap('create:block/fluid_pipe/rim_connector/north',tone)})
            save(ASSETS/'models/item'/(name+'.json'),{'parent':wrap('create:block/fluid_valve/item',tone)})
            wrap('create:block/fluid_valve/pointer',tone)
        # 方向箭头只占原泵出液法兰侧面；零速/超载时不绘制，不添加流体行为。
        arrow=Image.new('RGBA',(8,8))
        for y in range(1,7):
            for x in range(1,7):
                if (y<=3 and abs(x-3.5)<=y) or (y>=3 and x in (3,4)): arrow.putpixel((x,y),(118,236,247,255))
        export_svg(svg_pixels(arrow,'output_arrow'))
        texture='create_nuclear_industry:block/'+FAMILY+'/output_arrow'
        faces={d:{'texture':'#arrow','uv':[0,0,16,16]} for d in ['north','south','east','west']}
        # 基准箭头朝UP；四个侧面UV让箭头尖端朝高Y，避免全转速下被中央cog遮盖。
        save(ASSETS/'models/block'/FAMILY/'output_arrow.json',{'textures':{'arrow':texture,'particle':texture},'elements':[{'from':a,'to':b,'faces':{d:faces[d]}} for d,a,b in [('north',[6,13.1,1.99],[10,15.9,1.99]),('south',[6,13.1,14.01],[10,15.9,14.01]),('east',[14.01,13.1,6],[14.01,15.9,10]),('west',[1.99,13.1,6],[1.99,15.9,10])]]})
    save(HERE/'sources.json',sources)
    print('Create模型parent同源，SVG导出7张调色纹理+1张箭头；像素alpha逐一核对。')
if __name__=='__main__':
    parser=argparse.ArgumentParser(); parser.add_argument('--pump-only',action='store_true',help='只导出已保存泵SVG，不生成其他资源'); args=parser.parse_args()
    pump_only() if args.pump_only else produce()
