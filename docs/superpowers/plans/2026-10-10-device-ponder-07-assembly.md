# 屏蔽装配台思索实施计划

> **执行者：** 使用 superpowers:subagent-driven-development；本PM派发一个实现执行者，再做一次合并规格与质量的独立窄审。执行者无PM或Git写权限，不派发子Agent。

**任务ID / 状态：** DEVICE-PONDER-07-ASSEMBLY / 候选已交，待本台三幕播放，尚未合main。2026-10-10用户明确要求“屏蔽装配台的思索也开始做吧”后，由主PM派发单一实现执行者及随后一次独立审查。用户已确认先烧结炉、后装配台、逐台验收；[烧结炉两幕](../../reviews/2026-10-10/fuel-sintering-ponder-06/ACCEPTANCE.md)已播放通过。本台只教授已有装配台规则，其他工程主线暂缓，美术视觉门独立保留。

**目标：** 玩家看懂整台放置、底部接动力、四周物流，以及新燃料组件装配和乏燃料封装两种工序；三幕各自简短，物料和接口可见。

**架构：** 增加一个专属Ponder场景类、三份确定性模板和对应生成器，在现有插件绑定装配台物品。全部变化限于客户端教学，不新增设备、配方、数值、GUI或正式状态同步。

**技术栈：** MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。

**现行合同：** `production/ShieldedAssembly{Block,BlockEntity,PartBlock,PartBlockEntity,Layout,Structure,State,Recipe}.java`、`recipe/shielded_assembly/{fresh_fuel_assembly,sealed_spent_fuel_cask}.json`；[装配台功能验收](../../reviews/2026-10-03/fuel-02e/ACCEPTANCE.md)、[封存验收](../../reviews/2026-10-06/store-01/ACCEPTANCE.md)及本卡。历史策划中的屏蔽边界、辐射、再生燃料或危险物流不能当成已实现玩法。

## 前置、技能与隔离

- 复用同级逻辑树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支 `codex/ore-acquisition`。派发前HEAD为`c9e4cda219cb971c3851fb98b5e2211f4231dc4a`，main为`a80ac483e64c4d51e623a5cf8230b1df7eba0783`；装配源、配方、Ponder/语言/生成工具及Gradle依赖共47个路径Git内容核对一致。PM同步本次任务状态后，执行者在报告登记实际HEAD；不得整体合并两树的其他历史差异。
- 保留日志、pycache和美术输入；不修改用户世界、不启动客户端、不进入美术树。本批教学与美术02R1/运行动画相互独立。
- 开工前实际读取根AGENTS、治理5.1/5.2、文档入口及本卡；读取 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md` 及适用的superpowers实施/验证技能。核对本地依赖API，不照搬新版示例或升级技术栈。
- PM只写文档、审核和Git；执行者只写本卡允许的实现及报告。中文注释说明职责、坐标、单位和临时世界边界；依治理5.1只做必要定向检查。

## 必须保持的现行规则

- 一件装配台放出完整2×2×2设备；主控为局部原点，`part=x+2*z+4*y`，按水平朝向旋转。只在主控底面接竖直动力轴，不能画成任意底面都是动力口。
- 四周16个外露水平面可输入材料、取成品；顶部四面只进料；底面无物品能力。真实物料集中于主控四个输入槽和一个单件输出槽，代理格不能有第二份库存。
- 新燃料配方为8烧结芯块＋4包壳管＋2锡合金焊料＋1钢格架→1满耐久新燃料组件；25600 RPM·tick，64RPM参考20秒。
- 封装配方为1枯竭铀燃料组件＋1玻璃固化基材＋1铅屏蔽桶→1已封装乏燃料桶；12800 RPM·tick，64RPM参考10秒。输出承载原组件完整记录，不生成新的空桶或不存在的灌封中间件。
- 两工序最低32RPM，正反转均按转速绝对值加工，上限256RPM计工；缺动力/超载或输出占用停止加工。首次实际投料锁定工序，同批不能混投另一工序；输入与输出清空且无进度才解除锁定。不得写成右键切换模式或任意时刻换料继承进度。

## 三幕

| 故事板 / 方法 | 画面与正文 |
| :--- | :--- |
| `shielded_assembly_placement` / `placement` | 先展示占地和整台放置，再露出主控底部动力轴；从可见的两个水平外侧安排进/出料器，补充顶部只进料。短句说明“预留2×2×2空间，一件放出整台”“从底部动力轴驱动，至少32RPM”“四周可进料或取成品，顶部只进料”。不用逐格拼装代理，也不重复漏斗基础知识。 |
| `shielded_assembly_manufacture` / `manufacture` | 在一台可见装配台上，利用两个可见侧面上下分开的输入器送入四种图标/实际批次数量，另一个可见水平面取新组件到容器；完整材料→加工→一件产物的变化连贯。正文分段讲8/4/2/1配比、转速影响工时、及时取出成品以继续加工；不把教学压缩播放时长当正式20秒工时。 |
| `shielded_assembly_sealing` / `sealing` | 新的干净批次，分别投入枯竭组件、基材和铅桶，展示一次完成及成品从水平面取出。用短句说明三种各1、一次封装，以及切换用途前清空本批材料和成品。到输出完整桶为止，不新增贮存架教学或演示未实现辐射/再处理。 |

每幕正文宜3段；执行者可按真实API调整镜头和短句，机制、数量和方向不能变化。新燃料与封装分幕不要求新增模式控件。图标演示配合主控临时快照，不依赖Ponder客户端运行正式服务端加工tick。

## 画面、快照与文案约束

- 基座最低y=0，设备主控建议y=2，让底部动力轴位于y=1且始终可辨；所有教学设备不埋地台。2×2×2机体、输入器、成品承接和动力链集中于镜头中央，留标题、字幕及底部控件空间。
- 主控模板设`expanded=true`，代理PART与朝向按真实Layout一致。固定教学UUID只用于该临时世界；主控 `ShieldedAssemblyOwner` 与各代理 `OwnerId`/`MasterPos` 对应，`complete()` 必须能识别同一台机。不要为显示改正式结构判定。
- 只用现有 `ShieldedAssembly` 保存格式/API写临时输入、输出、Operation、Costs、Work、Progress；状态槽放ItemStack而非整数计数。封装输出用既有 `SpentFuelPayload` 或等价实际保存格式携带完整组件，禁止把无载荷JEI模板当真实桶。
- 漏斗朝向与extracting/输入模式对应真实外表面能力，图标落点与成品容器明确；要从多个方向投料时有目的短转镜头，不用机身遮住材料入口。
- 正文一次一段，后一段在前段含淡出完全结束后至少10tick再出现。不得只等待正文duration；沿用06已核实的Ponder1.0.82生命周期。
- 双语与Java fallback同序同义，只讲装配台特有操作；删开发措辞、创造马达解释和显然Create知识，不添加重复停机排查页。动画、美术、辐射和再处理属于其他任务，不提前声明已完成。

## 精确写集

- 新建 `src/main/java/com/iksxh/create_nuclear_industry/ponder/ShieldedAssemblyPonderScenes.java`，公共静态入口 `placement/manufacture/sealing(SceneBuilder, SceneBuildingUtil)` 及专属私有辅助方法。
- 同目录 `P1PonderPlugin.java`：仅增加上述三幕及 `shielded_assembly_station` 物品绑定，不改旧入口/顺序，不把无物品代理注册为教程入口。
- 三份 `src/main/resources/assets/create_nuclear_industry/ponder/shielded_assembly_{placement,manufacture,sealing}.nbt`。
- 新建 `tools/ponder/shielded_assembly_scenes.py`：仅生成、回读和校验三份模板；允许只读复用现有NBT工具。
- 双语 `src/main/resources/assets/create_nuclear_industry/lang/{zh_cn,en_us}.json`：仅追加本批 `ponder.shielded_assembly_*` 键，保留原键/顺序，不全文件格式化。
- 新建 `src/test/java/com/iksxh/create_nuclear_industry/ShieldedAssemblyPonderContractTest.java`，少量合同检查。
- 执行者报告只写 `docs/reviews/2026-10-10/shielded-assembly-ponder-07/IMPLEMENTATION.md`；审查者只写同目录 `REVIEW.md`；原始证据写 `build/reports/extension/DEVICE-PONDER-07-ASSEMBLY/`。

正式设备逻辑、能力、配方、配置、模型/素材、其他故事板与测试、构建脚本、全局文档及Git只读。若现有Ponder无法载入合法快照，先向PM报告实际API与缺口，不自行增加正式setter或删去关键演示。

## 实施、验证与人工门

- [x] PM派发前核对前置及47个相关路径一致，已读指定技能和保存/物流/工时现行合同；既有脏项只有两个自动日志和三处pycache，保持。执行者开工继续登记实际HEAD及核实临时模板API。
- [x] 写少量合同red：三幕入口与双语；解压实际模板校验完整八格/唯一归属/高于地台的动力及外侧物流；真实配方数量/工序、完整封装输出与正文净空。基础设施异常单独记录，不冒充断言失败。
- [x] 实现三幕/模板/生成器/语言，回读模板并验证确定性。按实际模板坐标校验物料面和模型无遮挡的最低条件，禁止只搜Java字符串代替NBT检查。
- [x] 定向 `test --tests '*ShieldedAssemblyPonderContractTest' assemble` 退出0，检查XML实际测试数、失败/错误/跳过及JAR入口/class/双语/NBT与源一致；冻结写集和制品SHA。只有出现新的具体失败才扩大验证。
- [x] 一次独立规格+质量窄审读已有证据，不重复跑Gradle或客户端。PM核对准确写集与证据，登记候选。
- [ ] 用户只播放本台三幕：完整机体和底轴、物流入口及成品承接可见，配料/完成/封装讲述清楚，字幕不重叠或裁切。静态检查不代替这一门；通过后才净教学合入main。

不重测已验收正式加工/封存功能、前六台教学或旧存档，不运行全量或真实测试服；若触及正式共享机制才由PM另定相关验证。用户已授权本台实施，自动交付与独立审查后仍须单独播放验收，不声明客户端通过。

## 本批候选登记（2026-10-10）

三幕净实现提交 `d6fd20a8934aa3f5dc66e6ccd015b0ec6d22ef3c` 留在同级逻辑树；[IMPLEMENTATION](../../reviews/2026-10-10/shielded-assembly-ponder-07/IMPLEMENTATION.md)、[REVIEW](../../reviews/2026-10-10/shielded-assembly-ponder-07/REVIEW.md)及[播放清单](../../reviews/2026-10-10/shielded-assembly-ponder-07/CANDIDATE.md)已登记。原red为3项断言失败；最终3/3、0失败/错误/跳过，唯一增量assemble退出0。独立审查必改项无，PM核对9源与7项JAR入口/报告绑定一致，原日志保持；复用本批已有证据，未再次运行测试或客户端。

仅用户三幕播放仍未通过，不合main实现、不接其他主线、不关闭美术03/02R1视觉门。
