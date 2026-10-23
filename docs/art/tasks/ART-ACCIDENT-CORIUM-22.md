# ART-ACCIDENT-CORIUM-22：事故材质一版联合候选

2026-10-23，美术负责人按主工程[联合协作卡](/E:/MyMC/NewMod/Create_NuclearIndustry/docs/superpowers/plans/2026-10-23-accident-art-integration-01.md)组织本批。本页是美术分工，不维护全局任务状态。

## 来源与边界

- 唯一写入树`E:/MyMC/NewMod/Create_NuclearIndustry-accident`，`codex/accident-thermal-04b`，基准`1985d1b3172a5efd1f0c87f3aa39ce3fe2a6feb7`；派发时tracked干净。本批按用户指定在逻辑候选实施，不新树、不切分支、不复制旧整包。
- 先读主工程AGENTS、治理1.2/5.1/6、STATUS、联合协作卡及04B/创造拆除卡。事故树核心文档可能较旧，不能以其覆盖主工程合同。
- 所有人无Git写权。原世界、根跟踪JAR/日志及其它Java/注册/玩法保持。事故火沿用原版；不新增光源、粒子、状态或伤害。主PM随后提交及生成联合候选，美术侧不jar/build/clean/rerun。
- 实际读取应用`C:/Users/IKSXH/.codex/skills/{minecraft-resource-pack,minecraft-modding,minecraft-testing}/SKILL.md`；核实际Gradle锁定版本MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。原创源稿/工具/素材MIT，第三方许可不覆盖。

## 一版视觉合同

以深铅灰金属结壳、灰银燃料/结构碎片和橙白熔融裂隙表达事故混合熔融物。保持低分辨率块面、高低光及克制细节；避免直接复刻原版熔岩噪声、紫黑曜石或木炭。

- 熔融静止：大块结壳浮在连续橙红金属液中，少量白热亮点；缓慢循环，首尾连续。全部不透明，暗环境依赖既有流体光照，不新增渲染机制。
- 熔融流动：同配色/材质，竖向流带带动碎壳。遵守原生流动sprite中心采样/旋转语义，边界可平铺，不自建流体模型。
- HOT：与冷态相同凝固碎壳布局，亮橙裂隙轻微呼吸；不是整面平铺亮噪声。冷态保留灰银熔接碎片、厚边缘与暗氧化缝，没有余热发光。二者正常/暗光下可区分。
- 废料16×16小图：单一厚实凝固块轮廓，附少量可识别的灰银燃料碎片，与冷残骸同材质，留透明外边；小尺寸清楚。
- SVG是可编辑真源（整数像素矩形、crispEdges）；先SVG后同像素PNG，禁止图像生成。方块维持现有完整立方体，保持hot=false/true和原物品行为。

## 实施分工与互斥写集

### 素材执行者：dry19_assets_recovery

仅联合卡精确生产写集：`reactor_corium` blockstate，三个block模型、两个item模型、两张block PNG及mcmeta、两张fluid PNG及mcmeta、一张item PNG；`assets/minecraft/atlases/blocks.json`只追加两sprite并保留原六项。共享Java只改`accident/client/AccidentThermalClientEvents.java`的still/flow资源引用和对应中文注释。

专属工具写集`tools/art-assets/accident-corium-22/`内除`preview/`外路径，含source SVG、导出/窄校验工具、mapping及ASSETS说明。报告写`docs/art/reports/ART-ACCIDENT-CORIUM-22-ASSETS/`，证据写`build/reports/art/ART-ACCIDENT-CORIUM-22/assets/`及`manifest.json`。先保存本批基线身份；校验生产JSON/UV/PNG/mcmeta/atlas引用、SVG导出像素、动画首尾/平铺和精确SHA。生产资源稳定后通知负责人；仅该执行者持有一次增量`compileJava processResources --console=plain`，不打包。最终SHA排除自引用清单，不重复构建。

### 预览执行者：dry19_preview

只写`tools/art-assets/accident-corium-22/preview/`、`docs/art/reports/ART-ACCIDENT-CORIUM-22-PREVIEW/`、`build/reports/art/ART-ACCIDENT-CORIUM-22/preview/`。读真实生产模型/PNG/mcmeta，通过本地`E:/MyMC/NewMod/block-model-renderer`展示冷热方块，可旋转；静止/流动动画以原帧序/时间播放，提供流体面及原生中心采样示意；废料显示16/32及放大小图。明暗环境、暂停/逐帧、实际资源路径可检查，标明浏览器展示不是Minecraft实测。不在预览重画替代素材，不改共享预览工具；所有临时构建缓存也限本目录。只绑定127.0.0.1，核依赖许可/真实路径，不安装或改外部库。

### 窄审：dry19_review

在两执行者停写后，一次组合只读审查真实差异、源/导出与现有验证证据，重点写集、atlas原六项、Java仅sprite、帧/UV/预览真实性及原事故功能/创造修复未触碰。只写`docs/art/reports/ART-ACCIDENT-CORIUM-22-REVIEW/`及对应`build/reports/art/ART-ACCIDENT-CORIUM-22/review/`，不重跑测试/构建。出现具体问题仅相关范围整改复看。

## 交付与观察

负责人读取预览并记录观察，汇总精确清单、源稿/导出/SHA及报告。静态/浏览器/编译分别记录，复用原04B和创造拆除业务证据，不重跑JUnit/GameTest或长矩阵。联合候选由主PM生成，用户仅集中看外观和创造拆残骸零掉落；不重新要求已反馈完成的整组流体功能。未实际观察的Minecraft/光影/多人项目不补记通过。

## R1：碎壳方向与形状整改（2026-10-23）

用户查看4210后反馈“看起来太规则了，都是重复的同方向三角形”。据此收回首版构图作为最终外观的候选资格；首版证据保持历史语境，不追记用户认可。

- 同一事故树、原17项生产允许集不扩大。R1仅允许更新四张冷/HOT/still/flow PNG、flow对应mcmeta（如周期扩展确需），四份对应SVG、专属导出/窄校验工具、mapping/manifest和本批报告。废料小图、其余mcmeta、模型、Java、atlas、Gradle、世界及根包/日志均保持本轮前字节。
- 先完整保留当前首版四SVG、四PNG/flow元数据、mapping/manifest、原素材/预览/审查/负责人观察及既有build证据到本批build目录的`r1/before/`。不删除旧检查；本轮后续证据只写`r1/`，源稿/导出工具允许在原专属路径继续修订。
- 冷/HOT/still取消复制同一个切角块的整齐排列。采用5～7块互不全等、尺度错落的手工像素轮廓，至少包含一块较宽的断裂板、一条短条片、凹缺/折角和小碎块；长轴方向交错，避免相邻同向尖头和均匀网格。冷热共享同一固壳布局，高光方向统一，碎块轮廓不统一。液相通道宽窄变化、分叉而非均匀亮边。
- flow使用完整32×32周期的独立手工布局，不再把同一个16×16四次复制。不同方向/大小的金属碎壳在黏稠液相中分散，保留足够大块面，避免变成随机噪点。维持每6tick向下1px；如需32相位完成32px周期，可将flow扩为32帧/9.6秒，属同一已有动画而非新增状态。原生半幅/旋转采样及空间边界连续必须保留。
- 原SVG真源→PNG整数像素路线保持。still小幅液相运动、HOT轻热脉动、配色和冷热辨识保持，不新增发光或玩法。禁止用任意旋转高光制造光照方向混乱；只转碎壳的轮廓/长轴，照明来自同一侧。
- 素材执行者沿原工具写集实施本次修订；只作受影响像素/帧/循环/平铺/元数据/引用和R1非批字节检查。Java及加载引用没变，复用首版compileJava；如需更新处理输出，仅一次增量processResources，不重新编译/打包或跑测试。不得为了通过旧的“2×2同图”实现断言而保留用户已否定的构图，校验应改查实际32周期连续和四象限差异。
- 4210原预览程序支持真实mcmeta，优先原样消费R1素材，负责人刷新并观察，不重建框架或重跑全部HTTP矩阵。审查者只复看R1实际差异/现成证据，不复审未变首版接线；全部结果补至R1报告及交付入口，最后停在美术对话。
