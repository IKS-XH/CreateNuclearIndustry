# ART-MCMOD-COVER-13 背景交付

2026-10-14，背景执行者按卡交付真实最小5×5×5反应堆斜视透明背景。已读本地AGENTS、用户最新治理、项目治理1.2/5.1、任务卡和美术README，应用minecraft-resource-pack与verification-before-completion；浏览器操作使用cua_repl。没有取得负责人/Git管理权限。

成品：[2048×2048透明PNG](../../../tools/art-assets/mcmod-cover-13/background/reactor-5x5x5.png)，SHA256 `c4493e3b6953478e0361aa3afb39c92a1be9ff8da598778eff8d9c39bebca249`。可编辑源和重现说明见[背景README](../../../tools/art-assets/mcmod-cover-13/background/README.md)，独立[125格布局](../../../tools/art-assets/mcmod-cover-13/background/layout.json)、[真实来源hash](../../../tools/art-assets/mcmod-cover-13/background/sources.json)与[固定场景](../../../tools/art-assets/mcmod-cover-13/background/scene.mjs)在同目录。

Node入口需要gl/headless-three/skia-canvas，但本机库及bundled包缺少gl，所以直接复用现成浏览器构建/WASM，无安装。新服务绑定127.0.0.1:4189，PID76412；独占自建IAB Tab1，交回负责人，不操作用户Tab。主目录及原核电站工具、第三方目录全只读，不运行Git/Gradle/Minecraft，不读取或写入世界。

复用原设备5³坐标与CT函数，实际核`ReactorStructureDefinition.java`后只在本批布局修正旧网页样例：六燃料列顶盖为换料口、侧面取消非法换料口，仪表移至合法西侧槽，保留正面九连接窗；中央三列空气格与控制棒显示parts保持真实边界。蓝辉使用原`fuel_rod_glow` OBJ和贴图，固定宣传alpha0.65；不声称游戏截图或真实运行/热工，未模拟冷却液。

有限检查：`verify.py`退出0。125个坐标覆盖0..4三轴；77壳体、9窗口、18燃料格、9空气格、6换料口、3驱动、仪表/冷热口各1；边角/底面、列身体和顶盖、唯一仪表与合法槽位核对通过。PNG是RGBA2048²，alpha范围0..255，四角透明，非透明边界[214,211,1834,1837]，轮廓留白超过60px。68条实际只读来源hash一致。实际浏览器渲染936 draw calls/9480 triangles，已实看PNG的完整轮廓、CT连续边框、顶部5格结构、窗内真实棒体与蓝辉；封面执行者实看确认无需构图调整。原生240×150封面检查由封面执行者在组合稿上进行，不拿本背景替代。

原始证据：[渲染参数/实际读取](../../../tools/art-assets/mcmod-cover-13/background/render-evidence.json)、[有限验证](../../../build/reports/art/ART-MCMOD-COVER-13/background/verification.json)、同目录server日志。首次自建server对`.mjs`缺MIME导致模块未运行，已在本批server内修正，并重新加载得到正式PNG；没有向主工具或第三方写入。最终一次组合窄审由负责人安排，不自行追加第二轮审查，用户外观选择仍待最终封面反馈。

收尾运行资源（负责人绑定冻结图后）：再次通过Win32_Process核PID76412的ExecutablePath和CommandLine，精确匹配bundled node.exe与本批background/server.mjs后执行停止，再查PID已不存在。仅停止本批服务器，没有操作原8735或其他进程。尝试关闭本批Tab1时，cua_repl返回所属浏览器已不可用；随后cua.getState实际为`apps:[], browsers:[]`，当前无可访问浏览器/Tab可关闭，因此不能记录成成功执行close。未重新建立浏览器或操作任何用户Tab；图源、PNG和全部证据保持冻结，未再渲染或跑验证。
