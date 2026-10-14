"""15C有限资产检查：原元素唯一分区、OBB真实扫掠/接合、四向主控映射；不启动游戏。"""

from __future__ import annotations
import hashlib
import itertools
import json
import math
from pathlib import Path
import numpy as np

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
REPORT = ROOT / "build/reports/art/ART-SHIELDED-ASSEMBLY-15C/assets"
MODELS = ROOT / "src/main/resources/assets/create_nuclear_industry/models/block/shielded_assembly_station"
MEMBERS = {
    "left_base": ("left_arm", list(range(10)) + [28]), "left_tool": ("left_arm", list(range(10, 28))),
    "right_base": ("right_arm", list(range(10)) + [28]), "right_tool": ("right_arm", list(range(10, 28))),
    "fixture_base": ("fixture", list(range(10)) + [26, 27]), "fixture_carriage": ("fixture", list(range(10, 18))),
    "fixture_jaw_left": ("fixture", [18, 19, 22, 23]), "fixture_jaw_right": ("fixture", [20, 21, 24, 25]),
}


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def save(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def source_groups() -> dict:
    sources = {name: json.loads((MODELS / f"partials/{name}.json").read_text(encoding="utf-8")) for name in ("left_arm", "right_arm", "fixture")}
    return {group: [sources[source]["elements"][index] for index in indices] for group, (source, indices) in MEMBERS.items()}


def matrix(y: float = 0, z: float = 0) -> np.ndarray:
    y, z = math.radians(y), math.radians(z)
    ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    rz = np.array([[math.cos(z), -math.sin(z), 0], [math.sin(z), math.cos(z), 0], [0, 0, 1]])
    return ry @ rz


def obb(element: dict, translation=(0, 0, 0), pivot=(0, 0, 0), y=0, z=0) -> dict:
    rotation = matrix(y, z)
    lo, hi, p = np.array(element["from"], float), np.array(element["to"], float), np.array(pivot, float)
    center = rotation @ ((lo + hi) / 2 - p) + p + np.array(translation, float)
    half = (hi - lo) / 2
    corners = np.array([center + rotation @ (half * np.array(sign)) for sign in itertools.product((-1, 1), repeat=3)])
    return {"center": center, "half": half, "rotation": rotation, "corners": corners, "name": element["name"]}


def critical_angles(elements: list, pivot: list, axis: str, low: float, high: float) -> set:
    """单轴小角旋转的每个角点坐标是a cosθ+b sinθ；加入导数为0的临界角，连续包络不靠采样猜。"""
    values = {low, high, 0.}
    for element in elements:
        for corner in itertools.product(*zip(element["from"], element["to"])):
            x, y, z = np.array(corner) - np.array(pivot)
            roots = (math.atan2(-y, x), math.atan2(x, y)) if axis == "z" else (math.atan2(z, x), math.atan2(-x, z))
            for root in roots:
                for shift in range(-2, 3):
                    angle = math.degrees(root + shift * math.pi)
                    if low <= angle <= high:
                        values.add(angle)
    return values


def intersects(first: dict, second: dict) -> bool:
    """以15条OBB分离轴判定正体积相交；面相接不当作穿模，容差仅1e-7模型单位。"""
    if np.any(np.minimum(first["corners"].max(axis=0), second["corners"].max(axis=0)) -
              np.maximum(first["corners"].min(axis=0), second["corners"].min(axis=0)) <= 1e-7):
        return False
    a, b = first["rotation"], second["rotation"]
    axes = [a[:, k] for k in range(3)] + [b[:, k] for k in range(3)]
    axes += [np.cross(a[:, i], b[:, j]) for i in range(3) for j in range(3)]
    distance = second["center"] - first["center"]
    for axis in axes:
        if np.dot(axis, axis) < 1e-14:
            continue
        radius_a = np.sum(first["half"] * np.abs(a.T @ axis))
        radius_b = np.sum(second["half"] * np.abs(b.T @ axis))
        if radius_a + radius_b - abs(np.dot(distance, axis)) <= 1e-7:
            return False
    return True


def static_shell() -> list:
    elements = []
    for part in range(8):
        offset = np.array([16 * (part & 1), 16 * (part >> 2), 16 * ((part >> 1) & 1)])
        model = json.loads((MODELS / f"parts/part_{part}_off.json").read_text(encoding="utf-8"))
        elements += [{**element, "from": (np.array(element["from"]) + offset).tolist(),
                      "to": (np.array(element["to"]) + offset).tolist()} for element in model["elements"]]
    return elements


def exploration() -> dict:
    """先用未改原几何量出20～30°与安全小角的真实障碍，结果只作rig定界证据。"""
    groups = source_groups()
    rows = []
    for angle in (0, 14, 17, 20, 25, 30):
        moving = [obb(e, pivot=(10, 20, 14.5), z=angle) for e in groups["left_tool"]]
        contacts = []
        for group in ("left_base", "fixture_base", "fixture_carriage", "fixture_jaw_left", "fixture_jaw_right"):
            for first in moving:
                for e in groups[group]:
                    if intersects(first, obb(e)):
                        contacts.append([first["name"], group, e["name"]])
        tips = [v["corners"] for v in moving if "夹指" in v["name"]]
        extent = np.vstack(tips)
        rows.append({"zDegrees": angle, "positiveVolumeContacts": sorted({tuple(c) for c in contacts}),
                     "fingerBounds": [extent.min(axis=0).tolist(), extent.max(axis=0).tolist()]})
    result = {"task": "ART-SHIELDED-ASSEMBLY-15C", "unmodifiedGeometryExploration": rows,
              "scope": "接触项用实际原箱体OBB，不能删几何或硬放大角度；肩部钢芯局部嵌入须单独作为关节接合声明。"}
    save(REPORT / "motion-exploration.json", result)
    print(json.dumps(result, ensure_ascii=False))
    return result


def verify() -> dict:
    rig = json.loads((HERE / "rig.json").read_text(encoding="utf-8"))
    groups = source_groups()
    errors = []
    counts = {}
    for source in ("left_arm", "right_arm", "fixture"):
        actual = sorted(index for _, (name, indices) in MEMBERS.items() if name == source for index in indices)
        original = json.loads((MODELS / f"partials/{source}.json").read_text(encoding="utf-8"))
        if actual != list(range(len(original["elements"]))):
            errors.append("原元素未唯一分区:" + source)
    for name, definition in rig["groups"].items():
        candidate = json.loads((HERE / definition["candidate"]).read_text(encoding="utf-8"))
        original = json.loads((MODELS / f"partials/{MEMBERS[name][0]}.json").read_text(encoding="utf-8"))
        if candidate["elements"] != groups[name] or candidate["textures"] != original["textures"]:
            errors.append("分件几何/UV/槽漂移:" + name)
        counts[name] = len(candidate["elements"])
    shell = [obb(e) for e in static_shell()]
    fixed = [(name, obb(e)) for name in ("left_base", "right_base", "fixture_base") for e in groups[name]]
    findings = []
    envelopes = {}
    # 角度按0.5°、侧滑按极值/中点扫掠；与小角同轴的连续包络再由所有角点临界值补核。
    for name, definition in rig["groups"].items():
        if name.endswith("base"):
            continue
        samples = []
        if name in ("left_tool", "right_tool"):
            sign = 1 if name == "left_tool" else -1
            z_angles = set(np.linspace(min(0, 14 * sign), max(0, 14 * sign), 29)) | critical_angles(groups[name], definition["pivot"], "z", min(0, 14 * sign), max(0, 14 * sign))
            y_angles = set(np.linspace(-6, 6, 25)) | critical_angles(groups[name], definition["pivot"], "y", -6, 6)
            samples += [(t * sign, 0, 0, a) for t in (0, .125, .25) for a in z_angles]
            samples += [(t * sign, 0, a, 0) for t in (0, .75, 1.5) for a in y_angles]
        elif name == "fixture_carriage":
            samples = [(0, lift, 0, 0) for lift in np.linspace(0, 1, 21)]
        else:
            ranges = (-.5, .1) if name.endswith("left") else (-.1, .5)
            samples = [(x, lift, 0, 0) for x in np.linspace(*ranges, 13) for lift in (0, .5, 1)]
        corners = []
        for x, lift, ry, rz in samples:
            objects = [obb(e, (x, lift, 0), definition["pivot"], ry, rz) for e in groups[name]]
            for moving in objects:
                corners += list(moving["corners"])
                if np.any(moving["corners"] < -1e-7) or np.any(moving["corners"] > 32 + 1e-7):
                    errors.append("运动超出2³:" + name)
                for fixed_name, static in fixed:
                    if not intersects(moving, static):
                        continue
                    allowed = (name in ("left_tool", "right_tool") and fixed_name == name.replace("tool", "base") and
                               moving["name"] == "执行杆加工钢芯" and static["name"] == "肩中央关节体")
                    allowed |= (name == "fixture_carriage" and fixed_name == "fixture_base" and static["name"] == "升降中柱" and
                                moving["name"] in ("工作台锁固圈", "精加工台面"))
                    if not allowed:
                        findings.append([name, moving["name"], fixed_name, static["name"], x, lift, ry, rz])
                for static in shell:
                    if intersects(moving, static):
                        findings.append([name, moving["name"], "shell", static["name"], x, lift, ry, rz])
                if name in ("left_tool", "right_tool"):
                    for other in ("fixture_carriage", "fixture_jaw_left", "fixture_jaw_right"):
                        # 工具动作时升降必须为0；钳口按最大打开/最紧闭合的极值检查。
                        poses = (0,) if other == "fixture_carriage" else ((-.5, .1) if other.endswith("left") else (-.1, .5))
                        for jaw_x in poses:
                            for e in groups[other]:
                                if intersects(moving, obb(e, (jaw_x, 0, 0))):
                                    findings.append([name, moving["name"], other, e["name"], x, lift, ry, rz])
        array = np.array(corners)
        envelopes[name] = [array.min(axis=0).round(8).tolist(), array.max(axis=0).round(8).tolist()]
    if findings:
        errors.append("非允许运动接触:" + str(len(findings)))
    # 两种动作域都保持左右工具独立，保守整包络也不相交。
    if envelopes["left_tool"][1][0] >= envelopes["right_tool"][0][0]:
        errors.append("左右工具扫掠包络相交")
    # 北向0..32围绕主格中心8转向；这一校验只核模型坐标，B仍需用真实PoseStack核消费矩阵。
    facing_bounds = {}
    corners = list(itertools.product((0, 32), repeat=3))
    for facing, turns in (("north", 0), ("east", 1), ("south", 2), ("west", 3)):
        moved = []
        for x, y, z in corners:
            for _ in range(turns):
                x, z = 16 - z, x
            moved.append([x, y, z])
        points = np.array(moved)
        facing_bounds[facing] = [points.min(axis=0).tolist(), points.max(axis=0).tolist()]
    before = json.loads((REPORT / "before-identity.json").read_text(encoding="utf-8"))
    changed = [path for path, old in before.items() if sha(ROOT / path) != old["sha256"]]
    if changed:
        errors.append("原C/R1冻结源漂移")
    result = {"task": "ART-SHIELDED-ASSEMBLY-15C", "passed": not errors, "errors": sorted(set(errors)),
              "groupElementCounts": counts, "totalElements": sum(counts.values()), "sweptBounds": envelopes,
              "unexpectedContacts": findings, "allowedContacts": rig["allowedContacts"], "protectedChanged": changed,
              "continuousEnvelopeCriticalAnglesIncluded": True, "facingBoundsAroundMaster8": facing_bounds,
              "scope": "实际箱体OBB/SAT扫掠，原几何/UV唯一分组；B需核生产动作JSON连续插值和原生朝向，不等于Minecraft动画验收。"}
    save(REPORT / "checks.json", result)
    print(json.dumps({key: result[key] for key in ("passed", "errors", "groupElementCounts", "totalElements", "sweptBounds")}, ensure_ascii=False))
    return result


def verify_animation() -> dict:
    """只补核实际关键帧与smoothstep轨迹，复用已过几何域，不再生成/扫掠原模型。"""
    path = ROOT / "src/main/resources/assets/create_nuclear_industry/shielded_assembly/animation.json"
    raw = path.read_bytes()
    data = json.loads(raw)
    rig = json.loads((HERE / "rig.json").read_text(encoding="utf-8"))
    errors, counts, segments, samples = [], {}, 0, 0
    if data["rigSha256"] != sha(HERE / "rig.json") or data["domains"] != rig["motionDomains"] or data["easing"] != "smoothstep":
        errors.append("实际rig/动作域/easing不同")
    if data["facingPivot"] != [8, 0, 8] or data["rotationOrder"] != "XYZ":
        errors.append("实际朝向pivot或旋转顺序不同")
    for name, definition in rig["groups"].items():
        if data["groups"][name] != {key: definition[key] for key in ("pivot", "parent", "fixed")}:
            errors.append("实际组pivot/parent/fixed不同:" + name)
    def active(pose: dict) -> bool:
        return any(abs(x) > 1e-10 for name in ("left_tool", "right_tool") for channel in pose[name].values() for x in channel)
    def pressing(pose: dict) -> bool:
        return abs(pose["fixture_carriage"]["translation"][1]) > 1e-10
    def check(pose: dict, domain: str) -> None:
        for name, value in pose.items():
            if rig["groups"][name]["fixed"]:
                if any(abs(x) > 1e-10 for channel in value.values() for x in channel):
                    errors.append("固定组非零:" + name)
                continue
            bounds = rig["motionDomains"][domain if name.endswith("tool") else "fixture_press"][name]
            for channel in ("translation", "rotation"):
                for k, number in enumerate(value[channel]):
                    if not math.isfinite(number) or number < bounds[channel + "Min"][k] - 1e-10 or number > bounds[channel + "Max"][k] + 1e-10:
                        errors.append("实际轨迹越域:" + name)
        if pressing(pose) and active(pose):
            errors.append("实际轨迹混合工具和升降")
    for operation, definition in data["operations"].items():
        frames = definition["frames"]
        domain = "manufacture_align" if operation == "manufacture" else "sealing_lock"
        counts[operation] = len(frames)
        if definition["toolDomain"] != domain or frames[0]["at"] != 0 or frames[-1]["at"] != 1:
            errors.append("工序域或批次覆盖错误")
        poses = []
        for frame in frames:
            if not set(frame["pose"]) <= set(rig["groups"]):
                errors.append("未知分件")
            pose = {name: frame["pose"].get(name, {"translation": [0, 0, 0], "rotation": [0, 0, 0]}) for name in rig["groups"]}
            check(pose, domain)
            if operation == "sealing" and pressing(pose):
                errors.append("封装平台非固定")
            poses.append(pose)
        for edge in (poses[0], poses[-1]):
            if any(abs(x) > 1e-10 for value in edge.values() for channel in value.values() for x in channel):
                errors.append("批次端点非待机")
        for index, (first, second) in enumerate(zip(poses, poses[1:])):
            segments += 1
            if not 0 <= frames[index]["at"] < frames[index + 1]["at"] <= 1:
                errors.append("关键帧进度非严格递增")
            if (pressing(first) and active(second)) or (pressing(second) and active(first)):
                errors.append("相邻端点会混合工具与升降域")
            # 每分量共享s=3t²-2t³；域为凸区间，端点与跨域互斥成立即可约束完整连续区间。
            for t in (0, .25, .5, .75, 1):
                s = t * t * (3 - 2 * t)
                pose = {name: {channel: [a + (b - a) * s for a, b in zip(first[name][channel], second[name][channel])]
                               for channel in ("translation", "rotation")} for name in rig["groups"]}
                check(pose, domain)
                samples += 1
    if path.read_bytes() != raw:
        errors.append("检查期间生产JSON变化，当前快照不能绑定最终来源")
    result = {"passed": not errors, "errors": sorted(set(errors)), "animationSha256": hashlib.sha256(raw).hexdigest(),
              "rigSha256": data["rigSha256"], "keyframeCounts": counts, "totalKeyframes": sum(counts.values()),
              "interpolatedSegments": segments, "smoothstepSamples": samples,
              "continuousBoundReason": "每工序固定凸动作域，s=3t²-2t³在[0,1]单调；跨工具/升降的相邻端点另核。",
              "geometryEvidenceReused": "既有checks.json；未生成/扫掠旧模型。",
              "scope": "实际生产JSON姿态约束；Java/JS采样同源、时钟/GPU由B和负责人验证。"}
    save(REPORT / "actual-animation-checks.json", result)
    print(json.dumps(result, ensure_ascii=False))
    return result


if __name__ == "__main__":
    import sys
    if "--animation" in sys.argv:
        if not verify_animation()["passed"]:
            raise SystemExit(1)
    elif "--explore" in sys.argv:
        exploration()
    elif not verify()["passed"]:
        raise SystemExit(1)
