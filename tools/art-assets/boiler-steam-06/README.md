# ART-BOILER-06 蒸汽候选素材

本目录提供两种锅炉蒸汽的同源SVG帧候选：普通蒸汽为乳白灰，超临界蒸汽为青白。still与flow轮廓各自不同；同一still或flow的两种材质共用逐像素相同的遮罩和亮度细节，仅替换色板。动画是材质循环，表示蒸汽翻卷/上升的外观，不代表流向或流量。消费端按固定材质权重将still/flow做成单个16×16动态纹理，再按真实库存连续混色；权重只影响材质表现。汽区库存浓度和超临界占比由后续消费端按实际库存连续处理；内部透明度由渲染顶点控制，PNG帧alpha保持0/255。

mapping.json列出四个注册sprite、四组8帧SVG、四张16×128候选atlas及mcmeta、32张独立内部16×16帧和目标映射。阶段进度：四张流体atlas PNG及对应mcmeta、32张内部帧已安装；客户端消费者接续中，真实游戏视觉仍待用户复看。

## 预览

打开preview/index.html可看四种still/flow循环；preview/contact-sheet.png提供八帧并列总览。浓度控件对比透窗低/高汽量，超临界占比滑条连续混色。预览是资源与材质示意，不代表游戏内采光、顶点混合或最终渲染验收。

## 再现命令

在美术树根目录使用bundled Python：

& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/boiler-steam-06/pipeline.py author
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/boiler-steam-06/pipeline.py build
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/boiler-steam-06/pipeline.py check

资源映射与逐帧检查不修改正式资源。素材复看及安装已完成；客户端消费者与真实游戏视觉复看仍待完成。


