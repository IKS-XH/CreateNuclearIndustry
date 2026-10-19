package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.IndustrialFluidContent;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.iksxh.create_nuclear_industry.industrial.IndustrialPumpBlock;
import com.iksxh.create_nuclear_industry.industrial.IndustrialPumpBlockEntity;
import com.iksxh.create_nuclear_industry.industrial.IndustrialValveBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Fluid;

/** 独立工业域真实世界能力与管网验据，不以额定数字或创造无限库存证明有限守恒。 */
@GameTestHolder("create_nuclear_industry_industrial")
@PrefixGameTestTemplate(false)
public final class IndustrialFluidNetworkGameTests {
    private static final java.util.Map<GameTestHelper, java.util.Map<BlockPos, Integer>> SUPPLIES = new java.util.WeakHashMap<>();
    private IndustrialFluidNetworkGameTests() {}
    /** 旧业务夹具补给改走有限Create源罐和真实专用泵；返回上次补给以来实际从源移出的mB。 */
    static int feed(GameTestHelper h, BlockPos port, Direction outward, Fluid fluid, int requested) {
        return feed(h, port, outward, fluid, requested, false);
    }
    /** 密排机组将西侧供给向下折弯，供给部件不能覆盖另一台已成型机组。 */
    static int feedBelow(GameTestHelper h, BlockPos port, Fluid fluid, int requested) {
        return feed(h, port, Direction.WEST, fluid, requested, true);
    }
    private static int feed(GameTestHelper h, BlockPos port, Direction outward, Fluid fluid, int requested, boolean below) {
        return feed(h, port, outward, fluid, requested, below, false);
    }
    /** 相邻热口的两套齿轮上下错开，不能让相邻同速齿轮啮合形成冲突动力源。 */
    static int feedSeparated(GameTestHelper h, BlockPos port, Direction outward, Fluid fluid, int requested) {
        return feed(h, port, outward, fluid, requested, false, (port.getX() & 1) == 0);
    }
    private static int feed(GameTestHelper h, BlockPos port, Direction outward, Fluid fluid, int requested, boolean below, boolean lowerDrive) {
        BlockPos pump = port.relative(outward, 2), tankPos = port.relative(outward, 4);
        if (below) { pump = port.west().below(2); tankPos = port.west().below(4); }
        if (!(h.getLevel().getBlockEntity(h.absolutePos(pump)) instanceof IndustrialPumpBlockEntity)) {
            if (below) { h.setBlock(port.west(), IndustrialFluidContent.CERAMIC_PIPE.get()); h.setBlock(port.west().below(), IndustrialFluidContent.CERAMIC_PIPE.get()); outward = Direction.DOWN; }
            else
            h.setBlock(port.relative(outward), IndustrialFluidContent.CERAMIC_PIPE.get());
            h.setBlock(pump, IndustrialFluidContent.PUMP.get().defaultBlockState().setValue(IndustrialPumpBlock.FACING, outward.getOpposite()));
            h.setBlock(below ? pump.below() : port.relative(outward, 3), IndustrialFluidContent.CERAMIC_PIPE.get()); h.setBlock(tankPos, AllBlocks.FLUID_TANK.get());
            BlockPos cog = below ? pump.north() : outward.getAxis() == Direction.Axis.Y ? pump.east() : lowerDrive ? pump.below() : pump.above();
            BlockPos shaft = cog.relative(outward), motor = cog.relative(outward, 2);
            h.setBlock(cog, AllBlocks.COGWHEEL.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, outward.getAxis()));
            h.setBlock(shaft, AllBlocks.SHAFT.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, outward.getAxis()));
            h.setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, outward.getOpposite()));
            ((CreativeMotorBlockEntity)h.getBlockEntity(motor)).generatedSpeed.setValue(-256 * outward.getOpposite().getAxisDirection().getStep());
        }
        var tank = ((FluidTankBlockEntity)h.getBlockEntity(tankPos)).getTankInventory();
        var previous = SUPPLIES.computeIfAbsent(h, ignored -> new java.util.HashMap<BlockPos, Integer>());
        int transferred = Math.max(0, previous.getOrDefault(tankPos, tank.getFluidAmount()) - tank.getFluidAmount());
        // 仅夹具构建初始源库存；真实扣源、目标过滤、每口预算与缓存由生产调度器完成。
        if (tank.getFluidAmount() < requested) tank.fill(new FluidStack(fluid, requested - tank.getFluidAmount()), IFluidHandler.FluidAction.EXECUTE);
        previous.put(tankPos, tank.getFluidAmount()); return transferred;
    }
    /** GameTest定时断言可能先于同刻onEachTick记数；读取已真实扣源但尚未登记的量，不修改账本。 */
    static int unreportedFeed(GameTestHelper h, BlockPos port, Direction outward) {
        BlockPos pos = port.relative(outward, 4);
        var stock = ((FluidTankBlockEntity)h.getBlockEntity(pos)).getTankInventory();
        return Math.max(0, SUPPLIES.getOrDefault(h, java.util.Map.of()).getOrDefault(pos, stock.getFluidAmount()) - stock.getFluidAmount());
    }
    /** 有限目标已被真实专用路线交付后，夹具消费者记录出库；不把它包装成公开受限能力。 */
    static int consumeFixtureTank(GameTestHelper h, BlockPos pos, int requested) {
        return ((FluidTankBlockEntity)h.getBlockEntity(pos)).getControllerBE().getTankInventory().drain(requested, IFluidHandler.FluidAction.EXECUTE).getAmount();
    }
    /** 使用与供给相同的真实有限路线，仅将专用泵朝向改为出库方向。 */
    static BlockPos outlet(GameTestHelper h, BlockPos port, Direction outward) {
        feed(h, port, outward, Fluids.WATER, 0);
        BlockPos pump = port.relative(outward, 2);
        if (h.getBlockState(pump).getValue(IndustrialPumpBlock.FACING) != outward)
            h.setBlock(pump, h.getBlockState(pump).setValue(IndustrialPumpBlock.FACING, outward));
        return port.relative(outward, 4);
    }
    /** 用实际低RPM保持旧生命周期夹具的观测窗口，不改变生产默认或物理口额度。 */
    static BlockPos outletAtRate(GameTestHelper h, BlockPos port, Direction outward, int amount) {
        BlockPos tank = outlet(h, port, outward);
        BlockPos motor = port.relative(outward, 2).above().relative(outward, 2);
        int rpm = (int)((long)amount * 256 / 16384);
        ((CreativeMotorBlockEntity)h.getBlockEntity(motor)).generatedSpeed.setValue(-rpm * outward.getOpposite().getAxisDirection().getStep()); return tank;
    }
    /** 相邻汽口输出保持三个真实泵，齿轮按Z交错上下，避开无关机架及相邻齿轮冲突。 */
    static BlockPos outletSeparated(GameTestHelper h, BlockPos port, Direction outward) {
        feed(h, port, outward, Fluids.WATER, 0, false, (port.getZ() & 1) == 0);
        return outlet(h, port, outward);
    }
    private static IFluidHandler capability(GameTestHelper h, BlockPos pos) {
        return h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), Direction.NORTH);
    }
    private static int amount(GameTestHelper h, BlockPos pos) { var cap = capability(h, pos); return cap == null ? 0 : cap.getFluidInTank(0).getAmount(); }
    /** 原生储罐同份库存只用于构建有限初始条件，运输断言始终走真实能力及调度。 */
    private static void seed(GameTestHelper h, BlockPos pos, Fluid fluid, int amount) {
        ((FluidTankBlockEntity) h.getBlockEntity(pos)).getControllerBE().getTankInventory().fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE);
    }
    private static void motor(GameTestHelper h, BlockPos pump, int speed) {
        motor(h, pump, speed, Direction.EAST);
    }
    private static void motor(GameTestHelper h, BlockPos pump, int speed, Direction side) {
        BlockPos cog = pump.relative(side), shaft = cog.north(), motor = cog.north(2);
        h.setBlock(cog, AllBlocks.COGWHEEL.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        h.setBlock(shaft, AllBlocks.SHAFT.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        h.setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH));
        ((CreativeMotorBlockEntity) h.getBlockEntity(motor)).generatedSpeed.setValue(speed);
    }
    /** 短收缓存已经经过输入强化阀，第二台泵不能再借缓存绕过该阀8192额度。 */
    @GameTest(template = "industrial_empty", timeoutTicks = 45)
    public static void sharedInputEdgeChargesExtractedFluidIncludingShortReceipt(GameTestHelper h) {
        BlockPos source = new BlockPos(7, 2, 1), target = new BlockPos(7, 2, 7);
        h.setBlock(source, AllBlocks.FLUID_TANK.get()); h.setBlock(source.above(), AllBlocks.FLUID_TANK.get());
        h.setBlock(source.south(), IndustrialFluidContent.REINFORCED_VALVE.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Z));
        h.setBlock(source.south(2), IndustrialFluidContent.REINFORCED_PIPE.get());
        for (int x : new int[]{6, 8}) {
            for (int z : new int[]{3, 4, 6}) h.setBlock(new BlockPos(x, 2, z), IndustrialFluidContent.CERAMIC_PIPE.get());
            BlockPos pump = new BlockPos(x, 2, 5);
            h.setBlock(pump, IndustrialFluidContent.PUMP.get().defaultBlockState().setValue(IndustrialPumpBlock.FACING, Direction.SOUTH));
            motor(h, pump, -256, x == 6 ? Direction.WEST : Direction.EAST);
        }
        h.setBlock(target.north(), IndustrialFluidContent.CERAMIC_PIPE.get()); h.setBlock(target, AllBlocks.PORTABLE_FLUID_INTERFACE.get());
        var storage = new net.neoforged.neoforge.fluids.capability.templates.FluidTank(20000) {
            @Override public int fill(FluidStack stack, FluidAction action) { return super.fill(action.execute() ? stack.copyWithAmount(Math.min(2000, stack.getAmount())) : stack, action); }
        };
        h.runAfterDelay(8, () -> {
            var psi = (com.simibubi.create.content.contraptions.actors.psi.PortableFluidInterfaceBlockEntity)h.getBlockEntity(target);
            try {
                var parent = com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity.class;
                var connected = parent.getDeclaredField("connectedEntity"); connected.setAccessible(true);
                connected.set(psi, new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), 0, 0, 0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK)));
                var timer = parent.getDeclaredField("transferTimer"); timer.setAccessible(true); timer.setInt(psi, 8);
                var capability = psi.getClass().getDeclaredField("capability"); capability.setAccessible(true); capability.set(psi, psi.new InterfaceFluidHandler(storage));
                h.getLevel().invalidateCapabilities(h.absolutePos(target));
            } catch (ReflectiveOperationException e) { throw new IllegalStateException("锁定PSI短收夹具不匹配", e); }
            seed(h, source, Fluids.WATER, 16000);
        });
        h.runAfterDelay(9, () -> {
            var a = (IndustrialPumpBlockEntity)h.getBlockEntity(new BlockPos(6, 2, 5));
            var b = (IndustrialPumpBlockEntity)h.getBlockEntity(new BlockPos(8, 2, 5));
            int cache = a.cachedFluid().getAmount() + b.cachedFluid().getAmount();
            h.assertTrue(a.getSpeed() == 256 && b.getSpeed() == 256, "两泵没有有效并行动力");
            h.assertTrue(amount(h, source) == 7808 && storage.getFluidAmount() == 2000 && cache == 6192,
                    "共输入边短收收费错误：source=" + amount(h, source) + " target=" + storage.getFluidAmount() + " cache=" + cache);
            h.assertTrue(amount(h, source) + storage.getFluidAmount() + cache == 16000 && a.actualFlow() + b.actualFlow() == 2000, "短收有限守恒/实收统计错误");
            System.out.println("[industrial] shared input valve8192 = delivered2000 + cache6192; two powered pumps source7808, finite total16000"); h.succeed();
        });
    }
    /** 控制器绑定变化而成员能力缓存尚未刷新时，只读取当前同库，缺失控制器不隐式加载。 */
    @GameTest(template = "industrial_empty", timeoutTicks = 50)
    public static void tankMemberUsesCurrentControllerAndRejectsMissingController(GameTestHelper h) {
        tankRoute(h, true, false);
        BlockPos member = new BlockPos(4, 2, 2), old = new BlockPos(1, 2, 2), current = new BlockPos(1, 2, 4);
        h.setBlock(old, AllBlocks.FLUID_TANK.get()); h.setBlock(current, AllBlocks.FLUID_TANK.get());
        h.runAfterDelay(8, () -> {
            seed(h, old, ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 3000);
            seed(h, current, ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 5000);
            var tank = (FluidTankBlockEntity)h.getBlockEntity(member); tank.setController(h.absolutePos(old));
            try {
                // 夹具制造合法当前绑定与旧成员cap的时间窗；生产必须避开旧handler库存。
                var controller = FluidTankBlockEntity.class.getDeclaredField("controller"); controller.setAccessible(true); controller.set(tank, h.absolutePos(current));
            } catch (ReflectiveOperationException e) { throw new IllegalStateException("锁定罐控制器字段不匹配", e); }
        });
        h.runAfterDelay(9, () -> {
            h.assertTrue(amount(h, old) == 3000 && amount(h, current) == 0 && amount(h, new BlockPos(4, 2, 8)) == 5000, "成员旧能力抽取旧控制器库存");
            try {
                var tank = (FluidTankBlockEntity)h.getBlockEntity(member);
                BlockPos missing = h.absolutePos(member).east(512); var chunks = h.getLevel().getChunkSource();
                h.assertTrue(chunks.getChunkNow(missing.getX() >> 4, missing.getZ() >> 4) == null, "缺控制器负例区块已加载");
                var controller = FluidTankBlockEntity.class.getDeclaredField("controller"); controller.setAccessible(true); controller.set(tank, missing);
                var topology = Class.forName("com.iksxh.create_nuclear_industry.industrial.IndustrialTopology");
                var query = topology.getDeclaredMethod("capabilityAt", net.minecraft.world.level.Level.class, BlockPos.class, Direction.class); query.setAccessible(true);
                for (int i = 0; i < 3; i++) h.assertTrue(query.invoke(null, h.getLevel(), h.absolutePos(member), Direction.NORTH) == null, "缺失控制器被工业能力授权");
                h.assertTrue(chunks.getChunkNow(missing.getX() >> 4, missing.getZ() >> 4) == null && amount(h, old) == 3000, "工业控制器探测隐式加载或修改库存");
                controller.set(tank, h.absolutePos(current));
            } catch (ReflectiveOperationException e) { throw new IllegalStateException("控制器无强载夹具反射失败", e); }
            System.out.println("[industrial] current controller5000 delivered; old controller3000 untouched; missing controller queries null without loaded chunk"); h.succeed();
        });
    }
    private static void tankRoute(GameTestHelper h, boolean industrial, boolean reinforced) {
        h.setBlock(new BlockPos(4, 2, 2), AllBlocks.FLUID_TANK.get());
        h.setBlock(new BlockPos(4, 2, 8), AllBlocks.FLUID_TANK.get());
        var pipe = industrial ? (reinforced ? IndustrialFluidContent.REINFORCED_PIPE.get() : IndustrialFluidContent.CERAMIC_PIPE.get()) : AllBlocks.FLUID_PIPE.get();
        for (int z : new int[]{3, 4, 6, 7}) h.setBlock(new BlockPos(4, 2, z), pipe);
        BlockPos pump = new BlockPos(4, 2, 5);
        h.setBlock(pump, industrial ? IndustrialFluidContent.PUMP.get().defaultBlockState().setValue(IndustrialPumpBlock.FACING, Direction.SOUTH)
                : AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.SOUTH));
        motor(h, pump, industrial ? -256 : 256);
    }
    /** 非法侧面先触达阀或机器，不得吞掉稍后有界绕行的合法入口与能力面。 */
    @GameTest(template = "industrial_empty", timeoutTicks = 60)
    public static void illegalEarlyFaceDoesNotConsumeValveOrEndpointVisit(GameTestHelper h) {
        BlockPos root = new BlockPos(4, 2, 5), source = root.north(3), target = new BlockPos(7, 2, 8);
        h.setBlock(source, AllBlocks.FLUID_TANK.get()); h.setBlock(target, AllBlocks.FLUID_TANK.get());
        h.setBlock(root, IndustrialFluidContent.PUMP.get().defaultBlockState().setValue(IndustrialPumpBlock.FACING, Direction.SOUTH)); motor(h, root, -256);
        for (BlockPos pos : new BlockPos[]{root.north(), root.north(2), root.south(), new BlockPos(5, 2, 6), new BlockPos(6, 2, 6), new BlockPos(6, 2, 5), new BlockPos(7, 2, 5), new BlockPos(7, 2, 7)}) h.setBlock(pos, IndustrialFluidContent.CERAMIC_PIPE.get());
        h.setBlock(new BlockPos(7, 2, 6), IndustrialFluidContent.REINFORCED_VALVE.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Z));
        BlockPos second = new BlockPos(15, 2, 5), hotSource = second.north(3), machine = new BlockPos(17, 2, 6);
        h.setBlock(hotSource, AllBlocks.FLUID_TANK.get());
        h.setBlock(second, IndustrialFluidContent.PUMP.get().defaultBlockState().setValue(IndustrialPumpBlock.FACING, Direction.SOUTH)); motor(h, second, -256);
        for (BlockPos pos : new BlockPos[]{second.north(), second.north(2), second.south(), new BlockPos(16, 2, 6), new BlockPos(15, 2, 7), new BlockPos(16, 2, 7), new BlockPos(17, 2, 7)}) h.setBlock(pos, IndustrialFluidContent.CERAMIC_PIPE.get());
        h.setBlock(machine, com.iksxh.create_nuclear_industry.content.HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState().setValue(com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlock.FACING, Direction.NORTH));
        h.runAfterDelay(8, () -> { seed(h, source, Fluids.WATER, 7000); seed(h, hotSource, ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 7000); });
        h.runAfterDelay(25, () -> {
            h.assertTrue(amount(h, target) == 7000 && amount(h, source) == 0, "阀侧面提前visited吞掉轴向合法绕行");
            var owner = (com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlockEntity)h.getBlockEntity(machine);
            h.assertTrue(owner.ledger().hot() == 4000 && amount(h, hotSource) == 3000, "机器无能力面提前visited吞掉背侧合法面或有限液不守恒");
            System.out.println("[industrial-review] valve invalid WEST then valid NORTH loop delivered7000; endpoint invalid WEST then valid SOUTH delivered4000, source3000"); h.succeed();
        });
    }
    /** 实际BlockItem.place创造连续放置只搬一次，完整原生Ctrl追加NBT链保持空缓存。 */
    @GameTest(template = "industrial_empty", timeoutTicks = 40)
    public static void creativePlacementAndNativePickPostprocessingDoNotCopyCache(GameTestHelper h) {
        BlockPos original = new BlockPos(3, 2, 3), first = new BlockPos(6, 2, 3), second = new BlockPos(8, 2, 3);
        h.setBlock(original, IndustrialFluidContent.PUMP.get()); h.setBlock(first.below(), Blocks.STONE); h.setBlock(second.below(), Blocks.STONE);
        h.runAfterDelay(5, () -> {
            var pump = (IndustrialPumpBlockEntity)h.getBlockEntity(original); var tag = new net.minecraft.nbt.CompoundTag();
            tag.put("Cache", new FluidStack(Fluids.WATER, 3000).save(h.getLevel().registryAccess())); pump.restorePortable(tag);
            var item = net.minecraft.world.level.block.Block.getDrops(h.getBlockState(original), h.getLevel(), h.absolutePos(original), pump).getFirst();
            h.setBlock(original, Blocks.AIR);
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel()); boolean creative = player.getAbilities().instabuild;
            try {
                player.getAbilities().instabuild = true; player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, item);
                for (BlockPos pos : new BlockPos[]{first, second}) {
                    var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(pos.below())), Direction.UP, h.absolutePos(pos.below()), false);
                    var context = new net.minecraft.world.item.context.BlockPlaceContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, item, hit);
                    h.assertTrue(((net.minecraft.world.item.BlockItem)item.getItem()).place(context).consumesAction(), "真实创造BlockItem放置失败");
                }
                var a = (IndustrialPumpBlockEntity)h.getBlockEntity(first); var b = (IndustrialPumpBlockEntity)h.getBlockEntity(second);
                h.assertTrue(a.cachedFluid().getAmount() == 3000 && b.cachedFluid().isEmpty() && !item.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA), "创造连续放置复制了同一携液凭据");
                var picked = h.getBlockState(first).getCloneItemStack(new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(first)), Direction.UP, h.absolutePos(first), false), h.getLevel(), h.absolutePos(first), player);
                var copied = a.saveCustomAndMetadata(h.getLevel().registryAccess()); h.assertTrue(copied.contains("Cache"), "当前世界保存丢失真实缓存");
                // 锁定Minecraft.addCustomNbtData使用的完整公共链；不是只测默认clone返回值。
                a.removeComponentsFromTag(copied); net.minecraft.world.item.BlockItem.setBlockEntityData(picked, a.getType(), copied); picked.applyComponents(a.collectComponents());
                var custom = picked.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
                h.assertTrue((custom == null || !custom.copyTag().contains("Cache")) && a.cachedFluid().getAmount() == 3000, "Ctrl选取原生追加NBT重新带入Cache或修改原库存");
            } finally { player.getAbilities().instabuild = creative; player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY); }
            System.out.println("[industrial-review] creative BlockItem.place cache3000 then0; Ctrl clone+native NBT/component postprocessing empty, world cache3000 intact"); h.succeed();
        });
    }
    @GameTest(template = "industrial_empty", timeoutTicks = 90)
    public static void poweredIndustrialHotRouteConservesFiniteCreateInventory(GameTestHelper h) {
        tankRoute(h, true, false);
        h.runAfterDelay(8, () -> seed(h, new BlockPos(4, 2, 2), ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 7000));
        h.runAfterDelay(30, () -> {
            int source = amount(h, new BlockPos(4, 2, 2)), target = amount(h, new BlockPos(4, 2, 8));
            var pump = (IndustrialPumpBlockEntity) h.getBlockEntity(new BlockPos(4, 2, 5));
            h.assertTrue(pump.getSpeed() == 256 && target == 7000, "专用热液路线没有有效动力/交付，speed=" + pump.getSpeed() + " target=" + target);
            h.assertTrue(source + target + pump.cachedFluid().getAmount() == 7000, "源目标缓存有限守恒失败");
            h.assertTrue(capability(h, new BlockPos(4, 2, 8)).drain(7000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "专用送罐后标准抽取绕过禁运"); h.succeed();
        });
    }
    @GameTest(template = "industrial_empty", timeoutTicks = 90)
    public static void poweredIndustrialScRouteConservesFiniteCreateInventory(GameTestHelper h) {
        tankRoute(h, true, true);
        h.runAfterDelay(8, () -> seed(h, new BlockPos(4, 2, 2), BoilerContent.SUPERCRITICAL_STEAM.get(), 7000));
        h.runAfterDelay(30, () -> {
            var pump = (IndustrialPumpBlockEntity) h.getBlockEntity(new BlockPos(4, 2, 5));
            int source = amount(h, new BlockPos(4, 2, 2)), target = amount(h, new BlockPos(4, 2, 8));
            h.assertTrue(target == 7000 && source + target + pump.cachedFluid().getAmount() == 7000, "SC专用有限运输失败：" + source + "/" + target); h.succeed();
        });
    }
    private static void nativeRejected(GameTestHelper h, Fluid fluid) {
        tankRoute(h, false, false);
        h.runAfterDelay(8, () -> seed(h, new BlockPos(4, 2, 2), fluid, 7000));
        h.runAfterDelay(50, () -> {
            var pump = (com.simibubi.create.content.fluids.pump.PumpBlockEntity) h.getBlockEntity(new BlockPos(4, 2, 5));
            h.assertTrue(pump.getSpeed() != 0, "原生禁运负例没有真实动力");
            h.assertTrue(amount(h, new BlockPos(4, 2, 2)) == 7000 && amount(h, new BlockPos(4, 2, 8)) == 0, "原生泵错误运输受限工质"); h.succeed();
        });
    }
    @GameTest(template = "industrial_empty", timeoutTicks = 90)
    public static void nativePoweredPumpRejectsHot(GameTestHelper h) { nativeRejected(h, ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get()); }
    @GameTest(template = "industrial_empty", timeoutTicks = 90)
    public static void nativePoweredPumpRejectsSc(GameTestHelper h) { nativeRejected(h, BoilerContent.SUPERCRITICAL_STEAM.get()); }
    private static void nativeAllowed(GameTestHelper h, Fluid fluid) {
        tankRoute(h, false, false); h.runAfterDelay(8, () -> seed(h, new BlockPos(4, 2, 2), fluid, 2000));
        h.runAfterDelay(70, () -> {
            int source = amount(h, new BlockPos(4, 2, 2)), target = amount(h, new BlockPos(4, 2, 8));
            h.assertTrue(target > 0 && source + target == 2000, "普通Create路线失效或丢液：" + source + "/" + target); h.succeed();
        });
    }
    @GameTest(template = "industrial_empty", timeoutTicks = 100)
    public static void nativeWaterStillTransports(GameTestHelper h) { nativeAllowed(h, Fluids.WATER); }
    @GameTest(template = "industrial_empty", timeoutTicks = 100)
    public static void nativeColdCoolantStillTransports(GameTestHelper h) { nativeAllowed(h, ModFluids.COMPOUND_COOLANT_SOURCE.get()); }
    @GameTest(template = "industrial_empty", timeoutTicks = 100)
    public static void nativeOrdinarySteamStillTransports(GameTestHelper h) { nativeAllowed(h, TurbineContent.STEAM.get()); }

    /** 真实开度包、红石、拆管和动力反向变化后都无需拆泵恢复。 */
    @GameTest(template = "industrial_empty", timeoutTicks = 110)
    public static void valveRedstoneBrokenPipeAndReverseRecover(GameTestHelper h) {
        tankRoute(h, true, false); BlockPos valve = new BlockPos(4, 2, 6), source = new BlockPos(4, 2, 2), target = new BlockPos(4, 2, 8);
        h.setBlock(valve, IndustrialFluidContent.CERAMIC_VALVE.get());
        h.runAfterDelay(8, () -> {
            var entity = (IndustrialValveBlockEntity)h.getBlockEntity(valve);
            var control = (com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour)entity.getAllBehaviours().iterator().next();
            control.setValue(0); seed(h, source, ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 7000);
        });
        h.runAfterDelay(12, () -> {
            h.assertTrue(amount(h, source) == 7000 && amount(h, target) == 0, "关阀仍有交易");
            var entity = (IndustrialValveBlockEntity)h.getBlockEntity(valve);
            ((com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour)entity.getAllBehaviours().iterator().next()).setValue(100);
            h.setBlock(valve.east(), Blocks.REDSTONE_BLOCK);
        });
        h.runAfterDelay(16, () -> { h.assertTrue(amount(h, source) == 7000 && amount(h, target) == 0, "红石高电平未强制关阀"); h.setBlock(valve.east(), Blocks.AIR); });
        h.runAfterDelay(20, () -> {
            h.assertTrue(amount(h, target) == 7000, "开阀后专用路线没有自动恢复");
            ((CreativeMotorBlockEntity)h.getBlockEntity(new BlockPos(5, 2, 3))).generatedSpeed.setValue(256);
        });
        h.runAfterDelay(28, () -> {
            h.assertTrue(amount(h, source) == 7000 && amount(h, target) == 0, "真实反向动力未反转输送");
            h.setBlock(new BlockPos(4, 2, 4), Blocks.AIR);
            ((CreativeMotorBlockEntity)h.getBlockEntity(new BlockPos(5, 2, 3))).generatedSpeed.setValue(-256);
        });
        h.runAfterDelay(34, () -> {
            h.assertTrue(amount(h, source) == 7000 && amount(h, target) == 0, "断管仍凭缓存路径运输");
            h.setBlock(new BlockPos(4, 2, 4), IndustrialFluidContent.CERAMIC_PIPE.get());
        });
        h.runAfterDelay(42, () -> {
            h.assertTrue(amount(h, target) == 7000 && amount(h, source) == 0, "补管后没有自动恢复");
            h.assertTrue(((IndustrialPumpBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 5))).cachedFluid().isEmpty(), "完整交付留下虚假周转"); h.succeed();
        });
    }
    @GameTest(template = "industrial_empty", timeoutTicks = 90)
    public static void creativeTankRestrictedSourceUsesIndustrialRouteAndKeepsInfiniteSemantics(GameTestHelper h) {
        tankRoute(h, true, false); BlockPos source = new BlockPos(4, 2, 2), target = new BlockPos(4, 2, 8);
        h.setBlock(source, AllBlocks.CREATIVE_FLUID_TANK.get());
        h.runAfterDelay(8, () -> {
            var tank = (com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity)h.getBlockEntity(source);
            ((com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity.CreativeSmartFluidTank)tank.getTankInventory())
                    .setContainedFluid(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 1));
            h.assertTrue(capability(h, source).drain(1000, IFluidHandler.FluidAction.SIMULATE).isEmpty(), "创造罐标准模拟泄露SC");
        });
        h.runAfterDelay(30, () -> {
            h.assertTrue(amount(h, target) == 8000 && amount(h, source) == 8000, "创造SC源应保持无限语义且专用目标有限容量不超额"); h.succeed();
        });
    }
    @GameTest(template = "industrial_empty", timeoutTicks = 90)
    public static void creativeTankRestrictedSinkUsesIndustrialRoute(GameTestHelper h) {
        tankRoute(h, true, false); BlockPos source = new BlockPos(4, 2, 2), target = new BlockPos(4, 2, 8);
        h.setBlock(target, AllBlocks.CREATIVE_FLUID_TANK.get());
        h.runAfterDelay(8, () -> seed(h, source, ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 7000));
        h.runAfterDelay(30, () -> {
            h.assertTrue(amount(h, source) == 0 && amount(h, target) == 0, "创造汇应保持销毁槽语义，不生成第二份库存");
            h.assertTrue(capability(h, target).fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 10), IFluidHandler.FluidAction.EXECUTE) == 0, "创造汇标准能力允许热液填入"); h.succeed();
        });
    }
    @GameTest(template = "industrial_empty", timeoutTicks = 70)
    public static void pumpCacheHasOneDropAndSurvivesPlacementAndSave(GameTestHelper h) {
        BlockPos pos = new BlockPos(4, 2, 5); h.setBlock(pos, IndustrialFluidContent.PUMP.get());
        h.runAfterDelay(5, () -> {
            var pump = (IndustrialPumpBlockEntity)h.getBlockEntity(pos);
            var tag = new net.minecraft.nbt.CompoundTag(); tag.put("Cache", new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 3000).save(h.getLevel().registryAccess()));
            pump.restorePortable(tag);
            int old = com.iksxh.create_nuclear_industry.config.IndustrialFluidConfig.PUMP_FLOW.get();
            try {
                com.iksxh.create_nuclear_industry.config.IndustrialFluidConfig.PUMP_FLOW.set(1000);
                h.assertTrue(pump.cachedFluid().getAmount() == 3000, "缩小配置删除了既有缓存");
                var pick = h.getBlockState(pos).getCloneItemStack(new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(pos)), Direction.NORTH, h.absolutePos(pos), false), h.getLevel(), h.absolutePos(pos), net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel()));
                h.assertTrue(!pick.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA), "创造选取复制了缓存");
                var drops = net.minecraft.world.level.block.Block.getDrops(h.getBlockState(pos), h.getLevel(), h.absolutePos(pos), pump);
                h.assertTrue(drops.size() == 1 && drops.getFirst().getCount() == 1, "持液泵不是唯一掉落");
                var data = drops.getFirst().get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
                h.assertTrue(data != null, "唯一掉落没有周转数据");
                h.setBlock(pos, Blocks.AIR); h.setBlock(pos, IndustrialFluidContent.PUMP.get());
                var restored = (IndustrialPumpBlockEntity)h.getBlockEntity(pos); restored.restorePortable(data.copyTag());
                h.assertTrue(restored.cachedFluid().getAmount() == 3000, "拆放没有恢复同份缓存");
                var current = restored.saveWithFullMetadata(h.getLevel().registryAccess()); restored.loadWithComponents(current, h.getLevel().registryAccess());
                h.assertTrue(restored.cachedFluid().getAmount() == 3000, "当前版本保存读取改变缓存数量");
            } finally { com.iksxh.create_nuclear_industry.config.IndustrialFluidConfig.PUMP_FLOW.set(old); }
            h.succeed();
        });
    }
    /** 真实PSI能力提供者保留连接与keepAlive；夹具只设连接前提及同一有限底层库存。 */
    @GameTest(template = "industrial_empty", timeoutTicks = 40)
    public static void portableInterfaceStandardCapabilityRejectsRestrictedWithoutInventoryWrite(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3); h.setBlock(pos, AllBlocks.PORTABLE_FLUID_INTERFACE.get());
        h.runAfterDelay(5, () -> {
            var psi = (com.simibubi.create.content.contraptions.actors.psi.PortableFluidInterfaceBlockEntity)h.getBlockEntity(pos);
            var storage = new net.neoforged.neoforge.fluids.capability.templates.FluidTank(8000);
            try {
                var parent = com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity.class;
                var connected = parent.getDeclaredField("connectedEntity"); connected.setAccessible(true);
                connected.set(psi, new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), 0, 0, 0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK)));
                var timer = parent.getDeclaredField("transferTimer"); timer.setAccessible(true); timer.setInt(psi, 8);
                var capability = psi.getClass().getDeclaredField("capability"); capability.setAccessible(true); capability.set(psi, psi.new InterfaceFluidHandler(storage));
                h.getLevel().invalidateCapabilities(h.absolutePos(pos));
                var handler = capability(h, pos); h.assertTrue(psi.canTransfer(), "PSI禁运负例未满足原连接守卫");
                storage.setFluid(new FluidStack(Fluids.WATER, 1000));
                h.assertTrue(handler.drain(500, IFluidHandler.FluidAction.EXECUTE).getAmount() == 500 && storage.getFluidAmount() == 500, "PSI普通介质/同库语义被破坏");
                for (Fluid fluid : new Fluid[]{ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), BoilerContent.SUPERCRITICAL_STEAM.get()}) {
                    storage.setFluid(new FluidStack(fluid, 2000)); int beforeTimer = timer.getInt(psi);
                    h.assertTrue(handler.getFluidInTank(0).getAmount() == 2000, "PSI标准能力读不到同份受限库存");
                    h.assertTrue(handler.drain(1000, IFluidHandler.FluidAction.SIMULATE).isEmpty() && handler.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "PSI标准能力可抽取受限流体");
                    h.assertTrue(handler.fill(new FluidStack(fluid, 1000), IFluidHandler.FluidAction.SIMULATE) == 0 && handler.fill(new FluidStack(fluid, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "PSI标准能力可灌入受限流体");
                    h.assertTrue(storage.getFluidAmount() == 2000 && timer.getInt(psi) == beforeTimer, "PSI拒绝路径修改库存或keepAlive");
                }
            } catch (ReflectiveOperationException e) { throw new IllegalStateException("锁定PSI连接夹具字段不匹配", e); }
            h.succeed();
        });
    }

    /** 真锅炉四类口默认2048/4096；标准禁运与工业成交共用原物理口账本。 */
    @GameTest(template = "industrial_empty", timeoutTicks = 90)
    public static void boilerFourPhysicalPortKindsUseGroupedSharedRates(GameTestHelper h) {
        ExtensionBoilerGameTests.build(h, 5, 5, 5, 2);
        BlockPos water = new BlockPos(6, 3, 4), hot = new BlockPos(7, 2, 4), cold = new BlockPos(7, 4, 4), steam = new BlockPos(4, 5, 5);
        h.setBlock(new BlockPos(5, 3, 3), Blocks.REDSTONE_BLOCK);
        for (int y : new int[]{2, 4}) {
            for (int z : new int[]{1, 3}) h.setBlock(new BlockPos(7, y, z), IndustrialFluidContent.CERAMIC_PIPE.get());
            h.setBlock(new BlockPos(7, y, 2), IndustrialFluidContent.PUMP.get().defaultBlockState().setValue(IndustrialPumpBlock.FACING, y == 2 ? Direction.SOUTH : Direction.NORTH));
            h.setBlock(new BlockPos(7, y, 0), AllBlocks.FLUID_TANK.get()); motor(h, new BlockPos(7, y, 2), -256);
        }
        h.setBlock(steam.west(), IndustrialFluidContent.CERAMIC_PIPE.get());
        h.setBlock(steam.west(2), IndustrialFluidContent.PUMP.get().defaultBlockState().setValue(IndustrialPumpBlock.FACING, Direction.WEST));
        h.setBlock(steam.west(3), IndustrialFluidContent.CERAMIC_PIPE.get()); h.setBlock(steam.west(4), AllBlocks.FLUID_TANK.get());
        h.setBlock(new BlockPos(2, 4, 5), AllBlocks.COGWHEEL.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
        h.setBlock(new BlockPos(1, 4, 5), AllBlocks.SHAFT.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
        h.setBlock(new BlockPos(0, 4, 5), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST));
        ((CreativeMotorBlockEntity)h.getBlockEntity(new BlockPos(0, 4, 5))).generatedSpeed.setValue(-256);
        boolean[] passiveReady = {false};
        int[] filteredInventory = {0};
        java.util.function.Consumer<net.neoforged.neoforge.event.tick.LevelTickEvent.Post> passive = event -> {
            if (event.getLevel() != h.getLevel() || !passiveReady[0]) return;
            passiveReady[0] = false;
            var cc = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(cold), Direction.NORTH);
            h.assertTrue(cc.drain(1024, IFluidHandler.FluidAction.EXECUTE).getAmount() == 1024, "冷口标准实际交易失败");
        };
        // 高优先级Post与正式调度器在同一gameTime内运行，避免GameTest回调跨tick造成假共用证据。
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, passive);
        h.runAfterDelay(8, () -> {
            var owner = ExtensionBoilerGameTests.owner(h); h.assertTrue(owner.currentForm() != null, "四口锅炉夹具未成型");
            var tag = owner.ledger().save(); tag.putInt("Cold", 4000); ExtensionBoilerGameTests.seedInventories(tag, 0, 0, 14400, 14800); owner.ledger().load(tag); owner.selectMinimum(10);
            var wc = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(water), Direction.NORTH);
            var hc = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(hot), Direction.NORTH);
            var cc = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(cold), Direction.NORTH);
            var sc = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(steam), Direction.WEST);
            h.assertTrue(wc.fill(new FluidStack(Fluids.WATER, 10000), IFluidHandler.FluidAction.SIMULATE) == 4096 && owner.ledger().water() == 0, "水口模拟不是4096零写");
            h.assertTrue(wc.fill(new FluidStack(Fluids.WATER, 3000), IFluidHandler.FluidAction.EXECUTE) == 3000
                    && wc.fill(new FluidStack(Fluids.WATER, 10000), IFluidHandler.FluidAction.EXECUTE) == 1096, "给水口同tick没有共享4096");
            h.assertTrue(hc.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 1000), IFluidHandler.FluidAction.SIMULATE) == 0, "锅炉热口标准能力绕过禁运");
            h.assertTrue(sc.drain(1000, IFluidHandler.FluidAction.SIMULATE).isEmpty() && sc.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "SC口标准能力绕过禁运");
            h.assertTrue(cc.drain(10000, IFluidHandler.FluidAction.SIMULATE).getAmount() == 2048 && owner.ledger().cold() == 4000, "冷口模拟不是2048零写");
            passiveReady[0] = true;
            seed(h, new BlockPos(7, 2, 0), ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 7000);
        });
        h.runAfterDelay(9, () -> {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(passive);
            h.assertTrue(amount(h, new BlockPos(7, 2, 0)) == 4952 && ExtensionBoilerGameTests.owner(h).ledger().hot() == 2048, "热口实际专用流量不是2048");
            h.assertTrue(amount(h, new BlockPos(7, 4, 0)) == 1024, "同tick标准冷口1024＋工业冷口没有共用2048，actual=" + amount(h, new BlockPos(7, 4, 0)));
            h.assertTrue(amount(h, steam.west(4)) == 4096, "SC专用口实际流量不是4096，pumpSpeed=" + ((IndustrialPumpBlockEntity)h.getBlockEntity(steam.west(2))).getSpeed());
            System.out.println("[industrial] physical boiler water4096 hot2048 cold1024+1024 SC4096; standard restricted transactions zero");
            filteredInventory[0] = ExtensionBoilerGameTests.owner(h).ledger().steam(com.iksxh.create_nuclear_industry.boiler.BoilerSteamInventoryKind.SUPERCRITICAL);
            BoilerSteamSelectionGameTests.submit(h, steam, 0);
        });
        h.runAfterDelay(10, () -> {
            h.assertTrue(amount(h, steam.west(4)) == 4096 && ExtensionBoilerGameTests.owner(h).ledger().steam(com.iksxh.create_nuclear_industry.boiler.BoilerSteamInventoryKind.SUPERCRITICAL) == filteredInventory[0], "切普通汽过滤仍偷取SC或混输");
            BoilerSteamSelectionGameTests.submit(h, steam, 1);
        });
        h.runAfterDelay(11, () -> {
            h.assertTrue(amount(h, steam.west(4)) == 8000 && ExtensionBoilerGameTests.owner(h).ledger().steam(com.iksxh.create_nuclear_industry.boiler.BoilerSteamInventoryKind.SUPERCRITICAL) == filteredInventory[0] - 3904, "过滤恢复后没有按有限余容量3904恢复");
            System.out.println("[industrial] SC filter stop preserves stock; restore transfers3904 to finite full target8000 without pump replacement"); h.succeed();
        });
    }
    @GameTest(template = "industrial_empty", timeoutTicks = 90)
    public static void unknownCreateEndpointCannotReceiveRestrictedIndustrialFluid(GameTestHelper h) {
        tankRoute(h, true, false); BlockPos source = new BlockPos(4, 2, 2), target = new BlockPos(4, 2, 8);
        h.setBlock(target, AllBlocks.BASIN.get());
        h.runAfterDelay(8, () -> { h.assertTrue(capability(h, target) != null, "未知端点没有真实标准流体能力"); seed(h, source, ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 7000); });
        h.runAfterDelay(30, () -> {
            h.assertTrue(amount(h, source) == 7000 && amount(h, target) == 0, "未适配库存被自动授权接收受限工质");
            h.assertTrue(((IndustrialPumpBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 5))).cachedFluid().isEmpty(), "未知端点拒绝仍抽源入缓存"); h.succeed();
        });
    }
    /** 公开能力随绑定撤销；专用泵在结构修复后通过新能力恢复，旧能力不会复活。 */
    @GameTest(template = "industrial_empty", timeoutTicks = 70)
    public static void reactorHotPortRestrictsStandardAndRebindsIndustrialWithoutOldHandle(GameTestHelper h) {
        BlockPos origin = new BlockPos(4, 1, 6), instrument = origin.offset(1, 1, 0), hot = origin.offset(3, 1, 0), rod = origin.offset(1, 1, 1);
        for (int x = 0; x < 5; x++) for (int y = 0; y < 5; y++) for (int z = 0; z < 5; z++) {
            boolean frame = x == 0 || x == 4 || y == 0 || y == 4 || z == 0 || z == 4;
            h.setBlock(origin.offset(x, y, z), frame ? P1Blocks.REACTOR_CASING.get().defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        for (int y = 1; y <= 3; y++) h.setBlock(origin.offset(1, y, 1), P1Blocks.REACTOR_FUEL_ROD.get());
        h.setBlock(origin.offset(1, 4, 1), P1Blocks.REACTOR_REFUELING_PORT.get()); h.setBlock(instrument, P1Blocks.REACTOR_INSTRUMENT_PORT.get());
        h.setBlock(hot, P1Blocks.REACTOR_HOT_PORT.get()); h.setBlock(origin.offset(4, 1, 1), P1Blocks.REACTOR_COLD_PORT.get());
        h.setBlock(hot.north(), IndustrialFluidContent.CERAMIC_PIPE.get()); h.setBlock(hot.north(2), IndustrialFluidContent.PUMP.get().defaultBlockState().setValue(IndustrialPumpBlock.FACING, Direction.NORTH));
        h.setBlock(hot.north(3), IndustrialFluidContent.CERAMIC_PIPE.get()); h.setBlock(hot.north(4), AllBlocks.FLUID_TANK.get()); motor(h, hot.north(2), -256);
        ReactorStructureLifecycle.rescanAroundNow(h.getLevel(), h.absolutePos(instrument)); IFluidHandler[] stale = {null};
        h.runAfterDelay(7, () -> {
            var owner = (ReactorInstrumentPortBlockEntity)h.getBlockEntity(instrument); h.assertTrue(owner.structureValid(), "热口夹具未成型"); owner.setCoolantInventories(0, 5000);
            stale[0] = capability(h, hot); h.assertTrue(stale[0].drain(1000, IFluidHandler.FluidAction.SIMULATE).isEmpty() && stale[0].drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "反应堆公开热口可被原生能力抽取");
        });
        h.runAfterDelay(8, () -> {
            h.assertTrue(amount(h, hot.north(4)) == 2048, "反应堆热口实际工业交易不是2048"); h.setBlock(rod, Blocks.AIR); ReactorStructureLifecycle.rescanAroundNow(h.getLevel(), h.absolutePos(instrument));
        });
        h.runAfterDelay(12, () -> {
            h.assertTrue(amount(h, hot.north(4)) == 2048 && stale[0].getTankCapacity(0) == 0, "失效绑定仍授权工业交易/旧能力");
            h.setBlock(rod, P1Blocks.REACTOR_FUEL_ROD.get()); ReactorStructureLifecycle.rescanAroundNow(h.getLevel(), h.absolutePos(instrument));
        });
        h.runAfterDelay(18, () -> {
            h.assertTrue(stale[0].getTankCapacity(0) == 0 && amount(h, hot.north(4)) == 5000, "修复后旧能力复活或工业泵未恢复"); h.succeed();
        });
    }
    /** 真区块事件和当前保存重载；显式加载仅由夹具完成，生产网络只能检查hasChunkAt。 */
    @GameTest(template = "industrial_empty", timeoutTicks = 470, batch = "industrial_chunk_lifecycle")
    public static void remoteChunkUnloadsWithoutForcedTransportAndRecoversSavedInventory(GameTestHelper h) {
        BlockPos root = new BlockPos(160, 2, 160), source = root.north(2), target = root.south(224);
        var level = h.getLevel(); var ownerChunk = new net.minecraft.world.level.ChunkPos(h.absolutePos(root));
        var targetChunk = new net.minecraft.world.level.ChunkPos(h.absolutePos(target));
        // owner票级31会传播到生成级44；至少14区块才会真正物理卸载，FULL不可用不足以作证。
        int chunkDistance = Math.max(Math.abs(ownerChunk.x - targetChunk.x), Math.abs(ownerChunk.z - targetChunk.z));
        h.assertTrue(chunkDistance >= 14, "卸载夹具仍处于owner票据生成传播范围");
        var ticket = net.minecraft.server.level.TicketType.<net.minecraft.world.level.ChunkPos>create("industrial_loaded_fixture", java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong));
        var ownerTicket = net.minecraft.server.level.TicketType.<net.minecraft.world.level.ChunkPos>create("industrial_owner_fixture", java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong));
        java.util.Set<net.minecraft.world.level.ChunkPos> chunks = new java.util.HashSet<>();
        for (int z = -2; z <= 224; z++) chunks.add(new net.minecraft.world.level.ChunkPos(h.absolutePos(root.offset(0, 0, z))));
        for (var chunk : chunks) {
            level.getChunkSource().addRegionTicket(ticket, chunk, 0, chunk);
            // 初始夹具显式进入真实加载/计时状态，随后撤票；不能把仅FULL可用当成已发卸载事件。
            level.setChunkForced(chunk.x, chunk.z, true);
        }
        level.getChunkSource().addRegionTicket(ownerTicket, ownerChunk, 2, ownerChunk);
        h.setBlock(source, AllBlocks.FLUID_TANK.get()); h.setBlock(target, AllBlocks.FLUID_TANK.get());
        h.setBlock(root.north(), IndustrialFluidContent.CERAMIC_PIPE.get());
        for (int z = 1; z < 224; z++) h.setBlock(root.south(z), IndustrialFluidContent.CERAMIC_PIPE.get());
        h.setBlock(root, IndustrialFluidContent.PUMP.get().defaultBlockState().setValue(IndustrialPumpBlock.FACING, Direction.SOUTH)); motor(h, root, -256);
        int[] unloads = {0}; java.util.function.Consumer<net.neoforged.neoforge.event.level.ChunkEvent.Unload> listener = event -> {
            if (event.getLevel() == level && event.getChunk().getPos().equals(targetChunk)) unloads[0]++;
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        h.runAfterDelay(12, () -> seed(h, source, ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 3000));
        h.runAfterDelay(20, () -> {
            h.assertTrue(amount(h, target) == 3000, "卸载前长合法专用路线没有真实运输");
            for (var chunk : chunks) { level.setChunkForced(chunk.x, chunk.z, false); level.getChunkSource().removeRegionTicket(ticket, chunk, 0, chunk); }
        });
        h.runAfterDelay(360, () -> {
            level.getChunkSource().tick(() -> true, false);
            h.assertTrue(unloads[0] > 0 && !level.hasChunkAt(h.absolutePos(target)), "目标区块没有发生真实卸载，events=" + unloads[0] + " loaded=" + level.hasChunkAt(h.absolutePos(target)) + " forced=" + level.getForcedChunks().contains(targetChunk.toLong()));
            h.assertTrue(level.hasChunkAt(h.absolutePos(source)), "保留源与泵的owner票据失效");
            seed(h, source, ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 3000);
        });
        h.runAfterDelay(372, () -> {
            h.assertTrue(!level.hasChunkAt(h.absolutePos(target)) && amount(h, source) == 3000, "生产网络强载区块或从源抽入无目标缓存");
            for (var chunk : chunks) { level.getChunkSource().addRegionTicket(ticket, chunk, 0, chunk); level.setChunkForced(chunk.x, chunk.z, true); level.getChunkSource().getChunk(chunk.x, chunk.z, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true); }
        });
        h.runAfterDelay(392, () -> {
            h.assertTrue(amount(h, source) == 0 && amount(h, target) == 6000, "当前区块保存读取后库存不守恒或没有自动恢复，source=" + amount(h, source) + " target=" + amount(h, target));
            h.assertTrue(((IndustrialPumpBlockEntity)h.getBlockEntity(root)).cachedFluid().isEmpty(), "区块恢复复制了周转缓存");
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
            for (var chunk : chunks) { level.setChunkForced(chunk.x, chunk.z, false); level.getChunkSource().removeRegionTicket(ticket, chunk, 0, chunk); }
            level.getChunkSource().removeRegionTicket(ownerTicket, ownerChunk, 2, ownerChunk);
            System.out.println("[industrial] real ChunkEvent.Unload=" + unloads[0] + ", no forced load, saved3000+new3000=6000mB"); h.succeed();
        });
    }

    /** 真区块事件和当前保存重载；显式加载仅由夹具完成，生产网络只能检查hasChunkAt。 */
    @GameTest(template = "industrial_empty", timeoutTicks = 480, batch = "industrial_review_chunk")
    public static void boundaryValveSecondOrderSignalDoesNotForceChunkAndRecovers(GameTestHelper h) {
        BlockPos proposed = new BlockPos(160, 2, 160);
        BlockPos root = proposed.offset(14 - (h.absolutePos(proposed).getX() & 15), 0, -(h.absolutePos(proposed).getZ() & 15));
        BlockPos source = root.north(2), target = root.south(226), valve = root.south(224);
        var level = h.getLevel(); var ownerChunk = new net.minecraft.world.level.ChunkPos(h.absolutePos(root));
        var targetChunk = new net.minecraft.world.level.ChunkPos(h.absolutePos(target));
        // owner票级31会传播到生成级44；至少14区块才会真正物理卸载，FULL不可用不足以作证。
        int chunkDistance = Math.max(Math.abs(ownerChunk.x - targetChunk.x), Math.abs(ownerChunk.z - targetChunk.z));
        h.assertTrue(chunkDistance >= 14, "卸载夹具仍处于owner票据生成传播范围");
        var ticket = net.minecraft.server.level.TicketType.<net.minecraft.world.level.ChunkPos>create("industrial_loaded_fixture", java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong));
        var ownerTicket = net.minecraft.server.level.TicketType.<net.minecraft.world.level.ChunkPos>create("industrial_owner_fixture", java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong));
        java.util.Set<net.minecraft.world.level.ChunkPos> chunks = new java.util.HashSet<>();
        for (int z = -2; z <= 226; z++) chunks.add(new net.minecraft.world.level.ChunkPos(h.absolutePos(root.offset(0, 0, z))));
        var signalChunk = new net.minecraft.world.level.ChunkPos(h.absolutePos(valve.east(2)));
        level.setChunkForced(signalChunk.x, signalChunk.z, true);
        level.getChunkSource().getChunk(signalChunk.x, signalChunk.z, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);
        for (var chunk : chunks) {
            level.getChunkSource().addRegionTicket(ticket, chunk, 0, chunk);
            // 初始夹具显式进入真实加载/计时状态，随后撤票；不能把仅FULL可用当成已发卸载事件。
            level.setChunkForced(chunk.x, chunk.z, true);
        }
        level.getChunkSource().addRegionTicket(ownerTicket, ownerChunk, 2, ownerChunk);
        h.setBlock(source, AllBlocks.FLUID_TANK.get()); h.setBlock(target, AllBlocks.FLUID_TANK.get());
        h.setBlock(root.north(), IndustrialFluidContent.CERAMIC_PIPE.get());
        for (int z = 1; z < 226; z++) h.setBlock(root.south(z), IndustrialFluidContent.CERAMIC_PIPE.get());
        h.setBlock(root, IndustrialFluidContent.PUMP.get().defaultBlockState().setValue(IndustrialPumpBlock.FACING, Direction.SOUTH)); motor(h, root, -256);
        h.setBlock(valve, IndustrialFluidContent.REINFORCED_VALVE.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Z));
        h.setBlock(valve.east(), Blocks.STONE);
        int[] unloads = {0}; java.util.function.Consumer<net.neoforged.neoforge.event.level.ChunkEvent.Unload> listener = event -> {
            if (event.getLevel() == level && event.getChunk().getPos().equals(targetChunk)) unloads[0]++;
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        h.runAfterDelay(12, () -> seed(h, source, ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 3000));
        h.runAfterDelay(20, () -> {
            h.assertTrue(amount(h, target) == 3000, "卸载前长合法专用路线没有真实运输");
            level.setChunkForced(signalChunk.x, signalChunk.z, false);
            for (var chunk : chunks) { level.setChunkForced(chunk.x, chunk.z, false); level.getChunkSource().removeRegionTicket(ticket, chunk, 0, chunk); }
        });
        h.runAfterDelay(360, () -> {
            level.getChunkSource().tick(() -> true, false);
            h.assertTrue(unloads[0] > 0 && !level.hasChunkAt(h.absolutePos(target)), "目标区块没有发生真实卸载，events=" + unloads[0] + " loaded=" + level.hasChunkAt(h.absolutePos(target)) + " forced=" + level.getForcedChunks().contains(targetChunk.toLong()));
            h.assertTrue(level.hasChunkAt(h.absolutePos(source)), "保留源与泵的owner票据失效");
            seed(h, source, ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 3000);
        });
        h.runAfterDelay(372, () -> {
            h.assertTrue(!level.hasChunkAt(h.absolutePos(target)) && amount(h, source) == 3000, "生产网络强载区块或从源抽入无目标缓存");
            for (var chunk : chunks) { level.getChunkSource().addRegionTicket(ticket, chunk, 0, chunk); level.getChunkSource().getChunk(chunk.x, chunk.z, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true); }
        });
        h.runAfterDelay(392, () -> {
            var entity = (IndustrialValveBlockEntity)h.getBlockEntity(valve);
            entity.updateSignal();
            h.assertTrue(!level.hasChunkAt(h.absolutePos(valve.east(2))) && entity.effectiveLimit() == 0, "二阶强导体红石读强载缺失邻区块或未安全关阀");
            h.assertTrue(amount(h, source) == 3000 && amount(h, target) == 3000, "红石边界缺块仍交易有限液");
            var tooltip = new java.util.ArrayList<net.minecraft.network.chat.Component>(); entity.addToGoggleTooltip(tooltip, false);
            h.assertTrue(tooltip.stream().anyMatch(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().endsWith("unloaded_redstone")), "红石缺块关阀没有可读诊断");
            level.setChunkForced(signalChunk.x, signalChunk.z, true); level.getChunkSource().getChunk(signalChunk.x, signalChunk.z, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);
        });
        h.runAfterDelay(405, () -> {
            h.assertTrue(amount(h, source) == 0 && amount(h, target) == 6000, "阀/红石区块重载后没有自动恢复6000有限守恒");
            h.assertTrue(((IndustrialPumpBlockEntity)h.getBlockEntity(root)).cachedFluid().isEmpty(), "边界恢复复制缓存");
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
            level.setChunkForced(signalChunk.x, signalChunk.z, false);
            for (var chunk : chunks) { level.setChunkForced(chunk.x, chunk.z, false); level.getChunkSource().removeRegionTicket(ticket, chunk, 0, chunk); }
            level.getChunkSource().removeRegionTicket(ownerTicket, ownerChunk, 2, ownerChunk);
            System.out.println("[industrial-review] boundary valve ChunkEvent.Unload=" + unloads[0] + "; second-order signal missing chunk stays unloaded, safely closed; load restores finite6000"); h.succeed();
        });
    }

    /** 十个真实物理口，两条不共边专用路线；有限源与共享反应堆容量支持连续五tick交付。 */
    @GameTest(template = "industrial_empty", timeoutTicks = 100)
    public static void tenPhysicalReactorPortsDeliverTwentyThousandPerTick(GameTestHelper h) {
        BlockPos origin = new BlockPos(3, 1, 7);
        for (int x = 0; x < 16; x++) for (int y = 0; y < 5; y++) for (int z = 0; z < 5; z++) {
            boolean frame = x == 0 || x == 15 || y == 0 || y == 4 || z == 0 || z == 4;
            h.setBlock(origin.offset(x, y, z), frame ? P1Blocks.REACTOR_CASING.get().defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        for (int y = 1; y < 4; y++) h.setBlock(origin.offset(1, y, 1), P1Blocks.REACTOR_FUEL_ROD.get());
        h.setBlock(origin.offset(1, 4, 1), P1Blocks.REACTOR_REFUELING_PORT.get());
        h.setBlock(origin.offset(1, 1, 0), P1Blocks.REACTOR_INSTRUMENT_PORT.get());
        h.setBlock(origin.offset(15, 1, 1), P1Blocks.REACTOR_HOT_PORT.get());
        for (int x : new int[]{5, 6, 7, 8, 9, 12, 13, 14, 15, 16}) {
            h.setBlock(new BlockPos(x, 2, 7), P1Blocks.REACTOR_COLD_PORT.get());
            h.setBlock(new BlockPos(x, 2, 6), IndustrialFluidContent.CERAMIC_PIPE.get());
        }
        for (int x : new int[]{7, 14}) {
            for (int y = 1; y <= 10; y++) h.setBlock(new BlockPos(x, y, 2), AllBlocks.FLUID_TANK.get());
            h.setBlock(new BlockPos(x, 2, 3), IndustrialFluidContent.CERAMIC_PIPE.get());
            h.setBlock(new BlockPos(x, 2, 4), IndustrialFluidContent.CERAMIC_PIPE.get());
            h.setBlock(new BlockPos(x, 2, 5), IndustrialFluidContent.PUMP.get().defaultBlockState().setValue(IndustrialPumpBlock.FACING, Direction.SOUTH));
            motor(h, new BlockPos(x, 2, 5), -256);
        }
        ReactorStructureLifecycle.rescanAroundNow(h.getLevel(), h.absolutePos(origin.offset(1, 1, 0)));
        h.runAfterDelay(12, () -> {
            var owner = (ReactorInstrumentPortBlockEntity) h.getBlockEntity(origin.offset(1, 1, 0));
            h.assertTrue(owner.structureValid(), "十口反应堆未成型");
            h.assertTrue(owner.boundPorts(com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity.BindingType.COLD_COOLANT).size() == 10, "并联夹具不是十个真实绑定口");
            seed(h, new BlockPos(7, 2, 2), ModFluids.COMPOUND_COOLANT_SOURCE.get(), 80000);
            seed(h, new BlockPos(14, 2, 2), ModFluids.COMPOUND_COOLANT_SOURCE.get(), 80000);
            h.assertTrue(amount(h, new BlockPos(7, 2, 2)) + amount(h, new BlockPos(14, 2, 2)) == 160000, "有限源真实罐容量没有形成160000mB");
        });
        long[] previous = {0}; int[] samples = {0};
        h.onEachTick(() -> {
            if (h.getTick() < 14 || h.getTick() > 18) return;
            var owner = (ReactorInstrumentPortBlockEntity) h.getBlockEntity(origin.offset(1, 1, 0));
            long current = owner.snapshot().coldCoolantMb();
            if (samples[0] > 0) h.assertTrue(current - previous[0] == 20480, "十口真实合计非20480mB/t：" + (current - previous[0])
                    + " cold=" + current + " source=" + (amount(h, new BlockPos(7, 2, 2)) + amount(h, new BlockPos(14, 2, 2)))
                    + " speed=" + ((IndustrialPumpBlockEntity)h.getBlockEntity(new BlockPos(7, 2, 5))).getSpeed());
            previous[0] = current; samples[0]++;
            long source = amount(h, new BlockPos(7, 2, 2)) + amount(h, new BlockPos(14, 2, 2));
            long cache = ((IndustrialPumpBlockEntity) h.getBlockEntity(new BlockPos(7, 2, 5))).cachedFluid().getAmount()
                    + ((IndustrialPumpBlockEntity) h.getBlockEntity(new BlockPos(14, 2, 5))).cachedFluid().getAmount();
            h.assertTrue(source + current + owner.snapshot().hotCoolantMb() + cache == 160000, "十口源/共享目标/缓存不守恒");
            if (samples[0] == 5) { System.out.println("[industrial] 10 real ports, 2 powered independent pumps, 5 ticks x20480mB/t, finite total160000mB"); h.succeed(); }
        });
    }
    @GameTest(template = "industrial_empty", timeoutTicks = 40)
    public static void standardCreateTankRejectsHotCoolantBeforeDraining(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3); helper.setBlock(pos, AllBlocks.FLUID_TANK.get());
        helper.runAfterDelay(5, () -> {
            var tank = (FluidTankBlockEntity) helper.getBlockEntity(pos);
            tank.getTankInventory().fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 3000), IFluidHandler.FluidAction.EXECUTE);
            var handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.WEST);
            helper.assertTrue(handler.getFluidInTank(0).getAmount() == 3000, "标准读能力必须看到同份热液库存");
            helper.assertTrue(handler.drain(1000, IFluidHandler.FluidAction.SIMULATE).isEmpty(), "标准能力模拟不能抽取受限热液");
            helper.assertTrue(handler.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "标准能力执行不能抽取受限热液");
            helper.assertTrue(tank.getTankInventory().getFluidAmount() == 3000, "拒绝路径必须保留源库存"); helper.succeed();
        });
    }
}
