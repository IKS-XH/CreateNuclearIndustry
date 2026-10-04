# EXT-B-BOILER-01D 交付报告

## 范围与实现

基线为 `a95d497`，运行版本保持 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82。代码只改锅炉结构、控制器和锅炉 GameTest；没有更改共享库存/热量算法、保存账本、压力实现、控制器锚点或资源模型。

- `BoilerStructure.inspect` 与 `issue` 仍要求固定5×5×5、唯一侧面中央控制器、唯一顶中央安全阀、底部热段及原内部空气；给水只认第2层，蒸汽只认第4层。每侧三个非棱边槽允许对应端口；控制器占用所在面的中央给水格，因此最多11水口、12汽口。朝向按外表面轴确定，面内偏移不参与法线判断。
- owner反查可从面内±1位置倒查到既有控制器；候选检查先使用`hasChunkAt`，不加载区块。结构缓存、epoch旧能力失效和全部侧排候选格的能力失效均保留。
- 结构形成或拆除后，控制器重算第2/4层、四面每组三个相邻Create管面的方块状态并传播变化，覆盖偏移给水与汽口。每口仍独立256mB/t，水汽各自保持16000mB共享库存。

## 测试覆盖与复核

综合侧排GameTest先让中央口锅炉成型，再加满11个水口和12个汽口。它逐口真实交易并核对共享库存及额度、清除owner缓存后从南面偏移水口执行反查，并确认错朝向、错层和棱边给水口均拒绝成型且`issue`定位到对应坐标。

Create输送测试通过真实泵从南面偏移水口进水，并经东面切向偏移汽口向原生储罐出汽。既有`steamPipePlacedBeforeFormationReconnectsAfterShellRestore`的东侧支路现改为`CENTER.offset(2,3,1)`偏移汽口，管线和储罐对应移至`z+1`，继续复用先铺管成型、加口、拆接、拆壳、重搭及守恒流程。拆口t34记录蒸汽罐新基线，确认管压释放、当前管流为假；重接t47要求储罐蒸汽严格高于该基线；拆壳前重新获取有效句柄，拆壳后句柄失效；重搭后双路分别超过拆壳前基线且总量等于8000mB原有库存加2000mB补入量。独立审查者静态确认了坐标、基线时序和棱边诊断断言。

- 最终命令：`gradlew.bat -PgameTestNamespace=create_nuclear_industry_boiler -PgameTestDirectory=build/gametest/EXT-B-BOILER-01D-reviewed runGameTestServer --console=plain`。18/18 required GameTest通过，世界保存完成，构建成功；新run目录没有现成`server.properties`，服务端记录缺文件后以默认设置继续启动。证据为`EXT-B-BOILER-01D/gametest-final.log`。
- 最终`gradlew.bat assemble --console=plain`增量打包成功，证据为`EXT-B-BOILER-01D/assemble-reviewed.log`。
- 未重跑JUnit：锅炉账本及压力算法没有变化，复用`EXT-B-BOILER-01C/test-assemble-rerun1.log`中的12/12锅炉JUnit通过记录。

## 证据说明与后续门

审查期间曾启动一次未完成的同名GameTest命令。正式运行时日志目标仍沿用审查者临时使用的`gametest-final-reviewed.log`，因此该未完成日志被完整正式运行覆盖；不把被覆盖的中断运行作为证据。完整正式日志另复制为项目经理指定的`gametest-final.log`，此前原始日志`gametest.log`和`assemble-final.log`仍保留。独立审查者确认本轮补强静态符合合同。客户端仍需由项目经理安排同面三口及非中央管线人工复测。

任务开始时工作区已有其他docs、日志及缓存改动，均未编辑或清理；没有执行Git写操作。

## 技能

实际读取并使用`minecraft-modding`、`minecraft-testing`技能，按治理§5.1选择本批锅炉GameTest与增量assemble；依赖版本核对后未升级技术栈。
