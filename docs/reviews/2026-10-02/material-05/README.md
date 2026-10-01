# 材料05与素材07：石英粉、耐火砖和重型轴承

**状态：候选自动检查及独立审查通过，等待客户端人工验收。** 用户于2026-10-02确认本批参数；功能与4项SVG在同级候选`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition` / `codex/ore-acquisition`实施，派发基线`53f4ebd`。主工程功能仍为材料04，人工通过前不合并材料05。已按用户授权暂停自动推进。

## 本批内容

- 1下界石英经磨石产1石英粉，粉碎轮使用Create原生磨石回退；加工参数100、无副产物。
- 1原版砖块、1黏土球和1石英粉，经普通加热的工作盆动力搅拌产4耐火砖；参数100。无热和阴燃不满足，超级加热可用；耐火砖此阶段是材料物品。
- 1坚固板作为基底，机械手依次加入1钢锭、1精密构件，最后压片产1重型轴承；1轮、100%成功、无热和副产物。半成品沿Create原生单件及进度组件。
- 四项新SVG及双语模型资源，旧65张游戏PNG（含8项冷却剂）保持。设备、燃料和发电运行合同不属于本批。

配方细节以[批准方案](../../../superpowers/plans/2026-10-02-mainline-material-05-proposal.md)和[任务卡](../../../superpowers/plans/2026-10-02-ext-a-material-05.md)为准。参数100不是固定5秒；新增轴承必成不改变精密构件自身的概率路线。

## 自动证据与审查边界

| 检查 | 最终候选结果 |
| :--- | :--- |
| 构建与JUnit | 整改后`build --rerun-tasks --max-workers=1`退出0，实际执行`:test`；52套件265项，失败/错误/跳过均0 |
| 完整GameTest | 整改后180项required断言全过，本批13项覆盖实际制粉/加热/装配及边界 |
| 中断覆盖整改 | 首审发现旧用例只验证开工前无热/无动力；现分别观察搅拌进度16→15后断热、断动力，再恢复，各恰好一批4砖；不强加精确进度冻结 |
| GameTest退出 | 180项汇总后停在`Saving worlds`，超过60秒核对后仅结束本轮PID37540；子进程-1、Gradle1，不能称正常退出成功 |
| 制品与语言 | 292源assets/data与292 JAR条目逐字节一致，69游戏PNG，四项中英文共8名称正确；PM运行既有工具独立复核PASS |
| 工作区保护 | 默认run实际182文件的路径、长度、UTC时间和哈希完全一致；仅保留原3个Java服务，无本批游戏残留；PM核对前后快照后仅恢复本批两个根跟踪日志 |

整改前`final-*`文件的179项结果和旧JAR哈希保留为历史证据，最终以`fix1-*`及最新JUnit XML为准。保存退出停滞是既有环境限制，与断言和真实普通服结果分开记录。编译仍有既存API弃用及Gradle弃用提示，本轮不升级技术栈。

隔离普通服完成外部等价石英粉标签停用→启用→停用的实际reload，匹配false→true→false；启用阶段真实加热搅拌消耗替代材料并产4砖。普通服正常stop/Gradle0。临时包仅在本批报告隔离世界中，未进入正式资源或默认客户端。

素材离线验证为70清单、66SVG、69游戏PNG；65旧图、51项历史基线和8冷却剂原图保持。首轮工具新增的字节不等拒绝限制会阻止合法SVG更新，PM发现后退回；现已通过可恢复像素修改的RED→GREEN→还原和独立定点复审。完整验证仍PASS，四图已恢复交付哈希。耐火砖倒角可进一步方正的Minor意见保留供客户端视觉判断，不作为未批准的重绘任务。

整批终审另发现验证器依赖未跟踪build快照，已限定改读本页`art/`中的固定65图JSON；保留逐项PNG哈希保护，不锁定JSON换行排版。临时build快照缺席时，旧脚本RED为预期FileNotFoundError，最终脚本完整验证GREEN/PASS，随后原样恢复临时快照。素材与JAR字节不变，[PM工具修复后复核](./pm-post-tool-fix-verification.json)仍PASS；同一终审者定点复审确认问题解决，可进入客户端候选门。

## 归档导航

- [功能独立审查及整改复审](./EXT-A-MATERIAL-05-REVIEW.md)、[整批终审及最终限定复审](./EXT-A-MATERIAL-05-FINAL-REVIEW.md)：无开放阻断问题，保留各轮历史结论及后续修复，不据此宣布人工通过。
- [功能执行报告](./EXT-A-MATERIAL-05.md)、[功能精简证据包](./functional-evidence.zip)、[包索引](./evidence-index.txt)及[条目哈希](./evidence-manifest.csv)。包内111项、196916字节，包含原始JUnit XML、完整RED/探针/整改前后日志、真实reload、隔离init、默认run与PID/根日志前后快照；不含世界、缓存或JAR。原索引中的`evidence-small.zip`归档名为`functional-evidence.zip`，字节相同。
- [PM最终制品复核](./pm-artifact-verification.json)、[现成只读核对工具](./verify-artifact.ps1)。工具的`-ProjectRoot`应指向候选；跨机器复核前需重建JAR并把精简包恢复到`build/reports/extension/`，其中包含工具所需旧图快照。
- [素材执行报告](./EXT-ART-07.md)、[独立审查及定点复审](./EXT-ART-07-REVIEW.md)。报告保留原交付路径语境；其中`EXT-ART-07/`下的归档文件映射到本目录`art/`。
- [完整素材验证](./art/verification.json)、[命令记录](./art/commands.json)、[65旧图快照](./art/preexisting-game-png-sha256.json)。
- [合法编辑RED](./art/update-regression-red.json)、[修复GREEN及还原](./art/update-regression.json)、[限定修复差异](./art/art-fix1-review.diff)。
- [build快照缺失RED](./art/snapshot-resolution-red.json)、[归档输入GREEN](./art/snapshot-resolution-green.json)、[验证器最终修复差异](./art/final-fix-review.diff)。
- 四图原尺寸及明暗底预览：[石英粉](./art/quartz_dust-comparison.png)、[耐火砖](./art/refractory_brick-comparison.png)、[重型轴承](./art/heavy_bearing-comparison.png)、[半成品](./art/incomplete_heavy_bearing-comparison.png)。
- [客户端完整验收清单](./CLIENT-CHECKLIST.md)：现可从同级候选启动；只测试部分时明确未测项。

最终JAR SHA-256：`8D5BB2559370A47CB5DD9AE087DB20826E5BCB3C9FA54727DB725B54D37DB6E4`。精简包SHA-256：`C3A11ACFC573A72F165FBD6EAB6DB334E2180B766A42CBC9658081470D88CEE1`。主工程原`.vscode/launch.json`保持`65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`，不纳入本批提交。

## 人工门

此处尚未记录任何材料05人工通过。交接时核对磨石/粉碎轮、加热与耗料、轴承装配、中断恢复、半成品保存重进、JEI/双语名称及4项外观。自动组件序列化、离线PNG检查和服务端GameTest分别保留证据，不能代替客户端结果。
