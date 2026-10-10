# ART-REACTOR-02A 连接纹理资产原型

本目录为14个现有反应堆面sprite准备Create `AllCTTypes.RECTANGLE`资产。每类16份可编辑的16×16整数rect SVG，共224份；严格渲染为224张16×16 RGBA图块，再按锁定索引粘贴成14张64×64图集。PNG只来自SVG，不在Python中绘制游戏图块。

本批不安装游戏资源，不改模型或Java，不扫描反应堆或接入客户端成型状态。当前玩法固定5×5×5；6×5×8和9×7×5只用于尺寸适配假设，不能据此宣称已支持可变尺寸。

## 来源与材料

只读参考同工程 `tools/art-assets/sources/block/` 的14份既有SVG，外壳和冷热口沿用ART-REACTOR-01未提交候选的暖灰钢、铅灰压板、浅暖灰混凝土及红横标/蓝竖标。仪表、换料口和控制棒驱动器沿用已有局部功能图案，周围背景统一到本批暖灰材料。没有复制第三方图像，没有新增旋钮、动态读数或运动状态。

只有面外沿保留连续钢框，角螺栓仅在两个外沿相交时出现；面内没有重复的箱体粗框。窗口源稿以未覆盖区域保留原8×8透明孔（x/y均为4至11）；其他13类实体面全不透明。每格最多16种RGBA颜色（含透明），每套色板最多16种实色，alpha仅0/255。

`build/reports/art/ART-REACTOR-02/seed-sources.py`记录一次机械展开过程，仅作为制作证据。正式导出不读取该脚本或旧SVG；直接编辑本目录224份SVG后重新运行导出器即可。

## 实际命令

从美术工作树运行，无需安装依赖；使用现有捆绑Python/Pillow，`-B`避免写入共享目录的字节码：

```powershell
Set-Location -LiteralPath 'E:/MyMC/NewMod/Create_NuclearIndustry-art-studio'
$env:PYTHONIOENCODING = 'utf-8'
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/reactor-ct/export.py
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/reactor-ct/export.py --check
```

脚本按自身路径定位，允许从其他cwd调用绝对脚本路径；输出路径固定。默认先校验全部SVG、色板和mapping，再编码所有图块、图集和预览，最后只写字节变化文件。写入失败时恢复本次触及文件。预览字体缺失时降级，`--check`不加载字体或预览。没有`--install`，也没有输出路径参数。

`--check`只读比较SVG预期PNG字节与已有224图块、14图集，拒绝缺失、变化或额外生成文件。对输入的内存覆盖仅供检查脚本模拟非法输入，不能扩大实际源路径或输出路径。

## 锁定索引

Create `6.0.10-280` RECTANGLE为4×4等格，`index=column+4*row`，孤立格是12。列顺序是左右都不连/仅右/双侧/仅左；行顺序是仅下/上下/仅上/都不连。不采用OMNI或47格CTM。

| 上下连接 \ 左右连接 | 都不连 | 仅右连 | 双侧连 | 仅左连 |
| :--- | ---: | ---: | ---: | ---: |
| 仅下连 | 0 | 1 | 2 | 3 |
| 上下连 | 4 | 5 | 6 | 7 |
| 仅上连 | 8 | 9 | 10 | 11 |
| 都不连 | 12 | 13 | 14 | 15 |

源稿名称`tile_00.svg`至`tile_15.svg`就是对应index。顶面、底面和侧面各自按本面的局部上/下/左/右选择格，不把世界Y方向直接套到所有贴图面。

## 后续消费接口

`mapping.json` schema_version为1，记录`ct_type: RECTANGLE`、`tile_size: 16`、`sheet_size: 4`、孤立格12、16种布尔连接上下文，以及下列14项完整sprite ID和固定相对路径：

```text
reactor_casing_side / reactor_casing_top / reactor_casing_bottom
reactor_hot_port_side / reactor_hot_port_top
reactor_cold_port_side / reactor_cold_port_top
reactor_window
reactor_instrument_port_side / reactor_instrument_port_top
reactor_refueling_port_side / reactor_refueling_port_top
control_rod_drive_side / control_rod_drive_top
```

对每个`<name>`：original为`create_nuclear_industry:block/<name>`，target为`create_nuclear_industry:block/reactor_ct/<name>`，源目录为`sources/<name>`，图集为`generated/<name>_ct.png`；图块为`generated/tiles/<name>/tile_<index两位>.png`。target只是后续接口，本批不创建相应game路径。

主PM接入合同明确未来game图名为`textures/block/reactor_ct/<name>.png`（无`_ct`后缀），由后续接入卡消费本批`generated/<name>_ct.png`。工具文件名与game文件名不同是明确接口，不是漏掉或重命名了原sprite。

逻辑接入者须提供可靠的成型有效性、owner、真实origin/bounds与合法共面成员。相邻但owner不同要保留各自边框；数据未知或结构失效时保留original单块贴图。此工具的离线`face`仅模拟选格，不能证明任何游戏接入或同步实现。

## 输出及预览

- 本目录`generated/`：14张64×64图集和224张16×16图块。
- `build/reports/art/ART-REACTOR-02/indices.png`：14类16格，原尺寸与最近邻4倍并列。
- 同目录`wall-preview.png`：5×5混合墙、内格功能件、顶底长方形铺面与2×2外沿。
- `geometry-preview.png`：闭合5×5×5、6×5×8、9×7×5三面及正交墙面。
- `owner-preview.png`：不同owner结构实际接壤、原游戏单块与连接候选对照、缩小/灰度/降亮55%模拟。

预览的透明孔与双色后方对照合成，不修改图集alpha。几何使用统一顶点投影闭合三面，没有使用V形展开片。离线明暗倍率只作用于预览，不证明客户端光照、管道遮挡或实际成型状态消费。

本批范围明确排除Gradle打包、GameTest、客户端和历史全量导出。真实游戏接入后的相关编译、打包与人工视觉门由后续任务独立承担。
