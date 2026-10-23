# ART-ACCIDENT-CORIUM-22 原创事故材质

SPDX-License-Identifier: MIT
Copyright (c) 2026 IKSXH and contributors

2026-10-23。本批原创SVG、PNG及作者工具按本目录LICENSE的MIT使用。许可文字只读复制主工程现行LICENSE，不修改事故树根许可或旧Gradle元数据；不覆盖第三方内容。没有使用原版贴图像素或图像生成。

唯一实施树`E:/MyMC/NewMod/Create_NuclearIndustry-accident`，`codex/accident-thermal-04b`，基线`1985d1b3172a5efd1f0c87f3aa39ce3fe2a6feb7`。17项精确生产允许集、8项已有文件原字节快照和6896项tracked基线在`build/reports/art/ART-ACCIDENT-CORIUM-22/assets/baseline.json`。未读取/复制/扫描用户世界，不触碰主工程运行资源、Git、根JAR或日志。

## 材质和真正源稿

深铅灰厚结壳以大片中灰、暗氧化边与上缘银灰高光区分，三处灰银混合碎片嵌在壳中；液相以橙红底、橙白亮筋表达黏稠混合熔融物。HOT沿冷残骸同一大壳布局，只给缝隙轻微明度脉动；冷态无橙色余热。废料是单个厚实冷凝块，透明边界、少量大银灰碎片，不用木炭或紫黑曜石借图。

SVG真源位于本目录sources，全部整数rect/crispEdges并带MIT声明。produce先写SVG，再实际解析SVG像素写RGBA8 PNG，PNG不是另画的近似稿。mapping.json逐项列源SHA、PNG SHA、帧尺寸、时序及17项生产当前/旧SHA。

| SVG源及对应PNG名 | 单帧 | 存储尺寸 | 帧序/每帧tick | 循环 |
| --- | --- | --- | --- | --- |
| reactor_corium | 16×16 | 16×16 | 0 / 1 | 单帧冷态 |
| hot_reactor_corium | 16×16 | 16×64 | 0,1,2,3,2,1 / 20，插值 | 120tick / 6秒 |
| molten_corium_still | 16×16 | 16×64 | 0,1,2,3,2,1 / 16，插值 | 96tick / 4.8秒 |
| molten_corium_flow | 32×32 | 32×512 | 0..15 / 6，不插值 | 96tick / 4.8秒 |
| accident_fuel_waste | 16×16 | 16×16 | 无mcmeta | 静态图标 |

still大结壳固定，液相少量亮筋和两像素小碎壳按[0,0]→[1,0]→[1,1]→[0,1]相位往复，最大1像素，不是只让全图变亮。flow以16像素周期排成32帧内2×2，逐帧向下1像素，最后一帧到首帧仍续移1像素；支持原生中心采样/旋转和侧面0..0.5采样。纹理空间边缘使用周期坐标与环绕邻居决定结壳边缘着色，不强行把边缘抹成一条统一色带。

## 生产引用与保留边界

冷/HOT仍为minecraft:block/cube_all，完整原立方体、默认0..16 UV与自有all/particle；熔融模型只改particle，不加面或自建流体几何。废料generated引用自有图；reactor_corium的原blockstate hot=false/true及物品父链已经正确，因此这两文件逐字节保持。17允许项实际15更新：5PNG、4mcmeta、4模型JSON、atlas、1Java。

atlas只在原六个single source后追加两项`fluid/molten_corium_still`与`flow`；原六项内容、字节前缀与CRLF保持。一处新增行CRCRLF已只去掉额外一个回车，证据`assets/atlas-newline-update.json`保存旧新SHA，没有重导图像。共享AccidentThermalClientEvents仅更改两sprite引用及对应中文注释；逆除这三处后与基线原字节一致。

HOT仍沿原OBSIDIAN属性，无状态发光或emissive；本稿亮色不是新光源。流体继续已有light12/getFlow ZERO，实际顶面通常使用still，flow主要是原生侧面。实际锁定LiquidBlockRenderer.java:149–181为ZERO flow/top与中心旋转选择，284–288为侧面UV。浏览器流动图案播放不代表新增物理流速、粒子、温度或辐射状态。

## 一次验证与输出

唯一`.\gradlew.bat compileJava processResources --console=plain`真实exit0、13秒，2 executed、1 up-to-date；仅一条既有initializeClient弃用警告，完整原始日志及调用数/退出在本批assets。没有test/build/clean/rerun/jar或用户客户端。

随后verify唯一一次exit0：5SVG到5PNG共18944像素零差，mcmeta尺寸/帧序/首尾、flow2×2及逐像素周期续接、HOT/冷大壳一致、still1像素运动、透明小图边界、模型/UV/particle、Java窄差异和atlas原六项/无CRCRLF均核实。16项processResources输出与生产源逐SHA相同，两个客户端class输出身份已记；6888项非批tracked文件逐SHA保持，原根跟踪JAR/日志包含在保护中，世界未枚举。

准确源、生产及处理输出SHA见`build/reports/art/ART-ACCIDENT-CORIUM-22/manifest.json`，排除自引用清单、过程结束后写入的verification日志/退出、叙述报告及独立预览文件。报告和ASSETS不是新的生产输入，不用它们回改既有源SHA。

实际技能入口均`C:/Users/IKSXH/.codex/skills/`：minecraft-resource-pack用于PNG/mcmeta/UV/atlas，minecraft-modding用于真实版本及流体sprite边界，minecraft-testing用于只做必要增量/像素检查并复用04B和创造拆除业务证据。锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，服从主工程AGENTS/治理1.2/5.1/6及联合卡；事故树旧许可元数据保持，未借技能做升级、Git或全量矩阵。

独立预览、Minecraft正常/暗光、光影和多人观察分开记录；本作者仅完成资源/静态/增量编译，不声明游戏视觉或创造零掉落复看已通过。联合JAR由主PM随后生成，素材侧完成后停写。
