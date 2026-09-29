# EXT-A-MATERIAL-01A：青金石粉物品与通用标签基础

**状态：** 待验收，存在整改/复验阻塞；执行者 Boyle（`01a0d22c-f59a-71b2-a0ad-7184181cbeb9`）已交付并停止，2026-09-28 PM 为换机保存候选后关闭执行者，未宣布验收通过。

**前置更新（2026-09-29）：** `P1-VERIFY-02/03` 已通过 PM 验收，P1 交接前置解除；本卡自己的完整 GameTest、纹理整改/评审及客户端检查仍待完成。下文换机时“合并等待 P1”是历史状态，不再作为当前阻塞，但不能因此把本候选直接记为验收通过或合并。

**换机检查点（2026-09-28）：** 用户要求先提交并推送。候选提交 `8996fa78e76fd0ce4227763db7da9b103d32a518` 位于 `codex/ext-a-material-01a`，未合并；报告及证据归档位于候选分支 `docs/handoffs/2026-09-28/`。最终历史 XML 265 项全通过，PM 本次已复算；构建为执行者历史成功结果，本次未重跑。完整 GameTest 受运行器异常阻塞，1254×1254 纹理与客户端外观仍待处理。自动日志先归档后由 PM 定向恢复。新电脑从主分支[换机交接](../../handoffs/2026-09-28/README.md)恢复任务，不将检查点提交当作合并或验收许可。
**目标：** 注册唯一的 `create_nuclear_industry:lapis_dust`，在创造标签页显示“青金石粉 / Lapis Dust”，加载有效物品模型和纹理，并正确加入具体及父粉末标签。本卡不提供生存配方。
**需求与授权：** D-02 已批准物品身份及 `c:dusts/lapis`、`c:dusts` 合同；用户于 2026-09-24 在纠正重复确认后明确要求“开始让执行者执行吧”。PM 据此拆出不依赖加工数值的基础实现，允许在隔离候选中开发；这是本小卡的排程调整，不是免除 P1 客户端验收或授权其余生产实现。合并主线仍等待 P1 交接，不改变 P1 测试工作树。
**设计入口：** `docs/recipes.md` 标签合同、生产准备计划 2.2 节、本卡及 `AGENTS.md`、`docs/project-governance.md`、活动 P1 计划。D-03e 候选不得作为本卡已批准参数。
**架构：** 沿用 DeferredRegister 注册普通物品、现有创造页、1.21.1 模型与语言资源；用数据包标签建立兼容入口。普通物品无新增状态、网络、配置或方块实体。
**技术栈：** Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6，All Rights Reserved，不升级依赖。
**基线：** 功能源自 `3c0b7cd`；PM 将本卡提交后创建原生管理的独立工作树，执行者记录实际路径和 HEAD；不得在主工作区实现。

**实际派发：** 原生工作树 `C:/Users/lenovo/.codex/worktrees/ext-a-material-01a/CreateNuclearIndustry`，PM 创建分支 `codex/ext-a-material-01a`，任务卡与代码基线 `c8b8f8e2bc938d5c74a9b76f87eae087b2ce765d`。开工前 Git 干净；本段仅由主工作区 PM 补记，执行者无须同步 Git 或修改核心文档。

## 角色与技能

执行者不是项目经理，不能改核心文档、派发他人、宣布验收或执行任何 Git 写操作；仅交付未提交修改和报告。中文注释按 AGENTS.md；技能中的提交/建分支步骤不适用。

实际读取并应用：

- `C:/Users/lenovo/.codex/skills/minecraft-modding/SKILL.md`
- `C:/Users/lenovo/.codex/skills/minecraft-testing/SKILL.md`
- `C:/Users/lenovo/.codex/skills/minecraft-resource-pack/SKILL.md`
- 如制作新纹理，读取 `C:/Users/lenovo/.codex/skills/minecraft-imagegen/SKILL.md`，按其生成图像流程执行；无法使用时报告资源阻塞，不借用原青金石/蓝染料图标冒充成品粉末。

## 精确允许写集（相对独立工作树根目录）

- `src/main/java/com/iksxh/create_nuclear_industry/content/ModItems.java`：仅增加 `LAPIS_DUST` 普通物品及必要职责注释，不改变已有注册。
- `src/main/java/com/iksxh/create_nuclear_industry/content/ModCreativeTabs.java`：增加上述物品展示。
- `src/main/resources/assets/create_nuclear_industry/models/item/lapis_dust.json`
- `src/main/resources/assets/create_nuclear_industry/textures/item/lapis_dust.png`
- `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`、`en_us.json`：仅追加该物品语言条目。
- `src/main/resources/data/c/tags/item/dusts/lapis.json`、`src/main/resources/data/c/tags/item/dusts.json`：追加具体标签及父引用。
- `src/test/java/com/iksxh/create_nuclear_industry/LapisDustDataContractTest.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/LapisDustGameTests.java`
- `build/reports/extension/EXT-A-MATERIAL-01A.md` 及同名证据目录；运行命令正常生成的忽略构建/测试输出允许保留。

禁止修改配方、P1ContentIds、已有测试断言、构建/依赖、配置、治理与核心文档、反应堆功能、模拟器、其他资源。需要写集外修改先向 PM 报告，不能自行扩大任务。

## 行为合同与验证重点

1. `lapis_dust` 为普通可堆叠物品，沿用默认普通物品堆叠行为，不携带燃料耐久、热量或核安全状态，不增加别名。
2. 具体标签仅追加该物品，父 `c:dusts` 通过 `#c:dusts/lapis` 引用纳入；不覆盖生态标签、不把整颗青金石、蓝染料或燃料粉混入。不能把已批准新增的标签称为 NeoForge 内置。
3. 无第三方材料模组时注册、资源与标签正常加载。已有红石/荧石等父标签成员应保留；用运行时标签检查验证追加关系，而非仅检查 JSON 字符串。
4. 模型采用当前版本 `models/item` 资源路径和 generated 物品模型；纹理为透明背景、蓝色粉末形态，遵循项目现有像素风格。按实际纹理尺寸留证，至少验证 PNG 可读、非空及模型引用有效；真实客户端外观未检查必须明示。
5. 本卡没有粉碎或冷却剂配方，不证明加工可达性、外部等价粉末实际配方匹配或完整生产链通过。已有 P1 生存配方禁令及冷热互转禁令不需调整。

## 执行步骤与验收

- [ ] 核对工作树、HEAD、干净状态、版本和技能；先运行 `./gradlew.bat test --max-workers=1`，记录接单基线。基础失败先报告，不掩盖。
- [ ] 增加必要的资源合同和真实注册/标签 GameTest：正式 ID 可取、物品属性、具体/父标签成员、错误形态不在具体标签、已有父标签成员保留；使用现有空模板与 GameTest 注册方式，不新建框架。测试不得只搜索实现源字符串来证明注册成功。
- [ ] 按允许写集实现物品、展示、语言、模型/纹理和标签。源码与测试的手写注释使用中文；不改既有功能。
- [ ] 运行 `./gradlew.bat test --max-workers=1`、`./gradlew.bat runGameTestServer --rerun-tasks --max-workers=1`、`./gradlew.bat build --max-workers=1`、`git diff --check`，保存完整日志与 XML 计数。基线参考为 261 JUnit、111 required GameTest，新增项单列，不把历史数量当本轮结果。
- [ ] GameTest 全部成功后如进程卡在保存，分开记录测试结果和退出问题；不得用重跑覆盖可复现失败，不随意强杀其他 Java 进程。不得启动客户端或操作用户存档；本工作树仅运行自动测试世界。
- [ ] 报告实际技能、基线、修改清单、测试命令/结果、纹理来源及查看证据、未测项、兼容影响和最终 Git 状态，交 PM 验收后停止。需要人工看图或游戏内显示时标为待验，不能宣称已目视通过。

PM 负责验收、候选提交和后续合并。独立候选即使自动测试通过，也不解除 P1 客户端门或授权下一生产配方；若资源/运行环境受阻，保留已完成实现和证据，报告确切阻塞。
