# REACTOR-ROD-COOLANT-01 主分支复看候选

2026-10-11：源码修复已整合main `79ca4e4f006c0e1fbbec179f79997b24e407b54d`，美术树净同步`eef4f181e76ed190801586e39fac0cffc9a8eab8`。自动与独立审查门通过；真实GPU视觉门待用户确认，不记为最终视觉验收，不推进其他主线。

## 修复与证据

加注冷却液后，原液体共享批次可能先于棒体提交并写深度，使棒体被深度测试遮挡。修复仅为原256个液体纹理槽注册固定缓冲，并在所有方块实体提交后，按棒体、原燃料辉光、液体的次序定向刷新。棒体模型、实际控制深度、液位/冷热混色、材质透明度与深度状态不变；L1/L2、服务端库存、热工、控件、资源、依赖和构建脚本无变更。

- 实际应用Minecraft modding/testing、系统排错、测试先行、完成前验证及独立窄审；版本保持Java21、MC1.21.1、NeoForge21.1.219和Create6.0.10-280。
- 原生来源为本树锁定NeoForge sources JAR；真实BufferSource回归捕获初始共享液体提前提交。首轮27项通过后，独立审查发现原辉光滞后，新增两项回归先复现失败，再作最小修正。两轮失败/中间候选完整保留。
- 最终唯一追加定向命令为`test --tests '*ReactorInternalRenderBuffersTest' --tests '*ReactorAnimationVisualStateTest' --tests '*ReactorAnimationMaterialsTest' jar --console=plain`，退出0、16秒，29/29（7+15+7），零失败/错误/跳过，实际执行compileJava/test/jar。原始证据在隔离树`build/reports/reactor-rod-coolant-01/04-glow-red*`、`05-final*`、`06-final-identities.json`和真实生产字节码。
- [实施报告](./IMPLEMENTATION.md)和[同一独立复核](./REVIEW.md)核实最终源码、class与冻结JAR绑定，首轮唯一P2已关闭，最终无必改项。测试证明真实批次保留/刷新顺序，不是GPU像素验证。
- 主分支只快进上述6个代码/报告路径，3个源码Git内容逐项与已审候选和美术树一致；9个PM保护文件SHA与开工前一致。既有日志、.gitignore及未跟踪样例保持，不停止用户客户端，不修改任何世界。

按治理5.1复用未改范围证据，没有重复全量build、GameTest或此前设备教学；本轮29项与增量JAR为修复证据，此前454项完整build仅保留为基线历史。

## 当前制品

主目录`E:/MyMC/NewMod/Create_NuclearIndustry/build/libs/create_nuclear_industry-0.1.0.jar`已更新为同基线隔离树的已审冻结制品：**2507570字节**，SHA-256 **`0de3aa3c20a99850f56d68f2ab8fd0a5538aadcfb9cfd2628f84553f8500c2d5`**。安装后重新核对完全相同，记录在主目录`build/reports/reactor-rod-coolant-01/pm-integration.json`。前一JAR原字节保存在同目录`pre-integration/create_nuclear_industry-0.1.0.previous.jar`，SHA为`0974e4b3ee9c4c15931c4e0b59625252dfa0687903e60f072d3cfc7b07c8670b`；旧构建验收中的制品身份保持历史。

## 用户复看

退出当前客户端，在主目录重新执行`.\gradlew.bat runClient`加载修复源码。对同一反应堆分别观察空、部分及满冷却液时的控制棒，调整插入深度确认棒体升降持续可见；运行时顺带观察燃料蓝辉仍可透过液体显示。无需重测配方、热工或已验收设备教学。

当前自动验证没有执行客户端GPU事件或取得修复后截图；此复看通过后才关闭本故障视觉门。此前美术其他独立视觉记录不由本修复或代码审查覆盖。
