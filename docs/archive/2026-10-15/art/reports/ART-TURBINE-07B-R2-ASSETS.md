# ART-TURBINE-07B-R2 资产交付记录

本批按已批准的B机壳方案为d5、d7制作稀疏钢面纹理候选。原创源为64×32整数矩形SVG atlas，确定性导出单张PNG；d5使用上排两相位格、周期2，d7使用下排三相位格、周期3，轴向保持16像素/世界格。d3现行纹理和R1端盖UV/资源维持原样。相位仅作外观变化：`floorMod(worldPos·machine_facing.getOpposite(), period)`，不要求与机组前后同步。

新增正式资源仅为 `src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_casing_07_sparse.png`。安装前的候选PNG与mapping记录的源哈希一致；安装后字节核对为219字节、SHA-256 `aff06b77869abb21ba6939a2300d9ba07c7c06c7684866ddcaf5e3459be4c741`。原07B工具、3张旧机壳PNG和206张既有成型OBJ共210项由源指纹保护，最终小检查记录未发现变化。没有改写旧R1端盖或旧素材。

八格机壳预览来自实际OBJ，分别展示d3、d5、d7旧/新对照；d3新旧相同。观察窗预览使用消费者提供的实际mesh/UV证据 `build/reports/art/ART-TURBINE-07B-R2/consumer/window-mesh-glass-uv.json`，包含单窗及三窗连接前后对照。负责人已在浏览器复看稀疏纹理和连接mesh预览。该复看属于离线候选展示，不等同游戏客户端视觉验收。

本批检查使用仓库指定的 bundled Python。生成命令：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/turbine-casing-art-07-r2/build_candidate.py
```

生成退出码为0；原始输出见 `build/reports/art/ART-TURBINE-07B-R2/assets/generation-output.txt` 和 `generation-exit.txt`。最终小检查命令：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/turbine-casing-art-07-r2/build_candidate.py --check
```

检查退出码为0，输出：`CHECK_OK svg_rects=109 png_svg=64x32 phase_tiles=5 casing_previews=6 window_previews=3 protected_inputs=210; installed_png=matches`。原始记录在 `final-check-output.txt` 与 `final-check-exit.txt`。安装字节记录在 `install-byte-check.txt`，结果为 `byte_equal=True`。

素材作者未运行Gradle、测试或游戏；消费者负责唯一候选构建。客户端内的纹理周期、连接窗外观和整体游戏视觉仍待用户复看。未使用生图工具。

本批实际读取并应用的技能：

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/brainstorming/SKILL.md`
