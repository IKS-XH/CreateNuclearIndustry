# ART-COOLANT-04：锁定普通冷却剂材质预检

本批只读核查；实际读取 AGENTS、美术入口、治理1.2/5.1与 minecraft-modding、minecraft-resource-pack、minecraft-testing 技能。锁定 MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6。未改代码/资源/工具，未构建、测试、启动游戏或Git写操作。

## 结论

04卡已冻结PNG alpha=255，保持普通流体现有渲染/透明策略；只复用配色、八帧、流纹与元数据，不申请或实施Java透明层补充。建议每帧 still16×16、flow32×32。flow由同一个无缝16像素图案2×2重复组成，8帧纵向条带分别16×128与32×256；可保留实际注册ID及block纹理路径。尺寸不是加载器硬限制，而是匹配原生采样与像素密度的选择。

MC LiquidBlockRenderer.java:160–164静止顶面使用still全幅UV；169–181流动顶面使用flow，以UV(.5,.5)为中心旋转、偏移系数.25；284–288侧面使用flow的U0..0.5、V0..0.5（随高度裁切）。所以四象限重复能保证侧面一格仍呈16像素完整图案及顶面旋转连续；不是只把一个16图放32画布左上，也不是所有表面始终只取同一个quarter。Create原生流体BER有独立分块/UV算法，不能将世界侧面quarter规则直接套在全部Create面上。

默认 IClientFluidTypeExtensions.getTintColor():int 为0xFFFFFFFF（锁定源:60–61），世界/FluidStack重载:262–278返回同值。项目 ModFluids.java:102–125只覆写still/flow资源路径，没有额外染色。颜色与alpha来自PNG，顶点tint白色不会再次稀释。

## Alpha与现有管线边界

- Create储罐 FluidTankRenderer 字节码246–270调用Catnip FluidRenderHelper.renderFluidBox；透明直管调用Create FluidRenderer.renderFluidStream，再取得同一个FluidRenderHelper.getFluidBuilder。Catnip PonderRenderTypes.FLUID使用原生entityTranslucent shader和TRANSLUCENT_TRANSPARENCY；putVertex提取tint ARGB传入setColor。PNG alpha可以参与混合，仍受遮挡、透明面排序、照明和shader近零alpha丢弃限制。普通不透明管没有可见液体面，材质不会让管壳自动透明。
- 动态桶：项目compound_coolant_bucket.json使用neoforge:fluid_container。锁定DynamicFluidContainerModel.java:73使用translucent块层及ITEM_UNSORTED_TRANSLUCENT；流体层使用still sprite、桶mask及tint。因此PNG半透明参与液体层混合，桶底图/覆盖层仍保留，不能期待桶整体透视。hot液体未注册独立桶，本批不增加。
- 世界：LiquidBlockRenderer.java:88–89保留tint alpha；但ItemBlockRenderTypes.java:402–405对未登记fluid默认RenderType.solid()。本树全src/main/java检索没有ItemBlockRenderTypes/setRenderLayer调用；LiquidBlock复制WATER属性不会复制流体注册表渲染层。故仅PNG/mcmeta能换图与播放，不能保证世界液体半透明。这是现有管线限制；04固定alpha255并保持现有策略，无需也不安排setRenderLayer或其他Java改动。

## 仅资产动画与真实限制

四个现有textures/block/*coolant_{still,flow}.png旁放同名.png.mcmeta，animation可用frametime:2、interpolate:true、width/height明确16或32；frames省略则按行优先遍历全部帧。AnimationMetadataSection.java:34–41在未指定宽高时取图像最小边为方帧，因此纵向条带可自动分帧。SpriteContents.java:83–122会丢弃无效时长/越界帧，一帧不创建动画ticker；正常Ticker:323–340按时间切换上传atlas帧，BER/世界/动态桶都引用atlas sprite，无需自建DynamicTexture或Java动画。

interpolate只混RGB：SpriteContents.java:277–280保留当前帧alpha，并不插值alpha。应各帧保持同一alpha场，避免透明度跳变；04固定所有帧alpha255，排除透明度跳变；mipmap仍可能削弱细纹。动画是材质全局时间循环，不代表真实库存/流量，不与内部每堆runtime纹理自动锁相，也不执行冷热混色。内部动画风格可复用SVG来源和调色，而普通冷/热材质仍分别显示自己的端点颜色。

SVG导出PNG与mcmeta一致性、flow四象限无缝及引用路径检查足以覆盖本批资产自动检查；真实世界透明排序、储罐/透明管/桶和静止/流动顶面视觉仍留客户端门。本次没有执行这些检查。

## 原始证据

唯一目录 build/reports/art/ART-COOLANT-04/probe/：LiquidBlockRenderer.java.txt、ItemBlockRenderTypes.java.txt、IClientFluidTypeExtensions.java.txt、SpriteContents.java.txt、AnimationMetadataSection.java.txt、DynamicFluidContainerModel.java.txt、CreateFluidRenderer-bytecode.txt、CreateFluidTankRenderer-bytecode.txt、CreateTransparentPipeRenderer-bytecode.txt、CatnipFluidRenderHelper-bytecode.txt、CatnipPonderRenderTypes-bytecode.txt。

MC/NeoForge合成锁定源码JAR：C:/Users/IKSXH/.gradle/caches/neoformruntime/intermediate_results/sourcesAndCompiledWithNeoForge_4a83a73d95fdbeba9a47b7d6bbaabb6346ecca49_output.jar。NeoForge扩展源JAR：C:/Users/IKSXH/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/21.1.219/eefeb66da85d5ced69c1d0bc86b8140ddff77c1/neoforge-21.1.219-sources.jar。Create slim与Ponder1.0.82缓存字节码证据均直接javap，只读，不复制第三方资产。
