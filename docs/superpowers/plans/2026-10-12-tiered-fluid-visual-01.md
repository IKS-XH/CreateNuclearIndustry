# TIERED-FLOW-VISUAL-01：Create原模型、调色和显示整改

> **For agentic workers:** 使用superpowers:subagent-driven-development，由主PM派一位执行者完成本卡并交一次独立规格/质量窄审。执行者不写Git、核心文档或派子代理，通用技能的提交流程/重复全量由AGENTS和治理5.1覆盖。

**Goal：** 按用户直接要求，把新增五种管泵阀换成Create原有模型及调色贴图，修复紫黑缺图、泵实际流向不可辨和新浮窗图标遮字。

**Architecture：** 复用锁定Create资源模型/UV/连接规则，按原像素确定性调色，绘图源用SVG；只增加必要客户端模型/方向绘制。浮窗复用已有GoggleTooltip.indentFirstLine，不改全局字体或Create。

**Tech Stack：** MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；实际本地依赖核对，不升级。

**Spec：** 用户2026-10-12直接反馈“新增的管道是没有贴图的紫黑色方块，新增的动力泵分不清输入输出，并且又出现了眼镜图标挡文字的问题，这几个新增的东西直接用原版Create的模型，将原贴图改个色调就可以了”。本卡是该已批准视觉整改，不新增流量/压力或玩法取舍。

## 证据、基线与边界

- 现有候选树E:/MyMC/NewMod/Create_NuclearIndustry-fluid-network，codex/tiered-fluid-network-flow，生产a5d1fe6、交接58d3e23。继续本树本分支，保留已测试世界，不搬动/回写/新建存档。
- 当前run/logs/latest.log在18:04、18:07明确ceramic_fluid_pipe/valve的minecraft:block/smooth_quartz贴图缺失；该ID不是实际PNG。PumpBE与ValveBE首行直接tooltip.add，没有接现有图标避让辅助。
- 五ID、配方、流量8192/32768/16384、4096SU、现有口2048/4096、拓扑/预算、流体库存及交易、持液泵搬移和禁运全部保持。只有模型匹配所必需的碰撞几何可精确适配，不改连接/流向算法、状态身份或物理角色。
- 复用Create普通fluid_pipe、mechanical_pump、fluid_valve模型/UV。二级钢灰、三级浅陶瓷，工业泵钢灰；保留原高亮、阴影、轮廓及透明度，不能整图抹平或AI重画。Create模型/材质出处、依赖SHA和许可证/署名实际核对并保留，不改本项目许可证。
- 原Create管有完整多分支/弯头资源；按现六连接布尔生成对应multipart，不把原FluidTransportBehaviour加到独立网以换取外观。泵复用原body/item/cog，方向标记依据现outputDirection与Create同步实际getSpeed：反转/扳手翻向均一致；停转不伪造运行。必要渲染只读，不能凭客户端修改服务器。
- 阀保持现axis/六连接/开度/红石规则；适配原模型至现状态。普通Create阀多属性不得直接粘贴成不存在的状态组合；原开度控件可见并保留现几何命中。不得为外观改变原红石安全/加载边界。
- 原543/543、20工业及相关回归和一次审查复用，本批只定向资源/方向/浮窗与必要增量assemble。不重复完整build、GameTest热工矩阵、旧版本兼容或无关思索。人工门只追加五新部件六向/弯头/分支/调色、正反转箭头及两浮窗。
- 读主目录最新AGENTS、治理5.1/5.2、本卡、minecraft-modding/testing/resource-pack，systematic-debugging/TDD/verification-before-completion；C:/Users/IKSXH/.codex/skills和plugins/cache/openai-curated-remote/superpowers/6.4.2/skills。手写注释/Javadoc中文。
- 当前sandbox只允许主目录写，本候选同级目录需按精确任务路径申请require_escalated；已有用户授权不是任意目录权限。失败先报告实际自动审查原因，不转写main功能代码绕过隔离，不更改用户权限设置。

## 唯一执行者允许写集

J=src/main/java/com/iksxh/create_nuclear_industry，T=src/test/java/com/iksxh/create_nuclear_industry。

| 路径 | 允许内容 |
| --- | --- |
| J/industrial/IndustrialPumpBlockEntity.java、IndustrialValveBlockEntity.java | 仅首行接GoggleTooltip，必要增加已同步方向显示；不能改库存/NBT/调度/额度/红石逻辑 |
| 新J/industrial/client/** | 仅工业泵Create原cog/流向显示、必要阀原模型指针、Dist.CLIENT注册和纯方向/模型适配；不建立流体能力/新状态同步或包外生命周期 |
| J/industrial/IndustrialPipeBlock.java、IndustrialValveBlock.java、IndustrialPumpBlock.java | 仅模型所必要的getShape/getCollisionShape匹配，原连接、放置、扳手、掉落缓存和流向行为逐字保持；额外路径先报PM |
| assets/create_nuclear_industry/blockstates与models/item中仅五ID.json；models/block中五ID及原arm子模型；新models/block/industrial_fluid_visual_01/**；新textures/block/industrial_fluid_visual_01/** | 原Create模型/纹理适配与完整引用；旧arm不可遗留smooth_quartz错误引用；不写assets/create、不覆盖其他设备或现有美术工具 |
| assets/create_nuclear_industry/lang/zh_cn.json、en_us.json | 仅新方向/必要视觉说明键，不改已验设备正文 |
| 新tools/art-assets/industrial-fluid-visual-01/** | 独立SVG调色源、确定性生产/验证工具和Create来源清单；原低分辨率像素/alpha保持，不调用全设备producer |
| 新T/industrial/IndustrialVisualContractTest.java；原T/industrial/IndustrialDataContractTest.java | 实际资源递归引用、Create模型几何和64连接组合、六朝向/流向一致与首行避让；原7个multipart数量断言应改为真实语义，不改配方成本/产量/禁运或跳过测试 |
| docs/reviews/2026-10-12/tiered-fluid-visual-01/IMPLEMENTATION.md；build/reports/tiered-fluid-visual-01/** | 精确交付/来源/命令/原日志及JAR清单；其他docs只读 |

## Task1：单批视觉整改

- [ ] 核实际日志与Create锁定JAR资源、模型、纹理、原机械泵可视化及现工业泵正反方向；列必要新增文件/路径缺口，先核资源及client-only边界，不凭字符串存在判断图能加载。
- [ ] 最小RED：资源解析发现smooth_quartz实际缺图、首行未避让；方向用实际入口作六方向/正负转速检查，避免复制同一算法写伪回归。资源测试解析父模型、#纹理变量和外部Create/Minecraft纹理到实际PNG，并检测循环/悬空引用。
- [ ] 只在本树实现Create原模型/调色SVG与PNG，泵原cog/方向绘制及两首行避让；原流体/动力热工源码未授权区域保持。
- [ ] 定向运行IndustrialVisualContractTest和IndustrialDataContractTest；如形状/方向适配有必要编译调用，核原getSpeed/server表达相同。只跑受影响组，不启动用户客户端或写世界。
- [ ] 必要一次增量assemble，冻结资源/编译类与JAR相符及SHA；不给原main替换包，不clean/全量/重复打包。若失败实际触发窄修及对应增量，保留原失败记录。
- [ ] 自审报告精确文件、源/产物映射、许可证/署名、定向结果、client-only/业务保护清单。PM安排同一次独立窄审，无必改后提交候选并更新原集中人工清单；用户视觉复看前未合main，不重开已验设备全套。

## Review Focus与自审

1. 外部Create模型继承的#变量递归解析、item/particle也必须存在实际PNG；原错误引用需全部退出。
2. 64连接组合、六朝向、弯头/三通与Create轮廓同源，不能靠固定multipart数量或仅文件名证明。
3. 正/反转和FACING翻转下箭头与真实工业出液方向一致；零速/超载不伪造实流。
4. 客户端PartialModel/字体/renderer不会被专服加载，注册时序及原Createcog无重叠。
5. 泵/阀首行均实际调用旧GoggleTooltip辅助，消息参数/流量不改；所有受保护服务端代码无夹带修改。

本卡自审：五ID、原Create模型/调色、缺图、实际方向与图标问题均由Task1覆盖；没有新玩法或待用户取舍。原流量候选为需整改，旧自动/审查证据保留，整改视觉门并入同一次集中复看。
