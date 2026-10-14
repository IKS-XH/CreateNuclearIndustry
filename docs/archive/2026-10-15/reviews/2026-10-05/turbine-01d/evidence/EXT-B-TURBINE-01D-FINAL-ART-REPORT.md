# EXT-B-TURBINE-01D 最终素材交付（C）

状态：**资源与生成器已冻结，交还 PM 安排 A 执行最终资源打包检查。** 本报告只说明静态资源检查；不代表游戏画面人工验收。基线 `9ac94ae6ec67ac66c7032ec7f9cef22fb7955115`，候选 `Create_NuclearIndustry-ore-acquisition`。

## 修复与源路径

- `tools/art-assets/turbine_models.py` 是 OBJ、MTL 和新增 8 张实体 PNG 的可复现生成源。实体 PNG 由既有汽轮机图标合成不透明底色，其中进、排汽侧纹使用同配色钢管条纹；原有物品图标 PNG 未覆盖，观察窗玻璃仍使用原透明纹理。输出在 `src/main/resources/assets/create_nuclear_industry/textures/block/turbine/*_surface.png` 与 `turbine_{inlet,exhaust}_{face,side}.png`。
- `Mesh.face` 翻转三角面绕序时同步翻转 UV，`transform_mesh` 及未成型转子缩放保留逐顶点 UV。圆端面经世界方向转换后不再逐片重映射。进、排汽口内端在方孔边界内加封面，背视不再透空；旧 `inlet_top`/`exhaust_top` OBJ 一并由生成器维护。
- 保留 B 已完成的机壳轴向封口、控制器背盖、前后轴承径向净空、`rotor_middle` 材质修复、六世界向模型和动力输出轴名称。

## 验证证据

- 只读旧基线负例：`../EXT-B-TURBINE-01D-ASSETS/EXT-B-TURBINE-01D-ASSETS-baseline-negative-checks.json` 用 `git show` 提取 `9ac94ae`，确认旧 `rotor` 缺材质、圆端 UV 断裂、独立薄壳和控制器缺盖、轴承内壁与轴身同半径及重复端盖。C 安装前运行增强检查器，实际在旧 `support` 图标透明像素处失败。
- 最终命令：`python -B tools/art-assets/turbine_models.py --install` 与 `python -B tools/art-assets/turbine_01d_checks.py`，均退出 0；使用 Codex bundled Python。原始生成结果在 `model-generation.json`，审计结果在 `EXT-B-TURBINE-01D-FINAL-ART-checks.json`。
- 检查器校验生成源与正式文件字节一致；扫描仍存在的全部 **294** 组 OBJ/包装 JSON、MTL 与纹理路径、**6075** 个面的每顶点法线和绕序；**5967** 个实体面使用不透明贴图，玻璃透明除外；确认圆端共享顶点 UV 连续、端口内侧封面、轴身与轴承内壁径向净空，以及动态 `rotor_blades_d3/d5/d7` 显式引用。生成器自身校验 267 个主模型 JSON、269 个 OBJ（含两份旧顶部端口 OBJ）、7 个物品模型与方块状态。
- `EXT-B-TURBINE-01D-FINAL-ART-preview.png` 从正式 OBJ 的逐顶点 UV/法线及 MTL PNG 绘制，并做背面剔除，展示进汽口正反/侧下、排汽口上端、独立壳上下、控制器正反/下侧和前后轴承/侧下。此图是离线核对，不是 NeoForge 客户端截图。

## 边界与交还

未运行 Gradle、GameTest 或客户端；A 的先前 9/9 GameTest 是本轮既有证据，不记作 C 重跑。B 的原 `EXT-B-TURBINE-01D-ASSETS-preview.png` 和检查 JSON 曾在本次中间审计被重新生成，历史失败依据以旧基线负例 JSON、PM 已保存的截图和本报告为准。最终证据已单独写入 `EXT-B-TURBINE-01D-FINAL-ART`。只读 `git diff --check` 发现 `logs/debug.log` 与 `logs/latest.log` 两处尾随空白；两者不在素材写集，未触碰。自本报告起不再执行 `--install` 或修改正式资源。

实际读取并应用：`minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`、`systematic-debugging`；依据仓库 AGENTS 与治理协议 5.1 选择局部资源检查。版本维持 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280。
