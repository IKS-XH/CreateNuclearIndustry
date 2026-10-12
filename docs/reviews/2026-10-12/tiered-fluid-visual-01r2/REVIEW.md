# TIERED-FLOW-VISUAL-01R2 独立规格与质量窄审

结论：在本卡允许写集及下列证据范围内，未发现剩余必改项。可以由主PM登记为四项外观人工复看候选；本报告不是GPU验收、功能重新验收或合main授权。

审查对象为 E:/MyMC/NewMod/Create_NuclearIndustry-fluid-network 的稳定未提交R2，基线e48cac4（原视觉609914f、功能a5d1fe6）。实际读取最新主AGENTS、治理5.1/5.2、01R2卡、FEEDBACK、IMPLEMENTATION与本批src/tools差异；使用minecraft-modding、minecraft-testing、minecraft-resource-pack、requesting-code-review及verification-before-completion。核实际gradle.properties与主目录锁定源码：MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。遵从卡与治理，审查者仅读原日志和冻结制品记录，不重复测试、构建、资源生产或启动客户端；没有Git写、生产/测试写、子代理或世界操作。

## 规格和实现检查

- 两管0/1/单轴两端直线互斥选择原Create fluid_pipe/item；完整Z段按X/Y轴旋转。它与原中央multipart/RIM互斥，避免重复共面面。原item保留8像素管身、10像素2厚法兰和原端面UV；自由端没有cullface，不会再由缺失轴向core或远端内向法兰冒充闭合端面。
- 实际PipeAttachmentModel.addQuads与FluidPipeBlock.shouldDrawCasing确认原Create需要动态壳。新静态条件等价于某轴以外连接数大于2：共面T及4～6向补10像素casing，三轴各一向不补。其余连接采用原central multipart及RIM；casing位于中心，连接附件延伸到格边，未额外塞入全格封闭方壳。64态独立组合预期和真实面包络覆盖这些区别。
- 原casing的#0明确是pipes，core_x/y/z的#0是pipes_connected，item的#1是pipes。producer改为读实际父链；两新增casing包装保留Create几何和UV，只绑定已存在钢灰/陶瓷材质。递归资源合同验证parent、#变量、particle/item直至实际PNG。既有7种源材质调色位图及原创8×8箭头没有像素/alpha改动。
- 箭头只将UV从[0,0,8,8]改成完整[0,0,16,16]，原尖端与杆部均进入实际采样面；没有反转服务器outputDirection。renderer调用与测试同源的orientArrow，再沿原SuperByteBuffer渲染链绘制。CachedBuffers.partial没有隐式FACING变换；实际ShadeSeparatingSuperByteBuffer按输入pose乘局部变换。新测试用真实FaceBakery、BlockFaceUV/块轴旋转及真实PNG定位不透明头/杆，再调用实际buffer变换核四面、六FACING和±RPM。覆盖全部相反FACING，与未改的扳手翻向入口相符；零速及原生超载隐藏保持。
- CPU夹具仅替代GPU atlas装载并提供真实PNG宽高；FaceBakery的16单位换算、UV缩边和面旋转仍是锁定代码。所选原元素rotation均angle=0，测试传null不会改变本次几何。getShape改动逐项匹配管身/法兰/casing；测试包络断言配合源码检查，不把包络等同完整GPU或体素验收。
- Git生产差异只有3个Java及9个资源；PipeBlock除getShape外、原连接/图/事务/流量/禁运/服务器方向、原cog和阀方向未改。原成本/产量、阀窗口、浮窗等断言保留；移除的旧管面合同只照抄原不完整模型列表，已由实际表面合同替代。卡外PM文档、自动日志与既有未跟踪文件不纳入本审查生产写集。

## 证据与制品绑定

实际读取 build/reports/tiered-fluid-visual-01r2/ 的01-red.log、02-red.log/xml、03-free-end-red.log/xml、最终三XML、05-final-green.log、06-svg-validation.log、07-assemble.log、freeze.py、source-manifest.json、jar-verification.json与business-protection.json。

首次编译失败是Flywheel实现类不在静态测试编译classpath，未冒称业务RED；后续真实RED分别捕获孤立黑atlas、中心缺面、glyph截取及T缺壳，补充RED明确捕获mask1外侧自由端。最终XML为Surface 5、Data 2、递归资源1，共8项，零失败/错误/跳过；原日志记录38秒成功。唯一增量assemble原日志记录4秒成功。这些是实现者本批原始运行证据，不是审查者重跑；旧11项静态证据未被宣称覆盖此次GPU故障。

冻结记录绑定18条src/tools差异来源、14个独立JAR文件entry（12变更、2新增），与实现差异及对应原编译输出/资源比较一致；原包全部其他文件entry保持，零删除或卡外制品变化。最终JAR为build/libs/create_nuclear_industry-0.1.0.jar，2,857,489 bytes，SHA256：0c963bbd0726c74b6d7a4ffaab7f686feabe9f67b4c2926605ec574c8e1d1f7b。原包be28ad02d98152532932595912dda375448bb46dce84abd183be0fb56db0d03b及失败/用户日志保留；审查者读并核对冻结记录，没有另生成重复清单。

用户明确通过的功能按FEEDBACK保持，不重开功能/热工/旧版本测试。仍需在原树、原世界复看孤立段、两节相连自由端、T中心及泵正反/扳手翻向箭头；CPU检查不证明实际GPU、光照、图集重载和肉眼辨识通过。四项门未关闭前不合main，不推进其他主线。
