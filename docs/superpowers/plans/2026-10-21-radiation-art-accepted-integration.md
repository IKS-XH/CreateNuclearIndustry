# 防辐射服21B与辐射计20B/20C美术集成

2026-10-21，用户明确要求“把防辐射服和辐射计的美术变更合并到主分支里”。授权合入最新21B及20B→20C完整依赖，不补造未逐项反馈的游戏/多人验收。21B已有用户“四件套手动测试也通过了”记录；20C包含此前批准的纯客户端动态指针。

## 基准与职责

- main `4ad0c91fa22dbbfb3db6fc698aafe55a89d2686b`，正式包17A/3525495bytes。主目录3D工具dirty原样保留；本批不并入事故04A/04B或其它未请求候选。
- 复用`E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration`，PM已创建`codex/radiation-art-accepted-20261021`，tracked干净。main仅Gradle daemon、无游戏进程，世界lock独占开闭已核；原目标世界完整移入`run/save-backups/radiation-art-accepted-20261021/新的世界`，27文件逐SHA核对。随后完整携带main世界27文件/13337721bytes，逐SHA一致且来源未变，清单在本批报告目录。
- PM只管理文档/Git/收件；唯一执行者导入精确写集、必要验证和最终打包，不Git写、改核心文档、启动用户世界或操作其它树。一次集成窄审读取已有证据，不重复原像素/预览/全量业务。
- 必读AGENTS、治理5.1/6、STATUS、本卡、art-studio的21B ACCEPTED/CANDIDATE和20B/20C CANDIDATE/原REVIEW。实际使用本机Minecraft modding/testing/resource-pack技能；制品用ci-release。核实际MC1.21.1/Java21/NF21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，不改构建/版本/许可。

## 固定来源与精确写集

源目录`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/`。ZIP哈希、entry数已由PM读取核对；禁止将整份旧JAR、旧art分支逻辑或其它美术批次覆盖main。

| 来源 | ZIP及SHA256 | 允许目标 |
| --- | --- | --- |
| ART-PROTECTIVE-SUIT-21B/artifact | ART-PROTECTIVE-SUIT-21B-59-source-resource-files.zip；B89F436B6D45A38870C233A2856B207216268DA4EBEBC5EA505FFDCA7519E719 | 全部59entry，56资源及RadiationArmorItem、RadiationContent、RadiationClientRegistration三Java |
| ART-RADIATION-METER-20B/artifact | ART-RADIATION-METER-20B-10-resources.zip；7D4BC7A5F49EA907986B99AD3D8015E0FBE9C4EEFD8C45AB54AFB5CFDE8FC85E | 全部10资源，先保实体/GUI/材质依赖 |
| ART-RADIATION-METER-20C/artifact | ART-RADIATION-METER-20C-9-source-resource-test-files.zip；B2DAF3B3C74D8FE803E822C17755B8C4C0064860445BDDC0DB4EE3C4CDFF9C85 | 全部9entry：4资源、RadiationClientEvents只读getter及3新客户端类、RadiationMeterNeedleMotionTest |

允许写集就是上述不可变ZIP中78个entry的精确并集（70资源、7生产Java、1测试），须在导入前拒绝越界/路径穿越并列出清单。现有Java与各候选闭合基准逐项比较；有真实逻辑冲突交PM决定后在同写集最小合并，不自行拉其它代码。

另允许本树`build/reports/radiation-art-accepted-20261021/`的一份EXECUTION、SOURCE、REVIEW及必要原始输出/构建身份；PM已写world/prior-world清单不得覆盖。普通编译输出允许，跟踪JAR与根日志先备份，变化交PM处理。

## 保持的行为

防护服沿原皮革属性、耐久/修复/配方、ID和Create护目镜，分层仅控制布料颜色及固定部件；饰纹和贫铀公共护甲入口保持。辐射计主副手/F5独立指针只读已有同步field，GUI/掉落/展示框保静态，无服务端/采样/剂量/协议/HUD或声音变化。颜色/饰纹/指针的原模型与像素按批准候选逐字节导入，不重画或顺手调玩法。

## 验证与交付

1. 核各ZIP/entry哈希、原基准和实际导入差异；原静态像素、预览和各次审查/1项装备测试与9项指针测试记录复用。因7个Java首次组合到main，本树一次定向`test --tests '*RadiationEquipmentDataTest' --tests '*RadiationMeterNeedleMotionTest' --console=plain`核编译/API与相关合同；无新业务不启动测试服/全量或用户客户端。
2. 阶段A交SOURCE及简短EXECUTION，唯一组合窄审只核78项来源、必要Java并集/共享护甲与客户端边界、实际证据/未验范围。若无真实问题，不重做原批审查；整改仅受影响项复验。
3. PM提交准确来源后，阶段B唯一一次增量`jar --console=plain`，原始同字节制品及全部class/资源/声明与构建输出绑定，核本批测试未入包及非批载荷保持。不得重封包/再跑同套测试，历史既有测试/probe按原打包规则保持并说明。
4. PM合入main、核同源后提升根跟踪正式JAR并登记SHA；保留来源、用户世界/dirty及旧包证据，不推送发布。本次只记录用户明确的合入授权及21B手测原文，20B/20C细项/多人未反馈不补记。
