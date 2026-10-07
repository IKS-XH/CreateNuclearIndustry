# 核换热器首期：人工验收与主工程合入

**状态：2026-10-04用户确认本批手动测试通过，候选已合入main。** 运行候选为`c002fb5`，合并提交为`b6f323c`。

## 验收范围

用户原话：“手动测试通过了，接下来做什么”。对应[01C最新合并清单](./README.md#合并人工复测)的三组运行复测：原装置冷热总量、低流量输出、断供/堵塞及保存恢复。本轮据此解除核换热器当前人工门；不扩展为尚未实现的工作盆加热、专用锅炉、蒸汽/汽轮机、冷凝或基础封存验收，也不凭这句话新增一次制造配方逐项复测或模型专项评价。

本次合入包括首台核换热器及其材料、用户指定的九格配方和鳍片造型，以及01C共享容量守恒修复与自适应供热。01A/01B原自动检查与审查记录沿用，本次人工信息单独记录，不改写旧证据。

## 集成与验证

- 主工程原为`922d13d`，仅有用户`.vscode/launch.json`未提交改动。复用同级、干净且没有运行进程的`Create_NuclearIndustry-svg-art`工作树，创建`codex/exchanger-acceptance`完成无冲突合并，再将main快进到`b6f323c`；旧美术分支保留。
- 合并后的`src`、`tools`、Gradle构建与版本配置与已验候选`c002fb5`一致。复用该候选30项定向JUnit、15项required GameTest、打包与独立审查；没有重复JUnit/GameTest、没有clean或全量资源检查。
- 主工程新跑一次`./gradlew.bat assemble --console=plain`，8秒成功，见[增量打包日志](./evidence/acceptance-assemble.txt)。这是本次新证据，与候选测试服在断言通过后保存退出停滞的历史情况分开记录。
- 主工程用户`.vscode/launch.json`哈希保持`65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`；候选生成日志和`tools/art-assets/__pycache__/`保留且未提交。没有操作用户客户端或迁移/修改用户世界。
- PM实际应用`minecraft-modding`、`minecraft-testing`、`minecraft-ci-release`及分支收尾技能；按治理5.1复用同一候选证据。未升级依赖、推送或发布。

## 后续启动与建议

主目录现在包含换热器及本批修复，可在关闭旧客户端后运行：

```powershell
Set-Location E:\MyMC\NewMod\Create_NuclearIndustry
.\gradlew.bat runClient
```

两个目录各用自己的`run`存档；原测试世界继续从候选目录启动即可，本轮没有迁移存档。

**后续优先级修订（2026-10-04）：** 验收时曾建议先补工作盆供热；用户随后明确要求先主线，工作盆后置。其只读准备已停止，未实施运行代码、未批准耗热参数。

当前推进[专用高压锅炉方案准备](../../../archive/2026-10-08-completed-plans/2026-10-04-high-pressure-boiler-preparation.md)，随后衔接超临界汽轮机→普通蒸汽冷凝回水，再补乏燃料基础封存。当前只完成核热接入Create原生锅炉的发电支线，完整`EXT-B-API-01`及专用机组闭环仍未完成。本次排序调整不改变上述验收证据。
