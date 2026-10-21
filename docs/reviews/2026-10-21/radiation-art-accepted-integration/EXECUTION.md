# 防辐射服21B与辐射计20B→20C集成：阶段A执行交付

2026-10-21。阶段A执行完成，交PM组织一次组合窄审及源码提交。此处不宣布main验收；未运行jar或assemble，后续阶段B须由PM提交准确来源后另行授权。

## 精确来源与导入

工作树`E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration`，分支`codex/radiation-art-accepted-20261021`，来源基准`4ad0c91fa22dbbfb3db6fc698aafe55a89d2686b`。开工tracked干净。PM在main新增卡提交`fec2d9d64c4b48958fb68acad26d05770062a19b`不涉及生产源，本阶段未执行Git写操作。

按主目录活动卡实读AGENTS、治理、STATUS、21B ACCEPTED/CANDIDATE、20B/20C CANDIDATE及三份原REVIEW后记。实际使用本机minecraft-modding/testing/resource-pack技能核组合/API、定向验证及资源来源，ci-release核制品身份边界；实际Gradle锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，MIT及构建规则保持。

| 不可变来源 | entry | ZIP SHA256 |
| --- | ---: | --- |
| ART-PROTECTIVE-SUIT-21B-59-source-resource-files.zip | 59 | b89f436b6d45a38870c233a2856b207216268da4ebebc5ea505ffdca7519e719 |
| ART-RADIATION-METER-20B-10-resources.zip | 10 | 7d4bc7a5f49ea907986b99ad3d8015e0fbe9c4eefd8c45ab54afb5cfde8fc85e |
| ART-RADIATION-METER-20C-9-source-resource-test-files.zip | 9 | b2daf3b3c74d8fe803e822c17755b8c4c0064860445bddc0db4ee3c4cdff9c85 |

导入前核原ZIP哈希、78个entry精确并集、大小写重复、绝对路径/冒号/反斜杠/路径穿越/符号链接及目标根。三包无重复路径；仅70资源、7生产Java、1测试。逐entry原字节导入及测试后回读均78/78一致，净70新增、8修改。完整路径/原源/目标旧SHA/导入SHA见`paths.json`，汇总及引用哈希见`SOURCE.json`；不以候选旧JAR替换当前工程。

现有RadiationContent、RadiationClientRegistration与21B闭合基准`c71fe791ec57cd2123b1a4422cf02c4dfdf74356`相同；RadiationClientEvents与20C上游main`0a748df0ce1d35172d5fbd3e7bd43f15e6528e67`相同。四个新增生产Java在相应旧基准及当前main均不存在，7项均无真实逻辑冲突，无需扩大写集或手工合并。细目见`PREFLIGHT.json`；可读语义差异见`java-import-normalized.diff`。

首次preflight因现有JSON工作区CRLF与Git提交LF原字节不等而停在导入前。仅调整核验为CRLF→LF规范化比较，分别保留原工作区与提交SHA；没有把逻辑差异当换行放行。原失败说明保存在`preflight-initial-failure.txt`。本批仍严格保留ZIP原字节，未进行源码换行重写。

## 组合验证及身份

唯一命令：`./gradlew.bat test --tests '*RadiationEquipmentDataTest' --tests '*RadiationMeterNeedleMotionTest' --console=plain`。Java21，2026-10-21T13:34:19.1276540Z至13:34:41.8129554Z，真实exit0，`BUILD SUCCESSFUL in 22s`；8任务中4执行、4UP-TO-DATE，没有jar/assemble任务。

实际JUnit XML为Equipment 1项、NeedleMotion 9项，共10项，failure/error/skipped均0。`test.log`、`test-exit.json`及`tests/`保存原始证据。日志中6个既有或批准候选使用的EventBusSubscriber弃用警告及Gradle弃用提示保留，未借此改规则或升级。

- 16个允许生产class与本次编译输出、对应原21B/20C候选JAR及原manifest SHA逐字节一致；其余758个正式基线class与本次编译输出逐字节一致。完整清单为`class-identity.json`。
- 70资源与当前源、processResources输出及对应候选JAR逐字节一致，清单为`resource-identity.json`。20B十资源与20C四增量共同存在，父模型/材质依赖没有遗漏。
- 2,277个非批输入在导入及定向测试前后保持；Git状态恰8修改、70新增，`diff --check`通过。原始前后输入清单及精确SHA在`before-inputs.json`、`after-inputs.json`；阶段A并未声称最终新JAR身份已核验。

防护服原皮革属性、type/耐久5、四ID、修复/配方与Create头罩护目镜保持；公共外观类不依赖客户端，贫铀及公共护甲入口未改。辐射计只新增客户端包装/显示状态及同包只读getter，原receive/HUD/声响/服务端/协议/剂量未改。主副手/F5使用本机实体上下文，GUI/掉落/展示框与他人保静态；实现与原候选无二次调整。

## 保护与证据边界

导入前将17A根跟踪JAR备份到`backups/before-import/build/libs/`，3525495bytes、SHA256 `17a0a41616c27b4c2fdc7ee5a2e005973ab5c1eb3d1ca1ba16db6f2b904acb88`；测试后根JAR仍同字节。根`logs/debug.log`/`latest.log`开工时均不存在、当前不跟踪，备份清单记不存在；本次测试生成的普通运行日志不恢复、不暂存。详见`backups.json`及`SOURCE.json`。

未开客户端/服务端、未改世界；本树携带世界27文件/13337721bytes在交付时仍逐SHA等于PM的`world-copy.json`。PM原`world-copy.json`与`prior-world.json`均未覆盖。main的3D工具dirty、事故树、旧候选、源码以外规则/许可/版本保持，本执行者未写其它树。

原像素/离线预览/三次审查证据复用，没有重跑。当前21B ACCEPTED记录用户原话“四件套手动测试也通过了”；没有补造逐饰纹/颜色/多人观察。20B/20C原生游戏细项及真人多人反馈仍未报告，10项JUnit、离线预览及class身份不代替它们。当前交付停止在组合窄审/源码提交之前，等待PM的阶段B授权。
