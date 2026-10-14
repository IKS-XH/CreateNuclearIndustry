# ART-TURBINE-07B-R2 独立窄审

结论：规格与代码/资源质量窄审未发现必改项，可交负责人作为游戏试用候选。用户中大型疏线、连接窗及真实光照/资源重载视觉门保持待验。本轮仅审新增三生产类、两专属测试、一PNG与同批来源/证据；HEAD为6042fd2598865d42de88f95c2ba8110c3b7e1196。已读取活动卡含末尾玻璃采样补充、AGENTS、美术职责、治理1.2/5.1及Minecraft modding/testing/resource-pack技能，版本保持MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。

规格核查：CasingAppearanceModel仅对真实上下文合法LOCATED的d5/d7，把07panel sprite映至64×32 atlas；long坐标投影和floorMod保证正负位置及四水平朝向的周期2/3，UV为((phase+u)/4,(row+v)/2)，轴向每相位仍16像素。d3、R1端盖、其他sprite保持。窗口仅接受真实Window、合法MIDDLE四轴心槽，同PIECE/facing/FORMED并沿长轴±1；不扫描结构/owner。连接撤整端实体框/端壁及玻璃端面，延伸长框/玻璃到边，保留外首尾、侧长框、厚度和原色alpha。该规则是既有局部显示状态消费，不声明运行owner权威验证。

玻璃核查：原16PNG非透明bbox=(4,3)-(12,12)，alpha范围0..136。mask非零才按四顶点恒定外向深度.96/.92识别两张大面，采样U4.5/16..11.5/16、V3.5/16..11.5/16；不能单凭getDirection误把共sprite边面一起裁采样。边面/单窗/缺data/未知上下文/ITEM/Ponder降级原quad。生产getQuads只读ModelData，修改前clone顶点，不读世界、不改原quad；模型包装继续委托原RenderType、光照标志与物品入口。

原生接入核查：锁定NeoForge ModelManager.reload:105先同步GeometryLoaderManager.init（RegisterGeometryLoaders），后loadBlockModels/loadBlockStates，之后ModifyBakingResult:225。独立Dist.CLIENT MOD入口在该事件直接幂等注册，没有enqueue或提前DeferredBlock.get。Create6.0.10 ModelSwapper按block注册ID枚举状态包装，与实际TurbineContent的turbine_casing/turbine_window一致；没有额外注册Create listeners。原生blocks atlas的block目录源会包含新增sparse PNG。Catnip1.0.82的SpriteShifter缓存entry，StitchedSprite.onTextureStitchPost每次更新sprite；包装只保存entry、每次取target，不跨重载保存atlas对象。SectionCompiler:80实际将RenderChunkRegion传入getModelData，生产识别该原生类并拒绝未知/Ponder包装。

复用验证：consumer/final-test原XML为8/8、exit0、21秒；最终玻璃UV修改只复测consumer/glass-uv-green窗口4/4、failure/error/skip0、exit0、20秒，壳4复用。夹具读取真实OBJ并使用原生BlockModelRotation，覆盖四水平朝向、四窗面、合法/非法邻接、首尾保留、未知回退、原数组不变及两大面/边面UV。consumer/final-assemble原日志为唯一assemble成功、exit0、1秒。未重跑测试、生成、构建或旧保护矩阵。

制品绑定：本轮只读核五源/测试与freeze条目一致；三生产类及Appearance/Connection两个record共五class与JAR逐字一致；PNG generated→正式安装→build/resources→JAR四路一致，219 bytes、SHA256 aff06b77869abb21ba6939a2300d9ba07c7c06c7684866ddcaf5e3459be4c741。当前build/libs JAR与consumer/ART-TURBINE-07B-R2.jar字节相同，2616697 bytes、SHA256 b1c3c69c29dce947b99ccd0f0a187e9689958217a2f3727c80487d9033cd8d88。consumer/freeze.json含36 owned及12 readonly，SHA256 50e7b1a92d601463e8afe1b219bdce45e2237207da7f2a566febf76f1dddadef；精确源/class路径与SHA见consumer/verification.json。原R1/旧资产保护复用作者已有基线差异证据，未建立新全仓矩阵。

已独立看final-model-preview.png及final-window-preview.png：d3保持，d5/d7重复刻线减少，R1端盖保留；三连窗横框撤下、玻璃跨格，外周框可辨。素材预览读取最终window-mesh-glass-uv.json生产输出及原PNG/MTL，不把它称作完整原生OBJ bake或GPU证明。实际区段裁切、两侧/上下窗口、日夜光照、原存档连接撤销及F3+T后的视觉仍交用户。审查仅新增本报告，已停写；没有生产/Git/游戏/存档写或子委派。
