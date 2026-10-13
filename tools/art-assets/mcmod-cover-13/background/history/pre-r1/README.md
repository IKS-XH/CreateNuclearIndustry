# ART13 最小反应堆透明背景

`reactor-5x5x5.png` 为2048×2048 RGBA，供MC百科封面合成；实际方块/OBJ与纹理来自当前主工程，渲染库固定block-model-renderer 2.20.0、Three 0.180.0，资源语义Minecraft1.21.1。不是游戏截图，不代表服务端热工状态。

`layout.json` 包含125格完整布局（其中9格为空气）与30个固定动态显示partial。六列燃料×三格高度，顶端六换料口；中央三控制棒列为真实空气格，其驱动器在顶面。控制棒partial和辉光partial仅用于固定宣传显示。仪表在西面、冷热口在南面、正面北侧九格窗，侧面边角和底面都是壳体。该摆法按主工程`ReactorStructureDefinition`当前合法侧槽和列合同核对。

现有核电站工具默认尺寸正确，但其旧样例侧面换料口与燃料列壳体顶盖不能满足当前成型规则，本批在自己的场景内修正。仅复用其模型加载、OBJ解析、连接纹理映射与现成资源缓存，不修改旧工具。

本机可重现入口：用`C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe`运行本目录`server.mjs`，打开`http://127.0.0.1:4189/`，装配完成后自动保存本目录PNG及渲染记录。需要后台运行时使用PowerShell `Start-Process -WindowStyle Hidden`。服务只绑定127.0.0.1；`/plant/`为主工程既有工具/资源的只读路由，唯一POST保存入口只写本批两个固定文件。没有新增依赖或全量资源准备。

用`C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe`运行本目录`verify.py`，会生成独立布局、来源hash与background范围的验证JSON。Pillow为现有bundled依赖。再次渲染会覆盖本批PNG；冻结后无需重跑。

`scene.mjs` 是可编辑场景，相机、灯光和固定辉光alpha在其中。实际燃料蓝辉使用原OBJ与原纹理，固定alpha0.65，发光材质只映射原纹理，没有手画几何或替代材质。未模拟冷却液、功率或运动。前窗以真实CT图集显示连续3×3边框。

`sources.json` 记录实际读取的68条来源，包括项目原创模型/贴图、原Create/Minecraft所需父模型和只读适配器。实际读入hash在本次检查时一致。依赖原资源的许可证和归属保持：block-model-renderer为MPL-2.0；主项目、Create和Minecraft的素材仍分别属于各自来源，不将其宣称为本批原创模型。不分发系统字体或新的第三方包。

PNG SHA256：`c4493e3b6953478e0361aa3afb39c92a1be9ff8da598778eff8d9c39bebca249`。
