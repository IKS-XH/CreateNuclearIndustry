# EXT-A-FUEL-01D 运行线交付

候选工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；开工 HEAD `4ade0dc`。本报告仅说明运行线未提交改动，资源线独立交付。未进行 Git 写操作，也未运行默认客户端或全量 GameTest。

## 实施

- 同一 `enrichment_centrifuge` 以 `half=lower|upper` 表示上下段；定制 BlockItem 同次放置两格。源码核对：`BlockItem.place` 在写块后才消耗物品，NeoForge `CommonHooks.onPlaceItemIntoWorld` 对两个捕获快照发多方块放置事件，取消时倒序回滚并恢复物品计数。预检高度、替换性、上段碰撞；第二格写失败时恢复下段原方块状态。
- 下段唯一持有 Create 动力实体、库存、密闭批次、磨损及加工 tick；上段不创建实体。旧单格缺少 `CentrifugePaired` 标志，默认暂停，不占用上方；护目镜提示扳手收起后重放。旧 `CentrifugeFilters` NBT 不再参与读写，携物快照保留原料、产品、批次与磨损。
- 上段 UP 只接料浆且可抽回；上下段四个水平面只出水及两粉；内部上下接面、下段 DOWN 与 null 不暴露物料。缓存 handler 每次操作复查方块、朝向、有效下段和实体身份。空手取粉、桶交易、下段正面停机维修继续走世界内交互。流体容器命中错误面或缺段时消费交互，避免倒入机器旁。Create 护目镜瞄准上段转到下段。
- `getDrops` 仅构造快照，重复查询不占拆卸标记。普通玩家挖掘在 `playerWillDestroy` 取快照并标记真实拆卸；`playerDestroy` 不再次掉落。Create 蹲扳手经过 BreakEvent 取消门后取掉落并标记；爆炸路径的原版 `onExplosionHit` 先取掉落，再由 `onBlockExploded` 标记。直接方块替换则在 `onRemove` 保全账本并静默清除另一半。创造拆除不返还物料。双段扳手旋转同步朝向。转子客户端只在有效下段渲染，渲染包围框为两格高度；方块采用 `noOcclusion` 配合资源线开口几何。

## 验证

- 锁定版本：Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280。实际读取 `minecraft-modding` 与 `minecraft-testing` 技能，并以本仓库锁定源码确认 BlockItem、NeoForge 放置回滚、Create 扳手、IBE 与爆炸调用顺序。
- 定向 `gradlew test --tests '*CentrifugePortsTest' --tests '*CentrifugeContainerTransactionTest' --tests '*CentrifugeSlurryBucketTest' --offline`：初次 6/6 通过。纯 `getDrops` 副作用整改后，只复跑受影响的 `CentrifugePortsTest`，当前该类 3/3 通过；其余两类各 2/2 是复用初次证据，不与整改轮相加。其中新增测试重复调用 `getDrops` 实际使用的 `portableTag`，两份账本相等且未占拆卸标记。该测试不模拟 `ServerLevel` 及完整 `getDrops` 回调。三份原始 XML 与准确命令、Gradle 工具输出摘录位于同名证据目录。
- 资源线冻结后一次 `gradlew assemble --offline` 成功；掉落标记整改后增量 `gradlew test --tests '*CentrifugePortsTest' assemble --offline` 也成功。独立审查指出 `playerDestroy` 空实现遗漏原版 `BLOCK_MINED` 统计和 0.005 疲劳；对照锁定原版源码，仅补这两项，不再次调用掉落路径。该限定修正后 `gradlew assemble --offline` 成功，未重跑 JUnit。仅有既有 FluidType 与事件订阅注解废弃警告。最终 JAR SHA-256：`D86D2CC3BB8446502F63443C26747CCBD0A49FE6193924E756C80A1AB1B72929`。已读取 JAR，含 8 个 `half/facing` blockstate 变体与 lower、upper、item、rotor 模型路径。
- `git diff --check` 仅报告测试自动改写的跟踪日志 `logs/debug.log`、`logs/latest.log` 的尾随空白；执行者未修改或恢复它们，交项目经理在整合时处理。

## 待审与人工门

- 纯查询提前占用领取标记的问题已按项目经理要求整改；标记只在实际拆卸、爆炸移除或直接替换事务写入，且不持久化。独立审查仍需静态核对各回调次序与取消门，JUnit 不证明真实世界拆除。
- 转子在停转或过载时静止于零相位，转动切换可能跳位；是否接受由资源与客户端视觉审查确认。
- 现有 JUnit 无可靠真实世界放置/破坏模拟，本批未临时扩建测试框架。客户端需查：空间不足和多方块放置取消不耗物；任一半普通挖掘、蹲扳手、创造和爆炸/替换只留下单件且账本一致；缺段/错朝向/替换后的缓存端口拒绝；旧单格保留状态且可收起重放；上下八个水平面输出、上顶进浆/抽回、底动力与一台应力；一批实际加工、转子和双格外观。

技能实际使用：`minecraft-modding` 用于 NeoForge/Create 生命周期和能力接入；`minecraft-testing` 用于区分 JUnit 策略、原桶事务与必须留在客户端的真实世界行为。遵循治理 5.1 的定向验证，不重复全量测试。
