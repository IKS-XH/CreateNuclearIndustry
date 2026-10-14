> 当前状态更新（2026-10-12）：用户已确认本组合的可变尺寸、快捷换料、观察窗和顶面控件复看及最近重进存档手测通过，已[净合main `6bdef37`](../reactor-variable-accepted-integration/ACCEPTANCE.md)。下文待验/未合main与旧JAR身份为历史记录，锅炉尺寸16³新增范围独立待验。

# 反应堆可变尺寸01：客户端候选

2026-10-12，PM核验。实现和自动审查完成，停在本批集中人工门；不表示客户端玩法、视觉或操作已经验收。源码未合main，主目录正式反应堆仍5×5×5，主目录制品未被本候选覆盖。

当前组合补充：共享库存显示修复`209cd81`和双面铅灰观察窗`fe30e83727fe6125690ee84a0a4f46855355740d`已精确接入本候选，见[ART08组合记录](ART08-INTEGRATION.md)。当前JAR为2,667,047字节、SHA256 `0940bddc9a8c6d991a3d3bbd7b42e0dd3382c6d9ca3fa0f0d482682aaea8958b`，必要组合assemble exit0/8秒；下方31源/50class清单与`794a9ca…4cef`包为原变量尺寸冻结历史，均已保留，不能当作当前包身份。

## 实际工作区与来源

- 候选目录：`E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`。
- 分支：`codex/reactor-variable-size`。
- 净实现提交：`08409069a5d6abc094dbf3f198a5256546bd50fa`，31个src路径和两份原始实现/审查报告；PM候选及入口文档另提交，不夹带日志、缓存、存档或其他美术改动。
- 生产基线：main `5019dbecd0a3b1e9a14d23a22c0f718d0387d207`；需求文档基线`bf25343be89ac420e82c691269390a8ac44fb028`。
- 合同：[活动计划](../../../plans/2026-10-12-reactor-variable-size-01.md)。实际栈仍为Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82。

主目录中本候选说明及报告的副本仅用于文档导航；原始build日志、XML、冻结清单和实际JAR始终位于上述候选目录。执行者报告中的“未提交/基线HEAD”是报告形成时点，以本页PM提交登记为当前Git状态。

## 本批行为

完整长方体宽、高、深分别为5～16整数，允许偶数、不等边长和跨区块。SERVER配置`reactor.minDimension=5`、`maxDimension=16`，合法组合5≤min≤max≤16；工程上限16不要求对齐区块。

棒列及接口沿用原规则，主体高度为外高减2。一列仍只装一个真实燃料组件；产热和燃耗按实际高度线性计算，单钢板维修量为0.25/实际内部高度。空列和控制棒列的主体格共同贡献冷热共享容量，默认1000mB/格；控制棒升降不改变容量。6×8×7一燃料列为114000mB；16³一燃料列为2730000mB。

全堆新生热只在汇总并限幅后向上取整一次，小数燃耗与冷却余数保留。多口仍按每口限流；安全拆除、SCRAM和缺区块暂停保持原语义。缺区块及首次恢复扫描前的旧组件占据记录只用于保守拆除保护，不用来宣告成型或继续热工；清理旧遥测和管道通知不强载远端区块。

显示消费实际包围盒/主体格，原材质、棒体液体渲染顺序及控件合同保持。思索继续使用5³例子，只调整可变范围及“填满内部高度”两处搭建措辞；无其他设备/教程/配方变更。

## 验证边界与制品

证据根：[候选原始证据](E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/build/reports/reactor-variable-size/)。

| 证据 | 实际结果与边界 |
| :--- | :--- |
| `12-build.log`、`12-test-results/` | 唯一完整增量build，exit0/24秒，94套514/514，零失败/错误/跳过；这是审查窄修前的完整证据，保留原冻结身份 |
| `10d-world-green.log` | 6×8×7、16³两项真实业务required断言通过，覆盖远端绑定/维修、缺FULL、真实物理卸载及新实例恢复；随后保存退出挂起，Gradle exit1，不能称整次命令成功。仅结束本轮自有进程，原日志及停止记录保留 |
| `19-waiting-final-green.log` | 审查整改的真实安全探针1/1通过，正常保存退出，exit0/26秒；覆盖首次Post前、等待、近端新实例恢复、无关位置放行、事务回滚和无强载入口 |
| `20-directed-final.log`、`20-test-results/` | 审查窄修后受影响9套51/51，零失败/错误/跳过，exit0/21秒；复用原完整build，不重复两项已过业务 |
| `21-assemble-final.log` | 最终必要增量打包，exit0/1秒；随后仅删除一处新测试文件EOF空行并窄复核，测试/生产语义及JAR不变，无再次构建 |
| [独立审查](REVIEW.md) | 一次合并规格/质量审查及同项窄复审关闭全部P1/P2/P3必改项；未以自动描述符、局部管网探针代替客户端验收 |

细节、RED记录与公式见[实现报告](IMPLEMENTATION.md)。最终[冻结清单](E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/build/reports/reactor-variable-size/final-identity.json)绑定31源、50生产class及4资源的源码/安装/JAR身份；清单SHA256：`67e417661cba142195779555747b756ff0c1c80ec0daa849457994fd01f20b3b`。

原变量尺寸冻结JAR：2659488字节，SHA256：`794a9ca78ec091116c5500be70999ac0082d1f219357cbe62ab9be66f18c4cef`，现保留在`build/reports/reactor-variable-size/shared-window-08-integration/before-combination.jar`。main未拿此包替换；当前组合[JAR](E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/build/libs/create_nuclear_industry-0.1.0.jar)及补充身份以上方记录为准。

## 世界与手测

主目录“新的世界”共27文件已完整复制到候选`run/saves/`，数量及SHA256全部一致；候选原世界留在`run/save-backups/reactor-variable-size-01-20261012/`。见[复制记录](E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/build/reports/reactor-variable-size/world-copy.json)。原主世界保留，不自动回写；GameTest使用独立build世界。

按[集中检查单](CLIENT-CHECKLIST.md)复看5³原例、6×8×7偶数长方体、16³跨区块的成型、实际供热、完整棒体/液位、顶面控件、远端安全拆件及当前版本保存恢复。无需重复其他设备或生产线全套测试，不研究旧版本存档迁移。

```powershell
cd E:\MyMC\NewMod\Create_NuclearIndustry-art-integration
.\gradlew.bat runClient
```

当前候选自动门已关闭，人工门待用户反馈；本阶段保持分支、工作树及来源证据，不合main功能、不推进下一主线。
