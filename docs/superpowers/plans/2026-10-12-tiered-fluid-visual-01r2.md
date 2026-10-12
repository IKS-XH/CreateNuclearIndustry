# TIERED-FLOW-VISUAL-01R2：管段端面、分支中心与实际箭头

> 主PM派原唯一视觉执行者完成本卡，交一次独立规格/质量窄审。执行者不Git、不改核心docs、不派子代理；治理5.1的定向/证据复用覆盖通用技能的重复全量。

**目标：** 修复用户本次实际客户端反馈的四项：单独管段贴图错误、两节相连的自由端空面、T形中心消失、工业泵箭头反向。用户明确除这四项外观问题外功能测试全部通过；只保留这四项视觉复看，不重新开启输送/禁运/库存/浮窗/阀门功能门，不自动合main或推进其他主线。

**树/基线：** E:/MyMC/NewMod/Create_NuclearIndustry-fluid-network，codex/tiered-fluid-network-flow，HEAD e48cac4，视觉609914f，原功能a5d1fe6。仍用本树和现测试世界，19:41:54实际日志已保存退出；不新建分支/复制/写世界。main必要转子显示接口与美术动画另行保持，不同步到本视觉候选。

**现有证据及技术边界：** 锁定Create6.0.10-280的AllBlocks.java为普通管安装PipeAttachmentModel；其shouldDrawCasing按非某轴的连接数>2动态补FLUID_PIPE_CASING。现独立网只复制30条静态multipart及RIM，漏此动态壳。当前mask0简单叠core_x/y/z，源模型本为部分连接面的atlas UV；mask1的自由端也未证明完整。现箭头纹理为8×8、JSON UV用[0,0,8,8]，UV单位实际固定16；此前方向测试只将名义UP向量用JOML转到outputDirection，没有检查最终glyph/FaceBakery/Flywheel绘制。这些必须实际读锁定实现并建立独立预期，不凭猜测整体反转outputDirection。

## 合同与允许写集

MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6不升级。实际读最新主AGENTS、治理5.1/5.2、本卡、minecraft-modding/testing/resource-pack、systematic-debugging/TDD/verification。中文手写注释。二级钢灰、三级浅陶瓷及原Create模型/UV/像素SVG调色继续；不改其他设备或再研究许可证。

- 保持原五ID、配方、流量/应力、六连接布尔、拓扑/预算/事务/缓存、方向计算、红石与禁运。不得添加Create原生FluidTransportBehaviour、伪造连接或改服务器流向来迎合外观。
- 可在工业client包中精确补纯显示适配，优先基于现六布尔静态补齐Create原动态壳/端面，或者必要的纯客户端baked model适配。不得扫描服务端/读流体能力来改外观；依赖原模型parent，保留原像素和UV，不重画几何风格。
- 两管64连接组合都要有正确可见中心及自由端，特别0、1、2直线/弯头、3个共面T以及非共面3、4～6。连接表面与自由表面分开，不能把整六面方壳塞进所有情况造成管孔封堵/共面闪烁。零连接选择合适原Create完整模型作为静止独立段，不拼出黑cube；纯几何碰撞匹配可精确适配getShape，不改连接/放置/扳手/调度。
- 箭头实像尖端须与原outputDirection一致，六FACING、±RPM、扳手翻向均正确；0/原生超载保持隐藏。实际读Flywheel TransformStack、CachedBuffers/SuperByteBuffer及MC1.21.1 FaceBakery，使用完整16单位UV，验证真实尖端/杆部在每个可见面上的最终位置与矢量，不只检查名义UP矩阵。保持泵原cog/法兰及已通过阀指针/浮窗。

J=src/main/java/com/iksxh/create_nuclear_industry，T=src/test/java/com/iksxh/create_nuclear_industry。

| 可写路径 | 仅允许内容 |
| --- | --- |
| J/industrial/client/** | 仅两管纯显示补面及IndustrialPumpRenderer/VisualOrientation实际箭头变换，Dist.CLIENT烘焙接入如必需；不改阀/运行同步 |
| J/industrial/IndustrialPipeBlock.java | 仅原模型匹配的getShape几何，其他逐字保持 |
| assets/create_nuclear_industry/blockstates/{reinforced_fluid_pipe,ceramic_fluid_pipe}.json；对应models/block管及原arm；models/block/industrial_fluid_visual_01/** | 仅本两管中心/端面/附件parent与箭头模型UV；原泵/阀body不改 |
| assets/create_nuclear_industry/textures/block/industrial_fluid_visual_01/**；tools/art-assets/industrial-fluid-visual-01/** | 只必要本管材质绑定或原创箭头SVG/PNG、原producer/validator/source映射；源Create材质像素/alpha不改，不生成其他设备 |
| T/industrial/IndustrialVisualContractTest.java、IndustrialDataContractTest.java；新T/industrial/IndustrialPipeSurfaceTest.java（如需要） | 精确本次渲染回归；用实际可见面/UV/像素/烘焙方向独立预期替代原不完整multipart照抄合同；成本、产量、helper与阀窗口断言保留 |
| docs/reviews/2026-10-12/tiered-fluid-visual-01r2/IMPLEMENTATION.md；build/reports/tiered-fluid-visual-01r2/** | 精确失败/根因/原日志及最终差异源/JAR清单，其他docs只读 |

卡外必要路径先报告主PM。当前主根目录可写、同级树需精确require_escalated；不得转写main生产源码绕过隔离。原候选制品be28ad02…及本次用户日志先保留为本任务证据，原存档不改。

## 单批执行

1. 实际核静态parent与Create动态附壳/附件、自由端与0连接的面/UV/alpha；核原pump实际getSpeed/outputDirection到最终绘制glyph的每一层。先报确定根因、最小修法和新增文件，不继续盲补。
2. 最小RED复现0连接纹理/自由端缺面/T中心及真实glyph反向或UV截取，旧11项通过不计为本次覆盖。新验证必须检测中心/闭合自由面/UV有效区域，不能把原缺失模型列表当预期。
3. 只改上述显示范围，沿用Create正确资源/绘制方向；运行受影响定向组，保留已通过阀/图标合同即可，不跑GameTest/完整build或热工/旧存档矩阵。
4. 稳定后唯一必要增量assemble，冻结本批差异及最终class/资源/JAR身份，核旧包差异只属于本卡；失败/独立审查确实需窄修时保留实际记录再做必要增量。
5. 中文简短交付。PM安排一次独立窄审，仅读证据不重复测试。通过后交本候选四项局部人工复看；功能通过已记录，不用自动检查冒称GPU结果。未关闭四项外观门前不合main。

**自审：** 四项客户端失败均在本卡，原测试的静态模型/名义向量缺口被替换为实际表面/像素/渲染方向合同。没有新玩法决策，输送与现已通过功能保持，不扩大独立美术或只读转子前置。
