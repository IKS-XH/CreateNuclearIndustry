# ART-REACTOR-02R1实施报告

执行者交未提交候选；资源、消费者和工具冻结，等待一次独立窄审及用户客户端视觉门。负责人已实际看13项第二稿及四张完整窗口/混合预览并允许安装；这不是用户视觉通过，也不扩展固定5×5×5玩法。未启动客户端、GameTest/专服/全量套件或旧存档测试，无Git写操作。

## 合同与技能

唯一工作区`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，每条shell显式workdir；HEAD `8b83a1a97f1b539e6dcc7b6714b9ee96c13854e0`。实际读取本树/主工程AGENTS、治理1.2/5.1、美术入口、02R1卡、L1 HANDOFF/02B执行与独立报告，最新版PM共享补充4971字节，SHA256 `ccf3a3a0be2aaf1c02e97b1fa1818016fae40193bf407bfba2ccdc86f30556a7`。

实际应用minecraft-modding/testing/resource-pack：锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；消费者只使用现有客户端快照与Create API。应用systematic-debugging、receiving-code-review、TDD及writing-good-tests、verification-before-completion：先实际行为断言失败，再最小补丁，按5.1只验证本批受影响路径；所有新增手写注释/Javadoc中文。技能文件为卡内`C:/Users/IKSXH/.codex/skills/`及superpowers/6.4.2/skills路径，未升级或安装依赖。

## 交付变化

- 新独立`tools/art-assets/reactor-ct-r1/`：272整数rect SVG、272图块PNG、14图集与palette/mapping/export/README。13项RECTANGLE64保持已审第二稿：原深色钢、原面积嵌板与凹槽，内部1px压板台阶，粗钢框只在外沿。13孤立格等于原sprite，10功能面核心不变，见`depth-specific-check.json`；首稿不足的两张图保留为`*-first.png`。
- 窗口64格OMNIDIRECTIONAL128，47有效Create原生上下文/17不可达隔离回退。边透明须对应合法窗；角透明须两邻边和对角都连接。内窗54为空SVG，孤立0及17回退槽保留原像素。共享严格renderer仍实际抛`ValueError: 源稿不能没有矩形`，本导出器只对预期全透明窗允许严格空根/注释，未放宽共享工具。
- 只改原两消费者与专属test：真正生产shift工厂与可靠getDataType的窗口均OMNIDIRECTIONAL；窗口只连同可靠窗，四邻及面内严格对角；其余背景仍四邻且可连窗口。身份、localID、外面、bounds及scope守卫不变。原生buildContext双邻边门控保留。
- ScopedCTModel同次capture作用域内完成CT及共享面mask，finally恢复/清理。可靠同窗簇切向四邻接口写入不可变ModelData；未知/非窗/不同owner为0。getQuads只读ModelData，有side仅拒绝该面，side=null复制按quad方向过滤，不读世界/ThreadLocal、不改super列表。外圈、外向面、非共享背面保留。
- 14张game `<name>.png`只逐字节复制本批`generated/<name>_ct.png`，target ID保持无`_ct`：`create_nuclear_industry:block/reactor_ct/<name>`。没有改CT事件、L1/API、Block/JSON、构建/注册、原sprite、ART01或02A源工具。

## 真实red/green与原生映射

| 阶段 | 实际用例/断言 | 退出码与证据 |
|---|---|---|
| 首次red | 15项，3失败，0 errors：窗→壳体原误连、getDataType原RECTANGLE、共享面原仍绘制 | 1；`tdd-red.log/.exit/.xml` |
| 工厂type red | 真实initializeOnce使用的工厂，1项失败：expected OMNIDIRECTIONAL but RECTANGLE | 1；`entry-type-red.log/.exit/.xml` |
| 消费green | 17项全通过，新增实际模型窗簇证据 | 0；`consumer-green.log/.exit/.xml` |
| 唯一最终test+jar | 17项，0 failure/error/skipped，22s增量成功 | 0；`final-test-jar.log/.exit`、`final-test.xml` |

red为真实JUnit断言失败，不是编译失败或源码字符串；工厂test不注入假type。47项调用真实Create OMNIDIRECTIONAL.getTextureIndex，全部与交付mapping逐项相等，17回退槽集合也核对；关键0/54/53/17/20覆盖。真实buildContext验证仪表打断、缺角/全角及只角相触门控；六面严格对角、轴向跨两格拒绝、不同身份/localID/非外面有定向断言。共享面覆盖双方、外向/外圈保留、未知不继承上次值、不同owner、非窗、缺失ModelData、side=null及原列表不变。复用L1/02B生命周期证据，没有重复旧矩阵。

## SVG、负例与预览

窗口导出exit0，全部输入先检验后写出；只读check通过。一次重复导出改变0文件，全部产物SHA/mtime保持，见`window-verification.json/.log/.exit`。8条负例含共享空源拒绝、孤立/实体空源、透明例外非法属性/circle、未知颜色填孔、偏移原生mapping；每条实际异常留JSON，写前拒绝且SHA/mtime不变。另补合法色填孤立孔与全透明孔，真实抛`透明区域不匹配八向窗口/实体规则`，见`hole-negative.json`；避免把色板先拒绝误称孔验证。

负责人实际复看的新增图：`mixed-triptych.png`、`closed-cube.png`、`window-clusters.png`、`window-thickness.png`。三联同尺度光照含原单块/旧成型/新成型、冷热口与3×3仪表缺口；平面含2×2/1×N/N×1/L形/不同owner/全透明内窗。厚度页实际消费`native-window-scenes.json`中的ScopedCTModel.getModelData与getQuads方向清单及buildContext索引，仅投影/光照模拟；非外面使用原SVG，SOUTH背面保留在真实清单中但此视角背朝外。所有预览已自行打开核对。离线页不能证明Minecraft透明排序/光照或替代用户复看。

## 制品与范围保护

JAR `build/libs/create_nuclear_industry-0.1.0.jar`，2373666 bytes，SHA256 `ca591c0d64b6d184930bd42ba65a2c1647ab9ac8decdf9206ed94f788f80d533`。本批候选副本保存到证据`candidate/`，旧02B JAR副本仍在baseline，SHA `4cd8f15fd6108596cd37831bec7501b48b2add668ed83cecca648847350a9c71`。

14组SVG导出/安装/JAR字节一致，13图64/window128；每图SHA、路径、尺寸和JAR entry在`final-verification.json`，无额外安装目标。原JAR中940项原模型、item模型、blockstate及非reactor_ct方块纹理逐字节保留。

944保护集合来源为02B baseline的899路径加02B冻结清单45路径，开工与旧冻结hash一致。分类：Java280、test73、resource50、旧reactor-ct466、旧作者SVG7/generated7/palette1、构建/版本2、旧证据57、旧报告1。最终只做一次范围哈希核对，不跑旧像素/逻辑矩阵；17授权变化=2Java+1test+14安装PNG，927其余保持。旧事件、L1、原模型/原sprite/ART01、02A源/工具/PNG及02B历史报告/证据不变。详见`baseline.json`与`final-verification.json`。

范围核对首次因安装metadata的Windows反斜杠与仓库正斜杠不一致误判exit1，保存`final-verification-path-first.*`；只归一证据路径后exit0，未改生产/像素或重复构建。开工13项第一轮色板拒绝的实际日志也保留，修正为原已有合法色后继续。手写SVG/工具/Java空白检查0错误；全树git diff --check实际exit2，仅JUnit自动改`logs/debug.log:7`、`logs/latest.log:7`尾空格，保留原样，未清理/回退。负责人并行art文档及PM共享授权在状态里单独可见，非执行者写集。

实际命令/日志入口`build/reports/art/ART-REACTOR-02R1/commands.md`。冻结清单`frozen-manifest.json`绑定全部580交付路径、当前证据及候选JAR（不自包含清单本身）。之后不继续改实现/源图/导出器；交负责人一次独立窄审，停在用户客户端视觉门。
