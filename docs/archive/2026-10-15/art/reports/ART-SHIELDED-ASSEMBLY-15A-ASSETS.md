# ART-SHIELDED-ASSEMBLY-15A 资产交付

2026-10-14，唯一资产执行者 `shielded15_assets` 在 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio` / `codex/art-studio` / `88b8fd289d49df90d7e3c655b6e81381ad01130f` 完成本批静态设计稿。本报告不登记选稿、正式安装或用户视觉验收。

已完整读取15A卡、美术树README、主目录最新AGENTS及治理1.2/5.1/5.2，实际应用 `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md` 的显式元素、面、材质槽和0..16 UV规则。锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，不改变技术栈。系统using-superpowers的子Agent段明确不适用于本派发实现；本批已批准设计和精简检查按任务卡沿用。

## 可消费来源

- `tools/art-assets/shielded-assembly-concepts-15/assets/concepts.json`：schemaVersion1，实际来源SHA、北向整机坐标、rig及三稿槽/模型路径；路径均相对assets。
- 每稿 `models/{a,b,c}/` 含 shell/left_arm/right_arm/fixture 四个真实JSON，无parent、无硬编码替代图形。
- 每稿 `textures/{a,b,c}/` 含lead/steel/machined/fastener/brass/window/lamp_off/lamp_on/deck九张16² SVG；共12 JSON、27 SVG。有限色板、crispEdges，仅rect，无滤镜；玻璃透明底和稀疏不透明反光，钢壳不透明。
- `make-assets.py` 仅生成本批候选和有限检查，不调用旧fuel_02e_assets.py，不导出PNG、不安装资源、不造静态输入轴。脚本以`-B`执行，没有新增`__pycache__`。

与预览执行者已直接确认槽名、路径、无parent、整机坐标不再偏移及`#lamp_off`替换`lamp_on`的静态状态灯对照。真实Create SHAFT_HALF及第三方来源由预览执行者只读处理。

## 三稿及几何约定

| 稿 | 形体区别 | 最终元素数 shell / left / right / fixture |
| :--- | :--- | :--- |
| A 厚壁屏蔽热室 | 中明度铅灰大板、厚退台横窗、层叠钢肩/内收铅顶和重底台 | 93 / 28 / 28 / 26 |
| B 铸钢装配工作站 | 明确铸钢立柱和上横梁、分开的双观察区/中央加强柱、较厚下座和侧筋 | 101 / 29 / 29 / 28 |
| C 模块检修舱 | 浅暖分段护板、四角梁、独立窗模块、凹接缝、双片顶盖和检修压盖 | 107 / 29 / 29 / 28 |

主材来自实际机械动力合成配方：铅板、钢板、两个Create机械手及辐射传感器；黄铜只用肩部/执行杆功能锁圈与原夹具锁圈。双臂含铸肩导轨、钢芯、功能锁圈、腕座和分开的夹指；中央夹具有升降座、锁圈、台面和双钳口。没有假装料、假加工循环或新接口提示。

三稿外壳统一0..32整机模型单位，即2×2×2格；part=x+2*z+4*y的16格偏移只属于原八part对照，候选四分件均不加偏移。朝向pivot始终x=z=8，底轴中心[8,0,8]；静止底台保留[6,0,6]..[10,4,10]开口，未挪到整机中心16。

原rig保持：左臂pivot[9,21,15]、idle[7,13,10]..[13,23,22]、+X行程0..1；右臂pivot[23,21,15]、idle[19,13,10]..[25,23,22]、-X行程0..1；夹具pivot[16,13,16]、idle[13,11,12]..[19,18,20]、+Y行程0..3。候选包络与原值一致；扫掠与静止机壳没有实体相交。原fixture底部y=11接触静止座属于合法接触。

所有面显式UV，长盒在16单位格线上切片但不输出切片内部面；实面跨度等于UV跨度，保持16 texel/格，不将整张16²图压进小螺栓。灯槽按真实1单位高面采样，off/on完整槽差异不会被错误UV裁掉。

## 本批有限证据

最终实际命令（0.8秒、exit0）：

```powershell
$env:PYTHONIOENCODING = 'utf-8'
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B 'tools/art-assets/shielded-assembly-concepts-15/make-assets.py'
```

原输出与身份在 `build/reports/art/ART-SHIELDED-ASSEMBLY-15A/assets/`：

- `final-output.txt`、`checks.json`：passed=true、errors=[]；12模型/27 SVG；引用、UV密度、几何/rig包络、通孔、扫掠、待机分件相交、选定窗内射线、工作灯前射线及外露同向共面检查通过。
- `source-protection-before.json`：395项原输入身份；最终protectedChanged=[]。覆盖实际装配台输入、旧fuel-02e、14A/14B工具/证据、现有离心机安装资源、旧__pycache__、日志及现有JAR，未扫描世界或旧全设备矩阵。
- `output-identity.json`：最终41项可消费文件（脚本+契约+12模型+27SVG）的SHA256与大小。
- `first/`：保留首轮模型/契约/脚本/原检查和身份。首轮有限检查通过后，写报告前源面复核识别了A顶片与铅顶共面、fixture前盖共面、A/B灯嵌在上梁中的具体风险。
- `refined-red/`：保留增强同向共面检查的中间RED原文；格线切片内面和已被相接实体遮住的内部接合面也被首版增强检查报告。最终仅修本批局部面：顶片实体分区、夹具前退盖、灯嵌槽、窗框紧固面；切片不输出内部面，外法向微移采样区分实体内部接合面，保留外露重叠检查。

窗内射线是有限几何线索；B中央柱会挡中心正交线，记录的夹具侧表面射线从左窗可达。三稿材质/窗口与机构实际可读性由负责人真实浏览和本轮组合窄审另记录，自动检查不替代GPU视觉或Minecraft客户端验收。

本批复用14B已通过的历史证据，未测试旧设备。没有Gradle/JUnit/GameTest/Minecraft、PNG磁盘输出/安装、游戏源码写入、Git写入、世界/配置写入或JAR重建。本人写集只含新assets、make-assets.py、本批assets证据及本报告。最终source/contract已经告知负责人和预览执行者ready并冻结，等待同批具体反馈。
