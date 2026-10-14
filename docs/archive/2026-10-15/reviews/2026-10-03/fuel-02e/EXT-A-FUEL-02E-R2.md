# EXT-A-FUEL-02E-R2：屏蔽装配台共面闪烁修复

## 结果

已修复静态整机、完整物品模型和旋转削角端盖中的共面重绘。修复前，落盘世界模型几何扫描到70对同向同平面正面积重合；旋转削角面与静态支撑面检查另检出46处端盖交叠。修复后世界待机、三个活动件预留行程端点及完整物品模型的共面交叠计数均为0。此结果来自资源几何与离线深度预览，不代表用户GPU实测。

根因在模型生成方式：端口、灯区、黄铜边条和外壳按独立盒体逐个导出面，盒体相交时重合表面仍被重复绘制；45度削角棱柱的端盖又与底台/顶台在水平面相交。`design()` 中侧板接口与壳体共面、后接口与后墙共面、前灯与横梁共面均被修前扫描命中，黄铜条和上下壳体也有同类重叠。

修复只调整`fuel_02e_assets.py`生成算法与本设备模型JSON：同向同平面重叠区由后定义盒体负责，生成器仅在该重叠区域切分表面；削角保持原45度与x/z轮廓，只将y=0、4、28、32的端盖向内收`1/32`模型单位（`1/512`方块）。该偏移未触及y=16分件接面，旋转后各部件仍在合法单格坐标内。活动件分件、pivot、左右各1单位和夹具上移3单位预留不变。侧口保持齐平材质分区。

## 验证

- 修复前快照保存在`EXT-A-FUEL-02E-R2/baseline/`；包含原16个静态模型、3个partial、完整物品模型及113张PNG的SHA-256清单。修前轴对齐面扫描记录70对，削角端盖扫描记录46处。
- 运行`python -B tools/art-assets/fuel_02e_assets.py`成功。资源检查确认16个静态模型、3个partial、16个主状态变体、56个代理状态变体；静态/活动件体积碰撞、世界待机同面、活动件端点同面及物品同面计数均为0。part坐标、UV、运动范围检查通过。
- 正面、侧面等距、顶面、底面、完整物品和运动预留预览均已从落盘模型、PNG和实际深度生成，见`EXT-A-FUEL-02E-R2/previews/`。检查未见削角轮廓、封口、活动件或物品表现异常。
- 当前113张游戏PNG与生成前逐字节哈希完全相同。
- 资源冻结后仅执行一次`assemble --console=plain`（`JAVA_HOME=C:/Program Files/Java/jdk-21`）：`BUILD SUCCESSFUL`，4项任务中2项执行、2项UP-TO-DATE；未运行JUnit、GameTest、全量build或clean。JAR内16个part、3个partial及物品模型共20个JSON均与源码逐字节一致。

## 制品与边界

- JAR：`build/libs/create_nuclear_industry-0.1.0.jar`
- SHA-256：`136752F67DF5F4F283EB5A9CF22C8D305C02823E11A01EC1275C3BA010DF22E0`
- 面片元素数（静态off/on、3个partial和物品合计）：基线354，修复后1056。新增元素用于局部重合区域切分；PNG未改。
- 证据索引：`EXT-A-FUEL-02E-R2/geometry-check-summary.json`、`resource-check.json`、`jar-resource-check.json`、`baseline/`、`previews/`。
- 已复用用户2026-10-03确认的02E功能手测，以及02E报告中的5项GameTest和3项JUnit证据；本R2没有改玩法或状态算法，也未重跑功能测试。
- 离线预览与几何扫描不能证明不同GPU、着色器或视角下用户端的实际闪烁已消失，最终客户端画面仍由项目经理按既有人工边界处理。

技能实际使用：`minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`和`systematic-debugging`。本任务仅处理1.21.1资源模型，没有升级版本或改动Java/PNG/SVG/rig。
