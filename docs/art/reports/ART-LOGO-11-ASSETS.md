# ART-LOGO-11 唯一资产/消费执行报告

2026-10-13。当前已交原创图形与共享UI两处消费，负责人实际离线复看后批准两项新增正式资源，独立读回一致；同一次组合审查关闭源码/素材/安装必改后，已完成唯一必要增量assemble、实际字节码/打包核对和完整JAR冻结。当前交负责人及原审查者进行同轮制品收尾；没有启动Minecraft，离线预览和编译制品不代表用户游戏视觉验收。

## 图形与来源

用户已选齿轮＋蓝色核芯。唯一绘图入口为[32px SVG](../../../tools/art-assets/mod-logo-11/source/mod_logo.svg)：本任务手写原创八齿钢灰轮体、克制铜色轮圈、四向钢卡爪与蓝白核芯，无文字、细轨道、第三方图片或Create logo复制。72个直属整数`rect`全部落在2px网格，12色；不透明像素与透明背景只有alpha255/0。SVG源SHA256为`11fcd852c27164a14ee4e8d3080bc7c7f0c94a665ba900d7d521ad432dc5b470`。

独立[预览入口](../../../tools/art-assets/mod-logo-11/index.html)含明暗/透明背景、真实CSS 16/24/32px、256px放大和16px创造标签模拟；[离线检查图](../../../tools/art-assets/mod-logo-11/generated/preview-sheet.png)由同源PNG生成。执行者实际查看检查图：宽齿轮、铜环与中央蓝芯在16px样本中可辨。负责人随后实际看检查图与8735浏览器的明暗/透明、16/24/32和标签模拟，按11卡末段登记内部实看通过；Minecraft加载与用户视觉门仍未通过。

`export_logo.py`直接解析SVG矩形生成32/128/256透明RGBA PNG，放大仅最近邻，不含Logo绘图坐标。完整独立Logo文件已交工具目录；候选平面`item/generated`模型指向`create_nuclear_industry:item/mod_logo`，32px候选贴图与32px导出逐字相同。[映射](../../../tools/art-assets/mod-logo-11/resource-map.json)仅拟两新增资源，不覆盖任何旧设备资产。

## 本轮必要自动证据

固定已有运行时`C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe`，`-B -X utf8`；实际Pillow12.3.0，未安装依赖。

| 实际命令尾部 | 结果 | 原始证据 |
| --- | --- | --- |
| `tools/art-assets/mod-logo-11/export_logo.py export` | exit0；SVG72rect/12色；32/128/256与候选32导出 | `build/reports/art/ART-LOGO-11/phase1-export.log`、`.exit` |
| `tools/art-assets/mod-logo-11/export_logo.py check` | 15/15、exit0；像素同源、alpha、16px无损逻辑缩放、模型/预览引用、两正式目标不存在 | `phase1-check.log`、`.exit`、`phase1-verification.json`、`phase1-manifest.json` |
| `tools/art-assets/mod-logo-11/check_ui.py` | 2/2、exit0；仅允许注册与icon两处逐字变化 | `shared-check.log`、`.exit`、`shared-consumption.diff`、`shared-identity.json` |
| `tools/art-assets/mod-logo-11/check_install.py` | 2/2、exit0；独立读回候选→正式逐字一致，冻结17项本批来源 | `installation-readback.log`、`.exit`、`installation-identity.json`、`installed-source-manifest.json` |
| `.\gradlew.bat assemble` | 唯一增量命令，BUILD SUCCESSFUL in 6s、exit0 | `assemble.log`、`.exit`、`assemble-run.json` |
| 实际JDK21的`javap -p -c`，分别读取两class | 两次exit0；MOD_LOGO注册与icon supplier实际字节码可见 | `ModItems.javap.log/.exit`、`ModCreativeTabs.javap.log/.exit` |
| `build/reports/art/ART-LOGO-11/finalize_package.py` | exit0；两资源四层/两源/两class、JAR全条目差异与17项来源冻结 | `final-package-check.log/.exit`、`final-package-identity.json`、`final-source-manifest.json` |

check原日志保留Pillow关于`getdata()`将在14版移除的两条弃用告警；当前12.3.0检查实际通过，无异常或失败。未为此安装/升级依赖或扩大测试。

## 两新增资源的局部安装

实际读取11卡的“候选实看与两新增资源安装许可”后，复制前同时核两正式目标均不存在、冻结候选及map身份正确，才各复制一次；没有覆盖同名文件、重生候选或修改映射。原始安装动作的工具退出0记录在`installation-action.json`。独立读回结果为：

| 正式新增路径 | SHA256 | 字节 |
| --- | --- | --- |
| `src/main/resources/assets/create_nuclear_industry/models/item/mod_logo.json` | `12b704b0441f7e0c7274e855b1fc4d3b986b39975e0b59bbb3ae1bcffc3417b0` | 118 |
| `src/main/resources/assets/create_nuclear_industry/textures/item/mod_logo.png` | `aa7798fecc9eb620f1eb7451a757605d978664bad863dda10a798af88cd6fb35` | 460 |

资源映射SHA256为`9c94d090704a63476aacf685e5de2f514080e6778c373903ad3e3b92bf276d10`。`installed-source-manifest.json`为安装后、预览阶段文字更新前的历史17文件清单；原`phase1-manifest.json`为候选初交时点，不能作为当前安装状态。当前来源见`final-source-manifest.json`，仍17项，只有README与HTML阶段文案改变，SVG/PNG/model/map及两Java已审身份不变；原15/15像素/引用和2/2共享边界复用，不再运行要求正式目标不存在的phase1检查。

## 唯一增量包与实际保护边界

先将原美术包完整备份到`build/reports/art/ART-LOGO-11/previous-jar/create_nuclear_industry-0.1.0.jar`并独立读回SHA；原10B冻结件未覆盖。`previous-jar-identity.json`保留原包2777331B、SHA256=`9be1d3105034c37d27dd8448b3d1d00e2c375462d2f82b0d66a5b579b2e4ca6a`。

唯一`.\gradlew.bat assemble`在美术树实际执行，6秒成功、exit0；没有`test`、`build`全量、`clean`、GameTest或Minecraft启动。原ModFluids的`initializeClient`弃用警告和ExtensionShieldedAssemblyGameTests弃用提示保留在原日志，无构建失败或第二次build。

| 完整新制品 | 字节 | SHA256 |
| --- | --- | --- |
| `build/libs/create_nuclear_industry-0.1.0.jar` | 2778351 | `d12cca860f69f0b411a99b3c7f34bacc17895c7ac4fb7bbd149616769dc839c5` |
| [本批冻结JAR](../../../build/reports/art/ART-LOGO-11/final/create_nuclear_industry-0.1.0.jar) | 2778351 | `d12cca860f69f0b411a99b3c7f34bacc17895c7ac4fb7bbd149616769dc839c5` |

两新增模型/贴图在candidate→formal→`build/resources/main`→实际JAR逐字一致；独立32px PNG仍与候选PNG同源同字节。两Java修后源身份保持上表，与原共享冻结/精确差异相同。对应实际`build/classes/java/main`两class与JAR逐字一致，class主版本65（Java21）。实际JDK路径`C:/Program Files/Java/jdk-21`的javap输出显示`MOD_LOGO` DeferredItem字段、`mod_logo`字符串经`DeferredRegister.Items.register`写入，以及Tab supplier读取`ModItems.MOD_LOGO.get()`后构建`ItemStack`；没有把静态存在检查扩写为真实客户端加载成功。

完整比较原9BE1与新JAR：只新增Logo JSON/PNG两条，只改变ModItems.class/ModCreativeTabs.class两条，移除0，其余2129个条目逐字相同。1122个原有assets文件全保持，覆盖原实验外壳模型/贴图及此前设备资源；没有顺带更新旧producer或其他美术。主目录两共享源只读实哈仍为协调卡main基线（Items6e83859b…、Tab3ef6d255…）；未写main包、管理员候选、用户实例或世界，未Git。

最终17项清单更新了README/HTML当前阶段文字，保留“离线模拟非Minecraft截图”说明；图形像素、资源映射、正式模型/PNG和共享Java均未改，未因此重跑素材检查或构建。最终包与同轮审查者收尾交回后，用户在美术候选的创造标签实际显示仍需独立观察；主PM后续只取本批UI注册/icon精确差异，不用旧art Tab或包覆盖main现行管网/管理员内容。

## 面向当前main基线的两UI候选补丁

按主PM已确认的最小净集成授权，最后只在本工具目录新增[接收者候选](../../../tools/art-assets/mod-logo-11/main-ui-candidate/)与[标准unified补丁](../../../tools/art-assets/mod-logo-11/main-ui.patch)。生成前实际只读main两源实哈仍为协调基线6e83859b…/3ef6d255…；如漂移则停止，未自行选新基线。本次没有回写main、管理员或art Java，未Git、再build/test或造新JAR。

| 接收者源 | 实际before SHA256 | 候选after SHA256 |
| --- | --- | --- |
| `ModItems.java` | `6e83859b330c20e003142d15f2211ffabe38c7e864a119d6f2dcb45cf0ecea74` | `974f80bd82e6609ccae5dbb04cf5484e6124ec54c7e4c68bc0e265cf0834e6f0` |
| `ModCreativeTabs.java` | `3ef6d255c83d131b2b9260974c2494cce66a96c7383d50309c1510479a29fb88` | `240c45f5a5b7cd3c2519a40bbf6b29477b7991138e58bbbe92f06931a6c45598` |

ModItems候选增加已审原样MOD_LOGO/Javadoc，与art已审修后字节一致；Tab候选基于接收者现行完整列表只替换同一MAIN.icon supplier，完整工业管网五项与全部`displayItems`后缀逐字相同。不是复制旧art Tab；补丁hunk上下文直接来自实际main，包含其现行工业首行。标准hunk在内存中核旧上下文并重建结果，逐字等于候选，未调用Git或把候选复制回源码树。

两候选源位于`main-ui-candidate/src/main/java/com/iksxh/create_nuclear_industry/content/`，独立`scope-proof.json`与`manifest.json`绑定before/after、整个展示后缀SHA、工业五项、实际接收者生成后未改、原art17来源与完整新JAR读回身份不变。补丁1856B、SHA256=`4a205f766ddc818eb361701b766d05f52035bfa2854665aa943d184f724234fd`。原始`main-ui-candidate.log/.exit`与`main-ui-jar-protection.log/.exit`均exit0；当前/冻结JAR仍为d12cca86…9dc839c5。

`final-source-manifest.json`17项保持原最终美术来源历史，不混入这批接收者候选；新交付另以`main-ui-candidate/manifest.json`绑定两源、补丁及scope proof四项。只交主PM净取与原同次审查者的接收者diff/SHA收尾，不以此宣布main/管理员已集成。

## 实际共享消费

执行前实际读取主PM [共享协调](../../../../Create_NuclearIndustry/docs/art/ART-LOGO-11-COORDINATION.md)及任务卡末段临时授权。美术树两原件完整冻结于`build/reports/art/ART-LOGO-11/shared-before/`；没有同步main Tab或引入管网依赖。

| 路径 | 修前SHA256 | 修后SHA256 |
| --- | --- | --- |
| `content/ModItems.java` | `6e83859b330c20e003142d15f2211ffabe38c7e864a119d6f2dcb45cf0ecea74` | `974f80bd82e6609ccae5dbb04cf5484e6124ec54c7e4c68bc0e265cf0834e6f0` |
| `content/ModCreativeTabs.java` | `036f1d9596f37823eb5bf97ec7c192adb9e1596beb0f55553486b2052c887419` | `56a2c887f8b5e62d397ce14560ebd7618b76955efc486c86500686ce2633b519` |

ModItems新增原生延迟`MOD_LOGO`/`mod_logo`注册；Tab仅将MAIN.icon supplier换成`new ItemStack(ModItems.MOD_LOGO.get())`。新增中文Javadoc说明UI职责与服务端无状态边界；不提前实例化，不加入`displayItems`或配方。精确重建与冻结原字节比对保证既有注册、完整展示内容和顺序逐字保持。没有main/管理员候选回写、Git或世界操作。

## 技能与版本应用

已读主AGENTS、治理1.2/5.1、美术README与11卡；使用`minecraft-modding`核锁定版本与DeferredItem/ItemStack延迟读取、`minecraft-resource-pack`约束1.21.1的原生平面模型及RGBA路径、`minecraft-testing`按本批资源/UI风险选择有限加载合同核对。`writing-plans`/`subagent-driven-development`采用已派发单一写集和负责人与唯一执行者分工，不另制治理计划、不子委派或Git；`verification-before-completion`要求实际命令/退出与原始日志支撑报告。设计已由用户选定，未重复询问风格。

实际`gradle.properties`与`build.gradle`核为MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6。全局语言、metadata、旧producer、旧实验外壳模型/贴图、玩法与其他设备资源均不在写集。原15＋2＋2定向证据复用，完成本卡唯一必要增量assemble和制品核对，未新增镜像JUnit或GameTest/全量矩阵。
