# 汽轮机机壳B方案候选

本目录保存ART-TURBINE-07B的两张原创SVG、导出PNG、独立MTL、212份OBJ UV映射、映射清单与实际模型预览。两张PNG、独立MTL及212份OBJ已安装；客户端内的真实游戏视觉仍待复看。

- [真实OBJ/MTL/UV预览](index.html)：未成型up基准、d3/d5/d7成型组合。
- [面板SVG](panel.svg)：钢束带与窄铜缝；[端盖SVG](endcap.svg)：钢法兰与铜内嵌面。
- [212个机壳OBJ映射清单](mapping.json)。

外壳U取实际标准局部Z。V按完整截面同一外法线平面的全局切向边界计算，裁切邻格共享UV，不逐face归一化。端盖UV按完整d3/d5/d7截面XY归一化。未成型朝向按旧生成器六向预烘焙变换逆回up基准。独立MTL只替换casing/endcap；inside/edge继续用原贴图。候选OBJ只改mtllib与目标面引用的vt；v/vn/f/usemtl/o保持原样。

工具脚本只生成和检查候选副本；正式PNG、MTL与OBJ已安装，并已完成一次assemble。游戏未运行，客户端真实视觉仍待复看。低分辨率贴图源为原创16×16整数rect SVG，PNG由同源SVG确定性导出。
