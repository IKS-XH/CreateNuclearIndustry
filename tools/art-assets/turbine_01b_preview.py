"""汽轮机01B三档薄壳、叶片和单件网格离线预览器。"""
from __future__ import annotations

import importlib.util
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[1]
MODEL_SCRIPT = ROOT / "turbine_models.py"
REPORT = REPO / "build/reports/extension/EXT-B-TURBINE-01B-ASSETS"
SPEC = importlib.util.spec_from_file_location("turbine_models_01b", MODEL_SCRIPT)
models = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = models
SPEC.loader.exec_module(models)
EXPORTED_FILES = models.all_files()
MESH_CACHE = {}

MATERIAL = {
    "casing": (128, 151, 155, 255), "inside": (83, 103, 108, 255),
    "edge": (83, 103, 108, 255), "endcap": (112, 133, 138, 255),
    "blade": (153, 173, 172, 255), "blade_edge": (104, 126, 130, 255),
    "rotor_hub": (115, 139, 142, 255), "shaft": (87, 107, 111, 255),
    "brass": (205, 160, 83, 255), "glass": (129, 208, 218, 100),
}


def light(color, normal):
    """以固定方向光给网格面着色，保持同一素材导出的预览一致。"""
    length = math.sqrt(sum(c * c for c in normal)) or 1.0
    dot = max(0.0, sum(a * b for a, b in zip(normal, (-0.4, 0.8, -0.5))) / length)
    factor = 0.55 + 0.45 * dot
    return tuple(min(255, int(channel * factor)) for channel in color[:3]) + (color[3],)


def draw_mesh(draw, mesh, origin, camera, scale, cx, cy, alpha_layer=None, face_queue=None):
    """将OBJ同源Mesh面按深度绘出，并用相同局部变换呈现壳体和叶轮。"""
    ox, oy, oz = origin
    faces = []
    for material, indices in mesh.faces:
        pts = [mesh.vertices[v - 1] for v, _, _ in indices]
        normal = mesh.normals[indices[0][2] - 1]
        world = [(p[0] + ox, p[1] + oy, p[2] + oz) for p in pts]
        depth = sum(p[0] * camera[0] + p[1] * camera[1] + p[2] * camera[2] for p in world) / len(world)
        projected = [(cx + scale * (p[2] * 0.86 - p[0] * 0.62),
                      cy + scale * (p[2] * 0.44 + p[0] * 0.44 - p[1] * 0.94)) for p in world]
        base = MATERIAL.get(material, MATERIAL["casing"])
        faces.append((depth, material, projected, light(base, normal)))
    if face_queue is not None:
        for face in faces:
            if face[3][3] < 255 and alpha_layer is not None:
                ImageDraw.Draw(alpha_layer).polygon(face[2], fill=face[3])
            else:
                face_queue.append(face)
        return
    for _, material, pts, color in sorted(faces, key=lambda row: row[0]):
        if color[3] < 255 and alpha_layer is not None:
            ImageDraw.Draw(alpha_layer).polygon(pts, fill=color)
        else:
            draw.polygon(pts, fill=color)


def section_meshes(diameter, length, cutaway=False):
    """从生成资源中的OBJ读取薄壳与叶轮网格构造单台预览。"""
    for z in range(length):
        section = "front" if z == 0 else "rear" if z == length - 1 else "middle"
        if section == "middle":
            cells = models.shell_cells(diameter)
        else:
            r = (diameter - 1) // 2
            cells = [(x, y) for y in range(-r, r + 1) for x in range(-r, r + 1)
                     if models.polygon_area(models.clip_to_cell(models.octagon(diameter), x, y)) > 1e-8]
        for x, y in cells:
            if section != "middle" and x == 0 and y == 0:
                # 轴心方块替代端盖中心格，避免用封闭盖片遮住实际输出轴。
                continue
            if cutaway and section == "middle" and y == (diameter - 1) // 2:
                continue
            mesh = exported_mesh(f"casing_d{diameter}_{section}_x{x}_y{y}")
            yield mesh, (x - 0.5, y - 0.5, z)
        if section == "middle" and not cutaway:
            r = (diameter - 1) // 2
            pane = exported_mesh(f"window_d{diameter}_middle_x0_y{r}")
            yield pane, (-0.5, r - 0.5, z)
        if section == "middle":
            yield exported_mesh(f"rotor_blades_d{diameter}"), (-0.5, -0.5, z)


def exported_mesh(name):
    """从模型生成器本次导出的OBJ字节读取网格，避免预览与资源脱节。"""
    if name not in MESH_CACHE:
        key = f"assets/create_nuclear_industry/models/block/turbine/mesh/{name}.obj"
        mesh = models.Mesh(name, bounds=False)
        for material, points, normal in models.parse_obj(EXPORTED_FILES[key]):
            mesh.face(points, material, normal)
        MESH_CACHE[name] = mesh
    return MESH_CACHE[name]


def external_parts(diameter, length):
    """从实际方块OBJ按机身占格位置放置端轴、控制面板和蒸汽接口。"""
    r = (diameter - 1) // 2
    mid = (length - 1) // 2
    return [
        (exported_mesh("output_shaft_front"), (-0.5, -0.5, 0)),
        (exported_mesh("output_shaft_rear"), (-0.5, -0.5, length - 1)),
        (exported_mesh("controller_right"), (r - 0.5, -0.5, mid - 0.5)),
        (exported_mesh("inlet_left"), (-r - 0.5, -0.5, 0.5)),
        (exported_mesh("exhaust_right"), (r - 0.5, -0.5, length - 2.5)),
    ]


def machine(draw, diameter, length, box, cutaway=False):
    x0, y0, x1, y1 = box
    scale = min((x1 - x0) / (length * 0.86 + diameter * 0.62 + 2),
                (y1 - y0) / (length * 0.44 + diameter * 1.5 + 1))
    cx, cy = (x0 + x1) / 2, y0 + (y1 - y0) * 0.56
    alpha_layer = Image.new("RGBA", draw._image.size, (0, 0, 0, 0))
    face_queue = []
    for mesh, origin in section_meshes(diameter, length, cutaway):
        draw_mesh(draw, mesh, origin, (0.7, 1.0, 0.4), scale, cx, cy, alpha_layer, face_queue)
    if not cutaway:
        for mesh, origin in external_parts(diameter, length):
            draw_mesh(draw, mesh, origin, (0.7, 1.0, 0.4), scale, cx, cy, alpha_layer, face_queue)
    for _, material, pts, color in sorted(face_queue, key=lambda row: row[0]):
        draw.polygon(pts, fill=color, outline=light(MATERIAL.get("edge"), (0, 0, 1)))
    draw._image.alpha_composite(alpha_layer)


def main():
    """导出三档真实网格外观、剖面及独立构件预览。"""
    REPORT.mkdir(parents=True, exist_ok=True)
    canvas = Image.new("RGBA", (2200, 1330), "#17212A")
    draw = ImageDraw.Draw(canvas)
    draw._image = canvas
    title = ImageFont.load_default(size=28)
    label = ImageFont.load_default(size=20)
    small = ImageFont.load_default(size=17)
    draw.text((40, 24), "EXT-B-TURBINE-01B / EXPORTED OBJ GEOMETRY REVIEW", font=title, fill="#E3ECE8")
    draw.text((40, 62), "Flat-color geometry check read from generated OBJ resources; section removes one shell side to expose the static rotor.",
              font=small, fill="#A6B9C2")
    for row, (diameter, length, rotors) in enumerate(((3, 5, 3), (5, 8, 6), (7, 11, 9))):
        top = 118 + row * 395
        draw.text((40, top + 16), f"D{diameter} / {length} axial blocks / {rotors} rotor stages", font=label, fill="#D9E5E2")
        machine(draw, diameter, length, (40, top + 54, 1050, top + 350), False)
        machine(draw, diameter, length, (1120, top + 54, 2130, top + 350), True)
        draw.text((70, top + 320), "Closed shell + continuous inspection window", font=small, fill="#A6B9C2")
        draw.text((1150, top + 320), "Cutaway section / true hollow + spaced blades", font=small, fill="#A6B9C2")
    output = REPORT / "turbine-assets-preview.png"
    canvas.save(output, format="PNG", optimize=False)
    print(f"PASS: true mesh preview generated from shell and rotor functions: {output.relative_to(REPO)}")
    parts_preview()


def parts_preview():
    """把真实OBJ构件按投影包围盒缩放进固定视口，避免重叠和裁切。"""
    canvas = Image.new("RGBA", (1900, 980), "#17212A")
    draw = ImageDraw.Draw(canvas)
    title = ImageFont.load_default(size=27)
    label = ImageFont.load_default(size=19)
    small = ImageFont.load_default(size=16)
    draw.text((36, 22), "EXT-B-TURBINE-01B / EXPORTED OBJ GEOMETRY CHECK", font=title, fill="#E3ECE8")
    draw.text((36, 58), "Flat material colors; geometry is read from exported OBJ resources, while textures are shown in the separate 3x3 sheet.",
              font=small, fill="#A6B9C2")
    centers = [250, 710, 1190, 1650]
    top_cells = [(center - 220, 100, center + 220, 405) for center in centers]
    top_meshes = [exported_mesh("casing_d5_middle_x0_y2"), exported_mesh("rotor_blades_d5"),
                  exported_mesh("rotor_blades_d5"), exported_mesh("window_d5_middle_x0_y2")]
    draw_fitted(draw, top_meshes[0], top_cells[0])
    draw_rotor_face_fitted(draw, top_meshes[1], top_cells[1])
    draw_fitted(draw, top_meshes[2], top_cells[2])
    draw_fitted(draw, top_meshes[3], top_cells[3])
    for center, name in zip(centers, ("Thin shell panel", "12-blade rotor face", "12-blade rotor edge", "Frame + glass")):
        draw.text((center - 115, 420), name, font=small, fill="#A6B9C2")
    bottom_cells = [(center - 220, 540, center + 220, 845) for center in centers]
    parts = [exported_mesh("output_shaft_front"), exported_mesh("controller_right"),
             exported_mesh("inlet_left"), exported_mesh("exhaust_right")]
    for cell, part in zip(bottom_cells, parts):
        draw_fitted(draw, part, cell)
    for center, name in zip(centers, ("Output shaft", "Side controller", "Steam inlet", "Steam exhaust")):
        draw.text((center - 115, 860), name, font=small, fill="#A6B9C2")
    output = REPORT / "turbine-01b-parts-preview.png"
    canvas.save(output, format="PNG", optimize=False)
    print(f"PASS: uncropped parts preview generated: {output.relative_to(REPO)}")


def projected_xy(point):
    x, y, z = point
    return z * 0.86 - x * 0.62, z * 0.44 + x * 0.44 - y * 0.94


def draw_fitted(draw, mesh, cell):
    """按OBJ顶点投影bounds缩放并居中放入固定预览格。"""
    x0, y0, x1, y1 = cell
    projected = [projected_xy(point) for point in mesh.vertices]
    if not projected:
        return
    min_x, max_x = min(p[0] for p in projected), max(p[0] for p in projected)
    min_y, max_y = min(p[1] for p in projected), max(p[1] for p in projected)
    width, height = max_x - min_x, max_y - min_y
    scale = min((x1 - x0 - 24) / max(width, 1e-6), (y1 - y0 - 24) / max(height, 1e-6))
    cx = (x0 + x1) / 2 - scale * (min_x + max_x) / 2
    cy = (y0 + y1) / 2 - scale * (min_y + max_y) / 2
    draw_mesh(draw, mesh, (0, 0, 0), (0.7, 1.0, 0.4), scale, cx, cy)


def draw_rotor_face_fitted(draw, mesh, cell):
    """按实际转子OBJ的XY顶点范围绘制轴向正视。"""
    x0, y0, x1, y1 = cell
    scale = min((x1 - x0 - 30) / 4.4, (y1 - y0 - 30) / 4.4)
    draw_front_rotor(draw, mesh, (x0 + x1) / 2, (y0 + y1) / 2, scale)


def draw_front_rotor(draw, mesh, cx, cy, scale):
    """从12片叶片OBJ的顶面取XY轮廓，作轴向正视以检查弦宽与净隙。"""
    for material, indices in mesh.faces:
        if material != "blade":
            continue
        points = [mesh.vertices[v - 1] for v, _, _ in indices]
        polygon = [(cx + (p[0] - 0.5) * scale, cy - (p[1] - 0.5) * scale) for p in points]
        draw.polygon(polygon, fill=(153, 173, 172, 255), outline=(91, 112, 118, 255))
    draw.ellipse((cx - 0.33 * scale, cy - 0.33 * scale,
                  cx + 0.33 * scale, cy + 0.33 * scale), fill=(115, 139, 142, 255),
                 outline=(205, 160, 83, 255), width=4)
    draw.ellipse((cx - 0.19 * scale, cy - 0.19 * scale,
                  cx + 0.19 * scale, cy + 0.19 * scale), fill="#17212A",
                 outline=(83, 103, 108, 255), width=3)


if __name__ == "__main__":
    main()
