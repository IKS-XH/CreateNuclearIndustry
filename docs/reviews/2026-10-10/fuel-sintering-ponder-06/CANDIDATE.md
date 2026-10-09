# 燃料烧结炉思索06：播放候选

2026-10-10，PM登记。用户随后明确报告“烧结炉的思索也测试通过了”，两幕按[最终验收](./ACCEPTANCE.md)关闭播放门，main净教学提交`57763f8`。以下候选路径与播放清单保留为历史交付证据，不再要求重复播放；下一台按根AGENTS及[06任务](../../../superpowers/plans/2026-10-10-device-ponder-06-sintering.md)准备屏蔽装配台，其他主线暂缓。

## 候选与获取

- 工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支`codex/ore-acquisition`；源码候选`e76c8d9`，准确提交8个实现/资源/测试路径与两份交付报告。
- [本批冻结测试JAR](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/build/reports/extension/DEVICE-PONDER-06-SINTERING/create_nuclear_industry-0.1.0-ponder06.jar)，沿用MC1.21.1 / NeoForge21.1.219 / Create6.0.10-280环境。
- JAR SHA256：`0aead57d60b796e17c461a98bdeee2e7721d413a6cffbabcbe45af3986ab5206`，与最终构建包逐字节一致；这是候选复制，没有再次构建。

也可从候选目录启动现有开发客户端：

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
.\gradlew.bat runClient
```

## 已通过的两页播放清单

在背包中查看燃料烧结炉物品提示，按住W进入思索，依次播放：

| 页面 | 观察内容 |
| :--- | :--- |
| 烧结与供热 | 炉体与底部燃烧室均可见；生芯块从顶部投入并生成烧结芯块；断热暂停、恢复继续的步骤清楚；字幕与提示不遮挡。 |
| 自动化与核热 | 顶部窗口溜槽的物料、北侧出料漏斗及承接容器可辨识；随后核换热器位于炉底，东西冷热管路可见且方向与文字一致；画面完整，不被标题、按钮或进度条裁切。 |

两页正文只讲操作，逐段退出后再显示下一段。用户已确认本批两页播放通过，无需重跑供热功能、旧设备或旧存档测试。

## 已有自动与审查证据

执行者[IMPLEMENTATION](./IMPLEMENTATION.md)、独立[REVIEW](./REVIEW.md)保留原交付时点，不改写其中“未提交”的历史说明。PM核对原始最终`06-final.log/.exit/.xml`：实际JUnit3/3、0失败/错误/跳过，test与增量assemble退出0；新模板读取、确定性、JAR入口/双语/NBT及冻结8路径SHA均匹配。首次InterruptedException运行环境中断与真正3项断言失败red分别保留，未将中断冒充合同失败。

原始证据目录`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/build/reports/extension/DEVICE-PONDER-06-SINTERING/`。审查与PM未重复运行Gradle、生成器、全量回归、GameTest或客户端。静态时序及几何检查不等于实际渲染通过；本次人工门依据用户明确反馈关闭，不覆盖美术02R1的独立视觉门。
