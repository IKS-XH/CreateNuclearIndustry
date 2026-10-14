# TIERED-FLOW-VISUAL-01R2 实现交付

继续 E:/MyMC/NewMod/Create_NuclearIndustry-fluid-network，codex/tiered-fluid-network-flow，基线e48cac4；原视觉609914f、功能a5d1fe6。仅整改用户四项外观反馈，不重新打开已经手测通过的输送/禁运/库存/阀/浮窗等功能门。本报告为执行者交付，不代替独立审查或GPU人工复看。

实际读取最新主目录AGENTS、治理5.1/5.2、本R2卡，以及minecraft-modding/testing/resource-pack、systematic-debugging、TDD、verification-before-completion；锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，未升级。没有Git写、派子代理、main同步、客户端启动或用户run/saves/run/config写入。

## 四根因与最窄修法

1. **孤立段黑错图**：旧0连接叠core_x/y/z；原core只为连接投影提供部分面，并取pipes_connected的局部atlas占位区域。现在零连接采用原fluid_pipe/item完整Z段（原几何/UV通过parent），只做钢灰/浅陶瓷变量覆盖。
2. **两相连自由端空面**：单轴原multipart缺轴向外侧闭合面，旧测试又可能把另一端内向法兰当作自由端。现在单连接/两端直线按连接轴选原完整静止段，与原中央multipart/RIM互斥，不更改六连接布尔或伪造输送连接；外侧半空间回归明确复现mask1的自由端缺面。
3. **T中心消失**：锁定Create的AllBlocks为普通管安装PipeAttachmentModel；后者还会按FluidPipeBlock.shouldDrawCasing动态补FLUID_PIPE_CASING。独立管静态资源遗漏这个壳。现在仅按原“某轴以外连接数>2”条件补原10像素casing：共面T及4～6向需要，三轴各一向的非共面三向不补壳。casing的0材质按真实原父链取pipes，不被旧目录深度规则强行改成pipes_connected。其余弯头/分支继续原中央面片与RIM。
4. **箭头像反向**：8×8图使用[0,0,8,8]模型UV，只采左上四分之一，实际杆部被截掉；此前名义UP/JOML检查没有验证glyph。现在仅改成完整[0,0,16,16]，原8×8SVG/PNG字节保持。真实FaceBakery定位不透明头部与杆部，经真实ShadeSeparatingSuperByteBuffer/Flywheel TransformStack后，四可见面、六FACING与±256 RPM都与原outputDirection同向。没有取反输出、修改服务器方向或调整其正负规则。原getSpeed零速/超载隐藏保持。

纯客户端提取orientArrow为生产与实际回归共用的SuperByteBuffer入口；原泵cog及阀指针/渲染层没有改。getShape只匹配完整段8像素管身/10像素2厚法兰及原casing包络；连接/放置/扳手/调度逐字保持。新增生产资源只有两种材质的fluid_pipe/casing parent包装；新测试IndustrialPipeSurfaceTest。原producer按同一PNG/SVG确定性生成，退出60件仅键顺序差异，实际管材质位图/alpha无变化。没有原生FluidTransportBehaviour、能力读取、世界扫描、转子API或其他设备改动。

## 实际验证与保留证据

证据根：build/reports/tiered-fluid-visual-01r2/。

- baseline-visual.jar：完整保留原be28ad02d98152532932595912dda375448bb46dce84abd183be0fb56db0d03b视觉包。user-latest.log/user-debug.log保留用户本次19:41退出的实际客户端日志，不改原日志或世界。
- 01-red.log：首次测试编译因unitTest仅含Flywheel API而不能静态访问实现TransformStack类失败；没有计为业务RED。改用运行classpath真实实现的反射入口，不修改构建或依赖。外部GPU atlas仅以SpriteContents尺寸夹具替代；FaceBakery、BlockFaceUV、真实PNG及实际SuperByteBuffer/TransformStack仍调用锁定实现。
- 02-red.log/xml：4项真实失败，16秒。分别为孤立黑atlas、中心空面、真实glyph杆部落在截取UV外、T原壳遗漏。03-free-end-red.log/xml补明确外侧半空间，单项真实复现mask1自由端缺面，16秒。原失败和未成功记录均保留。
- 04-green.log/04-pipe-surface-green.xml：首轮7/7定向，27秒退出0。
- 05-final-green.log及final三XML：最终8/8，38秒自然退出0，零失败/错误/跳过。命令：./gradlew.bat test --tests com.iksxh.create_nuclear_industry.industrial.IndustrialPipeSurfaceTest --tests com.iksxh.create_nuclear_industry.industrial.IndustrialVisualContractTest.everyModelParentFaceParticleAndItemResolvesToRealPng --tests com.iksxh.create_nuclear_industry.industrial.IndustrialDataContractTest。
- SurfaceTest 5项：两管每个64组合实际FaceBakery表面和外侧自由面；不可透明/悬空、无共面重叠；独立预期外包络及当前getShape匹配；按实际面尺寸核绑定pipes或pipes_connected的真实PNG像素、UV不越界；孤立完整段和T原壳专项；实际扫描不透明glyph头/杆位置、六向±RPM的四面真绘制链、零速/超载隐藏。原不完整“复制原模型列表作预期”方法已由这些实际表面合同替代。
- 资源递归1项仍解析parent/#变量/item/particle至实际Create/Minecraft PNG，原成本/产量与multipart合法语义2项通过；不重跑已验阀、图标或任何服务器功能矩阵。旧相关断言保留在源码，并未因本卡删掉阀/图标合同。
- 06-svg-validation.log：8张既有SVG/PNG像素一致、原alpha保留；最终JAR逐条比原be28证明所有既有纹理PNG不变。相同资源JSON只有保持原键顺序的排版调整后，复用同一语义定向结果，不机械重复测试。
- 07-assemble.log：本批唯一必要增量assemble，4秒自然退出0；compileJava增量复用。没有clean、完整build、GameTest、热工/兼容测试或GPU客户端启动。

本批diff --check通过；首次工具说明EOF空行发现后只修该说明并更新源hash，未再打包。保留自动logs与PM的CANDIDATE/CLIENT-CHECKLIST/.superpowers等独立差异，不归生产源。原生API/Gradle弃用及既有unchecked提示如日志保留，未扩展升级范围。

## 最终差异冻结与保护

- source-manifest.json：本批18条精确差异src/tools来源，每件SHA256/大小；3生产Java、9资源、3测试、3独立工具/说明。报告单列不自引用。
- jar-verification.json：14个独立class/资源entry与编译输出/来源逐字一致。旧包全部实际文件entry比较只有本卡12变更+2新增，零删除/零其他差异；无错误把ZIP目录entry当文件。
- business-protection.json：shape以外PipeBlock源码逐字一致，服务器出液规则和速度读取、既有阀方向、原cog绘制源码保持；所有其他JAR文件保持，所有既有专项PNG保持。没有世界复制、修改或回写。
- 最终JAR：build/libs/create_nuclear_industry-0.1.0.jar，**2,857,489 bytes**，SHA256 **0c963bbd0726c74b6d7a4ffaab7f686feabe9f67b4c2926605ec574c8e1d1f7b**。

原543/543、工业20/20及此前独立审查证据保留复用，用户已通过功能不重跑。当前只待同一次独立窄审及四项局部GPU复看：孤立段、两相连自由端、T中心、正反/翻向箭头。自动烘焙/像素结果不宣称用户视觉通过，未自行验收、合main或推进其他主线。
