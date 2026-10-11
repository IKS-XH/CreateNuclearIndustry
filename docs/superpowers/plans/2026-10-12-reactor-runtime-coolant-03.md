# REACTOR-RUNTIME-COOLANT-03：普通库存更新不撤销完整运行样本

状态：需求及根因已核实，安排最小共享修复；反应堆可变尺寸01仍独立停在人工门。本任务不推进其他玩法。

## 来源与合同

用户2026-10-12在已任命美术负责人对话`01a11c6b-f440-77c1-9275-5b2dca5cfa0f`报告反应堆库存变化时液面闪烁，并明确为“整片液体短暂消失再出现”。PM已只读核原用户消息、ART-REACTOR-08卡及真实链：普通EXECUTE fill/drain调用通用setSnapshot，作用域外立即发送Available=false，客户端撤active，下一正式成功tick恢复。主目录与美术树这两份共享源一致；可变尺寸候选仍含同一撤销入口。

这是已有真实库存显示的缺陷修复，不增加玩法或同步字段。只将普通冷液输入/热液输出的纯库存事务区分于外部通用快照改写：

1. 给真实FluidHandler使用专门的服务端库存入口，输入单位mB、输出仍为原权威库存替换及原持久化/同步；不得接收或修改列、燃料、控制、热工、容量或尺寸字段。
2. 只有已有可靠L1和可用完整L2匹配当前owner/generation/geometry及有效结构时，允许暂保留上一正式完成样本。该对象的sample/revision/serverGameTime/columns/库存/热量必须逐字不变；不续租、不中途投影，不拼当前库存与旧热工。下一正式成功结算按原路径完整投影，原五成功tick心跳和20活动tick租约保持。
3. 通用setSnapshot（包括同值外部调用）继续撤销；结构变化、列/控制/燃料变化、提交前失败、异常、坏包、卸载和未知数据继续按原合同撤销。不可用/过期样本不能被库存入口复活。
4. 真实冷热数量、共享容量、每口限流、SIMULATE/EXECUTE、热工及正式提交顺序不改。FluidHandler只替换两处普通库存写入口，其他事务调用者保留原setter。
5. 客户端State、协议/Codec、Factory、Renderer、缓冲、材质与美术源全部只读。窗口内面CT和铅灰SVG归美术08，不夹带于本任务。

## 工作区和技能

- 主目录基线main `8a8f6ad`；新隔离树`E:/MyMC/NewMod/Create_NuclearIndustry-runtime-coolant`，分支`codex/reactor-runtime-coolant`。原生工作树工具没有指定同级绝对路径参数，用户明确要求同级目录，故PM用Git创建指定树。既有其他树有候选或未提交内容，不切换或清理。
- 新树已按用户要求复制主目录run/saves：新的世界27文件、逐一SHA一致，目标原不存在，无覆盖或回写。GameTest仍用独立build世界。
- 实际应用`minecraft-modding`、`minecraft-testing`，流程systematic-debugging、writing-plans、using-git-worktrees、subagent-driven-development、verification-before-completion；执行者另读test-driven-development。锁定1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82。
- PM管理文档/Git，不写代码；执行者禁止Git、用户世界写及子委派。按5.1节仅必要定向验证，不做完整build或旧存档迁移。

## 顺序任务与写集

1. 写一个独立域`ReactorRuntimeCoolantGameTests`，真实成型仪表/端口调用普通冷热capability并解码实际update packet。先RED复现fill/drain撤销，再验证普通事务的权威mB变化而L2完整对象冻结；下一正式成功tick产生同龄新样本并包含真实库存。SIMULATE/零量不变、通用外部setter仍撤销、正式早退/结构失效不复活均须检查。实际NeoForge注册按template namespace过滤，跨域复用无法单独运行本批，因此只将已有`data/reactor_runtime_display_probe/structure/empty.nbt`逐字复制到`data/reactor_runtime_coolant_probe/structure/empty.nbt`，不生成新内容、不改原测试。两次启动排错日志不计为行为RED或通过。
2. 最小修改`blockentity/ReactorInstrumentPortBlockEntity.java`和`reactor/ReactorCoolantFluidHandler.java`；中文职责/单位/服务端/不变量注释。专门库存入口内部保持原持久化和同步，只在可靠条件成立时免撤L2。
3. 定向运行已有`ReactorRuntimeLifecycleTest`、`ReactorRuntimeDescriptorTest`、`ReactorRuntimeProjectionTest`，覆盖重复sample不续租、20tick到期、卸载/坏包/几何拒绝与真暂停原边界；不改其断言来放宽合同。真实独立域只跑本批新用例，记录命令/exit/断言与保存进程结果。唯一最终增量assemble并冻结净源码、class/资源/JAR身份；不clean/rerun。
4. 执行者写`docs/reviews/2026-10-12/reactor-runtime-coolant-03/IMPLEMENTATION.md`及build本批证据。一次独立合并规格/质量审查，读原证据，不重复测试构建。报告不可把自动状态/包检查写成液面视觉通过。

允许代码写集仅上述两个生产Java、新`gametest/ReactorRuntimeCoolantGameTests.java`、原模板逐字复制的`src/main/resources/data/reactor_runtime_coolant_probe/structure/empty.nbt`及必要专属测试（若需要须先报告精确路径）；原NBT只读，禁止新增API依赖、改通用setter其他语义/玩法/注册/配方/美术/用户存档。主目录、可变候选及美术树源只读，跨树同步由PM管理；如Git应用有代码冲突，PM只确定写集，执行者修复。

## 集成和人工边界

自动及独立审查通过后PM提交净共享修复，将精确写集同步main和美术，并核可变候选的同一调用点、保留既有尺寸源与原证据；不将可变尺寸合main。客户端公共签名和L1/L2包格式保持，新增库存入口仅服务端FluidHandler使用。HANDOFF记录实际净提交/API/源与class身份及可安全同步路径，美术08只需在本批最终打包接入，不重跑旧矩阵。

视觉复看并入美术08的库存变化/双面连窗/铅灰玻璃门，不新增单独人工流程，也不关闭可变尺寸01成型/运行/视觉门。所有其他待验收来源和既有美术素材/JAR保持。
