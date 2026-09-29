# EXT-ART-02A 执行交付报告

日期：2026-09-29。执行者：coolant_restore。仅交付未提交改动；是否验收、保存候选由项目经理决定。

## 合同与实际环境

- 任务卡：主工程 `docs/superpowers/plans/2026-09-29-ext-art-02a-coolant-restore.md`。
- 工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支 `codex/ore-acquisition`，基线 `c92e76282828927915dea5b5b3be399cb880eaab`；开工 Git 状态为空。
- 实际读取主工程与候选 AGENTS、主工程治理协议、原 EXT-ART-02 合同和客户端检查点，遵守执行者不得修改核心文档/Git 的限制。
- 实际读取并应用技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`（核对既有 ModFluids 客户端纹理引用与禁止改变注册/行为）；同根 `minecraft-testing/SKILL.md`（资源静态验证、构建和人工视觉分层，不以离线结果冒充游戏验收）；同根 `minecraft-resource-pack/SKILL.md`（PNG RGBA、尺寸、命名空间路径、mcmeta 保持原状）。另读取 TDD 和 verification-before-completion 技能；先观察新原图字节断言失败，再实施保留分支和最终复验。
- 核对 `gradle.properties` / 构建：Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6；没有修改版本、许可证或构建。

## 精确改动

1. 仅恢复游戏纹理 `textures/{block,fluid}/{compound_coolant,hot_compound_coolant}_{still,flow}.png` 共8项。原字节来源为各自已跟踪 baseline PNG，并与 `git show eddd097^:<游戏路径>` 逐字节互证，不重编码、不采样、不近似绘制。
2. 管线保持固定51路径写集；仅这8项允许 `preserve: baseline-original`。其路径及旧 SHA-256 独立硬编码冻结，所有输入、原图哈希、尺寸/模式/alpha 验证完成后才写出。冷却剂输出直接使用已验证原字节；其他43游戏PNG仍使用既有SVG导出。没有引入通用位图导入。
3. 四张撤回的冷却剂 SVG 留档、仍执行严格语法校验，明确不再安装；没有改动任何 SVG 或 baseline 文件。清单、工具说明、导出/验证逻辑同步此例外。
4. 对应8张 generated PNG、冷却剂预览、总览和 HTML 同步最终候选；其他44张 generated（含青金石粉候选）不变，其他7张分组预览未发生 Git 差异。
5. 总计24个跟踪文件有改动：8个游戏PNG、8个generated PNG、README/export.py/manifest.json/pipeline.py/verify.py、preview.html/preview.png/previews/coolant-1.png。报告及证据在本目录，未提交。

## 客户端引用与不变项

`src/main/java/com/iksxh/create_nuclear_industry/content/ModFluids.java:104` 起的 `coolantType` 构造 `block/<id>_still`、`block/<id>_flow`，并通过 `IClientFluidTypeExtensions` 返回；实际加载的是4个 block 路径。4个 fluid 兼容路径也分别恢复各自旧字节，本次没有依赖它们恰好相同来覆盖原图差异。冷/热流体身份、UV、颜色/透明度设置、注册、模型及 mcmeta 全部保持原状。

`scope-verification.json` 对开工快照执行全量 SHA-256 比较：全部 src 文件集合相同且只有上述8个PNG变化；其余43个游戏PNG、全部 Java/数据/测试/模型/语言、矿物实现保持原值。全部 SVG、baseline PNG 与 baseline.json 不变；非冷却剂 generated 44项不变。该审计未读取或修改主工程用户 `.vscode/launch.json`。

## 原图与整改前 SHA-256

下表“原图”同时等于整改后游戏PNG与 generated PNG；恢复后尺寸等于原尺寸，均为 RGBA、alpha=255。8项完整的前/后 SHA-256 和尺寸字段见 `scope-verification.json`。

| 路径 | 尺寸 | 原图 = 整改后 SHA-256 | 整改前 SHA-256 |
| :--- | :--- | :--- | :--- |
| `block/compound_coolant_flow.png` | 16×64 | `ff5fc3fe7a385cb77343c81d191ffec7ca4801c1003c250e2d27658331f48adf` | `bfc84c9bfa91c641fb3dd71acf92322b60b3700d5bad59dc79a9dcd931aaad87` |
| `block/compound_coolant_still.png` | 16×16 | `de30924110d0ec43e801151e94369db6a39d5fee08db889155c991b2116d3bc9` | `128dbdc31f99ff4005c7e9e8446dbfa524908baa76a7b0e5365b7c503e77156d` |
| `block/hot_compound_coolant_flow.png` | 16×64 | `44aee2d725d6baa6c4a60232aad0d87549fb61a086b064d0ccbc99b7ea5f29f6` | `b77862b6bbdf2bc218c0eec99e6eedee74af0187bb61f1456a9e7179d4143e84` |
| `block/hot_compound_coolant_still.png` | 16×16 | `7456bf7510e47f104b9481eed3468ab0de1454a500c437d9af7b8541b468f9ea` | `fee5b3896265ee0890adb3e01b04d79f487b6b6883635ce2098216416fd56873` |
| `fluid/compound_coolant_flow.png` | 16×64 | `ff5fc3fe7a385cb77343c81d191ffec7ca4801c1003c250e2d27658331f48adf` | `bfc84c9bfa91c641fb3dd71acf92322b60b3700d5bad59dc79a9dcd931aaad87` |
| `fluid/compound_coolant_still.png` | 16×16 | `de30924110d0ec43e801151e94369db6a39d5fee08db889155c991b2116d3bc9` | `128dbdc31f99ff4005c7e9e8446dbfa524908baa76a7b0e5365b7c503e77156d` |
| `fluid/hot_compound_coolant_flow.png` | 16×64 | `44aee2d725d6baa6c4a60232aad0d87549fb61a086b064d0ccbc99b7ea5f29f6` | `b77862b6bbdf2bc218c0eec99e6eedee74af0187bb61f1456a9e7179d4143e84` |
| `fluid/hot_compound_coolant_still.png` | 16×16 | `7456bf7510e47f104b9481eed3468ab0de1454a500c437d9af7b8541b468f9ea` | `fee5b3896265ee0890adb3e01b04d79f487b6b6883635ce2098216416fd56873` |

## 实际执行验证

解释器：`C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe`，Python 3.12.14 / Pillow 12.3.0。下列命令均在候选工作树运行：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' tools/art-assets/export.py --install
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' tools/art-assets/verify.py
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' build/reports/extension/EXT-ART-02A/audit_scope.py
git diff --check
```

- 初始 RED：新增“冷却剂原图字节保留”断言在 `block/compound_coolant_flow.png` 失败，证明旧管线输出不满足恢复要求；见 `red.log`（初始终端编码使中文错误文本乱码，路径与 AssertionError 保留）。
- 实施后最终 `verify.py` 退出0：52个候选、51个游戏路径；四张已批准样稿回归通过；两次默认导出和两次显式安装分别哈希完全稳定，默认导出保持游戏字节不变。
- 30 类非法SVG/flow越界拒绝；6个真实CLI失败场景（非法SVG、额外路径、错误尺寸、错误映射、非冷却剂声明原图保留、冷却剂遗漏保留声明）都在任何输出写入前失败，state哈希不变。坏SVG只临时作用于被撤回的冷却剂 SVG，finally恢复，不修改非冷却剂源稿。
- `audit_scope.py` 退出0；完整比较证据见 `before-files.json` / `scope-verification.json`。`git diff --check` 退出0，只有工作树 LF/CRLF 标准提示，无空白错误。
- 实际查看了整改前 `coolant-before.png` 与最终 `tools/art-assets/previews/coolant-1.png`（复制为 `coolant-after.png`）：冷态青蓝颗粒、水流细纹及热态橙红颗粒/亮纹均与重绘前 BEFORE 列相同；完整16×64 flow未裁切，明暗底与平铺均显示恢复后的旧图。未调用生图工具。

## 未验证项与人工门

未启动 Gradle、游戏客户端或 GameTest；按任务由项目经理统一资源处理、打包和启动准备。离线PNG查看不是客户端视觉复验。等待用户确认两种冷却剂游戏外观；粗矿9:1压缩、真实粉碎轮、保存重进仍未全部人工通过，不在本次自动勾选。没有推进材料/设备依赖批次，没有任何 Git 写操作或核心文档修改。

证据：`commands.json`（真实CLI命令/退出码/stdout/stderr）、`verification.json`、`green.log`、`red.log`、`before-files.json`、`coolant-before.json`、`scope-verification.json`、`audit_scope.py`、前后PNG。
