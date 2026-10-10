# REACTOR-ROD-COOLANT-01 实施报告

## 独立复核后的最终整改候选

基线与写集保持；此次仅修改新辅助类和专属测试。独立复核确认第一轮固定液体使仪表末尾的原生 `RenderType.translucent()` 辉光共享批次留到液体之后。先新增两项真实 Native BufferSource 回归，`04-glow-red` 命令退出 1，7 项中仅新增两项顺序断言失败，15 秒；失败实际序列缺少液体之前的末尾辉光，不是环境/编译失败。

生产整改仅增加一行 `buffers.endBatch(RenderType.translucent())` 与相应中文说明。最终 AFTER_BLOCK_ENTITIES 顺序为 cutoutMipped 棒体、translucent 辉光、固定液体；不调用全量 endBatch，不改原辉光 shader/几何/亮度或液体状态。新增回归覆盖共享/固定棒体、两 owner 交错、阶段过滤、无关 solid 留存及空边界幂等。

因该具体新风险，补唯一一次同样三个类的定向 test 加增量 jar：`05-final-command.txt` 中完整命令，退出 0，16 秒，**29/29**（缓冲 7、VisualState 15、Materials 7），零失败/错误/跳过。compileJava、test、jar 实际执行，compileTestJava 复用本次 RED 已编译的新测试；9 项中 3 执行、6 UP-TO-DATE。完整日志/退出码/时间为 `05-final.log`、`05-final.exit`、`05-final-run.json`，最终 XML 在 `05-final-xml/`。

当前 JAR：`E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/build/libs/create_nuclear_industry-0.1.0.jar`，**2507570 字节**，SHA-256 **`0de3aa3c20a99850f56d68f2ab8fd0a5538aadcfb9cfd2628f84553f8500c2d5`**。冻结制品/三个源码在本批 `05-final-candidate/`；准确源与三个生产 class 的编译/JAR逐字绑定见 `06-final-identities.json`，刷新调用字节码见 `06-final-helper.javap.txt`，仅辅助类/测试的差异见 `06-helper-delta.diff`、`06-test-delta.diff`。Renderer 源 SHA 保持 `51701712aa483fa170566abc88252165140c0db7b72528c16ff4c2a026b286b7`；最终 helper SHA `fabc64b39f5fc0c3158916b5a328348673eb1622226d41a7de2763a0dd7e7db7`，最终测试 SHA `e3c761dbbc11cd8d99d055ad5c731070454dda67736285aba2fe5d16f8fd93cf`。

已实际读取新卡末节及 receiving-code-review，复用本批锁定原生来源与已读 Minecraft/排错/TDD/验证技能。01/02/03、第一轮源码/JAR和报告证据保持为中间候选；当前身份只以 05/06 为准。此轮没有其他源码、Git、全量、素材、客户端或世界操作；真实 GPU 视觉仍待用户复看。交同一审查者 delta 复审，执行者停写。

## 第一轮中间候选记录（保留）

工作区 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`，HEAD `e50ef067b19296a22e7b2538ccc367f3cda6022f`；交付未提交源码与本报告，未做 Git 写操作。

已按补齐后的卡，仅修改 `ReactorInternalRenderer.java` 的液体取缓冲入口并增加窄测试接缝；新增 `ReactorInternalRenderBuffers.java`、`ReactorInternalRenderBuffersTest.java`。液体仍使用原生 entityTranslucent，现有 256 个材质槽在客户端注册固定缓冲，初始总容量 393216 字节（384 KiB），不新增 DynamicTexture。AFTER_BLOCK_ENTITIES 中先定向结束 cutoutMipped 棒体，再结束液体批次；其他 RenderType 不由该入口结束。注册和游戏阶段监听分别订阅客户端 mod/game 总线，类初始化只创建层列表，不分配原生缓冲。

该阶段位于全部 BER、原生不透明 Sheet 与 outline 提交之后，主要区块透明层之前。固定液体不再因 BER 的共享层切换提前画入深度；原 shader、LEQUAL、COLOR_DEPTH_WRITE、混合、alpha、采光、UV、mesh 与纹理池保持。原杆 BER、Materials、VisualState、生命周期事件、模型/素材、共享控件、L1/L2及服务端只读；六个相关保护路径 `git diff --exit-code` 退出 0。正常 JUnit 日志变化保留。

RED 先建立保持旧行为的生产取缓冲接缝，以及原先无固定注册/阶段刷新的辅助入口。真实原生 BufferSource 调度执行，观察 startedBuilders 与空批次 endBatch，不复制刷新公式或调用 GPU。唯一 RED 命令 `.\gradlew.bat test --tests '*ReactorInternalRenderBuffersTest' --console=plain` 退出 1，5 项中 4 项行为断言失败：液体在棒体/另一纹理切换时提前结束、无固定槽、原共享路径结束了其他批次；原生 shader/深度/混合身份检查通过。不是编译、缺类、NPE 或无 GPU 失败。完整 `01-red.log`、`01-red.xml`、`01-red.exit`、`01-red-failures.json` 保留。

最小修复后唯一最终命令：

```powershell
.\gradlew.bat test --tests '*ReactorInternalRenderBuffersTest' --tests '*ReactorAnimationVisualStateTest' --tests '*ReactorAnimationMaterialsTest' jar --console=plain
```

现有 JDK 21，退出 0，17 秒；27/27 项通过，0 失败/错误/跳过：新缓冲回归 5、原 VisualState 15、原 Materials 7。覆盖两种 BER 顺序、多纹理交错、固定/共享 cutoutMipped 键、阶段过滤、边界棒体先于液体、无关层保留、空边界、256 槽与原生状态。实际执行 compileJava、compileTestJava、test、jar；9 项中 4 执行、5 UP-TO-DATE。编译有原版本 EventBusSubscriber.Bus 的待删除警告，无构建失败；不升级 API 或依赖。

全部证据在 `build/reports/reactor-rod-coolant-01/`：`02-green.log` / `.exit` / `02-green-run.json`，最终三份 XML 在 `02-final-xml/`；输入/输出来源清单 `03-candidate-identities.json`，生产调用字节码 `03-production.javap.txt`，原渲染器差异 `03-renderer.diff`。源与 JAR 已冻结到 `candidate/sources/`、`candidate/create_nuclear_industry-0.1.0.jar`。源码 SHA-256：

- Renderer：`51701712aa483fa170566abc88252165140c0db7b72528c16ff4c2a026b286b7`
- RenderBuffers：`6b7bb0d28d38d36ef6d312887a17d9347f3da015a8f7f523d20987bff94ddc2f`
- Test：`cb619842e28eba05900df895b3647cb77637624702a4ed75f76e643ece1ca229`

实际 JAR：`E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/build/libs/create_nuclear_industry-0.1.0.jar`，2507556 字节，SHA-256 `f3519cc6afb9dad55815228480ee4aa07cebdc9b080084925fceebed5723ff76`。Renderer、辅助类及嵌套 GameEvents 三个 class 与本轮编译输出逐字匹配，散列在来源清单中。差异空白检查退出 0。

实际应用已读 AGENTS/本卡/治理 5.1/5.2、Minecraft modding/testing、系统排错、TDD 与完成前验证；锁定版本未变。未全量、clean、强制重跑、素材生成、GameTest、客户端或用户世界操作。此次自动证据证明真实缓冲保留/刷新行为及原数值/材质保护；不证明 GPU 最终外观，仍需用户在主分支复看空/部分/满液位及控制棒升降。执行者停写，交 PM 独立窄审与 Git 管理。
