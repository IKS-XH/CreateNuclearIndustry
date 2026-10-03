# EXT-A-FUEL-02D 合并独立审查

日期：2026-10-03。只读审查工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，基线 HEAD `675e7a4`。已读 `AGENTS.md`、02D实施卡及完整方案、治理协议5.1和先期技术核对；实际应用 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack` 技能，并以仓库锁定的 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280 为准。未改实现、核心文档或Git，未运行Gradle、测试或客户端。

**结论：未发现阻止交付客户端手测的明确问题。** 这是候选代码、资源与既有证据的审查结论，不是四组人工门的验收。

- **配方与守恒：** `ShieldedAssemblyState.java:64-75` 只在四料齐全且成品槽空时完成一次 8/4/2/1 扣料和一件输出；`ShieldedAssemblyBlockEntity.java:53-73` 逐tick核对配方、动力与过载，产物直接新建正式 `FRESH_FUEL_ASSEMBLY`，初始损伤0。32RPM下限、单tick最多256RPM工时及4SU/RPM原生应力分别见该 BE `:43-46,65-70`；退料不足清工时见账本 `:43-52`，失效配方只清工时并保料见 BE `:59-63,77-103`。新配方、设备工作台配方的数量和身份与冻结方案一致。
- **接口与所有权：** `ShieldedAssemblyBlockEntity.java:115-116,194-226` 将顶面限定为四个输入槽、四个水平面映射同一输出槽，底部和 null 不提供能力；模拟操作不写账本。机械臂点 `ShieldedAssemblyArmInteractionPoint.java:51-61` 仅向顶面投料、拒绝抽取。完整 `ItemStack` 与进度由单个账本保存，区块和携带物使用同一格式（`ShieldedAssemblyState.java:79-100`、BE `:147-165`）；普通破坏、直接替换和潜行扳手均走单件携物机器及防重复标记（`ShieldedAssemblyBlock.java:94-146`）。本批差异没有修改既有反应堆换料端口实现。
- **呈现与资源：** JEI四料数量、设备催化剂和64RPM参考20秒、中英双语等待文案均接入；渲染注册限定客户端。三份机身模型各10个有体积元素，四朝向乘工作状态共8条引用；独立静态核对坐标、UV和纹理键有效。实际打开物品栏、运行正面、背面、顶部、底部预览，削角、露出基座、顶口及轴接口可辨。五张游戏PNG均16×16 RGBA，当前游戏纹理与导出副本的清单哈希一致；既有98张游戏PNG无Git差异。实际客户端显示仍须手测。

证据复用：`EXT-A-FUEL-02D/TEST-com.iksxh.create_nuclear_industry.production.ShieldedAssemblyStateTest.xml` 为3/3通过；首次 `gametest.log` 有漏斗逐件输送等待过短的断言失败，顺延该等待后 `gametest-rerun1.log` 为2/2必需用例通过并正常退出。真实用例覆盖配方加载、漏斗顶进、侧面能力、机械臂模拟与提交、底轴动力和过载判定、满输出、单件携物恢复及失配保料；过载通过Create公开网络入口模拟容量不足，未搭建自然容量不足的完整管网。成功GameTest后的尾改只涉及等待提示、注释/import及旧机械臂TAKE模式读回归一，未改变上述事务；按治理5.1复用原定向测试。最终 `assemble-final.log` 为 `BUILD SUCCESSFUL`，`compileJava`、`processResources`、`jar` 执行；`resource-check.txt` 记录JAR内17/17项本批资源与源码字节一致。独立复核JAR SHA-256为 `D6DEBDDB38DE7B423A11927EBDB7CF0A344160C99986C2A00B204E31BE1DBC4D`。

剩余四组客户端人工门：①制造/JEI/放置与手持模型；②四料准确扣取、错料、满输出及满耐久成品；③混合顶部物流、侧面提取、机械臂与动力暂停恢复；④存档、携带拆放和组件装入既有换料端口。Gradle产生的已跟踪 `logs/debug.log`、`logs/latest.log` 仍有差异，项目经理整合时需避免夹带。
