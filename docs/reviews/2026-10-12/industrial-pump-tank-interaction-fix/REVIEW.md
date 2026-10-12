# 工业泵与创造罐交互独立窄审

2026-10-12，同一轮规格/质量审查。基线 `d17c18ad7b340d47887b6834c14c4590f3078aa2`；已读 main AGENTS、活动窄修卡及其撤销 cutout/96 点补片的最终扩卡、锁定 Create 源码、当前差异、IMPLEMENTATION/HANDOFF 和原始证据。实际应用 minecraft-modding、minecraft-testing、minecraft-resource-pack、requesting-code-review 与 verification-before-completion；保持 MC1.21.1/Java21/NF21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。审查没有运行测试、构建或素材生产，没有 Git 写操作；仅写本报告。

**结论：最终范围内无剩余必改项，可交 PM 登记客户端复看候选。GPU 视觉与用户桶操作人工门尚未关闭。**

桶修复只包原 `FluidTankBlock.useItemOn` 的两处 FluidHelper 调用，各调用 original 一次并原样返回结果。仅原 helper 成功后，对实际服务端目标 BE 与合法创造 controller 复核，再接续既有身份 setter。客户端、普通罐、失败、缺区块、BE 替换和活动锅炉分支不追加 setter；无 ThreadLocal、裸能力返回或新许可。公共 Mixin 没有 client 类依赖，真实专用服务端记录也完成装载。原 `CreateTankCapabilityRestrictionMixin` 与 `RestrictedFluidHandler` 相对基线无语义差异，缓存能力保持受限；热液/SC 模拟及执行禁运、读库存副本均有原 03 代表证据。

泵初始 alpha0/solid 假设在本轮被实际像素与 UV 反证；05/05b 是诊断假设失败，不能当作渲染层业务 RED，相关写权已撤销且没有实施。最终差异为原 SVG 的 x3..14/y19..26 共 96 点 `#000008/alpha255` 改为 `#747e86/alpha255`，单张导出 PNG；其余 928 点与全部 alpha 保持。生产器支持单泵重现，并如实记录派生补片。Renderer、三包装模型、几何、青箭头、方向/流量/SU及其他材质保持。真实烘焙像素证明已消费近黑区域被消歧，不证明存在独立旧方向 glyph，也不代替截图中三角的最终 GPU 观察。

原始证据已逐项读取：02 真实玩家入口的身份丢失 RED（exit1）；03 外部能力 JUnit 2/2；04 真正 `BlockState.useItemOn` 服务端 3/3，自然保存 exit0，涵盖冷液标记/清除及桶数量返还、水副手及非容器失败、普通罐有限交易；06 正确像素 RED（exit1）；07 单张 SVG/PNG 核验；08 真实 COG 像素与原六向正反青箭头 2/2（exit0）；09 唯一增量 assemble（1 秒、exit0）。客户端/替换/锅炉防护由源码路径核对，不冒称这些分支另有实际 GameTest。

最终独立只读核验当前 11 项来源 SHA 零差异，JAR 中 3 项资源与 2 个新 class 实哈希符合冻结清单。正式与冻结包 SHA256 均为 `1829ef1cc99039fb4a340d7794d5a9adf58da34955a3e2582ea7a1770a74530c`；用户 `261012` 包仍为 `ecbe0e38f0620db6764fb56fb63b2a5b17d56bb93059db4be4ff7849e1e65ce3`。已读包对比原始记录：仅本批 5 个 JAR 条目变化，其余 2145 条目字节保持；未触碰用户世界或其他 dirty。

本次不判断旧存档兼容（治理明确后置）、已通过整机/管网/汽轮机人工门（本卡禁止重开），也不判断未执行的 GPU 客户端效果。用户只需按 HANDOFF 重启原客户端，同处复看泵图案与青箭头，并复看冷却液桶标记/空桶清除。
