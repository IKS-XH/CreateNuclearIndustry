# ART-SHIELDED-ASSEMBLY-15C 屏蔽装配室动画候选

主PM更新：用户追加要求最新美术合入后，已[合main并生成含当前逻辑的33da9002…组合包](../reviews/2026-10-14/latest-production-art-integration/INTEGRATION.md)，源码双亲提交`497f64b`。下方dd6美术候选及“未合main”描述保留交付历史；Minecraft动画手测状态仍未报告。

2026-10-14用户要求“接着制作动画吧”，并明确采用制造双臂交替送入/对齐、夹具压装与封装固定/同时合拢短行程锁紧、完成退回的分工动作。本批在原美术树完成客户端实现、同源预览和必要增量候选；当前只待用户游戏观察，未合main或操作Git。

## 当前包与预览

- 来源树：`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，分支`codex/art-studio`，来源基线`88b8fd2`，保留既有未提交美术来源。
- [候选JAR](E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/libs/create_nuclear_industry-0.1.0.jar)：2,844,567字节，SHA256 `dd6e25b8b04be4567f8d55d24918d75fad2c37a26ea41fb2c7675e420c6a9414`。
- [同源动画预览](http://127.0.0.1:4195/)：正式模型/PNG与游戏使用同一动作JSON，页面控制项为示范输入，没有连接用户游戏。
- 原R1修复包完整备份在`build/reports/art/ART-SHIELDED-ASSEMBLY-15C/client/backup/create_nuclear_industry-0.1.0-602985f8.jar`，2,812,546字节，SHA `602985f85d2522c7914d7a4cef0b1c822f8c50ce11687e60af2be697428e3cd3`；用户已确认正常的黑窗修复继续有效。

## 实际行为

原三个待机partial的86个元素精确拆成八个固定/活动分件，原UV、纹理、窗层、静态完整物品和底部Create动力轴保持。制造时双臂交替送入、对齐并退回，由夹具合拢、短行程升降压装后松开；封装时夹具固定、双臂同时合拢作短行程锁紧，再退回松开。

动作读取真实工序、加工进度、实际RPM和过载状态。客户端在现有同步间隔内有界平滑，断电/过载/单人暂停保持当前位置，恢复不补停机时间；批次完成后回待机，缺料与输出堵塞不空转。工序切换和实体/世界更换使用各自基准。没有改变配方、工时、库存、服务端tick、网络、结构或碰撞。

## 已核证据与边界

当前两新增测试类实际17/17、exit0；原失败和进度到包双计整改证据保留，之后的两次姿态导出只各跑单项，不冒称另一套17。八分件源到安装一致、45原C/R1资源保持；同一动作28关键帧/26区间保持实际运动约束。负责人使用本地block-model-renderer实看正常窗制造/封装、升降剖视、暂停/过载/恢复和完成待机，并关闭预览四向裁边问题；两次失败原图/身份保留。以上不代替Minecraft动画验收。

原一次规格/质量组合审查及同次制品补核记录于[REVIEW](reports/ART-SHIELDED-ASSEMBLY-15C-REVIEW.md)。唯一增量`assemble --no-daemon`实际10.5092882秒、exit0；9新资源从生产/处理输出到JAR一致，567当前class到JAR一致，19新增载荷为10客户端class+9资源，1953个非本批旧载荷保持（含555旧class），无测试/探针泄漏。两旧Renderer载荷中，生成切换表仅调试行号变化、原生opcode一致；核脚本首次漏列及纠正证据保留，没有重新打包。

详细来源见[ASSETS](reports/ART-SHIELDED-ASSEMBLY-15C-ASSETS.md)、[CLIENT](reports/ART-SHIELDED-ASSEMBLY-15C-CLIENT.md)、[PREVIEW](reports/ART-SHIELDED-ASSEMBLY-15C-PREVIEW.md)、[负责人实看](reports/ART-SHIELDED-ASSEMBLY-15C-GPU/README.md)及`build/reports/art/ART-SHIELDED-ASSEMBLY-15C/client/final-identity.json`。原main正式包、其它候选、全部世界/配置和既有服务未写入或覆盖。

## 本次集中游戏观察

保存并退出当前美术客户端，再从原美术树启动，以加载新的客户端渲染器；继续使用原存档与已搭好的C版装配室，无需重新建造。

```powershell
Set-Location -LiteralPath 'E:/MyMC/NewMod/Create_NuclearIndustry-art-studio'
.\gradlew.bat runClient
```

1. 制造新燃料：透过原观察窗看双臂交替、夹具压装及完成退回，动作与护目镜进度一致。
2. 封装乏燃料：确认夹具固定、双臂同时短锁紧，与制造动作能区分，结束回待机。
3. 任一工序中途断动力或造成过载，再恢复：机构应停在当前位置后接续，不能空转或跳过停机期间工时。

本批不重新打开已验黑窗或旧业务门，不自动推进其它设备、main合入或发布。反馈只登记实际观察到的动画结果。
