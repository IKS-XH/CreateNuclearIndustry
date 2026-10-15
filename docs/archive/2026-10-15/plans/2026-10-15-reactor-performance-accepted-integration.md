# REACTOR-PERF-INTEGRATION：已验性能优化接入

日期：2026-10-15。需求来源为本主逻辑对话“优化的效果不错，合并到主分支里吧”；原候选对话“诊断超大反应堆性能”（01a13d6f-e26b-7710-aa55-064133dcbc40）的用户逐批批准及最终效果反馈已只读核对。状态与最终结论只写入[STATUS](../../../STATUS.md)，本卡保留当时执行边界；交付见[集成记录](../../../reviews/2026-10-15/reactor-performance-accepted-integration/INTEGRATION.md)。

## 目标与来源

- main基准7e214b979f1d3bb5528278bb117a2867914f88d7；生产前件876346979190fb6f49c73e4070e36ef127af9d54。
- 来源目录：E:/MyMC/NewMod/Create_NuclearIndustry-reactor-performance-diagnosis-20261015；完整候选在C1L/source，最终手测包C1L/artifacts/c1l-performance.jar，SHA256 fe446ef51c63a07430bf4daed40d8f246a11612fc5fdd022c29fc7ee77e1b0dc。
- 接入S1端口索引、S3裂变邻接与精确反馈缓存、C1客户端辉光缓存/生命周期、C1L整列低面数镂空全亮辉光。保留原液体、控制棒效果及服务端热工、燃耗、库存、保护和管理员规则。C1L视觉简化已由用户批准并手测认可；不实施C2或同步协议优化。
- 来源是未注册Git的独立候选副本，不能直接merge整个目录；核对相对基准的完整净差异后，由PM精确导入已验文件，禁止夹带诊断脚本、探针、运行目录或无关工业/模拟器改动。

## 分工与写集

PM负责文档、验收和全部Git写操作，不写功能代码或自动化程序。按AGENTS职责安排一个集成执行者及一次组合窄审，不向其转授PM权限。

1. 集成执行者第一阶段仅可写build/reports/reactor-performance-accepted-integration/：冻结当前dirty、来源及构建输入，记录正式包当前缺失；从Git读取旧正式包作备份；审计完整净文件/哈希与证据，给出精确导入清单。不改main源码、文档、存档、配置、.git或正式JAR，不构建。
2. PM审核清单后精确导入。若需代码冲突适配，另发精确写集给执行者；不得猜改已验语义。
3. 独立审查者只写docs/reviews/2026-10-15/reactor-performance-accepted-integration/REVIEW.md，审查规格/质量/来源与已有证据，不重复构建和游戏测试。
4. 来源提交后，由唯一集成执行者在报告目录完整导出准确Git树，先核输入，再运行一次assemble --no-daemon --console=plain。构建及校验工具仅放报告目录。核新JAR全部class/资源与输入输出、相对旧正式包的载荷差异，确认无测试/探针泄漏。PM最后替换并提交正式包。

第一阶段确认的导入写集为报告import-paths.txt内16项（7个生产Java、1个开发GameTest、2个模型资源、6个JUnit来源），以import-manifest.json哈希锁定。额外仅授权集成执行者修改build.gradle现有jar排除块，追加ReactorPortLookupGameTests*.class精确排除，包含内部类，不调整既有测试、任务、依赖或其他打包规则；这是标准assemble会泄漏新测试类的必要整改。候选源文件由PM按清单精确复制，执行者不修改这些已验实现。

## 保护与验证

- 所有既有dirty和用户世界/配置保留；正式JAR预先已缺失，保存该事实和Git旧包，不提交纯删除。无新分支/工作树、无世界搬运或回写，无推送/发布/来源清理。
- 实际基线Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；以实际Gradle核对。实际读取并应用minecraft-modding/testing/ci-release/resource-pack，入口C:/Users/IKSXH/.codex/skills/对应目录/SKILL.md。
- 依治理5.1复用同一实现的候选JUnit、真实服务端、性能与客户端证据，不重复性能采样或全量build。S3已有一个两版均失败的旧GameTest（2.25HU断言与现取整3HU冲突），保留原失败，不删断言、隐藏或顺手改玩法；C1L三项Sodium条件跳过不计通过。若净差异含未验证来源或实质冲突，再扩大对应验证。
- 用户反馈覆盖候选视觉/性能认可，不扩称所有硬件、Sodium/Iris或多人性能验收。性能数字引用候选同场景记录，不冒称本轮重测。
- 交付报告索引原始证据；提交中文；最终STATUS登记源码提交、正式JAR身份和边界，完成卡归档。
