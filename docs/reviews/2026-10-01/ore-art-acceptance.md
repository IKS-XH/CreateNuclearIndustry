# 三矿与素材批次验收收尾

**日期：** 2026-10-01。项目经理按用户既有自动派发、审核和 Git 管理授权收尾；不推送或发布。

## 人工结果

用户本次明确反馈：“粗矿的合成与拆解测试也通过了，重载存档测试通过，两种冷却剂的素材回退测试也都通过了”。本次据此关闭粗矿 9:1 合成/拆解、保存重进及两冷却剂回退后的视觉复验三项门槛。

此前已确认三矿自然生成、采集、粉碎轮加工及其余重绘素材通过，JEI 界面也已出现。本批列明的人工门全部完成；不补写未提供的逐次掉落数量、概率统计、第三方模组联调或完整生产链通过。

## 收尾执行与写集

- `ore_art_acceptance_check`：高速模型 `gpt-6-luna`，在现有同级候选工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`、基线 `39db2b4`，只读复核候选与已审查 `c92e762` 的差异。运行已有 `test build --rerun-tasks --max-workers=1`；检查 JUnit、51 张 PNG、8 张保留冷却剂、发布 JAR 与 JEI 隔离。不启动客户端/服务端/GameTest，不改用户存档，不修改实现。唯一写集为该工作树 `build/reports/extension/EXT-A-ORE-ART-ACCEPT-01.md` 和同名证据目录。
- `next_material_gate`：高速模型 `gpt-6-luna`，主工程只读核对下一最小材料批。只形成未决参数和推荐方案，不实现未批准配方。唯一写集为主工程 `build/reports/extension/EXT-A-NEXT-MATERIAL-GATE-01.md` 和同名证据目录；不运行 Gradle 或游戏。
- 两执行者均须读取实际 AGENTS、治理、相应活动卡与 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`；不得 Git 写、修改核心文档、自行验收或派发。
- PM 使用上述 Minecraft 技能、`minecraft-ci-release` 与分支收尾技能核对范围、制品和集成证据。用户既有 Git/自动验收授权优先于技能默认再次询问合并方式；保留现有同级工作树和用户 `.vscode/launch.json`。

## 集成与后续门

人工门已解除，待本次复核后在干净的候选工作树中合入最新 main 文档，核对合并结果，再将 main 快进到已验证结果。若代码冲突或验证失败，交执行者整改，不由 PM 编写代码。

历史 GameTest 的 116 项 required 断言通过、保存阶段挂起并退出 1 原样保留，不改写为正常退出，也不冒充本轮新运行。三矿 Java/数据未变时，复用既有真实设备、采样和 GameTest 证据，补充当前 JUnit/build 与制品验证。

下一步仍是基础材料加工后再到设备；未批准的产率、经验、加工时间和配方比例须形成可审核方案交用户决定，到达该门后暂停。
