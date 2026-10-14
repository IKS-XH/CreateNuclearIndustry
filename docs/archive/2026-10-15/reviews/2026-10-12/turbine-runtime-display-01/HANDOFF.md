# TURBINE-RUNTIME-DISPLAY-01 实际接口交接

本树 `E:/MyMC/NewMod/Create_NuclearIndustry-turbine-runtime` 的已编译只读接口；源码/ABI/JAR 身份及实际验证见 [IMPLEMENTATION](IMPLEMENTATION.md)。用于已批准 ART-TURBINE-10B 动画消费，本批未写 Renderer 或动画相位。下列签名已由 `build/reports/turbine-runtime-display-01/r1/compiled-api.txt` 核验。

## 美术消费者唯一取样入口

```java
// 包 com.iksxh.create_nuclear_industry.turbine
public Optional<TurbineRuntimeSample> TurbineRotorBlockEntity.runtimeDisplay();
```

在当前转子的客户端实例直接调用。`Optional.empty()` 表示缺少可信绑定，必须撤销旋转轨道并采用静止 fallback；有值且 `rpm()==0` 是可靠停转，应将速度置零，不应因停转撤掉合法几何。该方法会核本转子真实实例/位置/外观、两端权威轴位、当前控制器实例与 generation/epoch、相关区块和租约。只返回整包，不能混用上次方向或当前配置 RPM。

```java
// 包 com.iksxh.create_nuclear_industry.turbine.display
public record TurbineRuntimeSample(
    String dimension, BlockPos owner, UUID generation, int epoch,
    BlockPos front, Direction facing, int diameter, int rotors,
    int index, UUID member, long serverTick, float rpm);
public BlockPos memberPos();
public Set<ChunkPos> requiredChunks();
public CompoundTag encode();
public static Optional<TurbineRuntimeSample> decode(CompoundTag tag);
public static float localRpm(Direction facing, float actualShaftRpm);
```

| 字段 | 实际含义 |
| --- | --- |
| dimension | 当前服务端世界维度完整 ID，消费时还必须以当前世界会话隔离相位。 |
| owner / generation / epoch | 控制器坐标、该服务端 BE 实例随机 UUID、原正式成型 epoch。拆件/重建或卸载后旧绑定不能使用。 |
| front / facing | 前轴坐标；facing 为机器朝前方向，前→后为 `facing.getOpposite()`。 |
| diameter / rotors | 当前成型直径（3/5/7 方块）和转子数量（合法边界 3..16，由正式 Form 给出）。 |
| index / member / memberPos() | index 由前轴后第一列起从 0 计；member 为服务端转子实例 UUID；位置为 `front.relative(facing.getOpposite(), index + 1)`。 |
| serverTick | 同一整组样本的服务端 gameTime，仅排序/同龄证据，不能用作客户端租约时钟或直接乘新 RPM 重算历史角度。 |
| rpm | 有限 float、单位转/分；已经转换为局部前→后 +Z，含真实外源反转与原生过载 0，消费者不得再次调用 localRpm。 |

`TurbineRuntimeSample` 构造器会拒绝缺字段/非法几何；`decode()` 严格核 NBT 类型、水平朝向、合法控制器侧槽、坐标/数量/index 及有限 RPM，坏样本整体拒收。record 的标准访问器等同上述字段签名，另有编译生成的 `equals/hashCode/toString`。

符号来自世界正轴的 Create 实际 `getSpeed()`：北朝向后轴在 +Z，西朝向后轴在 +X，保持符号；南朝向后轴在 -Z、东朝向后轴在 -X，取反。服务端只读取已成型所属前轴，并核两端轴实速一致；不一致时暂时不可用，不主动修网。真实 probe 明确证明 NORTH 下外源 `getSpeed()=-64` → `rpm=-64`，其余三向换算由独立字面期望 JUnit 覆盖。

建议取样方式（示例只表明消费合同，不提供本批未实现的相位 API）：

```java
var runtime = rotor.runtimeDisplay();
if (runtime.isEmpty()) {
    // 撤销这个成员旧绑定对应的角度轨道，并绘制静止 fallback。
    return;
}
var value = runtime.orElseThrow();
float signedLocalRpm = value.rpm(); // 已是前→后局部 +Z，不再反号。
// 用会话 + dimension/owner/generation/epoch 作为整机相位身份；member/index 用于本列绑定。
// 按真实客户端活动 tick/partial 累计角度，0 立即停止，换速不能用新 RPM 乘总世界年龄。
```

同组各列共用服务端时间、机主身份、epoch 和 rpm，可共享整机相位。成员 UUID 用于撤销具体列旧绑定，不能把各列随机 UUID 当同机共享相位 key。客户端相位缓存仍须自行服从世界卸载、可靠样本丢失和真实暂停；本接口不负责画帧或累计角度。

现行R1在同generation、同serverTick时，仅同身份且整个record逐字段相等的包视为真正重复，不续租；epoch/member/几何/RPM等任何完整差异立即撤销。撤销后再发旧重复包不能复活，必须等待更晚完整样本。runtimeDisplay核对的是最终返回的租约样本本身（含member、当前owner generation/epoch/对象和几何），同时要求与pending整包一致，不能由新pending包为旧旋转背书。

## 生命周期与同步保证

- 正式控制器 `onLoad()` 登记弱引用，`LevelTickEvent.Post` 在世界 BE/Create 动力 tick 完成后，每 5 tick 采已登记机主；不扫描全世界，不强载区块。
- 先验证正式完整 Form、两轴及所有转子，再构造所有同龄样本；只用真实 `getSpeed()`，保留过载/冻结返回 0，外源贯通如实显示。
- Controller 的 `invalidateForm/remove/onChunkUnloaded` 撤销成员；转子自身 `setRemoved/onChunkUnloaded` 清显示。客户端旧 owner 对象不能为同位置新对象续期。
- 原生 BE `getUpdateTag/getUpdatePacket` 提供首次跟踪和整体心跳；显示不调用 `saveAdditional` 持久化，Controller 的身份字段仅 `clientPacket=true` 写出，可携数据不写。
- 客户端租约 20 活动 tick，只有完整相同的重复 serverTick 不续期；同tick差异立即撤销；错误/旧身份、水位倒退及完整坏样本撤销。会话或区块卸载即使暂停也撤销，恢复必须等更晚新包。
- `ClientTickEvent.Pre` 在 1.21.1 真暂停中仍触发，因此仅在 `Minecraft.isPaused()==false` 推进租约；没有用 wall clock。世界 Unload 后禁止由仍挂在 `Minecraft.level` 的旧对象重开会话。
- 若首次转子包早于控制器身份包，先安全静止，下一个至多 5 tick 心跳再绑定；不拼接旧字段等待补齐。

## 其余实际公共签名（实现内部，不作为美术写入口）

```java
// TurbineControllerBlockEntity：只读身份
public UUID runtimeGeneration();
public int runtimeEpoch();

// TurbineRotorBlockEntity：发布器/原生同步/生命周期适配
public UUID runtimeMemberGeneration();
public void publishRuntime(TurbineRuntimeSample next); // 服务端整包或 null 撤销
public CompoundTag getUpdateTag(HolderLookup.Provider registries);
public ClientboundBlockEntityDataPacket getUpdatePacket();
public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries);
public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries);
public void acceptRuntimeClient(long now);
public void tickRuntimeClient(long now);
public void revokeRuntimeClient();
public void unloadRuntimeChunk(ChunkPos chunk);
public void onLoad();
public void setRemoved();
public void onChunkUnloaded();

// TurbineRuntimeLease：客户端状态模型
public TurbineRuntimeLease();
public boolean receive(TurbineRuntimeSample next, long now, UUID owner, int epoch, UUID member);
public Optional<TurbineRuntimeSample> current(long now);
public void revoke();

// TurbineRuntimeClock：无客户端类依赖的活动时钟
public TurbineRuntimeClock();
public void advance(Object world, boolean paused);
public long tick(Object world);

// TurbineRuntimePublisher：服务端自动订阅
public static void register(TurbineControllerBlockEntity owner);
public static void revoke(TurbineControllerBlockEntity owner);
public static void unregister(TurbineControllerBlockEntity owner);
public static void onLevelPost(LevelTickEvent.Post event);
public static void onLevelUnload(LevelEvent.Unload event);

// TurbineRuntimeSyncEvent：中立事件
public TurbineRuntimeSyncEvent(TurbineRotorBlockEntity rotor);
public TurbineRotorBlockEntity rotor();

// TurbineRuntimeClientEvents：仅 Dist.CLIENT 自动订阅
public static void onTick(ClientTickEvent.Pre event);
public static void onUpdate(TurbineRuntimeSyncEvent event);
public static void onChunkUnload(ChunkEvent.Unload event);
public static void onWorldUnload(LevelEvent.Unload event);
```

公共构造器、继承方法、原有 Controller API 及编译桥方法完整列表保存在 `compiled-api.txt`。美术不得调用上述发布/续租方法伪造可靠性，也不得以 Controller.signedRpm、TurbineView.Rpm、generatedSpeed、邻近轴搜索代替 `runtimeDisplay()`。

## 原消费者接点及权限

已只读核对美术树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/tasks/ART-TURBINE-10B.md` 与现有 `TurbineAnimationVisualState.java`。实际接点是原包 `src/main/java/com/iksxh/create_nuclear_industry/turbine/client/` 的 `TurbineRotorRenderer.java`、`TurbineClientEvents.java`、`TurbineAnimationVisualState.java`；输出轴仍由其 `TurbineOutputShaftRenderer.java` 使用 Create 自身 `getSpeed()`。

原中立写集已含 ClientEvents、OutputShaftRenderer、AnimationVisualState 及两专属测试；原卡明确 RotorRenderer 在实际共享 HANDOFF 后由负责人另补写权，本执行者不授予权限。本交接提供补卡所需的实际编译接口；PM 净同步必要前置、原美术负责人向原消费者补充 RotorRenderer 绑定范围后接续。美术已完成中立测试及本前置R1的16/16及复用的真实3/3证据按同源复用，只验证实际新增消费差异。

本批不关闭转子实际旋转、停转、四向、拆件、重载、真暂停的客户端人工观察门；已通过的三件模型不因本前置重开。

现行制品为审查P2整改R1，JAR SHA256=`203E955881FE60074069E5A12D3C480A4166B877C2BD56DCDAFB57C5F7833266`；公共签名未变化。初次AEC制品与原始失败/通过日志全部保留，当前源/编译类/JAR绑定见证据 `r1/`。本轮只重跑受影响的定向组和必要第二次增量assemble，未重跑服务端真实组。