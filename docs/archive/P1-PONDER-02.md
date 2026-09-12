# P1-PONDER-02 实验反应堆基础教学交付报告

## 1. 任务与基线

- 任务 ID：`P1-PONDER-02`
- 交付状态：`DONE_WITH_CONCERNS`，等待项目经理验收
- 任务卡基线代码：`832d57b`
- 本次开工提交：`c0a35a5 细化P1-PONDER-02执行任务卡`
- 基线版本：Minecraft `1.21.1`、Java `21`、NeoForge `21.1.219`、Create `6.0.10-280`、Ponder `1.0.82`、Flywheel `1.0.6`
- 开工时既有未提交改动：`logs/debug.log`、`logs/latest.log`，以及本任务允许范围内的 Ponder Java/lang 文件；执行者未覆盖范围外改动
- Git：未执行任何 Git 写操作；未创建提交、分支或标签

## 2. 实际使用的技能

- `C:\Users\lenovo\.codex\skills\minecraft-modding\SKILL.md`：核对 NeoForge 1.21.1 与 Create/Ponder 接入边界；未采用技能中的其他 Minecraft 版本示例。
- `C:\Users\lenovo\.codex\skills\minecraft-testing\SKILL.md`：执行既有 JUnit 5、NeoForge GameTest 和构建回归。
- `C:\Users\lenovo\.codex\skills\minecraft-resource-pack\SKILL.md`：核对双语 lang JSON、资源命名空间和 Ponder 文本键。
- `C:\Users\lenovo\.codex\plugins\cache\openai-bundled\computer-use\26.903.61454\skills\computer-use\SKILL.md`：尝试通过 Windows 客户端做 Ponder 实机入口核验；客户端窗口未被 CUA 暴露。

## 3. 实现摘要

本次仅扩写已有 `experimental_reactor` 故事线，没有新增 Ponder 插件、注册 ID、结构 NBT、玩法生产逻辑、网络协议或服务端状态。场景仍使用固定 `5x5x5` 的现有结构模板；为避免冻结的占位 NBT 边界限制完整结构显示，Ponder 临时世界会扩展到完整边界并按结构契约写入方块。临时教学剖面结束后恢复被移除的外墙、观察窗、仪表端口和教学用冷热端口，避免后续说明停留在缺墙或错误端口结构上。

本轮人工反馈后又补齐了第 2 段的视觉表达：切面打开时显示并框选顶层八个换料端口，随后在同一段文字持续期间恢复外墙并显示顶层，使“换料端口—内部燃料列—结构封顶”的关系连续可见。冷热端口段增加了背面/右侧镜头切换，且切面恢复后重新放回教学额外端口，防止白框落到反应堆外壳。

场景入口位于 [`P1PonderScenes.java`](../../src/main/java/com/iksxh/create_nuclear_industry/ponder/P1PonderScenes.java)，双语文本位于 [`en_us.json`](../../src/main/resources/assets/create_nuclear_industry/lang/en_us.json) 和 [`zh_cn.json`](../../src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json)。Java 代码新增的恢复辅助方法使用中文 Javadoc，未修改既有英文注释。

## 4. 十段场景合同

| 段落 | 实现内容 |
| :--- | :--- |
| 1 | 自下而上展示固定 `5x5x5` 外壳、观察窗和侧面接口；没有可变尺寸表述。 |
| 2 | 临时切面框选顶层八个换料端口，说明其下三层燃料棒、控制棒驱动器下的空气列，以及控制棒不是可放置的柱方块；随后恢复两面外墙并显示顶层换料端口、驱动器和外壳完成封顶。 |
| 3 | 聚焦唯一仪表端口并展示 Create 扳手；说明一次服务端成型诊断、恰好一个仪表端口、至少一个合法冷端口和热端口、外壳及内部列检查，且不修改状态。 |
| 4 | 展示 Create 工程师护目镜摘要：尺寸、燃料列、控制棒列、冷热端口数量和配置的冷加热总容量；明确不是当前库存，动态热量和逐列数据留待后续。 |
| 5 | 聚焦驱动器和竖直列；说明新成型时目标深度与实际深度均完全插入，驱动器位于空列顶部，不伪造控制棒方块或碰撞体。 |
| 6 | 展示逐棒 Create 风格滑块；说明所有玩家可用、客户端连续预览、服务端校验提交，红石不调整驱动器。 |
| 7 | 单独高亮冷端口；说明共享冷缓冲和冷却剂账本、每个物理端口的可配置 tick 配额，以及增加端口只提升输入吞吐而不复制库存。 |
| 8 | 在冷端口文字结束并空闲后再高亮热端口；说明共享热缓冲、独立输出接收能力、无额外全堆流量上限，以及吞吐、库存、容量、发热和背压共同限制转化。未硬编码 `128 mB/t`。 |
| 9 | 仪表端口红石高电平时说明 SCRAM 请求、保存此前目标，并命令仍可移动的控制棒完全插入；明确不保证卡死或任意布局必然成功。 |
| 10 | 移除红石后说明解除 SCRAM、恢复保存的 SCRAM 前目标深度，并明确不删除燃料和余热。 |

场景共有且仅有十次 `.text(...)` 调用，对应 `text_1` 至 `text_10`；标题使用 `header`。两种语言均存在完整的 `header`、`text_1`..`text_10`，键顺序和语义一一对应。

## 5. 修改范围

允许范围内修改：

1. `src/main/java/com/iksxh/create_nuclear_industry/ponder/P1PonderScenes.java`
2. `src/main/resources/assets/create_nuclear_industry/lang/en_us.json`
3. `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`
4. `build/reports/p1/P1-PONDER-02.md`

验证过程中 NeoForge/Gradle 更新了仓库原有的 `logs/debug.log` 与 `logs/latest.log`；这两个文件不属于本任务允许修改范围，我没有覆盖或清理。`build/gradle-home` 是为绕过受限的全局 Gradle 锁而使用的构建缓存目录，未纳入源代码交付。

## 6. 测试与命令结果

| 命令/等价命令 | 精确结果 |
| :--- | :--- |
| `gradlew.bat test --tests com.iksxh.create_nuclear_industry.P1Ponder01ContractTest` | `BUILD SUCCESSFUL`。修改换料端口/封顶时序后再次以 `--rerun-tasks` 执行，仍 `BUILD SUCCESSFUL`。 |
| `gradlew.bat test --rerun-tasks` | `50` 个结果文件、`251` 项通过，失败 `0`、错误 `0`、跳过 `0`，`BUILD SUCCESSFUL`。 |
| `gradlew.bat runGameTestServer --max-workers=1` | 前两次命中已知 `GameTestInfo.tickInternal` / fastutil 瞬态空指针；第三次输出 `110 GAME TESTS COMPLETE` 和 `All 110 required tests passed :)`，随后停在已知 `Saving worlds` 阶段，结束已完成的残留进程。无模组断言失败。 |
| `gradlew.bat build --rerun-tasks` | `BUILD SUCCESSFUL`。 |
| 两个 lang 文件 `ConvertFrom-Json` | 均解析成功；英文和简体中文各有 `11` 个 Ponder 键，缺失键为空。 |
| `git diff --check` | 完整检查仅报告测试生成的 `logs/debug.log`、`logs/latest.log` 第 7 行尾随空白；对本任务允许的 Java/lang 文件执行范围检查，无错误。 |

已有工程警告包括 NeoForge 弃用 API、Ponder 开发环境 refmap、缺少 JetBrains 注解类、Create 版本检查提示，以及既有 `compound_coolant` 方块状态模型缺失警告；本次未改动相关范围。

## 7. 客户端实机核验

客户端由用户手动启动和操作，执行者没有代替用户启动客户端。用户先完成中文逐段复验，再以另一个直接入口完成英文完整播放，并确认全部画面正常。确认范围包括十段顺序、时间轴分段、切面、换料端口、结构封顶、扳手、护目镜、控制棒滑块、冷热端口背面视角、SCRAM 高电平和解除信号。

关键中文截图证据：

- 换料端口与三层燃料列：`C:\Users\lenovo\AppData\Local\Temp\codex-clipboard-08ae1b2d-a8d2-4545-9037-d0e92c5771b8.png`
- 外墙恢复、顶层出现并完成封顶：`C:\Users\lenovo\AppData\Local\Temp\codex-clipboard-c5c19c8f-22f7-495b-9783-03ebca8348fc.png`
- 冷端口和热端口：`C:\Users\lenovo\AppData\Local\Temp\codex-clipboard-08f9eca9-a2fb-46f1-8843-5c08ad588e77.png`、`C:\Users\lenovo\AppData\Local\Temp\codex-clipboard-342c3b6b-0a44-47fe-9cd6-3578446505fa.png`
- SCRAM 高电平与解除信号：`C:\Users\lenovo\AppData\Local\Temp\codex-clipboard-eca1d455-2ea5-4b2a-9936-9b5cc8cb1514.png`、`C:\Users\lenovo\AppData\Local\Temp\codex-clipboard-af436aa4-2708-49c7-a135-60e3d56e4e7e.png`

用户确认英文完整播放正常；本轮未额外提供英文截图路径。客户端未发现文字错位、关键方块缺失、端口指向错误、紫黑资源、场景崩溃或无法手动选择分段的问题。

## 8. 兼容性与越界检查

- 存档/NBT：无改动。
- 注册 ID、Ponder 插件、客户端初始化：无改动。
- 网络、服务端权威状态、配置和生产逻辑：无改动。
- 结构资源 `experimental_reactor.nbt`：无改动。
- GameTest、JUnit、Gradle 脚本、模拟器和核心文档：无改动。
- 场景不会读取或写入服务端反应堆状态；文字仅描述已冻结的设计合同。
- 未实现 P1-PONDER-03 的动态运行数据、燃料/热量流程、事故、维修、重建或清除逻辑。

## 9. 已知事项与下一步

1. GameTest 前两次命中 NeoForge/Minecraft 已知的 `GameTestInfo.tickInternal` / fastutil 瞬态崩溃；第三次 110/110 required tests 全部通过，但进程卡在已知 `Saving worlds` 退出阶段。
2. 两个测试生成日志已产生越界工作区差异和尾随空白；执行者未清理，需项目经理决定整合前处理方式。
3. 中英文客户端人工复验已由用户完成；场景实现和双语资源可交项目经理审核。执行者不修改任务状态、不执行 Git 写操作。

## 10. 项目经理复验与归档补记

项目经理于 `2026-09-12` 审查允许写集、场景实现、双语键和客户端截图，确认交付仅修改 `P1PonderScenes.java`、`en_us.json` 与 `zh_cn.json`，没有改变唯一插件、11 项入口、`experimental_reactor` 故事板、冻结结构 NBT、玩法服务端状态、配置、网络或注册 ID。用户完成中英文完整播放并确认十段画面正常，因此客户端验收成立。

项目经理独立复验结果为 50 个测试结果文件、251/251 项 JUnit 全部通过，失败、错误和跳过均为 0；GameTest 输出 `110 GAME TESTS COMPLETE` 与 `All 110 required tests passed :)`；`build --rerun-tasks` 通过。GameTest 在成功后停于已知 `Saving worlds`，由项目经理终止残留进程。测试生成的 `logs/debug.log` 与 `logs/latest.log` 已恢复到提交基线，没有纳入交付。

本任务最终状态为 **已完成**。同日用户确认思索功能不影响核心玩法；本任务成果保留，`P1-PONDER-03/04` 及后续设备思索任务移到反应堆、锅炉和汽轮机具体事故实现并验收之后，不再阻塞 P1 核心验收与交接。
