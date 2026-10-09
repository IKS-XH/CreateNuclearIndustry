"""只生成并读取校验烧结炉两幕模板；gzip时间戳固定，复用现有NBT编码器。"""

from __future__ import annotations

import gzip
import hashlib
import io
import struct
from pathlib import Path

from heat_exchanger_scenes import add, add_tank, encode

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "src/main/resources/assets/create_nuclear_industry/ponder"
SIZE = (7, 6, 7)
NS = "create_nuclear_industry:"


def scene(automation: bool) -> dict:
    """热源y=1、炉体y=2；物流在北侧，冷热回路在东西侧，设备不埋入地台。"""
    blocks = {}
    for x in range(7):
        for z in range(7):
            add(blocks, (x, 0, z), "minecraft:polished_andesite")
    add(blocks, (3, 1, 3), "create:blaze_burner", {"blaze": "kindled"})
    add(blocks, (3, 2, 3), NS + "fuel_sintering_furnace", {"facing": "north", "lit": "true"})
    if automation:
        add(blocks, (3, 3, 3), "create:chute", {"facing": "down", "shape": "window", "waterlogged": "false"})
        add(blocks, (3, 4, 3), "minecraft:barrel", {"facing": "up", "open": "false"})
        add(blocks, (3, 2, 2), "create:andesite_funnel", {"facing": "north", "extracting": "true", "powered": "false"})
        add(blocks, (3, 1, 2), "minecraft:hopper", {"facing": "west", "enabled": "true"})
        add(blocks, (2, 1, 2), "minecraft:barrel", {"facing": "north", "open": "false"})
        for x in (0, 6):
            add_tank(blocks, (x, 1, 3))
        for x in (1, 2, 4, 5):
            add(blocks, (x, 1, 3), "create:fluid_pipe", {"east": "true", "west": "true", "north": "false",
                "south": "false", "up": "false", "down": "false", "waterlogged": "false"})
    return blocks


def read_nbt(data: bytes) -> dict:
    """解析本生成器使用的标准NBT类型，读取落盘结构而非仅比较压缩字节。"""
    stream = io.BytesIO(data)

    def number(fmt):
        return struct.unpack(fmt, stream.read(struct.calcsize(fmt)))[0]

    def string():
        return stream.read(number(">H")).decode("utf-8")

    def payload(kind):
        if kind == 3:
            return number(">i")
        if kind == 8:
            return string()
        if kind == 9:
            child, count = number(">B"), number(">i")
            return [payload(child) for _ in range(count)]
        if kind == 10:
            result = {}
            while (child := number(">B")) != 0:
                key = string()
                result[key] = payload(child)
            return result
        raise ValueError(f"不支持的NBT类型: {kind}")

    if number(">B") != 10 or string() != "":
        raise ValueError("结构根须为无名Compound")
    result = payload(10)
    if stream.read():
        raise ValueError("结构包含尾随字节")
    return result


def validate(root: dict, expected: dict, automation: bool) -> None:
    """校验实体ID、属性、坐标、相邻热源和合法物流方向，不加载或写入游戏世界。"""
    assert root["size"] == list(SIZE) and root["entities"] == []
    blocks = {}
    for entry in root["blocks"]:
        pos = tuple(entry["pos"])
        assert pos not in blocks and all(0 <= p < SIZE[i] for i, p in enumerate(pos))
        state = root["palette"][entry["state"]]
        blocks[pos] = state["Name"], state.get("Properties", {})
        assert pos[1] >= 1 or state["Name"] == "minecraft:polished_andesite"
    assert blocks == expected
    assert blocks[(3, 2, 3)][0] == NS + "fuel_sintering_furnace"
    assert blocks[(3, 1, 3)] == ("create:blaze_burner", {"blaze": "kindled"})
    if automation:
        assert blocks[(3, 3, 3)][1]["facing"] == "down"
        assert blocks[(3, 2, 2)][1]["extracting"] == "true"
        assert blocks[(3, 2, 2)][1]["facing"] == "north"
        assert blocks[(3, 1, 2)][1]["facing"] == "west"
        assert blocks[(2, 1, 2)][0] == "minecraft:barrel"
        for x in (1, 2, 4, 5):
            assert blocks[(x, 1, 3)][1]["east"] == blocks[(x, 1, 3)][1]["west"] == "true"


def main() -> None:
    for suffix in ("operation", "automation"):
        automation = suffix == "automation"
        blocks = scene(automation)
        path = OUTPUT / f"fuel_sintering_{suffix}.nbt"
        first = gzip.compress(encode(blocks, SIZE), mtime=0)
        path.write_bytes(first)
        validate(read_nbt(gzip.decompress(path.read_bytes())), blocks, automation)
        second = gzip.compress(encode(scene(automation), SIZE), mtime=0)
        assert first == second, "同输入重新生成必须字节一致"
        print(f"{path.name}: size={SIZE}; blocks={len(blocks)}; read-back valid; deterministic=true; sha256={hashlib.sha256(first).hexdigest()}")


if __name__ == "__main__":
    main()
