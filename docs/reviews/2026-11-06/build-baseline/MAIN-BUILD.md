# 主目录正常构建入口交付

- 工作区 `E:/MyMC/NewMod/Create_NuclearIndustry`，main准确源 `25abcf437e09861e57985c979b228ace285e5b00`，HEAD前后不变。仅执行一次 `.\gradlew.bat build --console=plain`，无clean/rerun/exclude；没有修改源码、Git或用户世界。
- 2026-11-06 17:45:00.317～17:45:50.910 +08:00；exit0；墙钟50.592秒，Gradle50秒。9项actionable：processResources/jar/test共3项实际执行，compileJava/compileTestJava两项FROM-CACHE，其余4项UP-TO-DATE。完整任务状态保存在MAIN-BUILD.json/tasks与gradle.log。
- JUnit实际153类、871项：860通过、0失败、0错误、11既有Sodium夹具门SKIP；与候选一致。本轮test实际执行，未将其称作缓存通过。11项完整名称/源文件行/原@BeforeAll assumeTrue条件都保存在MAIN-BUILD.json/junit/skippedCases，原因是未提供cni.sodium.fixtureDir，未改任何禁用/跳过规则。1400裂变/600完整tick严格比较输出仍在差分类XML中。

## 输入与用户工作

- 2426条输入（src/main、src/test、Gradle文件）构建前后SHA完全一致；准确源与候选相同，主树与候选的文本物理换行存在既有差别，不声称两树源文件原始字节相同。
- 初始状态 `status-before.txt` 保留了正式JAR工作副本D、2项tracked 3D改动及原untracked。生成正式路径后仅JAR由D变M，其余所有状态行保持。原2工具SHA前后保持：package.json `1C0D0AE2818A8BF62398AFCB3F33013788CFC083C42AFA5092BCA0910A4FB990`；serve.mjs `D7402108AE99217AD555C85969E0CF3DD04EA218A78DBC710D5B82D43155E63D`。
- 原3个额外打包文本（LICENSE/NOTICE/Create许可证）在初始/完成Git状态中均无修改，当前SHA与包中载荷逐字节绑定，见packaging-sources-after.sha256。这3项未在最初2426条预构建SHA清单中，故不补写未实际取得的历史SHA；来源以HEAD、前后clean状态与当前打包字节绑定记录。

## JAR与逐载荷核验

本轮在原缺失的正式路径正常生成JAR：3781913 bytes，SHA256 `0B912E12FA128F485432A3E1FB71937D2A88EA3844A3F7F07FB9E9B79095D932`。没有拿候选覆盖主目录包；原E0AB完整JAR仍保存在候选 `build/reports/build-baseline-20261106/full-build/original-E0AB.jar`。

相对原E0AB与候选E0AB：2621载荷，2310原字节保持，311修改，0新增/0删除。每个修改项均用严格UTF-8解码，将CRLF统一为LF后逐字符严格相等；仅行结束符不同，不套用旧303项豁免。311项分别为78 JSON、5 MCMETA、223 OBJ、2 MTL和3许可证/归属文本；几何/UV/法线/材质指令与JSON字段正文保持。`payload-deltas.csv`及MAIN-BUILD.json/payload.changed逐项记录双侧大小/SHA、CRLF和LF计数、源码路径/SHA及分类，311项新载荷全部与对应当前来源文件字节绑定。

3项非src/main/resources来源单列：

| 包内载荷 | 实际来源 | 旧→新CRLF数量 |
| --- | --- | --- |
| LICENSE | LICENSE | 21→0 |
| NOTICE.md | NOTICE.md | 75→0 |
| META-INF/licenses/Create-LICENSE.md | tools/art-assets/industrial-fluid-visual-01/CREATE-LICENSE.md | 35→1 |

825包内class全部与原E0AB保持，并与本轮main编译输出逐字节一致；缺失/不等0。编译目录其余21项均为既有打包规则排除的开发GameTest类，完整名称在MAIN-BUILD.json/classBinding。生产行为字节保持，没有生产Java或配置修改。

## 原始证据

本目录保留gradle.log、process.json、153份完整TEST-*.xml、head-before/after、status-before/after、inputs-before/after.sha256、user-tools-before/after.sha256、packaging-sources-after.sha256、MAIN-BUILD.json、payload-deltas.csv与只读核验工具audit.ps1。候选报告在隔离树 `build/reports/build-baseline-20261106/full-build/BUILD.md` 与BUILD.json；其中audit.ps1确实存在并实际执行，另保留的audit.py是不可用Windows Store别名触发后未执行的初稿。

此报告只说明本卡自动构建/单元测试及制品字节核对，不替代被SKIP的Sodium真实夹具、客户端视觉或多人验收。两次正常build均完成；正式JAR当前为待PM核对的工作副本M，执行者没有暂存、提交、替换别的来源或更新核心文档。
