# 锅炉事故候选制品

2026-10-27。已提交生产源 **7d2a28eb28260c3eb51502b9c9f8217f73d60259**；唯一执行者实际读取`C:/Users/IKSXH/.codex/skills/minecraft-ci-release/SKILL.md`并按本项目锁定版本/制品治理应用，没有照搬升级、修改CI/许可/发布方式。

- 命令：`.\gradlew.bat jar`。`jar.log`记录 **exit0 / 1秒**，compileJava/processResources UP-TO-DATE，仅jar执行，未clean/build/test/rerun，未重复逻辑测试。
- 候选树`build/libs/create_nuclear_industry-0.1.0.jar`：**3710963 bytes**，SHA256 **9829AC0E3FC6E2AA8424D1591059B64B183D60EADD19422E94058A3CB10D5297**。原F29包保preflight；没有Git写/复制或提升main。
- source-final.json的26项源SHA构建前后不变，HEAD为准确生产提交。JAR内 **820class＋1775资源＋manifest/三许可=2599载荷**逐项与本次现有编译/资源输出及许可/manifest文件绑定；未排除输出无遗漏，无重复/意外排除项。详细条目及SHA见ARTIFACT.json。
- 新`BoilerSafetyGameTests*.class`及`data/cni_boiler_safety/`模板域均不存在于候选JAR；其它既有域遵原排除策略，没有借打包清理旧探针。

行为依据复用FIX.md：定向52项通过，19个真实注册场景核心断言通过，最后读域1项实跑/18明确SKIP；原全JUnit865=851pass/3既有fail/11skip未隐藏，不称gradlew build通过。R4跨维度真实BlockItem/核心状态与旧任务/库存已过，Nether headless实体加载前置不足导致完整目的地成型交易仍为明确未验子项，保留集中真人手测；客户端显示/粒子/事故视觉与正常真人重进未由本次打包验收。

交PM核制品并提交候选；最终用户手测/合main门未越过。
