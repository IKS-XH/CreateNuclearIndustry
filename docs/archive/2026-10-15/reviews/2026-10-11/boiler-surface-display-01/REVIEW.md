# ART-BOILER-05-L1 独立规格与质量窄审

2026-10-11。结论：**通过本批独立窄审，未发现必须整改项或阻断交付的合同遗漏。** 本结论只适用于冻结的锅炉成型表面接口，不代表美术 CT、客户端模型或用户视觉验收通过。

审查者为独立执行者，仅写本报告；未改实现、测试、构建、治理或其他文档，未执行 Git 写操作、派发、测试/构建重跑或启动用户世界。审查目录为 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`，冻结实施基线为 `951935fe7043c879ebad2225fa7e438da41ce7d3`。已实际读取该树 AGENTS、治理 5.1/5.2、活动实施卡、`docs/art/ART-BOILER-05-L1-INTERFACE.md`、实施报告及 Minecraft modding/testing 技能；按实际 MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6 审查。

## 源码与合同核对

下表路径均相对本审查目录，Java 主源码根为 `src/main/java/com/iksxh/create_nuclear_industry/`。

| 核对点 | 实际依据与结论 |
| --- | --- |
| 唯一逻辑接入与玩法边界 | `boiler/BoilerControllerBlockEntity.java:73-80,107-144,349-380`：服务端在原缓存决策后调用纯投影；缓存未脏的正常 tick 返回路径不投影。独立失效即时同步且同几何去重；write 仅编码冻结值。与基线差异仅新增显示字段、缓存投影、clientPacket 和生命周期通知，原 tick 热账本结算及端口/压力语义未修改。 |
| 合法运行状态仍成型 | 控制器投影输入仅为原 `cachedForm`，不读运行 Status、Fill 或账本。红石、待液/待热、冷液满、成员 adopt 失败以及坏换热器运行配置均不新增失效条件；原锅炉配置/结构缓存失效才撤销。真实探针 `gametest/BoilerSurfaceDisplayGameTests.java:27-79` 验证待液、红石、NaN 换热器配置不增几何版本，坏锅炉配置撤销后恢复。 |
| 完整纯 Form 表面 | `boiler/BoilerSurfaceDescriptorFactory.java:15-42`：已验证 Form 的窗、四类口、底机、顶阀及独立 owner 覆盖完整外表面，其余为 casing；不读世界块、不调用 inspect，sections 不进入表面。`BoilerSurfaceDescriptor.java:142-153` 保留六向真实外面，棱角及底边热口多面完整。 |
| 不可变与安全预算 | `BoilerSurfaceDescriptor.java:37-68,89-139,155-172`：位置、列表、面集合防御复制；schema/精确类型、有限 dimension、安全坐标、UUID/revision、5..32 各轴、合法 partition、精确数组及合法索引、唯一 owner 均受检。无效信封只保留 owner 有界身份；坏包不能产生刷新边界。wire 每表面一 byte，公开模型仍为不可变成员。 |
| 首包与卸载身份 | 控制器 `:74,117-118,349-380` 保留早于客户端 onLoad 的首包，区块卸载标记延续至随后 invalidate。`boiler/client/BoilerSurfaceSnapshots.java:89-116,149-189` 区分 suspend 与真 remove 退休，并校验当前实例 token、generation、revision 水位及等版本内容；坏包撤销时保留水位，旧实例延迟回调不删除新实例。 |
| 真实区块、槽位及恢复 | `boiler/client/BoilerSurfaceClientEvents.java:73-138,169-214`：更新与 Load 只绑定当前世界中真实 owner；每 tick 只核已知 ChunkPos 和该区块 BE 表中的 owner 槽位。Load 事件中的 BE 列表用于发现/恢复，tick 不扫描未知 BE 或方块。实际缓存对象替换、成员卸载、owner 暂停及重新加载均会撤销/恢复；UNLOADING 标记处理原生 drop 在缓存移除前发事件的次序。 |
| 世界与渲染上下文 | `BoilerSurfaceClientEvents.java:38-68` 清空世界会话并防旧 Unload 回调重新打开会话；`BoilerSurfaceSnapshots.java:22-40` 将真实世界与快照一次原子发布。context capture 只接受当前真实 ClientLevel 或精确原生 RenderChunkRegion 的同一世界对象；旧世界、Ponder、未知包装为空。沿用已有 RenderChunkRegion.level AT，未改构建/AT。 |
| 刷新顺序与去重 | `BoilerSurfaceSnapshots.java:207-242` 仅脏时重建可靠全加载成员索引，剔除歧义，仅返回真正变化的旧/新 bounds；`BoilerSurfaceClientEvents.java:142-162` 先发布再将边界外扩一格，按已加载区块和世界区段裁剪，以 SectionPos 集合去重。同值不重建快照或刷新。未加入 heartbeat/运行租约。 |
| 编译消费及线程边界 | `boiler/client/BoilerSurfaceSnapshot.java:23-39` 与实际 ABI 含合同全部十个 Member 访问器，含绝对 partitionY；公共 BE 不引用客户端类型。描述/快照无世界、BE、库存引用，客户端订阅限定物理客户端；新增非显然算法及边界注释为中文。CT 消费者保持在本写集之外。 |

客户端原生次序同时复用并核对反应堆 L1 冻结的 `Create_NuclearIndustry-reactor-display/build/reports/art/ART-REACTOR-02-L1/api-sources/ClientChunkCache.java:61-68,104-128,141-159,201-218`：drop 的事件先于缓存移除，packet 先安装 BE 后发 Load，缓存换槽及缩视距不能只依赖 Unload。上述新生命周期处理与这些实际路径一致。

## 原始证据及冻结绑定

证据根为 `build/reports/art/ART-BOILER-05-L1/`，本审查复用已通过证据，没有重新运行业务验证。

- 四份原始 `TEST-*.xml`：BoilerState 15、Descriptor 5、Lifecycle 6、Snapshot 3，共 **29/29**；失败、错误、跳过均为 0。`03-green.log` 记录增量 assemble **25 秒成功**，`03-green.exit` 为 0。
- `04-probe.log` 仅启用 `boiler_surface_display_probe`，**1/1 required 真实 BE 探针通过**，随后全部维度正常保存、服务端正常关闭，**24 秒成功**；`04-probe.exit` 为 0。探针复用原同包搭建 helper，未改旧测试或公共注册/构建。客户端事件处理的依据为源码与定向状态机行为证据，未将服务端探针称为真实客户端 CT 测试。
- 保留 `01-red.xml/.log/.exit` 的编译成功后 1/1 行为失败；保留 `02-compile-error.log/.exit` 的中间 logger API 编译错误。它们未被成功结果覆盖，也未当作通过证据。
- `nbt-budget.txt` 与测试源码一致：32³ 实测 **5768 members、6144 outward faces、错位 9 chunks、5978 bytes**，低于 16 KiB；无需新增尺寸或改变玩法配置。
- 对 `manifest.json` 中 **12/12 源 SHA、16/16 生产 class 的编译/JAR SHA、3/3 测试 class SHA、模板 source/install/JAR SHA** 逐项只读核验，零差异。实际 Java21 `javap -public` 输出 77 行与冻结 `compiled-abi.txt` 逐行一致，退出 0。
- 最终 JAR `build/libs/create_nuclear_industry-0.1.0.jar` 实测 SHA256：`f95f60040b6e82aff3a907cd3f7f447c2e1f4f5505ba203b7590d7d671632e93`，与清单和实施报告一致。实际源码差异符合 12 路径写集；已有日志 dirty 和 `tools/ponder/__pycache__/` 未改动。

审查发现：**无阻断项，无必改项。** 可以由 PM 继续本合同的净集成与实物 HANDOFF；美术消费规则及用户视觉门独立保留。
