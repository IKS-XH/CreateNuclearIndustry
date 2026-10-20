# ADMIN-RELEASE-01 唯一组合窄审

日期：2026-10-20。审查者只读两树源码、差异和已有证据，仅新增本报告；未实施、未重新测试、未构建、未操作用户世界、未执行 Git 写操作或再派发。以下为代码审查结论，不是 PM 验收或人工体验结论。

## 审查对象与依据

- 主线隔离树：`E:/MyMC/NewMod/Create_NuclearIndustry-admin-release-fix`，当前 HEAD 与指定基准均为 `7ea305cad732b7bb26bc8c14504130bae059c12a`。
- 事故候选树：`E:/MyMC/NewMod/Create_NuclearIndustry-accident`，当前 HEAD 与指定基准均为 `fc9cbd888864566eaf53c6977b7be838ad231a67`。
- 实读主工程及两树 AGENTS、主工程治理/STATUS、活动卡 `docs/superpowers/plans/2026-10-20-admin-release-01.md`、现行管理员合同，以及两树本报告目录中的 REPORT、DIFF、SOURCE-HASHES 和所列日志/XML。
- 实际使用 `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/requesting-code-review/SKILL.md` 及其 reviewer 模板检查合同、严重度与证据；使用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 追踪正式服务端扫描/所有者/红石/生命周期；使用同根 `minecraft-testing/SKILL.md` 区分纯 JUnit、真实 GameTest 和客户端/重启边界。using-superpowers 的 SUBAGENT-STOP 适用于本审查者，未再派发。
- 两树实际 Gradle 均保持 Minecraft 1.21.1 / Java toolchain 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82 / Flywheel 1.0.6；未据技能示例改版本、注册或构建规则。
- 逐一重新读取并计算两树共 10 个交付源码/测试文件 SHA256，全部与各自 SOURCE-HASHES.json 一致。两树 Git status 的源码写集均仅为指定 5 项；事故树另有已披露的 `logs/debug.log`、`logs/latest.log`，不计入源码差异审查。

## 做得正确的部分

1. 正式退出入口位于仪表接受扫描后的有效分支，Service 再核服务端、当前 BE 实例、非移除、`structureValid()`、origin、claimed、非正常停服及零 AUTOMATION 端口。扫描器只读已加载格，`structureValid()` 另核全结构区块可用；无效/缺区块缓存不能借此退出，仅拆燃烧室仍保留端口、原 stop/revoke 合同未放宽。
2. `State.release()` 只在 claimed 时增加一次 epoch；之后重复退出返回原对象。退出清管理员绑定/实例/观测/曲线/显示窗口/续接与检查点，撤旧逐棒会话，沿 `setAutomationState -> syncControlRodDrives` 同步只读投影。正式 Slider/GUI 身份校验未削弱，重装仍经新绑定安全 STOP。
3. STOP/FAULT 的退出不重提安全基准，避免改写保存 SCRAM 的目标表；待续接运行信封先提交全插目标，再清意图。退出本身不清燃料、积热、冷热液、实际棒位、保存 SCRAM 或卡死。
4. 事故树沿用原闭环，仅在永久退出清旧 `automationLoadFollowSession`/网登记；`accidentId` 令结构无效，仪表 `updateStructureCache` 直接早退，核 tick 同样早退，退出入口不能穿透事故接管。新增事故反例使用正式 meltdown 事件和托管账，覆盖了真实组件唯一转移与重复事件保持。
5. 事故 helper 改接既有 `CommonHooks.fireBlockBreak`，与该树原有 `ReactorBreakFinalizationMixin -> finishBlockBreak` 最终提交边界一致；没有用单独 EVENT_BUS.post 冒充最终维护事务，也未新增构建规则/域/模板。主线继续使用其原监听提交入口。

## 阻断项

### [P2] 运行信封首扫退出前遗漏当前高电平 SCRAM

位置：两树 `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationService.java`，主线第 64–67 行、事故树对应第 65–68 行；调用位置为仪表主线第 377–379 行、事故树第 418–420 行。

触发条件是本批明确支持的当前格式“正常运行信封 + 首次正式有效零接口”加载，同时快照未保存 SCRAM、仪表现有红石输入为稳定高电平。`updateStructureCache` 在 release 前调用 `updateRedstoneScram`，但该方法在 `ReactorAutomationResume.pending(this)` 时直接返回（主线第 483–486 行、事故树第 525–528 行）。随后 release 仅写全插目标、清 pending 并置 claimed=false，没有再次处理当前高输入。待续接信封加载时 binding 为 null，末尾 notifyPermission 因 burnerPos=null 也不会补发红石通知。

这样首扫结束后 `snapshot.scramActive()` 仍可为 false；正式 `ControlRodSliderService.validate` 第 278–292 行只查已提交的管理员锁/SCRAM/卡死，新的 START/COMMIT 可以写浅目标。`tickReactor` 不重新采样红石，稳定输入也不保证再产生 neighborChanged，因此不能依赖下一 tick 自动收敛。这是静态调用链确认的问题，本审查未新增现场复现，不将其表述为已运行失败。

现有 `administrator_release_saved_claim_preserves_protections` 确认已保存 SCRAM 和卡死保持；它后半段运行信封场景清空 SCRAM，但没有布置真实高输入，因而 6/6、7/7 不覆盖此边界。

整改要求：在撤掉加载冻结并开放逐棒之前，按既有 SCRAM 语义处理当前真实高输入，不能让 pending 的一次采样抑制跨过控制权交接；同时保留现有保存 SCRAM 的低沿保护、目标表和无输入时实际棒位不变。用既有 Retrofit 域在运行信封零接口首扫前布置稳定高输入，验证首扫退出后正式 Slider 仍拒绝，再按原红石规则解除；无需扩到新的高温功能或全量矩阵。

Critical：未发现。除此项外，未发现其它必须修复的源码问题。

## 非阻断项与证据核对

| 树 | 已有最终真实服务端证据 | 已有定向 JUnit | 审查解释 |
| --- | --- | --- | --- |
| 主线 | `final-safety-green.log` 第 122/124 行业务标记、第 126–127 行 6/6、第 138 行所有维度保存、第 143 行 BUILD SUCCESSFUL；同名 exit.txt 为 0 | Authority 4、Controller 18、Resume 10；XML 合计 32，failures/errors/skipped 均 0 | 6 项为 3 项本批业务加 3 项原 Retrofit，不扩大成 6 个新增业务；Service 最终窄条件由该真实域重验，未改变对应 State/Controller/Resume 的 JUnit 实现证据可复用 |
| 事故 | `final-green.log` 第 94 行事故锁/托管标记、第 114–115 行保存/正式交互标记、第 118–119 行 7/7、第 130 行所有维度保存、第 133 行 BUILD SUCCESSFUL；同名 exit.txt 为 0 | Authority 4、Controller 18、Resume 8；XML 合计 30，failures/errors/skipped 均 0 | 7 项为上述 6 项加 1 项事故反例；最后仅调整测试 helper，生产与 JUnit 对应逻辑未变，复用 30 项证据合理 |

- 主线原 `red.log` 第 278 行明确 sameOwner=true、claimed=true、FAULT、Slider=SCRAM_LOCKED、displayLocked=true，整体 exit 1；本批目标有真实 RED。
- 主线 `final-green.log` 真实保存保护用例失败、整体 exit 1；事故 `green.log` helper 不匹配导致 2 项失败、整体 exit 1。报告均已明确区分 JUnit 成功、业务失败和命令失败，不将中间命令算整体成功。
- 事故两份生成 tracked logs 已在报告中披露并备份。审查开始时这两项仍 dirty，收尾再次读取 status 时已不再 dirty；恢复动作不由本审查执行，备份身份/恢复方式仍由 PM 负责记录。这是工作区交接事项，不是本窄修源码阻断。
- 新增/修改手写说明均为中文，生产 diff 无高温/熔融物、物料托管、热工、GUI 协议或构建范围扩张。

## 合同结论与未测边界

**技术审查结论：需上述 P2 整改，当前不建议进入准确源码提交及候选打包。** 已有证据支持原复现目标和所列安全反例，但遗漏的当前高输入交接会影响既有红石安全合同；不以通过数量抵消该问题。该结论不修改任务状态或替 PM/用户验收。

本审查仅静态读代码/差异、核已有日志/XML/哈希，未运行任何测试。现有证据未新证明物理 ChunkEvent.Unload、完整服务端正常停服再启动、真实客户端连续操作/视觉或真人多人交接；保存验证是正式 BE 读写与首扫，卸载验证限 BE 回调与正式不可用缓存。正式 Slider handler 和驱动 updateTag 证据不代替客户端体验。尚未审查本批候选 JAR 身份或打包结果，也未授予合入 main、推送或高温功能施工结论；用户人工复看门保持。

## 同一 P2 整改核验与最终阻断状态

2026-10-20 追加。本节仅核原 P2 的新增差异与执行者已有证据，保留上述首次发现及当时阻断结论；未开启第二轮全范围审查、未重新测试或构建。当前技术结论以本节为准。

### 最小修正与合同

两树 `ReactorAutomationService.releaseIfPortAbsent` 的原 pending 分支均仅扩展 5 行：主线第 64–71 行（事故树对应第 65–72 行）先 `submitDepth(100,true)`，在仍 claimed/ARMING、逐棒仍锁的状态下清 `automationResumeSession`，调用原 `updateRedstoneScram(level.hasNeighborSignal(...))`，最后才 `endAutomationTakeover` 发布退出。这样当前高输入经既有 SCRAM 提交到唯一核快照后才开放管理员锁，不再依赖未来邻居事件或 tick 补采样；低输入无 SCRAM 时仍保持实际棒位，仅安全目标为全插。

原保存 SCRAM 解码不进入运行 pending，新增采样分支不会重写 STOP/FAULT 保存目标或绕过 `automationSavedScramAwaitingHigh` 低沿保护；原 SCRAM 算法、Slider 校验、Instrument 和事故 `structureValid/updateStructureCache` 门均未改变。事故接管在进入 pending 分支前已被 structureValid 拒绝，新增代码没有清事故 id、核账本或托管物料。源码修正与原 P2 要求一致，未引入新的玩法或扩大采样频率。

扩展仍位于原保存 GameTest：主线第 201–236 行、事故树第 240–275 行。真实红石块先稳定 3 tick，再重新读入无保存 SCRAM 的运行信封，经过正式扫描及正式 Slider START/COMMIT；随后真实移除红石块，经原低沿规则核安全目标及新逐棒提交。用例保留原低输入实际深度 40%、保存 SCRAM/卡死、燃料/23HU积热/1234mB冷液/56mB热液断言；高输入下实际棒位全插是原高沿 SCRAM 的既有行为，未将其混写为无输入退出改变实际棒位。

### 新证据和准确来源

- 主线 `review-high-red.log` 第 111 行为 `high=true claimed=false pending=false scram=false start=ACCEPTED commit=ACCEPTED`，第 116 行仅该业务失败、第 145 行 BUILD FAILED；同名 exit.txt 为 1，所有维度已保存。原静态 P2 现已有正式真实 RED，未把该命令计为成功。
- 主线 `review-high-green.log` 第 123 行为 `high=true claimed=false pending=false scram=true start=SCRAM_LOCKED commit=SCRAM_LOCKED`；第 124 行真实低沿/原保护通过标记，第 127–128 行 6/6，第 139 行所有维度保存、第 144 行 BUILD SUCCESSFUL；同名 exit.txt 为 0。
- 事故 `review-high-green.log` 第 132–133 行给出相同高输入拒写和真实低沿恢复标记，第 112 行事故 id/托管/epoch 保护标记，第 136–137 行 7/7，第 148 行所有维度保存、第 153 行 BUILD SUCCESSFUL；同名 exit.txt 为 0。同源 RED 复用主线，未机械重复。
- 更新后的两份 REPORT/SOURCE-HASHES 及 DIFF 中对应 P2 的分支已核对。重新计算 10 项交付文件哈希全部匹配新清单；State、Instrument、AuthorityTest 的哈希与初审快照完全相同，只有两树各自 Service 和原 RetrofitGameTests 改变。主线 Service 为 `79B792D54524D0C27EDABF3ADF4DE16238F56790F0E3BB3A8CE890E87897EB68`，GameTest 为 `082B9E1FCADAF6B9CD710F090774A6A9CAFD8C78007FEDACEDD19B2D50532890`；事故 Service 为 `49D4E8A768A7F123FEBD73F92CE5D71883EB565B6F2A54DEFF20500016433CB4`，GameTest 为 `835BCB2B9003CFE05474D92037BF7CD8D874995A83CBF1E9872BDCC6A9AF53A5`。
- State/Controller/Resume 及相关 JUnit 来源未改，初审核过的主线 32/32、事故 30/30 可继续复用；新增 Service 分支由上述两树真实域覆盖。当前两树 status 仍仅各 5 项源码/测试。PM 已说明两份事故 tracked logs 在核对 JUNIT-LOG-BACKUPS.json 源/副本同 SHA 后精确恢复并保留全字节备份，本审查未执行该恢复。

**最终技术结论：原 P2 已关闭；本次唯一组合窄审当前无未闭源码阻断项。** 可以按既定卡继续由 PM 处理准确来源提交及随后候选打包。本结论仅覆盖本窄修与列明证据，不审查尚未生成的候选 JAR，也不代替用户客户端复看、PM 验收、main 合入或高温功能开工；前述物理卸载、完整重启、客户端/多人未测边界保持。
