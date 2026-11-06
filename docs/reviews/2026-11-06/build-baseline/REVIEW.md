# BUILD-BASELINE-20261106 独立规格与质量组合窄审

结论：**PASS（源提交前窄审）**。未发现阻断问题。仅表示以下两项测试修复与已有定向证据符合任务卡；不宣布主分支、全量构建、正式制品或人工验收完成。

- 审查工作区：`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix`，分支 `codex/build-baseline-20261106`，HEAD `b8905473123d01c905daee4dc56198de66ad3052`。
- 只读审查两文件完整 Git diff、关联只读调用链、历史提交 `850904df6220487f8903b00cc9936b8f68307d32`、`EXECUTION.md`、`SOURCE.diff`、源清单、基线/定向 XML 与日志、诊断工具及 `first-divergence.log`。本审查没有运行测试、构建、客户端或 Git 写操作；唯一写入为本报告。
- 实际读取 AGENTS、治理5.1/6、STATUS、活动卡及 `C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-ci-release}/SKILL.md`。技能用于核实锁定版本、JUnit与真实游戏验证边界、提交源与制品门；MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6及Gradle9.0.0保持。

## 规格与实现核对

1. `HeatExchangerBoilerBridgeTest.java` 只补8项冷凝缓存并更新中文说明。新增项与 `HeatExchangerConfig.condensationSettings()` 的8次读取一一对应。每项先把原 `cachedValue` 保存进 `ORIGINAL_VALUES`，再设规格默认值；`@AfterAll` 遍历同表原样还原（包括原null）并清表，未加载/保存配置。原测试、真实pauseHeat入口、并发锁/重入断言均未改。基线两异常的真实链为 `pauseHeat:349 → topologyChanged:126 → HeatExchangerLine.find:33 → condensationSettings:64`，确属未加载配置的测试夹具遗漏，没有给生产代码加默认兜底。
2. `ReactorTickReference.java` 的行为变化仅为 `allocateQuantizedRemainder` 中逐列筛选从 `remaining > HEAT_EPSILON` 改为 `remaining > 0.0D`，另加中文说明。全堆阈值、总预算、逐列份额、末列补差及缓存热均保持。这与已验 `850904df` 的生产修复同一规则吻合，属于参考遗漏同步。
3. 独立裂变计算仍调用 `ReactorFissionReference.calculate`，未替换为生产 `ReactorFissionCalculator` 或 `ReactorServerTick.advance`；参考算法未被整体复制替换。`ReactorFissionReference.java` 和 `ReactorFissionDifferentialTest.java` 的当前SHA与基线输入记录相同。700固定种子布局、每布局两次比较（1400）、6×100完整tick（600）、调棒/卡死/燃耗/维护输入、完整结果严格相等均保持；无新增容差、skip、异常吞掉、删断言或覆盖缩水。

## 根因与已有自动证据

- 修改前 `baseline/`：13条桥接 + 2条差分，15项/3失败/0错误/0跳过，exit1；失败名和异常与任务所述一致。
- `first-divergence.log`：scenario0/tick4 的 controlSnapshot、fission、maintenance、coolant 均相等，首个阶段为thermal；首个字段为 `(0,1).quantizedHeatRemainderHu`。参考为 `0.0` / `0x0000000000000000`，生产为 `1.4490234156865493E-13` / `0x3d4464a8348e7b89`。该列实际remaining为 `1.4490234156865794E-13` / `0x3d4464a8348e7c00`，旧参考被1e-12逐列门漏掉。两侧总量同为 `0.2497484322059904` / `0x3fcff7c1b246a800`；诊断从未改差分输入逐tick推进，仅反射读取各自私有分配结果，证据支持参考漏同步，不能归咎为裂变求和顺序或缓存故障。
- 修改后 `targeted/` 四份XML：桥接13、差分2、ServerTick9、CoolantAdapter9，合33项/0失败/0错误/0跳过；日志 `BUILD SUCCESSFUL in 20s`，exit0，test实际执行。差分system-out确认1400与600次严格比较。
- 既有微尾热回归确在GREEN XML中，包括 `subEpsilonColumnsDoNotCreateAnAggregateShutdownDeadZone`、保存149列尾热、无冷却拒拆、`tinyPositiveHeatRetainsSafeQuantizationWithoutChangingFluid` 与缺冷液的真实热短缺；不是仅凭类名推定覆盖。
- `git diff --check`通过；Git仅两测试文件有diff，生产源码、配置、资源、依赖和构建脚本无diff。前后生产class清单逐行比较无差异（846条），compileJava/processResources均UP-TO-DATE。846是现有编译输出清单数，不代表正式JAR载荷数。

## 剩余门与风险

本次修复沿用现有反射缓存夹具机制；锁定NeoForge版本未变，没有增加生产风险。已核的class前后相同仅证明此次定向阶段未改变生产编译输出，不代替源提交后的JAR载荷核验。

仍需PM提交稳定源，再由原唯一执行者按卡运行一次正常全量build并保留计数、跳过原因、exit与制品身份；当前尚不能声称全量构建出口已恢复或最终JAR等价。生产无行为改动，本窄审不新增客户端手测门，也不关闭任何既有人工/多人观察事项。
