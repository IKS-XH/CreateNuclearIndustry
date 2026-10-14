# STORE-01 R1：铅屏蔽桶有序合成

按用户确认将铅屏蔽桶改为原生有序工作台配方，原料身份、数量和产量不变：

```text
" R "
"L L"
"LSL"
```

`L` 为 `c:plates/lead`（4），`S` 为 `c:plates/steel`（1），`R` 为正式物品 `create_nuclear_industry:seal_ring`（1），输出 4 个 `create_nuclear_industry:lead_shielding_cask`。原版有序配方允许水平镜像。

已修改源配方和 `tools/art-assets/store-01/generate.py`：完整导出与 `--lead-cask-only` 局部导出均生成该有序配方；校验器会展开 pattern 并检查 `L4/S1/R1`、两个公共板材标签、密封环身份、`minecraft:crafting_shaped`、`category=misc` 及四桶产量。局部模式只写本配方及 `evidence/resource-checks.json`，不重导其他资源。静态导出器检查所复用的 `c:plates/lead`、`c:plates/steel` 标签仍分别包含项目铅板和钢板。

本次唯一打包命令为 `gradlew.bat processResources jar --console=plain`，退出码 0，Gradle 报告 3 秒；`:processResources` 与 `:jar` 执行，`:compileJava` 和 `:createMinecraftArtifacts` 为 `UP-TO-DATE`。未运行 JUnit、GameTest 或客户端。产物为 `build/libs/create_nuclear_industry-0.1.0.jar`，大小 2,144,709 字节，SHA-256：

```text
4DAEABE7385D81B861F25E0DB6F08E7D6050B82D8C58FD01A6A9AE1CE91E1AA6
```

已从 JAR 读取 `data/create_nuclear_industry/recipe/crafting/lead_shielding_cask.json`：JAR 条目与源 JSON 逐字节相同，schema 为 `minecraft:crafting_shaped`，pattern 展开计数为 `L4/S1/R1`，输出仍为 4 桶。对应结果保存在本批 `tools/art-assets/store-01/evidence/resource-checks.json` 的 `lead_cask_recipe_verification` 和 `r1_resource_packaging` 字段。原 `checks.minecraft_runtime_or_gradle` 记录保留其历史语境，指首轮 STORE-01-ART 未运行 Gradle；R1 单次资源打包单独记录在上述字段。

本次只变更配方、导出/校验逻辑及相应检查证据。首轮 STORE-01 的 8 项 JUnit 和 6 项 GameTest 行为证据按治理5.1复用，本次未重跑。实际工作台合成及 JEI 显示并入现有唯一联合手测第1项，不新增人工门。
