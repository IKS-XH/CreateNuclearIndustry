# 已验锅炉尺寸合入：独立组合窄审

2026-10-15。审查基线 main `83cd1b6d8284db3e1f91043b62b637f7c399933c`，已验尺寸来源 `30020e469ec8719560a33833081979c8f7225daf`。按现行 AGENTS、治理职责及验证政策、[合入卡](../../../archive/2026-10-15/plans/2026-10-15-boiler-size-accepted-integration.md)、原尺寸计划/实现/审查/候选和本批验收执行一次规格与质量组合窄审；只写本报告，没有修改来源、Git、配置或世界，没有运行测试、构建、游戏或派发代理。应用 minecraft-modding、minecraft-testing、minecraft-ci-release、requesting-code-review 及 verification-before-completion 技能；锁定 MC 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82 / Flywheel 1.0.6。

## 来源阶段结论

七项来源适配通过，无 Critical、Important 或有依据的 Minor 必改项。下方已完成本次审查内的最终来源及冻结制品补核；PM 的正式包复制与归档另行记录。

- `BoilerConfig` 仅改变默认最大值及元素合法上界为 16，并同步中文说明；`BoilerState` 仅改变 DEFAULT 最大值和 Settings 合法上界。当前两个流量参数及默认 2048/4096、long 乘积预检、双汽库存、共同保压、主动出汽与后续管理员/美术逻辑均未被旧整文件覆盖。
- `BoilerStateTest` 的四项尺寸合同按当前 Settings 签名接入，17 拒绝有效；水/汽/阀溢出分别在合法最大 16 下测试，避免先被尺寸非法挡住。16³ 分区的 1372/1176 格及 2744000/2352000 mB、原库存与 HU 保持断言成立。
- 原 `ExtensionBoilerGameTests` 仅三处改动，11 格偏心分区参考及其 162/486 断言保留，默认非法尺寸改为 17。新 `BoilerSizeLimitGameTests` 与空模板的 Git blob 分别为 `5cef4aefe2f7a0968ccd7a72bc17c8dd738b9769`、`6ed1a30e3c52a8ed3787936cdefa6989d07869bb`，实际与原 30020e4 完全一致。
- `build.gradle` 原延迟 Jar 配置只追加上述新真实测试类与尺寸模板两条排除，原排除项保持。

## 已核证据与保护范围

实际读取 `build/reports/boiler-size-accepted-integration/` 下 STAGE1、adaptation、原始测试日志/退出记录及三份 `junit-stage1` XML：BoilerStateTest 19、BoilerDualSteamInventoryTest 13、IndustrialPortRateTest 1，共 33/33，失败/错误/跳过均 0；唯一测试进程 exit0，日志 BUILD SUCCESSFUL。没有本批 GameTest 或 assemble 运行，不将已编译测试类称为新真实运行。原 29/29、最终真实 3/3 及用户本次尺寸/外观验收按原证据复用，不扩大到装配台动画或重跑全矩阵。

独立将当前文件与 before-manifest 比较，105 项原 dirty 已全部恢复一致（含 PM 恢复的两个测试自动日志）；59 项世界/运行配置只有本卡允许的锅炉配置变化。配置前后均 2366 字节，唯一 offset 109 从 49 改为 54，即 dimensionRange `[5,11]`→`[5,16]`；其余字节保持，当前 SHA256 `d2f9ac5134fccb91db51a8e341ef25630745a53e4807f92a3915ad648d026608`。原配置完整备份保留，配置不作为 Git 来源。此时正式包仍为原 `33da90021f938d2349e9d88c4522df72bfe587a4f1ca388d3158d34d9e8e9e8b`。

客户端尺寸/外观通过来自用户直接验收登记；审查者没有冒称亲自手测、客户端已加载新配置，亦未扩大其他任务的验收范围。

## 同次最终制品补核

准确来源提交为 `876346979190fb6f49c73e4070e36ef127af9d54`，tree `59db58a8e0d972b3417ab652d845db2f57bb38b2`。实际读取 export-manifest、assemble-result、assemble.log、artifact-summary 与 artifact-verification，并独立核 Git ls-tree、导出 ZIP、当前导出生产来源、构建输出及冻结 JAR。6617 项清单与该 Git 树逐一匹配；ZIP 每项 SHA256/原始 Git blob 均匹配，ZIP SHA256 为 `238d26d59d6da4ff12608b9c702ebfa7f787617223090f0713b7ec1354092506`。1970 项生产来源无漂移。导出工作区中已跟踪的旧 build/libs JAR 被本次 assemble 正常更新，不属于生产来源漂移；原件仍在来源 ZIP 和冻结旧包中。

唯一必要 assemble 的原始日志为 BUILD SUCCESSFUL in 19s，退出记录 exit0、实际 19.9850509 秒；compileJava 为 FROM-CACHE，不能误称重新全量编译或本轮完整 build。未补跑 JUnit/GameTest。冻结文件 `build/reports/boiler-size-accepted-integration/built-c12a7ded.jar` 实测 3304938 字节，SHA256 `c12a7ded24ef6541eef4564fc3412eeb9aa79eb19805e86b822df52202958c3c`，与干净导出工作区的构建 JAR 完全相同。

独立核全部 2278 个构建输出及 2261 个 JAR 文件项：691 个 class、1569 个资源、1 个 Manifest 全部逐项匹配；无额外、缺失或测试载荷泄漏。实际排除输出为 18 项，这是既有与新增规则共同匹配的文件数，不是规则条数；本卡仅追加两条排除，且新 Size 测试 class 和模板确已排除。相对原 33da 包无文件新增/删除，仅 `BoilerState.class`、`BoilerState$Settings.class`、`BoilerConfig.class`、`ExtensionBoilerGameTests.class` 四项字节改变；其余 **687 个 class**、全部 1569 个资源和 Manifest 原字节保持。summary 的 `unchanged_non_batch_classes=686` 是其允许集合外统计，不能作为全部未改变 class 的总数。

末次独立哈希复核：105 项原 dirty 全等，59 项世界/运行配置仍仅允许的锅炉 dimensionRange 单键变化。七来源当前主目录身份及 Git 换行适配绑定见逐项记录；未将原 dirty 混入来源包。

最终结论：本次来源适配及冻结制品组合窄审通过，无剩余必改项。用户尺寸/外观人工验收沿本批 ACCEPTANCE 保持，不替代其他任务人工门；本结论交 PM 完成正式包复制与文档归档，审查者至此停止写入。
