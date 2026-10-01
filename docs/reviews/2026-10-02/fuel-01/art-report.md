# EXT-ART-08 美术交付记录

本批新增铀尾矿、低浓缩铀粉、贫化铀粉、密闭料浆桶、铀尾矿砖、铀料浆静止/流动纹理，以及离心机单格模型和六面贴图。铀精矿继续复用已有 SVG、PNG 和物品模型，没有改写旧素材，也没有增加离心机半成品。

离心机模型默认正面朝北：北面显示状态窗与转子观察窗，南面显示动力轴；面向北侧正面时，东面是左侧料浆接口、西面是右侧水接口。顶面封闭，底面显示两个粉末出口。`facing` 四向状态使用方块模型旋转，旋转后接口随机器一同转向。功能执行者已确认模型键和朝向，GUI 不使用专用纹理。

导出安装新增 13 张 PNG，游戏纹理总数为 82。69张开工 PNG 的版本化 SHA-256 快照作为本批验收证据，存放在[`EXT-ART-08-existing-game-png-sha256.json`](../../../../tools/art-assets/baselines/EXT-ART-08-existing-game-png-sha256.json)；它不限制以后通过白名单的合法素材更新。快照比对结果和本批新图哈希见 [`verification.json`](./art/verification.json)；原始预览见 [`preview.png`](../../../../tools/art-assets/preview.png)、[`centrifuge-model-faces.png`](./art/centrifuge-model-faces.png) 和 [`preview.html`](../../../../tools/art-assets/preview.html)。

实际使用工作区自带 Python 运行时执行 `tools/art-assets/export.py --install`：导出器报告 `PASS: 83 generated PNG; 82 game targets`。随后逐项核对全部69张旧图的 SHA-256、13张新图的尺寸与导出结果、82张游戏 PNG 路径集合、方块模型六面纹理引用和四向 blockstate；结果均通过。另将尾矿 SVG 中一个合法色值由 `#75614B` 改为同色板内的 `#C0A47B`，用完整工作区 Python 路径执行一次 `export.py --install`，观察已安装尾矿 PNG 哈希改变；恢复 SVG 原始字节后再执行同一命令，已安装及 generated PNG 均恢复原始字节，69张旧图哈希仍一致。两次命令均返回退出码0，详见 [`verification.json`](./art/verification.json)。已查看贴图总览和离心机六面预览。

本任务实际读取并应用 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack` 技能。资源工作按现有1.21.1命名空间、16×16材质、16×64流体流动贴图及模型/方块状态 JSON 接入；本批哈希核对是验收证据，导出器仍允许未来合法 SVG 更新。未启动 Gradle 或 Minecraft，也未运行验证器内已有的历史 SVG 负例序列。客户端中的实际外观、流体动画和资源加载仍待人工验收。
