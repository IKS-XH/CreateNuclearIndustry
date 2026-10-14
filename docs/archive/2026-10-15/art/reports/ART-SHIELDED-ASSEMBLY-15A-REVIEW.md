# ART-SHIELDED-ASSEMBLY-15A 组合窄审

2026-10-14，唯一独立审查者 `shielded15_review`。三稿最终来源稳定、负责人六张GPU截图到齐后，完成同一次规格与质量窄审：**未发现本批静态设计稿的必改项，可交用户复看与选择。** 不推定A已选，不登记正式安装、工序动画或Minecraft客户端验收。

仅写本报告。实际读取主目录最新AGENTS、15A卡、治理1.2/5.1/5.2及 `minecraft-resource-pack/SKILL.md`；应用审查与verification的证据原则，按本卡复用原检查。实际核配置为MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。工作树基线 `88b8fd289d49df90d7e3c655b6e81381ad01130f`，写树仍为同级art-studio。

## 当前来源与证据

- 读取 [资产报告](ART-SHIELDED-ASSEMBLY-15A-ASSETS.md)、[预览报告](ART-SHIELDED-ASSEMBLY-15A-PREVIEW.md) 及各自原始检查；复用资产passed=true/errors=[]、395项保护零变化、预览68/68与52 HTTP输入同源/exit0。审查没有重跑生成、HTTP矩阵或旧设备测试。
- 本轮只读SHA核对：`assets/output-identity.json`的41项及`preview/preview-source-identity.json`的14项与当前文件零差异；原rig、Renderer、Layout与动力合成配方四项权威来源也与契约SHA一致。契约SHA256为 `b666789912cce985a7363e71d7dbb8c81cd294b490f86dc41ac3ab8bb79e41a2`。
- 已读最终 `browser-evidence.json`：ready=true、errors=[]、默认/off/完整机壳、static=true、processingAnimation=false；其45项已加载输入逐项吻合原52项HTTP身份记录。三稿相机一致，实际draw-call为24/24/27；此证据支持真实模型加载，不扩展为游戏烘焙结论。
- 已实际查看 [GPU记录](ART-SHIELDED-ASSEMBLY-15A-GPU/README.md) 的normal、front-on、cutaway、bottom、back、top六张完整页面PNG。使用E盘完整图，没有引用C盘ENOSPC产生的0B文件。

## 规格与画面核对

| 检查点 | 本次结论 |
| :--- | :--- |
| 三稿真实形体 | A厚退台横窗、层叠内收顶肩和重底台；B立柱/前后厚横梁/双窗中央柱及侧筋；C分段侧背护板、凹接缝、独立窗模块与分片顶。正常、背面与俯视均可区分，实际shell为93/101/107元素，差异不限于调色。 |
| 材质与UV | 27张16²有限色SVG、12份真实JSON，铅/钢为主、黄铜限关节锁圈。独立加工钢/紧固件槽与显式UV按实面跨度取样；玻璃透明底与稀疏反光保留内部线索，普通壳体不透明，网页没有以全亮材质或大面积发光补可视度。 |
| 内部层次 | 正面及剖视可辨左右肩轨、杆件、腕座、分开的夹指，以及中央升降座/台面/双钳口。B中央柱确会挡中心正交线，原检查已改用夹具侧表面有限射线；剖视证明形体可读，不将一条射线当全部视角验收。 |
| 尺寸与枢轴 | 四件均为整机0..32，即2×2×2格；库中心补偿和1/16缩放恢复原坐标，partial不加第二次格偏移。相机围整机中心观察不改变主控朝向pivot(8,8)；三件idle包络、原pivot与限定走廊保留。 |
| 原生底轴 | 读取真实Create `AllPartialModels.SHAFT_HALF -> create:block/shaft_half`、原axis/axis_top及Mojang父模型。原字节码的DOWN中心旋转与页面X+90°一致，包络[6,0,6]..[10,8,10]、底接触[8,0,8]；底图保主控位置与通孔，未迁到整机中心16。参考条目SHA `bbd1d6226933a4b29b365a449e87ecff1f851965f82f802c0800747b530a0576`及第三方来源已保留。 |
| 静态灯与流程边界 | on/off仅切换shell灯SVG，三partial不运动、不假装料、不循环加工。九件partial实际面没有lamp槽；肩前小黄点是模型标明的黄铜功能锁圈。负责人已纠正GPU记录的原观察称呼，无需改模型/网页或新增一轮切换测试。 |
| 工具边界 | 真正读取四JSON/各槽SVG并使用现成renderer2.20.0/Three0.180.0；仅本地4193白名单，capture只有固定三图/JSON，原机对照才对原八格加16偏移。未触及14A/14B、src、Git、main、世界/配置或JAR。 |

本轮无Critical/Important必改，无资源或代码整改。原作者局部共面/灯嵌槽修订的首轮、RED和最终证据均保留，未重开历史通过项。审查所见是最终网页静态预览；正式八格拆分、资源安装及实际工序动画须沿用户选稿后另卡实施，用户选择仍为独立门。
