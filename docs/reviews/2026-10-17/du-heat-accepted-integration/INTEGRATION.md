# 贫化铀与锅炉队列联合集成记录

2026-10-17，PM按用户[三项联合验收及合入授权](ACCEPTANCE.md)执行。对应[原执行与集成卡](../../../archive/2026-10-17/plans/2026-10-17-du-heat-accepted-integration.md)归档，当前结果及正式包以[STATUS](../../../STATUS.md)为准。

## 来源与范围

主目录基准`90030bd210c3a9b3f96b90f290ee9ae94e283ef6`存在48项tracked状态dirty，包含正式JAR的工作副本删除；前件已冻结。使用同级干净detached集成树`Create_NuclearIndustry-du-accepted-integration`，完整携带已保存退出的main世界27文件/13337924 bytes，路径、数量及SHA零差异后未启动、扫描或回写。

先快进到锅炉队列已验分支`02b581e`，保留实际生产来源`8712f4310eb1ff6ac8f2af03fde33e4154a71325`；再合贫化铀功能`0a10e361cee9a8069f8bd0b2a711f2cb60349797`形成`e8be20aaa4a9e5d064eb412d68f61e2b6b82db19`。仅README、STATUS、路线图三份核心文档冲突，PM核对后保留main最新内容，没有代码冲突。之后逐字安装已验18B集成ZIP的67路径，单独提交`af79645f3091831039933ac56df4cade7eafa8b6`。

阶段A确认安装前全部生产/构建Git对象正好等于main叠加两份已验净增量；安装只改67项，非manifest生产变化0。ZIP31671 bytes、SHA F1580CB2…；已验美术包3389257 bytes、SHA04466D8A…；完整身份见[ART-SOURCE.json](ART-SOURCE.json)。Git提交将51JSON和一个公共Java钩子的CRLF规范为LF，逐项证明仅行尾差异、JSON值相同；15PNG保持原字节。没有修改设计或重新生成外观。

## 准确来源打包

唯一执行者从已提交生产来源`af79645f3091831039933ac56df4cade7eafa8b6`导出准确Git原字节：2228输入、11586450 bytes，逐项核Git blob；原树与导出输入构建前后均未变。一次增量assemble，exit0、16秒、4任务执行，没有clean、强制重跑、全build、JUnit/GameTest或游戏。36项既有EventBusSubscriber removal及deprecated/unchecked提示保留，没有按提示改依赖或扩大任务。

原assemble同字节交付包3382155 bytes，SHA256 `9C556236051AB1304BBFEBAC79AC300A1CA1E400DFE9D1DAFF11C38EC1D83F50`，没有手改JAR。目标正式路径为`build/libs/create_nuclear_industry-0.1.0.jar`；其准确[来源/命令/退出/制品身份](ARTIFACT.json)已由PM读回。

与main原B366EB49…正式包比较：2278→2377文件载荷，新增99、修改10、删除0、2268保持。增改严格来自DU功能、66项18B资源、三项新增装备class、注册/创造栏两class及Heat桥接一class。全部704入包class匹配本次编译输出，1672资源匹配处理后资源；META-INF及源资源到处理后资源差异0。DU测试class/整个测试资源域排除；main原5项radiation_sensor基础资源与自动化口保持，不带辐射候选、17B外观或辅助工具。

已有DU四项JUnit/三项真实GT及保存退出、18B四项JUnit/assemble/同次组合审查、Heat三十四项回归/assemble/组合审查与用户本次三项手测按治理5.1复用。没有代码冲突或未决问题，不额外开审查层、重启游戏或重复功能/视觉矩阵。

## 原始证据与保护

原始报告位于集成树`build/reports/du-heat-accepted-20261017/`：PREPARE.md、FINAL.md/FINAL.json、准确输入清单、构建日志/退出记录、class/资源绑定、逐条载荷变化和已验文本行尾分类。初次准备因PM并行文档被保护检查拦住、两次最终核验因main既有同名radiation资源和原ZIP CRLF SHA粗筛而停止，均保留失败日志；只修核验归因，未改源、重建或改造包。最终完整核验exit0。

main的48项保护清单在报告`preserved-main-status-dirty-before.json`；除本次授权的正式JAR更新外，其余内容必须逐字保持。原DU、art-studio、已验运行副本、原世界和输入日志均保留；不推送、发布、自动安装服务器或删除来源树。本批实际应用minecraft-modding/testing/resource-pack/ci-release与分支收尾、隔离树、完成前核验技能，锁定依赖保持。
