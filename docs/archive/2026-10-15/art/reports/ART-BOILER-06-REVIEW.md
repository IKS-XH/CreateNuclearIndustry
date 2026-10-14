# ART-BOILER-06 独立组合审查

## 结论

规格符合：通过。内部质量：通过。必改项：无。本结论仅覆盖冻结候选的源码、素材、原始自动证据和离线表现，不代替用户游戏视觉验收，也不授权合入main。

## 范围与执行

实际读取本树AGENTS、美术入口、治理1.2/5.1/5.2、更新任务卡、批准方案及AUDIT、实际容量getter HANDOFF、ASSETS与CONSUMER报告。历史准备段按其当时时态理解，以末段冻结交付为准。复用同轮前置原生渲染边界及透明插值窄核；本次完整审读五个新增客户端类、两个专属测试、素材pipeline/mapping及原始证据。不重开05B/L1或反应堆旧矩阵。

实际应用技能：minecraft-modding（客户端事件、BER与真实接口边界）、minecraft-testing（生产路径断言、RED及XML证据性质）、minecraft-resource-pack（SVG同源、PNG/动画与NativeImage格式）；入口为`C:/Users/IKSXH/.codex/skills/<技能名>/SKILL.md`。同轮已读取requesting-code-review与verification-before-completion，按治理5.1复用已运行证据，未派代理或重复验证。锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。仅写本报告，未运行测试、构建、生成、游戏或Git写操作。

## 实际核查

- Renderer在客户端完整packet读取后的渲染入口消费实际`syncedSteamCapacityMb()`。采样核当前世界、真实Controller槽位、局部registry ID、L1成员及descriptor的owner/代次/修订/尺寸/隔层；库存与服务器容量各采一次。未知、撤销、非法容量、零库存无雾，不按Status/窗口Fill或默认配置推算容量；查询不强加载区块。
- 网格覆盖真实上部汽域，向内epsilon，固定14 quad而非库存水位或逐cell枚举；全炉AABB、global BER offscreen及到汽域的64格距离裁剪协同。顶点与采光坐标处于合法汽区空气，实时世界光经owner局部Pose提交，法线与绕序匹配。水区、隔层和壳体不被扩展覆盖。
- 连续库存比与浓度分别用于色彩和alpha，同源帧及still/flow共用alpha加权RGB再反除alpha的ABGR混合。真实白色不透明到透明黑半帧应为`0x80ffffff`；旧`0x80808080`会在原生straight-alpha混合中再次压暗RGB，因此此前修正必要且正确。原生shader纹理alpha裁剪发生在乘vertex alpha前，不能将低顶点alpha误判为全部丢弃。
- 实际锁定NativeImage/DynamicTexture链：新纹理默认GL_REPEAT，`upload`传clamp=false，NativeImage仅clamp=true时改为CLAMP_TO_EDGE；纹理筛选不改wrap。因此超出0..1的汽域UV可按格循环，并非边缘色铺满。此为原生源码审读，不是GPU运行实测。
- 64个固定纹理槽与固定buffer有界；更新上传先于顶点，绘制期间不驱逐仍用槽，安全阶段结束自有层后释放。AFTER_BLOCK_ENTITIES在全部BER之后、区块透明玻璃之前提交，不在单owner中flush。暂停冻结tick与partial，世界session、owner失效、卸载及reload撤销旧缓存/材质；坏资源降级无雾。未修改既有反应堆批次或服务端账本。
- 素材pipeline以32份整数rect SVG为绘图源，mapping生成四普通atlas/四mcmeta及32内部帧；复用原始源检查与40/40安装记录。实际查看32帧contact sheet及生产离线截图，乳白/青白云纹、浓度差异与上部汽区/下部水区分离可辨。离线生产样本取14quad、48%配比、22%/82%库存和基准alpha约0.103/0.166，明确neutralLight=1；素材单层示意不冒充生产浓度。

## 冻结与原始证据

冻结入口：`build/reports/art/ART-BOILER-06/consumer/candidate-manifest.json`，SHA256 `be680503a9f8e72c5240f0fa1ef19e920adbb77ebee5bf11412cc67c394a4bae`，HEAD `b8cb60af0e81ea5404f1ad0bf5d1ed420da1d9df`。

本次只读逐项核对7源、21编译class、40资源、Controller只读一源/两class及两份XML；当前文件和manifest身份一致，JAR内21客户端class、40资源与两依赖class一致。候选副本与build/libs均为2587701字节、SHA256 `b5764e6f2630542975223c4b2a2bff16de0b3f663d1868a95903ce4fbad63ee5`。未重新执行旧保护矩阵，旧素材/来源检查按本批原始记录复用。

实际读取consumer下`junit-assemble.log`、`.exit.txt`及两原始XML：唯一最终定向test+assemble约19秒、exit0；RendererTest 7项与VisualStateTest 4项均零失败/错误/跳过。测试调用真实生产sample、mesh、Frames混合、pool、阶段选择、Pose/VertexConsumer提交及实际Controller packet路径，不是源码字符串镜像。`alpha-red.log/xml/exit.txt`保留旧透明混合的一项AssertionFailedError/exit1，其后独立固定数值断言纳入最终green。测试对GPU、Font和实际玻璃排序没有证明力。

素材原始检查/安装证据在本批assets目录；原生straight-alpha依据在consumer/straight-alpha-locked-source.txt；生产离线输入/输出为consumer/production-offline-sample.json、.svg及production-offline-browser-crop.jpg。

## 人工门

真实GPU透窗可见度、跨透明层排序、夜间/实际空气光、鼠标移动观察及暂停/重载后的游戏动画仍待用户；普通蒸汽与超临界蒸汽在管/罐/桶中的显示同样待复看。原生entityTranslucent的深度写入及跨批次排序不因CPU预览或11项测试而被宣称全场景无问题。02R1及其他独立人工门不由本批关闭。

同轮文案收尾：只读复核preview/index.html第47行，已改为正式材质已安装、运行渲染已接通、实际透窗与管/罐动画待客户端复看；此为无行为进度文字修正。当前pipeline.py仅检查预览文件存在，没有HTML进度句生成模板。32 SVG、40正式资源及客户端源码/class/JAR冻结结论不变，无需重复素材矩阵。

