# MIT许可与原创美术开放组合窄审

- 日期：2026-10-20；唯一规格与质量组合审查，供PM记录最终结论。
- 基准：main `e6a21b7737ac81f183489590ee3610848aad9f1d`。已读AGENTS、治理、STATUS、活动卡和IMPLEMENTATION；审阅本轮全部10项暂存差异（含原未跟踪LICENSE、NOTICE及活动卡），复核时未暂存差异为0。
- 结论：本轮声明、元数据及打包配置差异未发现阻塞项；不把此结论扩大为完整历史第三方审计或包内核验。

## 具体核对

- `LICENSE`为标准MIT授权、保留声明与免责全文，无Logo或其他附加限制条款。`NOTICE.md`、README和当前治理/美术入口一致：原创代码、文档、非Logo原创美术及可编辑源文件适用MIT，贡献者版权保留。
- Logo像素图、高清/历史源与导出、封面等嵌入副本的Logo部分明确排除；非Logo原创部分及导出/校验工具代码保持MIT。运行图标及两处Logo目录存在，封面说明与导出入口确实引用高清Logo。
- Create模型/调色派生资产未被重新许可；来源映射、原许可、运行目录均存在，原许可明确assets保留权利、代码MIT。原创`output_arrow`单独保持MIT。three.js原MIT署名文本及预览引用目录存在，声明保留原有第三方权利。
- `gradle.properties`与NeoForge的许可字段均为`MIT`，中文注释指向NOTICE。`build.gradle`仅新增根`LICENSE`、`NOTICE.md`和原Create许可到`META-INF/licenses/Create-LICENSE.md`三个文件入口；现有exclude、版本、依赖、功能和资源未改。
- 复用IMPLEMENTATION的静态检查及字节回退比较证据；审查只读取差异、声明和相关来源/路径，没有重新运行实现者检查、构建、测试或Git写操作。唯一写入为本报告。

## 尚未核验

本轮审查时尚未提交来源或生成新JAR。新包的许可字段、三份声明逐字一致性、非声明条目与旧包一致性及正式制品身份，仍须按活动卡在准确来源提交后的一次增量`jar`中核对；不能以当前配置静态审阅代替。

## 实际技能入口

- `C:/Users/IKSXH/.codex/skills/minecraft-ci-release/SKILL.md`：许可、打包与来源/制品边界。
- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：实际NeoForge元数据位置及版本基线。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：区分静态和真实运行证据，按任务范围不重复测试。
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：模型、贴图、Logo路径与代码/资源边界。
