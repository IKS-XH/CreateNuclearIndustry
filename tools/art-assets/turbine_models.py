"""汽轮机静态分格OBJ模型与NeoForge模型状态资源生成器。"""
from __future__ import annotations

import argparse
import io
import itertools
import json
import math
import shutil
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[1]
REPORT = REPO / "build/reports/extension/EXT-B-TURBINE-01D-FINAL-ART"
STAGING = REPORT / "generated_resources"
ASSETS = REPO / "src/main/resources/assets/create_nuclear_industry"
BLOCKS = ("turbine_casing", "turbine_rotor", "turbine_controller", "turbine_output_shaft", "turbine_inlet", "turbine_exhaust")
FACING = ("north", "east", "south", "west")
RING_ROLES = ("top", "upper_left", "upper_right", "left", "right", "lower_left", "lower_right", "bottom")
AXIAL = ("front", "middle", "rear")
OUTWARD = ("north", "east", "south", "west", "up", "down")
ROTATION = {"north": 0, "east": 90, "south": 180, "west": 270}
SIDE_WORLD = {
    "north": {"left": "west", "right": "east"},
    "east": {"left": "north", "right": "south"},
    "south": {"left": "east", "right": "west"},
    "west": {"left": "south", "right": "north"},
}
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

    def __init__(self, name: str, bounds: bool = True):
        self.name = name
        self.bounds = bounds
        self.vertices: list[tuple[float, float, float]] = []
        self.uvs: list[tuple[float, float]] = []
        self.normals: list[tuple[float, float, float]] = []
        self.faces: list[tuple[str, list[tuple[int, int, int]]]] = []

    def face(self, points, material: str, target_normal=None, uv_axis=None, uv_coords=None):
        points = [tuple(map(float, p)) for p in points]
        if len(points) < 3:
            raise ValueError("OBJ面至少需要三个顶点")
        if self.bounds and any(any(c < -1e-8 or c > 1.00000001 for c in p) for p in points):
            raise ValueError(f"{self.name}: 顶点越出单格0..1范围: {points}")
        if uv_coords is not None and len(uv_coords) != len(points):
            raise ValueError(f"{self.name}: UV数量必须与面顶点数量一致")
        if len(points) > 4:
            normal = cross(tuple(points[1][i] - points[0][i] for i in range(3)),
                           tuple(points[2][i] - points[0][i] for i in range(3)))
            length = math.sqrt(dot(normal, normal))
            if length < 1e-9:
                raise ValueError(f"{self.name}: 零面积网格面")
            normal = tuple(c / length for c in normal)
            if target_normal is not None and dot(normal, target_normal) < 0:
                points.reverse()
                if uv_coords is not None:
                    uv_coords = list(reversed(uv_coords))
                normal = tuple(-c for c in normal)
            if uv_coords is None:
                if uv_axis is None:
                    dominant = max(range(3), key=lambda i: abs(normal[i]))
                    uv_axis = {0: (2, 1), 1: (0, 2), 2: (0, 1)}[dominant]
                us = [p[uv_axis[0]] for p in points]
                vs = [p[uv_axis[1]] for p in points]
                u0, u1 = min(us), max(us)
                v0, v1 = min(vs), max(vs)
                du, dv = max(u1 - u0, 1e-6), max(v1 - v0, 1e-6)
                uv_coords = [((p[uv_axis[0]] - u0) / du, (p[uv_axis[1]] - v0) / dv) for p in points]
            for i in range(1, len(points) - 1):
                tri_points = [points[0], points[i], points[i + 1]]
                tri_uvs = [uv_coords[0], uv_coords[i], uv_coords[i + 1]]
                self._emit_face(tri_points, material, target_normal, tri_uvs)
            return
        normal = cross(tuple(points[1][i] - points[0][i] for i in range(3)),
                       tuple(points[2][i] - points[0][i] for i in range(3)))
        length = math.sqrt(dot(normal, normal))
        if length < 1e-9:
            raise ValueError(f"{self.name}: 零面积网格面")
        normal = tuple(c / length for c in normal)
        if target_normal is not None and dot(normal, target_normal) < 0:
            points.reverse()
            if uv_coords is not None:
                uv_coords = list(reversed(uv_coords))
            normal = tuple(-c for c in normal)
        if uv_axis is None:
            dominant = max(range(3), key=lambda i: abs(normal[i]))
            uv_axis = {0: (2, 1), 1: (0, 2), 2: (0, 1)}[dominant]
        if uv_coords is None:
            us = [p[uv_axis[0]] for p in points]
            vs = [p[uv_axis[1]] for p in points]
            u0, u1 = min(us), max(us)
            v0, v1 = min(vs), max(vs)
            du, dv = max(u1 - u0, 1e-6), max(v1 - v0, 1e-6)
            uv_coords = [((p[uv_axis[0]] - u0) / du, (p[uv_axis[1]] - v0) / dv) for p in points]
        self._emit_face(points, material, target_normal, uv_coords)

    def _emit_face(self, points, material, target_normal, uv_coords):
        normal = cross(tuple(points[1][i] - points[0][i] for i in range(3)),
                       tuple(points[2][i] - points[0][i] for i in range(3)))
        length = math.sqrt(dot(normal, normal))
        if length < 1e-9:
            raise ValueError(f"{self.name}: 零面积网格面")
        normal = tuple(c / length for c in normal)
        if target_normal is not None and dot(normal, target_normal) < 0:
            points = list(reversed(points))
            uv_coords = list(reversed(uv_coords))
            normal = tuple(-c for c in normal)
        ids = []
        for point, uv in zip(points, uv_coords):
            self.vertices.append(point)
            self.uvs.append(uv)
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
        mesh.face([p0, p1, q1, q0], material, desired,
                  uv_coords=[(i / sides, 0), (j / sides, 0), (j / sides, 1), (i / sides, 1)])
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
    cylinder(mesh, "z", (0.5, 0.5), 0.42, 0.04, 0.96, "rotor_hub", sides=16, caps=True)
    return mesh


def controller_mesh(name="controller_front") -> Mesh:
    mesh = Mesh(name)
    # 前端薄面板封住轴心开口，后方控制器壳体与面板边缘相接但不共面。
    mesh.quads_for_box((0, 0, 0), (1, 1, 0.08), "endcap", cap_front="controller", cap_back="endcap")
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
    side_material = f"{cap}_side"
    # 钢管主体分成标识环与管身两个不重叠区间，端面颜色表示介质方向。
    if role == "left":
        cylinder(mesh, axis, center, 0.32, band_end, end, "casing", sides=16, caps=False)
        cylinder(mesh, axis, center, 0.32, start, band_end, side_material, sides=16, caps=False)
        disk(mesh, axis, center, 0.32, start, cap, end_sign)
    else:
        cylinder(mesh, axis, center, 0.32, start, band_start, "casing", sides=16, caps=False)
        cylinder(mesh, axis, center, 0.32, band_start, end, side_material, sides=16, caps=False)
        disk(mesh, axis, center, 0.32, end, cap, end_sign)
    square_hole_plate(mesh, axis, inboard[0], inboard[1], 0.35, "endcap", "casing")
    # 方孔内侧以独立平面封口；其范围恰落在薄板开孔内，不与四条端板共面重叠。
    inner_plane = inboard[0] if end_sign < 0 else inboard[1]
    inner_square = [(0.15, 0.15), (0.85, 0.15), (0.85, 0.85), (0.15, 0.85)]
    mesh.face([point_on_plane(axis, inner_plane, u, v) for u, v in inner_square],
              "inside", axis_normal(axis, -end_sign))
    return mesh


TIER_DIAMETERS = (3, 5, 7)
SHELL_THICKNESS = 3.0 / 16.0
ROTOR_RADII = {3: 1.1875, 5: 2.1875, 7: 3.1875}


def polygon_area(points):
    return abs(sum(points[i][0] * points[(i + 1) % len(points)][1] -
                   points[(i + 1) % len(points)][0] * points[i][1]
                   for i in range(len(points))) / 2) if len(points) >= 3 else 0.0


def clip_polygon(points, axis, bound, keep_greater):
    """用Sutherland-Hodgman将多边形裁切到一个轴向半平面。"""
    if not points:
        return []
    out = []
    previous = points[-1]
    previous_inside = previous[axis] >= bound - 1e-9 if keep_greater else previous[axis] <= bound + 1e-9
    for current in points:
        current_inside = current[axis] >= bound - 1e-9 if keep_greater else current[axis] <= bound + 1e-9
        if current_inside != previous_inside:
            delta = current[axis] - previous[axis]
            if abs(delta) > 1e-12:
                t = (bound - previous[axis]) / delta
                out.append((previous[0] + t * (current[0] - previous[0]),
                            previous[1] + t * (current[1] - previous[1])))
        if current_inside:
            out.append(current)
        previous, previous_inside = current, current_inside
    return out


def clip_to_cell(points, x, y):
    polygon = list(points)
    for axis, bound, greater in ((0, x - 0.5, True), (0, x + 0.5, False),
                                 (1, y - 0.5, True), (1, y + 0.5, False)):
        polygon = clip_polygon(polygon, axis, bound, greater)
    return polygon


def offset_convex_polygon(points, distance):
    """沿每条外法线平移固定距离，求八边形等法向厚度的内轮廓。"""
    lines = []
    count = len(points)
    signed_area = sum(points[i][0] * points[(i + 1) % count][1] -
                      points[(i + 1) % count][0] * points[i][1] for i in range(count)) / 2
    orientation = 1 if signed_area > 0 else -1
    for i, p in enumerate(points):
        q = points[(i + 1) % count]
        dx, dy = q[0] - p[0], q[1] - p[1]
        length = math.hypot(dx, dy)
        nx, ny = orientation * dy / length, -orientation * dx / length
        lines.append(((p[0] - nx * distance, p[1] - ny * distance), (dx, dy)))
    result = []
    for i in range(count):
        (p1, v1), (p2, v2) = lines[i - 1], lines[i]
        det = v1[0] * v2[1] - v1[1] * v2[0]
        t = ((p2[0] - p1[0]) * v2[1] - (p2[1] - p1[1]) * v2[0]) / det
        result.append((p1[0] + t * v1[0], p1[1] + t * v1[1]))
    return result


def octagon(diameter):
    half = diameter / 2
    cut = diameter / (2 + math.sqrt(2))
    return [(-half + cut, -half), (half - cut, -half), (half, -half + cut),
            (half, half - cut), (half - cut, half), (-half + cut, half),
            (-half, half - cut), (-half, -half + cut)]


def shell_cells(diameter):
    """返回外表面与按3/16法向内缩轮廓之间有正面积的壳格。"""
    outer = octagon(diameter)
    inner = offset_convex_polygon(outer, SHELL_THICKNESS)
    r = (diameter - 1) // 2
    cells = []
    for y in range(-r, r + 1):
        for x in range(-r, r + 1):
            out_area = polygon_area(clip_to_cell(outer, x, y))
            in_area = polygon_area(clip_to_cell(inner, x, y))
            if out_area - in_area > 1e-8:
                cells.append((x, y))
    return cells


def casing_cell_mesh(diameter, section, x, y, seal_axial=False):
    """生成单格连续壳壁；端层同时带3/16封闭端盖和接续侧壳。"""
    name = f"d{diameter}_{section}_x{x}_y{y}"
    mesh = Mesh(name)
    outer = octagon(diameter)
    inner = offset_convex_polygon(outer, SHELL_THICKNESS)
    band_z0 = SHELL_THICKNESS if section == "front" else 0.0
    band_z1 = 1 - SHELL_THICKNESS if section == "rear" else 1.0
    _append_shell_band(mesh, diameter, x, y, band_z0, band_z1)
    if seal_axial and section == "middle":
        # 独立薄壳没有相邻轴向格遮住端口，需封闭外壁与内壁之间的截面。
        for i in range(8):
            band = [outer[i], outer[(i + 1) % 8], inner[(i + 1) % 8], inner[i]]
            clipped = clip_to_cell(band, x, y)
            if polygon_area(clipped) <= 1e-9:
                continue
            local = [(px - x + 0.5, py - y + 0.5) for px, py in clipped]
            mesh.face([(px, py, 0.0) for px, py in local], "endcap", (0, 0, -1), uv_axis=(0, 1))
            mesh.face([(px, py, 1.0) for px, py in local], "endcap", (0, 0, 1), uv_axis=(0, 1))
    if section in ("front", "rear"):
        # 端盖厚3/16格并覆盖完整八棱截面；侧壳从盖内缘接续到下一段。
        clipped = clip_to_cell(outer, x, y)
        local = [(px - x + 0.5, py - y + 0.5) for px, py in clipped]
        z0, z1 = (0.0, SHELL_THICKNESS) if section == "front" else (1 - SHELL_THICKNESS, 1.0)
        mesh.face([(px, py, z0) for px, py in local], "endcap", (0, 0, -1), uv_axis=(0, 1))
        mesh.face([(px, py, z1) for px, py in local], "endcap", (0, 0, 1), uv_axis=(0, 1))
        for i in range(len(local)):
            p0, p1 = local[i], local[(i + 1) % len(local)]
            mid = ((p0[0] + p1[0]) / 2 + x - 0.5, (p0[1] + p1[1]) / 2 + y - 0.5)
            dx, dy = p1[0] - p0[0], p1[1] - p0[1]
            mesh.face([(p0[0], p0[1], z0), (p1[0], p1[1], z0),
                       (p1[0], p1[1], z1), (p0[0], p0[1], z1)],
                      "casing", (dy, -dx, 0))
    return mesh


def _append_shell_band(mesh, diameter, x, y, z0, z1):
    """把裁切后的等法向厚度壳带追加到指定单格OBJ，保持边界面朝外。"""
    outer = octagon(diameter)
    inner = offset_convex_polygon(outer, SHELL_THICKNESS)
    for i in range(8):
        band = [outer[i], outer[(i + 1) % 8], inner[(i + 1) % 8], inner[i]]
        clipped = clip_to_cell(band, x, y)
        if polygon_area(clipped) <= 1e-9:
            continue
        local = [(px - x + 0.5, py - y + 0.5) for px, py in clipped]
        for j in range(len(local)):
            p0, p1 = local[j], local[(j + 1) % len(local)]
            gp0, gp1 = clipped[j], clipped[(j + 1) % len(clipped)]
            midpoint = ((gp0[0] + gp1[0]) / 2, (gp0[1] + gp1[1]) / 2)
            material = "edge"
            for k in range(8):
                if _point_segment_distance(midpoint, outer[k], outer[(k + 1) % 8]) < 1e-6:
                    material = "casing"
                    break
                if _point_segment_distance(midpoint, inner[k], inner[(k + 1) % 8]) < 1e-6:
                    material = "inside"
                    break
            if any(_point_segment_distance(gp0, outer[k], inner[k]) < 1e-6 and
                   _point_segment_distance(gp1, outer[k], inner[k]) < 1e-6 for k in range(8)):
                # 扇区共用的径向接边只保留一处，不输出相互重叠的内部面。
                continue
            dx, dy = gp1[0] - gp0[0], gp1[1] - gp0[1]
            target = (dy, -dx, 0)
            mesh.face([(p0[0], p0[1], z0), (p1[0], p1[1], z0),
                       (p1[0], p1[1], z1), (p0[0], p0[1], z1)], material, target)


def _point_segment_distance(point, a, b):
    dx, dy = b[0] - a[0], b[1] - a[1]
    denom = dx * dx + dy * dy
    if denom < 1e-12:
        return math.hypot(point[0] - a[0], point[1] - a[1])
    t = max(0.0, min(1.0, ((point[0] - a[0]) * dx + (point[1] - a[1]) * dy) / denom))
    return math.hypot(point[0] - (a[0] + t * dx), point[1] - (a[1] + t * dy))


def turbine_window_mesh(side):
    """生成带薄钢框、透明玻璃层和独立内外框边的观察窗格。"""
    mesh = Mesh(f"turbine_window_{side}")
    axis = {"up": "y", "down": "y", "left": "x", "right": "x"}[side]
    sign = {"up": 1, "down": -1, "left": -1, "right": 1}[side]
    plane = 1.0 if sign > 0 else 0.0
    inset = 0.14
    # 框在机壳外侧形成稳定金属边，玻璃片退入框后1/16格，避免共面闪烁。
    glass_plane = plane - sign * 0.04
    if axis == "y":
        strips = [((0, 0), (1, inset)), ((0, 1 - inset), (1, 1)),
                  ((0, inset), (inset, 1 - inset)), ((1 - inset, inset), (1, 1 - inset))]
        def point(u, v, depth): return (u, depth, v)
        normal = (0, sign, 0)
    else:
        strips = [((0, 0), (1, inset)), ((0, 1 - inset), (1, 1)),
                  ((0, inset), (inset, 1 - inset)), ((1 - inset, inset), (1, 1 - inset))]
        def point(u, v, depth): return (depth, v, u)
        normal = (sign, 0, 0)
    for (u0, v0), (u1, v1) in strips:
        corners = [point(u0, v0, plane), point(u1, v0, plane),
                   point(u1, v1, plane), point(u0, v1, plane)]
        mesh.face(corners, "casing", normal)
        inner_plane = plane - sign * SHELL_THICKNESS
        back = [point(u, v, inner_plane) for u, v in
                ((u0, v0), (u1, v0), (u1, v1), (u0, v1))]
        mesh.face(back, "inside", tuple(-n for n in normal))
        for i in range(4):
            j = (i + 1) % 4
            mesh.face([corners[i], corners[j], back[j], back[i]], "edge", normal)
    glass = [point(inset, inset, glass_plane), point(1 - inset, inset, glass_plane),
             point(1 - inset, 1 - inset, glass_plane), point(inset, 1 - inset, glass_plane)]
    mesh.face(glass, "glass", normal)
    glass_back_plane = glass_plane - sign * 0.04
    back_glass = [point(inset, inset, glass_back_plane), point(1 - inset, inset, glass_back_plane),
                  point(1 - inset, 1 - inset, glass_back_plane), point(inset, 1 - inset, glass_back_plane)]
    mesh.face(back_glass, "glass", tuple(-n for n in normal))
    for i in range(4):
        p0, p1 = glass[i], glass[(i + 1) % 4]
        q0, q1 = back_glass[i], back_glass[(i + 1) % 4]
        mesh.face([p0, p1, q1, q0], "glass_edge", normal)
    return mesh


def annular_sleeve(mesh, center, outer_radius, inner_radius, start, end, sides=24, cap_ends=True):
    """生成中空轮毂套筒，内孔给独立转子轴留出净空。"""
    outer0, outer1, inner0, inner1 = [], [], [], []
    for i in range(sides):
        angle = 2 * math.pi * i / sides
        co, si = math.cos(angle), math.sin(angle)
        outer0.append((center[0] + outer_radius * co, center[1] + outer_radius * si, start))
        outer1.append((center[0] + outer_radius * co, center[1] + outer_radius * si, end))
        inner0.append((center[0] + inner_radius * co, center[1] + inner_radius * si, start))
        inner1.append((center[0] + inner_radius * co, center[1] + inner_radius * si, end))
    for i in range(sides):
        j = (i + 1) % sides
        mid = ((outer0[i][0] + outer0[j][0]) / 2 - center[0],
               (outer0[i][1] + outer0[j][1]) / 2 - center[1], 0)
        u0, u1 = i / sides, (i + 1) / sides
        mesh.face([outer0[i], outer0[j], outer1[j], outer1[i]], "rotor_hub", mid,
                  uv_coords=[(u0, 0), (u1, 0), (u1, 1), (u0, 1)])
        mesh.face([inner0[j], inner0[i], inner1[i], inner1[j]], "inside", (-mid[0], -mid[1], 0),
                  uv_coords=[(u1, 0), (u0, 0), (u0, 1), (u1, 1)])
        if cap_ends:
            mesh.face([outer1[i], outer1[j], inner1[j], inner1[i]], "rotor_hub", (0, 0, 1),
                      uv_coords=[(u0, 1), (u1, 1), (u1, 0), (u0, 0)])
            mesh.face([outer0[j], outer0[i], inner0[i], inner0[j]], "rotor_hub", (0, 0, -1),
                      uv_coords=[(u1, 1), (u0, 1), (u0, 0), (u1, 0)])


def rotor_blades_mesh(diameter):
    """生成不含独立轴的空心轮毂与12片宽弦斜叶轮。"""
    radius = ROTOR_RADII[diameter]
    mesh = Mesh(f"rotor_blades_d{diameter}", bounds=False)
    annular_sleeve(mesh, (0.5, 0.5), 0.33, 0.19, 0.22, 0.78, sides=24)
    annular_sleeve(mesh, (0.5, 0.5), 0.29, 0.19, 0.13, 0.22, sides=24)
    annular_sleeve(mesh, (0.5, 0.5), 0.29, 0.19, 0.78, 0.87, sides=24)
    for blade in range(12):
        base = 2 * math.pi * blade / 12
        r0, r1 = 0.28, radius
        a0, a1 = base - 0.16, base + 0.12
        twist, width0, width1 = 0.20, 0.31, 0.27
        r1 = math.sqrt(radius * radius - width1 * width1)
        pts = [
            (0.5 + r0 * math.cos(a0) - width0 * math.sin(a0), 0.5 + r0 * math.sin(a0) + width0 * math.cos(a0)),
            (0.5 + r1 * math.cos(a1) - width1 * math.sin(a1), 0.5 + r1 * math.sin(a1) + width1 * math.cos(a1)),
            (0.5 + r1 * math.cos(a1) + width1 * math.sin(a1), 0.5 + r1 * math.sin(a1) - width1 * math.cos(a1)),
            (0.5 + r0 * math.cos(a0) + width0 * math.sin(a0), 0.5 + r0 * math.sin(a0) - width0 * math.cos(a0)),
        ]
        zbase = 0.5 + twist * ((blade % 2) - 0.5)
        top = [(px, py, zbase + dz) for (px, py), dz in zip(pts, (0.045, -0.045, -0.045, 0.045))]
        bottom = [(px, py, z - 0.075) for px, py, z in top]
        mesh.face(top, "blade", (0, 0, 1))
        mesh.face(bottom, "blade", (0, 0, -1))
        for i in range(4):
            mesh.face([top[i], top[(i + 1) % 4], bottom[(i + 1) % 4], bottom[i]],
                      "blade_edge", (0, 0, 1))
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
    return all_files_01b()


def transform_mesh(source: Mesh, name: str, transform) -> Mesh:
    """将标准构件变换到世界面，并保留逐顶点UV，避免圆端面重新分片贴图。"""
    target = Mesh(name, bounds=False)
    for material, indexes in source.faces:
        points = [source.vertices[v - 1] for v, _, _ in indexes]
        normal = source.normals[indexes[0][2] - 1]
        transformed = [transform(point) for point in points]
        transformed_normal = transform(normal, vector=True)
        uvs = [source.uvs[t - 1] for _, t, _ in indexes]
        target.face(transformed, material, transformed_normal, uv_coords=uvs)
    return target


def controller_side_mesh(side):
    """把仪表控制器从本地前向基准旋转到指定四侧平面。"""
    transforms = {
        "right": lambda p: (1 - p[2], p[1], p[0]),
        "left": lambda p: (p[2], p[1], 1 - p[0]),
        "up": lambda p: (p[0], 1 - p[2], p[1]),
        "down": lambda p: (p[0], p[2], 1 - p[1]),
    }
    def do_transform(p, vector=False):
        if vector:
            return tuple(transforms[side](p)[i] - transforms[side]((0, 0, 0))[i] for i in range(3))
        return transforms[side](p)
    return transform_mesh(controller_mesh(f"controller_{side}"), f"controller_{side}", do_transform)


def port_direction_mesh(kind, direction):
    """将端口外端从统一局部西向基准旋转到六个世界方向。"""
    transforms = {
        "west": lambda p: (p[0], p[1], p[2]),
        "east": lambda p: (1 - p[0], p[1], 1 - p[2]),
        "north": lambda p: (1 - p[2], p[1], p[0]),
        "south": lambda p: (p[2], p[1], 1 - p[0]),
        "up": lambda p: (p[1], 1 - p[0], p[2]),
        "down": lambda p: (p[1], p[0], 1 - p[2]),
    }
    transform = transforms[direction]
    def apply(point, vector=False):
        if not vector:
            return transform(point)
        origin = transform((0, 0, 0))
        endpoint = transform(point)
        return tuple(endpoint[i] - origin[i] for i in range(3))
    base = port_mesh(kind, "left", f"{kind}_loose_{direction}")
    return transform_mesh(base, f"{kind}_loose_{direction}", apply)


def output_shaft_mesh(end):
    """生成端部承轴板、轴承座与0..1格内的轴身，端面平接相邻动力轴。"""
    mesh = Mesh(f"output_shaft_{end}", bounds=False)
    if end == "front":
        plate_start, plate_end = 0.0, SHELL_THICKNESS
        square_hole_plate(mesh, "z", plate_start, plate_end, 0.19, "endcap", "casing")
        annular_sleeve(mesh, (0.5, 0.5), 0.27, 0.155, plate_start, plate_end, sides=24, cap_ends=False)
        cylinder(mesh, "z", (0.5, 0.5), 0.14, 0.0, 1.0, "shaft", sides=20, caps=False)
        disk(mesh, "z", (0.5, 0.5), 0.14, 0.0, "shaft", -1, sides=20)
    else:
        plate_start, plate_end = 1.0 - SHELL_THICKNESS, 1.0
        square_hole_plate(mesh, "z", plate_start, plate_end, 0.19, "endcap", "casing")
        annular_sleeve(mesh, (0.5, 0.5), 0.27, 0.155, plate_start, plate_end, sides=24, cap_ends=False)
        cylinder(mesh, "z", (0.5, 0.5), 0.14, 0.0, 1.0, "shaft", sides=20, caps=False)
        disk(mesh, "z", (0.5, 0.5), 0.14, 1.0, "shaft", 1, sides=20)
    return mesh


def axle_mesh():
    """单格转子轴心；范围在0..1且不包含叶片或空心轮毂。"""
    mesh = Mesh("rotor_axle")
    cylinder(mesh, "z", (0.5, 0.5), 0.14, 0.0, 1.0, "shaft", sides=16)
    return mesh


def small_item_rotor():
    """把单档完整叶轮缩放成未成型/物品展示模型，不改变正式扫掠半径。"""
    source = rotor_blades_mesh(3)
    scale = 0.42 / ROTOR_RADII[3]
    target = Mesh("turbine_rotor_unformed", bounds=False)
    for material, indexes in source.faces:
        points = [source.vertices[v - 1] for v, _, _ in indexes]
        normal = source.normals[indexes[0][2] - 1]
        uvs = [source.uvs[t - 1] for _, t, _ in indexes]
        target.face([(0.5 + (p[0] - 0.5) * scale,
                      0.5 + (p[1] - 0.5) * scale, p[2]) for p in points], material, normal,
                    uv_coords=uvs)
    return target


def canonical_piece_cells():
    """按接口规定D/section/y/x稳定顺序生成1..206 piece映射。"""
    result = {}
    number = 0
    for diameter in TIER_DIAMETERS:
        r = (diameter - 1) // 2
        outer = octagon(diameter)
        for section in ("front", "middle", "rear"):
            candidates = shell_cells(diameter) if section == "middle" else [
                (x, y) for y in range(-r, r + 1) for x in range(-r, r + 1)
                if polygon_area(clip_to_cell(outer, x, y)) > 1e-8
            ]
            allowed = set(candidates)
            for y in range(-r, r + 1):
                for x in range(-r, r + 1):
                    if (x, y) in allowed:
                        number += 1
                        result[number] = (diameter, section, x, y)
    return result


def obj_resource_files(meshes):
    """将静态网格与NeoForge OBJ包装JSON写到候选暂存树。"""
    out = {}
    particle_by_material = {
        "casing": "turbine_casing_panel", "inside": "turbine_casing_panel",
        "edge": "turbine_casing_panel", "endcap": "turbine_casing_panel",
        "glass": "turbine_window_glass", "glass_edge": "turbine_window_glass",
        "rotor_hub": "turbine_rotor_metal", "blade": "turbine_rotor_blade",
        "blade_edge": "turbine_rotor_metal", "shaft": "turbine_output_shaft_surface",
        "brass": "turbine_brass_surface", "controller": "turbine_controller_surface",
        "inlet": "turbine_inlet_face", "exhaust": "turbine_exhaust_face",
        "inlet_side": "turbine_inlet_side", "exhaust_side": "turbine_exhaust_side",
        "support": "turbine_bearing_support_surface",
    }
    for name, mesh in meshes.items():
        name = name.replace("/", "_")
        obj_path = f"assets/create_nuclear_industry/models/block/turbine/mesh/{name}.obj"
        out[obj_path] = mesh.text().encode("utf-8")
    materials = {
        "casing": "turbine_casing_panel", "inside": "turbine_casing_panel",
        "edge": "turbine_casing_panel", "endcap": "turbine_casing_panel",
        "glass": "turbine_window_glass", "glass_edge": "turbine_window_glass",
        "rotor_hub": "turbine_rotor_metal", "blade": "turbine_rotor_blade",
        "blade_edge": "turbine_rotor_metal", "shaft": "turbine_output_shaft_surface",
        "brass": "turbine_brass_surface", "controller": "turbine_controller_surface",
        "inlet": "turbine_inlet_face", "exhaust": "turbine_exhaust_face",
        "inlet_side": "turbine_inlet_side", "exhaust_side": "turbine_exhaust_side",
        "support": "turbine_bearing_support_surface",
    }
    mtl = []
    for material, texture in materials.items():
        mtl.extend([f"newmtl {material}", "Ka 0.25 0.25 0.25", "Kd 1 1 1",
                    f"map_Kd create_nuclear_industry:block/turbine/{texture}"])
        if material in ("glass", "glass_edge"):
            mtl.append("d 0.53")
        mtl.append("")
    out["assets/create_nuclear_industry/models/block/turbine/mesh/turbine.mtl"] = "\n".join(mtl).encode("utf-8")
    return out


def model_wrapper(name, particle, culling=True, render_type=None):
    result = {
        "loader": "neoforge:obj",
        "model": f"create_nuclear_industry:models/block/turbine/mesh/{name}.obj",
        "textures": {"particle": f"create_nuclear_industry:block/turbine/{particle}"},
        "automatic_culling": culling, "shade_quads": True, "flip_v": False,
        "emissive_ambient": False,
    }
    if render_type is not None:
        result["render_type"] = render_type
    return result


def write_model_wrapper(out, path, mesh, particle, culling=True, render_type=None):
    out[f"assets/create_nuclear_industry/models/block/turbine/{path}.json"] = write_json(
        Path(path), model_wrapper(mesh, particle, culling, render_type))


def model_and_state_files():
    """生成与A冻结接口一致的206段壳/窗状态、转子、轴和侧接口模型。"""
    files, meshes = {}, {}
    piece_map = canonical_piece_cells()
    for number, (diameter, section, x, y) in piece_map.items():
        name = f"d{diameter}_{section}_x{x}_y{y}"
        meshes[f"casing/{name}"] = casing_cell_mesh(diameter, section, x, y)
        particle = "turbine_casing_panel"
        write_model_wrapper(files, f"casing/{name}", f"casing_{name}", particle)
        r = (diameter - 1) // 2
        if section == "middle" and ((x == 0 and abs(y) == r) or (y == 0 and abs(x) == r)):
            side = "up" if y == r else "down" if y == -r else "right" if x == r else "left"
            window_mesh = turbine_window_mesh(side)
            meshes[f"window/{name}"] = window_mesh
            write_model_wrapper(files, f"window/{name}", f"window_{name}", "turbine_window_glass",
                                render_type="translucent")

    for diameter in TIER_DIAMETERS:
        blades = rotor_blades_mesh(diameter)
        meshes[f"rotor_blades_d{diameter}"] = blades
        write_model_wrapper(files, f"rotor_blades_d{diameter}", f"rotor_blades_d{diameter}",
                            "turbine_rotor_blade", culling=False)
    meshes["rotor_axle"] = axle_mesh()
    write_model_wrapper(files, "rotor_axle", "rotor_axle", "turbine_output_shaft")
    for end in ("front", "rear"):
        meshes[f"output_shaft_{end}"] = output_shaft_mesh(end)
        write_model_wrapper(files, f"output_shaft_{end}", f"output_shaft_{end}", "turbine_output_shaft", False)
    for side in ("up", "down", "left", "right"):
        meshes[f"controller_{side}"] = controller_side_mesh(side)
        write_model_wrapper(files, f"controller_{side}", f"controller_{side}", "turbine_controller")
        for kind in ("inlet", "exhaust"):
            source = port_mesh(kind, "top" if side in ("up", "down") else side, f"{kind}_{side}")
            if side == "down":
                source = transform_mesh(source, f"{kind}_down", lambda p, vector=False: (
                    (p[0], 1 - p[1], p[2]) if not vector else (p[0], -p[1], p[2])))
            meshes[f"{kind}_{side}"] = source
            write_model_wrapper(files, f"{kind}_{side}", f"{kind}_{side}", f"turbine_{kind}")
    for direction in OUTWARD:
        for kind in ("inlet", "exhaust"):
            model_name = f"{kind}_loose_{direction}"
            meshes[model_name] = port_direction_mesh(kind, direction)
            write_model_wrapper(files, model_name, model_name, f"turbine_{kind}")

    # 独立面板沿用同一片3/16格薄壳，并预烘焙六个世界朝向，避免与机身朝向重复旋转。
    d3_top = casing_cell_mesh(3, "middle", 0, 1, seal_axial=True)
    meshes["turbine_casing_unformed"] = d3_top
    write_model_wrapper(files, "turbine_casing_unformed", "turbine_casing_unformed", "turbine_casing_panel")
    meshes["turbine_window_unformed"] = turbine_window_mesh("up")
    write_model_wrapper(files, "turbine_window_unformed", "turbine_window_unformed", "turbine_window_glass",
                        render_type="translucent")
    face_transforms = {
        "up": lambda p: (p[0], p[1], p[2]),
        "down": lambda p: (p[0], 1 - p[1], 1 - p[2]),
        "north": lambda p: (p[0], p[2], 1 - p[1]),
        "south": lambda p: (p[0], 1 - p[2], p[1]),
        "west": lambda p: (1 - p[1], p[0], p[2]),
        "east": lambda p: (p[1], 1 - p[0], p[2]),
    }
    def face_transform(transform):
        def apply(point, vector=False):
            if not vector:
                return transform(point)
            origin = transform((0, 0, 0))
            endpoint = transform(point)
            return tuple(endpoint[i] - origin[i] for i in range(3))
        return apply
    for face, transform in face_transforms.items():
        if face == "up":
            continue
        casing_name = f"turbine_casing_unformed_{face}"
        window_name = f"turbine_window_unformed_{face}"
        meshes[casing_name] = transform_mesh(d3_top, casing_name, face_transform(transform))
        write_model_wrapper(files, casing_name, casing_name, "turbine_casing_panel")
        meshes[window_name] = transform_mesh(meshes["turbine_window_unformed"], window_name,
                                             face_transform(transform))
        write_model_wrapper(files, window_name, window_name, "turbine_window_glass", render_type="translucent")
    meshes["turbine_rotor_unformed"] = small_item_rotor()
    write_model_wrapper(files, "turbine_rotor_unformed", "turbine_rotor_unformed", "turbine_rotor_blade", False)
    # 保留旧partial资源路径，避免客户端已缓存的块状态或调用方再次遇到rotor缺失。
    meshes["rotor_middle"] = rotor_mesh()
    write_model_wrapper(files, "rotor_middle", "rotor_middle", "turbine_rotor_metal")
    meshes["turbine_controller_unformed"] = controller_mesh("turbine_controller_unformed")
    write_model_wrapper(files, "turbine_controller_unformed", "turbine_controller_unformed", "turbine_controller")
    meshes["turbine_output_shaft_unformed"] = output_shaft_mesh("front")
    write_model_wrapper(files, "turbine_output_shaft_unformed", "turbine_output_shaft_unformed", "turbine_output_shaft", False)
    meshes["turbine_output_shaft_unformed_rear"] = output_shaft_mesh("rear")
    write_model_wrapper(files, "turbine_output_shaft_unformed_rear", "turbine_output_shaft_unformed_rear",
                        "turbine_output_shaft", False)
    for kind in ("inlet", "exhaust"):
        meshes[f"turbine_{kind}_unformed"] = port_mesh(kind, "left", f"turbine_{kind}_unformed")
        write_model_wrapper(files, f"turbine_{kind}_unformed", f"turbine_{kind}_unformed", f"turbine_{kind}")

    files.update(obj_resource_files(meshes))
    files.update(blockstates_01b(piece_map))
    display = {
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
        "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
        "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.25, 0.25, 0.25]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
        "head": {"rotation": [0, 0, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    }
    for block in (*BLOCKS, "turbine_window"):
        parent = "turbine_rotor_unformed" if block == "turbine_rotor" else f"{block}_unformed"
        files[f"assets/create_nuclear_industry/models/item/{block}.json"] = write_json(
            Path(block), {"parent": f"create_nuclear_industry:block/turbine/{parent}", "display": display})
    return files


def blockstates_01b(piece_map):
    """按定位态选择已定位网格或六向独立板，formed仅表示运行资格。"""
    out = {}
    model = lambda name: f"create_nuclear_industry:block/turbine/{name}"
    independent_faces = {0: "up", 207: "down", 208: "north", 209: "south", 210: "west", 211: "east"}
    for block in ("turbine_casing", "turbine_window"):
        variants = {}
        for located, facing, piece in itertools.product((False, True), FACING, range(212)):
            key = f"located={str(located).lower()},machine_facing={facing},piece={piece}"
            candidate = piece_map.get(piece)
            selected = "turbine_window_unformed" if block == "turbine_window" else "turbine_casing_unformed"
            rotation = facing
            if located and candidate:
                if block == "turbine_window":
                    valid = candidate[1] == "middle" and (
                        (candidate[2] == 0 and abs(candidate[3]) == (candidate[0] - 1) // 2) or
                        (candidate[3] == 0 and abs(candidate[2]) == (candidate[0] - 1) // 2))
                    if valid:
                        selected = f"window/d{candidate[0]}_{candidate[1]}_x{candidate[2]}_y{candidate[3]}"
                else:
                    selected = f"casing/d{candidate[0]}_{candidate[1]}_x{candidate[2]}_y{candidate[3]}"
            elif not located and piece in independent_faces:
                face = independent_faces[piece]
                selected = f"{block}_unformed" if face == "up" else f"{block}_unformed_{face}"
                rotation = None
            variants[key] = variant(model(selected), rotation)
        out[f"assets/create_nuclear_industry/blockstates/{block}.json"] = write_json(Path(block), {"variants": variants})

    variants = {}
    for located, facing, diameter in itertools.product((False, True), FACING, ("d3", "d5", "d7")):
        key = f"diameter={diameter},located={str(located).lower()},machine_facing={facing}"
        variants[key] = variant(model("rotor_axle" if located else "turbine_rotor_unformed"), facing)
    out["assets/create_nuclear_industry/blockstates/turbine_rotor.json"] = write_json(Path("turbine_rotor"), {"variants": variants})

    variants = {}
    for located, facing, end in itertools.product((False, True), FACING, ("front", "rear")):
        key = f"end={end},located={str(located).lower()},machine_facing={facing}"
        if located:
            selected = f"output_shaft_{end}"
        else:
            selected = "turbine_output_shaft_unformed" if end == "front" else "turbine_output_shaft_unformed_rear"
        variants[key] = variant(model(selected), facing)
    out["assets/create_nuclear_industry/blockstates/turbine_output_shaft.json"] = write_json(Path("turbine_output_shaft"), {"variants": variants})

    variants = {}
    for located, facing, side in itertools.product((False, True), FACING, ("up", "down", "left", "right")):
        key = f"located={str(located).lower()},machine_facing={facing},side={side}"
        variants[key] = variant(model(f"controller_{side}" if located else "turbine_controller_unformed"), facing)
    out["assets/create_nuclear_industry/blockstates/turbine_controller.json"] = write_json(Path("turbine_controller"), {"variants": variants})

    for block, kind in (("turbine_inlet", "inlet"), ("turbine_exhaust", "exhaust")):
        variants = {}
        for located, facing, role, outward in itertools.product((False, True), FACING, RING_ROLES, OUTWARD):
            key = f"located={str(located).lower()},machine_facing={facing},ring_role={role},outward={outward}"
            side = {"top": "up", "bottom": "down", "left": "left", "right": "right"}.get(role)
            expected = side if side in ("up", "down") else SIDE_WORLD[facing].get(side)
            if located:
                selected = f"{kind}_{side}" if side and outward == expected else f"{block}_unformed"
                rotation = facing
            else:
                selected = f"{kind}_loose_{outward}"
                rotation = None
            variants[key] = variant(model(selected), rotation)
        out[f"assets/create_nuclear_industry/blockstates/{block}.json"] = write_json(Path(block), {"variants": variants})
    return out


def all_files_01b():
    files = model_and_state_files()
    # 旧包装JSON仍可能加载顶部端口OBJ，保持其端面与新端口同样封闭且UV连续。
    for kind in ("inlet", "exhaust"):
        files[f"assets/create_nuclear_industry/models/block/turbine/mesh/{kind}_top.obj"] = (
            port_mesh(kind, "top", f"{kind}_top").text().encode("utf-8"))
    files.update(opaque_surface_files())
    return files


def opaque_surface_files():
    """从原图标构造实体面贴图；透明底只供物品图标使用，玻璃继续透明。"""
    icon_root = ASSETS / "textures/block/turbine"
    specs = {
        "turbine_controller_surface": ("turbine_controller", (62, 80, 88)),
        "turbine_inlet_face": ("turbine_inlet", (80, 62, 42)),
        "turbine_exhaust_face": ("turbine_exhaust", (38, 65, 77)),
        "turbine_output_shaft_surface": ("turbine_output_shaft", (80, 92, 96)),
        "turbine_bearing_support_surface": ("turbine_bearing_support", (89, 77, 59)),
        "turbine_brass_surface": ("turbine_casing_endcap", (101, 79, 53)),
    }
    result = {}
    for output, (source, base) in specs.items():
        background = Image.new("RGBA", (16, 16), (*base, 255))
        foreground = Image.open(icon_root / f"{source}.png").convert("RGBA")
        background.alpha_composite(foreground)
        stream = io.BytesIO()
        background.save(stream, format="PNG", optimize=False)
        result[f"assets/create_nuclear_industry/textures/block/turbine/{output}.png"] = stream.getvalue()
    for name, base, accent in (
        ("turbine_inlet_side", (80, 62, 42), (240, 138, 54)),
        ("turbine_exhaust_side", (38, 65, 77), (133, 205, 224)),
    ):
        texture = Image.new("RGBA", (16, 16), (*base, 255))
        painter = ImageDraw.Draw(texture)
        painter.rectangle((0, 0, 15, 2), fill=(*accent, 255))
        painter.rectangle((0, 13, 15, 15), fill=(*accent, 255))
        painter.line((0, 5, 15, 5), fill=(163, 183, 182, 255), width=1)
        painter.line((0, 10, 15, 10), fill=(163, 183, 182, 255), width=1)
        stream = io.BytesIO()
        texture.save(stream, format="PNG", optimize=False)
        result[f"assets/create_nuclear_industry/textures/block/turbine/{name}.png"] = stream.getvalue()
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


def verify_01b(files: dict[str, bytes]) -> dict:
    """校验汽轮机01B模型引用、状态覆盖、206格映射和转子局部坐标。"""
    json_files = {path: json.loads(data.decode("utf-8")) for path, data in files.items()
                  if path.endswith(".json")}
    model_files = {path for path in json_files if "/models/block/turbine/" in path}
    obj_files = {path for path in files if path.endswith(".obj")}
    state_files = {path for path in json_files if "/blockstates/" in path}
    item_files = {path for path in json_files if "/models/item/turbine_" in path}
    assert len(canonical_piece_cells()) == 206
    tier_counts = {d: {section: sum(1 for entry in canonical_piece_cells().values()
                                     if entry[0] == d and entry[1] == section)
                       for section in AXIAL} for d in TIER_DIAMETERS}
    assert tier_counts == {3: {"front": 9, "middle": 8, "rear": 9},
                           5: {"front": 25, "middle": 16, "rear": 25},
                           7: {"front": 45, "middle": 24, "rear": 45}}
    assert len(model_files) == 267 and len(obj_files) == 269
    assert len(state_files) == 7 and len(item_files) == 7
    path_set = set(files)
    for path in model_files:
        value = json_files[path]
        assert value.get("loader") == "neoforge:obj", path
        obj_ref = value.get("model", "").split(":", 1)[-1]
        assert f"assets/create_nuclear_industry/{obj_ref}" in path_set, (path, obj_ref)
        particle = value.get("textures", {}).get("particle", "").split(":", 1)[-1]
        assert f"assets/{particle}.png" in path_set or (ASSETS / "textures" / f"{particle}.png").is_file(), (path, particle)
    state_counts = {}
    for path in state_files:
        variants = json_files[path].get("variants", {})
        state_counts[Path(path).stem] = len(variants)
        for entry in variants.values():
            model_path = entry["model"].split(":", 1)[-1]
            assert f"assets/create_nuclear_industry/models/{model_path}.json" in path_set, (path, model_path)
    material_libraries = {path: data.decode("utf-8") for path, data in files.items() if path.endswith(".mtl")}
    for path in obj_files:
        obj_text = files[path].decode("utf-8")
        library_names = [row.split(maxsplit=1)[1] for row in obj_text.splitlines() if row.startswith("mtllib ")]
        assert library_names, (path, "缺少mtllib")
        declared = set()
        for library_name in library_names:
            library_path = (Path(path).parent / library_name).as_posix()
            assert library_path in material_libraries, (path, library_path)
            declared.update(row.split(maxsplit=1)[1] for row in material_libraries[library_path].splitlines()
                            if row.startswith("newmtl "))
        used = {row.split(maxsplit=1)[1] for row in obj_text.splitlines() if row.startswith("usemtl ")}
        assert used <= declared, (path, sorted(used - declared))
        vertices = [tuple(float(v) for v in row.split()[1:4]) for row in obj_text.splitlines() if row.startswith("v ")]
        normals = [tuple(float(v) for v in row.split()[1:4]) for row in obj_text.splitlines() if row.startswith("vn ")]
        for row in (line for line in obj_text.splitlines() if line.startswith("f ")):
            refs = [tuple(int(index) for index in token.split("/")) for token in row.split()[1:]]
            points = [vertices[ref[0] - 1] for ref in refs]
            normal = normals[refs[0][2] - 1]
            geometric = cross(tuple(points[1][i] - points[0][i] for i in range(3)),
                              tuple(points[2][i] - points[0][i] for i in range(3)))
            assert dot(geometric, normal) > 1e-9, (path, row, geometric, normal)
    uv_probe = Mesh("uv_probe", bounds=False)
    polygon = [(math.cos(2 * math.pi * i / 8), math.sin(2 * math.pi * i / 8), 0) for i in range(8)]
    uv_probe.face(polygon, "probe", (0, 0, 1))
    by_point = {}
    for face_material, indexes in uv_probe.faces:
        for vertex_index, texture_index, _ in indexes:
            point, uv = uv_probe.vertices[vertex_index - 1], uv_probe.uvs[texture_index - 1]
            by_point.setdefault(point, set()).add(uv)
    assert all(len(values) == 1 for values in by_point.values())
    expected_counts = {"turbine_casing": 1696, "turbine_window": 1696,
                       "turbine_rotor": 24, "turbine_output_shaft": 16,
                       "turbine_controller": 32, "turbine_inlet": 384,
                       "turbine_exhaust": 384}
    assert state_counts == expected_counts, state_counts
    for block, port in (("turbine_inlet", "inlet"), ("turbine_exhaust", "exhaust")):
        variants = json_files[f"assets/create_nuclear_industry/blockstates/{block}.json"]["variants"]
        for facing, side in itertools.product(FACING, ("top", "bottom", "left", "right")):
            local = {"top": "up", "bottom": "down", "left": "left", "right": "right"}[side]
            expected = local if local in ("up", "down") else SIDE_WORLD[facing][local]
            good = f"located=true,machine_facing={facing},ring_role={side},outward={expected}"
            assert variants[good]["model"].endswith(f"/{port}_{local}")
            wrong = next(direction for direction in OUTWARD if direction != expected)
            bad = f"located=true,machine_facing={facing},ring_role={side},outward={wrong}"
            assert variants[bad]["model"].endswith(f"/{block}_unformed")
    face_codes = {0: "up", 207: "down", 208: "north", 209: "south", 210: "west", 211: "east"}
    for block in ("turbine_casing", "turbine_window"):
        variants = json_files[f"assets/create_nuclear_industry/blockstates/{block}.json"]["variants"]
        for piece, face in face_codes.items():
            selected = set()
            for facing in FACING:
                key = f"located=false,machine_facing={facing},piece={piece}"
                value = variants[key]
                selected.add(value["model"])
                assert "y" not in value, (block, key, value)
            expected_model = f"{block}_unformed" if face == "up" else f"{block}_unformed_{face}"
            assert selected == {f"{MODEL_ROOT}/{expected_model}"}, (block, piece, selected)
        for located, facing, piece in itertools.product((False, True), FACING, range(212)):
            assert f"located={str(located).lower()},machine_facing={facing},piece={piece}" in variants
    window_models = [path for path in model_files if "/window/" in path]
    assert len(window_models) == 12
    assert all(json_files[path].get("render_type") == "translucent" for path in window_models)
    independent_windows = ["assets/create_nuclear_industry/models/block/turbine/turbine_window_unformed.json"] + [
        f"assets/create_nuclear_industry/models/block/turbine/turbine_window_unformed_{face}.json"
        for face in ("down", "north", "south", "west", "east")]
    assert all(json_files[path].get("render_type") == "translucent" for path in independent_windows)
    unformed_window = "assets/create_nuclear_industry/models/block/turbine/turbine_window_unformed.json"
    assert json_files[unformed_window].get("render_type") == "translucent"
    rotor_data = {}
    for diameter in TIER_DIAMETERS:
        path = f"assets/create_nuclear_industry/models/block/turbine/mesh/rotor_blades_d{diameter}.obj"
        text = files[path].decode("utf-8")
        vertices = [tuple(float(v) for v in row.split()[1:4]) for row in text.splitlines() if row.startswith("v ")]
        materials = {row.split()[1] for row in text.splitlines() if row.startswith("usemtl ")}
        assert vertices and "shaft" not in materials and materials <= {"rotor_hub", "inside", "blade", "blade_edge"}
        center = (0.5, 0.5)
        radius = max(math.hypot(v[0] - center[0], v[1] - center[1]) for v in vertices)
        expected_radius = diameter / 2 - SHELL_THICKNESS - 1 / 8
        assert abs(radius - expected_radius) < 1e-5, (diameter, radius, expected_radius)
        assert min(v[2] for v in vertices) >= 0.12 and max(v[2] for v in vertices) <= 0.88
        rotor_data[f"d{diameter}"] = {"origin": [0.5, 0.5, 0.5], "max_sweep_radius": round(radius, 5),
                                       "vertex_z_range": [round(min(v[2] for v in vertices), 5),
                                                          round(max(v[2] for v in vertices), 5)],
                                       "materials": sorted(materials)}
    for end in ("front", "rear"):
        path = f"assets/create_nuclear_industry/models/block/turbine/mesh/output_shaft_{end}.obj"
        vertices = [tuple(float(v) for v in row.split()[1:4]) for row in files[path].decode("utf-8").splitlines()
                    if row.startswith("v ")]
        assert all(all(-1e-8 <= c <= 1.00000001 for c in point) for point in vertices), path
        shaft_faces = parse_obj(files[path])
        plate_plane = 0.0 if end == "front" else 1.0
        assert not any(material == "rotor_hub" and all(abs(p[2] - plate_plane) < 1e-8 for p in points)
                       for material, points, _ in shaft_faces), (path, "轴承环端面与端板共面")
        assert any(material == "shaft" and all(abs(p[2] - plate_plane) < 1e-8 for p in points)
                   for material, points, _ in shaft_faces), (path, "外向轴端封面缺失")
    independent_casing = ["turbine_casing_unformed"] + [f"turbine_casing_unformed_{face}"
                                                        for face in ("down", "north", "south", "west", "east")]
    for name in independent_casing:
        faces = parse_obj(files[f"assets/create_nuclear_industry/models/block/turbine/mesh/{name}.obj"])
        cap_planes = set()
        for material, points, normal in faces:
            if material != "endcap":
                continue
            for axis in range(3):
                if all(abs(point[axis] - points[0][axis]) < 1e-8 for point in points) and abs(normal[axis]) > 0.999:
                    cap_planes.add((axis, round(points[0][axis], 6), 1 if normal[axis] > 0 else -1))
        assert any(axis == other_axis and sign == -other_sign and abs(plane - other_plane) > 0.5
                   for axis, plane, sign in cap_planes for other_axis, other_plane, other_sign in cap_planes), \
            (name, "轴向截面未形成方向相反的封口")
    controller = parse_obj(files["assets/create_nuclear_industry/models/block/turbine/mesh/turbine_controller_unformed.obj"])
    assert any(material == "endcap" and all(abs(point[2] - 0.08) < 1e-8 for point in points)
               for material, points, _ in controller), "控制器大面板背面未封口"
    shaft_states = json_files["assets/create_nuclear_industry/blockstates/turbine_output_shaft.json"]["variants"]
    for facing in FACING:
        front_key = f"end=front,located=false,machine_facing={facing}"
        rear_key = f"end=rear,located=false,machine_facing={facing}"
        assert shaft_states[front_key]["model"].endswith("/turbine_output_shaft_unformed")
        assert shaft_states[rear_key]["model"].endswith("/turbine_output_shaft_unformed_rear")
        assert shaft_states[front_key].get("y") == ROTATION[facing]
    for block, kind in (("turbine_inlet", "inlet"), ("turbine_exhaust", "exhaust")):
        variants = json_files[f"assets/create_nuclear_industry/blockstates/{block}.json"]["variants"]
        for direction in OUTWARD:
            key = f"located=false,machine_facing=north,ring_role=top,outward={direction}"
            value = variants[key]
            assert value["model"].endswith(f"/{kind}_loose_{direction}") and "y" not in value, (block, key, value)
    for path, data in files.items():
        if path.endswith(".obj") and "/rotor_blades_" not in path and "output_shaft" not in path:
            vertices = [tuple(float(v) for v in row.split()[1:4]) for row in data.decode("utf-8").splitlines()
                        if row.startswith("v ")]
            assert all(all(-1e-8 <= c <= 1.00000001 for c in point) for point in vertices), path
    required_display = {"thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand",
                        "firstperson_lefthand", "head", "gui", "ground", "fixed"}
    for path in item_files:
        assert required_display <= set(json_files[path].get("display", {})), path
    return {"blockstate_variants": state_counts, "model_json_count": len(model_files),
            "obj_mesh_count": len(obj_files), "item_model_count": len(item_files),
            "piece_mapping": {"total": 206, "tier_sections": tier_counts},
            "state_selection": "located controls exterior model; formed omitted from selector; piece 0/207..211 is world-face independent",
            "independent_faces": "six pre-rotated panel and window meshes; no machine_facing rotation",
            "valid_window_models": len(window_models), "rotor_geometry": rotor_data,
            "rotor_axis_and_center": "+Z axis, block-local (0.5,0.5,0.5); partial has no shaft",
            "output_shaft_extent": "front/rear axes and 3/16 bearing plates remain within local z=0..1",
            "model_obj_particle_references": "PASS", "regular_mesh_bounds": "0..1 per block",
            "reachable_obj_materials_and_normals": "PASS", "triangulated_uv_projection": "PASS",
            "independent_shell_and_controller_sealing": "PASS", "bearing_coplanar_faces": "PASS",
            "ordinary_shaft_and_port_orientation": "PASS"}


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
    parser = argparse.ArgumentParser(description="生成汽轮机01B真实薄壳OBJ模型与NeoForge资源")
    parser.add_argument("--install", action="store_true", help="将已校验资源写入src/main/resources")
    args = parser.parse_args()
    files = all_files()
    summary = verify_01b(files)
    write_files(files, STAGING)
    if args.install:
        # 安装范围仅是本批汽轮机模型、方块状态和物品模型。
        target = REPO / "src/main/resources"
        for relative, data in files.items():
            if ("/models/block/turbine/" in relative or "/blockstates/turbine_" in relative or
                    "/models/item/turbine_" in relative or "/textures/block/turbine/" in relative):
                path = target / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(data)
    summary.update({"install": bool(args.install), "staged_root": STAGING.relative_to(REPO).as_posix(),
                    "runtime_root": "src/main/resources", "rotor_origin": "block-local (0.5,0.5,0.5)",
                    "status": "PASS"})
    REPORT.mkdir(parents=True, exist_ok=True)
    (REPORT / "model-generation.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=True, indent=2))


if __name__ == "__main__":
    main()
