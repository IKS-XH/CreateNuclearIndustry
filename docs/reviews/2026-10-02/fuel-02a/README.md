# 燃料02A：生芯块与燃料烧结炉

**状态：候选`575400e`已通过定向验证及独立审查，等待用户客户端手测，尚未合入main功能。** 已批准合同见[方案](../../../superpowers/plans/2026-10-02-fuel-sintering-furnace-proposal.md)和[任务卡](../../../superpowers/plans/2026-10-02-ext-a-fuel-02a.md)。本页只覆盖低浓缩铀粉→生燃料芯块→烧结燃料芯块，后续包壳、燃料棒和组件尚未实施。

## 启动与人工门

功能放在同级候选工作树；主目录目前仍为已验收的离心机版本。完整退出旧客户端再运行：

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
.\gradlew.bat runClient
```

使用[一张四组手测清单](./CLIENT-CHECKLIST.md)。本批人工通过前不合入main功能、不继续包壳或装配；不重复旧矿物、材料和离心机验收。

## 交付与证据

- 压片1低浓缩铀粉→1生芯块；炉下另放完整烈焰人燃烧室，普通加热下每400有效tick烧成1件。单格无GUI，无转轴，顶进四侧出，输入/输出各64件。
- 炉体9格动力合成器：`RSR/RBR/SIS`，R为耐火砖、S为钢板、B为完整燃烧室、I为工业传感器；制造与运行总需两台完整燃烧室。
- 定向JUnit为4/4：400tick、断热与满输出暂停恢复、全部/部分取生料、快照及模拟不变性。
- 隔离GameTest为1/1：真实Create燃烧室煤供热、无热/阴燃拒绝、原生漏斗顶面投料、烧结与侧面capability，以及FakePlayer部分/满背包取料守恒。侧出没有外接Create漏斗，不能记为物流端到端通过。
- 美术冻结后一次增量`assemble`退出0，用时4秒。PM核对JAR中的24项本批资源与源码逐字节一致、6个本批新类存在。JAR为候选`build/libs/create_nuclear_industry-0.1.0.jar`，SHA-256：`877E9C41D8E9BC4CD05B0FFEE2C2AD412FC806A34E1802113530B6BBA865F0FE`。
- 两枚芯块、矮八棱炉体与冷热窗口合计7对SVG/PNG；3个物品模型、8个方块状态。PM实际看过明暗底物品图与两个JSON几何角度。已修复底座断层、遗漏物品模型及导出覆盖源稿问题；旧离心机和冷却剂纹理无改动。

[运行交付](./EXT-A-FUEL-02A-runtime.md)、[美术交付](./EXT-A-FUEL-02A-assets.md)、[独立审查](./EXT-A-FUEL-02A-REVIEW.md)、[最终制品核对](./artifact-check.json)索引必要证据；原始JUnit XML、GameTest日志和构建控制台随报告同目录归档。美术报告UV计数的文字笔误由PM按检查JSON订正，未改资源。独立审查未发现P1/P2阻断。

JEI、世界内护目镜/交互、玩家挖掘与扳手携物重放、实际压片/动力合成及新模型仍待人工。普通与超级加热同速是同一状态逻辑和Create热级源码支持的实现结论，本批没有逐档实测全部热级。不把构建或离线预览当作客户端验收。

按治理5.1复用未变证据：未跑旧JUnit/GameTest全量、旧素材负例或客户端，也未使用clean/强制重跑。仅因审查定位的背包部分接收风险复验受影响GameTest。

根目录两份历史跟踪日志曾被本次JUnit改写，PM确认来源后仅恢复这两份文件，最终差异检查通过；用户默认run目录未改动。主工程`.vscode/launch.json`原有修改完整保留。候选提交后只同步本批文档，不再重复构建或测试。
