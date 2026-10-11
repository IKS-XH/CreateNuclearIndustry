from pathlib import Path
import json
import xml.etree.ElementTree as ET
root=Path('tools/art-assets/boiler-concepts-05')
styles={
'A':('01-horizontal-hoop','横箍压力舱',['#252B30','#3D4549','#657176','#A6B0AE','#D6D6C8','#7B5A3C','#B98A62','#D7AE55','#5B8FA7','#B9573D'], '深钢壳与横向压力箍'),
'B':('02-refractory-jacket','耐火砖护套',['#2A2826','#51463E','#796653','#B99775','#E4D2B2','#C5AD8E','#A57E5F','#68777A','#4F8CA4','#B8583E'], '耐火砖护套与钢收边'),
'C':('03-steel-frame','钢骨架舱',['#20272B','#364147','#52636A','#8E9DA0','#BDC6C1','#75533A','#B28A61','#D3AD62','#548CA2','#B8533A'], '强化钢骨架与内凹板件')}
# 调色板字符0到9映射有限RGB色，逐行合并相邻像素并输出整数矩形。
def tile(key,kind):
 g=[['0']*16 for _ in range(16)]
 def r(x,y,w,h,c):
  for yy in range(y,y+h):
   for xx in range(x,x+w):g[yy][xx]=c
 if kind=='casing_side':
  r(1,1,14,14,'1');r(2,2,12,12,'2')
  if key=='A':
   r(2,3,12,2,'3');r(2,5,12,1,'0');r(2,10,12,1,'0');r(2,11,12,2,'3');r(3,6,10,4,'1');r(4,6,8,1,'3');r(7,7,2,3,'5');r(7,7,1,1,'6');r(3,5,1,1,'7');r(12,5,1,1,'7');r(3,10,1,1,'7');r(12,10,1,1,'7')
  elif key=='B':
   r(2,2,12,12,'4')
   for y in (2,6,10,14):r(2,y,12,1,'1')
   for y,off in ((3,0),(7,4),(11,0),(13,4)):
    r(2+off,y,1,3,'1');r(2,y,1,3,'2');r(13,y,1,3,'2')
   r(3,3,10,3,'3');r(3,7,10,3,'5');r(3,11,10,2,'3')
  else:
   r(2,2,12,12,'1');r(4,3,8,10,'2');r(5,4,6,8,'1')
   for y in range(2,14):r(2,y,2,1,'3' if y%3 else '4');r(12,y,2,1,'2' if y%3 else '3')
   for i in range(7):r(4+i,4+i,2,1,'3')
   r(5,4,6,1,'4');r(5,11,6,1,'0')
 elif kind=='casing_top':
  r(1,1,14,14,'1');r(2,2,12,12,'2')
  if key=='A':r(2,2,12,2,'3');r(2,12,12,2,'0');r(2,4,2,8,'3');r(12,4,2,8,'0');r(6,6,4,4,'1');r(7,7,2,2,'2');r(3,3,1,1,'7');r(12,3,1,1,'7')
  elif key=='B':
   r(2,2,12,12,'4')
   for y in (2,7,12):r(2,y,12,1,'1')
   for x,y in ((2,3),(7,3),(12,3),(4,8),(9,8)):r(x,y,1,4,'1')
   r(3,3,3,3,'3');r(8,8,3,3,'5')
  else:
   r(2,2,12,12,'1');r(4,4,8,8,'2');r(5,5,6,6,'1')
   for x in (2,3,12,13):r(x,2,1,12,'3')
   for y in (2,3,12,13):r(2,y,12,1,'3')
   r(7,7,2,2,'6')
 elif kind=='window_frame':
  r(2,1,12,14,'0');r(3,2,10,12,'3');r(4,3,8,10,'1');r(5,4,6,8,'8');r(6,5,2,6,'2');r(9,5,1,6,'3');r(4,3,8,1,'4')
  if key=='B':r(1,2,2,12,'2');r(13,2,2,12,'1');r(5,5,6,1,'4');r(5,10,6,1,'4')
  if key=='C':r(2,1,2,14,'3');r(12,1,2,14,'2');r(5,4,1,8,'4');r(10,4,1,8,'1')
 elif kind in ('water_port','steam_port','hot_port','cold_port','controller_face'):
  r(2,2,12,12,'1');r(3,3,10,10,'2');r(4,4,8,8,'0');r(5,5,6,6,'3');r(6,6,4,4,'1')
  if kind=='water_port':r(6,5,4,2,'8');r(6,10,4,1,'8')
  if kind=='steam_port':r(5,5,6,2,'3');r(7,7,2,4,'4');r(6,11,4,1,'3')
  if kind=='hot_port':r(5,5,6,2,'9');r(7,7,2,4,'9');r(6,11,4,1,'7')
  if kind=='cold_port':r(5,5,2,6,'8');r(9,5,2,6,'8');r(7,6,2,4,'2')
  if kind=='controller_face':r(6,5,4,2,'4');r(5,8,6,2,'7');r(6,11,4,1,'0')
 elif kind=='safety_valve':
  r(3,12,10,2,'0');r(4,10,8,2,'2');r(5,8,6,2,'3');r(6,6,4,2,'1');r(7,4,2,2,'4');r(5,13,2,1,'7');r(9,13,2,1,'7')
  if key=='B':r(4,12,8,2,'5');r(6,8,4,2,'4')
  if key=='C':r(3,11,2,3,'3');r(11,11,2,3,'3')
 # 将同一行相邻同色像素合并成整数矩形。
 cs=styles[key][2];lines=['<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges">']
 for y,row in enumerate(g):
  x=0
  while x<16:
   c=row[x];e=x+1
   while e<16 and row[e]==c:e+=1
   lines.append(f'<rect x="{x}" y="{y}" width="{e-x}" height="1" fill="{cs[int(c)]}"/>');x=e
 lines.append('</svg>');return '\n'.join(lines)+'\n'
kinds=['casing_side','casing_top','window_frame','water_port','steam_port','hot_port','cold_port','controller_face','safety_valve']
for key,(folder,title,pal,desc) in styles.items():
 d=root/folder;d.mkdir(parents=True,exist_ok=True)
 for kind in kinds:(d/f'{kind}.svg').write_text(tile(key,kind),encoding='utf-8')
# 统一SVG组件与等距投影；像素尺寸以16为源单位，面向屏幕坐标投影后保持每块一张贴图。
def defs(folder,prefix=None):
 # 将本批每张低分辨率SVG的rect内联成symbol，独立预览不依赖子SVG相对路径。
 pieces=[]
 for k in kinds:
  raw=(root/folder/f'{k}.svg').read_text(encoding='utf-8')
  inner=raw[raw.index('>')+1:raw.rfind('</svg>')]
  pieces.append(f'<symbol id="{folder}-{k}" width="16" height="16" viewBox="0 0 16 16">{inner}</symbol>')
  pieces.append(f'<pattern id="{folder}-pattern-{k}" width="16" height="16" patternUnits="userSpaceOnUse"><use href="#{folder}-{k}" width="16" height="16"/></pattern>')
 return '<defs>'+''.join(pieces)+'</defs>'
def txt(x,y,s,size=15,color='#344046',weight=700):return f'<text x="{x}" y="{y}" font-family="Segoe UI,Arial,sans-serif" font-size="{size}" font-weight="{weight}" fill="{color}">{s}</text>'
def poly(points,fill):return f'<polygon points="{points}" fill="{fill}" stroke="#242c30" stroke-width="1.3"/>'
def put_tile(folder,kind,matrix,board=False):
 # 每格贴图用同一组整数投影轴映射，像素单位从16×16变换到对应面。
 return f'<use href="#{folder}-{kind}" width="16" height="16" transform="matrix({matrix})"/>'
def thumb(folder,kind,x,y,size):
 scale=size/16
 return f'<use href="#{folder}-{kind}" width="16" height="16" transform="matrix({scale},0,0,{scale},{x},{y})"/>'
def cube(cx,top,scale,folder,key,board=False):
 dx,dy,H=30*scale,15*scale,27*scale;out=[]
 # 侧面每格的投影面积必须非零；深度轴为(-X,+Y)，竖轴为(0,+Y)。
 for j in reversed(range(5)):
  for level in range(7):
   x=cx-5*dx+j*dx;y=top+5*dy+j*dy+level*H
   m=f'{dx/16},{dy/16},0,{H/16},{x},{y}'
   out.append(put_tile(folder,'casing_side',m,board));pts=f'{x},{y} {x+dx},{y-dy} {x+dx},{y-dy+H} {x},{y+H}';out.append(poly(pts,'none'))
 for i in range(5):
  for level in range(7):
   x=cx+i*dx;y=top+10*dy-i*dy+level*H;m=f'{dx/16},{-dy/16},0,{H/16},{x},{y}'
   out.append(put_tile(folder,'casing_side',m,board));pts=f'{x},{y} {x+dx},{y-dy} {x+dx},{y-dy+H} {x},{y+H}';out.append(poly(pts,'none'))
 # 顶面两条水平轴分别沿(+X,+Y)和(-X,+Y)，网格格点不共线且完整闭合。
 for j in range(5):
  for i in range(5):
   x=cx+i*dx-j*dx;y=top+i*dy+j*dy
   kind='casing_top'
   m=f'{dx/16},{dy/16},{-dx/16},{dy/16},{x},{y}'
   out.append(put_tile(folder,kind,m,board));pts=f'{x},{y} {x+dx},{y+dy} {x+dx-dx},{y+dy+dy} {x-dx},{y+dy}';out.append(poly(pts,'none'))
 # 按现有模型0..7/7..10/10..14/14..16的X/Z层级，用同一投影轴绘制四层矮阀。
 def vpoint(vx,vz,vy):return (cx+(vx-vz)*dx/16,top+4*dy+(vx+vz)*dy/16-vy*H/16)
 def vpoly(points,color,tier):
  coords=' '.join(f'{x},{y}' for x,y in points)
  return f'<polygon data-valve-tier="{tier}" points="{coords}" fill="{color}" stroke="#20272b" stroke-width="{0.7*scale}"/>'
 tiers=[(0,16,0,16,0,7,'#3D4549','#657176','#A6B0AE'),(5,11,5,11,7,10,'#364147','#52636A','#8E9DA0'),(6,10,6,10,10,14,'#3D4549','#657176','#A6B0AE'),(3,13,3,13,14,16,'#75533A','#B28A61','#D3AD62')]
 for tier,(x0,x1,z0,z1,y0,y1,leftc,frontc,topc) in enumerate(tiers,1):
  out.append(vpoly([vpoint(x0,z0,y0),vpoint(x0,z1,y0),vpoint(x0,z1,y1),vpoint(x0,z0,y1)],leftc,tier))
  out.append(vpoly([vpoint(x0,z1,y0),vpoint(x1,z1,y0),vpoint(x1,z1,y1),vpoint(x0,z1,y1)],frontc,tier))
  out.append(vpoly([vpoint(x0,z0,y1),vpoint(x1,z0,y1),vpoint(x1,z1,y1),vpoint(x0,z1,y1)],topc,tier)) # 压力箍沿两侧共享边闭合；砖缝或骨架则保持各自独有的构造节奏。
 if key=='A':
  for lev in (1,3,5):
   y=top+lev*H
   out.append(f'<path d="M {cx-5*dx} {top+5*dy+y-top} L {cx} {top+10*dy+y-top} L {cx+5*dx} {top+5*dy+y-top}" fill="none" stroke="#171c1f" stroke-width="{4*scale}"/><path d="M {cx-5*dx} {top+5*dy+y-top-2*scale} L {cx} {top+10*dy+y-top-2*scale} L {cx+5*dx} {top+5*dy+y-top-2*scale}" fill="none" stroke="#A6B0AE" stroke-width="{1.4*scale}"/>')
 elif key=='B':
  for j in range(5):
   for lev in (2,4,6):
    x=cx-5*dx+j*dx;y=top+5*dy+j*dy+lev*H
    out.append(f'<path d="M {x} {y} L {x+dx} {y+dy}" stroke="#342d28" stroke-width="{2.2*scale}"/>')
  out.append(f'<path d="M {cx-5*dx} {top+5*dy} v {7*H} M {cx+5*dx} {top+5*dy} v {7*H}" stroke="#3c4140" stroke-width="{4*scale}"/>')
 else:
  out.append(f'<path d="M {cx-5*dx} {top+5*dy} v {7*H} M {cx} {top+10*dy} v {7*H} M {cx+5*dx} {top+5*dy} v {7*H}" stroke="#1a2023" stroke-width="{5*scale}"/>')
  for lev in (1,3,5):
   y=top+lev*H
   out.append(f'<path d="M {cx-5*dx} {y+5*dy} l {1.4*dx} {1.4*dy} l {1.4*dx} {-1.4*dy}" fill="none" stroke="#A6B0AE" stroke-width="{2.6*scale}"/>')
 # 接口放在正确高度；端口贴图映射到一个整格面，位置由所在格确定。
 for i,lev,k in ((1,5,'steam_port'),(3,3,'cold_port'),(1,1,'water_port'),(3,1,'controller_face'),(2,0,'hot_port'),(2,2,'window_frame'),(2,4,'window_frame')):
  row=6-lev;x=cx+i*dx;y=top+10*dy-i*dy+row*H;m=f'{dx/16},{-dy/16},0,{H/16},{x},{y}'
  out.append(put_tile(folder,k,m,board))
 return ''.join(out)
# 单方案页：整机置于上半区，贴图、平铺与接口对照置于下方。
for key,(folder,title,pal,desc) in styles.items():
 board=['<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="1000" viewBox="0 0 1200 1000" preserveAspectRatio="xMidYMin meet" shape-rendering="crispEdges"><rect width="1200" height="1000" fill="#f2f0e9"/>',defs(folder,'.'),txt(36,48,key+' · '+title,30),txt(930,48,'SVG设计稿，未接入游戏',17,'#9c4e36'),txt(36,76,'5×5×7示意 · 隔层Y=3 · 顶面内置四段阶梯安全阀',14,'#59666a',500),cube(600,170,1.15,folder,key,True),txt(36,650,'单格 16×16',17),txt(360,650,'3×2 平铺',17),txt(770,650,'顶面 / 窗 / 水汽接口',17)]
 for n,k in enumerate(kinds[:8]):
  x=36+(n%4)*76;y=672+(n//4)*90;board.append(f'<rect x="{x}" y="{y}" width="56" height="56" fill="#f2f0e9"/>');board.append(thumb(folder,k,x,y,56));board.append(f'<rect x="{x}" y="{y}" width="56" height="56" fill="none" stroke="#30383b" stroke-width="2"/>');board.append(txt(x,y+70,k.replace('_',' '),9,'#59666a',500))
 for row in range(2):
  for col in range(3):
   x=370+col*40;y=674+row*40;board.append(thumb(folder,'casing_side',x,y,40))
 board.append(f'<rect x="370" y="674" width="120" height="80" fill="none" stroke="#252c30" stroke-width="2"/>')
 board.append(txt(770,683,'顶面',11));board.append(f'<rect x="770" y="694" width="54" height="54" fill="#f2f0e9"/>');board.append(thumb(folder,'casing_top',770,694,54));board.append(f'<rect x="770" y="694" width="54" height="54" fill="none" stroke="#30383b" stroke-width="2"/>')
 for n,k in enumerate(('window_frame','water_port','steam_port','hot_port','cold_port')):
  x=840+n*58;board.append(f'<rect x="{x}" y="694" width="50" height="50" fill="#f2f0e9"/>');board.append(thumb(folder,k,x,694,50));board.append(f'<rect x="{x}" y="694" width="50" height="50" fill="none" stroke="#30383b" stroke-width="2"/>')
 board += [txt(770,790,'项目现行反应堆外壳材质参考',14),'<image href="../../../../src/main/resources/assets/create_nuclear_industry/textures/block/reactor_casing_side.png" x="770" y="804" width="96" height="96" image-rendering="pixelated"/>',txt(880,834,'现行反应堆外壳',12,'#59666a',500),'</svg>']
 (root/folder/'board.svg').write_text('\n'.join(board),encoding='utf-8')
# 总览把上方主图留给三种整机；所有细节只占下方区域。
over=['<svg xmlns="http://www.w3.org/2000/svg" width="1800" height="1200" viewBox="0 0 1800 1200" preserveAspectRatio="xMidYMin meet" shape-rendering="crispEdges"><rect width="1800" height="1200" fill="#f1efe8"/>']+[defs(x[0]) for x in styles.values()]
over += [txt(48,58,'高压锅炉外观提案 · 三方向同尺度对照',32),txt(1390,58,'SVG设计稿，未接入游戏',17,'#9c4e36')]
for ix,(key,(folder,title,pal,desc)) in enumerate(styles.items()):
 x=ix*600;over.append(f'<rect x="{x+14}" y="80" width="572" height="1090" rx="8" fill="#faf9f5" stroke="#d0cec5" stroke-width="2"/>')
 over += [txt(x+34,121,key+' · '+title,23),txt(x+34,150,{'A':'深暖钢 / 三道横箍 / 窄耐火嵌片','B':'错缝耐火砖主面 / 钢收边','C':'突出钢骨架 / 阶梯斜筋 / 凹面板'}[key],13,'#59666a',500),cube(x+300,218,1.15,folder,key),txt(x+34,675,'5×5×7 示意 · 隔层Y=3',13,'#59666a',500),txt(x+34,724,'外墙单格',14),f'<rect x="{x+34}" y="738" width="58" height="58" fill="#f1efe8"/>',thumb(folder,'casing_side',x+34,738,58),f'<rect x="{x+34}" y="738" width="58" height="58" fill="none" stroke="#30383b" stroke-width="2"/>',txt(x+122,724,'3×2 平铺',14)]
 for row in range(2):
  for col in range(3):over.append(thumb(folder,'casing_side',x+122+col*24,738+row*24,24))
 over.append(f'<rect x="{x+122}" y="738" width="72" height="48" fill="none" stroke="#30383b" stroke-width="1"/>')
 over += [txt(x+286,724,'顶面',14),f'<rect x="{x+286}" y="738" width="52" height="52" fill="#f1efe8"/>',thumb(folder,'casing_top',x+286,738,52),f'<rect x="{x+286}" y="738" width="52" height="52" fill="none" stroke="#30383b" stroke-width="2"/>',txt(x+350,724,'窗/水/汽',14)]
 for n,k in enumerate(('window_frame','water_port','steam_port')):
  xx=x+350+n*54;over.append(f'<rect x="{xx}" y="738" width="48" height="48" fill="#f1efe8"/>');over.append(thumb(folder,k,xx,738,48));over.append(f'<rect x="{xx}" y="738" width="48" height="48" fill="none" stroke="#30383b" stroke-width="2"/>')
 over += [txt(x+34,866,'项目现行反应堆材质对照',14),f'<image href="../../../src/main/resources/assets/create_nuclear_industry/textures/block/reactor_casing_side.png" x="{x+34}" y="880" width="80" height="80" image-rendering="pixelated"/>',txt(x+132,910,{'A':'横向压力箍建立主体节奏。','B':'耐火砖错缝占据主表面。','C':'框架决定轮廓，板件内凹。'}[key],12,'#4f5d60',500),txt(x+34,1038,'材质示意，不代表锅炉结构或客户端效果。',12,'#59666a',500)]
over.append('</svg>');(root/'overview.svg').write_text('\n'.join(over),encoding='utf-8')
page='''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>锅炉外观设计稿</title><style>body{margin:0;background:#e6e5df;color:#253036;font:15px Segoe UI,Arial,sans-serif}header{padding:14px 24px;background:#263238;color:#f7f4eb}header h1{margin:0 0 4px;font-size:22px}header p{margin:0;color:#d4dedc}.tabs{display:flex;gap:8px;padding:10px 22px}.tabs button{border:1px solid #7d8889;background:#faf9f5;padding:9px 15px;border-radius:4px;font-weight:700;cursor:pointer}.tabs button.active{background:#53666c;color:white}main{margin:0 18px 18px;background:#faf9f5;border:1px solid #c4c5bf;line-height:0}object{display:block;width:100%;height:calc(100vh - 125px);min-height:760px;object-fit:contain;object-position:top center}</style><header><h1>高压锅炉外观提案</h1><p>三套SVG设计稿 · 设计稿，未接入游戏</p></header><nav class="tabs"><button class="active" data-src="overview.svg">总览</button><button data-src="01-horizontal-hoop/board.svg">A 横箍压力舱</button><button data-src="02-refractory-jacket/board.svg">B 耐火砖护套</button><button data-src="03-steel-frame/board.svg">C 钢骨架舱</button></nav><main><object id="board" data="overview.svg" type="image/svg+xml" aria-label="锅炉外观设计稿"></object></main><script>document.querySelectorAll('button').forEach(b=>b.addEventListener('click',()=>{document.querySelectorAll('button').forEach(x=>x.classList.remove('active'));b.classList.add('active');document.getElementById('board').data=b.dataset.src;}));</script></html>'''
(root/'index.html').write_text(page,encoding='utf-8')
(root/'README.md').write_text('# ART-BOILER-05A 样稿\n\n`overview.svg`为三方向总览，`index.html`可直接在浏览器切换单稿。三个方向各含9张16×16 SVG及一张组合细节页。图片仅作外观设计稿，不是游戏资源；只用整数矩形、有限RGB色板和crispEdges。本批PNG_created=0。\n',encoding='utf-8')
(root/'manifest.json').write_text(json.dumps({'task':'ART-BOILER-05A','png_created':0,'formal_resources_replaced':False,'tile_size':'16x16','styles':{k:{'directory':v[0],'tile_count':len(kinds),'materials':v[3]} for k,v in styles.items()}},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('overview', (root/'overview.svg').resolve())
print('index', (root/'index.html').resolve())
print('tiles',sum(1 for _ in root.glob('*/*.svg')))
















