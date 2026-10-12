# TURBINE-RUNTIME-DISPLAY-01 实施交付

执行树 `E:/MyMC/NewMod/Create_NuclearIndustry-turbine-runtime`，分支 `codex/turbine-runtime-display`，起点 `d63abfe`。本报告为执行者交付，不作验收或 Git 集成结论。

**当前交付为同一次独立审查P2窄修后的 R1：16/16 JUnit，复用原真实3/3，必要第二次增量assemble退出0。首轮AEC制品与清单原位保留，现行冻结在证据目录的 r1/。**

## 结果与边界

新增同龄、不可变的转子成员运行投影及原生 BE 同步，服务端只读合法前轴 `getSpeed()`，并要求后轴实速一致；Post 每 5 tick 发布。北/西朝向保持世界轴速符号，南/东反号，统一为前轴→后轴的局部 +Z。可靠停转返回样本且 `rpm()==0`，不可用返回空。客户端 20 活动 tick 租约，单人真实暂停冻结年龄；拆件、身份/世代不符、坏包、缺块、卸载、世界结束均撤销。

Controller 仅增实例 UUID、原 epoch getter、更新包身份字段及显示登记/撤销钩子；原 `onLoad` 失效调用仍保留。RotorBE 只增不落盘显示状态、原生更新包、查询与生命周期。现有配置/账本、热工、汽耗、容量、双轴传播、残转恢复、流体能力、辅助建造、模型与 Renderer 未修改。控制器磁盘/可携数据不写显示身份，转子不保存运行样本。

实际使用技能：`minecraft-modding`、`minecraft-testing`、`systematic-debugging`、`test-driven-development`（含 writing-good-tests）、`verification-before-completion`。已核实际 MC 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6；通用完整 build 被 AGENTS/治理 5.1 与本卡定向合同覆盖。

## 原始验证记录

证据目录：`build/reports/turbine-runtime-display-01/`。

| 记录 | 实际结果 |
| --- | --- |
| `01-red.log` | 9/9 RED；编译成功，反射行为测试在缺少运行接口处断言失败（ClassNotFound），不是已有运行实现的失败复现。 |
| `02-green.log` | 第一次实现编译失败：Create `SmartBlockEntity.setRemoved()` 为 final。核锁定字节码后改用 `remove()` 生命周期，不改动力。 |
| `03-green.log` | 初始 9/9 JUnit，exit 0。 |
| `04-gametest.log` | 第一次真实服启动：Gradle exit 0，但 `No test functions were given!`，没有业务测试，不计通过。锁定 NeoForge 按 templateNamespace 过滤；PM 精确扩卡到专属资源命名空间。 |
| `05-green.log` | 补充旧世代/水位/字段类型与暂停边界，12/12，exit 0。 |
| `06-gametest.log` | 专属 3/3 真实 GameTest 通过，保存完成、自然退出，Gradle exit 0，26 秒。 |
| `07-final-test-assemble.log` | 首轮冻结 13/13 JUnit（0 失败/错误/跳过）及首次必要 `assemble`，exit 0，16 秒。生产 compileJava UP-TO-DATE，无完整 build。 |
| `final-junit.xml` | 首轮冻结原始 JUnit XML。 |

真实组独立 namespace=`turbine_runtime_display_probe`，目录=`build/gametest-turbine-runtime-display`，没有启动用户客户端或写 `run/saves`。三项覆盖：

1. 正式机组合法成员归属、配置 256 与实速 0 的区别、完整初次跟踪包及不持久化、Create 外源 -64 RPM 与停止、转子拆除/恢复、控制器重建不同 UUID。
2. 正式 SC 流体能力真实填入/排出建立 SU 与 256 RPM，断流后自然窗口衰减与轴实际 0，显示 0；没有改热工矩阵。
3. 外源 64 RPM 建网后，夹具通过原生 `updateFromNetwork` 注入过载结算状态，验证理论 64、`getSpeed()==0`、显示 0；另调用真实 `onChunkUnloaded()` 钩子验证撤销。此项不等同自然负载过载或真实区块驱逐矩阵。

专服实际加载成功验证 common BE/发布器无客户端类装载故障。JUnit 覆盖本地 clock/lease 的真实暂停、换会话和撤销；没有运行图形客户端，不宣称 GPU/真实鼠标/单人暂停人工验收。

## 首轮冻结与保护（原件保留）

- `sources.json` 绑定 11 个本批源/测试/模板；原件复制到 `frozen-source/`。
- `jar-entries.json`：12 个实际编译 class 与 1 个模板，共 13 项 JAR 内容逐字一致。
- `compiled-api.txt` 是本次 `javap -public` 的实际编译签名，不是设计稿。
- JAR：`create_nuclear_industry-0.1.0.jar`，SHA256=`AEC242A8D3212281BC847610152A2007DC068098E5AA0ADC288C381DD6F925A6`。
- `protected-source.json`：State、Structure、ShaftPowerSource、OutputShaftBE、RotorRenderer、TurbineConfig、build.gradle、gradle.properties 八项与 HEAD Git blob 一致。
- Controller diff 只增加显示身份、网络元数据和生命周期；原交易与运行方法保持。未写美术模型、材质、动画相位或 Renderer。
- Gradle 测试自动改写根 `logs/debug.log`、`logs/latest.log`（仓库已跟踪）；没有手动清理/回退，交 PM 处理，不纳入实现净集。
- 任务卡和 `docs/art/ART-TURBINE-10B-COORDINATION.md` 由 PM 写入，本执行者未修改。无 Git 写操作、无世界/配置/构建脚本手写修改。

实际接口和原消费者接点见 [HANDOFF](HANDOFF.md)。生产冻结后已报 PM 安排本批一次独立组合窄审；本报告不冒称审查或动画视觉通过。

## 同次审查 P2 窄修 R1

审查指出：原 Lease 在同 generation、同 serverTick 时直接拒收，未撤销带不同 epoch/member/几何/RPM 的包；Rotor 查询又只核 pending，却返回旧 lease.latest。已经实际核实并复现旧128RPM残留，未以“拒收返回false”误作失效证据。

- `08-review-red.log`、`08-review-red.xml`：3 个新增回归，2 个失败、1 个通过。失败直接命中同tick身份变化与几何/RPM变化后旧动画残留；真正同身份同整包重复保持不续租测试通过。
- 仅改 `TurbineRuntimeLease.java`：同tick必须逐字段相同才作为不续租的真正重复；任何差异立即 revoke，不拼包、不沿用旧转动。
- 仅改 `TurbineRotorBlockEntity.runtimeDisplay()` 的客户端查询：取实际将返回的 lease.current.value，核其自身member、与pending整包一致、几何/owner generation/epoch以及当前owner对象，再返回。失败撤销；不再用pending为旧value背书。
- 专属测试增加同tick epoch/member、index/rotors/front-owner几何、RPM差异和真正重复/撤销不复活边界。
- `09-review-green-assemble.log`：最终16/16（0失败/错误/跳过），第二次必要增量assemble exit0，17秒。未重跑真实3/3；发布器、正式动力、流体、Renderer、模板与GameTest源码均未改，原证据复用。
- `r1/final-junit.xml`、`r1/sources.json`、`r1/frozen-source/`、`r1/jar-entries.json`、`r1/compiled-api.txt` 为当前冻结。11来源/13JAR项逐字一致，公共ABI与首轮完全一致。
- `r1/changed-sources.txt` 只列上述2生产+1测试；`r1/changed-jar-entries.txt` 只列 RotorBE、Lease 两class。原AEC JAR与首轮清单不覆盖。
- 当前JAR SHA256=`203E955881FE60074069E5A12D3C480A4166B877C2BD56DCDAFB57C5F7833266`。

已将冻结交回PM及原审查者做同一次差异复核；不新增第二轮独立审查，不宣称未收到的审查结论或客户端动画验收。