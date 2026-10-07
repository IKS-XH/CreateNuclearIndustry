# 材料03：人工通过与钢材名称修订

**后续结果：** 本页保留03A交付时证据。03B现已完成剩余155项required回归并将钢材合入main，最新启动入口、制品与保存退出限制见[最终验收](../material-03b/README.md)。

用户确认材料03的[完整客户端清单](../material-03-client.md)全部通过，并要求游戏内普通“合金钢”简称“钢”。PM登记七组人工项通过，包括三制粉入口、两种混粉与动力恢复、三种熔炼、压板与维修身份、JEI/外观、保存重进；这不是完整自动回归的通过证明。

## 名称修订

候选提交`c860cf7538a5bc9f7ac19e27fd61df643aa8f6d1`只修改`zh_cn.json`和`en_us.json`的18个值：钢粉/钢锭/钢板、Steel Dust/Ingot/Plate，以及对应的燃料列和控制棒列维修提示。每语言仍123键，注册ID、配方、测试、纹理和存档格式均不变。现行内容与配方文档同步普通钢材简称，强化合金钢等其他材料及历史报告不重命名。

执行者`processResources jar`退出0；PM独立打开最终JAR，两语言各123键与源文件逐键一致、差异0，三物品名称正确。没有新增测试或重复整链手测；前批265项JUnit和14项专项仍按原日期/版本记录，不冒充本轮新跑。最终JAR SHA-256为`4AB547405137075565DEF7FCD51C3F3CF4D16D630A11F78BC2953B27CF2B4501`。[名称交付报告](./EXT-A-MATERIAL-03A.md)记录源文件哈希、命令与实际技能。

## 尚未解除的整合门

[只读回归诊断](./EXT-A-MATERIAL-03A-REGRESSION-AUDIT.md)指出旧测试延迟回调内继续调度，会在运行器迭代同一map期间写入。它与既有fastutil异常有一致的候选解释，但未经隔离复现，不能认定具体测试或因果已经定位。后续最小验证为隔离单测`P1ControlGameTests.redstoneOnControlRodDriveDoesNotTriggerScram`并记录实际调度/异常；没有再次盲跑155项，也没有修改旧测试或生产代码绕过断言。

显示名修订已完成；完整155项GameTest仍未完成，钢材链尚未合入main，不派发依赖它的下一批零件/设备。人工门已解除，不再要求用户重复确认或重测本批。当前使用新名称仍从`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`运行`gradlew.bat runClient`；已运行的客户端需重新启动才能加载更新后的内置语言文件。

PM依据[03A任务卡](../../../archive/2026-10-08-completed-plans/2026-10-01-ext-a-material-03a.md)审核纯显示范围、18值差异与制品，管理文档和Git；执行者未做Git写。主工程既有`.vscode/launch.json`保持，不迁移或修改世界，不发布或推送。
