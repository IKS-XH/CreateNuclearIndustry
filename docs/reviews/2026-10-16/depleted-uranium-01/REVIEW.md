# DU-EQUIPMENT-01 组合窄审查

2026-10-16。审查目录：`E:/MyMC/NewMod/Create_NuclearIndustry-depleted-uranium`；比较基准：`060f3f65fa7dc46b3784001587eca1783f0b1009`；范围为未提交净变更与新增文件。本次仅只读核代码、Git、依赖源码、资源、候选JAR和已有验证证据，唯一写入本报告；未运行测试、构建、客户端或游戏世界，未做Git写操作。

## 结论

未发现可定位的功能/合同阻塞问题，无P0/P1/P2整改项。当前实现与本批自动化证据可交付PM进入候选人工门；此结论不代表客户端操作/外观验收、合入或正式发布。

## 规格与质量核对

- 注册与服务端边界：`DepletedUraniumContent.java:22-52`集中新增锭、普通铁性质储存块和九件装备，主入口及创造页只追加本批注册；新增生产类型不引用客户端类。保留原有贫化铀粉身份，没有新网络、NBT事务、辐射或额外战斗效果。
- 属性及唯一材料修复：`DepletedUraniumTier.java:14-19`委托`Tiers.NETHERITE`的耐久、采速、伤害、采掘标签与附魔能力，仅替换修复材料；`DepletedUraniumArmorItem.java:11-17`直接复用原版ArmorMaterial且修复不调用super，因此不会漏接下界合金锭。`DepletedUraniumContent.java:30-46`工具参数/耐火与护甲耐久和原版对应；真实GT逐件对照有效属性补足静态核查。
- 13条配方为2熔炼、2存取、9原生smithing_transform，模板/对应钻石装备/DU锭及数量符合卡。原版`SmithingTransformRecipe.assemble`的transmuteCopy保留底材组件，结果JSON无额外components覆写。真实SmithingMenu结果槽onTake验证三格实际扣料，覆盖九件名称/附魔/损伤/铁砧惩罚及四护甲饰纹；不是只比JSON或自行模拟扣料。
- 旧配方与邻近范围：相对基准的tracked recipe差异为空，新文件均属本批配方；核废料、尾矿、燃料处理没有净改。新增minecraft基础标签只追加对应装备并保留replace=false；真实GT遍历对应原版装备的全部minecraft标签，覆盖附魔/工具/护甲槽位及饰纹资格。镐的cluster_max_harvestables补齐原版采收语义。
- 占位资源：锭/块/九装备均引用批准的原版铁或下界合金资源，没有PNG新增。四护甲各10条trim overrides已由审查者与本机1.21.1 client JAR逐项独立比对，全部相同、目标存在；本机`BlockModel.getOverrides`确实不继承父级overrides，显式复制必要。`ItemProperties`为trim_type注册通用属性，无需为新物品增加客户端注册。穿戴层和trim材质身份由共享原版ArmorMaterial提供。
- JAR：审查者只读打开最终候选ZIP，13条本批配方存在，`DepletedUraniumGameTests*`及整个`data/cni_depleted_uranium/`均无残留；JAR中存在对应源码的全部assets/data文件与源码字节相同。3363957 bytes，SHA256 `53b0b554ad68f69f8bab384b1012599aacc3627469d592147ed970b4421c7189`，与最终REPORT一致。`build.gradle:158-161`排除仅针对本批测试内容，依赖/旧域/版本未变。
- 写集：生产/测试变更符合卡列路径；候选核心文档按PM另行合法写入处理，自动生成的tracked JAR/logs不误报为执行越界。

## 已有验证证据复用

本次未重新运行以下命令，按治理5.1复用执行者同一实现证据：

- `junit-green.xml:2`：定向JUnit 4项，0失败0错误；`verification.log:47`为test+assemble正常成功。测试覆盖数量守恒、9条升级材料、无直接工作台装备制作、资源与双语。
- `gametest-r1.log:93-94,115,118`：九件属性/耐久/附魔/耐火/修复/铁砧/标签/采掘、九个原生锻造菜单、真实熔炉与高炉及存取配方，3/3 required通过。
- `gametest-r1.log:126-129,134`及`gametest-r1-exit.txt`：三个维度保存完成，全部维度保存完成，Gradle 22秒正常退出0；断言、保存与进程退出分别有证据。
- `assemble-final.log:9`：最终仅打包排除调整后的assemble成功，业务逻辑未变，未要求重复JUnit/GameTest。
- 首轮失败的`gametest-initial-failure.log`、exit1及`gametest-run/crash-reports/crash-2026-10-16_16.26.49-server.txt`仍在。根据失败链和最终夹具源码，修正限于以FakePlayerFactory提供菜单上下文、避免Create未握手登录同步，未掩盖生产断言。独立新测试目录首次缺server.properties的日志随后正常初始化并通过，不构成本批功能问题。

## 独立未闭门

客户端尚未启动/验收。候选临时世界须按REPORT末尾组合清单观察实际资源加载、四件穿戴、五件手持、饰纹/附魔光效，并由玩家完成粉炼锭、块存取、原生锻造扣料/继承、铁砧修复及正常保存退出。自动化可交付不关闭此人工门，不触碰用户世界。

## 实际读取并应用的技能

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：锁定1.21.1/NeoForge的公共注册、原版物品机制、服务端类加载边界。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：区分数据合同、真实菜单/加工GT、保存、退出和客户端证据。
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：核1.21.1模型/标签/语言/loot路径、原版引用及trim覆盖。

实际Gradle文件锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；未套用技能中其他版本示例。已读AGENTS、治理（含5.1）、STATUS、活动卡和最终REPORT。审查不修改任务状态，验收和Git由PM处理。
