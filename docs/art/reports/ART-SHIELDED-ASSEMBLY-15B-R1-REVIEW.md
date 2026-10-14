# ART-SHIELDED-ASSEMBLY-15B-R1 组合窄审

2026-10-14，原唯一审查者 `shielded15_review`。当前原生层证据和最小资源修订的组合窄审**无剩余必改，可由原包作者执行本批唯一必要增量assemble**。此结论确认了原生静态窗面错误绑定SOLID及其最小修订，不表示用户Minecraft黑窗已复看通过；Three透明画面与构建均不能关闭该故障门。

仅写本报告。已读R1完整卡及唯一离线探针精确扩卡、主目录最新AGENTS、治理1.2/5.1/5.2；应用systematic-debugging、已读resource-pack/modding/testing与verification/组合审查规则。锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280，复用art-studio/HEAD88b8fd2。原故障包为a92a0bed…，待本次真实打包后仅补制品身份。

## 原生证据和适用边界

实际读取 [ASSETS](ART-SHIELDED-ASSEMBLY-15B-R1-ASSETS.md)、[PREVIEW](ART-SHIELDED-ASSEMBLY-15B-R1-PREVIEW.md)、[CLIENT](ART-SHIELDED-ASSEMBLY-15B-R1-CLIENT.md)，以及R1/assets的原始native RED/GREEN、metadata、probe身份和提取的锁定原生源/shader。

1. 实际window PNG的247个透明像素是黑色RGB/alpha0，其余9个反光像素alpha255。part0/1/4/5各off/on共8模型有实际#window面；其它8模型虽然材质字典含该槽，却没有窗面。
2. 已读真实BlockModel/ExtendedBlockModelDeserializer hint入口、SimpleUnbakedGeometry和SimpleBakedModel层查询、ItemBlockRenderTypes的默认SOLID map及NamedRenderTypeManager的vanilla cutout_mipped组。solid shader没有alpha discard，透明黑样本仍写色/深度；cutout_mipped shader在alpha<0.5时discard。该链解释静态黑窗底覆盖，不能由网页自行alphaTest反证。
3. 探针实际用BlockModel.fromString解析生产JSON，ElementsModel按完整元素、实际PNG sprite和customData进行原生烘焙，再查询BakedModel.getRenderTypes；不是只断言JSON字段。原生RED完整16模型、8个窗面目标solid=true/cutoutMipped=false、incorrectBindings=8、exit1；同一已编译探针GREEN为错误0/exit0，窗面8个cutoutMipped=true/solid=false、非窗8个SOLID保持，16项quadCount不变。
4. 离线环境显式提供空LoadingModList，仅调用原生vanilla层预注册，不发MOD事件；IRON状态验证未映射方块的默认回落。源中的主控/part无静态层映射，与该回落一致。这是锁定原生层检查，未覆盖用户整套mod加载、插件/GPU或真实光照。编译错误和首次FeatureFlags前置失败另存，未计作故障RED。
5. 原BER三partial已用cutoutMipped及`.light(light)`，世界采光为另链；noOcclusion也已存在。此次没有满光、改光传播/注册/碰撞或删除玻璃，不归因插件。原生非translucent BlockItem fallback为Sheets.cutoutBlockSheet，故没有扩itemmetadata。

## 最小修订、身份与保护

仅8个窗面候选/对应生产part模型增加 `render_type: minecraft:cutout_mipped`。删除该新字段后对象与a92备份相同，原C几何/UV/色板/9PNG/三partial/item display/灯映射保持。produce.py的实际导出分支按真实面是否使用#window设置声明；独立R1模式验证原生RED、a92包和全部29映射后安装，不重导PNG或重切八格。旧generate/install防回写实际exit1，R1身份保持，未覆盖旧15B检查/备份。

复用metadata-checks的passed=true/errors=[]、29映射同源、18预期变化路径与实际一致、57其它原输入保持。审查只读当前SHA核对：62项当前资产来源/生产、6项probe源/class/classpath/RED/GREEN及6项preview/client来源全部零漂移；scene仍为原 `989e60022e1006a7015494f2ea6a0fc789dc0596e911c6e8942a18dfc2c1eb32`。原作者37/37、9个变化HTTP输入同源/exit0复用，不重跑原79检查。

4193/4194及原托管身份保持，没有新Three截图或capture来冒称游戏修好。探针Java仅在tools、class仅在R1/assets，未进src或JAR；本轮审查没有运行探针、测试、生成、HTTP、Gradle、游戏或Git写。无Critical/Important必改，原C/15A/14B不扩审。

## 同次制品补核

等待原包作者唯一必要assemble及实际8模型production/processResources/JAR读回；失败保留原证据和a92包，不拼装JAR。最终候选只交用户复看同一台观察窗与内部机构，不重测全设备/配方，不推进动画或main。

### 2026-10-14实际制品追加

上文“等待构建”为补核前阶段。现已读取原作者真实唯一构建及制品比对，并完成同次身份补核：**候选无剩余必改，可交用户局部复看；Minecraft黑窗视觉门仍未关闭。** 没有新一轮审查、探针/测试、生成、HTTP或构建。

- 实读R1/client的assemble.log/result：BUILD SUCCESSFUL、exit0、13.1894887秒；compileJava UP-TO-DATE，processResources/jar仅各执行一次，未跑全build或游戏。
- 当前JAR独立SHA/大小读回为 **2,812,546B / `602985f85d2522c7914d7a4cef0b1c822f8c50ce11687e60af2be697428e3cd3`**，与原作者构建/制品记录及负责人读回一致。
- jar-verification原始passed=true/errors=[]，8项候选/production/processResources/JAR读回均一致；变化恰为part0/1/4/5 off/on的8个批准metadata模型，removed=[]。1947个非本批旧载荷保持，其中557个class未变，离线probe及其class未入JAR；PNG/partial/item和其它资源载荷保持。此处复用同源制品比对，不重新运行核验脚本。
- a92故障原包备份独立读回 **2,812,280B / `a92a0bed0f948fc000b58800cad34f84c03ffb314b167fea3a839f117b12ff7f`**；原7d/d12及历史日志保持。本包只在art-studio候选，未写main/Git/世界/配置。

制品补核只绑定已审最小修订，不能证明用户渲染栈/GPU和真实采光效果。接下来只看同一观察窗与内部机构的实际反馈；不借此关闭15B其它未明确报告项或开始加工动画。审查至此停写。
