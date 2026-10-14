# EXT-ART-02A 独立只读审查

审查日期：2026-09-29。审查者为执行者，不具备项目经理或验收权限。

审查位置：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支 `codex/ore-acquisition`，HEAD / 对照基线 `c92e76282828927915dea5b5b3be399cb880eaab`；审查对象为当前未提交改动。

## 审查发现

**无阻塞发现。** 未发现本次修改引入的 P0–P3 可操作缺陷。结论限于资源字节、管线代码与离线预览；不代表用户冷却剂客户端视觉复验通过，也不改变任何任务状态。

## 独立核对

- 逐项读取 `git show eddd097^:src/main/resources/assets/create_nuclear_industry/textures/<路径>` 的二进制内容，与当前游戏 PNG、各自 `tools/art-assets/baseline/`、各自 `generated/` 及 `pipeline.prepare()` 返回的保留原字节比较：8/8 完全相同。block/fluid 分别核对，没有用兼容路径代替原图。
- 另外 43 张游戏 PNG 及相应 generated PNG 与 `c92e762` 逐字节相同。Java、数据 JSON、模型、baseline、SVG 源稿与色板无 Git 差异。完整差异仅有合同允许的八张游戏图及工具文件，未见矿物实现、mcmeta、构建、存档或核心文档变更。
- 使用捆绑 Python 3.12.14 / Pillow 12.3.0，在 `-B` 模式中只执行读取及内存运算：52 张候选与内存 prepare/编码结果一致，全部分页、总览和 HTML 与内存生成结果一致；已有 `verification.json` 的 113 条输出 SHA-256 均与磁盘现状相符。
- 内存替换 manifest 内容，独立确认 extra_path、wrong_mapping、unauthorized_preserve、missing_preserve 四种输入均被拒绝；未写回任何源稿/清单。`git diff --check` 退出 0，仅有工作树行尾提示。

| 文件名（block/ 与 fluid/ 各一份） | 尺寸 | 恢复后 SHA-256 |
| --- | --- | --- |
| compound_coolant_flow.png | 16×64 | ff5fc3fe7a385cb77343c81d191ffec7ca4801c1003c250e2d27658331f48adf |
| compound_coolant_still.png | 16×16 | de30924110d0ec43e801151e94369db6a39d5fee08db889155c991b2116d3bc9 |
| hot_compound_coolant_flow.png | 16×64 | 44aee2d725d6baa6c4a60232aad0d87549fb61a086b064d0ccbc99b7ea5f29f6 |
| hot_compound_coolant_still.png | 16×16 | 7456bf7510e47f104b9481eed3468ab0de1454a500c437d9af7b8541b468f9ea |

八项均为 RGBA。旧尺寸、alpha 和 PNG 编码字节保留。

## 管线与证据审查

`pipeline.py` 独立硬编码 51 条游戏路径，新增原图保留表严格限定八条冷却剂路径及其 SHA-256。只有这八条允许且必须携带 `preserve: baseline-original`；其他项不能加入 preserve，也不能通过 manifest 改映射或指定任意位图。所有 SVG（包括保留为历史的冷却剂 SVG）仍先经过原严格解析、尺寸、色板及 alpha 检查，随后验证保留 PNG 哈希；prepare 全部成功且预览在内存生成后才进入写入。`--install` 的实际路径集合、目录边界与已有尺寸校验仍在写入之前。

默认导出的八张 generated 使用已核验原字节；显式安装从同一输出取值，因此不会再次安装撤回 SVG。`verify.py` 同样按独立八路径集合核对 Git 原图，并在安装后要求游戏字节等于原图。`export.py` 的修改只有说明文字，严格 SVG 解析未放宽。

本轮不重新执行会写输出或暂改输入的 export/verify CLI。已阅读执行者本轮 `commands.json`、`green.log`、`verification.json`：两次默认导出和两次显式安装退出 0；六次实际 CLI 坏输入均退出 1，验证脚本检查失败前后输出不变；记录包含 30 类非法 SVG/flow 越界拒绝。当前输出与证据哈希独立复核相符，CLI 重复运行结论属于审阅已有执行证据，不冒充本审查者重跑。

`ModFluids.java` 的 `coolantType` 当前返回 `block/<id>_still` 与 `block/<id>_flow`，实际客户端引用覆盖本次恢复的 block 四图。fluid 四图作为现有兼容资源各自恢复，不改 Java、颜色乘算、流体行为或动画。

## 实际看图与未验证项

使用图像查看工具实际打开 `tools/art-assets/previews/coolant-1.png`。八个面板 BEFORE 与 RESTORED 图形一致；冷态为青蓝像素纹理，热态为橙红像素纹理，flow 完整展示长画布。原尺寸、明暗底与平铺均可辨认，未见本次恢复产生的裁切或文字遮挡。该图已通过内存重建逐字节一致检查，所示最终候选确为恢复后的旧 PNG。

未启动 Minecraft 客户端、未运行 Gradle / GameTest、未重新打包或检查 JAR。用户冷却剂静止/流动面视觉复验仍未完成；粗矿压缩、真实粉碎、保存重进的人工状态也不由本审查改变。未越过人工门安排其他任务。

## 规则与技能实际应用

已读主工程及执行工作树 AGENTS、主工程治理协议、EXT-ART-02A 合同、原 EXT-ART-02 合同及客户端检查点。

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：核对 gradle.properties 与 build.gradle 的实际版本，查阅 ModFluids 客户端纹理引用，确认资源整改未变更注册与逻辑。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：区分本轮独立静态/内存核对、已有 CLI 运行证据及未完成的客户端验收；按合同不扩增无关游戏测试。
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：核对 PNG 路径、RGBA、16×16/16×64 布局与实际预览；遵循只读审查授权，不运行可能产生输出的通用验证器。

固定技术栈：Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。

本审查唯一写入为本报告；未修改实施者文件、核心文档，未执行 Git 写操作、派发或验收。
