# 高压锅炉最大尺寸16³：客户端候选

2026-10-15更新：用户已确认锅炉尺寸上限与新外观手测通过，本批人工门关闭，尺寸增量已接续main集成。见[本次验收](../../2026-10-15/boiler-size-accepted-integration/ACCEPTANCE.md)。以下候选身份、未合main和待测文字保留交付当时语境，不作为当前状态。

2026-10-12，主PM完成来源、原始日志/XML和冻结制品核对；一次独立规格/质量窄审无必改项。功能提交 `30020e469ec8719560a33833081979c8f7225daf` 留在 `codex/reactor-variable-size`，未合main。反应堆/快捷换料/窗口与控件的上一批已验收并合main，本批只保留锅炉新增尺寸的人工门。

## 启动与存档

候选目录 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`：

```powershell
cd E:\MyMC\NewMod\Create_NuclearIndustry-art-integration
.\gradlew.bat runClient
```

用户已明确本批继续用当前候选测试存档，保留 `run/saves/` 中已保存的测试进度。本批没有新建/切换分支，也没有复制、覆盖或回写世界；主目录原件与旧备份保持。实现/审查报告中“存档来源待答复”表示报告形成时点，以本页和计划末尾的用户答复为当前状态。

`run/config/create_nuclear_industry-boiler.toml` 的 `dimensionRange` 已由 `[5,11]` 改为 `[5,16]`。原文件完整备份在 `build/reports/boiler-size-limit-01/boiler-config-before.toml`，仅一字节改变，其他键不变。当前配置SHA256 `dbe002f800ae2db1e759a2952b5f9307db5a136b9fcd6b2e5ff29c9dd9795b58`；这是实例配置身份，尚不把它当客户端已加载的证明。若世界使用自己同名覆盖文件，以实际服务器配置为准，不改动用户世界。

## 改动与证据

- 默认和配置合法区间均为5≤min≤max≤16；三边分别判断，允许偶数、长方体及跨区块，不要求区块对齐。
- 分区容量、隔层/端口/换热段规则、温压、双汽库存、每口流量和热工保持。16³隔层localY=8时水1372格、汽1176格，默认容量2744000mB/2352000mB；其他隔层高度仍按实际格数计算。
- 原29/29定向JUnit通过；最终真实服务端3/3通过并自然保存退出0，包括16³跨X/Z区块、实际Form/真实端口容量、不增加强载或工质/HU、17拒绝，以及原11格偏心参考。原2/2和初次assemble保留为中间证据；补旧12拒绝断言后第二次必要增量assemble退出0，没有重跑原29项或全量矩阵。
- 最终JAR `build/libs/create_nuclear_industry-0.1.0.jar` 为2682849字节，SHA256 `72f2bb2b197b1441d7f72c1e0f8e43fd9114fcbf3f47c71323ad44aee003227d`，与冻结 `build/reports/boiler-size-limit-01/boiler-size-limit-candidate.jar` 一致。原接受包15c0ab89留作备份；旧1925条目1921逐字相同，仅三个生产class及旧GameTest class变化，新增本批GameTest class和空模板。六项来源及build/JAR绑定一致。
- [实现报告](IMPLEMENTATION.md)、[独立窄审](REVIEW.md)、[本批人工检查单](CLIENT-CHECKLIST.md)。源代码默认、运行配置、自动验证与人工结果分别记录，不互相替代。

## 当前门槛

只等待本批锅炉16³成型、超过16拒绝及实际尺寸/分区容量的客户端检查。通过前不合main，不推进其他工程主线，不重复原锅炉生产线或已通过反应堆/美术测试。主目录仍保留已验收锅炉的旧默认[5,11]代码，源码状态由本候选单独交付。
