# ART-CENTRIFUGE-14B A版正式资产交回

2026-10-14。用户明确选择“采用第A版开始实施吧”，原资产作者按[14B卡](../tasks/ART-CENTRIFUGE-14B.md)执行；工作树为 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，基线HEAD88b8。原作者不是PM，未子委派或Git写入。本批只写新14B资产工具/来源/证据/本报告及卡列11个正式资源，Java/Gradle/客户端预览由原另一作者持有。

已读主目录最新AGENTS永久中文注释规则和当前治理、治理1.2/5.1/5.2、14B全文，实际使用 `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md` 处理MC1.21.1模型/显式UV/16²RGBA纹理；另读minecraft-modding和minecraft-testing技能，遵守现有注册/客户端资源边界与有限验证分工，不采用通用新版本/datagen/GameTest/全量构建示例。实际Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6不改。

## 已选来源与精确备份

14A A最终R3长轴稿四模型167/155/23/1与六SVG为唯一来源，契约SHA-256 `6634afc2c06b183d759519a351d61d2166f78b22e04662cd882d0d1b89f16ee6` 实际一致。四JSON、六SVG和完整批准契约原字节保存在[14B assets/source](../../../../../tools/art-assets/centrifuge-art-14b/assets/source)，原14A三稿和producer没有修改或重生。

安装前对精确11目标备份：8项存在的旧资源逐字保存，3项原本不存在（shaft JSON、machined PNG、fastener PNG）记录为新增；原物品display以备份文件为权威。身份及路径见[backup/manifest.json](../../../../../build/reports/art/ART-CENTRIFUGE-14B/assets/backup/manifest.json)。本资产作者没有覆盖或打包旧d12 JAR，客户端作者按卡持有其产包备份和唯一assemble。

## 候选与正式映射

[produce.py](../../../../../tools/art-assets/centrifuge-art-14b/produce.py)默认只生成候选，`--install` 仅复制同一候选的11项，不重绘/重导出。来源、候选、安装及SHA契约见[assets.json](../../../../../tools/art-assets/centrifuge-art-14b/assets.json)，schemaVersion1，status=installed，当前SHA-256 `cab843377ae30aba096096cc76b0f7ea8b4d93d26b1392a6d761556f79e3e0d4`。

| 正式JSON | 来源及行为 |
| :--- | :--- |
| enrichment_centrifuge.json | A lower167元素，几何/UV原样，仅六槽转正式纹理ID |
| enrichment_centrifuge_upper.json | A upper155元素，半格原坐标/UV保持 |
| enrichment_centrifuge_rotor.json | A R2增粗rotor23原字节几何/UV保持 |
| enrichment_centrifuge_shaft.json | A长轴1元素，真实4×4×8/XZ6..10/Y0..8，原六面取样保持 |
| enrichment_centrifuge_item.json | lower167＋upper155的Y/originY+16＋rotor23＋shaft1＝346元素；原display不改 |

模型都使用 `create_nuclear_industry:block/enrichment_centrifuge_{casing,brass,glass,dark,machined,fastener}` 六个正式纹理ID，没有设计稿虚拟路径。对应6份PNG只从现有A SVG导出，全部16×16 RGBA；本批SVG本来均是实色矩形、alpha255，六PNG逐像素/alpha与来源相同，不调亮或重画。

导出复用旧 `tools/art-assets/export.py` 的strict像素纯函数，未调用其批量/安装入口；原SVG中的非视觉title只在内存移除以符合该函数的rect限制，原文件及14B保存副本逐字保持。每坐标单位对应一个像素、文档顺序覆盖；Pillow12.3固定PNG无额外元数据，compress_level9、optimize=false。首轮实际导出仅有getdata弃用提示，未影响颜色/alpha或exit0，不为该提示改候选或更新依赖。

blockstates的8个half/facing映射及models/item的原parent只读；资产工具还保护14A producer与旧exporter身份。未写blockstates、物品parent、注册、语言、datagen、服务端/配方/库存/碰撞等路径。

## RED后顺序安装与有限证据

候选生成实际exit0，11资源/11源副本、346物品、原display及源身份通过，见[candidate-checks.json](../../../../../build/reports/art/ART-CENTRIFUGE-14B/assets/candidate-checks.json)。物品分段为lower `[0,167)`、upper `[167,322)`、rotor `[322,345)`、shaft `[345,346)`；显式UV仍在0..16，upper平移没有重新投影纹理。

原客户端作者在旧正式资源上实际取得3测试/3失败/errors0、BUILD FAILED in16s后明确发来“现在可安装11正式资源”。资产作者读回[result.json](../../../../../build/reports/art/ART-CENTRIFUGE-14B/client/red/result.json)和真实[gradle.log](../../../../../build/reports/art/ART-CENTRIFUGE-14B/client/red/gradle.log)，才执行：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' 'tools/art-assets/centrifuge-art-14b/produce.py' --install --red-evidence 'build/reports/art/ART-CENTRIFUGE-14B/client/red/gradle.log'
```

安装实际exit0，精确11项候选/正式字节一致，原源身份保持；RED日志SHA绑定在[installation-checks.json](../../../../../build/reports/art/ART-CENTRIFUGE-14B/assets/installation-checks.json)。未抢写安装、未并行执行安装/Gradle。

末次独立读回11候选/安装、15原来源/只读入口、8旧备份无漂移；10选A模型/SVG副本及批准契约副本字节一致，六正式PNG16²/像素SHA/alpha255一致，物品lower/rotor/shaft逐项原样、upper和rotation.origin精确+16、原display保持，实际exit0见[readback.json](../../../../../build/reports/art/ART-CENTRIFUGE-14B/assets/readback.json)。复用14A已有真实几何扫掠及原同次窄审，不重新跑三稿、SVG/网页旧矩阵或设备测试。

当前候选/安装/producer身份已稳定并告知原客户端作者。客户端该类GREEN、双partial/真实PNG预览、负责人GPU浏览、组合窄审与唯一assemble由原作者/负责人按卡接续；本资产作者不冒称这些已验收，完成后停止写入。main/管理员树、Git、世界/配置/JAR未写，Minecraft视觉与动画仍待本次候选用户手测。
