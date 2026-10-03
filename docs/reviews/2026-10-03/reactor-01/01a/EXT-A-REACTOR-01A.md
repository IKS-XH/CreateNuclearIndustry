# EXT-A-REACTOR-01A 执行报告

## 实施基线与技能

- 实现基线：`c83ca0a4c8c72340b1ed58e2c39b8645087b0654`。
- 实际读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 与 `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`。
- 技术版本保持锁定：Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82。

## 实施内容

- 新增 `mixing/shielded_glass.json`：1个铅锭标签输入和1块玻璃经普通加热搅拌产1块铅屏蔽玻璃；Create 加工参数为100，不表示固定5秒；无副产物。
- 修改 `crafting/reactor/reactor_fuel_rod.json`：原竖列钢板/钢格架布局不变，产量从3改为1。
- 新增3条 `create:mechanical_crafting` 配方：仪表端口 `CST`、换料端口 `DS/CR`、控制棒驱动器 `PE/CM`；均禁止镜像并各产1件。
- 删除上述玻璃、仪表端口、换料端口和驱动器的四条旧序列配方。控制棒组件原序列配方未改，旧半成品身份与素材未动。
- 更新 `ExtensionReactorCraftingGameTests`：燃料柱工作台配方产量、铅玻璃配方声明和实机搅拌、旧序列ID移除、机械配方注册对象/输入形状/输出、合法结构和24块燃料柱检查。

## 验证

唯一构建与测试命令：

```powershell
.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_reactor_crafting -PgameTestDirectory=build/gametest-reactor-crafting-01a assemble --console=plain
```

结果：`compileJava`、隔离 GameTest 组、`jar` 与 `assemble` 均成功；6/6 required GameTests passed。实际运行覆盖了铅玻璃搅拌无热不消费、普通加热后精确消耗1铅锭和1玻璃并产1块，工作台燃料柱产量为1，机械配方已加载对象的类型、尺寸、按序输入身份、镜像开关和单件目标输出，以及四个旧序列ID不再加载。机械配方工作台输入拒绝已覆盖。保留的结构断言仍确认正式模板含24个燃料柱方块。

未运行JUnit或其他测试组。机械配方测试检查已加载配方对象的输入声明，不声称执行了三条配方的正向 `MechanicalCraftingInput` 匹配、错位坐标拒绝或真实动力合成器逐格消耗。Create 6.0.10 的 `MechanicalCraftingInput` 构造器为私有，但可通过 `RecipeGridHandler.GroupedItems` 的公开构造、`mergeOnto`、`calcStats` 与 `MechanicalCraftingInput.of` 公开入口拼接输入；本轮测试已结束，不为增加该运行时matcher案例重复测试。该检查留给人工验收。未启动用户客户端，也未访问用户世界或存档。

游戏测试服务器日志开头记录了隔离目录未预先存在 `server.properties` 的提示；服务器仍正常启动，配方加载完成，6项测试通过并正常关服。详细原始命令输出见 [`verification.log`](EXT-A-REACTOR-01A/verification.log)，GameTest 服务器日志见 [`gametest-latest.log`](EXT-A-REACTOR-01A/gametest-latest.log)。GameTest 任务未生成JUnit XML。

## JAR 静态核对

产物：`build/libs/create_nuclear_industry-0.1.0.jar`  
SHA-256：`8DC7E6447A3D210C6063BF1781EFF57A86DDDA0D89928A91FC9DF34DD55D0082`

最终JAR含本卡五条现行资源（铅玻璃搅拌、燃料柱工作台、三条动力合成），且不含四条被删除的序列路径。逐项记录见 [`jar-resource-check.txt`](EXT-A-REACTOR-01A/jar-resource-check.txt)，哈希记录见 [`jar-sha256.txt`](EXT-A-REACTOR-01A/jar-sha256.txt)。

## 人工验收边界

请在锁定版本客户端检查铅玻璃需普通加热、铅板不适用、燃料柱工作台每次产1件，以及三个动力合成配方的JEI显示、实际动力合成、旧序列路线消失。三条动力配方的实机正向匹配与逐格消耗尚未由本轮自动测试证明。当前未宣称两批人工测试通过。
