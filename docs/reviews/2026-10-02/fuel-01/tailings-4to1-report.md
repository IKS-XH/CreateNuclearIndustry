# EXT-A-FUEL-01B：尾矿四合一压块

- 基线：候选 `31d02944aa4649b7c9225ba07a3d32502d150a59`，MC 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280。
- 技能：已读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 与 `minecraft-testing/SKILL.md`；前者用于对照本地锁定版 Create 配方/工序，后者用于遵循卡片限定的非游戏静态验证。
- 改动：删除旧 `recipe/pressing/uranium_tailings_brick.json` 的 1:1 入口；新增 `recipe/compacting/uranium_tailings_brick.json`，以四个相同的 `nuclear_materials/uranium_tailings` 标签输入产出一个 `uranium_tailings_brick`，没有加热或加工时间字段。Create 原生 compacting 配方省略结果数量时产出 1 件。
- 格式依据：本机锁定 Create sources JAR `create-1.21.1-6.0.10-280-sources.jar` 中的 `data/create/recipe/compacting/ice.json` 将同一 `minecraft:snow_block` 输入重复 9 次并产 1 块冰；相邻 `data/create/recipe/pressing/copper_ingot.json` 是单个 `c:ingots/copper` 输入、产铜板。该对照确认多份同类物品通过重复 ingredients 表示，压块使用 `create:compacting`，单件压片则是 `create:pressing`。

静态检查通过：新 JSON 可解析；四个输入项均为目标标签；结果恰为一个尾矿砖；无加热/时间字段；资源配方目录中仅有一个尾矿砖入口，旧 pressing 文件不存在。原始记录见 [`static-check.log`](./tailings-4to1/static-check.log)。

仅运行一次增量 `.\gradlew.bat assemble --max-workers=1 --console=plain`，结果 `BUILD SUCCESSFUL`（4 项任务中 2 项执行、2 项为 `UP-TO-DATE`；Java 编译为 `UP-TO-DATE`）。完整输出见 [`assemble.log`](./tailings-4to1/assemble.log)。JAR 核对确认新 compacting 配方存在、旧 pressing 配方不存在，新条目 SHA-256 与 Gradle 处理资源一致；记录见 [`jar-check.log`](./tailings-4to1/jar-check.log)。

按任务卡未运行 JUnit、GameTest 或游戏。客户端人工验收时确认工作盆内 3 个尾矿不加工，补第 4 个后产 1 个尾矿砖，8 个尾矿产 2 个尾矿砖，并在 JEI 中显示 4:1；此次配方交付不替代 01A 与主任务的其他人工门。执行者未运行 Git 写操作。

## 项目经理交付记录

修复候选提交为`53b0fad`。高速执行者`centrifuge_fix_finish`完成配方与一次增量打包；独立审查者`centrifuge_01a_review`只读核对四份输入、原生压块格式、无加热及旧入口移除，通过且没有重跑测试。PM读取构建和JAR原始证据并核对提交写集。当前待原客户端清单第2项的3/4/8份尾矿及JEI复测；01A五项与整批人工门仍未获得通过确认，未合入main。
