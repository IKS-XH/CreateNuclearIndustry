# 汽轮机机壳 R2 疏线候选

本页保留R2导出与预览合同，以下客户端“待复看”是交付时记录；该批验收与当前美术状态以[STATUS](../../../docs/STATUS.md)为准。

本目录记录中大型机壳的稀疏纹理试用素材和离线模型预览。d5 使用上排两个相位格，d7 使用下排三个相位格；每格为 16×16 像素，轴向密度保持每世界格16像素。d3沿用现行B纹理，R1端盖保持不变。

- [8格长实际OBJ旧/新预览及观察窗连接对照](index.html)：机壳按 front、六个 middle、rear 排列；观察窗对照使用消费者提供的实际单窗和三窗连接mesh数据。
- [64×32原创SVG atlas](turbine_casing_07_sparse.svg)与[PNG](generated/textures/turbine_casing_07_sparse.png)。
- [UV与来源映射](mapping.json)。
- [本批资产报告](../../../docs/archive/2026-10-15/art/reports/ART-TURBINE-07B-R2-ASSETS.md)。

唯一新增PNG已安装试用，安装字节核对通过。负责人已复看离线机壳和实际连接mesh预览；实际游戏客户端视觉仍待用户复看。消费者负责本批唯一候选构建。候选工具复用07B只读OBJ解析器，生成输出仅写本目录。客户端相位是纯装饰，按世界长轴坐标与机组朝向计算；本预览固定south朝向和轴向起点0，便于观察周期。横截面V保持既有完整外表面UV，不调整跨截面比例。观察窗预览采用消费者提供的mesh与UV证据，仅用于离线展示，不代表客户端渲染验收。

重新生成候选：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/turbine-casing-art-07-r2/build_candidate.py
```
