# 反应堆管理员03客户端候选

2026-10-14验收更新：用户确认保存重进与管理员其余项目全部通过，见[联合验收](../reactor-administrator-accepted-integration/ACCEPTANCE.md)。实际已验组合为d217196/dc82b20a…，已[精确合入main](../reactor-administrator-accepted-integration/INTEGRATION.md)；02条件前置已过，新增闭环仍未实现。下方33来源/78acd5d/879c及原审查为03内部交付历史，不替换回旧包或重复测试。

## 功能与操作

- 空手右击任职燃烧室打开总览／堆芯／保护三页，原四个世界控件撤下。深度与毛HU/t可直接输入，Shift步进100、Ctrl步进10；“应用”只保存完整设置，“启动”“停堆”“复位”独立操作，每页保留固定停堆按钮。
- 部分燃料列耗尽或补料不因可用数变化停堆；全部可用燃料归零才缺料保持。重新装料后需人工复位和启动。原卡死、未受控燃料、冷却与红石等保护仍有效。
- 普通燃料冷启动后，由反应堆真实新生热与缓存支付默认1HU/t，维持普通燃烧并暂停备用普通燃料倒计时；不足整笔不扣热，回外部燃料。停堆余热可继续供能，耗尽后停止；特殊燃料保留原时长，不获得无限超级加热。
- 建立联锁期间改模式回停堆，要求重新启动；预警期间全插基准保持，解除后执行已保存的手动目标。保护页显示实际红石高低与阶段预期，普通数字刷新保留输入焦点、光标、选区及未提交草稿版本。

## 同一目录与存档启动

复用`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-automation`，分支`codex/reactor-automation-basic`，当前测试世界原位保留。本批未启动或改写用户世界，没有新树、搬档或回写主目录。

```powershell
cd E:\MyMC\NewMod\Create_NuclearIndustry-reactor-automation
.\gradlew.bat runClient
```

进入原世界后空手右击任职燃烧室，按[集中复看清单](CLIENT-CHECKLIST.md)确认GUI、供能、部分／全部耗尽、保护与原01R改装状态。主目录尚无本批管理员功能，请使用上方候选目录。

## 当前制品身份

- 生产基线`2dff787`，开工文档`6fcf9c0`；最终功能净提交`78acd5d64fcb800d79c7388406a399eb03d8ecea`。仅33条manifest来源入Git，日志／世界／其他dirty保持。
- 正式包：`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-automation/build/libs/create_nuclear_industry-0.1.0.jar`。
- SHA-256：`879c3d72bf5a80f9cf93263abe847d8e0354d22499f7312bc1a9e661925506fb`；3183722 bytes。
- 冻结包`build/reports/reactor-administrator-03/final-candidate.jar`同字节；33写集源、658生产class、1542资源由PM独立核对当前源／编译／处理输出／JAR，零漂移；两测试域无class/NBT文件载荷泄漏，四个空ZIP目录不计正式模板。
- 原5e04、d372、e1c各包及源／清单／失败证据保持，当前身份以上方879c为准。[执行报告](IMPLEMENTATION.md)中的先前制品与未提交描述按各阶段保留。
- main管理员源码与正式JAR未改；main包仍`34bedc81d6cf0bbf53628f3f7511d031c2966dff1b0cb3fa9736927debb0be31`。日志及其他任务dirty不纳入本批净提交。

## 自动证据范围

本批证据在候选`build/reports/reactor-administrator-03/`，不通过叠加不同轮次计数声称最终完整套。详细命令与失败历史见[执行报告](IMPLEMENTATION.md)，四项审查发现及同次复核见[审查报告](REVIEW.md)。

| 证据 | 可以确认的范围 |
| --- | --- |
| 20-rod-heat-green.log | 35/35受影响热工，杆／混合缓存守恒、燃料冷却独立性及维护不冒充事故冷却，exit0。后续未改热工按此阶段复用。 |
| 22-real-final.log | 配置纠错前六真实业务6/6并自然保存exit0。不是之后所有源码的退出证明。 |
| 27-invalid-config-real-green.log | 六真实业务6/6，含非法配置下身份与STOP，之后保存挂起；业务与进程退出分列。 |
| 28-config-final-narrow.log | 配置窄补齐后的草稿／codec／投影／布局4/4，exit0。 |
| 32-review-directed-green.log | 最终Controller18＋Commands3，共21/21，exit0。 |
| 33-startup-real-green.log | 最终两个新增真实启动窗口2/2，三维度保存完成并自然exit0；不追认原六业务最终保存或用户卸载／重进。 |
| 34原生API／当前Screen字节码 | 输入对象、焦点与草稿版本的实际调用链证据；不是GPU或玩家鼠标键盘验收。 |
| 35-review-correction-assemble.log | 必要纠错增量assemble，1秒exit0；当前879c包。 |

原16／25／27／31轮保存挂起、26轮世界锁零用例、夹具诊断与冻结工具失败均保留。保存线程位于原区块卸载完成链，无本批算法归因证据；不得据此宣称用户保存／卸载／重进已通过，也不扩展为旧存档兼容研究。最后两个新增窗口自然退出只关闭其自身自动检查。

## 交付与人工门

本页和[实施卡03](../../../superpowers/plans/2026-10-14-reactor-administrator-gui-03.md)状态为内部交付、待集中手测。01／01R未验项目承接同一清单，不追认用户通过；已验Logo、管网和设备教学不重开。用户确认本批后才修订并派发02 GUI接入卡，不在此阶段实现PI或合main。

PM净提交时Git对已跟踪的config源父目录给出ignored提示并返回非零；实际33条全部已暂存。经索引与批准manifest逐项确认完全一致、diff检查通过后提交，未改ignore、force添加其他文件或重跑构建。
