"""只读冻结R1前的本批来源、映射、候选与安装身份；不覆盖原17份旧源备份。"""
import hashlib
import json
import shutil
from pathlib import Path

ROOT=Path(__file__).resolve().parent
REPO=ROOT.parents[2]
REPORT=REPO/"build/reports/art/ART-TURBINE-10B/assets/r1"
SNAPSHOT=REPORT/"pre-r1"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    if SNAPSHOT.exists():
        raise RuntimeError("pre-r1快照已存在，禁止覆盖冻结历史")
    resources=json.loads((ROOT/"resource-map.json").read_text(encoding="utf-8"))["resources"]
    assert len(resources)==26
    sources=[ROOT/name for name in ("produce.py","check_assets.py","install_frozen.py","preview.js","index.html","README.md","resource-map.json")]
    sources+=sorted((ROOT/"sources/textures").glob("*.svg"))
    sources+=sorted((ROOT/"comparison").glob("*"))
    references=[REPO/"tools/art-assets/turbine-rotor-bearing-concepts-10"/name for name in ("preview.js","sources/textures/B_rotor.svg","sources/textures/B_bearing.svg")]
    plans=[("source",path,path.relative_to(ROOT)) for path in sources if path.is_file()]
    plans+=[("reference",path,path.relative_to(REPO/"tools/art-assets/turbine-rotor-bearing-concepts-10")) for path in references]
    for resource in resources:
        candidate=ROOT/"candidate"/resource["candidate"]
        installed=REPO/resource["destination"]
        assert sha(candidate)==sha(installed)==resource["sha256"]
        plans.append(("candidate",candidate,Path(resource["candidate"])))
        plans.append(("installed",installed,Path(resource["destination"])))
    result=[]
    for category,source,relative in plans:
        target=SNAPSHOT/category/relative
        assert target.resolve().is_relative_to(REPORT.resolve())
        target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copy2(source,target)
        assert sha(source)==sha(target)
        result.append({"category":category,"source":source.relative_to(REPO).as_posix(),"snapshot":target.relative_to(REPO).as_posix(),"sha256":sha(target)})
    identity={"task":"ART-TURBINE-10B-R1","pre_r1_mapping_sha256":sha(ROOT/"resource-map.json"),"candidate_installed_identity_count":26,"original_17_backups_untouched":True,"files":result}
    (REPORT/"pre-r1-identity.json").write_text(json.dumps(identity,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"source_files":sum(r["category"]=="source" for r in result),"reference_files":3,"candidate_files":26,"installed_files":26,"copy_hash_verified":len(result),"mapping_sha256":identity["pre_r1_mapping_sha256"]}))


if __name__=="__main__":
    main()
