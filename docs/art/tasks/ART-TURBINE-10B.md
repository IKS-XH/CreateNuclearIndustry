# ART-TURBINE-10B：B方案正式转子、轴承与动画

用户2026-10-12明确“我更喜欢B方案，但是转子叶片太厚了，先按照B方案实施吧”。确认B双列错相短叶盘、阶梯轮毂和箱式端座，正式制作并接客户端动画；原07B-R2机壳视觉门独立，不推定其他验收。10A/R1选稿门已由本次选择关闭，旧文档的待选描述为历史。

## 权限、基线及几何决策

唯一写树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，HEAD `2ae83b9c9d355c5229f973849e04c449dea0ceb5`加既有07/08/09未提交输入。负责人维护docs/art并派发；作者/客户端消费者实际实现。无Git、世界/配置复制、游戏自动启动或其他任务写入。读主目录最新AGENTS、治理1.2/5.1/5.2、本卡及minecraft-modding/testing/resource-pack；锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，中文注释。采用writing-plans/subagent-driven-development/verification-before-completion；项目定向检查、单次组合窄审和唯一必要增量assemble优先于通用技能的重复全量/提交流程。

实际10A preview的B参数depth=.12并未被消费；真正叶片两列Z=.13..47和.53..87，各厚.34。已向用户说明正式收薄为.125格（16px尺度两像素），两列中心.30/.70保持，即Z=.2375..3625及.6375..7625；每列12片，错相15°，后掠.09rad。不要依据未使用的depth字段只改文字。B外轮毂及盘轮外形以实际R1 preview为参考（D3基准R.52、阶梯盘），轴孔/内圈按真实R.14轴及R.155配合，不将孔径整体随直径放大而脱轴。三档全扫掠R上限1.1875/2.1875/3.1875，壳内均留.125；轮毂总Z仍.13..87。未成型复用完整D3网格XY等比缩至R.42；不额外叠遗留rotor_middle。前后板厚3/16、孔半宽.19、固定轴承外圈R.27及内圈R.155保持；前后绕序/封闭面需正确。输出轴外露范围按实际旧模型只读核对，外座不转。

## 素材作者独立写集

第一阶段仅新 `tools/art-assets/turbine-rotor-bearing-art-10b/`、`build/reports/art/ART-TURBINE-10B/assets/`及`docs/art/reports/ART-TURBINE-10B-ASSETS.md`。旧10A/07工具和所有旧素材只读，不调用旧turbine_models.py全库生成。用新16×16整数rect/crispEdges/限色SVG及独立确定性producer生成B薄叶片模型、材质和PNG候选；可只读复用旧Mesh工具函数，但不能写旧producer或重生机壳/窗口。

候选包括三档完整旋转partial（叶片+轮毂+贯穿轴）、未成型完整B转子、前后固定箱座及前后旋转轴/内圈partial、未成型输出轴完整模型。生产located转子静态基座需去除动态轴/毂以避免重复；输出固定模型去除旋转轴/内圈。保留既有blockstate/注册ID和模型ID的对应关系，优先仅替换其JSON指向及精确旧OBJ，动态输出新增`output_shaft_front_rotating`/`output_shaft_rear_rotating`两个模型ID。不改变方块/碰撞/配方/材料清单，少量铜色仅视觉锁圈。

作者先交确定的候选资源映射表、真实OBJ/MTL加载预览、SVG来源/候选PNG一致及几何/绕序/孔壁/扫掠检查；负责人实际看薄叶片和两端支承后，原作者按映射表局部安装（无需再问用户）。安装前仅备份被替换的精确路径，保持来源/候选/安装一致。旧shared turbine.mtl与所有casing/window/07B-R2资源禁止写；使用新专属MTL和纹理ID。安装写集由负责人在本卡追加具体名单，未获该名单不得写正式资源。

负责人已实际复看冻结模型的薄厚对照、D3/D5/D7、前内拆件及后内旋转，读原produce/check退出0及素材报告，核映射SHA256=`a39ff51073c711fd3e5480b1fc972033f8211780182c8c6f7a5710b877855d0c`。现仅授权原作者安装该冻结映射的26路径（17替换/9新增）：先完整备份原17文件，候选逐项SHA保持，新增路径若出现不属于本批的既有文件先报告，禁止覆盖。只复制冻结候选，不重生或运行Gradle；安装报告/原始清单追加素材证据。精确写集为：
 
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/output_shaft_front.obj`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/output_shaft_front_rotating.obj`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/output_shaft_rear.obj`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/output_shaft_rear_rotating.obj`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/rotor_blades_d3.obj`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/rotor_blades_d5.obj`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/rotor_blades_d7.obj`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/turbine_output_shaft_unformed.obj`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/turbine_output_shaft_unformed_rear.obj`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/turbine_rotor_bearing_10b.mtl`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/turbine_rotor_unformed.obj`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/output_shaft_front.json`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/output_shaft_front_rotating.json`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/output_shaft_rear.json`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/output_shaft_rear_rotating.json`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/rotor_axle.json`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/rotor_blades_d3.json`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/rotor_blades_d5.json`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/rotor_blades_d7.json`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/turbine_output_shaft_unformed.json`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/turbine_output_shaft_unformed_rear.json`
 - `src/main/resources/assets/create_nuclear_industry/models/block/turbine/turbine_rotor_unformed.json`
 - `src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_10b_bearing.png`
 - `src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_10b_blade.png`
 - `src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_10b_copper.png`
 - `src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_10b_steel.png`

## 客户端消费者与必要共享接口

先只读核当前真实Create动画/速度/时间暂停链、现模型JSON/partial边界、所需包围盒和状态；交精确客户端/测试写集建议。允许规划客户端显示，不自行改RotorBE、ControllerBE、TurbineStructure、网络/NBT、玩法或构建脚本。

用户同意动画的必要转子运行显示前置已发主PM：现RotorBE静态锚点，Controller虽有TurbineView.Rpm但client缺可靠归属/几何/有效性，ownerForPart仅服务端。必须等主PM实际HANDOFF/API后绑定，不能猜附近轴、客户端服务端定位或自行加权威状态。输出轴使用Create已同步getSpeed（实网速，非发电额定RPM）；其固定座/外圈与转动轴/内圈拆分。真实转速为0即停止，符号决定方向；不新增惯性、动力或耗汽规则。

拟客户端范围仅原TurbineRotorRenderer、TurbineClientEvents及新TurbineOutputShaftRenderer/专属客户端相位辅助与必要定向测试；精确名单须在实际接口落定后追加。角度围localZ中心(.5,.5)，再按MACHINE_FACING映射世界轴；同一机组各列相位同源，按实际tick/partial插值累计而非将新RPM乘整个世界年龄造成跳相；真暂停冻结，世界/卸载/代次变化撤销旧展示。同步缺失按合同安全静止fallback，图形显示不得倒灌服务器。不得使固定板整体转动、静态/partial重叠，跨格叶片视锥边界完整。

第一阶段只读报告后，现批准接口中立写集：修改`src/main/java/com/iksxh/create_nuclear_industry/turbine/client/TurbineClientEvents.java`；新增同包`TurbineOutputShaftRenderer.java`、`TurbineAnimationVisualState.java`；新增`src/test/java/com/iksxh/create_nuclear_industry/turbine/client/TurbineAnimationVisualStateTest.java`及`TurbineAnimationRendererTest.java`。原`TurbineRotorRenderer.java`仍只读，待实际共享HANDOFF另补。上述五路径允许实际客户端实现/测试编写，但全部输入未齐前不Gradle；MODEL partial预初始化、Bus.MOD渲染注册与Game客户端tick生命周期必须使用正确总线，不能将Post事件当作必定非暂停。弱身份缓存不得由value/闭包反向强持BE而失去卸载能力。测试实际生产相位/变换入口，不只断言源码字符串；结果仅覆盖代码合同，不冒称GPU。

## 验证、构建及收尾

素材局部检查真实产物/孔壁/绕序/完整扫掠及精确引用；客户端只做受影响的相位、方向、失效/暂停及资源加载合同定向验证。作者不Gradle；最终组合消费者唯一持有必要测试和增量assemble，所有资源/API到位才构建。主PM共享前置已审证据直接复用，不重复服务端/全量/旧存档矩阵。最终一次独立组合规格/质量窄审，整改只复核受影响差异；同源来源/安装/build/JAR和必要保护清单绑定，保留原09/R2制品。

当前26素材已安装；组合窄审P2指出按可见性20tick清可信轨道会跳相，原消费者已窄修并补两真实入口用例，同一审查代码整改关闭。共享前置尚未交回，为核已完成中立代码的具体风险，现批准唯一消费者先运行上述两测试类的15例及其必要增量编译；不提前assemble/JAR、不GameTest/全量。这替代开工时“API齐前不Gradle”的等待约定，理由是实际P2及外部前置待交，非机械重测。共享绑定后未变化的15例证据直接复用；仅绑定改变已测实现/失败/新具体风险才复验受影响部分，最终组合assemble仍唯一持有。

负责人实际看生成模型/薄叶片/旋转离线预览、读原始检查及组合结果，交可运行候选和聚焦客户端清单（3/5/7、四向、前后轴、低高转/停转/真暂停、结构拆除与重载）。真实游戏视觉等待用户，不用浏览器/自动检查关闭；Git净集成由主PM，其他任务及世界不自动推进。

## 2026-10-12 实际接口交回后的旋转消费补卡

用户直接告知“接口已经交回”。已实际核对主PM HANDOFF与同步树：美术HEAD为8a3389306eab790130b55308df9bac0274cee3b8，运行前置冻结0351ac3；原等待接口文字为历史。原客户端唯一消费者继续，不新派素材实现者或第二位消费者。负责人只维护美术任务/证据，不编写实现。

现追加允许修改 `src/main/java/com/iksxh/create_nuclear_industry/turbine/client/TurbineRotorRenderer.java`，限 runtimeDisplay 整包只读绑定、既有B partial旋转/静止fallback、与已成型扫掠相符的必要包围盒消费。原 `TurbineClientEvents.java`、`TurbineAnimationVisualState.java` 与两专属 `TurbineAnimationVisualStateTest.java` / `TurbineAnimationRendererTest.java` 仍为必要客户端/定向测试写集；`TurbineOutputShaftRenderer.java` 原权限保持，优先只读复用已测变换入口。报告与日志只写原客户端报告及 `build/reports/art/ART-TURBINE-10B/client/`。禁止改模型/SVG/PNG、RotorBE、Controller、turbine/display、服务端测试/同步/动力/汽耗、构建脚本或其他美术范围。

唯一可靠入口为当前转子 `runtimeDisplay()`。有值0为可靠停转，保留停角/合法几何；empty撤销该成员旧绑定并静止fallback。rpm已按前→后局部+Z签名，直接使用，禁止二次反号。以当前世界会话与dimension/owner/generation/epoch建立整机共享相位，member/index只用于本列绑定，不拆成各列独立相位。按活动tick/partial累计角度；serverTick仅排序完整样本，不能乘新rpm重算世界年龄。混合到达的各列心跳不得由旧整包覆盖新整包速度，也不能让绘制顺序决定机组速度。暂停冻结时间；缺可靠样本、身份替换、卸载/换世界和资源重载按合同撤销。缓存弱持BE，不查询邻近机主，不调用接口内部publish/accept/tick/续租入口。真实界面冻结由客户端消费者处理，可靠性由前置整包校验负责。

复用原中立15例、前置16/16及真实3/3、素材R1的7检查/120叶组与26项来源安装证据，不重复服务端/模型或已通过人工模型观察。只执行新增或受影响客户端测试；若修改原RendererTest类，按该受影响类定向复验，未变化VisualStateTest证据复用。原唯一消费者先完成实现、读原始定向结果并冻结源；同一组合审查按新增差异复核，不新开第二轮独立审查。最终唯一必要增量assemble仍由原消费者持有，构建前备份当前美术JAR，生成后核资源/客户端/实际接口class与JAR身份，不替换main制品。根负责人确认最终审查/制品证据后交可运行候选；真实供汽旋转、停转/真暂停/重载仍需用户观察，三件已通过模型不重开。

当前沙箱仅主目录和可视化根可写；本同级美术树的授权写入/Gradle须使用精确require_escalated请求，经自动审查执行。无Git写操作、无存档复制覆盖、无自动启动客户端。
