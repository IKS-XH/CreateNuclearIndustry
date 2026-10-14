# REACTOR-SODIUM-SURFACE-01 执行者交付

2026-10-13，main `4da5e94033b79bed75aec76fc3d4c2cf140df822` 原位未提交候选。需求与来源见 [当前诊断](DIAGNOSIS.md)。用户本次直接恢复并要求修复，执行者保持角色，不修改 AGENTS/治理/活动计划，不执行任何 Git 写操作。

## 结果及精确写集

- 生产代码仅 `src/main/java/com/iksxh/create_nuclear_industry/structure/client/ReactorSurfaceSnapshots.java`：为真实 Sodium final `LevelSlice` 读取构造时绑定的 declared `final ClientLevel level`，核对原子发布中的同一世界对象身份；每个类身份只解析一次字段。字段不存在、类型/修饰符不符、不可访问继续降级为空。
- 测试仅新增 `src/test/java/com/iksxh/create_nuclear_industry/structure/client/ReactorSodiumSurfaceTest.java`：真实 Sodium 类夹具、有限切片数组、Create CT 与模型数据消费、同世界/旧世界/无发布/未知包装与原生入口。
- 报告仅本目录的 `DIAGNOSIS.md`、`IMPLEMENTATION.md`，证据与临时脚本仅 `build/reports/reactor-sodium-surface-01/**`。
- 不查询仪表、结构、BE、区块、光照或世界方块，不调用 Minecraft 当前世界作为替代；未知包装、Ponder、旧世界仍降级。原生 ClientLevel/精确 RenderChunkRegion 保持。
- 无 Sodium/Iris 编译、运行或生产打包硬依赖，无 Mixin/config/材质/包格式改变；成型、热工、库存、尺寸、刷新生命周期与工业流体 dirty 保持。

## 实际验证

通过绝对路径 `C:/Program Files/Java/jdk-21/bin/java.exe -jar gradle/wrapper/gradle-wrapper.jar` 执行，避免默认 Java 路径漂移。第三方 JAR 仅由测试自建 URLClassLoader 加载，没有加入 Gradle 工程依赖或模组加载清单。

| 检查 | 实际结果 | 证据 |
| --- | --- | --- |
| 修复前真实 Sodium context RED | 3 项，2 项预期失败：同世界 capture 为空、Create CT 类型为空；原生/未知隔离通过 | `01-red.log`、`red-results/` |
| 修复后定向 JUnit | 26/26，失败/错误/跳过均 0；真实 Sodium 3、原 CT 21、原快照 2 | `02-green.log`、`green-results/` |
| 首次候选改名脚本 | 在项目插件创建 jar 任务前访问 named 失败；未执行打包，不隐去此失败 | `03-assemble.log` |
| 必要增量 assemble | 改用惰性 configureEach 后 exit 0，2 秒；唯一实际 jar 打包，其余生产任务 UP-TO-DATE | `04-assemble.log`、`candidate.init.gradle` |
| 无 Sodium/Iris 专用服启动探针 | 既有 `reactor_surface_display_probe` 真实 1/1，20 秒 exit 0，自然保存退出 | `05-server-probe.log`，隔离 `server-probe/` |
| JAR 所有解压载荷核对 | 2150→2151 个文件，2145 保持，5 改变＋1 新增，仅本客户端类及其内部类；0 删除/越界/第三方或夹具项 | `artifact-verification.json`、`check-artifact.ps1` |
| 未修改状态机语义核对 | Bounds/Publication/State/State$Owner 的 `javap -p -c` 完全相同；其 class 差异为调试行号，非状态机指令 | `nested-bytecode-verification.json` |
| 来源与编译输出绑定 | Java、测试源 SHA 与 6 个编译 class/JAR SHA 已绑定 | `source-bindings.json` |

服务器探针只在 `build/reports/reactor-sodium-surface-01/server-probe/` 创建临时 GameTest 世界，没有访问主目录或用户实例存档。首次隔离启动有缺省 `server.properties` 的 NoSuchFileException 日志，之后生成默认文件并完成 1/1；开发环境另有原 Ponder refmap/终端等警告，未冒称日志零警告。JUnit 正常更新主目录 `logs/latest.log`、`logs/debug.log`，没有清理或覆盖其他用户改动。全仓 `git diff --check` 会报告运行日志行尾空白，本批生产代码与新增测试/报告单独差异检查通过。

未跑全量 build、其他设备或既有管网/热工用例。此次变化限于模型上下文身份，原 L1/L2/CT 权威规则与此前证据复用；无优化模组客户端的原生入口由实际 CT JUnit 回归覆盖，没有冒称此次启动完整原生客户端/GPU 验收。

## 可安装候选及联机复看

候选：`E:/MyMC/NewMod/Create_NuclearIndustry/build/libs/create_nuclear_industry-0.1.0_261013_sodium-surface.jar`

SHA256：`8c03dbd11e4812705b18942407ae4dd64aba8e7c0cbbc18d03ab2690a0ac1f23`

原正式 `build/libs/create_nuclear_industry-0.1.0_261013.jar` 与客户端旧包都仍为 `745e936c60c473ad2a47f83b9d683a0ae0dc227fdf07024523395edbc196c361`，原包另保存于本批 `baseline-745e936c.jar`。用户客户端/专用服务器实例未安装或改写；候选与用户旧包的其他 2145 载荷逐个一致。

收尾复核时客户端旧包当前名称为 `create_nuclear_industry-0.1.0_261013.jar`，与开始时的 `create_nuclear_industry-0.1.0_261012_2.jar` 名称不同，SHA256 未变。本执行者没有执行该实例改名；安装步骤以当前实际文件名和上述哈希为准。

本修复只需客户端换 CNI 包，服务器包、协议、配置与世界均不需变更。客户端退出后把原 CNI JAR 移到 mods 目录外保存，再将候选放入同一实例 mods，保持只加载一份 CNI。不要同时保留旧包和候选在 mods 下。

集中局部复看：

1. 保留 Sodium 0.8.12、Iris 1.8.14-beta.1，连接原专用服务器并观察已成型反应堆：护目镜状态/功能正常，外壳外平面与窗口切换为成型连接纹理；不需要拆掉仪表重建反应堆。
2. 在安全测试结构中拆坏再恢复一块外壳，确认纹理随“未成型→已成型”切换；无需重做已有设备整机测试。
3. 重连同一服务器或卸载后返回区块，确认已成型连接显示恢复；换世界/服务器的同坐标普通未成型结构应保持分离纹理。
4. 若使用光影，分别关闭和开启当前光影观察同一外壳/窗口，记录两者差异。没有差异也只说明本次外观，没有据此宣称 Iris 全面兼容。

若仍分离，应保留该轮客户端日志并采集“实际模型 context 类型→slice 所属世界身份→当前发布世界身份/成员数”的运行证据，继续判别实际模块反射权限、L1 包/加载时序或其他渲染包装。不据此扩展为修改服务端成型逻辑。

## 交付边界

本轮证明现行 CNI 对真实 Sodium LevelSlice 的确定缺陷并交付窄修，不宣称现场唯一根因已经闭环，不归因 Iris 自身故障。真实 Sodium/Iris 联机 GPU 外观仍待用户复看。

本执行者按 AGENTS 的“禁止派发或指挥其他执行者”约束，没有派子 Agent 或伪称独立审查。差异已自检，原卡要求的最终一次独立窄审仍由 PM 接续；未修改任务/验收状态，未提交源码或替换原正式制品。

实际技能入口与应用：`minecraft-modding` 核客户端/服务端边界与锁定版本；`minecraft-testing` 区分真实类夹具、原生 JUnit、真实专用服与 GPU 人工门；`systematic-debugging` 从实际依赖字节码追溯输入；`brainstorming` 按用户已明确的局部修复范围整理短设计；`test-driven-development` 实际 RED→GREEN；`verification-before-completion` 核日志、退出码、XML 和载荷。`requesting-code-review` 已读取，派审动作遵循用户 AGENTS 的角色限制留给 PM。
