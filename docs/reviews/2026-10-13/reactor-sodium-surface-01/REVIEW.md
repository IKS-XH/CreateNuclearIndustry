# REACTOR-SODIUM-SURFACE-01 独立规格与质量窄审

2026-10-13。审查结论：本批精确实现及同源候选未发现剩余必改项，可由主PM按活动卡接续净集成。该结论仅覆盖原反应堆成型连接纹理故障的客户端上下文适配，不构成 Iris、其他设备或所有优化模组的全面兼容保证。

## 角色、基线与范围

本审查者是独立执行者，不是项目经理。实际读取 `AGENTS.md`、[活动卡](../../../superpowers/plans/2026-10-13-reactor-sodium-surface-01.md)、[DIAGNOSIS.md](DIAGNOSIS.md)、[IMPLEMENTATION.md](IMPLEMENTATION.md)，完整检查两份交付源码及生产源码对基线的差异。当前 HEAD 为 main `4da5e94033b79bed75aec76fc3d4c2cf140df822`；新增测试在该基线不存在。

本批源码范围仅为：

- `src/main/java/com/iksxh/create_nuclear_industry/structure/client/ReactorSurfaceSnapshots.java`。
- `src/test/java/com/iksxh/create_nuclear_industry/structure/client/ReactorSodiumSurfaceTest.java`。

本次唯一写入为本 `REVIEW.md`。未派发、未执行 Git 写操作，未修改实现、测试、构建脚本或其他报告，未运行测试、构建、资源生成或重写原证据。只读检查原始日志/XML、字节码、SHA256、编译输出与 JAR 解压载荷；主目录其他大量 dirty、用户包和世界均不在本审查写集。

已从实际 `gradle.properties` 与 `build.gradle` 核对 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。没有使用技能新版本示例替换本工程技术栈。

## 规格与实现核查

| 核查点 | 审查结果与依据 |
| --- | --- |
| 实际失败路径 | 原 Sodium 字节码显示 `ChunkBuildContext` 将任务世界交给 `BlockRenderCache`，后者创建 `LevelSlice`；`BlockRenderer.renderModel` 把该 slice 交给 `PlatformModelAccess.getModelData`，`NeoForgeModelAccess` 原样调用 `BakedModel.getModelData`。基线 capture 仅支持 ClientLevel/精确 RenderChunkRegion，因此真实 slice 被降级。原 RED 的同世界 capture 与 Create CT 类型两项失败与此吻合。 |
| 世界来源可靠性 | 已核实的 `LevelSlice` 是 final 类，declared `private final ClientLevel level` 只在构造器赋值；原字节码中的 copyData/reset 不重绑该字段。生产第30～43行只接受确切类名与 final 类，并验证 declared 字段类型恰为当前 ClientLevel、final、非 static、可读取。没有放宽为任意包装器、光照委托或坐标匹配。 |
| 同世界与旧世界隔离 | 第53～59行先原子取得完整 Publication，再按 slice 自身 final 字段得到的世界对象做 `==` 比较；不会以 Minecraft 当前世界替代。旧世界 slice、null、未知包装或无发布身份均不能借用新世界快照。真实 slice 测试含发布世界交换及撤销；原 CT 测试复用一次构建持有同一快照、嵌套/异常退出恢复作用域的检查。 |
| 原生与 Ponder 边界 | 原生 ClientLevel 与精确 RenderChunkRegion 判别保持，新增真实类测试实际检查二者。未知代理的方法全部抛错而 capture 仍返回空，证明本入口没有调用代理。只读核 Ponder 1.0.82 的继承链为 `PonderLevel → SchematicLevel → WrappedLevel → Level`，不是 ClientLevel 或新增 slice 类型，因此仍降级；本批没有伪称新做了 Ponder 播放或 GPU 检查。 |
| 模型线程与性能 | 新入口只读反射字段和已发布不可变快照，不查询仪表BE、结构、区块、方块、光照或网络。ClassValue 按真实 Class 身份缓存 Optional<Field>，缓存不保存世界实例；稳定路径是类缓存命中及一次字段读取。首次并发缓存计算即使出现重复解析，也只涉及本地元数据与访问权限，不修改世界。原 AtomicReference 信封及 ThreadLocal 模型作用域保持，不新增共享可变世界状态或世界锁；本批没有性能基准结果，不作吞吐数值保证。 |
| 异常降级 | 缺字段、字段形状不符、trySetAccessible 返回 false、ReflectiveOperationException、SecurityException 均得空缓存；读取遭 IllegalAccessException 返回 null 后降级。Field 由 context.getClass() 解析且字段类型已精确核验，普通读取不会发生错误目标类型/字段值转型。未发现需扩大可选接入范围的异常处理缺口。 |
| 无硬依赖 | 生产只含 Sodium 类名字面量，没有 import、class 字面量、Class.forName、Mixin、工程依赖或第三方打包新增。只读候选字节码确认新的匿名 ClassValue 类仅使用 Java 反射及既有 ClientLevel 类型；真实无 Sodium/Iris 专用服原始日志完成加载与 1/1。 |
| 真实类与 Create 消费 | 测试 URLClassLoader 加载实际提取的 LevelSlice，并断言其定义加载器身份。身份测试故意不初始化方块数组；消费测试使用真实 slice 索引方法与有限局部数组，实际调用 Create buildContext、包装模型的 final getModelData 及共享窗口面过滤。旧世界模型回退保留原共享面。测试没有自行复制连接算法。 |
| 权威规则与范围 | 基线差异仅增加反射/Modifier/Optional 及可选世界提取，原 State、Publication、Bounds 业务未改。无服务端成型、热工、库存、尺寸、协议、材质、刷新生命周期或工业流体实现变更。本批新增非显然代码说明为中文，职责与实际行为一致。 |

## 原自动证据复用

实际读取 `build/reports/reactor-sodium-surface-01/` 内的原始 `01-red.log`、RED XML、`02-green.log`、三个 GREEN XML、`03-assemble.log`、`04-assemble.log`、`05-server-probe.log`，以及夹具与候选 init script。未用报告叙述替代原始结果，也未重新运行这些检查。

| 原检查 | 核实结果 |
| --- | --- |
| RED | 3项、2失败、0错误/跳过。同世界 slice capture 未得到发布快照，原生 Create CT 预期 OMNIDIRECTIONAL 实得 null；原生/未知隔离通过。 |
| GREEN | 原 XML 为真实 Sodium 3＋原 CT 21＋快照2＝26/26；失败、错误、跳过均0，日志 BUILD SUCCESSFUL，18秒。 |
| 首次候选命名脚本失败 | `03-assemble.log` 在初始化脚本访问尚不存在的 jar 任务时报错；没有实际 jar 任务执行，保留为失败历史。 |
| 唯一实际增量 assemble | `04-assemble.log` 仅 jar 执行，生产编译/资源等 UP-TO-DATE，BUILD SUCCESSFUL，2秒。 |
| 无可选模组专用服 | `05-server-probe.log` 的模组表没有 Sodium/Iris，既有 reactor_surface_display_probe 1/1，通过后自然保存各维度并关闭，BUILD SUCCESSFUL，20秒。初次隔离目录缺 server.properties 的异常以及原开发环境警告保留，不写成零警告。 |

真实 Sodium 3项依赖本批显式 `cni.sodium.fixtureDir`；普通构建未提供时会跳过该可选类夹具，不能将普通构建通过另算为真实 Sodium 覆盖。当前已冻结 GREEN 的三项实际没有跳过。Unsafe 分配及有限数组不模拟完整客户端启动、Sodium/Iris Mixin、网络包到达或 GPU，现有交付对这些限制说明准确。

## 当前来源与制品身份

只读复核 `source-bindings.json` 的两源码及六个编译 class；源码 SHA、当前编译输出 SHA、候选中六个 class 的 SHA 均与绑定完全一致，零漂移：

- ReactorSurfaceSnapshots.java：`f95fc1cc8b756ef2b38caef229b23ae655ff86fe9494fd7620ed32de99b3941b`。
- ReactorSodiumSurfaceTest.java：`e48ceed51127840e82a0e952a3ddc367370d6a72f130f3ffb6779fc958fb153f`。
- 候选 `build/libs/create_nuclear_industry-0.1.0_261013_sodium-surface.jar`：3048078字节，SHA256 `8c03dbd11e4812705b18942407ae4dd64aba8e7c0cbbc18d03ab2690a0ac1f23`。
- 冻结基线 `baseline-745e936c.jar`：SHA256 `745e936c60c473ad2a47f83b9d683a0ae0dc227fdf07024523395edbc196c361`。
- 原提取与夹具 `sodium-mod.jar` 当前同为 `54695055628a28678b483e7ca5774cf0f298624e1ef223c381cd641c261bd1e7`。

当前只读遍历两 JAR 全部文件载荷，与原 `artifact-verification.json` 一致：2150→2151，2145保持，仅 ReactorSurfaceSnapshots 本类和四个内部类改变，新增一个匿名 ClassValue 类；没有删除、越界载荷或新增 Sodium/Iris/Fabric/测试夹具制品。对 Bounds、Publication、State、State$Owner 再作只读 javap 指令比较均相同，与 `nested-bytecode-verification.json` 的四项 true 一致；未把调试行号改变登记为状态机语义改动。

`IMPLEMENTATION.md` 关于原正式261013包、用户实例包名/哈希、尚待联机复看的文字是执行者交付时点的历史记录。本审查仅对上述候选与冻结基线确认当前身份，不据此断言正式包或用户实例如今未改名、未替换；没有覆盖、复制或安装任何包。

## 用户反馈与剩余项

主PM本次提供的人类直接反馈为：“这个渲染问题已经解决了，我手动测试也没问题了”。活动卡已接续该反馈。此人工结果只对应原反应堆已成型而客户端仍分离的渲染故障；本审查没有观察用户客户端，也不补写光影开关、拆坏恢复、重连、换世界或全部设备兼容的逐项手测结果。世界隔离与原生入口在此是源码/字节码和原自动证据，不混写为人工验收。

剩余必改项：无。没有新增旧存档迁移、全面 Iris 兼容或其他设备复测门。主PM可依据本审查与原用户反馈处理本批净集成及验收登记；其他独立门与未授权主线保持其原状态。

## 实际技能

实际读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`，以及 `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/requesting-code-review/SKILL.md`、`verification-before-completion/SKILL.md`。分别用于版本与两侧边界、证据层级、独立规格/质量审查及当前来源身份复核。派发与重复测试步骤依用户 AGENTS 和本批活动卡的角色及证据复用要求不执行；本报告交回主PM，不自行验收、提交或推进任务。
