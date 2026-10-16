# THERMAL-SODIUM-01 规格＋质量组合窄审

2026-10-17；审查者 `/root/thermal_sodium_review`。基准 `7d8fdaabe0ea71032e970d18c393616db2290ee1`，工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-thermal-sodium`。本报告是执行范围内的独立审查意见，最终验收、任务状态和Git由PM处理。

## 结论

- **规格：符合本卡局部修复合同，未发现阻断项。**
- **质量：未发现需要整改的可证实代码或制品问题。**
- 当前证据足以交PM进入集中人工观察门；不能据此宣布真实Sodium/Iris联机GPU外观已验或用户现场已修复。

审查以主树权威卡 `docs/superpowers/plans/2026-10-17-boiler-turbine-sodium-01.md`（含正常测试输出例外和窄包补充裁定）为准，已读本树AGENTS、治理5.1、STATUS、Gradle实际版本与既有调用链诊断。仅写本报告；未运行测试、构建、游戏或服务端，未派Agent、执行Git写操作、修改源码或核心文档。

## 规格核对

1. `BoilerSurfaceSnapshots.java:30-44` 与 `TurbineCasingAppearanceModel.java:38-52` 均只接收确切名称的final `net.caffeinemc.mods.sodium.client.world.LevelSlice`；精确 `getDeclaredField("level")`，校验字段类型恰为 `ClientLevel.class`、final、非static及可访问，再用 `ClassValue<Optional<Field>>` 缓存。没有可选模组类加载/硬依赖、反射字段遍历或全局Minecraft世界兜底；生产反射仅 `Field.get`，失败返回原降级路径。新增说明为中文且与行为一致。
2. 锅炉 `capture(context):53-60` 一次读取原子Publication；从切片自身读取绑定世界并以 `== publication.world()` 比较。不会拿旧世界切片消费当前快照，也不查方块、BE、区块或光照。原生ClientLevel与精确RenderChunkRegion入口保持；未知/Ponder安全返回空。快照发布与状态机未修改。
3. 汽轮机 `realWorld:106-117` 只校验上下文资格，不取得全局快照或转读绑定ClientLevel。atlas继续用入参state/pos；直接消费者 `TurbineWindowConnectedModel.getModelData` 保持只读传入上下文长轴正负各一格，无owner或整机扫描。旧切片仍按自身局部数据连接。小型、未知/Ponder、缺失或尺寸不符的材质回退原模型。
4. Git只读差异与 `source-bindings.json` 对应：生产只上述两文件，新增测试只 `BoilerSodiumSurfaceTest.java`、`TurbineSodiumAppearanceTest.java`。本轮重新读取四文件长度/SHA256，均与绑定清单一致。反应堆源码、服务器/热工/流体/应力、依赖、构建脚本和资源源文件没有变化。tracked根日志和原始build/libs JAR是正常自动输出，由PM排除本次源码提交。

## 测试与质量证据

已逐一阅读新增两类测试和直接模型消费路径。真实类由独立URLClassLoader加载，并断言实际类加载器；局部数组索引调用真实Sodium方法，未以仿制LevelSlice替代。5个夹具JAR实际SHA256与本批清单及前次真实夹具逐项相同；既有 `LevelSlice.javap.txt` 的final类/字段及 `NeoForgeModelAccess.javap.txt` 的直接BakedModel调用与本次适配相符。

本轮只复核执行者原始日志、exit与归档XML：

| 已有运行 | 实际结果 | 证明范围 |
| --- | --- | --- |
| `01b-red-complete.log` / `red-results/` | 8项，6 failures，0 errors/skipped，exit1 | 未适配时的有效入口/最终模型消费断言失败；不是装载错误或skip |
| `02-green.log` / `green-results/` | 32项，0 failures/errors/skipped，exit0 | 新增8项全部执行；锅炉同/旧世界、Create CT与final共享面，汽轮机中/大型atlas、双端/单端/不连接及旧切片局部数据、原生/未知与材质回退；同批包含旧反应堆3项 |
| `03-assemble.log` | 增量assemble，exit0 | 此实现的一次打包；本审查没有重跑 |

XML逐套计数与 `green-counts.json`、日志一致。测试会在无显式夹具目录时条件跳过，但本次init脚本仅转发夹具目录，归档结果没有跳过，不存在将skip算作通过的情况。Unsafe只用于隔离测试对象初始化；真实getBlockState及生产模型数据路径仍实际参与断言。编译/弃用警告已保留，不称无警告。这里没有完整客户端、Mixin渲染器或GPU证明，也不以此代替真人联机。

## 窄候选身份复核

只读核验baseline的Git blob与 `HEAD:build/libs/create_nuclear_industry-0.1.0.jar` 一致（`0eebd12deb207366e4559d65e48fc3c95b259a36`）。原始assemble保留；其9项class差异恰属两处生产来源，1231项非class差异在已有分类清单均为CRLF差异，分类脚本按严格UTF-8归一后逐字符比较，未借该语义等价声称原始资源字节相同。

已审 `make-narrow-candidate.ps1`，并独立以只读方式读取baseline、assemble、candidate全部实际ZIP条目计算载荷哈希；未重新生成候选。结果与 `narrow-candidate-verification.json` 一致：

- 最终文件：`candidate/create_nuclear_industry-0.1.0_261017_thermal-sodium.jar`。
- 长度 **3336385 bytes**，SHA256 **B366EB49F8994BB0213B676F530AE395E03A58F53E59CBA23AB9A77B682D7606**。
- baseline 2276项，candidate 2278项；**2269项原载荷相同，7项替换＋2项新增class，0删除、0越界差异**。
- 9项差异各自与assemble相同；分别为BoilerSurfaceSnapshots外类、`$1`、`$Bounds`、`$Publication`、`$State`、`$State$Owner`及TurbineCasingAppearanceModel外类、`$1`、`$Appearance`。其余服务器/反应堆class、资源和metadata保持baseline字节。
- 三包均无重复载荷条目；最终包无Sodium/Fabric或三类Sodium测试夹具条目。

该文件是PM明确允许的“baseline原载荷＋assemble九项class”窄候选，身份区别于3347467 bytes的原始assemble包。没有把工作树原始build/libs输出当作已通过精确资源字节门的候选。

## 保留人工门与技能入口

保留用户当前Sodium/Iris，以候选客户端连接原服，观察锅炉成型壳/窗、中大型汽轮机壳/轴向连窗、拆坏补全、重连、离开区块再返回，并分别复看光影开关。真人客户端、真实服务器连接下的GPU外观尚未验；无需因本次纯客户端入口修复另开重复服务端测试。本审查不认定该入口缺陷是现场唯一原因。

实际读取应用的技能：

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：按实际Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6核客户端上下文与线程边界。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：核隔离JUnit真实类夹具、RED/GREEN断言和人工证明边界。
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：核atlas/OBJ材质回退与资源载荷保持，不改变资源。
- `C:/Users/IKSXH/.codex/skills/minecraft-ci-release/SKILL.md`：核baseline、assemble及最终候选的制品来源/载荷身份，不升级版本或发布。
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/verification-before-completion/SKILL.md`：按证据而非执行者结论形成报告；依治理5.1及明确窄审指令复用既有运行，本次只作独立只读核验。
