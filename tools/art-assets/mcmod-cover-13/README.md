# MC百科简介封面 ART-MCMOD-COVER-13

画布比例8:5。中文名称为“机械动力：核工业”，英文名称为“Create: Nuclear Industry”。左侧沿用ART-LOGO-12-HD厚铸钢R2 Logo原始几何和颜色，右侧使用本批实际模型/贴图渲染的5×5×5最小成型反应堆。背景是模型渲染，不是游戏截图；成品SVG内含栅格背景，不能称整张纯矢量。

`output/mcmod-cover.svg` 是自包含交付源：标题为轮廓路径，Logo为原矢量，背景PNG使用data URI内嵌。`output/mcmod-cover-editable.svg` 保留可编辑名称文字；文字源依赖本机微软雅黑粗体，若在其他系统编辑，需要自行具备适当字体。系统字体不随包分发。

包内`output/`含1920×1200、960×600、240×150 PNG与两份SVG、身份清单；`sources/`保留逐字原Logo和透明反应堆渲染输入，供进一步编辑或确认来源。反应堆布局、渲染场景和入口另见同批background交付目录。草案位于工作目录draft/，不在最终ZIP内。

本机确定性导出入口使用现有bundled Node、sharp和@napi-rs/canvas，未安装依赖。工作树运行：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe' './export-cover.cjs' --final --background='./background/reactor-5x5x5.png'
```

解压ZIP后，从包根运行同一工具，背景参数改为`--background=./sources/reactor-render.png`。工具优先读取包内sources/mod_logo_hd.svg；仍需本机已有微软雅黑粗体和指定bundled运行时，包内没有安装器或字体文件。已交付路径版SVG和PNG显示不依赖该字体。

原Logo SHA-256：`cd763c08bf7f29ce21aef613230fd9dc37c06b826a5dbd2f7ec776e862b16c2c`。

R1背景 SHA-256：`3254b7728b5f3b085cca8600d3d1197d2567ba96a7db73a4a4005d7ed0436bb0`。本次模型宣传姿态包含完整3×3×3青蓝冷却液包络，原控制棒灰钢/铜带材质恢复；液体不是服务器实际库存声明。原三PNG、两SVG、ZIP、身份和工具完整保留在工作目录`history/pre-r1/`，原用户反馈未被内部检查追认通过。R1仅替换背景输入，Logo、名称、字体、版式和背景视口保持；三尺寸品牌区与视口外像素对比见本批cover/r1证据。

MC百科封面240×150比例依据：https://mcmod.readthedocs.io/zh/latest/mod/ 。仅作宣传素材，没有写入游戏资源、玩法、配置或世界，也没有运行Minecraft、Gradle或模组测试。内部预览与有限检查不代替用户外观选择。
