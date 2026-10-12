# 多级专用管网：禁运边界只读技术核查

日期：2026-10-12。角色：只读技术核查执行者。本文是给 PM 的实现可行性证据及建议，不冻结玩法、参数、任务状态或人工验收结果。

## 范围与证据身份

已读根目录 `AGENTS.md`、`docs/project-governance.md` 相关职责/版本/验证节，以及 `docs/superpowers/plans/2026-10-12-large-reactor-infrastructure-preparation.md`。准备稿的“一档优先、不修改 Create 所有普通泵管”是原建议；本次用户的新要求为热冷却液与超临界蒸汽只能专用管道和泵运输、分级提高耐压与流量。若落实后者，必须新增仅针对这些流体的 Create 运输拦截，不能继续把原建议当成完全禁运的实现依据。

实际应用技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 用于版本、服务端能力、注册与集成边界核查；`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md` 用于区分纯逻辑与真实能力/管网验证，形成未来必要验证清单。未采用技能新版本示例，未执行技能构建、测试或 Git 步骤。

本轮只读生产源、现有依赖源、锁定 JAR；只写本报告。未运行 Gradle、JUnit、GameTest、客户端或服务端，未操作分支、存档、核心文档、功能代码或构建脚本。开工时已有 `.gitignore`、`docs/project.md`、日志、`tools/reactor-simulator/` 多项 dirty 及其他 untracked，均未触碰。

`gradle.properties` 核实 MC1.21.1 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82；`build.gradle:13` 为 Java21，`:103` 引用 Create slim 坐标。锁定 Create JAR 为：

`C:/Users/IKSXH/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280/92471e8fc5ed4e3c2279001d77ebcec6fd38bcab/create-1.21.1-6.0.10-280-slim.jar`

SHA256：`F0652BEE27460F2D26A748F537CB5D687981441378D3A526D17FFAAB5A1072BB`。内置 `META-INF/neoforge.mods.toml` 报 Create6.0.10、Minecraft[1.21.1]、NeoForge[21.1.219,)；280由依赖坐标/路径确定。

下文 Create 源定位使用 `build/p0-sources/create-src/com/simibubi/create/`；该目录未在本轮证明每个文件都与锁定源码包逐字相同。因此核心结论额外用 `C:/Program Files/Java/jdk-21/bin/javap.exe -c -p` 直接读取上述锁定 JAR 的 `FluidNetwork`、`FluidTransportBehaviour`、`AllTags$AllFluidTags`、`PumpBlockEntity`、智能管/阀行为、软管 handler、创造罐内部 handler 字节码交叉核对。未把旧提取源当唯一证据。NeoForge `IFluidHandler.java` 则与缓存 `neoforge-21.1.219-sources.jar` 内同名文件归一换行后完全一致。

## 1. Create 是否有现成流体禁运标签/API

结论：核到的锁定原生管网没有可直接填入某个 fluid tag 即禁止管泵输送的现成合同。`AllTags.AllFluidTags` 的 `BOTTOMLESS_ALLOW/DENY` 控制软管无限池资格，其余为风扇处理催化剂及食品类标签；它们不是管道黑名单。`content/fluids/FluidTransportBehaviour.java:49` 的 `canPullFluidFrom(FluidStack, BlockState, Direction)` 默认恒 true，锁定字节码为 `iconst_1; ireturn`。这是自定义行为可覆写的流体谓词，不是可向现有所有原生管注册的黑名单服务。

原生执行链为 `FluidTransportBehaviour.tick` → `PipeConnection.manageFlows` → `FlowSource.provideFluid(predicate)` 建立流态 → `FluidNetwork.tick` 统一真实搬运。普通、玻璃、包壳、智能管、阀、泵共享此链。智能管与阀的 `canPullFluidFrom` 允许分支会调用 super，锁定字节码也核实；因此针对基类谓词的限制对当前原生子类可用，但不能保证任意第三方子类继续调用 super。

建议最小可靠运输闸门放在 `FluidNetwork.tick()V` 已完成流体识别、任何真实抽取前。例如锁定字节码偏移669的 `keepPortableFluidInterfaceEngaged()` 调用前，此时网络 `fluid` 已建立，仍未进入模拟/执行搬运。仅对热复合冷却剂 source/flowing 与超临界蒸汽身份拒绝整次原生交易；其余流体原调用逐字保留。可额外在 `canPullFluidFrom` 阻止不合法流态/动画建立，但不能把表现层谓词当唯一守恒闸门。

锁定 `FluidNetwork.tick` 实际有 `IFluidHandler.drain(FluidStack, action)`（偏移927）与 `drain(int, action)`（956），随后唯一 `fill(FluidStack, action)`（1232）。**不能只拦 fill**：执行阶段已经扣源，会丢液；仅按资源参数拦其中一种 drain 也不完整。整次预先拒绝能同时保证模拟/执行一致。注入点需严格 `require/expect/allow` 或等价显错检查，升级 Create 时重新核验。已有 `src/main/java/com/iksxh/create_nuclear_industry/mixin/FluidNetworkSharedFillMixin.java` 正在拦同一 fill，新增闸门须与其共享接收容量修正并存，而不替换现有修正。

独立工业网络建议不继承这条原生最终搬运链，避免自己的专用运输被闸门拦截。若选择复用，必须有可验证工业路径的明确授权上下文，不能以管网中“出现一个高级泵”就把其余普通管全部白名单化。

## 2. 来源、接收端与绕过矩阵

| 对象/路径 | 当前实际入口与行为 | 对新要求的影响 |
| --- | --- | --- |
| 反应堆热端 | `reactor/ReactorCoolantFluidHandler.java:159-183` 两种 drain，返回新热液 FluidStack；仅检查绑定/代次、源量与同口tick额度 | 没有运输者身份检查；普通 Create 泵与第三方泵当前可抽。新增工业口仍须与同口共享预算/账本一致 |
| 锅炉热液输入/冷液输出 | `boiler/BoilerControllerBlockEntity.java:298-350` 外向面 `Port`，hot fill、cold drain | 工业热液可接现有能力，但普通管禁运应由全局流体专属闸门覆盖；只改反应堆出口不够 |
| 锅炉超临界汽输出 | 同一 `Port`按选择的汽库存 drain；`:279-296 pushSteam`向邻接非Create pipe的任意能力先模拟后填充 | 原生闸门只管 Create 网络；“必须经过专用管/泵”是否排除相邻储罐直推需 PM 明确。若排除，SC直推路径也要收紧，普通汽保留 |
| 锅炉无需泵的普通管输出 | `BoilerSteamPressure.refresh/spread` 从锅炉汽口传播 `portFlow×2` 原生压力，控制器每tick调用 | 拦原生交易后不再实际搬运SC，但还应停止SC对普通管的无意义流态/加压贡献，保留普通汽贡献，不改炉压/库存 |
| 核热换热器 | `heat/NuclearHeatExchangerBlockEntity.java:350 fluidPort`、`:512-587 Port`：列尾热液fill、列首冷液drain；冷凝模式只收普通steam | 标准能力无来者检查。内部同列热账本转移属于机器内部过程，不应误当外部普通管运输；仍需保留原模式约束 |
| Create普通储罐 | `FluidTankBlockEntity.registerCapabilities/handlerForCapability` 暴露共享 `SmartFluidTank`，所有侧/同多块罐成员可查相同库存 | 工业流体存入后，普通泵可从罐抽出，或向另一罐输送。统一 `FluidNetwork` 闸门可挡该漏洞，不需要禁止储存 |
| Create创造储罐 | `CreativeSmartFluidTank.fill`接受请求量、两种drain强制SIMULATE；`setContainedFluid`设无限源身份 | 可作为受限流体无限源/吞量目标；同一原生闸门需对它生效。不能把创造罐fill返回值当普通有限库存守恒证据 |
| 普通泵紧贴罐、不铺管 | `PumpBlockEntity`自身有 `FluidTransportBehaviour/PipeConnection` | 仍属原生网络链，应受禁运约束；不能只检查“普通管方块邻接” |
| 软管滑轮/开放管口 | `HosePulleyFluidHandler.fill`先检查 `FluidHelper.hasBlockState`；热液无block，SC `StorageOnlySteamFluid.createLegacyBlock=AIR` | 正常软管不能把这些流体放入世界。无桶/无世界block并不阻止罐间能力运输；开放管口有自己的内部罐，可接受/缓存一些非可放置流体，不能据世界不可放置就宣称管运输已禁 |
| 汽轮机 | `TurbineControllerBlockEntity.Port`输入只收SC；输出/主动邻罐输出为普通steam；`TurbineSteamPressure`只遍历exhausts | SC输入是专用网目的端；其普通排汽线路不在本次两种受限流体范围，不应一并撤销普通排汽加压 |
| 便携流体接口/移动储罐 | 原生 `FluidNetwork`有 `keepPortableFluidInterfaceEngaged`，接口可作为源/目标 | 通过原生管网卸载仍受闸门，但把装满储罐的构件整体搬运是另一玩法边界；本轮未深入冻结移动构件/物品携带合同 |

储罐允许储存时，不能简单改其 `isFluidValid=false` 或所有fill拒绝，否则工业管也无法注入。工业管到普通管的连接器也不能把受限工质转为不受限别名；热液两种注册态必须同时判断。

## 3. 任意第三方“绝对禁运”的技术边界

NeoForge21.1.219 `IFluidHandler.fill/drain`只收 FluidStack/数量与 action，没有调用者、泵等级、路径、所在方块位置或真实压力；能力查询侧 `Direction` 表示被查询设备面，不是来者凭证。[NeoForge1.21.1官方能力说明](https://docs.neoforged.net/docs/1.21.1/inventories/capabilities/)与本地锁定源码一致。读取相邻方块猜测来者不可靠：第三方可缓存句柄、从远处查询、忽略邻接或包装能力。全局调用栈探测/ThreadLocal猜测来者也不是稳定公共合同。

因此可以明确保证“本模组设备 + 锁定 Create 自带管泵路径”禁运；不能同时开放任意第三方罐的通用 `IFluidHandler`，又承诺任意第三方泵都无法从罐提取受限流体。Create闸门对自行调用handler的第三方泵不生效。

如果用户要严格限制全部外部运输，建议受限工质采用私有工业 capability/受控服务端事务，标准能力只显示或拒绝受限fill/drain；储存限于受控储罐/逐个适配的Create罐包装与工业适配器。允许 Create 罐储存但其标准交易受限，需要专用工业入口同时读写同一权威库存；还须处理直接 `getTankInventory()`/移动罐等非能力入口，不能承诺阻止恶意或不遵守约定的模组修改内存。任意第三方储罐只有明确适配后才能纳入严格模式。

建议 PM 集中确认保证层级：首发锁定Create禁运且标准端点开放，还是受限工质走私有工业事务并收紧储罐兼容。前者实现面更小，后者更接近“只有本模组专用设备”但必须明确兼容范围，不把选择替用户冻结。

## 4. 三种压力与实际元数据

1. **锅炉归一炉压**：`BoilerState.pressure()`为 `Σ(各汽库存mB×本池温度)/(汽区容量mB×配置超临界温度)`；无量纲，护目镜乘100显示。它参与产汽分类、最小输出和安全阀。它不是MPa，不能直接把0..100控件标成工程管压。
2. **Create原生pressure**：`PumpBlockEntity`以 `abs(getSpeed())`提供，按并联分支分配；`FluidNetwork.transferSpeed`为 `max(1, inboundPressure/2)`取整。它描述流向/运输速率，锅炉也把端口流量×2映射到它。没有真实流体压强单位，也不是管壁耐压。
3. **未来工程输送压力**：目前不存在工业管内工程压力字段。分级耐压须先定义是流体种类准入/抽象工况等级，还是新网络实际压力模型（单位、泵扬压、阻力、失压、堵塞、混级、故障）。不能从转速压力或超临界身份自动推导每一管段MPa。

现有真实账本：锅炉保存分池mB/HU、由比焓派生各池温度与归一炉压；反应堆保存冷热库存及自身温度；换热器保存自身热账本。热液FluidType注册温度900、SC FluidType注册温度900都是类型属性；外部 `new FluidStack(fluid, amount)` 没有携带每批温度/HU/炉压component。普通Create罐仅存身份、mB和原有FluidStack组件；此处源机器未附带热工元数据。SC身份保留其种类，但不能视为保存了来源锅炉压力。现有“已付款库存才能放汽”与汽轮机按汽种核算能量合同应保持，不因管网等级重新创造热量。

可实施的较小设计是先定义各级可接流体及额定mB/t，耐压作为抽象工程等级；真正压力传输/泄漏爆裂另冻结。如果要真实压力，则必须显式新增并验证端点到网络的元数据合同，而不是改一项 FluidType 温度或复用原生pressure。

## 5. 后续最小验证与未决项

未来实施至少要用真实 `level.getCapability` 与锁定Create链证明：反应堆/锅炉/普通罐/创造罐各作受限源时，普通管、紧贴泵、玻璃/包壳/智能管、打开阀均不产生实际成交；冷热另一侧及水/普通steam保持可用。另测工业网→Create罐→普通泵漏洞、受限专用路径通过、混级瓶颈、断管/区块卸载恢复、模拟零副作用与同口预算、旧缓存句柄、保存后当前版本重载。流量守恒断言必须同时看源/目标库存，不能只看动画停止或fill返回0。创造罐单独用预期无限源/汇语义验证，不能混入有限库存守恒总数。

需 PM 与用户确认的设计项：保障范围/第三方兼容；储罐直推是否必须经过专用管泵；移动罐与手持容器是否算运输；等级按抽象工况还是真实工程压强；各级流体准入、流量、泵转速/应力、混级行为及过压结果；锅炉工业接入与现有普通端口的关系。本报告不自行选择默认数值、合成成本、泄漏/爆裂规则或改变旧候选人工门。

本轮结论仅为已读取版本的技术可行性核查。没有新增自动测试/客户端运行结果，没有功能完成或人工验收声明。
