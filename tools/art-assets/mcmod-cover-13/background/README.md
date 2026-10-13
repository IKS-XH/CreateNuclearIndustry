# ART13 最小反应堆透明背景

当前`reactor-5x5x5.png` 为R1的2048×2048 RGBA，供MC百科封面合成；实际方块/OBJ与纹理来自当前主工程，渲染库固定block-model-renderer 2.20.0、Three 0.180.0，资源语义Minecraft1.21.1。不是游戏截图，不代表服务端热工状态。初版原图/源/布局/来源/检查和说明保留在`history/pre-r1/`，原图SHA为c4493e3b…。

`layout.json` 包含125格完整布局（其中9格为空气）与30个固定动态显示partial。六列燃料×三格高度，顶端六换料口；中央三控制棒列为真实空气格，其驱动器在顶面。控制棒partial和辉光partial仅用于固定宣传显示。仪表在西面、冷热口在南面、正面北侧九格窗，侧面边角和底面都是壳体。该摆法按主工程`ReactorStructureDefinition`当前合法侧槽和列合同核对。

现有核电站工具默认尺寸正确，但其旧样例侧面换料口与燃料列壳体顶盖不能满足当前成型规则，本批在自己的场景内修正。仅复用其模型加载、OBJ解析、连接纹理映射与现成资源缓存，不修改旧工具。

本机可重现入口：用`C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe`运行本目录`server.mjs`，打开`http://127.0.0.1:4189/`，装配完成后自动保存本目录R1 PNG及渲染记录。`?mode=color-diagnostic`只做中性顶点色对照，不画液体，输出独立`diagnostic-neutral-color.png/json`，不覆盖正式PNG。需要后台运行时使用PowerShell `Start-Process -WindowStyle Hidden`。服务只绑定127.0.0.1；`/plant/`为主工程既有工具/资源的只读路由，唯一POST保存入口只写上述固定文件。没有新增依赖或全量资源准备。

用`C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe`运行本目录`verify.py`，会生成独立布局、来源hash与background范围的验证JSON。Pillow为现有bundled依赖。再次渲染会覆盖本批PNG；冻结后无需重跑。

`scene.mjs` 是可编辑场景，相机、灯光和125格布局与初版完全一致。R1先实际确认OBJ材质启用vertexColors且几何没有color，正式控制棒贴图本身为钢灰/铜色；本批仅给缺color的OBJ补中性白顶点乘数，保留原材质/贴图和正常灯光。对照实际渲染只改变窗内区域，钢灰棒体和铜带恢复，没有全亮白替换。实际燃料蓝辉使用原OBJ与原纹理，固定alpha0.65。

R1冷却液采用正式`create_nuclear_industry:fluid/compound_coolant_still`原生第0tick的16×16帧，整个内部包络三轴从1.01到3.99，包含燃料管周边间隙和中央控制棒槽。固定宣传满液alpha0.36、FrontSide、depthTest=true、depthWrite=false、renderOrder=2；蓝辉order=3，壳体和钢灰棒体保持正常深度。顶面和青蓝体积可见，不把透窗色片或中央空气槽当成整堆冷却液。没有推导库存、功率、流量或运动。前窗仍为真实CT连续3×3边框。

`sources.json` 记录实际读取和核查的72条来源，包括项目原创模型/贴图、原Create/Minecraft所需父模型、只读适配器、真实库材质入口及液体参考。实际读入hash在本次检查时一致。依赖原资源的许可证和归属保持：block-model-renderer为MPL-2.0；主项目、Create和Minecraft的素材仍分别属于各自来源，不将其宣称为本批原创模型。不分发系统字体或新的第三方包。

R1 PNG SHA256：`3254b7728b5f3b085cca8600d3d1197d2567ba96a7db73a4a4005d7ed0436bb0`。
