# ART-REACTOR-03R2方形控制棒定向模型整改

候选仅保存在本目录：19份16×16整数rect SVG、rig/palette/mapping/生成器，33个OBJ/MTL/JSON与纹理产物。全部原创，参考本项目钢板/钢格栅燃料架配方、钢杆/吸收陶瓷/黄铜板控制棒配方及冷青/热橙色族；没有复制外部Mod。03A/03A1/03B原始冻结证据保持，只读旧CT与原流体纹理。03R1模型/材质与实际位姿/合法空气采光是已冻结历史候选；03R2仅控制杆方形截面，原动画消费与所有SVG/PNG保持。离线预览和自动检查不追认客户端视觉通过。

## 单位、分件与未来路径

OBJ以方块为单位，Y向上，16模型单位=1方块。静态燃料模块X/Z0..1，底板Y0..2/16、顶板14/16..1；九管轴心为X/Z4/8/12单位，八边外接直径3.5单位，Y2/16..14/16。每面只写一个外向绕序面，无反面副本；显式法线和UV，flip_v=false。钢板/竖管闭合，三格堆叠板接头封闭。

`fuel_rod_glow`只有九管侧面，不含板。每管半径已在OBJ中加0.0005方块偏置，轴心保持4/8/12，Y与同批钢管一致；**消费者无需再次缩放**，尤其不能全模型XZ缩放挪动九轴心。灰钢静态面保留，独立局部透明蓝带通过真实列HU/t控制亮度/顶点alpha；0/未知应不画。emissive_ambient只记录候选OBJ设置，不证明世界照明或最终运行材质已接通。

`control_rod_shaft`规范Y0..1方块，方柱X/Z0.2..0.8、截面边长精确0.6方块（9.6模型单位），轴线X/Z=.5；只按真实body长度缩放一次。`control_rod_head`固定Y0..3/16、方箍X/Z0.175..0.825、边长0.65方块（10.4单位），黄铜只用于端箍。shaft杆底平移到`capY-bodyLength*actualDepth`，head从杆顶`杆底+bodyLength`起；depth改变位移，不能改变shaft长度；head不缩放。枢轴(.5,0,.5)。预览驱动器仅用原壳纹理示意静止，未改正式模型。

静态消费ID仍`create_nuclear_industry:block/reactor_fuel_rod`；三个partial ID是`create_nuclear_industry:block/reactor_animation/{fuel_rod_glow,control_rod_shaft,control_rod_head}`。OBJ/MTL以后安装至`models/block/reactor_animation/mesh/`，JSON的model引用与OBJ同名MTL相对引用均按该路径生成。三材质目标`create_nuclear_industry:block/reactor_animation/<name>`；工具generated路径与将来游戏目录的对应由mapping明确记录，**本工具没有安装入口**。

## 冷热帧与连续混色

冷/热各8份SVG及图块，16×128帧表竖排，frame0..7、每帧16px。每次相位前进2px，空间16px周期，07→00也平移2px；低对比波纹不遮棒。帧mcmeta所需animation字典记录在mapping，未来安装任务再写正式mcmeta，本段不另扩产物。

SVG只有alpha0/255：钢、控制棒、冷热作者源全不透明，辉光源局部透明。流体透明由消费者顶点色/alpha提供。一个合法EMPTY/CONTROL_ROD内部空间网格按`(coldMb+hotMb)/capacity`定统一液位、`hotMb/(coldMb+hotMb)`连续混色；不叠冷热完整双盒、不固定上下两层，不填燃料列，也不能从outer bounds猜合法空间。0库存或容量不可用不画。离线混比0/.5/1及示意0/.18/.42/.65辉光不是正式运行接口或HU/t标定。

## 复现与定向校验

```powershell
# 每条shell workdir固定 E:/MyMC/NewMod/Create_NuclearIndustry-art-studio
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/reactor-animation/generate.py
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/reactor-animation/generate.py --check
```

生成器只读共享严格SVG renderer，先校验全部19源、色板、帧循环、rig与固定引用，内存生成并核对面法线/绕序/UV后再写出；产物相同不改字节或mtime，写失败恢复已有候选。无Java/构建/服务/浏览器启动或Git操作。

证据`build/reports/art/ART-REACTOR-03A/`中preview.py实际解析保存OBJ的v/vt/vn/f，做正交三角形深度和UV采样。模型/辉光/棒深/透窗四PNG与8帧GIF均为离线示意；窗口只读复用02R1已审图块，右侧围壁与驱动器是原材质预览参照。流体只画示意EMPTY/CONTROL_ROD空间，未把燃料列计液。模型投影、渐变与动画预览不能替代客户端渲染、真实HU/t、实际深度/卡死或库存同步验收；03B已有正式真实运行消费；本批材质与管径离线对照不是客户端采光/透明排序证明，运行整改仍待用户三项视觉复看。

03R1只重画steel/control两份SVG，采用原色板增强钢灰棱面/纵槽、窄边沿与分段箍纹，黄铜限固定端箍；另17份SVG（蓝辉与冷热16帧）原字节保持。原/新对照以本批baseline中的实际OBJ/UV、原版GUI与手持变换为依据，固定同光照/缩放，输出仅新03R1证据目录。

03R2模型阶段只改rig/generate/本说明与控制shaft/head两OBJ；其他31generated字节保持。方柱四侧及两端封闭，沿用control_rod SVG的钢灰纵槽/箍纹与黄铜端部UV区域；不改任何19SVG、palette、mapping、PNG、MTL、JSON、燃料/辉光/冷却液或原六Java/两测试。实际旧/新OBJ/UV三视与0/50/100%行程对照在本批03R2证据；三张模型图已由负责人实际复看，两正式OBJ已逐字节安装；主PM共享补充已生效，控件按冻结六面位置/尺度同批组合交付。工具本身没有安装或控件实现入口，最终来源/定向检查与JAR见03R2报告，用户视觉门仍待确认。
