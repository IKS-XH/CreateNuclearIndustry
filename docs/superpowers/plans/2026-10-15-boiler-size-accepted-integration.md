# 已验锅炉尺寸上限合入

**目标：** 根据用户2026-10-15“锅炉的尺寸上限变更和新的外观手动测试都通过了”，关闭锅炉16³候选人工门，将已审30020e4的尺寸净增量合入main并绑定正式JAR。外观按锅炉范围登记，不扩大为装配台15C动画验收。

**执行：** PM管理文档/Git，一位原集成执行者负责适配和制品核验，一位原审查者仅做一次组合窄审；已确认授权自动接续，不重问玩法或机械重复原测试。基线main83cd1b6；原候选和其他树保留，不开新树或搬世界。

## 写集与不变量

- 执行者先冻结当前dirty、源码/构建输入、正式33da9002…JAR与运行配置/世界；报告及工具仅写`build/reports/boiler-size-accepted-integration/`，交付报告可写`docs/reviews/2026-10-15/boiler-size-accepted-integration/IMPLEMENTATION.md`。
- 生产仅`src/main/java/com/iksxh/create_nuclear_industry/config/BoilerConfig.java`与`boiler/BoilerState.java`：沿当前main把默认最大11/校验32改16。主线2048/4096流量字段、双汽库存、主动出汽、温压、管理员和美术保持，不用旧候选整文件覆盖。
- 测试仅原候选的`src/test/java/com/iksxh/create_nuclear_industry/boiler/BoilerStateTest.java`、`src/main/java/com/iksxh/create_nuclear_industry/gametest/BoilerSizeLimitGameTests.java`、`gametest/ExtensionBoilerGameTests.java`和`src/main/resources/data/create_nuclear_industry_boiler_size/structure/boiler_size_empty.nbt`。按现行Settings签名适配原29/3项合同；原11格偏心参考不变，旧12拒绝改17。
- `build.gradle`只在现有延迟Jar配置中增加新`BoilerSizeLimitGameTests*.class`及`data/create_nuclear_industry_boiler_size/structure/boiler_size_empty.nbt`两排除，保持全部原排除；不引入测试域载荷泄漏，不改任务/依赖或其他打包行为。
- 主目录`run/config/create_nuclear_industry-boiler.toml`完整备份后仅dimensionRange从[5,11]改[5,16]，保其他键/字节，配置不进Git。无游戏进程证据已由PM核实；用户世界完全不写，其他运行配置保持，不研究旧版本迁移。
- 必读当前AGENTS、治理、原尺寸卡/候选/实现与审查；实际用minecraft-modding/testing/ci-release及verification-before-completion，锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。中文注释。

## 顺序与检查

- [ ] 冻结基线并适配七项来源，交净差异/冲突说明和配置单键证据；PM确认没有旧端口等主线回退。
- [ ] 原29/29与真实3/3自然退出及人工门作为既有证据复用；本次因Settings后来增加流量字段，适配后只运行受影响`BoilerStateTest`、`BoilerDualSteamInventoryTest`、`IndustrialPortRateTest`一次，确认默认、17拒绝、守恒和主线流量。无新失败不扩大真实域或整套build。
- [ ] 原审查者一次组合窄审，重点增量最小、现行设置字段保持、旧边界断言和打包排除。必要整改只复验受影响部分，不增加多层审查。
- [ ] PM提交准确源码；执行者从该干净Git树导出并核全部输入后，唯一必要增量assemble。不clean、不覆盖main的其他dirty、不在复制完成前启动构建。
- [ ] 核全部应打包class/资源/Manifest、新测试排除及非本批主线载荷保持，保留新旧包；PM核SHA后更新Git跟踪的正式JAR、文档与验收，其他树/世界不变。不推送、发布或开启下一主线。

若出现超出写集的问题先报最小链和建议，未经PM扩卡不顺手修复。执行者不做Git写操作、核心文档更改或子代理派发。审查报告仅本批`REVIEW.md`，不运行测试或构建。
