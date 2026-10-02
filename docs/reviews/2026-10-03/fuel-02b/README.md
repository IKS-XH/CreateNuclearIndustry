# 燃料02B：整格炉体与工作台配方

**状态：2026-10-03用户确认下表两项复测通过，02A/02B已一并验收合入main，见[最终验收](./ACCEPTANCE.md)。** 此前[02A四组手测](../../2026-10-02/fuel-02a/CLIENT-CHECKLIST.md)已全部通过，运行证据继续复用。实施合同见[02B任务卡](../../../superpowers/plans/2026-10-03-ext-a-fuel-02b.md)。

炉体保留八棱外形，放置模型外包宽/高/深填满一格，端盖闭合且不越界，物品保持常规显示大小。制造改为工作台3×3，布局如下，产1台：

```text
耐火砖  钢板    耐火砖
耐火砖  高炉    耐火砖
钢板    传感器  钢板
```

中央为原版`minecraft:blast_furnace`，旧燃烧室制造配方移除。运行仍需底部完整烈焰人燃烧室；400tick、1:1、无GUI及顶进四侧出均不变。

完整退出旧客户端后，可直接从主目录启动：

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry'
.\gradlew.bat runClient
```

以下两项复测均已获用户确认通过，其他02A人工项未重复：

| 项目 | 操作与预期 |
| :--- | :--- |
| 工作台制造与JEI | 工作台按上图放入4耐火砖、3钢板、1高炉、1工业传感器，制得1炉；JEI显示新工作台配方，中央已换高炉，无旧燃烧室成本配方。 |
| 模型 | 放置后炉体宽、高、深占满一格，八棱外形和冷热窗口正常；绕看端盖/四周无缝隙与闪烁，不越入相邻方块；物品栏与手持仍为正常大小的完整模型。 |

本批人工门已解除；后续包壳及装配的配比与设备分工仍需另卡确认，不计入本批完成范围。

## 本批证据

[实施报告](./EXT-A-FUEL-02B.md)、[限定独立复审](./EXT-A-FUEL-02B-REVIEW.md)与[制品核对](./artifact-check.json)记录本次范围。复审未发现P1/P2阻断。唯一增量`assemble`退出0，用时2秒；Java编译为UP-TO-DATE，仅重新处理资源并打包。JAR SHA-256为`41ba1c74cd935b1e3ec05d70f695e5addfc5aec07cd76fbd082226c292787f15`；PM核对3份炉体模型和新配方与源码逐字节一致，旧机械配方在包内已不存在。

三模型外包范围在浮点容差内为0..16³，每份320个显式UV面，无越界；补齐北面钢壳/窗框并消除共面覆盖，正面采样无缺口或重叠。7张既有PNG字节不变，SVG、Java与测试源无改动。PM实际查看了[顶部/正面预览](./EXT-A-FUEL-02B/furnace-north-top-angle.png)和[底部/热态预览](./EXT-A-FUEL-02B/furnace-bottom-angle.png)。几何预览使用简化光栅器，不代表Minecraft最终贴图、光照或视觉验收。

锁定Create的`RecipeGridHandler`先查普通`RecipeType.CRAFTING`，服务器设置`allowRegularCraftingInCrafter`默认启用；关闭该配置会关闭普通配方的动力合成器入口，不影响工作台。本批不额外复制专用机械配方。

按治理5.1，复用02A的4项JUnit、1项GameTest及用户全部手测确认；未重跑这些测试，未启动客户端、默认run世界或改动构建配置。合入后主目录仅增量assemble一次，9秒退出0；主目录制品及换行差异核对见[最终验收](./ACCEPTANCE.md)。
