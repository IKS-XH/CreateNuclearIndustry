# SPDX-License-Identifier: MIT
# Copyright (c) 2026 IKSXH and contributors
"""22-R1只核四材质/32周期/半幅样图及13项保护，不重扫旧非批或执行构建。"""
from pathlib import Path
import ast,hashlib,json,math,re,struct,subprocess,zlib,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parent
PROJECT=ROOT.parents[2]
REPORT=PROJECT/'build/reports/art/ART-ACCIDENT-CORIUM-22/r1'
ASSETS=REPORT
R='src/main/resources/assets/create_nuclear_industry/'
def sha(b):return hashlib.sha256(b).hexdigest()
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def require(ok,msg):
    if not ok:raise ValueError(msg)
def dump(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def ident(p):
    b=p.read_bytes();return {'path':p.relative_to(PROJECT).as_posix(),'bytes':len(b),'sha256':sha(b)}
def svg(p):
    root=ET.fromstring(p.read_bytes());w,h=(int(root.attrib[k]) for k in ['width','height'])
    require(root.tag=='{http://www.w3.org/2000/svg}svg' and root.attrib=={'width':str(w),'height':str(h),'viewBox':f'0 0 {w} {h}','shape-rendering':'crispEdges'},'SVG规格')
    require('SPDX-License-Identifier: MIT' in p.read_text(encoding='utf-8'),'源SVG许可')
    pixels=bytearray(w*h*4);seen=set()
    for r in root:
        require(r.tag=='{http://www.w3.org/2000/svg}rect' and set(r.attrib)=={'x','y','width','height','fill'},'SVG仅rect')
        require(all(re.fullmatch(r'\d+',r.attrib[k]) for k in ['x','y','width','height']),'整数rect')
        x,y,rw,rh=(int(r.attrib[k]) for k in ['x','y','width','height'])
        require(0<=x<x+rw<=w and 0<=y<y+rh<=h and re.fullmatch(r'#[0-9a-f]{6}',r.attrib['fill']),'SVG边界/色值')
        c=bytes.fromhex(r.attrib['fill'][1:])+b'\xff'
        for yy in range(y,y+rh):
            for xx in range(x,x+rw):
                index=yy*w+xx;require(index not in seen,'SVG重叠rect');seen.add(index);pixels[index*4:index*4+4]=c
    return w,h,bytes(pixels)
def png(p):
    b=p.read_bytes();require(b[:8]==b'\x89PNG\r\n\x1a\n','PNG签名');i=8;compressed=b'';header=None
    while i<len(b):
        n=struct.unpack('>I',b[i:i+4])[0];k=b[i+4:i+8];d=b[i+8:i+8+n]
        require(zlib.crc32(k+d)&0xffffffff==struct.unpack('>I',b[i+8+n:i+12+n])[0],'PNG CRC')
        if k==b'IHDR':header=struct.unpack('>IIBBBBB',d)
        if k==b'IDAT':compressed+=d
        i+=12+n
    require(header[2:]==(8,6,0,0,0),'RGBA8/非交错')
    w,h=header[:2];raw=zlib.decompress(compressed);stride=w*4+1
    require(len(raw)==stride*h and all(raw[y*stride]==0 for y in range(h)),'PNG像素行')
    return w,h,b''.join(raw[y*stride+1:(y+1)*stride] for y in range(h))
def delta(a,b):return sum(abs(x-y) for x,y in zip(a,b))/len(a)
def main():
    baseline=read(REPORT/'baseline.json');mapping=read(ROOT/'mapping.json');build=read(REPORT/'processResources-exit.json')
    require(build['exitCode']==0 and build['invocationCount']==1,'唯一processResources未成功')
    require(mapping['revision']=='R1' and mapping['flowPeriodPixels']==32,'R1新构图协议')
    require(len(baseline['beforeFiles'])==48,'首版备份项目数')
    for row in baseline['beforeFiles']:
        require(sha((PROJECT/row['snapshot']).read_bytes())==row['sha256'],'首版快照被修改:'+row['snapshot'])
    protected=[]
    for row in baseline['protectedProductionAndSvg']:
        require(sha((PROJECT/row['path']).read_bytes())==row['sha256'],'R1未授权文件变化:'+row['path']);protected.append(row['path'])
    require(len(protected)==13,'十二生产项及废料SVG保护')
    names=['reactor_corium','hot_reactor_corium','molten_corium_still','molten_corium_flow'];decoded={};pixels_compared=0;source_records=[];checks=[]
    for name in names:
        row=next(r for r in mapping['textures'] if r['id']==name);source=PROJECT/row['source'];target=PROJECT/row['target']
        w,h,p=svg(source);nw,nh,n=png(target)
        require((w,h)==(nw,nh)==(row['width'],row['height']) and p==n,'四SVG/PNG像素不符')
        require(sha(source.read_bytes())==row['sourceSha256'] and sha(target.read_bytes())==row['targetSha256'],'当前source/PNG身份')
        require(all(a==255 for a in n[3::4]),'固体/流体仍须不透明')
        size=row['frameWidth'];count=row['storedFrames'];require(h==size*count and row['frameHeight']==size==w,'纵排帧')
        frames=[n[i*size*size*4:(i+1)*size*size*4] for i in range(count)];decoded[name]=frames
        meta=read(Path(str(target)+'.mcmeta'))
        require(meta=={'animation':{'width':size,'height':size,'frametime':row['frametime'],'frames':row['order'],'interpolate':row['interpolate']}},'帧元数据')
        diffs=[delta(frames[a],frames[b]) for a,b in zip(row['order'],row['order'][1:]+row['order'][:1])]
        if count>1:require(diffs[-1]<=max(diffs[:-1])+1e-9,'首尾突跳')
        checks.append({'id':name,'width':w,'height':h,'frames':count,'comparedPixels':w*h,'differentRGBA':0,'cycleTicks':len(row['order'])*row['frametime'],'adjacentMeanByteDeltasIncludingLoop':diffs})
        pixels_compared+=w*h;source_records.append(ident(source))
    require(pixels_compared==35072,'R1四材质像素总数')
    mask=mapping['largeCrustMask'];require(len(mask)==256,'固壳底版尺寸');ids=sorted(set(mask)-{0});areas=[mask.count(i) for i in ids]
    require(len(ids)==6 and len(set(areas))==6,'六块轮廓面积必须不同，不能复制同款切角块')
    for fragment in mapping['solidFragments']:
        count=sum(r.count('#') for r in fragment['rows']);require(count==mask.count(fragment['id']),'手工轮廓与底版不符')
    cold=decoded['reactor_corium'][0];seam=bytes.fromhex(mapping['palette']['seam'][1:])+b'\xff'
    require(all((cold[i*4:i*4+4]!=seam)==bool(m) for i,m in enumerate(mask)),'冷态轮廓未真实落图')
    for f in decoded['hot_reactor_corium']+decoded['molten_corium_still']:
        require(all(f[i*4:i*4+4]==cold[i*4:i*4+4] for i,m in enumerate(mask) if m),'冷热/still大固壳不同布局或颜色')
    motion=mapping['stillMotionFeatures'];require(motion['offsets']==[[0,0],[1,0],[1,1],[0,1]],'1px动作范围')
    for phase,(dx,dy) in enumerate(motion['offsets']):
        x,y=motion['smallCrustBase'][0];x+=dx;y+=dy;i=(y*16+x)*4
        require(not mask[y*16+x] and decoded['molten_corium_still'][phase][i:i+3]==bytes.fromhex(mapping['palette']['edge'][1:]),'小壳1px往复没有落图')
    flow=decoded['molten_corium_flow'];require(len(flow)==32 and mapping['textures'][3]['height']==1024,'flow应为32相/32x1024')
    for phase,f in enumerate(flow):
        for y in range(32):
            for x in range(32):
                i=(y*32+x)*4;origin=(((y-phase)%32)*32+x)*4
                require(f[i:i+4]==flow[0][origin:origin+4],'真实32周期逐帧下移1px')
                last=(((y-1)%32)*32+x)*4
                require(flow[0][i:i+4]==flow[-1][last:last+4],'末帧到首帧不续移1px')
    def crop(frame,x,y,w,h):return b''.join(frame[(yy*32+x)*4:(yy*32+x+w)*4] for yy in range(y,y+h))
    quadrants=[crop(flow[0],x,y,16,16) for y in [0,16] for x in [0,16]]
    require(len({sha(q) for q in quadrants})==4,'四象限必须有真实不同构图，不保留旧2x2镜像断言')
    require(any(crop(flow[0],0,y,16,1)!=crop(flow[0],16,y,16,1) for y in range(32)),'不得水平16周期复制')
    require(crop(flow[0],0,0,32,16)!=crop(flow[0],0,16,32,16),'不得垂直16周期复制')
    samples=[];sample_pixels=0
    for sample in mapping['samplingEvidence']:
        sw,sh,p=svg(PROJECT/sample['svg']);pw,ph,n=png(PROJECT/sample['png']);require((sw,sh)==(pw,ph)==(sample['width'],sample['height']) and p==n,'取样图SVG/PNG不同')
        _,_,cw,ch=sample['crop'];require(sample['frame']==0,'当前读感样图只代表帧0')
        for y in range(sh):
            for x in range(sw):
                i=(y*sw+x)*4;j=((y%ch)*32+x%cw)*4
                require(n[i:i+4]==flow[0][j:j+4],'半幅图不是实际生产flow帧0裁切/平铺')
        samples.append({**sample,'svgIdentity':ident(PROJECT/sample['svg']),'pngIdentity':ident(PROJECT/sample['png']),'differentRGBA':0});sample_pixels+=sw*sh
    require(sample_pixels==8704,'三取样图像素总数')
    half_seams=[]
    for frame in flow:
        differences=[abs(frame[(y*32+15)*4+c]-frame[(y*32)*4+c]) for y in range(32) for c in range(3)]
        half_seams.append(sum(differences)/len(differences))
    allowed=set(baseline['r1AllowedProductionFiles']);changed=[];processed=[]
    old={r['source']:r for r in baseline['beforeFiles']}
    for path in allowed:
        data=(PROJECT/path).read_bytes();require(sha(data)!=old[path]['sha256'],'R1声明文件没有增量')
        changed.append({'path':path,'bytes':len(data),'sha256':sha(data),'firstVersionSha256':old[path]['sha256']})
        out=PROJECT/'build/resources/main'/path.removeprefix('src/main/resources/')
        require(out.read_bytes()==data,'R1处理输出不同源');processed.append(ident(out))
    for row in mapping['files']:require(sha((PROJECT/row['path']).read_bytes())==row['sha256'],'17项当前清单不符')
    git=lambda *args:subprocess.check_output(['git','-C',str(PROJECT),*args]).decode('utf-8')
    require(git('rev-parse','HEAD').strip()==baseline['sourceCommit'],'HEAD改变')
    require(set(git('diff','--name-only','HEAD').splitlines())==set(baseline['trackedDiffBefore']),'tracked差异范围扩大')
    require(set(git('ls-files','--others','--exclude-standard','--','src').splitlines())<={r['path'] for r in mapping['files']},'src写集外新增')
    first=read(REPORT/'before/manifest.json')
    for row in first['compiledClientClasses']:require(sha((PROJECT/row['path']).read_bytes())==row['sha256'],'原compileJava输出被改动')
    for p in ROOT.glob('*.py'):ast.parse(p.read_text(encoding='utf-8'),filename=str(p))
    result={'task':'ART-ACCIDENT-CORIUM-22-R1','result':'PASS_R1_RESOURCES_ONLY','sourceCommit':baseline['sourceCommit'],'svgPngComparedPixels':pixels_compared,'differentRGBA':0,'textures':checks,
        'solidFragmentAreas':dict(zip(ids,areas)),'handDrawnSolidFragments':6,'flowIndependent32Layout':True,'flowQuadrantSha256':[sha(q) for q in quadrants],'flowCycleTicks':192,'flowLastToFirstOnePixelContinuation':True,
        'sampleComparedPixels':sample_pixels,'samplingEvidence':samples,'halfWidthCutMeanRgbDifferenceByPhase':half_seams,'halfWidthReadability':'actual crop/repeat images provided; visual assessment belongs to root, no forced equality threshold',
        'changedProductionFiles':changed,'protectedFilesUnchanged':protected,'protectedCount':13,'beforeSnapshotCount':48,'beforeSnapshotsUnchanged':True,'processResourcesFilesCompared':5,'build':build,
        'compileJavaReused':{'source':'r1/before/build/assets/compile-exit.json','exitCode':0,'seconds':13,'notRerun':True,'twoClassOutputsUnchanged':True},
        'scopeLimits':['没有重扫6888非批文件或世界','未运行compileJava/test/jar/build/clean','废料PNG/SVG、Java/atlas/模型/其它meta字节保持','HOT无新增光源，流体原light12/getFlow ZERO保持','半幅样图不是游戏截图，空间平铺读感由4210观察']}
    dump(REPORT/'verification.json',result)
    svg_sources=[ident(PROJECT/t['source']) for t in mapping['textures']]
    manifest={'schemaVersion':1,'task':'ART-ACCIDENT-CORIUM-22','revision':'R1','branch':'codex/accident-thermal-04b','sourceCommit':baseline['sourceCommit'],'productionFiles':mapping['files'],'svgSources':svg_sources,
        'authorToolsAndLicense':[ident(p) for p in sorted(ROOT.glob('*.py'))]+[ident(ROOT/'mapping.json'),ident(ROOT/'LICENSE')],
        'processedResourceOutputs':processed,'compiledClientClasses':first['compiledClientClasses'],'compileJavaEvidenceReused':'r1/before/build/assets/compile-exit.json',
        'r1ProtectedFilesUnchanged':13,'r1EvidenceRoot':str(REPORT),'r1BeforeRoot':str(REPORT/'before'),'r1SamplingEvidence':samples,
        'evidence':[ident(REPORT/'baseline.json'),ident(REPORT/'produce.log'),ident(REPORT/'produce-exit.json'),ident(REPORT/'processResources.log'),ident(REPORT/'processResources-exit.json'),ident(REPORT/'verification.json')],
        'scope':'four SVG/four PNG and flow mcmeta only; independent 32x32/32-frame flow; five newly processed outputs; first-version compile reused',
        'shaExclusions':['this root manifest and r1/manifest (self references)','verification.log and exit written after process','narrative R1 report written after verification','independent preview/root documentation']}
    dump(REPORT/'manifest.json',manifest);dump(REPORT.parent/'manifest.json',manifest)
    print(json.dumps({k:result[k] for k in ['result','svgPngComparedPixels','differentRGBA','handDrawnSolidFragments','flowIndependent32Layout','flowCycleTicks','sampleComparedPixels','protectedCount','beforeSnapshotCount','processResourcesFilesCompared']},ensure_ascii=False))
if __name__=='__main__':main()
