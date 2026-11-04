# PONDER-SYNC-01 执行交付

2026-11-04；执行者交付，供PM组合窄审，不宣布客户端验收。工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix`，分支 `codex/ponder-sync-20261104`，本轮读取/实施HEAD `f95a8442b800bfcd15f0253c94870abc3235bc5c`。原只读盘点见[AUDIT.md](AUDIT.md)。开工时已读取治理、STATUS、实际活动卡及四项指定Minecraft技能；版本仍为MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。

## 交付内容

在冻结写集内完成五个新幕，以及原锅炉、换料维修、汽轮机通汽、离心机/烧结炉数量表述、贮存称呼同步。未改生产机理、GUI/网络、配置、配方或注册内容类；未打开/搬动/修改世界，未执行Git写入。按PM要求先交源，**未执行assemble或改正式JAR**。

- 锅炉三原幕纠正5～16，保7³例炉。出汽幕改为连续工业管，去掉外置泵和马达并说明主动出汽；安全幕显示两个顶面阀、分别堵/通、排汽方向与每阀默认能力；说明红石停新收热、干烧损毁、超压阈值/恢复计时和危险拆除。
- 换料幕显示一次持新料右键，端口不出现中间空槽，旧件用手边物品提示返还；机械臂仍先取后放。补钢板维修燃料列/控制棒，并保余热和燃料不被维修重置的边界；无管理员时撤红石恢复目标的范围已限定。
- 管理员只从自动化接口进入两幕。先显露接口，再显露会挡住接口的相邻燃烧室；红石反相实际几何接到仪表，运行幕在讲两模式前已显示运行许可/低急停。三页、应用/启动、统一深度/恒功率、核热值班、部分缺料、保护复位和正常续接均为分段操作说明。
- 事故幕含停堆余热、危险拆除与实际外爆、高温/熔融物、可见左键扑灭火点；水实际相邻下移后的熔融物，熔融物在原位变为热残骸后再冷却，两处固体各保其显示载荷，最后示意采掘回收与封装。未以真实事故来驱动画面。
- 辐射幕对照新/已用/乏燃料，教主副手辐射计、穿戴护具/头罩、普通容器、屏蔽混凝土、装配台封装与贮存架；不向真实HUD/玩家状态注入读数。离心机和烧结炉已删具体加工数量句；贮存架沿现行核废料桶称呼。换热器、装配台原幕及其它未受影响镜头保持。

新增和较长改段已按中英文阅读量延长等待，仍统一正文结束后覆盖Ponder额外10tick淡出并另留10tick净空。界面只作燃烧室位置高亮与右键图示，不实际打开正式GUI或讲已移除的应力闭环。

## 字幕与模板索引

每行均有同ID的标题及连续 `text_1..N`；双语各 **187** 个Ponder键（25标题+162正文），键集合相同且无重复。新幕中文默认句与中文索引顺序逐条静态一致，英文同序；未改非Ponder语言键。

| 场景ID | 正文N | 模板方块数 | 模板处理 |
| --- | ---: | ---: | --- |
| experimental_reactor | 8 | 110 | 保原模板 |
| experimental_reactor_rods | 8 | 117 | 保原模板 |
| experimental_reactor_operation | 7 | 123 | 保原模板 |
| experimental_reactor_refueling | 8 | 116 | 保原模板 |
| experimental_reactor_safety | 9 | 281 | 新增 |
| reactor_administrator_setup | 8 | 286 | 新增 |
| reactor_administrator_operation | 7 | 286 | 新增 |
| enrichment_centrifuge | 8 | 105 | 保原模板 |
| high_pressure_boiler_build | 7 | 243 | 保原模板 |
| high_pressure_boiler_operation | 8 | 264 | 保原模板 |
| high_pressure_boiler_steam | 6 | 249 | 主动汽路更新 |
| high_pressure_boiler_safety | 10 | 243 | 新增 |
| steam_turbine_build | 12 | 720 | 保原模板 |
| steam_turbine_operation | 8 | 161 | 保原模板 |
| steam_turbine_efficiency | 6 | 161 | 保原模板 |
| nuclear_heat_exchanger_introduction | 3 | 28 | 保原模板 |
| nuclear_heat_exchanger_heating | 5 | 161 | 保原模板 |
| nuclear_heat_exchanger_boiler | 3 | 260 | 保原模板 |
| nuclear_heat_exchanger_processing | 4 | 158 | 保原模板 |
| nuclear_heat_exchanger_condensation | 3 | 154 | 保原模板 |
| fuel_sintering_operation | 3 | 51 | 保原模板 |
| fuel_sintering_automation | 3 | 64 | 保原模板 |
| shielded_assembly_placement | 3 | 66 | 保原模板 |
| dry_storage_usage | 6 | 30 | 保原模板 |
| radiation_protection | 9 | 195 | 新增 |

## 客户端隔离与坐标

- 管理员仅使用临时方块、红石画面和 `slider().setClientDisplayedValue`；不调用服务端调棒/管理员许可，不写AutomationState，不打开菜单或发命令。燃烧室 `(5,2,3)` 与接口 `(5,2,4)` 相邻，输出反相火把 `(6,3,2)` 经红石 `(6,3,3)` 接仪表 `(6,3,4)`。
- 残骸只用 `loadWithComponents` 读入Ponder客户端单件 `Payload`；不调用accept/acceptMolten或创建SavedData。熔融演示从 `(6,3,7)` 移至 `(6,2,7)`，邻水为 `(5,2,7)`（下方另有水帮助固体冷却），同位置原位凝固。临时事故火 `(8,1,7)` 没有绑定Lease/事故身份，灭火只改临时方块。
- 辐射只创建展示物品副本，已用标记使用无世界副作用的副本方法；架体沿既有客户端读入快照展示，不调用插入/采样/剂量方法。样品 `(6,2,3)` 与观察侧 `(1,2,3)` 间的屏蔽墙实际覆盖 `x=3,y=1..3,z=2..4`。装配台八格机体及底轴完整，平移后从属NBT主控位置同步。
- 锅炉第二阀 `(8,6,6)` 与首阀 `(6,6,6)` 均在顶面非棱边；遮挡分别放在各自正上方。说明干烧损毁部件前移开顶盖和内部隔层，再高亮底层换热器。

坐标/隔离合同是静态证据，未宣称客户端已经看见全部效果。新增火点与长句整改以后仅复验受影响合同。

## 实际验证

| 检查 | 本轮结果与证据 |
| --- | --- |
| Java版本/增量编译 | Java21.0.7；`compileJava compileTestJava` exit0/7秒。见[java-version.log](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/ponder-sync-20261104/java-version.log)、[compile-first.log](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/ponder-sync-20261104/compile-first.log)。有既有API/Gradle弃用提示，未称无警告 |
| 首轮Ponder合同 | `test --tests '*Ponder*'`，8类32项，32通过/0失败/0错误/0跳过，exit0/16秒；见[ponder-first.log](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/ponder-sync-20261104/ponder-first.log)及[first-test-xml](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/ponder-sync-20261104/first-test-xml/) |
| 后续delta | 火点及长段时序调整后，仅同步合同、锅炉、原反应堆入口、汽轮机四个受影响合同，18项全部通过，exit0/21秒；该命令包含所需增量编译。见[ponder-delta.log](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/ponder-sync-20261104/ponder-delta.log)及[delta-test-xml](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/ponder-sync-20261104/delta-test-xml/)。其余14项复用首次成功结果，不冒称第二次跑了32项 |
| 模板/双语/写集 | 25/25现存NBT解压字节等于当前生成函数内存编码；全部坐标唯一/在界/palette索引有效。19旧模板文件SHA256原样，旧模板仅出汽一份变化；双语无重复/无非Ponder改键，25路径精确匹配冻结写集。见[static-verification.log](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/ponder-sync-20261104/static-verification.log)、[static-verification.json](static-verification.json)、[verify_sync.py](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/ponder-sync-20261104/verify_sync.py)、[baseline-template-hashes.json](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/ponder-sync-20261104/baseline-template-hashes.json) |
| 差异格式 | `git diff --check` exit0 |

没有运行全量JUnit、原三项基线失败、机器GameTest/服务器/客户端，也未找到本项目现成的独立无头Ponder播放构造入口，未为本次扩建框架。上述合同和NBT核验不替代播放、视角/遮挡、动画或用户操作验收。

## 最终写集与源绑定

共 **25路径**：10 Java、3 Python、6 NBT（1改+5新）、2语言、4测试（3改+1新），精确清单与逐文件SHA256在[source-manifest.json](source-manifest.json)，该清单文件SHA256为 `074A633AB4401DDCF8CA014F95248CBBFCA9107E278889D18BB350E008934F34`。下列路径除此之外没有源改动；本目录的报告/日志不进入生产源。

- `src/main/java/com/iksxh/create_nuclear_industry/ponder/BoilerPonderScenes.java`
- `src/main/java/com/iksxh/create_nuclear_industry/ponder/CentrifugePonderScenes.java`
- `src/main/java/com/iksxh/create_nuclear_industry/ponder/DryStoragePonderScenes.java`
- `src/main/java/com/iksxh/create_nuclear_industry/ponder/FuelSinteringPonderScenes.java`
- `src/main/java/com/iksxh/create_nuclear_industry/ponder/P1PonderPlugin.java`
- `src/main/java/com/iksxh/create_nuclear_industry/ponder/P1PonderScenes.java`
- `src/main/java/com/iksxh/create_nuclear_industry/ponder/RadiationPonderScenes.java`
- `src/main/java/com/iksxh/create_nuclear_industry/ponder/ReactorAdministratorPonderScenes.java`
- `src/main/java/com/iksxh/create_nuclear_industry/ponder/ReactorSafetyPonderScenes.java`
- `src/main/java/com/iksxh/create_nuclear_industry/ponder/TurbinePonderScenes.java`
- `src/main/resources/assets/create_nuclear_industry/lang/en_us.json`
- `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`
- `src/main/resources/assets/create_nuclear_industry/ponder/experimental_reactor_safety.nbt`
- `src/main/resources/assets/create_nuclear_industry/ponder/high_pressure_boiler_safety.nbt`
- `src/main/resources/assets/create_nuclear_industry/ponder/high_pressure_boiler_steam.nbt`
- `src/main/resources/assets/create_nuclear_industry/ponder/radiation_protection.nbt`
- `src/main/resources/assets/create_nuclear_industry/ponder/reactor_administrator_operation.nbt`
- `src/main/resources/assets/create_nuclear_industry/ponder/reactor_administrator_setup.nbt`
- `src/test/java/com/iksxh/create_nuclear_industry/BoilerPonderContractTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/P1Ponder01ContractTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/PonderCurrentFeaturesSyncContractTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/TurbinePonderContractTest.java`
- `tools/ponder/boiler_scenes.py`
- `tools/ponder/radiation_scenes.py`
- `tools/ponder/reactor_scenes.py`

## 集中播放复看建议

1. 自动化接口按W：两管理员页，重点先露接口、任职燃烧室、反相路径、三页/应用启动、两模式及停机保护；检查字幕与顶帽/燃烧室图示不遮挡。
2. 换料端口按W：换料维修页的一次新料点击/旧件返还，机械臂分开的取放，钢板目标可见；运行页的红石范围限定。
3. 锅炉部件按W：原搭建/运行尺寸两句，出汽页确实无外泵，安全页两个阀可见/各自堵通及干烧、压力、拆除后果。
4. 反应堆部件、事故废料或残骸按W：新事故页的火点扑灭、水相邻、原位凝固再冷却、采掘与封装；区分热伤和辐射。
5. 辐射计/护具/桶/架/屏蔽混凝土按W：样品、穿戴、屏蔽墙与封存链，确认不会出现真实HUD或真实剂量变更。
6. 汽轮机通汽页复看堵排与红石停机区别；离心机、烧结炉、贮存架只复看改段，不重开其它原镜头。

源已在本次静态清单处冻结，交PM进行一次组合窄审。未打JAR，未自动合main/发布；后续按卡待准确源提交后再一次增量打包，停在集中播放人工门。
