# 01F：两种真实蒸汽库存的限定设计分析

本轮仅做只读设计，不修改代码、测试、配置、用户世界或 Git，不运行 Gradle。用户已确认“两种库存分开记录，共用总容量，各口只取对应库存”；PM 随后确认 SC 低于原 50% 生产门槛仍可输出至公共下限，但交付必须具有实际 `h(Tsc)` 比焓；冷却不足的 SC 保留身份和容量，等待真实再热。新生产批次按本次生产后的共同炉压及该批实际汽温归类。周转缓存建议撤下，不实现缓存、滞回、降级或外部流体重标。

## 实际核对与旧故障边界

已读取 AGENTS、治理 5.1/5.2、01E/R1 活动合同，并实际使用 `minecraft-modding`、`minecraft-testing`、`superpowers:brainstorming` 的架构探索阶段。技术栈沿用仓库 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280，不套用技能中的升级示例。

当前 `BoilerState` 只有 `steam/steamHu` 一池；`supercritical()` 对这一整池每次重算当前温压资格。控制器 `outputFluid()` 决定唯一实际流体，端口再与独立选择比较；`Port.valid()` 同时比较保存的流体与最新实际流体。因此旧模式中跨线改变的是整池身份及句柄资格，而不是已有两种真实库存。

R1 的 14 号日志中，独立 NORMAL 13 管支路有 295 个资格 tick、103 个窗口、最长 5 tick，但普通罐 SIMULATE/EXECUTE 均为 0；直邻普通罐收到真实 56000mB 后满罐。15 号取消部分温压变化拓扑刷新候选仍失败，已撤回；控制器当前相对 HEAD 的 diff 为空。本设计替换整池自动切种合同，不能宣称旧候选修复成功，也不能以旧“热跨线必须撤销旧句柄”断言继续要求自动切种。

## 账本与共同压力

最小状态是 `normalSteamMb/normalSteamHu` 与 `supercriticalSteamMb/supercriticalSteamHu`。仍只有一个几何汽容量 `C`；新产汽可用空间为 `max(0, C-mN-mS)`。总显示量、总 HU 与守恒计算分别使用两池之和，不能为每池各分配一份 `C` 后允许总量达到 `2C`。公共压力下限和每口配置流量额度保持原值、原单位及原同 tick 记账方式。

沿用现有焓函数：`hB=cpWater*Tb+latent`，`h(T)=hB+cpSteam*max(0,T-Tb)`。对于非空库存 `k`：

```
hk = Hk / mk
Tk = Tb + max(0, hk-hB) / cpSteam
Q  = mN*TN + mS*TS
P  = Q / (C*Tsc)
```

空池贡献为 0。总汽温可继续显示 `Q/(mN+mS)`，但这只是混合总量观察值；分池温度和焓才是交易输入。这个压力扩展在两池同温或仅有一池时严格退化到现有公式。

尺寸/配置缩小导致已存在的库存超过新容量，是既有“保留库存、不补量、不删除 HU、停收停产”的边界；不能为了形式上的 `sum≤C` 静默删除现存蒸汽。正常填充和新生产始终保证总容量，缩容储备只允许真实抽取/泄放后回到容量内。非法配置仍禁止交易。

## 新批次归类与 tick 顺序

保留当前顺序：补水/收热 → 水侧预热 → 已有汽真实再热 → 新批次汽化 → 逐口输出 → 安全阀。生产使用现有目标汽温、比热、潜热及水携热公式；不要通过“想产 SC”改变额外收费或先创造未付 HU。

先用旧生产算法计算一个批次的 `n`、真实 `Tnew`、`n*h(Tnew)` 及对应水侧/HU 扣账，且 `n` 受共享余量约束。当前算法的 `Tnew=max(Tsc,已有总汽温)` 可沿用；这里的已有总汽温为上述质量加权观察值。把批次加入后的压力预览为 `Pafter=(Q+n*Tnew)/(C*Tsc)`，不依赖批次最终进哪一池，故无循环判定。

按 PM 已确认规则，该整个新批次在 `Tnew≥Tsc && Pafter≥Psc` 时进入 SC 池，否则进入 NORMAL 池；比较容差沿用现有 EPS。归类看该批实际汽温，不要求旧 NORMAL 池或混合平均温度重新符合 SC。先补已有池热量产生的压力也计入这次真实 `Pafter`。已存在 NORMAL 不因加热/增压升级；已存在 SC 不因抽汽/调低下限降级。加入同种池时只相加真实 mB/HU，池内真实混合会改变其平均比焓，不转移另一池的 HU。

无需逐 mB 拆分本批次跨线点：现有生产单位就是一个 tick 内已结算批次，按批次最终温压一次归类即可。实现注释和测试应明确这一边界，防止下一位实现者另加门槛、汽种滞回或隐含优先级。

## 已有库存再热与散热

两池分别计算到现有目标 `Tsc` 的热缺口 `Dk=max(0,mk*h(Tsc)-Hk)`。总再热需求为 `DN+DS`，仍消耗真实 `processHu`，且仅在原允许余热路径中使用水/炉壁高于沸点的已付余热。压力封顶热空间仍是 `max(0,(Popen-P)*C*Tsc*cpSteam)`：给任意汽池补 `dH` 都使 `Q` 增加 `dH/cpSteam`，不会因为两池而多出一份封顶余量。

推荐将本次可支付再热量按两池热缺口比例分配，并以剩余差额收尾，避免固定先 SC 或 NORMAL 导致长期饥饿。这属于必要的确定性分配算法，未新增热工目标；若 PM 希望某种优先再热，应先把优先级写入合同，不能暗设。

散热总额度沿用 `steamCells*idleSteamCoolingHuPerCellPerTick*ticks`，不能两池各扣一份。推荐按可散显热 `max(0,Hk-mk*hB)` 比例分摊；池达到沸点焓后不再散潜热。原本不足汽化焓的异常/无资格库存也不能通过取最大值被补出免费潜热。卸载间隔与 idle 路径共用相同分池结算，只有真实散失后的 HU 变化，没有汽种重标。

SC 输出资格为本池真实 `HS≥mS*h(Tsc)`；不再检查当前炉压是否达到生产用 `Psc`。若冷却使 SC 比焓不足，库存仍是 SC、仍占共享容量，SIMULATE/EXECUTE 均不可抽，等待再热。NORMAL 输出资格为 `HN≥mN*hB`。这两道已付焓边界都是原热量合同的延续。

这项保护确有必要：当前汽轮机输入仅校验 SC 流体 ID 和 mB，`TurbineState.totalSu()` 按真实流量乘固定 `suPerMbPerTick`/效率计算；外部 `FluidStack` 没有传入锅炉 HU。直接交付冷却不足的 SC 会获得同样 SC 发电额度，不能靠“扣掉较少真实 HU”自行消除。此处不改汽轮机或外部流体，也不承诺建立跨设备通用能量传输协议。

## 按种类保压抽取、模拟与安全阀

对已选池 `k`，当前可抽量须每次从最新两池总压力重算：

```
nPressure = max(0, floor((Q-minimum*C*Tsc)/Tk + EPS))
n = min(requested, mk, 本口本tick剩余额度, nPressure)
```

仅在该池真实焓资格满足时交易；空池返回 EMPTY。执行从该池扣 `n` 和 `n*hk`，另一池 mB/HU 不动。相同比焓抽取保持该池温度，其共同压力减量为 `n*Tk/(C*Tsc)`。SIMULATE 完全纯读，不改库存、热量、预算、更新时间或汽种。

例如 `C=32000,Tsc=2,mN=4000,TN=1,mS=8000,TS=2` 时 `P=31.25%`；下限 10% 时压力余量 `Q-6400=13600`。抽 NORMAL 的压力上界为 13600mB，抽 SC 的上界为 6800mB，再各受本池库存及每口额度限制。把总平均温度用于两者同一抽量会违反下限。多口执行必须顺序重读账本，不能把各口 SIMULATE 的压力余量当独立配额叠加。

保留安全阀既有启闭压力、合计每 tick 流量上限、阻塞判断及 once-per-tick。阀泄放也逐池扣实际焓，不转换流体；推荐按库存量比例给两池分配泄放量，再对每次真实扣账用该池温度和剩余共同压力到关阀线的空间封顶。整数尾差按固定顺序有界分配，不能从两个池分别泄放一份阀流量，也不能用混合平均温度推导一个池的抽量。具体配额只是阀内部确定性算法，不改变端口 256mB/t 配置额度或阀独立额度。

## 能力、原生压力与显示的最小接口改动

- `BoilerState` 增加按 `BoilerSteamKind` 查询量/HU/温度/资格和抽取的入口；总量入口保留用于总览。所有输出方传实际所选种类，不能保留未分种抽取后由控制器贴标签的入口。
- 控制器 `Port` 固定捕获该口选择对应的流体 ID，库存查询返回该池真实量；没有该池库存时 EMPTY。句柄有效性只依赖结构/端口选择生命周期，数量、压力、焓变化不会把合法旧句柄变成另一种流体。结构拆件、方向改变、该口选择改变仍撤销旧句柄；冷液口等仍不受汽口变化影响。
- `drain(FluidStack)` 同时校验固定选择与请求流体；`drain(int)` 依相同固定种类。没有已付热或没有压力余量时真实模拟返回 EMPTY，不假冒可抽量或库存。直接罐 push 路径用同一种类重新核对接收/扣账，只有接收成功才扣 HU/mB。
- `publishedSteamFluids/steamEpochs/refreshSteamKind` 的职责应收窄为端口选择/结构身份；不再以共同炉压跨 `Psc` 发布/撤销全炉流体。主动压力 predicate 改为“该口选中池有真实库存、已付焓合格且有共同压力余量”，保持空池、热不足和公共下限时真实无流。不要把本口当 tick 额度恰好用完当成拓扑资格变化；额度只由交易入口逐次限制，下一 tick 自动续算。停空/恢复仍尊重 Create 原生 EMPTY 清 Flow 与 2tick 等待。
- `BoilerSteamPressure` 自有压力归属/BFS 及 `BoilerPipePressureMixin` 的 network 忘记机制不持有汽种库存，现阶段没有必改理由。保留选择/结构改变时的拓扑刷新；不能把它们作为存两种汽必须扩修的文件。
- 护目镜保留共同压力、公共下限与总汽量，补两池 mB/实际温度或实际输出资格；“当前产汽”若保留应指最后新批次种类/本 tick 产量，不能再显示整个库存唯一汽种。端口仍是原两项过滤，无新 GUI。

## 建议精确写集及定向验证

建议正式卡必需生产写集：

1. `src/main/java/com/iksxh/create_nuclear_industry/boiler/BoilerState.java`：两池、公式、再热/散热/阀、 typed drain、当前版本保存加载。
2. `src/main/java/com/iksxh/create_nuclear_industry/boiler/BoilerControllerBlockEntity.java`：端口固定身份、真实 typed 交易、主动压力 predicate、状态/客户端同步与护目镜。
3. `src/main/java/com/iksxh/create_nuclear_industry/boiler/BoilerSteamKind.java`：修正旧“实际整炉汽种”相关语义注释，保留 enum/ID/原生选项。
4. `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json` 与 `en_us.json`：只调整受到两池显示影响的键。

`BoilerPortBlockEntity.java` 只有当端口新增状态显示确实需要入口时才增加写集；现有选项、callback 与 persisted ScrollValue 可直接复用。`BoilerConfig`、流体注册、汽轮机、Create mixin、构建脚本不在最小生产范围。

必要测试写集首先为 `src/test/java/com/iksxh/create_nuclear_industry/boiler/BoilerStateTest.java`、`src/main/java/com/iksxh/create_nuclear_industry/gametest/BoilerSteamSelectionGameTests.java`、`src/main/java/com/iksxh/create_nuclear_industry/gametest/BoilerMultiportStopGameTests.java`。复用 R1 空模板和实际两支管/真实接收夹具；统计改为各池持续实际可抽、各支接收与守恒，不再累计“整炉 NORMAL 短窗”作为合同。01D 热跨线失效断言改成“身份不换、选择/拆件才撤销”，不能为了旧断言恢复自动切种。

旧 `BoilerControlsGameTests.java`、`BoilerTurbineFlowGameTests.java`、`ExtensionBoilerGameTests.java` 的部分场景使用 `Steam/SteamHu` 人工 seed 或期待单池热跨线自动切种，正式卡需要允许仅迁移相关 seed/已被替代断言。它们不是额外业务需求；可先静态逐项列出确有影响的方法，再纳入精确写集/必要筛选，不机械重跑全部历史域。生产核心与测试 helper 尽量提供一处显式分池 seed，不能偷偷依赖历史单池 NBT 回退来让测试绿。

定向单测覆盖共享总容量、生产前后跨线归类、已有 NORMAL/SC 不改身份、不同温度逐种保压、同 tick 多口不复制预算/余量、SIMULATE 纯读、冷 SC 停交付→真实再热恢复、两池散热/阀总额及 mB/HU 守恒、当前版本两池 NBT roundtrip 与 used budget。真实管路至少复用 R1 主场景和直罐对照，检验两库存积累后的 NORMAL 13 管实际成交以及 SC 低于生产门槛仍真实成交；普通接收罐必须有足够余量，满罐须明确停止验收窗口。外部普通/SC 混管导致真实异种背压仍允许等待，不为让混管绿改写罐库存。

按治理 5.1，阶段性失败先保留日志定位，不重复旧 17 项、无关 JUnit 或全域。按治理 5.2，只检查新模型本版本自身正常保存恢复；不研究旧存档迁移，不改用户世界，不把历史 NBT 矩阵加成门槛。正式卡的字段命名和 helper 更新应统一，不在本只读报告中实现兼容层。

## 剩余设计边界

库存方式、SC 冷却后交付热资格、新批次采用生产后共同压力和该批实际温度，均已由 PM 确认，不再作为未决玩法重复询问。推荐的按热缺口再热、按显热散热及按量泄放，是保持现有总额/无固定汽种优先的局部确定性方案，需 PM 在正式卡选定实现顺序即可。若改为汽种优先或强制两池瞬时等温，会改变热量分配和交付时机，才是新的真实玩法取舍；现任务无需引入。

本模型消除“已有 NORMAL 随压力跨线整池失效”的原因，不能仅靠静态设计宣称现场已经修复。当前只读交付完成，等待正式 01F 卡授权实现及定向失败/通过证据。
