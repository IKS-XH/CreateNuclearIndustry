# 热端与汽轮机服务端配置

本批配置化随 [EXT-B-TURBINE-01A](./superpowers/plans/2026-10-04-ext-b-turbine-01a.md) 实施，定向自动验证已通过，当前为[待人工复测候选](./reviews/2026-10-04/turbine-01a/README.md)。默认值沿用已批准方案；数值调整由服务器决定，客户端护目镜读取服务端同步结果。

## 文件位置与修改方式

配置类型为 NeoForge SERVER。当前锁定的 NeoForge 21.1.219 默认在实例的 `config/` 目录生成；世界的 `serverconfig/` 中已有同名文件时，该世界优先使用覆盖文件：

- `create_nuclear_industry-boiler.toml`：高压锅炉。
- `create_nuclear_industry-heat-exchanger.toml`：核换热器，保留原文件和已有键。
- `create_nuclear_industry-turbine.toml`：三档汽轮机。

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

## 汽轮机

文件：`create_nuclear_industry-turbine.toml`。三档字段使用 `short`、`medium`、`long` 前缀，例如 `shortRotorCount`。

| 键后缀 | 短/中/长默认 | 含义 |
| :--- | :--- | :--- |
| `RotorCount` | 3 / 6 / 9 | 内部转子节数，轴向总长度为节数＋2 |
| `RateMbPerTick` | 54 / 108 / 162 | 实际最高加工量，mB/t |
| `InputCapacityMb` | 4000 / 8000 / 12000 | 超临界入口容量，mB |
| `ExhaustCapacityMb` | 4000 / 8000 / 12000 | 普通蒸汽排汽容量，mB |

| 全局键 | 默认 | 含义 |
| :--- | ---: | :--- |
| `rpm` | 128 | 工作转速，不能超过当前Create服务器配置上限 |
| `suPerMbPerTick` | 32768 | 每mB/t实际平均流量对应的总SU |
| `smoothingTicks` | 40 | 实际加工量的移动平均窗口，tick |
| `inletPortFlowMbPerTick` | 256 | 每个物理进汽口限流，mB/t |
| `exhaustPortFlowMbPerTick` | 256 | 每个物理排汽口限流，mB/t |
| `frontShare` | 0.5 | 前轴份额；后轴由1减此值派生 |

只允许配置中的三种有效且不同的转子数，截面始终为3×3；不能用配方或额外端口突破所选档位的处理率。单端未连接不会把份额转给另一端。修改转子数会改变合法搭建长度，既有结构应按新规格重新核验。

总SU＝最近窗口内实际处理mB之和÷窗口tick数×转换系数。历史不写存档，加载后重新累积；停机或结构失效立即清除本机动力。排汽仍是等体积普通蒸汽，不能再次输入汽轮机发电。


## 存量与合法性

容量调小不直接删除已存液体，超过新上限的罐拒绝新增输入，原库存仍可处理或排出。安全阀等正常运行规则继续有效。修改换算系数、余热或平滑窗口时，不可复用的过程尾差会清除，不按新参数放大已支付的热量或动力。

TOML注释记录单位和数值范围；跨字段组合无效时机器停机并显示配置错误。配方仍为数据包JSON，材料数量通过配方覆盖调整。注册ID、1:1流体守恒、坐标算法和固定截面属于结构规则，不作为平衡开关。
