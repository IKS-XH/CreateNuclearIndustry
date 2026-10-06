"""生成并校验离心机 Ponder 专用结构模板。"""

from __future__ import annotations

import gzip
import struct
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "src/main/resources/assets/create_nuclear_industry/ponder/enrichment_centrifuge.nbt"


def _string(value: str) -> bytes:
    data = value.encode("utf-8")
    return struct.pack(">H", len(data)) + data


def _tag(tag_type: int, name: str, payload: bytes) -> bytes:
    return bytes([tag_type]) + _string(name) + payload


def _int(value: int) -> bytes:
    return struct.pack(">i", value)


def _compound_payload(entries: list[bytes]) -> bytes:
    return b"".join(entries) + b"\x00"


def _named_compound(entries: list[bytes]) -> bytes:
    return _compound_payload(entries)


def _list(tag_type: int, values: list[bytes]) -> bytes:
    return bytes([tag_type]) + _int(len(values)) + b"".join(values)


def _string_tag(name: str, value: str) -> bytes:
    return _tag(8, name, _string(value))


def _palette_entry(block_id: str, properties: dict[str, str]) -> bytes:
    fields = [_string_tag("Name", block_id)]
    if properties:
        fields.append(_tag(10, "Properties", _compound_payload([
            _string_tag(key, value) for key, value in sorted(properties.items())
        ])))
    return _named_compound(fields)


def _block_entry(position: tuple[int, int, int], state: int, *, paired_machine: bool = False) -> bytes:
    fields = [
        _tag(9, "pos", _list(3, [_int(value) for value in position])),
        _tag(3, "state", _int(state)),
    ]
    if paired_machine:
        fields.append(_tag(10, "nbt", _compound_payload([
            _string_tag("id", "create_nuclear_industry:enrichment_centrifuge"),
            _tag(1, "CentrifugePaired", b"\x01"),
        ])))
    return _named_compound(fields)


def _pipe_state(**directions: bool) -> tuple[str, dict[str, str]]:
    state = {name: str(value).lower() for name, value in directions.items()}
    state["waterlogged"] = "false"
    return "create:fluid_pipe", state


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
    if tag_type == 11:
        length = struct.unpack_from(">i", data, offset)[0]
        offset += 4
        return tuple(struct.unpack_from(f">{length}i", data, offset)), offset + 4 * length
    raise ValueError(f"unsupported NBT tag {tag_type}")


def _decode_root(data: bytes) -> dict:
    if not data or data[0] != 10:
        raise ValueError("NBT root is not a Compound")
    name, offset = _read_string(data, 1)
    if name:
        raise ValueError("StructureTemplate root name must be empty")
    root, offset = _read_payload(10, data, offset)
    if offset != len(data):
        raise ValueError("trailing bytes after root Compound")
    return root


def _structure() -> bytes:
    # 坐标与 CentrifugePonderScenes 中的分段一致；底部传动、上方接管和回水支路均可见。
    blocks: list[tuple[tuple[int, int, int], tuple[str, dict[str, str]]]] = []
    for x in range(9):
        for z in range(9):
            blocks.append(((x, 0, z), ("minecraft:smooth_stone", {})))

    blocks.extend([
        ((1, 1, 4), ("create:creative_motor", {"facing": "east"})),
        ((2, 1, 4), ("create:shaft", {"axis": "x"})),
        ((3, 1, 4), ("create:shaft", {"axis": "x"})),
        ((4, 1, 4), ("create:gearbox", {"axis": "z"})),
        ((4, 2, 4), ("create_nuclear_industry:enrichment_centrifuge", {"facing": "north", "half": "lower"})),
        ((4, 3, 4), ("create_nuclear_industry:enrichment_centrifuge", {"facing": "north", "half": "upper"})),
        ((4, 4, 4), _pipe_state(up=True, down=True)),
        ((4, 5, 4), ("create:mechanical_pump", {"facing": "down", "waterlogged": "false"})),
        ((4, 6, 4), _pipe_state(up=True, down=True)),
        ((4, 7, 4), ("create:fluid_tank", {"top": "true", "bottom": "true", "shape": "window"})),
        ((3, 5, 4), ("create:cogwheel", {"axis": "y"})),
        ((3, 6, 4), ("create:shaft", {"axis": "y"})),
        ((3, 7, 4), ("create:shaft", {"axis": "y"})),
        ((3, 8, 4), ("create:creative_motor", {"facing": "down"})),
        ((4, 2, 3), ("create:brass_funnel", {"facing": "north", "extracting": "true"})),
        ((4, 1, 3), ("minecraft:hopper", {"enabled": "true", "facing": "down"})),
        ((5, 3, 4), ("create:brass_funnel", {"facing": "east", "extracting": "true"})),
        ((5, 2, 4), ("minecraft:hopper", {"enabled": "true", "facing": "down"})),
        ((4, 2, 5), _pipe_state(north=True, east=True)),
        ((5, 2, 5), _pipe_state(west=True, east=True)),
        ((6, 2, 5), ("create:mechanical_pump", {"facing": "east", "waterlogged": "false"})),
        ((6, 2, 4), ("create:cogwheel", {"axis": "x"})),
        ((7, 2, 4), ("create:shaft", {"axis": "x"})),
        ((8, 2, 4), ("create:creative_motor", {"facing": "west"})),
        ((7, 2, 5), ("create:fluid_tank", {"top": "true", "bottom": "true", "shape": "window"})),
    ])

    palette: list[tuple[str, dict[str, str]]] = [("minecraft:air", {})]
    state_key = lambda entry: (entry[0], tuple(sorted(entry[1].items())))
    index = {state_key(palette[0]): 0}
    for _, state in blocks:
        key = state_key(state)
        if key not in index:
            index[key] = len(palette)
            palette.append(state)
    positions = [position for position, _ in blocks]
    if len(positions) != len(set(positions)) or any(
            not (0 <= x < 9 and 0 <= y < 9 and 0 <= z < 9) for x, y, z in positions):
        raise SystemExit("模板中存在重复或越界方块坐标")
    machine_halves = {state[1].get("half") for _, state in blocks
                      if state[0] == "create_nuclear_industry:enrichment_centrifuge"}
    if machine_halves != {"lower", "upper"}:
        raise SystemExit("模板必须包含上下两段离心机")
    required = {
        "create:mechanical_pump", "create:fluid_pipe", "create:fluid_tank",
        "create:brass_funnel", "create:gearbox", "create:cogwheel", "minecraft:hopper",
    }
    if not required.issubset({state[0] for _, state in blocks}):
        raise SystemExit("模板缺少教学所需的泵、管道、储罐、漏斗、齿轮箱或漏斗接收斗")
    block_entries = [
        _block_entry(
            pos,
            index[state_key(state)],
            paired_machine=(state[0] == "create_nuclear_industry:enrichment_centrifuge"
                            and state[1].get("half") == "lower"),
        )
        for pos, state in blocks
    ]
    payload = _compound_payload([
        _tag(9, "size", _list(3, [_int(9), _int(9), _int(9)])),
        _tag(9, "palette", _list(10, [
            _palette_entry(block_id, properties) for block_id, properties in palette
        ])),
        _tag(9, "blocks", _list(10, block_entries)),
        _tag(9, "entities", _list(10, [])),
    ])
    return _tag(10, "", payload)


def main() -> None:
    raw = _structure()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_bytes(gzip.compress(raw, mtime=0))

    with gzip.open(OUTPUT, "rb") as template:
        decoded = template.read()
    parsed = _decode_root(decoded)
    size_type, size_values = parsed.get("size", (0, []))
    if decoded != raw or size_type != 3 or size_values != [9, 9, 9]:
        raise SystemExit("NBT 解码后 size 必须为包含三个 TAG_Int 的 TAG_List")
    palette_type, palette = parsed.get("palette", (0, []))
    block_type, placed_blocks = parsed.get("blocks", (0, []))
    if palette_type != 10 or block_type != 10 or not placed_blocks:
        raise SystemExit("palette/blocks 必须是 Compound 列表且不能为空")
    required = {
        "create:mechanical_pump", "create:fluid_pipe", "create:fluid_tank",
        "create:brass_funnel", "create:gearbox", "create:cogwheel", "minecraft:hopper",
        "create_nuclear_industry:enrichment_centrifuge",
    }
    placed_ids = {palette[block["state"]]["Name"] for block in placed_blocks}
    if not required.issubset(placed_ids):
        raise SystemExit("NBT palette 缺少离心机思索所需组件")
    lower = next(block for block in placed_blocks
                 if palette[block["state"]]["Name"] == "create_nuclear_industry:enrichment_centrifuge"
                 and palette[block["state"]].get("Properties", {}).get("half") == "lower")
    block_nbt = lower.get("nbt", {})
    if block_nbt.get("CentrifugePaired") != 1:
        raise SystemExit("下段离心机模板必须初始化 CentrifugePaired=true 才能显示真实转子")
    if any(block.get("pos", (0, []))[0] != 3 or len(block["pos"][1]) != 3
           for block in placed_blocks):
        raise SystemExit("StructureTemplate.load 要求每个 pos 是含三个 TAG_Int 的 TAG_List")
    state_at = {
        tuple(block["pos"][1]): palette[block["state"]]
        for block in placed_blocks
    }
    expected_states = {
        (4, 5, 4): ("create:mechanical_pump", {"facing": "down"}),
        (4, 2, 3): ("create:brass_funnel", {"facing": "north", "extracting": "true"}),
        (4, 1, 3): ("minecraft:hopper", {"facing": "down"}),
        (5, 3, 4): ("create:brass_funnel", {"facing": "east", "extracting": "true"}),
        (5, 2, 4): ("minecraft:hopper", {"facing": "down"}),
        (3, 8, 4): ("create:creative_motor", {"facing": "down"}),
        (8, 2, 4): ("create:creative_motor", {"facing": "west"}),
        (3, 5, 4): ("create:cogwheel", {"axis": "y"}),
        (6, 2, 4): ("create:cogwheel", {"axis": "x"}),
    }
    for pos, (block_id, properties) in expected_states.items():
        state = state_at.get(pos, {})
        if state.get("Name") != block_id or not properties.items() <= state.get("Properties", {}).items():
            raise SystemExit(f"模板在 {pos} 缺少正确的方块方向或属性: {state}")
    print(
        f"wrote {OUTPUT} ({OUTPUT.stat().st_size} bytes); "
        f"NBT size=TAG_List<{size_type}>[{len(size_values)}]={size_values}; "
        f"all {len(placed_blocks)} pos values=TAG_List<TAG_Int>[3]; "
        "all positions in range; gzip/NBT consumed through EOF; "
        "paired model, pump direction, powered rigs, filtered funnels, and hoppers verified"
    )


if __name__ == "__main__":
    main()
