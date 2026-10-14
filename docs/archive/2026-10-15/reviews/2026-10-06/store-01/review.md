# STORE-01 合并规格、代码与资源审查

审查身份：只读执行者，不是项目经理。审查目录 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；候选 HEAD `4268d008647080b21dcc1531553c312da3a636f0`。本批实现是未提交差异和新文件，不能只按提交区间审查。核心 docs 的 PM 更新与既有日志、`tools/art-assets/__pycache__/` 不计入实现者夹带。

**最终审查结论：初轮唯一 P2 已按最小范围整改并完成定点复核，当前无 P1/P2 阻断问题，可进入既定联合客户端手测。** 自动通过与静态预览均不等于人工验收完成。本报告仅记录审查结论，不修改任务状态，不授予合入或验收权限。

## 唯一确认问题：P2，架子物品缺少原版显示变换继承

- 定位：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/src/main/resources/assets/create_nuclear_industry/models/item/dry_storage_rack.json:2`；其父模型 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/src/main/resources/assets/create_nuclear_industry/models/block/dry_storage_rack_0.json:1`；生成源 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/tools/art-assets/store-01/generate.py:177`。
- 复现：沿 item 的 parent 读取零档方块模型，它既没有继续引用 `minecraft:block/block`，也没有 `display`。已检查同版本本地 Minecraft 资源与 `BlockModel` 源码：原版标准 GUI、ground、fixed、firstperson、thirdperson 变换定义在 `minecraft:block/block`；缺少 display 时解析为 `ItemTransforms.NO_TRANSFORMS`，只沿显式父链继承。本候选及原 JAR 的父链都止于零档模型，因此实际使用无缩放/无旋转变换，不能取得合同要求的标准方块物品展示，手持和物品栏展示尺度/视角错误。这里是确定的资源缺失，具体画面仍待客户端确认。
- 影响：STORE-01-ART 明确要求“物品模型用0档方块模型及原版display继承”；素材报告 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/docs/reviews/2026-10-06/store-01/assets.md:9` 所称“继承其标准显示行为”与实际资源不符。现有静态检查只检查父路径存在，没有核验最终 display 来源，所以会遗漏本问题。
- 最小整改：在零档方块模型补 `parent: minecraft:block/block` 或等价标准 display；同步本批生成器的零档模型输出，补一次实际父链/显示继承检查并修正素材报告。保持方块状态、库存、Java 和配方不变。重新增量打包并核对该资源；不重跑 JUnit、GameTest 或热端回归。PM 已派原素材执行者按上述范围整改，初轮记录时尚未复核整改产物。

## 合同与实现核对

- 注册与边界：独立注册三种材料、max-stack 1 的封装桶、架子方块/物品/BE；独立 SERVER 配置文件、1～16 容量范围已接入主入口。新增 `SpentFuelStorageConfig.java` 受通用 `config/` 忽略，但已直接读取及用 `rg --no-ignore` 核对，原 JAR 包含对应 class。PM 纳入 Git 时必须显式包含该源码，不能凭普通 status 判定写集完整。未改技术栈、构建脚本、许可证或机器尺寸/动力参数。
- 载荷：`SpentFuelPayload` 校验正式单件桶与唯一数量1的正式枯竭组件；`seal` 保存完整原始 ItemStack 的拷贝到 `DataComponents.CONTAINER`，拒绝新燃料、缺载荷、多件和异物；桶不是可重新使用的新燃料，也没有另开可提取载荷库存。实际封存输出由槽0原栈生成，JEI result 是展示模板，生产过程不直接使用无载荷模板。
- 装配：首件真实接受的原料选工序；模拟、拒收和其他工序材料不锁工序；输入/输出/进度全空才解锁。最多四输入、唯一单件输出账本被人工、漏斗能力与机械臂复用。原制造 JSON 为8/4/2/1及25600 RPM·tick；封存为1/1/1及12800 RPM·tick。普通输入数量和工时来自数据，配方重载逐 tick 核对，参数变化/失效或退料不足批清工时，原料保留；停转、低速、过载、满输出暂停有效进度。32 RPM 门槛、256计工封顶与4 SU/RPM保持。
- 展示与物流：机械臂调用同一输入能力，出料仍从侧面；JEI 根据 inputs 列表显示三/四料和实际数量，参考时间按 work/1280 计算。护目镜中文标题通过项目统一缩进入口，状态与实际原料数量显示齐备。实际 Create/原版漏斗搬运、JEI 排版及护目镜遮字仍属联合手测。
- 架子：固定16单件槽，六面代理同一账本；顶/四侧可投取，底部只取。满背包不扣桶，空手取最后占用槽。减容后全架总数与新有效槽范围同时限制新增，所有原槽保留且可取出；storage_level 只由真实库存派生。保存与同步携带完整栈。移除回调 drain 后清空，重复回调不能再次掉桶；普通拆除、创造拆除保存内容、扳手回收空架以及缓存能力失效已有对应实际断言。架子 loot 不携带第二份桶。原版创造模式通常不生成架子物品，规格只要求创造拆除保全已存桶，不据此增设创造掉架需求。
- 原生配方与标签：1无色玻璃磨制→1碎料/100；碎料+石英粉+黏土球 heated mixing→4基材/100；四个独立铅板 ingredient+钢板+密封环→4桶；屏蔽混凝土+两钢板→1架。专属 vitrification_media、lead_shielding_casks 标签被封存配方实际引用，公共铅板/钢板/石英粉标签含本项目正式材料。loot/pickaxe 与1.21.1单数资源目录一致。
- 素材：实际查看静态预览、SVG/PNG检查明细、模型/方块状态。四物品可区分；四向五档映射完整；模型纹理引用成立，六面封闭元素、单格边界与完整碰撞一致。接触面的反向面不作为可见同向 coplanar 叠面报告。前后/四向实际光照、遮挡、闪烁与碰撞表现仍待客户端。
- 中文合同：已读新增 storage/config/content/GameTest、被改装配类与本批生成器；手写说明/Javadoc 使用中文，原文 API/协议名保留。关键服务端唯一账本、模拟、配方重载、容量减容、同步与拆除分支有职责/不变量说明。未发现需要扩展修改的注释语义缺口。

## 复用的自动与打包证据

本审查没有执行 Gradle、重跑测试、运行导出器或启动客户端；只读核对既有原始记录与制品。

- `build/reports/extension/STORE-01/red-missing-api.log` 实际记录新增用例在旧接口上编译失败，缺少 configure/operation/带工序 insert；它不是业务通过证据。
- `junit-assemble.log` 是一次定向 test+assemble，`BUILD SUCCESSFUL in 23s`；对应两个 XML 实际为装配5项+贮存3项，共8项，失败/错误/跳过均0。
- `gametest.log` 只启用 `create_nuclear_industry_store01`，发现6项并明确 `All 6 required tests passed`，正常保存全部维度且退出成功。已读六个场景断言，涵盖载荷、首料/模拟/混料、真实动力封存与原制造、动态数量/工时及失效恢复、六面/满包/减容保存、拆除/扳手/缓存能力；不把断言保存回读等同用户正常退出重进。
- `generated-storage.toml` 记录实际独立配置生成，默认16、范围1～16。素材检查记录13模型/9纹理、四原生配方及标签、4×5方向/档位组合；该证据不覆盖本次查出的 display 继承缺口。
- 初轮 JAR `build/libs/create_nuclear_industry-0.1.0.jar` 实测2,144,665字节，SHA-256 `FD3A507934AE6F39CA6A78CE37F988ED3C884EFAA7302099679B7152C0EC3978`。检查其入口注册相关类、新配置/storage 类、本批模型/纹理/配方/标签/loot 均已包含；38个本批变更/新增资源与该 JAR 逐字节一致。整改后旧哈希须保留原语境，不能作为修正后的资源打包证明。

## 实际使用的审查技能与不纳入判断的范围

实际读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md` 和 `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/requesting-code-review/SKILL.md` 及其 code-reviewer 模板。核对本仓库 Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，不照搬技能的新版本目录/接口。

已实际读取候选 AGENTS、治理5.1/5.2、STORE-01卡、已确认轻量方案及两份实施报告。按治理执行一轮联合审查与已有证据复用；没有派发代理、Git 写操作或修改实现，只写本报告。

按合同明确不纳入本次判断：旧版本存档迁移/兼容矩阵（治理5.2排除）；辐射、污染、温度、衰变热、再处理与动画（规格排除）；无关热端全量和既有日志/缓存（受影响范围及派发排除）。没有因这些范围扩展实现、测试或人工门。

客户端待验沿用 PM 的唯一联合清单：四原生生产配方、原新燃料工序、封存及切换、漏斗搬运、满架拒收/取出/拆除/正常退出重进、四向五档外观/碰撞、标准物品显示、中文护目镜与 JEI。只有 PM 可以据用户手测作最终验收。

## 唯一 P2 整改后的定点复核

状态：**P2 已闭合**。保留上文初轮问题及旧制品语境。本次只读整改资源、生成器的相关差异、两份报告追加段及重打包证据，没有重复审查全部库存逻辑、运行导出器、Gradle 或测试。

- `dry_storage_rack_0.json:1373` 已明确添加 `parent: minecraft:block/block`；`generate.py:178` 同步零档生成逻辑，新增父链检查读取锁定 Minecraft 1.21.1 客户端 JAR 的真实 `block/block.json`，不覆盖两个本批模型的 display。检查内容与原问题直接对应。
- `resource-checks.json.rack_item_display_chain` 记录 item→zero→vanilla 的实际父链。审查者直接读取同一客户端 JAR，核实 SHA-256 `499F6897D1837516680F3114072D8106E11C9ADCD933FE5CF051B551089B0C99` 与证据相符，且实际提供 gui、ground、fixed、thirdperson_righthand、firstperson_righthand、firstperson_lefthand 六个标准上下文；父链有效且本批模型没有 display 覆盖，原版显示变换现在能够继承。
- 已直接打开新 JAR，核实 item 的 parent 仍为本批零档模型，零档的 parent 为 `minecraft:block/block`；两项打包内容与源码逐字相同。新制品2,144,687字节，实算 SHA-256 `90A96EB4EE401FC56B529120A3A165A822CDAF747F2DBE3A9B49782F1B0E8FB1`，与 `resource-parent-repack-evidence.json` 相符。
- `resource-parent-repack.log` 明确 processResources 与 jar 实际执行，compileJava UP-TO-DATE，`BUILD SUCCESSFUL in 4s`；执行者记录 exit0。功能/测试源码没有晚于本批成功 GameTest 日志的新修改，整改范围为资源父链及生成/检查工具和报告，未改库存、配方、工时或载荷行为。因此继续复用首次8项 JUnit/6项 GameTest证据，无需重跑。
- `assets.md` 与 `implementation.md` 已追加修复事实和证据；旧尺寸/哈希留作原自动验证上下文，新哈希单独标明，未把资源打包成功写成人工门已通过。

本次定点复核未发现剩余实质问题。后续仍由 PM 交一张联合手测清单，待用户反馈后作验收。审查者已停止写入。
