# DEVICE-PONDER-04-FRAMING 实现报告

**基线：** `5ced0b704bb1d8bff0f65268cfb4f9ac8fbfab7a`

**交付状态：** 执行者已交付，仍待项目经理审查和用户定向播放确认。

**技能：** 已读取并应用 `minecraft-modding`、`minecraft-testing` 与 `superpowers:systematic-debugging`；版本依据为仓库 `gradle.properties`，包括 Minecraft 1.21.1、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6 和 Java 21。

## 根因与实现

Ponder 1.0.82 锁定源码中的 `PonderScene.SceneTransform.apply` 以 `basePlateSize / 2 + basePlateOffsetX/Z` 作为视口中心，并将 `scaleFactor` 作为整幕统一倍率。原搭建页将26格方形基准板放在原点，因此中心为 `(13,13)`；小、中、大汽轮机分别位于 `x=8/12/20`，相对镜头中心明显分散。小型机偏离中心后落入页面底部控件区域，大型机位于画面右侧并被裁切。单纯降低整幕倍率不能校正该偏移。

搭建页改用中心 `(8,8)`、14格基准板，整幕上移 `0.35`；既有 `0.82` 倍率保留，以适配最大汽轮机完整轮廓。三档按原始真实尺寸依次独立显示，连同各自模板底座移动到共同中心；小型上移1格并沿轴向移1格，中型沿横向移`-4`格、轴向移`+0.5`格，大型沿横向移`-12`格。底板先从初始搭建阶段收起，再与各档设备一起显示/隐藏，避免原26格底板继续影响构图。

区段变换后的包围范围（含对应底座，坐标单位为方块）：

| 档位 | 位移 `(x,y,z)` | 展示包围盒 `x/y/z` |
| --- | --- | --- |
| 小型 3×5×3 | `(0,+1,+1)` | `6..10 / 1..4 / 5..11` |
| 中型 5×8×5 | `(-4,0,+0.5)` | `5..11 / 0..5 / 3.5..12.5` |
| 大型 7×11×7 | `(-12,0,0)` | `4..12 / 0..7 / 2..14` |

Ponder 的 `SceneBuilder.configureBasePlate`、`scaleSceneView` 和 `setSceneOffsetY` 都设置整幕参数；锁定版本 `WorldSectionElement` 支持区段平移/旋转，没有逐区段缩放 API。因此本实现保留模型块的原始尺寸差异，用逐档平移和抬高解决镜头位置，再用适配大型机的统一倍率。模板文件和结构合同均未变化。

## 独立复核整改

复核发现中、小型独立区段的 Z 位移没有同步到基于整格 `Selection` 构造的高亮框。现改用 Ponder 1.0.82 的 `chaseBoundingBoxOutline`，由档位几何尺寸和同一组 X/Y/Z 区段位移生成 `AABB`；小型 `+1` 格与中型 `+0.5` 格位移均原精度应用，正文锚点使用该 AABB 的中心，大型 `0` 格也沿用相同路径。由此框选范围与画面中的独立设备区段一致。

整改后增量构建日志单独保存于 `build/reports/extension/DEVICE-PONDER-04-FRAMING/assemble-review-fix.log`，原始 `assemble.log` 保留不变。

通汽页中文现为“超临界蒸汽从进汽口进入，蒸汽从排汽口排出。”；英文资源和 Java fallback 同步使用 `exhaust port`。

## 写入范围

- `TurbinePonderScenes.java`：镜头中心、整幕抬高、三档区段独立平移及英文 fallback。
- `zh_cn.json`、`en_us.json`：仅汽轮机通汽页 `text_1`。
- `build/reports/extension/DEVICE-PONDER-04-FRAMING/assemble.log`：完整构建输出。
- `build/reports/extension/DEVICE-PONDER-04-FRAMING/assemble-review-fix.log`：高亮框复核整改后的增量构建输出。
- 本报告。

未改模板、故事板绑定、正式结构尺寸或玩法；未执行任何 Git 写操作。既有日志、Python 缓存及其他工作区改动均保留。

## 验证与待确认

- 锁定 Ponder 1.0.82 源码核实中心变换、静态缩放参数和区段变换能力。
- 中文/英文语言文件可解析，汽轮机通汽键逐句语义对应。
- 本任务三份改动文件的 `git diff --check` 通过。仓库既有日志的尾随空白不属于本次改动。
- 初次 `./gradlew.bat assemble` 与高亮框复核整改后的一次增量 `./gradlew.bat assemble` 均为 `BUILD SUCCESSFUL`；分别见两份日志。
- 未改模板，未运行合同测试、全量测试或 `runClient`。
- 最终人工门仍需用户播放搭建页，确认三档完整可见、端口与轴清楚、当前重点不受文本/底部控件遮挡，并复看“排汽口”文案。

需要重点复核的实现文件为 `src/main/java/com/iksxh/create_nuclear_industry/ponder/TurbinePonderScenes.java`；其他未授权工作区改动不在本次写集内。
