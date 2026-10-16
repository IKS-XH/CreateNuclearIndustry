# THERMAL-SODIUM-01 锅炉与汽轮机渲染上下文窄修

2026-10-17，主逻辑PM。用户报告专用服中的锅炉、汽轮机没有连接外观，并指出与已验收的反应堆问题相似；本卡承接局部缺陷修复授权，不引入玩法取舍。当前进度只见[STATUS](../../STATUS.md)。

## 已核实依据与目标

main基准`0b29d59`，已提交生产来源`850904d`；Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。工程依赖不升级。

锅炉`BoilerSurfaceSnapshots.capture(context)`仍只接受真实ClientLevel或精确RenderChunkRegion，遇实际Sodium LevelSlice必返回空。汽轮机的`TurbineCasingAppearanceModel.realWorld`有相同类型限制，且被`TurbineWindowConnectedModel`共用，导致中大型外壳atlas与轴向连窗都回退原模型。反应堆已修同类入口，见[上次诊断](../../archive/2026-10-15/reviews/2026-10-13/reactor-sodium-surface-01/DIAGNOSIS.md)。

当前用户实例Sodium外层SHA256为`075FD258D40438240CD9FB5AD7ACAD14F341076D53C6E031716679FF9D8737DD`，与已核真实字节码相同，可复用其LevelSlice→NeoForgeModelAccess→getModelData调用链证据。仍须在实际真实类夹具中先复现再修复；不凭安装Iris归因，也不把该确定缺陷当现场唯一原因。

目标：Sodium真实区块上下文按既有显示规则消费锅炉快照和汽轮机局部状态，原生与未知包装的边界保持。反应堆已验修复不重构。

## 允许写集和不变量

唯一执行者，完成后一次规格＋质量组合窄审；审查不重复运行测试。执行者禁止Git、核心文档/活动卡、用户实例或世界写入，不再派Agent。

生产仅允许：

- `src/main/java/com/iksxh/create_nuclear_industry/boiler/client/BoilerSurfaceSnapshots.java`
- `src/main/java/com/iksxh/create_nuclear_industry/turbine/client/TurbineCasingAppearanceModel.java`

测试仅允许新增：

- `src/test/java/com/iksxh/create_nuclear_industry/boiler/client/BoilerSodiumSurfaceTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/turbine/client/TurbineSodiumAppearanceTest.java`

报告、只读提取的第三方夹具、临时测试参数脚本和候选身份只写`build/reports/thermal-sodium-20261017/`。正常Gradle输出可生成但不提交；不改build.gradle、依赖、版本、资源或协议。不修改服务器成型、热工、库存、流体、应力、运行同步、转子动画、Ponder及已验反应堆入口。

只识别上次已证实的确切final LevelSlice及其非static final ClientLevel字段，用ClassValue缓存只读访问，不加载可选模组硬依赖、不调用当前全局Minecraft世界、不泛搜任意包装器字段。失败安全回退。

锅炉仍把切片自身绑定世界与原子Publication.world按对象身份比较，旧世界不能借用当前快照；capture不读方块/BE/光照/区块。汽轮机没有全局快照，继续只用传入上下文的自身显示状态和正负各一格邻居；不要为套用锅炉规则引入全局世界或owner扫描。旧切片只能消费它自身局部状态，未知/Ponder仍回退。保留现有小型外壳和资源缺失回退。

## 执行和必要验证

1. 读取AGENTS、治理5.1、本卡、上次诊断与实际生产/已有相关测试。使用Minecraft modding/testing、systematic-debugging和TDD；资源边界按resource-pack核对，制品按ci-release。技能真实路径根为`C:/Users/IKSXH/.codex/skills/`，Superpowers为`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/`，报告记录实际应用。
2. 用已核哈希的真实Sodium类夹具，在未改生产入口前取得有效RED：锅炉capture/最终Create CT模型数据回退；汽轮机中大型atlas和连窗回退。不能用skip或无测试exit0当复现。
3. 在两处生产文件补相同严格上下文适配，保持上述不变量。真实夹具测试核同世界/旧世界锅炉隔离、原生level/region与未知包装；汽轮机核中/大型atlas、轴向连窗、局部状态和小型/未知回退。读到的必须是实际传入LevelSlice的数据。
4. 最终一次定向JUnit包含新增两类、已有BoilerConnectedTexturesTest、BoilerConnectedTextureBehaviourTest、BoilerSurfaceSnapshotTest、TurbineCasingAppearanceModelTest、TurbineWindowConnectedModelTest；已验反应堆Sodium测试可同次低成本复核，不另开测试批。临时init脚本只向Test转发夹具目录。无跳过的新Sodium用例才算通过。
5. 增量assemble一次并核制品变化范围：第三方/测试夹具不得打入生产包；生产payload差异只能对应两处类及必要内部类，服务端和资源保持。源码、制品字节数/SHA256、实际测试数/exit分别记录。不跑全build、clean、全部GameTest或真实服；本次不改变服务器路径，真实Sodium/Iris联机GPU外观保留集中人工门。
6. 一份REPORT.md给精确写集、RED/GREEN日志、源/JAR身份、载荷差异和观察步骤。审查后若整改仅复验实际差异；PM处理提交、候选和文档，不自动安装用户实例或替换main正式包。

人工观察：保留用户当前Sodium/Iris，使用候选客户端连接原服，观察锅炉成型壳/窗及中大型汽轮机壳/连窗、拆坏补全、重连与离开区块再返回；光影开关分别复看。服务器功能不变不等于客户端外观已验。

## 工作区和存档

隔离候选拟用`E:/MyMC/NewMod/Create_NuclearIndustry-thermal-sodium`，`codex/thermal-sodium-surface`，基准为本卡提交后的main。其他同级树分别保留独立内容/人工门，不混入本批。原生工作树接口不能指定项目要求的同级目录，因此按已定目录约定用Git工作树命令。

main世界已于2026-10-16 08:20:59保存全部维度，当前session.lock可独占只读打开；本次不停止用户正在联机的整合包客户端。新目标不存在时完整复制main的run/saves和配置，27文件/13337924 bytes按相对路径、长度和SHA256核对。目标若已存在须先完整备份再复制，禁止混合或链接共享；副本不进Git、不启动测试、不回写main。复制身份写报告目录，由PM准备后再派发。
