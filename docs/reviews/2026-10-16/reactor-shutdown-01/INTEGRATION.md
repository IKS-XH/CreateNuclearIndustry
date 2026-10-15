# 反应堆微小尾热停机拆除修复

2026-10-16，用户报告main一台16³反应堆始终提示“尚未完全停机”。依据用户已保存退出的唯一世界，只读副本实际定位仪表(-8,-59,0)。本批按既有明确任务自动派发/验收授权接入main；用户原场景拆除复看仍由[STATUS](../../../STATUS.md)独立维护。[实施卡](../../../archive/2026-10-16/plans/2026-10-16-reactor-shutdown-diagnosis.md)归档，原始失败及结果不改写。

## 根因与范围

现场149燃料列、47棒实际/目标全插，无卡死、新生热、燃耗或融毁倒计时；冷液658000mB、热液0。仅有5.247766665661402e-10HU缓存尾热，但量化标记为0。原Adapter误用1e-9的整数mB容差作为HU尾热资格门，完全停机却要求活动尾热不超过1e-12；原算法推进2000tick后完整快照不变。同族边界是三列各7.5e-13HU被逐列过滤，汇总超过停机门而没有安全标记。

生产仅修改Adapter的正热资格及ServerTick安全余数的逐列正数筛选。真实整数冷液门、完全停机epsilon、缓存热、实际流体转化、燃耗和危险拆除分支保持。修复按已有规则标记安全量化余数，不删除/取整库存热或改NBT；正常权威tick可处理原现场，无需编辑用户世界。相关阈值早于S1/S3性能优化已存在，未据时间邻近归因优化。

## 验证与审查

- 正确RED为18项中3条预期业务失败；更早数据占位解析失败另存，不当作RED。最小修复后6类55项JUnit通过，失败/错误/跳过均0，Gradle exit0。覆盖149列真实数据、分散微热、无冷液、守恒、20tick及当前NBT往返。
- 真实NeoForge普通服务端复用原模板/开发测试类及原生报告器，安全尾热正式tick后真实BreakEvent放行、清空事务完成、实际拆壳使结构失效。原生日志记录通过，正常保存全部维度并exit0。
- 当次另一用例误把tickReactor的“状态或物品是否变化”返回值当作结算成功，保留1过1夹具失败原记录。仅新增测试前置改为确认权威sample+1及fullyStopped=false，原BreakEvent取消、快照与实际燃料不变断言保留；只补跑该失败单例，通过并正常保存exit0。两生产改动和JUnit未变，复用原55项证据，不写成某次2/2。
- 入口筛空、工作目录覆盖导致RCON未开启、首次测试名错误及上述夹具失败均保留在报告目录。没有运行全套JUnit/GameTest、客户端或性能回归；独立旧2.25HU断言未改、不计通过。[一次组合窄审](REVIEW.md)直接核差异与原始日志，无阻断项。

原始诊断、source.patch/manifest、RED/GREEN XML、真实服务端日志及[最终交付](../../../../build/reports/reactor-shutdown-20261016/FINAL.md)均在build/reports/reactor-shutdown-20261016/。本批实际使用systematic-debugging、minecraft-modding/testing、TDD和隔离树流程；制品按minecraft-ci-release核对。版本基线保持MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。

## 来源与正式制品

PM只提交两生产、两JUnit及现有开发GameTest共5文件为850904df6220487f8903b00cc9936b8f68307d32，tree ce0bf3eb76c0899aa0f5f32958c8dd47a07ef57a；main从db1e6aa快进，未夹带日志或其他dirty。1994项实际生产/构建输入逐字匹配准确Git LF对象，构建前后不变。输入准备时Windows GBK误读UTF-8配置的错误在启动assemble之前被修正并保留；没有额外一次构建。

唯一assemble --no-daemon --console=plain用时15.473秒、exit0，compileJava使用缓存，资源处理与jar实际执行。正式包3333619 bytes，SHA256 `a963b483260ab732b16b74fe0c2c625d8e58328e882be675a0497c469b173ca3`。2276项载荷中仅ReactorCoolantSimulationAdapter.class与ReactorServerTick.class变化；其余697class及1577项非class载荷/Manifest逐字等于前78175包。全部699class匹配本次编译输出，全部包含资源匹配processResources，现有测试类排除保持，无探针/工具/报告泄漏。

PM独立读回构建日志、输入/载荷证明和冻结FINAL.json身份，核对新包及旧包备份SHA后替换正式路径并再次核SHA；正式JAR继续跟踪Git。此为准确来源assemble，不称全build或用户原场景已通过。

同级shutdown-fix隔离树保留。已完整携带27文件世界和7项有效TOML并核身份；原件与候选携带世界未启动、未回写，真实测试仅用独立临时世界。无关dirty、原正式包备份、失败日志和来源树均保留，不推送或发布、不接续其他任务。
