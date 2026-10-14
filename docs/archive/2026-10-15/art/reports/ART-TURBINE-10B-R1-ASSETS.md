# ART-TURBINE-10B-R1：恢复原B材质的窄修候选

当前状态：负责人实看冻结候选后已批准并完成14变化安装；实际安装与来源/备份读回均退出0，详见末尾安装补充。以下候选阶段记录及原问题证据保留。

2026-10-12，原素材执行者按R1卡恢复已选B稿的SVG、纯钢分区与纹理投影。当前只交更正候选，生产资源仍为preR1已安装版；不Git、Java、Gradle、游戏/世界或子派发。薄叶片0.125格、两列中心/错相、完整扫掠、固定孔径和静态/动态拆分保持。原10B检查/备份/审查保留历史；本轮材质整改及离线预览不关闭GPU/用户视觉门。

## 原问题与身份冻结

实际差异是原10B producer另画了灰蓝亮条纹，并把它铺到原稿采用纯钢色的轮毂、轴、圈和支承。参照为原`B_rotor.svg/B_bearing.svg`与`preview.js`的`texturePattern/rotorMesh/supportPlate/plateMesh`；原B材质不是全表面贴图。负责人参照截图为`build/reports/art/ART-TURBINE-10B/assets/root-reference-b.png`，整改前页面为`root-material-before-r1.png`，均保留。

新证据统一位于`build/reports/art/ART-TURBINE-10B/assets/r1/`。先执行`freeze_r1.py`：`freeze.log/freeze.exit`退出0，13份旧工具/source、3份原B只读参照、26份候选、26份安装共68项复制SHA核对；清单为`pre-r1-identity.json`，文件保存于`pre-r1/`。preR1映射SHA为`a39ff51073c711fd3e5480b1fc972033f8211780182c8c6f7a5710b877855d0c`；原17份基线备份没有覆盖。

## 原B材质与atlas方案

叶片和外板SVG分别逐字复制原`B_rotor.svg/B_bearing.svg`至原四PNG ID对应的新source，原12/10色保持，PNG逐像素栅格化这些rect；原稿背景rect未写x/y时按SVG默认0处理。`turbine_10b_steel`现在是原B17个纯钢色的采样表，面UV取对应色块中心；`turbine_10b_copper`为纯`#b27a4e`。色块表不拉满实体，不再产生全圈条纹。

| 区域 | 原B材质/投影恢复 |
| --- | --- |
| 叶片两个端面 | 原B SVG；根部负弦角p[0]为原点，径向theta+0.063及切向基底，16px对应1格 |
| 叶片薄边 | 端尖`#a8b1af`、正弦侧`#454e50`、其余`#333b3d` |
| 阶梯轮毂 | 前/中/后`#7e898b/#657173/#596467`；肩面`#667274/#9ba5a3`，孔壁`#41494b` |
| 固定端板 | 只有外面用原B_bearing整块XY；内板/边/孔壁为`#4c5658/#626d6f/#424b4d` |
| 固定/旋转圈与箱座 | 原外圈/孔壁/内圈`#626d6f/#41494b/#9ba5a4`；支撑`#899395`、螺钉`#c8cfcd` |
| 轴与小铜件 | 转子轴`#697476`；输出轴在端板3/16厚度内`#858f91`，其余`#697476`；偏置铜钉及轴承锁圈`#b27a4e` |

10B已有轮毂整周环实体保持，但原B的rotorMesh仅有偏置小铜钉，因此该既有环恢复前段轮毂钢色`#7e898b`，避免保留原稿没有的整周铜线。输出轴钢/银区域用取色表两个一像素列和原Z投影实现，不添加轴的实体切面。

原B的pattern是一格周期，不能把整张SVG拉满每片，不能让sprite UV越出0..1或在网页私自repeat。先只读`study_r1_uv.py`并保存`uv-study.log/uv-study.exit/original-b-uv-study.json`，退出0：四种正式转子模型每个48端面均跨微负V；D3 Umax0.6802529203、D5 1.2530974808、D7 1.8259420445、未成型0.2405947191。实际只需V=0以及D5/D7的U=1切线，没有U=2/V=1。

负责人追加批准后，仅四份转子OBJ的24片叶片端面及tool厚度对照作这些共面切分；薄边只补对应共线分点并分成同色平面片，保持闭合。每片UV减去所在纹理整数周期后限制0..1；五边形仅作同向三角分解，避免NeoForge面角上限。未成型按照缩小实体重新计算一格纹理密度，不继承D3缩放后的贴图密度。新增点全部位于原平面/原边内，覆盖面积、方向与原面一致。

## 锁定NeoForge的直接V证据

实际读取的本地源为：

`C:/Users/IKSXH/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/21.1.219/eefeb66da85d5ced69c1d0bc86b8140ddff77c1/neoforge-21.1.219-sources.jar!/net/neoforged/neoforge/client/model/obj/ObjModel.java`

源JAR SHA256：`814983ff6c233935f176a99b10a495cfa37971ffe1a73a6f8b097e893553ca8c`。ObjModel.java条目SHA256：`aa420616da3599acc0b69a3a69d863d4f20f7be487d1e73b9718f61d880a7ed1`。实际375–376行为：

```java
texture.getU(texCoord.x),
texture.getV((flipV ? 1 - texCoord.y : texCoord.y)));
```

因此原JSON的`flip_v=false`直接取V，预览旧`1-v`翻向已改成直接V。原JSON没有改。精确路径/两个SHA/行文本保存于`neoforge-obj-uv-source.json`，只读369–379片段保存于`neoforge-obj-uv-excerpt.txt`；没有靠网页翻向掩盖正式UV。

## 候选与必要检查

最终`resource-map.json`冻结SHA为`238f377912d128ed1b8bbd7531aac16803ae3c133d05c53728d4f9b2ccb29704`。保持原26路径边界及四PNG ID，实际仅14项变化：10 OBJ材质/UV和4 PNG；11 JSON与专属MTL逐字未变。当前生产26文件逐项仍与preR1安装快照一致，未安装R1。

`produce.log/produce.exit`最终生成退出0；`check.log/check.exit`退出0，`checks.json`记录7项必要检查。它核原B SVG逐字及原12/10色、4 SVG→PNG像素同源、实际VT0..1及逐顶点原一格投影取余/外板XY/纯色分配、实际引用与flip_v、批准叶片切分覆盖/外向/闭合，以及全部非叶片位置/法向逐面与preR1完全相同、包络不变、26路径和当前安装身份。只复核4正式模型与tool对照合计120个被切分叶片组；原轮毂/轴/圈/支承闭合、扫掠和孔径证据复用，没有重跑旧完整7项矩阵或当前installedZ前提已经失效的旧检查器。

R1两次必要候选生成：第一版仍给10B既有整周轮毂环铜色，逐面对原稿后仅恢复该环钢色再生成最终版。首版`produce-first.log/check-first.log/resource-map-first.json`保留，其map为`0e473d39e450804fb5bff754c6adf86abe2768581ea6951d59c5b8a3cffeba43`。最终7项核对后，后续preview修正没有重生或改变冻结候选。负责人提供原reviewer对冻结R1材质/UV无必改的消息；验收及安装名单仍由负责人管理。

## 真实OBJ预览与Canvas窄修

`http://127.0.0.1:8766/`继续读取实际候选OBJ/专属MTL/四PNG，未重画概念几何。预览纯色从PNG色块采样；原B仅圆柱侧面那组离线采光保留，纹理端面/板面不统一另乘灰蓝阴影。修正直接V时有一次局部JS命名冲突，Node语法检查退出1，改名后`preview-syntax.log/preview-syntax.exit`退出0；候选未受影响。

负责人实看发现Canvas假放射线和三角对角线，问题截图为`root-candidate-preview-seams.png`。实际读source后定位：每个quad被人为拆两三角，det=0纯色仍clip后fill，且阴影又填一遍，共享边的抗锯齿掩膜重复造成缝。旧source保存`preview-seams-before.js`。窄修后纯色完整face一次填色；仅同色、同平面、成对共享的内部边补0.65px同色覆盖，孔边/外轮廓不画线框。纹理quad以前三UV作一次仿射映射提交完整face，使用同次brightness而不另叠阴影mask，不扩大到WebGL，也不修改模型。`preview-seam-fix-syntax.log/preview-seam-fix-syntax.exit`退出0；实际消缝效果等待负责人复看，不把语法检查冒称视觉通过。

本作者当前cua_repl可用浏览器清单为空，未伪造新浏览器截图；负责人已提供实际候选视觉证据并能从8766复看。生产安装另等负责人看最终预览、批准14变化路径；原17份备份及preR1安装快照均保持。

## 实际技能与范围

已读R1卡及新增切分权限、原10B卡，复用本对话已实际读的minecraft-modding/testing/resource-pack与verification-before-completion合同；本轮再次核resource-pack模型/UV/PNG要求并实际应用。Canvas异常实际读`superpowers/6.4.2/skills/systematic-debugging/SKILL.md`，按截图、source、原B对照与提交路径定位重复AA，再做最小preview修正。新增手写注释中文。最终组合检查/唯一assemble仍由原客户端消费者负责，本素材报告不覆盖Java/权威同步接口或游戏视觉验收。

## 负责人复看与14变化安装补充

负责人已实读本报告、原7项检查退出0与锁定ObjModel片段，并实际复看D5薄厚45°同视角、D7侧面、D3前内轴承和D5后外板，确认材质恢复原B，Canvas假三角线已在同视角消除；真实截图为`r1/root-thin-after.png`、`r1/root-d7-after.png`等。该离线复看不等同于用户游戏视觉验收。负责人随后在R1卡追加明确14路径及冻结map238f权限，原reviewer在同轮继续读实际安装证据。

本作者执行新`install_r1_frozen.py`，先读冻结映射和卡内14名单，并核全部26现安装与preR1 SHA相同；无并行写入冲突。将14原件完整复制到新`r1/installation-backup/src/main/resources/...`，全部14备份核对完成后再开始生产复制，只复制10 OBJ与4 PNG；11 JSON和专属MTL共12文件不复制、不改。

`r1/install.log/install.exit`退出0，安装清单为`r1/installation-manifest.json`，原始顺序事件为`r1/installation-events.jsonl`，记录26个preR1身份预检及全部14备份完成早于第一份生产写入。候选没有重生；映射SHA仍`238f377912d128ed1b8bbd7531aac16803ae3c133d05c53728d4f9b2ccb29704`。

安装后独立读回`r1/install-readback.log/install-readback.exit/install-readback.json`退出0：来源→安装26/26 SHA一致；14旧SHA→R1新备份14/14一致；11 JSON/MTL原样12/12；原17份基线备份SHA保持17/17。preR1快照目录未写入或覆盖，原来源/映射/候选/安装历史保持。

没有重跑已通过7项R1检查；其中安装前生产仍preR1的断言属于原候选阶段前提，当前安装身份以安装/读回清单为准。没有Gradle、Git、Java或游戏/世界操作。安装到位已通知原客户端消费者，最终组合测试/唯一assemble仍归其在主PM共享实际HANDOFF齐备后的职责。本素材作者追加此报告后停写，供原reviewer同轮核实际安装差异。
