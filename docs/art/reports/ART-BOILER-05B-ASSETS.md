# ART-BOILER-05B 素材候选报告

已完成A横箍压力舱的离线素材候选。源稿与预览位于[boiler-art-05](E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/tools/art-assets/boiler-art-05/README.md)，浏览入口为[preview/index.html](E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/tools/art-assets/boiler-art-05/preview/index.html)，固定映射见[mapping.json](E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/tools/art-assets/boiler-art-05/mapping.json)。

候选含12张16×16基础SVG/PNG、8张RECTANGLE图集及1张OMNIDIRECTIONAL窗口图集，共21张目标PNG。基础面中7项沿用已批准05A作者SVG；换热器面/顶、阀体材质、独立小安全牌及透明单窗由本批整数rect SVG绘制。颜色沿用A稿钢灰和暖金色板。端口CT保留对应基础SVG中心功能轮廓，只替换外周边缘；隔层强调为厚钢压边和窄暖米色嵌片。阀体贴图是通用钢质材料，不会在四段几何上重复绘出完整阀体。

9个候选CT target固定为 block/boiler_ct/{side,top,partition_side,water_port_face,steam_port_face,controller_face,hot_coolant_port,cold_coolant_port,window_frame}。RECTANGLE上下文和窗口47个有效八向索引、17个回退槽来自只读R1的Create 6.0.10锁定表，mapping保存了参考文件SHA。窗口孔为透明中区、细反光；单窗、2×2和L形缺角示例使用与R1消费者相同的八向mask及对角门控，内部共享边无框，外周和缺角保留法兰。

负责人已在浏览器复看最终基础纹理、CT图集和单窗/2×2/L形预览，要求的端口轮廓、阀体/标牌区分和隔层条带调整均已通过该离线展示复看。此结果不代表游戏渲染验收。

后续模型消费建议仅供消费者阶段采用，不属于本批写集。基础sprite引用按mapping中的12项原模型地址保留。需要更新引用的现有模型包括锅炉壳体、窗口、水/汽口、控制器、冷热口、换热隔层和安全阀。当前窗口模型有18个elements：12个周边框条、4个玻璃面和上下2个内沿面。消费者应避免保留会在连窗内部形成实体横条的周边框及内沿；框/孔按完整16×16面和1:1 CT UV设计，不沿用当前3..13玻璃区域或0..10 UV。阶段二再按负责人精确写集修改；本阶段没有写模型JSON、Java或正式纹理路径。

最终检查命令在美术树根目录执行：

& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/boiler-art-05/check_assets.py

实际退出码为0。检查确认12/12基础PNG逐像素符合SVG、128/128 RECTANGLE图格和64/64窗口图格符合各自SVG、9个CT ID与批准集合一致、原生上下文逐项等于R1映射、透明中孔及2×2/L形边界成立。非法输入x=1.5被拒绝，错误为“坐标与尺寸必须为非负整数”。Pillow打印了Image.getdata弃用提示，未影响退出码或检查结果。原始命令/输出与一次已修复的生成器失败记录见[assets evidence](E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-BOILER-05B/assets/check-command.txt)。

最终PNG导出命令同样使用上述bundled Python，脚本为tools/art-assets/boiler-art-05/build_assets.py，退出码0。最初一次候选导出因错误地把SVG交给PNG读取器而退出1；改为解析SVG源后最终导出成功，原错误和最终输出保存在assets evidence目录。导出阶段未运行Gradle、未安装正式纹理、未修改模型/Java/游戏数据，也未执行Git写操作；之后按本报告“阶段一安装补充”安装了获批的21张纹理。

## 阶段一安装补充

按任务卡新增补充授权，将候选目录中的12张基础PNG和9张CT PNG逐字节复制到已核准的正式纹理目标，共21项。一次安装后比对逐项读取生成件与安装件的字节数组及长度，21/21完全一致，PowerShell命令退出码0。实际复制/比对命令和完整逐项输出见[安装证据](E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-BOILER-05B/assets/install-command.txt)与[安装输出](E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-BOILER-05B/assets/install-output.txt)。

本补充仅安装任务卡核准的21张纹理；未重生成候选、未重跑源检查，未改模型JSON、Java或其他PNG，未运行Gradle、游戏或Git写操作。模型/CT/API消费与后续构建仍由阶段二执行者负责。

