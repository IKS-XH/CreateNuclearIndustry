"""EXT-A-REACTOR-01材料与半成品的可编辑SVG绘制及确定性离线导出。"""
from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / "tools" / "art-assets"
sys.path.insert(0, str(ART))
import export as strict_exporter

ASSET = ROOT / "src/main/resources/assets/create_nuclear_industry"
SOURCE = ART / "sources" / "reactor-01"
GENERATED_ITEM = ART / "generated" / "item"
GENERATED_BLOCK = ART / "generated" / "block"
EVIDENCE = ROOT / "build/reports/extension/EXT-A-REACTOR-01-ART"
REPORT = ROOT / "build/reports/extension/EXT-A-REACTOR-01-ART.md"

# 蓝灰钢沿用本模组钢板色阶；陶瓷分别以象牙白和深石墨色区分。
PALETTE: dict[str, set[str]] = {
    "steel_rod": {"#182329", "#D1DFDA", "#A3B7B8", "#788E94", "#5B7078", "#3E5058"},
    "seal_ring": {"#182329", "#D1DFDA", "#A3B7B8", "#788E94", "#5B7078", "#3E5058"},
    "pressure_fitting": {"#182329", "#D1DFDA", "#A3B7B8", "#788E94", "#5B7078", "#3E5058"},
    "industrial_ceramic": {"#292B28", "#FFF0C8", "#E7D7AD", "#CBB98D", "#9B8967", "#695B49"},
    "neutron_absorbing_ceramic": {"#151C20", "#829397", "#53676B", "#34454B", "#252D31", "#C36B28", "#F1A642"},
    "shielded_glass": {"#182329", "#D1DFDA", "#788E94", "#53676B", "#303E42", "#79C8CC", "#B7F4E8"},
    "incomplete_shielded_glass": {"#182329", "#D1DFDA", "#788E94", "#53676B", "#303E42", "#79C8CC", "#B7F4E8", "#C36B28", "#F1A642"},
    "incomplete_reactor_instrument_port": {"#182329", "#D1DFDA", "#A3B7B8", "#788E94", "#53676B", "#303E42", "#3E5058", "#D0B66B", "#9A8249", "#C36B28", "#F1A642"},
    "incomplete_reactor_refueling_port": {"#182329", "#D1DFDA", "#A3B7B8", "#788E94", "#53676B", "#303E42", "#3E5058", "#D0B66B", "#9A8249", "#C36B28", "#F1A642"},
    "incomplete_control_rod": {"#182329", "#D1DFDA", "#A3B7B8", "#788E94", "#53676B", "#303E42", "#252D31", "#34454B", "#C36B28", "#F1A642"},
    "incomplete_control_rod_drive": {"#182329", "#D1DFDA", "#A3B7B8", "#788E94", "#53676B", "#303E42", "#D0B66B", "#9A8249", "#C36B28", "#F1A642"},
    "shielding_concrete": {"#292B28", "#77766D", "#625F56", "#4C4A45", "#393A37", "#A39A83"},
}

# 每个整数矩形都是一个像素块，按列表次序覆盖，SVG源稿可直接手工编辑。
PIXELS: dict[str, list[tuple[int, int, int, int, str]]] = {
    "steel_rod": [
        (4, 11, 2, 2, "#182329"), (3, 10, 2, 2, "#182329"), (5, 10, 2, 2, "#D1DFDA"),
        (5, 9, 2, 2, "#A3B7B8"), (6, 8, 2, 2, "#788E94"), (7, 7, 2, 2, "#A3B7B8"),
        (8, 6, 2, 2, "#D1DFDA"), (9, 5, 2, 2, "#A3B7B8"), (10, 4, 2, 2, "#788E94"),
        (11, 3, 2, 2, "#182329"), (10, 3, 2, 2, "#182329"), (11, 4, 2, 2, "#3E5058"),
        (4, 12, 2, 1, "#3E5058"), (12, 3, 1, 2, "#182329"),
    ],
    "seal_ring": [
        (5, 2, 6, 1, "#182329"), (3, 3, 2, 1, "#182329"), (5, 3, 6, 1, "#D1DFDA"), (11, 3, 2, 1, "#182329"),
        (2, 5, 1, 6, "#182329"), (3, 4, 1, 1, "#D1DFDA"), (4, 4, 2, 1, "#A3B7B8"), (6, 4, 4, 1, "#788E94"), (10, 4, 2, 1, "#A3B7B8"), (12, 4, 1, 1, "#5B7078"),
        (3, 5, 2, 6, "#A3B7B8"), (5, 5, 1, 6, "#788E94"), (10, 5, 1, 6, "#5B7078"), (11, 5, 2, 6, "#A3B7B8"), (13, 5, 1, 6, "#182329"),
        (4, 11, 2, 1, "#788E94"), (6, 11, 4, 1, "#5B7078"), (10, 11, 2, 1, "#3E5058"),
        (5, 12, 6, 1, "#182329"), (6, 6, 4, 4, "#182329"), (7, 6, 2, 1, "#3E5058"), (7, 9, 2, 1, "#3E5058"),
    ],
    "pressure_fitting": [
        (4, 8, 2, 3, "#182329"), (3, 8, 2, 3, "#182329"), (3, 7, 2, 4, "#D1DFDA"), (4, 7, 2, 4, "#A3B7B8"),
        (5, 8, 2, 2, "#788E94"), (6, 7, 2, 3, "#5B7078"), (7, 6, 2, 3, "#788E94"),
        (8, 5, 2, 3, "#A3B7B8"), (9, 4, 2, 3, "#D1DFDA"), (10, 4, 2, 3, "#A3B7B8"),
        (11, 3, 2, 3, "#182329"), (12, 3, 2, 3, "#182329"),
        (4, 9, 1, 1, "#182329"), (11, 4, 1, 1, "#3E5058"), (5, 10, 2, 1, "#3E5058"), (9, 5, 2, 1, "#788E94"),
    ],
    "industrial_ceramic": [
        (5, 3, 6, 1, "#292B28"), (4, 4, 2, 1, "#292B28"), (6, 4, 5, 1, "#FFF0C8"), (11, 4, 1, 1, "#292B28"),
        (3, 5, 2, 1, "#292B28"), (5, 5, 7, 1, "#E7D7AD"), (12, 5, 1, 1, "#695B49"),
        (3, 6, 1, 1, "#292B28"), (4, 6, 2, 1, "#FFF0C8"), (6, 6, 6, 1, "#E7D7AD"), (12, 6, 1, 1, "#9B8967"),
        (3, 7, 1, 3, "#292B28"), (4, 7, 2, 3, "#E7D7AD"), (6, 7, 6, 3, "#CBB98D"), (12, 7, 1, 3, "#695B49"),
        (4, 10, 2, 1, "#695B49"), (6, 10, 5, 1, "#9B8967"), (11, 10, 1, 1, "#292B28"),
        (5, 11, 6, 1, "#292B28"), (6, 8, 1, 1, "#FFF0C8"), (9, 9, 1, 1, "#E7D7AD"),
    ],
    "neutron_absorbing_ceramic": [
        (5, 3, 6, 1, "#151C20"), (4, 4, 2, 1, "#151C20"), (6, 4, 5, 1, "#829397"), (11, 4, 1, 1, "#151C20"),
        (3, 5, 2, 1, "#151C20"), (5, 5, 7, 1, "#53676B"), (12, 5, 1, 1, "#252D31"),
        (3, 6, 1, 1, "#151C20"), (4, 6, 2, 1, "#829397"), (6, 6, 6, 1, "#53676B"), (12, 6, 1, 1, "#34454B"),
        (3, 7, 1, 3, "#151C20"), (4, 7, 2, 3, "#53676B"), (6, 7, 6, 3, "#34454B"), (12, 7, 1, 3, "#252D31"),
        (4, 10, 2, 1, "#252D31"), (6, 10, 5, 1, "#252D31"), (11, 10, 1, 1, "#151C20"), (5, 11, 6, 1, "#151C20"),
        (8, 6, 1, 1, "#C36B28"), (9, 6, 1, 1, "#F1A642"), (9, 7, 1, 1, "#C36B28"),
    ],
    "shielded_glass": [
        (4, 2, 8, 1, "#182329"), (3, 3, 1, 9, "#182329"), (4, 3, 1, 1, "#D1DFDA"), (5, 3, 7, 1, "#788E94"), (12, 3, 1, 9, "#182329"),
        (4, 4, 1, 7, "#788E94"), (5, 4, 6, 6, "#53676B"), (11, 4, 1, 7, "#303E42"),
        (5, 4, 5, 1, "#B7F4E8"), (5, 5, 1, 4, "#79C8CC"), (6, 5, 4, 1, "#79C8CC"), (7, 6, 3, 3, "#53676B"), (9, 9, 2, 1, "#79C8CC"),
        (4, 11, 8, 1, "#182329"), (5, 10, 6, 1, "#303E42"),
    ],
    "incomplete_shielded_glass": [
        (4, 2, 6, 1, "#182329"), (3, 3, 1, 9, "#182329"), (4, 3, 1, 1, "#D1DFDA"), (5, 3, 6, 1, "#788E94"), (11, 3, 1, 2, "#182329"),
        (4, 4, 1, 7, "#788E94"), (5, 4, 6, 6, "#53676B"), (11, 5, 1, 6, "#303E42"), (5, 4, 5, 1, "#B7F4E8"),
        (5, 5, 1, 4, "#79C8CC"), (6, 5, 4, 1, "#79C8CC"), (7, 6, 3, 3, "#53676B"), (9, 9, 2, 1, "#79C8CC"),
        (4, 11, 7, 1, "#182329"), (5, 10, 6, 1, "#303E42"), (11, 3, 2, 1, "#C36B28"), (12, 4, 1, 2, "#F1A642"), (11, 6, 1, 1, "#C36B28"),
    ],
    "incomplete_reactor_instrument_port": [
        (4, 2, 8, 1, "#182329"), (3, 3, 10, 1, "#182329"), (2, 4, 2, 7, "#182329"), (4, 4, 8, 1, "#D1DFDA"), (12, 4, 2, 7, "#182329"),
        (4, 5, 1, 5, "#53676B"), (5, 5, 6, 5, "#788E94"), (11, 5, 1, 5, "#303E42"), (5, 10, 7, 1, "#3E5058"), (3, 11, 10, 1, "#182329"),
        (5, 6, 6, 1, "#9A8249"), (6, 7, 4, 2, "#D0B66B"), (7, 7, 2, 1, "#303E42"), (7, 8, 2, 1, "#303E42"), (9, 7, 1, 1, "#F1A642"),
        (2, 12, 3, 1, "#182329"), (6, 12, 4, 1, "#182329"), (4, 12, 2, 1, "#C36B28"),
    ],
    "incomplete_reactor_refueling_port": [
        (5, 2, 6, 1, "#182329"), (4, 3, 8, 1, "#182329"), (3, 4, 2, 1, "#182329"), (5, 4, 7, 1, "#D1DFDA"), (12, 4, 1, 1, "#182329"),
        (2, 5, 2, 6, "#182329"), (4, 5, 1, 5, "#53676B"), (5, 5, 7, 1, "#788E94"), (12, 5, 2, 6, "#182329"),
        (5, 6, 6, 4, "#53676B"), (11, 6, 1, 4, "#303E42"), (4, 10, 9, 1, "#3E5058"), (3, 11, 10, 1, "#182329"),
        (6, 6, 5, 1, "#9A8249"), (6, 7, 4, 2, "#D0B66B"), (7, 7, 2, 1, "#303E42"), (7, 8, 2, 1, "#303E42"),
        (5, 9, 6, 1, "#788E94"), (4, 12, 2, 1, "#C36B28"), (6, 12, 4, 1, "#182329"), (11, 3, 2, 1, "#F1A642"),
    ],
    "incomplete_control_rod": [
        (7, 2, 2, 1, "#182329"), (6, 3, 4, 1, "#182329"), (7, 3, 2, 1, "#D1DFDA"),
        (6, 4, 4, 6, "#303E42"), (7, 4, 2, 1, "#788E94"), (7, 5, 2, 3, "#53676B"), (7, 8, 2, 2, "#34454B"),
        (5, 5, 1, 2, "#182329"), (10, 5, 1, 2, "#182329"), (5, 5, 1, 1, "#C36B28"), (10, 6, 1, 1, "#F1A642"),
        (6, 10, 4, 1, "#182329"), (7, 11, 2, 1, "#A3B7B8"), (7, 12, 2, 1, "#788E94"), (6, 13, 4, 1, "#182329"),
    ],
    "incomplete_control_rod_drive": [
        (7, 2, 2, 1, "#182329"), (6, 3, 4, 1, "#182329"), (7, 3, 2, 1, "#D1DFDA"), (7, 4, 2, 4, "#53676B"),
        (5, 5, 6, 1, "#182329"), (4, 6, 8, 1, "#182329"), (4, 7, 2, 3, "#182329"), (6, 7, 4, 2, "#788E94"), (10, 7, 2, 3, "#182329"),
        (5, 7, 1, 2, "#D0B66B"), (6, 7, 1, 1, "#F1A642"), (7, 9, 2, 2, "#53676B"), (6, 11, 4, 1, "#182329"),
        (7, 12, 2, 1, "#A3B7B8"), (6, 13, 4, 1, "#182329"), (11, 6, 1, 1, "#C36B28"), (12, 6, 1, 1, "#F1A642"),
    ],
    "shielding_concrete": [
        (0, 0, 16, 16, "#625F56"), (0, 0, 16, 1, "#393A37"), (0, 15, 16, 1, "#393A37"),
        (0, 1, 1, 14, "#393A37"), (15, 1, 1, 14, "#393A37"), (1, 1, 14, 1, "#77766D"),
        (1, 14, 14, 1, "#4C4A45"), (1, 3, 3, 2, "#77766D"), (5, 2, 2, 1, "#A39A83"),
        (9, 3, 3, 1, "#4C4A45"), (12, 4, 2, 2, "#77766D"), (3, 7, 3, 2, "#4C4A45"),
        (7, 6, 2, 2, "#77766D"), (10, 8, 4, 2, "#4C4A45"), (2, 11, 2, 2, "#77766D"),
        (6, 11, 3, 1, "#A39A83"), (8, 12, 2, 1, "#77766D"), (5, 4, 1, 1, "#4C4A45"),
        (13, 10, 1, 1, "#A39A83"), (4, 13, 1, 1, "#77766D"), (11, 2, 1, 1, "#A39A83"),
    ],
}

ITEMS = tuple(name for name in PIXELS if name != "shielding_concrete")
OUTPUT_TEXTURE_PATHS = {
    f"textures/{'block' if name == 'shielding_concrete' else 'item'}/{name}.png"
    for name in PALETTE
}


def make_svg(name: str) -> str:
    """将像素矩形清单转为严格渲染器支持的直属rect SVG。"""
    body = "\n".join(
        f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{color}"/>'
        for x, y, w, h, color in PIXELS[name]
    )
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" '
            f'viewBox="0 0 16 16" shape-rendering="crispEdges">\n{body}\n</svg>\n')


def write_json(path: Path, value: dict) -> None:
    """稳定写入缩进一致的游戏原生模型JSON。"""
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def seed_missing_sources() -> None:
    """只在源稿缺失时创建草绘，后续运行始终以可编辑SVG为准。"""
    SOURCE.mkdir(parents=True, exist_ok=True)
    for name in PIXELS:
        path = SOURCE / f"{name}.svg"
        if not path.exists():
            path.write_text(make_svg(name), encoding="utf-8")


def install_models() -> None:
    """为11件物品和普通cube_all方块生成最小原生模型引用。"""
    for name in ITEMS:
        write_json(ASSET / "models/item" / f"{name}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"create_nuclear_industry:item/{name}"},
        })
    write_json(ASSET / "models/block/shielding_concrete.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": "create_nuclear_industry:block/shielding_concrete"},
    })
    write_json(ASSET / "models/item/shielding_concrete.json", {
        "parent": "create_nuclear_industry:block/shielding_concrete",
    })
    write_json(ASSET / "blockstates/shielding_concrete.json", {
        "variants": {"": {"model": "create_nuclear_industry:block/shielding_concrete"}},
    })


def render_and_install() -> dict[str, tuple[int, int, int]]:
    """逐份严格渲染SVG，并仅写本批12个generated与游戏PNG路径。"""
    results = {}
    for name, colors in PALETTE.items():
        source = SOURCE / f"{name}.svg"
        if not source.is_file():
            raise FileNotFoundError(f"缺少可编辑源稿：{source}")
        image = strict_exporter.render_svg(source.read_text(encoding="utf-8"), colors, (16, 16))
        if image.size != (16, 16) or image.mode != "RGBA":
            raise ValueError(f"{name}渲染结果必须为16×16 RGBA，实际为{image.size} {image.mode}")
        counts = {value: count for count, value in (image.getchannel("A").getcolors(maxcolors=256) or [])}
        if set(counts) - {0, 255}:
            raise ValueError(f"{name}包含非二值透明度")
        if name == "shielding_concrete" and counts.get(0, 0):
            raise ValueError("普通cube_all混凝土纹理必须完全不透明")
        if name != "shielding_concrete" and not counts.get(0, 0):
            raise ValueError(f"{name}物品图标应保留透明边缘")
        generated = GENERATED_BLOCK if name == "shielding_concrete" else GENERATED_ITEM
        game_folder = "block" if name == "shielding_concrete" else "item"
        (generated / f"{name}.png").parent.mkdir(parents=True, exist_ok=True)
        (ASSET / "textures" / game_folder).mkdir(parents=True, exist_ok=True)
        image.save(generated / f"{name}.png")
        image.save(ASSET / "textures" / game_folder / f"{name}.png")
        bounds = image.getchannel("A").getbbox()
        results[name] = (bounds[0], bounds[1], counts.get(0, 0)) if bounds else (0, 0, counts.get(0, 0))
    return results


def checker(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], light: str, dark: str, cell: int = 8) -> None:
    """给透明物品区域铺棋盘底，能同时看清浅色和深色轮廓。"""
    x0, y0, x1, y1 = box
    for y in range(y0, y1, cell):
        for x in range(x0, x1, cell):
            color = light if ((x - x0) // cell + (y - y0) // cell) % 2 == 0 else dark
            draw.rectangle((x, y, min(x + cell - 1, x1 - 1), min(y + cell - 1, y1 - 1)), fill=color)


def preview_sheet(background: str, name: str, panel: tuple[str, str]) -> Path:
    """输出含实际16像素展示与8倍最近邻图的独立证据预览。"""
    width, height, columns = 1160, 800, 4
    image = Image.new("RGB", (width, height), background)
    draw = ImageDraw.Draw(image)
    font = ImageFont.load_default()
    draw.text((18, 14), f"EXT-A-REACTOR-01 | {name} background | original 16x16 and 8x nearest", fill="#FFFFFF" if background == "#202a2f" else "#202020", font=font)
    cell_w, cell_h = 284, 248
    for index, asset_name in enumerate(PIXELS):
        column, row = index % columns, index // columns
        x, y = 14 + column * cell_w, 48 + row * cell_h
        title_color = "#FFFFFF" if background == "#202a2f" else "#202020"
        draw.text((x + 5, y + 2), asset_name, fill=title_color, font=font)
        small = Image.open((GENERATED_BLOCK if asset_name == "shielding_concrete" else GENERATED_ITEM) / f"{asset_name}.png").convert("RGBA")
        draw.text((x + 5, y + 26), "Original 16x16", fill=title_color, font=font)
        if asset_name == "shielding_concrete":
            draw.rectangle((x + 12, y + 48, x + 68, y + 104), fill="#51514f")
            image.paste(small, (x + 32, y + 68))
        else:
            checker(draw, (x + 12, y + 48, x + 68, y + 104), panel[0], panel[1], cell=8)
            image.paste(small, (x + 32, y + 68), small)
        draw.text((x + 86, y + 26), "8x nearest neighbor", fill=title_color, font=font)
        if asset_name == "shielding_concrete":
            draw.rectangle((x + 92, y + 48, x + 220, y + 176), fill="#51514f")
            image.paste(small.resize((128, 128), Image.Resampling.NEAREST), (x + 92, y + 48))
        else:
            checker(draw, (x + 92, y + 48, x + 220, y + 176), panel[0], panel[1], cell=16)
            image.paste(small.resize((128, 128), Image.Resampling.NEAREST), (x + 92, y + 48), small.resize((128, 128), Image.Resampling.NEAREST))
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    target = EVIDENCE / f"reactor-01-{name}-preview.png"
    image.save(target)
    return target


def sha256(path: Path) -> str:
    """读取文件字节并返回稳定SHA-256校验值。"""
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> None:
    seed_missing_sources()
    before = sorted(
        path for path in (ASSET / "textures").rglob("*.png")
        if path.is_file() and path.relative_to(ASSET).as_posix() not in OUTPUT_TEXTURE_PATHS
    )
    before_hashes = {path.relative_to(ASSET).as_posix(): sha256(path) for path in before}
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    (EVIDENCE / "existing-game-pngs-before.json").write_text(json.dumps(before_hashes, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    bounds = render_and_install()
    install_models()
    after_hashes = {path.relative_to(ASSET).as_posix(): sha256(path) for path in before}
    if before_hashes != after_hashes:
        raise RuntimeError("已有游戏PNG字节发生变化，未通过保留核对")
    light = preview_sheet("#f4f2e9", "light", ("#EEEEEE", "#C8C8C8"))
    dark = preview_sheet("#202a2f", "dark", ("#536168", "#37444a"))
    (EVIDENCE / "resource-check.json").write_text(json.dumps({
        "task": "EXT-A-REACTOR-01-ART",
        "game_png_before_count": len(before),
        "game_png_before_hashes": before_hashes,
        "game_png_after_existing_hashes_match": before_hashes == after_hashes,
        "new_game_pngs": [f"textures/{'block' if name == 'shielding_concrete' else 'item'}/{name}.png" for name in PALETTE],
        "source_svg_count": len(list(SOURCE.glob("*.svg"))),
        "source_svg_paths": [str((SOURCE / f"{name}.svg").relative_to(ROOT).as_posix()) for name in PALETTE],
        "item_model_count": len(ITEMS),
        "block_models_and_state": [
            "models/block/shielding_concrete.json", "models/item/shielding_concrete.json", "blockstates/shielding_concrete.json"],
        "transparent_bounds_and_zero_alpha_pixels": bounds,
        "previews": [str(light.relative_to(ROOT).as_posix()), str(dark.relative_to(ROOT).as_posix())],
    }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"已导出12张16×16纹理、11个物品模型及屏蔽混凝土cube_all资源；原有PNG SHA-256核对通过：{len(before)}张")
    print(f"明底预览：{light}\n暗底预览：{dark}")


if __name__ == "__main__":
    main()
