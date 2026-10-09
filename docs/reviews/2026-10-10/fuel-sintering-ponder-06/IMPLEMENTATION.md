# DEVICE-PONDER-06-SINTERING 实现交付

2026-10-10，执行者交未提交候选；未验收、未提交、未启动客户端。工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，功能派发基线 `f3d5cb7`；执行期间PM纯文档提交后当前HEAD为 `5a17143`。正式设备逻辑、配方、配置及其他教学未修改。

## 实现与写集

新增 `FuelSinteringPonderScenes.java` 两幕：`fuel_sintering_operation` 展示底部普通燃烧室、顶部投料、一进一出、断热暂停和恢复；`fuel_sintering_automation` 展示顶部窗口溜槽、北侧Create漏斗取料、下方原版漏斗承接并向西侧桶送出，再隐藏物流并把同一底部格替换为朝东核换热器。冷热储罐与管道沿东西轴，后侧热液、前侧等量冷液，先显示供应库存再点亮热源。炉体y=2、热源及承接设备y=1；两份模板7×6×7，地台y=0。

只写任务卡允许的9个路径：

- `src/main/java/com/iksxh/create_nuclear_industry/ponder/FuelSinteringPonderScenes.java`
- 同目录 `P1PonderPlugin.java`（新增独立入口和两幕ID；原入口顺序保持）
- `src/main/resources/assets/create_nuclear_industry/ponder/fuel_sintering_operation.nbt`
- 同目录 `fuel_sintering_automation.nbt`
- `tools/ponder/fuel_sintering_scenes.py`
- `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`
- 同目录 `en_us.json`（分别仅追加8个烧结教学键）
- `src/test/java/com/iksxh/create_nuclear_industry/FuelSinteringPonderContractTest.java`
- 本 `IMPLEMENTATION.md`

库存/进度使用既有 `FuelSintering` NBT快照，工作单位仍为有效tick，不复制正式加工引擎；字幕不宣称教学压缩时长等于400有效tick。正文每幕三段，中文fallback与中文资源完全对应，英文同序同义。Ponder1.0.82本地 `FadeInOutInstruction` 构造器确认额外10tick生命周期，正文统一等待duration+20，保证退出后至少10tick净空。核热段不复讲换热器成本、串联和多用途。

## 实际读取与API核对

已读取本树AGENTS、文档入口、任务卡、治理5.1/5.2；使用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md`，以及superpowers实施、TDD和verification-before-completion技能。本Agent按派发执行者权限实施，不按通用技能做Git写、另建树、写核心文档或派发子Agent；独立审查由PM安排。

Gradle确认MC1.21.1、Java toolchain21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。本地jar通过javap核对 `modifyBlockEntityNBT`、`ChuteBlockEntity.setItem`、Chute的WINDOW形态、漏斗EXTRACTING及面向后方库存访问、文本淡出生命周期；源码核对 `FuelSintering{Block,BlockEntity,State}`，输入顶部、输出四水平面、底面无能力、400有效tick和断热保留工时。

## 验证证据

全部原始证据在 `build/reports/extension/DEVICE-PONDER-06-SINTERING/`。

| 命令或检查 | 结果与证据 |
|---|---|
| 首次定向测试被运行环境中断 | `01-red.log`、`01-infrastructure.xml`保留；深层原因为InterruptedException，不计合同red，没有伪造正常退出码 |
| `./gradlew.bat test --tests '*FuelSinteringPonderContractTest'`，实现前 | 退出1，实际3项合同失败（缺实现、入口、模板）；`02-red.log/.exit/.xml` |
| Python `-B tools/ponder/fuel_sintering_scenes.py` | 退出0；生成后读取落盘NBT，校验ID/属性/坐标/上下相邻/输入输出方向/最低高度，同输入再次编码压缩字节一致；`03-templates.log/.exit` |
| 首版定向green | 退出0，3/3；`04-green.log/.exit/.xml` |
| 顶部投料容器显示及窗口溜槽调整后的模板验证 | 退出0；`05-templates-final.log/.exit` |
| `./gradlew.bat test --tests '*FuelSinteringPonderContractTest' assemble`，最终差异 | 退出0，JUnit3/3、0跳过/失败/错误，增量assemble成功；`06-final.log/.exit/.xml` |
| 本地Ponder文本生命周期 | `07-ponder-fade-api.txt` |
| 本批跟踪文件定向`git diff --check` | 退出0；`08-diff-check.log/.exit` |
| JAR读取与源码一致性 | 两份新NBT与源字节一致，新场景class及双语各8键进入JAR；`09-artifact.json`同时登记最终8个源码/资源/测试SHA |

最终JAR：`build/libs/create_nuclear_industry-0.1.0.jar`；SHA256 `0aead57d60b796e17c461a98bdeee2e7721d413a6cffbabcbe45af3986ab5206`。

最终模板SHA256：operation `a1f74ddb22c02a400b828047b0bf4b7190868496e0bf056a5f38c5c2a7945964`；automation `51799a2bf72f0a128f8db0e8d245e68c101493d791144b61e31733533b913b73`。第一版automation模板SHA保留在03证据，不冒充最终版本。

初次测试中断后恢复运行；未修改构建脚本、依赖、设备逻辑或过滤测试绕过失败。模板生成使用`-B`避免新增pycache。开工已存在logs/debug.log、logs/latest.log及三个pycache目录未清理、未回退；NeoForge定向JUnit会继续写同一既有日志，因此全工作树diff-check可看到日志的空白告警，本批定向差异检查为0。

## 交付边界与下一依赖

自动检查仅证明编译/合同/NBT/打包结果。尚待PM一次独立规格+质量审查，再由用户逐幕播放观察底部热源、投料/水平取料、暂停/恢复、核热回路、镜头和文案净空；本执行者不宣布人工视觉通过、不改变任务状态、不接装配台或其他主线。没有新增玩法或旧存档兼容影响，无越界实现请求。
