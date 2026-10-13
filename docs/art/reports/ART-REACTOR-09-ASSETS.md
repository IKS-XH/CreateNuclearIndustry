# ART-REACTOR-09 素材与安装记录

候选将驱动器表现为钢色导轨、轴承桥和中央方杆通道；换料口使用连续铜色舱盖、铰链与锁扣。铜色仅作为视觉身份，不表示配方中含铜。四个基础纹理均为16×16整数像素SVG；对应CT纹理沿用Create RECTANGLE 4×4索引、64×64图集与isolated index12。

候选及可浏览预览位于 [reactor-device-art-09](../../../tools/art-assets/reactor-device-art-09/README.md)，首页为 [index.html](../../../tools/art-assets/reactor-device-art-09/preview/index.html)。工具目录含4张基础SVG、4类各16张CT SVG、8张本地PNG、映射/色板及导出器。预览展示基础旧/新对照、12×12驱动器/舱盖混排近远图、CT图集与方杆插入/拔出遮挡。二维示意中的虚线椭圆只显示冻结本格命中椭球的X/Z投影（半轴0.35/0.075、中心x=0.5/z=0.0875）；法向半轴0.10未绘出。方杆和端箍截面分别为0.6格和0.65格。素材及页面均不是客户端或鼠标验收。

为让旧/新对照不随将来正式PNG替换而变化，四张旧基础PNG只读快照保存在 `tools/art-assets/reactor-device-art-09/references/original/base/`；文件SHA-256已登记在 `mapping.json`，最终预览检查确认四项哈希一致。混排和遮挡SVG内嵌本批PNG数据，避免浏览器把SVG作为图片打开时屏蔽外部引用。

| Sprite | 基础SVG / 本地PNG | CT SVG / 本地图集 | 正式路径 |
| --- | --- | --- | --- |
| `control_rod_drive_top` | `sources/base/control_rod_drive_top.svg` → `generated/base/control_rod_drive_top.png` | `sources/ct/control_rod_drive_top/tile_00..15.svg` → `generated/reactor_ct/control_rod_drive_top.png` | `textures/block/control_rod_drive_top.png`；`textures/block/reactor_ct/control_rod_drive_top.png` |
| `control_rod_drive_side` | `sources/base/control_rod_drive_side.svg` → `generated/base/control_rod_drive_side.png` | `sources/ct/control_rod_drive_side/tile_00..15.svg` → `generated/reactor_ct/control_rod_drive_side.png` | `textures/block/control_rod_drive_side.png`；`textures/block/reactor_ct/control_rod_drive_side.png` |
| `reactor_refueling_port_top` | `sources/base/reactor_refueling_port_top.svg` → `generated/base/reactor_refueling_port_top.png` | `sources/ct/reactor_refueling_port_top/tile_00..15.svg` → `generated/reactor_ct/reactor_refueling_port_top.png` | `textures/block/reactor_refueling_port_top.png`；`textures/block/reactor_ct/reactor_refueling_port_top.png` |
| `reactor_refueling_port_side` | `sources/base/reactor_refueling_port_side.svg` → `generated/base/reactor_refueling_port_side.png` | `sources/ct/reactor_refueling_port_side/tile_00..15.svg` → `generated/reactor_ct/reactor_refueling_port_side.png` | `textures/block/reactor_refueling_port_side.png`；`textures/block/reactor_ct/reactor_refueling_port_side.png` |

上述正式路径均相对 `src/main/resources/assets/create_nuclear_industry/`。素材候选阶段没有安装PNG；负责人追加阶段二A安装授权后，已只安装表中八张PNG。没有修改模型、Java、旧资产或共享导出器。

实际执行的捆绑Python入口为 `C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe`，参数使用 `-B -X utf8`。68张来源的最终候选检查在 `build/reports/art/ART-REACTOR-09/assets/check.log` 记录：

```text
CHECK_OK svg=68 base_png=4 atlas_png=4 atlas_size=64x64 ct=RECTANGLE isolated=12 scope=8-local-targets
exit=0
```

该检查核对SVG整数rect与色板、尺寸、CT格数、index12一致性，并逐字节核对8张本地PNG与SVG渲染结果。之后仅修复了预览资源引用与说明，68张SVG及8张候选PNG未改，因此未重复运行素材检查。预览生成实际命令为 `C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe -B -X utf8 tools/art-assets/reactor-device-art-09/export.py --preview-only`，记录于 `build/reports/art/ART-REACTOR-09/assets/preview-only-final.log`，exit 0。单独的预览检查记录于 `preview-final-check.log`，exit 0：混排SVG含144张内嵌PNG图像，遮挡SVG含2张；椭圆投影说明、4张旧图引用及SHA均有效。

首次执行时的失败原始输出保留在 `first-seed-failure.log`、`second-seed-failure.log` 和 `first-export-failure.log`：前两次seed因局部颜色变量名不一致失败，修正后seed exit 0；首次导出因基础图与index12孤立格不一致失败，改为从同一矩阵导出后正常。此后实际导出结果在 `export.log`，exit 0。早期预览的外链资源空白问题经负责人浏览器指出后，改为内嵌PNG数据并固定旧图快照，预览引用检查exit 0。

本批实际读取并应用：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`；修复预览引用故障时使用 `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/systematic-debugging/SKILL.md`。版本与项目锁定版本保持。负责人已实际浏览器复看基础对照、近远混排、遮挡示意和CT图集，素材辨识方向清楚；该预览审阅不是客户端验收。八张PNG现已安装为试用候选，客户端材质与操作视觉仍待验收。

## 正式PNG安装记录

按新增安装授权先把八张原正式PNG逐字节备份到 `build/reports/art/ART-REACTOR-09/assets/baseline/`，再用捆绑Python的 `-B -X utf8` 安装候选。原始命令输出及退出码见 `build/reports/art/ART-REACTOR-09/assets/install-8.log`（exit 0）：`BASELINE_OK files=8 byte_identity=8`、`INSTALL_OK files=8 byte_identity=8 dimensions=8`。逐项来源、正式目标、基线备份、文件大小及候选/基线/安装SHA-256列于 [install-manifest.json](../../../build/reports/art/ART-REACTOR-09/assets/install-manifest.json)；安装后八个正式文件均与generated候选字节一致，基础图16×16、CT图集64×64。原正式图备份SHA与此前记录的四张基础图快照SHA相符。此项只确认文件安装身份，不代表游戏内材质或操作视觉验收。未运行Gradle，也未改其他资源。

安装收尾元数据修正：消费者指出`mapping.json`仍保留安装前状态。本次仅把mapping、工具README及预览页状态同步为“八张PNG已授权/安装试用、客户端实机视觉未验收”；没有改68张SVG、8张PNG、安装清单或任何源/校验/安装结果，也没有重跑导出或检查。
