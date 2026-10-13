"""核本批固定宣传渲染的尺寸、透明边缘、125格列合同和实际读入来源；不运行Minecraft。"""
from pathlib import Path
from collections import Counter
import json
import hashlib
from PIL import Image

ROOT = Path(__file__).resolve().parent
REPORT = Path('E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-MCMOD-COVER-13/background')
MAIN = Path('E:/MyMC/NewMod/Create_NuclearIndustry')
data = json.loads((ROOT / 'render-evidence.json').read_text(encoding='utf-8'))
layout = data['layout']
blocks = {tuple(cell['pos']): cell['id'] for cell in layout['blocks']}
assert len(blocks) == 125
assert set(blocks) == {(x, y, z) for x in range(5) for y in range(5) for z in range(5)}
prefix = 'create_nuclear_industry:'
for x in range(1, 4):
    for z in range(1, 4):
        assert blocks[x, 4, z] == prefix + ('control_rod_drive' if x == 2 else 'reactor_refueling_port')
        for y in range(1, 4):
            assert blocks[x, y, z] == ('minecraft:air' if x == 2 else prefix + 'reactor_fuel_rod')
for x in range(1, 4):
    for y in range(1, 4):
        assert blocks[x, y, 0] == prefix + 'reactor_window'
for pos, block in blocks.items():
    boundary_axes = sum(c in (0, 4) for c in pos)
    if boundary_axes >= 2 or pos[1] == 0:
        assert block == prefix + 'reactor_casing'
assert Counter(blocks.values())[prefix + 'reactor_instrument_port'] == 1
assert Counter(blocks.values())[prefix + 'reactor_cold_port'] == 1
assert Counter(blocks.values())[prefix + 'reactor_hot_port'] == 1
image = Image.open(ROOT / 'reactor-5x5x5.png')
assert image.size == (2048, 2048) and image.mode == 'RGBA'
alpha = image.getchannel('A')
bbox = alpha.getbbox()
assert bbox and min(bbox[0], bbox[1], 2048 - bbox[2], 2048 - bbox[3]) > 60
assert all(alpha.getpixel(point) == 0 for point in [(0, 0), (2047, 0), (0, 2047), (2047, 2047)])
for source in data['sources']:
    assert hashlib.sha256(Path(source['path']).read_bytes()).hexdigest() == source['sha256'], source['path']
# 结构列与蓝辉只读来源另外登记，不把网页显示工况当服务端热工结果。
for name in ['structure/ReactorStructureDefinition.java', 'structure/client/ReactorInternalRenderer.java']:
    path = MAIN / 'src/main/java/com/iksxh/create_nuclear_industry' / name
    raw = path.read_bytes()
    data['sources'].append({'path': str(path), 'bytes': len(raw), 'sha256': hashlib.sha256(raw).hexdigest()})
(ROOT / 'layout.json').write_text(json.dumps(layout, ensure_ascii=False, indent=2), encoding='utf-8')
(ROOT / 'sources.json').write_text(json.dumps(data['sources'], ensure_ascii=False, indent=2), encoding='utf-8')
summary = {'checks': '125格、外壳边角/底面、六燃料列/三控制列与顶盖、唯一仪表/冷热口、九连窗、RGBA尺寸与留白、真实读取hash', 'layoutCells': len(blocks), 'counts': dict(Counter(blocks.values())), 'size': image.size, 'alphaExtrema': alpha.getextrema(), 'contentBoundsPx': bbox, 'sourceCount': len(data['sources']), 'pngSha256': hashlib.sha256((ROOT / 'reactor-5x5x5.png').read_bytes()).hexdigest(), 'drawCalls': data['drawCalls'], 'triangles': data['triangles'], 'gameClientRun': False}
(REPORT / 'verification.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(summary, ensure_ascii=False, indent=2))
