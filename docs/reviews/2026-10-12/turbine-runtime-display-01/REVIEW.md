# TURBINE-RUNTIME-DISPLAY-01 独立规格与质量窄审

审查对象：`E:/MyMC/NewMod/Create_NuclearIndustry-turbine-runtime` 的未提交候选，基线 `d63abfe`。本次为同一轮独立组合窄审，含原实现者完成的一项 P2 整改差异复核。审查者未执行任何测试、构建或 Git 写操作，唯一写入为本报告；不作项目验收或集成操作。

## 结论

R1 无剩余必改项，可由主PM按活动卡管理净集成并交原美术消费者。实际交付是只读运行前置；转子动画尚未在本批实现，动画客户端人工门保持。

当前冻结 JAR SHA256：`203E955881FE60074069E5A12D3C480A4166B877C2BD56DCDAFB57C5F7833266`。初次 `AEC242A8D3212281BC847610152A2007DC068098E5AA0ADC288C381DD6F925A6` 制品和原失败/通过记录保留；本结论绑定 R1。

## 实际阅读与审查范围

已读取实际 AGENTS、活动卡、治理5.1/5.2、IMPLEMENTATION/HANDOFF，以及 minecraft-modding、minecraft-testing、requesting-code-review（含 reviewer 模板）、verification-before-completion 技能。已核仓库锁定 MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280，不采用技能新版本示例改变技术栈。通用重复测试及派代理流程由本卡和治理覆盖。

已逐一读取 Controller/Rotor 差异、新 display 六类型、专属 JUnit/GameTest 与周边原 Form/动力源。锁定 Create 字节码确认 `getSpeed()` 在过载/冻结返回0，`SmartBlockEntity.setRemoved()` 最终通过 `remove()` 完成正常移除；NeoForge Post 文档确认采样位于世界本轮工作后。

## 规格与质量核对

- Publisher 只遍历 onLoad 登记机主弱引用，在服务端 Level Post 每5tick读取合法所属前轴实际 `getSpeed()`；后轴实速一致仅作安全判定。不采用配置256、TurbineView 或 generatedSpeed，不修改网络。真实外源和供汽/断汽证据与该链一致。
- 每个成员使用同一轮 owner/generation/epoch/front/facing/diameter/rotors/index/member/serverTick/rpm 完整不可变样本。水平合法几何、控制器侧槽、坐标、NBT精确类型及有限RPM整包检查；世界轴统一到前→后局部+Z，北/西保持符号，南/东反号。
- 控制器失效/卸载/移除、成员拆除或替换、身份与epoch不符、坏包、缺块、客户端会话结束均撤销。登记结构不强持 World/BE，不扫描全世界，区块访问前核可用性；初次跟踪使用新运行包，磁盘和可携数据不恢复可信样本。
- 客户端采用20活动tick租约，重复整包不续租，倒退与退休世代拒收。Pre事件以真实 `Minecraft.isPaused()` 冻结年龄，世界/区块结束仍撤销；common 代码不引用 Minecraft 客户端类，ClientEvents 仅 Dist.CLIENT，真实专服启动无隔离故障。
- Controller 原运行/交易代码保持，仅加实例身份、更新包元数据及登记撤销钩子。独立重新核8项保护来源与基线 Git blob 一致：State、Structure、ShaftPowerSource、OutputShaftBE、RotorRenderer、TurbineConfig、build.gradle、gradle.properties。模型、ledger、汽耗/SU/双端传播、流体能力、红石及成型规则未见夹带修改。
- HANDOFF 的 `runtimeDisplay(): Optional<TurbineRuntimeSample>`、字段单位、符号、成员查询、停转0与不可用空的区别符合实际实现；R1公共ABI与初次列表逐行相同。美术仅消费整包，不能调用内部发布/续租伪造可靠性。接动画仍由原消费者在其授权写集完成，本审查不授予新路径权限。

## 同一次审查 P2：已关闭

初次 `TurbineRuntimeLease.java:21` 在相同 serverTick 时提前返回，未撤销同tick更高epoch或不同member/几何/RPM的包；`TurbineRotorBlockEntity.java:37-38` 当时验证新pending却返回旧lease.latest。

失效序列：旧样本 G/epoch2/M/tick10/128RPM 已生效；随后收到与当前权威身份匹配的 G/epoch3/M2/tick10/0RPM，receive返回false，但current仍可返回旧epoch2/M/128RPM。查询又可能由新包通过校验而返回旧成员。

已核 R1 `TurbineRuntimeLease.java:21-25` 仅完整equals整包作为不续租重复，其他同tick差异立即撤销；`TurbineRotorBlockEntity.runtimeDisplay()` 先取得实际返回值，再核其member、整包一致、几何、当前owner对象及generation/epoch。新增测试涵盖身份、几何、RPM差异、真重复及撤销不复活。

`08-review-red.log/xml` 实际3项、2失败1通过，明确复现旧128RPM残留；`09-review-green-assemble.log` 与 `r1/final-junit.xml` 实际16/16、零失败/错误/跳过，必要第二次增量assemble成功17秒。整改只改2生产+1测试，JAR仅RotorBE和Lease两class变化。业务、Publisher和真实探针未变，复用06的真实3/3，不扩大或重跑。

## 证据身份及验证边界

证据索引见 IMPLEMENTATION。审查者独立读到 `06-gametest.log` 的3个实际测试、全通过、保存完成与自然关闭、Gradle成功26秒；读到初次07定向13/13及增量成功16秒。首次 final override 编译失败与首次 `No test functions were given!` 均保留，筛空的Gradle成功未计业务通过。

独立重新计算 R1 的11项当前来源与冻结副本SHA256全部吻合；13个独立JAR项与当前编译class/资源逐字一致，build/libs与R1冻结JAR整体SHA一致。初次与R1 ABI对比无差异。仅本P2新增证据复核，没有复跑服务端、JUnit或构建。

考虑后未纳入本批验收的行为：

- 自然负载过载全矩阵：本探针仅用原生updateFromNetwork夹具注入，证明实际getter的0读取；不宣称自然负载矩阵。
- 实际区块驱逐/图形客户端世界切换与单人暂停：本批覆盖真实卸载钩子、纯活动clock/lease及代码撤销链；不扩大为客户端人工通过。
- 三个其他朝向的真实模型旋转/GPU表现：符号字面期望JUnit覆盖四向，真实轴探针为NORTH；实际动画观察由原美术人工门承接。
- 原热工/汽耗矩阵、未验流量候选、锅炉16³、旧版本存档迁移：活动卡和治理明确范围外，原证据/人工门保持，不据此扩展任务。

这些范围已在IMPLEMENTATION/HANDOFF明确，未以自动证据关闭人工门。无其他Critical/Important问题；不为格式偏好扩大实现或添加机械重复验证。
