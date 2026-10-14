# REACTOR-ADMINISTRATOR-05 启动后界面常驻崩溃诊断与窄修

当前结果：只读诊断、唯一短工具烟测与实际六分钟355样本观察完成，用户确认正常运行、没有再崩溃。原故障仍未确定具体源码根因，暂不实施或重新打包。实际身份与未验边界见[观察登记](../reviews/2026-10-14/reactor-administrator-05/OBSERVATION.md)，后文为当时的分阶段授权，不代表已批准猜测性代码修补。

2026-10-14。用户报告在管理员界面点击启动，过一会游戏崩溃，并确认管理员界面一直打开。按既有自动窄修授权处理，不改变已批准玩法；原04与03/01/01R人工门仍未验，main管理员功能合入和02 PI均暂停。

## 已有事实与边界

- 当前候选为`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-automation`、`codex/reactor-automation-basic`，HEAD `410807f`，功能源/JAR提交`d217196`，正式3199564B／dc82b20a19f17e81db2fd4e9efa69ed2b861fd703deaca4553ac7dfcbd6a7b05包。
- 候选`run/logs/latest.log`与debug.log：11:48:11开始真实runClient，11:49:21加入“新的世界”；11:51:39 PID90548的NVIDIA驱动报告`GL_OUT_OF_MEMORY`／无法分配buffer data，随后JavaLauncher中的runClient wrapper exit1。没有找到本轮Minecraft crash-reports或hs_err；不能编造Java堆栈，也不能仅凭显存错误就把管理员启动逻辑或Sodium/Iris定为根因。
- 用户确认点击启动后管理员界面一直打开；这是实际操作条件，不替代可靠复现。
- 版本保持MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。本次运行没有Sodium/Iris，不扩兼容专项。
- 当前事故后的用户世界、日志和配置原位保护；不能沿04开工前的38文件哈希覆盖用户已测试进度。不启动/改写用户世界、不改配置/驱动、不终止用户进程、不新树/分支或搬档。

## 唯一执行与首阶段写集

复用原唯一执行者`/root/reactor_automation_retrofit_impl`；最终若确有代码窄修，仍交原审查者`/root/reactor_automation_review`一次本故障组合窄审及必要同次复核。执行者不得Git、治理写或转派。

首阶段**只读诊断**，允许写：

- `build/reports/reactor-administrator-05/**`：保留当前事故日志、原制品和本次用户数据基线，准确记录采样时刻/范围。
- `docs/reviews/2026-10-14/reactor-administrator-05/DIAGNOSIS.md`。

只读实际管理员Screen/Menu/Projection/客户端更新、反应堆BER/动画材质与缓冲生命周期及相关锁定MC/Create/Flywheel API；先追查buffer/texture对象的创建、复用和释放，以及屏幕一直打开时后台世界渲染/暂停/网络投影顺序。可只读查看当前日志和公开本机进程/显存状态，不把崩溃后空闲显存当事故现场峰值。

首阶段不写功能/测试/构建脚本、不跑build/GameTest/runClient或启动用户世界。报告必须区分直接事实、已证实调用链与待验证假设，给最小必要修复路径和能够证明同一机制的窄验证；未找到代码根因不得用关闭动画、提高内存、降渲染距离或驱动升级掩盖。

## 后续裁定

PM依据准确报告将实际必要生产/测试路径与必要验证精确扩入本卡后，原执行者才实施。功能修复前取得对应行为RED或可靠资源生命周期错误证据；只修根因，不改热工、模式、菜单权威/安全保护、保存续接或资源美术含义。原定向通过不机械重跑。

稳定窄修后进行一次原审查者组合审查，必要同次差异复核；原执行者唯一必要增量assemble，原dc82候选保留，核源/编译/资源/JAR一致和测试排除。PM净提交精确源、正式候选JAR及交付文档；main只同步文档，不合管理员功能，不推进02。

若无法安全可靠复现或代码归因未成立，保留只读证据并向用户请求最少的复现信息或有界观察；不要冒称解决，也不增无关测试或环境改造。

## 人工门

本批必须复看同一候选里“管理员界面点击启动并持续打开”的实际场景，足够覆盖原约两分钟窗口；关闭/重新打开、停机与保存重进只在确受影响时复看。GPU真实观察不由纯JUnit/静态合同替代；原04及03未验项目仍承接。

## 只读采样补充授权

用户确认当前仅发生一次。已有静态调用链暂未给出具体代码根因，停止扩大静态渲染审计；先保存DIAGNOSIS，再接续唯一执行者制作有界观察工具，补足事故显存峰值缺失。

唯一新增工具为`build/reports/reactor-administrator-05/observe-client-memory.ps1`，交付使用说明只写本批DIAGNOSIS。默认6分钟、每秒一次；接受1～360秒的短烟测时长，不无限运行。记录UTC/本地时间、NVIDIA全卡显存总量/已用/空闲、当前候选forgeclientdev PID/工作集/私有内存/退出或未找到标记；可读取Windows公开GPU进程计数器时记录其实际口径，缺项明确不可用，不造零值。全卡占用与游戏进程占用不可混为一谈，不把事故后采样当事故峰值。

只写本报告目录内独立时间戳CSV/JSON及短烟测结果；不得导出完整命令行、环境变量、账户或认证信息。脚本不启动/停止游戏、不读写用户世界/配置、不改GPU/驱动/画质、不发送任何游戏指令。用户自己运行原runClient、操作START；如后台启动采样辅助进程，必须使用`Start-Process -WindowStyle Hidden`。结束自动给结果路径与采样缺项，不能因一次正常曲线宣称崩溃已修复。

采样可同时读取Win32_OperatingSystem公开的TotalVisibleMemorySize/FreePhysicalMemory/TotalVirtualMemorySize/FreeVirtualMemory，明确单位KiB；这是区分系统提交余量与显存耗尽所需的只读观测，不包含账户/环境。事故后当前余量不能反推事故时刻。

仅允许一次3～5秒只读工具烟测，验证当前采样字段、CSV和自动结束；不跑Minecraft/Gradle/Java测试或构建。工具交付后等待实际同条件复测及采样，不先写修复代码或重新打包。该诊断工具属本批证据范围，其他build输出不纳Git，原dc82包保持；若后续取得代码根因，再按前节精确扩卡。

实际使用技能：systematic-debugging、minecraft-modding、minecraft-testing；实现若获精确扩卡再使用TDD与verification-before-completion。PM只管理计划/审查/Git和文档，禁止写功能、测试或自动程序。
