# 工业泵指示与创造储罐桶操作窄修

2026-10-12，main已验收合入 `d17c18a` 后用户报告：工业泵原Create风格方向指示与实际运输相反，新增青色箭头与实际运输相同；允许去掉原指示或统一方向。用户随后报告冷却液桶不能向Create创造流体储罐标记冷却液。PM选择移除冲突原方向指示、保留实际青色指示，不反转已通过的运输逻辑；创造罐按原Create手持流体容器用法恢复。这是缺陷修复，不增加玩法。

已读当前main、锁定Create6.0.10-280实际JAR/提取源及01R2证据。IndustrialPumpRenderer绘制原COG与新ARROW，未调用原PumpRenderer；旧外观指示须进一步定位具体模型/UV/贴图，不能猜测其来源。Create FluidTankBlock.useItemOn用能力返回对象的CreativeSmartFluidTank类型判断调用setContainedFluid；handlerForCapability被当前Mixin统一包装后，冷液虽允许fill，但该类型分支丢失。原wrapper/JAR失败与只读字节码证据不能冒充玩家动作复现。

## 当前范围与职责

- 在当前主目录既有main窄修，不建分支、不搬动/回写存档；用户未提交模拟器、文档、日志与艺术稿保持。当前若Git仅报LF/CRLF的stat变化，必须核diff及实际SHA，不能借修复格式化或清理。
- PM只写计划/登记、审查与Git。沿用唯一执行者；实现者先报告原指示具体来源和创造罐最小接入方案，再在精确写集内实施，不另派任务或Git。
- 玩家创造罐标记/空桶清除与普通罐桶操作沿用原行为；冷液、热液、水及可用容器分别按实际验证。不能为通过而对外返回未受限原库存，不能让原生管泵/泛用能力绕过热冷却液或SC禁运。若需要新Mixin/入口，先报告具体目标及捕获/清理边界，再由PM精确扩卡。
- 全部数值、outputDirection、拓扑、周转、库存与热工/SU不改；泵原齿轮/机壳及青色方向绘制保持，只移除反向旧指示。若旧指示不可局部分离，先报告证据，不擅自反转泵。

## 唯一执行者初始写集

| 路径 | 允许范围 |
| --- | --- |
| tools/art-assets/industrial-fluid-visual-01/produce.py、verify.py、sources.json、svg/steel_pump.svg | 原指示精确定位后局部SVG/生产/验证；不批量再生管阀 |
| src/main/resources/assets/create_nuclear_industry/textures/block/industrial_fluid_visual_01/steel_pump.png；models/block/industrial_fluid_visual_01/steel/mechanical_pump/block.json、cog.json、item.json | 仅移除原反向方向图案/图形；原几何/齿轮不随意替换，必要新专属资源先报告 |
| src/test/java/com/iksxh/create_nuclear_industry/industrial/IndustrialVisualContractTest.java、IndustrialPipeSurfaceTest.java；新IndustrialTankInteractionTest.java | 实际UV/像素与现青色箭头方向，桶操作/能力边界；不放宽其他断言 |
| src/main/java/com/iksxh/create_nuclear_industry/mixin/CreateTankCapabilityRestrictionMixin.java；industrial/RestrictedFluidHandler.java | 仅桶操作缺陷所需最小接入，外部受限能力合同保持；新调用入口/注册扩卡后才写 |
| 新src/main/java/com/iksxh/create_nuclear_industry/gametest/IndustrialTankInteractionGameTests.java；data/industrial_interaction_probe/structure/empty.nbt | 原真实手持容器交互及禁运代表探针；复制原空模板，不改用户世界/其他域 |
| docs/reviews/2026-10-12/industrial-pump-tank-interaction-fix/IMPLEMENTATION.md、HANDOFF.md；build/reports/industrial-pump-tank-interaction-fix/** | 实际原始日志、来源/包冻结、方案与限制 |

## 验证与收尾

实际使用minecraft-modding、minecraft-testing、minecraft-resource-pack、systematic-debugging，锁定MC1.21.1/Java21/NF21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。先用实际旧输入产生有意义RED，固定后仅受影响JUnit和独立真实桶操作/禁运代表组；禁止旧存档兼容和重复全矩阵。唯一执行者在稳定后一次必要增量assemble并保存旧ecbe0e38…主JAR，不clean、不rerun、不启动客户端。实际沙箱缓存权限失败保留后直接请求require_escalated，用既有用户缓存，不修改构建或系统环境。

一次独立规格/质量窄审仅读原证据，不重复生成/构建；失败或审查发现缺口只整改受影响差异。PM核来源/修改范围/打包并登记，用户只需复看泵一套方向指示与冷却液桶标记/清除；不重新打开已通过整机、管网功能或汽轮机美术门。

## 实际定位与精确扩卡

用户即时澄清所指为泵上的“深色三角形”，不等于完整灰色转动部分。执行者已核原Create block/cog模型及32×32材质，目前未找到独立方向glyph，因此不能假删原贴图或声称原图案已定位；先按实际截图继续核UV/轮廓/阴影。此澄清替代将整个原转动部分当箭头的假设。

创造罐已核当前项目仅注册冷却液桶、没有热液桶；锁定Create对fluid.getBucket()==AIR不能用普通空桶装该工质。此次只恢复实际已有容器的原用法，不增热桶/泛用容器路线，也不为假设容器开能力许可或ThreadLocal。

精确新增写权：`src/main/java/com/iksxh/create_nuclear_industry/mixin/CreateTankPlayerInteractionMixin.java` 与 `src/main/resources/create_nuclear_industry.mixins.json`。只在FluidTankBlock.useItemOn的两处原FluidHelper容器helper调用用WrapOperation接续：真实原helper成功、合法服务端创造罐controller确认后，使用其现有CreativeSmartFluidTank设置原模拟读到的流体/清EMPTY；普通罐、失败与客户端仍走原结果。原桶数量/返还/声音等保留Create调用链，不直接暴露原库存。原CreateTankCapabilityRestrictionMixin与RestrictedFluidHandler保持只读，不实行先前较宽的作用域草案。

**历史诊断假设（后被反证，不是当前授权）：** 初步认为原材质alpha0/黑RGB区域被COG大面UV[0,9,9,14]消费，solid可能形成深色伪面，曾准备将COG缓冲和三包装模型改cutoutMipped。该假设随后被实际采样否定，未实施；下段是实际撤销及最终写集。

**上述cutout假设已被实际采样否定并撤下写权。** 原05/05b日志显示实际COG面内部采样alpha均255；alpha0在未消费atlas空区，不能把透明背景当作该深色孔。执行者尚未实施cutout，因此没有资源/代码回退，不纠正断言制造该假设GREEN。Renderer与三模型继续只读，相关失败是诊断采样假设错误，历史日志保留。

现精确授权采用已核96点的最小外观消歧：COG实际大面UV[0,9,9,14]中，32×32 PNG的x=3..14、y=19..26共96点由RGBA(0,0,8,255)改为相邻同面钢灰#747e86/alpha255。仅修改原steel_pump.svg并单张SVG导出steel_pump.png，其他928像素与全部alpha、其他七材质、几何、青色箭头、旋向/服务端方向保持；不是新箭头或流向变更。produce/verify支持本泵局部重现与验证，sources.json如实记录这处派生补片，禁止全库再生。正确RED需实际烘焙UV映射到这处深色区并证明原输入仍有近黑点，GREEN核精确96变化及其余输入保持；这种图案消歧不冒称独立旧glyph原存在，最终视觉仍待原同处复看。

## 实际交付

11来源窄修及4/4定向JUnit、真实手持3/3自然退出、唯一1秒增量assemble与同一次独立审查完成。最终1829ef1c…包仅5项变化、其余2145条目与原ecbe包保持；原用户261012包/世界与其他dirty不动。原夹具双域与05/05b错误alpha假设分别保留，不计业务RED；正确桶/像素RED和原最终证据见[候选](../../reviews/2026-10-12/industrial-pump-tank-interaction-fix/CANDIDATE.md)。执行及审查写集已释放，只待原两项客户端局部复看，不以自动证据关闭人工门。
