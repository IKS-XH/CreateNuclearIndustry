# EXT-A-FUEL-02E 任务2素材交付（PM已看图冻结）

## 范围与依据

- 基线：02E卡片与已确认八格方案；Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280。
- 实际阅读 `AGENTS.md`、治理5.1、02E实施卡与方案；应用 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`。本任务仅制作资源，不运行Gradle或游戏客户端。
- 写入限于任务2允许的脚本、SVG/rig、十张新前缀PNG及游戏副本、16个静态parts模型、3个partial、物品合成模型、主/代理blockstate和本报告/证据。

## 模型与rig

- 北向以主控最小角为(0,0,0)，整机[0,32]³，`part=x+2*z+4*y`。八个parts各在局部0..16；主控`expanded=false`仍走02D旧模型，`expanded=true`走`part_0_off/on`；代理1..7按`facing,part,working`映射。
- 四朝向blockstate y分别为0/90/180/270。y90对应水平向量`(x,z)->(-z,x)`，北向part1东朝向在主控南侧；世界以主控中心(8,8)旋转，预览仅把旋转后的整机平移至画布中心。partial采用主控原点整机坐标，运行期由主控绘制一次。
- 底轴标在主控底面中心(8,0,8)，轴体仍由Create原生半轴呈现。左/右臂与中央夹具在静态八格中没有重复烘焙；pivot、轴、行程与扫掠范围见 `tools/art-assets/sources/fuel-02e/rig.json`。
- 基座、顶盖及立柱四角先移除方角体积，再用实际外露45°旋转棱柱封口；前侧两格贯通观察口能看到两臂和夹具。侧背外面使用中性接口纹理，不标特定原料槽。
- 物品模型把八格壳体与三活动件缩至0..16，带GUI、双手、地面和固定展示变换；物品parent保持指向该完整合成模型。

## 导出与核对

- 命令：`C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe -B tools/art-assets/fuel_02e_assets.py`。SVG仅在`--initialize-sources`时补缺失文件；默认从现有SVG读入，严格渲染16×16完全不透明RGBA，不改源稿。
- 从落盘JSON与导出PNG重新生成逐像素深度预览。已实际打开北/东/南/西四朝向等距、正/背/顶/底、物品及三个独立partial；可见外露削角、贯通观察区、四格顶盖与仅主控底轴标记，未见斜面后方方盒或分件错位。证据在 `EXT-A-FUEL-02E-assets/previews/`；`motion-envelope.png`是实际正面/顶面预览上的行程叠图，不代表本批运行时动作。
- 脚本检查：16个静态模型、3个partial、242个元素、主控16种与代理56种blockstate；所有state模型及纹理引用存在、UV在0..16、分块及旋转后的几何在0..16、物品合成及旋转后在0..16、partial在Minecraft允许的-16..32内；三活动件待机和端点对静态机架正体积碰撞数0。机器内运动建议行程左臂+X 0..1、右臂-X 0..1、夹具+Y 0..3；具体动作时序后续另批。
- 冻结前只读核对了两臂各2个端点与夹具Y=0/1/2/3的全部组合：臂对臂4组、各臂对夹具8组均无正体积相交。两臂在最内端分别与夹具左右面恰好接触（零体积交集），符合夹持预留；两臂之间无接触。该核对只说明几何空间，不实现动作时序。
- 游戏旧PNG枚举103张；`git ls-tree -r HEAD`也为103张，`git diff --name-only HEAD -- src/main/resources/assets/create_nuclear_industry/textures`为空，旧PNG逐张与HEAD字节一致。`git diff --check`对本批已跟踪文件通过。原02D `shielded_assembly_station_off/on`模型与旧五张PNG未改。
- 资源详情与103张摘要在 `EXT-A-FUEL-02E-assets/resource-check.json`。PM已实际看过四朝向等距、正/背/顶/底、物品与行程示意，并认可离线视觉和资源冻结。最终assemble及客户端人工验收尚待运行执行者与用户完成；离线预览不代替游戏内验收。
