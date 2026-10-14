# DEVICE-PONDER-01 离心机思索实现报告

- 候选文档基线：`d4f8687`（功能源码基线仍为 `b7afb817530a74bd82ac77f2bc4301eaab2e3fbb`；快进仅含项目文档）。
- 主目录构建证据：`E:/MyMC/NewMod/Create_NuclearIndustry/docs/reviews/2026-10-06/store-01/main-integration.md`；主目录仅增量 `assemble` 成功，JAR 为 2,150,731 bytes，SHA-256 `e0f34616c7b554285d7e3ce92e14d851b0b413f02d6cbd0f95f594037a02ea02`。主目录构建日志为 `build/reports/extension/DEVICE-PONDER-01/main-assemble.log`。
- 锁定版本：Minecraft 1.21.1、Create 6.0.10-280、Ponder 1.0.82。核对了 Ponder `forComponents/addStoryBoard`、临时方块实体修改与动力场景 API，以及 Create 原生 `PumpBlock`、`PumpBlockEntity`、`FunnelBlockEntity`、`GearboxBlock` 源码。

## 实现

- 新增独立 `enrichment_centrifuge` 故事线，只通过额外的 `helper.forComponents(realCentrifugeId)` 注册；旧 `DIRECT_ENTRY_IDS`、反应堆场景和既有翻译值未改，并将插件入口 Javadoc 修正为同时说明实验堆入口与独立离心机教程。
- 八帧依次展示用途、单件放置上下机身并留出顶部接管空间、下段轴端输入、顶部唯一进浆、当前配比、外部粉末过滤与回水、护目镜排查和批次保留、停机后重型轴承维修操作标记。没有展示事故，也没有调用机器服务端事务。
- 结构包含完整上下段并在下段 BE NBT 设置 `CentrifugePaired=true`；底部电机/轴/齿轮箱驱动离心机。进浆泵朝下，侧接竖轴齿轮和示意动力源；回水泵朝向储罐，侧接同轴齿轮、轴和动力源。两个动力源仅是可视化示例。按 Create 源码，泵的 FACING 轴决定流体前后向：进浆泵从上方吸入并向下压送，回水泵从西侧吸入并向东侧压送；演示动力轮转向与各泵相反。
- 上下段各展示一个 `extracting=true` 黄铜漏斗，分别过滤贫化铀粉与低浓缩铀粉，并将原生落料动画导向下方漏斗收集斗；回收水经过另一只有动力的机械泵送入储罐。字幕说明顶部唯一进浆、上下两段四周均可输出两种粉末或水，具体物料由外部过滤选择。
- 配比字幕按当前代码描述为 1000 mB 料浆 → 1 低浓缩铀粉、7 贫化铀粉、1000 mB 水，并说明画面切换只是示意、实际耗时随转速变化。转速字幕说明任意非零且稳定 20 tick 即可运行，128 RPM 仅是示例。中英文字幕各 9 个新键。
- 模板脚本使用 NBT tag 编码并在生成后解压、逐标签解析至根 Compound 结束。锁定的 Ponder 1.0.82 `PonderSceneRegistry.loadSchematic(InputStream)` 调用 `NbtIo.read` 后直接进入 `StructureTemplate.load`；锁定 MC 1.21.1 `StructureTemplate.java` 第 709 行以 `getList("size", 3)` 读尺寸，第 750 行以 `getList("pos", 3)` 读坐标，因此新模板两处均编码为元素类型 TAG_Int（类型号 3）、长度 3 的 TAG_List。校验空根名、完整消费 EOF、尺寸 `[9,9,9]`、全部 106 个三整数坐标、坐标范围、上下段与配对状态、两个泵方向/动力支路、抽取漏斗和漏斗收集斗。未对旧模板做兼容测试。

## 验证

- 主目录增量 `./gradlew.bat assemble --console=plain`：成功；记录见主目录构建报告与日志。
- 场景/资源代码完成后的候选打包，以及 Javadoc 修正后的增量检查均成功。修正 `size`/`pos` NBT 格式后的资源增量 `assemble`：`BUILD SUCCESSFUL in 3s`，4 actionable tasks（2 executed、2 up-to-date）；编译任务 up-to-date，资源处理与 JAR 已重打包。日志：`build/reports/extension/DEVICE-PONDER-01/candidate-assemble.log`。
- 定向校验通过：生成 NBT 完整解码到 EOF；中英 JSON 可解析、每种语言恰有 9 个新键、与 d4f8687 基线相比旧键值未变；故事线恰有 8 个关键帧；`DIRECT_ENTRY_IDS` 与 d4f8687 基线一致；构建 JAR 含场景类、插件类、模板和两份语言文件，JAR 中模板与源码模板字节相同。
- 候选 JAR：`create_nuclear_industry-0.1.0.jar`，2,153,059 bytes，SHA-256 `0be6f77cd015f9c3bc290b2d08a9af07c0c1c840f917bd5eafa36e908782d862`。已验证 JAR 中 NBT 与新生成模板逐字节相同。
- 本次按 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack` 与 `superpowers:verification-before-completion` 技能核对 API、资源与按范围验证要求；未运行 GameTest、客户端、旧世界矩阵或历史模板兼容测试。

## 待人工验收

尚未启动客户端。需在临时世界播放 Ponder，确认真实转子可见、上下模型和轴系朝向正确、两只泵与齿轮传动动画协调、粉末落料视觉可读、流体与回水示意连贯，以及中英文关键帧翻译和镜头构图；客户端临时世界边界/可视范围也需在播放中确认。`MANUAL-CHECKLIST.md` 由项目经理维护，本报告不替代其人工验收记录。
