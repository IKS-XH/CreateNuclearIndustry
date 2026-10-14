# EXT-B-BOILER-REWORK-01D 执行交付

执行区：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。基准HEAD：`200073cf17ce3e3aec53cd75bff9902e45a01029`。本报告为执行者交付，不代表项目经理验收或集中客户端手测通过。未执行Git写操作。

## 实现

- 每个汽口独立持有Create原生`ScrollOptionBehaviour<BoilerSteamKind>`，只有蒸汽/超临界蒸汽两项，默认超临界。服务端拒收越界值及错误行，原生行为负责当前NBT与客户端快照；控制器仍只有既有压力下限。
- 四类口仍共用`PORT_BE`。实体改为`SmartBlockEntity`，水汽方块ticker初始化原生行为；只有汽口注册控件，水/冷热液口没有增加行为或复制库存。
- 控制器按位置持有汽口代次及已声明资格。一个口改变选择立即使该口旧句柄失效；实际温压改变仅撤销声明资格变化的口。其余汽口、水、热液、冷液句柄保持原代次。
- 主动邻罐直填、能力声明、typed/generic drain及SIMULATE统一过滤当前实际汽种。不匹配声明为空且不成交，不降级、升级或重标库存。同tick主动与被动输出仍共用原账本额度，按真实比焓扣HU。
- 复用01C的`forgetSteamEndpointNetwork`，仅在控制器tick对脏汽口的朝外首段管道/机械泵连接遗忘来源网络；drain内只置脏与撤销能力，不重建正在交易的Create网络。不匹配口不贡献锅炉主动输送压力。
- 每口护目镜显示所选汽种及未成型/无汽/汽温不足/等待匹配/等待压力/可出汽状态，首行使用`GoggleTooltip.indentFirstLine`。控制器当前产汽仍来自真实炉内资格。
- 汽口槽中心上移到朝外面的`y=.94`。锁定Create源码中水平`PUMP`轮廓遮挡`y=2/16～14/16`，中心`.94`位于上缘空隙；`ValueBoxTransform.testHit`半径为`.32/2=.16`。这是静态可命中判断，实际客户端瞄准、图标和文字仍待人工确认。

未修改`BoilerState`或汽轮机实现、配置、尺寸、成本、安全阀、热工参数。纯过滤造成积汽/泄放仍按原玩法处理。

## 写集

生产：`boiler/BoilerPortBlock.java`、`BoilerPortBlockEntity.java`、`BoilerControllerBlockEntity.java`、`BoilerSteamPressure.java`、`BoilerControls.java`，新增`BoilerSteamKind.java`；语言文件仅增加本批汽口键。

测试：新增`gametest/BoilerSteamSelectionGameTests.java`及`data/create_nuclear_industry_boiler_steam_selection/structure/selection_empty.nbt`（复用原空模板）；旧`BoilerControlsGameTests`、`BoilerTurbineFlowGameTests`、`ExtensionBoilerGameTests`只补显式选择夹具，未删除测试或放宽mB/HU、冷液、流量、停转断言。

本报告是唯一执行者文档写入。开工已有logs、三个`__pycache__`以及PM并行文档改动均保留。

## 实际技能与版本核对

已读取根AGENTS、01D冻结卡及01B/01C相关实现。实际读取并应用：

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：核对侧别、能力与实体生命周期。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：真实NeoForge结构/能力/原生管网GameTest隔离域。
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/brainstorming/SKILL.md`：沿用用户已批准纯过滤规格，不重新设计或索取已获授权。
- 同目录`subagent-driven-development`、`test-driven-development`及`test-driven-development/writing-good-tests.md`：由已派发的单一执行者完成紧耦合实现，先观察默认过滤失败，再实现；无执行者自行派发。
- 同目录`systematic-debugging`：保留编译失败，按锁定Create返回类型定位并修正压力诊断断言。
- 同目录`verification-before-completion`、`requesting-code-review`：据实际命令退出及日志交付；交由PM统一启动唯一一次只读规格/质量合并审查，不执行技能中的Git操作。

已检查Create锁定源码jar中的`ScrollOptionBehaviour`、`ScrollValueBehaviour`、`ValueSettingsPacket`、`SmartBlockEntity`、`ValueBoxTransform`、`PumpBlock/AllShapes`、`FlowSource`及`FluidNetwork`；尤其确认`FluidNetwork.reset()`不清旧source。MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6保持锁定。

## 验证证据

制品目录：`build/reports/extension/EXT-B-BOILER-REWORK-01D/`。所有运行均使用下列定向命令，Gradle自行管理的运行区为卡内指定的隔离GameTest目录；未启动用户客户端或改写用户存档/配置。

```powershell
./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_boiler_steam_selection -PgameTestDirectory=run/verification/boiler-steam-selection-01d --console=plain
./gradlew.bat assemble --console=plain
```

| 证据 | 退出 | 实际时长 | 结果/原因 |
| --- | --- | --- | --- |
| `red-01.log` / `red-01.exit.txt` | 1 | 24.81秒 | 首次1项失败：默认超临界口错误声明/模拟输出普通汽；证明测试捕获缺失过滤 |
| `green-01.log` / `.exit.txt` | 1 | 2.72秒 | 编译失败：压力诊断把`Couple<Float>`与整数比较；随后改为读取入向压力值，未清缓存 |
| `green-02.log` / `.exit.txt` | 0 | 23.11秒 | 4/4通过，服务端正常保存并退出 |
| `green-03.log` / `.exit.txt` | 0 | 19.79秒 | 最终4/4通过；新增普通口拒绝实际超临界降级及直邻泵同tick快速选择往返缓存恢复断言后复验 |
| `assemble.log` / `assemble.exit.txt` | 0 | 5.42秒 | 唯一一次增量assemble成功；原有4个API弃用警告，无新编译错误 |

`green-03-server-latest.log`和`green-03-server-debug.log`归档实际服务端原始日志。此次默认GameTest命令未生成XML，未把解析/自制结果冒充原始XML。

四项实际断言：

1. 默认超临界口拒绝普通汽的声明、SIMULATE及主动直填；显式选普通汽后同tick被动128+主动128=256mB。
2. 两口原生netId0独立提交、当前NBT恢复、非法值/行拒收、客户端选项快照；水/冷热液口没有控件，控制器唯一压力行为不串扰；下限100显示等待压力，不匹配显示等待汽种。
3. 普通汽256mB携带204.8HU；SIMULATE不改变账本、选项、代次或脏网络集合；仅切换口旧句柄失效，其他汽口和水/冷热液原句柄保持；来回切换不复制同tick额度。实际超临界256mB携带256HU，跨门槛仍返回先前声明的超临界，旧句柄不得再抽普通汽；普通选择口不能把实际超临界降级输出。
4. 真实双路原生网络（管道与直接邻接机械泵）、真实冷液泵持续；单口不匹配时自身压力清零，他口继续成交；外罐普通汽保持2816mB直至接收方明确排空，之后管道自行恢复超临界。直邻泵SC→普通→SC同tick往返后继续成交，无需拆管。最终A超临界2304mB、B超临界2560mB，冷液1536→8000mB；每tickmB与实际比焓/最多0.9HU散热检查通过。

最后Green之后仅增加“无合格汽→汽温不足”的遥测分支、整理局部缩进和上移控件布局；这些改动由最终assemble编译核对。布局实际可点范围及低汽温文字显示仍列入客户端人工项，未将其声称为GameTest覆盖。

未重复旧域或全量JUnit/build/clean/rerun。01B的15项已审定向单测与01C已审HU/停转证据只复用未变的账本公式和汽轮机边界；旧自动汽口换种输出行为已被纯过滤替换，不能称继续通过。三类旧测试夹具虽已适配并编译，此次未运行其旧域，不声明旧域新实现通过。

## 制品与待人工项

JAR：`build/reports/extension/EXT-B-BOILER-REWORK-01D/create_nuclear_industry-0.1.0.jar`。已核对封包含新汽种模型、汽口原生行为、新GameTest类与独立NBT模板。

SHA256：`c699c2a97e84d318cfbda33a79a4409ad3ed0d88c038dd5e33678620a72a5412`。

停在锅炉集中客户端人工门：每口朝外上缘控件接管/直邻泵后实际命中与文字显示、不同选择和等待状态、切换后无需拆管、冷液持续、保存退出再载入当前设置。未开展旧存档研究、汽轮机持续残转专项或其他设备教学。
