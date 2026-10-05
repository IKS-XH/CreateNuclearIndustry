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
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
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

/** 三档侧控汽轮机的真实结构、双轴、周转流量和原生管网场景。 */
@GameTestHolder("create_nuclear_industry_turbine")
@PrefixGameTestTemplate(false)
public final class ExtensionTurbineGameTests {
    private static final BlockPos FRONT = new BlockPos(5, 5, 1);
    private ExtensionTurbineGameTests() {}

    /** 持续真实排汽后，前后端连接的 Create 风扇读取同一份总容量。 */
    @GameTest(template = "turbine_empty", timeoutTicks = 115)
    public static void sharedCapacityDrivesEitherEndAtRealExhaustFlow(GameTestHelper helper) {
        TurbineState.Settings settings = TurbineConfig.settings();
        TurbineState.Tier tier = settings.shortTier();
        build(helper, FRONT, tier.rotorCount(), false);
        BlockPos rearPos = part(FRONT, 0, 0, tier.length() - 1);
        BlockPos frontFanPos = FRONT.north();
        BlockPos rearFanPos = rearPos.south();
        helper.setBlock(frontFanPos, AllBlocks.ENCASED_FAN.getDefaultState()
                .setValue(EncasedFanBlock.FACING, Direction.NORTH));
        helper.onEachTick(() -> {
            IFluidHandler input = handler(helper, inlet(FRONT, tier), Direction.WEST);
            if (input != null) input.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(),
                    tier.ratedFlowMbPerTick()), IFluidHandler.FluidAction.EXECUTE);
            IFluidHandler output = handler(helper, exhaust(FRONT, tier), Direction.EAST);
            if (output != null) output.drain(tier.ratedFlowMbPerTick(), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(75, () -> {
            TurbineOutputShaftBlockEntity front = shaft(helper, FRONT);
            TurbineOutputShaftBlockEntity rear = shaft(helper, rearPos);
            KineticBlockEntity fan = (KineticBlockEntity) helper.getLevel()
                    .getBlockEntity(helper.absolutePos(frontFanPos));
            float capacity = front.getOrCreateNetwork().calculateCapacity();
            require(helper, front.network.equals(rear.network)
                    && Math.abs(capacity - owner(helper, FRONT).ledger().totalSu()) < 2
                    && !front.isOverStressed() && !rear.isOverStressed()
                    && Math.abs(fan.getSpeed()) == settings.rpm(),
                    "前端容量/流量不符：网络=" + capacity + " 账本=" + owner(helper, FRONT).ledger().totalSu()
                            + " 平均=" + owner(helper, FRONT).ledger().averageFlowMbPerTick());
            helper.setBlock(frontFanPos, Blocks.AIR);
            helper.setBlock(rearFanPos, AllBlocks.ENCASED_FAN.getDefaultState()
                    .setValue(EncasedFanBlock.FACING, Direction.SOUTH));
        });
        helper.runAfterDelay(85, () -> {
            TurbineOutputShaftBlockEntity front = shaft(helper, FRONT);
            TurbineOutputShaftBlockEntity rear = shaft(helper, rearPos);
            KineticBlockEntity fan = (KineticBlockEntity) helper.getLevel()
                    .getBlockEntity(helper.absolutePos(rearFanPos));
            float capacity = rear.getOrCreateNetwork().calculateCapacity();
            require(helper, front.network.equals(rear.network)
                    && Math.abs(capacity - owner(helper, FRONT).ledger().totalSu()) < 2
                    && !front.isOverStressed() && !rear.isOverStressed()
                    && Math.abs(fan.getSpeed()) == settings.rpm(),
                    "后端容量/流量不符：网络=" + capacity + " 账本=" + owner(helper, FRONT).ledger().totalSu());
            helper.succeed();
        });
    }

    /** 无外源时停止真实供汽，等待残留与40tick窗口衰减后应撤销整条双端动力网。 */
    @GameTest(template = "turbine_empty", timeoutTicks = 330)
    public static void zeroFlowStopsBothEndsAndExternalShafts(GameTestHelper helper) {
        TurbineState.Settings settings = TurbineConfig.settings();
        TurbineState.Tier tier = settings.shortTier();
        build(helper, FRONT, tier.rotorCount(), false);
        BlockPos rearPos = part(FRONT, 0, 0, tier.length() - 1);
        BlockPos frontExternal = FRONT.north();
        BlockPos rearExternal = rearPos.south();
        helper.setBlock(frontExternal, AllBlocks.SHAFT.getDefaultState()
                .setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(rearExternal, AllBlocks.SHAFT.getDefaultState()
                .setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        final boolean[] feeding = {true};
        helper.onEachTick(() -> {
            if (!feeding[0]) return;
            IFluidHandler input = handler(helper, inlet(FRONT, tier), Direction.WEST);
            if (input != null) input.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(),
                    tier.ratedFlowMbPerTick()), IFluidHandler.FluidAction.EXECUTE);
            IFluidHandler output = handler(helper, exhaust(FRONT, tier), Direction.EAST);
            if (output != null) output.drain(tier.ratedFlowMbPerTick(), IFluidHandler.FluidAction.EXECUTE);
        });
        TurbineOutputShaftBlockEntity[] frontRef = {null};
        helper.onEachTick(() -> {
            if (feeding[0]) return;
            TurbineControllerBlockEntity owner = owner(helper, FRONT);
            if (owner != null && owner.ledger().totalSu() == 0) {
                if (frontRef[0] == null) frontRef[0] = shaft(helper, FRONT);
                forceCreateKineticValidation(frontRef[0]);
            }
        });
        // 真实流量窗口归零后，测试夹具固定父类校验相位以复现 0->0 生命周期竞态。
        helper.runAfterDelay(93, () -> {
            TurbineControllerBlockEntity owner = owner(helper, FRONT);
            TurbineOutputShaftBlockEntity front = shaft(helper, FRONT);
            TurbineOutputShaftBlockEntity rear = shaft(helper, rearPos);
            KineticBlockEntity frontExternalShaft = (KineticBlockEntity) helper.getLevel()
                    .getBlockEntity(helper.absolutePos(frontExternal));
            KineticBlockEntity rearExternalShaft = (KineticBlockEntity) helper.getLevel()
                    .getBlockEntity(helper.absolutePos(rearExternal));
            require(helper, owner.ledger().totalSu() > 0 && front.network.equals(rear.network)
                    && Math.abs(front.getTheoreticalSpeed()) == settings.rpm()
                    && Math.abs(rear.getTheoreticalSpeed()) == settings.rpm()
                    && Math.abs(frontExternalShaft.getTheoreticalSpeed()) == settings.rpm()
                    && Math.abs(rearExternalShaft.getTheoreticalSpeed()) == settings.rpm(),
                    "断汽回归场景在供汽期未建立双端真实动力网");
            feeding[0] = false;
        });
        helper.runAfterDelay(240, () -> {
            TurbineControllerBlockEntity owner = owner(helper, FRONT);
            TurbineOutputShaftBlockEntity front = shaft(helper, FRONT);
            TurbineOutputShaftBlockEntity rear = shaft(helper, part(FRONT, 0, 0, tier.length() - 1));
            KineticBlockEntity frontExternalShaft = (KineticBlockEntity) helper.getLevel()
                    .getBlockEntity(helper.absolutePos(FRONT.north()));
            KineticBlockEntity rearExternalShaft = (KineticBlockEntity) helper.getLevel()
                    .getBlockEntity(helper.absolutePos(part(FRONT, 0, 0, tier.length() - 1).south()));
            require(helper, owner.ledger().totalSu() == 0 && owner.ledger().averageFlowMbPerTick() == 0,
                    "停止输入后流量窗口未衰减归零：SU=" + owner.ledger().totalSu()
                            + " 平均=" + owner.ledger().averageFlowMbPerTick());
            require(helper, !front.hasNetwork(), "Create校验先把生成轴speed清零后，前轴未执行源网络拆除：network="
                    + front.network + " source=" + front.source
                    + " 本机SU=" + owner.ledger().totalSu() + " 实际RPM=" + front.getTheoreticalSpeed());
            require(helper, front.getGeneratedSpeed() == 0 && front.getTheoreticalSpeed() == 0
                    && rear.getTheoreticalSpeed() == 0
                    && frontExternalShaft.getTheoreticalSpeed() == 0
                    && rearExternalShaft.getTheoreticalSpeed() == 0,
                    "无外源时双轴或外接轴仍残留转速：本机源=" + front.getGeneratedSpeed()
                            + " 前轴=" + front.getTheoreticalSpeed() + " 后轴=" + rear.getTheoreticalSpeed()
                            + " 外接前=" + frontExternalShaft.getTheoreticalSpeed()
                            + " 外接后=" + rearExternalShaft.getTheoreticalSpeed());
            feeding[0] = true;
        });
        helper.runAfterDelay(295, () -> {
            TurbineControllerBlockEntity owner = owner(helper, FRONT);
            TurbineOutputShaftBlockEntity front = shaft(helper, FRONT);
            TurbineOutputShaftBlockEntity rear = shaft(helper, part(FRONT, 0, 0, tier.length() - 1));
            require(helper, owner.ledger().totalSu() > 0 && front.network != null
                    && front.network.equals(rear.network)
                    && Math.abs(front.getTheoreticalSpeed()) == settings.rpm()
                    && Math.abs(rear.getTheoreticalSpeed()) == settings.rpm()
                    && Math.abs(front.getOrCreateNetwork().calculateCapacity() - owner.ledger().totalSu()) < 2,
                    "停止后恢复供汽未重新建立唯一本机容量：SU=" + owner.ledger().totalSu()
                            + " 前轴=" + front.getTheoreticalSpeed() + " 后轴=" + rear.getTheoreticalSpeed());
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
        final boolean[] feeding = {true};
        helper.onEachTick(() -> {
            if (!feeding[0]) return;
            IFluidHandler input = handler(helper, inlet(FRONT, tier), Direction.WEST);
            if (input != null) input.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 40),
                    IFluidHandler.FluidAction.EXECUTE);
            IFluidHandler output = handler(helper, exhaust(FRONT, tier), Direction.EAST);
            if (output != null) output.drain(40, IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(28, () -> {
            feeding[0] = false;
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
        helper.runAfterDelay(29, () -> {
            CreativeMotorBlockEntity liveMotor = (CreativeMotorBlockEntity) helper.getLevel()
                    .getBlockEntity(helper.absolutePos(motorPos));
            float external = liveMotor.calculateAddedStressCapacity() * Math.abs(liveMotor.getGeneratedSpeed());
            float actual = liveMotor.getOrCreateNetwork().calculateCapacity();
            require(helper, actual <= external + 2,
                    "当前格式恢复首tick重复容量：实际=" + actual + " 外源=" + external);
        });
        helper.runAfterDelay(35, () -> {
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

    /** GameTest专用：将Create前轴下一次真实validateKinetics对齐到SU归零后。 */
    private static void forceCreateKineticValidation(KineticBlockEntity kinetic) {
        try {
            var field = KineticBlockEntity.class.getDeclaredField("validationCountdown");
            field.setAccessible(true);
            field.setInt(kinetic, 0);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("无法在GameTest中对齐Create动力校验相位", exception);
        }
    }

    /** 三档均形成薄八棱壳，两端同网共享总容量且实际转速取 SERVER 配置。 */
    @GameTest(template = "turbine_empty", timeoutTicks = 115)
    public static void threeTiersFormAndAllocateOnlyActualProcessedSteam(GameTestHelper helper) {
        BlockPos[] fronts = {new BlockPos(2, 5, 1), new BlockPos(8, 5, 1), new BlockPos(15, 5, 1)};
        TurbineState.Settings settings = TurbineConfig.settings();
        TurbineState.Tier[] tiers = {settings.shortTier(), settings.mediumTier(), settings.longTier()};
        List<BlockPos> feeds = new ArrayList<>();
        List<BlockPos> outlets = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            build(helper, fronts[i], tiers[i].rotorCount(), i == 2);
            feeds.add(inlet(fronts[i], tiers[i]));
            outlets.add(exhaust(fronts[i], tiers[i]));
        }
        helper.onEachTick(() -> {
            for (int i = 0; i < feeds.size(); i++) {
                IFluidHandler input = handler(helper, feeds.get(i), Direction.WEST);
                if (input != null) input.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(),
                                tiers[i].ratedFlowMbPerTick()),
                        IFluidHandler.FluidAction.EXECUTE);
                IFluidHandler output = handler(helper, outlets.get(i), Direction.EAST);
                if (output != null) output.drain(tiers[i].ratedFlowMbPerTick(), IFluidHandler.FluidAction.EXECUTE);
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
            int secondAccepted = second.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 256),
                    IFluidHandler.FluidAction.EXECUTE);
            require(helper, secondAccepted <= tiers[2].ratedFlowMbPerTick()
                    && owner(helper, fronts[2]).ledger().remainingInput(
                            helper.absolutePos(part(fronts[2], 0, 3, 5)).asLong(),
                            helper.getLevel().getGameTime()) == 0,
                    "第二物理进口绕过整机每 tick 额定值");
        });
        helper.runAfterDelay(75, () -> {
            for (int i = 0; i < 3; i++) {
                TurbineControllerBlockEntity owner = owner(helper, fronts[i]);
                TurbineState.Tier tier = tiers[i];
                double expected = tier.ratedFlowMbPerTick() * settings.suPerMbPerTick()
                        * tier.maxEfficiencyMultiplier();
                require(helper, owner.ledger().averageFlowMbPerTick() == tier.ratedFlowMbPerTick()
                        && Math.abs(owner.ledger().totalSu() - expected) < 2,
                        "真实蒸汽流量/SU 不符：" + tier + " 流量=" + owner.ledger().averageFlowMbPerTick()
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
        helper.runAfterDelay(95, () -> {
            require(helper, owner(helper, fronts[0]).currentForm() != null
                    && helper.getBlockState(fronts[0]).getValue(TurbinePartBlock.MACHINE_FACING)
                    == Direction.NORTH
                    && shaft(helper, fronts[0]).getGeneratedSpeed() == settings.rpm(),
                    "前轴改向后没有按服务器结构重新派生唯一外向轴面");
            helper.succeed();
        });
    }

    /** 红石与控制器拆放撤销本机 SU，携物 NBT 只保留一份周转残留。 */
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
            require(helper, port != null && port.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 54),
                    IFluidHandler.FluidAction.EXECUTE) == 54, "真实进汽口未收汽");
        });
        final IFluidHandler[] oldHandle = {null};
        helper.runAfterDelay(18, () -> {
            var owner = owner(helper, FRONT);
            require(helper, owner.ledger().exhaust() == 54
                    && owner.ledger().totalSu() == 0, "未排汽时周转残留或 SU 不符");
            oldHandle[0] = handler(helper, input, Direction.WEST);
            helper.setBlock(control.below(), Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(25, () -> {
            var owner = owner(helper, FRONT);
            TurbineOutputShaftBlockEntity front = shaft(helper, FRONT);
            TurbineOutputShaftBlockEntity rear = shaft(helper, part(FRONT, 0, 0, tier.length() - 1));
            require(helper, owner.ledger().totalSu() == 0 && front.getGeneratedSpeed() == 0
                    && rear.getGeneratedSpeed() == 0, "红石未撤销两端轴SU");
            require(helper, owner.ledger().exhaust() == 54, "红石停机改变周转残留");
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
                    && owner.ledger().exhaust() == 54,
                    "携物重放没有成型或复制/删除周转残留");
            require(helper, owner.ledger().totalSu() == 0, "重放保留旧动力历史");
            helper.succeed();
        });
    }

    /** 两张独立 Create 泵/管网同时运行，按源罐扣量与目标罐实收核对守恒及额定吞吐。 */
    @GameTest(template = "turbine_empty", timeoutTicks = 100)
    public static void ordinarySteamMovesThroughNativeCreatePipe(GameTestHelper helper) {
        TurbineState.Tier tier = TurbineConfig.settings().mediumTier();
        BlockPos front = new BlockPos(10, 5, 1);
        build(helper, front, tier.rotorCount(), false);
        BlockPos inlet = inlet(front, tier);
        BlockPos sourceTank = inlet.west(2).south(2);
        BlockPos sourcePipe = inlet.west(2).south();
        BlockPos pump = inlet.west(2);
        BlockPos targetPipeA = pump.north();
        BlockPos targetPipeB = targetPipeA.east();
        BlockPos inletPipe = inlet.west();
        BlockPos cog = pump.west();
        BlockPos shaft = cog.south();
        BlockPos motor = shaft.south();
        helper.setBlock(sourceTank, AllBlocks.FLUID_TANK.get());
        IFluidHandler source = handler(helper, sourceTank, Direction.EAST);
        require(helper, source != null && source.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 8000),
                IFluidHandler.FluidAction.EXECUTE) == 8000, "Create 源罐装汽失败");
        helper.setBlock(sourcePipe, AllBlocks.FLUID_PIPE.get());
        helper.setBlock(targetPipeA, AllBlocks.FLUID_PIPE.get());
        helper.setBlock(targetPipeB, AllBlocks.FLUID_PIPE.get());
        helper.setBlock(inletPipe, AllBlocks.FLUID_PIPE.get());
        helper.setBlock(pump, AllBlocks.MECHANICAL_PUMP.getDefaultState()
                .setValue(PumpBlock.FACING, Direction.NORTH));
        helper.setBlock(cog, AllBlocks.COGWHEEL.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(shaft, AllBlocks.SHAFT.getDefaultState()
                .setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.NORTH));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(motor)).generatedSpeed.setValue(256);
        sealOpenEnds(helper, new BlockPos[]{sourcePipe, targetPipeA, targetPipeB, inletPipe});
        BlockPos exhaust = exhaust(front, tier);
        BlockPos[] pipes = {exhaust.east(), exhaust.east(2), exhaust.east(3)};
        BlockPos tankPos = exhaust.east(4);
        helper.setBlock(tankPos, AllBlocks.FLUID_TANK.get());
        for (BlockPos pipe : pipes) helper.setBlock(pipe, AllBlocks.FLUID_PIPE.get());
        sealOpenEnds(helper, pipes);
        helper.runAfterDelay(15, () -> {
            require(helper, owner(helper, front).currentForm() != null
                    && ((PumpBlockEntity) helper.getBlockEntity(pump)).getSpeed() != 0,
                    "双管网场景未成型或入口机械泵未转动：成型="
                            + (owner(helper, front).currentForm() != null)
                            + " 泵速=" + ((PumpBlockEntity) helper.getBlockEntity(pump)).getSpeed()
                            + " 马达速=" + ((CreativeMotorBlockEntity) helper.getBlockEntity(motor)).getSpeed());
        });
        final int[] baseline = {0};
        helper.runAfterDelay(45, () -> {
            IFluidHandler tank = handler(helper, tankPos, Direction.WEST);
            baseline[0] = tank == null ? 0 : tank.getFluidInTank(0).getAmount();
        });
        helper.runAfterDelay(65, () -> {
            IFluidHandler tank = handler(helper, tankPos, Direction.WEST);
            int moved = tank == null ? 0 : tank.getFluidInTank(0).getAmount();
            IFluidHandler sourceEnd = handler(helper, sourceTank, Direction.EAST);
            int remaining = sourceEnd == null ? 0 : sourceEnd.getFluidInTank(0).getAmount();
            require(helper, moved > 0 && TurbineContent.isOrdinarySteam(tank.getFluidInTank(0))
                    && moved + owner(helper, front).ledger().exhaust() + remaining == 8000
                    && owner(helper, front).ledger().exhaust() <= tier.ratedFlowMbPerTick(),
                    "双 Create 管网未守恒：源余=" + remaining + " 目标=" + moved
                            + " 周转=" + owner(helper, front).ledger().exhaust());
            require(helper, moved - baseline[0] == tier.ratedFlowMbPerTick() * 20,
                    "原生 Create 管路稳定段未达到额定流量：20tick 实收="
                            + (moved - baseline[0]) + " 预期=" + tier.ratedFlowMbPerTick() * 20);
            helper.succeed();
        });
    }

    /** 堵塞只占一 tick 周转空间；开放原生管路后低供汽仍守恒但无 SU，满供汽恢复额定与共享动力。 */
    @GameTest(template = "turbine_empty", timeoutTicks = 155)
    public static void blockedPipeThenLowAndFullSupplyConservesSteam(GameTestHelper helper) {
        TurbineState.Tier tier = TurbineConfig.settings().mediumTier();
        build(helper, FRONT, tier.rotorCount(), false);
        BlockPos outlet = exhaust(FRONT, tier);
        BlockPos[] pipes = {outlet.east(), outlet.east(2), outlet.east(3)};
        BlockPos tankPos = outlet.east(4);
        helper.setBlock(tankPos, Blocks.IRON_BLOCK);
        for (BlockPos pipe : pipes) helper.setBlock(pipe, AllBlocks.FLUID_PIPE.get());
        sealOpenEnds(helper, pipes);
        final int[] supply = {tier.ratedFlowMbPerTick()}, accepted = {0};
        helper.onEachTick(() -> {
            IFluidHandler input = handler(helper, inlet(FRONT, tier), Direction.WEST);
            if (input != null) accepted[0] += input.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(),
                    supply[0]), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(25, () -> {
            TurbineControllerBlockEntity owner = owner(helper, FRONT);
            require(helper, accepted[0] == tier.ratedFlowMbPerTick()
                    && owner.ledger().exhaust() == tier.ratedFlowMbPerTick()
                    && owner.ledger().totalSu() == 0, "堵塞时未将真实入汽封顶保留在周转缓存");
            supply[0] = 10;
            helper.setBlock(tankPos, AllBlocks.FLUID_TANK.get());
            for (BlockPos pipe : pipes) FluidPropagator.propagateChangedPipe(helper.getLevel(),
                    helper.absolutePos(pipe), helper.getBlockState(pipe));
        });
        helper.runAfterDelay(75, () -> {
            TurbineControllerBlockEntity owner = owner(helper, FRONT);
            IFluidHandler tank = handler(helper, tankPos, Direction.WEST);
            int moved = tank == null ? 0 : tank.getFluidInTank(0).getAmount();
            require(helper, moved > 0 && moved + owner.ledger().exhaust() == accepted[0]
                    && owner.ledger().averageFlowMbPerTick() < owner.ledger().minimumFlowMbPerTick()
                    && owner.ledger().totalSu() == 0,
                    "低供汽实际排出/守恒/零动力不符：入=" + accepted[0] + " 出=" + moved
                            + " 缓存=" + owner.ledger().exhaust());
            supply[0] = tier.ratedFlowMbPerTick();
        });
        helper.runAfterDelay(130, () -> {
            TurbineControllerBlockEntity owner = owner(helper, FRONT);
            IFluidHandler tank = handler(helper, tankPos, Direction.WEST);
            int moved = tank == null ? 0 : tank.getFluidInTank(0).getAmount();
            List<Component> tooltip = new ArrayList<>();
            owner.addToGoggleTooltip(tooltip, false);
            boolean running = tooltip.size() > 1 && tooltip.get(1).getContents() instanceof TranslatableContents text
                    && text.getKey().endsWith(".running");
            require(helper, moved + owner.ledger().exhaust() == accepted[0]
                    && owner.ledger().averageFlowMbPerTick() == tier.ratedFlowMbPerTick()
                    && running && owner.ledger().totalSu() > 0,
                    "恢复后管路未持续额定排汽或护目镜误报堵塞：入=" + accepted[0]
                            + " 出=" + moved + " 平均=" + owner.ledger().averageFlowMbPerTick()
                            + " 状态=" + (tooltip.size() > 1 ? tooltip.get(1).getContents() : "无"));
            TurbineOutputShaftBlockEntity front = shaft(helper, FRONT);
            TurbineOutputShaftBlockEntity rear = shaft(helper, part(FRONT, 0, 0, tier.length() - 1));
            require(helper, front.network.equals(rear.network)
                    && Math.abs(front.getOrCreateNetwork().calculateCapacity() - owner.ledger().totalSu()) < 2,
                    "恢复后两端未共享唯一真实排汽容量");
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
    static BlockPos exhaust(BlockPos front, TurbineState.Tier tier) {
        return part(front, (tier.diameter() - 1) / 2, 0, 1);
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
