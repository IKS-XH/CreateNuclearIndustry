# EXT-A-FUEL-01E 独立审查记录

## 本轮范围

本轮只审查已冻结的资源线：四个模型 JSON、`tools/art-assets/centrifuge_01d_preview.py`、资源交付报告及其修前/修后证据。未运行测试、Gradle、生成器或游戏客户端；没有审查或判定尚在配置中的运行线。

## 审查结论：资源静态合同通过

- 对候选工作树的实际 JSON 逐面读取后，`enrichment_centrifuge` 为 57 个元素/92 个面，`enrichment_centrifuge_upper` 为 59/114，完整物品模型 `enrichment_centrifuge_item` 为 139/344，转子 `enrichment_centrifuge_rotor` 为 23/138。四份模型所有实际存在的面都显式提供 UV，坐标均在 0..16 范围内；没有隐式 UV 或越界值。
- 逐面比对完整物品模型的几何序列，底段 57 个元素与下段模型、随后 59 个元素与上段模型、最后 23 个元素与转子模型的 UV 都逐面一致。特别是物品中的上段保留了上段局部 UV，没有因整体上移 16 格而重新按全局 Y 生成越界 UV。物品入口 JSON 指向完整双格模型。
- 模型纹理表所引用的四张离心机 PNG 均存在。候选工作树状态显示这轮模型/脚本变更没有触及 PNG；资源报告记录了四张既有纹理的哈希。
- 壳体接缝不是由空集合通过：我从两个接缝对应的实际 JSON 中分别筛得 8 片外侧板；每片都有单一水平外侧面，旋转面也有显式旋转数据。现有检查器对从磁盘读取的元素应用旋转后校验外法线，再对外平面做 4096 方位射线覆盖；它强制要求恰好 8 片，并在修前/修后分别记录 96/0 条未覆盖射线。修后相邻法线最小夹角为 45°、最小搭接余量为 0.016468、无 `cullface`。角度与顶点旋转公式和这批 `-45°/+45°` 的模型定义相符，计数与输出 JSON 一致。
- 中央观察口没有被一张连续壳面封住。实际几何由上下封边、两条分置的 casing 侧肋和两条窄玻璃边构成，侧肋之间留有中央开口；预览中可以看到开口/内部转子。修后预览只用于几何和配色，不采样真实 PNG，也不能证明客户端 UV 外观。
- 报告所列修前诊断数值与修前 JSON 结构吻合：完整物品 168 个默认 UV 越界面、转子 76 个；修后四模型的面数和逐面显式 UV 数均与实际 JSON 相符。两个最终预览已查看，呈现完整两格机身、上下封边、顶部结构和维修面。

## 限制与待验收项

资源线的静态合同没有发现需要整改的问题。手持纹理是否正确、游戏内接缝是否可见及实际光照效果仍须按任务卡由客户端人工复测；几何预览不能替代该门。

## 运行线独立审查

- 生产改动符合单状态所有者边界：`CentrifugeUpperProxyBlockEntity` 只继承 `BlockEntity`，没有额外字段或 ticker；`CentrifugeBlock.newBlockEntity` 按 `HALF` 区分下段动力实体和上段代理，上段 `getTicker` 返回 `null`。能力注册仍按方块委托给 `CentrifugeBlock.owner`，其唯一返回值是已配对的下段 `CentrifugeBlockEntity`。上下方向切换时原有能力失效调用覆盖两个位置。
- 旧区块场景不是仅从放置入口验证：GameTest先放置双格机器，显式移除上段代理BE模拟01D缺少BE标签，再调用Create的五面连接判定，并断言查询后代理重新存在。`FluidPipeBlock.canConnectTo` 实际将传入方向取反后调用 `hasFluidCapability`；测试按“机器外向的面”传入其反向，因此五个面方向映射正确。旧BE惰性补建可覆盖Create查询路径；没有另行执行完整区块卸载/重载模拟。
- 对照锁定版 Minecraft `GameTestHelper` / `GameTestSequence` 源码，`succeedWhen` 将条件放入每tick重试的 `thenWaitUntil`；GameTest断言异常会被等待循环捕获并重试。此处 `helper.fail(...)` 会抛异常，`return` 不会造成条件正常返回；流体量/身份不满足时 `assertTrue` 同样抛出。只有观测到上段 1000mB 正确料浆且源罐为 0mB 后才执行 `helper.succeed()`，不存在提前空返回假绿。
- 隔离日志中的修前结果是明确业务失败：启用namespace仅含 `create_nuclear_industry_01e`，实际运行 1 个required用例，并报上段 `up` 面未识别。最终 `latest.log` 启动同一限定域、运行 1 个required用例并通过，记录耗时497.8ms。用例确实覆盖代理无ticker、五面连接、旧BE移除后补建，以及Create玻璃管、机械泵和动力轴网的实际1000mB传输。JUnit XML记录 `CentrifugePortsTest` 共3项、0失败、0错误、0跳过。
- 新namespace下唯一NBT fixture与既有空fixture的SHA-256相同：`F2F22AC0ABA73D67682C2E07BC725B47FD83142B628DA4DABFF66B10860ADB22`。构建目录中的最终 JAR 为 `create_nuclear_industry-0.1.0.jar`（908792字节）；只读检查确认它包含上段代理类、定向GameTest类、新namespace fixture、四个修改后的模型与物品模型入口。运行报告记录本批唯一一次 `assemble` 成功；本审查没有重复运行。
- 有一项兼容范围尚未由本批验证：Create普通 `FluidPipeBlock` 会把各面连接状态保存在blockstate中。锁定源码显示它在 `onPlace` 时安排传播tick；`neighborChanged` 仅在被通知方向原本已打开时安排tick；普通管块及其管道BE没有区块加载时重算连接状态的钩子。若旧01D区块保存的普通管道朝向离心机的连接bit为false，惰性创建上段代理不会自行改写该bit，因此不能据此宣称现存普通管道必定自动复连。当前GameTest用的是 `GlassFluidPipeBlock`，其连接按管轴决定，没有普通管道这种逐面保存的连接bit。客户端人工复测旧存档时需观察既有普通管口；若没有显示/形成连接，可重接相邻管道，测试没有要求拆除离心机。
- 除上述未宣称通过的既有普通管状态恢复外，生产实现、Gradle入口、GameTest断言、JUnit证据和JAR内容没有发现需整改项。测试是机器先就位再接玻璃泵管；未覆盖pipe-first完整玩家操作时序，也没有客户端可视验证。根目录 `logs/` 被定向JUnit的 `forgejunitdev` 更新，PM已确认按其记录处理；本审查没有清理或恢复该目录。

本次运行线审查未运行测试、Gradle或生成器，按治理5.1直接读取既有日志、JUnit XML、构建产物与锁定版本源码。

## 实际使用的技能

- `minecraft-modding/SKILL.md`：对照任务卡锁定的 Minecraft 1.21.1、NeoForge 21.1.219 及模型资源边界。
- `minecraft-testing/SKILL.md`：按治理 5.1 只读取既有资源证据，不重复运行测试。
- `minecraft-resource-pack/SKILL.md`：核对元素面 UV 的 0..16 坐标约束和贴图引用；技能中的通用示例没有用于改变本项目版本。
