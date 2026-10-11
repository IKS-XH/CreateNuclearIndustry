# ART-BOILER-05A-CT：锅炉连接外观补充

本批只扩展三方向SVG设计稿，未接入游戏。连接预览位于`tools/art-assets/boiler-concepts-05/connected/`，总览为`connected/overview.svg`，A/B/C放大稿分别为`connected/A/board.svg`、`connected/B/board.svg`和`connected/C/board.svg`；首页`index.html`默认显示连接总览，并可返回原单块稿。负责人已在浏览器实际复看最终总览和三份单稿，确认两种尺寸、三面闭合与共边、无内框2×2连窗、端口高度、A横箍、B错缝砖、C连续X筋和阶梯安全阀显示正常。该复看仅确认样稿展示，用户尚未选择方向。

新增12张连接模块SVG（每方向侧面、边界侧面、顶面、连窗各一张），加3张单稿和1张总览，共16张SVG。模块统一使用面内邻接函数及缺邻格边界掩码；侧墙对5×5×7与7×5×9分别按W/D独立铺贴，顶面和两侧按投影共享边闭合。连窗落在实际Y=4/5，不跨Y=3隔层，移除窗格内框并保留外周法兰。接口嵌入连续壳面，按实际Y标注给水/控制1、冷液回流3、蒸汽H−2、热液0；均位于各自面的非角列。安全阀从SVG投影反算四级顶部高度0–7、7–10、10–14、14–16及顶面中心位置。

A以连续横箍和少量焊缝为主；B沿用原暖灰耐火砖色板与错缝砖，钢框收在外周和分区；C用稀疏跨格X筋支撑深凹钢板。所有连接模块沿用原稿有限RGB颜色，仍是手绘整数矩形SVG。总览和单稿沿用只读正式反应堆贴图作为对照；其路径为`src/main/resources/assets/create_nuclear_industry/textures/block/reactor_casing_side.png`。本批没有生成PNG/OBJ，没有修改Java或游戏JSON，也没有触及正式资源。

旧05A的27张tile、3份board、旧overview和`build_concepts.py`在连接稿生成前保存了32文件轻量组合SHA；最终检查确认32/32仍一致，组合值为`92b778a6a7b7586a04191f917daa31aa142b226248a1eecc20d6ff8c53d8fe58`。现行反应堆贴图只读，检查时记录的SHA256为`e228d94f13cf12dd40d876e19e2e8fa51fc1d50d76058561753bb6dc0c413f3b`；本批写入范围没有包含该正式资源。连接稿目录PNG数量为0。

最终检查使用bundled Python，真实命令为：

`& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 'E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/tools/art-assets/boiler-concepts-05/check_connected.py'`

最终退出码为0，stdout为：

```text
styles=3; connected_tiles=12; preview_svgs=16; models=9; png_created=0
svg_numeric_values/references=PASS; neighbor_boundaries/window_inner_frame=PASS; non_square_projection/face_tile_counts=PASS; matrix_derived_actual_y_ports/windows=PASS; four_tier_valve_projected_levels=PASS; old_sources=32/32 PASS; formal_reference=READ_ONLY
```

前3次检查器尝试的真实exit 1日志保存在`build/reports/art/ART-BOILER-05A-CT/attempt-01.log`至`attempt-03.log`：依次修正冻结SVG缺少诊断标记时的实际几何解析、右墙负向Y窗口反算，以及基线JSON文件末尾非法字面量。第三项只删除了基线JSON尾部的`\n`字面字符，没有改动32个源文件SHA或组合SHA。最终检查结果、原始命令/stdout、先前尝试原因及哈希记录见`build/reports/art/ART-BOILER-05A-CT/check-result.json`与同目录`old-source-baseline.json`。

用户尚未选择A/B/C；当前仍停在选稿门，正式接入应另开任务。
