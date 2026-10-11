# ART-BOILER-06-CAPACITY-01 独立差异窄审

2026-10-11。**结论：通过；无必改项或阻断交付的合同遗漏，可由 PM 按本卡做最小净同步。** 本结论仅针对已有汽容量读数的公开入口，不合入或验收美术05B/06候选，不关闭用户锅炉视觉门。

独立审查目录为 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`，实际 HEAD 为 `3eb88c9e61214fc812411339182334512509e3ac`。已读取本树 AGENTS、活动计划、`docs/art/ART-BOILER-06-CAPACITY-INTERFACE.md`、实施报告、治理5.1/5.2、原L1 HANDOFF，以及实际 `minecraft-modding` / `minecraft-testing` 技能；版本核对为 MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。仅写本报告，未改生产/测试/构建/核心文档，未执行 Git 写、派发、测试/构建重跑或启动用户世界。

## 差异、ABI与消费边界

源码路径为 `src/main/java/com/iksxh/create_nuclear_industry/boiler/BoilerControllerBlockEntity.java`。

- **唯一改动为六行 getter/Javadoc，位置68-73。** 独立从新源码删除该精确六行块后，重新计算字节 SHA256 为 `cbc83e98cd8bca3b6d60f71e5e9348d7e1c9a48fe27818127c4a20b236622b21`，与 PM 编译前原 L1 源备份完全一致。字段/初始化、原write/read、网络、ledger、L1、生命周期及服务端结算逻辑没有变化。
- **实际 ABI 和实现符合合同。** Java21 `javap -public` 与新冻结 `compiled-abi.txt` 一致，明确存在 `public int syncedSteamCapacityMb();`；实际 `javap -c -p` 为 `aload_0 → getfield steamCapacityView:I → ireturn`。无重算、状态写入、扫描、包发送或客户端类型引用。
- **单位与权威边界准确。** 第69-71行中文 Javadoc 明确仅作客户端显示、单位 mB、首包前值不是同步/成型证明、完整包读取后执行 L1 门控及容量为正。第48行仍初始化为 `BoilerState.CAPACITY`，而 `BoilerState.java:12` 为18000；服务端权威仍是原 ledger，不以显示字段进行模拟。
- **原包语义保持。** 第374行仍从权威 `ledger.steamCapacity()` 写入 `SteamCapacity`，第389行仍直接使用 `tag.getInt("SteamCapacity")`。已只读反汇编锁定 `build/moddev/artifacts/neoforge-21.1.219.jar` 的 CompoundTag，确认缺失/非数值返回0，数值沿原 NumericTag 的 getAsInt；getter 不兜底、不修正原值，非正数不可作分母。
- **完整读取边界不能省略。** 原 `read` 第385-386行发布 L1 通知早于第389行容量赋值，本批保持原顺序。因此仅收到该通知或看到正的18000，均不能证明这次容量已经读完；消费必须等整次原 clientPacket 读取完成，核当前真实 owner/同世界、有效 descriptor 和当前 L1 的同 generation/revision/min/max/partition，再一次读取两个真实库存及容量。此义务已写入合同和 Javadoc，消费者属于后续获准美术任务，不在本 getter 卡追加同步或首包 flag。

## 冻结证据

新证据根为 `build/reports/art/ART-BOILER-06-CAPACITY-01/`。

| 项目 | 独立核对结果 |
| --- | --- |
| 唯一增量构建 | `01-jar.log` 为 `jar` 增量编译/打包，3秒成功，`01-jar.exit` 为0；保留四项已有NeoForge弃用警告。本审查未再次运行Gradle。 |
| 当前一源 | 实测 SHA256 `843abb4ec1a3fbe5517db4f248cf5505582e90b9f5baad1d5b5078f15e48c796`，与 manifest 一致。 |
| 两个 BE class | 控制器及其 Port 编译 class 与 manifest、JAR 内对应字节逐项一致，无差异。 |
| 实际 ABI/字节码 | 独立 javap 公开 ABI 与冻结内容零差异；实际 getter 指令、原 getInt 缺失值行为与对应冻结字节码一致，javap退出0。 |
| 原 L1 备份 | `previous-l1/` 的原 controller 源、两个class及原JAR共4件实测SHA均与新 manifest 和原L1冻结值一致。旧 manifest 仍保留原实施HEAD和原JAR身份，未重建历史矩阵。 |
| 新 JAR | `build/libs/create_nuclear_industry-0.1.0.jar` 实测 SHA256 **`9071b937ad82970186fad6bfc5e3aa8d2efda3702af11956c7307599136b9f58`**，与新 manifest 和实施报告一致。 |

按低影响只读 getter 合同与治理5.1，复用原L1的29/29定向、1/1真实控制器探针及已完成独立窄审，参见 `docs/reviews/2026-10-11/boiler-surface-display-01/HANDOFF.md` / `REVIEW.md`。没有新增镜像 getter 测试、重跑原探针或扩大资产矩阵；原日志/cache及用户存档保持。后续美术消费及用户视觉验收独立保留。
