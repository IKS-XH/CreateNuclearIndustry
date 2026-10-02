# 燃料02A/02B最终验收

**结论：2026-10-03用户确认02B两项复测通过，结合此前02A四组手测通过，生芯块与燃料烧结炉已验收合入main。** 主目录由`241af88`快进至候选功能及交付基线`129d5c5`，含02A实现`575400e`和02B调整`b1537c8`；本文件随收尾文档提交，不改变实现。

## 已确认范围

- 1低浓缩铀粉压成1生燃料芯块；专用炉在底部完整烈焰人燃烧室普通加热下，每400有效服务端tick烧成1烧结芯块。无GUI、无转轴，顶部进料、四侧出料，输入/输出各64件，断热与满输出暂停；保存、自动化及携带拆放按02A合同验收。
- 放置模型保留八棱外形，宽/高/深外包范围占满一格；端盖、冷热状态、物品栏与手持外观通过02B复测。
- 工作台3×3有序合成`RSR/RBR/SIS`：4耐火砖、3钢板、中央1原版高炉、1工业传感器，产1台；JEI显示与新制造方式通过复测。旧燃烧室制造配方已移除，运行时底部燃烧室仍需保留。

人工证据为用户对[02A四组清单](../../2026-10-02/fuel-02a/CLIENT-CHECKLIST.md)及[02B两项清单](./README.md)的通过确认；未补写用户未提供的逐步耗时、截图或数量记录。

## 验证与集成

按治理5.1复用02A的4/4定向JUnit、1/1隔离GameTest、独立审查，以及02B资源检查、候选增量构建和限定复审。原始范围及限制保留在[02A交付](../../2026-10-02/fuel-02a/README.md)与[02B交付](./README.md)；本次未重跑这些测试。

主目录与候选的`src`、构建配置及美术工具Git差异为空，快进无冲突。主目录仅执行一次增量`assemble`，9秒成功、退出0，Java编译使用缓存。见[构建日志](./acceptance/assemble-main.log)、[退出码](./acceptance/assemble-main-exit.txt)及[制品比较](./acceptance/artifact-main-check.json)。

主目录JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，SHA-256为`830d174b1a6a8c63d26e902e68d40c6a7440b8ffdc2f6a45a9e3b0af0cdc3a4d`。它与候选JAR的608个文件条目一致：365项字节相同，243项文本仅CRLF/LF不同；没有缺失、多余或其他内容差异，不声称整个JAR逐字节相同。

用户已有`.vscode/launch.json`改动保留，合入前后SHA-256均为`65ebb9ecb32c45f3254e2f511d3d134b17829e2ff0d73b7cb8094583ede18c07`，未纳入本次提交。未启动或终止用户客户端、未操作run存档、未推送或发布。

技能沿用本批实际应用的`minecraft-modding`、`minecraft-testing`及`minecraft-resource-pack`；PM收尾读取并应用`minecraft-ci-release`的版本与验收规则，保留现有技术栈和发布方式。未修改功能、测试或构建代码。

## 启动与后续

完整退出旧客户端后从主目录启动：

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry'
.\gradlew.bat runClient
```

当前生产线已到烧结燃料芯块。下一段为包壳、焊料、格架及燃料棒→燃料组件两级装配；具体配比、设备分工与交互尚需新卡确认，未计为完成或自动派发实现。
