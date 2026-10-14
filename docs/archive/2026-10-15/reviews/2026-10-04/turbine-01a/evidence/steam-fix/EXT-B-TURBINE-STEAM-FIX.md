# EXT-B-TURBINE-STEAM-FIX 交付报告

- 候选基线：`01c816f`（`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`）。
- 根因：`TurbineContent.java` 的 `initializeClient` 引用 `create_nuclear_industry:fluid/steam_still` 与 `...:fluid/steam_flow`（第72–82行），两张 PNG 位于 `textures/fluid/` 且存在；但原 `assets/minecraft/atlases/blocks.json` 只登记矿浆和超临界蒸汽共四张图，未登记普通蒸汽，故流体引用无法在方块图集中解析，表现为缺失材质。运行日志包含 2026-10-04 22:24 的客户端启动及 22:49 正常关闭，没有普通蒸汽或图集错误记录；此日志不推翻资源清单中的直接缺项证据。
- 改动：仅在 `assets/minecraft/atlases/blocks.json` 追加两条现有普通蒸汽纹理；中文流体名改为“蒸汽”，中英文汽轮机排出库存文案改为明确的“排出蒸汽 / exhaust steam”。ID、FluidType、PNG、超临界蒸汽名称和交易逻辑未改。
- 技能：实际读取并应用 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack` 与 `systematic-debugging` 技能；技术版本按任务卡记录的 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280 核对。
- 静态检查：atlas 与两份语言 JSON 均可解析；两 PNG 均解码为 16×16；原四个 atlas 来源均保留并新增两个 steam 来源；储罐文案每种语言均保留四个 `%s` 占位符。
- 资源处理：执行 `./gradlew.bat processResources --console=plain`，退出码 `0`，`BUILD SUCCESSFUL`。打包输出中的 atlas、两份语言文件及两张 PNG 均存在，五个文件与源资源 SHA-256 一致。原始日志与退出码见同目录 `EXT-B-TURBINE-STEAM-FIX-processResources.log`、`.exit`。
- 边界：未启动客户端或世界，实际储罐/透明管纹理及护目镜名称仍待合并人工复测。前端轴“无法出动力”本轮未复现或定位；既有 Create 双轴探针仅证明其测试布局中的容量发布与生命周期，不证明用户本次具体搭建条件。
