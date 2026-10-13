# ART-MCMOD-COVER-13 背景交付

当前入口已更新R1，PNG SHA256 `3254b7728b5f3b085cca8600d3d1197d2567ba96a7db73a4a4005d7ed0436bb0`。下方初版交付保持历史，R1实际诊断与有限证据见末节；用户指出棒体发黑/漏绘液体后，没有追认初版这两项视觉通过。

2026-10-14，背景执行者按卡交付真实最小5×5×5反应堆斜视透明背景。已读本地AGENTS、用户最新治理、项目治理1.2/5.1、任务卡和美术README，应用minecraft-resource-pack与verification-before-completion；浏览器操作使用cua_repl。没有取得负责人/Git管理权限。

成品：[2048×2048透明PNG](../../../tools/art-assets/mcmod-cover-13/background/reactor-5x5x5.png)，SHA256 `c4493e3b6953478e0361aa3afb39c92a1be9ff8da598778eff8d9c39bebca249`。可编辑源和重现说明见[背景README](../../../tools/art-assets/mcmod-cover-13/background/README.md)，独立[125格布局](../../../tools/art-assets/mcmod-cover-13/background/layout.json)、[真实来源hash](../../../tools/art-assets/mcmod-cover-13/background/sources.json)与[固定场景](../../../tools/art-assets/mcmod-cover-13/background/scene.mjs)在同目录。

Node入口需要gl/headless-three/skia-canvas，但本机库及bundled包缺少gl，所以直接复用现成浏览器构建/WASM，无安装。新服务绑定127.0.0.1:4189，PID76412；独占自建IAB Tab1，交回负责人，不操作用户Tab。主目录及原核电站工具、第三方目录全只读，不运行Git/Gradle/Minecraft，不读取或写入世界。

复用原设备5³坐标与CT函数，实际核`ReactorStructureDefinition.java`后只在本批布局修正旧网页样例：六燃料列顶盖为换料口、侧面取消非法换料口，仪表移至合法西侧槽，保留正面九连接窗；中央三列空气格与控制棒显示parts保持真实边界。蓝辉使用原`fuel_rod_glow` OBJ和贴图，固定宣传alpha0.65；不声称游戏截图或真实运行/热工，未模拟冷却液。

有限检查：`verify.py`退出0。125个坐标覆盖0..4三轴；77壳体、9窗口、18燃料格、9空气格、6换料口、3驱动、仪表/冷热口各1；边角/底面、列身体和顶盖、唯一仪表与合法槽位核对通过。PNG是RGBA2048²，alpha范围0..255，四角透明，非透明边界[214,211,1834,1837]，轮廓留白超过60px。68条实际只读来源hash一致。实际浏览器渲染936 draw calls/9480 triangles，已实看PNG的完整轮廓、CT连续边框、顶部5格结构、窗内真实棒体与蓝辉；封面执行者实看确认无需构图调整。原生240×150封面检查由封面执行者在组合稿上进行，不拿本背景替代。

原始证据：[渲染参数/实际读取](../../../tools/art-assets/mcmod-cover-13/background/render-evidence.json)、[有限验证](../../../build/reports/art/ART-MCMOD-COVER-13/background/verification.json)、同目录server日志。首次自建server对`.mjs`缺MIME导致模块未运行，已在本批server内修正，并重新加载得到正式PNG；没有向主工具或第三方写入。最终一次组合窄审由负责人安排，不自行追加第二轮审查，用户外观选择仍待最终封面反馈。

收尾运行资源（负责人绑定冻结图后）：再次通过Win32_Process核PID76412的ExecutablePath和CommandLine，精确匹配bundled node.exe与本批background/server.mjs后执行停止，再查PID已不存在。仅停止本批服务器，没有操作原8735或其他进程。尝试关闭本批Tab1时，cua_repl返回所属浏览器已不可用；随后cua.getState实际为`apps:[], browsers:[]`，当前无可访问浏览器/Tab可关闭，因此不能记录成成功执行close。未重新建立浏览器或操作任何用户Tab；图源、PNG和全部证据保持冻结，未再渲染或跑验证。

## R1：实际诊断与满液体整改

用户报告控制棒发黑及内部未充满冷却液后，先将初版本目录全部文件和原报告归档`background/history/pre-r1/`，原PNG SHA仍为c4493e3b…；原检查/运行日志归档`build/reports/art/ART-MCMOD-COVER-13/background/history/pre-r1/`。只续用background写集，无Git、Gradle、Minecraft或主工具/游戏资源修改。

诊断已读真实`obj-loader.js`只写position/uv/normal/index，库`src/core/models.js`的scene材质是MeshStandardMaterial且vertexColors=true。原正式`control_rod.png`为16²，主色RGB132/144/149、99/112/120，以及铜色173/139/66等共8档，非纯黑；逐色证据见`background/control-rod-texture-colors.json`。

只补中性白顶点color、不绘液体的实际对照渲染已保存[诊断PNG](../../../tools/art-assets/mcmod-cover-13/background/diagnostic-neutral-color.png)及[实际材质/几何记录](../../../tools/art-assets/mcmod-cover-13/background/diagnostic-neutral-color.json)。372个OBJ mesh记录为hasColorBefore=false、vertexColors=true、material color=[1,1,1]、真实map存在；代表控制棒shaft/head的map为16²，补色后hasColorAfter=true。钢灰杆体、铜带和燃料钢板恢复，诊断PNG SHA为`03d85389a472fcefd21724ec160e7712df6296489b6ad2355c6ced7e3250f808`，与初版像素差只发生在窗内边界[1030,875,1510,1478]。保留原材质、原图、相机与灯光，未用全亮白材质掩盖错误。

本批scene给缺color的真实OBJ补中性乘数，然后增加满内部冷却液：实际`compound_coolant_still`第0tick原生16²帧，三轴包络[1.01..3.99]，固定alpha0.36，FrontSide/depthTest=true/depthWrite=false/order2；燃料蓝辉order3。液体覆盖燃料组件间隙与中央棒槽，前窗所见为青蓝满液体与可辨钢灰杆/铜带/蓝辉；没有由此宣称游戏库存、流量或热工。原125格布局、相机和灯光与归档逐字一致，外壳与CT不变。

R1操作中，子Agent cua表面不存在iab，重置后仍不可用；负责人自身cua.getState能看到IAB，遂接管唯一新Tab，先打开color-diagnostic再跳正式入口完成两次实际capture。背景执行者读取这两组真实文件并实看PNG，不把准备好的代码或文件存在当成渲染通过。R1自有server是bundled Node PID73564，127.0.0.1:4189；实际Tab编号及后续关闭由负责人记录。原8735、主目录与第三方均未操作。

有限`verify.py`真实退出0：诊断材质状态、原布局/相机/灯光不变、实际满液包络/原生帧/alpha/深度、RGBA2048²/alpha0..255/完整轮廓留白及72条真实来源hash通过。内容边界仍[214,211,1834,1837]，937 draw calls/9492 triangles；R1 PNG SHA `3254b7728b5f3b085cca8600d3d1197d2567ba96a7db73a4a4005d7ed0436bb0`。原壳体列合同复用同一有限入口，未加旧设备测试/模组检查。新[验证JSON](../../../build/reports/art/ART-MCMOD-COVER-13/background/verification.json)、render-evidence与sources记录覆盖当前R1，旧证据仍在历史。封面只接此冻结背景，由负责人实看后绑定和原同次窄审；本报告不关闭用户最终视觉门。

R1运行资源清理：负责人实际读到Tab9的保存状态并通过CUA关闭该本批Tab，随后browser2的listTabs为空。背景执行者按单独派发再次核Win32_Process：PID73564的ExecutablePath为bundled `node/bin/node.exe`，完整CommandLine精确等于该Node路径加本批`mcmod-cover-13/background/server.mjs`；匹配后仅停止此PID。停止后再查PID不存在，Get-NetTCPConnection确认本机4189无Listen项。没有操作原8735或其他进程/服务，没有删除文件或证据，没有改scene/PNG、重渲染或重跑有限检查；本阶段停写。
