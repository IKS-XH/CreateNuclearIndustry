# ART-BOILER-05B 独立整批规格与质量窄审

2026-10-11；唯一工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`。本报告接续资产/模型预读，属于同一轮组合审查。审查只读现有源、原始证据及制品，没有运行生成器、检查器、JUnit、Gradle、GameTest、客户端或保护矩阵，没有修改实现/Git/存档或派发子Agent；唯一新增本报告。

## 结论

规格行为符合：通过。内部行为质量：通过。Critical/Important必改项：无；一项Minor中文注释需最小整改，见下文。

用户游戏视觉门仍待验。内部通过不证明实际透明排序、水位透视、各尺寸/朝向成型与撤销效果，也不表示本批已合main；不重新选择用户已批准的A风格。

## 合同与技能

实际读取当前AGENTS、治理1.2/5.1、美术入口、05B卡（含预读与阶段二生效补充）、两份执行报告及实际L1 HANDOFF。共享净同步53d699b、后续文档同步9878fd8与本批未提交实现分开核对，不以BASE..HEAD代替工作区候选。

实际应用已读技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`核客户端生命周期与正式API；同目录`minecraft-testing/SKILL.md`审生产行为路径及原始结果；`minecraft-resource-pack/SKILL.md`核SVG/图集、模型、sprite与透明面。重新读取并应用`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/requesting-code-review/SKILL.md`；复用此前verification-before-completion规则，按项目5.1合并规格/质量、复用证据，不另派代理或机械重跑。MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6保持。

## 资产与模型预读复用

此前已实际审读boiler-art-05构建/检查工具、映射、色板、SVG来源及资产/安装原始输出，并查看基础钢壳/控制与冷热口/换热面/阀牌、侧/顶/隔层/窗口图集、2×2和L形预览。12基础、8 RECTANGLE＋1 OMNIDIRECTIONAL共21PNG来源明确，204张整数rect SVG供确定性导出，有限色板保持A稿工业暗线。端口中心逐像素保留，阀体为材料而非重复完整阀图标，隔层为钢压边和窄米色条。窗口47有效/17回退索引沿已锁定原生表，正交邻边门控下保留L缺角法兰、内部接边去框。

原始check退出0，覆盖基础/图格来源像素、原生上下文、透明孔及2×2/L；非整数rect拒绝例和首轮SVG误读失败保留。21/21安装逐字一致。窗口旧18element改为单个0..16六面纹理框，逐面UV0..16/cullface、壳体particle与translucent成立，没有旧玻璃/实体内框叠层；原小检查通过。预读Minor README进度已由作者仅改文案关闭。素材未变，不重做上述矩阵。

## 最终消费者审读

完整审读三个新增生产类和两个专属测试，按实际Create 6.0.10-280 CTModel字节码核调用链。独立Dist.CLIENT/MOD的RegisterGeometryLoaders内同步幂等初始化，没有setup排队、DeferredBlock提前取值或其他事件改动。仅八个正式block注册ID登记模型，物品、独立nuclear_heat_exchanger和内部再热段不包装；后两者分别只作允许邻接证据或排除。九个真实shift固定映射；普通side和partition_side分别保存，不因共用原sprite覆盖。entry沿Catnip重载更新，不缓存旧atlas sprite。

gather入口一次capture(context)，super生成原生CT索引、共享窗面与隔层mask都复用该不可变快照；嵌套/异常finally恢复，顶层remove。两格须dimension/owner/generation/revision/min/max/partition一致，各自位置、实际局部ID、合法材质及inclusive外平面匹配才连接；窗口仅连窗并允许严格面内对角，原生buildContext负责双邻边门控，其他背景四邻可连窗。未知/Ponder/撤销/局部冲突返回单块，未重扫结构或借Fill/运行状态判定。

Create在gather生成方向索引，在getQuads才按真实quad sprite选择shift。因此隔层面mask存入ModelData，出quad阶段仅建立由该mask派生的短作用域，并finally恢复；不读取世界、新快照或构建快照作用域。可靠绝对Y等于partitionY且为水平壳面才选强带，其他回普通side。当前getShift(null)及未映射sprite返回null，Create保留原quad；接口图标、facing及阀四段几何未变。

共享窗mask仅来自同炉窗口共同可靠外平面的切向邻接，外向、外圈和非共享背面保留。指定side可直接返回空；side=null按实际quad方向复制过滤，不改super共享列表。未知mask明确0，重建时覆盖旧数据，保留原碰撞/noOcclusion和水位BER。

## 原始结果与最终绑定

唯一证据根 `build/reports/art/ART-BOILER-05B/consumer/`。实际读取junit-assemble.log、exit.txt和两份XML：各5项，共10/10，failure/error/skip均0；唯一同次定向test＋assemble为26秒exit0，未clean/rerun/full build/GameTest。过时API警告保留，不作失败或升级依据。没有独立行为RED，不把缺类/编译失败当RED。

测试调用生产canConnect/partitionFaces、原生buildContext、真实包装getModelData/getQuads及生产登记边界，覆盖六面跨材质/身份与局部冲突、2×2/L对角、共享面两路过滤、撤销后旧ModelData的隔层选择、嵌套/异常恢复和幂等8ID/9shift。外部世界使用有限读取ClientLevel夹具，原模型使用最小BakedModel；隔层shift测试验证实际生产选择，不等同完整真实模型UV/atlas/GPU或客户端资源重载运行。一次capture的调用位置亦由生产控制流确认。

本次只做一次候选绑定核查：5份源、8个编译类、22资源、12共享依赖及两XML全部匹配candidate-manifest；8类/22资源对应候选JAR条目逐字匹配。共享12项与expectedFrozenSHA一致，复用前置29/29、1/1真实探针及独立审查，不重新审共享状态机。窗口保持SHA `049cf9404d019b015c9aa624ddf1929e465af05389faaa9aa99be409cea69656`，21PNG沿原安装件；无资源再生成。

manifest SHA256：`45ddce34f96185f168503103ed696a0b8e3b6489adfb54c9070c85eb1741c419`。冻结候选 `consumer/frozen/create_nuclear_industry-0.1.0.jar` 为2,528,938字节，SHA256 `d9745639811f57b9b5f164f46c943f6335339c83e62903ea3b40114622818a51`，build/libs同哈希。源/API/资源/制品绑定未发现漂移；相对eef4f18的共享差异属于PM独立交接，不误计为美术执行越界。

## Minor注释整改

`src/main/java/com/iksxh/create_nuclear_industry/boiler/client/BoilerConnectedTextures.java:124`称“不读世界/ThreadLocal”，但128行读取QUAD_PARTITION_SCOPE保存上层作用域，181行实际getShift读取该短作用域。行为符合HANDOFF且数据来自本次ModelData，但这句话容易误导维护者，不符合注释准确性合同。最小修法仅将其表述为“不读取世界或构建快照，隔层选择来自本次ModelData，短作用域保存/恢复供Create出quad使用”。不改变实现、测试或旧证据，不重跑JUnit/构建/素材矩阵；完成后只核注释差异及候选源身份说明。consumer报告开头是窗口资源阶段历史，后续阶段二已明确，不把旧时态当业务缺口。本审查没有自行修源码。

## 人工门

用户仍须在现有客户端观察不同尺寸成型/撤销、前后同色及压边、实际隔层横箍、各facing功能口和阀体、单窗/长条/2×2/L及斜视共享面、水位透视与资源重载。离线图和自动断言不替代此门。其他设备与反应堆既有视觉/鼠标门独立，Git集成由PM或用户处理。

## 同轮Minor关闭与最终结论（2026-10-11）

只读比对consumer/before-comment-correction中的原源/清单与当前文件，实际差异仅第124行中文注释，以及manifest对应源码SHA/长度。注释已准确说明“不读取世界或构建快照，隔层选择来自本次ModelData，短作用域保存/恢复供Create出quad使用；缺失掩码为0”，此前Minor关闭。README进度句亦明确资源/消费/候选构建已完成、真实视觉待用户、未合main。

当前BoilerConnectedTextures.java SHA256为`2b7d12b6754786b0351c62fb4fe6637c04bbcbee03b61db2a22a333e13b03e72`；当前candidate-manifest.json SHA256为`469e5ae0c19cbafba295a78c9e6a88b157f8bc98cea4fe4dbb7d234105ce2274`。原清单与源保留在before-comment-correction，前文旧manifest身份为初审时点。生产行为、class、资源、JAR及10/10结果沿用同一证据，不重生成、测试、构建或重做整批核验；注释修改不宣称产生新编译制品。

最终结论：规格符合、内部质量通过；所有已指出问题关闭，必改项无。游戏视觉门保持待用户，其他人工门与Git集成权限不变。本同轮审查结束并停止写入。
