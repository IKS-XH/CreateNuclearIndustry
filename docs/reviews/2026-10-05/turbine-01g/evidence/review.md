# EXT-B-TURBINE-01G 联合规格/质量审查

审查对象：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，HEAD `56dd5efdae0633f3c49d00e1f198c5af021f14a2`；仅审查任务指定的四个 `src/main/java/com/iksxh/create_nuclear_industry/` 文件差异。未审查并未改动任务卡、用户日志或其他工作树变更。未运行测试或构建，复用了本次冻结实现的原始证据。

## 结论

**没有发现需要整改的规格或代码问题；源码可进入用户指定的现场复测门。** 此结论不表示人工验收或客户端同步已通过。

## 优点

- `turbine/TurbineShaftPowerSource.java:124-135` 只在“之前有本机 SU、当前 SU 为 0、旧 RPM 有效、无上游 source、网络仍存在、父类已把实际速度清零”这一相位恢复旧生成 RPM，再走 Create 原有的速度归零与网络拆除路径。此守卫对应报告确认的 0→0 漏拆源，不强制清零整个网络。
- 该分支按 `hasSource()` 排除由外部动力源驱动的轴；探针测试同时验证同速 Creative Motor 的 SU 与 128 RPM 保留，生产蒸汽供给恢复后也重新建立唯一本机容量。
- 差异没有改变 01F 的流量窗口、倍率、阈值、配置、结构或共享容量规则。新增/调整的手写说明为中文，描述与调用顺序一致。
- 新断言使用真实 Create 动力网。修前固定校验相位的失败日志记录本机 SU=0、轴 RPM=0 但源网络仍存在；修后日志记录 17/17 GameTest 通过，增量 `assemble` 退出码为 0。

## 问题

### Critical（必须修复）

无。

### Important（应修复）

无。

### Minor（可改进）

无。

## 证据边界与未评估项

- `gametest/turbine/TurbineKineticProbeGameTests.java:70-97` 与 `TurbineProbeShaftBlockEntity.java:38-43` 的计数器差值证明受控探针路径中下游 `sendData()` 被调用，且包装仍委托 `super.sendData()`。这不是客户端已收到数据包、速度表已刷新或动画已停止的证据；应按任务卡保留现场客户端复测。
- `ExtensionTurbineGameTests.java` 的真实汽轮机用例还覆盖无外源断汽后两端和外接普通轴归零，以及再次供汽后恢复。校验计数通过测试夹具对齐相位，属于确定性生命周期回归，不等同于自然相位客户端手测。
- 未审全仓库其他差异、旧版本存档兼容、全项目测试、其他设备或冷凝联调；它们不属于本次合同范围。

## 审查依据

- Create 锁定源码 `create-1.21.1-6.0.10-280-sources.jar`：`KineticBlockEntity.tick/validateKinetics` 先运行并可直接把 `speed` 写为 0；随后汽轮机账本更新调用 `GeneratingKineticBlockEntity.updateGeneratedRotation`。以恢复的旧速度进入 `applyNewSpeed(old, 0)` 后，原生移除路径可调用 `RotationPropagator`，并由其下游清理发送数据。
- 修前失败：`build/reports/extension/EXT-B-TURBINE-01G/race-before-fix.log`，退出码文件为 1。
- 修后通过：`build/reports/extension/EXT-B-TURBINE-01G/power-network-final.log` 显示 17/17 通过，`power-network-final.exit-code.txt` 为 0；`assemble.log` 与 `assemble.exit-code.txt` 为成功/0。
- 已按任务要求读取 AGENTS、01G 与 01F 合同，以及 `minecraft-modding`、`minecraft-testing`、`requesting-code-review/SKILL.md` 和其 `code-reviewer.md` 模板；复核了本地锁定 Create 源码，没有查看新版依赖。

**交付评估：** 代码层面无阻断项，可提交用户现场复测：稳定供汽后撤除输入，待周转量及 40tick 窗口衰减，确认无外源时两端停止；重新供汽确认恢复。客户端显示/动画和现场行为仍待用户操作确认。
