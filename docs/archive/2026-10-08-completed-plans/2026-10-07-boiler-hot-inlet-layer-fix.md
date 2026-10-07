# EXT-B-BOILER-REWORK-01A：底层热液入口与棱边成型整改

> **2026-10-08收尾：** 用户确认最终01F精简手测通过；本卡继续适用的锅炉规则已随REWORK-01～01F[验收合入main](../../reviews/2026-10-08/boiler-rework-01/ACCEPTANCE.md)。被01F替代的整炉瞬时切种不再要求复测。下方候选、失败及待验收描述保留历史含义，不作为当前人工门；不扩展为所有动力场景或首发验收，不自动接其他主线/教学。

**状态：** 2026-10-07定向整改已实施，3项真实GameTest通过，必要增量打包和审查完成，功能快照`cca0dbeb5eefb82d4022938497c0e545f1f3f077`。热液入口必须与底部换热器同层，且可以组成棱边；本项并入锅炉待验收候选，不开启其他主线或教学。

**规格裁定：** 热液入口只允许底面外围四条边的非角点位置，替代这一格外壳，端口水平朝向炉外。换热器仍只在底面非边框格；底层四角、竖向棱边、顶层棱边仍须外壳。角点未明确放开且有两个水平外向面，本次不增加角点端口规则。原水区较高层的热液入口不再合法；冷液口仍在隔层高度非边框侧面。多热液入口继续共享整炉库存、逐口限流，不要求入口与某一换热器逐格对齐。

**基线：** 同级候选`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，`codex/ore-acquisition`，HEAD `1f547c8cf848d5f7e46ca7bb5deb3e83b84649f3`；锅炉功能快照`d5d3604`。主工程`main`已验收两台思索，本锅炉仍未合入。

**根因：** `BoilerStructure.inspectDetailed`先用`edges >= 2`拒绝所有非外壳，后来仅允许水区非棱边热液入口。应在通用棱边校验前处理严格限定的底层热液入口，而非放开所有边框部件。

## 执行与允许写集

PM维护文档、审查与Git；单一高速执行者实施代码并持有本轮构建，不Git写入、不改治理/其他核心文档、不另派代理、不启动用户客户端或操作存档。保持中文注释、锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280，不改温压、配方、容量、限流、模式或旧版本迁移。保留既有日志、pycache及无关改动。

执行者必须读取本卡、AGENTS.md、原[REWORK-01](2026-10-07-ext-b-boiler-rework-01.md)，并实际使用：

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`
- `superpowers:systematic-debugging`与`superpowers:test-driven-development`；项目治理5.1精简验证优先，不以通用技能为由重复全域红绿/全套验证或提交。

允许写：

- `src/main/java/com/iksxh/create_nuclear_industry/boiler/BoilerStructure.java`：热口层位及外向面的严格校验、必要中文注释。
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionBoilerGameTests.java`与`BoilerReviewGameTests.java`：只移动既有合法夹具的热口到底层，保持原有断言覆盖。
- 同包新增`BoilerHotInletGameTests.java`，专用域`create_nuclear_industry_boiler_hot_inlet`；必要时仅复用/复制现有空模板至本域，不改构建或测试框架。
- 两种语言JSON仅修改锅炉结构诊断中与本次层位/棱边规则直接冲突的文字，不格式化全文件，不改思索文案。
- 唯一报告写口`docs/reviews/2026-10-07/boiler-rework-01/hot-inlet-layer.md`；原始日志和制品`build/reports/extension/EXT-B-BOILER-REWORK-01A/`。

如果实际接入需要改其他生产代码，先向PM报告具体调用与证据，不自行扩范围。

## 集中验证与人工门

- [x] 实现底层非角点热入口例外，并移除较高水区热入口许可；端口归属、能力与管道刷新继续消费同一Form，不另建库存。
- [x] 三项有意义定向GameTest：四向底边多个热口合法并共用库存/逐口额度；较高层热口、错向、角点和其他棱边端口非法；真实Create泵/管道通过底边热口送入热液，拆装能力撤销/恢复不丢量。
- [x] 专用域实际运行3项并最终全通过；新增夹具的编译、恢复、传动和镜像流向失败轮均保留，修复后仅复验本域。既有29项账本单测和15项完整域证据保留复用，不机械重跑。
- [x] 增量`./gradlew.bat assemble --console=plain`成功；初次打包后发现旧多口夹具的第二热口遗漏下降，只修该坐标并再次增量封包（2秒），不重跑不受影响的新3项。未clean或build全套。
- [x] PM核对差异和证据，`/root/boiler_hot_inlet_review`一次规格/质量合并只读审查；第二热口遗漏由PM发现并交同一执行者补齐，原审查者定向复核关闭。
- [x] PM保存候选、更新现行搭法与既有集中手测清单；本改动并入锅炉人工门，不单独增加完整测试轮。
- [x] 本卡继续适用的规则已随2026-10-08最终01F精简验收通过；被后续方案替代的旧操作不作为现行测试要求，具体范围见本页收尾说明及验收记录。

## 派发前核对

| 范围/接口 | 核对与裁定 |
| :--- | :--- |
| 校验器与流体能力 | 底边非角点只有一个水平外向面，现Form.outward及控制器代理可复用；不能仅放开edges而漏方向校验。 |
| 校验器与旧测试夹具 | 两个锅炉GameTest夹具的热口必须同步下降；不改其他设备夹具或账本算法。 |
| 专用域与既有测试 | 仅运行新增三项真实世界场景；既有完整证据保留，更新夹具随本次源码编译。 |
| 当前与旧方案文字 | 本用户指令优先于原“所有棱边仅外壳”和“水区非边框热口”；角点与其他边框限制保留。 |

## 执行记录

2026-10-07派发并完成`/root/boiler_hot_inlet_fix`（gpt-6-luna/medium），复用现有同级隔离工作树，不新建工作区。PM已读取Minecraft开发/测试技能、核对锁定版本、Git工作树与原校验/能力调用；本卡兼作当前整改进度账。原计划共享账本与本次几何修改不冲突，不增加账本重测。

PM已读取最终`gametest-retry-3.log`（真实3/3、exit0、正常保存退出）及`assemble-final.log`（2秒、exit0）；复核代码写集及JAR资源封包。最终JAR为2,186,418字节，SHA-256 `577CB895A33175AB18F08A65096B24E059271C94D95570622FC30638AFEC4414`，保存于两目录的`build/reports/extension/EXT-B-BOILER-REWORK-01A/client-candidate/`。原重构制品与失败证据保留。实施及PM审查记录见[报告](../../reviews/2026-10-07/boiler-rework-01/hot-inlet-layer.md)，人工门见[候选页](../../reviews/2026-10-07/boiler-rework-01/CANDIDATE.md)。
