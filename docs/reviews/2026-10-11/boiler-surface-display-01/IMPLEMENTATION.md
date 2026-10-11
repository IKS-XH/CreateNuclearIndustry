# ART-BOILER-05-L1 实施交付

2026-10-11，执行者交付未提交候选；基线 `951935fe7043c879ebad2225fa7e438da41ce7d3`，唯一实现目录 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`。严格使用实施卡 12 个交付路径及本报告、专属原始证据目录；未执行 Git 写操作。

服务端只在原 `currentForm()` 缓存决策后投影，`invalidateForm()` 独立撤销并即时同步；编码与公开查询只读取冻结描述。完整表面默认 casing，按 Form 覆盖九种正式注册 ID，包含底机、顶阀及底边热口的两个外面，内部 sections 排除。实例 UUID 不保存，revision 只随几何/有效性/角色变化；字段仅进入原 clientPacket。

客户端使用锅炉独立不可变快照与状态机，复用现有原生 RenderChunkRegion AT。客户端 onLoad 不清首包，卸载暂停与真正移除退休分离；包水位、对象 token 和实际所需区块实例共同证明身份。每 tick 只核实已知 ChunkPos 和 owner 在区块 BE 表中的槽位；不读世界方块、不强载、不扫描未知 BE。先原子发布，再去重刷新旧/新可靠 bounds 外扩一格的已加载区段。同描述不重建快照或刷新。

证据统一位于 `build/reports/art/ART-BOILER-05-L1/`：

| 验证 | 实际结果 | 原始证据 |
| --- | --- | --- |
| 首轮行为 RED | 编译成功；合法完整 5³ 紧凑信封因接口缺失无法解码，1/1 断言失败，exit 1 | `01-red.log`、`01-red.xml`、`01-red.exit` |
| 定向 JUnit 与增量 assemble | 表面 14/14 + 原 BoilerState 15/15，共 29/29；零失败/错误/跳过，25 秒 exit 0 | `03-green.log`、`03-green.exit`、四份 `TEST-*.xml` |
| 实际最大 NBT | 32³ 表面 5768 块、6144 面，错位跨 9 区块；5978 bytes，低于 16 KiB | `nbt-budget.txt` |
| 唯一真实隔离探针 | 1/1 通过，24 秒 exit 0，正常保存关闭 | `04-probe.log`、`04-probe.exit` |
| 编译与制品绑定 | 12 源 SHA；16 生产 class 与 JAR 逐项一致；3 测试 class；模板 source/install/JAR 一致 | `manifest.json`、`source-sha256.txt`、`compiled-abi.txt` |

探针使用既有同包 `ExtensionBoilerGameTests.build(helper, 6, 6, 8, 2)`，专属 namespace/directory `boiler_surface_display_probe`；核实原生 update tag/packet、绝对隔层、底边双面、无水热/红石/坏换热器运行配置不推进几何版本、拆窗即时撤销/恢复、连续无效去重、坏锅炉配置恢复、新实例新 generation、server/portable 保存无显示字段。所有配置在 finally 恢复，没有启动或再复制用户存档。客户端生命周期为真实状态机行为测试，未以其声称客户端模型/CT或视觉通过。

最终 JAR SHA256：`f95f60040b6e82aff3a907cd3f7f447c2e1f4f5505ba203b7590d7d671632e93`。编译 ABI 的 Member 包含合同全部十个访问器：`dimension/ownerPos/ownerGeneration/revision/min/maxInclusive/partitionY/pos/expectedBlockId/outwardFaces`。

首个编译尝试错误调用不存在的 MinecraftServer logger API，已按仓库既有 `LogUtils.getLogger()` 修正；保留 `02-compile-error.log/.exit`，该错误没有启动探针。成功编译包含现有 NeoForge 弃用警告；新隔离服务器首次启动记录缺少 server.properties 后采用默认值，探针断言和退出仍正常。未为这些日志重跑业务。

已实际阅读并应用 `minecraft-modding`、`minecraft-testing`、`executing-plans`、`test-driven-development`、`systematic-debugging`、`verification-before-completion`；以实际 MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6 为准。技能全量/提交/额外工作目录步骤由项目治理 5.1/5.2 与精确写集覆盖。

必要范围差异见 `controller.diff` 与 `scope-status.txt`：原玩法、账本、tick结算、端口及配置保持原语义；原反应堆 L1/L2、构建/AT、美术和教学路径未修改。已有日志 dirty 与 `tools/ponder/__pycache__/` 保留。等待 PM 安排一次独立规格/质量窄审和 Git 集成；后续美术消费及用户视觉门独立保留。
