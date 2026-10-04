# EXT-B-TURBINE-DEVICE-01 执行报告

日期：2026-10-04。执行目录：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。基线：`fd19c1f`。角色：执行者；未进行 Git 写操作，也未修改核心文档。

## 交付

- 在已验证的 `TurbineShaftPowerSource` 与 C0 `TurbineConfig`、`TurbineState` 上实现正式六件汽轮机、普通蒸汽、三档 3×3×(转子数+2) 结构、四向角色与向外端口、唯一控制器库存、红石停机、拆放携物 NBT、扳手诊断及护目镜翻译快照。两个独立 Create 轴按实际处理 SU 和配置份额登记，端轴以 RPM 换算 capacity；结构成员不可 tick 时撤销输出。
- 普通蒸汽端口可经原生 Create 管道和储罐移动。Create 6.0.10 在 `FluidPropagator.hasFluidCapability` 中要求端口存在 `BlockEntity`，所以进排汽口新增共用的无库存 `TurbinePortBlockEntity`；真实库存仍只由控制器代理。首轮管网失败时，端口已有 1000mB 和首管 512 压力，但储罐为 0；添加实体后正式管网场景通过。
- 资源接口采用 D 提交的逐格静态 OBJ / JSON 模型，碰撞由机器方向旋转到实际八棱外壳、支脚、转子及端口安装板。六件配方在正式 GameTest 中从 `RecipeManager` 逐项确认 ID、产量和工序。没有新增 GUI。
- 测试：`ExtensionTurbineGameTests` 独立 namespace 含三档、多端口、双轴、红石、携物重放、真实 Create 管网；`ExtensionTurbineConfigGameTests` 在独立运行目录加载非默认 TOML，核对真实机组长度、流量、容量、RPM、系数、窗口和轴分配。

## 验证证据

| 检查 | 结果 | 日志 |
|---|---|---|
| C0 定向账本 JUnit | 9/9，0 failure，0 error；此前同一已冻结 C0 检查点 | `EXT-B-TURBINE-DEVICE-01-junit.log`、`build/test-results/test/TEST-com.iksxh.create_nuclear_industry.turbine.TurbineStateTest.xml` |
| 修复后增量编译 | Gradle exit 0 | `EXT-B-TURBINE-DEVICE-01-compile-port.log`、`.exit` |
| 默认正式 GameTest | 3/3 required；Gradle exit 0；全部维度保存、服务端关闭 | `EXT-B-TURBINE-DEVICE-01-default-gametest-final.log`、`.exit` |
| 非默认 TOML GameTest | 1/1 required；Gradle exit 0；全部维度保存、服务端关闭 | `EXT-B-TURBINE-DEVICE-01-nondefault-gametest.log`、`.exit` |
| 最终 assemble | Gradle exit 0 | `EXT-B-TURBINE-DEVICE-01-assemble.log`、`.exit` |

独立非默认原始配置留在 `EXT-B-TURBINE-DEVICE-01-nondefault.toml`，已写入 `run/turbine_config/config/create_nuclear_industry-turbine.toml` 并由服务器加载。NeoForge 首次启动将其改写为带默认说明的标准排版，实际运行后的完整副本另存为 `EXT-B-TURBINE-DEVICE-01-nondefault-runtime.toml`，SHA-256 为 `6079BB59F2B357BD9891FC3AA68F8504F5662FCF10AD6E9081A6A2CAF3B068DB`。逐键比较 18/18 均相同、0 处数值差异，改写仅为注释及格式；测试还逐一断言精确非默认值与实际设备表现。该首次运行目录没有 `server.properties`，Minecraft 报文件缺失后创建默认文件，GameTest、保存与退出均成功。默认正式测试还核对六件配方在运行时确已加载。

此前两次诊断失败日志保留为原因追踪，不作交付成功依据：`EXT-B-TURBINE-DEVICE-01-default-gametest.log` 与 `EXT-B-TURBINE-DEVICE-01-default-diagnostic.log`。前者暴露停机后测试代码误读空轴网；后者确认 Create 管道要求物理端口实体，并发现把长档稳态采样提前至 55 tick 过早。最终测试在 75 tick 稳态采样，三项通过。

## 边界

B 探针的真实跨区块生命周期与同网去重证据未改动，见 `EXT-B-TURBINE-API-01.md`，本轮按治理 5.1 未重复运行。客户端模型显示、手持物品和实际搭建体验仍需用户人工客户端门确认；自动检查不能代替视觉验收。D 资源由 D 执行者安装并冻结，本报告只记录 C 的接入与运行时结果。

实际读取并应用的技能入口：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`；按既定任务计划执行时还读取 `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/executing-plans/SKILL.md`，诊断 GameTest 失败时读取同目录的 `systematic-debugging/SKILL.md`。技术示例均以实际 Minecraft 1.21.1、NeoForge 21.1.219、Create 6.0.10 源码及本仓库代码为准。
