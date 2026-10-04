# EXT-B-EXCHANGER-01E：高压锅炉供热状态翻译

状态：语言资源修复、增量打包及PM独立资源审查通过，待客户端确认；候选`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，基线`dd3df15`。不据本次反馈将锅炉01C/01D人工门记为通过。

## 范围与根因

`HeatExchangerState`在专用锅炉热液转换时返回`dedicated`；护目镜按`gui.create_nuclear_industry.heat_exchanger.state.`拼接翻译键。现行中英文文件缺少`dedicated`，与用户截图裸键一致。补中文“向高压锅炉供热”及对应英文；不改供热算法、其他读数或布局。

## 执行合同

- 先读AGENTS、治理§5.1、当前锅炉01D任务及本卡；实际使用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md`。核对MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280，不升级。
- 允许写集仅`src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`与`en_us.json`的相关状态键。保留排序/其他翻译，不重排文件。不改功能/测试/构建脚本、docs、日志、世界、缓存；禁止Git写和转派。
- 执行者独占本批构建：核对换热器可达状态中英文键覆盖、JSON解析与重复键；运行一次增量`assemble`，确认输出资源及JAR包含新键与值。仅语言资源，不新增自动化测试、不跑JUnit/GameTest，热工及管线复用既有证据。
- 若发现缺失不限于语言键或需要修改算法，先报告，不扩大范围。交付简短报告仅`build/reports/extension/EXT-B-EXCHANGER-01E.md`及同前缀证据目录，列实际技能与运行结果。
- PM独立审查资源差异与打包证据，记录待用户在相同护目镜场景确认中文；不推进下批主线。

## 交付

执行者`/root/exchanger01e_lang`使用高速模型，仅在两份语言JSON各补一行。中英文全部9个换热器状态键覆盖、JSON解析/重复键检查通过，一次增量assemble成功；未跑JUnit/GameTest。PM独立查看两行差异并读取JAR及`build/resources/main`确认新键和值已打包。[交付与验证记录](../../reviews/2026-10-04/exchanger-01e/EXT-B-EXCHANGER-01E.md)已归档。

客户端仅需在候选工程运行的游戏内按F3＋T重载资源（或关闭后从候选目录重新runClient），再次观察给高压锅炉供热的换热器护目镜提示，应显示“向高压锅炉供热”。此前锅炉端口人工门仍待确认。
