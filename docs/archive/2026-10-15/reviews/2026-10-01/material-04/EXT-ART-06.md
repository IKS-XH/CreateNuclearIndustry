# EXT-ART-06 美术素材交付报告

**基线：** `9295e48c9275b013a8613ad8e54534d99e34c9a7`（`codex/ore-acquisition`）
**范围：** 锡条、工业传感器、辐射传感器及两种序列装配半成品的16×16 SVG与PNG。只交付离线素材和管线清单扩充；客户端外观仍待用户验收。

## 素材与识别

- 锡条采用两根错位的薄银灰长条，使用现有锡材色板；原尺寸下可与厚实单块的锡锭区分。
- 工业传感器用冷灰仪表壳、蓝绿色读数窗识别；辐射传感器用铅灰壳和黄黑标志识别。
- 两种半成品沿用各自成品外壳，分别表现未装入核心的读数槽和未完成的辐射标志。
- 五个 SVG 均为16×16画布，使用整数坐标的直属 `rect`、透明外圈和固定色板；没有嵌入位图。生成 PNG 为 RGBA，管线验证 alpha 仅为0或255、每张不超过16种实色。

## 技术基线与技能

项目合同为 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。本任务只产出纹理，不改游戏代码、模型、配方或注册。

- 实际读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：按锁定项目版本与明确的纹理写集工作，没有引入技能示例中的其他版本或游戏功能。
- 实际读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：素材验证限定为本地离线像素、白名单和失败不写检查；没有把它描述成游戏测试。
- 实际读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：使用16×16 RGBA物品纹理规则，并保留透明像素及像素边缘。

## 验证结果

使用捆绑 Python `3.12.14` 与 Pillow `12.3.0`。命令为：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/verify.py
```

脚本退出码为0，结果 `PASS`。记录显示清单66条、65张游戏纹理（历史51张加14张明确新增纹理，其中本批5张），另保留青金石粉工具候选。导出和安装各重复两次，输出哈希一致；失败不写覆盖非法SVG、越界或错映射路径、尺寸错误及保留声明错误。脚本还拒绝了30种SVG语法、alpha、色板和画布负例。

开工前60张已有游戏PNG的SHA-256 快照保存在 `build/reports/extension/EXT-ART-06/baseline-60-game-png-sha256.json`；脚本确认这60张均未改变。冷却剂8张的独立快照在 `baseline-coolant-8-sha256.json`，其8个路径和哈希逐项匹配首次60图快照、验证脚本读取的锁定原图及最终游戏PNG。该文件从首次写入时的1086字节改为873字节，是因为最初按文件名包含 `coolant` 筛选时误纳入了两张主冷却泵贴图，并带有错误的额外换行字面量；随后重建为任务合同明确列举的8个冷却剂路径。该调整只改报告证据清单，没有改动游戏PNG。原51张历史基线未改变，既有四项批准样稿回归也通过。五张新PNG与源稿导出一致；青金石粉仍是工具候选，不安装为游戏PNG。

机器结果见同目录的 `verification.json` 与 `commands.json`。开工哈希及冷却剂细目分别见上述两个快照文件。

## 视觉检查与限制

已使用图像查看器检查全部五项的原尺寸和放大图。完整对照图是 `build/reports/extension/EXT-ART-06/five-items-comparison.png`；五张单项对照为：

- `build/reports/extension/EXT-ART-06/tin_wire-comparison.png`
- `build/reports/extension/EXT-ART-06/industrial_sensor-comparison.png`
- `build/reports/extension/EXT-ART-06/radiation_sensor-comparison.png`
- `build/reports/extension/EXT-ART-06/incomplete_industrial_sensor-comparison.png`
- `build/reports/extension/EXT-ART-06/incomplete_radiation_sensor-comparison.png`

每张单项图包含16×16原尺寸，以及浅色、深色背景上的最近邻8倍放大。导出器总览和分组对照位于 `tools/art-assets/preview.png`、`tools/art-assets/preview.html`、`tools/art-assets/previews/`。这些离线图像查看和字节检查不代替 Minecraft 客户端中的JEI、手持外观或半成品保存重进验收；本执行者未运行 Gradle 或游戏。

## 交付文件

- SVG 源稿：`tools/art-assets/sources/item/{tin_wire,industrial_sensor,radiation_sensor,incomplete_industrial_sensor,incomplete_radiation_sensor}.svg`
- 色板、固定清单和管线：`tools/art-assets/palette.json`、`tools/art-assets/manifest.json`、`tools/art-assets/pipeline.py`
- 经PM批准扩展计数的既有验证脚本：`tools/art-assets/verify.py`
- 五个游戏PNG：`src/main/resources/assets/create_nuclear_industry/textures/item/{tin_wire,industrial_sensor,radiation_sensor,incomplete_industrial_sensor,incomplete_radiation_sensor}.png`
- 导出候选、全局预览及分组预览：`tools/art-assets/generated/`、`tools/art-assets/preview.png`、`tools/art-assets/preview.html`、`tools/art-assets/previews/`
- 本报告及证据：`build/reports/extension/EXT-ART-06.md`、`build/reports/extension/EXT-ART-06/`

素材已稳定，可供功能候选最终打包。最终接受状态、客户端外观及存档体验由项目经理和用户按计划验收。

## 独立审查 Minor 整改

按 `build/reports/extension/EXT-ART-06-REVIEW.md` 完成两项定点修正：

- `tools/art-assets/pipeline.py` 的分组页标题由 `EXT-ART-05` 更新为 `EXT-ART-06`，重新导出后查看 `tools/art-assets/previews/items-4.png`，标题已更新且显示本批半成品。
- `industrial_sensor.svg` 的中文注释改为描述实际灰色表壳和蓝绿色读数窗；不改任何矩形或色板。

修正前先记录五张本批PNG和全部65张游戏PNG哈希，记录在证据目录的 `minor-fix-before-5-hashes.json` 与 `minor-fix-before-65-hashes.json`。默认导出重复两次，78个导出及预览文件的哈希相同；随后重跑 `verify.py`，退出码0且结果为 `PASS`，记录了65张开工游戏PNG及65张最终PNG。独立比对确认修前后全部65张游戏PNG哈希相同，五张本批PNG也逐字节不变并继续匹配 `generated/item/` 输出；修后清单保存在 `minor-fix-after-5-hashes.json`、`minor-fix-after-65-hashes.json`。未运行Gradle、游戏或Git操作。
