# BOILER-SIZE-LIMIT-01 实施报告

状态：DONE（执行者范围实现、验证、打包与冻结完成）；待PM独立窄审及本批用户客户端手测，不作客户端验收。

## 来源与实际使用技能

- 唯一工作目录：`E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`，基线 `edb5b653c51867df26a2f16030f5ed52cf1923f3`，分支 `codex/reactor-variable-size`。执行者无 Git 写操作、核心文档修改或子代理派发。
- 已阅读根 `AGENTS.md`、活动任务简报 `.superpowers/sdd/boiler-size-limit-01/task-1-brief.md`。
- 实际使用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 和 `minecraft-testing/SKILL.md`，核对现有 NeoForge 注解注册、空模板、真实能力/扫描读取链，不照搬新版本目录或 API。
- 实际使用 superpowers `test-driven-development/SKILL.md`、其 `writing-good-tests.md` 和 `verification-before-completion/SKILL.md`：生产改动前先观察真实行为 RED，容量与溢出期望独立手算，完成结论绑定命令退出状态。按仓库现行窄范围验证规则不重复全量 build/已验收矩阵。
- 实际环境：MC 1.21.1、Java 21.0.7、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82；依赖与构建脚本未变。

## 实现与写集

- `src/main/java/com/iksxh/create_nuclear_industry/config/BoilerConfig.java`：默认 `[5,11]` 改 `[5,16]`，单元素合法上界 32 改16，中文配置注释同步。原跨字段快照校验继续要求恰好两个整数和有序区间。
- `src/main/java/com/iksxh/create_nuclear_industry/boiler/BoilerState.java`：`DEFAULT` 最大16，`Settings.valid()` 拒绝上限17及以上；原 long 分区容量/阀泄放乘积预检保持逐字。
- `src/test/java/com/iksxh/create_nuclear_industry/boiler/BoilerStateTest.java`：新增默认一致、配置17元素拒绝、运行17拒绝及交易阻断、1372/1176格扩容不增工质/HU；原范围校验改用17。原溢出断言在合法最大16下分别核水容量、汽容量、阀额度；最坏分区2352格乘1000000为2352000000，超过 `Integer.MAX_VALUE`，不依赖32尺寸自身非法而通过。
- 新增 `src/main/java/com/iksxh/create_nuclear_industry/gametest/BoilerSizeLimitGameTests.java`：复用正式锅炉搭建法，隔离域两项用例核16³实际跨X/Z区块及17×16×16越界，再委托原11格偏心用例组成最终三项；无测试配置赋值、无强载票据、无新世界加载逻辑。
- PM同批扩卡批准 `src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionBoilerGameTests.java` 仅 `elevenBoundaryAndOffCenterPartition` 方法：原11格参考、162水格/486汽格断言保持；非法宽度12→17，中文提示相应更新。全文件与HEAD仅三处精确文本替换（统一行尾后），其他方法逐字保持，见 `legacy-scope-check.txt`。
- 新增 `src/main/resources/data/create_nuclear_industry_boiler_size/structure/boiler_size_empty.nbt`：逐字复制原 `create_nuclear_industry_boiler_rework/structure/boiler_empty.nbt`，SHA256均 `A46FBCA10B8C94CD0228BD493BF5F1D19787EC034047715DB6AC40B9AB211941`。
- 扫描器、显示协议的工程32边界、普通/超临界库存、热工、压力、端口限流、配方与美术均未改。

## 自动证据

证据目录：`build/reports/boiler-size-limit-01/`。

|阶段|实际命令/结果|证据|
|---|---|---|
|生产前RED|`./gradlew.bat test --tests '*BoilerStateTest' --console=plain`，exit1，18项中3项按预期失败：配置默认11、运行17有效、17非法范围断言|`01-red-unit.log`|
|配置元素RED|`./gradlew.bat test --tests '*BoilerStateTest.configDimensionElementsRejectSeventeen' --console=plain`，exit1，1项失败于真实 ValueSpec 接受17|`02-red-config.log`|
|最小GREEN|`./gradlew.bat test --tests '*BoilerStateTest' --tests '*BoilerDualSteamInventoryTest' --console=plain`，exit0，19+10=29项，零失败/错误/跳过，20秒|`03-green-unit.log`、`junit-green/`|
|隔离真实服务端|`./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_boiler_size -PgameTestDirectory=build/gametest-boiler-size-limit-01 --console=plain`，实际注册2项、2项必需通过，保存所有维度后自然exit0，19秒|`04-gametest.log`|
|初次增量assemble|`./gradlew.bat assemble --console=plain`，exit0，1秒，仅jar执行|`05-assemble.log`|
|扩卡后的最终真实服务端|同一隔离域与目录命令，实际注册3项、3项必需通过，保存所有维度后自然exit0，20秒；原29项源码未变复用原证据|`06-final-gametest.log`|
|最终增量assemble|`./gradlew.bat assemble --console=plain`，exit0，1秒，仅jar执行；为新增委托/旧边界断言class更新，实际第二次打包|`07-final-assemble.log`|

16³实际隔层localY=8：截面14×14、水区7层1372格、汽区6层1176格。Form、账本和真实水/汽端口容量分别为2744000mB和2352000mB；账本 Settings 等于服务端加载的配置快照。两轴跨区块，强载区块集合前后相同。空炉实际库存与HU均为0。17格宽度在同一有效默认16配置下以 `bounds` 拒绝。

运行日志出现现有弃用 API/Gradle、Mixin refmap、assets URL 等警告，以及独立新目录首次 `server.properties` 不存在的加载日志；服务端随后正常运行并自然保存退出，不将日志写成完全无警告。未运行客户端，自动证据不替代手测。

## 候选运行配置

主PM明确放行后，完整备份 `run/config/create_nuclear_industry-boiler.toml` 到证据目录 `boiler-config-before.toml`；只改第2行 `dimensionRange = [5, 11]` 为 `[5, 16]`。文件长度不变，唯一字节差异 offset109 从49（1）到54（6），其余全部逐字保持。

- 原配置SHA256：`817FB9B0E2163A1AAF21BEC2D29FE188BC7CD7BFC7A40268570C21305259C891`。
- 当前配置SHA256：`DBE002F800AE2DB1E759A2952B5F9307DB5A136B9FCD6B2E5FF29C9DD9795B58`。
- 证据 `config-sha.txt`、`config-byte-diff.txt`。独立GameTest在build专用新目录运行，无读取/改写候选运行文件以掩盖默认的做法。
- 未读取、复制、覆盖或写入 `run/saves/` 用户世界；仅独立GameTest自己的 `build/gametest-boiler-size-limit-01/world` 由服务器生成和保存。

## 最终冻结及交付边界

初次assemble前已完整备份原接受组合JAR为 `accepted-input.jar`，SHA256 `15C0AB893D080C6AA93C11F05E5E09EF55931979BD4FC8693AD502802DB14DDD`。中间候选JAR SHA256 `FF18B2A437AEE536F4EB37476991E25A18D639D3D8998964F1ACABDC899C12DD`。

中间候选身份保留在 `intermediate-freeze/`；最终 `build/libs/create_nuclear_industry-0.1.0.jar` 与证据目录 `boiler-size-limit-candidate.jar` SHA256均为 `72F2BB2B197B1441D7F72C1E0F8E43FD9114FCBF3F47C71323AD44AEE003227D`。

最终原1925条目中1921条目逐字一致，仅上述三个生产class和 `ExtensionBoilerGameTests.class` 四项变化；新增本批GameTest class及专属空模板两项。变化/新增的6条均与build输出SHA一致。`source-before.json` 的3966个既有src/tools文件仅两个生产源及获批原GameTest源三项变化；原JUnit改动先于该保护快照，最终差异另外由Git只读diff核对。详见 `freeze-summary.json`、`jar-entry-changes.csv`、`build-jar-identity.csv`、`source-identity.csv`。两个生产class及Settings在中间与最终候选完全相同，原29项证据仍绑定同源生产行为。

全过程确实执行两次assemble：首次在发现旧断言后的暂停消息抵达前已完成；按PM随后写入计划的精确补充，第二次仅为原边界断言和新增委托class的必要打包。原2/2和首次assemble明确仅为中间证据，最终采用3/3和第二次assemble。未重跑29项或已验收矩阵。

最终候选仍须PM独立窄审和用户16³/17拒绝、尺寸显示/容量手测。本报告不修改治理任务状态，不替用户验收；源码未提交，未合main，用户世界来源仍由PM与用户决定。
