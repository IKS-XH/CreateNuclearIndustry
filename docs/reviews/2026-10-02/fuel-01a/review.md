# EXT-A-FUEL-01A 限定独立审查报告

- 候选：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，基线 `6b2138a4dd7c54954e4dc0a42e8d3a4e637687ec`；依照主工程卡与治理协议 5.1 审查。锁定 MC 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82。
- 范围：五项已报缺陷相邻差异；菜单同步、普通与 Shift 取出权限/守恒；料浆 source、LiquidBlock、BucketItem 与 Create 注液契约；离心机及精矿模型引用、父链、朝向继承。未扩展到旧反应堆或整模块回归。
- 技能：已读取并按锁定版本应用 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`；技能示例版本未用于改变项目栈。

## 结论

未发现需整改的确定问题；可以进入五项客户端复测。当前证据支持的是代码、定向测试报告、资源静态链及封包记录，不代表人工门已通过。

## 审查发现与证据

- **菜单同步/取物：无发现。** `CentrifugeMenu.java:32-35, 60-73, 80-112` 为客户端显示副本覆盖同步写入与预测移除；服务端移除仍走 `SlotItemHandler` 对只读输出端口的提取，`set` 不向库存写入，插入被拒。与 `CentrifugeBlockEntity.java:287-310` 的复制读取、拒绝插入及服务端提交提取一致。没有看到普通/Shift 路径能够仅凭客户端副本增加服务端物品或向机器插入。
- **料浆与 Create 装桶：无发现。** `FuelProcessingContent.java:85-96, 117-121` 将同一个 source fluid 连接到标准 `BucketItem` 与 `LiquidBlock`；入口不再为专用旧桶手动注册包装能力。`CentrifugeSlurryBucketTest` 使用实际注册的料浆/桶和 Create 方法，覆盖 1000mB、桶能力与整桶事务。另从候选锁定的 Create 6.0.10-280 source JAR 只读核对 `GenericItemFilling`：普通桶包装器的特例要求非空桶匹配 `fluid.getBucket()`，且包装器只接受精确 `BucketItem` 类（MilkBucketItem 例外）；本次注册满足此条件。该源码核对支持填桶 API 契约，但不证明 Basin 完整点击流程。
- **模型：无发现。** 精矿物品模型引用现存 PNG；离心机 item → block → `minecraft:block/block`，已有六面元素与四向 blockstate 保留。静态父链、方向、尺寸和继承展示参数见 `EXT-A-FUEL-01A-models/verification.json`，JAR/资源及哈希核对见运行报告。
- **已复用的自动证据：** `EXT-A-FUEL-01A-runtime.md` 记录定向 6/6、`targeted-test-final.log` 的 BUILD SUCCESSFUL，以及接手后 assemble/JAR 项核对；模型报告及 verification.json 记录资源检查。没有重跑 Gradle、游戏或全量测试。

## 尚待用户客户端复测

Create 工作盆完整灌桶与生存模式背包返桶、储罐互通、料浆世界放置/舀回，以及精矿显示、离心机 GUI 立体角度和第一/第三人称手持比例均未由本次静态审查验证。按五项复测清单确认这些体验后，再由项目经理处理最终验收。
