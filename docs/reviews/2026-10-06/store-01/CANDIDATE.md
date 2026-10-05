# STORE-01轻量封存：联合手测候选

**状态（2026-10-06）：** 功能、素材、必要自动验证及一轮合并审查完成；唯一模型显示P2已整改并复核闭合。现停在用户联合手测门，尚未最终验收或把功能合入main。

**功能提交：** `cb2412af6fcb46caabc45fa82de61722621eb925`，分支`codex/ore-acquisition`。后续仅文档提交不改变这份代码和资源。技术栈保持MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280及0.1.0 Alpha。

## 运行候选

本批功能在主工程同级的`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；主目录目前保留上批已验收功能，主目录runClient尚不包含这批封存实现。原测试世界及已有配置保留。

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
.\gradlew.bat runClient
```

[打开联合手测清单](./MANUAL-CHECKLIST.md)。只需一轮，包含原生材料、原燃料制造、三料封装、工序切换、漏斗搬运、16桶满架及拒收、取出/拆除/本版本保存重进、模型和中文显示。

## 本批内容

- 1玻璃磨制为1碎料；碎料、石英粉、黏土球加热搅拌产4固化基材。
- 四铅板、一钢板、一密封环工作台无序产4空铅桶；一屏蔽混凝土、两钢板产1贮存架。
- 装配台以1枯竭组件＋1基材＋1空桶，一次产1有效封装桶，64RPM参考10秒；原新燃料四料及20秒不变，首料自动选工序。
- 单格无GUI贮存架默认16桶，完整保存原始组件；顶/侧投取，底部只取。容量入口为SERVER文件`create_nuclear_industry-storage.toml`的`spentFuelStorage.rackSlots`，详见[配置指南](../../../server-config.md#乏燃料贮存架)。

## 验证及制品

定向JUnit8项全部通过；本批隔离GameTest6项全部通过，正常保存退出；增量test+assemble退出0。素材13模型、9纹理、20种方向/占用组合静态检查通过。只针对审查发现的物品显示父链修正重新运行processResources jar，4秒退出0；行为没有变化，复用原8/6证据。客户端视觉与人工流程尚待用户确认。

最终JAR：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/build/libs/create_nuclear_industry-0.1.0.jar`，2,144,687字节。

SHA-256：`90A96EB4EE401FC56B529120A3A165A822CDAF747F2DBE3A9B49782F1B0E8FB1`。

[实施证据](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/docs/reviews/2026-10-06/store-01/implementation.md)、[素材证据](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/docs/reviews/2026-10-06/store-01/assets.md)、[联合审查](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/docs/reviews/2026-10-06/store-01/review.md)、[静态预览](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/tools/art-assets/store-01/evidence/store-01-static-preview.png)。原始日志在候选`build/reports/extension/STORE-01/`，属于构建产物。新增配置源码已由PM精确强制纳入Git，既有日志、缓存和主目录launch配置未提交。

用户确认联合手测范围通过后，再完成验收与main合入；此前停止自动推进。首发前不安排旧存档兼容矩阵，本批没有辐射、温度、衰变热、再处理或动画实现。
