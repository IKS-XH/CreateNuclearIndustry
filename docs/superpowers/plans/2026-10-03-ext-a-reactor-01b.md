# EXT-A-REACTOR-01B：仪表端口提升至21格制造

**状态：实现、6项定向GameTest、增量assemble和独立复审通过，客户端仍待验。** 实现提交`70b55c403441891d91b22dc87d55bfc900a8f4a9`，见[交接清单与证据](../../reviews/2026-10-03/reactor-01/README.md)。 用户明确要求仪表端口更昂贵、与离心机同级；沿用“数值和设定先用推荐值”授权，PM确定以下单配方调整，取代01A中的三格横排方案。没有宣称原两批人工验收通过。

## 冻结合同

沿用`create:mechanical_crafting`与现有配方路径，固定5×5去四角21格、禁止镜像，每次产1个现有`reactor_instrument_port`。直接投入10钢板、6金板、2精密构件、1工业传感器、1电子管、1反应堆外壳。相对三格配方增加10钢板＋6金板＋2精密构件；与离心机共享21格、钢/金板与精密构件制造层级，不要求两种机器材料完全等价。

```text
 SPS
SGIGS
SGCGS
SGTGS
 SPS
```

每行长度5，首末行末尾各一个空格；四角为空且不占动力合成器。S=`c:plates/steel`，G=`c:plates/gold`，P=`create:precision_mechanism`，I=现有工业传感器，T=`create:electron_tube`，C=正式反应堆外壳。两个P位于顶/底中央，传感器/外壳/电子管依次位于中间三行中央。原小配方原地替换，无工作台/序列备用路线。其它配方、注册、运行行为、纹理与存档均不变。

## 写集与验证

候选`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支`codex/ore-acquisition`，实现基线`25b4932`；既有未跟踪`tools/art-assets/__pycache__/`不动，主工程用户启动配置不动。执行者只可修改：

- `src/main/resources/data/create_nuclear_industry/recipe/mechanical_crafting/reactor_instrument_port.json`。
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionReactorCraftingGameTests.java`中仪表端口的现有期望布局：5×5、25位置含4空位，各投入数量与单件结果；其余断言保持，不新增测试框架/物理Crafter探针。
- `build/reports/extension/EXT-A-REACTOR-01B.md`与同名证据目录：简短报告列出基线、实际技能应用、命令结果、JAR哈希、配方打包和手测边界。

必读AGENTS、治理5.1、本卡；实际使用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`与`minecraft-testing/SKILL.md`。锁定Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82。中文注释；不得Git写、改核心docs/其它源码/构建、操作用户客户端/存档或再派发。

更新现有加载态布局断言后，仅运行一次最小已有隔离组及增量打包：`./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_reactor_crafting -PgameTestDirectory=build/gametest-reactor-crafting-01b assemble --console=plain`。该6项小组为当前已有筛选边界，不额外改构建实现单方法筛选；不重复JUnit、01B冷却剂组或全量，不clean/rerun-tasks。核对JAR只有新的21格仪表配方，输出1且其余两条机械配方保持。加载配料检查不称为真实动力合成加工，实际21格连线、JEI和出料保留客户端手测。

PM负责一次独立差异/证据复审，不另跑构建；更新现行配方表、原01成本与合并手测清单，归档后保存候选，不合入main运行内容。Ponder单堆仍1仪表端口，因此结构账由157增为167钢锭当量，另加6金板、2精密构件；已有外壳/传感器/电子管数量不变。
