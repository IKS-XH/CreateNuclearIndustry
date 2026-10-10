# ART-REACTOR-02B执行报告

2026-10-09，执行者`reactor_ct_assets`。本批客户端消费与14张game图候选已实现，定向自动检查通过；交负责人一次独立规格/质量审查。用户客户端视觉门待实际观察，不宣称可变尺寸反应堆玩法已实现。

## 合同、技能与写集

实际读取主工程及本树最新AGENTS、治理1.2/5.1、美术入口、02B当前任务卡、L1 HANDOFF/实施/独立审查与只读接口、02A冻结报告及独立报告。美术树实际HEAD为`8b83a1a97f1b539e6dcc7b6714b9ee96c13854e0`；调用已同步编译接口，不修改逻辑前置。

实际应用`C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-resource-pack}/SKILL.md`：按实际客户端MOD事件接入、以JUnit验证消费边界、保留原模型/窗口透明及资源路径合同。应用`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/test-driven-development/{SKILL.md,writing-good-tests.md}`：先真实断言red，最小编译占位不提供行为，测试运行生产判断/登记/作用域与真实模型接口，不读取源码字符串或重写索引算法。遇方向预期失败使用同目录`systematic-debugging/SKILL.md`核实锁定字节码；最终按`verification-before-completion/SKILL.md`以实际退出/XML/JAR字节和范围证据报告。

版本保持MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。治理5.1优先于通用技能全量验证/提交流程，复用L1已审26项JUnit、专服与02A矩阵，无重复GameTest/全量suite/导出/客户端/旧存档研究。未派子代理或执行任何Git写操作，每个shell均显式使用唯一美术工作区。

手写交付仅新增三客户端Java、一份`ReactorConnectedTextureTest`、14张`textures/block/reactor_ct/<原名>.png`、本报告及`build/reports/art/ART-REACTOR-02B/`证据。开工18个Java/测试/PNG目标实际均不存在；未改客户端共享入口、注册、逻辑API、AT、构建或任何原模型/语言/Ponder/配置。旧ART01七SVG/PNG和02A工具/源/产物保留。JUnit自动改写的两份tracked日志另述。

## 最终行为

- `ReactorConnectedTextureClientEvents`只订阅Dist.CLIENT MOD `ModelEvent.RegisterGeometryLoaders`，处理器同步直接`initializeOnce()`。依L1锁定时序，该同步事件早于ModelManager模型/atlas准备；无enqueue、geometry loader或重复Create监听器，专服公共入口不引用新增客户端类。
- `ReactorConnectedTextures`幂等登记14个RECTANGLE entry与七个block包装。Create登记键为`create_nuclear_industry:<block>`，原/target与02A mapping完整ID一致；game目标无`_ct`，工具源PNG有`_ct`。只用Create block model表，item模型保持原样。存储entry/ID，实际sprite由Catnip既有重载监听更新，无跨重载atlas sprite缓存。
- ScopedCTModel覆写protected `gatherModelData`，在整个super CT计算前一次`capture(world)`。全部面/邻接从同一不可变快照查询；ThreadLocal按上下文对象身份隔离，嵌套finally恢复、顶层remove，无作用域不重capture。继承的final getModelData及原生buildContext/UV算法保留，快照不带入getQuads。
- `getDataType`核对真实朝外面、局部当前BlockState ID、描述预期、合法材质与inclusive外平面；不可靠返回null。`getShift`按quad真实sprite ID选14项映射，未知图返回null。六参connectsTo核对两端实际局部状态、同dimension/owner/generation/revision、同bounds、同朝外外平面和相邻距离；七种合法表面材质可互连。无世界扫描、BE/NBT查询、区块强制加载或服务端修改。宽高深独立取bounds，未写死5。

## 实际验证及失败记录

原始命令与路径见[commands.md](../../../build/reports/art/ART-REACTOR-02B/commands.md)，证据统一在`build/reports/art/ART-REACTOR-02B/`。

| 阶段 | 实际结果 | 原始证据 |
| :--- | :--- | :--- |
| TDD red | 编译通过；10 tests，7 failures，0 errors；全部为AssertionFailedError；退出1 | `tdd-red.log/exit/xml`及两个无行为占位源文本 |
| 实现首轮 | 11 tests，1 assertion failure；退出1。新增原生北面方向预期写反 | `native-direction-first.log/exit/xml` |
| 最终`test --tests '*ReactorConnectedTextureTest' jar` | BUILD SUCCESSFUL；17秒；退出0；XML实际11 tests，0 failures/errors/skipped | `final-test-jar.log/exit`、`final-tests.xml` |
| 图集与JAR | 14个冻结源/安装目标/JAR entry逐字节相同；CT目录无额外PNG；21份原block/item/blockstate JSON及14份原sprite JAR字节保持；测试类未打包 | `installed-atlases.json`、`verification.json/log/exit` |
| 原输入与范围 | 899保护文件全部SHA256匹配开工，02A冻结报告另核其manifest SHA；三新增类只互相引用，无公共/专服引用 | `baseline.json`、`verification.json` |
| 空白 | 精确Java Git diff检查退出0，四份未跟踪Java另做实际逐行尾空白检查通过 | `verification.json` |

真实red异常包括`expected: <true> but was: <false>`、`Expected java.lang.IllegalStateException to be thrown, but nothing was thrown.`及快照对象assertSame不匹配。不是编译失败。首轮新增原生方向断言同为`expected: <true> but was: <false>`；锁定Create javap证明北面getRightDirection为WEST，因此东侧格应为left。仅改测试预期并重跑必要定向命令，没有改生产方向算法或掩盖原始失败。

11项测试覆盖手工六面几何/跨材质、各身份分量、局部替换/未知材质/缺记录、面/bounds/外平面/相邻距离、同一次发布变化、嵌套与异常恢复、未知/旧/无作用域回退、实际14映射/七注册键/幂等工厂、真实quad sprite选择和原quad保留。补充用真实已注册方块、仅替代外部局部读取的ClientLevel测试子类贯穿六参connectsTo、原生buildContext、真实final getModelData；局部首次读取撤销发布后整次super仍用进入时快照，可靠世界异常也清理作用域。测试辅助全部位于专属test文件，生产作用域/纯判断均实际用于模型路径。

899集合来源：开工全部既有main Java 277份、test Java 72份、02A工具/源/PNG 466份、02A证据31份、build.gradle/gradle.properties/AT 3份、七种原block/item/blockstate JSON 21份及14原sprite PNG共35份、ART01七SVG/七生成PNG/共享色板15份。仅核这些已有输入，不重复全资源扫描；具体每路径SHA保留baseline.json。

全树只读`git diff --check`本次实际退出2，报告两份自动日志第7行尾空白：`logs/debug.log`、`logs/latest.log`。JUnit原生日志与手写写集区分，保留原样，未回退/清理/修日志；负责人并行文档及既有ART01/02A候选也保留。初次Python读取Git stderr用默认GBK产生readerthread UnicodeDecodeError，初次输出保留`verification-encoding-first.log`；改证据脚本UTF-8读取后定向字节/范围核对退出0，无新增测试重跑。

## 制品与边界

本树JAR `build/libs/create_nuclear_industry-0.1.0.jar`，2367612字节，SHA256 `4cd8f15fd6108596cd37831bec7501b48b2add668ed83cecca648847350a9c71`。

| 新增源码 | SHA256 |
| :--- | :--- |
| ReactorConnectedTextureBehaviour.java | `9aa375a49a0d98800c9543e42870d34cad4622d87426703e97dba8de089ddfa2` |
| ReactorConnectedTextures.java | `282e1519d0b0de8caa3819ccc5bf9fb1d5ffb5d67b7a31af0bad880ae3a79092` |
| ReactorConnectedTextureClientEvents.java | `3d6e94f8b907b09889b4dc76b2fbb9a39671ac3f3b5649ea65d0479df90c921d` |
| ReactorConnectedTextureTest.java | `8b94ac1e678f32a7d4c3811272bcc0acee81ad50eb4f4af860e24c322954ceed` |

不重复L1同步/撤销/区块恢复/世界会话矩阵或02A像素/透明孔验证。自动检查不替代真实成型/拆坏、相邻owner、六面边角/冷热口/透明窗、管道遮挡、远近昼夜、跨区块重载、退出重进与资源重载的用户客户端观察；这些人工门仍由用户/已协调主PM执行。本批只消费现有成型描述并适配独立bounds，不交付可变反应堆玩法。

交付资源、代码、测试与报告冻结；最终哈希见本批`frozen-manifest.json`，待一次独立规格/质量审查。执行者不修改任务状态或作最终验收。
