# EXT-ART-06 独立审查报告

**结论：需整改（2 项 Minor；未发现 Critical/Important 阻断项）。** 除下列标记与注释问题外，离线素材、白名单、旧图保持和证据记录符合任务合同。整改后应由项目经理限定复审这两处；客户端视觉验收仍待用户完成。

- 审查基线：`9295e48c9275b013a8613ad8e54534d99e34c9a7`；候选：`E:\MyMC\NewMod\Create_NuclearIndustry-ore-acquisition`。
- 实际读取并应用：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`（按锁定的 MC 1.21.1 和美术专属写集核对，不据技能示例扩大运行/功能范围）；`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`（区分离线断言证据与游戏测试证据）；`C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`（核对16×16 RGBA物品纹理及透明像素合同）。

## 证据与合同核对

- 任务报告、活动计划及实际差异相符：66项 manifest = 65项游戏 PNG + 1项 lapis 工具候选；62个唯一源稿由8条冷却剂 block/fluid 映射共用4个源稿解释。`pipeline.py` 的历史集合仍固定51条，新增集合是先前9条加本批精确5条；manifest 必须与65条路径集合完全相等。`verify.py` 的计数和材料阶段调整遵守 PM 批准的补充写集，坏 SVG、额外路径、错误尺寸/映射和保留声明的真实 CLI 失败不写断言仍在。
- 五个 SVG 均为16×16，仅包含整数坐标的直属 `rect` 和固定色板色值；实体绘制离边缘至少2像素。导出器仍拒绝额外 SVG 元素/属性、非整数/越界矩形，并要求 RGBA、alpha 仅0/255、物品透明外圈与不超过16色。报告所附 `verification.json` 记录 PASS、重复导出/安装及失败不写结果；审查未运行 `verify.py` 或 `export.py`。
- 独立 SHA-256 对比确认五个新游戏 PNG 分别与 `tools/art-assets/generated/item/` 对应输出逐字节一致。开工60张快照的60个路径均存在，当前哈希与快照一致；再以 `git hash-object` 对比 `9295e48` 的 Git blob，60项均匹配，未见旧图篡改。冷却剂子清单现为8条，逐条是这60项的子集且哈希相同。交付报告说明最初筛选误纳入两张主冷却泵贴图、10条误计后来修成合同要求的8条；本次核实的是修正后清单及基线，不把未留存的旧10条文件当作独立复验。
- 已用图像查看器实际查看五项最终单项对比图：锡条清楚表现为两条错位金属条；工业和辐射成品可区分；两个半成品沿用各自成品外壳并表现未完成状态。图中有原尺寸以及浅/深底放大图。独立对比图位于 `build/reports/extension/EXT-ART-06/`。

## 整改项

1. **Minor — 预览任务标识未同步。** [pipeline.py:185](</E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/tools/art-assets/pipeline.py:185>) 仍把每张分组总览标题写成 `EXT-ART-05`。当前生成的 [items-4.png](</E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/tools/art-assets/previews/items-4.png>) 实际显示 `EXT-ART-05 / ITEMS / 4`，而内容已含本批两种传感器半成品。将标题更新为 `EXT-ART-06` 并刷新受影响的分组预览，避免将本批审阅图归到旧任务。
2. **Minor — 源稿注释与画面不符。** [industrial_sensor.svg:2](</E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/tools/art-assets/sources/item/industrial_sensor.svg:2>) 写有“红色状态灯”，但该 SVG 与 `industrial_sensor` 色板均没有红色；实际画面是灰色表壳和蓝绿色显示窗。按实际画面修正中文注释，不需要为满足注释而擅自新增未要求的视觉元素。

## 未判断

未运行 Gradle、verify/export 管线、游戏或 Minecraft 客户端；运行时资源加载、JEI、手持外观、Create 设备中的物品显示、存档重进及客户端人工验收均不在本次证据范围。离线 PNG 对比不等同客户端通过。并行功能代码及 PM 文档差异不属于本审查写集，未据此提出美术缺陷；README 同步由 PM 负责。

## 定点复审（2026-10-01）

**复审结论：原两项 Minor 均已关闭；限定复审通过。** 本次只检查这两项及对 PNG 的影响，没有重做全审。

- [pipeline.py:185](</E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/tools/art-assets/pipeline.py:185>) 已使用 `EXT-ART-06`；我重新查看 `tools/art-assets/previews/items-4.png`，图内标题为 `EXT-ART-06 / ITEMS / 4`，且页面内容包含本批半成品。第一项已修正。
- [industrial_sensor.svg:2](</E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/tools/art-assets/sources/item/industrial_sensor.svg:2>) 已改为准确描述灰色仪表壳和蓝绿色读数窗，未加入红色灯。第二项已修正。
- 对影响面的限定核对：`minor-fix-before/after-5-hashes.json` 的5项键和值一致；`minor-fix-before/after-65-hashes.json` 的65项键和值一致（独立比较为0差异）。当前全部65张游戏 PNG 与修后65项快照一致；5张本批游戏 PNG 与各自 `generated/item/` 文件也一致。交付记录的最终 `verification.json` 为开工65张、最终65张、结果 `PASS`。
- 证据时间点需区分：初次审查所用 `baseline-60-game-png-sha256.json` 是原开工60张快照；其60项已在初审中逐项与 `9295e48` Git blob 比对且全部相同。`verification.json` 后来被定点整改后的复跑覆盖，当前65→65记录不是初次60项验证的替代，也没有单独留存初次完整 `verification.json`。因此这里分别引用旧60快照/Git证据与本次65→65记录，不混为同一次起始计数。
- 本次未运行 `verify.py`、`export.py`、Gradle或游戏；交付报告所记重跑命令仅作为既有证据读取，没有由审查者复跑。
