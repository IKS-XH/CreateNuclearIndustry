# 反应堆Sodium联机连接纹理窄修

状态：用户2026-10-13将本问题交独立对话处理后，向主PM明确反馈“这个渲染问题已经解决了，我手动测试也没问题了”。本故障的用户人工验收通过；旧暂停段为历史时点。主PM接收原位未提交交付，复用同源自动和候选证据，补齐原要求的一次独立窄审后精确提交，不再次实现、测试或构建，不由此关闭其他故障或推进其他主线。

2026-10-13用户报告：独立专用服务器上的反应堆护目镜显示已成型、功能正常，但客户端一直显示搭建时的分离纹理。用户确认客户端安装Sodium/Iris。基线main `6d1baba`，工业泵R2已验收，其素材与原验收保持。

目标是修复现行版本在实际客户端渲染路径下的成型连接显示，保持服务端权威成型、运行、库存、尺寸和已有材质。当前发现L1模型快照仅接受真实ClientLevel或原生精确RenderChunkRegion，其余上下文返回空；这可能使Sodium模型构建一直降级，但需实际依赖调用链与行为证据确认，不能只凭用户装了Sodium认定根因。

主目录原位窄修，不建分支或搬存档、不写用户客户端/服务器实例或世界。PM只写任务/治理/验收文档并管理Git，唯一执行者实施，禁止Git写或转派；稳定后PM派一次独立窄审。实际使用minecraft-modding、minecraft-testing、systematic-debugging，维持1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6；实际用户客户端依赖只读核对，不升级工程技术栈。

## 先诊断、再精确授权

执行者先只读当前仪表BE更新包→L1事件/不可变快照→Create CT模型路径，同时审计用户实际Sodium/Iris JAR中的模型构建上下文、所属ClientLevel及线程生命周期。当前只读找到实例`E:/MyMC/.minecraft/versions/现代战争与新时代工业整合包`，Sodium外层0.8.12 JAR含同版嵌套mod JAR，Iris1.8.14-beta.1；该实例是否为本次客户端待用户核实。不能修改第三方JAR或将它打入本模组。

定位阶段只允许写`docs/reviews/2026-10-13/reactor-sodium-surface-01/DIAGNOSIS.md`、`IMPLEMENTATION.md`以及`build/reports/reactor-sodium-surface-01/**`。可提取锁定依赖、保存javap/调用链/来源哈希和诊断夹具；无生产代码修改权限。报告实际模型调用的context类型、可靠世界访问入口、旧世界隔离、无Sodium专用服安全、最少可选接入路径和必要失败验证建议，随后PM扩精确写集。

## 不变量与验证边界

- 模型任务只能使用它所属的真实世界会话，不用Minecraft当前世界替代上下文证据，不放宽为任意包装/光照委托或仅匹配方块坐标；旧世界、未知上下文、Ponder仍降级。
- 不在模型线程查询仪表BE、扫描结构、强制取区块或发送网络请求；只读已发布不可变快照。
- 可选渲染接入不能要求专用服或无Sodium客户端加载Sodium类；不为了客户端故障改成型规则、包格式/版本、材质或已验收设备。
- 先以实际上下文路径做RED，修复后核同世界可连接、旧世界/未知/Ponder不可借用、原生入口保持；必要时一个无可选模组真实专用服启动探针。不以纯模拟状态机通过冒称真实Sodium路径覆盖。
- 定向测试和一次必要增量assemble；一次独立窄审，复用原L1/L2/CT及泵/管网证据。不上全量build、兼容存档研究或各设备复测；有具体失败/新风险才追加必要检查。
- 最终交main可安装候选和具体联机复看入口；用户客户端Sodium/Iris下真实外观是独立人工门，不以自动证据关闭。原JAR/用户实例/世界与其他dirty保持，未报告其他问题不扩范围。

## 独立对话交回与验收接续

独立执行者从main `4da5e94`完成[诊断](../../reviews/2026-10-13/reactor-sodium-surface-01/DIAGNOSIS.md)和[实施交付](../../reviews/2026-10-13/reactor-sodium-surface-01/IMPLEMENTATION.md)。最终精确源码为原 `ReactorSurfaceSnapshots.java` 与新增 `ReactorSodiumSurfaceTest.java`；生产仅适配已核实的 final Sodium LevelSlice 世界身份，不改服务端、材质、协议或生命周期。执行者没有Git写或派审。

主PM只读核对原26/26、无Sodium真实专用服1/1、唯一实际增量assemble及来源/JAR身份；一次独立审查者仅写 `docs/reviews/2026-10-13/reactor-sodium-surface-01/REVIEW.md`，禁止实现、再次测试/构建、Git写或转派。审核后仅净提交两源码及本批报告/治理文档，保持用户包、世界与其他dirty。候选 `create_nuclear_industry-0.1.0_261013_sodium-surface.jar` 的SHA256为 `8c03dbd11e4812705b18942407ae4dd64aba8e7c0cbbc18d03ab2690a0ac1f23`；不为改文件名重复打包或覆盖用户正式包。

用户本次反馈仅登记原反应堆成型连接纹理故障通过，不补写光影切换、世界隔离或全部设备优化模组兼容的逐项人工结果。实际使用minecraft-modding/testing、requesting-code-review及verification-before-completion；沿用已核锁定工程版本和治理5.1证据复用规则。

接续结果：[独立窄审](../../reviews/2026-10-13/reactor-sodium-surface-01/REVIEW.md)无剩余必改项，当前两源码/六class与8c03dbd…候选绑定一致，原自动证据复用；[用户验收](../../reviews/2026-10-13/reactor-sodium-surface-01/ACCEPTANCE.md)关闭原故障，源码及本批文档净提交main，不再重复构建或覆盖正式/用户包。本项完成，其他独立门保持。
