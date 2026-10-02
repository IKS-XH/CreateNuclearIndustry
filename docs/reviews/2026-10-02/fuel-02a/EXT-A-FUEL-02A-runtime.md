# EXT-A-FUEL-02A 运行侧交付

状态：运行、配方、注册及定向验证完成；美术冻结后的一次增量 `assemble` 已通过，停止本批代码/资源写入，等待只读审查。

## 实现范围

- 新增 `production/FuelSintering{State,BlockEntity,Block,Recipe,JeiPlugin}.java`、一项隔离 GameTest、一个定向 JUnit 类。注册范围只修改 `FuelProcessingContent`、`ModCreativeTabs`、`CreateNuclearIndustry` 的本批入口。
- 新增原生压片 `1低浓缩铀粉→1生芯块`、3×3 动力合成 `RSR/RBR/SIS`、专用烧结 `1生芯块→1熟芯块/400有效tick`。机器每 tick 从配方管理器核对专用配方 ID、输入、产物和工时；配方缺失或不合合同则不加工。
- 炉体水平 `facing` 与热源达标 `lit`；BE 唯一保存两槽数量及进度。顶面只投生料，四侧只取熟料，底面无物品能力。无 GUI、无轴/应力/流体。使用正下方 `BlazeBurnerBlock.getHeatLevelOf` 与 `HeatCondition.HEATED`，冷/阴燃拒绝、普通热与超热同速。
- 普通挖掘与潜行扳手回收一件带状态机器，查询掉落只构造快照，旧端口句柄拆除后失效。放置完成后读取携带快照。取空生料清工时；部分取料保留工时。物品放置失败不改写快照。
- 新增双语名称、护目镜输入/输出/热级/进度/等待原因、JEI 专用配方时间与最低热级提示、矿物标签及挖掘标签。GameTest 模板为原 109 字节 fixture 的逐字节副本，SHA-256 见证据。

## 实际验证

| 命令 | 退出码 | 结果 |
| --- | ---: | --- |
| `.\gradlew.bat test --tests '*FuelSintering*Test' --console=plain` | 0 | 4/4 JUnit；400 tick 一件、冷/满输出暂停恢复、部分/全部取料、快照和模拟不变性。XML：`EXT-A-FUEL-02A-runtime/junit.xml`。 |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_02a -PgameTestDirectory=build/test-worlds/fuel-02a --console=plain` | 0 | 1/1 required。真实 Create 燃烧室消耗煤、阴燃拒绝、原生漏斗顶投、400 tick 成品、侧面世界 capability 抽取与拒绝错误物品；FakePlayer 部分背包容量和满背包手工取料。`EXT-A-FUEL-02A-runtime/gametest-latest.log` 与 `gametest-console-result.txt`。 |
| `.\gradlew.bat assemble --console=plain` | 0 | 美术冻结后唯一一次增量装配，`BUILD SUCCESSFUL in 4s`；控制台全文 `EXT-A-FUEL-02A-runtime/assemble-console.txt`。JAR `build/libs/create_nuclear_industry-0.1.0.jar`，952795 字节，SHA-256 `877E9C41D8E9BC4CD05B0FFEE2C2AD412FC806A34E1802113530B6BBA865F0FE`；详情 `jar-sha256.txt`。 |
| `git diff --check` | 1 | 仅仓库跟踪的 `logs/debug.log`、`logs/latest.log` 因测试进程写入含尾随空格；运行侧任务写集未授权修改或恢复这些日志，由 PM Git 阶段排除/恢复。 |

首次 JUnit 编译因新 BaseEntityBlock 缺少 `codec()` 失败，补齐后定向通过；首次 GameTest 通过后，PM 指出原版 `Inventory.add` 的 boolean 只代表本轮放入过至少一部分。已以实际 remainder 计算接收量并在同一 GameTest 增加仅容一件与满背包断言，随后仅复验受影响 GameTest，通过。锁定版本 `neoforge-21.1.219-sources.jar` 的 `Inventory.add(int, ItemStack)` 返回表达式为 `p_36042_.getCount() < i`，确认部分放入可返回 true。

## 验证边界

- GameTest 的输出侧为真实方块 capability 取出，没有放置外部 Create 漏斗形成完整物流链；该外部物流入口及玩家生存挖掘/潜行扳手、携物重放、放置失败、原生压片与 9 格动力合成实机、JEI/护目镜及外观均留人工客户端核对。
- 进度每有效 tick 在服务端记脏，护目镜视图每 20 tick 或完成时同步，库存与热态变化即时同步。此次未测长时运行性能。
- 本批未更改 P1、离心机功能、构建脚本、依赖和版本；未执行全量、clean 或强制重跑。
- 装配 JAR 内核对本批 3 个物品模型、炉体 blockstate、3 项配方和隔离 fixture 均存在，索引见 `EXT-A-FUEL-02A-runtime/jar-entries.txt`；此检查不代替客户端视觉验收。

实际读取并应用的技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`。依仓库 MC 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 使用现有注册与 GameTest 入口，未套用技能较新版本示例。
