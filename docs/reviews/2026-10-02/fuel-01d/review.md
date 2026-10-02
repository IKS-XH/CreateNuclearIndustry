# EXT-A-FUEL-01D 合并规格与质量审查

审查候选：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，基线 `4ade0dc` 加本轮冻结改动。只读代码、任务合同、运行/资源报告、定向测试 XML 与 JAR；没有运行测试、构建或游戏，没有 Git 写操作。实际使用 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack` 技能，并按仓库锁定的 Minecraft 1.21.1、NeoForge 21.1.219、Create 6.0.10-280 源码核对放置、挖掘、扳手与爆炸回调。

## 结论

整改后没有发现当前 P0/P1 代码阻断。初审发现的 `getDrops` 在纯查询时占用拆卸标记问题已修复：掉落查询现在只生成带账本快照的栈，标记移动到普通拆除、扳手、爆炸和直接替换的实际移除流程。静态顺序检查未发现同一台双格机器重复掉落或另一台机器受同 tick 标记影响的路径。

## 规格与实现核查

- 单件放置：`CentrifugeBlockItem` 先检查上段高度、可替换性与碰撞，再写下段和上段；第二格失败会恢复下段原状态。锁定版 `BlockItem.place` 在成功写块后才消费物品；NeoForge `CommonHooks.onPlaceItemIntoWorld` 会对双快照发多方块放置事件，取消时倒序恢复快照和物品计数。这里是锁定源码与控制流核查，不是世界内放置实测。
- 拆除：玩家挖掘在 `playerWillDestroy`、蹲扳手通过 `BreakEvent` 取消门后、爆炸在原版取掉落后进入 `onBlockExploded`，各自只领取一份便携账本；直接替换由 `onRemove` 兜底并清掉配对段。创造拆除按合同不返还物料。上下任一半触发时都能从下段唯一 BE 保存同一状态。纯查询 helper `portableTag` 不写移除标记。
- 状态与能力：唯一 BE、应力和加工 tick 位于下段；上段不创建 BE。旧未配对单格因 `isCurrentMachine()` 失败而停机并保留自身数据。两个位置的 capability handler 每次操作都重新校验完整配对、朝向、移除状态和 owner，错段或拆换后的缓存引用拒绝读写。护目镜从上段代理到有效下段。
- 物流与手工操作：只有上段顶面接受/抽回铀浆；八个水平机面提供水和两粉提取；底面、内部接面和无方向查询不提供物料。错误方向的容器交互被消费但无端口事务，避免液体掉在机器旁。新增 JUnit 通过无世界测试验证端口入口，不覆盖真实配对机器。
- 客户端和模型：客户端 renderer 仅注册在 `Dist.CLIENT`，只在有效下段渲染转子；过载、零速和轴承磨损门会停转，render AABB 从下段扩到两格高。资源证据列出八种状态、部件范围和纹理引用；我检查了两张冻结预览，未见贯穿接缝缺口。预览不等于游戏 UV、光照、放置和手持效果验证。
- JAR 只读核验：`build/libs/create_nuclear_industry-0.1.0.jar` 的 SHA-256 为 `D86D2CC3BB8446502F63443C26747CCBD0A49FE6193924E756C80A1AB1B72929`，与运行报告一致；JAR 含 8 个 blockstate 变体和 lower、upper、item、rotor 四个模型文件。

## 非阻断建议（已关闭）

初审发现 `playerDestroy` 空实现遗漏原版 `BLOCK_MINED` 统计与 `0.005F` 疲劳；运行线已按限定范围补回，仍不调用 `super.playerDestroy`，因此不会再次生成掉落。详见下方定点复核。

## 仍需人工验收的边界

运行报告中的 `CentrifugePortsTest` 整改后 3/3 通过，但新增用例只重复调用 `portableTag` 并检查账本与标记不变；没有真实 `ServerLevel`，也没有调用完整 `getDrops` 或模拟拆除事件。另两类原有桶/交易 JUnit 2/2 通过可复用。当前证据因此不能证明真实世界的放置取消回滚、双半普通挖掘/扳手/创造/爆炸/替换、陈旧 capability 生命周期或实际物流/加工行为。

客户端验收清单仍需覆盖：空间不足与权限取消不耗机件；任一半拆除只返还一台且保留库存；创造拆除不复制物料；爆炸与方块替换不留残壳或重复掉落；错配、缺段及替换后端口拒绝；旧单格暂停保留并可收起重放；顶部料浆桶、八个水平面的水/两粉、底部传动及唯一应力；代表性加工、转子停转/过载、完整两格外观和物品手持尺寸。此清单是待用户操作的门，不是当前代码审查失败，也没有在本轮代替用户运行。

证据索引：`build/reports/extension/EXT-A-FUEL-01D-runtime.md`、`build/reports/extension/EXT-A-FUEL-01D-assets.md` 与相邻 evidence/XML/preview 文件。审查限于本任务合同，没有扩展到无关玩法或全量资源。

## 最后定点复核：playerDestroy 原版副作用

运行线补回 `Stats.BLOCK_MINED` 和 `player.causeFoodExhaustion(0.005F)` 两项；没有调用 `super.playerDestroy`，掉落仍只在 `playerWillDestroy` 预先领取一次，避免第二次掉落。只读检查了实现、运行报告摘录及 JAR：限定 `assemble --offline` 输出为 `BUILD SUCCESSFUL in 2s`；最终 JAR SHA-256 为 `D86D2CC3BB8446502F63443C26747CCBD0A49FE6193924E756C80A1AB1B72929`，与本机文件计算值一致。没有重跑 JUnit，也没有重新检查其他范围。该建议关闭。
