# EXT-A-FUEL-01A 运行与流体修复交付报告

- 候选基线：`6b2138a4dd7c54954e4dc0a42e8d3a4e637687ec`（`codex/ore-acquisition`）。
- 范围：修复离心机菜单输出槽同步、料浆原生桶/Create 装桶接入，并核查资源包；未改配方产率、其他生产算法、依赖、PNG 或第三方 Create 代码。
- 实际技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`（核对注册、NeoForge/Create 1.21.1 接入边界）；`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`（核对目标测试和自动/人工证据边界）；`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/systematic-debugging/SKILL.md`（沿异常与流体调用链核实根因）。锁定版本仍为 MC 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82。

## 修复内容

- `CentrifugeMenu.OutputSlot` 在客户端持有菜单同步的显示副本；服务端继续从只读输出端口取物。外部插入仍被拒绝，常规取出和 Shift 取出仍只通过服务端提取。
- 原菜单崩溃的根因是 NeoForge `SlotItemHandler.set` / 初始化路径把只读 `OutputHandler` 强转为 `IItemHandlerModifiable`；手测报告中的 `container_set_content` 栈追踪已定位到该类型假设，不是网络连接问题。
- 料浆现使用真实注册的标准 `BucketItem`、源/流动态、`LiquidBlock` 和原有 FluidType 客户端纹理扩展；Create 的 `GenericItemFilling` 测试直接使用注册料浆和原生空桶。移除了 `SlurryBucketItem` 与专为旧自制桶补的能力注册。工作盆/管道和离心机以同一源流体身份工作；放置/舀回遵循原生桶行为。冷却剂同样以 FluidType 纹理扩展和 atlas 接线渲染，没有独立方块状态/方块模型，本次保持该实现模式。
- 原生 Create 创造模式取桶分支会消耗所取流体但不返还满桶；这是 Create 原生行为，本次没有改动。数量/返桶验收以生存模式并检查玩家背包为准。

## 自动验证证据

前执行者于 `2026-10-02T07:33:58Z`（上海时间 15:33:58）执行定向 `test`，`targeted-test-final.log` 记录 `BUILD SUCCESSFUL`。三份原始 XML 均为 0 skipped / 0 failures / 0 errors，共 6/6：

- `build/test-results/test/TEST-com.iksxh.create_nuclear_industry.production.CentrifugeContainerTransactionTest.xml`：2 项，覆盖整桶数量、错误容器、容量不足和守恒边界。
- `build/test-results/test/TEST-com.iksxh.create_nuclear_industry.production.CentrifugeMenuOutputSlotTest.xml`：2 项，覆盖真实菜单槽的客户端初始化同步，以及服务端只取与禁止写入。
- `build/test-results/test/TEST-com.iksxh.create_nuclear_industry.production.CentrifugeSlurryBucketTest.xml`：2 项，直接使用注册的真实料浆、标准桶和 Create `GenericItemFilling`，覆盖 1000 mB 填桶、NeoForge 桶能力、LiquidBlock 对应关系及整桶事务边界；没有用水桶替代料浆验证。

原完整 Gradle 命令行没有保存在前执行者日志中，不将推测命令记为已运行。等价的定向复现写法为：`gradlew.bat test --tests '*CentrifugeContainerTransactionTest' --tests '*CentrifugeMenuOutputSlotTest' --tests '*CentrifugeSlurryBucketTest' --max-workers=1 --console=plain`；这是复现用法，本次没有再次运行这些测试。

接手后仅运行 `.\gradlew.bat assemble`，结果 `BUILD SUCCESSFUL`，1 个任务执行、3 个任务为 `UP-TO-DATE`。未运行测试任务。生成物为 `build/libs/create_nuclear_industry-0.1.0.jar`。JAR 检查确认包含精矿与离心机两个物品模型、离心机方块模型、方块 atlas、料浆 still/flow 贴图及料浆桶物品模型；逐项 SHA-256 与 Gradle 处理后资源一致。相关 JSON 均能解析。

资源线的静态模型核对见 `build/reports/extension/EXT-A-FUEL-01A-models.md` 及其 `EXT-A-FUEL-01A-models/verification.json`：包括精矿引用、六面贴图、父链和原版展示变换。此报告不替代实际客户端观感。

## 未完成的人工门

Create 工作盆/储罐实际装桶、玩家背包返桶、世界放置与舀回尚未在客户端验证；自动测试调用的是 Create `GenericItemFilling`，不能证明完整工作盆交互和客户端体验。仍需按五项复测清单在生存模式检查：（1）精矿模型正常显示；（2）空手打开离心机不崩溃；（3）GUI 中离心机显示为正确立体视角；（4）第一/第三人称手持比例正常；（5）Create 工作盆将料浆灌入空桶、背包获得料浆桶，并能放置和舀回；另确认储罐路径与同一流体兼容。创造模式的原生取桶差异不作为失败。

本次 JUnit 使候选根目录 `logs/latest.log` 和 `logs/debug.log` 相对基线变脏。按交接要求未还原/覆盖日志；用户客户端 PID 28352 及其他既有 Java 进程均未关闭。

执行者仅提交本报告及任务写集内未提交代码/资源，未执行 Git 写操作；交付后等待一次限定独立复查。最终验收及主线状态由项目经理处理。
