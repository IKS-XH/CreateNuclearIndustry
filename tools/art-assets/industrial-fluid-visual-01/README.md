# Create 原始来源与本地视觉派生

锁定依赖 Create 6.0.10-280 / Minecraft 1.21.1；JAR路径、SHA256、每件原模型/纹理SHA见 sources.json。模型通过原Create parent继承，保持原几何/UV；仅本模组命名空间覆盖纹理变量。原作者：The Create Team / The Creators of Create，原模型保留上游 Made with Blockbench credit。

版本出处：https://github.com/Creators-of-Create/Create/tree/mc1.21.1-6.0.10

准确版本许可：https://github.com/Creators-of-Create/Create/blob/mc1.21.1-6.0.10/LICENSE.md 。原assets为All Rights Reserved，原代码为MIT；许可证原文保留于 CREATE-LICENSE.md。当前锁定JAR没有自带LICENSE entry，不能从JAR臆测MIT覆盖美术。用户明确要求在本地候选使用原模型与调色贴图，本专项记录本地派生来源，不声称获得对原资产重新授权，也不改变项目许可证。PNG内保留署名/出处元数据。

运行独立工具 produce.py 从锁定PNG按像素亮度调色，生成 svg/ 下整数像素rect；最终PNG由这些SVG矩形解析导出，尺寸与alpha保留。钢灰保留原亮度层次并加少量冷色，陶瓷略微提亮。原 valve_open/axis 等未调色标记继续引用实际Create资源。output_arrow.svg 是本批原创只读出液方向标记，四个法兰侧面使用同一箭头；设备原轮廓不重画。

本工具只生产五新增ID及 industrial_fluid_visual_01 目录；不调用已有全设备生产器。生产与验证使用本机 bundled Python/Pillow，不使用AI图像生成。依赖的具体路径随本机工具链变化，源模型/纹理的真实加载检查由 IndustrialVisualContractTest 执行。

01R2展示修正：两管的零/单连接/两端直线互斥采用原fluid_pipe/item完整段；弯头/分支保留原中央multipart和RIM，仅原shouldDrawCasing条件成立时补原casing。材质0按实际父模型来源绑定，casing取pipes，连接中央面取pipes_connected，不根据目录深度猜材质。碰撞仅匹配这些模型的管身/法兰/壳包络。

箭头PNG为8×8，但Minecraft模型UV始终使用完整[0,0,16,16]。生产通过真实SuperByteBuffer的orientArrow入口旋转；回归调用实际FaceBakery并定位PNG不透明头部与杆部，再读取实际Flywheel变换矩阵核六朝向、正负转速，不以名义UP矩阵替代glyph证据。
