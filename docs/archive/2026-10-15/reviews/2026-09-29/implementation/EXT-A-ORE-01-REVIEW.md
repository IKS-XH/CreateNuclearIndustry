# EXT-A-ORE-01 独立交付审查（2026-09-29）

**结论：未发现三矿实现的合同阻塞项，可交 PM 做最终审核和候选整合。** 本次只读审查 `C:/Users/IKSXH/.codex/worktrees/ore-acquisition/Create_NuclearIndustry` 相对基线 `adaf347b911d92ed3394a720ada94dff51918ce7` 的源码、资源和已有日志；没有并发运行 Gradle 或 Minecraft，也没有批准客户端人工门。

| 核对项 | 结果及边界 |
| --- | --- |
| 身份与写集 | `OreContent` 只登记六矿石、三粗矿块、三粗矿共 12 个新 ID（九方块及对应 BlockItem、三普通物品），入口和创造栏仅接入本批。模型、双语、九掉落表、三 configured/placed feature、三个 biome modifier、六压缩/解压配方及 `c:*`/原版工具标签齐备；未见 PNG、P1 状态、构建脚本或核心文档改动。运行产生的 `logs/debug.log`、`logs/latest.log` 已由 PM 复制至本卡证据目录 `pm-generated-logs/` 后定向恢复；现有工作树仅余任务允许的 `src/` 变更。 |
| 生成与去重 | 三矿 JSON 的高度起点、次数、规模、暴露丢弃与批准表完全一致；每矿一条 placed feature 同时用 stone/deepslate target。`OreGenerationFilter.java:55–66` 先核对实际 `minecraft:overworld`，再判 disabled/enabled/auto；auto 读取已绑定 `c:ores/<矿物>` 方块标签，缺标签保守关闭，不读取物品标签或缓存空集合。Biome modifier 使用 `#minecraft:is_overworld`；真实维度过滤不依赖该群系标签单独成立。 |
| 采集与加工 | 普通 Block 复制相应原版物理属性并要求正确工具；矿石 loot 为普通一份粗矿、Silk Touch 自身、Fortune `minecraft:ore_drops`、爆炸衰减；粗矿块掉自身，工具等级由镐/石或铁级标签给出。六配方 9 粗矿↔1 对应粗矿块，输入通用标签、输出本模组 ID。锁定 Create 6.0.10-280 JAR 中九条粉碎配方均存在，时长 400、保证/75% 结果与任务卡一致；本批没有竞争的粉碎配方。最终 GameTest 实际用玩家破坏、加载配方管理器和通电双粉碎轮/Capability 投料，九输入均有实体产出记录。 |
| 自动证据 | 最终 `test-final` 退出 0，52 个 JUnit XML 合计 **265/0 failures/0 errors/0 skipped**；其中新增 `ExtensionOreDataContractTest` 四项全过。最终 `gametest-final` 记录 **116 required tests 全部断言通过**（111 旧项、4 项本批有效测试、1 项在平坦 GameTest 世界明确不采样）；随后停在 Saving worlds，仅对应 JVM 强停，Gradle **退出 1**，不能表述为命令正常成功。最终 `build-final` 退出 0，其中 `test` 为 UP-TO-DATE，复用刚通过的 XML。PM 处理运行日志后，我独立复跑全树 `git diff --check` 退出 0。 |
| 普通世界生成 | 三个固定种子各扫描正常生成器中的 64 个新区块、均正常保存退出 0：seed 20260929 铅 1629/锡 2678/铀 249；42 为 1428/1937/214；314159265 为 1641/2847/241。计数是两矿石形态合计；**普通形态 `uranium_ore` 在三样本均为 0**，不能宣称六形态均自然发现。Y 范围为实际矿块范围，原生 height provider 约束尝试起点而非逐块裁切。 |
| 数据包与生命周期 | 隔离包均正常服退出 0：auto 加外部方块标签成员后三矿全 0，禁用包并重载后标签门恢复；enabled 在外部成员存在时仍生成；disabled 全 0；仅 ITEM 标签不触发去重。使用 `minecraft:iron_ore` 作为模拟外部 tag 成员，只证明标签机制，不等于真实第三方同矿联调。另在同一隔离世界验证 disabled→enabled 的 placed-feature mode **运行中 reload 不生效、重启后新区块生效**；任务报告明确了 mode 与即时标签重载的不同边界。 |
| Y49 定点 | seed 314159265 的最高深层锡矿在 `(1975,49,1928)`，复采仍存在；邻近为真实 `minecraft:deepslate`，目标 tag 允许 deepslate/tuff，采样远离 GameTest 结构。锁定 Create 的 striated ores 提供高处深板岩的可行来源；没有逐块 feature 调用追踪，故此为有依据的地质解释，不能写成完整来源证明。未据异常点改变已批准的矿脉数值。 |

执行者报告 `build/reports/extension/EXT-A-ORE-01.md` 的最终核验段如实列出非零 GameTest 退出、三种子/数据包/重载边界及客户端待验。PM 整合美术后还需核对最终制品并向用户提供客户端检查；静态、自动和普通服务器证据不替代新世界/旧世界新区块的实际采集、工具、Create 机器与贴图体验。

实际读取并应用本机 `minecraft-modding`（NeoForge 注册/服务端边界）、`minecraft-testing`（JUnit、GameTest、普通服证据分层）、`minecraft-world-generation`（configured/placed、BiomeModifier、维度和数据包覆盖）、`minecraft-resource-pack`（1.21.1 模型/纹理路径）及最新任务卡。通用技能示例未改变锁定版本。
