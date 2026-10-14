# ART-REACTOR-09：驱动器与换料口辨识度、深度控件可读性

2026-10-12用户提供密集顶盖和近距离控件截图，明确反馈控制棒驱动器/换料口不易一眼区分，顶面深度字号小且不好操作。这是现有外观和操作可读性的窄整改，目的为密集顶盖下辨认设备并方便调深；沿用Create风格、SVG素材与现有无独立GUI交互。08实机结果尚未被用户明确确认，原候选/证据/人工门保持。

工作仅 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，基线HEAD `2ae83b9c9d355c5229f973849e04c449dea0ceb5` 和既有未提交08/07B-R2。美术负责人维护本卡/PLAN、安排原执行者并审查；执行者和负责人均不写Git，不运行用户游戏/修改存档，不覆盖原制品或其他dirty。跨逻辑/交互共享改动先按专项授权与主PM协调，03R2/R3旧临时写集不自动扩展到本批。

## 阶段一：只读定位

原消费者 `reactor_ct_assets` 实际读AGENTS、治理1.2/5.1/5.2、美术README/PLAN、本卡和已安装minecraft-modding/testing/resource-pack及systematic-debugging。核当前ControlRodSliderBehaviour、原Transform测试与锁定Create真实ValueBox/TextValueBox/命中/长按设置入口；指出截图小字号的实际矩阵根因。比较主目录、美术树及可变候选相关源差异，但不修改/运行测试或构建。说明最小安全写集、顶面本格内扩大数值与命中是否能避开0.6格方杆及0.65格端箍、六面/0..100/权威提交/锁定保持方案；不自行改变协议/客户端编辑行为/玩法。

原SVG作者 `reactor_svg_impl` 实际读上述规则/资源技能，只读核两端口基础SVG/正式PNG/模型和成型CT SVG/atlas映射、CTBehaviour采用的真实方向/索引及现行配方材质。指出两者密集排列过于相似的形状/配色原因，给出最小独立工具/安装写集及草图方向。目标是换料口表达铜色装料舱盖、驱动器表达钢色升降机构；两者同时在亮度、轮廓、机械符号上区分，不仅改中心细线或叠文字。顶部中心仍为方杆路径，驱动器外围保留清晰操作区；要看实际棒体完全插入/拔出时的遮挡。本阶段不生成、安装或修改正式资源/模型/生产代码。

两者只交定位消息，不子委派。负责人核实后补后续精确素材/消费写集和必要共享协调；未明确的生产文件保持只读，不开始assemble。

## 阶段二A：独立SVG与阵列预览

作者已核两类设备共用近似钢色外框、棕金方环与深中心槽，16px小符号差异在远景丢失；基础及成型source→formal均逐字一致，不是CT错位。配方支持驱动器的执行机构身份；换料口没有显式铜配方项，本批铜色仅作为装料舱盖的视觉配色，不写成配方事实或改配方。

原作者 `reactor_svg_impl` 可只写新目录 `tools/art-assets/reactor-device-art-09/`、专属证据 `build/reports/art/ART-REACTOR-09/assets/` 与短报告 `docs/art/reports/ART-REACTOR-09-ASSETS.md`。旧基础sources/generated、reactor-ct-r1与共享工具均只读。新目录包含4张16×16基础SVG、四种sprite各16张CT SVG、局部export/check、palette/mapping/README与HTML/SVG预览，必要输出generated基础PNG/64×64图集；所有新绘制均以整数rect SVG为唯一来源，不从PNG后期绘色，不使用imagegen。

四种sprite为 `control_rod_drive_top`、`control_rod_drive_side`、`reactor_refueling_port_top`、`reactor_refueling_port_side`。保留R1 RECTANGLE 4×4、16格、isolated index12及边缘位/UV/纹理ID；生成器仅处理本批8目标，不重导其他素材。驱动器为较亮钢色双导轨/轴承座与深色中央方杆通道，外围可读机械升降标记/操作平台，小黄铜紧固件；換料口为大面积暖铜色闭合舱盖、粗铰链与锁扣，钢压边。不要用同样方环换几条中心像素、靠文本标签或纯色平面充当辨识度。维持当前厚重的亮边/暗边/凹槽层次，像素密度不提高。

实际预览必须比较原版/新稿单格顶/侧、成型连接、12×12或更大混排阵列和近远缩小图；标明离线示意，不能冒充游戏截图。驱动器以本批消费者核实的本格操作位置叠加示意，中心0.6格杆及0.65格端箍在完全插入/拔出时检查遮挡，外围身份仍需清晰。操作位置未冻结前可先做素材预览，不能把猜测坐标标为最终。负责人实际浏览器复看后再补唯一正式安装授权；此时禁止正式PNG/模型/Java写、Gradle、用户游戏/存档/Git。

后续安装候选只有8张PNG：`src/main/resources/assets/create_nuclear_industry/textures/block/`及其`reactor_ct/`下上述四个sprite的`.png`。基础16×16、CT64×64；无需改cube_bottom_top/model、JSON、item/incomplete图或注册。正式安装前完整保存这八张旧PNG到本批证据并记录来源；共享控件补充、安装和最终唯一组合打包另待生效卡。

## 阶段二B：已生效的控件共享窄改

主PM已直接核本批原用户消息，实际将 [09共享补充](../ART-REACTOR-09-COORDINATION.md) 复制至美术树；负责人已完整读取，新的两路径临时权限生效。原消费者 `reactor_ct_assets` 实際读补充再实施：仅 `src/main/java/com/iksxh/create_nuclear_industry/control/ControlRodSliderBehaviour.java` 的原 `CornerTransform` 及原 `src/test/java/com/iksxh/create_nuclear_industry/control/ControlRodSliderTransformTest.java`。生产非几何正文逐字保护，不改既有原生控件设置链或权威状态。

UP/DOWN为 `(.5,真实1或0面,.0875)`、scale `.20`、font scale `1.8/64`，四侧原offset/`.40`/super字体及命中保持；top/bottom命中为同一offset中心的严格椭球，X `.35`、Z `.075`、法向 `scale/2`，原isSideActive条件保持。构造null方向默认与fromSide缓存安全；不拉扁字体、不引入客户端注册/自绘GUI。所有其他冻结几何和验证边界依实际共享补充，不凭预览推算覆盖。

原测试先对旧生产实现得到真实几何RED，再最小生产窄改、同class定向GREEN；六面完整后置矩阵、0/50/100字面边界与端箍、严格内外/边界/中心/邻格拒绝、侧面原退路和切面缓存，调用实际生产与原生Pose，不写镜像命中自测。只保存本批 `build/reports/art/ART-REACTOR-09/consumer/` 的原log/exit/XML、几何投影及保护绑定；报告 `docs/art/reports/ART-REACTOR-09-CONSUMER.md`。原03R2/R3输出不能覆盖。真实Font/GPU/鼠标限制如实说明，长按审计和其余协议/业务原证据复用，不启动游戏或新探针。不执行assemble，直到素材正式安装并由负责人追加最终组合授权。

## 验证与交付边界

## 阶段二A安装授权：八张指定PNG

负责人已在真实浏览器逐项查看四组基础原/新、12×12近远混排、0.6格棒体/0.65格端箍遮挡及四类CT图集；区分清晰，操作区位于端箍之外。68SVG/8PNG的原check/export均exit0，预览引用故障已单独修复且原失败记录保留。原作者 `reactor_svg_impl` 现在只可把本批 `generated/base/` 和 `generated/reactor_ct/` 中四类sprite的8PNG逐字节复制到本卡已列的8个正式目标；先将八张旧正式PNG完整保存至 `build/reports/art/ART-REACTOR-09/assets/baseline/`，记录来源/候选/备份/安装SHA和尺寸。不得重导或改绘68SVG/8PNG，不得触碰模型/Java/其他资源或执行Gradle。报告追加安装8/8身份与具体备份，不将预览审核写成实机验收。

后续组合assemble仍由原消费者在素材安装结束、控件报告及真实定向GREEN核实后持有，需负责人另补授权。

## 阶段三：唯一最终组合与窄审

负责人已读实际安装8/8日志、消费双源diff、RED/GREEN原日志/exit与消费报告，并实际打开GREEN XML数据的离线布局图；9/9定向GREEN、八目标安装与源相同。原消费者 `reactor_ct_assets` 现在持有唯一增量 `assemble --console=plain`，不得重测原class或运行全量/clean/游戏。先完整保存原ART08 JAR并核SHA；最终JAR另冻结于本批consumer，不覆盖旧08/07B-R2冻结。记录命令、原日志、自然退出码和实际源/两compiled class/八PNG（来源SVG、generated、formal、processResources、JAR）一致性，仅核本次变更与必要既有输入保持，不重新建立全仓保护矩阵。

净09来源清单明确列双源、八正式PNG、新工具目录和本批报告/任务文档，既有07B-R2/08dirty只作为先前输入，不夹带到净09同步。报告补组合身份与未运行Font/GPU/鼠标限制。完成后停止源修改，原审查者 `reactor_ct_audit` 对此同一组合执行一次独立规格/质量窄审，只写本批REVIEW/证据；同一审查整改可复核具体差异，不机械重复构建测试。主PM负责按净清单接入可变候选，人工视觉与操作门仍待用户。

阶段三内部交付完成：负责人已实读[审查](../reports/ART-REACTOR-09-REVIEW.md)、原RED/GREEN XML/log/exit、安装日志和唯一3秒exit0组合日志；两源、两class、八资源与当前/冻结JAR及99净来源绑定一致，同一次审查无必改项。所有执行者停止写入。当前美术JAR2,624,615字节/SHA737d3bd5606109f711998da92714917de19b6ed891f0abb7fbb27ca3f1049050；最终freeze SHA79fdaf1e3895f390f40faef4248d5e72ec8380bd2a4b0008228f4fce7ec1e67d，旧08/R2冻结保持。主PM已收精确清单并计划与其用户另行指定的无交叉换料操作范围做可变候选唯一组合；本卡不授权该玩法写集。[当前候选](../ART-REACTOR-09-CANDIDATE.md)区分本批已核证据与待完成接入/真实人工门。

可变候选净接入现已完成，艺术源26f4ba25ff71085009bf799ded418e9b796c141d、最终文档HEAD d819fb66da0b1c355a324201f4456d73406b3f4f；艺术双源/八图及控件class保持，必要最终组合包2,677,917字节/SHA896c2c38d984680eb61452d2a6ba5757e4a08e8931a2f19335acc5e26a57d6bf。负责人实读最终日志/exit、绑定、候选与集中单，实际启动入口及交接报告换行规范化身份已写09候选。原艺术freeze/旧备份不改，全部生产执行与审查停写，交用户真实Font/GPU/鼠标/密集辨识复看；不扩展任务或关闭其他人工门。

新资产在本批新SVG工具目录产出，旧producer/R1/08源不重导。负责人实际浏览器复看单格、混排大阵列及棒体遮挡对照后安装指定资源；自动检查只核本批SVG、来源/安装/包身份。控制几何/命中只按实际改变做必要定向回归，原协议/库存/其他美术矩阵复用。一次最终增量assemble、原08/07B-R2制品保留、同一次组合窄审；实际字号、顶面长按操作与密集辨识度交用户现有存档复看。可变候选净接入由主PM管理，不将其玩法导入美术树。
