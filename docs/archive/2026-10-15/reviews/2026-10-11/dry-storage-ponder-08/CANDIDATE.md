# 干式贮存架思索08：单页播放候选

**现行状态：** 用户2026-10-11报告“存储架的思索也测试通过了”，已按[验收记录](ACCEPTANCE.md)关闭单页播放门并净合main `d2b9345`；主目录JAR已更新为本页冻结制品。以下候选身份、待播放说明及存档复制记录保留原时点，不重新开启人工门。

2026-10-11。**实现、定向自动检查与一次独立窄审通过；用户播放待验收。** 本页只介绍现有设备用法，不讲具体配方。其他七类已验收教学和正式贮存功能保持。

## 候选身份

- 工作目录：`E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`。
- 分支：`codex/art-integration`；净教学提交 `f87185a52093a3dbf7c847b9fe27be32c4adda3f`，实际实现基线 `96fde12fe54b65112f1790ede812cd1a797c27fd`。
- 精确七交付路径及源SHA见[实现报告](IMPLEMENTATION.md)，唯一页面 `dry_storage_usage`。源码还未合main。
- JAR：该候选目录 `build/libs/create_nuclear_industry-0.1.0.jar`，2514642字节，SHA256 `35c4befd197051338c7840c8f4e80d66e4462d1090de2f7ab1a27b6853b17da7`。
- 冻结源/class/JAR、模板回读、最终XML及原命令位于候选目录 `build/reports/extension/DEVICE-PONDER-08-STORAGE/`，以 `04-manifest.json` 和 `04-frozen/` 为准。

最终定向 `DryStoragePonderContractTest` 4项及 `P1Ponder01ContractTest` 6项共10/10，失败/错误/跳过均0；增量assemble退出0、17秒。首轮70°镜头被实际原生变换核对发现不朝向正面，修正后才复跑同范围检查并绑定最终制品，旧中间候选保留。没有全量、GameTest、客户端或旧存档验证。[独立窄审](REVIEW.md)规格/质量均通过，无必改项；PM已核最终日志/原始XML、七源SHA、实际JAR SHA与Git范围，不重复构建或保护矩阵。

## 本次播放

在PowerShell中运行：

```powershell
Set-Location E:\MyMC\NewMod\Create_NuclearIndustry-art-integration
.\gradlew.bat runClient
```

进入候选客户端，将鼠标悬停在干式贮存架物品上按住W，播放“存取与自动化”这一页。只需查看：

1. 手持封装桶右击存入、空手右击取出和背包满保留的说明清楚。
2. 东侧输入及底部向南输出可区分；贮存架、桶图标和路径都可见。
3. 默认16桶的占用增加、满架上游等待、腾位继续输入能够理解。
4. 全页只有一个场景；正文不相互遮挡，机体/漏斗/正文不被底部播放条遮挡。

此候选使用自己的运行目录和世界。主目录仍提供此前已验收的JAR；本页尚未整合，不用在主目录找新入口。用户播放通过后再做净源码整合，不重复原正式贮存功能或旧教学手测；此门不覆盖独立美术视觉或发布验收。

用户随后要求以后新分支一起携带存档，当前候选已补复制主目录`run/saves/新的世界`：27文件逐一SHA256一致，来源保持；候选原同名世界的25文件完整备份在候选`run/save-backups/2026-10-11-before-main-copy/新的世界`。复制时客户端未运行，没有合并世界文件、改存档内容或重跑教学验证；清单位于候选`build/reports/maintenance/2026-10-11-world-copy/manifest.json`。

## 工作区与接续

候选提交只包含七交付路径与两份冻结报告，原日志及pycache保留；主目录仅登记计划、报告与候选状态，没有提前集成新教学源码。已停在本页播放门，其他工程主线继续暂缓。
