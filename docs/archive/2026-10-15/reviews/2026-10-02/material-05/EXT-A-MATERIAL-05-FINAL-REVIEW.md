# EXT-A-MATERIAL-05 / EXT-ART-07 整批独立终审

**本轮结论：暂不建议以“整批终审通过”进入客户端候选验收。** 发现一个新 Important：素材验证器新增对被忽略的临时报告文件的强制依赖，干净检出无法按工具说明验证。功能、四项素材接口和当前 JAR 未发现阻断问题；该项限定整改并复审通过后，可进入既定客户端人工门，无须因这一工具路径问题重跑无关 GameTest。此结论是执行者审查意见，任务状态及验收由 PM 记录，任何自动通过都不代表可以合入 main。

## 范围与基线

- 工作区：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；只读确认 HEAD 为 `53f4ebdd7b2f3bd580d96d85fb39b18e02fbb98d`，候选为该基线上的未提交改动。
- 唯一整批文本入口：`build/reports/extension/EXT-A-MATERIAL-05/final-review.diff`，117653 字节，实算 SHA-256 `C8AE398E30445D04B061797E5C177B839C309476A77D52F1F274DED82A0E06B0`。完整阅读功能、测试、JSON、四 SVG、素材管线和工具说明差异，没有重新生成 Git diff。
- 已读 AGENTS、治理协议、扩展准备计划当前门槛、材料05实施卡及 ledger、已批准材料05方案、两项交付及分项审查报告（含 fix1 限定复审）。实际版本核对 `gradle.properties` 与 `build.gradle:13`：MC 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82 / Flywheel 1.0.6，未采用新版技能示例改变栈。
- 本次未运行 Gradle、游戏、素材 `verify.py` 或全套测试；未修改实现、素材或核心文档，未执行 Git 写，未派发其他执行者。新增写入仅本报告。

## 新发现

**Critical：0。Important：1。新 Minor：0。**

### Important — 验证器把临时 build 证据作为不可缺少的输入

位置：`tools/art-assets/verify.py:57`，相关路径定义在第21行。

新增的 `snapshot=json.loads((EVIDENCE/'preexisting-game-png-sha256.json').read_text(...))` 无条件读取 `build/reports/extension/EXT-ART-07/preexisting-game-png-sha256.json`。`.gitignore:2` 忽略整个 `build/`，只读 `git check-ignore` 也确认该快照被忽略；脚本在读取之前只创建目录，没有生成或恢复快照。`tools/art-assets/README.md` 仍直接提供运行 `verify.py` 的常规命令，未设置这一前置。

实际影响：新检出或清除构建输出后，即使受版本管理的源稿、PNG、历史基线和归档证据齐全，验证器也会在第57行因缺失文件抛出 `FileNotFoundError`，尚未进入本批重复导出、坏输入不写与安装检查。本机曾经 PASS 依赖工作区残留快照，不能证明交付后的验证命令可复现。这是本次新增的开发工具回归，不是当前 JAR 的游戏运行故障，普通 `export.py` 也不受这一输入依赖影响。结论来自明确静态执行路径与忽略规则核对，未删文件、未重跑验证器来制造失败。

建议限定修复：把只读输入指向已归档的 `docs/reviews/2026-10-02/material-05/art/preexisting-game-png-sha256.json`，或使用等效的可随交付稳定取得的输入；保留 `EVIDENCE` 作为输出目录及65图集合/哈希断言，不把当前文件重采样成“历史快照”。本次实算临时与归档快照哈希相同，均为 `3977CBA171DEE0F38144C5C56E5B2A59B6BB4BA9EE3B20961568C79C106CF2E5`。整改验证应证明临时快照缺席时仍能完成预期验证，并保留原负例与当前69图哈希；无需改配方、素材或游戏测试。

## 三条生产路线及接口

- `BasicMaterialContent.java` 注册三种普通材料和原生 `SequencedAssemblyItem` 半成品；`ModCreativeTabs.java` 只增加三个成品。四 ID 在注册、模型、双语名称、SVG/清单/PNG 和最终 JAR 中对应一致；半成品不进入创造页，不添加自有 NBT 协议。
- `recipe/milling/quartz_dust.json:1-6` 是一份 `c:gems/quartz`、参数100、唯一一粉，无副产物。仅增加磨石路线，真实粉碎轮测试检查选中该磨石配方回退；加载断言保留 Create 原生石英矿粉碎成石英路线，没有错误地禁止原矿加工。
- `recipe/mixing/refractory_brick.json:1-11` 严格使用砖块、黏土球、具体石英粉标签各一份，最低 `heated`、参数100、四件耐火砖；普通热与超热同产量，无新增生坯/建筑方块或机器覆写。具体粉末、父粉末及耐火砖标签均追加。真实无热/阴燃、错误形态/错粉/缺料、输出堵塞与恢复证据和声明吻合。
- `recipe/sequenced_assembly/heavy_bearing.json:1-12` 是坚固板起始、机械手钢锭标签、机械手精密构件、压片、一轮唯一一成品；没有修改精密构件原配方。真实装配断言核对逐步扣料和原生步骤组件、错序/错物品/缺料及停动力恢复。组件编码往返只证明编码保留，不冒充客户端存档重进。
- 整改后 `ExtensionRefractoryBearingGameTests.java:350-391` 先观察原生 `running`、`runningTicks==20` 和 `processingTicks` 连续下降才分别切断热/动力；第568行起按完整三输入或唯一四输出检查守恒，恢复后精确四砖。原始 fix1 日志两场景均为16→15、三输入零输出，后续各通过。没有要求 Create 未承诺的精确进度冻结；分项审查已对锁定依赖源码核实进度字段语义，本次未重复展开该源码审查。

## 素材与合法编辑

四份 SVG 为16×16整数 rect 和有限色板，模型引用正确。最终管线仍固定69个游戏路径、70清单、66个不同来源，青金石粉保留 `game=null`；冷却剂8项原图固定保护保持。`--install` 已移除“目标字节不同就拒绝”的错误限制，保留路径/集合/尺寸/冷却剂保护及相同字节跳过写入。

已读取合法石英粉像素编辑的 RED、GREEN 和 finally 恢复证据：相同源稿编辑原失败，整改后真实更新为 `0d37207539e4ba7998769ed261e04d6b4db1c91cd18a185f9e81808a0c4152fa`，随后恢复源、游戏图和生成输出。修复后素材 `verification.json` 为 PASS，默认导出/安装各两次、六类 CLI 非法输入失败不写仍在；本次仅复核记录，没有重放。

独立读取当前69张游戏PNG并对65张开工快照逐项哈希比较，差异0；四新图与生成PNG及交付哈希一致。实际打开四个 `*-comparison.png` 查看1倍、浅底9倍和深底9倍：粉末、砖、成品轴承与缺口半成品可区分，轴孔透明。既有耐火砖圆润/倒角轮廓 Minor 留客户端视觉判断，不重开重绘任务，也不新增为本次缺陷。

## 原始证据与当前制品

- `fix1-build.log:78,90` 显示 `:test` 实际执行及 BUILD SUCCESSFUL，退出文件为0。独立汇总最新 `junit-xml/` 的52份 XML：265 tests，failure/error/skipped 各0。
- `fix1-gametest.log:362-363` 为180/180 required断言通过，本批13场景都有成功记录。第367行之后停在 Saving worlds；停止记录明确为本轮 PID 37540，子进程-1、Gradle1单列。因此本报告不声称 GameTest 正常退出；不使用整改前179项结果证明处理中断。
- 原始 `reload-false-true-false-latest.log:72,84,86,100` 记录外部燧石标签成员 false→true→false，true阶段真实盆加工四砖；普通服随后有 stop/保存完成记录，正常退出0来自分项交付及独立复审证据。这是临时数据包模拟等价标签，不声称第三方模组联调。
- 独立只读打开当前 JAR，逐项比较全部源 assets/data：292源、292 JAR资源、缺失及内容差异0。JAR实算 SHA-256 为 `8D5BB2559370A47CB5DD9AE087DB20826E5BCB3C9FA54727DB725B54D37DB6E4`，与最新 `pm-fix1-artifact-verification.json` 一致；69 PNG、65旧图/4新图、8名称核查结果吻合。旧 `final-*` 与 `pm-artifact-verification.json` 仅是历史，不用于当前制品结论。
- 精简 ZIP 实算 SHA-256 `C3A11ACFC573A72F165FBD6EAB6DB334E2180B766A42CBC9658081470D88CEE1`，111条目、196916字节；独立读取110条清单，逐条比较 ZIP 项长度及哈希，无缺失/差异。清单自身作为第111项的内外一致性已有分项复审，本次未再重复。
- 独立比较默认 run 前后 CSV：各182行、差异0；前后 Java 记录均只有原 PID 21468、24304、464。根两个跟踪日志由 PM 核对快照后恢复的后续情况按分项复审说明记录，本次不把恢复前哈希当成现状，也不操作根日志。

## 主动排除与后续门

不重跑已通过的构建/GameTest/素材验证，不将历史运行包装为本次新运行；本次工作是源码、记录和制品的独立只读复核。没有发现其他需扩展到锁定依赖源码或旧机器实现的具体风险，不进行泛化重构、安全扫描或无关素材重绘。

不审查尚未批准的设备功能、青金石粉及水洗副产物，不评判历史任务状态，不把 PM 正在维护的“终审待完成/人工未交接”文案当成缺陷。当前待修的唯一新项属于验证器输入路径。

这一项修复并限定复审通过后，仍需用户完成客户端真实加工、热级/数量/恢复、轴承半成品保存重进、JEI及双语名称、四项外观检查；当前静态、真实服务器、离线外观、制品证据均不能替代这些人工门。人工门未解除前不得据本报告合入 main 或启动后续设备实现。

## 实际技能使用

实际读取 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md`、`minecraft-ci-release/SKILL.md`：分别应用于锁定版本/注册与原生配方、真实机器与组件/人工证据边界、模型语言及16×16 RGBA素材引用、制品一致与交接范围。技能通用提交、发布、验证器运行建议服从本任务只读及不重跑约束。

## 唯一 Important 的限定复审（2026-10-02，后续有效结论）

**原 Important 已解决；本次修复未发现新问题。建议可以进入客户端候选验收。** 此段取代首审的“暂不建议整批放行”结论，首审正文保留作为发现与整改历史。当前开放 Critical 0、Important 0、新 Minor 0；既有耐火砖轮廓 Minor 继续留客户端视觉判断。该结论仍不是人工通过或合入 main 授权，任务状态由 PM 记录。

本次只读审查 `EXT-ART-07/final-fix-review.diff`、相应当前行、快照路径 RED/GREEN、最新素材验证/命令记录、交付报告补充和 PM 制品复核；未再次展开整批审查，未运行验证器、Gradle或游戏。

- 最终差异仅新增 `PREEXISTING_SNAPSHOT` 固定归档路径、把读取入口改为该路径、在验证结果中记录实际来源。`verify.py:23,58` 现在读取 `docs/reviews/2026-10-02/material-05/art/preexisting-game-png-sha256.json`；第179-180行仍核对固定65路径集合及每张PNG哈希，第190-191行仍写入 build 报告目录。没有读取当前PNG生成伪历史快照、放松集合或内容断言，也没有新增对JSON原始字节/换行格式的锁定。README运行说明已明确归档输入及build输出的区别。
- `snapshot-resolution-red.json` 记录旧脚本在build副本临时缺失时退出1，异常精确指向旧第57行的 `FileNotFoundError`。`snapshot-resolution-green.json` 记录最终当前脚本在相同缺失条件下完整运行退出0/PASS；其stdout与最新 `verification.json` 一致，明确使用归档路径，69游戏PNG/70清单/66来源、65旧图不变、默认导出与安装各重复两次。最新 `commands.json` 中两次默认导出和两次安装均退出0，六类非法输入各退出1并保持原失败原因；原验证断言未被删减。
- 执行者报告第42行补记安全临时改名与finally恢复。独立实算恢复后的build快照SHA-256仍为 `3977CBA171DEE0F38144C5C56E5B2A59B6BB4BA9EE3B20961568C79C106CF2E5`；归档快照65项对当前PNG逐项差异0，四新PNG也各保持首审交付哈希。
- 新 `pm-post-tool-fix-verification.json` 为292资源/69PNG/65旧图/4新图/8名称PASS。独立实算当前JAR仍为 `8D5BB2559370A47CB5DD9AE087DB20826E5BCB3C9FA54727DB725B54D37DB6E4`，功能精简ZIP仍为 `C3A11ACFC573A72F165FBD6EAB6DB334E2180B766A42CBC9658081470D88CEE1`。本次只改开发工具输入路径，不重复执行或重新计数原游戏证据，首审记录的GameTest保存退出非零边界保持。
- 归档JSON存在且未被忽略；当前整个候选尚未提交，此文件的只读Git状态为 `??`，因此“可随版本交付”以PM将该归档文件连同本批变更纳入候选提交为前提，不能把当前尚未提交写成已经跟踪。它已在计划归档写集中，此项属于正常交付核对，不是新增阻断缺陷。

原“依赖本机build残留文件”的执行路径已解除，针对缺失快照条件的RED/GREEN与修复内容对应。后续仅保留既定客户端加工、存档重进、JEI/双语及四项外观人工门；人工通过前不合 main、不推进未批准设备。
