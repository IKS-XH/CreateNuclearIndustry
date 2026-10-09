# DEVICE-PONDER-07-ASSEMBLY 独立规格与质量审查

2026-10-10，主PM派发的一次独立窄审。审查树为 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，基线及当前HEAD均为 `03e3fbeee747b9042c3468eba49bc0c5f29365f2`；实现未提交，因此同时审读跟踪差异和实际新增文件。本报告仅记录审查结论，不改变任务状态或验收门。

**规格结论：通过本批静态与自动证据审查。质量结论：通过本次窄审。必改项无。** 未发现需要分级整改的具体缺陷；可由PM登记本台播放候选。此结论不代表客户端播放通过或可跳过用户验收。

## 审读范围与依据

实际读取根AGENTS、文档入口、活动三幕计划、治理5.1/5.2、`reviewer-brief.md`、`review-package.txt`、IMPLEMENTATION，以及当前三幕Java、专属合同测试、生成器、插件/双语净差异。正式合同只读核对 `ShieldedAssemblyLayout/Structure/Block/BlockEntity/PartBlockEntity/State/Recipe/Renderer`、两正式配方、相关材料标签和 `SpentFuelPayload`；模型及blockstate仅用于确认本批模板的原有加载与几何边界。

实际读取并应用 `minecraft-modding`（临时客户端世界与正式服务端生产边界）、`minecraft-testing`（实际NBT/配方合同和原始JUnit证据）、`minecraft-resource-pack`（双语与打包资源引用）、`requesting-code-review`（独立规格及质量结论）和 `verification-before-completion`（以实际文件与原始证据支撑结论）。本地依赖保持MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；技能的新版示例、Git写与重复运行步骤服从任务卡和治理。本审查不再派代理。

## 规格与行为核对

- 插件新增仅为三幕ID列表和 `shielded_assembly_station` 物品入口；原六台入口/顺序保持，未给无物品代理新增入口。每种语言恰好追加12键，原键值与顺序保持；逐段核对Java中文fallback及英文含义，三幕均为标题加3段正文，无开发措辞或重复Create基础说明。
- 独立只读解压三份实际NBT，均为7×6×7、66格且无实体：49格地台、完整八格机体、唯一底轴及物流。主控 `(3,2,3)` 朝北且expanded；七代理PART符合 `x+2*z+4*y`，同一owner与压位MasterPos对应。按正式Structure判定逐项核对归属条件，代理没有第二份库存；此处未启动世界调用 `complete()`。
- 轴仅在主控正下方 `(3,1,3)`，axis=y；全部设备y≥1。北侧两个上下输入器及西侧两个上下输入器均朝外，extracting=false，背面对应机体的合法外露面；北侧另一面extracting=true，经下方朝西原版漏斗进入 `(3,1,2)` 桶。顶部溜槽位于上层格 `(4,3,4)` 正上方，只在接口幕显露。没有内侧能力口或埋地演示。
- `ShieldedAssemblyPonderScenes.java:60` 起的四种实际栈为8/4/2/1，封装从第88行起为三种各1；材料注册身份与正式配方/标签对应。第171行起只用正式ItemStack保存格式和State.load写临时主控，Operation/RecipeId/Costs/Work/Progress符合正式现行入口；工时为25600及12800 RPM·tick，参考64RPM约20秒/10秒。默认新组件damage=0，封装第98行通过正式 `SpentFuelPayload.seal(spent)` 复制原栈进入CONTAINER，不使用无载荷JEI模板。
- 逐项投料后只向主控写累积库存；完成快照清输入并写单件输出；第159行起先移出主控产物，再写承接桶。三幕不调用正式生产advance/tick或便携账本setter，没有正式服务端物流事务、功能/配方/配置变更。
- 整台揭示、随后底轴及接口揭示符合三幕分工；新燃料和封装使用独立空批次模板。投到西面时短转25度，随后复原；工作状态投影覆盖八格。正文辅助方法第197行起等待duration+20，已读本地1.0.82的FadeInOutInstruction原始字节码证据，正文生命周期duration+10，因此段间至少10tick净空。几何及指令时序能支持播放候选，不能证明实际镜头、实体轨迹或字幕观感。

## 原始验证与制品绑定

读取 `build/reports/extension/DEVICE-PONDER-07-ASSEMBLY/` 原始证据，未重新运行Gradle、客户端、真实服或资源生成。

| 证据 | 实际核对 |
| :--- | :--- |
| `01-red.log/.exit/.xml` | 原定向运行exit1，3测试、3断言失败、0错误/跳过；分别缺模板、场景和入口，属于本批合同red |
| `03-templates.log/.exit` | 原生成器exit0，三模板各66格、回读和确定性记录；审查另独立解析现有gzip/NBT核对布局，未重生成 |
| `04-final.log/.exit/.xml` | 原定向test＋增量assemble exit0，3测试、0失败/错误/跳过，BUILD SUCCESSFUL；本审查复用该结果，不称新运行 |
| `05-artifact-check.py/.json/.log/.exit` | 阅读检查脚本与结果；当前9实现文件及报告SHA与冻结一致，实时/冻结JAR SHA一致，7个资源/class入口与当前源资源/编译class逐字节一致 |
| `06-diff-check`、状态及日志保存证据 | 本卡跟踪写集diff检查无空白错误；实际Git脏项符合原两日志/三pycache及允许实现/报告。两日志当前字节与original-logs备份一致，未清理原脏项 |

冻结JAR SHA256：`7bafdd14a47d940da34b42390b1a06084965bc1fdf374689f26c2e46a10143d4`。三模板SHA256均为 `67f03765acb3d1e5d5273cdcec15ebe47892bb0438a98b0dfbd3ff9df77645f2`。同布局由三个不同Java故事板编排，资源/class加载路径对应。

专属3个测试真实读取NBT、语言、正式配方及当前插件/场景源入口；源字符串断言仅证明约定入口存在，不能证明客户端执行效果或封装运行时往返。质量结论还依据实际State、Recipe、Payload及场景调用的独立审读，没有将字符串检查扩大解释成游戏行为测试，也未放宽正式核心约束以获取green。

只读API查询核对Ponder物品创建、实体tick和直接方块实体修改边界；物品实体保留普通客户端tick，正式机器服务端事务不在本批演示中运行。模型边界及视角参数只构成最低几何条件，最终动态可见性仍归用户播放门。

## 未验边界与停止位置

用户仍需播放本台三幕，确认完整机体/唯一底轴、四种入口与成品承接清楚，配料、压缩加工和一次封装连贯，镜头/物品运动及字幕不遮挡、重叠或裁切。自动证据不关闭这个门。

未重测已验收正式加工/封存、前六台教学、真实服或旧存档；本批不触及这些机制，依治理5.1/5.2复用并保留既有证据。ART03运行动画及02R1材质连窗的独立视觉门均保持未通过，未合main或推进其他工程主线。本审查唯一写入为本REVIEW.md；未修改实现、核心文档或执行任何Git写操作。
