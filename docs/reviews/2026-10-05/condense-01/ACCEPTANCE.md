# 汽轮机与冷凝回水联合验收

**日期：2026-10-05。接收候选：01b1578，最终功能基线：8273aff。** 用户先确认停机/恢复复测，随后针对冷凝闭环、效率曲线和此前外观的范围问题明确“所有测试项都通过了”，本批人工门全部解除。

## 通过范围

- 01D/R1六项外观、端口外方内圆的方向/密封、叶片与核心材质、独立壳/控制器封面与碰撞、输出轴闪烁和名称；三档功能沿用此前实际通过记录。
- 01E双端共享唯一容量；01F按实际排汽计量、30%启动门槛、0.5倍起点到各档1.2/1.5/1.8倍的效率曲线、54/108/216mB/t与1tick微周转、堵排汽后的拒收和恢复。
- 01G无外源断汽后两端与外接轴/转速表停止，重新供汽恢复；不再持续零SU转动。
- 冷凝[联合清单](./manual-checklist.md)：锅炉→汽轮机→冷凝→给水守恒，顶格冷源、移除及堵塞暂停恢复，代表性融水/蒸发观察；护目镜图标、产汽读数及中文提示顺带通过。

自动覆盖的全部冷源种类、非默认配置和非清单专项，不因“所有测试项”扩记为额外人工测试；安全阀实际排汽仍会损失工质。首发前不研究旧存档兼容。

## 自动证据复用

| 批次 | 已通过证据 | 实现基线 |
| :--- | :--- | :--- |
| 01D/R1资源、普通放置与碰撞 | [资源/运行与原审查](../turbine-01d/README.md)，含最终加载与资源打包一致性 | b0b3693、9159155 |
| 01E共享双轴 | [运行、账本和审查](../turbine-01e/README.md) | 0477d8f |
| 01F新效率/周转 | [7项JUnit、11项真实GameTest、1项非默认SERVER及审查](../turbine-01f/README.md) | f80dcad |
| 护目镜显示 | [1项GameTest、增量打包与审查](../goggles-01/README.md) | dee613b |
| 冷凝回水 | [22项JUnit、8项真实GameTest、配置与审查](./README.md) | fdfba38 |
| 01G停机时序 | [修前失败、修后17项真实GameTest与审查](../turbine-01g/README.md) | 8273aff |

以上是各批原运行，不叠加成一次新运行。PM核对最终候选在8273aff后没有修改功能、资源、依赖及配置默认值；无冲突整合后的相应目录与候选一致。按治理5.1复用原证据，只新做一次主目录增量assemble，不运行重复JUnit/GameTest或客户端测试。

## 整合与启动

从main基线6e1ed25，在现有干净同级工作树`Create_NuclearIndustry-svg-art`的`codex/thermal-loop-acceptance`准备无冲突合并，接收`codex/ore-acquisition`的01b1578。PM仅更新文档与执行Git；功能源码由原执行者实现。主目录随后快进到整合提交，运行方式：

```powershell
cd E:\MyMC\NewMod\Create_NuclearIndustry
.\gradlew.bat runClient
```

主目录`.vscode/launch.json`保留原用户改动；其SHA-256仍为`65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`。候选原日志、pycache、运行配置与测试世界均保留，不搬移世界、不清理工作树或推送/发布。

**本次合入验证：** 主目录增量打包及制品记录待执行，完成后在此补充实际日志与结果；原候选制品SHA-256为`79E3B45C2D0A6142A566994260C549FE75CD31E6F9736790DA8C1B136C1827A2`。

## 下一主线与版本边界

准备乏燃料基础封存：玻璃固化基材、屏蔽装配台灌封、铅屏蔽桶、干式贮存架。已批准工序不重问；配方数量、封装状态/交互及库存合同须另行冻结后派发。工作盆加热、完整温压、二级耐压、可变结构和动画继续后置。

PM实际应用minecraft-modding、minecraft-testing、minecraft-ci-release及finishing-a-development-branch、verification-before-completion、using-git-worktrees；沿用锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。版本仍为0.1.0 Alpha；热端闭环验收不等于首个完整生存版本发布出口完成。
