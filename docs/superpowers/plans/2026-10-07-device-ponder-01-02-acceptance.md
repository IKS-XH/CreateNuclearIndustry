# DEVICE-PONDER-01/02：联合播放验收与主目录整合

**用户证据（2026-10-07）：** 用户明确“离心机、反应堆思索已经手动测试通过了”。关闭离心机R1/R2/R3及反应堆02-R1四情景的各自播放门；这句话不覆盖分区温压锅炉REWORK-01，也不授权接下一台教学。

**当前状态：** 两批已验收思索的六个功能提交已由PM无冲突挑拣到main，当前功能HEAD为`6acd310efd392aed7a27d0fb948c89a5f5feeaac`。13个教学源码/模板/语言/合同/工具文件与原最终提交`c4e486a`完全一致；锅炉、换热器、配置、内容注册及模组入口相对主目录基线`6188b66`无变化。一次main增量assemble及7项资源字节核对通过，已保存用户验收与制品记录；锅炉独立人工门仍未通过。

## 范围与版本

- 本批只完成已验收教学的main整合、现有资源封包和文档/Git收尾，不编写或修改功能、测试、构建或工具代码。
- 源提交：离心机`75265de`、`91941ef`、`d17376e`、`c4e486a`；反应堆`72b8ff1`、`c2bb583`。对应main提交为`b44977a`、`db6b73d`、`94b0162`、`6acd310`以及`df0ee0f`、`f6d6129`。
- MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6不变。主目录`E:/MyMC/NewMod/Create_NuclearIndustry`，锅炉候选及用户测试世界保留原位置。
- 用户2026-10-02治理5.1优先于通用技能的强制全量/重复测试/清理要求；当前规格和用户授权优先于旧任务卡中“人工门未通过”的历史状态。

## 精确派发：单一高速执行者打包

执行者只读AGENTS、治理5.1/5.2、本卡、两批报告及`gradle.properties`，实际读取并应用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`与`minecraft-testing/SKILL.md`。执行者不是PM，不Git写入、不修改核心文档或运行代码，不再派代理。

允许写集仅为：Gradle自然生成的主目录`build/`文件、本卡证据目录`build/reports/extension/DEVICE-PONDER-01-02-ACCEPTANCE/`及报告`docs/reviews/2026-10-07/ponder-acceptance-01/integration.md`。报告不能改变验收状态或玩法。

1. 主目录只运行一次`.\gradlew.bat assemble --console=plain`，用Java21，保存完整日志和退出码。不运行test/build/GameTest、clean、rerun、模板生成器或客户端。
2. 成功后核对五个教学NBT及中英文语言资源与JAR封包字节一致，核对三个场景/接入class存在，记录JAR大小和SHA-256。这是本次合入验证，不重做已验收场景合同或人工播放。
3. 若实际编译/封包失败，保留现场并报告准确错误；没有修改代码或擅跑辅助测试的授权。PM决定修复范围。

## 复用与人工边界

- 离心机R1提示寿命、R2双语/后备一致性、R3模板/方向/指示及增量制品证据复用原报告。
- 反应堆02-R1现有6项合同全部通过，四模板/双语/后备与原制品一致；复用原XML/日志。
- 两台播放通过来自用户本轮明确确认，不声称代理启动客户端复验。
- REWORK-01七项锅炉集中手测仍待用户；不合入锅炉重构代码、不推进其他主线或教学。

## 收尾清单

- [x] PM只合入六个教学功能提交，确认13文件与原候选一致，无锅炉夹带。
- [x] 单一高速执行者`/root/ponder_acceptance_build`（gpt-6-luna/medium）完成一次main增量assemble（exit0、11秒）和制品核对7/7，提交[integration.md](../../reviews/2026-10-07/ponder-acceptance-01/integration.md)。
- [x] PM读取实际日志/退出码/资源记录并独立核对JAR哈希，保存[验收页](../../reviews/2026-10-07/ponder-acceptance-01/ACCEPTANCE.md)、更新活动任务/治理/入口并提交收尾；没有再次运行Gradle或测试。
