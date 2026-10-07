# 离心机01E缺陷修复候选

**最新人工反馈（2026-10-02）：** 用户确认“模型的上盖和底盖跟四周一圈八棱柱仍有接缝，其他的都手动测试通过了”。其余人工项记为通过，只保留[01F端盖接合整改](../../../archive/2026-10-08-completed-plans/2026-10-02-ext-a-fuel-01f.md)与视觉门。下文为01E交付时的证据和复测步骤，未实测边界不追溯扩大。

**01E交付记录：** 候选功能提交`211cad5b0e99472c22e99a15262cca49e5915c7b`。工作树为`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`、分支`codex/ore-acquisition`。main只同步文档，未合入本轮功能，未推送或发布。

用户在01D手测发现壳体接缝、手持模型上部纹理错乱及上段不能连接Create管道，依据[01E整改卡](../../../archive/2026-10-08-completed-plans/2026-10-02-ext-a-fuel-01e.md)完成以下修复：

- 八棱壳板长度改按外侧半径计算，加入微量搭接；观察孔和造型保持原合同。
- 四个模型全部明确UV，物品上段沿用局部贴图坐标，跨格转子重新映射到0..16，避免采到纹理图集的邻居。
- 上段增加无状态、无动力和加工ticker的代理BE，满足Create在查询能力前的实体检查；下段继续独占库存、批次和应力。上段顶部进浆、四周输出规则不变。

## 已验证范围

| 证据 | 结果与边界 |
| :--- | :--- |
| [运行报告](./runtime-report.md)、[真实业务RED](./runtime/red-gametest.log)、[最终GREEN](./runtime/green-gametest.log) | 隔离namespace仅运行1个GameTest；修前上段UP连接识别失败，修后五个面被Create识别。测试移除上段BE后查询补建，再由实际Create储罐、机械泵、玻璃管及动力轴网送入1000mB料浆，断言源罐0mB、机器1000mB及流体身份；最终1/1通过并正常保存退出。 |
| [端口JUnit XML](./runtime/TEST-com.iksxh.create_nuclear_industry.production.CentrifugePortsTest.xml) | `CentrifugePortsTest` 3项、0失败、0错误；未运行全套JUnit或GameTest。未改桶事务、配方及加工数值沿用01D已有证据。 |
| [模型报告](./assets-report.md)、[修前诊断](./assets/pre-fix-diagnostics.json)、[修后检查](./assets/geometry-check.json) | 物品168面、转子76面的UV越界均归零；四模型688面全部显式UV且在0..16。两个壳体接缝横截面各4096条射线从96条未覆盖变为0；四张PNG及显示缩放未改。 |
| [独立合并审查](./review.md) | 核对实际JSON、锁定Create/Minecraft源码、代理生命周期、测试条件和JAR；没有重复测试。几何预览不等于实际游戏UV/光照。 |

资源冻结后一次增量`assemble`通过；最终JAR SHA-256为`B19F67275265169C79BAFD5C9CB190D74E2092B9907CA00912475F7537D81898`。最后仅修正两行过时中文注释，未重复构建或测试；后续文档同步也不重跑。

测试入口保留`-PgameTestNamespace=create_nuclear_industry_01e -PgameTestDirectory=build/test-worlds/centrifuge-01e`，默认Gradle行为未变；独立namespace使用一份与原空模板字节相同的fixture。前期模板配置失败和零测试启动不计为业务RED/GREEN。根`logs/`由本轮`forgejunitdev`写入，PM确认身份后定向恢复；未改默认run世界、用户进程或主工程`.vscode/launch.json`。

## 复测与兼容说明

完整退出客户端，再从候选目录运行`.\gradlew.bat runClient`，只按[三项复测清单](./CLIENT-CHECKLIST.md)检查接缝、物品外观和上段接管。旧普通Create管道可能保存着未连接的方向状态，若重启后仍没有接头，拆下紧邻机器的管道再接上即可，离心机无需重放。

自动测试未覆盖普通旧管连接状态自动刷新、完整区块卸载重载或先管后机完整玩家操作；上段侧面实际抽水及视觉仍留人工确认。01D其他尚未确认的人工项目保留原状态。自动推进暂停，不提前合入main或推进后续设备。
