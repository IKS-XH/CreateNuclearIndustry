# 设备服务端配置

> **版本边界：** 三档结构、256RPM、01E共享双轴、01F流量效率/微周转、01G停机及冷凝配置已完成现行联合验收并合入main，见[联合验收](./reviews/2026-10-05/condense-01/ACCEPTANCE.md)。已有实例的显式设置不会因代码默认值改变而自动覆盖；主目录与候选各使用自己的运行配置。

三设备配置化随[01A](./superpowers/plans/2026-10-04-ext-b-turbine-01a.md)实施并经后续各批接续，当前数值与行为按下表及SERVER配置执行。数值调整由服务器决定，客户端护目镜读取服务端同步结果。

## 文件位置与修改方式

配置类型为 NeoForge SERVER。当前锁定的 NeoForge 21.1.219 默认在实例的 `config/` 目录生成；世界的 `serverconfig/` 中已有同名文件时，该世界优先使用覆盖文件：

- `create_nuclear_industry-boiler.toml`：高压锅炉。
- `create_nuclear_industry-heat-exchanger.toml`：核换热器，保留原文件和已有键。
- `create_nuclear_industry-turbine.toml`：三档汽轮机。
- `create_nuclear_industry-storage.toml`：轻量封存候选中的干式贮存架，尚待联合手测。

开发候选默认目录为 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/run/config/`；主目录启动的客户端使用主目录自己的 `run/config/`。如需仅对某个单人世界生效，可将相应文件复制到 `run/saves/<世界名>/serverconfig/` 再修改；已有覆盖文件会优先于实例配置。不要混淆两套运行目录。

退出世界或关闭服务器，修改当前实际生效的 TOML，重新进入或启动后核对护目镜。本文不承诺在线热重载，也不把旧版Forge默认的存档路径说明当作当前版本行为。路径依据为锁定版 `ServerLifecycleHooks.handleServerAboutToStart` 与 `ConfigTracker.resolveBasePath`，并由本批隔离服务端实际生成文件核实。

## 锅炉

文件：`create_nuclear_industry-boiler.toml`。所有键在文件顶层。

| 键 | 默认 | 含义 |
| :--- | ---: | :--- |
| `waterCapacityMb` | 16000 | 整炉水容量，mB |
| `steamCapacityMb` | 16000 | 整炉超临界蒸汽容量，mB |
| `portFlowMbPerTick` | 256 | 每个物理给水口、汽口独立限流，mB/t |
| `sectionHeatHuPerTick` | 18 | 每个换热段最高收热，HU/t |
| `steamHuPerMb` | 1 | 每mB超临界蒸汽的产汽耗热，HU/mB |
| `warmHuPerSection` | 3600 | 每个换热段对应暖炉所需热量，HU |
| `coolingHuPerSectionPerTick` | 0.9 | 未收热时每段每tick散热，HU/t |
| `reheatFraction` | 0.25 | 低于暖炉上限此比例时重新预热 |
| `valveOpenFraction` | 0.9 | 安全阀开启汽量比例 |
| `valveCloseFraction` | 0.8 | 安全阀关闭汽量比例，必须小于开启值 |
| `valveFlowMbPerTick` | 256 | 安全阀每tick最多排汽，mB/t |

端口主动输出与外部抽取共用该口限流；增加端口不增加水汽容量。固定5×5×5、最多9个换热段仍是当前结构合同，可变尺寸与随规模计算容量另批实现。安全阀排放仍会消耗工质。

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

**冷凝增量（2026-10-05，已通过联合手测并合入main）：** [冷凝回水01](./superpowers/plans/2026-10-05-ext-b-condense-01.md)在同一文件顶层追加下表，不改变现有核热字段的含义。默认8字段及非默认回收率/容量已由真实SERVER断言核对，证据见[候选交付](./reviews/2026-10-05/condense-01/README.md)；最终运行验收见[联合验收](./reviews/2026-10-05/condense-01/ACCEPTANCE.md)。

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

**01F修订（2026-10-05，已通过联合手测并合入main）：** 用户已确认三档最高倍率1.2/1.5/1.8、大型额定耗汽216mB/t、最低倍率0.5及30%启动门槛，并取消大储罐、保留极小周转缓存。以下表格记录[01F](./superpowers/plans/2026-10-05-ext-b-turbine-01f.md)字段与默认值；部署和验证结果见[本批交付](./reviews/2026-10-05/turbine-01f/README.md)，最终运行验收见[联合验收](./reviews/2026-10-05/condense-01/ACCEPTANCE.md)。

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

2026-10-05用户确认[01E共享容量](./superpowers/plans/2026-10-05-ext-b-turbine-01e.md)，当前已验收实现：完整汽轮机两端内部贯通，同属一个Create动力网；只接任意一端即可使用全部容量，同时接出合计负载、共同过载，不翻倍总SU。现有`frontShare`标记为废弃，不再生效；红石立即停机，断汽沿用40tick窗口衰减归零，均不抹除同网外部动力源。

设平均排汽流量`q`、额定流量`R`、工作比例`x=clamp(q/R,0,1)`、门槛`t`、最低倍率`m`、本档最高倍率`M`。当`x<t`时本机SU为0；否则倍率为`m+(M-m)*(x-t)/(1-t)`，总SU为`q*suPerMbPerTick*倍率`。不将平均流量取整；默认三档启动门槛16.2/32.4/64.8mB/t。

旧`InputCapacityMb`、`ExhaustCapacityMb`字段退出汽轮机配置。入口接受超临界蒸汽即等量转入周转空间，只有真实排汽才计流量；SIMULATE不占量、不计功。堵塞时最多填满剩余周转空间，满后拒收，不丢弃残留。当前版本保存周转残留，但不保存动力历史；重载后重新积累流量。红石停机或结构失效立即撤销本机动力；断流/堵塞沿窗口衰减并在低于门槛时归零。排汽为等体积蒸汽，不能再次输入汽轮机发电。


## 乏燃料贮存架

文件：`create_nuclear_industry-storage.toml`，NeoForge SERVER；本批候选已完成必要自动检查，客户端联合手测待用户完成，见[STORE-01卡](./superpowers/plans/2026-10-06-ext-a-store-01.md)。

| 键 | 默认 | 含义 |
| :--- | ---: | :--- |
| `spentFuelStorage.rackSlots` | 16 | 每架可插入的单件封装桶位，范围1～16 |

降低容量会保留全部已有桶，并允许从全部原槽取出；新插入同时受有效槽范围和全架总桶数限制，已用数达到或超过新容量时全架拒收。四档外观仅表示占用比例，护目镜显示实际桶数和当前容量。修改生效方式沿用本文的SERVER路径与重进步骤。

玻璃碎料、固化基材、制桶和造架的数量/工时，以及屏蔽装配台的两种工序均由本批数据配方控制。封存默认12800 RPM·tick，64RPM下约10秒；原新燃料仍25600 RPM·tick，约20秒。架子容量配置不改变配方、单桶唯一载荷或输入输出方向。

## 存量与合法性

容量调小不直接删除已存液体，超过新上限的罐拒绝新增输入，原库存仍可处理或排出。安全阀等正常运行规则继续有效。修改换算系数、余热或平滑窗口时，不可复用的过程尾差会清除，不按新参数放大已支付的热量或动力。

TOML注释记录单位和数值范围；跨字段组合无效时机器停机并显示配置错误。配方仍为数据包JSON，材料数量通过配方覆盖调整。注册ID、核热冷热流体等体积转换、坐标算法和只支持3/5/7外径的网格集合属于结构规则，不作为平衡开关；冷凝回收率按上述独立字段配置。
