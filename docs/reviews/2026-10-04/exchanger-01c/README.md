# 换热器01C：闭环守恒与低流量供热

**状态：2026-10-04用户确认手动测试通过，已合入main，见[最终验收](./ACCEPTANCE.md)。** 下文保留本批候选`c002fb5`的交付与清单；候选位于`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，本批起点`3ea9e89`。主目录现在也包含运行代码。

## 原因与改动

1. 用户确认统计全部设备后总冷/热液仍持续下降。真实Create泵管复现证明，同一反应堆三个冷端共用容量，却被管网模拟重复计算：总13000mB在接满时变12928mB，损失72mB；单端对照保持13000mB。
2. 修复只收窄一次Create分流模拟中本模组共享接收口的承诺量，分别识别共享库存和物理端口预算；真实填充仍由原handler执行，逐口128mB/t不变。同步覆盖换热器多个面共用热罐，普通Create目标原样透传。锁定现有Create6.0.10-280及已有MixinExtras，不升级依赖。
3. 用户已批准[自适应供热](../../../archive/2026-10-08-completed-plans/2026-10-04-heat-exchanger-adaptive-output-proposal.md)：峰值18级、720HU上限与0.5HU/mB不变，储热逐步升降档；持续18mB/t最终9级，36mB/t最终18级。预热不再承诺40tick满功率；连续40tick无实际热转冷后停热。旧存档、携物和卸载不刷新已付余热期限。

## 验证口径

| 范围 | 最终结果与证据 |
|---|---|
| 受影响JUnit | 30/30通过：共享接收计划7、换热账本12、端口聚合6、逐口预算5；四份XML在[evidence](./evidence/) |
| 真实Create机器 | 两命名空间15/15 required通过，见[最终日志](./evidence/gametest-final-connected-faces-unified.txt)；三端与单端均为源1000＋堆冷12000＝13000mB，双面换热器源1000＋热罐4000＝5000mB；原生罐分流4000mB守恒；真实锅炉18mB/t稳定9级、36mB/t升至18级及生命周期边界通过 |
| 增量打包 | [assemble成功](./evidence/assemble-final.txt)。最终JAR含Mixin类、JSON及TOML中唯一配置引用；未新增依赖 |
| 独立审查 | [审查报告](./reports/REVIEW.md)未发现必须整改项；PM另已核对原始日志、XML和最终JAR接线 |

最终JAR为`build/libs/create_nuclear_industry-0.1.0.jar`，SHA-256：`E104542A0606AD363736B552D384795781ADD8AFBF8DECDDDE80922329C1045C`。GameTest在报告全部通过后停在`Saving worlds`；按治理只结束本轮自有测试JVM/包装器，保留daemon和用户客户端，再单独增量assemble。不能把该测试服退出异常描述成整条Gradle命令成功，也没有因此重复游戏断言。

必要复现与修正记录均保留：原始[三端72mB损失](./evidence/gametest-console-delayed-branch-flow.txt)是有效修复前证据；前两轮三端夹具分别未真实横连/过早断言，补充双面夹具曾有开放管口，封口改动又曾覆盖传动齿轮及误判非连接面，均不计为有效闭环测试。最终夹具只封实际连接的空气出口，保留真实传动、逐支流量和逐tick守恒断言。夹具整改期间生产代码保持冻结。

实现说明见[守恒修复](./reports/FIX.md)、[自适应供热](./reports/ADAPTIVE.md)、[动态复现及验证](./reports/REPRO.md)。[守恒诊断](./reports/CONSERVATION.md)和[低流量诊断](./reports/LOWFLOW.md)保留当时推断与方案语境；其中`build/reports/...`路径指候选中的原始执行目录，当前结论以本页和已批准方案为准。PM和执行者实际使用Minecraft模组、测试与系统化排错技能；验证按治理5.1节限定相关范围，复用未变代码的JUnit证据，不跑P1全量或资源全量矩阵。原始日志保留加载器输出的行尾空格，差异空白检查仅对源码与手写文档执行，未为了清除提示改写运行日志。

## 合并人工复测

关闭旧客户端后，在候选目录启动：

```powershell
Set-Location E:\MyMC\NewMod\Create_NuclearIndustry-ore-acquisition
.\gradlew.bat runClient
```

1. **原来的封闭回路：** 保留三冷端及现有管路，连续运行并比较全部冷热罐、反应堆、换热器的冷热总量；不操作桶或更换设备时，总量应保持一致。
2. **降低热液供给：** 保证锅炉水路正常，观察换热器护目镜和动力输出。升温后应维持较低档位，避免原先满18级与停热的大幅循环；非整数档位可在相邻等级间变化，极低流量仍可能0/1间歇。
3. **断供/堵回液与保存恢复：** 内部热液耗尽或冷罐堵满、实际转换停止后，最长40tick余热窗口结束即停热；恢复供液/排液后逐步升温。保存重进或扳手收起重放保持库存，不凭空恢复完整储热或延长期限。

用户已确认上述运行复测通过；自动证据与人工反馈仍分别记录。后续工作盆、专用蒸汽链与封存按各自方案和人工门推进，本次通过不代表这些功能已完成。
