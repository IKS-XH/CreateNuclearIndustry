# ART-REACTOR-01 执行报告

## 交付状态

七项SVG、色板、专用导出器与离线预览已完成；负责人已复看并通过模型投影。本批七张游戏PNG已安装，唯一一次 `jar` 成功，候选等待独立规格与质量审查。基线为 `6bcdbdd45d22029f7b60208bd14172e5c815ecad`。

## 资源与设计

唯一图像写集为 `reactor_casing_side`、`reactor_casing_top`、`reactor_casing_bottom`、`reactor_hot_port_side`、`reactor_hot_port_top`、`reactor_cold_port_side`、`reactor_cold_port_top` 的SVG源稿、generated PNG和游戏PNG路径；色板仅更新这七项。另有专用 `tools/art-assets/art-reactor-01/export.py`、`README.md` 与本批证据目录。

外壳使用暖灰钢框、螺栓点、铅灰压板和两块浅暖灰混凝土嵌面；顶面是封闭盖板接缝，底面是封闭钢板加固条。端口使用相同钢法兰和黑色密封，扩大红/蓝染料面，并用顶部横条表示热口、两侧竖条表示冷口。所有七图均为手绘16×16整数矩形SVG，通过既有严格 `render_svg` 导出；未调用图像生成，也未改共用导出器、manifest、模型或Java。

当前配方已只读核对：外壳使用钢板、铅板与屏蔽混凝土；冷热口分别使用红/蓝染料、压力接头、密封环与外壳；密封环由钢板压制，压力接头由钢板和焊料部署。未据此改变配方。

风格检查读取只读样例目录 `E:/MyMC/NewMod/Create_NuclearIndustry/tools/art-assets/create-style-samples-2026-10-08/`，以及本机 Create `6.0.10-280` 缓存JAR中的 `andesite_casing.png`、`machine_base_top.png` 和 `andesite_encased_cogwheel_side.png`。只参考钢灰色层次、框边、螺栓和覆盖关系；没有复制第三方贴图到模组资源。

## 预览

- BEFORE/AFTER原尺寸、最近邻放大、明暗底、2×2平铺、缩小/灰度/降亮模拟：`build/reports/art/ART-REACTOR-01/preview.png`
- 实际现行cube底顶模型的三面等距投影及底面材质：`build/reports/art/ART-REACTOR-01/model-preview.png`
- 闭合2×2外壳拼接：`build/reports/art/ART-REACTOR-01/casing-assembly-preview.png`

投影用于检查材质面、比例和接缝，不模拟Minecraft客户端光照、过滤或游戏内暗处可视度。

## 验证与环境

项目版本核对为 Minecraft `1.21.1`、Java `21.0.7`、NeoForge `21.1.219`、Create `6.0.10-280`、Ponder `1.0.82`、Flywheel `1.0.6`。实际读取并应用的技能入口为：

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：核对锁定版本与游戏纹理资源路径。
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：执行16×16 RGBA纹理约束。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：按资源改动范围选择定向验证；本批不运行GameTest。

`build/reports/art/ART-REACTOR-01/baseline.json` 保存开工时189张游戏纹理哈希，`originals/` 保存七张原图。安装命令为 `& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/art-reactor-01/export.py --install`；随后运行相同入口加 `--check`。导出器检查函数打印的通过行是 `只读检查通过：7张游戏PNG与SVG逐字节一致`，检查调用返回成功；PowerShell当时把中文显示成乱码，未另存原始stdout日志。

重复导出验证在Python 3.12.14中用 `subprocess.run([python, '-B', str(export_script)], cwd=root, check=True, capture_output=True, text=True)` 连续调用两次，比较调用前后七张 `generated/block` PNG及四张预览PNG的SHA-256映射；两次退出码均为0且哈希映射相等。对应实际结果摘要保存在 `export-check.json` 的 `repeat_export_byte_identical_final_candidate: true`。子进程stdout被捕获但未写入日志，故没有逐行导出stdout记录。

非法输入负例通过导入本批导出器并调用 `run(install=True, source_overrides=...)` 完成；对 `reactor_casing_side` 的内存源稿在 `</svg>` 前追加 `<circle cx="8" cy="8" r="2" fill="#ffffff"/>`，随后捕获到 `ValueError`，调用未进入写出，调用前后189张游戏PNG的SHA-256映射相同。共享渲染器对此节点的拒绝分支消息为 `只支持直属 rect 及 x/y/width/height/fill 五个属性`；该负例脚本只捕获异常类型，没有打印或保存当次异常文本，因此此处的消息是由已执行分支的代码确定，非原始日志摘录。最终 `--check` 与非法输入结果摘要见 `export-check.json`。

开工189张图中仅本批七项哈希变化，其他182张保持原字节；模型引用语义不变。`git diff --check`通过。一次 `./gradlew.bat jar` 成功（18秒，4项Gradle任务中3项执行、1项缓存），制品为 `build/libs/create_nuclear_industry-0.1.0.jar`；JAR中七张贴图与游戏源文件逐字节一致。日志和逐项SHA记录见 `build/reports/art/ART-REACTOR-01/jar.log` 与 `jar-check.json`。未启动客户端、GameTest或全量测试。
