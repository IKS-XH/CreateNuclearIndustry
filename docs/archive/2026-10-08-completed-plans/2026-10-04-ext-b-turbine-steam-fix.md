# EXT-B-TURBINE-STEAM-FIX：蒸汽名称与缺失材质整改

状态：资源整改及PM审查通过，等待随结构新版合并视觉复测；与尚待细节确认的汽轮机结构改版分开。本卡只修用户明确报告的蒸汽名称和紫黑材质，不改变加工与流体身份。报告及构建日志已归入[01A后续修复证据](../../reviews/2026-10-04/turbine-01a/evidence/steam-fix/EXT-B-TURBINE-STEAM-FIX.md)。

## 合同与权限

- 用户来源：2026-10-04 本轮手测反馈第3项，将“普通蒸汽”显示为“蒸汽”，修复游戏中的紫黑材质。
- 工作目录：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，基准 `01c816f`；保留日志、pycache和其他既有改动。主目录不写功能。
- 执行者仅可修改下列资源及报告；不做Git写操作，不修改治理/核心文档，不派发子任务。
- 必读：AGENTS、治理5.1、01A卡；技能 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md` 和当前superpowers的systematic-debugging技能。
- 版本：Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82。使用当前图集格式，不升级依赖。

## 允许写集

1. `src/main/resources/assets/minecraft/atlases/blocks.json`：仅追加蒸汽两张纹理的显式图集来源，保留已有条目。
2. `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`、`en_us.json`：仅修改本批 `steam` 显示名和汽轮机文本中对排出蒸汽的称呼，保留键名和占位符；不改变超临界蒸汽名称。
3. `build/reports/extension/EXT-B-TURBINE-STEAM-FIX*`：诊断、资源检查与构建日志。

## 行为与验证

- 保留 `create_nuclear_industry:steam` 的注册ID、FluidType、PNG内容、无桶/无世界方块以及1:1交易。中文显示“蒸汽”，英文“Steam”。
- 先验证 `fluid/steam_still`、`fluid/steam_flow` 真实文件存在及客户端引用；该目录不因放入PNG就自动被方块图集扫描。若根因超出上述写集，先报告，不扩大修改。
- 资源修复不新增镜像实现的单测，不重跑服务器GameTest。核对JSON、两处sprite引用、PNG解码和旧图集条目保留，再跑一次 `./gradlew.bat processResources`，把退出码与资源复制结果记入报告。
- 不启动用户客户端/世界，不宣称静态检查证明游戏视觉已经通过。留一个合并复测点：Create储罐/透明管中的蒸汽材质和护目镜名称正常。
- 交付报告列出实际技能、根因证据、准确改动、命令/退出码与视觉待验收边界。由PM审查后维护状态。
