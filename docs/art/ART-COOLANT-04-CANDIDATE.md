# ART-COOLANT-04：普通冷热冷却剂同源动画候选

状态：资源检查、唯一增量打包及一次独立规格/质量窄审通过，必改项无；游戏内显示待用户复看。仅美术树候选，尚未合main。

普通冷液改用反应堆内部的冷蓝流纹，热液改用对应热橙流纹。两者直接由原十六份SVG确定性导出；静止面和流动面都有八帧、每帧2tick及原生RGB插值，八帧循环0.8秒。flow每帧32×32由原16×16纹样精确2×2平铺，匹配原生流面UV。八PNG及八mcmeta已安装，普通流体各自显示端点颜色、固定循环；内部原真实库存混色、转换量调速和顶点透明度由原消费者处理。

## 复看

工作目录 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`。重启美术树客户端并打开原测试存档，可同时观察此前03R3的Java修正；单独本批材质可用F3+T重载。无需重建设备或迁移存档。

| 位置 | 本批预期 |
| :--- | :--- |
| 分别装冷液、热液的Create储罐 | 冷蓝/热橙与内部对应颜色同源，低对比流纹连续循环，无静止条带或丢图 |
| 已有透明直管里的两种流体 | 可见部分沿用相同配色/纹样；普通不透明管壳不产生透视效果 |
| 冷却剂桶、冷液世界静止面及流动面 | 桶沿用still sprite；世界流面图案像素密度正常，斜向流动与侧面没有明显平铺断线 |
| F3+T资源重载 | 动画恢复循环，不出现缺图或错误分帧 |

热液当前没有独立桶或可放置方块，本批不添加这些注册对象。普通PNG alpha保持原来的255，世界现有solid策略保持；本批是材质与循环统一，不以离线图推定全场景透明/光照效果。03R3液体浸泡/顶面操作、02R1材质连窗和其它教学人工门仍各自独立。

## 证据入口

- [同源静态对照](../../build/reports/art/ART-COOLANT-04/preview/same-source-comparison.png) / [八帧循环GIF](../../build/reports/art/ART-COOLANT-04/preview/coolant-loop.gif)：取实际新PNG与内部冻结sheet，GIF显示100ms关键帧，原生中间插值留游戏复看。
- [实施报告](reports/ART-COOLANT-04.md)、[锁定管线PROBE](reports/ART-COOLANT-04-PROBE.md)、[资产任务卡](tasks/ART-COOLANT-04.md)、[专用工具与SVG来源](../../tools/art-assets/coolant-fluid-04/README.md)。历史pipeline恢复锁不管理这次新用户指定材质，应使用本批专用producer。
- [唯一独立窄审](reports/ART-COOLANT-04-REVIEW.md)：规格/内部质量通过，必改项无，SHA256 `61220e612b26fb83e6e65ef7e27ecbd95c5ddb5b6c3edad11284193ad870088e`。负责人实际完整读取，核报告/冻结/JAR身份保持；75项绑定和16资源映射由审查者一次只读核对，未重跑流水线。
- [冻结清单](../../build/reports/art/ART-COOLANT-04/frozen-manifest.json)：29交付、22证据、24只读依赖；SHA256 `b48d3bf8968b0f92fe06ba6ed5b90d1df59dc834ce557b10482045db7dfa0580`。
- [唯一候选JAR](../../build/reports/art/ART-COOLANT-04/candidate/create_nuclear_industry-0.1.0-ART-COOLANT-04.jar)：2,468,602字节，SHA256 `7f9f2da39128203700edb677ca525bf11f1d50a0811194ef24f1d3330d983b68`。一次jar退出0/4秒，compileJava复用，无JUnit/GameTest。
- 十六资源生成物→安装→build资源→JAR一致；其余1048正式资源、371源码/测试、1089旧素材/工具及旧R3候选/冻结保持，详见[本批资源绑定](../../build/reports/art/ART-COOLANT-04/verification.json)。

负责人实际看同源对照、核GIF八帧/100ms与loop0，读取完整producer/mapping、PROBE、最终实施和原check/jar证据，核冻结身份；原reviewer只读一次合并规格/质量窄审，未重复运行流水线。实际游戏效果不由这些自动/离线证据关闭。
