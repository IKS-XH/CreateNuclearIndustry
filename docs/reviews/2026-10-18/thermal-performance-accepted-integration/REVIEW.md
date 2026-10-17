# 汽轮机 S1/C1 一次组合窄审

2026-10-18，独立规格＋质量审查者。审查基线为 main `a66d4342200c04d61136afedd539c67268330c01`，仅覆盖本批活动卡列出的六个源码/测试文件；不包含后置锅炉同步专项。审查者未做 Git 写操作、生产修改、构建、测试重跑、客户端启动或性能采样。

## 结论与唯一发现

未发现 P0/P1/P2 问题。已完成的 S1/C1 六文件可按原冻结来源继续本批合入；存在下述明确 P3 视觉瑕疵，PM 已决定本次保持已授权原候选来源/制品身份，不为此改源换包。这是审查建议，不替代 PM 的最终验收或制品绑定核验。

**[P3] 渲染原点重定位时旋转部件短暂缺一帧。**

- 位置：`C1/source/src/main/java/com/iksxh/create_nuclear_industry/turbine/client/TurbineRotorVisual.java:24`（构造器24–26行，实例首次创建在47–52行），同目录 `TurbineOutputShaftVisual.java:37`（构造器37–39行，首次创建54–59行）。两构造器只执行 `super`，没有初始实例。
- 真实触发链：锁定 Flywheel 1.0.6 源码中，`backend/engine/EngineImpl.java:62–75` 在相机超出渲染原点距离阈值时更新原点并返回 true；`impl/visualization/VisualizationManagerImpl.java:107–117` 此帧选择 `recreateAll` 分支，跳过包含 `beginFrame` 的 update 分支；`impl/visualization/storage/Storage.java:122–172` 删除旧 visual，构造新 visual，只做注册及 `updateLight`，不会调用新 visual 的 `beginFrame`。
- 结果：该重定位帧两类旋转部件没有实例；默认 visualizer 仍跳过其 BER，故不能当帧回补。下一正常帧 `beginFrame` 创建并变换实例即恢复，不是持续缺失。固定机壳、服务器吞吐、租约及热工状态不受影响。
- 证据性质：由上述实际源码调用链确定；本次没有新增现场复现，也不把它写成已实测画面故障。后续如处理，应在两 visual 的初始化生命周期内准备有效实例，同时保留原首帧相位、采光及静止回退。

## 规格与调用链核对

- S1 `TurbineStructure.java:31–77,291–338`：OwnerEntry 只弱持控制器，Form/成员坐标与闭区间 PartBounds 不反向持有 Level；生产 scan 生成不可变成员列表，Form 身份改变即重算 bounds。真实成员重叠必导致包围盒相交；粗筛后仍调用 `other.claimedForm()` 检查当前 `live()`，最终比较实际成员，不以包围盒直接判冲突。空集合、负坐标、边界接触及稀疏成员语义保持。
- S1 `212–264`：仅预计算3/5/7静态空气截面；坐标及遍历顺序与原 at 相同，可变游标只在单次检查内使用。每次仍检查当前区块 tick 资格、端口/两轴身份及空气；`TurbineControllerBlockEntity.live/validPort/validShaft` 和带 epoch 的旧流体句柄没有改动，没有新增跨 tick 资格缓存。forget/失效/区块世代原路径保持。
- C1 `TurbineClientEvents.java:58–63` 与 Flywheel `SimpleBlockEntityVisualizer`、`VisualizationHelper.tryAddBlockEntity`、`SectionCompilerMixin`：支持可视化时原 BER 被排除；后端关闭或不支持该上下文时仍走原 BER。两 visual 使用锁定1.0.6允许在 frame 中创建/steal 的 TRANSFORMED 实例，实例换 partial 复用，隐藏使用零矩阵，delete 后清空实例/partial/visible。
- C1 采光与材质：RendererReloadCache 随资源重载清空，使用相同 partial 并显式沿用 cutoutMipped；原 BER 与实例均以锚点采光。AbstractBlockEntityVisual 的 section 采光通知及 null 实例安全 relight 适用。转子扩展 AABB 与原 BER 一致；两类均保留中心严格小于64格门槛，使用 renderOrigin 相对 visualPos 变换/视锥。
- C1 动画：rotorPose/outputAngle、tick、clear、重载与卸载共享同一类锁，避免多个 Flywheel worker 交错修改相位/弱绑定集合。读取 runtimeDisplay 的几何和相位同一次取样，empty 静止回退不补旧 RPM；同 BE 身份/成员/世界会话与暂停算法保持原实现。Flywheel frame/tick 同步与删除 syncPoint 已按本地依赖调用链核对；未宣称扩展到未测试优化模组/所有并行环境。
- 相位与法向矩阵、四向、正负角与64格边界已有真实入口定向测试；服务端热工/交易/网络/NBT/资源未在 C1 改动。没有通过少绘目标转子、降低流量或降低转速换性能的代码变化。

## 复用的既有证据与边界

证据根目录：`E:/MyMC/NewMod/Create_NuclearIndustry-thermal-performance-diagnosis-20261017/`。

- 已阅读 S1/S1.patch、C1/C1.patch、两份 REPORT、六文件及必要原调用链；S1/source 与 C1/source 的 TurbineStructure 和新增 LookupTest 当前逐字相同，SHA分别为 `CF552C6FCCAEDB6CED2F7C59BF5282605FE45F9A9BAC2F7F3F5137A09E369906`、`C8AF3C3F64F119AB4B11349AF1CA9FB1DDA5ABC3952B8BE59160549C2F04C287`。
- S1/evidence/green-tests.log 为4/4；functional-summary.json 与 remote-functional.log 记录7/7真实汽轮机GameTest、同 tick 空腔/端口/轴失效，以及远端真实 ChunkEvent.Unload、epoch 2→3、重载后旧句柄0容量。原首轮将 getChunkNow=null 误认卸载及原点票据超时均保留，不把这些失败说成通过。
- C1/evidence/test-summary.json 与 gradle-targeted.log 为27项、0失败/错误并 BUILD SUCCESSFUL；为原 NeoForge 定向入口结果，本次未重跑。
- C1/evidence/functional/ 的 after-reload-all-visible、on-restored、rejoined 均为176 visual、144可见转子；原176全部delete及实例引用释放。off-static 为关闭后端后0 visual；static-fallback 有18个可见转子且全部不可靠/静止；night-before-light/night-with-light 实际11个锚点采光变化。重载首次侧视164项的失败与后续全机组俯视176项恢复区分保留。
- 同场景 A1/B/A2 原生帧与 PERFORMANCE.csv 支持所报远景/俯视收益；每次前后16台均216mB/t、256RPM。S1 原客户端堵塞瞬态异常、C1 解锁帧率后每秒分配/GC增加及锅炉近景GC尾事件均不删去或称已解决；不把每帧下降改写为总内存/总CPU下降。
- 当前只读复核原包 SHA：基线 `9C556236051AB1304BBFEBAC79AC300A1CA1E400DFE9D1DAFF11C38EC1D83F50`，S1 `0A01D5709D48FD4207FEFD8031A63ECDF4467F1DA083F9830B0D799B079BF3F8`，C1 `9DA77B19BDF3C30EB9FE1EEAA4FF8A6CC6463E950ACC407C62DFE892E5537230`。逐 entry 载荷比较分别只有原S1六个和原C1八个所属class差异，其余载荷同字节；没有重新封装。这不替代集成执行者对最终已提交源码、编译class及提升制品的绑定核验。
- 已核原功能服及A1/S1/A2进程退出记录exit0，C1四个进程exit0记录保留；不据此宣称本次重新执行或整套GameTest通过。Sodium/Iris、真人多人、真正 isPaused=true 单人暂停仍未现场验证；原定向JUnit暂停覆盖不替代这些现场结论，不自行新增阻塞门。

## 技能、版本与权限

已读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`（物理客户端边界、生命周期/注册与Create集成）、`minecraft-testing/SKILL.md`（JUnit/真实服/视觉证据分离）及 `minecraft-ci-release/SKILL.md`（来源与制品身份）。已按 AGENTS、治理5.1、STATUS及本批活动卡执行；通用技能示例不改变项目版本、测试入口、许可或Git权限。

实际 Gradle 基线为 MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；依赖源码读取自本地Gradle缓存的1.0.6 sources JAR。审查仅写此 REVIEW.md，没有修改 main、冻结 source、核心文档、用户存档/配置或原始证据。
