# 机械动力：核工业

> **Create: Nuclear Industry** — 面向《机械动力》（Create）的核工业附属模组。

《机械动力：核工业》希望在 Create 的机械美学和自动化语言中，构建一套可读、可控制、会发生真实因果故障的核工业玩法。玩家需要布置堆芯、调节控制棒、维持冷却剂回路、处理燃料与余热，并最终通过换热器、锅炉和汽轮机向 Create 应力网络提供大规模旋转动力。

本项目强调“看懂系统并搭出控制回路”，而不是随机事故或复杂查表。热量、流量、燃耗、损伤与安全联锁均由服务端权威结算，并通过 Create 风格交互和工程师护目镜呈现。

> [!WARNING]
> 当前版本为 `0.1.0` Alpha 开发快照，尚未发布稳定可玩版本。存档兼容、内容平衡、正式美术和生存流程都可能继续变化，请勿用于重要存档或生产服务器。

## 当前开发基线

基础材料/燃料制造、5～16格可变反应堆、核热与蒸汽回水、三档汽轮机、温压锅炉、专用流体管网、乏燃料封存及反应堆管理员已按相应范围验收合入main。管理员提供三页GUI、统一调棒、核热值班、恒功率和保存续接；完整范围、制品与未闭门见[当前状态](docs/STATUS.md)。

八类设备基础思索和换热器对工作盆/烧结炉的持续供热已通过各自手测。最新离心机外观、屏蔽装配台外观及动画已合入；新动画游戏内观察仍独立。锅炉16³尺寸上限与锅炉外观已通过手测，尺寸变更已合入main。

## 设计方向

最终主机组计划形成以下闭环：

```text
燃料制造 → 实验/商用反应堆 → 热复合冷却剂 → 核换热器
                                                ├─→ 高压锅炉 → 超临界蒸汽 → 汽轮机 → Create 旋转应力
                                                └─→ Create 原生供热与蒸汽设备
```

首发目标能源是Create旋转应力（SU）。热液/SC走专用工业管网，Create储罐保留；管网压力、事故世界效果、辐射与复杂再处理后置。规则见[项目设计](docs/project.md)，后续见[路线图](docs/implementation-roadmap.md)。

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

构建成功后，开发JAR位于build/libs；[正式开发JAR](build/libs/create_nuclear_industry-0.1.0.jar)已跟踪Git，版本身份见[STATUS](docs/STATUS.md)。

开发客户端 `runClient`、`runClientA`、`runClientB` 自动加载 JEI，版本固定在 `gradle.properties` 的 `jei_version`。JEI 仅用于开发时查看物品和配方，不进入服务端/GameTest/JUnit、发布依赖或模组 JAR。配置变更后需重启开发客户端。三矿与最终素材已在 main 生效；下一批未验收功能继续在同级隔离工作树测试，具体位置见任务卡。

开发验证按[改动范围](docs/project-governance.md#51-按改动范围验证2026-10-02起生效)选择定向测试与增量打包，沿用已审证据；全量JUnit/GameTest只在明确风险触发时运行。

反应堆离线数值模拟器位于 [`tools/reactor-simulator/`](tools/reactor-simulator/README.md)，用于验证堆芯布局、控制棒、冷却剂、损伤和融毁模型；它是开发工具，不是模组内 GUI。

## 项目文档

从[统一文档入口](docs/README.md)查看设计职责、当前任务、后置计划和历史交接；从[实施路线图](docs/implementation-roadmap.md)查看进度与下一步。

开工前阅读 [AGENTS.md](AGENTS.md)、[治理协议](docs/project-governance.md)及具体派发任务卡。项目经理维护需求、文档、验收与 Git；执行者在指定写集内实现并交付证据，不做 Git 写操作。技能、中文注释和中文提交规则以治理入口为准。

## 已知边界

- 当前没有稳定发布包，也不承诺开发存档向后兼容。
- 反应堆和高压锅炉默认三边均为5～16，允许长方体及跨区块；汽轮机三档固定规格。
- 已有设备美术与部分动画已接入，剩余动画/声音/粒子按各自批次验收。
- 基础矿物、材料、燃料组件、现有设备制造和乏燃料基础封存已可生存获取；复杂再处理尚未实现。
- 核换热器供Create原生锅炉、专用锅炉核热产汽、三档超临界汽轮机及蒸汽冷凝闭环已实现并验收；温压、双汽库存及工作盆/烧结炉持续核热已实现并验收。
- 不实现受污染复合冷却剂、冷却剂净化器、工业仪表或独立 SCRAM 联锁器。

## 许可与声明

本项目原创代码、文档，以及 **除 Logo 外的原创美术素材** 统一采用 **MIT License**，完整条款见 [LICENSE](LICENSE)，具体范围与例外见 [NOTICE.md](NOTICE.md)。美术素材包括贴图、模型、动画、音效及其可编辑源文件。

你可以依照 MIT 使用、复制、修改、分发和商业使用这些内容；分发副本或实质性部分时须保留版权及许可声明，不要求公开修改后的源码。以上为阅读摘要，以 [MIT 协议全文](LICENSE)为准。

**Logo 保留所有权利，不适用 MIT。** 包括游戏图标 `mod_logo.png`、像素与高清 Logo 源文件、各尺寸导出及其在宣传图中的 Logo 部分。允许玩家使用官方模组包，也允许整合包在保留署名和许可声明的前提下收录、分发未经修改的官方模组包；Logo 的其他使用、修改或单独再分发需另行授权。具体路径见 [NOTICE.md](NOTICE.md)。Logo 导出工具的程序代码仍为 MIT。

第三方代码、资源及依赖继续遵守各自原有许可，本声明不替代或扩大其授权。工业管泵阀中使用的 Create 原模型与调色派生贴图不因此变为 MIT，原许可和来源保留在 [第三方声明](NOTICE.md#third-party-content-retains-its-original-terms)。贡献代码或非 Logo 原创素材前须确认有权按 MIT 授权，并保留来源与署名；贡献不自动转移著作权。

Original project code, documentation and non-Logo artwork are licensed under MIT. The Logo is All Rights Reserved; third-party content retains its original terms. See [LICENSE](LICENSE) and [NOTICE.md](NOTICE.md).

本项目大量采用agent生成内容，其中包括程序代码、动画建模、贴图素材。

本项目是非官方 Minecraft 模组，与 Mojang Studios、Microsoft 或 Create 团队不存在隶属或认可关系。Minecraft 是 Mojang Studios 的商标；Create 及其相关资源归各自权利人所有。

## English summary

**Create: Nuclear Industry** is an in-development NeoForge 1.21.1 addon for fuel production, variable-size reactors (5–16 blocks per side), thermal boilers, three turbine sizes, dedicated fluid transport and spent-fuel storage. A Blaze Burner administrator provides a three-page GUI, power regulation, stress-reserve control and save/resume continuity. Eight basic device tutorials are accepted; radiation, concrete accident effects, pressure transport and reprocessing remain future work. See the [current status](docs/STATUS.md) for separate candidate and visual-review gates.
