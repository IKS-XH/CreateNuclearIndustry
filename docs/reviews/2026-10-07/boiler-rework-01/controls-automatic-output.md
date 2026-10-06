# EXT-B-BOILER-REWORK-01B 执行交付

执行者：`/root/boiler_controls_01b`，非项目经理。工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。仅交付未提交改动；本报告不关闭锅炉集中客户端人工门。

## 实现与证据边界

- 控制器仅保留一个Create原生压力控件，滚动框、原生数值板及护目镜统一用百分数；范围0～100，逐整数保存。删除模式枚举、模式行为和`selectMode`；账本持久化唯一`Minimum`/`MinimumSet`，统一默认配置`outputMinPressure=0.6`、范围[0,1]。显式设置不被tick或默认配置覆盖，100%及高于安全阀开启线的下限不会使配置非法；安全阀算法保持原规则。
- `supercritical()`读取已付汽化热、实际汽温与炉压；无汽或未付足汽化热为无可输出汽，温压两个资格均达标才是超临界蒸汽，否则为蒸汽。热工目标仍为配置的超临界温度，汽种资格不增减库存或HU。
- 原结构`epoch`保留；增加独立蒸汽代次和汽种快照。实际汽种变化只撤销汽口能力、本炉汽口压力贡献，并标记汽口相邻原生网络重建；冷液/热液/水口不被撤销。端口在交易前捕获汽种，真实drain先按账本实际比焓扣热，再撤销跨门槛的旧汽句柄；返回交易前声明的汽种。旧汽网络generic drain无法抽出新汽种。拓扑传播安排在控制器tick，避免改动Create正在迭代的交易。
- 已有`BoilerSteamPressure`本炉贡献释放/补回机制可直接复用，无需修改该类或其他生产入口。外部储罐流体不被重写或删除；异种接收罐保持原生背压。
- 护目镜显示已同步实际“当前产汽：蒸汽／超临界蒸汽／无”，几何行改为“有效热力回路”，炉压和出汽下限以百分数显示；安全阀泄放仍用独立文案。

锁定Create来源：`create-1.21.1-6.0.10-280-sources.jar`，位于任务卡指定Gradle缓存路径。实际读取并确认：`ValueSettingsBehaviour.java:96-98`默认`netId()`为0；`ValueSettingsPacket.java:56-69`按netId首次匹配即提交并return；两个旧行为只有独立BehaviourType，没有独立netId。因此压力提交可能误分发至模式行为，进而覆盖旧默认保压及递增全局epoch。这是源码调用链实证，未声称本轮启动旧客户端复现该UI误路由。整改后的真实ValueSettingsPacket服务端入口已在GameTest提交17/43/70/0/100验证。

`FluidNetwork.java:195-214`先查声明流体、再做generic drain，且包含SIMULATE和EXECUTE两轮。generic真实抽取后若与网络缓存的fluid不同会被丢弃；本次以独立蒸汽代次、快照校验及定向刷新堵住这一路径。真实管路用例每tick同时检查实际接收质量和按实际比焓的HU变化。

## 实际修改路径

- `boiler/BoilerControls.java`、`BoilerControllerBlockEntity.java`、`BoilerState.java`。
- `config/BoilerConfig.java`。
- `assets/create_nuclear_industry/lang/zh_cn.json`及`en_us.json`，仅本卡锅炉键。
- `src/test/java/.../boiler/BoilerStateTest.java`：更新旧双模式断言，增加实际门槛、单下限、当前持久化及无免费HU验证。
- `gametest/ExtensionBoilerGameTests.java`：只同步受移除API直接影响的汽种场景和相关能力取得顺序；保留质量/HU断言。配置和BoilerReview测试没有旧API直接调用，未修改。
- 新增`gametest/BoilerControlsGameTests.java`及专用域`data/create_nuclear_industry_boiler_controls/structure/boiler_empty.nbt`；NBT逐字节复用01A空模板。

## 定向验证

命令均在本工作树、`JAVA_HOME=C:/Program Files/Java/jdk-21`执行。原始证据及候选JAR归档于`build/reports/extension/EXT-B-BOILER-REWORK-01B/`。

1. 红测：`test --tests com.iksxh.create_nuclear_industry.boiler.BoilerStateTest.actualThresholdsChooseSteamAndMinimumIsIndependent --console=plain`。1项运行、1项失败，exit 1；旧账本在实际炉压低于资格线时仍报告超临界。保存`junit-red.log`及`junit-red.xml`，失败位置为该测试的实际资格断言。
2. 定向JUnit：`test --tests com.iksxh.create_nuclear_industry.boiler.BoilerStateTest --console=plain`。**15项运行，0失败，0错误，0跳过，exit 0**。`junit.log`和`junit.xml`记录门槛两侧、0/17/43/70/100、显式设置恢复、无免费汽化热、模拟纯读、同tick多口额度、实际HU与阀行为。
3. 专用GameTest：`runGameTestServer -PgameTestNamespace=create_nuclear_industry_boiler_controls -PgameTestDirectory=run/verification/boiler-controls-01b --console=plain`。首轮3项运行，2通过、1失败，exit 1：蒸汽用例人工注入显热时仅重置HU观测基线，未同步重置质量基线，导致跨夹具修改的混合快照误报。保存`gametest-first-failure.log`；仅修正该测试基线，生产逻辑未变。复验同专用小域：**3项运行、全部required通过，exit 0**，保存`gametest.log`。通过后仅删去同一测试一处重复、无副作用的观测赋值，增量assemble已编译最终源；不重复启动服务器。
4. 增量`assemble --console=plain`一次：**exit 0**，`assemble.log`。没有clean、全量build或重复既有29/15/01A3域。现有4条弃用API编译警告未在本卡范围内改动。
5. 实现路径`git diff --check`通过；语言JSON可解析，专用NBT与原空模板字节一致。

3项GameTest证明：

- 原生数值包真实netId路由提交17/43/70/0/100后，服务端tick和当前保存恢复都保留；一次SC->普通跨压力门槛drain返回交易前声明SC、移出256mB/256HU，旧generic拒绝继续抽取，新汽口同tick额度没有刷新；真实拆装仍永久撤销结构旧冷口。
- 真实Create机械泵持续抽冷液，调压43/100及蒸汽->SC->蒸汽时，接收罐由**3328 -> 6528 -> 8000mB**，初始冷口句柄始终有效；整条冷液回路总量**30000mB**。末段达到单罐容量，遵循正常背压。复用01A齿轮/轴/创造马达位置，冷液流向北出，因此仅将泵FACING由热液输入的SOUTH反向为NORTH；锁定PumpBlockEntity:337-338的`isPullingOnSide(front)=!front`支持该方向选择，不将夹具方向调整当作产品根因。
- 实际蒸汽管路在保留原管道情况下经历蒸汽->SC->蒸汽，每tick质量等式不丢量，真实HU损失等于输出实际比焓加每tick自然显热损失的允许误差；外罐SC在炉内降级后仍保持SC，直到用接收罐能力真实抽走，为普通蒸汽腾出空间。未通过删除外部流体制造通过。

## 制品与人工门

最终候选JAR：`build/libs/create_nuclear_industry-0.1.0.jar`；同字节归档副本位于本批证据目录。

- 大小：**2,193,605字节**。
- SHA-256：`94F5B6481AB62BDB4B8594AAD1044B2FB693DCAB009A6FA644A77CEBE818AA2F`。

已读取并实际应用`minecraft-modding`、`minecraft-testing`、`systematic-debugging`、`test-driven-development`及`verification-before-completion`；版本核对为MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6。按治理5.1选定向账本与原生网络验证，不改技术栈、构建依赖、治理/核心文档，不执行Git写操作，不派发子代理，不启动用户客户端，不操作用户run/config或存档。仅GameTest隔离目录生成其自有运行文件；原logs与三个__pycache__未主动改动。

等待PM规格/质量合并审查。用户集中客户端仍需确认：唯一压力控件实际滚动及数值板能选逐整数中间值和0/100、退出重进保留设置；冷液真实管路在调压和自动汽种变化后持续运行；真实温压对应汽种与护目镜百分数/有效热力回路文案；外部异种接收罐背压正常。自动证据不视为人工验收，不接续新主线。

## PM交付记录

PM已读取上述原始日志/XML及[只读合并审查](./controls-review.md)，确认15项单测/3项真实GameTest与增量构建成功，未发现确定性阻断缺陷。已独立核对JAR语言/NBT与源码逐字节一致、生产类存在、旧ModeBehaviour不再封包；记录`pm-artifact-validation.log`。两工程目录已保存同哈希01B快照，原01/01A快照保留。

功能提交`32e6f897c03d8d8813f776dd4702bdf5cfe87b7f`仅含10项实现/测试/资源文件。首次精确git add因既有全目录`config/`忽略规则返回警告，但PM核实十项均已正确暂存、无额外路径，随后范围检查通过并提交，未修改忽略规则或强制添加目录。主工程只同步PM文档与报告，不合入未人工验收锅炉。当前交付停在[原集中人工门](./CANDIDATE.md)，此处补充替代上文“等待PM审查”的实施时状态。
