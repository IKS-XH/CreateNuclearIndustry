# DEVICE-PONDER-03-BOILER 实施报告

## 结果与范围

已实现高压锅炉四条独立 Ponder 故事线，并生成四份完整模板。功能改动仅涉及锅炉 Ponder 场景、现有 Ponder 插件中的锅炉注册、锅炉 Ponder 双语资源、模板生成器和本批合同测试；未改锅炉、汽轮机或换热器运行代码、配置、配方、构建脚本、依赖、现有教学或用户世界。场景内的液位、储罐流体、汽口选项、动力转速和停机后的汽罐增量只写入 Ponder 临时客户端世界。

实施时读取并应用 `minecraft-modding`、`minecraft-testing`、`subagent-driven-development` 技能。实际依赖保持 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280 和 Ponder 1.0.82；从本机对应 Create/Ponder 源码核对了泵、滚动选项、共享文本和场景接口，没有升级依赖。遵守任务要求未派生子代理。

新增场景类、四份 NBT 模板、生成器和合同测试；修改 `P1PonderPlugin.java` 仅添加九个已注册锅炉部件与四个故事板入口，并为 `zh_cn.json`、`en_us.json` 各添加本批四幕条目。`PLAYBACK.md` 由 PM 管理，本报告没有修改它。

## 四幕布局与提示

四幕均使用同一台完整 7×7×7 锅炉，模板范围为 13×9×13，炉体原点 `(3,0,3)`；没有并排摆放第二台炉。炉内换热器位于底面，水区为 `y=1～2`，完整隔层为 `y=3`，汽区为 `y=4～5`。水口在正面 `(4,1,3)`、`(8,2,3)`，控制器在 `(6,2,3)`，冷液口在 `(4,3,3)`、`(8,3,3)`，蒸汽口在 `(4,4,3)`、`(8,4,3)`；热液口在底层正面底边非角点 `(4,0,3)`、`(8,0,3)`。所有侧面端口水平朝外。安全阀在顶面中心 `(6,6,6)`，不是顶部棱边；开阀上方保持净空。可见侧使用观察窗显示水位。搭建和运行幕分别隐藏正面壳层来讲解水区、隔层和汽区，讲解后恢复完整外壳。

1. **搭建与分区**：先展示底框及换热器，再切开正面讲水区；随后单独显示完整隔层、汽区和观察窗；最后恢复壳体并展示安全阀、多组给水/蒸汽/冷热液口及有效热回路数。文案顺序为尺寸范围、底层热交换与热口、隔层与冷口、水区端口、汽区端口、顶阀与多口、有效热力回路。
2. **接通并运行**：正面切面旁同时展示给水、底层热液输入、隔层冷液回收及各自储罐、泵、齿轮和马达。四组驱动均可见；外部马达/齿轮设正 `64` 转速，泵设负 `64`，并显示旋向及转速指示。给水与热液泵朝南，流向 `+Z`、由 `z=0` 储罐送往 `z=3` 锅炉；冷液泵朝北，流向 `-Z`、由锅炉送回储罐。箭头分别沿对应管线方向。文案顺序为默认尺寸、水/汽区容量关系、三路独立接管、冷回路堵塞、水区较大时升温较慢、汽区较大时升压较慢、护目镜读数。
3. **蒸汽输出与调压**：两个蒸汽口分别通过正面 `z=2、1` 管线连接 `(4,4,0)` 和 `(8,4,0)` 独立储罐；左路储普通蒸汽，右路储超临界蒸汽，并用白/绿箭头区分。汽口原生 `ScrollValue` 在临时方块实体上明确设为 `0=普通蒸汽`、`1=超临界蒸汽`，不是只画滚动提示；控制器显示 0～100 压力下限控件。文案顺序为按实际温压分类并分别记账、共用容量/压力且调压不改既有汽种、每口选择且只抽对应库存、压力下限不是目标炉压或安全阀压力、分管以免混流堵塞。
4. **停机与排查**：显示邻接控制器的红石拉杆并切换其供电状态，控制器出现红石指示。外接给水/冷热液泵及动力在红石切换后仍以正负匹配的转速运行；没有把停机误画成外接泵断电。切换后再填入两种出汽储罐，并显示给水、冷液回收和两种蒸汽的独立方向，表现余热继续产汽。随后通过隐藏/恢复给水、冷回路和一段出汽管，演示恢复检查路径；最后标出安全阀上方泄汽净空。文案顺序为红石仅停止锅炉新收热且泵仍运行、缺水检查、冷热液检查、积汽检查、阀门及净空。

每幕正文只用一个公共 `note` 辅助方法。普通正文参数时长为 90～105 tick；辅助方法空等 `duration+20` tick，而 Ponder 正文实际寿命为 `duration+10`，所以相邻正文至少有 10 tick 净间隔。运行幕护目镜提示时长 95 tick、空等 115 tick；红石交互、滚轮和方块切换均与正文错开。每条正文均附关键帧。玩家文案说明了默认长宽高各 5～11、水区与汽区分别决定相应容量、双汽库存共用总容量/炉压且不会因调压换种、出汽压力下限不等于目标炉压，以及红石停收热后余热仍可产汽；没有加入事故或未实现机制。

## 验证与证据

- 生成器：`tools/ponder/boiler_scenes.py` 成功，四模板块数依次为 243、264、249、270；检查了压缩往返、尺寸、唯一位置、边界、palette/state 索引、完整锅炉部件、端口层位/方向、安全阀顶面位置、泵流向及管线不穿炉腔。日志和退出码在 `build/reports/extension/DEVICE-PONDER-03-BOILER/generator.log`、`generator.exit-code.txt`（0）。
- 定向测试：`JAVA_HOME=C:\Program Files\Java\jdk-21 .\gradlew.bat test --tests '*BoilerPonderContractTest' --tests '*P1Ponder01ContractTest' --console=plain` 成功，exit code 0。合同测试检查故事板与九部件 ID、四模板存在性、NBT 索引/位置/端口和安全阀布局、四组泵与四台驱动马达，以及双语标题/正文 key。首轮曾有两个合同断言失败（故事板绑定与时序实现细节的过度约束）；按 PM 指示删除源文本/注释/写法镜像断言，保留实际入口、模板和 NBT 结构合同。首轮与修正后原始日志/退出码分别保存在 `targeted-test-initial.*` 和 `targeted-test.*`。
- 最终增量构建：`JAVA_HOME=C:\Program Files\Java\jdk-21 .\gradlew.bat assemble --console=plain` 成功，exit code 0；日志和退出码在 `assemble.log`、`assemble.exit-code.txt`。候选 JAR 已复制至本证据目录：`create_nuclear_industry-0.1.0.jar`，大小 2,282,008 bytes，SHA-256 `9CB361E0361607D2253B6218915E154E65F1A6EADC5F882C5BCB51F476CB3539`。
- 对两个语言 JSON 完成解析检查。限定到本批既有源文件的 `git diff --check` 未发现空白错误；全量检查提示的尾随空白仅在按指示保留的既有 `logs/debug.log`、`logs/latest.log` 中。既有三个 `__pycache__` 目录和两份日志均保留。

本批没有启动用户客户端或实际播放 Ponder；没有运行 GameTest、全量 JUnit、`clean` 或其他设备验证。默认镜头构图、储罐和窗口液位动画、汽口实际控件外观、字幕遮挡与关键帧回放，仍须用户在集中播放门中人工确认。静态模板、编译、定向测试和打包通过不代表人工播放门已通过；本报告交付后停在本台播放门。

## R1 独立审查整改

独立审查发现并要求同批修复五项：Ponder剖视选择未接回不可变 `Selection.add` 的返回值、停机故障演示未等待淡出且正文与故障错开、第三幕缺少实际控件调压与存量SC出汽规则、NBT合同只看 palette 未验证真实引用方块，以及运行幕未体现先给水/冷回收再供热。以下记录取代前文与这些问题相矛盾的描述。

- `frontCasing` 现在每次都将 `casing.add(...)` 的返回选择重新赋值，并按实际模板区分水口 `(4,1,3)/(8,2,3)`、控制器 `(6,2,3)`、冷口 `(4,3,3)/(8,3,3)`、汽口 `(4,4,3)/(8,4,3)` 和观察窗格。剖视隐藏正面其余壳格；水腔、汽腔与隔层可从正面露出，真实端口和观察窗保留。搭建幕讲解后恢复壳体。
- 第三幕用 `modifyBlockEntity` 从控制器 `getAllBehaviours()` 取真实 `ScrollValueBehaviour` 并设为 `10`，随后再次显示滚动控件；该 Ponder 方块实体在客户端临时世界，`selectMinimum()` 的客户端分支直接返回，不写正式服务端账本。核对了 `BoilerState.save/load` 中的 `Minimum`、`MinimumSet`，以及控制器 `Boiler` 子账本读写和 `synchronizeControls()`；本幕只改临时控件显示，不伪造服务端汽水库存或发起交易。普通汽储罐先显示，超临界汽接收罐在调低控件后才填入超临界流体并显示独立绿箭头；玩家文案明确说明已付热合格的存量超临界汽，即使低于新汽分类压力也能排至公共下限，且不改变汽种。
- 停机幕三处故障现在先 `hideSection` 并空等20tick（Ponder淡出为15tick），再开始相应正文；故障在正文期间保持可见。缺水段同时清空临时供水罐并降低窗口液位；冷回收段隐藏整条回路；出汽段隐藏普通汽出口。正文结束后恢复各路线/储罐或液位，继续空等20tick确保淡入完成，再显示恢复流向箭头。
- 运行幕改为分阶段操作：先显示并启动给水泵、填水罐和窗口；再显示并启动冷液回收、填两个回收罐；最后显示并启动热液供给。每步单独显示管线和转向，文案与实际顺序一致。正文总数更新为八条。
- 合同测试现在从NBT实际 `blocks[].state` 解析被引用方块和坐标，而非只检查 palette 名称。逐模板核对两处水口、两处热口、两处冷口、两处蒸汽口、控制器和安全阀实际方块；检查底面内部换热器、25格完整隔层及至少一个再加热段，并确认顶阀准确位于 `(6,6,6)`、其上方两格无占位方块。未增加测试类或模拟器。

R1没有改模板或生成器，因此未重跑生成器；反应堆定向合同测试沿用首轮已审证据。本轮只运行了必要的 `JAVA_HOME=C:\Program Files\Java\jdk-21 .\gradlew.bat test --tests '*BoilerPonderContractTest' --console=plain` 和一次 `assemble --console=plain`，两者均 exit code 0。R1 原始日志、退出码及JAR单独保存在 `build/reports/extension/DEVICE-PONDER-03-BOILER-R1/`；新版 JAR 为 `create_nuclear_industry-0.1.0-R1.jar`，大小 2,283,220 bytes，SHA-256 `0B3656542BD83E9B962895648BF165C0A840E748F65B7A3E93FFB5F1D8FC7AAE`。四幕英文fallback与 `en_us.json` 按当前正文顺序逐条比对一致。此次仍未启动客户端，故障显隐、窗口动画、压力控件外观及所有镜头/回放仍等待用户集中播放确认。

R1复核通过后补回蒸汽调压正文中“出汽压力下限可设0～100”的范围，并同步英文fallback、`en_us.json` 与 `zh_cn.json`；同一句仍说明将其调至10%只限制正常出汽，不设定目标炉压或安全阀压力。该文案修正后以 `assemble --console=plain` 做唯一一次增量重打包，静态资源对照与构建均成功（exit code 0）；没有重跑JUnit。新增最终候选 `build/reports/extension/DEVICE-PONDER-03-BOILER-R1/create_nuclear_industry-0.1.0-R1-final.jar`，大小 2,283,254 bytes，SHA-256 `A5EA3E8B371FB542CB171B4017392AF58981A2B37CA43347E198AF2FBD33FD3A`；日志与退出码分别为同目录 `assemble-final.log`、`assemble-final.exit-code.txt`。原R1测试日志、测试退出码和此前R1 JAR均保留。本次只改正文资源，模板/生成器/NBT合同未变化；仍未启动客户端，人工播放范围未变。

## R2 客户端首次进入崩溃整改

用户在本候选对锅炉按W后崩溃，原始报告由PM快照至 `build/reports/extension/DEVICE-PONDER-03-BOILER-R2/crash-2026-10-08_03.06.39-client.txt`。堆栈从 `PonderUI.tick` 进入 `hideSection`，在 `WorldSectionElementImpl.erase` 访问空 `section`。按任务指定读取本机锁定的 Ponder 1.0.82 源码：`showBasePlate()` 只是排入 `showSection(..., Direction.UP)`；其 `DisplayWorldSectionInstruction(15, ...)` 在15tick淡入结束后才并入基础区段。`hideSection()` 则直接排入 `scene.getBaseWorldSection().erase(selection)`，并为该选择建立淡出元素。搭建幕开头在显示基础板后立刻擦除未显示的整台锅炉，触发基础区段尚未初始化时的空引用；原有资源合同只覆盖NBT/注册，不覆盖此客户端指令生命周期。

搭建幕移除了整炉初始 `hideSection`。基础板改为四段Y=0周边选择，覆盖13×13底板但避开炉体x/z=3～9的7×7底层；随后炉底作为唯一一段显示，避免底板和模板炉底重复淡入。水区、隔层和汽区显示后各等待15tick再隐藏对应正面壳层，使选择完成合并后再剖视。其余本文件的隐藏点也已逐一核对：运行幕的正面剖视发生在完整锅炉显示和长正文等待之后；停机幕给水、冷回路和出汽管的隐藏发生在开场区域显示及多个正文/明确等待之后。本轮没有改其他场景行为或文案。

在 `BoilerPonderContractTest` 中新增一次针对该生命周期的静态回归：从实际搭建方法读取四段底板选择并计算覆盖坐标，确认周边底板完整且不与7×7炉底重叠；确认未显示整炉不先被擦除；并从实际指令顺序累计每个剖面显示至隐藏之间的 `idle` tick，要求不少于锁定API的15tick。该回归先以现状失败，再在修复后通过。它检查真实场景源码选择和顺序，但普通JUnit不能驱动客户端 `PonderScene.tick`、`WorldSectionElementImpl` 渲染/合并过程，因此不等同实际客户端回放；按本批范围未启动 `runClient`，客户端手动播放仍是后续门槛。

R2实际读取并应用 `minecraft-modding`、`minecraft-testing`、`systematic-debugging`、`test-driven-development` 技能；依赖保持 Minecraft 1.21.1、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82，未升级。先红后绿的定向命令均为 `JAVA_HOME=C:\Program Files\Java\jdk-21 .\gradlew.bat test --tests '*BoilerPonderContractTest' --console=plain`；最终合同红版exit 1（原因是未显示整炉被初始擦除），修复后exit 0，4项通过。证据目录还保留一份较早的初步红测日志：该版曾按最初假设要求等待后擦除整炉，PM审阅后改为移除这次不安全的擦除，最终合同红绿以 `targeted-test-red-contract.*`、`targeted-test-green.*` 为准。随后唯一增量 `assemble --console=plain` exit 0。测试日志、退出码、原始崩溃快照、构建日志/退出码和JAR位于 `build/reports/extension/DEVICE-PONDER-03-BOILER-R2/`。候选 `create_nuclear_industry-0.1.0-R2.jar` 大小 2,283,313 bytes，SHA-256 `9EC12EB5F923697D9D1D24A6140BB169180915C8BA6FF4CC2DA8F6C8F23FA17F`。未运行GameTest、全量JUnit或`clean`，未启动用户客户端，也未改世界、正式锅炉代码、模板、其他报告或核心文档。
