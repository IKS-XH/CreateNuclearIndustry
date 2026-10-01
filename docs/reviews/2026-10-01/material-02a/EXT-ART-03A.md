# EXT-ART-03A 两板方正化交付报告

**候选基线：** `0d38c99fe8d3e041c6148057fc4f62827b9e45b4`（`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`）
**结果：** 执行者已交付；待项目经理审核及用户客户端视觉确认。
**版本基线：** Minecraft 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82。

## 改动

铅板和锡板已改为平直边的近方形薄板，主体不透明区域为 12×11 像素，宽高比 1.0909；顶角各收一像素，底边保留暗色边。材质仍使用原有限定色板，左上受光，铅色较暗、锡色较亮。原稿副本保存在本目录的 `lead_plate.before.svg` 与 `tin_plate.before.svg`；清晰排版的放大前后对照见 [`comparison.png`](./EXT-ART-03A/comparison.png)。

本次仅改动以下候选文件：

- `tools/art-assets/sources/item/{lead_plate,tin_plate}.svg`
- `tools/art-assets/generated/item/{lead_plate,tin_plate}.png`
- `src/main/resources/assets/create_nuclear_industry/textures/item/{lead_plate,tin_plate}.png`
- `tools/art-assets/preview.png`、`preview.html`、`previews/items-1.png`、`previews/items-2.png`
- 本报告及 `build/reports/extension/EXT-ART-03A/` 下的原稿副本、临时核验入口和证据

## 核验

已实际读取并应用 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack` 技能（入口均为 `C:/Users/IKSXH/.codex/skills/<skill>/SKILL.md`）。本项是 Minecraft 1.21.1 资源图更新；没有修改 Java、注册、配方或资源模型，也没有运行 Gradle、客户端或服务端。

使用系统工作区 Python 3.12.14 / Pillow 12.3.0。系统 `py.exe` 启动被拒绝访问，后续改用 `C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe` 并始终加 `-B`，避免生成缓存。

- `python.exe -B tools/art-assets/pipeline.py --install`：退出码 0；完成本轮 SVG 渲染与安装。
- `python.exe -B build/reports/extension/EXT-ART-03A/verify-03a.py`：退出码 0。临时入口复用现有验证的源稿解析、独立逐像素比对、透明边框/调色板检查、历史样稿检查和安装复现检查；跳过会临时改写清单及旧冷却剂源稿的拒绝输入实验，避免触碰任务写集。其记录的两次 `export.py --install` 子进程均退出码 0，输出 PASS。
- `python.exe -B build/reports/extension/EXT-ART-03A/finish-evidence.py`：退出码 0，整理写集、路径及哈希证据。

核验结果：两张图均为 16×16 RGBA，alpha 仅含 0/255，最外一圈透明，每图使用 6 种原色板颜色；SVG、generated PNG 与游戏 PNG 解码像素相同。历史 51 张游戏 PNG 的 SHA-256 与运行前记录一致；最终游戏 PNG 路径集恰为 53 项（历史 51 项加两张板材）；其他分组页、manifest、palette、baseline 和正式导出/验证脚本均保持原字节。详细记录见 `final-evidence.json`、`pre-change-evidence.json`、`verification.json` 与 `commands.json`。

关键 SHA-256：

| 文件 | SHA-256 |
| --- | --- |
| 铅板 SVG | `4ec7d17df54b1645c707e60e448aec1452480d3a91a2a72c48e1e93b9dedb101` |
| 铅板 generated / 游戏 PNG | `9961b34f836b8ea85c6d4a2fa6334ffdb009c9392825aff1d06cae445c10dac7` |
| 锡板 SVG | `52953cc01af0d1683f604861125843a0da9187c01b6f05b2e24fd7cbd4e8c7ef` |
| 锡板 generated / 游戏 PNG | `2f84e0eebbf0f2fa278539e25095e9c5cfcd17f0b4f8c6aa21535e0f60624672` |
| `preview.png` | `cc5448ae3e19eaee249141deb26d45dac85b0f4bc76e6ea991ebd16e752319a6` |
| `previews/items-1.png` | `4ead3913686f29c011d2e6bc910f1e68e02bf40f362afc2ff713ba493e87d269` |
| `previews/items-2.png` | `75e520722e1c95ac8a9d4b41ec6b58b8f96051bc7ebb5a6ec153635d7c029211` |

执行中第一次未使用 `-B` 的管线调用意外产生过 `tools/art-assets/__pycache__/`；项目经理随后备份其两份 pyc 至本报告目录并移除了缓存目录。后续所有 Python 命令均使用 `-B`；最终 Git 状态未见缓存目录或其他写集外改动。

## 视觉门

已实际查看原尺寸及放大前后对照、`items-1.png`、`items-2.png` 和 overview。预览中两板现为接近方形的直边板件，旧稿为明显横向扁长的圆滑锭状。客户端内的实际视觉仍待用户确认；本报告不代表客户端人工验收通过。
