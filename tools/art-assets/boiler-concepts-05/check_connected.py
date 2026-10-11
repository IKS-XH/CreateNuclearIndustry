from collections import Counter
from pathlib import Path
import hashlib
import json
import math
import re
import sys
import xml.etree.ElementTree as ET


SOURCE = Path(__file__).resolve().parent
PROJECT = SOURCE.parents[2]
CONNECTED = SOURCE / "connected"
EVIDENCE = PROJECT / "build/reports/art/ART-BOILER-05A-CT"
BASELINE = EVIDENCE / "old-source-baseline.json"
FORMAL_REFERENCE = PROJECT / "src/main/resources/assets/create_nuclear_industry/textures/block/reactor_casing_side.png"


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def local_name(tag):
    return tag.rsplit("}", 1)[-1]


def floats(value):
    return [float(item) for item in re.findall(r"[-+]?(?:\d*\.\d+|\d+)(?:[eE][-+]?\d+)?", value)]


def check_svg(path):
    root = ET.parse(path).getroot()
    require(local_name(root.tag) == "svg", f"SVG根节点无效：{path}")
    view_box = floats(root.attrib.get("viewBox", ""))
    require(len(view_box) == 4 and view_box[2] > 0 and view_box[3] > 0, f"viewBox无效：{path}")
    if root.attrib.get("width") != "16":
        require(root.attrib.get("preserveAspectRatio") == "xMidYMin meet", f"SVG缩放锚点无效：{path}")
    ids = {node.attrib["id"] for node in root.iter() if "id" in node.attrib}
    for node in root.iter():
        tag = local_name(node.tag)
        href = node.attrib.get("href") or node.attrib.get("{http://www.w3.org/1999/xlink}href")
        if href:
            if href.startswith("#"):
                require(href[1:] in ids, f"SVG片段引用不存在：{path} -> {href}")
            elif tag == "image":
                require((path.parent / href).resolve().is_file(), f"图片引用不存在：{path} -> {href}")
        for key in ("x", "y", "width", "height", "font-size", "stroke-width"):
            if key in node.attrib:
                values = floats(node.attrib[key])
                require(len(values) == 1 and math.isfinite(values[0]), f"数值属性无效：{path} {key}={node.attrib[key]}")
        if "transform" in node.attrib:
            transforms = re.findall(r"(matrix|translate|scale)\(([^)]*)\)", node.attrib["transform"])
            require(transforms, f"变换语法无效：{path} {node.attrib['transform']}")
            for name, args in transforms:
                values = floats(args)
                expected = {"matrix": (6,), "translate": (1, 2), "scale": (1, 2)}[name]
                require(len(values) in expected and all(math.isfinite(value) for value in values), f"变换数值无效：{path} {node.attrib['transform']}")
        if tag == "polygon":
            values = floats(node.attrib.get("points", ""))
            require(len(values) >= 6 and len(values) % 2 == 0, f"多边形坐标无效：{path}")
    return root


def parse_pair(value):
    values = floats(value)
    require(len(values) == 2, f"投影顶点无效：{value}")
    return values[0], values[1]


def parse_matrix(value):
    match = re.fullmatch(r"matrix\(([^)]*)\)", value or "")
    require(match is not None, f"矩阵变换无效：{value}")
    numbers = floats(match.group(1))
    require(len(numbers) == 6 and all(math.isfinite(number) for number in numbers), f"矩阵参数无效：{value}")
    return numbers


def near(a, b):
    return abs(a - b) < 0.02


def neighbor_mask(x, y, cells):
    mask = 0
    for bit, (dx, dy) in enumerate(((0, -1), (1, 0), (0, 1), (-1, 0))):
        if (x + dx, y + dy) in cells:
            mask |= 1 << bit
    return mask


def check_modules():
    palette_by_style = {}
    connected_files = sorted(CONNECTED.glob("*/*.svg"))
    # 每个方案包含四张连接tile和一张单稿board，因此此处合计15张SVG。
    require(len(connected_files) == 15, f"连接模块/单稿SVG数量异常：{len(connected_files)}")
    expected_names = {"casing_side.svg", "casing_side_boundary.svg", "casing_top.svg", "window_module.svg", "board.svg"}
    for style, original in (("A", "01-horizontal-hoop"), ("B", "02-refractory-jacket"), ("C", "03-steel-frame")):
        old_tiles = list((SOURCE / original).glob("*.svg"))
        palette = set()
        for old in old_tiles:
            old_root = ET.parse(old).getroot()
            palette.update(node.attrib.get("fill") for node in old_root.iter() if local_name(node.tag) == "rect")
        palette_by_style[style] = palette
        files = {path.name for path in (CONNECTED / style).glob("*.svg")}
        require(files == expected_names, f"方案{style}文件集合无效：{sorted(files)}")
        for name in ("casing_side.svg", "casing_side_boundary.svg", "casing_top.svg", "window_module.svg"):
            path = CONNECTED / style / name
            root = check_svg(path)
            require(root.attrib.get("width") == "16" and root.attrib.get("height") == "16" and root.attrib.get("viewBox") == "0 0 16 16", f"连接tile画布无效：{path}")
            require(all(local_name(node.tag) == "rect" for node in list(root)), f"连接tile含非矩形图形：{path}")
            colors = {node.attrib.get("fill") for node in root.iter() if local_name(node.tag) == "rect"}
            require(len(colors) <= 14 and colors <= palette, f"连接tile超出原色板：{path}")
            for node in root.iter():
                if local_name(node.tag) == "rect":
                    for key in ("x", "y", "width", "height"):
                        number = float(node.attrib[key])
                        require(number.is_integer(), f"连接tile非整数矩形：{path} {key}")
                    require(int(node.attrib["width"]) > 0 and int(node.attrib["height"]) > 0, f"连接tile空矩形：{path}")


def check_models(svg_paths):
    sizes = set()
    model_count = 0
    for path in svg_paths:
        root = check_svg(path)
        for model in root.iter():
            if "data-model-width" not in model.attrib:
                continue
            model_count += 1
            width = int(model.attrib["data-model-width"])
            depth = int(model.attrib["data-model-depth"])
            height = int(model.attrib["data-model-height"])
            sizes.add((width, depth, height))
            back = parse_pair(model.attrib["data-back"])
            left = parse_pair(model.attrib["data-left"])
            right = parse_pair(model.attrib["data-right"])
            front = parse_pair(model.attrib["data-front"])
            # 冻结预览只记录back；新生成器中的center与back同点。
            center = parse_pair(model.attrib.get("data-center", model.attrib["data-back"]))
            require(all(near(a, b) for a, b in zip(back, center)), f"back与顶面中心不一致：{path}")
            dx_l, dy_l = (back[0] - left[0]) / depth, (left[1] - back[1]) / depth
            dx_r, dy_r = (right[0] - back[0]) / width, (right[1] - back[1]) / width
            require(dx_l > 0 and dy_l > 0 and near(dx_l, dx_r) and near(dy_l, dy_r), f"非正方体面轴不一致：{path} {width}x{depth}x{height}")
            expected_front = (back[0] + (width - depth) * dx_l, back[1] + (width + depth) * dy_l)
            require(all(near(a, b) for a, b in zip(front, expected_front)), f"front未闭合：{path} {width}x{depth}x{height}")

            face_uses = [node for node in model.iter() if local_name(node.tag) == "use" and node.attrib.get("data-face") in {"left", "right", "top"}]
            face_counts = Counter(node.attrib.get("data-face") for node in face_uses)
            require(face_counts == Counter({"left": width * height, "right": depth * height, "top": width * depth}), f"面tile数量错误：{path} {face_counts}")
            require(int(model.attrib["data-left-tiles"]) == width * height and int(model.attrib["data-right-tiles"]) == depth * height and int(model.attrib["data-top-tiles"]) == width * depth, f"面计数标记错误：{path}")

            for face, grid_width, grid_height in (("left", width, height), ("right", depth, height), ("top", width, depth)):
                cells = []
                for node in face_uses:
                    if node.attrib.get("data-face") != face:
                        continue
                    a, b, c, d, x, y = parse_matrix(node.attrib.get("transform"))
                    dx, dy = a * 16, b * 16
                    if face == "left":
                        col = (x - left[0]) / dx
                        row = (y - left[1] - col * dy) / (d * 16)
                    elif face == "right":
                        col = (x - front[0]) / dx
                        row = (y - front[1] + col * abs(dy)) / (d * 16)
                    else:
                        u, v = (x - back[0]) / dx, (y - back[1]) / dy
                        col, row = (u + v) / 2, (v - u) / 2
                    require(near(col, round(col)) and near(row, round(row)), f"{face}面tile未落在整数网格：{path}")
                    cells.append((round(col), round(row), int(node.attrib["data-edge-mask"]), node.attrib.get("data-kind")))
                require(len({(col, row) for col, row, _, _ in cells}) == grid_width * grid_height, f"{face}面存在重复或缺失网格：{path}")
                full_cells = {(col, row) for col, row, _, _ in cells}
                window_cells = {(col, row) for col, row, _, kind in cells if kind == "window"}
                for col, row, mask, kind in cells:
                    neighbors = window_cells if kind == "window" else full_cells
                    require(mask == neighbor_mask(col, row, neighbors), f"{face}面邻接掩码错误：{path} cell={col},{row}")

            # 旧版冻结SVG未带辅助世界坐标标记；从每张面内tile的仿射矩阵反算列/屏幕行。
            tile_windows = {"left": [], "right": []}
            for node in face_uses:
                face = node.attrib.get("data-face")
                if face not in tile_windows or node.attrib.get("data-kind") != "window":
                    continue
                a, b, _, d, x, y = parse_matrix(node.attrib.get("transform"))
                dx, dy, vertical = a * 16, b * 16, d * 16
                if face == "left":
                    col = (x - left[0]) / dx
                    row = (y - left[1] - col * dy) / vertical
                else:
                    col = (x - front[0]) / dx
                    row = (y - front[1] - col * dy) / vertical
                require(near(col, round(col)) and near(row, round(row)), f"窗口不落在整数tile位置：{path} {face}")
                tile_windows[face].append((round(col), round(row), int(node.attrib["data-edge-mask"])))
            for face, face_width in (("left", width), ("right", depth)):
                windows = tile_windows[face]
                require(len(windows) == 4, f"{face}面连窗不是2×2：{path}")
                world_y = [height - 1 - row for _, row, _ in windows]
                require(Counter(world_y) == Counter({4: 2, 5: 2}), f"连窗不在隔层上方实际Y4/5：{path}")
                require(len({col for col, _, _ in windows}) == 2 and len({row for _, row, _ in windows}) == 2, f"连窗没有形成相邻2×2：{path}")
                require(Counter(mask for _, _, mask in windows) == Counter({3: 1, 6: 1, 9: 1, 12: 1}), f"连窗内部仍有分隔边：{path}")
                require(face_width >= 4, f"连窗所在面宽度不足：{path}")

            # 从冻结SVG中嵌入的原功能口symbol与父组矩阵反算实际面列、底部Y。
            parents = {child: parent for parent in model.iter() for child in parent}
            ports = {}
            for node in model.iter():
                if local_name(node.tag) != "use":
                    continue
                href = node.attrib.get("href", "")
                port = href.rsplit("-", 1)[-1]
                if port not in {"water_port", "cold_port", "steam_port", "hot_port", "controller_face"}:
                    continue
                group = parents.get(node)
                while group is not None and "transform" not in group.attrib:
                    group = parents.get(group)
                require(group is not None, f"接口缺少定位矩阵：{path} {port}")
                ports[port] = (group, parse_matrix(group.attrib["transform"]))
            expected_ports = {
                "water_port": ("left", 1, 1, width),
                "cold_port": ("left", 3, width - 2, width),
                "steam_port": ("right", height - 2, depth - 2, depth),
                "hot_port": ("right", 0, 1, depth),
                "controller_face": ("right", 1, depth // 2, depth),
            }
            require(set(ports) == set(expected_ports), f"功能口集合错误：{path}")
            for port, (face, world_y, column, face_width) in expected_ports.items():
                group, matrix = ports[port]
                a, b, _, d, x, y = matrix
                dx, dy, vertical = a * 16, abs(b * 16), d * 16
                if face == "left":
                    actual_column = (x - left[0]) / dx
                    actual_row = (y - left[1] - actual_column * dy) / vertical
                else:
                    actual_column = (x - front[0]) / dx
                    actual_row = (y - front[1] + actual_column * dy) / vertical
                require(near(actual_column, column) and near(height - 1 - actual_row, world_y), f"功能口实际位置错误：{path} {port}")
                require(0 < column < face_width - 1, f"功能口列落在角点：{path} {port}")

            valve_polys = [node for node in model.iter() if local_name(node.tag) == "polygon"]
            require(len(valve_polys) >= 15, f"安全阀棱柱面数不足：{path}")
            tier_top_levels = []
            valve_faces = valve_polys[-12:]
            _, _, _, vertical_scale, _, _ = parse_matrix(next(node for node in face_uses if node.attrib.get("data-face") == "left").attrib.get("transform"))
            vertical = vertical_scale * 16
            valve_center_x = back[0] + (width - depth) * dx_l / 2
            valve_center_y = back[1] + (width + depth) * dy_l / 2
            for tier in range(4):
                faces = valve_faces[tier * 3:tier * 3 + 3]
                require(len(faces) == 3, f"安全阀第{tier + 1}段侧面缺失：{path}")
                top_points = floats(faces[2].attrib.get("points", ""))
                require(len(top_points) == 8, f"安全阀顶面无效：{path} tier={tier + 1}")
                xs, ys = top_points[0::2], top_points[1::2]
                center_x, center_y = sum(xs) / 4, sum(ys) / 4
                require(near(center_x, valve_center_x), f"安全阀未落在顶面中心列：{path} tier={tier + 1}")
                level_value = (valve_center_y - center_y) * 16 / vertical
                level = round(level_value)
                require(near(level_value, level), f"安全阀层高非整数：{path} tier={tier + 1}")
                tier_top_levels.append(level)
            require(tier_top_levels == [7, 10, 14, 16], f"安全阀实际四段层高错误：{path} {tier_top_levels}")
            for node in valve_polys[-12:]:
                points = floats(node.attrib.get("points", ""))
                require(len(points) >= 8 and len(points) % 2 == 0, f"安全阀分段面坐标无效：{path}")
    require(sizes == {(5, 5, 7), (7, 5, 9)}, f"组合尺寸覆盖不正确：{sizes}")
    require(model_count == 9, f"模型组合数量不正确：{model_count}")
    return model_count


def check_original_sources():
    saved = json.loads(BASELINE.read_text(encoding="utf-8"))
    current = []
    for item in saved["files"]:
        path = PROJECT / item["path"]
        require(path.is_file() and digest(path) == item["sha256"], f"旧05A原稿字节变化：{item['path']}")
        current.append({"path": item["path"], "sha256": item["sha256"]})
    combined = hashlib.sha256("".join(item["path"] + item["sha256"] for item in current).encode()).hexdigest()
    require(combined == saved["combined_sha256"], "旧05A源文件组合哈希变化")
    return len(current), combined


def main():
    require(BASELINE.is_file(), "缺少旧源轻量组合哈希记录")
    require(FORMAL_REFERENCE.is_file(), "现行反应堆外壳只读参考不可读")
    svg_paths = sorted(CONNECTED.rglob("*.svg"))
    require(len(svg_paths) == 16, f"连接预览SVG数量异常：{len(svg_paths)}")
    for path in svg_paths:
        check_svg(path)
    check_modules()
    model_count = check_models([CONNECTED / "overview.svg", *sorted(CONNECTED.glob("*/board.svg"))])

    html_path = SOURCE / "index.html"
    html_text = html_path.read_text(encoding="utf-8")
    data_sources = re.findall(r'data-src="([^"]+)"', html_text)
    require(len(data_sources) == 8 and all((SOURCE / item).is_file() for item in data_sources), "首页连接/原稿切换入口无效")
    require(re.search(r'data="connected/overview\.svg"', html_text), "首页默认入口不是连接总览")
    require(not list(CONNECTED.rglob("*.png")), "连接稿目录出现PNG文件")
    old_count, old_hash = check_original_sources()

    stdout = [
        f"styles=3; connected_tiles=12; preview_svgs=16; models={model_count}; png_created=0",
        "svg_numeric_values/references=PASS; neighbor_boundaries/window_inner_frame=PASS; non_square_projection/face_tile_counts=PASS; matrix_derived_actual_y_ports/windows=PASS; four_tier_valve_projected_levels=PASS; old_sources=32/32 PASS; formal_reference=READ_ONLY",
    ]
    command = f"& '{sys.executable.replace(chr(92), '/')}' -B -X utf8 '{(SOURCE / 'check_connected.py').as_posix()}'"
    result = {
        "task": "ART-BOILER-05A-CT",
        "command": command,
        "exit_code": 0,
        "stdout": stdout,
        "models_checked": model_count,
        "legacy_source_file_count": old_count,
        "legacy_source_combined_sha256": old_hash,
        "formal_reference": FORMAL_REFERENCE.relative_to(PROJECT).as_posix(),
        "formal_reference_sha256": digest(FORMAL_REFERENCE),
        "png_created": 0,
    }
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    (EVIDENCE / "check-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    for line in stdout:
        print(line)


if __name__ == "__main__":
    main()
