# ART-BOILER-06 / 06R1：汽段雾效与内部可见度整改候选

**现行状态：2026-10-11。** 用户在主对话确认锅炉美术重置通过验收，本批与05B整体视觉门关闭，最终06R1及同源蒸汽动画已合入main `81a161e`。从主目录使用更新后的JAR；来源、复用证据及制品身份见[验收与集成](../reviews/2026-10-11/boiler-art-integration/ACCEPTANCE.md)。以下待验/美术目录启动说明是原候选历史，不补记未报告的逐项结果，不重复要求手测。

2026-10-11用户决定“锅炉的美术就先这样了”，当前版本暂定并停止继续细化，下一项为汽轮机方案构思。下列候选与原始证据保留；此决定不补写用户未具体报告的逐项游戏观察结果。

用户已批准普通蒸汽乳白灰、超临界蒸汽青白，以及按实际库存控制浓度和连续混色的方案。用户随后明确反馈“锅炉内部蒸汽太淡”，当前候选为06R1内部可见度整改：2/2受影响既有检查、一次增量assemble及一次独立差异窄审通过，必改项无；原06的11/11及完整审查证据复用并保留。用户反馈属于整改触发，真实客户端可见度仍待复看，尚未合入main。

实施和启动目录为 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，当前消费基线 `b8cb60a`。PM的实际容量只读入口经`a3540b2`净同步，合同与实物见[HANDOFF](../reviews/2026-10-11/boiler-steam-capacity-view-01/HANDOFF.md)；美术写集和批准设计见[06任务卡](tasks/ART-BOILER-06.md)，内部整改见[06R1任务卡](tasks/ART-BOILER-06R1.md)。本批没有运行用户游戏或改动存档。

## 当前表现

- 唯一控制器渲染器在绝对隔层上方的真实汽区绘制缓慢翻卷、向上流动的半透明雾；网格固定14片，自然适应当前可靠尺寸，不逐尺寸列模型。
- 总真实蒸汽mB/服务器实际汽容量控制浓度，超临界mB/总mB控制颜色。气体覆盖整个汽区，没有液位；零库存无雾，停热但尚有库存时保留雾。
- 06R1只提高内部汽雾alpha曲线，满库存外/内片不透明度由.126/.063提高为.476/.238，约3.8倍；零/非法库存仍无雾，浓度保持单调。网格、混色、实际气室采光与40个原正式资源保持。
- 两种蒸汽的still/flow各8帧，四个流体sprite与内部32帧共用原创整数SVG雾纹。普通流体atlas为固定16tick循环；内部为80显示tick循环并叠加缓慢上行UV，材质动画不表示真实流向/流量。
- 当前世界、控制器对象、L1和descriptor身份一致才绘制；按真实汽区裁剪、采内部空气光，暂停冻结动画，卸载/换世界/资源重载安全撤销。原窗口水位渲染保持。

## 当前06R1自动证据与制品

负责人已完整读取[整改消费报告](reports/ART-BOILER-06R1-CONSUMER.md)、[独立差异窄审](reports/ART-BOILER-06R1-REVIEW.md)及原始日志/退出码/两份XML，并逐行核两源与原06冻结差异。唯一命令为 `./gradlew.bat test --tests '*BoilerSteamVisualStateTest.realCapacityAndBothStocksDetermineConcentrationWithoutLiquidLevel' --tests '*BoilerSteamRendererTest.actualSubmissionKeepsLocalPoseAirLightAlphaAndSlowUpwardUv' assemble --console=plain`，17秒exit0、2/2、零失败/错误/跳过。没有新增镜像测试或重复原矩阵、素材生成/安装。

[当前冻结JAR](/E:\MyMC\NewMod\Create_NuclearIndustry-art-studio\build\reports\art\ART-BOILER-06R1\consumer\frozen\create_nuclear_industry-0.1.0.jar)为2,587,696字节，SHA256 `dae3321befc092903905ba42234eda769bf53cad3e5d1bc9f3ee22f64d73b245`，负责人核其与当前`build/libs`一致。[R1清单](/E:\MyMC\NewMod\Create_NuclearIndustry-art-studio\build\reports\art\ART-BOILER-06R1\consumer\candidate-manifest.json)SHA256 `ea3f40a336fbbbb4e7f202917e3b590c5c9a633f4c58c3703e5e357a9fc53119`，绑定两源、9个相关class和新JAR，并链接原06清单复用其余依赖。独立审查核9class及54个未变JAR依赖，报告SHA256 `7b2d6e661629d777ac4bc8fa7501e673da2e346f01518133d060e7785a7dc1b7`；未扩大为全仓保护矩阵。

## 原06自动证据与制品（历史，继续复用）

负责人已读[素材报告](reports/ART-BOILER-06-ASSETS.md)、[消费报告](reports/ART-BOILER-06-CONSUMER.md)及原始日志/XML。32SVG来源检查和40资源安装一致exit0复用原证据；白雾透明插值的一项真实行为RED保留，按alpha加权RGB修复后并入最终回归。

最终唯一命令 `./gradlew.bat test --tests '*BoilerSteamVisualStateTest' --tests '*BoilerSteamRendererTest' assemble --console=plain`，19秒exit0；4项状态测试＋7项渲染测试，共11/11，零失败/错误/跳过。不重复L1、反应堆或05B检查。

[原冻结JAR](/E:\MyMC\NewMod\Create_NuclearIndustry-art-studio\build\reports\art\ART-BOILER-06\consumer\frozen\create_nuclear_industry-0.1.0.jar)为2,587,701字节，SHA256 `b5764e6f2630542975223c4b2a2bff16de0b3f663d1868a95903ce4fbad63ee5`，完整保留为历史，当前`build/libs`已是上述R1制品。[原清单](/E:\MyMC\NewMod\Create_NuclearIndustry-art-studio\build\reports\art\ART-BOILER-06\consumer\candidate-manifest.json)绑定原7源、21专属编译class、40资源及共享Controller一源/两class；SHA256 `be680503a9f8e72c5240f0fa1ef19e920adbb77ebee5bf11412cc67c394a4bae`。共享一源仍为PM实物SHA256 `843abb4ec1a3fbe5517db4f248cf5505582e90b9f5baad1d5b5078f15e48c796`。

原06交付时负责人一次实核7源、共享一源、清单及当时live/frozen JAR身份一致；class/资源的JAR绑定由[原同轮独立审查](reports/ART-BOILER-06-REVIEW.md)核实通过，报告SHA256 `1dcbe0e6d2d20067c9c54404b2fadf7c41986fc2b5927351ca484887cf512e9b`。原完整证据保持，不重跑；05B冻结JAR与视觉门保留，R1当前制品仍包含既有05B候选。

## 预览与客户端复看

[同源蒸汽循环预览](http://127.0.0.1:8735/tools/art-assets/boiler-steam-06/preview/index.html)可比较四组循环和颜色比例；它使用单层材质示意透明度，不代表实际网格可见度。[生产CPU采样](/E:\MyMC\NewMod\Create_NuclearIndustry-art-studio\build\reports\art\ART-BOILER-06\consumer\production-offline-sample.svg)来自实际Mesh/Frames/alpha，负责人已实际浏览低22%与高82%浓度、48%超临界混色及Y45..50汽域。中性背景、假设材质光为1，没有真实玻璃、气室采光或GPU排序，不能作客户端验收。

[06/R1同条件CPU对比](/E:\MyMC\NewMod\Create_NuclearIndustry-art-studio\build\reports\art\ART-BOILER-06R1\consumer\production-offline-comparison.svg)只改变生产alpha，其余48%超临界比例、22%/82%库存、帧/遮罩/权重/汽域和中性光相同。负责人已实际浏览并保存[浏览器复看截图](/E:\MyMC\NewMod\Create_NuclearIndustry-art-studio\build\reports\art\ART-BOILER-06R1\consumer\production-offline-browser-crop.jpg)，支持浓度提升，不代表真实透窗或夜间视觉通过。

06R1修改了Java渲染代码，需要关闭并从上述美术目录重新运行 `./gradlew.bat runClient`，F3+T不能加载代码修改。进入已经搬来的现有存档，无需重建设备，本轮集中复看隔层上方观察窗的雾是否足够明显，尤其低库存、斜看及实际气室较暗时；对照护目镜确认浓度仍随库存变化。原06其他观察项目继续保留，不因R1重复要求已确认结果：

1. 从隔层上方观察窗看汽区：有库存时雾纹缓慢翻卷/上升，隔层以下仍为原水区；零蒸汽时无雾。近看、斜看以及离控制器较远的窗口都应显示。
2. 对照护目镜内两种真实库存：浓度随总库存增加，普通为乳白灰、超临界为青白，混合时连续过渡；停止供热而留有蒸汽时雾保留。
3. 在储罐、透明管和桶中比较两种蒸汽：新的静止/流动云纹颜色清晰、循环连续；F3+T之后资源正常恢复。
4. 单人真暂停时雾停止，恢复后继续；正常离开并重进世界，或拆坏/修复结构后，显示分别撤销/恢复。实际气室采光、透明玻璃排序与FAST/FANCY/FABULOUS可见度以本次游戏观察为准。

本批游戏结果只用于锅炉06；05B连接外观及其他设备历史验收独立保持。Git提交与main集成交主PM或用户，等待本批视觉结果时不自动推进其他设备。
