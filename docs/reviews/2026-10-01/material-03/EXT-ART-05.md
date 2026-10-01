# EXT-ART-05 交付报告

## 完成范围

按B卡新增 `iron_dust`、`coal_dust`、`charcoal_dust`、`steel_dust`、`steel_ingot` 五种16×16手绘整数像素SVG、generated PNG及游戏纹理，并将它们接入美术 manifest、色板、管线、验证器和README。钢锭经PM预览反馈后改为锡锭式平顶厚锭轮廓，保留钢材冷灰色板；没有改动钢板纹理、五种物品注册、模型或语言资源。没有使用生图模型。

基线仍为51项；白名单共60张游戏PNG，manifest共61项（含game为空的青金石工具候选）。源稿共57个唯一SVG，新增路径只有本批五项。最终总览根据条目数按行调整高度，实际检查`preview.png`末行五项完整；九页分组预览中实际查看`items-3.png`的五项原尺寸、明暗底和2×平铺，并查看报告目录中的五素材与钢板放大对照。预览检查不代替Minecraft客户端验收，五项外观仍需客户端人工检查。

## 验证结果

执行环境为Python 3.12.14、Pillow 12.3.0；Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82未升级。实际读取并应用`minecraft-modding`、`minecraft-testing`和`minecraft-resource-pack`技能。

最终正式验证命令：

```powershell
$env:PYTHONDONTWRITEBYTECODE='1'
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/verify.py
```

退出码0，结果`PASS`。验证覆盖固定51项基线、像素与Alpha合同、保留冷却剂原字节、四张已批准样稿不变、默认导出重复2次、安装重复2次、非法SVG拒绝，以及unsupported SVG、非法路径、尺寸、映射和preserve错误共六类CLI失败不写出。正式验证在运行开始时直接快照已有游戏路径和哈希，不依赖被忽略的旧报告文件。完整命令与记录保存在`build/reports/extension/EXT-ART-05/commands.json`及`verification.json`。

开工55张游戏PNG的独立核对命令退出码0：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B build/reports/extension/EXT-ART-05/check_opening_55.py
```

55张哈希全部匹配开工快照，最终60张中新增且仅新增上述五条游戏路径。记录见`opening-55-preservation.json`。历史51项未变；未新增或伪造基线记录。

| 物品 | 游戏PNG SHA-256 |
|---|---|
| iron_dust | `969c1f2d4ea7ed035f51a93b6d9d4bbff34ea2ab0b4374a63c835820c8770a85` |
| coal_dust | `b42de73093ac7c0ed7f88fa678c4a179c7d6af098c4ebede57d6bceddb32a277` |
| charcoal_dust | `3a12381928272f6b6f25f0c771d02ab430b4eaa5382455b49feee25df11ab13b` |
| steel_dust | `2729af44c5cd43592fd44b0fcc609781b065caf3d6929d2b9a33c7cd41c33419` |
| steel_ingot | `69e3d681cafdc4f3cb189a8a2eb25c2de93b00f4579d099d598d18e12553c322` |

## 交接边界

本任务未运行Gradle或Minecraft，也未改Java、测试、配方、物品注册或钢板。已完成资源写入并停止；A可开始其独占Gradle窗口。候选工作树改动未提交，未执行任何Git写操作。
