# EXT-B-EXCHANGER-01D：定向接口与直列共享流体

**最新状态（2026-10-04）：** 本卡及后续锅炉01C/01D、换热器01E均已获用户手测通过并合入main，见[最终验收](../../reviews/2026-10-04/boiler-01d/ACCEPTANCE.md)。下文保留当时实施合同与证据，不再表示当前暂停或未合入。

**状态：2026-10-04用户已确认[整套方案](./2026-10-04-heat-exchanger-chain-proposal.md)，候选实现、定向验证和独立审查通过，暂停于联合人工门。** 候选为`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`、分支`codex/ore-acquisition`、Git基线`9e8690b`，叠加正在执行的锅炉01B未提交改动。PM已明确授权本卡，执行者禁止修改其他任务写集、核心文档或执行Git写操作；不得转派。保留用户日志、缓存、客户端配置和世界。

## 合同

1. 顶部供热，正面`FACING`为冷液出口，背面为热液入口；左右/底面/无方向查询均无流体接口。方向同时限制模拟和执行，禁止入口出液、出口进液以及错误流体。外观前蓝后橙，并以箭头/图案区分，保留上鳍片下基座和正常物品缩放。
2. 相同朝向、轴向相邻的水平直列共享冷热库存；最大16台，侧邻或反向不合并，仅列两端暴露外部管口。超过上限时整列明确拒绝工作，不截取16台让超长列局部工作或复制库存。无转弯、分支、GUI、自泵送冷液或热储备合并。
3. 每台冷热容量各4000mB；组容量各4000N。存档仍保存每台实际库存份额，拆机物品仅携本机份额和本机已付热。连接/断开/转向仅重建拓扑，不复制或凭空搬账；护目镜明确显示组库存/台数，勿误导逐台加总。
4. 每台热储备、分数流量、世界tick和余热期限保持独立，保留单台自适应1～18级与专用锅炉18HU/t上限。工作机的热转冷必须可读写整列成员：全列预检热液/冷液空间，按实转量原子扣热液并加等量冷液，然后仅给工作机补HU。其本地热为空或冷满不能在组库存充足时错误停机。普通查询、模拟与客户端不得结算。
5. 生命周期包括放置、拆除、扳手转向、收起、保存恢复、旧单机NBT、区块卸载和非活动区块。不能完整确认全列已加载且可tick时暂停共享事务及发布供热，不强加载；拓扑变化撤销缓存能力，旧句柄不得重活或作用于新成员；恢复后Create需能重新获取有效句柄。保留每台原绝对余热期限，不因重连刷新。
6. 复用现有`SharedFluidReceiver`身份/容量协议；多来源填充不能超额承诺或丢液，整列使用稳定库存身份和实时总空位。执行阶段复核实际接收量，不以模拟结果当实收量。禁止无必要地重写已验收的共享填充Mixin。
7. 旧机器库存/热NBT保留，既有侧/底管路需玩家重接新前后面；不自动改用户世界。旧配方/成本/ID/每台热工均不变。

## 技能与执行方式

执行者须读AGENTS、治理5.1、相关活动卡以及`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`；素材额外读`minecraft-resource-pack/SKILL.md`。核对MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82，不升级依赖。手写代码及非显然流体/生命周期算法中文注释，静态证据不代替客户端手测。共享状态使用较高能力执行者；外观使用高速模型。

## C：共享库存实现与定向回归

允许写：

- `src/main/java/com/iksxh/create_nuclear_industry/heat/`下本次相关类及必要直列/事务类；`content/HeatExchangeContent.java`仅能力接入必需部分。
- `gametest/ExtensionHeatExchangerGameTests.java`、`ExtensionHeatExchangerLoopGameTests.java`和新增`ExtensionHeatExchangerChainGameTests.java`；专用链路测试模板仅新增，不覆盖旧模板。锅炉测试由D所有，需接口协商。
- `src/test/java/com/iksxh/create_nuclear_industry/heat/`相关测试；保留已验收守恒/低流量/NBT边界，旧五面输入用例应改为合法定向布局，不能删除真实管网断言来提速。
- 两语言文件仅新增或修改`gui.create_nuclear_industry.heat_exchanger.*`键，与D的锅炉键互不覆盖，修改前重新读取文件。
- `build/reports/extension/EXT-B-EXCHANGER-01D-DEVICE.md`与同名证据目录。

只读准备报告在`build/reports/extension/EXT-B-EXCHANGER-01D-PREP.md`。其“仅聚合外部Handler”的初步建议已被复核纠正：不改本地热事务无法让中央机消费远端热液，也不能使用远端冷空位；不得按已否定的窄实现交付。

共享Create/mixin需变更时先报具体文件和原因，PM追加写集后执行。C不写锅炉包、构建脚本、配方或模型。全部Gradle运行权由D持有，C先交代码和测试，不自行并发启动。

## V：输入/输出外观

允许写`tools/art-assets/heat-exchanger-device/`的生成器、SVG源与generated输出；该目录`COMPONENTS.md`允许仅更新部件职责、前后映射和复现指令，不得改变玩法。资源写集限`models/block/nuclear_heat_exchanger/`及实际关联的根模型、`models/item/nuclear_heat_exchanger.json`、`textures/block/nuclear_heat_exchanger/`及`blockstates/nuclear_heat_exchanger.json`。不得新增Java状态或改其他素材；发现实际资源目录差异先报PM。

正面蓝色外向箭头、背面橙色内向箭头，左右和底部去掉可接管误导。输入输出面应在世界和物品预览可辨；维持鳍片/基座分件、动画预留及正常手持尺度，避免共面闪烁与突出到邻机的接口。生成器报告改为独立01D证据目录，不覆盖01B历史。交付`build/reports/extension/EXT-B-EXCHANGER-01D-ART.md`及同名目录。V不跑Gradle。

## 验证、审查和人工门

- C/D代码与两组素材就绪后，D统一一次增量assemble和必要定向验证。账本受改，运行HeatExchangerState相关JUnit；GameTest按锅炉与换热器相关namespace，复用其他已审资源/材料证据；失败只整改重跑受影响部分，不跑全P1。
- 必须有真实3台直列两端Create管线能使中央机工作证据，以及三排各3台支撑专用锅炉9段的代表场景；不能靠直接逐机灌液代替中央机共享证明。
- 必须覆盖错面/反向/侧邻/超限、全列热冷守恒、共享填充、成员本地空热/满冷但组仍可工作的事务、合并/中间拆分/旋转/旧句柄、拆放/NBT/非活动成员恢复。可以在少数有明确断言的场景合并验证，不机械扩增重复用例。
- 由未编写本轮实现的执行者做一次合并规格与质量审查。PM审核后归档证据、更新启动与手测清单；停在锅炉01B和换热器01D联合人工门，不开汽轮机。

## 本轮候选交付

[联合交付与证据](../../reviews/2026-10-04/boiler-01b/CANDIDATE.md)：22项相关JUnit、40项required GameTest及最终增量assemble通过，独立审查发现的Create接面恢复问题已整改并复验。PM核对代码/资源写集和原始日志，未重复全量；[三组人工清单](../../reviews/2026-10-04/boiler-01b/CLIENT-CHECKLIST.md)仍待用户，未合main、不启动汽轮机。
