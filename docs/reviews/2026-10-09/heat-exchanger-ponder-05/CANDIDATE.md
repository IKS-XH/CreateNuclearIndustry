# 核换热器思索：R4锅炉剖面复看

**状态：R4候选`d6e17c7`已提交，4/4定向合同、增量打包与独立窄复核通过，待用户播放。** 原默认镜头下控制器、东侧墙和顶盖遮挡核心。本轮讲解时移开两面近墙、顶盖及内部邻壳，核心留在原位，先讲换热器再讲再加热段，之后恢复端口管路，最后完整恢复。教学尚未合main。

其他四幕、模板及生成器未改，已验收功能不重测。其他范围若尚未验收，保留原状态，本页不代替验收。

## 启动与本轮复看

重启候选客户端加载本次Java及双语：

```powershell
Set-Location E:\MyMC\NewMod\Create_NuclearIndustry-ore-acquisition
.\gradlew.bat runClient
```

对核换热器按W，切到第三幕 **锅炉内置**，只复看：

1. 开头完整锅炉站在地台上。
2. 剖开后底层换热器在原位，本体与单独高亮清楚可见；字幕不遮挡目标。
3. 随后可看见上方再加热段，与底层换热器的关系明确。
4. 两段讲完恢复冷热端口和管路，最后外壳、顶盖完整恢复。

高亮与正文自带轮廓的叠加观感、实际画面及阅读效果待本次客户端确认。无需重跑其他四幕或已通过的设备功能。

## 自动证据

- 实现与报告提交`d6e17c7`；[实施](./IMPLEMENTATION.md)和[窄复审](./REVIEW.md)均追加R4，历史过程保留。
- Ponder1.0.82的hide/mask及默认镜头按实际API核对；测试直接调用生产Selection，两个核心共54条射线净空并核对分阶段恢复。几何不替代视觉验收。
- `test-r4.exit=0`，XML为4 tests、0 failures、0 errors、0 skipped；`assemble-r4.exit=0`。模板未变，复用R3五模板证据；没有全量、GameTest或旧存档测试。
- `build/libs/create_nuclear_industry-0.1.0.jar`，2330001字节，SHA256 `C9DD6BD4423D71CA3FB1D55620134AFCFC3B97F3ACABF11315FC723A2C9B8604`。

原始日志/API/几何/XML保存在候选`build/reports/extension/DEVICE-PONDER-05-EXCHANGER/`。待本台播放门，不自动推进其他主线。
