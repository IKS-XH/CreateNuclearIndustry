# HEAT-PENDING-01 三次专用服崩溃诊断

2026-10-17，主逻辑PM，只读核输入和当前源码。用户要求“查一下是什么原因导致的，怎么修复”；具体改动按[归档窄修卡](../../../archive/2026-10-17/plans/2026-10-17-heat-bridge-pending-01.md)，当前状态只见[STATUS](../../../STATUS.md)。原始报告保留于用户指定目录，没有修改、上传或复制玩家/世界详情到本记录。

## 三份输入的共同事实

| 文件（E:/MyMC/cn_crash_20261017/） | 报告时间 | SHA256 |
| --- | --- | --- |
| crash-2026-10-17_175251-server.txt | 17:52:51 | F9DE166B3034E6D520CCFF305F354DC89FE52810606C1DA0E6C1D2FE40D7FCB1 |
| crash-2026-10-17_175536-server.txt | 17:55:36 | 37415B0E9E13698F8B42932C635C07AA4AAD85002FE7A0A115965424D29FDEE7 |
| crash-2026-10-17_175944-server.txt | 17:59:44 | BF6479FD4827D8E0C44CC23984EBDB29C6BBBF96C43A37ADAF4ACABA7D406AFD |

三次均为`Exception in server tick loop`，同一异常栈：

```text
java.util.ConcurrentModificationException
HashMap$HashIterator.nextNode
HashMap$KeyIterator.next
Iterable.forEach
HeatExchangerBoilerBridge.lambda$afterServerTick$2:155
WeakHashMap.forEach
HeatExchangerBoilerBridge.afterServerTick:155
NeoForge EventBus / ServerTickEvent.Post
```

现场Minecraft1.21.1、Java21.0.2、NeoForge21.1.235、Create6.0.10、本模组0.1.0（文件名后缀261216）。这是直接终止服务端tick的集合迭代异常，报告没有以OutOfMemoryError或watchdog超时结束。报告顶部Sable的通用提示不是造成这条异常的证据。

## 源码中的确定缺口

在生产来源`eb1ec32`，`HeatExchangerBoilerBridge`用`WeakHashMap<Level, Set<BlockPos>> PENDING`保存加载后待重算的Create储罐控制器；每个位置集合为HashSet。

- `chunkLoaded`第136～142行：加载区块内有原版Create储罐时，直接computeIfAbsent/add，没有与读取端共享保护。
- `afterServerTick`第153～156行：在两层实时集合的forEach中调用refresh，最后整体clear，没有快照/原子交接。
- `refresh`会读取实际世界/方块、调用Create公开`BoilerHeater.findHeat`及控制器notifyUpdate；Create源码确认findHeat会执行注册热源的回调。回调链有机会触发额外加载登记，源集合却仍在遍历。

三份栈顶的HashMap.KeyIterator对应内部HashSet遍历，证明位置集合在遍历期间发生结构修改。日志没有记录写入方的线程和调用栈，因此只能确定队列访问不安全，不能断言一定是异步区块线程、某个Sable补丁或某个玩家操作。ConcurrentModificationException也可以由同线程回调重入触发。

此外，即使某次变更碰巧没抛异常，刷新期间新增的请求仍可能被末尾clear擦掉。已有ACTIVE/LOADED两表的锁与强引用快照没有覆盖PENDING，现有测试也主要覆盖那两表。

## 最小修复方案

将登记和“取出本轮批次＋清理原队列”放在同一短锁内，取得独立位置快照后释放锁，再逐个刷新。期间的新登记进入下一轮队列，不能修改本轮遍历对象或被本轮结束清空。锁内不做世界、区块或热源回调；保留去重、弱世界引用、不强载和实际热工算法。

仅换成ConcurrentHashMap、吞掉异常或在回调结束后继续全clear不能同时解决重入、安全交接和丢请求。本次不要求卸载Sable、改Java参数、升级NeoForge或删除设备/存档；精确外部触发者仍需额外运行记录才能归因。

执行验证将使用真实生产事件/队列的可控重入与并发登记，先取得有效失败，再核新增请求次tick处理、不丢、去重、跨世界隔离和回调不持锁。自动证据与原整合包多人持续观察分开记录，不能以局部夹具承诺所有其他服务器问题都已解决。

## 派发与工作区

唯一执行者`/root/heat_pending_impl`，gpt-6.1-sol/high；跨回调的原子交接及实际生产事件回归需要完整生命周期判断。复用干净同级树`Create_NuclearIndustry-render-accepted-integration`，新分支`codex/heat-bridge-pending-fix`从`b8019db`开始（相对eb1ec32仅本次管理文档）。main已保存全部维度且session.lock可独占只读打开，完整携带世界和配置，27文件/13337924 bytes、0哈希差异；目标此前不存在，没有混合/覆盖。

仅允许桥接类与其既有测试，报告在候选build/reports/heat-pending-20261017；不修改原始崩溃目录、用户实例/服务器或世界。实现与一次组合窄审完成后交服务器观察候选，不自动安装生产服、合main或替换正式包。当前其他人工门不变。
