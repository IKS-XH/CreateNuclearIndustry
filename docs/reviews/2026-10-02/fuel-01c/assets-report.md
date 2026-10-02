# EXT-A-FUEL-01C 资源线交付

按资源线唯一写集更新了铀料浆桶模型与离心机方块面映射。

- 铀料浆桶现在使用 `neoforge:fluid_container`、`neoforge:item/bucket` 和 `create_nuclear_industry:uranium_slurry`。NeoForge 21.1.219 随附模型父链以 `item/bucket` 作为原版桶身贴图，并用 `neoforge:item/mask/bucket_fluid` 遮罩流体层；动态模型按物品所含流体选取其 still texture。因此桶轮廓和桶身材质保持原版，内容区显示铀料浆。注册代码仍是 `BucketItem`，料浆桶/源流体绑定未变。
- 离心机 `north` 保留正面观察窗；`down` 改用原后侧轴承纹理；`south/east/west/up` 及 `particle` 统一使用原底部中性双槽接口纹理。方块模型父项与元素几何、离心机物品模型及其显示变换未改。
- 专用料浆桶 PNG/SVG 和导出管线未改；物品模型不再引用专用 PNG，SVG 保留作既有源稿。

静态核对通过：两个目标 JSON 均可解析；桶模型的 loader、parent、fluid 与锁定版本现有冷却剂桶范例一致（流体 ID 按本桶注册替换）；机器六面及粒子贴图映射与卡片相符；料浆注册仍由原 `BucketItem`/`BaseFlowingFluid` 连接。实际读取并应用 `minecraft-modding` 技能中的版本核验要求、`minecraft-resource-pack` 技能中的模型资源约定，并读取 `minecraft-testing` 技能确认自动/运行时测试适用范围；本资源线仅改静态模型且卡片明确禁止运行 Gradle/测试/游戏，因此只做静态 JSON 与字段核对，不新增或启动测试。逐项结果见[静态记录](./assets/verification.json)。

依资源线卡片限制，未运行 Gradle、测试或游戏。本交付供运行线统一打包和验证。