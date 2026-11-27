# GitHub 0.2.0 Alpha 发布回执

用户于 2026-11-27 确认本批手测全部通过，并要求合并、提交和发布。[GitHub Release](https://github.com/IKS-XH/CreateNuclearIndustry/releases/tag/v0.2.0) 于 2026-11-27 18:53:52（北京时间）公开，标为 Alpha 预发布。仓库为 `IKS-XH/CreateNuclearIndustry`。

- 已验候选 `c91bb92c337eb330881b9bd6d899a9b29be42396` 快进合入 main。制品提交 `61e3a7864462f0fabed7be571c0f8f4c5b17c991` 将正式 JAR、校验值和中英说明纳入 Git。远程注释标签 `v0.2.0` 的对象为 `9c2c8c53f229de4be40e0d37e985516d21316147`，解引用到该制品提交。main 与明确标签原子推送。
- 在受验源运行一次正常 `./gradlew.bat build --console=plain`，45 秒、退出 0。153 类共 872 项 JUnit，861 项通过，11 项为既有 Sodium 专用夹具跳过，零失败或错误。候选与正式 0.2.0 JAR 整包及全部 2,629 个文件条目原字节一致，版本、依赖和许可证经核对。手测见 [验收记录](../../../STATUS.md#020-验收)。
- [发布附件 JAR](https://github.com/IKS-XH/CreateNuclearIndustry/releases/download/v0.2.0/create_nuclear_industry-0.2.0-neoforge-1.21.1.jar) 为 3,806,167 字节，SHA-256 `31AB90218DDF226154C4907CBD8BBF5B9F9DE1306718980507CF86FE38BED5A7`。[校验值附件](https://github.com/IKS-XH/CreateNuclearIndustry/releases/download/v0.2.0/SHA256SUMS.txt) 为 116 字节。两个附件上传完成后从公开地址重新下载，摘要均与本地发布件一致。
- 主树合入前后 25 个 3D 工具工作文件的 Git 状态及 SHA-256 相同。0.1.0 正式 JAR 保留，SHA-256 为 `D2B5390C368779CA881A391711F7F1B4158A666F407B5FF94F860506EA6175F3`。

构建日志、包内比较、测试计数、GitHub 草稿/发布回执及回下载附件位于候选工作树 `build/reports/github-0.2.0-release-20261127/`。现行中英说明见[发布说明](../../../releases/0.2.0-alpha.md)。
