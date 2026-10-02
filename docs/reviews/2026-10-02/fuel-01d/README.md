# 两格离心机01D候选交付

**状态：待客户端增量验收，自动推进暂停。** 功能提交`97ca72a436f9653745ed8a879e25871227088b1f`，候选工作树`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`、分支`codex/ore-acquisition`。当前main仅同步文档，功能仍为材料05；本批尚未合入main、未推送或发布。

用户已确认上一版离心机工作手测通过，并批准两格高、顶部进浆、四周由黄铜漏斗或管道选择产物。本批按[01D任务卡](../../../superpowers/plans/2026-10-02-ext-a-fuel-01d.md)实现以下增量：

- 一件放出宽1×深1×高2的设备，下段独占库存、批次、磨损和加工；上段代理到同一下段。
- 上段顶面进料浆，下段底面传动；上下两段的八个水平机面都能抽两种粉或回收水，位置由外置漏斗/管道决定。删除全部机身过滤槽，继续无GUI。
- 重做八棱鼓壳、顶盖、黄铜金属箍、观察孔、底座与旋转部件；完整立体物品模型配专用显示缩放。成本、产率、速度、应力和维护参数不变。
- 旧单格暂停并保留数据，不覆盖上方方块；用扳手收起后重放升级。双格拆除使用唯一便携快照；纯掉落查询不占领取标记。

## 验证与证据

| 证据 | 已确认范围 |
| :--- | :--- |
| [运行交付](./runtime-report.md)及[Gradle输出摘录](./runtime/gradle-output-excerpts.txt) | 初次端口/桶事务/料浆桶共6项JUnit通过；掉落查询整改后仅端口类3项复跑通过，另两类各2项复用初次证据；最终原版挖掘统计/疲劳修正后增量assemble通过，未重复JUnit。摘录不是完整控制台日志。 |
| [端口与快照XML](./runtime/TEST-com.iksxh.create_nuclear_industry.production.CentrifugePortsTest.xml)、[容器事务XML](./runtime/TEST-com.iksxh.create_nuclear_industry.production.CentrifugeContainerTransactionTest.xml)、[料浆桶XML](./runtime/TEST-com.iksxh.create_nuclear_industry.production.CentrifugeSlurryBucketTest.xml) | 仅证明所列无世界测试；没有真实ServerLevel或完整放置/拆除事件。 |
| [资源交付与预览](./assets-report.md) | 四份SVG导出16×16纹理，lower/upper/item/rotor模型、八种块状态、纹理引用与几何范围检查通过。两角度预览读取最终JSON，材质近似，不证明游戏UV/光照或手持效果。 |
| [独立合并审查](./review.md) | 一名审查者核对锁定NeoForge/Create生命周期、能力、唯一状态和模型。发现的纯掉落查询副作用已整改，挖掘统计/疲劳遗漏也已关闭；整改仅定点回看。 |

最终`build/libs/create_nuclear_industry-0.1.0.jar`的SHA-256：`D86D2CC3BB8446502F63443C26747CCBD0A49FE6193924E756C80A1AB1B72929`。最后功能修正后打包，后续文档整合不改变功能文件，不重跑构建。项目经理恢复了仅由本轮JUnit改写的根目录跟踪日志；没有修改默认run存档、停止用户客户端或改动主工程既有`.vscode/launch.json`。

执行者报告按交付时点归档，其中“未提交”“待审”等文字保留原语境；最终提交和审查结果以本页及审查追加记录为准。项目经理实际使用minecraft-modding、minecraft-testing、minecraft-resource-pack核对任务及验收，minecraft-ci-release核对候选提交边界；执行者实际技能使用见各自报告。

## 客户端停止点

完全退出旧客户端后运行：

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
.\gradlew.bat runClient
```

只按[五组增量清单](./CLIENT-CHECKLIST.md)检查旧机迁移、上下接口、一批/三台守恒、拆放恢复和模型。原配方及未改参数不机械重复。真实世界放置取消、拆除/爆炸/替换、缓存能力等没有本轮动态证据，不把静态审查记成人工通过；无保护模组时不另装插件。转子停机时回到零相位，启停可能跳位，留作视觉确认。

本轮未运行GameTest、完整回归或客户端。等待用户本批人工反馈后再处理整体验收与main功能合并，不提前推进后续燃料设备。
