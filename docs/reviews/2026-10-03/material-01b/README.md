# EXT-A-MATERIAL-01B：候选交付与三项客户端验收

**状态：实现、定向验证与审查通过，等待人工验收，运行内容尚未合入main。** 用户于2026-10-03确认“不需要加热，其他参数按推荐的来”。[任务卡](../../../superpowers/plans/2026-10-03-ext-a-material-01b.md)与[完整方案](../../../superpowers/plans/2026-10-03-coolant-production-proposal.md)记录最终合同。

**后续授权：** 用户随后要求睡醒后一起手测，数值设定先采用推荐方案。01B仍未人工验收；接续的固定实验堆制造01已完成候选交付，本页三项清单保留并并入[两批交接](../reactor-01/README.md)，不再将其作为该批实施前的暂停点。

## 玩家可用内容

- 粉碎轮：1青金石→1青金石粉，无副产物。磨石保持Create原有蓝色染料路线。
- 无热动力搅拌：青金石粉、红石粉、荧石粉各1份＋1000mB水→1000mB复合冷却剂，无副产物。两道加工参数均为100，实际速度仍由Create决定。
- 沿用已有冷态流体、原生桶及此前恢复并验收的冷却剂贴图。青金石粉沿用已认可16×16 SVG产物，支持具体粉末标签的等价成员；没有新增机器、GUI或反应堆机制。

## 启动与人工清单

完全退出旧客户端，再从同级候选目录启动：

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
.\gradlew.bat runClient
```

| 待验项目 | 操作与预期 |
| :--- | :--- |
| 1. JEI和显示 | 查询青金石粉及冷却剂配方；名称、粉末图标正常，两道配方数量正确，搅拌无加热要求。 |
| 2. 青金石加工 | 1青金石经过粉碎轮得到1粉；磨石仍产蓝色染料。 |
| 3. 无热制液与取液 | 不放燃烧室，将三粉各1和1000mB水投入有动力的搅拌器/工作盆；产出1000mB冷却剂，用现有桶或管道正常取走。 |

当前仅等待这三项，不重复02E功能清单，不提前推进反应堆制造、热端或封存。主目录`runClient`已有02E及模型R2修复，但尚无本批新物品和配方。

## 实现、审查与证据

- 运行内容提交：`bae4dba`，分支`codex/ore-acquisition`；`a2df303`恢复历史素材清单。其后仅同步文档，运行实现、依赖和JAR未变，按治理5.1复用同一组证据。
- 实施者`coolant_01b_impl`、审查者`coolant_01b_review`均使用高速模型`gpt-6-luna`、high思考；PM负责任务、交付审核与Git，没有代写实现。
- [实施原报告](./EXT-A-MATERIAL-01B.md)、[独立审查](./EXT-A-MATERIAL-01B-REVIEW.md)记录实际应用的Minecraft模组、测试与资源技能，以及开发期间修正的失败。PM在交付核对发现旧导出器固定数量约束，已要求保留原manifest，仅单独接入认可PNG，避免破坏历史导出入口；不扩大共用管线改造。
- [最终GameTest输出](./evidence/gametest-final.log)：6/6 required通过，正常保存退出、Gradle退出0。覆盖真实粉碎轮、磨石、无燃烧室搅拌守恒、输出满时保料及恢复、配方/标签和模拟外部标签重载。
- [相关JUnit XML](./evidence/p1-data-contract-results.xml)：`P1DataContractTest` 5/5通过。旧测试仅精确放行已批准的02E组件配方及本批冷态配方；该修正单独提交`b61fae5`并同步main，未提前合入01B运行内容。
- [增量assemble输出](./evidence/assemble-final.log)：退出0；核对新物品、标签、配方与原生桶资源均正确打包。没有再跑全量测试或客户端。
- 临时外部标签探针只模拟`minecraft:flint`加入`c:dusts/lapis`，不是第三方模组实测。复跑该GameTest前将[两文件数据包](./evidence/simulated-external-datapack/pack.mcmeta)放入`build/gametest-coolant-production/world/datapacks/coolant-compat-pack/`；完整相对路径与内容见实施报告。测试依次禁用、启用、禁用并重载，最后确认成员移除。fixture未进入发布资源或用户存档。
- 测试追加的根目录两份受跟踪日志已由PM保存到候选`build/reports/extension/EXT-A-MATERIAL-01B/evidence/root-logs/`后恢复，未夹带提交。主工程用户`.vscode/launch.json`保持原改动。

JAR：候选`build/libs/create_nuclear_industry-0.1.0.jar`，SHA-256：

```text
67A2F0F39278DCD5B27F88ECC4A1B61FEF20CFF7BFCCCEDD3C921E272F6D1880
```

青金石粉PNG与认可产物一致，SHA-256为`7011BC285C26C756D40AE7087B47022C3C9FD5002EC03F349A67E15D1475CE09`；本批前113张既有游戏PNG均未变化。

## 前置模型修复边界

[02E最终验收](../fuel-02e/ACCEPTANCE.md)记录用户全部功能手测通过。R2已裁除共面重叠区域并微调旋转端盖；实际旋转几何、物品模型及运动端点扫描没有剩余重叠，模型打包核对通过，已合入main。修后GPU画面未再次人工确认；用户已明确要求修复后直接开始下一步，因此没有新设重复02E人工门，也不将离线检查写成游戏内视觉通过。
