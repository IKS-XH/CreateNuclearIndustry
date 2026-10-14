# EXT-A-FUEL-01E 模型整改证据

## 根因与原版源码

候选锁定 Minecraft 1.21.1 / NeoForge 21.1.219。使用本机 NeoForm 缓存中的映射源包核对模型烘焙实现：`C:/Users/IKSXH/.gradle/caches/neoformruntime/intermediate_results/sourcesWithNeoForge_751f4ca44c50f024d6929f99f0119fcf429717cc_output.zip`。对应流水线记录 `sourcesWithNeoForge_751f4ca44c50f024d6929f99f0119fcf429717cc.txt` 引用 `minecraft_1.21.1_version_manifest.json`、`neoform/1.21.1-20240808.144430` 与 `neoforge/21.1.219`。

- `BlockElement.java:51-55` 在构造时运行 `fillUvs()`，对每个面调用 `uvsByFace()`，然后传入 `BlockFaceUV.setMissingUv()`。
- `BlockElement.java:58-72` 按方向从元素 `from/to` 生成默认UV；侧面V坐标包含 `16 - to.y` / `16 - from.y`。
- `BlockFaceUV.java:51-55` 仅在JSON未提供UV时写入这组默认值。
- `FaceBakery.java:111-126,158-165` 烘焙四个顶点时用默认/显式UV除以16并交给 `TextureAtlasSprite.getU/getV`。`TextureAtlasSprite.java:68-71,86-89` 对sprite的U/V区间线性映射，越界输入会外推到精灵范围外。

因此整机模型将上段坐标整体加16后，默认侧面V落为负数；跨格转子纵向Y=5..27也超出单张16×16贴图的合法坐标。

## 修前与修后检查

修前证据保存在 `EXT-A-FUEL-01E-assets/pre-fix-diagnostics.json`，由检查器读取磁盘中的候选JSON获得：完整物品模型344个面中168个默认UV越出0..16；独立转子138个面中76个越界。lower的92面与upper的114面虽然都是隐式UV且未越界，也一并写成等价的显式原版UV。

修后检查器读取实际生成的四个JSON模型并覆盖每个面：lower 92/92、upper 114/114、item 344/344、rotor 138/138均有显式UV；隐式UV为0，越界UV为0。转子纵向坐标按整根转轴Y=5..27线性映射进0..16；整机上段沿用自身单方块模型UV。检查还重新读取物品模型显示变换，确认GUI 0.45、ground 0.32、fixed 0.35、第三人称手持0.38、第一人称手持0.38未变。

## 壳体接缝

旧正八边形面板按中线半径7.0和长度5.8建模，但面板有0.4厚度；外侧可见平面在半径7.2处，面板长度未覆盖该平面的八边形角点。检查器读取lower模型y=13..16与upper模型y=0..3的真实旋转后外侧面，按方位角4096条射线检查覆盖：修前两处各有96条射线无外壳覆盖，最大连通缺口1.054688度。

生成器现在按外平面半径计算弦长并额外搭接0.02模型单位。修后两个接缝处均找到8个朝外、非退化的外侧面；4096条射线无缺口，最小搭接余量0.016468模型单位，相邻面法线夹角45度且无`cullface`。因此搭接封边不形成共面z-fighting；观察孔与转子结构保持原几何。

## 资源范围与产物

修改项仅为`tools/art-assets/centrifuge_01d_preview.py`及四个本离心机模型JSON；其他并行运行线文件未触碰。四张PNG未重导出，SHA-256：

- casing: `f6d76a76e5193114a91d2c3ccfcde08787e324192e78bca6c2ff65d61218328d`
- brass: `00675e6d11ea472cee61ec2ae598e11ca413a7d9879a7ca7c054dfd9dc2566ce`
- glass: `4d5ae6f32ab8c6369aa26f5dc863d2c8bc13f1d284fb3f6a8830ef08cef40b54`
- dark: `537b2e358b295cd1ca4fc364e4f93a84ac6498db274d40b7d5a8ad73dc6fbefd`

运行 `python -B tools/art-assets/centrifuge_01d_preview.py --diagnose-only` 留存修前证据；再运行 `python -B tools/art-assets/centrifuge_01d_preview.py --models-only` 生成模型、UV范围检查、周向覆盖检查与两张几何预览。没有运行Gradle或Minecraft。几何预览只显示形状与近似材质色，不声称它采样了PNG；纹理安全由原版UV公式复算、所有面显式0..16范围检查证明。最终预览：`assets/centrifuge-final-front.png`、`assets/centrifuge-final-side.png`；汇总数据：`assets/geometry-check.json`。

实际使用技能：`minecraft-modding/SKILL.md`（按MC1.21.1与NeoForge 21.1.219核对资源调用边界）；`minecraft-testing/SKILL.md`（只对本批资源执行定向离线检查，不将其宣称为客户端验证）；`minecraft-resource-pack/SKILL.md`（模型UV、1.21.x元素边界与PNG规格）；`systematic-debugging/SKILL.md`（先复现和查源再修复）。
