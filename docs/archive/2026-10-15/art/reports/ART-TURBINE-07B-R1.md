# ART-TURBINE-07B-R1 端盖试用版交付

R1将B方案端盖改为分块钢板表面：细八角法兰、固定像素螺栓座、四块稳定钢色板、暗拼缝及邻接高光、凹暗轴口与分层轴座，并保留少量铜嵌片。端盖atlas为128×256原创整数rect SVG，像素密度16 px/格；d3/d5/d7及未成型端面使用同一规则。负责人已复看三档实际OBJ新旧对照，允许安装试用；用户客户端视觉仍待复看。

精确安装写集为mapping中的164个endcap消费者OBJ的endcap面UV，加正式端盖PNG，共165项。UV基线和旧预览固定读取保留的07B generated OBJ/PNG；48个middle OBJ不写。其他顶点、法线、面、mtllib、非endcap UV、壳面贴图及专用MTL保持原样。没有Java、测试、模型几何或游戏运行改动。

原创资源和映射见[候选目录](../../../../../tools/art-assets/turbine-endcap-art-07-r1/README.md)，确切目标与源SHA见[mapping.json](../../../../../tools/art-assets/turbine-endcap-art-07-r1/mapping.json)。

检查与安装证据保存在 `build/reports/art/ART-TURBINE-07B-R1/`：`candidate-check.log`记录bundled Python `build_candidate.py --check` 的exit 0，输出SVG/PNG字节同源、128×256 atlas、48个铜色像素、两处实际短筋采样、164 OBJ、1744个endcap vt变更及非endcap vt/其余OBJ记录保持、MTL引用有效。安装前逐项核对07B基线和formal目标SHA；`install.log`记录164 OBJ及1 PNG复制成功，`install-byte-check.log`记录165/165逐字一致。

旧JAR完整备份为 `pre-assemble-create_nuclear_industry-0.1.0.jar`，SHA-256 `328072b13f941705fc86e8d62df7b13e6b90b1f9a3fc3851c34cfa279380167c`。唯一执行命令为 `.\gradlew.bat assemble --console=plain`，见`assemble.log`，5秒成功，exit 0。`jar-check.log`记录JAR内164 OBJ与1 PNG共165条目均与候选字节一致。当前JAR SHA-256为`4724e54c45e18b7c1917e3272fe3cf2b8e217b89f6cfc7fc0466f957955810b9`。

实际读取并应用的技能入口：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`。锁定版本保持MC1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。此批无行为变化，因此未新增或运行JUnit/GameTest；没有启动客户端。

