# 屏蔽装配室15C工具

## 几何与资产契约

`assets.json`给出八个新增生产分件及候选SHA；`rig.json`固定组名、原元素索引/名称、真实pivot、互斥动作域和父级。原三个partial的86元素精确分为11/18/11/18/12/8/4/4，各原元素只归一个组，UV/纹理槽不改；原壳体、物品、九PNG和R1窗层保持。

所有模型保留北向主控原点的0..32坐标，不做格偏移。位移/pivot消费前除16，角度单位为度；先本组pivot旋转，再组位移/父升降，最后绕主控x=z=8转朝向。工具pivot是执行杆顶面中心[10,20,14.5]、[22,20,14.5]。

制造域只允许左右Z向内0..14°/0..-14°、侧滑向内≤0.25；封装域只允许Z=0、向内侧滑≤1.5、Y短锁紧±6°，两域不得混成自由多轴组合。升降台Y0..1仅在双工具完全待机时使用；钳口左右沿X[-0.5,0.1]/[-0.1,0.5]移动，并继承父台Y。三base始终零姿态。

允许接合仅钢芯顶部与同侧肩中央socket、台锁圈与静止导柱。20～30°大倾角会碰原肩轨/工作台，失败证据保留；最终末端到台侧有约0.086模型单位空隙，不宣称接触工件。实际GPU可辨动作与运行输入由客户端/预览部分分别验证。

生成/检查/安装入口依次为`produce.py --contract`、`produce.py`、`verify-assets.py`、`produce.py --install`。本批已完成，不重复运行旧素材生成或大扫掠；证据在`build/reports/art/ART-SHIELDED-ASSEMBLY-15C/assets/`。原C/R1/15A素材及历史证据只读。

## 客户端与同源预览

正式动作在`src/main/resources/assets/create_nuclear_industry/shielded_assembly/animation.json`，绑定rig SHA c2a25527…，制造16关键帧、封装12关键帧。Java `ShieldedAssemblyAnimation`与网页`preview/motion.mjs`读取这一份JSON，使用smoothstep、模型像素/度及同一F*D顺序；原物品保持静态。动作数据SHA f94c46fd1fb8d4c1bf99a7fa1553953bb6e9cebc3ae26a47eb3be77ff32cb47f。八生产分件和旧C正式PNG直接读入本地block-model-renderer2.20.0/Three180，库顶点减8由+.5/scale1/16补偿后才应用局部变换，外层场景偏移[-1,0,-1]。

`ShieldedAssemblyRenderer`只读当前主控/expanded、WORKING、实际RPM/过载、原state.operation/progress/work/ready/outputEmpty，不写账本或新增送包。弱BE键与弱世界身份缓存隔离每机时钟；原256工时同步间隔内最多外推256且不达到完成值。输出先于滞后一tick的WORKING；停止/过载/真暂停保持姿态，恢复不追停机时间，超过20tick未渲染/坏时间/换批重建收到的进度基准。原生资源重载会重建Renderer，构造读取当前资源管理器，坏JSON回原三个待机partial。ClientEvents仅一处中文数量注释更新，注册逻辑保持。

4195为普通exec托管前台会话54375，PID101936，Node `D:/Program Files/nodejs/node.exe`，cwd为本工具；绑定127.0.0.1固定白名单，原4191～4194保持。网页工序/32～256RPM/运行/暂停/过载/进度拖动只是示范输入，不连接游戏；完成不自动开新批。示范工时25600/12800取自现行制造/封装配方。`window.shieldedAssemblyAnimation15C.getState()`只读身份/阶段/矩阵；保存按钮只写本批preview目录machine/item两latest图和browser-evidence.json。

自动证据位于本批client/preview目录：原clock9项GREEN、同步到达双计整改1项GREEN均按阶段保留；原red-pose受外部中断未得退出/断言，不计RED，recovery实际7项5失败；当前两专属测试完整17/17 GREEN。实际模型角点/四向证据只补受影响Pose导出方法，分别1/1通过；没有旧R1/全量业务测试或全build。52文件有限HTTP同源、72组原生姿态矩阵、36四向矩阵及29 JS clock步骤通过；GPU由负责人独占IAB实看，游戏内动画门独立。

`preview/build-client.ps1 -Reviewed`只在负责人GPU及原同次组合审查无必改后调用一次assemble --no-daemon；602原包2812546B/SHA602985f8…已精确备份，日志/E进程tmp及最终差异核只写15C/client。失败保真实日志与原包，不机械重跑、不手工造JAR。`preview/verify-jar.ps1`核九新增资源production/processResources/JAR、当前class、全部非本批旧载荷与无测试/探针泄漏。

同批GPU窄修仅scene相机/剖面：目标与Z=16剖面随实际整机S*F移动，物品中心保持。原52HTTP与首轮East裁图保留，修后仅scene语法/1HTTP/四向函数检查（scene-camera-identity.json），无需重启服务或重测Java/动作/rig。

目标修正后的第二轮East仍裁边，原因是349×475窄画布原横向视野不足；最终scene47a60f43…按实际库mesh世界Box3八角的view投影同时适配两轴，10%边距。root实际E/S/W完整入画（withinViewport=true）及北向正常窗两工序通过；两轮失败/原52证据保留，游戏内动画待用户验收。

原同次组合审查通过后唯一assemble实际10.5092882秒exit0，候选2844567B/SHA dd6e25b8b04be4567f8d55d24918d75fad2c37a26ea41fb2c7675e420c6a9414。制品9资源/567当前class同源，555旧class/1953非本批旧载荷保持（1953含class）；核脚本首漏Renderer编译生成 $1 的真实失败保jar-first，native opcode相同/行号50→75及当前class→JAR证明后仅修分类，同一包核通过，无第二次assemble。用户Minecraft动画门保留。
