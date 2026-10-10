# 屏蔽装配台思索07-R1：仅保留用法

**状态：** 用户2026-10-10明确报告手动测试通过，单页播放门已关闭，main净教学提交`3f17657846095b7e34e6a7c82481b3cf9ac1d6f4`；见[验收](../../reviews/2026-10-10/shielded-assembly-ponder-07/ACCEPTANCE.md)。7集成路径与已审单页候选及冻结源SHA一致，复用3/3定向合同、增量assemble和独立窄审。旧三幕候选与证据保留，不扩记为独立美术视觉通过。

**任务：** DEVICE-PONDER-07-ASSEMBLY-R1。复用同级逻辑树`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，开工HEAD`d6ecac2aaadc7e9b46c32a1d9821fd97bd397d0a`。主PM只写文档/审核/Git；沿用原实现执行者与原独立审查者，禁止执行者Git写或派发。参考[原卡](./2026-10-10-device-ponder-07-assembly.md)的八格归属、接口、镜头和正文净空约束，以下新范围替代其中三幕、配比/工时与加工快照要求。

## 用户确定的结果

- 物品按W只有“搭建与接口”一页；保留2×2×2整台放置、主控底轴至少32RPM、四周进出物品/顶部只进料的已有用法。
- 删除`manufacture`、`sealing`故事板、方法和仅为两幕存在的批次快照/物料辅助代码。删双语两幕Ponder键和两份模板；不留下打包资源或生成器重新生成的路径。放置模板保持原字节、八格归属与底轴/物流布置。
- 不补替代页面，不把具体配比、材料清单或工时移入第一幕。配方仍由现有查询功能承担，真实设备配方/JEI/加工不改。
- 思索只讲用法、不讲具体配方，作为此后文案原则；本批仅改装配台，不重做已验收其他设备教学。

## 精确实现写集

1. `src/main/java/com/iksxh/create_nuclear_industry/ponder/ShieldedAssemblyPonderScenes.java`：只保留placement及其实际所需私有方法/引用，清理两幕专用代码和中文注释。
2. 同目录`P1PonderPlugin.java`：装配台列表/注册只剩placement，其他设备绑定和顺序保持。
3. `tools/ponder/shielded_assembly_scenes.py`：只生成/核对placement，保留当前布局与确定性编码；不运行工具修改范围外资源。
4. `src/main/resources/assets/create_nuclear_industry/lang/{zh_cn,en_us}.json`：只删manufacture/sealing的8个Ponder键，保留全部其他键及placement正文。
5. `src/main/resources/assets/create_nuclear_industry/ponder/shielded_assembly_{manufacture,sealing}.nbt`：删除这两份已弃用教学资源。
6. `src/test/java/com/iksxh/create_nuclear_industry/ShieldedAssemblyPonderContractTest.java`：更新现有合同为单入口且两幕不存在；保留实际placement模板八格/唯一归属、动力/物流、双语及正文时序检查，撤下配方/载荷教学断言，不新写生产功能测试。

`shielded_assembly_placement.nbt`只读并核对SHA；正式设备/配方、其他教学、美术、构建/配置、存档、全局docs均只读。实际脏日志/pycache保持，不清理。遇范围缺口先报告，不擅自扩大。

## 验证与交付

开工实际读取根AGENTS、本卡及治理5.1/5.2；应用`minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`和适用SDD/TDD/verification技能，版本保持MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。

- 先改已有合同，保存旧三幕实现下确实失败的定向断言证据；再作最小删除。不是新功能，不增加另一套重复测试。
- 只跑现有`ShieldedAssemblyPonderContractTest`与一次增量assemble；检查最终XML数/失败/错误/跳过及退出值。不跑全量、GameTest、客户端或已验收功能手测。
- 冻结新JAR到`build/reports/extension/DEVICE-PONDER-07-ASSEMBLY-R1/`，检查只有一页class/插件/双语/模板与实际源对应、两弃用模板不在JAR、placement原字节保持；生成器校验不改其他资源。保存精确源SHA、JAR SHA及改动/保护清单。
- 实现报告只写`docs/reviews/2026-10-10/shielded-assembly-ponder-07/R1-IMPLEMENTATION.md`；独立审查只写同目录`R1-REVIEW.md`，读上述新范围和证据，不重复跑构建/测试。原报告原JAR保留。
- PM绑定后交新的单页候选，停在“按W只有搭建与接口、机体/底轴/物流可见、正文不重叠”播放门。未反馈通过前不合main、不接新主线，也不关闭美术视觉门。
