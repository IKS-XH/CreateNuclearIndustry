# ART-REACTOR-08：双面窗口CT消费报告

阶段二客户端交付完成记录（历史）：仅修改ReactorConnectedTextureBehaviour与既有ReactorConnectedTextureTest；没有安装资源或assemble，07B-R2 JAR仍为SHA256 `b1c3c69c29dce947b99ccd0f0a187e9689958217a2f3727c80487d9033cd8d88`。HEAD6042fd2598865d42de88f95c2ba8110c3b7e1196；MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6保持。实际读取活动卡、AGENTS、美术README/PLAN、治理1.2/5.1及minecraft-modding/testing/resource-pack；按systematic-debugging追踪实际调用，TDD先真实断言失败再补实现。

双面根因：ReactorSurfaceSnapshot.findSurface仅返回真实outwardFaces；Create CTModel.createCTData收到getDataType=null便跳过面（锁定字节码129/136–139）。Behaviour现在真实外面优先，仅实际window的内面允许选择反向的可靠外面Member。getDataType与六参connectsTo共用该选择，再以真实外平面运行原localID/pos/bounds/dimension/owner/generation/revision及严格邻接检查。Create.buildContext仍收到实际渲染face（字节码158），没有复制外面索引或自写镜像。实体壳面、canConnect及sharedWindowFaces原代码文本保持；L1与ScopedCTModel没有修改。

## 原始检查

实际命令均由consumer/run-gradle.py保存cwd、命令及退出码；cwd固定美术树，Java21，增量无clean/build/rerun。两次均执行`gradlew.bat test --tests '*ReactorConnectedTextureTest' --console=plain`。

- [red日志](../../../build/reports/art/ART-REACTOR-08/consumer/red/gradle.log)：21项（原17＋新增4），3个AssertionFailedError，errors/skip0，exit1、29秒。六向内面类型、内面缺角上下文、边角内面连接真实失败；不是编译或夹具失败。
- [green日志](../../../build/reports/art/ART-REACTOR-08/consumer/green/gradle.log)：21/21、failure/error/skip0，exit0、22秒。原始JUnit XML及exit.json分别保存，不覆盖red。
- 新回归实际调用生产Behaviour与锁定Create.buildContext/getTextureIndex：六向内外完全连接索引54；SOUTH内面缺右上53、仅上右缺角17、上右有角20、独立对角0；不同owner/generation/revision/bounds、本地材质替换、缺Member及原壳内面退路；真实边角保留多个外面优先规则。

[锁定Create调用证据](../../../build/reports/art/ART-REACTOR-08/consumer/locked-create-ctmodel.javap.txt)保存真实javap命令/exit0与字节码。实际输出供素材只读复用：[六向内外上下文](../../../build/reports/art/ART-REACTOR-08/consumer/native-inner-outer-contexts.json)、[47原生索引](../../../build/reports/art/ART-REACTOR-08/consumer/native-window-contexts.json)、[原生窗簇/共享面场景](../../../build/reports/art/ART-REACTOR-08/consumer/native-window-scenes.json)。后两项仅将旧测试输出路径重定向本批，旧mapping输入/断言未改，02R1旧证据不覆盖。这些是生产消费及原生上下文证据，不是客户端/GPU视觉验收。

## 保护及共享缺口

[scope.json](../../../build/reports/art/ART-REACTOR-08/consumer/scope.json)绑定两源SHA、21受保护路径及零差异：其余15个Reactor客户端源、共享Instrument/FluidHandler、4 Descriptor/Factory保持。canConnect/sharedWindowFaces文本与基线一致，两个手写源无尾空格。JUnit自动写logs/debug.log、logs/latest.log保留，未清理或手修日志。主/美术树阶段一核16相关client源，3份仅换行字节差、规范化文本一致；4 Descriptor/Factory及Instrument完全一致。

液面闪断属于共享撤销：FluidHandler EXECUTE fill/drain的152/180行→Instrument.setSnapshot:122→invalidateRuntimeDescriptor:228–236发Available=false（Revision++、Sample不变、零库存及空Columns）→L2.active=false→InternalRenderer:32找不到owner整段return；下一成功结算573–589恢复完整新样本并立即发送。只读检查没有将整片消失归因于库存层切换、UV或透明面提交顺序。共享修复由主PM协调，本执行者没有续租旧撤销信封、拼装假同龄数值或修改Liquid Renderer/VisualState/Materials/RenderBuffers/共享State。

本consumer冻结入口为[freeze.json](../../../build/reports/art/ART-REACTOR-08/consumer/freeze.json)及freeze.sha256，仅包含本执行者两源、报告、独立consumer证据与只读保护绑定，不含作者assets或负责人活动卡/PLAN。整体资源/共享修复、唯一assemble及组合窄审仍待负责人后续授权；真实双面透窗、玻璃透明排序和液面视觉留给用户复看。未启动游戏/服务，未操作Git、主树、存档、资源或依赖。

## 阶段三最终组合绑定

阶段三已按生效卡唯一运行`gradlew.bat assemble --console=plain`，cwd固定美术树，exit0、7秒；[最终原日志](../../../build/reports/art/ART-REACTOR-08/consumer/final-assemble/gradle.log)、exit.json与执行脚本保存。没有test/build/clean/rerun，没有改生产、测试或素材。自身两源与21/21 GREEN身份一致；复用共享HANDOFF的1/1真实及17/17定向证据。

最终HEAD `f787aa40a323d8fdd09a0f95e5798a9a2c8e3818`。唯一冻制品[final-ART-REACTOR-08.jar](../../../build/reports/art/ART-REACTOR-08/consumer/final-ART-REACTOR-08.jar)：2624312 bytes，SHA256 `a4b08dd0a5bebe72555dc44969553ede120080ef66ff2ab38d27974648d353c7`。本批基线备份07B-R2 JAR2616697 bytes、SHA b1c3c69c29dce947b99ccd0f0a187e9689958217a2f3727c80487d9033cd8d88；原R2候选保持，不覆盖。

新窗口atlas generated/安装/build/ZIP一致，1458 bytes、SHA `f18296427db4c106579f778e3b0443284d5fd074ebf997889d13c495f1874723`，CT Behaviour class/build/ZIP一致。共享5个class及新旧109字节NBT均与本树build/ZIP及主PM final-identity逐字一致；实际javap服务端公开入口`public void setCoolantInventories(long, long);`见final-server-api.javap.txt。主PM专用入口保持最后完成的可靠同龄信封，不增加sample/revision/time或续租；下一完整结算更新，本客户端仍立即尊重真实撤销。

预检首先发现共享三Java raw SHA与PM不同，随后只读逐行确认纯Git CRLF同步差异、LF规范化文本完全相同；新NBT逐字相同。没有改共享源或强行改行尾。本树/PM原始SHA、大小和规范化一致性分别登记final-preflight.json，主PM原final-identity及HANDOFF私有副本保留。原scope/freeze的Instrument/FluidHandler旧身份作为历史，最终仅这两个受保护源允许主PM授权改变，其余19路径保持。

07B-R2的5源、5class、稀疏PNG继续保持原SHA；原JAR全部1113个assets条目比较仅当前观察窗CT atlas改变，其他旧反应堆及汽轮机素材没有夹带。此比较为同轮打包字节绑定，不重跑旧source矩阵。所有绑定见[final-binding.json](../../../build/reports/art/ART-REACTOR-08/consumer/final-binding.json)。

[final-net-art-sources.json](../../../build/reports/art/ART-REACTOR-08/consumer/final-net-art-sources.json)仅列08艺术净写集：Behaviour、专属Test、唯一atlas、新reactor-window-art-08目录及ASSETS/CONSUMER两报告；没有其他dirty/turbine源。共享4源/资源另列为主PM输入，活动卡/PLAN/CANDIDATE/README和待续review报告只列交接路径，不列不可变文件SHA。最终冻结入口final-freeze.json/final-freeze.sha256，不覆盖阶段二18项freeze/red/green或旧共享证据。

最终停写，交原同一次review补最终绑定，整体用户双面透窗/铅灰玻璃/真实库存液面视觉门仍待复看。未启动客户端/服务或操作Git、主目录、存档；本树没有可变反应堆玩法。
