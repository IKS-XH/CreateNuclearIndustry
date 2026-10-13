# 反应堆管理员首批候选

更新：拆壳改装成型故障已交[01R候选](../reactor-automation-retrofit-fix/CANDIDATE.md)`2dff787`，当前正式包为`5e04c667…`。本页下方`4757f27`/`4204ab17…`及47来源为原01交付时点，原证据保留；启动仍用同一目录与世界，人工门未关闭。

2026-10-13。用户已批准完整设计及实施顺序，唯一执行者已完成并经一次独立组合审查/同次整改窄复核通过。**当前停客户端集中手测，未合main、未替换主目录包或世界。**

## 已实现

- 可选结构方块“反应堆自动化接口”，侧壁非框架位置、每堆最多一个；原Create烈焰人燃烧室在炉外相邻格任职，原补料/机械臂行为保留。
- 燃烧室外侧四控件：模式、全堆插深、绝对目标HU/t、确认复位。统一手动调棒、恒定发热、故障保持；手动允许逐棒精修，预警不能提棒。
- 每10tick正常提出最多1/插入最多5个百分点，40tick平均只用于显示；每tick检查真实冷却与有效余热。故障或重绑不自动运行，健康稳定默认100tick后显式复位只返回停堆。
- 运行许可15/0经火把反相接仪表急停；安全状态幂等保持可动棒全插入。接口同回合原位替换拒绝旧请求，原维修解卡后的棒也不能复活旧浅目标。卡死/未受控燃料的真实剩余发热保留。
- 独立SERVER文件`create_nuclear_industry-reactor-automation-server.toml`包含批准15参数。有序`STS/PIP/SCS`产1接口：4钢板、2精密构件、1工业传感器、1电子管、1反应堆外壳。

## 启动与手测

候选目录：`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-automation`，分支`codex/reactor-automation-basic`，从main`0674b2f`建立，源码候选提交`4757f27`。使用已携带的`run/saves/新的世界`，27文件核对一致，主原件保留；6份CNI配置保持实际设置，世界不共用链接、不回写主目录。新管理员配置首次客户端启动会按批准默认生成。

```powershell
Set-Location E:\MyMC\NewMod\Create_NuclearIndustry-reactor-automation
.\gradlew.bat runClient
```

按[集中手测清单](CLIENT-CHECKLIST.md)检查安装、四控件、整堆/逐棒交接、恒定HU、真实反相线路、故障/复位、卸载与保存重进。普通小堆即可，不要求再次搭16³。空手面对燃烧室外正面操作，旁侧接反相支撑/火把，保持正面空格。先设非零HU目标再选恒定功率；复位选1确认，0取消。

## 制品与自动证据

正式候选：[create_nuclear_industry-0.1.0.jar](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-automation/build/libs/create_nuclear_industry-0.1.0.jar)。大小3,138,045字节，SHA256：

`4204ab17618b6dbbfa056218d6aa3a5c1c4a460c688d233388feb56aa3dd9301`

冻结副本为`build/reports/reactor-automation-01/reactor-automation-reviewed.jar`；初`f29c4ea7...`包、原8c03基线、失败日志及首次清单保留。47来源、646生产class、1546资源与最终清单匹配，发布包排除本批GameTest及模板。主目录原745e/8c03/261012包未覆盖。

| 检查 | 结论与准确范围 |
| --- | --- |
| 结构/配方初轮 | 10/10，真实旧结构拒绝单接口/缺配方RED先保留；必要成员白名单和枚举适配已登记 |
| 初轮组合 | 31/31；最终受影响整改28/28，未变项复用；没有重复完整build |
| 真实代表 | 同一最终生产源码4/4自然保存exit0，覆盖控制、故障保持、换代/NBT、原生燃烧室；两人服务端入口不等于两客户端GUI体验 |
| 无下降沿补充 | 真实断粉尘后维修断言4/4通过、epoch稳定；本次保存挂起后终止，不能记退出成功。原因未确认，与前一自然退出证据分列 |
| 增量包 | 初次与审查必改后的第二次各1秒exit0；两Important及两准确性说明关闭，同次审查无剩余必改 |

GPU/鼠标、真正跨区块卸载、完整保存重进及安装包启动仍未手测，不能由NBT或开发服务器代表替代。保存挂起限制详[实现报告](IMPLEMENTATION.md)，审查结论见[REVIEW](REVIEW.md)。原泵/现场流量/16³锅炉等独立门保持，不以本批候选覆盖。
