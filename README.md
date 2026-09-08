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

目前正在完成 P1 固定实验反应堆核心切片，已经具备并通过对应自动化或客户端验收的主要能力包括：

- 固定 `5×5×5` 实验反应堆，以及由“燃料列 / 控制棒列 / 空列”任意组合的 `3×3` 堆芯平面。
- 服务端权威的逐列裂变、燃耗、四向反馈超频、冷却、余热、完整度损伤、热量传播和融毁倒计时模型。
- Create 风格控制棒浮动滑块；所有玩家均可操作，不引入身份、权限或所有权差异。
- 通过唯一 `reactor_instrument_port` 接收红石高电平 SCRAM，低电平恢复停堆前控制棒目标位置。
- 冷态与热态复合冷却剂、共享冷热库存、冷转热结算，以及 Create 储罐、动力泵和流体管道接入。
- 多个冷端和热端独立提供吞吐量：默认每个物理端口 `128 mB/t`，不设置全堆流量上限。
- 每个换料端口独立保存一个完整燃料组件 `ItemStack`；支持玩家与 Create 机械臂原子换料、未成型时的安全取料锁，以及燃尽后的冷却乏燃料组件。
- 使用 `steel_plate`（合金钢板）逐次维修燃料列和控制棒列；维修不会补充燃料、清除余热或回退融毁进度。
- 工程师护目镜分层显示：仪表端口显示结构和全堆运行摘要，换料端口与控制棒驱动器显示对应列详情。
- 完整度归零且燃料未耗尽的失效燃料列继续发热、燃耗和传播；失效覆盖达到阈值后建立可暂停、继续并持久化的融毁倒计时。
- NBT 版本迁移、服务器配置、JUnit 与 NeoForge GameTest 回归，以及通过人工验收的离线 HTML 反应堆数值模拟器。

当前自动化基线为 **217 项 JUnit、89 个 required GameTest 和完整构建通过**。换料端口燃料所有权、未成型安全取料、Create 机械臂换料及燃料列/控制棒列维修均已完成客户端人工验收。`P1-MELTDOWN-01` 已完成融毁覆盖与倒计时闭环，但只产生事故完成信号，不会破坏世界或生成事故产物。

P1 当前的下一道实施门是冻结 `P1-MELTDOWN-02` 事故产物表，包括高放射性废物的注册 ID、形态和数量、热复合冷却剂喷出量以及结构破坏范围。完成幂等事故执行器后，才会继续危险状态拆除、完全停机、破坏性重组成型、完整 Ponder 教学和最终端到端验证。

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

构建成功后，开发 JAR 位于 `build/libs/`。常用验证命令：

```powershell
.\gradlew.bat test --rerun-tasks --max-workers=1
.\gradlew.bat runGameTestServer --rerun-tasks --max-workers=1
```

反应堆离线数值模拟器位于 [`tools/reactor-simulator/`](tools/reactor-simulator/README.md)，用于验证堆芯布局、控制棒、冷却剂、损伤和融毁模型；它是开发工具，不是模组内 GUI。

## 项目文档

- [项目经理职责、技能要求与版本管理协议](docs/project-governance.md)
- [项目定位、玩法规则与阶段验收](docs/project.md)
- [实施路线图与当前进度](docs/implementation-roadmap.md)
- [反应堆局部控制与状态模型](docs/reactor-local-control-revision-design.md)
- [注册内容与素材清单](docs/content-catalog.md)
- [配方、材料关系与守恒约束](docs/recipes.md)
- [当前 P1 单步执行计划](docs/superpowers/plans/2026-08-18-agent-developer-execution-plan.md)
- [历史计划与验收报告](docs/archive/README.md)

开发者和自动化执行者开始工作前必须阅读 [`AGENTS.md`](AGENTS.md)、[项目治理与协作协议](docs/project-governance.md) 和活动任务计划。用户指定的项目经理 Codex 负责玩法与技术讨论、可行性评审、文档、任务计划、验收及版本管理，不编写实现代码；其他 Agent 和开发者只按任务卡实施，禁止暂存、提交、回退、分支、合并等 Git 写操作。项目经理与执行者均须使用任务相关的已安装 Minecraft 技能。新增或修改的手写代码使用中文注释，Git 提交信息统一使用中文。

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
