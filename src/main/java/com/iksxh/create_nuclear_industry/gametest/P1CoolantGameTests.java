package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.config.P1ServerConfig;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.ReactorCoolantLedger;
import com.iksxh.create_nuclear_industry.reactor.ReactorCoolantSimulationAdapter;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

/** 验证正式冷/热端口的 capability、共享库存、单端口流量和 Create 管路适配。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1CoolantGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    // 这些是标准模板的侧面端口槽位：仪表在北面，冷/热端口在南面。
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    private static final BlockPos COLD = new BlockPos(1, 2, 4);
    private static final BlockPos HOT = new BlockPos(3, 2, 4);

    private P1CoolantGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void formalCoolantCapabilitiesConvertDrainAndReloadConservatively(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            require(helper, instrument.structureValid(), "coolant test structure did not form");

            IFluidHandler cold = fluidHandler(helper, COLD);
            IFluidHandler hot = fluidHandler(helper, HOT);
            FluidStack coldStack = new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 256);
            int configuredFlowLimit = Math.max(0, P1ServerConfig.VALUES.perPortFlowMbPerTick.get());
            long configuredColdCapacity = P1ServerConfig.VALUES.coldInventoryCapacityMb.get().longValue();
            long configuredHotCapacity = P1ServerConfig.VALUES.hotInventoryCapacityMb.get().longValue();
            int initialSimulation = (int) Math.min(
                    Math.min(256L, configuredFlowLimit), configuredColdCapacity);
            int firstRequest = Math.min(80, initialSimulation);
            int secondRequest = (int) Math.min(
                    Math.max(0L, configuredFlowLimit - firstRequest),
                    Math.max(0L, configuredColdCapacity - firstRequest));

            require(helper, cold.fill(coldStack, FluidAction.SIMULATE) == initialSimulation,
                    "cold capability did not apply the per-port limit");
            require(helper, instrument.snapshot().coldCoolantMb() == 0L,
                    "simulated cold input mutated the authoritative snapshot");
            require(helper, cold.fill(new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), firstRequest),
                    FluidAction.EXECUTE) == firstRequest,
                    "cold capability did not accept the first same-tick transfer");
            require(helper, cold.fill(new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), secondRequest),
                    FluidAction.SIMULATE) == secondRequest,
                    "simulated cold input did not observe the remaining same-tick quota");
            IFluidHandler coldAgain = fluidHandler(helper, COLD);
            require(helper, coldAgain.fill(new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), secondRequest),
                    FluidAction.EXECUTE) == secondRequest,
                    "recreated capability bypassed the physical port quota");
            require(helper, coldAgain.fill(coldStack, FluidAction.EXECUTE) == 0,
                    "multiple execute calls exceeded the physical port quota");
            long acceptedThisTick = firstRequest + secondRequest;
            require(helper, instrument.snapshot().coldCoolantMb() == acceptedThisTick,
                    "cold input was not stored in the instrument snapshot");

            helper.runAfterDelay(1, () -> {
                int nextTickAccepted = (int) Math.min(
                        Math.min(256L, configuredFlowLimit),
                        Math.max(0L, configuredColdCapacity - acceptedThisTick));
                require(helper, coldAgain.fill(coldStack, FluidAction.EXECUTE) == nextTickAccepted,
                        "physical port quota did not reset on the next server tick");
                require(helper, instrument.snapshot().coldCoolantMb() == acceptedThisTick + nextTickAccepted,
                        "next-tick input did not use the refreshed quota");
                instrument.setSnapshot(instrument.snapshot().withCoolantInventories(acceptedThisTick, 0L));

                ReactorCoolantLedger.PortSummary ports = ReactorCoolantLedger.summarizePorts(List.of(
                        ReactorCoolantLedger.Port.cold("cold", configuredFlowLimit),
                        ReactorCoolantLedger.Port.hot("hot", configuredFlowLimit)
                ));
                ReactorCoolantSimulationAdapter.Result converted = ReactorCoolantSimulationAdapter.settle(
                        instrument.snapshot(),
                        new ReactorCoolantSimulationAdapter.TickInput(
                                acceptedThisTick * 0.5D, ports, 0.0D, 0.0D,
                                configuredHotCapacity, 0.5D));
                instrument.setSnapshot(converted.nextSnapshot());
                require(helper, instrument.snapshot().coldCoolantMb() == 0L
                                && instrument.snapshot().hotCoolantMb() == acceptedThisTick,
                        "ledger conversion did not move cold coolant to shared hot inventory");

                int hotAmount = (int) Math.min(Integer.MAX_VALUE, acceptedThisTick);
                int hotSimulation = Math.min(Math.min(256, hotAmount), configuredFlowLimit);
                int hotFirst = Math.min(80, hotSimulation);
                int hotSecond = Math.min(
                        Math.max(0, configuredFlowLimit - hotFirst),
                        Math.max(0, hotAmount - hotFirst));
                require(helper, hot.drain(256, FluidAction.SIMULATE).getAmount() == hotSimulation,
                        "hot capability did not expose converted hot coolant");
                require(helper, instrument.snapshot().hotCoolantMb() == hotAmount,
                        "simulated hot output mutated the authoritative snapshot");
                require(helper, hot.drain(hotFirst, FluidAction.EXECUTE).getAmount() == hotFirst,
                        "hot capability did not output the first same-tick transfer");
                require(helper, hot.drain(hotSecond, FluidAction.SIMULATE).getAmount() == hotSecond,
                        "simulated hot output did not observe the remaining same-tick quota");
                IFluidHandler hotAgain = fluidHandler(helper, HOT);
                require(helper, hotAgain.drain(hotSecond, FluidAction.EXECUTE).getAmount() == hotSecond,
                        "recreated hot capability bypassed the physical port quota");
                require(helper, hotAgain.drain(1, FluidAction.EXECUTE).isEmpty(),
                        "multiple hot execute calls exceeded the physical port quota");
                require(helper, instrument.snapshot().hotCoolantMb() == 0L,
                        "hot output was not deducted from the shared snapshot");

                long blockedCold = Math.min(64L, configuredColdCapacity);
                ReactorSnapshot blocked = new ReactorSnapshot(
                        instrument.snapshot().fuelColumns(),
                        instrument.snapshot().controlRodColumns(),
                        blockedCold,
                        configuredHotCapacity,
                        instrument.snapshot().meltdownProgressTicks(),
                        instrument.snapshot().meltdownCountdownStarted()
                );
                instrument.setSnapshot(blocked);
                ReactorCoolantLedger.PortSummary blockedPorts = ReactorCoolantLedger.summarizePorts(List.of(
                        ReactorCoolantLedger.Port.cold("cold", 128.0D),
                        ReactorCoolantLedger.Port.hot("hot", 0.0D)
                ));
                ReactorCoolantSimulationAdapter.Result blockedResult = ReactorCoolantSimulationAdapter.settle(
                        instrument.snapshot(),
                        new ReactorCoolantSimulationAdapter.TickInput(
                                100.0D, blockedPorts, 0.0D, 0.0D,
                                configuredHotCapacity, 0.5D));
                require(helper, blockedResult.nextSnapshot().equals(blocked),
                        "full hot buffer and blocked hot port consumed coolant or heat");

                CompoundTag saved = instrument.saveForServerTest(helper.getLevel().registryAccess());
                ReactorInstrumentPortBlockEntity reloaded = new ReactorInstrumentPortBlockEntity(
                        helper.absolutePos(INSTRUMENT),
                        P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
                reloaded.loadForServerTest(saved, helper.getLevel().registryAccess());
                require(helper, reloaded.snapshot().equals(blocked),
                        "blocked coolant inventories did not survive snapshot reload");
                helper.succeed();
            });
        });
    }

    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(INSTRUMENT);
        require(helper, blockEntity instanceof ReactorInstrumentPortBlockEntity,
                "expected instrument port block entity, got " + blockEntity);
        return (ReactorInstrumentPortBlockEntity) blockEntity;
    }

    private static IFluidHandler fluidHandler(GameTestHelper helper, BlockPos pos) {
        IFluidHandler handler = helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(pos),
                Direction.UP
        );
        require(helper, handler != null, "fluid capability missing at " + pos);
        return handler;
    }

    private static void buildCanonicalStructure(GameTestHelper helper) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.canonicalTemplate().entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    private static Block blockForId(String id) {
        return switch (id) {
            case "minecraft:air" -> Blocks.AIR;
            case "create_nuclear_industry:reactor_casing" -> P1Blocks.REACTOR_CASING.get();
            case "create_nuclear_industry:reactor_window" -> P1Blocks.REACTOR_WINDOW.get();
            case "create_nuclear_industry:reactor_instrument_port" -> P1Blocks.REACTOR_INSTRUMENT_PORT.get();
            case "create_nuclear_industry:reactor_cold_port" -> P1Blocks.REACTOR_COLD_PORT.get();
            case "create_nuclear_industry:reactor_hot_port" -> P1Blocks.REACTOR_HOT_PORT.get();
            case "create_nuclear_industry:reactor_refueling_port" -> P1Blocks.REACTOR_REFUELING_PORT.get();
            case "create_nuclear_industry:reactor_fuel_rod" -> P1Blocks.REACTOR_FUEL_ROD.get();
            case "create_nuclear_industry:control_rod_drive" -> P1Blocks.CONTROL_ROD_DRIVE.get();
            default -> throw new IllegalArgumentException("unknown canonical structure block " + id);
        };
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
