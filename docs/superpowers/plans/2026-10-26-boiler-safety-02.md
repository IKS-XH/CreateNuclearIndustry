# BOILER-SAFETY-02：真实过热、干烧与定向爆裂

## 合同、派发门与职责

承接[01](2026-10-26-boiler-safety-01.md)，仅在01实现及其必要定向验证通过后由同一执行者续接，不另加用户中途手测。权威为用户2026-10-26批准的[冻结设计](../specs/2026-10-26-boiler-accidents-design.md)，危险拆除直接触发其实际事故。PM已只读核对热工、CommonHooks最终许可、两个自定义潜行扳手和环境移除入口，并读取执行者`build/reports/boiler-safety-20261026/TECHNICAL-02.md`；下列写集据这些实际入口制定，不授全局重构权。

继续唯一执行者、同一树`E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration`、`codex/boiler-safety-20261026`和01已复制世界；不再复制/打开用户世界。PM负责文档/Git，不写代码。执行者无Git写、核心文档、其他任务派发或验收权限。必读AGENTS、治理5.1/5.2/6、STATUS、冻结设计、两卡；实际使用minecraft-modding/testing/resource-pack，最终制品使用minecraft-ci-release，沿01实际技能路径和锁定版本，不升级依赖。

## 玩家行为与状态不变量

- 保留既有双汽mB/HU身份、共享总汽容量、纯过滤汽口、物理口预算、有效回路与结构缓存。拆掉旧的堵阀/空炉收热兜底、满汽温度2停热限制，让正常热工流程之后剩余的已付HU进入炉壁或真实库存蒸汽显热；无水无汽可加热炉壁，不能给空池造汽、给炉压免费加值。热液不足、冷液堵塞、红石仍停止新收热；红石不能删除余热或免除仍高温的危险。
- 干烧是water=0且炉壁温度不低于既有沸点；连续400加载tick损毁随机一个现存换热器/再加热段、不掉完整设备，补水或降至沸点以下清计时。只干烧不清整炉真实库存，不额外产生核废料/辐射/熔融物。
- 归一额定耐压1.0；正常出汽和逐阀泄压完成后判定。超过1.0累计200加载tick，达到1.5立即爆裂。回到1.0以下暂停累计，<=0.95连续40加载tick才清零；同tick重复调用、保存加载、结构重查不刷新额度或补离线计时。
- 爆裂一次选六面之一并保存，以所选外表面为破口向外定向喷爆；不能从大炉中心发一个到不了外壳的小球。r=实际压力/额定压力，R=clamp(2+4*(r-1),2,6)，最高16HP随距离衰减、每实体每事故一次，不能按炉体积再放大。不伤全部背面；不可破坏方块/原生抗性与最终保护取消仍有效。
- 真正爆裂提交一次后，原炉水、双汽、冷热液和其HU实际损失并撤销旧能力/供热；原生携带控制器掉落不能再带第二份事故前库存。干烧局部损毁仅失效相应结构，保留其它真实物料。附近设备被实际破坏时走其既有唯一库存移除路径，不直接抹其账本或额外造核事故。
- 玩家普通挖掘或自定义潜行扳手，在最终许可后、掉落纯快照之前检查真实危险：超压优先爆裂；仅干烧立即损毁一件，不等待自然时长。不靠LOWEST监听提前提交，不用onRemove追补。已经被外部取消/异常的拆除不得扣料、提交或改计时；正常工况沿用原规则，不把所有热炉判危险。
- 使用独立持久锅炉世代/事故ID，不能复用未持久的surfaceGeneration。冻结计划、原对象绑定、方向/半径/参数和收据先记录，原控制器消失仍可推进；旧计划不得破坏同址新建的新世代。事故环境移除有局部授权，不递归触发手动事故。
- 阶段/伤害/破坏只在服务端。无强载、无正常tick全体积扫描；事故仅处理已加载范围并用现有每维度共享64动作上限，不能给锅炉另开64份预算。恢复只推进活跃任务，不每tick遍历全部历史。

所有新增阈值、时长、恢复线、半径/倍率/伤害进入锅炉配置并校验组合、有限值和单位。可用独立事故参数record，避免无关热工Settings调用方扩散。护目镜简示干烧/超压危险和实际进度，复用图标留白，无新GUI/外观/配方。

## 精确允许写集

J=`src/main/java/com/iksxh/create_nuclear_industry/`，T=`src/test/java/com/iksxh/create_nuclear_industry/`，R=`src/main/resources/`。

- 01既有J/boiler/{BoilerState,BoilerControllerBlockEntity,BoilerStructure}.java及J/config/BoilerConfig.java；只接本批热工、持久危险/能力撤销/缓存世代。
- 新增J/boiler/{BoilerAccidentState,BoilerAccidentLedger,BoilerAccidentSavedData,BoilerAccidentExecutor,BoilerAccidentLifecycle}.java；允许Ledger合入SavedData减少文件，不另建通用框架。
- J/boiler/BoilerControllerBlock.java、J/heat/NuclearHeatExchangerBlock.java仅自定义潜行扳手最终许可后接同一事务，以及必要的已消耗目标掉落/对象再核；如确需J/boiler/BoilerShellBlock.java，仅抑制已由事故领取的完整部件掉落，不加onRemove事故或改正常掉落。
- J/mixin/ReactorBreakFinalizationMixin.java仅在原反应堆最终处理之后、最终未取消时追加锅炉处理，不改变原反应堆拒拆/回滚/燃料规则。不新增任意世界写全局钩子。
- J/accident/MoltenCoriumEffects.java仅将既有corrode环境移除事务抽出小的公共窄入口供锅炉复用：连带目标、两格读取域已加载、最终BreakEvent许可/异常、对象复核、真实容器与物料掉落、AUTO逐写预算维持同义。锅炉不取得熔融物身份；原corrode算法/冷却/速度/数量不变。共享预算沿已公开ReactorAccidentSavedData接口，不改其格式/算法。
- 新executor可用锁定版本现有自注册方式；若实际需要主类注册，仅允许J/CreateNuclearIndustry.java追加这一类的事件注册，不改其它生命周期。
- T/boiler/{BoilerStateTest,BoilerDualSteamInventoryTest}.java只更新“堵阀安全停热”被本批准取代的断言和新增真实HU边界，保其它双汽/额度/守恒断言；新增T/boiler/BoilerAccidentStateTest.java。其它01测试仅必要签名适配，不删测试或顺带修已知3项JUnit失败。
- J/gametest/BoilerSafetyGameTests.java扩展本批真实危险/保护/恢复用例，沿01精确排除、模板和筛选；不新建测试框架。共用环境移除的代表性真实容器/燃料口/装配台、最终取消由本域覆盖，不机械重跑全部熔融物矩阵。
- R/assets/create_nuclear_industry/lang/{zh_cn,en_us}.json仅本批危险/进度提示；手写注释中文。
- 本树build/reports/boiler-safety-20261026/下报告/窄核验工具及普通编译测试输出。不得改其它生产资源、反应堆/管理员/工业管网、用户世界、正式根包或版本/许可/发布脚本。编译发现准确额外入口先报PM。

## 必要证据、最终审查与候选

先固定真实热工/危险业务RED，再实现并一次运行受影响boiler单元测试和本域真实服务端。合并覆盖：实际付热堵阀升压、冷空炉/热干烧/补水复位/只毁一件且库存不复制、轻超压累计与迟滞恢复/150%即时、同tick正常足额泄压可避免事故、六向投影边界及实体单次伤害、世代/方向/计时/游标保存往返、控制器移除后推进、同址重建不受旧任务损坏、普通挖掘及两个自定义潜行扳手最终取消/成功、环境保护/不可破块/容器守恒/共享预算/未加载不强载。必要持久化用本版本自身保存恢复，不开旧存档矩阵；无法用自动证据覆盖的真人显示/保存重进列到同一最终手测清单，不伪称通过。

公共环境事务/最终拆除接线属于本次扩大回归触发：增加其代表性真实回归与相关HeatExchanger/拆除测试，记录具体选择和结果，不为历史3失败改无关逻辑。全量仅最终实际需要时一轮，并保通过/失败/跳过/退出及原XML；不能把断言过但保存退出异常称为进程成功。

01证据在实现未再改变处复用；若02改阀/热工使旧测试需跟进，只复验实际影响。不重复备份扫描/全矩阵/分层封包。最终交短EXECUTION.md、SOURCE.md及原始日志索引，PM安排一轮规格+质量组合窄审，整改只复审差异。稳定后PM先提交精确生产来源，执行者才运行一次增量jar并交身份/排除清单；PM核对源与包，最后一次集中交用户候选手测，不自动合main或替换其正式包。
