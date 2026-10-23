# SPDX-License-Identifier: MIT
# Copyright (c) 2026 IKSXH and contributors
"""22批只读保存事故树基线与17项允许集；不枚举世界，不进行Git写或构建。"""
from pathlib import Path
import hashlib,json,subprocess
ROOT=Path(__file__).resolve().parent
PROJECT=ROOT.parents[2]
REPORT=PROJECT/'build/reports/art/ART-ACCIDENT-CORIUM-22/assets'
BASE='1985d1b3172a5efd1f0c87f3aa39ce3fe2a6feb7'
R='src/main/resources/assets/create_nuclear_industry/'
ALLOWED=[R+'blockstates/reactor_corium.json']+[R+'models/block/'+n+'.json' for n in ['reactor_corium','hot_reactor_corium','molten_corium']]+[R+'models/item/'+n+'.json' for n in ['reactor_corium','accident_fuel_waste']]+[R+'textures/'+folder+'/'+name+'.png'+suffix for folder,names in [('block',['reactor_corium','hot_reactor_corium']),('fluid',['molten_corium_still','molten_corium_flow'])] for name in names for suffix in ['','.mcmeta']]+[R+'textures/item/accident_fuel_waste.png','src/main/resources/assets/minecraft/atlases/blocks.json','src/main/java/com/iksxh/create_nuclear_industry/accident/client/AccidentThermalClientEvents.java']
def sha(b):return hashlib.sha256(b).hexdigest()
def git(*args):return subprocess.check_output(['git','-C',str(PROJECT),*args])
def main():
    if (REPORT/'baseline.json').exists():raise RuntimeError('基线已存在，禁止覆盖')
    if git('rev-parse','HEAD').decode().strip()!=BASE or git('branch','--show-current').decode().strip()!='codex/accident-thermal-04b':raise RuntimeError('候选来源不同')
    if git('diff','--name-only','HEAD').strip():raise RuntimeError('tracked开工不干净')
    records=[];allowed=[]
    for name in git('ls-files','-z').decode('utf-8').split('\0'):
        if not name or name.startswith(('run/saves/','run/save-backups/')):continue
        p=PROJECT/name
        if not p.is_file():raise RuntimeError('tracked来源不是普通文件:'+name)
        data=p.read_bytes();records.append({'path':name,'bytes':len(data),'sha256':sha(data)})
    for name in ALLOWED:
        p=PROJECT/name
        if p.exists():
            data=p.read_bytes();snapshot=REPORT/'originals'/name;snapshot.parent.mkdir(parents=True,exist_ok=True);snapshot.write_bytes(data)
            allowed.append({'path':name,'existed':True,'bytes':len(data),'sha256':sha(data),'snapshot':str(snapshot)})
        else:allowed.append({'path':name,'existed':False,'bytes':None,'sha256':None})
    REPORT.mkdir(parents=True,exist_ok=True)
    evidence={'task':'ART-ACCIDENT-CORIUM-22','branch':'codex/accident-thermal-04b','head':BASE,'allowedProductionFiles':allowed,'allowedCount':len(ALLOWED),'trackedBaseline':records,'trackedCount':len(records),'initialStatus':git('status','--porcelain','--untracked-files=all').decode('utf-8'),'worldsEnumerated':False,'rootJarAndLogs':'tracked identities preserved; no copy or mutation'}
    (REPORT/'baseline.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    license_source=Path('E:/MyMC/NewMod/Create_NuclearIndustry/LICENSE')
    if (ROOT/'LICENSE').exists():raise RuntimeError('本批许可文件已存在')
    (ROOT/'LICENSE').write_bytes(license_source.read_bytes())
    print(json.dumps({'allowedProductionFiles':len(ALLOWED),'trackedBaselineFiles':len(records),'worldsEnumerated':False,'rootLicenseChanged':False}))
if __name__=='__main__':main()
