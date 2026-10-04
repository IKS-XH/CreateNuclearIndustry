# EXT-B-TURBINE-01C 独立合并规格与质量审查

**审查结论：需整改后再复审最终差异与验证证据。** 审查基线 `441ec68`；仓库 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。本轮只读，不运行 Gradle/JUnit/GameTest，不执行 Git 写操作。

## 发现

1. **阻断：定位档位诊断和世界内错误格提示未实现。** `TurbineControllerBlockEntity.diagnostic()` 仍只在控制器/输出轴交互时构造旧 `inspect(issue, x, y, z)` 消息（当前约第481–490行）；没有使用新语言键 `turbine.located`，也没有客户端渲染错误坐标的标记/高亮入口。`rg` 检查汽轮机实现没有额外世界渲染监听器或错误格绘制路径，只有 `TurbinePlacement` 的 Create ghost。玩家因此仍需凭坐标文字寻找缺件，未满足01C“显示当前定位档位、缺件类别/位置并在世界内标出错误处”的冻结合同。离线预览和服务端测试不能覆盖这项客户端交互。

2. **阻断：定位歧义被当成普通未定位并允许普通落块。** `TurbineAssembly.at()` 将零个匹配与多个匹配都返回 `null`（当前第51–55行）；`TurbinePlacement.useOn()` 在 helper 无落点且 `at()==null` 时返回 `PASS_TO_DEFAULT_BLOCK_INTERACTION`（当前第55–57行）。当多个完整轴列都覆盖被点击的构件/壳位时，辅助器无法区分歧义与尚未定位，会让原版 BlockItem 尝试在点击面放块，不会给出歧义提示。该结果可能把面板放进相邻机组的壳/腔体位置并破坏布局，违反“歧义时明确提示、不猜机组”。应分辨歧义与无定位，并在歧义时拦截默认放置。

## 已核对项

- 复读了 `AGENTS.md`、治理协议第5.1节、01C任务卡与已批准规格；实际读取并应用 `minecraft-modding` 和 `minecraft-testing`。版本合同为 MC 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82 / Flywheel 1.0.6。
- 阅读完整冻结接口、B资源报告及 `EXT-B-TURBINE-01C-review-package.md` 中手写差异和全部4个新增Java/测试/工具文件；海量生成blockstate按交付报告的统计、状态路径摘要及少数实际状态核对，没有逐份展开。
- 审查包是在A最后两项修改前生成的。我另外读了当前 `TurbinePartBlock.java` 和 `TurbineAssembly.java`：IWrenchable现仅由机壳/观察窗实现；接口只有合法侧槽、控制器中央列、进汽中央列及排汽端列被设为`located`，两项修正与合同一致。
- `TurbinePlacement` 将成功落点交由本地Create `PlacementOffset.placeInWorld`，并显式以玩家交互距离裁剪候选；成功路径使用传入手槽的BlockItem路径，失败时不执行消耗。A报告所述用例实际从方块`useItemOn`进入该路径并检查36件扣料、阻挡无扣料及解除后补件。测试不是完整客户端输入事件模拟；真实Create ghost、透明窗、玩家视角选点及手感仍须用户客户端门验收。
- `TurbineAssembly`仅据加载区块内两端轴和连续转子列定位；布局标记写入外观状态，`TurbineContent`流体能力及`TurbineStructure.ownerForPort()`仍要求`formed`，控制器仍是唯一机主。拆壳事件先让运行Form失效，再延后一tick刷新相邻部件；局部外观与服务端运行授权保持区分。关键逻辑未发现第二项已证实的权限绕过或邻机串接缺陷。
- 资源报告与摘要显示生成253模型JSON/OBJ、7物品模型，六向独立板模型与碰撞包围面匹配；8440项状态路径检查确认`formed`不改变外观选模。该离线证据不等于游戏渲染验收。
- 现有JUnit日志记录2/2通过，GameTest日志记录6/6 required通过且退出码0，assemble退出码0；这些是A首轮运行证据。首轮之后已改IWrenchable范围和接口外观筛选，尚不能当作最终候选的验证。遵循治理5.1，本审查没有重跑测试；待上述整改后只需A对受影响差异进行定向验证并更新报告。

## 人工验收边界

合并质量整改后仍需用户在客户端从空地逐步搭建，核对轴列、半包壳、缺件、完整和拆件外观，Create预览、六向薄板/观察窗、模型/碰撞、输出轴连接与真实玩家操作距离。用户手测前不应将客户端视觉体验标记为已验收。

## 最终修正复审（2026-10-05）

**范围结论：原两项阻断均 ADDRESSED；本次定向复审未发现新的真实代码阻断。** 最终用户客户端视觉验收仍待完成。

| 原发现 / 自查项 | 结果 | 复核证据与边界 |
| --- | --- | --- |
| 定位档位、缺件说明及错误格提示缺失 | **ADDRESSED** | `TurbineControllerBlockEntity.diagnostic()`（当前约485–496行）先查询服务端缺件，再附加`located(diameter,length)`和缺件类别/坐标；`highlightIssue()`（500–505行）只对服务端玩家发送Create `HighlightPacket(issue.pos())`。控制器空手和扳手入口均调用这两项（`TurbineShaftBlock.java:123–143`）。缺入口/出口的`TurbineStructure.missingPortTarget()`仅在合法侧槽内选空气/壳/窗候选（约190–204行）。新增GameTest验证尺寸诊断键与缺入口/出口的候选坐标（`ExtensionTurbineAssemblyGameTests.java:68–99`）。代码目标明确、服务端权威；测试没有捕获网络包或客户端画面，最终可见高亮须人工确认，属既定视觉门，不构成当前代码阻断。 |
| 多布局歧义误退回普通落块 | **ADDRESSED** | `TurbineAssembly.lookup()`区分`NONE/UNIQUE/AMBIGUOUS`（`TurbineAssembly.java:21–59`）；`TurbinePlacement.useOn()`只对`NONE`退回普通放置，`AMBIGUOUS`显示未唯一定位提示并返回`FAIL`，不会走`placeInWorld`或扣料（`TurbinePlacement.java:55–68`）。交叉完整轴GameTest断言`AMBIGUOUS`、`FAIL`且物品数不变（`ExtensionTurbineAssemblyGameTests.java:38–63`）。无布局时仍允许独立普通放板，保留合同所需行为。 |
| 扳手接口范围过宽 | **ADDRESSED** | 当前只有`Casing`和`Window`实现`IWrenchable`（`TurbinePartBlock.java:210–218`）；父类保留公开`onWrenched`实现，但只有Casing和Window声明`IWrenchable`，因此Create不会将该扳手能力分派给其他子类。 |
| 错误位置的控制器/端口获得布局外观 | **ADDRESSED** | `stylePart()`先检查`sideSlot`，控制器/进汽仅中央列、排汽仅`z=1`或`length-2`赋`located`（`TurbineAssembly.java:263–281`）；不合法接口保留自身回退状态供完整扫描报告。定向GameTest检查缺入口/出口时标记的是当前布局内可替换合法候选。 |

最终冻结证据已核对：`compile-final`、`junit-final`、`gametest-final`、`assemble-final`及对应exit文件均为0；JUnit日志BUILD SUCCESSFUL，GameTest日志记载8/8 required通过、服务器正常保存并关闭，assemble成功。本轮复审未重跑测试。A报告的JAR资源清单列出29项且缺失为0，只证明入包存在；字节一致性按PM安排另行复核。

本次只针对修正差异、三个当前新增类、相关接入与新增定向GameTest复审；没有扩查无关旧逻辑。客户端 Create ghost、透明窗、错误格高亮实际绘制、输出轴连接及真实玩家手感仍待用户手测。

