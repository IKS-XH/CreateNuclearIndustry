# ART-CENTRIFUGE-14A 网页预览交付

2026-10-14。执行者负责网页预览，工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，基线 `88b8fd289d49df90d7e3c655b6e81381ad01130f`。按14A卡、最新用户AGENTS及治理1.2/5.1执行；不取得PM或Git权限。

## 当前入口与写集

最终入口 [http://127.0.0.1:4191](http://127.0.0.1:4191)，自有PID **84900**，`D:/Program Files/nodejs/node.exe`，参数 `server.mjs 4191`，cwd为美术树 `tools/art-assets/centrifuge-concepts-14`。端口启动前核为空闲，Windows `Start-Process -WindowStyle Hidden`，服务保留供负责人及用户实看，不在交付后自动停止。身份见 `build/reports/art/ART-CENTRIFUGE-14A/preview/runtime.json`。

仅新增 `index.html`、`preview/scene.mjs`、`preview/style.css`、`preview/verify.mjs`、`server.mjs`、本目录 `README.md`、本报告及本批 `build/reports/art/ART-CENTRIFUGE-14A/preview/`。资产作者的JSON、SVG及生成器只读；主目录、正式资源、Java、旧Logo/封面、第三方、世界、配置、JAR与Git均未写。

## 实际预览合同

- 直接读取作者 `assets/concepts.json` schemaVersion1、A/B/C九模型与十二SVG；材质四槽 casing/brass/glass/dark 一一绑定实际来源，SVG浏览器解码成16²内存纹理，不写磁盘生产PNG或安装资源。
- 实际库目录是同级 `E:/MyMC/NewMod/block-model-renderer`，2.20.0为版本。dist JS/WASM逐项符合主工具既有锁定manifest SHA；Three只读使用主工具现有0.180.0。没有安装、升级或准备全量缓存。
- 真实模型加载后将上壳体偏移16模型单位，转子下格原点Y5..27绕[8,16,8]竖轴；壳体与独立转子分组。三稿同一正交相机、尺寸、灯光，拖动/滚轮同步。网页观察转台只改变相机，低速为24°/秒网页姿态演示，明确非游戏实际RPM；停止保留当前相位，重置回默认视角及零相位。
- 正面/背面/侧面/俯视/底部按钮、壳体剖视、原机对照、实际纹理像素近看均在本页。剖视仅以Z=8中心面裁去北半壳，完整转子保留；不是游戏结构。原机直接读取正式60/62/23元素JSON及四PNG，其七项SHA与作者基线一致。
- glass使用原SVG/PNG alpha并保持可见淡染色，双面观察合理；普通壳体不透明，纹理/UV不重画、不替代色块。作者已说明玻璃槽是实际观察孔染色边缘，未把整片机壳伪装成透明壳。
- 顶部UP矿浆输入/回取、上下四水平面清水与两粉末输出、下段DOWN动力和随facing旋转的北向检修盖说明来自实际接口，不新增假管口或改行为。
- `window.centrifugePreview.getState()`只读记录当前实际输入SHA、模型元素数、相机、材质、draw-call与错误；显式错误区域保留失败。固定 `/capture` 仅接本页最多四幅观察画布及JSON，写本批报告目录，不能任意写路径。

## 本批证据与边界

实际使用 `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md` 核1.21.1元素/UV及槽引用；实际读取verification-before-completion与requesting-code-review，按卡只用有限实物证据，组合窄审由负责人安排，不子委派。

`node --check` 对本页server/scene/verify均exit0。`node preview/verify.mjs http://127.0.0.1:4191` 实际 **52/52** 本页依赖/来源有限检查exit0，包括21候选源HTTP、七正式对照SHA、现有库/Three身份和未开放路由拒绝；原始结果见 `finite-checks.json`。该记录明确 `httpAndSourceOnly=true`、`browserVisualVerified=false`，不能当作视觉验收。标题去重为后续仅文案窄修，随后scene HTTP200核补丁生效且语法检查exit0；最终六网页源身份见 `preview-source-identity.json`。

子执行者首次CUA创建可见IAB返回 **“IAB visibility is not supported in a subagent thread”**。按任务卡直接交根负责人接管同一入口，没有反复重置、headless或外部浏览器绕过。根负责人已反馈原4190页实际加载、三卡不同机身且已截图；最终4191的正背面/剖视/转停与证据保存由根负责人继续实看，最终状态以负责人实际证据为准。

### 负责人4191实看与保存回调窄修

负责人实际浏览4191，反馈壳体剖视＋低速显示正确。首次点击保存后AX出现“实际模型预览加载失败”，原始错误 `TypeError: Cannot set properties of null (setting 'disabled') at scene.mjs:43:553`。实际回读显示POST已经保存三PNG及browser-evidence：ready=true、errors=[]、cut=true、slow=true，A/B/C drawCalls为10/11/11、triangles为1912/2104/2872。首次三图与JSON原样保留在 `preview/capture-first-attempt/`，原错误和根因保留在 `capture-first-error.json`，不删改为无错误历史。

根因是保存回调的await完成后 `event.currentTarget` 已清空，finally恢复disabled时误读null；不是loadModel失败。只将按钮引用在await前保存为const button，finally使用该引用；保存异常仅提示“观察证据保存失败，模型预览可继续使用”。该行增加中文边界注释，没有改模型/纹理/其他操作。实际scene语法检查exit0、HTTP200回读确认补丁生效；不重复52项，当前单项证据与最终网页SHA分别在 `capture-fix-check.json`、`preview-source-identity.json`。修后保存按钮恢复、统一正背与转停由负责人同页重载继续实际复看。

首轮服务4190/PID2112监听成功，Node Fetch有限检查报 `TypeError: fetch failed / Error: bad port`；根IAB却实际浏览原4190成功，这是观察到的环境差异，不把它改写为“浏览器已经失败”。随后核自有PID/命令身份并停止2112，选空闲4191、Hidden启动84900，检查通过。原stdout及最终stdout/stderr保留。

未运行Minecraft、Gradle、JUnit/GameTest、旧矩阵或模组构建；没有游戏客户端验收、选稿、资源安装、上传网站、Git集成。内部网页观察不关闭Minecraft人工门。等待用户在实际三稿选择后，才能制定正式实施写集。

## R1 三稿精修网页接续

2026-10-14用户要求“能不能更精致一些”，原14A卡追加R1。初版后续负责人已实际完成正背/底部/斜视、剖视、低速/停止、修后capture及重置观察，初版候选同次审查无剩余必改项；以上早先“等待根复看”记录保留原时点。此内部交回没有追认为用户风格通过，R1精修和用户选稿保持独立。

写前已将初版六网页文件原样保留至 `tools/art-assets/centrifuge-concepts-14/preview/history/pre-r1/`，并将当时三latest观察图、browser-evidence、finite-checks、网页SHA、runtime及capture错误/窄修检查共九证据复制到 `build/reports/art/ART-CENTRIFUGE-14A/preview/history/pre-r1/`。六网页归档SHA逐项相同，记录在归档 `web-archive-identity.json`；初版报告内容与结论未改写。

与原资产作者确认R1原四槽外新增 `machined` 与 `fastener`，均是a/b/c目录实际16²SVG。网页按候选textures所有键逐槽解码、在模型中一一绑定和显示缩略，明确机加工钢/实体紧固件中文标签；六缩略为两行，点击仍显示实际原SVG像素近看。原机对照只读取四正式PNG，不扩展假正式槽。标题与简短说明标R1层叠法兰、密封窗框和紧固层次。没有另画材质或伪几何，不把细节压入全机一张钢图。

相机/光照/尺寸、模型坐标换算、剖视、转台、24°/秒网页演示、转停和已修capture保持初版。不新建复杂对照页面，初版截图供负责人同角度比较。`server.mjs`未改、PID84900未重启；原a/*.svg白名单已支持新增来源，当前实际URL仍 [http://127.0.0.1:4191](http://127.0.0.1:4191)。子线程没有重试CUA，负责人同IAB页接续。

R1仅补 `preview/verify-r1.mjs` 核当前候选模型/全部SVG及网页补丁的HTTP字节SHA等于磁盘来源，复用原52项、锁定库及原操作证据，不重跑旧矩阵。作者稳定交回 `revision=r1` 的18SVG/9JSON后，实际命令 `node preview/verify-r1.mjs http://127.0.0.1:4191` **30/30 exit0**，包括manifest、18SVG、9JSON及index/scene两网页补丁；三稿均为实际六槽。前端scene与R1检查脚本语法实际exit0。最终输入身份在本批 `preview/r1/input-checks.json`，七网页源SHA在 `preview/r1/preview-source-identity.json`。壳体元素为A144/155、B152/163、C184/195，三个转子各23；负责人同页重载实际精修观察，网页加载/细节实看和用户选稿仍以负责人实际证据为准，不能以新增SVG存在代替实看。

## R2 转子增粗网页接续

2026-10-14用户要求“转子加大一些吧”，按原卡R2节仅接三份候选rotor真实载荷。R1后续负责人已实看正常三稿、顶/正面/剖视并实际capture成功，28输入/42来源与当前六槽绑定；该内部交回和原同次局部审查保持历史，不冒称用户选稿或正式资源通过。

写前将R1七网页文件原样保留至 `tools/art-assets/centrifuge-concepts-14/preview/history/pre-r2/`，并归档当时三画布、browser-evidence、runtime、R1输入检查及网页SHA和本报告原件至本批证据 `preview/history/pre-r2/`。七网页SHA逐项一致，见 `web-archive-identity.json`。归档实际样本为ready=true、errors=[]、28输入/42来源、正常壳体/停止/零相位默认视角。初版和R1历史内容与结论均未改写。

网页只更新R2修订/增粗描述，并把实际契约revision加入只读/捕获证据。scene仅三处revision相关替换，与R1归档精确比较一致；模型加载1/16换算、上格偏移、转子枢轴、相机/光照/尺寸、动画/剖视/转台/capture及其他控件逐字保持，没有网页group.scale假放大。server也与R1归档逐字一致，4191/PID84900继续原服务，未重启。

新增 `preview/verify-r2.mjs` 仅读取当前三rotor、契约与index/scene必要补丁的HTTP字节身份；不跑六壳/18SVG或原30/52矩阵，也不运行MC/Gradle/Git。作者稳定交回revision=r2后，实际 **6/6 HTTP字节与磁盘载荷SHA同源、exit0**；scene与R2检查脚本语法exit0。有限载荷在 `preview/r2/input-checks.json`，最终八网页源SHA在 `preview/r2/preview-source-identity.json`，仅revision相关scene差异与原缩放/枢轴/相机/光照/动画/capture保持证据在 `preview/r2/patch-scope.json`。

原资产作者实数：23转子元素中1..22横向XZ按1.28增大，主轴element0维持原嵌座配合及原1.3截面/Y5..27；不称23件全部净空。真实360度扫掠外包络直径7.874802→10.079747模型单位（盒状零件扫掠圆包络，非圆柱实物直径），八杆宽.76→.9728、叶片.8→1.024。新增/增大22件最小实体净距三稿均.96模型单位（下横连接件至端座轴向净距），同Y最小径向净距A1.770126/B1.640126/C1.730126，主轴原嵌座接触单独记录。以上引用作者R2实际几何/扫掠证据，网页未额外放大或改壳。

窗内体量、剖视和低速完整转动由负责人同页实际复看；当前入口 [http://127.0.0.1:4191](http://127.0.0.1:4191) 与PID84900保持，子线程未重试CUA，未提前认定浏览器视觉通过。此阶段不安装正式资源或关闭游戏人工门。

## R3 底部独立旋转轴网页接续

2026-10-14用户指出底部动力输入应是旋转轴，原14A卡追加R3。本次只消费实际新增shaft与局部lower，原机对照仍三模型；不修改Minecraft动力或真实停机规则。R2后续负责人已真实查看剖视/低速不同相位及正常窗内，实际capture成功、revision=r2/ready=true/errors=[]，原同次局部审查无剩余必改。该交回保留原时点，不能扩大为用户选稿或正式游戏实现验收。

写前原样归档R2八网页/README及三画布、browser-evidence、runtime、R2输入/网页SHA/范围证据和本报告至各自 `preview/history/pre-r3/`。八网页SHA逐项一致，见证据归档 `web-archive-identity.json`；归档实际样本revision=r2、ready=true、errors=[]、28输入/42来源、正常壳体/停止/零相位默认视角，不改初版/R1/R2历史结论。

与原资产作者约定schemaVersion1/revisionr3，每稿models新增`shaft`指向a/b/c/shaft.json，shaft使用lower-local底面为原点、竖轴x=z=8、现有machined槽。网页按实际models键枚举，不构造任何临时轴几何；shaft与R2rotor共享现有pivot[8,16,8]，两者child仍原1/16换算与lower-local Y偏移，静止/低速/重置始终同相位。lower/upper仍静态壳体，轴座不跟转；剖视只裁静壳，shaft和rotor完整保留。证据新增实际部件parent/scale/position/phase枚举，能直接判断真实shaft是否进入运动枢组。

只修改必要枚举、shaft运动归组和修订说明；原camera/light/尺寸/scale1/16、其他控件/capture保持。server现有a/b/c/*.json白名单已支持shaft，无路由/保存边界修改、未停PID84900或建额外服务；当前仍4191。scene与新R3检查脚本语法实际exit0、实际scene HTTP200回读枚举及同枢组补丁已生效。

作者稳定交回revision=r3后，`preview/verify-r3.mjs` 实际 **9/9同源检查exit0**，仅核三lower/三shaft、契约与index/scene实际网页补丁；scene/R3脚本语法exit0。有限输入在 `preview/r3/input-checks.json`，最终九网页源SHA在 `preview/r3/preview-source-identity.json`。对照R2归档逐段确认相机、光照/WebGL尺寸、控件/动画/capture和server代码逐字一致，证据在 `preview/r3/patch-scope.json`。

实际模型坐标由作者给出：shaft 4×4截面、XZ6..10、lower-local Y0..4、现machined槽；自由露轴侧面Y0..1.25，静台/轴座方孔6.4×6.4，360度实际正净隙.371573模型单位。上端Y3.98..4约.02模型单位与原端板装配接触单列，不称全段净空。lower为A163/B171/C203元素，原底部静态接头及必要台孔改为四角脚/围孔静止座，未使整台底座旋转。原上壳、18SVG及R2rotor24输入字节保持按原资产证据复用。

不重跑MC/Gradle/Git、旧矩阵或全量检查；当前原服务仍 [http://127.0.0.1:4191](http://127.0.0.1:4191)/PID84900。实际底部、正常/剖视及低速不同相位由负责人同页GPU复看，子线程不重试CUA，不以HTTP成功替代实看；正式资源与游戏轴行为未实现，未来游戏接入另定写集。

### R3 同次长轴整改

负责人首轮真实看底部及剖视，确认4×4×4钢轴虽然静动分离、全角间隙和同相位均正确，完整形体仍像正立方钢块；该首轮结构/身份检查不作为用户所需轴形体完成。长轴资产写前，将首轮R3九输入检查、九网页SHA、patch-scope、当前browser-evidence和三latest共七文件原字节归档至 `preview/history/r3-first/`，SHA逐项一致，见 `archive-identity.json`；另保留当时本报告原件。实际归档capture为revision=r3、ready=true、errors=[]、底部低速phase2.6915909218897034、31输入，三稿shaft/rotor同phase、lower/upper phase0。负责人首轮bottom-r3/bottom-moving-r3/cutaway-r3截图独立保留，不覆盖或追认为视觉通过。

按卡补充及负责人明确授权，原资产作者交回revision=r3/refinement=long-shaft：三shaft改为真实4×4×8、XZ6..10、lower-local Y0..8，侧UV4×8、端4×4；三lower仅四原端板拆为八端条、中心局部6.4孔，原Y错层/rotation.origin/angle与表面取样密度保持。作者实核静态全角净距.371573模型单位；不再使用首轮Y3.98..4端板嵌座边界。shaft与rotor0的Y5..8、9/10的Y7..7.65、内斜叶底Y8为同phase轮毂接合，按资产证据独列，不当作静态擦壳或声称全部转子净空。

此整改没有修改scene/index/README/控件/server或任何网页文件，直接由现有动态models加载真实长轴；首轮网页九文件身份、范围和语法证据复用，没有重新跑scene语法、首轮九项或旧矩阵。仅对当前三lower/三shaft/契约 **7/7实际HTTP字节与磁盘SHA同源、exit0**，新输入在 `preview/r3-long/input-checks.json`，记录真实refinement=long-shaft。真实GPU长轴形体、底部/剖视和转动由负责人同页复看；[http://127.0.0.1:4191](http://127.0.0.1:4191)/PID84900原服务保持，最终截图由负责人另命名r3-long保留。没有修改正式游戏资源或运行MC/Gradle/Git。
