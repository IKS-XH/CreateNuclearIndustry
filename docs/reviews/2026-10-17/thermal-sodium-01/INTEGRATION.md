# THERMAL-SODIUM-01 用户验收与主分支合入

2026-10-17，主逻辑PM。用户明确反馈“渲染问题测试通过了，合到主分支里吧”，本记录据此接纳本次锅炉/汽轮机连接外观修复；不扩写未单独报告的其他Iris功能或多人整机验收。当前状态统一见[STATUS](../../../STATUS.md)。

## 来源与合入

- 已验候选来源`80153dd9f52bcd9139d4b5909abec666ff92641b`，本批恰两处客户端入口及两类真实Sodium回归测试；[四项来源](SOURCE.json)、[实现](IMPLEMENTATION.md)、[组合窄审](REVIEW.md)保持原证据。
- main原HEAD`470b5b0`。主目录存在无关dirty，本次在同级干净的detached集成树`E:/MyMC/NewMod/Create_NuclearIndustry-render-accepted-integration`完成三方合并，再将main快进至`ec2430956c1adf1552f6406398ba0f0ae4b29835`。没有代码冲突；所有src、build.gradle、gradle.properties和settings.gradle相对已验来源无Git差异。
- 集成树仅作本批已验来源的Git合并，无新玩法分支、不启动游戏、不读取/复制/回写世界。原候选树及其存档保留，未推送、发布或删除来源分支/工作树。
- 30份合入前实际存在的无关tracked修改逐项长度/SHA256核对，合入后0变化；其余dirty、未跟踪文件和运行日志不暂存。本批仅更新明确受授权的正式JAR工作副本。

## 正式制品

[正式JAR](../../../../build/libs/create_nuclear_industry-0.1.0.jar)：**3336385 bytes**，SHA256 **`B366EB49F8994BB0213B676F530AE395E03A58F53E59CBA23AB9A77B682D7606`**。

交付时报告目录候选路径现已无实物；本次只读核用户客户端实例`mods/create_nuclear_industry-0.1.0_261017_thermal-sodium.jar`仍为以上已验哈希，再原字节复制至main正式路径。没有修改安装包、用户实例或世界，也没有重新生成候选。源修复已在main提交后才提升正式包，随后将JAR与本验收记录纳入Git。

该包仍是已批准的baseline原载荷＋一次assemble九项已编译class的窄包：2269项保持、7替换＋2新增class、0删除/越界，详见[制品证明](ARTIFACT.json)。没有夹带主目录未提交工业管网/模拟器/工具内容，也未改变服务器、反应堆已验实现、资源和metadata。与用户实际测试包逐字节相同，现有客户端无需再换另一份包。

## 验证复用与文档收尾

沿用同一来源的有效RED、32/32定向JUnit（0失败/错误/跳过、exit0）、一次assemble/exit0及一次组合窄审；本轮新增的是用户联机外观通过、无冲突合并和正式包身份核验。依[治理5.1](../../../project-governance.md#51-按改动范围验证2026-10-02起生效)，没有再跑整套JUnit/GameTest、全build或存档扫描，也不把历史运行写成本轮新测试。

完成卡移入[归档](../../../archive/2026-10-17/plans/2026-10-17-boiler-turbine-sodium-01.md)，活动入口撤下；原候选/审查报告保留当时的待手测语境，通过本记录接续。辐射、贫化铀、其他历史观察门均保持原状态。

本轮实际应用Minecraft modding/testing核客户端边界与证据复用、minecraft-ci-release核来源与制品身份、finishing-a-development-branch执行用户已选的本地合入；治理5.1和保留世界/来源树的项目规则优先于技能通用的重跑全量、再次询问合并选项和自动清理示例。
