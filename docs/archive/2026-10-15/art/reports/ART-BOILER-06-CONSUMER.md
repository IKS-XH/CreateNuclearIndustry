# ART-BOILER-06：消费者准备进度

2026-10-11，按06卡独立准备写集实施。已实际应用minecraft-modding/testing/resource-pack及TDD入口，沿用MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6；治理5.1与本卡的唯一最终组合验证优先，不提前执行Gradle或把缺类编译视作行为RED。

当前只新增BoilerSteamVisualState、BoilerSteamMaterials、BoilerSteamRenderBuffers、BoilerSteamClientEvents，以及同包两个专属测试；未新增Renderer采样入口，不引用未交容量getter，不读取private或NBT绕取。既有生产/资源、05B源码与冻结证据保持只读。

VisualState以实际闭边界/绝对partitionY生成6外面＋4横片＋4竖片，共14quad；全体顶点内缩且仅在真实汽区，owner局部坐标、周期一格的世界同相UV，采光坐标钳制在真实内域air格。库存Long合计，零/非法容量无雾，浓度与两种占比只调材质而不调气域高度。缓存有界，远端外壳区块卸载也按完整bounds撤销。时钟冻结tick和partial。

Materials读取卡中32份16×16内部帧（normal/supercritical×still/flow×0..7）；同类型still/flow以固定视觉权重.35/.65混合，帧插值后按真实sc/total连续混色，含NativeImage ABGR alpha。单炉一个16×16动态纹理，64固定槽；prepare关闭解码缓冲并保留不可变帧，apply坏资源撤旧材质，释放前只flush本批固定层。draw不抢占已提交的其它炉槽，撤销/失见在安全tick边界回收。向上缓慢UV运动与实际入口尚待Renderer接通，不自称已完成GPU视觉。

独立ClientEvents只登记重载与当前世界/卸载/暂停生命周期；原Window BER与L1不变。RenderBuffers固定使用原生entityTranslucent，AFTER_BLOCK_ENTITIES统一结束，保留深度写入。跨层玻璃排序仍属真实客户端门。

测试先于生产编写，目前7个准备行为用例：真实容量/两库存/浓度极值、14quad合法内域与大坐标、缓存撤销/会话、暂停tick与partial、四帧域实际混色/插值与循环/不可变副本、有界池不抢槽与安全边界释放、生产批次原生层及阶段门。没有执行测试，不宣称RED/GREEN。待PM实际容量getter、素材安装交接后补真实Renderer入口、采光/可靠归属/AABB与距离用例，再由本消费者执行唯一test+assemble及冻结。

当前未运行构建/测试/游戏、未改已有源码/素材、无Git写，准备阶段停在实际API和素材门。

## 透明边缘插值定向整改

按负责人追加指令，只运行actualPixelMixerUsesAllFourFrameDomainsAndLoopsSmoothly一个真实生产材质回归RED：`./gradlew.bat test --tests 'com.iksxh.create_nuclear_industry.boiler.client.BoilerSteamRendererTest.actualPixelMixerUsesAllFourFrameDomainsAndLoopsSmoothly' --console=plain`。实际退出1，22秒，编译通过；1测试/1行为失败/0错误，白色不透明到透明黑的半帧期望0x80ffffff而旧生产Frames.mix实际为0x80808080。原始log/exit/XML在consumer/alpha-red.*，不是编译缺类RED，未运行其余准备测试或assemble。

锁定shader在entity_translucent.fsh:21–26读取纹理后乘vertexColor；RenderStateShard.java:76–86对RGB用SRC_ALPHA/ONE_MINUS_SRC_ALPHA，确属straight-alpha。精确源片段保存consumer/straight-alpha-locked-source.txt。生产lerp已改为alpha加权RGB插值后反除alpha，全透明输出0；帧/同类型still-flow/两库存配色全部复用此逻辑，避免透明黑在alpha混合前再把RGB变灰。

回归去掉调用生产lerp计算expected的镜像断言，固定数值预期包括不透明雾纹域0xff3a4a5a/0xffaabaca及混合0xff728292、半透明白0x80ffffff、不等alpha蓝/红0x60aa0055、两全透明颜色输出0；循环与源副本断言保留。修后未重跑GREEN，留到实际容量ABI与最终BER的唯一组合test+assemble。素材报告已读取40/40安装一致exit0，本消费者未重复生成/校验资源。仍等待PM容量getter，未接猜测ABI，完成本次整改后停写。

## 实际容量交接后的最终候选

已完整读取06卡最终生效节、容量合同与实物HANDOFF，消费实际`public int syncedSteamCapacityMb()`。新增BoilerSteamRenderer及独立CONTROLLER_BE注册，原Window水位BER/Fill、所有共享源码和05B冻结保持只读。实际采样仅在渲染入口完成，核同一世界/当前BE槽位/局部控制器ID、有效descriptor与L1的dimension/owner/generation/revision/min/max/partition全部一致，再一次读取NORMAL/SUPERCRITICAL库存及实际同步容量；没有在L1通知回调读取旧容量、没有NBT/反射绕取容量或调用结构扫描。

可靠Controller BER进入global列表，但以真实汽域有限AABB及到汽域最近点64格距离裁剪；未知恢复单格包围盒并无雾。14quad固定网格全汽区覆盖，库存不改高度；实际内部air采光点若局部不再是已加载air则安全无雾，采光逐点使用LevelRenderer而非宿主光/满亮。owner局部位置与法线共用生产Pose，UV以世界整数格同相锚定。内部帧10显示tick一帧、80tick循环，竖片UV每tick .012格上行；真暂停同时冻结tick与partial。一个16×16材质/一个单炉网格，64固定槽，撤销/卸载/世界切换/reload先安全提交本批层再清槽，不修改反应堆批次。

**透明度证据边界**：生产基准alpha在库存正值时为(.035,.18]，外片乘.7、内片乘.35；最大顶点alpha分别.126/.063，并经原生byte量化，纹理alpha也参与。14片的视线叠加不是素材preview的22%/82%单层alpha，不能复用素材preview声称实际浓度显示通过。实际shader还保留深度写入和片排序；气室光、斜视窗玻璃、FAST/FANCY/FABULOUS可见度及普通两种蒸汽管/罐/桶/重载仍是用户客户端门。

单项透明边缘RED原证据保留；修后的真实帧/域/库存alpha加权RGB已纳入最终回归。最终唯一命令`./gradlew.bat test --tests '*BoilerSteamVisualStateTest' --tests '*BoilerSteamRendererTest' assemble --console=plain`：实际exit0，BUILD SUCCESSFUL in 19s，9任务（4执行/5最新）。VisualStateTest 4/4、RendererTest 7/7，共11/11，零失败/错误/跳过。原始日志/exit/XML在本consumer目录。不clean/rerun/full build/GT/游戏，无Git写；未重复素材源校验或安装核对。

新增行为覆盖实际Controller.readClient完成后生产采样使用9000mB而非默认18000、两独立库存/库存0/容量0/停热库存、可靠对象/世界/本地ID/旧修订/撤销/缺块门、真实汽域AABB/offscreen/有限距离、生产Pose/airlight/alpha/慢上行UV、64网格缓存界限，以及此前纯状态/混色/安全池/阶段门。实际输入读包fixture只替代有限外部世界槽位，没有反射Controller私有字段或复制生产采样算法。没有重复L1同步矩阵。

唯一离线证据`production-offline-sample.json`由实际测试消费生产Mesh/Frames/alpha导出；`production-offline-sample.svg`依据该结果做48%超临界、22%/82%浓度的正交透窗样本，标出真实Y45..50汽域与隔层。取真实Z方向外/内片权重，遵循纹理alpha阈值及straight-alpha合成；中性背景、假设材质光=1，不含真实玻璃/气室采光/GPU深度排序。该证据用于审查范围和生产透明度差别，不是素材作者工具或游戏资源，不作为GPU验收。

冻结`candidate-manifest.json`与`frozen/`绑定7源、21实际专属生产class、36PNG＋4mcmeta、共享Controller一源及两个实际class。21class/40资源与唯一JAR entries逐字相同，共享源码SHA仍`843abb4ec1a3fbe5517db4f248cf5505582e90b9f5baad1d5b5078f15e48c796`。候选JAR 2587701字节，SHA256 `b5764e6f2630542975223c4b2a2bff16de0b3f663d1868a95903ce4fbad63ee5`；此为包含既有05B候选的06制品，旧05B冻结JAR不变。资源来源/40安装一致复用资产作者原报告，本次只核最终打包绑定，不建全仓保护矩阵。

消费者完成即停写，交负责人安排既定一次独立组合审查。不是用户游戏视觉验收或Git集成。
