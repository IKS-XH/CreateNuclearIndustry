# EXT-B-BOILER-REWORK-01～01F：锅炉重构联合验收

**验收日期：2026-10-08。** 用户在01F候选交付及精简清单之后明确回复“手动测试通过了”。PM据此关闭本轮现行锅炉集中人工门；不将确认扩大为未列场景、所有动力生命周期或首发可发布。

## 现行结果

- 锅炉采用配置范围内可变长宽高，内置核换热器与完整再加热隔层，水/汽容量分别按区容积计算，热力回路、温压、保压及安全阀沿用已批准合同。
- 蒸汽与超临界蒸汽分别保存真实mB/HU，共用唯一汽区容量和炉压。新批次按实际温压归类，已有库存不自动换种，各汽口只取对应库存，不降级、不转换外部异种流体。
- 本轮用户确认覆盖精简清单中的双库存/共享容量、原现场60→10后分别接管输出、逐口选项与冷液持续，以及当前版本保存恢复。未变搭建、外观、材料与汽轮机功能不要求重复全测；旧版存档迁移不在范围。

## Git整合与证据

主目录基线`2060820`，来源为最终候选`e721797`，01F功能快照`fcb4b6d`。PM无冲突合入main，提交`611d3ca7ad0ba29475dd9a0a3fd4dfb3a83a892a`。本次净变更65个源码/资源/美术源路径；合并暂存树与候选逐路径一致，构建文件与依赖未变。原两台已验收思索、主目录独有README及.vscode启动设置保留。PM未手写或修补功能、测试、构建及模拟器代码。

自动证据复用[01F实施](../../2026-10-07/boiler-rework-01/steam-inventory-01f-implementation.md)和[独立审查](../../2026-10-07/boiler-rework-01/steam-inventory-01f-review.md)：25项账本检查、07的十项通过加08单项通过，共11个不同真实用例。07整轮exit1与全部早期编译/夹具失败保留，不改写成单轮全绿；前序锅炉/停转定向证据各自按原报告保留。本轮没有重跑JUnit、GameTest或全量build。

主目录仅新增一次增量`assemble --console=plain`，退出码0、8秒；24项指定class/语言/NBT封包与编译输出或资源一致。详见[执行报告](./integration.md)、[构建与退出码](../../../../build/reports/extension/EXT-B-BOILER-REWORK-ACCEPTANCE-01/assemble-result.txt)、[24项核对](../../../../build/reports/extension/EXT-B-BOILER-REWORK-ACCEPTANCE-01/artifact-validation.json)。实际使用minecraft-modding/testing核对平台与证据边界，minecraft-ci-release与finishing-a-development-branch核对版本/整合；治理5.1覆盖通用重复全量要求，mod0.1.0及依赖不升级，没有推送、标签或公开发布。

## 主目录启动与制品

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry'
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
.\gradlew.bat runClient
```

[本轮主目录JAR](../../../../build/reports/extension/EXT-B-BOILER-REWORK-ACCEPTANCE-01/create_nuclear_industry-0.1.0-main-acceptance.jar)：2,271,628字节，SHA-256 `DDF93DAEF196C9669CB8A1E059B6D2C4F4A57C7175DB72E2B088BA4E454613E1`。原01F/01E历史制品不覆盖。同级候选工作树、原测试世界及运行配置保留，两目录仍各自使用原运行目录，不搬移或改写用户存档。

本轮收尾后停止自动推进；其余主线及下一台思索另行安排。事故、辐射、工作盆加热、辅助热和耐压系统没有因本次验收获得新实现授权。