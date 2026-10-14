# ART-BOILER-06-CAPACITY-01 实物交接

2026-10-11，主逻辑PM。**单字段只读入口已编译、独立窄审并净合main/美术树，美术06可以接通实际容量消费。** 此交接仅完成必要共享前置；05B连接外观及06汽雾/流体动画仍由美术负责人完成候选和用户视觉验收。

## 实际接口与来源

`com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity` 新增实际公开签名 `public int syncedSteamCapacityMb()`，单位mB；编译字节码为 `aload_0 → getfield steamCapacityView:I → ireturn`。纯返回原有同步字段，不重算客户端默认容量，不新增字段、网络、首包标志或改变读取顺序、账本、温压、玩法。

- 实施基线 `3eb88c9e61214fc812411339182334512509e3ac`，逻辑树净提交 `c23a67c17737a1f55c4491a59a9344df8081390d`。
- main净提交 `eb196c6aed743aff0607ae0be95a3de1cb8ed840`；美术树净同步 `a3540b25ac79c5d48fb1a1064ffecb2aa201da17`。仅一份控制器源码及[实施](IMPLEMENTATION.md)/[独立审查](REVIEW.md)两份报告，三个路径逐字复制一致。
- 控制器源SHA256 `843abb4ec1a3fbe5517db4f248cf5505582e90b9f5baad1d5b5078f15e48c796`。唯一增量 `jar` 3秒、exit0，两个控制器class与JAR匹配；JAR SHA256 `9071b937ad82970186fad6bfc5e3aa8d2efda3702af11956c7307599136b9f58`。原四项NeoForge弃用警告保留，没有重复原测试/探针、全量构建或启动用户世界。
- 原始日志、实际ABI/字节码与一源/两class/JAR的manifest在 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/build/reports/art/ART-BOILER-06-CAPACITY-01/`。编译前原L1的控制器源、两个class及JAR共四件保持于该目录 `previous-l1/`，旧manifest继续记录原时点，不改写历史证据。
- main `build/libs/create_nuclear_industry-0.1.0.jar` 已替换为上述冻结文件，原L1 JAR `f95f60040b6e82aff3a907cd3f7f447c2e1f4f5505ba203b7590d7d671632e93` 完整备份于main `build/reports/art/ART-BOILER-06-CAPACITY-01/pre-integration/`。未覆盖美术树JAR或混入05B/06候选素材。

## 消费边界

getter自身不保证成型或已收服务器数据，原初始化值18000不能当同步证明，原包缺失/非数值读取为0。原read发布L1通知早于容量赋值，因此必须在**整次原clientPacket读取完成之后**消费；不能在通知回调中把旧容量与新几何直接组合。

按[已批准消费合同](../../../art/ART-BOILER-06-CAPACITY-INTERFACE.md)，核当前真实owner对象、同一世界、有效surfaceDescriptor，以及当前L1同generation/revision/min/max/partition；随后一次采两个真实库存和同步容量。容量非正、未知/撤销/归属冲突则无雾，不作分母。总库存用宽类型求和并有限限幅，按总库存/容量定浓度、超临界库存/总量定连续混色；真实汽区始终覆盖，不作液位。服务端容量权威仍为原ledger/config，显示getter不得参与模拟。

## 美术06准许接通

美术负责人据用户既有专项任命维护自己的06卡、派发实现及视觉门；不授予子Agent负责人或Git权限。本批接口已实际同步，可在其已批准独立写集内接通：

- 生产仅 `boiler/client/BoilerSteamRenderer.java`、`BoilerSteamVisualState.java`、`BoilerSteamMaterials.java`、`BoilerSteamRenderBuffers.java`、`BoilerSteamClientEvents.java`；测试仅同包 `BoilerSteamVisualStateTest.java`、`BoilerSteamRendererTest.java`。现有Controller、L1、BoilerClientEvents、Window Fill/BER、反应堆缓冲、服务端及构建只读。
- 独立CLIENT事件只登记现有 `BoilerContent.CONTROLLER_BE` 的BER。Renderer自行负责可靠汽区AABB、offscreen及有限距离裁剪，不修改共享BE包围盒；owner局部坐标、真实内部空气采光、单炉有界网格，不每窗重复整域或客户端扫描结构。
- 延续其卡的同源SVG材质、库存连续混色、真暂停时钟及安全世界/卸载/reload边界；汽雾批次在全部BER之后、透明窗之前统一结束，既有反应堆批次只读。素材安装/预览属于美术负责人写集，不由本前置代替其验收。
- 消费候选稳定后按美术06卡仅一次必要定向检查与最终增量assemble、同轮独立审查，绑定本实际getter源与最终消费资源/JAR。复用原L1 29/29、1/1和原审查，05B已有证据独立保持；真实透窗、GPU和管/罐流体动画仍交用户复看。

main/逻辑日志、缓存及美术05B/06素材、独立计划和报告均保留。没有新建分支或重新搬动/运行存档；已携带的27文件世界与原备份保持。通过本共享文档完成交接，其他工程主线仍暂缓。
