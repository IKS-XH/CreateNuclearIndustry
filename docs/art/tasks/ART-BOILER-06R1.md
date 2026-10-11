# ART-BOILER-06R1：内部汽雾可见度整改

2026-10-11用户反馈“透明度太高了，看不见”，随后明确是“锅炉内部蒸汽太淡”。此反馈直接授权本批内部雾效调整，沿用已批准乳白/青白、实际库存浓度/比例、上部全汽域及慢上行动画，不重开选稿。工作树仅`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，当前HEAD `b8cb60a`；根AGENTS、治理1.2/5.1、原06卡和美术README适用，无Git写。

## 核实与修正

负责人已读实际VisualState/Renderer及原冻结消费报告。原基准alpha为`.035+.145*sqrt(concentration)`，外片权重.7、内片.35，满库存顶点最大仅.126/.063，另乘纹理遮罩及真实气室光。可确认原显示曲线过于克制；实际GPU/照明不以离线图追认。独立原生审查已确认纹理alpha裁剪在乘顶点alpha之前、循环UV为repeat，本批不改这些管线。

唯一实现者为原消费者`reactor_ct_audit`，只改`src/main/java/com/iksxh/create_nuclear_industry/boiler/client/BoilerSteamVisualState.java`的alpha显示曲线及准确中文注释：库存零/非法严格0，正值采用`.18+.50*sqrt(clampedConcentration)`，上限.68。外/内片最大变为.476/.238，约为旧满库存的3.8倍；低22%/高82%仍有单调浓度差别。真实库存/实际容量、混色、14quad几何/权重、时钟、帧循环、采光、RenderType/批次、缓存和公共接口只读，不满亮、不修改玩法。

测试仅允许原`BoilerSteamRendererTest.java`中CPU证据输出路径改到本批独立`build/reports/art/ART-BOILER-06R1/consumer/production-offline-sample.json`，保持全部断言/fixture。可逆视觉参数不新增镜像数值测试或人为RED；复用原06 11/11及完整审查，本批只运行受影响单调性/零库存和现有生产提交/CPU采样两个用例，同时一次增量assemble：

`./gradlew.bat test --tests '*BoilerSteamVisualStateTest.realCapacityAndBothStocksDetermineConcentrationWithoutLiquidLevel' --tests '*BoilerSteamRendererTest.actualSubmissionKeepsLocalPoseAirLightAlphaAndSlowUpwardUv' assemble --console=plain`

报告仅`docs/art/reports/ART-BOILER-06R1-CONSUMER.md`；证据/独立新冻结仅`build/reports/art/ART-BOILER-06R1/consumer/`。先保留原06所有证据、CPU图/JSON、manifest/frozen JAR；本批CPU图用实际新采样与旧06采样对比同一48%配比/22%和82%浓度、同一片权重/遮罩/中性光，SVG证据放新目录，不生成或安装任何游戏素材。源PNG/32SVG/四流体atlas及40资源保持只读。

冻结本批两源及实际相关VisualState编译类与新JAR，原其余客户端/40资源/共享源依赖沿原06 manifest复用，只核必要未变依赖，不另建全仓保护矩阵。最终当前build/libs与新冻结JAR一致；不要覆盖旧06 frozen。原执行者完成即停写，由原独立审查者只读窄审差异与原始结果，报告仅`docs/art/reports/ART-BOILER-06R1-REVIEW.md`；不重跑旧矩阵、素材生成/安装、构建或用户世界。

负责人读取实际日志/XML、生产CPU对比和独立窄审后更新06候选入口，实际观察窗/气室光仍由用户复看。本批不关闭05B或原管罐视觉门，不自动推进其他设备；所有Git/全局治理由主PM或用户管理。

## 内部交付结果

原消费者已完成并停写，[交付报告](../reports/ART-BOILER-06R1-CONSUMER.md)实际为2/2、17秒exit0；负责人完整读取原日志/两XML、逐行核两源差异、核当前源与冻结清单及live/frozen JAR一致，未重跑。R1 JAR为2,587,696字节/SHA256 `dae3321befc092903905ba42234eda769bf53cad3e5d1bc9f3ee22f64d73b245`；清单SHA256 `ea3f40a336fbbbb4e7f202917e3b590c5c9a633f4c58c3703e5e357a9fc53119`。

同一[独立差异窄审](../reports/ART-BOILER-06R1-REVIEW.md)规格/内部质量通过、必改无，负责人完整读取，报告SHA256 `7b2d6e661629d777ac4bc8fa7501e673da2e346f01518133d060e7785a7dc1b7`。实际浏览旧/新生产CPU SVG并保存独立浏览器截图，低/高库存的加浓方向可见；neutralLight=1的离线图不关闭GPU/真实采光门。当前[候选入口](../ART-BOILER-06-CANDIDATE.md)已更新为R1并保留原06历史制品，用户需重启美术树客户端复看内部透窗可见度，F3+T不加载Java变化。
