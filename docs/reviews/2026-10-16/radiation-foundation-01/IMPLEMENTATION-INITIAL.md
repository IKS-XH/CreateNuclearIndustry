# RADIATION-01 第一批执行交付

本报告只表示候选实现与证据交付，验收/合入状态由PM维护。工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-radiation`，分支 `codex/radiation-foundation`，基准 `5e706e535d97cbcfe167fa4338265b44a610964d`。没有暂存、提交、切分支、合并、推送或修改核心文档；未启动携带的用户世界。正式包由PM另行管理。

## 实现与冻结接口

- `FuelUseHistory` 只向独立副本的 `CUSTOM_DATA` 命名空间子项 `create_nuclear_industry:FuelHistory.Used` 写历史。`tickReactor` 在全部端口一致性校验的准备阶段消费本次 `fission.columns().plannedFuelBurnUnits`，正小数燃耗也标记；只有原提交循环成功才写回端口。失败输入不变，邻列传热/全插棒不标记。燃料库存仍由真实端口拥有。枯竭转换复制原始组件patch、保留名称/历史等，移除燃料专用 DAMAGE/MAX_DAMAGE，产物使用原乏燃料ID。
- `RadiationParameters` 不可变完整参数组；`RadiationConfig` 使用独立 SERVER 文件 `create_nuclear_industry-radiation-server.toml`，加载/重载整组验证，非法回退默认并节流告警。退出卸载不误报非法参数。事故源强4只保留参数，没有事故物品/残骸/世界动作。
- `RadiationItemSources.describe` 返回未屏蔽/自身容器屏蔽后D/s、放射性载荷标志、完成状态和访问数。数据组件 CONTAINER/BUNDLE_CONTENTS 用显式栈；合法桶施加一次0.05，非法但含料桶仍按裸载荷读，空桶0。放射性身份独立于配置源强是否设0；有限强度极大值在限总场强前饱和加乘，避免Infinity/NaN。
- `RadiationSourceIndex.sample` 按已加载区块收集真实只读投影，距离 `1/max(1,d²)`，半径默认32，先屏蔽后累加限10 D/s。相同位置合并射线路径；不同真实物品不按内容去重。带只按同一 TransportedItemStack 实例身份去重。所有世界/玩家tick完成后统一处理玩家，避免同tick缓存跨越玩家物流动作。
- `RadiationContainment` 保存已验证边界和端口对应堆内点，不保存燃料。壳完整性只在载入/相关真实方块变化后核验；停机、缺料、内部机构无效不取消完整壳。按扫描器的底/棱边仅外壳、顶帽外壳/换料口/驱动、侧壁外壳/窗/侧口名单判定。普通墙按明确标签做有界体素射线，每格一次。持久化不依赖仪表继续存活。
- `RadiationDoseCalculator.advance` 只计算在线tick的剂量/恢复。玩家剂量在 NeoForge persistentData 子项 `create_nuclear_industry:RadiationDose` 保存，非死亡克隆保留，死亡清零；创造/旁观只测真实场强。25D警告、50D短时虚弱/缓慢、80D每40tick1HP，伤害绕过普通护甲。效果自然到期，不主动删除其他来源药水，牛奶不改剂量。关闭辐射不伤害，按安全规则恢复。
- 四件防护服使用原版皮甲材质、耐久、防御、皮革修复，按实际槽位减25/30/25/15个百分点，最多95%。五条有序配方恰24皮革+4铅板，以及传感器/黄铜外壳/玻璃板各1。皮甲/饰纹模型和穿戴贴图复用原版，客户端补原生颜色绑定；仪表复用钟的静态图标，没有时间用途。
- `RadiationExposurePayload` 仅S2C：维度、gameTime、场强、剂量、吸收、完成状态；没有客户端改剂量入口。主/副手任一持表仅发一包，默认1Hz。右上侧三行HUD避开左侧护目镜和底部手持/状态区域；旧世界/过期样本拒绝，断线清缓存。声音按强度节流，每样本最多一次。显示/声音的实际客户端验收仍未做。
- 放射性原版 ItemEntity 在真实加入/到期入口无限寿命；Mixin 仅拦截火/岩浆、仙人掌、爆炸伤害。正常拾取不受阻，普通物品仍按原版销毁。虚空、管理员/创造删除及任意第三方删除设备不承诺拦截。

## 支持矩阵与预算

| 范围 | 真实读取入口/唯一所有者 | 自动证据与边界 |
| --- | --- | --- |
| 玩家 | Inventory全部槽一次，含主副手/装备；原版随身容器组件 | 真实装备/持久化用FakePlayer；真人携带源影响旁人留多人手测 |
| 掉落 | ItemEntity实际栈 | 加入、到期、四类伤害、拾取真实断言 |
| 箱/陷阱箱/木桶/潜影盒/漏斗 | 各BE的Container本地槽；不读双箱合并capability | 代表性箱、嵌套组件、同坐标替换BE真实断言；各外观逐一手动搬运未做 |
| 反应堆 | ReactorPortBlockEntity唯一物品栈，按已知列映射为堆内点 | 小数真实燃耗/端点提取、取消提交、壳失效/修补/非法顶帽 |
| 离心机/烧结炉/装配台 | 原设备公开输出/输入入口；装配台只读主控 | 不增加可收物品种类；这些设备的逐一动力物流操作未做 |
| 贮存架 | state十六个真实槽，每桶先验合法 | 完整架0，满架16桶实测；非法含料记录不凭架外观享受屏蔽 |
| Create带 | 控制端BeltInventory正式表+toInsert−toRemove，只读Accessor | 通过相邻已加载段发现控制端，运输实例去重；不叠加segment capability；待入队/重复身份/待移除真实断言 |
| Create机械臂/置物台 | heldItem只读Accessor / getHeldItem | 原端点提取事务、真实持物/置物状态断言；连续动力动画往返留人工 |
| Create工作盆/保险库 | 输入+输出库存 / 每格getInventoryOfBlock | 两份独立盆载荷4D/s、保险库局部库存2D/s真实断言；不再叠加控制端CombinedInvWrapper |
| 其他公开设备 | 一个无面ItemHandler，仅作为可读入口 | 不宣称任意模组、私有背包、远程仓库、移动构造库存的全面兼容 |

默认每维度每tick技术上限：32768个栈读取/遍历工作单位、4096个BE审计工作单位、32768个射线格、8192个外壳格。标准完整16³和满架在一个采样回调内完成；默认检测周期20tick。超预算区块按BE/槽游标续接，嵌套Cursor续接，已完成区块在等待期至多保留20tick，40tick无人请求撤队；普通完整采样下一tick重读。BE.setChanged、真实方块提交、区块卸载清理对应投影，方块变化/区块卸载清射线缓存；只查已加载块，不强载。组件元数据变化使物品摘要失效。

未完成样本明确为incomplete，HUD显示采样，既不按0恢复也不推进吸收/安全等待；已有高剂量症状/伤害时钟独立继续。配置关闭时可确定吸收为0，仍安全恢复。65536件深层嵌套测试在4个真实tick后完成；连续不停改变超大库存可能反复使其审计失效，读数会明确维持采样态。本批不宣称此类极限动态场景能满足20tick，不用0掩盖未完成。

## 使用技能与实际版本

实际读取并应用：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`（NeoForge注册、真实物品/生命周期/S2C）；`minecraft-testing/SKILL.md`（定向JUnit、真实GameTest与FakePlayer边界）；`minecraft-resource-pack/SKILL.md`（1.21.1单数资源目录、原版模型/标签）；`minecraft-ci-release/SKILL.md`（候选制品排除与SHA256）。

流程技能实际入口为 `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/` 下 `executing-plans/SKILL.md`、`test-driven-development/SKILL.md`、`systematic-debugging/SKILL.md`、`verification-before-completion/SKILL.md`。按项目规则只读Git、唯一执行者、不逐步另派审查。RED证据覆盖使用历史缺口/枯竭丢名称、基础源/剂量接口与配方缺失、零强度载荷身份、极大有限源强和非法顶帽；后补细项参数/剂量/资源断言集中验证，没有声称每个细项都逐条先RED。

实际Gradle基线保持 Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；没有新依赖、升级、加载器/许可改动。

## 命令、失败与复核

- 定向：`./gradlew.bat test --tests '*radiation.*' --tests '*reactor.FuelRefuelingTransactionTest' --tests '*reactor.ReactorServerTickTest'`。最终41测试、0失败/错误，日志 `targeted-delivery-junit.log`，XML独立保存在 `junit-xml/`。参数、分类、剂量、防护成本/修复、旧燃料事务12项和旧服务端tick9项均覆盖。
- 真实：`./gradlew.bat runGameTestServer -PgameTestNamespace=cni_radiation_foundation -PgameTestDirectory=run/radiation-foundation-gametest`。仅独立测试域/目录，没有伪登录/用户世界。
- 增量打包：`./gradlew.bat assemble`，日志 `assemble-final.log`。没有clean、--rerun-tasks或机械全量测试。
- `task1-red.log`：2个预期失败（缺使用历史接口、枯竭丢名称）。`contracts-red.log` 曾因ItemStack构造器要求Holder编译失败；纠正后 `contracts-red-fixedcompile.log` 中历史4项绿、源/配方存在性2项预期红。`compile-foundation.log` 保留 BeltInventory实际transport包路径、ItemExpireEvent不是可取消事件两个编译整改。
- `gametest-first.log`：4/7通过，Java退出3/Gradle1；拆箱产生真实掉落被测试误叫幽灵、未初始化单段带被Create移除、FakePlayer固定免伤。修夹具时保留真实掉落行为，先领取载荷再验证空BE替换；带立即设置测试链长度；专用FakePlayer仅解除默认免伤/出生保护，不创建登录连接。
- `gametest-corrected.log`：5/7通过，Java2/Gradle1；测试误写字段respawnInvulnerabilityTime，实际源码为spawnInvulnerableTime；修改夹具字段后 `gametest-final.log` 7/7、保存全部维度、正常exit0。
- 索引续接/库存脏通知之后的 `gametest-audit-final.log` 为9/9，`gametest-delivery.log` 在外壳预算/HUD调整后9/9，`gametest-frozen-source.log` 在有限数/当前玩家完整NBT保存加载后9/9；每轮保存和退出均实际记录，旧7/7不代替收尾来源。
- `zero-strength-payload-red.log`、`finite-source-red.log` 各1项预期失败，修复后进入最终41项绿。`shell-position-red.log` 真实复现非法顶帽观察窗仍获整壳零外泄，8/9通过、Java1/Gradle1。PM批准按位置名单整改，最终壳体复核见下方冻结证据。
- 历史断言未删除/放宽。最终真实服日志中的非法阈值告警是配置负向用例预期输出，结束前值已恢复。既有Mixin/弃用API/原版命令歧义/版本检查告警不作为本批通过项。

## 性能解释与人工项

性能为独立真实GameTest服务端的ServerTickEvent.Pre/Post耗时分布，每阶段40个在线tick。无16³阶段仍有同批已加载的其他小夹具；GameTest可快进，样本短且有JIT/GC影响，不据此声称生产服开关净收益或长期MSPT。完整16³、196件已用组件+满架的查询计数为443栈工作单位、203 BE、射线0，稳定期外壳读取增量0，未超过默认周期。S2C计数0对应没有真实持表登录玩家，不当成网络性能通过。四真人、客户端看向/背向、GUI和原液体/控制棒视觉仍未验证。

集中手测入口：在候选树执行 `./gradlew.bat runClient`，由用户明确选择携带的世界或新测试世界；本次未启动该客户端。依计划第4节依次核：新燃料0/首次不足1耐久即放射；主副手仪表三行中文与声音；真实动力臂→带→置物台→箱/潜影盒→桶→架；完整停机壳/破口/合法补壳；裸露逐步症状、满套仍吸收5%、离开恢复；中途正常保存退出重进恢复剂量/标记/壳体；真人携带源对旁人以及16³看向/背向/GUI卡顿。真实NBT往返与服务端保存不替代真人重进/视觉验收。

首批没有事故账本、破坏/冲击、事故废料、残骸或其封装配方。没有热工/管理员/旧配方/DU平衡改动；没有兼容旧存档迁移研究。

## 文件、制品与携带世界

精确最终写集及SHA256见 `source-files.txt` / `source-sha256.json`；包含被既有 `.gitignore:15 config/` 忽略的 **src/main/java/com/iksxh/create_nuclear_industry/config/RadiationConfig.java**。该文件已进入编译/JAR，但Git提交须由PM对明确源文件另行force-add；执行者未改ignore或Git。

候选身份见 `candidate-jar.json`；这是未提交生产来源的候选，不视作main正式制品。JAR核验本批GameTest类（含嵌套性能观察器）和模板均不携带，RadiationConfig/SourceIndex和仪表资源存在。首次收集器误把两个空GameTest目录也当测试内容失败，随后按整个独立域路径排除，保留其整改说明。

携带main世界27文件/13337924 bytes逐项核哈希仍完全一致，原 `saved-world-sha256.json` 未修改，复核另存 `saved-world-after-verification.json`。候选根跟踪的logs/latest.log与debug.log由JUnit自动写出（有既有尾随空白告警），未手改/回滚，PM不纳入生产提交。所有原始失败日志、探针源码摘录和独立运行世界均保留。


## 最终冻结证据

最终壳体位置修正后的真实日志是 `shell-position-green-final.log`：9/9 required断言通过，三维度全部保存，Java与Gradle正常退出0。它包括非法顶帽观察窗/合法帽恢复、完整停机/内部无效壳、实际小数燃耗/换料、当前玩家完整NBT往返、嵌套4tick续接、配置重载和真实掉落保护。定向41项JUnit对应 `targeted-delivery-junit.log`；最后仅壳位置名单及制品域排除变化，复用未改变的JUnit实现证据，新增壳分支由上述真实域覆盖。此后未再改生产源。

最终每阶段40个GameTest服务端tick的Pre/Post区间如下（毫秒；非因果净收益，非真人客户端帧时）：

| 场景 | P50 | P95 | P99/最大 |
| --- | ---: | ---: | ---: |
| 未建16³，不调度采样 | 0.7372 | 1.2041 | 1.2955 |
| 未建16³，20tick调度采样 | 0.5023 | 1.2560 | 2.0387 |
| 16³空闲堆，启用采样 | 2.8939 | 6.2473 | 33.7253 |
| 16³/196已用组件/满架，启用采样 | 5.6042 | 10.4421 | 26.1276 |

最终源文件共61个（含测试/模板/资源），逐项身份在 `source-sha256.json`。候选JAR：

- 路径：`E:\MyMC\NewMod\Create_NuclearIndustry-radiation\build\libs\create_nuclear_industry-0.1.0.jar`
- 字节数：3417633
- SHA256：`9F78F1100D0EEAE6545B6A1A92B0416AB504CC081ED160D25B9CB53B0982B46A`
- 本批GameTest类/模板/命名空间目录条目：0。RadiationConfig.class、RadiationSourceIndex.class及仪表资源存在。

精确写集（全部相对候选根，忽略规则内配置源亦显式列出）：

```text
build.gradle
src/main/java/com/iksxh/create_nuclear_industry/CreateNuclearIndustry.java
src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java
src/main/java/com/iksxh/create_nuclear_industry/config/RadiationConfig.java
src/main/java/com/iksxh/create_nuclear_industry/content/ModCreativeTabs.java
src/main/java/com/iksxh/create_nuclear_industry/content/RadiationContent.java
src/main/java/com/iksxh/create_nuclear_industry/gametest/RadiationFoundationGameTests.java
src/main/java/com/iksxh/create_nuclear_industry/mixin/RadiationArmAccessor.java
src/main/java/com/iksxh/create_nuclear_industry/mixin/RadiationBeltAccessor.java
src/main/java/com/iksxh/create_nuclear_industry/mixin/RadiationBlockChangeMixin.java
src/main/java/com/iksxh/create_nuclear_industry/mixin/RadiationInventoryChangeMixin.java
src/main/java/com/iksxh/create_nuclear_industry/mixin/RadiationItemEntityMixin.java
src/main/java/com/iksxh/create_nuclear_industry/radiation/FuelUseHistory.java
src/main/java/com/iksxh/create_nuclear_industry/radiation/ItemSourceSummary.java
src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationContainment.java
src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationDoseCalculator.java
src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationDoseState.java
src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationEquipment.java
src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationExposurePayload.java
src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationFieldSample.java
src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationItemSources.java
src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationNetwork.java
src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationParameters.java
src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationServerEvents.java
src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationSourceAdapters.java
src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationSourceIndex.java
src/main/java/com/iksxh/create_nuclear_industry/radiation/client/RadiationClientEvents.java
src/main/java/com/iksxh/create_nuclear_industry/radiation/client/RadiationClientRegistration.java
src/main/java/com/iksxh/create_nuclear_industry/reactor/FuelAssemblyItemCodec.java
src/main/java/com/iksxh/create_nuclear_industry/reactor/FuelRefuelingTransaction.java
src/main/java/com/iksxh/create_nuclear_industry/storage/DryStorageBlockEntity.java
src/main/resources/assets/create_nuclear_industry/lang/en_us.json
src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json
src/main/resources/assets/create_nuclear_industry/models/item/radiation_boots.json
src/main/resources/assets/create_nuclear_industry/models/item/radiation_chestplate.json
src/main/resources/assets/create_nuclear_industry/models/item/radiation_helmet.json
src/main/resources/assets/create_nuclear_industry/models/item/radiation_leggings.json
src/main/resources/assets/create_nuclear_industry/models/item/radiation_meter.json
src/main/resources/create_nuclear_industry.mixins.json
src/main/resources/data/cni_radiation_foundation/structure/radiation_empty.nbt
src/main/resources/data/create_nuclear_industry/damage_type/radiation.json
src/main/resources/data/create_nuclear_industry/recipe/crafting/radiation_boots.json
src/main/resources/data/create_nuclear_industry/recipe/crafting/radiation_chestplate.json
src/main/resources/data/create_nuclear_industry/recipe/crafting/radiation_helmet.json
src/main/resources/data/create_nuclear_industry/recipe/crafting/radiation_leggings.json
src/main/resources/data/create_nuclear_industry/recipe/crafting/radiation_meter.json
src/main/resources/data/create_nuclear_industry/tags/block/radiation_shielding.json
src/main/resources/data/minecraft/tags/damage_type/bypasses_armor.json
src/main/resources/data/minecraft/tags/item/chest_armor.json
src/main/resources/data/minecraft/tags/item/enchantable/armor.json
src/main/resources/data/minecraft/tags/item/foot_armor.json
src/main/resources/data/minecraft/tags/item/head_armor.json
src/main/resources/data/minecraft/tags/item/leg_armor.json
src/main/resources/data/minecraft/tags/item/trimmable_armor.json
src/test/java/com/iksxh/create_nuclear_industry/radiation/FoundationContractsTest.java
src/test/java/com/iksxh/create_nuclear_industry/radiation/FuelUseHistoryTest.java
src/test/java/com/iksxh/create_nuclear_industry/radiation/RadiationDoseTest.java
src/test/java/com/iksxh/create_nuclear_industry/radiation/RadiationEquipmentDataTest.java
src/test/java/com/iksxh/create_nuclear_industry/radiation/RadiationItemSourcesTest.java
src/test/java/com/iksxh/create_nuclear_industry/radiation/RadiationParametersTest.java
src/test/java/com/iksxh/create_nuclear_industry/radiation/RadiationShieldingTest.java
```
