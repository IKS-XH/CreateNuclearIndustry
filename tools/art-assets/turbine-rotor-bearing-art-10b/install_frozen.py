"""按10B卡的26路径复制冻结候选；先备份全部17旧源，新增路径冲突时不写任何正式资源。"""
from __future__ import annotations

import hashlib
import json
import re
import shutil
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[2]
REPORT = REPO/"build/reports/art/ART-TURBINE-10B/assets"
BACKUP = ROOT/"backups/2026-10-12-install"
MAP_SHA256 = "a39ff51073c711fd3e5480b1fc972033f8211780182c8c6f7a5710b877855d0c"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    """离线文件安装入口；失败直接保留原始清单，不回退、不再生、不运行Gradle。"""
    map_path=ROOT/"resource-map.json"
    if sha(map_path)!=MAP_SHA256:
        raise RuntimeError("冻结资源映射已变化，停止安装")
    resources=json.loads(map_path.read_text(encoding="utf-8"))["resources"]
    card=(REPO/"docs/art/tasks/ART-TURBINE-10B.md").read_text(encoding="utf-8")
    approved=set()
    for line in card.splitlines():
        match=re.fullmatch(r"- `(src/main/resources/[^`]+)`",line.strip())
        if match:
            approved.add(match[1])
    if set(r["destination"] for r in resources)!=approved or len(resources)!=26:
        raise RuntimeError("实际任务卡的26路径与冻结映射不一致")
    if Counter(r["operation"] for r in resources)!={"replace":17,"add":9}:
        raise RuntimeError("替换/新增数量偏离批准的17/9")
    if BACKUP.exists():
        raise RuntimeError("本批备份目录已存在，禁止重复覆盖旧源备份")
    rows=[]
    # 先全量检查本批精确路径，确认9项新增没有其它批次文件，再创建旧源备份。
    for resource in resources:
        source=(ROOT/"candidate"/resource["candidate"]).resolve()
        target=(REPO/resource["destination"]).resolve()
        if not source.is_relative_to(ROOT) or not target.is_relative_to(REPO):
            raise RuntimeError("候选或目标路径越出批准工作树")
        if sha(source)!=resource["sha256"]:
            raise RuntimeError(f"候选SHA变化: {resource['candidate']}")
        if resource["operation"]=="add" and target.exists():
            raise RuntimeError(f"新增路径已有非本批文件: {resource['destination']}")
        if resource["operation"]=="replace" and not target.is_file():
            raise RuntimeError(f"应备份的旧文件缺失: {resource['destination']}")
        rows.append({**resource,"old_sha256":sha(target) if target.is_file() else None})
    REPORT.mkdir(parents=True,exist_ok=True)
    ledger={"task":"ART-TURBINE-10B","timestamp_utc":datetime.now(timezone.utc).isoformat(),"mapping_sha256":MAP_SHA256,"backup_root":BACKUP.relative_to(REPO).as_posix(),"status":"preflight_passed","resources":rows}
    manifest=REPORT/"installation-manifest.json"
    manifest.write_text(json.dumps(ledger,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    with (REPORT/"installation-events.jsonl").open("x",encoding="utf-8") as events:
        def event(value):
            events.write(json.dumps(value,ensure_ascii=False)+"\n")
            events.flush()
        for row in rows:
            if row["operation"]!="replace":
                continue
            target=REPO/row["destination"]
            backup=BACKUP/row["destination"]
            backup.parent.mkdir(parents=True,exist_ok=True)
            shutil.copy2(target,backup)
            if sha(backup)!=row["old_sha256"] or sha(target)!=row["old_sha256"]:
                raise RuntimeError(f"旧源备份不一致: {row['destination']}")
            row["backup"]=backup.relative_to(REPO).as_posix()
            row["backup_sha256"]=sha(backup)
            event({"event":"backup","destination":row["destination"],"backup":row["backup"],"sha256":row["backup_sha256"]})
        if len(list(BACKUP.rglob("*")))<17 or sum(p.is_file() for p in BACKUP.rglob("*"))!=17:
            raise RuntimeError("旧源备份数量必须恰好17文件")
        # 所有备份完成后再核旧源与新增路径；不允许把并行新写入覆盖成候选。
        for row in rows:
            target=REPO/row["destination"]
            if row["operation"]=="replace" and sha(target)!=row["old_sha256"]:
                raise RuntimeError(f"备份后旧源被并行改动: {row['destination']}")
            if row["operation"]=="add" and target.exists():
                raise RuntimeError(f"备份后新增路径冲突: {row['destination']}")
        event({"event":"all_backups_verified","count":17})
        for row in rows:
            source=ROOT/"candidate"/row["candidate"]
            target=REPO/row["destination"]
            target.parent.mkdir(parents=True,exist_ok=True)
            shutil.copyfile(source,target)
            row["installed_sha256"]=sha(target)
            if row["installed_sha256"]!=row["sha256"]:
                raise RuntimeError(f"复制后SHA不一致: {row['destination']}")
            event({"event":"installed","destination":row["destination"],"source_sha256":sha(source),"installed_sha256":row["installed_sha256"]})
        ledger["status"]="installed_verified"
        ledger["backup_count"]=17
        ledger["installed_count"]=26
        ledger["added_count"]=9
        manifest.write_text(json.dumps(ledger,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
        event({"event":"completed","installed_count":26,"backup_count":17,"added_count":9})
    print(json.dumps({"installed":26,"replaced":17,"added":9,"backed_up":17,"candidate_installed_sha_match":26,"mapping_sha256":MAP_SHA256},ensure_ascii=False))


if __name__=="__main__":
    main()
