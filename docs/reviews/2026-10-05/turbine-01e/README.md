# 汽轮机01E：两端贯通与共享容量

**状态：定向验证与独立审查通过，待人工复测。** 用户已确认[01E合同](../../../superpowers/plans/2026-10-05-ext-b-turbine-01e.md)，01D/R1外观人工门继续保留。候选功能未合入main。

两个输出轴现在通过机内连接属于同一Create动力网络。任意单端均可使用整台应力容量；两端同时接出时负载合计，超限共同停转、减载恢复，外部回接不会复制容量。

默认256RPM、三档处理率、总SU换算、40tick平滑窗口和耗汽不变。`frontShare`已废弃；红石立即停机，断汽沿用40tick窗口衰减归零。完整机组仍可传递同网外部动力。当前版本保存恢复时重复计算本机容量的问题一并修复。

用户于2026-10-05要求首个可发布版本完成前不再研究旧存档兼容，已写入AGENTS及治理5.2。本轮旧双源NBT调查已终止；相应新增测试和迁移代码已撤回，历史记录不作为交付门。当前格式保存与加载仍做必要检查。

## 精简人工复测

关闭当前客户端后从候选目录重启，仅F3+T无法加载Java更改：

```powershell
cd E:\MyMC\NewMod\Create_NuclearIndustry-ore-acquisition
.\gradlew.bat runClient
```

1. 稳定进汽、排汽畅通且无其他动力源时，前端、后端分别接同一组负载；负载大于总容量一半、小于总容量，两种接法均应正常工作。可降低进汽量减少所需负载。
2. 两端同时接出时容量只有一份，合计负载超限应共同过载，减载恢复；护目镜显示共享容量。
3. 当前版本停机/恢复及保存重进正常，不做跨版本迁移测试。

此前[01D/R1端口方向与密封复测](../turbine-01d/manual-checklist.md)按实际画面确认，不重测材料、配方、锅炉和换热器。人工门前不合main、不推进冷凝。

## 验证与证据

- JUnit **9/9**通过，复用同批未再修改的状态算法证据。
- 最终实际汽轮机GameTest **11/11**、真实Create网络探针 **4/4**通过，增量assemble成功，命令均退出0。探针覆盖外部回接、停机保留外源及双向实际区块卸载/重载。
- 当前格式真实供汽后保存实体并恢复，修前首tick多出32768SU，修后通过。证据为隔离服务端实体NBT恢复，不冒充客户端保存重进验收。
- 一次最终定点审查通过，未为审查重复运行测试。用户run及主目录launch配置未改；既有日志与缓存保留且不纳入提交。

最终证据：[执行报告](./evidence/implementation.md)、[JUnit](./evidence/TurbineStateTest.xml)、[当前机组与恢复](./evidence/machine-current-final.log)、[动力探针](./evidence/probe-current-final.log)、[当前恢复修前失败](./evidence/current-reload-failure.log)、[最终审查](./evidence/review-final.md)、[制品摘要](./evidence/artifact.json)。

本目录旧`machine-legacy-final.log`、`review-initial.md`、`review-r1.md`仅保留范围变更前历史，不代表当前兼容承诺或尚未解除的门槛。
