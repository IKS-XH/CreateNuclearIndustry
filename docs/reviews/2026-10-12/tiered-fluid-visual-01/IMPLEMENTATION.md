# TIERED-FLOW-VISUAL-01 实现交付

本报告是执行者交付，包含同一次独立审查P2必要整改，未代替PM审查结论或用户视觉验收。继续现候选 E:/MyMC/NewMod/Create_NuclearIndustry-fluid-network，codex/tiered-fluid-network-flow，HEAD 58d3e23，功能基线a5d1fe6。无Git写操作、无main同步、无客户端启动；用户run/saves与run/config未写。

## 实际范围

- 五新增ID采用锁定Create原fluid_pipe、mechanical_pump、fluid_valve父模型/UV。二级钢灰、三级浅陶瓷、工业泵钢灰。原中央管面片完整复用，六连接按现布尔绑定原rim_connector与rim；孤立管使用三轴原core面补齐六面。旧五ID/arm自制元素与smooth_quartz错误引用已退出。
- 现阀仅axis+六布尔；按X/Y/Z选择原vertical/horizontal模型，未粘贴不存在的Create原enabled/facing/waterlogged/axis_along_first属性。原阀pointer仅客户端读取0..100实际开度绘制；红石有效流量仍由既有服务端与原浮窗说明。现开度控件及命中入口保持。
- 泵body/item/cog同源，cog依照实际Create同步转速旋转。新增只读箭头在出液法兰四侧绘制，读取既有outputDirection，±RPM、FACING翻转均同源；getSpeed为零（含超载与冻结）不画实流箭头。未注册Flywheel实例visual，因此renderer直接画原cog，避免Create基类全局visualization跳过后丢失活动件。分件在客户端模型烘焙前登记，专服入口不引用图形类。
- PumpBE/ValveBE仅首行调用现有GoggleTooltip.indentFirstLine，原键、参数和后续行不变。Pipe/Valve仅getShape适配原模型尺寸；泵原AllShapes.PUMP未动。原拓扑、预算、许可、缓存、NBT、搬移、红石安全、配置、配方成本/产量及热工保持。
- 7张32×32原像素调色纹理由独立SVG矩形确定性导出；原alpha与明暗层次保留。1张8×8原创方向箭头也来自SVG。资源只在create_nuclear_industry命名空间，不覆盖Create或其他设备，不调用全设备生产器。

## 技能与版本应用

实际读取主目录AGENTS、治理5.1/5.2、活动视觉卡，并应用 C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md、minecraft-testing/SKILL.md、minecraft-resource-pack/SKILL.md，以及 superpowers/6.4.2/skills/systematic-debugging、test-driven-development（含writing-good-tests）、verification-before-completion；收到审查后实际读取并应用receiving-code-review。先以真实latest.log的18:04/18:07缺图与锁定资源/API建立根因，保存RED后实现，按治理范围做定向验证。通用技能全量/提交建议由本卡和AGENTS覆盖。

MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6未升级。锁定Create JAR SHA256 f0652bee27460f2d26a748f537cb5d687981441378d3a526d17ffaab5a1072bb。实际JAR没有LICENSE entry；准确版本mc1.21.1-6.0.10的官方LICENSE.md将assets列为All Rights Reserved、代码MIT，版权属于The Create Team / The Creators of Create。出处、原许可及每件原资源SHA保留在 tools/art-assets/industrial-fluid-visual-01/{README.md,CREATE-LICENSE.md,sources.json}，PNG中也保留署名/出处。模型通过依赖parent引用，不复制或重新声明几何所有权；本项目许可证未改。

## 本轮验证与原始证据

证据根 build/reports/tiered-fluid-visual-01/。

1. 01-red.log：最小IndustrialVisualContractTest真实5/5失败，分别是实际纹理加载、原连接面、原泵body/旋转、首行helper、缺少真实方向显示入口。19秒，Gradle失败。未把RED计为通过。
2. 02-intermediate-green-failed.log：一次Python辅助脚本默认GBK读取中文源码中断，导致shape/两测试旧断言适配未应用；随后定向7项仅5项通过、2项失败（旧multipart7数量与尚未追加原附件期望）。23秒。错误为执行脚本编码，不改被保护的生产算法。改为显式UTF-8并完成原附件语义核查；此失败如实保留。
3. 03-green-final.log及两个独立XML：./gradlew.bat test --tests com.iksxh.create_nuclear_industry.industrial.IndustrialVisualContractTest --tests com.iksxh.create_nuclear_industry.industrial.IndustrialDataContractTest，22秒自然退出0，7/7＋2/2，零错误/跳过。解析实际parent、#变量、item/particle/face引用至本地Create/Minecraft PNG；检测循环/悬空引用。两管各64组合核原中央/附臂面和UV；阀各三轴64布尔组合；泵六朝向原body/旋转；真实PumpBE正反/零速/overStressed与矩阵方向；两首行helper接入。首行自动项是接入合同，不宣称GPU图标排版人工通过。
4. 04-svg-png-validation.log：独立verify.py读取已保存SVG逐像素比较8张PNG，尺寸、原alpha、原纹理色阶保持；不改生产资源。原Pillow getdata弃用提示在producer改用当前get_flattened_data后退出；未修改依赖。
5. 05-assemble.log：首次 ./gradlew.bat assemble，1秒自然退出0，compileJava/processResources增量复用。没有clean、完整build、GameTest或热工重复测试。
6. 06-business-protection.json：四个修改生产类在获准首行/getShape之外逐字相同。freeze.py核旧候选JAR所有实际文件entry，只出现本批资源/显示class差异，无其他变更或删除。初次核查将ZIP新增目录条目误作文件差异，修正为只对实际文件条目比较后通过；不以目录误报修改生产内容。

编译显示仓库既有NeoForge EventBusSubscriber.Bus.MOD弃用、Gradle弃用及既有GameTest unchecked提示，定向/打包均成功；不扩展本卡升级。logs/debug.log、logs/latest.log与既有.superpowers、tools/ponder/__pycache__为运行/其他来源差异，不归生产源、不清理。受影响源路径git diff --check通过；全库diff --check有自动日志尾随空白，保留原日志。

## 同一次审查的阀指针P2窄整改

独立审查指出首次9项合同未覆盖阀pointer窗口可见性：原pointer位于Y=13.5/2.5平面，X/Z管轴遗漏原Create的Rx90后落在实心上下壳内；阀body默认为solid，原alpha0窗口也会写深度。逐上游模型/UV核实后，整改仅工业客户端指针矩阵、六个阀body/item包装及专项producer和测试。三种body均将pointer正X90转入局部Z=2.5/13.5窗口；Z体先同body绕Y负90，Y管轴加原Create竖直管的90度开度基准。仅阀body/item明确minecraft:cutout_mipped，这是NeoForge21.1.219真实内建ID；未使用不存在的neoforge:cutout_mipped。泵/管/原pointer的实际使用UV为不透明，保留原solid，未扩大渲染层修改。原模型几何、控件与服务端行为未改。

- 07-review-red.log：新增两个回归真实2/2失败，15秒，分别为窗口cutout缺失和精确指针窗口矩阵缺失。
- 08-review-green.log与review-两XML：同前两组定向命令，最终IndustrialVisualContractTest 9/9＋IndustrialDataContractTest 2/2，22秒自然退出0，零错误/跳过。新增真实pointer两个平面各端点/中心，三管轴开度0/100经过生产同源矩阵投影，必须位于原Z=2.5/13.5窗口并逐原主壳UV采样alpha0；窗口render_type同时核锁定内建命名空间。自动矩阵/资源合同不能代替GPU与鼠标观察。
- 09-review-assemble.log：整改触发第二次必要增量assemble，2秒退出0，编译/资源处理增量复用。首次JAR/日志保留，不声称全批只有一次打包。
- 10-review-delta.json：对首次视觉清单/包仅10来源及8个JAR文件entry发生必要变化；最终source-manifest.json仍154项、jar-verification.json仍143独立entry，全部一致。原功能候选JAR完整保护及四个服务端源保护再次通过。

## 冻结来源和制品

- source-manifest.json：154条精确src/tools来源，含8生产Java、2测试Java、131资源、13独立素材源与工具；每件SHA256/大小。报告单列，不自引用入清单。
- jar-verification.json：143个独立资源/class entry逐字一致，无不符；131资源、12生产class（含编译器生成枚举switch分件）。与旧候选包比较的全部新增/变更文件entry都属于这些来源；无删除/无其他业务class差异。
- pre-visual.jar保留原功能候选，SHA256 364ec4277fc8c8b8ca2a99ef114a86ab17e5fff73a311b905c18d4d21edbf053，不替换main制品。
- 首次视觉候选另保留为first-visual.jar：2,853,419 bytes，SHA256 79756f05a15eb738178463905c30ec336238fb2a1455eed4d4652af57afd9a5f；首次清单保留为first-source-manifest.json及first-jar-verification.json。
- **最终整改候选** build/libs/create_nuclear_industry-0.1.0.jar：2,853,790 bytes，SHA256 **be28ad02d98152532932595912dda375448bb46dce84abd183be0fb56db0d03b**。

本批复用原a5d1fe6的543/543完整build、工业20/20及相关真实代表域与独立审查证据，未冒称本轮重跑。无管网压力/管理员/兼容研究。尚待同一次独立视觉窄审及用户六向/弯头/分支、调色、正反转/翻向箭头、两浮窗与现阀开度控件真实复看；不自行关闭人工门或合main。
