# GitHub公开发布收据

2026-11-09，已按用户本轮授权公开首个0.1.0 Alpha；这份收据记录实际发布结果，源/制品准备见[INTEGRATION](INTEGRATION.md)。不补记未报告的整线、多人、视觉或稳定版验收。

- 仓库：`IKS-XH/CreateNuclearIndustry`，PUBLIC；原MyMC地址仍重定向。
- [公开Release](https://github.com/IKS-XH/CreateNuclearIndustry/releases/tag/v0.1.0)：`Create: Nuclear Industry 0.1.0 Alpha (NeoForge 1.21.1)`。
- 实际字段：`isDraft=false`、`isPrerelease=true`、tagName=`v0.1.0`，未标Latest稳定版。
- 公开时间：2026-11-09 12:25:58 Asia/Shanghai（API `2026-11-09T04:25:58Z`）。
- 已推注释标签对象`2214e8d58abcf30467fcace449d660155510c46e`，解引用准确制品提交`5fa028a74be5ac92c61f49c5f624479d82da36d1`；该提交只叠加源码已冻结后的JAR和文档，运行源仍ea7ed52f。main与明确标签一次原子推送，未force或推其它分支。

## 附件与回核

| 附件 | 实际结果 |
| --- | --- |
| [模组JAR](https://github.com/IKS-XH/CreateNuclearIndustry/releases/download/v0.1.0/create_nuclear_industry-0.1.0-neoforge-1.21.1.jar) | uploaded，3781898 bytes |
| [SHA256SUMS.txt](https://github.com/IKS-XH/CreateNuclearIndustry/releases/download/v0.1.0/SHA256SUMS.txt) | uploaded，116 bytes |

先创建带两个附件的草稿，核准确tag/目标、Alpha字段、正文逐字符与冻结说明一致及资产状态后，公开该Release。随后通过GitHub重新下载两个附件：JAR SHA256为`D2B5390C368779CA881A391711F7F1B4158A666F407B5FF94F860506EA6175F3`，与正式路径及发布复制件一致；下载的SHA文件与本地附件逐字节一致。`git ls-remote`独立核对标签解引用到5fa028a7；匿名公开页面也已读到Release标题、预发布标记、时间和正文。

GitHub网页的动态资产列表在匿名抓取中曾显示加载错误，但实际Release API记录两个uploaded附件且下载成功，未把动态页面错误当作附件缺失或重复上传。中英正文、安装基线与许可链接均随冻结tag保留。

原始`github-draft.json`、`github-published.json`及下载原件位于main `build/reports/github-alpha-release-20261109-main/`。命令使用从官方CLI release取得、官方校验文件验证的gh2.103.3便携版；沿用本机既有GitHub账号认证，只在进程内使用并恢复环境，没有创建新凭据或改权限，未把认证信息写报告/Git/发布附件。

PM发布后只补当前STATUS、路线和本收据文档并推main，保已公开标签不变。不上传存档、日志、报告或无关3D dirty；未推送其它候选、未发布CurseForge/Modrinth、未自动开启后续功能或兼容研究。
