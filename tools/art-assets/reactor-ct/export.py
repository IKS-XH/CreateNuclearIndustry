"""只消费本批整数矩形SVG的RECTANGLE图集导出器。

图块为16×16像素，图集为4×4格。全部源稿和映射校验、编码成功后才写出；
输出限定本目录generated与固定离线证据目录，没有游戏安装或路径扩展入口。
这是离线资产工具，不读取游戏结构状态，也不证明成型或合法玩法尺寸。
"""
from __future__ import annotations

import argparse
import importlib.util
import io
import json
from pathlib import Path
import re
from PIL import Image, ImageDraw, ImageEnhance, ImageFont, ImageOps

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
EVIDENCE = ROOT / 'build/reports/art/ART-REACTOR-02'
NAMES = (
    'reactor_casing_side', 'reactor_casing_top', 'reactor_casing_bottom',
    'reactor_hot_port_side', 'reactor_hot_port_top',
    'reactor_cold_port_side', 'reactor_cold_port_top', 'reactor_window',
    'reactor_instrument_port_side', 'reactor_instrument_port_top',
    'reactor_refueling_port_side', 'reactor_refueling_port_top',
    'control_rod_drive_side', 'control_rod_drive_top',
)
_spec = importlib.util.spec_from_file_location('reactor_ct_shared_svg', HERE.parent / 'export.py')
if _spec is None or _spec.loader is None:
    raise RuntimeError('无法载入共用严格SVG渲染器')
_shared = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_shared)


def context(index: int) -> dict:
    """返回Create6.0.10-280锁定的上下左右连接语义，index单位为图格。"""
    column, row = index % 4, index // 4
    return dict(index=index, column=column, row=row, up=row in (1, 2),
                down=row in (0, 1), left=column in (2, 3), right=column in (1, 2))


def select_index(up: bool, down: bool, left: bool, right: bool) -> int:
    """离线选格：列为无/右/双/左，行为下/双/上/无，不依赖尺寸奇偶。"""
    column = {(False, False): 0, (False, True): 1,
              (True, True): 2, (True, False): 3}[left, right]
    row = {(False, True): 0, (True, True): 1,
           (True, False): 2, (False, False): 3}[up, down]
    return column + 4 * row


def validate_mapping(mapping: dict) -> None:
    """固定消费接口；拒绝更换sprite、排列、源目录及输出路径，避免误装游戏资源。"""
    expected = dict(schema_version=1, ct_type='RECTANGLE', tile_size=16,
                    sheet_size=4, isolated_index=12,
                    contexts=[context(i) for i in range(16)], assets=[
                        dict(name=n, original_sprite=f'create_nuclear_industry:block/{n}',
                             target_sprite=f'create_nuclear_industry:block/reactor_ct/{n}',
                             source_directory=f'sources/{n}', atlas=f'generated/{n}_ct.png')
                        for n in NAMES])
    # JSON字节结构比较同时排除bool冒充数字，Python的True == 1不能作为协议校验。
    if json.dumps(mapping, sort_keys=True) != json.dumps(expected, sort_keys=True):
        raise ValueError('mapping必须完整匹配锁定的14项sprite、RECTANGLE索引与相对路径')


def encode(image: Image.Image) -> bytes:
    """确定性编码PNG；原图块RGBA字节不经预览光照/滤波变换。"""
    stream = io.BytesIO()
    image.save(stream, format='PNG', optimize=False)
    return stream.getvalue()


def read_inputs(source_overrides: dict | None = None, mapping_override: dict | None = None) -> dict:
    """校验全部224份SVG、14色板及映射；内存覆盖只供拒绝负例，不改变文件路径。"""
    mapping = (mapping_override if mapping_override is not None else
               json.loads((HERE / 'mapping.json').read_text(encoding='utf-8')))
    validate_mapping(mapping)
    palette = json.loads((HERE / 'palette.json').read_text(encoding='utf-8'))
    if not isinstance(palette, dict) or set(palette) != set(NAMES):
        raise ValueError('色板必须且只能包含本批14项')
    expected_sources = {f'{n}/tile_{i:02d}.svg' for n in NAMES for i in range(16)}
    actual_sources = {p.relative_to(HERE / 'sources').as_posix()
                      for p in (HERE / 'sources').rglob('*') if p.is_file()}
    if actual_sources != expected_sources:
        raise ValueError('源目录必须精确包含224份tile_00至tile_15.svg')
    if source_overrides is not None and (not isinstance(source_overrides, dict) or
                                         not set(source_overrides).issubset(expected_sources)):
        raise ValueError('内存源稿覆盖必须使用本批已知SVG相对名称')
    images = {}
    for name in NAMES:
        colors = palette[name]
        if (not isinstance(colors, dict) or not 2 <= len(colors) <= 16 or
                any(not isinstance(c, str) or not re.fullmatch(r'#[0-9A-Fa-f]{6}', c)
                    for c in colors.values()) or
                len(set(c.upper() for c in colors.values())) != len(colors)):
            raise ValueError(f'{name}: 色板必须为2至16种不重复#RRGGBB实色')
        allowed = {c.upper() for c in colors.values()}
        tiles = []
        for index in range(16):
            key = f'{name}/tile_{index:02d}.svg'
            source = (source_overrides[key] if source_overrides and key in source_overrides else
                      (HERE / 'sources' / key).read_text(encoding='utf-8'))
            image = _shared.render_svg(source, allowed, (16, 16))
            pixels = tuple(image.get_flattened_data())
            if len(set(pixels)) > 16 or any(p[3] not in (0, 255) for p in pixels):
                raise ValueError(f'{key}: 每格最多16种RGBA颜色，alpha仅0/255')
            transparent = {(x, y) for y in range(16) for x in range(16)
                           if pixels[y * 16 + x][3] == 0}
            expected_hole = {(x, y) for y in range(4, 12) for x in range(4, 12)}
            if name == 'reactor_window' and transparent != expected_hole:
                raise ValueError(f'{key}: 必须保留原8×8透明窗口孔')
            if name != 'reactor_window' and transparent:
                raise ValueError(f'{key}: 实体面必须全不透明')
            tiles.append(image)
        images[name] = tiles
    return images


def generated_bytes(images: dict) -> dict:
    """仅粘贴SVG渲染结果组成图集；禁止直接绘制或改色游戏图块。"""
    files = {}
    for name, tiles in images.items():
        atlas = Image.new('RGBA', (64, 64))
        for index, tile in enumerate(tiles):
            files[HERE / f'generated/tiles/{name}/tile_{index:02d}.png'] = encode(tile)
            atlas.paste(tile, (16 * (index % 4), 16 * (index // 4)))
        files[HERE / f'generated/{name}_ct.png'] = encode(atlas)
    return files


def font(size=17):
    """预览字体延迟加载；缺少Windows字体时降级，不影响--check。"""
    try:
        return ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', size)
    except OSError:
        return ImageFont.load_default(size=size)


def label(page, xy, text, size=17, color='#303638'):
    """写离线预览标注，单位为预览像素，不向SVG或游戏图块写文字。"""
    ImageDraw.Draw(page).text(xy, text, font=font(size), fill=color)


def checker(size, light='#B9AA85', dark='#7D9787'):
    """以后方双色对照显露透明孔；所有格均不透明，供alpha合成背景。"""
    result = Image.new('RGBA', size, light)
    draw = ImageDraw.Draw(result)
    for y in range(0, size[1], 4):
        for x in range(0, size[0], 4):
            if (x // 4 + y // 4) % 2:
                draw.rectangle((x, y, x + 3, y + 3), fill=dark)
    return result


def face(images, width, height, material='reactor_casing_side', features=None,
         formed=True, owners=None):
    """按面坐标和owner选择格；不同sprite合法共面，同位置透明窗显示后方对照。

    width/height单位为方块，每方块保持16像素。owners是离线模拟输入，
    不查询Minecraft，也不能替代逻辑侧的可靠客户端归属快照。
    """
    result = checker((width * 16, height * 16))
    features = features or {}
    owners = owners or {(x, y): 'A' for x in range(width) for y in range(height)}
    indices = []
    for y in range(height):
        row = []
        for x in range(width):
            owner = owners[x, y]
            connected = [formed and owners.get(p) == owner
                         for p in ((x, y-1), (x, y+1), (x-1, y), (x+1, y))]
            index = select_index(*connected)
            row.append(index)
            result.alpha_composite(images[features.get((x, y), material)][index], (x*16, y*16))
        indices.append(row)
    return result, indices


def feature_positions(width, height):
    """中间与偏移位置同时示例，不依赖奇数中心或固定五格。

    功能件位置仅为离线视觉样例，不声明游戏内放置限制。
    """
    return {(1, max(1, height-2)): 'reactor_hot_port_side',
            (width-2, 1): 'reactor_cold_port_side',
            (width//2, height//2): 'reactor_window',
            (1, 1): 'reactor_instrument_port_side'}


def index_preview(images):
    """14类同屏索引页，原16像素和最近邻4倍并列，窗口以背景显露孔。"""
    page = Image.new('RGB', (1640, 1540), '#E5E3DA')
    label(page, (20, 15), 'RECTANGLE：列 无/右/双/左；行 下/双/上/无；孤立 index12')
    for number, name in enumerate(NAMES):
        ox, oy = 20+(number%4)*405, 65+(number//4)*365
        label(page, (ox, oy), name, 14)
        for index, tile in enumerate(images[name]):
            x, y = ox+(index%4)*96, oy+30+(index//4)*78
            bg = checker((16,16)); bg.alpha_composite(tile)
            page.paste(bg.convert('RGB'), (x,y+17))
            page.paste(bg.resize((64,64), Image.Resampling.NEAREST).convert('RGB'), (x+23,y))
            label(page, (x+3,y+43), str(index), 12)
    return page


def wall_preview(images):
    """5×5混合墙、内格与功能件，以及顶底封闭铺设和2×2外沿。"""
    page = Image.new('RGB', (1460, 960), '#E5E3DA')
    label(page,(24,16),'成型连接纹理离线首稿 | 5×5正面墙 + 面内格 + 热口 / 冷口 / 透明窗口',20)
    label(page,(24,48),'只有结构面外沿保留钢框；窗口后方双色对照不是玻璃或黑面板。')
    wall,_=face(images,5,5,features=feature_positions(5,5))
    page.paste(wall.resize((480,480),Image.Resampling.NEAREST).convert('RGB'),(30,90))
    entries=('reactor_casing_side','reactor_hot_port_side','reactor_cold_port_side','reactor_window',
             'reactor_instrument_port_side','reactor_refueling_port_side','control_rod_drive_top')
    for i,name in enumerate(entries):
        x,y=550+(i%4)*216,90+(i//4)*224
        label(page,(x,y),name.replace('reactor_',''),13)
        tile=checker((16,16));tile.alpha_composite(images[name][6])
        page.paste(tile.resize((160,160),Image.Resampling.NEAREST).convert('RGB'),(x,y+24))
        label(page,(x,y+188),'中间格 index06',13)
    for i,(material,w,h) in enumerate((('reactor_casing_top',6,3),('reactor_casing_bottom',6,3),('reactor_casing_side',2,2))):
        x=30+i*480;label(page,(x,605),f'{material} | {w}×{h}',15)
        image,_=face(images,w,h,material)
        scale=4 if w>2 else 7
        page.paste(image.resize((w*16*scale,h*16*scale),Image.Resampling.NEAREST).convert('RGB'),(x,640))
    label(page,(24,921),'仅离线图块与铺面；不证明客户端成型接入、真实光照或合法尺寸。')
    return page


def draw_projected_face(page, texture, project, shade=1.0):
    """将整个已铺平面的每个像素投影到同一个长方体面，几何顶点共用。

    仅预览使用光照倍率；渲染图块和图集不经过此函数。
    """
    draw=ImageDraw.Draw(page); pixels=texture.load()
    for v in range(texture.height):
        for u in range(texture.width):
            points=[project(u,v),project(u+1,v),project(u+1,v+1),project(u,v+1)]
            draw.polygon([(round(x),round(y)) for x,y in points],
                         fill=tuple(round(c*shade) for c in pixels[u,v][:3]))


def draw_cube(page, images, dimensions, origin, scales=(2.3,1.15,2.3), formed=True):
    """闭合长方体投影：P(x,z,y)=(ox+(x-z)a,oy+(x+z)b+yc)。

    dimensions按宽、高、深，单位方块；三面共享P(W,D,0)，
    正面z=D、右面x=W、顶面y=0，不用展开V片伪造立方体。
    """
    width,height,depth=dimensions; ox,oy=origin; a,b,c=scales
    W,H,D=width*16,height*16,depth*16
    def p(x,z,y): return ox+(x-z)*a,oy+(x+z)*b+y*c
    front,_=face(images,width,height,features=feature_positions(width,height),formed=formed)
    right,_=face(images,depth,height,features={(depth-2, height//2):'reactor_refueling_port_side'},formed=formed)
    top,_=face(images,width,depth,'reactor_casing_top',
               features={(1,1):'control_rod_drive_top',(width-2,depth-2):'reactor_refueling_port_top'},formed=formed)
    draw_projected_face(page,front,lambda u,v:p(u,D,v),.90)
    draw_projected_face(page,right,lambda u,v:p(W,D-u,v),.77)
    draw_projected_face(page,top,lambda u,v:p(u,v,0),1.0)


def geometry_preview(images):
    """同结构的5×5×5、6×5×8、9×7×5及正交面，后两者明确只是尺寸适配假设。"""
    page=Image.new('RGB',(1980,1230),'#E5E3DA')
    label(page,(24,16),'完整闭合三面 | 宽×高×深 | 每方块16×16像素，不拉伸整面纹理',21)
    label(page,(24,48),'6×5×8、9×7×5仅为尺寸适配假设，非合法玩法尺寸声明；功能件示意位置不限定中心。')
    for i,dims in enumerate(((5,5,5),(6,5,8),(9,7,5))):
        left=24+i*650;w,h,depth=dims
        label(page,(left,95),'×'.join(str(n) for n in dims)+(' 当前几何' if i==0 else ' 适配假设'),20)
        draw_cube(page,images,dims,(left+35+depth*16*2.3,155))
        label(page,(left,735),'正交正面：不同sprite共面连接',16)
        im,_=face(images,w,h,features=feature_positions(w,h))
        page.paste(im.resize((w*16*3,h*16*3),Image.Resampling.NEAREST).convert('RGB'),(left,775))
        label(page,(left,1140),f'正面 {w}×{h}；右面 {depth}×{h}；顶面 {w}×{depth}',15)
    label(page,(24,1190),'预览倍率仅用于几何构造检查；没有加载Minecraft、扫描反应堆或消费客户端结构数据。')
    return page


def owner_preview(images):
    """跨owner接壤、未成型/成型对照及缩小/灰度/降亮模拟。"""
    page=Image.new('RGB',(1560,1170),'#E5E3DA')
    label(page,(24,16),'归属与降级 | 两台接壤仍各自封边，未成型显示单块外观',21)
    owners={(x,y):('A' if x<3 else 'B') for x in range(6) for y in range(3)}
    im,_=face(images,6,3,owners=owners,features={(1,1):'reactor_hot_port_side',(4,1):'reactor_cold_port_side'})
    label(page,(30,70),'owner A（3×3） | owner B（3×3）',18)
    page.paste(im.resize((576,288),Image.Resampling.NEAREST).convert('RGB'),(30,105))
    for i,formed in enumerate((False,True)):
        x=665+i*440
        label(page,(x,70),'成型连接（离线假设）' if formed else '未成型：当前独立资源',17)
        if formed:
            small,_=face(images,3,3,features={(1,1):'reactor_window'})
        else:
            # 实际游戏降级保留original sprite，所以用开工保存的原图作对照。
            small=checker((48,48))
            for y in range(3):
                for xx in range(3):
                    n='reactor_window' if (xx,y)==(1,1) else 'reactor_casing_side'
                    original=Image.open(ROOT/f'src/main/resources/assets/create_nuclear_industry/textures/block/{n}.png').convert('RGBA')
                    small.alpha_composite(original,(xx*16,y*16))
        page.paste(small.resize((288,288),Image.Resampling.NEAREST).convert('RGB'),(x,105))
    label(page,(30,425),'跨owner几何：两台不同归属的3×3×3在x方向真实接壤；可见正面各自封边',17)
    # 第二台原点沿同一x轴平移，底面与顶部几何共面；保持各自的独立选格。
    origin=(160,490);s=(2.4,1.2,2.4)
    draw_cube(page,images,(3,3,3),origin,s)
    draw_cube(page,images,(3,3,3),(origin[0]+48*s[0],origin[1]+48*s[1]),s)
    wall,_=face(images,5,5,features=feature_positions(5,5)); wall=wall.convert('RGB')
    variants=(('原样',wall),('缩小至40×40后放大',wall.resize((40,40),Image.Resampling.BOX)),
              ('灰度',ImageOps.grayscale(wall).convert('RGB')),('降亮55%',ImageEnhance.Brightness(wall).enhance(.55)))
    for i,(title,image) in enumerate(variants):
        x=25+i*385;label(page,(x,820),title,16)
        page.paste(image.resize((280,280),Image.Resampling.NEAREST),(x,860))
    label(page,(530,480),'窗口后方使用双色对照；孔没有被黑色/钢板覆盖。',16)
    label(page,(530,516),'缩小与降亮模拟不证明游戏光照、管路遮挡或实际成型。',16)
    return page


def preview_bytes(images):
    """默认导出四张离线审查页；全部编码完成后才允许写出。"""
    return {EVIDENCE/'indices.png':encode(index_preview(images)),
            EVIDENCE/'wall-preview.png':encode(wall_preview(images)),
            EVIDENCE/'geometry-preview.png':encode(geometry_preview(images)),
            EVIDENCE/'owner-preview.png':encode(owner_preview(images))}


def write_files(files):
    """只写字节变化文件，失败时恢复本次已触及的文件；固定路径不由mapping决定。"""
    allowed_roots=((HERE/'generated').resolve(), EVIDENCE.resolve())
    if any(not any(p.resolve().is_relative_to(r) for r in allowed_roots) for p in files):
        raise ValueError('输出只能位于本批generated和固定证据目录')
    old={p:p.read_bytes() if p.exists() else None for p in files};touched=[]
    try:
        for path,data in files.items():
            if old[path]==data: continue
            path.parent.mkdir(parents=True,exist_ok=True);touched.append(path);path.write_bytes(data)
    except Exception:
        for path in reversed(touched):
            if old[path] is None: path.unlink(missing_ok=True)
            else: path.write_bytes(old[path])
        raise
    return len(touched)


def run(check=False, source_overrides=None, mapping_override=None):
    """执行全输入校验后导出，或只读比较238份图块/图集的确定性PNG字节。"""
    images=read_inputs(source_overrides,mapping_override)
    files=generated_bytes(images)
    if check:
        bad=[str(p.relative_to(HERE)) for p,b in files.items() if not p.is_file() or p.read_bytes()!=b]
        actual={p for p in (HERE/'generated').rglob('*') if p.is_file()}
        if bad or actual!=set(files):
            raise ValueError('生成物与SVG预期不一致或存在额外生成文件：'+', '.join(bad))
        print('只读检查通过：224图块、14图集与SVG逐字节一致；映射及alpha完整')
        return
    files.update(preview_bytes(images))
    changed=write_files(files)
    print(f'导出通过：224图块、14图集、4预览；实际更新{changed}文件；未安装游戏资源')


def main():
    parser=argparse.ArgumentParser(description='ART-REACTOR-02A SVG连接图集与离线预览')
    parser.add_argument('--check',action='store_true',help='只读校验224图块及14图集字节')
    args=parser.parse_args()
    run(check=args.check)


if __name__=='__main__':
    main()
