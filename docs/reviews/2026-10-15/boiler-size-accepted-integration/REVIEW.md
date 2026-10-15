# 已验锅炉尺寸合入：独立组合窄审

2026-10-15。审查基线 main `83cd1b6d8284db3e1f91043b62b637f7c399933c`，已验尺寸来源 `30020e469ec8719560a33833081979c8f7225daf`。按现行 AGENTS、治理职责及验证政策、[合入卡](../../../superpowers/plans/2026-10-15-boiler-size-accepted-integration.md)、原尺寸计划/实现/审查/候选和本批验收执行一次规格与质量组合窄审；只写本报告，没有修改来源、Git、配置或世界，没有运行测试、构建、游戏或派发代理。应用 minecraft-modding、minecraft-testing、minecraft-ci-release、requesting-code-review 及 verification-before-completion 技能；锁定 MC 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82 / Flywheel 1.0.6。

## 来源阶段结论

七项来源适配通过，无 Critical、Important 或有依据的 Minor 必改项。最终来源提交与正式制品尚待本次审查内补核；本节不宣称正式包已完成。

- `BoilerConfig` 仅改变默认最大值及元素合法上界为 16，并同步中文说明；`BoilerState` 仅改变 DEFAULT 最大值和 Settings 合法上界。当前两个流量参数及默认 2048/4096、long 乘积预检、双汽库存、共同保压、主动出汽与后续管理员/美术逻辑均未被旧整文件覆盖。
- `BoilerStateTest` 的四项尺寸合同按当前 Settings 签名接入，17 拒绝有效；水/汽/阀溢出分别在合法最大 16 下测试，避免先被尺寸非法挡住。16³ 分区的 1372/1176 格及 2744000/2352000 mB、原库存与 HU 保持断言成立。
- 原 `ExtensionBoilerGameTests` 仅三处改动，11 格偏心分区参考及其 162/486 断言保留，默认非法尺寸改为 17。新 `BoilerSizeLimitGameTests` 与空模板的 Git blob 分别为 `5cef4aefe2f7a0968ccd7a72bc17c8dd738b9769`、`6ed1a30e3c52a8ed3787936cdefa6989d07869bb`，实际与原 30020e4 完全一致。
- `build.gradle` 原延迟 Jar 配置只追加上述新真实测试类与尺寸模板两条排除，原排除项保持。

## 已核证据与保护范围

实际读取 `build/reports/boiler-size-accepted-integration/` 下 STAGE1、adaptation、原始测试日志/退出记录及三份 `junit-stage1` XML：BoilerStateTest 19、BoilerDualSteamInventoryTest 13、IndustrialPortRateTest 1，共 33/33，失败/错误/跳过均 0；唯一测试进程 exit0，日志 BUILD SUCCESSFUL。没有本批 GameTest 或 assemble 运行，不将已编译测试类称为新真实运行。原 29/29、最终真实 3/3 及用户本次尺寸/外观验收按原证据复用，不扩大到装配台动画或重跑全矩阵。

独立将当前文件与 before-manifest 比较，105 项原 dirty 已全部恢复一致（含 PM 恢复的两个测试自动日志）；59 项世界/运行配置只有本卡允许的锅炉配置变化。配置前后均 2366 字节，唯一 offset 109 从 49 改为 54，即 dimensionRange `[5,11]`→`[5,16]`；其余字节保持，当前 SHA256 `d2f9ac5134fccb91db51a8e341ef25630745a53e4807f92a3915ad648d026608`。原配置完整备份保留，配置不作为 Git 来源。此时正式包仍为原 `33da90021f938d2349e9d88c4522df72bfe587a4f1ca388d3158d34d9e8e9e8b`。

客户端尺寸/外观通过来自用户直接验收登记；审查者没有冒称亲自手测、客户端已加载新配置，亦未扩大其他任务的验收范围。

## 同次最终制品补核

待 PM 准确来源提交及执行者干净 Git 导出、唯一必要 assemble、完整来源/载荷冻结后填写；不要求重复上述测试。
