# ART-ACCIDENT-CORIUM-22 真实资源预览交接

2026-10-23。执行者仅交预览和限定CLI证据，不登记Minecraft视觉/事故业务/创造拆除验收。

## 服务与范围

- 地址：<http://127.0.0.1:4210>，绑定127.0.0.1；前台exec **session 68748 / PID 80084**。开工窄查端口无占用，旧服务未动。
- 唯一树：`E:/MyMC/NewMod/Create_NuclearIndustry-accident`，实际只读核分支`codex/accident-thermal-04b`、HEAD `1985d1b3172a5efd1f0c87f3aa39ce3fe2a6feb7`。
- 仅写`tools/art-assets/accident-corium-22/preview/`、本报告目录和`build/reports/art/ART-ACCIDENT-CORIUM-22/preview/`。无生产/共享工具/Java/任务状态/Git/世界/根包/日志写入，无Gradle/test/jar、安装或外部库修改。
- 实际读取主工程AGENTS、治理1.2/5.1/6、STATUS、联合卡、04B及创造拆除卡和事故树本批卡。三技能真实入口为`C:/Users/IKSXH/.codex/skills/{minecraft-resource-pack,minecraft-modding,minecraft-testing}/SKILL.md`，分别用于模型/PNG/mcmeta、客户端边界及CLI/浏览器/游戏证据分离。核实际Gradle锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。旧事故树许可字段不覆盖本批主工程合同；原创预览MIT，第三方原许可保持。

## 真实输入与显示

仅读事故树`src/main/resources/assets/create_nuclear_industry/`以下15路径，实际SHA/字节在原始CLI记录及浏览器输入快照中：

| 路径组 | 实际消费 |
| --- | --- |
| `blockstates/reactor_corium.json` | 真实`hot=false/true`模型路由 |
| `models/block/{reactor_corium,hot_reactor_corium,molten_corium}.json` | 冷热完整cube_all父链及实际流体particle来源；不为流体捏造世界模型 |
| `models/item/{reactor_corium,accident_fuel_waste}.json` | 真实物品模型来源，废料从generated的layer0消费 |
| `textures/block/{reactor_corium,hot_reactor_corium}.png`与各自mcmeta | 冷16×16单帧；HOT16×64、4帧，序0/1/2/3/2/1、20tick、插值，循环6秒 |
| `textures/fluid/molten_corium_{still,flow}.png`与各自mcmeta | still16×64、4帧，序0/1/2/3/2/1、16tick、插值，循环4.8秒；flow32×512、16个32²帧，序0…15、6tick、不插值，循环4.8秒 |
| `textures/item/accident_fuel_waste.png` | 真实16²，16/32/128最近邻同源小图 |

冷热走本地`E:/MyMC/NewMod/block-model-renderer` **2.20.0/MPL-2.0**的`parseBlockstate→resolveModelData→loadModel`，真实UV和几何；手动`createAnimator.update(ms)`与二维帧图共用时间。库及wasm直接只读服务，不复制修改。Three直接读主工程既有vendor（MIT），两许可文本可从页底打开。必要四原版父链从本机锁定client.jar只提取至preview/native，源条目/SHA见其source-index.json；原游戏版权不归本批MIT。

共同拖转/缩放、立体/正背/顶面、正常/暗环境、播放/暂停、下一帧和重播已实现。下一帧跳到所有真实动画最近的下一帧边界，暂停冻结时间；元数据从实际mcmeta解析，未将32宽flow误切为16帧。固定小图和完整帧无平滑缩放。真实mcmeta启用插值时仅按RGB混合、保留当前alpha。

## 原生边界

实际读`AccidentContent`：流体已有lightLevel12；固体沿OBSIDIAN，无HOT光级设置，因此冷热共享环境灯，没有新增全亮或emission。明暗是材质观察示意，不是Minecraft光照传播模拟。

实际`MoltenCoriumFluid`为`getFlow=Vec3.ZERO`、amount8，顶面使用still，flow主要供侧面。另只读本机锁定缓存FlowingFluid#getOwnHeight与LiquidBlockRenderer，独立液格高度8/9，侧面归一化U0…0.5，V(1-8/9)/2…0.5；未执行邻居高度平均和图集收缩。页面展示完整flow帧、该侧面取样及中心半幅旋转诊断；后者明确不是实际流向/搬移逻辑，也不声称模拟真实世界液面。

## 一次限定验证与待观察

原始记录：`build/reports/art/ART-ACCIDENT-CORIUM-22/preview/cli-check.json`。

- `node --check server.mjs/scene.mjs/animation.mjs`各exit0。
- 一次41项必要HTTP/拒绝检查全符预期：入口、脚本/样式、15生产路径、四原版父链/索引、本地库/wasm/Three/许可；未开放源稿、Java、其它资源、任意磁盘路径、服务器源码；无Origin或无ready证据的capture被拒绝。
- 从实际PNG头及mcmeta核帧宽/高、每个真实序列边界、循环回绕、半帧插值和下一帧边界；四素材时间轴均通过。废料真实generated/layer0/16²路由通过。未重复作者源像素、平铺或导出校验。
- 此CLI不执行浏览器GPU，**拖转、实际播放/暂停、明暗读感及库最终网格显示待负责人独占浏览器观察**；未开启浏览器或制造截图。

负责人可用`window.coriumPreview.getState()`读取ready/errors、输入SHA、实际模型、帧尺寸/序/时/插值/当前帧、Box3/NDC和相机。`setPlaying(boolean)`、`setTime(ms)`、`step()`、`setLight('normal'|'dark')`供控制读回。`capture()`或页底保存按钮仅从当前画布输出固定`cold.png/hot.png/still.png/flow.png/waste.png/overview.json`至本批build报告preview目录；不同光照/帧由负责人归档固定输出后保存。

交付后停止写入。浏览器展示不替代Minecraft客户端、光影、事故功能、创造拆除或真人多人观察；原04B和创造拆除业务证据按卡复用，不重跑。
