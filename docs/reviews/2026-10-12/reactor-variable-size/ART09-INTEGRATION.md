# ART09与快捷换料：候选组合接入

本批沿用可变尺寸候选树和其原客户端存档。只净接入已经完成内部检查的ART09来源；快捷换料由主PM单独任务管理，三个端口功能源与ART09控件双源、八素材无交叉。两项人工门以及可变尺寸/ART08门分别保持。

## 美术来源与复用证据

- 来源树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，HEAD `2ae83b9c9d355c5229f973849e04c449dea0ceb5`；净清单 `build/reports/art/ART-REACTOR-09/consumer/final-net-art-sources.json` 为99项。目标副本见本候选 `build/reports/refueling-quick-swap-01/art09/final-net-art-sources.json`。
- PM实际逐项核来源并复制后核目标：99/99 SHA一致；换料三个生产Java复制前后未变。净集包括原共享控件两源、8正式PNG、87个独立新工具文件、ASSETS/CONSUMER两报告；不夹带07B-R2或固定尺寸玩法。
- 原 `final-freeze.json` SHA256 `79fdaf1e3895f390f40faef4248d5e72ec8380bd2a4b0008228f4fce7ec1e67d`，副本与原绑定在上述art09目录。原美术JAR `737d3bd5606109f711998da92714917de19b6ed891f0abb7fbb27ca3f1049050`、2624615字节仅作来源证据，未用它覆盖可变包。
- 复用原RED9断言exit1、定向GREEN9/9 exit0/17秒、素材安装8/8、唯一美术assemble exit0/3秒，以及[同一次独立组合审查](../../../art/reports/ART-REACTOR-09-REVIEW.md)，无必改项。PM实际读报告及原冻结身份，不重复原控件/素材测试、生成或构建。
- 双源职责只为CornerTransform顶底锚点、等比字号与本格椭球命中。四侧、0..100、提交及锁定保持；基础/成型材质同步区分钢色驱动器和铜色换料舱盖。

## 最终组合与交付

原可变/ART08组合包已备份至 `build/artifact-backups/refueling-quick-swap-01/before-art09-and-refuel.jar`，其SHA为 `0940bddc9a8c6d991a3d3bbd7b42e0dd3382c6d9ca3fa0f0d482682aaea8958b`。由快捷换料执行者在本次功能GREEN后执行一次最终组合assemble，最终源/class/资源/JAR绑定及状态以[快捷换料候选](../refueling-quick-swap-01/CANDIDATE.md)为准；本页不把接入本身冒充功能或视觉验收。

从同一候选 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration` 运行 `./gradlew.bat runClient`。Java已变化，需重启客户端，不能只F3+T。复看密集顶盖近远辨识、顶/底0/50/100%字体与原长按调节、棒体升降遮挡及相邻格不误触；功能新增只检查枯竭替换成功、尚有耐久拒绝及物品返还。尺寸和ART08原集中单继续独立记录。

PM提交检查补充：净复制时99/99字节一致。随后仅目标CONSUMER报告原多重CR换行触发git whitespace检查，PM规范化为正常LF并保留文本；原美术报告/冻结清单不改，目标报告SHA为 `2c573b630e539fe44a8acd50f27b0bbfd69d4855a65e198b27cb0361e6fc71f3`。其他98源身份保持，未修改生产/测试/素材/工具或追加验证。
