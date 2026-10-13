"""独立读回ART-LOGO-11已授权的两项安装，冻结当前工具/消费来源清单。

只读取正式资源，不重生候选或修改资源映射；既有phase1像素检查复用。
两共享源仅登记当前身份，精确消费边界沿用shared-check与冻结原件证据。
"""

import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[2]
REPORTS = REPO / "build/reports/art/ART-LOGO-11"
EXPECTED = {
    "assets/create_nuclear_industry/models/item/mod_logo.json": "12b704b0441f7e0c7274e855b1fc4d3b986b39975e0b59bbb3ae1bcffc3417b0",
    "assets/create_nuclear_industry/textures/item/mod_logo.png": "aa7798fecc9eb620f1eb7451a757605d978664bad863dda10a798af88cd6fb35",
}


def sha256(content):
    return hashlib.sha256(content).hexdigest()


def record(path):
    content = path.read_bytes()
    return {"path": path.relative_to(REPO).as_posix(), "bytes": len(content), "sha256": sha256(content)}


def main():
    """仅核两文件安装身份及已冻结来源没有漂移，结果写本批reports。"""
    mapping = ROOT / "resource-map.json"
    assert sha256(mapping.read_bytes()) == "9c94d090704a63476aacf685e5de2f514080e6778c373903ad3e3b92bf276d10", "冻结映射漂移"
    installed_records = []
    for relative, expected in EXPECTED.items():
        candidate = ROOT / "candidate" / relative
        installed = REPO / "src/main/resources" / relative
        candidate_bytes = candidate.read_bytes()
        installed_bytes = installed.read_bytes()
        assert sha256(candidate_bytes) == expected, "候选身份漂移"
        assert installed_bytes == candidate_bytes, "正式读回与候选字节不同"
        installed_records.append({"candidate": record(candidate), "installed": record(installed), "byte_identical": True})
        print(f"INSTALL READBACK {relative} bytes={len(installed_bytes)} sha256={expected} candidate=installed")
    data = {"task": "ART-LOGO-11", "new_formal_files": installed_records, "source_check_reused": "phase1-verification.json",
            "mapping_sha256": sha256(mapping.read_bytes()), "minecraft_visual_acceptance": False, "gradle_run": False}
    (REPORTS / "installation-identity.json").write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    paths = sorted(path for path in ROOT.rglob("*") if path.is_file())
    paths.extend(REPO / "src/main/resources" / relative for relative in EXPECTED)
    paths.extend(REPO / "src/main/java/com/iksxh/create_nuclear_industry/content" / name for name in ("ModItems.java", "ModCreativeTabs.java"))
    manifest = {"task": "ART-LOGO-11", "phase": "installed-pre-assemble", "files": [record(path) for path in paths]}
    (REPORTS / "installed-source-manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"INSTALL CHECK PASS=2/2; frozen_source_files={len(paths)} formal_writes=2 regen=0 gradle=0")


if __name__ == "__main__":
    main()
