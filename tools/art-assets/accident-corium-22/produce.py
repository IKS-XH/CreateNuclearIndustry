# SPDX-License-Identifier: MIT
# Copyright (c) 2026 IKSXH and contributors
"""22-R1只重做四张材质，保首版证据和全部接线；SVG真源先落盘再导出PNG。"""
from pathlib import Path
import hashlib,json,struct,zlib,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parent
PROJECT=ROOT.parents[2]
REPORT=PROJECT/'build/reports/art/ART-ACCIDENT-CORIUM-22/r1'
R='src/main/resources/assets/create_nuclear_industry/'
PALETTE={'seam':'#30383b','oxide':'#414a4d','lead':'#5b6568','lead2':'#657073','edge':'#899391','silver':'#a5afa6','silverLight':'#c7cec0','silverShade':'#7b8985'}
def sha(b):return hashlib.sha256(b).hexdigest()
def read(p):return json.loads(p.read_text(encoding='utf-8'))
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
SOLID_PARTS=[
    (1,'宽断板',0,1,['#########','##########','######.###','#####..##.','######....','.####.....']),
    (2,'斜短条',11,4,['...##','..###','.###','##']),
    (3,'凹缺折角',1,9,['####','##..','##..','#####','.####']),
    (4,'纵向裂片',8,8,['###','####','.###','.##.','.##.','###.','##..']),
    (5,'小碎块',14,10,['##','##','.#']),
    (6,'边缘撕裂片',12,14,['####','#####','####','..##'])]
FLOW_PARTS=[
    (1,'横宽断板',-3,3,['##########..','############','##########..','######..##..','#####...###.','.####....##.','..###......']),
    (2,'斜向缺口板',12,5,['...####','..#####','.######','######.','#####..','.###...']),
    (3,'纵向双叉片',24,12,['#####','######','###.##','##..##','##..##','######','.####.']),
    (4,'细长纵片',8,14,['###.','####','.###','.###','..##','..##','.###','###.','##..']),
    (5,'宽折角板',16,24,['..#######..','.#########.','##########.','#####..####','####....###','.###....##.','..##.......']),
    (6,'横向钩形片',-1,24,['#####..','###....','###....','#######','.######','..#####']),
    (7,'小银灰块',23,20,['###','.##']),
    (8,'短横条',2,16,['####','#####','.####']),
    (9,'上缘斜短片',12,30,['####','#####','..###','...##'])]
def plate(mask,ident,x,y,rows,size=16):
    for dy,row in enumerate(rows):
        for dx,c in enumerate(row):
            if c=='#':
                index=((y+dy)%size)*size+(x+dx)%size
                if mask[index]:raise ValueError('手工碎壳轮廓重叠')
                mask[index]=ident
def solid_mask():
    m=[0]*256
    for ident,name,x,y,rows in SOLID_PARTS:plate(m,ident,x,y,rows)
    return m
def flow_mask():
    m=[0]*1024
    for ident,name,x,y,rows in FLOW_PARTS:plate(m,ident,x,y,rows,32)
    return m
def color(key):return PALETTE.get(key,key)
def frame(mask,kind,phase=0,size=16):
    """大结壳不移动；冷热共用布局和金属色，只液相或裂缝变色，避免把颜色当新温度状态。"""
    pixels=[]
    for y in range(size):
        for x in range(size):
            ident=mask[y*size+x]
            near=lambda dx,dy:mask[((y+dy)%size)*size+(x+dx)%size]
            if ident:
                if near(0,-1)!=ident:c='edge'
                elif near(-1,0)!=ident:c='silverShade'
                elif near(0,1)!=ident or near(1,0)!=ident:c='oxide'
                else:c='lead' if ident%2 else 'lead2'
            elif kind=='cold':c='seam'
            else:
                neighbors=sum(bool(near(dx,dy)) for dx,dy in [(0,-1),(0,1),(-1,0),(1,0)])
                if kind=='hot':
                    c=['#a74e2c','#b35730','#bd6136','#c66c3c'][phase] if neighbors else ['#e89044','#f09a4b','#f7a853','#ffb45c'][phase]
                else:c=['#a64424','#ac4925','#b04d27','#aa4725'][phase] if neighbors else ['#e9802d','#ef8b32','#f29338','#ed8830'][phase]
            pixels.append(color(c))
    # 大块灰银混合碎片焊在金属结壳里；不同于熔岩随机亮点，不增加独立几何。
    fragments=[[(2,2),(3,2),(4,2),(2,3),(3,3)],[(9,9),(9,10),(10,10)],[(13,15),(14,15),(14,0)]] if size==16 else [[(0,4),(1,4),(2,4),(0,5),(1,5)],[(15,7),(16,7),(16,8)],[(10,17),(10,18),(11,18)],[(19,25),(20,25),(21,25),(19,26),(20,26)]]
    for coords in fragments:
        for i,(x,y) in enumerate(coords):
            if mask[y*size+x]:pixels[y*size+x]=color('silverLight' if i<2 else 'silver')
    if kind=='hot':
        for x,y in [(10,2),(7,8)]:
            if not mask[y*16+x]:pixels[y*16+x]=['#ffd498','#ffddac','#ffe6bf','#fff0d1'][phase]
    if kind=='still':
        dx,dy=[(0,0),(1,0),(1,1),(0,1)][phase]
        for x,y in [(6,9),(6,10),(7,11),(10,1),(11,2)]:
            xx,yy=x+dx,y+dy
            if not mask[yy*16+xx]:pixels[yy*16+xx]='#ffd080' if y in (1,9) else '#ffb957'
        # 只有两像素小碎壳随黏稠液体在1px内往复，大壳始终保持沉重。
        for x,y,c in [(5+dx,8+dy,'edge'),(6+dx,8+dy,'lead')]:
            if not mask[y*16+x]:pixels[y*16+x]=color(c)
    if kind=='flow':
        for x,y in [(10,3),(10,4),(11,5),(10,6),(17,15),(17,16),(18,16),(21,23),(22,23),(23,24)]:
            if not mask[y*32+x]:pixels[y*32+x]='#ffc369' if y%2 else '#fff0ca'
    return pixels
def waste():
    p=[None]*256
    for y,x,w in [(2,6,4),(3,4,7),(4,3,9),(5,2,11),(6,2,12),(7,2,12),(8,2,12),(9,2,12),(10,2,11),(11,3,10),(12,4,8),(13,6,4)]:
        for xx in range(x,x+w):p[y*16+xx]=color('lead')
    def solid(x,y):return 0<=x<16 and 0<=y<16 and p[y*16+x] is not None
    shape=p[:]
    for y in range(16):
        for x in range(16):
            if shape[y*16+x] is None:continue
            if not solid(x,y-1):p[y*16+x]=color('edge')
            elif not solid(x+1,y) or not solid(x,y+1):p[y*16+x]=color('oxide')
            elif not solid(x-1,y):p[y*16+x]=color('silverShade')
    for x,y in [(5,7),(6,7),(7,7),(8,8),(9,8),(10,8),(8,9)]:p[y*16+x]=color('seam')
    for x,y in [(5,4),(6,4),(7,5),(5,5),(6,5),(5,6),(9,9),(10,9),(11,9),(9,10),(10,10)]:p[y*16+x]=color('silverLight' if y in (4,9) else 'silver')
    return p
def svg(w,h,p):
    lines=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}" shape-rendering="crispEdges">','  <!-- SPDX-License-Identifier: MIT; Copyright (c) 2026 IKSXH and contributors -->']
    for y in range(h):
        x=0
        while x<w:
            c=p[y*w+x];end=x+1
            while end<w and p[y*w+end]==c:end+=1
            if c:lines.append(f'  <rect x="{x}" y="{y}" width="{end-x}" height="1" fill="{c}"/>')
            x=end
    return ('\n'.join(lines+['</svg>'])+'\n').encode()
def raster(path):
    root=ET.fromstring(path.read_bytes());w,h=(int(root.attrib[k]) for k in ['width','height']);p=bytearray(w*h*4)
    for r in root:
        if r.tag!='{http://www.w3.org/2000/svg}rect':raise ValueError('SVG源不是整数rect')
        x,y,rw,rh=(int(r.attrib[k]) for k in ['x','y','width','height']);c=bytes.fromhex(r.attrib['fill'][1:])+b'\xff'
        for yy in range(y,y+rh):
            for xx in range(x,x+rw):p[(yy*w+xx)*4:(yy*w+xx+1)*4]=c
    return w,h,bytes(p)
def png(w,h,p):
    def chunk(k,d):return struct.pack('>I',len(d))+k+d+struct.pack('>I',zlib.crc32(k+d)&0xffffffff)
    rows=b''.join(b'\0'+p[y*w*4:(y+1)*w*4] for y in range(h))
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(rows,9))+chunk(b'IEND',b'')
def main():
    if (REPORT/'mapping.json').exists():raise RuntimeError('R1已导出，禁止覆盖同批证据')
    baseline=read(REPORT/'baseline.json');old={r['source']:r for r in baseline['beforeFiles']};mapping=read(ROOT/'mapping.json')
    allowed=set(baseline['r1AllowedProductionFiles']);written=[]
    def install(path,data):
        require_old=old[path]
        if path not in allowed or sha((PROJECT/path).read_bytes())!=require_old['sha256']:raise RuntimeError('R1目标不是首版身份:'+path)
        (PROJECT/path).write_bytes(data);written.append(path)
    mask=solid_mask();cold=frame(mask,'cold');hot=[frame(mask,'hot',i) for i in range(4)];still=[frame(mask,'still',i) for i in range(4)]
    flowmask=flow_mask();flow0=frame(flowmask,'flow',size=32)
    flow=[[flow0[((y-phase)%32)*32+x] for y in range(32) for x in range(32)] for phase in range(32)]
    jobs=[('reactor_corium',16,[cold]),('hot_reactor_corium',16,hot),('molten_corium_still',16,still),('molten_corium_flow',32,flow)]
    for name,size,frames in jobs:
        row=next(r for r in mapping['textures'] if r['id']==name);source=PROJECT/row['source']
        if sha(source.read_bytes())!=old[row['source']]['sha256']:raise RuntimeError('R1源不是首版身份')
        source.write_bytes(svg(size,size*len(frames),sum(frames,[])))
        w,h,pixels=raster(source);install(row['target'],png(w,h,pixels))
        row.update({'width':w,'height':h,'frameWidth':size,'frameHeight':size,'storedFrames':len(frames),'sourceSha256':sha(source.read_bytes()),'targetSha256':sha((PROJECT/row['target']).read_bytes())})
        if name=='molten_corium_flow':
            row['order']=list(range(32));row['frametime']=6
            install(row['target']+'.mcmeta',(json.dumps({'animation':{'width':32,'height':32,'frametime':6,'frames':list(range(32)),'interpolate':False}},indent=2)+'\n').encode())
    if set(written)!=allowed:raise RuntimeError('R1必须恰五项生产写入')
    for row in mapping['files']:
        data=(PROJECT/row['path']).read_bytes();row['bytes']=len(data);row['sha256']=sha(data);row['r1Changed']=row['path'] in allowed
    mapping.update({'revision':'R1','largeCrustMask':mask,'flowCrustMask':flowmask,'solidFragments':[{'id':i,'name':n,'x':x,'y':y,'rows':rows} for i,n,x,y,rows in SOLID_PARTS],
        'flowFragments':[{'id':i,'name':n,'x':x,'y':y,'rows':rows} for i,n,x,y,rows in FLOW_PARTS],
        'flowPeriodPixels':32,'flowLayout':'independent hand-drawn 32x32; four quadrants differ; no 16x16 duplication',
        'stillMotionFeatures':{'smallCrustBase':[[5,8],[6,8]],'offsets':[[0,0],[1,0],[1,1],[0,1]]},'r1EvidenceRoot':str(REPORT)})
    samples=[]
    for name,cw,ch,rx,ry in [('native_side_16x16_3x2',16,16,3,2),('half_width_16x32_3x2',16,32,3,2),('full32_2x2',32,32,2,2)]:
        w,h=cw*rx,ch*ry;pixels=[flow0[(y%ch)*32+x%cw] for y in range(h) for x in range(w)]
        source=REPORT/'sampling'/(name+'.svg');target=source.with_suffix('.png');source.parent.mkdir(parents=True,exist_ok=True)
        source.write_bytes(svg(w,h,pixels));sw,sh,p=raster(source);target.write_bytes(png(sw,sh,p))
        samples.append({'id':name,'svg':str(source.relative_to(PROJECT)).replace('\\','/'),'png':str(target.relative_to(PROJECT)).replace('\\','/'),'sourceTexture':R+'textures/fluid/molten_corium_flow.png','frame':0,'crop':[0,0,cw,ch],'repeat':[rx,ry],'width':w,'height':h,'meaning':'actual frame0 nearest-neighbor crop/repeat; not Minecraft capture'})
    mapping['samplingEvidence']=samples
    dump(ROOT/'mapping.json',mapping);dump(REPORT/'mapping.json',mapping)
    print(json.dumps({'task':'ART-ACCIDENT-CORIUM-22-R1','changedPng':4,'changedSvg':4,'changedMcmeta':1,'solidDifferentFragments':len(SOLID_PARTS),'flowFrameSize':[32,32],'flowStoredPixels':[32,1024],'flowFrames':32,'flowCycleTicks':192,'samplingImages':len(samples)}))
if __name__=='__main__':main()
