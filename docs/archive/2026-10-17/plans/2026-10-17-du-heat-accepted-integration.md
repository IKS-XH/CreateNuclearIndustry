# 贫化铀与锅炉队列已验内容联合合入

2026-10-17，主逻辑PM。用户确认“贫铀装备和对应的外观都通过手动测试了，合到主分支里吧”，随后追加“服务器端崩溃那个问题也通过测试了，一块合并进来”。此卡只管理三项已验净增量和正式制品，当前结果见[STATUS](../../../STATUS.md)。

## 来源与既有证据

- 贫化铀功能来源`0a10e361cee9a8069f8bd0b2a711f2cb60349797`，4项定向JUnit、3项真实菜单/加工GameTest及保存/exit0、一次组合窄审，用户本次确认功能手测。生产规则、13配方和原生升级不改。
- 对应美术为ART-DEPLETED-URANIUM-18B：art-studio的67文件集成ZIP，SHA256 `F1580CB2E645FBC3A15AF7227104A170147FAE7A885CA18311DB36C0F95918D3`，唯一manifest在`build/reports/art/ART-DEPLETED-URANIUM-18B/artifact/integration-manifest.json`。66资源＋一个公共护甲外观钩子，已验候选JAR `04466D8AC3ADA5BA5533D7EAA06F371C5AA9F7F6CB0144D18076BF2685F024EE`；原4JUnit、一次assemble和组合审查复用，用户确认外观手测。
- 锅炉队列来源`8712f4310eb1ff6ac8f2af03fde33e4154a71325`，交付分支`codex/heat-bridge-pending-fix`的02b581e；34项定向回归、一次assemble和组合窄审，用户确认原服务器观察通过。热工/依赖保持，不再诊断未确定的第三方触发者。
- main基准`90030bd`。使用同级干净detached树`E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration`，未动main无关dirty；三份合并冲突仅在核心文档，由PM保留main最新路线/验收记录，不存在代码冲突。

## 执行分工与写集

PM管理Git、核心文档和正式包；唯一执行者不做Git写或派Agent。执行者只允许：

1. 在集成树逐字安装上述manifest明确的67个生产路径，不自行修改生成器、资源、属性、标签、配方或API。先核ZIP/已验包/条目SHA和路径闭合，任一不符报PM。
2. 报告、来源/制品核验程序、准确Git输入导出和构建暂存只写集成树`build/reports/du-heat-accepted-20261017/`。正常Gradle输出允许保留，不提交；不改main、art-studio、原DU树或已验运行副本。
3. 第一段只安装和冻结来源，交PM提交。PM明确给出已提交生产ID后，才导出该ID的原始Git字节到报告内新空构建根，避免Windows资源CRLF漂移；原源码树保持，不批量改行尾。
4. 同一执行者执行一次增量assemble，不clean/全build/强制重跑/JUnit/GameTest/游戏。核所有导出输入与准确Git对象、打包class与本次编译输出、包含资源与已处理资源一致；发布包排除DU开发测试类/整个测试域。与main基线比变化只能来自DU净增量、18B和队列桥接，不带辐射、17B自动化口美术或工具内容。
5. 如果构建/载荷失败，保留证据并定位后回报，不改造JAR掩盖不一致；构建任务外的源/行为修补须先报PM。已验逻辑/依赖/资源未变则复用原审查和手测，不开额外审查层或重跑全矩阵。PM读实际日志、冻结输入和制品身份后才提升正式包。

## 工作区与保护

主目录日志已保存全部维度，独占只读打开main“新的世界”session.lock成功；目标此前不存在。完整携带main/run/saves与config，27世界文件、13337924 bytes、路径/数量/SHA零差异，记录在报告saved-world-sha256.json。禁止启动、编辑或回写世界，不重复扫描。

main原dirty和正式JAR删除现状已记录；正式制品提升是本次用户授权的版本操作，只有已提交来源并核验后才写入main/build/libs正式路径。其余dirty保留，不reset/stash、推送、发布或删除任何来源树。

实际技能使用minecraft-modding/testing核公共原生机制及证据复用，minecraft-resource-pack核批准资源/模型/饰纹，minecraft-ci-release核制品；同根`C:/Users/IKSXH/.codex/skills/<name>/SKILL.md`。Superpowers finishing-a-development-branch、using-git-worktrees、verification-before-completion位于`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/`，按治理5.1复用与已明确合入授权，不重复征询或清理树。MC1.21.1/Java21/NF21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6不变。
