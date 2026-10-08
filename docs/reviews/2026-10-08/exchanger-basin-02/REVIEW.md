# EXT-B-EXCHANGER-02 合并规格与代码质量审查

**当前结论（2026-10-08 整改复核）：通过本批规格与代码审查。** 初审 P1/P2 均已关闭，新增证据为定向 JUnit 19/19、真实 GameTest 4/4 及增量 assemble 成功；审查者只读代码和日志，未运行这些测试。人工界面与工作盆/汽轮机分别验收仍待用户确认。整改复核详情在本文末尾。

**初审历史结论：需要整改，尚不通过。** 当时确认 1 项 P1、1 项 P2；下列初审正文保留原代码行号、原证据路径与当时限制，不代表整改后的当前状态。初审及整改复核均未运行 Gradle、测试服、客户端或用户世界。

**范围与基线：** `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，`HEAD=3bf0f88d049b032b08b40867f072a98667b95b19`；本批未提交功能差异，包括 PM 已明确加入写集的 `BasinOperatingMixin.java`。PM 手测文档、汽轮机候选说明、既有 `logs/**` 和三个 `tools/**/__pycache__/` 不作为功能缺陷，也未修改。审查者仅写本报告，不执行 Git 写操作，不派发其他执行者。

**实际技能与合同：** 已读取 `AGENTS.md`、治理 5.1/5.2、本批实施卡、具体方案、`IMPLEMENTATION.md` 及 PREP 源码摘录；实际使用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`，核对 Minecraft 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82。复用现有证据，不要求旧存档兼容研究或重复全量测试。

## 初审必须整改（历史，现已关闭）

### [P1] 非换热器工作盆的原生需热加工被全局拒绝

位置：`src/main/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerBasinBridge.java:122-123`；全局调用入口为 `src/main/java/com/iksxh/create_nuclear_industry/mixin/BasinRecipeHeatMixin.java:15-19`。

`BasinRecipeHeatMixin` 对所有非模拟的 `BasinRecipe.apply` 调用 `beginApply`。普通或超级热配方遇到盆底没有本模组换热器时，`machineFor(basin)` 返回 `null`，随后直接返回 `false`。因此玩家使用 Create 烈焰人燃烧室时，原生热查询与配方匹配仍可通过，搅拌器也能开始动画，但最终加工在原生输入提取前被取消，普通和超级热配方都无法产出。该行为直接违背“非本设备原样调用”的接入边界，也与方法注释宣称的非核热盆保留原行为不符。

需要先区分“盆底不是本设备”和“本设备存在但无有效热源”：前者完整放行 Create 原生 apply，后者才执行本批拒绝/付款逻辑。整改证据需包含一个原生燃烧室真实需热加工代表，并核对两种热条件都不再被该分支拒绝；现有换热器成功用例不能证明这一互操作边界。

### [P2] 超容量批费仍进入需求集合并持续消耗热液

位置：`src/main/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerBasinBridge.java:49-53`，相关取需求路径 `:94-102`；账本转换及钳位为 `src/main/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerState.java:87-112`。

热探测匹配后，代码在检查 `cost > capacity` 之前已经把配方加入 `CANDIDATES`。虽然原生匹配返回 `false`，源 tick 随后用该集合覆盖匹配结果，仍将这个已停用配方的费用提交给 `tickBasin`。账本没有拒绝超容量需求，只在每次转换后把储备钳位到容量，因此储备填满之后仍会继续把热液转成冷液，并丢弃无法存入的 HU；该配方始终不能完成付款。

具体合法配置：`heatLevel=1`、`huPerLevel=1`、`bufferTicks=40`，保留默认两项等效 tick 数。容量为 40 HU，超级热批费为 80 HU；放入 `create:lava_from_cobble` 的有效原料、动力和输出空间后，储备达到 40 HU 仍会每 tick 转换 2 mB 冷却剂，永远达不到 80 HU 的启动门槛。持续供液/排液会形成持续无产出的耗热负载，而护目镜显示“正在为有效工作盆配方充热”，未按合同提示对应配置停用。

需要在生成需求前排除费用非有限、非正或超容量的候选，并使账本对不可能完成的需求安全停用；对应提示须与停用原因一致，保留旧锅炉配置有效性。整改只需补这一真实边界的定向断言，不扩展全设备矩阵。

## 其余静态判断与证据边界

- 默认费用为普通 `40 × huPerLevel`、超级 `40 × 2 × huPerLevel`，默认 40/80 HU；默认密度 0.5 HU/mB 对应 80/160 mB 等量热液转冷，`huPerLevel=2` 的 80/160 HU 已有账本断言。锅炉旧参数、既有 `tick` 和冷凝核心算法未改。
- 探测沿用 Create 的 filter、物品/流体输入、容器剩余物、概率产物和输出模拟；`PROBE` 以 `finally` 复位。真实热查询绕开本设备的 `cachedHeatLevel`，正常模拟不扣 HU；概率取样仍推进 Create 原生世界 RNG，实施报告已准确限定这一边界。
- 正常实际 apply 在输入提取前扣入同一账本预留，正常 `false` 归还、`true` 保留扣款；连续批次再次检查余额。热源检查覆盖服务端、当前实例、可 tick、锅炉归属、核热模式和直列冲突，退款指向旧账本对象，不查找同位置新实例；临时支付未持久化。查询不会建立独立热池。
- 异常路径的有限声明：当前付款只在 `@At("RETURN")` 清理，原生 apply 抛异常时没有 `finally` 保证清理 `PAYMENT` 或归还费用；`HeatExchangerState.java:116` 的“失败/异常由调用方归还”不能作为已实现保证。该事实不等于要求重写 Create 的第三方 capability 库存回滚；本报告未将其扩大为第三项阻断，也未声称异常交易已经验证。
- 候选集在本批通常的搅拌配方路径复用原生候选并按物品原料数稳定排序；并未另建过滤/容器引擎。现有自动证据没有提供多候选优先序、过滤及容器/概率产物代表断言，不能把这些静态判断写成真实运行通过。

## 已读取验证证据

- `build/test-results/test/TEST-com.iksxh.create_nuclear_industry.heat.HeatExchangerStateTest.xml`：18 项、0 失败、0 错误；`...CondensationStateTest.xml`：6 项、0 失败、0 错误。本轮仅读取，合计 24/24 为实现者既有运行。
- `build/gametest-ext-b-basin/logs/latest.log`：2026-10-08 15:00:56 记录 `All 2 required tests passed`，其后全部维度保存并正常关闭。结合测试代码确认普通用例实际产出 1 个铅玻璃、冷液 80 mB、剩余 HU 为 0；超级用例实际产出 50 mB 熔岩、冷液 160 mB、剩余 HU 为 0。这证明两条新增换热器成功加工路径。
- `build/reports/extension/EXT-B-EXCHANGER-02/` 审查时仅见 `junit-startup-failure.xml`，属于已修正的早期启动失败，不能当作最终失败；最终 JUnit 和 GameTest 使用上列实际路径。`assemble` 正常及制品信息按 `IMPLEMENTATION.md` 的实现者记录引用，本审查未重跑构建，也未独立声称读取到了最终 Gradle 控制台日志。JAR 哈希文书核对由 PM 正在处理，本报告不重写实施报告。
- 本轮只读执行 `git diff --check -- src` 返回 0；新增语言与 Mixin JSON 已解析成功。该差异命令覆盖跟踪文件，新建 Java 文件另行读审，不冒称该命令覆盖全部未跟踪内容。

已通过的自动成功场景不覆盖本报告两项缺陷；真实停转/堵塞恢复、过滤/容器代表及客户端护目镜界面仍不能宣称自动通过。整改通过后，工作盆人工项与汽轮机三幕分别记录验收，不提前认定用户手测完成，也不重开已通过的锅炉教学门。

## 2026-10-08 集中整改复核

**复核范围：** 仅原 P1/P2 的整改代码、对应提示/测试及新增原始证据；没有重新开展全量审查或运行测试，继续应用初审实际读取的技能与治理 5.1/5.2。`HEAD` 仍为 `3bf0f88d049b032b08b40867f072a98667b95b19`，实现仍为候选未提交差异。

### P1 关闭：原生热源完整放行

当前 `HeatExchangerBasinBridge.java:129-132` 将 `machine == null` 单独返回 `true`；只有本模组换热器存在时才检查有效热源、动力和批费。此前全局取消 Create 燃烧室需热 apply 的分支已消除。

新增真实用例为 `ExtensionHeatExchangerBasinGameTests.java:90-123`，通过 Create `BlazeBurnerBlock.tryInsert` 实际投入煤/烈焰蛋糕并断言燃料被接受。普通用例断言 1 个铅玻璃；超级用例投入恰好 1 个圆石并精确断言 50 mB 熔岩。最终 4/4 日志与这些实际断言相符，已覆盖原 P1 的普通和超级条件。早期伪设热级、连续多批导致数量不同的夹具失败记录不作为通过证据。

### P2 关闭：超容量需求安全停用

当前 `HeatExchangerBasinBridge.java:51-56` 在登记需热配方前检查费用，非法费用仅登记停用原因，不加入可加工配方集合；`:106-110` 将无合法需求且存在非法费用的状态导向停用显示。`HeatExchangerState.java:92-98` 在转换前直接拒绝 `demandHu > capacity`，不再出现填满容量后继续转换并钳位丢热的路径。新增中英文 `basin_cost_invalid` 提示与本项停用原因一致，未改变旧锅炉的 `Settings.valid()`。

新增 `impossibleBasinBatchCostStopsWithoutConvertingHotCoolant` 使用容量 40 HU、超级批费 80 HU 的原问题配置，断言热液保留 200 mB、冷液/储备/转换量均为 0、状态为 `basin_cost_invalid`。该项列于最终 19 项通过 XML。候选筛除为静态确认，账本不转换为已有定向运行断言，不将二者混写为真实整机超容量测试。

### 新增验证与制品

- 已读取 `build/test-results/test/TEST-com.iksxh.create_nuclear_industry.heat.HeatExchangerStateTest.xml`：19 项、0 失败、0 错误，包含超容量费用断言。`remediation-junit-assemble.log` 记录 `BUILD SUCCESSFUL`、`EXIT_CODE=0`。当前定向 test 已替换 XML 目录，初审冷凝 6 项仅复用本文既有读审记录，本轮未重读该 XML、未运行冷凝测试，也不将其加入本轮 19 项。
- 已读取 `build/reports/extension/EXT-B-EXCHANGER-02/remediation-gametest-final-3.log`：4 项全部通过，全部维度保存后正常关闭，Gradle 成功且退出码 0。两项本设备成功加工仍断言 40/80 HU 与 80/160 mB 冷液；两项原生燃烧室互操作断言见上。普通本设备用例还实际设置不匹配过滤，断言无冷液转换/无产出，随后清空过滤并停转，断言无充热/原料保留，再恢复动力并完成一批。这些新增事实替代初审“过滤/停转恢复尚无自动证据”的当前状态，初审正文仍保留历史语境。
- 已读取 `remediation-assemble-final.log`：增量 assemble 成功、退出码 0。本轮只读计算 `build/libs/create_nuclear_industry-0.1.0.jar` SHA-256，结果为 `B64B97153EB460524B375C89F1E325237342D39630DC4404691EC4C67BFC3DC8`，与最终 `IMPLEMENTATION.md` 一致。PM 已说明初审制品报告哈希准确，原消息仅手打缺字；不存在需要重写准确实施记录的问题。

**剩余边界：** PAYMENT 的异常 `finally` 保证没有新增，正常 true/false 结算的有限保证维持；对应手写注释已收敛为正常 `false` 退款，不再承诺异常归还。Create 任意第三方 capability 的全面回滚仍不在合同内。本轮没有新增容器/概率产物或客户端显示断言，不把原生源码复用当成这些项目的真实运行验收。

**当前交付判断：** 本次针对原 P1/P2 的复核没有剩余整改项，可以交 PM 管理候选版本及集中手测。客户端缺热等待、堵塞恢复、实际流体网络守恒和护目镜界面仍待人工，汽轮机三幕另记结果；本报告不替代用户人工验收或授权合入主工程。仅修改本审查报告，未修改实现、测试、构建或用户世界，未执行 Git 写操作。
