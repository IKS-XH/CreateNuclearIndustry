# ART-REACTOR-02R1独立作者源

272份16×16整数rect SVG：13项各16格RECTANGLE/64×64，观察窗64格OMNIDIRECTIONAL/128×128。窗口47有效上下文和17不可达槽隔离回退分别登记；索引由锁定Create 6.0.10真实运行输出固化，专属JUnit逐项核对mapping，不实现替代UV公式。

13项保留原深色基底、原面积嵌板、凹槽/亮侧/阴影和功能件。只在整面外沿保留粗钢框，内部压板使用1px台阶；孤立格等于原sprite。窗口只连接同可靠窗簇；孔延伸到合法共享边，角透明须两邻边与对应对角全连接。不同owner/仪表打断保留外圈和真实凹角。其他材质可连接窗口背景。

`export.py`只读共用严格SVG renderer；272源/色板/mapping全部验证和内存编码后才写出。唯一例外是原生索引54（mask255）的全透明内窗：只允许严格空根SVG及注释，其他空源继续被共享renderer拒绝。RGBA≤16色、alpha0/255，非窗口全不透明。所有游戏纹理像素只由保存的SVG渲染并粘贴；棋盘、光照和几何只用于离线预览。

14个完整原sprite/target ID保持：目标`create_nuclear_industry:block/reactor_ct/<name>`。工具图集`generated/<name>_ct.png`对应game `reactor_ct/<name>.png`；默认仅写本generated和R1证据，没有game安装入口。未修改旧02A、ART01或共享工具。

```powershell
# 每条命令workdir固定 E:/MyMC/NewMod/Create_NuclearIndustry-art-studio
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B 'tools/art-assets/reactor-ct-r1/export.py'
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B 'tools/art-assets/reactor-ct-r1/export.py' --check
```

默认预览要求本批专属JUnit生成`build/reports/art/ART-REACTOR-02R1/native-window-scenes.json`。完整混合三联/cube同缩放光照；窗簇索引来自真实buildContext/getTextureIndex。厚度页使用实际ScopedCTModel一次capture后ModelData与getQuads(side=null)保留的方向清单，仅画视角朝向的NORTH/UP/WEST面，非外向面用原SVG；缺失共享面不是手工猜测。非共享背面SOUTH仍保留在清单，只因本视角背朝外不显示。旧对照按原四向占格与owner边界拼合且保留接口。离线预览不能替代客户端材质光照、半透明排序或用户视觉验收；安装与最终增量包须负责人实际复看允许。
