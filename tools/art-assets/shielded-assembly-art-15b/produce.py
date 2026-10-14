"""从15A最终C原字节生成15B正式候选，并在预览就绪后精确安装29资源。

本工具仅使用SVG矩形原色/alpha导出16² PNG；八格切片保留外表面UV，
活动件仍为主控原点整机坐标，物品完整归一化至0..16。不生成工序动画。
"""

from __future__ import annotations

import sys
sys.dont_write_bytecode = True

import argparse
import copy
import hashlib
import json
from pathlib import Path
import shutil
import xml.etree.ElementTree as ET

from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
ASSETS = HERE / "assets"
REPORT = ROOT / "build/reports/art/ART-SHIELDED-ASSEMBLY-15B/assets"
R1_REPORT = ROOT / "build/reports/art/ART-SHIELDED-ASSEMBLY-15B-R1/assets"
APPROVED = ROOT / "tools/art-assets/shielded-assembly-concepts-15/assets"
APPROVED_CONTRACT_SHA = "b666789912cce985a7363e71d7dbb8c81cd294b490f86dc41ac3ab8bb79e41a2"
BASE = "src/main/resources/assets/create_nuclear_industry"
MODEL_BASE = BASE + "/models/block"
SLOTS = ("lead", "steel", "machined", "fastener", "brass", "window", "lamp_off", "lamp_on", "deck")
PARTIALS = ("left_arm", "right_arm", "fixture")
OLD_MODELS = [f"{MODEL_BASE}/shielded_assembly_station/parts/part_{part}_{state}.json" for part in range(8) for state in ("off", "on")]
OLD_MODELS += [f"{MODEL_BASE}/shielded_assembly_station/partials/{name}.json" for name in PARTIALS]
ITEM_TARGET = MODEL_BASE + "/shielded_assembly_station_item.json"
OLD_MODELS += [ITEM_TARGET]
PNG_TARGETS = {slot: f"{BASE}/textures/block/shielded_assembly_15c_{slot}.png" for slot in SLOTS}
FACE_NORMAL = {"north": (2, 0), "south": (2, 1), "east": (0, 1), "west": (0, 0), "up": (1, 1), "down": (1, 0)}
FACE_AXES = {"north": (0, 1), "south": (0, 1), "east": (2, 1), "west": (2, 1), "up": (0, 2), "down": (0, 2)}


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def save(path: Path, value: dict | list) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def relative(path: Path) -> str:
    return path.relative_to(HERE).as_posix()


def copy_once(source: Path, destination: Path) -> None:
    """冻结源与备份不覆盖已有不同内容；不把后续安装的新模型混作旧备份。"""
    if destination.exists():
        if sha(destination) != sha(source):
            raise ValueError(f"冻结路径已有不同内容：{destination}")
    else:
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)


def texture_map() -> dict:
    textures = {slot: f"create_nuclear_industry:block/shielded_assembly_15c_{slot}" for slot in SLOTS}
    textures["particle"] = textures["lead"]
    return textures


def frozen_model(name: str) -> dict:
    return load(ASSETS / f"source/models/c/{name}.json")


def protected_paths() -> list[Path]:
    """只核批准C、旧装配资源入口、旧PNG和相关Java，不复制旧全设备保护矩阵。"""
    paths = [APPROVED / "concepts.json", APPROVED.parent / "make-assets.py"]
    paths += list(APPROVED.rglob("*.json")) + list(APPROVED.rglob("*.svg"))
    paths += [ROOT / "tools/art-assets/fuel_02e_assets.py"]
    paths += list((ROOT / "tools/art-assets/sources/fuel-02e").glob("*"))
    paths += [ROOT / f"{BASE}/blockstates/{name}.json" for name in ("shielded_assembly_station", "shielded_assembly_part")]
    paths += [ROOT / f"{MODEL_BASE}/shielded_assembly_station_{state}.json" for state in ("off", "on")]
    paths += [ROOT / f"{BASE}/models/item/shielded_assembly_station.json"]
    paths += list((ROOT / f"{BASE}/textures/block").glob("shielded_assembly*.png"))
    paths += list((ROOT / "src/main/java/com/iksxh/create_nuclear_industry/production").glob("ShieldedAssembly*.java"))
    paths += [ROOT / "gradle.properties", ROOT / "logs/debug.log", ROOT / "logs/latest.log",
              ROOT / "build/libs/create_nuclear_industry-0.1.0.jar"]
    # 原离心机已经通过，不重测它；只核本批不写的现有安装来源身份。
    paths += list((ROOT / f"{MODEL_BASE}").glob("enrichment_centrifuge*.json"))
    paths += list((ROOT / f"{BASE}/textures/block").glob("enrichment_centrifuge*.png"))
    return sorted(set(p for p in paths if p.is_file() and p.as_posix() not in {str(ROOT / target).replace('\\', '/') for target in PNG_TARGETS.values()}))


def freeze() -> dict:
    if sha(APPROVED / "concepts.json") != APPROVED_CONTRACT_SHA:
        raise ValueError("批准15A契约SHA不匹配，停止生成")
    contract = load(APPROVED / "concepts.json")
    concept = contract["concepts"]["c"]
    copy_once(APPROVED / "concepts.json", ASSETS / "source/approved-concepts.json")
    for path in (*concept["models"].values(), *concept["textures"].values()):
        copy_once(APPROVED / path, ASSETS / "source" / path)
    before = REPORT / "source-protection-before.json"
    if not before.exists():
        save(before, {p.relative_to(ROOT).as_posix(): {"sha256": sha(p), "bytes": p.stat().st_size} for p in protected_paths()})
    backups = []
    for target in OLD_MODELS:
        original = ROOT / target
        backup = ASSETS / "backup" / target
        copy_once(original, backup)
        backups.append({"target": target, "backup": relative(backup), "sha256": sha(backup), "bytes": backup.stat().st_size})
    return {"contract": contract, "concept": concept, "backups": backups}


def expected_pixels(svg_path: Path) -> list[tuple]:
    """对本批只有rect的像素SVG按像素中心覆盖规则取原RGBA，不引入插值或调色。"""
    tree = ET.fromstring(svg_path.read_text(encoding="utf-8"))
    if {k: tree.attrib.get(k) for k in ("width", "height", "viewBox", "shape-rendering")} != {
            "width": "16", "height": "16", "viewBox": "0 0 16 16", "shape-rendering": "crispEdges"}:
        raise ValueError("SVG尺寸或渲染契约错误")
    pixels = [(0, 0, 0, 0)] * 256
    for node in tree:
        if node.tag.rsplit("}", 1)[-1] != "rect" or any(k in node.attrib for k in ("opacity", "fill-opacity", "transform", "filter")):
            raise ValueError("批准SVG含本批直接像素导出不支持的节点/属性")
        fill = node.attrib["fill"]
        if len(fill) != 7 or not fill.startswith("#"):
            raise ValueError("批准色必须为原16²槽的RGB十六进制色")
        color = tuple(int(fill[i:i + 2], 16) for i in (1, 3, 5)) + (255,)
        x0, y0, width, height = [float(node.attrib[k]) for k in ("x", "y", "width", "height")]
        for y in range(16):
            for x in range(16):
                if x0 <= x + .5 < x0 + width and y0 <= y + .5 < y0 + height:
                    pixels[y * 16 + x] = color
    return pixels


def write_png(source: Path, target: Path) -> None:
    image = Image.new("RGBA", (16, 16))
    image.putdata(expected_pixels(source))
    target.parent.mkdir(parents=True, exist_ok=True)
    image.save(target, format="PNG", optimize=False)


def clipped_face(face: str, original: dict, lo: list, hi: list) -> dict:
    """用原UV端点线性裁取实面区域，垂直面V方向保持Minecraft原映射。"""
    value = copy.deepcopy(original["faces"][face])
    if lo == original["from"] and hi == original["to"]:
        return value
    u, v = FACE_AXES[face]
    uv = value["uv"]
    old_lo, old_hi = original["from"], original["to"]
    def sample(position: float, axis: int, begin: float, end: float, reversed_axis: bool = False) -> float:
        fraction = (position - old_lo[axis]) / (old_hi[axis] - old_lo[axis])
        if reversed_axis:
            fraction = 1 - fraction
        return begin + (end - begin) * fraction
    v_reverse = face in ("north", "south", "east", "west")
    value["uv"] = [sample(lo[u], u, uv[0], uv[2]), sample(hi[v] if v_reverse else lo[v], v, uv[1], uv[3], v_reverse),
                   sample(hi[u], u, uv[0], uv[2]), sample(lo[v] if v_reverse else hi[v], v, uv[1], uv[3], v_reverse)]
    return value


def split_shell(shell: dict, part: int, working: bool) -> dict:
    """按实际part公式裁成局部模型，只保留源盒原外面，不添加格界内面。"""
    offset = [16 * (part & 1), 16 * (part >> 2), 16 * ((part >> 1) & 1)]
    elements = []
    for original in shell["elements"]:
        if "rotation" in original:
            raise ValueError("批准C含旋转几何，当前八格裁剪须另核；不得猜测切割")
        lo = [max(original["from"][k], offset[k]) for k in range(3)]
        hi = [min(original["to"][k], offset[k] + 16) for k in range(3)]
        if any(lo[k] >= hi[k] for k in range(3)):
            continue
        faces = {}
        for face in original["faces"]:
            axis, high = FACE_NORMAL[face]
            if (hi if high else lo)[axis] != original["to" if high else "from"][axis]:
                continue
            value = clipped_face(face, original, lo, hi)
            if working and value["texture"] == "#lamp_off":
                value["texture"] = "#lamp_on"
            faces[face] = value
        elements.append({"name": original.get("name", "C稿外壳"), "from": [lo[k] - offset[k] for k in range(3)],
                         "to": [hi[k] - offset[k] for k in range(3)], "faces": faces})
    result = {"credit": "ART-SHIELDED-ASSEMBLY-15B 已选C八格外壳", "ambientocclusion": shell["ambientocclusion"],
              "textures": texture_map(), "elements": elements}
    # 静态壳体不走BER的cutoutMipped缓冲；仅实际含稀疏透明窗面的八格模型声明原生命名层。
    if any(face["texture"] == "#window" for element in elements for face in element["faces"].values()):
        result["render_type"] = "minecraft:cutout_mipped"
    return result


def generate() -> dict:
    frozen = freeze()
    models = {name: frozen_model(name) for name in ("shell", *PARTIALS)}
    mappings = []
    part_paths = []
    def record(target: str, data: dict | None, kind: str) -> str:
        path = ASSETS / "candidate" / target
        if data is not None:
            save(path, data)
        source = relative(path)
        mappings.append({"kind": kind, "source": source, "target": target, "sha256": sha(path)})
        return source
    for part in range(8):
        item = {"part": part, "offset": [16 * (part & 1), 16 * (part >> 2), 16 * ((part >> 1) & 1)]}
        for state in ("off", "on"):
            target = f"{MODEL_BASE}/shielded_assembly_station/parts/part_{part}_{state}.json"
            item[state] = record(target, split_shell(models["shell"], part, state == "on"), "model")
        part_paths.append(item)
    partial_paths = {}
    for name in PARTIALS:
        candidate = copy.deepcopy(models[name])
        candidate["textures"] = texture_map()
        candidate["credit"] = "ART-SHIELDED-ASSEMBLY-15B 已选C待机机构，原整机坐标"
        target = f"{MODEL_BASE}/shielded_assembly_station/partials/{name}.json"
        partial_paths[name] = record(target, candidate, "model")
    original_item = load(ASSETS / "backup" / ITEM_TARGET)
    elements = []
    for name in ("shell", *PARTIALS):
        for original in models[name]["elements"]:
            element = copy.deepcopy(original)
            element["from"] = [value / 2 for value in element["from"]]
            element["to"] = [value / 2 for value in element["to"]]
            if "rotation" in element:
                element["rotation"]["origin"] = [value / 2 for value in element["rotation"]["origin"]]
            elements.append(element)
    item = {"credit": "ART-SHIELDED-ASSEMBLY-15B 已选C完整物品", "ambientocclusion": models["shell"]["ambientocclusion"],
            "textures": texture_map(), "elements": elements, "display": copy.deepcopy(original_item["display"])}
    item_path = record(ITEM_TARGET, item, "model")
    texture_paths = {}
    for slot, target in PNG_TARGETS.items():
        path = ASSETS / "candidate" / target
        write_png(ASSETS / f"source/textures/c/{slot}.svg", path)
        texture_paths[slot] = record(target, None, "texture")
    source_files = sorted(p for p in (ASSETS / "source").rglob("*") if p.is_file())
    manifest = {"schemaVersion": 1, "task": "ART-SHIELDED-ASSEMBLY-15B", "selectedConcept": "c", "name": "模块检修舱",
                "approvedContractSha256": APPROVED_CONTRACT_SHA, "approvedContract": "assets/source/approved-concepts.json",
                "models": {"parts": part_paths, "partials": partial_paths, "item": item_path}, "textures": texture_paths,
                "productionMappings": mappings, "backupModels": frozen["backups"],
                "sources": {relative(p): {"sha256": sha(p), "bytes": p.stat().st_size} for p in source_files},
                "coordinates": frozen["contract"]["coordinates"], "rig": frozen["concept"]["rig"],
                "status": "候选生成；生产安装须预览执行者ready后单独执行--install",
                "state": "on仅实际灯面绑定lamp_on；三partial是原待机姿态，无工序动画，Create SHAFT_HALF仍由原Renderer消费。",
                "reuse": "15A最终checks/单次REVIEW与负责人C实图按源SHA复用，不重跑三稿/旧全矩阵。"}
    save(HERE / "assets.json", manifest)
    return manifest


def restore_element(element: dict, offset: list) -> dict:
    value = copy.deepcopy(element)
    value["from"] = [value["from"][k] + offset[k] for k in range(3)]
    value["to"] = [value["to"][k] + offset[k] for k in range(3)]
    return value


def canonical(elements: list) -> list[str]:
    return sorted(json.dumps(element, sort_keys=True, ensure_ascii=False) for element in elements)


def validate(manifest: dict, installed: bool = False) -> dict:
    """只检查本批C同源、九PNG像素/alpha、八块还原、部分件、物品与现行引用。"""
    errors = []
    svg_png = {}
    for slot, path in manifest["textures"].items():
        image = Image.open(HERE / path).convert("RGBA")
        expected = expected_pixels(ASSETS / f"source/textures/c/{slot}.svg")
        actual = list(image.getdata())
        differences = sum(a != b for a, b in zip(actual, expected))
        alpha = sorted(set(pixel[3] for pixel in actual))
        if image.size != (16, 16) or differences:
            errors.append(f"{slot}: PNG像素与批准SVG不同")
        svg_png[slot] = {"dimensions": list(image.size), "pixelDifferences": differences, "alphaValues": alpha,
                         "sha256": sha(HERE / path)}
    source = {name: frozen_model(name) for name in ("shell", *PARTIALS)}
    reconstructed = []
    lamp_changes = []
    part_counts = {}
    for item in manifest["models"]["parts"]:
        off, on = load(HERE / item["off"]), load(HERE / item["on"])
        part_counts[str(item["part"])] = len(off["elements"])
        reconstructed += [restore_element(element, item["offset"]) for element in off["elements"]]
        expected_on = copy.deepcopy(off["elements"])
        changed = 0
        for element in expected_on:
            for face in element["faces"].values():
                if face["texture"] == "#lamp_off":
                    face["texture"] = "#lamp_on"
                    changed += 1
        if expected_on != on["elements"]:
            errors.append(f"part{item['part']}:on改变了非灯面")
        lamp_changes.append({"part": item["part"], "changedLampFaces": changed})
    if canonical(reconstructed) != canonical(source["shell"]["elements"]):
        errors.append("八格还原与批准C外露元素/UV不一致")
    partials = {}
    for name, path in manifest["models"]["partials"].items():
        candidate = load(HERE / path)
        matches = candidate["elements"] == source[name]["elements"]
        if not matches:
            errors.append(f"{name}:整机坐标或分件几何/UV漂移")
        partials[name] = {"sourceElementsExact": matches, "elements": len(candidate["elements"]), "rig": manifest["rig"][name]}
    item = load(HERE / manifest["models"]["item"])
    restored_item = []
    for element in item["elements"]:
        original = copy.deepcopy(element)
        original["from"] = [value * 2 for value in original["from"]]
        original["to"] = [value * 2 for value in original["to"]]
        if "rotation" in original:
            original["rotation"]["origin"] = [value * 2 for value in original["rotation"]["origin"]]
        restored_item.append(original)
    expected_item = [element for model in source.values() for element in model["elements"]]
    item_display_exact = item["display"] == load(ASSETS / "backup" / ITEM_TARGET)["display"]
    if restored_item != expected_item or not item_display_exact:
        errors.append("物品未完整归一化或现行display改变")
    for mapping in manifest["productionMappings"]:
        path = HERE / mapping["source"]
        if sha(path) != mapping["sha256"]:
            errors.append(f"候选源SHA漂移:{mapping['target']}")
        if mapping["kind"] == "model":
            model = load(path)
            limit = 32 if "/partials/" in mapping["target"] else 16
            if model["textures"] != texture_map() or "parent" in model:
                errors.append(f"实际生产槽或parent不正确:{mapping['target']}")
            for element in model["elements"]:
                if any(not 0 <= element["from"][k] < element["to"][k] <= limit for k in range(3)):
                    errors.append(f"模型边界错误:{mapping['target']}")
                for face in element["faces"].values():
                    slot = face["texture"][1:]
                    if slot not in SLOTS or len(face["uv"]) != 4 or any(not 0 <= value <= 16 for value in face["uv"]):
                        errors.append(f"模型UV/面槽错误:{mapping['target']}")
    blockstate_detail = {}
    for name in ("shielded_assembly_station", "shielded_assembly_part"):
        state = load(ROOT / f"{BASE}/blockstates/{name}.json")
        models = [entry["model"] for entry in state["variants"].values()]
        invalid = []
        for model_id in models:
            path = ROOT / f"{BASE}/models/{model_id.split(':', 1)[1]}.json"
            if not path.is_file():
                invalid.append(model_id)
        if invalid:
            errors.append(f"{name}:当前四朝向引用不存在:{invalid}")
        blockstate_detail[name] = {"variants": len(state["variants"]), "missingReferences": invalid,
                                   "sha256": sha(ROOT / f"{BASE}/blockstates/{name}.json")}
    protection = load(REPORT / "source-protection-before.json")
    changed = [path for path, value in protection.items() if not (ROOT / path).is_file() or sha(ROOT / path) != value["sha256"]]
    if changed:
        errors.append(f"范围外保护输入改变:{changed}")
    installed_detail = []
    if installed:
        for mapping in manifest["productionMappings"]:
            target = ROOT / mapping["target"]
            target_sha = sha(target) if target.is_file() else None
            installed_detail.append({**mapping, "installedSha256": target_sha, "matches": target_sha == mapping["sha256"]})
            if target_sha != mapping["sha256"]:
                errors.append(f"生产SHA读回不同:{mapping['target']}")
    result = {"task": "ART-SHIELDED-ASSEMBLY-15B", "passed": not errors, "errors": errors,
              "svgPng": svg_png, "partElementCounts": part_counts, "wholeShellSourceExact": canonical(reconstructed) == canonical(source["shell"]["elements"]),
              "lampOnOnly": lamp_changes, "partials": partials, "item": {"elements": len(item["elements"]), "displayExact": item_display_exact,
                  "completeNormalizedSourceExact": restored_item == expected_item}, "blockstates": blockstate_detail,
              "protectedFiles": len(protection), "protectedChanged": changed, "installed": installed_detail,
              "limitations": "静态C正式候选；未运行Minecraft/Gradle。复用15A批准C的几何/窗可读性，用户Minecraft人工验收独立。"}
    save(REPORT / ("installed-checks.json" if installed else "candidate-checks.json"), result)
    save(REPORT / "source-identity.json", {relative(p): {"sha256": sha(p), "bytes": p.stat().st_size} for p in
         [HERE / "produce.py", HERE / "assets.json"] + sorted(p for p in ASSETS.rglob("*") if p.is_file())})
    print(json.dumps({"passed": result["passed"], "errors": errors, "candidateResources": len(manifest["productionMappings"]),
                      "backups": len(manifest["backupModels"]), "wholeShellElements": len(reconstructed), "itemElements": len(item["elements"]),
                      "protectedFiles": len(protection), "installedReadback": len(installed_detail)}, ensure_ascii=False))
    return result


def install(manifest: dict) -> None:
    """显式--install仅在外部预览ready后调用；先全量核29目标，再写，避免混合安装。"""
    # 复用已通过的几何/像素证据；安装仅核绑定SHA和新旧目标，不重复生成或重跑同套检查。
    result = load(REPORT / "candidate-checks.json")
    if not result["passed"] or sha(ASSETS / "source/approved-concepts.json") != APPROVED_CONTRACT_SHA:
        raise ValueError("原有限候选检查或批准来源不通过，禁止安装")
    for mapping in manifest["productionMappings"]:
        if sha(HERE / mapping["source"]) != mapping["sha256"]:
            raise ValueError(f"候选已改变，需先报负责人：{mapping['source']}")
    for backup in manifest["backupModels"]:
        if sha(ROOT / backup["target"]) != backup["sha256"]:
            raise ValueError(f"生产模型已被其它作者改动，禁止覆盖：{backup['target']}")
    for target in PNG_TARGETS.values():
        if (ROOT / target).exists():
            raise ValueError(f"新PNG目标已被占用，禁止覆盖：{target}")
    for mapping in manifest["productionMappings"]:
        target = ROOT / mapping["target"]
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(HERE / mapping["source"], target)
    installed = [{**mapping, "installedSha256": sha(ROOT / mapping["target"]),
                  "matches": sha(ROOT / mapping["target"]) == mapping["sha256"]} for mapping in manifest["productionMappings"]]
    protection = load(REPORT / "source-protection-before.json")
    changed = [path for path, before in protection.items() if not (ROOT / path).is_file() or sha(ROOT / path) != before["sha256"]]
    errors = [entry["target"] for entry in installed if not entry["matches"]] + changed
    final = {"task": "ART-SHIELDED-ASSEMBLY-15B", "passed": not errors, "errors": errors,
             "candidateEvidenceReused": "candidate-checks.json，几何/PNG来源与29候选SHA保持",
             "installed": installed, "protectedFiles": len(protection), "protectedChanged": changed}
    save(REPORT / "installed-checks.json", final)
    save(REPORT / "source-identity.json", {relative(p): {"sha256": sha(p), "bytes": p.stat().st_size} for p in
         [HERE / "produce.py", HERE / "assets.json"] + sorted(p for p in ASSETS.rglob("*") if p.is_file())})
    print(json.dumps({"passed": final["passed"], "errors": errors, "installedReadback": len(installed),
                      "protectedFiles": len(protection), "geometryPixelEvidenceReused": True}, ensure_ascii=False))
    if not final["passed"]:
        raise ValueError("安装后读回或保护检查失败，保留证据并报告；不得自行回滚旧来源")


def render_layer_r1() -> None:
    """复用真实原生RED，只给已安装C的透明面part加层声明；不回写旧20模型备份或历史证据。"""
    diagnosis = load(R1_REPORT / "diagnosis-inputs.json")
    red = load(R1_REPORT / "native-red.json")
    before = load(R1_REPORT / "before-identity.json")
    manifest = load(HERE / "assets.json")
    if "renderLayerR1" in manifest:
        raise ValueError("R1已经应用，禁止重复安装或覆盖原RED/GREEN证据")
    baseline_sha = "a92a0bed0f948fc000b58800cad34f84c03ffb314b167fea3a839f117b12ff7f"
    jar = ROOT / "build/libs/create_nuclear_industry-0.1.0.jar"
    if sha(jar) != baseline_sha or jar.stat().st_size != 2812280:
        raise ValueError("当前包不是R1故障a92基线，停止修改")
    targets = [entry["target"] for entry in diagnosis["affected"]]
    red_targets = [entry["target"] for entry in red["bindings"] if entry["windowFaces"] > 0]
    if red["passed"] or red["incorrectBindings"] != 8 or len(targets) != 8 or set(targets) != set(red_targets):
        raise ValueError("真实RED与八个透明面目标不匹配，禁止扩大修改")
    # 先核所有29当前来源/生产保持a92资源身份，确保局部修改不夹带其它设备或候选漂移。
    for mapping in manifest["productionMappings"]:
        if sha(HERE / mapping["source"]) != mapping["sha256"] or sha(ROOT / mapping["target"]) != mapping["sha256"]:
            raise ValueError(f"R1原资源身份漂移：{mapping['target']}")
    changed = []
    for mapping in manifest["productionMappings"]:
        if mapping["target"] not in targets:
            continue
        candidate, installed = HERE / mapping["source"], ROOT / mapping["target"]
        model = load(candidate)
        if "render_type" in model or not any(face["texture"] == "#window" for element in model["elements"] for face in element["faces"].values()):
            raise ValueError("目标原模型与真实RED不一致")
        model["render_type"] = "minecraft:cutout_mipped"
        save(candidate, model)
        shutil.copyfile(candidate, installed)
        mapping["sha256"] = sha(candidate)
        restored = copy.deepcopy(load(candidate))
        del restored["render_type"]
        original = load(ROOT / before[mapping["target"]]["backup"])
        if restored != original or sha(installed) != mapping["sha256"]:
            raise ValueError("R1出现非层声明变化或生产读回不同")
        changed.append({"target": mapping["target"], "source": mapping["source"], "sha256": mapping["sha256"], "metadataOnly": True})
    manifest["renderLayerR1"] = {"task": "ART-SHIELDED-ASSEMBLY-15B-R1", "changedTargets": targets,
        "baselineJarSha256": baseline_sha, "baselineJarBytes": 2812280, "renderType": "minecraft:cutout_mipped",
        "nativeRed": "build/reports/art/ART-SHIELDED-ASSEMBLY-15B-R1/assets/native-red.json",
        "scope": "仅静态透明面层声明；几何/UV/PNG/partial/item保持，GREEN和局部Minecraft复看另记录"}
    save(HERE / "assets.json", manifest)
    expected = {"tools/art-assets/shielded-assembly-art-15b/produce.py", "tools/art-assets/shielded-assembly-art-15b/assets.json"}
    for entry in changed:
        expected |= {entry["target"], (HERE / entry["source"]).relative_to(ROOT).as_posix()}
    actual = {path for path, old in before.items() if sha(ROOT / path) != old["sha256"]}
    errors = []
    if actual != expected:
        errors.append({"unexpected": sorted(actual - expected), "missing": sorted(expected - actual)})
    for mapping in manifest["productionMappings"]:
        if sha(ROOT / mapping["target"]) != mapping["sha256"] or sha(HERE / mapping["source"]) != mapping["sha256"]:
            errors.append(mapping["target"])
    result = {"task": "ART-SHIELDED-ASSEMBLY-15B-R1", "passed": not errors, "errors": errors, "changedModels": changed,
              "metadataOnly": True, "all29MappingsMatch": not errors, "expectedChangedPaths": sorted(expected),
              "actualChangedPaths": sorted(actual), "unchangedProtectedFiles": len(before) - len(actual),
              "reused": "原15B像素/几何/物品/rig检查；不重新导出PNG、不重切八格、不覆盖15B原source-identity/checks。"}
    save(R1_REPORT / "metadata-checks.json", result)
    identities = [HERE / "produce.py", HERE / "assets.json", HERE / "verify-render-layer-r1.py", HERE / "ShieldedAssemblyRenderLayerR1Probe.java"]
    identities += [HERE / mapping["source"] for mapping in manifest["productionMappings"]]
    identities += [ROOT / mapping["target"] for mapping in manifest["productionMappings"]]
    save(R1_REPORT / "source-identity.json", {path.relative_to(ROOT).as_posix(): {"sha256": sha(path), "bytes": path.stat().st_size} for path in identities})
    print(json.dumps({"passed": not errors, "errors": errors, "changedModels": len(changed), "all29MappingsMatch": not errors,
                      "metadataOnly": True, "unchangedProtectedFiles": len(before) - len(actual)}, ensure_ascii=False))
    if errors:
        raise SystemExit(1)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--install", action="store_true", help="外部预览执行者ready后安装批准29项，不重新生成")
    parser.add_argument("--render-layer-r1", action="store_true", help="真实原生RED确认后仅修八个静态透明面模型metadata")
    args = parser.parse_args()
    if args.render_layer_r1:
        render_layer_r1()
        return
    if (HERE / "assets.json").exists() and "renderLayerR1" in load(HERE / "assets.json"):
        raise ValueError("15B历史生成/旧20模型install已冻结；R1不得回写历史检查或覆盖当前C")
    REPORT.mkdir(parents=True, exist_ok=True)
    if args.install:
        install(load(HERE / "assets.json"))
    else:
        manifest = generate()
        if not validate(manifest)["passed"]:
            raise SystemExit(1)


if __name__ == "__main__":
    main()
