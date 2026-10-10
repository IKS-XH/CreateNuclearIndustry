# ART-REACTOR-03R1实施报告

本批已完成模型/材质与实际渲染采光整改、18/18定向JUnit及一次最终增量JAR，交负责人做一次组合独立窄审。用户客户端视觉门仍未通过；离线OBJ投影与自动断言不替代九管/棒体厚度、白天拔出及堆内杆细节、非零库存液体透窗/液位/混色的实际观察。

## 实现与实际来源

燃料九轴仍为X/Z4、8、12，八边外径3.5单位；底板Y0..2、顶板14..16、管身2..14。辉光每管偏置0.0005方块已烘焙且与新钢管同轴，不做全模型XZ缩放。控制shaft外径5、单位Y0..1；黄铜端部外径6、厚3/16方块，固定端部不随行程缩放，完整棒按actualDepth整体升降/上方露出，包围盒与中文说明同步3/16。只修改steel/control两份整数SVG，保留钢灰棱面、纵槽、浅色分段箍纹；其他17SVG、palette与mapping原字节不动。

实际生成OBJ/UV与baseline旧OBJ同尺度/同光照投影于本批五张PNG：geometry-comparison、three-module-views、control-comparison、material-comparison、item-display-comparison。物品展示使用已冻结原版block/block的GUI/双手持变换；生产静态包装parent保持，partial无parent。负责人已实际复看全部五图并认可方向，准许安装。生成依据见preview-provenance.json与preview.py；CPU投影不是世界采光证明。

运行控制杆通过生产renderPiece提交入口，把实际bottom平移/travel缩放或head实际top平移放进SBB自身变换；外部BER Pose保持基准，useLevelLight矩阵仅为cap世界平移，保留原生法线和非全亮钢材。液体生产submitFace按Face.cell合法空气位置调用LevelRenderer.getLightColor，同面四顶点使用该值；不从boundary取邻外壳、不继承仪表0光。原库存/并集拓扑、连续冷热混色、UV/alpha/帧均保留。材质prepare失败每次reload诊断一次资源组和异常，仍撤旧材质，无逐帧日志。

审计历史保存库存8748+126mB、合法空气SKY11..13及外露15用于定位宿主0光差异，不能称为截图实时库存；本执行没有启动客户端或读写用户存档。采光合同来自卡尾及audit中实际MC源码/Catnip字节码，未补最低亮度、全亮液体或增大alpha。

## 真实red与最终green

| 检查 | 实际结果与证据 |
| --- | --- |
| baseline | exit0；66允许路径原字节/mtime另存baseline/；旧03B JAR身份7ef99a…441f |
| 首次生成 | exit1；严格SVG根属性异常，补齐仅两源的crispEdges后生成exit0；共享renderer未改 |
| 五张真实OBJ/UV预览 | exit0；preview.log/provenance，负责人实际复看通过 |
| 行为red | exit1；18项中原16通过、新2断言失败；red.log与red/两XML/三源码副本 |
| 独立OBJ尺寸/同轴/闭合/绕序/UV | exit0；asset-check.json/log，九管保守间隙0.5模型单位 |
| 重复生成 | exit0；33/33字节和mtime保持，实际输出改变0文件；generate-repeat.log |
| 最终test+jar | exit0，19s；18项全通过，无failure/error/skipped；final.log与final/两XML |
| 来源/范围核对 | exit0；verification.log/json、resource-source-installed-jar.json |

red先提取原绘制行为为真正生产入口，再以record VertexConsumer实际提交液面和record SBB提交边界断言。液面实际查询[BlockPos(10,20,30),…]宿主而期望合法空气[BlockPos(11,21,31),…]；杆worldCalls期望1实际0，证明仍固定宿主光。失败为AssertionFailedError，不是编译失败或源码字符串。之后才修生产入口。

新增两项验证保留原16：液面实际顶点的空气SKY12、白色RGBA、原alpha、连续UV、overlay和法线；杆以真实column actual=0/.5/1且target=.9调用生产位姿/提交，shaft/head世界采样位置正确、外部相机Pose未进入采光、无重复变换。SBB边界记录使用原生PoseStack/JOML变换，实际Catnip稍内缩顶点采样行为由保存字节码证明；没有复制生产矩阵或六面算法自证。

安装前asset-check首次遇子Python默认GBK输出被按UTF8解码导致UnicodeDecodeError（随后None拼接TypeError），未复制资源；保存asset-check-first.log/exit1，给子进程加-X utf8后同项检查exit0。它不是行为red。首次SVG属性异常也单独保留generate-first.log/exit1，不混作TDD证据。

唯一最终命令为固定工作区下JAVA_HOME=Java21的`gradlew.bat test --tests '*ReactorAnimation*Test' jar`。只跑本批两测试类；原16作为同类回归保留，没有GameTest、全量套件、L2/CT旧测试、客户端、存档、旧素材矩阵。保留两条EventBusSubscriber已弃用API警告及Gradle未来版本警告，不升级版本。

## 制品、边界与冻结

候选副本：`build/reports/art/ART-REACTOR-03R1/candidate/create_nuclear_industry-0.1.0-ART-REACTOR-03R1.jar`，2,474,232 bytes；SHA256 `ec9901a46d95707efdb2c4bc2b63f52fa76a3223f6e115950062eba2f8a44ed1`。

19项路径保持原03B集合：17JSON/OBJ/MTL/PNG逐字节generated→安装→JAR一致；两mcmeta与原mapping元数据语义一致且安装→JAR字节一致。本次仅4OBJ＋2材质PNG（6项）变化，13正式资源不动，未安装16单帧图块或改全局流体。相对旧03B的1056assets有1050保持；相对旧02R1的1038assets仍1037保持，唯一旧项变化仍为03B已授权燃料静态包装，原item保留。该检查为JAR字节对照，不重跑旧矩阵。

66原有允许路径本批变22、保持44，详见verification.json；其中Java实际变4类和1测试，Events/Models/MaterialsTest原字节保持。共享L2 R2十二路径、CT两类＋专属测试三源、17SVG、mapping及四旧冻结manifest原SHA保持。03A/03A1/03B旧报告/证据/JAR不改写，本次已授权作者/模型漂移的原字节另存本批baseline，不声称旧冻结源哈希仍匹配已整改文件。执行者无Git写、卡外资源/代码/配置或维护任务状态。

开工HEAD a0cf5f3…786；最终HEAD d36e79134b822a8eb8c5e058b241674e7050ad94，期间主PM只同步AGENTS、docs/README、美术README、03 HANDOFF四文档；R2源码12/12哈希仍一致。logs/debug.log、latest.log原有累计修改，本轮JUnit自动续写，最终哈希单列；未手改、清理或回退。手写范围无行尾空白。

冻结入口`build/reports/art/ART-REACTOR-03R1/frozen-manifest.json`包含66允许路径＋本报告、实施证据/原字节备份/新旧候选哈希；audit归只读依赖另列。源码与资源现冻结，交负责人接一次独立窄审。没有额外生成/构建。常规Catnip世界Matrix4f在极大世界坐标仍有float精度限制，本批未扩全局渲染框架；原生透明排序及实际采光视觉仍须客户端确认。

实际应用已读minecraft-modding/testing/resource-pack、systematic-debugging、TDD与verification-before-completion，以正式卡/治理精确权限及MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6锁定API为准。
