# 工业泵静止方向与主目录反应堆流量配置窄修

2026-10-13用户明确暂停Sodium/Iris兼容，报告当前main两项：工业泵方向箭头只在有动力时显示；四个反应堆端口只有400mB/t。基线main`6d1baba`，此前泵两端三角R2已验收，原36点素材与管网/汽轮机功能保持。

PM已实际核源：`IndustrialVisualOrientation.displayedOutput`在`getSpeed()==0`返回null，Renderer因此不画箭头；Create的getSpeed在超载/冻结时为0，getTheoreticalSpeed保留真实同步轴速符号。P1ServerConfig默认2048，正式ReactorCoolantFluidHandler与仪表冷却账本均读取同一配置；主目录`run/config/create_nuclear_industry-server.toml`实际`reactor.perPortFlowMbPerTick=100`，其注释Default已为2048。此为现行配置覆盖，不是未合限流代码或旧存档迁移问题。

主目录原位不建分支/搬存档。唯一执行者实施，PM只管理文档/Git与核验；执行者禁止Git写或转派，稳定后一次独立窄审。实际读取AGENTS、minecraft-modding/testing、systematic-debugging，维持锁定1.21.1/Java21/NF21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6；精简定向验证，不用通用技能重复全套测试。

## 精确写集与行为

只允许以下生产/测试/配置路径：

- `src/main/java/com/iksxh/create_nuclear_industry/industrial/client/IndustrialVisualOrientation.java`
- `src/test/java/com/iksxh/create_nuclear_industry/industrial/IndustrialVisualContractTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/industrial/IndustrialPipeSurfaceTest.java`
- `run/config/create_nuclear_industry-server.toml`，仅把reactor.perPortFlowMbPerTick从100改为2048，其他字节/设置保持；先备份原文件到本批报告目录，此实例配置不进Git。

报告只写`docs/reviews/2026-10-13/industrial-pump-idle-arrow-flow-config/IMPLEMENTATION.md`，原始日志/来源/包/配置备份在`build/reports/industrial-pump-idle-arrow-flow-config/**`。不改Renderer、泵服务端、NBT、成型/热工/接口、配置注册/默认值或其他设备，不续做Sodium。

方向箭头表示预定运输方向，任何动力状态都显示：理论轴速非负（含新放置/零速）按FACING，理论轴速负按FACING反向；实际运行时应与原outputDirection完全一致。超载/冻结只停止流体运输，不抹去已知轴速符号，不能借显示箭头启用无动力流量。保留原glyph/六面矩阵与R2素材，不新增持久状态或第二个方向配置。

## 必要验证与交付

1. 先更新原两个方向测试，使无动力与超载正负转向的显示可见；实际运行六向正负方向及真实FaceBakery glyph/SuperByteBuffer矩阵保持，留RED精确原因。冻结分支若现有夹具可直接测则同组核，不能只用源码文字证明行为。
2. 修复后只跑这两项及必要的原泵流量零速/应力代表；不重跑管道64矩阵、材质生产、原R2表面/桶/整机或fullbuild。现行端口数值不写新的镜像测试，核配置解析实际2048、唯一键差异和正式handler/账本消费链；原2048端口测试证据复用。
3. 稳定后唯一必要增量assemble，备份原b0包；冻结实际源码/包及配置前后身份，核其他设置和已验素材/用户包保持。用户可能继续游玩，只记录本任务没有世界写入，不把游戏自己保存计作本任务修改。
4. PM一次独立规格/质量窄审，无代码改动不重复构建。最后交main新JAR与两项集中复看：停转/超载可辨方向、供液/动力/管路条件充足时单个冷热口按2048上限；四个同向口理论合计8192，不把实际产熱、管路瓶颈或2冷2热混计为保证流量。

最终只等待上述局部人工复看，不重新开启原已通过门、不推进管理员/管网压力或兼容研究。用户已确认每口2048，无需重复确认；本次只恢复主目录实际设置，不全盘重置用户配置，也不自动改联机实例配置。

## 实际交付

已交[main候选](../../reviews/2026-10-13/industrial-pump-idle-arrow-flow-config/CANDIDATE.md)：三源码与实例唯一100→2048键修正完成，最终3/3定向检查、唯一1秒增量assemble及一次独立窄审通过。16来源与745e936c…正式/冻结JAR一致，原R2素材/运输和用户包保持；首轮配置夹具及包审计错误如实保留，未因此重跑整机门。配置不进Git，源码与本批文档净提交main，只待停转箭头和实际端口流量局部复看。
