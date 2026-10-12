# 汽轮机美术与专用管网验收合入

2026-10-12，用户明确“汽轮机美术重置和管道的外观整改都测试通过了，合并到主分支吧，合并后build一下”。按此关闭原10B运行动画/机壳及01R2四项外观门，原管网功能验收保持；不补写用户未报告的具体测量或多人结果。

## 范围与输入

- 目标是当前主目录main `67e6aef`，保留用户已有未提交文档、日志、模拟器及所有世界；本批不建新分支，不复制或回写存档。
- 管网从fluid-network树 `9014f15` 的已审功能 `a5d1fe6`、视觉 `609914f`、01R2 `b5310f3` 接入；精确来源限这些候选的src与工具路径。保留main已接入的汽轮机运行显示API，不能以候选旧Controller覆盖它。
- 美术取art-studio树原07B-R2机壳/观察窗与10B/R1模型、素材、客户端动画及其生成工具、专属检查；依据原任务卡、冻结清单和现行候选身份确定净写集，不取其他反应堆美术、概念草稿或未验收任务。
- 原16³锅炉候选仍独立待测，不能借本次合入；管网数字、设备热工/汽耗与美术只读API保持已批准合同。

## 执行职责

PM核精确来源/预像并管理Git和文档，不手写功能或测试代码。唯一执行者核本批来源清单、报告实际共享冲突；仅在PM给出的精确路径解决语义冲突或本批标准build失败，不自行扩范围、Git、改治理或另派任务。

唯一执行者持有合入后的标准 `./gradlew.bat build`，增量默认、不clean/不强制rerun、不先重复定向组。结果保存至 `build/reports/turbine-fluid-accepted-integration/`；保存原命令、退出、XML统计及最终JAR。只有实际失败或语义冲突才增加受影响窄验证/独立审查，既有无冲突候选证据直接复用。

实际技能入口：minecraft-modding、minecraft-testing，Git/构建收尾参考finishing-a-development-branch和verification-before-completion；版本锁定1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，技能新版本示例不升级项目。

## 必要步骤

1. 冻结当前候选身份和主目录未提交状态；核管网净diff与美术当前冻结来源，确定客户端注册/partial/共享API并存点。
2. PM净接入已审输入。代码冲突由唯一执行者处理，只允许保留双方已批准语义，不能删除测试以取得通过。
3. 唯一执行者在主目录运行用户要求的一轮build；失败按实际根因窄修后必要复验，保留失败历史。
4. PM核源码/资源与输入对应、最终包及build退出，登记验收与合入提交。原候选/制品备份保持，不发布、不推送或清理其他工作树。

报告允许路径：`docs/reviews/2026-10-12/turbine-fluid-accepted-integration/IMPLEMENTATION.md`、`build/reports/turbine-fluid-accepted-integration/**`；PM另写ACCEPTANCE和治理状态。来源核验阶段仅只读，不提前改功能代码或构建。

## 完成记录

949来源净合main `151ceba`，没有代码冲突或新手写功能；标准build实际57秒exit0，605/605零失败/错误/跳过，来源/资源/编译类与最终ecbe0e38…JAR一致。首次补丁文本重编码及wrapper缓存锁失败分别保留，不当作业务测试RED；原候选和用户世界/其他dirty保持。详见[联合验收](../../reviews/2026-10-12/turbine-fluid-accepted-integration/ACCEPTANCE.md)和[实际执行报告](../../reviews/2026-10-12/turbine-fluid-accepted-integration/IMPLEMENTATION.md)。本卡完成，不推进其他主线或重开已通过人工门。
