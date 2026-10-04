package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.config.TurbineConfig;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.iksxh.create_nuclear_industry.turbine.TurbineControllerBlockEntity;
import com.iksxh.create_nuclear_industry.turbine.TurbineGeometry;
import com.iksxh.create_nuclear_industry.turbine.TurbineOutputShaftBlockEntity;
import com.iksxh.create_nuclear_industry.turbine.TurbinePartBlock;
import com.iksxh.create_nuclear_industry.turbine.TurbineShaftBlock;
import com.iksxh.create_nuclear_industry.turbine.TurbineState;
import com.iksxh.create_nuclear_industry.turbine.TurbineStructure;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 三档侧控汽轮机的真实世界结构、双轴、库存和原生管网场景。 */
@GameTestHolder("create_nuclear_industry_turbine")
@PrefixGameTestTemplate(false)
public final class ExtensionTurbineGameTests {
    private static final BlockPos FRONT = new BlockPos(5, 5, 1);
    private ExtensionTurbineGameTests() {}

    /** 实际汽轮机两端各接 Create 风扇：单端可用超过半额，两端合计超限时同网过载。 */
    @GameTest(template = "turbine_empty", timeoutTicks = 80)
    public static void sharedCapacityDrivesEitherEndAndOverloadsTogether(GameTestHelper helper) {
        TurbineState.Settings settings = TurbineConfig.settings();
        TurbineState.Tier tier = settings.shortTier();
        build(helper, FRONT, tier.rotorCount(), false);
        BlockPos rearPos = part(FRONT, 0, 0, tier.length() - 1);
        BlockPos frontFanPos = FRONT.north();
        BlockPos rearFanPos = rearPos.south();
        helper.setBlock(frontFanPos, AllBlocks.ENCASED_FAN.getDefaultState()
                .setValue(EncasedFanBlock.FACING, Direction.NORTH));
        helper.runAfterDelay(12, () -> {
            KineticBlockEntity fan = (KineticBlockEntity) helper.getLevel()
                    .getBlockEntity(helper.absolutePos(frontFanPos));
            float oneLoad = fan.calculateStressApplied() * settings.rpm();
            int steam = (int) Math.ceil(oneLoad * 1.5 * settings.smoothingTicks()
                    / settings.suPerMbPerTick());
            require(helper, oneLoad > 0 && steam > 0 && steam <= tier.ratedFlowMbPerTick(),
                    "测试负载无法由少量蒸汽构造半额以上的边界");
            IFluidHandler input = handler(helper, inlet(FRONT, tier), Direction.WEST);
            require(helper, input != null && input.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), steam),
                    IFluidHandler.FluidAction.EXECUTE) == steam, "实际机组未收到测试蒸汽");
        });
        helper.runAfterDelay(18, () -> {
            TurbineOutputShaftBlockEntity front = shaft(helper, FRONT);
            TurbineOutputShaftBlockEntity rear = shaft(helper, rearPos);
            KineticBlockEntity fan = (KineticBlockEntity) helper.getLevel()
                    .getBlockEntity(helper.absolutePos(frontFanPos));
            float capacity = front.getOrCreateNetwork().calculateCapacity();
            float load = front.getOrCreateNetwork().calculateStress();
            require(helper, front.network.equals(rear.network) && load > capacity / 2 && load < capacity
                    && !front.isOverStressed() && !rear.isOverStressed()
                    && Math.abs(fan.getSpeed()) == settings.rpm(), "前端单独超过旧半额未正常运转");
            helper.setBlock(frontFanPos, Blocks.AIR);
            helper.setBlock(rearFanPos, AllBlocks.ENCASED_FAN.getDefaultState()
                    .setValue(EncasedFanBlock.FACING, Direction.SOUTH));
        });
        helper.runAfterDelay(23, () -> {
            TurbineOutputShaftBlockEntity front = shaft(helper, FRONT);
            TurbineOutputShaftBlockEntity rear = shaft(helper, rearPos);
            KineticBlockEntity fan = (KineticBlockEntity) helper.getLevel()
                    .getBlockEntity(helper.absolutePos(rearFanPos));
            float capacity = rear.getOrCreateNetwork().calculateCapacity();
            float load = rear.getOrCreateNetwork().calculateStress();
            require(helper, front.network.equals(rear.network) && load > capacity / 2 && load < capacity
                    && !front.isOverStressed() && !rear.isOverStressed()
                    && Math.abs(fan.getSpeed()) == settings.rpm(), "后端单独超过旧半额未正常运转");
            helper.setBlock(frontFanPos, AllBlocks.ENCASED_FAN.getDefaultState()
                    .setValue(EncasedFanBlock.FACING, Direction.NORTH));
        });
        helper.runAfterDelay(28, () -> {
            TurbineOutputShaftBlockEntity front = shaft(helper, FRONT);
            TurbineOutputShaftBlockEntity rear = shaft(helper, rearPos);
            require(helper, front.network.equals(rear.network)
                    && front.getOrCreateNetwork().calculateStress() > front.getOrCreateNetwork().calculateCapacity()
                    && front.isOverStressed() && rear.isOverStressed(), "双端合计超限未共同过载");
            helper.setBlock(frontFanPos, Blocks.AIR);
        });
        helper.runAfterDelay(33, () -> {
            require(helper, !shaft(helper, FRONT).isOverStressed()
                    && !shaft(helper, rearPos).isOverStressed(), "减载后两端未一起恢复");
            helper.succeed();
        });
    }

    /** 当前格式机组供汽后保存恢复，账本历史归零时不重复登记本机容量。 */
    @GameTest(template = "turbine_empty", timeoutTicks = 80)
    public static void currentFormatReloadKeepsOnlyLiveCapacity(GameTestHelper helper) {
        TurbineState.Tier tier = TurbineConfig.settings().shortTier();
        build(helper, FRONT, tier.rotorCount(), false);
        BlockPos rear = part(FRONT, 0, 0, tier.length() - 1);
        BlockPos motorPos = FRONT.north();
        helper.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.SOUTH));
        CreativeMotorBlockEntity motor = (CreativeMotorBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(motorPos));
        motor.generatedSpeed.setValue(TurbineConfig.settings().rpm());
        helper.runAfterDelay(13, () -> {
            IFluidHandler input = handler(helper, inlet(FRONT, tier), Direction.WEST);
            require(helper, input != null && input.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 40),
                    IFluidHandler.FluidAction.EXECUTE) == 40, "当前格式恢复前机组未接收蒸汽");
        });
        helper.runAfterDelay(20, () -> {
            TurbineControllerBlockEntity owner = owner(helper, FRONT);
            CreativeMotorBlockEntity liveMotor = (CreativeMotorBlockEntity) helper.getLevel()
                    .getBlockEntity(helper.absolutePos(motorPos));
            BlockPos frontWorld = helper.absolutePos(FRONT);
            BlockPos rearWorld = helper.absolutePos(rear);
            BlockPos motorWorld = helper.absolutePos(motorPos);
            CompoundTag frontTag = shaft(helper, FRONT).saveWithFullMetadata(helper.getLevel().registryAccess());
            CompoundTag rearTag = shaft(helper, rear).saveWithFullMetadata(helper.getLevel().registryAccess());
            CompoundTag motorTag = liveMotor.saveWithFullMetadata(helper.getLevel().registryAccess());
            require(helper, owner.ledger().totalSu() > 0
                    && frontTag.getCompound("Network").getFloat("AddedCapacity") > 0
                    && frontTag.getCompound("Network").getFloat("TurbineGeneratedRpm")
                    == TurbineConfig.settings().rpm(),
                    "当前格式保存前缺少真实机组生成容量");
            // 当前格式的控制器恢复会清空临时供汽历史；按电机、前轴、后轴顺序重建实体。
            owner.ledger().load(owner.ledger().save());
            replaceKinetic(helper, motorWorld, new CreativeMotorBlockEntity(AllBlockEntityTypes.MOTOR.get(),
                    motorWorld, helper.getLevel().getBlockState(motorWorld)), motorTag);
            replaceKinetic(helper, frontWorld, new TurbineOutputShaftBlockEntity(frontWorld,
                    helper.getLevel().getBlockState(frontWorld)), frontTag);
            replaceKinetic(helper, rearWorld, new TurbineOutputShaftBlockEntity(rearWorld,
                    helper.getLevel().getBlockState(rearWorld)), rearTag);
        });
        helper.runAfterDelay(21, () -> {
            CreativeMotorBlockEntity liveMotor = (CreativeMotorBlockEntity) helper.getLevel()
                    .getBlockEntity(helper.absolutePos(motorPos));
            float external = liveMotor.calculateAddedStressCapacity() * Math.abs(liveMotor.getGeneratedSpeed());
            float actual = liveMotor.getOrCreateNetwork().calculateCapacity();
            require(helper, actual <= external + 2,
                    "当前格式恢复首tick重复容量：实际=" + actual + " 外源=" + external);
        });
        helper.runAfterDelay(27, () -> {
            CreativeMotorBlockEntity liveMotor = (CreativeMotorBlockEntity) helper.getLevel()
                    .getBlockEntity(helper.absolutePos(motorPos));
            float external = liveMotor.calculateAddedStressCapacity() * Math.abs(liveMotor.getGeneratedSpeed());
            float actual = liveMotor.getOrCreateNetwork().calculateCapacity();
            require(helper, shaft(helper, FRONT).network.equals(shaft(helper, rear).network)
                    && Math.abs(actual - external) < 2,
                    "当前格式恢复后外源网络残留本机容量：实际=" + actual + " 外源=" + external);
            helper.succeed();
        });
    }

    private static void replaceKinetic(GameTestHelper helper, BlockPos pos, BlockEntity replacement,
                                       CompoundTag snapshot) {
        helper.getLevel().removeBlockEntity(pos);
        replacement.loadWithComponents(snapshot, helper.getLevel().registryAccess());
        helper.getLevel().setBlockEntity(replacement);
    }

    /** 旧控制器动力NBT只触发旧Source分支清理，原账本库存继续保留。 */
    @GameTest(template = "turbine_empty", timeoutTicks = 50)
    public static void oldControllerKineticSourceDoesNotSurviveMigration(GameTestHelper helper) {
        BlockPos control = new BlockPos(5, 5, 5);
        BlockPos oldShaft = control.east();
        helper.setBlock(control, TurbineContent.CONTROLLER.get().defaultBlockState());
        helper.setBlock(oldShaft, AllBlocks.SHAFT.getDefaultState()
                .setValue(ShaftBlock.AXIS, Direction.Axis.X));
        helper.runAfterDelay(2, () -> {
            TurbineControllerBlockEntity owner = (TurbineControllerBlockEntity) helper.getLevel()
                    .getBlockEntity(helper.absolutePos(control));
            KineticBlockEntity shaft = (KineticBlockEntity) helper.getLevel()
                    .getBlockEntity(helper.absolutePos(oldShaft));
            CompoundTag stock = new CompoundTag();
            stock.putInt("Input", 700);
            owner.ledger().load(stock);
            CompoundTag legacy = owner.saveWithFullMetadata(helper.getLevel().registryAccess());
            legacy.putFloat("Speed", 128);
            CompoundTag oldNetwork = new CompoundTag();
            oldNetwork.putLong("Id", helper.absolutePos(control).asLong());
            oldNetwork.putFloat("Capacity", 16384);
            legacy.put("Network", oldNetwork);
            owner.loadWithComponents(legacy, helper.getLevel().registryAccess());
            shaft.source = helper.absolutePos(control);
            shaft.setSpeed(128);
            shaft.network = helper.absolutePos(control).asLong();
            require(helper, owner.ledger().input() == 700 && shaft.hasSource(), "旧NBT测试前置无效");
        });
        helper.runAfterDelay(5, () -> {
            TurbineControllerBlockEntity owner = (TurbineControllerBlockEntity) helper.getLevel()
                    .getBlockEntity(helper.absolutePos(control));
            KineticBlockEntity shaft = (KineticBlockEntity) helper.getLevel()
                    .getBlockEntity(helper.absolutePos(oldShaft));
            require(helper, owner.ledger().input() == 700, "迁移清理删除了唯一库存");
            require(helper, !shaft.hasSource() && !shaft.hasNetwork() && shaft.getTheoreticalSpeed() == 0,
                    "旧控制器 Source/Network/Speed 仍留在邻接轴");
            helper.succeed();
        });
    }

    /** 三档均形成薄八棱壳，两端同网共享总容量且实际转速取 SERVER 配置。 */
    @GameTest(template = "turbine_empty", timeoutTicks = 115)
    public static void threeTiersFormAndAllocateOnlyActualProcessedSteam(GameTestHelper helper) {
        BlockPos[] fronts = {new BlockPos(2, 5, 1), new BlockPos(8, 5, 1), new BlockPos(15, 5, 1)};
        TurbineState.Settings settings = TurbineConfig.settings();
        TurbineState.Tier[] tiers = {settings.shortTier(), settings.mediumTier(), settings.longTier()};
        List<BlockPos> feeds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            build(helper, fronts[i], tiers[i].rotorCount(), i == 2);
            feeds.add(inlet(fronts[i], tiers[i]));
        }
        helper.onEachTick(() -> {
            for (BlockPos feed : feeds) {
                IFluidHandler input = handler(helper, feed, Direction.WEST);
                if (input != null) input.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 256),
                        IFluidHandler.FluidAction.EXECUTE);
            }
        });
        helper.runAfterDelay(14, () -> {
            for (int i = 0; i < 3; i++) {
                TurbineControllerBlockEntity owner = owner(helper, fronts[i]);
                require(helper, owner != null && owner.currentForm() != null
                        && owner.currentForm().rotors() == tiers[i].rotorCount()
                        && owner.currentForm().diameter() == tiers[i].diameter(),
                        "三档尺寸或侧控制器未真实成型：" + tiers[i]);
            }
            IFluidHandler second = handler(helper, part(fronts[2], 0, 3, 5), Direction.UP);
            require(helper, second != null && second.fill(new FluidStack(TurbineContent.STEAM.get(), 100),
                    IFluidHandler.FluidAction.EXECUTE) == 0, "普通蒸汽被进汽口接收");
            require(helper, second.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 256),
                    IFluidHandler.FluidAction.EXECUTE) > 0, "第二物理进口未独立接收超临界蒸汽");
        });
        helper.runAfterDelay(55, () -> {
            for (int i = 0; i < 3; i++) {
                TurbineControllerBlockEntity owner = owner(helper, fronts[i]);
                TurbineState.Tier tier = tiers[i];
                double expected = tier.ratedFlowMbPerTick() * settings.suPerMbPerTick();
                require(helper, owner.ledger().processed() == tier.ratedFlowMbPerTick()
                        && owner.ledger().totalSu() == expected,
                        "真实蒸汽流量/SU 不符：" + tier + " 流量=" + owner.ledger().processed()
                                + " SU=" + owner.ledger().totalSu());
                TurbineOutputShaftBlockEntity front = shaft(helper, fronts[i]);
                TurbineOutputShaftBlockEntity rear = shaft(helper,
                        part(fronts[i], 0, 0, tier.length() - 1));
                require(helper, front != null && rear != null && front.hasNetwork() && rear.hasNetwork()
                        && front.getTheoreticalSpeed() == settings.rpm()
                        && rear.getTheoreticalSpeed() == settings.rpm(), "两端轴未以配置RPM进入同一Create网络");
                float frontSu = front.getOrCreateNetwork().calculateCapacity();
                float rearSu = rear.getOrCreateNetwork().calculateCapacity();
                require(helper, front.network.equals(rear.network)
                        && Math.abs(frontSu - expected) < 2 && Math.abs(rearSu - expected) < 2,
                        "两端未共享唯一机组总容量");
            }
            // 成型轴若被扳手改向，机主下一 tick 必须失效，不能向错误面继续发布旧 SU。
            helper.setBlock(fronts[0], helper.getBlockState(fronts[0])
                    .setValue(TurbinePartBlock.MACHINE_FACING, Direction.EAST));
            require(helper, shaft(helper, fronts[0]).getGeneratedSpeed() == 0,
                    "前轴改向的同tick仍向错误面发布旧SU");
        });
        helper.runAfterDelay(58, () -> {
            require(helper, owner(helper, fronts[0]).currentForm() != null
                    && helper.getBlockState(fronts[0]).getValue(TurbinePartBlock.MACHINE_FACING)
                    == Direction.NORTH
                    && shaft(helper, fronts[0]).getGeneratedSpeed() == settings.rpm(),
                    "前轴改向后没有按服务器结构重新派生唯一外向轴面");
            helper.succeed();
        });
    }

    /** 红石与控制器拆放撤销旧SU，携物NBT只保留一份库存。 */
    @GameTest(template = "turbine_empty", timeoutTicks = 100)
    public static void redstoneStopAndPortableControllerKeepSingleLedger(GameTestHelper helper) {
        TurbineState.Tier tier = TurbineConfig.settings().shortTier();
        build(helper, FRONT, tier.rotorCount(), false);
        BlockPos control = controller(FRONT, tier);
        BlockPos input = inlet(FRONT, tier);
        helper.runAfterDelay(12, () -> {
            var owner = owner(helper, FRONT);
            require(helper, owner != null && owner.currentForm() != null, "拆装场景未成型");
            var port = handler(helper, input, Direction.WEST);
            require(helper, port != null && port.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 256),
                    IFluidHandler.FluidAction.EXECUTE) == 256, "真实进汽口未收汽");
        });
        final IFluidHandler[] oldHandle = {null};
        helper.runAfterDelay(18, () -> {
            var owner = owner(helper, FRONT);
            require(helper, owner.ledger().input() + owner.ledger().exhaust() == 256
                    && owner.ledger().totalSu() > 0, "加工不守恒或没有SU");
            oldHandle[0] = handler(helper, input, Direction.WEST);
            helper.setBlock(control.below(), Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(25, () -> {
            var owner = owner(helper, FRONT);
            TurbineOutputShaftBlockEntity front = shaft(helper, FRONT);
            TurbineOutputShaftBlockEntity rear = shaft(helper, part(FRONT, 0, 0, tier.length() - 1));
            require(helper, owner.ledger().totalSu() == 0 && front.getGeneratedSpeed() == 0
                    && rear.getGeneratedSpeed() == 0, "红石未撤销两端轴SU");
            require(helper, owner.ledger().input() + owner.ledger().exhaust() == 256,
                    "红石停机改变库存");
            ItemStack carried = Block.getDrops(helper.getBlockState(control), helper.getLevel(),
                    helper.absolutePos(control), owner).stream()
                    .filter(stack -> stack.is(TurbineContent.CONTROLLER_ITEM.get())).findFirst().orElse(ItemStack.EMPTY);
            require(helper, !carried.isEmpty() && carried.get(DataComponents.CUSTOM_DATA) != null,
                    "控制器掉落没有携带唯一库存NBT");
            helper.setBlock(control, Blocks.AIR);
            require(helper, oldHandle[0] != null && oldHandle[0].fill(
                    new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 1),
                    IFluidHandler.FluidAction.EXECUTE) == 0, "拆除后旧句柄仍能写库存");
            helper.setBlock(control, TurbineContent.CONTROLLER.get().defaultBlockState()
                    .setValue(TurbinePartBlock.MACHINE_FACING, Direction.NORTH)
                    .setValue(TurbinePartBlock.SIDE, TurbinePartBlock.Side.DOWN));
            helper.getBlockState(control).getBlock().setPlacedBy(helper.getLevel(), helper.absolutePos(control),
                    helper.getBlockState(control), null, carried);
        });
        helper.runAfterDelay(39, () -> {
            var owner = owner(helper, FRONT);
            require(helper, owner.currentForm() != null
                    && owner.ledger().input() + owner.ledger().exhaust() == 256,
                    "携物重放没有成型或复制/删除库存");
            require(helper, owner.ledger().totalSu() == 0, "重放保留旧动力历史");
            helper.succeed();
        });
    }

    /** 原生Create无泵管道实际接收本机普通蒸汽。 */
    @GameTest(template = "turbine_empty", timeoutTicks = 100)
    public static void ordinarySteamMovesThroughNativeCreatePipe(GameTestHelper helper) {
        TurbineState.Tier tier = TurbineConfig.settings().mediumTier();
        build(helper, FRONT, tier.rotorCount(), false);
        BlockPos exhaust = part(FRONT, (tier.diameter() - 1) / 2, 0, 1);
        BlockPos[] pipes = {exhaust.east(), exhaust.east(2), exhaust.east(3)};
        BlockPos tankPos = exhaust.east(4);
        helper.setBlock(tankPos, AllBlocks.FLUID_TANK.get());
        for (BlockPos pipe : pipes) helper.setBlock(pipe, AllBlocks.FLUID_PIPE.get());
        sealOpenEnds(helper, pipes);
        helper.runAfterDelay(15, () -> {
            CompoundTag stored = new CompoundTag(); stored.putInt("Exhaust", 1000);
            owner(helper, FRONT).ledger().load(stored);
            require(helper, owner(helper, FRONT).currentForm() != null, "管网场景未成型");
        });
        helper.runAfterDelay(65, () -> {
            IFluidHandler tank = handler(helper, tankPos, Direction.WEST);
            int moved = tank == null ? 0 : tank.getFluidInTank(0).getAmount();
            require(helper, moved > 0 && TurbineContent.isOrdinarySteam(tank.getFluidInTank(0))
                    && moved + owner(helper, FRONT).ledger().exhaust() == 1000,
                    "原生管网未保量传递普通蒸汽");
            helper.succeed();
        });
    }

    /** 构造器严格按当前配置直径/长度，底侧控制器、侧进汽和两端轴各占一格。 */
    static void build(GameTestHelper helper, BlockPos front, int rotors, boolean extraPorts) {
        TurbineState.Tier tier = TurbineConfig.settings().tierForRotors(rotors);
        if (tier == null) throw new IllegalArgumentException("未知汽轮机配置档位：" + rotors);
        int length = tier.length(), radius = (tier.diameter() - 1) / 2;
        int center = length % 2 == 0 ? length / 2 - 1 : length / 2;
        for (int z = 0; z < length; z++) for (int y = -radius; y <= radius; y++)
            for (int x = -radius; x <= radius; x++) {
                if (!TurbineGeometry.footprint(tier.diameter(), x, y)) continue;
                BlockPos pos = part(front, x, y, z);
                BlockState state;
                if (z == 0 && x == 0 && y == 0 || z == length - 1 && x == 0 && y == 0)
                    state = TurbineContent.OUTPUT_SHAFT.get().defaultBlockState()
                            .setValue(TurbineShaftBlock.END, z == 0
                                    ? TurbineShaftBlock.End.FRONT : TurbineShaftBlock.End.REAR);
                else if (z > 0 && z < length - 1 && x == 0 && y == 0)
                    state = TurbineContent.ROTOR.get().defaultBlockState();
                else if (z > 0 && z < length - 1 && TurbineGeometry.airSlot(tier.diameter(), x, y))
                    continue;
                else if (z == center && x == 0 && y == -radius)
                    state = TurbineContent.CONTROLLER.get().defaultBlockState()
                            .setValue(TurbinePartBlock.SIDE, TurbinePartBlock.Side.DOWN);
                else if (z == center && x == -radius && y == 0
                        || extraPorts && z == center && x == 0 && y == radius)
                    state = TurbineContent.INLET.get().defaultBlockState().setValue(TurbinePartBlock.OUTWARD,
                            y > 0 ? Direction.UP : Direction.WEST);
                else if (z == 1 && x == radius && y == 0
                        || extraPorts && z == length - 2 && x == -radius && y == 0)
                    state = TurbineContent.EXHAUST.get().defaultBlockState().setValue(TurbinePartBlock.OUTWARD,
                            x < 0 ? Direction.WEST : Direction.EAST);
                else state = TurbineContent.CASING.get().defaultBlockState();
                helper.setBlock(pos, state);
            }
    }

    static BlockPos part(BlockPos front, int x, int y, int z) { return front.offset(x, y, z); }
    static BlockPos controller(BlockPos front, TurbineState.Tier tier) {
        int center = tier.length() % 2 == 0 ? tier.length() / 2 - 1 : tier.length() / 2;
        return part(front, 0, -(tier.diameter() - 1) / 2, center);
    }
    static BlockPos inlet(BlockPos front, TurbineState.Tier tier) {
        return part(front, -(tier.diameter() - 1) / 2, 0,
                tier.length() % 2 == 0 ? tier.length() / 2 - 1 : tier.length() / 2);
    }
    static TurbineControllerBlockEntity owner(GameTestHelper helper, BlockPos front) {
        for (TurbineState.Tier tier : new TurbineState.Tier[]{TurbineConfig.settings().shortTier(),
                TurbineConfig.settings().mediumTier(), TurbineConfig.settings().longTier()}) {
            BlockPos pos = controller(front, tier);
            if (helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof TurbineControllerBlockEntity owner)
                return owner;
        }
        return null;
    }
    static TurbineOutputShaftBlockEntity shaft(GameTestHelper helper, BlockPos pos) {
        return (TurbineOutputShaftBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }
    static IFluidHandler handler(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), side);
    }
    private static void sealOpenEnds(GameTestHelper helper, BlockPos[] pipes) {
        for (BlockPos pipe : pipes) FluidPropagator.propagateChangedPipe(helper.getLevel(),
                helper.absolutePos(pipe), helper.getBlockState(pipe));
        for (BlockPos pipe : pipes) {
            FluidTransportBehaviour behaviour = com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.get(
                    helper.getLevel(), helper.absolutePos(pipe), FluidTransportBehaviour.TYPE);
            require(helper, behaviour != null, "Create管道缺少FluidTransportBehaviour");
            for (Direction side : Direction.values())
                if (behaviour.getConnection(side) != null && helper.getBlockState(pipe.relative(side)).isAir()
                        && FluidPropagator.isOpenEnd(helper.getLevel(), helper.absolutePos(pipe), side))
                    helper.setBlock(pipe.relative(side), Blocks.IRON_BLOCK);
        }
        for (BlockPos pipe : pipes) FluidPropagator.propagateChangedPipe(helper.getLevel(),
                helper.absolutePos(pipe), helper.getBlockState(pipe));
    }
    static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
