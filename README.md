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

本项目采用 **代码开源、美术资源单独授权** 的许可方式。除文件或目录另有许可声明、或属于第三方内容外，本项目原创程序代码（包括模组代码、测试、构建脚本及开发工具代码）采用 **Mozilla Public License 2.0（MPL-2.0）**，完整条款见 [MPL-2.0 官方文本](https://www.mozilla.org/en-US/MPL/2.0/)。

你可以依照 MPL-2.0 使用、修改和分发代码，包括商业使用。对外分发源码或编译后的修改版时，受 MPL 覆盖的代码文件及其修改必须继续按 MPL 提供源码，并保留许可与版权声明；分发编译包时须告知接收者获取对应源码的方式。独立新增且不含受 MPL 覆盖代码的文件可采用其他许可，私下使用或修改不要求公开源码。以上为便于阅读的摘要，具体权利和义务以协议全文为准。

本项目原创贴图、模型及其源文件、音效和其他美术资源采用 **All Rights Reserved（保留所有权利）**，不属于上述代码开源范围。允许玩家使用官方模组包，并允许整合包作者在保留本项目署名和许可声明的前提下，收录及分发未经修改的官方模组包。除此之外，未经相关权利人另行授权，不得单独提取、修改或在其他项目及修改版中再分发这些资源；这些资源限制不改变 MPL 对代码授予的权利。

第三方代码、资源及依赖继续遵守各自原有许可，本声明不替代或扩大其授权。贡献代码前须确认可按 MPL-2.0 授权；贡献贴图、模型、音效等资源前须明确署名、来源及使用授权范围，贡献不自动转移著作权。

本项目大量采用agent生成内容，其中包括程序代码、动画建模、贴图素材。

本项目是非官方 Minecraft 模组，与 Mojang Studios、Microsoft 或 Create 团队不存在隶属或认可关系。Minecraft 是 Mojang Studios 的商标；Create 及其相关资源归各自权利人所有。

## English summary

**Create: Nuclear Industry** is an in-development NeoForge 1.21.1 addon for fuel production, variable-size reactors (5–16 blocks per side), thermal boilers, three turbine sizes, dedicated fluid transport and spent-fuel storage. A Blaze Burner administrator provides a three-page GUI, power regulation, stress-reserve control and save/resume continuity. Eight basic device tutorials are accepted; radiation, concrete accident effects, pressure transport and reprocessing remain future work. See the [current status](docs/STATUS.md) for separate candidate and visual-review gates.
