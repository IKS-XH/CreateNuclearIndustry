# ACCIDENT-THERMAL-04A 组合窄审

日期：2026-10-21。结论：**本卡范围内未发现未闭 P1/P2 代码阻断**。这是一次规格与质量组合窄审，不构成人工验收、main 合入或制品放行。

## 来源与范围

- 审查事故树 `E:/MyMC/NewMod/Create_NuclearIndustry-accident`，分支 `codex/accident-thermal-04a`，HEAD/基准 `37f50b9e3425992f74beecb8ecd6e2c79514257e` 上的未提交增量。
- 权威合同为主工程 `E:/MyMC/NewMod/Create_NuclearIndustry/docs/superpowers/plans/2026-10-21-accident-thermal-04a.md` 及其批准设计；已读根 AGENTS、治理 5.1、STATUS，实际使用 minecraft-modding、minecraft-testing、minecraft-resource-pack 技能。按实际 Gradle 维持 MC 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82 / Flywheel 1.0.6。
- 读完本卡生产差异、4 个新增生产类、相关测试与资源，以及 EXECUTION/SOURCE/RESOURCES 和最终必要日志。候选三份 docs 同步属于 PM 写集。未扩大至熔融物、溶蚀、旧存档迁移、原热工/接口矩阵或其他任务。
- 当前 23 个源码/资源/测试文件、20 个编译 class 的 SHA256 均匹配 `SOURCE.json`；`EXECUTION.md` SHA256 为 `75F235BFA7958AA7043C1E95EDE98E82EFA1EBA0DE90689A9089170D80716115`。`SOURCE.patch` 仅含 tracked 差异，新增文件结合 SOURCE 清单及实际文件审查，未将 patch 当作完整来源。
- 本审查没有运行构建、测试、服务端或用户世界，没有 Git 写操作，只新增本 REVIEW。

## 核心审查结果

以下代码位置均位于 `E:/MyMC/NewMod/Create_NuclearIndustry-accident/`。

| 检查点 | 代码位置与结论 |
| --- | --- |
| 原完成与拆除 | [ReactorAccidentLedger.java:73](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/ReactorAccidentLedger.java:73) 新热状态不进入原 complete 判据；[ReactorAccidentSavedData.java:12](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/ReactorAccidentSavedData.java:12) 分离初始/热活跃索引。Port、原安全门和热工未改。聚焦 shutdown 业务明确在热状态仍活跃时执行真实空口重绑、仪表维护和 FULL_RESET，没有等待 2400tick。 |
| 真实物料与接触伤害 | [ReactorCoriumBlockEntity.java:21](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/ReactorCoriumBlockEntity.java:21) 接收唯一单件废料后才创建热引用，`:27` 先取走载荷并终止热引用；新增热状态不含 ItemStack。[ReactorCoriumBlock.java:19](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/ReactorCoriumBlock.java:19) 的 stepOn/inside 汇入 [AccidentThermalEffects.java:78](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/AccidentThermalEffects.java:78)，按真实 BE 载荷、当前账本及剩余时间判断，空 HOT 外观无伤害；实体共享 20tick 节流，默认 2HP/3秒，使用原生 hotFloor/fire resistance。 |
| 火点身份与终态 | [ReactorAccidentService.java:64](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/ReactorAccidentService.java:64) 只从原内部破裂计划选择有限候选；[AccidentThermalEffects.java:54](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/AccidentThermalEffects.java:54) 在原 complete 后一次检查空气、支撑和新世代。[AccidentThermalState.java:38](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/AccidentThermalState.java:38) 上限 8、同位置不重选；`:53` 默认 600 个加载tick 到期。[AccidentFireBlock.java:27](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/AccidentFireBlock.java:27) 原生移除写终态，`:35` UUID 租约与 [AccidentThermalEffects.java:70](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/AccidentThermalEffects.java:70) 联合阻止过期任务清除后来方块。 |
| 原生火边界 | [AccidentFireBlock.java:16](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/AccidentFireBlock.java:16) 继承 BaseFireBlock，不继承 FireBlock 的传播调度；`:21` 覆盖原生门户入口。已对照本地锁定 NeoForge sources 中 BaseFireBlock 的 onPlace/entityInside；fire 标签保留原生左键扑灭路径。06 另有原生玩家攻击、真实向下水流到达火格及邻木不燃证据。 |
| 加载、预算与索引 | [AccidentThermalEffects.java:14](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/AccidentThermalEffects.java:14) 同时要求 hasChunkAt/getChunkNow，六面水检查也先验加载；[AccidentThermalState.java:49](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/AccidentThermalState.java:49) 只扣当前可用tick，同tick防重、不追补离线差额。[ReactorAccidentExecutor.java:24](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/ReactorAccidentExecutor.java:24) 只组合活跃集合；[ReactorAccidentSavedData.java:52](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/ReactorAccidentSavedData.java:52) 原动作/冷热投影/火点共享维度额度，同tick重复入口不刷新。每次 thermal one 至多一次世界写，flags 18；逻辑到期先停止危险，再排队清投影。没有引入每tick全历史账本扫描。 |
| 配置与资源 | [AccidentThermalParameters.java:8](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/AccidentThermalParameters.java:8) 默认 2400/4/8/600/2HP/3秒，事故创建冻结并保存；fire 数/时长硬上限 8/600，非法组整体回退。HOT 两状态、原版 magma/crying_obsidian、fire_floor0 与 cutout 引用一致；火无物品入口、无掉落。PM docs 的六键/默认值及固体范围与实际入口无明显失配。 |

新增手写注释为中文，覆盖热引用与库存职责、加载计时、共享预算、火点身份和终态等非显然边界；未发现为风格需要阻断的问题。

## 证据与局限

- 定向命令为 `./gradlew.bat test --tests '*AccidentThermalStateTest' --tests '*AccidentLedgerTest' --console=plain`。`03-unit.log`、退出文件及 final-unit-results XML 相符：Ledger 4 + ThermalState 5，合计 **9/9，0 失败/错误/跳过，exit 0**。
- 最终命令使用 `CNI_ACCIDENT_FOCUS=thermal_solid_fire` 与既有 `runGameTestServer -PgameTestNamespace=cni_accident_execution -PgameTestDirectory=build/reports/accident-thermal-04a-20261021/server-final-focused --console=plain`。`06-server-final.log:145`、`:240` 与测试源码对应：17 份真实废料和 1 份原件返还、真实顶面移动接触、节流/火抗/空残骸、火点扑灭/身份/到期、当前 BE/DataStorage 读回、接水及自然加载冷却、双事故峰值 64/t；shutdown 关联业务同时执行。
- `06-server-final.log:245` 的 13 required passed 实际是 **2 项业务 + 11 项显式 SKIP**，不宣称 13 项业务覆盖。`:255` 起全部区块/维度保存，Java 正常结束、Gradle BUILD SUCCESSFUL；最终 exit 文件为 **0**。
- 保留 `01-red` 的 8 项中 4 项预期业务失败及 exit 1；`04-server` 的 2 项夹具断言失败、Java/Gradle 异常退出亦保留。当前证据显示 `05-server-related` 已成功（exit 0），06 是进一步收紧真实水流/采掘等断言后的最终成功，不把 04 或 05 作为最终结论的替代。
- `RESOURCES.json` 与其校验脚本实际读取锁定本地 MC 资源，父模型/贴图/标签/语言/空 loot 链完整；这不等于客户端视觉或玩家操作验收。
- 物理 ChunkEvent.Unload、04A 实际 FULL 撤销/恢复、独立进程重启后的热状态恢复、真实多人和客户端视觉未在本批实证。加载暂停主要由定向状态断言与实际加载守卫支撑；当前格式读回及正常停服保存已经覆盖。保留这些边界，不为本批另开矩阵，也不将旧事故服务端证据扩大解释为本次热状态的卸载实测。
- `git diff --check -- docs src` 通过。SOURCE 内根日志状态是恢复前快照；generated-root-logs 两份生成日志 SHA 为 `BB79CF1B69C58BCB43A6D4E58B494CE35B0B74EA1269C21E2577AD52B26CAE87`，现根日志均恢复为 PRE-VALIDATION 的 `B094FA55E95CBD30DD0B2B58647AB547EEFB257D63DD22410F754B16AC19E56D`，未误算为生产写集。
- 尚未生成本批候选 JAR，未审不存在制品。受保护原 JAR 仍为 `81E7CD98D589DDC9C0AAF398C06776DC4318EA54C70CD19376F0C127E4A3DE7B`；该一致性核对不代表其包含本批代码。用户世界未由本审查触碰。
