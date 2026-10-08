# 现有设备美术资产与动画接口盘点

日期：2026-10-09。盘点者：本美术负责人对话。首轮仅盘点与提出方案，没有重绘或接入资源。

## 1. 工作区、证据与技能

- 实际工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`。
- 实际分支：`codex/art-studio`；基线：`5e10d0e7e8a36105a95b6f0929f2bb12d57415b8`。
- 开工时 `git status --short` 无输出。所有命令显式在上述工作树执行；没有 Git 写操作，没有创建其他工作树。
- 已读根 [AGENTS.md](../../AGENTS.md)、[治理1.2节](../project-governance.md#12-美术负责人的专项授权2026-10-09)、[美术入口](README.md)、[文档入口](../README.md)和[首发扩展入口](../superpowers/plans/2026-09-22-first-release-extension-preparation-plan.md)。
- `gradle.properties` 与 `build.gradle` 实际核对：MC **1.21.1**、Java **21**、NeoForge **21.1.219**、Create **6.0.10-280**、Ponder **1.0.82**、Flywheel **1.0.6**。不采用技能中的1.21.11资源/测试示例，不升级技术栈。

实际读取与使用的技能：

| 技能入口 | 本轮应用 |
| :--- | :--- |
| `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` | 核对注册、客户端渲染注册、BlockEntity和状态同步的边界 |
| `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md` | 将资源静态检查、客户端体验与玩法GameTest分开；本轮文档不触发构建或游戏测试 |
| `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md` | 核对1.21.1的模型/物品路径、贴图尺寸、blockstate与纹理引用；不引入新版`assets/.../items/` |
| `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/brainstorming/SKILL.md` | 第一台设备按有限范围提出方案；未确定的外观保持待讨论 |
| `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/verification-before-completion/SKILL.md` | 用本轮只读检查结果支持盘点结论，交付前核对文档与写集 |

本报告中的路径相对美术工作树。`docs/reviews/`与旧预览只作历史证据，不改写历史验收。

## 2. 资源总体情况

盘点范围：`src/main/resources/assets/create_nuclear_industry/`。设备以实际注册类和实现为依据，不能由PNG名称推断功能已经存在。

| 类别 | 数量与结果 |
| :--- | :--- |
| blockstate JSON | 48；含P0探针和早期实验样例，不等于48种正式设备 |
| 模型 JSON | 478；包含物品模型、状态变体、跨格分件与OBJ包装 |
| 汽轮机目录 | 294份模型JSON、294份OBJ、1份MTL；数量包括各档位/朝向/壳体位置变体 |
| 游戏PNG | 189，全部PNG颜色类型6（RGBA）；184张16×16、5张16×64 |
| 纹理动画元数据 | 0份`.png.mcmeta`；16×64流动画布的存在不代表已配置帧动画 |
| JSON语法与直接引用 | 解析528份JSON，无语法错误；本命名空间的直接`parent`、`model`、贴图和OBJ文件引用未发现缺失 |

上述只读检查使用捆绑Python，最终命令退出码0。它不检查纹理别名递归、MTL材质映射、UV质量、共面闪烁、完整状态覆盖、GPU表现或Minecraft生命周期，不能据此宣布全部资产视觉验收通过。`src/generated/resources/assets`在本基线不存在，本轮没有生成它。

### 来源与复现方式

- 共用入口：[tools/art-assets/README.md](../../tools/art-assets/README.md)、`manifest.json`、`palette.json`、`sources/`、`export.py`、`pipeline.py`、`generated/`。当前清单83项，其中82项游戏路径和1项工具候选；它没有覆盖全部189张游戏PNG。
- 后续设备使用专用工具：`centrifuge_01d_preview.py`、`fuel_02a_assets.py`、`fuel_02e_assets.py`、`reactor_01_assets.py`、`boiler_01a_assets.py`、`boiler-rework-01/export_boiler_ports.py`、`turbine_assets.py`、`turbine_models.py`、`heat-exchanger-device/generate.py`、`store-01/generate.py`。
- 工具名称含`preview`不保证只读。离心机工具会写游戏JSON，换热器/装配台生成器也会写资源；本轮没有运行这些导出器。`verify.py`包含安装和负例恢复流程，不作首轮只读入口。
- 共用管线保留8项冷却剂原图字节合同；新批次不得顺带改冷却剂、旧基线、许可证或历史证据。
- 源文件主要是本仓库像素SVG和生成器内几何定义；没有在这些设备源目录发现作为统一编辑入口的`.bbmodel`工程。不能把导出的PNG/OBJ当作唯一可维护源。
- 原创/第三方来源按既有记录保持。装配台渲染器直接引用Create原生`SHAFT_HALF`，这是依赖资源引用，不复制为本模组原创资产；新批次继续单列来源。

既有风格参考只读位置：`E:/MyMC/NewMod/Create_NuclearIndustry/tools/art-assets/create-style-samples-2026-10-08/`。已读`samples.json`、相关SVG和材质说明：暖灰钢框、像素螺栓/搭接边、按配方区分钢/铅/铜/混凝土、红蓝色标。该目录不在本工作树，也不纳入本轮写集。浏览器拒绝`file:`协议，未渲染该HTML样例；本轮样例证据是源码/色板核对，不能写成新样例画面已复看通过。

## 3. 实际设备盘点

这里的“可保留”表示有明确源稿、引用链或已认可结构可复用，不是新授予整机视觉验收。

| 设备及注册身份 | 当前游戏资源与源入口 | 可保留的结构/资产 | 可见问题或下一优化点 | 动画准备程度 |
| :--- | :--- | :--- | :--- | :--- |
| 铀浓缩离心机 `enrichment_centrifuge` | `models/block/enrichment_centrifuge{,_upper,_rotor,_item}.json`；`textures/block/enrichment_centrifuge_*.png`；`sources/block/enrichment_centrifuge_01d_*.svg`与`centrifuge_01d_preview.py` | 实际上下两格壳体、独立转子、完整物品模型；下/上壳60/62盒体，转子23盒体 | 早期六面图不等于现行两格模型；当前模型使用casing/brass/glass/dark四种材质。后续只对真实模型做局部质感与接口辨识评审，不恢复旧立方体。配方实际含金板，不能按其他设备禁金色规则一刀切替换 | **已有旋转**，详见下一节；优先保留现行行为 |
| 燃料烧结炉 `fuel_sintering_furnace` | `_off/_on/_item.json`；五张`fuel_sintering_furnace_*.png`；`sources/fuel-02a/`、`fuel_02a_assets.py` | 单格八棱炉体与明暗窗，每种模型61盒体，边界0..16 | `lit`表示下方热级达标，空炉也可亮；不能把窗亮解释为正在加工。后续可强化耐火内衬和钢壳的材质差异 | **状态换模**；无独立活动机构渲染器 |
| 屏蔽装配台 `shielded_assembly_station`及代理`shielded_assembly_part` | `models/block/shielded_assembly_station/parts/`、`partials/`，共19份JSON；`shielded_assembly_station_item.json`；`sources/fuel-02e/rig.json`和10张材质SVG；`fuel_02e_assets.py` | 2×2×2八格壳体、完整物品、左右机械臂和夹具三分件，已有枢轴和扫掠范围 | 分件已准备但尚未按工序运动；黄铜色结构/铅色护板/玻璃应按本机配方与既有认可继续细化，不能用单一整面贴图覆盖八格 | **机构待机渲染＋底部轴旋转**；工序动画待定 |
| 固定实验反应堆，P1八类方块 | `reactor_casing/window/instrument_port/cold_port/hot_port/refueling_port/fuel_rod/control_rod_drive`的blockstate/model；`sources/block/reactor_*.svg`和`control_rod_drive_*.svg`；制造材料在`sources/reactor-01/` | 稳定注册ID与静态cube模型；窗口透明区、冷热口、护目镜/滑条交互均可保留。旧`experimental_reactor_casing`另列，非本批目标 | 现行外壳金黄色角部与冷热口橙色金属观感，和既有钢＋铅＋混凝土、红蓝染料方案不一致；外壳顶面圆形标记像独立部件，材料层次弱。建议先仅优化外壳＋冷热口7张图 | **静态资源**；驱动器滑条/完整度同步存在，不等于已有棒体运动 |
| 核换热器 `nuclear_heat_exchanger` | `models/block/nuclear_heat_exchanger/{base,fins,fins_lit,item_static}.json`；15张目录材质图；`heat-exchanger-device/generate.py`及[部件说明](../../tools/art-assets/heat-exchanger-device/COMPONENTS.md) | 基座Y=0..12，七片铜鳍Y=12..16，三条槽内热提示；4朝向multipart，单格边界清晰 | 现有等距预览的大面质感平；后续可按钢基座/铜鳍/端面方向细化。目录中仍有brass命名槽，须按实际面引用确认再处理，不凭命名断定当前画面用了黄铜 | **静态分件＋lit提示**；工作盆分支先清lit，不能直接把lit当全部供热模式的工作指示 |
| 高压锅炉及水/汽/冷热口、窗口、隔层、安全阀 | `high_pressure_boiler_*.json`、`boiler_heat_exchange_section.json`、`boiler_safety_valve.json`；10张`textures/block/high_pressure_boiler/`材质及两张冷热口图；`svg/block/high_pressure_boiler/`、`boiler_01a_assets.py`、`boiler-rework-01/` | 可变尺寸壳体与端口、透明窗、已实现水位显示 | 后续优先大面平铺及水/蒸汽/冷热口远距离区分；已有双汽库存和安全阀排汽泄压逻辑，不能用外观暗示已有库存自动切种或阀门机械运动已实现 | **窗内水位动态显示**；未发现阀门独立运动分件 |
| 三档汽轮机 `turbine_*`七类方块 | `models/block/turbine/`的OBJ/包装、三种`rotor_blades_d{3,5,7}`；21张目录PNG；`svg/block/turbine/`、`turbine_models.py`与端口密封整改工具 | 三档圆壳、窗口、独立轴与叶片、端口密封几何、完整物品模型 | 资产变体多，需按生成器维护。后续钢壳/铜束带和端口辨识应针对整机平铺评审，保留既有端口封口。当前Ponder整改不属于本写集 | **叶片partial静态**；不是已有正式叶片旋转动画 |
| 干式存储架 `dry_storage_rack` | `_frame/_door/_occupancy_0..4`模型及5张PNG；`store-01/sources/`、`store-01/generate.py` | 门框、内部空间、0..4库存显示件可保留，单格静态multipart | 名为door的模型没有开门状态/动画合同；只按实际库存显示，不增加机械门交互 | **storage_level状态显示**；无已实现开门动画 |

补充：`main_coolant_pump_*`、`pressure_pipe_tier_1`、`pressure_valve_tier_1`等图以及P0模型不构成可独立优化的正式生产设备。矿物/材料/燃料/桶共用资源本轮只计数，未纳入第一台设备重绘。

## 4. 动画分件、枢轴与真实状态来源

JSON几何统一按**16模型单位＝1方块**说明；OBJ生成器坐标按方块单位。跨格partial允许跨出0..16，不能用单格检查器强行裁切。渲染边界不是碰撞或放置边界。

| 设备 | 静止/运动分件与枢轴 | 已核对的客户端入口和状态 | 后续限制 |
| :--- | :--- | :--- | :--- |
| 离心机 | 上下壳静止；转子绕Y轴，轴线X=8、Z=8，当前几何Y=5..27；由下段唯一owner绘制，AABB覆盖两格 | `production/CentrifugeRenderer.java`、`CentrifugeClientEvents.java`；`getSpeed()`（Create转速）、`isOverStressed()`、`pauseReason()`。当前角度`renderTime × speed × 3/10`，单位度，再转弧度 | 转子有动力且未过应力/轴承磨损时转动，不是只在加工时转。沿用该真实语义；速度归零当前直接回0角，平滑停转是新视觉建议，尚未批准 |
| 装配台 | 壳体八格静止；左臂pivot(9,21,15)、+X位移0..1；右臂(23,21,15)、−X位移0..1；夹具(16,13,16)、+Y位移0..3。朝向绕主格X=8、Z=8；底轴用Create原生模型 | `ShieldedAssemblyRenderer.java`只画三件待机模型；`WORKING`、`EXPANDED`来自blockstate；主控`read/write`同步`ShieldedAssembly`状态，进度变化分段sendData | `rig.json`的axis/travel描述平移，不是已经批准的机械臂转角。工序进度虽在同步数据内，渲染用只读访问与归一化工时仍需明确；不可先编一个连续假加工循环 |
| 汽轮机 | 壳/端口/轴与叶片已分离；叶片轮毂中心OBJ(0.5,0.5,0.5)，北向基准轴为Z，随整机朝向映射到X/Z；三档扩大渲染AABB | `turbine/client/TurbineRotorRenderer.java`只用`LOCATED`、`DIAMETER`、`MACHINE_FACING`；`TurbineRotorBlockEntity`无转速/owner状态；主控同步`TurbineView.Rpm`等视图 | 主控`viewRpm`是私有显示字段；`signedRpm()`依赖`live()`与服务端ledger，不能未经评审直接给客户端叶片使用。必须明确转子到owner的可靠映射与显示转速来源，并处理外源贯通轴传动 |
| 锅炉 | 壳/窗口框静止；窗内水面高度改变，无机械枢轴 | `BoilerClientEvents.WindowRenderer`读取`BoilerWindowBlockEntity.fill()`，无量纲0..1；`Fill`更新包，变化阈值0.002；显示高度0.07..0.93方块 | 只用已同步比例，不新增库存或热账本；水面变化不能当汽种/炉压显示 |
| 换热器 | 基座与鳍片静止；lit仅增加槽内提示，无旋转枢轴；4朝向绕(8,8,8)的Y轴 | `NuclearHeatExchangerBlockEntity`普通供热分支以`viewHeat > 0`设lit；工作盆分支先清lit后调用Basin桥 | 现有lit不足以表示所有模式。若要统一热态/流动动画，先向逻辑侧确认字段与工作盆显示合同 |
| 烧结炉/存储架/反应堆 | 当前静态模型及状态变体；没有可直接驱动的独立棒体/门片 | 烧结炉lit＝热级达标；存储架storage_level＝0..4；控制棒驱动器同步列位置、完整度与滑条显示值 | 不把亮窗/占用级别/滑条预览当成已完成加工、开门或实际棒体运动 |

### 需要主逻辑协调的接口清单（建议，未派发）

| 需求 | 字段/单位与来源要求 | 更新、插值与缺数据显示 |
| :--- | :--- | :--- |
| 汽轮机叶片旋转 | 可在客户端可靠确定的owner位置/有效性、实际有符号转速（RPM）、整机朝向与直径；区分蒸汽发电视图转速和可能的外源贯通轴转速 | 优先复用既有同步，新增字段交逻辑侧；有效性/停机变更要可及时观察，帧间积分相位不在速度变化时跳角；owner缺失/卸载时冻结或静止，不推测额定转速 |
| 装配台按工序运动 | 已同步状态的只读展示：working、operation、progress/总工时（原单位）或归一化0..1、owner有效性；尽量通过现有NBT包装，不直接新增网络协议 | 暂停保留合法位姿；拆机/换配方/完成时明确复位；没有阶段数据时保持待机，不假装已装料或完成封装 |
| 控制棒驱动器运动 | 实际插入深度0..100%及方向定义、棒体行程（方块）、列/高度/结构有效性；必须区分已提交权威深度与拖动预览/目标值 | 主逻辑确定它是即时状态还是渐变过程后才设计机构；缺状态保持静止，不从完整度推算位置 |
| 换热器统一热态 | 供热模式、实际供热有效性或热级（说明HU/t还是Create热级）、实际转换流量mB/t；覆盖工作盆/锅炉/冷凝的含义 | 优先已有lit或视图但不得混用；缺液/满冷液立即停热的显示以逻辑合同为准，不能由纯贴图mcmeta自动决定 |

## 5. 本轮视觉证据与结论边界

实际查看了`tools/art-assets/previews/reactor-1.png`、`reactor-2.png`、`centrifuge-1.png`，换热器`generated/heat-exchanger-preview.png`，以及历史离心机六面图、装配台`east-isometric.png`。后两项和离心机六面页明确视为历史预览，不代表本轮重新验收现行整机。

第一批目标7张反应堆游戏PNG与`tools/art-assets/generated/block/`同名产物逐字节一致，因此本轮reactor预览的目标图可用于讨论当前外壳/冷热口纹理。新样例仅源码核对，实际客户端环境光、手持、平铺、透明排序与动画生命周期均未验证。

建议首批：[ART-REACTOR-01，实验反应堆外壳＋冷热端口](PLAN.md#3-第一台设备方案art-reactor-01)。理由是材质依据明确、只需7张图、无需改共享Java入口，能先确立可复用风格。所有后续顺序是美术建议；本报告不改汽轮机Ponder或换热器工作盆人工门，也不推进其他逻辑主线。

交付前再次核对HEAD仍为上述基线。本轮新增写集只有两份美术文档；另观察到`docs/art/README.md`创建信息和`docs/superpowers/plans/2026-10-09-device-ponder-04-framing-fix.md`的并行文档变更，已只读查看并保留，不纳入本轮交付。前者现已记录本对话ID`01a11c6b-f440-77c1-9275-5b2dca5cfa0f`；后者的逻辑侧状态仍由主PM维护。
