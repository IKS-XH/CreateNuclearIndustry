# ART-REACTOR-02R1 独立规格与质量窄审

审查日期：2026-10-10。角色：独立执行者；仅写本报告，不修改实现、资源、冻结证据、任务状态或Git。

## 结论

- **规格符合：通过。** 本轮写集、原生窗口CT类型/索引、共享接口面处理与材质整改符合任务卡及协调补充。
- **内部质量：通过。** 未发现需要整改的 Critical / Important / Minor 问题；**必改项：无**。
- **用户客户端视觉门：仍未通过，需复看。** 本报告不验收Minecraft真实光照、透明排序或用户最终观感，不表示可变尺寸反应堆玩法已实现。

## 实际审查

实际读取当前主工程及美术树AGENTS、治理1.2/5.1、美术入口、02R1任务卡、协调文件（含共享接口面mask补充）、实施报告、两CT消费者、专属测试新增行为、独立导出工具与说明、最终核验脚本和原始结果。版本维持MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6。

本次复用此前实际读取的技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`用于锁定客户端生命周期/原生Create接入边界；`minecraft-testing/SKILL.md`同根入口用于检查真实red/green、生产工厂与buildContext/getModelData/getQuads行为断言；`minecraft-resource-pack/SKILL.md`同根入口用于RGBA、资源ID、透明区域、SVG来源及安装/JAR绑定。另复用`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/requesting-code-review/SKILL.md`与`verification-before-completion/SKILL.md`，按当前治理精简规则合并规格/质量一次审查，以原始证据支持结论，不重复测试或Git操作。

- 272份16×16整数rect SVG作为唯一游戏绘图源；13项RECTANGLE64、窗口OMNIDIRECTIONAL128。47原生有效索引与17不可达隔离回退明确分开，14target ID不变。严格共用renderer未改；仅预期mask255/index54全透明窗允许严格空根SVG。所有输入验证和编码先完成，默认写集有限、只写changed；check只读且不依赖预览字体。
- 生产初始化实际使用的shift工厂与可靠getDataType对窗口一致采用OMNIDIRECTIONAL。窗口只连可靠同窗簇，严格面内对角与原生双邻边门控；非窗口保持四邻且可连窗口。身份、局部ID、外平面、bounds、代次/修订守卫保持。
- 共享窗接口mask与CT在同一次capture内计算并进入ModelData；未知、非窗、不同owner明确0。getQuads只读ModelData，无世界/ThreadLocal访问；side=null按quad方向复制过滤，保留super列表、外向/外圈及非共享背面。原有scope finally恢复/清理不变。
- 实际打开`mixed-triptych.png`、`closed-cube.png`、`window-clusters.png`、`window-thickness.png`：新成型保持深色基底、嵌板和亮/暗台阶；闭合三面读感一致；条形/L形/仪表缺口/不同owner边界可辨，窗簇内部透明连续。厚度页读取专属JUnit产生的`native-window-scenes.json`，真实ScopedCTModel输出方向清单；离线投影只能支持内部结构判断。

## 证据复用与冻结绑定

以下均位于`build/reports/art/ART-REACTOR-02R1/`，本审查未重新构建、导出、运行测试或启动客户端。

- 原始`final-test.xml`：17/17，failure/error/skipped均0；`final-test-jar.log/.exit`：22s增量成功、exit0。`tdd-red.xml`为15项/3断言失败，`entry-type-red.xml`为真实工厂1项/1断言失败；consumer-green为17项通过。新增测试调用真实生产工厂、Create原生上下文/索引以及模型接口，不以源码字符串代替行为。
- `window-verification.json`及脚本/日志记录一次重复导出0变更、check只读、8条写前拒绝及SHA/mtime保持；`hole-negative.json`补足合法色填孔的透明规则拒绝。没有把先触发色板拒绝的负例误当孔形验证。
- `final-verification.json`与实际核验脚本：14导出/安装/JAR字节一致；旧JAR940项原模型/blockstate/非目标纹理保留；944保护输入中17授权变化（2消费者+1test+14game PNG），927保持。保护集合类别已核对，复用该一次结果，不重新扫描旧矩阵。
- 为确认本轮源与证据未漂移，本审查只读核对`frozen-manifest.json`全部**671项（580交付+91证据）SHA256，0缺失/0不一致**。包含生产源码、SVG/图集、实际测试与预览证据；不改冻结目录。
- 候选副本`candidate/create_nuclear_industry-0.1.0-ART-REACTOR-02R1.jar`实际2373666 bytes，SHA256 `ca591c0d64b6d184930bd42ba65a2c1647ab9ac8decdf9206ed94f788f80d533`，与冻结记录一致。旧02B副本亦与旧冻结SHA一致。
- `final-full-diff-check.log`的exit2仅自动`logs/debug.log:7`与`logs/latest.log:7`尾空白；手写白名单空白0错误，日志保留。负责人/PM并行文档及旧ART01/02A/02B未提交输入不计本轮越界。

L1、02A、02B未改生命周期/矩阵证据沿用，不重审整支。候选交负责人安排本批用户客户端定向复看与PM交接，人工门不由离线/自动结果关闭。
