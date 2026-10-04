# EXT-B-BOILER-01B A：锅炉外观与强化钢板配方

- 基线：`9e8690b`；候选：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。执行时未进行任何Git写操作。
- 已读取技能：`minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`。本执行者只改任务A的SVG、锅炉纹理、七方块模型、锅炉专用素材生成器和强化钢板配方；未改Java/测试、换热器模型或01A以外资源。
- 外观：色板复用当前反应堆的蓝灰钢板、浅钢边、黄铜角铆钉；锅炉壳顶和换热段顶的导出PNG均按水平及垂直两轴逐像素镜像。窗口模型声明`render_type: translucent`，保持四个嵌入式玻璃片和既有窗口尺寸/朝向/物品父模型。
- 强化钢板：原配方ID、过渡件ID、钢板标签输入、成品ID、单轮1件均保留；当前序列为`create:deploying`加1坚固板，随后`create:pressing`，不再引用精密构件。
- 预览：[反应堆/核换热器与锅炉纹理同图对照](./EXT-B-BOILER-01B-ASSETS/boiler-reactor-style-comparison.png)按相同像素放大比例展示参考与锅炉素材；它是纹理样本，不能替代Minecraft实际模型渲染。生成文件及SHA-256见[assets.json](./EXT-B-BOILER-01B-ASSETS/assets.json)。
- 可复现导出：`C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe tools/art-assets/boiler_01a_assets.py`。此入口只写A范围中的十张锅炉SVG/PNG、七方块块/物品模型、强化钢板配方及01B证据，不会重写锅炉旧配方、蒸汽纹理/图集或标签。
- 本地静态检查：0个错误；覆盖JSON与引用、7个方块模型/物品模型、模型元素边界/相交/外表面共面、PNG尺寸、窗口半透明层与四面嵌入结构、顶面双轴镜像、两工序材料/数量/副产物与精密构件移除。
- 差异核查：A写集`git diff --check`通过；以`9e8690b`为基线核对旧锅炉配方、蒸汽纹理/SVG、流体图集、标签和掉落表，差异为空。其他执行者与用户已有的工作区改动均保留。
- Create `6.0.10-280`原生进度边界（静态源码核对，未加载游戏）：过渡件保存原生配方ID和整数`step`，不是已解析工序快照；旧工序数为3，新工序数为2。同配方ID下，旧step=0或1可依次映射到新序列；旧step=2会按`step % sequence.size()`回到新序列第0步，并可能在下一次操作后完成而未经过压片。旧step=2表示坚固板和精密构件步骤已执行，不会免付坚固板，但既有在途件的最终压片语义不能证明兼容。候选档案不做自动迁移；该边界需由PM安排手测/清空或补兼容策略后再宣称旧在途件安全。
- 未运行Gradle、JUnit、GameTest或客户端；窗口在真实世界中的透视仍待D/用户客户端复测。D继续负责Java/结构/管线范围；本报告不处理新提出的换热器定向与共享库存设计。
