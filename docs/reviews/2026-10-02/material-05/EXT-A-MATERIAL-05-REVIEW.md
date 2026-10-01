# EXT-A-MATERIAL-05 任务1独立审查

**角色与结论：** 只读审查执行者；不改变任务状态、不做 Git 写操作。基线 `53f4ebdd7b2f3bd580d96d85fb39b18e02fbb98d`，审查对象为该基线上的任务1未提交差异，完整输入为 `build/reports/extension/EXT-A-MATERIAL-05/functional-review.diff`。规格静态实现与已批准配方一致，但有 **1 项 Important 验证覆盖缺口**，本轮不能据现有证据确认“加热处理中断后守恒”；质量审查未发现独立的实现或中文注释缺陷。Critical 0、Important 1、Minor 0。此结论供项目经理安排限定整改与定点复审，不表示任务验收。

## Important：加热盆中断用例没有先进入加工

`src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionRefractoryBearingGameTests.java:337-357` 的 `mixerHeatAndPowerInterruptionPreserveMaterials` 在投入三原料与煤后，立即于第343行将搅拌机动力设为 `0`。第65 tick 的回调先核对静止输入，随即**同一个回调**移除燃烧室并把动力设为 `256`（第347-348行）。因此动力存在时已无热源；第150 tick 再补热前，配方从未同时具备动力与热级。第365 tick 的四砖结果只证明“起始停机/无热时保料，条件恢复后加工”，不能证明正在加热搅拌时断热或断动力不丢不重。最终 `final-gametest.log:160` 的通过标记沿用该场景名称，不能补足缺失的过程。

任务卡 `docs/superpowers/plans/2026-10-02-ext-a-material-05.md` 的全局合同及任务1要求实际中断与恢复。建议仅在本任务允许的新 GameTest 文件内补一个定点场景：先让真实加热盆进入加工，再切断热源或动力，检查输入、原生中间态和输出的总量，恢复后确认只得到一批四砖。Create 原生机制可能在中断后完成当前一批；只需验证守恒与无重复，不要求立即冻结或精确进度恢复，也无需修改生产机器。旧 `ExtensionSteelProcessingGameTests.java:318-335` 有未加热搅拌的运行中断动力夹具，可作时间安排参考，但不能代替本批加热配方的证据。

## 规格核对

- `BasicMaterialContent.java:31-39` 注册三个普通成品及一个 `SequencedAssemblyItem` 半成品；`ModCreativeTabs.java:64-66` 只加入三个成品。四个模型和中英文各四名称齐备。相关手写注释/Javadoc为中文，未见与行为冲突。
- `recipe/milling/quartz_dust.json:1-6` 为 `c:gems/quartz` 一份、100参数、唯一一粉；没有新增粉碎配方。真实磨石、粉碎轮回退的最终标记分别在 `final-gametest.log:186,190`；加载合同检查原生 `create:crushing/nether_quartz_ore` 仍在。矿石原生加工与本批直接制粉的边界正确。
- `recipe/mixing/refractory_brick.json:1-11` 只收 `minecraft:bricks`、`minecraft:clay_ball`、`c:dusts/quartz`，最低 `heated`、100参数、唯一四砖。具体与父粉末标签均为追加语义，耐火砖公共材料标签存在。最终真实无热/阴燃、普通热、超热、错误形态/错粉/缺料、输出堵塞场景的断言通过；其范围仍受上述运行中断缺口限制。
- `recipe/sequenced_assembly/heavy_bearing.json:1-12` 用坚固板起始，钢锭标签机械手、精密构件机械手、压片，`loops=1`、唯一一轴承。真实三步扣料、错序/错误物品/缺料/断动力恢复及原生组件编码往返均有最终通过标记；组件测试不代替客户端存档重进。
- 隔离普通服原始 `reload-false-true-false-latest.log` 与 `reload-evidence.txt` 显示外部 `minecraft:flint` 标签成员经 disable→reload、enable→reload、disable→reload 呈 false→true→false；启用时真实加热盆产四砖。临时外部数据包位于报告目录，正式 `src/main/resources` 未发现该包。

## 质量与证据边界

- `red-gametest.log:338` 是缺少 `quartz_dust` 注册的业务 RED。最终 `final-test-build.log` 显示 `BUILD SUCCESSFUL` 且退出码0；直接汇总52份原始 JUnit XML 得到265项、失败0、错误0、跳过0。
- `final-gametest.log:359-364` 是本轮179/179项 required GameTest 断言通过，随后停在 `Saving worlds`。`final-gametest-stop.txt` 指向本轮隔离游戏 PID 30776；`final-gametest-exit.txt` 为 Gradle 退出1，日志记载子进程退出 `-1`。因此只确认业务断言通过，**不声称 GameTest 进程干净退出**，也不套用此前 `green-probe-3` 结果。
- 独立逐文件比较 `src/main/resources/assets` 与 `data` 对最终 JAR 的 SHA-256：源292、JAR对应292、缺失0、内容差异0，其中游戏 PNG 69。PNG 的画面质量和旧图保护由独立 `EXT-ART-07` 审查负责。
- 独立比较 `default-run-before.csv` 与 `default-run-after.csv` 的路径、长度、UTC时间和 SHA-256：各182项、差异0。`java-before.json` 与 `java-after.json` 均为原有三个 PID，无本轮隔离游戏残留；根日志前后副本存在且当前根日志与后副本哈希一致。隔离 init 在 RED 与最终日志中均核对模型及 JavaExec 游戏目录指向本批报告路径。
- 本审查没有运行 Gradle、游戏或重新全量测试。精简 `evidence-small.zip` 与 `evidence-index.txt` 因上述限定整改由项目经理要求暂缓封包；定点复审时核查其实际存在、内容与哈希，不将此临时状态列为第二项缺陷。客户端真实加工、半成品保存重进、JEI/名称及四项外观仍属用户人工门，自动证据不能代替。

**审查依据：** `AGENTS.md`、`docs/project-governance.md`、活动卡全局合同/任务1、已批准 `2026-10-02-mainline-material-05-proposal.md`；实际读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 核对锁定版本与原生注册/配方，`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md` 区分真实 GameTest、JUnit和客户端人工验证。锁定版本 MC 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280，未采用技能中其他版本示例。

## 同一 Important 的限定复审（fix1，后续结论）

**本次结论：** 首审 Important 所述验证缺口已由定点整改和新一轮证据解决；限定复审残留 Critical 0、Important 0、Minor 0。首审记录保留以说明整改原因；本段取代其待整改结论，仍不代替项目经理验收和用户客户端人工门。只审 `build/reports/extension/EXT-A-MATERIAL-05/fix1-review.diff`、本轮日志及封包，未重新展开全范围审查，也未运行 Gradle/游戏或执行 Git 写操作。

- 差异只修改 `ExtensionRefractoryBearingGameTests.java`。第339-346行拆为已启动后的断热与断动力场景；第350-391行先给予普通煤热和动力256，再等服务端 `running=true`、`runningTicks==20` 且 `processingTicks` 连续递减才中断。`fix1-gametest.log:138-139` 分别记录 `heat`、`power` 的 `16→15`，中断当刻均为三输入、零输出。因“字段是否代表真实加工”是首审缺口的关键，额外只读核对锁定 `create-1.21.1-6.0.10-280-sources.jar` 中 `MechanicalMixerBlockEntity.java:148-177`：该阶段在服务端初始化并递减 `processingTicks`，降至零才调用 `applyBasinRecipe()`。因此新断点确实位于实际加工中、提交产物前。
- 第377-391行在中断后核对热源已移除或搅拌机转速为零，并以第568行起的守恒断言接受“完整三输入、零输出”或“零输入、四砖输出”两种 Create 原生状态；恢复后两场景各须精确得到唯一四砖。`fix1-gametest.log:165-166` 两场景通过，汇总行362-363为整改后 **180/180 required GameTests passed**。没有强加进度冻结，也没有修改生产机器。
- `fix1-build.log` 和退出记录显示整改后 `build --rerun-tasks` 退出0，`:test` 实际执行。独立重新合计最新52份 JUnit XML 为265项、失败/错误/跳过均0。整改后 JAR 的实算 SHA-256 为 `8D5BB2559370A47CB5DD9AE087DB20826E5BCB3C9FA54727DB725B54D37DB6E4`；`pm-fix1-artifact-verification.json` 为292源/JAR资源一致的 PASS。整改前 `pm-artifact-verification.json` 对应旧 JAR，仅作历史证据。
- 新 GameTest 汇总后仍停于 `Saving worlds`；`fix1-gametest-stop.txt` 指向本轮隔离 PID 37540，日志记录子进程 `-1`、Gradle `fix1-gametest-exit.txt` 为1。180/180断言通过与进程退出非零继续分别记载。默认 `run` 前后各182项且路径、长度、UTC时间、SHA-256差异0；`java-before.json` 与最终 `java-after.json` 均仅有原PID 21468、24304、464。
- 独立打开最终 `evidence-small.zip`：196916字节、SHA-256 `C3A11ACFC573A72F165FBD6EAB6DB334E2180B766A42CBC9658081470D88CEE1`，共111个文件条目。外部 `evidence-manifest.csv` 对110个证据条目逐个记录长度和SHA-256，逐项对 ZIP 验证均无缺失、长度或内容差异；第111项是清单自身，其 ZIP 内外哈希相同。封包含整改后日志、最新52份 JUnit XML、报告、隔离配置与快照；未见 JAR、游戏世界或缓存。`evidence-index.txt` 的条目数、字节数和总哈希与独立结果一致。
- 项目经理随后通报已核对根 `logs/latest.log`、`logs/debug.log` 当时与最终 `after` 快照一致，再按开工 Git blob 恢复这两个跟踪日志；ZIP 中前后副本仍保留。本审查首轮关于“当前根日志与后副本一致”的记录只描述恢复前的检查时点，不代表本报告最终交付时的根日志状态。恢复操作由项目经理完成，审查执行者未操作 Git 或根日志。

**人工门：** 客户端真实加工、半成品保存重进、JEI/名称及四项外观仍须用户按计划操作；上述自动证据不替代这些体验结果。
