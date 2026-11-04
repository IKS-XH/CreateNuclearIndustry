# PONDER-SYNC-01：现行功能教学查漏补缺

用户2026-11-04要求在两事故候选合main后，对思索全面查漏补缺，把新增或变更的部分同步到教程。前置main `9dbe952ff2e5766a46742d77027e62735e94336d`，本卡只更新已有玩法的教学，不借此新增设备机制、配方或恢复管理员应力闭环。

## 工作区、职责与边界

PM维护本卡/核心文档、确定精确写集及验收、管理Git；唯一执行者负责盘点、教学代码/模板/语言及必要测试、构建；最后一次组合窄审。执行者不得Git写、修改核心文档、派发或宣告人工验收。

复用干净`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix`，同目录从最新main切`codex/ponder-sync-20261104`，保原位世界，不另搬动、混合或打开用户世界。主目录3D工具dirty和已验正式包不动；新教程候选待用户集中播放验收，不自动合main或发布。

必读AGENTS、治理5.1/5.2/6、STATUS、本卡、现行project/server-config/reactor-automation和原八类教学。实际使用`C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-resource-pack,minecraft-ci-release}/SKILL.md`，锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；核实际Gradle，不照搬别版API或升级依赖。

教学只讲用法，不讲具体配方、不带开发措辞，不重复Create基础知识。每次仅一段正文，字幕结束后至少留原淡出净空；关键端口/机构须可见且高亮，镜头适配大小与底部控件，不用文字声称已显露却仍被结构遮挡。沿真实客户端显示边界更新临时世界，禁止调用服务端事务、启动真实事故或对用户世界写入。模板与生成脚本保持一致，注册/标题/中英文字幕按同一场景索引。

工业管网不单独新增教学。管理员入口为自动化接口`reactor_automation_port`按W，交互在任职烈焰人燃烧室上。不绑定所有Create燃烧室，不增加设备GUI；可说明已存在的管理员三页GUI。辐射/事故只介绍现行防护、触发、停堆/余热、冷却凝固、回收和屏蔽链，不讲未实现的永久区块污染、药物、再处理或管网压力。

## 第一阶段：只读盘点

先逐一核对八类既有场景（20份模板）、生成器、注册入口、中英文字幕与对应现行代码。重点查锅炉5～16/多阀/堵阀/干烧/超压、反应堆快捷换料/外爆融毁/管理员、已用燃料辐射/护具/辐射计/屏蔽封存；其他原场景只改真实发现的陈旧知识，不能无理由重做已验镜头。

允许写本树`build/reports/ponder-sync-20261104/`的`AUDIT.md`及必要只读检查工具。交简短对照表：现有覆盖、准确缺口/过时句子、玩家应学会的动作、对应真实代码/资源、建议复用/补幕及精确文件写集。**第一阶段不改教学代码/模板/语言/测试，不构建或重跑原机器测试。** PM据盘点冻结下面实施段后，同一执行者接续，无须重新询问已授权的教程同步。

## 第二阶段：冻结实施写集与内容

2026-11-04，PM已核第一阶段`AUDIT.md`：20/20模板解压原始NBT与生成器一致、双语各134键/零重复；八类均已核。盘点未改变教学源或机器。按用户本次授权直接同步下列真实缺口，不新增玩法，不需要再次确认已批准设备规则。唯一执行者接续，仍在同目录/分支。

1. 锅炉原三幕：默认尺寸5～16；保7³例炉；出汽页移除外置泵/马达改合法连续工业管并演示汽口主动输出，保双汽纯过滤和压力下限含义。新增`high_pressure_boiler_safety`一幕：两个合法阀可见、高亮并分别堵/通，默认每阀512mB/t和90%开/80%关；干烧20秒损毁一件、100%/10秒/150%超压喷爆及95%持续2秒清累计、危险拆除直接事故、停热不清余热，均按默认值说明。
2. 反应堆：原运行页撤红石恢复目标限定为无管理员接管。原换料页真实改为手持新组件一次右键换出枯竭原件，有耐久旧件不能快捷替换；机械臂仍先取后装。补安全停裂变/取料后钢板维修燃料列、驱动器维修控制棒，不教修复能消余热/返还燃料。英文乏料名称按现行Cooled Spent Fuel Assembly，中文物品名保持。新增`experimental_reactor_safety`一幕，教失冷/积热与危险拆除/实际外爆、余热、火点可扑灭但不代表残骸已冷却/辐射消失、热残骸/下烧熔融物、凝固冷却后采掘及既有封装回收；不教任意附近爆炸或冷空堆必事故，不执行正式事故。火点仅在Ponder临时摆放/移除，不绑定Lease或SavedData。
3. 管理员新增`reactor_administrator_setup`、`reactor_administrator_operation`两幕，只从自动化接口按W进入，燃烧室相邻任职后在燃烧室空手右击。高亮真实位置并用原生操作图示说明三页、应用/启动、统一深度/恒功率、冷启动燃料/真实核热值班、部分缺料不停整堆、保护/人工复位及正常续接。MANUAL允许安全校验后的逐棒精调，CONSTANT及安全状态锁棒；不必展开全部内部守卫，不误写两模式都永久锁棒。禁止实际打开GUI、发送命令或写管理员状态/全局缓存。
4. 新增`radiation_protection`一幕：新料/已用料的区别，主副手仪表、穿戴防护/头罩护目镜、累积剂量/脱离恢复、普通容器/合法封装桶/贮存架屏蔽和装配台封存用途。可用现有屏蔽混凝土示范隔离，不做具体材料/数量配方，不向真实HUD或辐射状态注入假读数。废料/残骸可直达事故页，仪表/护具/桶/架/屏蔽方块可直达防护页，沿正式已注册ID，不绑所有Create燃烧室。
5. 汽轮机通汽页修正外置泵必需的暗示，补排汽堵塞/周转满拒收、红石立即撤本机动力并区别断汽平均窗口；保三档搭建/效率原镜头。离心机/烧结炉删具体加工数量，只讲速度、加工和取料用法。贮存架改为现行核废料桶称呼并涵盖合法事故废料桶；屏蔽知识在防护新幕说明。换热器、装配台及其它仍准确已验镜头保持，不新增工业管网独立教学。

精确允许写集：J=`src/main/java/com/iksxh/create_nuclear_industry/ponder/`，R=`src/main/resources/assets/create_nuclear_industry/`，T=`src/test/java/com/iksxh/create_nuclear_industry/`。

- J既有：`P1PonderPlugin.java`、`P1PonderScenes.java`、`BoilerPonderScenes.java`、`CentrifugePonderScenes.java`、`FuelSinteringPonderScenes.java`、`DryStoragePonderScenes.java`、`TurbinePonderScenes.java`；只改上述教学内容、注册和相应中文职责注释。
- J新增：`ReactorAdministratorPonderScenes.java`（两幕）、`ReactorSafetyPonderScenes.java`、`RadiationPonderScenes.java`。不写生产设备、GUI、网络、事故/辐射实现类。
- `tools/ponder/reactor_scenes.py`追加管理员/事故布局；`tools/ponder/boiler_scenes.py`改汽路/追加安全幕；新`tools/ponder/radiation_scenes.py`。保标准库Python3.10可运行，实际解释器`D:/Thonny/python.exe`；不安装依赖或重写NBT框架。
- R模板仅改`ponder/high_pressure_boiler_steam.nbt`；新`ponder/{high_pressure_boiler_safety,experimental_reactor_safety,reactor_administrator_setup,reactor_administrator_operation,radiation_protection}.nbt`。其余19旧模板字节保持；修改生成器不得顺带改旧19份。新幕关键部件、载荷展示、剖面/镜头/高亮与生成器坐标一致。
- R`lang/{zh_cn,en_us}.json`只改本次对应Ponder键/新增键，字幕索引与Java默认文字同义/同顺序；不改物品正式名或其它功能词条。
- T`BoilerPonderContractTest.java`、`P1Ponder01ContractTest.java`、`TurbinePonderContractTest.java`仅适配真实新增幕/段及主动汽路，保原边界断言；新增`PonderCurrentFeaturesSyncContractTest.java`覆盖新入口/模板、双语、关键坐标及客户端显示隔离/时序的实际风险，不镜像机器算法。其它测试发现确需改动先报准确差异，不借机放宽。
- 报告/必要窄工具仍只写`build/reports/ponder-sync-20261104/`。禁止build.gradle、配置/配方、其它美术/音效/注册内容类、核心文档、世界、main正式包和Git写。

## 必要验证、审查与候选

教学与真实玩法一致性为首要标准。仅选择受影响Ponder合同测试、模板/语言/入口校验及增量编译；原机器功能测试和原三项JUnit基线失败不在本卡重跑/修复。确有无头教学场景构建入口时可运行受影响脚本验证注册和构造，不能把无头/静态通过当客户端播放通过。不新增照抄实现的测试；新增测试须覆盖本次入口、模板、关键坐标/可见性或字幕间隔的真实故障风险。

一次独立规格/质量组合窄审，整改只复审delta。稳定源由PM精确提交后执行者一次增量assemble，核源前后与候选包身份/排除。最终交一份按入口和场景列出的集中复看清单及候选；停在播放人工门，不自动合main，不扩展到下一功能。
