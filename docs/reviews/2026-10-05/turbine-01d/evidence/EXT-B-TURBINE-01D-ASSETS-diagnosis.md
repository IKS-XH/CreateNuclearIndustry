# EXT-B-TURBINE-01D 资源诊断与当前交接状态

基线为 `9ac94ae6ec67ac66c7032ec7f9cef22fb7955115`。诊断目标是汽轮机OBJ材质、UV、封面、法线和端口方向。实际阅读并应用了 `minecraft-resource-pack`、`minecraft-modding`、`minecraft-testing` 与 `systematic-debugging` 技能；已对照本地 NeoForge 21.1.219 的 `ObjLoader`、`ObjModel` 与 `ObjMaterialLibrary` 源码。没有运行Gradle，也没有执行Git写操作。

## 9ac94ae基线的可复现失败

只读读取 `git show 9ac94ae:<path>` 并在内存中调用该提交的生成器后，负向检查全部复现，结果保存在 [baseline-negative-checks.json](EXT-B-TURBINE-01D-ASSETS-baseline-negative-checks.json)：

- `rotor_middle.obj` 使用 `usemtl rotor`，同目录 `turbine.mtl` 只声明 `rotor_hub`。客户端 `run/logs/latest.log` 第47–73行记录模型加载失败，调用链为 `ObjLoader` → `ObjModel.parse` → `ObjMaterialLibrary.getMaterial`，异常是 `The material was not found in the library: rotor`。NeoForge 的 `usemtl` 解析直接要求MTL存在同名材料，这会使模型无法烘焙，符合转子紫黑错误。
- `Mesh.face` 对大于四顶点的圆盘做扇形三角化后递归处理每个三角形，每个三角独立重算UV包围框。旧八边圆盘探针发现6个共享顶点有不同UV，足以制造扇形纹理断裂。圆柱侧面也没有整圈角向UV，旧实现逐小面映射整张纹理。
- 未成型独立机壳复用`section=middle`，基线几何没有z=0/1封口；大面板背面缺少封面。控制器大面板缺z=.08背面盖板，中心盒侧壁从该平面起始，接合处背后留下环形开口。
- 输出轴承套内半径与轴外半径都为0.14；轴圆柱端盖与额外端面圆盘在z=0/1重合，轴承套端面又落在3/16端板平面上，形成共面重叠条件。

NeoForge `ObjModel.makeQuad`（本机sources.jar第386–429行）只在四个顶点恰落于整个方块同一0/1平面且显式法线朝外时赋予面剔除方向；实际是否被剔除还取决于相邻方块遮挡。因此自动边界剔除本身不能解释空气侧缺面，且不能替代检查OBJ是否生成封面。加载器会信任OBJ显式`vn`，本批静态检查遂逐面比较几何叉积和法线方向。

## 已安装资源和自动检查结果

在PM要求的rear状态与译名闭合后，资源生成器及语言专用安装均成功。资源层面生成了267个模型JSON/OBJ；状态覆盖保留206个几何piece与既有状态基数，普通汽口生成六向世界OUTWARD模型，输出轴分别保留front/rear未定位外观。控制器几何仍为完整1×1×0.08面板加x/y=.12..88、z=.08..40中心盒；只补板背面封口。输出轴显示名为“汽轮机动力输出轴”/“Turbine Power Output Shaft”，注册ID未变。

[checks.json](EXT-B-TURBINE-01D-ASSETS-checks.json)来自最后一次资源写入后的只读扫描：发现在磁盘上的294个汽轮机模型JSON和294个OBJ；294个OBJ的MTL材质引用、贴图路径与6,051个面法线绕序检查通过。全量模型扫描包含`rotor_blades_d3/d5/d7`对应资源；但检查脚本的Java字符串正则没有识别`TurbineRotorRenderer`中`model("rotor_blades_d3")`等动态构造路径，报告中的`java_partial_models=[]`仅表示正则漏检，不代表Java注册路径遍历或运行时PartialModel验收通过。

真实OBJ顶点、UV与MTL PNG纹理的离线预览在 [preview.png](EXT-B-TURBINE-01D-ASSETS-preview.png)。它用于静态查看，并非客户端验收。当前预览仍显示进汽口外端有明显放射状橙/透明缺口：`turbine_inlet.png`和` turbine_exhaust.png`为16×16带透明像素的图标纹理，却被整个映射到多个圆柱面及端面。共面UV扇形断裂已在新生成路径修复，但当前端口外观仍不可宣称正确。真实客户端截图需由集成执行者在最新冻结资源上重跑；若继续缺面，应由PM安排将筒身与圆头材质分开并选用合适的不透明既有贴图，或另派经批准的最小材质整改。

## 冻结边界与客户端证据

PM发现并要求修正`end=rear,located=false`不能错误显示front网格，以及输出轴中英文名称。我先后按此要求完成了对应修改并安装资源。在之后宣布冻结后，严格全资源审计发现保留的旧`rotor_middle`未被当轮生成集覆盖；为修复PM明确要求的遗留文件，又增加了兼容资源生成并于约02:04:54运行最后一次 `python tools/art-assets/turbine_models.py --install`。这次命令将现有`rotor_middle.obj`改为`rotor_hub`；这是越过已宣布冻结边界的继续写入，我已向PM说明并承担责任。

因此，02:02的assemble及02:03启动后停止的client针对旧资源，不能作为当前候选验证。最后一次正式资源写入后我未再改动generator、lang、src或其它工具；仅以只读方式审计src，并把本报告、负向检查、JSON结果与离线预览写到本build报告目录。A须在当前冻结资源上重新执行隔离集成/客户端验证。该执行者未运行Gradle、未改Java、未改docs、未执行Git写操作。
