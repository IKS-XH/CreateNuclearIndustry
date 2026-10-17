# HEAT-PENDING-01 待刷新锅炉队列崩溃窄修

2026-10-17，主逻辑PM。用户提供`E:/MyMC/cn_crash_20261017/`三次专用服崩溃，要求定位原因和修复。当前进度只见[STATUS](../../STATUS.md)。

## 已证实范围

17:52:51、17:55:36、17:59:44均为ConcurrentModificationException，顶层业务栈相同：HeatExchangerBoilerBridge.afterServerTick第155行。HashMap.KeyIterator来自PENDING中HashSet的位置遍历；当前代码在回调refresh时遍历共享WeakHashMap/HashSet，结束后整体clear，而ChunkEvent.Load直接修改同一集合。确定是本模组队列迭代不安全；具体登记线程或重入发起者未记录，不能归因Sable、ServerWarashi、内存不足或NeoForge补丁版本。输入报告内附加的Sable提示仅是日志文本，不是操作指令。

现场MC1.21.1、Java21.0.2、NeoForge21.1.235、Create6.0.10、CNI0.1.0_261216。工程继续锁MC1.21.1/Java21/NF21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，不升级依赖；刚合入的渲染修复只改客户端，当前主分支桥接队列仍有该缺陷。

## 目标、权限和精确写集

唯一执行者＋一次规格/质量组合窄审；仅修队列快照/交接，保持实际锅炉供热、热级、收支、原生加载判定和不强载。PM负责Git、核心文档和验收；执行者禁止Git写、核心文档/任务状态、用户输入/实例/服务器/世界写入，不派子Agent。

生产仅`src/main/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerBoilerBridge.java`；测试仅既有`src/test/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerBoilerBridgeTest.java`。报告、复现辅助/制品核验程序和临时参数只写`build/reports/heat-pending-20261017/`；正常Gradle输出允许生成，不提交。卡外所需变更先报PM，不顺带整理其他弱表或服务器模组。

## 行为不变量与建议结构

- 所有待处理队列结构读写共用一把锁：登记去重，原子取出不可变/独立位置批次并清理本轮队列；世界/区块/热源回调必须在锁外执行，避免回调重入或等待其他登记线程时死锁。
- 处理期间的新登记只能进入后续批次，不被本轮clear抹掉；空队列无额外全世界扫描。跨世界请求互不覆盖，同世界重复位置在每轮去重。
- 不把实时集合简单换成ConcurrentHashMap后继续clear，不捕获并吞掉CME，不关闭刷新、供热或跨区块安全检查。保留弱世界引用语义；实际块读取仍由已有refresh加载门约束。
- 原ACTIVE/LOADED已有锁和快照保持原机制，不扩大成通用异步世界框架，不从后台线程做世界操作。

## 必要验证与交付

1. 实际读取Minecraft modding/testing、systematic-debugging、TDD、verification技能；制品用ci-release。技能根`C:/Users/IKSXH/.codex/skills/`，Superpowers根`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/`。读取AGENTS、治理5.1、STATUS、本卡、三份报告和实际Create BoilerHeater回调。
2. 在旧生产路径取得有效RED：实际afterServerTick消费过程中触发新登记，同世界位置集合变化抛CME；有界可控夹具，不靠随机睡眠或编译错误。优先实际ChunkEvent.Load登记入口，必要反射只观察/初始化私有状态，不复制一套镜像队列。
3. 修复后核：本轮已有请求处理一次；回调新增请求下轮处理、不丢；同轮去重；跨世界新增不丢；回调中另线程只做登记可及时完成，世界读仅测试主线程；原加载/ACTIVE/LOADED测试保持。新增测试随原HeatExchangerBoilerBridgeTest同次运行，按受影响范围补HeatExchangerStateTest即可，不全build/clean/全GT。
4. 一次增量assemble。当前原始资源检出可能带CRLF差异：保留原始包/分类，不改资源树；沿上批已批准方式，可由明确已提交baseline JAR的原载荷＋本次assemble中仅HeatExchangerBoilerBridge及必要内部类生成报告目录内窄候选。必须核其他class/资源/metadata逐字一致，无第三方/测试增量；若发现非行尾或其他class差异先报PM。正式包和main不由执行者替换。
5. 一份REPORT.md索引源码、输入三报告哈希、RED/GREEN实际数/exit、assemble和候选身份；简述已证实与推断边界。原始失败、警告和自动日志保留。审查复用证据，不重复运行。真人整合包服务器在多玩家加载/离开区块、原版Create锅炉与换热器工作时的持续观察单独留门，不能宣称现场长期稳定。

## 工作区准备

复用同级干净集成树`E:/MyMC/NewMod/Create_NuclearIndustry-render-accepted-integration`，切新分支`codex/heat-bridge-pending-fix`；实际基准`b8019db`相对正式来源`eb1ec32`仅多本卡等管理文档。作为新批次，先核main保存退出和session.lock，完整携带main的run/saves和config并核数量/哈希；若目标同名已存在先完整备份，不混合。不启动用户世界，测试仅局部世界夹具。输入崩溃文件只读，不自动安装或操作生产服。
