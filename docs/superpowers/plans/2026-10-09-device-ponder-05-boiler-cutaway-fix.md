# 换热器思索锅炉剖面整改 R4

**任务：** DEVICE-PONDER-05-EXCHANGER-R4。2026-10-09用户再次反馈：锅炉内置幕剖口仍未展示换热器。该反馈是既有教学合同的局部缺陷，不新增玩法。五幕仍待播放；本轮只复看这一幕，不重复已通过设备功能。

**工作树与基线：** `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，`codex/ore-acquisition`，HEAD `7abf4117964a7132f75aca5f146af4ed361d05da`。原候选源码 `f4aa143`。保留既有日志和三个__pycache__目录，不进入美术树、不改用户存档。

## 执行与根因要求

PM只管理本卡、审查、状态与Git；执行者实现并交未提交差异，不执行任何Git写操作，不派发子Agent。先读根AGENTS、治理5.1/5.2、原[五幕卡](./2026-10-09-device-ponder-05-heat-exchanger.md)的现行合同、此卡；实际读取 minecraft-modding、minecraft-testing、minecraft-resource-pack 及 systematic-debugging，锁定Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82。

1. 先对照用户截图、当前模板与镜头，读取实际Ponder Selection、show/hideSection、WorldSectionElement的裁剪/显示逻辑；核对分段隐藏是否生效，端口、近侧外壳、隔层、底板及方块面剔除是否继续挡住中心 `(7,1,7)`。先交明确根因，不能仅扩大一个字符串断言或换字幕称已露出。
2. 改为确实能展示底层换热器和上方再加热段的剖视。开头完整合法锅炉，讲解时建立明确的可见剖面，近侧端口/管路如果遮挡可暂移开；换热器本体完整可辨识、单独醒目高亮，再介绍再加热段及对应回路；讲后恢复完整壳体及端口。只操作客户端Ponder临时世界，不影响正式结构判定或设备。
3. 镜头、高亮与字幕指向实际显示的方块；换热器所在底层和再加热隔层均保留可理解的参照，不能只把设备取出来放成无法对应位置的孤立演示。避免新近侧遮挡和字幕遮挡；正文一次一段，删除“剖面中可看清”这类没有操作信息的句子。
4. 尽量保持原模板及其他四幕；若临时世界替换空气是所核对API下必要的显示方式，保存并准确恢复临时世界方块状态。不得靠永久缺格模板或取消恢复来规避问题。

## 允许写集

- `src/main/java/com/iksxh/create_nuclear_industry/ponder/HeatExchangerPonderScenes.java`：仅内置锅炉幕及其专属私有辅助方法。
- `src/test/java/com/iksxh/create_nuclear_industry/HeatExchangerPonderContractTest.java`：替换本幕无效的字符串存在性验证为针对实际根因的少量检查，不删其他有效合同、不照抄每个实现细节。
- 双语 `lang/{zh_cn,en_us}.json`：仅 `ponder.nuclear_heat_exchanger_boiler` 本幕相关文案，与Java fallback同义；不重排全文件。
- 仅在证明确需变更模板时准许 `tools/ponder/heat_exchanger_scenes.py` 的boiler部分及 `ponder/nuclear_heat_exchanger_boiler.nbt`，不得连带改其他四模板。
- 实施追加 `docs/reviews/2026-10-09/heat-exchanger-ponder-05/IMPLEMENTATION.md` 的R4章节；原始API/日志/XML在 `build/reports/extension/DEVICE-PONDER-05-EXCHANGER/` 使用R4后缀保留。审查者仅追加同目录REVIEW.md。

正式设备、配置、注册、模型/纹理/美术、其他Ponder、构建脚本和核心文档均不在写集。新增/修改手写注释用中文。

## 精简验证与门槛

- 根因对应的定向验证；一轮 `test --tests '*HeatExchangerPonderContractTest'` 与增量 `assemble`，记录实际测试数/退出码/制品哈希。模板未变复用R3生成证据；改模板才运行一次生成校验并核对其他四模板无差异。不跑全量build、GameTest、旧存档或设备功能回归。
- 一轮独立规格+质量窄审查，读取实际API与已有日志，不重新跑Gradle。PM核对写集及证据后提交同级候选，尚不合main。
- 客户端视觉不能由字符串、几何或构建断言替代。交付只要求用户复看“锅炉内置”：完整外观→底层换热器可见且高亮→上层再加热段→完整恢复。其他四幕及原功能不因本次反馈重开。

**状态：** R4实现候选`d6e17c7`、4/4定向合同、增量assemble及独立窄审查通过，待用户仅复看锅炉内置幕，未合main。

## PM交付收据

执行者`/root/exchanger_cutaway_r4_impl`核实Ponder mask正常，原默认东北俯视下控制器、近侧东墙和顶盖遮挡核心。新生产选区移开两面近墙、顶盖及邻壳，核心留在原位；两个核心讲完再恢复端口/管路，最后完整恢复。模板、其他四幕与正式功能未改。

原审查者`/root/exchanger_ponder_review`完成一次窄复审，无阻断。PM直接读取R4原始exit0日志、XML为4 tests且0 failure/error/skip、实际Ponder字节码和差异；复用未变模板证据，没有重跑测试或全量。实物2330001字节，SHA256 `C9DD6BD4423D71CA3FB1D55620134AFCFC3B97F3ACABF11315FC723A2C9B8604`。只提交源文件、对应双语/测试及两份报告，共六路径；原日志、pycache、PM文档未夹入源提交。

[本幕复看入口](../../reviews/2026-10-09/heat-exchanger-ponder-05/CANDIDATE.md)。射线净空与构建不代表客户端视觉通过；本体轮廓/正文自带轮廓叠加的实际观感随本幕复看，不能写为已验收。保留先前失败与审查记录，不重测既有设备。
