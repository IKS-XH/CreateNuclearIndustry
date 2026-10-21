# ACCIDENT-THERMAL-04A 高温固体与有限火点候选

2026-10-21，按[04A限定卡](../../../superpowers/plans/2026-10-21-accident-thermal-04a.md)交付。来源`9f53a7d5218d1a51b6fdc43bbeb98735a72d9acd`，分支`codex/accident-thermal-04a`，工作目录仍为`E:/MyMC/NewMod/Create_NuclearIndustry-accident`，基准`37f50b9e`。进度和人工门只在[STATUS](../../../STATUS.md)维护，不将本候选当main已集成或用户已体验通过。

## 本批行为

新事故残骸携带原单件真实废料，默认接触每实体每20tick至多2HP热伤、点燃3秒；高温2400加载tick，邻水加速4倍，冷后废料和辐射仍在。顶面/侧面共用节流，空残骸不伤害，正常火抗保留。高温可提前采掘，只掉原废料。

原事故破裂区最多8处有限火点、各最长600加载tick；可左键或用水扑灭，不蔓延、不复燃。身份租约防止过期清理删除后来方块。热引用不持第二份物品，未加载暂停，无离线追补或强载；火/冷热投影与原动作共享维度64写/t。

原complete仍只表示事故初始结算完成，热活跃独立；新世代空口重绑和原安全拆除不等热阶段结束。未用组件返还、已用组件每件1废料、工质全损及原封存链保持。伤害、点燃、冷却、水倍率和火限/时间均在SERVER事故配置中冻结。

有限熔融物、凝固、流动、溶蚀及相应取液保护是后续范围，本包没有以静态方块冒充已实现流体。移动周期/体积上限仍待用户答复。

## 验证与来源

[EXECUTION](EXECUTION.md)保留必要失败和最终证据索引，[唯一组合窄审](REVIEW.md)无未闭P1/P2代码阻断；实现23份源码/资源/测试文件及PM3份核心文档，未改Port/Lifecycle/CommonHooks最终事务、热工或管理员。

- 最终JUnit为9/9、exit0；原8项中4项业务RED保留。
- 最终真实服2业务＋11显式SKIP，Java/Gradle exit0、全部维度正常保存；覆盖实际站立碰撞/火抗、原生玩家灭火和真实水流、17废料＋1原件守恒、加载计时/身份/预算、BE及DataStorage当前格式读回，以及热活跃期间旧空口重绑和正式冷态拆除。
- 首轮两处夹具断言失败及后续相关复验单独保留，不把All13当13项业务；未重复全量。服务端证据不代替客户端体验、物理卸载、完整进程重启续热或真人多人观察。
- 原世界保留在原目录，未由实现/测试/审查打开；主目录3D工具dirty与其它工作区不动。

## 候选包

[原始Gradle候选JAR](/E:/MyMC/NewMod/Create_NuclearIndustry-accident/build/reports/accident-thermal-04a-20261021/candidate-accident-thermal-04a.jar)，3554973 bytes，SHA256 `87D81CE03E02EACFB252BE87662EC304D4FAF39A98B33146C43ECA5D04ED4C16`。准确已提交源唯一一次增量jar/exit0，compileJava/processResources保持UP-TO-DATE，769class/1697资源/1manifest与输出绑定，测试类/测试域未入包。20个受测编译class保持，18个生产class入包、2个GameTest内部类按原规则排除。见[BUILD](BUILD.md)及[身份摘要](ARTIFACT.json)；完整原始清单留候选树本批build/reports目录。

PM独立复算新候选及原始输出同SHA，核旧81E备份后按原字节恢复候选树根跟踪JAR；生成日志已保存再按测试前字节恢复，候选树tracked干净。需要安装本批请取上方候选包；main正式17A未替换，本批尚未合main或推送。

## 集中体验

在`E:/MyMC/NewMod/Create_NuclearIndustry-accident`运行`.\gradlew.bat runClient`，沿用原“新的世界”。触发一次新的融毁，集中检查高温外观、接触/火抗、灭火不复燃、邻水快冷、提前采掘与原链封装；正常保存退出重进后热状态应继续而不重置。服务端已复看旧空口和拆除，不要求重跑整套已验设备清单。反馈前仅记录候选就绪，不称人工通过。
