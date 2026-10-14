# ART-REACTOR-03R3 独立组合窄审

审查日期：2026-10-10。审查对象为冻结的本批液体显示与共享滑块候选；执行者只读源、证据和图片，仅写本报告。没有运行测试、构建、导出、生成预览、客户端或服务，没有 Git 写操作、主树写入或子代理。

## 结论

- 规格符合：通过。当前实现遵守生效任务卡及主PM共享补充，限定显示修正，不改变库存、容量、服务端状态、材质来源或鼠标协议。
- 内部质量：通过。没有发现需要整改的 Critical、Important 或 Minor 问题；必改项：无。
- 用户客户端视觉与操作门：未通过/待复看。上述通过只针对冻结内部候选，不追认03、03R1、03R2或02R1视觉验收，不代表已合main。

## 合同与技能

实际读取本树最新 AGENTS.md、治理1.2/5.1/5.2、美术入口、ART-REACTOR-03R3任务卡、ART-REACTOR-03R3-COORDINATION.md和实施报告。以用户当前执行者权限、精简验证及首发前存档边界优先，不扩展旧矩阵或旧存档研究。

复用此前已实际读取的技能，并本批应用其规则：

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：审查客户端消费者与共享Behaviour边界、真实生产调用及锁定依赖。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：审查真实mesh/VertexConsumer、原生transform/PoseStack/testHit回归与原始XML，不以源码镜像当行为测试。
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：区分资源alpha和顶点alpha，核既有SVG/模型/纹理保持及来源安装打包证据。
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/requesting-code-review/SKILL.md` 与 `verification-before-completion/SKILL.md`：规格和质量合并独立审读，依据原始结果及冻结绑定得出结论。任务明确禁止重复流水线，未因通用技能重跑或另派代理。

MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6保持。

## 实际生产审读

完整审读本批两液体生产类、共享Behaviour精确差异及两专属测试的新增行为，并对照原始审计、实际锁定Create ValueBox/TextValueBox后置变换字节码。

液体先对descriptor.coolantSpace的canonical空气集合执行原allocate，再从已分配层高扩展FuelColumn显示格。燃料格未并入容量或重新分配库存，缺少合法同层液位时不虚构液体。扩展集合统一调用原并集裁面，空/控制/燃料相邻内部隔面撤下，fill为0全部撤销。缓存保存同层合法采光坐标，等距按确定顺序选取；生产submitFace每次取当前世界光，不缓存packed light。缓存键/撤销仍遵循owner身份和既有生命周期，材质、冷热连续混合、帧活跃度和暂停处理未改。

顶点透明度侧面.46..56、水平面.45；沿外法向反向内缩1/1024，切向端点和UV保持。绕序与外法线一致，相邻同平面边点一致；原不透明模型深度遮挡钢板和管体。此为批准的显示包络方案，未另造库存或管间实体几何。

共享Behaviour仅六面法向移到真实0/1面及说明改变，原切向、.18/.40比例、旋转、构造null安全、fromSide同步缓存和原生命中路径保持。框和文字不是只核transform原点：测试在原生transform后按锁定ValueBox框绘制、TextValueBox文字基线变换验证。顶底框四角面外约.01130625、文字基线.0140625；侧面约.025125/.03125。顶面角落显示及原生hit范围避开端箍，未新增绘制补偿或自写点击公式。生产手写说明使用中文且与行为一致。

## 原始证据与冻结绑定

证据根为 `build/reports/art/ART-REACTOR-03R3/`。实际读 final-test-jar.log、exit.txt、final两份XML，以及液体red/green、滑块top-red/green相关证据。最终15+8=23项，failure/error/skip均0，唯一最终test+jar18秒exit0。液体明确AssertionFailedError red覆盖包络/液位/alpha，旧R2顶面两项真实AssertionFailedError red保存；首轮环境/入口失败没有当作业务red。

完整TextValueBox构造遇到Minecraft单例缺失的NPE有原始输出。后置链检查只证明锁定字节码下的框角和文字基线深度，不能宣称完成真实Font绘制。实际核对相关字节码的outline scale/translate与font scale/translate，测试不是自行重写旋转或命中。

本次进行一次只读绑定核验，未复跑既有验证矩阵：

- frozen-manifest.json SHA256为 `4de2f5595eb8ec8fc6bfbbc4c850504f3d297759bf4f4b539eae5e4ebdbdfc61`；6交付、77证据、12只读依赖共95项逐一SHA匹配，无漂移。
- 三份当前生产源、其17个编译类与候选JAR条目逐一匹配source-class-jar.json。预览生产输入绑定亦在冻结证据内。
- 候选JAR为2477690字节，SHA256 `3a52c6cb61ed422ce94faa92a959ca0d272d7710388f8de099d1d02ca3bcce04`；build/libs同哈希。
- 阅读verify-final.py、verification.json、source-installed-jar.json及日志，复用111保护源、19来源/安装/JAR映射、1056资源保持及旧候选/冻结保持结果，没有另扫legacy或重做资源矩阵。共享非几何精确保持、HEAD仅PM文档同步和自动日志单列有证据；不清理日志。

## 实际看图与人工边界

实际打开 coolant-full-comparison.png、coolant-partial-comparison.png、slider-final-depth.png。

满液新图在冷/半混/热三种颜色下连续包围燃料管，原中间窄液膜与燃料区空气割裂得到改善；钢板仍保持灰色和遮挡关系。跨层部分液位图保留上部露出和下部浸色界线，没有把未湿层伪装成满液。滑块深度图旧顶面框/文字在实体内，新六面均出面，顶面角落方向符合合同。三图足以支持内部方向通过。

液体预览来自实际新旧mesh/submitFace顶点及原mixInto材质输入，但仍是离线投影，不验证游戏透窗透明排序、夜间实际光照或GPU深度效果。滑块图不是真实Font字形或鼠标体验。用户仍须在既有美术树客户端复看透窗液体、部分液位、实时库存变化、顶面文字/遮挡及鼠标调节；02R1外观门独立保持，不自动推进其他设备。
