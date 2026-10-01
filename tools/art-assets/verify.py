"""离线核对EXT-ART-08素材：旧图保留、SVG像素、复现性和失败不写出。

只在显式执行本脚本时做临时坏输入实验，finally 原样恢复源稿/清单。
不调用游戏、Gradle 或 Git 写操作；结果保存在本任务证据目录。
"""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET
from collections import Counter

sys.dont_write_bytecode=True
from PIL import Image, ImageDraw, __version__ as pillow_version
import pipeline
from export import render_svg, read_palette, ROOT

REPO=ROOT.parents[1]
EVIDENCE=REPO/'build/reports/extension/EXT-ART-08'
EVIDENCE.mkdir(parents=True,exist_ok=True)
PREEXISTING_SNAPSHOT=ROOT/'baselines/EXT-ART-08-existing-game-png-sha256.json'
ENV=dict(os.environ,PYTHONIOENCODING='utf-8',PYTHONDONTWRITEBYTECODE='1')
log=[]
# 用户只撤回两种冷却剂的八条路径；独立列举，避免随管线白名单错误一起放宽。
RETAINED_COOLANTS=frozenset(f'{directory}/{name}_{state}.png'
    for directory in ('block','fluid')
    for name in ('compound_coolant','hot_compound_coolant')
    for state in ('still','flow'))


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(*args,good=True):
    """保留真实子进程命令和退出码；失败检查不得只调用内部解析函数。"""
    command=[sys.executable,str(ROOT/'export.py'),*args]
    result=subprocess.run(command,cwd=REPO,env=ENV,text=True,encoding='utf-8',capture_output=True)
    log.append({'command':command,'exit':result.returncode,'stdout':result.stdout,'stderr':result.stderr})
    assert (result.returncode==0)==good,result.stderr


def state():
    """记录候选、全部预览、游戏路径的字节；失败实验不允许改变其中任何一项。"""
    paths=list((ROOT/'generated').rglob('*.png'))+list((ROOT/'previews').glob('*.png'))+[ROOT/'preview.html',ROOT/'preview.png']+list(pipeline.GAME_ROOT.rglob('*.png'))
    return {p.relative_to(REPO).as_posix():digest(p) for p in sorted(paths)}


entries,images,before,retained=pipeline.prepare();palette=read_palette()
baseline=json.loads((ROOT/'baseline.json').read_text(encoding='utf-8'))
assert {r['game'] for r in baseline['records']}==pipeline.GAME_FILES
assert len(baseline['records'])==51
assert len(entries)==83
assert Counter(tuple(e['size']) for e in entries if e['game'])=={(16,16):77,(16,64):5}
actual_before={p.relative_to(pipeline.GAME_ROOT).as_posix() for p in pipeline.GAME_ROOT.rglob('*.png')}
snapshot=json.loads(PREEXISTING_SNAPSHOT.read_text(encoding='utf-8'))
snapshot={record['path']:record['sha256'] for record in snapshot['records']}
plate_stage=pipeline.GAME_FILES|frozenset({'item/lead_plate.png','item/tin_plate.png'})
nugget_stage=plate_stage|frozenset({'item/lead_nugget.png','item/tin_nugget.png'})
previous_material_stage=nugget_stage
steel_stage=previous_material_stage|frozenset({'item/iron_dust.png','item/coal_dust.png','item/charcoal_dust.png','item/steel_dust.png','item/steel_ingot.png'})
sensor_stage=steel_stage|frozenset({'item/tin_wire.png','item/industrial_sensor.png','item/radiation_sensor.png','item/incomplete_industrial_sensor.png','item/incomplete_radiation_sensor.png'})
preserved_stage=pipeline.GAME_FILES|pipeline.PREVIOUS_NEW_GAME_FILES
assert actual_before in (pipeline.GAME_FILES,plate_stage,nugget_stage,steel_stage,sensor_stage,preserved_stage,pipeline.ALLOWED_GAME_FILES),'开工时游戏PNG必须是已知历史/素材阶段的完整白名单'
pre_existing_hashes={name:digest(pipeline.GAME_ROOT/name) for name in actual_before}
results={'python':sys.version,'pillow':pillow_version,'baseline':baseline['head'],'preexisting_snapshot':str(PREEXISTING_SNAPSHOT.relative_to(REPO).as_posix()),'historical_game_count':51,'pre_existing_game_count':len(snapshot),'game_count_at_verifier_start':len(pre_existing_hashes),'new_game_count':31,'current_batch_game_count':13,'final_game_count':82,'manifest_entry_count':83,'source_count':len({e['source'] for e in entries}),'records':[]}
for e in entries:
    name=e['game'] or 'item/lapis_dust.png'
    if name in RETAINED_COOLANTS:
        original=subprocess.check_output(['git','show',f'eddd097^:src/main/resources/assets/create_nuclear_industry/textures/{name}'],cwd=REPO)
        assert (ROOT/'generated'/name).read_bytes()==original==(ROOT/'baseline'/name).read_bytes(),f'冷却剂必须逐字节保留原图: {name}'
        with Image.open(ROOT/'generated'/name) as output:
            pipeline.validate_alpha(e,output)
            assert output.tobytes()==images[name].tobytes()
        results['records'].append({'game':name,'size':e['size'],'retained_original_sha256':hashlib.sha256(original).hexdigest()})
        continue
    source=ROOT/'sources'/e['source']
    # 独立使用 Pillow 矩形 API，核对导出器手动覆盖像素的坐标和边界。
    reference=Image.new('RGBA',tuple(e['size']));draw=ImageDraw.Draw(reference)
    for rect in ET.parse(source).getroot():
        x,y,w,h=[int(rect.get(k)) for k in ('x','y','width','height')]
        draw.rectangle((x,y,x+w-1,y+h-1),fill=rect.get('fill'))
    with Image.open(ROOT/'generated'/name) as output:
        assert output.mode=='RGBA' and list(output.size)==e['size']
        assert output.tobytes()==reference.tobytes()==images[name].tobytes(),name
        pipeline.validate_alpha(e,output)
        colors={p[:3] for p in output.get_flattened_data() if p[3]}
        allowed={tuple(int(c[i:i+2],16) for i in (1,3,5)) for c in palette[e['palette']].values()}
        assert colors<=allowed and len(colors)<=16
    results['records'].append({'game':e['game'],'source':e['source'],'size':e['size'],'colors':len(colors),'generated_sha256':digest(ROOT/'generated'/name),'use':e['use']})
    if e['game'] in pipeline.GAME_FILES:
        record=next(r for r in baseline['records'] if r['game']==name)
        assert digest(ROOT/'baseline'/name)==record['sha256'],'旧图证据改变'
    elif e['game'] in pipeline.NEW_GAME_FILES:
        results['records'][-1]['historical_baseline']='none; explicitly new candidate'

# 已批准四样稿使用工具基线的原始字节作独立回归依据，不以新导出互相比较。
for name in ('item/lapis_dust','item/lead_ingot','item/steel_plate','block/lead_ore'):
    for directory,suffix in (('sources','.svg'),('generated','.png')):
        rel=f'tools/art-assets/{directory}/{name}{suffix}'
        original=subprocess.check_output(['git','show',f'49e6c86:{rel}'],cwd=REPO)
        current=(REPO/rel).read_bytes()
        # Git 工作树可能启用 CRLF；SVG 只归一化行尾，PNG 仍逐字节比较。
        if suffix=='.svg':current=current.replace(b'\r\n',b'\n');original=original.replace(b'\r\n',b'\n')
        assert current==original,f'已批准样稿被改变: {rel}'
results['approved_pilot_unchanged']=True

valid='<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges"><rect x="1" y="1" width="2" height="3" fill="#28353A"/></svg>'
bad={}
for tag in ('path','image','use','g','linearGradient','filter','text'):
    bad[tag]=valid.replace('<rect ',f'<{tag} ').replace('/></svg>',f'/></svg>')
for attr in ('style','transform','opacity','stroke','rx','href'):
    bad[attr]=valid.replace('<rect ',f'<rect {attr}="unsupported" ')
for key,old,new in [('fraction','x="1"','x="1.5"'),('negative','x="1"','x="-1"'),('zero','width="2"','width="0"'),('bounds','width="2"','width="16"'),('missing',' height="3"',''),('named','#28353A','gray'),('palette','#28353A','#000000'),('alpha','#28353A','#28353A80'),('url','#28353A','url(#x)'),('namespace','http://www.w3.org/2000/svg','urn:x'),('viewbox','0 0 16 16','0 0 32 32'),('tail','/></svg>','/>bad</svg>'),('child','/></svg>','><rect/></rect></svg>')]:
    bad[key]=valid.replace(old,new)
bad['DTD']='<!DOCTYPE svg>'+valid
bad['PI']=valid.replace('<rect','<?x y?><rect')
bad['empty']=valid[:valid.index('<rect')]+'</svg>'
for name,text in bad.items():
    try:render_svg(text,{'#28353A'})
    except (ValueError,ET.ParseError):pass
    else:raise AssertionError(f'非法输入未拒绝: {name}')
flow=valid.replace('height="16"','height="64"').replace('0 0 16 16','0 0 16 64').replace('y="1"','y="61"')
assert render_svg(flow,{'#28353A'},(16,64)).getpixel((2,63))==(40,53,58,255)
try:render_svg(flow.replace('y="61"','y="62"'),{'#28353A'},(16,64))
except ValueError:pass
else:raise AssertionError('长画布越界未拒绝')
results['invalid_svg_rejected']=list(bad)+['flow_bounds']

# 默认导出两次必须保持全部游戏字节不变，同时派生PNG和预览可复现。
run()
prior=state()
run();assert state()==prior,'默认导出非确定或改动了游戏'
results['default_repeat_exports']=2

# 临时向后序源稿加入未知元素，确认真实CLI在写第一个输出前就失败。
source=ROOT/'sources/fluid/hot_compound_coolant_still.svg';saved=source.read_bytes()
try:
    source.write_text(saved.decode('utf-8').replace('</svg>','<circle/></svg>'),encoding='utf-8')
    run('--install',good=False);assert state()==prior
finally:source.write_bytes(saved)

manifest=ROOT/'manifest.json';saved=manifest.read_bytes()
for mutation in ('extra_path','wrong_size','wrong_mapping','unauthorized_preserve','missing_preserve'):
    data=json.loads(saved)
    if mutation=='extra_path':data['entries'][0]['game']='block/not_authorized.png'
    elif mutation=='wrong_size':data['entries'][0]['size']=[16,16]
    elif mutation=='wrong_mapping':data['entries'][0]['source']='../outside.svg'
    elif mutation=='unauthorized_preserve':data['entries'][2]['preserve']='baseline-original'
    else:data['entries'][0].pop('preserve',None)
    try:
        manifest.write_text(json.dumps(data),encoding='utf-8')
        run('--install',good=False);assert state()==prior
    finally:manifest.write_bytes(saved)
results['cli_failure_no_writes']=['unsupported_svg','extra_path','wrong_size','wrong_mapping','unauthorized_preserve','missing_preserve']

# EXT-ART-07 轴承视觉合同继续保留：孔洞透明、成品闭合、半成品留出装配缺口。
for name in ('heavy_bearing','incomplete_heavy_bearing'):
    bearing=images[f'item/{name}.png']
    assert bearing.getpixel((8,8))[3]==0,f'{name}: 轴孔中心必须保持透明'
assert images['item/heavy_bearing.png'].getpixel((12,4))[3]==255
assert images['item/incomplete_heavy_bearing.png'].getpixel((12,5))[3]==0
assert read_palette()['heavy_bearing']==read_palette()['incomplete_heavy_bearing']
results['previous_batch_visual_invariants']=['transparent_bearing_bores','completed_bearing_frame','incomplete_open_frame','shared_bearing_palette']

# 新粉末保持独立轮廓/颜色；料浆桶留透明外圈，液体流图保留完整长画布。
powders=[images[f'item/{name}.png'] for name in ('uranium_tailings','low_enriched_uranium_dust','depleted_uranium_dust')]
assert len({image.tobytes() for image in powders})==3
assert all(image.getpixel((0,0))[3]==0 for image in powders)
bucket=images['item/uranium_slurry_bucket.png']
assert bucket.getpixel((0,0))[3]==0 and bucket.getbbox() is not None
assert images['fluid/uranium_slurry_flow.png'].size==(16,64)
assert len({images[name].tobytes() for name in ('block/enrichment_centrifuge_front.png','block/enrichment_centrifuge_back.png','block/enrichment_centrifuge_slurry_port.png','block/enrichment_centrifuge_water_port.png','block/enrichment_centrifuge_top.png','block/enrichment_centrifuge_bottom.png')})==6
results['current_batch_visual_invariants']=['three_distinct_powders','bucket_transparent_outline','slurry_flow_16x64','six_distinct_centrifuge_faces','tailings_brick_texture']

# 显式接入后逐条核对：开工69张图保持原字节，本批13项新增素材与导出候选一致。
historical_before={name:digest(pipeline.GAME_ROOT/name) for name in pipeline.GAME_FILES}
run('--install')
assert {p.relative_to(pipeline.GAME_ROOT).as_posix() for p in pipeline.GAME_ROOT.rglob('*.png')}==pipeline.ALLOWED_GAME_FILES
for e in entries:
    if e['game']:
        assert (pipeline.GAME_ROOT/e['game']).read_bytes()==(ROOT/'generated'/e['game']).read_bytes()
        if e['game'] in pipeline.NEW_GAME_FILES:
            assert not (ROOT/'baseline'/e['game']).exists(),'新增图不得伪造旧基线'
        elif e['game'] in RETAINED_COOLANTS:
            assert (pipeline.GAME_ROOT/e['game']).read_bytes()==(ROOT/'baseline'/e['game']).read_bytes(),'旧冷却剂被再次覆盖'
assert {name:digest(pipeline.GAME_ROOT/name) for name in pipeline.GAME_FILES}==historical_before,'历史51张游戏PNG被改动'
assert all(digest(pipeline.GAME_ROOT/name)==value for name,value in pre_existing_hashes.items()),'两次安装改动了开工前已有的游戏PNG'
assert set(snapshot)==pipeline.ALLOWED_GAME_FILES-pipeline.CURRENT_BATCH_GAME_FILES,'开工哈希快照必须精确覆盖原有69张PNG'
assert {name:digest(pipeline.GAME_ROOT/name) for name in snapshot}==snapshot,'本批69张开工游戏PNG哈希变化'
after=state();run('--install');assert state()==after
results['install_repeat_exports']=2
results['historical_51_game_png_unchanged']=True
results['pre_existing_game_png_unchanged']=True
results['preexisting_game_hash_snapshot_unchanged']=True
results['preexisting_game_hash_snapshot_count']=len(snapshot)
results['svg_or_retained_original_generated_game_equal']=True
results['output_hashes']=after
results['result']='PASS'
(EVIDENCE/'verification.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(EVIDENCE/'commands.json').write_text(json.dumps(log,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:v for k,v in results.items() if k not in ('records','output_hashes','invalid_svg_rejected')},ensure_ascii=True,indent=2))
