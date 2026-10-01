# EXT-ART-04：铅粒与锡粒素材交付

**候选基线：** `0d38c99fe8d3e041c6148057fc4f62827b9e45b4`，工作树已有 EXT-ART-03A 两板未提交修改。
**范围：** 仅新增铅粒、锡粒两张纹理源稿及生成/游戏 PNG，并将两条资源路径接入美术清单与导出、验证管线；未实现物品注册、模型、语言、配方或游戏逻辑。

## 素材与管线

- 新增 `tools/art-assets/sources/item/{lead_nugget,tin_nugget}.svg`。两者为16×16整数矩形、6色、透明外圈，采用紧凑小金属碎片轮廓；铅为冷暗灰，锡为亮银灰，保留左上高光和右下暗面。原尺寸、浅/深底与相邻铅锡锭、粗矿、板材对照见 [nugget-preview.png](./EXT-ART-04/nugget-preview.png)。已查看渲染像素和对照预览。
- `manifest.json` 现有56条记录：51条历史白名单路径、两张板材、两种金属粒，以及 lapis 工具候选；新粒没有历史基线记录。`palette.json` 仅追加对应两套有限色板。
- `pipeline.py` 固定历史白名单仍为51项，明确新增白名单为两板+两粒4项；接受导出安装前51项、既有板材阶段53项或完整55项集合，仍拒绝其他路径及错误映射。`verify.py` 保留非法 SVG、路径、映射、透明声明拒绝与失败不写保护，并只向 EXT-ART-04 写本次证据。
- `README.md` 同步素材管线计数、使用方式及历史基线说明。baseline 文件未改写。

## 字节与验证证据

开工时已逐字节记录现有53张游戏 PNG，快照位于 [pre-existing-53-game-png-hashes.json](./EXT-ART-04/pre-existing-53-game-png-hashes.json)。两张板材 SVG 导出图和游戏图均保持在任务指定哈希：

| 路径 | SHA-256 |
| --- | --- |
| `lead_plate.png`（generated 与游戏纹理） | `9961b34f836b8ea85c6d4a2fa6334ffdb009c9392825aff1d06cae445c10dac7` |
| `tin_plate.png`（generated 与游戏纹理） | `2f84e0eebbf0f2fa278539e25095e9c5cfcd17f0b4f8c6aa21535e0f60624672` |
| `lead_nugget.png`（generated 与游戏纹理） | `42974d703c62164dab85cdae2b7d9aca959d8da416a82b8a9e99eb7fb4645988` |
| `tin_nugget.png`（generated 与游戏纹理） | `ce89fb42afc7f23082d9cb8b986972461d0d9470bd3ad61ef4d1cd7f8a8b36d0` |

使用捆绑 Python 3.12.14 / Pillow 12.3.0，并为所有 Python 调用加 `-B` 和 `PYTHONDONTWRITEBYTECODE=1`。正式验证器命令为：

```powershell
$env:PYTHONDONTWRITEBYTECODE='1'
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/verify.py
```

正式验证器退出码为 `0`。其证据 [verification.json](./EXT-ART-04/verification.json) 记录51项固定历史基线、56条 manifest 记录、55条最终游戏 PNG 路径；默认导出两次、安装两次一致；新增图片无伪造基线；冷却剂原图及四个批准样稿保留；SVG 像素、alpha 与有限色板检查通过。六种非法 SVG/清单输入的子进程均按预期退出 `1`，且验证器确认失败不写任何输出；具体命令和 stdout/stderr 在 [commands.json](./EXT-ART-04/commands.json)。

开工时53图的独立快照检查命令退出码为 `0`，证据见 [pre-existing-snapshot-check.json](./EXT-ART-04/pre-existing-snapshot-check.json)；两板哈希与任务卡所给值完全一致。另将该快照临时移出 EXT-ART-04 目录后再次运行正式验证器，退出码仍为 `0`，然后通过 `finally` 恢复原快照，证明正式 `verify.py` 不依赖被忽略的报告目录内容；细节见 [verification-independence.json](./EXT-ART-04/verification-independence.json)。临时文件已恢复，无 `.pyc`/`__pycache__` 残留。

管线导出验证和像素预览不等于 Minecraft 客户端外观验收；未运行 Gradle、GameTest 或客户端。本任务交付后停止所有素材写入，A 可进入其获准的构建验证窗口。

PM 后续文档复核后，仅对 `tools/art-assets/README.md` 补齐 Python 示例的 `-B`、将预览页数更新为9，并将预览查看说明收窄到历史报告范围及本轮实际查看的粒子对照图；此文档修正未运行导出、验证、Gradle或游戏。

## 实际使用的技能

- `minecraft-modding`：核对本任务只交付两张素材与路径接入，不新增注册或运行时逻辑；沿项目锁定的 Minecraft 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82 范围工作。
- `minecraft-testing`：将资源像素、透明度、固定路径、失败不写与重复导出列入离线验证；该资源任务不启动 Gradle / GameTest。
- `minecraft-resource-pack`：采用游戏 item texture 的16×16 RGBA PNG、1:1 像素和既有资源路径，并用离线纹理预览核对。
- `superpowers:brainstorming`：按任务卡已冻结的小金属碎片设计边界执行，没有扩展物品身份或玩法范围。
