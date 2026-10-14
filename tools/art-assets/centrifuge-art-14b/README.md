# ART-CENTRIFUGE-14B 正式资源预览

从本美术树实际已安装的五JSON和六PNG读取，不从SVG或14A设计稿构造贴图。`assets.json` 的安装映射及SHA是只读输入；每个浏览器实际载荷核对SHA，缺失或不同源直接报错。

启动 `node server.mjs 4192`；启动前核4192空闲，Windows后台 `Start-Process -WindowStyle Hidden`，仅127.0.0.1。固定路由只开放本页、资产契约、五正式模型/六PNG与原本地renderer2.20.0和Three0.180.0，不安装或升级。原4191设计稿服务保持。

运行机视图含真实lower/upper/rotor/shaft，四部件沿原下格坐标换算；独立4×4×8输入轴和加粗转子共享竖轴枢组。物品视图直接读346元素整机组合JSON，保持静态完整几何；网页几何近看不冒充游戏库存屏幕，游戏物品display仍按资产合同原值保留。

正/背/侧/俯/底与拖动/滚轮同步两视图。剖视只裁运行机北半静壳，保留完整轴/转子；物品保持完整。低速为24°/秒网页演示，非真实游戏RPM；停止和重置沿既有约定。六PNG像素近看显示实际安装内容和透明棋盘。

`window.centrifugeArt14B.getState()`记录实际11安装载荷/契约SHA、模型元素、各运动归组/相位、相机与draw-call。保存按钮只通过固定capture将两画布与JSON写本批 `build/reports/art/ART-CENTRIFUGE-14B/preview/`，不写任意路径。

本页不是Minecraft客户端手测。正常双格、物品显示、四朝向、正反转/停止/超载/轴承磨损和重载由用户在本批候选里集中观察；仅网页加载与截图不能替代游戏验收。未改Java游戏状态、世界/配置、Git或14A源稿。
