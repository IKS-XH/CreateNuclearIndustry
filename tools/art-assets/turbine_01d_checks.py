"""汽轮机01D资源离线审计与真实UV纹理预览工具。"""
from __future__ import annotations

import importlib.util
import json
import math
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[1]
ASSETS = REPO / "src/main/resources/assets/create_nuclear_industry"
REPORT = REPO / "build/reports/extension/EXT-B-TURBINE-01D-FINAL-ART"


def read_obj(path: Path):
    """读取OBJ面、UV、法线与材质，索引转为零起始值。"""
    vertices, uvs, normals, faces = [], [], [], []
    material = ""
    for row in path.read_text(encoding="utf-8").splitlines():
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
            material = " ".join(fields[1:])
        elif fields[0] == "f":
            refs = [tuple(int(value) for value in token.split("/")) for token in fields[1:]]
            faces.append((material, [vertices[ref[0] - 1] for ref in refs],
                          [uvs[ref[1] - 1] for ref in refs], [normals[ref[2] - 1] for ref in refs]))
    return vertices, faces


def texture_path(resource: str) -> Path:
    """把OBJ MTL的命名空间资源位置换算为模组PNG路径。"""
    namespace, location = resource.split(":", 1) if ":" in resource else ("minecraft", resource)
    return REPO / "src/main/resources/assets" / namespace / "textures" / f"{location}.png"


def audit_resources() -> dict:
    """扫描汽轮机模型JSON、全部OBJ及其MTL材质和贴图引用。"""
    model_root = ASSETS / "models/block/turbine"
    model_json = sorted(model_root.rglob("*.json"))
    obj_root = model_root / "mesh"
    obj_paths = sorted(obj_root.rglob("*.obj"))
    assert model_json and obj_paths, "汽轮机模型资源缺失"

    materials = {}
    for path in obj_root.rglob("*.mtl"):
        declared = {}
        current = None
        for row in path.read_text(encoding="utf-8").splitlines():
            fields = row.split(maxsplit=1)
            if not fields:
                continue
            if fields[0] == "newmtl":
                current = fields[1]
                declared[current] = {"opacity": 1.0, "texture": None}
            elif current and fields[0] in ("d", "Tr"):
                value = float(fields[1])
                declared[current]["opacity"] = value if fields[0] == "d" else 1.0 - value
            elif current and fields[0] == "map_Kd":
                declared[current]["texture"] = fields[1].split()[-1]
        materials[path] = declared

    referenced_objs = set()
    for path in model_json:
        value = json.loads(path.read_text(encoding="utf-8"))
        if value.get("loader") != "neoforge:obj":
            continue
        resource = value["model"]
        namespace, model_path = resource.split(":", 1)
        obj_path = REPO / "src/main/resources/assets" / namespace / f"{model_path}"
        assert obj_path.is_file(), (path, obj_path)
        referenced_objs.add(obj_path)

    renderer = (REPO / "src/main/java/com/iksxh/create_nuclear_industry/turbine/client/TurbineRotorRenderer.java")
    renderer_source = renderer.read_text(encoding="utf-8")
    partial_refs = {f"rotor_blades_d{diameter}" for diameter in (3, 5, 7)}
    assert all(f'"{name}"' in renderer_source for name in partial_refs), (renderer, "动态叶片模型清单变化")
    for java in (REPO / "src/main/java").rglob("*.java"):
        source = java.read_text(encoding="utf-8")
        partial_refs.update(re.findall(r'"block/turbine/([^" ]+)"', source))
    for name in partial_refs:
        path = ASSETS / "models/block/turbine" / f"{name}.json"
        assert path.is_file(), ("Java PartialModel", path)

    face_count = 0
    opaque_face_count = 0
    texture_refs = set()
    for path in obj_paths:
        text = path.read_text(encoding="utf-8").splitlines()
        libs = [row.split(maxsplit=1)[1] for row in text if row.startswith("mtllib ")]
        assert libs, (path, "missing mtllib")
        declared = set()
        texture_by_material = {}
        for lib in libs:
            library_path = path.parent / lib
            assert library_path.is_file(), (path, library_path)
            for material, data in materials[library_path].items():
                declared.add(material)
                texture_by_material[material] = data["texture"]
                assert 0.0 <= data["opacity"] <= 1.0, (library_path, material, data)
                if data["texture"]:
                    texture = texture_path(data["texture"])
                    assert texture.is_file(), (library_path, material, texture)
                    texture_refs.add(texture)
        used = {row.split(maxsplit=1)[1] for row in text if row.startswith("usemtl ")}
        assert used <= declared, (path, sorted(used - declared))
        vertices, faces = read_obj(path)
        assert vertices and faces, path
        cap_uvs = {}
        for material, points, coords, normals in faces:
            face_count += 1
            edge_a = tuple(points[1][i] - points[0][i] for i in range(3))
            edge_b = tuple(points[2][i] - points[0][i] for i in range(3))
            geometric = (edge_a[1] * edge_b[2] - edge_a[2] * edge_b[1],
                         edge_a[2] * edge_b[0] - edge_a[0] * edge_b[2],
                         edge_a[0] * edge_b[1] - edge_a[1] * edge_b[0])
            for normal in normals:
                alignment = sum(a * b for a, b in zip(geometric, normal))
                assert alignment > 1e-9, (path, material, "面绕序与逐顶点显式法线反向", alignment)
                assert 0.95 < math.sqrt(sum(value * value for value in normal)) < 1.05, (path, material, normal)
            assert all(math.isfinite(value) for uv in coords for value in uv), (path, coords)
            if material in ("inlet", "exhaust"):
                for point, uv in zip(points, coords):
                    key = tuple(round(value, 6) for value in point)
                    mapped = tuple(round(value, 6) for value in uv)
                    assert key not in cap_uvs or cap_uvs[key] == mapped, (path, key, "圆端相邻三角UV断裂")
                    cap_uvs[key] = mapped
            if material not in ("glass", "glass_edge"):
                texture = texture_path(texture_by_material[material])
                alpha = Image.open(texture).convert("RGBA").getchannel("A")
                assert alpha.getextrema() == (255, 255), (path, material, texture, "实体面贴图含透明像素")
                opaque_face_count += 1
    for end in ("front", "rear"):
        path = obj_root / f"output_shaft_{end}.obj"
        _, faces = read_obj(path)
        def axial_radii(material):
            return [math.hypot(p[0] - 0.5, p[1] - 0.5)
                    for face_material, points, _, _ in faces if face_material == material
                    and max(v[2] for v in points) - min(v[2] for v in points) > 1e-6 for p in points]
        shaft_radii, bearing_radii = axial_radii("shaft"), axial_radii("inside")
        assert shaft_radii and bearing_radii, (path, "轴身或轴承内壁缺失")
        assert min(bearing_radii) > max(shaft_radii) + 0.005, (path, "轴承内壁与轴身等半径共面")
        outer_z = 0.0 if end == "front" else 1.0
        caps = [points for material, points, _, _ in faces if material == "shaft"
                and all(abs(p[2] - outer_z) < 1e-6 for p in points)]
        assert len(caps) == 18, (path, "外向轴端三角封面数量异常或重叠", len(caps))
    for kind in ("inlet", "exhaust"):
        path = obj_root / f"{kind}_loose_west.obj"
        _, faces = read_obj(path)
        sealed = [points for material, points, _, normals in faces if material == "inside"
                  and len(points) == 4 and all(abs(p[0] - 0.92) < 1e-6 for p in points)
                  and all(normal[0] > 0.99 for normal in normals)]
        assert len(sealed) == 1, (path, "普通端口背侧方孔未封闭")
    return {"model_json": len(model_json), "obj_meshes": len(obj_paths),
            "wrapper_references": len(referenced_objs), "java_partial_models": sorted(partial_refs),
            "faces_with_winding_normal_check": face_count, "opaque_solid_faces": opaque_face_count,
            "bearing_shaft_radial_clearance": "PASS", "port_inboard_seal": "PASS",
            "circular_cap_shared_uv": "PASS",
            "material_textures": len(texture_refs),
            "mtl_material_references": "PASS", "texture_resource_paths": "PASS",
            "explicit_normal_winding": "PASS"}


def material_definitions(obj_path: Path):
    """读取OBJ对应材质的贴图和MTL透明度。"""
    result = {}
    for row in obj_path.read_text(encoding="utf-8").splitlines():
        fields = row.split(maxsplit=1)
        if fields and fields[0] == "mtllib":
            library = obj_path.parent / fields[1]
            current = None
            for line in library.read_text(encoding="utf-8").splitlines():
                parts = line.split(maxsplit=1)
                if not parts:
                    continue
                if parts[0] == "newmtl":
                    current = parts[1]
                    result[current] = {"texture": None, "opacity": 1.0}
                elif current and parts[0] == "map_Kd":
                    result[current]["texture"] = texture_path(parts[1].split()[-1])
                elif current and parts[0] in ("d", "Tr"):
                    value = float(parts[1])
                    result[current]["opacity"] = value if parts[0] == "d" else 1.0 - value
    return result


def render_mesh(obj_path: Path, eye, size=(360, 300)) -> Image.Image:
    """按OBJ真实UV与MTL贴图绘制正交投影，供客户端前的离线检查。"""
    vertices, faces = read_obj(obj_path)
    textures = material_definitions(obj_path)
    loaded = {}
    for material, config in textures.items():
        if config["texture"] and config["texture"] not in loaded:
            loaded[config["texture"]] = Image.open(config["texture"]).convert("RGBA")

    target = tuple((min(p[i] for p in vertices) + max(p[i] for p in vertices)) / 2 for i in range(3))
    forward = tuple(target[i] - eye[i] for i in range(3))
    length = math.sqrt(sum(value * value for value in forward))
    forward = tuple(value / length for value in forward)
    world_up = (0, 1, 0) if abs(forward[1]) < 0.96 else (0, 0, 1)
    right = (forward[1] * world_up[2] - forward[2] * world_up[1],
             forward[2] * world_up[0] - forward[0] * world_up[2],
             forward[0] * world_up[1] - forward[1] * world_up[0])
    rlen = math.sqrt(sum(value * value for value in right))
    right = tuple(value / rlen for value in right)
    up = (right[1] * forward[2] - right[2] * forward[1],
          right[2] * forward[0] - right[0] * forward[2],
          right[0] * forward[1] - right[1] * forward[0])
    projected = []
    for point in vertices:
        relative = tuple(point[i] - target[i] for i in range(3))
        projected.append((sum(relative[i] * right[i] for i in range(3)),
                          sum(relative[i] * up[i] for i in range(3)),
                          sum((point[i] - eye[i]) * forward[i] for i in range(3))))
    scale = min((size[0] - 28) / max(max(p[0] for p in projected) - min(p[0] for p in projected), 1e-5),
                (size[1] - 28) / max(max(p[1] for p in projected) - min(p[1] for p in projected), 1e-5))
    center = (size[0] / 2, size[1] / 2)
    screen = [(center[0] + p[0] * scale, center[1] - p[1] * scale, p[2]) for p in projected]
    screen_by_point = {point: screen[index] for index, point in enumerate(vertices)}
    image = Image.new("RGBA", size, (23, 33, 42, 255))
    pixels = image.load()
    zbuffer = [[float("inf") for _ in range(size[0])] for _ in range(size[1])]
    for material, points, coords, normals in faces:
        if sum(normals[0][i] * (eye[i] - points[0][i]) for i in range(3)) <= 0:
            continue
        tex_path = textures.get(material, {}).get("texture")
        tex = loaded.get(tex_path)
        opacity = textures.get(material, {}).get("opacity", 1.0)
        for fan in range(1, len(points) - 1):
            point_indices = [0, fan, fan + 1]
            tri = [screen_by_point[points[idx]] for idx in point_indices]
            uv = [coords[idx] for idx in point_indices]
            min_x = max(0, int(math.floor(min(p[0] for p in tri))))
            max_x = min(size[0] - 1, int(math.ceil(max(p[0] for p in tri))))
            min_y = max(0, int(math.floor(min(p[1] for p in tri))))
            max_y = min(size[1] - 1, int(math.ceil(max(p[1] for p in tri))))
            denom = ((tri[1][1] - tri[2][1]) * (tri[0][0] - tri[2][0]) +
                     (tri[2][0] - tri[1][0]) * (tri[0][1] - tri[2][1]))
            if abs(denom) < 1e-9:
                continue
            for py in range(min_y, max_y + 1):
                for px in range(min_x, max_x + 1):
                    x, y = px + 0.5, py + 0.5
                    a = ((tri[1][1] - tri[2][1]) * (x - tri[2][0]) +
                         (tri[2][0] - tri[1][0]) * (y - tri[2][1])) / denom
                    b = ((tri[2][1] - tri[0][1]) * (x - tri[2][0]) +
                         (tri[0][0] - tri[2][0]) * (y - tri[2][1])) / denom
                    c = 1.0 - a - b
                    if min(a, b, c) < -1e-7:
                        continue
                    depth = a * tri[0][2] + b * tri[1][2] + c * tri[2][2]
                    if depth >= zbuffer[py][px]:
                        continue
                    uu = (a * uv[0][0] + b * uv[1][0] + c * uv[2][0]) % 1.0
                    vv = (a * uv[0][1] + b * uv[1][1] + c * uv[2][1]) % 1.0
                    color = tex.getpixel((min(tex.width - 1, int(uu * tex.width)),
                                          min(tex.height - 1, int((1 - vv) * tex.height)))) if tex else (170, 180, 185, 255)
                    alpha = int(color[3] * opacity)
                    if alpha <= 0:
                        continue
                    old = pixels[px, py]
                    blend = tuple((color[channel] * alpha + old[channel] * (255 - alpha)) // 255
                                  for channel in range(3)) + (255,)
                    pixels[px, py] = blend
                    if alpha >= 200:
                        zbuffer[py][px] = depth
    return image


def make_preview() -> Path:
    """生成端口、独立壳、控制器和轴承的真实UV纹理预览。"""
    samples = [
        ("Inlet outward / west", "inlet_loose_west", (-2.0, 0.7, 0.6)),
        ("Inlet inward / east", "inlet_loose_west", (2.0, 0.7, 0.6)),
        ("Inlet side / below", "inlet_loose_west", (-1.4, -1.3, 2.0)),
        ("Exhaust outward / up", "exhaust_loose_up", (0.7, 2.0, 0.7)),
        ("Loose casing / underside", "turbine_casing_unformed", (1.7, -0.8, 2.1)),
        ("Loose casing / reverse", "turbine_casing_unformed", (0.6, 0.6, 2.1)),
        ("Controller / front", "turbine_controller_unformed", (0.6, 0.6, -2.0)),
        ("Controller / reverse", "turbine_controller_unformed", (0.6, 0.6, 2.1)),
        ("Controller / underside", "turbine_controller_unformed", (1.7, -0.8, 2.1)),
        ("Front bearing / outward", "output_shaft_front", (0.5, 0.5, -2.0)),
        ("Rear bearing / outward", "output_shaft_rear", (0.5, 0.5, 2.0)),
        ("Bearing / side below", "output_shaft_front", (1.8, -1.0, -1.0)),
    ]
    canvas = Image.new("RGB", (1120, 1420), "#17212A")
    draw = ImageDraw.Draw(canvas)
    title_font = ImageFont.load_default(size=24)
    label_font = ImageFont.load_default(size=17)
    small_font = ImageFont.load_default(size=14)
    draw.text((24, 16), "EXT-B-TURBINE-01D / OBJ UV + MTL TEXTURE CHECK", font=title_font, fill="#E3ECE8")
    draw.text((24, 44), "Offline rasterization from installed OBJ faces and referenced PNG textures; not a Minecraft-client pass.",
              font=small_font, fill="#A6B9C2")
    for index, (label, name, eye) in enumerate(samples):
        col, row = index % 3, index // 3
        x0, y0 = 20 + col * 370, 76 + row * 330
        obj = ASSETS / f"models/block/turbine/mesh/{name}.obj"
        image = render_mesh(obj, eye)
        canvas.paste(image.convert("RGB"), (x0, y0))
        draw.rectangle((x0, y0, x0 + 359, y0 + 299), outline="#5B727D", width=1)
        draw.text((x0 + 5, y0 + 304), label, font=label_font, fill="#E3ECE8")
    REPORT.mkdir(parents=True, exist_ok=True)
    path = REPORT / "EXT-B-TURBINE-01D-FINAL-ART-preview.png"
    canvas.save(path, format="PNG", optimize=False)
    return path


def main() -> None:
    generator_path = ROOT / "turbine_models.py"
    spec = importlib.util.spec_from_file_location("turbine_models", generator_path)
    generator = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(generator)
    generated = generator.all_files()
    generated_summary = generator.verify_01b(generated)
    for relative, data in generated.items():
        if ("/models/block/turbine/" in relative or "/blockstates/turbine_" in relative or
                "/models/item/turbine_" in relative or "/textures/block/turbine/" in relative):
            assert (REPO / "src/main/resources" / relative).read_bytes() == data, (relative, "生成源与正式资源不一致")
    resource_summary = audit_resources()
    preview = make_preview()
    report = {"generator_checks": generated_summary, "installed_resource_checks": resource_summary,
              "preview": preview.relative_to(REPO).as_posix(),
              "client_rendering": "NOT VERIFIED; offline image is only static texture and mesh inspection"}
    REPORT.mkdir(parents=True, exist_ok=True)
    (REPORT / "EXT-B-TURBINE-01D-FINAL-ART-checks.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
