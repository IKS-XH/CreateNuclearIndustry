# DEVICE-PONDER-04-TURBINE 实施报告

## 交付状态

已完成汽轮机三幕 Ponder 候选及 R1 教学整改，交付等待项目经理核对实际差异和客户端集中播放。没有启动客户端；源码、模板、合同测试和打包通过不代表镜头与字幕已人工验收。

改动限于本任务写集：新增 `TurbinePonderScenes.java`、`tools/ponder/turbine_scenes.py`、三份汽轮机模板、`TurbinePonderContractTest.java`；修改 `P1PonderPlugin.java` 仅增加七个构件入口和三条故事线绑定；中英文资源只增加本台 Ponder 文案。本报告是任务卡指定交付文档。未修改生产机制、配置、正式模型、配方、依赖、其他设备教学或核心文档，也未执行任何 Git 写操作。工作区原有 `logs/debug.log`、`logs/latest.log` 修改及工具 `__pycache__` 均保留未碰。

## 三幕与 R1 整改

搭建幕使用汽轮机正式八棱几何，三档直径3/5/7、长度5/8/11，中心轴连续、端面封闭、内腔留空；每个机体由地台支撑，展示位集中在26格基板范围内。小型机先显示两端轴和连续转子，随后逐一显示前端壳环、三个轴向外壳截面和后端壳环；每步均提示手持机壳右击，延伸提示指向已显示的外壳。后端端盖在接口观察前补齐，后续小档完整机体也会再次显示。进汽口与排汽口分别切换到可见侧观察；观察窗改到西侧合法中段槽，默认俯视时可见。之后逐档展示小、中、大机体及其尺寸与转子数。

通汽幕缩至15×9×11展示区，3×5×3机体落在地台上。超临界蒸汽输入管、普通蒸汽排出管、两端外接轴均单独可见，直管两端显式连通。输入储罐先显示1000mB超临界蒸汽、排汽罐为空；演示阶段将输入示值改为500mB，并在接收罐显示500mB普通蒸汽，借两罐液位变化展示流向。镜头分别转到东侧进汽口、西侧排汽口和两端轴。两端显示速度同时设为256RPM；断汽后先等待40tick，再将双轴显示速度同时清零，随后正文说明窗口均值回落及停机门槛。操作仅改 Ponder 临时世界的显示状态，不运行正式汽轮机账本或流体交易。

效率幕用相同紧凑机体与两路完整管线，以临时储罐的十tick累计量区分三阶段：低于门槛的10mB/t处理100mB且无动力；门槛16.2mB/t再处理162mB、动力从0.5倍开始；短档额定54mB/t再处理540mB、倍率到1.2倍。储罐显示的是累计样例量，不用满罐液位替代流量。后续文字逐档标出中档108mB/t与1.5倍、长档216mB/t与1.8倍，并说明40tick实际排汽均值。这里仍是临时视觉示例，不声称模拟服务端十tick交易。

全部玩家文案为短句且每次只显示一段。场景正文后额外等待20tick，以满足 Ponder 1.0.82 文本寿命并留出净间隔；隐藏已显示区段前等待15tick。没有演示事故、普通蒸汽驱动或未实现玩法。

最终文案将汽轮机排汽统一称为“蒸汽”，不再使用“普通蒸汽”。通汽幕分别说明超临界蒸汽由进汽口进入、等量转为蒸汽、排汽可送往换热器冷凝回水；效率幕使用“小型”指标说明10、16.2、54 mB/t阶段；搭建尺寸名称改为小型/中档/大型。移除了未被场景使用的 `operation.text_7`，英文资源及 Java fallback 与玩家含义同步，合同测试的文案数量期望也随之更新为6条。

## 实际技能与锁定版本

实际读取并使用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 和 `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`；并读取根 `AGENTS.md`、本任务卡及 R1 `review.md`。从候选仓库配置核对 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。另核对本机锁定 Ponder 源码的 `InputElementBuilder.withItem`、镜头旋转接口和静态 `showBigLine` 行为；未按更高版本技能示例升级技术栈。

## 验证证据

- 最终生成器退出码0：搭建模板 `(26,10,15)` / 720方块；通汽与输出、流量与效率模板各 `(15,9,11)` / 159方块。坐标界限、合法汽轮机占格、冲突检测、端口、NBT 压缩往返均通过。R1 结果记录在 `build/reports/extension/DEVICE-PONDER-04-TURBINE/generator-r1.log` 和 `generator-r1.exit-code.txt`；初版生成器日志保留在 `generator.log`。
- 最终定向命令 `JAVA_HOME=C:\Program Files\Java\jdk-21 .\gradlew.bat test --tests '*TurbinePonderContractTest' assemble --console=plain` 退出码0。汽轮机合同测试3项通过、0失败；增量 `assemble` 成功。测试读取三份实际 NBT，按 `TurbineGeometry` 核对三档占格与槽位，检查地台、两条管路及直管属性、双轴和储罐位置，并验证七入口、三故事线、双语文案、手持机壳提示、逐环展示与后端端盖时序、延伸提示锚点、镜头切换、临时蒸汽液位变化、断汽后等待40tick再清零及区段/文本寿命间隔。最终 R1 整改命令记录在 `targeted-test-assemble-r1-final.log` 与 `targeted-test-assemble-r1-final.exit-code.txt`；JUnit XML 为 `build/test-results/test/TEST-com.iksxh.create_nuclear_industry.TurbinePonderContractTest.xml`。
- 最终候选 `build/libs/create_nuclear_industry-0.1.0.jar` 为2,293,737 bytes，SHA-256 `F0A83F9ADA762A126BA5E85D8A44F017167C005A90B7E1B0990E10AA11F3A3ED`。已核对包内含三份汽轮机 NBT 与 `TurbinePonderScenes.class`。玩家文案收尾的定向 JSON/key 顺序核验通过；增量 assemble 退出码0，日志在 `build/reports/extension/DEVICE-PONDER-04-TURBINE/assemble-copy-edit.log` 与 `assemble-copy-edit.exit-code.txt`。按项目经理要求复用前一轮3项结构合同结果，本次未重跑合同测试；测试源码中的文案数量期望已同步。
- 此前 R1 验证曾先后遇到普通蒸汽注册字段引用错误、随后合同测试仍按旧区段显示断言；均已修正，记录保留在对应的 initial/contract failure 日志。本轮收尾复核又发现搭建壳环需要逐截面显示，以及断汽衰减需精确等待40tick；对应合同检查已加入并由本次最终成功命令验证。原实现阶段的 initial/contract failure、prelayout/presection success 和此前 R1 日志均原样保留，没有覆盖。

没有启动 `runClient`、按W打开并播放三条故事线，也没有做画面核验。实际窗体可见度、东侧入口和两端轴镜头、三档构图、管线呈现及文本遮挡仍待客户端播放确认；本报告不把自动化结果写作人工验收。
