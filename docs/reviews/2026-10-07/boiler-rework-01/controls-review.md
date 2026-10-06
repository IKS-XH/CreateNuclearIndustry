# EXT-B-BOILER-REWORK-01B 规格与质量合并审查

审查基线：候选 HEAD `7b263d7c8d410541545b58d68968a09e42b3bbaa`；审查范围仅限本卡生产代码、直接受影响测试、专用 GameTest 与模板。未审阅 PM 文档、logs、`__pycache__` 作为产品差异；未运行测试或启动客户端。

## 结论

**未发现阻断合入的确定性规格或质量缺陷。** 单控件、统一下限、按已付汽化热及实际温压自动判汽种、汽种定向刷新、冷液能力生命周期及护目镜同步的实现与本卡相符。未做代码整改。

## 关键核查

- [BoilerControls.java](../../../../src/main/java/com/iksxh/create_nuclear_industry/boiler/BoilerControls.java:30) 只注册一个 `ScrollValueBehaviour`，范围为0–100。数值板的 `maxValue=100`、单行和 formatter 使 Create 6.0.10 的原生板提交0–100每个整数；项目锁定 Create 源码中的 `ValueSettingsScreen` 按1递增取值，`ValueSettingsPacket` 以唯一 `netId` 分发。服务端值由 `selectMinimum` 写入账本，tick 再同步控件，不会恢复旧默认。
- [BoilerState.java](../../../../src/main/java/com/iksxh/create_nuclear_industry/boiler/BoilerState.java:78) 的汽种资格以库存、已付汽化热、汽温和炉压为准；热工目标仍为超临界温度。唯一下限保存为 `Minimum`/`MinimumSet`，限制于[0,1]；合法性不再把它与安全阀开启线比较。[BoilerConfig.java](../../../../src/main/java/com/iksxh/create_nuclear_industry/config/BoilerConfig.java:33) 暴露统一 `outputMinPressure=0.6`。
- [BoilerControllerBlockEntity.java](../../../../src/main/java/com/iksxh/create_nuclear_industry/boiler/BoilerControllerBlockEntity.java:201) 只在实际输出流体变化时递增独立 `steamEpoch`、释放本炉蒸汽压力贡献并标记蒸汽端点刷新。结构 `epoch` 仍由拆装生命周期管理；汽种改变不撤销冷、热或水口。蒸汽句柄捕获本次汽种并校验代次，真实 drain 后按事务开始时汽种返回；这与锁定 Create `FluidNetwork` 的 SIMULATE/EXECUTE 与 generic drain 分支吻合，避免旧网络抽到新汽后把返回量丢弃。[pushSteam](../../../../src/main/java/com/iksxh/create_nuclear_industry/boiler/BoilerControllerBlockEntity.java:209) 先按同一汽种模拟接收，再执行接收并按实际接受量扣账，多个物理口继续使用账本逐口额度。
- [护目镜字段](../../../../src/main/java/com/iksxh/create_nuclear_industry/boiler/BoilerControllerBlockEntity.java:286) 将当前汽种、炉压和下限写入客户端数据；显示汽种来自账本实际资格，压力值乘100后按百分比文案呈现。中英文键已删除手动模式项，替换为当前产汽、有效热力回路与百分数文本。
- 已只读核验交付日志：定向 JUnit XML 为15项、0失败；专用 GameTest 日志为3项全部 required passed，含原生数值包、真实冷液泵和蒸汽管网汽种切换下质量/HU守恒；assemble 日志成功。首轮 GameTest 夹具失败及日志保留，交付说明表明只修正测试观测基线。未重跑这些检查。

## 待人工验收的证据边界

自动证据没有证明玩家端数值板实际打开后的视觉排布/释放选值体验，也没有客户端护目镜逐项显示验收；需沿用锅炉集中人工门核对0/17/43/70/100、重进后保留设置、冷液持续流动、汽种及百分数遥测。此为既定人工门，当前不构成已确认代码缺陷或自动验收失败。
