# 屏蔽装配室15B · 已选C正式外观

本页读取 `assets.json` 映射的29个候选/生产资源：八格off/on JSON、三件待机partial、完整物品JSON和九张实际16² PNG。默认入口读取生产资源，`?source=candidate`仅供安装前核候选。所有模型和PNG逐项核SHA；缺失不会退回SVG、旧材质或替代方块。

```powershell
node server.mjs 4194
```

服务仅127.0.0.1，固定当前29资源/页面/现成库/15A已核Create参考白名单，无任意读写。按15B安全补卡以普通exec托管会话前台启动，保留session ID，不创建脱离管理后台进程。自有PID/executable/cwd由 `/health` 与本批preview报告记录。capture仅写固定machine/item两图及JSON。

整机按 `part=x+2*z+4*y` 各格offset16恢复0..32模型；三partial仍整机坐标不再偏移。实际物品为shell+三partial完整归一化0..16，使用其保留GUI配置，不简化成方块图标。六观察方向/同步拖动/缩放、整机剖视/隐藏、真实八on/off灯模型与重置均为静态。物品保持正式静态off模型。

底轴复用15A锁定Create shaft_half/轴PNG/Mojang父模型，绕主控[8,8,8]X+90°映射DOWN，包络[6,0,6]..[10,8,10]，底接触点[8,0,8]。保持Create第三方身份，不画入CNI静态机壳。不新增工序动作或游戏状态。

依赖复用本地block-model-renderer2.20.0和现成Three0.180.0，未安装或升级。PNG原alpha采用cutout；普通机壳不透明。最终资源检查/负责人GPU/组合审查/游戏手测分别记录。

15A原机JSON代码已按15B卡指向本工具20模型完整路径备份，旧PNG来源保持；其历史三稿与capture未改。自动审批拒绝原后台/重启命令后按补卡保持运行中的4193/PID18084，原机按钮禁用并标明“旧原机模型已存档”；磁盘server重定向尚未运行，不再重试或终止旧服务。两拒绝原证据与托管4194成功身份分开记在15B报告。

本批增量assemble由预览执行者在安装29资源及同次审查通过后唯一执行。旧7d包先备份，子进程TEMP/TMP/java.io.tmpdir可指向E盘本批client目录；不改全局环境或原Gradle缓存。失败保完整日志、exit与原包，不手工拼JAR。资源与包核验脚本均在preview目录。

## 15B-R1真实Minecraft黑窗窄修

用户实际Minecraft报告观察窗整片黑，原Three/生产PNG预览通过不能关闭该故障。R1由资产作者核实际原生BlockModel渲染层/alpha链，只对确认目标修改必要模型metadata；本页scene/GUI/几何/九PNG及既有强制alpha剔除保持，不用网页透明效果冒充游戏修复。

当前assets.json的renderLayerR1记录本次批准changedTargets，verify.mjs仅核新manifest/变化模型HTTP身份。build-client.ps1与verify-jar.ps1现匹配a92故障包（2,812,280B/a92a0bed…），R1记录进入新的ART-SHIELDED-ASSEMBLY-15B-R1目录，原15B脚本原字节先备份。旧79/原图/7d/d12和旧日志不覆盖；4194/session53337/PID16460、4193均不启停。

原同次资源审查无必改后才唯一assemble，载荷仅准changedTargets中的模型metadata；不改Java或其它资源、不启动游戏或全测试。R1局部候选仍需用户观察同一窗口与内部机构，打包/原生选择检查不能替代该游戏视觉门。
