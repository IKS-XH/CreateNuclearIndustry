"""汽轮机静态分格OBJ模型与NeoForge模型状态资源生成器。"""
from __future__ import annotations

import argparse
import itertools
import json
import math
import shutil
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[1]
REPORT = REPO / "build/reports/extension/EXT-B-TURBINE-ASSETS-01"
STAGING = REPORT / "generated_resources"
ASSETS = REPO / "src/main/resources/assets/create_nuclear_industry"
BLOCKS = ("turbine_casing", "turbine_rotor", "turbine_controller", "turbine_output_shaft", "turbine_inlet", "turbine_exhaust")
FACING = ("north", "east", "south", "west")
RING_ROLES = ("top", "upper_left", "upper_right", "left", "right", "lower_left", "lower_right", "bottom")
AXIAL = ("front", "middle", "rear")
OUTWARD = ("north", "east", "south", "west", "up")
ROTATION = {"north": 0, "east": 90, "south": 180, "west": 270}
MODEL_ROOT = "create_nuclear_industry:block/turbine"
OBJ_ROOT = "create_nuclear_industry:models/block/turbine/mesh"


def f(value: float) -> str:
    """格式化OBJ浮点数，避免不必要的精度噪声。"""
    if abs(value) < 1e-9:
        value = 0.0
    if abs(value - 1.0) < 1e-9:
        value = 1.0
    return f"{value:.6f}".rstrip("0").rstrip(".") or "0"


def dot(a, b):
    return sum(x * y for x, y in zip(a, b))


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


class Mesh:
    """本格OBJ网格；顶点单位为方块，限制在0..1。"""

    def __init__(self, name: str):
        self.name = name
        self.vertices: list[tuple[float, float, float]] = []
        self.uvs: list[tuple[float, float]] = []
        self.normals: list[tuple[float, float, float]] = []
        self.faces: list[tuple[str, list[tuple[int, int, int]]]] = []

    def face(self, points, material: str, target_normal=None, uv_axis=None):
        points = [tuple(map(float, p)) for p in points]
        if len(points) < 3:
            raise ValueError("OBJ面至少需要三个顶点")
        if any(any(c < -1e-8 or c > 1.00000001 for c in p) for p in points):
            raise ValueError(f"{self.name}: 顶点越出单格0..1范围: {points}")
        if len(points) > 4:
            for i in range(1, len(points) - 1):
                self.face([points[0], points[i], points[i + 1]], material, target_normal, uv_axis)
            return
        normal = cross(tuple(points[1][i] - points[0][i] for i in range(3)),
                       tuple(points[2][i] - points[0][i] for i in range(3)))
        length = math.sqrt(dot(normal, normal))
        if length < 1e-9:
            raise ValueError(f"{self.name}: 零面积网格面")
        normal = tuple(c / length for c in normal)
        if target_normal is not None and dot(normal, target_normal) < 0:
            points.reverse()
            normal = tuple(-c for c in normal)
        if uv_axis is None:
            dominant = max(range(3), key=lambda i: abs(normal[i]))
            uv_axis = {0: (2, 1), 1: (0, 2), 2: (0, 1)}[dominant]
        us = [p[uv_axis[0]] for p in points]
        vs = [p[uv_axis[1]] for p in points]
        u0, u1 = min(us), max(us)
        v0, v1 = min(vs), max(vs)
        du, dv = max(u1 - u0, 1e-6), max(v1 - v0, 1e-6)
        ids = []
        for point in points:
            self.vertices.append(point)
            self.uvs.append(((point[uv_axis[0]] - u0) / du, (point[uv_axis[1]] - v0) / dv))
            self.normals.append(normal)
            idx = len(self.vertices)
            ids.append((idx, idx, idx))
        self.faces.append((material, ids))

    def quads_for_box(self, low, high, side="casing", cap_front=None, cap_back=None):
        x0, y0, z0 = low
        x1, y1, z1 = high
        faces = [
            ([(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)], side, (-1, 0, 0)),
            ([(x1, y0, z0), (x1, y1, z0), (x1, y1, z1), (x1, y0, z1)], side, (1, 0, 0)),
            ([(x0, y1, z0), (x0, y1, z1), (x1, y1, z1), (x1, y1, z0)], side, (0, 1, 0)),
            ([(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)], side, (0, -1, 0)),
        ]
        for points, material, normal in faces:
            self.face(points, material, normal)
        if cap_front:
            self.face([(x0, y0, z0), (x0, y1, z0), (x1, y1, z0), (x1, y0, z0)], cap_front, (0, 0, -1))
        if cap_back:
            self.face([(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)], cap_back, (0, 0, 1))

    def text(self) -> str:
        lines = [f"mtllib turbine.mtl", f"o {self.name}"]
        for x, y, z in self.vertices:
            lines.append(f"v {f(x)} {f(y)} {f(z)}")
        for u, v in self.uvs:
            lines.append(f"vt {f(u)} {f(v)}")
        for x, y, z in self.normals:
            lines.append(f"vn {f(x)} {f(y)} {f(z)}")
        current = None
        for material, ids in self.faces:
            if current != material:
                lines.append(f"usemtl {material}")
                current = material
            lines.append("f " + " ".join(f"{v}/{t}/{n}" for v, t, n in ids))
        return "\n".join(lines) + "\n"


def quad(mesh, points, material, normal, uv_axis=None):
    mesh.face(points, material, normal, uv_axis)


def support_box(mesh: Mesh, x0: float, x1: float, axial: str):
    """绘制本格内宽0.20、高3/16的承重脚，沿单格轴向贯通。"""
    y0, y1 = 0.0, 0.1875
    faces = [
        ([(x0, y0, 0), (x0, y0, 1), (x0, y1, 1), (x0, y1, 0)], (-1, 0, 0)),
        ([(x1, y0, 0), (x1, y1, 0), (x1, y1, 1), (x1, y0, 1)], (1, 0, 0)),
        ([(x0, y0, 0), (x1, y0, 0), (x1, y0, 1), (x0, y0, 1)], (0, -1, 0)),
    ]
    for points, normal in faces:
        quad(mesh, points, "support", normal)
    if axial == "front":
        quad(mesh, [(x0, y0, 0), (x0, y1, 0), (x1, y1, 0), (x1, y0, 0)], "support", (0, 0, -1))
    elif axial == "rear":
        quad(mesh, [(x0, y0, 1), (x1, y0, 1), (x1, y1, 1), (x0, y1, 1)], "support", (0, 0, 1))


def cap_polygon(mesh: Mesh, role: str, z: float, material: str, front: bool):
    polygons = {
        "top": [(0, 0), (1, 0), (1, 1), (0, 1)],
        "bottom": [(0, 0.1875), (1, 0.1875), (1, 1), (0, 1)],
        "left": [(0, 0), (1, 0), (1, 1), (0, 1)],
        "right": [(0, 0), (1, 0), (1, 1), (0, 1)],
        "upper_left": [(0, 0), (1, 0), (1, 1), (0.5, 1), (0, 0.5)],
        "upper_right": [(0, 0), (1, 0), (1, 0.5), (0.5, 1), (0, 1)],
        "lower_left": [(0, 0.5), (0.3125, 0.1875), (1, 0.1875), (1, 1), (0, 1)],
        "lower_right": [(0, 0.1875), (0.6875, 0.1875), (1, 0.5), (1, 1), (0, 1)],
    }
    points = [(x, y, z) for x, y in polygons[role]]
    mesh.face(points, material, (0, 0, -1 if front else 1), uv_axis=(0, 1))


def casing_mesh(axial: str, role: str) -> Mesh:
    mesh = Mesh(f"casing_{axial}_{role}")
    if role == "top":
        quad(mesh, [(0, 1, 0), (0, 1, 1), (1, 1, 1), (1, 1, 0)], "casing", (0, 1, 0))
    elif role == "bottom":
        quad(mesh, [(0, 0.1875, 0), (1, 0.1875, 0), (1, 0.1875, 1), (0, 0.1875, 1)], "casing", (0, -1, 0))
    elif role == "left":
        quad(mesh, [(0, 0, 0), (0, 0, 1), (0, 1, 1), (0, 1, 0)], "casing", (-1, 0, 0))
    elif role == "right":
        quad(mesh, [(1, 0, 0), (1, 1, 0), (1, 1, 1), (1, 0, 1)], "casing", (1, 0, 0))
    elif role in ("upper_left", "upper_right"):
        diagonal = {
            "upper_left": ((0, 0.5), (0.5, 1), (-1, 1, 0)),
            "upper_right": ((0.5, 1), (1, 0.5), (1, 1, 0)),
        }
        (a, b, normal) = diagonal[role]
        # 真实45度斜切面沿轴向贯穿单格，不用透明贴图遮盖立方体角。
        quad(mesh, [(a[0], a[1], 0), (a[0], a[1], 1), (b[0], b[1], 1), (b[0], b[1], 0)], "casing", normal)
        if role == "upper_left":
            quad(mesh, [(0, 0, 0), (0, 0, 1), (0, 0.5, 1), (0, 0.5, 0)], "casing", (-1, 0, 0))
            quad(mesh, [(0.5, 1, 0), (0.5, 1, 1), (1, 1, 1), (1, 1, 0)], "casing", (0, 1, 0))
        elif role == "upper_right":
            quad(mesh, [(0, 1, 0), (0, 1, 1), (0.5, 1, 1), (0.5, 1, 0)], "casing", (0, 1, 0))
            quad(mesh, [(1, 0, 0), (1, 0, 1), (1, 0.5, 1), (1, 0.5, 0)], "casing", (1, 0, 0))
    elif role == "lower_left":
        quad(mesh, [(0, 0.5, 0), (0, 0.5, 1), (0, 1, 1), (0, 1, 0)], "casing", (-1, 0, 0))
        # 主体底边抬高至3/16格，左下斜角随之从8/16高处接至主体底边。
        quad(mesh, [(0, 0.5, 0), (0, 0.5, 1), (0.3125, 0.1875, 1), (0.3125, 0.1875, 0)], "casing", (-1, -1, 0))
        quad(mesh, [(0.3125, 0.1875, 0), (0.3125, 0.1875, 1), (1, 0.1875, 1), (1, 0.1875, 0)], "casing", (0, -1, 0))
        support_box(mesh, 0.65, 0.85, axial)
    else:
        quad(mesh, [(1, 0.5, 0), (1, 0.5, 1), (1, 1, 1), (1, 1, 0)], "casing", (1, 0, 0))
        # 右下斜角镜像处理；底边为3/16格。
        quad(mesh, [(0, 0.1875, 0), (0, 0.1875, 1), (0.6875, 0.1875, 1), (0.6875, 0.1875, 0)], "casing", (0, -1, 0))
        quad(mesh, [(0.6875, 0.1875, 0), (0.6875, 0.1875, 1), (1, 0.5, 1), (1, 0.5, 0)], "casing", (1, -1, 0))
        support_box(mesh, 0.15, 0.35, axial)
    if axial == "front":
        cap_polygon(mesh, role, 0, "endcap", True)
    elif axial == "rear":
        cap_polygon(mesh, role, 1, "endcap", False)
    return mesh


def cylinder(mesh: Mesh, axis: str, center, radius: float, start: float, end: float,
             material: str, sides=16, caps=True, cap_material=None):
    """沿单格主轴绘制闭合圆柱；分段截面完全位于本方块。"""
    if axis == "z":
        points0 = [(center[0] + radius * math.cos(2 * math.pi * i / sides),
                    center[1] + radius * math.sin(2 * math.pi * i / sides), start) for i in range(sides)]
        points1 = [(x, y, end) for x, y, _ in points0]
        normal_fn = lambda p: (p[0] - center[0], p[1] - center[1], 0)
        cap_normals = ((0, 0, -1), (0, 0, 1))
        cap_uv_axis = (0, 1)
    elif axis == "x":
        points0 = [(start, center[0] + radius * math.cos(2 * math.pi * i / sides), center[1] + radius * math.sin(2 * math.pi * i / sides)) for i in range(sides)]
        points1 = [(end, y, z) for _, y, z in points0]
        normal_fn = lambda p: (0, p[1] - center[0], p[2] - center[1])
        cap_normals = ((-1, 0, 0), (1, 0, 0))
        cap_uv_axis = (1, 2)
    else:
        points0 = [(center[0] + radius * math.cos(2 * math.pi * i / sides), start, center[1] + radius * math.sin(2 * math.pi * i / sides)) for i in range(sides)]
        points1 = [(x, end, z) for x, _, z in points0]
        normal_fn = lambda p: (p[0] - center[0], 0, p[2] - center[1])
        cap_normals = ((0, -1, 0), (0, 1, 0))
        cap_uv_axis = (0, 2)
    for i in range(sides):
        j = (i + 1) % sides
        p0, p1, q1, q0 = points0[i], points0[j], points1[j], points1[i]
        raw = cross(tuple(p1[k] - p0[k] for k in range(3)), tuple(q1[k] - p0[k] for k in range(3)))
        mid = tuple((p0[k] + p1[k]) / 2 for k in range(3))
        desired = normal_fn(mid)
        mesh.face([p0, p1, q1, q0], material, desired)
    if caps:
        mesh.face(points0, cap_material or material, cap_normals[0], uv_axis=cap_uv_axis)
        mesh.face(points1, cap_material or material, cap_normals[1], uv_axis=cap_uv_axis)


def disk(mesh: Mesh, axis: str, center, radius: float, plane: float, material: str,
         normal_sign: int, sides=16):
    """绘制独立圆盘端面，避免以零长度圆柱生成退化侧面。"""
    points = []
    for i in range(sides):
        angle = 2 * math.pi * i / sides
        if axis == "x":
            points.append((plane, center[0] + radius * math.cos(angle), center[1] + radius * math.sin(angle)))
        elif axis == "y":
            points.append((center[0] + radius * math.cos(angle), plane, center[1] + radius * math.sin(angle)))
        else:
            points.append((center[0] + radius * math.cos(angle), center[1] + radius * math.sin(angle), plane))
    normal = {"x": (normal_sign, 0, 0), "y": (0, normal_sign, 0), "z": (0, 0, normal_sign)}[axis]
    mesh.face(points, material, normal)


def rotor_mesh(name="rotor_middle") -> Mesh:
    mesh = Mesh(name)
    cylinder(mesh, "z", (0.5, 0.5), 0.42, 0.04, 0.96, "rotor", sides=16, caps=True)
    return mesh


def controller_mesh(name="controller_front") -> Mesh:
    mesh = Mesh(name)
    # 前端薄面板封住轴心开口，后方控制器壳体与面板边缘相接但不共面。
    mesh.quads_for_box((0, 0, 0), (1, 1, 0.08), "endcap", cap_front="controller")
    mesh.quads_for_box((0.12, 0.12, 0.08), (0.88, 0.88, 0.40),
                       "casing", cap_back="casing")
    return mesh


def shaft_mesh(name="output_shaft_rear") -> Mesh:
    mesh = Mesh(name)
    cylinder(mesh, "z", (0.5, 0.5), 0.19, 0.10, 0.34, "shaft", sides=12, caps=False)
    cylinder(mesh, "z", (0.5, 0.5), 0.19, 0.34, 0.43, "support", sides=12, caps=False)
    cylinder(mesh, "z", (0.5, 0.5), 0.19, 0.43, 0.70, "shaft", sides=12, caps=False)
    cylinder(mesh, "z", (0.5, 0.5), 0.19, 0.70, 0.79, "support", sides=12, caps=False)
    cylinder(mesh, "z", (0.5, 0.5), 0.19, 0.79, 1.0, "shaft", sides=12, caps=False)
    disk(mesh, "z", (0.5, 0.5), 0.19, 1.0, "shaft", 1, sides=12)
    square_hole_plate(mesh, "z", 0.88, 1.0, 0.25, "endcap", "casing")
    return mesh


def point_on_plane(axis: str, plane: float, u: float, v: float):
    if axis == "x":
        return (plane, u, v)
    if axis == "y":
        return (u, plane, v)
    return (u, v, plane)


def axis_normal(axis: str, sign: int):
    return {"x": (sign, 0, 0), "y": (0, sign, 0), "z": (0, 0, sign)}[axis]


def square_hole_plate(mesh: Mesh, axis: str, start: float, end: float, half: float,
                      face_material: str, wall_material: str):
    """制作带方形轴孔的薄端板；轴孔周围有足够净空且不覆盖转轴。"""
    low, high = 0.5 - half, 0.5 + half
    strips = [
        [(0, 0), (1, 0), (1, low), (0, low)],
        [(0, high), (1, high), (1, 1), (0, 1)],
        [(0, low), (low, low), (low, high), (0, high)],
        [(high, low), (1, low), (1, high), (high, high)],
    ]
    for plane, sign in ((start, -1), (end, 1)):
        for strip in strips:
            quad(mesh, [point_on_plane(axis, plane, u, v) for u, v in strip],
                 face_material, axis_normal(axis, sign))
    # 面板外缘和方孔内壁把两层端面连成实体薄板。
    for fixed, is_u, normal in (
        (0.0, True, -1), (1.0, True, 1), (0.0, False, -1), (1.0, False, 1),
    ):
        if is_u:
            points = [point_on_plane(axis, start, fixed, 0), point_on_plane(axis, end, fixed, 0),
                      point_on_plane(axis, end, fixed, 1), point_on_plane(axis, start, fixed, 1)]
            vector_axis = "y" if axis == "x" else "x"
            sign = normal
        else:
            points = [point_on_plane(axis, start, 0, fixed), point_on_plane(axis, start, 1, fixed),
                      point_on_plane(axis, end, 1, fixed), point_on_plane(axis, end, 0, fixed)]
            vector_axis = "y" if axis == "z" else "z"
            sign = normal
        quad(mesh, points, wall_material, axis_normal(vector_axis, sign))
    for fixed, is_u, sign in (
        (low, True, 1), (high, True, -1), (low, False, 1), (high, False, -1),
    ):
        if is_u:
            points = [point_on_plane(axis, start, fixed, low), point_on_plane(axis, end, fixed, low),
                      point_on_plane(axis, end, fixed, high), point_on_plane(axis, start, fixed, high)]
            vector_axis = "y" if axis == "x" else "x"
        else:
            points = [point_on_plane(axis, start, low, fixed), point_on_plane(axis, start, high, fixed),
                      point_on_plane(axis, end, high, fixed), point_on_plane(axis, end, low, fixed)]
            vector_axis = "y" if axis == "z" else "z"
        quad(mesh, points, wall_material, axis_normal(vector_axis, sign))


def port_mesh(kind: str, role: str, name: str) -> Mesh:
    mesh = Mesh(name)
    if role == "left":
        axis, center, start, end = "x", (0.5, 0.5), 0.0, 1.0
        band_start, band_end, inboard = 0.0, 0.11, (0.92, 1.0)
        end_sign = -1
    elif role == "right":
        axis, center, start, end = "x", (0.5, 0.5), 0.0, 1.0
        band_start, band_end, inboard = 0.89, 1.0, (0.0, 0.08)
        end_sign = 1
    else:
        axis, center, start, end = "y", (0.5, 0.5), 0.0, 1.0
        band_start, band_end, inboard = 0.89, 1.0, (0.0, 0.08)
        end_sign = 1
    cap = "inlet" if kind == "inlet" else "exhaust"
    # 钢管主体分成标识环与管身两个不重叠区间，端面颜色表示介质方向。
    if role == "left":
        cylinder(mesh, axis, center, 0.32, band_end, end, "casing", sides=16, caps=False)
        cylinder(mesh, axis, center, 0.32, start, band_end, cap, sides=16, caps=False)
        disk(mesh, axis, center, 0.32, start, cap, end_sign)
    else:
        cylinder(mesh, axis, center, 0.32, start, band_start, "casing", sides=16, caps=False)
        cylinder(mesh, axis, center, 0.32, band_start, end, cap, sides=16, caps=False)
        disk(mesh, axis, center, 0.32, end, cap, end_sign)
    square_hole_plate(mesh, axis, inboard[0], inboard[1], 0.35, "endcap", "casing")
    return mesh


def cube_mesh(name: str, side="casing", front="endcap", back="endcap") -> Mesh:
    mesh = Mesh(name)
    mesh.quads_for_box((0, 0, 0), (1, 1, 1), side, cap_front=front, cap_back=back)
    return mesh


def wrapper(model_name: str) -> dict:
    if "casing" in model_name:
        particle = "turbine_casing"
    elif "rotor" in model_name:
        particle = "turbine_rotor"
    elif "controller" in model_name:
        particle = "turbine_controller"
    elif "output_shaft" in model_name:
        particle = "turbine_output_shaft"
    elif "inlet" in model_name:
        particle = "turbine_inlet"
    elif "exhaust" in model_name:
        particle = "turbine_exhaust"
    else:
        raise ValueError(f"缺少OBJ粒子材质映射: {model_name}")
    return {
        "loader": "neoforge:obj",
        "model": f"{OBJ_ROOT}/{model_name}.obj",
        "textures": {"particle": f"create_nuclear_industry:block/turbine/{particle}"},
        "automatic_culling": True,
        "shade_quads": True,
        "flip_v": False,
        "emissive_ambient": False,
    }


def write_json(path: Path, value) -> bytes:
    return (json.dumps(value, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def mesh_files() -> dict[str, bytes]:
    meshes = {}
    for axial, role in itertools.product(AXIAL, RING_ROLES):
        name = f"casing_{axial}_{role}"
        meshes[name] = casing_mesh(axial, role)
    meshes["rotor_middle"] = rotor_mesh()
    meshes["controller_front"] = controller_mesh()
    meshes["output_shaft_rear"] = shaft_mesh()
    for kind, role in itertools.product(("inlet", "exhaust"), ("left", "right", "top")):
        name = f"{kind}_{role}"
        meshes[name] = port_mesh(kind, role, name)
    meshes["turbine_casing_unformed"] = cube_mesh("turbine_casing_unformed", side="casing", front="endcap", back="endcap")
    meshes["turbine_rotor_unformed"] = rotor_mesh("turbine_rotor_unformed")
    meshes["turbine_controller_unformed"] = controller_mesh("turbine_controller_unformed")
    meshes["turbine_output_shaft_unformed"] = shaft_mesh("turbine_output_shaft_unformed")
    meshes["turbine_inlet_unformed"] = port_mesh("inlet", "left", "turbine_inlet_unformed")
    meshes["turbine_exhaust_unformed"] = port_mesh("exhaust", "left", "turbine_exhaust_unformed")
    out = {}
    for name, mesh in meshes.items():
        out[f"assets/create_nuclear_industry/models/block/turbine/mesh/{name}.obj"] = mesh.text().encode("utf-8")
        out[f"assets/create_nuclear_industry/models/block/turbine/{name}.json"] = write_json(
            Path(name), wrapper(name))
    materials = {
        "casing": "turbine_casing", "rotor": "turbine_rotor", "controller": "turbine_controller",
        "shaft": "turbine_output_shaft", "support": "turbine_bearing_support",
        "endcap": "turbine_casing_endcap", "inlet": "turbine_inlet", "exhaust": "turbine_exhaust",
    }
    mtl = []
    for material, texture in materials.items():
        mtl.extend([f"newmtl {material}", "Ka 0.2 0.2 0.2", "Kd 1 1 1",
                    f"map_Kd create_nuclear_industry:block/turbine/{texture}", ""])
    out["assets/create_nuclear_industry/models/block/turbine/mesh/turbine.mtl"] = "\n".join(mtl).encode("utf-8")
    return out


def variant(model: str, facing: str | None = None) -> dict:
    result = {"model": model}
    if facing:
        result["y"] = ROTATION[facing]
        result["uvlock"] = True
    return result


def blockstates() -> dict[str, bytes]:
    out = {}
    for block in BLOCKS:
        variants = {}
        if block == "turbine_casing":
            for formed, facing, role, axial in itertools.product((False, True), FACING, RING_ROLES, AXIAL):
                key = f"formed={str(formed).lower()},machine_facing={facing},ring_role={role},axial_role={axial}"
                model = (f"{MODEL_ROOT}/{f'casing_{axial}_{role}'}" if formed else f"{MODEL_ROOT}/turbine_casing_unformed")
                variants[key] = variant(model, facing)
        elif block in ("turbine_rotor", "turbine_controller", "turbine_output_shaft"):
            formed_model = {"turbine_rotor": "rotor_middle", "turbine_controller": "controller_front",
                            "turbine_output_shaft": "output_shaft_rear"}[block]
            for formed, facing in itertools.product((False, True), FACING):
                key = f"formed={str(formed).lower()},machine_facing={facing}"
                model = f"{MODEL_ROOT}/{formed_model if formed else block + '_unformed'}"
                variants[key] = variant(model, facing)
        else:
            kind = "inlet" if block == "turbine_inlet" else "exhaust"
            for formed, facing, role, outward in itertools.product((False, True), FACING, RING_ROLES, OUTWARD):
                key = f"formed={str(formed).lower()},machine_facing={facing},ring_role={role},outward={outward}"
                if not formed:
                    model = f"{MODEL_ROOT}/{block}_unformed"
                elif role in ("left", "right", "top"):
                    model = f"{MODEL_ROOT}/{kind}_{role}"
                else:
                    model = f"{MODEL_ROOT}/{block}_unformed"
                variants[key] = variant(model, facing)
        out[f"assets/create_nuclear_industry/blockstates/{block}.json"] = write_json(Path(block), {"variants": variants})
    return out


def item_models() -> dict[str, bytes]:
    out = {}
    display = {
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
        "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
        "head": {"rotation": [0, 0, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
        "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.25, 0.25, 0.25]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    }
    for block in BLOCKS:
        out[f"assets/create_nuclear_industry/models/item/{block}.json"] = write_json(Path(block), {
            "parent": f"{MODEL_ROOT}/{block}_unformed", "display": display,
        })
    return out


def all_files() -> dict[str, bytes]:
    result = mesh_files()
    result.update(blockstates())
    result.update(item_models())
    return result


def verify(files: dict[str, bytes]) -> dict:
    models = [key for key in files if key.endswith(".json") and "/models/block/turbine/" in key]
    obj_files = [key for key in files if key.endswith(".obj")]
    states = [key for key in files if "/blockstates/" in key]
    items = [key for key in files if "/models/item/" in key]
    assert len(models) == 39, len(models)
    assert len(obj_files) == 39, len(obj_files)
    assert len(states) == 6 and len(items) == 6
    state_counts = {}
    model_names = {Path(path).stem for path in models}
    obj_names = {Path(path).stem for path in obj_files}
    for path, data in files.items():
        if path.endswith(".json"):
            value = json.loads(data.decode("utf-8"))
            if "/models/block/turbine/" in path:
                assert value.get("loader") == "neoforge:obj" and value.get("textures", {}).get("particle"), path
                obj_location = value["model"].split(":", 1)[1]
                assert Path(obj_location).stem in obj_names, (path, obj_location)
                particle_path = REPO / "src/main/resources/assets/create_nuclear_industry/textures" / (value["textures"]["particle"].split(":", 1)[1] + ".png")
                assert particle_path.is_file(), (path, particle_path)
        elif path.endswith(".obj"):
            text = data.decode("utf-8")
            vertices = [tuple(float(x) for x in row.split()[1:4]) for row in text.splitlines() if row.startswith("v ")]
            assert vertices and all(all(-1e-9 <= v <= 1.000000001 for v in vertex) for vertex in vertices), path
            assert all("/" in part for row in text.splitlines() if row.startswith("f ") for part in row.split()[1:]), path
    for path in states:
        data = json.loads(files[path])
        state_counts[Path(path).stem] = len(data["variants"])
        for variant_data in data["variants"].values():
            model = variant_data["model"].split(":", 1)[1].split("/", 3)[-1]
            assert model in model_names, (path, variant_data["model"])
    assert state_counts["turbine_casing"] == 192
    assert state_counts["turbine_inlet"] == 320 and state_counts["turbine_exhaust"] == 320
    for part in ("turbine_rotor", "turbine_controller", "turbine_output_shaft"):
        assert state_counts[part] == 8
    required_display = {"thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand",
                        "firstperson_lefthand", "head", "gui", "ground", "fixed"}
    for path in items:
        value = json.loads(files[path])
        assert required_display.issubset(value.get("display", {})), path
    return {"json_model_count": len(models), "obj_mesh_count": len(obj_files), "blockstate_count": len(states),
            "item_model_count": len(items), "variants_per_block": state_counts,
            "obj_vertex_bounds": "0..1 block units", "casing_meshes": 24,
            "endcap_axial_faces": "front z=0; rear z=1; middle has none"}


def parse_obj(data: bytes):
    vertices, normals, faces, material = [], [], [], "casing"
    for row in data.decode("utf-8").splitlines():
        parts = row.split()
        if not parts:
            continue
        if parts[0] == "v":
            vertices.append(tuple(float(x) for x in parts[1:4]))
        elif parts[0] == "vn":
            normals.append(tuple(float(x) for x in parts[1:4]))
        elif parts[0] == "usemtl":
            material = parts[1]
        elif parts[0] == "f":
            indexes = [tuple(int(value) for value in token.split("/")) for token in parts[1:]]
            faces.append((material, [vertices[v - 1] for v, _, _ in indexes], normals[indexes[0][2] - 1]))
    return faces


def mesh_preview(files: dict[str, bytes]) -> Path:
    """从已生成OBJ面直接拼接三档结构，供资源接口离线检查。"""
    ring_cells = {
        "upper_left": (0, 2), "top": (1, 2), "upper_right": (2, 2),
        "left": (0, 1), "right": (2, 1),
        "lower_left": (0, 0), "bottom": (1, 0), "lower_right": (2, 0),
    }
    material_colors = {
        "casing": (113, 132, 138), "rotor": (82, 102, 108), "controller": (183, 205, 204),
        "shaft": (91, 113, 119), "support": (168, 121, 66), "endcap": (83, 104, 111),
        "inlet": (240, 138, 54), "exhaust": (133, 205, 224),
    }
    mesh_cache = {}

    def get_mesh(name):
        if name not in mesh_cache:
            key = f"assets/create_nuclear_industry/models/block/turbine/mesh/{name}.obj"
            mesh_cache[name] = parse_obj(files[key])
        return mesh_cache[name]

    canvas = Image.new("RGBA", (1500, 1280), "#17212A")
    draw = ImageDraw.Draw(canvas)
    title = ImageFont.load_default(size=20)
    label = ImageFont.load_default(size=16)
    draw.text((30, 22), "EXT-B-TURBINE-ASSETS-01 / MESH ASSEMBLY PREVIEW", font=title, fill="#E3ECE8")
    draw.text((30, 50), "Rendered from generated OBJ faces; offline geometry review, not Minecraft-client evidence.",
              font=label, fill="#A6B9C2")
    scale, center_x = 34.0, 770.0
    for row, length in enumerate((5, 8, 11)):
        scene = []

        def add(name, ox, oy, oz):
            for material, points, normal in get_mesh(name):
                world = [(x + ox, y + oy, z + oz) for x, y, z in points]
                depth = sum(-0.66 * p[0] + 0.35 * p[1] - 0.66 * p[2] for p in world) / len(world)
                scene.append((depth, material, world, normal))

        for z in range(length):
            axial = "front" if z == 0 else "rear" if z == length - 1 else "middle"
            for role, (cx, cy) in ring_cells.items():
                if z == 1 and role == "left":
                    name = "inlet_left"
                elif z == length - 2 and role == "right":
                    name = "exhaust_right"
                else:
                    name = f"casing_{axial}_{role}"
                add(name, cx, cy, z)
            add("controller_front" if z == 0 else "output_shaft_rear" if z == length - 1 else "rotor_middle",
                1, 1, z)
        row_top = 90 + row * 390
        draw.text((32, row_top + 155), f"{length - 2} rotors / L={length}", font=title, fill="#E3ECE8")
        faces = sorted((face for face in scene
                        if -0.66 * face[3][0] + 0.35 * face[3][1] - 0.66 * face[3][2] > 0.015),
                       key=lambda face: face[0])
        for _, material, points, normal in faces:
            projected = [(center_x + (x - z) * 0.7071068 * scale,
                          row_top + 280 - (0.247 * x + 0.933 * y + 0.247 * z) * scale) for x, y, z in points]
            color = material_colors.get(material, (140, 140, 140))
            light = max(0.68, min(1.1, 0.9 + 0.1 * (normal[1]) + 0.05 * (normal[0] - normal[2])))
            shaded = tuple(max(0, min(255, int(channel * light))) for channel in color)
            draw.polygon(projected, fill=shaded, outline="#243138")
    path = REPORT / "turbine-model-mesh-preview.png"
    REPORT.mkdir(parents=True, exist_ok=True)
    canvas.save(path, format="PNG", optimize=False)
    return path


def write_files(files: dict[str, bytes], target_root: Path) -> None:
    for relative, data in files.items():
        path = target_root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)


def main() -> None:
    parser = argparse.ArgumentParser(description="生成汽轮机静态分格OBJ、JSON模型和blockstates")
    parser.add_argument("--install", action="store_true", help="将已校验资源写入src/main/resources")
    args = parser.parse_args()
    files = all_files()
    summary = verify(files)
    preview_path = mesh_preview(files)
    write_files(files, STAGING)
    if args.install:
        write_files(files, REPO / "src/main/resources")
    summary.update({"install": bool(args.install), "staged_root": STAGING.relative_to(REPO).as_posix(),
                    "runtime_root": "src/main/resources", "mesh_preview": preview_path.relative_to(REPO).as_posix(),
                    "status": "PASS"})
    REPORT.mkdir(parents=True, exist_ok=True)
    (REPORT / "model-generation.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=True, indent=2))


if __name__ == "__main__":
    main()
