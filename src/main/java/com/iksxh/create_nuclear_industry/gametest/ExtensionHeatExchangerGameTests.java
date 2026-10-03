package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.heat.HeatExchangerBoilerBridge;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 原生储罐连通、引擎和真实水能力驱动；不注入锅炉热缓存或伪造供水采样值。 */
@GameTestHolder("create_nuclear_industry_heat_exchanger")
@PrefixGameTestTemplate(false)
public final class ExtensionHeatExchangerGameTests {
    private static final BlockPos BASE = new BlockPos(2, 2, 2);
    private ExtensionHeatExchangerGameTests() {}

    @GameTest(template = "boiler_empty", timeoutTicks = 240)
    public static void smallNativeBoilerUsesRatedFlowAndQueriesArePure(GameTestHelper helper) {
        buildBoiler(helper, BASE, 2, 1, true);
        var machine = machine(helper, BASE.below());
        feed(helper, BASE, 10, machine, true);
        helper.runAfterDelay(105, () -> {
            var controller = controller(helper, BASE);
            require(helper, controller.getTotalTankSize() == 4 && controller.boiler.attachedEngines == 1,
                    "原生四储罐锅炉/引擎未形成");
            require(helper, controller.boiler.activeHeat == 18, "真实锅炉未读取换热器18级");
            require(helper, controller.boiler.getMaxHeatLevelForBoilerSize(4) == 1
                    && controller.boiler.getMaxHeatLevelForWaterSupply() == 1,
                    "原生尺寸和10mB/t供水未限制到一级");
            require(helper, machine.ledger().converted() == 36, "小锅炉错误降低额定36mB/t耗液");
            var snapshot = machine.savePortableData();
            var pos = helper.absolutePos(BASE.below());
            var port = machine.fluidPort(Direction.EAST);
            for (int i = 0; i < 100; i++) {
                require(helper, BoilerHeater.findHeat(helper.getLevel(), pos, machine.getBlockState()) == 18,
                        "公开BoilerHeater查询不是18");
                port.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 500), IFluidHandler.FluidAction.SIMULATE);
                port.drain(500, IFluidHandler.FluidAction.SIMULATE);
            }
            require(helper, snapshot.equals(machine.savePortableData()), "查询/模拟更改库存、HU或计时");
            require(helper, helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP) == null,
                    "顶面错误开放流体接口");
            require(helper, port.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE) == 0
                    && port.drain(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 100),
                    IFluidHandler.FluidAction.EXECUTE).isEmpty(), "错误流体进入或热液直接排出");
            helper.succeed();
        });
    }

    @GameTest(template = "boiler_empty", timeoutTicks = 260)
    public static void fullNativeBoilerReachesEighteenAndWaterCapsOutput(GameTestHelper helper) {
        buildBoiler(helper, BASE, 3, 8, true);
        var machine = machine(helper, BASE.below());
        int[] water = {180};
        helper.onEachTick(() -> supply(helper, BASE, water[0], machine, true));
        helper.runAfterDelay(110, () -> {
            var controller = controller(helper, BASE);
            require(helper, controller.getTotalTankSize() == 72, "原生72储罐连通未完成");
            require(helper, controller.boiler.activeHeat == 18
                    && controller.boiler.getMaxHeatLevelForBoilerSize(72) == 18
                    && controller.boiler.getMaxHeatLevelForWaterSupply() == 18,
                    "满尺寸及180mB/t真实供水未产生18级");
            require(helper, controller.boiler.getEngineEfficiency(72) == 1, "原生引擎效率未达满值");
            water[0] = 10;
        });
        helper.runAfterDelay(225, () -> {
            var controller = controller(helper, BASE);
            require(helper, controller.boiler.activeHeat == 18
                    && controller.boiler.getMaxHeatLevelForWaterSupply() == 1,
                    "降低真实供水后原生采样未限制有效输出");
            require(helper, machine.ledger().converted() == 36, "供水受限时额定流量错误变化");
            helper.succeed();
        });
    }

    @GameTest(template = "boiler_empty", timeoutTicks = 220)
    public static void bareTankDoesNotConsumeAndRemovedMachineKeepsSinglePaidSnapshot(GameTestHelper helper) {
        buildBoiler(helper, BASE, 2, 1, false);
        var machine = machine(helper, BASE.below());
        var port = machine.fluidPort(Direction.DOWN);
        // NeoForge 的 onLoad 在放置后调度；载入前能力按合同拒绝操作，不能把拒绝误判为耗液。
        helper.runAfterDelay(2, () -> require(helper,
                port.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 2000),
                        IFluidHandler.FluidAction.EXECUTE) == 2000, "加载后热液未实际注入"));
        helper.runAfterDelay(45, () -> {
            require(helper, machine.ledger().hot() == 2000 && machine.ledger().cold() == 0, "裸储罐错误耗热液");
            placeEngine(helper, BASE);
        });
        helper.onEachTick(() -> supply(helper, BASE, 10, machine, false));
        helper.runAfterDelay(115, () -> {
            var controller = controller(helper, BASE);
            require(helper, controller.boiler.activeHeat == 18, "移除前未真实供热");
            var saved = machine.savePortableData();
            var absolute = helper.absolutePos(BASE.below());
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
            require(helper, player.gameMode.destroyBlock(absolute), "真实生存铁镐破坏失败");
            require(helper, controller.boiler.activeHeat == 0, "移除后真实锅炉仍缓存旧热");
            require(helper, port.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 1),
                    IFluidHandler.FluidAction.EXECUTE) == 0 && port.drain(1, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                    "缓存旧能力在移除后仍修改库存");
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(1),
                    e -> e.getItem().is(HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER_ITEM.get()));
            require(helper, drops.size() == 1 && drops.getFirst().getItem().getCount() == 1, "破坏不是恰好掉一台");
            ItemStack drop = drops.getFirst().getItem().copy();
            require(helper, drop.get(DataComponents.CUSTOM_DATA) != null, "掉落缺少携物数据");
            helper.setBlock(BASE.below(), HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get());
            var restored = machine(helper, BASE.below());
            restored.getBlockState().getBlock().setPlacedBy(helper.getLevel(), absolute, restored.getBlockState(), null, drop);
            require(helper, saved.equals(restored.savePortableData()), "物品拆放更改库存/储备/时间戳");
            require(helper, BoilerHeater.findHeat(helper.getLevel(), absolute, restored.getBlockState()) == -1,
                    "恢复未经服务端负载验证就发布热");
            helper.succeed();
        });
    }

    @GameTest(template = "boiler_empty", timeoutTicks = 220)
    public static void lifecycleUnloadClearsControllerAcrossChunkBoundary(GameTestHelper helper) {
        // 选择模板内跨 X 区块边界的位置，控制器与右下角换热器必定在不同区块。
        int x = 15 - Math.floorMod(helper.absolutePos(new BlockPos(0, 0, 0)).getX(), 16);
        BlockPos base = new BlockPos(x, 2, 2);
        buildBoiler(helper, base, 2, 1, true);
        BlockPos source = base.offset(1, -1, 0);
        helper.setBlock(base.below(), Blocks.AIR);
        helper.setBlock(source, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get());
        var machine = machine(helper, source);
        feed(helper, base, 10, machine, true);
        helper.runAfterDelay(105, () -> {
            var controller = controller(helper, base);
            require(helper, controller.boiler.activeHeat == 18, "跨区块锅炉供热未建立");
            require(helper, (controller.getBlockPos().getX() >> 4) != (machine.getBlockPos().getX() >> 4),
                    "测试场景未实际跨区块");
            var saved = machine.savePortableData();
            var oldPort = machine.fluidPort(Direction.EAST);
            machine.onChunkUnloaded();
            require(helper, controller.boiler.activeHeat == 0 && machine.publishedHeat() == -1,
                    "实体卸载生命周期未清除跨区块锅炉缓存");
            require(helper, oldPort.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 1),
                    IFluidHandler.FluidAction.EXECUTE) == 0, "卸载后缓存能力仍有效");
            machine.loadPortableData(saved);
            machine.onLoad();
            require(helper, saved.equals(machine.savePortableData()) && controller.boiler.activeHeat == 0,
                    "恢复刷新已付储备或提前发布旧热");
            require(helper, oldPort.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 1),
                    IFluidHandler.FluidAction.EXECUTE) == 0, "已卸载的旧能力随同一实体恢复而复活");
            helper.succeed();
        });
    }

    @GameTest(template = "boiler_empty", timeoutTicks = 240, batch = "heat_liveness")
    public static void fullButNonTickingSourceRevokesHeatAndCapabilityCacheRecovers(GameTestHelper helper) {
        int x = 15 - Math.floorMod(helper.absolutePos(new BlockPos(0, 0, 0)).getX(), 16);
        BlockPos base = new BlockPos(x, 2, 2);
        buildBoiler(helper, base, 2, 1, true);
        BlockPos source = base.offset(1, -1, 0);
        helper.setBlock(base.below(), Blocks.AIR);
        helper.setBlock(source, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get());
        var machine = machine(helper, source);
        var level = helper.getLevel();
        var cache = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(
                Capabilities.FluidHandler.BLOCK, level, machine.getBlockPos(), Direction.EAST);
        IFluidHandler[] oldPort = {null};
        net.minecraft.nbt.CompoundTag[] beforePause = {null};
        var sourceChunk = level.getChunkAt(machine.getBlockPos());
        var previousStatus = nativeFullStatusSupplier(sourceChunk);
        boolean[] paused = {false};
        helper.onEachTick(() -> {
            supply(helper, base, 10, machine, false);
            if (!paused[0] && machine.current()) {
                var port = cache.getCapability();
                if (port != null) {
                    port.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 4000), IFluidHandler.FluidAction.EXECUTE);
                    port.drain(4000, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        });
        helper.runAfterDelay(105, () -> {
            require(helper, controller(helper, base).boiler.activeHeat == 18 && machine.canTick(), "停tick前真实供热未建立");
            oldPort[0] = cache.getCapability();
            beforePause[0] = machine.savePortableData();
            // 固定真实LevelChunk的原生FullStatus门；LevelChunk内部ticker自行拒绝运行。
            // 不改生产开关、不调用撤热方法，弱集合server事件必须自行发现失活源。
            sourceChunk.setFullStatus(() -> net.minecraft.server.level.FullChunkStatus.FULL);
            paused[0] = true;
        });
        helper.runAfterDelay(155, () -> {
            try {
                require(helper, level.hasChunkAt(machine.getBlockPos()) && level.getBlockEntity(machine.getBlockPos()) == machine,
                        "测试源已卸载，未覆盖FULL但不tick的缺口");
                require(helper, sourceChunk.getFullStatus() == net.minecraft.server.level.FullChunkStatus.FULL
                        && !machine.canTick() && level.shouldTickBlocksAt(controller(helper, base).getBlockPos()),
                        "原生FullStatus停tick门或跨chunk控制器门不符");
                require(helper, beforePause[0].equals(machine.savePortableData()), "停tick时源仍结算或旧句柄改库存");
                require(helper, machine.publishedHeat() == -1 && controller(helper, base).boiler.activeHeat == 0,
                        "源FULL但不tick时邻区块锅炉仍无限保留热");
                require(helper, oldPort[0].fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 1),
                        IFluidHandler.FluidAction.EXECUTE) == 0, "停tick旧能力仍修改库存");
                require(helper, cache.getCapability() != oldPort[0], "暂停没有使BlockCapabilityCache失效");
            } finally {
                sourceChunk.setFullStatus(previousStatus);
                paused[0] = false;
            }
        });
        helper.runAfterDelay(160, () -> {
            require(helper, machine.canTick() && machine.publishedHeat() == -1 && machine.ledger().reserve() < 720,
                    "恢复后未扣停tick期间热量或免费恢复完整余热");
            var port = cache.getCapability();
            require(helper, port != oldPort[0] && port != null && port.getTanks() == 2,
                    "恢复后真实BlockCapabilityCache未刷新可用端口");
            require(helper, oldPort[0].drain(1, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "恢复后旧epoch句柄复活");
        });
        helper.runAfterDelay(215, () -> {
            require(helper, machine.publishedHeat() == 18 && controller(helper, base).boiler.activeHeat == 18,
                    "恢复后缓存端口未重新提供热液并完成有偿预热");
            helper.succeed();
        });
    }

    /** 只读保存原生状态supplier以便finally精确恢复；不重建或覆盖实际票据系统。 */
    @SuppressWarnings("unchecked")
    private static java.util.function.Supplier<net.minecraft.server.level.FullChunkStatus> nativeFullStatusSupplier(
            net.minecraft.world.level.chunk.LevelChunk chunk) {
        try {
            var field = net.minecraft.world.level.chunk.LevelChunk.class.getDeclaredField("fullStatus");
            field.setAccessible(true);
            return (java.util.function.Supplier<net.minecraft.server.level.FullChunkStatus>) field.get(chunk);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("无法保存锁定版本原生FullStatus门", exception);
        }
    }
    private static void buildBoiler(GameTestHelper helper, BlockPos base, int width, int height, boolean engine) {
        helper.setBlock(base.below(), HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get());
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) for (int z = 0; z < width; z++)
            helper.setBlock(base.offset(x, y, z), AllBlocks.FLUID_TANK.get());
        if (engine) placeEngine(helper, base);
    }
    private static void placeEngine(GameTestHelper helper, BlockPos base) {
        helper.setBlock(base.west(), AllBlocks.STEAM_ENGINE.getDefaultState()
                .setValue(SteamEngineBlock.FACE, AttachFace.WALL).setValue(SteamEngineBlock.FACING, Direction.WEST));
    }
    private static NuclearHeatExchangerBlockEntity machine(GameTestHelper helper, BlockPos pos) {
        return (NuclearHeatExchangerBlockEntity) helper.getBlockEntity(pos);
    }
    private static FluidTankBlockEntity controller(GameTestHelper helper, BlockPos pos) {
        return ((FluidTankBlockEntity) helper.getBlockEntity(pos)).getControllerBE();
    }
    private static void feed(GameTestHelper helper, BlockPos base, int water,
                             NuclearHeatExchangerBlockEntity machine, boolean replenish) {
        helper.onEachTick(() -> supply(helper, base, water, machine, replenish));
    }
    private static void supply(GameTestHelper helper, BlockPos base, int water,
                               NuclearHeatExchangerBlockEntity machine, boolean replenish) {
        var handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(base), Direction.NORTH);
        if (handler != null) handler.fill(new FluidStack(Fluids.WATER, water), IFluidHandler.FluidAction.EXECUTE);
        if (replenish && machine.current()) {
            var port = machine.fluidPort(Direction.EAST);
            port.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 4000), IFluidHandler.FluidAction.EXECUTE);
            port.drain(4000, IFluidHandler.FluidAction.EXECUTE);
        }
    }
    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
