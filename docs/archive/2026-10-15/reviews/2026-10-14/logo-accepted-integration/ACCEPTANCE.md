# ART-LOGO-11 验收与主分支净集成

2026-10-14。用户在主对话明确：“新绘制的logo通过验收了，合并进主分支吧”。据此关闭本批Logo整体视觉门，未补写用户未报告的逐项客户端观察数据。已按[集成卡](../../../plans/2026-10-14-logo-accepted-integration.md)净合main，生产/可编辑来源提交`d8efdc9`。

## 已接入

- 原创钢灰齿轮、铜圈、蓝白核芯Logo；可编辑SVG、32/128/256透明PNG、导出工具和预览完整保留于`tools/art-assets/mod-logo-11/`。
- `ModItems.MOD_LOGO`的UI-only延迟注册及MAIN.icon supplier，创造标签由实验反应堆外壳占位图改为Logo。Logo没有配方、不入displayItems，外壳自身物品和模型保持。
- 两新正式资源`models/item/mod_logo.json`、`textures/item/mod_logo.png`；MC1.21.1平面物品模型及单层贴图。

仅应用已审接收者`main-ui.patch`，没有复制旧art Tab。接入后Items SHA为`974f80bd82e6609ccae5dbb04cf5484e6124ec54c7e4c68bc0e265cf0834e6f0`，Tab SHA为`240c45f5a5b7cd3c2519a40bbf6b29477b7991138e58bbbe92f06931a6c45598`，逐字等于已审主接收者候选。五项工业管网物品和整个displayItems、顺序、其他注册字节保持。模型与贴图SHA分别`12b704b0441f7e0c7274e855b1fc4d3b986b39975e0b59bbb3ae1bcffc3417b0`、`aa7798fecc9eb620f1eb7451a757605d978664bad863dda10a798af88cd6fb35`。

原[美术候选](../../../art/ART-LOGO-11-CANDIDATE.md)、[执行报告](../../../art/reports/ART-LOGO-11-ASSETS.md)、[唯一审查](../../../art/reports/ART-LOGO-11-REVIEW.md)及11卡按原时点复制保留。原17来源和接收者四项清单在接入前实哈核对通过；本次22条生产与工具来源另冻结，不改写旧美术清单。

## 验证与当前主包

原素材15/15、共享2/2、安装2/2及同一独立组合窄审无剩余必改的证据复用，没有重复生成、测试或客户端观察。`git apply --check`和实际接入通过，生产两路径空白检查通过。全写集空白检查只指出历史统一补丁中两行合法的空上下文前导空格；按已审补丁原字节保留，未将其当作生产代码错误或为此重格式/重打包。

主目录唯一必要`.\gradlew.bat assemble --console=plain`实际**11秒exit0**。原ModFluids弃用提示保留；未clean、未完整build或重复设备域。原主包已完整备份为`build/reports/logo-accepted-integration/previous-main-8c03.jar`，3048078B，SHA256 `8c03dbd11e4812705b18942407ae4dd64aba8e7c0cbbc18d03ab2690a0ac1f23`。

当前`build/libs/create_nuclear_industry-0.1.0.jar`与本批`main-logo-reviewed.jar`冻结副本一致，**3049098B**，SHA256：

`34bedc81d6cf0bbf53628f3f7511d031c2966dff1b0cb3fa9736927debb0be31`

实际ZIP文件载荷比较：只新增上述两个Logo资源，只改变`ModItems.class`、`ModCreativeTabs.class`，删除0，其余2149文件逐字保持。两class与当前编译输出一致、major65；两资源在正式/source、build和JAR逐字一致。当前包609个class是全包实数，不宣称历史GameTest全部排除。原美术`d12cca86…`包未替代主包。

原始命令日志、退出码、精确四项载荷验证、22来源及冻结包在主目录`build/reports/logo-accepted-integration/`。本次未新建/清理工作树、搬世界、写运行配置，其他dirty和用户包保持；美术树/原素材/原包保留。管理员候选未同步，01/01R与02人工门、GUI设计讨论及其他主线状态不因本次Logo验收变化。
