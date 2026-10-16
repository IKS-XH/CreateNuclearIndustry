# THERMAL-SODIUM-01 客户端候选

2026-10-17，主逻辑PM交付记录；保留交付时语境，当前状态只见[STATUS](../../../STATUS.md)，后续见[合入记录](INTEGRATION.md)。依据为[局部修复卡](../../../archive/2026-10-17/plans/2026-10-17-boiler-turbine-sodium-01.md)，执行和证据见[实现报告](IMPLEMENTATION.md)、[一次组合窄审](REVIEW.md)。

## 根因与范围

锅炉快照入口和汽轮机外观资格入口仍只接受ClientLevel/RenderChunkRegion，缺少已验反应堆入口中的真实Sodium LevelSlice支持。汽轮机的中大型外壳atlas与轴向连窗共用这一资格判断，所以都会回退。当前实例Sodium0.8.12哈希与上次真实调用链相同，本次实际类夹具复现了确定缺陷；没有把它追认为现场唯一原因或归因Iris。

生产只改两处入口：锅炉仍核切片绑定世界与原子快照世界一致，汽轮机仍只用入参切片的局部状态。原生/未知/Ponder回退保留，无Sodium硬依赖，不改反应堆已验入口、服务器成型、热工、流体、库存、应力、资源源文件或协议。

## 来源与制品

- 独立树`E:/MyMC/NewMod/Create_NuclearIndustry-thermal-sodium`，分支`codex/thermal-sodium-surface`，基准`7d8fdaa`。
- 已提交来源`80153dd9f52bcd9139d4b5909abec666ff92641b`；恰两处生产文件、两类回归测试，[四项实物清单](SOURCE.json)已核身份。
- 交付前只读核用户实例现有`create_nuclear_industry-0.1.0_261216.jar`：3333619 bytes、SHA256 `A963B483260AB732B16B74FE0C2C625D8E58328E882BE675A0497C469B173CA3`，与本次baseline完全一致，不是辐射或贫化铀独立候选；执行者和PM均未修改该实例。
- 交付候选原路径`E:/MyMC/NewMod/Create_NuclearIndustry-thermal-sodium/build/reports/thermal-sodium-20261017/candidate/create_nuclear_industry-0.1.0_261017_thermal-sodium.jar`；同字节实物现见[正式JAR](../../../../build/libs/create_nuclear_industry-0.1.0.jar)：**3336385 bytes**，SHA256 `B366EB49F8994BB0213B676F530AE395E03A58F53E59CBA23AB9A77B682D7606`。
- 最终包按本卡补充裁定由已提交baseline原载荷＋一次assemble的9项目标class生成，[逐项制品证明](ARTIFACT.json)：2269项保持、7替换＋2新增class、0删除/越界；9项均与assemble相同，其余服务器/反应堆class、资源和metadata逐字节相同。没有新增第三方或测试类。
- 原始assemble包有Windows检出造成的1231项纯CRLF资源差异，分类和原包均保留；原始包不作为本次精确字节门通过的交付候选。最终链接特意指向`candidate/`内窄包，不是工作树`build/libs`或报告根目录的原始包。

## 验证事实

有效RED为8项中6项预期失败、0 errors/skipped；最终定向JUnit **32/32、0 failures/errors/skipped、exit0**，含新增8项及原反应堆Sodium3项。测试使用真实独立类加载器和实际LevelSlice局部数组，覆盖锅炉Create CT及final模型共享面、汽轮机中/大型atlas与OBJ连窗、原生/未知及旧世界边界。增量assemble仅一次、exit0；组合窄审未发现代码或制品整改项。

PM读取归档XML、exit、日志，核四项源码和三包长度/哈希；审查者另独立核最终ZIP全部实际载荷。没有重复构建/测试、启动服务器或把FakePlayer/隔离类测试当真人GPU验收。全部原始失败、警告、自动日志和夹具保存在候选`build/reports/thermal-sodium-20261017/`；本次源码提交没有夹带生成JAR或日志。

## 集中联机观察

本批是客户端入口修复，**只需更换客户端本模组JAR，服务器包可保持现状**。关闭客户端后用上方候选替换旧包，客户端只留一份本模组，保留当前Sodium/Iris并连接原服务器：

1. 锅炉已成型时外壳、观察窗恢复连接；中大型汽轮机外壳分块和轴向连窗正常，小型外观保持。
2. 在可安全拆装的机组上观察拆坏/补全刷新，退出重连及离开区块再返回均应一致。
3. 光影关闭/开启各复看一次，记录仍异常的具体机组和表现。

真实联机GPU外观尚未验，候选未合main、正式JAR未替换，也未自动安装用户实例。通过反馈后由PM按同一来源证据管理合入。

新树已按约定携带main保存退出世界及配置，27文件/13337924 bytes逐项哈希一致，未启动/修改副本或回写main；实际联机观察直接使用用户原服务器，不要求重建世界。
