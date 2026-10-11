# ART-BOILER-06-CAPACITY-01 实施交付

2026-10-11，执行者未提交候选。唯一工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`，实际基线 `3eb88c9e61214fc812411339182334512509e3ac`。唯一生产改动为 `BoilerControllerBlockEntity.java` 新增六行中文 Javadoc 与 `public int syncedSteamCapacityMb()`，方法纯返回既有 `steamCapacityView`。

已核源差异：从新源码逐字删除这六行后与 PM 保存的原 L1 源文件完全一致。既有字段、18000 mB 初始化值、write/read、L1、ledger 与其他运行逻辑均未改变；未修改测试、构建、美术资源或消费者，未执行 Git 写操作。日志与 `tools/ponder/__pycache__/` dirty 保留；沿用已携带存档，未启动或复制世界。

唯一 `.\gradlew.bat jar --console=plain` 增量构建成功，3 秒、exit 0。`javap -public` 实物签名为 `public int syncedSteamCapacityMb();`；`javap -c -p` 实际方法仅 `aload_0 → getfield steamCapacityView:I → ireturn`，不重算容量、不写状态、不扫描或引用客户端类型。原有四项 NeoForge 弃用警告保留。

显示边界已核：原字段初始化 `BoilerState.CAPACITY=18000` 不是首包或成型证据；原 `read(clientPacket)` 使用 `tag.getInt("SteamCapacity")`，锁定 CompoundTag 字节码证实缺失/非数值返回 0，数值按原 NumericTag 读取。getter 不包装或修正原始值，非正容量不得用作分母。消费须在整次原 clientPacket 读取完成后核当前真实 owner、有效 descriptor 与当前 L1 的同世界/generation/revision/min/max/partition，并确认容量为正；服务端权威仍为 ledger。新 Javadoc 明确这些边界，消费者实现留给获准的美术06任务。

原始证据只在 `build/reports/art/ART-BOILER-06-CAPACITY-01/`：

- `01-jar.log/.exit`：唯一构建原始输出与退出码。
- `compiled-abi.txt`、`controller-bytecode.txt`、`getter-bytecode.txt`：真实公开 ABI 和方法指令。
- `compound-tag-bytecode.txt`、`get-int-bytecode.txt`：既有包读数的缺失值行为。
- `manifest.json`、`controller.diff`、`scope-status.txt`：一源、两个 BE class 与对应 JAR SHA 绑定和精确差异；PM 原 `previous-l1/` 只读核实，未覆盖历史文件或旧 manifest。

新源码 SHA256：`843abb4ec1a3fbe5517db4f248cf5505582e90b9f5baad1d5b5078f15e48c796`。新 JAR SHA256：`9071b937ad82970186fad6bfc5e3aa8d2efda3702af11956c7307599136b9f58`。两个控制器 class 均与 JAR 内对应字节一致。

沿用本会话实际读取的 `minecraft-modding`、`minecraft-testing` 与实施/验证技能，并重新核对 AGENTS、当前卡/合同、治理5.1/5.2及 L1 HANDOFF；栈保持 MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。按本卡与低影响只读 getter 的验证规则，复用 L1 原29/29、1/1及独立窄审，不写镜像测试或重跑 JUnit/GameTest/客户端。等待本次独立差异窄审及 PM 净同步；不合入05B/06美术候选，不关闭视觉门。
