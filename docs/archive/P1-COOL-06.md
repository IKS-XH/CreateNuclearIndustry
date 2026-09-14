# P1-COOL-06 执行报告

## 交付边界

本报告由执行者提交，不改变任务状态、不替项目经理验收。基准为 detached `HEAD`
`82d3f0fa0a92219e61374dec10ab2b06d22e645b`；执行期间未运行任何 Git 写操作，改动保持未提交。

实际技术栈为 Minecraft `1.21.1`、Java `21`、NeoForge `21.1.219`、Create `6.0.10-280`、
Ponder `1.0.82`、Flywheel `1.0.6`。已完整读取并应用：

- `C:/Users/lenovo/.codex/skills/minecraft-modding/SKILL.md`：按当前 NeoForge 版本核对
  `IFluidHandler`、服务端配置、方块实体同步和 Create 流体边界。
- `C:/Users/lenovo/.codex/skills/minecraft-testing/SKILL.md`：使用纯 JUnit、NeoForge
  GameTest、真实方块 capability 和 Create 管网分层验证。

## 基线复现

基线只读检查确认 `ReactorInstrumentPortBlockEntity.structureSummary()` 将冷、热容量分别
从两个独立配置读取为 `1000 mB`，而不是读取有效结构缓存。因此中心空列八燃料布局与三行
`F-C-F` 布局都沿同一生产路径得到固定冷/热各 `1000 mB`（总显示 `2000 mB`）；旧摘要
单元测试也在该固定值合同下通过。该复现是基于 `git show HEAD` 的生产代码证据和基线定向
测试完成的；没有在执行者工作区回退或重建基线世界。

## 实现结果

容量权威路径现在为：

```text
coolantSpaceBlockCount = Σ column.bodyPositions().size()
  where column.type() is EMPTY or CONTROL_ROD
coolantCapacityMb = coolantSpaceBlockCount * coolantCapacityPerEmptyBlockMb
```

- 新增 `ReactorCoolantCapacity` 纯派生模型，只读取最近一次有效 `ScanResult` 缓存。
  `FUEL`、外壳、端口、顶盖、控制棒驱动器和观察窗均不计入；固定上限为 24 个主体格。
- `P1ServerConfig` 移除冷/热双容量权威字段，新增
  `reactor.coolantCapacityPerEmptyBlockMb`，默认 `1000`，允许 `0`，上限为
  `Integer.MAX_VALUE / 24`。
- `ReactorInstrumentStructureSummary`、仪表端口 capability、服务端 tick、冷却剂账本、
  适配器和护目镜均使用同一单一共享容量。冷端 fill 按 `cold + hot` 计算剩余空间，两侧
  `getTankCapacity(0)` 相同，热端只排热态流体；增加端口只增加吞吐量。
- 冷转热为同体积状态变化，不再受独立热容量或热端剩余空间截断。已有超容量存量不丢失，
  新 fill 为零，转化与热端排出仍可进行，降至容量以内后输入自动恢复。
- 摘要新增 `emptyColumnCount`、`coolantSpaceBlockCount`、
  `coolantCapacityPerEmptyBlockMb`、`coolantCapacityMb`，只进入临时同步标签；未修改
  `ReactorSnapshot`、NBT 编解码或 NBT 版本。
- 中英文护目镜显示冷量、热量、合计/共享容量，超容量显示超额量。只修正了允许范围内的
  Ponder 语言键；`P1PonderScenes.java` 未修改。

## 精确改动文件

生产代码：

- `src/main/java/com/iksxh/create_nuclear_industry/config/P1ServerConfig.java`
- `src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorCoolantCapacity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorInstrumentStructureSummary.java`
- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorCoolantLedger.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorCoolantSimulationAdapter.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorCoolantFluidHandler.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorServerTick.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorInstrumentGoggleDisplay.java`
- `src/main/resources/assets/create_nuclear_industry/lang/en_us.json`
- `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`

直接受容量合同影响的 GameTest 与 JUnit 已按任务卡允许范围更新；新增
`P1CoolantCapacityGameTests.java` 和 `ReactorCoolantCapacityTest.java`。完整文件名可由最终
`git status --short` 对照，未修改 `AGENTS.md`、`docs/`、Gradle、注册、网络、Ponder Java、
模型或持久化代码。

## 测试与证据

### JUnit

命令：`./gradlew.bat test --rerun-tasks`

结果：`261 tests completed, 0 failures, 0 errors`，`BUILD SUCCESSFUL`。

关键用例与断言包括：

- `ReactorCoolantCapacityTest`：中心空列八燃料为 `1/3/3000 mB`，`F-C-F` 为
  `0/9/9000 mB`，全燃料为 `0 mB`，24 格上限、零容量、最大安全值和非法输入。
- `ReactorInstrumentStructureSummaryTest`：摘要派生、控制棒主体格统计、零容量和同步往返。
- `ReactorCoolantLedgerTest`、`ReactorCoolantSimulationAdapterTest`：全冷、全热、混合、
  恰满、超容量恢复、热端阻塞、热量守恒和 NBT 往返；新增全冷满载仍可转化的断言。
- `ReactorInstrumentGoggleDisplayTest`：冷/热/合计行及超容量文本；新增超容量 `1600/1500`
  与 `100 mB` 超额断言。
- `P1Thermal01ContractTest` 及现有冷却、生命周期、管网、服务端 tick 回归：保留整数 mB
  余数安全、端口配额和热量/库存守恒。

### NeoForge GameTest

命令：`./gradlew.bat runGameTestServer --rerun-tasks --max-workers=1`

最终结果：`111 GAME TESTS COMPLETE`，`All 111 required tests passed :)`。新测试
`P1CoolantCapacityGameTests.realLayoutsDeriveThreeAndNineThousandSharedCapacity` 使用真实
世界方块依次验证中心空列、`F-C-F`、全燃料零容量和冷热 capability 相同容量；
`P1CoolantGameTests.formalCoolantCapabilitiesConvertDrainAndReloadConservatively` 覆盖
SIMULATE 无副作用、逐端口配额、冷热转换、热端排出、阻塞库存和快照重载；现有
`P1CoolantCreateNetworkGameTests`、`P1Coolant05*`、`P1Loop*`、`P1Structure*` 与护目镜回归
一并通过。

GameTest 成功后原生 runner 停在 `Saving worlds`，与任务卡记录的环境行为一致；已在成功结果
打印后发送 Ctrl-C。未将该停滞误报为模组断言失败。最终运行未出现模组断言失败或
`GameTestInfo.tickInternal` fastutil 瞬态崩溃。

### 构建

命令：`./gradlew.bat build --rerun-tasks`

结果：`BUILD SUCCESSFUL`。仅有既有 NeoForge 弃用 API 警告和已有 GameTest unchecked 警告。

## 客户端、配置和管网人工验收

本执行者未进行交互式 `runClient` 创造世界观察，也未执行“真实配置改为 500、热重载/重启、
逐字节恢复并核对前后 SHA-256”的人工验收，因此以下项目仍交由项目经理或用户完成，不能以
自动 GameTest 代替：

1. 在专用创造世界成型中心空列八燃料和三行 `F-C-F`，护目镜核对 `3000/9000 mB`、空列、
   空气格、冷量、热量和合计占用。
2. 用真实 Create 储罐—动力泵—管道填满共享容量，确认额外输入停止，再运行到冷热混合且
   合计不超容量。
3. 临时修改
   `C:/Users/lenovo/.codex/worktrees/d948/CreateNuclearIndustry/run/config/create_nuclear_industry-server.toml`
   的 `coolantCapacityPerEmptyBlockMb` 为 `500`，重载或重启后核对两布局为 `1500/4500 mB`；
   恢复原内容并核对 SHA-256。执行结束时观察到的默认配置 SHA-256 为
   `CD9ECDB8F80E015CAAD1C8D42A1132C359F2226A96916D667A673210774C1E21`，本次未修改该文件。

## 差异与副作用

最终状态为 `HEAD (no branch)`，任务代码和测试改动未提交。Gradle/GameTest 运行改动了任务
写集外的 `logs/debug.log`、`logs/latest.log`；执行者按治理规则未删除或还原它们。
`git diff --check` 的非零结果仅来自这两个生成日志中的两行 trailing whitespace；代码改动
没有发现同类内容。日志属于项目经理后续整理范围。

执行者交付状态：已执行完成，等待项目经理复验和验收。

## 项目经理复验与归档补记

用户于 `2026-09-14` 在执行者隔离工作树的专用创造世界完成全部客户端与配置人工验收，确认：

- 中心空列八燃料布局显示并使用 `3` 个可计空气格和 `3000 mB` 共享容量；
- 三行 `F-C-F` 布局显示并使用 `9` 个可计空气格和 `9000 mB` 共享容量；
- 真实 Create 储罐—动力泵—流体管道可以填至共享容量，满容量后拒绝额外输入；
- 冷态与热态共同占用单一容量，运行到混合库存时合计不超容量；
- `coolantCapacityPerEmptyBlockMb=500` 时两种布局无需重搭即变为 `1500/4500 mB`，缩容造成的超容量存量不丢失，冷端停止输入、热端继续排出，回落到容量以内后输入恢复；
- 保存退出并重新进入后容量、库存和原位管网继续有效。

人工验收初次看到固定 `2000 mB` 的截图，经项目经理读取正在运行进程的
`-Dfml.modFolders` 和两个工作树的日志时间确认，实际加载的是主工作区旧构建；该界面还保留
“冷却剂总容量”和冷/热各 `1000 mB` 的旧语言键，不是本任务候选实现。用户从执行者工作树
完整重启客户端后完成上述通过结果。客户端日志显示正确工作树于 `21:01` 启动、`21:16`
正常停止。实际生效的 `run/config/create_nuclear_industry-server.toml` 已恢复
`coolantCapacityPerEmptyBlockMb=1000`，恢复后 SHA-256 为
`CD9ECDB8F80E015CAAD1C8D42A1132C359F2226A96916D667A673210774C1E21`，与执行者记录的基线一致。

项目经理独立复验结果：51 个测试结果文件共 `261/261` 项 JUnit 通过，失败、错误和跳过均为
0；NeoForge GameTest 一次取得 `111 GAME TESTS COMPLETE` 与
`All 111 required tests passed :)`，随后停在既有 `Saving worlds` 并由项目经理结束 runner；
`build --rerun-tasks --max-workers=1` 通过。只有既有 NeoForge 待删除 API 与 GameTest unchecked
警告，没有模组断言失败。生产和测试差异的 `git diff --check` 通过，测试产生的
`logs/debug.log`、`logs/latest.log` 已由项目经理恢复到基线，未纳入提交。

执行者遵守角色和 Git 禁令，实际使用 `minecraft-modding` 与 `minecraft-testing`；改动保持在
任务卡允许写集，没有修改持久化 `ReactorSnapshot`、NBT 版本、网络协议、Gradle、Ponder Java、
模拟器或事故范围。实现由项目经理提交为 `ba5d7b2`（`实现布局派生共享冷却剂容量`）并快进
合入 `main`。本任务最终状态为 **已完成**，`P1-VERIFY-02` 的阻塞解除。
