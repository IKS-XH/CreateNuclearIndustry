# 创造模式拆残骸不掉废料：修复候选

2026-10-22，用户完成流体熔融物手测后报告创造模式打掉残骸仍掉事故燃料废料。按[窄修卡](../../../superpowers/plans/2026-10-22-corium-creative-removal-01.md)处理，仅修改残骸创造拆除入口及既有域短测试，当前结果和剩余复看统一在[STATUS](../../../STATUS.md)。

来源`1985d1b3172a5efd1f0c87f3aa39ce3fe2a6feb7`，事故树`E:/MyMC/NewMod/Create_NuclearIndustry-accident`，仍为`codex/accident-thermal-04b`。原`onRemove`直接弹出库存，绕过原版创造模式不执行loot的分支；修复在最终取消/权限检查之后、实际移除之前清除创造拆除的唯一库存及热引用。生存及环境路径沿原逻辑回收实际一份，不改热、辐射或熔融物规则。

[修复JAR](/E:/MyMC/NewMod/Create_NuclearIndustry-accident/build/reports/corium-creative-removal-20261022/candidate-corium-creative-removal.jar)：3589179 bytes，SHA256 `75A26775A9BA80E6850BD7ED002EFB9895C2FD196296232C3192A7DC0E3051C3`。准确源唯一增量jar/2秒/exit0，原始输出同字节复制。全部783class、1700资源及manifest绑定构建输出；相比04B原包，仅`ReactorCoriumBlock.class`变化，其余2483项逐字节相同。本批GameTest域与JUnit仍排除；85个历史probe类按既有打包边界保持。见[BUILD](BUILD.md)、[身份](ARTIFACT.json)。

[执行证据](EXECUTION.md)保留真实RED（创造拆出1份，exit1）和集中GREEN（1业务通过＋17显式SKIP，Java/Gradle exit0、全部维度保存）。实际覆盖创造冷/HOT/空残骸零掉落、最终取消保留原库存及热引用、生存与环境移除各回收原具名一份、后续HOT索引撤销。[一次组合窄审](REVIEW.md)无未闭P1/P2；没有重复04B长时钟或封存矩阵。

原始日志/清单在上述事故树报告目录。PM独立复算候选及原始输出SHA，核旧81E备份后恢复事故树根跟踪JAR，事故树tracked干净；原04B包0F950及用户“新的世界”保持。主目录正式JAR本轮未写入；收尾只读发现其工作副本路径缺失，而日期名`build/libs/create_nuclear_industry-0.1.0_261022.jar`与EF3C同字节，保留这项外部未暂存变动及3D工具dirty，不替用户恢复或夹入提交。

沿原事故目录执行`.\gradlew.bat runClient`，或安装上方候选包。本轮只复看：创造模式打掉一块带真实废料的残骸，应无掉落；生存回收行为已做定向验证。用户此前完成的流体熔融物手测不要求重跑；未报告的物理卸载/真人多人不补记通过。本修复未合main，也不自动推进下一事故功能。
