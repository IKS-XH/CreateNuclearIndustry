# ART-ACCIDENT-CORIUM-22 素材执行报告

2026-10-23，执行者dry19_assets_recovery。完成一版原创熔融静/流、HOT/冷残骸与废料小图；只在指定事故候选写集实施，结束停写。本报告不维护全局任务状态，不代表Minecraft或创造拆除人工通过。

## 来源与实际范围

唯一树`E:/MyMC/NewMod/Create_NuclearIndustry-accident`，`codex/accident-thermal-04b`，基线与最终观察HEAD均`1985d1b3172a5efd1f0c87f3aa39ce3fe2a6feb7`。已读主工程最新AGENTS、治理1.2/5.1/6、STATUS、联合协作卡、04B及创造拆除卡；本树22卡只作美术分工。当前批次是用户指定在逻辑候选制作的例外，不新建树、不切分支、不搬其它整包、不修改主工程资源。

`assets/baseline.json`保存17项精确生产允许集、8项已有文件原字节快照及6896项tracked身份。没有枚举用户世界，原世界原位保持。原跟踪JAR/日志纳入非批保护，不重新生成或覆盖；无Git写、注册/配置/玩法/其它Java变更。

17允许项实际更新15，保留2个原路由文件：reactor_corium blockstate已有hot=false/true映射正确、其item已有自有冷块父级正确，均保持原字节。实际15项为5PNG、4mcmeta、4模型JSON、atlas及AccidentThermalClientEvents。三block模型仍为冷/HOT完整cube_all及无几何流体particle模型；废料generated自有layer0。Java只有still/flow资源引用和中文注释，逆除后原字节相同。

## 真源与视觉实现

`tools/art-assets/accident-corium-22/sources/`五份SVG为可编辑整数像素真源，先落SVG再解析成同像素RGBA8 PNG。深铅灰金属结壳、银灰混合碎片、橙白液相相配；大块面/明暗厚边，没有借原版熔岩、岩浆块、哭泣黑曜石或木炭像素。五PNG对应同名SVG，完整源/输出SHA在mapping与最终manifest，不以预览替代真素材。

| 生产纹理 | 像素尺寸 / 帧 | 节奏 |
| --- | --- | --- |
| block/reactor_corium | 16×16 / 1 | 冷态固定无余热色 |
| block/hot_reactor_corium | 16×64 / 四16²帧 | 0,1,2,3,2,1，20tick，6秒轻热脉动 |
| fluid/molten_corium_still | 16×64 / 四16²帧 | 同序16tick，4.8秒；少量液筋/小壳1px往复 |
| fluid/molten_corium_flow | 32×512 / 十六32²帧 | 每帧6tick，逐帧向下1px，4.8秒无缝周期 |
| item/accident_fuel_waste | 16×16 / 1 | 单个厚实冷凝块与少量银灰碎片，透明外边 |

HOT和冷态大结壳布局/金属色一致，HOT只变裂隙明度；still大壳不移动，两像素小壳和少量亮筋在[0,0]/[1,0]/[1,1]/[0,1]四相位往返，最大1像素。flow32帧内是16周期2×2，末帧到首帧继续下移1像素，适配中心旋转与侧面裁切。实际本体getFlow ZERO保持，所以顶面选still、flow主要用于原生侧面，预览不能说已改物理流速。

HOT仍是OBSIDIAN属性，无状态光源/emissive；亮色裂隙不是新增光照。流体既有light12不变。冷热/明暗最终观感交独立预览和人工门，本报告不把亮色当温度/辐射新状态。

## atlas与唯一窄修

原六single source原样保留，仅在末尾追加`create_nuclear_industry:fluid/molten_corium_still`、`molten_corium_flow`，现在8项。root发现新增两项之间一处CRCRLF，已只去掉额外一个回车并同步工具插入算法/清单，未重导图像；`assets/atlas-newline-update.json`保旧SHA `4789d6bf0294f42c944252aa68c0658b2bd6f7e4d2fd2cb53349fe61ae75ea05`、新SHA `687d361c364b5bf1728571875cc45cf5ad8e7f1f79bc69c57b3cfa1363d657e0`及原六项字节前缀/结构保持证据。最终检查确认全atlas沿原CRLF、CRCRLF=0。

## 实际检查、编译与身份

唯一命令`.\gradlew.bat compileJava processResources --console=plain`真实exit0，`BUILD SUCCESSFUL in 13s`，3 actionable tasks：2 executed、1 up-to-date。只出现一条原AccidentContent.initializeClient弃用警告，完整原始日志保留；没有test/build/clean/rerun/jar，不启动用户世界或客户端。

随后verify唯一一次真实exit0，结果PASS_STATIC_AND_INCREMENTAL_COMPILE：

- 5SVG/5PNG共18944RGBA像素零差；4mcmeta尺寸/帧序/时限和循环末首合法。
- HOT/冷/静止液相的大结壳固定，still小壳1px位移正确；flow16像素周期、32帧内四象限相同，末到首续移1px。
- 非物品纹理完全不透明，废料有透明外边；原立方体/UV/particle/物品父链与HOT键保持，atlas原六项及两新增sprite合法。
- Java只两sprite/注释；16项processResources输出逐SHA匹配生产源，两个对应客户端class身份记录。
- 6888项非批tracked文件逐SHA保持，未触碰原功能/创造修复/根JAR/日志；世界没有枚举或重复核验。
- 原Git只读diff给出LF将按仓库规则转CRLF的提示，原样保存；没有执行Git转换/写操作。

证据均在`build/reports/art/ART-ACCIDENT-CORIUM-22/`：

| 文件 | 用途 |
| --- | --- |
| assets/baseline.json、originals/ | 开工身份、允许集与旧8文件 |
| assets/source-output-mapping.json | 五SVG、五PNG、帧及17项源身份 |
| assets/compile-processResources.log、compile-exit.json | 完整原始命令日志、UTC起止、调用数1、真实exit0 |
| assets/verification.json、verification.log、verification-exit.json | 实际资源/循环/非批/输出核验、调用数1、exit0 |
| assets/atlas-newline-update.json | 单一新增行回车整改及旧新SHA |
| manifest.json | 精确生产源、SVG、工具许可、16处理输出及2class SHA |

最终manifest明确排除自身、进程结束后写入的verification日志/退出、后写叙述报告/ASSETS及独立预览文件，避免自引用或把他人写集纳入本作者声明。报告只索引原始证据，不重新扫世界或重复生成检查。

## 技能、许可及未观察项

实际应用三个入口`C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`、`minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`：分别用于真实PNG/mcmeta/UV/atlas、实际版本/原生fluid sprite边界、仅必要增量/资源证据及复用04B和创造拆除业务。实际Gradle为MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，未升级或改协议。

本批原创源/工具/素材明确SPDX MIT、IKSXH and contributors；必要许可文字在本批tools/LICENSE，只读参照主工程现行MIT。事故树旧根许可/Gradle元数据保持，未做许可整改、借图或图像生成。

资源稳定已先通知root让4210读取真素材，预览目录未触碰。没有重跑JUnit/GameTest/04B长矩阵；既有功能反馈保留，不把本次编译或网页代替Minecraft视觉、创造拆残骸零掉落复看、光影或多人观察。联合JAR由主PM后续生成；本作者当前停写，无Git/打包或状态验收权限。
