# ART-TURBINE-07B：B分段轮舱机壳试用版

2026-10-12用户在07A三稿预览中明确要求“先按照第二版做一个试试吧”，确认B分段轮舱方向并授权制作游戏试用版。本批只处理汽轮机机壳表面；厚钢束带、窄铜缝、清楚的凹槽/压边及钢铜明暗来自已选B，不重开选稿。三档3/5/7格八棱形状、既有长度、方块角色和成型规则保持。连接利用现有模型/UV模块延展，不增加动态CT逻辑、枚举新尺寸或修改运行状态。

## 角色、工作区与技能

负责人为用户任命的美术对话，依据治理1.2制定本卡；具体制作交原SVG作者`reactor_svg_impl`，只读映射审计交`reactor_ct_audit`。两者没有负责人或Git权限，不再委派。唯一写入树为`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，开工实物HEAD为`e5426bd`，同一活动树沿用，不开分支或刷新存档。主目录、逻辑树、07A三稿及旧候选/日志只读，保留已有未提交文档。

读取用户最新AGENTS、美术README/PLAN、治理1.2/5.1、`C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-resource-pack}/SKILL.md`；通用技能的反复审批/全量构建/新Agent/Git步骤服从本项目已授予的专项分工与精简验证规则。锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，未升级技术栈。手写注释中文。

## 外观与生产映射要求

- 同一B钢铜调色板覆盖未成型、物品及成型机壳；束带高光、暗内沿与面板铆钉有厚度，不能因接续消除明暗或变浅。环向分段的方向在顶、侧及斜面一致。
- 机壳内表面/薄壳接边用克制的暗钢；端盖应有B厚钢法兰与铜内嵌片，不把轴承、输出轴或控制器的共用装饰素材误当成专属端盖。
- 现有OBJ/MTL在不同设备间复用材料名；正式写集按实际机壳消费者确定，独立机壳材质优先，避免全局替换波及窗口、接口、控制器和叶轮。UV适配仅改变贴图坐标/材质引用，所有顶点、法线、面绕序、占格、JSON阴影/裁切设置保持。
- 低分辨率资产以原创16×16整数rect、crispEdges和有限RGB SVG为源，PNG只由SVG确定性导出供游戏读取；不生图、不用旧PNG反绘新源。07A源稿保留，只读复用已选B。

## 阶段一：素材与精确映射候选

当前派发给原SVG作者的写集仅为新增`tools/art-assets/turbine-casing-art-07/`下SVG、同源小型导出/安装工具、mapping、generated、实际模型贴图预览、README；报告`docs/art/reports/ART-TURBINE-07B.md`，证据`build/reports/art/ART-TURBINE-07B/`。Python固定`C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe -B -X utf8`。旧素材/导出器只读。

先读实际机壳模型引用与OBJ UV，结合只读审计结果提交最小精确正式资源映射。预览必须由实际机壳OBJ/MTL/UV加新SVG材质投影，不能继续用07A抽象形状代替接入后的效果；包含未成型、三档成型的代表壳面/斜面/端盖，分段衔接可见。若需UV改变，明确共用轴向/环向坐标算法及保持几何的检查。候选完成后由负责人实际复看并补列正式写集；当前没有正式资源、Java、模型生成器或Gradle写/运行权。

## 阶段二：安装与一次打包

负责人完成实际映射核对后在本卡追加精确资源清单和实施范围，再由同一作者安装候选。不改任何Java、注册、配方、配置、语言、blockstate角色、玩法、碰撞、库存或动力；不做窗口/端口/叶轮重置，不启动Minecraft或写存档/Git。资源核对覆盖整数SVG/PNG一致、MTL引用可解析、实际UV方向/连接及几何保持；一次增量`./gradlew.bat assemble --console=plain`，不clean/rerun/build/JUnit/GameTest。只有本次实际失败或整改影响的部分复验。

最终由独立审计者同一轮只读窄审检查实际资源差异及已有证据，不重复导出/测试/打包。负责人读取原始检查/构建记录并交用户在美术树现有存档复看；离线预览和资源校验不代替真实游戏视觉。仅美术树交未提交试用版，主分支集成由PM或用户管理。

## 已核最小映射与精确实施边界

原审计者已返回实际UV/MTL证据，负责人读取并核源：直接换共享PNG会影响其他部件，水平壳U/V与竖壳不同。当前HEAD已随PM锅炉验收文档推进为`6042fd2`，与预读`e5426bd`的差异只有五份锅炉文档，汽轮机输入保持。07B冻结以`6042fd2`及实际未提交07A B源为基线。

明确采用独立机壳MTL和纯UV适配，精确候选/后续安装目标如下：

- 新增`src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/turbine_casing_07.mtl`，只保留原`casing/inside/edge/endcap`材料名。`casing`映射新B侧壳，`endcap`映射新B端盖；`inside/edge`保持原`block/turbine/turbine_casing_panel`。原`turbine.mtl`不改。
- 新增两张PNG：`src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_casing_07_panel.png`及`turbine_casing_07_endcap.png`，由新目录的两张16×16 SVG确定性导出。原`turbine_casing_panel.png`、`turbine_casing_endcap.png`及其派生资源只读。
- 仅206个已有`models/block/turbine/mesh/casing_d{3,5,7}_{front,middle,rear}_x*_y*.obj`（三档26/66/114）与6个`turbine_casing_unformed*.obj`允许改变`mtllib`和必要`vt`；候选工具生成确切212路径清单，安装只读该清单。不得新增整机尺寸或改变`v/vn/f/usemtl/o`记录。现行六个未成型模型的UV按其既有预旋转关系处理，不能假定所有面局部Z都是轴向。
- 外侧壳的U统一沿标准模型局部Z，并保留真实0..1轴向位置，端层3/16裁切不重新拉满；环向V沿完整八棱面切向一致，跨裁切单格保持相位。B束带采用每轴向模块重复的明暗钢带和窄铜缝，07A额外绘出的Z=2/4/6环带不复制为新的凸起几何或奇偶状态。
- 端盖用完整d3/d5/d7截面的XY归一化UV呈现同一径向钢铜内嵌纹，不逐格重复中心图形；属于贴图坐标适配，完整几何保持。预旋转未成型端截面沿原模型局部截面处理。

不改blockstate、包装JSON（含粒子材质/阴影/裁切）、旧生成器、窗口/端口/控制器/轴承/叶轮OBJ或其他生产文件。作者先交含实际OBJ/UV的候选预览、212路径与引用检查；负责人复看并明确通知后安装以上目标，再唯一增量assemble和读取JAR条目核资源一致。无需新测试或服务端前置；正式资源写权只对上述清单生效。

## 阶段二已准许

负责人已实际复看四份OBJ模型预览并完整读取候选小检查原始输出，最终退出0：两组SVG/PNG一致、212机壳几何记录保持、专用MTL与首页引用可解析。首轮预览的路径和裁切坐标问题已单独修正，随后将内嵌图像设为像素采样；失败截图及检查器title误判日志保留，不当游戏结果。模型使用前/中/后三模块组合观察连接，不代表三档完整设备的游戏截图。

现通知原作者只安装上述215项资源，安装前核mapping来源SHA、安装后核候选字节；旧build/libs制品另存本批证据，唯一增量assemble并读取JAR同一215条目。mapping基线保持6042fd2的原始输入，不从已安装资源重生成覆盖来源。作者交短报告后停写，由原审计者做一次只读窄审；真实游戏视觉仍待用户观察，未授权主树/Git集成。

## 游戏试用版已交

原作者完成215项安装及唯一增量assemble，4秒退出0，生产Java复用。负责人已读小检查、安装与打包原始输出：SVG/PNG一致，候选→安装及安装→JAR均215/215；实际JAR为2,601,212字节/SHA `328072b13f941705fc86e8d62df7b13e6b90b1f9a3fc3851c34cfa279380167c`，旧JAR备份保留。精确生产差异为212OBJ加专用MTL/两PNG，无Java、测试、构建脚本、旧生成器或其他生产素材修改。

唯一[独立窄审](../reports/ART-TURBINE-07B-REVIEW.md)未发现必改项，负责人完整读取；审计实际非UV记录与HEAD一致，代表顶/侧/斜面轴向U、front裁切相位、端盖整截面映射及六向逆变换成立，未重复生成/测试/构建。两位执行者停写。[试用说明](../ART-TURBINE-07B-CANDIDATE.md)交用户在美术树现有存档复看；本批内部交付结束，游戏视觉待验、未合main，不启动游戏或接续转子动画。
