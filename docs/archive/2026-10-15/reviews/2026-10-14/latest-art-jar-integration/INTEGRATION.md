# 最新美术分支与最终JAR集成记录

2026-10-14。用户直接要求合并最新美术分支，并把最终构建JAR纳入Git。本次已完成本地集成与制品版本管理，未推远端、上传MC百科或关闭管理员03的人工门。

## 美术来源与实际合并

- 原main`c5328a6`，原art-studio`8a33893`。美术按执行者职责交付未提交来源，PM剔除两个Python缓存、日志后将1151条已交来源净提交为`88b8fd289d49df90d7e3c655b6e81381ad01130f`。
- 游戏内汽轮机、反应堆美术及Logo此前已按独立验收净合main。976条现有来源与主目录逐字一致；166条缺项补齐到main`7e02b76`，只涉及美术文档、可编辑／宣传素材与历史预览。
- 新增宣传交付为厚重R2高清Logo、恢复钢灰棒体并充满冷却液的R1 MC百科封面。原12-HD/13内部审查与原始导出证据复用，用户本次合并指令授权Git接入，不补记未明确报告的逐项视觉结果。
- 9条差异中，旧美术ModCreativeTabs缺主目录的五项管网物品，保留现行main；主目录已更新的窗口08五个工具、PLAN/README及11历史说明也保留，通过PM入口追加当前状态。没有以旧Tab、旧JAR或旧工具覆盖主线。
- 完成上述已验净接入及新增缺项补齐后，双亲合并提交`48dc8d153180078f081307f8667b47f04eb57dc4`承接`88b8fd2`历史，使用ours策略保留已整合当前树。art分支实际是main祖先；相对本轮起点src、build.gradle和gradle.properties无变化。

## 干净源码构建

从合并提交48dc8d1的Git树`b78aeb594d454f94f7bad5f51fa7300640439242`导出源码到本批被忽略的报告目录。不创建新工作树或分支、不复制／启动用户存档，主目录未提交工业逻辑和模拟器等改动没有进入正式包。

首轮有PM操作时序失误：源码ZIP导出／解压尚未结束即启动构建，访问转换器未按完整源码初始化，三个RenderChunkRegion.level访问在编译时报protected错误，8秒exit1、未到测试。已保留原日志／退出记录。等待实际导出进程退出后，确认访问转换配置逐字一致，再以同源码、同命令运行，未改功能／测试／构建脚本。

有效命令为Java21下`.\gradlew.bat build --console=plain`，实际**52秒exit0**，108份JUnit XML共612项：**609通过、0失败、0错误、3跳过**。跳过均为ReactorSodiumSurfaceTest：标准构建未指定cni.sodium.fixtureDir，原assumeTrue按设计跳过显式真实Sodium夹具检查；没有伪记通过或重开已验客户端兼容门。本轮未运行Minecraft／GameTest，也不宣称新的GPU或用户世界验收。

## 正式JAR与Git身份

- 正式路径：[build/libs/create_nuclear_industry-0.1.0.jar](../../../../../../build/libs/create_nuclear_industry-0.1.0.jar)。
- 大小：**3049912 bytes**。
- SHA-256：`26cb18d0dfb09e050b7bbee6de7b4c86912680bb9e158d8b58ece4f8f2182536`。
- 制品提交：`4290bcbd3cf68f3dbcb2f9b813e5ddbe89b7a9e9`；Git blob `afc54dc86a842d2168a34b241e7f5e232824d390`，与当前文件hash-object完全一致。
- JAR中的609个class与1543资源逐项等于干净构建输出／处理资源，零漂移；JarTask生成MANIFEST单列，ZIP共2153文件载荷。原生metadata、Logo两资源和工业泵class存在，没有未合管理员03的GUI／测试域条目；不扩大成历史全部GameTest均已排除的声明。
- 原主包34bedc81…完整保留为本批previous-main-34bedc81.jar；当前副本final-main.jar与正式包逐字一致，管理员879c候选未动。

仅精确force-add该JAR，保留用户现有ignore修改；它现在是跟踪文件，后续构建修改可由Git显示，其他build产物仍忽略。后续正式版本更新需在匹配源码验证后替换并提交同一路径，不自动夹带其他包或缓存。

## 证据与保持范围

原始证据位于主目录build/reports/latest-art-jar-integration：art-inputs.json、main-dirty-before.json、build-source.json、首轮失败日志／退出记录、最终build.log／build-exit.json、最终XML及final-package.json／final-verification.json。

主目录原107个未提交文件逐项SHA保持，日志、配置、世界、模拟器和其他任务均未收纳或重置。美术树原日志／两个Python缓存原位，未删除分支或工作树。管理员03源码仍只在原候选，01／01R／03人工门及02等待状态保持。
