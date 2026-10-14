# EXT-A-MATERIAL-02 A+B 最终候选独立审查

**结论：未发现阻塞本批生产实现的缺陷，可供 PM 进行客户端候选审核；用户人工门仍待完成。** 本报告是执行者的只读审查结论，不改变任务状态，不构成最终验收、提交或合入授权。

审查工作树为 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，相对基线 `7d01b86d9ac7e07fe73700052af62a904cdb7a7c`。已完整读取 AGENTS、治理、主工程最新任务卡及批准参数页，实际应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md`：分别核对注册/版本和资源目录、真实机器与运行证据边界、模型/纹理及制品对应。版本保持 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。本审查未启动构建、游戏、测试或素材导出，未修改实现、核心文档和 Git；唯一写入为本报告。

## 实现与范围

- `BasicMaterialContent` 只注册普通可堆叠的 lead_ingot、tin_ingot、lead_plate、tin_plate；主入口只接入注册，创造栏只追加四项。四个模型正确引用两张既有锭图和两张新板图，中英名称完整。
- 六配方使用 1.21.1 的单数 `recipe` 路径。四 cooking 配方各取对应 `c:raw_materials/<metal>`，固定输出本模组对应锭；熔炉200 ticks、高炉100 ticks、经验0.7。两 Create pressing 各取 `c:ingots/<metal>`，固定输出对应板，无副产物或自定义 processing_time；省略数量沿原生默认1。运行时用例进一步断言数量和身份。
- 新细分和汇总标签全部 `replace:false`，汇总使用嵌套引用。默认 raw 标签仅各自粗矿，未接入粗矿块、粉碎料、粒或铀；没有额外旁路或硬编码黑名单。P1、旧矿物配方、JEI开发配置、构建配置及许可证未改。
- 九个新 GameTest 使用真实炉方块实体 tick 和真实 Create 压片机/置物台 capability、相邻创造电机动力连接；没有用手动调用加工辅助方法替代机器运行。覆盖四种 cooking、两种无动力等待及恢复压片、配方管理器合同、铅熔炉堵塞恢复、标签替代。中文职责说明与非显然分支说明符合本次范围。
- 证据边界明确：堵塞实际用例仅铅熔炉；等价标签仅以铅侧燧石/黏土球模拟，验证标签成员、配方匹配与固定输出，不证明第三方模组联调，也不是替代物完整机器加工测试。默认运行包含粉碎料/错误粗矿/粗矿块/铀拒绝断言；粒未独立列为运行断言，其不被接收由窄标签内容及配方输入静态确认。

## 自动证据与制品

| 证据 | 独立核验结果 |
| --- | --- |
| RED `red-gametest.log` | 125项中九个新测试失败，缺注册/配方及机器不加工符合RED；退出码1，非正常结束 |
| 初次 GREEN `green-gametest.log` | fastutil / GameTestInfo.tickInternal中途崩溃，退出1，无完整通过汇总；不算全过 |
| 修正后的 `isolated-gametest.log` | 15:37:09明确125项全部required通过；随后Saving worlds挂起，精确进程结束后退出码1。断言全过与进程异常分开记录 |
| `pm-directory-check.log` 与修正init | 两个run model及两个任务的gameDirectory均落在报告目录，隔离GameTest与普通服路径不同 |
| `isolated-reload/logs/latest.log` | 同进程三次RESULT为false→true→false，各matchedExpected=true；两次显式Reloading；15:41:04全维保存完成。runServer退出0来自A执行报告的进程结果，本审查未找到独立退出码文件，不能冒称已读取该文件 |
| `final-test-build.log`、退出码文件、JUnit XML | 最终源码后的BUILD SUCCESSFUL，退出0；265项、0失败/错误/跳过 |
| 最终JAR | SHA-256 `BE7D16211E1E0600AAF0CE9E697159FFD1FB00E27807BBECDD0800197499F80A`；219个已打包assets/data资源与当前源字节一致，包含53张本模组PNG |

标签RESULT日志位于其配方匹配/固定产物断言之后，因此三态有实质断言支持。最终build在GameTest后执行，没有把它描述成又一轮GameTest。

## B段复用与过程偏差

已审阅 `EXT-ART-03.md` 与独立 `EXT-ART-03-REVIEW.md`，复用其SVG、两次导出/安装、六类失败不写、旧51图/8冷却剂原字节、四样稿和预览证据；本审查额外核对最终JAR53图及源字节一致。工具README已区分02A历史与03本批证据路径。02A证据误写、PM备份误写副本及恢复原文件的过程在B审查/计划中披露；不能描述为全程无越界。

A初始init只改JavaExec.workingDir，被ModDevGradle RunGameTask的gameDirectory覆盖，故原RED/GREEN和普通服首启实际使用候选 `run/`，不属于隔离运行。原 `run/world` 含9/29历史测试区块，普通服此次生成level.dat并重写server.properties，缺修改前配置备份。A终版明确保留现场，PM已备份至 `isolation-incident/`；原世界与配置仍在原处，不能宣称恢复。根跟踪日志的备份/恢复也不等于run目录恢复。随后直接配置两个run model gameDirectory、核对模型/任务属性并在报告内重跑，是本次有效隔离证据。已核对最终A报告与以上结论无冲突。

## 剩余人工门

客户端仍须由用户确认熔炉/高炉产出、时长与经验表现、真实Create压片及无动力恢复、JEI配方/用途、四物品外观、保存退出重进。B离线预览与自动机器断言不能替代GUI和游戏内视觉体验。候选启动应由PM明确安全目录并处理上述run现场边界；不得把旧run目录表述成已恢复的干净测试环境。未获人工确认前，不据此报告推进钢材/设备依赖或合入新材料批。
