# EXT-A-FUEL-02D 素材交付

状态：任务2资源已生成并冻结，待项目经理审查。

- 实际基线：HEAD `675e7a4`，隔离工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。
- 实际使用技能：`minecraft-modding/SKILL.md` 核对NeoForge/Minecraft资产边界；`minecraft-testing/SKILL.md` 确认静态资源证据不替代运行集成；`minecraft-resource-pack/SKILL.md` 应用1.21.1自定义元素、方块状态和UV约束。实际运行基线仍锁定Minecraft 1.21.1 / NeoForge 21.1.219 / Create 6.0.10-280。
- 资源：5张16×16不透明RGBA纹理，SVG源稿5份；共检查30个模型元素及8个朝向/工作状态组合。所有UV和元素坐标均在0..16，几何无零厚元素；模型纹理引用完整。
- 整改记录：首轮物品预览投影与相机方向不一致，且仅用面排序造成错误遮挡；现以一致的相机基向量、局部UV及逐像素深度重绘。首轮机身保留方盒角、基座漏上表面，并把整张顶/前纹理反复压到窄条；现已修正为真实削角、完整台阶顶面及对应位置的局部UV。
- 密闭罩主体的四个方角已实际移除：中央实体、四块止于斜角前的直面与四个旋转铅灰斜面相接。基座露出台阶，上表面完整；顶口位于连续顶面，底部仅画轴接口并由Create原生halfshaft渲染转轴。
- 保护：生成前枚举到98张已有游戏PNG，生成后SHA-256逐张一致；变化数0。完整记录见`EXT-A-FUEL-02D-assets/existing-game-png-sha256.json`。
- 离线预览逐像素使用模型JSON的实际顶点、局部UV及深度遮挡；实际打开检查正面停机/运行、背面、顶面进料口、底面动力接口和物品栏等距视角，纹理未在窄条上重复压缩成窗或顶口。
- 未运行Gradle或Minecraft客户端。

文件：

- 生成器：`tools/art-assets/fuel_02d_assets.py`；SVG源：`tools/art-assets/sources/fuel-02d/`。
- 游戏模型：`assets/create_nuclear_industry/models/block/shielded_assembly_station_{off,on,item}.json`、`models/item/shielded_assembly_station.json`、`blockstates/shielded_assembly_station.json`。
- 游戏纹理及导出副本：`textures/block/shielded_assembly_station_*.png`、`tools/art-assets/generated/block/`。
- 预览：`EXT-A-FUEL-02D-assets/previews/`；新资源SHA-256：`EXT-A-FUEL-02D-assets/new-assets-sha256.json`。
