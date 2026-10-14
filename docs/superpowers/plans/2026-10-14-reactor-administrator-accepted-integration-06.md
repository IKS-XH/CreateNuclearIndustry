# REACTOR-ADMINISTRATOR-06 已验管理员功能精确集成

2026-10-14。用户已明确保存重进与管理员其余项目全部手测通过，见联合ACCEPTANCE。本卡只组合已审/已验01、01R、03、04来源与main当前已提交美术；不重新开发管理员或推进02代码。

## 基线与角色

- 来源只读：E:/MyMC/NewMod/Create_NuclearIndustry-reactor-automation，63b2d56，功能/JAR d217196，3199564B/dc82b20a…；用户世界已正常保存退出，原树/分支/包/世界保持。
- 写入目标：E:/MyMC/NewMod/Create_NuclearIndustry，93cc213及PM纯验收文档。原正式JAR3049912B/26cb18d0…，原件必须保留。
- 原唯一执行者/root/reactor_automation_retrofit_impl持有本卡精确写集；原审查者/root/reactor_automation_review只组合差异窄审及制品补核，复用原功能审查/测试，不重审全部特性。
- 执行者禁Git/治理写/转派。PM不写功能代码；main其他工业管网/模拟器/日志等dirty全部保留，不纳本次包。

## 精确功能写集（main根下）

下面70路径来自来源分支相对共同基线的实际差异；只接管理员实际变更。没有卡外写集，发现缺口先报告。

- `build.gradle`
- `src/main/java/com/iksxh/create_nuclear_industry/CreateNuclearIndustry.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationBehaviours.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationBinding.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationBlazeBridge.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationController.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationGoggles.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationHeatSupply.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationNetwork.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationObservation.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationParameters.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationPortBlock.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationPortBlockEntity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationResume.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationService.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationState.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/client/ReactorAdministratorLayout.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/client/ReactorAdministratorScreen.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/client/ReactorAutomationClientEvents.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/gui/ReactorAdministratorCommands.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/gui/ReactorAdministratorMenu.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/gui/ReactorAdministratorProjection.java`
- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ControlRodDriveBlockEntity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/config/ReactorAutomationConfig.java`
- `src/main/java/com/iksxh/create_nuclear_industry/content/ModCreativeTabs.java`
- `src/main/java/com/iksxh/create_nuclear_industry/content/ReactorAutomationContent.java`
- `src/main/java/com/iksxh/create_nuclear_industry/control/ControlRodScramService.java`
- `src/main/java/com/iksxh/create_nuclear_industry/control/ControlRodSliderBehaviour.java`
- `src/main/java/com/iksxh/create_nuclear_industry/control/ControlRodSliderService.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/ReactorAdministratorGameTests.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/ReactorAdministratorResumeGameTests.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/ReactorAutomationGameTests.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/ReactorAutomationRetrofitGameTests.java`
- `src/main/java/com/iksxh/create_nuclear_industry/mixin/BlazeBurnerAutomationBlockMixin.java`
- `src/main/java/com/iksxh/create_nuclear_industry/mixin/BlazeBurnerAutomationEntityMixin.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorMaintenanceHeat.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorServerTick.java`
- `src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorStructureDefinition.java`
- `src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorStructureDiagnostics.java`
- `src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorStructureLifecycle.java`
- `src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorStructureScanner.java`
- `src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorSurfaceDescriptor.java`
- `src/main/resources/assets/create_nuclear_industry/blockstates/reactor_automation_port.json`
- `src/main/resources/assets/create_nuclear_industry/lang/en_us.json`
- `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`
- `src/main/resources/assets/create_nuclear_industry/models/block/reactor_automation_port.json`
- `src/main/resources/assets/create_nuclear_industry/models/item/reactor_automation_port.json`
- `src/main/resources/create_nuclear_industry.mixins.json`
- `src/main/resources/data/cni_reactor_administrator/structure/administrator_empty.nbt`
- `src/main/resources/data/cni_reactor_administrator_resume/structure/resume_empty.nbt`
- `src/main/resources/data/cni_reactor_administrator_resume_review/structure/review_empty.nbt`
- `src/main/resources/data/cni_reactor_administrator_startup/structure/startup_empty.nbt`
- `src/main/resources/data/cni_reactor_automation/structure/automation_empty.nbt`
- `src/main/resources/data/cni_reactor_automation_retrofit/structure/retrofit_empty.nbt`
- `src/main/resources/data/create_nuclear_industry/loot_table/blocks/reactor_automation_port.json`
- `src/main/resources/data/create_nuclear_industry/recipe/crafting/reactor/reactor_automation_port.json`
- `src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json`
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAdministratorCommandsTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAdministratorHeatSupplyTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAdministratorLayoutTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationAuthorityTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationBlazeTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationControllerTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationDataTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationResumeTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationStructureTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/reactor/ReactorMaintenanceHeatTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/structure/ReactorInstrumentStructureSummaryTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/structure/ReactorStructureContractTest.java`

## 两阶段流程

1. 先读取main AGENTS、本卡、联合验收与候选原审查清单；捕获main原源/dirty/JAR身份。逐路径对照来源及共同基线：新文件精确接入；旧文件只接管理员净差异。特别保留main最新Logo、ModCreativeTabs图标与既有物品清单、两语言美术/工业词条、混入列表、启动注册及测试排除。不得整体旧文件覆盖新main，也不得研究旧存档或增加新逻辑。
2. 只写上列实际必要路径、build/reports/reactor-administrator-accepted-integration/**和docs/reviews/2026-10-14/reactor-administrator-accepted-integration/IMPLEMENTATION.md。先交净来源/冲突处理/真实dirty保护清单，**不构建**。没有卡外源码或自动验收记录写入。
3. PM实核并将精确来源提交main；确认空索引/原其他dirty未变，再由PM把该已提交Git树导出到本报告目录workspace。导出不是新分支/工作树，不复制或启动世界；不要把dirty main直接打包。
4. 唯一执行者在干净已提交导出树中执行唯一必要增量assemble，不运行clean或机械重复全量test/build。Gradle锁定版本/依赖不改；原JAR精确class/NBT排除保持。如集成引入实际编译失败，保留原失败、报告具体缺口，仅批准必要源纠错后才重包。
5. 冻结导出源码/编译class/资源/最终JAR，实核每项输出与JAR一致、本批和旧管理员测试域无正式载荷；main最新美术与候选已验管理员均保留。来源候选dc82与main26cb原包备份不覆盖。
6. 原审查者只审真正组合差异、未破坏当前main来源与实际制品身份，无必改后PM把同源最终JAR更新main并跟踪Git，写INTEGRATION。执行者不擅自覆盖正式main包。

用户手测及原自动/审查证据复用，不因集成关闭锅炉16³、压力或其他独立门。05事故/未复现记录原样保持，不冒称代码已修。原候选世界不回写main；本批无GUI/配方/参数再设计、发布、删除树或外部实例操作。

## 后续02

联合验收已满足原条件授权，但02卡须先准确映射到当前GUI、维护＋1最低毛HU和04原状态续接。集成结束后再建立同级02树/分支，并携带当前已保存测试世界；不得用这份集成卡扩写闭环代码。

技能实际应用：minecraft-modding/testing、minecraft-ci-release、verification-before-completion；PM按原唯一执行者/一次组合窄审，仓库角色与用户已批准范围优先。
