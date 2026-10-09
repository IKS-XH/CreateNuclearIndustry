# ART-REACTOR-02R1：PM范围补充

日期2026-10-10；维护者主逻辑PM。美术负责人已转交用户客户端反馈：02B成型后颜色/线条变浅、凹凸感不足，窗口仍有内部逐格框。原02B视觉门未通过，冻结资产/证据保留。本文件是既有连接纹理目的的必要定向整改授权，不修改反应堆玩法或L1接口。

## 核对依据与类型补充

PM已读取美术树`docs/art/tasks/ART-REACTOR-02R1.md`、原消费合同、现有连接判断及锁定Create的`AllCTTypes-javap.txt`/`Omnidirectional-javap.txt`预检。当前RECTANGLE仅四向和固定8×8透明孔不能保留真实凹角并去除完整窗组内框；窗口采用Create原生OMNIDIRECTIONAL、sheet8及角上下文。执行者仍须以真实`buildContext`/`getTextureIndex`核对映射，不能只用自写公式互证。

授权补充仅为：14项target ID保持；其中13项仍RECTANGLE、4×4格/64×64像素；只有`reactor_window`变为OMNIDIRECTIONAL、8×8格/128×128像素，每格仍16×16。47有效原生上下文和17不可达回退槽分别登记。正文中的固定窗口中央8×8透明、所有14图固定64×64及全四向要求在本批窗口范围由此替代，其他合同保持。

## 精确可写范围

- 美术树既有`structure/client/ReactorConnectedTextureBehaviour.java`、`ReactorConnectedTextures.java`和专属`ReactorConnectedTextureTest.java`，仅窗口类型、材质域及八向邻接消费。
- 新独立`tools/art-assets/reactor-ct-r1/`及14张`textures/block/reactor_ct/`游戏图，均由新SVG确定性导出安装；旧ART01/02A/02B源和冻结证据不改。
- 唯一实施报告`docs/art/reports/ART-REACTOR-02R1.md`与本批`build/reports/art/ART-REACTOR-02R1/`证据。美术负责人维护其art任务卡/登记；PM共享文件不属于执行者写集。

L1/API/AT、CT事件入口、JSON模型、注册、语言/Ponder、玩法、存档及其他设备仍只读。美术树唯一工作目录；不合main、不Git写、不启动客户端，最终集成仍由PM管理。

## 不变量与验收边界

窗口只连接窗口，可允许同外平面中两个切向轴各相差1的对角；法向差0、轴向跨两格拒绝。其他13表面保持四向，仍可连接窗口的背景绘图域。不能泛化distance≤2，也不能把不同owner/失效表面当合法窗组。

原同世界上下文、owner/generation/revision/bounds、实际局部ID及可靠快照验证保持。只复用原生角门控和模型一次快照，不新增世界扫描、同步、替代UV或跨线程可变数据。

按实际根因作专属red/green测试；13项材质与新窗口预览由美术负责人核对后安装。一次最终定向测试与增量JAR、一轮独立规格+质量窄审，核对SVG/安装/JAR字节一致及原生映射；复用L1未变证据，不做全量或旧存档测试。用户只复看本批视觉失败项与受影响回退，不追记为已通过。燃料烧结炉06教学并行，两个写集不相交。

本文件实际复制到美术树后即可据此派发窗口段；无需等待聊天回复，也不把等待时间当授权。出现真实API/共享逻辑缺口先报告PM，不自行扩展。
