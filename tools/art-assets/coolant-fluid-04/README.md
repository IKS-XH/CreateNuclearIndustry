# ART-COOLANT-04 专用普通流体资产

原创源稿只读引用 `../reactor-animation/sources/coolant_cold/frame_00.svg` 至 `07` 与 `coolant_hot/` 八帧，mapping记录16份源稿SHA。producer严格读取16×16整数rect；不绘制替代PNG，不调用旧pipeline。普通cold/hot图案与内部冻结sheet逐帧一致，alpha全部255。

still每帧16×16、sheet16×128。flow每帧32×32、sheet32×256，由对应16帧精确2×2重复，匹配世界侧面quarter UV及流动顶面旋转采样；Create有自己的流体UV算法。四个mcmeta明确宽高、帧0..7、2tick、interpolate=true。八帧关键帧循环0.8秒；GIF只显示100ms关键帧，原生atlas负责中间RGB插值。

工作区根使用捆绑Python（不安装依赖）：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/coolant-fluid-04/producer.py generate
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/coolant-fluid-04/producer.py check
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/coolant-fluid-04/preview.py
```

check只读实际PNG/元数据，比对SVG、内部sheet、四象限、alpha、2px含7→0周期和冻结ModFluids字节。安装后 `check --installed` 再验证block/fluid两组精确对应；producer仅生成新独立目录，没有自动安装入口。非法来源/目标/规格在全部输入验证完成前拒绝，写前不创建输出。

本批八目标优先由本专用producer管理，名称/两组路径见mapping。历史 `../pipeline.py` 的 `RETAINED_ORIGINALS` 恢复锁会把冷却剂恢复成旧材质，禁止运行其全量export/install覆盖本批，不修改或宣称旧工具全量check通过。

普通atlas固定循环，分别显示冷蓝/热橙端点，不执行内部消费者的真实冷热库存混色及转换量调速。内部顶点alpha与浸泡包络保持原Java；普通世界默认solid策略保持。储罐、透明管、动态桶、冷液世界面的颜色/流纹/资源重载需客户端复看，热液当前无可放置世界方块。

首稿证据在 `build/reports/art/ART-COOLANT-04/preview/`，负责人已实际复看通过。八PNG/八mcmeta已逐字安装，唯一增量jar完成（exit0/4秒，无测试）；16资源生成物/双安装/build资源/JAR绑定见同证据目录 `verification.json`，冻结入口为 `frozen-manifest.json`。实际游戏显示待用户复看。
