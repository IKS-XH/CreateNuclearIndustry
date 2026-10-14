# EXT-B-EXCHANGER-01C-FIX：共享流体接收计划

日期2026-10-04；候选`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；基线`3ea9e89`。执行者代码已交付，待唯一Gradle执行者统一验证；不代表修复验收或客户端闭环已经通过。

## 已证实根因

修复前真实Create泵管三冷端用例在32tick后由源4000/冷9000/热0，总13000mB，变成源928/冷12000/热0，总12928mB；损失72mB。日志同时证明三支路完整出流，单端对照总13000mB保持不变。本执行者实际读取了`EXT-B-EXCHANGER-01C-REPRO/gametest-console-delayed-branch-flow.log:58-71`；该运行由REPRO执行者完成，不冒称本执行者重新运行。

锁定Create6.0.10的`FluidNetwork.tick`只按handler对象累计模拟接收。多个本模组handler共享库存或同一物理口预算时，模拟重复许诺空间；实际源已被抽取，目标实际接收不足的余量被原生管网丢弃。详细源码与保存拓扑见`EXT-B-EXCHANGER-01C-CONSERVATION.md`。

## 实施内容与精确文件

1. `src/main/java/com/iksxh/create_nuclear_industry/compat/create/SharedFluidReceiver.java`：本模组显式接收口接口；只读Limits给出库存owner身份/剩余空间、物理预算身份/剩余配额，单位mB。
2. 同目录`SharedFluidFillPlan.java`：只在单次同步模拟中存在的纯Java计划。按handler、库存owner、物理预算三层对象身份累计；每次重分配先扣除当前handler旧份额，再计算新累计接受量。
3. `src/main/java/com/iksxh/create_nuclear_industry/mixin/FluidNetworkSharedFillMixin.java`：仅包装`FluidNetwork.tick()V`唯一`IFluidHandler.fill`调用点。原调用恰好执行一次；仅显式接口的SIMULATE收窄接受量，EXECUTE/外部handler返回原结果。
4. `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorCoolantFluidHandler.java`：仅实现接口并增加只读Limits；使用已有owner及flowBudget身份。既有fill/drain/快照记账没有修改。
5. `src/main/java/com/iksxh/create_nuclear_industry/heat/NuclearHeatExchangerBlockEntity.java`：仅Port实现接口，五面共享ledger热罐剩余空间。既有epoch有效性、fill/drain和热工不变。
6. `src/main/resources/create_nuclear_industry.mixins.json`：required、Mixin0.8.7/MixinExtras0.5.0、JAVA_21及唯一Mixin类；目标失配要求显错。
7. `src/main/resources/META-INF/neoforge.mods.toml`：仅末尾追加上述Mixin配置。
8. `src/test/java/com/iksxh/create_nuclear_industry/compat/create/SharedFluidFillPlanTest.java`：7个算法边界用例。

没有修改REPRO独占真实测试类/模板，没有修改构建脚本、依赖版本、注册、配置、热工、流量配额或核心文档。既有PM文档改动、用户pycache保留；未操作用户客户端/存档，未执行Git写操作。

## 保留的行为与兼容边界

- 多个冷端共享库存，但不同flowBudget仍各自有128mB/t上限，可合计大于128；同一物理口多个面不重复出售剩余配额。
- 普通能力SIMULATE仍走原handler纯读路径。计划对象不写入BE、静态字段或ThreadLocal；`@Share LocalRef`每次方法调用独立，异常/早退均不留下跨调用状态。
- 原生唯一SIMULATE阶段内部while重新分配共用计划。源drain回退在目标分配之前，不会重复开启计划；EXECUTE首次目标fill后清空局部引用并原样透传。下一网络调用/下一tick天然新计划。
- 原生/外部handler不实现本接口，其fill返回值和调用次数保持不变；本修改不是通用第三方共享库存修复。
- 既有NBT及物品数据没有变更，没有新增存储库存、补液或热级配置。
- `require=1,expect=1,allow=1`及required配置用于发现锁定目标发生变化，不用静默可选注入绕过守恒问题。

## 算法测试意图

7个测试分别覆盖三口共享36mB空位、累计重分配与重复查询、同口多面128上限、两个不同口合计256、值相等但不同身份的owner/预算/handler隔离、新网络作用域不残留、换热器多面共享64空位且不扩大原模拟接受量。

测试先于本轮新增生产算法写入；真实三冷端失败是本次修复的已验证RED证据。新增JUnit尚未运行，遵从PM要求交REPRO一次统一验证，不为满足通用技能而另启动Gradle或全量回归。

## 技能与验证状态

已应用`minecraft-modding`、`minecraft-testing`、`systematic-debugging`；本轮另外实际读取`test-driven-development`及`writing-good-tests.md`、`executing-plans`、`verification-before-completion`。全部受项目治理第5.1节、执行者Git禁令和PM唯一测试运行规则约束，不采用通用技能的额外全量/提交/转派流程。

锁定Minecraft1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82。MixinExtras0.5.0已由现有NeoForge提供，未添加依赖。

本执行者已完成静态检查：`git diff --check`退出0（仅工作区既有LF→CRLF提示）；Mixin JSON经PowerShell `ConvertFrom-Json`正常解析；逐文件差异核对确认旧fill/drain没有改动。没有运行Gradle，尚无本轮编译、JUnit或真实GameTest通过声明。

下一门由PM通知REPRO统一执行：新增计划JUnit、相关旧端口预算/handler用例、修复前失败的三冷端用例及单端对照、原生目标透传/换热器多面代表用例、增量assemble。测试异常由实现执行者按具体证据整改；人工真实闭环复测另立证据，不能被自动通过代替。
