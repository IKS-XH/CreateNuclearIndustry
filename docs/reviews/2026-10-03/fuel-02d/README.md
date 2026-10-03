# 燃料02D：屏蔽装配台与组件直接装配

**状态：候选已实现，自动验证与集中审查通过，等待四组客户端手测。** 用户已确认[完整方案](../../../superpowers/plans/2026-10-03-shielded-assembly-station-proposal.md)，范围以[02D实施卡](../../../superpowers/plans/2026-10-03-ext-a-fuel-02d.md)为准：8烧结芯块＋4包壳管＋2焊料＋1钢格架制1满耐久组件。生产机保持无GUI，不改变既有反应堆和燃料寿命。

实现提交`3f7b207`，分支`codex/ore-acquisition`。main本轮只更新文档与证据，功能保留在候选；未推送或发布。构建自动写入的两条根目录日志已先保留到候选build证据目录，再由PM恢复基线，不夹带到提交。

## 启动位置与四组手测

完整退出旧客户端，从同级候选目录启动；人工通过前不合入main，直接在主目录启动还不会出现本批装配台：

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
.\gradlew.bat runClient
```

| 组别 | 操作与预期 |
| :--- | :--- |
| 制造、JEI、外观 | 工作台`铅钢铅/钢机械手钢/铅辐射传感器铅`产1台；JEI显示4料数量及工序。放置/旋转/待机与工作状态、物品栏与手持均完整立体，大小接近整格，无接缝、紫黑或超大模型；右键不打开GUI。 |
| 一批与库存边界 | 手动投入8芯块/4包壳/2焊料/1格架，64RPM持续加工约20秒产1满耐久组件，准确扣料；错误物品和已有组件不能回灌。成品未取出时暂停，下批原料不被吞掉；潜行空手按固定槽序退料，退至不足一批时清进度。 |
| 动力与物流 | 顶部混合输入四料，任一侧漏斗取成品到底座/置物台；原生机械臂可对机器投料并从外置置物台把组件转运至既有换料端口。停轴、低于32RPM或过载暂停，恢复后续作；128RPM约10秒一批，护目镜显示库存、进度与等待原因。 |
| 保存与下游 | 带未完成进度/库存退出再进，普通挖掘或潜行扳手收起后重放，物品和合法进度保留且只掉1台，不另掉一份库存。成品可装入现有合法、已隔离的换料端口，显示100%剩余耐久。 |

时间以20TPS为参考，不重复旧机器全套回归。反应堆端口仍不接普通漏斗；本组只验新产物可接入原有合同，不要求重新验收整座反应堆。未实现的乏燃料/封存/辐射与后续冷却剂生产不计入本批。

## 证据

[运行报告](./EXT-A-FUEL-02D-runtime.md)、[素材报告](./EXT-A-FUEL-02D-assets.md)与[一次合并审查](./EXT-A-FUEL-02D-REVIEW.md)分别记录实现、实际检查及人工边界。[只读核对](./EXT-A-FUEL-02D-PROBE.md)保留为技术可行性依据。

- 定向JUnit **3/3**：[原始XML](./evidence/TEST-com.iksxh.create_nuclear_industry.production.ShieldedAssemblyStateTest.xml)。独立命名空间GameTest **2/2**、正常保存退出0：[成功日志](./evidence/gametest-rerun1.log)。首轮因漏斗逐件传输等待不足失败，调整该场景时点后通过；[首轮日志](./evidence/gametest.log)保留，不隐藏失败。
- 尾部提示文字与机械臂旧模式归一经独立审查不影响已测事务，复用上述证据；最终源码已由[增量assemble](./evidence/assemble-final.log)重新编译打包，退出0、3秒。没有全量测试或重复客户端启动。
- JAR相关资源 **17/17** 与工作树字节一致：[打包核对](./evidence/resource-check.txt)。JAR为候选目录`build/libs/create_nuclear_industry-0.1.0.jar`，SHA-256 `D6DEBDDB38DE7B423A11927EBDB7CF0A344160C99986C2A00B204E31BE1DBC4D`。初版素材预检日志另存[assemble.log](./evidence/assemble.log)，不充当终版证据。
- 5张新SVG/PNG、三份模型共30元素、8个朝向/工作状态组合通过定向检查；98张旧游戏PNG与基线一致。PM及独立审查实际查看[物品视角](./EXT-A-FUEL-02D-assets/previews/item-gui-off.png)、[工作正面](./EXT-A-FUEL-02D-assets/previews/front-on.png)等最终预览；这仍不能代替游戏内视觉验收。

未运行用户客户端或改写默认run存档；主目录`.vscode/launch.json`保持用户原改动。四组人工门通过前，不宣布02D验收完成，不推进冷却剂或其他下游任务。
