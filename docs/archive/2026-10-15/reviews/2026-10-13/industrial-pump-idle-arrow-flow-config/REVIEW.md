# 工业泵静止箭头与端口实例配置独立窄审

2026-10-13，一次独立规格/质量窄审；基线 main `6d1baba697079f2c88c86db550d70bc0e0dfb3a1`。已读 AGENTS、活动卡、当前三源码差异、锁定 Create 速度入口、正式端口/仪表消费链、IMPLEMENTATION、原日志/XML、配置备份与最终来源/包审计。复用已读 minecraft-modding/testing、requesting-code-review 和 verification-before-completion；技术栈保持锁定版本。仅写本报告，未实现、测试、构建、派发或 Git 写。

**结论：当前精确范围内无剩余必改项，可登记客户端局部复看候选；静止箭头和现场端口流量尚未人工验收。**

生产仅改 `IndustrialVisualOrientation`：读取原 FACING 和同步理论轴速，零/正速沿 FACING、负速反向。运行时仍与原服务端 outputDirection 一致；超载/原生冻结使真实 getSpeed 返回零，纯 IndustrialFlowBudget 仍产生零运输额度。原 Renderer、glyph、六面旋转入口、服务端 pump/transport 与 R2 素材保持，不新增方向持久状态或运输许可。两个原测试覆盖六向×七种明确动力状态，最小 ClientLevel 夹具只提供原生 TickRateManager，实际速度/显示入口及 FaceBakery/SuperByteBuffer 算法照常执行。

配置为本实例覆盖值修正。P1ServerConfig 默认2048；正式 ReactorCoolantFluidHandler 每个物理端口额度及 ReactorInstrumentPortBlockEntity 冷热账本均读取同一 perPortFlowMbPerTick。已独立解析当前 TOML：原备份100、当前2048；去掉该键后全部字段相等，当前字节严格等于原文件的唯一100→2048替换。原配置先备份，其他热工自定义、编码/换行/注释保持；未改注册/默认值、其他实例或强制迁移。当前文件值不冒称正在运行的旧客户端/服务端已经重新加载。

证据区分保留：01原两个方向用例分别因零速返回null失败，属于正确行为RED；02首次GREEN已有glyph与速度预算两项通过，方向夹具因 pump.flowLimit 触及未装载JUnit配置失败，不登记为生产故障；仅测试改用真实getSpeed接原纯预算入口，03同组最终XML为3/3、零失败/错误/跳过，17秒exit0。04唯一assemble为1秒exit0，jar实际执行。05首次包审计错误预期仅外层class，实际同源switch伴随类亦变化；原失败保留，javap差异明确仅LineNumberTable行16→21，指令/异常表/局部变量表一致，06修正两制品集合后冻结exit0，没有重构建。没有重跑R2/桶/管网全矩阵或原2048端口组。

最终独立只读绑定：16项来源/依赖SHA零漂移；当前配置SHA为 `1c43d5bfbb736dc60c9a3f585e80161d1ffc2c4e0cab780a3c4ba6120954af1a`，原备份为 `7b30ec8e812bcb67e5d4d5d69fbedb081df6f4ae35e345afb384f483b1ddef10`。正式与冻结JAR实哈希均 `745e936c60c473ad2a47f83b9d683a0ae0dc227fdf07024523395edbc196c361`，3,046,392B。实际逐文件读取与R2 b0包对比：路径集合相同，仅 IndustrialVisualOrientation.class 与其 $1.class 两个制品变化，其余2148文件保持，包括全部素材、桶与正式端口消费链。

开工标准b0包已被用户移出 build/libs，任务备份来源是原R2冻结b0，不能表述为覆盖用户移出的包。用户原位261012/ecbe包实哈希保持。已读5887开工文件保护审计，任务外保护变化为空，仅测试框架正常更新两日志；任务没有世界写入，用户正常游玩保存不算任务修改，也不据此声称所有世界字节未变。

保留两项人工门：停转/超载箭头可辨且预定方向正确；配置在实际服务端加载后，供液/动力/管路条件充足时单个冷热口2048mB/t上限。四个同向口理论合计8192，不把2冷2热、产热或管路瓶颈混计成保证流量。Sodium/Iris按用户要求暂停；已验R2、管网、汽轮机及其他独立门保持，不展开旧存档兼容研究。
