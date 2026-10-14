# ART-TURBINE-10B 唯一组合窄审

日期：2026-10-12。角色：独立审查执行者，非PM或负责人。唯一读源树为`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`；仅写本报告。没有子委派、Git写操作、代码修复、测试/构建、游戏启动或世界操作。本轮按任务卡分初始与追加阶段，追加仅复核实际增量及原始证据，不另开审查人。

## 初始阶段结论

**存在1项P2必改，组合审查尚未关闭。** 素材26项及接口中立实现中，除下述可见性回收导致的相位跳变外，当前读源范围未发现其他必改。转子API绑定、最终定向GREEN和组合JAR尚未交，不能据本阶段结论称为已编译、已测试、已打包或真实GPU视觉验收。

### P2-01：不要因离屏21 tick撤销仍有效身份的累计相位

位置：`src/main/java/com/iksxh/create_nuclear_industry/turbine/client/TurbineClientEvents.java:95-98`；相关初始化为`TurbineAnimationVisualState.java:49`。

`tickWorld`先核BE/世界/区块/朝向/端位有效性，却额外以`CLOCK.ticks() - track.lastSeen > 20`清掉同一已加载BE的轨道与相位。`lastSeen`只在BER取角度时更新。相机移开约1秒就把“未绘制”当成“身份失效”，回来后新`Phase`从0°开始。具体读源路径：同一输出轴16 RPM推进5 tick得到24°，采样0 RPM停在24°；不再调用BER取角度、但继续21 tick，轨道被清除；同一轴仍已加载、LOCATED且0 RPM时再次取角度得到0°。这违背当前卡的累计相位/零速停在当前角度合同，模型分面和纹理方向会变化。持续旋转的离屏轴同样丢掉逐tick的换速锚定；不能将这一回收策略复制到同owner的转子组相位。

建议在同一有效身份仍存在时继续弱引用逐tick维护，仅在真实BE/几何身份失效、区块/世界卸载、重载或代次撤销时清理；若必须回收轨道，应保留能可靠恢复的累计相位，不能从可见性超时推定身份已失效。必要定向用例是离屏超过20 tick后零速角度保持、非零速/离屏换速仍连续，且真实卸载/替换仍撤销。本审查没有执行该用例，结论来自生产调用链推演。

## 已审范围及依据

- 已读本批任务卡、`docs/art/ART-TURBINE-10B-CANDIDATE.md`、ASSETS/CLIENT报告、主目录最新AGENTS及治理1.2/5.1/5.2。执行者权限与项目一次定向组合审查优先；不扩审其他dirty、09/R2或共享服务端逻辑，不制作全树保护矩阵。
- 实际应用`minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`、`requesting-code-review`、`verification-before-completion`的SKILL.md；版本核`gradle.properties/build.gradle`为MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。技能中的新版本示例、另派审查及重复测试/构建不适用于本批授权。
- 素材读新producer/checker/installer、冻结资源映射、安装清单/原始日志及实际模型引用与OBJ样本。producer不调用旧全库工具，独立MTL/四SVG像素源，located转子空elements，前后固定座不含旋转轴/内圈，未成型为完整单件。真实OBJ的前后端盖、孔壁法向与绕序、连续圆筒/整体端板UV实现和原局部检查相符；没有用预览修正法向掩盖生产绕序。已看原`front-inside-exploded.jpg`，该截图仅支持离线拆件观察。
- 原`check-assets.log/check-assets.exit`为7项检查、10模型、156闭合实体组及exit0；原`install.log/install.exit`为26安装、17替换/9新增、17备份及exit0；原`install-readback.log/install-readback.exit`为来源→安装26/26、旧源→backup17/17及exit0。映射SHA256为`a39ff51073c711fd3e5480b1fc972033f8211780182c8c6f7a5710b877855d0c`。这些是读原证据，未重跑checker、producer或安装器。安装后原checker末尾读取生产旧轴路径的语义已变化，报告已如实指出应从旧源备份回溯，不要求重复执行它。
- 读`Clock/Phases`、OutputRenderer、ClientEvents和两测试。0.3度/RPM/tick与锁定Create字节码一致；rotateCentered输入弧度，Yaw*Roll按localZ先转、再映射，北/西+与东/南-符合世界正轴符号。速度来自真实`getSpeed()`，过载/冻结返回0；未用额定/配置RPM代替运行速度。换速先以旧速推进后锚定新速，暂停冻结tick/partial，值对象key与弱BE/world无反向强持闭包。MOD总线负责partial预初始化/renderer注册/重载，Game总线负责Post及卸载，显式暂停gate。仅客户端类引用渲染/ClientLevel，不新增服务端调用。
- 原RotorRenderer仍静态，保留跨格AABB并未擅自绑定裸TurbineView或客户端locator；属于尚待交付的范围。两测试共13例实际调用累计相位/生产提交/事件边界，尚未运行；目前离屏测试只有10 tick，覆盖不到P2-01的21 tick回收。没有将夹具SBB/Matrix断言扩大为GPU验证。

## 初始客户端源身份

以下SHA256为本阶段只读取证，供同轮追加区分实际差异：

| 文件 | SHA256 |
| --- | --- |
| TurbineClientEvents.java | 7bce48a901cb4fd5671dbe62f985d1f87b6ccfd8d1ef773fba43b985fcdaf1ee |
| TurbineOutputShaftRenderer.java | 471c7b5b4f7c9dafac1bfa06d15a19fd63b6b641d151c309b6219d2dbe7f16d3 |
| TurbineAnimationVisualState.java | 43b802d41b55a6492785a397aac6edbd8231ff5db442816c476be9a3ca1a712a |
| TurbineAnimationVisualStateTest.java | 72fd9218d31986566e1ee2c0f5ed3283062a06bc755c8dbd22b88abfe7a420e2 |
| TurbineAnimationRendererTest.java | 47162910e9887ca230a0fd1be5b759a0367227e338a81967f2d7922a703baa0d |

## 同轮追加阶段待审

等待负责人提供实际共享HANDOFF、授权的最终Rotor绑定差异、P2-01整改差异及原始定向GREEN/增量assemble日志、最终来源→安装→build/JAR身份。届时只核API可靠归属/有效性/真实速度的消费、同owner/代次多列相位与失效/卸载、完整partial和固定件提交边界、受影响用例及实际制品。共享前置已审证据直接复用，不重审服务端、不重复运行测试。真实游戏3/5/7、四向、前后轴、变速/停转/真暂停、拆除/重载视觉门仍由用户完成；07B-R2独立门保持。

追加阶段尚未开始；此处不预填通过结论。

## 同轮追加一：P2-01窄修读源复核

同日收到原消费者窄修后，仅读`client/p2-fix.diff`、修前两源、修后两源及`p2-after-source-manifest.json`；未重复审素材，也未运行测试/Gradle/checker。

**P2-01的代码整改读源已关闭，未发现新增必改。** 原可见性TTL、lastSeen字段及BER更新已全部移除，仍可信的同BE即使长期离屏也由`tickWorld`逐tick锚定真实速度；弱引用结构保留。真实实体替换、LOCATED撤销、方向/端位变化、缺区块/区块卸载、世界更换/卸载及模型重载仍清理旧相位，可见性不再冒充身份失效。修前两文件SHA与初始审查记录一致；修后实际SHA与交付manifest一致：ClientEvents=`5f62bf467840415f5c98908d98ca24caf0e3cbcd7976629e97601ae229853a59`，RendererTest=`d095d0b0a68494d1b68f83f1444a3a91e0dbb8f52e3f0e44f953c3a45edbc258`。

新增两例读源核对：第一例通过真实outputAngle/tickWorld先停在24°、100tick不调用BER仍要求24°；随后不在换速瞬间调用BER，tick锚定32RPM并离屏推进40tick，要求`(24 + 32×0.3×40) mod 360 = 48°`，能覆盖原20tick回收及离屏换速缺陷。第二例直接调用生产GameEvents的`LevelEvent.Unload`入口，要求同一已加载停止轴旧24°相位撤销为0°。既有替换/缺区块/换世界例保留。两测试现共15例**待运行**；以上为读源合同核对，不称RED/GREEN或测试通过。

完整组合审查仍未关闭：原RotorRenderer只读，实际共享HANDOFF/API绑定、对应最终GREEN、必要增量assemble及组合JAR身份继续待同轮追加；初始素材结论与独立游戏人工门保持。

## 同轮追加二：中立GREEN证据

同日仅读消费者`client/01-neutral-test.log/.exit.txt`、`02-neutral-test.log/.exit.txt`、两份`02-neutral-test-results`原XML、summary/verified-source-manifest，以及实际测试局部差异与五源SHA。没有运行测试、Gradle、checker或浏览器，不重复审素材。

初次01的生产compileJava及processResources已执行，`compileTestJava`因测试直接引用运行classpath内的Flywheel `PartialModel`缺少编译期类而失败，exit1；**这是测试编译RED，不是业务断言RED**。随后仅将原`frontAndRearSelectDistinctRotatingPartials`测试改成反射调用真实生产`partialFor`及真实`modelLocation`，前后不同ID及两个精确ResourceLocation断言完整保留；未改变生产、构建脚本或依赖。该窄修没有把资源断言换成源码字符串或跳过测试，未发现必改。

02原日志实际为`BUILD SUCCESSFUL in 16s`、exit0；两份原XML分别VisualState 6例、Renderer 9例，合计15/15，failures/errors/skipped均0，XML时间为2026-10-12T04:30:59Z。case名单含长期离屏停转/换速及实际LevelUnload两例，与summary一致。**P2-01已由实际对应GREEN证据关闭。** 这是读取消费者原运行证据，非审查者重新执行，也不覆盖真实GPU或世界游戏验收。

五份实际源码SHA均与`02-neutral-verified-source-manifest.json`一致：三生产及VisualStateTest与追加一/初始身份保持；RendererTest现为`96b48f2246c84749d75befd87e2b391b021cb29dd7473126b7c58c4a7fd93f2d`，差异仅前述反射入口窄修（追加一的两新例保留）。中立实现目前没有未关闭必改；素材26/26安装与17backup原证据复用。

组合审查仍未完成：原RotorRenderer/API绑定、最终受影响GREEN与必要assemble/JAR尚待实际HANDOFF后同轮追加，不能把这次中立15/15称为完整转子动画或组合交付通过。

## 同轮追加三：R1冻结材质/UV候选

用户指出正式预览贴图偏离已选B稿后，依据`ART-TURBINE-10B-R1.md`仅审原producer的材质/UV增量、原B参考和R1原始证据；没有另派审查人、执行checker/producer/Gradle/浏览器或扩审Java/服务端。前述10B几何与中立15例保持历史证据，不能据几何通过推定用户已接受风格。

**当前冻结材质/UV读源未发现必改；不是R1整体完成结论。** 已实际核map SHA256=`238f377912d128ed1b8bbd7531aac16803ae3c133d05c53728d4f9b2ccb29704`及两份来源SVG与原B逐字SHA：blade=`40acb859f5694001a185fd8043b12cdad708ea9bc5e90ab8a5a03e76d726de34`，bearing=`a647c2eff202c0f0492295c7fcbc409598970cfdc391f0e5ce8f3101e90d908c`。producer直接复制原B的12/10色SVG，PNG按原矩形顺序栅格化；轮毂、轴、内板、孔壁等取17个原B钢色块中心，小铜为`#b27a4e`，不再在这些面全铺新条纹。输出轴侧面使用纯钢取色列表达端板厚度内与内侧色区，ID/MTL不变。

读原B`texturePattern/rotorMesh/supportPlate/plateMesh`、R1投影研究及实际producer：叶片负弦根角原点、theta+.063径向/切向基与1格纹理周期一致；未成型依据实际缩小几何重新计算同一1格密度。U/V整数seam采用原平面插值、tile折返至0..1；仅四转子OBJ及工具厚对照叶片两端面共面切分，薄边补对应共线分片保持闭合，不改变原实体表面。五边形拆同向三角形，实际输出不保留超过四角OBJ面；非叶片面的位置/法向不改。实际D3 OBJ差异样本为原V/VN保持、VT改中心取样，JSON仍`flip_v:false`。锁定NeoForge直接V行为目前来自本卡/作者核实；本批尚无已交ObjModel源码片段，后续只补该精确原证据，不作外部广查。

原`r1/freeze.log/.exit`记录68项冻结（13源、3参考、26candidate、26installed）及exit0；`pre-r1-identity.json`绑定原map和既有17backup保持。原最终`produce.log/.exit`为26候选生成exit0，`check.log/.exit/checks.json`为7项、10候选模型及厚对照、120叶片组exit0；已读checker，确实逐叶片验证分片处于原凸面/边界、同向面积覆盖、闭合/法向、全部非叶片几何签名与bounds、逐顶点1格投影UV同源/有界及SVG→PNG像素。差异14资源为10OBJ+4PNG，11JSON和MTL逐字保持，26生产资源与preR1安装身份保持。这些均为读原运行证据，没有审查者重跑或重新封包。

R1生产尚未安装；纯色UV的Canvas三角AA接缝由原作者进行网页窄修，当前不预填网页/视觉整改通过。R1-ASSETS报告尚未交，原负责人实际复看、网页差异/原证据、精确安装及来源→安装身份留同轮追加。原Java15例直接复用，Rotor共享HANDOFF/API和最终assemble/JAR仍待审；GPU/用户视觉及07B-R2门保持独立。

## 同轮追加四：R1直接V与网页AA窄修

收到R1-ASSETS报告及原源码证据后，仅读报告、`r1/neoforge-obj-uv-source.json`与`neoforge-obj-uv-excerpt.txt`、`preview-seams-before.js`到当前preview的局部差异、原Node语法日志/退出码及负责人保存的`root-thin-after.png`。没有启动浏览器或运行Node/checker/Gradle；Java和素材几何已审证据不重审。

**直接V证据缺口已补，网页窄修未发现必改。** 锁定NeoForge21.1.219的sources.jar路径/源JAR SHA=`814983ff6c233935f176a99b10a495cfa37971ffe1a73a6f8b097e893553ca8c`、ObjModel.java条目SHA=`aa420616da3599acc0b69a3a69d863d4f20f7be487d1e73b9718f61d880a7ed1`由原作者记录；已实际读369–379行片段，其中375–376行`getU(texCoord.x)`及`getV(flipV ? 1-y : y)`，与保持`flip_v:false`的生产JSON及网页直接V相符。此为已读原锁定源码证据，不冒称审查者另行提取JAR。

网页对纯色面直接从实际PNG色块取色、整面一次填色；只在两面共享同一3D边、同色且同向法线时补0.65px内部覆盖，外轮廓/孔边不会因单面边而被描线。非纯色的每片共面UV为仿射投影，整个face一次clip/draw；shade在同一brightness提交，移除两个三角分别提交及另一次阴影mask，没有改OBJ、PNG、map或几何。本次preview实际SHA=`7cf6228f9146bc84d44256a740c67c0087719304390e268877393197682a55c8`；原`preview-seam-fix-syntax.exit`为0。原较早命名冲突及随后语法整改由报告保留，不把Node语法通过扩大为视觉通过。

已看负责人保存的45°薄厚图，支持本次离线假三角线整改观察；D7及前后轴承观察由负责人另有记录。冻结map实际SHA仍`238f377912d128ed1b8bbd7531aac16803ae3c133d05c53728d4f9b2ccb29704`。R1-ASSETS报告已交，替代追加三“报告尚未交”的时点；作者报告里的“等待负责人复看”是作者交付时点，负责人后续复看和14项安装授权不推定为游戏验收。

正式14项安装正在等待作者交回manifest/readback，本阶段尚不能关闭安装身份。最终来源→安装证据、Rotor实际API绑定及必要assemble/JAR继续同轮待审；不预填R1正式交付完成。

## 同轮追加五：R1正式14项安装收尾

仅读R1卡末尾精确14路径授权、作者R1-ASSETS末尾安装补充、`r1/install.log/.exit`、installation-manifest/events及`install-readback.log/.exit/.json`。不运行素材/Node/Gradle检查，不重审Java15例或权威服务端。

**R1材料整改与正式资源安装阶段无必改，本阶段审查关闭。** 安装清单中14个变化路径与授权一致，仅10OBJ+4PNG；原始事件中`all_14_backups_verified`早于第一份`installed`。14个preR1旧源备份位于新的`r1/installation-backup/`，未沿用/覆盖旧17基线备份。全部26preR1安装身份已先预检；原安装日志为14复制、14备份、26candidate-installed一致、12保护不变及exit0。

原独立读回exit0/json记录来源→安装26/26、14旧SHA→新备份14/14、11JSON+专属MTL12/12原样、旧17基线备份17/17保持；清单及事件的候选SHA/安装SHA和旧SHA/backupSHA相符，冻结map仍`238f377912d128ed1b8bbd7531aac16803ae3c133d05c53728d4f9b2ccb29704`。作者最终报告已追加实际安装状态；安装前checker的preR1生产身份断言保留为历史前提，不要求当前再次运行它。负责人另有26源/安装独立核对，此审查复用上述原始安装/读回证据，未重新封包或计算完整保护矩阵。

本关闭仅覆盖恢复已选B材质、UV/网页窄修及正式资源身份。原中立15例GREEN直接复用，Rotor共享实际HANDOFF/API消费及完整组合必要验证/assemble/JAR继续待审；全部10B动画与GPU/用户视觉验收未完成，07B-R2独立门保持。

## 同轮追加六：实际runtimeDisplay消费差异

已读原10B旋转消费补卡、实际PM`docs/reviews/2026-10-12/turbine-runtime-display-01/HANDOFF.md`、`03-rotor-binding.diff`对应三生产与原RendererTest、03冻结清单/源副本及原始日志/exit/XML/summary。只核新增客户端消费，不重审Provider的16/16+3/3、R1资源或服务端，不执行测试/Gradle。

### P2-02：Post和BER重新绑定member时必须使用一致的撤销路径

位置：`TurbineClientEvents.java:160`，对照BER路径113–115行。

BER在成员UUID/index/整机身份不匹配时调用`removeRotor`，让没有其他可信登记成员的旧组撤角度再绑定；Post分支却直接`entry.setValue(new RotorTrack(...))`，随后继续激活旧组，跳过同一撤销。具体合法输入边界：同一客户端BE收到较新可信整包，member UUID更换、整机generation/epoch保持，且旧组只有这一列被登记。旧16RPM相位已到24°。若先由BER消费新member，旧唯一成员撤掉、共享phase重建从0°；若先由Post消费，则旧Group仍active，沿用24°累计。两入口的次序决定能否沿用旧成员轨道，不符合身份替换撤销及绘制顺序不决定动画的合同。runtimeDisplay客户端member字段可由原生整包更新，不能假设同一个客户端BE的member永不变。

必改建议：Post在重新绑定前等价撤销旧track/相应旧组，并保留其他仍可信成员支持的共享相位；避免迭代期间修改同一map导致并发修改异常。新增定向用例应通过真实runtimeDisplay/rotorPose/tickWorld入口覆盖仅一登记成员UUID更换的Post先/BER先两种顺序，以及其他可信成员存在时相位继续。审查者没有运行该用例；这是实际分支读源推演，当前17例没有member更换的这一边界。

### 必改注释：暂停采样合同需准确区分Rotor与Output

`TurbineClientEvents.java:130`称暂停“不增加时间或采样新速度”，而Rotor分支在冻结`CLOCK.time`下仍接纳可信新整包。冻结时间保证旧角度不推进，可靠性撤销和新可信RPM锚定本身符合实际HANDOFF，不要求改行为。按AGENTS中文注释与实际一致合同，应仅改准Javadoc：暂停冻结时间/角度、仍核可靠性并接纳可信Rotor整包，Output暂停不采样速度。纯注释整改无需重跑测试。

### 已核无额外必改的边界及原GREEN身份

唯一runtimeDisplay整包同时提供几何/局部有符号rpm，没有再次localRpm反号或客户端locator；RotorPose empty静止fallback、可靠0保留停角，三档完整partial复用已审Yaw*Roll动态单次提交及现有全扫掠AABB。整机key为会话+dimension/owner/generation/epoch，member/index只绑定弱BE；serverTick只排序完整样本，同tick共享字段冲突撤到更晚包，旧列包不能覆盖最新RPM；空组撤角度保留数值水位。世界/区块/重载/ended旧世界封口及弱引用无反向强持边界已核，未发现其他必改。

03原日志`BUILD SUCCESSFUL in 16s`、exit0；原XML实际RendererTest17/17、0fail/error/skip，时间2026-10-12T11:56:43.842Z，包含原受影响9例与新增8例。未变化Clock/Phases/Phase逐字段与VisualStateTest6例复用02证据，OutputRenderer源SHA保持。六实际源SHA与03manifest一致（Rotor=`7922a71b9d1c70640db99e9825b8b9c3e701ef7f7ce50eed38eb42802707b72c`、Events=`82da4d156b6c4f5cfb91309d954d1de20759b6178b9c00960cd20e54c54e5854`、VisualState=`e8277c6f533a52d3b128ee6330ce53610970bb5354dc66ba3be124d3a21aa2e3`、RendererTest=`cb412df91adfcd38a276f54dd8969a77ccbbd75cb9812e4dd38c336208c1fb20`）；原Output/VisualStateTest身份见追加二。以上是读原冻结证据，17例通过不覆盖P2-02遗漏边界。

代码差异审查暂不关闭，等待原消费者P2-02和注释窄修及对应增量证据。同轮只复核受影响差异；当前尚未assemble/JAR，不作组合完成或GPU验收结论。

## 同轮追加七：P2-02与暂停注释关闭

仅读`04-review-fix.diff`、对应Events/RendererTest实际与冻结源、manifest、CLIENT末节及原RED/GREEN日志/exit/XML。没有运行测试/Gradle或复审素材/服务端。变化仅两文件，其余四源保持03身份。

**P2-02及暂停注释必改均已关闭；当前代码组合差异无剩余必改。** Post现在先在现有iterator内移除旧失配成员，弱引用新轨道与完整样本暂存到局部map；迭代结束后以仍可信旧成员集合retain共享相位，再putAll重新绑定并统一采样。唯一登记旧成员失去支持时撤相位，其他可信旧成员存在时保留他们的共享角度；不存在活动iterator内跨函数修改ROTORS，也没有新加强持BE/世界的缓存。serverTick排序和暂停冻结行为保持，Javadoc已准确区分Rotor可信整包接纳与Output暂停不采样。

原`04-review-red`为两新例、1业务断言失败、0error/skip、16秒exit1；原XML明确expected0而实际24，复现本审查发现的唯一登记成员Post顺序缺陷。原`04-review-green`为受影响RendererTest19/19、0fail/error/skip、17秒exit0，XML时间2026-10-12T12:12:12.802Z。两新例实际调用runtimeDisplay/rotorPose/tickWorld，分别循环Post先/BER先：唯一成员更换都重建为0，另有可靠成员时都保留24；没有以跳过或放宽断言取得GREEN。原6纯核心与Provider16/16+3/3、R1检查/26安装证据直接复用。

实际Events SHA=`45cb680c157671712d36dbf22a94e04231ae5d38bd2321bb9003ce03e4b8829e`、RendererTest SHA=`47e74f4285d0183f2c3cca093dfa465bb6749d34c44489938b745ea03797c2bf`与04manifest/冻结身份一致，其余四源见03原身份。原RED、03结果与修前源保留，以上是读原运行证据，非审查者另行执行。

代码必改关闭后，等待原唯一消费者的必要增量assemble及实际JAR/来源安装编译身份作本轮最后制品收尾；当前仍没有新JAR证据，不能称完整可运行候选或GPU验收完成。

## 同轮追加八：最终代码与制品组合审查关闭

仅读消费者`client/final/assemble.log/.exit.txt`、pre-assemble、package-identity的JSON/log/exit及实际`verify-package.py`、javap输出/exit、CLIENT最终段；另只读核当前/冻结/旧备份JAR的SHA和当前/冻结尺寸，没有运行Gradle、测试、身份脚本、素材或浏览器。

**最终代码、R1资源与JAR组合无剩余必改，本次唯一组合规格/质量审查关闭。** 原唯一增量assemble日志为`BUILD SUCCESSFUL in 1s`、exit0，compileJava/processResources UP-TO-DATE，jar实际执行；不把缓存任务写成新编译或新测试。原美术旧JAR与完整备份实际SHA均`737d3bd5606109f711998da92714917de19b6ed891f0abb7fbb27ca3f1049050`，pre-assemble记录2,624,615 bytes，独立新备份不覆盖历史制品。

最终当前`build/libs/create_nuclear_industry-0.1.0.jar`及`client/final/frozen-jar/`完整副本，实际SHA均`9be1d3105034c37d27dd8448b3d1d00e2c375462d2f82b0d66a5b579b2e4ca6a`、2,777,331 bytes，与package-identity一致。实际身份脚本按批准的精确资源与四客户端前缀核身份，未替代业务测试；原身份日志exit0、errors为空。已读逐项清单：六源04身份保持及原19例XML SHA不变；26R1资源来源→安装→build→JAR逐项同源；15客户端主/嵌套class集合和字节与build一致，集合对应当前四生产源，没有旧Rotor提交类混入；Provider11源/13JAR项与其R1冻结及本树编译/资源字节同源。

实际javap原输出公开`Optional<TurbineRuntimeSample> runtimeDisplay()`，原exit0、stderr空，归一化整个公共ABI与Provider冻结相同，编译API文本SHA=`8259af615115fdd2a93acb7f78d52786aefc52013e3b8dc640218b9651c40e23`。前置Provider属于已审依赖，本轮仅读身份而不重审服务端实现。WindowsApps Python别名首启的StandardOutputEncoding错误原日志保留，脚本尚未执行；随后bundled Python同脚本身份核验exit0，没有第二次assemble。该启动异常不冒称业务失败，也未被抹去。

最终受影响Renderer19/19及未改纯核心6/6、Provider16/16+真实3/3、R1原7项/120叶组和26安装证据按前述同源复用，未机械重跑。P2-01、P2-02和暂停注释均在本次同一轮增量中关闭；初始/整改/RED/后续证据保持各自历史时点。

当前美术树制品可作为本批运行候选，但本审查不关闭真实供汽旋转、停转/反转、四向、真暂停、拆除/卸载及重载的GPU/用户操作门；三件已通过模型不重开，07B-R2独立门保持。未执行Git或main合入，不声称已更新main制品、世界或玩法。此为独立审查结论，交负责人管理候选与人工门。
