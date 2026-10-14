# ART-CENTRIFUGE-14A 网页预览

本页直接读取 `assets/concepts.json`、候选上下格/转子JSON及四槽SVG，使用同级 `block-model-renderer` 2.20.0和主目录现有Three 0.180.0。SVG仅在浏览器内存解码为纹理，不输出或安装正式PNG；现有正式JSON/PNG只读对照。

启动：`node server.mjs 4191`。仅监听 `127.0.0.1`；启动前检查端口，若占用使用另一个空闲端口。本次Node Fetch检查拒绝4190并报`bad port`，负责人IAB却已能实际浏览原4190；这是已观察到的检查环境差异。最终使用4191通过本页HTTP检查并保留给负责人实看。Windows后台用 `Start-Process -WindowStyle Hidden`。本页不需要安装依赖或全量缓存。

三稿共用正面、背面、侧面、俯视、底部与统一斜视角；拖动和滚轮同步三稿。剖视沿中心Z=8裁去北侧半壳并保留完整转子，方便检查实际内部；它不是游戏结构。观察转台改变相机；低速演示仅以24°/秒旋转转子，明确不是实际Minecraft RPM。停止保留当前角度，重置回统一观察角和零相位。

上壳体偏移16模型单位；独立转子仍为下格本地Y=5..27，绕[8,16,8]竖轴。顶部UP矿浆进/回取、上下四水平面清水/两粉末输出、下格DOWN动力与北向检修盖说明按现行接口，不添加假管口。

点击纹理可按最近邻放大实际16²来源。`window.centrifugePreview.getState()`提供只读实际输入SHA、模型元素数、相机/材质、draw-call、错误状态。网页“保存本页观察证据”只保存本批当前画布和JSON至 `build/reports/art/ART-CENTRIFUGE-14A/preview/`；证明网页实际渲染，不能代替Minecraft人工验收。

本批不运行Minecraft、Gradle、JUnit，不生成模组JAR，不修改正式资源、主工具、世界、配置或Git。浏览器不可用时由负责人在同一URL接续实看。

## R1 三稿精修

用户要求更精致，原作者在三稿原方向上细化层叠顶法兰、端座台阶、轴承检修盖及窗框。候选仍使用schemaVersion1，纹理按 `textures` 的真实槽名动态读取；新增 `machined`、`fastener` 槽各自对应实际16²SVG，缩略与像素近看同步，不写磁盘PNG。

本页相机、灯光、尺寸、转子轴心、演示与操作规则保持初版；原机对照仍只读四槽PNG。初版六网页文件原样归档在 `preview/history/pre-r1/`，当前初版实际截图/browser-evidence及检查证据保留在本批报告目录 `preview/history/pre-r1/`，负责人可以同角度比较原截图。没有新增复杂对照页面或修改服务。

## R2 转子增粗

用户要求转子加大，本轮大小只来自三份候选rotor的实际JSON。R1六壳及18SVG保持；网页模型换算仍为1/16，转子枢轴、机身偏移、相机/光照/尺寸、低速演示及capture不改，不使用网页group缩放放大转子。`getState()`证据增加实际契约revision，便于识别当前载荷。

R1当前七网页、说明和实际观察/来源证据已原样保留至各自 `preview/history/pre-r2/`。R2仅核三rotor、契约及必要文案的当前HTTP字节身份与语法，复用R1六槽及原操作检查；作者的尺寸和全角度间隙以R2资产报告为准。根负责人在同一4191页面实际看窗内体量、剖视和低速旋转，服务不重启。
