package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity;
import com.iksxh.create_nuclear_industry.boiler.BoilerSurfaceDescriptor;
import com.iksxh.create_nuclear_industry.config.BoilerConfig;
import com.iksxh.create_nuclear_industry.config.HeatExchangerConfig;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;

/** 唯一隔离锅炉探针：核实真实缓存、原生更新包和保存边界，不启动用户存档或消费者模型。 */
@GameTestHolder("boiler_surface_display_probe")
@PrefixGameTestTemplate(false)
public final class BoilerSurfaceDisplayGameTests {
    private static final BlockPos BASE = new BlockPos(4, 2, 4);
    private static final BlockPos CONTROL = BASE.offset(1, 1, 0), WINDOW = BASE.offset(5, 1, 2);
    private BoilerSurfaceDisplayGameTests() { }

    @GameTest(template = "boiler_empty", timeoutTicks = 80)
    public static void actualControllerTracksFormationOnlyAndKeepsDisplayOutOfSaves(GameTestHelper helper) {
        ExtensionBoilerGameTests.build(helper, 6, 6, 8, 2);
        helper.runAfterDelay(4, () -> {
            var owner = ExtensionBoilerGameTests.owner(helper);
            require(helper, owner.currentForm() != null, "6×6×8 偏心隔层未成型");
            var first = packet(owner);
            require(helper, first.valid() && first.min().equals(helper.absolutePos(BASE))
                    && first.maxInclusive().equals(helper.absolutePos(BASE.offset(5, 5, 7)))
                    && first.partitionY() == helper.absolutePos(BASE).getY() + 2, "真实 bounds 或绝对隔层错误");
            require(helper, first.members().size() == 192, "真实完整外表面数量错误");
            require(helper, first.members().stream().anyMatch(m -> m.pos().equals(helper.absolutePos(BASE.offset(3, 0, 0)))
                    && m.outwardFaces().containsAll(List.of(Direction.NORTH, Direction.DOWN))), "底边热口遗漏真实朝外面");
            require(helper, first.equals(owner.surfaceDescriptor().orElseThrow()), "更新包与权威冻结描述不一致");
            require(helper, first.equals(BoilerSurfaceDescriptor.decode(owner.getUpdateTag(helper.getLevel().registryAccess())
                    .getCompound("BoilerSurface")).orElseThrow()), "原生 update tag 与 packet 不一致");
            require(helper, !owner.saveWithoutMetadata(helper.getLevel().registryAccess()).contains("BoilerSurface")
                    && !owner.savePortableData().contains("BoilerSurface"), "显示字段进入 server 或 portable 保存");
            owner.tick(); require(helper, packet(owner).equals(first), "无水热的正常遥测改变几何版本");
            helper.setBlock(CONTROL.north(), Blocks.REDSTONE_BLOCK);
            owner.tick(); require(helper, packet(owner).equals(first), "红石停机改变成型含义");
            helper.setBlock(CONTROL.north(), Blocks.AIR);
            double oldDensity = HeatExchangerConfig.HU_PER_LEVEL.get();
            try {
                HeatExchangerConfig.HU_PER_LEVEL.set(Double.NaN);
                owner.tick(); require(helper, packet(owner).equals(first), "换热器坏运行配置撤销几何");
            } finally { HeatExchangerConfig.HU_PER_LEVEL.set(oldDensity); }

            helper.setBlock(WINDOW, Blocks.AIR);
            require(helper, !packet(owner).valid(), "拆窗未立即撤销显示");
            require(helper, owner.currentForm() == null, "拆窗后旧 Form 仍有效");
            var broken = packet(owner);
            owner.invalidateForm(); require(helper, packet(owner).equals(broken), "连续无效重复推进版本");
            helper.setBlock(WINDOW, BoilerContent.WINDOW.get());
            require(helper, owner.currentForm() != null, "恢复窗口未重新成型");
            var restored = packet(owner);
            require(helper, restored.valid() && restored.revision() == broken.revision() + 1
                    && restored.ownerGeneration().equals(first.ownerGeneration()), "恢复窗口的代次/版本错误");
            var oldRange = List.copyOf(BoilerConfig.DIMENSION_RANGE.get());
            try {
                BoilerConfig.DIMENSION_RANGE.set(List.of(11, 5));
                require(helper, owner.currentForm() == null && !packet(owner).valid(), "锅炉非法配置没有撤销缓存显示");
            } finally { BoilerConfig.DIMENSION_RANGE.set(oldRange); }
            require(helper, owner.currentForm() != null && packet(owner).valid(), "配置恢复未自动恢复显示");
            var controlState = helper.getBlockState(CONTROL);
            helper.setBlock(CONTROL, Blocks.AIR);
            helper.setBlock(CONTROL, controlState);
            helper.runAfterDelay(2, () -> {
                var replacement = ExtensionBoilerGameTests.owner(helper);
                require(helper, replacement != owner && replacement.currentForm() != null, "同坐标新控制器未原生恢复");
                var replay = packet(replacement);
                require(helper, replay.valid() && !replay.ownerGeneration().equals(first.ownerGeneration()), "新实例复用持久化 generation");
                require(helper, !replacement.saveWithoutMetadata(helper.getLevel().registryAccess()).contains("BoilerSurface")
                        && !replacement.savePortableData().contains("BoilerSurface"), "新实例显示错误保存");
                com.mojang.logging.LogUtils.getLogger().info("ART-BOILER-05-L1 probe: all authoritative packet/lifecycle assertions passed");
                helper.succeed();
            });
        });
    }
    private static BoilerSurfaceDescriptor packet(BoilerControllerBlockEntity owner) {
        return BoilerSurfaceDescriptor.decode(owner.getUpdatePacket().getTag().getCompound("BoilerSurface")).orElseThrow();
    }
    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
