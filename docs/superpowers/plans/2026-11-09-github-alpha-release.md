# 首个GitHub公开Alpha发布

用户2026-11-09表示现在可在GitHub发布成品，授权本轮完成发布。基于现行0.1.0 Alpha及尚未报告的整线/多人观察，按公开预发布处理，不宣称稳定版或补记人工通过。仅发布本项目GitHub，不涉及CurseForge/Modrinth或新CI。

## 来源、范围与职责

- main起点`55b88c42`，正式包0B912E12…95D932；原完整build/871项证据见STATUS。远程旧地址`https://github.com/IKS-XH/MyMC`实际重定向`https://github.com/IKS-XH/CreateNuclearIndustry`；远程main目前为1b8899d5，未有tag或Release，发布前刷新核对。
- 版本仍`0.1.0`，拟用tag`v0.1.0`、标题`Create: Nuclear Industry 0.1.0 Alpha (NeoForge 1.21.1)`，GitHub prerelease=true。不擅自改0.1.0内部版本、版本范围/依赖/许可/首发路线。
- PM负责文档、Git、tag/push与GitHub Release发布；唯一执行者只修发布元数据/构建核验；一次组合审查读已有日志/制品，不重复跑测试。工具或登录缺失先完成可独立准备，遇真正认证阻塞再交用户，不索取密码/token。
- 复用`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix`，PM同目录切`codex/github-alpha-release-20261109`到main，保原位世界。所有主树3D dirty、来源树、旧JAR和存档保持，不启动游戏/搬运世界。
- 实际使用本机`C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-ci-release,minecraft-resource-pack}/SKILL.md`；仍锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。

## 执行者唯一写集

1. 候选`gradle.properties`仅`mod_description`：把早期外壳样例描述改为当前真实用途，与下方模组列表描述一致。
2. 候选`src/main/resources/META-INF/neoforge.mods.toml`仅description正文，同步以下英文内容，不变其它元数据/依赖/版本/许可证：

   `A Create addon for uranium processing, configurable reactors, heat and steam systems, rotational power generation, radiation protection and spent-fuel storage.`

3. 候选`build/reports/github-alpha-release-20261109/`内短报告、清单、必要只读核验工具与正常Gradle输出；源提交后允许正常构建正式路径JAR。不写README/核心文档或主树、不Git写、不向GitHub发内容。

PM同步README过时英文（移除已删除应力闭环及“辐射/事故未实现”）、安装入口和`docs/releases/0.1.0-alpha.md`。不借首发准备改任何玩法、外观、声音、配方或构建脚本。

## 验证与发布顺序

1. 唯一执行者核元数据两项一致及当前MIT/Logo/第三方声明保持，交最小源diff。PM提交准确源后，执行者只运行一次正常`.\gradlew.bat build --console=plain`；此次是明确首发出口且发布元数据已变，允许这轮全量JUnit，不clean/rerun/skip。

   本批源门后采用main现有正常构建入口冻结，避免两树已有物理换行差别造成无意义重包：PM先接入两项准确元数据，再授权原执行者仅在main运行这一轮build及写`build/reports/github-alpha-release-20261109-main/`。主树只允许正常输出/报告，不编辑源码或3D工具；记录前后dirty及两工具SHA。不再另跑候选build，正式包仍必须来自已提交源。
2. 比对冻结0B9旧包与新包，预期仅`META-INF/neoforge.mods.toml`正文差异，生产class/所有其它载荷保持；记录输入前后身份、测试计数/原环境skip、JAR大小/SHA、包内版本/依赖/许可/排除规则与描述。出现其它差异须逐项解释，不泛用历史换行豁免。
3. 一次规格/质量组合审查只读差异、现有build日志/载荷与说明；纯描述且生产逻辑/素材字节不变，复用已验设备及教学，不增加重复手测门。残余独立观察在STATUS和Alpha说明如实保留，不声称发布即关闭。
4. PM接main、只提交审核写集和对应已提交源正式JAR，生成给玩家的JAR及SHA256文本、准备中英发布说明。核远程不分叉后仅推main与明确tag，不推其它分支/dirty、不force。
5. 创建GitHub Release草稿，绑定准确tag与JAR/SHA附件；检查完整说明/附件后将本轮已授权Release公开为prerelease。不要求重复发布确认。下载发布附件回核SHA，确认远端tag/源码和Release均一致后记录真实URL/资产身份。认证/权限阻塞保持草稿与本地完成物，准确报告而不宣称已发布。
6. 唯一执行者和审查者不执行Git写/发布，不扩权。当前同源正常保存/加载已有证据复用，不开展旧存档兼容/迁移研究；实际首发后仍须另行讨论后续兼容策略，不把本卡当作自动实现授权。

结果和未闭门只在STATUS记录。正式公开后完成首个公开Alpha出口，不等于稳定版、所有整线/多人/视觉环境验收通过。
