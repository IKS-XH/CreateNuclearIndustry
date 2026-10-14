# ART-TURBINE-10B 客户端消费者报告

日期：2026-10-12。角色：客户端执行者；唯一工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，HEAD `2ae83b9c9d355c5229f973849e04c449dea0ceb5`。本节为第一阶段只读定位，尚未写生产 Java、测试代码或正式资源，也未运行 Gradle、GameTest 或游戏。

已实际读取主目录最新 `AGENTS.md`、治理1.2/5.1/5.2、本树 `ART-TURBINE-10B.md`，并使用 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`；另读取本卡要求的 `writing-plans`、`subagent-driven-development`、`verification-before-completion`。依据项目权限，不子委派、不 Git 写、不另写计划，验证按本批受影响范围进行。

## 已核当前链

- `TurbineRotorBlockEntity` 仅静态锚点；`TurbineRotorRenderer` 仅在 `LOCATED` 时绘制 `rotor_blades_d3/d5/d7`，没有角度或速度源。当前朝向 yaw 为北0、东-90、南-180、西-270度。
- 成型转子静态模型为 `rotor_axle`；作者已确认10B候选将其改为空 `elements`，三档 partial 包括完整轴、轮毂和薄叶。成型输出 `output_shaft_front/rear` 保留固定箱座/外圈，新增 `output_shaft_front_rotating/rear_rotating` 仅轴/内圈。未成型沿用旧三个完整模型ID。作者候选尚未安装，具体资源写集由负责人落卡。
- 输出轴继承 Create `GeneratingKineticBlockEntity`，没有独立 renderer，也未覆盖 `getSpeed()`。其生成转速不能代替实际 Create 网络转速。
- `TurbineControllerBlockEntity.refreshView()` 的 `viewRpm=ledger.rpm()`，`TurbineState.rpm()` 直接返回配置 `settings.rpm()`；裸 `TurbineView.Rpm` 不是可靠停转依据。此发现已交负责人转主PM。转子必须等待实际只读接口 HANDOFF，不调用服务端 `ownerForPart`、不猜近邻轴。

## 锁定依赖核实与接口中立方案

`gradle.properties` 与 `build.gradle` 锁定 MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；未改变技术栈。

- Create `KineticBlockEntityRenderer.getAngleForBe()` 为 `(renderTime * getSpeed() * 3 / 10 + offset) % 360` 再转弧度。1 RPM对应0.3度/client tick；`SuperByteBuffer.rotateCentered` 使用弧度。直接复用该绝对时间乘新速会换速跳相，因此不作为本动画相位源。
- Create `getSpeed()` 在动力网过载或世界 tick 冻结时返回0，其余返回真实同步 `speed`；`Speed` 由原生 write/read 同步。输出 renderer 只消费此入口，不读取 `getGeneratedSpeed()`、配置RPM或机组额定RPM。
- `AnimationTickHolder.tick()` 在 `Minecraft.isPaused()` 时只增加独立 pausedTicks；普通 getTicks 不包括 pausedTicks，计数1728000取模。getRenderTime使用 tick+timer partial，PonderLevel另有时间域。
- 已读与锁定 NeoForge ClientHooks 内容一致的本地 Minecraft/DeltaTracker 源：`ClientTickEvent.Pre` 和 `Post` 都在 pause 判断之外触发。Post不能视为“世界必定推进”；自有纯客户端Clock须显式 gate `isPaused()`，暂停也冻结最后一次 partial 时间。
- 纯相位辅助采用 `旧角度 + 旧RPM * 0.3 * 时间差` 后锚定新速度；零速立即停止、负速反向，不增加惯性或动力规则。客户端Post对已登记且仍可靠的弱引用轨道逐tick采样，防止离屏期间以新速度追算整个间隔。renderer仅采样同一时钟和身份；同机组转子列共享同一owner/generation相位。缺失、失效、世界/区块卸载或代次变化撤下旧相位，未知恢复从静止fallback开始。
- `TurbineStructure.at()` 的localZ指向 `facing.getOpposite()`。Create速度符号沿世界正轴：转为localZ时北/西乘+1、东/南乘-1，再按既有yaw映射。SBB实际为矩阵右乘；调用顺序yaw后localZ roll，得到 `Yaw * Roll`，几何先绕localZ中心(.5,.5)旋转，再映射世界朝向。
- `CachedBuffers.partial` 经 `SuperBufferFactory` 创建 `ShadeSeparatingSuperByteBuffer`，其真实rotate同时右乘pose/normal矩阵。继续使用既有BER+CachedBuffers路径，不套用默认Kinetic renderer的Flywheel visualization早退，也不增加visual注册。
- 原转子 `new AABB(pos).inflate(diameter/2)` 已包围10B的R上限1.1875/2.1875/3.1875与Z=0..1，可保持；输出旋转轴Z=0..1，固定板不进入动态提交。

## 建议精确客户端与测试名单（待负责人追加授权）

| 动作 | 文件 | 职责 |
| --- | --- | --- |
| 修改 | `src/main/java/com/iksxh/create_nuclear_industry/turbine/client/TurbineClientEvents.java` | 提前初始化三个转子/两个输出partial，注册既有输出BE renderer；客户端Clock、弱身份轨道与世界/卸载清理 |
| 修改，等HANDOFF | `src/main/java/com/iksxh/create_nuclear_industry/turbine/client/TurbineRotorRenderer.java` | 绑定实际可靠转子显示接口、同owner相位、完整partial旋转；不读服务器定位 |
| 新增 | `src/main/java/com/iksxh/create_nuclear_industry/turbine/client/TurbineOutputShaftRenderer.java` | 按END选择动态轴/内圈partial、真实getSpeed、localZ/四向映射、静态fallback |
| 新增 | `src/main/java/com/iksxh/create_nuclear_industry/turbine/client/TurbineAnimationVisualState.java` | 纯客户端Clock/累计相位与身份撤销，接口中立，不持有服务器/NBT状态 |
| 新增 | `src/test/java/com/iksxh/create_nuclear_industry/turbine/client/TurbineAnimationVisualStateTest.java` | 累计单位、变速连续性、停转/反转、同identity同相、暂停partial冻结、撤销/恢复 |
| 新增 | `src/test/java/com/iksxh/create_nuclear_industry/turbine/client/TurbineAnimationRendererTest.java` | 实际生产变换/提交入口的四向矩阵与符号、真实getSpeed语义、partial/固定模型分界与局部引用合同 |

可先实施接口中立辅助、输出renderer和事件注册；转子renderer待主PM实际API后最后绑定。负责人需先落具体名单；本报告不自行扩大权限。生产与测试手写注释均使用中文。

## 后续必要检查与证据

全部素材安装及API到位后，由本消费者唯一持有一次相关 `test --tests ...TurbineAnimationVisualStateTest --tests ...TurbineAnimationRendererTest` 与必要增量 `assemble`。不加clean/rerun，不重复服务端、全量或旧存档矩阵；主PM前置证据复用。未运行这些命令，不能将只读核实记为测试通过。

原始本地依赖反编译与暂停源片段保存于 `build/reports/art/ART-TURBINE-10B/client/`：`KineticBlockEntityRenderer.javap.txt`、`KineticBlockEntity.javap.txt`、`AnimationTickHolder.javap.txt`、`PonderClientEvents.javap.txt`、`PonderClient.javap.txt`、`Affine.javap.txt`、`Rotate.javap.txt`、`CachedBuffers.javap.txt`、`SuperBufferFactory.javap.txt`、`ShadeSeparatingSuperByteBuffer.javap.txt`、`DefaultSuperByteBuffer.javap.txt`、`SuperByteBufferBuilder.javap.txt`、`Minecraft-locked-source.java.txt`、`DeltaTracker-locked-source.java.txt`。这些是只读API证据，非构建或游戏验收。

## 接口中立实现交付（同日，负责人追加五路径授权后）

本卡已追加五路径实施权限，现仅修改 `TurbineClientEvents`、新增 `TurbineOutputShaftRenderer`、`TurbineAnimationVisualState` 和表内两测试。原 `TurbineRotorRenderer` 及全部 BE/Controller/NBT/网络/服务端代码继续只读。

- MOD总线在模型准备前初始化partial，注册既有输出BE renderer，并在模型重载后清相位；嵌套Game订阅只负责ClientTick.Post、世界和区块卸载。暂停显式冻结tick及partial，暂停期间仍撤销无效身份。
- 输出轨道key为打包坐标，value仅弱引用BE以及值类型朝向/端位/tick；没有反向强持BE/世界的闭包。弱世界身份切换、BE替换、方向/端位改变、卸载和超过20个未渲染tick都撤销；仍保留的离屏轨道每tick采样真实getSpeed，防新速追时。未知/Ponder锚点不会按同坐标误清当前真实BE轨道。
- 累计核心以旧速度推进后锚定新速；两个生产renderer所需的中心旋转链按锁定SBB真实矩阵约定实现。此阶段仅输出轴已接入，转子仍是原静态renderer，未编造任何转子API。
- 两测试先编写，再实施生产代码：共13个待运行用例，调用实际累计、事件边界与SBB提交入口，实际PoseStack/Affine负责变换；世界夹具只替代外部已加载查询与最终提交，不以源码字符串替代行为。`getSpeed()`使用真实OutputBE，覆盖生成源为0时网络仍可转、原生过载/冻结、四向、变速、暂停和身份撤销。

本阶段遵照负责人“全部素材/API未齐前不Gradle”，未进行RED/GREEN运行、编译、assemble、GameTest或游戏，因此这不是TDD运行证明或通过报告。实际阅读 `test-driven-development` 及其 `writing-good-tests.md` 后，以项目授权的集中运行边界为准，仅提前写测试。共享接口尚未HANDOFF，素材尚未安装；后续由负责人落完整绑定写集后继续。

## 现有轴线状态的非常窄只读可行性判断

结论：严格轴线匹配能定位几何上的两端轴，不能替代本卡要求的可靠机主/世代/有效性显示接口；仍需实际HANDOFF。本轮仅重读 `TurbineAssembly`、`TurbineStructure`、`TurbineControllerBlockEntity` 的相关边界，没有设计或实现另一套客户端定位器，也没有改生产代码或运行Gradle。

“只沿当前朝向、有界且连续匹配同向同直径Rotor，并要求两端为正确END的Output”与猜近邻轴不同：它可排除非共轴近邻、空隙及中途端轴，确定一个几何轴列。但现状态的实际职责与运行资格分离：`TurbineAssembly.completeAxis`（122行起）仅要求两端轴和连续Rotor；`stylePart`（239行起）直接写LOCATED/朝向/直径/END，不核定或写FORMED。`TurbinePartBlock` 60行明确搭建布局与运行Form分离。

不能将轴列当作当前权威关联的具体边界：

- **旧标志窗口：** Controller `invalidateForm`（64行起）立即令activeForm=null、ledger.stop、epoch++并forget/sendData，明确将旧外观留给下一owner tick；轴线状态可以暂时完整。即便额外要求每格FORMED=true，客户端逐格更新也不是同一世代的原子快照。
- **缺区块及机组失效：** 缺轴线区块可用hasChunkAt保守拒绝且不强载；轴线区块都可见仍不能证明侧方壳体、控制器、接口、空腔和服务端BLOCK_TICKING票据有效。`TurbineStructure.scan`/`quickLive` 的有效性范围超出轴线。
- **误关联与拼接：** 完整且彼此隔开的机组不会因严格轴线匹配自动跨越中间Output；风险在局部拆建、逐格同步及同向同直径旧标志组合时，几何轴列看似一致，客户端没有owner认领信息证明它已作为这一机组重新成立。`TurbineStructure.unique` 的权威重叠认领也不在这些字段中。
- **端轴速度：** 原生getSpeed是该Output真实网速，但不携带Rotor归属；失效机组撤销内部连接后，端轴仍可能受外源带动。轴速非零不能补足Form有效性。
- **多列相位与更换：** 所有列若解析出同一几何端点，可形成临时同相键；同坐标重新成型、原端轴对象继续存在、端轴更换及部分卸载恢复时，这个几何键没有owner/epoch证据，不能证明旧相位应继承或全部列已进入同一新世代。

因此不能以视觉方便把几何匹配升级为可靠权威归属。可以复用原生Output速度；本卡的转子关联和撤销门保持等待主PM实际接口。素材作者随后报告26项已经安装，正式组合构建仍等共享接口齐备，此处未重复素材检查或提前运行。

## 同轮审查P2窄修：可见性不作为身份失效

负责人接受同一独立组合审查的P2：旧 `lastSeen > 20tick` 会令仍加载、同BE、同LOCATED/朝向/端位的停止轴移开相机后丢失24度停转角，也提前终止离屏速度采样。只读核对确认这个退出条件确实来自客户端可见性，不能代表可信身份失效。

本次仅改原五路径中的 `TurbineClientEvents.java` 与 `TurbineAnimationRendererTest.java`：移除20tick期限、lastSeen字段和可见性更新；保留每tick已加载/同BE/状态匹配检查、弱引用、世界/区块/重载撤销。value仍仅持有WeakReference与值字段，没有新增强持BE/世界的闭包。此修正替代上文初版报告的“超过20个未渲染tick撤销”规则，原报告和来源快照保留为历史。

先新增两项真实入口用例再窄修生产条件：同身份停转24度后100tick无BER调用仍保留角度，随后离屏32RPM锚定并40tick推进到48度；真实 `LevelEvent.Unload` 经生产Game事件清除旧停转相位。既有缺区块/实体替换/换世界用例保留。两测试现共15项待运行用例，仍未编译、未运行RED/GREEN或Gradle；总包等待共享HANDOFF，不把检查代码写成测试通过。

修前两文件已保存 `build/reports/art/ART-TURBINE-10B/client/p2-before/`，局部修差异见 `p2-fix.diff`，修后源身份见 `p2-after-source-manifest.json`；原 `interface-neutral-source-manifest.json` 不覆盖。本轮复核由原审查者只读实际修diff及随后对应运行证据，不新增reviewer或改HANDOFF门。

## 获批中立部分定向运行

负责人随后在本卡精确批准先运行这两测试类及必要增量编译，明确替代原“API齐前不Gradle”的等待约定；未授权提前assemble/JAR、GameTest或全量。实际命令（Java21）：

```powershell
./gradlew.bat test --tests 'com.iksxh.create_nuclear_industry.turbine.client.TurbineAnimationVisualStateTest' --tests 'com.iksxh.create_nuclear_industry.turbine.client.TurbineAnimationRendererTest'
```

- `01-neutral-test.log` / `.exit.txt`：exit1，生产compileJava成功，compileTestJava在测试直接引用Flywheel `PartialModel`类型处失败；没有执行测试或生成本轮XML。这是编译RED，不是业务断言RED。保存原始输出，不把失败隐藏为通过。
- 窄修仅测试的前/后partial用例：测试编译classpath未暴露Flywheel实现类型，用反射调用原真实 `partialFor` 和 `modelLocation`；保留全部两个资源选择断言，不改生产模型、构建脚本、BE或Controller。
- `02-neutral-test.log` / `.exit.txt`：exit0，16秒；增量compileJava/resources复用，compileTestJava及test实际执行。两XML确认 `TurbineAnimationVisualStateTest` 6/6、`TurbineAnimationRendererTest` 9/9，共15/15，失败/错误/跳过均0。P2的100tick停转角保留、离屏换速锚定及真实世界卸载事件用例均在其中。
- 既有Bus.MOD待移除警告及Gradle/API弃用提示如实保留；没有用重复全量、clean/rerun或改构建脚本消除提示。

两份原始XML已复制到 `client/02-neutral-test-results/`，用例/计数/时间见 `02-neutral-test-summary.json`；当前五源SHA见 `02-neutral-verified-source-manifest.json`。原阶段/P2来源快照仍保留。

此证据只证明已批准中立代码合同与必要编译。转子renderer未绑定也未改、共享接口尚未实际HANDOFF；本轮未assemble、未产出新JAR、未GameTest或游戏，不记GPU/用户视觉通过。后绑定若不改变这些实现则复用15例证据；若确切修改已测行为或发生新失败，仅复验受影响部分。

## 实际0351ac3接口交回后的转子消费与冻结

用户直接告知接口交回，负责人在原卡追加RotorRenderer权限后，本消费者基于当前美术HEAD `8a3389306eab790130b55308df9bac0274cee3b8`继续。已实际读取主最新AGENTS/治理、原卡旋转消费补卡、前置实际HANDOFF及同步后的API；原16/16与真实3/3、R1素材26来源/安装和人工三件模型证据复用，不重开已验模型。

实际本批修改四路径：`TurbineRotorRenderer.java`、`TurbineClientEvents.java`、`TurbineAnimationVisualState.java`、原 `TurbineAnimationRendererTest.java`。OutputRenderer与VisualStateTest未改且SHA与02冻结一致；RotorBE、Controller、turbine/display及构建脚本无本批差异，素材全部只读。

- 当前转子的唯一 `runtimeDisplay()` 整包同时提供几何和角度输入；`sample.rpm()` 已为前→后局部+Z，直接累计，不调用输出轴的localRpm再次反号。可靠0保留停角和几何，empty立即撤销成员旧绑定并静止fallback。
- 整机phase key是当前世界会话加dimension/owner/generation/epoch；member/index仅绑定当前弱BE。每组保存最大完整serverTick和对应整包值，旧包不能覆盖RPM；相同tick若前轴/朝向/直径/数量/RPM共享字段冲突则静止到更晚样本。serverTick只比较先后，不计客户端年龄，角度始终使用原活动Clock的tick/partial累计。
- empty时也立即检查其余已登记弱成员的真实runtimeDisplay；没有可信成员的组撤角度，保留纯数值排序水位。其他列的旧同tick/更早包不能重启，必须有更晚完整样本；仅丢一列且其他列仍可靠时，共享相位继续。水位/整包值不强持BE或世界，当前会话结束/重载清空全部数值状态。
- 不以可见性失效，已登记可信弱BE持续Post采样；暂停冻结Clock和partial但不跳过可信性查询。世界/区块/成员或身份变化按合同撤销，弱ended标记阻止Unload后仍挂在Minecraft.level的旧世界对象重开相位会话。没有调用前置publish/accept/tick/续租方法，没有另做机主或轴链定位。
- RotorRenderer绘制原完整B partial，复用已测OutputRenderer的Yaw*Roll提交入口；合法直径包围盒沿用现有覆盖全扫掠的范围，不改模型、碰撞或材料。empty几何fallback只使用当前合法外观，不拼上一包RPM/朝向。

先添加八个消费用例与只配置外部同步/世界边界的夹具，再落实际生产实现。夹具不替换runtimeDisplay、共享phase、SBB/Affine/PoseStack；前置租约实现和服务端发布不在本轮重测范围。新增覆盖真实API四向局部符号、整机多列同相、混合新旧整包/绘制顺序、可靠0停角/负转、empty最后成员的旧包复活禁止、部分成员失效、暂停中撤销、旧世界Unload、epoch/重载和同tick冲突。

实际唯一受影响类定向命令：

```powershell
./gradlew.bat test --tests 'com.iksxh.create_nuclear_industry.turbine.client.TurbineAnimationRendererTest'
```

`03-rotor-binding-test.log` / `.exit.txt`：16秒exit0，生产/测试增量编译成功，资源准备复用；原XML确认17/17、失败/错误/跳过均0，其中原受影响9项和新增8项。未改的原Clock/Phases/Phase段逐字相同，见 `03-core-reuse.json`；原VisualStateTest 6/6复用02证据，没有机械再测纯核心、GameTest、全量或clean/rerun。仍保留原Bus.MOD/API/Gradle弃用提示，不因提示扩写构建。

原XML副本与用例计数保存在 `03-rotor-binding-test-results/`、`03-rotor-binding-test-summary.json`；实际六源SHA在 `03-rotor-binding-source-manifest.json`，冻结副本在 `03-verified-sources/`。开工四源快照为 `03-binding-before/`，同轮新增消费差异为 `03-rotor-binding.diff`，供原组合审查者只读新增范围。

工具上下文说明：本子代理的开发者权限仍为danger-full-access/never且明确禁止sandbox参数，与root的workspace-write不同；根已确认按本子代理实际允许的工具执行精确授权写集和单类测试。所有实现/日志仍仅在同级美术树，未转写main，未执行Git写操作、世界/配置复制或客户端启动。

当前实现与定向结果已冻结待原同一组合审查。此处没有assemble/JAR；实际供汽旋转、启停、四向、真暂停和重载仍需用户客户端观察，不将17/17记为GPU验收或把已通过模型重新设门。最终唯一增量assemble待负责人通知，再仅备份实际美术旧JAR并核对应资源/客户端/共享接口class，不替换main冻结制品。

## 同轮P2-02及暂停注释窄修

已读原REVIEW同轮追加六，确认Post的 `entry.setValue` 重新绑定跳过了BER撤销旧唯一成员轨道的路径。合法边界为同一客户端BE仅member UUID更换、机组generation/epoch不变且较新整包；原17例未覆盖，不能以原GREEN关闭本发现。

先只增加并运行两项真实消费入口回归，使用原真实runtimeDisplay、rotorPose与tickWorld夹具：

```powershell
./gradlew.bat test --tests 'com.iksxh.create_nuclear_industry.turbine.client.TurbineAnimationRendererTest.sameClientMemberReplacement*'
```

`04-review-red.log` / `.exit.txt`：16秒exit1，两例中一失败；XML的明确断言为“唯一登记成员替换必须撤销旧停角，不能由Post/BER先后决定，expected0而实际24”。这是实际业务断言RED，不是编译失败。其他可靠成员存在的回归通过。没有在RED阶段重跑原17项，原XML已保存 `04-review-red-results/`。

本次仅窄修 `TurbineClientEvents.java` 与原 `TurbineAnimationRendererTest.java`：Post先遍历旧轨道，移除不再匹配的旧成员，并暂存只有弱BE引用的新轨道与完整样本值；迭代完成后，以其他仍可信旧成员集合保留共享相位，再绑定新成员并按原最高serverTick协议采样。唯一旧成员丢失则撤角度、保留排序水位，较新合法样本冷启动；存在其他可靠成员时保留他们共享角度。没有在活动iterator中调用会改ROTORS的removeRotor/rotorIdentities，未增加强持BE/世界或另做续租。暂停Javadoc已准确区分冻结时间/角度、可信Rotor整包仍核/接纳，以及Output暂停不采样新网速，未改暂停行为。

随后仅定向运行受影响RendererTest全类：`04-review-green.log` / `.exit.txt`，17秒exit0；原XML确认19/19、失败/错误/跳过均0。两个新增回归分别覆盖Post先/BER先两顺序的唯一成员替换和存在其他可靠成员的保留行为。未变化Clock/Phases/Phase、原6核心、Provider16/16+3/3与素材证据均复用，没有全量、GameTest、clean/rerun或重复素材检查。

04绿XML/摘要为 `04-review-green-results/`、`04-review-green-summary.json`；六源SHA与冻结副本为 `04-review-source-manifest.json`、`04-verified-sources/`；仅两文件相对原03的实际差异见 `04-review-fix.diff`，修前两文件在 `04-review-before/`。原03源/结果/差异全部保留。其余四源SHA与03一致，BE/Controller/turbine/display/构建脚本无本批差异，素材保持只读。

本批仍未assemble或产出JAR。当前窄修与实际RED→GREEN交原同一组合审查者只复核受影响差异；用户旋转/启停/真暂停/重载观察门及已验模型的独立状态不变。

## 最终唯一增量assemble与制品身份

原同一组合审查追加七已关闭P2-02和暂停注释，代码组合差异无剩余必改。按负责人本次明确授权，先核六源与04冻结逐项一致，再将美术当前 `build/libs/create_nuclear_industry-0.1.0.jar` 完整复制到新 `client/final/previous-jar/`。原件及备份均为SHA256 `737D3BD5606109F711998DA92714917DE19B6ED891F0ABB7FBB27CA3F1049050`、2,624,615 bytes；实际读回见 `final/pre-assemble.json`，原09/R1备份未覆盖。

实际唯一产包命令为 `./gradlew.bat assemble`。`final/assemble.log` / `assemble.exit.txt` 保留原输出：`BUILD SUCCESSFUL in 1s`、exit0，4项任务中1项执行、3项up-to-date；compileJava/processResources复用已编译/安装结果，只执行本次jar。未运行测试、clean、全量或GameTest，没有第二次构建。

最终美术JAR为2,777,331 bytes，SHA256 `9BE1D3105034C37D27DD8448B3D1D00E2C375462D2F82B0D66A5B579B2E4CA6A`；完整冻结副本位于 `client/final/frozen-jar/create_nuclear_industry-0.1.0.jar`，读回SHA与当前build/libs相同，另有 `final/jar.sha256`。`final/package-identity.json` / `.log` / `.exit.txt` 保存真实制品身份核验，exit0、无不一致：

- 当前六客户端/测试源与04冻结6/6一致；04原GREEN XML保持原SHA，不改写原RED、03/04源及测试结果。
- R1映射SHA仍为 `238f377912d128ed1b8bbd7531aac16803ae3c133d05c53728d4f9b2ccb29704`；精确26项候选来源→src/main/resources安装→build/resources/main→JAR四处逐项与该映射同源。
- 仅枚举TurbineRotorRenderer、TurbineOutputShaftRenderer、TurbineClientEvents、TurbineAnimationVisualState四生产前缀，实际15个主/嵌套class的编译字节与JAR一致，两个集合完全相同，没有多余旧嵌套项。
- 前置11来源与runtime树 `build/reports/turbine-runtime-display-01/r1/sources.json` 一致；13个独立JAR项与其 `jar-entries.json` 冻结字节及本树编译/资源字节一致。直接以最终JAR运行javap读取实际公共ABI，与Provider `r1/compiled-api.txt` 完整归一化文本一致，runtimeDisplay公共整包入口存在；实际输出、stderr和exit0分别为 `final/compiled-api.txt`、`.stderr.txt`、`.exit.txt`。

身份辅助第一次通过WindowsApps的python别名启动时，PowerShell重定向报StandardOutputEncoding异常，脚本尚未执行；保留 `final/package-identity-launch-error.*`。随后仅改用已定位的bundled Python运行同一个身份脚本成功，没有修改业务、资源、Provider或重复assemble。`final/verify-package.py` 保留实际核验过程，写入只在final证据目录。

本次是交付身份核验，原RendererTest19/19、核心6/6、Provider16/16与真实3/3、素材7项/120叶组及已通过模型观察全部复用，不扩成GPU验收。源码/素材、当前美术JAR与冻结副本已形成可运行候选，交root及原同一审查者作最后制品收尾；实际供汽旋转、启停、四向、真暂停和重载仍需用户客户端观察。没有修改main JAR、Git、玩法/网络/构建脚本，没有启动客户端、搬动或回写世界。
