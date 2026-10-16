# RADIATION-01 组合窄审

2026-10-16；审查者 `/root/radiation_foundation_review`。仅审查工作差异与既有证据，不实现、不派子Agent、不执行Git写操作，不重复运行测试、构建或游戏。本报告不记录验收、合入或客户端手测通过。

## 两项结论

- **规格符合性：存在需整改项。** 首次小数燃耗标记、真实物品历史、基本源分类/屏蔽/剂量、防护服/仪表及五配方已形成相应实现和定向证据；但预算耗尽后的公平续接没有覆盖射线和实体列表，可能永久停在 incomplete；持表同步在该状态下也不满足默认1Hz合同。具体为下列3项P2，无P0/P1发现。
- **代码质量：需整改。** 服务端权威、S2C方向、物品库存归属与几何缓存分离总体清楚；当前把部分跨帧进度隐含在精确坐标缓存中，而非完整采样任务，形成活性缺陷。需要按发现定向修复/补证据；不要求重复现有41项/9项全部业务。

## 输入、规则与身份

权威为候选 `docs/superpowers/specs/2026-10-16-accident-radiation-design.md`、主树 `docs/superpowers/plans/2026-10-16-radiation-foundation-01.md` 和主树 `docs/reviews/2026-10-16/radiation-foundation-01/DISPATCH.md`。已按裁定采用保险库每格局部库存、BE.setChanged只读通知和外壳位置名单，未以旧候选卡措辞否定这些批准项。

读取候选根AGENTS、治理及5.1、STATUS；实际使用以下技能：

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：真实物品、事件生命周期、Mixin、S2C/专服边界。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：区分纯JUnit、真实BE/FakePlayer断言、正常保存与客户端操作，不重复启动验证。
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：1.21.1单数数据路径、原版物品模型、标签追加和五配方。
- `C:/Users/IKSXH/.codex/skills/minecraft-ci-release/SKILL.md`：只读核对候选JAR身份和测试类/模板排除，不引入版本/许可/发布变更。

实际Gradle文件仍为Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。读取 `review-source.diff` 及实际源码、必要Create/NeoForge本地sources依赖；未联网获取另一版本的API。

本次只读重算 `source-sha256.json` 的61个文件：字节数/SHA256均一致，HEAD仍为 `5e706e535d97cbcfe167fa4338265b44a610964d`，生产改动尚未提交。JAR实物为3417633 bytes、SHA256 `9F78F1100D0EEAE6545B6A1A92B0416AB504CC081ED160D25B9CB53B0982B46A`，与 `candidate-jar.json` 一致；ZIP条目中本批GameTest类及独立域目录/模板为0，RadiationConfig.class存在。被ignore的RadiationConfig.java也包含在61项核对中。

## 实际发现

### R1 [P2] 移动目标的射线采样没有可保证完成的跨tick续接

位置：`E:/MyMC/NewMod/Create_NuclearIndustry-radiation/src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationSourceIndex.java:120`（相关142～155、22～23行）；`RadiationServerEvents.java:49`。

触发：默认32格内存在一批位置不同、未屏蔽的源，全部路径合计超过32768个射线格；玩家在重试期间持续移动。可构造约2000份位置各异、位于玩家20～25格一侧的已用组件掉落，路径每条至少约20格；总路径超过40000格，而栈读取远未达到32768。这里给的是静态控制流构造，审查未新开游戏复现。

证据：每次 `sampleAt` 都新建局部 `merged`、从头遍历它；`path` 超预算直接抛出，只把**已经完整计算且start/end完全相同**的路径放入 `rayCache`。`sample(player,p)` 每次取玩家当前精确position。持续走动使每次重试的end不同，上轮缓存全部无法命中；每tick重新耗尽射线预算，永远返回 incomplete。即使玩家静止，维度内任意实际方块变更也经 `RadiationBlockChangeMixin.java:19` 调用 `shieldingChanged` 清空整个射线缓存，可造成同类无进展。

影响：不是有限的采样延迟。`RadiationServerEvents` 仅在完整样本时推进吸收，所以低剂量玩家可在持续暴露下永久不增加剂量；已有高剂量伤害虽继续，并不能弥补新增剂量冻结。也不满足计划“射线预算按公平游标分帧”的合同。现有65536件嵌套测试只证明ItemSource Cursor在静止目标下续接，未覆盖本路径。

建议：把待完成采样的目标/源投影/时间、累积值及源与射线游标作为明确任务跨tick保留，保证任务完成后再接后续位置；或采用另一个有界且可证明前进的方案。对实际相关屏蔽变更做失效，不能依靠整个维度任何更新都清空的精确坐标缓存充当唯一进度。定向补“超过射线预算＋玩家每tick移动”和“无关位置方块更新”的完成/非零剂量证据，保留未完成不恢复和现有高剂量伤害约束。

### R2 [P2] 实体列表超栈预算后始终重读前缀，没有实体审计游标

位置：`E:/MyMC/NewMod/Create_NuclearIndustry-radiation/src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationSourceIndex.java:116`（相关118、131～139行）。

触发：半径内掉落实体数量大于单tick STACK_BUDGET，例如32769个不合并的已用组件ItemEntity，静止玩家、静止物料且没有库存变化即可触发。也可由附近玩家库存和其他读取共同耗尽剩余预算。该构造同样是源码推导，未额外启动测试。

证据：与BE的ownerCursor/slotCursor不同，`getEntitiesOfClass`结果和其迭代位置只存于本次调用；第32769次 `read` 会在读取摘要缓存**之前**因 `++stacks` 超限抛出。下一tick预算清零后，又从相同实体前缀开始。即便所有摘要已缓存，每个实体仍消耗一次stack工作单位，因此永远无法到达完整样本。局部 `merged` 和已累积实体结果也不会跨tick保留。

影响：静态载荷也会永久饿死，并冻结附近玩家新增剂量。掉落保护使已用组件不会自然消失，系统不能依赖实体自然过期来恢复。与报告已声明的“连续修改超大库存”局限不同，此处**无需任何数据变化**。

建议：为掉落/随身库存枚举建立与BE同等级的有限预算续接/快照，或用实体生命周期维护可重建投影，确保静态集合超过单帧限额时仍能完成一轮；不能提高限额代替续接或把部分结果当完整。补静态超列表预算的定向活性证据即可，不需重跑无关业务。

### R3 [P2] incomplete重试把持表S2C从1Hz提升为逐tick

位置：`E:/MyMC/NewMod/Create_NuclearIndustry-radiation/src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationServerEvents.java:59`（相关38、46、49～50行）。

触发：持表真人玩家达到默认20tick采样点后，本次样本 incomplete；现有65536件嵌套场景已能提供短时 incomplete，R1/R2则可长时间维持。

证据：ONLINE_TICKS只在样本完整或关闭辐射时复位。incomplete后elapsed保持≥20，随后每个tick进入采样分支；发包位于完整性判断外且没有独立发送时钟，于是每tick发一个新gameTime的包，默认可达20包/秒。客户端会接受这些递增时间包；声音自身节流不等于网络节流。

影响：违反1Hz必要数值同步合同，并在最忙的超预算阶段放大多玩家网络和客户端处理成本。报告中的S2C计数0来自没有真人登录的夹具，不能覆盖该分支。

建议：将采样任务重试/剂量累计和对外发送节拍分开，incomplete状态也按默认20tick最多发送一次；完成信息按合同节拍交付。可定向核持表真人或可统计发送入口，证明完整和incomplete两种状态的速率，不需把FakePlayer夹具伪装为真实登录。

## 已核对且未提出新增问题的范围

- 仪表prepareFuelPortCommit消费本次正plannedFuelBurnUnits、只准备副本，并沿原提交入口写端口；首次小数燃耗与拒绝提交有真实断言。枯竭转换复制原patch并去掉DAMAGE/MAX_DAMAGE，精确提取保留历史；新件不继承旧件标记。
- 原版CONTAINER/BUNDLE_CONTENTS显式栈、桶合法性/非法桶裸载荷、数量累加和有限强度饱和处理；源身份不随配置强度0消失。带读取正式表/toInsert/toRemove并按运输实例去重；保险库每格getInventoryOfBlock避免叠加CombinedInvWrapper。已核本地Create源码的控制端加载检查，未见该调用强载控制区块。
- 外壳按底/棱边、顶帽、侧壁名单判断；壳完整性与内部机构/运行合法性分离；保存几何与端口位置不复制燃料库存，破口/补壳由实际提交通知失效。末轮真服新增的非法顶帽断言对应最终实现。
- 剂量持久化/死亡克隆清零、非死亡克隆保留、关闭配置恢复、伤害独立于不完整采样继续、短时症状不主动remove其他来源效果、伤害tag绕护甲；四槽防护和皮甲属性/修复保持批准值。
- 五配方、屏蔽标签、damage_type与bypasses_armor追加、1.21.1模型路径、原版外观复用和中文提示；未增加事故第二批内容。S2C注册没有C2S设置剂量入口，客户端有维度/时间/有限数检查和退出清理，专服既有启动证据支持端边界。
- 新增/修改手写注释为中文；未以格式偏好、通配import或一行写法提出无实质行为影响的整改。未发现借本批改热工、管理员、旧配方、DU平衡或旧存档迁移的差异。

## 复用证据、局限与未判断事项

- 直接读取归档JUnit XML：9个suite共41 tests、0 failures/errors；这是复用已有结果。最后仅壳位置名单/打包域排除变化，相关壳分支由最终真实服覆盖，不需要重跑未变的纯数学/事务断言。
- `shell-position-green-final.log` 明确9/9 required通过、三维度全部保存、BUILD SUCCESSFUL和GRADLE_EXIT=0；`assemble-final.log`为增量assemble成功/exit0。早期失败保留，不用旧7/7代替最终来源。已用源码哈希确认最终实际实现与封存源一致。
- 9项真服不是9项真人操作：物流用实际BE/端点但部分通过反射/分阶段持物交接；玩家为FakePlayer；掉落到期测试包含事件入口，完整NBT往返和维度保存也不等于退出后真人重进。连续动力机械臂/带动画、各常规容器逐一搬运、真人换维/重登、主副手HUD/声音/遮挡和多人携带源影响旁人仍留既定人工门。没有据此预判手测通过。
- 性能数据为每阶段40个GameTest服务端Pre/Post区间，包含其他小夹具、JIT/GC和快进；最终完整16³+196已用组件+满架443栈/203BE/0射线、稳定shellReads增量0与日志一致。没有完整已用堆的独立关闭采样对照、4真人或客户端数据，因此不能计算新增因果MSPT/网络净成本，也不能证明上述超预算活性；报告已如实限定口径。
- 本次未新增跑测。R1/R2/R3是静态控制流和预算下界推导，尚无新运行日志；整改者应只补相应复现/修复证据。事故账本、残骸、世界动作、新废料封装以及旧版本迁移均不在本次判断范围。
- Git跟踪的日志/JAR工作副本与被ignore配置源均只读保留；如何暂存精确生产写集及正式包身份仍由PM管理。
