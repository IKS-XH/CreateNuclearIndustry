# STORE-01轻量封存：联合手测候选

**状态（2026-10-06）：** 功能、素材、必要自动验证及合并审查完成；唯一模型显示P2已闭合，用户确认铅桶R1有序配方手测通过。其余13条设备配方有序整改已完成，五条拆解保留无序。现停在原联合手测门，尚未整体验收或把功能合入main。

**功能提交：** `f2cbb593ce1751bad28ce6406a878f6601c851c1`，分支`codex/ore-acquisition`。后续仅文档提交不改变这份代码和资源。技术栈保持MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280及0.1.0 Alpha。

## 运行候选

本批功能在主工程同级的`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；主目录目前保留上批已验收功能，主目录runClient尚不包含这批封存实现。原测试世界及已有配置保留。

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
.\gradlew.bat runClient
```

[打开联合手测清单](./MANUAL-CHECKLIST.md)。只需一轮，包含原生材料、原燃料制造、三料封装、工序切换、漏斗搬运、16桶满架及拒收、取出/拆除/本版本保存重进、模型和中文显示。

## 本批内容

- 1玻璃磨制为1碎料；碎料、石英粉、黏土球加热搅拌产4固化基材。
- 四铅板、一钢板、一密封环按环顶中/铅两侧/钢底中的工作台有序布局产4空铅桶；一屏蔽混凝土、两钢板产1贮存架。
- 其余13条设备工作台配方也改为有序；原料、标签、数量和产量不变。粗矿块及铅/锡锭的五条拆解保持原无序，三处导出器已同步。
- 装配台以1枯竭组件＋1基材＋1空桶，一次产1有效封装桶，64RPM参考10秒；原新燃料四料及20秒不变，首料自动选工序。
- 单格无GUI贮存架默认16桶，完整保存原始组件；顶/侧投取，底部只取。容量入口为SERVER文件`create_nuclear_industry-storage.toml`的`spentFuelStorage.rackSlots`，详见[配置指南](../../../server-config.md#乏燃料贮存架)。

## 验证及制品

**工作台整改：** 铅桶R1已获用户手测确认。CRAFT-SHAPED-01完成其余13条设备有序JSON和三个导出器同步；基线多重集合、完整result及元数据核对通过，五条拆解及铅桶字节不变，src与工具无设备/零件无序漏项。唯一processResources jar资源重包3秒退出0，未跑JUnit/GameTest/客户端或全量导出。PM已审实际差异，并从最终JAR核对13条修改、五条拆解及铅桶共19条与源逐字节一致；简短报告见[本轮证据](./crafting-shaped.md)。代表性工作台/JEI与搅拌机不匹配确认并入原清单第1项。

**R1历史证据：** 原铅桶数量4铅/1钢/1环产4桶，资源重包3秒退出0；报告见[R1](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/docs/reviews/2026-10-06/store-01/lead-cask-shaped.md)。封存和架子逻辑未改，原8/6行为证据继续复用。

定向JUnit8项全部通过；本批隔离GameTest6项全部通过，正常保存退出；增量test+assemble退出0。素材13模型、9纹理、20种方向/占用组合静态检查通过。只针对审查发现的物品显示父链修正重新运行processResources jar，4秒退出0；行为没有变化，复用原8/6证据。客户端视觉与人工流程尚待用户确认。

最终JAR：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/build/libs/create_nuclear_industry-0.1.0.jar`，2,145,081字节。

SHA-256：`670F89993F555A89FDE0690D8A29F76A9DC51B7F6BC63CB49C6393A34EC9A1E7`。

[实施证据](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/docs/reviews/2026-10-06/store-01/implementation.md)、[素材证据](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/docs/reviews/2026-10-06/store-01/assets.md)、[联合审查](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/docs/reviews/2026-10-06/store-01/review.md)、[静态预览](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/tools/art-assets/store-01/evidence/store-01-static-preview.png)。原始日志在候选`build/reports/extension/STORE-01/`，属于构建产物。新增配置源码已由PM精确强制纳入Git，既有日志、缓存和主目录launch配置未提交。

用户确认联合手测范围通过后，再完成验收与main合入；此前停止自动推进。首发前不安排旧存档兼容矩阵，本批没有辐射、温度、衰变热、再处理或动画实现。
