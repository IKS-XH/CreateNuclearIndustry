# EXT-UI-GOGGLES-01：锅炉遥测与护目镜遮字修复

**状态：实现、定向验证及独立审查通过，等待人工显示复测。** 2026-10-05用户报告锅炉正常运行但本tick产汽始终为0，且新增设备的护目镜图标遮挡首行文字。本卡仅修复显示；汽轮机01F的流量效率人工门仍待确认。

## 基线与职责

- PM负责本卡、交付审查与Git，执行者负责代码与必要测试；禁止执行者Git写入、派发其他执行者、修改核心文档。
- 工作目录`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支`codex/ore-acquisition`，基线`f80dcad`。保留根目录logs改动和tools/art-assets/__pycache__；不干预用户客户端/存档。
- 必读AGENTS、治理5.1/5.2、本卡；实际使用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`与`minecraft-testing/SKILL.md`。MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82，不升级依赖。
- 所有修改的手写代码注释遵循中文规则，不研究旧档兼容，不增加GUI或修改平衡/库存/产热算法。

## 已定位原因与修复合同

1. BoilerState.produced/vented是运行期计数，save/load不传递；BoilerControllerBlockEntity的View同步仅带Paid等字段，客户端tooltip却直接读取ledger.produced/vented，故一直为0。以明确的客户端显示快照补齐本tick产汽与排放，通过初始更新标签和后续包同步；有产汽/排放显示真实数值，停止/归零后不保留旧值。服务端计算、现有mB单位和总量不变，不把显示修复扩展为账本持久化变更。
2. 本模组设备直接tooltip.add首行，未使用Create的图标缩进。锁定Ponder的LangBuilder.forGoggles按实际客户端字体宽度预留默认16像素；统一沿用此原生机制，保留译文、样式与内容层级，不硬改语言资源前导空格、不改全局Create浮窗或隐藏图标。
3. 检查所有生产设备addToGoggleTooltip入口：离心机、烧结炉、屏蔽装配台、换热器、锅炉、汽轮机，以及现有反应堆端口/仪表/控制棒驱动器。只修确有遮字的排版，不改变反应堆字段归属与逻辑，不重复缩进Create已添加的内容。研究用P0探针排除。
4. 原生forGoggles内部引用Minecraft/font，须保留专服与现有服务端tooltip测试的安全性，采用最小的客户端边界；不得让服务端加载客户端类。可以只对需避让图标的首行使用原生格式，避免无关层级重排。

## 写集

- `src/main/java/com/iksxh/create_nuclear_industry/boiler/BoilerControllerBlockEntity.java`。
- 上述九个设备现有tooltip所属Java类：`production/{CentrifugeBlockEntity,FuelSinteringBlockEntity,ShieldedAssemblyBlockEntity}.java`、`heat/NuclearHeatExchangerBlockEntity.java`、`turbine/TurbineControllerBlockEntity.java`、`blockentity/{ReactorPortBlockEntity,ReactorInstrumentPortBlockEntity,ControlRodDriveBlockEntity}.java`；除锅炉同步外仅允许显示格式修复。
- 必要时新增单个共用goggle排版工具及其客户端桥接类；包路径沿用现有工程组织，报告确切路径。不可引入全局事件/混入重写Create。
- 允许原字节复制既有`data/create_nuclear_industry_boiler/structure/boiler_empty.nbt`到`src/main/resources/data/create_nuclear_industry_goggle_sync/structure/boiler_empty.nbt`。锁定NeoForge按模板命名空间筛选测试；保留独立域可只运行本批1条用例，不能将零用例进程退出0记为通过。
- 必要定向测试：新增`gametest/ExtensionGoggleSyncGameTests.java`与对应单元测试，或在现有锅炉GameTest增补最小场景。已有tooltip测试仅可因新增合理外层格式适配断言，不能降低内容检查。
- 执行报告/日志/隔离测试配置目录`build/reports/extension/EXT-UI-GOGGLES-01/`；隔离运行目录`build/runtime-goggles-01`。其他文件需先向PM报告。

## 精简验证与交付

- 一组真实锅炉生产后的更新标签/数据包round-trip验证：非零产汽、排放计数正确同步，停止后更新为0；明确检查的是客户端镜像数据读取路径，无需重测锅炉整套热工/管路。
- 排版核对全部上述入口、Create原生缩进与专服安全；可复用既有tooltip内容断言，不为每个设备复制一套测试。人工客户端负责实际图标/字体显示确认。
- 一次定向验证加增量assemble。可独立GameTest namespace，沿用既有-PgameTestNamespace与-PgameTestDirectory，不改构建脚本、不clean、不跑全项目。
- 一份简短implementation-report.md记录根因、精确写集、技能、命令/退出码/证据、未验证边界。完成后停止修改，由PM安排一次联合规格质量审查，审查复用日志。
- 手动仅两项：运行锅炉浮窗显示非零产汽且停机归零；查看锅炉及其他新增设备，首行名称不再被图标遮挡。测试前需重启候选runClient加载Java变更。未人工确认不合main、不推进新玩法。


## PM收尾记录

- 已补齐显示快照和九个入口的原生图标间距，定向GameTest明确1/1 required通过，服务端正常退出、assemble退出0。模板/API夹具失败及零用例运行均保留且不计为通过。
- [交付与两项手测](../../reviews/2026-10-05/goggles-01/README.md)保存报告、日志与审查；当前未合main，01F手测仍待确认。
