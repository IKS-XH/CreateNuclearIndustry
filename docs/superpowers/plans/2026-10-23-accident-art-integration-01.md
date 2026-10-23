# ACCIDENT-ART-01：事故候选外观与功能联合交付

## 用户授权与协作

用户2026-10-23要求“让美术对话在这个分支里把外观也一起做了吧，一块测试一块合并”。由已任命的“美术负责人：贴图、建模与动画”对话`01a11c6b-f440-77c1-9275-5b2dca5cfa0f`规划/派发和交件，主逻辑PM管理核心文档、Git及最终合入，不另任命负责人。

本批用户明确指定在逻辑候选制作，作为治理1.2默认美术独立树的单批例外：唯一实施目录`E:/MyMC/NewMod/Create_NuclearIndustry-accident`，分支`codex/accident-thermal-04b`，起点`1985d1b3172a5efd1f0c87f3aa39ce3fe2a6feb7`，当前tracked干净。原美术树及主工程不改运行资源，不搬旧美术整包或回退逻辑。仍无Git写权限，不新建/切换分支；原测试世界原位保留，不从main覆盖。最新游戏日志2026-10-22 19:58:03全部维度已保存，2026-10-23派发核无Java游戏进程，不由美术启动用户世界。

美术负责人须先读主工程AGENTS、治理1.2/5.1/6、STATUS、本卡，以及[04B行为合同](2026-10-21-accident-thermal-04b.md)、[创造拆除修复](2026-10-22-corium-creative-removal-01.md)。技能实际入口`C:/Users/IKSXH/.codex/skills/`的minecraft-resource-pack、minecraft-modding、minecraft-testing；制品交PM按minecraft-ci-release办理。核实际Gradle锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6；不升级、不改协议或发布方式。

## 唯一目标与外观合同

制作一版完整可评审的熔融物、HOT/冷态残骸及事故燃料废料物品外观，代替当前原版熔岩/岩浆块/哭泣黑曜石/木炭借用图。沿既有Create风格和低分辨率SVG可编辑工作流导出PNG，不引入图像生成、粒子/音效/新玩法。由美术负责人决定具体构图、材质和色调，无需因常规造型选择重复询问主逻辑PM。

熔融态应可见熔融金属和废料混合特征、静止/流动连续；HOT有清晰高温辨识，冷态保留可识别凝固废料特征，物品小图与残骸相配。正常光照及较暗环境均可读，不靠光影模组才显示。不把发光颜色当辐射强度或实际温度的新状态；只消费现有HOT/流体渲染状态。事故火继续原版外观。

## 精确允许写集

R=`src/main/resources/assets/create_nuclear_industry/`。未列路径不写，确需扩写先给准确依赖，由主PM协调。

| 路径 | 允许范围 |
| --- | --- |
| R/blockstates/reactor_corium.json | 保持既有hot=false/true键，只调整本批模型引用 |
| R/models/block/{reactor_corium,hot_reactor_corium,molten_corium}.json | 本批模型/纹理/particle引用，不改游戏碰撞、渲染逻辑或原生有限流体表现 |
| R/models/item/{reactor_corium,accident_fuel_waste}.json | 背包/手持/掉落展示；注册ID和物品行为不变 |
| R/textures/block/{reactor_corium,hot_reactor_corium}.png及各自.png.mcmeta | 固体冷热纹理，可在既有状态内做轻量帧动画 |
| R/textures/fluid/molten_corium_{still,flow}.png及各自.png.mcmeta | 原生流体静止/流动sprite；格式/尺寸/帧序列正确 |
| R/textures/item/accident_fuel_waste.png | 本批废料小图 |
| src/main/resources/assets/minecraft/atlases/blocks.json | 仅追加上述两张流体sprite，原6项来源原样保留，不覆盖原版或其它流体 |
| src/main/java/com/iksxh/create_nuclear_industry/accident/client/AccidentThermalClientEvents.java | 仅getStillTexture/getFlowingTexture改用本模组sprite和相应中文注释；不加逻辑状态/同步/碰撞/服务端依赖 |
| tools/art-assets/accident-corium-22/ | 可编辑SVG、必要导出/预览工具及素材来源，不复写公共预览工具 |
| docs/art/tasks/ART-ACCIDENT-CORIUM-22.md；docs/art/reports/ART-ACCIDENT-CORIUM-22*/ | 本批分工、资源/预览/审查报告；任务状态与未闭门归主工程STATUS，不维护另一份全局状态 |
| build/reports/art/ART-ACCIDENT-CORIUM-22/ | 校验/预览/原始日志/精确文件SHA清单及待集成素材；普通编译输出允许 |

禁止其它Java、注册、NBT、配置、loot/recipe、语言、熔融物调度/溶蚀/冷却/伤害/辐射、库存守恒、创造拆除修复、管理员安全门、Gradle、用户世界及根跟踪JAR/日志的内容变更。新增手写注释中文，源码/资源原许可边界保持；原创素材和源稿MIT，第三方沿原声明。

## 交付与联合验收

1. 美术负责人按上述精确写集安排执行；本批原逻辑执行者停工，不双写。先交一版完整可评审素材；确有需用户选择的视觉取舍留给同一美术对话，不以普通贴图调整反复开逻辑测试。
2. 检查PNG/JSON/mcmeta/UV/atlas引用及真实运行模型，做可旋转/播放的浏览器预览，冷热/熔融流动/物品小图均可见；浏览器校验和Minecraft视觉体验分开记录，不以占位图或合成截图冒充游戏。
3. 仅必要增量compileJava/processResources；复用原04A/04B及创造拆除的既有业务证据，不重跑全JUnit/GameTest/长冷却矩阵，不运行build/clean/rerun，不自行打包或改正式JAR。一次组合窄审只读本批差异/既有证据，出现新具体问题才扩大。
4. 报告列精确写集、来源/输出SHA、技能入口、预览与检查结果、未观察项；完成后通知主PM。主PM核源并提交，再一次增量生成联合候选。用户在原事故世界集中查看新外观并复看创造拆残骸零掉落；不要求重复已反馈完成的熔融物整组功能。
5. 本次用户已指定联合交付，等这份联合候选手测通过后，由主PM将已验事故功能链、窄修及本批美术一并精确接入main并更新跟踪正式JAR。当前不提前合事故功能、不混入其它未验美术，也不自动推送/发布。
