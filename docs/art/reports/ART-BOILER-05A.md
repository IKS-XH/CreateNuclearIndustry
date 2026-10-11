任务：ART-BOILER-05A
基线：eef4f181e76ed190801586e39fac0cffc9a8eab8
范围：锅炉三方向原创SVG概念稿，未接入游戏。

交付：
- `tools/art-assets/boiler-concepts-05/overview.svg`：A/B/C三台同尺度闭合长方体对比，顶面、两侧墙共边；上方主体使用独立内嵌SVG符号，下方包含等比例单格、无缝3×2平铺与材料对照。
- `tools/art-assets/boiler-concepts-05/index.html`：可切换总览及三份单方向组合页。
- 每个方案目录包含9张16×16整数矩形SVG和`board.svg`。A为横箍压力舱与窄耐火嵌片，B为耐火砖错缝护套，C为强化钢骨架与内凹面板。整机墙面有隔层上下分开的窄观察窗；水、汽、冷热与控制口按高度位置示意。
- `build_concepts.py`与`check_concepts.py`均限定在本批工具目录；没有写入正式游戏资源。

设计边界：三稿统一使用5×5×7示意尺寸、隔层Y=3；蒸汽口在隔层以上，给水/控制口在下方，冷液口在隔层高度，热液口在底层非角点一侧。安全阀绘为顶面内侧的四级低矮棱柱，按只读现行模型分段X/Z与高度0..7、7..10、10..14、14..16投影；不画高烟囱。几何仅用于外观沟通，不代表CT已实现。

材质依据：锅炉用钢、强化钢和耐火砖；B的暖灰砖色取自既有`tools/art-assets/sources/item/refractory_brick.svg`。现行反应堆外壳比较图来自只读正式文件`src/main/resources/assets/create_nuclear_industry/textures/block/reactor_casing_side.png`，SVG引用路径经检查可解析；不再引用旧baseline图。反应堆PNG与模型均未改动。

样稿规格：27张16×16 SVG（每方向9张），每张仅整数`rect`与不超过14色的RGB色板，并使用`crispEdges`。总览内联新贴图rect符号；3D面按投影轴将单张tile映射到单个面。单格和平铺预览按正方形比例放大，不重复缩小贴图或留人工格缝。本批PNG_created=0；未创建OBJ或游戏JSON，也未替换PNG。

技能实际使用：
- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：按治理确认项目版本边界；本批没有模组运行时改动。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：按治理5.1未增加JUnit/GameTest/Gradle，只运行本批小型源规格与引用检查。
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：使用16×16像素、有限色板和等比例像素预览；本批是设计稿，未生成可加载资源包。

验证命令：
`& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' tools/art-assets/boiler-concepts-05/check_concepts.py`
实际结果：`styles=3; tile_svg=27; overview=present; switchable_boards=3; png_created=0`；`integer_rects=PASS; palette<=14=PASS; material_geometry=PASS; visible_windows=PASS; four_tier_valve=PASS; inline_tile_refs=PASS; three_face_projection=PASS; square_tile_samples=PASS; valid_references=PASS; formal_texture_diff=NONE`。检查器读取每份组合图的非fragment图片路径并确认文件存在；通过只读`git diff --name-only -- src/main/resources/assets/create_nuclear_industry/textures`确认正式纹理差异为空。

人工复看：负责人已实际在浏览器复看最终总览、A/B/C单稿，确认三面凸体、贴图与无缝平铺、观察窗/接口、低矮四段阶梯阀、现行反应堆外壳对照及顶部对齐正常；三种材料构造方向均可交用户选择。该复看只确认样稿展示，不代表用户已选择或批准接入游戏。当前仍停在用户选稿门；正式接入需另开任务。
