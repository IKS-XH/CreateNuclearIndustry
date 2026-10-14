# 15B-R1 黑窗局部验收

2026-10-14，用户收到[修复包](ART-SHIELDED-ASSEMBLY-15B-R1-CANDIDATE.md)并复看后明确反馈：

> 这次是对的了

据此关闭屏蔽装配室C版观察窗整片发黑、内部机构被遮住的本次局部视觉门。此结论来自用户的Minecraft实机反馈，不以网页预览或原生烘焙检查替代。其它未明确报告的朝向、光照、物品视图、工序动作与整机业务不补记通过；工序动画仍未实施。

## 接受的修订与制品

仅part0/1/4/5各off/on的8个实际含窗模型补充`minecraft:cutout_mipped`，原SVG/PNG、几何/UV、三partial、物品模型与Java保持。工作树仍为`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，分支`codex/art-studio`，基线`88b8fd2`。

本次接受的修复候选为美术树`build/libs/create_nuclear_industry-0.1.0.jar`：2,812,546B，SHA256 `602985f85d2522c7914d7a4cef0b1c822f8c50ce11687e60af2be697428e3cd3`。登记时只读复核当前JAR的SHA与交付身份一致；用户反馈没有提供其客户端安装文件的独立哈希，不另补记客户端制品核验。

复用[ASSETS](reports/ART-SHIELDED-ASSEMBLY-15B-R1-ASSETS.md)原生RED16/8错误→GREEN16/0错误、[PREVIEW](reports/ART-SHIELDED-ASSEMBLY-15B-R1-PREVIEW.md)37/37及9项HTTP来源、[CLIENT](reports/ART-SHIELDED-ASSEMBLY-15B-R1-CLIENT.md)唯一13.189秒增量assemble exit0与8映射身份、[同次组合窄审及制品补核](reports/ART-SHIELDED-ASSEMBLY-15B-R1-REVIEW.md)无剩余必改的既有证据。没有重跑检查、构建或启动游戏；a92故障包、用户原截图及失败证据保留。

本次仅在`docs/art/`登记反馈和更新入口，不改资源/JAR/世界/配置，不进行Git写操作、main集成、发布或自动推进工序动画。后续集成仍由主PM或用户管理。
