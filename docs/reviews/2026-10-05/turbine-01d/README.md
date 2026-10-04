# 汽轮机01D：外观、端口方向与碰撞修复候选

**状态：01D六项候选已交付，用户随后反馈成型端口未密封；R1修复候选已完成，等待端口复测。** 对应[01D任务卡](../../../superpowers/plans/2026-10-05-ext-b-turbine-01d.md)，基线9ac94ae。本页只记录本轮六项反馈；01C人工外观未通过，旧自动证据不改写。功能位于 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，主目录仅同步文档，未合main。

## 用户反馈与处理范围

| 反馈 | 根因与处理 |
| :--- | :--- |
| 输出轴、进排汽口乱纹、缺面及放置方向 | 圆头三角化和模型方向变换两处丢失连续UV；透明图标错用作实体表面；普通模型与Java方向约定不同。保留变换前的逐顶点UV，实体表面使用不透明材质，普通汽口按世界OUTWARD六向选模，普通轴按前/后端分别选模 |
| 定位转子紫黑 | 旧`rotor_middle`引用缺失的`rotor`材质，改为有效材质；另明确提前创建三档叶片PartialModel，避免错过首次模型注册 |
| 独立壳体和控制器缺面 | 补壳体轴向端面及控制器面板背盖；从侧下和背面核对，避免地面挡住缺陷 |
| 碰撞与模型不一致 | 独立壳六向坐标保留；控制器按薄面板和中央盒体并集定义选取/碰撞，四周后方留空，按定位侧面和机向变换 |
| 输出轴闪烁条纹 | 轴承内壁与轴身等半径重合、端盖重复，移开内壁并移除重复面，补针对径向与轴向重叠的检查 |
| 后输出轴名称 | 游戏中改为“汽轮机动力输出轴”，英文“Turbine Power Output Shaft”；注册ID、配方、存档不改 |

三档尺寸、256RPM、蒸汽消耗、库存、双轴动力分配、接口位置及核心先行辅助包壳合同均保持。

## 验证与边界

运行侧9项定向GameTest已通过，含本轮六向普通汽口、四向前轴和控制器两组件形状检查。其余为既有汽轮机搭建/生命周期用例；未重跑无关材料、热端或全量测试。资源后续收尾不改变Java，复用该轮服务端证据。

首次资源交还后仍有安装写入，造成早期JAR/探针使用旧资源；这些记录保留为失败/过期证据，不作为最终候选。真实UV预览随后发现透明实体材质和转向丢UV，由C集中收尾并冻结。最终静态检查覆盖294组OBJ/包装JSON、6075个面的逐顶点法线和绕序，5967个实体面贴图不透明；圆端UV连续、端口内封面、轴承净空和三档动态叶片引用均通过。PM已查看12视角真实UV/纹理/背面剔除预览。

冻结后的 `processResources assemble` 退出0。本批126项变更/新增资源（含8张实体PNG）与JAR逐字节一致，源前后漂移、缺失和不一致均为0。JAR为候选 `build/libs/create_nuclear_industry-0.1.0.jar`，SHA-256：`657ac1d35ce532da93d49fb79e038f90c5310159da3f5c0a761890c2267e14d0`。最终隔离客户端在02:26:39完成图集/模型加载，无汽轮机模型加载错误，02:27:40正常停止、退出0；未打开世界，不能代替游戏内外观验收。日志保留了本任务范围之外的既有流体blockstate警告，未扩大整改。

验证过程中第一次客户端探针的workingDir被ModDev的gameDirectory覆盖，更新了开发目录的`run/config/fml.toml`及日志；未进入世界，未修改存档、options或其他配置。已停止自有进程，并改为gameDirectory/workingDir双校验。第二次在`build/runtime-01d-client`生成日志，原run未继续写入；资源可加载但不等于画面通过。原始事故和主动退出码均在运行报告中记录，不作静默回退或清理。

[六项精简手测清单](./manual-checklist.md)现在可用。人工门通过前不合main、不推进冷凝；游戏中的完整搭建体验仍需用户确认。

## 留存证据

- [运行交付与探针隔离记录](./evidence/EXT-B-TURBINE-01D-RUNTIME.md)、[最终独立审查](./evidence/EXT-B-TURBINE-01D-FINAL-REVIEW.md)。审查器尚未对非玻璃MTL强制`d/Tr=1`；当前实际材质无此问题，记为非阻断的工具改进建议。
- [最终素材报告](./evidence/EXT-B-TURBINE-01D-FINAL-ART-REPORT.md)、[静态检查结果](./evidence/EXT-B-TURBINE-01D-FINAL-ART-checks.json)、[12视角离线预览](./evidence/EXT-B-TURBINE-01D-FINAL-ART-preview.png)。离线图不是游戏截图。
- [GameTest原始成功日志](./evidence/EXT-B-TURBINE-01D-RUNTIME-gametest-final.log)、[冻结后构建](./evidence/EXT-B-TURBINE-01D-RUNTIME-assemble-FINAL-C.log)、[制品资源核对](./evidence/EXT-B-TURBINE-01D-RUNTIME-jar-resources-FINAL-C.txt)、[最终客户端加载日志](./evidence/EXT-B-TURBINE-01D-RUNTIME-client-probe-FINAL-C.log)。对应退出码和哈希清单一并留存。
- [原缺材质日志片段](./evidence/before-rotor-model-error.log)、[旧基线五项负例](./evidence/EXT-B-TURBINE-01D-ASSETS-baseline-negative-checks.json)、[早期资源诊断](./evidence/EXT-B-TURBINE-01D-ASSETS-diagnosis.md)。失败编译、两轮MockPlayer方向测试失败和首次探针原始记录亦保留；B的中间预览/检查JSON曾被覆盖，未伪称保留原图。报告中的build路径保持执行时语境，本目录为精选证据副本。

## R1：成型端口密封与内外朝向

用户在 `b0b3693` 复测时发现进/排汽口周围仍透空，并明确要求方形面朝外、圆柱朝内。原始反馈见[截图](./r1/feedback.png)。原模型的圆管朝外、安装板朝内；之前检查覆盖了材质和内盖，遗漏整机外侧的连续密封面。本次按用户明确方向纠正模型，Java接口方向、尺寸和加工参数不改；旧自动证据不作新的人工通过证据。

**R1候选已完成：** 外侧方形安装面与薄壳平齐，厚3/16格；圆柱向机内伸入，最深距外面9/32格，对三档叶轮均留1/32格净空。橙/蓝中心标识独立映射，外围机壳纹理连续；不翻转Java的OUTWARD或流体能力。模型更新不要求重造已有机器。

旧模型负例：289条外向采样中24条真透空，另168条要深入机内才遇到可见面；新22个方向模型每个289条采样均在外侧遮挡。资源审计与方向检查通过，PM已查看真实相邻薄壳与进/排汽口的内外组合预览。无Java变更，复用01D运行证据，未重跑GameTest或启动客户端。

`processResources assemble`退出0；24个变更OBJ源前后无漂移，与JAR内容逐字节一致。新JAR SHA-256为`3edb84512f3d42566af1274638e7e301b9ec45e3e8c8ca2fefdf315c73b1d8ef`，取代本页上方原01D包作为当前候选。[模型报告](./r1/R1-port-seal-report.md)、[定向检查](./r1/R1-port-seal-checks.json)、[组合预览](./r1/R1-adjacent-shell-port-preview.png)、[复审](./r1/review.md)、[构建记录](./r1/assemble.log)及[制品核对](./r1/artifact.json)已留存。复审指出的UV报告措辞已由执行者更正并经PM核对；仅修改文字，没有再次安装或改变模型资源。

请按[清单R1节](./manual-checklist.md#r1本次只补端口检查)在原机器上复测。其他五项及整体搭建体验仍保留原人工待验状态，不因本次单项反馈推定通过；主目录只同步文档，功能仍在同级候选，未合main。