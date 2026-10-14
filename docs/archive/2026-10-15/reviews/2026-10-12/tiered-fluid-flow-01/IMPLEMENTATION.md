# TIERED-FLOW-01 执行交付

2026-10-12，唯一执行者 `/root/tiered_flow_impl`。交付本树未提交实现与原始证据，供 PM 和本次独立组合审查核读；不表示用户验收、合入 main 或关闭人工门。

## 来源与范围

- 工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-fluid-network`，分支 `codex/tiered-fluid-network-flow`；功能基线 `07b2c1e`。冻结时 HEAD 为 PM 文档提交 `da3aa4101c939511a714f6be5309da774aa3e518`，执行者没有 Git 写操作。
- 实际读取本树 AGENTS、活动 flow-01 卡、已确认 spec、RESTRICTION-STUDY；实际应用 `minecraft-modding`、`minecraft-testing`、TDD、systematic-debugging、verification-before-completion，以及收到本次审查后读取并应用 receiving-code-review。唯一原写者及 Git/文档禁令覆盖技能中的通用提交、派发与重复验证步骤。
- 锁定 MC 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280；未改构建脚本、依赖、SharedFluidFillMixin、美术渲染或像素素材。
- 未写用户 `run/saves`、未生成或修改用户 `run/config`、未同步其他工作树。PM 已完成的 27 文件携带不重复核验。未夹带锅炉 16³候选。

## 实际实现

仅五个新 ID：`reinforced_fluid_pipe`、`ceramic_fluid_pipe`、`industrial_fluid_pump`、`reinforced_fluid_valve`、`ceramic_fluid_valve`。二/三级管阀默认 8192/32768 mB/t，唯一泵默认 16384 mB/t@256 RPM、16 SU/RPM；流量按绝对转速线性缩放并封顶，反转改变流向，停转不搬液。阀用 Create 开度控件，红石优先关闭；缺失红石边界区块安全关闭并显示原因。资源含朝向/连接模型、物品、唯一掉落、五种工作台配方和双语说明，复用现有/原版纹理，未新增 PNG。

服务端统一调度，以有界拓扑缓存、加载/邻格事件和配置快照失效处理路线；不每 tick 扫世界。搜寻默认每侧 256 管段、最多 4096 节点，环路去重，合法轴向入口先于去重，端点按实际能力面独立查找。泵、源和目标使用确定性轮转；共享物理管段/串泵共用同 tick 预算。管段不持液，泵持有真实短收余量：输入侧按实际扣源收费、输出侧按实收收费，显示流量只计实收。SIMULATE 不修改库存、缓存或额度。

现有反应堆口默认 2048；锅炉冷热口默认 2048，给水/蒸汽口默认 4096，并有两个独立 SERVER 配置/Settings 字段。主被动与专用交易共用各物理口原账本。原锅炉尺寸 5..11、温压、双汽/HU、核热供盆/炉和汽轮机性能保持。

热冷却剂 source/flowing 与 SC 的标准能力仍可读库存，但 fill/drain（含模拟）均拒绝；原生 Create 网络在真实 drain 前另有禁运闸门。机器/Create 普通和创造罐复用同份库存，工业交易只获得内部、单次、有限额且逐操作复核路径/实体身份的许可；未知库存不因提供普通能力获受限授权。PSI 内部同库存 wrapped 也受标准禁运，普通介质与连接/keepAlive 保持。创造罐沿原无限源/销毁槽语义，不纳入有限守恒证明。SC 不再贡献原生锅炉输送压力或邻罐直推，普通 steam 原路径保留。

工业端点在能力查询前拒绝缺失 BE。Create 成员罐先查当前已加载、同类型的实际控制器，再取当前控制器标准能力，避开成员旧缓存；每次真实操作复核成员/控制器绑定和身份。读块/实体只用已加载表，阀红石通过安全 SignalGetter 保留弱/强信号算法并限制二阶读取。

泵掉落最多一个携液项、堆叠上限 1；成功放置消费携液数据，即使创造不扣物品数量也不能重复搬同份液。原生 Ctrl 选取追加 NBT 的 removeComponentsFromTag 去除 Cache，世界保存仍保留。持液泵不随构造移动。当前保存/重载、配置缩小保留既有缓存已覆盖；未研究跨版本迁移。

受影响的原 Ponder 热液/SC 管泵与必要驱动已最小替换，普通冷液/排汽保持原路；没有新配方教学或教程页。旧 GameTest 正例按专用运输更新，保留禁运、守恒、热工、双轴停止与生命周期断言。

## 验证与原始索引

原始证据根：`build/reports/tiered-fluid-flow-01/`。每个日志保留真实任务输出；带 PROCESS_EXIT 的结束值为启动命令捕获的退出码，不能仅凭 Gradle 0 判断真实测试成功。

| 证据 | 实际结果/含义 |
| --- | --- |
| 01/02 RED、03 GREEN | 纯预算/短收核心先缺失失败，随后 7 项通过；泵转速、共享段、串泵、模拟和缓存守恒 |
| 05-capability-red、06-port-red、08/09 GREEN | 标准热罐禁运与新端口默认的真实/定向 RED，随后真实 1 项、相关 JUnit 45 项通过 |
| 18-psi-red | 原 PSI 标准能力可抽受限液的实质 RED；后续 28 中真实同库/零写负例通过 |
| 13/15 | 阶段性 9/9、13/13，自然退出 0；保留此前失败和修复过程，不代替最终来源绑定 |
| 26-frontier-reload-focused | 真实卸载/当前保存恢复 1/1，自然退出 0；修复缺块前沿在 FULL 晋升后自动重建 |
| 28-expanded-industrial | 正式独立工业域非零注册 20/20，自然保存退出 0 |
| 33-turbine-fixtures | 原汽轮机代表域 12/12，自然保存退出 0；性能、双轴、外源、保存与停止保持 |
| 34-review-focused | 3 项中方向/多面与创造放置/Ctrl完整后处理两项通过，边界阀失败；整轮 exit1，不记 3/3 |
| 35-review-focused | 边界阀与恢复后的整机输入测试 2/2，自然保存退出 0；边界阀真实 Unload、二阶缺块零强载/关闭、恢复有限 6000 |
| 39-turbine-budget-ample-capacity | 仅原三档方法 1/1，自然退出 0；31 个同 tick 样本两源合计 216、InputUsed216、exhaust216<capacity432、两口 remaining0、缓存0，turnover恢复1 |
| 40-final-targeted-junit / 40-targeted-xml | 最终定向 83/83，失败/错误/跳过 0，exit0 |
| 41-steam-selection-focused | 原六方法实际注册 6/6，自然保存退出 0；原双路/背压、NORMAL 128+3968 共4096、SC 专用256携热、混合保存、再热专用128 |
| 42-heat-loop-fixtures | 正式原换热器回路域 4/4，自然保存退出 0；普通原生冷液/分流与专用热侧，近满 source4000+预填1000→source1000/热4000/总5000 |
| 43-final-build / 43-full-build-xml | 首次完整标准增量 build：543 项中 542 过、1 失败、0 错误/跳过，exit1；唯一失败是原 P1Balance 默认字符串128 |
| 44-final-build-green / 44-full-build-xml | PM 精确扩卡改该断言2048后，第二次标准增量 build：99 XML、543/543、失败/错误/跳过0，21秒 exit0；jar/assemble UP-TO-DATE，复用同一生产包 |

28 正式域的重要实际行为：10 个真实反应堆冷口、两条有效动力路线连续五 tick 每 tick 20480 mB、有限总量160000；共输入二级干线8192=目标实收2000+泵缓存6192，第二泵不能再抽源，源7808、总16000；锅炉水4096/热2048/同 tick 标准冷1024+专用冷1024/SC4096；SC过滤停流后恢复交付3904到有限满罐8000；热/SC 原生泵禁运及 water/cold/ordinary steam 原生通过；普通/创造罐、PSI、未知受限接收拒绝、绑定撤销/重新成型、关阀/红石/反转/断管恢复、持液泵拆放与保存。源/目标/缓存有限守恒与模拟零写分别断言，未拿创造无限罐充当有限守恒。

首次完整 build 确实失败，因此本批不是“唯一一次完整 build”。明确的旧默认断言失败触发 PM 批准第二次，不先额外重复同类测试，不 clean、不 --rerun-tasks。实际完整命令均为 `./gradlew.bat build`。最终定向命令包含 Industrial、BoilerState、BoilerDualSteamInventory、ReactorCoolantPortFlowBudget、ReactorCoolantLedger 及受影响资源/Ponder合同；原命令参数在对应日志/本卡中可核。

必要失败与夹具边界：10/11 初始未绑定端点须登记几何候选，交易时仍需当前有效 owner；12 使用早期 RED 自动生成的128测试配置导致吞吐不足，经批准仅备份该 build 测试域配置，再由生产新默认自然生成2048，13/28通过。21 冷口共享测试原回调与正式 Post 不同 tick，改同 gameTime 高优先级观测，保留1024+1024实质断言。82格卸载夹具仍在 owner31 到 MAX44 的生成传播范围，改至少14区块/224格后才有真实 Unload；25有卸载但前沿恢复失败，26关闭。34 边界阀重载夹具的 forced31 会保留邻块 FULL，改 region0=33 后才准确覆盖二阶缺块，35关闭。30～32旧汽轮机夹具缺实体、密排齿轮覆壳、记数时序失败，33真实通过。27/36 为新测试局部类型/import编译失败，随后关闭。全部失败原日志保留。

37 虽 Gradle 0，却为 `No test functions were given!`，不计任何通过。六汽种方法实际模板域为 boiler_inventory，经 PM 批准临时筛选才在41真实注册6项；正式注解已恢复。

早期128配置 SHA256 `501c79b8cd3a1225a88e8ba1448460442714cb1f5e3ca86866fa8ad65f21cb3f`；自然生成的2048测试域配置 SHA256 `fafa3d63483bcddde93f27052f8b0755f1fd0ae8f3328f24e541a5a6e4646192`。仅 build 测试域，用户配置未动。review域临时 turnover2已恢复1；两组测试配置实际锅炉2048/4096。

## 冻结制品与文件清单

- JAR：`E:/MyMC/NewMod/Create_NuclearIndustry-fluid-network/build/libs/create_nuclear_industry-0.1.0.jar`，2,790,597 bytes。
- SHA256：`364ec4277fc8c8b8ca2a99ef114a86ab17e5fff73a311b905c18d4d21edbf053`。
- `source-manifest.json`：102 条本批 src/tools 路径、每文件 SHA256/大小。显式包含被存量 `config/` ignore 规则忽略的新 `src/main/java/com/iksxh/create_nuclear_industry/config/IndustrialFluidConfig.java`，PM 需准确加入此单文件。
- `jar-verification.json`：按每个源文件前缀展开共122次 class/资源字节比较、117个独立entry，mismatches为空，与独立review的117项同范围。5次重复来自 PumpBlock 前缀带入 PumpBlockEntity、Transfer 前缀带入 TransferPermit、ValveBlock 前缀带入 ValveBlockEntity及其两个内部class；重复entry在JSON准确列名。这是比较次数，不是122个独立文件。临时 industrial_chunk/industrial_review 域条目为零。
- `final-verification-summary.json`：最终 99 XML/543 项与配置身份摘要。执行报告与任务报告单列，不纳入自引用 SHA 清单。
- 原临时注解恢复，build临时模板仅在核绝对路径位于本树 build/data 后显式删除。未更改正式模板域或构建脚本。
- 自动生成的 `logs/debug.log`、`logs/latest.log`、`tools/ponder/__pycache__/` 不属于交付源；PM 的文档/审查/清单改动不属于执行者实现，未覆盖或清理这些差异。

## 验据限制与人工门

已真实运行的旧代表域为汽轮机12、汽种六方法6、换热器回路4。BoilerMultiportStop、BoilerTurbineFlow、P1Coolant05Coverage、ExtensionBoiler 等其余旧方法仅完成与新规则直接相关的源码适配/编译；不声称这些长工况全部已绿。受限SC原“无泵/直邻罐”正例已改为有动力专用路线，旧方法名属于历史，不能据此理解为保留原生禁运路线。

未实现项目均是已冻结后置/取消范围：管网压力、泄漏、破裂、管理员功能、新工业端口、分级泵、锅炉16³及旧版本迁移。初期模型/复用材质仍需客户端观察。真实客户端放置/扳手/开度/反转/护目镜、热/SC禁运与普通介质、普通/创造罐、端口联动、现有世界保存重进及受影响Ponder播放，保持 PM `CLIENT-CHECKLIST.md` 人工门；服务端完整Ctrl链验证不等于人工按键/视觉验收。未自行验收、提交、合 main 或推进其他批次。
