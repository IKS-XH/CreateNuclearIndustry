# EXT-A-REACTOR-01A 独立复审

**基线：** `c83ca0a4c8c72340b1ed58e2c39b8645087b0654`。复审只读检查本批九条配方路径、更新的 GameTest、执行报告、原始日志与最终 JAR；未运行 Gradle、测试或生成器。

未发现违反三项用户要求的配方或运行逻辑问题。铅锭标签与无色玻璃的混合配方使用普通加热、100 tick 并输出一个铅屏蔽玻璃；燃料柱工作台配方保持 `S/G/S` 且产量为1；三条新 `create:mechanical_crafting` 配方与冻结的 `CST`、`DS/CR`、`PE/CM` 布局、单件输入、单件输出及不可镜像设置一致。旧四条序列配方已从源码和最终 JAR 移除，控制棒组件原有序列配方仍保留；注册、运行代码和素材没有本批改动。

执行证据记录6/6 required GameTests通过，`assemble`成功。测试实际验证铅玻璃无热时不消耗输入、普通加热后消费1铅锭与1玻璃并产1件，也检查铅板不匹配；燃料柱工作台产量为1。三个动力合成配方的自动检查仅确认已加载的原生配方类型、尺寸、按序配料声明、镜像开关、单件输出及普通工作台输入不匹配。自动测试**没有**验证正向 `MechanicalCraftingInput` 匹配、错位布局拒绝或真实动力合成器逐格消耗；这些仍需客户端手测，报告和任务卡对此已有明确边界。

原始 GameTest 日志曾记录隔离目录缺少 `server.properties` 并出现设置加载错误，但服务端继续启动、加载配方，6项 required tests 通过并正常关服；构建日志为 `BUILD SUCCESSFUL`。最终 JAR SHA-256复算为 `8DC7E6447A3D210C6063BF1781EFF57A86DDDA0D89928A91FC9DF34DD55D0082`，与报告一致。JAR静态清单包含5条新/更新配方，四条旧序列路径为0，原控制棒序列仍在。

用户手测仍待进行：加热铅玻璃、铅板不适用、燃料柱工作台产量、三种动力配方的JEI与真实连接/逐格消耗，以及旧序列路线消失。本复审不表示这些验收门已通过。

**实际使用技能：** `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 用于核对锁定版本与Create原生机械合成/搅拌配方；`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md` 用于区分数据加载检查、真实Mixer加工与机械设备人工验收。版本按任务卡锁定为 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82。
