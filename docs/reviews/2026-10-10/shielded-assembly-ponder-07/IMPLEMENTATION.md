# DEVICE-PONDER-07-ASSEMBLY 实现交付

2026-10-10，执行者提交未提交教学候选，等待PM一次独立规格与质量窄审及用户三幕播放。工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，实际开工及交付HEAD均为 `03e3fbeee747b9042c3468eba49bc0c5f29365f2`。未执行Git写操作、派代理、启动客户端/游戏服或操作用户世界。

## 实现与精确写集

新增三幕 `shielded_assembly_placement/manufacture/sealing`：整台2×2×2放置、主控唯一底轴、四周物流及顶部只进料；8/4/2/1新组件装配；三种各1的乏燃料一次封装。机体主控 `(3,2,3)` 朝北，底轴 `(3,1,3)`；北侧 `(3,2,2)/(3,3,2)` 与西侧 `(2,2,3)/(2,3,3)` 四入口上下分开，投到西侧时短转镜头25度，随后复原。北侧另一面 `(4,2,2)` 取出，经 `(4,1,2)` 向西漏斗进入 `(3,1,2)` 桶。顶部窗口溜槽仅在接口幕显露。地台y=0，所有教学设备y≥1。

实际批次数量用ItemStack显示并逐项累积到唯一主控；完成后清空输入，写单件输出，再从主控移走并写承接桶。使用正式 `ShieldedAssemblyState.load` 和ItemStack保存格式，工时为RPM·tick。封装通过 `SpentFuelPayload.seal(spent)` 构造真实带 `DataComponents.CONTAINER` 的产物，图标、主控输出与承接桶复用该完整栈；没有使用无载荷JEI结果模板。正式生产tick和物流事务不运行，教学播放压缩时长不作为正式工时。

模板三份均为7×6×7、66格，同一固定教学UUID配对 `ShieldedAssemblyOwner/OwnerId`，七代理使用原版 `BlockPos.asLong` 格式的 `MasterPos`，PART严格符合 `x+2*z+4*y`。主控 `expanded=true`，仅主控保存空的正式账本，代理不复制库存。生成器回读落盘NBT并核对全部坐标、状态、NBT和归属，同输入再生成字节一致。

允许的10个路径均已交付：

- `src/main/java/com/iksxh/create_nuclear_industry/ponder/ShieldedAssemblyPonderScenes.java`
- 同目录 `P1PonderPlugin.java`：只追加三幕与装配台物品绑定，不绑定无物品代理，不改原入口顺序。
- `src/main/resources/assets/create_nuclear_industry/ponder/shielded_assembly_placement.nbt`
- 同目录 `shielded_assembly_manufacture.nbt`、`shielded_assembly_sealing.nbt`
- `tools/ponder/shielded_assembly_scenes.py`
- `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`、`en_us.json`：各追加12键，原键值和顺序保持。
- `src/test/java/com/iksxh/create_nuclear_industry/ShieldedAssemblyPonderContractTest.java`
- 本 `IMPLEMENTATION.md`

双语和Java fallback同序同义，每幕3段正文。统一等待duration+20tick；本地Ponder1.0.82字节码确认正文实际生命周期为duration+10，因此后一段距前段完全淡出至少10tick。字幕/控件及镜头最终可读性等待客户端观察。

## 读取与API核对

实际读取本树AGENTS、文档入口、本卡/implementer-brief、治理5.1/5.2，及 `ShieldedAssembly{Block,BlockEntity,PartBlock,PartBlockEntity,Layout,Structure,State,Recipe,Renderer}`、两正式配方、`SpentFuelPayload`、已验收06场景/模板/测试。版本实物为MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。

实际使用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`（客户端临时世界与正式服务端边界）、`minecraft-testing/SKILL.md`（JUnit/真实NBT合同）、`minecraft-resource-pack/SKILL.md`（语言键、现有模型边界和打包引用），以及superpowers实施/TDD/验证技能。通用技能的全量重复、Git写和派代理步骤服从本卡及治理；只做本批定向red/green与唯一增量assemble，独立审查交PM安排。

## 原始证据与结果

证据目录：`build/reports/extension/DEVICE-PONDER-07-ASSEMBLY/`。

| 检查 | 实际结果及证据 |
| :--- | :--- |
| 实现前 `./gradlew.bat test --tests '*ShieldedAssemblyPonderContractTest'` | 退出1，实际3测试/3失败，缺入口/模板/场景；`01-red.log/.exit/.xml`。无环境中断，不把API探查或Python别名失败算合同red。 |
| Ponder正文生命周期 | `02-ponder-fade-api.txt`，构造器`duration+10`。`javap`不在PATH，使用已有JDK21绝对路径核对，未安装或修改环境。 |
| Python `-B tools/ponder/shielded_assembly_scenes.py` | 退出0，三份实际模板回读、66格完整布局/归属及确定性均通过；`03-templates.log/.exit`。系统Python应用别名重定向异常后使用工具返回的bundled Python，异常见`03-python-alias-failure.txt`。 |
| 最终 `./gradlew.bat test --tests '*ShieldedAssemblyPonderContractTest' assemble` | 退出0，3/3，0失败/错误/跳过，增量assemble成功；`04-final.log/.exit/.xml`。未clean/强制重跑/全量/GameTest。 |
| 源—JAR与范围检查 | `05-artifact-check.py/.log/.exit`、`05-artifact.json`：9实现文件SHA冻结；7个JAR入口与编译class/源资源逐字节一致，双语旧键顺序/值保持且中文12/12 fallback相符；8静态机体模型元素均位于各自合法格内。 |
| 差异与日志恢复 | `06-diff-check.log/.exit`、`06-final-status.txt`、`06-log-preservation.json`；本批跟踪差异检查退出0，新文件另核无尾随空白。保存本轮日志后恢复开工两份dirty日志原字节，三个既有pycache不清理。 |

最终JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，SHA256 `7bafdd14a47d940da34b42390b1a06084965bc1fdf374689f26c2e46a10143d4`；同名冻结副本在证据目录。三份模板SHA均为 `67f03765acb3d1e5d5273cdcec15ebe47892bb0438a98b0dfbd3ff9df77645f2`（布局相同，Java分别编排三幕）。

开工两份日志SHA均为 `1b5f6021658526a5ea13ea617c2ec5e39364be0e68d8ec7b7126f4995562c215`，原备份在 `original-logs/`，本轮日志在 `run-logs/`，交付再次按原字节比较。正式设备逻辑/能力/配方/配置/模型与其他故事板均未修改。没有API缺口或未决机制取舍。

## 停止位置

自动证据只覆盖合同、结构、编译、语言及打包；不能声明用户播放通过。提交候选给PM安排一次独立审查，再由用户观察完整机体/唯一底轴、四种材料入口和成品承接、配料与一次封装、镜头与字幕不重叠/裁切。本批不关闭美术02R1或03视觉门，不推进其他主线。
