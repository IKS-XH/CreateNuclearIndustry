"""仅提取本批真实模型父链所需的四份锁定原版JSON；不向生产目录写入。"""
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile

root = Path(__file__).resolve().parent / 'native'
jar = Path('C:/Users/IKSXH/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar')
paths = [f'models/{name}.json' for name in ('block/block', 'block/cube', 'block/cube_all', 'item/generated')]
records = {}
with ZipFile(jar) as source:
    for path in paths:
        entry = f'assets/minecraft/{path}'
        data = source.read(entry)
        target = root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
        records[path] = {'entry': entry, 'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest()}
root.joinpath('source-index.json').write_text(json.dumps({'minecraft': '1.21.1', 'client': str(jar), 'entries': records, 'notice': '本机原版游戏输入，原版权保持；不属本批原创MIT素材。'}, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps({'extracted': len(records), 'root': str(root)}))
