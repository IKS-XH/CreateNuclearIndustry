# ART-LOGO-11 UI图标共享接入协调

更新（2026-10-14）：用户已验收并明确合入，已按[集成验收](../reviews/2026-10-14/logo-accepted-integration/ACCEPTANCE.md)将本卡批准的两处UI净差异接入main`d8efdc9`，正式包34bedc81…；管理员候选未同步。以下权限/基线记录为原协调时点，不作为当前main哈希或禁止本次已批准集成的依据。

2026-10-13。主PM已只读核美术负责人对话01a11c6b-f440-77c1-9275-5b2dca5cfa0f的直接用户消息：制作mod logo并替换创造标签实验反应堆外壳占位图；用户选择齿轮＋蓝色核芯。此为独立美术授权，不推进管理员02或关闭01人工门。

当前main9fbef42；ModItems实哈6e83859b330c20e003142d15f2211ffabe38c7e864a119d6f2dcb45cf0ecea74与art-studio一致。main ModCreativeTabs为3ef6d255c83d131b2b9260974c2494cce66a96c7383d50309c1510479a29fb88，art为036f1d9596f37823eb5bf97ec7c192adb9e1596beb0f55553486b2052c887419；两路径均无未提交改动。当前管理员01R唯一执行者范围不包含这两路径。

**不能整份同步main Tab至美术：实际美术树不存在IndustrialFluidContent.java，main Tab新增五个管网引用会造成编译缺依赖。** 不为logo搬入整套管网或清理旧树。

仅临时授权美术11原唯一资产/消费执行者在art-studio修改两共享路径：`content/ModItems.java`新增UI-only `MOD_LOGO`/`mod_logo` Item注册，无配方、不入displayItems/功能状态；`content/ModCreativeTabs.java`只将MAIN.icon supplier改为`new ItemStack(ModItems.MOD_LOGO.get())`。各树其余列表、顺序、注册逐字保持；当前main与管理员候选不回写。新增手写注释中文，正式模型/贴图ID依美术11卡。

消费后交两路径的原/新哈希及精确差异，生产验收证据与素材同一批唯一必要增量包、同一次组合窄审；不独立重造共享前置包。Git与未来净集成由主PM处理：main采用仅注册/图标两个净改动，管理员候选已有自动化列表也须保留，不以旧art Tab覆盖任何新列表。美术原已验资源、候选、main用户dirty及全部世界保持；无额外负责人转授。
