# DEV-JEI-01 独立审查

日期：2026-10-01。角色：只读审查执行者，无项目经理、验收或 Git 写权限。

目标：`E:/MyMC/NewMod/Create_NuclearIndustry` 的 `build.gradle` / `gradle.properties`。开始时检查相对 `af8ccf8` 的未提交改动；PM 随后将同一两文件保存为 `e4107516cbea3ec79e04205235a87c98ac511280`。最终只读确认工作树这两文件与该提交无差异，正式审查范围为 `af8ccf8..e410751`，两文件共增加 10 行。不审查 PM 其他未提交文档，不涉及候选同步。

## 发现

**无阻塞发现，未发现需整改的具体缺陷。** JEI 固定版本仅进入三个开发客户端，现有证据支持启动参数准备和制品隔离要求。此结论不等于实际客户端启动或界面验收通过。

## 构建配置审查

- 新 Maven 仓库为合同指定的 `https://maven.blamejared.com/`，内容过滤严格限定 `mezz.jei` 组。
- `jei_version=19.27.0.340` 固定版本，无动态版本或现有栈升级。
- 三条依赖分别加入 `clientAdditionalRuntimeClasspath`、`clientAAdditionalRuntimeClasspath`、`clientBAdditionalRuntimeClasspath`；未加入 implementation、compileOnly、全局 runtimeOnly、全局 additionalRuntimeClasspath、jarJar 或 mod 元数据。
- 已查看锁定 ModDevGradle 2.0.143 的 `mdg-run-model.txt` 字节码证据：RunModel 为每个运行创建对应 additionalRuntimeClasspath。现有依赖解析结果与配置名一致，未使用未支持的 API。
- 唯一新增手写注释为中文，职责与行为一致。`git diff --check af8ccf8 e410751 -- build.gradle gradle.properties` 退出 0。

## 版本兼容证据

实际只读打开已下载的 JEI JAR，核对 mod ID `jei`、版本 `19.27.0.340`、NeoForge 范围 `[21.0.118-beta,)`；项目的 NeoForge 21.1.219 满足该下限。JEI JAR SHA-256 与验证记录及三个启动 classpath 指向的缓存文件一致：`8AAF547432F1B4958239B036356B910692FE40F858C3073D996F56BBF7C99826`。

MC 元数据原范围确为 `[1.21, 1.21.1)`，不能声称它直接包含 1.21.1。已阅读 `fml-version-support-matrix.txt` 的锁定 FML 4.0.42 证据：MC 为 1.21.1 时，为 `mod.minecraft` 加入 1.21 替代版本；主范围检查失败后再尝试替代版本。因此该范围由现有加载器兼容映射接受，而非修改 JEI 元数据或升级项目。已查看较新 19.57.0.450 元数据的 NeoForge 下限 21.1.238，确认不适用于锁定的 21.1.219。

这支持版本范围层面的兼容，不替代实际启动后的运行时兼容验证。

## 验证证据与独立复核

读取 `build/reports/development/DEV-JEI-01/` 中 dependencies.log、prepare-and-jar.log、verification.json、五组启动参数、JEI/项目元数据和两份插件/加载器字节码证据。已有日志记录 dependencies 与客户端/服务端启动准备及 jar 均 BUILD SUCCESSFUL，jarJar 为 NO-SOURCE。本审查未重跑这些 Gradle 命令。

用现有捆绑 Python 的 `-B` 模式执行只读脚本，未写缓存或其他输出，独立核对：

1. 原始依赖日志中只有六个配置节含 JEI：三个客户端 AdditionalRuntimeClasspath 与三个客户端 LegacyClasspath；compileClasspath、testRuntimeClasspath、runtimeClasspath、runtimeElements、apiElements、serverLegacyClasspath、gameTestServerLegacyClasspath 全部存在且均不含 JEI。
2. 三个客户端 classpath 各一份 19.27.0.340 JEI；server 和 gameTestServer 各零份。证据中的五份 classpath 与当前 `build/moddev/` 文件逐字节一致。
3. 实际项目 JAR 的 SHA-256 为 `CF93E36E4B0F874F58F340F5E07E307B57BED0EDBD093587F137511E7F811E62`，与记录一致。读取 ZIP 目录无 `mezz/jei/` 类和任何内嵌 `.jar`；内部 neoforge.mods.toml 无 JEI 字样，且与现有源码元数据逐字节一致。

因此现有证据可支持 JEI 不进入服务端、GameTest、JUnit、编译或发布依赖，以及项目制品无内嵌 JEI、无新增强制 JEI 依赖。

## 技能、范围及剩余验收

本轮重新读取 AGENTS 与 DEV-JEI-01 合同；复用本会话已读取的治理协议及以下技能：

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：核对 Minecraft 1.21.1 / Java 21 / NeoForge 21.1.219 / MDG 2.0.143 / Create 6.0.10-280 基线、客户端依赖边界与制品元数据。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：区分配置解析、启动准备、制品检查与真实客户端验收，不为构建依赖配置新增重复性测试或运行全量 GameTest。

尚未实际启动 Minecraft，JEI 物品列表及配方界面由用户下次启动后确认。本审查不证明候选工作树已同步，也不放行原有玩法人工门。

唯一写入是本报告。未执行 Gradle、游戏启动、Git 写操作、源文件修改或全局审计。
