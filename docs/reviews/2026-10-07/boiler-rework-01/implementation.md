# EXT-B-BOILER-REWORK-01 核心实施交付

日期：2026-10-07。角色：执行者；本报告只记录未提交实现及证据，不改变任务状态或宣布人工验收通过。

工作区：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。开工与交付基线 HEAD：`d303913be852e345dd747f6e06b04ec039030125`，任务代码基线 `51dedfc`。主工程、用户客户端与用户世界均未操作；未执行 Git 写操作。开工已有 `logs/` 与 Python `__pycache__` 改动保留。期间 PM 写入的核心文档不属于本执行者改动。

## 实际使用的规则与技能

- 实际读取候选 `AGENTS.md`、`docs/project-governance.md`、已确认 proposal 及 REWORK-01 实施卡；采用整组已确认参数，不另增玩法决策。
- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：核对 DeferredRegister、能力生命周期、服务端权威与客户端隔离。实际版本仍为 MC 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82 / Flywheel 1.0.6。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：真实 GameTestHolder、既有 Gradle 测试域、当前 NBT、独立设备冒烟；复用已存在 JUnit 平台，不改测试框架。
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：核对 1.21.1 模型/语言/资源路径、透视窗口渲染；新口静态资源由任务2执行者交付，未覆盖其文件。
- 实际读取 TDD、systematic-debugging、verification-before-completion 技能。先用参考分区容量断言得到旧实现 16000 != 18000 的真实红测试；后续按项目治理5.1集中定向验证，未执行 clean、rerun-tasks 或全量 build。
- Create 原生控件和窗口调用以本地锁定 sources JAR 中 `SmartBlockEntity`、`ScrollOptionBehaviour`、`ScrollValueBehaviour`、`ValueBoxTransform`、`FluidTankRenderer` 为准，没有升级依赖。

## 实现与接口选择

### 明确几何与单一归属

`BoilerStructure.Form` 保存闭包围盒 `min/max`、隔层世界 Y、水汽有效格数、全部换热器/再加热段/四类端口/窗口及阀位置。以水区控制器内侧空气格寻边后做一次完整有界验证，支持默认5..11中的偶数和长方体、非居中隔层和任意合法面内位置。棱角只允许外壳；完整隔层由外壳/再加热段铺满；底面非边框换热器计入成员，不再从额外炉底层取热。

只登记已加载控制器的弱引用，不加载区块。部件放拆、炉腔邻块通知、配置快照变化及卸载触发失效；正常查询复用快照并检查区块可用性，没有每tick全体积搜索。重叠包围盒被拒绝；管道刷新、能力归属及窗口分区读取同一快照。

成型时对所有成员当前核库存/HU统一预检，再无外部流体回调地转入控制器并清空成员。冷凝工质未清空的成员拒绝接管。炉内成员在独立直列扫描和 Create 热回调前被排除，能力代次改变使旧独立句柄不能复活。拆炉后控制器保留其唯一账本；空成员可以恢复独立用途，既有锅炉库存不会复制回成员。

### 水汽热量与输出

`BoilerState` 保存水/汽/热液/冷液、`WaterHu`/`SteamHu`、加工尾热和冷却剂已付储备。`setGeometry` 决定容量，改变尺寸或降低配置不会裁切现存工质/HU。配置使用 `dimensionRange` 单范围项与 proposal 全部新参数；旧固定容量/暖炉键不再生效，跨字段无效或整数乘积溢出拒绝运行。

热液等量转冷产生HU，仅进入一个储备；收热受有效配对、成员额定、需求、热液及冷回流空间限制。成员已有储热也只转入一次。水侧预热后先补已有汽焓，再新增汽化；新汽携带原水热，唯一账本只支付目标汽焓差额。默认参考满水预热32400HU，冷水至普通汽0.8HU/mB、至SC汽1HU/mB，9000mB沸点汽再热需1800HU。满汽和缺水不阻止已有汽的付费再热。

实际温度、实际炉压和资格来自 Ew/Es，不按库存占比冒充炉压。两模式共用同一汽量和Es；切模式保留能量、撤销旧能力与自有运输压力、刷新管面，管外已有流体保留。降温失格拒绝SC输出。主动邻罐、原生Create管道抽取、外部抽取和泄放均逐次消费最新库存与实际比焓；同一口主动/被动共用额度，不同口各有256mB/t默认额度，保压余量全炉共享。

红石停止新收热；已付高于沸点的炉体/汽侧热可支撑有限余热。无收热时只扣显热、不凝水、不扣潜热。阀按0.9/0.8实际炉压启闭，堵塞时再热与新增汽化都不能推过开启保护线。拆件时不再汽化，只保留和自然冷却现有账本。

### 原生控件、显示与新口

控制器改为 Create `SmartBlockEntity`，前面上下两个独立类型的原生选项/数值行为分别控制汽种和当前模式保压。SC默认选择，普通与SC保压独立保存；服务端回调限制SC不低于资格门槛。无独立GUI。

窗口新增轻量BE，只同步当前窗口格内水位比例，没有能力或第二库存。客户端事件独立注册渲染器，专用服务端不加载渲染类。控制器护目镜显示尺寸、分区格数、有效配对、四库存、温度、真实炉压/保压、收热/产汽/排放及堵塞原因。炉内换热器只显示共享归属说明，避免沿用已接管的独立直列罐量和Create热级。

两新口固定ID、水平朝外注册、物品栏、采掘、掉落及规定有序配方已接入。热口横排 `PFC`，冷口竖排 `P/F/C`，各用1管道+1耐压接头+1外壳→1口。`boiler_heat_exchange_section` 保留ID与成本，显示改为锅炉再加热段。仅修改锅炉相关语言，静态比较确认其他语言键值保持。

## 文件范围

核心包：

- 修改 `boiler/BoilerState.java`、`BoilerStructure.java`、`BoilerControllerBlockEntity.java`、`BoilerControllerBlock.java`。
- 新增 `boiler/BoilerControls.java`、`BoilerWindowBlock.java`、`BoilerWindowBlockEntity.java`、`BoilerClientEvents.java`。
- 修改 `config/BoilerConfig.java`、`content/BoilerContent.java`。
- 按PM精确扩集修改 `content/ModCreativeTabs.java` 的两新口条目，以及 `CreateNuclearIndustry.java` 的两口搬移禁令和锅炉放拆/邻块失效通知。
- 修改 `heat/NuclearHeatExchangerBlockEntity.java`、`NuclearHeatExchangerBlock.java`、`HeatExchangerLine.java`、`HeatExchangerState.java`，仅锅炉接管及独立路径排除；未改汽轮机参数、冷凝规则或反应堆。

测试与数据：

- 修改 `src/test/java/com/iksxh/create_nuclear_industry/boiler/BoilerStateTest.java`，旧固定暖炉/旧存档迁移断言被当前合同测试替代；独立换热器账本16例保持并复验。
- 修改 `gametest/ExtensionBoilerGameTests.java` 与 `ExtensionConfigGameTests.java`，旧固定锅炉夹具替换为分区夹具；既有独立换热/冷凝/汽轮机冒烟通过调用原用例执行，不改其断言。
- 新增专用域 `data/create_nuclear_industry_boiler_rework/structure/boiler_empty.nbt`，复制既有锅炉空模板；不改构建脚本或测试运行框架。
- 修改中英文语言JSON与两个采掘标签；新增两个配方和两个掉落JSON。
- 任务2八件新口静态资源及其SVG工具见同目录 `assets.md`，本核心统一打包。

## 验证证据

原始日志目录：`build/reports/extension/EXT-B-BOILER-REWORK-01/`。

| 检查 | 命令/证据 | 结果 |
| --- | --- | --- |
| 参考容量红测试 | `test --tests ...BoilerStateTest.referencePartitionHasEighteenBucketsPerZone`；`red-reference.xml` | 旧16000容量真实失败，随后实现替换 |
| 首次集成编译 | `compile-first.log` | 4处旧配置测试夹具签名不匹配；同步新夹具后修复，未改游戏公式规避错误 |
| 定向JUnit | `./gradlew.bat test --tests com.iksxh.create_nuclear_industry.boiler.BoilerStateTest --tests com.iksxh.create_nuclear_industry.heat.HeatExchangerStateTest --console=plain`；`junit.log`、两份XML | 13+16=29例，0失败、0错误 |
| 首轮GameTest | 相同本批域命令；`gametest-first.log` | 实际13例、11通过、2夹具初值失败，保留日志 |
| 初值修正复验 | `gametest-fixture-corrected.log` | 13/13通过，正常保存退出 |
| 最终增强GameTest | `./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_boiler_rework -PgameTestDirectory=run/verification/boiler-rework-01 --console=plain`；`gametest.log` | 13/13通过，3.016秒断言，总24秒，正常退出 |
| 增量制品 | `./gradlew.bat assemble --console=plain`；`assemble.log` | 一次assemble成功，总2秒 |
| 资源与范围 | `artifact-validation.log`、`git diff --check -- src/main src/test` | 两语言JSON有效且无关键值保持；两口各四朝向、有序配方；JAR包含全部12件新口资源/数据；无差异空白错误 |

首轮两项根因：堵阀夹具以18000mB/16200HU误当P0.9，实际Ts1.5、P0.75；修正为17280HU并明确断言P0.9。SC管夹具14000mB/14000HU恰好Ts2且未供热，等待管道建立时自然散热令其失格；改为显式已付的14100HU，并保留低温汽不得进入SC管线断言。真实Create管道输出标准未放宽。NeoForge锁定Main只支持已注册域集合，现Gradle只暴露namespace，PM批准同域复验；未禁用用例或新增测试过滤框架。

最终13个真实用例覆盖：5³参考/四能力；11边界、偏心隔层、拒绝12；偶数长方体、隔层破洞/外壳补齐/非法棱角；四类双口SIMULATE与EXECUTE、同tick额度、汽保压与拆装旧句柄；成员冷热液与18HU储热一次性转移及独立旧句柄排除；600tick实际连续水/热液/冷回流/邻罐SC输出并对账；真实Create SC与普通管道各一例、模式切换外部流体保留；堵阀/红石/当前保存；合法非默认SERVER配置与非法范围；独立Create供热、独立冷凝、SC汽轮机原用例冒烟。

没有把仅编译、空域或启动进程当作通过。GameTest全部正常保存退出，不涉及终止客户端或测试进程。最终只调整炉内换热器护目镜显示分支后由assemble编译验证，热量事务与GameTest通过版本相同，不重复全套。

## 制品与待人工项

制品：`build/libs/create_nuclear_industry-0.1.0.jar`，**2172263 bytes**。

SHA-256：`20C92A42B3910EF0FD8C0C9C0683A83C63AB20D4C9CCDB2C1DDB9BDDF1170162`。

仍需PM一次规格/质量审查及集中客户端验收：尺寸/隔层搭建体验、原生双控件可见与命中、两模式保压保存、窗口分层液位、四朝向端口外观、充压及连续闭环、红石余热和堵塞恢复。没有客户端视觉通过证据；32只做配置/整数安全封顶，未宣称最大规模性能验收。未研究旧版本存档迁移；两台思索播放门保持原状态。

## 审查整改 R1 / R2 追加（2026-10-07）

已读取 `review.md` 两项 Important，并按PM派发仅修复生命周期恢复与重叠判定。追加实际应用 `superpowers/receiving-code-review/SKILL.md` 的先核对再实施流程；继续使用本报告已记录的 Minecraft 技能与锁定版本。未改温压、工质、配方或费用，未改构建框架、注册入口、其他设备及客户端操作。

### 最终实现策略和变动文件

- `boiler/BoilerControllerBlockEntity.java`：失败结构检查时记录控制器合法跨度内当时不可用的区块。范围按当前 `maxDimension-1` 且封顶31格，只查询 `hasChunk`，最多5×5个区块。失败缓存此后仅检查记录的坐标；**任一**记录区块恢复可用便重新置脏，所以附近无关区块持续缺失不会阻止恢复。已知快照丢失区块和首次寻边遇缺区块均进入同一记录路径，不需要每tick全体积扫描、全世界扫描或强载区块。
- `boiler/BoilerStructure.java`：两炉候选控制器的轴向距离封顶改为62（两个最大32格闭包围盒各贡献31格），再执行实际闭包围盒相交判断。发现实际相交时撤销对方已有快照；失败检查不相互逐tick撤销。结构/控制器变化可唤醒距离范围内的失败缓存，使拆改邻炉后能恢复；该通知不触碰已成型且不相交邻炉的能力或运输压力。仍只遍历本维度已加载控制器注册表，不扫描世界。
- 新增 `gametest/BoilerReviewGameTests.java` 两项本批namespace用例。没有保留最初试作的 `BoilerChunkEvents`：FULL暂退后恢复可能没有新Load事件，单纯监听不足以覆盖本次根因。

### 新断言及精确边界

R1使用合法 `[5,32]`、32×5×5炉体：相对于夹具中心chunk，X为15..46，控制器在chunk0，唯一底机/再热段在chunk1，chunk2只含外壳/空气/普通隔层且断言无BE。夹具远离模板1000区块，避免GameTest模板票据干扰；中心使用BLOCK_TICKING票据，远端使用独立FULL票据。撤销远端票据后实际观察 `hasChunk=false`，同时控制器保持 `current()`、中间区块保持FULL；连续查询确认进入失败缓存，旧水口拒收。恢复同一远端票据后，不改块、不重建控制器、不手动置脏，恢复成型及新能力；控制器实例相同、旧句柄仍拒收，123mB水、100mB汽、37mB冷液和80HU潜热保持不变。此初值没有高于沸点的显热，故自然冷却不应改写HU。

此用例证实的是**FULL可用性退降/恢复**，与生产 `Form.loaded` 的 `hasChunk` 判据一致，不宣称完整 `ChunkEvent.Unload` 或磁盘区块卸载：锁定 `ChunkMap.updateChunkScheduling` 与 `ChunkLevel.isLoaded/MAX_LEVEL` 显示距2区块还可由生成依赖保留，但已经退出FULL，因此未必触发Unload/Load。也不将默认11格邻区块自然卸载称为已复现。初次边界缺失使用相同失败记录代码，但本用例并未另建全新控制器专测启动时序。

R2使用合法 `[5,20]`，两座20×5×5炉体分别在X=0..19与19..38、控制器相距38。先断言两次独立inspect均合法、两控制器均可运行，再断言共享纯外壳面时两者都不成型。随后将第二炉改为X=20..39，断言真正不相交的相邻两炉均可成型。夹具给两端明确票据并等待可运行状态，不依赖模板随机世界起点。

两个改全局dimensionRange的新增用例分别在 `review_overlap`、`review_chunk` 独立batch；既有配置测试在 `config` batch，原12项在 `defaultBatch`。最终日志明确显示四批串行。配置与测试票据在成功、断言异常及显式超时分支恢复/移除，没有更改玩家配置文件。

### 整改验证历史

使用既有命令 `./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_boiler_rework -PgameTestDirectory=run/verification/boiler-rework-01 --console=plain`，锁定域实际注册并运行15项；未删用例、空筛选或增加过滤框架。由于真实失败而追加必要复验，全部历史日志保留：

| 证据 | 实际结果与原因 |
| --- | --- |
| `gametest-review-fixes.log` | 15项中13通过、2失败。Load事件版未恢复FULL暂退的失败缓存；初版62格广泛失效通知扰动原SC管路。随后改为缺区块可用性记录及精确相交撤销，未放宽管路断言。 |
| `gametest-review-corrected.log` | 原13项全部通过；两个新增夹具失败。R2随机模板起点下远端控制器未block tick，增加明确票据并等待；R1额外等待真实Unload，但距2生成依赖保留使其不发生，依据锁定源码改为精确FULL状态退降/恢复断言。 |
| `gametest-review-final.log` | **15/15通过**，3.786秒断言，命令22秒；两个`[boiler-review]`断言记录均存在，服务端正常保存退出。 |
| `assemble-review-fixes.log` | 修正后一次增量assemble成功，2秒。 |
| `artifact-review-validation.log` | 记录新制品字节数与SHA-256；本次锅炉源文件 `git diff --check` 通过。 |

复用原29例JUnit及原13例首次成功证据；本轮生命周期/重叠修复没有改变账本算法，未机械重跑JUnit或无关矩阵。最终GameTest之后仅修正BoilerStructure类说明，使其准确包含区块恢复触发验证；该注释由最终assemble编译覆盖，运行代码与15/15通过时一致。

### 整改后制品和交付状态

上文旧制品数据保留为审查前历史。**当前** `build/libs/create_nuclear_industry-0.1.0.jar`：**2179767 bytes**；SHA-256：`A93ED326EAB0F9BF471DBD9A6A9497D968D226028509AB30D0E3B09856B9640D`。

整改代码停止修改，交PM及同一审查者仅核对本次差异与新证据。未进行Git写操作、客户端启动/终止或人工验收；集中客户端验收与两台思索播放门仍保留。
