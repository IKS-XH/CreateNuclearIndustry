# 机械动力：核工业

> **Create: Nuclear Industry** — 面向《机械动力》（Create）的核工业附属模组。

![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-62B47A)
![NeoForge 21.1.219](https://img.shields.io/badge/NeoForge-21.1.219-EA6A28)
![Create 6.0.10](https://img.shields.io/badge/Create-6.0.10-6F4E37)
![Java 21](https://img.shields.io/badge/Java-21-ED8B00)
![Development status](https://img.shields.io/badge/status-Alpha%20开发中-D9A441)
![License](https://img.shields.io/badge/license-All%20Rights%20Reserved-lightgrey)

《机械动力：核工业》希望在 Create 的机械美学和自动化语言中，构建一套可读、可控制、会发生真实因果故障的核工业玩法。玩家需要布置堆芯、调节控制棒、维持冷却剂回路、处理燃料与余热，并最终通过换热器、锅炉和汽轮机向 Create 应力网络提供大规模旋转动力。

本项目强调“看懂系统并搭出控制回路”，而不是随机事故或复杂查表。热量、流量、燃耗、损伤与安全联锁均由服务端权威结算，并通过 Create 风格交互和工程师护目镜呈现。

> [!WARNING]
> 当前版本为 `0.1.0` Alpha 开发快照，尚未发布稳定可玩版本。存档兼容、内容平衡、正式美术和生存流程都可能继续变化，请勿用于重要存档或生产服务器。

## 当前开发基线

P1 固定 `5×5×5` 实验反应堆已于 **2026-09-29 完成最终交接**，包括逐列热工/燃耗/损伤、控制棒与红石 SCRAM、真实 Create 冷热管网、玩家/机械臂换料、维修、护目镜遥测和基础 Ponder。融毁目前只有服务端事件占位符，具体事故后果后置。

客户端总验收由用户确认通过；本轮 261 项 JUnit、111 项 required GameTest 断言与 build 通过。GameTest 断言全过后保存挂起，强停导致 Gradle 退出 1，该限制未消除。详见[总验收](docs/archive/P1-VERIFY-02.md)和[最终交接](docs/archive/P1-VERIFY-03.md)。

三矿获取、粉碎与素材批，以及铅锡直熔、水洗9粒、粒锭合拆、压板和新素材，均已于2026-10-01完成人工验收并合入main，见[铅锡收尾记录](docs/reviews/2026-10-01/material-02-acceptance.md)。铁粉与煤/木炭粉→4+1搅拌成5钢粉→熔炼钢锭→现有钢板也已[验收并合入main](docs/reviews/2026-10-01/material-03b/README.md)，物品名现简称“钢”；旧遥测测试调度异常已修复，GameTest断言通过后的保存停滞单列为环境限制。[锡条、两种传感器及五项素材](docs/reviews/2026-10-01/material-04/ACCEPTANCE.md)已通过完整人工清单并合入main。材料05的石英粉、耐火砖、重型轴承和4项SVG也已[完整手测通过并合入main](docs/reviews/2026-10-02/material-05/ACCEPTANCE.md)，从主目录runClient即可使用。后续按[改动范围选择验证](docs/project-governance.md#51-按改动范围验证2026-10-02起生效)，不在推进前后重复全量回归。铀原料加工与两格富集离心机也已[完成人工验收并合入main](docs/reviews/2026-10-02/fuel-01/ACCEPTANCE.md)，主目录runClient即可使用；下一段为[生芯块与烧结炉方案](docs/superpowers/plans/2026-10-02-fuel-sintering-furnace-proposal.md)，[02A全部手测已通过](docs/reviews/2026-10-02/fuel-02a/README.md)，已交付[02B整格外观与工作台/高炉配方候选](docs/reviews/2026-10-03/fuel-02b/README.md)；只待这两项复测，本批暂只在同级候选目录启动。铅锡水洗副产物暂缓。完整生存生产和发电链尚未完成；当前任务见[实施路线图](docs/implementation-roadmap.md)。

## 设计方向

最终主机组计划形成以下闭环：

```text
燃料制造 → 实验/商用反应堆 → 热复合冷却剂 → 核换热器
                                                ├─→ 高压锅炉 → 超临界蒸汽 → 汽轮机 → Create 旋转应力
                                                └─→ Create 原生供热与蒸汽设备
```

首发目标能源是 Create 旋转应力（SU），不是 FE/RF。锅炉、核换热器、超临界汽轮机、完整燃料生产线、辐射系统、可变尺寸反应堆和烈焰人反应堆管理员均属于后续阶段，当前游戏内出现的占位内容不代表这些玩法已经完成。

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

反应堆离线数值模拟器位于 [`tools/reactor-simulator/`](tools/reactor-simulator/README.md)，用于验证堆芯布局、控制棒、冷却剂、损伤和融毁模型；它是开发工具，不是模组内 GUI。

## 项目文档

从[统一文档入口](docs/README.md)查看设计职责、当前任务、后置计划和历史交接；从[实施路线图](docs/implementation-roadmap.md)查看进度与下一步。

开工前阅读 [AGENTS.md](AGENTS.md)、[治理协议](docs/project-governance.md)及具体派发任务卡。项目经理维护需求、文档、验收与 Git；执行者在指定写集内实现并交付证据，不做 Git 写操作。技能、中文注释和中文提交规则以治理入口为准。

## 已知边界

- 当前没有稳定发布包，也不承诺开发存档向后兼容。
- P1 固定为 `5×5×5`；可变长宽高后置。
- 正式连接纹理、Blockbench 多边形模型、Flywheel 动画、粒子和音效在功能闭环后制作。
- 完整生存配方、矿物生成、核燃料生产线和乏燃料长期处理尚未完成。
- 锅炉、换热器、汽轮机和最终 SU 输出仍处于设计/后续实施阶段。
- 不实现受污染复合冷却剂、冷却剂净化器、工业仪表或独立 SCRAM 联锁器。

## 许可与声明

本仓库当前采用 **All Rights Reserved**。除非项目所有者另行书面授权，否则不得复制、修改、再分发或发布本项目的代码与资源。许可方案可能在正式发布前重新评估。

本项目是非官方 Minecraft 模组，与 Mojang Studios、Microsoft 或 Create 团队不存在隶属或认可关系。Minecraft 是 Mojang Studios 的商标；Create 及其相关资源归各自权利人所有。

## English summary

**Create: Nuclear Industry** is an in-development NeoForge 1.21.1 addon that brings reactor control, coolant circulation, fuel management, thermal damage and safety systems into Create's mechanical automation language. The current Alpha focuses on a fixed `5×5×5` experimental reactor. Boilers, heat exchangers, turbines, full survival progression and final rotational-power generation are planned for later milestones.
