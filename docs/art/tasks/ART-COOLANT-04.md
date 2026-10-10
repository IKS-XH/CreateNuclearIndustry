# ART-COOLANT-04：普通冷热冷却剂沿用内部流纹

日期：2026-10-10。用户直接要求“把冷热冷却剂流体的贴图和动画做成跟反应堆内部动画一样的”。本卡属于美术负责人专项资产权限，执行者为原 `reactor_ct_assets`；负责人只写美术计划/报告与审查，不实施资源或工具。工作目录固定 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，开工 HEAD `ca7c20dd5c81ed2b7b99bbf3824cf7d87f85d098`，现有未提交候选完整保留。

## 已确认设计

- 直接复用 `tools/art-assets/reactor-animation/sources/coolant_cold/frame_00.svg` 至 `frame_07.svg` 和 `coolant_hot/` 同名八帧，源稿只读。冷蓝、热橙四阶配色、每帧循环位移2像素、八帧首尾循环与内部材质同源。
- 普通 still 每帧16×16、整图16×128。flow 暂按锁定原生流面UV所需每帧32×32、整图32×256；每帧由原16×16图案精确2×2平铺组成，禁止拉伸插值或重画。实际本地流体审计若否定该规格，暂停该片段报告负责人。
- 四个 sprite 均添加原生 `.png.mcmeta`，明确宽/高、八帧索引0..7、每帧2tick及 `interpolate:true`，沿用内部素材的元数据播放规格。普通 atlas 循环不受某台反应堆功率/库存驱动；反应堆内部仍由原消费者按实际库存混色和转换量调速。
- PNG保留原SVG的全覆盖alpha255，与普通流体现有alpha一致。内部液体的顶点透明度/浸泡包络属于原Java消费者，本卡不修改，不把PNG切换当作全场景透明度合同。
- `ModFluids` 实际引用 `block/compound_coolant_{still,flow}` 和 `block/hot_compound_coolant_{still,flow}`。`fluid/` 四个同名旧副本当前逐字相同；同批保持这两组路径内容/元数据一致，注册ID与动态桶引用不变。

## 精确写集

1. 仅新增 `tools/art-assets/coolant-fluid-04/`：专用导出/验证工具、mapping及简短说明、SVG来源副本或只读引用清单、导出四PNG/四mcmeta、离线静态对照/循环预览。手写说明和非显然算法用中文注释；SVG为原创既有来源，PNG只能从SVG确定性导出。
2. 仅替换 `src/main/resources/assets/create_nuclear_industry/textures/block/` 与 `textures/fluid/` 两目录中的 `compound_coolant_still.png`、`compound_coolant_flow.png`、`hot_compound_coolant_still.png`、`hot_compound_coolant_flow.png`，并新增这八文件各自的 `.mcmeta`。不得改该目录其它文件。
3. 实施报告 `docs/art/reports/ART-COOLANT-04.md`；证据只在 `build/reports/art/ART-COOLANT-04/`。独立审查由原 `reactor_asset_review` 只写 `docs/art/reports/ART-COOLANT-04-REVIEW.md`。
4. 负责人另写 `docs/art/ART-COOLANT-04-CANDIDATE.md` 并追加 `docs/art/PLAN.md` 本批记录；执行者不得写这两文件或修改本卡。只读审计者的 PROBE 报告由其独立写，实施者不覆盖。

全部Java、测试、注册、温度/流体性质、容量/库存、配方、模型/桶模型、渲染层、shader、L1/L2和内部动画SVG/PNG/OBJ/生产工具均只读。禁止旧通用 `tools/art-assets/pipeline.py` 全量export/install（其恢复锁与本批新用户指定变更不一致），禁止重跑旧03/02证据或全量JUnit/GameTest，不启动游戏、不写存档、配置/依赖/语言/Git或其它治理文档，不再委派。

## 实施与最小验证

1. 实读 `AGENTS.md`、美术入口、治理1.2/5.1、本卡及 minecraft-modding/testing/resource-pack 技能；版本锁定 MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6。
2. 本批基线记录八旧PNG、相关素材16SVG/两内部sheet、现有Java/测试和其它正式资源摘要；保留旧R3冻结与唯一JAR，旧原PNG备份在本批证据。只在新目录生成候选/静态对照和循环预览，先交负责人实际看图；尚不安装正式资源或打包。
3. 窄资源检查覆盖16SVG与内部sheet同源逐帧RGB、尺寸/像素密度、flow四象限等于对应still帧、alpha255、0..7与7→0两像素周期、元数据正确切帧、双路径一致、实际ModFluids引用不变。相同工具支持check读取比对，不以重复生成充当验证。必要输入/路径/规格错误写前拒绝即可，不铺设无关负例矩阵。
4. 负责人实际复看后，由同一执行者统一安装八PNG/八mcmeta，唯一一次增量 `jar`（包含processResources）或 `assemble`；不触发test/GameTest。保存原命令、退出码和完整日志；唯一候选JAR另存本批candidate目录。
5. 绑定SVG来源→专用导出→正式安装→build/resources/main→JAR的十六资源条目和SHA；核其它源/正式资源及旧冻结候选未漂移，源/工具/报告/证据形成可读冻结清单。执行者停写，由原reviewer一次规格与质量合并窄审，复用已有日志，不重跑流水线。
6. 用户在美术树既有客户端/存档确认冷热流体在储罐/透明管道/动态桶和冷液世界面中的颜色、流纹、循环及资源重载。热液当前没有可放置方块，不能假报世界热液通过。自动/离线检查不替代实际游戏显示。

## 进度

- [x] 用户同源风格要求、实际资源引用/八副本及缺失mcmeta核实；已通知主PM资产范围，旧03R3/02R1及装配台人工门保持独立。
- [x] 锁定原生16/32 UV与alpha/metadata只读核查：[PROBE](../reports/ART-COOLANT-04-PROBE.md)已完整读取，负责人实际核原生旋转UV、SpriteContents只插值RGB及未知fluid默认solid来源。04固定alpha255，资产动画不需Java补充。
- [x] 新目录素材导出、负责人实际看预览：已看same-source-comparison.png，读专用producer/mapping；冷蓝/热橙与内部原帧一致，flow为精确2×2，GIF为八个100ms关键帧示意。方向通过，准许原执行者统一安装/唯一增量jar；游戏仍待验。
- [x] 唯一安装/增量打包、冻结及一次独立窄审：八PNG/八mcmeta、十六资源生成/安装/build/JAR一致，唯一jar退出0/4秒，未运行测试；29交付＋22证据＋24依赖冻结SHA`b48d3bf8968b0f92fe06ba6ed5b90d1df59dc834ce557b10482045db7dfa0580`。负责人实际读最终报告/原日志/资源绑定及完整[独立报告](../reports/ART-COOLANT-04-REVIEW.md)，规格/内部质量通过、必改无；报告SHA`61220e612b26fb83e6e65ef7e27ecbd95c5ddb5b6c3edad11284193ad870088e`、JAR SHA`7f9f2da39128203700edb677ca525bf11f1d50a0811194ef24f1d3330d983b68`。其余资源/源/旧R3保持，已集中[候选复看](../ART-COOLANT-04-CANDIDATE.md)，不重跑旧验证。
- [ ] 用户普通流体显示观察；未合main。
