# ART-TURBINE-10B 素材第一阶段报告

2026-10-12，执行者只在`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`实施10B素材卡第一阶段。已交冻结候选和真实加载预览；尚未安装正式资源，未进行Git写操作、Gradle、游戏启动或世界/配置操作。内部素材检查与离线观察不替代用户客户端视觉/动画验收，07B-R2机壳门独立。

## 已交候选与接口映射

独立工具入口：`tools/art-assets/turbine-rotor-bearing-art-10b/`。`resource-map.json`记录26项候选/目标路径及每项SHA256，映射文件冻结SHA256为`a39ff51073c711fd3e5480b1fc972033f8211780182c8c6f7a5710b877855d0c`。保持已有注册ID和blockstate；两份blockstate完全只读。

| 模型ID | 候选内容 | 文件数 |
| --- | --- | --- |
| `rotor_blades_d3/d5/d7` | 完整动态B薄叶片、阶梯轮毂、贯穿轴 | 3 OBJ + 3 JSON |
| `rotor_axle` | located空elements静态基座，避免重复轴/毂 | 1 JSON |
| `turbine_rotor_unformed` | 完整D3网格XY按0.42/1.1875缩小，不额外叠rotor_middle | 1 OBJ + 1 JSON |
| `output_shaft_front/rear` | 固定端板、箱座、外圈和窄铜锁圈 | 2 OBJ + 2 JSON |
| `output_shaft_front_rotating/rear_rotating` | R0.14轴与R0.14..0.155旋转内圈 | 2 OBJ + 2 JSON |
| `turbine_output_shaft_unformed`、`turbine_output_shaft_unformed_rear` | 对应前后固定与旋转件合成完整未成型单件 | 2 OBJ + 2 JSON |
| `turbine_rotor_bearing_10b.mtl` | 仅本候选专属材质引用 | 1 MTL |
| `turbine_10b_steel/blade/bearing/copper` | 新16px SVG源确定性导出RGBA PNG | 4 PNG + 工具内4 SVG源 |

共10 OBJ、11 JSON、1 MTL、4 PNG进入候选映射。原shared`turbine.mtl`、`rotor_middle`以及全部casing/window/07B-R2正式资源只读，未重生。新动态输出ID没有别名需求。现有输出轴旧模型实际Z为0..1，动态轴仍完整保留这一范围。

## 实际几何

每列12片，第二列相对第一列错相15°，后掠0.09rad。每列真实厚度0.125格，两列Z为0.2375..0.3625与0.6375..0.7625；轮毂总Z仍0.13..0.87。D3外轮毂R0.52，D5/D7按外形比例放大，三档孔R0.155及轴R0.14固定，孔没有随直径放大。

| 档位 | 实际全部动态网格扫掠R | 合同R上限 | 实际单格Z |
| --- | --- | --- | --- |
| D3 | 1.1698278181571171 | 1.1875 | 0..1 |
| D5 | 2.154945979151457 | 2.1875 | 0..1 |
| D7 | 3.1400641394190894 | 3.1875 | 0..1 |

实际扫掠不超合同上限，壳内净空至少0.125格。前板0..0.1875，后板0.8125..1，方孔半宽0.19；固定外圈R0.27/孔R0.155，旋转内圈R0.14..0.155。固定螺钉外凸0.0075格以免与端板共面：固定前模型Z为-0.0075..0.2715，后模型0.7285..1.0075；轴身仍0..1。旋转输出partial真实bounds为XY0.345..0.655、Z0..1。铜色仅窄视觉锁圈/小锁钉，不改材料、配方、碰撞或玩法。

## 验证与真实预览

原始证据位于`build/reports/art/ART-TURBINE-10B/assets/`。最终`produce.log/produce.exit`记录26资源生成退出0；`check-assets.log/check-assets.exit`记录7项检查、10模型、156闭合实体组通过并退出0，明细为`checks.json`。检查实际OBJ的有向/无向边闭合、法向与绕序一致、每组正体积、孔壁朝孔内、三档完整扫掠、真实24片/15°错相/薄度/中心、固定孔径、固定旋转拆分、实际JSON→OBJ→MTL→纹理引用，以及4 SVG整数rect/限色与PNG逐像素一致。定向核对全部26候选SHA256；不扫描旧资产全库。

独立producer没有导入或调用旧全库工具；PNG不使用imagegen。首版几何检查退出0，但浏览器真实加载后发现端板分段重复整张轴承UV形成小面板；已改成整块端板UV、连续圆筒UV与整体帽面投影，并把孔壁/板侧用钢材质及内侧窄锁圈可见位置落实。共三次必要候选生成，原始首版`produce-first.log/check-assets-first.log/thin-vs-before-first.jpg`及UV轮`produce-uv.log/check-assets-uv.log/front-inside-uv.jpg`保留；没有覆盖它们追认首版视觉。最终增加真实错相/轮毂外R/旋转内圈核对时仅再跑受影响检查，没有重生候选；前一最终检查日志保留为`check-assets-final-before-phase-audit.log`。

真实加载预览入口为`http://127.0.0.1:8766/`，源为工具的`index.html/preview.js`。浏览器实际读取10候选OBJ+1旧真实0.34厚度比较OBJ、专属MTL和4 PNG；最后状态由`browser-snapshot.txt`保留为11 OBJ/1 MTL/4 PNG、角度54.7°、静止、启用背面剔除，页面错误栏为空。实际播放观察到角度由45°增加至49.0°，停止后为54.7°；固定座不应用旋转矩阵，仅动态轴/内圈/完整转子旋转。演示速度不冒充真实Create速度。

已保存最终浏览器截图：`thin-vs-before.jpg`（0.125/0.34同源比较）、`front-inside.jpg/rear-inside.jpg`、`front-inside-exploded.jpg`（真实孔与固定/旋转拆开）、`front-outside.jpg/rear-outside.jpg`、`d3-assembled.jpg/d5-assembled.jpg/d7-assembled.jpg`、`d7-phase45.jpg/d7-stopped.jpg`、`unformed-complete.jpg`。side/end/inside使用10A/R1相机，后端为对应反向视角；正常剔除背面，没有整体翻转内外面。预览不装入旧机壳模型，07B-R2机壳保持独立。

## 实际使用的规则与技能

已实际读取主目录最新AGENTS、10B卡、项目治理1.2/5.1/5.2，并核对本树gradle.properties/build.gradle：Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。

实际技能入口：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`用于版本及客户端/资源边界；`minecraft-testing/SKILL.md`用于把本批素材定向检查与真实游戏验收分开；`minecraft-resource-pack/SKILL.md`用于模型ID/引用及16px RGBA纹理合同；`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/verification-before-completion/SKILL.md`用于先运行并保存原始结果再作检查结论。另读computer-use技能后用cua_repl查看/保存浏览器真实加载截图。所有新增手写代码的注释和职责说明使用中文。

素材第一阶段交付等待负责人实际看冻结薄叶片及两端支承，再按卡补充精确安装名单；此前不写正式资源。客户端共享运行接口、Java消费、唯一组合测试/assemble由对应执行者和主PM协调，本作者未代改或代测。

## 第二阶段：负责人看冻结版后批准的精确安装

负责人已实际看薄厚对照、D5/D7整列、前内拆件、后内旋转/暂停并读D3截图、原始检查及报告；10B卡追加26个精确生产路径后，本作者仅安装同一冻结候选。17份被替换旧源全部先备份到`tools/art-assets/turbine-rotor-bearing-art-10b/backups/2026-10-12-install/`，保留原相对路径；9份新增在写前确认不存在，无其它批次文件冲突。

本批使用新工具`install_frozen.py`读取任务卡精确名单，核定映射冻结SHA与17/9计数后先完成全部备份、逐项核旧SHA一致，再开始复制。首次预检因作者的严格列表解析没有允许任务卡行首空格，退出1发生在任何备份和正式资源写入之前；原始`install-preflight-failed.log/install-preflight-failed.exit`保留。只修正本批安装器的行首空格解析后成功执行，未改变冻结映射或候选。

`install.log/install.exit`记录安装退出0，26份生产文件与冻结候选SHA逐字一致，17份替换/9份新增。逐项旧SHA、备份路径、候选SHA与安装SHA见`installation-manifest.json`；原始顺序事件见`installation-events.jsonl`，明确全部17备份核对结束早于第一份生产写入。安装后独立读回`install-readback.log/install-readback.exit`同样退出0：来源→安装26/26一致，旧SHA→备份17/17一致，映射SHA仍`a39ff51073c711fd3e5480b1fc972033f8211780182c8c6f7a5710b877855d0c`。冻结映射内`installed:false`保留为第一阶段快照，当前安装状态以安装清单的`installed_verified`为准。

按负责人反馈，仅在新`preview.js`将薄厚比较页的未成型缩略隐藏，避免覆盖右侧厚轮；未改变候选几何、材质或纹理。`node --check`退出0。补截图时原iab浏览器已不可用，同名浏览器新建页也失败，因此未取得这一行显示条件整改后的新浏览器截图；原冻结版全部截图保留，不冒称新增视觉观察。负责人可在本地预览重新打开后观察这一清晰度修改。

本阶段没有重生、重跑素材几何检查、Gradle、游戏/存档或Git写操作，也没有写shared MTL、旧工具或26路径以外的正式资源。原`check_assets.py`最后两份旧轴模型Z核对属于安装前证据；安装后这两条生产路径已是固定座拆件，不能把再次读取新固定座当成旧轴边界复验，必要回溯应读本批17份旧源备份。本作者在第二阶段没有改写原检查器或原证据；客户端最终组合检查/唯一assemble仍由消费者在共享API实际HANDOFF齐备后执行。安装与报告补充完成后停止本批写入。
