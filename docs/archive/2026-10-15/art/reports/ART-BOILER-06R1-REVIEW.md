# ART-BOILER-06R1 独立差异窄审

规格符合：通过。内部质量：通过。必改项：无。用户反馈“内部蒸汽太淡”属于整改触发，不能作为视觉验收通过。

## 范围与实际检查

实际读取06R1任务卡、CONSUMER报告、两源与原06 frozen的逐行差异、原始日志/退出码/两XML、R1 manifest、旧/新生产CPU JSON和对比SVG。沿用当前AGENTS、治理1.2/5.1及原06独立审查。实际应用同轮已读的minecraft-modding/testing/resource-pack技能（`C:/Users/IKSXH/.codex/skills/<技能名>/SKILL.md`），分别核客户端显示边界、真实定向断言及资源/透明混合复用；requesting-code-review/verification-before-completion按治理5.1复用证据。版本不变。本次仅只读差异/哈希核对并写此报告，未生成、跑测试/构建、启动游戏或Git写。

实际生产diff只有VisualState第43–44行中文注释与alpha曲线：`.18+.50*sqrt(clampedConcentration)`。原clamp保持：非有限数为0，有限值夹到0..1；零/负数/非法返回0，正值单调递增、上限.68。原外/内权重.7/.35保持，顶点理论最大.476/.238，约为旧满库存的3.78倍。原真实库存/服务器容量、14quad、采光、混色、暂停/缓存/共享接口无新增差异。

测试diff仅RendererTest第121行CPU输出目录06→06R1，全部断言与fixture保持；没有新增镜像测试或人为RED。旧06完整11项与材质/原生管线审查继续复用。

## 原始证据与绑定

`build/reports/art/ART-BOILER-06R1/consumer/junit-assemble.log`与exit文件实际为17秒、exit0；两XML各1项，合计2/2、零failure/error/skip，确为既有浓度/单调性用例及生产提交/CPU采样用例。此次没有重跑。

只读核R1两源与冻结副本、9相关编译class及其JAR entry均匹配；旧06其余专属源、40正式资源与共享依赖当前哈希保持。新JAR内54项复用依赖（12其他汽雾class、40资源、2 Controller class）保持；两XML哈希绑定一致。未扩大为全仓保护矩阵。

R1 manifest SHA256：`ea3f40a336fbbbb4e7f202917e3b590c5c9a633f4c58c3703e5e357a9fc53119`。build/libs与R1 frozen候选均为2587696字节、SHA256 `dae3321befc092903905ba42234eda769bf53cad3e5d1bc9f3ee22f64d73b245`。旧06冻结入口仍为`be680503a9f8e72c5240f0fa1ef19e920adbb77ebee5bf11412cc67c394a4bae`。

CPU新旧JSON逐字段只变化lowAlpha/highAlpha：22%从0.103011到0.414521，82%从0.166303到0.632769；48%超临界配比、phase、像素材质、14quad、片权重和真实汽域bounds一致。对比SVG明确同条件neutralLight=1、非GPU验收，支持加浓与浓度关系，不能证明透玻璃及夜间效果。

## 人工门

实际观察窗、夜间气室采光、GPU透明排序及最终是否足够可见仍待用户复看。原06管/罐/桶视觉门、05B及02R1独立人工门均不由本批关闭；不据内部窄审宣布合main。
