# 汽轮机思索04：镜头与排汽文案整改

**任务ID：** DEVICE-PONDER-04-FRAMING

**状态：已完成。** `a94f2ea`实现、独立静态复核及用户2026-10-09两页定向播放确认通过，见[验收](../../reviews/2026-10-09/turbine-ponder-framing/ACCEPTANCE.md)，净教学整合main。

**来源：** 用户2026-10-09截图反馈：搭建页小型机被底部进度条遮挡、大型机显示不完整；通汽页应写“从排汽口排出”。

## 已确认范围

- 搭建页三档设备分别展示，并分别居中、调整缩放与显示高度，使完整设备和当前重点处于标题、正文及底部按钮以外的可用画面。修复巨大基础板和分散坐标造成的偏移，不能只改全局缩放而保留同一问题。
- 核心、辅助包壳、接口和三档尺寸教学顺序不变；保持三故事板绑定、正式尺寸与玩法不变。
- 通汽页中文改为“超临界蒸汽从进汽口进入，蒸汽从排汽口排出。”，英文翻译及Java fallback语义同步。
- 沿用一次一段正文、留足文本退出和区段合并时间的规则，不引入新的重叠或空区段异常。

## 执行合同

工作区：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；基线`5ced0b7`。原有日志和Python缓存改动保留。项目经理不修改实现，由执行者在下列范围交未提交改动，禁止Git写、改核心文档或派发任务。

允许写集：

- `src/main/java/com/iksxh/create_nuclear_industry/ponder/TurbinePonderScenes.java`
- `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`、`en_us.json`：仅受影响的汽轮机思索键
- 如须调整模板：`tools/ponder/turbine_scenes.py`及`src/main/resources/assets/create_nuclear_industry/ponder/steam_turbine_build.nbt`
- 仅在真实模板几何合同变化时：`src/test/java/com/iksxh/create_nuclear_industry/TurbinePonderContractTest.java`
- `docs/reviews/2026-10-09/turbine-ponder-framing/IMPLEMENTATION.md`、`build/reports/extension/DEVICE-PONDER-04-FRAMING/`证据

实际读取技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`；调试采用`superpowers:systematic-debugging`，以本卡和精简验证规则为准。核对MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；镜头API以锁定依赖源码为准。

## 验证与交付

实际交付见[实现报告](../../reviews/2026-10-09/turbine-ponder-framing/IMPLEMENTATION.md)与[复核闭环](../../reviews/2026-10-09/turbine-ponder-framing/REVIEW.md)。首次增量构建及发现高亮错位后的增量构建均成功；模板未变，未重复合同测试、GameTest或客户端启动。半格位移采用同一组坐标构造AABB高亮框，文字键序不变。

1. 找到镜头偏移根因，核对锁定Ponder坐标/镜头变换；将三档逐个移到统一展示中心并抬升，整幕倍率容纳大型完整轮廓，不让设备继续分散在超大基础板各角。Ponder1.0.82的基础板、缩放、场景偏移是静态参数，不以多次写静态值伪装逐档动态缩放。
2. 静态检查场景显示/隐藏生命周期、文本间距、JSON与中英一致性；不写仅断言缩放常数的测试。
3. 未改模板只做一次增量`./gradlew.bat assemble`；改模板则一次`./gradlew.bat test --tests '*TurbinePonderContractTest' assemble`。保存完整输出和实际结果，不跑全量测试、clean、GameTest或用户客户端。
4. 报告实际写集、根因、镜头方案、技能、验证结果及人工边界。PM审查和窄范围提交；用户复看搭建页三档完整可见、通汽页排汽用词即可。本整改不重开已通过设备门，不推进后续教学；工作盆供热人工门仍单独保留。
