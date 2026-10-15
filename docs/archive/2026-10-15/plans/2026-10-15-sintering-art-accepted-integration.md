# ART-SINTERING-16B-INTEGRATION：已验烧结炉外观接入

2026-10-15，用户直接确认“烧结炉的新外观也ok了，合进来吧”。接入已选A锻钢箱炉，不接新玩法/炉门/加工动画。本卡保留当时执行边界，交付见[集成记录](../../../reviews/2026-10-15/sintering-art-accepted-integration/INTEGRATION.md)，现状和最终包见[STATUS](../../../STATUS.md)。

## 来源及精确范围

- main基准00b1e2385bfa4793a6600d128839f7c5b1129dd6，保留5ea9c50反应堆性能优化和全部后续主线。
- 美术来源E:/MyMC/NewMod/Create_NuclearIndustry-art-studio / codex/art-studio，基准794d16a0dee67bb4ec611355535ea408eaac5374，ART-SINTERING-16B待提交来源。候选包8e6321824d7f1e535107d58aeebcd40761975b382105a42957e38c37fe29aabd，2855180 bytes，仅作12资源身份和用户手测对象，不覆盖main旧逻辑。
- main生产导入仅models/block/fuel_sintering_furnace_{off,on,item}.json及textures/block/fuel_sintering_furnace_{steel,edge,refractory,dark,window_off,window_on,sensor,port,heat}.png共12件。原blockstate/item wrapper、全部Java/配方/Gradle/其他资源保持。
- 原16A/16B概念、可编辑来源、工具和交付文档由PM在art分支精确提交留存；不把旧美术README/PLAN全量覆盖main。main用本批验收/集成文档索引原来源。

## 分工及验证

PM负责文档、Git及精确行政复制；一个集成执行者只写build/reports/sintering-art-accepted-integration/，不改生产/核心文档/.git/正式JAR/运行目录/候选。不派发其他执行者。

1. 先读AGENTS、治理5.1、STATUS和原16B卡/IMPLEMENTATION/REVIEW，实际使用C:/Users/IKSXH/.codex/skills/下minecraft-resource-pack/modding/testing/ci-release。核锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。
2. 冻结12资源清单及SHA，绑定原installed-identity、src/processResources/候选JAR；只核本次冲突/非目标保护，复用原像素/模型/一次组合窄审及已确认游戏反馈，不重跑JUnit/GameTest/网页/游戏。main正式JAR开工再次预先缺失，从Git取8405包备份并登记，保留无关dirty、日志、用户世界/配置，不为纯资源集成重扫全存档或复制世界。
3. 交PM可精确提交的art相关来源清单及main12资源导入清单；PM审核提交后通知构建。若发现非资源改动/语义冲突或新问题先报告，不擅自适配。
4. 来源提交后，唯一执行者在报告目录导出准确Git生产/构建输入并核验，执行一次assemble --no-daemon --console=plain，不clean/全build/重复构建。核新包12资源与候选同源，所有class和非目标载荷与main原8405保持，不泄漏开发工具/报告；冻结新包由PM最终替换及Git跟踪。

构建前输入门实际发现三模型原候选为CRLF、Git提交对象规范化为LF；9PNG不存在此差异。PM接受仅这三份JSON的纯换行表示差异：导出和构建保持准确Git LF字节，逐项保存原/新SHA并证明仅CRLF→LF、JSON内容一致；最终三模型对候选按该精确归一化绑定，9PNG逐字一致。不得恢复CRLF覆盖导出、改几何/像素或放宽其他载荷比较。输入门先中止时assemble尚未启动，不计重复构建，不为表示差异重复原游戏验收。

不新建分支/树，不搬世界或回写候选，无推送/发布/清理来源。原16B已有一次组合审查，本次无冲突纯资源集成按治理5.1复用，只补差异/打包一致性；无需为合入再启动一轮相同资源审查。装配台15C与其他未闭门不因本次反馈扩大验收。完成卡归档，来源/JAR与必要记录交付后停止。
