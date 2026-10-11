# ART-BOILER-06R1：内部汽雾可见度整改交付

2026-10-11，按用户明确反馈“锅炉内部蒸汽太淡”和06R1窄卡实施。已读取新卡并沿用根AGENTS/治理1.2/5.1、原06及minecraft-modding/testing/resource-pack技能，锁定版本不变。

唯一生产改动为BoilerSteamVisualState.alpha及其中文注释：零/非法严格0，正值`.18+.50*sqrt(clampedConcentration)`，最高.68；满库存外/内片alpha=.476/.238。真实库存/容量、14片和权重、实际air采光、混色/时钟/RenderType/深度写入全部未改。另一允许改动仅BoilerSteamRendererTest的CPU证据输出路径从06改到06R1，断言/fixture不变；没有新增测试或人为RED。

可确认原满库存顶点最高仅.126/.063，纹理遮罩和真实气室光继续削弱可见度；本修正只提高材质alpha，不满亮、不改玩法。原06的11/11和全面独立审查复用；旧06日志/XML/CPU图/JSON/manifest/frozen JAR保持。

唯一命令：`./gradlew.bat test --tests '*BoilerSteamVisualStateTest.realCapacityAndBothStocksDetermineConcentrationWithoutLiquidLevel' --tests '*BoilerSteamRendererTest.actualSubmissionKeepsLocalPoseAirLightAlphaAndSlowUpwardUv' assemble --console=plain`。实际exit0，BUILD SUCCESSFUL in 17s，9任务（4执行/5最新）；两个既有用例各1/1，共2/2，零失败/错误/跳过。新原始log/exit/XML保存于`build/reports/art/ART-BOILER-06R1/consumer/`；未重复其它测试、素材生成/安装/检查或启动游戏。

新`production-offline-sample.json`由既有测试实际消费生产Mesh/Frames/alpha输出。与旧06逐项比对：48%SC、phase3.25、22%/82%库存、像素遮罩/材质、14quad/片权重/真实汽区bounds均相同，仅lowAlpha/highAlpha变化：.103011→.414521、.166303→.632769。`production-offline-comparison.svg`在相同背景/neutralLight=1、相同straight-alpha与Z方向片权重条件下并排原06/06R1。它显示CPU可见度提升和汽区范围；不包含实际玻璃、气室光、深度/GPU排序，不能称游戏视觉通过。

新`candidate-manifest.json`冻结两源、9个实际VisualState相关class与新JAR，并以原06 manifest SHA链接其余依赖。仅核本批相关未变JAR entry54项（其它12汽雾class、40资源、两个Controller class）及其余专属源/共享getter仍与原06相同，不建全仓保护矩阵。9相关class与新JAR匹配；当前build/libs与新frozen JAR逐字一致。新JAR 2587696字节，SHA256 `dae3321befc092903905ba42234eda769bf53cad3e5d1bc9f3ee22f64d73b245`；新manifest SHA256 `ea3f40a336fbbbb4e7f202917e3b590c5c9a633f4c58c3703e5e357a9fc53119`。

本批完成即停写，交同一独立窄审与负责人；用户仍需复看真实观察窗/气室光下可见度。无Git、共享/服务端/构建脚本或游戏资源写操作，不关闭05B或管罐视觉门。
