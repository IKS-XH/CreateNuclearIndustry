# REACTOR-EXPLOSION-01 候选制品身份

2026-10-27。PM已核最终源清单并完成组合窄复审后，执行者仅在外爆候选树构建一次增量JAR。生产来源提交为`96335648e964f86e2bc55548319c1dc4bb847f10`；其前置`0fb21e9d`仅为实际测试输出路径的文档说明，11项被测实现清单没有漂移。构建前工作树干净，构建后仅跟踪JAR变更。未执行Git写操作、未复制或提升main、未触碰锅炉候选。

| 身份项 | 本次结果 |
| --- | --- |
| 命令 | `.\gradlew.bat jar --console=plain` |
| 退出与耗时 | exit0；命令墙钟2072ms，Gradle报告1s |
| 增量状态 | compileJava/processResources/classes全部UP-TO-DATE；jarJar NO-SOURCE；jar执行，4任务中1执行/3复用 |
| 来源SHA | `96335648e964f86e2bc55548319c1dc4bb847f10` |
| 候选路径 | `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/libs/create_nuclear_industry-0.1.0.jar` |
| 大小 | 3,681,192 bytes |
| SHA256 | `F8C87A27C6909EF0D225016AA59C3BEC6DA1954A6F9BA2689B12A0FA392537CA` |
| 源稳定性 | 11项构建前后路径/bytes/SHA均与最终冻结清单完全一致 |
| 载荷身份 | 2590项：811 class、1775资源、manifest及3份许可/说明；每项解压字节SHA全部绑定当前实际输出/源文件 |
| 新生产载荷 | ReactorExplosionFinalizationMixin.class、ReactorExplosionLifecycle.class及新Mixin注册均存在 |
| 测试排除 | 无任何ReactorExternalExplosionGameTests及其内部类；无`data/cni_reactor_external_explosion`域载荷 |

已读并实际应用`C:/Users/IKSXH/.codex/skills/minecraft-ci-release/SKILL.md`的制品身份与发布边界，沿本项目冻结版本/许可执行；没有运行clean/build/test/rerun或更改发布方式。此次构建未重复JUnit/真实服业务，不称全量通过、真人测试、客户端视觉或多人验收。

与原F29比较：0项删除，2项生产class新增，7项属于本批生产类型及其内部类的class变化，1项Mixin注册JSON变化。另49项现有辐射装备/辐射计model JSON仅CRLF/LF换行字节不同，逐项规范换行后完全相同，并已核当前源blob等于b9基准及本次HEAD的同路径blob、Jar字节等于当前资源输出与源文件；没有新增美术内容或锅炉内容。完整差异见`artifact-vs-F29-changed.json`及`artifact-preexisting-resource-delta.json`，没有把换行差异隐藏为零非批字节变化。

原3,677,228bytes/F29D0F8B20646E577A273CF5B768FBF9E9244B30699F2EB28B923DD783FBB13C仍保存在本报告目录`original-F29.jar`；此次仅替换候选树原跟踪输出，未改main原状态或用户日期包。

必要原始身份索引：

- `jar.log`、`jar-command.json`：命令、增量任务、来源、退出和墙钟。
- `artifact-source-before.json`、`artifact-source-after.json`：本批11项源的构建前后清单。
- `artifact-summary.json`、`artifact-payloads.json`、`artifact-origins.json`：全部2590载荷解压SHA、输出绑定及排除结果。
- `artifact-vs-F29-changed.json`、`artifact-vs-F29-removed.json`、`artifact-preexisting-resource-delta.json`：原包差异及49项换行核验。
- `verify-artifact.ps1`：仅报告目录内的只读载荷核对工具，不修改资源、JAR或Git。

执行者制品交付完成，交PM独立核包与提交候选；本报告不宣告用户手测通过或合入main。
