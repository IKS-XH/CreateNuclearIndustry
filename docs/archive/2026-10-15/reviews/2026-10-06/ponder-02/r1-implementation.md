# DEVICE-PONDER-02-R1 实施交付

基线 `753125ae1f6b15b6f83e8095c54ac9d19172035d`，工作区 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。接续前执行者的未提交 A/B/D 及生成器修改，单一执行者收尾，没有 Git 写操作。证据时间 `2026-10-07T00:28:03.441538+08:00`，目录保持原 2026-10-06 批次。

## 实际技能和边界

已读候选 AGENTS、02-R1 卡及治理5.1/5.2。实际应用 minecraft-modding（锁定API、临时客户端世界）、minecraft-testing（沿用入口合同）、minecraft-resource-pack（模板/语言资源）与 verification-before-completion（读真实输出后报告）。保持 MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。

只调用临时展示的 readClient、滑块显示及实体图标；不调用正式服务器 tick、绑定、装料、SCRAM、网络事务，不修改生产算法。没有启动/终止客户端、服务端，没有旧存档检查。

## 实际写集

- `P1PonderScenes.java`：A布局/多口、B前侧回路、新D及专用展示辅助。
- `P1PonderPlugin.java`：A→D→B→C四故事板；11直接入口与离心机注册保持。
- 四反应堆NBT（含新D，C仅必要端口位置同步）。
- `lang/zh_cn.json`、`lang/en_us.json`：替换A8段及新增D8段和标题；B/C与全部离心机原值保持。
- `tools/ponder/reactor_scenes.py`：四模板与坐标生成/原格式核验。
- `P1Ponder01ContractTest.java`：沿用6项合同更新四故事板；保留11入口、资源、排除项及单次客户端接线，删除机械精确字幕段数断言，以故事方法完整收尾覆盖替代。
- 本报告；原始证据 `build/reports/extension/DEVICE-PONDER-02-R1/`。

保护 PM 文档改动、日志、已有缓存/测试世界；主目录源码未写入。

## 画面和语义核对

A：北列选区 `(2,1,1)..(2,3,1)`；中心控制列为空，内部3×3区用燃料顶盖、控制驱动和空列顶盖轮廓区分占用状态。前侧仪表 `(2,2,0)`；冷口 `(1,2,0)`/`(1,1,0)`，热口 `(3,2,0)`/`(3,1,0)`。额外冷热口依次出现，中间10tick，模板最终外壳完整。

B：仪表 `(6,2,4)`；冷/热口 `(5,2,4)`/`(7,2,4)`；各自同x管道 z=2、3，泵 z=1，罐 z=0。冷泵 facing=south 朝 +z入口，热泵 facing=north 朝 -z储罐；motor `(6,2,0)`/`(8,2,0)` facing=south 朝z=1齿轮（axis=z），与邻接泵共面。拉杆 `(6,2,3)`在仪表前侧，错开管路。原流体变化和冷热箭头语义保持。

D：六燃料列为十字五列加东南角列，控制棒位于西北/东北角。使用锁定 `RotateSceneInstruction(-65,145,false)` 俯视剖面；先中心列独立装料，第2段加入北列及对角列，第3段装西/东/南列，中心四向连接由1增至4。两棒40%→100%/40%（目标仍红色产热）→两棒100%（北/西/东受控列停止）。中心/南/东南角无相邻控制棒，完整列红色轮廓和竖线连续340tick覆盖停堆/不可抽取说明，最后仅这些未受控列切换枯竭组件；受控列保留新燃料图标。没有硬编码数字热功率、倍率或真实耗尽时长。

锁定 Ponder源码确认 createItemEntity 返回 ElementLink<EntityElement>、modifyEntity 接受该句柄、showOutline 需要独立 Object slot；锁定 Create源码确认泵前侧为输出。A/D/B正文 duration=80、idle=100；按duration+10寿命计算净间隔至少10tick；正文均带关键帧，各情景使用独立模板。

C方法与机械臂动画、原临时数据/机械臂helpers与HEAD逐方法一致，locale值保持。C的NBT仅四位置置换：冷口 `(5,2,8)`→`(5,2,4)`，热口 `(7,2,8)`→`(7,2,4)`，旧位恢复外壳、新位替换外壳，其余方块/状态不变。离心机Java文本（规范化仓库CRLF后）、NBT字节与HEAD一致，两语言所有原值一致。

## 实际验证与证据

| 检查 | 本轮结果 | 原始证据 |
|---|---|---|
| 四模板生成/gzip回读 | 1次，exit0；A110/B123/C116/D117方块，size/pos为三整数TAG_List，无重复/越界 | templates.log |
| 静态核对 | 最终通过：完整外壳、模组方块ID、端口/泵/motor方向、语言/后备、保留范围 | static-review.log |
| `gradlew.bat test --tests com.iksxh.create_nuclear_industry.P1Ponder01ContractTest --console=plain` | 1次，exit0；6 tests、0失败/错误/跳过，36s | contract.log、contract.exit.txt、原始JUnit XML |
| `gradlew.bat assemble --console=plain` | 1次，exit0；增量1s | assemble.log、assemble.exit.txt |
| 打包核对 | 四NBT和双语资源与最终源文件逐字节一致 | artifact.json |
| scoped `git diff --check` | exit0 | 本轮终端输出 |

临时静态检查脚本曾误匹配方法调用及未规范化Java换行，校正比较后通过，未因此改产品代码或增加Gradle测试。Python Store别名不可运行，改用内置真实Python；只读Create查询第一次错选缓存jar，随后按锁定路径完成。没有全量、GameTest、生产逻辑或旧存档验证；测试/assemble成功后源码与资源未再修改。

JAR `E:\MyMC\NewMod\Create_NuclearIndustry-ore-acquisition\build\libs\create_nuclear_industry-0.1.0.jar`；大小 `2163923` bytes；SHA-256 `67ff2c85e5565fcd15cf701cc12aedeca3b22813a23caaa6d1d77adc5479b1cf`。

## 人工门

静态/自动化证据不代表播放验收。PM审查并管理候选后，用户播放A/B/D和四情景入口顺序、关键帧/独立回放，确认多口/自由布局、默认镜头前侧管路、均值插棒和未受控列持续红色→枯竭结束。C及离心机既有播放门保持，不自动推进下一设备。
