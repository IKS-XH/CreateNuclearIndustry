# 高压锅炉三情景思索：验收与整合

**状态（2026-10-08）：** 用户要求删除重复的“停机与排查”，并表示其他页面效果良好。前三幕按此总体播放反馈通过，第四幕撤销；R3删减与定向复核完成，三幕已整合到main，唯一最终增量打包及封包核对通过，本批关闭。

## 定稿与人工依据

- 保留搭建与分区、接通并运行、蒸汽输出与调压三幕，九个锅炉部件都有这三个入口。
- 删除第四幕的绑定、方法、专属常量、生成器分支、模板和双语键；前三幕、R2显隐时序及三份NBT保持原样。
- 人工依据为用户“删掉停机与排查这一页吧，该说的前面几页都说了，其他的都挺好”的总体反馈；未附逐项操作记录，不声称代理另行播放或单独重测。纯删页不新增人工门。

## 整合与必要验证

原功能候选为`c113447`、首次进入与剖视修正`92f6d07`、R3删页`064fae9`；main以指定路径快照整合最终九个源码/语言/模板/合同/工具文件，功能提交`58a965e2f64c6643c1fb21092e1c88c613ff4ddb`。PM核对main与候选的`src`及`tools/ponder`差异为空，无代码冲突；其他设备教学、生产机制、构建配置及用户世界未改。

R3仅新跑一次`test --tests '*BoilerPonderContractTest' --console=plain`，退出0，4项零失败/错误/跳过；三份NBT与R2候选blob相同，注册仅三故事板，两语言无第四幕键。PM已读取实际日志/XML及差异；本次删页的独立复核见[审查R3](./review.md)，没有重跑R1/R2整套检查。R3证据保存在`build/reports/extension/DEVICE-PONDER-03-BOILER-R3/`。

最终main只执行一次增量assemble，退出0、6秒；三模板/双语/两个场景插件class共7项与资源/编译输出逐字节匹配，删除的NBT和双语键不存在。JAR为2,285,374字节，SHA-256 `0E5C60C18676A8229386C1536207AE3459240BFDC0325D5A50964EA72BA089D0`。PM读取实际日志、退出码和核对记录，并独立核对制品哈希；原始证据及`create_nuclear_industry-0.1.0-main-accepted.jar`快照在`build/reports/extension/DEVICE-PONDER-03-BOILER-ACCEPTANCE/`。未运行全量、GameTest、clean、生成器或客户端；不研究旧存档迁移。

## 使用与范围

本台教学已在主目录，可用`E:/MyMC/NewMod/Create_NuclearIndustry`下的`.\gradlew.bat runClient`启动。候选及原测试世界保留，不移动或删除。原[播放清单](./PLAYBACK.md)转为已验收记录，本批不要求再测；其他设备教学和工程主线不自动接续。

PM与执行者实际应用minecraft-modding、minecraft-testing、receiving-code-review、分支收尾及完成前验证技能；版本保持MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10/Ponder1.0.82。历史四幕、失败及R1/R2报告保留原时点含义，以本三幕定稿为当前状态。
