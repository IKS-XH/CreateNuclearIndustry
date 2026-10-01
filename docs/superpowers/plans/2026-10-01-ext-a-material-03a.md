# EXT-A-MATERIAL-03A：钢材显示名与验收收尾

**需求与状态：** 用户已确认材料03客户端手动测试全部通过，并要求游戏内“合金钢”改称“钢”。显示名修订已完成，候选提交`c860cf7`；只读回归诊断已交付，155项完整回归仍未完成。见[收尾记录](../../reviews/2026-10-01/material-03a/README.md)。不重复人工整链验收，不将人工通过写成自动回归通过。

**基线：** 候选 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition` / `codex/ore-acquisition` / `feb2cb3`干净；main `b36616e`仅保留用户既有`.vscode/launch.json`修改。MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6不变。

## A：显示名实施

- 派高速执行者。必读AGENTS、治理、材料03卡、本卡；实际应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`和`minecraft-resource-pack/SKILL.md`。执行者无Git写、核心文档或再派发权限。
- 唯一生产写集：候选 `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json` 与 `en_us.json`。中文三物品改为钢粉、钢锭、钢板，英文对应Steel Dust、Steel Ingot、Steel Plate；两文件中引用该钢板的维修提示同步简称。其他键值、格式、注册ID、配方、测试、纹理与存档不得改动。强化合金钢等其他规划材料不在此次显示名修改范围。
- 临时验证与交付仅候选 `build/reports/extension/EXT-A-MATERIAL-03A.md` 和同名目录。两语言JSON解析、键集/差异范围与最终值核对；使用现有 `processResources jar` 打包并核对源/输出/JAR语言值一致，不为纯显示文案新增测试或重跑全量游戏。报告前后JSON哈希、命令与退出码、JAR值、Git范围和实际技能。禁止启动客户端/服务端、改默认run或根日志。

## B：剩余自动回归只读诊断

- 另一高速执行者独立只读 `EXT-A-MATERIAL-03` 最终报告、两次GREEN异常日志、锁定运行器源码与本仓库测试调度。实际应用上述modding/testing及systematic-debugging技能。
- 唯一写集为候选 `build/reports/extension/EXT-A-MATERIAL-03A-REGRESSION-AUDIT.md` 和同名目录。禁止改任何实现/测试/构建、运行Gradle/游戏、Git写、修改旧证据或再派发。
- 目标是给出具体根因证据或仍未知的范围、最小可验证后续步骤。重点检查异常所在批次的测试与回调内调度，不能仅因历史同栈判定与本批无关；不再盲目重复整套运行。若发现必须修改旧测试或运行器，先报告精确路径/函数与理由，由PM另定允许写集。

## PM收尾

PM记录用户完整人工验收，更新当前设计/清单的普通钢材名称及维修提示语义，历史交付报告保持原语境。审核语言差异与打包；全量回归门尚未解决前，不宣布整批最终验收或合入main，也不以改名引入新的人工门。执行者代码或资源修订只能由指定执行者实施，PM管理文档与Git。

执行结果：A改动严格为两语言18个值，JSON各123键不变，`processResources jar`退出0，PM独立读取JAR核对全部语言键值差异0；Git仅提交两份语言文件。B只读报告找到嵌套调度的静态风险与候选机制，未复现定位具体触发，不据此豁免回归。后续先隔离单项诊断再定整改写集，不为纯名称修订再次运行完整游戏。
