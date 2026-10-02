"""EXT-ART-02固定白名单管线及EXT-ART-08铀燃料链素材扩展，先验证全部输入再接入游戏。

无游戏逻辑或注册修改。清单仅允许69张既有游戏PNG及本批新增纹理；
保留完整 flow 画布；EXT-ART-02A 的八项冷却剂按各自原始 PNG 保留。
"""
import argparse
import hashlib
import io
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont
from export import ROOT, read_palette, render_svg

REPO = ROOT.parents[1]
GAME_ROOT = REPO/'src/main/resources/assets/create_nuclear_industry/textures'
# 独立于可编辑 manifest 的固定写入白名单，任何新增/改名都先拒绝。
GAME_FILES = frozenset('''block/compound_coolant_flow.png
block/compound_coolant_still.png
block/control_rod_drive_side.png
block/control_rod_drive_top.png
block/deepslate_lead_ore.png
block/deepslate_tin_ore.png
block/deepslate_uranium_ore.png
block/hot_compound_coolant_flow.png
block/hot_compound_coolant_still.png
block/lead_block.png
block/lead_ore.png
block/main_coolant_pump_side.png
block/main_coolant_pump_top.png
block/pressure_pipe_tier_1.png
block/pressure_valve_tier_1.png
block/raw_lead_block.png
block/raw_tin_block.png
block/raw_uranium_block.png
block/reactor_casing_bottom.png
block/reactor_casing_side.png
block/reactor_casing_top.png
block/reactor_cold_port_side.png
block/reactor_cold_port_top.png
block/reactor_fuel_rod_side.png
block/reactor_fuel_rod_top.png
block/reactor_hot_port_side.png
block/reactor_hot_port_top.png
block/reactor_instrument_port_side.png
block/reactor_instrument_port_top.png
block/reactor_refueling_port_side.png
block/reactor_refueling_port_top.png
block/reactor_window.png
block/tin_block.png
block/tin_ore.png
block/uranium_ore.png
fluid/compound_coolant_flow.png
fluid/compound_coolant_still.png
fluid/hot_compound_coolant_flow.png
fluid/hot_compound_coolant_still.png
item/control_rod.png
item/cooled_spent_fuel_assembly.png
item/dosimeter.png
item/fresh_fuel_assembly.png
item/lead_ingot.png
item/lead_shielding_cask.png
item/raw_lead.png
item/raw_tin.png
item/raw_uranium.png
item/steel_plate.png
item/tin_ingot.png
item/uranium_concentrate.png'''.splitlines())
PREVIOUS_NEW_GAME_FILES = frozenset({'item/lead_plate.png', 'item/tin_plate.png', 'item/lead_nugget.png', 'item/tin_nugget.png', 'item/iron_dust.png', 'item/coal_dust.png', 'item/charcoal_dust.png', 'item/steel_dust.png', 'item/steel_ingot.png', 'item/tin_wire.png', 'item/industrial_sensor.png', 'item/radiation_sensor.png', 'item/incomplete_industrial_sensor.png', 'item/incomplete_radiation_sensor.png', 'item/quartz_dust.png', 'item/refractory_brick.png', 'item/heavy_bearing.png', 'item/incomplete_heavy_bearing.png'})
CURRENT_BATCH_GAME_FILES = frozenset({'item/uranium_tailings.png', 'item/low_enriched_uranium_dust.png', 'item/depleted_uranium_dust.png', 'item/uranium_slurry_bucket.png', 'block/uranium_tailings_brick.png', 'block/enrichment_centrifuge_front.png', 'block/enrichment_centrifuge_back.png', 'block/enrichment_centrifuge_slurry_port.png', 'block/enrichment_centrifuge_water_port.png', 'block/enrichment_centrifuge_top.png', 'block/enrichment_centrifuge_bottom.png', 'fluid/uranium_slurry_still.png', 'fluid/uranium_slurry_flow.png'})
NEW_GAME_FILES = PREVIOUS_NEW_GAME_FILES | CURRENT_BATCH_GAME_FILES
ALLOWED_GAME_FILES = GAME_FILES | NEW_GAME_FILES

# 用户批准的原始 PNG 例外，路径与哈希均冻结；不能借 manifest 导入任意位图。
# block/fluid 分别核对，即使当前字节相同也不以兼容路径替代原图。
RETAINED_ORIGINALS = {
    'block/compound_coolant_flow.png': 'ff5fc3fe7a385cb77343c81d191ffec7ca4801c1003c250e2d27658331f48adf',
    'block/compound_coolant_still.png': 'de30924110d0ec43e801151e94369db6a39d5fee08db889155c991b2116d3bc9',
    'block/hot_compound_coolant_flow.png': '44aee2d725d6baa6c4a60232aad0d87549fb61a086b064d0ccbc99b7ea5f29f6',
    'block/hot_compound_coolant_still.png': '7456bf7510e47f104b9481eed3468ab0de1454a500c437d9af7b8541b468f9ea',
    'fluid/compound_coolant_flow.png': 'ff5fc3fe7a385cb77343c81d191ffec7ca4801c1003c250e2d27658331f48adf',
    'fluid/compound_coolant_still.png': 'de30924110d0ec43e801151e94369db6a39d5fee08db889155c991b2116d3bc9',
    'fluid/hot_compound_coolant_flow.png': '44aee2d725d6baa6c4a60232aad0d87549fb61a086b064d0ccbc99b7ea5f29f6',
    'fluid/hot_compound_coolant_still.png': '7456bf7510e47f104b9481eed3468ab0de1454a500c437d9af7b8541b468f9ea',
}


def load_manifest():
    """验证路径、尺寸、用途及兼容映射，禁止 manifest 扩大游戏写集。"""
    document=json.loads((ROOT/'manifest.json').read_text(encoding='utf-8'))
    if set(document)!={'schema','entries'} or document['schema']!=1:
        raise ValueError('manifest 版本或字段错误')
    entries=document['entries']
    if not isinstance(entries,list) or len(entries)!=83:
        raise ValueError('清单必须恰好为82个游戏路径加lapis候选')
    games=[e.get('game') for e in entries if isinstance(e,dict) and e.get('game') is not None]
    if len(games)!=82 or len(set(games))!=82 or set(games)!=ALLOWED_GAME_FILES:
        raise ValueError('游戏路径必须与69项既有素材和本批13项素材白名单完全一致')
    if sum(e.get('game') is None for e in entries)!=1:
        raise ValueError('只能有一个工具候选')
    for e in entries:
        fields={'game','source','palette','size','kind','group','use'}
        if e['game'] in RETAINED_ORIGINALS:
            fields.add('preserve')
            if e.get('preserve')!='baseline-original':raise ValueError('冷却剂必须保留已批准原图')
        if set(e)!=fields:
            raise ValueError('manifest 记录字段不匹配')
        game=e['game']; name=game or 'item/lapis_dust.png'
        source=('fluid/'+name.split('/')[-1] if 'compound_coolant' in name else name)[:-4]+'.svg'
        size=[16,64 if name.endswith('_flow.png') else 16]
        kind='item' if name.startswith('item/') else ('window' if name=='block/reactor_window.png' else 'opaque')
        if e['source']!=source or e['palette']!=Path(source).stem or e['size']!=size or e['kind']!=kind:
            raise ValueError(f'{name}: 源稿映射、尺寸、色板或透明语义不符')
        if e['group'] not in ('items','minerals','reactor','coolant','centrifuge','fluids') or not isinstance(e['use'],str):
            raise ValueError('分组或用途字段非法')
        if not (ROOT/'sources'/source).resolve().is_relative_to((ROOT/'sources').resolve()):
            raise ValueError('源稿越过源目录')
    return entries


def validate_alpha(entry,image):
    """验证二值 alpha；窗口中央8×8必须完全透明，物品四周保留透明像素。"""
    values=set(image.getchannel('A').tobytes())
    if image.mode!='RGBA' or list(image.size)!=entry['size'] or not values<={0,255}:
        raise ValueError('像素尺寸、模式或透明度不合约')
    alpha=image.getchannel('A')
    if entry['kind']=='opaque' and values!={255}:raise ValueError('实体/流体贴图必须不透明')
    if entry['kind']=='window':
        if values!={0,255} or any(alpha.getpixel((x,y)) for x in range(4,12) for y in range(4,12)):
            raise ValueError('窗口中央必须保留8×8透明观察区域')
    if entry['kind']=='item':
        if values!={0,255} or any(alpha.getpixel((x,y)) for x in range(16) for y in range(16) if x in (0,15) or y in (0,15)):
            raise ValueError('物品必须有可见主体与全透明外圈')


def prepare():
    """先完整解析全部源和基线；调用者在此成功返回之前不能写任何输出。"""
    entries=load_manifest();palette=read_palette();images={};before={};retained={}
    if set(palette)!={e['palette'] for e in entries}:raise ValueError('清单与色板集合不同')
    for e in entries:
        key=e['game'] or 'item/lapis_dust.png'
        allowed={c.upper() for c in palette[e['palette']].values()}
        image=render_svg((ROOT/'sources'/e['source']).read_text(encoding='utf-8'),allowed,tuple(e['size']))
        validate_alpha(e,image); images[key]=image
        if e['game']:
            if key in GAME_FILES:
                with Image.open(ROOT/'baseline'/key) as old:before[key]=old.convert('RGBA')
                if list(before[key].size)!=e['size']:raise ValueError('基线尺寸不匹配')
            else:
                # 新路径没有旧图，预览中明确标为新增候选，不伪造历史基线。
                before[key]=image.copy()
        if key in RETAINED_ORIGINALS:
            data=(ROOT/'baseline'/key).read_bytes()
            if hashlib.sha256(data).hexdigest()!=RETAINED_ORIGINALS[key]:
                raise ValueError(f'{key}: 固定保留原图哈希不匹配')
            with Image.open(io.BytesIO(data)) as original:
                validate_alpha(e,original)
                images[key]=original.copy()
            # 预览只解码；输出直接使用已验证的原字节，禁止重编码或套用新色板。
            retained[key]=data
    return entries,images,before,retained


def png_bytes(image):
    """固定压缩参数，无时间元数据，便于逐文件哈希复验。"""
    buffer=io.BytesIO();image.save(buffer,format='PNG',optimize=False,compress_level=9)
    return buffer.getvalue()


def image_on(canvas,image,x,y,scale,background):
    """最近邻整数倍预览；保留flow纵横比，不压成正方形。"""
    width,height=image.width*scale,image.height*scale
    tile=Image.new('RGBA',(width,height),background)
    tile.alpha_composite(image.resize((width,height),Image.Resampling.NEAREST))
    canvas.alpha_composite(tile,(x,y))


def previews(entries,images,before):
    """分组分页同时展示原尺寸、前后、明暗底与平铺，不读取外部字体。"""
    outputs={};names=[]
    font=ImageFont.load_default(size=13);small=ImageFont.load_default(size=11)
    for group in ('minerals','reactor','items','coolant','centrifuge','fluids'):
        items=[e for e in entries if e['group']==group]
        for page in range((len(items)+7)//8):
            selected=items[page*8:page*8+8]
            sheet=Image.new('RGBA',(1184,960),'#17212A');draw=ImageDraw.Draw(sheet)
            draw.text((24,18),f'EXT-ART-08 / {group.upper()} / {page+1}',font=ImageFont.load_default(size=25),fill='#E3ECE8')
            draw.text((24,54),'BEFORE / AFTER  |  nearest neighbour  |  original size + light / dark + 2x2 tiling',font=font,fill='#A6B9C2')
            for i,e in enumerate(selected):
                x=24+(i%4)*290;y=95+(i//4)*424;key=e['game'] or 'item/lapis_dust.png';im=images[key];old=before.get(key,im)
                draw.rectangle((x,y,x+272,y+405),fill='#25323D')
                label=key[:-4];split=label.rfind('_',0,30) if len(label)>30 else -1
                lines=[label] if split<0 else [label[:split],label[split+1:]]
                for j,line in enumerate(lines):draw.text((x+10,y+10+j*16),line,font=font,fill='#ECF0EB')
                is_new=key in NEW_GAME_FILES
                draw.text((x+10,y+49),'NEW CANDIDATE' if is_new else ('BEFORE' if e['game'] else 'APPROVED PILOT'),font=small,fill='#A6B9C2');draw.text((x+138,y+49),'NEW' if is_new else ('RESTORED' if key in RETAINED_ORIGINALS else ('AFTER' if e['game'] else 'UNCHANGED')),font=small,fill='#DDE5CF')
                scale=2 if im.height==64 else 7
                image_on(sheet,old,x+10,y+67,scale,'#D8D9D4');image_on(sheet,im,x+138,y+67,scale,'#D8D9D4')
                image_on(sheet,im,x+10,y+208,1,'#69737A');draw.text((x+38,y+210),'1x',font=small,fill='#A6B9C2')
                draw.text((x+10,y+286),'LIGHT / DARK',font=small,fill='#A6B9C2')
                image_on(sheet,im,x+10,y+306,1 if im.height==64 else 4,'#EBE5DA')
                image_on(sheet,im,x+84,y+306,1 if im.height==64 else 4,'#111720')
                tile_y=268 if im.height==64 else 306
                draw.text((x+163,y+tile_y-20),'2x2 TILE',font=small,fill='#A6B9C2')
                # flow 使用完整长画布的2×2平铺；普通图每单元48×48。
                s=1 if im.height==64 else 3
                for ty in range(2):
                    for tx in range(2):image_on(sheet,im,x+163+tx*im.width*s,y+tile_y+ty*im.height*s,s,'#69737A')
            name=f'previews/{group}-{page+1}.png';outputs[name]=png_bytes(sheet);names.append(name)
    links=''.join(f'<figure><img src="{name}" alt="{name}"><figcaption>{name}</figcaption></figure>' for name in names)
    overview_rows=(len(entries)+7)//8
    overview_height=95+overview_rows*130+20
    overview=Image.new('RGBA',(1184,overview_height),'#17212A');od=ImageDraw.Draw(overview)
    od.text((24,18),'EXT-ART-08 / 82 GAME TEXTURES + LAPIS PILOT',font=ImageFont.load_default(size=22),fill='#E3ECE8')
    od.text((24,52),'69 preserved paths + 13 new fuel-processing textures / preview pending',font=font,fill='#A6B9C2')
    for i,e in enumerate(entries):
        x=24+(i%8)*145;y=95+(i//8)*130;key=e['game'] or 'item/lapis_dust.png';im=images[key]
        image_on(overview,im,x,y,1 if im.height==64 else 4,'#596773')
        label=key.split('/')[-1][:-4]
        split=label.rfind('_',0,21) if len(label)>21 else -1
        lines=[label] if split<0 else [label[:split],label[split+1:]]
        for j,line in enumerate(lines):od.text((x,y+72+j*13),line,font=small,fill='#E3ECE8')
        od.text((x,y+102),key.split('/')[0],font=small,fill='#A6B9C2')
    outputs['preview.png']=png_bytes(overview)
    outputs['preview.html']=('''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><title>EXT-ART-08 贴图预览</title><style>body{background:#17212a;color:#e3ece8;font:16px system-ui;margin:24px}figure{margin:24px 0}img{max-width:100%;height:auto;image-rendering:pixelated}figcaption{margin-top:8px}</style><h1>EXT-ART-08 贴图预览</h1><p>包含69项开工前游戏PNG保护快照、本批13张新增贴图，以及青金石粉工具候选。新图不登记旧图基线；既有冷却剂原图按批准字节保留。每页含原尺寸、明暗底与平铺。</p>'''+links+'</html>\n').encode('utf-8')
    return outputs


def main():
    parser=argparse.ArgumentParser(description='固定69项既有PNG及十三条EXT-ART-08新增路径；默认不接入游戏')
    parser.add_argument('--install',action='store_true',help='显式接入69张既有贴图和十三项新增素材PNG')
    args=parser.parse_args()
    entries,images,before,retained=prepare()
    outputs={f'generated/{key}':retained[key] if key in retained else png_bytes(im) for key,im in images.items()}
    outputs.update(previews(entries,images,before))
    game_outputs={e['game']:outputs['generated/'+e['game']] for e in entries if e['game']}
    if args.install:
        actual={p.relative_to(GAME_ROOT).as_posix() for p in GAME_ROOT.rglob('*.png')}
        plate_stage = GAME_FILES | frozenset({'item/lead_plate.png', 'item/tin_plate.png'})
        nugget_stage = GAME_FILES | frozenset({'item/lead_plate.png', 'item/tin_plate.png', 'item/lead_nugget.png', 'item/tin_nugget.png'})
        previous_material_stage = GAME_FILES | frozenset({'item/lead_plate.png', 'item/tin_plate.png', 'item/lead_nugget.png', 'item/tin_nugget.png'})
        steel_stage = previous_material_stage | frozenset({'item/iron_dust.png', 'item/coal_dust.png', 'item/charcoal_dust.png', 'item/steel_dust.png', 'item/steel_ingot.png'})
        sensor_stage = steel_stage | frozenset({'item/tin_wire.png', 'item/industrial_sensor.png', 'item/radiation_sensor.png', 'item/incomplete_industrial_sensor.png', 'item/incomplete_radiation_sensor.png'})
        preserved_stage = GAME_FILES | PREVIOUS_NEW_GAME_FILES
        if actual not in (GAME_FILES, plate_stage, nugget_stage, previous_material_stage, steel_stage, sensor_stage, preserved_stage, ALLOWED_GAME_FILES):
            raise ValueError('实际游戏PNG集合必须为69项冻结既有白名单或完整82项白名单，拒绝接入')
        for e in entries:
            if not e['game']:continue
            target=GAME_ROOT/e['game']
            if not target.resolve().is_relative_to(GAME_ROOT.resolve()):raise ValueError('游戏路径越界')
            if e['game'] in NEW_GAME_FILES and not target.exists():
                continue
            with Image.open(target) as current:
                if list(current.size)!=e['size']:raise ValueError('游戏现有尺寸改变，拒绝覆盖')
    for relative,data in outputs.items():
        path=ROOT/relative
        if path.exists() and path.read_bytes()==data:continue
        path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
    if args.install:
        for relative,data in game_outputs.items():
            path=GAME_ROOT/relative
            if path.exists() and path.read_bytes()==data:continue
            path.write_bytes(data)
    print(f'PASS: {len(images)} generated PNG; 82 game targets (51 historical + 31 new); install={args.install}; {len(outputs)-len(images)-2} comparison pages + overview')
    print('manifest sha256='+hashlib.sha256((ROOT/'manifest.json').read_bytes()).hexdigest())


if __name__=='__main__':main()
