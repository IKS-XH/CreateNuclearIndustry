# 离心机01F端盖接缝修复

**状态：修复提交`1b5f81a`已交付，增量打包和独立审查通过，只待端盖视觉复测。** 用户已确认01E后的其他手测项目通过，本轮仅修复上下端盖与八棱壳体的接缝。候选工作树`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支`codex/ore-acquisition`；main仅同步文档，功能未合入。

底部原盖板顶面为Y=5，壳体从Y=6起，存在1模型单位的空隙；两端原方形盖板也没有覆盖壳体外缘。现改为四条不同方向的贯通板组成八棱端盖，扩大轮廓并与侧壳微量搭接，各板表面微小错层，避免可见共面重叠。观察孔、顶口、转子、PNG和物品显示变换不变。依据[01F任务卡](../../../superpowers/plans/2026-10-02-ext-a-fuel-01f.md)。

## 证据

- [实现报告](./assets-report.md)：改动仅生成器及下段、上段、完整物品三个模型。
- [修前诊断](./assets/pre-fix-diagnostics.json)与[修后检查](./assets/geometry-check.json)：两端接合处各4096个周向探点由0覆盖变为全部覆盖；四模型共760面显式UV，无越界。原中间拼接检查仍通过。
- [斜角预览](./assets/centrifuge-endcap-angle.png)与[底视预览](./assets/centrifuge-endcap-bottom.png)：仅展示几何与配色，不代替客户端材质、光照及端盖视觉验收。
- [唯一一次增量assemble](./assets/assemble.log)：2秒通过，仅执行资源处理和打包，Java无需重新编译；未跑JUnit、GameTest、全素材测试或clean。三个打包模型与源码字节一致。
- [独立审查](./review.md)：一名审查者核对实际模型与既有证据，无阻塞项；未重复运行验证。报告中的90°为长轴方向，JSON以交换X/Z尺寸表达，未使用Minecraft不支持的90°元素旋转。

最终JAR SHA-256：`36ED63B5D8A02C532CA81E234280075872FD23AF884DB0529D652BC14734A543`。后续文档同步不重跑验证。

## 唯一人工项

完整退出客户端，再从候选目录运行`.\gradlew.bat runClient`。绕现有离心机略俯视、仰视观察上盖/底盖与八棱壳体接合，预期无透背景接缝、无盖板重叠闪烁；顺带看手持完整模型的同一位置。无需重做已通过的加工、物流和保存恢复测试。

记录用户视觉结果前，暂停自动推进及main功能合入。
