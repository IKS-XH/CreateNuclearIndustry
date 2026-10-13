# 高清宣传 Logo：厚重工业 R2

2026-10-14。用户要求将现有 Logo 高清化用于宣传，选定连续矢量版，并在首稿实看后明确要求更厚重、有明显重工业风格。当前交付保留八齿钢灰轮体、铜圈、四向压板和蓝白核芯，增加厚钢背层、直线切角、暗铜八螺栓法兰及深嵌核芯；首稿已保留于本批history/r1，不登记为视觉通过。

## 宣传素材

- [可编辑、可任意缩放的SVG](../../tools/art-assets/mod-logo-12-hd/source/mod_logo_hd.svg)
- 透明RGBA PNG：[1024](../../tools/art-assets/mod-logo-12-hd/generated/mod_logo_hd_1024.png)、[2048](../../tools/art-assets/mod-logo-12-hd/generated/mod_logo_hd_2048.png)、[4096](../../tools/art-assets/mod-logo-12-hd/generated/mod_logo_hd_4096.png)
- [完整素材ZIP](../../tools/art-assets/mod-logo-12-hd/create_nuclear_industry_logo_hd.zip)，只含当前R2 SVG、三尺寸PNG和用途/导出说明。
- [明暗背景及原图标对照预览](http://127.0.0.1:8735/tools/art-assets/mod-logo-12-hd/index.html)

SVG以1024 viewBox手工连续矢量绘制，无字体、外部图片、嵌入位图或生图；10个颜色端点沿用原Logo色板。每尺寸直接从该SVG渲染，透明画布保留至少6.25%边长的外缘留白，适合在海报、封面和视频中叠放。

## 实际证据与边界

负责人实看了R2实际1024导出及浏览器明暗背景；截图在本对话可视化根logo12-preview.png。当前SVG与R2阶段源逐字一致，SHA256 cd763c08bf7f29ce21aef613230fd9dc37c06b826a5dbd2f7ec776e862b16c2c；4K PNG为276f2262726a67f61c809a4da7e618674dd2a4a79cc202e8c09e643836f6dd8e。最终ZIP1298555字节，SHA256 bd7eb7aa52d8ae7f9cf3ad2e7b685abfce8d85161eebee998efc8315e8c37ab5，五条目与来源一致。

原始1024导出、其余两尺寸导出及有限检查均exit0，42/42检查通过；只覆盖新源结构/引用、尺寸/alpha/渐变/留白、阶段与稳定入口、ZIP身份和旧六件保护。执行报告见[ASSETS](reports/ART-LOGO-12-HD-ASSETS.md)，原始记录与14项最终来源位于build/reports/art/ART-LOGO-12-HD/r2/。本次[独立组合窄审](reports/ART-LOGO-12-HD-REVIEW.md)未发现必改项，负责人已读回报告及原始退出/身份记录；没有重复导出、检查或设备测试。

这是宣传素材交付，没有替换游戏内32px图标或改动模组源码/JAR，没有Minecraft测试、构建、Git写入或世界操作。原11源稿、两正式资源、两共享Java与当前美术JAR六件保持。内部检查与负责人实看不冒称用户已最终接受此宣传外观。
