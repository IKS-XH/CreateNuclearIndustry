# 材料03 / 素材05：粉末制钢候选

**状态：候选实现与本批专项已通过独立审查，待客户端人工验收；完整GameTest回归仍未完成。未合入main，不推进后续零件或设备。** 开工基线为 `9097338`，实现提交为 `a1353dd16b4b1447349c6639d37e12d94fdc391b`，任务合同见[实施卡](../../../superpowers/plans/2026-10-01-ext-a-material-03.md)。后续文档同步不改变本次代码及制品。

本批加入铁粉、煤粉、木炭粉、合金钢粉、合金钢锭，以及八条原生配方：三条1原料→1粉、两条无热4铁粉+1碳粉→5钢粉、熔炉/高炉各1粉→1锭、1锭压1原有钢板。制粉/混粉的Create加工参数均100，压片沿原生速度；炉子200/100 tick、经验0.1，各步无副产物。原钢板、维修逻辑及旧55张游戏图保持。

## 证据与限制

| 范围 | 本轮结果 |
| :--- | :--- |
| 最终单元测试及构建 | `test build --rerun-tasks --max-workers=1`退出0；52套件265项，失败/错误/跳过均0 |
| 本批14项专项 | 隔离普通服务器逐项执行，真实粉碎轮、两碳源搅拌、错误输入/欠料/断电/堵塞恢复、炉/高炉/单件熔岩风扇、压板及加载契约通过；成功标记位于所有断言和`helper.succeed()`之后 |
| 外部标签重载 | 运行中启用测试包并实际`/reload`，flint作为等价钢粉由真实炉子产1钢锭；停用并实际`/reload`恢复不匹配，false→true→false。夹具文件保留在隔离报告目录且处于停用状态，不加入默认客户端或生产标签 |
| 完整155项GameTest | GREEN及独立目录重试均在`GameTestInfo.tickInternal`/fastutil iterator异常中断；没有155项完成证据。未找到本批回调嵌套调度的证据，根因仍未定位；新14项通过不能替代这项回归 |
| 五项SVG素材 | 16×16 RGBA，严格整数矩形子集；导出、安装重复及负例保护通过；55旧图哈希不变，最终60游戏图。PM与独立审查实际查看离线预览，客户端视觉待验收 |
| 最终制品与隔离 | JAR的264项assets/data与当前源码逐字节一致，含60张PNG；默认run的179文件路径/大小/哈希/修改时刻前后无差异；普通服正常保存并退出0 |
| 人工测试 | 尚未执行；按[客户端清单](../material-03-client.md)检查JEI、玩家操作、五项外观、原钢板身份及保存重进 |

此前普通服没有在线玩家，内置成功报告不输出日志，因此前两轮命令提交不作为完成证明。最终版本新增明确成功标记后重新构建并运行；独立审查确认断言失败会抛出，无法继续输出成功。RED、两次框架失败及最终普通服成功分别保留，不改写为一次全量成功。

## 报告与运行入口

- [功能交付](./EXT-A-MATERIAL-03.md)、[整批独立审查](./EXT-A-MATERIAL-03-REVIEW.md)。
- [制品及结果核对](./artifact-verification.json)、[原始证据包](./evidence.zip)：含52份JUnit XML、初次/最终构建、RED和两次框架失败、最终普通服日志与命令、隔离配置、标签夹具、默认run清单；不包含世界、缓存或JAR。
- [素材交付](./EXT-ART-05.md)、[素材独立审查](./EXT-ART-05-REVIEW.md)、[最终预览](./steelmaking-preview.png)、[离线验证](./art-verification.json)、[开工55图保持](./art-opening-55-preservation.json)。
- [更正后的技术预检](./EXT-A-MATERIAL-03-REVISION-PRECHECK.md)。旧`EXT-A-MATERIAL-03-PRECHECK.md`只保留历史旧路线，不作为新合同。

候选位于`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支`codex/ore-acquisition`。运行`gradlew.bat runClient`必须切入该候选目录；主工程仍是已验收的铅锡版本。存档互相独立，本轮不迁移存档。按用户“遇人工测试暂停”的授权边界，在收到客户端结果之前停止自动推进；本记录也不豁免尚未完成的完整回归。

最终JAR SHA-256为`D23732ABBFF58C2742671BF7A57A541D1EC930DEAB791815EF14FF10B895B33B`；证据包为`0B73E3CC04A845BF080B24F0350B97A7D0E408C15FFBC264F16D92B97B83CCD1`。PowerShell transcript未捕获完整子进程输出，证据中的`normal-server-final-latest.log`是完整服务器日志，`normal-server-final-commands.txt`是实际命令整理，不能把后者称为原始PTY转录。

PM已保留运行生成日志副本，仅恢复候选`logs/debug.log`与`logs/latest.log`的开工内容。主工程既有`.vscode/launch.json`保持SHA-256 `65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`，不纳入提交。

独立审查的R1要求补足失败阶段两种原料的保留断言，执行者已修正并在最终真实机器测试中通过；没有未关闭的代码整改项。实现仅提交候选，主工程仅登记需求、报告与人工清单；客户端和完整回归门均保留。

PM实际使用modding/testing进行合同、机器与证据审核，使用ci-release与完成前验证流程区分候选、main和制品；没有发布、推送或改变技术栈。
