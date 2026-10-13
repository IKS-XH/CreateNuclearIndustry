# Create: Nuclear Industry 高清宣传徽标

此套素材为ART-LOGO-12-HD R2厚重工业宣传版，承接ART11的八齿钢灰轮体、铜圈、四向钢卡爪和蓝白核芯。按用户“明显重工业风格”反馈，增加八枚六角法兰螺栓、宽钢压板与深扣合缝；透明底、正面构图，无图中文字/原子轨道/警示带。用于封面、海报、视频画面或介绍页排版，不替换游戏内32px像素标签。

`source/mod_logo_hd.svg`是当前R2唯一公开绘图来源：1024×1024 viewBox、八齿直线切角、右36/下40坐标的钢背层、宽实心轮缘、暗铜法兰、四块厚压板与缩小的深嵌蓝芯。去除R1泡状椭圆反光，不用字体、嵌入位图或外部依赖；颜色端点沿用ART11色板。R2专名SVG/PNG只保留本批阶段来源，与当前稳定入口逐字相同；不要把高清稿当作像素PNG插值放大。

交付透明RGBA PNG：`generated/mod_logo_hd_1024.png`、`mod_logo_hd_2048.png`、`mod_logo_hd_4096.png`。SVG适合无损缩放，PNG适合视频/文档软件直接放置。图形四边保留透明留白，排版时建议保留原画布比例；可选用4096PNG做大幅输出。

导出使用已有bundled Node24.19.0 / sharp0.35.5 / libvips8.18.7 / librsvg2.63.2。`export_hd.mjs`读取同一SVG，在72/144/288 DPI分别直接栅格化1024/2048/4096图，未先渲染低分辨率再放大，不安装新依赖。

以下为仓库工具目录的重现命令；宣传ZIP只交付素材与说明，日常使用PNG/SVG无需运行脚本或安装依赖。

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe' 'tools/art-assets/mod-logo-12-hd/export_hd.mjs'
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 'tools/art-assets/mod-logo-12-hd/check_package.py'
```

本轮先导出1024供实看，再以同一SVG导出2048/4096，每尺寸仅一次。导出器拒绝覆盖R2阶段PNG；公开旧入口只有在与完整R1历史归档逐字相同的条件下才同步为R2，避免无关覆盖。已有交付不需要重跑；变更SVG后须在负责人派发的整改范围保留原证据。

当前`create_nuclear_industry_logo_hd.zip`仅绑定R2可编辑SVG、三种透明PNG及本说明，与R2专名ZIP逐字相同，身份见本批报告。轻量玻璃R1已因用户反馈转为历史，源/图/旧ZIP和证据保留于`history/r1/`；当前下载不会指向R1。离线HTML引用真实SVG/PNG及原像素版作对照，不代表游戏已加载或用户视觉验收。原ART11源稿、正式资源、Java和美术JAR均保持。
