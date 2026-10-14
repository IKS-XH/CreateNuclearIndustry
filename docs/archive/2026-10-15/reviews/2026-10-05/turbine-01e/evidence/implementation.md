# EXT-B-TURBINE-01E 执行报告

最终范围按用户最新要求收束：首个可发布版本前不研究旧存档兼容。本报告下方旧双源 NBT 试验保留为已发生的历史运行记录，相关新增测试类与旧格式迁移实现均已撤回；最终代码只处理当前格式机组自身的保存恢复。

- 基线：`9159155`，工作树 `codex/ore-acquisition`；角色为实现执行者，未执行任何 Git 写操作，未改 `docs/`、构建脚本、用户 `run/config/world`。
- 实际技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/systematic-debugging/SKILL.md`。仓库目标仍为 MC 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280。

## 行为与根因

旧 `TurbineOutputShaftBlockEntity` 分别从 `TurbineState.frontSu/rearSu` 领取份额，`TurbineShaftBlock.Output` 仅有向外的实体轴连接；因此单端只能取得旧半额。Create 6.0.10 本地源码 `RotationPropagator.getPotentialNeighbourLocations/getRotationSpeedModifier` 调用实体的 `addPropagationLocations/propagateRotationTo`，`KineticNetwork.calculateCapacity` 对同网每个生成源按生成 RPM 累加容量，`GeneratingKineticBlockEntity.updateGeneratedRotation/applyNewSpeed` 负责原生接入、停机及网络迁移。若两个端口各发布总额，会造成双重容量。

现在前轴是唯一总 SU 发布源，后轴通过双向、倍率为 1 的内部 Create 传播与它共享同一网络。连接每次由服务端完整结构及相关区块的有效性决定；端轴记录上次有效远端，断开时使用 Create 原生 detach/attach 与 Source 清理。停汽或红石只撤销本机生成 SU，不取消完整结构的轴连接，外部同速源继续驱动。外部异速、反向及回接仍交给 Create 原生冲突规则。总容量、256 RPM、40 tick 平滑、耗汽、三档结构和外观未调整。

保留 SERVER 配置 `frontShare` 及旧 Settings 字段供旧 TOML 兼容，但它不参与任何 SU 分配或计算；TOML 注释标为废弃。护目镜仅显示一份两端共用的总 SU；中英语言资源与获 PM 额外授权的 `tools/art-assets/turbine_data.py` 对应提示同步。

## 修改路径

- `src/main/java/com/iksxh/create_nuclear_industry/turbine/`：`TurbineShaftPowerSource.java`、`TurbineOutputShaftBlockEntity.java`、`TurbineControllerBlockEntity.java`、`TurbineShaftBlock.java`、`TurbineState.java`。
- `src/main/java/com/iksxh/create_nuclear_industry/config/TurbineConfig.java`。
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionTurbineGameTests.java`、`gametest/turbine/TurbineKineticProbeGameTests.java`、`TurbineProbeOwnerBlockEntity.java`、`TurbineProbeShaftBlockEntity.java`。
- `src/test/java/com/iksxh/create_nuclear_industry/turbine/TurbineStateTest.java`。
- `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`、`en_us.json` 的汽轮机 SU 提示；`tools/art-assets/turbine_data.py` 同一提示。

## 实际验证

所有命令在本工作树执行，均未使用 `clean`、`--rerun-tasks` 或用户游戏目录。`-PgameTestDirectory` 在现有 `build.gradle` 的 `gameTestServer` run 配置中映射为独立 `gameDirectory`。

| 命令 | 结果 |
| --- | --- |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine_probe -PgameTestDirectory=build/runtime-01e-red --console=plain` | 修改测试预期但未改实现时，3 项中 1 项按新需求失败：`splitjoinandsplitcapacity` 报“前端未取得整机容量”；退出码 1。 |
| `.\gradlew.bat test --tests com.iksxh.create_nuclear_industry.turbine.TurbineStateTest --console=plain` | 9/9 JUnit 通过，退出码 0；含旧 `frontShare` 改值不改变容量。 |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine_probe -PgameTestDirectory=build/runtime-01e-probe --console=plain` | 初始 3/3 GameTest 通过，退出码 0。 |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine -PgameTestDirectory=build/runtime-01e-machine --console=plain` | 正式机组 10/10 GameTest 通过，退出码 0；新增真机组两端单独带超过旧半额的 Create 风扇，合计超限共同过载，减载共同恢复；三档、红石和库存现有用例亦通过。 |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine_probe -PgameTestDirectory=build/runtime-01e-probe-reverse-far --console=plain` | 最终探针 4/4 GameTest 通过，退出码 0；新增反向跨区块场景验证前轴源所在 chunk 真正卸载、后轴及外源仍加载时只保留外源容量，重载后恰好恢复。 |
| `.\gradlew.bat assemble --console=plain` | 增量打包成功，退出码 0。 |

反向跨区块测试工装曾三次失败，均已保留对应隔离运行目录：`runtime-01e-probe-reverse` 中两个测试的强制区块范围重叠，退出码 2；`runtime-01e-probe-reverse-isolated` 初始外源方向与前轴冲突，Create 原生销毁轴，诊断字符串进一步触发空实体异常，退出码 1；`runtime-01e-probe-reverse-speed` 将外源方向调一致后，近侧强制区块让前轴保持加载，无法满足真实卸载前提，退出码 1。最终将外源置于远侧并隔离场景坐标后，通过了要求的真实卸载断言。没有针对这些工装失败调整生产规则。

受影响写集的 `git diff --check` 无空白错误；完整工作树中的 `logs/debug.log`、`logs/latest.log` 是既有保留文件，JUnit 写入日志后其中有尾空白，因此全局 `git diff --check` 报这两份日志，未清理或覆盖。`docs/` 的同时变更由 PM 负责，不属于本交付。

PM 独立审查后，定点修正控制器类注释及 Create 网络探针类注释中的旧“双轴份额”措辞，明确前轴唯一总 SU 源和两端贯通共享。仅注释与本报告变化，未改运行逻辑；依治理 5.1 复用上述验证，不重跑测试或构建。

## 历史审查记录：旧两源 Network NBT 试验（已撤回）

审查曾指出旧 `9159155` 的两个输出轴会各自保存 `Network.Capacity/AddedCapacity`，而新账本加载后先清除 40 tick 平滑历史。曾新增、现已撤回 `src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionTurbineLegacyGameTests.java`，使用正式机组的真实前/后输出轴及外接 Create Creative Motor。用例从真实实体取得 NBT，再合成旧版前、后轴各 `16384 SU` 的 `Network` 快照：前轴旧网络另含电机容量及其 `AddedCapacity`，后轴旧网络单独持有半额；依次换入前轴、后轴、电机新实体，由服务器下一 tick 调用 Create 自身的 `initialize`。这只是历史合成试验，未打开实际 `9159155` 世界文件或执行完整区块磁盘卸载，不属于最终交付范围。

**原无 Source 用例修前未失败；有 Source 的真实前置使风险复现。** 原用例先换前轴再换电机，后者替换时 Create 会清除前轴的旧 Source，因而它只证明无 Source 路径。新增同一测试类的 `oldFrontSourceToMotorDoesNotRetainCapacity` 用 `NbtUtils.writeBlockPos` 在前轴 NBT 写入 `Source→电机`，先恢复电机、再恢复前后轴，并在首次服务器 tick 前断言前轴实体的 Source 确实仍指向电机。旧网络中电机和前轴共用 `Network.Id`，电机容量为 4,194,304 SU，前轴旧半额为 16,384 SU，后轴旧半额在独立网络。修前有效前置的首 tick 实际为 4,210,688 SU，超出外源恰好 16,384 SU；因此此路径是已复现缺陷，不把先前未确认 Source 的 12/12 当作通过证据。

试验阶段曾在 `TurbineShaftPowerSource.initialize` 实现旧快照核销，用户收缩范围后已撤回该旧格式迁移层；其历史通过结果不代表最终代码承诺旧世界兼容。`TurbineControllerBlockEntity.totalSu` 注释说明“红石立即清零，断汽依 40 tick 历史衰减”；探针注释改为“本机生成容量”。

修复后两种旧 NBT 变体均在首次 tick 限定容量 ≤ 电机自身容量 + 2 SU，稳定后两轴和电机同网且容量等于外源 4,194,304 SU（误差 < 2 SU）；重新供汽后容量等于外源加机主账本当前总 SU（误差 < 2 SU）。当前格式二次保存、账本历史归零并恢复实体后重复首 tick、稳定和供汽断言，未残留旧容量，也未抹掉外部电机。

| 追加命令 | 结果 |
| --- | --- |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine_legacy -PgameTestDirectory=build/runtime-01e-legacy-red --console=plain` | 初版测试工装电机构造器参数不符，编译失败，退出码 1；未触发用例断言。 |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine_legacy -PgameTestDirectory=build/runtime-01e-legacy-red-compiled --console=plain` | NeoForge 为模板自动添加测试 namespace，手写双 namespace 造成模板 ResourceLocation 非法并令测试服崩溃；未触发断言。已改用现有汽轮机测试域与模板，退出码 1。 |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine -PgameTestDirectory=build/runtime-01e-legacy-red-turbine --console=plain` | 首次/二次稳定恢复容量均通过；正式域 11/11，退出码 0。 |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine -PgameTestDirectory=build/runtime-01e-legacy-double --console=plain` | 新增两端与外源同网及第二次恢复断言，11/11，退出码 0。 |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine -PgameTestDirectory=build/runtime-01e-legacy-first-tick --console=plain` | 再增首次和二次初始化首 tick 容量上界断言，最终 11/11，退出码 0。 |
| `.\gradlew.bat assemble --console=plain` | 新增测试和注释后增量打包，退出码 0，`compileJava` 为 UP-TO-DATE。 |

### R1 Source 图追补命令

| 命令 | 结果 |
| --- | --- |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine -PgameTestDirectory=build/runtime-01e-legacy-source-red --console=plain` | 未断言 Source 是否保留时 12/12，退出码 0；后续证实该工装换电机时清掉了 Source，不能作为本路径通过证据。 |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine -PgameTestDirectory=build/runtime-01e-legacy-source-assert --console=plain` | 增加实体 Source 前置断言后，新用例在工装前置失败，11/12，退出码 1；未到容量断言。 |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine -PgameTestDirectory=build/runtime-01e-legacy-source-retained --console=plain` | 先恢复电机使 Source 前置成立；修前首 tick 两个用例均观察到 4,210,688 SU 对 4,194,304 SU，10/12，退出码 1（测试服进程退出值 2）。原无 Source 用例因暂时统一改了重建顺序而同样失败，随后恢复原顺序。 |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine -PgameTestDirectory=build/runtime-01e-legacy-source-fixed --console=plain` | 原生初始化核销初版后，含有效 Source 前置与原无 Source 用例，12/12，退出码 0。 |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine_probe -PgameTestDirectory=build/runtime-01e-probe-legacy-fix --console=plain` | 初始化改动后的动力探针 4/4，退出码 0。 |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine -PgameTestDirectory=build/runtime-01e-legacy-source-final --console=plain` | 限定服务端并按本机生成 RPM 核销后的正式机组 12/12，退出码 0。 |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine_probe -PgameTestDirectory=build/runtime-01e-probe-legacy-final --console=plain` | 同一最终代码下动力探针 4/4，退出码 0。 |

## 出口与限制

自动化覆盖 Create 真网络容量、共同过载、停机外源、断开重建与两个方向的实际区块卸载重载。未启动客户端或用户世界；01D/R1 外观门仍需既定人工复测。本次共享容量的客户端观察按任务卡人工出口由 PM 安排，不把 GameTest 当作客户端视觉验收。

## 最终收束：当前格式保存恢复

移除旧双源合成测试及对应历史格式核销后，在 `ExtensionTurbineGameTests.currentFormatReloadKeepsOnlyLiveCapacity` 使用现行正式机组实际供汽 40 mB、原样保存前后轴和 Create 电机 NBT，清空控制器运行历史并重建实体。修前首 tick 网络为 4,227,072 SU，外部电机为 4,194,304 SU，恰多当前前轴真实保存的 32,768 SU。这个缺陷属于当前格式自身保存恢复，故保留必要修复。

最终 `TurbineShaftPowerSource` 在计算本轴实际容量时记录同一时点的本机生成 RPM，并与 Create 的 `Network.AddedCapacity` 同存 `Network.TurbineGeneratedRpm`。服务器恢复当前格式且账本已无生成容量时，`initialize` 暂用该 RPM 让 Create 原生 `addSilently` 核销本轴旧额度，随后清除临时零容量源条目；未携带此字段的旧 NBT 不走该分支。外部电机的容量和成员保持，旧版双源迁移不在本任务范围。测试明确断言保存的 `AddedCapacity>0` 且生成 RPM 为配置值、恢复首 tick 容量不超过外源、稳定后两端同网且容量只等于外源。

| 最终范围命令 | 结果 |
| --- | --- |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine -PgameTestDirectory=build/runtime-01e-current-reload-red --console=plain` | 当前格式新用例修前失败，正式域 10/11，退出码 1；首 tick 多 32,768 SU。 |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine -PgameTestDirectory=build/runtime-01e-current-reload-fixed --console=plain` | 修复后正式域 11/11，退出码 0；共享容量、共同过载和当前格式保存恢复均通过。 |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine_probe -PgameTestDirectory=build/runtime-01e-probe-current-final --console=plain` | 两端贯通、停机外源、正反向真实区块卸载探针 4/4，退出码 0。 |
| `.\gradlew.bat assemble --console=plain` | 最终增量打包成功，退出码 0，`compileJava` 为 UP-TO-DATE。 |
