# 屏蔽装配台思索07-R1：单页播放候选

2026-10-10，按用户要求删除“新燃料装配”和“乏燃料封装”两页，只保留“搭建与接口”。思索只介绍用法，不介绍具体配方。3/3定向检查、一次增量构建与一次独立窄审通过；尚未完成客户端播放确认，源码暂留逻辑树。

## 候选与启动

- 逻辑树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支`codex/ore-acquisition`。
- 净候选：`0a4e1e58efb8070db891acccdc7ac1ff5264f159`，6个文本改动、2个模板删除、实现及审查报告，共10个精确路径。
- [冻结JAR](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/build/reports/extension/DEVICE-PONDER-07-ASSEMBLY-R1/create_nuclear_industry-0.1.0.jar)，SHA256 `bc5dd22005d45a982f589e7e9d9aed0b1e2f4e3adfb0216d8e3972be4e75d11f`，与本次`build/libs`制品一致。

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
.\gradlew.bat runClient
```

## 本次只复看一页

查看屏蔽装配台物品提示并按住W：确认只剩“搭建与接口”，整台机体、主控底轴和外侧物流清楚，顶部接口说明可见，三段正文逐段出现、不重叠或被按钮/进度条裁切。保留页的模板、镜头与正文未改，不重复正式加工功能或其他已验收教学手测。

## 实施与证据

两加工方法及其专属辅助代码、注册、双语各8个Ponder键、两份NBT和生成路径已删除；生成器只保留placement，新增只读`--check`。正式设备、配方及其他教学保持。

[R1-IMPLEMENTATION](./R1-IMPLEMENTATION.md)与独立[R1-REVIEW](./R1-REVIEW.md)保留执行时点。原始证据位于逻辑树`build/reports/extension/DEVICE-PONDER-07-ASSEMBLY-R1/`：原red为3测试/2实际断言失败/0错误/跳过；最终`03-final.log/.exit/.xml`为3测试/0失败/错误/跳过，定向test与唯一增量assemble退出0，BUILD SUCCESSFUL。独立窄审必改项无。

PM实时核对6源、2模板确实删除、保留模板SHA `67f03765acb3d1e5d5273cdcec15ebe47892bb0438a98b0dfbd3ff9df77645f2`、保留方法原文、新JAR及5个class/双语/模板入口，全与交付一致。两个原dirty日志逐字节保持；没有重跑构建/测试、客户端、服务端或生成器。绑定记录位于本台`.superpowers/sdd/2026-10-10-device-ponder-07-assembly/r1-pm-binding-check.json`。

[原三幕候选](./CANDIDATE.md)及其报告/JAR完整保留为历史，原三页播放清单由本页替代。单页反馈通过后才关闭播放门并净教学合入main；不关闭独立美术门、不推进其他主线。
