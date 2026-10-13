# 汽轮机B端盖R1：历史游戏试用版

当前中大型疏线与连窗试用请看[07B-R2说明](ART-TURBINE-07B-R2-CANDIDATE.md)。本页保留R1端盖及原验证的历史记录。

用户实机反馈“两端太丑了”后，两端已重绘为分块钢板、细八角法兰、螺栓座和内凹轴座，铜缩为小嵌片。贴图由原创整数SVG导出，同一规则适配既有3/5/7格截面，保持每世界格16像素的细节密度。侧壳继续使用已选第二稿，实际轴件和其他部件保持现行版本。

在`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`目录运行：

```powershell
.\gradlew.bat runClient
```

若旧客户端仍开着，先保存退出，再从美术树启动，打开已有存档即可。重点复看三档两端的整体钢色、法兰和拼缝是否清楚，以及轴件周围的安装面是否协调。浏览器是实际机壳前/中/后三模块的OBJ新旧对照，未包含完整机组或实际输出轴，不替代游戏视觉观察。

- [旧新模型预览](http://127.0.0.1:8735/tools/art-assets/turbine-endcap-art-07-r1/index.html)
- [实施报告](reports/ART-TURBINE-07B-R1.md)
- [独立差异窄审](reports/ART-TURBINE-07B-R1-REVIEW.md)
- [最终预览截图](../../build/reports/art/ART-TURBINE-07B-R1/final-model-preview.jpg)

本轮精确安装164个OBJ的endcap纹理坐标及一张端盖PNG，共165项；非endcap坐标、几何、MTL和侧壳保持。源核对、安装和JAR的165项字节一致，唯一增量assemble退出0、5秒。没有新增或运行测试、启动游戏或操作存档。

一次独立差异窄审未发现生产必改项，确认atlas映射与现行flip_v=false消费一致，并独立看最终三档/未成型截图。负责人已完整读取实施、原始日志及审查，复看最终模型，内部交付结束；真实轴件遮盖、昼夜照明与整体审美仍由游戏复看确认。

当前JAR为2,603,740字节，SHA256：`4724e54c45e18b7c1917e3272fe3cf2b8e217b89f6cfc7fc0466f957955810b9`。原07B JAR已完整备份至`build/reports/art/ART-TURBINE-07B-R1/pre-assemble-create_nuclear_industry-0.1.0.jar`，原07B来源和证据保留。本批仍在美术树未提交，未合main；用户游戏视觉待复看。
