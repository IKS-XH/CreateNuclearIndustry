# 可变尺寸候选：库存显示与窗口08组合补充

可变尺寸玩法仍独立待用户手测，未合main。PM只将已审共享库存修复和窗口08净来源接入同一候选，以便在实际大型反应堆复看；未引入美术树07B-R2等其他未提交内容。

## 来源与净范围

- 可变玩法原净源`08409069a5d6abc094dbf3f198a5256546bd50fa`及原候选报告保留；共享修复净应用为`209cd816ae018f734c05f08a99f309de77d30525`。两共享源的应用差异仅新库存入口和两处调用；`withCoolantInventories`仍完整保留原dimensions，远端安全/绑定/保存保护未变。
- 窗口08来自美术树HEAD`f787aa40a323d8fdd09a0f95e5798a9a2c8e3818`上的已冻结未提交输入。实际只接入Behaviour、专属Test、唯一window atlas三个src路径及`tools/art-assets/reactor-window-art-08/`136文件，共139个来源路径。PM在复制后与最终`final-net-art-sources.json`逐项核SHA相同；随后仅候选的README.md、export_window.py、index.html、mapping.json、seed_sources.py移除EOF多余空行，保留原CRLF及其他字节。134项原SHA保持，5项新/旧来源SHA在补充identity独立记录；不改原美术冻结，不重新生成或构建。原两艺术报告保留在来源树，不将其相对证据链接错误复制成候选本地日志。
- 原美术最终manifest SHA256：`1222868a903111c4a419adb0a8b416e9abf47532a10a74c34a0abd19fa2e775b`。其[同一次独立窄审](E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/reports/ART-REACTOR-08-REVIEW.md)已关闭唯一说明Minor，无必改项；[消费](E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/reports/ART-REACTOR-08-CONSUMER.md)和[资产](E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/reports/ART-REACTOR-08-ASSETS.md)是原证据入口。美术树组合JAR是其含07B-R2的独立试用包，不用于替换本候选。

窗口Behaviour只把实际窗口内面映至相反可靠外面作身份/平面校验，仍由原生Create按实际渲染face计算连接；没有固定尺寸假设。铅灰玻璃pane alpha64，原金属框角保持。普通库存入口只保留原完整运行样本，下一成功结算更新，不续租或拼接新旧数据。

## 当前组合制品与复用边界

PM在本候选只执行一次必要增量`./gradlew.bat assemble --console=plain`，8秒exit0；未重跑JUnit、真实业务或素材生成。共享原真实GREEN1/1、17/17回归及独立审查，窗口原21/21、像素检查及同一次窄审直接复用。可变尺寸原514项及整改后51项/真实用例边界仍按原CANDIDATE报告，不能改称本轮新测试或全部自然退出。

当前`build/libs/create_nuclear_industry-0.1.0.jar`为2,667,047字节，SHA256 `0940bddc9a8c6d991a3d3bbd7b42e0dd3382c6d9ca3fa0f0d482682aaea8958b`。补充[identity.json](E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/build/reports/reactor-variable-size/shared-window-08-integration/identity.json)绑定36源、53class、6资源、136工具来源；build与JAR对应条目全部一致。原`final-identity.json`的SHA仍为`67e417661cba142195779555747b756ff0c1c80ec0daa849457994fd01f20b3b`，原JAR`794a9ca…4cef`保留为`shared-window-08-integration/before-combination.jar`，未覆盖原冻结证据。

## 集中复看

从本候选重启`runClient`，沿原[尺寸手测单](CLIENT-CHECKLIST.md)检查5³、6×8×7和16³跨区块结构；同时观察近侧/对侧窗口内外面连续、铅灰玻璃仍可辨认内部、真实管道持续进出液时整片液体不再消失重现，液位与冷热混色随真实库存变化。Java修复须重启，F3+T不代替。无需重复其他设备全套测试。

当前源与组合身份已核，但实际GPU/光照/游戏视觉尚待用户反馈；本补充不关闭ART08或可变尺寸01人工门、不推进其他主线。原存档复制与备份保持，不回写main存档。
