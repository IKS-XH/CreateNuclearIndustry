# ADMIN-RELEASE-01 阶段A执行报告（主线隔离树）

执行者交付未提交源码；不代表PM验收或用户手测。工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-admin-release-fix`，分支 `codex/admin-release-fix`，初始/当前HEAD `7ea305cad732b7bb26bc8c14504130bae059c12a`。开工无dirty；未执行Git写、jar、assemble、客户端或用户世界操作。

## P2审查整改（当前交付证据）

已实读同目录`REVIEW.md`并核完整调用链：仪表`neighborChanged/onPlace`及正式结构扫描采样红石；扫描内`updateRedstoneScram`被待续接pending早退；旧release随后撤pending并解锁，而正式核tick没有补采样、稳定高输入也没有新的邻居事件。该问题与审查描述一致。

沿原保存运行信封用例增加真实红石块，先稳定3tick，再重新读取无保存SCRAM的正常运行信封并正式重扫。`review-high-red.log`/`review-high-red.exit.txt`记录真实RED：high=true、claimed=false、pending=false、scram=false，正式START/COMMIT均ACCEPTED；6项中仅此业务失败，Gradle及Java exit1，正常保存退出。

仅在Service已有零接口release的pending分支内，安全目标提交后先清续接冻结，调用原`updateRedstoneScram`采样当前真实输入，之后才发布unclaimed状态。未改Instrument、原SCRAM算法、Slider校验或tick采样频率。原低输入实际棒位保持、保存SCRAM低沿锁、卡死和故障合同继续适用。

`review-high-green.log`/`review-high-green.exit.txt`为当前最新真实域：6/6、exit0、全部维度保存并正常结束。高输入标记为scram=true，正式START/COMMIT均SCRAM_LOCKED；真实移除红石块后低沿释放，安全目标仍100%，新逐棒会话可提交75%。同例同时核燃料/23HU积热/1234mB冷液/56mB热液保持；可动棒按既有高沿SCRAM立即全插，低输入分支实际棒位仍保持40%，已有保存SCRAM/卡死反例保留。

本轮命令分别为`./gradlew.bat runGameTestServer -PgameTestNamespace=cni_reactor_automation_retrofit -PgameTestDirectory=build/admin-release-high-review-red --console=plain`及对应`build/admin-release-high-review-green`。测试仍为原6项，只扩展现有保存业务，不新增域/模板/构建规则。State/Controller/Resume和相关JUnit来源未改，复用下列32/32，不重复运行。另实际使用`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/receiving-code-review/SKILL.md`核查审查意见后再实施。当前DIFF/SOURCE-HASHES已刷新，仍停阶段A等待PM复核，不替审查者改REVIEW结论。

## 根因和最小改动

`onPortRemoved/revoke -> stop`正确保留claimed，但正式结构变为零AUTOMATION接口后没有单独结束接管的转换。`rodsLocked`和正式Slider因此一直只读。保留原stop和所有Slider校验；仅在仪表接受当前已加载、有效扫描且零接口后调用release。退出清绑定/实例、观测、曲线、显示窗口、加载续接/检查点及权限，撤逐棒旧会话，一次推进epoch并沿原同步路径更新驱动。STOP/FAULT保持已有安全目标及保存SCRAM；待正常续接的运行信封先只提交全插目标，保持实际棒位后清意图。

仅改5个允许文件：AutomationService、AutomationState、InstrumentPortBlockEntity、RetrofitGameTests、AuthorityTest。生产净增35行；不改接口移除/卸载生命周期、热工或其它功能。`DIFF.patch`和`SOURCE-HASHES.json`给出交付快照。

## 实际证据

- `red.log` / `red.exit.txt`：首次指定真实复现，命令 `./gradlew.bat runGameTestServer -PgameTestNamespace=cni_reactor_automation_retrofit -PgameTestDirectory=build/admin-release-gametest-red --console=plain`。4项框架测试中3项原探针通过；新增业务失败：安全STOP任职且有全新燃料无热，BreakEvent拆口获准，空气阶段无效仍持权，补壳同一仪表正式有效重成型，claimed=true/FAULT，正式Slider=SCRAM_LOCKED、驱动displayLocked=true。Gradle/外层exit1，正常保存退出。
- `final-safety-green.log` / `final-safety-green.exit.txt`：首次阶段A生产逻辑的真实域，目录 `build/admin-release-gametest-final-safety`；同一命令其余参数相同。6/6，exit0，所有维度保存并正常结束。框架6项 = 3项本批业务 + 3项原Retrofit，不把数量扩成更多独立业务。当前最新证据见上方P2整改。
- 本批3项业务：①真实拆口补壳同仪表、正式Slider START/COMMIT、驱动更新tag解锁、旧逐棒会话拒绝、重复扫/tick幂等、重装STOP且旧GUI拒绝；②当前格式无口持权保存及正常MANUAL运行信封首扫退出、燃料/积热/冷热液/实际棒位保持、保存SCRAM和卡死仍拒写、续接意图清除且目标安全；③接口onChunkUnloaded回调、CHUNKS_UNAVAILABLE缓存和仅拆燃烧室仍claimed/FAULT，重装燃烧室不自动启动。
- `final-green.log` 中定向JUnit任务通过，XML位于 `build/test-results/test/`：Authority 4/4、Controller 18/18、Resume 10/10，共32/32，无失败/错误。命令 `./gradlew.bat test --tests '*ReactorAutomationAuthorityTest' --tests '*ReactorAutomationControllerTest' --tests '*ReactorAutomationResumeTest' runGameTestServer -PgameTestNamespace=cni_reactor_automation_retrofit -PgameTestDirectory=build/admin-release-gametest-final --console=plain`。该组合命令随后真实域1项失败，整体exit1；不能将其整体称成功。最终仅调整Service中待续接运行信封的安全钳位条件；JUnit涉及的State/Controller/Resume实现未变，32项证据复用，最终真实域重新验证Service分支。
- `git diff --check` exit0。无全量测试、无打包、未替换main正式JAR。

## 保留的中间失败

`red-coverage.log`整体exit1：保存无口仍锁是业务RED；扩展菜单测试最初站在驱动器，超出菜单0额外距离，属于夹具失败。`green.log`整体exit1：JUnit32/32，真实域两处夹具问题为FakePlayer关闭原生openMenu、ControlRodColumnState参数顺序导致actualDepth期望写错；改用既有管理员域的原生ServerPlayer+隔离连接并修正参数。`green-repair.log`6/6、exit0。`final-green.log`再次真实域失败指出无条件重提安全基准会改写保存SCRAM目标表；最终改为仅对尚未执行失联基准的运行续接信封提交安全目标，STOP/FAULT不重复改表，见最终6/6。

## 范围和技能

已实读两树AGENTS/治理/STATUS、主线卡及管理员合同，核Gradle基线 Minecraft1.21.1 / Java21.0.7 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6。实际使用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 核服务端所有者和生命周期；同根 `minecraft-testing/SKILL.md` 复用真实NeoForge域/模板、区分JUnit/真实服务端；同根 `minecraft-ci-release/SKILL.md` 核版本与制品边界，不采用其示例改版本。实际使用 `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/systematic-debugging/SKILL.md`、`test-driven-development/SKILL.md`、`verification-before-completion/SKILL.md` 追踪正式入口、先取得业务RED、核日志和退出码。using-superpowers已读，其SUBAGENT-STOP适用于本执行者。

物理ChunkEvent.Unload/完整服务器重启和客户端操作未新测；本批卸载证据限真实BE卸载回调及正式不可用缓存，当前保存证据限正式BE读取+首扫。所有GameTest目录均在build下，与run/saves隔离。用户复看/PM审查、准确源码提交和每树唯一一次后续打包均未执行，等待PM通知。
