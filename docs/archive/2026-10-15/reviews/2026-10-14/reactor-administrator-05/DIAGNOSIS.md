# REACTOR-ADMINISTRATOR-05 只读诊断

2026-10-14。唯一执行者 `/root/reactor_automation_retrofit_impl`。已读AGENTS、05卡、04候选交付和Minecraft/systematic-debugging技能。**已确认GPU缓冲分配失败，尚未确认源码根因；未实施修复。** 用户确认点击START后界面一直打开，目前只发生一次，没有重复复现证据。

## 直接事实

- 候选HEAD `410807f6facd48691929c29be2263c2899db3bc4`，原功能/包提交d217196；MC1.21.1、NeoForge21.1.219、Create6.0.10、Flywheel1.0.6、Java21。本次不是Sodium/Iris环境。
- 冻结日志显示11:49:27.774出现`Saving and pausing game`，11:51:39.193～194 NVIDIA报告PID90548的out-of-memory及`GL_OUT_OF_MEMORY`、`Failed to allocate memory for buffer data`。这证明GL缓冲分配失败，不能代替Java异常堆栈或指出哪项分配。未获得本轮Minecraft crash报告/hs_err；任务卡已记录runClient wrapper exit1。90548现已不存在，不能补取当时峰值。
- 12:00:48.820的`build/reports/reactor-administrator-05/00-incident-identity.json`及`incident-copy/`冻结了**事故后的38世界文件、27配置、15日志**，全部读取时稳定；没有用04开工前数据覆盖用户测试进度。原包3199564B，SHA-256 `dc82b20a19f17e81db2fd4e9efa69ed2b861fd703deaca4553ac7dfcbd6a7b05`，副本在`incident-copy/build/libs/`。另记录2060源码身份与现有run/options.txt。

## 已核调用链及其限度

- `ReactorAdministratorScreen`继承的实际`AbstractContainerScreen.isPauseScreen()`返回false；普通Screen基类返回true。管理员界面本身不暂停。日志那次暂停可能来自其他屏幕/窗口状态，单条记录不能证明随后整个故障窗口一直暂停，也不能证明每tick都在结算。
- Screen初读仅文字、填充和控件，无私有GPU缓冲/纹理创建。菜单显示投影按10tick或状态/代际变化发送，客户端只更新当前菜单投影；没有看到投影更新直接创建GL对象的入口。
- `ReactorAnimationMaterials`为256个固定ID、16×16纹理槽，reserve仅新归属创建，并有boundary/clear调用release；`ReactorInternalRenderBuffers`固定注册256个memoized entityTranslucent层。静态数量有界，不是每帧新增无限槽。
- 锁定LevelRenderer的`AFTER_BLOCK_ENTITIES`静态分发路径通达本模组finish，finish提交指定层；原MultiBufferSource.endBatch移除startedBuilders，BufferUploader沿VertexFormat的immediate缓冲上传。GUI打开时后台世界可以继续渲染。**这些是静态调用链，不是事故期间逐帧计数；有界对象仍可能有较高容量/驱动内存峰值，尚不能证明无泄漏或资源峰值安全。** API材料保存在`01-render-api.txt`和`02-world-buffer-api.txt`。

当前只能降低“Screen显式每帧创建私有GL对象”“无限纹理槽ID”这两种具体猜测的优先级；不能把管理员启动、热工、驱动、背景动画或当前系统内存压力确定为根因。崩溃后空闲显存不作为事故峰值。没有可靠复现/运行期分配统计，暂无可提出的精确生产修复写集。

## 已交付只读观察工具；尚未进行正式同条件复测

05卡“只读采样补充授权”已允许并交付`build/reports/reactor-administrator-05/observe-client-memory.ps1`：默认**6分钟、请求每秒一次，只读采样**，`-Background`通过`Start-Process -WindowStyle Hidden`仅启动观察子进程。时长限制1～360秒；每次使用独立UTC时间戳与随机后缀生成CSV/JSON，拒绝覆盖。用户自己runClient，观察者不启动/关闭游戏、不读写世界/配置、不改包或驱动。内部只读命令行及候选build/moddev小参数文件用于证明forgeclientdev身份，其原文不进入输出；多客户端歧义不任意选PID。

| 工具 | 实际字段与口径 |
|---|---|
| 已确认`C:/WINDOWS/system32/nvidia-smi.exe` | GPU index、memory.total/used/free，MiB，明确标为**全卡global**，不是Java进程独占显存 |
| `C:/Windows/System32/WindowsPowerShell/v1.0/powershell.exe`的Get-Process | 当前候选forgeclientdev的PID、创建时间、PrivateMemorySize64、WorkingSet64、VirtualMemorySize64，bytes；仅内部匹配候选进程，不输出完整CommandLine/环境/账户 |
| 已确认存在的`Win32_PerfFormattedData_GPUPerformanceCounters_GPUProcessMemory` | 按目标PID匹配GPU实例的DedicatedUsage、SharedUsage、TotalCommitted，Windows计数器原值/bytes；不可用则记缺测，不伪造0或把global当进程值 |
| `Win32_OperatingSystem` | TotalVisibleMemorySize、FreePhysicalMemory、TotalVirtualMemorySize、FreeVirtualMemory，原值KiB，与GPU/Java进程分列；当前余量不能反推事故时刻 |

记录UTC/本地时间、目标waiting/alive/exited/ambiguous/unavailable状态与工具缺测。JSON null/CSV空白表示不可用，真实零值仍记录为零。JSON汇总状态、实际样本与时长；慢查询不补造样本，保留真实查询时间。CSV/摘要用于区分入世界前已高占用、持续增长、一次分配峰值；1秒采样可能漏掉瞬时峰值，趋势也不能单独证明代码归因。

在候选目录`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-automation`执行：

```powershell
.\build\reports\reactor-administrator-05\observe-client-memory.ps1 -Background
.\gradlew.bat runClient
```

第一条立即返回观察PID及CSV/JSON路径，六分钟后自行结束。如客户端已经运行，只执行第一条，再自行操作原客户端。进入同一世界、打开管理员、点击START并保持原页面，至少覆盖原约两分钟窗口；记录START的大致时间和停留页，若再发生故障告知时间。可在第一条附加`-Marker '概览页；START约12:30'`作纯文本备注，工具不会探测或执行START。无需先改画质、内存、动画或驱动。

唯一工具烟测：`-DurationSeconds 3 -Background`，PowerShell解析0错误；隐藏子进程78236自然退出，summary为completed，实际3.011秒、2条样本/2行CSV（首轮查询约1.75秒）。识别到用户已自行运行的候选客户端PID92324，全卡GPU、进程三项bytes、WDDM三项bytes及OS四项KiB均available。该进程不是工具启动；本轮没有故障复现，也没有修改用户操作。真实缺测分支没有另起负面实验，不冒称全部分支运行覆盖。

证据：`03-observer-smoke.json`及独立`20261014T042551Z-6e6c5d03-memory.csv/json`；脚本SHA-256 `98274561602e278662d157b866b638aa1b899d1b30e9d96d24839bd6ca546588`。烟测实核原正式JAR仍为上述dc82 SHA。当前采样不是11:51事故峰值，也不能据此称崩溃已修复。正式六分钟同条件观察仍待用户提供。

本阶段仅写05证据目录与本DIAGNOSIS。除了这一次只读观察工具烟测，未跑Minecraft/Gradle/Java测试、build或runClient；未改生产/测试/构建脚本、JAR、世界、配置，未Git/转派或终止用户进程。04与03/01/01R人工门、main管理员合入及02 PI仍保持原状态。
