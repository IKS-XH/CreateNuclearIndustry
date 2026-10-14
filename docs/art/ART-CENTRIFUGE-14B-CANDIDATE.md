# 铀浓缩离心机 A 版客户端候选

2026-10-14用户明确新的离心机外观手动测试通过，已按[本次反馈](ART-CENTRIFUGE-14B-FEEDBACK.md)关闭整体外观视觉门；当前包SHA只读核保持7d62147c…，原证据复用，不重测/构建或补写未逐项报告的操作结果。源码未合main，下文“待手测”保留交付时点，下一设备进入屏蔽装配室方案准备。

2026-10-14用户选定A并要求实施。正式候选保存在 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，分支 `codex/art-studio`，基线88b8fd289d49df90d7e3c655b6e81381ad01130f；代码/资源未提交或合main。当前源码、资源、真实PNG预览与唯一增量包均已交回，原一次组合窄审及同次制品绑定无剩余必改，等待本批Minecraft外观/动画手测。

## 已实现

采用14A最终R3长轴版A，安装五模型与六张由SVG确定性导出的16²PNG，保留可编辑SVG和三稿历史。机壳为精密钢筒、层叠钢法兰/完整窗框/检修盖与少量金色压环；转子沿用28%横向增粗版本，底部是4×4×8模型单位长轴及静止轴座真孔。A的原几何/UV/颜色/alpha保持，不在正式资源中重新绘制。

运行机lower/upper/rotor/shaft分别167/155/23/1元素。物品整机346元素，与四运行部件一致，upper和其旋转origin都上移16模型单位；原GUI/手持display参数保持。blockstate八个half/facing和物品parent不改。

仅CentrifugeRenderer新增SHAFT partial与一次同angle/Direction.UP/light/cutoutMipped绘制，和原ROTOR同相位。有效lower owner、真实有符号getSpeed、非零/非超载/非BEARING_WORN条件及时间公式逐字保持；静态壳/轴座不转，不调用super重复轴。CentrifugeClientEvents读取ROTOR初始化整类，故没有修改注册文件。玩法/矿浆/粉末/磨损/维修/动力/碰撞/放置/配置/世界不改。

## 验证与实际预览

新专属Minecraft BlockModel实际解析合同最终3/3通过、0失败/错误/跳过，定向Gradle15秒exit0。安装前旧资源RED3/3失败及首次GREEN的一个float ULP断言失败保留；仅测试Y偏移按单精度2ULP容差窄纠错，原X/Z/UV及严格JSON/像素身份仍保持，不更改生产模型迎合测试。

资产11候选/安装一致，15来源/8旧备份/10选A源副本无漂移；六RGBA16PNG逐像素及alpha255与SVG一致。已审14A几何/360间隙复用，不重新生成三稿或旧矩阵。新[正式PNG预览](http://127.0.0.1:4192/)直接读本美术树五正式JSON/六PNG，22/22必要HTTP/来源/拒绝路由检查及三脚本语法通过；没有用SVG或色块冒充安装PNG。

负责人在4192/Tab14通过CUA实际看正常运行机/完整物品、底部轴侧端面、剖视完整长杆/增粗转子与低速不同相位。真实capture成功并恢复按钮；相位0→1.2086754135912774中shaft/rotor同值、壳和静态物品为0，未见新擦壳/跳动或漏材质。最终停止、关剖视、重置回默认yaw=-35/pitch16/zoom1/phase0，ready=true/errors=[]、12实际输入/19来源，运行机和物品各4152三角。独立观察图与运动原始捕获在：

- [正常机与完整物品](C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14b/overview-final.png)
- [底部输入轴](C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14b/bottom.png)
- [运行机剖视](C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14b/cutaway.png)
- [低速另一相位](C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14b/moving.png)，同目录motion.json保留该实际捕获。

原一次组合审查核源码/资源/定向结果/真实PNG与capture，无剩余必改，允许原唯一构建持有人必要增量assemble；制品身份只在同次报告追加，不重测/重生/重复封包。该内部与网页结论不代替Minecraft客户端视觉/动画验收。

## 最终测试包

`build/libs/create_nuclear_industry-0.1.0.jar`：2,815,098字节，SHA256 `7d62147c84392f00062d19635cd0a5e91300d89860b1e8502cea17079e98f507`。原构建持有人唯一 `assemble --console=plain` 实测1.845秒、Gradle1秒exit0，仅jar执行，未重测；11资源与契约/安装/build输出/JAR相同，Renderer.class与编译输出相同且实际字节码初始化两partial、使用同一角度/光照。

旧2,778,351字节/d12cca860f69f0b411a99b3c7f34bacc17895c7ac4fb7bbd149616769dc839c5包已原字节备份于 `build/reports/art/ART-CENTRIFUGE-14B/client/backup/create_nuclear_industry-0.1.0-d12cca860f69.jar`。与旧包仅11资源/1 Renderer.class共12项预期差异（三新增/九修改），其余1934载荷字节保持，无工具/网页/测试泄漏。实际日志、result、artifact-identity、javap与最终预览捕获在 `build/reports/art/ART-CENTRIFUGE-14B/client/assemble/`；负责人已读回原报告/结论并独立核最终整包SHA一致，不再次打包。

## 启动与本批集中手测

从美术工作树启动，复用原存档；本批没有移动或覆盖存档：

```powershell
Set-Location -LiteralPath 'E:/MyMC/NewMod/Create_NuclearIndustry-art-studio'
.\gradlew.bat runClient
```

1. 对照原已放置离心机：上下两格A造型、窗口可见增粗转子；四朝向检修盖和原接口位置正确，没有黑紫或丢失材质。
2. 物品栏和手持是同一A整机，顶/底/转子/输入轴齐全，没有上壳错位。
3. 接入正转与反转动力：底部输入轴和内转子同轴/同向/同相位，壳体和轴座保持静止。
4. 停止动力及已有超载/轴承磨损状态下，两运动部件保持原停转表现，维修/接口交互维持原有规则。
5. 资源重载后观察模型/纹理和再次运行，无重复轴或缺失模型。

只需本批外观/受影响动画复看，不重开已验收业务/其它设备/旧存档兼容。用户选A已确认，当前游戏视觉/动画仍待反馈；本批不自动main合入、Git写入或发布。

## 来源与报告

- [实施卡](tasks/ART-CENTRIFUGE-14B.md)
- [资产来源/导出/安装](reports/ART-CENTRIFUGE-14B-ASSETS.md)
- [客户端/定向结果/正式预览与构建](reports/ART-CENTRIFUGE-14B-CLIENT.md)
- [单次组合规格与质量窄审](reports/ART-CENTRIFUGE-14B-REVIEW.md)

新预览PID85932、4192仅127.0.0.1，Tab14已标交付；原4191/PID84900保持。原d12包已备份，当前主目录正式包和其它任务保持。
