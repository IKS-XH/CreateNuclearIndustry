# ART-LOGO-11 独立组合规格/质量审查

2026-10-13。本次仅担任原唯一独立审查者，不取得PM或美术负责人权限。审查基线为11卡记录的美术HEAD `8a3389306eab790130b55308df9bac0274cee3b8` 加本批未提交来源；共享原件由主PM协调卡绑定。同轮已完成源码、素材、离线预览、正式安装及唯一增量JAR身份收尾，未发现剩余必改项。下文保留打包前记录，最终制品结论见末节；用户游戏视觉及main净集成仍独立待后续处理。

## 源码/素材阶段结论（打包前记录）

已读实际SVG、三个导出器/有限检查入口、独立HTML、候选及正式模型、两Java源、修前冻结/精确差异和现有原日志。源码/素材/正式安装范围未发现Critical、Important或其他必改项。可以由负责人安排原唯一作者按11卡备份当前美术JAR并完成唯一必要增量assemble；本审查不预填打包成功、可运行、Minecraft视觉通过或main集成。

## 规格与质量核对

- SVG为32×32透明画布、`crispEdges`，72个直属整数rect落在2px网格，12色；图形元素为八齿钢灰轮体、铜圈、钢卡爪及蓝白核芯。实际源无文字绘制、路径细轨道、嵌入图片或外部引用。作者明确声明本任务手写原创；本审查可核源和交付路径中没有引入第三方图像，未将此扩大为全面商标检索结论。
- `export_logo.py`的Logo坐标来自SVG，半开矩形通过右/下端点减一映射Pillow像素；32/128/256导出均为RGBA，放大仅最近邻。没有另画PNG图形。原15/15检查覆盖逐像素同源、alpha0/255、16px缩图回32px、候选32px逐字身份及模型/HTML引用；已读完整原日志与exit0，未重复运行。
- 独立打开`generated/preview-sheet.png`并以原图查看明暗/透明底、16/24/32px样本和标签模拟：中心蓝芯与外围铜/钢层次仍可辨，图中明确写明离线模拟。HTML实际使用同源PNG、原生CSS尺寸和无外部资源的本地引用；未把离线模拟或负责人的浏览器实看冒充Minecraft截图。
- 正式新增仅`models/item/mod_logo.json`与`textures/item/mod_logo.png`；模型为MC1.21.1适用的`minecraft:item/generated`、单层`create_nuclear_industry:item/mod_logo`，没有误用1.21.4+的`assets/.../items`定义。实际正式文件SHA与候选/冻结映射一致，原实验外壳模型未被用作覆盖目标。
- `MOD_LOGO`沿用已注册的`DeferredRegister.Items`、使用注册supplier创建普通`Item`；`MAIN.icon`在原生supplier内读取`ModItems.MOD_LOGO.get()`并创建`ItemStack`。两源只依赖common类型，不引入client类、额外注册总线或玩法状态；主入口已有`ModItems.register(modEventBus)`和`ModCreativeTabs.register(modEventBus)`。
- 读共享修前原件和实际两源；原2/2检查按允许追加/替换重建整份字节，精确差异仅UI注册/Javadoc与icon supplier。当前独立读回哈希同原证据。原`displayItems`、顺序及其他注册逐字保持；未用main的新Tab覆盖缺`IndustrialFluidContent`的美术树。新增手写注释及Javadoc为中文，职责与边界符合实际行为。

## 本次实际证据

| 证据 | 实际读取结果 |
| --- | --- |
| `phase1-export.log` / `.exit` | 原SVG72rect/12色，三种透明PNG导出，exit0 |
| `phase1-check.log` / `.exit`、`phase1-verification.json`、`phase1-manifest.json` | 原15/15、exit0；两个Pillow弃用告警保留，无异常；正式目标不存在属于安装前时点 |
| `shared-check.log` / `.exit`、`shared-consumption.diff`、`shared-identity.json`、`shared-before/` | 原2/2、exit0；两源基线及唯一消费范围一致 |
| `installation-action.json` | 复制前两目标不存在，冻结两候选预检后各新增一次，动作exit0，无重生 |
| `installation-readback.log` / `.exit`、`installation-identity.json`、`installed-source-manifest.json` | 原2/2、exit0；候选→正式逐字一致；17文件冻结记录，未assemble |
| 本审查`Get-FileHash -Algorithm SHA256`只读身份核对 | SVG、映射、三种PNG、检查图、共享原件/当前两源及正式两资源与上述记录相符 |

关键当前身份：

| 文件 | SHA256 |
| --- | --- |
| SVG源 | `11fcd852c27164a14ee4e8d3080bc7c7f0c94a665ba900d7d521ad432dc5b470` |
| 资源映射 | `9c94d090704a63476aacf685e5de2f514080e6778c373903ad3e3b92bf276d10` |
| 正式/候选模型 | `12b704b0441f7e0c7274e855b1fc4d3b986b39975e0b59bbb3ae1bcffc3417b0` |
| 正式/候选/32px导出PNG | `aa7798fecc9eb620f1eb7451a757605d978664bad863dda10a798af88cd6fb35` |
| 当前ModItems.java | `974f80bd82e6609ccae5dbb04cf5484e6124ec54c7e4c68bc0e265cf0834e6f0` |
| 当前ModCreativeTabs.java | `56a2c887f8b5e62d397ce14560ebd7618b76955efc486c86500686ce2633b519` |

## 打包前暂不判断及理由

- 最终JAR身份、编译结果与可运行候选：尚未交回唯一assemble及新JAR，保持待同轮制品收尾。
- Minecraft标签实际显示、GPU及用户视觉验收：本轮只有离线预览，客户端观察仍由用户完成。
- 模组列表`logoFile`、全局语言、配方和其他设备资源：11卡明确排除，不以通用新Item清单扩写本批范围。
- main/管理员Tab净集成及现行管网展示列表：主PM负责，只交精确UI两处差异；旧art Tab不作为全文件同步来源。
- 旧存档兼容、整机业务回归及其他人工门：没有新增本批行为风险，按治理5.1/5.2和11卡保留原证据，不重复或借此关闭。

## 技能与执行边界

实际读取主AGENTS、治理1.2/5.1、美术README、11卡及主PM共享协调；应用`minecraft-modding`的Deferred注册/common边界、`minecraft-resource-pack`的1.21.1平面模型/RGBA引用、`minecraft-testing`的范围分层和`verification-before-completion`的证据先于结论。`requesting-code-review`及其reviewer模板用于一次独立规格/质量审查，不子委派；项目治理优先于通用重复测试/Git步骤。实际properties/build锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。

本审查没有运行Gradle、producer、checker、Node、浏览器或Minecraft，没有Git操作或实现写入；仅写本指定审查报告。历史工具README/映射的candidate-only阶段和要求formal absent的phase1命令按原时点理解，当前安装状态以ASSETS及安装证据为准，不为阶段说明改动冻结图形/映射。

## 同轮最终制品收尾

2026-10-13原唯一作者交回最终包后，本席位仅读新增制品证据，没有二次代码全审、运行验证或重新启动构建。实际读取`assemble.log/.exit`、`assemble-run.json`、`previous-jar-identity.json`、`final-package-check.log/.exit`、`final-package-identity.json`、`final-source-manifest.json`、实际两份`javap.log/.exit`及制品核对入口`finalize_package.py`；另读取当前/冻结/原包文件属性与README/HTML阶段文字。

原唯一`.\gradlew.bat assemble`日志实际为`BUILD SUCCESSFUL in 6s`，exit0，run记录耗时6.6516364秒。已有ModFluids移除告警与GameTest类弃用提示保留，没有失败或第二次构建。最终核对原日志exit0，包含以下实际制品关系：

| 核对对象 | 同轮读取结果 |
| --- | --- |
| 当前`build/libs`及本批`final/`完整JAR | 同一SHA256 `d12cca860f69f0b411a99b3c7f34bacc17895c7ac4fb7bbd149616769dc839c5`；均2778351B，本审查文件属性读回长度一致 |
| 原美术包`previous-jar/` | 完整保留2777331B，SHA256 `9be1d3105034c37d27dd8448b3d1d00e2c375462d2f82b0d66a5b579b2e4ca6a`，备份原记录读回一致 |
| Logo模型/PNG | candidate→formal→build资源→JAR共两项逐字相同，SHA保持本报告前表 |
| 两Java来源 | `974f80bd…` / `56a2c887…`保持本轮已审修后来源；最终17项来源记录确认SVG/PNG/model/map及两源未变 |
| ModItems.class | 实际编译目录与JAR相同，4043B，SHA256 `da005306c137ee13cf07de725a3e869e54dde0f7423d8eabebfc8a7b6290200a`，class major65 |
| ModCreativeTabs.class | 实际编译目录与JAR相同，9619B，SHA256 `3fa70a5b109420d261b12d3254f7de1a1602b7366081e23b687ff7e02561df32`，class major65 |
| 整份新旧JAR条目 | 原核对实际逐字比较：只新增两Logo资源、只改变上述两class、移除0，其余2129条目及1122既有assets文件保持 |
| 主目录两共享源码 | 原只读制品证据仍为协调卡Items `6e83859b…` / Tab `3ef6d255…`；本批未向main消费 |

实际`ModItems.javap.log`有`MOD_LOGO` DeferredItem字段；静态注册链为`mod_logo`字符串→supplier→`DeferredRegister.Items.register`→写入`MOD_LOGO`。实际`ModCreativeTabs.javap.log`的builder通过supplier设置icon，`lambda$static$0`读取`ModItems.MOD_LOGO`、调用`DeferredItem.get()`后构建`ItemStack`。两份原javap退出均0，与前述真实class/JAR相同身份记录共同支撑编译后的UI接入，未将其扩大为Minecraft运行或GPU结论。

制品核对入口实际对旧/新JAR共有条目逐字比较、断言唯一新增/修改/零移除，逐字比较两资源四层及两class，并将17项来源与原安装清单逐项比对。只允许并实际发现HTML/README阶段文字变更：HTML当前SHA `8f28d7c28b2453c1a34ddc33a88970366f7599565428ba4abfb3882313a14060`，README当前SHA `df3a86583c906aa37450ccf4e0e02af55f3f26c03a1a75acdd370104867fd02a`。实际文字现说明已安装/已增量打包及仍待客户端观察，并解释phase1检查只适用于安装前；图形、导出器、模型/映射、资源引用和共享源码未变。原phase1/安装17项历史清单保留，不覆写为当前时点。

**最终审查结论：素材、共享源码、正式安装及本批JAR身份无剩余必改项。** 原15/15素材、2/2共享、2/2安装证据复用，只读取原作者唯一assemble和制品结果。可交负责人登记本批候选；没有执行Minecraft观察，也没有main/管理员净集成。用户实际创造标签显示仍待观察，后续main仅取两处精确UI差异，不以旧art Tab或完整包替换现行main内容。

## 同轮main接收者补丁收尾

原作者追加`tools/art-assets/mod-logo-11/main-ui.patch`及`main-ui-candidate/`后，本席位仅核接收者基线、两源候选、补丁上下文及原制品身份，不重开素材/代码全审。

独立只读main当前两源及SHA仍为协调基线：ModItems `6e83859b330c20e003142d15f2211ffabe38c7e864a119d6f2dcb45cf0ecea74`，ModCreativeTabs `3ef6d255c83d131b2b9260974c2494cce66a96c7383d50309c1510479a29fb88`。实际读两处patch hunk，其路径仅这两源，原行/上下文匹配当前main。ModItems候选SHA `974f80bd82e6609ccae5dbb04cf5484e6124ec54c7e4c68bc0e265cf0834e6f0`与原已审美术来源完全相同，新增原样中文Javadoc及`MOD_LOGO`延迟注册，不夹带其他注册变化。

接收者Tab候选9107B、SHA `240c45f5a5b7cd3c2519a40bbf6b29477b7991138e58bbbe92f06931a6c45598`。实际完整读取此候选与main，并通过只读字节关系核对：当前main只替换原MAIN.icon supplier所得内容与该候选逐字相同，结果True。五项`IndustrialFluidContent`的REINFORCED_PIPE_ITEM、CERAMIC_PIPE_ITEM、PUMP_ITEM、REINFORCED_VALVE_ITEM、CERAMIC_VALVE_ITEM及完整`displayItems`、顺序、其他所有源码字节保持；没有用旧art Tab覆盖现行main列表。

实际读取`scope-proof.json`、接收者四项`manifest.json`，补丁本体1856B、SHA `4a205f766ddc818eb361701b766d05f52035bfa2854665aa943d184f724234fd`，独立只读哈希一致。接收者候选与证明单独交付，原final17历史来源清单不被改写成新增补丁清单。

再次只读原当前/冻结JAR、正式模型/PNG及map身份：两JAR均仍 `d12cca860f69f0b411a99b3c7f34bacc17895c7ac4fb7bbd149616769dc839c5`，模型 `12b704b0…`、PNG `aa7798fe…`、map `9c94d090…`保持前述已审身份。生成接收者补丁没有新增资源或新包。

**接收者补丁结论：无剩余必改项，可交主PM按已绑定main基线处理两处净UI差异。** 此结论不表示补丁已应用、main已编译或用户已完成游戏观察。本席位未写main、Java、资源或Git，未新建审查树，未执行producer/checker、整批测试、构建或设备检查；仅追加本指定报告及做上述只读内容/身份核对。
