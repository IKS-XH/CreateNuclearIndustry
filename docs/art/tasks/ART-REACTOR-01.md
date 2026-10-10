# ART-REACTOR-01：反应堆外壳与冷热端口SVG优化

授权：用户2026-10-09批准[PLAN推荐方案A](../PLAN.md#3-第一台设备方案art-reactor-01)，追加“主要提高辨识度和可视度，风格尽量贴近原版Create，绘制低分辨率素材使用SVG，不直接生成图片”。执行方法：独立执行子代理实现，美术负责人审查，不再重复询问这项授权。

基线：`6bcdbdd45d22029f7b60208bd14172e5c815ecad`。唯一实际工作目录：`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`；每个命令必须显式设置该workdir。不得在主目录、逻辑候选或其他工作树写入；不得创建/切换工作树。只读Git，交付未提交改动。

## 职责与必读

你是实现执行者，不是美术负责人或全局PM，不派发子代理、不修改需求/计划/治理、不做任何Git写操作。读取本卡、根AGENTS、治理1.2/5.1节、美术入口、资产盘点及共用工具README；实际读取并应用以下三项技能，报告用途：

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`

锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。新增手写Python注释/说明用中文。通用技能的提交/全套验证/额外设计门服从本项目范围和用户已批准的实施指令。

## 唯一资源写集

目标恰为七名：`reactor_casing_side`、`reactor_casing_top`、`reactor_casing_bottom`、`reactor_hot_port_side`、`reactor_hot_port_top`、`reactor_cold_port_side`、`reactor_cold_port_top`。

- `tools/art-assets/sources/block/<七名>.svg`
- `tools/art-assets/generated/block/<七名>.png`
- `src/main/resources/assets/create_nuclear_industry/textures/block/<七名>.png`
- `tools/art-assets/palette.json`仅七个同名条目；不重排/格式化其他内容。
- 新增`tools/art-assets/art-reactor-01/export.py`、`README.md`。
- 执行者报告`docs/art/reports/ART-REACTOR-01.md`。
- 本批证据`build/reports/art/ART-REACTOR-01/`及正常Gradle自身构建输出；不写其他历史证据目录。

其他资源、共用manifest/pipeline/export/verify、Java、JSON模型、语言、配方、Ponder、AGENTS与核心文档只读。美术管理者同期维护docs/art中的计划/卡片，别把这些并行文档差异当作你可修改的范围。

## 视觉合同

七图仍为16×16、RGBA全不透明；SVG使用既有严格整数rect子集、有限色板（每图最多16种实色），无抗锯齿、渐变、外部图片或直接生图。所有新游戏PNG必须从对应SVG导出。

外壳侧面用暖灰钢框、钢螺栓、铅灰压板与两块浅暖灰混凝土嵌面；去金黄色装饰。顶面同框宽与材质，采用封闭盖板接缝，去中央圆形部件符号；底面为封闭钢板/加固边。冷热口同结构钢法兰与黑色密封，热口沉红色、冷口蓝色染料环，并用色标短条形状或位置区分。**大轮廓与材料块优先、细节克制**，不要在16×16里塞满噪点。沿用现有cube模型：四侧复用side，上下复用top，不画新增面向箭头、旋钮、动态仪表或运动暗示。

先只读核对当前配方（钢/铅/混凝土、红蓝染料/pressure_fitting/seal_ring）。尽量参考本机缓存中原版Create6.0.10的外壳/机器贴图配色与螺栓/搭接语言，可读缓存JAR；不要安装新依赖或复制Create图片到游戏资源。主工程`tools/art-assets/create-style-samples-2026-10-08/`只读参考，不运行或写入该目录。将参考来源和原创绘制边界写进报告。

离线预览至少包括：原尺寸、最近邻放大、明暗背景、2×2贴图平铺、外壳/冷热口同屏的实际现行cube模型等距预览，并增加缩小后的对比、灰度或降亮模拟帮助评估辨识度。降亮仅作为模拟，不声称证明游戏暗处可视度。预览不必增加复杂3D工具；按现有模型面材质做准确静态投影即可。

## 定向导出接口与检查

专用`export.py`直接从当前七SVG和共用色板读取，复用`tools/art-assets/export.py`的严格SVG渲染函数。硬编码七项白名单，不允许参数扩展目标。建议CLI：默认生成七项产物和预览、`--install`安装七项游戏图、`--check`只读检查七项游戏图与SVG产物一致；实现选定后在README写确切命令。全部输入校验并准备成功后再写目标，只更新字节变化的目标。失败不得安装部分游戏图。

开工先在本批证据目录保存189张游戏PNG当前哈希及七项原图，用于before/after与非目标保持核对；正常数量若变化按实际数量记录，不硬编码总量。严禁写/改旧baseline。共用manifest仍指向原七SVG，所以同步七个色板条目保证后续共用工具可继续读取；不执行历史全量install/verify。

验证安排（一次覆盖本批，不跑全量）：

1. 七SVG/PNG尺寸、颜色、alpha、路径；三种模型引用不变；非目标游戏PNG哈希保持。
2. 一次定向重复导出一致；一个代表性非法SVG输入试验证明失败不安装（可用内存/临时证据副本，不破坏正式源稿）；写前校验检查。保留实际命令/输出。
3. 用本批导出器产生准确预览并自己看图；无需伪造“测试必先失败”的绘图测试。
4. 只有最终候选做一次`./gradlew.bat jar`，日志写本批证据。核对JAR内七张PNG与游戏源文件字节一致。Java21若不在PATH，使用现有环境/工具配置，不安装Java、不改构建脚本。构建失败交具体错误与可用环境，不反复全量重跑。
5. `git diff --check`和精确文件差异核对。不启动客户端，不跑GameTest/clean/全量测试，不改其他任务状态，不做Git提交。

报告写实际基线、技能、修改文件、材料/参考、SVG来源、实际验证与限制、人工预览路径、未决项。完成后向负责人只返回简短状态、预览/报告路径、必要检查结果和风险。需要越界时暂停相关片段并报告。
