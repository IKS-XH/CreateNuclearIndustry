# ART-TURBINE-07B 交付报告

汽轮机机壳B方案候选已按负责人授权安装并完成一次增量`assemble`。本批只安装两张原创PNG、独立MTL和mapping.json中列出的212个OBJ；真实客户端视觉仍待用户复看。候选依照任务卡使用专属机壳MTL，避免替换会影响控制器、端口、窗口及叶轮的共享材质。

候选入口：[实际OBJ预览首页](../../../tools/art-assets/turbine-casing-art-07/index.html)。两张原创16×16整数rect SVG为[侧壳面板](../../../tools/art-assets/turbine-casing-art-07/panel.svg)和[八棱端盖](../../../tools/art-assets/turbine-casing-art-07/endcap.svg)，由同源源文件导出候选PNG。B映射为侧壳钢束带与窄铜缝、端盖钢法兰与铜内嵌片；inside/edge继续引用原贴图。

[mapping.json](../../../tools/art-assets/turbine-casing-art-07/mapping.json)列出精确212个OBJ候选：206个d3/d5/d7成型外壳与6个未成型朝向模型。仅改专用`mtllib`引用及casing/endcap面的必要`vt`；顶点、法线、面绕序、材质组和对象记录保持一致。成型侧壳U保留真实局部Z，V按完整截面同法线的全局切向范围计算并跨邻接裁切格连续；端盖按整档八棱截面XY映射。

负责人浏览器首轮发现首页预览引用路径错误、屏幕裁切坐标随纹理变换重复变换。已修正首页引用并将屏幕坐标裁切放在外层分组，负责人复看确认铜缝和端盖显现；再将内嵌贴图设为像素最近邻并缩短d3标题，负责人刷新复看通过。此离线预览不能代替游戏客户端视觉验收。

构建候选使用的实际命令为：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/turbine-casing-art-07/build_candidate.py
```

退出码为0，输出`CANDIDATE_READY svg=2 png=2 obj_targets=212 preview=4`，记录见[候选生成退出码](../../../build/reports/art/ART-TURBINE-07B/candidate-build-exit.txt)。

最终小检查的实际命令为：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/turbine-casing-art-07/build_candidate.py --check
```

退出码为0；原始输出见[最终检查记录](../../../build/reports/art/ART-TURBINE-07B/candidate-check-final-output.txt)。检查确认两组SVG/PNG一致、212项映射及几何保持、候选MTL纹理目标和首页预览引用。修正检查器前的首次检查因将`title`元数据误判为绘图图元而退出1，原始记录保留在[首次检查记录](../../../build/reports/art/ART-TURBINE-07B/candidate-check-output.txt)；检查器现跳过`title`并继续验证所有图元为整数rect。

阶段二安装前，使用mapping.json逐一核对212个原OBJ源SHA与候选SHA，均为212/212；安装后对212个OBJ、1个MTL和2张PNG逐字节比较，结果215/215一致，原始记录见[安装字节核对](../../../build/reports/art/ART-TURBINE-07B/install-byte-check.txt)。

安装前旧JAR已备份至[安装前JAR](../../../build/reports/art/ART-TURBINE-07B/pre-install-create_nuclear_industry-0.1.0.jar)，备份与原`build/libs/create_nuclear_industry-0.1.0.jar` SHA256一致：`DAE3321BEFC092903905BA42234EDA769BF53CAD3E5D1BC9F3EE22F64D73B245`。

按授权仅执行一次`./gradlew.bat assemble --console=plain`，退出码0，日志见[assemble原始输出](../../../build/reports/art/ART-TURBINE-07B/assemble-output.txt)。新JAR内215项与正式资源逐字节一致，核对退出码0，SHA256为`328072b13f941705fc86e8d62df7b13e6b90b1f9a3fc3851c34cfa279380167c`；记录见[JAR字节核对](../../../build/reports/art/ART-TURBINE-07B/jar-byte-check-output.txt)。未运行测试、游戏或客户端，未操作存档/Git；唯一构建与打包资源一致性不代替客户端视觉验收。
