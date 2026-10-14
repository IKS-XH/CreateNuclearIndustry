# EXT-A-ORE-01 执行交付报告

日期：2026-09-29。执行者 production_start_packet；提交PM复核，尚未经过本批客户端人工门。仅本任务隔离工作树 C:/Users/IKSXH/.codex/worktrees/ore-acquisition/Create_NuclearIndustry。基线 adaf347b911d92ed3394a720ada94dff51918ce7；未执行任何Git写操作。

## 实现与批准边界

用户已批准首轮参数：铅(-48..32，三角峰-8，6次/区块、规模7、暴露丢弃0、铁镐)，锡(-16..112，峰48，12次、8、0、石镐)，铀(-64..-16，峰-40，2次、4、0.25、铁镐)。本批全部照此实现；没有采用铀3次/零丢弃备选，没有按采样结果改数值。高度是原生height provider的尝试起点，规模与次数不是产量；原生OreFeature矿团可上下延伸少量方块。

- OreContent.java:19 注册三矿；:25为统一身份记录；:42仅复制原版物理属性，使用普通Block无挖矿经验。共9方块+12物品身份（含9 BlockItem）。入口CreateNuclearIndustry.java:45接线，ModCreativeTabs.java:44展示。
- OreGenerationFilter.java:26 codec读取方块标签/mode；:55实际ServerLevel.dimension严格限定minecraft:overworld，再判disabled/enabled/auto。每次放置读当前绑定标签，不预初始化或缓存；只读取BLOCK注册表，item锭/粉/矿标签不触发。getTag缺失时关闭该次放置。正常worldgen发生在服务端标签已绑定后，实际WorldGenRegion.getLevel返回ServerLevel。filter放在count之前，每矿一条placed feature，不重复普通/深层次数。
- 三个configured_feature使用原生minecraft:ore和stone_ore_replaceables/deepslate_ore_replaceables两个target；BiomeModifier同时限定# minecraft:is_overworld（JSON中无空格）。数据包可覆盖worldgen参数和mode，未新增TOML。
- 六矿石原版风格silk替代分支、raw默认1、fortune ore_drops、explosion_decay；九方块正确工具等级门。粗矿块掉自身1；六条压缩配方通用tag输入、固定本模组输出，9↔1。
- 仅补标签使已有Create九条粉碎配方可达，未新增粉碎配方。实际轮对与电机、真实capability投料、真实实体收集已测试；不是把JSON存在当成机器验证。
- 原矿：保证1粉碎粗矿，额外1为75%，经验1为75%；粗矿：保证1，经验1为75%；粗矿块：保证9，经验count=9且chance=.75，Create ProcessingOutput.rollOutput逐颗roll，最多9；processingDuration=400，未解释为固定秒数。三矿同合同，铀没有副石料。

完整实际源/资源变更列表在 EXT-A-ORE-01/changed-files.txt，关键行索引在source-lines.txt。PNG、P1功能/测试、build脚本、AGENTS、docs均未改。logs/debug.log与logs/latest.log为运行导致的已跟踪日志副作用，按PM要求保留并交PM处理，不纳入功能候选。报告和运行证据在忽略的build目录，PM归档时须显式保存。

## 版本、技能、证据源

实际固定Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，无依赖升级。实际读取 minecraft-modding（DeferredRegister/服务端边界）、minecraft-testing（真实GameTest与JUnit分层）、minecraft-world-generation（configured/placed与BiomeModifier）、minecraft-resource-pack（1.21.1模型/标签单数路径）四份SKILL.md，位于C:/Users/IKSXH/.codex/skills/。另实际读取并应用superpowers的test-driven-development、systematic-debugging与verification-before-completion；技能中的Git/派发步骤未覆盖项目治理禁令。

API/JSON以锁定本地JAR/源码核对：MC/NeoForge sourcesWithNeoForge_751f4ca44c50f024d6929f99f0119fcf429717cc_output.zip；Create create-1.21.1-6.0.10-280-sources.jar、同版slim.jar；Minecraft资源取build/moddev/artifacts/neoforge-21.1.219-client-extra-aka-minecraft-resources.jar。选读源码已保存为证据目录内 *.java.txt，地质JSON与原版替换标签另存Create-*、Vanilla-*。未用技能新版本例子改变技术栈。

## 自动验证

完整命令均在本工作树执行，临时设置JAVA_HOME=C:/Program Files/Java/jdk-21并将其bin加入本次进程PATH；测试无其他执行者并发。

1. `./gradlew.bat test --no-daemon`，test-red.log/exit：264 tests，新增3个合同因真实资源缺失失败，旧261通过，exit1。实现后green1至4均exit0；最终test-final.log/exit是**265 tests、0 failures、0 errors、exit0**（旧261+新4），最终JUnit XML在build/test-results/test，覆盖实际模型引用/纹理、已批准高度/目标/次数、工具/标签/语言/loot算法、压缩守恒。
2. `./gradlew.bat runGameTestServer --no-daemon`：首次gametest-1.log exit1，测试夹具makeMockServerPlayerInLevel走登录时Create发无握手自定义payload而崩溃。完整调用链保留；仅改本批夹具为NeoForge FakePlayerFactory，不登录玩家列表，仍走ServerPlayerGameMode.destroyBlock；未改Create网络或产品逻辑。
3. gametest-2.log显示**116 required passed**：111既有+4新增有效功能测试+1在flat内显式跳过的自然采样。4有效测试分别为工具/掉落/附魔、六配方、真实维度/绑定tag/mode、真实粉碎轮九输入。保存阶段停在Saving worlds，非正常成功退出；仅核对并终止本任务JVM20348，终止记录gametest-2-stop-note.txt与进程命令快照；Gradle最终exit1。所有普通采样服务器均串行在其后运行并正常保存退出。
4. 最终GameTest与build结果见本报告末尾最终核验记录。最终代码比gametest-2多了挖矿无XP断言、普通采样每矿非零断言和最高坐标上下文记录。

ExtensionOreGameTests.java:59 使用真实玩家破坏六矿石，正确/不足镐与错误斧、silk、FortuneIII上下界，粗矿块无时运增殖；:105匹配真实RecipeManager；:126绑定tag与实际Nether维度过滤；:156真实双轮128rpm电机（测试夹具速度）九次实际投料，检查匹配的create recipe ID、400与全部ProcessingOutput精确chance/count；:221之后普通世界8×8采样，0矿种会失败。源码实际行号以source-lines.txt为准。

## 三个固定种子普通世界采样

均为隔离新世界Minecraft默认normal生成器，扫描chunk X/Z=120..127（方块X/Z=1920..2047），外围119..128预生成以完成相邻特征，共扫描64区块/种子、合计192区块；完整Y=-64..319。每个普通服务器保存退出0，证据 runs/seed-20260929、seed-42、seed-314159265 的console.log/result.txt。三矿每种实际至少找到矿块后才记SAMPLE_DONE，flat GameTest不承担这条证据。

| seed | 铅普通 count [Y] | 铅深层 | 锡普通 | 锡深层 | 铀普通 | 铀深层 |
|---|---|---|---|---|---|---|
|20260929|438 [-2,31]|1191 [-50,7]|2528 [-3,63]|150 [-17,7]|0 无范围|249 [-58,-18]|
|42|377 [-5,31]|1051 [-48,7]|1753 [-4,51]|184 [-15,7]|0 无范围|214 [-62,-20]|
|314159265|345 [-13,27]|1296 [-45,10]|2595 [1,103]|252 [-18,49]|0 无范围|241 [-63,-21]|

日志中0计数的minY1000/maxY-1000只是初始哨兵，不是实际高度。普通铀三个样本均未发现，符合铀起点位于深层区域且需要stone目标的约束；不能宣称六形态都自然发现。六形态掉落由真实GameTest另外覆盖。深层锡Y49的定点复核见末尾记录；不能用“深层”物品名代替目标地质标签，也未对高度做额外裁切。

人工可复现坐标，seed=20260929，默认normal新测试世界，同版模组/数据包：普通铅(1935,10,1926)，深层铅(1920,-5,1927)，普通锡(1921,33,1920)，深层锡(1924,-4,1920)，深层铀(1920,-48,1929)。普通铀本样本无坐标，使用创造获取验证该形态贴图/掉落即可，不假造自然发现。启用作弊后可旁观传送附近再切生存开采；不要直接落入实体方块而窒息。

这是有界生成样本，不是采矿时间、长期平均产量、经济平衡或机器吞吐结论。

## 数据包与生命周期

可复制包和具体操作见 EXT-A-ORE-01/packs/README.md，pack_format48。测试包不在src发布资源内。auto-external把minecraft:iron_ore追加至三矿BLOCK tags模拟其他namespace；enabled-external在外部成员存在时强制开启；disabled关闭；item-only只追加同名ITEM tags。

- 默认：三个seed各矿非零。
- auto-external：已绑定external=true、loadedAllows=false，64新区块所有本模组三矿为0；随后禁用包触发重载，external=false、loadedAllows=true。实际证据runs/auto-external/console.log，exit0。
- enabled-external：external=true仍loadedAllows=true，64区块与seed20260929基线计数一致，exit0。
- disabled：external=false仍loadedAllows=false，64区块均0，exit0。
- item-only：external=false/loadedAllows=true，计数与基线一致，exit0。

这是模拟tag兼容验证，不是真实第三方矿模组联调。注册/掉落/既有区块不被去重注销或删除。标签重载实际生效；worldgen codec参数为动态注册表数据，应重启后用于新区块，具体reload/重启对照见最终记录。开启同样不能绕过实际非主世界限制，服务端GameTest对auto/enabled/disabled都用实际Nether ServerLevel验证拒绝（没有宣称穷举所有第三方维度）。

## 人工门与交付边界

客户端未启动、未做视觉/手感验收；候选不是最终验收。PM需整合独立美术候选后提供JAR，再请用户测试：新世界上述坐标的自然矿、旧世界新区块；对应最低镐/不足镐/精准/时运；9粗矿双向压缩；Create真实机器投料；九方块三物品的贴图加载与辨识。旧世界使用备份测试，勿把旧已生成区块当新增矿验证。美术51PNG本执行者未写。

没有实现或确认粒→锭数量、熔炼/洗矿/富集/设备参数，也没有宣布整线成本、吞吐或闭环完成。全部本批可测试候选交PM后停止，人工门之前不进入下批。

## 定点复核和重载边界的最终记录

seed314159265重新生成的隔离世界inspect-314159265，深层锡最高坐标为(1975,49,1928)，上/北/西三邻居都是minecraft:deepslate[axis=y]，下/南/东为深层锡。日志ORE_HIGHEST/ORE_NEIGHBOR可直接核查。原版deepslate_ore_replaceables仅deepslate与tuff（Vanilla-data_minecraft_tags_block_deepslate_ore_replaceables.json）；本批configured目标与该tag一致。故高Y深层锡是在真实高处深板岩区域出现，并非把stone错误映射为深层矿石或测试结构污染（测试结构在0/16附近，采样在1920..2047）。Create配置striated_ores_overworld.json:712..719有在stone目标生成deepslate的层，placed_feature起点-30..70。这给出了同环境中高处深板岩的明确生成途径；没有插桩追踪每个被替换块，因此不宣称逐块追溯到某次Create feature调用。

复采count为铅普通344/深层1297、锡普通2594/深层252、铀普通0/深层241，与初次少数块有1块差异；高度极值与Y49位置稳定。两次完整证据均保留，不作不同运行逐块完全一致的保证，也不据此调参。人工坐标是实际已发现位置，环境/其他数据包/区块生成次序变化可能影响局部结果。

mode-reload-before-restart.log:71..93：disabled启动时false，64新区块三矿0；证据脚本只把隔离世界内disabled测试包的mode改为enabled，运行/reload后依旧false。保存退出0。随后同一测试世界重启，runs/mode-reload/console.log:60..62三矿loadedAllows=true；采样新的chunk160..167（方块2560..2687），铅1544、锡2657、铀238，正常退出0。动态worldgen参数需重启的边界得到实际验证，原测试包packs/disabled仍保持disabled供复制使用。auto外部tag重载即时响应属于另一条已独立验证的运行时标签路径。

## 最终核验记录（文件已稳定）

- 最终 `test --no-daemon`：test-final.log，exit0，265 tests/0 failures/0 errors（261既有+4新增）；junit-extension-final.xml保存4个本批用例，junit-final-summary.txt记录全量XML统计。
- 最终 `runGameTestServer --no-daemon`：gametest-final.log:162..163，116 required全部断言通过，耗时7.810秒。分解仍是111既有+4本批实际功能+1flat采样跳过（:68）；无XP新增断言、ProcessingOutput精确定义断言已包含。19:57:41停在Saving worlds，超过32秒未退出；仅核对并强停本任务JVM23764，gametest-final-stop-note.txt和stopped-process.json记录。Gradle实际exit1、被停JVM exit -1：**断言通过，整个GameTest命令并非正常退出成功**。
- 隔离普通服三seed、四种包以及重载/重启/高处地质复核均exit0；独立console.log/result.txt保留。无需以GameTest保存挂起替代或抹掉普通服生成证据。
- 最终 `build --no-daemon`：build-final.log，BUILD SUCCESSFUL，exit0；test任务复用刚通过的最终XML（UP-TO-DATE），未冒称重复执行了265个测试。功能候选JAR SHA256=2A90BA9582ECC10D36B5A4E68D305C7423A79E30D9A643D4B352C0914E6AFA90；这是未整合美术任务的新功能构建，PM后续整合构建会产生新的哈希。
- 功能范围 `git diff --check -- . ':!logs/debug.log' ':!logs/latest.log'` exit0，证据diff-check-functional.*；全树diff-check仅运行日志自身尾空白，保留交PM，不擅自回退。changed-files.txt列91项，其中89项功能/测试/资源、2项运行日志副作用。无PNG、构建或核心docs修改；报告/证据另在build目录。
- 最后检查无本任务GameTest/普通server JVM遗留。交付后停止写入；由PM复核、Git提交、整合美术及单次整合build，再进入用户客户端人工门。此报告不批准或跳过人工门。
