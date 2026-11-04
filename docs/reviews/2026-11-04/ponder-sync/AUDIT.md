# PONDER-SYNC-01 第一阶段只读盘点

2026-11-04；执行者交付，供PM冻结实施段，未宣布验收。工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix`，分支 `codex/ponder-sync-20261104`，开工基准 `67a984b823eb9bea87bf0a39a715b2471e3f5af7`；交付时PM已快进纯文档状态至 `ea8085128f6d4a87004f15a5bde48dd7adcc2f78`，src/build实现未变，静态证据沿用；开工 `git status --short` 为空。未操作Git写入，未搬动、打开或修改世界，未改源码、模板、语言、测试及正式JAR，未构建或运行JUnit/GameTest/客户端。

已读本树AGENTS、治理（含5.1/5.2/6）、STATUS、活动卡、project、server-config、reactor-automation及全部八类教学源码/生成器/双语。实际技能入口均为 `C:/Users/IKSXH/.codex/skills/` 下的 `minecraft-modding/SKILL.md`（客户端/服务端边界、注册）、`minecraft-testing/SKILL.md`（合同与实际播放证据分离）、`minecraft-resource-pack/SKILL.md`（资源/双语/引用）、`minecraft-ci-release/SKILL.md`（核锁定版本与制品边界，不引入其发布示例）。`gradle.properties`及`build.gradle:13,103-108`实核为MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，`settings.gradle`未引入其他加载器。

## 1. 结论与八类对照

原20幕的模板与生成器同源，入口、标题和字幕索引没有缺失。需要同步的真实缺口是锅炉尺寸/主动汽口/安全、快捷换料与维修、管理员、辐射与事故处理；不重做已验无关镜头。建议新增5幕（锅炉安全1、反应堆事故1、管理员2、辐射1），复用原有模板/镜头完成其余短段。

本表源码路径相对 `src/main/java/com/iksxh/create_nuclear_industry/`；字幕键省略 `create_nuclear_industry.ponder.`。

| 类别 | 现有覆盖 | 准确缺口/陈旧内容及玩家动作 | 实施建议与真实依据 |
| --- | --- | --- | --- |
| 离心机（1幕） | 两格机体、底轴、顶面入浆、上下四侧分类出粉/回水、停转保进度、停动力后换重型轴承 | `enrichment_centrifuge.text_5`“每1000 mB…1份…7份…1000 mB水”及Java同段是具体加工数量，与思索不讲具体配方原则冲突；数量本身未发现过时 | 只将本段改为转速影响加工速度等用法，保既有出料/维修镜头；不改真实加工。`ponder/CentrifugePonderScenes.java:118-162`；`production/CentrifugeBlockEntity.java:126-141`；`production/CentrifugeBlock.java:267-285` |
| 反应堆（4幕） | 5～16、长方体/自由列布局、四向反馈与覆盖、无棒列不可SCRAM、冷热回路、护目镜、余热、手工和机械臂换料 | `experimental_reactor_refueling.text_5`只教“空手…再装入”，画面也实际两次点击，漏掉手持有耐久新料一次替换枯竭原件；剩余耐久旧件不能快捷替换。漏钢板维修燃料列/棒。`operation.text_7`撤红石恢复旧目标应限定无管理员接管。无事故教学 | 原换料页换成一次右键新料、旧件返还；保机械臂先取后装，不误教机械臂快捷替换；尾部补钢板在换料口/驱动器维修的两段，不编造维修会消余热/恢复燃料。原运行页加红石范围限定。新增事故与回收一幕。`reactor/FuelRefuelingTransaction.java:145-190`；`block/ReactorPortBlock.java:126-145`；`reactor/FuelColumnRepairTransaction.java:93-115`；`block/ControlRodDriveBlock.java:44-66`；`reactor/ControlRodColumnRepairTransaction.java:102-124` |
| 高压锅炉（3幕） | 分区、底部换热器、隔层/回路数、冷热/给水口、先水后热、双汽库存、汽种过滤、公共出汽下限与生产汽种区分 | `build.text_1`与`operation.text_1`均误写默认5～11；`build.text_6`只说顶面安全阀，三模板均单阀，缺多阀独立排量/堵一个只损一个。出汽页两条管线均有泵+马达且Java设动力，遗漏汽口主动出汽、不需外置泵。无干烧/超压/危险拆除/红石停止新收热教学 | 两句改5～16、保7³例炉；出汽页拆外置泵动力图示，改连续合法工业管，字幕明确两种汽都能从汽口主动推送。新增多阀/安全一幕，见§3。`config/BoilerConfig.java:22-50`；`boiler/BoilerControllerBlockEntity.java:195-223,313-326`；`boiler/BoilerState.java:327-348`；`boiler/BoilerAccidentState.java:22-40` |
| 汽轮机（3幕） | 三档尺寸/转子、搭壳、SC入/普通汽出、冷凝回水、双轴共享应力、256RPM、40tick平均与额定/门槛效率 | `steam_turbine_operation.text_1`双语当前“专用管泵将…”/“Dedicated pipes and pumps feed…”容易把外置泵当必需；Java默认句实际仅说流体进出。漏排汽堵塞满后拒收、红石立即撤本机动力，断汽的40tick叙述不能套到红石停机 | 只在通汽页补来源/背压/停机操作短段，保三档镜头和效率页；有外置泵的示例本身合法，可留作从储罐供汽的可选方案，明确锅炉汽口主动供汽也可。`turbine/TurbineState.java:138-164,185-197`；`turbine/TurbineControllerBlockEntity.java:177-185` |
| 核换热器（5幕） | 前冷后热、同向直列共库存、Create锅炉供热及余热、锅炉内置、空盆/空炉持续耗热与断流/堵回水立停、普通汽1:1冷凝与合法冷源消耗 | 未发现新增/变更玩法的实质漏教。加工4mB/t和冷凝1:1描述设备热/工质行为，并非具体制造配方；保留。五幕现行语言与代码吻合 | 不改Java/模板/生成器，不重开原镜头。`heat/HeatExchangerBasinBridge.java:35-53`；`heat/CondensationState.java:11-32,50-73`；`ponder/HeatExchangerPonderScenes.java:63-455` |
| 烧结炉（2幕） | 底热源、顶面手动/溜槽进料、四侧出料、断热保进度、核热前后口 | `fuel_sintering_operation.text_2`“每个生芯块烧结为一个烧结芯块”及Java同句仍在讲具体加工数量 | 仅该段改为查看/取出成品等操作，不改真实工时/产量，不重做供热或物流。`ponder/FuelSinteringPonderScenes.java:57-60`；`production/FuelSinteringBlockEntity.java:43-70` |
| 屏蔽装配台（1幕） | 一件放整台2³、唯一主控底轴≥32RPM、四周物品双向/顶部只入/底部不通 | 原单页按批准范围只讲搭建接口，未介绍“乏燃料/事故废料→封装→贮存架”的屏蔽用途；这是跨设备用法缺口，不能借机恢复配方教学 | 保本页和模板；在新增辐射页展示封存链，不报固化材料/桶的具体配比。`production/ShieldedAssemblyBlockEntity.java:70,129-137`；`production/ShieldedAssemblyRecipe.java:73-92`；`storage/SpentFuelPayload.java:15-33` |
| 干式贮存架（1幕） | 手动存取、顶/侧输入与底输出、容量/满架等待/恢复 | `dry_storage_usage.text_1`写“封装乏燃料桶”，现物品名已是“封装核废料桶”，实际也收含事故废料的合法桶；漏完整架屏蔽及拿出后恢复桶级透射 | 只将现页物品称呼改为核废料桶，并可补一句两种合法来源；屏蔽解释集中新增辐射页，保原容量/漏斗镜头与NBT。`storage/SpentFuelPayload.java:15-33`；`radiation/RadiationSourceAdapters.java:38-42`；`config/RadiationConfig.java:23-24` |

管理员两幕从 `reactor_automation_port` 按W进入，现 `P1PonderPlugin` 完全没有此入口或管理员场景；不绑定所有Create燃烧室。辐射仪表/护具也完全没有直接入口或正文。详见§3和§4建议写集。

补充术语：中文物品 `cooled_spent_fuel_assembly` 的现行名称确实是“枯竭铀燃料组件”，不擅自改中文物品名；英文物品名为 `Cooled Spent Fuel Assembly`，原换料 `text_3` 却写 `depleted uranium fuel assembly`，应仅在该教学英文及Java默认句统一现行英文名称。现实贫化铀粉/锭/装备并非该乏燃料，不能混为辐射源。

## 2. 20份模板、8个生成器、9个Java及双语实核

只读命令使用 `D:/Thonny/python.exe -B -X utf8 -`（Python3.10.11；系统python.exe是不可运行的WindowsApps别名）。导入各模块但不调用任何 `main/_write`，仅在内存调用布局/编码函数；换热器四条工业路线按其main中的同参数调用 `industrial_route`。将各现存NBT解压与内存编码比较，同时解析现存NBT核坐标唯一、边界和palette状态索引；未落盘重生成、未产生pycache。这是静态数据核验，不是正式结构扫描或播放通过。

表中“同源”均为**解压后NBT原始字节完全相同**；不把gzip文件头差异当模板变化。正文数来自两语言相同场景索引；全部20幕均有1标题和连续正文，无孤立模板或缺失标题。

| 场景ID（也是NBT文件名） | 生成器（tools/ponder/） | 大小 | 方块数 | 正文数 | 同源/布局 |
| --- | --- | --- | ---: | ---: | --- |
| experimental_reactor | reactor_scenes.py | 5×5×5 | 110 | 8 | 相同/有效 |
| experimental_reactor_rods | reactor_scenes.py | 13×5×13 | 117 | 8 | 相同/有效 |
| experimental_reactor_operation | reactor_scenes.py | 13×5×13 | 123 | 7 | 相同/有效 |
| experimental_reactor_refueling | reactor_scenes.py | 13×5×13 | 116 | 6 | 相同/有效 |
| enrichment_centrifuge | centrifuge_scene.py | 9×9×9 | 105 | 8 | 相同/有效 |
| high_pressure_boiler_build | boiler_scenes.py | 13×9×13 | 243 | 7 | 相同/有效 |
| high_pressure_boiler_operation | boiler_scenes.py | 13×9×13 | 264 | 8 | 相同/有效 |
| high_pressure_boiler_steam | boiler_scenes.py | 13×9×13 | 253 | 5 | 相同/有效 |
| steam_turbine_build | turbine_scenes.py | 26×10×15 | 720 | 12 | 相同/有效 |
| steam_turbine_operation | turbine_scenes.py | 15×9×11 | 161 | 6 | 相同/有效 |
| steam_turbine_efficiency | turbine_scenes.py | 15×9×11 | 161 | 6 | 相同/有效 |
| nuclear_heat_exchanger_introduction | heat_exchanger_scenes.py | 11×7×13 | 28 | 3 | 相同/有效 |
| nuclear_heat_exchanger_heating | heat_exchanger_scenes.py | 15×9×13 | 161 | 5 | 相同/有效 |
| nuclear_heat_exchanger_boiler | heat_exchanger_scenes.py | 15×8×13 | 260 | 3 | 相同/有效 |
| nuclear_heat_exchanger_processing | heat_exchanger_scenes.py | 15×9×13 | 158 | 4 | 相同/有效 |
| nuclear_heat_exchanger_condensation | heat_exchanger_scenes.py | 15×8×13 | 154 | 3 | 相同/有效 |
| fuel_sintering_operation | fuel_sintering_scenes.py | 7×6×7 | 51 | 3 | 相同/有效 |
| fuel_sintering_automation | fuel_sintering_scenes.py | 7×6×7 | 64 | 3 | 相同/有效 |
| shielded_assembly_placement | shielded_assembly_scenes.py | 7×6×7 | 66 | 3 | 相同/有效 |
| dry_storage_usage | dry_storage_scenes.py | 5×5×5 | 30 | 6 | 相同/有效 |

9个Java为本表八类的 `P1PonderScenes`、`CentrifugePonderScenes`、`BoilerPonderScenes`、`TurbinePonderScenes`、`HeatExchangerPonderScenes`、`FuelSinteringPonderScenes`、`ShieldedAssemblyPonderScenes`、`DryStoragePonderScenes`，加唯一索引 `P1PonderPlugin`。插件由 `CreateNuclearIndustry.java:150-151` 的客户端setup注册一次。已逐条对照八类注册方法、模板名、scene.title及正文次序。

`assets/create_nuclear_industry/lang/{zh_cn,en_us}.json` 各134个Ponder键（20标题+114正文），集合相等、两文件均零重复键。7处默认句与两语言都不逐字相等（换热器introduction.text_1，汽轮机build.text_3/7、efficiency.text_4/5、operation.text_1/5）；逐条读后大多只是同义，不是索引错位，不为追求字节等同重写。operation.text_1的泵前提按§1修正。

各新幕/改段沿现有 `duration+20` 正文等待习惯（覆盖Ponder额外10tick淡出，再留10tick净空），保一次一段。原8类未发现必须重做的无关视角问题；本次未启动客户端，因此“不新增镜头问题”尚不能当播放结论。新增必须先显露端口/机构，必要移除遮挡或转镜头，保底部控件净空；不能仅靠字幕称已看见。

## 3. 新增五幕应教的动作与真实边界

### 锅炉：high_pressure_boiler_safety

复用原7³炉布局，顶面增加第二个合法非边框阀。先同时显露两个阀和各自上方空气，再只堵一个、展示另一个仍可泄压；默认每畅通阀最多512mB/t，90%开/80%关，不按容积放大。护目镜能读畅通/总阀数、总排量及危险进度（`BoilerControllerBlockEntity.java:452-493`）。

之后分段说明供水、汽路和冷回路要畅通；控制器红石只停止新收热，既有热量仍在。无水且壁温不低于沸点，默认连续20秒损毁一个真实换热器/再加热段；超额定100%累计10秒或到150%立即向随机一面喷爆，95%及以下持续2秒才清累计超压计时；危险时拆炉不会安全绕过事故，应先停热、泄压、降温再正常维护。数值均标默认，不教配方或把出汽下限当阀门/目标压力。干烧/爆裂只做临时显示快照/局部结果，禁止调用 `BoilerAccidentLifecycle.commit/finishObserved` 或真实爆炸。

依据：`BoilerStructure.java:164-211`、`BoilerState.java:327-348`、`BoilerControllerBlockEntity.java:195-223,313-326`、`BoilerAccidentState.java:22-40`、`BoilerAccidentLifecycle.java:25-39,48-70`、`config/BoilerConfig.java:39-50`。

### 反应堆：experimental_reactor_safety

先以停堆、持续排余热、护目镜观察为正常动作，再说明融毁倒计时、危险拆除，以及TNT/苦力怕最终实际破坏仍危险运行的成员会引发事故；不误教“任意附近爆炸都会触发”或冷空堆同样事故。外爆必须来自最终真实损伤，`structure/ReactorExplosionLifecycle.java:21-49`及`ReactorStructureLifecycle.java:196-236`已经复用原事故，无新热工阈值。

画面只用当前已有残骸/熔融物方块和载荷显示：高温接触有热伤/点燃；熔融物会向下移动/溶蚀，不能当普通液体用桶/泵收走，邻水加快凝固和冷却，卸载不继续计时。凝固冷却后普通镐回收真实事故废料，经装配台封装进架；冷却不会消除其辐射，源回收/屏蔽后场强下降，不叙述永久区块污染。避免把热危害与辐射防护当同一机制，也不保证普通屏蔽墙能阻止下烧。

依据：`accident/ReactorAccidentService.java:52-61`（已用/小数燃耗/原乏料归废料）、`MoltenCoriumParameters.java:9`、`AccidentThermalParameters.java:9`、`MoltenCoriumEffects.java:72-82,110-146`、`MoltenCoriumBlock.java:23`（不可桶取）、`AccidentThermalEffects.java:77-86`、`ReactorCoriumBlock.java:25-34`、`storage/SpentFuelPayload.java:15-33`。不用事故服务端托管、真实伤害或真实爆炸来驱动画面。

### 管理员：reactor_administrator_setup / reactor_administrator_operation

接入幕：合法侧面自动化接口→相邻烈焰人燃烧室任职→先有有效燃料冷启动并准备冷却→运行许可经红石反相接仪表急停。教学W入口在自动化接口，实际空手右击控件在任职燃烧室；手持燃料仍走原补料。只在燃烧室位置高亮+右键图示，分段讲总览/堆芯/保护三页：总览模式目标与状态，堆芯只读、换料维修仍在世界端口，保护只读阈值/复位条件。讲清草稿“应用”与“启动”独立、关界面不暂停；不实际打开/劫持正式GUI、不发送设置或启动网络包。

运行幕：统一深度应用全堆可移动棒的目标、恒定发热设置HU/t并观察实际/可达范围；真实核热可维持普通燃烧且有自耗，不能当免费冷启动或事故冷却。部分列空/枯竭且仍有可用料不会单因缺部分料整堆停机；全无可用料、无覆盖、卡死、冷却不足、失联/急停等保护优先。故障满足健康稳定条件后人工复位、再按真实条件启动；正常保存重进经过安全重验续接模式/棒位，人工停机、故障、急停仍保持停机。无应力闭环、Ctrl绑表或远程电站协调教学。

依据：`automation/ReactorAutomationBlazeBridge.java:13-24,32-44`、`gui/ReactorAdministratorMenu.java:31-50`、`client/ReactorAdministratorScreen.java:43-68,109-129,167-173`、`ReactorAutomationController.java:81-109,119-157`、`ReactorAutomationHeatSupply.java:42-45,65-86`、`ReactorAutomationResume.java:32-81,170-211`；上述后四个路径仍位于`automation/`下。

MANUAL精调边界已核实，不单凭名称推断：`automation/ReactorAutomationState.java:18-20#rodsLocked` 仅 `claimed && phase != MANUAL` 锁逐棒；`control/ControlRodSliderService.java:278-289#validate` 对接管下PREVIEW/COMMIT再调用 `automation/ReactorAutomationService.java:171-184#canSetManualRod`，该方法仅MANUAL允许，仍拒绝故障、红石、卡死，预警只许插深，且需实际冷却能承担拟定各棒目标。`ReactorAutomationController.java:148`在普通MANUAL周期不反复统一改棒；GUI应用/启动统一目标见`automation/gui/ReactorAdministratorCommands.java:61-66,87,102`。字幕不宣称两模式均永久锁死逐棒，也无须扩大讲解精调细节；若PM认为现合同措辞不足，由PM处理文档，不改管理员实现。

### 辐射：radiation_protection

对照全新组件/实际使用过的组件/乏燃料与事故废料，说明前者及铀原料、贫化铀材料装备不放射，首次实际燃耗（包括小数）后使用历史保留。手持辐射计（主/副手均可）读场强/累计剂量/实际吸收；穿戴防护服削减吸收，头罩兼护目镜，携带不算穿戴且不会削弱携带源向外强度。离开/隔离源后剂量会逐步恢复，不教未实现药物。

对比普通容器、合法封装桶和完整贮存架：普通容器无额外屏蔽；默认合法桶95%削减、完整架屏蔽内部合法桶；用实际屏蔽混凝土/反应堆壳窗示范射线路径，不把任意装饰方块或铅材料误作屏蔽。装配台只讲封装用途和接口，不摆具体配比；乏燃料及事故废料均可沿同链处理。可在事故幕提示转查本幕，避免两幕重复大量护具内容。

依据：`radiation/FuelUseHistory.java:13-30`、`RadiationItemSources.java:34-42`、`RadiationEquipment.java:8-20`、`radiation/client/RadiationClientEvents.java:43-55`、`RadiationDoseCalculator.java:7-16`、`config/RadiationConfig.java:21-28`、`RadiationSourceAdapters.java:38-42`、`RadiationSourceIndex.java:284-301`与`src/main/resources/data/create_nuclear_industry/tags/block/radiation_shielding.json`。辐射计HUD只接真实当前客户端世界快照，不向其全局缓存注入Ponder假读数；本幕用物品图示与一段一段的读数说明/显示投影，不运行真实辐射采样/剂量/伤害。

## 4. 建议精确写集（待PM冻结，不是当前实施授权）

以下是实现上表全部真实缺口所需的建议边界；未列路径不写。所有手写注释/测试说明使用中文。

- 既有教学Java（前缀 `src/main/java/com/iksxh/create_nuclear_industry/ponder/`）：`P1PonderPlugin.java`、`P1PonderScenes.java`、`BoilerPonderScenes.java`、`CentrifugePonderScenes.java`、`FuelSinteringPonderScenes.java`、`DryStoragePonderScenes.java`、`TurbinePonderScenes.java`。
- 新教学Java（同前缀）：`ReactorAdministratorPonderScenes.java`（两幕）、`ReactorSafetyPonderScenes.java`（一幕）、`RadiationPonderScenes.java`（一幕）；锅炉安全加在既有Boiler类，不增加生产机制类。
- 生成器：`tools/ponder/reactor_scenes.py`（追加管理员两幕/事故一幕布局）、`tools/ponder/boiler_scenes.py`（更新出汽路线/追加安全幕）、新增`tools/ponder/radiation_scenes.py`（辐射幕）。其他六个原生成器不动。
- 既有模板仅改 `src/main/resources/assets/create_nuclear_industry/ponder/high_pressure_boiler_steam.nbt`；其余原19份不重生成。新增同目录 `high_pressure_boiler_safety.nbt`、`experimental_reactor_safety.nbt`、`reactor_administrator_setup.nbt`、`reactor_administrator_operation.nbt`、`radiation_protection.nbt`。若需要既有换料幕上的钢板操作，现模板已有换料口/驱动器，不需改NBT。
- 双语仅改 `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`、`src/main/resources/assets/create_nuclear_industry/lang/en_us.json` 的上述教学键/新幕键；不改物品正式名称、配方或其他功能词条。
- 必要合同测试：调整 `src/test/java/com/iksxh/create_nuclear_industry/BoilerPonderContractTest.java`（第四故事线/新幕特殊布局/主动出汽路线），`src/test/java/com/iksxh/create_nuclear_industry/P1Ponder01ContractTest.java`（反应堆第五故事线）；新增同目录`PonderCurrentFeaturesSyncContractTest.java`，只补新入口/双语/模板坐标与显示边界/单段时序的真实风险，不镜像机器算法，不新增全套机器测试。原烧结/贮存等测试若只校同索引可直接复用，无需为短句改写增加测试。
- 报告与必要只读工具：`build/reports/ponder-sync-20261104/`。不改AGENTS、治理、STATUS、活动卡、主策划等核心文档，不写build.gradle/配置/生产机理/GUI/网络/注册内容类、素材或世界。

建议注册：原反应堆11入口继续保4幕并追加事故幕；事故废料/残骸物品可直达事故幕。管理员只给 `reactor_automation_port` 两幕，不绑定 `create:blaze_burner`。辐射页建议挂 `radiation_meter`、四件防护服、`cooled_spent_fuel_assembly`、`accident_fuel_waste`、`sealed_spent_fuel_cask`、`dry_storage_rack`、`shielding_concrete`；不将这些新入口混进只代表P1内容的DIRECT_ENTRY_IDS，从而保原注册归属合同。工业管网不独立加幕。

## 5. 实施后必要验证建议与本阶段停止点

本阶段实际执行仅只读文件/源码检查、Python内存NBT对照和语言解析，结果如§2；没有机器测试、构建、游戏或播放证据。

冻结后优先跑受影响Ponder合同（反应堆/锅炉/烧结/贮存/汽轮机及新增同步合同）、新增模板/生成器/语言索引与关键可见坐标校验及增量编译。若已有可用无头场景构造入口，只跑本次受影响故事线；不为本任务新建大型框架。无须重跑原机器全套、原三条JUnit基线失败或全量GameTest。源稳定精确提交后按活动卡一次增量assemble并核源/包，候选播放验收与正式包身份分开。

集中客户端复看重点为一次快捷换料真实动画与旧件返还、维修目标可见；锅炉主动汽口无外泵、多阀/堵阀和安全信息；管理员接口W入口/燃烧室右键图示与两模式保护；事故残骸/熔融物/冷却回收；辐射装备和屏蔽封存。新段全部一次一段、保淡出净空及底部控件空间，不把静态坐标检查称实际可见。等待PM冻结实施段后接续，当前停在只读盘点交付。
