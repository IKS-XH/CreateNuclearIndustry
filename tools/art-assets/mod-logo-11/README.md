# ART-LOGO-11 原创徽标候选

用户已选“齿轮＋蓝色核芯”。本目录制作一个透明底32px像素徽标，钢灰八齿轮体、铜圈、四向钢卡爪与蓝白核芯。无文字、细轨道或第三方图片；绘图入口唯一为`source/mod_logo.svg`。

`export_logo.py`只读取直属整数`rect`，拒绝其他绘图语法；本稿全部几何按2px网格，色板最多12色。PNG从SVG像素直接导出，128/256使用最近邻，候选32px与独立32px导出逐字相同。预览图与HTML仅作离线构图复看。

在美术树根目录使用固定bundled Python：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 'tools/art-assets/mod-logo-11/export_logo.py' export
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 'tools/art-assets/mod-logo-11/export_logo.py' check
```

资源映射见`resource-map.json`，原始候选模型/PNG位于`candidate/assets/create_nuclear_industry/`。负责人实际复看后，两项正式模型/贴图已局部安装；主PM临时授权的延迟`MOD_LOGO`注册与MAIN.icon supplier两处消费已接入美术树，原展示内容和顺序逐字保持。素材/源码/安装的同一次组合审查无必改，唯一必要增量assemble实际exit0；候选待同轮制品审查及用户客户端观察，未启动Minecraft。

上面的`check`命令与`phase1-manifest.json`属于安装前时点，包含正式目标尚不存在的要求，不应在已安装状态重复运行。安装独立读回证据见`installation-identity.json`和`installed-source-manifest.json`；后者17项清单是安装后、阶段文案更新前的历史时点。最终17项来源见`build/reports/art/ART-LOGO-11/final-source-manifest.json`，只有本README及HTML的阶段文字更新，图形像素、模型、映射和Java消费仍保持已审身份。

版本从实际美术树`gradle.properties`与`build.gradle`核实：MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6。生成依赖为已有Pillow12.3.0，不安装新依赖。
