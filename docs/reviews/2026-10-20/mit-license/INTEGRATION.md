# MIT许可与原创美术开放

用户于2026-10-20明确要求将项目开源协议改为MIT，并开放除Logo外的美术素材。基准main为`e6a21b7737ac81f183489590ee3610848aad9f1d`，准确生产来源为`3b7a7028cb24abec72a91ea7f001119228c29ba3`。本批不改玩法、版本、依赖、资源像素或第三方原文，不推送或发布。

## 当前声明

根[LICENSE](../../../../LICENSE)采用标准MIT全文；原创代码、文档和非Logo原创贴图、模型、动画、音效及其可编辑源文件适用MIT。根[NOTICE](../../../../NOTICE.md)列出Logo的运行/源文件/高清导出/宣传嵌入部分及第三方例外。Logo保留所有权利，原Logo导出/校验代码仍MIT；官方模组包供玩家使用及由整合包原样收录的许可保持。

Create工业管泵阀的原模型及调色派生素材不重新许可；Create原许可证、sources映射和three.js的MIT署名保持。README、治理和美术/文档入口已同步，历史报告保留原语境；本批不是全面历史第三方权利审计，不把本项目MIT扩展到上游资产。

`gradle.properties:mod_license`与NeoForge的`license`均为MIT，TOML注释说明例外。现有jar任务只增加根LICENSE、NOTICE.md和`META-INF/licenses/Create-LICENSE.md`三个入口，原排除名单和过时简介未扩改。

## 验证与正式包

- 唯一执行者实施三处元数据/打包文件；[组合窄审](REVIEW.md)未发现阻塞项。PM文档与实现合计10项来源差异精确提交，构建前工作区干净。
- 对准确已提交源仅运行一次`.\gradlew.bat jar --console=plain`，18秒、exit0；compileJava FROM-CACHE、processResources与jar执行成功，没有重封包或重复构建。
- 正式`build/libs/create_nuclear_industry-0.1.0.jar`为 **3525495 bytes**，SHA256 **`17A0A41616C27B4C2FDC7EE5A2E005973AB5C1EB3D1CA1BA16DB6F2B904ACB88`**。
- JAR非目录条目2450→2453，仅新增上述三份声明，均与对应来源逐字节一致；TOML字段MIT并与源码逐字节一致，无删除条目。
- **762个class全部与前正式包逐字节一致**。其余304项原条目差异中，1项为TOML许可变更，303项是严格UTF-8解码后仅LF/CRLF的文本资源差异；每项新资源原字节均匹配当前已提交工作树来源。归一化只替换CRLF为LF，未修改内容或重新包装JAR；无其他内容差异，Logo及PNG等美术原字节不变。
- 本批不涉及运行行为，未运行JUnit/GameTest、真实服或客户端手测；功能来源`2561b1d`的既有证据按范围复用，不将静态或包内核验称为新增游戏验收。

原始实施报告、jar.log、退出码、旧JAR备份、两包条目哈希及303项换行分类保存在本机`build/reports/mit-license-20261020/`。原正式包为3523047 bytes/SHA256 `74E95F6DCD024802F21B25A7028EBB067FF3FD83AB21D8E90B6A4E79EADD4090`。

执行前主工程干净；本次没有新建分支或更换工作树，没有打开或改写世界，辅助工具WIP独立树及未验事故候选保持。完成卡已[归档](../../../archive/2026-10-20/plans/2026-10-20-mit-license.md)，当前结果只在STATUS维护。
