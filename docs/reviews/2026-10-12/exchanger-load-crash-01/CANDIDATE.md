# 换热器加载崩溃：整改候选

用户原“新的世界”进入时的服务端崩溃定位在换热器弱引用表快照：客户端与服务端生命周期均访问同一无同步表，服务端List.copyOf时触发ConcurrentModificationException。本批仅修复客户端登记隔离和两表统一同步，世界/热回调仍在锁外，不改变热工、库存、负载或现有候选玩法。

修复源码提交 `5d01314c1af38ba4665a6b1af32ea355e49770fc`，保留在 `codex/reactor-variable-size`；独立规格/质量窄审无必改项。尚未合main，真实客户端故障门及快捷换料、可变尺寸、ART08/09各自人工门保持。

## 复看入口

关闭原客户端后，在同一候选目录重新启动并进入原“新的世界”：

```powershell
cd E:\MyMC\NewMod\Create_NuclearIndustry-art-integration
.\gradlew.bat runClient
```

先确认能正常进入，并让现有换热器/管路运行片刻；通过后再继续原[集中检查](../reactor-variable-size/CLIENT-CHECKLIST.md)。本批没有打开、搬动、修改或回写用户世界，也没有新建分支。Java已改变，需重启客户端加载。

## 实际自动证据与制品

- [执行报告](IMPLEMENTATION.md)保存原崩溃、有效行为RED与夹具失败区别；有效RED为9项中5失败，包括两个真实弱集合的同类CME。
- 最终定向JUnit30/30（新桥9、原热账本21），零失败/错误/跳过，exit0/16秒；原独立Basin真实服务端7/7、保存关闭后正常exit0/19秒；唯一增量assemble exit0/1秒。
- [独立窄审](REVIEW.md)实际读原日志/XML、侧边界/所有锁访问/回调及当前制品，未重跑测试。Unsafe世界夹具和8项默认缓存恢复只证明测试边界，不冒充真实客户端进入成功。
- 当前 `build/libs/create_nuclear_industry-0.1.0.jar` 为2678313字节、SHA256 `15c0ab893d080c6aa93c11f05e5e09ef55931979bd4fc8693ad502802db14ddd`；冻结副本 `build/reports/exchanger-load-crash-01/final-candidate.jar`。
- `build/reports/exchanger-load-crash-01/final-binding.json` SHA256 `428332b1b3d7d77a307bee47eb7e53435c8bedea3505d4f3c05355d23d893081`。PM实际核两源、打包桥class及原34项候选输入保持；审查逐条对比旧新JAR各2120条目，唯一改变是桥class，其余2119项相同。
- 旧组合JAR `896c2c38d984680eb61452d2a6ba5757e4a08e8931a2f19335acc5e26a57d6bf` 已保留在本批 `original-refueling-art09-combined.jar`；原各批冻结/失败证据不改写。

自动检查及提交没有关闭用户进入存档复看门；确认后由PM记录本故障验收，不代替其他人工项。
