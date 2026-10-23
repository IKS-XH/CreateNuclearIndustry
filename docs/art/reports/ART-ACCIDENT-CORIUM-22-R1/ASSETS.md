# ART-ACCIDENT-CORIUM-22-R1 不规则碎壳整改

SPDX-License-Identifier: MIT
Copyright (c) 2026 IKSXH and contributors

2026-10-23，素材执行者dry19_assets_recovery。按用户“太规则、重复同方向三角形”反馈重做四张材质；首版只保留为历史证据，不登记用户认可。R1资源、一次processResources和一次窄核已完成，现停写。本报告不代表Minecraft视觉或用户已接受R1。

## 原件保留与范围

唯一目录仍为`E:/MyMC/NewMod/Create_NuclearIndustry-accident`，HEAD `1985d1b3172a5efd1f0c87f3aa39ce3fe2a6feb7`，未切分支或Git写入。

开始先将首版四SVG/PNG、flow mcmeta、mapping、manifest、produce/verify、原ASSETS、素材/预览/审查/负责人交付观察报告，以及已有assets/preview构建和捕获证据逐项复制到`build/reports/art/ART-ACCIDENT-CORIUM-22/r1/before/`，共48文件。新r1自身未被递归复制；首版原检查/日志/报告未覆盖，最终窄核确认48份快照SHA保持。对应来源/快照/字节/SHA与R1允许集在`r1/baseline.json`。

本轮生产只改四PNG和flow mcmeta共5项，另改四对应SVG及专属produce/verify/mapping/manifest。其余12项生产文件（Java、atlas、模型、blockstate、其它mcmeta、废料PNG）及废料SVG共13项逐字节保护；没有世界、根JAR/日志、注册、配置、玩法或其它Java操作。

## 构图实际变化

冷/HOT/still共用新的六块手工固壳底版，面积分别45/10/17/19/5/15像素，互不全等：

- 横向宽断板：最大块，边缘带不对称断口和凹缺，不是统一切角。
- 斜向短条：短而窄，与宽板长轴交错。
- 凹缺折角：保明显内凹与折边。
- 纵向裂片：长短边错落，方向与宽板不同。
- 小碎块：只有5像素，作为尺度对照。
- 边缘撕裂片：跨纹理边缘，轮廓与其它五块不同。

移除了首版反复使用的同款切角块和均匀网格。液相沿宽窄变化的通路分叉，大板/细片/小块比例错落。只是改变轮廓与长轴方向；所有结壳仍从统一上左侧取高光，右下保暗边，没有旋转光照方向。

HOT只在新裂缝中轻微热脉动；冷态保持灰银凝固色。still大壳固定，小碎壳及亮筋仍在1像素范围内往复，位置已适配新通路。原配色、无新增emissive、流体light12/getFlow ZERO保持。

flow另画完整独立32×32手工构图，九块不同大小/方向的宽板、双叉片、纵片、钩形片和少量短片交错，不再四次复制16×16。动画变为32帧，PNG为32×1024，每6tick向下1像素，完整32像素周期192tick/9.6秒；最后一帧到首帧继续下移1像素。HOT与still的原帧序、节奏和mcmeta字节均保持。

## 原生半幅读感样图

本轮另留三张素材读感小样，每张均先SVG再PNG，并逐像素对照实际生产flow帧0裁切平铺，不是另画的替代材质或Minecraft截图：

| r1/sampling文件名 | 真实取样及展示 |
| --- | --- |
| native_side_16x16_3x2.png | 左上16×16，即完整高度侧面U0..0.5/V0..0.5，3×2重复，48×32 |
| half_width_16x32_3x2.png | U0..0.5完整纵幅16×32，3×2重复，48×64 |
| full32_2x2.png | 完整32×32，2×2重复，64×64 |

这些小样用于看大壳是否在半幅切口形成规律竖缝；并未以“四象限逐字相同”来冒充平铺。素材作者已读取小样，宽板、斜片和钩形轮廓可辨；最终空间切口、明暗及动画读感仍由root在原4210框架观察，不从生成器断言推定视觉通过。小样仅代表帧0，完整动态读取真实32帧。

## 一次资源处理和窄核

仅执行`.\gradlew.bat processResources --console=plain`一次，真实exit0，`BUILD SUCCESSFUL in 4s`，1 task executed。没有compileJava、test、jar、build、clean或rerun。原13秒compileJava证据保存在before中复用，两个既有客户端class当前SHA保持，没有重编译。

随后新verify一次exit0，`PASS_R1_RESOURCES_ONLY`：

- 四SVG→四PNG共35072RGBA像素零差，帧尺寸/序列及不透明性正确。
- 六固壳面积不同，真实落在冷图上；HOT/still的大壳布局与金属像素保持一致，still小壳1像素往复正确。
- flow实际32周期，四象限SHA互异，水平/纵向都不是16周期复制；32帧逐像素向下平移及末首续移正确。已删除旧“2×2相同”的实现断言。
- 三裁切平铺样图共8704像素与实际flow帧0一致。半幅边界差异按相位记录为证据，没有把审美读感伪装为任意阈值。
- 5项受影响processResources输出逐字节匹配生产源；13保护项、48首版快照及原两个客户端class保持。
- Git tracked差异范围与R1前相同，src没有写集外新增。未重扫6888项旧非批源，也未枚举或扫描世界。

原只读Git换行提示保留在日志，没有执行Git转换。没有新增冗余JUnit或重跑首版接线/旧功能/HTTP矩阵。

## 精确身份与交接

当前专属`tools/art-assets/accident-corium-22/mapping.json`已更新R1真实尺寸、32帧及192tick周期；新副本和所有新原始证据仅在`build/reports/art/ART-ACCIDENT-CORIUM-22/r1/`：

- `baseline.json`、`before/`：首版完整保留与本轮5项写集/13项保护身份。
- `produce.log`、`produce-exit.json`：唯一导出实际结果。
- `processResources.log`、`processResources-exit.json`：唯一增量资源处理原始日志/UTC/exit0。
- `verification.json`、`verification.log`、`verification-exit.json`：唯一窄核、每相位数据与exit0。
- `mapping.json`、`manifest.json`：精确源SVG、PNG/flow metadata、处理输出与工具SHA，明列复用原编译证据。
- `sampling/`：三份实际取样小样的SVG真源/PNG及身份。

根本批`build/reports/art/ART-ACCIDENT-CORIUM-22/manifest.json`也已指向revision=R1；旧根manifest原件在before。最终清单排除自身、后写verification日志/退出、新叙述报告与独立预览/负责人文档，避免自引用。不把首版6888项检查冒充R1重新扫描，也不把五项新处理输出冒充全量构建。

原生UV和加载引用未改，继续复用首版实际Minecraft1.21.1/NeoForge21.1.219接口核查、resource-pack/modding/testing技能及主工程治理5.1。原创本批源/工具/素材沿MIT与IKSXH and contributors声明，未更改根许可元数据。没有改预览框架或操作浏览器；资源稳定已先通知root刷新。素材侧当前停写，交root限定R1复看与审查，不自行打包或宣布人工门通过。
