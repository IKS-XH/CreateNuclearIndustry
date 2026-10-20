# ADMIN-RELEASE-01 阶段A执行报告（事故候选树）

工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-accident`，分支 `codex/accident-execution-02`，初始/当前HEAD `fc9cbd888864566eaf53c6977b7be838ad231a67`。开工无dirty。执行者只交未提交窄修，未执行Git写、jar、assemble、用户客户端或run/saves操作；不代表PM验收或用户手测。

## P2审查整改（当前交付证据）

与主线同源整改`REVIEW.md`所列运行信封首扫稳定高输入漏采样：仅在Service既有零接口release的pending分支内，先写原安全目标、清加载冻结并调用原`updateRedstoneScram`采当前真实输入，然后发布unclaimed状态。本轮未改Instrument或原SCRAM算法/Slider/tick规则；事故id门、原余量模式、会话清理保持。

真实RED在主线同源用例证明，见绝对路径`E:/MyMC/NewMod/Create_NuclearIndustry-admin-release-fix/build/reports/admin-release-20261020/review-high-red.log`及exit.txt：红石块先稳定3tick，再读无保存SCRAM的运行信封；正式首扫后high=true、claimed=false、pending=false、scram=false，START与COMMIT均ACCEPTED，整体exit1并正常保存退出。本树不重复同源RED。

本树最新命令`./gradlew.bat runGameTestServer -PgameTestNamespace=cni_reactor_automation_retrofit -PgameTestDirectory=build/admin-release-high-review-green --console=plain`，日志/退出码为`review-high-green.log`/`review-high-green.exit.txt`：7/7、exit0，所有维度正常保存并结束。稳定高输入退出后scram=true，正式START/COMMIT均SCRAM_LOCKED；真实移除红石块后原低沿释放，目标保持100%而非旧浅目标，新会话可提交75%。同例核燃料/23HU积热/1234mB冷液/56mB热液保持；高沿沿原算法紧急插入，低输入分支保持40%真实棒位，保存SCRAM/卡死反例与正式事故托管锁继续通过。

不新增测试域/模板/构建规则，只扩原保存业务，框架数仍7。State/Controller/Resume及对应JUnit直接来源未变，复用30/30，不再运行JUnit。另实读并应用`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/receiving-code-review/SKILL.md`核链路、先在主线复现RED再同源修正。当前DIFF/SOURCE-HASHES已刷新；阶段A等待PM复核，REVIEW由原审查者处理。

## 移植范围

从主线隔离树移入相同的正式零接口退出分支、State.release和仪表运行数据清理。生产仅3文件新增36行；比主线多的1行是调用事故树既有`setAutomationLoadFollowSession(null)`释放原余量会话/网权，保留该树所有闭环玩法和代码。事故`accidentId`使structureValid为false、updateStructureCache早退，退出分支不能穿透该锁。

测试仅允许的RetrofitGameTests与AuthorityTest。移植前只读比较确认这两份基准文件与主线7ea305c对应原文件相同；没有复制主线其它优化、闭环删除、许可或文档。`DIFF.patch`仅含5项源码/测试，`SOURCE-HASHES.json`绑定本阶段交付。

## 验证命令及结果

主线指定真实RED复用 `E:/MyMC/NewMod/Create_NuclearIndustry-admin-release-fix/build/reports/admin-release-20261020/red.log`：同一仪表有效重成型后claimed=true/FAULT、正式Slider=SCRAM_LOCKED、驱动displayLocked=true，整体exit1，失败原因与本树原链一致。

1. `./gradlew.bat test --tests '*ReactorAutomationAuthorityTest' --tests '*ReactorAutomationControllerTest' --tests '*ReactorAutomationResumeTest' runGameTestServer -PgameTestNamespace=cni_reactor_automation_retrofit -PgameTestDirectory=build/admin-release-accident-gametest --console=plain`，见`green.log`及`green.exit.txt`。JUnit Authority4/4、Controller18/18、Resume8/8，共30/30，无失败/错误；XML在`build/test-results/test/`。随后GameTest7项中2项夹具失败，Gradle整体exit1、Java子进程exit2，正常保存退出，不能称整个命令成功。
2. `./gradlew.bat runGameTestServer -PgameTestNamespace=cni_reactor_automation_retrofit -PgameTestDirectory=build/admin-release-accident-gametest-final --console=plain`，见`final-green.log`及`final-green.exit.txt`。7/7、exit0，各维度保存并正常结束。首次命令到最终命令间仅修本树测试helper，生产及JUnit对应来源未变，30项结果复用。
3. 源码写集的`git diff --check -- <5项源码/测试>` exit0。未运行事故全矩阵或全量测试，未新增构建规则/域/模板。

框架7项 = 主线本批3项真实业务 + 3项原Retrofit + 1项事故接管反例。3项本批共同业务验证正式安全拆口/补壳同仪表Slider可提交及同步解锁、旧拖动/GUI拒绝、幂等、重装STOP、当前格式无口保存与运行意图退出、账本/SCRAM/卡死/真实棒位保持，以及卸载回调/不可用结构/仅拆燃烧室不释放。额外事故反例通过正式ReactorMeltdownEvent接管真实新组件，再把接口换壳证明几何有效且零口；正式重扫、直接退出检查及核tick都不能解除事故id/claimed/只读锁，epoch不变、许可为0，重放事件仍返回同一托管账，原口无组件、托管已capture。

## 首轮夹具差异与日志保留

本树已有ReactorBreakFinalizationMixin将实际维护事务放在CommonHooks.fireBlockBreak的post返回边界。旧Retrofit helper仅`NeoForge.EVENT_BUS.post`，只执行只读预检，因此原五格清空断言失败，新增场景后半段补料也因原燃料没被清走而失败。这两项是旧helper与事故树既有最终门不匹配，不能算生产故障。仅将允许文件内的breakEvent helper接到该既有CommonHooks正式入口后7/7；主线helper保持其对应的原正式监听入口。

首次阶段A的JUnit曾在本树根目录生成原已跟踪的`logs/debug.log`和`logs/latest.log`，均3493 bytes、SHA256 `E8B2FEE1F4A697A7A8B363AFE0025C827A3027B08D6998915EABA10A46C8F03C`。开工这两项干净，属首次阶段A生成；执行者未自行恢复或执行Git写。已逐字节完整备份并核源/备份SHA一致：`build/reports/admin-release-20261020/junit-generated-logs/debug.log`和`latest.log`。绝对路径、字节数和SHA见`JUNIT-LOG-BACKUPS.json`。PM在本轮P2整改前已核备份并恢复这两项，本轮未跑JUnit、未再生成根目录tracked logs；源码patch未夹带日志。

## 技能、版本与未测边界

实读本树AGENTS/治理/STATUS和管理员合同、主线绝对路径活动卡及更新合同。实际应用本机`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`核服务端接管/所有者/事故锁，`minecraft-testing/SKILL.md`选定向JUnit和原NeoForge域、适配当前原生最终入口，`minecraft-ci-release/SKILL.md`核版本与暂不打包的来源边界。实际应用`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/systematic-debugging/SKILL.md`、`test-driven-development/SKILL.md`、`verification-before-completion/SKILL.md`追踪差异、复用已证RED、核真实日志及exit。版本保持Minecraft1.21.1 / Java21.0.7 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6。

所有测试世界位于上述build隔离目录；未启动或修改事故用户手测世界、未覆盖其保存进度。当前保存验证是正式BE读写/首扫，卸载证据限BE回调/正式不可用缓存；未新证物理ChunkEvent.Unload、完整服务器重启或客户端手测。等待PM组合窄审、准确源码提交和随后每树唯一一次增量打包通知。
