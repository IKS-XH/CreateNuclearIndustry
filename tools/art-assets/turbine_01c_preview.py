"""从安装后的汽轮机资源绘制01C搭建阶段离线预览。"""
from __future__ import annotations

import importlib.util
import json
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[1]
ASSETS = REPO / "src/main/resources/assets/create_nuclear_industry"
REPORT = REPO / "build/reports/extension/EXT-B-TURBINE-01C-ASSETS"
MODEL_SCRIPT = ROOT / "turbine_models.py"
SPEC = importlib.util.spec_from_file_location("turbine_models_01c_preview", MODEL_SCRIPT)
models = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = models
SPEC.loader.exec_module(models)

FACING = "north"
FACE_CODES = {"up": 0, "down": 207, "north": 208, "south": 209, "west": 210, "east": 211}
FONT_PATH = Path("C:/Windows/Fonts/msyh.ttc")
PIECE_IDS = {
    (diameter, section, x, y): piece
    for piece, (diameter, section, x, y) in models.canonical_piece_cells().items()
}
BLOCKSTATE_CACHE = {}


def resolve_selector(block: str, state: dict[str, str]):
    """按Minecraft blockstate选择器的属性子集语义解析具体状态。"""
    if block not in BLOCKSTATE_CACHE:
        path = ASSETS / "blockstates" / f"{block}.json"
        BLOCKSTATE_CACHE[block] = json.loads(path.read_text(encoding="utf-8"))["variants"]
    variants = BLOCKSTATE_CACHE[block]
    matches = []
    for selector, value in variants.items():
        requirements = dict(part.split("=", 1) for part in selector.split(",") if part)
        if all(state.get(name) == expected for name, expected in requirements.items()):
            matches.append((selector, value))
    if len(matches) != 1:
        raise ValueError(f"状态选择不唯一: {block} {state}, 匹配数={len(matches)}")
    return matches[0][1]


def load_blockstate_model(block: str, key: str):
    """按资源方块状态路径解析最终模型，再读取安装态OBJ网格。"""
    state = dict(part.split("=", 1) for part in key.split(","))
    state.setdefault("formed", "false")
    choice = resolve_selector(block, state)
    wrapper_ref = choice["model"].split(":", 1)[1]
    wrapper_path = ASSETS / "models" / f"{wrapper_ref}.json"
    wrapper = json.loads(wrapper_path.read_text(encoding="utf-8"))
    obj_ref = wrapper["model"].split(":", 1)[1]
    obj_path = ASSETS / obj_ref
    mesh = models.Mesh(obj_path.stem, bounds=False)
    for material, points, normal in models.parse_obj(obj_path.read_bytes()):
        mesh.face(points, material, normal)
    return mesh, choice


def placed_piece(block, diameter, section, x, y, z, window=False):
    """通过located=true且formed=false状态键取真实网格，模拟运行前已定位部件。"""
    piece = PIECE_IDS[(diameter, section, x, y)]
    name = "turbine_window" if window else "turbine_casing"
    mesh, _ = load_blockstate_model(name, f"located=true,machine_facing={FACING},piece={piece}")
    return mesh, (x - 0.5, y - 0.5, z)


def end_cells(diameter):
    radius = (diameter - 1) // 2
    return [(x, y) for y in range(-radius, radius + 1) for x in range(-radius, radius + 1)
            if models.polygon_area(models.clip_to_cell(models.octagon(diameter), x, y)) > 1e-8
            and (x, y) != (0, 0)]


def axis_parts():
    """解析运行未成型的真实located轴状态，并加入A使用的d3叶轮partial。"""
    result = []
    for end, z in (("front", 0), ("rear", 4)):
        mesh, _ = load_blockstate_model(
            "turbine_output_shaft", f"end={end},located=true,machine_facing={FACING}")
        result.append((mesh, (-0.5, -0.5, z)))
    axle, _ = load_blockstate_model(
        "turbine_rotor", f"diameter=d3,located=true,machine_facing={FACING}")
    blades = models.Mesh("rotor_blades_d3", bounds=False)
    blade_path = ASSETS / "models/block/turbine/mesh/rotor_blades_d3.obj"
    for material, points, normal in models.parse_obj(blade_path.read_bytes()):
        blades.face(points, material, normal)
    for z in (1, 2, 3):
        result.extend(((axle, (-0.5, -0.5, z)), (blades, (-0.5, -0.5, z))))
    return result


def casing_parts(stage):
    """根据指定阶段组合小型真实piece模型；窗和运行formed均走独立状态路径。"""
    result = []
    for z in range(5):
        section = "front" if z == 0 else "rear" if z == 4 else "middle"
        cells = end_cells(3) if section != "middle" else models.shell_cells(3)
        for x, y in cells:
            # 半包壳拆去前端环盖和其余五面，只保留一条侧壁，让内部叶轮直接可见。
            if stage == "半包壳" and (section == "front" or x != -1):
                continue
            # “缺一件”和“拆一件”都留出一个外侧网格；区别是拆除前状态为完成结构。
            if stage in ("缺一件", "拆一件") and (x, y, z) == (1, 0, 2):
                continue
            is_window = stage in ("完成", "拆一件") and section == "middle" and (x, y) == (0, 1)
            result.append(placed_piece("turbine_window" if is_window else "turbine_casing",
                                        3, section, x, y, z, is_window))
    return result


def draw_stage(draw, title, stage, box):
    """将资源OBJ按块位置绘制；每格外观来自实际located且formed=false状态选择。"""
    x0, y0, x1, y1 = box
    scale = min((x1 - x0) / (5 * 0.86 + 3 * 0.62 + 2),
                (y1 - y0) / (5 * 0.44 + 3 * 1.5 + 1))
    cx, cy = (x0 + x1) / 2, y0 + (y1 - y0) * 0.56
    transparent = Image.new("RGBA", draw._image.size, (0, 0, 0, 0))
    face_queue = []
    meshes = axis_parts()
    if stage != "轴列":
        meshes.extend(casing_parts(stage))
    for mesh, origin in meshes:
        models_01b.draw_mesh(draw, mesh, origin, (0.7, 1.0, 0.4), scale, cx, cy,
                             transparent, face_queue)
    for _, _, points, color in sorted(face_queue, key=lambda row: row[0]):
        draw.polygon(points, fill=color, outline=(83, 103, 108, 255))
    draw._image.alpha_composite(transparent)
    font = ImageFont.truetype(str(FONT_PATH), 19) if FONT_PATH.is_file() else ImageFont.load_default(size=19)
    status_font = ImageFont.truetype(str(FONT_PATH), 15) if FONT_PATH.is_file() else ImageFont.load_default(size=15)
    draw.text((x0 + 10, y0 + 8), title, font=font, fill="#E3ECE8")
    note = "定位外观；尚未运行成型"
    if stage == "完成":
        note = "壳体几何齐全（接口未装，formed=false）"
    draw.text((x0 + 10, y1 - 32), note, font=status_font, fill="#9EB4BF")


def verify_state_paths():
    """确认形成与否共用同一外观，六向独立板不受machine_facing二次旋转。"""
    checked = 0
    for block in ("turbine_casing", "turbine_window"):
        for located, facing, piece in ((located, facing, number)
                                       for located in (False, True)
                                       for facing in ("north", "east", "south", "west")
                                       for number in range(212)):
            selected = []
            for formed in ("false", "true"):
                state = {"located": str(located).lower(), "formed": formed,
                         "machine_facing": facing, "piece": str(piece)}
                selected.append(resolve_selector(block, state))
            if selected[0] != selected[1]:
                raise AssertionError((block, facing, piece, selected))
            checked += 2
        for face, piece in FACE_CODES.items():
            models_for_face = []
            for facing in ("north", "east", "south", "west"):
                state = {"located": "false", "formed": "false",
                         "machine_facing": facing, "piece": str(piece)}
                models_for_face.append(resolve_selector(block, state))
            if len({choice["model"] for choice in models_for_face}) != 1:
                raise AssertionError((block, piece, face, models_for_face))
            if any("y" in choice for choice in models_for_face):
                raise AssertionError(("独立面不得叠加机身朝向", block, piece, face))
            checked += 4
    ordinary = {
        "turbine_rotor": [f"diameter={diameter},located=true,machine_facing={facing}"
                           for diameter in ("d3", "d5", "d7") for facing in ("north", "east", "south", "west")],
        "turbine_output_shaft": [f"end={end},located=true,machine_facing={facing}"
                                 for end in ("front", "rear") for facing in ("north", "east", "south", "west")],
        "turbine_controller": [f"located=true,machine_facing={facing},side={side}"
                               for facing in ("north", "east", "south", "west")
                               for side in ("up", "down", "left", "right")],
    }
    for block, keys in ordinary.items():
        for key in keys:
            base = dict(part.split("=", 1) for part in key.split(","))
            choices = [resolve_selector(block, {**base, "formed": formed})
                       for formed in ("false", "true")]
            if choices[0] != choices[1]:
                raise AssertionError((block, key, choices))
            checked += 2
    for block, kind in (("turbine_inlet", "inlet"), ("turbine_exhaust", "exhaust")):
        for located in ("false", "true"):
            for facing in ("north", "east", "south", "west"):
                for role in ("top", "upper_left", "upper_right", "left", "right",
                             "lower_left", "lower_right", "bottom"):
                    for outward in ("north", "east", "south", "west", "up", "down"):
                        key = {"located": located, "machine_facing": facing,
                               "ring_role": role, "outward": outward}
                        choices = [resolve_selector(block, {**key, "formed": formed})
                                   for formed in ("false", "true")]
                        if choices[0] != choices[1]:
                            raise AssertionError((block, key, choices))
                        checked += 2
    collision_envelopes = {
        "up": ("y", 0.8125, 1.0), "down": ("y", 0.0, 0.1875),
        "north": ("z", 0.0, 0.1875), "south": ("z", 0.8125, 1.0),
        "west": ("x", 0.0, 0.1875), "east": ("x", 0.8125, 1.0),
    }
    for block in ("turbine_casing", "turbine_window"):
        for face, (axis, low, high) in collision_envelopes.items():
            suffix = "" if face == "up" else f"_{face}"
            obj = ASSETS / f"models/block/turbine/mesh/{block}_unformed{suffix}.obj"
            points = [point for _, face_points, _ in models.parse_obj(obj.read_bytes())
                      for point in face_points]
            index = {"x": 0, "y": 1, "z": 2}[axis]
            actual_low = min(point[index] for point in points)
            actual_high = max(point[index] for point in points)
            if abs(actual_low - low) > 1e-6 or abs(actual_high - high) > 1e-6:
                raise AssertionError((block, face, actual_low, actual_high, low, high))
    for block in (*models.BLOCKS, "turbine_window"):
        item_path = ASSETS / f"models/item/{block}.json"
        item = json.loads(item_path.read_text(encoding="utf-8"))
        parent = item["parent"].split(":", 1)[1]
        if not (ASSETS / f"models/{parent}.json").is_file():
            raise AssertionError((block, parent))
        required = {"gui", "ground", "fixed", "thirdperson_righthand", "firstperson_righthand"}
        if not required <= set(item.get("display", {})):
            raise AssertionError((block, item.get("display", {})))
    return {"full_state_paths_checked": checked,
            "located_formed_visual_independence": "PASS",
            "independent_face_world_orientation": "PASS",
            "independent_model_collision_envelopes": "PASS",
            "hand_gui_item_models": "PASS"}


def tier_section_preview(models_01b):
    """从安装资源OBJ绘制中、大档完整体与开壳剖面，核对大直径叶轮空间。"""
    models_01b.EXPORTED_FILES = {
        f"assets/create_nuclear_industry/models/block/turbine/mesh/{path.name}": path.read_bytes()
        for path in (ASSETS / "models/block/turbine/mesh").glob("*.obj")
    }
    models_01b.MESH_CACHE.clear()
    canvas = Image.new("RGBA", (1900, 900), "#17212A")
    draw = ImageDraw.Draw(canvas)
    draw._image = canvas
    font = ImageFont.truetype(str(FONT_PATH), 21) if FONT_PATH.is_file() else ImageFont.load_default(size=21)
    draw.text((32, 24), "EXT-B-TURBINE-01C / 中大型安装态OBJ截面检查", font=font, fill="#E3ECE8")
    for row, (diameter, length, rotors) in enumerate(((5, 8, 6), (7, 11, 9))):
        y = 105 + row * 385
        draw.text((35, y), f"D{diameter} / {length} axial blocks / {rotors} rotor stages", font=font, fill="#D9E5E2")
        models_01b.machine(draw, diameter, length, (35, y + 42, 880, y + 335), False)
        models_01b.machine(draw, diameter, length, (975, y + 42, 1840, y + 335), True)
        draw.text((60, y + 330), "完整几何", font=font, fill="#A6B9C2")
        draw.text((1000, y + 330), "开壳剖面 / 叶轮净空", font=font, fill="#A6B9C2")
    output = REPORT / "turbine-01c-tier-section-preview.png"
    canvas.save(output, format="PNG", optimize=False)
    return output


def main():
    """输出五阶段小型搭建过程图与真实状态选择摘要。"""
    global models_01b
    old_preview_path = ROOT / "turbine_01b_preview.py"
    old_spec = importlib.util.spec_from_file_location("turbine_models_01b_preview", old_preview_path)
    models_01b = importlib.util.module_from_spec(old_spec)
    sys.modules[old_spec.name] = models_01b
    old_spec.loader.exec_module(models_01b)
    state_summary = verify_state_paths()
    canvas = Image.new("RGBA", (2600, 620), "#17212A")
    draw = ImageDraw.Draw(canvas)
    draw._image = canvas
    title_font = ImageFont.truetype(str(FONT_PATH), 24) if FONT_PATH.is_file() else ImageFont.load_default(size=24)
    draw.text((32, 22), "EXT-B-TURBINE-01C / 安装态网格与状态选择", font=title_font, fill="#E3ECE8")
    stages = ("轴列", "半包壳", "缺一件", "完成", "拆一件")
    labels = ("轴列已定位，尚无外壳", "半包壳，露出内部叶轮", "缺一壳板，外观仍保留", "壳体几何齐全，接口未装", "拆件后仍保留定位外观")
    for index, (stage, label) in enumerate(zip(stages, labels)):
        x = 28 + index * 510
        draw_stage(draw, label, stage, (x, 74, x + 450, 568))
    REPORT.mkdir(parents=True, exist_ok=True)
    output = REPORT / "turbine-01c-small-assembly-preview.png"
    canvas.save(output, format="PNG", optimize=False)
    tier_output = tier_section_preview(models_01b)
    print(f"PASS: {output.relative_to(REPO)}")
    print(f"PASS: {tier_output.relative_to(REPO)}")
    (REPORT / "preview-state-check.json").write_text(
        json.dumps(state_summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
