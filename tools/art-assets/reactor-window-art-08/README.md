# ART-REACTOR-08 窗口铅灰层试用

本目录只处理反应堆观察窗的R1窗口CT atlas，不改旧R1来源、共享SVG renderer或其他13类图集。64张16×16 SVG保留原CT形状、47个原生OMNIDIRECTIONAL上下文和17个fallback索引；只在原透明pane位置添加铅灰 `#66696A`，`fill-opacity="0.25098039215686275"`明确对应alpha64/255。已有窗框、角和其他不透明像素逐像素保留。

- [浏览器预览](index.html)：展示全连接旧空窗与候选单层/双层离线合成、单边及角窗、候选atlas。
- [源到纹理映射](mapping.json)：记录旧SVG SHA、native上下文、alpha和唯一正式目标。
- [窗口专用导出/检查器](export_window.py)：只导出本窗口tiles和128×128候选图集。
- `preview/core-backdrop.svg` 是手绘工业示意背景，不是游戏截图或生产纹理。

预览只表示浏览器对SVG alpha的合成效果，不模拟Minecraft的透明排序或光照，也不等同客户端视觉验收。唯一观察窗CT atlas已安装试用；实际Minecraft客户端透明排序与视觉仍待用户复看，不构成游戏验收。既有16×16 fallback及其他13张CT atlas保持原样。

在美术工作树根目录运行：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/reactor-window-art-08/export_window.py
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/reactor-window-art-08/export_window.py --check
```
