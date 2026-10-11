"""锅炉候选素材的小型只读检查；不重建图集、不写游戏目录。"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path

from PIL import Image

import build_assets as art

ROOT = Path(__file__).resolve().parent
WORKSPACE = ROOT.parents[2]
R1_MAPPING = WORKSPACE / "tools/art-assets/reactor-ct-r1/mapping.json"


def expect(condition, message):
    if not condition:
        raise AssertionError(message)


def read_rgba(path):
    with Image.open(path) as image:
        return image.convert("RGBA").copy()


def main():
    report = {"base": 0, "rectangle_sources": 0, "window_sources": 0, "connected_target_ids": []}
    mapping = json.loads((ROOT / "mapping.json").read_text(encoding="utf-8"))
    locked = json.loads(R1_MAPPING.read_text(encoding="utf-8"))
    expect(mapping["source_sha256"] == hashlib.sha256(R1_MAPPING.read_bytes()).hexdigest(),
           "Create原生索引证据SHA与只读R1 mapping不一致")
    expect(mapping["rectangle_sheet"]["contexts"] == locked["rectangle_contexts"],
           "RECTANGLE索引必须逐项等于锁定表")
    expect(mapping["window_sheet"]["contexts"] == locked["window_contexts"],
           "OMNIDIRECTIONAL索引必须逐项等于锁定表")
    expect(mapping["window_sheet"]["fallback_indices"] == locked["window_fallback_indices"],
           "窗口回退槽必须逐项等于锁定表")
    expect(len(mapping["rectangle_sheet"]["contexts"]) == 16, "RECTANGLE上下文数量错误")
    expect(len(mapping["window_sheet"]["contexts"]) == 47, "原生窗口上下文数量错误")
    expect(len(mapping["window_sheet"]["fallback_indices"]) == 17, "原生窗口回退槽数量错误")
    expect(set(art.TARGETS.values()) == {item["target_sprite"] for item in mapping["connected_assets"].values()},
           "9个CT目标ID与已批准集合不一致")
    expect(len(mapping["base_assets"]) == 12, "基础纹理映射必须为12项")

    base_images = {}
    for name in art.BASE:
        source = ROOT / "sources/base" / f"{name}.svg"
        expected = art.parse_svg(source.read_text(encoding="utf-8"))
        actual = read_rgba(ROOT / "generated" / f"base_{name}.png")
        expect(actual.size == (16, 16) and list(actual.getdata()) == list(expected.getdata()), f"基础PNG与SVG不一致：{name}")
        base_images[name] = expected
        report["base"] += 1

    rect_contexts = mapping["rectangle_sheet"]["contexts"]
    for target in art.RECT_TARGETS:
        folder = ROOT / "sources/ct" / target
        atlas = read_rgba(ROOT / "generated" / f"ct_{target}.png")
        expect(atlas.size == (64, 64), f"RECTANGLE图集尺寸错误：{target}")
        for entry in rect_contexts:
            index = entry["index"]
            tile = art.parse_svg((folder / f"{index:02}.svg").read_text(encoding="utf-8"))
            actual = atlas.crop(((index % 4) * 16, (index // 4) * 16, (index % 4 + 1) * 16, (index // 4 + 1) * 16))
            expect(list(actual.getdata()) == list(tile.getdata()), f"图格与SVG不一致：{target}/{index:02}")
            if target in ("water_port_face", "steam_port_face", "controller_face", "hot_coolant_port", "cold_coolant_port"):
                base_name = {
                    "water_port_face": "water_port_face",
                    "steam_port_face": "steam_port_face",
                    "controller_face": "controller_face",
                    "hot_coolant_port": "high_pressure_boiler_hot_coolant_port",
                    "cold_coolant_port": "high_pressure_boiler_cold_coolant_port",
                }[target]
                for y in range(3, 13):
                    for x in range(3, 13):
                        expect(tile.getpixel((x, y)) == base_images[base_name].getpixel((x, y)),
                               f"CT端口中心功能图形偏离基础SVG：{target}/{index:02}/{x},{y}")
            report["rectangle_sources"] += 1

    window_dir = ROOT / "sources/ct/window_frame"
    window_atlas = read_rgba(ROOT / "generated/ct_window_frame.png")
    expect(window_atlas.size == (128, 128), "窗口OMNIDIRECTIONAL图集必须为128×128")
    for index in range(64):
        tile = art.parse_svg((window_dir / f"{index:02}.svg").read_text(encoding="utf-8"))
        actual = window_atlas.crop(((index % 8) * 16, (index // 8) * 16, (index % 8 + 1) * 16, (index // 8 + 1) * 16))
        expect(list(actual.getdata()) == list(tile.getdata()), f"窗口格与SVG不一致：{index:02}")
        report["window_sources"] += 1
    isolated = art.parse_svg((window_dir / "00.svg").read_text(encoding="utf-8"))
    expect(isolated.getpixel((8, 8))[3] == 0 and isolated.getpixel((0, 8))[3] == 255,
           "孤立窗必须留透明中孔并保留不透明外法兰")
    expect(list(base_images["valve_body"].getdata()) != list(base_images["safety_badge"].getdata()),
           "阀体材质与安全识别牌不得复用整枚阀剪影")

    preview = ROOT / "preview"
    scenes = {
        "window_single.png": (16, 16),
        "window_2x2.png": (32, 32),
        "window_L.png": (32, 32),
    }
    for filename, size in scenes.items():
        image = read_rgba(preview / filename)
        expect(image.size == size, f"窗口预览尺寸错误：{filename}")
    square = read_rgba(preview / "window_2x2.png")
    expect(square.getpixel((15, 8))[3] == 0 and square.getpixel((16, 8))[3] == 0,
           "2×2连窗共享竖缝不得出现内框")
    l_shape = read_rgba(preview / "window_L.png")
    expect(l_shape.getpixel((15, 24))[3] == 255, "L形缺角应在真实边界显示外法兰")
    expect(l_shape.getpixel((8, 15))[3] == 0, "L形内部相接边不得保留横向内框")
    expect(l_shape.getpixel((8, 16))[3] == 0, "L形对角缺块不得在已连接的上边留下框条")

    # 失败输入必须在内存中拒绝；本检查不执行写出函数。
    invalid = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges"><rect x="1.5" y="0" width="1" height="1" fill="#3D4549"/></svg>'
    try:
        art.parse_svg(invalid)
    except ValueError as error:
        report["negative_input"] = {"input": "x=1.5的非整数rect", "rejected": str(error)}
    else:
        raise AssertionError("检查器接受了非法非整数坐标")

    report["connected_target_ids"] = sorted(art.TARGETS.values())
    report["preview"] = str((preview / "index.html").resolve())
    report["result"] = "PASS"
    print("基础纹理：12/12 SVG-PNG像素一致")
    print("RECTANGLE：8图集、128/128 SVG-PNG格一致，64×64")
    print("OMNIDIRECTIONAL：47项原生上下文+17回退，64/64格一致，128×128")
    print("窗口：透明孤立孔、2×2无内框、L缺角外法兰通过")
    print(f"负例：{report['negative_input']['input']} -> {report['negative_input']['rejected']}")
    print("结果：PASS")
    print(json.dumps(report, ensure_ascii=False, sort_keys=True))


if __name__ == "__main__":
    main()
