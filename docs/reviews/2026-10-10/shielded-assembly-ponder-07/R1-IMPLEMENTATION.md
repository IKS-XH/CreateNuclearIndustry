# DEVICE-PONDER-07-ASSEMBLY-R1 实现交付

2026-10-10，按用户直接要求删除装配台后两页配方场景，只保留“搭建与接口”一页。工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；开工与交付HEAD均为 `d6ecac2aaadc7e9b46c32a1d9821fd97bd397d0a`。交付未提交改动，等待PM独立窄审和用户单页播放；未运行客户端/游戏服、未操作存档、未Git写或派代理。

## 实现及保护范围

- `ShieldedAssemblyPonderScenes.java` 删除manufacture/sealing、投料/收料/批次快照/工作灯专用辅助方法和引用；只剩placement与其所需私有方法。placement方法正文逐字符与HEAD核对一致，三段原正文、镜头/轴速/物流布置、duration+20tick等待保持。
- `P1PonderPlugin.java` 装配台只注册placement；其他设备入口及顺序保持。
- `shielded_assembly_scenes.py` 只生成placement，不保留弃用路径；增加`--check`只读校验原模板，R1实际使用此参数，未重写placement。
- 双语各仅删除manufacture/sealing的8个Ponder键；所有保留键的值和顺序（含placement标题及三段正文）不变。
- 删除 `shielded_assembly_manufacture.nbt`、`shielded_assembly_sealing.nbt`，JAR中也不再包含；未增加替代页或把材料/配比/工时移到placement。
- 原位更新现有 `src/test/java/com/iksxh/create_nuclear_industry/ShieldedAssemblyPonderContractTest.java`：唯一入口、两页代码/字幕/模板不存在；保留实际八格/归属/动力/物流/双语与正文时序合同，撤下配方/载荷教学断言。新卡原测试目录笔误由PM核实并修正，没有迁移或新增测试。

精确改动为6个现有文本实现文件、删除2个模板及本报告，共9路径；全部SHA及删除前资源SHA见 `04-artifact.json`。正式设备/能力/配方/JEI、其他教学、美术、构建与配置未改。原三幕报告和证据目录保留；开工PM脏文档、两个dirty日志及三个pycache保持，日志按原字节备份/恢复。

placement模板仍为原始66格、完整八格同UUID归属和唯一主控库存，主控y=2、底轴y=1、合法水平漏斗/承接桶及顶部溜槽。源/原基线/JAR逐字节相同，SHA256 `67f03765acb3d1e5d5273cdcec15ebe47892bb0438a98b0dfbd3ff9df77645f2`。

## 技能与验证

R1实际重读根AGENTS、新卡及治理5.1/5.2，并应用本对话已经完整读取的 `minecraft-modding`（临时客户端边界）、`minecraft-testing`（原合同red/green及真实NBT）、`minecraft-resource-pack`（双语/资源/JAR）与实施/TDD/verification技能；本R1另读取SDD流程。执行者按本卡不派代理、不Git写、不重复全量，独立审查由PM安排。锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6未变。

全部新证据位于 `build/reports/extension/DEVICE-PONDER-07-ASSEMBLY-R1/`，旧证据未覆盖。

| 检查 | 实际结果/原始证据 |
| :--- | :--- |
| 先更新原合同，再在旧三幕实现下运行定向test | 退出1，3测试/2失败/0错误/0跳过；入口仍注册配方页、场景仍有配方快照的断言失败；实际placement模板合同通过。`01-red.log/.exit/.xml`。 |
| bundled Python `-B tools/ponder/shielded_assembly_scenes.py --check` | 退出0；仅回读placement，66格布局/归属与确定性通过，原字节保持。`02-template.log/.exit`。 |
| `./gradlew.bat test --tests '*ShieldedAssemblyPonderContractTest' assemble` | 退出0，最终XML为3/3、0失败/错误/跳过，唯一增量assemble成功；无clean/强制重跑/全量/GameTest。`03-final.log/.exit/.xml`。 |
| JAR/source与保护清单 | 退出0；5项class/双语/模板逐字节与源或编译class一致；JAR不含两弃用模板/方法；javap确认场景只有placement公共静态入口。双语各删恰好8键，保留键值/顺序与placement方法不变。`04-artifact-check.py/.log/.exit`、`04-class-api.txt`、`04-artifact.json`。 |
| 本批差异与日志保护 | 定向diff-check退出0；`05-diff-check.log/.exit`、`05-log-preservation.json`、`05-final-status.txt`。保存本轮新日志后恢复开工dirty字节，pycache及PM改动保持。 |

冻结JAR副本：新证据目录 `create_nuclear_industry-0.1.0.jar`，SHA256 `bc5dd22005d45a982f589e7e9d9aed0b1e2f4e3adfb0216d8e3972be4e75d11f`，与 `build/libs/create_nuclear_industry-0.1.0.jar` 一致。报告SHA也登记于清单。

## 交付门

自动检查不能替代客户端播放。PM独立审查后交用户确认：按W只有“搭建与接口”，完整机体/唯一底轴/水平物流及顶部接口可见，三段正文不重叠或裁切。未反馈通过前不合main、不推进其他主线，不关闭美术视觉门。
