# 高压锅炉思索03交付入口

**状态：R2修正候选待重新播放。** 用户首次按W打开时的Ponder区域初始化空指针已形成修正候选`92f6d07`，诊断及范围见[原任务卡R2](../../../superpowers/plans/2026-10-08-device-ponder-03-boiler.md)。四幕仍为搭建与分区、接通并运行、蒸汽输出与调压、停机与排查；九种锅炉部件都有入口。已有离心机和反应堆教学保持，本台功能只在同级候选，尚未通过实际播放或合入main。

启动候选并在高压锅炉控制器上按住W，按[唯一播放清单](./PLAYBACK.md)观看四幕：

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
.\gradlew.bat runClient
```

默认镜头使用7×7×7完整锅炉与动态剖视：底部换热、再加热隔层、上下炉腔、可见冷热回路、两汽独立出路和临时压力控件。正文逐条错开，讲解默认尺寸范围、分区容量、现行双汽库存及压力下限规则；未改锅炉热工、配置、配方或其他主线。

实现、技能应用与原始命令见[实施报告及R1](./implementation.md)。[唯一规格/质量审查及同批复核](./review.md)发现五项具体问题，均已关闭；原失败记录保留。初版锅炉3项/P1六项定向检查通过，R1增强实际NBT合同后仅新跑锅炉3项，原P1六项复用。最后补回0～100范围的文案只增量打包，没有机械重测。未运行GameTest、全量测试、clean、旧档兼容或用户客户端。

PM独立读最终构建日志和退出码：`assemble-final`退出0，5秒；R1 XML三项零失败/错误/跳过。最终JAR四模板、双语及四个场景/plugin class共10项与对应资源/编译输出逐字节一致，证据为 `build/reports/extension/DEVICE-PONDER-03-BOILER-R1/artifact-validation-final.json`。最终JAR为同目录 `create_nuclear_industry-0.1.0-R1-final.jar`，2,283,254字节，SHA-256 `A5EA3E8B371FB542CB171B4017392AF58981A2B37CA43347E198AF2FBD33FD3A`。

### R2当前修正候选

删除未显示整炉的初始隐藏；Y=0基础板绕开炉底，使炉底只淡入一次；三处剖面均在15tick显示合并后隐藏。独立复核按锁定Ponder实际调度确认隐藏在第16次tick才执行，并核对其余隐藏点及重播初始化路径。新回归先在故障版本失败，再通过全部锅炉四项；唯一增量assemble退出0、2秒。PM已读取日志/XML和实际差异，封包class与当前编译输出逐字节一致，未改模板、语言或生成器，原资源证据复用。

证据见 `build/reports/extension/DEVICE-PONDER-03-BOILER-R2/`，含原始崩溃、最终合同红/绿日志、XML、构建退出码与 `artifact-validation.json`。当前制品 `create_nuclear_industry-0.1.0-R2.jar` 为2,283,313字节，SHA-256 `9EC12EB5F923697D9D1D24A6140BB169180915C8BA6FF4CC2DA8F6C8F23FA17F`。上方R1封包记录保留原时点含义。

这些仍是静态、编译与制品证据，新时序回归没有驱动真实客户端Ponder调度。实际按W进入、四幕画面及重播/关键帧仍须用户按原清单确认。完成后停在本台播放门；不自动派发下一台教学，主目录继续保留已验收生产实现。
