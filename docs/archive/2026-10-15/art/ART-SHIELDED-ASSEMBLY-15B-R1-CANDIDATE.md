# 15B-R1 黑窗局部候选

最新状态：2026-10-14用户明确“这次是对的了”，按[局部验收](ART-SHIELDED-ASSEMBLY-15B-R1-ACCEPTANCE.md)关闭本次观察窗黑面及内部可见性门。修复包身份保持，下方待用户复看的描述为交付时历史；未补记其它未明确项目或推进工序动画/main。

2026-10-14用户实际Minecraft反馈[中间发黑](ART-SHIELDED-ASSEMBLY-15B-FEEDBACK.md)，按[R1卡](tasks/ART-SHIELDED-ASSEMBLY-15B-R1.md)修正静态玻璃面绑定层。旧a92故障包已精确备份，用户局部故障门尚未关闭。

## 根因与最小修订

原C玻璃PNG有247个透明黑RGB像素。含窗part0/1/4/5的off/on模型共8个没有render_type，NeoForge原生完整烘焙实际回落SOLID；solid shader不会剔除alpha0且写入颜色/深度，导致窗面遮住内部并画黑。仅这8模型加入minecraft:cutout_mipped，原9PNG、C几何/UV/窗形/灯、三partial、完整物品/display与Java保持。

离线原生探针使用锁定MC1.21.1/NeoForge21.1.219的16模型完整ElementsModel烘焙与BakedModel层查询：真实RED错误绑定8/exit1；同一探针GREEN错误绑定0/exit0，window8真实cutoutMipped且非solid，另8保持solid，原quad数不变。工具空FML清单/原生vanilla预注册只用于离线bootstrap，未启动游戏或新GL窗口；编译/环境前置失败另保，不计根因RED。

## 实际证据与当前状态

- [ASSETS](reports/ART-SHIELDED-ASSEMBLY-15B-R1-ASSETS.md)：真实原生链/两个shader来源、247alpha0、RED/GREEN、8候选/生产metadata-only、57保护零变化与29映射同源。原导出器同步R1入口、防旧模式回写，历史检查/备份保留。
- [PREVIEW](reports/ART-SHIELDED-ASSEMBLY-15B-R1-PREVIEW.md)：仅变化manifest+8模型9HTTP身份、37/37、exit0；原Three/scene/GUI未改。网页不能重现solid shader错误，旧图只支持不变C外观，不称作故障复看。
- [原同次组合窄审及制品补核](reports/ART-SHIELDED-ASSEMBLY-15B-R1-REVIEW.md)已实读交回，无剩余必改；62资产/6probe/6预览来源身份零漂移，真实层与最小修订符合。唯一assemble13.189秒exit0，作者和审查者已停写，局部游戏画面待用户复看。

## 当前修复包

使用美术树 [create_nuclear_industry-0.1.0.jar](../../../../build/libs/create_nuclear_industry-0.1.0.jar)，2,812,546B，SHA256 `602985f85d2522c7914d7a4cef0b1c822f8c50ce11687e60af2be697428e3cd3`；root实际GetFileHash与原同次审查独立读回一致。唯一增量assemble的compileJava为UP-TO-DATE、processResources/jar各执行一次。8候选/production/processResources/JAR均同源，恰好8个批准metadata模型改变，0removed/0errors；1947个非本批旧载荷保持，其中557class保持，离线probe没有进入JAR。见[CLIENT](reports/ART-SHIELDED-ASSEMBLY-15B-R1-CLIENT.md)与本批client原始log/exit/核验JSON。

原2,812,280B/a92故障包精确备份至R1/client/backup，原7d/d12及其它历史不写；旧a92入口为故障基线，不再作为当前包。新包只交局部复看，不发布/Git/main集成。

工作在 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio` / `codex/art-studio`，原4193/4194/PID16460/session53337保持。main/Git/世界/配置/玩法/光传播/渲染器与工序动画未改；没有删除玻璃或强制满光。

## 仅本次局部复看

在本美术树重新启动`./gradlew.bat runClient`，或更新到上方当前修复包后重进游戏，在同一台设备、同一扇观察窗看透明区域与内部双臂/夹具。原进度/配方/整机业务和其它设备不用重测，本批不把它们补记通过。静态层原生GREEN与打包身份都不代替Minecraft真实画面。
