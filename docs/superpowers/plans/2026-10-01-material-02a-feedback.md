# EXT-A-MATERIAL-02A / EXT-ART-03A / EXT-ART-04：客户端反馈整改

**状态：** 待客户端复测，自动推进暂停。2026-10-01 用户反馈铅锡矿石、粉碎粗矿不能熔炼，且两张板材希望更方正；本卡实现、自动验证和独立审查现已完成，人工门未通过，不合入main。复用 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`、`codex/ore-acquisition`，开工候选 `0d38c99`，本轮功能提交 `7b27a14`；主工程已有 `.vscode/launch.json` 改动保持原样。

## 已确认方向

1. 用户先批准普通矿石、深层矿石、粉碎粗矿各1件产1对应锭，熔炉200ticks/高炉100ticks、经验0.7；随后明确“对锡铅两种矿物的处理完全照搬原版的create矿物处理路线就行”。以后一条为最新准则，核对本工程锁定Create6.0.10-280的实际铅锡链，不凭其他版本或铜铁经验填数值。先前02卡的“粉碎料不得直熔”限制不再作为拒绝实现的依据。
2. 优先接入原生配方与通用标签，不重复制造竞争配方；原生缺少唯一对应时报告具体缺口，再由PM与用户明确，不自行设计洗矿收益。
3. 两张板材已获直接调整授权：近正方形、平直边缘、小倒角，保留可见薄板厚度、左上光照及铅暗锡亮的色彩区分，不再呈扁长圆滑锭形。16×16整数矩形SVG、原色板、透明合同保持；不用生图模型。

版本保持Minecraft1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82。用户对本轮其他人工项尚未逐项确认，不能从本次反馈推断其全部通过。

## A：原生路线核对与功能补齐

高速执行者 `native_lead_tin_audit` 只读锁定Create JAR/源码，逐条提取铅锡粉碎、洗矿、熔炼/高炉与压缩拆解的输入、输出、数量、概率、时间、经验、条件。检查本模组已有注册/标签能否满足原生配方，特别是金属粒身份与条件输出，明确哪些已有、哪些必须补齐，比较上述200/100ticks与0.7是否有差异。

只读审计执行者的唯一写集为候选 `build/reports/extension/EXT-A-MATERIAL-02A-NATIVE-AUDIT.md` 及同名目录，该角色禁止Gradle、客户端、服务端及生产文件写入。审计现已交付；后续实现执行者只按下文PM补充的A实现写集和运行放行门执行。

### 已核实的事实与本批解释

Create铅锡粉碎已由现有标签接通，无需新配方。锁定Create的洗矿、粉碎料熔炼/高炉只提供第三方模组条件兼容配方，输出固定为那些模组的粒/锭；仅补本模组tag不能激活这些输出。粉碎料熔炉200ticks/高炉100ticks、经验0.1；洗矿固定9粒，无额外产物，不声明processing_time。铅锡压板没有可直接使用的无条件自有产物配方。

PM结合用户两次明确要求保留已批准粗矿直熔、补普通/深层矿石直熔（200/100ticks、0.7），粉碎料与洗矿按上述Create兼容数值提供本模组对应产物。Create本体不提供基础金属模组的矿石熔炼，不等于禁止该入口；不能据此撤销用户刚批准的直熔要求。保留既有1锭→1板。新增预留身份 `lead_nugget`（铅粒）、`tin_nugget`（锡粒），9粒↔1锭参照Create锌/原版金属的压缩拆解，不能1粒熔成1锭。不新增磨粉、金属储存块、杆、线或下游设备。

### A实现允许写集

- `src/main/java/com/iksxh/create_nuclear_industry/content/BasicMaterialContent.java`、`content/ModCreativeTabs.java`：只增加两个普通64堆叠粒及创造栏项，更新相关中文职责说明。
- 新建 `src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionNativeMetalProcessingGameTests.java`；必要时仅调整原 `ExtensionBasicMaterialGameTests.java` 中被新合同真正取代的断言，不删仍正确的“某条粗矿配方拒绝其他形态”断言。不得改P1测试。
- `src/main/resources/assets/create_nuclear_industry/models/item/{lead_nugget,tin_nugget}.json`、`lang/{en_us,zh_cn}.json`：只补两粒模型/名称，纹理由独立素材执行者提供。
- `src/main/resources/data/c/tags/item/nuggets.json`、`nuggets/{lead,tin}.json`：`replace:false`，汇总嵌套引用；不把粉碎物混入raw标签。
- `src/main/resources/data/create_nuclear_industry/recipe/smelting/` 和 `blasting/` 各新增 `<metal>_ingot_from_<metal>_ore.json` 与 `<metal>_ingot_from_crushed_raw_<metal>.json`（metal为lead/tin，共8文件）：分别读取 `c:ores/<metal>`、`c:crushed_raw_materials/<metal>`，每份固定输出本模组1锭；参数按上文。
- `recipe/splashing/crushed_raw_{lead,tin}.json`：输入对应粉碎粗矿标签，固定输出本模组9粒，照原生无副产物/无自设时长。
- `recipe/crafting/materials/{lead,tin}_ingot_from_nuggets.json` 与 `{lead,tin}_nugget_from_ingot.json`：读取对应通用标签，9粒合1锭、1锭拆9粒，参照锁定Create锌的JSON形态。
- 报告与临时验证入口仅候选 `build/reports/extension/EXT-A-MATERIAL-02A.md` 及同名目录。正式构建、版本、JEI、旧报告、`run/`、用户存档禁止写入。

### A验证及运行隔离

覆盖普通/深层矿石和粉碎料的铅锡两种熔炉真实tick，检查类型、数量、时间、经验声明；已加载洗矿配方/结果与真实有动力水洗风扇处理铅锡两种粉碎物，覆盖无动力等待后恢复；9粒↔锭检查合成输入、输出、错误金属/不足数量拒绝及往返守恒。测试不能仅比较JSON或直接调用机器加工辅助方法。旧粗矿、压板、铀和粗矿块行为保留，不造额外产量。只对实际改变的范围补有意义回归。

先测试缺配方/缺粒的可执行失败，再补实现。A只可准备代码及报告内验证配置；美术结束后由PM放行Gradle串行窗口。必须复用上轮已修正的run model `gameDirectory`方式，并改为本次02A报告内独立目录，在启动任务前输出/断言模型和任务实际目录，确认无默认run路径后才启动；禁止仅改JavaExec workingDir。旧默认run/world事故现场不清理。

保存命令、实际路径、PID与退出码。GameTest若断言全过但保存挂起，独立记录并只收尾本任务精确进程；框架异常最多一次有依据复测，不无限重跑。最终test/build、资源打包需与最终源码一致；不额外进入用户客户端或存档。第三方金属模组联调不属于本次验收证据。

## B：两张板材方正化 EXT-ART-03A

**允许写集仅：**

- `tools/art-assets/sources/item/{lead_plate,tin_plate}.svg`。
- 对应 `tools/art-assets/generated/item/{lead_plate,tin_plate}.png` 与 `src/main/resources/assets/create_nuclear_industry/textures/item/{lead_plate,tin_plate}.png`。
- 既有导出产生的 `tools/art-assets/preview.{png,html}`、`previews/items-1.png`、`previews/items-2.png`；其他分组预览重新生成须保持原字节。
- 报告及临时验证入口仅 `build/reports/extension/EXT-ART-03A.md` 及同名目录，可保存本次修改前两板副本和前后对照。

不得改正式导出/验证脚本、manifest、palette、baseline、原51张源稿/游戏图、功能/测试代码或文档。不可直接运行会覆盖旧03证据的 `verify.py`；在本次报告目录用临时入口复用现有检查并把输出限定到03A，正式脚本保持原样。禁止覆写02A/03历史报告或基线。

沿现有导出器完成两次导出/安装，检查两板SVG与PNG像素一致、53项游戏路径和51项历史基线不变，原51张游戏PNG（含8冷却剂）逐字节不变，两新图仍16×16、alpha仅0/255、外边框透明、色板不增加。无需新增测试代码或跑GameTest；实际查看原尺寸与放大前后对照再交付。不得进入客户端或运行Gradle，整合打包由PM串行安排。

## C：洗矿所需两种粒素材 EXT-ART-04

新增 `lead_nugget`、`tin_nugget` 的SVG与PNG是洗矿产物身份的必要资源。沿已批准的16×16像素金属风格，使用小金属颗粒形状，体积明显小于锭，区别于粗矿的杂石外观；铅暗冷灰、锡亮银灰，左上光照和全透明外框。不改两张已整改板材的字节。

**允许写集：** `tools/art-assets/sources/item/{lead_nugget,tin_nugget}.svg`、`generated/item/` 下对应两PNG、游戏 `textures/item/` 下对应两PNG；`tools/art-assets/{manifest.json,palette.json,pipeline.py,verify.py,README.md}` 只接入两新路径及相应计数/证据出口；必要预览 `preview.{png,html}`、`previews/`。报告及临时工具仅 `build/reports/extension/EXT-ART-04.md` 和同名目录。不能修改baseline、旧源稿/游戏图、其他模型/语言/注册、正式构建或核心文档。

固定历史白名单及baseline继续51项，明确新增白名单由两板扩为两板+两粒4项；游戏PNG55、manifest含lapis工具候选共56项。新粒不伪造历史基线，默认验证证据出口改为04，旧02A/03/03A报告保持不变。现有非法SVG/路径/映射/preserve拒绝和失败不写保护保留，不为通过新计数删除保护。

开始写正式管线前，须等B方正化完成、PM确认其哈希。先保存当时全部53图哈希，新增两粒后确认既有53图字节不变。两次导出/安装一致，实际查看像素/预览，准备新粒与两板对照供客户端复测。现有verify的临时坏输入检查可在没有其他素材写入时串行执行，必须finally逐字节恢复测试文件，并在报告中记录恢复；禁止遗留对旧SVG/manifest的更改或覆盖旧证据。所有Python加 `-B` 或禁用字节码，避免仓库新增缓存。不得运行Gradle/游戏。

## 共同职责与交付门

执行者必须读主工程最新AGENTS、治理、本卡及02卡，并实际使用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`；B另读 `minecraft-resource-pack/SKILL.md`。必要手写注释用中文，禁止任何Git写、核心文档修改、任务状态变更和再派发。交付未提交改动与指定报告；PM负责审查、Git和文档。

A与素材写集独立，B/C共用导出管线须先B后C，Gradle再串行。按 `dispatching-parallel-agents` 派发，高速模型优先；涉及真实风扇和测试隔离的A沿用有上下文的 `gpt-6-sol/high` 执行者。新增未决玩法暂停，不提前画未批准的其他素材。整合后须提供明确启动候选、当前制品与客户端复测项；未获用户确认不合入新材料批、不推进钢材或设备。

## 本轮执行与独立审查

- B/C均已交付并停止资源写入；PM已查看两板前后对照及粒/锭/粗矿/板对照。两板轮廓改为12×11外接框；C增加两粒后保留开工53图原字节，正式验证器在移开忽略目录中的历史快照后仍可独立运行。B最后仅清理SVG末尾空行，C最后仅修正文档命令和预览页数，未改像素。
- A已获独占Gradle窗口；先用新init确认模型/任务gameDirectory均在02A报告目录，保存默认run与run/saves哈希清单。RED实际发现141测试，新16项均因缺注册/配方失败，随后运行器fastutil异常中断，不能声称形成完整RED汇总；继续补实现与最终验证。
- 独立实现审查复用未参与功能或素材实现的 `native_lead_tin_audit` 高速执行者。该阶段唯一写集为候选 `build/reports/extension/EXT-A-MATERIAL-02A-REVIEW.md` 及同名目录；其余全部只读，禁止Gradle、游戏、素材导出或坏输入试验。先审B/C，再审A最终差异、真实机器证据、资源制品及隔离保护；发现问题交回原执行者，不代写代码。审查结论须区分可交人工复测与可合入main，后者仍受用户人工门约束。

## 当前交付检查点

- A最终265项JUnit与build退出0；141项required GameTest断言全过，包含本卡16项新增检查，保存阶段挂起并在有限等待后只结束精确PID，Gradle退出1。默认run的176文件及run/saves内68文件前后哈希相同，旧事故现场未改动。
- B/C已交付方正两板和两粒，历史51图不变，游戏总55图。PM核对全部240项assets/data与最终JAR一致。独立审查未发现实现阻塞；其旧范围文档意见已由PM标明首轮历史合同及被02A取代的条款。
- 素材提交 `fa94d67`，功能提交 `7b27a14`；所有执行者停止写入。原始证据、制品哈希及过程限制见 [归档索引](../../reviews/2026-10-01/material-02a/README.md)，具体启动及预期见 [客户端复测](../../reviews/2026-10-01/material-02-client.md)。后续只同步治理文档，不改已经验证的功能与资源。
- 未验收人工项保留：两金属各形态熔炼/高炉、真实Create水洗与动力恢复、粒锭合拆、压板、JEI、六物品外观及保存重进。按用户授权在此暂停，不提前合入材料批或推进钢材/设备。
