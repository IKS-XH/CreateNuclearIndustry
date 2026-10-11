from pathlib import Path
from xml.etree import ElementTree as ET
import re, json, subprocess
base=Path(__file__).parent
folders=['01-horizontal-hoop','02-refractory-jacket','03-steel-frame']
kinds=['casing_side','casing_top','window_frame','water_port','steam_port','hot_port','cold_port','controller_face','safety_valve']
fail=[]
def fail_if(test,msg):
 if test:fail.append(msg)
for folder in folders:
 for kind in kinds:
  p=base/folder/f'{kind}.svg'
  if not p.exists():fail.append(f'missing {p.relative_to(base)}');continue
  try:root=ET.parse(p).getroot()
  except Exception as e:fail.append(f'invalid tile XML {p.name}: {e}');continue
  fail_if(root.attrib.get('width')!='16' or root.attrib.get('height')!='16' or root.attrib.get('viewBox')!='0 0 16 16','bad 16x16 canvas '+str(p))
  fail_if(root.attrib.get('shape-rendering')!='crispEdges','missing crispEdges '+str(p))
  palette=set()
  for node in root:
   fail_if(not node.tag.endswith('rect'),f'non-rect tile primitive {p.name}: {node.tag}')
   for attr in ('x','y','width','height'):fail_if(not re.fullmatch(r'\d+',node.attrib.get(attr,'')),f'noninteger {attr}: {p.name}')
   color=node.attrib.get('fill','');fail_if(not re.fullmatch(r'#[0-9A-Fa-f]{6}',color),f'non-RGB fill {p.name}: {color}');palette.add(color.lower())
  fail_if(len(palette)>14,f'palette exceeds 14: {p.name} ({len(palette)})')
# 分别检查横箍节奏、耐火砖表面比例和骨架纵轨像素结构。
def rows(path):
 g=[['#000000']*16 for _ in range(16)]
 for r in ET.parse(path).getroot():
  x,y,w,h=map(int,(r.attrib['x'],r.attrib['y'],r.attrib['width'],r.attrib['height']));c=r.attrib['fill'].lower()
  for yy in range(y,y+h):
   for xx in range(x,x+w):g[yy][xx]=c
 return g
a=rows(base/folders[0]/'casing_side.svg');b=rows(base/folders[1]/'casing_side.svg');c=rows(base/folders[2]/'casing_side.svg')
fail_if(sum(a[y].count('#a6b0ae') for y in range(16))<24,'A横箍亮边不足')
brick={'#b99775','#e4d2b2','#c5ad8e','#a57e5f'}
fail_if(sum(1 for row in b for px in row if px in brick)<75,'B耐火砖表面比例不足')
fail_if(sum(1 for y in range(2,14) for x in (2,3,12,13) if c[y][x] in {'#8e9da0','#bdc6c1'})<30,'C强化钢外框纵轨不足')
# 校验组合SVG的数字、相对图片引用、内嵌符号和实际投影面。
for name in ('overview.svg',)+tuple(f'{folder}/board.svg' for folder in folders):
 p=base/name
 try:r=ET.parse(p).getroot()
 except Exception as e:fail.append(f'invalid composite SVG {name}: {e}');continue
 ids={n.attrib['id'] for n in r.iter() if 'id' in n.attrib};face_axes=set();tile_scales=[]
 tiers={n.attrib.get('data-valve-tier') for n in r.iter() if 'data-valve-tier' in n.attrib}
 fail_if(tiers!={'1','2','3','4'},f'four valve tiers missing in {name}: {tiers}')
 if name=='overview.svg':
  fail_if(not {'01-horizontal-hoop-casing_side','02-refractory-jacket-casing_side','03-steel-frame-casing_side'}.issubset(ids),'overview lacks inline tile symbols')
  for folder in folders:fail_if(sum(1 for n in r.iter() if n.tag.endswith('use') and n.attrib.get('href')==f'#{folder}-window_frame')<3,f'missing visible narrow windows for {folder}')
 for n in r.iter():
  for attr in ('x','y','width','height','cx','cy','r','rx','ry','font-size','stroke-width'):
   if attr in n.attrib:fail_if(not re.fullmatch(r'-?\d+(?:\.\d+)?',n.attrib[attr]),f'unparseable SVG number {name} {attr}={n.attrib[attr]}')
  if 'points' in n.attrib:fail_if(not re.fullmatch(r'-?\d+(?:\.\d+)?(?:,-?\d+(?:\.\d+)?)(?:\s+-?\d+(?:\.\d+)?,-?\d+(?:\.\d+)?)*',n.attrib['points']),f'unparseable SVG points {name}')
  if n.tag.endswith('image'):
   href=n.attrib.get('href','')
   fail_if(href.lower().endswith('.svg') or not href.lower().endswith('.png'),f'unexpected image reference {name}: {href}')
   fail_if(not (p.parent/href).resolve().is_file(),f'broken external image reference {name}: {href}')
  if n.tag.endswith('use'):
   href=n.attrib.get('href','');fail_if(not href.startswith('#') or href[1:] not in ids,f'unresolved SVG use in {name}: {href}')
   m=re.fullmatch(r'matrix\(([^)]+)\)',n.attrib.get('transform',''))
   if m:
    try:a1,b1,c1,d1,e1,f1=map(float,m.group(1).split(','));det=a1*d1-b1*c1
    except Exception:fail.append(f'non-numeric projection transform {name}');continue
    fail_if(abs(det)<1e-8,f'degenerate projected tile face {name}: determinant={det}')
    if name=='overview.svg':
     if a1>0 and b1>0 and abs(c1)<1e-8:face_axes.add('left')
     if a1>0 and b1<0 and abs(c1)<1e-8:face_axes.add('right')
     if a1>0 and b1>0 and c1<0:face_axes.add('top')
     if abs(b1)<1e-8 and abs(c1)<1e-8 and abs(a1-d1)<1e-8:tile_scales.append(a1)
 if name=='overview.svg':
  fail_if(face_axes!={'left','right','top'},'overview does not project three distinct faces')
  fail_if(sum(1 for v in tile_scales if abs(v-1.5)<1e-6)<18,'overview 3x2 samples are not square single-tile cells')
# 用只读Git差异确认本批没有改正式纹理。
proc=subprocess.run(['git','diff','--name-only','--','src/main/resources/assets/create_nuclear_industry/textures'],capture_output=True,text=True)
fail_if(proc.returncode!=0,'formal texture diff query failed')
fail_if(bool(proc.stdout.strip()),'formal texture paths differ: '+proc.stdout.strip())
manifest=json.loads((base/'manifest.json').read_text(encoding='utf-8'))
fail_if(manifest.get('png_created')!=0 or manifest.get('formal_resources_replaced') is not False,'manifest indicates forbidden output')
fail_if(list(base.rglob('*.png'))!=[],'PNG found in concepts directory')
fail_if(not (base/'index.html').exists(),'missing browser switch page')
print('styles=3; tile_svg=27; overview=present; switchable_boards=3; png_created=0')
print('integer_rects=PASS; palette<=14=PASS; material_geometry=PASS; visible_windows=PASS; four_tier_valve=PASS; inline_tile_refs=PASS; three_face_projection=PASS; square_tile_samples=PASS; valid_references=PASS; formal_texture_diff=NONE' if not fail else 'CHECKS_FAILED')
if fail:
 for msg in fail:print('FAIL:',msg)
 raise SystemExit(1)


