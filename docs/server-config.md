# 设备服务端配置

> **版本边界：** 三档结构、256RPM、01E共享双轴、01F流量效率/微周转、01G停机及冷凝配置已完成现行联合验收并合入main，见[联合验收](./reviews/2026-10-05/condense-01/ACCEPTANCE.md)。已有实例的显式设置不会因代码默认值改变而自动覆盖；主目录与候选各使用自己的运行配置。

三设备配置化随[01A](archive/2026-10-08-completed-plans/2026-10-04-ext-b-turbine-01a.md)实施并经后续各批接续，当前数值与行为按下表及SERVER配置执行。数值调整由服务器决定，客户端护目镜读取服务端同步结果。

**反应堆总产热（2026-10-06确认）：** [总产热取整任务](archive/2026-10-08-completed-plans/2026-10-06-reactor-heat-rounding-01.md)将新生裂变热汇总并限幅后向上取整为整数HU/t，冷却与遥测共用。沿用现有产热和吸热配置，不新增平衡开关；默认0.5HU/mB时100HU/t对应200mB/t。自定义吸热系数或变化的运行工况仍按实际账本结算，不承诺所有管路与负载都无波动；缓存余热不再取整。本项已在[封存/配方/取整联合验收](./reviews/2026-10-06/store-01/ACCEPTANCE.md)通过并合入main。

## 文件位置与修改方式

配置类型为 NeoForge SERVER。当前锁定的 NeoForge 21.1.219 默认在实例的 `config/` 目录生成；世界的 `serverconfig/` 中已有同名文件时，该世界优先使用覆盖文件：

- `create_nuclear_industry-boiler.toml`：高压锅炉。
- `create_nuclear_industry-heat-exchanger.toml`：核换热器，保留原文件和已有键。
- `create_nuclear_industry-turbine.toml`：三档汽轮机。
- `create_nuclear_industry-storage.toml`：已验收的轻量封存与干式贮存架。

开发候选默认目录为 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/run/config/`；主目录启动的客户端使用主目录自己的 `run/config/`。如需仅对某个单人世界生效，可将相应文件复制到 `run/saves/<世界名>/serverconfig/` 再修改；已有覆盖文件会优先于实例配置。不要混淆两套运行目录。

退出世界或关闭服务器，修改当前实际生效的 TOML，重新进入或启动后核对护目镜。本文不承诺在线热重载，也不把旧版Forge默认的存档路径说明当作当前版本行为。路径依据为锁定版 `ServerLifecycleHooks.handleServerAboutToStart` 与 `ConfigTracker.resolveBasePath`，并由本批隔离服务端实际生成文件核实。

## 锅炉

**01F现行配置（2026-10-08已验收合入main）：** [双汽库存](archive/2026-10-08-completed-plans/2026-10-07-boiler-dual-steam-inventory-01f.md)替代下文01B/01D整炉瞬时切种规则。两种汽各自保存真实mB/HU，共用`steamCapacityPerCellMb`计算的一份总容量；新批次按实际温压归类，库存不自动换种，各口只取所选库存。`supercriticalPressure`用于新批次定种，已有SC仍须已付热合格，低于该生产门槛可排至公共出汽压力下限。配置键与默认数值不增加、不改变，见[验收](./reviews/2026-10-08/boiler-rework-01/ACCEPTANCE.md)。

**锅炉版本边界：** [内置换热、分区、可变尺寸及温压](archive/2026-10-08-completed-plans/2026-10-07-high-pressure-boiler-rework-proposal.md)和01A～01F整改已于2026-10-08验收合入main。下列字段适用于新版锅炉，主目录runClient已包含实现；此前固定锅炉数值仅保留为[历史合同](archive/2026-10-08-completed-plans/2026-10-04-ext-b-turbine-01a.md)。主目录与同级候选仍分别读取自己的运行配置。

文件：`create_nuclear_industry-boiler.toml`。所有键在文件顶层。

| 键 | 默认 | 含义 |
| :--- | ---: | :--- |
| `dimensionRange` | `[5, 11]` | 三边分别允许的整数闭区间；恰好两值，5≤min≤max≤32 |
| `waterCapacityPerCellMb` / `steamCapacityPerCellMb` | 各2000 | 每个有效水区/汽区格的mB容量；01F两种汽共用汽区总容量 |
| `portFlowMbPerTick` | 256 | 每个水、汽、热液、冷液口的独立mB/t额度，主动/被动共用 |
| `pairHeatHuPerTick` | 18 | 每有效换热器/再加热段配对的HU/t上限 |
| `boilingTemperature` / `supercriticalTemperature` | 1 / 2 | 归一游戏温度，后者须大于前者 |
| `wallHeatCapacityHuPerWaterCell` | 1600 | 每个水区格提供的炉壁热容，HU/温升 |
| `waterSpecificHeatHuPerMb` / `steamSpecificHeatHuPerMb` | 0.1 / 0.2 | 水/汽比热，HU/mB/温升 |
| `vaporizationLatentHeatHuPerMb` | 0.7 | 汽化追加潜热，HU/mB |
| `supercriticalPressure` | 0.5 | 01F新增批次归为超临界汽的归一炉压门槛；不重标已有库存 |
| `outputMinPressure` | 0.6 | 默认出汽压力下限，合法范围[0,1]，控件按0～100%逐整数设置 |
| `valveOpenPressure` / `valveClosePressure` | 0.9 / 0.8 | 阀开/关炉压，开启线也用于堵阀保护 |
| `valveFlowPerSteamCellMbPerTick` | 32 | 每汽区格提供的泄放速率，mB/t |
| `idleWaterCoolingHuPerCellPerTick` | 0.9 | 无实际收热时每水区格显热损失，HU/t |
| `idleSteamCoolingHuPerCellPerTick` | 0.1 | 无实际收热时每汽区格高于沸点的显热损失，HU/t |

冷热库存容量读取换热器`hotCapacityMb`/`coldCapacityMb`并乘炉内换热器数，实际热功率还受换热器额定上限约束；不新增重复字段。完整隔层上下空气格分别决定容量，有效配对取换热器和再加热段数量的最小值，增加端口不扩容。配置合法性包括组合关系及long乘积预检；32为工程扫描封顶，不代表最大规模已完成客户端性能验收。

控制器沿用01B单一出汽压力下限，显式机器设置独立保存，不由默认配置覆盖。01F新批次按实际批次汽温及加量后的共同炉压归类，已有两种库存不变身份；炉压由两种汽量和各自实际汽温共同决定，与Create运输压力分开。压力下限可设100%，可能因安全阀先泄放而停止正常出汽；它不是目标炉压，也不强制产生普通汽。默认5³居中隔层上下各9格、水汽各18000mB，汽容量由两种共用；全部汽温为2时，下限0.6对应保留10800mB。冷水至沸点汽共0.8HU/mB、至目标SC汽共1HU/mB；已有汽再热和输出HU均实际扣账，普通汽不因再热变SC，欠热SC等待再热。散热不扣潜热、不模拟炉内凝水，安全阀排放仍消耗工质和热量。

历史配置键说明见已归档的重构合同，当前仅使用上表键；本次不删除用户配置或世界。

**汽口选择（01F现行）：** 沿用01D原生选项“蒸汽 / 超临界蒸汽”，默认超临界，各口独立保存。每个口只抽所选的真实库存，无对应库存、欠热或无保压余量时等待；已有SC在低于生产炉压门槛后仍可按公共下限输出。两种可同时经各自独立管路交付，不降级、混装、转换或清除外部异种流体。控制器不增加汽种开关，所有配置数值保持原值。01D历史4项证据保留，本批25项账本/11个不同真实用例及独立审查见候选说明，客户端门已由2026-10-08集中验收关闭。

## 换热器

文件：`create_nuclear_industry-heat-exchanger.toml`。所有键在文件顶层。

| 键 | 默认 | 含义 |
| :--- | ---: | :--- |
| `heatLevel` | 18 | Create锅炉最高热等级，1～18 |
| `huPerLevel` | 1 | 每级每tick耗热，HU |
| `bufferTicks` | 40 | 储热/断流余热窗口，tick |
| `hotCapacityMb` | 4000 | 每台热液罐容量，mB |
| `coldCapacityMb` | 4000 | 每台冷液罐容量，mB |
| `maxLineLength` | 16 | 同向共享直列最多台数，1～64 |

额定热功率由 `heatLevel × huPerLevel` 派生，储热上限再乘 `bufferTicks`。专用锅炉实际收热同时受换热器额定热功率、锅炉每段收热上限及实时需求限制；只改一端不能越过另一端的上限。

工质密度继续使用原 P1 SERVER 配置中的 `coolantAbsorptionHuPerMb`（默认0.5HU/mB），不新增第二份密度。它同时影响反应堆冷却语义，不能当作只调整换热器的局部参数。

**冷凝增量（2026-10-05，已通过联合手测并合入main）：** [冷凝回水01](archive/2026-10-08-completed-plans/2026-10-05-ext-b-condense-01.md)在同一文件顶层追加下表，不改变现有核热字段的含义。默认8字段及非默认回收率/容量已由真实SERVER断言核对，证据见[候选交付](./reviews/2026-10-05/condense-01/README.md)；最终运行验收见[联合验收](./reviews/2026-10-05/condense-01/ACCEPTANCE.md)。

| 键 | 默认 | 含义 |
| :--- | ---: | :--- |
| `condensationRateMbPerTick` | 54 | 每台冷凝最高处理蒸汽量，mB/t |
| `condensationRecoveryPermille` | 1000 | 回水千分比，1～1000；默认1mB蒸汽产1mB水 |
| `condensationSteamCapacityMb` | 4000 | 每台冷凝输入蒸汽容量，mB |
| `condensationWaterCapacityMb` | 4000 | 每台冷凝输出水容量，mB |
| `condensationSnowMeltAfterMb` | 100000 | 雪块变水源前累计冷凝蒸汽量，mB |
| `condensationIceMeltAfterMb` | 100000 | 冰变水源前累计冷凝蒸汽量，mB |
| `condensationPackedIceMeltAfterMb` | 900000 | 浮冰变水源前累计冷凝蒸汽量，mB |
| `condensationWaterEvaporateAfterMb` | 100000 | 水源变空气前累计冷凝蒸汽量，mB |

只认可每台正上方相邻一格的水源、雪块、冰、浮冰或蓝冰；流水、含水方块和雪层无效。蓝冰持续作冷源；其他源按本机实际成功冷凝量消耗，暂停不计量。融化后水源阶段重新从零开始，单格水蒸发后按原版规则补水；冷源变化不增减机内回水。回收率小于1000时小数尾量按单机累计，不能用逐包取整产生额外损失。默认满流量、20TPS下100000mB约93秒。

## 汽轮机

**01F修订（2026-10-05，已通过联合手测并合入main）：** 用户已确认三档最高倍率1.2/1.5/1.8、大型额定耗汽216mB/t、最低倍率0.5及30%启动门槛，并取消大储罐、保留极小周转缓存。以下表格记录[01F](archive/2026-10-08-completed-plans/2026-10-05-ext-b-turbine-01f.md)字段与默认值；部署和验证结果见[本批交付](./reviews/2026-10-05/turbine-01f/README.md)，最终运行验收见[联合验收](./reviews/2026-10-05/condense-01/ACCEPTANCE.md)。

文件：`create_nuclear_industry-turbine.toml`。三档字段使用 `short`、`medium`、`long` 前缀，例如 `shortRotorCount`。

| 键后缀 | 短/中/长默认 | 含义 |
| :--- | :--- | :--- |
| `RotorCount` | 3 / 6 / 9 | 内部转子节数，轴向总长度为节数＋2 |
| `Diameter` | 3 / 5 / 7 | 外径，单位格；只支持已有网格3、5、7，其他值拒绝成型 |
| `RateMbPerTick` | 54 / 108 / 216 | 全机每tick最高进汽及排汽量，mB/t；所有端口合计受此限制 |
| `MaxEfficiencyMultiplier` | 1.2 / 1.5 / 1.8 | 满流量时本档最高效率倍率 |

| 全局键 | 默认 | 含义 |
| :--- | ---: | :--- |
| `rpm` | 256 | 工作转速，不能超过当前Create服务器配置上限 |
| `suPerMbPerTick` | 32768 | 每mB/t实际平均排汽量对应的基准SU，计算时另乘效率倍率 |
| `smoothingTicks` | 40 | 实际排汽量的移动平均窗口，tick |
| `turnoverTicks` | 1 | 全机唯一排汽周转量容量为本档RateMbPerTick乘此值，默认54/108/216mB |
| `minEfficiencyMultiplier` | 0.5 | 恰到工作流量门槛时的倍率，不能高于任一档最高倍率 |
| `minimumOperatingFlowRatio` | 0.30 | 平均实际排汽流量占额定值的最低工作比例；低于此值仍耗汽排汽，但无本机动力 |
| `inletPortFlowMbPerTick` | 256 | 每个物理进汽口限流，mB/t |
| `exhaustPortFlowMbPerTick` | 256 | 每个物理排汽口限流，mB/t |
| `frontShare` | 0.5 | 01E起已废弃，不再控制前后份额 |

只允许配置中的三档合法组合，转子数各不相同且在3～16之间，截面为对应`Diameter`的八边形。默认外尺寸为3×5×3、5×8×5、7×11×7（宽×轴长×高）。不能用配方或额外端口突破所选档位的处理率。修改转子数或直径会改变合法搭法，既有结构需按新规格重新核验。

2026-10-05用户确认[01E共享容量](archive/2026-10-08-completed-plans/2026-10-05-ext-b-turbine-01e.md)，当前已验收实现：完整汽轮机两端内部贯通，同属一个Create动力网；只接任意一端即可使用全部容量，同时接出合计负载、共同过载，不翻倍总SU。现有`frontShare`标记为废弃，不再生效；红石立即停机，断汽沿用40tick窗口衰减归零，均不抹除同网外部动力源。

设平均排汽流量`q`、额定流量`R`、工作比例`x=clamp(q/R,0,1)`、门槛`t`、最低倍率`m`、本档最高倍率`M`。当`x<t`时本机SU为0；否则倍率为`m+(M-m)*(x-t)/(1-t)`，总SU为`q*suPerMbPerTick*倍率`。不将平均流量取整；默认三档启动门槛16.2/32.4/64.8mB/t。

旧`InputCapacityMb`、`ExhaustCapacityMb`字段退出汽轮机配置。入口接受超临界蒸汽即等量转入周转空间，只有真实排汽才计流量；SIMULATE不占量、不计功。堵塞时最多填满剩余周转空间，满后拒收，不丢弃残留。当前版本保存周转残留，但不保存动力历史；重载后重新积累流量。红石停机或结构失效立即撤销本机动力；断流/堵塞沿窗口衰减并在低于门槛时归零。排汽为等体积蒸汽，不能再次输入汽轮机发电。


## 乏燃料贮存架

文件：`create_nuclear_industry-storage.toml`，NeoForge SERVER；轻量封存已[联合验收合入main](./reviews/2026-10-06/store-01/ACCEPTANCE.md)，实施历史见[STORE-01卡](archive/2026-10-08-completed-plans/2026-10-06-ext-a-store-01.md)。

| 键 | 默认 | 含义 |
| :--- | ---: | :--- |
| `spentFuelStorage.rackSlots` | 16 | 每架可插入的单件封装桶位，范围1～16 |

降低容量会保留全部已有桶，并允许从全部原槽取出；新插入同时受有效槽范围和全架总桶数限制，已用数达到或超过新容量时全架拒收。四档外观仅表示占用比例，护目镜显示实际桶数和当前容量。修改生效方式沿用本文的SERVER路径与重进步骤。

玻璃碎料、固化基材、制桶和造架的数量/工时，以及屏蔽装配台的两种工序均由本批数据配方控制。封存默认12800 RPM·tick，64RPM下约10秒；原新燃料仍25600 RPM·tick，约20秒。架子容量配置不改变配方、单桶唯一载荷或输入输出方向。

## 存量与合法性

容量调小不直接删除已存液体，超过新上限的罐拒绝新增输入，原库存仍可处理或排出。安全阀等正常运行规则继续有效。修改换算系数、余热或平滑窗口时，不可复用的过程尾差会清除，不按新参数放大已支付的热量或动力。

TOML注释记录单位和数值范围；跨字段组合无效时机器停机并显示配置错误。配方仍为数据包JSON，材料数量通过配方覆盖调整。注册ID、核热冷热流体等体积转换、坐标算法和只支持3/5/7外径的网格集合属于结构规则，不作为平衡开关；冷凝回收率按上述独立字段配置。
