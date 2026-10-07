# STORE-01与数值优化：已验收候选记录

**状态（2026-10-06）：** 用户确认全部联合手测通过，封存/16桶架、设备有序配方和总产热取整已验收合入main，见[最终验收](./ACCEPTANCE.md)。本页保留接收候选的原证据与制品，不再表示待验收；新思索候选另见[离心机任务](../../../archive/2026-10-08-completed-plans/2026-10-06-device-ponder-01-centrifuge.md)。

**功能提交：** `3b1bc0a5b9f4c3616bb17cddab3796dc78323b43`，分支`codex/ore-acquisition`；包含原封存/有序配方候选和追加产热取整。后续仅文档提交不改变这份代码和资源。技术栈保持MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280及0.1.0 Alpha。

## 运行候选

本批功能已合入主目录`E:/MyMC/NewMod/Create_NuclearIndustry`，主目录runClient包含全部已验收功能；同级候选及原测试世界、配置保留，后续离心机思索仍在该候选实施。

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
.\gradlew.bat runClient
```

[已通过的联合手测清单](./MANUAL-CHECKLIST.md)保留实际范围与补充，不重复运行。主目录合入增量打包见[记录](./main-integration.md)。

## 本批内容

- 1玻璃磨制为1碎料；碎料、石英粉、黏土球加热搅拌产4固化基材。
- 四铅板、一钢板、一密封环按环顶中/铅两侧/钢底中的工作台有序布局产4空铅桶；一屏蔽混凝土、两钢板产1贮存架。
- 其余13条设备工作台配方也改为有序；原料、标签、数量和产量不变。粗矿块及铅/锡锭的五条拆解保持原无序，三处导出器已同步。
- 装配台以1枯竭组件＋1基材＋1空桶，一次产1有效封装桶，64RPM参考10秒；原新燃料四料及20秒不变，首料自动选工序。
- 单格无GUI贮存架默认16桶，完整保存原始组件；顶/侧投取，底部只取。容量入口为SERVER文件`create_nuclear_industry-storage.toml`的`spentFuelStorage.rackSlots`，详见[配置指南](../../../server-config.md#乏燃料贮存架)。
- 反应堆新生裂变热总量限幅后向上取整一次，按原比例分给各列，正式冷却与遥测共用；燃耗、缓存余热与热端设备参数保持原规则。

## 验证及制品

**产热取整：** [本项报告](./reactor-heat-rounding.md)记录原代码6.75→7红测失败；7类相关JUnit首轮49项中的39项未改通过证据复用，两类旧期望修正后仅复验10项并assemble成功，最终有效49项全过。PM已逐项审源码和测试差异；正式tick连续10步23.604…→24HU/48mB证明列热、冷却、库存及遥测一致，全插棒缓存0.75HU只转1mB并保留0.25HU。GameTest只同步断言并编译，未启动测试服或客户端。观察现有机组并入联合清单补充，不另开人工轮次。

**工作台整改：** 铅桶R1已获用户手测确认。CRAFT-SHAPED-01完成其余13条设备有序JSON和三个导出器同步；基线多重集合、完整result及元数据核对通过，五条拆解及铅桶字节不变，src与工具无设备/零件无序漏项。唯一processResources jar资源重包3秒退出0，未跑JUnit/GameTest/客户端或全量导出。PM已审实际差异，并从最终JAR核对13条修改、五条拆解及铅桶共19条与源逐字节一致；简短报告见[本轮证据](./crafting-shaped.md)。代表性工作台/JEI与搅拌机不匹配确认并入原清单第1项。

**R1历史证据：** 原铅桶数量4铅/1钢/1环产4桶，资源重包3秒退出0；报告见[R1](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/docs/reviews/2026-10-06/store-01/lead-cask-shaped.md)。封存和架子逻辑未改，原8/6行为证据继续复用。

定向JUnit8项全部通过；本批隔离GameTest6项全部通过，正常保存退出；增量test+assemble退出0。素材13模型、9纹理、20种方向/占用组合静态检查通过。只针对审查发现的物品显示父链修正重新运行processResources jar，4秒退出0；行为没有变化，复用原8/6证据。客户端视觉与人工流程已获用户确认。

验收时候选JAR（功能3b1bc0a）：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/build/libs/create_nuclear_industry-0.1.0.jar`，2,145,606字节。路径会随后续教学候选打包更新，下述SHA仅标识本次接收制品。

SHA-256：`B224EF40A6EFE02D994EF0451492A8E4A5331EE4809F7311A48011144971A971`。

[实施证据](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/docs/reviews/2026-10-06/store-01/implementation.md)、[素材证据](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/docs/reviews/2026-10-06/store-01/assets.md)、[联合审查](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/docs/reviews/2026-10-06/store-01/review.md)、[静态预览](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/tools/art-assets/store-01/evidence/store-01-static-preview.png)。原始日志在候选`build/reports/extension/STORE-01/`，属于构建产物。新增配置源码已由PM精确强制纳入Git，既有日志、缓存和主目录launch配置未提交。

用户已确认联合范围通过并完成main合入。主线按新指示暂停，现有设备思索逐台制作与验收；首发前不安排旧存档兼容矩阵，本批没有辐射、温度、衰变热、再处理或动画实现。
