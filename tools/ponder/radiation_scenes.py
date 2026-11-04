"""只生成辐射用法幕；复用已有装配台NBT编码，不运行机器或辐射结算。"""
from __future__ import annotations
import gzip
from pathlib import Path
from shielded_assembly_scenes import scene as assembly_scene, encode
from boiler_scenes import _decode

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "src/main/resources/assets/create_nuclear_industry/ponder/radiation_protection.nbt"
SIZE = (13, 7, 13)
NS = "create_nuclear_industry:"


def scene() -> dict:
    """三件燃料样品、真实屏蔽墙、普通容器、完整装配台和独立贮存架分阶段显露。"""
    blocks = {}
    def put(pos, name, props=None, nbt=None):
        assert pos not in blocks
        blocks[pos] = name, props or {}, nbt or {}
    for x in range(13):
        for z in range(13):
            put((x, 0, z), "minecraft:polished_andesite")
    for z in (1, 3, 5):
        put((6, 1, z), "minecraft:polished_andesite")
        put((6, 2, z), "create:depot")
    for y in range(1, 4):
        for z in range(2, 5):
            put((3, y, z), NS + "shielding_concrete")
    put((9, 1, 3), "minecraft:barrel", {"facing": "north", "open": "false"})
    put((3, 1, 8), NS + "dry_storage_rack", {"facing": "north", "storage_level": "0"})
    # 平移原已验八格机体和底轴，归属UUID保持独立，主控坐标同步平移；不带原配方物流。
    master = (7, 2, 7)
    packed = (master[0] << 38) | (master[2] << 12) | master[1]
    for pos, (name, props, nbt) in assembly_scene().items():
        if name not in (NS + "shielded_assembly_station", NS + "shielded_assembly_part", "create:shaft"):
            continue
        nbt = dict(nbt)
        if "MasterPos" in nbt:
            nbt["MasterPos"] = (4, packed)
        if "ShieldedAssembly" in nbt:
            state = dict(nbt["ShieldedAssembly"][1])
            state["Costs"] = (11, [0, 0, 0, 0])
            nbt["ShieldedAssembly"] = (10, state)
        put((pos[0] + 4, pos[1], pos[2] + 4), name, dict(props), nbt)
    return blocks


def raw_template() -> bytes:
    """沿用编码器但只临时替换其尺寸常量，返回字节后立即恢复，不写任何其他模板。"""
    import shielded_assembly_scenes as codec
    previous = codec.SIZE
    try:
        codec.SIZE = SIZE
        return encode(scene())
    finally:
        codec.SIZE = previous


def main() -> None:
    raw = raw_template()
    parsed = _decode(raw)
    assert parsed["size"][1] == list(SIZE)
    positions = [tuple(entry["pos"][1]) for entry in parsed["blocks"][1]]
    assert len(positions) == len(set(positions))
    assert all(all(0 <= value < SIZE[i] for i, value in enumerate(pos)) for pos in positions)
    OUTPUT.write_bytes(gzip.compress(raw, mtime=0))
    assert gzip.decompress(OUTPUT.read_bytes()) == raw
    print(f"{OUTPUT.name}: size={SIZE}; blocks={len(positions)}; read-back valid")


if __name__ == "__main__":
    main()
