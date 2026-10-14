> 当前状态更新（2026-10-12）：用户已确认本组合的可变尺寸、快捷换料、观察窗和顶面控件复看及最近重进存档手测通过，已[净合main `6bdef37`](../reactor-variable-accepted-integration/ACCEPTANCE.md)。下文待验/未合main与旧JAR身份为历史记录，锅炉尺寸16³新增范围独立待验。

# 换料端口快捷替换：客户端候选

> 2026-10-12追加现场故障：用户进入原世界触发换热器弱集合并发崩溃。本页原功能检查作为历史证据保留，现已交[加载窄修候选](../exchanger-load-crash-01/CANDIDATE.md)，当前包身份以它为准，可从同一目录重启再进入；本功能/尺寸/美术人工门未关闭。

已完成本批实现与独立规格/质量审查，源码留在 `codex/reactor-variable-size`，功能提交 `d811f841c89331ac19564ab80d94a331706ecaf7`。ART09净接入为 `26f4ba25ff71085009bf799ded418e9b796c141d`。本页只登记候选，快捷换料、可变尺寸及ART08/09人工门分别保持，尚未合main。

手持一件可用新燃料组件右击成型换料端口，只有原组件枯竭时才能一次完成换入并返还乏燃料；仍有耐久时拒绝替换。旧件优先回交互手，手中有余量则入包，满包掉落在玩家身边。主/副手、生存/创造均消费一件。空槽、空手取料、钢板维修和机械臂保持原规则；列余热、损坏和实际尺寸保持，没有相邻控制棒的新燃料仍按原规则放热。

## 启动与最小手测

从既有同级候选启动，沿用已经携带的世界；本批没有新建分支或搬动/回写世界：

```powershell
cd E:\MyMC\NewMod\Create_NuclearIndustry-art-integration
.\gradlew.bat runClient
```

1. 对已枯竭端口手持一件新组件右击：一次换入，原乏燃料回手，动作栏提示正确。
2. 对仍有耐久的端口同样右击：明确拒绝，两边物品和耐久保持。
3. 可补看副手、手中余量入包与满包单件掉落；无需重测整条产线或已验收其他设备。

同包还包含已内部审查的钢色驱动器/铜色换料口和放大的顶底深度控件；按[集中单第4节](../reactor-variable-size/CLIENT-CHECKLIST.md)一起复看真实字体、密集顶盖辨识与长按，按各自范围确认。

## 自动证据与制品身份

- [执行报告](IMPLEMENTATION.md)与[独立窄审](REVIEW.md)：原7项加新5项，JUnit12/12、零失败/错误/跳过；最终独立域真实服务端一个有限组合用例1/1通过，正常保存退出，命令exit0/20秒。它证明服务端模拟玩家入口，不代替实际客户端鼠标/提示验收。
- 最终必要增量assemble exit0/1秒。收尾创造满包的实际吞物品RED已修正；初次组合包与筛选/夹具失败日志作为历史保留，未冒作最终成功。不完整build、不clean，不重复纯事务及美术已审结果。
- 当前JAR `build/libs/create_nuclear_industry-0.1.0.jar`：2677917字节，SHA256 `896c2c38d984680eb61452d2a6ba5757e4a08e8931a2f19335acc5e26a57d6bf`，冻结副本在 `build/reports/refueling-quick-swap-01/final-refueling-and-art09-combined.jar`。
- 最终清单 `build/reports/refueling-quick-swap-01/final-binding.json` SHA256 `4e54bd761a5691e5598d9ebcadabd041b801a4beb64f1716627197f97159532a`。PM实际核8功能源、13打包class、3语言/模板资源及ART09两源8PNG与当前JAR一致；提交只改变Git身份，没有再次构建或改写这些输入。
- ART09原定向9/9、素材及独立组合审查复用，来源/报告换行补充见[组合记录](../reactor-variable-size/ART09-INTEGRATION.md)。未用固定尺寸美术包覆盖可变包。

用户确认后由PM记录本功能验收并安排净合入；自动检查不提前关闭其他人工门。
