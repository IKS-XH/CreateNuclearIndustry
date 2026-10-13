# ART-REACTOR-02-L1 可消费接口交付

2026-10-09，主逻辑PM。**L1自动与独立审查门通过，净显示接口已合main并同步美术工作树；02B可在既定美术授权及下列写集内接续。** 这不关闭ART01/02最终视觉门，不推进反应堆可变尺寸玩法，也不关闭换热器五幕播放门。

## 实物与基线

- 实现源提交`c5f29f02e30e2d1f7244c0619d0d53c35a9ca8b3`，工作树`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-display`。
- main净整合`c5e5fc3`；美术树`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`净同步`8b83a1a`。同步前核对15个提交路径与美术所有已跟踪/未跟踪修改无交叠；没有暂存、清理或覆盖ART01、02A资产及负责人文档。
- [实施报告](./IMPLEMENTATION.md)、[独立审查](./REVIEW.md)随代码同步；PM维护的[接口合同](../../../art/ART-REACTOR-02-INTERFACE.md)在主树、美术树与显示树一致。原始日志/API源码/字节码/XML留在显示树`build/reports/art/ART-REACTOR-02-L1/`。
- 候选JAR为显示树`build/libs/create_nuclear_industry-0.1.0.jar`，2352501字节，SHA256 `67644908B604A11488A35A3EDEF68EF01985A0DDFF3F4998DF48A31936D5923B`。主树与美术树未为同一净代码重复构建；不能将它们的旧JAR当本次制品。

## 编译消费合同

包`com.iksxh.create_nuclear_industry.structure.client`；实际javap签名与源码一致。

| 入口 | 含义 |
| :--- | :--- |
| `ReactorSurfaceSnapshots.capture(BlockAndTintGetter context)` → `ReactorSurfaceSnapshot` | 模型构建必用；只接受当前真实ClientLevel或原生精确类型RenderChunkRegion的所属世界，未知包装/Ponder/旧世界返回空 |
| `ReactorSurfaceSnapshots.capture()` → `ReactorSurfaceSnapshot` | 仅诊断；不能作为模型任务世界会话证据 |
| `ReactorSurfaceSnapshot.empty()` → `ReactorSurfaceSnapshot` | 无数据降级，不使用null |
| `snapshot.findSurface(BlockPos pos, Direction face)` → `Optional<ReactorSurfaceSnapshot.Member>` | 仅查询真实朝外面，内部/未知/歧义/未加载为空 |

Member实际访问器：`String dimension()`、`BlockPos ownerPos()`、`UUID ownerGeneration()`、`long revision()`、`BlockPos origin()`、`BlockPos maxInclusive()`、`String expectedBlockId()`、`BlockPos pos()`、`Set<Direction> outwardFaces()`。bounds包含端点，各轴长度独立，世界坐标单位为方块；位置及集合不可变，快照无Level/BE/NBT引用。当前5×5×5只在服务端投影适配层使用，消费不得写死5。

一次`gatherModelData`只捕获一次带上下文快照，整个super调用范围复用；ThreadLocal作用域须嵌套恢复并在finally清理，禁止邻接查询中重捕获。Create6.0.10-280的`getModelData`为final，不能覆写；美术可按已核对的protected `gatherModelData`接入。

连接必须同时检查：相同dimension/ownerPos/ownerGeneration，同一朝外方向和实际外平面，bounds和材质表允许，当前局部BlockAndTintGetter中的方块注册ID仍匹配双方expectedBlockId。revision用于一致构建和更新水位，不替代结构身份。只相邻同种方块不足以连接。无可靠描述时`getDataType`返回null，保留原quad；物品/未成型/Ponder保持单块显示。

**必需依赖：** 保留`src/main/resources/META-INF/accesstransformer.cfg`，它仅公开`RenderChunkRegion.level`且仍为final；现有ModDevGradle已自动发现，不改build.gradle。不得改成反射或light-engine身份判断，后者被Ponder委托。未知第三方渲染包装当前安全降级单块，本次没有擅自扩展兼容范围。

服务端schema 1及传输安全预算、卸载/暂停/新generation/revision保护详见实施报告。客户端区块真实加载证据、会话清空和发布后有限区段刷新由L1维护；02B不新增同步/扫描/会话状态，也不重复该生命周期测试矩阵。

## 02B准许写集与验证

- 仅新建`structure/client/ReactorConnectedTextureBehaviour.java`、`ReactorConnectedTextures.java`、`ReactorConnectedTextureClientEvents.java`，消费已交付接口、独立客户端注册并包装原模型。
- 14张游戏图集为`assets/create_nuclear_industry/textures/block/reactor_ct/<原sprite名>.png`，名单/原sprite见接口合同；源图集工具可保留`generated/<原sprite名>_ct.png`，目标sprite无`_ct`。不修改原JSON、注册、语言、碰撞、Ponder或玩法代码。
- 准许专属`src/test/java/com/iksxh/create_nuclear_industry/structure/client/ReactorConnectedTextureTest.java`，只测消费侧空/可靠上下文、跨owner/局部ID、六面连接及ThreadLocal嵌套/异常恢复，不改L1公共API或测试，不重复02A资产矩阵。
- 美术负责人按本边界维护02B卡与派发；执行者无Git写权限。只做本批消费相关定向测试与一次增量打包，真实客户端候选另验收成型/拆坏、相邻独立结构、各面拐角/功能孔、跨区块/视距恢复、退出重进/世界切换及Ponder降级。当前版本正常加载可验证，不做旧存档迁移。

## PM证据与保留项

PM直接核对最终日志exit0、六份XML共26 tests且0 failure/error/skip、唯一隔离服1/1通过并11:01:43正常退出、实物签名与JAR哈希；独立审查同时比对锁定源/字节码与真实事件次序，无阻断项。暂存的15个任务文件通过`git diff --cached --check`，日志和PM同步文档未夹带入代码提交。实施后未重跑全量、已通过玩法或旧存档测试。

main原有logs与样例目录、显示树自动生成logs、全部美术未提交候选保持。通过共享文档交付，没有未经用户授权向另一聊天发送回信。
