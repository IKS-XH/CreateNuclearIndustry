"""逐像素校验最终PNG来自已保存SVG，并核原图alpha/尺寸；不写生产资源。"""
import argparse, io, json, zipfile
from pathlib import Path
import xml.etree.ElementTree as ET
from PIL import Image
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
manifest=json.loads((HERE/'sources.json').read_text(encoding='utf-8'))
parser=argparse.ArgumentParser(); parser.add_argument('--pump-only',action='store_true',help='只核泵SVG/PNG，不重复其他资源'); args=parser.parse_args()
with zipfile.ZipFile(manifest['dependency']) as z:
    for svg_path in sorted((HERE/'svg').glob('steel_pump.svg' if args.pump_only else '*.svg')):
        svg=ET.parse(svg_path).getroot(); size=(int(svg.attrib['width']),int(svg.attrib['height']))
        expected=Image.new('RGBA',size)
        for rect in svg:
            if not rect.tag.endswith('rect'): continue
            attrs=rect.attrib; color=attrs['fill'][1:]; expected.putpixel((int(attrs['x']),int(attrs['y'])),tuple(int(color[i:i+2],16) for i in (0,2,4))+(int(attrs['data-alpha']),))
        png=ROOT/'src/main/resources/assets/create_nuclear_industry/textures/block/industrial_fluid_visual_01'/(svg_path.stem+'.png')
        actual=Image.open(png).convert('RGBA')
        assert actual.size==size and actual.tobytes()==expected.tobytes(),svg_path
        if svg_path.stem in manifest['textures']:
            original=Image.open(io.BytesIO(z.read(manifest['textures'][svg_path.stem]['source']))).convert('RGBA')
            assert original.size==size
            assert [p[3] for p in actual.get_flattened_data()]==[p[3] for p in original.get_flattened_data()]
            assert len(set(actual.get_flattened_data()))>8,'禁止整图抹平'
        print(svg_path.stem, size, 'SVG/PNG一致，原alpha保持')
