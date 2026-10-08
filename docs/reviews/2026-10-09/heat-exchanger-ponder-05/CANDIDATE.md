# 核换热器四情景思索：播放候选

**状态：候选已提交，等待本台四幕播放验收。** 实现提交`8c8f0a5`，位于`codex/ore-acquisition`；首轮与R1缺口已按[任务卡R1/R2](../../../superpowers/plans/2026-10-09-device-ponder-05-heat-exchanger.md)修复，定向合同3/3、增量打包和独立静态复审通过。未实际播放客户端，不把自动证据记为视觉验收。

已通过的工作盆、烧结炉供热与汽轮机思索分别净整合main（`857f38e`、`f659f7e`），无需重测。新教学只在同级逻辑候选交付，未合main，不在美术工作树验收。

## 启动与本轮范围

```powershell
Set-Location E:\MyMC\NewMod\Create_NuclearIndustry-ore-acquisition
.\gradlew.bat runClient
```

对核换热器按W，依次播放四幕：

1. **独立供热：** 锅炉和直列完整可见；橙色后入热液、蓝色前出冷液，管路去向与高亮能看清。
2. **锅炉内置：** 剖面看清底部换热器、完整再加热隔层和两层冷热口；讲解后壳体恢复。
3. **加工供热：** 盆换烧结炉无错误方块或崩溃；空设备持续换液、芯块烧结及停热/恢复画面能看清。
4. **蒸汽冷凝：** 进蒸汽、出水方向清楚；固态冷源融水、水源蒸发和蓝冰持续冷凝阶段可辨。

四幕正文一次一段，镜头/高亮/顶部设备不被标题、按钮或进度条遮挡，能够播完。本轮只需播放新教学，不重复生产设备、配方、存档和已验收教学全套。

## 证据

| 项目 | 最终证据 |
| :--- | :--- |
| 实现提交 | `8c8f0a5`，仅本批教学源码、四模板、生成器、双语和合同测试及报告；不改正式设备/美术 |
| 模板 | R1b四份NBT生成、布局及gzip往返通过，`generator-r1b.exit=0`；R2未改模板/生成器，复用该证据 |
| 定向测试 | `test-r2.exit=0`；保存的`test-r2.xml`为3 tests、0 failures、0 errors、0 skipped |
| 增量打包 | `assemble-r2.exit=0`，成功；未运行GameTest、全量或旧存档测试 |
| 独立审查 | [R2定向复审](./REVIEW.md)通过，确认冷源可见集合、锅炉完整隔层、Create组罐API、四幕接口与文案 |
| 制品 | `build/libs/create_nuclear_industry-0.1.0.jar`，2326858字节；SHA-256 `24B30A7E9B8485FBB3BEF46A70B93BBD7D78E4B902D89D0DA0764E0946D2EC47` |

原始证据保留在候选`build/reports/extension/DEVICE-PONDER-05-EXCHANGER/`；首轮、首次R1失败XML及日志、R1b/R2结果和历史制品哈希均保留。详见[实施](./IMPLEMENTATION.md)与[审查](./REVIEW.md)，失败经过不由最终通过覆盖。PM只规范报告Markdown空白并管理提交，没有修改功能/测试代码或重复执行Gradle。

尚待确认：实际镜头与文字不裁切、不重叠，顶部冷源始终可见、盆/炉替换无崩溃，四幕可以完整播完。用户确认后再净整合main；当前保存进度并停在本台播放门。
