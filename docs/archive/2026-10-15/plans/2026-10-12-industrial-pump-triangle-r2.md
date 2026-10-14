# 工业泵两端深色三角精准整改R2

2026-10-12，用户提供新的蓝框标注图，明确上一轮仍保留两端三角。基线main `7cae32f`/1829ef1c…包。上一轮COG下半图96点补片没有命中用户所指位置，不登记视觉通过；创造罐桶操作源码与原4/4、真实3/3证据保持，不因此重复测试或关闭其用户操作门。

用户原授权仍是去掉旧方向图案或与实际方向统一，PM继续选择消除深色误导形状、保留实际青色箭头与真实运输。不改流向/转速/压力/能力/几何玩法，不再先猜透明层或让名义像素检查代替截图定位。

## 唯一执行者与来源定位

沿用原唯一执行者，只读分析新的实际截图：`C:/Users/IKSXH/AppData/Local/Temp/codex-clipboard-581ae578-1ec0-4e93-a04b-f7c877ebee0b.png`。蓝框是泵轴线两端靠法兰的深色形状；必须分别明确block/cog哪个面、原UV、原像素区，以及齿轮遮挡/面阴影是否参与。先交真实模型/UV的标注或投影证据与精确改动建议，再授最终补片；不能再次仅依据某个深色矩形推定已匹配用户标注。

不建分支或搬动存档，main当前原位。PM只写文档、核证据和Git；执行者无Git或派发权。PNG从原SVG精确导出，所有代码注释/Javadoc保持中文。实际读取minecraft-modding/testing/resource-pack与systematic-debugging；版本锁定1.21.1/Java21/NF21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。

## 定位阶段写集

仅 `docs/reviews/2026-10-12/industrial-pump-triangle-r2/DIAGNOSIS.md`、`IMPLEMENTATION.md` 和 `build/reports/industrial-pump-triangle-r2/**`，允许生成只读来源的定位预览/UV图与清单；没有生产修改权限。定位后预计只局部修改同一steel_pump.svg/PNG及produce/verify/sources与原IndustrialPipeSurfaceTest；若必须改模型/Renderer，先给真实原因和精确路径。

上一轮错误COG96点补片应在新定位确认后恢复原96点与生成规则，不能夹带无关消歧。桶新Mixin、原cap/RestrictedFluidHandler、青箭头/旋向/几何/流量/SU、其他七材质与已有美术保持；用户261012包、前一标准包和所有世界保留。

## 实际定位后的精确补片授权

只读投影已反证upper中央32点假设：改该区域前后PNG逐字相同，不能把此假设记为根因。实际可见暗点回查位于两端静态法兰侧面，原UV `[8,0,6.5,6]`、rotation270，对应32×32原图的半开像素区域 `[13,0,16,12]`。左右ROI均命中同一侧条纹的深浅轮廓，与COG lower96及upper中央32不交。投影仅说明来源与遮挡，不冒称游戏GPU复现；共面端面也不能仅凭坐标相等登记为z-fighting根因。

据此授权唯一执行者修改以下六路径：

- `tools/art-assets/industrial-fluid-visual-01/svg/steel_pump.svg`
- `src/main/resources/assets/create_nuclear_industry/textures/block/industrial_fluid_visual_01/steel_pump.png`
- `tools/art-assets/industrial-fluid-visual-01/produce.py`
- `tools/art-assets/industrial-fluid-visual-01/verify.py`
- `tools/art-assets/industrial-fluid-visual-01/sources.json`
- `src/test/java/com/iksxh/create_nuclear_industry/industrial/IndustrialPipeSurfaceTest.java`

把上述侧面36点改为同源相邻钢灰RGBA `(116,126,134,255)`，消除方向状轮廓；恢复上一轮COG96点到原调色值 `(0,0,8,255)`。本次相对已验收ecbe源只能留下侧条纹36点差异，其余988点与全部alpha保持；相对1829补片源则应为36点新改和96点恢复。保留原模型/UV/Renderer/箭头/服务端源码，不改其他七张材质。生产前先建立实际两端侧面UV的失败检查；补片后核真实FaceBakery消费、同一投影可见区域的消色效果及完整像素差异，替换错误COG断言与生成规则。若侧条纹预览没有消除已定位轮廓，停止打包并报告，不重新猜其他区域。

定位图、负证据、来源清单、前包备份、新包冻结和日志均允许写在原R2报告目录；不新增长期模拟器或回写用户截图。造罐桶原证据复用，稳定后只做一次必要增量assemble，由PM派同一次独立窄审。

## 验证与交付

先确认截图中两处与实际模型来源一致，再用正确两端面/UV生成有意义RED/GREEN；可对实际未遮挡面投影作离线预览，明确它不是游戏GPU。只复验本批表面与原青色箭头必要代表，不重跑桶组、管网64矩阵、fullbuild或兼容研究。稳定后唯一必要增量assemble与同一次独立差异审查，保留前一错误补片、定位过程和所有旧证据。

最终停在原蓝框两处的单项客户端复看，未报告通过前不关闭此视觉门或推进其他主线；PM以当前候选登记实际预览/制品而不保证GPU已通过。

## 实际交付

已按六路径交付并冻结：两端真实RED各命中目标像素失败；修复后两法兰及原青色箭头共3/3通过。SVG/PNG同源校验、唯一1秒增量assemble及一次独立窄审通过。子工具wrapper启动权限失败单独保留，真实RED/GREEN及assemble由PM在其实际可写缓存环境运行原命令；没有调整缓存或重复构建。

11来源与b0aef730ef1411341d1a334985e5cc103a4d636622c02c27c833b8891bd6e6d8正式/冻结包一致，仅steel_pump.png载荷变化，其余2149文件保持；用户261012、前包和992世界文件保持。交付时停在[候选说明](../reviews/2026-10-12/industrial-pump-triangle-r2/CANDIDATE.md)中的蓝框单项客户端复看，没有以自动证据登记游戏视觉通过。

2026-10-12用户随后明确“OK，没问题了”，按[实际验收](../reviews/2026-10-12/industrial-pump-triangle-r2/ACCEPTANCE.md)关闭本项局部视觉门，R2任务完成。只读核对11来源零漂移与冻结JAR一致，原验证/审查证据复用，不再测试、构建或修改源/素材/包/世界。其他独立人工门与主线保持。
