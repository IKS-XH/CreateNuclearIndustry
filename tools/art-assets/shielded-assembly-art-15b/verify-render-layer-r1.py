"""R1只读来源检查：绑定真实模型透明面、默认层/命名层原生源码与alpha shader链。"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
import zipfile

from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
REPORT = ROOT / "build/reports/art/ART-SHIELDED-ASSEMBLY-15B-R1/assets"
BASE = "src/main/resources/assets/create_nuclear_industry"
SOURCES = ROOT / "build/moddev/artifacts/neoforge-21.1.219-sources.jar"
MC_ASSETS = ROOT / "build/moddev/artifacts/neoforge-21.1.219-client-extra-aka-minecraft-resources.jar"
JAR = ROOT / "build/libs/create_nuclear_industry-0.1.0.jar"
BASELINE_JAR_SHA = "a92a0bed0f948fc000b58800cad34f84c03ffb314b167fea3a839f117b12ff7f"


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def save(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    """只创建R1证据/备份，不改候选或生产；已有诊断身份不覆盖。"""
    REPORT.mkdir(parents=True, exist_ok=True)
    manifest = json.loads((HERE / "assets.json").read_text(encoding="utf-8"))
    affected = []
    for mapping in manifest["productionMappings"]:
        if "/parts/" not in mapping["target"]:
            continue
        installed = ROOT / mapping["target"]
        model = json.loads(installed.read_text(encoding="utf-8"))
        transparent_faces = [(index, face) for index, element in enumerate(model["elements"])
                             for face, value in element["faces"].items() if value["texture"] == "#window"]
        if transparent_faces:
            affected.append({"target": mapping["target"], "source": mapping["source"], "sha256": sha(installed),
                             "render_type": model.get("render_type"), "windowFaces": transparent_faces})
    image = Image.open(ROOT / f"{BASE}/textures/block/shielded_assembly_15c_window.png").convert("RGBA")
    pixels = [image.getpixel((x, y)) for y in range(16) for x in range(16)]
    alpha = sorted(set(p[3] for p in pixels))
    references = []
    entries = [(SOURCES, [
        "net/minecraft/client/renderer/block/model/BlockModel.java",
        "net/neoforged/neoforge/client/model/ExtendedBlockModelDeserializer.java",
        "net/neoforged/neoforge/client/model/geometry/BlockGeometryBakingContext.java",
        "net/neoforged/neoforge/client/model/geometry/SimpleUnbakedGeometry.java",
        "net/neoforged/neoforge/client/NamedRenderTypeManager.java",
        "net/minecraft/client/resources/model/SimpleBakedModel.java",
        "net/neoforged/neoforge/client/extensions/IBakedModelExtension.java",
        "net/minecraft/client/renderer/ItemBlockRenderTypes.java",
        "net/neoforged/neoforge/client/RenderTypeHelper.java",
        "net/minecraft/client/renderer/RenderType.java"]),
        (MC_ASSETS, ["assets/minecraft/shaders/core/rendertype_solid.fsh",
                     "assets/minecraft/shaders/core/rendertype_cutout_mipped.fsh"])]
    for jar, names in entries:
        with zipfile.ZipFile(jar) as archive:
            for name in names:
                raw = archive.read(name)
                path = REPORT / "native-sources" / name
                path.parent.mkdir(parents=True, exist_ok=True)
                if path.exists() and path.read_bytes() != raw:
                    raise ValueError("R1原生来源备份已有不同内容，停止覆盖")
                path.write_bytes(raw)
                references.append({"jar": jar.relative_to(ROOT).as_posix(), "jarSha256": sha(jar), "entry": name,
                                   "entrySha256": hashlib.sha256(raw).hexdigest(), "source": "锁定NeoForge/Minecraft第三方原生来源，仅诊断只读"})
    frozen_paths = [HERE / "produce.py", HERE / "assets.json"]
    frozen_paths += [HERE / mapping["source"] for mapping in manifest["productionMappings"]]
    frozen_paths += [ROOT / mapping["target"] for mapping in manifest["productionMappings"]]
    frozen_paths += [ROOT / f"{BASE}/blockstates/{name}.json" for name in ("shielded_assembly_station", "shielded_assembly_part")]
    frozen_paths += list((ROOT / "src/main/java/com/iksxh/create_nuclear_industry/production").glob("ShieldedAssembly*.java"))
    frozen_paths += [ROOT / "src/main/java/com/iksxh/create_nuclear_industry/content/FuelProcessingContent.java"]
    before = REPORT / "before-identity.json"
    identities = {}
    for path in sorted(set(frozen_paths)):
        rel = path.relative_to(ROOT).as_posix()
        backup = REPORT / "backup" / rel
        backup.parent.mkdir(parents=True, exist_ok=True)
        if backup.exists() and backup.read_bytes() != path.read_bytes():
            raise ValueError("R1原模型/工具备份已存在不同版本，停止覆盖")
        backup.write_bytes(path.read_bytes())
        identities[rel] = {"sha256": sha(path), "bytes": path.stat().st_size, "backup": backup.relative_to(ROOT).as_posix()}
    if not before.exists():
        save(before, identities)
    else:
        if json.loads(before.read_text(encoding="utf-8")) != identities:
            raise ValueError("R1原身份已有变化，停止重新诊断")
    result = {"task": "ART-SHIELDED-ASSEMBLY-15B-R1", "baselineJarSha256": sha(JAR), "baselineJarBytes": JAR.stat().st_size,
              "baselineJarMatches": sha(JAR) == BASELINE_JAR_SHA, "affected": affected,
              "window": {"sha256": sha(ROOT / f"{BASE}/textures/block/shielded_assembly_15c_window.png"),
                         "dimensions": list(image.size), "alphaValues": alpha, "transparentPixels": sum(p[3] == 0 for p in pixels),
                         "opaquePixels": sum(p[3] == 255 for p in pixels),
                         "transparentRgbValues": sorted(set(p[:3] for p in pixels if p[3] == 0))},
              "nativeReferences": references,
              "scope": "本结果只是实际输入/源码绑定；原生运行层RED/GREEN另由批准probe生成，不把JSON缺字段单独称作游戏结论。"}
    path = REPORT / "diagnosis-inputs.json"
    if path.exists():
        raise ValueError("diagnosis-inputs已冻结，禁止覆盖")
    save(path, result)
    print(json.dumps({"baselineJarMatches": result["baselineJarMatches"], "affectedModels": len(affected),
                      "parts": sorted({a["target"].split("part_")[-1].split("_")[0] for a in affected}),
                      "alpha": alpha, "transparentPixels": result["window"]["transparentPixels"], "backedUpFiles": len(identities)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
