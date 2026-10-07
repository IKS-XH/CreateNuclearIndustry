# 汽轮机思索实施计划（三情景）

> **For agentic workers:** 使用 `superpowers:subagent-driven-development`，由PM派发执行者实现。仓库角色、Git禁令和按范围验证优先于技能通用流程。用户已经要求开始，不重复确认本卡。

**Goal:** 用三个独立情景讲清现有汽轮机的搭建、接管、动力输出和流量效率，随后由PM整理文档；本台播放验收通过后才准备换热器工作盆供热。
**Architecture:** 新建 `TurbinePonderScenes`、三份NBT及可复现生成入口，接入现有 `P1PonderPlugin`。只操作Ponder客户端临时世界，不改汽轮机服务端规则、正式模型、配方或配置。
**Tech Stack:** Minecraft 1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，保持现有依赖。
**Spec:** 用户2026-10-08明确“开始实现汽轮机的思索教学吧，完工后整理一下文档”。后续顺序见[接续排期](./2026-10-08-turbine-exchanger-ponder-sequence.md)，现有机制以实际 `TurbineAssembly`、`TurbineGeometry`、`TurbineStructure`、`TurbineState` 与 `TurbineConfig` 为准；文案准则继承已验收离心机、反应堆和锅炉教学。

## 全局约束

- PM负责文档、审查、状态和Git；实现/代码整改由执行者承担。执行者不得Git写、改核心文档、派发子任务或覆盖无关改动。
- 复用同级候选 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，派发基线 `4e50e83`；日志与Python缓存等原有改动保留。主目录当前功能已验收，不直接在那里实现。
- 仅教授现有功能，不加入事故、辐射、普通蒸汽驱动、动画新机制或旧存档兼容。保持Create无GUI风格。
- 中文玩家文案短句、一次一个信息点。删去开发措辞、基础管泵知识和不言自明的描述，不单列重复“停机与排查”页。
- 一次只显示一段正文。Ponder1.0.82文本寿命含额外10tick，下一段至少再留10tick净空；关键帧应放在读完当前信息之后。
- 模板默认不可见；不得开场隐藏尚未showSection完成的区域。遵守15tick区段合并生命周期，基础板与设备显示区域避免重叠隐藏。复用锅炉R2修正经验，不捕获空指针或修改第三方库。
- 端口、轴和管路放在默认镜头可见侧，各流向能清楚区分。模板状态必须使用真实构件属性和占格；成型显示不可变成错误方块或漏壳。
- 本卡是教学任务：客户端临时NBT可展示库存/工况，禁止为播放调用正式服务端扫描/热账本或修改生产类提供捷径。注释明确客户端边界，不能把静态展示宣称为运行模拟。

## 现有玩法合同

- 默认三档直径/转子数为3/3、5/6、7/9，对应宽×轴向长×高为3×5×3、5×8×5、7×11×7；尺寸来自配置，但只支持三档八棱截面，不是任意尺寸。
- 两端面轴心放动力输出轴，中间轴心放连续转子；其余端盖为外壳，中段按八棱外环包壳，内腔非轴心空格留空。
- 控制器和蒸汽入口位于四侧中央轴向行；排汽口在四侧靠两端的内侧行，端面只放轴与壳。观察窗放在合法侧面位置。模板遵守实际几何规则，禁止只搭长方体箱体。
- 手持外壳右击核心逐块补环；右击已有壳沿轴补相邻环。先有完整轴列才能推导形态；全部必需构件完整后才能运行，拆件停机但其余外观保留。
- 仅输入超临界蒸汽，排出蒸汽。两种流体管路分别可见；出口堵塞会阻止继续处理。
- 默认256RPM，两端共享同一份总应力，不对半固定分流也不翻倍；可以从任一端取力。
- 默认额定处理54/108/216mB/t。按40tick窗口的平均实际排汽流量决定效率和启机资格，不按输入储罐量判断。
- 低于额定30%仍耗汽但不输出动力；30%处0.5倍，随流量线性提升，满流量达1.2/1.5/1.8倍。只保留约1tick额定量的周转缓存，不描述为大库存或长期续航。
- 断汽/堵塞后动力随有效处理停止；不把平滑窗口内短暂变化讲成故障。红石停机如演示须核对当前实际行为。

## Task 1：执行者制作三情景

**新增文件：**

- `src/main/java/com/iksxh/create_nuclear_industry/ponder/TurbinePonderScenes.java`
- `tools/ponder/turbine_scenes.py`
- `src/main/resources/assets/create_nuclear_industry/ponder/steam_turbine_build.nbt`
- `src/main/resources/assets/create_nuclear_industry/ponder/steam_turbine_operation.nbt`
- `src/main/resources/assets/create_nuclear_industry/ponder/steam_turbine_efficiency.nbt`
- `src/test/java/com/iksxh/create_nuclear_industry/TurbinePonderContractTest.java`

**可修改：** `P1PonderPlugin.java`仅新增本台绑定；`assets/create_nuclear_industry/lang/zh_cn.json`和`en_us.json`仅新增本台Ponder键。若现有模板目录实际为其他路径，沿用真实目录并在报告记录，不新建平行资源体系。

**入口：** 核对实际注册ID后，为 `turbine_casing`、`turbine_rotor`、`turbine_inlet`、`turbine_exhaust`、`turbine_output_shaft`、`turbine_controller`、`turbine_window`挂接全部三个故事板，不改变前面设备的绑定。

| 故事板ID | 中文标题 | 必须看见的内容 |
| :--- | :--- | :--- |
| `steam_turbine_build` | 汽轮机：搭建 | 连续轴列、手持外壳补环的操作、八棱包壳、控制器/进汽/排汽/观察窗的合法位置；分步展示三档粗细和长度差异，避免同屏拥挤 |
| `steam_turbine_operation` | 汽轮机：通汽与输出 | 完整小型机、可见SC进汽和蒸汽排出两条独立管路、两端外接轴，共用总应力；通汽运行及断汽停止，堵塞规则用一句说明 |
| `steam_turbine_efficiency` | 汽轮机：流量与效率 | 用流量变化演示低于30%耗汽无动力、达到门槛及满流量；三档吞吐和最高倍率对照，短窗口过渡及小周转缓存只讲操作含义 |

- [ ] 实际阅读根AGENTS、相关源文件、本卡和 `minecraft-modding` / `minecraft-testing`；核对版本。辅助使用Ponder锁定源码与已有模板工具，不查泛化的新版本API。
- [ ] 生成真实合法模板和形态属性；分别核对三档几何、入口/出口朝向、轴端和可见侧去向。观察窗必须能看到转子。
- [ ] 实现分段镜头与交互提示；文本有足够阅读时间，不遮挡当前重点、互不叠加。英文fallback与中文翻译语义一致。
- [ ] 一组必要合同检查覆盖故事板/资源入口、真实模板几何及显示生命周期/文本间距，避免只逐句匹配实现或加入全量GameTest。
- [ ] 运行一次 `./gradlew.bat test --tests '*TurbinePonderContractTest' assemble`（PowerShell），保留真实日志。无新修改时不重复执行，不运行clean/full build/GameTest，不启动用户客户端。
- [ ] 交付未提交改动与 `docs/reviews/2026-10-08/turbine-ponder-04/implementation.md`；证据位于 `build/reports/extension/DEVICE-PONDER-04-TURBINE/`。报告列出写集、实际技能、验证结果、镜头安排及尚待人工播放，不修改其他docs。

## Task 2：审查与文档整理（PM主持）

- [ ] 一名独立审查者在相同候选检查需求与代码质量合并审查，重点看实际几何、Ponder生命周期、管路可见性和文案；复用Task1证据，不再跑同样测试。只写 `docs/reviews/2026-10-08/turbine-ponder-04/review.md`，问题由执行者整改。
- [ ] 实现审查完毕后，PM归档已完成和被替代的活动计划；活动入口仅保留当前任务与仍未实施的有效后续合同。历史验收与决策证据保存，弃用当前说明、重复状态段落删除。
- [ ] 精简 `docs/README.md`、`docs/project.md`、`docs/content-catalog.md`、`docs/implementation-roadmap.md`、首发扩展入口及治理的重复历史；统一当前完成、候选待播放和后置状态，不把新的汽轮机教学写成已验收。
- [ ] 归档后修正受影响内部链接，做一次Markdown链接/差异检查。无文档需要时不扩大到许可证、发布、用户存档、日志或工具缓存。
- [ ] PM窄范围中文提交，经审查的新教学留同级候选；主目录可同步当前文档，但未获播放确认前不合入本台功能。

## 人工播放门

在同级候选运行 `./gradlew.bat runClient`，从任一汽轮机构件按W：三个情景能打开并播完；搭建形态/端口和三档差异清楚，进排汽与双轴可见，文案无重叠且规则准确。人工确认前停在本台，不自动实施工作盆供热。
