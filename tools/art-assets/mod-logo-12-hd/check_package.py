"""ART-LOGO-12-HD有限校验与宣传ZIP封包，不渲染SVG或改写旧Logo。

检查连续SVG及本次既有导出身份、RGBA尺寸/抗锯齿/留白、真实预览引用，
再一次创建五项素材ZIP并逐字读回；六件旧源/资源/Java/JAR必须保持基线。
"""

import hashlib
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET
import zipfile

from PIL import Image


ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[2]
REPORTS = REPO / "build/reports/art/ART-LOGO-12-HD/r2"
SVG_NS = "{http://www.w3.org/2000/svg}"


def digest(content):
    return hashlib.sha256(content).hexdigest()


def record(path):
    content = path.read_bytes()
    return {"path": path.relative_to(REPO).as_posix(), "bytes": len(content), "sha256": digest(content)}


def save_json(name, data):
    (REPORTS / name).write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main():
    """只做本批结构/像素边界/身份检查，不重跑旧像素checker或模组任务。"""
    checks = []
    def require(name, condition):
        checks.append({"name": name, "passed": bool(condition)})
        if not condition:
            raise AssertionError(name)

    source = ROOT / "source/mod_logo_hd.svg"
    source_bytes = source.read_bytes()
    svg = ET.fromstring(source_bytes)
    require("SVG为1024连续矢量主稿", svg.tag == SVG_NS + "svg" and svg.attrib == {"width": "1024", "height": "1024", "viewBox": "0 0 1024 1024"})
    allowed = {"svg", "title", "desc", "defs", "path", "linearGradient", "radialGradient", "stop", "clipPath", "circle", "use", "g", "ellipse"}
    require("SVG无位图/字体/文本绘图或外部样式", all(node.tag.removeprefix(SVG_NS) in allowed for node in svg.iter()))
    ids = [node.attrib["id"] for node in svg.iter() if "id" in node.attrib]
    references = []
    for node in svg.iter():
        for key, value in node.attrib.items():
            if key.endswith("href"):
                references.append(value)
            references.extend(re.findall(r"url\(([^)]+)\)", value))
    require("SVG全部引用仅为唯一内部ID", len(ids) == len(set(ids)) and all(ref.startswith("#") and ref[1:] in ids for ref in references))
    gear = next(node for node in svg.iter() if node.attrib.get("id") == "gear").attrib["d"]
    require("R2八齿为直线切角且厚背层偏移明确", gear.count("L") == 64 and "Q" not in gear and any(node.attrib.get("href") == "#gear" and node.attrib.get("transform") == "translate(36 40)" for node in svg.iter()))
    require("R2八螺栓及四厚压板前/背层同源且无泡状反光", sum(node.attrib.get("href") == "#bolt" for node in svg.iter()) == 8 and sum(node.attrib.get("href") == "#plateBlock" for node in svg.iter()) == 8 and not any(node.tag == SVG_NS + "ellipse" for node in svg.iter()))
    old_svg = ET.parse(REPO / "tools/art-assets/mod-logo-11/source/mod_logo.svg").getroot()
    old_palette = {node.attrib["fill"] for node in old_svg if "fill" in node.attrib}
    new_palette = set(re.findall(rb"#[0-9A-F]{6}\b", source_bytes))
    require("矢量颜色端点全部沿用ART11色板", bool(new_palette) and new_palette.issubset({color.encode("ascii") for color in old_palette}))
    origin = json.loads((REPORTS / "export-origin.json").read_text(encoding="utf-8"))
    require("三导出记录绑定当前唯一SVG", origin["svg_sha256"] == digest(source_bytes) and all(row["source_sha256"] == digest(source_bytes) for row in origin["exports"]))
    require("当前公开源稿与已实看R2阶段源逐字相同", source_bytes == (ROOT / "source/mod_logo_hd_r2.svg").read_bytes())
    png_records = []
    for output in origin["exports"]:
        size = output["size"]
        path = ROOT / output["path"]
        require(f"{size}px稳定公开PNG与R2阶段导出逐字相同", path.read_bytes() == (ROOT / f"generated/mod_logo_hd_{size}.png").read_bytes())
        require(f"{size}px输出仍为本次实际SVG导出字节", digest(path.read_bytes()) == output["sha256"] and output["density"] == 72 * size / 1024)
        with Image.open(path) as image:
            require(f"{size}px PNG尺寸及RGBA正确", image.size == (size, size) and image.mode == "RGBA")
            alpha = image.getchannel("A")
            histogram = alpha.histogram()
            require(f"{size}px透明/不透明及抗锯齿有效", histogram[0] > 0 and histogram[255] > 0 and sum(histogram[1:255]) > 0)
            bbox = alpha.getbbox()
            margins = [bbox[0], bbox[1], size - bbox[2], size - bbox[3]]
            require(f"{size}px四边透明留白且齿端不触边", min(margins) >= 64 * size / 1024 and all(alpha.crop(box).getextrema() == (0, 0) for box in ((0, 0, size, 1), (0, size-1, size, size), (0, 0, 1, size), (size-1, 0, size, size))))
            require(f"{size}px连续渐变产生超过512种实际颜色", image.getcolors(maxcolors=512) is None)
            png_records.append({**output, "alpha_bbox": bbox, "transparent_margins_px": margins, "antialiased_pixels": sum(histogram[1:255])})
    html = (ROOT / "index.html").read_text(encoding="utf-8")
    references = re.findall(r'(?:href|src)="([^"]+)"', html)
    # 新ZIP此时尚未创建；R1历史链接保持原样，只核本轮R2当前下载与图形。
    require("预览直接引用真实R2 SVG/PNG及原像素图", all((ROOT / ref).is_file() for ref in references if ref != "create_nuclear_industry_logo_hd.zip") and "../mod-logo-11/generated/mod_logo_32.png" in references)
    zip_path = ROOT / "create_nuclear_industry_logo_hd_r2.zip"
    entries = {"mod_logo_hd.svg": source, **{f"mod_logo_hd_{size}.png": ROOT / f"generated/mod_logo_hd_{size}.png" for size in (1024, 2048, 4096)}, "README.md": ROOT / "README.md"}
    with zipfile.ZipFile(zip_path, "x", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name, path in entries.items():
            info = zipfile.ZipInfo(name, date_time=(2026, 10, 14, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, path.read_bytes(), compresslevel=9)
    zip_records = []
    with zipfile.ZipFile(zip_path) as archive:
        require("宣传ZIP恰有源稿/三PNG/说明五项", set(archive.namelist()) == set(entries))
        for name, path in entries.items():
            data = archive.read(name)
            require("ZIP " + name + "与实际源文件逐字一致", data == path.read_bytes())
            zip_records.append({"entry": name, "bytes": len(data), "sha256": digest(data)})
    # ZIP当前公开入口同样先核R1历史完整，再同步本次独立读回的R2五项包。
    public_zip = ROOT / "create_nuclear_industry_logo_hd.zip"
    require("公开旧ZIP与R1历史逐字相同再切换", public_zip.read_bytes() == (ROOT / "history/r1/create_nuclear_industry_logo_hd.zip").read_bytes())
    public_zip.write_bytes(zip_path.read_bytes())
    require("公开ZIP与R2实际五项包逐字相同", public_zip.read_bytes() == zip_path.read_bytes())
    require("离线预览全部下载/图像引用可解析", all((ROOT / ref).is_file() for ref in references))
    baseline = json.loads((REPORTS / "protection-before.json").read_text(encoding="utf-8"))
    after = []
    for row in baseline["files"]:
        path = REPO / row["path"]
        current = record(path)
        require("旧件保持：" + row["path"], current == row)
        after.append(current)
    save_json("protection-after.json", {"task": "ART-LOGO-12-HD", "files": after, "unchanged": True})
    report = {"task": "ART-LOGO-12-HD", "checks": checks, "svg": record(source), "svg_palette_endpoints": len(new_palette),
              "png": png_records, "zip": record(zip_path), "zip_entries": zip_records, "legacy_protected": len(after),
              "user_visual_acceptance": False, "mod_build_or_game_test": False}
    save_json("verification.json", report)
    files = [source, ROOT / "source/mod_logo_hd_r2.svg", ROOT / "export_hd.mjs", ROOT / "check_package.py", ROOT / "index.html", ROOT / "README.md", *(ROOT / row["path"] for row in origin["exports"]), *(ROOT / f"generated/mod_logo_hd_{size}.png" for size in (1024, 2048, 4096)), zip_path, public_zip]
    save_json("source-manifest.json", {"task": "ART-LOGO-12-HD", "phase": "r2-heavy-industrial", "r1_history": "tools/art-assets/mod-logo-12-hd/history/r1/manifest.json", "files": [record(path) for path in files]})
    print(f"CHECK R2 PASS={len(checks)}/{len(checks)} SVG=continuous-heavy 1024 palette_endpoints={len(new_palette)}")
    for row in png_records:
        print(f"CHECK PNG {row['size']} RGBA margins={row['transparent_margins_px']} antialias_pixels={row['antialiased_pixels']}")
    print(f"CHECK ZIP entries={len(zip_records)} source-byte-identical=5/5 SHA256={report['zip']['sha256']} bytes={report['zip']['bytes']}")
    print(f"CHECK legacy_protection={len(after)}/{len(after)} mod-build=0 game-test=0 user-visual-acceptance=0")


if __name__ == "__main__":
    main()
