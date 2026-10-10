# ART-REACTOR-03A1物品展示继承实施报告

仅静态燃料架OBJ包装补继承`minecraft:block/block`，保持原item父ID及世界几何/材质；三个动画partial继续无parent。新补充已冻结，未安装正式资源、改Java、构建、启动客户端/服务/浏览器或Git写，仍不是03B运行接入。

## 来源与范围

唯一工作区`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，所有shell显式workdir；HEAD `8b83a1a97f1b539e6dcc7b6714b9ee96c13854e0`。实际读取03A1卡及03设计/原03A，复用项目规则与已读minecraft-modding/testing/resource-pack、verification-before-completion：只做物品显示继承与真实资源/OBJ投影定向检查，不重跑旧几何/材质/负例或944矩阵。技术栈保持，中文手写说明。

本轮只修改`tools/art-assets/reactor-animation/generate.py`两行中文说明/静态分支，以及`generated/models/reactor_fuel_rod.json`一个parent字段。SVG、rig、mapping、README、所有mesh/纹理/三个partial及原03A报告/冻结/证据只读。新报告/证据分别唯一03A1路径。

原item只继承`create_nuclear_industry:block/reactor_fuel_rod`。实际锁定本地client.jar的`assets/minecraft/models/block/block.json`及NeoForm源片段已保存新证据：BlockModel.getTransform沿parent取变换、无parent/本地变换时NO_TRANSFORM；ItemTransform.apply使用Quaternion.rotationXYZ。原版GUIrotation `[30,225,0]`、translation0、scale `.625`；第一人称`.4`、第三人称`.375`也由父模型保留。旧静态包装确无parent/display，记录baseline.json中的old_static_model，而非靠世界斜视猜item显示。

## 实际定向结果

| 检查 | 结果与证据 |
|---|---|
| 原冻结/副本 | 修改前93/93匹配，两旧文件逐字节保存；baseline.json/log/.exit0 |
| 唯一一次生成 | 改变1候选包装；generate-once.log/.exit0，未再运行生成或旧check |
| 其余32候选 | SHA与mtime_ns全部保持，verification.json/log/.exit0 |
| 包装引用 | 静态只多parent，原loader/OBJ/材质/particle/culling/shade/UV字段相同；三个partial parent计数0 |
| 历史链 | 原91项当前保持，两旧副本各匹配原hash，逻辑合并恢复原93链；原freeze文件SHA未变 |
| 原JAR | 仍`ca591c0d64b6d184930bd42ba65a2c1647ab9ac8decdf9206ed94f788f80d533`；无构建/正式安装 |

当前两项授权漂移详见verification.json的before/after/old_copy。原03A冻结清单SHA `ff7dfab9d4ff282f9dd031bb35b551b817c28a6cd840e457dd28fcba23c89f40`；不覆写旧清单去掩盖漂移。恢复历史93链使用旧两副本＋当前91hash进行核对，未把当前修正文件回退。generator.diff留最小改动证据。

## 真实GUI投影

已生成并实际打开`build/reports/art/ART-REACTOR-03A1/item-gui-preview.png`：左为旧NO_TRANSFORM的默认正面scale1，右为实际保存OBJ/UV按本地原版GUIrotation/scale投影。两图同镜头、固定比例尺，先将模型居中-.5，再scale、RX×RY×RZ（等价rotationXYZ）、translation/16；未对新图自动适配边界掩盖.625缩放。preview-provenance.json绑定OBJ、材质、原投影器和原版变换。

只从旧03A预览源码提取parse_obj/Raster两个只读定义，未导入或执行其顶层写出，避免重写旧冻结预览。几何/UV解析复用，光照与深度采样仅离线模拟，不能替代Minecraft GUI/手持最终观感。

用户本轮已明确选择完整控制棒整体升降、上方露出，与原03A已审预览一致；本补充不另改控制棒素材。未来BER需覆盖完整拔出范围，但本段不接Java或共享注册。

## 证据与冻结

真实命令/退出码在03A1 commands.md及对应日志。源片段提取首轮误假定方法为public导致substring not found/exit1，保存baseline-source-first.*；核对实际源后正确提取exit0，不是行为测试失败，未改候选或旧证据。其余必要步骤exit0，手写空白0错误。

新frozen-manifest.json绑定当前两文件＋本报告及所有新证据，关联原03A冻结SHA和明确两项授权漂移；旧两份原字节保存在本批baseline/，原91项及原报告/证据不改。提交未提交候选供负责人只读复看，不另开整批完整审查，不开始03B；等正式L2 HANDOFF与消费卡。
