# 自动化接口17B C稿主分支集成

2026-10-18，用户要求“反应堆自动化接口的外观重置也合进来”。按此指令合main并更新已跟踪正式包；仅接已选C稿“继电模块匣”两个资源，不合尚待人工验收的事故第二批或其它未验内容。

- 美术准确来源`4c354d9caf8cbeeb60763f113b3142200f65f12d`，从art-studio只提交指定模型/PNG，原其它dirty、原稿、预览与报告保持。
- main原基线`a00b1b908cd933e048e45cdf78a82ecd0a6915a8`，在现有干净集成树接入两个资源，提交`6fb97c40dfd50d0bf6c8a1835cd609374f514a51`后无冲突快进main。Java、注册、配方、交互、构建规则、版本和许可差异均为0。
- 模型`models/block/reactor_automation_port.json`为363 bytes/SHA256 `03497963005d2f891974ff54cadba15d70f4d45a69a82d53368acaa02edaa870`；原生cube_bottom_top，侧面/粒子用新PNG，顶底沿现行外壳。新增PNG`textures/block/reactor_automation_port_side.png`为237 bytes/SHA256 `f9cecc1fcb5f9b4348e222dbc82154f07a77f565a7fe5d978a041fe591449d13`。两文件与原17B集成清单逐字相同，原item/blockstate引用保持。

唯一构建执行者在`Create_NuclearIndustry-du-accepted-integration`运行一次增量assemble，14秒/exit0，compileJava UP-TO-DATE；不重跑JUnit/GameTest、网页/客户端或已完成像素/预览/组合审查。原17B卡、审查和集成清单位于保留的art-studio，完整构建记录及实际技能见[BUILD](BUILD.md)。锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。

正式包为上述准确提交的一次Gradle原输出，3490000 bytes，SHA256 `0763c210f654ccb1e3679e6d70ec9fb3a648f43f29997ef1685dace1b4fbcbed`。2436个entry全部解压/CRC核验，748class与1687资源及manifest逐项绑定构建输出，2137个来源输入与准确Git来源绑定，构建前后输入相同。PM复算SHA、复制到已跟踪build/libs路径并核暂存blob与实际文件完全相同，未重封包。身份见[ARTIFACT](ARTIFACT.json)。

相对前正式81E，748class、manifest及其它二进制均原字节相同；内容变化只有目标模型及PNG。另1325项文本仅LF→CRLF，已逐项严格UTF-8/换行计数/归一字节/准确Git blob证明无内容漂移。初次“只允许两项原始字节变化”失败与完整诊断保留在原始报告，不重打包消除证据。大清单只保存在集成树`build/reports/automation-art-accepted-20261018/`，不复制到核心文档。

新批次复用隔离树前核main世界已于01:47正常保存退出、session.lock可独占读取；原目标完整备份后携带main世界27文件/13337721 bytes并核0差异，未启动世界。main47项原dirty原始SHA保持，暂存区不夹带日志/工具/存档；源美术树与事故候选均保留。源树根旧JAR/四日志还原，有准确原输出供后续追溯。

本次用户指令授权资源合入；原像素/原生引用/生产PNG预览和组合窄审结果按未变资源复用，没有补记未报告的Minecraft逐面、采光或多人观察。当前结果与其它独立人工门只在[STATUS](../../../STATUS.md)维护。
