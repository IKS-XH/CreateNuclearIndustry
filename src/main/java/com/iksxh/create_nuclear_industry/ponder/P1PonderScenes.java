package com.iksxh.create_nuclear_industry.ponder;

import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
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

/** 反应堆三条 Ponder 客户端教学故事线；只操作各自的临时展示世界。 */
public final class P1PonderScenes {
    private static final BlockPos BUILD_DRIVE = new BlockPos(2, 4, 2);
    private static final BlockPos BUILD_INSTRUMENT = new BlockPos(2, 2, 0);
    private static final BlockPos BUILD_COLD_PORT = new BlockPos(1, 2, 4);
    private static final BlockPos BUILD_HOT_PORT = new BlockPos(3, 2, 4);

    private static final int REACTOR_OFFSET = 4;
    private static final BlockPos OP_DRIVE = reactorPos(2, 4, 2);
    private static final BlockPos OP_FUEL_PORT = reactorPos(2, 4, 1);
    private static final BlockPos OP_INSTRUMENT = reactorPos(2, 2, 0);
    private static final BlockPos OP_REDSTONE_LEVER = new BlockPos(6, 2, 3);
    private static final BlockPos OP_COLD_PORT = reactorPos(1, 2, 4);
    private static final BlockPos OP_HOT_PORT = reactorPos(3, 2, 4);
    private static final BlockPos OP_COLD_PUMP = new BlockPos(5, 2, 11);
    private static final BlockPos OP_HOT_PUMP = new BlockPos(7, 2, 11);
    private static final BlockPos OP_COLD_TANK = new BlockPos(5, 2, 12);
    private static final BlockPos OP_HOT_TANK = new BlockPos(7, 2, 12);
    private static final BlockPos FUEL_PORT = reactorPos(2, 4, 1);
    private static final BlockPos ARM = new BlockPos(6, 4, 3);
    private static final BlockPos ARM_INPUT = new BlockPos(4, 4, 3);
    private static final BlockPos ARM_OUTPUT = new BlockPos(8, 4, 3);

    private P1PonderScenes() {
    }

    /** 展示外壳、内部列、侧面端口和最终封顶顺序。 */
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
        Selection sidePorts = util.select().position(BUILD_INSTRUMENT)
                .add(util.select().position(BUILD_COLD_PORT))
                .add(util.select().position(BUILD_HOT_PORT));
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(10);
        caption(scene, util, "Build the 5×5 base and casing frame. Edges must use casing.",
                new BlockPos(0, 0, 0), 80);

        Selection walls = util.select().fromTo(0, 1, 0, 4, 3, 4).substract(innerColumns);
        scene.world().showSection(frame, Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(innerColumns, Direction.DOWN);
        Selection fuelPorts = util.select().position(1, 4, 2)
                .add(util.select().position(2, 4, 1))
                .add(util.select().position(2, 4, 3))
                .add(util.select().position(3, 4, 2));
        scene.world().showSection(fuelPorts, Direction.DOWN);
        caption(scene, util, "Each fuel column has three fuel-rod blocks below its refueling port.",
                new BlockPos(2, 2, 1), 80);

        scene.world().showSection(util.select().position(BUILD_DRIVE), Direction.DOWN);
        scene.idle(10);
        for (BlockPos fuelColumn : new BlockPos[]{
                new BlockPos(2, 2, 1), new BlockPos(1, 2, 2),
                new BlockPos(3, 2, 2), new BlockPos(2, 2, 3)}) {
            scene.overlay().showLine(PonderPalette.GREEN,
                    util.vector().centerOf(BUILD_DRIVE), util.vector().centerOf(fuelColumn), 70);
        }
        scene.overlay().showOutlineWithText(util.select().position(BUILD_DRIVE), 80)
                .text("Leave the control column hollow and cap it with a drive. It controls the four neighboring fuel columns.")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(110);

        scene.world().showSection(sidePorts, Direction.DOWN);
        scene.overlay().showOutlineWithText(sidePorts, 90)
                .text("Place one instrument port, at least one cold port, and one hot port on non-edge side blocks.")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(110);

        scene.world().showSection(walls.substract(frame).substract(sidePorts), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().layer(4), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showOutlineWithText(util.select().position(BUILD_INSTRUMENT), 80)
                .text("Seal every outside wall. Windows may replace casing on non-edge side blocks.")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(100);
        scene.overlay().showControls(util.vector().topOf(BUILD_INSTRUMENT), Pointing.DOWN, 70)
                .withItem(AllItems.WRENCH.asStack())
                .rightClick();
        scene.overlay().showOutlineWithText(util.select().position(BUILD_INSTRUMENT), 80)
                .text("Check the instrument port with a Create Wrench. Load fuel only after the structure forms.")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(BUILD_INSTRUMENT))
                .placeNearTarget();
        scene.idle(100);
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
        scene.world().showSection(util.select().fromTo(5, 2, 9, 5, 2, 12), Direction.SOUTH);
        scene.world().showSection(util.select().fromTo(7, 2, 9, 7, 2, 12), Direction.SOUTH);
        scene.world().showSection(util.select().fromTo(6, 2, 11, 6, 2, 12), Direction.SOUTH);
        scene.world().showSection(util.select().fromTo(8, 2, 11, 8, 2, 12), Direction.SOUTH);
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

        Selection coldDrive = util.select().fromTo(6, 2, 11, 6, 2, 12);
        Selection hotDrive = util.select().fromTo(8, 2, 11, 8, 2, 12);
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
