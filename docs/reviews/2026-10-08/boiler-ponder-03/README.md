# 高压锅炉思索03交付入口

**状态：待客户端集中播放验收。** 四个独立情景为搭建与分区、接通并运行、蒸汽输出与调压、停机与排查。九种锅炉部件都有入口；已有离心机和反应堆教学保持。实现仅在同级候选，功能快照`c113447`，尚未合入main。

启动候选并在高压锅炉控制器上按住W，按[唯一播放清单](./PLAYBACK.md)观看四幕：

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
.\gradlew.bat runClient
```

默认镜头使用7×7×7完整锅炉与动态剖视：底部换热、再加热隔层、上下炉腔、可见冷热回路、两汽独立出路和临时压力控件。正文逐条错开，讲解默认尺寸范围、分区容量、现行双汽库存及压力下限规则；未改锅炉热工、配置、配方或其他主线。

实现、技能应用与原始命令见[实施报告及R1](./implementation.md)。[唯一规格/质量审查及同批复核](./review.md)发现五项具体问题，均已关闭；原失败记录保留。初版锅炉3项/P1六项定向检查通过，R1增强实际NBT合同后仅新跑锅炉3项，原P1六项复用。最后补回0～100范围的文案只增量打包，没有机械重测。未运行GameTest、全量测试、clean、旧档兼容或用户客户端。

PM独立读最终构建日志和退出码：`assemble-final`退出0，5秒；R1 XML三项零失败/错误/跳过。最终JAR四模板、双语及四个场景/plugin class共10项与对应资源/编译输出逐字节一致，证据为 `build/reports/extension/DEVICE-PONDER-03-BOILER-R1/artifact-validation-final.json`。最终JAR为同目录 `create_nuclear_industry-0.1.0-R1-final.jar`，2,283,254字节，SHA-256 `A5EA3E8B371FB542CB171B4017392AF58981A2B37CA43347E198AF2FBD33FD3A`。

这些是静态、编译与制品证据。实际构图、液位/流体动画、原生控件、提示遮挡及回放仍须用户确认。完成后停在本台播放门；不自动派发下一台教学，主目录继续保留已验收生产实现。
