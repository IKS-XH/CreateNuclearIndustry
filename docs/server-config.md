# 服务端配置

更新：2026-10-18。本文统一记录当前main配置，锅炉16³尺寸上限与辐射基础已合入。数值以实际配置源码和实例TOML为准，状态见[STATUS](STATUS.md)。

## 文件和生效

配置类型为NeoForge SERVER。锁定21.1.219默认在实例config生成，世界serverconfig中同名文件优先覆盖。main开发实例在run/config，候选各有自己的run/config和世界。退出世界/服务器后修改实际生效文件，重启并用护目镜核对；已有显式值不会因代码默认改变被覆盖，不承诺所有键在线重载。

| 文件 | 范围 |
| --- | --- |
| create_nuclear_industry-server.toml | reactor节：尺寸、热工、燃耗、损伤、冷却 |
| create_nuclear_industry-reactor-automation-server.toml | 管理员及余量闭环，顶层键 |
| create_nuclear_industry-industrial-fluid.toml | 专用管网，顶层键 |
| create_nuclear_industry-boiler.toml | 锅炉，顶层键 |
| create_nuclear_industry-heat-exchanger.toml | 换热/冷凝/盆炉核热，顶层键 |
| create_nuclear_industry-turbine.toml | 三档汽轮机，顶层键 |
| create_nuclear_industry-storage.toml | spentFuelStorage节：贮存架 |
| create_nuclear_industry-radiation-server.toml | general/sources/shielding/dose/protection节：辐射与防护 |

容量调小不删除现有物料，超额拒收；机器合法性还检查跨字段关系，不能只看单值范围。材料数量和工时由数据配方控制，不在设备配置复制一份。

## 反应堆

键位于reactor节，来源为[P1ServerConfig](../src/main/java/com/iksxh/create_nuclear_industry/config/P1ServerConfig.java)。

| 键 | 默认 | 单位/意义 |
| --- | ---: | --- |
| minDimension / maxDimension | 5 / 16 | 三边各自外尺寸，组合5≤min≤max≤16，允许跨区块 |
| baseHeatPerFuelBlockHuPerTick | 1 | 每有效燃料主体格基础HU/t |
| fuelBurnTimeHours | 3 | 完整组件基准燃烧小时，高度/控制/反馈/损伤另乘 |
| coolantAbsorptionHuPerMb | 0.5 | 每mB工质吸热HU，反应堆与换热器共用 |
| perPortFlowMbPerTick | 2048 | 每个冷/热物理口共享mB/t额度，0为禁流 |
| coolantCapacityPerEmptyBlockMb | 1000 | 每可计空气格冷热共享mB容量 |
| damageHeatThresholdHuPerTick | 0.25 | 真实未覆盖热负荷的损伤阈值 |
| damageRatePerTickHuLoad | 0.0000005 | 完整度扣除速率/(tick·HU/t) |
| fuelColumnDamageHeatMultiplier / fuelColumnDamageBurnMultiplier | 2 / 3 | 全损伤端点，须1＜热倍率＜燃耗倍率 |
| damageTransferRate | 0.25 | 相邻失效燃料的残余热传播比例 |
| controlRodFailureThreshold | 0 | 控制棒卡死完整度阈值 |
| meltdownTriggerFraction / meltdownCountdownTicks | 0.2 / 900 | 有效失效覆盖比例与倒计时tick |
| controlResponseExponent | 1 | 插深响应指数 |
| overclockHeatMultiplier / overclockBurnMultiplier | 10 / 10 | 反馈热/燃耗倍率上限 |
| overclockFeedbackGain / overclockFeedbackExponent | 0.15 / 0.5 | 相邻反馈参数 |
| totalHeatMultiplierCap | 20 | 全堆新生裂变热限幅 |

总新生裂变热汇总限幅后向上取整，缓存余热不再次取整。默认100HU/t对应200mB/t，不把管理员维护自耗当冷却需求；无隐藏全堆流量封顶。完整算法见[反应堆合同](reactor-local-control-revision-design.md)。

## 管理员

来源为[ReactorAutomationConfig](../src/main/java/com/iksxh/create_nuclear_industry/config/ReactorAutomationConfig.java)，行为见[管理员合同](reactor-automation.md)。

| 键 | 默认 | 意义 |
| --- | ---: | --- |
| adjustIntervalTicks / displayAverageWindowTicks | 10 / 40 | 调棒周期/显示平均窗口tick |
| maxLiftPercentPerAdjustment / maxInsertPercentPerAdjustment | 1 / 5 | 每次升功率提出/降功率插入百分点 |
| powerDeadbandFraction | 0.02 | 恒功率比例死区 |
| maxTargetHeatHuPerTick | 1000000 | GUI目标上限HU/t，不是物理功率上限 |
| startupColdFraction / startupMaxHotFraction | 0.25 / 0.75 | 启动库存条件 |
| warningColdFraction / warningHotFraction / tripHotFraction | 0.15 / 0.85 / 0.95 | 库存预警/停堆阈值比例 |
| warningHeatThresholdFraction | 0.5 | 列积热预警比例 |
| resetStableTicks / controlLeaseTicks / armingTimeoutTicks | 100 / 20 / 40 | 复位稳定/控制租约/建立联锁超时tick |
| burnerMaintenanceHuPerTick | 1 | 真实核热值班HU/t，须正有限 |
| defaultTargetStressReservePercent | 20 | 新管理员目标应力余量%，0～90 |
| defaultMinimumOperatingHeatHuPerTick | 2 | 新管理员最低毛HU/t，默认维护1＋净输出1 |
| stressReserveDeadbandFraction / stressFilterTimeTicks | 0.03 / 40 | 余量比例死区/滤波时间常数tick |
| stressPiProportionalFraction / stressPiIntegralFractionPerSecond | 0.25 / 0.01 | PI增益，乘当前堆可达最大HU/t；积分为每秒 |
| stressEstablishmentTimeoutTicks | 1200 | 当前网建立/读档重建等待tick |

机器显式模式/目标保存于自身状态，默认参数不覆盖已有设定。最低值保留默认2时按维护成本＋1初始化；PI/滤波暂态重建，保存续接不恢复旧积分。

## 专用流体管网

来源为[IndustrialFluidConfig](../src/main/java/com/iksxh/create_nuclear_industry/config/IndustrialFluidConfig.java)。

| 键 | 默认 | 意义 |
| --- | ---: | --- |
| reinforcedFlowMbPerTick / ceramicFlowMbPerTick | 8192 / 32768 | 二/三级管阀节点共享mB/t额度 |
| pumpFlowMbPerTick / ratedRpm | 16384 / 256 | 唯一工业泵额定mB/t、最大周转mB及额定RPM |
| stressSuPerRpm | 16 | SU/RPM，256RPM默认4096SU |
| searchDistance / maxNetworkNodes | 256 / 4096 | 每侧最大搜索管段/拓扑节点数，不强载区块 |

泵流量为floor(额定流量×clamp(abs(RPM)/额定RPM,0,1))，再受路径/阀/源/物理口/接收量限制。串泵不叠额度，独立路线可并联。热液/SC经专用管网，Create罐保留；锅炉汽口可主动接合法工业管，无须外置泵。未实现管网压力。

## 锅炉

来源为[BoilerConfig](../src/main/java/com/iksxh/create_nuclear_industry/config/BoilerConfig.java)。main默认5～16，三边独立判断，允许跨区块；主目录现有配置已备份后仅将尺寸范围同步为[5,16]。

| 键 | 默认 | 意义 |
| --- | ---: | --- |
| dimensionRange | [5,16] | 外尺寸闭区间，两个整数须满足5≤min≤max≤16，可缩小合法范围 |
| waterCapacityPerCellMb / steamCapacityPerCellMb | 2000 / 2000 | 每有效水区/汽区格mB；双汽共用汽容量 |
| coolantPortFlowMbPerTick / waterSteamPortFlowMbPerTick | 2048 / 4096 | 冷热口/给水汽口mB/t，主被动及专用交易共用 |
| pairHeatHuPerTick | 18 | 每有效热力回路HU/t上限 |
| boilingTemperature / supercriticalTemperature | 1 / 2 | 游戏归一温度，后者须更高 |
| wallHeatCapacityHuPerWaterCell | 1600 | 每水区格HU/温升 |
| waterSpecificHeatHuPerMb / steamSpecificHeatHuPerMb | 0.1 / 0.2 | HU/mB/温升 |
| vaporizationLatentHeatHuPerMb | 0.7 | 汽化潜热HU/mB |
| supercriticalPressure | 0.5 | 新批次SC归一炉压门槛，不改写已有库存 |
| outputMinPressure | 0.6 | 默认出汽下限，[0,1]对应控件0～100% |
| valveOpenPressure / valveClosePressure | 0.9 / 0.8 | 安全阀开/关炉压 |
| valveFlowPerSteamCellMbPerTick | 32 | 每汽区格泄放mB/t |
| idleWaterCoolingHuPerCellPerTick / idleSteamCoolingHuPerCellPerTick | 0.9 / 0.1 | 无实际收热时每格散热HU/t |

冷热容量由炉内换热器数×其hotCapacityMb/coldCapacityMb派生，有效回路取换热器/再加热段最小值。出汽下限不是目标炉压或安全阀阈值；普通/SC各存mB/HU、共总容量，汽口只取所选库存，不转换/降级。既有合格SC可低于生产压力门槛后排至共同下限，实际热量仍扣账。

## 换热器

来源为[HeatExchangerConfig](../src/main/java/com/iksxh/create_nuclear_industry/config/HeatExchangerConfig.java)。

| 键 | 默认 | 意义 |
| --- | ---: | --- |
| heatLevel / huPerLevel / bufferTicks | 18 / 1 / 40 | 锅炉热级、HU/(级·tick)、储热/余热tick |
| hotCapacityMb / coldCapacityMb / maxLineLength | 4000 / 4000 / 16 | 单台热/冷mB、同向直列台数 |
| basinHeatLevelEquivalent | 2 | 工作盆/烧结炉固定耗热等效热级，共用费用 |
| condensationRateMbPerTick / condensationRecoveryPermille | 54 / 1000 | 蒸汽mB/t、回水千分比 |
| condensationSteamCapacityMb / condensationWaterCapacityMb | 4000 / 4000 | 冷凝输入/输出mB |
| condensationSnowMeltAfterMb / condensationIceMeltAfterMb | 100000 / 100000 | 实际冷凝累计量，融水mB阈值 |
| condensationPackedIceMeltAfterMb | 900000 | 浮冰融水mB阈值 |
| condensationWaterEvaporateAfterMb | 100000 | 水源蒸发mB阈值 |

吸热密度复用reactor.coolantAbsorptionHuPerMb。盆/炉存在即按basinHeatLevelEquivalent×huPerLevel持续耗HU，不检测工作状态，断流/堵塞立即停热、不用bufferTicks续烧；默认4mB/t。冷凝正上方仅水源/雪块/冰/浮冰/蓝冰有效，蓝冰不消耗，其他源按实际冷凝量变化，不认可流水/雪层/含水方块。

## 汽轮机

来源为[TurbineConfig](../src/main/java/com/iksxh/create_nuclear_industry/config/TurbineConfig.java)。三档键使用short/medium/long前缀。

| 后缀/键 | 默认 | 意义 |
| --- | ---: | --- |
| RotorCount / Diameter | 3/6/9；3/5/7 | 转子节数/外径，三档组合才合法 |
| RateMbPerTick | 54/108/216 | 全机进/排汽mB/t上限 |
| MaxEfficiencyMultiplier | 1.2/1.5/1.8 | 满流量效率 |
| rpm / suPerMbPerTick | 256 / 32768 | RPM、每mB/t平均实际排汽对应基准SU |
| smoothingTicks / turnoverTicks | 40 / 1 | 流量平均窗口/额定量周转tick |
| minEfficiencyMultiplier / minimumOperatingFlowRatio | 0.5 / 0.3 | 启动点效率/额定流量门槛比例 |
| inletPortFlowMbPerTick / exhaustPortFlowMbPerTick | 256 / 256 | 单进/排物理口mB/t，全机上限仍优先 |

低于额定30%仍耗汽排汽但无动力；达到后从0.5线性升至满流量倍率。两端共享唯一SU，堵塞保留微周转，断汽按窗口归零。代码仍声明的frontShare已废弃、不控制两端份额，不列作有效调节项。

## 贮存架

create_nuclear_industry-storage.toml中的spentFuelStorage.rackSlots默认16、合法1～16。容量降低保留全部已有桶并允许取出，达到/超过新容量拒收；不改变单桶载荷或封存配方。来源为[SpentFuelStorageConfig](../src/main/java/com/iksxh/create_nuclear_industry/config/SpentFuelStorageConfig.java)。

## 辐射与防护

来源为[RadiationConfig](../src/main/java/com/iksxh/create_nuclear_industry/config/RadiationConfig.java)及[RadiationParameters](../src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationParameters.java)。单位D是游戏剂量，不代表现实医学单位；场强在穿戴减免前限幅，吸收强度在穿戴后计算。

| 节.键 | 默认 | 单位/意义 |
| --- | ---: | --- |
| general.enabled / sampleIntervalTicks / detectionRadiusBlocks | true / 20 / 32 | 启用、结算tick、检测半径格（1～64） |
| sources.usedFuelDosePerSecond / spentFuelDosePerSecond / accidentWasteDosePerSecond | 1 / 2 / 4 | 每份实际载荷1格内D/s；事故废料供第二批接入，不因配置项存在就已注册 |
| sources.maxFieldDosePerSecond | 10 | 穿戴前总场强D/s上限 |
| shielding.caskTransmission / rackTransmission / reactorTransmission / blockTransmission | 0.05 / 0 / 0 / 0.05 | 合法桶、完整架、完整堆对外、每格屏蔽路径的向外比例 |
| dose.safeAbsorptionPerSecond | 0.01 | 不超过该D/s视为安全，可开始恢复等待 |
| dose.maxDose / warningDose / symptomDose / damageDose | 100 / 25 / 50 / 80 | 剂量上限、预警、虚弱/迟缓、高剂量伤害阈值D |
| dose.recoveryDelayTicks / recoveryPerSecond | 100 / 1 | 连续安全等待tick、恢复D/s；离线暂停 |
| dose.damageIntervalTicks / damageHealthPoints | 40 / 1 | 高剂量伤害周期tick、每次生命值点数 |
| protection.head / chest / legs / feet / maxReduction | 0.25 / 0.30 / 0.25 / 0.15 / 0.95 | 按实际穿戴部位相加的吸收削减及总上限 |

加载/重载时整组原子校验，非法组合整组回退默认并节流告警。数值须有限非负，比例在[0,1]，阈值满足warning＜symptom＜damage≤max；结算/伤害间隔正数、恢复等待非负。关闭辐射停止新增剂量和伤害，已有剂量按安全规则恢复，物料已使用历史不清除。世界动作事故配置在第二批候选交付后另列，不把远期参数写作main生效项。
