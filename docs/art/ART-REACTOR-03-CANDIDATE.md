# ART-REACTOR-03：内部运行动画候选与客户端观察

2026-10-10，美术负责人。03B运行消费已实现并冻结，最终16/16定向检查与增量JAR退出0，19资源源/安装/打包核对一致。负责人已实际读实施、源码、测试与原日志/XML，并核对63/63冻结哈希；一次组合独立审查规格及质量均通过、必改项无，内部候选可以交客户端观察。03动画视觉仍待用户，02R1门独立保持。

## 本批可见行为

- 燃料棒采用两块方形顶底钢板和九根竖直八边管。静态模型沿用原注册ID、物品显示变换与完整碰撞；管身蓝辉由可靠、可用燃料列的实际裂变HU/t驱动，功率越高越亮，停产热或失效熄辉。
- 控制棒杆身和固定端部一起整体升降，按实际深度而非目标滑条定位。完全拔出时完整棒体从驱动器上方露出；卡死保持实际位置，行程取正式列body长度。
- 合法空列及控制棒列内绘制一份透明冷却液，实际冷热库存合计决定液位，热液占比决定两套流动纹样的连续混色；零库存不绘制，没有固定上下冷热分层。邻接液体格的内部共享面剔除。

全部低分辨率纹理由[SVG作者源](../../tools/art-assets/reactor-animation/README.md)导出，游戏只读取已安装资源；既有02R1外壳与连窗整改保留。本批不改变固定5×5×5玩法、热工结算、控制速度、世界流体或玩家控件。

## 前置与证据入口

[03设计](ART-REACTOR-03-DESIGN.md)、[03A素材](reports/ART-REACTOR-03A.md)、[03A1物品显示](reports/ART-REACTOR-03A1.md)、[L2实际接口HANDOFF](../reviews/2026-10-10/reactor-runtime-display-02/HANDOFF.md)、[03B消费卡](tasks/ART-REACTOR-03B.md)。此前四模型/材质图及循环为真实OBJ/UV与SVG帧的离线投影，不能替代Minecraft运行、透明排序或最终观感。

## 运行制品绑定

- [冻结03B候选JAR](../../build/reports/art/ART-REACTOR-03B/candidate/create_nuclear_industry-0.1.0-ART-REACTOR-03B.jar)：2,473,054字节，SHA256 `7ef99a946c3be88c5b3d6f33fbc49181024c70a4360ce365624c8039e35d441f`。当前build/libs与副本一致，后续构建可能覆盖build/libs，证据以此副本为准。
- [最终实施](reports/ART-REACTOR-03B.md)、[原命令](../../build/reports/art/ART-REACTOR-03B/command-ledger.md)、[验证与XML索引](../../build/reports/art/ART-REACTOR-03B/verification.json)、[19资源对应](../../build/reports/art/ART-REACTOR-03B/resource-source-installed-jar.json)、[冻结清单](../../build/reports/art/ART-REACTOR-03B/frozen-manifest.json)。冻结清单SHA256 `6eafcbcaa90853bd63277d0669c2e5fa453607819217a88258ab77c55704efaf`，28交付＋35证据共63项全部实际核对一致。
- 源码基线为主PM同步L2 R2后的`2bece5b`，最终记录HEAD`36ea83355fab672cf1fdc7010b3e448e6396eaa6`只追加主PM文档同步；12/12共享源与R2一致，API不变。真暂停冻结租约年龄且继续失效核查，已审10/10生命周期及未改服务端证据复用。
- 本批16项为VisualState 9、Materials 7，failure/error/skip均0；唯一最终test＋jar退出0。17模型/纹理逐字节源→安装→JAR一致，两mcmeta与mapping语义一致、安装→JAR字节一致。原1038项assets中1037保持，仅授权燃料静态包装改变；原item、02R1三CT源和合并后59素材源/报告保持。

已实际读取[独立规格＋质量窄审](reports/ART-REACTOR-03-REVIEW.md)：六类/两测试与真实证据审读、五张既有预览复看通过，无必改项；未重跑构建或素材矩阵。审查报告SHA256 `6e0823f0f8c90a8cc39e2bd48ea568605c9bb931e9b8352423a444b4165d15f3`，绑定上述冻结清单及JAR。内部结论为自动与独立审查通过，停在下方用户客户端门。未启动客户端/服务或执行美术Git写操作，未将离线预览当客户端动画证明。

## 使用已有存档观察

在美术树既有运行目录使用已复制的`ore-acquisition - 新的世界`或`ore-acquisition - 新的世界 (1)`，无需重建设备。执行者与负责人均不启动客户端或改存档。

如果客户端仍在运行旧版，需要退出后重新启动本美术树，使新Java渲染器加载；F3+T仅用于启动新版后的资源重载观察。

```powershell
Set-Location -LiteralPath 'E:/MyMC/NewMod/Create_NuclearIndustry-art-studio'
$env:JAVA_HOME='C:/Program Files/Java/jdk-21'
.\gradlew.bat runClient
```

| 定向观察 | 预期 |
| :--- | :--- |
| 看燃料棒物品、手持、单块和堆内连续燃料列 | 两方形钢板及九根八边竖管清楚，同列接头闭合，物品显示大小/朝向合理 |
| 既有反应堆从零产热到低/较高产热，再停止；结合护目镜真实读数观察 | 只有可靠可用的实际产热列出现蓝辉，强度随功率变化，钢板仍有明暗，零产热熄辉 |
| 改现有控制深度，观察0%、中间值和100%；已有卡死状态时顺带看 | 完整棒体升降，上方露出拔出段；实际状态改变才移动，卡死保持实际位置，无驱动器离开视角后棒体突然消失 |
| 用现有接口改变冷热库存和总量，观察冷液主导、混合、热液主导及零库存 | 液位随库存，冷热为连续混色；只填合法空置空间，透窗可看杆体，内部无重复透明隔面 |
| 同一窗口正面/斜视、白天/较暗环境；观察升降和流动时暂停游戏 | 窗内蓝辉、棒体和液体可辨，透明层次稳定；暂停冻结位姿和流动相位 |
| 拆坏并恢复、离开相关区块再返回，以及F3+T资源重载 | 不保留失效动画，可靠恢复后正确重现，无缺材质、旧世界残影或异常悬空棒体 |

上述为03集中客户端门，收到用户实际观察前保持待验。02R1成型前后材质/连窗的[定向复看门](ART-REACTOR-02R1-CANDIDATE.md)也独立保持；不因动画自动测试、接口验收或教学验收关闭，不重复既有已验收玩法与Ponder。
