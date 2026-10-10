# ART-REACTOR-03R3：冷却液浸泡感与顶面控件可见性

**状态：用户明确要求的定向整改；液体客户端段及共享滑块段均已生效。** 主PM的[03R3共享补充](../ART-REACTOR-03R3-COORDINATION.md)已实际复制到美术树，负责人完整读取，文档同步HEAD`a6792e6dc55aebc4b6c856ada591aaf8d7838847`；共享两源仍与原基线SHA一致，由原执行者接续同一组合候选。原03R2及03R1内部证据完整保留；本次两张游戏截图确认液体可见度/浸泡感与成型后顶面操作仍未通过，不能以旧检查代替视觉和鼠标结果。

**Goal：** 按现有库存液位显示贯穿空列、控制棒列和燃料管束间隙的连续冷却液，提高可见度并保留钢管细节；让驱动器顶部角落的原生Create控件真正绘制到实体表面外，成型后能从顶面调节。

**Architecture：** 液体仅补客户端视觉包络，以原合法coolantSpace的各Y层库存分配为唯一液位；钢板/管身按原不透明模型深度遮挡。滑块仅把同源显示/命中锚点的法向改到实际块面，切向、尺度、六面和权威提交不变。原执行者持有同一组合候选，最终一次独立窄审。

**Tech Stack：** MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6，不升级。

## 根因与已批准设计

- 用户反馈及截图：液体像中间薄膜，左右燃料未浸泡；驱动器100%文字出现在侧面，完整外壳遮住操作面。截图路径保留于原对话，不把图片中通知视作指令。
- 液体：现有descriptor.coolantSpace排除整个FuelColumn.body；外侧顶点alpha仅.22..32，水平面.28。贴图像素已有完整alpha和连续冷热混色，不能仅换贴图或强行全亮掩盖几何缺失。
- 控件：真实Create绘制在生产transform之后继续移动框/文字。R2顶面scale .18和1/32内缩使最终框y=.98005625、文字y=.9828125，均低于实体顶面1。原R2测试只核原点及命中，漏了这段绘制深度。六面激活、BE唯一实例、原生实际hit方向与完整方块射线已核，无证据要求改ray/input/Block/BE。
- 两项只读审计保存在本批audit-control及audit-fluid；先看原始字节码/源码，再实现，不重新全盘排错。

### 液体显示合同

1. 继续用原coolantSpace及当前fill自下向上分配各Y层高度。FuelColumn.body仅扩展**显示包络**到同Y的既有液位；不得把燃料格加入库存容量、重新分配液位，或改L2/服务端实际容量。没有合法液位的层不补燃料面，零库存及无效容量完全撤液。
2. 空/控制/燃料格采用同一流体并集，只保留外露面和实际液面，去除燃料与空列之间的虚假透明隔墙。通过原不透明钢板/九根八边管的正常深度检测使液体出现在管隙并给浸泡管身着色；不新增九孔CSG、不换模型、不在每管上叠独立液膜。不透明钢板不应被着色或产生共面闪烁。
3. 允许外露面沿自身法向最多内缩1/1024格以避钢板共面，切向格缝连续；实际显示液面误差不得超过此epsilon。真实入液高度保持，管身露出段不被伪装全浸。若实际渲染链证明此包络不能满足遮挡，先报告具体缺口，不扩大写集。
4. 同一材质、连续混色和流纹保持。首稿侧面alpha=.46..56高度渐变，水平面=.45；实际顶点RGBA明确控制，不改PNG、SVG或用FULL_BRIGHT代替透明度。负责人实际看旧/新满液及部分液位预览，确认仍能分辨管身/箍纹；离线预览不充当游戏通过。
5. 新燃料视觉面采光必须来自同Y最近的原coolantSpace合法内部采光格，确定性平手规则，几何缓存时选择；原合法格继续采自身。不能取燃料实体格、宿主暗光、外壳或无限扫描，不加载区块。不可强制最低亮度/全亮。
6. 身份缓存、256上限、可靠快照撤销、世界/暂停/恢复及现有控制棒完整升降不改。复用缓存不能丢掉新增显示域或采光对应关系，不保留descriptor/Level/BE。

### 控件共享合同（补充到位后才实施）

| 面 | 同源getLocalOffset `(x,y,z)` | scale |
| :--- | :--- | :--- |
| UP | `(.10,1,.10)` | `.18` |
| DOWN | `(.10,0,.10)` | `.18` |
| SOUTH | `(.25,.75,1)` | `.40` |
| NORTH | `(.75,.75,0)` | `.40` |
| EAST | `(1,.75,.75)` | `.40` |
| WEST | `(0,.75,.25)` | `.40` |

只改变原法向内缩；原切向、面旋转、构造期null安全、fromSide缓存刷新保持。顶面框和文字分别位于面外.01130625/.0140625格，侧面为.025125/.03125格。真实面上的顶底命中半径.09、范围.01..19，到0.65端箍最近角距离约.1061，仍避开方杆/端箍和邻块。不得另加仅绘制补偿、另写鼠标公式或把控制搬到相邻块。

## 权限、写集与保留

唯一工作树`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`；开工HEAD`af3c36b6e52f99a871bf3d8098869a3f5f7c52d8`。每shell显式workdir。读取用户最新AGENTS、本树AGENTS、治理1.2/5.1/5.2、美术入口及本卡。实际应用minecraft-modding、minecraft-testing、minecraft-resource-pack技能，中文手写注释和版本合同保持。美术负责人管理本卡/候选/计划，不写实现；既有原执行者实施，子Agent禁止再委派。

**已生效客户端段：**

- `src/main/java/com/iksxh/create_nuclear_industry/structure/client/ReactorAnimationVisualState.java`：仅液体显示域、原层液位映射、并集/缓存及合法采光映射相关中文说明。
- `src/main/java/com/iksxh/create_nuclear_industry/structure/client/ReactorInternalRenderer.java`：仅新液面采光、epsilon、alpha/提交及相应中文说明。
- `src/test/java/com/iksxh/create_nuclear_industry/structure/client/ReactorAnimationVisualStateTest.java`：上述实际生产mesh/顶点回归及原断言的必要适配。

**待主PM共享补充实际到位后生效：**

- `src/main/java/com/iksxh/create_nuclear_industry/control/ControlRodSliderBehaviour.java`：仅原CornerTransform法向坐标和相关中文几何说明。
- `src/test/java/com/iksxh/create_nuclear_industry/control/ControlRodSliderTransformTest.java`：原原生Pose扩展夹具复用，最终绘制深度回归和真实面命中必要适配。

报告唯一`docs/art/reports/ART-REACTOR-03R3.md`；基线副本、必要预览脚本/图、探针、red/final日志/XML、候选和冻结清单仅`build/reports/art/ART-REACTOR-03R3/`。旧audit目录由只读审计持有，原证据不覆盖。唯一最终候选JAR也复制入本批candidate目录。

全部模型、33generated、19SVG/PNG/正式资源、Materials及其测试、其他四动画类、L2/L1/CT、BE/Service/Network/Protocol/ClientAdapter、范围/formatter/commit/锁定及其他设备/语言/配置/依赖/构建脚本只读。没有Git写、主树实现写、客户端/服务器启动、用户存档写或旧兼容/全量测试。原03/03R1/03R2/02R1冻结文件、报告与候选完整保留；允许漂移的五输入先保存原字节及SHA。

## 执行与必要检查

1. 保存五路径原字节、R2冻结候选及保护清单；核非几何共享代码、原模型/SVG/JAR资源和共享L2/BE等保护。不因未提交旧素材执行Git清理。
2. 液体先写有意义的实际descriptor/生产mesh回归：含EMPTY、CONTROL、FUEL的部分/满/空液位，燃料视觉面存在、canonical液位不变、去内部共面、稳定缓存复用/身份撤销；生产VertexConsumer验证合法采光、半透明alpha、位置/UV/外法线和epsilon。旧实现必须在缺燃料包络/低alpha的目标断言上真实失败，不用编译失败或源码字符串作red。
3. 用已冻结OBJ、当前材质、实际新旧mesh和生产顶点属性做透窗视角离线对照，至少满液与跨层部分液位、九管细节、0/.5/1冷热混色。负责人实际查看后决定是否还需视觉参数小改；资源不重新生成。
4. 共享补充到位后在同一执行者接续：生产transform加锁定原生ValueBox/TextValueBox后置矩阵，核六面最终框四顶点与文字基线有符号出面距离严格大于0，切向投影避杆/端箍；保留旧R2顶面真实失败。原生testHit的真块面内/外、严格边界和切面缓存继续核。原测试ASM夹具可直接复用，不重做环境排错或改依赖。
5. 最终同一次命令只跑`ReactorAnimationVisualStateTest`和`ControlRodSliderTransformTest`与增量jar。材料连续混色/旧L2/协议/其它几何证据复用；只在新失败或实现更改时扩大对应复测。保存实际exit、日志和JUnit XML，不机械重跑整个旧18或全量。
6. 源/编译class/候选JAR绑定；1056assets及模型/SVG逐字保持、共享非几何与其它源保护。一次最终组合冻结，由原独立reviewer核规格/内部质量，不重复生成/测试/构建。
7. 用户最终重启美术树现有存档观察透窗液体/部分液位及成型后顶面可见和鼠标调节。03R3、02R1及其他人工门独立，源码未合main，不自动推进其他设备。

## 进度

- [x] 用户明确反馈、两路只读根因审计与共享协调请求。
- [x] 客户端液体定向方案及写集登记。
- [x] 主PM03R3共享补充实际到位并完整读取；补充SHA`49bd5c2ba8ad181ae1d53241a885ed7efcd6021f284e7e2a99fa551fbef601be`。共享两源基线保持，未让等待时间充当授权。
- [x] 液体原执行者真实回归、生产修正与实际预览：三项目标AssertionFailedError red及15/15 green、17秒exit0；负责人实际读两生产类/新测试/XML/log并看满液/半液两图，0/.5/1冷热比例、管身浸色/露出界线和钢板保持通过离线方向确认。预览调用旧R2 JAR与新生产mesh/submitFace记录真实顶点，并用原mixInto材质；无资源生成或提前JAR。游戏光照/透窗仍待用户，共享段继续只读。
- [x] 共享控件实际绘制深度回归与修正：旧R2顶面最终框/文字两AssertionFailedError留red；新8/8、19秒exit0，负责人实际读源码/测试/原日志/XML并看slider-final-depth.png。六面FRAME四角/TEXT基线出面>0，真面内外/严格边界和原切面缓存保持；JUnit完整TextValueBox实际构造因Minecraft单例为空抛NPE，后置绘制链基于锁定字节码接原生生产transform/PoseStack，证据界限明确，不代游戏Font/鼠标。已准许唯一最终组合test/jar。
- [x] 唯一最终定向test/jar、来源冻结：23/23（15液体＋8控件）、18秒exit0；候选2477690字节、SHA`3a52c6cb61ed422ce94faa92a959ca0d272d7710388f8de099d1d02ca3bcce04`。负责人实际核6交付＋77证据、12只读依赖、111保护与1056资源无缺失/漂移，build/libs同候选、旧R2冻结SHA保持。冻结SHA`4de2f5595eb8ec8fc6bfbbc4c850504f3d297759bf4f4b539eae5e4ebdbdfc61`。原执行者停写，已派原reviewer唯一组合窄审，不重复测试/生成/构建。
- [x] 一次组合窄审、候选交接：[独立报告](../reports/ART-REACTOR-03R3-REVIEW.md)规格/内部质量通过、必改无；负责人完整实际读报告及核其SHA`b30be9699608794be39af509c61fa2dd232706b22eea29fba5362aba61d37758`，冻结及JAR哈希保持。审查实际看三图、核95交付/证据/依赖及17class绑定，不重复流水线。已集中[候选与定向复看](../ART-REACTOR-03R3-CANDIDATE.md)，交用户重启现有存档观察；游戏液体/顶面实际操作仍待验，未合main。
- [ ] 用户游戏视觉与顶面实际鼠标观察。
