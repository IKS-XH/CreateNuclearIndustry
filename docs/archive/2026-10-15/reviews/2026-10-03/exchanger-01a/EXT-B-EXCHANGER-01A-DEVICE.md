# EXT-B-EXCHANGER-01A DEVICE 交付报告

- 实际目录：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；只读确认分支 `codex/ore-acquisition`，开工文档基线 `3f64678`。执行者交付未提交改动，不变更任务验收状态。
- 已实际读取：`AGENTS.md`、治理协议第1/4/5.1节、01A完整任务卡、已确认方案、API源码报告；技能为 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`。按锁定依赖应用 DeferredRegister/NeoForge能力/服务端BE及隔离GameTest，不照搬技能较新版本示例。
- 实际基线：Minecraft 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82 / Flywheel 1.0.6。未升级依赖、改构建、P1热工或旧设备。

## 实现

1. 独立热账本持有两个4000mB整数罐、实际HU储备、分数流量、运行阶段、上次额定HU/t和原gameTime。默认40 tick实际转换1440mB→1440mB，支付720HU后，下一个tick先支付18HU再发布18级并补36mB实际热量；锅炉查询与能力模拟全为只读。同一gameTime不重复结算。
2. 堵塞时仅转换冷罐真实可容纳部分，并按实际mB记热；少量液体不补满储备。分数流量始终小于1mB，失败不会累积整数突发债务。整mB高密度导致的储备上界尾热安全散失，不增加能量、不把余热延长超过配置窗口。配置改变仅限缩实际HU；无效密度、非有限值安全停机。
3. 保存/物品携带完整账本及原时间戳。恢复第一tick扣除停算期间按旧额定值对应的HU，再验证真实负载、支付当前tick。服务器彻底关闭时gameTime冻结，重启本身不消耗现实墙钟时间，也不会刷新余热；世界运行期间的卸载/物品搬迁会按流逝gameTime散热。时间回退不增热。
4. 负载只读上方Create控制器底层、原生引擎/汽笛、原生尺寸/供水限值；不以activeHeat判断启动。Create供水是近期十组采样的最大值，保留原生短时估计滞后，未加入额外水监控。额定耗液不随小锅炉限制下降。
5. 使用公开BoilerHeater.REGISTRY，活动18（数值等效9个超级燃烧室），停止-1；无工作盆热属性/被动标签。热态改变或移除先发布/关闭本机，再重算控制器。重扫只遍历已加载底面，避免原生updateTemperature读取整个底面时强制加载卸载区块。
6. Chunk Load只收集该事件已加载储罐的控制器，tick末一次刷新，清除控制器先于热源恢复时的持久化热缓存；未扫描世界。源BE卸载前关闭热和旧能力，刷新仍加载的跨区块控制器。能力epoch确保同一BE恢复时旧句柄也不能复活。
7. 朝向及lit仅用于本机外观；五面热入冷出、顶面不提供流体能力。正常采集用原版生存/创造规则，掉落快照保留账本；onRemove不追加第二台。潜行扳手先过BreakEvent再收取唯一快照。活塞阻止及Create构造移动检查已接线；无GUI。
8. 护目镜显示罐量、实际/额定流量、热级、已付储备tick、状态和原生尺寸/供水限制。客户端只同步显示，无结算。语言清单由材料执行者合入两种语言。

## 文件

- `src/main/java/com/iksxh/create_nuclear_industry/heat/`：`HeatExchangerState.java`、`HeatExchangerBoilerBridge.java`、`NuclearHeatExchangerBlock.java`、`NuclearHeatExchangerBlockEntity.java`。
- `content/HeatExchangeContent.java`；**`config/HeatExchangerConfig.java`被现有根config/规则忽略，最终须由PM精确追踪此文件，未改.gitignore。**
- `CreateNuclearIndustry.java`、`content/ModCreativeTabs.java`：仅新增设备/材料/配置/锅炉生命周期/创造页及禁止构造移动接线。
- `src/test/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerStateTest.java`；`gametest/ExtensionHeatExchangerGameTests.java`。
- `data/create_nuclear_industry_heat_exchanger/structure/p0_probe_empty.nbt`（材料共用）、`boiler_empty.nbt`（20×12×9设备用）。
- 单台设备loot及pickaxe、needs_iron_tool标签增量。
- 本报告及同名证据目录；`language-keys.json`提供材料执行者合入，不直接改语言文件。

## 验证证据

必要增量编译 `compileJava` 通过，见 `EXT-B-EXCHANGER-01A-DEVICE/compile.log`。

首次统一命令：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-21'
./gradlew.bat test --tests '*heat.*' runGameTestServer -PgameTestNamespace=create_nuclear_industry_heat_exchanger -PgameTestDirectory=build/gametest-heat-exchanger-01a assemble --console=plain
```

- `validation.log`：5个账本JUnit通过；9个GameTest中7个通过，2个失败，服务端完整正常退出code2。
- DEVICE失败根因：测试在NeoForge onLoad前立即fill，被合法拒绝后误称裸罐耗液。已改为onLoad后2tick注入，并断言实际accepted=2000；生产防御未放宽。
- MATERIAL失败根因/整改由材料报告记录：MechanicalCraftingInput构造前需calcStats。没有删断言或改配方掩盖失败。
- 保留首次JUnit原始结果 `junit-first.xml`（5 tests / 0 failures）。

补测只增加已审查疑点的两个JUnit方法，复用首轮旧5例证据；无便捷现成单GameTest方法过滤，因此按PM授权复跑本9例小namespace，没有运行旧全量：

```powershell
./gradlew.bat test --tests '*HeatExchangerStateTest.highDensityRoundingAndPartialReturnSpaceNeverCreditUnconvertedFluid' --tests '*HeatExchangerStateTest.smallerConfigurationAndDuplicateTickCannotCreateOrExtendHeat' runGameTestServer -PgameTestNamespace=create_nuclear_industry_heat_exchanger -PgameTestDirectory=build/gametest-heat-exchanger-01a assemble --console=plain
```

- `validation-retry.log`：新增2个JUnit通过；`All 9 required tests passed`；`BUILD SUCCESSFUL`。测试服完整正常关闭，没有退出停滞或清理任何进程。
- `junit-added.xml`：2 tests / 0 failures。合计7个不同账本用例，覆盖守恒/默认40tick边界/断供/无负载/堵塞/模拟/NBT原时间戳/无效配置/小流量量化/单mB高密度/部分冷罐空间/重复tick/配置缩小。
- DEVICE 4个真实GameTest：①自然形成4罐+真实引擎+能力供10mB/t水，热源18但尺寸/水各限1；公开查询100次及模拟纯度。②自然形成72罐+180mB/t水，18级和原生引擎效率，随后真实供水降至10mB/t，采样稳定后水限1但仍额定36mB/t。③裸罐不耗液、真实生存铁镐破坏仅掉一台，携物快照完全相同且恢复未验证前热=-1。④控制器与源实处不同区块，通过真实BE onChunkUnloaded/onLoad生命周期断言缓存立即清零、旧句柄在恢复后仍拒绝。
- GameTest未直接写入锅炉activeHeat/水采样字段。Bridge实现重算缓存不属于测试伪造热量。
- 变更范围定向 `git diff --check` 通过。JUnit自身改写了仓库已跟踪`logs/debug.log`/`logs/latest.log`，此为框架输出，未手动编辑/清理；交付请PM排除。全仓diff-check因此出现日志尾空格，不是源码差异错误。

## 未验收边界

- 第④项是**跨真实区块位置的BE生命周期等效测试**，不是由chunk ticket驱动的真正整区块卸载。真实离开加载范围、控制器/source不同加载次序、保存退出重启仍列入人工恢复清单，不能冒称已证明完整磁盘重载。
- 客户端模型/护目镜/JEI、真实反应堆热液→Create锅炉发电→冷液回流、手工堵塞/断供/扳手/多人体验尚未人工验收。
- 没有接工作盆或专用蒸汽，不宣称完整EXT-B-API-01通过，不改任务状态、不合入主目录。
- 未启动客户端、触碰用户存档、`.vscode`或旧`__pycache__`，未执行任何Git写操作。等待PM统一独立复审及人工门。

## 独立复审 P1 整改：已加载但停止方块 tick

复审发现初版只处理卸载不足以封闭生命周期：`Level.tickBlockEntities` 检查 `shouldTickBlocksAt`，`LevelChunk` 内部ticker另检查 `FullChunkStatus.BLOCK_TICKING` 和实体加载。源区块仅保持FULL但不tick时，源不扣HU而邻区块锅炉可能继续使用旧缓存。已实际读取锁定1.21.1源码确认，相关源码只提取在本证据目录。

最小生产修复：

- `HeatExchangerBoilerBridge.ACTIVE` 弱集合只记录本模组已经发布热的源，ServerTick Pre/Post检验其实际原生tick门。失活时撤销发布、重算已加载关联锅炉并移出集合；源移除/卸载也清理。不扫描全部机器/锅炉或世界。
- 回调及Port直接检查距离、FullStatus、实体加载及世界边界门，停tick时立即拒绝新的热或库存修改。恢复后仍经原BE tick和保存的lastTick结算停算期间散热，不免费刷新。
- 暂停/恢复都递增capability epoch并调用`level.invalidateCapabilities`。这使旧句柄永久失效，同时允许Create的真实BlockCapabilityCache恢复获取新的可用句柄，避免仅失效epoch导致管道永久断开。
- 热账本未改变，因此复用此前7个不同JUnit证据，没有再跑JUnit。

定向验证过程与证据：

1. `validation-liveness.log`：旧9例继续通过，新增夹具第一次失败。此时单次将TickingTracker结果置33不足以维持原生门。
2. `liveness-diagnostic.log`：记录开始源`oldLevel=31 → sourceLevel=33/sourceTick=false`且控制器`controllerTick=true`，50tick后原生距离传播把源改回`31/sourceTick=true`，账本因真实tick而变化。确认是测试夹具未固定门，没有削弱生产守卫或删除断言。
3. 按PM授权采用较小稳定夹具：公开`LevelChunk.setFullStatus(() -> FULL)`封闭真实内部BE ticker，保留同一真实区块/实体和跨区块锅炉；反射只读保存旧supplier，在finally中精确恢复。没有生产测试开关、没有直接调用撤热函数或写锅炉热值。
4. 新增 `fullButNonTickingSourceRevokesHeatAndCapabilityCacheRecovers` 在独立batch稳定50tick保持FULL，断言账本完全不变、真实锅炉缓存0、回调-1、旧Port拒绝、真实BlockCapabilityCache已经失效。恢复后断言旧余热已散失、新缓存端口可用且旧Port不复活，继续真实补液后重新完成有偿预热并供18级。
5. `validation-liveness-fixed.log`：**All 10 required tests passed；assemble BUILD SUCCESSFUL**，测试服完整正常退出。最终相关GameTest为DEVICE 5例+MATERIAL 5例；首次9例记录保留其历史语境。

最终补测命令：

```powershell
./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_heat_exchanger -PgameTestDirectory=build/gametest-heat-exchanger-01a assemble --console=plain
```

证据边界：新增用例是真实区块/BE上的**受控原生FullStatus门**，不是自然玩家移动或chunk ticket迁移，更不是整区块磁盘卸载。自然距离票据迁移、实际离开区块范围及服务器重启仍保留在人工恢复清单。修复与补测已交PM作本次差异复审，执行者不自行宣告验收通过。
