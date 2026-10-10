# DEVICE-PONDER-07-ASSEMBLY-R1 定向规格与质量复审

2026-10-10，主PM派发原独立审查者复审用户要求的删页范围。工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，基线和当前HEAD均为 `d6ecac2aaadc7e9b46c32a1d9821fd97bd397d0a`；实现仍为未提交改动。

**规格结论：通过本次删页范围审查。质量结论：通过定向窄复审。必改项无。** 可交PM登记仅含“搭建与接口”的新播放候选；未完成客户端播放验收。

## 实际依据与技能

读取新卡 `2026-10-10-device-ponder-07-assembly-usage-only.md`、最新AGENTS范围变更、R1-IMPLEMENTATION、6个文本实现差异/当前场景及测试、模板删除差异、生成器路径，以及R1原始日志/XML/冻结清单/检查脚本/新JAR。沿用本对话已实际读取的治理5.1/5.2与 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`、`requesting-code-review`、`verification-before-completion` 技能，分别用于客户端范围、合同断言、语言/资源绑定及证据审查；锁定依赖版本未变。

原 `REVIEW.md` 的合法八格、归属、动力/物品接口及Ponder1.0.82正文生命周期审读证据复用。本次只覆盖删页后的实际差异，不重开原共享逻辑或其他设备审查；未运行Gradle、生成器、客户端或服务端，未派代理或Git写。

## 规格及质量核对

- `P1PonderPlugin.java` 装配台ID列表和注册只剩 `shielded_assembly_placement`，只绑定主控物品；其余设备入口和顺序没有差异。
- `ShieldedAssemblyPonderScenes.java` 仅保留placement及实际所需私有辅助方法。两加工方法、材料引用、投料/收料/工作灯/批次快照专用代码均删除，注释改为单页用法职责；没有添加替代页面或移入配比/材料清单/加工工时。
- placement方法与基线逐字符一致，镜头/地台/机体/底轴/物流/顶部揭示及三段正文保持。共用setup、机体/物流选择与note等待没有行为变化；speed仅新增准确中文边界注释。duration+20仍使已核实duration+10生命周期结束后至少10tick净空。
- placement NBT与基线及原冻结模板逐字节相同，SHA256 `67f03765acb3d1e5d5273cdcec15ebe47892bb0438a98b0dfbd3ff9df77645f2`。复用原独立解压证据：66格、完整八格同归属/PART、主控expanded、唯一主控库存、y=1底轴、合法外侧漏斗及承接桶/顶部接口保持；本R1未重新生成模板。
- 双语各恰好删除manufacture/sealing的8个Ponder键，保留键的值与顺序全部一致，placement标题和三段fallback原样保持；正式材料/配方/JEI语言没有删除。
- 两弃用NBT在实际资源目录和新JAR中均不存在。生成器仅引用placement路径，原编码/布局/回读逻辑保持；新增`--check`跳过写入，读原文件并与确定性编码核对，不会重生两弃用页。
- 原合同测试原位调整为单入口/弃用代码、字幕、资源不存在，实际NBT八格/归属/动力/物流检查保留。移除配方/载荷教学断言符合新卡，不是放宽正式生产约束，也未改生产测试。

## 原始证据与保护核对

证据目录 `build/reports/extension/DEVICE-PONDER-07-ASSEMBLY-R1/`：

| 项目 | 实际核对结果 |
| :--- | :--- |
| `01-red.log/.exit/.xml` | 原运行exit1，3测试/2断言失败/0错误/0跳过；失败对应旧实现仍有两页入口和配方快照，placement模板合同通过 |
| `02-template.log/.exit` | 原只读--check运行记录placement的66格、回读及确定性通过，exit0；本审查不重跑 |
| `03-final.log/.exit/.xml` | 原定向测试和唯一增量assemble退出0，3/3，0失败/错误/跳过，BUILD SUCCESSFUL；仅复用原结果 |
| `04-artifact-check.py/.json`、`04-class-api.txt` | 实际6源与报告SHA均符合清单；独立打开新冻结JAR，5个class/双语/模板入口与当前编译class/源资源逐字节一致；场景API仅placement，class中无两弃用方法名，JAR装配模板仅placement |
| 范围与历史保护 | 跟踪实现差异限定6文本与2NBT删除；全局文档变更为开工PM改动。正式设备/配方、其他教学/配置无差异；本卡文本diff-check无空白错误。两dirty日志与R1开工备份逐字节一致，原报告与HEAD内容一致、原冻结JAR保持原SHA，既有pycache保留 |

新冻结与实时构建JAR SHA256均为 `bc5dd22005d45a982f589e7e9d9aed0b1e2f4e3adfb0216d8e3972be4e75d11f`。原三幕证据目录及制品仍保留，未覆盖旧交付。

## 未验边界

以上为静态差异、合同与制品审查，未证明玩家按W后的实际页面、机体/底轴/物流可见性及字幕观感。用户仍需确认只剩“搭建与接口”一页，接口清楚且三段正文不重叠/裁切。此门通过前不合main、不接新主线；ART03及02R1独立美术视觉门保持未通过。

本复审唯一写入为本R1-REVIEW.md；未修改实现或其他报告，未执行任何Git写操作。
