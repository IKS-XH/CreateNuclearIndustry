# 改装自动化接口后的成型恢复候选

2026-10-14接续更新：原修复已随[03三页GUI集中候选](../../2026-10-14/reactor-administrator-03/CANDIDATE.md)`78acd5d`／879c3d72…接续交付，本页5e04为01R历史制品。改装与护目镜复看并入03清单，原世界原位，未追认用户通过。

2026-10-13。源码提交`2dff787`，在原`codex/reactor-automation-basic`及原测试世界交付；本次18/18定向、3/3真实代表自然保存退出0、唯一1秒增量assemble和一次独立组合窄审通过。当前只待改装与护目镜局部复看，原01集中人工门保持，02不派发；未合main。

## 修复与现有现场

实际16³结构在安全拆壳清空时退回默认5³，原冷端在延后重扫前补液使其不再是全空快照，仪表因此拒绝实际尺寸；扳手旧逻辑却只报告几何成功。现安全清空保留实际尺寸，初始化门仅接纳没有任何核状态、待迁移燃料且新容量容得下的纯冷液，冷量原样保留；热液、列、SCRAM、事故及超容量状态仍拒绝重组。扳手返回仪表最终接受结果。

当前保存型5³＋32768mB纯冷液已通过正常BE读取/重扫代表，数量未减少；用户世界没有被启动、编辑或清空。这是当前版本故障恢复，不增加旧版本迁移。实际16³世界加载及客户端护目镜仍需局部复看，不能由6³代表代替。

## 启动与局部复看

继续使用同级候选目录及原世界，不重新搬存档：

```powershell
Set-Location E:\MyMC\NewMod\Create_NuclearIndustry-reactor-automation
.\gradlew.bat runClient
```

重启候选并进入原世界，先观察正常加载后的成型与护目镜；如尚未刷新，持Create扳手右击仪表端口触发正式重扫。应显示实际尺寸、冷液库存和有效运行数据，接口与燃烧室可安全任职。不要求拆仪表、丢弃冷液或重搭16³。已有机组拆壳仍必须遵守原停堆与余热规则。

本次局部问题复看后，继续原[管理员集中手测](../reactor-automation-01/CLIENT-CHECKLIST.md)；没有据此关闭其控件、控制、联锁或生命周期项目。

## 证据与制品

- 旧生产有效行为RED为`04-red-corrected.log`三项预期失败；首轮零用例及两次夹具误设日志保留，不冒称故障RED。
- `05-junit-green.log`及冻结XML：摘要7、可变尺寸4、scanner4、接口结构3，共18/18，零失败/错误/跳过。
- `06-server-green.log`：改装窗口、正常NBT读取恢复、十项拒绝与干净尺寸重建三个真实代表全部通过，所有维度自然保存，Gradle退出0。BE读取代表不等于完整客户端重启。
- `07-assemble.log`：唯一1秒必要增量包，不重复完整build或已验旧设备域；[独立窄审](REVIEW.md)无剩余必改项。

正式候选`build/libs/create_nuclear_industry-0.1.0.jar`与本批冻结`build/reports/reactor-automation-retrofit-fix/reactor-automation-retrofit-candidate.jar`一致，3138597字节，SHA256：

`5e04c667fc509a0eb9349bb95faf6c3a15b5d19a961ffdeb29458d00b5c2d434`

原4204包在本批`baseline-4204.jar`及原01冻结副本中保留。五条本批来源、646生产class及1546资源经PM独立实哈核对与当前编译/复制输出和正式包一致；相对原包仅两生产class和两个指令相同的行号伴随class文件变化。新测试class/NBT未发布，仅两个空目录占位。第一次PM ZIP全项比较因此发现目录差异，限定非目录文件载荷后全部核对通过；没有为目录重复打包。

原生产功能、参数、配方、控制算法与所有用户世界/配置保持；原01nofall保存挂起与其他独立人工门不被本次自然退出证据覆盖。详细诊断与实际实施见[IMPLEMENTATION](IMPLEMENTATION.md)，活动权限见[窄修卡](../../../superpowers/plans/2026-10-13-reactor-automation-retrofit-fix.md)。
