# ART-REACTOR-09：驱动器、换料口与深度控件试用

当前状态（2026-10-12）：用户在主PM对话报告手测通过，并对“可变反应堆、快捷换料、观察窗和顶面控件复看”明确回答“上述项目都通过了”。依 [实际组合验收记录](</docs/archive/2026-10-15/reviews/2026-10-12/reactor-variable-accepted-integration/ACCEPTANCE.md>)，本09顶盖/控件组合及08对应范围的待复看状态结束；不将组合确认扩写为所有光照、全部面、网络的逐项验收，也不关闭其他设备美术门。

本组合274项src/tools已净合main `6bdef37e96a2d708be95d786e09f59adcd3a72b6`，来源为接受候选 `edb5b653c51867df26a2f16030f5ed52cf1923f3`。主目录复用同源冻结JAR2,678,313字节/SHA256 `15c0ab893d080c6aa93c11f05e5e09ef55931979bd4fc8693ad502802db14ddd`，负责人已实际读验收并核主包身份。原99来源、美术freeze和其他历史证据不改，没有新增测试/构建；本美术生产树尚未净同步新变量玩法，Git/跨逻辑同步仍由主PM负责。

以下为验收前来源、候选入口与当时人工门的历史记录，不再要求重复本次已通过的组合复看。锅炉上限16³是主PM另行管理的新逻辑范围，本交接不派发实现或改变其状态。

本批针对用户密集顶盖截图中的设备混淆与深度数字过小。驱动器用亮钢色导轨、轴承桥及中央方杆通道，换料口用大面积暖铜色舱盖、铰链和锁扣；四张基础与四张成型CT同步替换。铜色为视觉身份，不改变配方材质或设备规则。

顶/底原生百分数字号等比扩大约两倍，锚点移至顶面外缘中段；本格椭球命中截面面积约为原球截面的3.24倍。文字、白框与热点均避开0.6格棒体及0.65格端箍。四侧原生显示/命中、长按调节、0..100范围、权威提交与锁定保持，不增加独立GUI。

## 已核证据

负责人实际浏览 [四组对照、近远混排与遮挡预览](../../../../tools/art-assets/reactor-device-art-09/preview/index.html) 并打开 [生产Pose数据的离线布局](../../../../build/reports/art/ART-REACTOR-09/consumer/slider-layout.png)。预览是SVG/PNG二维示意，布局只重放锁定第三方后置矩阵；真实Font/GPU和玩家长按尚未运行，不代替客户端验收。

68张整数SVG导出八张PNG，RECTANGLE 4×4、孤立index12及原ID/UV保持。素材导出/check exit0；八张旧正式PNG先完整备份，候选/安装逐字一致。原几何真实RED为9项AssertionFailedError、exit1；最小控件修改后同class9/9 GREEN、failure/error/skip0、17秒exit0。唯一组合assemble exit0、3秒，未重测或全量构建。

[素材报告](reports/ART-REACTOR-09-ASSETS.md)、[消费报告](reports/ART-REACTOR-09-CONSUMER.md)及 [活动卡](tasks/ART-REACTOR-09.md)记录来源、失败与局限；本次 [独立组合窄审](reports/ART-REACTOR-09-REVIEW.md)已完成，无必改项。负责人已实际读原始log/exit、GREEN XML和最终审查，并核当前JAR/双源/freeze身份。原08玻璃/液面、07B-R2汽轮机以及可变尺寸业务的证据和人工门分别保留。

## 冻结与净接入

美术当前与 [冻结JAR](../../../../build/reports/art/ART-REACTOR-09/consumer/final-ART-REACTOR-09.jar) 为2,624,615字节、SHA256 `737d3bd5606109f711998da92714917de19b6ed891f0abb7fbb27ca3f1049050`。两源仍与GREEN同身份、两compiled class和八资源的build/JAR一致。原08 JAR先完整备份，原08/07B-R2冻结保持。

[净09清单](../../../../build/reports/art/ART-REACTOR-09/consumer/final-net-art-sources.json)为99路径：两共享源、八正式PNG、87新工具文件和两稳定报告。旧08/07B-R2只作为既有输入，不夹带到本次净同步。[最终冻结](../../../../build/reports/art/ART-REACTOR-09/consumer/final-freeze.json) SHA256 `79fdaf1e3895f390f40faef4248d5e72ec8380bd2a4b0008228f4fce7ec1e67d`，另绑定32消费证据、21素材证据及共享只读合同；负责人活动文档和审查列为交接路径，不是生产JAR输入。

主PM已将99净来源精确复制到可变候选，初次99/99 SHA一致，艺术来源提交 `26f4ba25ff71085009bf799ded418e9b796c141d`。候选CONSUMER交接报告仅规范化换行，由PM另外记录身份；本树原99/freeze未改。其用户独立指定的换料操作范围与本批无生产源交叉，由其执行者持有最终组合。固定尺寸美术JAR未覆盖可变候选JAR，原可变/08包已备份。

负责人已实读候选 `build/reports/refueling-quick-swap-01/13-final-combined-assemble.log`、exit0及14/16绑定：该次组合assemble exit0、1秒，本批双源/八正式PNG仍与原净清单一致，两控件class与build/JAR相同；本批没有重跑测试或生成。当时可变候选包2,677,917字节、SHA256 `896c2c38d984680eb61452d2a6ba5757e4a08e8931a2f19335acc5e26a57d6bf`，现已作为历史组合备份保留。原中间 `beb4b123...` 包也不是现行入口；换料独立审查已无必改项，功能及全部美术人工门仍分别保持。

## 现行组合入口更新（2026-10-12）

主PM通知原世界进入时发生换热器弱表CME，按其独立限定修复将候选更新至源提交 `5d01314c1af38ba4665a6b1af32ea355e49770fc`、文档HEAD `edb5b653c51867df26a2f16030f5ed52cf1923f3`。现行入口为 [换热器加载整改候选](</E:\MyMC\NewMod\Create_NuclearIndustry-art-integration\docs\reviews\2026-10-12\exchanger-load-crash-01\CANDIDATE.md>)，仍从同一art-integration目录和原存档重启；先确认正常进入，再继续原集中单第4节。

负责人已实际读该候选及其独立审查的制品差异记录，并核当前JAR为2,678,313字节、SHA256 `15c0ab893d080c6aa93c11f05e5e09ef55931979bd4fc8693ad502802db14ddd`。该批记录旧新2120条目中仅桥class变化、ART09及快捷换料34输入保持；本美术对话只登记入口，不复验换热器逻辑或重复任何测试/构建。本树美术生产源、99来源、原冻结/备份不改；该桥尚未同步本美术生产树，09/08/尺寸/快捷换料及进入存档人工门分别保持。

## 客户端集中复看

可变反应堆从 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration` 重启客户端，在该目录运行 `./gradlew.bat runClient`，沿用现有存档。Java改动需要重启，不能仅F3+T。本轮复看密集屋顶的钢色驱动器/铜色舱盖是否一眼可分、插入/拔出时外围特征是否可见；准星移到钢色驱动器顶面外缘中段，比较0/50/100%数字，按原Create长按方式调整，确认略微移动准星仍易维持操作、相邻格不会误触。

主PM最终文档HEAD为 `d819fb66da0b1c355a324201f4456d73406b3f4f`，本轮内部接入交付结束，停止自动推进。负责人实际读 [最终组合候选](</E:\MyMC\NewMod\Create_NuclearIndustry-art-integration\docs\reviews\2026-10-12\refueling-quick-swap-01\CANDIDATE.md>)、[集中复看单第4节](</E:\MyMC\NewMod\Create_NuclearIndustry-art-integration\docs\reviews\2026-10-12\reactor-variable-size\CLIENT-CHECKLIST.md>)及 [ART09组合来源](</E:\MyMC\NewMod\Create_NuclearIndustry-art-integration\docs\reviews\2026-10-12\reactor-variable-size\ART09-INTEGRATION.md>)。其目标CONSUMER报告SHA为 `2c573b630e539fe44a8acd50f27b0bbfd69d4855a65e198b27cb0361e6fc71f3`，仅修原多重CR换行，其余98来源保持；本树原freeze不改、不追加验证。本页不关闭09字号/操作、08视觉、可变尺寸业务或快捷换料人工门。
