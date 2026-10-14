# ART-CENTRIFUGE-14A 组合规格与质量窄审

2026-10-14。唯一独立审查者，工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`；只写本报告，无子委派、Git写入或其他文件写入。已读取最新main AGENTS、[14A卡](../tasks/ART-CENTRIFUGE-14A.md)、治理1.2/5.1、美术入口及minecraft-resource-pack技能；verification-before-completion/requesting-code-review按本卡有限实物证据执行，不重跑生成器、HTTP全套、MC、Gradle或旧设备检查。

**结论：设计稿与本地真实模型预览符合14A范围，当前无剩余必改项。** 首次保存按钮真实缺陷在同次审查中窄修并由负责人实际复看关闭。此结论不代替用户选择A/B/C，不代表Minecraft视觉或动画验收，也不授权正式资源安装。

## 资产与真实加载链

复用[ASSETS](ART-CENTRIFUGE-14A-ASSETS.md)和原始assets/checks、manifest、readback；审查者只读核manifest的14来源及22输出，0项漂移。12份SVG为16²有限色板/crispEdges，9模型四槽引用与显式UV有效；23元素转子坐标、旋转定义和UV保持，只有材质分区变化。A68/70、B76/78、C108/110上下格元素体现长窗、厚端座加强筋、分段护板的形体差异，并非仅换色。新增板按实际尺寸投影UV；越域修正平移UV而不缩放跨度，不改变像素密度。

已读make-assets的端座/八棱护板/孔缘构成和scene加载实现：九份实际JSON经loadModel读取；四槽SVG逐份解码为16²内存PNG字节并进prepareAssets，模型只重定向虚拟纹理路径，未重写元素UV。纹理缩略图与模型材质同源。block-model-renderer2.20.0/Three180沿已有本地库，无公网资源、安装或缓存扩建。

库实际元素坐标减[8,8,8]；预览lower位置Y=.5、upper=1.5，对应原坐标上下+16；rotor在Y=1枢轴下位置-.5，恢复lower原点坐标，绕[8,16,8]/Y轴，既有23元素不漂移。实体钢壳不透明；glass槽是实际孔缘，SVG本身不透明且中央无覆盖面，不能把可见内部误记为整面透明玻璃。剖视仅裁壳体，完整转子保留。

接口说明与只读CentrifugeBlock/BlockEntity/Renderer一致：上段UP矿浆输入/回取，上下四水平面清水和两粉末输出，下段DOWN动力；原北向下检修盖随facing，未新增指定侧功能口。钢/少量金色材料有现行配方依据。正式src、玩法、配方、动力/旋转、世界、配置和JAR不属于本批写集。

## 预览与保存整改

复用[PREVIEW](ART-CENTRIFUGE-14A-PREVIEW.md)和preview/finite-checks.json的52/52来源/HTTP检查；该证据明确只是HTTP与来源，不作为GPU视觉结论。server仅127.0.0.1，固定本页/库/正式离心机白名单读取；路径解析限制在各目录，POST最多四图、限载荷并只写本批固定报告文件，无任意目标路径。

负责人首次实际capture发生await后event.currentTarget=null，finally恢复disabled抛TypeError，AX误报模型加载失败；POST实际已经保存。原错误及首次三图/JSON保留于preview/capture-first-error.json和capture-first-attempt/。审查窄复核scene第44行：await前固定const button，finally使用此引用；保存异常提示改为保存失败、预览可继续。未改模型/UV/其他操作；复用capture-fix-check的语法exit0与HTTP200，未重复52项。4190的Node Fetch bad port与根IAB能显示的事实均保留，不冒称浏览器加载失败。

负责人在4191完成最终实际CUA观察并交回：正面、背面、底部正常，剖视完整转子可见且低速旋转；修后capture提示保存成功、按钮恢复enabled。点击停止后相位5.297898075209881，稍后再次保存仍同值且slow=false；重置回yaw=-35/pitch16/zoom1/phase0，另关剖视。审查者实看负责人overview.png与cutaway-stopped.png，三稿轮廓/分区可辨，剖视钢色转子和窄金环可见。

负责人截图位于 `C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14/`，包含overview、bottom、cutaway-moving、cutaway-stopped。最终browser-evidence.json和三latest.png是正常非剖视姿态；审查者读回ready=true/errors=[]、cut=false/slow=false/phase0，22实际输入、36服务器来源，来源逐项SHA无漂移，三稿drawCalls为10/11/11。输入及capture来源与当前实物绑定，不以文件存在代替实际浏览。

最终preview-source-identity.json的六份网页源另逐项只读核SHA，0漂移。当前入口 `http://127.0.0.1:4191`、自有PID84900由负责人保留给用户选稿。网页24°/秒仅姿态演示，不读游戏状态或声称性能。内部审查与负责人网页观察已齐备；用户选稿与以后正式实施/游戏验收仍独立。审查至此停写。

## R1 精修差异追加（2026-10-14）

用户实际看初版后要求“能不能更精致一些”，按原14A卡R1接续本次局部审查。上文为初版审查时点，不能追认为用户认可风格；初版资产/producer、网页和证据已分别原字节归档history/pre-r1，首次UV、capture及4190差异不被覆盖。

**R1结论：当前精修差异无剩余必改项，可交用户复看选稿。** 不代用户选择A/B/C，不代表Minecraft视觉验收或正式实施授权。

已读原作者ASSETS/PREVIEW的R1节和本批r1证据，复用资产受影响检查exit0/43项读回以及网页30/30输入HTTP与scene语法exit0；审查者不重跑生产器、30/52矩阵或设备测试。只读核当前R1 manifest的14正式来源与28输出，0漂移；七网页源身份另核0漂移。原四槽正式基线保持，候选新增machined/fastener经双方协商记录，为六槽18份16²SVG与9JSON；真实面分别使用加工钢窗框/台阶/钢杆、退台紧固件，未闲置新槽或只改缩略图。

精修三稿仍A简洁钢筒、B深钢厚壳八筋、C暖灰分段护板，壳体元素A144/155、B152/163、C184/195。顶面大金板改为层叠钢肩/退刀窄槽/钢凸台法兰，金色仅8.25模型平方单位窄压环，四组实体螺栓；最高upper-local16、顶部最上x/z7..9口域保持开口。北检修位置改凹片/搭接框/四紧固，动力原两套元素保留。八面完整四边钢框/染色密封保留实体孔；B1.9孔扣密封后净宽约1.44，A2.2、C正2.7/斜1.5。没有新增指定运输口、假仪表或碰撞/行为。

新增框、台阶和紧固件按模型真实面跨度取UV；螺栓只平移纹理取样起点到[7,7]，不把整张16²图压到小头部。转子仍23元素原坐标/旋转/全部UV，仅八杆切加工钢槽；两格偏移及[8,16,8]/Y轴线未变。只读对比初版scene：改动是按契约动态枚举槽、逐槽入内存资源包/重定向模型槽、同源六缩略和证据槽列表；相机、灯光、尺寸、剖视、动画速率、停止/重置及已修capture未改。server字节仍4702f8b4…，PID84900未重启，固定路由与报告写边界沿原结论。

负责人在当前IAB Tab11重载4191，实际观察R1正常三稿、俯视顶法兰、正面检修盖、剖视新材质；最终原生capture按钮保存成功且恢复enabled。审查者只读实看同目录的overview-r1.png、top-r1.png、front-r1.png、cutaway-r1.png：钢法兰/四紧固、完整窗框和检修层次可辨，剖视转子完整，三方向差异保留；原overview及其他初版截图未覆盖。当前三latest.png/browser-evidence是R1正常姿态，审查读回ready=true、errors=[]、28输入、42服务器来源，42来源SHA0漂移；六槽列表完整、元素数匹配，drawCalls15/16/16，yaw=-35/pitch16/zoom1/cut=false/slow=false/phase0。新增贴图与当前实物和实际浏览绑定，HTTP记录本身不被当作视觉验收。

R1仅追加本报告，未子委派、Git写、资源安装、MC/Gradle或其他文件写。当前Tab11为负责人交付页，旧Tab10不是live；4191保留供用户精修三稿复看/选稿。审查至此再次停写。

## R2 转子增粗局部追加（2026-10-14）

用户要求“转子加大一些吧”，原卡R2允许三rotor横向几何及相应UV改变；端轴边界进一步明确整根1.3宽主轴保持。仅复审此差异，原已审壳/六槽材质/渲染链不重开；初版/R1、原失败与R1 pre-r2归档均保留时点。

**R2结论：当前增粗差异无剩余必改项，可交用户复看。** 这是候选美术和网页观察结论，不代用户选稿、Minecraft验收或正式安装。

已读ASSETS R2及r2/checks/readback/manifest，复用原作者受影响检查exit0与43当前/8归档身份结果，未运行生成器或测试。审查只读核14正式来源、4当前输出、24R1六壳/18SVG保护文件与八网页源，SHA0漂移。实际rotor_r2以轴x=z=8缩放元素1..22的from/to XZ和局部originXZ×1.28，Y/originY/angle与23分工保持；element0整根坐标/UV原样，未扩大既有嵌座范围。纵杆宽.76→.9728、立叶.8→1.024，侧面仅重投影横U而原V保持，顶底按横截面真实跨度投影，仍在0..16；不是扩大网页Group或拉伸原贴图。

已读解析扫掠算法：本批元素均为Y旋转矩形挤出，距离轴线在矩形上连续，最小半径由轴是否在内/边段最近点求得、最大半径由角点求得；360度并集恰为该径向区间圆环。与固定壳/窗框/端座的径向区间和Y挤出区间求距，径向与轴向独立，另按所有有效Y端点分层复核；对当前实物成立，不是少数相位抽样。扫掠包络直径7.874802→10.079747，约增加28%。22增大件最小实体净距三稿均.960模型单位，发生于下层连接件与下端座的轴向间隔；同Y径向最小另为A1.770126/B1.640126/C1.730126。不能把.960说成全圈径向间隙，也不是制造公差/真实性能或碰撞验收。

原主轴Y5..27本来嵌入闭合下/上端座，实际八项shaftExistingContacts单列；原端座没有独立真轴孔。R2没有改壳、造假轴孔或扩大主轴，净距通过仅覆盖其它22件，不声称23件全净空。轴承端座既有边界按卡保留，未隐去重叠。

预览scene与R1归档只差revision初始化/证据/manifest读回三处；scale1/16、枢轴、相机、灯、演示、控件和capture逐字保持，server仍原字节/PID84900。复用preview/r2的6/6三rotor+契约+index/scene同源HTTP与两脚本语法exit0、patch-scope，不重跑18SVG/30/52或旧操作矩阵。

负责人在4191当前Tab12实际观察正常窗内/剖视和低速：同R1相机相位0的cutaway-r2显示主体、杆/叶片变粗而壳/法兰/窗框保持；实际低速capture相位.013990559283984108→6.179931628631883，另一角度无新增擦壳/跳动。随后停转、关cut、重置phase0并原生capture成功，按钮enabled。审查者只读实看cutaway-r2.png与overview-r2.png，当前三稿体量与外壳保持关系可辨；moving-r2.png和以上截图独立保存于原centrifuge14目录，不覆盖R1。当前三latest/browser-evidence是正常R2姿态，审查读回revision=r2/ready=true/errors=[]、yaw=-35/pitch16/zoom1/cut=false/slow=false/phase0；28输入/42来源SHA0漂移，drawCalls15/16/16，真实候选与网页绑定。

当前Tab12为负责人交付页，旧Tab11不是本轮live，4191/PID84900未重启并保留供用户复看。本次仅追加原REVIEW，无子委派、Git/MC/Gradle/资源安装或其他文件写。R2审查至此停写。

## R3 底部独立旋转轴及同次长轴窄修（2026-10-14）

用户指出底部动力点应为旋转轴，原卡授权真实shaft候选、必要静态底台围孔及网页同相位演示。首轮4×4×4虽实现独立转动/静座分离，负责人实际剖视仍见立方钢块；此具体形体问题没有登记通过。首轮资产/producer、证据和bottom/moving/cutaway截图原样保留。负责人随后确认4×4×8长轴及原四端板中心围孔的最小写集，当前只复审这一完整R3差异，原材质、上壳与R2转子不重开。

**当前结论：长轴形体必改已关闭，R3无剩余必改项，可交用户复看选稿。** 仅候选模型/网页演示，正式游戏旋转接入仍须选稿后独立实施写集；本批未改Java、动力或停机语义。

最终shaft是每稿实际JSON闭合4×4×8加工钢杆，XZ6..10/Y0..8，x=z=8；侧面UV4×8、端面4×4，取现有16² machined SVG真实跨度，未拉满纹理或只改名称。首轮lower仅原0/1/6/7/23改四角脚与6.4围孔静座，外轮廓/Y0保留，Y0..1.25露出轴侧面且Y0端面可见；长轴窄修再将R2原2..5四端板各拆两端条，共八片。原板窄宽约6.0675小于孔6.4，仅去中央必要区域；局部孔域4.8..11.2，Y微错层、外界、旋转origin/angle不变，UV按局部原坐标投影保密度。最终lower A167/B175/C207；首轮R3其它159/167/199对象逐项保持，检修盖/静座未扩大。

已读最终producer、r3-long/checks/manifest/readback，复用原作者受影响exit0、46当前/35首轮归档身份核对，未重跑生成或旧矩阵。审查只读核14正式源、24上壳/18SVG/R2三rotor保护及7输出SHA，0漂移。原R2增粗转子23元素全部字节保持。新增轴解析360度最大扫掠半径√8=2.828427，对所有实际静止lower的径向/Y挤出区间求距，最小.371573模型单位，等于孔内缘3.2减扫掠半径；不是零相位1.2面隙。所有静态接触当前均空，首轮Y3.98..4端板约.02嵌座已被真孔消除，旧装配豁免不沿用。

长轴Y5..8与原转子主轴0嵌接、Y7..7.65与横连接件9/10相交、顶Y8与内斜叶19..22底面接合，均在同运动组同相位，作为候选轮毂接合单列，不能混为静止摩擦或游戏传动规则。原上端细主轴嵌闭合上端座边界仍保持，不宣称完整转子全部净空。

预览按契约动态枚举真实lower/upper/rotor/shaft并loadModel，shaft与rotor同挂原[8,16,8]/Y枢轴，scale1/16、lower-local子位置Y=-.5正确恢复Y0..8；lower/upper仍静止。原机对照仍三模型，不捏造shaft。scene只作必要部件映射/来源证据与说明，相机、灯、尺寸、速率/控件/capture保持，server原读取/保存白名单和PID84900不改。复用首轮9输入与最终长轴7/7变化输入同源HTTP、语法exit0/patch-scope，不重跑18SVG、30/52或MC/Gradle。

负责人当前4191/Tab13实际看剖视长杆、底部侧/端面与低速，实点capture成功且按钮enabled；审查者实看cutaway-long-r3.png与bottom-long-r3.png，长杆长度与截面区分明确，静座与端面可辨，首轮立方轴不足已由实物关闭。moving-long-r3.png及motion-long-r3.json独立保留，实际相位0→.9483421023638166；审查读回三稿shaft/rotor均同相位.9483421023638166、shaft scale各.0625且inPivot=true，lower phase0，负责人观察无新增擦壳/跳动。

最终停转/关cut/reset后切底部再capture，当前browser-evidence和三latest为真实长轴，契约6634afc2…；审查读回revision=r3/ready=true/errors=[]、31输入/45来源SHA0漂移，yaw=-25/pitch=-46/zoom1/cut=false/slow=false/phase0。原R1/R2与首轮R3截图没有覆盖，长轴三截图和motion JSON位于原centrifuge14目录。当前Tab13标交付，4191/PID84900原服务保留给用户复看。仅追加本报告，无其它写入/测试/Git或子委派，R3至此停写。
