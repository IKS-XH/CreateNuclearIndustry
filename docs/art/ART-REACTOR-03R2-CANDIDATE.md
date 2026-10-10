# ART-REACTOR-03R2：0.6格方杆与滑块避让候选

2026-10-10，美术负责人。控制棒杆身已改为边长精确0.6格的方形截面，原Create深度控件显示与点击中心共同离开轴心。最终6/6定向检查、增量JAR及来源冻结通过，负责人实际看四张模型/布局图；一次独立组合窄审规格与内部质量通过、必改项无，用户客户端视觉及操作门仍待验，未合main。

## 本版改动

- 方柱杆身X/Z0.2..0.8；固定方形端箍边长0.65格、厚3/16。沿用钢灰纵槽、箍纹与黄铜材质，以及按实际深度完整升降、上方露出的行为。
- 顶底控件移到角落，局部X/Z为0.10/0.10、尺度0.18；四侧控件移到偏上位置、保留尺度0.40。仍可从原六面进入调节，显示变换与点击变换同源，切面时同步原生缓存尺度。
- 全部19份SVG、燃料/辉光/冷却液材质和原动画消费保持。原0..100范围、百分数、服务端权威提交与锁定保持。

控件布局依据锁定Create的实际6PX框、原生旋转及真实testHit输出；顶面框与点击圆均避开杆身和端箍。离线几何及行为检查不证明游戏内文字清晰度或鼠标手感。

## 制品与检查

[冻结候选JAR](../../build/reports/art/ART-REACTOR-03R2/candidate/create_nuclear_industry-0.1.0-ART-REACTOR-03R2.jar)，2,475,501字节，SHA256 `20b7223f899f7b75b56cdfb7d8374c7c7b629b26384add13202939ebc92e97ba`；build/libs同一字节。开工HEAD `d36e791`，主PM授权文档同步后为`6631353`，保留未提交美术源码。

[任务卡及六面布局](tasks/ART-REACTOR-03R2.md)、[主PM共享补充](ART-REACTOR-03R2-COORDINATION.md)、[实施报告](reports/ART-REACTOR-03R2.md)、[最终日志](../../build/reports/art/ART-REACTOR-03R2/final-confirmed.log)、[最终XML](../../build/reports/art/ART-REACTOR-03R2/final/TEST-com.iksxh.create_nuclear_industry.control.ControlRodSliderTransformTest.xml)。最终新专属测试6项，failure/error/skip均0，14秒exit0；jar复用本批未变生产的增量输出。首三次测试环境失败及两次成功原证据均保留，不当作行为red，不重复原18项动画/L2/协议检查。

[冻结清单](../../build/reports/art/ART-REACTOR-03R2/frozen-manifest.json)SHA256 `a502efc1f4843b6aedf0b1f09b3ea41f694af58af2cba02be7b853519396ef56`。负责人实际核对41交付+110证据、75保护与4只读审计依赖，均无缺失或漂移。[来源绑定](../../build/reports/art/ART-REACTOR-03R2/verification.json)记录两OBJ源/安装/JAR及共享三个class/JAR一致，原非几何方法逐字保持；31generated字节/mtime、1054非目标assets及旧03R1候选保持。

[滑块离线布局](../../build/reports/art/ART-REACTOR-03R2/slider-layout.png)来自最终XML的实际Pose/testHit输出；杆身三视、深度与材质对照同在本批证据目录。已实际复看，不重生成素材。

负责人已完整读取[独立规格与内部质量窄审](reports/ART-REACTOR-03R2-REVIEW.md)，报告SHA256 `0d3eb6fdbeca571cf92a070854b1d46898f8be746c90da695dfca379c369eef9`。审查实际看四图、核151冻结、生产变换/真实测试及来源绑定，未重跑生成、测试或构建。报告绑定的候选与清单SHA保持相同，内部候选可交用户复看。

## 客户端复看

退出当前Minecraft，重新启动`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`客户端，使用现有`ore-acquisition - 新的世界`或`ore-acquisition - 新的世界 (1)`。控件Java已修改，需要重启；F3+T只能重载资源。

```powershell
Set-Location -LiteralPath 'E:/MyMC/NewMod/Create_NuclearIndustry-art-studio'
$env:JAVA_HOME='C:/Program Files/Java/jdk-21'
.\gradlew.bat runClient
```

1. 观察完全拔出、半插入与完全插入时的方杆，确认0.6格截面、完整棒长、端箍与材质细节。
2. 从顶面角落及侧面偏上位置悬停、打开原生调节控件，检查0/50/100%可读且能命中，尤其完全插入时不被端箍挡住。
3. 实际改变设定深度，确认驱动器可操作，杆仍按真实深度升降；从顶面转向侧面后点击位置与可见框一致。

本页只记录03R2尺寸/控件门；03R1透窗与冷却液、02R1成型材质/连窗门分别保留原待验状态。独立审查或其他设备教学不会关闭这些用户视觉门。未启动客户端、修改存档或执行Git写；后续集成由主PM或用户管理。
