# ART-TURBINE-07B-R2：中大型机壳疏线与连窗

用户2026-10-12实机反馈：当前贴图在小型汽轮机上较好，中、大型线条过密，观察窗没有连接纹理。本批直接整改这两个显示问题，沿已选第二稿；不把小型反馈扩大为整批或其他尺寸已验收。R1端盖和既有证据保留。

## 目标与权限

小型d3机壳细节节奏保持；d5/d7增加完整钢板面积、减少重复束带与刻线，保留可辨明暗及钢铜材料。轴向像素密度仍为16/世界格，截面沿既有完整外表面UV；不能靠轴向放大旧单格贴图减少线条。窗口相邻去内框、外周保留厚框，按实际既有合法位置和同面方向连接；不穿过功能口、普通壳或不同机组/朝向连接。

唯一工作树`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，HEAD6042fd2及当前07B/R1未提交候选。负责人按用户美术专项任命制定卡和审查，执行者只实施精确写集；无Git、主/逻辑树、游戏或存档写，不继续委派。共享服务端、注册身份、碰撞/放置、网络、玩法、端盖/轴件/转子不在自主写集，若显示输入不足先报告给负责人协调。手写注释中文，原创SVG确定性导出PNG，不生图；版本保持MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。

实际读取AGENTS、治理1.2/5.1、美术入口/PLAN与Minecraft modding/testing/resource-pack技能，复用既有技能和证据，不重复全量构建或旧测试矩阵。

## 阶段一：只读定位

原SVG作者`reactor_svg_impl`只读核壳面重复纹理/UV与按尺寸降密度的最小显示路径，报告具体资源/客户端入口与是否需要缺失接口；原消费者`reactor_ct_assets`只读核窗口真实OBJ框/玻璃、blockstate/尺寸/方向、现有连纹参照与最小客户端连接路径。此阶段不写生产/候选源码、不生成、不测试/构建。可交简短消息，尚无需长报告。

负责人核实际文件后补精确写集与短显示设计；优先在已有状态与客户端显示消费内实现，不修改共享逻辑，不客户端猜运行成型。素材候选放独立新目录并先实际复看，原07B/R1工具、来源、制品和证据只读保存。

## 验证与交回

资源只做新SVG/PNG、真实UV/模型与安装引用小核对；若新增客户端连接行为，仅针对同面邻接、不同尺寸/方向/端部、未成型退路与正常资源生命周期做必要定向检查，不写镜像测试。一个最终增量assemble、旧R1 JAR先备份、对应资源/class-JAR核对及一次独立窄审；不clean/rerun/build、不旧存档兼容或全量GameTest。最终游戏观察继续交用户原存档复看，中大型疏线和连窗各自记录。

## 阶段二：已核最小显示设计与写集

只读确认：现有panel每轴向方块重复铜缝与亮刻线；PIECE可解尺寸及截面位置，同一middle piece沿长轴重复，没有机组内z同步。本批用世界长轴坐标与MACHINE_FACING铺排纯装饰相位，d5周期2、d7周期3；搬到另一世界位置时相位可能改变，这是材质铺排，不推断或伪造运行状态，不为装饰新增网络/服务端字段。保留d3原quad和R1端盖全部来源/资源。

窗口是四条宽0.14格、厚3/16格的实体框及内缩0.04/0.08格的玻璃盒；仅合法MIDDLE四轴心位置允许窗口。邻接只接受双方真实WINDOW、LOCATED=true、相同合法PIECE、MACHINE_FACING及FORMED相等，沿长轴正负一格；不连接横向、独立/未知窗。合法首尾端盖必须为普通壳，截断窗口链。本规则只消费既有显示状态，不声明验证权威owner。连接端撤下整条实体横框及侧壁，扩展玻璃到共享边并去共享玻璃端面；长框和首尾框保留。物品/Ponder/缺数据/未知状态返回原模型。

原SVG作者精确写集：新目录`tools/art-assets/turbine-casing-art-07-r2/`内原创64×32稀疏panel atlas SVG/导出PNG、只读旧来源的工具与mapping、generated实际模型预览、index/README；`docs/art/reports/ART-TURBINE-07B-R2-ASSETS.md`和`build/reports/art/ART-TURBINE-07B-R2/`。atlas上行d5两块16×16相位，下行d7三块16×16相位，余区钢色填充；带相位少量短铜/刻线，无带相位整片钢色。仅安装新`src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_casing_07_sparse.png`；不改旧PNG、OBJ、MTL或JSON。先交相同8格长度的三档旧/新真实OBJ预览，负责人实际复看后通知安装，不构建。

原消费者精确写集：新增`src/main/java/com/iksxh/create_nuclear_industry/turbine/client/TurbineCasingAppearanceModel.java`、`TurbineWindowConnectedModel.java`、`TurbineAppearanceClientEvents.java`；新增同包路径下两份专属`src/test/java/.../TurbineCasingAppearanceModelTest.java`与`TurbineWindowConnectedModelTest.java`；报告`docs/art/reports/ART-TURBINE-07B-R2-CONSUMER.md`及同批证据目录。独立Dist.CLIENT MOD事件在准备前幂等登记两block包装，不写原共享入口。壳面包装仅转换d5/d7既有casing sprite的UV至atlas对应16像素格，端盖/其他sprite和d3逐字保留，ModelData带尺寸/相位。窗口包装保存两位轴向邻接mask并重建连接几何，继续原sprite、透明RenderType、厚度/光照，getQuads不读世界；不改18份原OBJ或碰撞/放置。未知/无数据均降级原模型。Sprite entry按Catnip重载更新，不缓存跨重载atlas对象，包装不改共享quad顶点数组。

消费者先做真实几何/连接回归并定向检查，素材等待负责人复看安装；两路收尾后由消费者备份R1 JAR、执行唯一最终定向检查和增量assemble、核新资源及class条目并停写。独立审查复用旧原始证据，仅审本批差异。若原生API或素材需求实际超出以上五源/测试及一PNG写集，先报告，不擅自扩大。

## 阶段二补充：连接玻璃连续采样

实际原玻璃前后大面UV均为0..1，旧16×16 PNG有色区仅U=[4,12)/16、V=[3,12)/16；只延伸mesh仍会把透明边距重复在每一格，使蓝色玻璃看起来断开。因此仅邻接mask非零的两张玻璃大面，在新wrapper内将原UV映到有色texel中心U=4.5/16..11.5/16、V=3.5/16..11.5/16，避开透明边缘采样。单窗/mask0/未知/物品/Ponder原quad保持；PNG/MTL、色与alpha、厚度、采光和共享逻辑不改，不新增窗口资源。

首次8/8 GREEN已在暂停消息到达前结束，原记录和原UV mesh保持。消费者仅调整该UV并更新原窗口几何用例，窄复测受影响窗口类，另名保存新mesh；不再跑已通过壳面测试。作者以最终实际mesh+原PNG/MTL绘連窗预览，负责人复看后执行唯一增量assemble。

## 本批内部试用交付

原作者与消费者停写，独立窄审无必改项。负责人已实际复看最终模型/连窗并完整读原始检查、报告与审查，当前5源和JAR身份核对通过。最终壳4复用与最新窗4通过，唯一assemble exit0/1秒；当前JAR2,616,697字节/SHA b1c3c69c29dce947b99ccd0f0a187e9689958217a2f3727c80487d9033cd8d88。详细路径、绑定和真实客户端复看操作见[07B-R2候选](../ART-TURBINE-07B-R2-CANDIDATE.md)。内部交付结束，用户原存档游戏视觉门仍待；未合main或改其他范围。
