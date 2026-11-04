# 锅炉事故与反应堆外爆并集组合窄审

2026-11-04；唯一只读审查者，非PM。**技术审查PASS：未发现阻止本次已授权集成的规格或质量问题。** 本报告不代行用户验收、正式包提升或合main。

审查对象为 `codex/boiler-explosion-accepted-20261104` 的稳定生产并集 `16e14ae10735f7fb390e2f834b9b67e987586f45`。已读AGENTS、治理5.1/5.2/6、STATUS、本批活动卡及两原候选/整改/审查报告。用户本轮明确两个候选手测通过并授权合main，按活动卡采用该最新结论；历史报告中的待手测保留原时点语境，不据此重开已关闭的集中人工门。

实际技能入口为 `C:/Users/IKSXH/.codex/skills/` 下 `minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-ci-release/SKILL.md`、`minecraft-resource-pack/SKILL.md`，分别用于最终生命周期/服务端边界、分层证据及复用、源与制品身份、资源/测试域打包边界。实际Gradle仍锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，未升级依赖、版本或许可。

## 源码与组合边界

- 独立用只读Git比较：相对main起点 `8b6d19e5` 的全部src变更精确等于锅炉 `7d2a28eb` 的25项非build文件和外爆 `96335648` 的10项非build文件，并集35项无多余、无遗漏；每项最终提交blob均与对应原源blob完全一致。工作树的原字节差异由执行者逐项证明仅CRLF/LF，未混称原字节相同。审查末次核对src与构建配置相对稳定提交无内容差异，`git diff --check` 无错误。
- 唯一功能冲突为 `build.gradle` 排除块。最终相对外爆原源只增加原锅炉两行，外爆类/域和锅炉类/域四条规则并存，未改变其他构建逻辑。
- 最终 `CommonHooks.fireBlockBreak` 的post返回边界先调用反应堆原最终事务；仅仍未取消时调用锅炉最终检查。锅炉再次核服务端、取消态、原位置/状态及危险工况，保持最终许可后、原块删除前的事故提交。没有两个同类Mixin重复注入或绕过最终取消。
- 外爆仍在原生 `finalizeExplosion` 最终列表shuffle后、首块loot/删除前接管，仅DESTROY/DESTROY_WITH_DECAY；真实缓存成员和所有者去重，沿原危险评估与唯一托管事务。失败清空同一toBlow列表，继续原生空循环/profiler收尾，避免客户端列表与服务端回滚分离；KEEP/TRIGGER及冷空堆保持原边界。已对照原候选保留的锁定Explosion源码和失败整改证据。
- 锅炉执行器共享 `ReactorAccidentSavedData` 同维度预算，并取反应堆活跃任务冻结上限；原逐tick预算只能收紧，不被公共移除入口的64硬上限放大。环境移除沿原最终许可、连带容器、对象复核、两格读取域预检与逐写守卫，DEFER保游标，终结拒绝才领取。环境探针直接post且带PROBE身份，不走CommonHooks注入，也不误触玩家手拆事务。该共享文件属于原锅炉已验来源，外爆源未另改它，未产生新冲突或新的预算通道。

## 证据与制品

读取原始XML及关键真实服日志复用原结果，审查者未运行测试或构建，未打开用户世界：锅炉五类52/52、19注册场景核心/正常保存，末次无强载delta为1实跑/18显式SKIP；外爆三类17/17、7/7真实核心/正常保存，失败收尾delta为1实跑/6显式SKIP。SKIP不计业务通过。锅炉R4完整目的地成型交易的隔离Nether `SUBCASE_UNAVAILABLE` 继续是历史自动证据限制；不把用户整体手测扩写成逐项自动实证，未明确观察的真人多人/物理卸载不补记。

原全JUnit865=851通过/3既有失败/11跳过继续保留，两条HeatExchangerBoilerBridgeTest及一条ReactorFissionDifferentialTest未修、未重跑，不称全量build全绿。

已核 [INTEGRATION.md](INTEGRATION.md) 最终段、[assemble-command.json](assemble-command.json)、assemble.log及制品核验脚本/摘要：唯一执行者从已提交16e14ae1运行一次增量 `assemble --console=plain`，exit0/约14秒；2262项构建前后源0漂移，无机械重跑JUnit/GameTest。

最终Jar为 **3714927 bytes，SHA256 `C3A2C178CEDEBD05436DB3E1F3244CF361752AF7B8945C2A1AF154C82057D5C3`**。执行者2601载荷全部绑定实际输出/许可来源，822class、1775资源及4项manifest/许可说明，0重复、33项按规则排除、未排除输出0遗漏。审查者另只读打开实包并核SHA、载荷数、两测试类及两域完全排除、两个最终入口和两生命周期生产类存在，与报告相符。

相对保留的外爆原包仅31项锅炉来源载荷增改、0删除、0模型变化；原49模型CRLF/LF差异未扩大。两原候选身份保持，锅炉日期包未写，未搬动世界或夹带主目录工具dirty。最终正式包复制、文档状态、提交及main集成仍由PM按用户授权完成。本次仅写本报告，无其他写操作。
