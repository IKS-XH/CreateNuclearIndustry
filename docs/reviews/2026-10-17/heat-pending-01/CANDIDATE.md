# HEAT-PENDING-01 服务器观察候选

2026-10-17。当前进度及未闭门只见[STATUS](../../../STATUS.md)，三次共同根因见[诊断](DIAGNOSIS.md)。

## 来源与文件

- 来源分支`codex/heat-bridge-pending-fix`，生产/测试提交`8712f4310eb1ff6ac8f2af03fde33e4154a71325`；只含桥接队列与既有回归测试两文件。
- 工作树`E:/MyMC/NewMod/Create_NuclearIndustry-render-accepted-integration`。
- 供测试的文件：`E:/MyMC/NewMod/Create_NuclearIndustry-render-accepted-integration/build/reports/heat-pending-20261017/create_nuclear_industry-0.1.0-heat-pending-01.jar`。
- 3336867 bytes；SHA256 `43E399BEBE8C895ECD290A8721CEAB73AF057F5D2413B39339CDD85F0204E54C`。
- baseline为已提交正式包B366EB49…；仅替换本次assemble的桥接class，其他2277文件载荷保持。详见[制品身份](ARTIFACT.json)、[源码冻结](SOURCE.json)。不是候选树build/libs中的原始CRLF包。

[实现证据](IMPLEMENTATION.md)包含真实旧路径RED、34/34定向GREEN与一次assemble/exit0；[独立组合窄审](REVIEW.md)无发现。源码/相关配置未变，按治理5.1复用这些证据，没有提交后重复全量测试。main生产与正式包未提升；主目录现有正式JAR删除改动原样保留。

## 集中观察

在保留世界与原模组包后，停服替换原本模组JAR，不在mods内同时保留两个本模组版本；本批未自动安装、重启或操作用户生产服。使用原整合包环境，由多人加载/离开区块，同时让Create储罐锅炉与核换热器正常运行，观察原CME是否再出现及供热刷新是否正常。没有要求拆除机组或改写存档。

这次不新增注册ID、协议、配置或热工玩法。单元夹具没有覆盖原整合包实际所有线程/模组组合；若再崩溃，应按新日志区分是否仍为同一业务栈，不能把其他类型异常归为本次已修问题。用户反馈后再记录现场验收及合入，当前不自动推进其他主线。
