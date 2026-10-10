# DEVICE-PONDER-08-STORAGE 独立规格与质量窄审

2026-10-11；工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`，实际基线 `96fde12fe54b65112f1790ede812cd1a797c27fd`。卡中 `2f633749` 为同步历史，本次按 PM 明确的实际基线审查未提交冻结实现。

**规格结论：通过。质量结论：通过。必改项：无。** 自动证据与静态审查不关闭单页 W 播放门；人工门前不合 main、不推进其他主线。

## 规格核对

- 唯一公开故事线 `usage`，插件只增加贮存架物品到 `dry_storage_usage` 的直接绑定，无第二页。跟踪差异仅插件两行及两语言各七键；旧七类教学入口、顺序与正文保持。中英文六段正文依次介绍手持存入、空手取出/背包满保留、漏斗输入输出、护目镜占用、满架等待及腾位继续存入，没有具体配方、潜行要求、GUI/动力课或未实现承诺。
- 核对正式 `DryStorageBlock/BlockEntity/State` 与 `SpentFuelPayload`：手动存取、各面输出、底面拒绝输入、默认容量及背包满保留与字幕一致。教学桶通过正式 `SpentFuelPayload.seal` 创建；独立回读真实压缩 NBT，样例为数量1的正式桶，`minecraft:container` 内 slot=0、数量1的正式 `cooled_spent_fuel_assembly`。锁定 MC1.21.1 的 `ItemContainerContents.Slot.CODEC` 确实消费 `slot`/`item` 列表格式。
- `inventorySnapshot` 逐槽 `sealed.copy()`，保存 `Slot0..Slot15`，容量16，不以单栈 count=16替代。原 `DryStorageState.load` 逐槽解析，快照与 `storage_level=(used+3)/4` 的默认容量比例一致；正文明确16为可配置的默认值。现有测试还检查槽间修改独立、0/15/16占用。
- 教学仅排入 Ponder 临时世界的 `modifyBlockEntity`/`modifyBlock`，经已有 `loadWithComponents` 读入快照。实际核对锁定原生加载入口调用 `loadAdditional`、正式 `current()` 仍拒绝客户端事务；没有调用正式插入/取出/tick/同步，没有新增 setter 或权限放宽。教学快照并不声称通过真实服务端物流事务测试。
- 真实模板为5×5×5、30块、唯一南向空架；25块地台 y=0，其余设备 y>=1。架(2,2,2)，东侧输入漏斗(3,2,2)朝西，上方源桶(3,3,2)；底部输出漏斗(2,1,2)朝南连接承接桶(2,1,3)。存入时上游单件槽及图标清空，占用增加；满架保持额外桶在上游，最后16→15→16，取出的桶保存在承接桶，不表现销毁或吞物。

## 质量与实际证据

- 实际读取根 AGENTS、完整活动卡、治理5.1/5.2、实施报告、复核包、七交付路径和必要正式语义/API；应用 Minecraft modding/testing/resource-pack 技能。版本核对为 MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，不套用新版本资源或迁移示例。复核包开头的 `System.Object[]` 未提供可用跟踪差异，审查已直接读取实际 Git diff，未依赖该占位输出。
- 六次正文调用统一 `note`，每次 `idle(duration+20)`；显示时长80/85/110/100/85/90 tick，逐段覆盖淡出与净空，最后15 tick后收尾。高亮只指架体及具体物流路径。取景0.9缩放、5格地台和设备位置集中；核读原生 SceneTransform/UIRenderHelper 字节码及实际 RotateSceneInstruction，180°为相对增量，默认145°转到325°，GUI仅翻转Y，与 `04-camera.json` 的南/东观察方向一致。此推导不能确认不同屏幕下正文或底部播放条遮挡。
- 原 `01-red.log/.exit/.xml` 相符：4项4个 AssertionFailedError，0 error/skip，exit1，分别缺失实际入口、场景或模板，不是编译/环境失败。原生成和只读核对日志记录确定性压缩及独立落盘回读，exit0；审查没有重新运行生成器或其验证函数。
- 原最终 `04-final-command.txt/.log/.exit` 与两份 XML 相符：新合同4项、原P1合同6项，总10/10，0 fail/error/skip，17秒、exit0；实际 compileJava、jar、compileTestJava、test，9任务4执行5复用。03首轮与镜头修正保留为中间证据，最终以04为准。合同涵盖实际NBT、独立槽与单故事线，并含静态绑定/字幕时序检查；不把字符串检查当作播放成功。
- 独立重算七当前源与 `04-frozen/`，均逐字匹配 `04-manifest.json`。实际/冻结 JAR一致，2514642字节，SHA256 `35c4befd197051338c7840c8f4e80d66e4462d1090de2f7ab1a27b6853b17da7`。两个生产class的JAR条目匹配当前编译输出；新模板、两语言的源/build资源/JAR逐字一致。双语各498个旧键值与旧顺序保持，只各加七键。
- 当前 Git状态/差异未见越出七路径的源改动；冻结保护清单记录1715个范围外输入一致、19旧模板与22正式贮存资源打包一致，本审查读取记录并复用，不重跑保护矩阵。原日志与pycache保留，作用域空白检查原证据exit0。中文注释说明了教学客户端、单位、快照和正式事务边界。

## 未验边界与交接

本次仅写本报告，没有 Git写、改代码、Gradle、JUnit、生成器、客户端、真实服、世界操作或再派发。完整原始证据在 `build/reports/extension/DEVICE-PONDER-08-STORAGE/`。

仍需用户 W 播放确认：只有一页，手动存取清楚，两路漏斗可区分，桶图标和占用/满架等待可理解，机体/路径/字幕不被播放条遮挡且正文不重叠。真实GPU物品识别、屏幕位置及动画可读性未验；不覆盖独立美术视觉门或重复验收正式贮存功能。报告冻结，审查者停写交 PM。
