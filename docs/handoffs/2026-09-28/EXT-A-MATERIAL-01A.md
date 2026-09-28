# EXT-A-MATERIAL-01A 执行者交付报告

日期：2026-09-24。执行者交付实现与证据，**不宣布验收通过**。

## 结论

已在指定隔离工作树实现 `lapis_dust` 注册、创造页展示、双语、generated 模型、专用透明蓝色粉末 PNG，以及 `c:dusts/lapis` 和父 `c:dusts` 引用。新增 4 项 JUnit 和 3 项真实注册/标签/创造页 GameTest，未修改任何既有测试。

接单基线 261 项 JUnit 全通过；最终 265 项全通过，构建成功。**全量 GameTest 未完成：运行器 fastutil 空指针连续出现三次；整体 git diff --check 被测试自动改写的已跟踪日志尾随空格阻塞。** 源码 diff 检查通过。保留全部失败记录，停止重复尝试，交 PM 处理验收阻塞。

## 工作树和边界

- 工作目录：`C:/Users/lenovo/.codex/worktrees/ext-a-material-01a/CreateNuclearIndustry`，所有 shell 命令均显式使用此 cwd。
- 分支：`codex/ext-a-material-01a`。
- HEAD：`c8b8f8e2bc938d5c74a9b76f87eae087b2ce765d`；初始 `git status --short` 为空。
- 已读根 AGENTS、治理协议、活动 P1 计划、完整本任务卡、recipes 标签合同及生产准备计划 2.2。
- 已核对 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6、Gradle 9.0.0；GameTest 日志确认实际使用 `C:/Program Files/Java/jdk-21/bin/java.exe`。
- 无 Git 写操作、无核心文档/活动计划写入、无派发、无依赖或构建配置变更。未访问主工作区或 P1 客户端验收树；未启动 Minecraft 客户端，未操作用户存档，仅命令生成的自动测试世界。
- 未实现任何配方、加工参数或生存路线，未采用 D-03e 候选，未修改 P1ContentIds。

## 实际使用技能

均已读取并按仓库 1.21.1 基线应用，不套用 26.x 或 1.21.4+ 路径。

| 技能入口 | 应用 |
| --- | --- |
| `C:/Users/lenovo/.codex/skills/minecraft-modding/SKILL.md` | DeferredRegister 普通物品注册；保持服务端可加载，无客户端类引用。 |
| `C:/Users/lenovo/.codex/skills/minecraft-testing/SKILL.md` | 既有 JUnit、`@GameTestHolder`、`@PrefixGameTestTemplate(false)`、`p0_probe_empty` 模板；真实 registry、ItemStack 标签和创造页内容测试。 |
| `C:/Users/lenovo/.codex/skills/minecraft-resource-pack/SKILL.md` | 1.21.1 `models/item`、PNG alpha、双语、单数 `tags/item` 路径和资源链验证。没有新增 1.21.4+ `items` 定义。技能 Bash/jq 校验器未运行（当前 PATH 未提供 bash/jq）；本卡资源由新增 JUnit 和 jar 内容核验。 |
| `C:/Users/lenovo/.codex/skills/minecraft-imagegen/SKILL.md` | 内置 image_gen 生成独立粉末美术，再编辑为方形透明图；未借用青金石/染料原版图标。 |
| `C:/Users/lenovo/.codex/skills/.system/imagegen/SKILL.md` | 内置生成和编辑模式，保存工具输出到任务写集，保留生成来源与提示词，不使用 CLI/API fallback。 |
| `C:/Users/lenovo/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.1/skills/using-superpowers/SKILL.md` | 识别相关技能，用户授权与精确写集优先。 |
| 同根 `executing-plans/SKILL.md`、`test-driven-development/SKILL.md`、`test-driven-development/writing-good-tests.md` | 按已批准卡执行；先跑基线，再增加并运行失败测试；进度及证据限本报告目录，不建分支、不提交、不派人。 |
| 同根 `systematic-debugging/SKILL.md` | 读取异常栈和锁定版本依赖源码，修正本次测试的错误对照构造；写集外运行器问题保留证据上报。 |
| 同根 `verification-before-completion/SKILL.md` | 实际运行命令、读取 XML 和退出码，区分已通过与未完成。 |

## 手工修改清单（任务精确写集）

1. `src/main/java/com/iksxh/create_nuclear_industry/content/ModItems.java`：新增唯一 `LAPIS_DUST`，普通 `Item.Properties()`，保留默认 64 个堆叠；不增加别名、耐久、自定义组件或状态。
2. `src/main/java/com/iksxh/create_nuclear_industry/content/ModCreativeTabs.java`：展示此注册物品。
3. `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`：仅新增 `青金石粉`。
4. `src/main/resources/assets/create_nuclear_industry/lang/en_us.json`：仅新增 `Lapis Dust`。
5. `src/main/resources/assets/create_nuclear_industry/models/item/lapis_dust.json`：generated 父模型和专用 layer0。
6. `src/main/resources/assets/create_nuclear_industry/textures/item/lapis_dust.png`。
7. `src/main/resources/data/c/tags/item/dusts/lapis.json`：`replace:false`，仅追加正式粉末 ID。
8. `src/main/resources/data/c/tags/item/dusts.json`：`replace:false`，仅追加 `#c:dusts/lapis`。
9. `src/test/java/com/iksxh/create_nuclear_industry/LapisDustDataContractTest.java`。
10. `src/main/java/com/iksxh/create_nuclear_industry/gametest/LapisDustGameTests.java`。
11. 本报告及 `build/reports/extension/EXT-A-MATERIAL-01A/` 证据。

**自动产生的写集外 tracked 差异：** `logs/debug.log`、`logs/latest.log`。这两项虽然符合 `.gitignore` 的 `logs/` 规则，但已经被基线跟踪；既有 JUnit 启动加载器时自动写入。执行者未手工编辑、恢复或格式化它们，未执行 Git 写操作。最终 diff 检查所报尾随空格均来自这些自动日志，应由 PM 决定如何处理，不应作为功能修改集成。

## 测试合同与结果

### 先失败后实现

- `./gradlew.bat test --max-workers=1`：基线 exit 0，261 tests / 0 failures / 0 errors / 0 skipped。原始日志 `baseline-test.log`，XML 全量复制在 `baseline-junit-xml/`。
- 新增资源测试后执行 `./gradlew.bat test --tests '*LapisDustDataContractTest' --max-workers=1`：exit 1，4 项中 3 项失败，分别缺少模型、标签、语言；无配方检查通过。语言项初版因缺键直接读取报 NPE，最终补充明确缺键断言。日志 `red-test.log`、XML `red-junit-xml/` 保留。
- 实现前 `./gradlew.bat runGameTestServer --max-workers=1`：三个新增真实测试均明确报“正式青金石粉 ID 尚未注册”，证明不是源码字符串假验证。之后运行器空指针终止整轮；不计为完整基线或完整回归。见 `red-gametest.log`、`red-gametest-crash.txt`。

### 最终 JUnit / build

- `./gradlew.bat test --max-workers=1`：exit 0，**265 tests / 0 failures / 0 errors / 0 skipped**（既有 261 + 新增 4）。见 `final-test.log`、`final-junit-xml/`、`junit-counts.txt`。
- 新增 4 项：解析 generated 模型并 ImageIO 解码其实际 PNG、透明和非空检查；双语键值；具体/父标签 JSON 的追加关系与唯一成员；全部自带命名空间没有青金石粉配方。
- `./gradlew.bat build --max-workers=1`：exit 0。复用紧邻的最新通过 JUnit，jar 已生成；`build.log` 保留。检查 jar 包含模型、PNG、两级标签及 GameTest 类。
- `git diff --check -- src`：exit 0。未跟踪新文本文件另做尾随空白检查。
- `git diff --check`：exit 2，只有上述自动日志第 7 行尾随空格；见 `diff-check.log`，没有擅自清理。

### GameTest 未通过完整门槛

新增 3 项使用真实服务端加载后的注册表和标签：

- `formalIdResolvesToOrdinaryStackableItem`：正式键存在、普通 Item 类型、默认堆叠、无耐久/实例状态、普通组件集合和翻译键。
- `loadedTagsAppendDustAndPreserveExistingParentMembers`：正式粉末属于具体和父标签，父标签保留红石与荧石，整颗青金石、蓝染料、其他粉末、鲜/乏燃料组件不属于具体标签。
- `creativeTabExposesFormalDustOnce`：调用真实注册创造页的 `buildContents`，检查展示一次且进入搜索集合。

执行记录：

1. RED 轮如上，3 个预期缺失 ID 失败，随后运行器崩溃。
2. `./gradlew.bat runGameTestServer --rerun-tasks --max-workers=1`：`gametest.log`，exit 1。我的新增测试曾构造 `new Item(...)` 作为默认属性对照，触发 `Registry is already frozen`；已在允许测试文件中改为比较 `DataComponents.COMMON_ITEM_COMPONENTS`。该轮同时出现同样运行器空指针，见 `gametest-crash.txt`。
3. 修正测试后同一命令：`gametest-corrected-test.log`，exit 1。日志确认 **114 tests loaded**，第一批完成 50 项、第二批启动后，运行器再次因 `Object2LongOpenHashMap$MapIterator.nextEntry` 的 `this.wrapped == null` 崩溃，调用位置为 `GameTestInfo.tickInternal:130`。没有出现新增测试的失败消息，但这**不足以宣称新增 3 项全部完成**，更不能宣称 114/114 通过。见 `gametest-corrected-test-crash.txt`。

三轮完整日志和崩溃文件分别保留，没有覆盖失败或挑选通过轮次。没有生成 GameTest XML 或最终完成汇总，因此本报告不虚构 XML 计数。最后一轮发生在最终 PNG 落盘之前，最终 Java/标签实现一致；客户端资产是否加载不能由此推断。没有出现“测试全过但停在保存”的情形，也没有强杀 Java 进程。

**只读定位与交 PM 的线索：** 锁定版本依赖源码显示 `GameTestInfo.tickInternal` 在遍历 `runAtTickTimeMap` 时执行回调、再用迭代器删除条目；既有 `P1Refuel03GameTests.armPointRejectsInvalidTargetsAndRollsBack` 在 `runAfterDelay` 回调中再次 `runAfterDelay`（约 135、149 行），存在运行中修改同一调度集合的嫌疑。当前栈没有给出具体测试方法，尚未证明它就是根因；也不能把此异常直接认定为基线必现。本卡的新测试未创建延迟回调。禁止修改既有测试/运行器，因此停止此范围，待 PM 安排定位，不升级依赖、不改构建、不断重跑争取偶然成功。

## 纹理来源、查看证据与限制

- 使用内置 image_gen，一次生成、一次编辑。初稿实测 1312×1199，保存在 `lapis-dust-generated.png`；编辑稿实测 **1254×1254 RGBA**，正式资源和 `lapis-dust-final.png` 字节一致，大小 292416 字节。
- SHA256：`C97E0E20C59D153B7644CA4081DF9E683DA201B8DD1C3501D233A8E749ED8831`。
- 生成源：`C:/Users/lenovo/.codex/generated_images/01a0d22c-f59a-71b2-a0ad-7184181cbeb9/exec-a9216ecc-b10d-4fea-b771-f425120e7edf.png`。
- 正式图源：同目录 `exec-4b4ce532-fc96-4fef-929b-b3f2d6ab54ea.png`。已复制到本工作树，运行不依赖工具输出目录。
- 原始完整提示词见 `image-prompts.txt`。提示请求 16×16 逻辑像素、蓝色散粉、透明背景；编辑要求方形 1024 或可用时实际 16，但工具实际返回 1254 方形，**不能冒称 16×16 或 1024×1024**。未借用原版图标，也未运行 CLI fallback。
- 已查看参考项目精矿小图和两次工具返回图，确认返回图是蓝色粉堆与零散颗粒，无文字、容器或整颗宝石；已用 ImageIO 自动验证最终 PNG 的实际可读性、非空和透明像素。只有静态工具预览，**未检查真实游戏客户端外观、模型烘焙、16 像素槽位可读性或 mipmap 效果**。
- 明确质量限制：最终图不是项目常见的 16×16/2 的幂尺寸，图像生成未遵守精确导出尺寸。资源测试按任务卡的可读、非空、透明和模型引用合同编写，不把未授权的固定分辨率当作既有要求；移除了初版自行添加的幂次尺寸断言。尺寸规范与小尺寸视觉是否可接受交 PM 评审，不宣称美术最终验收通过。

## 兼容、注释与待验项

- 只新增正式物品 ID 和追加标签，无旧 ID 改名，无 NBT、网络、配置、反应堆热工或燃料行为变化。
- `c:dusts/lapis` 是本项目补充的通用标签，并非 NeoForge 内置。父标签通过子标签引用纳入，两个 JSON 均 `replace:false`。
- 所有新增手写注释与 Javadoc 使用中文；标识符、API 名称保持原文；未改既有英文断言/注释范围。
- 没有生存配方，不证明粉碎加工可达性、外部材料实际配方匹配、冷却剂生产或全链路通过；未安装第三方材料模组。
- 待 PM：处理 GameTest 调度异常、自动 tracked 日志、纹理尺寸/小槽位可读性评审；之后复验完整自动门槛。客户端人工验收只能由 PM 按原门槛安排，本执行者没有启动客户端。
- 未触及 P1 人工门，不提出跳过 P1 或合并主线；没有推进下一任务。

最终 Git 状态见 `final-git-status.txt`，差异及新文件清单见 `final-files.txt`。交付均未暂存、未提交。执行者至此停止，等待 PM 验收或整改任务。
