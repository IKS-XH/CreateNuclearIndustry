# DEVICE-PONDER-01：离心机基础思索教学

**状态（2026-10-07）：** R1提示时序、R2玩家文案及R3回水布局均已整改；当前功能提交`c4e486a`已打包并经PM审查，等待同一台播放验收。人工门尚未通过，主线及下一台继续暂停。当前交付见[候选说明](../../reviews/2026-10-06/ponder-01/CANDIDATE.md)。

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

## R3：回水管路布局整改（2026-10-07）

用户截图指出回水管拐弯后与粉末输出朝向重叠，难以辨认。这是同一教学候选的可见性整改，不更改设备功能，也不代表此前播放门已通过。基线为同级候选`f0550dd`；原提示时序、八段文案、两路漏斗和反应堆四情景均保留。

去掉机身南侧回水管的向东弯折，改成南向直线独立回水支路：管道`(4,2,5)`、泵`(4,2,6)`朝南、回水罐`(4,2,7)`；泵的齿轮`(3,2,6)`和轴`(3,2,7)`取Z轴，马达`(3,2,8)`朝北。两路粉末仍从北侧和东侧输出。执行者须按锁定Create源码核对泵/齿轮/马达关系；同步更新展示选择、回水罐指向和速度指示位置，使用离散选择避免包围框包入两种粉末接收端。可在同段增加沿回水支路的流向标记，不增加字幕、不改正文寿命或关键帧时序。

执行者精确写集：`CentrifugePonderScenes.java`的回水坐标/选择/指示、`tools/ponder/centrifuge_scene.py`的对应回水布局和原有方向校验、`enrichment_centrifuge.nbt`，以及新报告`docs/reviews/2026-10-06/ponder-01/layout-r3.md`。不改语言、注册、测试、其他模板、玩法、模型、配置、构建脚本或其他核心文档。禁止Git写操作、再派发、客户端启动及终止既有进程；保留日志、缓存、存档和既有无关改动。

验证按治理5.1精简为：生成器重建并解码合法NBT、回水管/泵/罐方向及动力接线核对、差异写集与原字幕/时序/其他模板保护、一次候选`assemble --console=plain`及JAR包含当前模板核对。证据放`build/reports/extension/DEVICE-PONDER-01/layout-r3/`，报告记录实际命令/退出码和JAR大小/SHA。入口不变，复用已审合同证据，不新增Java测试，不跑JUnit/GameTest或功能复测。实际清晰度与关键帧回放合并进原离心机播放门。

继续实际使用本卡Minecraft开发、测试、资源技能和系统调试/完成前验证技能，技术栈不升级；单一高速执行者实现，PM审查差异和日志后管理Git。主线与其他设备保持暂停。

## 必读技能与版本

读AGENTS、治理5.1/5.2、本卡、既有Ponder类、现行离心机代码/配方；实际读取`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md`，及`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/verification-before-completion/SKILL.md`。

核对MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，并读锁定Create/Ponder源码核对API。不升级依赖，中文源码注释；技能全量/重复测试和Git要求让位于仓库规则。Python用`C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe`，系统入口为WindowsApps；Git只读，用`D:/Program Files/Git/cmd/git.exe`。

## R1：提示时序整改（2026-10-06）

用户截图显示第5段配比提示与第6段输出提示同时可见。锁定Ponder的`TextInstruction`继承`FadeInOutInstruction`，实际寿命为`duration + 2 × 5 tick`。首版5→6只等待90tick，但前提示需115tick；6→7只等待120tick，但前提示需160tick。尚不能记为客户端通过。

只允许执行者修改`CentrifugePonderScenes.java`中的等待时序及必要中文说明，和新增报告`docs/reviews/2026-10-06/ponder-01/timing-r1.md`。不改字幕、八段内容、入口、模板、语言、设备行为或其他教程。无需新框架或持久化Java测试。

全部八段逐项核对：每段正文完全退场后，下一段正文再出现，预留至少10tick净间隔；保留同段操作图标与物料动画。按当前原生寿命计算一份整改前/后的时间表，先证明上述重叠，再证明新时序正文可见数量不超过1。定向检查可放在`build/reports/extension/DEVICE-PONDER-01/timing-r1/`，只运行一次候选增量assemble并记录新JAR大小/SHA；已审入口、双语、NBT与设备功能证据复用。增加必读`superpowers:systematic-debugging`技能，原Minecraft/验证技能继续沿用。PM审查实际差异后交同一台播放复验，不派发下一台。

## R2：玩家文案精简（2026-10-06）

用户明确要求删去“画面仅作示意”“创造马达只是动力示意”“管道本身不会泵送”等开发措辞，以及重复的Create基础知识和不言自明的说明。教程聚焦设备特有的规则与玩家操作；这项文案要求覆盖首版上方必须在玩家字幕中标明示意的旧约定。真实耗时和客户端临时世界边界仍由实现及中文代码注释准确说明，不改变玩法或动画，也不在字幕里追加开发免责声明。后续设备教程沿用此原则；本轮只改离心机。

八段中文定稿如下；英文同步表达同一含义，场景Java的英文后备文案必须与`en_us.json`对应值完全一致，标题与键名保持原样。

1. 离心机将铀料浆分离为低浓缩铀粉、贫化铀粉和可回收的水。
2. 设备占两格高度，上方留出进料管道的空间。
3. 从底部输入动力，转速稳定后开始加工。
4. 铀料浆只能从顶部输入。
5. 每1000 mB铀料浆可分离出1份低浓缩铀粉、7份贫化铀粉和1000 mB水。提高转速可以加快加工。
6. 上下两段的四个侧面都能输出粉末和水。用黄铜漏斗分开收集两种粉末，用管道回收水。
7. 动力中断、转速不稳或输出受阻时，加工会暂停，进度会保留。
8. 轴承耗尽后，先停止动力，再手持1个重型轴承右击下段正面进行维修。

执行者精确写集：`CentrifugePonderScenes.java`的八个`.text(...)`字符串、`zh_cn.json`/`en_us.json`的本故事线`text_1`～`text_8`值，以及新报告`docs/reviews/2026-10-06/ponder-01/copy-r2.md`。不修改其他键、标题、注释、R1的duration/idle/关键帧、设备行为、模板或其他教程，不执行Git写操作，不再派发。

按治理5.1仅做双语JSON解析、改动键集合与旧值保护、Java后备文案一致性、差异及一次候选增量assemble。资源加载引用与R1时序保持相同，复用既有证据；不新增测试框架，不跑JUnit/GameTest/设备手测，不启动客户端。构建和临时检查证据放`build/reports/extension/DEVICE-PONDER-01/copy-r2/`，报告记录实际技能、命令/退出码、JAR大小与SHA。PM核对实际文案及写集后，合并进当前离心机播放门，不增加单独一轮功能测试。
