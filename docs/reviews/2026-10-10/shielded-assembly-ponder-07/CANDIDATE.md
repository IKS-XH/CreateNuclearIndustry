> **历史候选：** 用户2026-10-10要求删除后两页配方场景，当前只保留搭建与接口；请使用[07-R1单页候选](./R1-CANDIDATE.md)。本文件及原三幕证据保留，原播放清单不再适用。

# 屏蔽装配台思索07：播放候选

2026-10-10，主PM登记。三幕已按用户明确指令完成，3/3定向合同、增量打包及一次独立规格/质量窄审通过；只等待本台用户播放。源码和教学资源留在逻辑候选树，尚未合入main。独立ART03/02R1视觉门继续保留，其他工程主线暂缓。

## 候选与启动

- 工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支`codex/ore-acquisition`；净候选提交`d6fd20a8934aa3f5dc66e6ccd015b0ec6d22ef3c`，包括9个实现/资源/测试路径及IMPLEMENTATION、REVIEW两份报告。
- [冻结测试JAR](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/build/reports/extension/DEVICE-PONDER-07-ASSEMBLY/create_nuclear_industry-0.1.0.jar)，SHA256 `7bafdd14a47d940da34b42390b1a06084965bc1fdf374689f26c2e46a10143d4`。与`build/libs`构建包一致，保留MC1.21.1 / NeoForge21.1.219 / Create6.0.10-280环境。

从逻辑候选目录启动开发客户端：

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
.\gradlew.bat runClient
```

主目录当前仍提供已验收的前六台教学；本次07须使用上述候选。两个目录分别使用自己的运行配置和世界，不改写用户存档。

## 本台三幕播放清单

在背包中查看屏蔽装配台物品提示，按住W进入思索，依次播放：

| 页面 | 观察内容 |
| :--- | :--- |
| 搭建与接口 | 一件放出完整2×2×2设备；主控底部唯一动力轴清楚；四周进出料与顶部只进料位置可见。 |
| 新燃料装配 | 四种材料8/4/2/1分别从可见外侧送入，完成后1个新组件经出料口进入承接桶；配比、64RPM约20秒及更高转速的说明清楚。 |
| 乏燃料封装 | 枯竭组件、基材、铅桶各1件，一次得到带原组件记录的封装桶；取出成品及清空本批后切换用途的说明清楚。 |

三幕共同观察：完整机体、底轴、材料图标及成品不被遮住；镜头转向后入口仍清楚；字幕逐段出现，不互相覆盖，不被标题、按钮或进度条裁切。只复看这三幕，不重测已验收正式装配/封存功能或前六台教学。

## 自动与独立审查证据

执行者[IMPLEMENTATION](./IMPLEMENTATION.md)、独立[REVIEW](./REVIEW.md)保留原交付时点的未提交说明；PM随后只暂存准确11路径形成上述候选。审查必改项无。

原始证据目录：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/build/reports/extension/DEVICE-PONDER-07-ASSEMBLY/`。原red为3项实际断言失败，Python应用别名异常单独保存；最终`04-final.log/.exit/.xml`为3测试、0失败/错误/跳过，test与唯一增量assemble退出0。模板实际回读、八格归属和生成确定性通过，源—JAR绑定及双语旧键保持通过。

PM实时核对9源文件SHA、IMPLEMENTATION冻结SHA、构建及冻结JAR、7个资源/class入口，全部与交付一致；两份原dirty日志逐字节恢复，既有pycache保持。PM检查记录位于本台忽略的SDD目录`pm-binding-check.json`。复用原构建证据，未重跑Gradle、全量、生成器、GameTest或客户端。

静态时序与几何检查不代表实际播放通过。本台等待用户反馈后才关闭播放门并净教学合入main；教学门不覆盖独立美术视觉门。
