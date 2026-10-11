package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.structure.ReactorRuntimeDescriptor;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 独立服务端世界中的真实库存事务回归：数量单位 mB，运行包仍只属于完整成功结算。
 * 使用正式端口 capability 和实际 BE 更新包；不把包检查当作客户端液面视觉验收。
 */
@GameTestHolder("reactor_runtime_coolant_probe")
@PrefixGameTestTemplate(false)
public final class ReactorRuntimeCoolantGameTests {
    private static final BlockPos OWNER = new BlockPos(2, 2, 0);
    private static final BlockPos COLD = new BlockPos(1, 2, 4);
    private static final BlockPos HOT = new BlockPos(3, 2, 4);

    private ReactorRuntimeCoolantGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void inventoryTransactionsKeepCompletedSampleUntilNextSettlement(GameTestHelper helper) {
        for (var entry : ReactorStructureDefinition.canonicalTemplate().entrySet()) {
            var position = entry.getKey();
            helper.setBlock(new BlockPos(position.x(), position.y(), position.z()),
                    BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.getValue())).defaultBlockState());
        }
        helper.runAfterDelay(2, () -> verifyTransactions(helper));
    }

    /** 所有事务在同一服务端 tick 内完成，排除自动 tick 与单口预算刷新造成的歧义。 */
    private static void verifyTransactions(GameTestHelper helper) {
        var owner = (ReactorInstrumentPortBlockEntity) helper.getBlockEntity(OWNER);
        rescan(helper);
        require(helper, owner.structureValid(), "真实反应堆未成型");
        owner.setSnapshot(owner.snapshot().withCoolantInventories(100, 100));
        owner.tickReactor();
        var cold = handler(helper, COLD);
        var hot = handler(helper, HOT);
        var first = packet(owner);
        var firstObject = owner.runtimeDescriptor().orElseThrow();
        var authoritative = owner.snapshot();
        require(helper, first.available() && first.coldCoolantMb() == 100 && first.hotCoolantMb() == 100,
                "初始完整结算包与权威库存不符");

        require(helper, cold.fill(cold(24), FluidAction.SIMULATE) == 24
                        && hot.drain(16, FluidAction.SIMULATE).getAmount() == 16,
                "真实端口 SIMULATE 数量错误");
        require(helper, cold.fill(FluidStack.EMPTY, FluidAction.EXECUTE) == 0
                        && hot.drain(0, FluidAction.EXECUTE).isEmpty(), "零量事务产生写入");
        require(helper, owner.snapshot() == authoritative && owner.runtimeDescriptor().orElseThrow() == firstObject
                        && packet(owner).equals(first), "SIMULATE 或零量改变权威库存或完整运行样本");

        require(helper, cold.fill(cold(24), FluidAction.EXECUTE) == 24, "冷液 EXECUTE 未接受 24mB");
        require(helper, owner.snapshot().equals(authoritative.withCoolantInventories(124, 100)),
                "加注未仅改变真实冷液库存");
        boolean fillFrozen = owner.runtimeDescriptor().orElseThrow() == firstObject && packet(owner).equals(first);
        LogUtils.getLogger().info("RUNTIME_COOLANT fill: cold=100->124, hot=100, available={}, fullEnvelopeFrozen={}",
                packet(owner).available(), fillFrozen);

        owner.tickReactor();
        var settledAfterFill = packet(owner);
        requireNewSettlement(helper, owner, first, settledAfterFill, 124, 100);
        var secondObject = owner.runtimeDescriptor().orElseThrow();
        var beforeDrain = owner.snapshot();
        require(helper, hot.drain(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 16),
                        FluidAction.EXECUTE).getAmount() == 16, "热液 EXECUTE 未排出 16mB");
        require(helper, owner.snapshot().equals(beforeDrain.withCoolantInventories(124, 84)),
                "排出未仅改变真实热液库存");
        boolean drainFrozen = owner.runtimeDescriptor().orElseThrow() == secondObject
                && packet(owner).equals(settledAfterFill);
        LogUtils.getLogger().info("RUNTIME_COOLANT drain: cold=124, hot=100->84, available={}, fullEnvelopeFrozen={}",
                packet(owner).available(), drainFrozen);
        require(helper, fillFrozen && drainFrozen,
                "普通库存事务撤销或改写完整 L2：fillFrozen=" + fillFrozen + ", drainFrozen=" + drainFrozen);

        owner.tickReactor();
        var settledAfterDrain = packet(owner);
        requireNewSettlement(helper, owner, settledAfterFill, settledAfterDrain, 124, 84);
        verifyRevocation(helper, owner, cold, hot);
        LogUtils.getLogger().info("RUNTIME_COOLANT complete: frozen fill/drain, next settlement, simulate/zero, external/early-return/structure revoke passed");
        helper.succeed();
    }

    /** 下一正式成功结算必须推进样本并完整携带结算后真实库存，不能继续发旧包。 */
    private static void requireNewSettlement(GameTestHelper helper, ReactorInstrumentPortBlockEntity owner,
            ReactorRuntimeDescriptor before, ReactorRuntimeDescriptor after, long coldMb, long hotMb) {
        require(helper, after.available() && after.sample() == before.sample() + 1
                        && after.revision() > before.revision()
                        && after.serverGameTime() == helper.getLevel().getGameTime()
                        && after.coldCoolantMb() == coldMb && after.hotCoolantMb() == hotMb
                        && after.coldCoolantMb() == owner.snapshot().coldCoolantMb()
                        && after.hotCoolantMb() == owner.snapshot().hotCoolantMb()
                        && after.coolantCapacityMb() == owner.coolantCapacityMb()
                        && after.columns().equals(before.columns()) && after.convertedCoolantMbPerTick() == 0,
                "下一正式结算未产生同龄完整新样本");
    }

    /** 同值外部快照、提交前早退及结构失效都仍撤销；普通库存事务不能复活不可用样本。 */
    private static void verifyRevocation(GameTestHelper helper, ReactorInstrumentPortBlockEntity owner,
            IFluidHandler cold, IFluidHandler hot) {
        var authoritative = owner.snapshot();
        owner.setSnapshot(authoritative);
        require(helper, !packet(owner).available(), "同值外部快照改写未撤销 L2");
        var revoked = owner.runtimeDescriptor().orElseThrow();
        require(helper, cold.fill(cold(8), FluidAction.EXECUTE) == 8
                        && hot.drain(8, FluidAction.EXECUTE).getAmount() == 8
                        && owner.snapshot().equals(authoritative.withCoolantInventories(132, 76)),
                "撤销后库存事务改变原权威数量规则");
        require(helper, owner.runtimeDescriptor().orElseThrow() == revoked && !packet(owner).available(),
                "库存入口复活了外部改写撤销的旧样本");
        owner.tickReactor();
        require(helper, packet(owner).available(), "外部改写后正式成功结算未恢复");

        // 保留有效缓存但移除真实换料端口，命中正式 hydrate 的提交前早退。
        var fuelCap = new BlockPos(1, 4, 1);
        var capState = helper.getBlockState(fuelCap);
        helper.setBlock(fuelCap, Blocks.AIR.defaultBlockState());
        require(helper, !owner.tickReactor() && !packet(owner).available(), "正式提交前早退未撤销 L2");
        var earlyRevoked = owner.runtimeDescriptor().orElseThrow();
        long beforeCold = owner.snapshot().coldCoolantMb();
        require(helper, cold.fill(cold(8), FluidAction.EXECUTE) == 8
                        && owner.snapshot().coldCoolantMb() == beforeCold + 8
                        && owner.runtimeDescriptor().orElseThrow() == earlyRevoked && !packet(owner).available(),
                "库存入口复活了正式早退撤销的样本");
        helper.setBlock(fuelCap, capState);
        rescan(helper);
        owner.tickReactor();
        require(helper, packet(owner).available(), "修复真实换料端口后正式结算未恢复");

        helper.setBlock(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
        rescan(helper);
        var invalidSnapshot = owner.snapshot();
        require(helper, !owner.structureValid() && !packet(owner).available()
                        && cold.fill(cold(8), FluidAction.EXECUTE) == 0
                        && hot.drain(8, FluidAction.EXECUTE).isEmpty()
                        && owner.snapshot() == invalidSnapshot && !packet(owner).available(),
                "结构失效后旧 capability 改库存或复活显示");
        helper.setBlock(BlockPos.ZERO, P1Blocks.REACTOR_CASING.get().defaultBlockState());
        rescan(helper);
        owner.tickReactor();
        require(helper, packet(owner).available(), "有效结构重建后正式结算未恢复");
    }

    private static FluidStack cold(int amountMb) {
        return new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), amountMb);
    }

    private static IFluidHandler handler(GameTestHelper helper, BlockPos position) {
        var handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(position), Direction.SOUTH);
        require(helper, handler != null, "真实冷/热端口缺少流体 capability");
        return handler;
    }

    private static ReactorRuntimeDescriptor packet(ReactorInstrumentPortBlockEntity owner) {
        return ReactorRuntimeDescriptor.decode(owner.getUpdatePacket().getTag().getCompound("ReactorRuntime"))
                .orElseThrow();
    }

    private static void rescan(GameTestHelper helper) {
        ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(), helper.absolutePos(OWNER));
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
