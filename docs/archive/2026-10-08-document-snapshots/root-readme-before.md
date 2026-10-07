# 机械动力：核工业

> **Create: Nuclear Industry** — 面向《机械动力》（Create）的核工业附属模组。

![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-62B47A)
![NeoForge 21.1.219](https://img.shields.io/badge/NeoForge-21.1.219-EA6A28)
![Create 6.0.10](https://img.shields.io/badge/Create-6.0.10-6F4E37)
![Java 21](https://img.shields.io/badge/Java-21-ED8B00)
![Development status](https://img.shields.io/badge/status-Alpha%20开发中-D9A441)
![Code license](https://img.shields.io/badge/code-MPL--2.0-blue)
![Assets license](https://img.shields.io/badge/assets-All%20Rights%20Reserved-lightgrey)

《机械动力：核工业》希望在 Create 的机械美学和自动化语言中，构建一套可读、可控制、会发生真实因果故障的核工业玩法。玩家需要布置堆芯、调节控制棒、维持冷却剂回路、处理燃料与余热，并最终通过换热器、锅炉和汽轮机向 Create 应力网络提供大规模旋转动力。

本项目强调“看懂系统并搭出控制回路”，而不是随机事故或复杂查表。热量、流量、燃耗、损伤与安全联锁均由服务端权威结算，并通过 Create 风格交互和工程师护目镜呈现。

> [!WARNING]
> 当前版本为 `0.1.0` Alpha 开发快照，尚未发布稳定可玩版本。存档兼容、内容平衡、正式美术和生存流程都可能继续变化，请勿用于重要存档或生产服务器。

## 当前开发基线

**2026-10-06待手测候选：** [轻量乏燃料封存](../../reviews/2026-10-06/store-01/CANDIDATE.md)已完成实现、必要自动验证和联合审查，默认一次装桶、单格架存16桶。新功能尚未合入main；请从同级`Create_NuclearIndustry-ore-acquisition`启动，按一张联合清单集中手测。

**2026-10-05最新验收：** 用户确认所有测试项通过，三档汽轮机的搭建、薄壳/叶轮/观察窗、共享双轴、流量效率与微周转，冷凝回水闭环及护目镜显示均已验收合入main，见[联合验收](../../reviews/2026-10-05/condense-01/ACCEPTANCE.md)。主目录可运行`.\gradlew.bat runClient`；同级候选及原测试世界保留。下一主线为乏燃料基础封存。

**2026-10-04最新验收：** 高压锅炉首期、5×5×5/九换热段、独立多端口、换热器定向共享直列及护目镜中文已全部手测通过并合入main，见[本批验收](../../reviews/2026-10-04/boiler-01d/ACCEPTANCE.md)。主目录runClient可用；本次仅增量assemble，复用候选定向验证。下一主线为超临界汽轮机及冷凝回水。

P1 固定 `5×5×5` 实验反应堆已于 **2026-09-29 完成最终交接**，包括逐列热工/燃耗/损伤、控制棒与红石 SCRAM、真实 Create 冷热管网、玩家/机械臂换料、维修、护目镜遥测和基础 Ponder。融毁目前只有服务端事件占位符，具体事故后果后置。

当时客户端总验收由用户确认通过，记录了261项JUnit、111项required GameTest断言及build通过；该轮GameTest保存挂起、强停导致Gradle退出1，作为历史运行器限制保留，不等同于本批运行结果。详见[总验收](../P1-VERIFY-02.md)和[最终交接](../P1-VERIFY-03.md)。

三矿获取、粉碎与素材批，以及铅锡直熔、水洗9粒、粒锭合拆、压板和新素材，均已于2026-10-01完成人工验收并合入main，见[铅锡收尾记录](../../reviews/2026-10-01/material-02-acceptance.md)。铁粉与煤/木炭粉→4+1搅拌成5钢粉→熔炼钢锭→现有钢板也已[验收并合入main](../../reviews/2026-10-01/material-03b/README.md)，物品名现简称“钢”；旧遥测测试调度异常已修复，GameTest断言通过后的保存停滞单列为环境限制。[锡条、两种传感器及五项素材](../../reviews/2026-10-01/material-04/ACCEPTANCE.md)和[石英粉、耐火砖、重型轴承及4项SVG](../../reviews/2026-10-02/material-05/ACCEPTANCE.md)均已完成人工验收并合入main。

[铀原料加工与两格富集离心机](../../reviews/2026-10-02/fuel-01/ACCEPTANCE.md)、[生芯块与整格燃料烧结炉](../../reviews/2026-10-03/fuel-02b/ACCEPTANCE.md)、[装配材料02C](../../reviews/2026-10-03/fuel-02c/ACCEPTANCE.md)及[八格屏蔽装配台02E](../../reviews/2026-10-03/fuel-02e/ACCEPTANCE.md)功能手测均通过并合入main，从主目录runClient即可使用。02E模型共面闪烁已修复并通过几何/打包核对，修后画面未再次人工确认；[青金石粉与无热冷却剂01B、固定实验堆生存制造01](../../reviews/2026-10-03/reactor-01/ACCEPTANCE.md)已全部手测通过并合入main，补齐7种材料与16条反应堆制造配方。模型活动件已拆分，动画另批；按[改动范围验证](../../project-governance.md#51-按改动范围验证2026-10-02起生效)，不重复全量回归。铅锡水洗副产物暂缓，完整生存生产和发电链尚未完成，见[实施路线图](../../implementation-roadmap.md)。

## 设计方向

最终主机组计划形成以下闭环：

```text
燃料制造 → 实验/商用反应堆 → 热复合冷却剂 → 核换热器
                                                ├─→ 高压锅炉 → 超临界蒸汽 → 汽轮机 → Create 旋转应力
                                                └─→ Create 原生供热与蒸汽设备
```

首发目标能源是Create旋转应力（SU）。基础燃料生产、核换热器供热、专用锅炉、三档汽轮机及蒸汽冷凝回水已完成现行验收；乏燃料基础封存、完整温压机制、辐射系统、可变尺寸反应堆/锅炉及烈焰人反应堆管理员分别按后续计划推进。各设备当前实现范围以活动计划和验收记录为准。

## 版本与依赖

| 组件 | 当前基线 |
| :--- | :--- |
| Minecraft | `1.21.1` |
| Java | `21` |
| NeoForge | `21.1.219` |
| Create | `6.0.10-280` |
| Ponder | `1.0.82` |
| Flywheel | `1.0.6` |
| Mod ID | `create_nuclear_industry` |
| 当前版本 | `0.1.0` |

本项目目前只面向 NeoForge 1.21.1。Fabric、旧版 Forge、多加载器和旧 Minecraft 版本不属于当前支持范围。

## 从源码运行

需要安装 Java 21。仓库包含 Gradle Wrapper，无需单独安装 Gradle。

Windows：

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

Linux / macOS：

```bash
./gradlew build
./gradlew runClient
```

构建成功后，开发 JAR 位于 `build/libs/`。

开发客户端 `runClient`、`runClientA`、`runClientB` 自动加载 JEI，版本固定在 `gradle.properties` 的 `jei_version`。JEI 仅用于开发时查看物品和配方，不进入服务端/GameTest/JUnit、发布依赖或模组 JAR。配置变更后需重启开发客户端。三矿与最终素材已在 main 生效；下一批未验收功能继续在同级隔离工作树测试，具体位置见任务卡。

常用验证命令：

```powershell
.\gradlew.bat test --rerun-tasks --max-workers=1
.\gradlew.bat runGameTestServer --rerun-tasks --max-workers=1
```

反应堆离线数值模拟器位于 [`tools/reactor-simulator/`](../../../tools/reactor-simulator/README.md)，用于验证堆芯布局、控制棒、冷却剂、损伤和融毁模型；它是开发工具，不是模组内 GUI。

## 项目文档

从[统一文档入口](../../README.md)查看设计职责、当前任务、后置计划和历史交接；从[实施路线图](../../implementation-roadmap.md)查看进度与下一步。

开工前阅读 [AGENTS.md](../../../AGENTS.md)、[治理协议](../../project-governance.md)及具体派发任务卡。项目经理维护需求、文档、验收与 Git；执行者在指定写集内实现并交付证据，不做 Git 写操作。技能、中文注释和中文提交规则以治理入口为准。

## 已知边界

- 当前没有稳定发布包，也不承诺开发存档向后兼容。
- 当前反应堆与高压锅炉均固定为`5×5×5`；两者后期都支持限定范围内自由选择长宽高，具体范围待确认。
- 正式连接纹理、Blockbench 多边形模型、Flywheel 动画、粒子和音效在功能闭环后制作。
- 基础矿物、材料、燃料组件与现有设备制造已可生存获取；乏燃料基础封存和复杂再处理尚未完成。
- 核换热器供Create原生锅炉、专用锅炉核热产汽、三档超临界汽轮机及蒸汽冷凝闭环已实现并验收；完整温压机制后置。
- 不实现受污染复合冷却剂、冷却剂净化器、工业仪表或独立 SCRAM 联锁器。

## 许可与声明

本项目采用 **代码开源、美术资源单独授权** 的许可方式。除文件或目录另有许可声明、或属于第三方内容外，本项目原创程序代码（包括模组代码、测试、构建脚本及开发工具代码）采用 **Mozilla Public License 2.0（MPL-2.0）**，完整条款见 [MPL-2.0 官方文本](https://www.mozilla.org/en-US/MPL/2.0/)。

你可以依照 MPL-2.0 使用、修改和分发代码，包括商业使用。对外分发源码或编译后的修改版时，受 MPL 覆盖的代码文件及其修改必须继续按 MPL 提供源码，并保留许可与版权声明；分发编译包时须告知接收者获取对应源码的方式。独立新增且不含受 MPL 覆盖代码的文件可采用其他许可，私下使用或修改不要求公开源码。以上为便于阅读的摘要，具体权利和义务以协议全文为准。

本项目原创贴图、模型及其源文件、音效和其他美术资源采用 **All Rights Reserved（保留所有权利）**，不属于上述代码开源范围。允许玩家使用官方模组包，并允许整合包作者在保留本项目署名和许可声明的前提下，收录及分发未经修改的官方模组包。除此之外，未经相关权利人另行授权，不得单独提取、修改或在其他项目及修改版中再分发这些资源；这些资源限制不改变 MPL 对代码授予的权利。

第三方代码、资源及依赖继续遵守各自原有许可，本声明不替代或扩大其授权。贡献代码前须确认可按 MPL-2.0 授权；贡献贴图、模型、音效等资源前须明确署名、来源及使用授权范围，贡献不自动转移著作权。

本项目大量采用agent生成内容，其中包括程序代码、动画建模、贴图素材。

本项目是非官方 Minecraft 模组，与 Mojang Studios、Microsoft 或 Create 团队不存在隶属或认可关系。Minecraft 是 Mojang Studios 的商标；Create 及其相关资源归各自权利人所有。

## English summary

**Create: Nuclear Industry** is an in-development NeoForge 1.21.1 addon that brings reactor control, coolant circulation, fuel management, thermal damage and safety systems into Create's mechanical automation language. The current Alpha focuses on a fixed `5×5×5` experimental reactor. Boilers, heat exchangers, turbines, full survival progression and final rotational-power generation are planned for later milestones.
