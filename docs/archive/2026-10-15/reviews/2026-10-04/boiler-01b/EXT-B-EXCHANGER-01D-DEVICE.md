# EXT-B-EXCHANGER-01D 设备实现交付

候选工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；本报告仅记录执行者 C 的代码与测试交付，最终验收由项目经理处理。未执行 Git 写操作，保留同工作树内锅炉、素材和文档的并行改动。

## 实现

- 正面 `FACING` 仅输出冷液，背面仅输入热液；内部连接面、侧面、上下及无方向查询不暴露能力。输入拒绝冷液，输出拒绝热液和填充。
- 新增水平同向直列瞬时视图，最多 16 台；超限、区块未加载或任一成员不可 tick 时整列拒绝流体事务及供热。每台仍保存本地 4000/4000mB、HU 储备、分数流量、世界 tick 和绝对断流期限；拓扑变化不搬账。
- 外部注入和冷液抽取按成员真实空位、库存顺序分配；换热先预检整列热液和冷空位，再完成等体积扣热/加冷，最后通知库存变化。工作机只补自己的 HU，原生 Create 与专用锅炉都走同一整列流体边界。`SharedFluidReceiver` 的整列身份和实时总空位供现有 Create 分流计划复用。
- 放拆、扳手旋转、成员加载/卸载或停止 tick 会撤销拓扑能力；旧句柄的快照包含全体成员身份，不能在恢复后重新生效。护目镜显示组库存/台数，直列不可用时明确显示本机库存。
- 真正管路故障的根因是 Create `FluidPipeBlock` 在机器能力尚未就绪时把接向机器的面关在方块状态中；仅撤销能力缓存或传播流量不会重开该面。拓扑变化及首次全列可 tick 后，设备对相邻已加载 Create 管调用锁定版 `updateBlockState` 重算接面，再传播流体连接。先管后机、拆分恢复及真实两端泵管均有世界内断言。

## 测试写集

- `HeatExchangerStateTest` 新增本地空热、冷满时使用远端库存的质量和已付 HU 守恒测试，保留原有低流量、期限与旧 NBT 测试。
- `ExtensionHeatExchangerGameTests` 按前后定向口适配原生锅炉和生命周期用例；`ExtensionHeatExchangerLoopGameTests` 保留真实 Create 泵管网近满守恒断言，热液改从合法背面输入。
- `ExtensionHeatExchangerChainGameTests` 与独立 `chain_empty.nbt` 六项覆盖三机定向接口、容量、错误流体、反向/侧邻、17 台超限、真实扳手旋转及中间拆分、物品只携本机份额、非首成员停 tick 后旧端点句柄失效、先管后机与拆分恢复连接、三机两端 Create 泵管和原生锅炉中央机远端取热、本地满冷改用其他成员空位。

## 验证状态

本执行者未运行 Gradle，遵守本批由 D 独占构建与 GameTest 的约定。已做写集 `git diff --check`（仅 Git 换行提示，无本写集空白错误）、中英语言 JSON 解析及新模板 NBT 头部尺寸核对（20×5×10）。D 的共享日志记录：`EXT-B-BOILER-01B-DEVICE-junit-assemble.log` 增量 assemble 与定向 JUnit 成功；`EXT-B-BOILER-01B-DEVICE-heat-exchanger-gametest.log` 14/14、`EXT-B-BOILER-01B-DEVICE-heat-loop-gametest.log` 4/4；最终修正后 `EXT-B-BOILER-01B-DEVICE-heat-chain-gametest.log` 重新编译生产 Java 并 6/6 通过。上述链测试先前失败的真实入口诊断为：源罐热液 8000mB、双泵均 256rpm、入口管 `connection=false`、三机库存皆零；接面修复后通过。D 后续锅炉整合与最终 assemble、客户端视觉/接管仍由共同验收流程记录，本报告不代替人工门。

实际读取并应用：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 用于 NeoForge 能力生命周期与 Create 负载接入；`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md` 用于 JUnit/GameTest 真实世界夹具和分层验证。按仓库锁定 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82 实施，未改技术栈。
