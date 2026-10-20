# ADMIN-RELEASE-01：拆除自动化接口后释放逐棒控制权

用户2026-10-20手测事故候选发现：曾接入管理员的反应堆拆掉接口后逐棒仍只读，必须重放仪表。按既有“没有管理员时局部控制照常”合同修复，不混入已另行批准、但须等本项人工复测的高温/熔融物玩法。结果与人工门只记STATUS。

## 来源与职责

- 主线生产基准`d39eeb4bf3c1ad931fab14e90e70171484fb0f33`，实际建树HEAD为其后仅文档提交`7ea305cad732b7bb26bc8c14504130bae059c12a`。使用同级隔离树`Create_NuclearIndustry-admin-release-fix`、分支`codex/admin-release-fix`；新树携带已保存main世界并核对，不打开用户世界，真实测试另用独立目录。
- 同一唯一执行者将最小修复移植到既有事故候选树`Create_NuclearIndustry-accident`，原HEAD `fc9cbd888864566eaf53c6977b7be838ad231a67`。这属于原事故批手测整改，不复制main世界覆盖事故测试进度、不顺带合入其它main增量。
- 执行者无Git写权限，不改核心文档/任务状态；PM负责准确来源提交、一次组合窄审与候选登记。两个候选均等用户复看，不自动合入main/事故正式验收或推送。

## 已定位链路与不变量

`ReactorAutomationService.onPortRemoved/revoke`撤运行许可后仍保持claimed，`ReactorAutomationState.rodsLocked`据claimed及phase锁逐棒。普通故障保留接管是既有正确合同，不能把所有失联一律解锁。

1. 当前已加载、正式验证有效且AUTOMATION端口数为0时，明确结束接管：清旧绑定/实例/观测/曲线/续接意图和运行许可，撤旧逐棒会话，合法推进epoch，同步驱动只读投影；同一仪表下恢复逐棒交互。
2. 不清空燃料、工质、积热或实际棒位，不伪造完全停机，不解除SCRAM/卡死保护；旧失联安全全插目标不还原成危险旧目标。
3. 临时区块不可用、未验证/无效结构、仅拆燃烧室、供能或冷却故障仍保留原保护。当前格式保存的无接口接管状态经正式核验可退出，正常保存续接保持，不做旧版本迁移。
4. 同位重装接口/燃烧室应成为安全停机的新会话/代际，不自动启动或接受旧GUI/逐棒请求；重复重扫不每tick重复推进epoch。

## 唯一允许写集（两树相同范围）

- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationService.java`、`ReactorAutomationState.java`。
- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`。
- 如区分真实移除与卸载必要，允许`automation/ReactorAutomationPortBlock.java`、`ReactorAutomationPortBlockEntity.java`对应生命周期分支；不重构其它行为。
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationAuthorityTest.java`、`ReactorAutomationResumeTest.java`。
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/ReactorAutomationRetrofitGameTests.java`：复用既有构造、域、模板和排除，不新增构建规则/通用夹具。
- 短报告/临时核验仅各自树`build/reports/admin-release-20261020/`；正常编译输出允许。

## 实施与核验

- [ ] 实读AGENTS/治理/STATUS、本卡、管理员合同及本机Minecraft modding/testing/ci-release、systematic-debugging/TDD技能；版本和中文注释规则保持。
- [ ] 先用真实“任职→安全拆接口补壳→同一仪表重新成型→逐棒交互”取得RED，再最小修复；不只改客户端锁或放宽Slider校验。
- [ ] 定向真实域覆盖退出/同步、重扫幂等、保存重进无接口、重装安全接管；仅拆燃烧室、临时卸载和SCRAM不能借机解锁。只跑必要业务场景，分开记录业务断言和框架总数。
- [ ] 主线跑授权/控制器/续接相关定向JUnit；事故树移植后跑真实退出及事故接管保持的必要场景，不重复事故全矩阵。全部失败、命令和退出码保留。
- [ ] 先交两树未提交差异与报告，停等PM组合窄审及准确源码提交；之后每树仅一次增量jar/assemble，核来源、条目与测试类排除。不替换main正式包，不合并/推送。

人工复看：更新候选中正常安全停机后将接口换回外壳，同一仪表下逐棒可调，再看重装管理员/SCRAM保持；未反馈不称手测通过。
