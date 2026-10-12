# 汽轮机美术与管网验收集成执行报告

## 只读来源核验

执行者实际读取主目录AGENTS、活动集成卡以及minecraft-modding/testing。锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，未按技能新版本示例升级。

当前美术HEAD为8a33893，实际动画和素材为原未提交已审候选。`build/reports/turbine-fluid-accepted-integration/art-inputs.json` 保存714个精确路径、当前SHA、来源证据、主目录和管网预像：7生产源、4专属测试、242正式资源、461工具文件。源码11/11符合07B-R2 freeze与动画04冻结；242资源逐项符合R2开工保护、07B-R2新增PNG或10B最终制品冻结，并与当前9be1d310…美术JAR逐项一致，核验无错误。原07B/R1工具403项逐项符合R2 baseline；新R2/10B工具按原报告授权范围纳入当前生成源、实际候选和预览，能对应原映射/检查的SVG与候选逐项核原SHA，其余工具记录当前身份，不冒称存在未提供的历史逐文件冻结。

机壳资源union包括07B的212个OBJ及专用MTL/两PNG，最终端盖采用R1对164个OBJ与端盖PNG的覆写身份；R2追加稀疏PNG。R2还必须新增Appearance事件、机壳包装与连窗包装三个客户端类及两份专属检查，不能只拷26份10B模型资源。动画输入为最终六源，其中TurbineClientEvents提供partial预初始化及Output/Rotor renderer注册，其余四源与两测试按冻结清单取值。

TurbineContent、TurbinePartBlock、TurbineGeometry在main/art/flow三树字节一致，无需复制；原11路径运行显示前置在main与art逐项一致，保留main已交runtimeDisplay，不从缺此前置的flow整体覆盖。美术714净路径与PM的235管网输入无同路径交集。原TurbineRuntimeClientEvents与新TurbineClientEvents/TurbineAppearanceClientEvents分别承担可靠样本租约、运动消费、外观包装，必须并存。flow的Controller改动与main运行API为PM三方净接入点。

本轮仅写此报告及来源核验数据/核验脚本；未编辑生产/测试、未Git写、未Gradle、未启动游戏，世界及其他任务dirty保持。反应堆08/09、概念目录、备份、缓存及16³锅炉候选排除。用户人工通过事实由PM登记，本报告只记录来源和自动核验。

## 合入后构建

PM完成两批净接入后，本执行者逐项读回：美术714/714 raw SHA一致，管网234/235与原冻结一致，唯一TurbineControllerBlockEntity相对原main67e6aef只有port返回增加RestrictedFluidHandler.wrap的一行；原runtimeGeneration/runtimeEpoch、运行发布/撤销生命周期逐字保留。其余运行前置10/10保持。详见`build/reports/turbine-fluid-accepted-integration/integrated-inputs-before-build.json`。

该核验同时保存当前.gitignore的用户版本SHA `d47f1ff28092a57d65357edc744f80f81ed75094994cb6520b329f3d4a7e17c3`及实际dirty/staged快照。尚未获得生产修改权限，未手写冲突修复。

PM净合提交为`151cebac2b0ff718b5d3da3d292835541aeff621`，949精确来源。唯一执行者执行原命令`.\gradlew.bat build`，不clean、不rerun、不先跑定向组。

首次沙箱启动时Java的user.home异常为C根，wrapper无法创建`C:\.gradle\wrapper\dists\gradle-9.0.0-bin\...zip.lck`，退出1，未编译或测试；这是环境启动失败，不计测试RED。原日志和退出完整保存在`01-wrapper-environment-failure.*`。随后按PM授权以require_escalated运行相同命令，正常主用户环境直接使用既有缓存，无需换GradleHome、下载另套工具或修改wrapper/项目配置。

实际标准build为57秒exit0，9任务中5执行、4复用；生产/测试编译、资源处理、jar及完整test均真实执行。本轮106份XML共605项，605/605，failure/error/skipped均0，原XML副本及SHA在本reportroot的`xml/`，原构建日志和exit在`build.log`、`build.exit.json`。原30项API弃用/unchecked提示与Gradle兼容提示保留，未为消除提示扩写构建。

最终JAR为3,039,185字节，SHA256 `ecbe0e38f0620db6764fb56fb63b2a5b17d56bb93059db4be4ff7849e1e65ce3`；当前`build/libs/create_nuclear_industry-0.1.0.jar`与本reportroot的`frozen-jar/`完整副本一致，原203e9558…主JAR完整备份在`previous-jar/`。实际核验脚本与`final-package-identity.json`保存949/949最终来源、400/400资源source→build→JAR、98/98受影响生产class编译→JAR及原前置10/10源码一致，errors为空；共享Controller以已核并存预像校验，不错误要求它等于单方原候选。

pre-build保存的108个dirty文件中仅JUnit自动更新`logs/debug.log`与`logs/latest.log`；其他106项逐文件SHA保持，包含用户.gitignore、项目文档和两模拟器。执行者只新增/更新本报告和本批核验数据，没有修改生产/测试、Git或世界，没有启动客户端或GameTest。既有人工验收由PM登记，自动build不扩成新的GPU/现场观察。实际应用verification-before-completion完成本轮命令/XML/制品核验；finishing-a-development-branch按已明确main净合决定和执行者Git禁令，不另行询问集成菜单或清理工作树。
