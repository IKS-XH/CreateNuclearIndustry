# 正常全量构建交付

准确提交：25abcf437e09861e57985c979b228ace285e5b00；候选工作区 codex/build-baseline-20261106，构建前后HEAD相同，开工与完成git status均干净。同一执行者仅运行一次正常 build --console=plain，无clean/rerun/exclude。

- 命令：.\gradlew.bat build --console=plain；2026-11-06 17:41:16.720～17:42:00.771 +08:00，exit0，墙钟44.052秒，Gradle43秒。
- 9项actionable：jar、test实际执行，7项UP-TO-DATE。classes/testClasses也标UP-TO-DATE；jarJar/processTestResources为NO-SOURCE。完整逐任务状态在BUILD.json/tasks与gradle.log。
- 153个JUnit类、871项：860通过、0失败、0错误、11既有Sodium夹具门跳过。完整XML153份保留；1400裂变/600完整tick严格对照仍实际输出。不是871项全过。
- 构建前后输入清单完全相同（src/main、src/test与Gradle实际文件），没有源码漂移。全量出口按本卡明确授权执行，未重复定向或强制任务。
- JAR重新打包后为3782088 bytes，SHA256 E0ABFC7E0EED70AE2F4476CFFE7DBD7CE2CBD422D6919A13791F31B8CC6D15EF，与保留的original-E0AB.jar逐字节一致。2621载荷全部原字节保持，新增/删除/修改均0。
- 包内825 class全部与本次main编译输出逐字节绑定，缺失/不等0；编译目录额外21个GameTest class逐项列在BUILD.json，均命中既有build.gradle:145-173的开发探针排除规则。测试修复没有改变任何生产载荷。

## 既有11项跳过

XML的skipped元素本身无message；原因来自3类原有@BeforeAll的assumeTrue(directory != null, ...)，本轮未提供cni.sodium.fixtureDir。BoilerSodiumSurfaceTest:56与ReactorSodiumSurfaceTest:56要求“本批真实 Sodium 检查需显式指定已核哈希的夹具目录”；TurbineSodiumAppearanceTest:44要求“真实 Sodium 检查需显式指定已核哈希的夹具目录”。完整名称和源码行/原因逐项保存在BUILD.json，不把SKIP记成业务通过。

- `BoilerSodiumSurfaceTest.actualSliceCapturesOnlyItsOwnPublishedWorldWithoutReadingWorldData()`
- `BoilerSodiumSurfaceTest.actualSliceWritesFinalConnectedWindowModelData()`
- `BoilerSodiumSurfaceTest.actualSliceFeedsNativeCreateContextForConnectedWindows()`
- `BoilerSodiumSurfaceTest.nativeRegionAndUnknownWrapperKeepTheirExistingIsolation()`
- `ReactorSodiumSurfaceTest.actualSliceCapturesOnlyItsOwnPublishedWorldWithoutReadingWorldData()`
- `ReactorSodiumSurfaceTest.nativeRegionAndUnknownWrapperKeepTheirExistingIsolation()`
- `ReactorSodiumSurfaceTest.actualSliceFeedsNativeCreateContextAndFinalModelDataForConnectedWindows()`
- `TurbineSodiumAppearanceTest.actualCurrentAndOldSlicesConnectOnlyTheirOwnLocalNeighbors()`
- `TurbineSodiumAppearanceTest.smallUnknownAndMissingMaterialKeepOriginalQuads()`
- `TurbineSodiumAppearanceTest.nativeAndUnknownContextsKeepTheirBoundaries()`
- `TurbineSodiumAppearanceTest.actualSliceSelectsMediumAndLargeAtlasTilesWithoutReadingBoundWorld()`

没有新增/修改skip规则、未启动客户端或GameTest、未读取世界；本结果不替代Sodium真实夹具、GPU视觉或多人验收。执行者未Git写，待PM处理后续主目录入口；候选制品已经准确源绑定。

可复查入口：本目录audit.ps1（只读Zip/class/XML核验）与BUILD.json；原始gradle.log、process.json、inputs-before/after.sha256、head-before/after.txt、status-before.txt。Windows Store的python/py别名不可执行，因此核验使用PowerShell/.NET，未改环境或安装工具。
