# BUILD-CONTRACT-01 独立规格与质量复核

**审查日期：** 2026-10-09
**基线：** `main` / `db6f354`
**结论：** R0复核曾阻断；R1定向复核通过，当前无未解决阻断项。
**范围：** 只审查两个未提交测试文件、任务卡、现行配方/标签与汽轮机已验收场景；未运行 Gradle、未启动游戏、未修改功能代码或执行 Git 写操作。

## 通过项

- 修前 XML 与报告根因相符：`P1DataContractTest`因锅炉配方中的`reinforced_steel_plate`包含`steel_plate`而失败；汽轮机合同仍查旧`showSection`/`hideSection`形式。修后两份目标 XML 分别为5/5、3/3通过，未跳过。
- 配方扫描已解析 JSON 并对完整注册 ID 做精确字符串等值匹配，因此现行锅炉及汽轮机中强化钢板引用不会再误判为钢板；`incomplete_reinforced_steel_plate`同样不会被这段比较误判。现行屏蔽装配灌封配方的冷却组件输入允许条件限定到`shielded_assembly/sealed_spent_fuel_cask.json`，并另行确认输入字段和结果字段，未把该消费路线当成通用生产白名单。
- 当前三条批准配方和灌封配方的真实 JSON 与测试中的路径/ID一致。三个批准路径是压片钢板、生产新燃料组件和搅拌冷却剂；热冷却剂、污染冷却剂、净化器及移除的旧钢板别名仍列在保护集合中。
- 汽轮机`showTier`实际使用同一个`section`句柄，顺序为独立显示、等待15tick、移动15tick、等待15tick、带边界正文、隐藏同一区段、淡出等待15tick。新测试按该顺序取索引、核对移动时长，并保留三个档位调用、正文寿命以及原有断汽检查；与已验收实现及[汽轮机验收](../turbine-ponder-framing/ACCEPTANCE.md)相符。
- R0审查复用首次`build`证据：`build-r0.log`记录`BUILD SUCCESSFUL in 17s`，目标 XML 通过，报告列明370例、0失败/错误/跳过及 JAR SHA-256。依照治理5.1，独立审查未重复运行构建；最终证据见下方R1复核。

## R0初判项（R1复核结论覆盖）

1. **R0边界疑点（已按PM澄清撤销）：** 初审把“不能被换用标签掩盖”解释成需要解析任意间接别名标签图。PM确认本批要求只覆盖 recipe 中直接等于受保护完整注册 ID 的 `item` 或 `tag` 字符串；间接标签图不属于验收范围，也不应据此阻断。R1对`item`和同名`tag`精确字符串的正例通过；详细结论见下方R1复核。

2. **R0缺口：批准产出白名单没有约束产物字段。** `approvedProductionReferences`使完整 ID 在获批配方任意 JSON 字段中出现即可成为预期值；因此钢板或新燃料组件 ID 即便从`results`/`result`移除、仅留在输入或其他字段，批准配方仍可能通过。当前正式配方确实把它们放在产物字段，但未来对配方的此类改动不会被合同发现。将三个批准例分别断言到各自的`results[].id`或`result.id`，保持禁止集合的全 recipe 精确匹配即可完成最小修正。

另外，R0指出未单列`incomplete_reinforced_steel_plate`负例；R1已补齐。

## 使用的技能与边界

- 已实际阅读`AGENTS.md`、治理协议5.1/5.2、BUILD-CONTRACT-01任务卡及`minecraft-modding`、`minecraft-testing`技能。
- 对照`gradle.properties`和任务卡版本：Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82。技能中的1.21.11示例未套用于本仓库。
- 本复核只覆盖当前合同内的具体测试断言；不要求泛化测试框架、额外完整重跑或游戏手测。R1差异与适用验证证据已复核，当前结论见下方R1复核。

## R1 定向复核（2026-10-09）

**复核结论：通过；没有剩余阻断项。** 本次仅审查R1两份测试差异和已保存证据，没有运行Gradle或启动游戏。

- **PM澄清并关闭R0标签图疑点：** 本批“标签不得掩盖”指 recipe 中直接出现、与受保护完整注册 ID 相同的 `item` 或 `tag` 字符串仍会被精确检查。R1新增了同一检查器对完整 `item` ID 与同名 `tag` ID 的正例。间接别名标签图的解析不属于本批验收合同，也不作为未完成项或阻断理由；本仓库当前没有污染冷却剂/净化器标签。R0提议扩展解析的数据集不应在本任务中实现。
- **批准产出字段缺口已修复：** 钢板压片明确检查 `results[].id`，新燃料组件检查 `result.id`，冷态冷却剂检查 `results[].id`。灌封仍单独断言冷却组件出现在 `inputs[].ingredient.item`，且不在产物中；未把合法消费路线扩展成生产白名单。
- **相似ID反例已补齐：** `reinforced_steel_plate` 和 `incomplete_reinforced_steel_plate` 均作为短小负例与钢板ID比较。R1未引入新测试方法、标签引擎或泛化测试框架。
- 汽轮机差异未再变化，既有独立区段句柄顺序、15tick合并/移动等待/淡出、三档调用、正文与寿命检查仍符合合同。
- 已复用R1既有验证证据：`exit-code-r1.txt`为`0`，`build-r1.log`记录`BUILD SUCCESSFUL in 18s`；R1目标XML中配方合同5/5、汽轮机合同3/3通过。当前JUnit XML汇总为69份、370例、0失败、0错误、0跳过。JAR仍为`build/libs/create_nuclear_industry-0.1.0.jar`，SHA-256与R0相同：`B428F571F316BB21C18082A849C0A672208C2503593395B380F159FAA7ABDD5B`。
