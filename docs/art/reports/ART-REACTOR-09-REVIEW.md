# ART-REACTOR-09 独立组合规格与质量窄审

结论：本批未发现必改项。审查范围为共享补充批准的双源几何、四种基础纹理与四种CT图集及其唯一组合制品；不关闭真实Font、GPU、玩家鼠标或玩法人工门。

实际读取根AGENTS、治理1.2/5.1/5.2、09任务卡、PM复制的COORDINATION、ASSETS/CONSUMER及冻结记录，应用minecraft-modding、minecraft-testing、minecraft-resource-pack和verification-before-completion技能。版本保持MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6；本轮只读源码、原始结果和制品，不运行测试、生成、构建或游戏。验证技能按治理5.1复用已通过同候选记录，并独立核实当前文件身份。

## 规格与行为

- `ControlRodSliderBehaviour.java:92`之后的CornerTransform是生产唯一差异。UP/DOWN同一显示/命中中心为`(.5,1或0,.0875)`；scale `.20`、font `1.8/64`。顶底严格三维椭球半轴X `.35`、Z `.075`、法向`scale/2`，保留isSideActive；四侧原offset、`.40`、super字体/球热点和fromSide缓存刷新保持。非几何正文与基线逐字不变，未改变协议、百分数或服务端校验。
- 锁定Create JAR的ValueBoxTransform.transform实际先平移、原生rotate、再等比scale；Sided构造先调用基类虚getScale、再初始化UP，fromSide只更新direction，因此null默认及显式缓存刷新确有必要。生产均正确处理。Sided.testHit实际先isSideActive再父球形命中；本批顶底保留此条件，四侧仍直接super。
- 原生Pose测试实际调用生产transform与Create rotate；完整框角、0/50/100%后置矩阵、等比字面及`.65`端箍避让均有断言。长短轴、斜点、法向、严格边界、中心/旧角/邻格拒绝与跨面缓存覆盖与生产算法对应，未镜像生产命中公式。默认ASCII宽12/18/24是锁定资产输入，不等于真实Font烘焙。
- 原始RED XML为9项、9个AssertionFailedError、error/skip0、exit1；GREEN XML为9/9、failure/error/skip0、exit0，日志17秒。原生TextValueBox构造因JUnit缺Minecraft单例实际NPE已明确记录；成功范围是Pose/矩阵，不能称Font、GPU或长按鼠标已运行。

## 素材与离线观察

实际读export.py与mapping：68张整数rect SVG，四基础16×16、四RECTANGLE 64×64图集；原index12、资源ID与模型UV合同不变。上下左右位映射及孤立格同源机制正确。复用原check/install exit0，未执行导出器。独立核当前68来源SHA和8个generated→formal→processResources→ZIP逐字一致，旧八图备份保持。

独立看负责人浏览器截图`device-preview.png`与实际生产Pose导出的`consumer/slider-layout.png`：钢色导轨/中央通道与大铜色舱盖、铰链/锁扣有明确形状与明度差异；顶底框和三种字面留在本格边缘且避方箍。密集近远阵列、杆/箍遮挡和四CT图集复用已登记的负责人完整离线复看；离线示意不能代替游戏光照、真实字体和距离读性验收。

## 最终组合身份

原始唯一assemble为exit0、3秒，compileJava UP-TO-DATE，processResources/jar增量；本轮没有重复运行。候选与当前build/libs JAR逐字相同：**2624615 bytes，SHA256 `737d3bd5606109f711998da92714917de19b6ed891f0abb7fbb27ca3f1049050`**。

Behaviour源码SHA `552176998215690a6897f1e97e99e1396146877058bf334807015a91b0e82597`；Test源码SHA `2de645ca58f9b9fe608476a8c422b1186bcf9d7050f799b2f167c27ce336a7fc`。两生产class实际build→ZIP一致。净09清单99路径为双源、八正式PNG、87新工具路径与两作者报告；没有把08/R2或可变玩法列入净09。34保护记录和非几何证据复用；另只核旧08包内R2五class/稀疏PNG及08 Behaviour/window图集共八项与新包逐字一致，原08 JAR备份身份保持。

`consumer/final-freeze.json` SHA256为`79fdaf1e3895f390f40faef4248d5e72ec8380bd2a4b0008228f4fce7ec1e67d`。阶段二旧报告副本与旧身份单独保存，活动交接文档不伪装成生产输入。独立身份核对记录见`build/reports/art/ART-REACTOR-09/review/identity-check.json`；正式来源/各PNG/class哈希复用`consumer/final-binding.json`。

## 人工门

仍需客户端核真实字体0/50/100%、顶底边缘瞄准与连续长按、方杆升降遮挡、密集驱动器/换料舱盖近远辨识及CT衔接。本批未启动游戏，不关闭09视觉/操作门；08对侧玻璃/库存闪断与可变反应堆玩法的人工门分别保持。审查写入到此停止。
