# REACTOR-ADMINISTRATOR-04 执行记录

2026-10-14。唯一执行者 `/root/reactor_automation_retrofit_impl`。原一次组合审查及同次差异复核已关闭两项Important，唯一assemble和25制品冻结已完成；待同一审查者制品补核及PM候选登记。下方18/23交审段落为原阶段记录；本记录不作为客户端验收或main合入。

## 范围和基线

实际读取本树AGENTS、04卡、Minecraft开发/测试与systematic-debugging/TDD/verification技能。沿原树 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-automation`、原分支 `codex/reactor-automation-basic`，文档HEAD `ce8ca822ef2db4b09dde737b2430a396a68b62ee`、原功能 `78acd5d`。未进行Git写、转派或治理改动。

原正式包3183722B，SHA-256 `879c3d72bf5a80f9cf93263abe847d8e0354d22499f7312bc1a9e661925506fb`，备份 `build/reports/reactor-administrator-04/baseline-879c.jar`。`baseline-identity.json`捕获2055源文件、27配置文件和38候选世界文件。本批未启动或回写用户世界、未改配置、未终止任何用户进程。18冻结复核这些世界/配置全部载荷一致，正式JAR仍是879c；PM文档dirty和用户日志保持。

## 实际生命周期依据和实现

锁定MC1.21.1、NeoForge21.1.219、Create6.0.10-280、Java21。`minecraft-server-api.txt`记录实际MC字节码：正常runServer退出先调用 `handleServerStopping`（约400），后设置stopped并stopServer；异常分支也设置stopped但不经过前述事件（约630/750）。`server-lifecycle-api.txt`证明该hook发布真实 `ServerStoppingEvent`。stopServer最终saveAllChunks之前还可能触发卸载。故不能以isStopped单独证明正常保存。

利用已有 `ReactorStructureLifecycle` 总线注册，正常停止事件捕获当前非权威意图并setChanged；只有同一个server已经收到正常事件且isStopped时，卸载回调才保留该最终检查点，ServerStopped清临时标记。运行中卸载/异常没有正常资格，仍原撤权。普通autosave写NormalSave=false；正常最终保存才写true。没有把“自动保存文件存在”当正常续接授权。

`ReactorAutomationResume`只保存本版本严格13字段信封：模式、目标、调节年龄、成员坐标和NormalSave；不保存/恢复旧generation、授权epoch、观测租约、菜单nonce、热票据。新仪表生成新身份；原MANUAL逐棒actual/target和CONSTANT调节位置留在原快照/意图。加载交接ARMING无正式运行权，整条棒推进、燃耗、热工和维护结算冻结，使用现行armingTimeoutTicks有限等待，不计算离线墙钟。

当前结构/列图、接口和燃烧室具体实例、原保护、逐棒下一tick冷却及维护预算核验后，才发新反馈许可给原生反相线。**反馈15只用于冻结期间等降沿，不是正式运行权，也不是热票据/KINDLED**。getter预算不查询红石，避免共同导体的真实递归；正式核tick仍独立读取真实输入，持续高/超时保护。真实低沿后首次原棒核事务必须先实际支付并出票，才恢复原模式/年龄和新epoch。零外燃料、零缓存热可从当前新生裂变热付1HU，不要求重投冷启动燃料；付款失败立即保护。

保存FAULT保留原因，保存SCRAM不被初始化低沿释放；运行意图带SCRAM不续接。人工STOP保持STOPPED及安全目标。另以只在read建立的无权inactive-load窗口协调STOP/FAULT首次notScanned缓存与燃料投影：初扫未到暂缓结算，首扫/绑定完成后保持原停止分类；实际扫描失败/列图变化/超时或运行卸载仍保护。此窗口不授START权限、不恢复运行。

原成功GUI命令在 `ReactorAdministratorCommands` 约104无条件notifyPermission，pending反馈15撤销的STOP已通知，不依赖state false→false比较；`Resume.fail`也显式通知临时反馈下降沿。APPLY在待续接期间原子拒绝，原STOP可取消待续接；原滑块依既有ARMING锁定。GUI/协议/原热工算法/冷却转换率/配置/PI均未改。

## 精确功能写集

18源清单/ZIP冻结11路径，其中生产Java7个；其余来源载荷与00基线一致，未删除源。

| 路径（候选根下） | 用途 |
|---|---|
| `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationResume.java`（新） | 本版本检查点分类、有限核验/反馈/首付交接 |
| `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java` | NBT read/write、整机冻结、STOP/FAULT初扫分类、真实首付提交顺序 |
| `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationService.java` | 等待不走冷启动全插；首次绑定保FAULT；正常卸载识别 |
| `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationBlazeBridge.java` | 有限新反馈、等待期不把未出票者当熄火故障 |
| `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationHeatSupply.java` | 只有首次已核验原状态交接可请求新热费，实际出票规则保持 |
| `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationPortBlockEntity.java` | 待续接APPLY拒绝 |
| `src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorStructureLifecycle.java` | 实际正常停服事件、最终dirty/save与卸载协调 |
| `src/main/java/com/iksxh/create_nuclear_industry/gametest/ReactorAdministratorResumeGameTests.java`（新） | 11个独立域真实代表 |
| `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationResumeTest.java`（新） | 6个检查点/等待纯合同 |
| `src/main/resources/data/cni_reactor_administrator_resume/structure/resume_empty.nbt`（新） | 原administrator_empty逐字节测试别名 |
| `build.gradle` | 只新GameTest class与NBT精确正式JAR排除 |

State/Controller/Mixin和原GUI文件未写。正式JAR排除仍待唯一assemble后实核，不提前声称打包无泄漏。

## 自动证据与失败历史

所有日志/XML/脚本仅在 `build/reports/reactor-administrator-04/`，不覆盖失败。业务通过、Gradle退出与自然保存分列。

| 阶段 | 结果与证据范围 |
|---|---|
| 01 `01-nbt-real-red.log` | 旧生产两项真实RED：读档强制STOP/全插、FAULT原因丢失。Java2/Gradle1；三维度自然保存。仅实际NBT/BE重建，非进程重启 |
| 02 `02-checkpoint-red.log` | 新纯合同5/5行为RED，非编译失败；17s exit1，XML保留 |
| 03 `03-resume-directed-green.log` | Instrument的ScramStatus未限定包导致编译失败；如实保留，不计行为RED/GREEN |
| 04 `04-resume-directed-green.log` | 29/29：Resume5、Authority3、Controller18、HeatSupply3；19s exit0、零失败/错误/跳过。此为前一源码阶段，不并入最终总数 |
| 05 `05-nbt-real-green.log` | 原两项真实2/2、20s自然exit0/三维度保存 |
| 06/07 | 人工STOP+保存SCRAM分类纯RED1/1，修后Resume6/6 GREEN18s exit0。各XML保留 |
| 08 `08-expanded-real.log` | 8/10；共同真实石头导体发生StackOverflowError，确认getter→observe→hasNeighborSignal重入；STOP末项夹具未先实际采样SCRAM，单列夹具错误。25s Java2/Gradle1、自然保存 |
| 09 `09-expanded-real-green.log` | getter预算取消红石查询、STOP夹具经原updateRedstoneScram读取真实电平后，10/10；24s exit0/自然保存 |
| 10 `10-final-real.log` | 加真实运行期BE卸载回调、失败反馈通知后11/11；23s exit0/自然保存。随后12读最终NBT发现STOP首扫后变FAULT，不能作为最终STOP持久化通过 |
| 11 | Resume6+Lifecycle2=8/8，16s exit0；前阶段，仅该定向范围 |
| 12 | 只读本轮隔离最终region，发现STOP最终structure_changed FAULT，证据原样保留 |
| 13 `13-stop-first-load-red.log` | 延迟首扫观察后10/11，实际STOP首次scan前tick变sample_lost FAULT；22s Java1/Gradle1、自然保存 |
| 14 `14-stop-first-load-green.log` | STOP/FAULT初扫门修后11/11；23s Gradle/Java0、三维度自然保存。首扫前tick、首扫/绑定后若干tick都实际保持STOP/SCRAM |
| 15 | 只读14本轮真实最终region NBT，健康MANUAL NormalSave1/SCRAM0；STOP最终STOPPED/NormalSave1/SCRAM1；FAULT hot_trip原因保持。`15-final-save-summary.json`为小摘要，大原JSON另存 |
| 16 `16-stable-directed.log` | 最终受影响Resume6+Lifecycle2=8/8，17s exit0/零失败错误跳过。未机械重复原热工/GUI全套 |
| 17 | 最后仅补中文职责/单位注释后compileJava，2s exit0；冻结实际class输出，不是assemble |

14真实11项：手动逐棒差异；恒定功率age9/command65/target2；owner-first与burner-first两顺序各零外燃料/零缓存、实际新生热支出1HU且消费新receipt；当前缺件；缺冷；持高有限超时；共同导体无递归但真实高仍超时；待续接实际ServerPlayer菜单APPLY拒绝/STOP取消且保存STOP/SCRAM；FAULT原原因及START拒绝；实际BE运行卸载回调不能凭单独事件标记冒充正常退出。非管理员原裂变与0维护费在恒功率场景中有实际断言。

15最终STOP坐标 `(2355864,-57,-13957827)`；hot_trip FAULT `(2355885,-57,-13957849)`；本轮完整11仪表位置及源region SHA在15证据。只读结果证明**真实正常停服事件最终保存信封**，不证明独立进程重启。

恒定功率场景在NBT/BE重建当时实际断言age9/command65/target2保持，随后同一位置还测试autosave与未任职，因此其最终region为STOP；没有把该最终STOP文件写成“恒功率最终停服保存”证明。

## 冻结、交审和未跑边界

18冻结：11功能路径、2059全源、当前main输出666class和1551资源（包含测试输出，非正式包载荷数）。

- `18-review-input-manifest.json`：`f0e0cae8655bcaa4ec7a7212c763bb1244b9cf06f5a052e16641fb6b43d2510a`。
- `18-reviewed-source.zip`：57714B／`f28d8ed4a659735ac49f3bdae7f7fb2a01b2f32309bcddc4dcf26ca3d4c6833e`。
- `18-functional.diff`只含功能现有文件差异；新文件在ZIP和manifest中。`18-execution-summary.json`给日志/自然保存分列索引。
- 38用户世界文件、27配置、原879c正式JAR与00载荷完全一致；没有Git写，其他dirty不清理。

当前提交原审查者一次组合规格/质量窄审，经PM接续。不提前打包/改正式JAR；审查后唯一必要assemble由本执行者持有，届时追加最终源/class/资源/JAR一致性、测试class/NBT排除与空目录实际结果。

**未跑**：真正由ChunkManager释放区块并再加载、独立进程重启，以及用户runClient/GPU/原世界人工续接。真实NBT/BE替换、直接原生BE卸载回调、实际ServerStopping最终保存是三种不同证据，未把前两者冒称真正卸载/重启。SPECIAL原计时、事故冷却与03旧热费代表业务未改，复用原证据，不冒称本轮重跑。无需旧版本迁移或扩大兼容研究。

01/01R/03集中人工门仍接续；本批不关闭这些门、不合main、不推进02 PI。本批各真实服务器均有界自然退出，未出现Saving worlds挂起或终止隔离进程；原03历史失败/保存线程和所有包保持。

## 同次组合审查集中整改（23稳定源）

原审查REVIEW汇总两个Important。用户中断后明确继续；本执行者实际重读AGENTS/04整改卡/REVIEW/本报告，核中断时无本批遗留Java进程、HEAD仍ce8ca82、18原源与879c包保持，没有重复初版实现。按receiving-code-review核对原hydrate/列归属和BreakSafetyFuelColumns读写/首扫覆盖链后集中窄修。

1. **合法稀疏燃料**：Instrument在NBT read单独严格校验已有BreakSafetyFuelColumns与合法完整表面/相同尺寸：缺失、错类型、重复坐标、越界或额外键均不授完整证据；原拆除防护的容错读保持。原完整物理位置存为immutable本次read副本，扫描刷新不覆盖它。Resume首次验证后固定会话副本，要求当前全部物理FUEL等于保存完整集合，已有fuel状态仅须属于集合，控制棒完整映射仍强等；原hydrate仍遍历每个真实换料口/已加载BE和合法物品。不补造从未装料的FuelColumnState。初始STOP/FAULT也不能以空核状态绕缺失完整证据。
2. **交接代际**：第一次合法首扫验证后固定当前新generation；后续pending重扫合同/原点改变先fail、全插、撤反馈，再改变代际。预算和首付完成均核同一个当前owner对象、固定generation、当前candidate/绑定完整身份及接口/燃烧室具体实例；同坐标对象存在不代替身份核验。初次合理新generation允许建立，等待期真实变化不能旧bind付费再被普通Service全插。

生产整改仅原Resume/Instrument两路径，其他04生产入口不改。按PM已入卡的单路径扩卡，在原GameTests新增ReviewWindows嵌套holder，域`cni_reactor_administrator_resume_review`/目录`build/reports/reactor-administrator-04/review-game`；仅新增四个真实代表。`data/cni_reactor_administrator_resume_review/structure/review_empty.nbt`逐字节复制既有resume_empty，build.gradle只加该NBT排除，原GameTest class通配已覆盖嵌套类。相对18共六功能路径变化：两生产、原GameTest、原unit、build.gradle和这一新增NBT；整个04写集现在12路径。没有新增框架或别名，原11场景未修改业务断言或重跑，仅setup(h)委托默认false保持原一FUEL夹具。

| 阶段 | 实际结果 |
|---|---|
| 19 `19-review-windows-red.log` | 四项均为行为RED：稀疏运行/STOP被误拒、原未装料整列删除被误认可、反馈窗口额外冷端代际变化未撤反馈；29s Java4/Gradle1，三维度自然保存。首次隔离目录缺server.properties告警由原测试服务正常生成，不是断言失败原因 |
| 20 `20-review-windows-green.log` | 四项4/4 GREEN，25s自然exit0/三维度保存。稀疏STOP实际断言通过后，同一位置串行替换为missing/duplicate/wrong_type/outside四种坏证据，均FAULT、无许可/receipt。该轮末尾phase=FAULT打印的是后续坏NBT循环已移除的旧对象，不是合法STOP保持失败 |
| 21 `21-review-directed.log`／`21-unit-xml` | Resume8＋Lifecycle2=10/10，22s exit0，0失败/错误/跳过；新增两纯合同核稀疏subset与完整物理布局、非法核状态归属/缺控制映射拒绝。原16八项不与此相加 |
| 22 `22-stable-review-windows.log` | 最后取消inactive空核状态绕过并明确日志前置阶段后，仅同四代表4/4，21s自然exit0/三维度保存。日志明确合法稀疏acceptedPhaseBeforeCorruptChecks=MANUAL与STOPPED；真实未装料整列删除仍FAULT/首费0，额外冷端使真实generation改变、反馈0、全插且后续低沿仍FAULT |

23纠错冻结不覆盖18：12源路径、2060全源、667main class输出/1552资源输出（含开发测试，尚非正式包载荷数）。

- `23-review-input-manifest.json`：`16c91fc8b516f766ee0d4e36eeaef8acba5fc01b91bca935ffe92f6f29941fe3`。
- `23-reviewed-source.zip`：61097B／`fc62c3e071e75a3af20055aa2927fedba405adadb01c9aa01bdbdaf479a58d08`。
- `23-functional.diff`、`23-correction-summary.log`与`23-execution-summary.json`给净源码和阶段证据；18初审源/所有失败/原包保持。
- 世界38文件、配置27文件和正式879c包再次零载荷漂移，未Git写、未启动用户世界/配置、未终止用户进程。

当前纠错源稳定，交**同一原审查者差异窄复核**，尚未assemble。原11/最终保存15证据在未改范围复用，不冒称纠错后新15项整套运行。稀疏STOP场景末尾是故意坏证据测试，因此本轮最终region不用于合法STOP最终停服保持证明；原15正常保存证据保留。真正ChunkManager卸载、独立进程restart、GPU/用户世界人工门仍未跑，未扩大旧版兼容专项。复核无剩余必改后才唯一增量assemble及正式包测试排除/来源冻结。

## 24唯一assemble与25最终制品冻结

原审查者在同一REVIEW差异窄复核关闭两项Important、无新必改；PM明确派发本批唯一增量assemble。实际命令：

```powershell
.\gradlew.bat assemble --console=plain
```

`24-final-assemble.log`：**BUILD SUCCESSFUL in 1s，exit0**；4 actionable tasks中仅jar执行、其余3 up-to-date，没有clean、全量build、测试或用户世界运行。源码未继续修改、未Git写；没有另起审查。

正式候选与冻结副本：

- `build/libs/create_nuclear_industry-0.1.0.jar`，3199564B，SHA-256 **`dc82b20a19f17e81db2fd4e9efa69ed2b861fd703deaca4553ac7dfcbd6a7b05`**。
- `build/reports/reactor-administrator-04/25-final-candidate.jar`，逐字节相同；879c基线/18与23源ZIP、所有失败和保存证据保持。
- `25-final-artifact-manifest.json`，1401941B，SHA-256 **`17ee65b0a6c1f8074e92435f7ed8c74263d0fe15d432c59c6a854570c386458d`**。
- `25-artifact-verification-summary.json`／`25-artifact-verification.log`给载荷、实际排除、空目录及旧包差异；执行脚本为`freeze-final-artifact.py`。

实核当前12功能来源/2060全源与23清单0漂移；实际main输出667class/1552资源与23一致。按build.gradle实际jar排除之后，正式包为**661class＋1546非class资源＋1个Gradle生成MANIFEST，共2208非目录文件**。1546的口径为全部非class文件且排除MANIFEST；不是把1552开发资源全计生产，不沿用之前其他报告的资源子集计数。每个正式class/资源载荷与相应当前输出逐字节一致，输出外唯一生成条目为`META-INF/MANIFEST.MF`。

实际排除六class、六NBT，含本批`ReactorAdministratorResumeGameTests.class`与`$ReviewWindows.class`，以及resume_empty/review_empty两个精确路径；**两域没有测试class或NBT文件载荷泄漏**。两测试域仍有四个空ZIP目录占位（各namespace与structure），单列于25清单，不写成“没有新增ZIP项”。

相对879c，非目录文件只新增Resume/Loaded/Session三个class、修改九个来自原六生产路径的class（含编译嵌套class），没有删除文件或任何既有资源文件变化。详细名单在25清单；目录占位独立统计，不与文件载荷混合。

25再次实核38用户世界文件和27配置载荷不变；没有世界/配置写入或用户进程终止。现交PM准确制品身份，由同一原审查者仅在原REVIEW补核包，不新增测试/构建。PM净提交源、批准候选JAR和文档前仍不合main；01/01R/03及04客户端门、未跑真实区块释放/独立进程restart/GPU边界保持，不推进02 PI。
