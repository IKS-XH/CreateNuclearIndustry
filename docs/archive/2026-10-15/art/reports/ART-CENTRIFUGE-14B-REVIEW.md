# ART-CENTRIFUGE-14B 单次组合规格/质量窄审

2026-10-14。原独立审查者接续用户选A正式实施；只写本报告，无子委派、生成、测试、打包或Git写入。已读最新main AGENTS、[14B卡](../tasks/ART-CENTRIFUGE-14B.md)、治理1.2/5.1/5.2和minecraft-modding/resource-pack/testing技能，锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。

**assemble前结论：当前源/11正式资源/受影响定向检查/真实PNG预览无剩余必改项，可由原唯一构建持有人执行卡内一次必要增量assemble。** 尚无本批最终JAR，本报告不提前宣称包交付；随后仅补包身份，不重新测试或生成。Minecraft视觉/真实运转人工门独立，main/Git集成不由本结论推进。

## 所选A资源与客户端接入

读取[ASSETS](ART-CENTRIFUGE-14B-ASSETS.md)、[CLIENT](ART-CENTRIFUGE-14B-CLIENT.md)、produce.py、assets.json及原始candidate/installation/readback证据。已选14A A契约6634afc2…锁定，四运行模型为lower167/upper155/rotor23/shaft1，正式转换仅六槽纹理路径，不改几何、每面UV/局部旋转或4×4×8轴及增粗转子。六SVG原字节保存，确定性像素导出六16² PNG，颜色/alpha与原SVG一致；当前alpha255，glass本来只是染色孔缘，中央实孔不被补成玻璃整面。审查只读核15来源/只读入口、11候选和11安装SHA，0漂移；原八旧资源备份保留，shaft/machined/fastener为三新增目标。

完整物品346元素的分段为lower[0,167)、upper[167,322)、rotor[322,345)、shaft[345,346)；upper from/to和rotation.origin同时+16，UV不重新投影，原display保持。正式texture ID无设计稿虚拟namespace，原blockstates八映射和item parent只读仍有效。14A其它三稿与历史、旧工具不被重画/安装器覆盖。

实际Renderer差异仅新增SHAFT PartialModel、中文职责与同angle/Direction.UP/light/cutoutMipped调用。原有效lower owner早退、signed getSpeed、非零/非超载/非BEARING_WORN及时间公式逐字保持；无全亮/假转速/super重复轴或静座跟转。CentrifugeClientEvents未改，读取非编译期常量ROTOR会初始化整个Renderer类并执行SHAFT字段初始化，现有客户端注册入口足够。无业务、碰撞、配方、供能、磨损、网络或停机语义改动。

## 定向与真实PNG预览证据

复用原客户端作者安装前专属RED：3测试/3失败/errors0、exit1/16s，缺所选A几何/新材质的真实失败保留；资产在收到此RED后才精确安装11项。首次安装后3测试/1失败/errors0，item expected28.015999/actual28.016的float ULP差保留。仅测试Y偏移比较改max(1e-6f,2×float ULP)，X/Z和UV容差保持，严格源JSON/像素身份另独立核；未改生产资源迎合断言。必要纠错后3/3通过、0失败/错误/跳过、exit0/15s，复用green-corrected完整日志/XML/result。三个测试实际使用Minecraft BlockModel反序列化核几何/旋转/UV/六正式材质、346物品upper/origin偏移及ImageIO解码，不复制角度公式或源码字符串测试冒充运行验证。

新4192/PID85932仅127.0.0.1固定白名单，读取本美术树五已安装JSON/六真实PNG、固定renderer2.20.0/Three180；不读SVG或重新画材质。prepareAssets交真实PNG字节，模型正式texture ID/UV不重绑；运行机原两格/轴线换算与静态346物品分别真实loadModel。capture限定两固定画布和本批报告JSON，按钮引用await前固定，无任意输出路径。原4191/PID84900未动。复用本批三脚本语法exit0和22/22输入/库/路由HTTP检查；该记录明确HTTP而非GPU验收。

负责人在4192/Tab14实际观察正常机与完整物品、底部侧端面、运行机剖视长轴/增粗转子及低速另一相位，无新擦壳/跳动/漏材质。审查者实看overview-final.png、cutaway.png、bottom.png，所选A钢壳/法兰/完整窗框和物品上段一致，无错位；moving.png与motion.json独立保存。运动原捕获显示shaft/rotor同相位1.2086754135912774、inPivot=true/scale.0625，下/上壳和346物品均phase0。网页24°/秒明确姿态演示，不能当成Minecraft实际RPM或停机验证。

截图/运动原捕获位于 `C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14b/`。负责人最后stop/关cut/reset/capture成功，按钮enabled；审查读回当前browser-evidence ready=true/errors=[]、12输入/19来源SHA0漂移，yaw=-35/pitch16/zoom1/cut=false/slow=false/phase0，安装契约与实物绑定。Tab14已标交付页，原服务保留；网页与有限自动证据不关闭本批Minecraft集中人工门。

## 待制品收尾

原唯一构建持有人可在精确备份旧d12cca…包后执行卡内唯一增量assemble；新包身份、11资源/变更Renderer class/输入绑定及无工具/测试载荷泄漏后续追加到本报告。本次源/资源组合审查至此结束，不重复测试/生成/包前广审。

## 唯一增量包身份收尾

assemble前审查放行后，原构建持有人先备份旧2,778,351B/d12cca860f69f0b411a99b3c7f34bacc17895c7ac4fb7bbd149616769dc839c5包，再执行唯一 `./gradlew.bat assemble --console=plain`；复用[assemble/result](../../../../../build/reports/art/ART-CENTRIFUGE-14B/client/assemble/result.json)与原日志，exit0，Gradle1秒、实测1.845秒，没有重复测试/生成。旧包SHA审查只读核仍相同。

最终 `build/libs/create_nuclear_industry-0.1.0.jar` 为 **2,815,098B / SHA-256 7d62147c84392f00062d19635cd0a5e91300d89860b1e8502cea17079e98f507**，审查只读核整包尺寸/SHA并直接读取ZIP十一资源及Renderer.class，全部与[artifact-identity](../../../../../build/reports/art/ART-CENTRIFUGE-14B/client/assemble/artifact-identity.json)一致。十一资源契约/安装/build output/JAR同源；Renderer.class为5c5fcf7291e52fe0eb224c07fc606abfb7279a22d0253f07612097d90c4a6958，与编译输出相同。作者旧包载荷对比仅本批12差异（三新增/九修改），其余1934保持；工具/网页/测试载荷泄漏列表为空，源码/输入身份不变。

复用并读取包内javap证据：clinit确实初始化ROTOR与SHAFT，两个draw均读取同一fload9角度并用UP/light/cutoutMipped；现register读取ROTOR仍触发整个类。原GPU默认姿态12输入/19来源另冻结preview-final.json，不重新浏览或测试。**源码、资源和最终制品组合无剩余必改项，候选交付可收尾。** 当前仍待用户本次Minecraft集中视觉/动画手测，main/Git/世界保持；本审查只补包身份并至此停写。
