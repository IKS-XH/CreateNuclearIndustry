# EXT-B-BOILER-01B 设备执行记录（D）

候选工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；基线 `9e8690b`。本执行者仅修改 01B 卡 D 写集（含 PM 精确追加的压力 Mixin 两文件），未执行 Git 写操作、未修改 heat 包、核心文档、用户客户端或世界。并行素材、配方 JSON 与 01D 换热器实现分别由 A、C、V 负责。

## 实现

- 锅炉固定 5×5×5；98 个壳位、3×3×3 空气，棱边只允许壳体，底面中央 3×3 可放 1～9 热段。控制器、水口、汽口和阀门改到新坐标；旧 3×3×4 不再成型，但保留控制器库存与已付暖炉 HU，重搭后按新段数续热。
- 原 18HU/t/段、3600HU/段暖炉、各 16000mB 容量、给水和出汽各共享 256mB/t 未放大。九段使用三排同向换热器直列共享冷热库存，只在各排尾入口补热、首出口返冷，中央热段确实参与结算。
- 汽口沿外向面主动向邻罐推送；普通 Create 管路通过原生 `PipeConnection` 压力输送。压力贡献按锅炉归属和连接面记录差值，Create 原生 wipe 后重建；读盘先从保存压力剔除旧锅炉贡献；原生泵、多锅炉份额互不覆盖。红石停产不停止已有汽输出，目标容器和被动抽取共用汽账。
- 结构失效或重新成型后才重算候选汽口邻接的 Create 管面状态及 Flow，覆盖先铺分支管时空能力导致接面关闭、拆壳断开与重搭恢复。活动蒸汽压力仍每 tick 按 Create 限距遍历原生管网，按连接上的锅炉自有份额差值写入，不逐 tick 累加压力。
- 锅炉 GameTest 夹具扩为 5³，并适配 01D 换热器背面热入、正面冷出。强化钢板测试改为坚固板 deploying 后 pressing 一轮，另核对旧 `step=2` 原生过渡件行为。

## 自动验证

- `assemble` 与受影响的 `BoilerStateTest`、`HeatExchangerStateTest`：成功，JUnit 9+13=22 项通过；证据 `EXT-B-BOILER-01B-DEVICE-junit-assemble.log` 与 `build/test-results/test/`。最后代码状态另跑增量 `assemble`，`EXT-B-BOILER-01B-DEVICE-final-assemble.log` 成功。
- `create_nuclear_industry_boiler`：16/16 GameTest 通过，含九段远端共享热、旧库存重搭、棱边拒绝、无泵真实管路/分支、开放口、邻罐、共享额度、泵换向/刷新/汽口拆除、多归属与压力读盘，以及先铺管→成型→拆壳→重搭和 8000mB 守恒；证据 `EXT-B-BOILER-01B-DEVICE-boiler-gametest.log`。
- `create_nuclear_industry_heat_exchanger`：14/14 通过，含新两工序真实加工与旧半成品；证据 `EXT-B-BOILER-01B-DEVICE-heat-exchanger-gametest.log`。
- `create_nuclear_industry_heat_loop`：4/4 通过；证据 `EXT-B-BOILER-01B-DEVICE-heat-loop-gametest.log`。
- `create_nuclear_industry_heat_chain`：6/6 通过，含三机两端真实 Create 管路加中央锅炉远端共享热及先管后机连接恢复；证据 `EXT-B-BOILER-01B-DEVICE-heat-chain-gametest.log`。此后只修改锅炉包与锅炉 GameTest，未修改 heat 包或该场景。

运行命令均为 `./gradlew.bat`：定向单测及初次构建用 `assemble test --tests ...BoilerStateTest --tests ...HeatExchangerStateTest`；每个 GameTest 域用 `runGameTestServer -PgameTestNamespace=<域> -PgameTestDirectory=build/gametest/EXT-B-BOILER-01B-DEVICE-<域> --console=plain`；最后用 `assemble --console=plain`。失败中间日志由同名文件的最终通过运行覆盖；无全量测试。D 写集 `git diff --check` 无空白错误，仓库原有 logs 未纳入本任务。

旧 `step=2` 过渡件的运行结果与 Create 6.0.10 源码一致：放到新两步同 ID 配方后，再消耗一块坚固板便直接完成，不再执行最终压片。这是旧在途件的兼容边界；本批未新增自定义迁移，需 PM 判断后续人工门/处理方式。

以上是服务端/静态证据；真实客户端模型观感、世界内手搭与实际游玩操作尚待人工验收。已实际读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 和 `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`，以仓库锁定 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280 为准。
