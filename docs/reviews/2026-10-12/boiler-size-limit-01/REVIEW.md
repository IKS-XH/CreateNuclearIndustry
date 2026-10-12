# BOILER-SIZE-LIMIT-01 独立规格与质量窄审

结论：**未发现 Critical / Important 必改项，Minor 可选项亦无。** 本批源码、测试补充和冻结制品符合活动卡及其末尾扩卡，可由PM接续候选登记；本结论不关闭用户16³/17拒绝、尺寸显示与容量的客户端人工门，不构成合入main授权。

## 审查身份、依据与范围

- 日期：2026-10-12。唯一审查树：`E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`；实际HEAD与生产基线均为 `edb5b653c51867df26a2f16030f5ed52cf1923f3`，审查工作区未提交差异与新增文件。
- 实际阅读 `AGENTS.md`、`docs/superpowers/plans/2026-10-12-boiler-size-limit-01.md` 全文及末尾扩卡、`IMPLEMENTATION.md`；应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md` 以及 superpowers 6.4.2 的 `requesting-code-review/SKILL.md` 与 `code-reviewer.md`。本席即独立审查执行者，无PM、Git写操作、源码修改及子代理权限。
- 技能示例按实际 `gradle.properties:6-12`、`build.gradle:13` 核对为MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82；未采用技能中的更新版本示例或通用重复构建流程。
- 生产仅 `config/BoilerConfig.java` 与 `boiler/BoilerState.java`；测试为原 `BoilerStateTest.java`、新 `BoilerSizeLimitGameTests.java`、独立域空模板，以及扩卡限定的原 `ExtensionBoilerGameTests.elevenBoundaryAndOffCenterPartition`。
- 使用只读Git差异、现有日志/XML、SHA256、内存读取ZIP条目核对；没有启动Gradle测试、构建或客户端，没有操作用户世界。本席唯一写入为本报告。

## 规格与质量核对

1. **默认和合法上界一致。** `BoilerConfig.java:20-21` 的默认为 `[5,16]`，配置元素仅允许5至16整数；`BoilerConfig.java:51-56` 的原快照入口仍把非双元素列表置为非法，`BoilerState.java:23-25` 继续拒绝低于5、逆序与大于16的范围。`BoilerState.java:44-45` 的独立默认为5至16。配置可提高下限或降低上限，无立方体、奇数或区块对齐限制。
2. **原算术安全合同保持。** `BoilerState.java:33-38` 的long最坏分区容量/阀额度预检在Git差异中逐字未变。合法最大16的最坏单分区为 `14×14×12=2352` 格，乘 `1000000` 为 `2352000000`，超过int上限；`BoilerStateTest.java:162-166` 分别让水、汽、阀乘积超界，不再依赖尺寸32本身非法来得到通过。默认2000mB/格与32mB/t/格在合法范围内。
3. **扫描读取正式配置，实际几何与能力相符。** 原 `BoilerStructure.java:108-135` 从 `BoilerConfig.settings()` 获取范围，逐轴核验尺寸并先检查区块可用性；`BoilerStructure.java:152-178` 按真实水汽区空气格计数，未改扫描器。原 `BoilerControllerBlockEntity.java:168-172,301-304,334-335` 将同一配置及Form格数用于账本与能力容量。新 `BoilerSizeLimitGameTests.java:29-50` 验证16³、X/Z两轴跨区块、隔层localY=8、1372水格/1176汽格、2744000/2352000mB、真实端口、空炉工质/HU为0及强载集合不变；`:56-64` 在有效默认16下验证17宽度因 `bounds` 拒绝。
4. **原规则保留，扩容不产生质量或能量。** 两个生产路径差异仅默认与尺寸校验/配置注释；热工、双汽库存、每口流量、隔层和端口规则没有生产改动。`BoilerStateTest.java:37-43` 在已有水/汽/HU状态扩容后核库存和HU不变；原双汽10项定向证据继续保留。现有扫描三轴独立处理，支持原长方体、偶数和偏心隔层；本批不重复旧矩阵。
5. **扩卡严格受限。** 原 `ExtensionBoilerGameTests.java:73-81` 保留11格参考和162/486偏心分区断言，仅提示“11边界”改为“11格参考结构”、非法宽度12改17、非法提示12改17。独立读取基线全文并应用这三处预期替换后，与当前全文逐字一致（统一行尾）；其余方法保持。新 `BoilerSizeLimitGameTests.java:22-25` 委托该方法，最终独立域包含该回归。
6. **注册与模板对应真实运行。** 新类采用仓库现行 `@GameTestHolder` 与 `@PrefixGameTestTemplate(false)`；模板路径为本版本单数 `structure/`。专属空模板与正式空模板SHA256均为 `A46FBCA10B8C94CD0228BD493BF5F1D19787EC034047715DB6AC40B9AB211941`；最终日志实际注册3项，非零注册证明了现有NeoForge发现链。

## 原始自动证据

以下均位于 `build/reports/boiler-size-limit-01/`，本席只读复核，没有重跑。

|证据|读取到的结果与边界|
|---|---|
|`01-red-unit.log:18-29,48`|生产前18项中3项失败，分别为默认16、Settings拒绝17与原范围断言；BUILD FAILED，与实现报告的RED exit1一致。|
|`02-red-config.log:14-17,38`|真实ValueSpec拒绝17的新增断言在原生产上失败；BUILD FAILED。|
|`03-green-unit.log:50` 与 `junit-green/TEST-*.xml:2`|BUILD SUCCESSFUL；BoilerStateTest为19项、BoilerDualSteamInventoryTest为10项，各自失败/错误/跳过均0，合计29/29。|
|`04-gametest.log:86-108`|中间候选实际2项、2项必需通过，随后保存所有维度并关闭服务器，BUILD SUCCESSFUL in 19s；仅为中间证据。|
|`05-assemble.log:6-10`|第一次增量assemble，jar执行，BUILD SUCCESSFUL in 1s；保留为中间候选。|
|`06-final-gametest.log:58-82`|最终实际3项、3项必需通过；16³格数/容量/跨区块与17拒绝打印与断言相符，随后保存所有维度、服务器自然关闭，BUILD SUCCESSFUL in 20s。与实施记录exit0一致，不存在把断言通过后挂在Saving worlds写成进程成功的情形。|
|`07-final-assemble.log:6-10`|扩卡后的第二次必要增量assemble，jar执行，BUILD SUCCESSFUL in 1s；最终制品采用本次结果。|

最终日志仍有现有Mixin/refmap、命令歧义、assets URL等警告；未见本批最终测试失败。实施报告如实区分两次assemble、中间2/2与最终3/3，未称整个过程唯一打包。两个生产class及Settings在中间/最终JAR逐条SHA相同，因此29项JUnit证据仍绑定同源生产行为，无需重复运行。

## 冻结与候选配置复核

- 独立计算当前 `build/libs/create_nuclear_industry-0.1.0.jar` 与冻结 `boiler-size-limit-candidate.jar` 的SHA256，均为 **`72F2BB2B197B1441D7F72C1E0F8E43FD9114FCBF3F47C71323AD44AEE003227D`**；原接受输入包为 `15C0AB893D080C6AA93C11F05E5E09EF55931979BD4FC8693AD502802DB14DDD`。
- 独立在内存读取两份JAR文件条目、排除ZIP目录条目：原1925、最终1927；1921条逐字相同，变化仅 `BoilerState.class`、`BoilerState$Settings.class`、`BoilerConfig.class`、`ExtensionBoilerGameTests.class`；新增仅新GameTest class与独立域空模板。与 `freeze-summary.json`、`jar-entry-changes.csv` 一致。
- `source-identity.csv` 六项当前SHA全部匹配；`build-jar-identity.csv` 六项全部独立复核为清单SHA＝当前build输出SHA＝最终JAR对应条目SHA。`source-before.json` 的3966既有文件逐项复算，仅两个生产源与获批旧GameTest源三项变化；原JUnit先于该快照，已另用基线Git差异核对。针对四个原路径的 `git diff --check` 无输出。
- 备份与当前候选运行配置均2206字节，独立逐字节对比只有offset109从49变54，即第2行 `[5,11]` 的11变16。原/当前SHA分别为 `817FB9B0E2163A1AAF21BEC2D29FE188BC7CD7BFC7A40268570C21305259C891` / `DBE002F800AE2DB1E759A2952B5F9307DB5A136B9FCD6B2E5FF29C9DD9795B58`，与 `config-sha.txt`、`config-byte-diff.txt` 一致。当前文件生效内容与独立服务端读取正式默认的证据分别核对；本席未启动客户端，因此不把运行文件身份写成客户端已加载结论。

## 发现分级

- **Critical（必改）：无。**
- **Important（必改）：无。**
- **Minor（可选）：无。**

## 明确不判定的行为

- 客户端16³实际搭建、17拒绝提示、护目镜尺寸/容量与本批显示操作：未启动客户端，按卡保留人工门；自动通过不替代用户报告。
- 用户存档来源与候选存档复制/回写：PM与用户尚有待决来源，本审查不操作世界，不以本批服务端保存证据断言用户存档手测通过。
- 旧版本存档兼容、跨版本迁移及32³传输格式：本批不新增格式或迁移合同，首发前治理明确排除兼容研究；原扫描/显示协议工程上界32为保守封顶，不能当作当前合法玩法上限，也不要求改成16。
- 原已验收锅炉完整热工生产线、美术矩阵以及其他dirty文档/日志：本批生产差异与JAR文件保护已证明其对应来源未变，范围外已有状态由PM管理，本席不重跑或清理。

技术审查可通过；建议PM仅接续本批候选与人工门，保留原自动记录和已关闭的其他验收门。
