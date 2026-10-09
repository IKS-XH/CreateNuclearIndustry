"""生成并校验核换热器五条 Ponder 故事线的结构模板。"""

from __future__ import annotations

import gzip
import struct
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
OUTPUT_DIR = ROOT / "src/main/resources/assets/create_nuclear_industry/ponder"
NS = "create_nuclear_industry:"
INTRODUCTION_SIZE = (11, 7, 13)
HEATING_SIZE = (15, 9, 13)
BOILER_SIZE = (15, 8, 13)
PROCESSING_SIZE = (15, 9, 13)
CONDENSATION_SIZE = (15, 8, 13)
MACHINE = NS + "nuclear_heat_exchanger"


def introduction_scene() -> dict:
    blocks: dict = {}
    size = INTRODUCTION_SIZE
    # 首幕只给本体和流路留一小块地台，避免大面积地台压缩主体。
    add_pad(blocks, size, margin=4)
    add(blocks, (5, 1, 5), MACHINE, {"facing": "north", "lit": "false"})
    add_tank(blocks, (5, 1, 1))
    add_tank(blocks, (5, 1, 11))
    for z in range(2, 5):
        add_pipe(blocks, (5, 1, z), "north", "south")
    for z in range(7, 11):
        add_pipe(blocks, (5, 1, z), "north", "south")
    # 串联的两台本体共用外端冷热液管路，朝向与单台一致。
    add(blocks, (5, 1, 6), MACHINE, {"facing": "north", "lit": "false"})
    return blocks


def add(blocks: dict[tuple[int, int, int], tuple[str, dict[str, str]]],
        pos: tuple[int, int, int], block_id: str,
        props: dict[str, str] | None = None) -> None:
    state = block_id, props or {}
    if pos in blocks and blocks[pos] != state:
        raise ValueError(f"模板坐标冲突: {pos} / {blocks[pos]} / {state}")
    blocks[pos] = state


def replace(blocks: dict, pos: tuple[int, int, int], block_id: str,
            props: dict[str, str] | None = None) -> None:
    if pos not in blocks:
        raise ValueError(f"无法替换缺失的锅炉壳格: {pos}")
    blocks[pos] = (block_id, props or {})


def add_pad(blocks: dict, size: tuple[int, int, int], margin: int = 1,
            excluded: set[tuple[int, int, int]] | None = None) -> None:
    excluded = excluded or set()
    for x in range(margin, size[0] - margin):
        for z in range(margin, size[2] - margin):
            if (x, 0, z) not in excluded:
                add(blocks, (x, 0, z), "minecraft:polished_andesite")


def add_pipe(blocks: dict, pos: tuple[int, int, int], *directions: str) -> None:
    props = {direction: "true" for direction in directions}
    props.update({direction: "false" for direction in ("east", "north", "south", "west")
                  if direction not in props})
    props["waterlogged"] = "false"
    add(blocks, pos, "create:fluid_pipe", props)


def add_tank(blocks: dict, pos: tuple[int, int, int]) -> None:
    add(blocks, pos, "create:fluid_tank", {"bottom": "true", "shape": "window", "top": "true"})


def heating_scene() -> dict:
    blocks: dict = {}
    size = HEATING_SIZE
    add_pad(blocks, size)
    # 两台同向北向相接；最北侧为冷液出口，最南侧为热液入口。
    for z in (5, 6):
        add(blocks, (7, 1, z), MACHINE, {"facing": "north", "lit": "false"})
        add_tank(blocks, (7, 2, z))
    add_tank(blocks, (7, 1, 1))
    add_tank(blocks, (7, 1, 10))
    # 2×2储罐直接位于两台换热器上方，西侧仅用一台蒸汽机展示最小合法锅炉。
    for x in (6, 7):
        for z in (5, 6):
            add_tank(blocks, (x, 2, z))
    add(blocks, (5, 2, 5), "create:steam_engine",
        {"face": "wall", "facing": "west", "waterlogged": "false"})
    # Create 按 facing 两格处查找 PoweredShaftBlockEntity；垂直轴才能驱动蒸汽机活塞渲染。
    add(blocks, (3, 2, 5), "create:powered_shaft", {"axis": "y"})
    for z in (2, 3, 4):
        add_pipe(blocks, (7, 1, z), "north", "south")
    for z in (7, 8, 9):
        add_pipe(blocks, (7, 1, z), "north", "south")
    return blocks


def boiler_scene() -> dict:
    blocks: dict = {}
    size = BOILER_SIZE
    add_pad(blocks, size)
    # 5×5×5是现行合法最小外壳；结构最低层从地台上方y=1开始。
    for x in range(5, 10):
        for y in range(1, 6):
            for z in range(5, 10):
                edges = sum((x in (5, 9), y in (1, 5), z in (5, 9)))
                if edges >= 2:
                    add(blocks, (x, y, z), NS + "high_pressure_boiler_casing")
                elif edges == 1:
                    add(blocks, (x, y, z), NS + "high_pressure_boiler_casing")

    # 底面正中换热器、隔层正中再加热段；端口位于合法非角位置并朝外。
    replace(blocks, (7, 1, 7), MACHINE, {"facing": "north", "lit": "false"})
    for x in range(6, 9):
        for z in range(6, 9):
            if (x, z) != (7, 7):
                add(blocks, (x, 3, z), NS + "high_pressure_boiler_casing")
    add(blocks, (7, 3, 7), NS + "boiler_heat_exchange_section")
    replace(blocks, (7, 1, 5), NS + "high_pressure_boiler_hot_coolant_port", {"facing": "north"})
    replace(blocks, (6, 3, 5), NS + "high_pressure_boiler_cold_coolant_port", {"facing": "north"})
    replace(blocks, (6, 2, 5), NS + "high_pressure_boiler_water_port", {"facing": "north"})
    replace(blocks, (8, 2, 5), NS + "high_pressure_boiler_controller", {"facing": "north"})
    replace(blocks, (6, 4, 5), NS + "high_pressure_boiler_steam_port", {"facing": "north"})
    replace(blocks, (7, 5, 7), NS + "boiler_safety_valve")

    for z in (2, 3, 4):
        add_pipe(blocks, (6, 3, z), "north", "south")
        add_pipe(blocks, (7, 1, z), "north", "south")
    add_tank(blocks, (6, 3, 1))
    add_tank(blocks, (7, 1, 1))
    return blocks


def processing_scene() -> dict:
    blocks: dict = {}
    size = PROCESSING_SIZE
    add_pad(blocks, size)
    add(blocks, (7, 1, 5), MACHINE, {"facing": "north", "lit": "false"})
    add(blocks, (7, 2, 5), "create:basin", {"facing": "down"})
    add(blocks, (7, 4, 5), "create:mechanical_mixer")
    add(blocks, (7, 5, 5), "create:shaft", {"axis": "y"})
    add_tank(blocks, (7, 1, 1))
    add_tank(blocks, (7, 1, 10))
    for z in (2, 3, 4):
        add_pipe(blocks, (7, 1, z), "north", "south")
    for z in (6, 7, 8, 9):
        add_pipe(blocks, (7, 1, z), "north", "south")
    return blocks


def condensation_scene() -> dict:
    blocks: dict = {}
    size = CONDENSATION_SIZE
    add_pad(blocks, size)
    add(blocks, (7, 1, 5), MACHINE, {"facing": "north", "lit": "false"})
    add(blocks, (7, 2, 5), "minecraft:snow_block")
    add_tank(blocks, (7, 1, 1))
    add_tank(blocks, (7, 1, 10))
    for z in (2, 3, 4):
        add_pipe(blocks, (7, 1, z), "north", "south")
    for z in (6, 7, 8, 9):
        add_pipe(blocks, (7, 1, z), "north", "south")
    return blocks


def _string(value: str) -> bytes:
    encoded = value.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded


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
        children.append(_tag(10, "Properties", _compound(
            [_str_tag(key, value) for key, value in sorted(props.items())])))
    return _compound(children)


def _block_entry(position: tuple[int, int, int], state: int) -> bytes:
    return _compound([_tag(9, "pos", _list(3, [_int(value) for value in position])),
                      _tag(3, "state", _int(state))])


def encode(blocks: dict, size: tuple[int, int, int]) -> bytes:
    palette = [("minecraft:air", {})]
    indexes = {(palette[0][0], ()) : 0}
    key = lambda state: (state[0], tuple(sorted(state[1].items())))
    for state in blocks.values():
        if key(state) not in indexes:
            indexes[key(state)] = len(palette)
            palette.append(state)
    entries = [_block_entry(pos, indexes[key(state)]) for pos, state in sorted(blocks.items())]
    root = _compound([
        _tag(9, "size", _list(3, [_int(value) for value in size])),
        _tag(9, "palette", _list(10, [_palette_entry(block, props) for block, props in palette])),
        _tag(9, "blocks", _list(10, entries)),
        _tag(9, "entities", _list(10, [])),
    ])
    return _tag(10, "", root)


def validate(name: str, blocks: dict, size: tuple[int, int, int]) -> None:
    for pos in blocks:
        if any(value < 0 or value >= size[axis] for axis, value in enumerate(pos)):
            raise ValueError(f"{name} 坐标越界: {pos} / {size}")
    ids = {state[0] for state in blocks.values()}
    if MACHINE not in ids or "create:fluid_pipe" not in ids or "create:fluid_tank" not in ids:
        raise ValueError(f"{name} 缺少换热器或冷热管路")
    if name == "nuclear_heat_exchanger_introduction":
        if blocks[(5, 1, 5)][0] != MACHINE or blocks[(5, 1, 6)][0] != MACHINE:
            raise ValueError("首幕必须展示单台本体与同向直列换热器")
        if blocks[(5, 1, 1)][0] != "create:fluid_tank" or blocks[(5, 1, 11)][0] != "create:fluid_tank":
            raise ValueError("首幕输入输出端储罐必须位于串联外端")
        if blocks[(5, 1, 5)][1].get("facing") != blocks[(5, 1, 6)][1].get("facing"):
            raise ValueError("首幕串联换热器必须同向")
    elif name == "nuclear_heat_exchanger_heating":
        if sum(state[0] == MACHINE for state in blocks.values()) != 2:
            raise ValueError("供热幕必须显示两台同向直列换热器")
        for pos in ((7, 1, 2), (7, 1, 3), (7, 1, 4), (7, 1, 7), (7, 1, 8), (7, 1, 9)):
            if blocks[pos][0] != "create:fluid_pipe":
                raise ValueError(f"供热幕外端管路缺格: {pos}")
        if not all(blocks[(7, 2, z)][0] == "create:fluid_tank" for z in (5, 6)):
            raise ValueError("供热幕需在换热器顶部显示Create储罐锅炉")
        if sum(state[0] == "create:fluid_tank" for state in blocks.values()) < 6:
            raise ValueError("供热幕Create锅炉需包含2×2储罐组和两端液罐")
        if not all(blocks.get((x, 2, z), (None,))[0] == "create:fluid_tank"
                   for x in (6, 7) for z in (5, 6)):
            raise ValueError("Create锅炉2×2储罐组缺格")
        engines = [state for state in blocks.values() if state[0] == "create:steam_engine"]
        if len(engines) != 1 or any(state[1].get("face") != "wall" or state[1].get("facing") != "west"
                                    for state in engines):
            raise ValueError("供热幕Create锅炉需显示合法侧接蒸汽机")
        if blocks.get((5, 2, 5), (None,))[0] != "create:steam_engine":
            raise ValueError("Create锅炉蒸汽机须侧接储罐西面")
        if blocks.get((3, 2, 5), (None,))[0] != "create:powered_shaft" or blocks[(3, 2, 5)][1].get("axis") != "y":
            raise ValueError("Create蒸汽机必须连接在其合法位置的垂直动力轴")
        if any(blocks[(7, 1, z)][0] != MACHINE for z in (5, 6)):
            raise ValueError("Create锅炉东侧储罐须直接位于两台换热器上方")
    elif name == "nuclear_heat_exchanger_boiler":
        required = {
            (7, 1, 7): MACHINE,
            (7, 3, 7): NS + "boiler_heat_exchange_section",
            (7, 1, 5): NS + "high_pressure_boiler_hot_coolant_port",
            (6, 3, 5): NS + "high_pressure_boiler_cold_coolant_port",
        }
        for pos, expected in required.items():
            if blocks.get(pos, (None,))[0] != expected:
                raise ValueError(f"锅炉内置格不符合结构合同: {pos} / {expected}")
        for x in range(6, 9):
            for z in range(6, 9):
                expected = NS + "boiler_heat_exchange_section" if (x, z) == (7, 7) else NS + "high_pressure_boiler_casing"
                if blocks.get((x, 3, z), (None,))[0] != expected:
                    raise ValueError(f"锅炉隔层内部不完整: {(x, 3, z)}")
        for x in range(5, 10):
            for z in range(5, 10):
                for y in (1, 5):
                    if blocks.get((x, y, z), (None,))[0] is None:
                        raise ValueError(f"内置锅炉上移后边框层缺格: {(x, y, z)}")
        if not all(blocks.get((x, 1, z), (None,))[0] is not None for x in range(5, 10) for z in range(5, 10)):
            raise ValueError("内置锅炉底层必须完整")
    elif name == "nuclear_heat_exchanger_processing":
        if blocks[(7, 2, 5)][0] != "create:basin" or blocks[(7, 4, 5)][0] != "create:mechanical_mixer":
            raise ValueError("加工幕缺少工作盆或搅拌器")
        if (7, 3, 5) in blocks:
            raise ValueError("加工幕搅拌器与工作盆之间必须留一格空隙")
        if blocks[(7, 5, 5)][0] != "create:shaft":
            raise ValueError("加工幕搅拌器转轴位置未同步上移")
    elif name == "nuclear_heat_exchanger_condensation":
        if blocks[(7, 2, 5)][0] != "minecraft:snow_block":
            raise ValueError("冷凝幕缺少顶部冷源起始状态")


def main() -> None:
    scenes = (
        ("nuclear_heat_exchanger_introduction", introduction_scene(), INTRODUCTION_SIZE),
        ("nuclear_heat_exchanger_heating", heating_scene(), HEATING_SIZE),
        ("nuclear_heat_exchanger_boiler", boiler_scene(), BOILER_SIZE),
        ("nuclear_heat_exchanger_processing", processing_scene(), PROCESSING_SIZE),
        ("nuclear_heat_exchanger_condensation", condensation_scene(), CONDENSATION_SIZE),
    )
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    for name, blocks, size in scenes:
        validate(name, blocks, size)
        raw = encode(blocks, size)
        path = OUTPUT_DIR / f"{name}.nbt"
        path.write_bytes(gzip.compress(raw, mtime=0))
        if gzip.decompress(path.read_bytes()) != raw:
            raise ValueError(f"NBT压缩往返失败: {path}")
        print(f"{path.relative_to(ROOT)}: size={size}; blocks={len(blocks)}; required layout and NBT round-trip valid")


if __name__ == "__main__":
    main()
