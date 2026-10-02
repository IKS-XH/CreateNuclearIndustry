# EXT-A-FUEL-02A 素材与模型交付

本批交付三项资源：`green_fuel_pellet`、`sintered_fuel_pellet`、`fuel_sintering_furnace`。新增7张16×16 RGBA贴图（两张芯块、炉体钢壳/黄铜/耐火材料/冷热观察窗）及7张可编辑16×16 SVG源稿。两芯块使用生成物品模型，炉体使用完整三维物品模型和冷/热两份静态方块模型；方块状态覆盖四向 `facing` 与 `lit=false/true` 共8种组合。

炉体为单格内的矮八棱壳体，使用钢灰面、两道黄铜箍、耐火顶衬、正面窄观察窗及顶/底接口。`lit`仅切换暗窗/亮窗静态纹理。完整物品模型含58个元素，旋转后范围为X=1..15、Y=0..11.75、Z=0.88..15，未越出方块空间；GUI缩放0.95，第一/第三人称分别0.4/0.375。

## 检查结果

- 专用生成器调用既有严格 `render_svg`，只接受本批白名单色和直属整数矩形SVG；实际生成的7张源稿全部通过渲染。普通导出只读取SVG，不会覆盖源稿；`--initialize-sources`仅在源稿缺失时创建，跳过已存在文件。
- 重新读取磁盘上的方块模型与方块状态JSON后校验：8个状态完整，所有材质与三项物品模型引用均可解析；7张PNG为16×16 RGBA；3份炉体模型每份338个显式UV面，隐式UV=0、越过0..16 UV=0。元素旋转只用0°与±45°；方块状态方向仍使用0/90/180/270°旋转。（PM归档时按检查JSON将原“共338”文字更正为“每份338”，未改资源或检查结果。）
- 修正了初版模型中的真实接合空段：钢制底座原止于Y=1.2，黄铜底箍从Y=1.8开始，导致0.6模型单位的侧面断层。底座现延伸至Y=1.8并与黄铜箍衔接。对落盘JSON的12个底座、侧壳、黄铜圈、耐火顶盖和接口接合高度分别采样4096条周向射线，合计49,152个探点均无未覆盖点；预览仍可能在斜边产生细像素线，这套离线光栅器不代表Minecraft客户端最终渲染。
- 物品预览按浅色和深色背景各展示两枚芯块；两张炉体预览直接读取落盘JSON，分别展示顶口与底部供热接口。离线几何预览不代替客户端光照、贴图投影或视觉验收。
- 生成时未写旧离心机纹理、旧模型、共用导出器、清单或全局色板；旧离心机资源无工作树差异。本批不涉及Java/配方，也未运行Gradle。

## 证据与复现

- 检查明细：[asset-check.json](./EXT-A-FUEL-02A-assets/asset-check.json)
- 芯块浅色/深色预览：[fuel-pellet-preview.png](./EXT-A-FUEL-02A-assets/fuel-pellet-preview.png)
- 顶口角度：[furnace-north-top-angle.png](./EXT-A-FUEL-02A-assets/furnace-north-top-angle.png)
- 底部接口角度：[furnace-bottom-angle.png](./EXT-A-FUEL-02A-assets/furnace-bottom-angle.png)
- 实际技能：`minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`；结合治理5.1仅执行本批资源、引用和几何检查，未运行无关JUnit/GameTest或全量素材管线。
- Windows复现命令（使用Python `-B`避免pycache）：

```powershell
& 'C:\Users\IKSXH\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' -B tools\art-assets\fuel_02a_assets.py
```

需初始化缺失源稿时可显式执行 `fuel_02a_assets.py --initialize-sources`；它不会替换已有源稿。
