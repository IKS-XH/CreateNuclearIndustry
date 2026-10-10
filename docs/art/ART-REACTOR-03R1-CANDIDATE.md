# ART-REACTOR-03R1：三项视觉整改候选

2026-10-10，美术负责人。用户反馈的管束过细、控制杆过黑缺细节、冷却液不可见已形成定向整改。18/18定向检查及唯一增量JAR通过，负责人已实际读生产提交、原red/final XML与日志，查看五张对照图并核对192/192交付/证据/只读依赖哈希一致。一次组合独立窄审规格及内部质量通过、必改项无；可以交新版客户端观察。用户客户端视觉门仍未通过，原03/02R1历史证据保留。

## 本版改动

- 燃料管外径2.5→3.5模型单位，两方形钢板厚1→2；九管中心不变，相互留隙，蓝辉仍按真实列功率且逐管同轴。
- 控制杆外径3→5，固定端箍外径6、厚3；新增可辨钢灰棱面、纵槽、浅色分段箍纹。完整棒体仍随实际深度升降，上方露出。
- 修复宿主外壳暗格光照被用于整根杆及所有液面的错误。杆按实际位姿采世界光，液面按各自合法空气格取光，保留真实库存液位、连续冷热混色、流动帧和透明度。

低分辨率素材仍由两份整数SVG作者源导出；其他17SVG原字节保持。没有修改库存/热工、控制效果、L2、碰撞、世界照明、其他设备或02R1外壳。离线预览不能替代客户端透窗、昼夜或透明排序观察。

## 制品和证据

[冻结候选JAR](../../build/reports/art/ART-REACTOR-03R1/candidate/create_nuclear_industry-0.1.0-ART-REACTOR-03R1.jar)，2,474,232字节，SHA256 `ec9901a46d95707efdb2c4bc2b63f52fa76a3223f6e115950062eba2f8a44ed1`。旧03B候选另存baseline，未覆盖原冻结。

[实施报告](reports/ART-REACTOR-03R1.md)、[整改卡及采光依据](tasks/ART-REACTOR-03R1.md)、[冻结清单](../../build/reports/art/ART-REACTOR-03R1/frozen-manifest.json)、[验证与来源绑定](../../build/reports/art/ART-REACTOR-03R1/verification.json)。清单SHA256 `4ee58009f19310693e6909208b7ffa8347470b07c2959db6fa0d2c905d18ef7a`；67交付＋115实施证据＋10只读audit依赖均实际核对。正式19项源/安装/JAR绑定，仅四OBJ和两PNG变化；L2 R2十二源、CT三源、原1037assets及item保持。

新两项真实行为red复现宿主采光，修复后与原16项共18通过（VisualState11、Materials7），failure/error/skip均0。仅一轮最终`test --tests '*ReactorAnimation*Test' jar`，19秒退出0；未启动客户端、修改存档、执行Git写或重复全量测试。最终HEAD`d36e791`相对开工`a0cf5f3`仅主PM同步四文档，功能前置不变。

负责人已实际读取[独立规格＋质量窄审](reports/ART-REACTOR-03R1-REVIEW.md)，报告SHA256 `dbdbdb9340fae17452d51e6a2961f839075014f272df63402b329c8ad903b9e3`。审查实际看五图、核182交付/证据与10只读依赖、审生产采光和两真实行为检查，未重跑构建；绑定上述候选和冻结清单不变。build/libs与候选副本SHA也已由负责人核对一致。

## 客户端复看

退出当前Minecraft，重新启动`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`的客户端，使用现有`ore-acquisition - 新的世界`或`ore-acquisition - 新的世界 (1)`；无需重建设备。Java采光修复需重启，F3+T只能重载新版中的资源。

```powershell
Set-Location -LiteralPath 'E:/MyMC/NewMod/Create_NuclearIndustry-art-studio'
$env:JAVA_HOME='C:/Program Files/Java/jdk-21'
.\gradlew.bat runClient
```

1. 看单块及堆内燃料管束、顶底板，确认厚重感与间隙。
2. 白天观察完全拔出、半插入和完全插入的控制杆，确认钢灰棱面/纵槽/箍纹可辨、完整长度与升降位置正确。
3. 结合护目镜非零真实库存透窗观察冷却液，确认液位、纹样流动和冷热连续混色，并能看清杆体。

保存中的8748/126mB及内部SKY11..13仅用于定位采光错误，不当作截图实时库存。原生透明排序、夜间真实无光环境及Catnip极大世界坐标float采样仍有客户端表现限制，未引入全亮/最低亮度补偿。03R1与02R1外壳视觉分别等待用户确认，不由自动检查或教学验收代替。
