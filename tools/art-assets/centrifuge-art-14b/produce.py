"""从已选14A A长轴稿生成14B正式候选，并在收到客户端RED证据后精确安装11项。

默认只备份/生成候选，不写src。SVG原字节保存；纯像素导出不改颜色或alpha。
物品将upper坐标/旋转原点加16，UV保持半格原取样；所有模型单位为1/16格。
"""

from __future__ import annotations

import sys
sys.dont_write_bytecode = True

import argparse
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import xml.etree.ElementTree as ET

from PIL import Image


ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
ASSETS = HERE/"assets"
EVIDENCE = ROOT/"build/reports/art/ART-CENTRIFUGE-14B/assets"
APPROVED = ROOT/"tools/art-assets/centrifuge-concepts-14/assets"
APPROVED_CONTRACT_SHA = "6634afc2c06b183d759519a351d61d2166f78b22e04662cd882d0d1b89f16ee6"
BASE = "src/main/resources/assets/create_nuclear_industry"
PARTS = {"lower": "enrichment_centrifuge", "upper": "enrichment_centrifuge_upper",
         "rotor": "enrichment_centrifuge_rotor", "shaft": "enrichment_centrifuge_shaft",
         "item": "enrichment_centrifuge_item"}
SLOTS = ("casing", "brass", "glass", "dark", "machined", "fastener")
READ_ONLY = [f"{BASE}/blockstates/enrichment_centrifuge.json", f"{BASE}/models/item/enrichment_centrifuge.json",
             "tools/art-assets/centrifuge-concepts-14/make-assets.py", "tools/art-assets/export.py"]


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def rel(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def write_json(path: Path, data: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")


def installed_model(part: str) -> str:
    return f"{BASE}/models/block/{PARTS[part]}.json"


def installed_texture(slot: str) -> str:
    return f"{BASE}/textures/block/enrichment_centrifuge_{slot}.png"


def source_identities() -> dict[str, str]:
    """锁定选A契约、四模型、六SVG及旧只读入口，避免生产阶段悄悄改选稿。"""
    paths = [APPROVED/"concepts.json", *[APPROVED/"a"/(part+".json") for part in PARTS if part != "item"],
             *[APPROVED/"a"/(slot+".svg") for slot in SLOTS], *[ROOT/path for path in READ_ONLY]]
    if sha(APPROVED/"concepts.json") != APPROVED_CONTRACT_SHA:
        raise ValueError("14A已选A契约SHA不符，停止本批导出")
    return {rel(path): sha(path) for path in paths}


def backup() -> dict:
    """首次备份精确11个正式目标；已有备份只读复用，三项未存在目标明确记为新增。"""
    manifest_path = EVIDENCE/"backup/manifest.json"
    if manifest_path.exists():
        return json.loads(manifest_path.read_text(encoding="utf-8"))
    paths = [*[installed_model(part) for part in PARTS], *[installed_texture(slot) for slot in SLOTS]]
    result = {"sourceIdentities": source_identities(), "resources": {}}
    for path in paths:
        source = ROOT/path
        exists = source.exists()
        result["resources"][path] = {"existed": exists, "sha256": sha(source) if exists else None}
        if exists:
            saved = EVIDENCE/"backup"/path
            saved.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, saved)
    write_json(manifest_path, result)
    return result


def pixel_svg(path: Path, palette: dict[str, str]) -> Image.Image:
    """复用原strict exporter纯函数；只在内存删非视觉title，源SVG原字节不改。

    原导出器严格拒绝非rect元素；title是本批无渲染作用的来源标题，不是图形。
    不运行它的安装/批量入口，不写旧工具或缓存，输出每SVG坐标单位正好一个像素。
    """
    tree = ET.fromstring(path.read_text(encoding="utf-8"))
    for child in list(tree):
        if child.tag == "{http://www.w3.org/2000/svg}title":
            tree.remove(child)
    spec = importlib.util.spec_from_file_location("art_pixel_export_readonly", ROOT/"tools/art-assets/export.py")
    exporter = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(exporter)
    return exporter.render_svg(ET.tostring(tree, encoding="unicode"), {color.upper() for color in palette.values()}, (16, 16))


def formal_model(source: dict) -> dict:
    """几何/UV逐项保持，仅将六槽纹理引用转成现有正式namespace。"""
    output = copy.deepcopy(source)
    output["textures"] = {"particle": "#casing", **{slot: f"create_nuclear_industry:block/enrichment_centrifuge_{slot}" for slot in SLOTS}}
    return output


def move_upper(element: dict) -> dict:
    """物品整机只平移upper的Y和元素旋转原点Y；显式UV仍使用原半格的采样域。"""
    output = copy.deepcopy(element)
    output["from"][1] += 16
    output["to"][1] += 16
    if "rotation" in output:
        output["rotation"]["origin"][1] += 16
    return output


def prepare() -> None:
    baseline = backup()
    before = source_identities()
    if before != baseline["sourceIdentities"]:
        raise ValueError("选稿或只读入口在备份后漂移")
    source_contract = json.loads((APPROVED/"concepts.json").read_text(encoding="utf-8"))
    selected = next(concept for concept in source_contract["concepts"] if concept["id"] == "a")
    (ASSETS/"source").mkdir(parents=True, exist_ok=True)
    shutil.copy2(APPROVED/"concepts.json", ASSETS/"source/approved-concepts.json")
    originals = {}
    models = {}
    textures = {}
    for part in ("lower", "upper", "rotor", "shaft"):
        path = APPROVED/selected["models"][part]
        saved = ASSETS/"source"/(part+".json")
        shutil.copy2(path, saved)
        originals[part] = json.loads(saved.read_text(encoding="utf-8"))
        candidate = ASSETS/"models"/(PARTS[part]+".json")
        write_json(candidate, formal_model(originals[part]))
        models[part] = {"source": rel(path), "sourceSha256": sha(path), "sourceCopy": rel(saved),
                        "candidate": rel(candidate), "installed": installed_model(part), "sha256": sha(candidate),
                        "elements": len(originals[part]["elements"])}
    old_item_path = EVIDENCE/"backup"/installed_model("item")
    old_item = json.loads(old_item_path.read_text(encoding="utf-8"))
    item = copy.deepcopy(old_item)
    item["textures"] = formal_model(originals["lower"])["textures"]
    item["elements"] = copy.deepcopy(originals["lower"]["elements"]) + [move_upper(e) for e in originals["upper"]["elements"]]
    item["elements"] += copy.deepcopy(originals["rotor"]["elements"]) + copy.deepcopy(originals["shaft"]["elements"])
    candidate = ASSETS/"models"/(PARTS["item"]+".json")
    write_json(candidate, item)
    models["item"] = {"source": installed_model("item"), "sourceSha256": sha(old_item_path),
                       "candidate": rel(candidate), "installed": installed_model("item"), "sha256": sha(candidate),
                       "elements": len(item["elements"]), "composition": ["lower", "upper+16", "rotor", "shaft"]}
    texture_checks = {}
    for slot in SLOTS:
        source = APPROVED/selected["textures"][slot]
        saved = ASSETS/"source"/(slot+".svg")
        shutil.copy2(source, saved)
        image = pixel_svg(saved, selected["palette"])
        candidate = ASSETS/"textures"/("enrichment_centrifuge_"+slot+".png")
        candidate.parent.mkdir(parents=True, exist_ok=True)
        image.save(candidate, format="PNG", optimize=False, compress_level=9)
        actual = Image.open(candidate).convert("RGBA")
        pixel_digest = hashlib.sha256(actual.tobytes()).hexdigest()
        textures[slot] = {"source": rel(source), "sourceSha256": sha(source), "sourceCopy": rel(saved),
                          "candidate": rel(candidate), "installed": installed_texture(slot), "sha256": sha(candidate),
                          "pixelSha256": pixel_digest, "size": list(actual.size), "mode": "RGBA"}
        texture_checks[slot] = {"pixelsEqualSVG": actual.tobytes() == image.tobytes(),
                                "size": list(actual.size), "alphaValues": sorted({pixel[3] for pixel in actual.getdata()}),
                                "colorValues": sorted({tuple(pixel[:3]) for pixel in actual.getdata()})}
    checks = {"sourcesUnchanged": before == source_identities(), "models": {}, "textures": texture_checks,
              "itemElements": len(item["elements"]), "displayPreserved": item.get("display") == old_item.get("display"),
              "sourceCopiesByteEqual": all(sha(ROOT/entry["sourceCopy"]) == entry["sourceSha256"]
                                            for entry in [*[value for key, value in models.items() if key != "item"], *textures.values()]),
              "errors": []}
    for part, entry in models.items():
        data = json.loads((ROOT/entry["candidate"]).read_text(encoding="utf-8"))
        invalid_uv = []
        for index, element in enumerate(data["elements"]):
            for face, definition in element["faces"].items():
                if len(definition["uv"]) != 4 or any(not 0 <= value <= 16 for value in definition["uv"]):
                    invalid_uv.append([index, face])
        equal = data["elements"] == (item["elements"] if part == "item" else originals[part]["elements"])
        paths_valid = all(data["textures"][slot] == f"create_nuclear_industry:block/enrichment_centrifuge_{slot}" for slot in SLOTS)
        checks["models"][part] = {"elements": len(data["elements"]), "sourceGeometryUVEqual": equal,
                                  "invalidUV": invalid_uv, "formalTexturesValid": paths_valid}
        if not equal or invalid_uv or not paths_valid:
            checks["errors"].append(f"{part}模型/UV/引用失败")
    upper_start = len(originals["lower"]["elements"])
    upper_count = len(originals["upper"]["elements"])
    checks["upperYAndOriginsOffset16"] = all(
        item["elements"][upper_start+index] == move_upper(element)
        for index, element in enumerate(originals["upper"]["elements"]))
    checks["itemPartRanges"] = {"lower": [0, upper_start], "upper": [upper_start, upper_start+upper_count],
                               "rotor": [upper_start+upper_count, upper_start+upper_count+23], "shaft": [345, 346]}
    if len(item["elements"]) != 346 or not all(checks[key] for key in ("sourcesUnchanged", "displayPreserved", "sourceCopiesByteEqual", "upperYAndOriginsOffset16")):
        checks["errors"].append("346物品组合/原display/来源身份失败")
    if not all(data["pixelsEqualSVG"] and data["size"] == [16, 16] for data in texture_checks.values()):
        checks["errors"].append("SVG到PNG像素漂移")
    checks["passed"] = not checks["errors"]
    contract = {"schemaVersion": 1, "task": "ART-CENTRIFUGE-14B", "status": "candidate-ready",
                "sourceConcept": {"id": "a", "contract": rel(APPROVED/"concepts.json"), "sha256": APPROVED_CONTRACT_SHA,
                                  "revision": "r3", "refinement": "long-shaft"},
                "models": models, "textures": textures, "sourceIdentities": before,
                "producer": {"path": rel(Path(__file__)), "sha256": sha(Path(__file__))},
                "installationCount": 11, "installation": [entry for entry in [*models.values(), *textures.values()]],
                "sweepEvidenceReused": "build/reports/art/ART-CENTRIFUGE-14A/assets/r3-long/checks.json"}
    write_json(HERE/"assets.json", contract)
    write_json(EVIDENCE/"candidate-checks.json", checks)
    print(json.dumps({"passed": checks["passed"], "candidateResources": 11, "sourceCopies": 11,
                      "itemElements": len(item["elements"]), "sourcesUnchanged": checks["sourcesUnchanged"],
                      "displayPreserved": checks["displayPreserved"], "errors": checks["errors"]}, ensure_ascii=False, indent=2))
    if not checks["passed"]:
        raise SystemExit(1)


def install(red_evidence: Path) -> None:
    """收到原客户端作者安装消息后使用；仅复制候选11项，不再导出或改变候选。"""
    if not red_evidence.exists():
        raise ValueError("缺少原资源真实RED证据，禁止安装")
    contract = json.loads((HERE/"assets.json").read_text(encoding="utf-8"))
    baseline = json.loads((EVIDENCE/"backup/manifest.json").read_text(encoding="utf-8"))
    if source_identities() != contract["sourceIdentities"]:
        raise ValueError("来源在候选后发生漂移")
    for entry in contract["installation"]:
        target = ROOT/entry["installed"]
        previous = baseline["resources"][entry["installed"]]
        if target.exists() != previous["existed"] or previous["existed"] and sha(target) != previous["sha256"]:
            raise ValueError(f"正式目标在RED/备份后漂移: {entry['installed']}")
        if sha(ROOT/entry["candidate"]) != entry["sha256"]:
            raise ValueError("候选身份漂移")
    for entry in contract["installation"]:
        target = ROOT/entry["installed"]
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(ROOT/entry["candidate"], target)
    installed = {entry["installed"]: {"candidate": entry["candidate"], "sha256": sha(ROOT/entry["installed"]),
                                      "byteEqual": sha(ROOT/entry["installed"]) == entry["sha256"]}
                 for entry in contract["installation"]}
    result = {"redEvidence": {"path": rel(red_evidence), "sha256": sha(red_evidence)},
              "installed": installed, "sourcesUnchanged": source_identities() == contract["sourceIdentities"],
              "passed": all(value["byteEqual"] for value in installed.values())}
    result["passed"] = result["passed"] and result["sourcesUnchanged"]
    contract["status"] = "installed"
    contract["installedResources"] = installed
    write_json(HERE/"assets.json", contract)
    write_json(EVIDENCE/"installation-checks.json", result)
    print(json.dumps({"passed": result["passed"], "installedCount": len(installed),
                      "sourcesUnchanged": result["sourcesUnchanged"]}, indent=2))
    if not result["passed"]:
        raise SystemExit(1)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="14B A版精确候选/安装入口")
    parser.add_argument("--install", action="store_true", help="只在收到原客户端作者RED后安装消息时使用")
    parser.add_argument("--red-evidence", type=Path, help="原正式资源模型合同的实际RED日志路径")
    args = parser.parse_args()
    if args.install:
        if args.red_evidence is None:
            parser.error("安装必须记录真实--red-evidence")
        install(args.red_evidence.resolve())
    else:
        prepare()
