# DEVICE-PONDER-01：离心机基础思索教学

**状态：** 用户2026-10-06要求暂停主线，现有设备思索一个一个来，并明确选择先做离心机。本台功能`75265de`已完成必要增量打包及PM审查，[候选与播放清单](../../reviews/2026-10-06/ponder-01/CANDIDATE.md)已交付；现停在客户端人工门，不接着做下一台。

**执行方式：** PM自动派发单一高速模型执行者，维护核心文档/Git；执行者禁止Git写操作、核心文档/状态修改或再派发。候选仍为`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，PM先同步已验收main与本卡。主线功能、配方、模型、运行数值不改。

## 连贯场景及八个关键帧

保留现有11项实验反应堆入口和原故事板，只为`create_nuclear_industry:enrichment_centrifuge`添加同名独立入口/故事板。逐步出现真实设备、动力、管道、粉末和储罐变化，不以静态机器加长文字交付。

1. 说明用途：料浆分为低浓缩铀粉、贫化铀粉和回收水，不展示未实现辐射/事故。
2. 一件放出两格高完整机器，顶部留接管空间，上下段均正确呈现。
3. 底部转轴供动力，128RPM仅为示意工况；先稳定转速再加工。当前任意非零且稳定20tick可工作，不宣称128是最低速度；创造马达须标明示意，不能暗示管道自动泵送。
4. 顶部输入料浆，可见储罐/管道/有动力的泵演示流向，并显示已注册的真实料浆纹理。水平面不能进浆。
5. 演示当前`centrifuging/uranium_slurry.json`示例配比：1000mB→1低浓粉＋7贫化粉＋1000mB水；加工时长随转速。教学物料瞬时变化不声称是真实耗时。
6. 上下段四周出粉/回水：两只原生黄铜漏斗设置各自粉末过滤，接可见物品接收端；另侧管道回收水。真实粉末物品与储罐水量变化必须可见。可提合流再分拣，不再增加第二套大布局。
7. 断动力、转速未稳定或输出堵塞会暂停，排查动力与三种输出，使用护目镜观察；保留未完成批次，无GUI。
8. 轴承磨损到限后停机，停转后在下段用1重型轴承维修；通过操作提示/教学标记表达，不调用服务端维修事务。

玩家文案双语、镜头无遮挡、方向和连接准确；中文长度适合画面。仅修改Ponder临时客户端世界、动画和展示数据，不访问真实服务器库存、玩家数据或世界。

## 精确写集

- `src/main/java/com/iksxh/create_nuclear_industry/ponder/P1PonderPlugin.java`（只新增本台注册，旧列表/入口不改）
- `src/main/java/com/iksxh/create_nuclear_industry/ponder/CentrifugePonderScenes.java`（新增）
- `src/main/resources/assets/create_nuclear_industry/ponder/enrichment_centrifuge.nbt`（新增独立合法模板）
- `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`与`en_us.json`（仅本场景键，旧值不改）
- `tools/ponder/centrifuge_scene.py`（仅如需重建模板/校验，不扩建框架）
- `docs/reviews/2026-10-06/ponder-01/implementation.md`（本台唯一交付报告）
- `docs/reviews/2026-10-06/store-01/main-integration.md`（只记录已验收功能的main打包）

日志/构建证据可写`build/reports/extension/DEVICE-PONDER-01/`。不改服务端设备、初始化、材料注册、原反应堆场景、模型纹理、配置、其他教程或旧存档。

## 精简验证与人工门

先在刚合入的主目录增量`assemble --console=plain`一次，记录main源码版本、日志、JAR大小/SHA，不重跑已验收行为测试。然后实现本台，候选增量assemble及定向资源检查：入口/故事板/模板可解析、压缩NBT合法且尺寸/状态正确、双语键齐全、旧入口/语言值不变、JAR包含实际场景与模板。既有Ponder契约测试若直接受新增注册影响，仅跑该类。不添加复述实现的Java测试，不跑GameTest/客户端或旧世界矩阵；实际失败才定点复验。

报告记实际技能、基线、改动、命令与结果、JAR大小/SHA、关键帧概要和未测项。PM一次只读合并规格/质量审查，复用证据。客户端需真实打开、完整播放/关键帧回放，检查完整模型、动力/流向、三产物、过滤、双语可读性、镜头和原反应堆入口；用户通过后才做下一台。

## 必读技能与版本

读AGENTS、治理5.1/5.2、本卡、既有Ponder类、现行离心机代码/配方；实际读取`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md`，及`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/verification-before-completion/SKILL.md`。

核对MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，并读锁定Create/Ponder源码核对API。不升级依赖，中文源码注释；技能全量/重复测试和Git要求让位于仓库规则。Python用`C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe`，系统入口为WindowsApps；Git只读，用`D:/Program Files/Git/cmd/git.exe`。
