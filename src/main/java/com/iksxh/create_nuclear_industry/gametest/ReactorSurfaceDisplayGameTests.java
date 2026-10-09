package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.iksxh.create_nuclear_industry.structure.ReactorSurfaceDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 最小真实仪表集成：验证实际 BE 包、扫描去重、结构失效恢复及同坐标重放身份。 */
@GameTestHolder("reactor_surface_display_probe")
@PrefixGameTestTemplate(false)
public final class ReactorSurfaceDisplayGameTests {
    private static final BlockPos OWNER = new BlockPos(2, 2, 0);
    private ReactorSurfaceDisplayGameTests() { }

    @GameTest(template = "surface_probe_empty", timeoutTicks = 80)
    public static void authoritativeSurfacePacketTracksFormationAndReplay(GameTestHelper helper) {
        for (var entry : ReactorStructureDefinition.canonicalTemplate().entrySet()) {
            var local = entry.getKey();
            helper.setBlock(new BlockPos(local.x(), local.y(), local.z()),
                    BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.getValue())).defaultBlockState());
        }
        helper.runAfterDelay(2, () -> {
            ReactorInstrumentPortBlockEntity owner = owner(helper);
            rescan(helper);
            ReactorSurfaceDescriptor first = packet(owner);
            require(helper, first.valid() && first.members().size() == 98, "有效更新包缺少完整 98 块表面");
            require(helper, first.ownerPos().equals(helper.absolutePos(OWNER)), "更新包 owner 坐标不正确");
            require(helper, first.origin().equals(helper.absolutePos(BlockPos.ZERO)), "更新包原点不正确");
            require(helper, owner.surfaceDescriptor().orElseThrow().equals(first), "实际更新包与权威描述不一致");
            require(helper, first.members().stream().noneMatch(member -> member.expectedBlockId().endsWith("reactor_fuel_rod")),
                    "内部燃料棒错误进入表面");
            rescan(helper);
            require(helper, packet(owner).equals(first), "相同扫描推进了表面 revision");
            var saved = owner.saveForServerTest(helper.getLevel().registryAccess());
            require(helper, !saved.contains("ReactorSurface"), "显示描述错误持久化进服务端存档");

            helper.setBlock(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
            rescan(helper);
            ReactorSurfaceDescriptor broken = packet(owner);
            require(helper, !broken.valid() && broken.revision() == first.revision() + 1, "拆坏未撤销或版本不正确");
            helper.setBlock(BlockPos.ZERO, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(
                    "create_nuclear_industry:reactor_casing")).defaultBlockState());
            rescan(helper);
            ReactorSurfaceDescriptor restored = packet(owner);
            require(helper, restored.valid() && restored.revision() == broken.revision() + 1
                    && restored.ownerGeneration().equals(first.ownerGeneration()), "修复后描述没有恢复同一实例");

            helper.setBlock(OWNER, Blocks.AIR.defaultBlockState());
            helper.setBlock(OWNER, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(
                    "create_nuclear_industry:reactor_instrument_port")).defaultBlockState());
            rescan(helper);
            ReactorSurfaceDescriptor replay = packet(owner(helper));
            require(helper, replay.valid() && !replay.ownerGeneration().equals(first.ownerGeneration()),
                    "同坐标新仪表复用了旧 generation");
            helper.succeed();
        });
    }

    private static ReactorInstrumentPortBlockEntity owner(GameTestHelper helper) {
        var entity = helper.getBlockEntity(OWNER);
        require(helper, entity instanceof ReactorInstrumentPortBlockEntity, "缺少真实仪表 BE");
        return (ReactorInstrumentPortBlockEntity) entity;
    }

    private static ReactorSurfaceDescriptor packet(ReactorInstrumentPortBlockEntity owner) {
        return ReactorSurfaceDescriptor.decode(owner.getUpdatePacket().getTag().getCompound("ReactorSurface")).orElseThrow();
    }

    private static void rescan(GameTestHelper helper) {
        ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(), helper.absolutePos(OWNER));
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
