# ART-SHIELDED-ASSEMBLY-15A 静态预览交付

2026-10-14，唯一预览执行者。最终素材ready后一次有限核对68/68、退出0，52个HTTP输入与当前磁盘SHA一致。网页源码稳定；最终GPU正常/剖视/底部/状态灯实看与三稿capture由负责人完成，此报告不代替用户选稿或Minecraft验收。

## 工作区与边界

唯一写树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，基线HEAD `88b8fd289d49df90d7e3c655b6e81381ad01130f`。读取15A卡、美术入口、主目录最新AGENTS及适用治理1.2/5.1/5.2。应用 `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md` 核模型面/UV、SVG槽和真实PNG引用，应用 `verification-before-completion/SKILL.md` 的先核证据后声明原则，验证服从本批精简边界。MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6由实际gradle.properties及依赖确认，未改版本。

仅新增本工具index.html、server.mjs、README.md、preview/，本批preview报告目录及本报告。没有写src、旧工具、14A/14B、第三方库、Git、世界、配置或JAR；没有安装、升级、Minecraft启动、Gradle或新测试套。原工具/资产保护复用资产作者本批395输入零变化证据，未在此机械重跑。

## 实际模型与坐标

与资产作者约定schemaVersion1，concepts为a/b/c对象，各稿四模型 `shell/left_arm/right_arm/fixture` 与九槽 `lead/steel/machined/fastener/brass/window/lamp_off/lamp_on/deck`。最终素材12 JSON/27 SVG，shell为93/101/107元素。本页逐件读取JSON、逐槽读取16² SVG，通过本地block-model-renderer2.20.0与既有Three0.180.0构面；SVG仅在浏览器内存解码，不手画替代3D或写正式PNG。

四件均为0..32整机模型坐标。库几何以[8,8,8]为中心，本页统一 `scale=1/16`、`position=[-.5,.5,-.5]`，得到 `[mx/16-1,my/16,mz/16-1]`。partial不再加格偏移；相机观察目标[0,1,0]仅为整机中心，真实朝向pivot保持主控 `x=z=8`。待机臂/fixture不运动，不制造加工循环或假装料。

底部轴真实读取锁定Create slim JAR的 `AllPartialModels.SHAFT_HALF -> create:block/shaft_half`、axis/axis_top PNG和Minecraft block/block父模型。读实际AllPartialModels/CachedBuffers/AngleHelper字节码，DOWN为水平0、X+90°，绕主控中心[8,8,8]旋转。实际半轴八角点映射得到包络 `[6,0,6]..[10,8,10]`，底接触点 `[8,0,8]`，不挪至整机中心16或改变长度。

JAR、原条目SHA、许可声明与第三方身份位于 [reference.json](../../../tools/art-assets/shielded-assembly-concepts-15/preview/refs/reference.json)。Create JAR SHA为 `f0652bee27460f2d26a748f537cb5d687981441378d3a526d17ffaab5a1072bb`，shaft_half条目SHA为 `bbd1d6226933a4b29b365a449e87ecff1f851965f82f802c0800747b530a0576`。其元数据许可证声明指向LICENSE.md，该slim包未含附件文本；本批保持Create来源标记，只用于本地参考，不作为CNI原创或正式安装资源。字节码原记录保存为本批 `create-partials-bytecode.txt` / `partial-facing-bytecode.txt`。

## 页面与服务

[预览入口](http://127.0.0.1:4193/)：三稿统一斜视、正/背/侧面、俯/底，同步拖动/缩放、机壳剖视或隐藏、静态灯off/on和重置。窗材质保实际alpha并采用cutout阈值，不把普通壳体变透明或发光。灯对照仅将shell的lamp_off槽绑定到真实lamp_on SVG，不改变几何或运行状态。

可选原机按钮读真实八格off/on JSON、三partial及十PNG；八格按 `part=x+2*z+4*y` 各加16模型单位恢复整机，原partial仍整体坐标。原机对照使用同照明/相机，不使用六面方块替代。

服务仅监听127.0.0.1:4193，Windows Hidden后台启动；自有PID **18084**，executable `D:/Program Files/nodejs/node.exe`，cwd `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/tools/art-assets/shielded-assembly-concepts-15`。见本批 `launch.json` 与 `http-input-identity.json`。没有终止、重启或修改4191/4192及其他既有服务。

路由固定白名单为本页、当前a/b/c候选、指定Create参考/父模型、指定库与原机资源。capture要求本页Origin、完整A/B/C三图、有效PNG与已加载本批证据；只写固定 `a-latest.png` / `b-latest.png` / `c-latest.png` / `browser-evidence.json`，不提供任意读写路径。

## 本批检查与负责人接续

最终ready后唯一命令 `node tools/art-assets/shielded-assembly-concepts-15/preview/verify.mjs 4193`：68/68、52输入同源、exit0，覆盖三脚本语法、锁定本地库/Three、schema1、候选/页面/必要参考HTTP身份、三条非白名单拒绝、自有服务身份、五参考SHA与真实DOWN轴包络。仅涉及本批输入；没有扩成HTTP矩阵、GPU替身或其他设备测试。

原始 [verify-output.log](../../../build/reports/art/ART-SHIELDED-ASSEMBLY-15A/preview/verify-output.log)、[http-input-identity.json](../../../build/reports/art/ART-SHIELDED-ASSEMBLY-15A/preview/http-input-identity.json)、`verify.exit`。本执行者写集身份索引为本批 `preview-source-identity.json`。

负责人已查看初版统一斜视/正面/隐藏机构，报告三稿形体与双臂/fixture可辨；初版未冻结截图，不冒称最终材质实看通过。最终素材已修局部共面及灯遮挡后重新ready，负责人应reload最终页，再核正常三稿、窗内机构、剖视/隐藏、底部原生半轴与灯off/on，使用保存按钮生成当前三图/JSON。只读状态入口为 `window.shieldedAssemblyPreview.getState()`，记录实际输入SHA、相机、材质、坐标与draw-call。

本批停在静态选稿；不推定A被选，不安装八格正式资源，不进入工序动画。网页与来源自本报告交回起停写，服务保留供负责人和用户复看。
