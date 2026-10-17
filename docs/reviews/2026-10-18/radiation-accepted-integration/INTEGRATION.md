# 辐射首批及头罩手测验收与合入

2026-10-18，主逻辑PM按用户原集中清单全部通过、头罩补充再次手测通过并要求继续开发的反馈，关闭第一批人工门。只记录用户反馈，不冒称PM亲自重跑；第二批事故体验仍独立验收。第一批[原卡](../../../archive/2026-10-18/plans/2026-10-16-radiation-foundation-01.md)和[本次集成卡](../../../archive/2026-10-18/plans/2026-10-18-radiation-foundation-accepted-integration.md)归档，现行结果只见[STATUS](../../../STATUS.md)。

## 来源与冲突

main `020cae4ba3d919ea90b05cfc305dade27c5191bb`与已验辐射`cc7d4fdc5918d833f40ad0a5e289ea85dfbf8f38`在同级干净树合为生产来源`f819fcfadf75509c7b23899daa9a7b9c59dbc56b`。没有Java冲突；两语言及五护甲标签由唯一执行者按双方准确Git对象取并集：各707语言键无改值/重复，四槽标签各2值、饰纹8值，`replace=false`，贫铀内容保持。仅这七JSON有手工资源处理。

2278输入来源证明为2103双方相同、114保main、51原辐射、7JSON并集及3准确自动合并（Gradle、模组入口、创意栏）。S1/C1、贫铀外观/装备、队列崩溃修复等main内容保留，头罩原配方和护目镜接线不变。[一次组合窄审](REVIEW.md)无新增发现。

## 验证与正式包

复用原辐射41项JUnit、13＋2真实服、原组合审查与原候选AFAD…，头罩1项定向JUnit、assemble及窄审与74950D69…候选，以及用户两次手测反馈。集成仅新增一次必要装备数据JUnit：辐射1＋贫铀4项共5/5通过、exit0；不重跑完整业务/性能/存档矩阵。

已提交源f819fcf导出2278项准确Git输入/11754430 bytes，原树和导出源构建前后未变；一次增量assemble、10秒、exit0，未clean或强制重跑。原输出同字节包 **3478572 bytes**、SHA256 **`81E7CD98D589DDC9C0AAF398C06776DC4318EA54C70CD19376F0C127E4A3DE7B`**。main快进到该生产来源后才复制到正式路径，读回SHA一致；继续精确跟踪Git，不在main无关dirty上构建。[来源、命令、退出和制品身份](ARTIFACT.json)为本轮实际记录。

旧main2382→新包2435个真实文件，新增53、修改31、删除0、2351原字节保持。748class逐项同编译输出，1686资源逐项同处理输出/准确Git源，另含manifest；辐射/贫铀测试class、域和模板不入包，META-INF保持。

31项修改包含原S1/C1的14class：上一包由无局部变量调试表的独立javac生成，此次标准Gradle包含调试属性。严格核验首次因此停止并保存失败；只补归因、不改源/包或重复assemble。14项在内存去除调试属性并重建常量池后完整class规范字节14/14相同，对应Java仍为main原blob；不是性能逻辑回退。其他103项关键main载荷逐字保持。原始javap、两侧class与规范比较证据完整保留。

## 保护与边界

复用树旧世界/config完整备份后，携带已保存退出的main世界27文件、13337721 bytes，路径/长度/SHA零差异；用户新辐射手测世界留在原候选，未覆盖或回写。自动检查只用隔离世界，本集成未启动用户世界。main47项原dirty独立核SHA保持，正式包按本次授权更新，不夹带其它内容。

原始阶段A/B报告、源/载荷清单、失败日志、世界备份及dirty记录在`Create_NuclearIndustry-du-accepted-integration/build/reports/radiation-accepted-20261018/`。实际应用modding/testing/resource-pack/ci-release及治理5.1，锁定依赖未改；不推送、发布、安装生产服或删来源树。本次合入只交第一批源/剂量/屏蔽/防护与护目镜头罩，事故破坏、残骸和新废料仅由[第二批计划](../../../superpowers/plans/2026-10-18-accident-execution-02.md)接续。
