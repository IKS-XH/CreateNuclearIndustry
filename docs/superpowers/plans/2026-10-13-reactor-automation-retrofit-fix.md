# REACTOR-AUTOMATION-01R 改装接口后运行数据不可用窄修

2026-10-13。用户在01手测中报告已成型反应堆拆壳换自动化接口后“无法成型”，进一步明确：扳手右键提示已成型，但护目镜显示未成型或数据不可用；接口位于侧壁中间、与仪表隔一格。尺寸清空与延后补液窗口已定位并完成窄修内部验证/审查，源码`2dff787`已交[局部复看候选](../../reviews/2026-10-13/reactor-automation-retrofit-fix/CANDIDATE.md)；客户端尚未验收。

## 范围与执行方式

- 只处理01候选本次拆壳改装路径；02应力余量计划保持待派发，01人工门保持，主目录源码/JAR不改。
- 沿用唯一执行者及一次独立组合窄审。原01实现者当前不在可用Agent列表，PM指定一位唯一接续执行者。PM只诊断、文档、Git和证据，不改功能/测试/构建代码。
- 原位使用`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-automation`，原分支`codex/reactor-automation-basic`、HEAD6d593df/来源4757f27、原包4204ab17…及失败/审查证据保留；不建分支、搬存档或回写用户世界。
- 执行者先只读追踪真实扫描、拆壳安全清理、snapshot重建、首次tick与护目镜可用性；不得将本反馈当作允许绕过拆壳安全、缺区块、控制租约或原红石联锁。

## 诊断阶段权限

读取01活动卡、批准规范、AGENTS和minecraft-modding/testing及systematic-debugging技能。生产源码/原测试/实际配置/日志可只读；确有需要只读检查已保存退出的候选世界，禁止写世界。临时诊断证据仅`build/reports/reactor-automation-retrofit-fix/**`，执行者报告仅`docs/reviews/2026-10-13/reactor-automation-retrofit-fix/IMPLEMENTATION.md`。

诊断阶段禁止源码、测试、构建脚本、资源及运行配置写入，禁止运行Gradle、启动用户世界或Git写/转派。先报告：失败层、精确源码行/实际条件、与新建成型路径差异、可复现最小步骤、必要写集与定向检查。发现护目镜文字混合静态/动态可用性时，先确定实际tick是否失败，不能只改提示掩盖运行失效。下节记录根因确认后向原执行者开放的精确实现写集。

## 已确认根因及实现扩卡

主PM已独立核候选`build/reports/reactor-automation-retrofit-fix/saved-reactor-entities.json`及实际源码：19:02:37全部维度保存现场，仪表(-8,-59,0)、接口(-10,-59,0)，实际外壳bounds[-16,-60,0]～[-1,-45,15]即16³；仪表快照仍5³、冷液32768mB、热液0、燃料/控制列均空、无SCRAM/事故、AutomationClaimed=false。FULL_SHUTDOWN_RESET以默认5格empty清空，旧缓存端口可在延后重扫前补冷液；此后updateStructureCache只允许全empty绑定新dims，所以拒16³。扳手显示独立几何scan，未反映owner尺寸门拒绝，护目镜显示owner同步的实际无效摘要。

允许唯一接续执行者在原候选原位修改且仅修改：

1. `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`：安全完全清空的计划nextSnapshot保留原实际dims；初始化尺寸门只接纳字段精确无核状态（无燃料/控制列、热液、SCRAM/保存目标、事故及待迁移燃料）的快照，纯冷液原量保留且不得超过新派生容量。干净无库存状态仍可按原完全停机规则重建尺寸，含任何真实核状态仍拒尺寸重组。不抹冷液、不改全局Snapshot/handler/核热工或拆壳安全事务。
2. `src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorStructureLifecycle.java`：显式重扫返回仪表最终接受的contract/origin，保留本次read-count和原扫描/流体刷新顺序，使拒绝时扳手不再报告假成功。其他排队、拆除、区块边界不变。
3. 新`src/main/java/com/iksxh/create_nuclear_industry/gametest/ReactorAutomationRetrofitGameTests.java`：独立域`cni_reactor_automation_retrofit`，复用原16×8×16空模板字节与当前原生块/玩家/接口入口。6³足以暴露5格占位错误，不复制用户世界或要求重搭16³。
4. `build.gradle`仅新增本测试类及下述同域模板别名的生产JAR排除，原测试模板排除及开发classpath保持，不改依赖或旧域规则。
5. 首次`01-red.log`为零用例：锁定NF按templateNamespace筛选域，引用原namespace会错过新类，Gradle exit0不能计行为通过或RED。允许把原`src/main/resources/data/cni_reactor_automation/structure/automation_empty.nbt`字节原样复制为唯一新`src/main/resources/data/cni_reactor_automation_retrofit/structure/retrofit_empty.nbt`，使用新域templateNamespace/retrofit_empty并核字节一致。这是隔离测试入口的必要别名，不改变原模板，不添加Generator/自定义Registry绕过或其他测试框架；该别名不得进入生产JAR。

必须先在旧生产上取得真实行为RED，再改生产：真实6³成型→安全BreakEvent拆侧壳→延后重扫前原冷端EXECUTE注入→换接口→扫描、owner有效性、实际更新包静态摘要与正式tick；另覆盖保存现场型5³+32768纯冷液恢复、任意热液/燃料/控制/SCRAM/事故/待迁移状态拒绝重组、超过新容量拒绝且冷量不丢、干净改尺寸及原5³。场景可合并成少量代表，但不得用源码字符串断言、强制清空或人工rescan穿插掩盖原事件窗口；显式扳手用例另证明失败显示实际拒绝。

定向JUnit只选原摘要/可变尺寸/本接口结构等实际受影响代表，由执行者列实际命令与结果；原01控制、管网/美术/设备域不机械重跑。真实验证仅本新域；业务断言与自然保存退出分列。稳定后一次必要增量assemble并完整保留4204原包，冻结精确source/class/JAR差异、无新测试class泄漏，原已审域证据复用。唯一组合审查由PM派原01审查者，同次差异整改不另开审查。

用户现保存世界不由执行者或PM写入：新代码应通过正常加载/重扫恢复该纯冷液现场，含真实核状态仍拒重组。这是当前版本故障修复及正常加载，不新增旧版本迁移/兼容研究。当前用户可能再次runClient，禁止终止其进程或覆盖世界/运行配置；测试仅隔离GameTest目录，临时进程若保存挂起只精确处理本次夹具并如实保留证据。

## 拟定窄边界与交付要求

根因确认后仅修实际失败入口并新增有意义行为RED/GREEN；不增加玩家重新拆仪表的绕过步骤、不重跑全部01或旧设备测试。按实际差异选择小堆改装真实代表和必要增量包，同一审查者复核实际差异。业务断言成功与保存退出挂起分列，保留原失败/原包，不清理无关dirty。

交付停此次改装/护目镜局部复看，未通过不能派02，不将局部复看扩大为01完整人工验收。原参数、配方、控件、温压、管网与美术保持。本卡后续写集扩充和实际结论由PM登记，不能由执行者自行改动。

## 实际交付

唯一执行者已按五条写集完成，原01审查者一次组合窄审无剩余必改；源码净提交`2dff787`。原`01-red`零用例和`02/03`夹具错误均保留，实际旧生产行为RED为`04-red-corrected`三个预期失败；最终定向18/18、真实三个代表自然保存退出0、唯一1秒增量assemble通过。旧设备/控制域未重复验证，不将6³代表或BE读取描述成用户16³重启验收。

正式及冻结包`5e04c667fc509a0eb9349bb95faf6c3a15b5d19a961ffdeb29458d00b5c2d434`，3138597B；4204原包保留。PM独立核五来源、646class、1546资源及正式/冻结/编译输出一致，文件载荷仅两生产入口和两个实际指令相同的行号伴随class变化；新域仅两个空目录，class/NBT无泄漏。最初PM全ZIP项比较发现目录差异，按文件载荷限定后通过，无必要语义变更故不重打包。

报告和启动见本批[CANDIDATE](../../reviews/2026-10-13/reactor-automation-retrofit-fix/CANDIDATE.md)、[IMPLEMENTATION](../../reviews/2026-10-13/reactor-automation-retrofit-fix/IMPLEMENTATION.md)、[REVIEW](../../reviews/2026-10-13/reactor-automation-retrofit-fix/REVIEW.md)。源码/JAR留原候选，主目录功能包与用户世界/配置未覆盖。01集中人工门和02待派发保持；独立Logo仅已交美术来源，未混入本包。
