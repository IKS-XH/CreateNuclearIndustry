# ART-ACCIDENT-CORIUM-22-R1 限定复审

2026-10-23。**R1实际差异范围内无未闭阻断，未发现必须先整改的问题。** 新碎壳构图已与首版同款、同向切角块及flow四份同图明显不同，可交用户复看；这不是用户认可R1或Minecraft视觉验收。

## 本次范围与依据

按22卡R1段，只复看四PNG／四SVG、flow mcmeta及专属produce／verify／mapping／manifest增量；首版Java、atlas、模型、废料、其余mcmeta和预览框架接线复用原审查，不重新展开。已读R1 ASSETS、OBSERVATION、交付README顶部R1入口，`build/reports/art/ART-ACCIDENT-CORIUM-22/r1/` 的baseline、verification、manifest、produce／processResources／verification原始日志及退出记录，并读取当前生成／验证源码和flow元数据。

继续按实际已读的 `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`、`minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md` 核受影响资源及证据分层，服从主工程治理5.1。来源仍为同事故树 `1985d1b3172a5efd1f0c87f3aa39ce3fe2a6feb7`；本审查仅写本报告，未运行任何生成、测试、构建、哈希、自动校验、HTTP或浏览器，无Git／世界／其它文档写入。

## 构图与半幅读感

已独立view_image查看R1正常cold／hot／still／flow、暗光cold／hot及三张裁切平铺小样。

- 冷／HOT／still的六块手工轮廓有宽断板、斜短条、凹缺折角、纵片、小块和边缘撕裂片，面积45／10／17／19／5／15像素；尺度、长轴和缺口不同。实际图中宽板、细片、小块可区分，液相通道有宽窄和分叉，未继续采用首版同向切角块的均匀排列。高光仍按同一上左方向生成，改的是轮廓，不是各自旋转光照。
- flow为独立32×32底版，九块形状／尺度错落的金属片分散在橙红液相；实际完整帧与小样未见首版四份相同16×16图。现有四象限身份各异，横／纵方向均不再是16周期复制；这些静态量只证明构图差异，审查没有把它们当作审美认可。
- 三小样分别为帧0左上16×16的3×2平铺、左半幅16×32的3×2平铺、完整32×32的2×2平铺。已看实际PNG中的宽板、窄片和钩形分区，未见必须先整改的同向三角列或单一重复切角块。小样是生产帧裁切／重复的素材展示，第一张采用完整高度侧面范围，不等同当前独立8/9液格或任意邻居高度的游戏画面；也不据帧0推断所有相位Minecraft液面已验。

## 动画、身份与已有有限结果

当前flow PNG为32×1024，mcmeta明确32×32单帧、0…31顺序、每帧6tick、无插值，完整192tick／9.6秒。produce使用同一独立32底版逐相下移1px；verify已删除首版四象限相同断言，改核真实32周期、象限差异与末首继续1px。冷热共用新大固壳与金属像素，still小壳仍在1px内往复；HOT／still原序列、时长及元数据保持。

现成唯一验证为PASS_R1_RESOURCES_ONLY、invocationCount=1、exit0：四SVG→PNG共35,072个RGBA像素零差，仍完全不透明；三小样8,704像素绑定生产flow帧0，零差。半幅切口数值逐相保留，未用人为审美阈值或旧同图断言强行宣布平铺观感通过。

唯一processResources为调用1次、exit0、BUILD SUCCESSFUL in 4s，1 task executed；五项新处理输出与当前生产源匹配。没有重compileJava，首版13秒编译及两个未变client class按现成身份复用；没有新测试或JAR。

baseline／verification登记13项保护对象保持，包括首版Java、atlas、六路由／模型、其它三mcmeta、废料PNG及SVG；48份首版快照保持。现有tracked差异范围没有扩大、src没有写集外新增。本轮没有重新扫描6,888项旧非批文件或世界，不把首版结果改称R1新运行。根manifest与r1/manifest均明示revision=R1、五项新处理输出、复用编译及自引用／后写文件排除；取件应使用当前R1身份，首版清单保留在before。

## 现成浏览器证据与交接

正常／暗光overview均ready=true、errors=[]，记录相同72300ms冻结时刻：flow实际帧17、next18，32帧与192tick已由原框架自动解析；HOT／still序列index均0，仍分别有0.3／0.375插值进度。四纹理及flow元数据输入SHA与R1清单一致。root通过原4210实际播放、暂停、逐帧和明暗对照，console无warn／error；恢复播放后继续到帧18。没有为R1重建预览或重跑旧HTTP矩阵。

暗光冷热仍随原环境变暗，HOT可凭裂隙颜色区分，没有新增全亮；原light12／getFlow ZERO及所有玩法不因本轮像素整改改变。原创源／工具／素材沿原MIT，未扩大第三方或旧候选许可范围。

本次只确认R1对应用户构图意见作出了实质变化且没有未闭技术阻断，最终读感仍交用户判断。原生游戏外观与创造拆残骸零掉落复看保持原联合候选门，不重开已反馈完成的04B整组功能；未补记光影／多人或用户认可。报告交root后停止写入。
