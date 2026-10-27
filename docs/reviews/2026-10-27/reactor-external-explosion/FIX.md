# 同轮窄审整改：失败后的原生收尾与发送列表

2026-10-27。唯一整改点来自组合窄审：Mixin位于原生profiler.push("explosion_blocks")之后，失败时ci.cancel绕过profiler.pop；同时ServerLevel.explode在finalize后仍以Explosion.getToBlow()构造ClientboundExplodePacket，非空列表会让客户端误删服务端保留的方块。

最终只改原失败分支：移除Mixin的cancellable及ci.cancel，接管返回false时调用Explosion.clearToBlow()，继续原生空块循环/空掉落循环、profiler.pop及空火点循环；后续返回和拟发送列表同为空。ReactorExplosionLifecycle同步返回值注释。没有更改成功路径、危险阈值、玩家事务或事故Service。

测试仅给已有`multiOwnerRollbackAndUniqueCommit`追加失败返回Explosion非空且getToBlow().isEmpty()断言；原块/两个所有者快照/真实燃料保留、无孤立ledger及随后唯一多机提交断言继续执行。新增仅用于本轮的环境开关，开启时其余6函数各自打印SKIP并返回，默认仍执行原全部业务。

```powershell
$env:CNI_EXTERNAL_EXPLOSION_FAILURE_FIX_ONLY='1'
./gradlew.bat runGameTestServer -PgameTestNamespace=cni_reactor_external_explosion -PgameTestDirectory=run/reactor-external-explosion-failure-fix --console=plain
```

实际结果：1对应业务实跑成功、6显式SKIP；不是7个业务重新通过。`failure-finalization-fix.log`第128-131/213/215行记录6个SKIP，第211行记录失败批次返回affectedBlocks=0、第212行记录全批回滚后多块唯一提交断言通过。服务端正常保存、进程及Gradle exit0、26s；原始服日志另存`gametest-failure-finalization-fix-latest.log`。故障注入仍产生预期异常日志，未隐藏。

未重跑17JUnit及其余6业务，复用原集中实现未受影响部分的证据。原F29跟踪JAR仍3,677,228bytes/SHA256 F29D0F8B20646E577A273CF5B768FBF9E9244B30699F2EB28B923DD783FBB13C，没有运行jar或Git写。

最终`source-manifest.json`相对`source-manifest-before-finalization-fix.json`仅3个允许文件变化：ReactorExplosionFinalizationMixin.java、ReactorExplosionLifecycle.java、新本域ReactorExternalExplosionGameTests.java；其它8文件逐字节一致。git diff --check无错误，当前提交仍为基准b9bedf22a7a15b42ff4bd157ecc7abb556ededbe。交同一审查者仅复审此delta，尚未宣布验收。
