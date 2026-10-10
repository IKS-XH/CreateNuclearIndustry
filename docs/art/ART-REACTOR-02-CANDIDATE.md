# ART-REACTOR-02：连接纹理客户端候选

2026-10-09，美术负责人。资源、客户端消费和制品已冻结，11/11定向测试与打包通过；一次独立规格/质量审查通过，必改项无。内部候选通过，正式客户端视觉门待用户观察，不改全局功能验收或Git状态。

**2026-10-10实际反馈：** 用户截图确认成型后颜色/线条变浅、表面变平及窗口逐格框问题；视觉门未通过。[02R1整改候选](ART-REACTOR-02R1-CANDIDATE.md)已完成17/17定向测试、增量打包与独立窄审，等待用户定向复看。本页的02B制品与证据作为历史保留，不把历史JAR标成修正后的候选。

## 候选与来源

- 唯一候选工作区：`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，基线`8b83a1a97f1b539e6dcc7b6714b9ee96c13854e0`。
- [02B历史JAR](../../build/reports/art/ART-REACTOR-02R1/baseline/create_nuclear_industry-0.1.0-02B.jar)：2367612字节，SHA256 `4cd8f15fd6108596cd37831bec7501b48b2add668ed83cecca648847350a9c71`。原制品已保留到整改基线，避免同名build/libs被新版替换后误指；这是02B历史制品，不代表整改完成或其他在制功能已集成。
- [可编辑SVG与导出工具](../../tools/art-assets/reactor-ct/README.md)：14类面sprite，各16份16×16整数SVG，共224份；确定性导出64×64 RECTANGLE图集。14张安装图与工具输出、JAR条目逐字节相同。
- [资源报告](reports/ART-REACTOR-02A.md)、[资源独立审查](reports/ART-REACTOR-02A-REVIEW.md)、[客户端报告](reports/ART-REACTOR-02B.md)、[客户端独立审查](reports/ART-REACTOR-02B-REVIEW.md)、[逻辑接口HANDOFF](../reviews/2026-10-09/reactor-surface-display-01/HANDOFF.md)。
- [离线几何预览](../../build/reports/art/ART-REACTOR-02/geometry-preview.png)、[混合墙面预览](../../build/reports/art/ART-REACTOR-02/wall-preview.png)、[归属与辨识预览](../../build/reports/art/ART-REACTOR-02/owner-preview.png)。这些是SVG资产铺设检查，不是游戏截图；6×5×8和9×7×5仅为未来尺寸的几何适配示例。

同一可靠成型结构的共面外壳、窗口和端口背景连接，整面外沿保留钢框与角螺栓，功能孔及冷热标识保持。未成型、失效、归属未知或上下文不可靠时使用单块外观；物品模型保持单块。实际边界分别读取宽高深，当前服务端合法反应堆仍是5×5×5，本批不改变尺寸玩法。

## 启动入口

沿用美术树自己的客户端与运行目录，或在测试实例使用上述JAR；保留既有版本锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。同名JAR路径会被后续构建覆盖，测试前按上面的SHA256确认候选。

用户需要在美术树启动开发客户端时，可自行执行：

```powershell
Set-Location -LiteralPath 'E:/MyMC/NewMod/Create_NuclearIndustry-art-studio'
$env:JAVA_HOME='C:/Program Files/Java/jdk-21'
.\gradlew.bat runClient
```

美术侧未启动客户端。显示接口的自动同步/撤销/区块恢复矩阵及专服证据已由主PM审查通过，下面只观察实际模型结果，不重复已验收的设备玩法或教学内容，也不检查旧存档迁移。

## 合并客户端视觉门

使用当前合法5×5×5结构；成型依据原有仪表/护目镜的真实状态，不凭连接外观反推成型。

| 定向操作 | 预期与需要记录的观察 |
| :--- | :--- |
| 首次启动，先看零散外壳/端口，再使合法反应堆成型；拆掉一格外壳并恢复 | 零散方块为单块框；可靠成型信息到达后整面连接；失效信息到达并重建后撤下连接；修复成型后恢复。记录延迟、残留面或漏更新的位置 |
| 绕看四侧、顶底和拐角，观察窗口、仪表、换料口、控制杆驱动器及冷热口 | 六面钢框闭合、材质接缝连续；功能孔不被盖住，窗口保持透明；无翻转、错格或缺失贴图 |
| 并排放两台各自合法成型的反应堆，使两者可见前面共面接壤 | 各自边框保留，背景不跨owner相连；单独拆坏一台时另一台的外观保持可靠状态 |
| 冷热口接实际管道，近看和拉远，白天与较暗处各看一次；查看物品栏和手持 | 热口红色横标、冷口蓝色竖标可识别，管道遮挡后仍易区分；连接面与单块物品风格协调，符合原版Create风格目标 |
| 离开再返回，使相关区块实际卸载/重载；改变视距、退出重进并切换世界 | 暂无可靠成员时单块回退，数据恢复后连接；没有旧世界外观串入、长期残留或缺失面 |
| 按F3+T资源重载；打开已有反应堆Ponder只观察外观 | 重载后已成型结构继续正确显示；无权威描述的Ponder临时世界保持单块，不以本批重开既有教学验收 |

反馈可直接列“通过/异常步骤、具体面或方块、光照与距离”；视觉风格意见与成型/刷新异常分别记录。自动检查与离线图不代替这张人工门。未收到实际结果前不关闭视觉验收，也不自动推进其他设备。

## 保留证据与集成边界

本批[原始命令](../../build/reports/art/ART-REACTOR-02B/commands.md)、[验证摘要](../../build/reports/art/ART-REACTOR-02B/verification.json)、[冻结清单](../../build/reports/art/ART-REACTOR-02B/frozen-manifest.json)保留真实red、方向预期纠正和最终green。11项最终JUnit无failure/error/skip，899个既有保护输入保持；原block/item/blockstate JSON、原sprite、L1接口、AT和构建未修改。

全树`git diff --check`实际退出2，仅JUnit自动改写的`logs/debug.log`及`logs/latest.log`第7行尾空白；日志原样保留，新增手写Java的逐行空白检查通过。美术侧未执行任何Git写操作；提交和主工程集成由主PM或用户管理，不能把此候选写成已经净合main。

美术负责人已在用户授权的本任务接口协调范围，向“梳理项目进度与后续任务”（`01a0ec74-ee73-7ff2-bb8b-f4956bc5f358`）发送最终候选、制品哈希、报告与人工门状态；不把消息发送成功当作Git集成或人工验收。负责人维护的四份艺术任务/候选/设计/计划文档另做逐行尾空白检查通过，实际HEAD仍为`8b83a1a`。
