# 锅炉事故与反应堆外爆修复联合合入

用户2026-11-04明确反馈“这两个都手动测试通过了，合并到主分支里吧”，对应锅炉多阀/干烧/超压事故和运行反应堆TNT/苦力怕外爆漏接修复。两份集中手测门关闭，历史候选/审查中的待手测文字保留原时点语境，不覆盖本次反馈。未明确报告的真人多人或物理卸载不补记通过；也不将整体手测改写为自动逐项证据。

## 来源与合入范围

| 项目 | 准确来源 |
| --- | --- |
| main起点 | `8b6d19e50318aa6a0c5ead4748fc0fa530b698d0`；本批派发卡文档提交`5a8beaf9` |
| 反应堆外爆生产源 | `96335648e964f86e2bc55548319c1dc4bb847f10`，原候选HEAD `f8ac63ff7ad1b51ed52098e08e5225ca2d9ca6cd` |
| 锅炉事故生产源 | `7d2a28eb28260c3eb51502b9c9f8217f73d60259`，原候选HEAD `0d40400b062f2297b50240aa4d3c75c3a50a325f` |
| 稳定生产并集 | `16e14ae10735f7fb390e2f834b9b67e987586f45` |
| 集成目录/分支 | `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix` / `codex/boiler-explosion-accepted-20261104` |

同目录切集成分支，按治理保留原位存档；本次不搬动、加载、混合或回写世界。PM先接main文档，再只取锅炉源提交，避免用较旧锅炉候选包充当并集。唯一冲突是`build.gradle`相邻Jar排除规则，由唯一执行者保留两边四条排除；锅炉25项与外爆10项非build源逐项blob保持对应已验提交，生产/测试文件无额外变更，见[SOURCE.json](SOURCE.json)。工作树CRLF/LF与Git规范blob的口径已分开核对，不改模型、材质或玩法。

锅炉新增多阀独立512mB/t/上方遮挡、真实继续收热、干烧损毁、持久超压喷爆及危险拆除直接事故；反应堆新增最终原生爆炸列表、首块loot前接管，保原危险评估、唯一燃料托管、回滚与取消边界。一次[组合窄审](REVIEW.md)确认最终CommonHooks/原生爆炸入口、共享环境移除及同维度动作预算没有新冲突，无阻断项；没有新业务代码整改。

## 验证与正式制品

原锅炉52/52定向JUnit、19场真实服核心和末次1实跑/18显式SKIP；原外爆17/17定向、7/7真实服和末次1实跑/6显式SKIP，均复用其未变来源，未冒称本轮重跑。锅炉R4隔离Nether完整成型交易因实体加载前置不足的`SUBCASE_UNAVAILABLE`仍是历史自动证据限制；用户本轮整体手测通过关闭集中人工门。原全JUnit三条基线失败未改，本次未跑全量test或`build`，不称全量测试全绿。

唯一执行者从已提交生产并集一次增量`assemble --console=plain`：exit0、13秒，2262生产输入前后无漂移，2601载荷绑定全部输出；两批新增测试类/域均排除。PM独立读取日志、审查和制品摘要并复核实包bytes/SHA，见[制品报告](ARTIFACT.md)、[身份](ARTIFACT.json)及[构建命令](assemble-command.json)。

最终正式路径为`build/libs/create_nuclear_industry-0.1.0.jar`，**3714927 bytes，SHA256 `C3A2C178CEDEBD05436DB3E1F3244CF361752AF7B8945C2A1AF154C82057D5C3`**。相对外爆包31项锅炉来源载荷增改、0删除、0模型变化；此前49项模型CRLF/LF差异没有扩大。本次准确源对应正式包纳入Git，不推送或发布。

旧main日期包`create_nuclear_industry-0.1.0_261024.jar`、锅炉日期测试包及原候选分支保持；旧外爆包在集成树报告目录有完整副本。main未提交3D工具及其它无关状态不暂存或改写。最终验收/未闭门只在[STATUS](../../../STATUS.md)维护；本次不自动推进下一功能。

原始执行/构建/源码/逐载荷清单和旧包副本保存在集成树`build/reports/boiler-explosion-accepted-20261104/`。实际使用minecraft-modding/testing/ci-release/resource-pack，技能入口与锁定版本详见[集成卡](../../../superpowers/plans/2026-11-04-boiler-explosion-accepted-integration.md)和审查报告。
