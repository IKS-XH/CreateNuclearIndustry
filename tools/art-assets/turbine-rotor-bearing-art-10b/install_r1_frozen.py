"""仅安装R1批准的14个冻结变化；先核26个preR1身份、备份14原件，另12文件只读。"""
import hashlib
import json
import re
import shutil
from datetime import datetime, timezone
from pathlib import Path

ROOT=Path(__file__).resolve().parent
REPO=ROOT.parents[2]
REPORT=REPO/"build/reports/art/ART-TURBINE-10B/assets/r1"
SNAPSHOT=REPORT/"pre-r1"
BACKUP=REPORT/"installation-backup"
MAP_SHA="238f377912d128ed1b8bbd7531aac16803ae3c133d05c53728d4f9b2ccb29704"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    """不再生候选、不改原备份；任何并行SHA变化均在生产复制前停止并保留原始日志。"""
    if sha(ROOT/"resource-map.json")!=MAP_SHA:
        raise RuntimeError("R1冻结映射SHA变化，停止安装")
    if BACKUP.exists() or (REPORT/"installation-events.jsonl").exists():
        raise RuntimeError("R1安装备份/事件已经存在，禁止重复覆盖历史")
    current=json.loads((ROOT/"resource-map.json").read_text(encoding="utf-8"))["resources"]
    before=json.loads((SNAPSHOT/"source/resource-map.json").read_text(encoding="utf-8"))["resources"]
    before_by_path={r["destination"]:r for r in before}
    assert len(current)==len(before)==26
    approved=set()
    for line in (REPO/"docs/art/tasks/ART-TURBINE-10B-R1.md").read_text(encoding="utf-8").splitlines():
        match=re.fullmatch(r"- `(src/main/resources/[^`]+)`",line.strip())
        if match:
            approved.add(match[1])
    changes=[]
    protected=[]
    for row in current:
        source=(ROOT/"candidate"/row["candidate"]).resolve()
        target=(REPO/row["destination"]).resolve()
        assert source.is_relative_to(ROOT) and target.is_relative_to(REPO)
        old=before_by_path[row["destination"]]
        assert sha(source)==row["sha256"],"R1候选与冻结映射不符"
        if sha(target)!=old["sha256"] or sha(SNAPSHOT/"installed"/row["destination"])!=old["sha256"]:
            raise RuntimeError(f"生产preR1身份变化: {row['destination']}")
        record={**row,"old_sha256":old["sha256"]}
        if old["sha256"]!=row["sha256"]:
            changes.append(record)
        else:
            protected.append(record)
    if len(changes)!=14 or {r["destination"] for r in changes}!=approved:
        raise RuntimeError("实际14变化路径与R1卡批准名单不符")
    assert len(protected)==12 and sum(r["destination"].endswith(".json") for r in protected)==11
    assert sum(r["destination"].endswith(".obj") for r in changes)==10
    assert sum(r["destination"].endswith(".png") for r in changes)==4
    manifest={"task":"ART-TURBINE-10B-R1","timestamp_utc":datetime.now(timezone.utc).isoformat(),"mapping_sha256":MAP_SHA,"preflight_installed_pre_r1_count":26,"status":"preflight_passed","changes":changes,"protected":protected}
    manifest_path=REPORT/"installation-manifest.json"
    manifest_path.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    with (REPORT/"installation-events.jsonl").open("x",encoding="utf-8") as events:
        def event(value):
            events.write(json.dumps(value,ensure_ascii=False)+"\n")
            events.flush()
        event({"event":"all_26_pre_r1_identity_verified","count":26})
        for row in changes:
            target=REPO/row["destination"]
            backup=BACKUP/row["destination"]
            assert backup.resolve().is_relative_to(REPORT.resolve())
            backup.parent.mkdir(parents=True,exist_ok=True)
            shutil.copy2(target,backup)
            assert sha(backup)==sha(target)==row["old_sha256"]
            row["backup"]=backup.relative_to(REPO).as_posix()
            row["backup_sha256"]=sha(backup)
            event({"event":"backup","destination":row["destination"],"backup":row["backup"],"sha256":row["backup_sha256"]})
        assert sum(p.is_file() for p in BACKUP.rglob("*"))==14
        # 全部备份先完成；复制前再次核26身份，避免覆盖其它执行者的并行写入。
        for row in changes+protected:
            if sha(REPO/row["destination"])!=row["old_sha256"]:
                raise RuntimeError(f"备份后生产文件被并行修改: {row['destination']}")
        event({"event":"all_14_backups_verified","count":14})
        for row in changes:
            source=ROOT/"candidate"/row["candidate"]
            target=REPO/row["destination"]
            shutil.copyfile(source,target)
            row["installed_sha256"]=sha(target)
            assert row["installed_sha256"]==sha(source)==row["sha256"]
            event({"event":"installed","destination":row["destination"],"candidate_sha256":row["sha256"],"installed_sha256":row["installed_sha256"]})
        for row in current:
            assert sha(REPO/row["destination"])==sha(ROOT/"candidate"/row["candidate"])==row["sha256"]
        for row in protected:
            assert sha(REPO/row["destination"])==row["old_sha256"]
        manifest.update(status="installed_verified",installed_change_count=14,backup_count=14,candidate_installed_count=26,protected_unchanged_count=12)
        manifest_path.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
        event({"event":"completed","installed_changes":14,"backups":14,"candidate_installed_identity":26,"protected_unchanged":12})
    print(json.dumps({"installed_changes":14,"obj":10,"png":4,"backed_up":14,"candidate_installed_identity":26,"protected_unchanged":12,"mapping_sha256":MAP_SHA}))


if __name__=="__main__":
    main()
