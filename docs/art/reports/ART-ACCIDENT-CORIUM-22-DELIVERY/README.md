# 事故材质22交付入口

后续用户反馈（2026-10-23）：用户指出首版“太规则了，都是重复的同方向三角形”。已按[R1整改合同](../../tasks/ART-ACCIDENT-CORIUM-22.md)重画碎壳布局，当前取件按下方R1入口；首版内容只保留交件历史，不能当作用户已认可的最终构图。

2026-10-23，美术负责人交件。本批按用户指定，在原事故逻辑候选内制作一版完整素材，不单独合逻辑、不制作另一美术JAR。原世界保持原位，未启动或复制世界。本页仅作美术交付，不替代主工程STATUS或用户游戏验收。

## R1当前取件（用户构图意见整改）

当前[根清单](../../../../build/reports/art/ART-ACCIDENT-CORIUM-22/manifest.json)与[R1清单](../../../../build/reports/art/ART-ACCIDENT-CORIUM-22/r1/manifest.json)均为revision=R1，取件不再使用首版PNG/SVG身份。相对首版仅四PNG及flow mcmeta共5生产项变化，四对应SVG重绘；其它12生产项和废料SVG逐字节保持。

冷热/still为六块不同大小/形状/长轴的手工断板/裂片/凹缺/小块。flow为独立32×32布局，取消四张16×16复制，改32相/每6tick/9.6秒周期，保持向下1px及末首衔接。当前[4210预览](http://127.0.0.1:4210/)直接消费R1真实素材与mcmeta，框架未改。

[R1素材报告](../ART-ACCIDENT-CORIUM-22-R1/ASSETS.md)及[负责人限定观察](../ART-ACCIDENT-CORIUM-22-R1/OBSERVATION.md)列明实际变化：35072RGBA像素SVG→PNG零差，一次processResources exit0/4秒，5处理输出匹配；13保护项与48首版快照保持，原两个client class保持。复用首版compileJava和未变业务/接线，没有重扫6888或世界，没有新JAR。新原始日志/验证/半幅平铺小样/明暗捕获均在本批build的r1目录，原报告正文未追改。

[R1限定复审](../ART-ACCIDENT-CORIUM-22-R1/REVIEW.md)已独立查看新轮廓、完整/半幅裁切平铺和现成明暗捕获，未发现未闭阻断。此结论不代替用户对R1读感的认可，亦不新增Minecraft视觉或创造拆除通过记录；联合候选继续按主工程合同处理。

## 首版交付历史

## 取件身份

- 唯一树：`E:/MyMC/NewMod/Create_NuclearIndustry-accident`；分支`codex/accident-thermal-04b`；源基点`1985d1b3172a5efd1f0c87f3aa39ce3fe2a6feb7`，本轮无Git写操作。
- [本批任务与分工](../../tasks/ART-ACCIDENT-CORIUM-22.md)；主工程[联合合同](/E:/MyMC/NewMod/Create_NuclearIndustry/docs/superpowers/plans/2026-10-23-accident-art-integration-01.md)约束优先于旧事故树核心文档。
- [精确SHA清单](../../../../build/reports/art/ART-ACCIDENT-CORIUM-22/manifest.json)包含17允许生产项（实际15更新、2原路由保持）、5SVG、必要导出工具/许可、16处理资源输出及2客户端class。没有自引用、叙述报告或独立预览SHA的伪完备声明，具体排除见清单。
- 可编辑源与导出说明：[ASSETS](../../../../tools/art-assets/accident-corium-22/ASSETS.md)、[mapping](../../../../tools/art-assets/accident-corium-22/mapping.json)。五SVG为真正导出源，原创素材/工具MIT，第三方及原游戏父模型保持原许可。

## 实际生产差异

R=`src/main/resources/assets/create_nuclear_industry/`。只有以下15项新增/更新；原`R/blockstates/reactor_corium.json`和`R/models/item/reactor_corium.json`的正确路由保持原字节。

| 文件 | 数量与职责 |
| --- | --- |
| R/models/block/{reactor_corium,hot_reactor_corium,molten_corium}.json | 3；冷/HOT沿cube_all，自有all/particle；流体仍仅particle |
| R/models/item/accident_fuel_waste.json | 1；generated使用自有小图 |
| R/textures/block/{reactor_corium,hot_reactor_corium}.png及各自.png.mcmeta | 4；冷凝结壳、HOT轻热脉动 |
| R/textures/fluid/molten_corium_{still,flow}.png及各自.png.mcmeta | 4；静止4相/4.8秒、侧面16相/4.8秒 |
| R/textures/item/accident_fuel_waste.png | 1；16×16透明边厚块小图 |
| src/main/resources/assets/minecraft/atlases/blocks.json | 1；原六source保留，只追加两sprite |
| src/main/java/com/iksxh/create_nuclear_industry/accident/client/AccidentThermalClientEvents.java | 1；仅still/flow引用和对应中文注释 |

深铅灰金属壳、灰银混合碎片和橙白熔融裂隙贯穿冷热/熔融/小图。没有新状态、粒子、声音、发光机制、几何碰撞、光照或热/辐射/库存规则；事故火不变。原逻辑及创造拆除窄修保持。

## 已交证据

- [素材报告](../ART-ACCIDENT-CORIUM-22-ASSETS/README.md)：一次导出，18944 SVG→PNG RGBA像素零差；PNG/mcmeta/UV/帧序/循环/平铺和图集原六项保持。一次新增行多回车窄修留原证据，未重导图像。
- 唯一增量`compileJava processResources --console=plain`，exit0、13秒；2 executed/1 up-to-date。一条既有initializeClient弃用警告保留，没有为它扩写Java。16处理输出逐SHA同源，6888非批tracked身份保持。见[原始日志](../../../../build/reports/art/ART-ACCIDENT-CORIUM-22/assets/compile-processResources.log)、[退出](../../../../build/reports/art/ART-ACCIDENT-CORIUM-22/assets/compile-exit.json)、[窄校验](../../../../build/reports/art/ART-ACCIDENT-CORIUM-22/assets/verification.json)。
- [预览报告](../ART-ACCIDENT-CORIUM-22-PREVIEW/REPORT.md)：本地block-model-renderer2.20.0实际模型/UV/像素与真mcmeta，3脚本语法和41 HTTP/拒绝项通过，真实帧时间轴有界检查通过。
- [负责人观察](OBSERVATION.md)：正常/暗环境、冷热与小图、暂停/逐帧/播放、拖转实际读回；GPU ready、无页面warn/error。本批[预览](http://127.0.0.1:4210/)保留，session68748/PID80084，绑定127.0.0.1。PNG及正常/暗环境overview在本批build报告preview目录；浏览器不能替代Minecraft观察。
- [唯一组合窄审](../ART-ACCIDENT-CORIUM-22-REVIEW/README.md)已读取本批差异、既有校验/编译、真实BMR调用链及正常/暗环境十张捕获，未发现未闭阻断；没有重复执行测试或构建。负责人已校正一处保存快照与随后播放读数的时刻描述，无实现或证据变化。
- 实际技能入口均`C:/Users/IKSXH/.codex/skills/`的minecraft-resource-pack、minecraft-modding、minecraft-testing，分别用于SVG/PNG/模型与atlas、锁定版本/客户端边界和按风险限定验证。实际Gradle锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，未升级。

## 后续联合候选边界

复用04B及创造拆除已有业务证据，本批没有重跑JUnit、GameTest、长冷却矩阵或世界哈希。没有jar/build/clean/rerun，没有替换根跟踪JAR/日志或改核心文档、主工程、旧美术树。开工只读主工程status已见根跟踪JAR缺失及3D工具dirty，本批保留既有状态，没有尝试恢复/清理。

主PM核对清单并提交后，按联合卡生成唯一联合候选。用户在原事故世界集中看本批外观与创造拆残骸零掉落，不重新要求已反馈完成的熔融物流体整组功能；原功能链、窄修及美术一起合入main。当前没有新增Minecraft视觉/创造拆除/光影/真人多人通过结论。
