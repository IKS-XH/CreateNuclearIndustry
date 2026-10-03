# EXT-B-BOILER-01A 素材与配方交付

- 基线：`8bf423d`，候选工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`
- 技能：`minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack` 均已读取；本任务只提交原生资源、素材和离线静态证据。
- 内容：七个锅炉方块的 blockstate、块/物品模型、12张PNG纹理和对应SVG；超临界蒸汽 still/flow 贴图；七条配方、七张单件掉落表；镐挖掘与铁工具标签追加。
- 流体图集：`assets/minecraft/atlases/blocks.json`登记超临界蒸汽still/flow两项，并保留原铀浆料两项。
- 预览：[锅炉纹理与轮廓示意](./EXT-B-BOILER-01A-ASSETS/boiler-block-assets-preview.png)只是正面纹理和方块剪影参考，不是游戏模型渲染；导出清单：[assets.json](./EXT-B-BOILER-01A-ASSETS/assets.json)。
- 导出：`C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe tools/art-assets/boiler_01a_assets.py`。
- 静态核查：脚本检查本批JSON语法、水平朝向、模型/纹理引用、元素边界/体积交叠/外表面共面重复、显式UV范围、PNG规格、六条工作台配方材料与数量、控制器5×5格数/禁镜像及七张单件掉落表；错误数：0。
- 未运行 Gradle、客户端、服务端或 Create 实际配方加载。游戏内透明层表现、方块朝向、掉落及真实工作台/动力合成加载仍待设备执行者接线与人工验收。
- 控制器普通掉落仅一件，不含库存NBT；库存快照由设备实现接入。

