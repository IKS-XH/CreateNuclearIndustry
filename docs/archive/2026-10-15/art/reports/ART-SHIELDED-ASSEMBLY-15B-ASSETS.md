# ART-SHIELDED-ASSEMBLY-15B C版资产交付

2026-10-14，原唯一资产执行者 `shielded15_assets` 接续用户已选C「模块检修舱」。实际写树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，`codex/art-studio` / `88b8fd289d49df90d7e3c655b6e81381ad01130f`。当前29项已正式安装并逐项SHA读回一致，20旧模型完整备份保持；本报告不表示Minecraft视觉通过。

开工读取主目录最新AGENTS、15B全卡、治理1.2/5.1/5.2与已选反馈。实际应用已读取的 `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：显式元素/面/UV、真实槽引用、16² RGBA与物品display；通用步骤受本卡精确写集约束。MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6保持。

## 来源、备份与候选

`tools/art-assets/shielded-assembly-art-15b/assets.json` 为schemaVersion1消费契约，包含models.parts八项（part/offset/off/on）、partials三槽、item、九textures槽、29项productionMappings（source/target/sha256）及20项backupModels。所有source路径相对15b工具根；target为完整仓库相对生产路径。与原预览执行者已直接确认契约，无模糊路径或临时素材别名。

- `assets/source/approved-concepts.json` 原字节冻结批准契约，SHA256 `b666789912cce985a7363e71d7dbb8c81cd294b490f86dc41ac3ab8bb79e41a2`。
- `assets/source/models/c/` 四JSON及`assets/source/textures/c/`九SVG精确复制最终C；source14项均保留原字节，不重绘、不改灯色/alpha，不执行15A生成器。
- `assets/backup/src/main/resources/...` 完整路径保存即将替换的16off/on、三个partial和物品共20个原模型；与安装前生产身份绑定。原15A对照由预览执行者只读这些备份；资产作者不写原15A服务。
- `assets/candidate/src/main/resources/...` 存29项正式候选：16局部off/on、三个整机坐标partial、一完整归一化物品，以及`shielded_assembly_15c_*`九新PNG。全部新增产物在E盘，没有C盘临时产物或新缓存。

## 几何与像素

C外壳原107个元素按part=x+2*z+4*y、[16*x,16*y,16*z]切成八个局部0..16模型。只保留源盒原外面、裁UV维持原取样、不增加格界内部面；八块按实际偏移还原后与批准C所有外壳元素/面/UV精确一致。on只将实际灯面`#lamp_off`改绑`#lamp_on`，其它面不变。

left_arm/right_arm/fixture分别29/29/28元素，几何、UV和整机坐标逐元素保持C原稿，不进行第二次偏移；原rig/pivot/idle/swept保持。正式物品完整组合shell+三个partial，共193元素，坐标及旋转原点各缩为0..16，UV保持，现行旧物品display逐字义不变。粒子绑定C铅槽。底轴不画进静态机壳，原Renderer的Create SHAFT_HALF及主控[8,0,8]保持。

九PNG由批准16² SVG的原rect顺序与像素中心覆盖规则直接导出RGBA，没有缩放、插值、调色或AI图像。九槽逐像素/alpha相等；window保留透明底和稀疏不透明反光，普通壳体不透明。工具在未知SVG节点/opacity/transform等不支持语法时明确停止，不猜测导出。

## 本批有限证据

实际候选生成命令0.8秒、exit0：

```powershell
$env:PYTHONIOENCODING = 'utf-8'
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B 'tools/art-assets/shielded-assembly-art-15b/produce.py'
```

`build/reports/art/ART-SHIELDED-ASSEMBLY-15B/assets/candidate-output.txt` 与 `candidate-checks.json`：passed=true、errors=[]，29候选、20备份、107外壳还原、193物品元素；九PNG/引用/边界/UV、八格同源、partial、物品display及现行四朝向blockstate引用检查通过。生成使用当前Pillow的getdata只读接口时输出一次后续版本弃用提示；本次实际像素读取和输出成功，没有升级依赖或因提示扩范围。

`source-protection-before.json` 记录106个有限相关输入；当前protectedChanged=[]。覆盖15A原素材/生成器、旧fuel-02e、装配台旧单格/旧PNG/入口、相关Java、锁定版本、现有离心机安装资源、当前日志及JAR；没有世界扫描或旧全设备测试矩阵。`source-identity.json` 绑定本批脚本、契约、冻结来源、20旧模型备份与29候选的大小/SHA256。

15A最终几何/窗可读性、原单次REVIEW与负责人C实图按批准来源复用，未重跑三稿或历史检查。资产作者没有Gradle/Minecraft/JUnit/GameTest、Git写入、世界/配置操作、旧PNG/Java/旧单格资源修改，也未新增工序动画。由原预览作者持有唯一必要assemble及组合审查后制品补核。

## 安装前置与实际安装

前置等待历史：负责人通知预览服务启动/停止受到自动审批拒绝、具体原因尚未返回，要求候选/备份冻结、暂不安装；本资产作者实际遵守，未用候选校验代替ready。随后负责人补卡并由预览执行者成功建立普通exec托管4194/PID16460/session53337；旧4193未停止或重启，15A原机按钮禁用并标存档。负责人及预览执行者均明确ready，本次恢复安装。服务操作与浏览证据由预览作者持有，本段来源为实际协调消息。

唯一安装命令0.6秒、exit0：

```powershell
$env:PYTHONIOENCODING = 'utf-8'
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B 'tools/art-assets/shielded-assembly-art-15b/produce.py' --install
```

安装前29候选SHA与生成契约一致、20原目标与冻结备份一致，九新PNG目标均未占用后才写29项。安装后 `installed-checks.json` / `install-output.txt` 实际passed=true、errors=[]：29来源→生产SHA读回29/29一致、106保护输入protectedChanged=[]。安装阶段复用候选几何/PNG证据，只做来源/目标/保护身份检查，不重新生成或重复像素/八格几何校验。

`source-identity.json` 已绑定最终produce.py、assets.json及完整65项本批来源/备份/候选身份；assets.json保留候选生成阶段说明，实际安装状态以本节与installed-checks为准。最终安装ready已通知负责人及预览执行者，候选/来源/正式安装资源冻结，等待同批真实浏览与组合窄审具体反馈。
