"""只生成干式贮存架单页模板，独立落盘回读载荷与物流，不写游戏世界。"""

from __future__ import annotations
import gzip
import hashlib
import io
import struct
import sys
from pathlib import Path
from heat_exchanger_scenes import _tag, _string, _int, _compound, _list, _palette_entry

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "src/main/resources/assets/create_nuclear_industry/ponder/dry_storage_usage.nbt"
SIZE = (5, 5, 5)
NS = "create_nuclear_industry:"


def payload(kind, value):
    """复用旧编码器，补充本页原版物品槽byte和组件嵌套list。"""
    if kind == 1: return struct.pack(">b", value)
    if kind == 3: return _int(value)
    if kind == 8: return _string(value)
    if kind == 9:
        child, entries = value
        return _list(child, [payload(child, entry) for entry in entries])
    if kind == 10: return _compound([_tag(t, key, payload(t, v)) for key, (t, v) in value.items()])
    raise ValueError(kind)


def sample():
    """独立的1.21.1组件格式样例：桶内只有一件正式冷却乏燃料组件。"""
    return {"Slot": (1, 0), "id": (8, NS + "sealed_spent_fuel_cask"), "count": (3, 1),
            "components": (10, {"minecraft:container": (9, (10, [{"slot": (3, 0),
                "item": (10, {"id": (8, NS + "cooled_spent_fuel_assembly"), "count": (3, 1)})}]))})}


def scene():
    """五格地台，南向正面，东侧向西输入与底部向南输出互不重叠。"""
    blocks = {}
    def put(pos, name, props=None, nbt=None):
        assert pos not in blocks
        blocks[pos] = name, props or {}, nbt or {}
    for x in range(5):
        for z in range(5): put((x, 0, z), "minecraft:polished_andesite")
    put((2, 2, 2), NS + "dry_storage_rack", {"facing": "south", "storage_level": "0"},
        {"id": (8, NS + "dry_storage_rack"), "Capacity": (3, 16), "DryStorage": (10, {})})
    for pos, direction in [((3, 2, 2), "west"), ((2, 1, 2), "south")]:
        put(pos, "minecraft:hopper", {"facing": direction, "enabled": "true"},
            {"id": (8, "minecraft:hopper"), "Items": (9, (10, []))})
    for pos, items in [((3, 3, 2), [sample()]), ((2, 1, 3), [])]:
        put(pos, "minecraft:barrel", {"facing": "south", "open": "false"},
            {"id": (8, "minecraft:barrel"), "Items": (9, (10, items))})
    return blocks


def encode(blocks):
    palette, indexes = [("minecraft:air", {})], {("minecraft:air", ()): 0}
    entries = []
    for pos, (name, props, nbt) in sorted(blocks.items()):
        key = name, tuple(sorted(props.items()))
        if key not in indexes:
            indexes[key] = len(palette)
            palette.append((name, props))
        tags = [_tag(9, "pos", _list(3, [_int(v) for v in pos])), _tag(3, "state", _int(indexes[key]))]
        if nbt: tags.append(_tag(10, "nbt", payload(10, nbt)))
        entries.append(_compound(tags))
    return _tag(10, "", _compound([_tag(9, "size", _list(3, [_int(v) for v in SIZE])),
        _tag(9, "palette", _list(10, [_palette_entry(n, p) for n, p in palette])),
        _tag(9, "blocks", _list(10, entries)), _tag(9, "entities", _list(10, []))]))


def read_nbt(raw):
    """独立解析落盘NBT，不用生成端对象代替回读。"""
    stream = io.BytesIO(raw)
    def num(fmt): return struct.unpack(fmt, stream.read(struct.calcsize(fmt)))[0]
    def string(): return stream.read(num(">H")).decode("utf-8")
    def read(kind):
        if kind == 1: return num(">b")
        if kind == 3: return num(">i")
        if kind == 8: return string()
        if kind == 9:
            child, count = num(">B"), num(">i")
            return [read(child) for _ in range(count)]
        if kind == 10:
            values = {}
            while (child := num(">B")):
                key = string()
                assert key not in values
                values[key] = read(child)
            return values
        raise ValueError(kind)
    assert num(">B") == 10 and string() == ""
    root = read(10)
    assert not stream.read()
    return root


def validate(root):
    """独立检查唯一架、合法输入/输出方向、设备高度和完整有效载荷。"""
    assert root["size"] == list(SIZE) and root["entities"] == []
    actual = {}
    for entry in root["blocks"]:
        pos = tuple(entry["pos"])
        assert pos not in actual and all(0 <= v < SIZE[i] for i, v in enumerate(pos))
        state = root["palette"][entry["state"]]
        actual[pos] = state, entry.get("nbt", {})
        assert pos[1] > 0 or state["Name"] == "minecraft:polished_andesite"
    assert len(actual) == 30
    assert sum(s["Name"] == NS + "dry_storage_rack" for s, _ in actual.values()) == 1
    rack, data = actual[(2, 2, 2)]
    assert rack["Properties"] == {"facing": "south", "storage_level": "0"}
    assert data == {"id": NS + "dry_storage_rack", "Capacity": 16, "DryStorage": {}}
    for pos, direction in [((3, 2, 2), "west"), ((2, 1, 2), "south")]:
        state, data = actual[pos]
        assert state["Name"] == "minecraft:hopper" and state["Properties"]["facing"] == direction
        assert data["Items"] == []
    assert actual[(2, 1, 3)][0]["Name"] == "minecraft:barrel"
    assert actual[(2, 1, 3)][1]["Items"] == []
    items = actual[(3, 3, 2)][1]["Items"]
    assert items == [{"Slot": 0, "id": NS + "sealed_spent_fuel_cask", "count": 1,
        "components": {"minecraft:container": [{"slot": 0, "item": {
            "id": NS + "cooled_spent_fuel_assembly", "count": 1}}]}}]


def main():
    first = gzip.compress(encode(scene()), mtime=0)
    if "--check" not in sys.argv: OUTPUT.write_bytes(first)
    saved = OUTPUT.read_bytes()
    validate(read_nbt(gzip.decompress(saved)))
    assert saved == first == gzip.compress(encode(scene()), mtime=0)
    print(f"{OUTPUT.name}: size={SIZE}; blocks=30; read-back valid; deterministic=true; sha256={hashlib.sha256(saved).hexdigest()}")


if __name__ == "__main__": main()
