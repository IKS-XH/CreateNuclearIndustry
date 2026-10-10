"""只用实际PNG/SVG导出帧作离线对照，预览不代表客户端显示通过。"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import producer

EVIDENCE = producer.ROOT / "build/reports/art/ART-COOLANT-04/preview"
FONT = "C:/Windows/Fonts/msyh.ttc"


def font(size):
    return ImageFont.truetype(FONT, size)


def image_frame(kind, flow, index):
    name = ("hot_" if kind == "hot" else "") + "compound_coolant_" + ("flow" if flow else "still")
    size = 32 if flow else 16
    sheet = Image.open(producer.GENERATED / (name + ".png")).convert("RGB")
    return sheet.crop((0, index * size, size, (index + 1) * size))


def board(index=0):
    canvas = Image.new("RGB", (1260, 650), "#182128")
    draw = ImageDraw.Draw(canvas)
    draw.text((25, 16), "ART-COOLANT-04 同源材质对照｜离线资产示意", font=font(26), fill="white")
    labels = ["旧普通still / flow", "内部冻结帧16×16", "新still 16×16", "新flow 32×32 (2×2)"]
    for column, label in enumerate(labels):
        draw.text((25 + column * 310, 65), label, font=font(19), fill="#BCD4DE")
    for row, kind in enumerate(("cold", "hot")):
        y = 135 + row * 220
        prefix = "hot_" if kind == "hot" else ""
        old = producer.ROOT / "build/reports/art/ART-COOLANT-04/baseline/old-png/block"
        for j, variant in enumerate(("still", "flow")):
            p = Image.open(old / f"{prefix}compound_coolant_{variant}.png").convert("RGB")
            canvas.paste(p.resize((112, 112), Image.Resampling.NEAREST), (25 + j * 125, y))
        internal = Image.open(producer.ROOT / f"tools/art-assets/reactor-animation/generated/textures/coolant_{kind}.png").convert("RGB")
        internal = internal.crop((0, index * 16, 16, (index + 1) * 16))
        canvas.paste(internal.resize((192, 192), Image.Resampling.NEAREST), (360, y))
        canvas.paste(image_frame(kind, False, index).resize((192, 192), Image.Resampling.NEAREST), (670, y))
        # flow全帧以同一预览宽度展示四象限；图集本身保持原16像素纹样精确重复。
        canvas.paste(image_frame(kind, True, index).resize((192, 192), Image.Resampling.NEAREST), (980, y))
        draw.text((25, y + 125), "冷蓝" if kind == "cold" else "热橙", font=font(20), fill="white")
    draw.text((25, 592), f"帧{index}/7｜alpha255｜八帧×2tick，interpolate=true；GIF只显示100ms关键帧", font=font(18), fill="#BCD4DE")
    draw.text((25, 618), "普通atlas固定循环；内部保持真实库存混色/调速。储罐、管道、桶及冷液世界面待游戏复看。", font=font(17), fill="#BCD4DE")
    return canvas


def main():
    producer.check(producer.read_mapping())
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    frames = [board(i) for i in range(8)]
    frames[0].save(EVIDENCE / "same-source-comparison.png")
    # GIF使用同一来源的八关键帧；中间RGB插值交由原生atlas执行，不伪造游戏证据。
    frames[0].save(EVIDENCE / "coolant-loop.gif", save_all=True, append_images=frames[1:], duration=100, loop=0, disposal=2)
    print(EVIDENCE)


if __name__ == "__main__":
    main()
