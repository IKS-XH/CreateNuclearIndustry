# EXT-A-FUEL-01C 候选交付

**状态：待客户端验收。** 功能提交`b7377fb2cb07f39ec89d7bf1cbb277bc1e0bad62`，位于`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。main只归档文档与证据，未合入本批功能；没有启动或关闭用户客户端、修改测试世界，也没有推进后续设备。

用户确认各机面直接使用Create原生过滤槽，设备统一不提供独立GUI。离心机改为底部唯一动力，四周及顶部共享原有两罐/两粉槽；空手从过滤框外收粉，护目镜查看状态，面过滤限定材料。原生桶/粉/过滤器均交Create匹配，空过滤不额外筛选。原批次、数值、维护与21格制造不变，01B尾矿4:1保留。

料浆桶采用与冷却剂相同的NeoForge动态流体桶模型，复用原版铁桶外壳，仅桶内显示料浆；旧桶源稿保留但不再被模型引用。机面纹理改为底部轴承、五面中性接口，正面观察窗保留。

## 验证与证据

- [运行交付](./runtime-report.md)：初轮三类关联JUnit共7/7；原始[命令](./runtime/targeted-test-command.txt)、[输出](./runtime/targeted-test-final.log)与`runtime/test-results/` XML已归档。
- [独立审查与定点复审](./review.md)：两项P2——普通右键安装被拦截、中央过滤框受管道遮挡——均已关闭。修正完整物品交互回退链，并把原生槽移到面边角。
- 整改仅复跑受影响的`CentrifugePortsTest`，最终4/4、零失败；见[准确命令](./runtime/review-fixes/targeted-test-command.txt)、[最终输出](./runtime/review-fixes/targeted-test-final.log)及对应XML。首次几何用例度量错误的失败日志也保留，不隐去；初轮7项与局部4项包含重复用例，不相加。
- 整改后[增量assemble](./runtime/review-fixes/assemble.log)成功。JAR已核对新Transform、两份模型，Menu/Screen类已删除。静态资源核对见[资源报告](./assets-report.md)和[JSON证据](./assets/verification.json)。没有clean、全量JUnit、GameTest或游戏运行。

JUnit不带真实Level；Create原生流体匹配会访问世界配方，相关测试边界已在报告中明确。当前自动证据只证明已列举的端口/原生物品过滤/快照/几何及既有桶事务，不证明实际流体管网、列表过滤和客户端视觉交互。真实世界同型BE替换也只有静态保护核对。按治理5.1，不为这些人工项目另搭框架或重复全量。

## 人工门

只使用[当前完整客户端清单](../fuel-01/CLIENT-CHECKLIST.md)，旧菜单与固定左右接口检查已经取代。完全重启同级候选客户端，已有机器动力由后方改接底部；重点检查五面过滤、接管后点选、普通物流安装/扳手、桶外观与数量、拆放保存和原批未确认项目。通过前不合入main。

两条实现线及独立复审均采用高速模型，实际应用`minecraft-modding`、`minecraft-testing`；资源线另用`minecraft-resource-pack`。按锁定Minecraft1.21.1、NeoForge21.1.219、Create6.0.10-280源码核对，未升级依赖。原始执行报告保留历史阶段和限制，归档副本仅调整证据相对路径。
