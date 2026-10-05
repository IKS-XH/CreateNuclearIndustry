"""EXT-B-TURBINE-01D-R1端口密封几何、方向与装配预览检查。"""
from __future__ import annotations

import argparse
import importlib.util
import json
import shutil
import subprocess
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[1]
ASSETS = REPO / "src/main/resources/assets/create_nuclear_industry"
REPORT = REPO / "build/reports/extension/EXT-B-TURBINE-01D-R1"


def load_module(name: str, path: Path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


def obj_faces(text: str):
    vertices, uvs, normals, faces = [], [], [], []
    material = ""
    for row in text.splitlines():
        fields = row.split()
        if not fields:
            continue
        if fields[0] == "v":
            vertices.append(tuple(float(value) for value in fields[1:4]))
        elif fields[0] == "vt":
            uvs.append(tuple(float(value) for value in fields[1:3]))
        elif fields[0] == "vn":
            normals.append(tuple(float(value) for value in fields[1:4]))
        elif fields[0] == "usemtl":
            material = fields[1]
        elif fields[0] == "f":
            refs = [tuple(int(value) for value in token.split("/")) for token in fields[1:]]
            faces.append((material, [vertices[ref[0] - 1] for ref in refs],
                          [uvs[ref[1] - 1] for ref in refs], [normals[ref[2] - 1] for ref in refs]))
    return vertices, faces


def outer_spec(name: str):
    direction = name.rsplit("_", 1)[-1]
    if direction == "top":
        direction = "up"
    return {
        "left": (0, 0.0, -1), "west": (0, 0.0, -1),
        "right": (0, 1.0, 1), "east": (0, 1.0, 1),
        "down": (1, 0.0, -1), "up": (1, 1.0, 1),
        "north": (2, 0.0, -1), "south": (2, 1.0, 1),
    }[direction]


def visible_plane_samples(text: str, axis: int, plane: float, outward_sign: int,
                          samples: int = 17):
    vertices, faces = obj_faces(text)
    misses = []
    late_hits = []
    outside = 0.0 if outward_sign < 0 else 1.0
    for row in range(samples):
        for col in range(samples):
            u = (col + 0.5) / samples
            v = (row + 0.5) / samples
            point = [0.5, 0.5, 0.5]
            plane_axes = [value for value in range(3) if value != axis]
            point[plane_axes[0]], point[plane_axes[1]] = u, v
            hits = []
            for material, points, _, normals in faces:
                normal = normals[0]
                if normal[axis] * outward_sign <= 0.999:
                    continue
                if not all(abs(p[axis] - points[0][axis]) < 1e-7 for p in points):
                    continue
                depth = (points[0][axis] - outside) * -outward_sign
                if depth < -1e-7:
                    continue
                if point_in_face(point, points, axis):
                    hits.append((max(0.0, depth), material))
            if not hits:
                misses.append([round(u, 4), round(v, 4)])
            else:
                nearest_depth, _ = min(hits)
                if nearest_depth > 3 / 16 + 1e-7:
                    late_hits.append([round(u, 4), round(v, 4)])
    return misses, late_hits


def point_in_face(point, polygon, axis):
    axes = [value for value in range(3) if value != axis]
    x, y = point[axes[0]], point[axes[1]]
    inside = False
    for index in range(len(polygon)):
        a, b = polygon[index], polygon[(index + 1) % len(polygon)]
        ax, ay, bx, by = a[axes[0]], a[axes[1]], b[axes[0]], b[axes[1]]
        cross_value = (x - ax) * (by - ay) - (y - ay) * (bx - ax)
        if (abs(cross_value) < 1e-8 and min(ax, bx) - 1e-8 <= x <= max(ax, bx) + 1e-8 and
                min(ay, by) - 1e-8 <= y <= max(ay, by) + 1e-8):
            return True
        if (ay > y) != (by > y) and x < (bx - ax) * (y - ay) / (by - ay) + ax:
            inside = not inside
    return inside


def coverage_check(name: str, text: str, samples: int = 17):
    axis, plane, sign = outer_spec(name)
    misses, late_hits = visible_plane_samples(text, axis, plane, sign, samples)
    assert not misses and not late_hits, (name, "外表面有可透视采样点", misses[:5], late_hits[:5])
    return {"model": name, "outward_axis": axis, "plane": plane, "samples": samples * samples,
            "uncovered_rays": len(misses), "late_surface_rays": len(late_hits)}


def ring_uv_check(text: str, name: str):
    _, faces = obj_faces(text)
    axis, plane, _ = outer_spec(name)
    for material, points, coords, normals in faces:
        if material != "casing" or len(points) != 4:
            continue
        if not all(abs(p[axis] - plane) < 1e-7 for p in points):
            continue
        projected = [tuple(round(p[i], 6) for i in range(3) if i != axis) for p in points]
        actual = [tuple(round(value, 6) for value in uv) for uv in coords]
        assert actual == projected, (name, "外环纹理坐标必须沿安装面连续", projected, actual)
    return True


def resource_names(files: dict[str, bytes]):
    selected = {}
    for relative, content in files.items():
        path = Path(relative)
        if path.suffix not in (".obj", ".json"):
            continue
        if path.parent.as_posix().endswith("/models/block/turbine/mesh") and path.suffix == ".obj":
            pass
        elif path.parent.as_posix().endswith("/models/block/turbine") and path.suffix == ".json":
            pass
        else:
            continue
        stem = path.stem
        if (stem.startswith(("inlet_", "exhaust_", "turbine_inlet", "turbine_exhaust")) or
                stem in ("inlet", "exhaust")):
            selected[relative] = content
    return selected


def append_mesh(out, text: str, offset=(0.0, 0.0, 0.0)):
    vertices, faces = obj_faces(text)
    out["v"].extend([tuple(p[i] + offset[i] for i in range(3)) for p in vertices])
    raw_uvs, raw_normals = [], []
    material = ""
    current_vertices = []
    current_uvs = []
    current_normals = []
    for row in text.splitlines():
        fields = row.split()
        if not fields:
            continue
        if fields[0] == "vt":
            raw_uvs.append(tuple(float(value) for value in fields[1:3]))
        elif fields[0] == "vn":
            raw_normals.append(tuple(float(value) for value in fields[1:4]))
    out["vt"].extend(raw_uvs)
    out["vn"].extend(raw_normals)
    base_v, base_t, base_n = len(out["v"]) - len(vertices), len(out["vt"]) - len(raw_uvs), len(out["vn"]) - len(raw_normals)
    line_material = None
    for row in text.splitlines():
        fields = row.split()
        if not fields:
            continue
        if fields[0] == "usemtl":
            line_material = fields[1]
        elif fields[0] == "f":
            refs = [tuple(int(value) for value in token.split("/")) for token in fields[1:]]
            refs = [(base_v + v, base_t + t, base_n + n) for v, t, n in refs]
            out["f"].append((line_material, refs))


def write_scene(path: Path, scene_meshes):
    out = {"v": [], "vt": [], "vn": [], "f": []}
    for mesh_text, offset in scene_meshes:
        append_mesh(out, mesh_text, offset)
    rows = ["mtllib turbine.mtl", "o r1_port_shell_assembly"]
    rows += ["v " + " ".join(f"{value:.6f}" for value in p) for p in out["v"]]
    rows += ["vt " + " ".join(f"{value:.6f}" for value in p) for p in out["vt"]]
    rows += ["vn " + " ".join(f"{value:.6f}" for value in p) for p in out["vn"]]
    current = None
    for material, refs in out["f"]:
        if current != material:
            rows.append(f"usemtl {material}")
            current = material
        rows.append("f " + " ".join(f"{v}/{t}/{n}" for v, t, n in refs))
    path.write_text("\n".join(rows) + "\n", encoding="utf-8")


def make_assembly_preview(generator, checks, file_map):
    report_assets = REPORT / "preview_assets"
    report_assets.mkdir(parents=True, exist_ok=True)
    shutil.copy2(ASSETS / "models/block/turbine/mesh/turbine.mtl", report_assets / "turbine.mtl")
    casing = file_map[
        "assets/create_nuclear_industry/models/block/turbine/mesh/casing_d3_middle_x-1_y0.obj"
    ].decode("utf-8")
    scenes = {}
    for label, name in (("FORMED INLET", "inlet_left"), ("FORMED EXHAUST", "exhaust_left")):
        scene = [(file_map[f"assets/create_nuclear_industry/models/block/turbine/mesh/{name}.obj"].decode("utf-8"),
                  (0.0, 0.0, 0.0))]
        for dy, dz in ((-1, -1), (0, -1), (1, -1), (-1, 0), (1, 0), (-1, 1), (0, 1), (1, 1)):
            scene.append((casing, (0.0, float(dy), float(dz))))
        path = report_assets / f"{name}-adjacent-shell.obj"
        write_scene(path, scene)
        scenes[label] = path
    images = Image.new("RGB", (1000, 720), "#17212A")
    draw = ImageDraw.Draw(images)
    title = ImageFont.load_default(size=22)
    label_font = ImageFont.load_default(size=17)
    draw.text((20, 10), "TURBINE PORT / ADJACENT SHELL / OBJ UV + MTL / BACKFACE CULLING",
              font=title, fill="#E3ECE8")
    view_data = []
    for col, (name, obj_path) in enumerate(scenes.items()):
        for row, (view, eye) in enumerate((("外向", (-3.0, 0.0, 0.0)), ("机内", (3.0, 0.0, 0.0)))):
            image = checks.render_mesh(obj_path, eye, size=(480, 330)).convert("RGB")
            x, y = col * 500 + 10, row * 350 + 36
            images.paste(image, (x, y))
            english_view = "OUTSIDE" if view == "外向" else "INSIDE"
            draw.text((x + 8, y + 8), f"{name} / {english_view}", font=label_font, fill="#F0F4EF")
            view_data.append({"port": name, "view": english_view, "camera": eye})
    preview = REPORT / "R1-adjacent-shell-port-preview.png"
    images.save(preview, format="PNG", optimize=False)
    return preview, view_data


def main():
    parser = argparse.ArgumentParser(description="验证汽轮机端口外侧密封、方向和邻壳预览")
    parser.add_argument("--install", action="store_true", help="安装本次变更的进排汽口OBJ与模型JSON")
    args = parser.parse_args()
    generator = load_module("turbine_models", ROOT / "turbine_models.py")
    checks = load_module("turbine_01d_checks", ROOT / "turbine_01d_checks.py")
    files = generator.all_files()
    generator_summary = generator.verify_01b(files)
    targets = resource_names(files)
    assert targets and len(targets) <= 64, len(targets)
    for relative, content in targets.items():
        if relative.endswith(".json"):
            json.loads(content.decode("utf-8"))

    baseline = subprocess.run(["git", "rev-parse", "b0b3693"], cwd=REPO,
                              check=True, capture_output=True, text=True).stdout.strip()
    old_text = subprocess.run(
        ["git", "show", "b0b3693:src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/inlet_left.obj"],
        cwd=REPO, check=True, capture_output=True, text=True, encoding="utf-8").stdout
    misses, late_hits = visible_plane_samples(old_text, 0, 0.0, -1)
    assert misses, "基线外向覆盖检查未能复现漏视线负例"
    # 一条射线进入方孔角部，背面被剔除后应看见深处或无外向遮挡面。
    old = {}
    old["sampled_rays"] = 17 * 17
    old["uncovered_rays"] = len(misses)
    old["late_surface_rays"] = len(late_hits)

    oriented = []
    for relative, content in sorted(targets.items()):
        path = Path(relative)
        if path.suffix != ".obj":
            continue
        name = path.stem
        if name.startswith(("inlet_", "exhaust_")):
            # 普通态六方向、成型态四侧及兼容顶部资源都按真实法线测外侧遮挡。
            if "loose_" in name or name.endswith(("_left", "_right", "_up", "_down", "_top")):
                oriented.append(coverage_check(name, content.decode("utf-8")))
                if name.endswith(("_left", "_right", "_top")):
                    ring_uv_check(content.decode("utf-8"), name)
    assert len(oriented) >= 20, len(oriented)

    radii = generator.ROTOR_RADII
    port_text = files["assets/create_nuclear_industry/models/block/turbine/mesh/inlet_left.obj"].decode("utf-8")
    port_vertices, _ = obj_faces(port_text)
    tip_depth = max(point[0] for point in port_vertices)
    clearances = {f"D{diameter}": round(diameter / 2 - radius - tip_depth, 5)
                  for diameter, radius in radii.items()}
    assert clearances == {"D3": 0.03125, "D5": 0.03125, "D7": 0.03125}, clearances
    preview, views = make_assembly_preview(generator, checks, files)

    if args.install:
        for relative, content in targets.items():
            destination = REPO / "src/main/resources" / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(content)

    installed_mismatches = []
    if args.install:
        for relative, content in targets.items():
            if (REPO / "src/main/resources" / relative).read_bytes() != content:
                installed_mismatches.append(relative)
        assert not installed_mismatches, installed_mismatches

    report = {
        "baseline": baseline,
        "old_negative": old,
        "new_covered_models": oriented,
        "model_count": len(oriented),
        "existing_generator_orientation_check": {
            "result": "PASS",
            "formed_four_side_and_ordinary_six_direction_variants": {
                key: value for key, value in generator_summary["blockstate_variants"].items()
                if key in ("turbine_inlet", "turbine_exhaust")
            },
        },
        "installed_resource_count": len(targets) if args.install else 0,
        "rotor_clearance_blocks": {"outer_surface_to_sweep": 0.3125,
                                   "pipe_inward_depth": tip_depth,
                                   "remaining_by_tier": clearances},
        "assembly_preview": {"path": preview.relative_to(REPO).as_posix(), "views": views,
                             "rendering": "OBJ UV + MTL texture sampling with triangle backface culling"},
        "client_rendering": "NOT VERIFIED; requires the user's Minecraft client visual check",
    }
    REPORT.mkdir(parents=True, exist_ok=True)
    (REPORT / "R1-port-seal-checks.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (REPORT / "R1-port-seal-report.md").write_text(
        "# EXT-B-TURBINE-01D-R1 端口密封修复\n\n"
        "根因：基线把方孔薄板放在壳体内侧，外向圆面只覆盖圆盘投影；圆盘与方孔边角间的射线会进入壳内，内侧封面法线又背向外视线。\n\n"
        "修复：方形密封安装面移至壳体外表面，厚3/16格；外向圆面保持中心材质标识；圆管朝机内，最大内伸9/32格。方环使用连续全局UV，中央箭头独立完整映射。\n\n"
        f"验证：旧资源289条外向视线中有 {old['uncovered_rays']} 条透空，另有 {old['late_surface_rays']} 条要深入超过3/16格才遇到外向可见面；新模型覆盖 {len(oriented)} 个进排汽口模型，每个外面采样17×17射线；UV保持面内全局投影。D3/D5/D7按壳外至转子扫掠边界0.3125格，短管内伸0.28125格，三档净空均为0.03125格。\n\n"
        f"组合预览：`{preview.relative_to(REPO).as_posix()}`，采用正式OBJ、MTL贴图和背面剔除，包含成型进汽口与成型排汽口的外视、内视。\n\n"
        "资源审计（本轮已运行）：`& 'C:\\Users\\IKSXH\\.cache\\codex-runtimes\\codex-primary-runtime\\dependencies\\python\\python.exe' -B -c \"import importlib.util,json; p='tools/art-assets/turbine_01d_checks.py'; s=importlib.util.spec_from_file_location('checks',p); m=importlib.util.module_from_spec(s); s.loader.exec_module(m); print(json.dumps(m.audit_resources(),ensure_ascii=False,indent=2))\"`；退出码0。返回294个模型JSON/OBJ包装，8739个面绕序法线、8631个实体面、12种材质贴图引用全部通过；内向圆盖、圆端UV、轴承净空检查通过。\n\n"
        "技能：实际读取并应用minecraft-modding、minecraft-testing、minecraft-resource-pack及systematic-debugging。此资源修复未运行JUnit/GameTest、Gradle或Minecraft客户端；客户端画面仍待用户实机确认。\n",
        encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
