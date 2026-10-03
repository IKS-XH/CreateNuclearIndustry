# EXT-A-FUEL-02E-R1 客户端活动模型加载整改报告

- 基线：候选 `8670d3f0a1c1cec6250712b4a3d0dace66cdea0a`；Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Flywheel 1.0.6。
- 执行者：仅实施02E-R1；未执行Git写操作、未改资源/存档、未启动或关闭客户端及用户进程。
- 实际使用技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`、`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/systematic-debugging/SKILL.md`。

## 根因与修复

Flywheel 1.0.6 的 `PartialModelEventHandler.onRegisterAdditional` 只枚举当时已存在的 `PartialModel.ALL`，`onBakingCompleted` 再按该集合回填烘焙结果。三个屏蔽装配台 `PartialModel` 原先只在 `ShieldedAssemblyRenderer` 的静态字段初始化时创建；事件监听器把 `ShieldedAssemblyRenderer::new` 方法引用交给注册表，没有可靠地在模型收集前主动初始化renderer类。资源JSON和纹理存在、日志没有相应路径加载报错，不能排除模型注册时序遗漏。PM转述的三机位同点紫黑块和活动件消失符合多个未烘焙partial使用默认missing模型的表现。

NeoForge 21.1.219 锁定源码的 `ClientHooks.initClientHooks` 标注其在Minecraft构造期间、初始资源加载前运行，并先发布 `EntityRenderersEvent.RegisterRenderers`。该事件现在显式读取 `ShieldedAssemblyRenderer.PARTIAL_MODELS`，触发类初始化；不可变列表创建三个partial，并由renderer复用。renderer仍只经标注 `Dist.CLIENT` 的事件类加载，专用服务端边界不变。

## 修改文件

- `src/main/java/com/iksxh/create_nuclear_industry/production/ShieldedAssemblyClientEvents.java`：注册renderer前读取partial列表，并更新客户端事件职责Javadoc。
- `src/main/java/com/iksxh/create_nuclear_industry/production/ShieldedAssemblyRenderer.java`：将三个private字段组合为不可变列表，在静态初始化时创建，并以列表渲染。

Git只读差异确认写集仅为上述两个Java文件；资源、blockstate、纹理和其他源码未修改。

## 验证证据

- 一次增量命令：`$env:JAVA_HOME='C:\Program Files\Java\jdk-21'; .\gradlew assemble --console=plain`，退出码0。Gradle报告4项任务、2项执行、2项最新。编译期间出现原有 `EventBusSubscriber.bus` 弃用警告；assemble成功。
- 编译字节码证据：`EXT-A-FUEL-02E-R1/compiled-event-bytecode.txt` 显示 `registerRenderers` 首先读取 `ShieldedAssemblyRenderer.PARTIAL_MODELS`；`compiled-renderer-bytecode.txt` 显示类静态初始化创建left_arm、right_arm、fixture三个partial。NeoForge与Flywheel锁定源码摘录及来源SHA256在同名证据目录。
- JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，SHA256 `C68426519B1A5B402107FE6641438093B6F671213C52D02D1E76DB4826CFF2FB`，见 `EXT-A-FUEL-02E-R1/jar-sha256.txt`。三个partial JSON和10张机身纹理均在JAR中，逐项JAR/工作树SHA256一致，见 `EXT-A-FUEL-02E-R1/resource-jar-compare.txt`。
- 候选客户端日志没有屏蔽装配台partial模型或对应机身纹理的加载报错，见 `EXT-A-FUEL-02E-R1/client-model-errors.txt`。症状上下文和来源边界见 `EXT-A-FUEL-02E-R1/client-symptom-context.txt`；截图情况是PM转述，本执行者未声称亲眼观察。
- `git diff --check`通过。02E的5/5服务端GameTest与素材证据沿用已审交付；本轮未重跑GameTest、JUnit、资源生成器或完整测试。

## 验收边界

本次确认编译产物包含初始化访问、三个partial及机身纹理打入JAR，并由锁定依赖源码支撑时序根因；没有运行客户端，故不能声称玩家画面已修复。后续需用户完整重启候选客户端，确认主控紫黑missing块消失且左右臂、夹具可见。原有四组人工验收门仍未因此判定通过。
