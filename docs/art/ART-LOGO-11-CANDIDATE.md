# ART-LOGO-11 Logo与创造标签图标交付

2026-10-13。用户已选择“齿轮＋蓝色核芯”：钢灰齿轮、铜色轮圈、蓝白核芯、图标无文字。原创32px像素SVG是唯一绘图来源，全部PNG由同一SVG确定性导出。

- [可编辑SVG](../../tools/art-assets/mod-logo-11/source/mod_logo.svg)
- [32px透明PNG](../../tools/art-assets/mod-logo-11/generated/mod_logo_32.png)、[128px](../../tools/art-assets/mod-logo-11/generated/mod_logo_128.png)、[256px](../../tools/art-assets/mod-logo-11/generated/mod_logo_256.png)
- [原生尺寸/明暗预览](http://127.0.0.1:8735/tools/art-assets/mod-logo-11/index.html)与[离线检查图](../../tools/art-assets/mod-logo-11/generated/preview-sheet.png)

## 实际图标接入

美术树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`、分支codex/art-studio。按[11任务卡](tasks/ART-LOGO-11.md)及主PM共享协调，ModItems延迟注册UI-only MOD_LOGO/mod_logo；MAIN.icon supplier现在返回该物品堆栈，原实验反应堆外壳不再作为标签图标。两新正式资源为models/item/mod_logo.json与textures/item/mod_logo.png，采用原生item/generated平面模型。

这不是替换实验外壳自身模型或移除它的普通物品；创造页完整内容和顺序保持，logo不列入displayItems、没有配方或新增Gameplay状态。当前源码与美术JAR已包含该图标，游戏内实际视觉仍需用户观察。

## 实际验证与制品

原SVG72个2px网格rect/12色，原15/15素材检查、2/2共享精确范围检查、2/2安装读回均exit0。负责人实际看过16/24/32px及明暗背景，浏览器截图在本对话可视化根logo11-preview.png；这些是离线视觉证据，不冒称Minecraft截图。

唯一必要增量assemble为6秒exit0；原ModFluids/ExtensionShieldedAssemblyGameTests弃用提示保留。两资源在候选/安装/build/JAR四层一致，两源码及实际两class编译/JAR一致，Java21主版本65、实际javap显示注册与supplier。相比原9BE1只新增两资源、改变两class、删除0，其他2129条目/1122既有assets保持。同一次独立组合审查最终无剩余必改。证据见[执行报告](reports/ART-LOGO-11-ASSETS.md)、[审查](reports/ART-LOGO-11-REVIEW.md)及build/reports/art/ART-LOGO-11/。

当前美术JAR和[完整冻结副本](../../build/reports/art/ART-LOGO-11/final/create_nuclear_industry-0.1.0.jar)均2778351字节，SHA256：

`d12cca860f69f0b411a99b3c7f34bacc17895c7ac4fb7bbd149616769dc839c5`

原9BE1完整备份在本批previous-jar，10B原冻结件保持。未Git、自动启动Minecraft或搬动/回写世界。

## 主目录与后续

当前main仍由主PM处理最小净接入，不把该旧美术基线包直接替代现行main包。主PM已核用户直接Logo/图标替换授权，无需再询问Logo方案；会先完成其当前自动化接口改装故障窄修，再按独立集成登记只取UI注册/唯一supplier差异与两新资源，保留主目录管网和管理员候选的完整列表。

针对实际main接收者另提供[精确补丁](../../tools/art-assets/mod-logo-11/main-ui.patch)及[独立候选清单](../../tools/art-assets/mod-logo-11/main-ui-candidate/manifest.json)。其两源候选基于实际main的6e83859b…/3ef6d255…，仅增加原已审注册并替换唯一icon supplier；当前五项工业物品和完整displayItems逐字保持。接收者Tab候选为240c45f5…，补丁为4a205f76…，实际范围与基线身份见同目录scope-proof.json。原美术shared-consumption.diff作为历史消费证据保留，不用其旧Tab整份同步main；原17来源和d12cca86…JAR不因接收者交付改变。

原同次独立审查者已完成接收者补丁收尾，无剩余必改项；负责人读回实际main两源及补丁/JAR身份一致。没有应用补丁、写main或再次构建。交回主PM时同时绑定此接收者四项独立清单与原两正式资源、最终制品报告，后续接入仍由主PM登记。

检查新标签图标需要重启包含这两Java改动的客户端，F3+T不能替换注册/supplier代码。本候选的独立已有run/saves与主目录世界不互相覆盖。未把新图标上线记为用户已手测，也不借此推进离心机或其他主线。
