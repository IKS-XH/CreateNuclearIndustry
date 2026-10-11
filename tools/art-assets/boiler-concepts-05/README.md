# ART-BOILER-05A 样稿

`overview.svg`为三方向总览，`index.html`可直接在浏览器切换单稿。三个方向各含9张16×16 SVG及一张组合细节页。图片仅作外观设计稿，不是游戏资源；只用整数矩形、有限RGB色板和crispEdges。本批PNG_created=0。

## 05A-CT 连接外观补充

`connected/overview.svg`是连接状态的默认对比图；`index.html`可切换A/B/C连接单稿和原单块稿。连接单稿分别展示5×5×7、7×5×9、连续墙面、隔层以上的2×2连窗和嵌入壳面的接口。三方向沿用原色板：A突出连续横箍，B保留暖砖错缝与外周钢框，C以疏朗整体斜筋支撑深凹面板。

每方向新增四张16×16连接模块SVG（侧面、边界侧面、顶面、连窗），由`build_connected.py`按相邻单元和外周边界组合；连窗在相邻窗格之间移除内框。新增连接图共16张SVG（12张模块、3张单稿board、1张总览）。本批没有生成PNG，也没有接入正式资源。

定向检查命令与真实stdout/exit记录见`build/reports/art/ART-BOILER-05A-CT/check-result.json`；前三次检查器/证据格式问题的原始日志也保留在同目录。旧05A原稿32个文件在连接稿生成前已有组合SHA记录，最终均匹配。
