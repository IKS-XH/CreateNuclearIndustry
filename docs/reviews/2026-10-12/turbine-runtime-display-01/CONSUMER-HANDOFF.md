# ART-TURBINE-10B 实际运行动画消费登记

**2026-10-12验收更新：** 用户已确认汽轮机美术重置测试通过，原模型与运行动画门关闭，按[联合验收](../turbine-fluid-accepted-integration/ACCEPTANCE.md)净合main `151ceba`。下方未提交候选/待GPU观察为原交回时点，不重开已通过门。

2026-10-12，主PM只读核验。美术负责人交回的是原10B授权范围内的动画消费候选；已实际读取该对话本轮直接用户消息“接口已经交回”（turn `01a12fdf-756b-7290-b18d-803278a74e99`），不由Agent回报增加授权、Git权限或用户验收。

## 当前候选

- 美术树：`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，`codex/art-studio`，当前已提交前置基线 `8a33893`；动画与素材继续以未提交候选保留。
- [美术动画候选](../../../../../Create_NuclearIndustry-art-studio/docs/art/ART-TURBINE-10B-ANIMATION-CANDIDATE.md)、[客户端报告](../../../../../Create_NuclearIndustry-art-studio/docs/art/reports/ART-TURBINE-10B-CLIENT.md)、[同轮独立审查](../../../../../Create_NuclearIndustry-art-studio/docs/art/reports/ART-TURBINE-10B-REVIEW.md)是实际消费者记录，不改写其历史阶段。
- 实际API仍是已交 `0351ac3` 的整包 `Optional<TurbineRuntimeSample> runtimeDisplay()`；RPM是前→后局部+Z有符号真实网速，可信0停转、空样本撤销。原消费者按此绑定完整B转子，整机共享累计相位，固定轴承箱座与外圈不动。没有修改供汽、SU、成型或显示发布规则。

## 本次读取的证据

最终 `04-review-green` 原XML为Renderer19/19，零失败、错误、跳过，原退出0；同次审查发现成员替换的Post/BER撤销顺序缺口，新增两例/一失败RED保留，整改后唯一成员替换撤相位、仍有其他可靠成员时保留共享角度，同次独立审查无剩余必改。纯核心6例、前置16/16和真实3/3以及原R1素材证据按同源复用；PM没有重跑测试、审查或构建。

原唯一必要增量assemble日志为1秒exit0，jar实际执行，compileJava/processResources为UP-TO-DATE。原package-identity状态verified、errors为空：六源、26模型/材质、15客户端class、前置11源/13独立JAR项及完整公共ABI一致。PM再次只读核对六源当前SHA、API生产路径无未提交差异、原日志/退出码、当前完整JAR身份；不把离线核验写成GPU通过。

实际美术制品为 `build/libs/create_nuclear_industry-0.1.0.jar`，2,777,331字节，SHA256：

`9be1d3105034c37d27dd8448b3d1d00e2c375462d2f82b0d66a5b579b2e4ca6a`

原冻结副本、旧737d制品备份、首次身份脚本启动异常以及全部RED/中间证据均原位保留。主目录203e9558…JAR（2,705,174字节）和管网0c963bbd…JAR（2,857,489字节）仍分别保持原身份；未复制、覆盖或回写任何世界，未净集成美术动画到main。

## 人工门保持

用户此前报告的三件模型通过保持关闭。本候选仅待真实供汽旋转、归零停角、旋向及各列同相、单人真暂停恢复、成员失效/重载的客户端观察，启动与操作以美术动画候选为准。没有补记用户尚未报告的GPU结果。

管网功能手测已通过，01R2独立只待孤立管段、直管自由端、T节点中心和实际箭头四项外观复看；锅炉16³及其他美术门仍按原独立记录，不借本登记推进管理员、管网压力或兼容研究。
