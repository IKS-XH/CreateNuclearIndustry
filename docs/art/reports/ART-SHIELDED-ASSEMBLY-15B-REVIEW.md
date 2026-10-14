# ART-SHIELDED-ASSEMBLY-15B 组合窄审

2026-10-14，原唯一审查者 `shielded15_review` 接续。用户已选C、29资源安装和修后GPU证据固定后，完成同一次规格/质量窄审：**当前正式资源与网页预览没有剩余必改项，可由原预览/包执行者运行本批唯一必要增量assemble。** 目前未执行构建，JAR身份补核待该真实结果；不登记可运行新包或用户Minecraft视觉通过。

审查仅写本报告。实际读取主目录最新AGENTS、15B整卡及服务安全补充、治理1.2/5.1/5.2；沿用已读资源模型/审查/verification技能，补读minecraft-modding/testing，通用重复测试、Git与全支审查步骤受本卡精确边界限制。实际版本仍为MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。复用art-studio/HEAD `88b8fd289d49df90d7e3c655b6e81381ad01130f`。

## 本轮来源及原证据

- 读取 [ASSETS](ART-SHIELDED-ASSEMBLY-15B-ASSETS.md)、[PREVIEW](ART-SHIELDED-ASSEMBLY-15B-PREVIEW.md)、[CLIENT](ART-SHIELDED-ASSEMBLY-15B-CLIENT.md) 和原始candidate/installed检查：passed=true、errors=[]、29安装读回一致、20完整路径原模型备份、106相关保护输入零变。几何和九PNG逐像素/alpha证据按候选身份复用，没有重新生成或复跑。
- 审查侧只读SHA核：`assets/source-identity.json`的65项当前来源/备份/候选与记录零差异，29生产文件全部等于契约SHA。批准C契约仍为 `b666789912cce985a7363e71d7dbb8c81cd294b490f86dc41ac3ab8bb79e41a2`；14冻结源是原字节，不重绘。
- 首轮生产检查79/79、42 HTTP输入同源、exit0保留。后续仅scene物品相机改为+Z，实际源SHA `989e60022e1006a7015494f2ea6a0fc789dc0596e911c6e8942a18dfc2c1eb32` 与窄修语法/HTTP记录及最终browser capture的源身份一致。其余检查复用，未称修后重新跑79项。
- 最终capture为ready=true/errors=[]、production、默认/off/完整壳/static、processingAnimation=false；35项实际加载输入吻合原42 HTTP身份。整机54/物品8 draw-call，物品193元素，相机[0,0,+5]。已实际读 [GPU记录](ART-SHIELDED-ASSEMBLY-15B-GPU/README.md) 的normal/front-on/cutaway/bottom及15a-archive-page完整图；首轮物品错误图只作历史，不作为最终通过图。

## 组合结论

| 范围 | 核对结果 |
| :--- | :--- |
| C正式八格 | 原107外壳元素依part=x+2*z+4*y转为八个局部0..16模型。源程序保留原外面与UV、不增加格界内面；原检查八格按16偏移还原后元素/面/UV与批准C精确相同。现行四朝向blockstate及item父入口保持原引用，不重制expanded=false旧形态。实际PNG整机保C分板、凹缝、双片顶、窗和底台。 |
| 待机机构与坐标 | 三partial为29/29/28元素，逐元素/UV等于C原稿，保持整机0..32和原rig，不叠加第二次格偏移。网页剖视可辨导轨、执行杆、夹指和中央台/双钳口；不增工序动作。朝向/底轴仍归主控(8,8)，相机中心不改变该约定。 |
| 完整物品 | shell+三partial共193元素归一化0..16，旋转原点同缩、UV不缩，原display精确保持；不是方块替代图标。首轮背底观察是网页相机问题，原作者依据真实库makeModelScene(+Z)和锁定MC GUI字节码改默认相机，29生产资源/display未改。修后默认前窗与顶面可见；其它观察按钮是预览视角，不冒称游戏GUI画面。 |
| 材质/alpha/灯 | 九张16² RGBA与选稿SVG原像素零差异；窗alpha仅0/255、钢铅壳alpha255。钢/铅主材和少量功能黄铜保持。on仅part4/5各一真实灯面改绑lamp_on，其它几何/面不变；物品熄灭灯、臂肩黄铜锁圈静态，不把灯当加工进度。 |
| 原生轴及边界 | 整机真实复用15A已核Create SHAFT_HALF/纹理/父模型与DOWN X+90°，包络[6,0,6]..[10,8,10]、底接触[8,0,8]保持。物品没有额外画入Create轴；底图的通孔不等于静态造轴。Java/碰撞/同步/能力/配方/世界/配置/main只读。 |
| 旧预览及服务 | 4193旧进程未停/重启；磁盘server已精确指向20原模型备份，但未宣称运行生效。15A原机按钮disabled/存档说明在实际图可见，原三稿仍可读。两次Start-Process相关自动拒绝原命令保留，不能推断特定拒绝原因。补卡后4194由普通exec托管成功，PID16460/session53337；只localhost/固定资源/capture，不脱离工具管理或绕停旧服务。 |

本轮无Critical/Important必改；已发生的物品默认相机问题由原作者在同批窄修后关闭，没有改正式模型或新增测试套。审查没有生成、安装、HTTP重跑、Gradle/Minecraft、Git写或旧设备复审。下一步只接真实唯一assemble及制品身份，若C盘写入造成失败则保日志/exit和原7d包，不能把当前资源/网页结论写成新JAR通过。

## 同次制品补核

待原包执行者唯一必要assemble结果。本节在真实结果到齐后仅补制品身份，不开第二轮审查或重复构建。用户Minecraft外观/交互验收保持独立，工序动画本批未实现。

### 2026-10-14实际结果追加

上述“尚未构建/待补核”为本次审查前阶段记录；现已读取原作者唯一构建与读回证据，并完成同次制品身份补核，**无剩余必改，可交art-studio客户端候选供用户手测**。本追加没有新一轮审查、生成、测试或构建。

- 实际 `client/assemble-attempt.json` / `assemble.log` / `assemble-result.json`：增量 `./gradlew.bat assemble --no-daemon`，exit0、10.6187349秒；日志明确BUILD SUCCESSFUL，compileJava UP-TO-DATE，processResources/jar各执行一次。临时目录仅本次进程使用E盘，缓存和全局环境保持。构建启动记录C盘Free=16,182,566,912B；此前0空间是准备阶段事实，本批没有据此清理或移缓存。
- 当前JAR实际独立SHA/大小读回为 **2,812,280B / `a92a0bed0f948fc000b58800cad34f84c03ffb314b167fea3a839f117b12ff7f`**，与原构建记录、`client/jar-verification.json`及负责人读回一致。
- 原作者真实JAR比对passed=true/errors=[]，29映射全部通过：候选SHA、production、processResources和JAR相等；变化仅20个模型替换与9个新PNG，removed=[]。1926个旧载荷保持，其中Java类未变；新包classCount=557。审查复用该同源制品比对，没有重新运行verify-jar或扩扫旧设备。
- 原包精确备份实际独立读回 **2,815,098B / `7d62147c84392f00062d19635cd0a5e91300d89860b1e8502cea17079e98f507`**，与冻结原包身份一致；备份路径为 `build/reports/art/ART-SHIELDED-ASSEMBLY-15B/client/backup/create_nuclear_industry-0.1.0-7d62147c.jar`。

本包仅是该树C外观候选，不替换main、不发布或提交Git。当前唯一未关闭的是本批用户Minecraft真实外观/交互复看；不借此追认工序动画、旧存档迁移或其它设备验收。审查至此停写。
