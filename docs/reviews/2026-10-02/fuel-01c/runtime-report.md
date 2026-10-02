# EXT-A-FUEL-01C 运行线交付

候选工作树 `Create_NuclearIndustry-ore-acquisition`，基线 `de2e0d150193cfce0664312ab4f80158c20923f1`。按本卡实现了离心机无菜单交互、底部唯一Y轴动力和五面共享物料端口；五面由 Create `SidedFilteringBehaviour` 原生行为提供独立槽，过滤身份实时约束物品/流体读写，禁止物品插入，过滤随原生行为NBT和机器便携快照保存。缺失旧便携过滤字段会先清空五面过滤。旧处理批次/库存/磨损状态结构未改。能力句柄在移除及 `level.getBlockEntity(pos) != this` 时失效，包括同型方块实体替换。过滤框命中先于桶/维修/取料；桶命中过滤槽时在此消费，以免回落到原版倾倒。空手在非底面过滤框外由 `useItemOn` 显式收取，并保留 `useWithoutItem` 路径；未打开菜单。

删除 `CentrifugeMenu`、`CentrifugeScreen`、菜单注册和菜单专属测试；护目镜保留。资源线由另一执行者修改桶及方块模型，静态交付见 [资源报告](./assets-report.md)。

定向测试最终命令见 `runtime/targeted-test-command.txt`，最终输出见 `targeted-test-final.log`，JUnit原始XML副本在 `runtime/test-results/`。三类共7/7通过：`CentrifugePortsTest` 3项（注册BE/五面与底面、缓存物品能力随原生过滤变化、便携过滤快照和旧字段清空），`CentrifugeSlurryBucketTest` 2项，`CentrifugeContainerTransactionTest` 2项。XML时间为 `2026-10-02T08:28:47Z`。

测试期间有一次真实边界发现：不带 `Level` 的JUnit夹具调用Create原生流体过滤时，`FilterItemStack.resolveFluid` 会经配方查询访问空世界并抛NPE。没有扩建世界/玩家模拟框架；因此最终自动用例覆盖真实注册BE、五面物品过滤/端口及过滤快照，Create流体过滤的实际 `Level` 匹配留给本批客户端验收。测试早期编译也校正了流体过滤必须调用每面 `FilteringBehaviour.test(FluidStack)`、而非父类物品重载。桶容器交易自动用例不替代过滤后的实际桶放置检查。

随后仅运行一次增量打包：`.\gradlew.bat assemble`，`BUILD SUCCESSFUL`，输出保存在 `runtime/assemble.log`。JAR `build/libs/create_nuclear_industry-0.1.0.jar` 中已核对动态桶模型、离心机模型存在且字段符合资产报告；`CentrifugeMenu.class` 和 `CentrifugeScreen.class` 均不存在。没有启动游戏或GameTest。编译输出仅有既存 `FluidType.initializeClient` deprecated/removal警告。

未自动证明真实Level上的粉末/桶管网行为、过滤原生界面渲染/设置/清空和与空手/维修的实际点按优先级，也未动态替换世界中的同型BE；这些保留在本卡人工清单。根目录 `logs/latest.log`、`logs/debug.log` 被本次JUnit运行更新，按交接要求保留供项目经理处理。

技能：按任务卡读取并应用 `minecraft-modding`、`minecraft-testing`；已按本工程锁定版本Create源码核对侧向原生过滤行为及其物品/流体匹配API，没有升级依赖。验证范围遵从治理§5.1：只跑关联JUnit和一次增量assemble，无clean、全量测试、GameTest或客户端启动。

增量打包的准确命令：

```powershell
.\gradlew.bat assemble
```



## 复审定点整改

限定复审指出的两项P2已修复。普通非过滤框的 `BlockItem` 在 `useItemOn` 返回默认交互后，`useWithoutItem` 现在只有主手为空才收粉；主手非空时明确继续 `PASS`，因此管道/漏斗放置以及扳手的后续 `Item.useOn` 不会被空手取粉抢占。过滤框、过滤桶保护、维修、桶事务和空手直取分支保留。

过滤槽改用 `CentrifugeFilterSlotTransform`（Create `ValueBoxTransform.Sided`），放在每个允许面上缘角落 `(1, 15, 15.5)` 像素位置，DOWN不显示。移除了 `forFluids()`，让共享桶/粉的过滤槽保持Create中性原生“Filter”标签；物品与流体仍各自走原生匹配重载。`CentrifugePortsTest` 新增五方向值框位置/命中半径避开管道中央 4..12 像素截面的断言。

本次只复跑受新Transform/过滤改动影响的 `CentrifugePortsTest`，最终4/4通过，XML、准确命令和最终终端日志在 `runtime/review-fixes/`。第一次几何断言失败是测试把Create `VecHelper.voxelSpace` 坐标误按以中心为原点计算；按其0..1方块局部坐标修正距离计算后通过。先前7项既有证据未重跑。

复审后的唯一增量打包命令仍为 `.\gradlew.bat assemble`，`BUILD SUCCESSFUL`；输出在 `review-fixes/assemble.log`。JAR含新的 `CentrifugeFilterSlotTransform.class`，保留两个模型资源，且不含菜单/屏幕类。没有运行客户端/GameTest；BlockItem安装、扳手和过滤框事件顺序的实际客户端检查继续列入人工清单。
