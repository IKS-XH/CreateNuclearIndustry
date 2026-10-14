# REACTOR-ADMINISTRATOR-04 独立组合审查

2026-10-14。原独立审查者一次规格/质量窄审。已读取本树AGENTS、04活动卡、IMPLEMENTATION、11个功能路径及相关原调用链，实际应用 minecraft-modding、minecraft-testing、requesting-code-review/code-reviewer 和 verification-before-completion。用户已确认“原状态续接”，本报告按04新合同判断，不沿用01“重进一律停机”否定正常续接。

审查范围为 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-automation` 当前未提交源码，HEAD `ce8ca822ef2db4b09dde737b2430a396a68b62ee`，功能差异相对 `78acd5d`，新增文件另读。没有转派、Git/生产/测试/治理写入，没有运行测试、构建、客户端或世界。唯一写入为本报告。

## 当前结论

**两个 Important，需原执行者同批集中整改及本席差异窄复核，当前不进入唯一assemble。** 无Critical，未发现其余必改或需单列的Minor。正常保存续接技术路由在本卡批准边界内成立；问题集中在合法稀疏燃料表示与交接期间当前身份固定。

当前没有04新JAR，原879c3d72…包保持。即使整改后内部通过，也须原执行者唯一assemble、同次制品身份/测试排除补核后才能交本批候选；不因此合main、派发02或关闭01/01R/03及04客户端门。

## 已核实的实现与采用的取舍

- 正常停服识别采用锁定NeoForge的 ServerStoppingEvent，加同一server实际isStopped；单独isStopped或单独事件标记不够。`ReactorStructureLifecycle.java:36–47` 捕获并dirty当前已加载owner，正常卸载不覆盖最终意图，ServerStopped撤销临时标记。已对照保存的实际MC runServer及ServerLifecycleHooks字节码：正常路径先Stopping，再stopped/stopServer；异常路径没有该Stopping入口。普通autosave写NormalSave=false是合理的正常退出与异常/运行卸载区分。
- `ReactorAutomationResume.java:45–78` 的13字段本版本信封保存模式、目标、年龄与成员坐标，不保存generation/epoch授权、观察租约、nonce或支付票据。decode只生成无运行权的加载ARMING；FAULT原因、明确STOP、已保存SCRAM分别保持。新read重新生成owner身份、清热票、旧绑定/样本/显示窗口和调节去重时刻。`Instrument.java:1805–1836`。
- 加载冻结发生在完整核循环入口，并覆盖独立棒推进入口；不先全插再提棒、不模拟离线时间。当前结构、真实燃料端口投影、实例与原逐棒预算通过后，才给有限新反馈。等待沿现行armingTimeoutTicks，重复getter/tick不延长窗口，缺区块不强载。`Resume.java:92–95,130–180`、`Instrument.java:728–755`。
- 反馈getter的currentBudget不读红石，避免共同导体递归；正式allowSettlement仍读真实hasNeighborSignal，高输入冻结结算，持续高/超时转原保护。反馈不是KINDLED或免费核票，不恢复旧15/租约。采用本卡允许的“初始化反馈先高后低”路由，不新增第二种红石权威或抹掉保存SCRAM。`Resume.java:121–128,166–180`、`Instrument.java:459–469`。
- 零备用/零缓存者预算与首次正式结算调用同一原控制推进、裂变、维护与冷却入口；有预警先用原逐棒安全基准。首次费用只能在原燃料端口和snapshot事务提交后出票，足费后才恢复原运行mode/age；失败撤权，不继续依靠预测预算。外部正常燃料仍按原实际供能路径工作。`Resume.java:134–140,182–191`、`HeatSupply.java:62–83`、`Instrument.java:781–851`。首付后重复commit受原committedAt去重，不二次扣费/出票。
- GUI/协议未改。待续接APPLY在接口写入前原子拒绝，START/RESET被现有phase拒绝；STOP通过setAutomationState清pending，再走原全插、epoch更新及无条件notifyPermission，能撤销反馈false→false的特殊窗口。Resume.fail也显式通知临时反馈下降沿。旧菜单持旧owner对象、generation及nonce仍不能复活。`PortBlockEntity.java:37–43`、`Instrument.java:148–162`、`Resume.java:114–118`，对照原Commands/Menu身份合同。
- 无权STOP/FAULT仅在read建立有限初扫窗口，首扫之前暂缓核结算；有效绑定后继续原停止分类。运行结构变化和真实卸载依旧按原保护，而非视为正常退出。未任职者decode为原initial、无加载session，原热工与0维护路径保持。下面两项遗漏不能由既有11场景通过替代。
- 新增公共职责、单位、服务端边界、保存/红石/首付顺序注释为中文；原热工公式、配置、GUI/资源美术/PI未改。11路径在卡的精确写集内。

## 必改发现

### Important 1：把合法未装料空燃料列误判成结构改变

- **位置：** `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationResume.java:108–112`；首扫拒绝入口为 `blockentity/ReactorInstrumentPortBlockEntity.java:346–352`。
- **实际触发：** 合法结构有至少两根物理FUEL列，只给其中一列装料并正常运行，另一列从未装过料。正常退出后以本版本NBT重建/首次重扫，或保存为STOP后重进。
- **问题与影响：** matchingColumns强制“全部结构FUEL位置 == snapshot.fuelColumns.keySet”。原生产明确允许稀疏燃料状态：`Instrument.java:1455–1456` 只为已有状态或真实非空物品保留key；`978–987` 只要求现有状态属于正确结构列。NBT编码/withoutFuelAssemblies也只保留已有key，不补造空列。该合法机组重进因此被报structure_changed、全插并FAULT，违背正常原状态续接，明确STOP也会被升级故障。当前新真实夹具都只有一根已装料FUEL，未覆盖此输入。
- **最小保护边界：** 不要补造未装料FuelColumnState，也不要只把相等改成任意subset。可复用本版本已保存的完整非权威扫描列图 `BreakSafetyFuelColumns`，核对全部物理FUEL位置；另保留已有燃料状态key的归属、完整控制棒映射，以及hydrate对每个真实已加载换料端口/合法物品的检查。
- **已核数据流：** `Instrument.java:366–370` 从扫描全部FUEL生成集合；`1786–1794` 随BreakSafetySurface保存；`1823–1836` 读档恢复并限制坐标在尺寸内。首扫核验346～352发生在368覆盖当前集合之前。应在覆盖前验证，并将首次已验证布局作为此加载会话固定副本，避免后续扫描覆盖旧证据、自动认可真实新增/删除列。证据缺失/坏数据安全拒绝，不扩旧版本迁移。补合法稀疏运行/STOP及真实列改变拒绝的必要代表。

### Important 2：反馈建立后结构代际变化，仍能沿旧绑定首付

- **位置：** `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java:321–327`；`automation/ReactorAutomationResume.java:130–132,158–165,185–190`。
- **实际触发：** 正常重进已完成首扫，Resume已捕获当前接口/燃烧室并发布反馈，尚在等真实降沿；此时玩家把一处合法侧壁改成额外冷端后触发正式重扫。尺寸、燃料与棒列、自动化接口和燃烧室对象不变，扫描仍合法，但ports合同变化使owner generation更新。原结构允许多个冷端（至少一个），不是非法结构夹具。
- **问题与影响：** updateStructureCache对pending跳过原撤权，却仍更新generation。Resume只在首次成员引用为空时写automationBinding，checkBudget只比引用，不核候选binding/current generation；故新预算仍过，低沿后可用旧binding完成首笔结算并恢复mode。Bridge随后因旧binding与新candidate不同输出0，下一tick普通Service又重绑定为STOP/全插：既没有在真实结构改变点直接保护，也没有正确续接原状态，出现一次旧代际首付及意外停机。
- **最小保护边界：** 首扫建立完整当前身份后，将这一新身份/结构代际固定于加载会话；之后真正的结构代际或成员变化立即fail、全插、撤反馈，不偷偷重绑定继续。候选/owner/binding generation必须一致，不能等首付后由普通Service发现。首次建立身份前的正常加载等待仍按现有有限窗口，不把首扫本身当运行结构故障。补在反馈已建立、真实低沿尚未核验窗口内的结构改变代表。

## 证据、范围与未跑边界

本席读取现有日志/XML、18冻结材料及15最终保存摘要，不执行任何测试。`18-review-input-manifest.json`实哈为 `f0e0cae8655bcaa4ec7a7212c763bb1244b9cf06f5a052e16641fb6b43d2510a`，本席逐项核当前11功能路径与该清单0漂移；7现有文件差异及4新增文件均已读。18的main输出class/资源含开发测试，不当正式JAR载荷数。

| 原证据 | 本次采用的结论 |
| --- | --- |
| 01-nbt-real-red.log | 旧生产真实NBT/BE重建两项RED；自然保存与业务失败分列。不是独立进程重启。 |
| 02-checkpoint-red.log、04-resume-directed-green.log | 新纯合同RED及初版29/29。29属于初版阶段，不冒称最终全套。 |
| 06/07 | 保存STOP/SCRAM分类的纯RED/GREEN。 |
| 08-expanded-real.log | 共同真实导体getter递归属于生产行为失败；STOP未实际采样SCRAM属于夹具错误，两者分别记录。09消除递归后10/10，不冒称最终STOP证据。 |
| 10～13 | 10轮11/11之后的最终region只读发现STOP首扫变FAULT，12原证据保留；13延迟首扫真实RED10/11。前轮通过未覆盖最终持久化缺陷。 |
| 14-stop-first-load-green.log | 当前11/11，23秒自然exit0；日志有三维度全部保存，含真实反相线、两BE顺序/零备用零缓存首付、逐棒差异、CONSTANT年龄、菜单STOP/APPLY、缺件/缺冷/持高、运行BE卸载回调及未任职代表。没有包含本报告两项新增触发。 |
| 15-final-save-summary.json及原region材料 | 实际正常ServerStopping后的最终region中健康MANUAL为NormalSave1/SCRAM0，明确STOP仍STOPPED/NormalSave1/SCRAM1，hot_trip FAULT原因保持。CONSTANT场景后来另测autosave/未任职，最终STOP不能当恒功率最终保存证明。 |
| 16-stable-directed.log / 16-unit-xml | 当前受影响Resume6＋Lifecycle2，共8/8，0失败/错误/跳过，17秒exit0；不与初版29相加。 |
| 17-stable-class-output.log | 最后仅中文说明补充后compileJava，2秒exit0，非assemble。 |

四个证据层级明确区分：

1. **已跑NBT/BE重建：** save/loadStatic、真实替换BE/onLoad、正式重扫与tick；reload夹具直接prepareNormalShutdown以取得信封，不能冒充进程正常退出。
2. **已跑实际正常停服最终保存：** 14自然结束触发真实事件，15只读本轮最终region；证明最终信封/停止分类。
3. **已跑直接原生运行BE卸载回调：** server仍运行且单独标记不足，FAULT/sample_lost、全插和NormalSave=false成立；并未由ChunkManager真正释放/重载区块。
4. **未跑：** 真正ChunkManager卸载再加载、独立进程restart、用户原世界runClient/GPU/键鼠与实际续接。这些保留人工门，不用前三级证据追认，也不要求机械重复旧热工或全环境兼容。

预算、首笔正式核事务、真实输入均走生产路径；核热费用不足及特殊燃料原规则沿03未改合同复用，不称本轮全部重跑。14本轮未出现Saving worlds挂起；03旧失败/保存线程仍保留，不能因04自然退出改写历史。

新resume_empty为111 bytes的原模板逐字节别名，清单SHA `71343a478b844ae6b0044f0f2142fae309499d727cfc731d6e5bbf9f6bc104e4`。build.gradle仅新增本域GameTest class及该NBT精确排除，旧域/依赖/流程不改；正式排除与新JAR身份须唯一assemble后同次确认，当前不提前宣称正式无泄漏。

## Declined to judge

空。未把合理使用者的稀疏装料或等待期间结构操作因规格未列触发而搁置。旧版本存档迁移、PI、整机新门均未扩展；真实卸载/restart/GPU作为未验范围明确保留。

两个Important已提前通知PM并获纳入同批。请原唯一执行者集中整改，只补实际两类触发及受影响证据，再交本席同一次窄复核。正常保存续接授权保持，main及02不推进。

## 同一次审查的两项整改差异窄复核（23稳定源）

2026-10-14。前文为18初审历史，两项原发现完整保留。本席实际重读04卡精确扩卡、IMPLEMENTATION整改尾部及19～23材料，只核两项实际差异，没有重审整包、转派、执行测试/构建或启动世界，唯一写入仍是本报告。

**两个Important均关闭，整改未产生新的必改；当前稳定源码可交原唯一执行者进行本批唯一assemble。** 此时尚无04新JAR，879c原包保持；不能提前称04制品交付完成。打包后仍由本席在此报告同次补核来源/身份及正式测试排除，不另起审查或重复测试。真实ChunkManager卸载/reload、独立进程restart、用户原世界/GPU与01/01R/03人工门保持未验，main和02不推进。

### Important 1关闭：稀疏核账本与完整保存布局分别核验

- `Instrument.java:1833–1852` 对04续接另行严格读取本版本BreakSafetyFuelColumns：要求有效表面/相同尺寸、真实List/Compound类型、坐标INT、仅X/Z两个键、范围内且无重复；缺失/坏数据得到null。原拆除防护仍使用既有容错副本，未把严格续接规则扩成旧版本迁移。
- `Instrument.java:87–89` 的immutable automationLoadedFuelLayout保存本次read的完整物理FUEL证据，普通扫描覆盖breakSafetyFuelColumns不覆盖它。`Resume.java:110–125` 比较全部当前物理FUEL与该保存集相等，已有核账本key只须属于集合，控制棒全映射仍强等；不补造未装料FuelColumnState。首扫验证后Session保存immutable布局副本。
- `Instrument.java:352–357` 的首次pending与inactive STOP/FAULT都核完整证据，取消了空核状态绕过。原hydrate对每个真实换料端口、BE加载与合法物品的检查未改。
- 新ReviewWindows嵌套holder的 `ReactorAdministratorResumeGameTests.java:255–288` 实际场景先构造物理FUEL=2、仅一份真实燃料且snapshot仅一个key；正常重建后MANUAL和STOPPED分别成立，24tick仍无空key/组件。STOP场景再串行验证missing/duplicate/wrong_type/outside四种坏证据，都FAULT且无receipt。合法状态在坏证据循环前明确记录，末尾旧对象不能当合法STOP失败证据。
- `ReactorAdministratorResumeGameTests.java:293–302` 真正移除从未装料的整列，几何仍合法却以保存FUEL=2/当前FUEL=1拒绝，FAULT、全插、首费0。两新增纯合同另证不存在的核状态归属及缺控制映射不能通过。

**裁定：关闭。** 当前合法稀疏表示可续接/保持STOP，完整保存布局又能拒绝真正列变化，没有弱化成任意subset或伪造空状态。

### Important 2关闭：首扫后的新代际与完整绑定固定

- `Resume.java:117–120` 首次完整列图验证后固定当前新generation；`hasVerifiedStructure`与 `Instrument.java:324–331` 在之后扫描合同/原点改变时先fail、全插并撤反馈，再更新generation。首次合理首扫仍能建立新身份。
- Session保存currentBinding及具体成员；`Resume.java:145–155` 核同一个owner实际对象、固定generation、当前candidate/owner binding完整相等、当前已加载接口/燃烧室引用。预算、反馈和首付完成共用此身份检查（203、211行），不在等待中偷偷换新binding继续。
- `ReactorAdministratorResumeGameTests.java:306–319` 先建立反馈15但正式runPermission=false的真实等待窗口，再将合法侧壁换为额外冷端并正式重扫；实际generation改变后立即FAULT/structure_changed、反馈0、全插、首费0，后续真实低沿不能恢复。没有等普通Service在首付后才发现旧代际。

**裁定：关闭。** 结构真实改变被保护点及时拦截，首次核结算不再沿旧bind恢复mode。

### 本轮原始证据及写集

| 原证据 | 采用范围 |
| --- | --- |
| 19-review-windows-red.log | 四场景均为对应行为RED：稀疏运行/STOP误拒、空列删除误认可、反馈期代际变化未撤销；29秒Gradle1/Java4，三维度自然保存。首次目录缺server.properties告警不是四断言失败根因。 |
| 20-review-windows-green.log | 四场景首次4/4；合法STOP断言之后串行坏证据的末尾FAULT不冒称合法STOP失效。 |
| 21-review-directed.log / 21-unit-xml | 当前Resume8＋Lifecycle2，共10/10，0失败/错误/跳过，22秒exit0。原16八项不相加为新套。 |
| 22-stable-review-windows.log | 最终同四场景4/4，21秒自然exit0，日志有三维度全部保存；明确acceptedPhaseBeforeCorruptChecks=MANUAL/STOPPED、坏证据拒绝、实际列删除首费0及代际反馈撤销。没有重跑原11。 |
| 23冻结 | 当前12功能路径及六净路径变化来源，非新JAR。 |

本席实核23清单SHA `16c91fc8b516f766ee0d4e36eeaef8acba5fc01b91bca935ffe92f6f29941fe3`、源ZIP SHA `fc62c3e071e75a3af20055aa2927fedba405adadb01c9aa01bdbdaf479a58d08`，当前12功能来源逐项与23清单0漂移。对照18清单仅六净路径变化：Resume、Instrument、原GameTests、ResumeTest、build.gradle及批准review_empty别名；其余04来源保持，未触及未任职、原热工/GUI/配置/支付规则入口。

精确扩卡在04卡第43行：原GameTests的ReviewWindows嵌套独立四代表，目录review-game；原setup(h)仅委托setup(h,false)保持一FUEL夹具，原11业务断言保留，未机械重复运行。新review_empty与resume_empty均111 bytes，实际SHA均为 `71343a478b844ae6b0044f0f2142fae309499d727cfc731d6e5bbf9f6bc104e4`。build.gradle:148只加这一NBT精确排除；142原class通配已覆盖ReviewWindows嵌套类，旧域/依赖/流程保持。

原11真实、15实际正常停服最终region与其他热费/保存证据只在未改范围分阶段复用，不合成纠错后新15项整套。稀疏STOP场景最后故意加载坏证据，因此本轮最终region不替代15的正常STOP最终保存证据。未跑的真实区块释放/进程restart/GPU界限与Declined to judge空列表均保持。下一步只有唯一assemble及同次制品补核。

## 同次唯一assemble后的制品补核（当前最终结论）

2026-10-14。本席只读取24/25日志、清单及实际ZIP载荷，核制品身份/排除，不复审源码、不运行任何测试/构建或世界。原两个Important与23差异复核保持关闭，历史发现/阶段证据不改写。

- `24-final-assemble.log`记录1秒exit0：compileJava/processResources均UP-TO-DATE，只有jar实际执行；本批唯一assemble。该构建结果不冒充额外测试。
- 本席实际核 `build/libs/create_nuclear_industry-0.1.0.jar` 与 `build/reports/reactor-administrator-04/25-final-candidate.jar`，两者均3199564 bytes、SHA-256 `dc82b20a19f17e81db2fd4e9efa69ed2b861fd703deaca4553ac7dfcbd6a7b05`。
- `25-final-artifact-manifest.json`实际SHA-256 `17ee65b0a6c1f8074e92435f7ed8c74263d0fe15d432c59c6a854570c386458d`。其中12来源与23已审冻结清单逐项0差异；25验证记录全部编译/处理输出载荷匹配JAR，唯一生成入口为META-INF/MANIFEST.MF。38用户世界文件/27配置文件载荷保持，不重复全存档哈希或改写用户目录。
- 本席实际读取正式ZIP，非目录文件2208：661 class＋1546非class资源（不含生成MANIFEST）＋1 MANIFEST。开发输出667 class/1552资源按实际排除6 class和6 NBT得到该正式载荷数，不能混用开发数。
- 正式ZIP中 `ReactorAdministratorResumeGameTests` 及所有嵌套class（含ReviewWindows）、resume_empty/review_empty两个NBT文件载荷均为0泄漏。25排除清单准确保留原四个开发测试class/四个旧模板排除，加本批两个class/两个模板；没有删除旧测试域。两个本批namespace各保留目录与structure目录，共四个空ZIP目录占位，另记而不当正式NBT模板。
- 相对保存的879c，25文件载荷差异只新增Resume/Loaded/Session三生产class、修改9个既有class（含同源Instrument/Lifecycle伴随类）；无删除、既有资源0改变。12源码、批准模板/build排除与这些差异相符，没有美术/GUI/热工/配置或其他来源夹带。

**制品补核无剩余必改，当前dc82b20a…候选可交04续接与原集中客户端复看。** 此为同一次组合审查及补核完成，不表示真实ChunkManager卸载/reload、独立进程restart、GPU/用户原世界、01/01R/03人工门通过。原11与纠错四代表不合成新15项整套，main功能不集成，02不派发。PM可按既定权限净提交已审12源、批准候选JAR及交付文档；本席未执行Git操作。
