# 首个公开Alpha源与制品冻结

2026-11-09。用户授权在GitHub发布成品，沿[发布卡](../../../superpowers/plans/2026-11-09-github-alpha-release.md)按现行版本发布0.1.0 Alpha，不冒称稳定版或补记尚未报告的整线/多人体验。

## 准确来源

main原基线55b88c42及正式0B9包；准备卡16f30320，README/中英说明8b2935a2与348da3fe，两项发布元数据源`ea7ed52f2bba93594a711cce4cbf32c8cd720984`。仅改gradle.properties的mod_description及mods.toml的description正文，不改版本、依赖、许可、玩法、配方、素材或构建脚本。README英文去除已删除应力闭环以及“事故/辐射尚未实现”旧措辞，补下载/安装入口，保留Alpha边界。

复用旧候选同目录切分支保原位世界，唯一执行者在准确源已接main后只运行这一轮正常build，没有另跑候选/clean/rerun。PM只管文档/Git/发布，不编写功能/测试代码或构建程序。[构建报告](BUILD.md)、[完整制品数据](ARTIFACT.json)、[一次组合审查](REVIEW.md)。

## 真实验证与附件

- `.\gradlew.bat build --console=plain`，58.8345秒，exit0，9项中processResources/jar/test实际执行，其余6项UP-TO-DATE。
- 153类871项：860通过，0失败/错误，11原有Sodium专用夹具条件跳过；与11月6日逐测试名相同。未修改跳过规则，不把11项算通过；已有25幕/设备手测按原范围复用，未新增游戏、多人或旧存档兼容研究。
- 包内版本0.1.0、MC1.21.1、NeoForge21.1.219、Create6.0.10-280及其余锁定基线保持；MIT、Logo保留权利与Create派生声明保持，三许可文件与原包及当前来源原字节相同。
- 正式JAR **3781898 bytes**，SHA256 **`D2B5390C368779CA881A391711F7F1B4158A666F407B5FF94F860506EA6175F3`**。相对已验0B9包的2621载荷无增删，仅`META-INF/neoforge.mods.toml`描述正文变化；全部825class与其它2620载荷原字节保持，开发探针原排除无泄漏。
- 发布附件另复制命名`create_nuclear_industry-0.1.0-neoforge-1.21.1.jar`，与正式路径原字节同源；`SHA256SUMS.txt`来自[校验文件](../../../releases/0.1.0-alpha.sha256)。不上传世界、日志、报告或其它未提交工具。

2440项输入构建前后SHA相同，HEAD不变，2项用户3D工具SHA及既有dirty/untracked保持；旧0B9完整包保存。原始before/after、逐载荷、XML、build日志和核验工具均在`E:/MyMC/NewMod/Create_NuclearIndustry/build/reports/github-alpha-release-20261109-main/`，候选源准备在同级reactor-explosion-fix树`build/reports/github-alpha-release-20261109/`。本目录只保必要短报告，报告内原始相对路径按各原报告根解析。

## 发布范围

仓库实际为`https://github.com/IKS-XH/CreateNuclearIndustry`，旧MyMC地址仍重定向；本次只操作该仓库。拟用`v0.1.0`、`Create: Nuclear Industry 0.1.0 Alpha (NeoForge 1.21.1)`，GitHub prerelease=true。发布说明见[冻结正文](../../../releases/0.1.0-alpha.md)，含准确安装环境、功能、Alpha边界及许可。

PM只推明确main与准确标签，先草稿核附件再按用户本轮授权公开；最终远端URL、tag提交、附件下载回核和Git收据另存RELEASE.md，未取得收据前不称已发布。此次冻结不自动开展后续兼容策略、压力、再处理、值班长、音效或新CI。
