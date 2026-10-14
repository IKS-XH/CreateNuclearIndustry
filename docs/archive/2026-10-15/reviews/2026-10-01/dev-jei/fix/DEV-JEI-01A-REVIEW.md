# DEV-JEI-01A 独立审查

日期：2026-10-01。审查者：只读执行者，不具备项目经理、验收或 Git 写权限。

范围：主工程 `E:/MyMC/NewMod/Create_NuclearIndustry` 相对 HEAD `dd7f2286a168cfcb498e47def252fa0d21bb5e81` 的 `build.gradle` 未提交修改及本轮 `build/reports/development/DEV-JEI-01A*` 证据。实际读取并采用 PM 修订后的 DEV-JEI-01A 卡：JEI 仅实际客户端 classpath 各一份，legacy 为零份；旧“legacy 同时一份”条件已撤销。

## 最终发现

**最终补丁无未解决阻塞，未发现需整改的具体缺陷。** 第二轮真实 runClient 已发现并加载 JEI 19.27.0.340 资源。结论仅覆盖主工程配置、单个隔离客户端的启动/资源加载及三个客户端任务的配置范围；进世界后的物品列表、配方交互尚未验收，不代表候选工作树已经同步。

## 首轮阻塞与修订依据

审查发现首轮 14:31:33 Mod List 仍无 JEI，14:31:45 ResourceManager 无 mod/jei，已及时反馈 PM 与实现者。真实进程启动命令的 -cp 已有 JEI；RunGameTask.exec 字节码调用 classpath(provider) 追加而非覆盖，不能归因为任务执行阶段覆盖配置。

DiscoveryProbe 调用了真实 FML DevEnvUtils，但它自行建立 URLClassLoader，没有执行完整 Bootstrap/模块层/DiscoveryPipeline。其 roots=1 仅证明该隔离类加载环境能枚举资源，不能证明游戏加载。DevEnvUtils 还依据自身是否在 named module 中选择 system/context classloader；此 helper 与真实启动的分支和模块环境不同。

已读取并核对锁定版本的 BootstrapLauncher、LaunchContext、DiscoveryPipeline 字节码：legacy 清单参与启动模块层，LaunchContext 将既有模块位置纳入 locatedPaths；DiscoveryPipeline.addPath 的 addLocated 返回 false 即跳过。因此“实际 classpath + legacy 都放 JEI”仍可能令它被当作已处理启动库，而不是注册为游戏模组。

PM 随后正式修订合同并批准独立 developmentJei 配置。首轮失败证据保留在 `client-smoke-first-failed-gradle.log`、`client-smoke-first-failed-latest.log`、`client-first-failed-process.json`；首轮 helper/log 不用于宣称最终加载成功。此前 DEV-JEI-01 审查仅覆盖依赖/清单及制品，不能作为安装成功证据。

## 最终实现与隔离

- developmentJei 可解析、不可消费，不 extendsFrom 通用 runtimeClasspath，固定原版本 19.27.0.340。
- 仅三个具名任务 runClient/runClientA/runClientB 追加此配置到实际 JavaExec classpath；原三条 AdditionalRuntimeClasspath JEI 声明已移除。
- 没有新增 JVM 扫描绕过参数、通用 runtimeOnly、模组 API、自定义 JEI 插件、jarJar 或强制模组依赖。中文注释解释真实发现路径与 legacy 启动模块的差异。
- 改动仍仅限 build.gradle，不升级 Minecraft 1.21.1 / Java 21 / NeoForge 21.1.219 / MDG 2.0.143 / Create 6.0.10-280。`git diff --check -- build.gradle` 退出 0。

本审查通过捆绑 Python 的 `-B` 只读脚本独立检查最终记录与现有文件：

| 项目 | 结果 |
| --- | --- |
| 原交付三个实际客户端任务的 JEI 数 | 各 0 |
| 最终三个实际客户端任务的 JEI 数 | 各 1，均 19.27.0.340 |
| server / GameTest 实际任务 JEI 数 | 各 0 |
| 三客户端及 server / GameTest 的当前 legacy JEI 数 | 全部 0 |
| 最终 dependencies 原始日志含 JEI 的配置节 | 只有 developmentJei |
| compileClasspath / testRuntimeClasspath / runtimeClasspath / runtimeElements / apiElements | 均不含 JEI |
| 实际项目 JAR | 无 mezz/jei 类、无任何内嵌 JAR、无 JEI 强制依赖 |

项目 JAR 哈希与最终证据为 `CF93E36E4B0F874F58F340F5E07E307B57BED0EDBD093587F137511E7F811E62`。旧 run/mods 19.27.0.336 已不存在于原路径，PM 备份文件 SHA-256 与 legacy-mod-backup.json 一致；隔离 smoke/mods 无 JAR，不存在测试世界 level.dat，未借旧 JAR 或旧日志证明成功。

## 本轮真实加载证据

第二轮 `client-smoke-second-gradle.log` 与当前/保存的第二轮 latest.log 来自 2026-10-01 14:35 启动：

- 14:35:41：Mod List 明确列出 `Just Enough Items 19.27.0.340 (jei)`。
- 14:35:50：JEI PluginCaller 执行 Sending ConfigManager。
- 14:35:53：ResourceManager 包含 `mod/jei`。
- 14:35:56：创建 `jei:textures/atlas/gui.png-atlas`。
- 14:36:09：Stopping，随后 BUILD SUCCESSFUL，最终记录 Gradle exit 0。

client-second-process.json 与 client-second-close.json 的 PID 均为 25660，Closed=true。测试目录由临时 init 指定到本卡报告内的 client-smoke；最终 restore-default-preparation.log 记录无 init 的正常启动准备成功。未发现 JEI 相关启动错误；日志中的 Ponder/注解、冷却剂 blockstate 等警告在首轮无 JEI 时也存在，不能描述为完全无警告，也不在本卡扩修范围。

这些证据证明真实模组发现、配置初始化和资源加载已发生；未进入世界，不能推断配方列表完整性或背包界面交互正常。

## 技能与执行边界

沿用已读 AGENTS、治理协议、minecraft-modding 与 minecraft-testing 技能，本轮补读 `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/systematic-debugging/SKILL.md`：沿依赖配置、实际进程、类加载器、模块发现、资源加载逐层核对，不以中间清单替代真实结果。

Minecraft 技能入口为 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 与 `minecraft-testing/SKILL.md`：分别用于版本/开发环境边界和验证层次区分。仅审查所需证据，未做全局审计。

审查者未启动 Gradle、游戏、诊断 Java 进程，未修改输入、核心文档、Git、用户配置或存档；唯一写入为本报告。实际运行日志为实施者执行证据，本审查为独立读取与文件/内存复核。

最终报告补核：已在实施者落盘后读取 DEV-JEI-01A.md；其明确撤销 legacy=1 旧条件、保留首轮失败、采用实际客户端各1/legacy0条件，并将第二轮真实加载与未完成的世界内 GUI 体验分开记录。报告与本次所读原始日志及独立检查一致。
