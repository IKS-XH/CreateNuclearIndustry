# REACTOR-RUNTIME-COOLANT-03：共享修复交接

普通冷液输入/热液输出导致完整运行样本被撤销的根因已修复；自动及一次独立规格/质量窄审通过。液面闪断的游戏视觉复看并入ART-REACTOR-08，尚未宣布视觉验收。可变尺寸01仍独立停在人工门。

## 净同步与公共入口

| 落点 | 本批净修复提交 | 实际状态 |
| --- | --- | --- |
| `Create_NuclearIndustry-runtime-coolant` / `codex/reactor-runtime-coolant` | `3b118ed9943275e8504e601fd232daef1e9f467b` | 两生产源、一个独立GameTest、原NBT逐字副本及两份报告，已冻结。 |
| 主目录 / `main` | `b1108670ebca3bc99456b52381a72b36cac78e85` | 六路径无冲突净同步；生产及构建输入与修复树Git内容相同，可复用已审JAR。未合可变尺寸玩法。 |
| 美术树 / `codex/art-studio` | `f787aa4` | 同六路径无冲突净同步；未覆盖07B-R2或08的独立美术源/素材。可执行ART08唯一组合assemble。 |
| 可变尺寸候选 / `codex/reactor-variable-size` | `209cd816ae018f734c05f08a99f309de77d30525` | 两共享源自动应用后PM已核净差异，仅新增库存入口和两调用；原尺寸/远端安全/保存保护保持。组合JAR待下方补充，原冻结证据不改写。 |

实际新增服务端入口：`ReactorInstrumentPortBlockEntity.setCoolantInventories(long coldCoolantMb, long hotCoolantMb)`，两个参数均为替换后的非负mB数量。生产仅`ReactorCoolantFluidHandler`的正数量EXECUTE fill/drain调用。可靠服务端结构缓存及匹配L1/L2的dimension、owner、generation、geometry、origin、bounds成立时，保留上一完整descriptor原对象；无新sample/revision/time、无中途投影、无客户端租约续期。下一正式成功结算发布同龄完整样本。

通用`setSnapshot`正文、正式完整投影、五成功tick心跳、20活动tick客户端租约、失败/结构/卸载/坏包撤销保持。客户端State/Codec/Factory/Renderer/缓冲、美术消费签名和包格式均未改。旧或不可用样本不能被普通库存入口复活。真实mB、共享容量、每口预算、保存与控件同步保持；该入口不接收热工、列、燃料、控制或尺寸。

## 验证与制品

原始证据唯一根：`E:/MyMC/NewMod/Create_NuclearIndustry-runtime-coolant/build/reports/reactor-runtime-coolant-03/`；命令和边界见[IMPLEMENTATION](IMPLEMENTATION.md)，规格及质量见[REVIEW](REVIEW.md)。

- 03真实RED准确复现fill/drain的`Available=false`和完整对象改写；保存阶段停滞，仅结束本轮自有子进程，Gradle exit1/child -1，不称自然保存成功。01/02只是模板启动排错，不计业务验证。
- 04真实GREEN：1/1、27秒、自然exit0，维度保存完成；实际世界capability及更新包验证原完整样本冻结、正式新结算、SIMULATE/零量、通用同值外部撤销、提交前早退及结构失效。
- 05原三套相关JUnit与唯一增量assemble：17/17、零失败/错误/跳过，25秒exit0。未放宽原断言，未跑完整build、clean或重复旧矩阵。
- `final-identity.json`绑定四源/资源、五class、新旧模板和JAR。新旧109字节NBT完全一致。独立窄审实核冻结身份，无必改项。
- 修复树JAR：2,637,995字节，SHA256 `0c54ddba94777ba45bfa1605410c43005fe6ace33aa80e9ce154241557dfc17d`。PM确认main全部`src`及构建输入Git内容相同后复用该JAR到主目录`build/libs/create_nuclear_industry-0.1.0.jar`，原制品保留在`build/artifact-backups/reactor-runtime-coolant-03/`。未将本JAR覆盖到美术或可变尺寸树。

## 下游与人工边界

美术08已可沿本共享提交执行唯一最终组合打包，再补同一次审查绑定及其视觉清单；不需要重跑既有17项、旧L1/L2/液体矩阵。窗口内面CT和铅灰SVG归08，本共享提交不包含它们。

PM随后核08精确净来源，单独同步必要窗口Behaviour、专属测试、唯一atlas与SVG来源到可变尺寸候选，再执行一次必要组合assemble，保留原变量尺寸冻结JAR/manifest为历史并补记当前身份。其他07B-R2素材、玩法不混入。若尚未完成这一步，可变候选就仍未带08窗修复，不能把本共享同步描述为完整08视觉制品。

本批不新开单独液面人工流程，不关闭ART08或可变尺寸01的手测门，不推进其他主线，不覆盖用户存档或回写候选存档。
