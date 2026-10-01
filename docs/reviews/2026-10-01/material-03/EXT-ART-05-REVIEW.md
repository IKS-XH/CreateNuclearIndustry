# EXT-ART-05 独立素材审查

审查基线 `909733887bedba39bea70a9a4470d8ca4c637540`，按 `docs/superpowers/plans/2026-10-01-ext-a-material-03.md` 的 B 合同只读检查候选最终差异、`EXT-ART-05.md`、`verification.json`、`commands.json`、开工55张哈希证据及实际预览。已读取并应用 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack` 技能；版本例子服从工程锁定版本。没有运行导出、verify、Gradle或游戏。

## 结论

- **合同符合性：符合。** 当前交付包含五个约定的新 SVG/PNG，以及 manifest、调色板、固定路径白名单、验证器、README与预览输出的必要接入；报告输入记录的修改范围符合 B 写集，没有改 `export.py`、baseline、旧 SVG、模型/语言/注册或功能代码。五个新游戏纹理路径恰为 `iron_dust`、`coal_dust`、`charcoal_dust`、`steel_dust`、`steel_ingot`。
- **素材实现质量：通过本次离线素材审查。** 五图为16×16整数像素风格，边缘留透明；铁粉亮灰、钢粉较深冷灰、煤粉近黑、木炭粉偏暖棕灰，仍属于同一粉堆视觉家族。钢锭采用短厚平顶轮廓，配色与现有钢板保持材料关联。放大和暗/亮底预览下四粉仍可区分，钢锭与钢板轮廓不同。
- **交接范围：可交 A 纳入打包，并可进入客户端人工看图。** 此审查不构成客户端外观验收；玩家物品栏、JEI、手持与实际游戏环境下的辨识仍由人工验收确认。

## 证据核对

| 项目 | 独立核对结果 | 证据 |
| --- | --- | --- |
| 历史基线、白名单与计数 | 51项历史基线；旧55张游戏 PNG 基线保存为独立哈希记录；新增五张后为60张游戏 PNG。manifest 61条，包含一个 `game:null` 的青金石粉候选。新增集合与五个指定物品完全一致。 | `build/reports/extension/EXT-ART-05/verification.json`；`opening-55-preservation.json`；`pre-existing-55-game-png-hashes.json`；`check_opening_55.py`；`tools/art-assets/manifest.json`、`pipeline.py` |
| 旧图字节保持 | 开工快照校验报告 `opening_game_png_unchanged=true`；独立验证报告 `historical_51_game_png_unchanged=true`、`pre_existing_game_png_unchanged=true`。正式验证在资产已安装后执行，单独55张开工快照负责证明五图加入前的全部旧游戏 PNG 未被改写。 | `opening-55-preservation.json`、`verification.json` |
| 确定性与严格拒绝 | 记录默认导出两次、安装两次；6种真实 CLI 负例为 unsupported SVG、额外路径、错误尺寸、错误映射、未授权 preserve、缺失 preserve。验证器在负例前后比较生成图、预览、HTML及游戏 PNG 的哈希状态，且对临时源稿/manifest 使用 `finally` 恢复。报告结果为 `PASS`。 | `commands.json`、`verification.json`；`tools/art-assets/verify.py`、`pipeline.py` |
| PNG/源稿一致性 | 记录所有输出 RGBA、尺寸与 Alpha 合同通过；对每个源稿以独立 Pillow 矩形绘制结果与 generated PNG 对照，检查调色板上限；清单新增项具备对应新路径。 | `verification.json` 的 records 与 `svg_or_retained_original_generated_game_equal=true`；`tools/art-assets/sources/item/{iron_dust,coal_dust,charcoal_dust,steel_dust,steel_ingot}.svg` |
| 总览及单图预览 | 实际查看完整 `preview.png`，底部总览行未截断，所有末行标签可读；实际查看 `previews/items-3.png` 的五项原尺寸、亮/暗底及2×平铺，以及 `steelmaking-preview.png` 的五项与现有钢板放大对照。 | `tools/art-assets/preview.png`；`tools/art-assets/previews/items-3.png`；`build/reports/extension/EXT-ART-05/steelmaking-preview.png` |

## 未覆盖范围

本次没有运行素材导出或验证命令，而是独立检查交付的代码、记录和最终 PNG；表中验证结论来自本批保存的命令/结果证据。没有启动 Minecraft，因此不能确认游戏内图集加载、JEI外观、手持渲染、环境光或玩家对粉末形态的实际辨识。未审查 A 的注册、模型、配方、资源打包或 Gradle/GameTest 结果。

未发现需阻断交 A 的素材问题。客户端人工看图仍是未完成门槛；在该门通过前，不据此声明 EXT-ART-05 最终验收。
