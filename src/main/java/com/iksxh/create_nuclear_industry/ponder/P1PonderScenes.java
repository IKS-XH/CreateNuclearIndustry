package com.iksxh.create_nuclear_industry.ponder;

import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ControlRodDriveBlockEntity;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;

import java.util.ArrayList;
import java.util.List;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.instruction.RotateSceneInstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** 反应堆四条 Ponder 客户端教学故事线；只操作各自的临时展示世界。 */
public final class P1PonderScenes {
    private static final BlockPos BUILD_DRIVE = new BlockPos(2, 4, 2);
    private static final BlockPos BUILD_INSTRUMENT = new BlockPos(2, 2, 0);
    private static final BlockPos BUILD_COLD_PORT = new BlockPos(1, 2, 0);
    private static final BlockPos BUILD_COLD_PORT_EXTRA = new BlockPos(1, 1, 0);
    private static final BlockPos BUILD_HOT_PORT = new BlockPos(3, 2, 0);
    private static final BlockPos BUILD_HOT_PORT_EXTRA = new BlockPos(3, 1, 0);

    private static final int REACTOR_OFFSET = 4;
    private static final BlockPos OP_DRIVE = reactorPos(2, 4, 2);
    private static final BlockPos OP_FUEL_PORT = reactorPos(2, 4, 1);
    private static final BlockPos OP_INSTRUMENT = reactorPos(2, 2, 0);
    private static final BlockPos OP_REDSTONE_LEVER = new BlockPos(6, 2, 3);
    private static final BlockPos OP_COLD_PORT = reactorPos(1, 2, 0);
    private static final BlockPos OP_HOT_PORT = reactorPos(3, 2, 0);
    private static final BlockPos OP_COLD_PUMP = new BlockPos(5, 2, 1);
    private static final BlockPos OP_HOT_PUMP = new BlockPos(7, 2, 1);
    private static final BlockPos OP_COLD_TANK = new BlockPos(5, 2, 0);
    private static final BlockPos OP_HOT_TANK = new BlockPos(7, 2, 0);
    private static final BlockPos FUEL_PORT = reactorPos(2, 4, 1);
    private static final BlockPos ARM = new BlockPos(6, 4, 3);
    private static final BlockPos ARM_INPUT = new BlockPos(4, 4, 3);
    private static final BlockPos ARM_OUTPUT = new BlockPos(8, 4, 3);

    private static final BlockPos RODS_TARGET_PORT = reactorPos(2, 4, 1);
    private static final BlockPos RODS_CENTER_PORT = reactorPos(2, 4, 2);
    private static final BlockPos RODS_WEST_PORT = reactorPos(1, 4, 2);
    private static final BlockPos RODS_EAST_PORT = reactorPos(3, 4, 2);
    private static final BlockPos RODS_SOUTH_PORT = reactorPos(2, 4, 3);
    private static final BlockPos RODS_UNCONTROLLED_PORT = reactorPos(3, 4, 3);
    private static final BlockPos RODS_WEST_DRIVE = reactorPos(1, 4, 1);
    private static final BlockPos RODS_EAST_DRIVE = reactorPos(3, 4, 1);
    private static final BlockPos RODS_REDSTONE_LEVER = new BlockPos(6, 2, 3);
    private P1PonderScenes() {
    }

    /** 展示自由列布局、多组冷热口及完整封壳顺序。 */
    public static void experimentalReactorBasics(SceneBuilder builder, SceneBuildingUtil util) {
        SceneBuilder scene = builder;
        scene.title("experimental_reactor", "Experimental Reactor: Build");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.8F);
        scene.showBasePlate();

        Selection innerColumns = util.select().fromTo(1, 1, 1, 3, 3, 3);
        Selection frame = util.select().fromTo(0, 1, 0, 0, 3, 0)
                .add(util.select().fromTo(0, 1, 4, 0, 3, 4))
                .add(util.select().fromTo(4, 1, 0, 4, 3, 0))
                .add(util.select().fromTo(4, 1, 4, 4, 3, 4));
        Selection primaryPorts = util.select().position(BUILD_INSTRUMENT)
                .add(util.select().position(BUILD_COLD_PORT))
                .add(util.select().position(BUILD_HOT_PORT));
        Selection extraCoolantPorts = util.select().position(BUILD_COLD_PORT_EXTRA)
                .add(util.select().position(BUILD_HOT_PORT_EXTRA));
        Selection fuelPorts = util.select().position(1, 4, 2)
                .add(util.select().position(2, 4, 1))
                .add(util.select().position(2, 4, 3))
                .add(util.select().position(3, 4, 2));
        Selection emptyCaps = util.select().position(1, 4, 1)
                .add(util.select().position(1, 4, 3))
                .add(util.select().position(3, 4, 1))
                .add(util.select().position(3, 4, 3));
        Selection fuelBodies = util.select().fromTo(2, 1, 1, 2, 3, 1)
                .add(util.select().fromTo(1, 1, 2, 1, 3, 2))
                .add(util.select().fromTo(3, 1, 2, 3, 3, 2))
                .add(util.select().fromTo(2, 1, 3, 2, 3, 3));
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(10);
        caption(scene, util, "Build the 5×5×5 casing frame; every edge must use reactor casing.",
                new BlockPos(0, 0, 0), 80);

        Selection walls = util.select().fromTo(0, 1, 0, 4, 3, 4).substract(innerColumns);
        scene.world().showSection(frame, Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(fuelBodies, Direction.DOWN);
        scene.world().showSection(fuelPorts, Direction.DOWN);
        scene.world().showSection(emptyCaps, Direction.DOWN);
        scene.world().showSection(util.select().position(BUILD_DRIVE), Direction.DOWN);
        scene.overlay().showOutline(PonderPalette.GREEN, new Object(), fuelPorts, 70);
        scene.overlay().showOutline(PonderPalette.BLUE, new Object(), util.select().position(BUILD_DRIVE), 70);
        scene.overlay().showOutline(PonderPalette.WHITE, new Object(), emptyCaps, 70);
        caption(scene, util, "Arrange fuel and control-rod columns freely; unused columns may stay empty.",
                new BlockPos(2, 2, 2), 80);

        scene.overlay().showOutline(PonderPalette.GREEN, new Object(), fuelBodies, 70);
        scene.overlay().showOutline(PonderPalette.GREEN, new Object(), fuelPorts, 70);
        caption(scene, util, "Stack three fuel rods vertically in each column and cap it with a refueling port.",
                new BlockPos(2, 2, 2), 80);

        scene.idle(10);
        for (BlockPos fuelColumn : new BlockPos[]{
                new BlockPos(2, 2, 1), new BlockPos(1, 2, 2),
                new BlockPos(3, 2, 2), new BlockPos(2, 2, 3)}) {
            scene.overlay().showLine(PonderPalette.GREEN,
                    util.vector().centerOf(BUILD_DRIVE), util.vector().centerOf(fuelColumn), 70);
        }
        scene.overlay().showOutline(PonderPalette.BLUE, new Object(), util.select().position(BUILD_DRIVE), 70);
        caption(scene, util, "Leave control-rod columns hollow and cap them with drives; each affects cardinal neighbors only.",
                BUILD_DRIVE, 80);

        scene.world().showSection(primaryPorts, Direction.SOUTH);
        scene.overlay().showOutline(PonderPalette.BLUE, new Object(), util.select().position(BUILD_INSTRUMENT), 70);
        scene.overlay().showOutline(PonderPalette.GREEN, new Object(),
                util.select().position(BUILD_COLD_PORT).add(util.select().position(BUILD_HOT_PORT)), 70);
        caption(scene, util, "Place one instrument port, one or more cold inlets and one or more hot outlets on non-edge sides.",
                BUILD_INSTRUMENT, 80);

        scene.world().showSection(util.select().position(BUILD_COLD_PORT_EXTRA), Direction.SOUTH);
        scene.idle(10);
        scene.world().showSection(util.select().position(BUILD_HOT_PORT_EXTRA), Direction.SOUTH);
        scene.overlay().showOutline(PonderPalette.BLUE, new Object(), extraCoolantPorts, 70);
        caption(scene, util, "Multiple cold and hot ports are allowed; place them on valid side blocks to suit the pipe layout.",
                BUILD_COLD_PORT_EXTRA, 80);

        scene.world().showSection(walls.substract(frame).substract(primaryPorts).substract(extraCoolantPorts),
                Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().layer(4), Direction.DOWN);
        scene.idle(10);
        caption(scene, util, "Seal every outside wall. Windows may replace casing on non-edge side blocks.",
                BUILD_INSTRUMENT, 80);

        scene.overlay().showControls(util.vector().topOf(BUILD_INSTRUMENT), Pointing.DOWN, 70)
                .withItem(AllItems.WRENCH.asStack())
                .rightClick();
        caption(scene, util, "Check the instrument port with a Create Wrench. Load fuel only after the structure forms.",
                BUILD_INSTRUMENT, 80);
        scene.markAsFinished();
    }

    /** 以俯视剖面展示已装料列、四向反馈、控制棒平均深度和SCRAM边界。 */
    public static void experimentalReactorRods(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("experimental_reactor_rods", "Experimental Reactor: Fuel and Control Rods");
        scene.configureBasePlate(0, 0, 13);
        scene.scaleSceneView(0.74F);
        // 锁定 Ponder 1.0.82 的相机指令：俯视剖面分离四向邻接与对角位置。
        scene.addInstruction(new RotateSceneInstruction(-65, 145, false));
        scene.showBasePlate();

        Selection core = util.select().fromTo(5, 0, 5, 7, 4, 7);
        Selection activePorts = util.select().position(RODS_TARGET_PORT)
                .add(util.select().position(RODS_CENTER_PORT))
                .add(util.select().position(RODS_WEST_PORT))
                .add(util.select().position(RODS_EAST_PORT))
                .add(util.select().position(RODS_SOUTH_PORT))
                .add(util.select().position(RODS_UNCONTROLLED_PORT));
        Selection rodDrives = util.select().position(RODS_WEST_DRIVE)
                .add(util.select().position(RODS_EAST_DRIVE));
        scene.world().showSection(core, Direction.DOWN);
        scene.world().showSection(activePorts.add(rodDrives), Direction.DOWN);
        scene.world().showSection(util.select().position(RODS_REDSTONE_LEVER), Direction.SOUTH);

        // 剖面只显露内部；仪表端口仍为前侧红石拉杆提供附着面。
        scene.world().showSection(util.select().position(OP_INSTRUMENT), Direction.SOUTH);
        ItemStack freshFuel = new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get());
        List<ElementLink<EntityElement>> uncontrolledFuelIcons = new ArrayList<>();
        uncontrolledFuelIcons.add(showPonderFuelAssembly(scene, util, RODS_CENTER_PORT, freshFuel));
        scene.idle(15);
        scene.overlay().showOutline(PonderPalette.GREEN, new Object(), util.select().position(RODS_CENTER_PORT), 70);
        scene.overlay().showOutline(PonderPalette.BLUE, new Object(), rodDrives, 70);
        caption(scene, util, "Fuel and control columns can be placed in different positions and numbers; layout sets heat and control coverage.",
                RODS_CENTER_PORT, 80);

        // 先只有中心列装料，再加入北侧列与对角列，对比四向增强和对角无反馈。
        showPonderFuelAssembly(scene, util, RODS_TARGET_PORT, freshFuel);
        uncontrolledFuelIcons.add(showPonderFuelAssembly(scene, util, RODS_UNCONTROLLED_PORT, freshFuel));
        scene.overlay().showLine(PonderPalette.GREEN,
                util.vector().centerOf(RODS_CENTER_PORT), util.vector().centerOf(RODS_TARGET_PORT), 70);
        scene.overlay().showLine(PonderPalette.BLUE,
                util.vector().centerOf(RODS_CENTER_PORT),
                util.vector().centerOf(RODS_UNCONTROLLED_PORT), 70);
        caption(scene, util, "Loaded fuel columns strengthen one another across cardinal neighbors; diagonal neighbors do not interact.",
                RODS_CENTER_PORT, 80);

        // 加入其余三根四向邻列，让中心列的反馈连接由一条增加到四条。
        showPonderFuelAssembly(scene, util, RODS_WEST_PORT, freshFuel);
        showPonderFuelAssembly(scene, util, RODS_EAST_PORT, freshFuel);
        uncontrolledFuelIcons.add(showPonderFuelAssembly(scene, util, RODS_SOUTH_PORT, freshFuel));
        showFuelAdjacencyLines(scene, util, PonderPalette.GREEN, 70);
        showLoadedCoreMarkers(scene, util, PonderPalette.GREEN, 70);
        caption(scene, util, "Overclocking raises heat and fuel use; more neighboring fuel columns strengthen the feedback.",
                RODS_CENTER_PORT, 80);

        scene.overlay().showLine(PonderPalette.BLUE,
                util.vector().centerOf(RODS_WEST_DRIVE), util.vector().centerOf(RODS_TARGET_PORT), 70);
        scene.overlay().showLine(PonderPalette.BLUE,
                util.vector().centerOf(RODS_EAST_DRIVE), util.vector().centerOf(RODS_TARGET_PORT), 70);
        setPonderRodSlider(scene, RODS_WEST_DRIVE, 40);
        setPonderRodSlider(scene, RODS_EAST_DRIVE, 40);
        scene.overlay().showScrollInput(util.vector().topOf(RODS_WEST_DRIVE), Direction.UP, 70);
        scene.overlay().showScrollInput(util.vector().topOf(RODS_EAST_DRIVE), Direction.UP, 70);
        caption(scene, util, "Control rods limit only cardinally adjacent fuel columns; deeper insertion means lower power.",
                RODS_TARGET_PORT, 80);

        setPonderRodSlider(scene, RODS_WEST_DRIVE, 100);
        setPonderRodSlider(scene, RODS_EAST_DRIVE, 40);
        scene.overlay().showScrollInput(util.vector().topOf(RODS_WEST_DRIVE), Direction.UP, 70);
        scene.overlay().showScrollInput(util.vector().topOf(RODS_EAST_DRIVE), Direction.UP, 70);
        scene.overlay().showLine(PonderPalette.RED,
                util.vector().centerOf(RODS_TARGET_PORT), util.vector().topOf(RODS_TARGET_PORT), 70);
        caption(scene, util, "With multiple neighboring rods, their insertion depths are averaged; one fully inserted rod is not enough to stop the column.",
                RODS_TARGET_PORT, 80);

        setPonderRodSlider(scene, RODS_WEST_DRIVE, 100);
        setPonderRodSlider(scene, RODS_EAST_DRIVE, 100);
        scene.overlay().showScrollInput(util.vector().topOf(RODS_WEST_DRIVE), Direction.UP, 70);
        scene.overlay().showScrollInput(util.vector().topOf(RODS_EAST_DRIVE), Direction.UP, 70);
        scene.effects().indicateRedstone(RODS_REDSTONE_LEVER);
        scene.world().toggleRedstonePower(util.select().position(RODS_REDSTONE_LEVER));
        // 两棒均到底后，北、西、东三列停止；中心、南侧和角列没有相邻控制棒。
        for (BlockPos controlledPort : new BlockPos[]{RODS_TARGET_PORT, RODS_WEST_PORT, RODS_EAST_PORT}) {
            scene.overlay().showOutline(PonderPalette.GREEN, new Object(), util.select().position(controlledPort), 70);
        }
        // 红色标记覆盖后续三段正文和等待镜头，在枯竭组件出现前持续保留。
        showUncontrolledCoreMarkers(scene, util, PonderPalette.RED, 340);
        caption(scene, util, "Insert all neighboring control rods to stop that fuel column; redstone shutdown works through rod insertion.",
                RODS_TARGET_PORT, 80);

        showUncontrolledCoreMarkers(scene, util, PonderPalette.RED, 70);
        scene.overlay().showControls(util.vector().topOf(RODS_UNCONTROLLED_PORT), Pointing.DOWN, 70)
                .withItem(ItemStack.EMPTY)
                .rightClick();
        caption(scene, util, "A loaded column with no neighboring control rod runs at full power, even during SCRAM.",
                RODS_UNCONTROLLED_PORT, 80);

        showUncontrolledCoreMarkers(scene, util, PonderPalette.RED, 70);
        caption(scene, util, "Rods and redstone cannot stop these columns or remove fuel while it produces heat; wait for exhaustion.",
                RODS_UNCONTROLLED_PORT, 80);
        scene.idle(40);
        for (ElementLink<EntityElement> fuelIcon : uncontrolledFuelIcons) {
            scene.world().modifyEntity(fuelIcon, Entity::discard);
        }
        ItemStack cooledSpentFuel = new ItemStack(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get());
        setPonderFuelPortDisplay(scene, RODS_CENTER_PORT, cooledSpentFuel);
        setPonderFuelPortDisplay(scene, RODS_SOUTH_PORT, cooledSpentFuel);
        setPonderFuelPortDisplay(scene, RODS_UNCONTROLLED_PORT, cooledSpentFuel);
        showPonderFuelAssembly(scene, util, RODS_CENTER_PORT, cooledSpentFuel);
        showPonderFuelAssembly(scene, util, RODS_SOUTH_PORT, cooledSpentFuel);
        showPonderFuelAssembly(scene, util, RODS_UNCONTROLLED_PORT, cooledSpentFuel);
        scene.idle(40);
        scene.markAsFinished();
    }

    /** 展示受控运行、冷却回路、红石停堆和撤去信号后的目标恢复。 */
    public static void experimentalReactorOperation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("experimental_reactor_operation", "Experimental Reactor: Run and Stop");
        scene.configureBasePlate(0, 0, 13);
        scene.scaleSceneView(0.63F);
        scene.showBasePlate();

        setTankFluid(scene, OP_COLD_TANK,
                new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 4000));
        setTankFluid(scene, OP_HOT_TANK, FluidStack.EMPTY);
        scene.world().showSection(util.select().fromTo(4, 0, 4, 8, 4, 8), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(5, 2, 0, 5, 2, 3), Direction.SOUTH);
        scene.world().showSection(util.select().fromTo(7, 2, 0, 7, 2, 3), Direction.SOUTH);
        scene.world().showSection(util.select().fromTo(6, 2, 0, 6, 2, 1), Direction.SOUTH);
        scene.world().showSection(util.select().fromTo(8, 2, 0, 8, 2, 1), Direction.SOUTH);
        scene.world().showSection(util.select().position(OP_REDSTONE_LEVER), Direction.SOUTH);
        ItemStack loadedFuel = new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get());
        setPonderFuelPortDisplay(scene, OP_FUEL_PORT, loadedFuel);
        var displayedFuel = scene.world().createItemEntity(
                util.vector().centerOf(OP_FUEL_PORT).add(0, 1, 0), Vec3.ZERO, loadedFuel);
        scene.world().modifyEntity(displayedFuel, entity -> {
            if (entity instanceof ItemEntity itemEntity) {
                itemEntity.setNoGravity(true);
                itemEntity.setPickUpDelay(Integer.MAX_VALUE);
            }
        });
        scene.idle(15);

        Selection coldDrive = util.select().fromTo(6, 2, 0, 6, 2, 1);
        Selection hotDrive = util.select().fromTo(8, 2, 0, 8, 2, 1);
        scene.world().setKineticSpeed(coldDrive, 128);
        scene.world().setKineticSpeed(hotDrive, 128);
        scene.world().setKineticSpeed(util.select().position(OP_COLD_PUMP), -128);
        scene.world().setKineticSpeed(util.select().position(OP_HOT_PUMP), -128);
        scene.effects().rotationSpeedIndicator(OP_COLD_PUMP);
        scene.effects().rotationSpeedIndicator(OP_HOT_PUMP);
        scene.overlay().showBigLine(PonderPalette.BLUE,
                util.vector().topOf(OP_COLD_TANK), util.vector().topOf(OP_COLD_PORT), 70);
        scene.overlay().showBigLine(PonderPalette.RED,
                util.vector().topOf(OP_HOT_PORT), util.vector().topOf(OP_HOT_TANK), 70);
        caption(scene, util, "Before starting, connect the coolant loop and confirm the fuel is loaded.",
                OP_COLD_PORT, 80);

        scene.overlay().showScrollInput(util.vector().topOf(OP_DRIVE), Direction.UP, 80);
        showFuelControlMarkers(scene, util, PonderPalette.GREEN, 70);
        caption(scene, util, "Raise the rods gradually to start fission. Deeper insertion means lower power.",
                OP_DRIVE, 80);

        setTankFluid(scene, OP_COLD_TANK,
                new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 2500));
        setTankFluid(scene, OP_HOT_TANK,
                new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 1500));
        scene.overlay().showBigLine(PonderPalette.BLUE,
                util.vector().topOf(OP_COLD_TANK), util.vector().topOf(OP_COLD_PORT), 70);
        scene.overlay().showBigLine(PonderPalette.RED,
                util.vector().topOf(OP_HOT_PORT), util.vector().topOf(OP_HOT_TANK), 70);
        caption(scene, util, "Cold coolant absorbs heat and leaves through the hot port. Keep both directions open.",
                OP_HOT_PORT, 80);

        scene.overlay().showControls(util.vector().topOf(OP_INSTRUMENT), Pointing.DOWN, 70)
                .withItem(AllItems.GOGGLES.asStack());
        caption(scene, util, "Engineer Goggles show heat production, cooling rate, and coolant stock at the instrument port.",
                OP_INSTRUMENT, 80);

        scene.effects().indicateRedstone(OP_REDSTONE_LEVER);
        scene.world().toggleRedstonePower(util.select().position(OP_REDSTONE_LEVER));
        showFuelControlMarkers(scene, util, PonderPalette.RED, 70);
        caption(scene, util, "Fully insert the control rods to stop. A redstone signal at the instrument port inserts every movable rod.",
                OP_INSTRUMENT, 80);

        setTankFluid(scene, OP_COLD_TANK,
                new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 1000));
        setTankFluid(scene, OP_HOT_TANK,
                new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 3000));
        caption(scene, util, "Fission stops before residual heat does. Keep coolant circulating until the core cools.",
                OP_HOT_TANK, 80);

        scene.world().toggleRedstonePower(util.select().position(OP_REDSTONE_LEVER));
        scene.overlay().showScrollInput(util.vector().topOf(OP_DRIVE), Direction.UP, 80);
        showFuelControlMarkers(scene, util, PonderPalette.GREEN, 70);
        caption(scene, util, "When the signal is removed, the control rod returns to its previous target depth.",
                OP_DRIVE, 80);
        scene.markAsFinished();
    }

    /** 展示手动装料、停裂变后取出，以及使用 Create 机械臂搬运燃料组件。 */
    public static void experimentalReactorRefueling(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("experimental_reactor_refueling", "Experimental Reactor: Load and Replace Fuel");
        scene.configureBasePlate(0, 0, 13);
        scene.scaleSceneView(0.63F);
        scene.showBasePlate();

        scene.world().showSection(util.select().fromTo(4, 0, 4, 8, 4, 8), Direction.DOWN);
        setPonderFuelPortDisplay(scene, FUEL_PORT, ItemStack.EMPTY);
        configureRefuelingArmTargets(scene);
        scene.world().showSection(util.select().fromTo(4, 3, 3, 8, 4, 3), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().position(ARM), 128);
        scene.effects().rotationSpeedIndicator(ARM);
        scene.idle(15);
        scene.overlay().showOutlineWithText(util.select().position(FUEL_PORT), 80)
                .text("Each refueling port serves the fuel column directly below it and holds one assembly.")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(100);

        ItemStack freshFuel = new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get());
        var displayedFreshFuel = scene.world().createItemEntity(
                util.vector().centerOf(FUEL_PORT).add(0, 1, 0), Vec3.ZERO, freshFuel);
        scene.world().modifyEntity(displayedFreshFuel, entity -> {
            if (entity instanceof ItemEntity itemEntity) {
                itemEntity.setNoGravity(true);
                itemEntity.setPickUpDelay(Integer.MAX_VALUE);
            }
        });
        setPonderFuelPortDisplay(scene, FUEL_PORT, freshFuel);
        scene.overlay().showControls(util.vector().topOf(FUEL_PORT), Pointing.DOWN, 70)
                .withItem(freshFuel)
                .rightClick();
        scene.overlay().showOutlineWithText(util.select().position(FUEL_PORT), 80)
                .text("With a formed reactor and a non-fissioning column, right-click the port with a fresh enriched uranium fuel assembly.")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(FUEL_PORT))
                .placeNearTarget();
        scene.idle(100);

        scene.overlay().showOutlineWithText(util.select().position(FUEL_PORT), 80)
                .text("After the fuel is exhausted, it becomes a depleted uranium fuel assembly.")
                .attachKeyFrame()
                .placeNearTarget();
        ItemStack cooledSpentFuel = new ItemStack(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get());
        setPonderFuelPortDisplay(scene, FUEL_PORT, cooledSpentFuel);
        var displayedSpentFuel = scene.world().createItemEntity(
                util.vector().centerOf(FUEL_PORT).add(0, 1, 0), Vec3.ZERO, cooledSpentFuel);
        scene.world().modifyEntity(displayedFreshFuel, Entity::discard);
        scene.world().modifyEntity(displayedSpentFuel, entity -> {
            if (entity instanceof ItemEntity itemEntity) {
                itemEntity.setNoGravity(true);
                itemEntity.setPickUpDelay(Integer.MAX_VALUE);
            }
        });
        scene.idle(100);

        scene.overlay().showScrollInput(util.vector().topOf(OP_DRIVE), Direction.UP, 70);
        showFuelControlMarkers(scene, util, PonderPalette.RED, 70);
        scene.overlay().showOutlineWithText(util.select().position(FUEL_PORT), 80)
                .text("Insert the neighboring control rod and wait for fission to stop in the target column. Keep cooling residual heat.")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(FUEL_PORT))
                .placeNearTarget();
        scene.idle(100);

        scene.overlay().showControls(util.vector().topOf(FUEL_PORT), Pointing.DOWN, 40)
                .rightClick();
        setPonderFuelPortDisplay(scene, FUEL_PORT, ItemStack.EMPTY);
        scene.world().modifyEntity(displayedSpentFuel, Entity::discard);
        scene.idle(50);
        scene.overlay().showControls(util.vector().topOf(FUEL_PORT), Pointing.DOWN, 40)
                .withItem(freshFuel)
                .rightClick();
        var replacementFuel = scene.world().createItemEntity(
                util.vector().centerOf(FUEL_PORT).add(0, 1, 0), Vec3.ZERO, freshFuel);
        scene.world().modifyEntity(replacementFuel, entity -> {
            if (entity instanceof ItemEntity itemEntity) {
                itemEntity.setNoGravity(true);
                itemEntity.setPickUpDelay(Integer.MAX_VALUE);
            }
        });
        setPonderFuelPortDisplay(scene, FUEL_PORT, freshFuel);
        scene.overlay().showOutlineWithText(util.select().position(FUEL_PORT), 80)
                .text("Take the old assembly with an empty hand, then load a fresh one.")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(FUEL_PORT))
                .placeNearTarget();
        scene.idle(100);

        scene.world().modifyEntity(replacementFuel, Entity::discard);
        setPonderFuelPortDisplay(scene, FUEL_PORT, ItemStack.EMPTY);
        scene.overlay().showControls(util.vector().topOf(ARM_INPUT), Pointing.RIGHT, 50)
                .withItem(freshFuel);
        scene.world().createItemOnBeltLike(ARM_INPUT, Direction.UP, freshFuel);
        scene.world().instructArm(ARM, ArmBlockEntity.Phase.MOVE_TO_INPUT, ItemStack.EMPTY, 0);
        scene.idle(24);
        scene.world().removeItemsFromBelt(ARM_INPUT);
        scene.world().instructArm(ARM, ArmBlockEntity.Phase.SEARCH_OUTPUTS, freshFuel, -1);
        scene.world().instructArm(ARM, ArmBlockEntity.Phase.MOVE_TO_OUTPUT, freshFuel, 0);
        scene.idle(24);
        setPonderFuelPortDisplay(scene, FUEL_PORT, freshFuel);
        scene.world().instructArm(ARM, ArmBlockEntity.Phase.SEARCH_INPUTS, ItemStack.EMPTY, -1);

        setPonderFuelPortDisplay(scene, FUEL_PORT, cooledSpentFuel);
        scene.world().instructArm(ARM, ArmBlockEntity.Phase.MOVE_TO_INPUT, ItemStack.EMPTY, 1);
        scene.idle(24);
        scene.world().instructArm(ARM, ArmBlockEntity.Phase.SEARCH_OUTPUTS, cooledSpentFuel, -1);
        scene.world().instructArm(ARM, ArmBlockEntity.Phase.MOVE_TO_OUTPUT, cooledSpentFuel, 1);
        scene.idle(24);
        scene.world().createItemOnBeltLike(ARM_OUTPUT, Direction.UP, cooledSpentFuel);
        scene.world().instructArm(ARM, ArmBlockEntity.Phase.SEARCH_INPUTS, ItemStack.EMPTY, -1);
        scene.overlay().showOutlineWithText(
                        util.select().position(ARM_INPUT).add(util.select().position(ARM_OUTPUT)), 80)
                .text("Power a Create Mechanical Arm to take fresh fuel from supply and deliver spent fuel to receiving storage. Hoppers cannot access the fuel port.")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(100);
        scene.markAsFinished();
    }

    /** 在 Ponder 客户端临时世界中显示一件组件，不触发物品交互或正式事务。 */
    private static ElementLink<EntityElement> showPonderFuelAssembly(
            CreateSceneBuilder scene,
            SceneBuildingUtil util,
            BlockPos port,
            ItemStack assembly
    ) {
        setPonderFuelPortDisplay(scene, port, assembly);
        ElementLink<EntityElement> icon = scene.world().createItemEntity(
                util.vector().centerOf(port).add(0, 1, 0), Vec3.ZERO, assembly.copy());
        scene.world().modifyEntity(icon, entity -> {
            if (entity instanceof ItemEntity itemEntity) {
                itemEntity.setNoGravity(true);
                itemEntity.setPickUpDelay(Integer.MAX_VALUE);
            }
        });
        return icon;
    }

    /** 只调整 Ponder 临时驱动器的 Create 滑块显示值，不发控制网络请求。 */
    private static void setPonderRodSlider(CreateSceneBuilder scene, BlockPos drivePos, int depthPercent) {
        scene.world().modifyBlockEntity(drivePos, ControlRodDriveBlockEntity.class, drive -> {
            if (drive.slider() != null) {
                drive.slider().setClientDisplayedValue(depthPercent);
            }
        });
    }

    /** 给剖面中实际装料的列加竖直绿色标记。 */
    private static void showLoadedCoreMarkers(
            CreateSceneBuilder scene,
            SceneBuildingUtil util,
            PonderPalette color,
            int duration
    ) {
        for (BlockPos port : new BlockPos[]{
                RODS_TARGET_PORT, RODS_CENTER_PORT, RODS_WEST_PORT,
                RODS_EAST_PORT, RODS_SOUTH_PORT, RODS_UNCONTROLLED_PORT}) {
            scene.overlay().showLine(color,
                    util.vector().centerOf(port), util.vector().topOf(port), duration);
        }
    }

    /** 显示中心燃料列与其四向相邻装料列之间的反馈方向。 */
    private static void showFuelAdjacencyLines(
            CreateSceneBuilder scene,
            SceneBuildingUtil util,
            PonderPalette color,
            int duration
    ) {
        for (BlockPos neighbor : new BlockPos[]{
                RODS_TARGET_PORT, RODS_WEST_PORT, RODS_EAST_PORT, RODS_SOUTH_PORT}) {
            scene.overlay().showLine(color,
                    util.vector().centerOf(RODS_CENTER_PORT),
                    util.vector().centerOf(neighbor), duration);
        }
    }

    /** 标出没有任何四向相邻控制棒、因此不能被SCRAM插棒动作停止的装料列。 */
    private static void showUncontrolledCoreMarkers(
            CreateSceneBuilder scene,
            SceneBuildingUtil util,
            PonderPalette color,
            int duration
    ) {
        for (BlockPos port : new BlockPos[]{
                RODS_CENTER_PORT, RODS_SOUTH_PORT, RODS_UNCONTROLLED_PORT}) {
            Selection column = util.select().fromTo(port.getX(), 1, port.getZ(),
                    port.getX(), 4, port.getZ());
            scene.overlay().showOutline(color, new Object(), column, duration);
            scene.overlay().showLine(color,
                    util.vector().centerOf(port).add(0, -3, 0), util.vector().topOf(port), duration);
        }
    }

    /** 以彩色临时标记强调中心控制棒只作用于四向相邻燃料列。 */
    private static void showFuelControlMarkers(
            CreateSceneBuilder scene,
            SceneBuildingUtil util,
            PonderPalette color,
            int duration
    ) {
        Vec3Target[] targets = {
                new Vec3Target(6, 2, 5),
                new Vec3Target(5, 2, 6),
                new Vec3Target(7, 2, 6),
                new Vec3Target(6, 2, 7),
        };
        for (Vec3Target target : targets) {
            scene.overlay().showLine(color,
                    util.vector().centerOf(OP_DRIVE),
                    util.vector().centerOf(target.x(), target.y(), target.z()), duration);
        }
    }

    /** 在 Ponder 临时 Create 储罐中直接调整可见流体，不走正式反应堆流体事务。 */
    private static void setTankFluid(CreateSceneBuilder scene, BlockPos position, FluidStack fluid) {
        scene.world().modifyBlockEntity(position, FluidTankBlockEntity.class, tank -> {
            tank.getTankInventory().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            if (!fluid.isEmpty()) {
                tank.getTankInventory().fill(fluid.copy(), IFluidHandler.FluidAction.EXECUTE);
            }
        });
    }

    /** 仅为 Ponder 临时端口写入客户端可见的绑定和组件数据，不调用绑定或换料事务。 */
    private static void setPonderFuelPortDisplay(SceneBuilder scene, BlockPos position, ItemStack assembly) {
        scene.world().modifyBlockEntity(position, ReactorPortBlockEntity.class, port -> {
            var level = port.getLevel();
            if (level == null) {
                return;
            }
            var registries = level.registryAccess();
            CompoundTag tag = port.saveWithFullMetadata(registries);
            tag.putBoolean("FuelColumnBound", true);
            tag.putInt("FuelColumnPositionX", position.getX() - REACTOR_OFFSET - 1);
            tag.putInt("FuelColumnPositionZ", position.getZ() - REACTOR_OFFSET - 1);
            tag.putDouble("FuelColumnIntegrity", 1.0);
            tag.putDouble("FuelColumnHeatHuPerTick", 0.0);
            tag.put("FuelAssembly", assembly.saveOptional(registries));
            port.readClient(tag, registries);
        });
    }

    /** 用 Create 自身序列化器为客户端机械臂装配供料、端口和接收目标。 */
    private static void configureRefuelingArmTargets(SceneBuilder scene) {
        scene.world().modifyBlockEntity(ARM, ArmBlockEntity.class, arm -> {
            var level = arm.getLevel();
            if (level == null) {
                return;
            }
            ArmInteractionPoint fuelSupply = createArmPoint(arm, ARM_INPUT, true);
            ArmInteractionPoint refuelingInput = createArmPoint(arm, FUEL_PORT, true);
            ArmInteractionPoint refuelingOutput = createArmPoint(arm, FUEL_PORT, false);
            ArmInteractionPoint spentFuelReceiver = createArmPoint(arm, ARM_OUTPUT, false);

            ListTag targets = new ListTag();
            targets.add(fuelSupply.serialize(ARM));
            targets.add(refuelingInput.serialize(ARM));
            targets.add(refuelingOutput.serialize(ARM));
            targets.add(spentFuelReceiver.serialize(ARM));

            var registries = level.registryAccess();
            CompoundTag tag = arm.saveWithFullMetadata(registries);
            tag.put("InteractionPoints", targets);
            arm.readClient(tag, registries);
        });
    }

    /** 将 Create 目标设置为取料或放料模式；Ponder 客户端不会执行物品事务。 */
    private static ArmInteractionPoint createArmPoint(
            ArmBlockEntity arm,
            BlockPos target,
            boolean input
    ) {
        var level = arm.getLevel();
        if (level == null) {
            throw new IllegalStateException("Ponder 机械臂缺少临时世界");
        }
        ArmInteractionPoint point = ArmInteractionPoint.create(level, target, level.getBlockState(target));
        if (point == null) {
            throw new IllegalStateException("Ponder 机械臂目标无效: " + target);
        }
        if (input) {
            point.cycleMode();
        }
        return point;
    }

    /** 在每条故事线中给正文留出超过动画寿命的退场间隔。 */
    private static void caption(
            SceneBuilder scene,
            SceneBuildingUtil util,
            String text,
            BlockPos target,
            int duration
    ) {
        scene.overlay().showText(duration)
                .text(text)
                .attachKeyFrame()
                .pointAt(util.vector().topOf(target))
                .placeNearTarget();
        scene.idle(duration + 20);
    }

    /** 将相对反应堆局部坐标映射到13×5×13故事线模板中的完整堆体。 */
    private static BlockPos reactorPos(int x, int y, int z) {
        return new BlockPos(x + REACTOR_OFFSET, y, z + REACTOR_OFFSET);
    }

    /** 控制棒标记只保存坐标，不关联实际方块或反应堆状态。 */
    private record Vec3Target(int x, int y, int z) {
    }
}
