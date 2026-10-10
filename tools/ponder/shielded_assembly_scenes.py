"""仅生成装配台搭建与接口模板，回读归属并验证确定性，不写游戏世界。"""

from __future__ import annotations
import gzip
import hashlib
import io
import struct
import sys
from pathlib import Path
from heat_exchanger_scenes import _tag, _string, _int, _compound, _list, _palette_entry

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "src/main/resources/assets/create_nuclear_industry/ponder"
SIZE = (7, 6, 7)
NS = "create_nuclear_industry:"
OWNER = [0x12345678, 0x12341234, 0x12341234, 0x12345678]
MASTER = (3, 2, 3)


def payload(kind, value):
    """编码本模板使用的NBT类型；长整数使用原版BlockPos压位格式。"""
    if kind == 3: return _int(value)
    if kind == 4: return struct.pack(">q", value)
    if kind == 8: return _string(value)
    if kind == 11: return _int(len(value)) + b"".join(_int(v) for v in value)
    if kind == 10: return _compound([_tag(t, key, payload(t, v)) for key, (t, v) in value.items()])
    raise ValueError(kind)


def scene():
    """主控y=2、底轴y=1；北侧和西侧各两个独立材料面，北侧另一面取成品。"""
    blocks = {}
    def put(pos, name, props=None, nbt=None):
        assert pos not in blocks
        blocks[pos] = name, props or {}, nbt or {}
    for x in range(7):
        for z in range(7): put((x, 0, z), "minecraft:polished_andesite")
    packed = (MASTER[0] << 38) | (MASTER[2] << 12) | MASTER[1]
    for part in range(8):
        pos = (3 + (part & 1), 2 + (part >> 2), 3 + ((part >> 1) & 1))
        props = {"facing": "north", "working": "false"}
        if part == 0:
            props["expanded"] = "true"
            nbt = {"id": (8, NS + "shielded_assembly_station"), "ShieldedAssemblyOwner": (11, OWNER),
                   "ShieldedAssembly": (10, {"Progress": (3, 0), "Operation": (8, ""),
                    "RecipeId": (8, ""), "Costs": (11, [8, 4, 2, 1]), "Work": (3, 25600)})}
        else:
            props["part"] = str(part)
            nbt = {"id": (8, NS + "shielded_assembly_part"), "OwnerId": (11, OWNER), "MasterPos": (4, packed)}
        put(pos, NS + ("shielded_assembly_station" if part == 0 else "shielded_assembly_part"), props, nbt)
    put((3, 1, 3), "create:shaft", {"axis": "y", "waterlogged": "false"})
    for pos in ((3, 2, 2), (3, 3, 2), (2, 2, 3), (2, 3, 3), (4, 2, 2)):
        put(pos, "create:andesite_funnel", {"facing": "west" if pos[0] == 2 else "north",
            "extracting": "true" if pos == (4, 2, 2) else "false", "powered": "false"})
    put((4, 1, 2), "minecraft:hopper", {"facing": "west", "enabled": "true"})
    put((3, 1, 2), "minecraft:barrel", {"facing": "north", "open": "false"})
    put((4, 4, 4), "create:chute", {"facing": "down", "shape": "window", "waterlogged": "false"})
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
    """从落盘数据独立解析NBT，包含归属UUID数组和主控坐标long。"""
    stream = io.BytesIO(raw)
    def num(fmt): return struct.unpack(fmt, stream.read(struct.calcsize(fmt)))[0]
    def string(): return stream.read(num(">H")).decode("utf-8")
    def read(kind):
        if kind == 3: return num(">i")
        if kind == 4: return num(">q")
        if kind == 8: return string()
        if kind == 11: return [num(">i") for _ in range(num(">i"))]
        if kind == 9:
            child, count = num(">B"), num(">i")
            return [read(child) for _ in range(count)]
        if kind == 10:
            values = {}
            while (child := num(">B")):
                key = string()
                values[key] = read(child)
            return values
        raise ValueError(kind)
    assert num(">B") == 10 and string() == ""
    root = read(10)
    assert not stream.read()
    return root


def validate(root):
    """回读八格、唯一库存与可见外表面，确保没有地台内设备或代理第二库存。"""
    assert root["size"] == list(SIZE) and root["entities"] == []
    actual = {}
    for entry in root["blocks"]:
        pos = tuple(entry["pos"])
        assert pos not in actual and all(0 <= v < SIZE[i] for i, v in enumerate(pos))
        state = root["palette"][entry["state"]]
        actual[pos] = state["Name"], state.get("Properties", {}), entry.get("nbt", {})
    for pos, (name, props, nbt) in scene().items():
        expected = {key: value for key, (_, value) in nbt.items()}
        if "ShieldedAssembly" in expected:
            expected["ShieldedAssembly"] = {k: v for k, (_, v) in expected["ShieldedAssembly"].items()}
        assert actual[pos] == (name, props, expected)
        assert pos[1] > 0 or name == "minecraft:polished_andesite"
    assert len(actual) == 66
    for part in range(1, 8):
        pos = (3 + (part & 1), 2 + (part >> 2), 3 + ((part >> 1) & 1))
        assert actual[pos][2]["OwnerId"] == actual[MASTER][2]["ShieldedAssemblyOwner"]
        assert "ShieldedAssembly" not in actual[pos][2]


def main():
    path = OUTPUT / "shielded_assembly_placement.nbt"
    first = gzip.compress(encode(scene()), mtime=0)
    # 整改验证使用--check只读原模板；维护时默认仍可确定性生成唯一用法页。
    if "--check" not in sys.argv:
        path.write_bytes(first)
    saved = path.read_bytes()
    validate(read_nbt(gzip.decompress(saved)))
    assert saved == first == gzip.compress(encode(scene()), mtime=0)
    print(f"{path.name}: size={SIZE}; blocks=66; read-back valid; deterministic=true; sha256={hashlib.sha256(saved).hexdigest()}")


if __name__ == "__main__": main()
