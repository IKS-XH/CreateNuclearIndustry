"""R1独立SVG导出器：所有输入先校验，图集仅粘贴保存的16×16 SVG渲染结果。

13项RECTANGLE与窗口OMNIDIRECTIONAL资产均从SVG导出。输出仅限本generated和R1离线证据，
没有游戏安装入口，不修改共享renderer/旧02A目录，不代替客户端视觉观察。
"""
from pathlib import Path
import argparse,importlib.util,io,json,re,xml.etree.ElementTree as ET
from PIL import Image,ImageDraw,ImageFont

HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[2];EVIDENCE=ROOT/'build/reports/art/ART-REACTOR-02R1'
RECTANGLE_NAMES=('reactor_casing_side','reactor_casing_top','reactor_casing_bottom','reactor_hot_port_side','reactor_hot_port_top',
       'reactor_cold_port_side','reactor_cold_port_top','reactor_instrument_port_side','reactor_instrument_port_top',
       'reactor_refueling_port_side','reactor_refueling_port_top','control_rod_drive_side','control_rod_drive_top')
NAMES=RECTANGLE_NAMES+('reactor_window',)
# 固化真实Create 6.0.10运行的47项输出，不依赖build目录；最终JUnit与mapping逐项核对。
NATIVE_WINDOW_PAIRS=((0, 0), (1, 1), (2, 2), (3, 3), (4, 8), (5, 9), (6, 10), (7, 11), (8, 16), (9, 17), (10, 18), (11, 19), (12, 24), (13, 25), (14, 26), (15, 27), (21, 12), (23, 33), (29, 48), (31, 29), (41, 20), (43, 41), (45, 49), (47, 28), (61, 50), (63, 30), (70, 13), (71, 32), (78, 56), (79, 35), (87, 34), (95, 37), (111, 36), (127, 38), (138, 21), (139, 40), (142, 57), (143, 43), (159, 45), (171, 42), (175, 44), (191, 46), (206, 58), (207, 51), (223, 53), (239, 52), (255, 54))
spec=importlib.util.spec_from_file_location('r1_strict_svg',HERE.parent/'export.py')
shared=importlib.util.module_from_spec(spec);spec.loader.exec_module(shared)

def context(i):
    """锁定RECTANGLE位置语义；格索引单位为图块，不推断玩法合法尺寸。"""
    col,row=i%4,i//4
    return dict(index=i,up=row in (1,2),down=row in (0,1),left=col in (2,3),right=col in (1,2))

def rectangle_index(up,down,left,right):
    return {(False,False):0,(False,True):1,(True,True):2,(True,False):3}[left,right]+4*{(False,True):0,(True,True):1,(True,False):2,(False,False):3}[up,down]

def expected_mapping():
    # 原生运行时输出是索引审计输入；最终JUnit再与真实Create逐项核对，离线不自写索引公式。
    native=[dict(mask=mask,index=index) for mask,index in NATIVE_WINDOW_PAIRS]
    if len(native)!=47 or len({c['mask'] for c in native})!=47 or len({c['index'] for c in native})!=47:raise ValueError('原生窗上下文须47项唯一')
    return dict(schema_version=2,phase='complete14',tile_size=16,rectangle_contexts=[context(i) for i in range(16)],
        window_contexts=native,window_fallback_indices=sorted(set(range(64))-{c['index'] for c in native}),assets=[
        dict(name=n,original_sprite=f'create_nuclear_industry:block/{n}',target_sprite=f'create_nuclear_industry:block/reactor_ct/{n}',
             source_directory=f'sources/{n}',atlas=f'generated/{n}_ct.png',ct_type='OMNIDIRECTIONAL' if n=='reactor_window' else 'RECTANGLE',
             sheet_size=8 if n=='reactor_window' else 4,isolated_index=0 if n=='reactor_window' else 12) for n in NAMES])

def transparent_pixels(mask):
    """按八位连接合同独立校验孔边/角；对角不能越过两条邻边门控。"""
    hole=set();corner_bit={frozenset((0,2)):4,frozenset((0,3)):5,frozenset((1,2)):6,frozenset((1,3)):7}
    for y in range(16):
        for x in range(16):
            edges=([0] if y<4 else [1] if y>=12 else [])+([2] if x<4 else [3] if x>=12 else [])
            required=edges+([corner_bit[frozenset(edges)]] if len(edges)==2 else [])
            if all(mask&(1<<bit) for bit in required):hole.add((x,y))
    return hole

def render_tile(source,allowed,fully_transparent=False):
    """共享工具拒绝零rect；只对全连接内窗的严格空根SVG提供透明例外，其他输入仍走共享校验。"""
    try:return shared.render_svg(source,allowed)
    except ValueError as error:
        if not fully_transparent or str(error)!='源稿不能没有矩形':raise
        if re.search(r'<!DOCTYPE|<!ENTITY|<\?',source,re.I):raise ValueError('透明SVG禁止声明/处理指令')
        tree=ET.fromstring(source,parser=ET.XMLParser(target=ET.TreeBuilder(insert_comments=True,insert_pis=True)))
        expected={'width':'16','height':'16','viewBox':'0 0 16 16','shape-rendering':'crispEdges'}
        if tree.tag!='{http://www.w3.org/2000/svg}svg' or tree.attrib!=expected or (tree.text and tree.text.strip()) or any(n.tag is not ET.Comment or (n.tail and n.tail.strip()) for n in tree):raise ValueError('全透明例外只允许严格空SVG和注释')
        return Image.new('RGBA',(16,16),(0,0,0,0))

def read_inputs(source_overrides=None,mapping_override=None):
    """全输入内存校验；白名单名称/颜色/alpha/路径不由用户提供的mapping扩展。"""
    mapping=mapping_override if mapping_override is not None else json.loads((HERE/'mapping.json').read_text(encoding='utf-8'))
    if json.dumps(mapping,sort_keys=True)!=json.dumps(expected_mapping(),sort_keys=True):raise ValueError('mapping必须匹配13 RECTANGLE/原生47窗口上下文及固定路径')
    palette=json.loads((HERE/'palette.json').read_text(encoding='utf-8'))
    if set(palette)!=set(NAMES):raise ValueError('色板项必须匹配14项')
    expected={f'{n}/tile_{i:02d}.svg' for n in NAMES for i in range(64 if n=='reactor_window' else 16)}
    actual={p.relative_to(HERE/'sources').as_posix() for p in (HERE/'sources').rglob('*') if p.is_file()}
    if actual!=expected:raise ValueError('源目录必须精确包含272份SVG')
    if source_overrides and not set(source_overrides).issubset(expected):raise ValueError('源覆盖名称不在白名单')
    images={}
    for n in NAMES:
        colors=palette[n]
        if not 2<=len(colors)<=16 or len(set(colors.values()))!=len(colors) or any(not re.fullmatch('#[0-9A-Fa-f]{6}',v) for v in colors.values()):raise ValueError(f'{n}: 非法色板')
        tiles=[]
        native={c['index']:c['mask'] for c in mapping['window_contexts']}
        for i in range(64 if n=='reactor_window' else 16):
            key=f'{n}/tile_{i:02d}.svg'
            text=source_overrides[key] if source_overrides and key in source_overrides else (HERE/'sources'/key).read_text(encoding='utf-8')
            mask=native.get(i,0);image=render_tile(text,set(colors.values()),n=='reactor_window' and mask==255)
            pixels=list(image.get_flattened_data())
            if len(set(pixels))>16 or any(p[3] not in (0,255) for p in pixels):raise ValueError(f'{key}: RGBA不超过16色，alpha仅0/255')
            holes={(x,y) for y in range(16) for x in range(16) if pixels[y*16+x][3]==0}
            if holes!=(transparent_pixels(mask) if n=='reactor_window' else set()):raise ValueError(f'{key}: 透明区域不匹配八向窗口/实体规则')
            tiles.append(image)
        images[n]=tiles
    return images

def encode(image):
    data=io.BytesIO();image.save(data,format='PNG',optimize=False);return data.getvalue()

def generated_bytes(images):
    """只从已验证SVG渲染图粘贴图集，不绘制或修色PNG。"""
    files={}
    for n,tiles in images.items():
        sheet=8 if n=='reactor_window' else 4;atlas=Image.new('RGBA',(16*sheet,16*sheet))
        for i,tile in enumerate(tiles):
            files[HERE/f'generated/tiles/{n}/tile_{i:02d}.png']=encode(tile);atlas.paste(tile,(16*(i%sheet),16*(i//sheet)))
        files[HERE/f'generated/{n}_ct.png']=encode(atlas)
    return files

def font(size):
    try:return ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',size)
    except OSError:return ImageFont.load_default(size=size)

def label(page,xy,text,size=18):ImageDraw.Draw(page).text(xy,text,font=font(size),fill='#303638')

def originals():
    """预览原单块也来自SVG，与当前原sprite核对后使用，预览不改游戏资源。"""
    palette=json.loads((HERE.parent/'palette.json').read_text(encoding='utf-8'));images={}
    for n in NAMES:
        image=shared.render_svg((HERE.parent/f'sources/block/{n}.svg').read_text(encoding='utf-8'),set(palette[n].values()))
        with Image.open(ROOT/f'src/main/resources/assets/create_nuclear_industry/textures/block/{n}.png') as original:
            if image.tobytes()!=original.convert('RGBA').tobytes():raise ValueError(f'{n}: 原SVG与原sprite不符，停止对照')
        images[n]=[image]*(64 if n=='reactor_window' else 16)
    return images

def old_tiles():
    """只读旧SVG并用共用renderer重建对照，不执行旧导出器或写旧证据。"""
    palette=json.loads((HERE.parent/'reactor-ct/palette.json').read_text(encoding='utf-8'))
    return {n:[shared.render_svg((HERE.parent/f'reactor-ct/sources/{n}/tile_{i:02d}.svg').read_text(encoding='utf-8'),set(palette[n].values())) for i in range(16)] for n in NAMES}

def face(images,w,h,material='reactor_casing_side',features=None):
    """离线结构背景四向铺面；窗口索引查实际Create输出，不计算替代UV。"""
    out=Image.new('RGBA',(w*16,h*16));features=features or {}
    native={c['mask']:c['index'] for c in expected_mapping()['window_contexts']}
    def window(x,y):return features.get((x,y),material)=='reactor_window'
    for y in range(h):
        for x in range(w):
            i=rectangle_index(y>0,y<h-1,x>0,x<w-1);n=features.get((x,y),material)
            if n=='reactor_window' and len(images[n])==64:
                bits=[window(x,y-1),window(x,y+1),window(x-1,y),window(x+1,y)]
                bits += [bits[0] and bits[2] and window(x-1,y-1),bits[0] and bits[3] and window(x+1,y-1),
                         bits[1] and bits[2] and window(x-1,y+1),bits[1] and bits[3] and window(x+1,y+1)]
                i=native[sum((1<<k) for k,v in enumerate(bits) if v)]
            out.paste(images[n][i],(x*16,y*16))
    return out

def checker(size):
    """预览底板用于识别透明孔，不写入任何作者纹理。"""
    image=Image.new('RGB',size,'#D9DED9');draw=ImageDraw.Draw(image)
    for y in range(0,size[1],8):
        for x in range(0,size[0],8):
            if (x//8+y//8)%2:draw.rectangle((x,y,x+7,y+7),fill='#BCC8C5')
    return image

def flat(page,texture,xy,size):
    texture=texture.resize(size,Image.Resampling.NEAREST);back=checker(size);back.paste(texture,(0,0),texture);page.paste(back,xy)

def projected(page,texture,project,shade):
    """同一共享投影与光照模拟，透明像素不绘制；预览不是游戏渲染验收。"""
    draw=ImageDraw.Draw(page)
    for v in range(texture.height):
        for u in range(texture.width):
            color=texture.getpixel((u,v))
            if len(color)==4 and color[3]==0:continue
            draw.polygon([tuple(round(n) for n in project(x,y)) for x,y in ((u,v),(u+1,v),(u+1,v+1),(u,v+1))],
                         fill=tuple(round(c*shade) for c in color[:3]))

def mixed_features():
    features={(x,y):'reactor_window' for y in range(1,4) for x in range(1,4)}
    features.update({(2,2):'reactor_instrument_port_side',(0,2):'reactor_cold_port_side',(4,2):'reactor_hot_port_side'})
    return features

def cube(page,images,origin):
    """5×5×5闭合三面共享P(x,z,y)；各组几何和光照完全相同。"""
    ox,oy=origin;W=H=D=80
    def p(x,z,y):return ox+(x-z)*1.85,oy+(x+z)*.8+y*1.85
    front=face(images,5,5,features=mixed_features())
    right=face(images,5,5,features={(2,2):'reactor_refueling_port_side'})
    top=face(images,5,5,'reactor_casing_top',{(1,1):'control_rod_drive_top'})
    projected(page,front,lambda u,v:p(u,D,v),.90)
    projected(page,right,lambda u,v:p(W,D-u,v),.77)
    projected(page,top,lambda u,v:p(u,v,0),1.0)

def old_scene_index(scene,c):
    """旧RECTANGLE对照仍遵守实际占格与owner边界；仪表属于背景连接域。"""
    cells={(v['x'],v['y']) for v in scene['cells']};x,y=c['x'],c['y']
    def joined(nx,ny):
        return (nx,ny) in cells and (scene['name']!='owners' or (x<scene['width']//2)==(nx<scene['width']//2))
    return rectangle_index(joined(x,y-1),joined(x,y+1),joined(x-1,y),joined(x+1,y))

def scene_texture(scene,images,new=True):
    """使用专属JUnit实际buildContext产生的索引；不同owner边界无需离线猜测。"""
    out=Image.new('RGBA',(16*scene['width'],16*scene['height']))
    for c in scene['cells']:
        n='reactor_window' if c['material']=='W' else 'reactor_instrument_port_side'
        i=c['index'] if new else old_scene_index(scene,c)
        out.paste(images[n][i],(16*c['x'],16*c['y']))
    return out

def thick_scene(page,scene,images,original,origin,new=True):
    """有厚度展示消费实际gatherModelData/getQuads输出的面清单，只有预览投影是模拟。

    NORTH为前面，局部右向为WEST。新图只绘制清单保留的面；旧图保留全部共享接口。
    非外向面使用原sprite，未遮蔽背面SOUTH仍保留在实际清单中，此视角因背面朝向不画。
    """
    ox,oy=origin;scale=4.0;depth=16
    def p(x,z,y):return ox+(x-z*.58)*scale,oy+(x*.22-z*.42+y)*scale
    # 远侧/上方块先绘制，后续正面透明孔露出真实未遮蔽侧面。
    for c in sorted(scene['cells'],key=lambda c:(c['y'],-c['x'])):
        x,y=c['x']*16,c['y']*16;n='reactor_window' if c['material']=='W' else 'reactor_instrument_port_side'
        visible=c['visible'] if new else ['DOWN','UP','NORTH','SOUTH','WEST','EAST']
        tile=original[n][0]
        if 'UP' in visible:projected(page,tile,lambda u,v,x=x,y=y:p(x+u,v,y),1.0)
        if 'WEST' in visible:projected(page,tile,lambda u,v,x=x,y=y:p(x+16,u,y+v),.68)
        i=c['index'] if new else old_scene_index(scene,c)
        if 'NORTH' in visible:projected(page,images[n][i],lambda u,v,x=x,y=y:p(x+u,0,y+v),.90)

def preview_bytes(images):
    """保留已审13项预览；只生成本批新增完整混合、窗簇与实际掩码厚度预览。"""
    original=originals();old=old_tiles();groups=(('原单块SVG',original),('旧02B成型SVG',old),('R1新成型SVG',images))
    page=Image.new('RGB',(1680,960),'#E5E3DA');label(page,(24,16),'同缩放/光照三联：3×3窗簇被仪表打断，冷热口与原材质层次',22)
    for k,(title,data) in enumerate(groups):
        x=24+k*552;label(page,(x,66),title,22);wall=face(data,5,5,features=mixed_features())
        flat(page,wall,(x,110),(480,480))
        dim=wall.copy();dim.putdata([tuple(round(c*.55) for c in pixel[:3])+(pixel[3],) for pixel in wall.get_flattened_data()])
        flat(page,dim,(x,636),(240,240));label(page,(x+264,674),'同55%光照模拟',17)
    label(page,(24,916),'透明孔用棋盘底显示；像素全部来自保存的SVG，不代替客户端实际光照/透明复看。',17)
    cubes=Image.new('RGB',(1680,680),'#E5E3DA');label(cubes,(24,16),'完整混合闭合cube：顶/正/右共享顶点；透孔不填黑',22)
    for k,(title,data) in enumerate(groups):
        x=24+k*552;label(cubes,(x,72),title,21);cube(cubes,data,(x+240,140))
    label(cubes,(24,624),'13项第二稿保持；本页为几何示意，窗口共享接口面的消费验证见厚度页。',17)
    scenes=json.loads((EVIDENCE/'native-window-scenes.json').read_text(encoding='utf-8'))
    titles={'two_by_two':'2×2窗组','row':'1×N','column':'N×1','ell':'L形真实凹角','instrument_hole':'3×3 / 仪表打断','owners':'不同owner边界','complete':'3×3 / 全透明内窗54'}
    planar=Image.new('RGB',(1500,1020),'#E5E3DA');label(planar,(24,16),'窗口八向平面：索引来自真实Create buildContext；不同owner不合并',22)
    for j,scene in enumerate(scenes):
        x=24+(j%4)*368;y=86+(j//4)*440;label(planar,(x,y),titles[scene['name']],18)
        size=(scene['width']*64,scene['height']*64);flat(planar,scene_texture(scene,images),(x,y+40),size)
    label(planar,(24,974),'47有效原生索引/17隔离回退；全连接内窗SVG允许严格空根，透明仅来自连接孔区域。',17)
    thickness=Image.new('RGB',(1500,1200),'#D9DED9');label(thickness,(24,16),'有厚度对照：共享面来自实际ModelData/getQuads清单，不是人工隐藏',21)
    picked=[s for s in scenes if s['name'] in ('two_by_two','instrument_hole','owners')]
    for j,scene in enumerate(picked):
        y=80+j*340;label(thickness,(24,y),titles[scene['name']],19)
        label(thickness,(230,y),'旧：逐格原孔＋共享接口仍绘制',18);label(thickness,(820,y),'新：八向孔＋实际共享面mask过滤',18)
        thick_scene(thickness,scene,old,original,(270,y+52),False);thick_scene(thickness,scene,images,original,(860,y+52),True)
    label(thickness,(24,1144),'输入：native-window-scenes.json（17项JUnit中的真实模型路径）；非共享外圈/背面保留。',17)
    return {EVIDENCE/'mixed-triptych.png':encode(page),EVIDENCE/'closed-cube.png':encode(cubes),
            EVIDENCE/'window-clusters.png':encode(planar),EVIDENCE/'window-thickness.png':encode(thickness)}

def write_files(files):
    allowed=((HERE/'generated').resolve(),EVIDENCE.resolve())
    if any(not any(p.resolve().is_relative_to(r) for r in allowed) for p in files):raise ValueError('输出不在本批固定写集')
    old={p:p.read_bytes() if p.exists() else None for p in files};touched=[]
    try:
        for p,data in files.items():
            if old[p]==data:continue
            p.parent.mkdir(parents=True,exist_ok=True);touched.append(p);p.write_bytes(data)
    except Exception:
        for p in reversed(touched):
            if old[p] is None:p.unlink(missing_ok=True)
            else:p.write_bytes(old[p])
        raise
    return len(touched)

def run(check=False,source_overrides=None,mapping_override=None):
    images=read_inputs(source_overrides,mapping_override);files=generated_bytes(images)
    if check:
        if any(not p.exists() or p.read_bytes()!=b for p,b in files.items()):raise ValueError('本批产物与SVG不一致')
        if {p for p in (HERE/'generated').rglob('*') if p.is_file()}!=set(files):raise ValueError('本批generated出现额外产物')
        print('SVG/PNG只读核对通过：272图块+14图集，窗口47有效/17隔离回退');return
    files.update(preview_bytes(images));changed=write_files(files)
    print(f'R1导出通过：272SVG图块+14图集+预览，变更{changed}文件；未写game')

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');run(parser.parse_args().check)
