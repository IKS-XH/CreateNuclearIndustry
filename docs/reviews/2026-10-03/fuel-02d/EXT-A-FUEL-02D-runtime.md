# EXT-A-FUEL-02D 运行实现交付报告

- 日期：2026-10-03；工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；分支：`codex/ore-acquisition`；开工 HEAD：`675e7a4`，开工工作树干净。执行者未做任何 Git 写操作。
- 实际读取并应用：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`；按仓库锁定 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280 API 核对，未改依赖。

## 已落地

- 新单格屏蔽装配台、正式方块物品、BE/唯一完整 ItemStack 账本、四输入与单输出、专用四料配方 codec/网络同步、注册、双语言、工作台配方、loot 和工具标签、JEI 四料数量与参考 64RPM 20 秒、护目镜及世界内状态、客户端 Create 原生半轴渲染。
- 底部动力输入，四料顶部自动输入、水平四侧共享一个成品输出，底部/null 无物料能力；机械臂专用点只接受投料，禁止从机身抽成品。8/4/2/1 完工原子扣取并产新建正式满耐久组件；32RPM 下限、每 tick 256RPM 工时上限、4SU/RPM；缺料退料及失配清无效进度，暂停保留合法进度。
- 普通采掘、潜行扳手和直接替换使用唯一携物机器快照；重新放置恢复完整物品栈及进度。不修改旧换料接口、旧机器或构建脚本。

## 已测证据

- `JAVA_HOME=C:/Program Files/Java/jdk-21; .\gradlew.bat test --tests '*ShieldedAssembly*Test' --console=plain`：最终一次执行退出码 0；`ShieldedAssemblyStateTest` 3/3，通过数量守恒、模拟只读、速度上下界、停机/退料和完整栈组件保存。原始 XML：`build/reports/extension/EXT-A-FUEL-02D/TEST-com.iksxh.create_nuclear_industry.production.ShieldedAssemblyStateTest.xml`。该次还完成新增 Java 编译。
- `JAVA_HOME=C:/Program Files/Java/jdk-21; .\gradlew.bat -PgameTestNamespace=create_nuclear_industry_02d -PgameTestDirectory=build/gametest-fuel-02d runGameTestServer --console=plain`：首轮退出码 1，2例中1例通过；真实漏斗投满8芯块的断言仅等35tick，短于原版逐件漏斗传输时间。见 `build/reports/extension/EXT-A-FUEL-02D/gametest.log`。仅把该测试等待时点顺延后复跑同一命名空间：退出码 0，2/2 必需用例通过，正常保存退出，见 `build/reports/extension/EXT-A-FUEL-02D/gametest-rerun1.log`，日志 78-79 行及 93 行。
- 真实用例覆盖已加载专用/工作台配方、原版漏斗顶进、顶/侧/底/null 能力及模拟、Create 机械臂实际点模拟/提交/禁抽、真实 Creative Motor 底轴 256RPM 完成一批、正式物品 216000 最大耐久且初始损伤 0、Create `updateFromNetwork` 过载状态暂停、满输出第二批四料守恒、水平多侧唯一成品槽、单件携物掉落/恢复以及失配保料清进度。过载场景用 Create 的公开网络更新入口设置容量小于负载；不是从自然发电源搭建的容量耗尽管网。
- JSON 已逐一解析；任务1受控修改路径 `git diff --check` 无空白错误。Gradle 产生的仓库已跟踪 `logs/debug.log`、`logs/latest.log` 在开工时干净，PM 已知并负责证据保留后恢复；执行者不作 Git 写操作。

## 最终打包与证据复用

- 收到素材“首版冻结”通知后曾执行预检 `assemble --console=plain`，退出码 0；随后 PM 看图指出物品模型问题并撤销该版冻结。该日志只作历史预检：`build/reports/extension/EXT-A-FUEL-02D/assemble.log`。
- PM 在任务2整改素材完成、实际看图通过后明确允许最终增量打包；执行 `JAVA_HOME=C:/Program Files/Java/jdk-21; .\gradlew.bat assemble --console=plain`，退出码 0，耗时 3 秒，`compileJava`、`processResources`、`jar` 均执行，见 `build/reports/extension/EXT-A-FUEL-02D/assemble-final.log`。最终 JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，1,012,135 字节，SHA-256 `D6DEBDDB38DE7B423A11927EBDB7CF0A344160C99986C2A00B204E31BE1DBC4D`。
- 逐条对比 JAR 中新方块状态、4个模型、5张PNG、两条配方、loot、双语言、两条工具标签与当前工作树原始字节：17/17 完全一致、缺失0、失配0；详见 `build/reports/extension/EXT-A-FUEL-02D/resource-check.txt`。实际游戏内视觉仍属人工门。
- 成功 GameTest 后仅修改世界内等待原因显示、对应双语言文本、注册入口职责注释、JEI 未用 import，并把机械臂旧保存 `TAKE` 模式读回时归一为 `DEPOSIT`。独立 reviewer 已确认这些尾改不影响已过的四料事务和两例真实场景；PM 按治理 5.1 确认复用原 JUnit/GameTest 证据，无需补跑。上述测试结果不表述为尾改后的新运行；最终源码由增量 assemble 编译通过。

## 待人工门
- 四组客户端人工门均未执行：①制造/JEI/模型，②准确批次/错料/满输出，③混合物流/机械臂/动力暂停，④存档携带拆放/组件装入既有换料端口。服务器、单元及静态证据均不代表客户端手测通过。
