# RADIATION-01 第一批执行交付（第二轮定向整改后）

本报告仅交候选实现/证据，验收与Git由PM管理。工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-radiation`、分支 `codex/radiation-foundation`、基准 `5e706e535d97cbcfe167fa4338265b44a610964d`。没有Git写操作、核心文档/用户世界修改或派新Agent。初审原报告在first-review-input/REPORT.md，fix1报告在second-review-input/REPORT.md；当前详细整改见 [REVIEW-FIX-02.md](REVIEW-FIX-02.md)。

本轮只补RR1：已处理路径区块及已知外壳范围的load/unload会使pending Job的相关屏蔽组重算，不失效无关区块任务、不强载。R1/R2/R3已在复审闭合，保持其续接与发送行为。生产源及测试最终冻结于review-fix-02-delivery.log（2/2、保存、exit0）对应来源，assemble已在生产源固定后完成，最终断言后仅身份收集/报告。

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


源调度已按R1/R2收尾为按玩家UUID保持固定目标的一轮Job：源快照/实体后缀/单条射线/验证/累加均显式跨tick。目标小幅移动和无关方块更新不重置整轮，真实相关源/屏蔽按依赖失效；退出/换维/世界卸载撤任务。R3把每20tick显示发送与重试/剂量时钟分离，未发complete优先送达，客户端允许完成结果接替pending。第一轮8项改变及RED/GREEN见REVIEW-FIX-01.md，本轮4项变化见REVIEW-FIX-02.md。


RR1新增Job.pathChunkDependencies与Containment.chunkFrames，load/unload命中实际已处理路径区块或已知壳范围时重排组；未知路径也登记，防止载入墙遗漏。只读可见性注入默认读取真实getChunkNow，注入不能越过实际加载检查。具体职责/限制见整改报告；默认上限和dirty壳缓存不变。

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


默认每维度每tick上限保持32768栈工作、4096 BE、32768射线格、8192外壳格。摘要采用弱ItemStack缓存与嵌套Cursor；每轮Job持固定目标/参数、实际owner/实体引用和只读源投影，不拥有库存。来源校验/求和均计工作，完整后撤队，下轮重读；40tick无人请求撤队。源变更按owner/区块/实体索引失效，屏蔽按射线格和已知壳依赖重算，无关方块不清整维度。仅已加载区块，绝不强载；新世界/换维/退出不能交付旧任务。原生空间查询和初始引用快照为同步集合枚举，预算不等于任意实体规模下硬耗时保证。

incomplete不按0恢复、不推进吸收/安全等待，症状及80D以上伤害仍按在线tick运行。标准16³/196已用组件/满架一个查询回调完成、20tick调度内；受控低预算移动样本25个续接tick、20实体尾部5tick、默认65536嵌套2tick。极端相关内容持续修改可继续延后，不能以complete的0冒充。多玩家共享预算公平性和真实多人性能没有实测。


以上标准场景工作量/延迟和超预算活性数值来自fix1的review-fix-01-delivery.log，本轮复用；新增区块依赖失效由本轮两项定向用例单独覆盖，没有把先前13项当作当前源码新跑结果。

## 使用技能与版本

实际读取并应用：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`（NeoForge注册、真实物品/生命周期/S2C）；`minecraft-testing/SKILL.md`（定向JUnit、真实GameTest与FakePlayer边界）；`minecraft-resource-pack/SKILL.md`（1.21.1单数资源目录、原版模型/标签）；`minecraft-ci-release/SKILL.md`（候选制品排除与SHA256）。

流程技能实际入口为 `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/` 下 `executing-plans/SKILL.md`、`test-driven-development/SKILL.md`、`systematic-debugging/SKILL.md`、`verification-before-completion/SKILL.md`。按项目规则只读Git、唯一执行者、不逐步另派审查。RED证据覆盖使用历史缺口/枯竭丢名称、基础源/剂量接口与配方缺失、零强度载荷身份、极大有限源强和非法顶帽；后补细项参数/剂量/资源断言集中验证，没有声称每个细项都逐条先RED。

实际Gradle基线保持 Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；没有新依赖、升级、加载器/许可改动。


本次审查整改另实际使用 `superpowers/6.4.2/skills/receiving-code-review/SKILL.md`，先核源码与三项RED再改。未改变批准玩法参数、源强、预算、热工/管理员/旧配方、第三方私有库存兼容或旧存档迁移；事故账本/世界破坏/事故废料/残骸与封装配方仍在第二批。


本轮重新读取并应用Minecraft modding/testing（服务端只读查询、NeoForge事件/GT边界）、ci-release（候选身份与排除）、receiving-code-review/TDD（核控制流、行为RED、最小修复）、verification-before-completion（真实断言/保存/退出/身份）；实际入口仍为前述C:/Users/IKSXH/.codex/skills与superpowers/6.4.2/skills目录。无资源/版本改动，资源技能沿用首批读取与证据。

## 当前验证与历史证据

本轮实际命令：

```text
./gradlew.bat runGameTestServer -PgameTestNamespace=cni_radiation_foundation -PgameTestDirectory=run/radiation-foundation-gametest --init-script build/reports/radiation-foundation-20261016/review-fix-02-select.init.gradle
```

- init脚本只显式注入cni.radiation.reviewChunkOnly=true；本卡测试类内注册两项，日志明确2 tests running/2 required passed。默认分支保留原9+4=13项，静态核对注册与templateNamespace，本轮没有启动默认13项。没有新增模板/改build.gradle。
- 首次review-fix-02-red.log是2个夹具Block/BlockState编译错误、Gradle1；修正后有效行为RED为review-fix-02-red-compiled.log，2/2失败、Java2/Gradle1并保存三维度。旧Job分别交付0和未衰减值，两项根因与复审一致。
- 修复后review-fix-02-green.log为2/2、保存、exit0。补明确源/目标真实区块仍加载断言后，最终review-fix-02-delivery.log **2/2 required通过、三维度全部保存、Java/Gradle退出0**，这才是当前冻结来源。
- 外壳另一块受控不可见后重算为1.953125E-4D/s；已处理路径块恢复可见且有实际墙，重算为6.377551020408162E-5D/s。可见性夹具+向NeoForge总线投递事件，**不是实际区块卸载/磁盘加载或真人操作**。外部目标为坐标，同一生产算法、源/owner仍实际加载。
- 生产源码固定后 `./gradlew.bat assemble`，review-fix-02-assemble.log增量成功/exit0；随后仅补两条被排除的测试断言，当前JAR全部生产class已与当前编译class逐字节核对，无需再次assemble。无build/clean/rerun-tasks。git diff --check -- src build.gradle通过。
- 原41项JUnit复用targeted-delivery-junit.log与junit-xml/；fix1的13项和同场景配对性能复用review-fix-01-delivery.log，其来源封存second-review-input/。没有重跑或冒称fix2新结果。原始所有失败、REVIEW/RE-REVIEW、历史报告与JAR均保留；详见两轮整改报告。

## 性能与人工门（复用fix1来源）

以下性能与标准预算计数对应fix1的review-fix-01-delivery.log，而非本轮新测。本轮只改区块事件相关依赖通知，没有新增多人/网络或长期性能数据。

| 同一16³/196已用组件/满架场景 | 样本 | P50 ms | P95 ms | P99/最大 ms |
| --- | ---: | ---: | ---: | ---: |
| 不调度采样 | 40 tick | 3.7506 | 6.6842 | 25.4380 |
| 每20tick调度采样 | 40 tick | 3.3997 | 5.9918 | 8.6439 |

这两个相邻阶段保持场景、服务端版本和实现一致，先关闭、再开启手动诊断查询调度；不是切换辐射玩法配置。Pre/Post服务端tick区间包含其他已加载夹具，GameTest可快进，样本短且未随机顺序/JIT/GC校正。观察差值P50为−0.3509ms、P95为−0.6924ms，不能据此推断采样带来加速或长期因果净成本。源查询实际使用生产默认预算，完整场景835栈工作/203 BE/0射线，配对后段834/203/0；增加的验证/累加工作亦计入栈预算，稳定外壳读取增量0，一个回调完成，默认调度最大等待20tick。

同日志其他各40tick阶段：无16³关闭P50/P95/最大0.6491/2.3495/4.5759ms，无16³开启0.5754/1.1170/1.3526ms，16³空闲开启2.7218/7.1825/26.6904ms，完整已用堆配对前开启5.6013/11.1276/25.9640ms。它们是背景记录，不代替上述同场景配对。所有数据是服务端GameTest；没有四真人、客户端看向/背向/GUI或真实网络性能证据。


集中手测入口：候选树 `./gradlew.bat runClient`，由用户选择携带世界或新测试世界，本次未启动。按计划第4节核首次不足1耐久燃耗即放射、主副手仪表中文HUD/声音/遮挡、原版防护服外观与护甲属性；真实动力臂→带→台→箱/潜影盒→桶→架；完整停机壳/破口/补壳；剂量症状/满套吸收5%/离开恢复；正常保存退出重进、真人换维、携带源影响旁人、16³看向/背向/GUI与四真人性能。真实BE分阶段交接、FakePlayer、NBT往返和维度保存不替代这些人工项。


本轮还明确保留真实区块卸载/加载体验未验证；受控可见性生命周期回归不替代它。事故账本/世界破坏/新废料残骸/封装配方仍第二批，未改热工/管理员/旧配方/DU平衡或研究旧存档迁移。

## 精确文件、候选与世界

当前source-files.txt/source-sha256.json共61项，固定副本source-review-fix-02.json；相对fix1恰4项变化、无新增/删除，review-fix-02-source-delta.json列前后身份。精确本轮文件：

- `src/main/java/com/iksxh/create_nuclear_industry/gametest/RadiationFoundationGameTests.java`
- `src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationContainment.java`
- `src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationServerEvents.java`
- `src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationSourceIndex.java`

**src/main/java/com/iksxh/create_nuclear_industry/config/RadiationConfig.java**仍被既有.gitignore:15 config/忽略，已在清单/编译/JAR中，本轮未变，由PM提交时force-add，执行者不改Git或ignore。

| 输入 | 字节数 | SHA256 |
| --- | ---: | --- |
| 初审候选（first-review-input/） | 3417633 | `9F78F1100D0EEAE6545B6A1A92B0416AB504CC081ED160D25B9CB53B0982B46A` |
| fix1候选（second-review-input/） | 3427684 | `6EEFAACBA5F86686016AEC72D4313B9870A52F3F4070FFC67E09BD973057593A` |
| fix2当前候选 | 3429128 | `AFAD58C122257112BD4BA05C275D86D30293AB5546F2D0531FFEDE5D38905817` |


当前候选路径 `E:/MyMC/NewMod/Create_NuclearIndustry-radiation/build/libs/create_nuclear_industry-0.1.0.jar`，详细身份candidate-review-fix-02.json。本批GameTest类/模板/域目录0条，配置类/索引/仪表资源存在；未提交来源的候选，不是main正式制品。fix1来源/报告/身份/原JAR实物备份second-review-input/，初审备份first-review-input/不变。

用户携带世界27文件/13337924 bytes与原清单逐项哈希一致，saved-world-after-verification.json记录，原saved-world-sha256.json未改。未启动客户端/用户世界。测试产生根logs保留给PM排除生产提交。最终源/制品/报告证据核验见review-fix-02-freeze-verification.json。

全部精确写集（相对候选根）：

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
