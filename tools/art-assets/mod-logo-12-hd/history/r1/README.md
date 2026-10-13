# Create: Nuclear Industry 高清宣传徽标

此套素材为ART-LOGO-12-HD连续矢量宣传版，承接ART11的八齿钢灰轮体、铜圈、四向钢卡爪和蓝白核芯。透明底、正面构图，无图中文字/原子轨道/新增符号。用于封面、海报、视频画面或介绍页排版；不替换游戏内32px像素标签。

`source/mod_logo_hd.svg`是唯一绘图来源：1024×1024 viewBox、连续圆角路径、克制钢/铜渐变和矢量玻璃反光，无字体、嵌入位图或外部依赖。全部颜色端点沿用原ART11色板；不要将此稿当作原像素PNG的插值放大。

交付透明RGBA PNG：`generated/mod_logo_hd_1024.png`、`mod_logo_hd_2048.png`、`mod_logo_hd_4096.png`。SVG适合无损缩放，PNG适合视频/文档软件直接放置。图形四边保留透明留白，排版时建议保留原画布比例；可选用4096PNG做大幅输出。

导出使用已有bundled Node24.19.0 / sharp0.35.5 / libvips8.18.7 / librsvg2.63.2。`export_hd.mjs`读取同一SVG，在72/144/288 DPI分别直接栅格化1024/2048/4096图，未先渲染低分辨率再放大，不安装新依赖。

以下为仓库工具目录的重现命令；宣传ZIP只交付素材与说明，日常使用PNG/SVG无需运行脚本或安装依赖。

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe' 'tools/art-assets/mod-logo-12-hd/export_hd.mjs'
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 'tools/art-assets/mod-logo-12-hd/check_package.py'
```

导出器默认拒绝覆盖本批已生成PNG和保护基线，避免将后续重跑混成首次证据。已有交付不需要重跑；变更SVG后须在负责人派发的整改范围保留原证据，再明确本次输出。

宣传ZIP仅含可编辑SVG、三种透明PNG及本说明；身份见本批报告。离线HTML直接引用真实SVG/PNG及原像素版作对照，不代表游戏已加载或用户视觉验收。原ART11源稿、正式资源、Java和美术JAR均保持。
