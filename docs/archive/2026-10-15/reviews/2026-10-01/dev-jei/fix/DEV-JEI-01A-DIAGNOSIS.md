# DEV-JEI-01A 只读诊断

2026-10-01；执行者 dev_jei；候选 9c4fb39。仅写诊断证据，无 Git 写或仓库配置变更，无游戏进程启动/关闭、无存档变更。

## 根因

MDG 2.0.143 的 ModDevRunWorkflow.setupRunInGradle 只将 RunModel.additionalRuntimeClasspathConfiguration 扩展进 <run>LegacyClasspath；RunGameTask.classpathProvider 来自 run.sourceSet.runtimeClasspath。FML 4.0.42 的 UserdevLocator 在发现外部模组时调用 DevEnvUtils.findFileSystemRootsOfFileOnClasspath，后者通过系统/上下文类加载器枚举 META-INF/neoforge.mods.toml 与 MANIFEST.MF，不读取 legacyClassPath 文本作为模组发现源。

所以 JEI 在 LegacyClasspath 清单出现并不代表进入真实启动 classpath。候选最新 2026-10-01 14:24:15 日志 Mod List 只有 Create、Create Nuclear Industry、Flywheel、Minecraft、NeoForge、Ponder，没有 JEI；用户无 JEI 的观察成立。主工程 09-29 旧日志不能验证新配置，主工程 run/mods 内已有 19.27.0.336 是旧日志的独立来源。

## 实际证据

- 锁定 MDG/FML 字节码：同名证据目录下 ModDevRunWorkflow.txt、RunGameTask.txt、moddiscovery.locators.UserdevLocator.txt、DevEnvUtils.txt。
- 对候选运行 gradlew -p <候选> -I read-classpath.init.gradle help，只通过 doLast 读取任务；退出 0。实际五个 run 的 JavaExec.classpath + classpathProvider 全部不含 JEI；actual-task-classpaths.json 记录完整清单。首次在 projectsEvaluated 解析被 Gradle 9 exclusive lock 阻止，改为 help doLast 的只读操作后成功。
- DiscoveryProbe.java 仅创建隔离 URLClassLoader，直接调用锁定 FML 4.0.42 的 DevEnvUtils 原方法，不启动 Minecraft。以实际 runClient classpath 运行得到 JEI roots=0；只额外加入已下载 19.27.0.340 后得到 JEI roots=1，两个 Java 进程均退出 0。对应 discovery-before.log 和 discovery-with-jei.log。这是同一真实发现方法的最小变量对照，不是自己复写发现算法。

## 最小修正建议

保留三个客户端独占范围，将同一个固定版本 JEI 同时接到三个客户端的实际 JavaExec classpath；不要改通用 runtimeOnly、implementation 或服务端配置。可建立一个仅用于开发 JEI 的可解析且不可消费配置，共同供客户端额外 classpath 和实际 JavaExec 使用；或者复用已解析的每客户端 LegacyClasspath 中唯一目标 JEI 文件。只选一种简洁实现。

修正后除依赖范围与打包外，必须覆盖真实发现路径并启动一次独立测试客户端确认 Mod List/JEI 完成初始化，不能再次用 LegacyClasspath 清单替代实际加载。主工程旧 run/mods JEI 需 PM 安全备份移出，避免修复后两个 JEI 版本重复；诊断者没有改动该文件。最终用户的物品列表及配方交互体验仍需单独确认。

## 真实启动后的诊断修正（14:31）

首次将 JEI 加入实际 JavaExec classpath 后，独立客户端 PID 32036 的真实命令行已经含 JEI，但 Mod List 仍无 JEI；仅枚举 FML DevEnvUtils 的 probe 没有覆盖完整加载管线。这推翻了“只补启动 classpath 且保留 JEI legacy 路径即可”的初始建议，初始建议不是最终根因的完整描述。

继续追踪发现：BootstrapLauncher 从 legacyClassPath.file 读取的是启动库清单并建立模块层；普通 Create/Ponder/Flywheel 模组并不在该清单。错误追加 JEI 到该清单会先将其作为启动模块加载。FML LaunchContext 构造器收集当前所有 module layers 中已加载路径到 locatedPaths；DiscoveryPipeline.addPath 对已 located 的路径直接跳过，因此即使 JEI 同时存在真实 -cp 也不会作为模组再次加载。对应 BootstrapLauncher.txt、LaunchContext.txt、DiscoveryPipeline.txt。

最终最小方向应移除 JEI 对 legacy 清单的注入，使用一个开发专用可解析、不可消费配置，仅接入三个客户端实际 JavaExec classpath。无需全局 runtimeOnly，不改变服务端/JUnit/发布范围；保留其他原有 legacy 库。MDG 2.0.143 ModModel 只有 sourceSet，没有 additionalModFiles（ModModel.txt），不能照抄其他版本新接口。

首次失败客户端已通过 CloseMainWindow 正常关闭，Gradle 退出 0；这只证明进程正常退出，不代表 JEI 成功加载。失败日志已另存到 DEV-JEI-01A/client-smoke-first-failed-*.log。
