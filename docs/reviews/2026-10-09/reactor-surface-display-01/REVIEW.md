# ART-REACTOR-02-L1 独立规格与质量审查

2026-10-09。审查者为受派执行审查者，非 PM。审查工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-display`，HEAD `000f8e800ba823c559fcedde60b2048dd34bb516`；候选为其上的冻结未提交实现（包括 untracked）。结论：**未发现需要整改的规格或质量阻断项，建议 PM 记录本轮审查门并交付可消费 API。** 本报告不改变任务状态，也不代表客户端 CT、最终视觉或换热器人工门通过。

已实际读取本树 AGENTS、治理 1.2/5.1/5.2、活动任务卡与 `docs/art/ART-REACTOR-02-INTERFACE.md`，以及 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`。技能用于核对物理端隔离、锁定 API、真实 BE/包与单元证据边界；版本确认为 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6，未套用新版示例。仅写本报告，无 Git 写、代码修改、派发、Gradle、测试或生成器运行。

## 规格与实现核对

1. **显示范围保持。** BE 差异只添加独立描述、扫描缓存投影、客户端包字段和生命周期通知。`ReactorSurfaceDescriptorFactory.project` 仅从既有有效扫描及 `ReactorStructureDefinition.SIZE` 投影实际外表面，并在服务端 `hasChunkAt` 失败时返回不可用；没有另一套成型判定、外壳 BE 或客户端世界扫描。固定 5×5×5、绑定、热工/库存、摘要和护目镜原有逻辑没有改写，显示描述不写服务端持久化。实际仪表 GameTest 核对 98 块表面及内部棒排除。
2. **包及快照边界成立。** `ReactorSurfaceDescriptor.java:34` / `:111` 验证 schema、字段类型、UUID、revision、坐标数组、维度长度、三边预算、完整外壳数量、重复/越界成员、唯一 owner、7 种 ID 白名单及准确朝外面；未知/坏包撤销当前显示。位置使用 `immutable()`，成员/面集合使用 `copyOf`。快照成员由不可变描述投影，索引由 `Map.copyOf` 发布，无 Level、BE、NBT 引用。Member 的 dimension/owner/generation、bounds、pos、外面和 expectedBlockId 足够供消费者检查同身份、同外平面与局部当前 ID；连接材质仍归消费侧。
3. **真实构建上下文有锁定依据。** `ReactorSurfaceSnapshots.java:31` 从原生精确类型 `RenderChunkRegion.level` 或直接 ClientLevel 取得身份，与原子 Publication 中的真实世界对象做 `==` 比较。未知包装、Ponder 与旧世界任务均得空快照；不读光照、远处 BE 或区块。`RenderRegionCache.createRegion` 实际把所属 Level 传入 region。AT 只有开放此 final 字段的一条规则，`build.gradle` 无差异，最终签名为 public final。Create 实物 `getModelData` 为 final 且直接向 `gatherModelData` 传递 BlockAndTintGetter，消费可在一次 gather 作用域捕获并复用。
4. **卸载实物顺序与恢复相符。** 常规 `ClientChunkCache.drop:61` 先发 Unload，再 Storage 替换与 `ClientLevel.unload`；`LevelChunk.clearAllBlockEntities:616` 先遍历 `onChunkUnloaded`，再 `setRemoved`，后清 BE 集合。Create SmartBE 字节码确认 `setRemoved` 总会调用 invalidate。BE `:1344` 显式保留卸载原因，后续 invalidate 继续 suspend；事件先移除 owner 时，后续同 token 回调不退休 generation。事件处理中的 UNLOADING 标记防止 drop 尚留在缓存中的旧对象被同轮 reconcile 重新接纳。
5. **缺失事件路径已覆盖。** `Storage.replace:201` 换槽不发 Unload；`updateViewRadius:141` 只复制新范围内的区块，外部项没有该事件。`ClientLevel.hasChunk:337` 恒 true，候选没有借此证明客户端加载。`ReactorSurfaceClientEvents.java:168` 只对已知描述的有限去重 ChunkPos 使用 `getChunk(FULL,false)` 比较实际实例：缺失/换实例先撤销，真正 owner 仍需包/onLoad/Load 的当前 BE 证据。每描述必要区块最多 25 个，当前固定尺寸最多 4 个，N 个 owner 上界 25N；没有方块查询、世界遍历或强加载。owner 卸载后丢弃无用途区块证据，成员块单独重载可恢复仍可靠 owner 的完整描述。
6. **先后、去重与版本保护成立。** 初始包可能在缓存安装前进入 BE；Load 发于安装之后，实际 BE 列表或 onLoad 会接入已保存的信封。`State.accept:136` 拒绝旧 revision、同 token 变 generation、退休 generation 与旧 token；suspended 水位保护同 generation 重载，真拆除与新实例分别退休/接纳。世界结束后 endedWorld 阻止 Minecraft.level 尚未清空时的旧回调重开会话。`State.rebuild:194` 删除所有重叠坐标归属，dirty 门与同描述去重避免遥测或每 tick 重建索引。`publishAndRefresh:141` 先原子发布，再刷新旧/new bounds 外扩一格的区段并跳过未加载块；单描述安全预算最多 216 区段，不把坏包坐标用于刷新。

公共 BE 与同步桥没有客户端类引用；客户端订阅为 Dist.CLIENT，并过滤游戏线程及当前真实世界。已冻结公共签名、schema 与消费注意事项与 IMPLEMENTATION 报告一致。新手写注释及 Javadoc 为中文；源差异 `git diff --check` 无错误。

## 证据复用与限制

原始证据目录为 `build/reports/art/ART-REACTOR-02-L1/`，本轮只读取并核对，没有重跑：

| 证据 | 核对结果 |
| --- | --- |
| `final-unit-assemble.log` / `final-unit-assemble-exit.txt` / `final-xml/` | BUILD SUCCESSFUL，exit 0；逐 XML 汇总 26 tests、0 failures、0 errors、0 skipped，含本批 10 用例和原摘要/生命周期/护目镜 16 用例 |
| `gametest.log` / `gametest-exit.txt` | 1/1 required passed，exit 0；11:01:43 正常保存全部维度并结束服务端，未停滞在 Saving worlds |
| `api-sources/` | ClientChunkCache、ClientLevel、LevelChunk、RenderChunkRegion 与本树 `build/moddev/artifacts/neoforge-21.1.219-sources.jar` 对应条目逐字一致；直接阅读卸载、缓存安装及 region 所属世界路径 |
| Create/Ponder 锁定 JAR 的本轮只读 javap | SmartBE 卸载标记/setRemoved→invalidate、final getModelData→gather、WrappedLevel 光照委托与源摘对应，未依赖状态机用例代替真实顺序 |
| `api-signatures.txt` / `render-region-signatures.txt` | capture 两入口、findSurface Optional、Member 实际字段及 AT 后字段匹配冻结合同 |
| 最终 JAR 只读 SHA256 | `67644908B604A11488A35A3EDEF68EF01985A0DDFF3F4998DF48A31936D5923B`，与派发候选及 artifact-hashes 一致 |

PM 同步的接口合同/计划以及自动改动的 `logs/debug.log`、`logs/latest.log` 与实现差异分别识别，未清理或当作实现夹带；无构建脚本、正式美术资源、CT 消费者或其他设备改动。IMPLEMENTATION 已在本轮读取，报告与原始证据吻合。

剩余限制为最终 CT 接入后的客户端视觉及真实后台构建体验：成型/拆坏、相邻独立结构、各面与功能孔、跨区块/视距恢复、退出重进、世界切换和 Ponder 降级。上述自动证据不证明这些人工效果已通过；按原合同交由最终候选定向确认，无需新增玩法、全量回归或旧存档门。
