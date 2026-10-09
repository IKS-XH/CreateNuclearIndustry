# DEVICE-PONDER-06-SINTERING 独立窄审

2026-10-10；审查执行者，无PM权限。工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，当前HEAD `5a17143336ac55a43dfd4df35b45cd758a77d45e`；实现仍未提交。本次合并规格与质量审查，只写本报告，未修改实现、任务、核心文档或Git状态，未派发子Agent。

## 结论

**静态规格与质量审查通过，未发现阻断问题。** 可以交PM登记候选并准备本台播放；本结论不关闭用户客户端播放门、不授权提前整合main或接装配台。

## 核查结果

- 阅读AGENTS、文档入口、治理5.1/5.2、06任务及IMPLEMENTATION，实际读取并应用 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`、`superpowers:requesting-code-review`（含审查模板）和 `verification-before-completion`。以仓库精简验证和权限规则覆盖通用技能的重复跑测/Git操作。Gradle锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。
- 两幕独立绑定烧结炉；插件原入口和顺序未变。双语仅各追加8键，原键值保留；六段正文顺序与中文Java fallback逐项对应，英文同义。所有新增手写职责与时序说明使用中文。
- 对本地依赖实际执行只读javap：确认Ponder `modifyBlockEntityNBT` 泛型、物品实体API、Create溜槽两种 `setItem`、WINDOW形态、漏斗以朝向反面访问库存，以及淡出构造器使用duration+10。场景统一等待duration+20，前段完全退出后至少10tick净空。
- 读取正式 `FuelSinteringBlockEntity/State`：顶部仅进生料、四水平面只取成品、底面无能力；400有效tick，断热保留工时。场景只载入既有 `FuelSintering` Compound中的整数Input/Output/Progress，未复制引擎或改正式行为。一进一出与停热200、恢复300、产物增加的快照顺序符合已有规则，未把教学缩时写成工时。
- 只读解压两份落盘NBT：7×6×7，分别51/62格，炉(3,2,3)、热源(3,1,3)直接接触，全部设备y≥1。顶部窗口溜槽下接炉顶；北面extracting漏斗后方为炉，下面承接漏斗朝西连接桶；物品演示向北下落并进入承接桶，无水平输入能力暗示。
- 核热段隐藏物流后只替换同一底部格；朝东换热器的后侧西进、前侧东出与 `HeatExchangerLine.inlet/outlet`一致，东西管路及储罐可见。先给热液库存，再点亮，后续热液减少200mB、冷液增加200mB；未新增蒸汽供热或正式事务。沿用既有教学的临时世界库存展示边界，不以这段动画证明真实管网运行。
- 合同测试读取实际NBT与双语，覆盖入口、热源高度/相邻格、合法物流面、文本时序及临时快照边界；3项规模符合本批风险。它们不证明渲染体验，未要求新增镜像测试或扩大测试。

## 原始证据与一致性

证据目录：`build/reports/extension/DEVICE-PONDER-06-SINTERING/`。

- 读取 `02-red.xml`：实际3项断言失败；首次环境中断未计合同red。
- 读取 `05-templates-final.log`：最终模板读取校验与确定性记录；本审查没有运行生成器main或写模板。
- 复用 `06-final.log/.exit/.xml`：test与assemble退出0，JUnit实际3/3、0失败/错误/跳过。这是执行者原始运行证据，审查没有重跑Gradle、全量、GameTest、客户端或旧存档测试。
- 实测冻结8路径SHA256全部匹配 `09-artifact.json`；实测JAR SHA256为 `0aead57d60b796e17c461a98bdeee2e7721d413a6cffbabcbe45af3986ab5206`。只读ZIP确认新场景class存在、双语各8键一致、两份NBT与源码字节一致。
- 最终模板SHA256：operation `a1f74ddb22c02a400b828047b0bf4b7190868496e0bf056a5f38c5c2a7945964`；automation `51799a2bf72f0a128f8db0e8d245e68c101493d791144b61e31733533b913b73`。`07-ponder-fade-api.txt`与本次本地javap核查相符。

## 保留人工门与未评判范围

- 未启动客户端：炉/热源实际遮挡、缩放/偏移、字幕与控件空间、窗口物料、承接动画及暂停恢复的观感，必须由用户逐幕播放观察；静态高度和时序合格不能宣布视觉通过。
- 不复测已经验收的烧结供热功能或其他设备教学；本批未改变这些实现。
- 不研究旧存档兼容，按治理5.2；不处理既有日志、pycache或独立美术视觉门。
