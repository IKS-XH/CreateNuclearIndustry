# EXT-B-BOILER-01D 独立规格与质量审查

## 审查范围

- 任务：`EXT-B-BOILER-01D`；审查基线：`a95d497b284540516411052eeca98be66d6475f4`。
- 候选：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。
- 只读检查：`BoilerStructure.java`、`BoilerControllerBlockEntity.java`、`ExtensionBoilerGameTests.java`；交付说明及 `build/reports/extension/EXT-B-BOILER-01D/` 原始构建日志。
- 除下文误启动外，以只读复核实现者证据为准；未执行 Git 写操作、未改功能代码或项目经理文档。

## 技能与版本

实际读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 与 `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`，据此核对结构坐标、NeoForge capability 生命周期和 GameTest 断言；同时按治理协议第 5.1 节依据改动范围复核实现者最终运行证据。候选 `gradle.properties` 与原始日志确认 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6，未见技术栈升级。

## 规格核查

- `inspect` 与 `issue` 将侧面三格定义为非棱边面板：给水只在第2层，蒸汽只在第4层；外法线由 ±2 外表面坐标决定，切线偏移不参与朝向。因此南北面非中央偏移口不会再按 `x` 正负误判。中央控制器仍占其面给水槽，控制器位置未移动。
- 上限计算与实现一致：水口 `4×3−1=11`，汽口 `4×3=12`。`owner` 从面内偏移最多一格枚举候选控制器，并且只接受候选有效结构中列出的端口；枚举前用 `hasChunkAt`，没有主动强制加载区块。
- 失效时能力 epoch 递增并清理 owner 缓存；当前有效端口、旧句柄以及给水/蒸汽两层每侧三个槽位均进入能力失效路径。管面刷新覆盖两层、四面和每面三个偏移位置，使用指向结构的反向面更新 Create 管状态并传播变化。
- 新增完整三格结构 GameTest 覆盖11水/12汽、四向法线、南面非中央冷缓存 owner 回查、逐口256mB额度、错层诊断、错朝向诊断和旧句柄失效。真实 Create 给水管网先记录偏移候选无能力的结构基线，再铺管、加偏移口并断言两个分支均出现流、泵实际运转和源罐/共享账本守恒；汽泵从南侧偏移口送汽至原生罐。共享账本与压力算法未改。

静态审查未发现南北面坐标、owner 归属、候选能力刷新或旧句柄 epoch 的实现硬错误。

## 补强复审

针对首轮审查列出的两项 P2 测试覆盖缺口，实现者对现有场景作了最小补强，并由我只读复核：

- `steamPipePlacedBeforeFormationReconnectsAfterShellRestore` 的东侧汽口、预铺管线和储罐整体沿切线 z+1，形成真实面内偏移口管路。t34 拆口后先记录储罐新基线，检查 Create 接口已消失或零压、没有活动流、能力关闭且拆前旧句柄失效；t47 重接后要求接口恢复且储罐量严格超过拆口后基线。之后仍断言拆壳压力释放、重搭后两条管线分别增量出汽，并以原始8000mB加明确补入2000mB核算守恒。
- `allSideSlotsFormWithOffsetOwnershipAndIndependentQuotas` 在错层口恢复后于面板棱边放入给水口，直接断言 `inspect` 拒绝、`issue.reason=edge` 且诊断坐标指向该棱边端口。

这两项补强满足对应任务卡测试要求；首轮 P2 缺口关闭。没有发现需要继续整改的代码或测试问题。本报告不代替项目经理验收。

## 构建证据与人工边界

- 实现者最终 `gametest-final.log` 与 `gametest-final-reviewed.log` 记录锅炉 namespace 18 个 required GameTest 全通过、服务端所有维度保存完成及 `BUILD SUCCESSFUL in 18s`；`assemble-reviewed.log` 记录 assemble 成功。已只读检查最终日志。
- 审查过程中我曾误启动一次同 namespace GameTest；收到项目经理停止运行的指示后立即发出中断。该进程当时的日志截在 `Saving players`，没有作为通过证据；随后实现者以唯一运行权重新执行并将同名 reviewed 日志更新为完整成功日志。本审查未再启动 Gradle，也未运行 JUnit。
- 锅炉账本与逐口额度算法未改，复用 01C 的 12/12 JUnit 证据，不重复运行。
- 自动化日志不能替代用户客户端体验；仍需按任务卡复测同面三口以及南北非中央 Create 管线。
