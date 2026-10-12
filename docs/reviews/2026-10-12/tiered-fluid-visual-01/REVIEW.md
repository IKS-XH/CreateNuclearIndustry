# TIERED-FLOW-VISUAL-01 独立规格与质量窄审

审查结论：同一次审查发现的两项P2均已由原执行者窄整改并完成差异复核，最终冻结候选无剩余必改项。此结论仅为本卡源码、资源、自动证据及制品身份审查，未代替用户客户端视觉/鼠标验收，不执行Git写入、任务验收或main集成。

基线：E:/MyMC/NewMod/Create_NuclearIndustry-fluid-network，codex/tiered-fluid-network-flow，HEAD 58d3e2378c6de8e446ab389711f74d92c578cca3，原生产a5d1fe6。实际读取本树AGENTS、活动视觉卡、治理5.1/5.2，应用minecraft-modding、minecraft-testing、minecraft-resource-pack、requesting-code-review及verification-before-completion技能；技能入口与锁定版本见IMPLEMENTATION。按治理5.1复用执行者原始日志，不重跑测试、构建、GameTest或客户端。只写本REVIEW文件，没有修改生产/测试、世界或配置，也未派其他执行者。

## 两项P2及关闭证据

1. **P2：原指针未转到窗口面，已关闭。** 首次IndustrialValveRenderer.java第24～27行仅在管轴Y时倾斜，管轴X/Z的pointer仍处于y=13.5/2.5；锁定Create原vertical_open壳体顶部/底部分别在y=14/2，顶部#2纹理UV=[9,1,15,7]采样全部alpha255，故指针被实心壳遮挡。实际读取锁定JAR模型以及Create FluidValveRenderer/FluidValveVisual字节码，原渲染对水平FACING使用Rx90。最终renderer第24～27行统一调用生产同源valvePointerTilt(+90)，Z管轴先随body作Ry(-90)，Y管轴保留原Create90度开度基准。三轴两端pointer落入body局部z=2.5/13.5窗口，0/100分别与管轴垂直/平行；原开度、红石门控和服务端状态没有修改。
2. **P2：透明窗口默认solid写深度，已关闭。** 首次四个阀body父包装只有parent/textures，原Create父模型也没有render_type，独立DeferredBlock没有其他cutout登记。锁定MC1.21.1的rendertype_solid.fsh没有alpha discard，原fluid_valve正面#2 UV=[1,1,7,7]存在alpha0圆孔；即使修正指针方向，solid窗口仍可遮挡内部指针。最终仅两材质各body_vertical/body_horizontal/item共六个包装第7行声明minecraft:cutout_mipped。实际锁定NeoForge21.1.219 NamedRenderTypeManager源码确认该默认命名空间内建ID。原pointer所用UV、泵body/cog、管rim采样为alpha255，维持原solid，不扩大到无关渲染层。

两项新增回归在07-review-red.log真实2/2失败；最终08-review-green.log及review-IndustrialVisualContractTest.xml记录9/9、review-IndustrialDataContractTest.xml记录2/2，零失败/错误/跳过，22秒成功。新回归读取真实pointer两个平面的端点/中心，经生产同源矩阵投影至窗口，再采样原壳体UV的alpha0；同时检查内建render_type。读取09-review-assemble.log，整改触发第二次必要增量assemble，2秒成功。首次测试/包及失败历史保留，不把首次9/9扩展为已覆盖指针可见性，也不宣称全批仅打包一次。

## 本卡规格与质量核对

- 五ID的body/item/particle/face父链及#变量递归到实际Create/Minecraft PNG；原smooth_quartz错误引用退出。源模型几何通过Create parent继承，无复制或重画elements。来源清单锁定Create6.0.10-280的JAR SHA、每件原模型/PNG SHA，署名/许可文件及PNG元数据如实保留；本地候选沿用项目许可，没有发布或重新授权原资产的声明。
- 两管原30条中央multipart逐件复用，六布尔适配原rim_connector/rim，孤立管补齐原三轴core。已有合同按两管各64连接组合核原elements/UV，覆盖六向、弯头与分支；形状仅匹配8像素core及10像素法兰包络，连接/拓扑不变。阀以现axis选原X/Y体及Z旋转，没有加入不存在的Create状态属性；形状使用与原FluidValveBlock相同的AllShapes.FLUID_VALVE按管轴取值。
- 实际Create PumpRenderer的getRotatedModel使用CachedBuffers.partialFacing(COG,state)，本批同源原cog链一致。基类全局visualization跳过不适用于没有Flywheel实例visual的工业BE，独立renderer直接调用原renderRotatingBuffer，避免活动件消失。UP箭头位于法兰y=13.1～15.9、侧面1.99/14.01，对齐后cog轴向区间3～13，与活动齿轮分离；六朝向矩阵和正负getSpeed均读取实际outputDirection，停转/超载隐藏。此为方向标记，不额外报告实际流量。
- 四个新增图形类仅由Dist.CLIENT MOD订阅器引用，普通生产入口没有新增客户端renderer/字体加载链。PartialModel在烘焙前RegisterGeometryLoaders事件触发静态登记，renderer注册使用原事件。未建立原生FluidTransportBehaviour、能力或新状态同步。
- PumpBE/ValveBE首行实际调用现GoggleTooltip.indentFirstLine，原翻译键、参数和后续行保持。保护区域源差异限于这两处首行/import与Pipe/Valve getShape；NBT、持液缓存、流向、预算、放置/扳手、红石加载安全、流量配置、禁运及其他设备源码保持。原DataContractTest仅把固定7条数量改为受支持属性/模型语义，原配方成本、产量与相关断言保留；新增合同补全64连接几何语义。
- 独立SVG调色producer保留原像素位置、尺寸、alpha与明暗顺序，八张SVG/PNG逐像素验证日志已读。整改producer仅同步六包装render_type，原PNG/SVG未变。无AI重画、全设备producer、技术栈或许可证变更。

## 最终冻结身份

读取source-manifest.json、jar-verification.json、10-review-delta.json，并独立只读计算核对实际文件：154份src/tools来源全部SHA匹配，143个资源/编译class与JAR逐字一致，无不符；与pre-visual.jar相比无删除或清单外业务项变化。与first-visual.jar相比仅10份来源、8个JAR entry变化，精确对应两客户端类、六阀包装、测试与producer，未新增/移除其他包项。

最终build/libs/create_nuclear_industry-0.1.0.jar为2,853,790 bytes，SHA256 **be28ad02d98152532932595912dda375448bb46dce84abd183be0fb56db0d03b**。首次视觉包79756f05a15eb738178463905c30ec336238fb2a1455eed4d4652af57afd9a5f及first-*清单、原功能包364ec4277fc8c8b8ca2a99ef114a86ab17e5fff73a311b905c18d4d21edbf053均保留。原a5d1fe6的543/543及工业20/20等证据按卡复用，不冒称本次重跑。

## 保留的人工门

未启动客户端，未观察真实GPU、Font、鼠标或当前存档。五新增部件的六向/弯头/分支轮廓、钢灰/陶瓷调色、泵正反转/扳手翻向箭头、两浮窗图标避让，以及阀三轴0/100指针与原开度控件操作仍待用户集中复看。自动资源/矩阵/原始日志和制品核对不能关闭这些门；原锅炉16³及独立美术门保持。
