# ART-REACTOR-01 定向资源导出

本工具只处理反应堆外壳和冷热端口七张16×16贴图。手工编辑 `tools/art-assets/sources/block/` 下的同名SVG，并同步 `tools/art-assets/palette.json` 中的同名色板条目。渲染复用共用 `tools/art-assets/export.py` 严格整数矩形解析器；不修改共用manifest、pipeline或旧基线。

在仓库根目录使用项目已配置的Python 3.12与Pillow：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/art-reactor-01/export.py
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/art-reactor-01/export.py --check
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/art-reactor-01/export.py --install
```

默认命令导出七张 `generated/block/*.png` 并生成 `build/reports/art/ART-REACTOR-01/` 离线预览，不接入游戏PNG。`--check`只读比较七张游戏PNG与当前SVG导出字节。`--install`先校验、光栅化并准备全套输出，再写入本批七张工具PNG、预览和游戏PNG；不接受扩展目标参数，源稿或色板有误时不会开始安装。导出只写字节变化文件。

预览包含原尺寸、最近邻放大、明暗底、2×2贴图平铺、缩小/灰度/降亮模拟，以及按现行 `cube_bottom_top` 面材质绘制的静态等距方块、底面材质与2×2侧面拼接。等距画面不模拟Minecraft客户端光照，也不代表游戏内可视度验收。

