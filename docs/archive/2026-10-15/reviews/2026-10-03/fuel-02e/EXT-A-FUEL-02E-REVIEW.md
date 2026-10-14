# EXT-A-FUEL-02E 独立合并审查

日期：2026-10-03。审查范围为同级候选工作树的未提交 02E 源码、资源及已有证据；功能基线为 `3f7b207`，文档同步基线为 `5d24c3f`。本报告只提交审查意见，不代项目经理验收、修改任务状态或操作 Git。

## 结论

当前终版源码与资源未发现剩余的确定性阻断缺陷。曾发现的主控双掉落、错误工具吞库存、代理客户端归属未同步、代理工作灯未更新、活动 partial 东西向旋转反号、代理先加载后的孤儿清理缺口，均已在同一候选中整改并按下述证据复核。5 项隔离 GameTest 通过，最终增量 `assemble` 通过，38 项源文件与 JAR 字节核对一致。客户端四组人工门仍待实际执行，因此本报告不把自动证据写成客户端验收。

## 合同与源码复核

- 实际读取 `AGENTS.md`、治理协议 §5.1、02E 任务卡、已批准的八格方案和只读探针；应用 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`。按锁定 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82 与 Flywheel 1.0.6 核对，未套用技能中的异版本示例。本审查未运行 Gradle、GameTest 或客户端。
- `ShieldedAssemblyLayout` 的 `part=x+2*z+4*y` 与四朝向坐标一致。一个 `ShieldedAssemblyBlockEntity` 持有库存、工时和动力，七个代理只保存主控坐标/UUID；完整性逐格验证已加载状态、方块编号、朝向与归属，缺件时停机且能力处理器逐次失效。外露 16 个水平面共用 5 槽，4 个顶面只有 4 输入槽，底部、内部及 `null` 关闭。Create 搬迁检查已覆盖主控和代理。
- `BlockItem.place` 的锁定原版流程是在 `setPlacedBy` 返回后消耗一件。预检拒绝时不放置也不消耗；`setPlacedBy` 中途失败路径清除已归属的代理/主控，并向非创造玩家返还携带物，再由原版消耗原手持物。预检失败有 GameTest；中途失败补偿、外部权限变化等异常路径本轮仅按源码复核，未宣称有运行注入证据。
- 主控通用 `getDrops` 已改为空；玩家普通挖掘从 `playerWillDestroy` 唯一生成携物，`destroyBlock(true)`、直接替换由 `onRemove` 唯一生成携物，代理 loot 为空且归属清理只删除匹配分块。非创造玩家即使用错误工具成功拆除，也保留一件含原料/工时的机器。创造模式沿现有无掉落规则。旧 `expanded=false` 保持原位暂停、可手取/收回；重放时升级八格并沿用 `CniShieldedAssembly` 四料和工时。
- 代理更新包与区块初始更新 tag 已携带主控坐标/UUID，主控 `setOwnerId` 通过 `changed()` 发送状态。主控生产 tick 同步七个代理 `WORKING`。代理加载后定时复核归属，主控区块尚未加载则等待且不强载；主控已加载但无匹配归属时静默清理，不凭代理重建库存或物品。
- 静态模型为 8 个 part 的 off/on 路径，活动件为 `left_arm`、`right_arm`、`fixture`，仅主控 renderer 待机绘制，AABB 覆盖按朝向计算的八格。锁定 Flywheel `Affine.rotateCentered(float, Direction.UP)` 通过 JOML 正角四元数旋转；renderer 的 EAST 已改 `-π/2`、WEST `+π/2`，与布局的 EAST `(-z,x)` 一致。旧单格模型和旧 103 张 PNG 保留。
- 制造资源为 Create 原生 5×5 去四角的 `12L+6S+2D+1R→1`；旧 9 格工作台入口已删除。物品模型把整机和待机 partial 缩至 0..16，语言与 blockstate/模型路径齐全。已亲自查看北/东/南/西四向等距、前/背/顶/底、物品、三个独立 partial 与行程预览；未见确定遮挡、缺面或活动件重复烘焙。素材报告对全朝向、UV、碰撞与旧 PNG 字节作了静态核对；预览不代替游戏渲染。

## 已运行证据与边界

- `build/reports/extension/EXT-A-FUEL-02E/gametest-latest.log` 在 16:09:27 记录 `5 GAME TESTS COMPLETE` 和 `All 5 required tests passed`。当前五场景覆盖空间占用失败和四朝向、8 个拆点唯一携物及错误工具、16 侧/4 顶/内部/`null` 能力与模拟守恒、真实 Create 安山及黄铜漏斗投料和黄铜出货、底轴动力批次、机械臂代理模拟/真投料、过载/满输出、正式成品 `maxDamage=216000`、旧携物四料与 1234 工时重放、非法配方清工时保库存、代理不可用与孤儿延后清理。真实漏斗证据经过已放置的漏斗方块及 Create `FunnelBlock.tryInsert`/提取 tick；不能扩写为用户客户端手感通过。
- `build/reports/extension/EXT-A-FUEL-02E/assemble-final-console.txt` 记录 Java 21 下增量 `assemble` 退出码 0、`BUILD SUCCESSFUL`。运行交付报告如实记录本批共执行 4 次增量 `assemble`。另外，最初两次定向 GameTest 失败分别因测试早于 Create 漏斗 BE 初始化、动力断言晚于批次完成，修正后保留业务守恒断言。历史失败不以终版成功覆盖。
- `jar-byte-compare.txt` 实数 38 行，全部以 `OK` 标记，源文件与 JAR 字节一致；`jar-inventory.txt` 记录 16 个静态模型、3 个 partial、10 张新 PNG、21 格配方、双语言及 fixture，旧工作台配方不存在。终版 JAR SHA-256：`059116471CF42FF5267D4F2F2082F7858FD0562FB6A5AAA6028C1B07CBADC8D0`。
- GameTest 用未加载远处坐标和延后改指已加载空坐标核对代理等待/清理，没有实际卸载并重载跨区块整机。客户端初放/重进护目镜、漏斗安装与外观、21 格制造、保存后跨区块恢复和任意部位拆放仍属于 02E 任务卡的四组人工门，需由项目经理组织并记录实际结果。

## 审查定位

本轮将规格、源码、资源与已有运行证据合在这一份报告。审查中没有写源码、核心文档或 Git，也没有重复运行测试。`EXT-A-FUEL-02E-assets.md`、`EXT-A-FUEL-02E-runtime.md`、原始运行日志及终版 JAR 清单是本报告的证据来源；若之后再修改功能或资源，应按实际改动风险重新判断必要增量验证，不能沿用本次 JAR 哈希。
