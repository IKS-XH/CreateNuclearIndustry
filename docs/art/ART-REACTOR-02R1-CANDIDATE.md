# ART-REACTOR-02R1：客户端定向整改候选

2026-10-10，美术负责人。针对用户截图中的成型后变浅、线条/立体感消失、窗口逐格边框问题，资源与消费者已冻结，17/17定向JUnit及增量打包通过；一次独立规格/质量窄审通过，必改项无。负责人已实际看新材质、窗簇与厚度预览并核对原始结果及审查报告，内部候选通过；用户客户端视觉门仍待实际观察，尚未通过。

## 本批变化

- 成型外壳沿用原深色钢材、成对浅色嵌板及亮侧/阴影侧，恢复局部凹槽和压板台阶。粗钢框位于整面外沿，内部使用细接缝；单块与成型保持同一材质家族。
- 窗口独用Create原生八向连接，内部孔连通，只保留窗口簇外圈及真实凹角。仪表打断或不同反应堆归属保留对应轮廓，只有斜角相触不去角框。
- 同一可靠窗簇的共享侧面由同次模型快照计算并通过ModelData遮蔽，消除斜视可见的内部隔框。未知、失效、局部替换或不同owner保守回退；外向面、外圈和非共享背面保留。

全部素材为可编辑16×16整数SVG，确定性渲染后安装图集。13项RECTANGLE图集64×64；窗口OMNIDIRECTIONAL图集128×128，47有效原生上下文及17不可达隔离回退分别登记。连接依据真实相邻关系，不穷举结构尺寸；当前玩法仍固定5×5×5。

## 制品与证据

唯一工作区`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`；实际HEAD`8b83a1a97f1b539e6dcc7b6714b9ee96c13854e0`，交付未提交，未由美术侧执行Git集成。

- [冻结候选JAR](../../build/reports/art/ART-REACTOR-02R1/candidate/create_nuclear_industry-0.1.0-ART-REACTOR-02R1.jar)：2373666字节，SHA256 `ca591c0d64b6d184930bd42ba65a2c1647ab9ac8decdf9206ed94f788f80d533`。
- [当前build/libs JAR](../../build/libs/create_nuclear_industry-0.1.0.jar)本批与冻结副本相同；该路径可能被后续构建覆盖，独立测试实例优先用冻结副本。
- [SVG作者源/导出说明](../../tools/art-assets/reactor-ct-r1/README.md)、[实施报告](reports/ART-REACTOR-02R1.md)、[独立审查](reports/ART-REACTOR-02R1-REVIEW.md)、[范围补充](ART-REACTOR-02R1-COORDINATION.md)、[原始命令](../../build/reports/art/ART-REACTOR-02R1/commands.md)。
- [最终校验](../../build/reports/art/ART-REACTOR-02R1/final-verification.json)、[测试XML](../../build/reports/art/ART-REACTOR-02R1/final-test.xml)、[冻结清单](../../build/reports/art/ART-REACTOR-02R1/frozen-manifest.json)。

实际17项测试0 failure/error/skip；最终定向test+jar退出0，22秒增量成功。14图源/安装/JAR字节相同，原JAR940个原模型/item/blockstate及其他方块纹理条目保持。944保护输入中17授权修改（2消费者、1专属测试、14安装图），927其余保持；L1/CT事件/Block/模型JSON与旧ART01/02A/02B来源和冻结证据沿用，不重复旧逻辑矩阵或旧存档测试。

全树空白检查退出2仅为自动logs两处尾空格，原样保留；新手写范围空白检查通过。首轮色板拒绝、真实JUnit red及最终范围核对路径误判日志保留，未用清理/回退抹去。

独立审查只读核对580交付+91证据共671项冻结哈希，0缺失/不一致，实际打开四张完整预览；未重复导出/测试/构建。当前候选仍未合main，Git集成由主PM或用户管理。

## 离线预览

[混合三联](../../build/reports/art/ART-REACTOR-02R1/mixed-triptych.png)按同一缩放/光照比较原单块、旧02B与新R1，包含原材质、冷热口和仪表打断的3×3窗口；[闭合外壳](../../build/reports/art/ART-REACTOR-02R1/closed-cube.png)展示顶/正/侧面。

[窗口簇](../../build/reports/art/ART-REACTOR-02R1/window-clusters.png)包含2×2、长条、L形、仪表缺口、不同owner及全透明内窗；[厚度对照](../../build/reports/art/ART-REACTOR-02R1/window-thickness.png)消费真实模型的quad方向清单，展示共享隔框遮蔽。预览来自SVG与模型行为数据的离线投影，客户端光照/透明排序和实际视觉仍由用户观察。

## 使用已有存档复看

使用美术树既有客户端与运行目录；此前已复制的`ore-acquisition - 新的世界`和`ore-acquisition - 新的世界 (1)`可继续使用，无需重建设备。本批没有启动客户端或修改存档。

```powershell
Set-Location -LiteralPath 'E:/MyMC/NewMod/Create_NuclearIndustry-art-studio'
$env:JAVA_HOME='C:/Program Files/Java/jdk-21'
.\gradlew.bat runClient
```

锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。成型依据原仪表/护目镜真实状态判断。

| 本次定向操作 | 预期 |
| :--- | :--- |
| 同一视角比较拆坏前后/恢复成型，白天和较暗处看外壳、顶面、冷热口 | 主体颜色与材质风格一致，钢框/嵌板凹槽和亮暗层次保留，连接不再换成大面积浅板 |
| 正面及斜视既有连续窗组，观察仪表周围和窗簇外沿 | 窗片间内部框及共享隔框消失，外圈、真实凹角和仪表边界完整，无缺图或异常透明面 |
| 再拆坏一格并恢复；有相邻不同归属结构时顺带观察它们的边界 | 失效后恢复单块框，不残留遮面；恢复后连通。同组内部去框，不同owner边框保留 |

仅复看本次失败项与受影响回退；既有功能/Ponder验收及L1生命周期证据保持独立。收到实际观察后再记录用户视觉结论，未得到结果前不推进下一设备。

美术负责人已在用户授权的接口协调范围，将本页、实际制品哈希、实施/独立报告及视觉门状态发送给主PM“梳理项目进度与后续任务”（`01a0ec74-ee73-7ff2-bb8b-f4956bc5f358`）。消息交接不代表用户视觉验收或Git集成；本批停在以上定向复看门。
