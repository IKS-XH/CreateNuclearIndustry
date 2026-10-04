# EXT-B-EXCHANGER-01E 交付报告

在 `zh_cn.json` 与 `en_us.json` 为护目镜状态 `gui.create_nuclear_industry.heat_exchanger.state.dedicated` 补齐翻译：“向高压锅炉供热” / “Supplying heat to high-pressure boiler”。其余逻辑、状态读数与布局未改。

`HeatExchangerState` 的全部可达状态均有中英文键，`line_unavailable` 回退键也覆盖。两份 JSON 解析通过且无重复键；一次增量 `./gradlew assemble` 成功，JAR 内新键与翻译值已核实。未运行JUnit/GameTest。

实际使用技能：`minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`。版本核对为 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82。

原始验证摘要：[validation.txt](EXT-B-EXCHANGER-01E-evidence/validation.txt)。后续仍需用户在相同护目镜场景确认中文显示。
