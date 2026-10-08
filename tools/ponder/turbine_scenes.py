"""生成并校验汽轮机三条 Ponder 故事线的真实结构模板。"""

from __future__ import annotations

import gzip
import struct
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
OUTPUT_DIR = ROOT / "src/main/resources/assets/create_nuclear_industry/ponder"
BUILD_SIZE = (26, 10, 15)
FLOW_SIZE = (15, 9, 11)
TIERS = ((3, 3, 5), (5, 6, 8), (7, 9, 11))
PARTS = {
    "casing": "create_nuclear_industry:turbine_casing",
    "rotor": "create_nuclear_industry:turbine_rotor",
    "inlet": "create_nuclear_industry:turbine_inlet",
    "exhaust": "create_nuclear_industry:turbine_exhaust",
    "shaft": "create_nuclear_industry:turbine_output_shaft",
    "controller": "create_nuclear_industry:turbine_controller",
    "window": "create_nuclear_industry:turbine_window",
}


def footprint(diameter: int, x: int, y: int) -> bool:
    half = diameter / 2
    if abs(x) - 0.5 >= half or abs(y) - 0.5 >= half:
        return False
    near_x, near_y = max(0, abs(x) - 0.5), max(0, abs(y) - 0.5)
    return near_x + near_y < 2 * half - diameter / (2 + 2**0.5)


def shell_slot(diameter: int, x: int, y: int) -> bool:
    if not footprint(diameter, x, y):
        return False
    half = diameter / 2
    max_x, max_y = abs(x) + 0.5, abs(y) + 0.5
    inner_diagonal = 2 * half - diameter / (2 + 2**0.5) - 3 * 2**0.5 / 16
    return max_x > half - 3 / 16 or max_y > half - 3 / 16 or max_x + max_y > inner_diagonal


def enumerate_piece_ids() -> dict[tuple[int, str, int, int], int]:
    result: dict[tuple[int, str, int, int], int] = {}
    piece_id = 0
    for diameter in (3, 5, 7):
        radius = (diameter - 1) // 2
        for section in ("front", "middle", "rear"):
            for y in range(-radius, radius + 1):
                for x in range(-radius, radius + 1):
                    if not footprint(diameter, x, y) or section == "middle" and not shell_slot(diameter, x, y):
                        continue
                    piece_id += 1
                    result[(diameter, section, x, y)] = piece_id
    return result


PIECE_IDS = enumerate_piece_ids()


def add(blocks: dict, pos: tuple[int, int, int], block_id: str,
        props: dict[str, str] | None = None) -> None:
    if pos in blocks:
        if blocks[pos] == (block_id, props or {}):
            return
        raise ValueError(f"模板坐标冲突: {pos} / {blocks[pos]} / {block_id}")
    blocks[pos] = (block_id, props or {})


def replace(blocks: dict, pos: tuple[int, int, int], block_id: str,
            props: dict[str, str] | None = None) -> None:
    if pos not in blocks:
        raise ValueError(f"无法替换缺失的合法机壳槽: {pos}")
    blocks[pos] = (block_id, props or {})


def put_turbine(blocks: dict, front: tuple[int, int, int], diameter: int, rotors: int) -> None:
    fx, fy, fz = front
    radius, length = (diameter - 1) // 2, rotors + 2
    facing = "north"
    for z in range(length):
        section = "front" if z == 0 else "rear" if z == length - 1 else "middle"
        for y in range(-radius, radius + 1):
            for x in range(-radius, radius + 1):
                if not footprint(diameter, x, y):
                    continue
                pos = (fx + x, fy + y, fz + z)
                if z in (0, length - 1):
                    if x == 0 and y == 0:
                        end = "front" if z == 0 else "rear"
                        add(blocks, pos, PARTS["shaft"], {
                            "formed": "false", "located": "true", "machine_facing": facing, "end": end,
                        })
                    else:
                        add(blocks, pos, PARTS["casing"], {
                            "formed": "false", "located": "true", "machine_facing": facing,
                            "piece": str(PIECE_IDS[(diameter, section, x, y)]),
                        })
                elif x == 0 and y == 0:
                    add(blocks, pos, PARTS["rotor"], {
                        "formed": "false", "located": "true", "machine_facing": facing,
                        "diameter": f"d{diameter}",
                    })
                elif shell_slot(diameter, x, y):
                    add(blocks, pos, PARTS["casing"], {
                        "formed": "false", "located": "true", "machine_facing": facing,
                        "piece": str(PIECE_IDS[(diameter, section, x, y)]),
                    })

    mid = rotors // 2 + 1
    # 控制器占顶部中央行，进汽口在右侧中部，排汽口在左侧靠端内行。
    ports = (
        ((0, radius, mid), "controller", "up"),
        ((radius, 0, mid), "inlet", "east"),
        ((-radius, 0, 1), "exhaust", "west"),
    )
    for (x, y, z), kind, outward in ports:
        block_id = PARTS[kind]
        if kind == "controller":
            properties = {"formed": "false", "located": "true", "machine_facing": facing, "side": "up"}
        else:
            role = "top" if y > 0 else "bottom" if y < 0 else "right" if x > 0 else "left"
            properties = {"formed": "false", "located": "true", "machine_facing": facing,
                          "ring_role": role, "outward": outward}
        replace(blocks, (fx + x, fy + y, fz + z), block_id, properties)
    # 观察窗移到西侧中段合法槽，避免俯视镜头被机体底面遮住。
    replace(blocks, (fx - radius, fy, fz + mid), PARTS["window"], {
        "formed": "false", "located": "true", "machine_facing": facing,
        "piece": str(PIECE_IDS[(diameter, "middle", -radius, 0)]),
    })


def add_pipe(blocks: dict, pos: tuple[int, int, int]) -> None:
    add(blocks, pos, "create:fluid_pipe", {
        "east": "true", "north": "false", "south": "false",
        "waterlogged": "false", "west": "true",
    })


def add_tank(blocks: dict, pos: tuple[int, int, int]) -> None:
    add(blocks, pos, "create:fluid_tank", {"bottom": "true", "shape": "window", "top": "true"})


def build_scene() -> dict:
    blocks: dict = {}
    # 三档独立落在地台上，镜头按各自中心聚焦，避免悬空与边缘偏置。
    positions = ((8, 2, 5), (12, 3, 4), (20, 4, 3))
    for (diameter, rotors, _), front in zip(TIERS, positions, strict=True):
        add_pad(blocks, front, diameter, rotors + 2)
        put_turbine(blocks, front, diameter, rotors)
    return blocks


def add_pad(blocks: dict, front: tuple[int, int, int], diameter: int, length: int) -> None:
    fx, _, fz = front
    radius = (diameter - 1) // 2
    for x in range(fx - radius - 1, fx + radius + 2):
        for z in range(fz - 1, fz + length + 1):
            add(blocks, (x, 0, z), "minecraft:polished_andesite")


def add_flow_pad(blocks: dict) -> None:
    # 操作与效率幕的短管路、储罐和机体均由同一地台承托。
    for x in range(1, 14):
        for z in range(2, 10):
            add(blocks, (x, 0, z), "minecraft:polished_andesite")


def operation_scene() -> dict:
    blocks: dict = {}
    add_flow_pad(blocks)
    front = (7, 2, 3)
    put_turbine(blocks, front, 3, 3)
    fx, fy, fz = front
    # 进汽与排汽分别接独立管路和储罐，外接轴在两端同时可见。
    for x in range(fx + 2, fx + 5):
        add_pipe(blocks, (x, fy, fz + 2))
    add_tank(blocks, (fx + 5, fy, fz + 2))
    for x in range(fx - 4, fx - 1):
        add_pipe(blocks, (x, fy, fz + 1))
    add_tank(blocks, (fx - 5, fy, fz + 1))
    add(blocks, (fx, fy, fz - 1), "create:shaft", {"axis": "z"})
    add(blocks, (fx, fy, fz + 5), "create:shaft", {"axis": "z"})
    return blocks


def efficiency_scene() -> dict:
    blocks = {}
    add_flow_pad(blocks)
    front = (7, 2, 3)
    put_turbine(blocks, front, 3, 3)
    fx, fy, fz = front
    for x in range(fx + 2, fx + 5):
        add_pipe(blocks, (x, fy, fz + 2))
    add_tank(blocks, (fx + 5, fy, fz + 2))
    for x in range(fx - 4, fx - 1):
        add_pipe(blocks, (x, fy, fz + 1))
    add_tank(blocks, (fx - 5, fy, fz + 1))
    add(blocks, (fx, fy, fz - 1), "create:shaft", {"axis": "z"})
    add(blocks, (fx, fy, fz + 5), "create:shaft", {"axis": "z"})
    return blocks


def _string(value: str) -> bytes:
    data = value.encode("utf-8")
    return struct.pack(">H", len(data)) + data


def _tag(tag_type: int, name: str, payload: bytes) -> bytes:
    return bytes([tag_type]) + _string(name) + payload


def _int(value: int) -> bytes:
    return struct.pack(">i", value)


def _compound(entries: list[bytes]) -> bytes:
    return b"".join(entries) + b"\x00"


def _list(tag_type: int, values: list[bytes]) -> bytes:
    return bytes([tag_type]) + _int(len(values)) + b"".join(values)


def _str_tag(name: str, value: str) -> bytes:
    return _tag(8, name, _string(value))


def _palette_entry(block_id: str, props: dict[str, str]) -> bytes:
    children = [_str_tag("Name", block_id)]
    if props:
        children.append(_tag(10, "Properties", _compound([_str_tag(k, v) for k, v in sorted(props.items())])))
    return _compound(children)


def _block_entry(position: tuple[int, int, int], state: int) -> bytes:
    return _compound([_tag(9, "pos", _list(3, [_int(v) for v in position])), _tag(3, "state", _int(state))])


def encode(blocks: dict, size: tuple[int, int, int]) -> bytes:
    palette = [("minecraft:air", {})]
    indexes = {(palette[0][0], ()) : 0}
    key = lambda entry: (entry[0], tuple(sorted(entry[1].items())))
    for state in blocks.values():
        if key(state) not in indexes:
            indexes[key(state)] = len(palette)
            palette.append(state)
    entries = [_block_entry(pos, indexes[key(state)]) for pos, state in sorted(blocks.items())]
    root = _compound([
        _tag(9, "size", _list(3, [_int(v) for v in size])),
        _tag(9, "palette", _list(10, [_palette_entry(block, props) for block, props in palette])),
        _tag(9, "blocks", _list(10, entries)),
        _tag(9, "entities", _list(10, [])),
    ])
    return _tag(10, "", root)


def validate(name: str, blocks: dict) -> None:
    size = BUILD_SIZE if name == "steam_turbine_build" else FLOW_SIZE
    for pos, (block_id, props) in blocks.items():
        if any(v < 0 or v >= size[i] for i, v in enumerate(pos)):
            raise ValueError(f"{name} 坐标越界: {pos}")
        if block_id in (PARTS["casing"], PARTS["window"]):
            piece = int(props["piece"])
            if piece <= 0 or piece > max(PIECE_IDS.values()):
                raise ValueError(f"{name} 无效外壳片编号: {pos} {piece}")
        if block_id in (PARTS["inlet"], PARTS["exhaust"]):
            if props["outward"] not in ("east", "west", "up", "down"):
                raise ValueError(f"{name} 端口朝向不在可见侧: {pos}")
    ids = {state[0] for state in blocks.values()}
    if not set(PARTS.values()) <= ids:
        raise ValueError(f"{name} 缺少汽轮机部件: {set(PARTS.values()) - ids}")
    if name != "steam_turbine_build":
        assert blocks[(7, 2, 3)][0] == PARTS["shaft"]
        assert blocks[(7, 2, 8)][0] == "create:shaft"
        assert blocks[(12, 2, 5)][0] == "create:fluid_tank"
        assert blocks[(2, 2, 4)][0] == "create:fluid_tank"


def main() -> None:
    scenes = (
        ("steam_turbine_build", build_scene()),
        ("steam_turbine_operation", operation_scene()),
        ("steam_turbine_efficiency", efficiency_scene()),
    )
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    for name, blocks in scenes:
        validate(name, blocks)
        size = BUILD_SIZE if name == "steam_turbine_build" else FLOW_SIZE
        raw = encode(blocks, size)
        path = OUTPUT_DIR / f"{name}.nbt"
        path.write_bytes(gzip.compress(raw, mtime=0))
        if gzip.decompress(path.read_bytes()) != raw:
            raise ValueError(f"NBT压缩往返失败: {path}")
        print(f"{path.relative_to(ROOT)}: size={size}; blocks={len(blocks)}; legal turbine occupancy and NBT round-trip valid")


if __name__ == "__main__":
    main()
