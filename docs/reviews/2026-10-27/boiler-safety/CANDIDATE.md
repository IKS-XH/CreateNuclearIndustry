# 锅炉多阀与事故候选

2026-10-27。用户已批准[冻结设计](../../../superpowers/specs/2026-10-26-boiler-accidents-design.md)。[同轮组合窄审及整改复审](REVIEW.md)已通过，生产来源提交`7d2a28eb28260c3eb51502b9c9f8217f73d60259`，分支`codex/boiler-safety-20261026`。这是待集中手测的候选，未合main生产或替换其正式包。

## 测试入口

候选目录为`E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration`，在其中执行`./gradlew.bat runClient`。本批默认携带main已保存的“新的世界”，源27文件/13337721bytes前后与副本逐文件SHA一致、原件未打开或回写；候选旧29文件世界完整保存在`run/save-backups/boiler-safety-20261026/新的世界`。复制清单与原始验证日志留在候选树`build/reports/boiler-safety-20261026/`，世界不进Git。

候选[JAR](E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration/build/libs/create_nuclear_industry-0.1.0.jar)已由已提交生产源增量构建并经PM复核：3710963bytes，SHA256 `9829AC0E3FC6E2AA8424D1591059B64B183D60EADD19422E94058A3CB10D5297`。`gradlew jar` exit0/1秒，没有重复测试；2599载荷与现有输出绑定，本批测试类/模板精确排除，见[制品报告](ARTIFACT.md)。main原日期F29D包未改，原正式路径缺失的用户状态也未擅自恢复。

## 本批行为与默认配置

锅炉顶面非边框允许多个泄压阀，正上方1格只有已加载空气才畅通，堵塞只影响该阀。每阀实际扣除相应mB/HU，正常汽口及双汽身份不变。堵阀/满汽/空水不再自动免除收热；红石、无热液、冷液满仍停止新收热，余热真实保留。

配置位于世界`serverconfig/create_nuclear_industry-boiler.toml`，阀参数在顶层，其余在`[accident]`：

| 参数 | 默认值 |
| --- | --- |
| valveFlowMbPerTick | 512mB/t/畅通阀 |
| valveOpenPressure / valveClosePressure | 0.9 / 0.8 |
| accident.ratedPressure | 1.0，即100% |
| accident.instantBurstPressureRatio / overpressureTicks | 1.5 / 200tick（10秒） |
| accident.recoveryPressureRatio / recoveryTicks | 0.95 / 40tick（2秒） |
| accident.dryFiringTicks | 400tick（20秒），无水且炉壁达到沸点才累计 |
| accident.minimumBurstRadius / burstRadiusPerPressureRatio / maximumBurstRadius | 2 / 4 / 6格，R=clamp(2+4×(实际压力/额定压力−1),2,6) |
| accident.maximumBurstDamageHp | 16HP，向外喷爆方向按距离衰减，每生物每事故一次 |

干烧连续到期随机损毁一个换热器或再加热段、不掉完整设备，其余实际工质不任意清空。超压超过时长或达到瞬时线，随机六面之一向外喷爆并损失原炉工质；不产生核废料/熔融物/辐射。危险手拆不等待计时：超压立即爆裂，仅干烧立即损毁一件；最终被外部保护取消的拆除无事故副作用。正常工况拆除沿旧规则。

## 一次集中手测（未验收）

1. 多阀：分别遮挡/疏通，观察仅畅通阀排汽，护目镜畅通数/总上限/实际排量与粒子一致、图标不挡字。
2. 干烧：冷空炉不计危险；达到沸点后无水连续20秒只损毁一件，补水或冷至沸点以下复位。红石停止新收热但不清余热，其余真实工质仍可回收。
3. 超压：>100%且<150%容忍累计10秒，<=95%持续2秒清累计，150%立即喷爆。正常汽口或足量泄压能及时降低当tick压力，不能误爆。
4. 拆除与效果：危险普通挖掘/控制器潜行扳手/换热器潜行扳手触发对应事故；正常拆除保持。观察随机破口、方向、范围和生物伤害，新放同态部件应正常回收，不被旧记录误吞。
5. 保存重进：危险进度与待执行方向/游标正常续接，不重复伤害、物料或控制器携带库存。
6. 携带控制器跨维度：玩家实际进入目的维度，使用真实携带物重新搭建并形成工质交易；确认无永久未成型、旧现场任务继续、工质不复制。这是下面明确未自动覆盖的子项。

## 已核证据与边界

最终五类锅炉单元52/52、无跳过，exit0；真实服务端19个注册场景的核心断言通过、无整场SKIP、正常保存退出。R4真实生存取件及BlockItem跨维度重放、pending解除/新世代/合法几何/旧任务完成/库存唯一均实测，但隔离Nether夹具的`areEntitiesLoaded=false`，完整成型交易明确为`SUBCASE_UNAVAILABLE`，不算通过。最后凭证置脏无强载修正仅复验对应1场，另18明确SKIP，其余未变实现复用原证据，见[整改与原始日志索引](FIX.md)。客户端显示、事故效果、真人保存重进、物理卸载和多人不能由这些自动证据替代。

本批因共享环境移除/最终拆除接线执行一次全JUnit：865项=851通过、3既有失败、11跳过，exit1；两项HeatExchangerBoilerBridgeTest配置未加载，一项ReactorFissionDifferentialTest全状态差分，准确仍为原基线。整改后仅复验受影响部分，不重跑全量，不称`gradlew build`全绿。原失败/RED/各轮夹具诊断保留，无无关热工修复。

PM已核26项最终源清单SHA与工作区一致再精确提交，[SOURCE.json](SOURCE.json)记录打包来源。原报告中的“本树”均指上述候选树，历史初审问题保留原语境。[步骤01](../../../superpowers/plans/2026-10-26-boiler-safety-01.md)、[步骤02](../../../superpowers/plans/2026-10-26-boiler-safety-02.md)的人工门未闭，不自行推进下一功能、合main或发布。
