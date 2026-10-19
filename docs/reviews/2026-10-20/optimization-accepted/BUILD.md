# OPT-INTEGRATION-20261020 构建与制品绑定

来源提交`a1ca0a6c249e81fd6ed66a25243964f72bab61b2`，唯一一次`.\gradlew.bat assemble --console=plain`退出0，20秒；4项任务中3执行、1项UP-TO-DATE。未clean、未强制重跑、未重复业务/游戏/性能测试，未重封候选。编译有12条既有弃用警告，无编译错误。

正式候选为本次assemble原输出`build/libs/create_nuclear_industry-0.1.0.jar`：**3560094 bytes**，SHA256 **88f21a64cd483d2767bc0e5016282052b1f226835cf06f59bca833de71e51bbb**。本记录不代替PM验收或Git集成，执行者没有Git写。

## 输入与完整载荷

- 实际Java为Oracle JDK21.0.7；工程仍锁MC1.21.1、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6、Gradle9.0.0。
- 2294项已跟踪源码、资源、测试及构建配置逐项与来源提交归一内容一致，构建前后原始哈希全同；src/main及src/generated没有未跟踪生产输入。PM未提交docs不属于生产输入。
- 最终30项分类为16个H1生产Java、2个T8开发GameTest Java、1个H1 Mixin清单、11个H1新增JUnit。最初28项复制审计保留在initial28文件，不能把30项统称全部同H1。
- JAR共2466项：778 class、1687处理后资源、1 manifest。每个class逐字绑定build/classes/java/main并由SourceFile回指已提交Java；每个资源逐字绑定build/resources/main和实际已提交源输入；manifest绑定build/tmp/jar/MANIFEST.MF。

## 与H1逐项比较

条目集合完全相同，没有新增或删除载荷：

| 分类 | 数量 | 核对结论 |
| --- | ---: | --- |
| class原始字节相同 | 710 | 直接SHA核对 |
| 仅局部变量调试表不同 | 66 | 只移除LocalVariableTable/LocalVariableTypeTable后，重建常量池的完整结构字节相同；字段、方法、参数元数据、描述符、注解、指令及验证帧均保留 |
| 开发夹具内部类仅行号变化 | 1 | IndustrialFluidNetworkGameTests$1，额外移除LineNumberTable后完整结构相同，SourceFile仍保留 |
| 授权开发夹具变化 | 1 | IndustrialFluidNetworkGameTests，仅sharedInputEdgeChargesExtractedFluidIncludingShortReceipt及其lambda回调变化，方法签名集合和类/字段结构保持 |
| 资源仅CRLF→LF | 1 | create_nuclear_industry.mixins.json，归一字节和JSON内容均相同 |
| 其它资源及manifest逐字相同 | 1687 | 全量SHA核对 |

BoilerIndustrialActiveOutputGameTests按既有jar规则排除。IndustrialFluidNetworkGameTests未排除，变化来自已授权T8/VALIDATION.patch：输入改为8192强化阀，原短收守恒值保持；其内部类行号随源文件插入变化。不能声称正式包全部payload与H1逐字相同。

ASM9.8只读检查工具在报告目录，未进入产品。工具使用空ClassWriter重建常量池，只过滤指定调试表，不过滤参数元数据、注解、字段、方法签名、Code指令、异常表或验证帧。非调试夹具差异限制为上述两个方法；未发现其它非调试差异。逐项哈希与分类见payload-manifest.json、class-structure.tsv。

## 证据和边界

机器可读身份为artifact.json；全部输入见inputs-before.json/inputs-after.json；唯一构建记录为assemble.log和assemble-exit.json。原T8/T9/H1定向JUnit、真实服及受控性能证据按INVENTORY.md复用，已知基线失败保持原状。本次没有新增人工客户端、真人多人或长时验收结论。

构建输出已经与准确已提交源绑定，可交PM进行组合审查及后续Git集成；没有写main、用户包或世界。
