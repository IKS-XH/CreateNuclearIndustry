# EXT-B-BOILER-01A：紧凑高压锅炉实施

**状态：2026-10-04整套方案获用户批准，候选`af106ab`完成实现、定向验证与独立审查，暂停于[人工验收门](../../reviews/2026-10-04/boiler-01a/CLIENT-CHECKLIST.md)，尚未合入main。** 批准合同：[完整方案](./2026-10-04-high-pressure-boiler-proposal.md)。本卡只细化执行分工，不新增玩法或再索取已获授权。

## 全局合同与基线

- 候选：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，`codex/ore-acquisition`。准备起点`8bf423d`，批准与派发文档已在`891f73e`冻结，本批功能审查以`891f73e`为基线。功能人工通过前不合入main。主目录用户`.vscode/launch.json`、两处客户端/存档均不操作；候选原有`logs/debug.log`、`logs/latest.log`、`tools/art-assets/__pycache__/`保留。
- MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82；不升级依赖、许可证或发布方式。沿用现有E盘隔离工作树，不新建C盘目录。
- 执行者须读AGENTS、治理5.1、本卡、完整方案和对应技能。角色为执行者，禁止Git写、核心文档、转派；中文注释合同适用。只有PM维护文档、验收和版本。
- 无GUI、固定3×3×4手搭结构；34壳位、内部两空气格；底中心外壳、外围1～8热段；侧下排1控制器＋1～3水口、侧上排1～4汽口；顶中心1安全阀。控制器唯一持有库存，端口只代理；禁止共享结构认领或重复热量所有者。
- 每段18HU/t，0.5HU/mB默认密度、每台36mB/t等量热返冷，八台最大144HU/t；1水＋1HU→1超临界蒸汽，流体1HU/mB身份不随配置重估。
- 水/汽各16000mB、整炉入水/出汽各256mB/t共享预算；暖炉3600HU/段、有效额定10秒；停收热损失0.9HU/t/段、≤25%退出就绪，重新暖满；炉体热不能重复产汽。泄压90%开/80%关、最高256mB/t且须上方空气，明确排放计数与表现。
- 原生Create锅炉自适应、反应堆热工/燃耗与已验收配方不变。工作盆、普通辅助热、普通蒸汽产汽/湿蒸汽曲线、二级耐压管、汽轮机/冷凝、事故及Ponder不在本批。
- 注册本批`supercritical_steam`（不注册尚无本批用途的普通steam），无桶/世界流体放置。七方块ID：`high_pressure_boiler_casing`、`high_pressure_boiler_window`、`high_pressure_boiler_water_port`、`high_pressure_boiler_steam_port`、`high_pressure_boiler_controller`、`boiler_safety_valve`、`boiler_heat_exchange_section`。不要另造整机物品或旧换热器模块。

## 任务D：设备、账本与真实接入

执行者BOILER-DEVICE：跨设备热事务与多口能力属复杂任务，使用较高能力模型；独占全部Java、语言文件及Gradle运行。

**允许写集：**

- 新`src/main/java/com/iksxh/create_nuclear_industry/boiler/`、`content/BoilerContent.java`、必要的`config/BoilerConfig.java`；修改`CreateNuclearIndustry.java`、`content/ModCreativeTabs.java`以注册接线。
- 现有`heat/HeatExchangerState.java`、`NuclearHeatExchangerBlockEntity.java`、`HeatExchangerBoilerBridge.java`及必要的`NuclearHeatExchangerBlock.java`、`content/HeatExchangeContent.java`。只为专用热量交付和生命周期扩展，不改变原生锅炉行为。
- 复用`compat/create/SharedFluidReceiver.java`等共享接收机制。若确需修改该包或`mixin/FluidNetworkSharedFillMixin.java`，先报告原因和限定范围，不重写无关管网；新锅炉水口必须直接实现共享库存/整炉配额身份，防止上批已修复的模拟过度承诺重现。
- `src/test/java/.../boiler/`及受影响`heat/HeatExchangerStateTest.java`；新`gametest/ExtensionBoilerGameTests.java`及本批必要的结构测试资源。实际类路径均以包前缀`com/iksxh/create_nuclear_industry`为准。
- `assets/create_nuclear_industry/lang/zh_cn.json`、`en_us.json`的本批名称/遥测键；必要的蒸汽流体兼容标签，限`data/create/tags/fluid/`内已验证用途的新增或追加，不删除旧成员。
- 固定设备移动限制可在`data/create/tags/block/non_movable.json`中仅追加本批七块，保留旧成员；不得借此扩大到其他设备或改写Create运动规则。
- 交付`build/reports/extension/EXT-B-BOILER-01A-DEVICE.md`及同名证据目录；构建输出。默认不改Gradle/依赖/测试框架，筛选已有`-PgameTestNamespace`与`-PgameTestDirectory`入口。

**实施顺序：**

1. 先用最小有意义用例覆盖暖炉与产汽分账、分数尾量、停热冷却、泄压及保存恢复，再实现纯状态。验证模拟请求不变状态，执行时库存与热量一次提交，时间回退不增能。
2. 实现固定结构校验及唯一控制器/端口代理；方位模型契约如下：控制器/水口/汽口使用水平`facing`；其余四块使用无方向静态模型。额外运行属性须兼容素材的部分属性匹配，不更名上述facing。方块/端口实际朝外，不借null方向暴露绕过限额的能力。
3. 对世界结构变化/拆放/重扫/区块活动建立失效机制；不正常tick逐块重扫，不强制加载区块。结构或源失效时缓存能力立即拒绝，未成型不吞水汽。移动结构不搬运活跃锅炉状态；沿用本项目现有固定机器移动限制。
4. 接入原换热器唯一热账本：专用锅炉真实申请→有界领取，源扣量等于受端收量。每世界tick各段最多额定18HU；多查询/源与控制器tick先后/同源切换负载不可重复付热。过期/未活动源不给热；没有有效需求不继续转冷。新增方法可自行设计，但不得更改已冻结玩法，不能从只读`publishedHeat()`推导免费HU。
5. 注册超临界蒸汽并打通Create真实泵管/储罐；不提供桶、世界放置或原版蒸汽引擎虚构输入。蒸汽源/流态按实际API兼容实现，禁止用空气放置把液体无声删掉。以实际流体能力及管网测试证明，不以JSON存在代替。
6. 护目镜/红石停产/扳手重扫、单件控制器携库存回收；普通破坏与扳手都恰好一份库存。阀门粒子预算有限，堵塞不排放不计损；来源查询、客户端显示和模拟不得变库存。
7. 运行本批必要定向测试和增量assemble，素材完毕后检查完整候选，交付原始日志与结果。不要启动用户客户端或修改其世界。

## 任务A：原生配方与素材

执行者BOILER-ASSETS：高速模型，独占以下资源和生成器；不改Java、语言文件或执行Gradle。

**允许写集：**七方块对应`assets/create_nuclear_industry/blockstates/`、`models/block/`、`models/item/`、`textures/block/high_pressure_boiler/`；蒸汽`textures/fluid/supercritical_steam*`；对应七条`data/create_nuclear_industry/recipe/`和七个`loot_table/blocks/`，以及`data/minecraft/tags/block/mineable/pickaxe.json`、`needs_iron_tool.json`中的本批追加；必要的本批专用物品标签。不得改旧配方或替换已有矿物/机器素材。

工具只写新增`tools/art-assets/boiler_01a_assets.py`、`tools/art-assets/svg/block/high_pressure_boiler/`及对应蒸汽SVG、生成输出/预览和`build/reports/extension/EXT-B-BOILER-01A-ASSETS.md`。不改全局导出器/manifest/README/美术基线，不使用生图模型；使用SVG/JSON可复现导出PNG。

蒸汽纹理位于`textures/fluid/`，A可在`assets/minecraft/atlases/blocks.json`仅追加两张超临界蒸汽的single图集源并核对引用，保留已验收浆料条目；不得覆盖或移除旧图集内容。

按完整方案第5节精确制作：外壳9宫格产8、五种部件无序工作台各1（窗、水口、汽口、安全阀、换热段）、控制器21格产1；材料ID/标签读取已实现配方，不凭中文名猜ID。21格关闭镜像。不注册序列半成品。

外观主体占满结构格、衔接面无共面重复；窗口可透明但框架完整，控制器与两口朝向可辨。安全阀/换热件分模型命名预留动画，当前静态模型无需额外渲染器。物品继承正确块模型和原版缩放，不做平面图标、不超大、不漏上盖/端盖。所有纹理有实际PNG且UV在有效范围。

控制器标准loot只出一个控制器，库存由D任务Java快照附加，勿再通过loot重复复制NBT；其余六块各掉自身。准备静态预览和资源引用核查；实际客户端视觉保留人工门。不得复跑旧素材全量矩阵。

## 验证与审查

- D持有唯一Gradle/测试服运行权，隔离测试目录；A只运行自己的导出/静态校验。实现前不跑未变基线全套。
- 定向JUnit：暖炉/产汽能量、体积与泄压账、共享额度与simulate、NBT/时间；补真正受影响的换热器和共享接收组，复用未改原生生产链证据。
- 必要GameTest：真实Create多入口给水不超承诺、真实蒸汽口→泵管→储罐；热源实付/无负载/截止窗口和至少一例已验收原生锅炉9/18行为；结构失效与控制器回收不复制库存；代表工作台/动力合成加载匹配。可在少量综合场景覆盖，不堆同义测试。
- 测试日志分别记录断言汇总与进程退出；遇已知Saving worlds停滞保留通过证据，只处理本轮自有测试进程，不反复重跑断言、不动用户客户端。
- 完整候选后一次独立规格＋质量审查，审查者只读既有证据；真实问题交原执行者修复并只复验受影响项。PM不代写功能/测试。
- 特别审查五项：源/控制器tick顺序与热量重复领取；多个水口共享容量/整炉速率；结构认领/卸载失效；控制器破坏和携物重载；蒸汽无桶/无世界放置且真实管网可输送。
- 用户人工门按完整方案三组，由PM整理一张带分层搭建图的清单；未手测不写完成、不推进汽轮机。

## 派发记录

- BOILER-DEVICE：`/root/boiler_device`，gpt-6-sol/high；设备、唯一热账本和真实Create接入已交付，27项定向JUnit、7项锅炉GameTest、11项受影响原生回归及assemble通过。
- BOILER-ASSETS：`/root/boiler_assets`，gpt-6-luna/high；七套资源/配方已交付。观察窗共面重复及无方向窗口仅单面可视已在同范围整改，重导出静态检查0错误；客户端视觉仍待人工门。
- 合并审查：`/root/boiler_review`，gpt-6-sol/high；复杂跨设备守恒和生命周期使用高级模型，合并规格/质量一次审查，无须修问题，可进入人工门。审查直接复用自动证据，未重复运行。
- [候选交付与归档证据](../../reviews/2026-10-04/boiler-01a/CANDIDATE.md)已保存。人工验收未进行，后续汽轮机暂停派发。
