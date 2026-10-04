# 锅炉01C：每端口独立限流

**最终状态：** 2026-10-04用户确认后续01D与翻译修复全部手测通过，包含本卡独立限流，已合入main。现行人工验收见[记录](../boiler-01d/ACCEPTANCE.md)，下文保留01C交付时点证据。

**后续修订：** 用户随后确认[锅炉01D](../../../superpowers/plans/2026-10-04-ext-b-boiler-01d.md)保留给水第2层/汽口第4层，放开各侧三个非棱边格。下文为01C交付时合同与证据；端口放置及人工复测以01D为准，原始测试结果保留。

状态：实现、定向自动验证和[独立审查](./EXT-B-BOILER-01C-REVIEW.md)通过，等待本轮客户端复测。功能基线`d10e1af`，沿用同级候选`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。

## 当前放置与流量

| 部件 | 合法位置 | 数量 | 每口上限 |
| :--- | :--- | :--- | :--- |
| 给水口 | 从底部数第2层，各侧中央，排除控制器 | 1～3 | 256mB/t |
| 蒸汽口 | 从底部数第4层，各侧中央 | 1～4 | 256mB/t |

所有口朝外；每个侧面仍最多一个给水口、一个汽口，同面并排增加端口会使结构不成型。本轮不扩展槽位。取消整炉256mB/t总额度，水/汽各16000mB库存仍共享。同一口的多次调用及主动/被动输出合计不能超过其额度，不同端口分别计量。实际产汽仍由供热与水量决定，九换热段最大162mB/t，不因增加端口而增产。

用户报告“加上第二个端口后不成型”；合法多面端口在自动场景中可成型，但测试直接设置正确位置和朝向，未复现用户当时的实际放置操作，不把独立限流改动宣称为已修复该现场结构问题。

## 自动证据

- 锅炉JUnit12/12：[XML](./TEST-com.iksxh.create_nuclear_industry.boiler.BoilerStateTest.xml)、[日志](./EXT-B-BOILER-01C/test-assemble-rerun1.log)。之后仅改GameTest，未重复JUnit。
- 最终锅炉GameTest17/17：[日志](./EXT-B-BOILER-01C/gametest-final-verified.log)，测试服正常保存退出。含同tick两口独立额度、同口主动被动预算、共享库存、真实多口管线及拆接恢复。
- 最终增量assemble成功：[日志](./EXT-B-BOILER-01C/assemble-final-verified.log)。JAR SHA-256：`DA68FEE6ED92B53691A53C860DCD045288FDC84D3B715BD3A4AACA2CC7EF1A18`。
- [执行报告](./EXT-B-BOILER-01C.md)及原始日志一并归档。三次GameTest失败来自对物理连接对象存在性的错误断言；一次初步17/17之后又补严拆壳恢复的时间基线、有效句柄及8000＋2000mB总守恒，最终证据仅认`verified`日志。PM与审查者未重复跑测试，未跑全P1或换热器全套。

主目录启动配置保持原SHA-256 `65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`；用户日志、缓存和存档未纳入提交。功能写集仅锅炉账本、控制器、压力适配和对应测试，未改结构槽位、共享Create兼容层或配方。

## 只需本轮两项手测

关闭旧客户端，从候选目录运行：

```powershell
Set-Location E:\MyMC\NewMod\Create_NuclearIndustry-ore-acquisition
.\gradlew.bat runClient
```

1. 在不同侧面的上述合法位置放两个给水口、两个汽口，正面均朝外，扳手检查成型。分别接给水泵路和无泵蒸汽管到储罐，确认各口能够使用。观察独立流量上限时需要充足库存及接收空间；实时产量不足512mB/t不能当作两个汽口限流失败。
2. 拆一个汽口并补回外壳，再换回汽口；随后短暂拆壳并重建。停输、旧连接失效、恢复出汽及保存重进后的库存应正常。原外观、强化钢板配方及换热器直列沿用[01B/01D已通过的手测](../boiler-01b/MANUAL-ACCEPTANCE.md)。

本轮人工门通过前不合入main、不启动汽轮机。任务合同见[01C](../../../superpowers/plans/2026-10-04-ext-b-boiler-01c.md)。
