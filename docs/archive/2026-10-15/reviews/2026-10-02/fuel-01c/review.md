# EXT-A-FUEL-01C 限定独立审查

- 候选基线：de2e0d150193cfce0664312ab4f80158c20923f1；按主工程 01C 卡限定审查离心机运行代码、两份模型、定向测试和交付证据。锁定 MC 1.21.1 / NeoForge 21.1.219 / Create 6.0.10-280。
- 技能：应用 minecraft-modding、minecraft-testing；Create/NeoForge调用按锁定源码核对。本轮未运行构建、测试或游戏。

## 结论

发现两项机面交互问题需定点处理后再进入客户端验收；其余所审能力方向、过滤和保存规则符合本卡。现有 7/7 定向测试及 assemble 可复用，但没有覆盖真实方块放置、槽位遮挡、Create 流体端点过滤或原生槽交互。

## 发现

- **P1C-01，P2：普通右键放置 Create 方块物品被消费。** CentrifugeBlock.java:137 对所有非空、非过滤槽、非扳手/轴承/桶的物品统一返回 sidedSuccess。Create 管道、黄铜漏斗等 BlockItem 右键安装到离心机面时，方块交互已消费动作，Minecraft 不会继续调用方块物品放置；Shift 绕过可作为临时规避，但五面原生物流连接不能按普通右键方式安装。应让需要原生放置的 BlockItem 分支返回 PASS_TO_DEFAULT_BLOCK_INTERACTION，同时保留过滤槽、桶、维修和扳手优先处理。该项阻断候选直接进入客户端验收，应由原执行者定点修复。
- **P1C-02，P2：过滤槽置于管道接面中央，管接上后难以点选。** CentrifugeBlockEntity.java:42 使用 CenteredSideValueBoxTransform；锁定 Create 的该变换点位为 face 中心 [8,8,15.5]。同版本 FluidPipeBlock 管半径为 4/16，中心接管截面占 4..12 像素，覆盖过滤框中心命中区。Create 自己的 SmartFluidPipeBlockEntity.SmartPipeFilterSlot 将槽放到管轴边缘（wall: y=11.4,z=0.55；floor/ceiling也偏轴），与上述遮挡风险相符。建议改用原生 ValueBoxTransform.Sided 将五面过滤框放到边缘，避开接管截面；不需要新样式。此项需与放置分支一并定点复查。

## 已核对无发现的范围与边界

- **过滤槽、空手与桶优先级：** Create 6.0.10 的 ValueSettingsInputHandler 在方块右键前命中 SidedFilteringBehaviour 槽位，取消事件并执行 onShortInteract；空手可清槽，桶可作为过滤物，不落入离心机取料/桶交易。槽外空手由 useWithoutItem 及空栈 useItemOn 桥接取料；非过滤面持桶即便失败也消费交互，避免落入世界倾倒分支。
- **能力规则/旧句柄：** 五面返回共用库存视图、底面不提供能力；物品仅可提取两粉、插入始终拒绝；流体只灌料浆，可排水或抽回剩余料浆；显式 FluidStack 请求只取对应流体。每次操作检查当前面和当前过滤，cached handler 不缓存筛选结果；拆除、替换和底面限制由当前 BE 检查拒绝。
- **Create 管网刷新：** 未发现机器过滤需要调用 FluidPropagator.propagateChangedPipe。SmartFluidPipe 的过滤会改变自己的 FluidTransportBehaviour.canPullFluidFrom 和泵传播网络，因此它使用该回调；离心机过滤只动态影响端点 handler。Create FlowSource.FluidHandler虽缓存 BlockCapabilityCache，但保留的本机 handler 按当前面实时读 SidedFilteringBehaviour；PipeConnection.manageFlows每tick重问流源，拒绝或流体改变时清旧 flow，后续按新条件启动；FluidNetwork在传输时逐次调用目标 fill。因此没有证据要求过滤变化重建网络拓扑。
- **定向测试边界：** 运行报告记录 7/7、assemble成功及模型JAR核对。JUnit没有 Level；锁定 Create FilterItemStack 对非空 FluidStack filter 会调用 GenericItemEmptying，空 Level 夹具NPE是合理环境边界，不据此认定生产代码有错。由此也不能声称自动覆盖真实桶过滤/管网，需要客户端验收。
- **显示与持久化：** 两份模型与资源报告一致。forFluids仅改变 FilteringBehaviour 的显示标签，不改变 item/FluidStack 两种 test；由于粉末也使用同一面槽，标签可能显示为“流体过滤”而略有误导，属于低优先级文字问题，不作为阻断项。Create行为参与世界BE保存，单件便携快照保存 CentrifugeFilters；恢复时先清空再读取，旧快照缺字段则为空过滤。世界存档、拆放和真实渲染没有本轮运行证据。

## 尚待人工确认

完成两项定点修复后，客户端仍需确认五面槽显示/桶粉过滤及清空、真实流体管网随换过滤器的拒绝与恢复、管道/漏斗安装及方向、空手取粉/持桶/维修优先级、过滤拆放保存和动态桶/机身观感。本轮未启动游戏。

## 复审：P1C-01 / P1C-02 定点整改

- **P1C-01 已关闭（静态交互链核对）。** `CentrifugeBlock.java:110-112` 扳手让出默认方块交互；`138-140` 普通 `BlockItem` 同样返回 `PASS_TO_DEFAULT_BLOCK_INTERACTION`。关键补丁在 `145-153`：经过该默认交互继续调用 `useWithoutItem` 时，只要主手非空便返回 `PASS`，不再执行取粉，因此后续 `BlockItem.useOn` 可尝试放置管道/漏斗，扳手的 `Item.useOn` 也可继续；空手才保留过滤槽外的手取粉。过滤槽、桶和维修仍由前置分支优先处理。此结论是按锁定 Minecraft 1.21.1 原生三段交互（方块 `useItemOn` → 默认 `useWithoutItem` → 物品 `useOn`）及当前分支返回值作静态核对，未在客户端实点安装。
- **P1C-02 已关闭（几何断言与包内容核对）。** `CentrifugeFilterSlotTransform.java:9-18` 将槽放到各允许面边角，排除 DOWN。新增 `CentrifugePortsTest.nativeFilterSlotsStayAtFaceCornersOutsidePipeCrossSection` 针对五方向变换位置验证 Create 值框命中半径与管道中央 `4..12/16` 截面分离，并确认底面不显示；定向测试最终 4/4、无失败（`runtime/review-fixes/targeted-test-final.log`及 JUnit XML）。未做真实模型光线追踪或连接管后的客户端点击。
- 增量打包证据 `runtime/review-fixes/assemble.log` 为 `BUILD SUCCESSFUL`。只读检查最终 JAR 确认含 `CentrifugeFilterSlotTransform.class`、两份离心机/桶模型；本轮没有重跑任何构建或测试。

**复审结论：** 两项代码级阻断已关闭，可进入本卡客户端复测。实际管道/黄铜漏斗右键安装、扳手操作、五面槽接管后的可点选性、过滤后流体网络行为及动态观感仍须人工确认；本轮静态结果不替代该门。
