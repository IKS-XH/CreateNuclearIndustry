# EXT-B-BOILER-01D：按层开放三格端口

**状态：实现、18项锅炉GameTest、增量assemble与独立审查通过，待本轮人工验收。** 基线`a95d497`（01C每口独立限流），候选`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`、`codex/ore-acquisition`。本轮接续用户手测优化，不将01C新行为另记人工通过，也不再要求先测01C才实施本次明确要求。

## 已确认合同

- 固定5×5×5；给水口仍在从底部数第2层（y=1），汽口仍第4层（y=3），放开每侧这一排的三个非棱边位置。控制器仍占第2层某一侧中央，因此控制器面2给水口、其余三面各3个，整炉1～11给水口和1～12汽口。各类仍至少1；端口法线朝外，不占棱边、角点、顶底面或其他层。
- 唯一控制器的位置、唯一顶中央安全阀、底面中央1～9热段及其他结构条件不变。每口独立256mB/t、水汽各16000mB共享、实际热量及产汽规则沿用01C。库存不足/接收空间不足时不承诺各口每tick满额。
- 原单中央口和01C存档兼容，扩展位置不改方块ID、配方、材质、模型或保存账本；合法增删口须使缓存能力/管面正确失效和恢复。
- 用户本轮同时询问压力与普通蒸汽：已由PM核对并解释首期压力为蒸汽占用率、安全阀90%/80%；核热暖炉后直接出超临界蒸汽，未实现温压品质转换、普通热源产普通汽及汽轮机排汽。该问题不作为本卡增设压力/蒸汽机制的授权。

## 实现注意与写集

- 执行者先读AGENTS、治理§5.1、本卡及`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`。核对实际gradle.properties：MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82，不升级。
- 允许写`src/main/java/com/iksxh/create_nuclear_industry/boiler/*.java`、`src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionBoilerGameTests.java`、必要锅炉中英说明键；如确需单测允许`src/test/java/com/iksxh/create_nuclear_industry/boiler/*.java`。不改通用兼容层/Mixin/构建/资源模型，不改预算与热量算法。报告仅`build/reports/extension/EXT-B-BOILER-01D.md`及同前缀证据目录。禁止Git写、核心docs编辑、转派。保留用户日志、缓存、配置、世界。
- 同步覆盖inspect、issue、缓存特殊格复核、端口反向查找owner、所有合法候选格capability失效以及相邻Create管面恢复。注意原`x<0 ? WEST : ...`只适用中央口，南北侧非中央格不能按x正负误判法线；先按落在外表面的轴确定法线。
- controller仍是原锚点，不能为放开侧排而移动控制器或引入新结构所有者。owner从非中央端口反查必须考虑沿面±1偏移且不强制加载区块；能力仍以实际form归属和epoch校验，不只凭候选位置开放。
- 新增/修改手写注释为中文，说明非显然坐标变换和生命周期条件。尽量共用少量位置辅助方法，让扫描、诊断、能力与管面枚举一致；不重构无关算法。

## 精简验证与交付

- 一名实现者持有Gradle运行权：一次最终锅炉namespace GameTest和增量assemble；账本算法未变则复用01C的12JUnit，不重跑换热器/P1全量。无必要不新增测试框架或模板。
- 用少量代表场景覆盖全部11水+12汽合法且四法线正确、非中央口同tick独立额度、错层/棱边/错误朝向拒绝；真实Create管对南北侧偏移口（能捕获旧法线算法问题）进出、成型后加口、拆接恢复。可扩展已有代表场景，测试量不按每个端口复制。
- 新测试的库存、基线和句柄在对应阶段重新记录；不要照搬01C早期不成立的PipeConnection null断言。停输允许连接对象消失或残存零压，并验证能力失效；恢复要比较恢复前基线，必要补料明确计入守恒。不得通过删断言掩盖失败。
- 实现后由另一执行者做一次独立规格/质量审查，复用日志。PM更新文档、归档并交用户仅复测同面三口及非中央管线；01C/01D手测合并，不合main、不推进汽轮机。

## 交付与人工门

执行者`/root/boiler01d_impl`、审查者`/root/boiler01d_review`均使用高速模型。功能写集为锅炉结构/控制器与GameTest三份文件；18项最终定向GameTest、增量assemble及补强后的独立审查通过，复用01C的12项账本JUnit。原始证据、执行过程和仅两项人工操作见[候选验收说明](../../reviews/2026-10-04/boiler-01d/README.md)。未记录用户人工通过，不合main。
