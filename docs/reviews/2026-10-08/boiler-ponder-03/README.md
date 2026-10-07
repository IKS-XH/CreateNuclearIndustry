# 高压锅炉思索03交付入口

**状态：三幕已验收并合入main。** 用户认可前三幕并要求删除重复第四幕，R3定稿`064fae9`与R1/R2修正已整合main`58a965e`。九种锅炉部件只有搭建与分区、接通并运行、蒸汽输出与调压三个情景；前三幕保持原样。定向检查及唯一main增量打包通过，详见[验收与制品记录](./ACCEPTANCE.md)。原崩溃及R1/R2证据保留历史含义。

从主目录启动，在高压锅炉控制器上按住W即可观看三幕。[原播放清单](./PLAYBACK.md)已关闭，不新增人工测试：

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry'
.\gradlew.bat runClient
```

默认镜头使用7×7×7完整锅炉与动态剖视：底部换热、再加热隔层、上下炉腔、可见冷热回路、两汽独立出路和临时压力控件。正文逐条错开，讲解默认尺寸范围、分区容量、现行双汽库存及压力下限规则；未改锅炉热工、配置、配方或其他主线。

## 历史实现与R1/R2证据

实现、技能应用与原始命令见[实施报告及R1/R2/R3](./implementation.md)。[规格/质量审查及同批复核](./review.md)保留先前问题与修正记录。下方R1/R2制品及待播放表述是当时状态，当前三幕验收和main制品以[验收记录](./ACCEPTANCE.md)为准。

PM独立读最终构建日志和退出码：`assemble-final`退出0，5秒；R1 XML三项零失败/错误/跳过。最终JAR四模板、双语及四个场景/plugin class共10项与对应资源/编译输出逐字节一致，证据为 `build/reports/extension/DEVICE-PONDER-03-BOILER-R1/artifact-validation-final.json`。最终JAR为同目录 `create_nuclear_industry-0.1.0-R1-final.jar`，2,283,254字节，SHA-256 `A5EA3E8B371FB542CB171B4017392AF58981A2B37CA43347E198AF2FBD33FD3A`。

### R2修正候选（历史）

删除未显示整炉的初始隐藏；Y=0基础板绕开炉底，使炉底只淡入一次；三处剖面均在15tick显示合并后隐藏。独立复核按锁定Ponder实际调度确认隐藏在第16次tick才执行，并核对其余隐藏点及重播初始化路径。新回归先在故障版本失败，再通过全部锅炉四项；唯一增量assemble退出0、2秒。PM已读取日志/XML和实际差异，封包class与当前编译输出逐字节一致，未改模板、语言或生成器，原资源证据复用。

证据见 `build/reports/extension/DEVICE-PONDER-03-BOILER-R2/`，含原始崩溃、最终合同红/绿日志、XML、构建退出码与 `artifact-validation.json`。当前制品 `create_nuclear_industry-0.1.0-R2.jar` 为2,283,313字节，SHA-256 `9EC12EB5F923697D9D1D24A6140BB169180915C8BA6FF4CC2DA8F6C8F23FA17F`。上方R1封包记录保留原时点含义。

R2回归没有驱动真实客户端Ponder调度，以上为当时的静态、编译与制品证据。后续用户认可前三幕并撤销第四幕，当前播放门已关闭，不再要求原四幕复测，也不自动派发下一台。
