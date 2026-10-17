# 贫化铀装备、18B外观与锅炉队列联合验收依据

2026-10-17，主逻辑PM。用户先确认“贫铀装备和对应的外观都通过手动测试了，合到主分支里吧”，随后追加“服务器端崩溃那个问题也通过测试了，一块合并进来”。这是本批三项人工门与合入授权；其他独立候选不在本次范围，当前结果统一见[STATUS](../../../STATUS.md)。

## 对应来源

- 贫化铀逻辑`0a10e361cee9a8069f8bd0b2a711f2cb60349797`：锭块链、九种钻石装备原生锻造升级、下界合金性能与贫化铀锭修复。原[4项JUnit/3项真实服务端及保存退出](../../2026-10-16/depleted-uranium-01/IMPLEMENTATION.md)、[一次组合审查](../../2026-10-16/depleted-uranium-01/REVIEW.md)复用；本次用户反馈关闭客户端功能门。
- 美术18B来源：`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-DEPLETED-URANIUM-18B/artifact/`，67文件集成ZIP为31671 bytes、SHA256 `F1580CB2E645FBC3A15AF7227104A170147FAE7A885CA18311DB36C0F95918D3`。对应已验JAR3389257 bytes、SHA256 `04466D8AC3ADA5BA5533D7EAA06F371C5AA9F7F6CB0144D18076BF2685F024EE`，为0a10e36加66资源及一个公共护甲材质钩子，不直接以这个旧主线整包覆盖main。原4项JUnit、一次assemble及同次组合审查已核；本次用户反馈关闭专属外观、穿戴/饰纹/附魔/手持视觉门。
- 锅炉桥接队列`8712f4310eb1ff6ac8f2af03fde33e4154a71325`：PENDING原子取批、锁外刷新，新请求保留到下一tick。[有效RED、34项GREEN与一次assemble](../heat-pending-01/IMPLEMENTATION.md)、[组合窄审](../heat-pending-01/REVIEW.md)复用；用户本次反馈关闭所报服务器崩溃的原服观察门，不延伸为所有整合包或其他异常保证。

无代码冲突的合入按治理5.1复用上述证据，不重复执行装备/机组功能、整套JUnit/GameTest或游戏视觉。三个核心文档冲突由PM保留main最新路线/验收记录；必要的新打包及来源/载荷一致性记录于本批集成报告。未自动推送、发布、安装生产服务器或回写任何世界。
