# BUILD-CONTRACT-01 实施报告

## 实施范围

仅修改 `P1DataContractTest` 与 `TurbinePonderContractTest`。配方测试解析 JSON 后按完整 `create_nuclear_industry:<id>` 字符串精确比较，并直接断言钢板压片 `results[].id`、新燃料组件 `result.id`、冷态冷却剂 `results[].id` 分别产出批准 ID。强化钢板与不完整强化钢板均不会误中钢板 ID。扫描正例确认同一个受保护 ID 以 `item` 或 `tag` 字符串直接出现时都会命中；这里不解析 tag 定义或其间接成员。冷却后乏燃料组件只允许在已批准灌封配方的 `inputs[].ingredient.item` 中出现，且不得成为其产物。热冷却剂、污染冷却剂、净化器、已移除合金钢板别名及其他受保护 ID 仍受禁止。

汽轮机测试保留三档展示断言，并检查独立 `section` 句柄的顺序：显示后等待15tick、同一句柄移动15tick、等待15tick、显示正文、隐藏同一句柄、等待15tick淡出。正文额外寿命断言保持原样。未修改正式配方、场景源码、构建脚本、依赖或候选目录。

## 修前根因与证据

- `P1DataContractTest` 将 JSON 原文做子串搜索，因 `reinforced_steel_plate` 包含 `steel_plate` 而误判。旧禁用规则也把灌封配方中合法的 `cooled_spent_fuel_assembly` 输入误判为未批准生产。
- `TurbinePonderContractTest` 搜索已被现行实现替代的 `showSection` / `hideSection` 文本；已验收实现场景使用同一独立区段句柄完成显示、移动和隐藏。
- 两个原始失败 XML 已在首次构建前复制保存：`build/reports/extension/BUILD-CONTRACT-01/before/`。其原始摘要分别为配方合同5例失败1例、汽轮机合同3例失败1例。快照 SHA-256：配方 `95BC630C804A6AB9C8723B3848E283613107A5725FC6B2853EE9CDD20BA4379E`；汽轮机 `F0BCEA2172C72914A97A2974459F98310433B7C48D00AB3C354F13A056D59267`。

## 验证结果

R0 按任务卡执行原命令 `.\gradlew.bat build`，退出码0，耗时17秒，370例全通过。收到复核后对测试合同新增断言并再次实际修改，因此按复核要求执行 R1；R1 验证由代码变化触发，不是阶段性重复验证。两次均未先跑定向测试，未使用 `clean` 或 `--rerun-tasks`。

- R1 退出码：`0`；Gradle 输出 `BUILD SUCCESSFUL in 18s`。
- R1 JUnit XML 汇总：69个报告文件，370例，0失败、0错误、0跳过。配方合同5/5通过，汽轮机合同3/3通过。
- JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，2,319,066 字节，SHA-256 `B428F571F316BB21C18082A849C0A672208C2503593395B380F159FAA7ABDD5B`。
- R0日志、退出码和两份目标XML快照保存在 `build/reports/extension/BUILD-CONTRACT-01/build-r0.log`、`exit-code-r0.txt`、`r0/`；R1 对应材料为 `build-r1.log`、`exit-code-r1.txt`、`r1/`。修前失败XML仍保留在 `before/`。R1构建后的目标XML也在 `build/test-results/test/`。
- 两个修改文件的 `git diff --check` 通过。

## 技能与边界

已阅读并应用 `minecraft-modding`、`minecraft-testing` 和 `systematic-debugging` 技能；项目版本取自本地 `gradle.properties`（Minecraft 1.21.1、Create 6.0.10-280、Ponder 1.0.82），未据技能示例升级版本。修前先核对失败 XML、现行配方及已验收场景，再针对已确认根因修改静态合同。执行者未作任何 Git 写操作；交由 PM 独立审查与集成。
