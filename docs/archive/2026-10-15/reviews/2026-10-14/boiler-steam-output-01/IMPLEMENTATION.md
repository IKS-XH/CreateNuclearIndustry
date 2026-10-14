# BOILER-STEAM-OUTPUT-01 实施交付

2026-10-14，执行者 `/root/tiered_flow_impl`。唯一工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-load-follow`，文档基线 `ec9f398`、功能基线 `8eaafd9`。PM已将8路径源码及最终JAR提交候选 `c71d690`；执行者未操作Git。原审查者已完成同次最终差异复核，Important关闭，无剩余必改。本报告记录自动验证、审查与候选，不宣布用户验收通过。

## 已实现行为

锅炉NORMAL与SC汽口按原选定真实库存主动进入合法工业管/阀路径，无需外置工业泵。控制器热工tick生成有限、仅同tick有效的请求，原统一Post调度消费；库存/已付HU/共同保压和4096mB/t物理口额度保持，与标准普通汽、邻罐普通汽主动输出及工业泵抽取共用。原普通Create路线保留，标准SC/普通Create管禁运保留，其他设备没有新增无泵运输。SC直贴邻罐不在本次修复范围。

一次性收据绑定原账本/汽池/物理口/tick及准确取出的mB/HU，在同步调用内结算。目标短收只退本次未交付原池量和同比例真实HU、原tick本次未交付口额度；close防止未结算遗漏，重复settle拒绝。没有管段库存、持久缓存或任意注汽/加热入口。目标仍用原受限许可和当前实体/罐controller绑定；源捕获汽口BE身份和原Port代次，同状态原位换BE也停止旧请求。

汽口与真实泵共用World.budget/media，路径按实交付计费；原泵短收缓存与输入段按抽取量计费规则保持。BFS沿原加载边界、方向/阀/实际泵动力及上限运行，环路去重、端点不作桥。不会绕过停泵或反向泵。

## 精确实施文件

生产仅四路径，均相对 `src/main/java/com/iksxh/create_nuclear_industry/`：

- `boiler/BoilerControllerBlockEntity.java`：汽口工业主动请求及有限源身份；普通原生分支保持。
- `boiler/BoilerState.java`：一次性原池蒸汽收据与精确退款。
- `industrial/IndustrialTopology.java`：共用BFS的汽口外向根入口。
- `industrial/IndustrialNetworkScheduler.java`：同一Post调度/预算/介质；同次审查公平轮转整改。

测试仅新增 `src/main/java/com/iksxh/create_nuclear_industry/gametest/BoilerIndustrialActiveOutputGameTests.java` 和原 `src/test/java/com/iksxh/create_nuclear_industry/boiler/BoilerDualSteamInventoryTest.java` 的三个收据行为例。`build.gradle`仅原jar排除块增加新真实类及内部类通配排除。没有注册、Mixin、配方、资源、依赖、版本或配置变更。新真实测试复用原industrial_empty，不复制模板。

## 原始验证索引

全部位于 `build/reports/boiler-steam-output-01/`，日志保留完整，不以原材料默认数字或源码字符串代替实收。

| 证据 | 实际结果与阶段 |
| --- | --- |
| `08-active-red.log` | 原生产无泵行为RED：24项，原23过、新SC例失败；合法成型及合格6000mB存量，实收0。自然三维保存后exit1 |
| `09-receipt-junit.log`、`.xml` | 定向BoilerDualSteamInventoryTest 13/13，包含新增3例；exit0。此后收据生产未变，不重复运行 |
| `10-active-green-attempt.log` | 初33/33（原23+新10），自然保存exit0 |
| `11-active-final-green.log` | 源口BE身份补核后34/34（原23+新11），自然保存exit0 |
| `12-active-assemble.log`、`13-final-freeze/**` | 首次必要增量assemble 3秒exit0；3248124B/e93c932a…初包与全冻结保留 |
| `14-fairness-red.log` | 同次审查RED：35项仅公平新例失败；12帧A24576/B0/泵24576，每帧4096、有限总量100000，源/目标余量始终足够。自然保存exit1 |
| `15-fairness-green.log` | 来源组整改35/35（原23+新12）；12帧A12288/B12288/泵24576，每帧4096、总量100000守恒。自然三维保存exit0 |
| `16-fairness-corrective-assemble.log`、`17-final-freeze/**` | 审查来源组整改触发第二次必要纠错增量assemble，exit0；3248290B/8c67e5d7…中间包及全冻结保留 |
| `18-target-rotation-red.log` | 36项仅新目标轮转例失败；本源6个交付帧北24576/西0，泵帧单列排除，每帧4096、总量100000且两个目标始终有空间。自然保存exit1 |
| `19-target-rotation-green.log` | 最终36/36（原23+新13）；本源6个交付帧北/西各12288，泵实流0才计此源。来源公平仍A/B各12288、泵24576，所有帧4096且守恒。自然保存exit0 |
| `20-target-rotation-corrective-assemble.log`、`21-final-freeze/**` | 同次Important目标侧修正触发第三次必要纠错增量assemble，exit0；最终全冻结 |

测试筛选域实际为 `create_nuclear_industry_industrial`：锁定NF按templateNamespace筛选，BeforeBatch在放模板之后，按PM后备组合原代表和新增例，不冒称独立新namespace注册。隔离运行目录是本报告内gametest-world，未加载用户世界。

新增13例集中证明NORMAL/SC各自无泵有限交付、当前15节点四SC口至真实汽轮机（源67840+周转216+真实普通汽排出1944=70000）、同tick标准抽取1024+主动/泵路径3072共4096、四口共8192节点、真实普通罐执行短收1024并换过滤后原SC4976/准确HU与口额度3072、停阀/断管/端口拆换恢复、同状态汽口BE替换旧请求零取、双池/旧目标介质拒混及恢复、普通原生路线与SC原生禁运、连续优先轮次来源公平与本源两个目标分别实收。

公平例单独批次，仅在隔离测试World的HIGH Post剔除已完成旧夹具登记，实核两个Controller自然发布请求与一台真实动力泵；不伪造主动身份/授权、不加生产API。合法50汽格锅炉60000、泵有限罐40000、目标有限64000，经强化阀50%真实4096瓶颈观测12帧；库存低于安全阀开启线，无补液掩盖。LOWEST读同gameTime实际口Budgets、泵actualFlow及源/目标/cache总量。这是合法真实测试场景，不称原用户世界公平性复现。

## 同次审查整改

同次Important含直接关联的两层计数耦合。第一层原cursor既决定两组先后奇偶又决定组内首位，两汽口+一泵共享瓶颈时B永久0。14日志实收复现后，只改Scheduler及新真实类：每组自己的首位只在该组实际先行时推进，对方为空也算先行，不能每tick同速递增。15日志证明双方汽口和泵均获正实收。

同次差异复核又指出目标起点仍取全局cursor：单汽源+一泵共享瓶颈时，本源有额度的奇偶固定，自己的首收目标偏置。18日志严格仅在本口Used>0且泵actualFlow=0的六个交付帧统计两罐，证明本源北24576/西0；没有把泵给另一罐的交付称为全网零实收。修正为每主动物理口临时目标起点，只有本口本轮真实交付>0才推进；无交付不耗轮次，源/路线/区块/配置失效清理。19证明本源北/西分别12288，两个目标持续有空间，原来源组公平仍成立。目标例仍使用合法60000/40000有限源及双64000有限罐、同一4096阀和十二帧守恒；没有临时namespace、复制模板或新增框架。初e93/34、8c67/35与全部RED保留。原审查者同一次最终差异复核已关闭Important，无剩余必改；PM独立核对21冻结的2078来源和2230正式JAR载荷与当前候选均零漂移。

## 最终身份与完整冻结

正式候选 `build/libs/create_nuclear_industry-0.1.0.jar`：**3248823字节**，SHA256 **`f23b5cf49a3bc6ec464f14efb722af1c65067658a51b378e336379be0bab0f57`**。原3239779B/86225aea…包备份为 `original-86225.jar`，初e93包在13冻结、8c67包在17冻结。三次增量assemble均由同一执行者运行；后两次是同次审查实际生产修正所需，不记为“唯一一次打包”。未运行全工程build、管理员或美术矩阵。

`21-final-freeze/summary.json`、`all-source-manifest.json`、`all-sources.zip`、`jar-entry-manifest.json`及冻结JAR绑定：全src与三个构建入口2078文件，正式输出681class+1548资源+生成Manifest，共2230个独立JAR文件entry，零缺失/字节差异/重复entry；新测试主类及匿名内部类均排除，零泄漏。相对原86225只有四个生产类型组相关class改变/新增，资源全部保持。原只读9输入的SHA、大小、UTC mtime全部不变；没有修改用户世界/运行配置，没有动main或02冻结，没有Git写。

## 实际技能与未关闭门

实际读取本树AGENTS、最新窄修卡，并使用minecraft-modding/testing、systematic-debugging、brainstorming、test-driven-development、receiving-code-review、verification-before-completion；栈固定MC1.21.1/Java21/NF21.1.219/Create6.0.10-280。所有新增手写注释为中文。

本批没有新建SC邻罐直推、其他设备无泵、管网压力/泄漏/破裂或玩法平衡。原23工业代表包含真实区块卸载/阀二阶边界，新增主动源覆盖实体/结构失效，不宣称新增主动口长距离卸载另有专项用例。未运行的管理员/闭环/美术矩阵不称通过。原02闭环集中人工门及本批现有无泵管路局部手测均保持待验，尚未合main或用户验收。
