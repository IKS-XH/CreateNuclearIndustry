# 高压锅炉01A候选交付

**状态：实现及独立审查通过，等待人工验收，未合入main。** 功能提交`af106ab`，基线`891f73e`；候选目录`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支`codex/ore-acquisition`。后续仅文档归档，不改变已测实现。

本批落实已批准的3×3×4手搭锅炉：七部件制造、1～8段核热输入、独立暖炉账本、原生Create水汽管网、无GUI交互、泄压与保存/携物恢复。超临界蒸汽不提供桶或世界流体方块，开放管口拒收。配方与数值见[完整合同](../../../superpowers/plans/2026-10-04-high-pressure-boiler-proposal.md)，不在交付中另定参数。

## 证据

| 范围 | 结果及依据 |
| :--- | :--- |
| 纯账本与共享事务 | 27/27：锅炉8、换热器12、共享填充计划7；本目录保留三份原始JUnit XML |
| 实际锅炉接入 | 7/7 GameTest，正常退出0；双水口近满灌注、原生泵管储汽、开放口防丢料、核热暖炉、结构失效、单件携物及代表配方 |
| 受影响原生锅炉 | 11/11 GameTest，正常退出0；包括既有9/18热级行为 |
| 构建与素材 | 增量assemble成功；本批资源静态检查0错误，已修观察窗共面重叠/单面开窗及蒸汽图集登记 |
| 独立审查 | [一次合并规格与质量审查](./EXT-B-BOILER-01A-REVIEW.md)未发现须修问题，可进入人工门；没有复跑套件 |

[设备执行报告](./EXT-B-BOILER-01A-DEVICE.md)、[JUnit/构建输出](./EXT-B-BOILER-01A-DEVICE-junit-assemble.log)、[锅炉运行输出](./EXT-B-BOILER-01A-DEVICE-gametest.log)、[原生回归输出](./EXT-B-BOILER-01A-DEVICE-native-regression.log)、[素材报告](./EXT-B-BOILER-01A-ASSETS.md)按原样归档。报告内的`build/`路径指本次候选的原始运行目录。本轮没有全量回归；审查和交接复用同一实现证据，不以静态资源检查代替客户端视觉。

## 用户下一步

按[人工清单](./CLIENT-CHECKLIST.md)从候选目录运行`./gradlew.bat runClient`，完成三组：制造/搭建/显示、真实换热储汽、停产/泄压/保存拆放。清单附四层俯视搭建图。

在用户反馈通过前暂停，不推进汽轮机或宣称专用发电闭环完成。汽轮机、冷凝、普通辅助热、二级耐压限制、工作盆、事故及Ponder仍是后续范围。主目录只保留已验收的换热器运行功能与批准文档；用户启动配置、两个客户端与原存档均未改动，候选原有日志和Python缓存不纳入本批提交。
