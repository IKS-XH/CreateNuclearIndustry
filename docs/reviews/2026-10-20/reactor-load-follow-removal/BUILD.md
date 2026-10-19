# AUTO-REMOVE-LOAD-20261020 构建与制品绑定

准确来源`2561b1d1021d4ccf52db2eabab44e1648dd16623`，唯一一次`.\gradlew.bat assemble --console=plain`退出0，Gradle报告1秒。1项jar执行，3项编译/资源任务UP-TO-DATE，复用已获48/48 JUnit和19/19真实服验证的同一实现；没有重跑测试、clean、强制重跑或重封候选。

本次assemble原输出`build/libs/create_nuclear_industry-0.1.0.jar`：**3523047 bytes**，SHA256 **74e95f6dcd024802f21b25a7028ebb067ff3fd83ab21d8e90b6a4e79eadd4090**。未执行Git写或main替换，本记录供PM组合审查与制品集成。

## 全部输入与输出

- 输入2282项，较上批2294减去本次12删除项。全部源码、资源、JUnit与构建文件和准确提交归一内容一致，并与冻结验证来源相同；构建前后原始哈希无变化，无未跟踪生产输入。
- 版本保持MC1.21.1、Java21.0.7、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6、Gradle9.0.0。
- JAR为2450项：762 class、1687处理后资源、1 manifest。全部class逐字匹配build/classes/java/main，并由SourceFile回指本次已提交Java；全部资源逐字匹配build/resources/main及源输入；manifest匹配build/tmp/jar/MANIFEST.MF。

## 相对main 222930f正式包

基线JAR SHA256为88f21a64cd483d2767bc0e5016282052b1f226835cf06f59bca833de71e51bbb，直接由该提交Git blob读取，没有使用main脏文件。

| 差异 | 数量 | 归属 |
| --- | ---: | --- |
| 删除class | 16 | 6个闭环专用类型及内部类、客户端WorldBinding、Network.Bind及CODEC内部类 |
| 修改class | 27 | 全部SourceFile属于本批14项允许生产Java；包含相关record/匿名类的字段或行号变化 |
| 修改资源 | 2 | zh_cn/en_us各仅删除同一25个闭环专用键，其它键值不变 |
| 新增载荷 | 0 | 无 |

其余735个class、1685个资源及manifest均与基线逐字相同，锅炉汽口及最新累计优化载荷不回退。完整逐项来源、SHA和差异分类见payload-manifest.json与payload-delta.json；ARTIFACT.json为附清单SHA引用的小型摘要，无须复制大清单进入治理文档。

ReactorLoadFollow*/ReactorStress*/WorldBinding/Network$Bind及闭环模板条目残留为0；class常量池中的相关旧类型、阶段、绑定包符号也无残留。专属GameTest和模板原本不在正式包，其源码及对应排除规则现已删除，不会重新进入JAR。

本阶段只做一次打包及身份核验，原48/48 JUnit、19/19真实服/正常退出0证据见IMPLEMENTATION.md。未补记真人客户端GUI或Ctrl交互观察。PM的README/核心文档和本批两份运行日志不属于生产输入，未由执行者改写或恢复。
