# ART-SHIELDED-ASSEMBLY-15B-R1 黑窗窄修

## 最新局部验收（2026-10-14）

- [x] 用户复看修复包后明确“这次是对的了”，本次观察窗黑面及内部机构被遮住的局部视觉门关闭。

详见[局部验收](../ART-SHIELDED-ASSEMBLY-15B-R1-ACCEPTANCE.md)。当前602985f8…包身份保持，原RED/GREEN、同次组合窄审与唯一增量包证据复用；本次只登记文档，无资源/构建/Git/main/世界改动，不推进工序动画。其它未明确项目不补记通过，下方待验文字是验收前历史。

用户实际Minecraft截图显示C版观察窗中间整片发黑、内部看不清，明确要求处理。按[反馈](../ART-SHIELDED-ASSEMBLY-15B-FEEDBACK.md)原位接续故障候选：美术树`codex/art-studio`、HEAD88b8fd2、当前包2,812,280B/a92a0bed0f948fc000b58800cad34f84c03ffb314b167fea3a839f117b12ff7f。原15B网页/自动证据不关闭此真实视觉门。

## 保护与分工

读主目录最新AGENTS、治理1.2/5.1/5.2、本卡与systematic-debugging、minecraft-resource-pack；使用真实客户端原生模型/层接口检查时另读minecraft-modding/testing、按锁定MC1.21.1/NeoForge21.1.219/Create6.0.10-280。root只维护docs/art/只读审查，由原资产作者负责模型导出窄修和必要检查，原预览/包作者负责同源入口与唯一增量包，原审查者同一次组合窄审。

世界/配置/main/其它树/Git只读，不启动用户游戏、不搬档/清缓存或改插件。原C色板/几何/UV/9PNG、3partial、物品display、原15A/14B及日志保持；a92故障包先精确备份。Java/注册/碰撞/光传播/同步/工序动作不在本卡写集，若原生证据要求这些改动先报最小链与必要路径，由负责人处理跨逻辑协调，不用强制满光或删除玻璃掩盖。

## 先定位的实际线索

C window SVG仅透明底与稀疏不透明反光，PNG alpha 0/255。生产parts没有render_type声明；原ShieldedAssemblyClientEvents仅预加载partial/注册BER，没有静态机壳渲染层映射。网页通过自身alphaTest剔除透明像素，不能用它证明游戏层。BER三partial明确cutoutMipped但static shell由方块模型决定，需区分这两条链。原FuelProcessingContent已有noOcclusion，不能猜测成需修改碰撞/注册。

原资产作者首先锁定实际NeoForge BlockModel render-type hint、模型烘焙/默认Iron类型层、solid/cutout_mipped shader对alpha0的处理。保留故障RED原输出或真实绑定结果，核透明面所在part及off/on实际加载，不能只检查JSON某字串当游戏层正确。若证据确认静态层错误，实施仅生产透明面模型render_type声明；若不确认，停假设并报证据，不批量“调亮”。采光仅记录其真实入口，与黑色静态玻璃覆盖分别论证，不捆绑改动。

## 资产作者写集

- 新本批 `build/reports/art/ART-SHIELDED-ASSEMBLY-15B-R1/assets/`；`docs/art/reports/ART-SHIELDED-ASSEMBLY-15B-R1-ASSETS.md`。
- 原15B `produce.py`、assets.json、assets/candidate下被确认含window实际面的part_{0..7}_{off,on}.json及其对应生产part JSON。只对实际含透明面目标加必要render_type；无window模型不改。若实际item原生层也需显式声明，先报告必要路径，再由root补入，不能扩20模型或所有设备。
- 可新增 `tools/art-assets/shielded-assembly-art-15b/verify-render-layer-r1.py`/本批必要检查工具；工具只核本批绑定/来源/alpha链，复用原29像素/几何结果，不镜像测试实现或复制旧全套。
- 只有需要真实MC原生烘焙检查而无现有可复用入口时，报告拟新增专属测试文件/真实调用链，root精确扩卡后写。禁止先写其它Java。

已报告并精确批准唯一离线原生探针 `tools/art-assets/shielded-assembly-art-15b/ShieldedAssemblyRenderLayerR1Probe.java`。实际NeoForge21.1.219的BlockModel/ExtendedBlockModelDeserializer hint→SimpleUnbakedGeometry/NamedRenderTypeManager→SimpleBakedModel.getRenderTypes与默认IRON状态为检查对象；直接javac/java，不进src或JAR，不启动Minecraft/GL窗口或Gradle。class/编译日志/RED/GREEN只写本批R1 assets目录，中文手写注释，sprite构造可复用现有测试模式。16off/on均解析，含window目标须真实cutout_mipped，非window原默认保持；不是仅断言某JSON字段。原生item fallback已锁Sheets.cutoutBlockSheet，暂不扩item metadata。探针写/运行由资产作者唯一持有，其他人复用其原结果，不另造一套检查。

修改前备份本批涉及的原produce.py/assets.json/候选/生产文件，保旧15B身份为历史，不覆盖旧checks/source-identity/日志。确认根因后原导出器跟生产同修，新增R1模式/独立R1证据，避免原install仍要求旧20原模型而覆盖已安装C；安装前匹配a92状态和备份，新增修改只对应实际透明面metadata，色板/几何/UV/alpha/灯映射不动。当前映射29身份保持准确，但不把新source-identity写回原15B冻结结果；R1报告绑定新身份并说明原包不再是当前。

有限受影响RED/GREEN，至少验证真实含透明面模型选择cutout层、PNG alpha仍0/255、色板/几何/UV等原批准C、真实off/on和实际blockstate引用、生产/候选同源；跳过原完整79HTTP、旧设备/工序/服务端功能测试。只有根因要求新客户端路径才扩对应验证。

## 原预览/包作者写集

新 `build/reports/art/ART-SHIELDED-ASSEMBLY-15B-R1/preview/`、`client/`；`docs/art/reports/ART-SHIELDED-ASSEMBLY-15B-R1-PREVIEW.md`、`-CLIENT.md`。必要更新原15B README/preview验证入口、唯一打包/制品核脚本以匹配a92原包和R1证据；不改scene视觉以虚构黑窗修好，不新服务或重启4193/4194。4194原托管session53337/PID16460保留，原三稿/服务说明保持。

跟随新manifest读取实际新候选/生产，只核变化metadata模型和manifest的HTTP身份；原PNG/几何/GUI视图不变，复用原实看，明确网页强制alpha剔除不能复现/关闭游戏黑窗。捕获若需，只绑定新输入，不冒称游戏GPU。

原组合资源审查无必改后，由本作者持有唯一必要增量assemble；先精确备份a92包，原7d/d12及历史保持。本次日志/exit在R1目录，生产/processResources/JAR变化只允许已批准模型metadata；Java/其它JAR载荷应保持。根据实际改动选择必要原生定向检查，禁止全build/旧全部测试/多轮打包。真实失败保留完整证据，不能手工拼JAR或覆盖历史检查结果。

## 同次审查与交付

原审查者仅 `docs/art/reports/ART-SHIELDED-ASSEMBLY-15B-R1-REVIEW.md`，一次组合根因/规格/质量窄审，实际读原生层选择证据/最小修订/原始RED-GREEN与绑定身份；打包后仅同次制品补核，复用不变C/PNG/15B测试。不能把metadata存在、Three预览透明或打包通过称作用户Minecraft故障已解决。

root交新局部候选后，只请用户复看同一台观察窗和内部机构，不重测全设备/配方，不推进加工动画/main。当前反馈只关闭该局部故障时再登记15B游戏门，其它未明确报告项目不补写通过。

## 记录

- [x] 原生层/alpha/采光链根因证据与故障RED。
- [x] 最小确认改动、受影响GREEN和新来源绑定。
- [x] 原同次组合窄审与唯一必要增量包/补核。
- [x] 新局部复看入口，用户实际故障门仍待验。

最终[局部候选](../ART-SHIELDED-ASSEMBLY-15B-R1-CANDIDATE.md)已交：原生RED16/8wrong→GREEN16/0wrong；仅8窗面模型cutout_mipped metadata、37/37变化来源核对与9HTTP同源。原同次组合窄审及制品补核无必改；唯一13.189秒assemble exit0，新2,812,546B/602985f8…包与8映射一致，仅8批准模型改变，其余1947旧载荷含557class保持、0删除，probe未入包。a92备份精确，作者/审查者停写。用户MC同一窗和内部机构局部复看仍待，网页/原生/构建均不关闭此门，不推进动画/main。

原生RED已取得：16个off/on实际ElementsModel完整烘焙及BakedModel.getRenderTypes，part0/1/4/5各off/on的8个含window模型无hint、真实SOLID，incorrectBindings=8、exit1；另8个无window默认SOLID符合限定范围。实际window为247个黑RGB透明像素/alpha0，solid shader无discard/cutout_mipped对alpha<0.5剔除。一次导入名称编译失败和一次空FML清单bootstrap前置失败另保原日志，均不计根因RED；设置探针空mod清单后真实RED单独保native-red结果，不更改游戏环境。下一步仅8个生产/candidate metadata与导出R1模式/manifest同修，待实际GREEN及同次审查/包。
