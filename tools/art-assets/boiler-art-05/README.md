# ART-BOILER-05B 候选素材

本目录保存A横箍压力舱的原创16×16整数rect SVG源稿、同源PNG候选和离线预览。素材沿用05A已选定的钢色、暖金细节和工业暗线；基础面复用对应05A作者SVG，换热隔层与安全牌使用本批同色板矢量面。窗口源稿保留透明中孔与窄反光。

mapping.json固定12项基础模型纹理引用，以及负责人确认的9个CT target：

create_nuclear_industry:block/boiler_ct/{side,top,partition_side,water_port_face,steam_port_face,controller_face,hot_coolant_port,cold_coolant_port,window_frame}

8个RECTANGLE图集为64×64，窗口OMNIDIRECTIONAL图集为128×128。窗口47个上下文和17个回退槽直接取只读tools/art-assets/reactor-ct-r1/mapping.json；R1文件SHA写在本批mapping中。回退格使用孤立窗，避免不可达索引形成透明漏面。

preview/index.html展示基础图、9张CT图集，以及按原生八向邻接拼接的单窗、2×2和L形缺角。棋盘底色用于识别透明玻璃。阶段进度：贴图安装、窗口模型、CT消费和候选构建均已完成；真实客户端视觉待用户复看，候选尚未合入main。

重建候选PNG和预览：

& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/boiler-art-05/build_assets.py

只读检查：

& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/boiler-art-05/check_assets.py


