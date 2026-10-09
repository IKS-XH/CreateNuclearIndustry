package com.iksxh.create_nuclear_industry.ponder;

import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlock;
import com.iksxh.create_nuclear_industry.production.FuelSinteringBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 核换热器五条客户端教学故事线。坐标以模板方块格为单位；所有方块、流体和热量变化仅作用于
 * Ponder 临时世界，不调用正式设备账本、流体能力或服务端工艺。
 */
public final class HeatExchangerPonderScenes {
    private static final BlockPos INTRO_EXCHANGER = new BlockPos(5, 1, 5);
    private static final BlockPos INTRO_COLD_TANK = new BlockPos(5, 1, 1);
    private static final BlockPos INTRO_HOT_TANK = new BlockPos(5, 1, 11);
    private static final BlockPos LINE_COLD_TANK = new BlockPos(7, 1, 1);
    private static final BlockPos LINE_HOT_TANK = new BlockPos(7, 1, 10);
    private static final BlockPos CREATE_TANK_CONTROLLER = new BlockPos(6, 2, 5);
    private static final BlockPos CREATE_ENGINE = new BlockPos(5, 2, 5);
    private static final BlockPos BOILER_COLD_TANK = new BlockPos(6, 3, 1);
    private static final BlockPos BOILER_HOT_TANK = new BlockPos(7, 1, 1);
    private static final BlockPos PROCESS_EXCHANGER = new BlockPos(7, 1, 5);
    private static final BlockPos PROCESS_BASIN = PROCESS_EXCHANGER.above();
    private static final BlockPos PROCESS_MIXER = PROCESS_BASIN.above(2);
    private static final BlockPos PROCESS_SHAFT = PROCESS_MIXER.above();
    private static final BlockPos CONDENSE_EXCHANGER = new BlockPos(7, 1, 5);
    private static final BlockPos CONDENSE_SOURCE = CONDENSE_EXCHANGER.above();
    private static final BlockPos CONDENSE_STEAM_TANK = new BlockPos(7, 1, 10);
    private static final BlockPos CONDENSE_WATER_TANK = new BlockPos(7, 1, 1);

    private HeatExchangerPonderScenes() {}

    /** 分别高亮换热器背面、正面和顶部，再展示同向首尾串联及共用冷热库存。 */
    public static void nuclearHeatExchangerIntroduction(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("nuclear_heat_exchanger_introduction", "核换热器：本体与串联");
        scene.configureBasePlate(0, 0, 12);
        scene.scaleSceneView(0.92F);
        scene.showBasePlate();

        Selection machine = util.select().position(INTRO_EXCHANGER);
        Selection pipes = util.select().fromTo(5, 1, 2, 5, 1, 4)
                .add(util.select().fromTo(5, 1, 7, 5, 1, 10));
        Selection tanks = util.select().position(INTRO_COLD_TANK).add(util.select().position(INTRO_HOT_TANK));
        scene.world().showSection(machine, Direction.DOWN);
        scene.idle(15);
        scene.rotateCameraY(180);
        scene.overlay().showOutline(PonderPalette.RED, new Object(), machine, 85);
        note(scene, machine, util.vector().centerOf(INTRO_EXCHANGER), 65,
                "背侧接入热液，正侧排出冷液。");
        scene.rotateCameraY(180);
        scene.overlay().showOutline(PonderPalette.BLUE, new Object(), machine, 85);
        note(scene, machine, util.vector().topOf(INTRO_EXCHANGER), 65,
                "顶部受热面可连接供热负载。朝向决定冷热端口位置。");

        scene.world().showSection(pipes.add(tanks), Direction.SOUTH);
        scene.idle(15);
        setTankFluid(scene, INTRO_HOT_TANK,
                new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 1000));
        setTankFluid(scene, INTRO_COLD_TANK, FluidStack.EMPTY);
        scene.overlay().showBigLine(PonderPalette.RED, util.vector().centerOf(INTRO_HOT_TANK),
                util.vector().centerOf(INTRO_EXCHANGER), 75);
        scene.world().showSection(util.select().position(5, 1, 6), Direction.SOUTH);
        scene.idle(12);
        scene.overlay().showBigLine(PonderPalette.BLUE, util.vector().centerOf(INTRO_EXCHANGER),
                util.vector().centerOf(INTRO_COLD_TANK), 75);
        scene.rotateCameraY(180);
        note(scene, pipes.add(tanks).add(machine).add(util.select().position(5, 1, 6)),
                util.vector().centerOf(INTRO_EXCHANGER), 75,
                "两台同向首尾相接，从串联两端接管并共用冷热液库存；每台顶部都可供热。");
        scene.markAsFinished();
    }

    /** 展示Create锅炉受换热器供热后驱动蒸汽机活塞与其真实连接的动力轴。 */
    public static void nuclearHeatExchangerHeating(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("nuclear_heat_exchanger_heating", "核换热器：供热驱动蒸汽机");
        scene.configureBasePlate(0, 0, 14);
        scene.scaleSceneView(0.88F);
        scene.showBasePlate();

        Selection exchangers = util.select().position(7, 1, 5).add(util.select().position(7, 1, 6));
        Selection pipes = util.select().fromTo(7, 1, 2, 7, 1, 4)
                .add(util.select().fromTo(7, 1, 7, 7, 1, 9));
        Selection tanks = util.select().position(LINE_COLD_TANK).add(util.select().position(LINE_HOT_TANK));
        Selection boilerTanks = util.select().fromTo(6, 2, 5, 7, 2, 6);
        Selection engine = util.select().position(CREATE_ENGINE);
        BlockPos shaftPosition = createShaftPosition();
        Selection shaft = util.select().position(shaftPosition);

        scene.world().showSection(exchangers.add(pipes).add(tanks), Direction.DOWN);
        scene.idle(15);
        setTankFluid(scene, LINE_HOT_TANK,
                new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 1000));
        setTankFluid(scene, LINE_COLD_TANK, FluidStack.EMPTY);
        scene.overlay().showBigLine(PonderPalette.RED, util.vector().centerOf(LINE_HOT_TANK),
                util.vector().centerOf(7, 1, 6), 70);
        note(scene, exchangers.add(pipes), util.vector().centerOf(7, 1, 6), 75,
                "热液从后侧进入，冷液由前侧排出；换热器从锅炉底部向上供热。");

        scene.world().showSection(boilerTanks, Direction.DOWN);
        scene.world().showSection(engine.add(shaft), Direction.WEST);
        scene.idle(15);
        scene.world().setBlock(shaftPosition, AllBlocks.POWERED_SHAFT.getDefaultState()
                .setValue(ShaftBlock.AXIS, Axis.Y), false);
        configureCreateBoilerTank(scene);
        setTankFluid(scene, CREATE_TANK_CONTROLLER, new FluidStack(Fluids.WATER, 4000));
        setCreateBoilerPower(scene, false);
        note(scene, boilerTanks.add(engine).add(shaft), util.vector().centerOf(CREATE_TANK_CONTROLLER), 75,
                "锅炉储罐贴接蒸汽机；下方换热器供热后，蒸汽机可输出动力。");

        scene.world().hideSection(boilerTanks, Direction.UP);
        scene.idle(12);
        scene.overlay().showOutline(PonderPalette.RED, new Object(), exchangers, 115);
        note(scene, exchangers, util.vector().centerOf(7, 1, 5), 95,
                "换热器位于锅炉底部，热液流动时持续向上供热。");
        scene.world().showSection(boilerTanks, Direction.DOWN);
        scene.idle(15);

        setTankFluid(scene, LINE_HOT_TANK,
                new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 500));
        setTankFluid(scene, LINE_COLD_TANK,
                new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 500));
        setExchangerLit(scene, true);
        setCreateBoilerPower(scene, true);
        scene.world().setKineticSpeed(shaft, 64);
        scene.effects().rotationSpeedIndicator(shaftPosition);
        note(scene, engine.add(shaft).add(boilerTanks), util.vector().centerOf(CREATE_ENGINE), 95,
                "锅炉达到小型储罐的工作热级后，蒸汽机活塞往复并带动动力轴旋转。");

        setTankFluid(scene, LINE_HOT_TANK, FluidStack.EMPTY);
        setExchangerLit(scene, false);
        scene.idle(40);
        setCreateBoilerPower(scene, false);
        scene.world().setKineticSpeed(shaft, 0);
        note(scene, engine.add(shaft).add(boilerTanks), util.vector().centerOf(CREATE_ENGINE), 75,
                "热液耗尽后余热短暂维持输出；余热消失后蒸汽机与动力轴停止。");
        scene.markAsFinished();
    }

    /** 以合法最小锅炉的剖面展示底层换热器与上方再加热隔层的对应关系。 */
    public static void nuclearHeatExchangerBoiler(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("nuclear_heat_exchanger_boiler", "核换热器：锅炉内置");
        scene.configureBasePlate(0, 0, 12);
        scene.scaleSceneView(0.8F);
        scene.showBasePlate();

        Selection boiler = util.select().fromTo(5, 1, 5, 9, 5, 9);
        Selection cutaway = boilerCutaway(util);
        Selection coolantLines = util.select().fromTo(6, 3, 1, 6, 3, 4)
                .add(util.select().fromTo(7, 1, 1, 7, 1, 4));
        Selection coolantTanks = util.select().position(BOILER_COLD_TANK).add(util.select().position(BOILER_HOT_TANK));
        scene.world().showSection(boiler.add(coolantLines).add(coolantTanks), Direction.DOWN);
        scene.idle(35);
        // 默认镜头从东北上方看入：同时移开北壁、东壁、顶盖和内部遮挡，端口不留在剖口中。
        // Ponder 1.0.82会重绘可见区，并将mask外邻块视为空气；无需改变模板或临时方块状态。
        scene.world().hideSection(cutaway.copy().add(coolantLines).add(coolantTanks), Direction.UP);
        scene.idle(15);
        Selection exchanger = util.select().position(7, 1, 7);
        Selection reheat = util.select().position(7, 3, 7);
        scene.overlay().showOutline(PonderPalette.RED, new Object(), exchanger, 115);
        note(scene, exchanger, util.vector().centerOf(7, 1, 7), 95,
                "锅炉底层的非边框位置放入换热器。");
        scene.overlay().showOutline(PonderPalette.BLUE, new Object(), reheat, 115);
        note(scene, reheat.add(exchanger), util.vector().centerOf(7, 3, 7), 95,
                "在上方隔层放置再加热段；有效热力回路数取两者数量较少的一方。");

        Selection ports = util.select().position(7, 1, 5).add(util.select().position(6, 3, 5));
        // 两段核心讲解及轮廓结束后才恢复近侧端口与管路；剖面壳体继续保持移开。
        scene.world().showSection(ports.copy().add(coolantLines).add(coolantTanks), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showBigLine(PonderPalette.RED, util.vector().centerOf(BOILER_HOT_TANK),
                util.vector().centerOf(7, 1, 5), 75);
        scene.overlay().showBigLine(PonderPalette.BLUE, util.vector().centerOf(6, 3, 5),
                util.vector().centerOf(BOILER_COLD_TANK), 75);
        note(scene, ports.copy().add(exchanger).add(reheat).add(coolantLines).add(coolantTanks),
                util.vector().centerOf(6, 3, 5), 85,
                "热液口与底层换热器同层，冷液口在隔层侧面；分别连接冷热液管路。");
        scene.world().showSection(cutaway.copy().substract(ports), Direction.DOWN);
        scene.idle(15);
        scene.markAsFinished();
    }

    /** 依次替换顶部工作盆和烧结炉，持续展示同一换热器及冷热管路。 */
    public static void nuclearHeatExchangerProcessing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("nuclear_heat_exchanger_processing", "核换热器：加工供热");
        scene.configureBasePlate(0, 0, 12);
        scene.scaleSceneView(0.9F);
        scene.showBasePlate();

        Selection loop = util.select().position(PROCESS_EXCHANGER)
                .add(util.select().fromTo(7, 1, 2, 7, 1, 4))
                .add(util.select().fromTo(7, 1, 6, 7, 1, 9))
                .add(util.select().position(CONDENSE_STEAM_TANK)).add(util.select().position(CONDENSE_WATER_TANK));
        Selection basin = util.select().position(PROCESS_BASIN).add(util.select().position(PROCESS_MIXER))
                .add(util.select().position(PROCESS_SHAFT));
        scene.world().showSection(loop.add(basin), Direction.DOWN);
        scene.idle(15);
        setTankFluid(scene, CONDENSE_STEAM_TANK,
                new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 1000));
        setTankFluid(scene, CONDENSE_WATER_TANK, FluidStack.EMPTY);
        scene.world().setBlock(PROCESS_EXCHANGER,
                HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState()
                        .setValue(NuclearHeatExchangerBlock.LIT, true), false);
        scene.world().setKineticSpeed(util.select().position(PROCESS_MIXER), 128);
        note(scene, loop.add(basin), util.vector().centerOf(PROCESS_BASIN), 85,
                "工作盆可持续超级加热，空盆也持续耗热；热液减少时等量冷液回流。");

        setTankFluid(scene, CONDENSE_STEAM_TANK,
                new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 750));
        setTankFluid(scene, CONDENSE_WATER_TANK,
                new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 250));
        scene.idle(35);

        scene.world().setKineticSpeed(util.select().position(PROCESS_MIXER), 0);
        scene.world().setBlock(PROCESS_BASIN, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(PROCESS_MIXER, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(PROCESS_SHAFT, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(PROCESS_BASIN,
                FuelProcessingContent.FUEL_SINTERING_FURNACE.get().defaultBlockState(), false);
        scene.idle(15);
        scene.world().setBlock(PROCESS_BASIN,
                FuelProcessingContent.FUEL_SINTERING_FURNACE.get().defaultBlockState()
                        .setValue(FuelSinteringBlock.LIT, true), false);
        setTankFluid(scene, CONDENSE_STEAM_TANK,
                new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 500));
        setTankFluid(scene, CONDENSE_WATER_TANK,
                new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 500));
        scene.idle(35);
        note(scene, loop.add(util.select().position(PROCESS_BASIN)), util.vector().centerOf(PROCESS_BASIN), 85,
                "空烧结炉已持续受热；投入生芯块后加工并产出烧结燃料芯块。");
        ElementLink<EntityElement> rawPellet = scene.world().createItemEntity(
                util.vector().centerOf(PROCESS_BASIN).add(0, 1.1, 0), Vec3.ZERO,
                new ItemStack(FuelProcessingContent.GREEN_FUEL_PELLET.get()));
        scene.world().modifyEntity(rawPellet, entity -> {
            if (entity instanceof ItemEntity item) {
                item.setNoGravity(true);
                item.setPickUpDelay(Integer.MAX_VALUE);
            }
        });
        scene.idle(35);
        scene.world().modifyEntity(rawPellet, Entity::discard);
        ElementLink<EntityElement> sinteredPellet = scene.world().createItemEntity(
                util.vector().centerOf(PROCESS_BASIN).add(0.65, 0.7, 0), Vec3.ZERO,
                new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get()));
        scene.world().modifyEntity(sinteredPellet, entity -> {
            if (entity instanceof ItemEntity item) {
                item.setNoGravity(true);
                item.setPickUpDelay(Integer.MAX_VALUE);
            }
        });
        setTankFluid(scene, CONDENSE_STEAM_TANK, FluidStack.EMPTY);
        setTankFluid(scene, CONDENSE_WATER_TANK,
                new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 1000));
        scene.idle(35);
        scene.world().setBlock(PROCESS_BASIN,
                FuelProcessingContent.FUEL_SINTERING_FURNACE.get().defaultBlockState(), false);
        scene.world().setBlock(PROCESS_EXCHANGER,
                HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState()
                        .setValue(NuclearHeatExchangerBlock.LIT, false), false);
        scene.idle(15);
        note(scene, loop.add(util.select().position(PROCESS_BASIN)), util.vector().centerOf(PROCESS_EXCHANGER), 85,
                "默认 4 mB/t；热液耗尽或冷液回流受阻时立即停热。");
        setTankFluid(scene, CONDENSE_STEAM_TANK,
                new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 1000));
        setTankFluid(scene, CONDENSE_WATER_TANK, FluidStack.EMPTY);
        scene.world().setBlock(PROCESS_EXCHANGER,
                HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState()
                        .setValue(NuclearHeatExchangerBlock.LIT, true), false);
        scene.world().setBlock(PROCESS_BASIN,
                FuelProcessingContent.FUEL_SINTERING_FURNACE.get().defaultBlockState()
                        .setValue(FuelSinteringBlock.LIT, true), false);
        scene.idle(35);
        setTankFluid(scene, CONDENSE_STEAM_TANK,
                new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 750));
        setTankFluid(scene, CONDENSE_WATER_TANK,
                new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 250));
        note(scene, loop.add(util.select().position(PROCESS_BASIN)), util.vector().centerOf(PROCESS_EXCHANGER), 85,
                "恢复冷热液循环后，炉体重新受热并继续加工。");
        scene.markAsFinished();
    }

    /** 展示普通蒸汽冷凝、有限冷源融化和水源蒸发后的蓝冰冷源。 */
    public static void nuclearHeatExchangerCondensation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("nuclear_heat_exchanger_condensation", "核换热器：蒸汽冷凝");
        scene.configureBasePlate(0, 0, 12);
        scene.scaleSceneView(0.9F);
        scene.showBasePlate();

        Selection machine = util.select().position(CONDENSE_EXCHANGER);
        // Ponder临时世界只替换已显示区域中的方块；先纳入冷源格，后续融水状态才可见。
        Selection coldSource = util.select().position(CONDENSE_SOURCE);
        Selection pipes = util.select().fromTo(7, 1, 2, 7, 1, 4)
                .add(util.select().fromTo(7, 1, 6, 7, 1, 9));
        Selection tanks = util.select().position(CONDENSE_STEAM_TANK).add(util.select().position(CONDENSE_WATER_TANK));
        scene.world().showSection(machine.add(pipes).add(tanks).add(coldSource), Direction.DOWN);
        scene.idle(15);
        setTankFluid(scene, CONDENSE_STEAM_TANK, new FluidStack(TurbineContent.STEAM.get(), 1000));
        setTankFluid(scene, CONDENSE_WATER_TANK, FluidStack.EMPTY);
        scene.world().setBlock(CONDENSE_SOURCE, Blocks.SNOW_BLOCK.defaultBlockState(), false);
        note(scene, machine.add(pipes), util.vector().centerOf(CONDENSE_EXCHANGER), 85,
                "蒸汽从后侧进入，冷凝水从前侧排出；默认蒸汽与回水按 1:1 变化。");

        scene.idle(35);
        scene.world().setBlock(CONDENSE_SOURCE, Blocks.ICE.defaultBlockState(), false);
        setTankFluid(scene, CONDENSE_STEAM_TANK, new FluidStack(TurbineContent.STEAM.get(), 750));
        setTankFluid(scene, CONDENSE_WATER_TANK, new FluidStack(Fluids.WATER, 250));
        scene.idle(40);
        scene.world().setBlock(CONDENSE_SOURCE, Blocks.PACKED_ICE.defaultBlockState(), false);
        setTankFluid(scene, CONDENSE_STEAM_TANK, new FluidStack(TurbineContent.STEAM.get(), 500));
        setTankFluid(scene, CONDENSE_WATER_TANK, new FluidStack(Fluids.WATER, 500));
        scene.idle(40);
        scene.world().setBlock(CONDENSE_SOURCE, Blocks.WATER.defaultBlockState(), false);
        setTankFluid(scene, CONDENSE_STEAM_TANK, new FluidStack(TurbineContent.STEAM.get(), 250));
        setTankFluid(scene, CONDENSE_WATER_TANK, new FluidStack(Fluids.WATER, 750));
        scene.idle(45);
        scene.world().setBlock(CONDENSE_SOURCE, Blocks.AIR.defaultBlockState(), false);
        setTankFluid(scene, CONDENSE_STEAM_TANK, FluidStack.EMPTY);
        setTankFluid(scene, CONDENSE_WATER_TANK, new FluidStack(Fluids.WATER, 1000));
        note(scene, coldSource.add(pipes), util.vector().centerOf(CONDENSE_SOURCE), 90,
                "顶部直接接触雪块、冰或浮冰即可冷凝并融水；水源用尽后蒸发消失。流水无效。");

        scene.world().setBlock(CONDENSE_SOURCE, Blocks.BLUE_ICE.defaultBlockState(), false);
        setTankFluid(scene, CONDENSE_STEAM_TANK, new FluidStack(TurbineContent.STEAM.get(), 1000));
        setTankFluid(scene, CONDENSE_WATER_TANK, FluidStack.EMPTY);
        scene.idle(35);
        setTankFluid(scene, CONDENSE_STEAM_TANK, new FluidStack(TurbineContent.STEAM.get(), 750));
        setTankFluid(scene, CONDENSE_WATER_TANK, new FluidStack(Fluids.WATER, 250));
        scene.idle(35);
        setTankFluid(scene, CONDENSE_STEAM_TANK, new FluidStack(TurbineContent.STEAM.get(), 500));
        setTankFluid(scene, CONDENSE_WATER_TANK, new FluidStack(Fluids.WATER, 500));
        note(scene, machine.add(coldSource).add(pipes),
                util.vector().centerOf(CONDENSE_SOURCE), 85,
                "蓝冰持续提供冷量且不会消耗；移除冷源或冷凝水回流受阻时，冷凝暂停。");
        scene.markAsFinished();
    }

    /**
     * 选出默认东北俯视镜头的两面近墙、顶盖与内部邻壳；坐标单位为模板方块格。
     * 仅供本幕客户端显隐使用，中心换热器和再加热段始终留在原位，远侧墙与底边保留层位参照。
     */
    private static Selection boilerCutaway(SceneBuildingUtil util) {
        Selection result = util.select().fromTo(5, 5, 5, 9, 5, 9)
                .add(util.select().fromTo(5, 1, 5, 9, 4, 5))
                .add(util.select().fromTo(9, 1, 6, 9, 4, 9));
        for (int x = 6; x <= 8; x++) for (int z = 6; z <= 8; z++) {
            if (x == 7 && z == 7) continue;
            result = result.add(util.select().position(x, 1, z)).add(util.select().position(x, 3, z));
        }
        return result;
    }

    /** Ponder虚拟世界不执行Create服务端组罐扫描，按原生组罐方块实体接口设置同一控制罐。 */
    private static void configureCreateBoilerTank(CreateSceneBuilder scene) {
        for (int x = 6; x <= 7; x++) for (int z = 5; z <= 6; z++) {
            BlockPos position = new BlockPos(x, 2, z);
            scene.world().modifyBlockEntity(position, FluidTankBlockEntity.class, tank -> {
                tank.setController(CREATE_TANK_CONTROLLER);
                tank.setWidth(2);
                tank.setHeight(1);
            });
        }
    }

    /** 用Create锅炉数据和蒸汽机输出BE建立实际的罐组—引擎—动力轴渲染关系。 */
    private static void setCreateBoilerPower(CreateSceneBuilder scene, boolean heated) {
        BlockPos shaftPosition = createShaftPosition();
        scene.world().modifyBlockEntity(CREATE_TANK_CONTROLLER, FluidTankBlockEntity.class, controller -> {
            controller.boiler.waterSupply = 10;
            controller.boiler.activeHeat = heated ? 1 : 0;
            controller.boiler.passiveHeat = false;
            controller.boiler.attachedEngines = 1;
            float efficiency = controller.boiler.getEngineEfficiency(controller.getTotalTankSize());
            if (controller.getLevel().getBlockEntity(shaftPosition) instanceof PoweredShaftBlockEntity shaft) {
                shaft.update(CREATE_ENGINE, 1, efficiency);
            }
            if (controller.getLevel().getBlockEntity(CREATE_ENGINE) instanceof SteamEngineBlockEntity engine) {
                engine.getShaft();
            }
        });
    }

    /** SceneBuilder状态与模板共用Create定义的轴位；水平蒸汽机需正交轴才能渲染往复活塞。 */
    private static BlockPos createShaftPosition() {
        var engineState = AllBlocks.STEAM_ENGINE.getDefaultState()
                .setValue(SteamEngineBlock.FACE, AttachFace.WALL)
                .setValue(SteamEngineBlock.FACING, Direction.WEST);
        return SteamEngineBlock.getShaftPos(engineState, CREATE_ENGINE);
    }

    /** 将外部供热阶段同步到两台换热器的可见灯态，只作用于Ponder场景方块状态。 */
    private static void setExchangerLit(CreateSceneBuilder scene, boolean lit) {
        BlockPos first = new BlockPos(7, 1, 5);
        BlockPos second = new BlockPos(7, 1, 6);
        scene.world().setBlock(first, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState()
                .setValue(NuclearHeatExchangerBlock.LIT, lit), false);
        scene.world().setBlock(second, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState()
                .setValue(NuclearHeatExchangerBlock.LIT, lit), false);
    }

    /** 展示罐内工质只改写 Ponder 临时 Create 储罐，不触发正式设备的端口事务。 */
    private static void setTankFluid(CreateSceneBuilder scene, BlockPos position, FluidStack fluid) {
        scene.world().modifyBlockEntity(position, FluidTankBlockEntity.class, tank -> {
            FluidTankBlockEntity controller = tank.getControllerBE();
            controller.getTankInventory().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            if (!fluid.isEmpty()) controller.getTankInventory().fill(fluid.copy(), IFluidHandler.FluidAction.EXECUTE);
        });
    }

    /** 正文锚定高亮目标；Ponder 1.0.82的正文寿命多10tick，额外留20tick确保两段不重叠。 */
    private static void note(CreateSceneBuilder scene, Selection target, net.minecraft.world.phys.Vec3 point,
                             int duration, String text) {
        scene.overlay().showOutlineWithText(target, duration).text(text)
                .attachKeyFrame().pointAt(point).placeNearTarget();
        scene.idle(duration + 20);
    }
}
