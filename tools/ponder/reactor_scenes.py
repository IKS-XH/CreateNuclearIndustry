"""生成并核验反应堆三条 Ponder 故事线的完整结构模板。"""

from __future__ import annotations

import gzip
import struct
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
OUTPUT_DIR = ROOT / "src/main/resources/assets/create_nuclear_industry/ponder"
FUEL_COLUMNS = ((1, 0), (0, 1), (2, 1), (1, 2))
CONTROL_COLUMN = (1, 1)


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


def _block_entry(position: tuple[int, int, int], state: int,
                block_entity: dict[str, bytes] | None = None) -> bytes:
    tags = [
        _tag(9, "pos", _list(3, [_int(value) for value in position])),
        _tag(3, "state", _int(state)),
    ]
    if block_entity is not None:
        tags.append(_tag(10, "nbt", _compound(list(block_entity.values()))))
    return _compound(tags)


def _add(blocks: dict[tuple[int, int, int], tuple[str, dict[str, str]]],
         position: tuple[int, int, int], block_id: str,
         properties: dict[str, str] | None = None) -> None:
    if position in blocks:
        raise ValueError(f"duplicate template position: {position}")
    blocks[position] = (block_id, properties or {})


def _reactor(blocks: dict[tuple[int, int, int], tuple[str, dict[str, str]]],
             origin: tuple[int, int, int]) -> None:
    ox, oy, oz = origin
    casing = "create_nuclear_industry:reactor_casing"
    windows = "create_nuclear_industry:reactor_window"
    for x in range(5):
        for y in range(5):
            for z in range(5):
                column_cap = y == 4 and 1 <= x <= 3 and 1 <= z <= 3
                if not column_cap and (x in (0, 4) or y in (0, 4) or z in (0, 4)):
                    _add(blocks, (ox + x, oy + y, oz + z), casing)

    for x, z in ((0, 2), (4, 2), (2, 4)):
        _replace(blocks, (ox + x, oy + 2, oz + z), windows)
    _replace(blocks, (ox + 2, oy + 2, oz), "create_nuclear_industry:reactor_instrument_port")
    _replace(blocks, (ox + 1, oy + 2, oz + 4), "create_nuclear_industry:reactor_cold_port")
    _replace(blocks, (ox + 3, oy + 2, oz + 4), "create_nuclear_industry:reactor_hot_port")

    fuel_body = "create_nuclear_industry:reactor_fuel_rod"
    refueling_cap = "create_nuclear_industry:reactor_refueling_port"
    drive_cap = "create_nuclear_industry:control_rod_drive"
    for core_x, core_z in FUEL_COLUMNS:
        x, z = core_x + 1, core_z + 1
        for y in range(1, 4):
            _add(blocks, (ox + x, oy + y, oz + z), fuel_body)
        _add(blocks, (ox + x, oy + 4, oz + z), refueling_cap)
    for core_x, core_z in ((0, 0), (0, 2), (2, 0), (2, 2)):
        _add(blocks, (ox + core_x + 1, oy + 4, oz + core_z + 1), casing)
    core_x, core_z = CONTROL_COLUMN
    _add(blocks, (ox + core_x + 1, oy + 4, oz + core_z + 1), drive_cap)


def _replace(blocks: dict[tuple[int, int, int], tuple[str, dict[str, str]]],
             position: tuple[int, int, int], block_id: str) -> None:
    if position not in blocks:
        raise ValueError(f"replacement target is absent: {position}")
    blocks[position] = (block_id, {})


def _operation_scene() -> tuple[tuple[int, int, int], dict[tuple[int, int, int], tuple[str, dict[str, str]]]]:
    blocks: dict[tuple[int, int, int], tuple[str, dict[str, str]]] = {}
    origin = (4, 0, 4)
    _reactor(blocks, origin)
    pipe_state = {"north": "true", "south": "true", "waterlogged": "false"}
    for x in (5, 7):
        _add(blocks, (x, 2, 9), "create:fluid_pipe", pipe_state)
        _add(blocks, (x, 2, 10), "create:fluid_pipe", pipe_state)
        direction = "north" if x == 5 else "south"
        _add(blocks, (x, 2, 11), "create:mechanical_pump",
             {"facing": direction, "waterlogged": "false"})
        _add(blocks, (x, 2, 12), "create:fluid_tank",
             {"top": "true", "bottom": "true", "shape": "window"})

    _add(blocks, (6, 2, 11), "create:cogwheel", {"axis": "z"})
    _add(blocks, (8, 2, 11), "create:cogwheel", {"axis": "z"})
    _add(blocks, (6, 2, 12), "create:creative_motor", {"facing": "north"})
    _add(blocks, (8, 2, 12), "create:creative_motor", {"facing": "north"})
    _add(blocks, (6, 2, 3), "minecraft:lever",
         {"face": "wall", "facing": "north", "powered": "false"})
    return (13, 5, 13), blocks


def _refueling_scene() -> tuple[tuple[int, int, int], dict[tuple[int, int, int], tuple[str, dict[str, str]]]]:
    blocks: dict[tuple[int, int, int], tuple[str, dict[str, str]]] = {}
    origin = (4, 0, 4)
    _reactor(blocks, origin)
    stone = "minecraft:smooth_stone"
    for x in (4, 8):
        _add(blocks, (x, 3, 3), stone)
        _add(blocks, (x, 4, 3), "create:depot")
    _add(blocks, (6, 3, 3), "create:creative_motor", {"facing": "up"})
    _add(blocks, (6, 4, 3), "create:mechanical_arm")
    return (13, 5, 13), blocks


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
    entries = []
    for position, state in sorted(blocks.items()):
        block_entity = None
        if state[0] == "create:mechanical_arm":
            block_entity = {"id": _string_tag("id", "create:mechanical_arm")}
        entries.append(_block_entry(position, indexes[state_key(state)], block_entity))
    payload = _compound([
        _tag(9, "size", _list(3, [_int(value) for value in size])),
        _tag(9, "palette", _list(10, [_palette_entry(block, props) for block, props in palette])),
        _tag(9, "blocks", _list(10, entries)),
        _tag(9, "entities", _list(10, [])),
    ])
    return _tag(10, "", payload)


def _read_string(data: bytes, offset: int) -> tuple[str, int]:
    length = struct.unpack_from(">H", data, offset)[0]
    offset += 2
    return data[offset:offset + length].decode("utf-8"), offset + length


def _read_payload(tag_type: int, data: bytes, offset: int):
    if tag_type == 1:
        return data[offset], offset + 1
    if tag_type == 3:
        return struct.unpack_from(">i", data, offset)[0], offset + 4
    if tag_type == 8:
        return _read_string(data, offset)
    if tag_type == 9:
        element_type, length = data[offset], struct.unpack_from(">i", data, offset + 1)[0]
        offset += 5
        values = []
        for _ in range(length):
            value, offset = _read_payload(element_type, data, offset)
            values.append(value)
        return (element_type, values), offset
    if tag_type == 10:
        values = {}
        while True:
            child_type = data[offset]
            offset += 1
            if child_type == 0:
                return values, offset
            name, offset = _read_string(data, offset)
            values[name], offset = _read_payload(child_type, data, offset)
    raise ValueError(f"unsupported NBT tag: {tag_type}")


def _validate(raw: bytes, size: tuple[int, int, int], required_ids: set[str]) -> int:
    if not raw or raw[0] != 10:
        raise ValueError("NBT root must be a Compound")
    name, offset = _read_string(raw, 1)
    root, offset = _read_payload(10, raw, offset)
    if name or offset != len(raw):
        raise ValueError("StructureTemplate root must be unnamed and consume all bytes")
    size_type, size_values = root["size"]
    palette_type, palette = root["palette"]
    block_type, blocks = root["blocks"]
    if size_type != 3 or size_values != list(size):
        raise ValueError(f"size must be TAG_List<TAG_Int>{size}, got {size_values}")
    if palette_type != 10 or block_type != 10:
        raise ValueError("palette and blocks must be TAG_List<TAG_Compound>")
    positions = []
    placed_ids = {entry.get("Name") for entry in palette}
    arm_points = None
    for entry in blocks:
        pos_type, pos_values = entry["pos"]
        if pos_type != 3 or len(pos_values) != 3:
            raise ValueError("every block pos must be a three-element TAG_List<TAG_Int>")
        if any(value < 0 or value >= size[axis] for axis, value in enumerate(pos_values)):
            raise ValueError(f"block position outside template bounds: {pos_values}")
        positions.append(tuple(pos_values))
        if palette[entry["state"]].get("Name") == "create:mechanical_arm":
            arm_nbt = entry.get("nbt", {})
            if arm_nbt.get("id") != "create:mechanical_arm":
                raise ValueError("mechanical arm is missing its block-entity ID")
            arm_points = arm_nbt.get("InteractionPoints", (0, []))[1]
    if len(positions) != len(set(positions)):
        raise ValueError("duplicate block positions")
    missing = required_ids - placed_ids
    if missing:
        raise ValueError(f"missing template block IDs: {sorted(missing)}")
    if len([p for p in positions if p[1] == 0]) < 25:
        raise ValueError("reactor base casing is incomplete")
    if "create:mechanical_arm" in required_ids:
        if arm_points:
            raise ValueError("mechanical-arm target points must be built with Create's runtime serializer")
    return len(blocks)


def _write(name: str, size: tuple[int, int, int],
           blocks: dict[tuple[int, int, int], tuple[str, dict[str, str]]],
           required_ids: set[str]) -> tuple[Path, int]:
    raw = _encode(size, blocks)
    path = OUTPUT_DIR / f"{name}.nbt"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(gzip.compress(raw, mtime=0))
    with gzip.open(path, "rb") as template:
        decoded = template.read()
    if decoded != raw:
        raise ValueError(f"gzip round trip changed {path.name}")
    return path, _validate(decoded, size, required_ids)


def main() -> None:
    required = {
        "create_nuclear_industry:reactor_casing",
        "create_nuclear_industry:reactor_window",
        "create_nuclear_industry:reactor_instrument_port",
        "create_nuclear_industry:reactor_cold_port",
        "create_nuclear_industry:reactor_hot_port",
        "create_nuclear_industry:reactor_refueling_port",
        "create_nuclear_industry:reactor_fuel_rod",
        "create_nuclear_industry:control_rod_drive",
    }
    build: dict[tuple[int, int, int], tuple[str, dict[str, str]]] = {}
    _reactor(build, (0, 0, 0))
    scenes = [("experimental_reactor", (5, 5, 5), build)]
    size, operation = _operation_scene()
    scenes.append(("experimental_reactor_operation", size, operation))
    size, refueling = _refueling_scene()
    scenes.append(("experimental_reactor_refueling", size, refueling))

    for name, size, blocks in scenes:
        scene_required = set(required)
        if name == "experimental_reactor_operation":
            scene_required.update({"create:mechanical_pump", "create:fluid_pipe", "create:fluid_tank",
                                   "create:creative_motor", "create:cogwheel", "minecraft:lever"})
        if name == "experimental_reactor_refueling":
            scene_required.update({"create:mechanical_arm", "create:depot", "create:creative_motor"})
        path, count = _write(name, size, blocks, scene_required)
        print(f"{path.relative_to(ROOT)}: size={size}; blocks={count}; palette IDs and Int-list positions valid")


if __name__ == "__main__":
    main()
