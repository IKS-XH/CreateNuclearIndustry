# 铀浓缩离心机三套设计稿交付

2026-10-14用户已选择A并明确“开始实施”，选稿门已关闭，接续[14B实施卡](tasks/ART-CENTRIFUGE-14B.md)。下文为设计稿阶段来源与观察历史，不把选择追认为Minecraft视觉/动画验收；正式候选以14B交付为准。

## 当前 R3 底部旋转轴

2026-10-14用户指出底部动力输入是方块，应为旋转轴；当前[同一预览](http://127.0.0.1:4191/)已接三稿真实独立加工钢长轴。轴为4×4×8模型单位（0.25×0.25×0.5格），XZ6..10、下格本地Y0..8；侧面UV跨度4×8、端面4×4，仍复用16²加工钢SVG。轴与转子绕原x=z=8竖轴同相位演示，底台/轴座/壳体静止，不使用网页临时几何或整体底座旋转代替实际模型。

底台中央改为围孔平台和静止轴座，四角支脚让下段轴侧面和端面可见；原四片下端板在自身局部坐标围6.4方孔，拆为八端条，外轮廓、Y微错层、旋转origin/角度与表面UV密度保持。轴全圈扫掠半径2.828427，孔最小半径3.2，三稿所有静态轴间隙最小0.371573模型单位；与R2主轴/下横连件的同相位轮毂接合单列，不声称整个转子全净空。R2转子23元素、三上壳与18SVG共24文件字节保持；当前lower元素A167/B175/C207，首轮R3其余159/167/199个lower对象保持。

首轮4×4×4短轴的结构/同源检查通过，但负责人剖视确认仍像立方钢块，未登记轴形体完成；首轮源码/producer、检查、相位2.6915909218897034及截图均保留历史。最终长轴是同次窄修，真实轴孔已消除首轮端板0.02嵌座，不沿用该豁免。新assets/r3-long证据一次受影响检查exit0、46当前/35首轮归档身份零漂移；preview/r3-long仅7/7变化输入HTTP/磁盘同源，网页代码/相机/灯光/缩放/控件/server保持，首轮语法和已审操作复用，不跑旧矩阵、MC/Gradle或打包。

负责人通过CUA实看完整杆身剖视、底部侧/端面和低速不同相位，保存相位0→0.9483421023638166，shaft/rotor相同、lower/upper为0，未见新擦壳或跳动；实际capture成功并恢复enabled。最终停转、关剖视、重置再切底部，ready=true/errors=[]、31输入/45来源、yaw=-25/pitch=-46/zoom1/phase0。独立截图为[长轴剖视](C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14/cutaway-long-r3.png)、[低速不同相位](C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14/moving-long-r3.png)与[最终底部](C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14/bottom-long-r3.png)，运动原始捕获另存同目录motion-long-r3.json。

负责人已读回[同次R3窄审](reports/ART-CENTRIFUGE-14A-REVIEW.md)，长轴形体必改已关闭，无剩余必改项；此内部结论不代替用户选稿或Minecraft验收。当前契约SHA256为6634afc2c06b183d759519a351d61d2166f78b22e04662cd882d0d1b89f16ee6，scene为94518bb07bdcb4e8e48056a6fdb275d8b7d36def31853ce57e729616db7db7d2。Tab13已标交付，4191/PID84900原服务保持；仍只交设计稿，不安装正式资源或改Java/动力/世界/配置/JAR/Git。

## R2 增粗转子历史

2026-10-14用户要求转子加大，当前[同一预览](http://127.0.0.1:4191/)已接三稿R2真实转子。23元素中22个主体/杆/叶片/连件横向XZ增大1.28倍，主轴元素0保持原1.3截面、坐标和UV；整机Y、各分件Y/局部旋转角、竖轴和R1外壳/18SVG保持。旋转originXZ同步，侧面V保持、U及顶底面按真实新跨度取样，不用网页group缩放代替模型变化。

| 当前几何指标（模型单位1/16格） | R1 | R2 |
| --- | --- | --- |
| 主体整圈扫掠圆包络直径 | 7.874802 | 10.079747（+28%） |
| 八杆截面宽 | 0.7600 | 0.9728 |
| 立叶截面宽 | 0.8000 | 1.0240 |

这里的直径是盒状分件扫掠包络，不是连续圆柱。解析360度扫掠以Y层和径向区间计算，22个增大件有正净距；同Y层最小径向净距A1.770126/B1.640126/C1.730126，下连接件至端座的最小轴向净距0.960。原主轴本就嵌入闭合端座，保留该基线接触并单列，不宣称全部23件端座净空，不伪造轴孔或改壳。

负责人通过CUA实际查看R2剖视与正常窗内，体量更宽/杆叶更粗；低速capture相位0.013990559283984108→6.179931628631883，另一角度未见新增擦壳或跳动。原生保存按钮成功并恢复enabled；最终停转、关剖视、重置为yaw=-35/pitch16/zoom1/phase0，ready=true/errors=[]，28输入/42来源。独立截图为[剖视增粗转子](C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14/cutaway-r2.png)、[低速另一相位](C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14/moving-r2.png)与[正常窗内](C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14/overview-r2.png)，R1截图未覆盖。

受影响资产检查exit0，43当前/8R1归档身份无漂移，24壳/18SVG保护及14正式来源保持；仅三rotor/契约及必要网页6/6输入同源、语法exit0，不跑原SVG/30/52或设备矩阵。负责人已读回原结果和[同次R2局部审查](reports/ART-CENTRIFUGE-14A-REVIEW.md)，无剩余必改项；原相机/光照/尺寸/演示/capture/其它控件不改。当前契约SHA256为57996e4fd946ddee30df88884df8efa7244b614c6b2988bcf9a472554ae3afcd，scene为eb37ea6d015a19401a831b38d9f620e94ab13e0350c89b408215958aa4b0b6b1。详见ASSETS/PREVIEW R2及本批assets/r2、preview/r2证据。

旧Tab11在本turn getTab不可见，负责人同一浏览器重开Tab12并markDeliverable；4191/PID84900始终不重启。当前等待三稿复看/选择，不改主目录、游戏资源/逻辑、Git、世界、配置或JAR。

## R1 精修稿历史

2026-10-14用户实看初版后要求更精致。当前同一[互动入口](http://127.0.0.1:4191/)已更新三套精修稿，仍等待选择A/B/C；初版检查与内部观察不追认为风格通过。

顶面大金板缩为嵌入钢法兰的小金色压环，层叠钢肩、窄槽与四处实体紧固点更清楚；北检修盖增加凹钢片、搭接框和四紧固，八面窗口有完整钢框和窄密封边。机加工钢/紧固件为新增独立槽，六材质18份16²SVG直接加载进实际模型，缩略同源；保持原相机/灯光/尺寸、两格比例、接口与23元素转子全部坐标/UV。

| 当前稿 | lower/upper/rotor元素 |
| --- | --- |
| A 精密钢筒 | 144/155/23 |
| B 铸钢加强壳 | 152/163/23 |
| C 分段检修机壳 | 184/195/23 |

负责人已通过CUA实看正常三稿、俯视顶法兰、正面检修盖与剖视，实际保存按钮成功且恢复enabled；当前browser-evidence为ready=true/errors=[]、yaw=-35/pitch16/zoom1/cut=false/slow=false/phase0，28实际输入/42来源，draw calls15/16/16、triangles3864/4056/4824。原转停及capture行为没有修改，复用已过证据。当前截图是[完整精修三稿](C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14/overview-r1.png)、[顶法兰](C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14/top-r1.png)、[正面](C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14/front-r1.png)与[剖视](C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14/cutaway-r1.png)，旧截图未覆盖。

原作者受影响资产检查exit0、43当前及27归档身份无漂移，14正式来源与初版相同，顶口/原动力套及转子保持。网页30/30当前输入HTTP200且localSame=true，scene语法exit0；原52项不重跑。负责人读回实际结果和当前SHA，[原同次R1局部复核](reports/ART-CENTRIFUGE-14A-REVIEW.md#r1-精修差异追加2026-10-14)无剩余必改项。原资料及证据原字节留在各自history/pre-r1；详情见ASSETS/PREVIEW的R1节及本批r1/checks、readback、input-checks和manifest。

当前契约SHA256为78a44c10703cb053e19cde2c927ee5ac7ba2fddd07dea34c25e4351a5ee6d299，scene为9f29869f90de0fcce513d624e81d659e10156d1a61fc605d367448a371cf5421。旧Tab10在当前live inventory中不存在，负责人同一浏览器重开Tab11并markDeliverable；4191/PID84900始终保留且server未改/重启，其他服务不操作。仍只交设计稿，用户风格选择与以后游戏安装/验收独立。

## 初版交付历史

下段12SVG/四槽、元素数及输入身份只对应已归档初版，当前稳定入口是上方R1。

2026-10-14。用户批准先制作三套设计稿；按[14A卡](tasks/ART-CENTRIFUGE-14A.md)完成候选模型/SVG与真实模型网页，当前等待选稿。

[三稿互动预览](http://127.0.0.1:4191/)已在IAB Tab10打开并标记为交付页，可统一切换正背面/侧面/俯视/底部、壳体剖视、停止/低速演示、重置角度与相位，逐槽查看实际SVG。网页低速为固定24度/秒展示，不是游戏实际RPM。

| 稿 | 方向 | lower/upper/rotor元素 |
| --- | --- | --- |
| A 精密钢筒 | 中亮钢灰、长窗与简洁纵向骨架、细金色端环 | 68/70/23 |
| B 铸钢加强壳 | 深钢灰厚端座、宽加强筋、小面积金色轴承件 | 76/78/23 |
| C 分段检修机壳 | 暖浅灰分段护板、深钢骨架、上下分区 | 108/110/23 |

12份16²像素SVG、9份候选JSON与[契约](../../../../tools/art-assets/centrifuge-concepts-14/assets/concepts.json)可编辑，生成器可复现。预览将实际四槽SVG解码至内存，由block-model-renderer2.20.0加载候选JSON，模型和纹理缩略同源。原23元素转子坐标/UV与竖轴保留，仅调整材质；少量金色有当前含金板配方依据。实际接口为上段UP矿浆进/回取、上下四水平面清水/两粉末输出、下段DOWN动力，北下检修盖随facing旋转。观察孔的染色窄缘保留，机壳不整体透明，剖视是网页工具。主目录与美术树的正式三模型/四PNG逐项SHA相同。

负责人实际CUA查看三稿正面、背面、底部与斜视，剖视完整转子可见，低速动作可见。首次capture已保存POST但await后event.currentTarget=null触发按钮错误并误报模型加载失败；原错误/三图/JSON保留，原作者只修按钮引用与文案。修后同页重载、多次实际保存均显示成功，按钮恢复enabled；当前ready=true/errors=[]。

实际低速首相位0.020985838925982256；点击停止后为5.297898075209881，稍后再次保存保持同值且slow=false。重置角度/相位后回到yaw=-35、pitch16、zoom1、phase0，另关闭剖视。当前browser-evidence与三latest.png为正常停止姿态；实际22输入/36读取来源，A/B/C draw calls为10/11/11、triangles为1912/2104/2872。截图为[完整三稿](C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14/overview.png)、[底部](C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14/bottom.png)、[剖视低速](C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14/cutaway-moving.png)与[停止](C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14/cutaway-stopped.png)。

资产有限检查exit0，37项来源/输出/producer身份读回无漂移，首次UV越域失败保留。网页52/52依赖/来源检查exit0，保存补丁只补受影响语法/HTTP和真实操作，不重跑原全套。负责人读回原结果、当前实物及[唯一组合窄审](reports/ART-CENTRIFUGE-14A-REVIEW.md)，无剩余必改项。详细证据见[ASSETS](reports/ART-CENTRIFUGE-14A-ASSETS.md)、[PREVIEW](reports/ART-CENTRIFUGE-14A-PREVIEW.md)及build/reports/art/ART-CENTRIFUGE-14A。静态结果/实际网页观察/用户选稿分别记录，不代替Minecraft验收。

当前契约SHA256为88fe6de0a87b8a7feb1d9cf009b01aec64a627a0f0c185bbe97b37166b879173；预览scene为dc2bfb692448a123bd0a79ce95cf44275346f9008fa08135d14527a35102e9fe。服务使用D:/Program Files/nodejs/node.exe，PID84900、server.mjs 4191、仅监听127.0.0.1，保留供选稿；旧自有2112已核身份停止。4190的Node Fetch bad port与根IAB实际能显示按环境差异保留，不伪记浏览器失败。

只交设计稿，不登记用户选稿/游戏视觉通过，不安装正式纹理PNG、替换src、改旋转/配方/逻辑、运行MC/Gradle/JUnit/GameTest、Git写或操作世界/配置/JAR。新增PNG仅网页观察截图；最新main美术/JAR及其他逻辑人工门保持。
