# ART-ACCIDENT-CORIUM-22 唯一组合窄审

2026-10-23。**本批精确改动范围内未发现未闭P1/P2阻断或必须先整改的资源问题。** 素材、预览执行者停写后，合并规格、质量与已有证据作本次唯一审查；未运行测试、构建、生成器、资源验证、HTTP或浏览器，无Git／世界／主工程写入。唯一写入为本报告。

## 合同与实际差异

已读主工程最新AGENTS、治理1.2／5.1／6、STATUS、联合协作卡、04B及创造拆除卡，事故树22卡仅作美术分工。实际应用 `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`、`minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`，分别核资源／动画引用、客户端sprite边界及证据复用。实际Gradle保持MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。

审查树为 `E:/MyMC/NewMod/Create_NuclearIndustry-accident`，来源 `codex/accident-thermal-04b`／`1985d1b3172a5efd1f0c87f3aa39ce3fe2a6feb7`。读取实际Git差异、生产JSON／mcmeta、代表SVG内容、作者prepare／produce／verify、预览scene／animation／server／提取程序、mapping／manifest、两执行报告、负责人OBSERVATION及现成日志。

- 17项允许生产集实际更新15项。原reactor_corium的hot=false／true blockstate及其冷块item父级保持；冷热模型仍为原完整cube_all／实际all及particle，废料generated指向自有PNG，流体模型仅换particle而无自建几何。
- 唯一Java差异是AccidentThermalClientEvents的still／flow两处ResourceLocation及对应中文注释。没有新增状态、光源、伤害、粒子或物理分支；实际getFlow=ZERO、既有流体light12、固体OBSIDIAN属性保持。现有验证将该Java逆除两引用／注释后恢复原字节。
- atlas保留原六single source，末尾仅加本批两fluid sprite。已读atlas-newline-update.json：只去掉新增项之间额外一个回车，PNG／SVG未重导；最终atlas SHA为 `687d361c364b5bf1728571875cc45cf5ad8e7f1f79bc69c57b3cfa1363d657e0`，原六项字节前缀与结构保持，CRCRLF=0。该身份与最终manifest一致。
- 原6,888项非批tracked文件按已有一次验证逐字节保持；实际tracked差异也限允许六个已有文件。原事故、创造拆除修复、配置／注册／loot／recipe、根跟踪JAR和日志没有本批改动。世界未枚举、复制、启动或回写，不借本次外观任务重开事故功能审查。

## SVG、PNG与动画

五可编辑SVG为整数rect／crispEdges真源，produce先写SVG再从SVG导出RGBA8 PNG。已有唯一verify为PASS_STATIC_AND_INCREMENTAL_COMPILE、invocationCount=1、exit0：共18,944像素SVG→PNG零差，方块／流体完全不透明，废料保留透明外边。

| 纹理 | 帧与节奏核对 |
| --- | --- |
| 冷残骸 | 16×16、单帧，暗氧化缝和灰银碎片，无余热亮色 |
| HOT残骸 | 16×64、四16²帧，0/1/2/3/2/1、20tick并插值，循环6秒 |
| still | 16×64、四16²帧，同往返序列、16tick并插值，循环4.8秒；大壳固定，小液筋／碎壳至多1px往复 |
| flow | 32×512、十六32²帧，0…15、6tick无插值；16px空间周期铺成2×2，逐帧下移1px，末首继续同一位移 |
| 废料 | 16×16单个厚块轮廓，少量银灰碎片，16／32像素可辨 |

冷热共享大结壳布局与金属色，HOT只改变裂隙；源算法及已有验证覆盖固定143结壳像素、首尾变化和flow平铺／末首连续性。实际mcmeta尺寸与生产纵排帧一致，没有将32宽flow误按16宽分帧。原生LiquidBlockRenderer锁定源码149–181、284–288分别确认ZERO流向顶面选still、非零中心旋转与侧面半幅采样；本批没有改变流速或原生有限流体表现。

## 真资源预览与观察

冷热由本地 `E:/MyMC/NewMod/block-model-renderer` 2.20.0真实 `parseBlockstate→resolveModelData→loadModel` 消费生产资源，原生四父链来自锁定client.jar。已对照库实际createAnimator.update(timeMs)及mcmeta解析实现；预览未另画模型或替换纹理。流体与废料从真实PNG解码，二维时间轴按实际mcmeta播放；只有mcmeta启用时进行插值，放大为最近邻。

读取正常overview及root-dark-overview、负责人操作记录，并逐图view_image查看两种环境下cold／hot／still／flow／waste共十PNG。完整立方体外侧面、金属厚边和银灰混合碎片可见；HOT橙裂隙与冷态暗缝区分明确，暗环境下两者随环境变暗。废料轮廓完整，没有黑面、反面或主体裁切。流体保持橙红液相与碎壳，不是原版熔岩／紫黑曜石／木炭替代图。

两overview均ready=true、errors=[]；冷热Box3均1×1×1、各12三角，HOT animator为animated=true。暗环境记录暂停64800ms、yaw10／pitch35.25；正常快照恢复播放与默认视角。root通过原UI实际暂停、下一帧、继续播放及拖转，console无warn／error。当前正常快照记录91305.3ms，帧值以各overview自身sample为准，不将保存后继续推进的页面读数混作同一帧身份。

页面明确侧面为独立8/9高度取样，未执行邻居平均／atlas收缩；中心半幅旋转只是诊断，ZERO流向实际顶面仍用still。明暗是材质示意，不是Minecraft光照传播模拟。CUA读取自定义window对象返回undefined已由负责人记录为DOM沙箱限制，现成UI捕获及ready证据没有页面加载错误；无需为此扩大工具或重测。

## 有限检查、写集与许可

- 已读唯一compileJava processResources原始日志及exit：调用1次、exit0、BUILD SUCCESSFUL in 13s，2 executed／1 up-to-date；仅一条原initializeClient弃用警告。已有验证绑定16个processResources输出和两个客户端class。没有test／jar／build／clean／rerun，本审查不将编译代替游戏或未来联合JAR身份。
- 既有preview CLI三份语法检查exit0，41条必要HTTP／拒绝结果符合预期，真实帧边界／回绕／半帧插值／下一帧记录通过；未重复作者SVG像素检查。服务仅127.0.0.1:4210，session68748／PID80084，固定资源白名单、同源固定五PNG及overview捕获目录，不开放任意文件或世界。
- 工具源、五SVG、四原版提取、预览程序及捕获均在本批专属工具／报告路径；库、wasm及Three直接只读服务，无安装、外部库或公共预览工具修改。普通编译输出为合同允许。manifest清楚排除自身、执行后日志／退出、后写说明和独立预览文件，没有自引用SHA或把他人写集冒充素材作者冻结集。
- 原创SVG／工具／素材适用本批MIT，许可证与来源实际可读；BMR保留MPL-2.0、Three保留MIT，四原版父链注明游戏原版权。事故树旧根许可／Gradle元数据保持，未借本批对整棵旧逻辑候选作许可整改，也未把第三方覆盖为原创MIT。

## 交接边界

本次无未闭阻断，可交主PM核源、提交并生成联合候选。原04B三P2整改和创造拆除RED／GREEN业务证据按未变范围复用；不重跑JUnit／GameTest／长冷却或整组流体手测。用户已反馈其余04B手测完成，本批仅集中观察新外观及创造拆残骸零掉落复看；未报告的Minecraft视觉、光影或多人不补记通过。尚无本批联合JAR身份，本报告不提前批准main合入或正式包替换。交root读回后停止写入。
