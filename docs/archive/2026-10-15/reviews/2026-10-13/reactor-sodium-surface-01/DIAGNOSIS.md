# REACTOR-SODIUM-SURFACE-01 当前诊断

2026-10-13，执行者基线 main `4da5e94033b79bed75aec76fc3d4c2cf140df822`。用户在本对话明确恢复排查并授权“先核实实际模型调用链，再修复客户端兼容”；该最新直接指令替代旧卡暂停及只读阶段限制。执行者不修改治理/计划，不执行 Git 写，不写用户实例、服务器或世界；主目录既有工业流体和模拟器 dirty 保持。

## 实际来源

- 实例：`E:/MyMC/.minecraft/versions/现代战争与新时代工业整合包`，用户本次明确指定。
- Sodium 外层 `0.8.12+mc1.21.1` SHA256 `075fd258d40438240cd9fb5ad7acad14f341076d53c6e031716679ff9d8737dd`；实际类在嵌套 `net.caffeinemc.sodium-neoforge-0.8.12+mc1.21.1-mod.jar`，只读提取至 `build/reports/reactor-sodium-surface-01/sodium-mod.jar`。
- Iris `1.8.14-beta.1+mc1.21.1` SHA256 `f83664550a8ddfa398d14863e02f33eed517e8cba7e4680fb1dc5dfc1b62c8a6`。
- 客户端 CNI 旧包 SHA256 `745e936c60c473ad2a47f83b9d683a0ae0dc227fdf07024523395edbc196c361`。
- 开始检查时客户端包名为 `create_nuclear_industry-0.1.0_261012_2.jar`；收尾只读复核时已变为 `create_nuclear_industry-0.1.0_261013.jar`，载荷哈希仍完全相同。本执行者未执行该改名或其他实例写入。
- 实例最新日志记录 NeoForge `21.1.235`；工程继续保持 `21.1.219`，Minecraft `1.21.1` / Java `21` / Create `6.0.10-280` / Ponder `1.0.82` / Flywheel `1.0.6`，不升级依赖。

## 已核实调用链与失败边界

实际 Sodium JAR 的 `javap -p -c` 结果保存在本批 build 报告目录：

1. `ChunkBuildContext(ClientLevel, ...)` 把任务所属世界传入 `BlockRenderCache`。
2. `BlockRenderCache` 用该世界创建 `LevelSlice`；该类是 final，`private final ClientLevel level` 在构造器赋值，`copyData/reset` 只更新/清理区块切片数据，不改世界身份。
3. `BlockRenderer.renderModel` 把保存的 slice 传入 `PlatformModelAccess.getModelData`。
4. `NeoForgeModelAccess.getModelData` 直接把这个 `LevelSlice` 当作 `BlockAndTintGetter` 调用 `BakedModel.getModelData`。
5. CNI `ScopedCTModel.gatherModelData` 建立作用域并调用 `ReactorSurfaceSnapshots.capture(context)`。旧实现只接受 `ClientLevel` 或精确的 `RenderChunkRegion`，所以真实 `LevelSlice` 必然返回空，CT 类型与共享面都降级。

这证明现行 CNI 与该 Sodium 路径存在确定兼容缺陷。尚未截获用户故障现场完整包→L1 发布→GPU 状态，因此不能宣布它是现场唯一原因，也无证据认定 Iris 自身存在缺陷。

服务端 BE 已有 `surfaceDescriptor` 同步及客户端 `ReactorSurfaceClientEvents` 的加载身份核验、原子 publish、发布后有限 section dirty；本批不改变这条生命周期，不以护目镜已成型代替客户端收到 L1 的证据。

## 客户端窄修与验证设计

生产写集仅 `src/main/java/com/iksxh/create_nuclear_industry/structure/client/ReactorSurfaceSnapshots.java`。为确切名称的 final Sodium `LevelSlice` 缓存只读的 declared `final ClientLevel level` 字段访问；无编译/运行硬依赖、无可选 Mixin、无第三方生产打包。结构不符合、字段不可访问或世界不匹配时继续返回空。原生两入口保持。反射只读世界对象身份，不调用世界方块/BE/区块/光照接口，不借用 Minecraft 当前世界。

测试写集仅新增 `ReactorSodiumSurfaceTest.java`；显式夹具目录加载实例真实类并初始化有限内存切片，先 RED 再 GREEN，覆盖同世界与旧世界身份、真实 Create CT context 与 final getModelData 共享窗口面、原生 region/level、未知包装降级。第三方夹具与临时 Gradle init script 仅放在 `build/reports/reactor-sodium-surface-01/**`，不修改 build.gradle。

复用现有 L1/CT 相关定向检查，一次必要增量 assemble。完整 Sodium/Iris 联机 GPU 外观仍是独立人工门；不冒称 isolated 类夹具覆盖整个渲染器/Mixin/联机链。不重跑泵、管网、热工或全量 build。

实际读取并应用 `minecraft-modding`、`minecraft-testing`、`systematic-debugging`、`brainstorming`（局部设计，用户已授权修复范围）、`test-driven-development`、`verification-before-completion`。技能目录分别为 `C:/Users/IKSXH/.codex/skills/` 与 `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/`。
