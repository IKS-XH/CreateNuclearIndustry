# ART-REACTOR-08 独立规格/质量窄审

## 预检查

窗口CT与资产预检查未发现生产必改项；同一次审查的组合候选最终身份与结论见下文。本轮以6042fd2及既有未提交候选为基线，仅审Behaviour/Test两源、08新窗口SVG/专用producer与唯一atlas，不重审L1/L2或液体生命周期矩阵。已读活动卡新增预检查节、AGENTS、美术职责/入口、治理1.2/5.1并应用minecraft-modding/testing/resource-pack和systematic-debugging；保持MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。

生产修复只把实际窗口内面映至反向可靠外面作为校验平面，实际外面优先；原生Create.buildContext仍接真实渲染face，不复制另一面索引。getDataType和connectsTo共用选择；两格真实localID、expectedID、位置/实际bounds外平面、dimension/owner/generation/revision仍受原判断限制。canConnect和sharedWindowFaces保持，不扩大实体壳内面或共享接口遮罩。原gather作用域只capture一次，同context读取并finally恢复/清除；本批没有扩大生命周期、缓存或同步。两源SHA与consumer/scope.json一致。

新增四回归调用真实生产Behaviour与Create上下文：六面内外完整索引54；SOUTH内面缺角53、仅上右缺角17、有角20、独立对角0；不同owner/generation/revision/bounds、本地替换、缺外面证据、边角实际外面优先及壳内面原回退。原consumer/red为21项中3个真实断言失败，green原XML为21/21、errors/failures/skip0、exit0，实际日志22秒；未运行新测试。

资产规则与实际像素核查：47原生上下文/17隔离fallback索引与旧R1 mapping完全一致。所有SVG半透明pane rect仅#66696A及64/255显式fill-opacity。已安装128×128 atlas与旧备份逐像素比较，9280个原alpha0像素全部变为(102,105,106,64)，7104个原框/角像素RGBA全部保持；generated与安装字节一致，1458 bytes、SHA256 f18296427db4c106579f778e3b0443284d5fd074ebf997889d13c495f1874723。producer限制整数直属rect、固定色板/透明度/覆盖及原mask，并核旧source SHA；复用assets/check.log、install-check.log，不执行生成器或重导其他图集。

Minor说明准确性：mapping与export_window.read_mapping的pane.scope当时仍写“只覆盖47个原生上下文中的原透明窗格”，实际17个隔离fallback中心的原透明窗格也着色；作者失败修正报告已准确说明。负责人已协调原作者仅修合同文字；本轮最终实读两处均为“只覆盖64个索引的原透明窗格（47个原生上下文及17个隔离fallback）”。SVG/PNG未变，Minor已关闭。

已独立查看assets/preview-before-install.png，单层/双层铅灰均保留内部示意辨识度；这是浏览器离线混合，不能代表Minecraft透明排序、光照或GPU视觉通过。真实对侧内面CT、铅灰玻璃质感、库存变化整液稳定仍需用户原存档观察。

## 同一次审查的最终绑定与结论

实际读取本树docs/reviews/2026-10-12/reactor-runtime-coolant-03/HANDOFF.md和REVIEW.md，复用主PM共享1/1真实GREEN、17/17相关回归及独立窄审，不重跑共享矩阵。公开服务端setCoolantInventories(long,long)仅更新真实mB库存，在可靠服务端结构/L1/L2身份成立时保留上一完整descriptor对象；不推进sample/revision/time、不中途投影或续租，下一成功正式结算才发布新完整样本。原通用setter、真实撤销、客户端State/Codec/Renderer/缓冲及美术消费ABI保持；最终javap也实见public void setCoolantInventories(long, long)。

本轮实读共享四输入：三个Java因Git净同步行尾而与修复树原raw SHA/bytes不同，LF规范化文本完全一致，已分别在final-preflight.json记录本树及PM身份；不可称跨树源raw逐字相同。109字节NBT逐字相同。本树实际编译共享五class、新旧NBT则均与本树build/ZIP及PM final-identity登记SHA逐字匹配。这是身份/ABI核查，不是重新审查共享实现。

消费者唯一组合assemble原命令/cwd/exit见consumer/final-assemble/，日志成功7秒、exit0，未执行test或重试。两艺术源保持原21/21 GREEN SHA。CT Behaviour class/build/ZIP一致；窗口atlas generated→安装→build/resources→ZIP四路一致，1458 bytes、SHA256 f18296427db4c106579f778e3b0443284d5fd074ebf997889d13c495f1874723。当前build/libs JAR与consumer/final-ART-REACTOR-08.jar完全相同，2624312 bytes、SHA256 a4b08dd0a5bebe72555dc44969553ede120080ef66ff2ab38d27974648d353c7，HEAD f787aa40a323d8fdd09a0f95e5798a9a2c8e3818。

final-net-art-sources.json精确艺术来源141项：Behaviour/Test两源、唯一atlas、136项新reactor-window-art-08工具来源及两报告；本轮当前SHA逐项吻合，未夹带07B-R2或共享输入，主PM四输入另列。07B-R2五源/五class/稀疏PNG及2616697字节原JAR备份实核原身份保持；旧1113资产仅窗atlas变化复用消费者ZIP差异证据，不建全仓矩阵。详细绑定见consumer/final-binding.json；最终manifest为final-freeze.json，SHA256 1222868a903111c4a419adb0a8b416e9abf47532a10a74c34a0abd19fa2e775b。原阶段二freeze、RED/GREEN及旧报告历史字节保留，负责人活动文档不纳不可变身份。

最终结论：本批规格/质量及组合制品身份窄审未发现必改项，说明Minor已关闭，可交负责人安排游戏试用。真实对侧内面八向CT、铅灰玻璃的内外排序/日夜光照、持续真实库存fill/drain时整液稳定仍待用户原存档观察；静态预览、自动合同及包身份均不替代GPU或游戏视觉。审查没有生产/Git/游戏/存档写、生成/测试/构建或子委派，仅更新本报告，已停止写入。
