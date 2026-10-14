# EXT-A-FUEL-02E 任务1运行交付

2026-10-03，隔离工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支 `codex/ore-acquisition`，开工 HEAD `5d24c3f`，功能基线02D `3f7b207`。仅改任务1写集；素材写集由另一执行者负责。未执行 Git 写操作，未触碰默认客户端或既有进程。

## 实现

- 主控 `shielded_assembly_station` 增加 `expanded`；旧世界缺该属性为 `false`，原位停机并保留四料/成品/工时，手动取料和收起仍可用。新放置（含旧携物）一件生成八格，北向 `part=x+2*z+4*y`，按主格中心转四向。七个 `shielded_assembly_part` 没有物品、库存、动力或制造 ticker；UUID 与主控坐标限制归属。
- 放置前查八格替换性、已加载状态与玩家权限，失败不消耗物品；异常中途失败清理已生成代理。主控完整性逐次核验，缺件/未加载时停机并关闭新旧能力句柄，不清库存或工时。代理主控区块暂未加载时按20/40 tick 间隔重查，确认主控已加载且无匹配归属时静默清理，不强载、不掉第二件。
- 16个水平外表面共享5槽，四个顶面仅四输入；底面、内部及 `null` 无物料能力。四输入只按身份插入，成品槽只从侧面提取。玩家、护目镜、机械臂代理同一主控；Create 安山与黄铜原生漏斗已在真实服务端场景验证。主控独占底部轴与4 SU/RPM，应力和02D 8/4/2/1→1、32RPM、25600 RPM·tick、容量均未调整。
- 主控/代理普通挖掘、直接替换与潜行扳手共用唯一携物快照；`destroyBlock(true)` 不会重复掉落，错误工具成功拆除也保留库存。未加载跨区块普通挖掘取消并提示稍后重试。Create 构造禁止单独搬移主控或代理；普通扳手提示收起重放。
- 代理同步归属更新包及主控 `working` 投影。主控 renderer 静态绘制三件活动 partial 与原生半轴，完整八格 AABB 随朝向变化。Flywheel/JOML `rotateCentered` 正角与方块 JSON `y` 约定不同，EAST 用 `-π/2`、WEST 用 `+π/2`，绕主格中心旋转；本批无活动动画时序。
- 新 `create:mechanical_crafting` 5×5/21格配方为12铅板、6钢板、2完整机械手、1辐射传感器；旧9格工作台配方删除。双语言、代理 loot/挖掘标签及02E fixture 已接入。JEI 保留生产配方分类与整机入口，制造配方交由 Create 原生显示。

## 自动证据

环境固定 `JAVA_HOME=C:/Program Files/Java/jdk-21`。本批增量 `compileJava` 与 `processResources` 均退出0；未跑 `clean`、全量 `build` 或无关测试。`ShieldedAssemblyState` 算法没有改，复用02D报告 `build/reports/extension/EXT-A-FUEL-02D-runtime.md` 中定向 JUnit 3/3（原始 XML 位于该报告所列路径），不写成本轮新运行。

命令：`$env:JAVA_HOME='C:\Program Files\Java\jdk-21'; .\gradlew -PgameTestNamespace=create_nuclear_industry_02e -PgameTestDirectory=build/gametest-fuel-02e runGameTestServer --console=plain`。最初两次退出1，分别暴露测试在 Create 漏斗 BE 首轮初始化前调用接口，以及动力测试时点晚于一批完成；均修正测试等待/断言时点，未删除业务守恒断言。后续为审查发现的去重、同步、错误工具、原断言保留和孤儿时序补测而定向复跑。最终补足 `maxDamage=216000` 断言后，于 2026-10-03 16:09:27 退出0，`create_nuclear_industry_02e` 5/5 必需 GameTest 通过，正常保存并退出；原始日志 `EXT-A-FUEL-02E/gametest-latest.log`。

五个场景合计实际验证：占位拒放与一件八格放置/四朝向、16侧面和4顶面的真实 capability/内部与null、跨面同账本及模拟只读；安山/黄铜漏斗真投料、黄铜侧抽成品；Create机械臂模拟/提交/禁抽、底轴动力、过载冻结工时并使七灯熄灭、恢复完成正式满耐久成品、满输出第二批不扣料；八拆点唯一携物（含 `destroyBlock(true)` 与错误工具）、旧单格保料暂停及旧携物升级、合法1234进度与四槽搬迁、非法料保栈清进度；归属错配及模拟未加载主控时能力失效、代理等待主控区块可见后静默清理。

素材由PM确认冻结后曾按指令增量 `assemble`，之后审查补项改变测试/源码，实际共执行 **4次** `assemble --console=plain`，每次退出0；最终一次在216000耐久断言通过后执行，`compileJava/processResources` 为 UP-TO-DATE，`jar` 执行，见 `EXT-A-FUEL-02E/assemble-final-console.txt`（从本对话工具结果原文保存）。`git diff --check` 退出0，仅有工作树 LF/CRLF 提示。

最终 `build/libs/create_nuclear_industry-0.1.0.jar` SHA-256：`059116471CF42FF5267D4F2F2082F7858FD0562FB6A5AAA6028C1B07CBADC8D0`。归档入口检查：16个分块状态模型、3个活动模型、10张新PNG、主/代理 blockstate、双语言、02E fixture及新机械合成均在包内；旧工作台配方不在包内；机械合成模式实际为12/6/2/1。38个本批资源条目与当前 `src/main/resources` 源文件逐字节 SHA-256 一致，失配0。明细 `EXT-A-FUEL-02E/jar-inventory.txt`、`jar-byte-compare.txt`、`jar-sha256.txt`。

## 人工边界

服务端自动测试不证明客户端的整机视觉、漏斗贴面手感、JEI/21格实际搭建、护目镜显示、潜行扳手操作体验或真实跨区块卸载/重载。未加载测试使用未加载的远处主控坐标并验证重试与清理，未强制加载测试区块。后续按任务卡四组用户客户端人工门验证；当前不宣称人工验收通过。

已实际阅读并应用 `AGENTS.md`、治理协议§5.1、02E实施卡与已确认方案，以及 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`.../minecraft-testing/SKILL.md`；按仓库锁定 Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280 和 Ponder1.0.82，未引入技能示例中的新版本。
