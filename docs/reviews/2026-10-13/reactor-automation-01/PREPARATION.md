# REACTOR-AUTOMATION-01 开工来源

用户2026-10-13确认完整设计及实施计划，要求按既定方式开工。本批使用一名实现执行者和一次独立组合窄审；四个任务是依赖顺序，不分别重开实现或审查。

- 分支：`codex/reactor-automation-basic`。
- 同级目录：`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-automation`。
- 基线：main `0674b2f`，生产逻辑含已验收Sodium修复`0d2b238`；前置仅四份批准状态文档变更。
- 用户同级目录要求优先于原生工具的默认目录；该工具未提供指定目标目录参数，本批使用`git worktree add`建立，不混入旧美术/流量候选。
- 建树后Git工作区为空。主目录`docs/project.md`两处用户材料表编辑、模拟器/美术/日志及其他dirty未携带、未暂存。

## 存档与实际配置

复制前未检测到主工程Minecraft客户端Java进程；主`run/logs/latest.log`末段记录2026-10-13 07:33:27保存玩家、三个维度和全部区块成功。

主`E:/MyMC/NewMod/Create_NuclearIndustry/run/saves/新的世界`完整复制到候选`run/saves/新的世界`，源世界保留；目标原先不存在，没有覆盖或合并旧世界。复制后27个文件逐项大小/SHA256核对一致，原始清单保存在候选`build/reports/reactor-automation-01/save-copy-manifest.json`。不共用链接、不回写主世界、不纳入Git。

主`run/config`中的6份`create_nuclear_industry*.toml`复制到候选同路径：boiler、heat-exchanger、industrial-fluid、server、storage、turbine。SHA256记录在`carried-configs.json`；P1实际端口2048与用户热工设置保留。新管理员SERVER配置由实现注册并使用本批批准默认值，不覆盖原SERVER配置。世界内已有serverconfig随27文件一并复制。

## 边界

当前仅完成批准登记、工作树和运行来源准备，尚无本批功能实现、测试或构建结论。执行者按[实施计划](../../../superpowers/plans/2026-10-13-reactor-automation-01.md)的精确写集实施，只能写IMPLEMENTATION及原始证据，禁止Git写、转派或修改核心文档。原泵/现场流量/16³锅炉等人工门独立保留；本批交付后停管理员集中人工门，不提前合main。
