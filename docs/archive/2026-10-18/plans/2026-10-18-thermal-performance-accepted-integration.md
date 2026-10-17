# 汽轮机S1与C1优化合入

2026-10-18，主逻辑PM。用户要求“把最新的优化项合并到主分支里来”。最新来源为独立诊断目录`E:/MyMC/NewMod/Create_NuclearIndustry-thermal-performance-diagnosis-20261017/`下S1服务端及其上的C1客户端实例化候选；不重开旧反应堆C1L或后置C2，不新增玩法。当前结果见[STATUS](../../../STATUS.md)，本卡保留当时写集与验证合同；最终来源和制品见[集成记录](../../../reviews/2026-10-18/thermal-performance-accepted-integration/INTEGRATION.md)。

用户随后提出锅炉优化也合入；只读核对确认锅炉换热器重复同步尚只有方案、没有实现候选后，用户明确选择“本次先合并已完成的S1／C1”。因此本卡范围不扩大，锅炉专项仍留待后续冻结和派发。

## 来源、范围与不变量

- 基准main `a66d4342200c04d61136afedd539c67268330c01`，已提交正式包3382155 bytes、SHA256 `9C556236051AB1304BBFEBAC79AC300A1CA1E400DFE9D1DAFF11C38EC1D83F50`。主目录该JAR又处于本地删除状态，本次仅在来源提交和核验后提升；其他dirty保持。
- S1候选3386971 bytes、SHA `0A01D5709D48FD4207FEFD8031A63ECDF4467F1DA083F9830B0D799B079BF3F8`。真实成员包围盒排除不相交机组，缓存3/5/7静态空气截面并复用局部可变坐标；不缓存跨tick运行资格，保持当前区块/实体/世代及重叠检查。
- C1包含S1，候选3403459 bytes、SHA `9DA77B19BDF3C30EB9FE1EEAA4FF8A6CC6463E950ACC407C62DFE892E5537230`。原模型、材质、相位及显示距离保持，转子与输出轴改用现有Flywheel TRANSFORMED实例；原BER为后端关闭/不支持时的回退，共享动画状态同锁。
- 精确六源码/测试文件：`turbine/TurbineStructure.java`、`turbine/client/TurbineClientEvents.java`、新增`TurbineRotorVisual.java`和`TurbineOutputShaftVisual.java`，以及`src/test/java/.../turbine/TurbineStructureLookupTest.java`与`.../turbine/client/TurbineFlywheelTransformTest.java`。生产均位于`src/main/java/com/iksxh/create_nuclear_industry/`，测试同包。禁止其他源码/测试/资源/配置/Gradle变更。
- 不改热工、流量/效率/取整、库存守恒、红石、NBT、网络、模型纹理或管网。独立Sodium/Iris、单人暂停及真人多人未报告的范围不补记通过；用户本次直接采纳并授权合入，不等同于宣称所有环境已手测。

## 分工与验证

唯一集成执行者＋一次独立规格/质量组合窄审。PM负责Git和核心文档；执行者/审查者无Git写或派发权限，均不改main、来源目录或用户实例/服务器/世界。

复用同级干净树`Create_NuclearIndustry-du-accepted-integration`，分支`codex/thermal-performance-accepted`从a66d434开始。执行者先逐字导入C1/source/src的上述六文件，核TurbineStructure及其新增测试同S1来源；其余生产/构建输入必须等于main准确Git对象。报告、核验程序与临时编译只能写该树`build/reports/thermal-performance-accepted-20261018/`，普通Gradle输出允许保留，不提交。

审查者读六文件、原调用链和锁定Flywheel接口，重点核实时资格/相交判定、弱引用、世界/区块生命周期、实例delete/重载/后端回退、动画线程/暂停、变换/采光及不降吞吐；复用原测试/性能/生命周期原始证据，不重新运行测试、构建或性能矩阵。发现问题给具体路径/行号，由原执行者在写集内修，PM不代写。

S1原4项JUnit、7项真实GameTest及额外真实卸载/同tick失效，C1原27项定向JUnit、实例重载/后端切换/重登与同场景A1/B/A2记录均复用，保留失败日志和指标限制。只在源/依赖变化或发现具体缺口时扩大验证，不再运行全build、全GT或新的性能/游戏采样。

先取得一次组合窄审与导入/来源冻结，PM只提交六文件。随后核准确已提交Git文本与候选编译输入的关系，核S1六项及C1八项class对应原增量编译产物，其余载荷逐字同9C55基线。必要时允许一次定向javac复编这四生产源以验证源/class绑定，必须保持原编译选项、复用冻结依赖，输出仅报告目录；不改变或重新封装候选。绑定无疑义时提升原9DA77候选同字节包，不机械另做assemble；若不能绑定先报PM，不偷改JAR或默认换包。

## 世界与保护

main日志全部维度已保存，session.lock独占只读打开成功。原复用树世界/config完整备份至`run/save-backups/thermal-performance-accepted-20261018/`并核旧27文件SHA；再完整携带main当前27文件/13337924 bytes，路径、数量和SHA零差异，不混合或回写。记录在报告previous-world-backup.json和saved-world-sha256.json，不再扫描/启动用户世界。

主目录48项tracked状态dirty已冻结，除本次授权正式JAR更新外其余必须逐字保持。来源树、测试场景、失败证据和工具保留，不推送、发布、安装生产服或删树。

实际技能：minecraft-modding/testing核锁定MC1.21.1/Java21/NF21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6及证据边界；minecraft-ci-release核来源和正式制品；finishing-a-development-branch、verification-before-completion按治理5.1和用户明确合入授权执行，不重新询问选项。入口为`C:/Users/IKSXH/.codex/skills/<name>/SKILL.md`和`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/`。
