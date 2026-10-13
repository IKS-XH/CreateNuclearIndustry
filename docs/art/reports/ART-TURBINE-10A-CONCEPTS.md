# ART-TURBINE-10A 转子与轴承设计稿

本批只交离线可选外观和演示动画，未替换游戏资源、未实现客户端动画。入口：`http://127.0.0.1:8735/tools/art-assets/turbine-rotor-bearing-concepts-10/index.html`。

三稿分别为A宽弦斜叶/开放锻钢轮毂/分体端座，B同一转子内的双列短叶盘/阶梯轮毂/箱式座，C稀疏双列叶轮/开敞轮毂/外露滚子座。页面含三档大小、端面与三分之四侧视、观察窗/剖视、拆件、未成型缩小件和暂停/慢放/低中高速演示；六张原创16×16 SVG纹理直接用于多面几何的UV投影。R2机壳作为只读背景引用。

几何依据来自同批消费者只读核查：转轴沿本地+Z并绕单格中心(.5,.5)旋转；叶轮最大扫掠半径为3/5/7档1.1875/2.1875/3.1875，壳体内接半径各多0.125；叶片处于Z=.13..87、段距1。前后承轴板厚3/16，固定座/外环不随转，轴和内圈示意随动。小铜件仅作视觉标记，不声称配方含铜或新增润滑功能。现有客户端没有可供该设计稿直接使用的权威转子归属/RPM动画输入，页面速度仅为演示。

实际使用技能入口：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`、`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/brainstorming/SKILL.md`。按项目锁定版本核对：Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6；未升级或写入游戏项目。

负责人已实际浏览器复看最新主视图：三稿端面轮廓和支承近景可比较，选择项与说明同步，A侧轴及B大尺寸端面可辨。C小型、拆件及播放/暂停/低高速度/慢放操作也已实际复看。另发现各`<svg>`都从`uv0`编号，浏览器DOM中的pattern ID会跨`cardA/cardB/cardC/viewport`冲突，导致观察窗素材显示为线框；已将ID改为目标SVG的`id`前缀并保留单调序号。负责人将重载后复看窗口纹理，本次不记为最终视觉冻结或用户选稿。

## 定向源检查

检查器：`tools/art-assets/turbine-rotor-bearing-concepts-10/check_concepts.py`。使用绝对命令：

```text
C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe -B -X utf8 tools/art-assets/turbine-rotor-bearing-concepts-10/check_concepts.py
```

最终原始stdout/退出码见 `build/reports/art/ART-TURBINE-10A/check.log`，命令见 `check-command.txt`。该有限检查覆盖六张源SVG的16×16整数rect/色板/边界、JSON有限值、三稿三档、九组扫掠角点和0.125间隙、端部轴承尺寸，以及本地预览引用/控制器/外网依赖。负责人实际浏览器检查为另一项展示复看，不由静态检查代替。

首次检查器运行有两次自身断言误报，均保留在 `check-first-failure.log` 与 `check-second-failure.log`：第一处将模板化材质路径误要求为字面文件名，第二处把SVG标准命名空间`http://www.w3.org/2000/svg`误判为外网资源。检查器改为分别验证动态路径模板及六个实际源文件，并只拒绝外链资源属性；未改候选SVG/几何/预览。修正后同一命令最终exit 0。

负责人检查pattern ID冲突后，仅修正`preview.js`的前缀生成，并在检查器增加目标SVG唯一前缀断言。追加的真实命令和原始结果见 `build/reports/art/ART-TURBINE-10A/pattern-check-command.txt` 与 `pattern-check.log`，exit 0；此前`check.log`原样保留。SVG像素、候选几何和生产资源均未改动。

后续同轮独立窄审指出三项预览表达问题，已只改`preview.js`：轴与支承内圈现在接收同一示意转角、固定端板与外圈保持静止；支承螺栓变为参与面深度排序的多边形，并由`emit`统一清空已投影面；端板内面由四片面围出半宽0.19的贯穿孔，移除原伪黑圆。定向检查增加上述合同断言，追加原始命令/exit结果于 `build/reports/art/ART-TURBINE-10A/review-fix-check-command.txt` 与 `review-fix-check.log`，exit 0。尚待负责人复看修正后的最终截图与动画；未改SVG、几何规格或任何游戏生产资源。

未生成PNG，未安装资源；未修改正式模型、MTL、Java、JSON、配方、注册或共享代码；未运行Gradle、JUnit、游戏或存档操作；未执行Git写操作。
## R1 内外面与轴承可读性修正

本轮仅改预览实现与视图控件：投影、视线和深度排序使用同一相机基底；不透明实体面按外法向剔除背面，观察窗玻璃单独保留双面。前后端板现在由外/内围孔面、分段外周墙和方孔内壁闭合；前后法向相反，轴承外座/固定套筒放在端板内侧，外端板保持不透明。圆柱归一化Z端点，外壁、孔壁及两端盖按各自法向构面。轴承环端面不再逐扇区描边，使用端面填色和内外轮廓；侧壁以明暗表达厚度。新增“内侧轴承”三分之四近景，观察内侧承座与伸出轴。

本次实际浏览器复看由负责人完成：实体外板遮挡正确；内侧斜视能辨出端板、孔圈和伸出轴，未使用透面。原六SVG、geometry.json和正式资源未改。

在实际预览页面 `http://127.0.0.1:8735/tools/art-assets/turbine-rotor-bearing-concepts-10/index.html#r1-check` 调用真实构面/排序函数 `window.runTurbineR1Diagnostic()`。结果保存在 `build/reports/art/ART-TURBINE-10A/preview-r1/geometry-diagnostic.json`：远/近面输出顺序为far→near且反面剔除；前后端板分别64条边、空边/重复边均为0，外/内面法向均正确；24段空心轴承套筒192条边、空边/重复边为0，整体pass。语法检查命令与原始空输出、exit 0见同目录 `syntax-check.log`。

未导出PNG、未安装资源、未改生产模型/Java/JSON/MTL或共享代码，未运行Gradle/JUnit/游戏/存档操作，未做Git写操作。本轮交付仍是离线设计稿，用户尚未选择方案。
