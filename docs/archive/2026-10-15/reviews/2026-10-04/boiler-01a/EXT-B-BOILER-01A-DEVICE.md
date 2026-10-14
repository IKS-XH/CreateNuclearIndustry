# EXT-B-BOILER-01A 设备实现交付

- 执行范围：七种固定锅炉部件注册、3×3×4 结构与唯一归属、控制器独占水汽/热账本、Create 水汽口、换热器实付热事务、护目镜/红石/扳手/安全阀/单件携物回收；只注册 `supercritical_steam`，无桶或世界放置。控制器、水口、汽口采用水平 `facing`，其他四块无方向。Create 构造移动检查禁止本批七块移动。
- 实际使用 `minecraft-modding`、`minecraft-testing` 技能，并按治理 5.1 只做定向增量验证。未修改 Git 状态或核心文档，未启动用户客户端/存档。
- JUnit：`BoilerStateTest` 8/8、受影响 `HeatExchangerStateTest` 12/12、`SharedFluidFillPlanTest` 7/7；`assemble` 成功。原始命令输出：[EXT-B-BOILER-01A-DEVICE-junit-assemble.log](EXT-B-BOILER-01A-DEVICE-junit-assemble.log)，原始断言见 `build/test-results/test/TEST-*.xml`。
- 锅炉 GameTest：7/7 必需测试通过，进程退出 0。覆盖真实 Create 双水口泵管近满灌注（仅接收余下 100 mB）、蒸汽口经泵管进入原生储罐、开放口拒收而不消失、真实换热器先暖炉后产汽、结构失效与重复归属、单件控制器携物重放、工作台与动力合成配方。原始输出：[EXT-B-BOILER-01A-DEVICE-gametest.log](EXT-B-BOILER-01A-DEVICE-gametest.log)，服务端日志 `build/gametest/boiler-01a/logs/latest.log`。
- 原生换热器 GameTest 回归：11/11 必需测试通过，进程退出 0，包含原生锅炉 9/18 热级行为。原始输出：[EXT-B-BOILER-01A-DEVICE-native-regression.log](EXT-B-BOILER-01A-DEVICE-native-regression.log)，服务端日志 `build/gametest/boiler-native-regression/logs/latest.log`。
- 最后定向 `git diff --check` 无空白错误；原工作区已有的 `logs/debug.log`、`logs/latest.log` 未触碰。资源由并行素材执行者交付，其图集补丁已纳入本次 `assemble`。
- 客户端视觉与用户人工三组测试尚待项目经理组织；本报告只陈述本次自动证据，不代替人工验收。
