# 创造模式拆残骸修复：阶段A执行报告

2026-10-22，事故树`E:/MyMC/NewMod/Create_NuclearIndustry-accident`，分支`codex/accident-thermal-04b`，基准`afb16899972cbe1495e31857dd2d3b75286ada59`。执行完成，交PM窄审/提交；未打JAR、未Git写、不宣布main合入或客户端验收。

已读主目录AGENTS、治理5.1/5.2/6、STATUS、活动卡及04B候选/EXECUTION。实际使用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`核锁定拆除入口，`minecraft-testing/SKILL.md`复用已有真实GameTest域/模板/短focus；MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6及当前事故树构建规则保持。未扩及main近期美术或许可变更。

## 根因、调用链与准确改动

原`ReactorCoriumBlock.onRemove`对所有原生替换都调用BE的`take()`并`popResource()`，绕过创造模式不执行普通loot的分支。因此真实创造拆除仍掉出唯一实载荷废料。

实读本机NeoForge21.1.219对应`sourcesWithNeoForge_581a5c08...zip`，以及同版本f5a61d36源；元数据明确注入21.1.219 sources.jar。原文/行号留`locked-api.txt`：`ServerPlayerGameMode.destroyBlock`先`CommonHooks.fireBlockBreak`，最终取消立即返回，再核权限，随后调用`playerWillDestroy`；创造分支之后才经`onDestroyedByPlayer`→`Level.removeBlock`→`onRemove`。本模组既有`ReactorBreakFinalizationMixin`在事件post返回边界完成最终结果，未修改该入口。

精确只改两文件，清单见`paths.json`，源SHA/编译身份见`SOURCE.json`，完整差异见`source.diff`：

- `src/main/java/com/iksxh/create_nuclear_industry/accident/ReactorCoriumBlock.java`：增加Player import及`playerWillDestroy`覆盖，共6行新增。只在服务端创造玩家路径调用既有`take()`，丢弃其返回值，再调用super；取消/权限失败尚未进入该钩子，库存不预清。后续原`onRemove`只见空库存，不产物。
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/AccidentExecutionGameTests.java`：增加56行，复用原`cni_accident_execution`/`accident_empty`/`CNI_ACCIDENT_FOCUS`，一个短业务夹具覆盖六情形及后续索引撤销。冷态夹具复用真实凝固交接，再进入既有冷却终态，不重等6000/2400tick。

BE无需修改：现有`take()`已清唯一payload并调用`removeSolid`结束热引用，随后原调度撤HOT活跃位置。BE源保持，编译class亦与原04B候选逐字节一致。生存及环境路径继续使用原`onRemove`回收实际一份；没有修改材料数、普通容器、熔融本体、辐射/伤害/运输或管理员拆除。其余2317个源/规则输入保持，`git diff --check -- src`成功，工作区仅上述两源变化。

## 必要真实服证据

两次均用现有`runGameTestServer`，`-PgameTestNamespace=cni_accident_execution`、`CNI_ACCIDENT_FOCUS=corium_removal`。独立gameDirectory分别在本报告的`server-red`和`server-green`，不读用户世界。原始命令/时段/退出码见对应exit JSON，服务端原日志另保存为`*-server-latest.log`。

| 运行 | 实际业务结果 | SKIP | 退出与保存 |
| --- | --- | ---: | --- |
| `01-red.log`，原生产源，仅先加测试 | 1业务失败：真实FakePlayer的`ServerPlayerGameMode.destroyBlock`，cold实载荷残骸`destroyed=true,drops=1,payload=0,heatActive=false` | 17显式 | Java1/Gradle1，30秒；全部维度正常保存并退出 |
| `02-green.log`，最小修复后唯一集中GREEN | 1业务通过；下列六情形及HOT索引检查均执行 | 17显式 | Java0/Gradle0，29秒；全部维度正常保存并退出 |

GREEN实际观测：创造冷态、HOT实载荷及空残骸（含HOT外观）三次均`destroyed=true,drops=0,payload=0,heatActive=false`。最终LOWEST取消经真实destroyBlock返回false，BE对象/具名原payload/热剩余保持且无物品，后续tick仍保料/活跃。生存持铁镐原生拆除与环境`Level.destroyBlock(...,true)`分别恰好产生原具名实际一份，BE及热引用清空；重复环境删除不另造物。两tick后已拆状态不再活跃，HOT位置索引均撤销。

原GameTest日志的“18 required tests passed”包含17项显式SKIP，不能称18个业务实测。两次初建隔离目录的server.properties缺失日志由原生初始化后自动生成，不是业务失败根因；RED明确为实际掉落断言失败。旧弃用/开发环境警告原样保留，未借机改版本或规则。

## 保护与后续边界

运行前按原字节备份跟踪81E JAR（3478572bytes，SHA256 `81e7cd98d589ddc9c0aaf398c06776dc4318ea54c70cd19376f0c127e4a3de7b`）及两份B094根日志到`before/`，见`BEFORE.json`。运行后这三份当前文件仍与备份逐字节一致，无需执行者恢复。原04B候选仍3589016bytes、SHA256 `0f950a1b7379f0d017339dff94a0747445f9076857265f3cbc99d68c70be08c9`。

未新建分支、搬世界、打开原用户世界、启动用户客户端、跑全JUnit或旧04B长封存/冷却矩阵，未改Gradle/资源/核心文档/其他树。原04B其余手测反馈与已有证据复用；本轮客户端只待复看创造拆残骸这一修复，未补记物理卸载/真人多人。当前停在一次窄审/源码提交门前，待PM后续阶段B授权再唯一增量打包。
