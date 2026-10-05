# EXT-B-TURBINE-01G：断汽后零容量残留转速整改

> PM使用systematic-debugging、writing-plans和subagent-driven-development组织单一整改与一次规格/质量联合审查；执行者不得派发其他代理、改核心文档或执行Git写操作。治理5.1精简验证优先于通用技能的重复流程。

**状态：已派发范围，待根因复现。** 用户2026-10-05反馈撤掉蒸汽输入后有时持续输出零应力转速，并确认持续超过数秒、不能自行停止。不能把此现象记为正常40tick平滑响应，也不能据此将闭环手测记为通过。

**目标：** 修复已批准01F“流量衰减后本机动力归零”合同内的生命周期缺陷，不更改玩法参数。

**规格：** [01F行为合同](./2026-10-05-ext-b-turbine-01f.md)及[效率规格](./2026-10-05-turbine-efficiency-proposal.md)。三档规模、256RPM、倍率、30%门槛、40tick窗口、极小周转量、两端共享唯一SU均不改。

**技术基线：** MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；只读本地锁定Create源码，不更新依赖。

## 写集与职责

- 候选`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，`codex/ore-acquisition`，派发前HEAD `ecc6305`。保留`logs/debug.log`、`logs/latest.log`及`tools/art-assets/__pycache__/`，不得覆盖其他任务或用户改动。
- 必读AGENTS、治理5.1/5.2、本卡、01F规格、冷凝[合并联调](../../reviews/2026-10-05/condense-01/manual-checklist.md)。实际读取技能`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`及superpowers的systematic-debugging；按既有注册/模板实现，版本示例不替换仓库基线。
- 功能仅允许修改`turbine/TurbineShaftPowerSource.java`、`TurbineOutputShaftBlockEntity.java`、`TurbineControllerBlockEntity.java`、`TurbineState.java`，均位于`src/main/java/com/iksxh/create_nuclear_industry/`。只修改实际根因所需文件，不借机重构。
- 测试仅允许修改`gametest/ExtensionTurbineGameTests.java`、`gametest/turbine/`既有Java文件、`src/test/java/com/iksxh/create_nuclear_industry/turbine/TurbineStateTest.java`。若需要隔离新增GameTest类/模板或其他文件，先报告PM确定准确路径。
- 报告与临时探针写`build/reports/extension/EXT-B-TURBINE-01G/`；隔离测试服使用`build/runtime-01g-*`。禁止写docs、配置、配方、美术、其他设备、构建脚本及全局mixins，禁止编辑用户世界或停止用户客户端。
- 所有手写注释/Javadoc中文；说明服务端权威、Create源/网络容量与实际转速的区别、tick顺序及关键不变量。首发前不安排旧存档兼容研究。

## 单任务实施与检查

- [ ] 追踪实际排汽→流量窗口→本机SU/RPM→两轴`updateGeneratedRotation`→Create网络Source/Speed的变化；读取当前Create `GeneratingKineticBlockEntity`、传播器与网络实现。先向PM报告可复现根因和最小修复方向，不因用户症状直接猜测修法。
- [ ] 添加代表性失败断言，先证明修前持续零SU残留转速。至少覆盖双轴机内贯通、两端接普通轴、无外源，持续供给后停止输入并等残留排出及窗口衰减；断言本机SU、生成转速、前后轴实际速度和外接轴速度归零。使用真正Create网络，不能只断言账本归零。
- [ ] 只修复已确认根因。无外源时停止本机及被它驱动的网络；有外部动力源时仅撤销本机生成容量，保留合法外源转速/容量及机内贯通。不能无条件强制全网速度归零来掩盖问题。
- [ ] 定向用例组合验证断汽停止与再次供汽恢复、容量不重复登记；同速外源下本机归零但外源继续；必要时针对tick先后顺序补一个代表性场景。原生40tick响应保留，不增加迟滞、等待或新参数。
- [ ] 默认只跑受影响动力网络GameTest域及一次增量assemble；如果修改账本，追加`test --tests "*TurbineStateTest"`；若修改真实控制器接入，跑相应已有真实汽轮机域。不跑全项目、冷凝整套或全部外观/跨区块历史矩阵；只有具体回归疑点才扩大并说明原因。记录准确命令、退出码及原始日志。
- [ ] 冻结写集，交付`build/reports/extension/EXT-B-TURBINE-01G/implementation-report.md`，列根因、最小差异、中文注释、技能使用、失败与成功证据、未测边界和实际写集；PM安排一次联合独立审查，复用运行证据，不重复测试。

## 人工验收与PM记录

用户现场只追加一项：正常供汽后撤掉输入，等周转排完与40tick窗口衰减，确认无外源时不再持续零SU转动；重新供汽恢复。冷凝与01F原未验收项仍按合并清单继续，不重测已通过的结构/模型。未完人工门不合main、不派发下一玩法。

- PM明确将此批作为已批准行为的bug整改，不改变运行规则或新加惯性设定；如调查结果确需新玩法取舍，停止该取舍并报告。
- 优先高速代理gpt-6-luna/high完成有界调查与整改；本地Create网络生命周期若超出该代理可确认范围，再升级复杂环节。
