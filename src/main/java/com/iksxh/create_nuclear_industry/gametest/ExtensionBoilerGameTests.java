package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity;
import com.iksxh.create_nuclear_industry.boiler.BoilerPartBlock;
import com.iksxh.create_nuclear_industry.boiler.BoilerPressureConnection;
import com.iksxh.create_nuclear_industry.boiler.BoilerState;
import com.iksxh.create_nuclear_industry.boiler.BoilerStructure;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.BasicMaterialContent;
import com.iksxh.create_nuclear_industry.content.HeatMaterialsContent;
import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlockEntity;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 固定结构、真实能力和付费核热的独立服务端场景。 */
@GameTestHolder("create_nuclear_industry_boiler")
@PrefixGameTestTemplate(false)
public final class ExtensionBoilerGameTests {
    private static final BlockPos CENTER = new BlockPos(10, 2, 3);
    private static final BlockPos CONTROL = CENTER.offset(0, 1, -2);
    private static final BlockPos WATER_EAST = CENTER.offset(2, 1, 0);
    private static final BlockPos WATER_SOUTH = CENTER.offset(0, 1, 2);
    private static final BlockPos STEAM_WEST = CENTER.offset(-2, 3, 0);
    private static final BlockPos SECTION = CENTER.offset(1, 0, 0);
    private static final BlockPos SOURCE = SECTION.below();
    private static final BlockPos WATER_TANK = new BlockPos(17, 3, 3);
    private static final BlockPos WATER_PUMP = new BlockPos(15, 3, 3);
    private static final BlockPos STEAM_TANK = new BlockPos(10, 5, 9);
    private static final BlockPos STEAM_PUMP = new BlockPos(10, 5, 7);
    private ExtensionBoilerGameTests() {}

    @GameTest(template = "boiler_empty", timeoutTicks = 80)
    public static void fixedShellAndPerPortWaterQuotaInvalidateOnBreak(GameTestHelper helper) {
        build(helper, true);
        helper.runAfterDelay(3, () -> {
            require(helper, BoilerStructure.inspect(helper.getLevel(), helper.absolutePos(CONTROL)) != null,
                    "5×5×5结构未成型");
            for (BlockPos part : new BlockPos[]{CENTER, CONTROL, WATER_EAST, WATER_SOUTH, STEAM_WEST,
                    SECTION, CENTER.above(4)})
                require(helper, !BlockMovementChecks.isMovementAllowed(helper.getBlockState(part),
                        helper.getLevel(), helper.absolutePos(part)), "锅炉部件可被Create搬运：" + part);
            var east = handler(helper, WATER_EAST, Direction.EAST);
            var south = handler(helper, WATER_SOUTH, Direction.SOUTH);
            require(helper, east != null && south != null, "两个真实水口能力缺失");
            require(helper, east.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.SIMULATE) == 256
                    && south.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.SIMULATE) == 256,
                    "模拟查询错误修改额度");
            int first = east.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
            int second = south.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
            require(helper, first == 256 && second == 256 && owner(helper).ledger().water() == 512,
                    "两个真实给水口没有各自完成256mB/t入水");
            require(helper, east.fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "同一个水口可重复绕过单口256mB/t额度");
            helper.setBlock(CENTER.above(4).offset(1, 0, 0), Blocks.AIR);
            var issue = BoilerStructure.issue(helper.getLevel(), helper.absolutePos(CONTROL));
            require(helper, issue.reason().equals("top_shell")
                    && issue.pos().equals(helper.absolutePos(CENTER.above(4).east())),
                    "扳手诊断没有指向损坏的顶面格");
            require(helper, east.fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "拆壳后缓存能力仍吞水");
            helper.succeed();
        });
    }

    /** 棱边窗口必须拒绝；旧3×3×4停机保存账本，原控制器扩建后恢复。 */
    @GameTest(template = "boiler_empty", timeoutTicks = 90)
    public static void edgeWindowAndLegacyShellRebuildKeepInventory(GameTestHelper helper) {
        buildLegacy(helper);
        var saved = new net.minecraft.nbt.CompoundTag();
        saved.putInt("Water", 400);
        saved.putInt("Steam", 123);
        saved.putDouble("WarmHu", 3600);
        saved.putBoolean("Ready", true);
        var portable = new net.minecraft.nbt.CompoundTag();
        portable.put("Ledger", saved);
        portable.putInt("Sections", 1);
        owner(helper).loadPortableData(portable);
        helper.runAfterDelay(4, () -> {
            require(helper, BoilerStructure.inspect(helper.getLevel(), helper.absolutePos(CONTROL)) == null,
                    "旧3×3×4意外继续成型");
            require(helper, owner(helper).ledger().water() == 400 && owner(helper).ledger().steam() == 123,
                    "旧结构停机删除了库存");
            build(helper, false);
            helper.setBlock(CENTER.offset(2, 2, 2), BoilerContent.WINDOW.get());
            var issue = BoilerStructure.issue(helper.getLevel(), helper.absolutePos(CONTROL));
            require(helper, BoilerStructure.inspect(helper.getLevel(), helper.absolutePos(CONTROL)) == null
                    && issue.reason().equals("edge"), "棱边窗口没有被拒绝");
            helper.setBlock(CENTER.offset(2, 2, 2), BoilerContent.CASING.get());
        });
        helper.runAfterDelay(7, () -> {
            require(helper, BoilerStructure.inspect(helper.getLevel(), helper.absolutePos(CONTROL)) != null,
                    "旧控制器扩建后未恢复成型");
            int water = owner(helper).ledger().water();
            int steam = owner(helper).ledger().steam();
            require(helper, water + steam == 523 && water <= 400 && steam >= 123
                    && owner(helper).ledger().warmHu() <= 3600,
                    "旧库存或已付暖炉HU在重搭后丢失/复制：水=" + water + " 汽=" + steam);
            helper.succeed();
        });
    }

    /** 三排只从尾端供热、首端返冷，中央机依靠01D共享直列获得实付热。 */
    @GameTest(template = "boiler_empty", timeoutTicks = 270)
    public static void nineSectionsWarmFromThreeSharedRows(GameTestHelper helper) {
        build(helper, false);
        for (int z = -1; z <= 1; z++) for (int x = -1; x <= 1; x++) {
            BlockPos section = CENTER.offset(x, 0, z);
            helper.setBlock(section, BoilerContent.HEAT_SECTION.get());
            helper.setBlock(section.below(), HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState()
                    .setValue(NuclearHeatExchangerBlock.FACING, Direction.WEST));
        }
        helper.onEachTick(() -> {
            for (int z = -1; z <= 1; z++) {
                BlockPos tail = CENTER.offset(1, -1, z);
                BlockPos head = CENTER.offset(-1, -1, z);
                var hot = ((NuclearHeatExchangerBlockEntity) helper.getBlockEntity(tail)).fluidPort(Direction.EAST);
                var cold = ((NuclearHeatExchangerBlockEntity) helper.getBlockEntity(head)).fluidPort(Direction.WEST);
                if (hot != null) hot.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 108),
                        IFluidHandler.FluidAction.EXECUTE);
                if (cold != null) cold.drain(4000, IFluidHandler.FluidAction.EXECUTE);
            }
        });
        helper.runAfterDelay(4, () -> {
            var water = handler(helper, WATER_EAST, Direction.EAST);
            require(helper, water != null && water.fill(new FluidStack(Fluids.WATER, 256),
                    IFluidHandler.FluidAction.EXECUTE) == 256, "九段结构无法给水");
        });
        helper.runAfterDelay(215, () -> {
            var form = BoilerStructure.inspect(helper.getLevel(), helper.absolutePos(CONTROL));
            require(helper, form != null && form.sections().size() == 9, "中央热段或九段结构未成型");
            require(helper, owner(helper).ledger().ready() && owner(helper).ledger().steam() > 0,
                    "三排尾入首出未使九段在额定时间内暖炉产汽");
            require(helper, ((NuclearHeatExchangerBlockEntity) helper.getBlockEntity(CENTER.below())).ledger()
                    .reserve() > 0, "中央换热器没有得到共享直列的实付热");
            helper.succeed();
        });
    }

    @GameTest(template = "boiler_empty", timeoutTicks = 280)
    public static void realExchangerPaysWarmingThenSteam(GameTestHelper helper) {
        build(helper, false);
        helper.onEachTick(() -> {
            var machine = source(helper);
            if (!machine.current() || !machine.canTick()) return;
            var hot = machine.fluidPort(Direction.EAST);
            var cold = machine.fluidPort(Direction.WEST);
            hot.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 36), IFluidHandler.FluidAction.EXECUTE);
            cold.drain(4000, IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(3, () -> {
            var water = handler(helper, WATER_EAST, Direction.EAST);
            require(helper, water != null && water.fill(new FluidStack(Fluids.WATER, 256),
                    IFluidHandler.FluidAction.EXECUTE) > 0, "给水能力不可用");
        });
        helper.runAfterDelay(215, () -> {
            var owner = owner(helper);
            require(helper, owner.ledger().ready() && owner.ledger().warmHu() == 3600,
                    "核热未把一段炉体暖满");
            require(helper, owner.ledger().steam() > 0, "暖炉完成后未真实产汽");
            var steam = handler(helper, STEAM_WEST, Direction.WEST);
            require(helper, steam != null && BoilerContent.isSteam(steam.getFluidInTank(0)),
                    "真实汽口未公开超临界蒸汽");
            require(helper, source(helper).ledger().hot() + source(helper).ledger().cold() >= 36,
                    "源端未真实转换冷热液");
            helper.succeed();
        });
    }

    /** 真实Create泵把同一源罐水分流到两个物理给水口，逐tick总入量不能突破整炉额度。 */
    @GameTest(template = "boiler_empty", timeoutTicks = 180)
    public static void realCreatePumpSplitsWaterWithoutOverpromise(GameTestHelper helper) {
        build(helper, true);
        helper.runAfterDelay(4, () -> {
            var nearFull = new net.minecraft.nbt.CompoundTag();
            nearFull.putInt("Water", 15900);
            owner(helper).ledger().load(nearFull);
            buildWaterNetwork(helper);
        });
        boolean[] flowed = {false, false};
        helper.onEachTick(() -> {
            flowed[0] |= activeFlow(helper, new BlockPos(13, 3, 3), Direction.WEST);
            flowed[1] |= activeFlow(helper, new BlockPos(10, 3, 6), Direction.NORTH);
        });
        helper.runAfterDelay(55, () -> {
            int water = owner(helper).ledger().water();
            var sourceTank = handler(helper, WATER_TANK, Direction.EAST);
            require(helper, sourceTank != null && sourceTank.getFluidInTank(0).getAmount() < 4000,
                    "真实Create源储罐未向锅炉送水：水=" + water + " 泵="
                            + ((PumpBlockEntity) helper.getBlockEntity(WATER_PUMP)).getSpeed()
                            + " 管=" + pipeInfo(helper, new BlockPos(13, 3, 3))
                            + " 入=" + pipeInfo(helper, new BlockPos(16, 3, 3)));
            require(helper, sourceTank.getFluidInTank(0).getAmount() + water == 19900,
                    "多口泵管分流发生水量漂移：源=" + sourceTank.getFluidInTank(0).getAmount() + " 炉=" + water);
            require(helper, water == BoilerState.CAPACITY && sourceTank.getFluidInTank(0).getAmount() == 3900,
                    "多口泵管未进入共享给水账本");
            require(helper, flowed[0] && flowed[1], "两个真实Create分支未同时触达物理给水口");
            require(helper, ((PumpBlockEntity) helper.getBlockEntity(WATER_PUMP)).getSpeed() != 0,
                    "真实Create给水泵没有动力");
            helper.succeed();
        });
    }

    /** 暖炉后真实Create汽泵将本模组超临界蒸汽抽入原生储罐。 */
    @GameTest(template = "boiler_empty", timeoutTicks = 290)
    public static void realCreateSteamPumpFillsNativeTank(GameTestHelper helper) {
        build(helper, false);
        helper.setBlock(STEAM_WEST, BoilerContent.CASING.get());
        BlockPos southSteam = CENTER.offset(0, 3, 2);
        helper.setBlock(southSteam, BoilerContent.STEAM_PORT.get().defaultBlockState()
                .setValue(BoilerPartBlock.FACING, Direction.SOUTH));
        helper.onEachTick(() -> {
            var machine = source(helper);
            if (!machine.current() || !machine.canTick()) return;
            var hot = machine.fluidPort(Direction.EAST);
            hot.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 36), IFluidHandler.FluidAction.EXECUTE);
            machine.fluidPort(Direction.WEST).drain(4000, IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(4, () -> {
            var water = handler(helper, WATER_EAST, Direction.EAST);
            require(helper, water.fill(new FluidStack(Fluids.WATER, 256), IFluidHandler.FluidAction.EXECUTE) > 0,
                    "蒸汽管网场景无法给水");
            buildSteamNetwork(helper);
        });
        helper.runAfterDelay(240, () -> {
            IFluidHandler tank = handler(helper, STEAM_TANK, Direction.SOUTH);
            require(helper, tank != null && BoilerContent.isSteam(tank.getFluidInTank(0))
                    && tank.getFluidInTank(0).getAmount() > 0,
                    "真实Create汽泵未把超临界蒸汽送入储罐：炉=" + owner(helper).ledger().steam()
                            + " 罐=" + (tank == null ? -1 : tank.getFluidInTank(0).getAmount())
                            + " 泵速=" + ((PumpBlockEntity) helper.getBlockEntity(STEAM_PUMP)).getSpeed()
                            + " 汽口=" + handler(helper, southSteam, Direction.SOUTH)
                            + " 入=" + pipeInfo(helper, new BlockPos(10, 5, 6))
                            + " 出=" + pipeInfo(helper, new BlockPos(10, 5, 8)));
            require(helper, ((PumpBlockEntity) helper.getBlockEntity(STEAM_PUMP)).getSpeed() != 0,
                    "真实Create汽泵没有动力");
            helper.succeed();
        });
    }

    /** 无外部泵时，汽口自有压力沿普通Create管路把库存送到储罐。 */
    @GameTest(template = "boiler_empty", timeoutTicks = 85)
    public static void steamPortDrivesNativePipeWithoutPump(GameTestHelper helper) {
        build(helper, false);
        BlockPos tankPos = new BlockPos(4, 5, 3);
        BlockPos[] pipes = {new BlockPos(7, 5, 3), new BlockPos(6, 5, 3), new BlockPos(5, 5, 3)};
        helper.setBlock(tankPos, AllBlocks.FLUID_TANK.get());
        for (BlockPos pipe : pipes) helper.setBlock(pipe, AllBlocks.FLUID_PIPE.get());
        sealOpenEnds(helper, pipes);
        helper.runAfterDelay(4, () -> seedSteam(helper, 1000));
        helper.runAfterDelay(45, () -> {
            var tank = handler(helper, tankPos, Direction.EAST);
            require(helper, tank != null && BoilerContent.isSteam(tank.getFluidInTank(0))
                    && tank.getFluidInTank(0).getAmount() > 0,
                    "无泵管线未收到蒸汽：入口=" + pipeInfo(helper, pipes[0]));
            require(helper, tank.getFluidInTank(0).getAmount() + owner(helper).ledger().steam() == 1000,
                    "无泵输送丢失或复制蒸汽");
            helper.succeed();
        });
    }

    /** 两个合法汽口各自给独立无泵管线供压，确认单口压力没有被汽口数量均分。 */
    @GameTest(template = "boiler_empty", timeoutTicks = 95)
    public static void twoSteamPortsDriveSeparateNativePipesWithoutPump(GameTestHelper helper) {
        build(helper, false);
        BlockPos eastSteam = CENTER.offset(2, 3, 0);
        helper.setBlock(eastSteam, BoilerContent.STEAM_PORT.get().defaultBlockState()
                .setValue(BoilerPartBlock.FACING, Direction.EAST));
        BlockPos westTank = new BlockPos(4, 5, 3);
        BlockPos eastTank = new BlockPos(16, 5, 3);
        BlockPos[] westPipes = {new BlockPos(7, 5, 3), new BlockPos(6, 5, 3), new BlockPos(5, 5, 3)};
        BlockPos[] eastPipes = {new BlockPos(13, 5, 3), new BlockPos(14, 5, 3), new BlockPos(15, 5, 3)};
        helper.setBlock(westTank, AllBlocks.FLUID_TANK.get());
        helper.setBlock(eastTank, AllBlocks.FLUID_TANK.get());
        for (BlockPos pipe : westPipes) helper.setBlock(pipe, AllBlocks.FLUID_PIPE.get());
        for (BlockPos pipe : eastPipes) helper.setBlock(pipe, AllBlocks.FLUID_PIPE.get());
        sealOpenEnds(helper, westPipes);
        sealOpenEnds(helper, eastPipes);
        helper.runAfterDelay(4, () -> seedSteam(helper, 4000));
        boolean[] bothPressured = {false};
        helper.runAfterDelay(8, () -> {
            var westPipe = FluidPropagator.getPipe(helper.getLevel(), helper.absolutePos(westPipes[0]));
            var eastPipe = FluidPropagator.getPipe(helper.getLevel(), helper.absolutePos(eastPipes[0]));
            require(helper, owner(helper).ledger().steam() > 0
                            && westPipe != null && eastPipe != null
                            && westPipe.getConnection(Direction.EAST) != null
                            && eastPipe.getConnection(Direction.WEST) != null
                            && westPipe.getConnection(Direction.EAST).getPressure().getFirst() >= 512f
                            && eastPipe.getConnection(Direction.WEST).getPressure().getFirst() >= 512f,
                    "库存仍有蒸汽时，每个独立汽口都应保有对应256mB/t的Create压力");
            bothPressured[0] = true;
        });
        helper.runAfterDelay(65, () -> {
            var west = handler(helper, westTank, Direction.EAST);
            var east = handler(helper, eastTank, Direction.WEST);
            int westAmount = west == null ? 0 : west.getFluidInTank(0).getAmount();
            int eastAmount = east == null ? 0 : east.getFluidInTank(0).getAmount();
            require(helper, westAmount > 0 && eastAmount > 0,
                    "两个汽口的独立无泵管线未同时收到蒸汽：西=" + westAmount + " 东=" + eastAmount);
            require(helper, westAmount + eastAmount + owner(helper).ledger().steam() == 4000,
                    "两条独立汽路没有与共享炉内库存守恒");
            require(helper, bothPressured[0], "没有在库存仍有蒸汽时验证到两个端口压力");
            helper.succeed();
        });
    }

    /** 覆盖先铺汽管再成型、成型后加口、拆口及拆壳重搭后的Create接面恢复。 */
    @GameTest(template = "boiler_empty", timeoutTicks = 100)
    public static void steamPipePlacedBeforeFormationReconnectsAfterShellRestore(GameTestHelper helper) {
        build(helper, false);
        BlockPos shell = CENTER.above(4).east().south();
        helper.setBlock(shell, Blocks.AIR);
        BlockPos westTankPos = new BlockPos(4, 5, 3);
        BlockPos eastTankPos = new BlockPos(16, 5, 3);
        // 分支使首管已有两条有效接面，不会被Create的直管自动补开朝向汽口的一面。
        BlockPos[] westPipes = {new BlockPos(7, 5, 3), new BlockPos(6, 5, 3),
                new BlockPos(5, 5, 3), new BlockPos(7, 5, 4)};
        BlockPos[] eastPipes = {new BlockPos(13, 5, 3), new BlockPos(14, 5, 3), new BlockPos(15, 5, 3)};
        helper.setBlock(westTankPos, AllBlocks.FLUID_TANK.get());
        helper.setBlock(eastTankPos, AllBlocks.FLUID_TANK.get());
        for (BlockPos pipe : westPipes) helper.setBlock(pipe, AllBlocks.FLUID_PIPE.get());
        for (BlockPos pipe : eastPipes) helper.setBlock(pipe, AllBlocks.FLUID_PIPE.get());
        sealOpenEnds(helper, westPipes);
        sealOpenEnds(helper, eastPipes);
        helper.runAfterDelay(4, () -> {
            var entry = FluidPropagator.getPipe(helper.getLevel(), helper.absolutePos(westPipes[0]));
            require(helper, entry != null && entry.getConnection(Direction.EAST) == null,
                    "未成型时先铺管错误连接了空汽口能力");
            seedSteam(helper, 8000);
            helper.setBlock(shell, BoilerContent.CASING.get());
        });
        int[] beforePortRemoval = {0};
        int[] beforeBreak = {0, 0};
        int[] beforeRebuild = {0, 0, 0};
        IFluidHandler[] removedPortHandle = {null};
        IFluidHandler[] formedPortHandles = {null, null};
        helper.runAfterDelay(20, () -> helper.setBlock(CENTER.offset(2, 3, 0),
                BoilerContent.STEAM_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.EAST)));
        helper.runAfterDelay(30, () -> {
            var west = handler(helper, westTankPos, Direction.EAST);
            var east = handler(helper, eastTankPos, Direction.WEST);
            beforeBreak[0] = west == null ? 0 : west.getFluidInTank(0).getAmount();
            beforeBreak[1] = east == null ? 0 : east.getFluidInTank(0).getAmount();
            var entry = FluidPropagator.getPipe(helper.getLevel(), helper.absolutePos(westPipes[0]));
            var addedPortEntry = FluidPropagator.getPipe(helper.getLevel(), helper.absolutePos(eastPipes[0]));
            require(helper, entry != null && entry.getConnection(Direction.EAST) != null && beforeBreak[0] > 0,
                    "先铺西侧管未在锅炉成型后重开接面并主动送汽");
            require(helper, addedPortEntry != null && addedPortEntry.getConnection(Direction.WEST) != null
                            && beforeBreak[1] > 0,
                    "成型后新增的东侧合法汽口未重开预铺管路并主动送汽");
            require(helper, beforeBreak[0] + beforeBreak[1] + owner(helper).ledger().steam() == 8000,
                    "双汽口成型后蒸汽不守恒");
            beforePortRemoval[0] = beforeBreak[1];
            removedPortHandle[0] = handler(helper, CENTER.offset(2, 3, 0), Direction.EAST);
            helper.setBlock(CENTER.offset(2, 3, 0), BoilerContent.CASING.get());
        });
        helper.runAfterDelay(34, () -> {
            var entry = FluidPropagator.getPipe(helper.getLevel(), helper.absolutePos(eastPipes[0]));
            var connection = entry == null ? null : entry.getConnection(Direction.WEST);
            require(helper, pressureReleased(connection),
                    "拆除新增汽口后东侧Create连接仍保留锅炉压力：连接=" + pressureInfo(connection)
                            + " 成型=" + (BoilerStructure.inspect(helper.getLevel(), helper.absolutePos(CONTROL)) != null));
            require(helper, handler(helper, CENTER.offset(2, 3, 0), Direction.EAST) == null
                            && removedPortHandle[0] != null
                            && removedPortHandle[0].drain(1, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                    "拆口后端口能力或拆除前缓存句柄仍可使用");
            helper.setBlock(CENTER.offset(2, 3, 0), BoilerContent.STEAM_PORT.get().defaultBlockState()
                    .setValue(BoilerPartBlock.FACING, Direction.EAST));
        });
        helper.runAfterDelay(47, () -> {
            var tank = handler(helper, eastTankPos, Direction.WEST);
            int received = tank == null ? 0 : tank.getFluidInTank(0).getAmount();
            var entry = FluidPropagator.getPipe(helper.getLevel(), helper.absolutePos(eastPipes[0]));
            require(helper, entry != null && entry.getConnection(Direction.WEST) != null
                            && received > beforePortRemoval[0],
                    "东侧汽口重新放回后未恢复管路和出汽");
            formedPortHandles[0] = handler(helper, STEAM_WEST, Direction.WEST);
            formedPortHandles[1] = handler(helper, CENTER.offset(2, 3, 0), Direction.EAST);
            require(helper, formedPortHandles[0] != null && formedPortHandles[1] != null
                            && formedPortHandles[0].getTanks() == 1 && formedPortHandles[1].getTanks() == 1,
                    "拆壳前无法重新取得两个有效汽口句柄");
            helper.setBlock(shell, Blocks.AIR);
        });
        helper.runAfterDelay(54, () -> {
            var westEntry = FluidPropagator.getPipe(helper.getLevel(), helper.absolutePos(westPipes[0]));
            var eastEntry = FluidPropagator.getPipe(helper.getLevel(), helper.absolutePos(eastPipes[0]));
            var westConnection = westEntry == null ? null : westEntry.getConnection(Direction.EAST);
            var eastConnection = eastEntry == null ? null : eastEntry.getConnection(Direction.WEST);
            require(helper, pressureReleased(westConnection) && pressureReleased(eastConnection),
                    "拆壳后汽路仍残留锅炉自有压力：西=" + pressureInfo(westConnection)
                            + " 东=" + pressureInfo(eastConnection)
                            + " 成型=" + (BoilerStructure.inspect(helper.getLevel(), helper.absolutePos(CONTROL)) != null));
            require(helper, formedPortHandles[0] != null && formedPortHandles[1] != null
                            && formedPortHandles[0].getTanks() == 0 && formedPortHandles[1].getTanks() == 0,
                    "拆壳后缓存汽口能力仍有效");
            var westTank = handler(helper, westTankPos, Direction.EAST);
            var eastTank = handler(helper, eastTankPos, Direction.WEST);
            beforeRebuild[0] = westTank == null ? 0 : westTank.getFluidInTank(0).getAmount();
            beforeRebuild[1] = eastTank == null ? 0 : eastTank.getFluidInTank(0).getAmount();
            beforeRebuild[2] = owner(helper).ledger().steam();
            require(helper, BoilerStructure.inspect(helper.getLevel(), helper.absolutePos(CONTROL)) == null
                            && beforeRebuild[0] + beforeRebuild[1] + beforeRebuild[2] == 8000
                            && beforeRebuild[2] + 2000 <= BoilerState.CAPACITY,
                    "拆壳阶段原始8000mB未守恒、锅炉未停机或共享蒸汽库存没有补入空间");
            seedSteam(helper, beforeRebuild[2] + 2000);
            helper.setBlock(shell, BoilerContent.CASING.get());
        });
        helper.runAfterDelay(78, () -> {
            var west = handler(helper, westTankPos, Direction.EAST);
            var east = handler(helper, eastTankPos, Direction.WEST);
            int westReceived = west == null ? 0 : west.getFluidInTank(0).getAmount();
            int eastReceived = east == null ? 0 : east.getFluidInTank(0).getAmount();
            var westEntry = FluidPropagator.getPipe(helper.getLevel(), helper.absolutePos(westPipes[0]));
            var eastEntry = FluidPropagator.getPipe(helper.getLevel(), helper.absolutePos(eastPipes[0]));
            require(helper, westEntry != null && westEntry.getConnection(Direction.EAST) != null
                            && eastEntry != null && eastEntry.getConnection(Direction.WEST) != null
                            && westReceived > beforeRebuild[0] && eastReceived > beforeRebuild[1]
                            && westReceived + eastReceived + owner(helper).ledger().steam()
                            == 10000,
                    "拆壳补入2000mB后重搭未使两路分别增量出汽或原始8000+补入2000总量不守恒");
            helper.succeed();
        });
    }

    /** 锅炉自身压力面对真正开放的 Create 管端时不得把非世界蒸汽当作排气删除。 */
    @GameTest(template = "boiler_empty", timeoutTicks = 75)
    public static void activeSteamPortRefusesOpenCreatePipe(GameTestHelper helper) {
        build(helper, false);
        BlockPos pipePos = new BlockPos(7, 5, 3);
        helper.setBlock(pipePos, AllBlocks.FLUID_PIPE.get());
        helper.runAfterDelay(4, () -> seedSteam(helper, 1000));
        helper.runAfterDelay(35, () -> {
            require(helper, FluidPropagator.isOpenEnd(helper.getLevel(), helper.absolutePos(pipePos), Direction.WEST),
                    "主动汽口测试管路并非开放端");
            require(helper, owner(helper).ledger().steam() == 1000,
                    "锅炉主动压力把非世界蒸汽从开放Create管口删除");
            helper.succeed();
        });
    }

    /** 无泵分支管路把汽送往两个原生罐，不用私有管网库存。 */
    @GameTest(template = "boiler_empty", timeoutTicks = 90)
    public static void boilerPressureSplitsAcrossTwoNativeTanks(GameTestHelper helper) {
        build(helper, false);
        BlockPos northTank = new BlockPos(5, 5, 1);
        BlockPos southTank = new BlockPos(5, 5, 5);
        BlockPos[] pipes = {new BlockPos(7, 5, 3), new BlockPos(6, 5, 3),
                new BlockPos(5, 5, 3), new BlockPos(5, 5, 2), new BlockPos(5, 5, 4)};
        helper.setBlock(northTank, AllBlocks.FLUID_TANK.get());
        helper.setBlock(southTank, AllBlocks.FLUID_TANK.get());
        for (BlockPos pipe : pipes) helper.setBlock(pipe, AllBlocks.FLUID_PIPE.get());
        sealOpenEnds(helper, pipes);
        helper.runAfterDelay(4, () -> seedSteam(helper, 4000));
        helper.runAfterDelay(55, () -> {
            var north = handler(helper, northTank, Direction.SOUTH);
            var south = handler(helper, southTank, Direction.NORTH);
            int first = north == null ? 0 : north.getFluidInTank(0).getAmount();
            int second = south == null ? 0 : south.getFluidInTank(0).getAmount();
            require(helper, first > 0 && second > 0,
                    "无泵压力未到达两条分支：北=" + first + " 南=" + second);
            require(helper, first + second + owner(helper).ledger().steam() == 4000,
                    "分支管网丢失或复制蒸汽");
            helper.succeed();
        });
    }

    /** 原生泵多次刷新不把锅炉自有压力叠加成无限大，也不停止真实输送。 */
    @GameTest(template = "boiler_empty", timeoutTicks = 95)
    public static void nativePumpRefreshKeepsOwnedPressureBounded(GameTestHelper helper) {
        build(helper, false);
        helper.setBlock(STEAM_WEST, BoilerContent.CASING.get());
        BlockPos southSteam = CENTER.offset(0, 3, 2);
        helper.setBlock(southSteam, BoilerContent.STEAM_PORT.get().defaultBlockState()
                .setValue(BoilerPartBlock.FACING, Direction.SOUTH));
        buildSteamNetwork(helper);
        helper.runAfterDelay(4, () -> seedSteam(helper, 12000));
        for (int tick : new int[]{12, 18, 24}) helper.runAfterDelay(tick, () -> {
            require(helper, owner(helper).ledger().steam() > 1000, "压力刷新前蒸汽已耗尽，无法检验重复加压");
            if (tick == 18 || tick == 24) helper.setBlock(STEAM_PUMP,
                    helper.getBlockState(STEAM_PUMP).setValue(PumpBlock.FACING,
                            tick == 18 ? Direction.NORTH : Direction.SOUTH));
            ((PumpBlockEntity) helper.getBlockEntity(STEAM_PUMP)).updatePressureChange();
            var pipe = FluidPropagator.getPipe(helper.getLevel(), helper.absolutePos(new BlockPos(10, 5, 6)));
            var entry = pipe == null ? null : pipe.getConnection(Direction.NORTH);
            require(helper, entry != null && entry.getPressure().getFirst() <= 768f,
                    "泵刷新期间重复叠加锅炉压力：" + (entry == null ? -1 : entry.getPressure().getFirst()));
        });
        helper.runAfterDelay(60, () -> {
            var tank = handler(helper, STEAM_TANK, Direction.SOUTH);
            int received = tank == null ? 0 : tank.getFluidInTank(0).getAmount();
            require(helper, received > 0 && received + owner(helper).ledger().steam() == 12000,
                    "原生泵刷新后蒸汽停运或不守恒");
            helper.setBlock(southSteam, BoilerContent.CASING.get());
        });
        helper.runAfterDelay(64, () -> {
            var pipe = FluidPropagator.getPipe(helper.getLevel(), helper.absolutePos(new BlockPos(10, 5, 6)));
            var entry = pipe == null ? null : pipe.getConnection(Direction.NORTH);
            require(helper, entry != null && entry.getPressure().getFirst() <= 256f,
                    "汽口拆除后仍保留锅炉自有压力");
            helper.succeed();
        });
    }

    /** 原生压力、多个锅炉压力和读盘后的重新认领均按独立份额结算。 */
    @GameTest(template = "boiler_empty")
    public static void pipePressureOwnershipSurvivesSaveAndWipe(GameTestHelper helper) {
        var connection = new PipeConnection(Direction.WEST);
        connection.addPressure(true, 128);
        var owned = (BoilerPressureConnection) connection;
        owned.createNuclearIndustry$setBoilerPressure(1L, true, 256);
        owned.createNuclearIndustry$setBoilerPressure(2L, true, 128);
        require(helper, connection.getPressure().getFirst() == 512, "多锅炉压力没有与原生压力相加");
        owned.createNuclearIndustry$setBoilerPressure(1L, true, 0);
        require(helper, connection.getPressure().getFirst() == 256, "撤销一台锅炉清除了另一台或原生压力");
        var tag = new net.minecraft.nbt.CompoundTag();
        connection.serializeNBT(tag, helper.getLevel().registryAccess(), false);
        var loaded = new PipeConnection(Direction.WEST);
        loaded.deserializeNBT(tag, helper.getLevel().registryAccess(), BlockPos.ZERO, false);
        require(helper, loaded.getPressure().getFirst() == 128, "管路读盘保留了过期锅炉压力");
        ((BoilerPressureConnection) loaded).createNuclearIndustry$setBoilerPressure(2L, true, 128);
        require(helper, loaded.getPressure().getFirst() == 256, "锅炉读盘重新认领时压力翻倍");
        loaded.wipePressure();
        ((BoilerPressureConnection) loaded).createNuclearIndustry$setBoilerPressure(2L, true, 128);
        require(helper, loaded.getPressure().getFirst() == 128, "原生wipe后锅炉份额没有重建");
        helper.succeed();
    }

    /** 两个相邻Create储罐分别接收各自汽口主动推送，并与同口被动抽取共用额度。 */
    @GameTest(template = "boiler_empty", timeoutTicks = 60)
    public static void adjacentTanksAndPassivePortsKeepIndependentSteamBudgets(GameTestHelper helper) {
        build(helper, false);
        BlockPos eastPort = CENTER.offset(2, 3, 0);
        BlockPos westTankPos = STEAM_WEST.west();
        BlockPos eastTankPos = eastPort.east();
        helper.setBlock(westTankPos, AllBlocks.FLUID_TANK.get());
        helper.setBlock(eastTankPos, AllBlocks.FLUID_TANK.get());
        helper.setBlock(eastPort, BoilerContent.STEAM_PORT.get().defaultBlockState()
                .setValue(BoilerPartBlock.FACING, Direction.EAST));
        helper.setBlock(CONTROL.north(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(4, () -> seedSteam(helper, 4000));
        helper.runAfterDelay(12, () -> {
            var westTank = handler(helper, westTankPos, Direction.EAST);
            var eastTank = handler(helper, eastTankPos, Direction.WEST);
            int westAmount = westTank == null ? 0 : westTank.getFluidInTank(0).getAmount();
            int eastAmount = eastTank == null ? 0 : eastTank.getFluidInTank(0).getAmount();
            require(helper, westAmount > 0 && eastAmount > 0, "两个汽口未各自向相邻储罐主动送汽");
            long now = helper.getLevel().getGameTime();
            int westUsed = BoilerState.FLOW_LIMIT - owner(helper).ledger()
                    .remainingDrain(helper.absolutePos(STEAM_WEST), now);
            var passive = handler(helper, STEAM_WEST, Direction.WEST);
            require(helper, passive != null, "汽口被动抽取能力不可用");
            int extracted = passive.drain(256, IFluidHandler.FluidAction.EXECUTE).getAmount();
            require(helper, westUsed + extracted <= BoilerState.FLOW_LIMIT,
                    "同一汽口的主动推送与被动抽取绕过256mB/t额度");
            require(helper, westAmount + eastAmount + owner(helper).ledger().steam() + extracted == 4000,
                    "相邻双罐和被动抽取没有与共享炉内库存守恒");
            require(helper, owner(helper).ledger().remainingDrain(helper.absolutePos(eastPort), now)
                            < BoilerState.FLOW_LIMIT,
                    "东侧合法汽口没有独立参与本 tick 主动出汽");
            helper.succeed();
        });
    }

    /** 非世界流体面对Create真实开放管口时，源储罐不得被抽空并删除蒸汽。 */
    @GameTest(template = "boiler_empty", timeoutTicks = 100)
    public static void openCreatePipeRefusesStorageOnlySteam(GameTestHelper helper) {
        BlockPos tankPos = new BlockPos(10, 2, 6);
        BlockPos pumpPos = new BlockPos(10, 2, 5);
        BlockPos pipePos = new BlockPos(10, 2, 4);
        helper.setBlock(tankPos, AllBlocks.FLUID_TANK.get());
        IFluidHandler tank = handler(helper, tankPos, Direction.SOUTH);
        require(helper, tank != null && tank.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 1000),
                IFluidHandler.FluidAction.EXECUTE) == 1000, "开放管测试蒸汽预装失败");
        helper.setBlock(pipePos, AllBlocks.FLUID_PIPE.get());
        helper.setBlock(pumpPos, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.NORTH));
        helper.setBlock(new BlockPos(11, 2, 5), AllBlocks.COGWHEEL.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(new BlockPos(11, 2, 6), AllBlocks.SHAFT.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(new BlockPos(11, 2, 7), AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.NORTH));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(new BlockPos(11, 2, 7))).generatedSpeed.setValue(256);
        propagate(helper, new BlockPos[]{pipePos});
        helper.runAfterDelay(50, () -> {
            require(helper, FluidPropagator.isOpenEnd(helper.getLevel(), helper.absolutePos(pipePos), Direction.NORTH),
                    "测试管口并非真实开放端");
            require(helper, ((PumpBlockEntity) helper.getBlockEntity(pumpPos)).getSpeed() != 0,
                    "开放管场景泵没有动力");
            require(helper, handler(helper, tankPos, Direction.SOUTH).getFluidInTank(0).getAmount() == 1000,
                    "开放Create管口无声删除了超临界蒸汽");
            helper.succeed();
        });
    }

    /** 生存破坏只掉一件携物控制器；缺壳拆放期间库存与非零炉体余温不复制、不清零。 */
    @GameTest(template = "boiler_empty", timeoutTicks = 100)
    public static void controllerBreakAndPortableRestoreKeepSingleInventory(GameTestHelper helper) {
        build(helper, false);
        helper.onEachTick(() -> {
            var machine = source(helper);
            if (!machine.current() || !machine.canTick()) return;
            var hot = machine.fluidPort(Direction.EAST);
            var cold = machine.fluidPort(Direction.WEST);
            hot.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 36), IFluidHandler.FluidAction.EXECUTE);
            cold.drain(4000, IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(3, () -> handler(helper, WATER_EAST, Direction.EAST).fill(
                new FluidStack(Fluids.WATER, 256), IFluidHandler.FluidAction.EXECUTE));
        final double[] warmBefore = {0};
        final int[] waterBefore = {0};
        IFluidHandler[] cached = {null};
        helper.runAfterDelay(55, () -> {
            warmBefore[0] = owner(helper).ledger().warmHu();
            waterBefore[0] = owner(helper).ledger().water();
            require(helper, warmBefore[0] > 0, "破坏前炉体没有真实余温");
            cached[0] = handler(helper, WATER_EAST, Direction.EAST);
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
            require(helper, player.gameMode.destroyBlock(helper.absolutePos(CONTROL)), "控制器生存破坏失败");
            require(helper, cached[0].fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "破坏后的旧端口句柄仍有效");
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(helper.absolutePos(CONTROL)).inflate(1),
                    e -> e.getItem().is(BoilerContent.CONTROLLER_ITEM.get()));
            require(helper, drops.size() == 1 && drops.getFirst().getItem().getCount() == 1,
                    "控制器破坏没有恰好掉一件");
            ItemStack carried = drops.getFirst().getItem().copy();
            require(helper, carried.get(DataComponents.CUSTOM_DATA) != null, "控制器掉落缺少携物数据");
            drops.getFirst().discard();
            helper.setBlock(CENTER.above(4).east(), Blocks.AIR);
            helper.setBlock(CONTROL, BoilerContent.CONTROLLER.get().defaultBlockState()
                    .setValue(BoilerPartBlock.FACING, Direction.NORTH));
            helper.getBlockState(CONTROL).getBlock().setPlacedBy(helper.getLevel(), helper.absolutePos(CONTROL),
                    helper.getBlockState(CONTROL), null, carried);
        });
        helper.runAfterDelay(59, () -> {
            require(helper, BoilerStructure.inspect(helper.getLevel(), helper.absolutePos(CONTROL)) == null,
                    "缺壳状态错误成型");
            require(helper, owner(helper).ledger().water() == waterBefore[0], "携物重放复制或删除给水");
            require(helper, owner(helper).ledger().warmHu() > 0
                    && owner(helper).ledger().warmHu() <= warmBefore[0], "未成型拆放清空或增加炉体余热");
            require(helper, cached[0].fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "旧端口句柄在重放后复活");
            helper.succeed();
        });
    }

    /** 原生工作台与Create动力合成在真实配方管理器中匹配输入并给出约定件数。 */
    @GameTest(template = "boiler_empty")
    public static void casingAndControllerRecipesLoadAndMatch(GameTestHelper helper) {
        Item steel = BuiltInRegistries.ITEM.stream().filter(item -> new ItemStack(item).is(TagKey.create(
                Registries.ITEM, ResourceLocation.parse("c:plates/steel")))).findFirst().orElseThrow();
        var casingHolder = helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                "create_nuclear_industry", "crafting/high_pressure_boiler_casing")).orElseThrow();
        require(helper, casingHolder.value() instanceof CraftingRecipe
                && !(casingHolder.value() instanceof MechanicalCraftingRecipe), "外壳不是原生工作台配方");
        var casing = (CraftingRecipe) casingHolder.value();
        List<ItemStack> casingGrid = new ArrayList<>();
        for (String row : new String[]{"SBS", "BRB", "SBS"}) for (char symbol : row.toCharArray())
            casingGrid.add(switch (symbol) {
                case 'S' -> new ItemStack(steel);
                case 'B' -> new ItemStack(BasicMaterialContent.REFRACTORY_BRICK.get());
                default -> new ItemStack(HeatMaterialsContent.REINFORCED_STEEL_PLATE.get());
            });
        CraftingInput casingInput = CraftingInput.of(3, 3, casingGrid);
        ItemStack casingOutput = casing.assemble(casingInput, helper.getLevel().registryAccess());
        require(helper, casing.matches(casingInput, helper.getLevel())
                && casingOutput.is(BoilerContent.CASING_ITEM.get()) && casingOutput.getCount() == 8,
                "八个外壳配方未真实匹配或数量错误");

        var controllerHolder = helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                "create_nuclear_industry", "mechanical_crafting/high_pressure_boiler_controller")).orElseThrow();
        require(helper, controllerHolder.value() instanceof MechanicalCraftingRecipe, "控制器不是Create动力合成配方");
        var controller = (MechanicalCraftingRecipe) controllerHolder.value();
        Item precision = BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:precision_mechanism"));
        List<ItemStack> controllerGrid = new ArrayList<>();
        for (String row : new String[]{" SSS ", "SRPRS", "SICIS", "SRPRS", " SSS "})
            for (char symbol : row.toCharArray()) controllerGrid.add(switch (symbol) {
                case 'S' -> new ItemStack(steel);
                case 'R' -> new ItemStack(HeatMaterialsContent.REINFORCED_STEEL_PLATE.get());
                case 'P' -> new ItemStack(precision);
                case 'I' -> new ItemStack(BasicMaterialContent.INDUSTRIAL_SENSOR.get());
                case 'C' -> new ItemStack(BoilerContent.CASING_ITEM.get());
                default -> ItemStack.EMPTY;
            });
        boolean ingredientsMatch = controller.getIngredients().size() == controllerGrid.size();
        for (int i = 0; ingredientsMatch && i < controllerGrid.size(); i++)
            ingredientsMatch = controller.getIngredients().get(i).test(controllerGrid.get(i));
        ItemStack output = controller.getResultItem(helper.getLevel().registryAccess());
        require(helper, ingredientsMatch && controller.getWidth() == 5 && controller.getHeight() == 5
                && !controller.acceptsMirrored()
                && output.is(BoilerContent.CONTROLLER_ITEM.get()) && output.getCount() == 1,
                "21格控制器配方未真实匹配或数量错误");
        helper.succeed();
    }

    private static void buildWaterNetwork(GameTestHelper helper) {
        helper.setBlock(WATER_TANK, AllBlocks.FLUID_TANK.get());
        var source = handler(helper, WATER_TANK, Direction.EAST);
        require(helper, source != null && source.fill(new FluidStack(Fluids.WATER, 4000),
                IFluidHandler.FluidAction.EXECUTE) == 4000, "Create给水源罐预装失败");
        BlockPos[] pipes = {new BlockPos(16, 3, 3), new BlockPos(14, 3, 3), new BlockPos(13, 3, 3),
                new BlockPos(13, 3, 4), new BlockPos(13, 3, 5), new BlockPos(13, 3, 6),
                new BlockPos(12, 3, 6), new BlockPos(11, 3, 6), new BlockPos(10, 3, 6)};
        for (BlockPos pipe : pipes) helper.setBlock(pipe, AllBlocks.FLUID_PIPE.get());
        helper.setBlock(WATER_PUMP, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.WEST));
        helper.setBlock(new BlockPos(15, 3, 4), AllBlocks.COGWHEEL.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
        helper.setBlock(new BlockPos(16, 3, 4), AllBlocks.SHAFT.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
        helper.setBlock(new BlockPos(17, 3, 4), AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.WEST));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(new BlockPos(17, 3, 4))).generatedSpeed.setValue(256);
        sealOpenEnds(helper, pipes);
    }

    private static void buildSteamNetwork(GameTestHelper helper) {
        helper.setBlock(STEAM_TANK, AllBlocks.FLUID_TANK.get());
        BlockPos[] pipes = {new BlockPos(10, 5, 6), new BlockPos(10, 5, 8)};
        for (BlockPos pipe : pipes) helper.setBlock(pipe, AllBlocks.FLUID_PIPE.get());
        helper.setBlock(STEAM_PUMP, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.SOUTH));
        helper.setBlock(new BlockPos(11, 5, 7), AllBlocks.COGWHEEL.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(new BlockPos(11, 5, 8), AllBlocks.SHAFT.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(new BlockPos(11, 5, 9), AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.NORTH));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(new BlockPos(11, 5, 9))).generatedSpeed.setValue(256);
        sealOpenEnds(helper, pipes);
    }
    private static void propagate(GameTestHelper helper, BlockPos[] pipes) {
        for (BlockPos pipe : pipes) FluidPropagator.propagateChangedPipe(helper.getLevel(), helper.absolutePos(pipe),
                helper.getBlockState(pipe));
    }
    private static boolean pressureReleased(com.simibubi.create.content.fluids.PipeConnection connection) {
        return connection == null || connection.getPressure().getFirst() == 0f
                && connection.getPressure().getSecond() == 0f;
    }
    private static String pressureInfo(com.simibubi.create.content.fluids.PipeConnection connection) {
        return connection == null ? "已移除" : connection.getPressure().toString();
    }
    private static String pipeInfo(GameTestHelper helper, BlockPos pipe) {
        FluidTransportBehaviour behaviour = com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.get(
                helper.getLevel(), helper.absolutePos(pipe), FluidTransportBehaviour.TYPE);
        if (behaviour == null) return "无行为";
        StringBuilder result = new StringBuilder();
        for (Direction direction : Direction.values()) {
            var connection = behaviour.getConnection(direction);
            if (connection != null) result.append(direction).append(':').append(connection.getPressure()).append('/');
        }
        return result.toString();
    }
    private static boolean activeFlow(GameTestHelper helper, BlockPos pipe, Direction side) {
        FluidTransportBehaviour behaviour = com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.get(
                helper.getLevel(), helper.absolutePos(pipe), FluidTransportBehaviour.TYPE);
        var flow = behaviour == null ? null : behaviour.getFlow(side);
        return flow != null && !flow.fluid.isEmpty();
    }
    private static void sealOpenEnds(GameTestHelper helper, BlockPos[] pipes) {
        propagate(helper, pipes);
        for (BlockPos pipe : pipes) {
            FluidTransportBehaviour behaviour = com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.get(
                    helper.getLevel(), helper.absolutePos(pipe), FluidTransportBehaviour.TYPE);
            require(helper, behaviour != null, "Create流体管缺少行为：" + pipe);
            for (Direction direction : Direction.values()) {
                if (behaviour.getConnection(direction) != null
                        && helper.getBlockState(pipe.relative(direction)).isAir()
                        && FluidPropagator.isOpenEnd(helper.getLevel(), helper.absolutePos(pipe), direction))
                    helper.setBlock(pipe.relative(direction), Blocks.IRON_BLOCK);
            }
        }
        propagate(helper, pipes);
    }

    private static void build(GameTestHelper helper, boolean twoWater) {
        for (int y = 0; y < 5; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            BlockPos part = CENTER.offset(x, y, z);
            // 扩建旧炉时保留原控制器实体，模拟玩家只重搭周围壳体。
            if (part.equals(CONTROL) && helper.getBlockState(part).is(BoilerContent.CONTROLLER.get())) continue;
            if (y >= 1 && y <= 3 && Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                helper.setBlock(part, Blocks.AIR);
                continue;
            }
            helper.setBlock(part, BoilerContent.CASING.get());
        }
        if (!helper.getBlockState(CONTROL).is(BoilerContent.CONTROLLER.get()))
            helper.setBlock(CONTROL, BoilerContent.CONTROLLER.get().defaultBlockState()
                    .setValue(BoilerPartBlock.FACING, Direction.NORTH));
        helper.setBlock(WATER_EAST, BoilerContent.WATER_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.EAST));
        if (twoWater) helper.setBlock(WATER_SOUTH, BoilerContent.WATER_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.SOUTH));
        helper.setBlock(STEAM_WEST, BoilerContent.STEAM_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.WEST));
        helper.setBlock(CENTER.above(4), BoilerContent.SAFETY_VALVE.get());
        helper.setBlock(SECTION, BoilerContent.HEAT_SECTION.get());
        helper.setBlock(SOURCE, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState()
                .setValue(NuclearHeatExchangerBlock.FACING, Direction.WEST));
    }
    private static void buildLegacy(GameTestHelper helper) {
        BlockPos oldCenter = CENTER.north();
        for (int y = 0; y < 4; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && z == 0 && (y == 1 || y == 2)) continue;
            helper.setBlock(oldCenter.offset(x, y, z), BoilerContent.CASING.get());
        }
        helper.setBlock(CONTROL, BoilerContent.CONTROLLER.get().defaultBlockState()
                .setValue(BoilerPartBlock.FACING, Direction.NORTH));
        helper.setBlock(oldCenter.offset(1, 1, 0), BoilerContent.WATER_PORT.get().defaultBlockState()
                .setValue(BoilerPartBlock.FACING, Direction.EAST));
        helper.setBlock(oldCenter.offset(-1, 2, 0), BoilerContent.STEAM_PORT.get().defaultBlockState()
                .setValue(BoilerPartBlock.FACING, Direction.WEST));
        helper.setBlock(oldCenter.above(3), BoilerContent.SAFETY_VALVE.get());
        helper.setBlock(oldCenter.east(), BoilerContent.HEAT_SECTION.get());
    }
    private static BoilerControllerBlockEntity owner(GameTestHelper helper) {
        return (BoilerControllerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(CONTROL));
    }
    private static void seedSteam(GameTestHelper helper, int amount) {
        var saved = owner(helper).ledger().save();
        saved.putInt("Steam", amount);
        owner(helper).ledger().load(saved);
    }
    private static NuclearHeatExchangerBlockEntity source(GameTestHelper helper) {
        return (NuclearHeatExchangerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(SOURCE));
    }
    private static IFluidHandler handler(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), side);
    }
    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
