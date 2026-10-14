"""15C原C分件生成器：先发布rig，再把86个原元素唯一分为八件；只安装新增八模型。"""

from __future__ import annotations
import argparse
import copy
import json
from pathlib import Path
import shutil
import sys
sys.dont_write_bytecode = True

import hashlib
import importlib.util

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
ASSETS = HERE / "assets"
REPORT = ROOT / "build/reports/art/ART-SHIELDED-ASSEMBLY-15C/assets"
MODEL_BASE = "src/main/resources/assets/create_nuclear_industry/models/block/shielded_assembly_station"
spec = importlib.util.spec_from_file_location("asset15c", HERE / "verify-assets.py")
helper = importlib.util.module_from_spec(spec)
spec.loader.exec_module(helper)


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def save(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def freeze() -> None:
    """只冻结既有29资源和原C来源身份，不写旧15B/R1契约或重导PNG。"""
    manifest = load(ROOT / "tools/art-assets/shielded-assembly-art-15b/assets.json")
    paths = [ROOT / mapping["target"] for mapping in manifest["productionMappings"]]
    paths += [ROOT / "tools/art-assets/shielded-assembly-art-15b/assets.json", ROOT / "tools/art-assets/shielded-assembly-art-15b/produce.py"]
    paths += list((ROOT / "tools/art-assets/shielded-assembly-art-15b/assets/source").rglob("*.json"))
    paths += list((ROOT / "tools/art-assets/shielded-assembly-art-15b/assets/source").rglob("*.svg"))
    identity = {path.relative_to(ROOT).as_posix(): {"sha256": sha(path), "bytes": path.stat().st_size} for path in sorted(set(paths))}
    original = REPORT / "before-identity.json"
    if original.exists() and load(original) != identity:
        raise ValueError("15C原C/R1来源已漂移，停止生成")
    if not original.exists():
        save(original, identity)
    for name in ("left_arm", "right_arm", "fixture"):
        source = ROOT / f"{MODEL_BASE}/partials/{name}.json"
        target = ASSETS / f"source/{name}.json"
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists() and target.read_bytes() != source.read_bytes():
            raise ValueError("原partial字节备份已有不同内容")
        if not target.exists():
            shutil.copyfile(source, target)


def contract() -> dict:
    freeze()
    groups = {}
    pivots = {
        "left_base": [10, 21.5, 16], "left_tool": [10, 20, 14.5],
        "right_base": [22, 21.5, 16], "right_tool": [22, 20, 14.5],
        "fixture_base": [16, 13.5, 16], "fixture_carriage": [16, 15, 16],
        "fixture_jaw_left": [15.4375, 16, 16], "fixture_jaw_right": [16.5625, 16, 16],
    }
    for name, (source, indices) in helper.MEMBERS.items():
        original = load(ASSETS / f"source/{source}.json")
        candidate = f"assets/candidate/{MODEL_BASE}/animation/{name}.json"
        parent = "fixture_carriage" if name.startswith("fixture_jaw_") else None
        fixed = name.endswith("base")
        groups[name] = {"model": f"create_nuclear_industry:block/shielded_assembly_station/animation/{name}",
            "candidate": candidate, "production": f"{MODEL_BASE}/animation/{name}.json", "parent": parent,
            "sourcePartial": source, "sourceIndices": indices, "sourceElementNames": [original["elements"][i]["name"] for i in indices],
            "pivot": pivots[name], "fixed": fixed,
            "pivotMeaning": "实际执行杆顶面中心" if name.endswith("tool") else "实际导柱/台面/钳口连接基准；固定组不转动",
            "domains": ["idle"] if fixed else (["manufacture_align", "sealing_lock"] if name.endswith("tool") else ["fixture_press"])}
    result = {"schemaVersion": 1, "task": "ART-SHIELDED-ASSEMBLY-15C", "units": "model pixels; 16=1 block; rotation degrees",
        "coordinates": {"origin": "北向主控格最小角", "northBounds": [[0, 0, 0], [32, 32, 32]], "facingPivotXZ": [8, 8],
                        "partialOffset": [0, 0, 0], "shaftCenter": [8, 0, 8]},
        "transformOrder": ["ancestorTranslation", "groupTranslation", "pivot", "rotationX", "rotationY", "rotationZ", "negativePivot", "facingAroundMaster"],
        "transformMeaning": "全局整机原点分件；列向量先绕本组pivot，再局部位移/父升降，最后绕主控x=z=8转朝向；消费前位移与pivot除16。",
        "groups": groups,
        "motionDomains": {
            "idle": {"translation": [0, 0, 0], "rotation": [0, 0, 0]},
            "manufacture_align": {"left_tool": {"translationMin": [0, 0, 0], "translationMax": [.25, 0, 0], "rotationMin": [0, 0, 0], "rotationMax": [0, 0, 14]},
                                  "right_tool": {"translationMin": [-.25, 0, 0], "translationMax": [0, 0, 0], "rotationMin": [0, 0, -14], "rotationMax": [0, 0, 0]},
                                  "carriageY": 0, "rule": "制造对齐，Z倾斜与短侧滑可组合；Y锁紧必须0，左右交替。"},
            "sealing_lock": {"left_tool": {"translationMin": [0, 0, 0], "translationMax": [1.5, 0, 0], "rotationMin": [0, -6, 0], "rotationMax": [0, 6, 0]},
                             "right_tool": {"translationMin": [-1.5, 0, 0], "translationMax": [0, 0, 0], "rotationMin": [0, -6, 0], "rotationMax": [0, 6, 0]},
                             "carriageY": 0, "rule": "封装合拢/短锁紧；Z必须0，不与制造倾斜域叠加。"},
            "fixture_press": {"fixture_carriage": {"translationMin": [0, 0, 0], "translationMax": [0, 1, 0], "rotationMin": [0, 0, 0], "rotationMax": [0, 0, 0]},
                              "fixture_jaw_left": {"translationMin": [-.5, 0, 0], "translationMax": [.1, 0, 0], "rotationMin": [0, 0, 0], "rotationMax": [0, 0, 0]},
                              "fixture_jaw_right": {"translationMin": [-.1, 0, 0], "translationMax": [.5, 0, 0], "rotationMin": [0, 0, 0], "rotationMax": [0, 0, 0]},
                              "rule": "jaw局部X叠父carriage升降；carriage非零时双tool严格待机，base全部零变换。"}},
        "crossDomainRules": ["制造与封装工具域是互斥动作域，不是各轴上限自由混合。", "工具任一非零姿态时carriage Y=0；升降时工具均完全待机。", "jaw可在工具域开合，但只沿X，Y只能继承carriage。"],
        "allowedContacts": [
            {"moving": "left_tool/right_tool:执行杆加工钢芯", "fixed": "同侧base:肩中央关节体", "meaning": "原钢芯顶部在肩部socket内受控接合，最大Y嵌入约0.303，不碰肩轨/黄铜圈。"},
            {"moving": "fixture_carriage:工作台锁固圈", "fixed": "fixture_base:升降中柱", "meaning": "原导柱/夹台内嵌接合；升降0..1保持连接，1时杆顶Y15与锁圈底Y15接触，不能再升。"}],
        "workingClearances": {"manufactureFingerToFixtureOuterEdgeMinX": .08625100452633,
                              "closedJawInnerGap": .05, "claim": "未画物料，不宣称工具已经碰到工件；接合与真实剩余空隙分别记录。"},
        "sourcePartials": {name: {"path": f"{MODEL_BASE}/partials/{name}.json", "sha256": sha(ASSETS / f"source/{name}.json")} for name in ("left_arm", "right_arm", "fixture")},
        "baselineJar": {"sha256": "602985f85d2522c7914d7a4cef0b1c822f8c50ce11687e60af2be697428e3cd3", "bytes": 2812546},
        "consumerRule": "同一生产animation.json绑定本rig SHA并承载动作；本rig只定义资产/几何限制，不驱动无进度假循环。"}
    save(HERE / "rig.json", result)
    return result


def generate() -> None:
    rig = load(HERE / "rig.json")
    freeze()
    mappings = []
    for name, definition in rig["groups"].items():
        source = load(ASSETS / f"source/{definition['sourcePartial']}.json")
        model = copy.deepcopy(source)
        model["credit"] = "ART-SHIELDED-ASSEMBLY-15C 原C精确分组：" + name
        model["elements"] = [copy.deepcopy(source["elements"][index]) for index in definition["sourceIndices"]]
        candidate = HERE / definition["candidate"]
        save(candidate, model)
        mappings.append({"group": name, "source": definition["candidate"], "target": definition["production"], "sha256": sha(candidate)})
    manifest = {"schemaVersion": 1, "task": "ART-SHIELDED-ASSEMBLY-15C", "rig": "rig.json", "rigPath": "rig.json", "rigSha256": sha(HERE / "rig.json"),
                "productionMappings": mappings, "groups": rig["groups"], "sourcePartials": rig["sourcePartials"],
                "models": {mapping["group"]: mapping["source"] for mapping in mappings},
                "totalOriginalElements": 86, "status": "候选生成；只安装八新增animation分件"}
    save(HERE / "assets.json", manifest)
    print(json.dumps({"candidateGroups": len(mappings), "originalElements": 86, "rigSha256": manifest["rigSha256"]}, ensure_ascii=False))


def install() -> None:
    """检查既有本批证据及八个未占用目标，然后只安装新增模型并读回，不重新生成。"""
    checks = load(REPORT / "checks.json")
    manifest = load(HERE / "assets.json")
    if not checks["passed"] or sha(HERE / "rig.json") != manifest["rigSha256"]:
        raise ValueError("15C资产检查或rig身份不通过")
    for mapping in manifest["productionMappings"]:
        if sha(HERE / mapping["source"]) != mapping["sha256"] or (ROOT / mapping["target"]).exists():
            raise ValueError("八新增候选已变或生产目标已占用")
    for mapping in manifest["productionMappings"]:
        target = ROOT / mapping["target"]
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(HERE / mapping["source"], target)
    entries = [{**mapping, "installedSha256": sha(ROOT / mapping["target"])} for mapping in manifest["productionMappings"]]
    before = load(REPORT / "before-identity.json")
    changed = [path for path, entry in before.items() if sha(ROOT / path) != entry["sha256"]]
    passed = all(entry["installedSha256"] == entry["sha256"] for entry in entries) and not changed
    save(REPORT / "installed-checks.json", {"passed": passed, "entries": entries, "oldProtectedFiles": len(before), "protectedChanged": changed})
    identity_paths = [HERE / "produce.py", HERE / "verify-assets.py", HERE / "assets.json", HERE / "rig.json"]
    identity_paths += sorted(path for path in ASSETS.rglob("*") if path.is_file())
    save(REPORT / "source-identity.json", {path.relative_to(ROOT).as_posix(): {"sha256": sha(path), "bytes": path.stat().st_size} for path in identity_paths})
    print(json.dumps({"passed": passed, "installedReadback": len(entries), "protectedChanged": changed}, ensure_ascii=False))
    if not passed:
        raise SystemExit(1)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--contract", action="store_true", help="只冻结来源并先发布rig")
    parser.add_argument("--install", action="store_true", help="本批检查通过后只安装八新增分件")
    options = parser.parse_args()
    if options.contract:
        result = contract()
        print(json.dumps({"groups": {name: len(data["sourceIndices"]) for name, data in result["groups"].items()}, "rigSha256": sha(HERE / "rig.json")}, ensure_ascii=False))
    elif options.install:
        install()
    else:
        generate()
