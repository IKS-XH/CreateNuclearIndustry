# 汽轮机B-R2：中大型疏线与连窗试用版

用户反馈小型效果较好、中大型线条密集且观察窗未连接后，本批已安装新的中大型稀疏钢面与客户端连窗模型。小型d3沿用现有节奏；d5每两格、d7每三格保留一组短铜缝/刻线，无带段形成完整钢面。轴向仍为16像素/世界格，截面沿原外表面UV，上一版R1端盖保留。

观察窗沿汽轮机长轴连接，撤掉相邻处完整实体横框和共享玻璃端面，保留窗带两侧与首尾厚框。连续玻璃采样原贴图有色内部，消除透明留白造成的分格蓝片；单窗保持原模型。只有合法中段、同尺寸/位置/朝向/成型状态的已定位窗口相连，普通壳、端盖或其他部件会截断。

在 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio` 运行：

```powershell
.\gradlew.bat runClient
```

先保存退出旧客户端，再从美术树重启并打开已有存档。这次包含新增客户端模型代码，F3+T不能替代重启。重点复看中、大型钢面与刻线密度，连续窗是否去掉内框、断开处是否保留端框，以及顶/底/两侧的玻璃透视和昼夜光照；重启后也可用F3+T观察资源重载。

- [三档机壳与实际连窗对照](http://127.0.0.1:8735/tools/art-assets/turbine-casing-art-07-r2/index.html)
- [资产报告](reports/ART-TURBINE-07B-R2-ASSETS.md)
- [消费与原始验证记录](reports/ART-TURBINE-07B-R2-CONSUMER.md)
- [一次独立窄审](reports/ART-TURBINE-07B-R2-REVIEW.md)
- [全页展示截图](../../build/reports/art/ART-TURBINE-07B-R2/final-model-preview.png)
- [连窗视口截图](../../build/reports/art/ART-TURBINE-07B-R2/final-window-preview.png)

当前最终检查为壳面4/4及玻璃窄修后的窗口4/4；唯一增量assemble退出0、1秒。独立审查未发现必改项，5源/测试、5新class和唯一新PNG与当前候选绑定一致。原始失败、首次8/8、玻璃窄修的RED/GREEN及旧mesh分开保存，未重跑旧业务或全量测试。

当前 `build/libs/create_nuclear_industry-0.1.0.jar` 为2,616,697字节，SHA256 `b1c3c69c29dce947b99ccd0f0a187e9689958217a2f3727c80487d9033cd8d88`；同一冻结候选在 `build/reports/art/ART-TURBINE-07B-R2/consumer/ART-TURBINE-07B-R2.jar`。原R1 JAR与旧来源/证据保持，完整绑定见该目录的verification.json与freeze.json。

负责人已完整读取报告、原始日志/XML及独立审查，复看真实OBJ与生产窗口mesh预览并核当前源/JAR身份。内部试用交付完成，游戏视觉仍待用户在原存档复看；预览不替代原生GPU效果。源码和资源留在美术树未提交，未合main；没有启动游戏、写存档或Git。
