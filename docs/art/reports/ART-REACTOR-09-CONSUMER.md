# ART-REACTOR-09 控件消费阶段

本批内部组合已完成并冻结，执行者停写。仅修改原 `ControlRodSliderBehaviour.CornerTransform` 与 `ControlRodSliderTransformTest`；素材由原作者独立持有，只执行下文一次assemble；未执行业务/protocol测试、游戏或Git写。实际HEAD `2ae83b9c9d355c5229f973849e04c449dea0ceb5`，按 [09共享补充](../ART-REACTOR-09-COORDINATION.md) 与生效任务卡实施。

## 改动和真实行为验证

顶/底锚点 `(.5,1或0,.0875)`、scale `.20`、font `1.8/64`，文字等比且标准化宽 `.151875` 格，为原两倍；本格椭球X/Z半轴 `.35/.075`、法向 `scale/2`，严格平方和 `<1`。保留isSideActive检查；四侧原offset、`.40`、super字体/球热点以及fromSide缓存路径保持。非几何正文（0..100、formatter、读写、回调、提交/response、展示值钳制）原字节保持，没有新同步或注册。

实际命令均由 [`run-gradle.py`](../../../build/reports/art/ART-REACTOR-09/consumer/run-gradle.py) 保存：`cmd /d /c gradlew.bat test --tests *ControlRodSliderTransformTest --console=plain`，cwd `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，JAVA_HOME `C:/Program Files/Java/jdk-21`。

- 原生产运行新增/更新几何断言：[RED原日志](../../../build/reports/art/ART-REACTOR-09/consumer/red/gradle.log)，自然exit1、15秒，9项中9个实际AssertionFailedError，error/skip0。失败是旧位置/尺度/默认字体与旧球热点，编译和原生夹具可用。原测试副本、XML与exit独立保存。
- 最小生产修改后：[GREEN原日志](../../../build/reports/art/ART-REACTOR-09/consumer/green/gradle.log)，自然exit0、17秒，9/9，failure/error/skip0。保留原生事件注解弃用及Gradle弃用警告，未额外整改依赖。
- 覆盖六面真实生产transform与Create原生rotate、最终6PX框/文字后置Pose；0/50/100%字面完整边界和等比、端箍避让；椭球长/短轴/斜点/法向/严格边界；中心、旧角、杆体、方箍和邻格拒绝；UP→SIDE→DOWN→SIDE尺度/字体/命中缓存，四侧原退路。

## 字体和离线投影边界

锁定1.21.1 client.jar默认非uniform ASCII中0/1/5/%实际advance均6，`0%/50%/100%`宽12/18/24。仅读取源数据作为后置矩阵输入，来源资产SHA及局限见 [`text-inputs.json`](../../../build/reports/art/ART-REACTOR-09/consumer/text-inputs.json)。复用锁定Create `ValueBox/TextValueBox`后置字节码和既有无mixin的Flywheel原生Pose扩展夹具；不复制生产offset/命中算法，不生成自定义旋转。新增字面检查重放第三方固定后置指令，真实Font/GPU绘制没有运行。

完整原生TextValueBox构造器在JUnit实际抛 `java.lang.NullPointerException: Cannot read field "level" because the return value of "net.minecraft.client.Minecraft.getInstance()" is null`，保存于GREEN XML。测试只通过实际生产Pose/原生旋转及完整后置矩阵，不称实机render/鼠标通过。原长按每tick调用同一testHit、失去命中cancel的审计复用03R2锁定字节码，不新增长按探针。

[`geometry.json`](../../../build/reports/art/ART-REACTOR-09/consumer/geometry.json)直接提取GREEN XML：24框角、6基线、72字面角与6布局。由 [`render-layout.py`](../../../build/reports/art/ART-REACTOR-09/consumer/render-layout.py) 生成 [布局SVG](../../../build/reports/art/ART-REACTOR-09/consumer/slider-layout.svg) / [PNG](../../../build/reports/art/ART-REACTOR-09/consumer/slider-layout.png)，执行者已打开核对。图中方箍/杆体仅已冻结XZ占位，完整升降时该切向投影相同，0/50/100标签是实际数值字面，不冒充运行控制棒动画或客户端截图。首轮投影筛选大小写导致的ValueError保存在layout-first-error.txt；仅修投影筛选，未改源数据/生产/测试或重测。

## 阶段二来源与保护（历史）

基线双源原字节保存在本批baseline，Behaviour原SHA `5479f5f14418f4d6a499fc01a825735d4646123a018ce74176fc8605f652d83e`，Test原SHA `cc740929108f0a555866893e6812d4ee06c4f03c09fe903745dd61cc7c342d24`。

当前Behaviour SHA `552176998215690a6897f1e97e99e1396146877058bf334807015a91b0e82597`，Test SHA `2de645ca58f9b9fe608476a8c422b1186bcf9d7050f799b2f167c27ce336a7fc`。[`scope.json`](../../../build/reports/art/ART-REACTOR-09/consumer/scope.json)记录当前源/两compiled class、实际结果与34项保护集合（control其他生产/测试和structure/client生产），零保护变化；四侧offset及fromSide尾部逐字保持。双源定向diff --check exit0。日志debug/latest为既有/自动JUnit改动，未手改、清理或回退；未保存本阶段日志前基线，不将其全部归因09。

阶段二时JAR保持ART08 `2624312` bytes / SHA `a4b08dd0a5bebe72555dc44969553ede120080ef66ff2ab38d27974648d353c7`，未生成09 JAR。MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6不变；实际应用minecraft-modding/testing/resource-pack、systematic-debugging/TDD与writing-good-tests，精简验证服从治理5.1和共享窄卡，原业务/03/08矩阵复用。

阶段三已按负责人追加授权完成素材安装后的唯一组合与绑定，具体身份见下节。用户真实Font、近远辨识及顶面长按门待现有存档复看，不改写ART08/可变业务人工门。

## 阶段三最终组合与冻结

负责人已实际读取RED/GREEN原始证据和双源差异，打开实际布局，并复看素材后批准8PNG安装与唯一组合。此次仅运行 `cmd /d /c gradlew.bat assemble --console=plain`，同一美术cwd及Java21；自然exit0、3秒，compileJava UP-TO-DATE，processResources/jar增量。没有重测、重试构建、clean/full/GameTest。命令/环境/原始日志及退出码见 [final-assemble.py](../../../build/reports/art/ART-REACTOR-09/consumer/final-assemble.py) 与 [原日志](../../../build/reports/art/ART-REACTOR-09/consumer/final-assemble/gradle.log)。

最终HEAD `2ae83b9c9d355c5229f973849e04c449dea0ceb5`。冻结候选 [final-ART-REACTOR-09.jar](../../../build/reports/art/ART-REACTOR-09/consumer/final-ART-REACTOR-09.jar)：**2624615 bytes / SHA256 `737d3bd5606109f711998da92714917de19b6ed891f0abb7fbb27ca3f1049050`**。

[final-binding.json](../../../build/reports/art/ART-REACTOR-09/consumer/final-binding.json)核对两源仍与GREEN同SHA、两compiled class/build/ZIP一致；68SVG来源关联8generated→formal→processResources→JAR逐字一致，安装历史八旧PNG备份保持。素材68SVG/8PNG导出/check/安装证据直接复用ASSETS报告，未重导。34保护再次与本阶段基线一致，生产非几何/四侧/fromSide原字节证据复用。首次制品绑定误用旧汽轮机包路径、第二次宽泛前缀多含原转子类，各自AssertionError保存在独立final-binding-first/second-error.txt；仅修证据筛选为旧R2精确五class，没有修改生产/资源或再构建。

原08 JAR/a4b08…3c7先完整备份在consumer/baseline/ART-REACTOR-08.jar；08与07B-R2历史冻结不写。只读比较原08包内07B-R2五class/稀疏PNG及08 CT Behaviour class/铅灰窗atlas共8条，最终包逐字保持。这些是先前输入，不加入净09源清单，不重审旧矩阵。

[final-net-art-sources.json](../../../build/reports/art/ART-REACTOR-09/consumer/final-net-art-sources.json)只列本批双源、八正式PNG、新reactor-device-art-09工具目录与ASSETS/CONSUMER两稳定报告；不夹带08/07B-R2dirty或可变玩法。主PM协调文档作为只读依赖；负责人任务卡/PLAN/README/CANDIDATE及待续审查仅列交接路径，不作为生产JAR输入或不可变SHA集。

[final-freeze.json](../../../build/reports/art/ART-REACTOR-09/consumer/final-freeze.json)冻结净艺术输入、本批consumer证据/候选及作者资产原始证据、共享只读补充。旧stage-identity不覆写，其阶段二报告原字节保存为final-stage-two-report.md，SHA与旧阶段身份一致。停止修改交唯一组合规格/质量窄审；原真实Font/GPU、密集顶盖辨识与玩家长按待用户现有存档复看，未称视觉或业务人工门关闭。
