# ART-REACTOR-03 独立规格＋质量窄审

2026-10-10，独立执行者。仅写本报告，不修改源码/素材/冻结证据/核心文档或Git；未派代理、运行Gradle、客户端或服务。

## 结论

- **规格符合：通过。** 已消费真实L2运行投影，满足实际库存连续混色、完整棒体整体升降且上方露出，以及九管燃料架和真实HU/t蓝辉合同。
- **内部质量：通过。** 未发现 Critical / Important / Minor 必改问题，**必改项：无**。
- **客户端人工门：未通过、待观察。** 本报告不确认真实透窗排序、昼夜材质观感、首次客户端播放和暂停/恢复体验。03与02R1视觉门各自保留，不推进其他设备，不表示可变尺寸反应堆玩法已实现。

## 审查依据及实际用途

实际读本树AGENTS、治理1.2/5.1/5.2、美术入口、03 DESIGN、03A/03A1/生效03B卡及三份实施报告、L2最新HANDOFF/R2独立窄审与绑定记录。逐一审读六客户端类、两专属测试、最终核验脚本、原始red/green/final日志与XML、资源映射及冻结记录。HEAD为`36ea83355fab672cf1fdc7010b3e448e6396eaa6`，共享R2源码同步为`2bece5b`。锁定MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6。

复用此前实际读取并应用的技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`核对客户端Dist、事件时序、只读快照和原生BER/PartialModel接入；同根`minecraft-testing/SKILL.md`核对真实行为断言、数值核心与GPU替身边界；同根`minecraft-resource-pack/SKILL.md`核对OBJ/UV、SVG来源、动画sheet、静态物品parent及安装资源路径。`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/requesting-code-review/SKILL.md`与`verification-before-completion/SKILL.md`用于组合审查和证据门，服从治理精简/权限合同，不机械重复验证或Git操作。

## 规格与生产控制流

1. 三partial在物理客户端RegisterGeometryLoaders同步幂等初始化；两BER只注册既有BE，reload及独立游戏事件不侵入L1/L2。每次BER绘制仅capture一次，从同快照取得owner/实际列；本地成员ID用FULL、forcing=false查询，不加载区块、不扫描补投影。异世界BER不写当前世界数值缓存，未知/成员矛盾撤下对应动态显示。
2. 仅可靠可用燃料且H>0绘制管身蓝辉；alpha实际为`0.12+0.53×H/(H+6)`，零/未知/耗尽熄辉。管身FULL_BRIGHT且disableDiffuse，钢板静态；逐管偏置已烘焙，无二次XZ放大。
3. 控制棒生产rodPose直接消费actualDepth，target不参与；卡死、首见、身份改变、未知恢复和失见超过4tick直接定位，其余最多4显示tick插值。杆身仅按真实body长度Y缩放一次；固定head随全长棒体平移。AABB覆盖最低棒底及全部拔出顶端/端部，不裁掉上方露出段。
4. 液体只用descriptor.coolantSpace，填充由溢出安全的cold+hot/capacity决定，按真实Y层面积分配；燃料列不填。合法格并集剔除共享内部面，不同侧向高度保留暴露条带，UV及位置先用小局部坐标，六面外向绕序正确；不叠冷热双盒或固定分层。
5. 两套8帧先时间插值再按hot/total混色，ABGR四通道与显式顶点RGBA区分。每堆稳定槽及自身256像素缓冲，提交该堆顶点前更新；256硬上限、draw不驱逐别堆，同帧不盗用槽。安全tick边界回收失效/20显示tick失见身份；会话/重载先endBatch后release，解码InputStream/NativeImage关闭，坏sheet撤旧液体。
6. 消费时钟冻结真正暂停的tick及partial，身份/网格/位姿缓存不持有BE/NBT/强Level，不含每心跳sample。L2 R2租约冻结、持续失效核查及公开ABI沿用已审共享合同，不做第二套租约；中文说明与实际单位、权威/显示边界一致。

## 实际图像与证据

实际打开03A的`models-preview.png`、`glow-preview.png`、`control-preview.png`、`coolant-preview.png`及03A1的`item-gui-preview.png`。九管八边几何与三格接头可辨，蓝辉保留钢材/板层次，控制棒固定长度整体升降，冷/热比例连续且零库存无液体，静态GUI继承`minecraft:block/block`保留锁定原版变换。以上是保存OBJ/UV/SVG的离线投影，未当作真实客户端运行证据。03A/03A1几何、导出、负例和历史93项/两项授权漂移证据复用，未重生成图或重跑旧矩阵。

最终证据入口`build/reports/art/ART-REACTOR-03B/`：

- `consumer-red.log`及red/两XML为12项真实AssertionFailedError（5+7），errors均0；占位核心保证编译，未把基础设施失败称为red。consumer-green为12/12。最终`final-test-jar.log/.exit.txt`为19s增量成功、exit0；final/两XML为**16/16（Materials7＋VisualState9），failure/error/skipped全0**。
- 测试调用生产glow/fill/allocate/faces/rodPose/Rods/Clock及Pool.draw的writer入口，真实ControlRodColumn证明target≠actual不移动棒并覆盖未知恢复；网格复用、ABGR端点/帧混色、缓冲复用、稳定256槽、会话隔离及安全释放均有行为断言。GPU后端为记录替身，不冒充真实GPU验证。
- `verification.json/.log`及实际`verify-final.py`核对19源→安装→JAR映射；17模型/纹理逐字节一致，两mcmeta按冻结metadata语义一致且安装/JAR字节一致。旧1038assets中**1037保留**，唯一授权变化为静态燃料模型；原item、CT三源码及03A/03A1合并后的59交付源保持。scope-check记录27精确实现路径，无手写尾空白；累积自动logs保持，不夹带治理/PM同步。
- `l2-r2-binding.json`与scope-check记录最终12/12共享源匹配R2、公共ABI保持；复用L2生命周期10/10、原投影/真实服及独立窄审，不重审共享源码或重跑前置。
- 本审查只读确认当前冻结清单SHA为`6eafcbcaa90853bd63277d0669c2e5fa453607819217a88258ab77c55704efaf`，且实际审读的8份Java当前SHA全部匹配。复用负责人已核28交付＋35证据、63/63冻结内容绑定，不重复整套哈希/资源矩阵。
- 候选`candidate/create_nuclear_industry-0.1.0-ART-REACTOR-03B.jar`记录**2473054 bytes**、SHA256 **`7ef99a946c3be88c5b3d6f33fbc49181024c70a4360ce365624c8039e35d441f`**，与冻结清单、原始核验输出和资源映射相符。

无需新增回归或扩大性能工程。后续只安排本批用户观察：实际功率/耗尽蓝辉、完整actual深度/卡死/拔出、真实液位/混色/透窗、真暂停/恢复、资源重载及拆坏恢复。自动与静态证据支持内部候选交接，最终人工验收仍由用户确认。
