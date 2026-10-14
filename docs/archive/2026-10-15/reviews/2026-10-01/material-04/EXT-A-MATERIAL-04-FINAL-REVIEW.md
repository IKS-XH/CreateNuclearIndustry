# EXT-A-MATERIAL-04 + EXT-ART-06 整批终审报告

**审查结论：未发现开放的 Critical、Important 或 Minor 问题，可由 PM 保存候选并交用户进行客户端验收。** 本结论只说明当前差异及证据具备进入人工门的条件，不是执行者验收整批、批准合入 main 或批准下一批实现。

- 审查日期：2026-10-01；角色：整批终审执行者。
- 候选：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，`codex/ore-acquisition`，派发基线 `9295e48`。
- 读取了 AGENTS、治理协议、活动准备计划、材料04参数方案与实施卡（含 `verify.py` 补充写集），完整功能/素材差异包、四份交付/分审报告及其最终补审，并抽查 PM 新增归档 README 和客户端清单。卡中调度记录随后由 PM 更新，未把历史过程状态当成功能问题。
- 唯一写入为本报告。未修改 src/tools/docs，未进行 Git 写操作、运行 Gradle/游戏、重跑 export/verify.py 或派发代理。

## 问题分级

| 等级 | 开放问题 | 处理建议 |
| --- | --- | --- |
| Critical | 无 | 无阻断项。 |
| Important | 无 | 无需功能或素材整改。 |
| Minor | 无 | 已关闭的预览标题与 SVG 注释两项不重复计入。 |

## 整体接口抽查

1. **身份与资源闭合。** `src/main/java/com/iksxh/create_nuclear_industry/content/BasicMaterialContent.java:28-34` 注册三个普通成品和两个原生 `SequencedAssemblyItem`；`ModCreativeTabs.java:61-63` 只展示三成品。五项模型的 `layer0`、中英文名称、PNG、SVG、manifest 均对应同名 ID，中文 `tin_wire` 为“锡条”。实际查看五项原尺寸/明暗底预览：锡条是两根错位细长条，两类传感器及各自半成品有区分。没有把离线图像判断提升为客户端视觉通过。
2. **配方沿原生合同闭合。** `src/main/resources/data/create_nuclear_industry/recipe/stonecutting/tin_wire.json:2-4` 是唯一锡条生产配方，`c:ingots/tin` → 2条；两条 `recipe/sequenced_assembly/` 文件分别使用铁板→锡条标签→红石→电子管→压片、铅板→本模组工业传感器→电子管→压片，均仅一轮、单一单件结果。两个 `data/c/tags/item/wires*` 标签采用追加语义。未见竞争切割、逆向拆解、传感器检测行为、自定义进度协议、热级或速度规则。
3. **锁定依赖支持该实现。** 现场读取缓存的 Create `6.0.10-280-sources.jar`：`SequencedAssemblyItem.java:10-28` 将堆叠设为1并从原生组件取进度；`SequencedAssemblyRecipe.java:113-143` 按步生成带配方身份的单件半成品并在末步取结果；`SawBlockEntity.java:395-402` 仅在原生开关允许时纳入切石配方。本批没有覆盖这些生产实现。技术栈现场仍是 MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，版本与许可证未变。
4. **测试没有用自造终态代替机器结果。** `ExtensionSensorProcessingGameTests.java:321-377` 的两种完整装配实际由 Deployer/Press 和世界 tick 推进，逐步检查耗料、组件身份、步数及终产1件；`:270-316` 覆盖工业路线代表性错序、缺料及断电恢复；`:161-212` 覆盖锯的堵塞与配置边界。`:226-238` 单独构造组件做序列化往返，报告准确限定为编码测试。夹具切换同一操作位的机械手与压片机，不证明整条自动物流具有原子事务，也没有作这种声明。
5. **艺术管线写集仍固定。** `tools/art-assets/pipeline.py:69-103` 保留历史51条及冻结的8项冷却剂哈希，仅增加本批明确5条，manifest 必须精确匹配65游戏路径加lapis候选；`verify.py` 的变化限于批准的计数、阶段集合和说明，原失败不写与旧图保持断言仍在。

## 本次只读证据核对

- 独立打开最终 JAR 并逐项比较当前 assets/data：**279资源、65PNG，缺失或字节不一致为0**。重新计算 JAR SHA-256 为 `5D47E144FA0909C73B513D8B21BB88589696E4536F9C3F6EE98A567A699791AD`，与执行者、分审及 PM 记录相同。
- 独立打开归档 `functional-evidence.zip`：**78条目，其中52份原始JUnit XML，合计265项，failure/error/skipped均0**；无世界、缓存或JAR。ZIP SHA-256 为 `C1E2DA17715EFB69255DCD41DD7B2A77F08EE673FF613092CA7606BBE9B5A3E8`。归档构建日志记录 `BUILD SUCCESSFUL`；这是对执行者本轮原始证据的核查，不是终审者重跑。
- 最终 GameTest 原始日志记录 **167项 required 全部通过**，本批真实机器、恢复和组件场景均有标记；随后停在 `Saving worlds`。精确终止记录指向 PID32544、创建时间22:56:20、父进程21468与本候选启动参数文件，超过60秒后被停止；子进程退出-1、Gradle退出1。**不得将其写成 GameTest 命令正常退出0，保存停滞原因及修复不在本次判断范围。**
- 普通服重载日志与命令记录相互对应：外部 flint 标签 disable/reload(false) → enable/reload(true且真实机械手消耗并得到step1) → disable/reload(false)。180tick夹具保留120tick真实机器断言；此前100tick超时是夹具调度错误，后续完整三段结果已覆盖。锯关闭配置的独立日志有通过标记。这里证明的是隔离数据包等价成员，不泛化为所有第三方模组兼容性。
- 现场将原60张游戏PNG逐项与开工快照及 `git show 9295e48:<path>` 的二进制内容比较，**60/60一致**；历史51项baseline目录和清单无差异。定点整改前后5项/65项快照分别一致。当前艺术 `verification.json` 明确是 **65→65、66项manifest、62个唯一SVG、PASS**；初次完整 verification 被覆盖，原60图保持结论来自首次快照与独立Git比较，不混称同一次验证。
- ZIP内默认run两份180项CSV字节及SHA-256一致，均为 `EE6C639CEB55512C52B8B12D9850B790CD80CE53BC6B44DE12BA99EF6633136B`。根日志当前字节与ZIP开工before一致，按仓库换行过滤计算的Git blob与HEAD一致；原始工作区CRLF与HEAD LF的直接字节差异不是未恢复。现场只见原Java21468，无本批游戏残留；主工程 `.vscode/launch.json` SHA-256仍为卡中 `65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`。
- 只读 `git diff --check` 未报告空白错误；仅有仓库现有换行转换提示。最新归档 README 明确区分断言通过、非正常退出、最终素材复跑时间点和剩余人工门。

## 未判断事项与人工门

未运行客户端、Gradle或艺术导出验证，因此不新增运行通过声明。仍由用户按 `docs/reviews/2026-10-01/material-04/CLIENT-CHECKLIST.md` 确认切石/锯入口、两条序列准确耗料与产量、JEI和创造页、五项名称与游戏外观、两类实际半成品保存重进，以及玩家操作下缺料/断电/堵塞恢复。辐射路线全部错误组合、任意第三方数据包和整线物流不是本轮自动证据已穷尽的范围。

PM可据本报告完成候选保存与人工交接。**在用户反馈前，不验收整批、不合入main、不推进下一材料或设备批。**

## 实际使用技能

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：核对版本、延迟注册、原生物品/组件与生产配方边界。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：区分原生数据加载、真实机器、组件编码及客户端证据，核查原始XML与GameTest日志。
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：核对1.21.1模型/纹理引用、双语名称和实际离线预览。
- `C:/Users/IKSXH/.codex/skills/minecraft-ci-release/SKILL.md`：核对最终制品身份、构建/测试退出边界和候选发布门；未使用通用示例升级版本、改变许可证或发布方式。
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/verification-before-completion/SKILL.md`：以本次只读字节、ZIP/XML和差异核查支持终审结论；遵守派发限制，不重复运行游戏测试，不把已有运行证据描述为本人重跑。
