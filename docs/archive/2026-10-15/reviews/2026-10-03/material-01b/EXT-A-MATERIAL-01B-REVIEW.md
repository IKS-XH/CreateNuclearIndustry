# EXT-A-MATERIAL-01B 独立审查

**审查结论：通过，未发现需整改的实现问题。** 本次只读核对候选源码、实施报告、JUnit XML、隔离 GameTest 日志和最终 JAR；没有重跑测试、Gradle或生成器，也没有执行 Git 写操作。

## 合同与实现核对

- `ModItems.LAPIS_DUST` 是普通可堆叠物品并加入创造页；中英文名称正确。两条配方与确认方案一致：青金石标签 1:1 制粉、`processing_time=100`；三种具体粉末各1份和1000mB水，经无热搅拌产出1000mB既有冷态 `compound_coolant`，同为100。未见新流体、桶、机器、GUI或热/污染路线。
- `c:dusts/lapis` 与父 `c:dusts` 均为追加；磨石蓝色染料配方保留。`P1DataContractTest` 只在组件装配与冷却剂两条精确路径分别放行对应ID；既有热冷却剂、污染物、净化器及其他P1 ID仍受禁令检查。
- 游戏贴图直接接入已认可的 `lapis_dust.svg` 生成PNG；游戏PNG与工具生成PNG SHA-256均为 `7011BC285C26C756D40AE7087B47022C3C9FD5002EC03F349A67E15D1475CE09`。素材工具清单保留 `game:null` 候选语义；实施报告记录113张旧PNG保持不变。
- 外部等价标签fixture只存在于隔离 GameTest 世界与交付证据目录，内容为 `replace:false` 的 `minecraft:flint` 成员，没有进入发布资源。探针按禁用、重载、启用、重载、匹配验证、禁用、重载恢复的顺序执行；源码使用延迟回调推进，没有等待或阻塞调用。

## 证据

- `build/reports/extension/EXT-A-MATERIAL-01B.md` 记录技能使用、候选基线和本批实现边界。
- `build/reports/extension/EXT-A-MATERIAL-01B/evidence/p1-data-contract-results.xml`：`P1DataContractTest` 5项通过，0失败、0错误、0跳过。
- `build/reports/extension/EXT-A-MATERIAL-01B/gametest-final.log`：锁定环境下6/6 required GameTests通过，服务端正常保存退出；其中包括真实粉碎轮1:1、磨石蓝色染料、无热搅拌守恒、满输出受阻与恢复，以及外部标签重载探针。副本 `evidence/gametest-server-latest.log` 含 `COOLANT_EXTERNAL_TAG_SIMULATION` 标记，并记录fixture启停和三次数据包重载。
- `build/reports/extension/EXT-A-MATERIAL-01B/assemble-final.log`：最终增量 `assemble` 成功。复核JAR SHA-256为 `67A2F0F39278DCD5B27F88ECC4A1B61FEF20CFF7BFCCCEDD3C921E272F6D1880`；包内可见青金石粉模型/贴图、细分及父标签、两条配方，以及既有冷却剂桶模型和冷/热流体贴图。
- 执行过程中出现过的测试源码API编译错误已在最终运行前修正；最终源码编译、GameTest和assemble均成功，因此不作为当前问题。

## 保留门槛

自动证据不能替代任务卡上的用户客户端验收。JEI与名称/图标、实际粉碎与磨石表现、无加热制液及现有桶/管道取液仍待人工确认。根目录 `logs/debug.log` 与 `logs/latest.log` 曾被本批运行追加；项目经理已按既定方式保存并恢复，本审查未修改它们。

审查中实际应用 `minecraft-modding`、`minecraft-testing` 和 `minecraft-resource-pack` 技能，并按治理第5.1节复用最终执行者证据，没有重复启动测试。

## 交付后定点复核

原审查遗漏了素材工具清单的兼容合同，并误写为将 `lapis_dust` 映射到游戏资源路径。项目经理发现：`tools/art-assets/pipeline.py::load_manifest` 固定要求83项清单含82个游戏路径和唯一的lapis候选；把候选 `game:null` 改成游戏路径会令默认清单校验失败。实施者已恢复 `tools/art-assets/manifest.json` 中该候选的两个历史字段。只读比较确认当前清单与 `b61fae5` 完全一致：83项、82个游戏路径、1个lapis候选，候选用途为“已批准候选，本批不新增注册和游戏PNG”；代码中的 `load_manifest` 数量/集合校验与该状态相符。更新实施报告记录捆绑Python调用 `load_manifest()` 成功、返回83/82及 `game=None`。修正仅涉及manifest，两项状态差异以外无实现改动；该次报告亦记录源码与JAR未变。本复核没有运行Gradle、测试或素材命令。