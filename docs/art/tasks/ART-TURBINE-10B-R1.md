# ART-TURBINE-10B-R1：恢复已选B稿的贴图与材质分配

用户直接指出当前正式OBJ预览贴图不同于已选设计稿。本次恢复10A/R1的B材质，不提出另一套设计；保留10B已确认的0.125格薄叶片、双列中心/错相、固定孔径及固定/旋转拆分。原10B自动和审查证据保留历史；当前材质偏离需整改，不能用几何检查通过代替风格符合。

## 已核根因与参照

负责人对比实际文件和两个页面：10A的`B_rotor.svg`、`B_bearing.svg`与10B四SVG并非同源。10B producer重新画了更亮的灰蓝条纹，并将纹理覆到整个轮毂/轴承圈/轴体；10A的轮毂/轴圈/箱座等大量表面是深灰至银灰的分区纯钢色，叶片端面和轴承外板使用原B SVG。当前10B页面是加载真实候选PNG，不能仅改网页造成预览与正式资源分离。

只读权威参照：`tools/art-assets/turbine-rotor-bearing-concepts-10/sources/textures/B_rotor.svg`、`B_bearing.svg`以及同目录`preview.js`的`texturePattern`、`rotorMesh`、`supportPlate`、`plateMesh`；不能修改这些文件。负责人已保存实际原B页面`build/reports/art/ART-TURBINE-10B/assets/root-reference-b.png`。

## 实施权限和边界

原素材执行者`turbine10b_models`继续本次材质窄修，负责人只写docs/art。唯一写树仍为`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，不Git、Gradle、游戏、世界/配置或Java；服务端接口门与原中立15例证据保持。任务需要实际读取minecraft-resource-pack及原10B卡；使用SVG像素源、确定性PNG，手写注释中文。

允许原10B独立tool中的producer、SVG、候选、resource-map和必要preview纹理提交适配；允许本卡报告`docs/art/reports/ART-TURBINE-10B-R1-ASSETS.md`与新证据目录`build/reports/art/ART-TURBINE-10B/assets/r1/`。旧source/map/候选和当前安装资源须先只读冻结到R1证据/新备份，不覆盖原17份基线备份。资源仍限制原10B精确26路径，无机壳/窗口/shared MTL/注册ID变更。先交具体材质/UV最小方案及更正候选；负责人实看后批准原作者安装变化路径，不自安装。

叶片和外板优先直接复制原B SVG，像素及色板不得任意再设计。其他纯钢/小铜表面从实际原B显示颜色提取有限色SVG色块，按表面类别采样，不能继续全圈铺满亮条纹。保留原16×16资源和四PNG ID；SVG允许原稿实际色数，原8色checker是此前新画素材的局部断言，不能为通过它删掉已选稿颜色。

本轮允许材质分配/UV变更，但不得重新设计实体几何。先核原B的纹理位置/朝向/密度，再给可在NeoForge atlas中正确执行的UV方案；不要用超出0..1的sprite UV而在网页私自repeat掩盖游戏串图。若必须面片切分以复现原纹理密度，先报告具体范围，由负责人核不改变实体表面后再追加名单。不会为了通过旧checker重跑整个几何库；只做必要纹理同源/引用/UV和未改变几何的差异核对，旧通过证据复用。

### 原作者最小方案与共面切分追加权限

原作者实际冻结preR1 68项身份exit0，已交最小方案。负责人批准：叶片/外板两SVG逐字复制原B；纯钢区域采原B显示色块、铜色固定`#b27a4e`，原B两SVG实际12/10色保持。外板UV整块XY映射，内板/边/孔壁分别原`#4c5658`/`#626d6f`/`#424b4d`；其他面UV只取对应钢色块中心。纹理单位复用原`texturePattern`，叶片原点根部负弦角p[0]、径向theta+.063/切向基，16px对应1格，不拉满每片。未成型按其实际缩小几何重新计算1格密度。

该原映射存在微负V和中大型跨U=1。现精确允许`rotor_blades_d3/d5/d7.obj`及`turbine_rotor_unformed.obj`的24片叶片两个端面，仅沿整数U/V seam共面切分，并在受影响原薄边面添加对应共线分点以保持封闭。tool对照厚叶片允许同样切分。不得改变叶片实体表面/厚度/中心/错相/包络/孔径/轴向范围/轮毂或固定动态拆分；新顶点须在原面或边上，UV必须在0..1内，分片覆盖/面积/方向与原完整面一致。其他输出/轮毂实体几何不改。核新切分封闭/法向/UV和旧真实几何保护，复用原不变扫掠/孔径证据，不机械重跑全库。

作者已实际核本机锁定NeoForge ObjModel第375～376行：`flip_v=false`直接`texture.getV(texCoord.y)`。当前预览用`1-v`有垂直翻向差异，允许原tool preview纹理取样改为实际直接V，报告记录锁定源身份/行证据；不能改生产JSON的flip_v或用预览遮盖正式UV错误。仍先交候选/映射和必要检查，生产安装另等负责人复看。

## 核验与交付

保存原问题与原B参照、实际候选source/PNG一致、几何位置/法向/包络保护及引用/UV检查日志退出码。负责人同视角实际复看D3/D5/D7、单件和前后轴承，按已选材质作判断。原唯一reviewer仅追加受影响材质/UV及安装差异窄审，不重审中立Java或服务端；最后组合assemble仍由原客户端消费者在共享接口到位后唯一执行。本次修正不关闭GPU/用户视觉人工门。

### 冻结候选安装权限

负责人已实读R1的7项/120叶片组检查exit0、来源报告和锁定ObjModel源码片段，实看修正纹样、薄厚45度对照、三档与前内/后外轴承；纯钢Canvas的三角假接缝已在同视角消除。原reviewer冻结材质/UV阶段无必改，网页窄修/源码证据与安装由同轮追加。现批准原作者只安装映射SHA256=`238f377912d128ed1b8bbd7531aac16803ae3c133d05c53728d4f9b2ccb29704`中实际变化的14路径：原10B十OBJ及四PNG（如下）；11JSON/专属MTL不复制、不改。先核26现安装仍为preR1身份，将14原件完整备份到新的R1安装备份目录，旧17基线备份和preR1证据保持；如果并行修改导致SHA不符先报告。只复制此冻结候选，不再生/不Gradle/Git，保存原始安装、备份和来源/安装读回日志。

- `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/output_shaft_front.obj`
- `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/output_shaft_front_rotating.obj`
- `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/output_shaft_rear.obj`
- `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/output_shaft_rear_rotating.obj`
- `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/rotor_blades_d3.obj`
- `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/rotor_blades_d5.obj`
- `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/rotor_blades_d7.obj`
- `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/turbine_output_shaft_unformed.obj`
- `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/turbine_output_shaft_unformed_rear.obj`
- `src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/turbine_rotor_unformed.obj`
- `src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_10b_bearing.png`
- `src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_10b_blade.png`
- `src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_10b_copper.png`
- `src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_10b_steel.png`
