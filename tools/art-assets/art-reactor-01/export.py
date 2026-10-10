from __future__ import annotations
import argparse
import importlib.util
import io
from pathlib import Path
from PIL import Image, ImageDraw, ImageEnhance, ImageFont, ImageOps

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / 'tools/art-assets'
EVIDENCE = ROOT / 'build/reports/art/ART-REACTOR-01'
SOURCES = ASSETS / 'sources/block'
GENERATED = ASSETS / 'generated/block'
GAMES = ROOT / 'src/main/resources/assets/create_nuclear_industry/textures/block'
FONT = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 14)
NAMES = ('reactor_casing_side','reactor_casing_top','reactor_casing_bottom','reactor_hot_port_side','reactor_hot_port_top','reactor_cold_port_side','reactor_cold_port_top')
_spec = importlib.util.spec_from_file_location('art_shared_svg_export', ASSETS / 'export.py')
if _spec is None or _spec.loader is None:
    raise RuntimeError('无法加载共用严格SVG渲染器')
def draw_label(draw, position, content, fill):
    draw.text(position, content, font=FONT, fill=fill)

_shared = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_shared)


def read_inputs(source_overrides=None):
    """先校验七份SVG和色板，再返回RGBA像素与确定性PNG字节。"""
    palette = _shared.read_palette()
    images, pngs = {}, {}
    for name in NAMES:
        if name not in palette:
            raise ValueError(f'{name}: 缺少色板条目')
        source = source_overrides[name] if source_overrides and name in source_overrides else (SOURCES / f'{name}.svg').read_text(encoding='utf-8')
        image = _shared.render_svg(source, set(palette[name].values()), (16,16))
        if image.mode != 'RGBA' or image.size != (16,16) or len(set(image.get_flattened_data())) > 16 or any(p[3] != 255 for p in image.get_flattened_data()):
            raise ValueError(f'{name}: 必须为16×16、最多16色、全不透明RGBA')
        stream = io.BytesIO(); image.save(stream, format='PNG', optimize=False)
        images[name], pngs[name] = image, stream.getvalue()
    return images, pngs


def tile(image, scale=4):
    """生成最近邻的2×2重复纹理。"""
    small = image.resize((16*scale,16*scale), Image.Resampling.NEAREST)
    result = Image.new('RGBA',(small.width*2,small.height*2))
    for x in (0,small.width):
        for y in (0,small.height): result.alpha_composite(small,(x,y))
    return result


def texture_preview(images):
    """生成BEFORE/AFTER原尺寸、最近邻、明暗底、平铺与缩小模拟页。"""
    page=Image.new('RGB',(1300,1320),'#E7E5DD'); d=ImageDraw.Draw(page)
    draw_label(d,(24,18),'ART-REACTOR-01 | BEFORE / AFTER 16×16贴图对照','#202324')
    draw_label(d,(24,42),'原尺寸同底对照、最近邻放大、2×2平铺；缩小、灰度和降亮仅作离线辨识模拟。','#303638')
    for x,label_text in ((24,'资源名'),(228,'BEFORE'),(292,'AFTER'),(360,'AFTER暗底'),(420,'NN×6'),(570,'缩小 / 灰度 / 降亮'),(760,'AFTER 2×2平铺4×')): draw_label(d,(x,76),label_text,'#303638')
    for i,name in enumerate(NAMES):
        y=108+i*166; im=images[name]; before=Image.open(EVIDENCE/'originals'/f'{name}.png').convert('RGBA')
        draw_label(d,(24,y),name,'#202324')
        d.rectangle((228,y+22,247,y+41),fill='#F5F3E8'); d.rectangle((292,y+22,311,y+41),fill='#F5F3E8'); d.rectangle((360,y+22,379,y+41),fill='#303638')
        page.paste(before,(230,y+24)); page.paste(im,(294,y+24)); page.paste(im,(362,y+24))
        page.paste(im.resize((96,96),Image.Resampling.NEAREST),(420,y+22))
        small=im.resize((8,8),Image.Resampling.BOX).resize((48,48),Image.Resampling.NEAREST)
        gray=ImageOps.grayscale(im).resize((48,48),Image.Resampling.NEAREST)
        dim=ImageEnhance.Brightness(im).enhance(.55).resize((48,48),Image.Resampling.NEAREST)
        page.paste(small.convert('RGB'),(570,y+22)); page.paste(gray.convert('RGB'),(620,y+22)); page.paste(dim.convert('RGB'),(670,y+22))
        draw_label(d,(570,y+74),'缩小','#303638'); draw_label(d,(620,y+74),'灰度','#303638'); draw_label(d,(670,y+74),'降亮55%','#303638')
        page.paste(tile(im).convert('RGB'),(760,y+22)); d.line((24,y+148,1272,y+148),fill='#B8B7AE')
    return page


def model_preview(images):
    """按cube_bottom_top模型投影完整顶面与左右侧面，并展示底材质。"""
    page=Image.new('RGB',(1300,470),'#D9D8D0'); d=ImageDraw.Draw(page)
    draw_label(d,(24,18),'AFTER | 现行 cube_bottom_top 面贴图等距预览','#202324')
    draw_label(d,(24,42),'三个闭合方块同尺度；顶部取 *_top、左右侧取 *_side，底面单独展示 *_bottom。','#303638')
    def draw_cube(cx,cy,top,side,px=4.0,py=2.0,vh=4.0):
        def face(im,project,shade):
            pix=im.load()
            for v in range(16):
                for u in range(16):
                    q=[project(u,v),project(u+1,v),project(u+1,v+1),project(u,v+1)]
                    fill=tuple(max(0,min(255,int(c*shade))) for c in pix[u,v][:3])
                    d.polygon([(round(x),round(y)) for x,y in q],fill=fill)
        face(side,lambda u,v:(cx-16*px+u*px,cy+u*py+v*vh),.86)
        face(side,lambda u,v:(cx+u*px,cy+16*py-u*py+v*vh),.72)
        face(top,lambda u,v:(cx+(u-v)*px,cy-16*py+(u+v)*py),1.0)
    entries=(('reactor_casing',images['reactor_casing_top'],images['reactor_casing_side']),('reactor_hot_port',images['reactor_hot_port_top'],images['reactor_hot_port_side']),('reactor_cold_port',images['reactor_cold_port_top'],images['reactor_cold_port_side']))
    for (name,top,side),cx in zip(entries,(190,520,850)):
        draw_cube(cx,175,top,side); draw_label(d,(cx-78,390),name,'#202324')
    draw_label(d,(1040,114),'reactor_casing_bottom','#202324')
    page.paste(images['reactor_casing_bottom'].resize((112,112),Image.Resampling.NEAREST),(1060,145))
    draw_label(d,(24,440),'静态材质投影；实际客户端角度、光照和可视度仍需游戏内确认。','#303638')
    return page


def assembly_preview(images):
    """绘制由同类方块组成的闭合2×2等距外壳组合。"""
    page=Image.new('RGB',(720,620),'#D9D8D0'); d=ImageDraw.Draw(page)
    draw_label(d,(24,18),'AFTER | 外壳2×2 cube_bottom_top拼接','#202324')
    draw_label(d,(24,42),'四个方块顶面连续铺开，外周侧面闭合；重复边框用于检查方块接缝。','#303638')
    px,py,vh=5.5,2.75,5.5; origin=(360,270)
    def face(cx,cy,texture,project,shade):
        pix=texture.load()
        for v in range(16):
            for u in range(16):
                q=[project(u,v),project(u+1,v),project(u+1,v+1),project(u,v+1)]
                fill=tuple(max(0,min(255,int(c*shade))) for c in pix[u,v][:3])
                d.polygon([(round(x),round(y)) for x,y in q],fill=fill)
    def position(i,j): return origin[0]+(i-j)*16*px,origin[1]+(i+j)*16*py
    for i in range(2):
        for j in range(2):
            cx,cy=position(i,j)
            if j==1: face(cx,cy,images['reactor_casing_side'],lambda u,v,cx=cx,cy=cy:(cx-16*px+u*px,cy+u*py+v*vh),.86)
            if i==1: face(cx,cy,images['reactor_casing_side'],lambda u,v,cx=cx,cy=cy:(cx+u*px,cy+16*py-u*py+v*vh),.72)
    for i in range(2):
        for j in range(2):
            cx,cy=position(i,j)
            face(cx,cy,images['reactor_casing_top'],lambda u,v,cx=cx,cy=cy:(cx+(u-v)*px,cy-16*py+(u+v)*py),1.0)
    draw_label(d,(24,590),'静态面投影；相邻外壳沿现行方块网格对齐。','#303638')
    return page

def encoded(image):
    """将离线预览编码为PNG。"""
    out=io.BytesIO(); image.save(out,format='PNG',optimize=False); return out.getvalue()


def write_files(files):
    """仅更新字节变化的目标，并在写入失败时恢复已触及文件。"""
    old={p:p.read_bytes() if p.exists() else None for p in files}; touched=[]
    try:
        for path,data in files.items():
            if old[path]==data: continue
            path.parent.mkdir(parents=True,exist_ok=True); touched.append(path); path.write_bytes(data)
    except Exception:
        for path in reversed(touched):
            if old[path] is None: path.unlink(missing_ok=True)
            else: path.write_bytes(old[path])
        raise


def run(install=False,check=False,source_overrides=None):
    """执行七项导出、安装或只读字节核对。"""
    images,pngs=read_inputs(source_overrides)
    if check:
        bad=[n for n in NAMES if not (GAMES/f'{n}.png').is_file() or (GAMES/f'{n}.png').read_bytes()!=pngs[n]]
        if bad: raise SystemExit('游戏贴图与当前SVG导出不一致: '+', '.join(bad))
        print(f'只读检查通过：{len(NAMES)}张游戏PNG与SVG逐字节一致'); return
    texture=texture_preview(images); model=model_preview(images)
    files={GENERATED/f'{n}.png':pngs[n] for n in NAMES}
    files.update({EVIDENCE/'texture-preview.png':encoded(texture),EVIDENCE/'model-preview.png':encoded(model),EVIDENCE/'casing-assembly-preview.png':encoded(assembly_preview(images)),EVIDENCE/'preview.png':encoded(texture)})
    if install: files.update({GAMES/f'{n}.png':pngs[n] for n in NAMES})
    write_files(files)
    print('已导出并安装七张游戏贴图' if install else '已导出七张工具PNG及离线预览')


def main():
    parser=argparse.ArgumentParser(description='ART-REACTOR-01七张反应堆贴图定向导出')
    group=parser.add_mutually_exclusive_group(); group.add_argument('--install',action='store_true',help='安装本批七张游戏贴图'); group.add_argument('--check',action='store_true',help='只读核对游戏PNG与SVG字节')
    args=parser.parse_args(); run(args.install,args.check)

if __name__=='__main__': main()

