# EXT-A-FUEL-01F 离心机端盖接缝修复交付

- 任务：EXT-A-FUEL-01F；候选分支 `codex/ore-acquisition`；基准 `bb7ea08`。
- 版本：Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82。未升级依赖。
- 本次使用技能：`minecraft-modding`（核对模型资源路径及锁定版本），`minecraft-testing`（按治理5.1只做资源几何检查和增量assemble，不跑无关JUnit/GameTest），`minecraft-resource-pack`（核对模型几何、UV、纹理引用与预览），`systematic-debugging`（先读取实际JSON并留修前证据，再实施针对根因的单一修复）。入口：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`、`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/systematic-debugging/SKILL.md`。

## 根因

修前实际JSON确认下端底座黄铜板为 `from=[2,4,2]`、`to=[14,5,14]`，下段外壳首圈从Y=6开始，二者之间竖向空档为1模型单位；底板平面范围也小于壳体外缘。上端壳体侧板止于局部Y=11，黄铜盖板从Y=11开始，竖向接触，但两端盖板原投影均为X/Z=2..14，壳体外侧平面距中心约7.2单位，未覆盖壳壁。修前诊断在下端Y=6.025和上端Y=10.975各测4096个周向探点，覆盖均为0/4096。证据：`EXT-A-FUEL-01F-assets/pre-fix-diagnostics.json`。

## 修复

生成器以四条贯通实体板（0°、45°、90°、135°）组成完整八棱盖，每条跨过中心并覆盖一对相对外缘；盖板外缘按7.30模型单位构造，比壳体外缘多0.10。底盖从Y=3.98延至6.04，上盖从Y=10.96延至12.02；四板上下表面每板错开0.002单位，总差0.006，以避免相交面严格共面，与壳体至少搭接0.034单位；与顶部中央casing和底座casing的最小搭接约0.014单位。观察窗、顶部进料口、维修口、颜色、转子、物品显示变换未改。

模型由 `tools/art-assets/centrifuge_01d_preview.py --models-only` 同步生成。仅以下四个受限写集文件有Git差异：该生成器，以及 `enrichment_centrifuge.json`、`enrichment_centrifuge_upper.json`、`enrichment_centrifuge_item.json`。转子JSON、方块状态、PNG/SVG、Java和构建文件未变；物品模型 `display` 变换与基准逐字段一致。

## 检查与构建

- 修后端盖检查读取磁盘模型，在底端Y=6.025、顶端Y=10.975各测4096个周向探点：两处均0缺口、100%覆盖；相邻八棱壳体接缝检查仍为0缺口，最小相邻法线夹角45°。证据：`EXT-A-FUEL-01F-assets/geometry-check.json`。
- 下段60项、上段62项、物品145项、转子23项；范围检查通过。完整物品模型有380个显式UV面，四个模型合计760个显式UV面；隐式UV和越界UV均为0；纹理引用、8种方块状态和原显示缩放检查通过。历史01E UV值单独标为 `historic_01e_pre_fix_uv_violations`，不属于本轮修前结果。
- 预览：`EXT-A-FUEL-01F-assets/centrifuge-endcap-angle.png` 与 `centrifuge-endcap-bottom.png`。它们仅展示JSON几何与纯色材质，不代表Minecraft客户端光照、UV或游戏内最终外观。
- 按任务卡运行唯一一次 `.\gradlew.bat assemble`（增量，无clean/强制重跑）：成功，`BUILD SUCCESSFUL`；日志为 `EXT-A-FUEL-01F-assets/assemble.log`。JAR内三份模型已核对与源码字节一致。
- JAR：`build/libs/create_nuclear_industry-0.1.0.jar`；SHA-256：`36ED63B5D8A02C532CA81E234280075872FD23AF884DB0529D652BC14734A543`。
- `git diff --check` 通过。未运行JUnit、GameTest、全素材检查；未启动用户客户端或默认run世界。

## 验收状态

资源与构建证据已交付。用户先前确认的其他手测项目沿用，不重复；端盖接合仍待用户客户端视觉复测，离线预览不能替代该门槛。现提交候选文件与证据供独立只读审查。
