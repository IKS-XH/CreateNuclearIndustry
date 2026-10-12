# 汽轮机美术与专用管网联合验收合入

2026-10-12，用户直接确认“汽轮机美术重置和管道的外观整改都测试通过了，合并到主分支吧，合并后build一下”。本次关闭汽轮机原07B-R2机壳/观察窗和10B/R1模型及运行动画门、管网01R2四项外观门；此前管网功能验收保持。没有补记用户未报告的现场数值或多人结果。

## 净合范围

生产与生成源净合main提交 `151cebac2b0ff718b5d3da3d292835541aeff621`，共949路径。来源为已审fluid-network `9014f15` 的235路径与art-studio已审未提交候选714路径（7生产Java、4专属测试、242正式资源、461模型/SVG生成工具）。精确清单和逐项当前SHA见主目录 `build/reports/turbine-fluid-accepted-integration/flow-inputs.json`、`art-inputs.json`。

管网234/235来源与main逐字一致，唯一共享TurbineController三方自动并存：原运行显示前置完整保留，原port新增受限流体包装；无冲突、无手写语义修补。美术714/714逐字一致，与管网路径交集0；运行显示其余10来源仍保持原冻结，原注册/几何公共入口原已在main、art、flow一致。原工具三个EOF空行提示是已审来源原样保留，不为消除提示重新生成素材。

模型/素材/动画及管网业务复用各原独立审查和自动/真实证据，详见[消费登记](../turbine-runtime-display-01/CONSUMER-HANDOFF.md)、[01R2候选](../tiered-fluid-visual-01r2/CANDIDATE.md)与[本轮执行报告](./IMPLEMENTATION.md)。本轮实际新增验证是用户要求的主目录标准build，不把历史检查写作本次重新运行。

主目录原203e9558…JAR完整备份在 `build/reports/turbine-fluid-accepted-integration/main-before-203e9558.jar`；两份来源候选、失败日志与原冻结制品原位保留。首次PowerShell重编码补丁检查失败改由Git原生输出后核验及应用成功，仅是补丁工具过程，不是代码/测试失败。

## 主目录构建

唯一执行者运行 `./gradlew.bat build`，增量默认，不clean、不强制rerun、没有先重复运行定向组。首轮wrapper缓存锁环境失败在编译/测试前，原日志/退出保留；随后同一命令以正常授权权限运行，实际57秒exit0，106份XML共605项、失败/错误/跳过均0。生产/测试编译、资源处理、jar和test实际执行；主PM再次读取原退出、日志、当前XML总计与JAR身份，未重复构建。

最终主目录 `build/libs/create_nuclear_industry-0.1.0.jar` 为3,039,185字节，SHA256：

`ecbe0e38f0620db6764fb56fb63b2a5b17d56bb93059db4be4ff7849e1e65ce3`

原 `build.log`、`build.exit.json`、XML与完整冻结副本保持；`final-package-identity.json` 实际verified/errors为空，949来源、400资源source/build/JAR、98受影响生产class编译/JAR及前置其余10源码一致。本轮未手写任何功能/测试修补。JUnit正常更新两份受跟踪日志，其他106个pre-build dirty文件SHA保持；这些日志仍不纳入本次Git提交。

## 保持边界

既有用户未提交文档、日志、模拟器与其他美术草稿不纳入本提交；本批没有建新分支、搬动/回写存档、改实例配置、替换候选制品或清理工作树。反应堆其他美术及独立锅炉16³候选未借此合入；管理员、新管网压力和旧存档兼容研究不推进。
