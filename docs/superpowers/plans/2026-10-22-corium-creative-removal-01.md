# CORIUM-CREATIVE-REMOVAL-01：创造模式拆残骸不掉废料

## 来源与范围

用户2026-10-22反馈04B流体熔融物手测完成，但创造模式打掉凝固残骸会掉事故燃料废料。本卡只修这一缺陷，保留其余手测反馈；不重开04B全清单，不自动合main。

延续`E:/MyMC/NewMod/Create_NuclearIndustry-accident`的`codex/accident-thermal-04b`，基准`afb16899972cbe1495e31857dd2d3b75286ada59`。不新建分支/搬存档，原“新的世界”保留；2026-10-22 19:58:03日志全部维度已保存，当前只有Gradle daemon，无游戏进程。主目录3D工具dirty及正式EF3C包不动。

PM维护文档/Git；唯一执行者不做Git写、核心文档修改或派发。先读AGENTS、治理5.1/5.2/6、STATUS、本卡及04B报告，实际使用minecraft-modding/testing；制品阶段使用minecraft-ci-release。核实际依赖，锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，新增手写注释中文。

## 允许写集与不变量

- `src/main/java/com/iksxh/create_nuclear_industry/accident/ReactorCoriumBlock.java`：按真实创造玩家拆除入口先移除唯一payload，避免`onRemove`再次掉落；准确入口须核本机锁定API及最终取消顺序。
- 同包`ReactorCoriumBlockEntity.java`仅在现有`take()`无法完成所需库存/热源注销时允许最小修改，不重写状态机。
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/AccidentExecutionGameTests.java`：复用既有真实域、模板、focus筛选；新增短路径业务验证，不改框架/构建。
- 本树`build/reports/corium-creative-removal-20261022/`：报告、原始失败/成功日志和候选身份。普通编译输出允许，跟踪包/日志先保留原件，交PM核验恢复；不打开用户世界。

创造玩家拆实载荷残骸不产生物品，BE唯一库存及HOT引用正确注销；冷却/空残骸同样不凭空产物。最终取消拆除不能预清库存。生存玩家正常采掘及原有环境移除仍恰好回收实际一份；不改普通容器、熔融本体、材料数量、辐射/热伤/运输或管理员拆除门。

## 验证与交付

1. 核`ServerPlayerGameMode.destroyBlock`、`playerWillDestroy`与NeoForge取消调用顺序，取得真实创造拆除RED，再最小修复；不以直接调用`onRemove`或纯布尔镜像代替真实玩家入口。
2. 一次聚焦真实服验证创造冷/HOT/空残骸、最终取消保留、生存回收及环境移除单份守恒。SKIP与业务分开记录，保留失败证据；不重跑长封存/04B/全JUnit矩阵。
3. 交简短EXECUTION及精确改动清单，一次组合窄审读已有证据。源稳定后由PM提交，再唯一一次增量jar及来源/资源/SHA绑定，存独立候选不覆盖04B原包/main正式包。根跟踪旧JAR/日志交PM恢复，原用户世界不动。
4. 客户端本轮仅剩创造拆残骸这一修复的复看；已报告其余04B手测不重复要求。未观察的物理卸载/真人多人不补记通过。
