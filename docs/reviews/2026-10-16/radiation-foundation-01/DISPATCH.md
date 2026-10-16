# RADIATION-01 派发依据与工作区

2026-10-16，主逻辑PM记录。当前进度统一见[STATUS](../../../STATUS.md)，本文件只保存本次派发证据，不代替实现、独立审查或用户手测。

## 授权与范围

用户先批准[事故与辐射整稿及推荐值](../../../superpowers/specs/2026-10-16-accident-radiation-design.md)，再明确“开始按批派发吧”。本次派发[第一批计划](../../../superpowers/plans/2026-10-16-radiation-foundation-01.md)全部Task 1～4；唯一执行者`/root/radiation_foundation_impl`，模型`gpt-6.1-sol`、high，因跨物品事务/库存/生命周期/屏蔽及网络，需要完整接入判断。结束后一次组合窄审，执行者不再派子Agent。

本批只做使用标记、辐射源/剂量、普通物流和封存屏蔽、防护服与仪表。事故世界动作、残骸/新废料及其封装出口为第二批；第一批人工门未闭前不越过依赖。贫化铀候选等独立人工门不追认通过。

## 工作树与原始输入

- 候选根`E:/MyMC/NewMod/Create_NuclearIndustry-radiation`，分支`codex/radiation-foundation`，执行基准`5e706e535d97cbcfe167fa4338265b44a610964d`。
- 从main `a7f2559`创建后，只快进同步本批派发管理记录；生产来源保持`850904df6220487f8903b00cc9936b8f68307d32`。执行前候选Git干净，源树所有无关dirty和正式JAR工作副本删除保持原状。
- 桌面附件没有可直接复用的活动工作树；其他同级候选仍有独立内容/人工门，不改它们。原生创建接口不接受用户要求的同级目录，故按目录约定使用Git工作树命令，未清理来源树。
- main的`run/logs/latest.log`在2026-10-16 08:20:59记录玩家退出、所有维度保存完成；各世界`session.lock`可独占只读打开。当前游戏进程只读`user.dir`指向整合包实例，未终止该客户端或改其数据。
- 来源main `run/saves`完整复制到候选同位置：27文件、13337924 bytes，逐项路径/长度/SHA256一致；目标此前不存在，无覆盖/世界混合。来源原件保留，`run/config`同时复制，候选世界仍被Git忽略。
- 存档清单位于候选`build/reports/radiation-foundation-20261016/saved-world-sha256.json`，不进Git、不允许执行者改写。该复制是当前保存世界的携带，不是迁移或旧版本兼容验证。

## 执行与证据出口

执行者只按卡内写集修改，Git只读、核心文档只读；普通编译输出只在候选产生。自动化世界限定`run/radiation-foundation-gametest/`，不运行携带的用户世界或main世界，不替换main正式包。

实际交付出口为候选`build/reports/radiation-foundation-20261016/REPORT.md`及其原始证据；要求定向JUnit、必要真实服物流/状态/屏蔽/掉落/穿戴、性能计数和制品身份分别记录。未执行、未稳定复现、进程保存/退出异常须如实说明，不能写成通过。独立审查复用这些证据，不机械重复运行。

本次PM实际使用工作树与子Agent执行技能，并读取Minecraft modding/testing技能核锁定版本和验证边界；治理5.1/角色权限优先于技能通用的逐任务重复审查、执行者提交及全量基线测试示例。baseline生产代码未变，原相关证据可复用；新增风险由本批定向测试证明。
