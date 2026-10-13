# ART-TURBINE-07B 独立窄审

审查结论：未发现本批生产制品的必改项，可交负责人作为游戏试用候选；不代表客户端视觉验收。实际工作树HEAD为6042fd2。已读取07B任务卡、实施报告、美术入口/PLAN、治理1.2/5.1，并应用minecraft-modding、minecraft-testing、minecraft-resource-pack技能；沿用MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，不运行生成、测试或Gradle。

实际差异核查：汽轮机模型/贴图目录中已有受跟踪修改恰为212机壳OBJ。逐个与HEAD只读比较，排除mtllib/vt后记录全部一致，因此v/vn/f/usemtl/o与原几何保持。新专用MTL仅映射casing/endcap至两张新PNG，inside/edge引用原panel；旧共享MTL、窗口、端口、控制器、轴承、叶轮、包装JSON及旧纹理没有本批差异。既有安装记录确认215/215候选与正式资源字节一致。

UV核查：build_candidate.py:206–226采用直径及外法线对应的全截面切向范围，:265–274使用实际Z作U、完整截面XY作端盖UV。实际代表d3竖面、d5顶面和d7斜面均为Z=0/1对应U=0/1；d5 front顶面Z=.1875对应U=.1875，未将裁短轴段拉满。邻格同平面使用同切向范围，端盖中心格实际UV=.4..6，而非每格0..1重复。不同八棱面各用本面0..1切向范围，转角为纹理压边，不能称全周无缝展开或动态CT。

六向核查：build_candidate.py:168–184的坐标/法线逆变换与旧turbine_models.py:1076–1082预旋转对应。实际五个其他朝向逆回up后，所有顶点最大误差0，vt列表均与up一致；没有将每个世界面局部Z误当标准轴。机身已有朝向旋转/模型选择保持只读。

复用原始证据：candidate-check-final-output.txt记录两SVG/PNG一致、212几何保持及引用可解析；install-byte-check.txt记录215/215安装一致；assemble-output.txt记录唯一增量assemble成功、4秒、退出0；jar-byte-check-output.txt记录JAR同一215条目一致、退出0。本轮另只读核实际JAR为2601212字节，SHA256为328072b13f941705fc86e8d62df7b13e6b90b1f9a3fc3851c34cfa279380167c，与交付记录一致。以上原始证据位于build/reports/art/ART-TURBINE-07B/，未重复构建或检查器。

文案两处旧阶段说明由原作者另行修正，不影响上述生产判断。游戏仍须观察未成型/物品与三档成型在既有朝向、光照下的束带方向、顶侧斜面连接、端盖整体钢铜读感及与未改窗口/端口的衔接；离线OBJ预览、nearest采样修正及打包成功均不代替该视觉门。本审查仅新增本报告，已停止写入，无Git写操作。
