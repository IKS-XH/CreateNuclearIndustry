# ART-REACTOR-03R1 独立规格＋质量窄审

2026-10-10，独立执行者；唯一工作区为美术树，仅写本报告。未改源码/素材/冻结/治理或Git，未派代理、运行Gradle、重生成、启动客户端/服务或读写用户存档。

## 结论

- **规格符合：通过。** 加粗几何、SVG钢灰细节、控制棒实际位姿采光与合法空气液面采光符合03R1卡及精确补充。
- **内部候选质量：通过。必改项：无。** 未发现 Critical / Important / Minor 必改问题。
- **用户视觉门：仍未通过。** 三截图反馈尚需新候选客户端复看；不把离线材质投影、历史库存或自动断言当作游戏视觉修复确认。03与02R1门独立保留。

## 实际审查与范围

实际读当前AGENTS、治理1.2/5.1/5.2、美术入口、03R1任务卡及实施报告；读取本轮4Java＋1test相对本批baseline的完整差异、生成器尺寸/UV差异、rig、独立OBJ核验脚本/结果、最终核验脚本/映射、原始red/final日志/XML和锁定MC/Catnip审计材料。复用原03已审未改数值/生命周期，不重审L2/CT或旧矩阵。

实际应用此前读过的`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、同根`minecraft-testing/SKILL.md`和`minecraft-resource-pack/SKILL.md`：分别核对客户端BER/SBB采光与坐标边界、真实生产提交链断言、OBJ/UV/SVG/安装资源来源。复用`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/requesting-code-review/SKILL.md`与`verification-before-completion/SKILL.md`的组合审查及证据门，服从治理精简和只读权限。MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6保持。

本轮实际变化为InternalRenderer、ControlRodRenderer、VisualState注释、Materials重载诊断及VisualStateTest；Events/Models/MaterialsTest保持。最终HEAD `d36e79134b822a8eb8c5e058b241674e7050ad94`的PM文档同步与实现写集分开，不将累计Git候选/自动logs算作越界。

## 实现核对

- 燃料九轴4/8/12保持，八边外径3.5、底板0..2/顶板14..16/管2..14；保守管间隙0.5模型单位。辉光与钢管同轴，半径偏置0.0005已烘焙，消费者未二次XZ缩放。控制杆外径5、固定端部径6/厚3，单位杆身与实际body行程保持，AABB端部同步3/16。独立OBJ解析验证真实顶点/环径/Y、闭合边、外法线绕序与UV，未用JUnit镜像模型生成公式。
- 生产BER实际调用`renderPiece`。shaft bottom平移/travel缩放及head top平移在SBB内部；外部cameraPose保持基准，useLevelLight矩阵只含cap世界平移。Catnip字节码确认实际取光为稍内缩模型顶点→内部变换→lightTransform，外部Pose仅用于提交位置/法线。无重复cap/bottom/travel变换，未改actual驱动或设整杆FULL_BRIGHT。
- 生产液体循环实际调用`submitFace`，按合法`Face.cell`调用原生LevelRenderer.getLightColor，每面四顶点使用该光。不会把表面边界floor进外壳、复用opaque仪表宿主light；原alpha、显式RGBA、连续局部UV、overlay/法线和并集拓扑保持，未以补光或加透明度掩盖故障。
- Materials只增每次失败reload的一条资源组/异常诊断，仍返回null并在apply撤旧材质，无render刷日志。库存、冷热ABGR帧混合、纹理槽、暂停/失效回退未改。采光仅客户端原生查询，不新增区块强制加载或共享状态写入口。

## 实际看图与原始证据

实际打开本批`geometry-comparison.png`、`three-module-views.png`、`control-comparison.png`、`material-comparison.png`、`item-display-comparison.png`：同尺度旧/新对照可见管径/板厚增大、三格接头闭合、控制杆固定长度整体移动；钢灰纵槽和分段箍纹可辨，黄铜限端部；静态GUI/手持继承及partial无parent保持。均为真实保存OBJ/UV/SVG投影，未模拟或证明真实世界采光。

所有证据位于`build/reports/art/ART-REACTOR-03R1/`：

- `red.log`与red/两XML：18项中原16通过，新增2项为真实AssertionFailedError，errors/skips为0。液面测试记录生产VertexConsumer最终写入的空气SKY12、白RGBA、原alpha/UV/overlay/法线；杆测试以真实column actual0/.5/1、target.9调用生产rodPose/renderPiece，record SBB边界验证内部/世界/相机矩阵及无固定宿主光。proxy替换的是提交边界，不声称执行了真实GPU；原生Catnip取光语义另由锁定字节码支撑。
- `final.log/.exit.txt`：唯一最终定向test＋jar，19s增量成功、exit0；final/两XML实际**18/18（VisualState11＋Materials7），failure/error/skipped全部0**。本审查未重跑。
- `asset-check.json/.log`及脚本、generate-repeat日志记录独立四OBJ尺寸/同轴/闭合/法线/UV通过，33产物重复生成字节/mtime保持，17SVG不动。初次SVG属性错误及子进程编码错误保留，均未混作行为red。
- `verification.json/.log`与实际verify-final脚本：19源→generated→安装→JAR来源链一致（17文件逐字节、两mcmeta语义与安装/JAR字节）；本轮只4OBJ＋2材质PNG变化，13安装资源保持。相对03B1050/1056assets保留；原1037assets/item、CT3源码、L2R2-12、17SVG、mapping及旧四freeze保持。66原有允许路径22变/44保持，历史原字节另存baseline，不错误宣称已整改作者源仍匹配旧冻结。
- 审查实际核对冻结清单SHA **`4ee58009f19310693e6909208b7ffa8347470b07c2959db6fa0d2c905d18ef7a`**；**67交付＋115证据共182项，以及10项audit依赖，0缺失/0哈希不一致**。包含真实差异baseline、源码、资源、预览、原始XML和候选JAR。
- 候选`candidate/create_nuclear_industry-0.1.0-ART-REACTOR-03R1.jar` **2474232 bytes**、SHA256 **`ec9901a46d95707efdb2c4bc2b63f52fa76a3223f6e115950062eba2f8a44ed1`**与冻结/原始核验一致。

审计的8748＋126mB及合法空气SKY11..13是磁盘历史，只支持宿主0光差异，不代表截图实时库存。常规Catnip绝对世界Matrix4f保留极大坐标float精度限制；本批不扩全局框架或专项测试。客户端透窗排序、白天/夜间真实采光与实时非零库存可见性仍由用户确认，内部审查只支持新候选交接。
