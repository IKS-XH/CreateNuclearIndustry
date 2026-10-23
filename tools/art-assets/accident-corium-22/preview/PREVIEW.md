# 事故熔融物22 离线真实资源预览

本工具只读本事故树实际生产资源，服务固定`http://127.0.0.1:4210`。在本目录用已有Node运行`node server.mjs`；不安装依赖、不修改共享库、不运行Gradle或Minecraft。

冷热方块实际解析`blockstates/reactor_corium.json`的`hot=false/true`、生产模型及原版父链；交本地`E:/MyMC/NewMod/block-model-renderer` 2.20.0构建真实几何/UV/材质，`createAnimator`接受同一毫秒时钟。库保留MPL-2.0；Three直接读主工程既有vendor并保留MIT。四必要原版模型JSON由本机锁定1.21.1 client.jar提取，字节身份在`native/source-index.json`，游戏原版权保持。原创预览程序按本批合同MIT。

PNG与mcmeta原字节先记SHA再解码。帧宽/高、二维帧表、真实帧序/时间、20tick/s和插值从mcmeta解析，不把32宽flow切作16宽帧，不重绘替代图案。暂停冻结共同时间；下一帧移至所有动画中最近的下一真实帧边界，保持各动画相位；复播从冻结处继续。废料从实际generated模型layer0读取16/32/128同源最近邻小图。

flow完整帧、独立液格侧面取样和中心半幅旋转诊断分别显示。实际`MoltenCoriumFluid.getFlow=Vec3.ZERO`，`getAmount=8`；顶面取still，flow主要用于侧面。已读锁定缓存FlowingFluid#getOwnHeight和LiquidBlockRenderer：无同种流体在上方时高度8/9，侧面归一化U=0…0.5，V=(1-8/9)/2…0.5；不包含邻居高度平均和图集收缩。旋转滑条仅检查中心取样，不声称运行非零流向或改变有限搬移逻辑。这里不构造Minecraft世界流体几何。

固体来源`AccidentContent`沿OBSIDIAN，无HOT光级改写；冷热共享同一环境灯，未添全亮。流体既有lightLevel12，2D流体材质样片在暗环境保持可读。环境灯、显示器和二维样片不是Minecraft光照传播模拟，浏览器不代替Minecraft视觉、事故功能、创造拆除或多人验收。

浏览器入口`window.coriumPreview`：`getState()`读取ready/errors、实际输入SHA、真实模型、帧尺寸/序列/当前帧、Box3/NDC/相机和边界声明；`setPlaying(boolean)`、`setTime(ms)`、`step()`、`setLight('normal'|'dark')`用于有限控制读回。`capture()`从当前画布写固定cold/hot/still/flow/waste PNG和overview.json至`build/reports/art/ART-ACCIDENT-CORIUM-22/preview/`。无客户端任意输出路径，负责人分模式归档后再保存。

仅固定生产15路径、四原版父链、两个Three文件及本地库bundle/wasm、两许可文本可GET；拒绝Java、其它资源、源稿、服务器脚本与目录枚举。所有提取文件与临时内容仅位于本preview目录。
