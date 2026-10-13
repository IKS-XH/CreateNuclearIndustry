"""核本批固定宣传渲染的尺寸、透明边缘、125格列合同和实际读入来源；不运行Minecraft。"""
from pathlib import Path
from collections import Counter
import json
import hashlib
from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parent
REPORT = Path('E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-MCMOD-COVER-13/background')
MAIN = Path('E:/MyMC/NewMod/Create_NuclearIndustry')
data = json.loads((ROOT / 'render-evidence.json').read_text(encoding='utf-8'))
baseline = json.loads((ROOT / 'history/pre-r1/render-evidence.json').read_text(encoding='utf-8'))
diagnostic = json.loads((ROOT / 'diagnostic-neutral-color.json').read_text(encoding='utf-8'))
assert data['presentation']['mode'] == 'r1-full-coolant'
assert diagnostic['presentation']['mode'] == 'color-diagnostic' and diagnostic['presentation']['coolant'] is False
assert data['layout'] == baseline['layout'] == diagnostic['layout']
assert data['camera'] == baseline['camera'] == diagnostic['camera']
assert data['lighting'] == baseline['lighting'] == diagnostic['lighting']
assert all(row['hasColorBefore'] is False and row['hasColorAfter'] is True and row['vertexColors'] is True and row['mapPresent'] is True and row['color'] == [1, 1, 1] for row in diagnostic['colorDiagnostics'])
coolant = data['presentation']['coolant']
assert coolant['textureId'] == 'create_nuclear_industry:fluid/compound_coolant_still'
assert coolant['bounds'] == {'min': [1.01, 1.01, 1.01], 'max': [3.99, 3.99, 3.99]}
assert coolant['frameTick'] == 0 and .3 <= coolant['alpha'] <= .45
assert coolant['depthWrite'] is False and coolant['depthTest'] is True and coolant['renderOrder'] == 2
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
before_image = Image.open(ROOT / 'history/pre-r1/reactor-5x5x5.png').convert('RGB')
diagnostic_image = Image.open(ROOT / 'diagnostic-neutral-color.png').convert('RGB')
diagnostic_delta = ImageChops.difference(before_image, diagnostic_image)
assert diagnostic_delta.getbbox() is not None
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
for path in [Path('E:/MyMC/NewMod/block-model-renderer/src/core/models.js'), MAIN / 'tools/nuclear-plant-3d/src/equipment/asset-equipment.js']:
    raw = path.read_bytes()
    data['sources'].append({'path': str(path), 'bytes': len(raw), 'sha256': hashlib.sha256(raw).hexdigest()})
(ROOT / 'layout.json').write_text(json.dumps(layout, ensure_ascii=False, indent=2), encoding='utf-8')
(ROOT / 'sources.json').write_text(json.dumps(data['sources'], ensure_ascii=False, indent=2), encoding='utf-8')
summary = {'checks': 'R1诊断OBJ中性color、原相机/灯光/125格布局保持、满冷却液3轴包络与原生帧/alpha/深度、RGBA尺寸与留白、真实读取hash；原壳体/列检查复用同一入口', 'layoutCells': len(blocks), 'counts': dict(Counter(blocks.values())), 'size': image.size, 'alphaExtrema': alpha.getextrema(), 'contentBoundsPx': bbox, 'sourceCount': len(data['sources']), 'pngSha256': hashlib.sha256((ROOT / 'reactor-5x5x5.png').read_bytes()).hexdigest(), 'diagnosticPngSha256': hashlib.sha256((ROOT / 'diagnostic-neutral-color.png').read_bytes()).hexdigest(), 'diagnosticChangedBoundsPx': diagnostic_delta.getbbox(), 'objDiagnosticMeshes': len(diagnostic['colorDiagnostics']), 'coolant': coolant, 'drawCalls': data['drawCalls'], 'triangles': data['triangles'], 'gameClientRun': False}
(REPORT / 'verification.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(summary, ensure_ascii=False, indent=2))
