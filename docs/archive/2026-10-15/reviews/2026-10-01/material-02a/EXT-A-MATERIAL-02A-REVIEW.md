# EXT-A-MATERIAL-02A 独立实现审查

候选基线：`0d38c99fe8d3e041c6148057fc4f62827b9e45b4`。本审查只读检查候选当前工作树、锁定资源、实际预览和既有报告；没有运行 Gradle、游戏、导出器或负向输入测试，没有执行 Git 写入。审查使用了 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack` 技能，结合锁定版本检查，不套用新版本示例。

**结论：**当前实现静态审查无功能阻塞，可交给用户做候选客户端手测；不能合入 main，用户的熔炼/压片/JEI/外观/保存重进人工门仍未完成。最终 265 项 JUnit 与 build 退出0、制品核对通过；141 项 GameTest 断言全部通过，但进程在 `Saving worlds` 后挂起并由 A 停止，GameTest Gradle 退出码为1，不能描述为干净退出或完整任务退出0。

## B/C 美术与资源证据

已实际查看 `tools/art-assets/preview.png`、`previews/items-1.png` 至 `items-3.png`、`EXT-ART-03A/comparison.png` 和 `EXT-ART-04/nugget-preview.png`。两板呈接近方形的硬边平板，与旧圆角长条区分；铅、锡色调彼此区分，且与钢板菱形及各自锭可辨。两粒的小尺寸轮廓仍可辨认。静态观感适合进入候选客户端手测，不替代游戏内视觉门。

计数按时点解释为：历史基线51张 + 先前两张板材 = C开工53张；C新增两粒后总计55张。正式报告 `EXT-ART-04.md:9-10,15-21,31-33` 及 `EXT-ART-04/verification.json:4-9` 分别记录51历史白名单、56条manifest（含非游戏候选）、正式验证前55张已存在、4张相对历史基线的新游戏图和最终55张。C开工53图快照逐项比对无差异；两板哈希与其证据相同。故 `new_game_count=4` 是相对51张历史基线的两板+两粒，不表示C阶段增加四张。

`EXT-ART-04.md:31-33` 记录正式验证退出0、六项非法输入按预期失败且失败不写、将忽略目录内快照暂时移出时正式验证仍通过并在之后还原；`verification-independence.json` 与 `pre-existing-snapshot-check.json` 支持此记录。此次只读核对既有证据，没有重跑资源检查。

## A 注册、配方、测试与隔离

`BasicMaterialContent.java:16-25` 注册锭、板、粒六种普通可堆叠物品；`CreateNuclearIndustry.java:47` 接入注册；`ModCreativeTabs.java:40-45` 展示六种身份。中英语言键和六个物品模型均已补齐。`c:ingots.json`、`c:plates.json`、`c:nuggets.json` 通过嵌套引用分别保留 leaf tags 的可扩展性；铅锡矿石、粉碎料、锭、板、粒 leaf tag 均是 `replace:false`。

配方逐项符合当前用户已批准方向：

- 保留粗铅/粗锡原有熔炉200 ticks、高炉100 ticks、经验0.7；普通与深层同金属矿石标签熔炉200/高炉100、经验0.7；Create 粉碎粗矿熔炉200/高炉100、经验0.1。
- 两种粉碎粗矿水洗各固定给9个对应金属粒，无附加产物；工作台9粒合1锭、1锭拆9粒，使用各自通用 leaf tag。
- 两种金属锭仍按原生 Create pressing 配方压成一板，没有固定 processing time。

相关生产文件可复核位置：铅锡矿石/粉碎料配方在 `src/main/resources/data/create_nuclear_industry/recipe/{smelting,blasting}/`；水洗在 `recipe/splashing/`；粒锭往返在 `recipe/crafting/materials/`；压板在 `recipe/pressing/`。当前实现没有移除用户已批准的 raw/ore 直熔。主工程计划卡旧合同仍写“不新增粉碎料直熔、洗矿、锭粒压缩”并描述总计六条配方（`docs/superpowers/plans/2026-10-01-ext-a-material-02.md:14-18`）；这是此前范围，须由 PM 按后续用户批准同步，不能用旧句反向拒绝当前实现。

新增 `ExtensionNativeMetalProcessingGameTests.java` 覆盖12种金属/形态/炉型的真实熔炉方块实体 tick，验证配方 id、时间、经验、输入输出及错金属/错形态；覆盖两项真实风扇有动力/无动力水洗、停机保料和9粒产出；也检查运行时水洗输出 tag、9粒合1锭/欠料与混料拒绝、1锭拆9粒。`green-gametest.log:222-227` 明确写出141项 required tests 全通过，然后立即开始停止与世界保存；`green-gametest-exit-code.txt` 为1，`green-java-stop.json` 记载PID 32348因保存阶段挂起、有限等待后停止。静态审查不把断言全过等同于运行器干净退出。

隔离 init 脚本 `EXT-A-MATERIAL-02A/material-02a-test.init.gradle:3-14` 将 GameTest 与 server 目录固定在本报告目录；`:16-24,28-39` 在执行前同时校验 run model/task 路径，且拒绝默认 `run/`。`run-sha256-comparison.json` 记录176项默认运行文件零变化，`run-saves-sha256-comparison.json` 记录68项存档零变化。最终 `final-test-build-exit-code.txt` 为0；`final-junit-count.json` 记录52个XML、265项、0失败/错误/跳过。`artifact-verification.json` 记录55张纹理、14条新配方，无缺失配方或字节差异，并记录最终JAR SHA-256 `A4382526AD6AF43A7A82A80992C2F33681D950F5DA1142AFC792108A66FD3A5A`。

| 严重性 | 位置 | 影响与建议 |
| --- | --- | --- |
| P2（文档同步） | `docs/superpowers/plans/2026-10-01-ext-a-material-02.md:14-18` | 旧合同与后来批准的水洗、金属粒/锭闭环及粗矿/矿石直熔冲突。PM 应在整合前同步当前已批准范围，确保后续执行者不会拿过期约束判断候选。此处审查者不修改核心文档。 |
| 无（实现阻塞） | 当前候选 A/B/C 写集 | 静态审查未发现实现阻塞。141项断言全通过，但GameTest进程保存阶段退出1应在最终交接中如实注明。 |

## 放行边界与排除项

**可交用户候选客户端手测：是。**现有 build、JUnit、GameTest断言及制品证据足以交付测试候选，同时说明 GameTest 保存阶段非零退出。

**可合入 main：否。**用户客户端人工验收尚未通过；不能以自动测试或静态资源预览替代人工门。计划卡范围同步属于 PM 文档工作，不由本审查者代改。

未运行客户端，未验收 JEI、存档重进或视觉最终效果；未验证第三方模组联调；没有重跑 Gradle、游戏、导出器或非法输入检查；未改任何生产文件、旧报告、存档或核心文档。此报告是独立审查，不是阶段验收。
