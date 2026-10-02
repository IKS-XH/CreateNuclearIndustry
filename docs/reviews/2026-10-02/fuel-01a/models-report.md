# EXT-A-FUEL-01A 模型资源整改

本次基线为候选 `6b2138a4dd7c54954e4dc0a42e8d3a4e637687ec`。用户截图及 `run/logs/latest.log` 指向两个资源根因：日志报缺少 `models/item/uranium_concentrate.json`，但精矿 PNG 存在；离心机 item 仅继承一个没有 `parent` 或 `display` 的自定义 block 模型，因此 GUI 使用单面正投影，手持时又没有标准方块缩放。

新增精矿 item 模型，使用 `minecraft:item/generated` 和既有精矿 PNG。离心机 block 模型现在继承 `minecraft:block/block` 的标准方块展示定义；其 item 模型继续继承此 block 模型，因此直接共享原版变换。模型仍为 `[0,0,0]` 至 `[16,16,16]` 的完整单格方块，六面纹理和 `facing` 方块状态未变。

父链已与锁定资源核对：原版 `cube_all → cube → block`，Create 6.0.10-280 的安山岩外壳 `item → block → minecraft:block/cube_all → cube → block`。原版 `minecraft:block/block` 提供 GUI 旋转 `[30,225,0]`、GUI 缩放 `0.625`、第三人称右手缩放 `0.375`、第一人称双手缩放 `0.4`，同时保留掉落与固定展示比例；本次没有自行定义新的变换。精确父链、显示参数、资源引用与文件哈希见 [`verification.json`](./models/verification.json)。

验证只解析本任务三个模型 JSON、既有方块状态、贴图存在性和六面引用，并从本机 Minecraft 1.21.1 客户端 JAR、Create 6.0.10-280 JAR 读取父模型定义。全部静态检查通过。按本任务范围未运行 Gradle 或 Minecraft；实际 GUI 与手持比例仍需客户端复测。

实际读取并应用 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack` 与 `systematic-debugging` 技能。实现限于三个指定模型路径；本次实际新增/修改文件为精矿物品模型和离心机方块模型，离心机物品模型已经正确继承 block 模型，无需改写。
