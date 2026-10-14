# REACTOR-ADMINISTRATOR-04 正常保存重进原状态续接窄修卡

2026-10-14。用户已明确确认“采用原状态续接”。本卡将已批准的短设计落为仓库要求的执行写集，不再重复索取开工授权。原01设计的“保存重进一律持锁STOP”只在本卡范围被替代；拆件、真实失联、故障、人工停堆及急停规则继续有效。

## 目标与基线

- 正常保存重进保留管理员运行模式、目标、控制棒实际与目标深度及调节进度；加载期间暂缓本机结算，重新核验结构、供能、冷却与红石后直接继续，不先全插棒、不重新点击启动。
- 复用 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-automation`、`codex/reactor-automation-basic`，HEAD `a1180c7`、功能源 `78acd5d`、原正式3183722B／879c3d72bf5a80f9cf93263abe847d8e0354d22499f7312bc1a9e661925506fb JAR。
- 原执行者 `/root/reactor_automation_retrofit_impl` 独占实现；原审查者 `/root/reactor_automation_review` 一次组合窄审及必要同次差异复核。禁止转派、Git写、治理写、PM角色转授及机械重复全量。
- 当前候选只用户日志dirty；世界、配置原位保持，不新树/分支、不搬档、不启动用户世界或覆盖用户配置。main功能及26cb18d0…正式JAR保持。本批与03集中人工门接续，不追认01/01R/03已通过，不推进02 PI。

## 已查明入口与不变量

1. 仪表read当前只恢复claimed并强制STOP；Service重建绑定会再次submitDepth100；onChunkUnloaded/remove等撤权，结构重扫又读取红石。快照本已保存ActualDepth/TargetDepth，需在这些真实入口协调续接，不能只改读NBT一处。
2. 接口现有模式/目标设置沿用，不新增控件、配方、热工参数或PI。运行续接检查点必须明确区分运行意图与授权；可保存模式、调节周期位置和必要非权威续接信息。旧generation、epoch授权、观察样本租约、菜单nonce和支付票据不能复活，必须重建当前实例身份。
3. 暂缓覆盖棒推进、燃耗、热工、维护扣账的整条本机结算；不存在先全插再悄悄提棒。重建后从原逐棒actual/target继续，手动各棒差异与卡死状态不被统一值覆盖。离线墙钟不产热/燃耗、不补算停机时间。
4. 正常服务器退出/保存的生命周期与运行中真实区块或成员失联必须区分，防止停服的卸载回调把已保存运行检查点改成故障；不得将任何卸载都当正常退出。结构破坏、端口或燃烧室替换、真实急停/FAULT/STOP仍撤权停机，之后不能自动续接。
5. 加载必须有限等待且不强载区块。沿用现有armingTimeoutTicks作为初始化等待上限，不新设玩法数值；当前真实结构完整性、燃料投影、供能、库存/积热/损伤保护及红石联锁核验后才继续。失败/超时走原保护，不无限冻结。
6. 原生反相联锁初始化可能先高后低；只能在本机结算冻结且当前完整身份/保护已经核验的交接窗口内处理正常反馈，不屏蔽真实红石急停、不清除保存的SCRAM、不用旧许可15掩盖加载缺件。运行后真实高信号仍即时保护。
7. 核热值班续接不能只恢复KINDLED或旧recent支付标记。供能恢复必须有当前真实热源的足额支付并遵守一次消费/事务提交；无需因正常重进再投一次冷启动燃料。供热不足、SPECIAL原时长、外部燃料原倒计时和事故冷却语义不变。
8. 未安装/未任职管理员反应堆原tick行为保持。若需要卡外入口先只读报告根因、准确路径及最小扩卡，未授权前不写卡外功能。

## 精确允许写集

候选根下，只有实际必要文件可以改；未触及文件保持。

现有生产：
- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationService.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationState.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationBlazeBridge.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationHeatSupply.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationPortBlockEntity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationController.java`
- `src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorStructureLifecycle.java`
- `src/main/java/com/iksxh/create_nuclear_industry/mixin/BlazeBurnerAutomationEntityMixin.java`

可新增：
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationResume.java`：当前版本检查点、加载交接及停服识别；职责可收敛到现有入口，不强制新增。
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/ReactorAdministratorResumeGameTests.java`
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationResumeTest.java`
- `src/main/resources/data/cni_reactor_administrator_resume/structure/resume_empty.nbt`：现有administrator_empty逐字节测试别名。
- 同次审查整改仅允许`src/main/resources/data/cni_reactor_administrator_resume_review/structure/review_empty.nbt`：上述resume_empty逐字节别名；四个整改代表放原GameTests的ReviewWindows嵌套holder，使用独立测试域及`build/reports/reactor-administrator-04/review-game`，不改原11代表。build.gradle仅追加该NBT精确正式排除，原class通配已覆盖嵌套。
- `build.gradle`：只上述新GameTest类及模板的正式JAR精确排除。

现有受影响测试可改：
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationAuthorityTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationDataTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationControllerTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAdministratorHeatSupplyTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/structure/ReactorStructureLifecycleContractTest.java`

证据/唯一执行报告：
- `build/reports/reactor-administrator-04/**`
- `docs/reviews/2026-10-14/reactor-administrator-04/IMPLEMENTATION.md`
- 原独立审查者只允许写`docs/reviews/2026-10-14/reactor-administrator-04/REVIEW.md`；其余文件只读，不复跑已有测试/构建。

禁止改GUI布局/协议、注册、资源美术、原真实热工公式、冷却转换率、世界/实例配置、其他docs/治理和测试框架。所有新增或关联改动注释按AGENTS中文合同。

## 顺序与必要验证

- [x] 实际读取AGENTS、本卡及Minecraft技能；捕获原源/JAR/用户世界与配置身份。先窄查停服、加载、核热与红石顺序，报告最小方案；正常退出与异常失联区别应有锁定API出处。
- [x] 针对原强制STOP/全插路径先获得行为RED，再完成检查点与原状态交接。旧保护断言仅按本卡准确修改保存重进部分，不删除失联/故障/急停证据。
- [x] 定向JUnit覆盖检查点分类与等待/超时，真实代表覆盖MANUAL逐棒差异、CONSTANT调节年龄及当前授权重建；各阶段按实际执行报告分列，不拼成最终新全套。
- [x] 独立测试世界验证真实NBT读写、真实BE加载/绑定和原生红石反相反馈，两种tick顺序，以及核热值班无额外冷启动燃料且全额收费。覆盖健康续接与真实缺件/高信号/冷却不足失败，保证非管理员保持。
- [x] 区分NBT重建、直接BE运行卸载回调与实际正常停服最终保存。真正ChunkManager卸载/重载、独立进程重启与客户端仍未验，不冒称通过。自然退出状态与业务断言分列，历史失败保留。
- [x] 一次原审查者组合规格/质量窄审；原执行者集中关闭两项Important，同一审查差异复核及制品补核无剩余必改。稳定后唯一必要增量assemble，1秒exit0，原包保持。
- [x] 冻结12来源、661class/1546资源及生成MANIFEST共2208文件；PM独立核真实ZIP零漂移、无本批测试载荷泄漏，源码及正式候选JAR净提交`d217196`。交付文档同批净提交；不合main，不自动发布。

内部交付已完成，详见[04候选](../../reviews/2026-10-14/reactor-administrator-04/CANDIDATE.md)。最终受影响定向10/10与纠错真实4/4分别通过；原11/最终保存15证据按未改范围复用，不合成新15项整套。原世界38文件、27配置及main26cb18d0…包保持；04与01/01R/03客户端门仍待用户，02未派发。

## 卡内技术裁定：正常停服与核热零缓存

原执行者只读核锁定Minecraft.runServer字节码：正常路径先handleServerStopping，再stopped=true/stopServer；异常路径也设置stopped=true却不经过handleServerStopping。因此仅isStopped不足以识别正常保存退出。允许在已注册的ReactorStructureLifecycle内用当前server的ServerStoppingEvent/ServerStoppedEvent临时标记协调保存和卸载，保证待保存状态dirty；异常及运行中卸载仍撤权。该标记不持久化为授权、不跨server实例残留。

纯核热值班可能没有外部备用燃料且积热为零。不能因旧付款已失效要求重投冷启动燃料，也不能在冻结期间先造新热。PM裁定允许在当前完整结构/实例、保存运行意图、无保存SCRAM/故障、原逐棒下一tick冷却与维护预算等核验后，有限加载窗口发新生成的反馈交接许可，冻结整条本机结算并等待原生反相降沿。它不恢复旧15/租约/票据、不免费显示KINDLED，不清保存SCRAM；真实高信号仍高或超时走原保护。降沿核验后第一笔正式核tick才从真实新生热足额支付并生成新receipt，失败立即保护，不能无限依赖预测预算。两种BE tick顺序、原生火把和零外燃料/零积热须在本批真实代表中验证。

## 同次组合审查集中整改

原审查者在[本批REVIEW](../../reviews/2026-10-14/reactor-administrator-04/REVIEW.md)报告两项Important，无新增必改。原执行者仍持有本卡写集，集中一次整改；同一审查者只复核两项实际差异及证据，不重启整轮审查/测试。

1. **稀疏燃料账本**：原生产允许从未装料的FUEL列没有snapshot状态，不能用全部结构FUEL位置与snapshot键强等判失败。首扫覆盖前，复用本版本NBT已保存的BreakSafetyFuelColumns完整扫描集核对实际FUEL布局，现有燃料核状态必须属于此集、控制棒完整映射和每个真实换料口加载/物品校验保持；不补造空列FuelColumnState。已验证的原布局在本加载会话固定，不能重扫先覆盖保存集再认可新增/删除。缺失/坏完整证据安全拒绝，不做旧版迁移。
2. **加载交接代际固定**：首扫建立新的当前结构/身份后，本加载会话固定它；等待反相降沿期间的真实结构代际或成员变化立即fail、撤反馈。预算与首次真实核结算核当前candidate/generation和既定身份，不能只比BE引用，也不能先沿旧绑定付费/恢复模式，再等普通Service发现过期身份全插。首次加载合理新generation与其后真实变更必须区别。

只补上述两类必要真实RED/GREEN（多FUEL仅装一列、加载反馈期间合法壳替换额外冷端/代际改变）与实际受影响定向；已有11域/保存/热费证据按未改范围复用。稳定纠错后更新源清单和报告，同一原审查者窄复核，无剩余必改再唯一必要assemble。不能把初审源清单或此前通过计数冒称纠错后完整套。

## 客户端交付门

同一候选目录 `.\gradlew.bat runClient`，用原测试世界验证手动/恒定功率正常保存重进无需点击启动，实际/目标棒位连续、热冷库存和核热供能正确；退出前STOP/故障/急停仍保持，运行中拆件/断冷却/红石急停仍保护。只复看本次续接与03未验项目，不重开既有美术/教学/管网已验门。

技能实际入口：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`；执行者用systematic-debugging/TDD/verification-before-completion，PM沿subagent-driven-development编排但仓库唯一执行者与一次组合窄审优先。
