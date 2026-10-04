# 汽轮机01C候选：核心先行与辅助包壳

**状态：2026-10-05候选实现、定向验证及独立复审完成，等待客户端手测。** [任务卡](../../../superpowers/plans/2026-10-05-ext-b-turbine-01c.md)与[已批准方案](../../../superpowers/plans/2026-10-05-turbine-assembly-experience-proposal.md)记录本轮范围。代码只在同级候选 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，主目录只同步文档，尚未合入main。

01B的自动通过未覆盖玩家逐块搭建体验，用户已指出散乱横板。只读调查还确认未成型模型/碰撞上下相反及独立板扳手合同遗漏；[AeroEngine调查](../turbine-assembly-study/REFERENCE.md)说明本轮借鉴的实际交互。

本次采用两端轴与转子列先定位、手持外壳逐块补壳/沿轴延伸、过程外观与完整运行状态分离。三档3×5×3、5×8×5、7×11×7，256RPM，接口位置、成本、吞吐、容量和双轴总SU均沿用已批准规则。

[一份合并复测清单](./manual-checklist.md)覆盖从空地搭建、缺件、运行、拆件和恢复。请关闭旧客户端后从候选目录重新运行 `./gradlew.bat runClient`；本次没有启动用户客户端或打开用户世界。小型走完整流程，中/大型重点核对截面和定位，不重跑旧热端/材料清单。

## 实际交付与验证

- 轴列定位与运行成型分开：缺壳时已有正确局部外观，完整结构才可处理蒸汽和输出动力。独立薄板按放置面显示，支持扳手改向，选取/碰撞同面。
- Create原生放置辅助逐块补环、沿轴延伸及补端盖；失败、受阻或布局歧义不扣料。控制器提示定位尺寸与缺件，并用Create原生高亮包标记服务端选定的位置。
- 最终JUnit **2/2**、隔离汽轮机GameTest **8/8**，编译与增量assemble通过，退出码均0。GameTest服务器正常保存关闭。覆盖实际方块物品交互的逐块包壳/扣料、歧义拒绝、诊断候选及既有运行生命周期；没有模拟完整客户端鼠标事件或验证画面。
- 资源生成检查覆盖8440条状态路径。PM将29项本批资源与最终JAR逐项比较SHA-256，**29/29字节一致**。最终JAR SHA-256：`C3D9AEE6DFC954D5678B1D1055F75EF11652819CA3AE8281C45965B0E831EBA7`。
- 独立复审原两项阻断及两处自查修正均已处理；复审没有重复运行测试。真实Create预览、高亮可见性、窗/手持渲染及玩家操作手感仍属人工门；到此暂停自动推进，不合main或开始冷凝。

## 留存证据

- [运行与测试报告](./evidence/EXT-B-TURBINE-01C-RUNTIME.md)、[冻结接口](./evidence/EXT-B-TURBINE-01C-RUNTIME-interface.md)、[资源报告](./evidence/EXT-B-TURBINE-01C-ASSETS-report.md)、[独立审查及最终复审](./evidence/EXT-B-TURBINE-01C-REVIEW.md)。审查首段是原始发现，最终状态见其末尾复审表。
- [最终GameTest日志](./evidence/EXT-B-TURBINE-01C-RUNTIME-gametest-final.log)、[JUnit结果](./evidence/TEST-com.iksxh.create_nuclear_industry.turbine.TurbineAssemblyLayoutTest.xml)、[资源字节核对](./evidence/EXT-B-TURBINE-01C-PM-resource-bytecheck.txt)。同目录保留最终编译/测试/构建日志及退出码。
- [小型五阶段离线预览](./evidence/turbine-01c-small-assembly-preview.png)、[中大型截面离线预览](./evidence/turbine-01c-tier-section-preview.png)。这些图由实际模型网格生成，不能代替客户端画面；“壳体几何齐全”图仍未安装全部接口，不代表已运行成型。

PM与执行者实际使用Minecraft modding/testing技能，资源执行者另用resource-pack；具体职责、模型分配与精简验证范围见任务卡。第三方参考仅借鉴经核实的搭建机制，没有复制AeroEngine代码或素材。
