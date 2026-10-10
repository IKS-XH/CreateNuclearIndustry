# ART-REACTOR-02A：SVG连接图集与多尺寸预览

> 执行方式：subagent-driven-development；实现交执行者，美术负责人只管理文档和审查。项目治理5.1优先于通用技能的固定全量测试、多层复审或Git提交步骤。

**目标：** 交付已批准反应堆连接纹理方案的原创SVG、Create RECTANGLE图集及多尺寸离线预览；此任务不实现游戏成型接入。

**本卡状态：** 资源/工具已交付冻结，定向自动门与一次独立规格/质量审查通过，必改项无。实际证据见[执行报告](../reports/ART-REACTOR-02A.md)及[独立报告](../reports/ART-REACTOR-02A-REVIEW.md)。用户视觉门、L1逻辑前置及02B真实接入各自保持，不据此宣布游戏CT完成。

**架构：** 14个既有面sprite各有16份16×16整数rect SVG，用共用严格渲染函数分别导出，再按锁定Create索引拼为64×64图集。专用工具只消费SVG并写新generated/证据目录，不安装游戏资源。

**技术栈：** MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；现有Python/Pillow。设计：[ART-REACTOR-02-DESIGN.md](../ART-REACTOR-02-DESIGN.md)。

## 授权、角色与基线

用户2026-10-09要求成型反应堆连接纹理，并回答“好”确认设计及主PM接口协调。美术负责人按专项权限派发资产段；执行者不是负责人/PM，不改任务状态、不派发子代理、不做任何Git写操作。

唯一工作区`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，每个shell命令显式workdir此路径；HEAD基线`6bcdbdd45d22029f7b60208bd14172e5c815ecad`。当前ART-REACTOR-01七项源稿/PNG/色板及美术文档为既有未提交候选，完整保留；本任务不能覆盖它们。主工程与逻辑候选只读，不创建或切换工作树。

用户本轮替换AGENTS，以最新用户指令为准；实际读取主工程`E:/MyMC/NewMod/Create_NuclearIndustry/AGENTS.md`最新副本及本工作树AGENTS、美术入口、治理1.2/5.1、本卡、设计与ART-REACTOR-01报告。最新主线换热器思索仍由主PM管理，本任务不写Ponder或关闭其他人工门。

实际读取并应用三项技能，报告用途和完整入口：

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`

## 唯一资产写集

目标14名（每名对应现有模型实际sprite）：

```text
reactor_casing_side
reactor_casing_top
reactor_casing_bottom
reactor_hot_port_side
reactor_hot_port_top
reactor_cold_port_side
reactor_cold_port_top
reactor_window
reactor_instrument_port_side
reactor_instrument_port_top
reactor_refueling_port_side
reactor_refueling_port_top
control_rod_drive_side
control_rod_drive_top
```

允许新增：

- `tools/art-assets/reactor-ct/sources/<14名>/tile_00.svg`至`tile_15.svg`（224份，每图16×16，坐标/rect均为整数）；按图集index命名。
- `tools/art-assets/reactor-ct/palette.json`：仅本批14名有限色板。
- `tools/art-assets/reactor-ct/mapping.json`：稳定sprite/图集/索引接口。
- `tools/art-assets/reactor-ct/export.py`、`README.md`。
- `tools/art-assets/reactor-ct/generated/<14名>_ct.png`（64×64图集）及`generated/tiles/<14名>/tile_00.png`至`tile_15.png`。
- `docs/art/reports/ART-REACTOR-02A.md`。
- `build/reports/art/ART-REACTOR-02/`本批预览、开工相关输入哈希、检查命令与实际输出。

其他全部只读，尤其现有game PNG、SVG、共用palette/manifest/export/pipeline、Java、JSON模型、语言、配方、Ponder、旧baseline及ART-REACTOR-01目录。负责人并行修改docs/art计划/卡片，不属于执行者写集。不得加入安装game目标、`--install`或扩展输出路径参数。

## 图集与视觉合同

`AllCTTypes.RECTANGLE`为4×4等格、每格16×16；index=column+4*row。列0/1/2/3分别是左右均不连/仅右连/两侧连/仅左连；行0/1/2/3分别是仅下连/上下连/仅上连/上下均不连。孤立格index12。不要套OMNI或通用47格CTM。

角/边/中间图块沿真实面边界铺设：未连接方向保留连续钢框，已连接方向撤掉重复粗框与角螺栓。中间保留克制材料接缝、避免每格仍像独立小箱。暖灰钢/铅灰压板/浅暖灰混凝土沿用首批，顶底封闭；尽量贴近Create的材料和构造语言。不可把整面图拉伸，不依赖奇数尺寸中心、固定5或某一块端口位置。

冷热口保留大红横标/蓝竖标与钢法兰黑密封。仪表、换料与控制棒驱动器保留现有可辨识功能图案，允许整理周围背景/框边，但不凭空画新旋钮、动态数值、运动状态。窗口保持原透明孔的用途，不用不透明面板堵窗；alpha只允许0/255，透明孔通过SVG未覆盖区域得到，其他13类实体面全不透明。每格最多16个RGBA颜色（含透明），色板实色最多16种，无渐变/抗锯齿/外部image/transform/filter/text。

可以只读参考现有SVG和Create缓存贴图的配色/螺栓搭接；不得复制第三方资产。源稿是可编辑整数rect SVG，PNG仅由SVG渲染而来，不能直接Pillow绘制游戏图块。批量变体可机械展开SVG矩形，但绘图几何和颜色必须体现在交付源稿内；导出器不可绕过SVG以Python像素图代替源稿。

## 输出接口

`mapping.json`记录schema版本、`ct_type: RECTANGLE`、`tile_size: 16`、`sheet_size: 4`、16种index/column/row及up/down/left/right布尔连接上下文、14项完整original sprite ID与对应target ID/源目录/图集相对路径。主PM已同步[接入合同](../ART-REACTOR-02-INTERFACE.md)，target ID固定为`create_nuclear_industry:block/reactor_ct/<14名>`（无`_ct`后缀），工具图集路径仍为`generated/<14名>_ct.png`；README写明此对应。它只是后续消费合同，本任务不创建game路径或修改模型。

导出器复用`tools/art-assets/export.py`的`render_svg(text, allowed_colors, (16,16))`；共用函数只支持16×16/16×64，不能扩大它。全部224源稿、色板与mapping先校验/编码后，才写生成物；仅写字节变化文件。默认生成图集及预览，`--check`只读比较SVG预期字节与已生成图块/图集；无安装入口。脚本路径由自身定位，现有字体缺失时可降级预览字体，不让仅检查图块受Windows字体依赖影响。所有新增手写代码职责、算法与非显然边界注释用中文。

README写实际可执行命令，图集原型/用途、SVG来源、索引对照、输出路径与检查方法。美术预览与客户端状态消费严格区分。

## 预览与必要验证

预览至少包含：

1. 每类16格索引页、原尺寸与最近邻放大；外壳/冷热口/仪表/窗口等同屏的面板墙与顶面/底面样例。
2. 同一结构的5×5×5、6×5×8、9×7×5几何等距/正交预览，准确闭合cube外表面，共享边/拐角正确；标明后两者为尺寸适配假设，非合法玩法尺寸声明。用角边中间格铺面，保持每块16×16密度，展示中间/偏移位置的功能件。
3. 相邻但owner不同的两结构接壤面保留各自边框；未成型独立方块与成型连接的前后对比。透明窗显示后方对照，避免预览把孔涂黑或堵住。
4. 缩小/灰度/降亮模拟；这些模拟不证明客户端光照、管道遮挡或实际成型接入。

按顺序执行并保留一次命令/输出：

- [x] 开工保存14张当前游戏参考图及首批7个源稿/产物的哈希，用于范围保持；只读核对14名对应现行模型与alpha，不重新全量扫所有PNG。
- [x] 完成224 SVG/14色板/映射/导出器，先检查16种上下左右上下文唯一完整、index与锁定API一致。
- [x] 默认导出，`--check`通过；一次重复导出后全部产物哈希保持一致。
- [x] 对代表性非法SVG和非法mapping各做一个内存/临时副本负例，确认写前拒绝且既有生成物不变；不破坏正式源稿。记录当次实际拒绝文本，不以推断分支文本冒充stdout。
- [x] 检查每格颜色/alpha、图集格位、边/角/一行/一列/长方形铺设及跨owner的纯离线选格结果；通过专用检查/直接断言即可，不另建框架，不伪造绘图TDD失败。
- [x] 实际打开自己生成的预览，确认闭合三面、尺寸标注与功能件辨识；交负责人看图后再冻结。
- [x] 相关旧输入哈希保持、Git差异精确写集及`git diff --check`通过。

本批只新增工具/资产准备，**不跑Gradle jar/build、GameTest、客户端或历史全量导出**。真实接入后再由单一执行者按逻辑侧卡片验证一次相关编译/打包；不能重复本批纯资源检查。

## 交付

报告列基线、实际技能用途、224源稿与14图集、mapping的消费接口、来源、检查命令/结果、预览绝对路径、范围保持及限制。资源与工具冻结后通知负责人，进行一次合并规格/质量独立审查；用户视觉与逻辑接口前置各自保持独立，不宣布游戏成型效果已实现。
