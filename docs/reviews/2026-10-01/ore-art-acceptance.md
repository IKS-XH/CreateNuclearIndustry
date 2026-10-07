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

已完成：候选 `39db2b4` 的 JUnit/build 通过后，PM 保存构建生成的两条日志并定向恢复，在干净候选中合入最新 main，得到 `d165d8828cef223c488fde01ae272f82d6a1bc63`。合并无冲突，src/tools/构建相对候选没有差异；合并结果再次运行 `test build --rerun-tasks --max-workers=1`，265项JUnit全过、build退出0。PM随后将main快进到该已验证结果；用户IDE配置哈希仍为 `65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`。

制品仍为 `4BA7B0AA2A25C30D1FDDC29D7E4EA2F50B66101C6FFCA32A368A8E685EFC65F5`；旧51张PNG与生成输出/JAR匹配，8张冷却剂与历史原图相同。检查报告原样归档（仅统一末尾空行），本地构建日志和JUnit原始记录在证据包。见 [执行者复核](./ore-art-final/EXT-A-ORE-ART-ACCEPT-01.md)、[合并结果核对](./ore-art-final/merged-verification.json)、[原始证据包](./ore-art-final/evidence.zip)。保留同级工作树供后续候选复用，未推送或发布。

历史 GameTest 的 116 项 required 断言通过、保存阶段挂起并退出 1 原样保留，不改写为正常退出，也不冒充本轮新运行。三矿 Java/数据未变时，复用既有真实设备、采样和 GameTest 证据，补充当前 JUnit/build 与制品验证。

用户随后批准铅锡粗矿1:1熔锭、200/100ticks与0.7经验、锭1:1原生压板，见 [批准参数](../../archive/2026-10-08-completed-plans/2026-10-01-lead-tin-material-proposal.md)。本批已完成，接续 [EXT-A-MATERIAL-02](../../archive/2026-10-08-completed-plans/2026-10-01-ext-a-material-02.md)；其余未批准配比和设备参数不因此放行。
