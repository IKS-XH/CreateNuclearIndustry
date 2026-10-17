# HEAT-PENDING-01 实现与验证证据

2026-10-17，PM依据唯一执行者的冻结交付整理。原因及不确定性见[诊断](DIAGNOSIS.md)，范围见[归档窄修卡](../../../archive/2026-10-17/plans/2026-10-17-heat-bridge-pending-01.md)，当前进度只见[STATUS](../../../STATUS.md)。

## 最小改动

生产只改`HeatExchangerBoilerBridge.java`：PENDING登记与取批使用同一短锁；锁内复制独立位置集合并清理原队列，锁外执行本轮refresh。回调中再次登记或另一线程登记的请求留给下一tick，不再修改正在遍历的集合，也不被末尾clear擦除。长期弱世界引用、原ACTIVE/LOADED机制、加载检查、不强载和实际供热算法保持。

既有`HeatExchangerBoilerBridgeTest.java`增加4项测试，使用真实Create储罐和ChunkEvent.Load、真实afterServerTick/refresh，局部世界边界夹具不启动存档。后台仅做预先准备的登记，世界读取断言JUnit主线程；不复制镜像队列。

## 实际运行记录

| 验证 | 结果 |
| --- | --- |
| 首次夹具尝试 | Create储罐容量配置未加载，1失败/exit1；保留，不算有效RED |
| 有效RED | 旧消费路径重入实际ChunkEvent.Load，afterServerTick:155抛同类CME；1失败/零跳过/exit1 |
| 定向GREEN | Bridge 13项＋State 21项，同次34/34、零失败/错误/跳过、exit0 |
| 一次增量assemble | exit0；未clean、未全build/全GameTest、未启动生产服 |
| 源码静态 | 两个授权文件git diff --check/exit0 |

新增覆盖同轮去重、已有请求一次处理、重入新位置/新世界次tick处理、不丢请求、空队列不重复、回调中另一线程登记可及时完成，以及原未加载区块门。原9项来源生命周期/并发和21项换热器状态用例保持。GREEN后只改测试loadDefaults一行中文职责注释，无语义或行号变化；按治理5.1复用证据，没有重复测试。

原始证据保留于`E:/MyMC/NewMod/Create_NuclearIndustry-render-accepted-integration/build/reports/heat-pending-20261017/`，REPORT.md索引red/green XML与日志、assemble.log、输入三报告SHA、源码冻结、原包差异和候选生成。Java/Gradle弃用提示、测试环境Ponder refmap及缺少ApiStatus.ScheduledForRemoval类警告均保留，没有改依赖处理这些警告。原始输入哈希交付前核未变。

## 来源与制品边界

执行基准`b8019db38d2c85251dd79c8eee36dd8406c9052b`，相对已提交正式来源仅本批管理文档；Gradle依赖保持锁定版本。组合窄审无发现后，仅两个授权源码提交为`8712f4310eb1ff6ac8f2af03fde33e4154a71325`。两授权文件和构建配置的冻结哈希见[SOURCE.json](SOURCE.json)，制品见[ARTIFACT.json](ARTIFACT.json)。

原assemble包3347852 bytes、SHA256 `D4B36429EF94871D0841E14DA478414E2BCDD378A937586912BB1045F9804A2B`，仅一项桥接class变化和1231项CRLF资源差异；资源统一换行后逐字相同。原包与失败过程留存，没有修改资源树或再次assemble。

依卡从明确已提交的baseline包B366EB49…取全部原载荷，仅替换本次assemble的`HeatExchangerBoilerBridge.class`，生成3336867 bytes窄候选，SHA256 `43E399BEBE8C895ECD290A8721CEAB73AF057F5D2413B39339CDD85F0204E54C`。总2516条目为238目录＋2278文件；2515其他条目包含238目录，实际2277其他文件载荷逐字保持，条目顺序及ZIP元数据保持，无新注册ID/网络协议/第三方/测试增量。组合审查独立核对了此口径与实际包。

窄包生成期间对Python zipfile默认改动UTF-8标记和零external_attr的断言失败均保留，最终显式保持原元数据后exit0。这是既有载荷与唯一新class的组合核验，不冒称另一次Gradle构建。

本批实际使用minecraft-modding/testing/ci-release、systematic-debugging、TDD及verification-before-completion，真实入口/应用见原REPORT。主目录世界、输入、客户端实例和生产服务器未改；候选携带世界27文件/0差异后未启动。main生产及正式包不在本候选交付中替换。原整合包多人区块加载和锅炉/换热器运行的持续观察另留人工门。
