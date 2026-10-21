# ACCIDENT-THERMAL-04B 执行候选

2026-10-21；基准 `9f53a7d5218d1a51b6fdc43bbeb98735a72d9acd`，分支 `codex/accident-thermal-04b`。源码冻结待唯一组合窄审，未打jar、未Git写、未操作用户世界、不声明人工验收。

实际使用 `C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-resource-pack}/SKILL.md`：核本地NeoForge/Create源签名，复用既有GameTest域、原生液面/熔岩纹理及资源JSON。Gradle实际锁MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；未改版本/Gradle。

独立本体/区块/活跃索引与唯一payload保存在原SavedData文件；Column交一次后清空，凝固由Corium BE接受一次后清空本体。来源只审计，20/40加载tick、6000→既有2400HOT、水×4、50%ceil无额外cap均创建冻结。零payload仍热/移动/溶蚀，凝固空残骸，不造物/辐射。
保护直接投递最终BreakEvent，环境作用域只跳过本模组手动预检；原手动提交Mixin未改。回调后重验、异常/取消不移料。LevelChunk写前守卫逐次计自动块写，搬移预留2、装配台全加载预留8；权限setblock/fill有意清除单独终止，不占自动预算。较低冻结预算同tick不能再被64硬上限放大。

| 证据（同目录） | 实际结果 |
| --- | --- |
| `01-red.log`、`red-unit-results/` | 业务RED：9项/4失败，exit1；数量、加载间隔、冷却、旧边界断言实际失败，非缺类编译错误 |
| `02-implementation.log` | 1.21.1不存在所尝试的客户端注册事件，编译exit1；改用本版本FluidType.initializeClient |
| `03-unit.log`、`09-unit-final.log` | 各9/9，exit0；后者含较低预算/同tick读回断言 |
| `04-server.log` | 软管旧队列有空液成功分支，主业务失败，Java/Gradle exit1；改为出队前精确拒绝并解除异常探针 |
| `05-server-related.log` | 普通原生物品随后燃烧使世界掉落计数失败，exit1；测试改为立即领取真实掉落，生产燃烧/掉落未放宽 |
| `06-server-containers.log`、`07-server-fuel.log`、`08-server-diagnostic.log` | 核端口夹具失败exit1；08明确returned=0、端口仍1件、旧结构有效、accident=null、attempts=0、below=air。核到GameTestHelper.relativePos额外180°旋转，改原相对坐标并断言本体恰在端口上方 |
| `10-server-final.log` | 4真实业务通过（04B主聚焦、04A热火、shutdown、原生待续保存）+10显式SKIP；Java/Gradle exit0，全部维度正常保存 |
| `11-server-resume.log` | 同一server-final世界正常重进，仅1真实续接业务+13SKIP；原位置/进度/单件载荷与无ledger保留，remaining4242→4240，Java/Gradle exit0，全部维度正常保存 |
| `12-unit-boundary.log`、`unit-results/` | 最终10/10，exit0；10/11后仅补允许int上界时进度加一防溢出及该边界断言，不改变默认20/40分支，未重复服务端矩阵 |

上述每轮退出码均见对应 `.exit.txt`。10中一次“夹具保护异常”ERROR为故意保护探针，已捕获且监听器finally解除；不同于04—08的真实断言失败。首次独立目录缺server.properties日志为原生初始化，随后正常生成/启动。

10实证：3真实废料→2熔融/1固体；空入口、4HP/5秒/同类20tick及火抗、桶/水/放块/无cap、软管发现与既有level0/1队列、授权setblock/fill、当前NBT及真实载荷辐射；来源ledger删除后两团仍独立完成并各保1废料；80层黑曜石/基岩、接水6000/4凝固及6000→2400结束；新旧/无事故共享峰64。旧04A/shutdown明确用0熔融份额复用，未替换新默认50%证明。
容器：普通箱子原生生成/实收3钻石；核端口单件具名原组件逐组件匹配，端口清空、旧结构快照失效且未新增事故ledger；装配台单件携物NBT与拆前一致，8个实际格/BE移除。原生实体掉落不算块写；所有自动连带块写走同一写前预算，保守事故收据也占额度。

`SOURCE.json`列32个源码/资源文件、51个编译class（48 main含真实域、3定向test，其中9个新增main源码对应13class）、6个处理资源及SHA/bytes；6资源与源码逐字节相同。源码 `git diff --check -- src` exit0。初次候选12轮身份保存在 `SOURCE-before-boundaries.json`，当前身份见下述整改及最新SOURCE，未据此冒称已装包。
`docs/project.md`、`docs/server-config.md`、`docs/content-catalog.md`由PM维护，清单单列，执行者未修改。跟踪JAR/根日志已先备份到 `pre-validation/`，身份见 `PRE-VALIDATION.json`；根JAR仍81E7CD98…、3478572bytes，根日志由测试改写留给PM恢复。

未观察边界：客户端液面/视觉/真人操作、真实多人、物理ChunkEvent.Unload；开口泵未安装真实传动网，仅核本地Create非源抽取入口及实际isSource=false/无cap/写前防护。普通箱子和本模组核燃料/装配台已实证，不声称任意第三方容器或直接section编辑兼容；未证明跨文件崩溃原子性。完整封存/采掘链复用04A既有证据，本轮未扩大旧事故/管理员矩阵。

## 阶段A三项P2整改（同日，原REVIEW保留待原审者复核）

只改 `MoltenCoriumEffects`、`ReactorAccidentSavedData`、既有 `AccidentExecutionGameTests`；新增短筛选 `molten_boundaries`，未改资源/参数/数学、Gradle或世界路径。原本体时钟/热周期/保存恢复证据复用10/11/12，不重复全套。
`13-boundaries-red.log`：3真实业务全部失败＋14显式SKIP，Java exit3 / Gradle exit1；箱子第二邻格真实FULL不可用，但spent1/inventory0/spawns3/secondFull=true；HOT仍指旧HOT；未投影碰撞spent2、moveProgress由20变0、revision由0变1。原始日志/退出码保留。
`14-boundaries-green.log`：3真实业务通过＋14显式SKIP，Java/Gradle exit0、全部维度正常保存。箱子无FULL时spent0/inventory3/spawns0/secondFull=false，测试显式加载后原生3钻石；HOT直接setblock及采掘后同ticksetblock均新空UUID/6000时钟、旧DONE/热引用终止，旧清理不能撤新索引且新体继续计时移动；向下/横向预留冲突均spent0、进度20/17及revision0不变、两份真实载荷各1，索引拒绝覆盖，原所有者随后成功投影。
P2-1在探针前及回调复验后、任何移料/loot之前保守双核六方向两格通知读取域；高度界外沿原生VOID_AIR，水平界外等待，不委托隐式getChunk。P2-2仅复用MOLTEN，先终止HOT再注册新空本体。P2-3移动/未投影统一核位置所有权，索引putIfAbsent与撤旧前校验避免覆盖/半次撤索引。临时事件监听与测试区块票据均finally解除。
最新 `SOURCE.json` 仍32文件/51class/6资源；`boundarySourceChanges`准确列上述3文件，main输出对应14轮，未变定向test输出对应12轮。原REVIEW及其原身份清单不覆盖；源码差异检查exit0，根JAR仍原81E且未Git写/打jar，已再次停工交窄复核。
