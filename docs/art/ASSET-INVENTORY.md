# 当前美术与渲染入口

更新：2026-10-15。真实ID见[内容](../content-catalog.md)，状态见[STATUS](../STATUS.md)，阶段稿见[归档](../archive/2026-10-15/README.md)。

| 内容 | 入口/边界 |
| --- | --- |
| 反应堆/锅炉/汽轮机 | 各自客户端只消费服务端显示快照；棒位、液位、雾效和runtimeDisplay不拥有热工/应力账本 |
| 离心机14B | [Renderer](../../src/main/java/com/iksxh/create_nuclear_industry/production/CentrifugeRenderer.java)，五模型/六PNG，底轴和转子共真实有符号RPM |
| 装配台15C | [Renderer](../../src/main/java/com/iksxh/create_nuclear_industry/production/ShieldedAssemblyRenderer.java)、[动作JSON](../../src/main/resources/assets/create_nuclear_industry/shielded_assembly/animation.json)，八活动分件，原窗口/完整物品/Create半轴保持 |
| 工业管泵阀 | Create原模型调色，实际运输方向由服务器决定；原冲突三角与静止箭头独立留验收 |
| 其他加工/贮存设备 | 现行heat_exchanger、fuel_sintering_furnace、dry_storage_rack等模型，后续美术不改变工序 |
| Logo/宣传 | mod_logo游戏图标，HD Logo/MC百科封面可编辑源在tools/art-assets，交付边界见美术README |

游戏资源在src/main/resources/assets/create_nuclear_industry，可编辑SVG/导出/预览在tools/art-assets；使用实际ID，不为未来清单造空内容。第三方来源/许可声明保持，不因调色/归档扩大授权。
