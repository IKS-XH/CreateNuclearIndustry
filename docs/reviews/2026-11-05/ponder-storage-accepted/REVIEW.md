# 思索与干式贮存架19B组合窄审

2026-11-05；审查范围绑定 `codex/ponder-storage-accepted-20261105`、当前基线 `54b35801162ea0a2deb1a4db74c12a2980c211ac`，以及本目录 `source-manifest.json` 的30路径。**技术审查结论：PASS，未发现有证据的阻断项。** 本结论覆盖源集成，尚不声明本轮新JAR构建或载荷核验完成，也不替PM记录最终验收。

已读本树AGENTS、治理5.1/6、STATUS、`docs/superpowers/plans/2026-11-05-ponder-storage-accepted-integration.md`，思索CANDIDATE/ARTIFACT/REVIEW及冻结清单；美术树19B卡、19A批准记录、CANDIDATE、原REVIEW、mapping、原manifest、像素及最终身份记录；本目录INTEGRATION、preflight、integration-verification与核验入口代码。最新用户授权按本卡及本次派发执行：新思索手测通过并合main，贮存架外观一并集成；不由历史候选的待反馈措辞重开人工门，不推定逐视觉细项或真人多人已通过。

实际读取并应用 `C:/Users/IKSXH/.codex/skills/` 下四个入口：`minecraft-modding/SKILL.md`用于状态与客户端边界，`minecraft-testing/SKILL.md`用于既有证据复用及人工验收区分，`minecraft-resource-pack/SKILL.md`用于模型/纹理/物品与状态引用，`minecraft-ci-release/SKILL.md`用于准确来源与制品身份。核实实际Gradle仍为MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，不采用技能的升级、Git或发布示例。

| 窄审项 | 结果与依据 |
| --- | --- |
| ZIP及批准B稿身份 | ZIP为38471 bytes、SHA256 `79D61972936AAA089BFBD6B175D19FE3AA918D26F6E42A7ACAB7A41CBDD0CBF7`，本审只读复核SHA相同。原manifest/mapping逐项绑定15生产资源及14批准B稿，核验入口逐条读取ZIP中央目录、限定显式15路径、拒重复/越界，并核每项字节/SHA与批准源SHA；已有preflight/after结果与该实现一致。未从旧候选JAR或旧美术逻辑取源。 |
| 精确集成与范围 | 本审读取当前30目标文件SHA，与冻结source-manifest逐项0差；15生产＋14可编辑B稿＋原mapping保持原字节。Git状态、未跟踪路径及原前置记录一致：生产新增12、修改3、删除0；全部写集新增27、修改3。三个替换路径是blockstate、item、旧level0，前置证据核原e6a21b7与当前基线同blob、原工作区SHA对应mapping.previousSha256；未吞并后续改动。旧frame/door/occupancy及旧纹理保留。 |
| 路由、模型和物品 | 20个互斥variants完整覆盖四朝向各storage_level=0..4，分别指向一份完整模型，y=0/90/180/270、uvlock缺省false。原证据与after工具核五模型173/226/279/332/385 elements，除正式纹理ID外与批准B模型一致，几何/UV在0..16，八纹理槽及particle可解析，无额外几何父级。item只继承新level0，五组display保持批准稿，完整空架且不重复elements。复用原2048 RGBA像素零差及正式PNG/预览审查，不重新导出或重做大型资产审查。 |
| 状态和思索接口 | 实读DryStorageBlock/BlockEntity/State：仍是单格完整碰撞、四水平朝向及0..4比例外观，MAX_SLOTS=16；visible由used/capacity派生，未把四档当四桶容量。DryStoragePonderScenes仍载入默认16槽客户端快照，以(used+3)/4显示占用；辐射页一桶快照使用level1。模型0..16不越出该单格接口，没有新增容量、碰撞、存取或辐射逻辑。 |
| 已验思索来源和夹带 | 当前25冻结源逐项SHA与原思索清单0差；只读`git diff 78f84c39 HEAD -- src gradle.properties build.gradle settings.gradle`无差异。原源提交`78f84c398add24cae45098422aee4d2baff86b4d`及64D8已验包证据保持；本轮未改Ponder Java/语言/模板/生成器/测试、机器Java、配置、注册、配方、依赖或其它美术。 |

原思索32/32及受影响delta18/18、25模板同源/19旧模板保持、双语187键和64D8包2609载荷证据均沿原EXECUTION/ARTIFACT复用，不表述为本轮重新运行。原19B的像素、几何及预览记录也只作精确同源证据；用户本轮授权不被扩写为逐朝向/逐状态/保存加载或多人操作实测结论。三项既有JUnit失败未改，不称全量build通过。

当前Git `core.autocrlf=true`的警告是后续checkout可能转换工作区换行；Git规范LF blob的相等关系与本次工作区/ZIP原始字节身份分开核对。当前30目标原字节均同源。旧19B的303项换行裁定仅属旧JAR，本轮未授予任何新包额外差异豁免。

可按既定卡进入PM准确源提交后、唯一执行者一次增量assemble及完整载荷身份门。最终新包须相对已验64D8思索包仅新增12、修改3、删除0，15资源原字节匹配原manifest，其余class/语言/思索及其它载荷保持，并核构建前后源及两个事故测试域/类排除；这些属于尚未执行的制品检查，不是本报告已通过结果。

本审只写本文件；仅阅读文件、Git只读状态/差异及必要来源SHA，未运行核验入口、测试、构建、导出、浏览器或游戏，未写生产源、Git、核心文档或世界，无派发及全局状态操作。
