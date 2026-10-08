# EXT-B-EXCHANGER-02 实施记录

**基线：** 候选工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，实施前 `HEAD=3bf0f88d049b032b08b40867f072a98667b95b19`。未执行 Git 写操作。

**技能与版本：** 实际读取并使用 `minecraft-modding`、`minecraft-testing`，并按仓库锁定的 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82 源码核对接入。诊断一次 Mixin 启动失败时实际读取并使用 `superpowers:systematic-debugging`。

## 实现

- `HeatExchangerState.Settings` 增加普通/超级盆批次等效tick配置，公式分别为 `basinHeatedEquivalentTicks × huPerLevel` 和 `basinSuperheatedEquivalentTicks × 2 × huPerLevel`，默认40/80HU；huPerLevel为2时为80/160HU。费用超出储热上限时对应配方不匹配，不改变锅炉共享配置有效性。
- 账本加入独立 `tickBasin` 路径：仅有效候选请求补热，单tick转换不超过既有 `heatLevel × huPerLevel` 额定功率和当前批次缺额；空闲保留真实HU，不执行锅炉按tick扣热或断流期限。批次预留在同一HU储备内同步扣下，原生apply失败返还，成功后保留扣款。
- 服务端只检查换热器正上方的盆及其上方搅拌器，要求核热、可tick、直列无冲突、非锅炉接管、动力有效且盆可继续加工。通过Create原生候选路径确认filter、输入、流体、容器剩余物与输出空间；候选集合使用Create已有顺序和排序，热不足时只建立目标批次需求，足热后唤醒原生检查。
- `BasinHeatLevelMixin` 直接返回实时服务端已付热级，不读写Create `cachedHeatLevel`；`BasinRecipeHeatMixin` 在实际原生apply前预留费用，并在正常true/false返回时确认或释放。若盆底没有本模组换热器，原生apply完整放行；只有本设备存在时才校验热源、动力并收取费用。NONE配方和盆压片不走费用路径。核热冷却剂沿现有直列事务等体积转换；普通蒸汽冷凝分支保持原行为。
- 候选只记录一次；费用非有限、非正或大于储热容量的需热配方从可加工候选中排除。账本再次拒绝大于容量的需求并停止转换，显示“批次费用超过储热容量，请调整等效tick或容量配置”；此项停用不改变旧锅炉 `Settings.valid()`。
- 增加护目镜盆储备/批次门槛提示、中英文状态文字，以及隔离命名空间的四个真实Create搅拌GameTest。无新增设备、GUI或配方。

## 验证

整改后定向JUnit与增量打包命令：

```powershell
.\gradlew.bat test --tests "com.iksxh.create_nuclear_industry.heat.HeatExchanger*Test" assemble
```

Gradle结果为 `BUILD SUCCESSFUL`，本次匹配到的 `HeatExchangerStateTest` 为19项、0失败、0错误，XML见 `build/test-results/test/TEST-com.iksxh.create_nuclear_industry.heat.HeatExchangerStateTest.xml`。冷凝未受整改影响，本次没有重跑；前一轮已有的 `CondensationStateTest` 6项通过记录按审查报告引用，不计入本次19项。

初轮实现GameTest曾运行2项并通过。集中整改后的隔离GameTest命令：

```powershell
.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_ext_b_basin -PgameTestDirectory=build/gametest-ext-b-basin-remediation
```

最终Gradle结果为 `BUILD SUCCESSFUL`，服务端记录 `All 4 required tests passed`，随后完成世界保存并正常关闭。原两项换热器用例仍分别断言 `create_nuclear_industry:shielded_glass` 实产1个、冷液80mB、HU余量0，以及原生 `create:lava_from_cobble` 实产50mB熔岩、冷液160mB、HU余量0。新增两个互操作代表：真实投入煤的原生烈焰人完成普通heated加工；真实投入烈焰蛋糕的原生烈焰人用恰好1个圆石完成超级热配方并产50mB熔岩。普通换热器用例先设置不匹配的Create原生过滤并断言没有冷液转换，再清除过滤、停转后断言未充热/未耗料，恢复动力后完成加工。最终控制台：`build/reports/extension/EXT-B-EXCHANGER-02/remediation-gametest-final-3.log`；初次测试夹具失败和多批输出的日志均保留在同目录，不作为最终通过证据。

单元测试新增超容量费用断言：批费80HU但储备容量40HU时，不转移热冷却剂、不生成HU，状态为 `basin_cost_invalid`。Create原生 `lava_from_cobble` 配方源文件确认单次消耗一个 `c:cobblestones` 并产50mB熔岩，因此最终互操作测试使用1个圆石和精确50mB断言，不把持续多批的调度量写入预期。

整改后还单独执行 `.\gradlew.bat assemble` 刷新制品，退出码0；完整控制台见 `build/reports/extension/EXT-B-EXCHANGER-02/remediation-assemble-final.log`。定向JUnit/assemble控制台为 `remediation-junit-assemble.log`。早期Mixin启动失败XML、审查报告与各次GameTest失败控制台继续保留，未覆盖。

资源JSON及本批Java源码的 `git diff --check -- src` 检查通过；三个语言/mixin JSON均成功解析。日志存在Git whitespace提示属于用户/PM既有日志内容，本报告未改写这些文件。首次JUnit启动曾在Mixin加载期失败：`BasinOperatingAccessor` 错将继承方法 `isSpeedRequirementFulfilled()` 声明为目标类自身成员；锁定Create源码确认其声明于父类 `KineticBlockEntity`，本实现改用该继承的public API直接调用。PM已将首次失败XML保存在 `build/reports/extension/EXT-B-EXCHANGER-02/junit-startup-failure.xml`；修正后定向JUnit与打包成功。

制品：`build/libs/create_nuclear_industry-0.1.0.jar`，最终SHA-256 `B64B97153EB460524B375C89F1E325237342D39630DC4404691EC4C67BFC3DC8`。

## 边界与待验收项

Create原生 `BasinRecipe.match` 会按其实现使用 `Level.random` 抽取概率产物用于输出空间模拟；本批不复制或改写其配方逻辑，因此“只读探测”限于不更改HU、工质、方块与配方状态，原生随机抽样行为保持不变。Create的原生apply对任意第三方capability异常不提供跨物品/流体的通用回滚保证；本实现只保证费用在正常false返回时释放，不声称扩展了上游事务语义。

普通/超级配方真实加工、原生烈焰人互操作、原生过滤/停转恢复及HU费用已有隔离GameTest证据；超容量费用安全停用已有账本断言。首次新增原生热测试的初版夹具因直接伪设燃烧室热级未建立真实燃料状态而失败；接入Create原生煤/烈焰蛋糕投入后普通用例通过。超级热代表曾因64圆石在350tick窗口内连续产出19批而得到950mB；锁定配方确认每批消耗一块圆石后，最终夹具改为1块并精确断言50mB，最终4项全部通过。上述历史失败日志保留以区分夹具问题与生产缺陷。用户客户端手测仍待完成，特别是缺热等待、堵塞恢复、实际流体网络守恒和护目镜显示。尚未验证客户端画面或用户世界。当前交付只待PM复审本次整改差异。
