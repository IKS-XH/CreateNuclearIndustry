# BUILD-BASELINE-20261106 执行交付（源提交门）

- 工作区：`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix`；分支 `codex/build-baseline-20261106`；基准 HEAD `b8905473123d01c905daee4dc56198de66ad3052`。唯一执行者，无 Git 写操作。
- 实际读取：本树 AGENTS、治理5.1/6、STATUS、本卡；技能入口 `C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-ci-release}/SKILL.md`。实际用于确认平台/版本、JUnit定向与发布源门；不套用示例升级或clean。Java21.0.7、MC1.21.1、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6、Gradle9.0.0保持。

## 修改与根因

仅改2个测试源，见 `SOURCE.diff` 和 `SOURCE-MANIFEST.sha256`：

1. `HeatExchangerBoilerBridgeTest.java`：补缓存8项冷凝 ConfigValue，仍逐项先保存 cachedValue、结束用 ORIGINAL_VALUES 原样还原（含原null）并清表，不加载/保存配置文件。原13项断言/并发/真实pauseHeat未变。
   - RED两项均为 `Cannot get config value before config is loaded`。真实栈 `pauseHeat:349 → topologyChanged:126 → HeatExchangerLine.find:33 → HeatExchangerConfig.condensationSettings:64`；首读 CONDENSATION_RATE，原夹具未列任何8冷凝值。新增项完整覆盖该方法8个get；核热、冷凝生产算法未改。
2. `ReactorTickReference.java`：只把余数分配逐列筛选 `remaining > HEAT_EPSILON` 同步为 `remaining > 0D`，并加中文职责说明。生产850904df（2026-10-16已验微热修复）已有此规则，参考漏同步；没有复制生产类或替换独立裂变oracle。
   - 未改输入独立诊断 first-divergence.log：scenario0/tick4 控制、裂变、维护、coolant阶段全部相等；第一阶段thermal，第一字段 `thermal.snapshot.fuelColumns[(0,1)].quantizedHeatRemainderHu`。
   - reference `0.0` bits `0x0000000000000000`；production `1.4490234156865493E-13` bits `0x3d4464a8348e7b89`。该列真实remaining `1.4490234156865794E-13` bits `0x3d4464a8348e7c00`，被旧reference的1e-12逐列筛选漏掉。两侧量化总量同为 `0.2497484322059904` bits `0x3fcff7c1b246a800`；反射只读各自private分配函数，证明不是裂变求和顺序/输入/反馈缓存分歧。后续传播差异是该安全子集漏分配的后果。

`ReactorFissionDifferentialTest.java`、`ReactorFissionReference.java`保持原字节；严格相等、固定种子700布局/1400裂变比较、6×100完整tick、调棒/卡死/燃耗/维护场景完全保留。

## 自动证据

- RED（修改前）：`.\gradlew.bat test --tests '*HeatExchangerBoilerBridgeTest' --tests '*ReactorFissionDifferentialTest' --console=plain`，15项/3失败/0跳过，exit1，29秒。日志、完整2类XML、源备份、输入身份与classes清单在 `baseline/`。
- GREEN：`.\gradlew.bat test --tests '*HeatExchangerBoilerBridgeTest' --tests '*ReactorFissionDifferentialTest' --tests '*ReactorServerTickTest' --tests '*ReactorCoolantSimulationAdapterTest' --console=plain`，33项/0失败/0错误/0跳过，exit0，20秒；13+2+9+9。完整4类XML和日志在 `targeted/`；差分类system-out确认1400裂变与600完整tick比较。
- 诊断工具仅放本报告目录 `FirstDivergence.java`、`diagnose.init.gradle`，入口 `gradlew.bat -I build/reports/build-baseline-20261106/diagnose.init.gradle diagnoseBuildBaseline --console=plain`；未修改项目Gradle。第一次诊断的泛型编译错误和第二次Gradle9旧javaexec入口错误原始日志分别保留，修正工具后诊断exit0；这两次不是业务测试结果。
- `git diff --check`通过；生产源码/配置/构建/资源没有diff。定向测试 compileJava/processResources为UP-TO-DATE，前后846个现有main class文件的路径/SHA清单完全相同，见 baseline/ 与 targeted/ 的 production-classes.sha256；此数字是本树编译输出（含已有测试设施class），不冒充JAR载荷数。
- 未构建/替换正式JAR。现有JAR仍 SHA256 `E0ABFC7E0EED70AE2F4476CFFE7DBD7CE2CBD422D6919A13791F31B8CC6D15EF`。源提交后再按卡做一次正常build及实际JAR载荷核验，当前不声称最终制品等价。

## 交付边界

停在PM独立窄审/源提交门，未运行全量build、未改main、未读写用户世界、未启动客户端。仅测试初始化/参考修正，没有生产行为和兼容影响；本报告只证明自动定向验证。下一依赖是PM审查并提交稳定源后，通知同一执行者运行正常全量build；若出现新失败先报告，不扩改。
