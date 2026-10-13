# Sodium联机连接纹理诊断暂停

此文件保留暂停时点的历史证据。用户随后交独立对话修复，并于2026-10-13向主PM确认手动测试通过；现行接续与故障关闭见[验收记录](ACCEPTANCE.md)，不再把下文暂停状态作为当前任务状态。

用户2026-10-13明确“先不管这个兼容性问题了，以后再说”。本项停止，执行者已随中断停下；没有生产代码、第三方JAR、用户客户端实例或世界修改，没有实现/测试/构建结果。

已得只读事实留供用户以后恢复：用户确认独立专用服务器，仪表显示已成型且各项功能正常、连接纹理始终缺失；客户端为`E:/MyMC/.minecraft/versions/现代战争与新时代工业整合包`，Sodium0.8.12及Iris1.8.14-beta.1实际加载。本模组客户端安装的`create_nuclear_industry-0.1.0_261012_2.jar`为已验收b0aef730…包。

当前`ReactorSurfaceSnapshots.capture(context)`只接受ClientLevel或精确RenderChunkRegion。实际Sodium嵌套mod含final `net.caffeinemc.mods.sodium.client.world.LevelSlice`，有private final ClientLevel `level`，没有公共getLevel；但具体Create模型消费调用链、最小安全可选接入和行为验证尚未完成，不登记根因已最终证实。

外层Sodium JAR的同版嵌套mod已只读提取至`build/reports/reactor-sodium-surface-01/sodium-mod.jar`，SHA256 `54695055628a28678b483e7ca5774cf0f298624e1ef223c381cd641c261bd1e7`。原仪表更新标签已包含ReactorSurface，锁定Create读取入口保留clientPacket=true；这些只作当前源码依据，不代替联机网络或GPU复现。

后续仅由用户恢复该任务时接续[诊断卡](../../../superpowers/plans/2026-10-13-reactor-sodium-surface-01.md)，不会由工业泵静止箭头和主目录配置窄修自动恢复兼容范围。
