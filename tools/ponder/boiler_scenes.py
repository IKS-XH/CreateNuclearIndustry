"""生成并校验高压锅炉三条 Ponder 故事线的完整结构模板。"""

from __future__ import annotations

import gzip
import struct
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
OUTPUT_DIR = ROOT / "src/main/resources/assets/create_nuclear_industry/ponder"
BOILER_ORIGIN = (3, 0, 3)
SIZE = (13, 9, 13)


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


def _string_tag(name: str, value: str) -> bytes:
    return _tag(8, name, _string(value))


def _palette_entry(block_id: str, properties: dict[str, str]) -> bytes:
    fields = [_string_tag("Name", block_id)]
    if properties:
        fields.append(_tag(10, "Properties", _compound([
            _string_tag(key, value) for key, value in sorted(properties.items())
        ])))
    return _compound(fields)


def _block_entry(position: tuple[int, int, int], state: int) -> bytes:
    return _compound([
        _tag(9, "pos", _list(3, [_int(value) for value in position])),
        _tag(3, "state", _int(state)),
    ])


def _add(blocks: dict[tuple[int, int, int], tuple[str, dict[str, str]]],
         position: tuple[int, int, int], block_id: str,
         properties: dict[str, str] | None = None) -> None:
    if position in blocks:
        raise ValueError(f"duplicate template position: {position}")
    blocks[position] = (block_id, properties or {})


def _replace(blocks: dict[tuple[int, int, int], tuple[str, dict[str, str]]],
             position: tuple[int, int, int], block_id: str,
             properties: dict[str, str] | None = None) -> None:
    if position not in blocks:
        raise ValueError(f"cannot replace missing boiler shell at {position}")
    blocks[position] = (block_id, properties or {})


def _boiler(with_front_windows: bool = True) -> dict[tuple[int, int, int], tuple[str, dict[str, str]]]:
    """建立7×7×7锅炉；隔层y=3，水区y=1～2，汽区y=4～5。"""
    blocks: dict[tuple[int, int, int], tuple[str, dict[str, str]]] = {}
    ox, oy, oz = BOILER_ORIGIN
    casing = "create_nuclear_industry:high_pressure_boiler_casing"
    for x in range(7):
        for y in range(7):
            for z in range(7):
                if x in (0, 6) or y in (0, 6) or z in (0, 6):
                    _add(blocks, (ox + x, oy + y, oz + z), casing)

    # 底层内格放炉内换热器，内部三层完整隔层由再加热段组成。
    for x in (2, 3, 4):
        _replace(blocks, (ox + x, oy, oz + 3), "create_nuclear_industry:nuclear_heat_exchanger")
    for y in (1, 2, 3, 4, 5):
        for x in (1, 2, 3, 4, 5):
            for z in (1, 2, 3, 4, 5):
                if y == 3:
                    _add(blocks, (ox + x, oy + y, oz + z),
                         "create_nuclear_industry:boiler_heat_exchange_section")

    # 所有端口均朝北，分置于同一可见侧的合法高度与底边非角点。
    for x, y, block_id in (
        (1, 1, "high_pressure_boiler_water_port"),
        (5, 2, "high_pressure_boiler_water_port"),
        (3, 2, "high_pressure_boiler_controller"),
        (1, 3, "high_pressure_boiler_cold_coolant_port"),
        (5, 3, "high_pressure_boiler_cold_coolant_port"),
        (1, 4, "high_pressure_boiler_steam_port"),
        (5, 4, "high_pressure_boiler_steam_port"),
    ):
        _replace(blocks, (ox + x, oy + y, oz), f"create_nuclear_industry:{block_id}",
                 {"facing": "north"})
    for x in (1, 5):
        _replace(blocks, (ox + x, oy, oz),
                 "create_nuclear_industry:high_pressure_boiler_hot_coolant_port", {"facing": "north"})
    _replace(blocks, (ox + 3, oy + 6, oz + 3), "create_nuclear_industry:boiler_safety_valve")

    # 正面观察窗替换侧面非棱边壳层，直接显示水区和汽区液位。
    if with_front_windows:
        for y in (1, 2, 4, 5):
            for x in (2, 4):
                _replace(blocks, (ox + x, oy + y, oz),
                         "create_nuclear_industry:high_pressure_boiler_window")
    return blocks


def _pipe(blocks: dict[tuple[int, int, int], tuple[str, dict[str, str]]],
          x: int, y: int, z: int) -> None:
    _add(blocks, (x, y, z), "create:fluid_pipe",
         {"north": "true", "south": "true", "waterlogged": "false"})


def _tank(blocks: dict[tuple[int, int, int], tuple[str, dict[str, str]]],
          x: int, y: int, z: int) -> None:
    _add(blocks, (x, y, z), "create:fluid_tank",
         {"top": "true", "bottom": "true", "shape": "window"})


def _operation_scene() -> dict[tuple[int, int, int], tuple[str, dict[str, str]]]:
    blocks = _boiler()
    ox, oy, oz = BOILER_ORIGIN
    # 给水与热液分别使用独立储罐、泵和可见短管；冷液回口接独立回收罐。
    for x, y in ((ox + 1, oy + 1), (ox + 5, oy)):
        _tank(blocks, x, y, 0)
        _add(blocks, (x, y, 1), "create:mechanical_pump",
             {"facing": "south", "waterlogged": "false"})
        _pipe(blocks, x, y, 2)
        # 每台泵由相邻齿轮与动力马达驱动，动力与流体路线保持分离。
        _add(blocks, (x + 1, y, 1), "create:cogwheel", {"axis": "z"})
        _add(blocks, (x + 1, y, 0), "create:creative_motor", {"facing": "south"})
    for x, y in ((ox + 1, oy + 3), (ox + 5, oy + 3)):
        _pipe(blocks, x, y, 2)
        _add(blocks, (x, y, 1), "create:mechanical_pump",
             {"facing": "north", "waterlogged": "false"})
        _tank(blocks, x, y, 0)
        _add(blocks, (x + 1, y, 1), "create:cogwheel", {"axis": "z"})
        _add(blocks, (x + 1, y, 0), "create:creative_motor", {"facing": "south"})
    _add(blocks, (6, 2, 2), "minecraft:lever",
         {"face": "wall", "facing": "north", "powered": "false"})
    return blocks


def _steam_scene() -> dict[tuple[int, int, int], tuple[str, dict[str, str]]]:
    blocks = _boiler()
    ox, oy, oz = BOILER_ORIGIN
    for x in (ox + 1, ox + 5):
        for z in (oz - 1, oz - 2):
            _pipe(blocks, x, oy + 4, z)
        _tank(blocks, x, oy + 4, oz - 3)
    return blocks


def _encode(size: tuple[int, int, int],
            blocks: dict[tuple[int, int, int], tuple[str, dict[str, str]]]) -> bytes:
    palette: list[tuple[str, dict[str, str]]] = [("minecraft:air", {})]
    state_key = lambda entry: (entry[0], tuple(sorted(entry[1].items())))
    indexes = {state_key(palette[0]): 0}
    for state in blocks.values():
        key = state_key(state)
        if key not in indexes:
            indexes[key] = len(palette)
            palette.append(state)
    entries = [_block_entry(position, indexes[state_key(state)])
               for position, state in sorted(blocks.items())]
    return _tag(10, "", _compound([
        _tag(9, "size", _list(3, [_int(value) for value in size])),
        _tag(9, "palette", _list(10, [_palette_entry(block, props) for block, props in palette])),
        _tag(9, "blocks", _list(10, entries)),
        _tag(9, "entities", _list(10, [])),
    ]))


def _read_string(data: bytes, offset: int) -> tuple[str, int]:
    length = struct.unpack_from(">H", data, offset)[0]
    offset += 2
    return data[offset:offset + length].decode("utf-8"), offset + length


def _read_payload(tag_type: int, data: bytes, offset: int):
    if tag_type == 1:
        return data[offset], offset + 1
    if tag_type == 2:
        return struct.unpack_from(">h", data, offset)[0], offset + 2
    if tag_type == 3:
        return struct.unpack_from(">i", data, offset)[0], offset + 4
    if tag_type == 4:
        return struct.unpack_from(">q", data, offset)[0], offset + 8
    if tag_type == 5:
        return struct.unpack_from(">f", data, offset)[0], offset + 4
    if tag_type == 6:
        return struct.unpack_from(">d", data, offset)[0], offset + 8
    if tag_type in (7, 11, 12):
        length = struct.unpack_from(">i", data, offset)[0]
        size = {7: 1, 11: 4, 12: 8}[tag_type]
        start = offset + 4
        return data[start:start + length * size], start + length * size
    if tag_type == 8:
        return _read_string(data, offset)
    if tag_type == 9:
        child_type, length = data[offset], struct.unpack_from(">i", data, offset + 1)[0]
        offset += 5
        values = []
        for _ in range(length):
            value, offset = _read_payload(child_type, data, offset)
            values.append(value)
        return (child_type, values), offset
    if tag_type == 10:
        values = {}
        while data[offset] != 0:
            child_type = data[offset]
            name, offset = _read_string(data, offset + 1)
            values[name], offset = _read_payload(child_type, data, offset)
        return values, offset + 1
    raise ValueError(f"unsupported NBT tag type {tag_type}")


def _decode(raw: bytes):
    if raw[0] != 10:
        raise ValueError("root tag must be a compound")
    name, offset = _read_string(raw, 1)
    if name:
        raise ValueError("Ponder structure root name must be empty")
    root, offset = _read_payload(10, raw, offset)
    if offset != len(raw):
        raise ValueError("trailing bytes after NBT root")
    return root


def _validate(raw: bytes, size: tuple[int, int, int],
              blocks: dict[tuple[int, int, int], tuple[str, dict[str, str]]]) -> int:
    root = _decode(raw)
    size_type, dimensions = root["size"]
    palette_type, palette = root["palette"]
    block_type, entries = root["blocks"]
    if size_type != 3 or dimensions != list(size):
        raise ValueError("template dimensions do not match the declared size")
    if palette_type != 10 or block_type != 10:
        raise ValueError("palette and blocks must be TAG_List<TAG_Compound>")
    if not palette or palette[0].get("Name") != "minecraft:air":
        raise ValueError("palette index zero must be air")
    ids = {entry.get("Name") for entry in palette}
    positions: set[tuple[int, int, int]] = set()
    for entry in entries:
        coords = entry.get("pos", (0, []))
        if coords[0] != 3 or len(coords[1]) != 3:
            raise ValueError("block positions must be three-int lists")
        position = tuple(coords[1])
        if position in positions:
            raise ValueError(f"duplicate template position: {position}")
        positions.add(position)
        if any(value < 0 or value >= size[index] for index, value in enumerate(position)):
            raise ValueError(f"block position outside template: {position}")
        state = entry.get("state", -1)
        if state < 0 or state >= len(palette):
            raise ValueError(f"palette state index out of range at {position}")
    if len(positions) != len(blocks):
        raise ValueError("encoded block count differs from source layout")
    ox, oy, oz = BOILER_ORIGIN
    boiler_ids = {"create_nuclear_industry:high_pressure_boiler_casing",
                  "create_nuclear_industry:high_pressure_boiler_window",
                  "create_nuclear_industry:high_pressure_boiler_water_port",
                  "create_nuclear_industry:high_pressure_boiler_steam_port",
                  "create_nuclear_industry:high_pressure_boiler_hot_coolant_port",
                  "create_nuclear_industry:high_pressure_boiler_cold_coolant_port",
                  "create_nuclear_industry:boiler_safety_valve",
                  "create_nuclear_industry:boiler_heat_exchange_section",
                  "create_nuclear_industry:high_pressure_boiler_controller",
                  "create_nuclear_industry:nuclear_heat_exchanger"}
    if not boiler_ids <= ids:
        raise ValueError(f"missing boiler component IDs: {sorted(boiler_ids - ids)}")
    safety_position = (ox + 3, oy + 6, oz + 3)
    if blocks.get(safety_position, (None, {}))[0] != "create_nuclear_industry:boiler_safety_valve":
        raise ValueError("the safety valve must occupy a non-edge position on the top face")
    for (x, y, z), (block_id, properties) in blocks.items():
        local = (x - ox, y - oy, z - oz)
        if block_id.endswith(("_water_port", "_steam_port", "_cold_coolant_port",
                              "_hot_coolant_port", "_controller")):
            if properties.get("facing") != "north":
                raise ValueError(f"boiler side interface must face outward: {(x, y, z)}")
        if block_id.endswith("_hot_coolant_port") and not (local[1] == 0 and local[2] == 0 and 0 < local[0] < 6):
            raise ValueError(f"hot inlet must be on a non-corner bottom edge: {local}")
        if block_id.endswith("_cold_coolant_port") and not (local[1] == 3 and local[2] == 0 and 0 < local[0] < 6):
            raise ValueError(f"cold outlet must share the partition layer on a side face: {local}")
        if block_id.endswith("_water_port") and local[1] >= 3:
            raise ValueError(f"water port must be in the water zone: {local}")
        if block_id.endswith("_steam_port") and local[1] <= 3:
            raise ValueError(f"steam port must be in the steam zone: {local}")
        if block_id.endswith("_controller") and local[1] >= 3:
            raise ValueError(f"controller must be in the water zone: {local}")
        if block_id == "create:mechanical_pump":
            expected_facing = "south" if (x, y, z) in {(4, 1, 1), (8, 0, 1)} else "north"
            if (x, y, z) not in {(4, 1, 1), (8, 0, 1), (4, 3, 1), (8, 3, 1)}:
                raise ValueError(f"pump is outside the demonstrated fluid routes: {(x, y, z)}")
            if properties.get("facing") != expected_facing:
                raise ValueError(f"pump direction does not match its route: {(x, y, z)}")
        if "fluid_pipe" in block_id and ox <= x <= ox + 6 and oz <= z <= oz + 6:
            if 1 <= y <= 5:
                raise ValueError(f"external pipe intersects a boiler chamber: {(x, y, z)}")
    if len(entries) != len(positions):
        raise ValueError("duplicate block positions in NBT")
    if len([block for block in blocks.values() if block[0] == "create:mechanical_pump"]) not in (0, 4):
        raise ValueError("fluid-loop scenes must show all four driven pumps")
    return len(entries)


def _write(name: str, blocks: dict[tuple[int, int, int], tuple[str, dict[str, str]]]) -> tuple[Path, int]:
    raw = _encode(SIZE, blocks)
    path = OUTPUT_DIR / f"{name}.nbt"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(gzip.compress(raw, mtime=0))
    with gzip.open(path, "rb") as template:
        decoded = template.read()
    if decoded != raw:
        raise ValueError(f"gzip round trip changed {path.name}")
    return path, _validate(decoded, SIZE, blocks)


def main() -> None:
    scenes = (
        ("high_pressure_boiler_build", _boiler()),
        ("high_pressure_boiler_operation", _operation_scene()),
        ("high_pressure_boiler_steam", _steam_scene()),
    )
    for name, blocks in scenes:
        path, count = _write(name, blocks)
        print(f"{path.relative_to(ROOT)}: size={SIZE}; blocks={count}; NBT palette, indices, positions and legal boiler ports valid")


if __name__ == "__main__":
    main()
