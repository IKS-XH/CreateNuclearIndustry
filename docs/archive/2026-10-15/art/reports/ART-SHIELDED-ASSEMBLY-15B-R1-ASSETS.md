# ART-SHIELDED-ASSEMBLY-15B-R1 黑窗静态层窄修

2026-10-14，原唯一资产执行者 `shielded15_assets` 在美术树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio` / `codex/art-studio` / `88b8fd2` 接续用户真实黑窗反馈。故障基线是2,812,280B / `a92a0bed0f948fc000b58800cad34f84c03ffb314b167fea3a839f117b12ff7f`。本报告已交最小来源修订与原生层RED/GREEN，不把网页、元数据或自动结果称为Minecraft视觉故障已经解决；新包由原预览作者在同次审查后唯一增量产生。

开工读取主目录最新AGENTS、治理1.2/5.1/5.2、R1整卡。实际使用systematic-debugging追踪边界和单变量假设、minecraft-resource-pack核模型/槽、minecraft-modding/testing核锁定原生调用链；RED先于生产修订，完成前读取verification-before-completion/test-driven-development并按本卡精简验证复用。Java手写说明中文，锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280。

## 确认的链与范围

`diagnosis-inputs.json` 绑定实际a92资源、原生第三方来源及PNG：window为16² RGBA，alpha仅0/255，247个透明像素RGB均[0,0,0]，9个不透明反光像素；实际使用`#window`面的part仅0/1/4/5，各有off/on，共8目标。不是“textures字典包含window就全改”，无window面的其余8part保持。

实际锁定源码已只读提取到R1 assets/native-sources，第三方版权原样保留，JAR与entry SHA见diagnosis-inputs：

1. `BlockModel.fromString/fromStream` 调用NeoForge `ExtendedBlockModelDeserializer.INSTANCE`；该反序列化器读取render_type并设到customData的renderTypeHint。
2. `SimpleUnbakedGeometry.bake` 从真实context取hint；无hint使用RenderTypeGroup.EMPTY，有hint经NamedRenderTypeManager取预注册组。`minecraft:cutout_mipped`的block层为RenderType.cutoutMipped，entity默认是无mipmap的ITEM_LAYERED_CUTOUT。
3. `SimpleBakedModel.getRenderTypes` 在无命名层时调用BakedModel默认层；`IBakedModelExtension`委托ItemBlockRenderTypes，其自有方块未映射时默认SOLID。FuelProcessingContent的装配台主控/part沿IRON属性建立并已有noOcclusion；ShieldedAssemblyClientEvents只注册BER/预加载partial，没有静态层映射。
4. `rendertype_solid.fsh`直接把采样色写入fragColor，无alpha discard；solid层默认NO_TRANSPARENCY及COLOR_DEPTH_WRITE。因此透明黑RGBA样本不会被剔除，仍挡住后方机构。`rendertype_cutout_mipped.fsh`明确在color.a<0.5时discard，透明底按原cutout目的露出内部。

本批确认的是静态透明面错误地绑定SOLID这条可复现链。原BER三个partial已经使用cutoutMipped并调用`.light(light)`，其真实世界采光是另一条链；本批没有满光、改光传播或删除玻璃，也没有归因用户渲染插件。物品的原生`RenderTypeHelper.getFallbackItemRenderType`已把非translucent BlockItem回落到Sheets.cutoutBlockSheet，故未申请或修改itemmetadata。

## 真实原生RED/GREEN

负责人在R1卡精确扩入唯一工具Java：`tools/art-assets/shielded-assembly-art-15b/ShieldedAssemblyRenderLayerR1Probe.java`。使用现有客户端classpath，直接javac/java；没有src/test或生产Java、Gradle、Minecraft客户端、GL窗口或用户世界。完整解析16个实际off/on JSON，实际九PNG供原生TextureAtlasSprite，再由ElementsModel原生完整元素烘焙、BakedModel.getRenderTypes查询IRON默认状态；不在工具内重新实现层选择或只读JSON字串断言。

离线探针需空LoadingModList，仅满足锁定NeoForge FeatureFlags原生读取；不扫描mod、不发事件、不改变实际资源。NamedRenderTypeManager只反射调用原生vanilla预注册，保其组映射原样，不运行MOD总线。现有Sprite夹具的受保护构造沿用，本次填实际PNG；所有NativeImage/SpriteContents均关闭。

- 首次探针编译误用`ModelRotation`类名，原失败`native-compile.log/.exit`保留；改为真实`BlockModelRotation`并加-proc:none后编译通过，第二编译原日志保留。
- 初次运行因离线没有LoadingModList而在FeatureFlags bootstrap报错，`native-red-first.log/.exit`保留；补明确空mod清单后才取得有效RED。环境错误不计作黑窗RED。
- 有效RED：`native-red.json` / `native-red-second.log/.exit`，完整16模型中8个含window目标hint=null、actual solid=true、cutoutMipped=false，incorrectBindings=8，exit1；其余8无window模型SOLID正确。
- 同一已编译探针GREEN：`native-green.json` / `native-green.log/.exit`，16模型incorrectBindings=0，exit0；8个透明面目标actual cutoutMipped=true、solid=false、hint=minecraft:cutout_mipped，另8个保持SOLID。原生quad数量不变，未删窗或机构。
- `native-probe-identity.json` 绑定probe源/两个class、classpath、RED/GREEN及实际classpath首匹配：BlockModel/NamedRenderTypeManager均来自锁定neoforge-21.1.219.jar，SHA `2e510bd1cc0c20a02c4723e67a666db02b83a6d79ed05c0136a4ba916fc327c1`。

最终编译入口使用JDK21 `javac -proc:none -encoding UTF-8 -cp <native-classpath.txt内容> -d <R1/assets/classes> <唯一probe.java>`。有效RED/GREEN运行入口相同，仅结果名不同：

```powershell
$r = 'E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-SHIELDED-ASSEMBLY-15B-R1/assets'
$cp = (Get-Content -LiteralPath ($r + '/native-classpath.txt') -Raw).Trim()
& 'C:/Program Files/Java/jdk-21/bin/java.exe' ('-Djava.io.tmpdir=' + $r + '/tmp') ('-Dorg.lwjgl.system.SharedLibraryExtractPath=' + $r + '/native-libs') -cp (($r + '/classes') + ';' + $cp) ShieldedAssemblyRenderLayerR1Probe 'build/reports/art/ART-SHIELDED-ASSEMBLY-15B-R1/assets/native-green.json'
```

临时目录、native库及全部新证据均E盘；没有清缓存或改全局环境。自动结果覆盖原生层绑定，不覆盖GPU/游戏实看。

## 最小修订、安装与保护

修改前 `before-identity.json` / backup按完整仓库相对路径冻结原produce.py、assets.json、29候选/生产、blockstates与相关Java共75项。a92包由预览作者精确备份。原15B历史source-identity/checks/日志不覆盖。

唯一资源变化是以下8个候选及对应生产JSON增加 `render_type: minecraft:cutout_mipped`：

- shielded_assembly_station/parts/part_0_off.json、part_0_on.json
- shielded_assembly_station/parts/part_1_off.json、part_1_on.json
- shielded_assembly_station/parts/part_4_off.json、part_4_on.json
- shielded_assembly_station/parts/part_5_off.json、part_5_on.json

原produce.py的实际八格导出分支也只在真实面使用#window时加该声明。新增`--render-layer-r1`独立模式先核真实RED、a92包和29当前来源/生产SHA，才安装这8metadata；当前R1已应用后禁止旧generate/--install回写历史结果或按旧20模型基线覆盖已安装C。没有重切八格、重导PNG或复制原29检查。

必要旧install防回写烟测已实际执行：`old-install-guard.log/.exit`明确在R1入口拒绝旧--install、exit1；`old-install-guard-check.json`核新R1全部身份仍不变，且实际RED/GREEN的16项quadCount逐项相等。未执行历史安装流程或写旧备份。

实际 `produce.py --render-layer-r1` 输出 `metadata-output.txt` / `metadata-checks.json`：passed=true、errors=[]，8模型metadataOnly=true，29mappings候选/生产SHA全部匹配；18个预期旧路径变化仅8候选+8生产+produce.py+assets.json，其余57项原输入不变。删除新增字段后模型对象与原备份完全相等，C色板/几何/UV/灯映射/9PNG/三个partial/物品display不变；当前blockstate四朝向引用保持原SHA。

assets.json新增renderLayerR1标记，含task、changedTargets8、故障a92 SHA/bytes及层ID；原29mappings继续准确。`R1/assets/source-identity.json`绑定新工具/manifest与29候选/生产，不覆原15B身份。原15B像素/几何/79预览/rig证据按不变来源复用；预览作者只核变化metadata与manifest，网页自身alpha剔除不能复现或关闭原游戏故障。

R1来源/生产ready已交负责人及预览执行者，交原审查者沿同次窄审，所有模型及证据冻结。未操作Git/main/其它树/世界/配置、未启动用户游戏、未写生产Java/注册/光/碰撞/同步/动画，也未由本作者打包。后续只复看同一台观察窗与内部机构，Minecraft局部故障门仍待用户。
