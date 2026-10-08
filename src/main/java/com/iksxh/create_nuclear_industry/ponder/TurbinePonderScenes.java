package com.iksxh.create_nuclear_industry.ponder;

import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** 三幕汽轮机客户端教学；模板与动画只服务于 Ponder 临时世界，不运行正式流体或动力账本。 */
public final class TurbinePonderScenes {
    private static final int FX = 7;
    private static final int FY = 2;
    private static final int FZ = 3;
    private static final BlockPos OP_INLET = new BlockPos(8, FY, 5);
    private static final BlockPos OP_EXHAUST = new BlockPos(6, FY, 4);
    private static final BlockPos OP_INPUT_TANK = new BlockPos(12, FY, 5);
    private static final BlockPos OP_OUTPUT_TANK = new BlockPos(2, FY, 4);
    private static final BlockPos OP_FRONT_SHAFT = new BlockPos(FX, FY, 2);
    private static final BlockPos OP_REAR_SHAFT = new BlockPos(FX, FY, 8);

    private TurbinePonderScenes() {}

    /** 先展示连续轴心，再逐环补壳并沿轴延伸，最后聚焦合法端口、观察窗和三档占格。 */
    public static void steamTurbineBuild(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("steam_turbine_build", "Steam turbine: Build");
        scene.configureBasePlate(1, 1, 14);
        scene.scaleSceneView(0.82F);
        scene.setSceneOffsetY(0.35F);
        scene.showBasePlate();

        int x = 8, fy = 2, fz = 5, length = 5;
        Selection core = util.select().position(x, fy, fz);
        for (int z = 1; z < length - 1; z++) core = core.add(util.select().position(x, fy, fz + z));
        core = core.add(util.select().position(x, fy, fz + length - 1));
        Selection small = machine(util, x, fy, fz, 3, length);
        scene.world().showSection(core.add(pad(util, 8, 2, 5, 3, 5)), Direction.DOWN);
        scene.idle(15);
        note(scene, core, util.vector().centerOf(x, fy, fz + 2), 80,
                "Place both end shafts, then keep the rotor row continuous through the center.");

        Selection frontCap = slice(util, x, fy, fz, 3, 0, 0).substract(core);
        scene.overlay().showControls(util.vector().centerOf(x, fy, fz), Pointing.DOWN, 50)
                .rightClick().withItem(new ItemStack(TurbineContent.CASING_ITEM.get()));
        scene.world().showSection(frontCap, Direction.DOWN);
        scene.idle(15);
        note(scene, frontCap, util.vector().centerOf(x + 1, fy, fz), 70,
                "Hold casing and right-click the core to fit the front end ring.");

        Selection firstRing = slice(util, x, fy, fz, 3, 1, 1).substract(core);
        scene.overlay().showControls(util.vector().centerOf(x + 1, fy, fz), Pointing.DOWN, 50)
                .rightClick().withItem(new ItemStack(TurbineContent.CASING_ITEM.get()));
        scene.world().showSection(firstRing, Direction.DOWN);
        scene.idle(15);
        note(scene, firstRing, util.vector().centerOf(x + 1, fy, fz + 1), 70,
                "Right-click the placed casing to extend the shell along the shaft.");

        Selection secondRing = slice(util, x, fy, fz, 3, 2, 2).substract(core);
        scene.overlay().showControls(util.vector().centerOf(x + 1, fy, fz + 1), Pointing.DOWN, 50)
                .rightClick().withItem(new ItemStack(TurbineContent.CASING_ITEM.get()));
        scene.world().showSection(secondRing, Direction.DOWN);
        scene.idle(15);
        note(scene, secondRing, util.vector().centerOf(x + 1, fy, fz + 1), 70,
                "Continue adding one shell ring at a time.");

        Selection thirdRing = slice(util, x, fy, fz, 3, 3, 3).substract(core);
        scene.overlay().showControls(util.vector().centerOf(x + 1, fy, fz + 1), Pointing.DOWN, 50)
                .rightClick().withItem(new ItemStack(TurbineContent.CASING_ITEM.get()));
        scene.world().showSection(thirdRing, Direction.DOWN);
        scene.idle(15);
        note(scene, thirdRing, util.vector().centerOf(x + 1, fy, fz + 3), 70,
                "Extend the shell toward the rear end.");

        Selection rearCap = slice(util, x, fy, fz, 3, 4, 4).substract(core);
        scene.overlay().showControls(util.vector().centerOf(x + 1, fy, fz + 3), Pointing.DOWN, 50)
                .rightClick().withItem(new ItemStack(TurbineContent.CASING_ITEM.get()));
        scene.world().showSection(rearCap, Direction.DOWN);
        scene.idle(15);
        note(scene, rearCap, util.vector().centerOf(x + 1, fy, fz + 4), 70,
                "Close the rear end with its casing ring.");

        scene.rotateCameraY(180);
        Selection inlet = util.select().position(x + 1, fy, fz + 2);
        note(scene, inlet, util.vector().centerOf(x + 1, fy, fz + 2), 75,
                "The inlet occupies a legal middle-row side slot.");
        scene.rotateCameraY(180);
        Selection exhaust = util.select().position(x - 1, fy, fz + 1);
        note(scene, exhaust, util.vector().centerOf(x - 1, fy, fz + 1), 75,
                "The exhaust port sits beside the front end ring.");
        Selection window = util.select().position(x - 1, fy, fz + 2);
        note(scene, window, util.vector().centerOf(x - 1, fy, fz + 2), 75,
                "Use a legal side window to inspect the rotor row.");

        scene.world().hideSection(small, Direction.SOUTH);
        scene.idle(15);
        scene.world().hideSection(util.select().fromTo(1, 0, 1, 14, 0, 14), Direction.DOWN);
        scene.idle(15);
        showTier(scene, util, 8, 2, 5, 3, 5, 8, 1, 1,
                "Small turbine: 3×5×3, with 3 rotors.");
        showTier(scene, util, 12, 3, 4, 5, 8, 8, 0, 0.5,
                "Medium tier: 5×8×5, with 6 rotors.");
        showTier(scene, util, 20, 4, 3, 7, 11, 8, 0, 0,
                "Large turbine: 7×11×7, with 9 rotors.");
        scene.markAsFinished();
    }

    /** 展示两路汽管、实际汽种标记、双端共享应力与窗口衰减后的停机。 */
    public static void steamTurbineOperation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("steam_turbine_operation", "Steam turbine: Steam and output");
        scene.configureBasePlate(0, 0, 14);
        scene.scaleSceneView(0.9F);
        scene.showBasePlate();
        Selection machine = util.select().fromTo(6, 1, 3, 8, 3, 7);
        Selection axles = axles(util);
        Selection input = lineSelection(util, 9, FY, 5, 12, FY, 5);
        Selection output = lineSelection(util, 2, FY, 4, 5, FY, 4);
        Selection pad = util.select().fromTo(1, 0, 2, 13, 0, 9);
        scene.world().showSection(machine.add(axles).add(input).add(output).add(pad), Direction.DOWN);
        scene.idle(15);
        setTankFluid(scene, OP_INPUT_TANK, new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 1000));
        setTankFluid(scene, OP_OUTPUT_TANK, FluidStack.EMPTY);
        note(scene, input.add(machine), util.vector().centerOf(OP_INPUT_TANK), 80,
                "Supercritical steam enters through the inlet and steam exits through the exhaust port.");

        scene.rotateCameraY(180);
        setTankFluid(scene, OP_INPUT_TANK, new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 500));
        setTankFluid(scene, OP_OUTPUT_TANK, new FluidStack(TurbineContent.STEAM.get(), 500));
        note(scene, input.add(output), util.vector().centerOf(OP_INLET), 90,
                "Supercritical steam passes through the turbine and converts to an equal amount of steam.");
        scene.rotateCameraY(180);
        note(scene, output, util.vector().centerOf(OP_EXHAUST), 75,
                "Exhaust steam can be sent to a heat exchanger for condensation and water return.");

        scene.rotateCameraY(35);
        scene.world().setKineticSpeed(axles, 256);
        scene.effects().rotationSpeedIndicator(OP_FRONT_SHAFT);
        scene.effects().rotationSpeedIndicator(OP_REAR_SHAFT);
        note(scene, util.select().position(OP_FRONT_SHAFT), util.vector().centerOf(OP_FRONT_SHAFT), 75,
                "The front output shaft runs at 256 RPM.");
        scene.rotateCameraY(180);
        note(scene, util.select().position(OP_REAR_SHAFT), util.vector().centerOf(OP_REAR_SHAFT), 75,
                "The rear shaft shares the same total stress capacity.");

        setTankFluid(scene, OP_INPUT_TANK, FluidStack.EMPTY);
        scene.idle(40);
        scene.world().setKineticSpeed(axles, 0);
        scene.effects().rotationSpeedIndicator(OP_FRONT_SHAFT);
        scene.effects().rotationSpeedIndicator(OP_REAR_SHAFT);
        note(scene, machine.add(axles), util.vector().centerOf(FX, FY, FZ + 2), 90,
                "After steam stops, wait 40 ticks; power stops when the average falls below threshold.");
        scene.markAsFinished();
    }

    /** 以临时储罐的十tick累计量呈现30%门槛、线性倍率和三档额定指标。 */
    public static void steamTurbineEfficiency(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("steam_turbine_efficiency", "Steam turbine: Flow and efficiency");
        scene.configureBasePlate(0, 0, 14);
        scene.scaleSceneView(0.9F);
        scene.showBasePlate();
        Selection machine = util.select().fromTo(6, 1, 3, 8, 3, 7);
        Selection lines = lineSelection(util, 9, FY, 5, 12, FY, 5)
                .add(lineSelection(util, 2, FY, 4, 5, FY, 4));
        Selection axles = axles(util);
        Selection pad = util.select().fromTo(1, 0, 2, 13, 0, 9);
        scene.world().showSection(machine.add(axles).add(lines).add(pad), Direction.DOWN);
        scene.idle(15);
        setTankFluid(scene, OP_INPUT_TANK, new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 1000));
        setTankFluid(scene, OP_OUTPUT_TANK, FluidStack.EMPTY);

        scene.overlay().showBigLine(PonderPalette.RED, util.vector().centerOf(OP_EXHAUST),
                util.vector().centerOf(OP_OUTPUT_TANK), 75);
        setTankFluid(scene, OP_INPUT_TANK, new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 900));
        setTankFluid(scene, OP_OUTPUT_TANK, new FluidStack(TurbineContent.STEAM.get(), 100));
        note(scene, machine.add(lines), util.vector().centerOf(OP_OUTPUT_TANK), 80,
                "Small turbine at 10 mB/t: below 30%, it still consumes steam but produces no power.");

        scene.overlay().showBigLine(PonderPalette.GREEN, util.vector().centerOf(OP_EXHAUST),
                util.vector().centerOf(OP_OUTPUT_TANK), 75);
        setTankFluid(scene, OP_INPUT_TANK, new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 738));
        setTankFluid(scene, OP_OUTPUT_TANK, new FluidStack(TurbineContent.STEAM.get(), 262));
        scene.world().setKineticSpeed(axles, 256);
        scene.effects().rotationSpeedIndicator(OP_FRONT_SHAFT);
        scene.effects().rotationSpeedIndicator(OP_REAR_SHAFT);
        note(scene, machine.add(lines), util.vector().centerOf(OP_OUTPUT_TANK), 80,
                "The small turbine starts producing power at 16.2 mB/t, at 0.5× efficiency.");

        scene.overlay().showBigLine(PonderPalette.GREEN, util.vector().centerOf(OP_EXHAUST),
                util.vector().centerOf(OP_OUTPUT_TANK), 100);
        setTankFluid(scene, OP_INPUT_TANK, new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 198));
        setTankFluid(scene, OP_OUTPUT_TANK, new FluidStack(TurbineContent.STEAM.get(), 802));
        note(scene, machine.add(lines), util.vector().centerOf(OP_OUTPUT_TANK), 90,
                "The small turbine reaches 1.2× efficiency at its rated flow of 54 mB/t.");
        note(scene, machine, util.vector().centerOf(FX, FY, FZ + 2), 75,
                "Medium tier: 108 mB/t rated flow, up to 1.5× efficiency.");
        note(scene, machine, util.vector().centerOf(FX, FY, FZ + 2), 75,
                "Large turbine: 216 mB/t rated flow, up to 1.8× efficiency.");
        note(scene, machine, util.vector().centerOf(FX, FY, FZ + 2), 75,
                "Efficiency uses actual average exhaust over 40 ticks, not the amount stored in the inlet tank.");
        scene.markAsFinished();
    }

    /** 将一档设备连同底座作为独立区段移到统一展示中心；位移单位为方块，仅调整客户端教学构图。 */
    private static void showTier(CreateSceneBuilder scene, SceneBuildingUtil util, int x, int fy, int fz,
                                 int diameter, int length, int viewX, int liftY, double offsetZ, String text) {
        Selection tier = machine(util, x, fy, fz, diameter, length)
                .add(pad(util, x, fy, fz, diameter, length));
        var section = scene.world().showIndependentSection(tier, Direction.DOWN);
        scene.idle(15);
        scene.world().moveSection(section, util.vector().of(viewX - x, liftY, offsetZ), 15);
        scene.idle(15);
        int radius = (diameter - 1) / 2;
        double minX = viewX - radius;
        double minY = fy - radius + liftY;
        double minZ = fz + offsetZ;
        AABB framedBounds = new AABB(minX, minY, minZ,
                minX + diameter, minY + diameter, minZ + length);
        noteWithBounds(scene, framedBounds, 75, text);
        scene.world().hideIndependentSection(section, Direction.SOUTH);
        scene.idle(15);
    }

    private static Selection pad(SceneBuildingUtil util, int x, int fy, int fz, int diameter, int length) {
        int radius = (diameter - 1) / 2;
        return util.select().fromTo(x - radius - 1, 0, fz - 1,
                x + radius + 1, 0, fz + length);
    }

    private static Selection slice(SceneBuildingUtil util, int x, int fy, int fz, int diameter, int from, int to) {
        int radius = (diameter - 1) / 2;
        return util.select().fromTo(x - radius, fy - radius, fz + from,
                x + radius, fy + radius, fz + to);
    }

    private static Selection machine(SceneBuildingUtil util, int x, int fy, int fz, int diameter, int length) {
        return slice(util, x, fy, fz, diameter, 0, length - 1);
    }

    private static Selection lineSelection(SceneBuildingUtil util, int x1, int y1, int z1,
                                           int x2, int y2, int z2) {
        return util.select().fromTo(x1, y1, z1, x2, y2, z2);
    }

    private static Selection axles(SceneBuildingUtil util) {
        return util.select().position(OP_FRONT_SHAFT).add(util.select().position(OP_REAR_SHAFT));
    }

    /** 正文额外空等20tick，补偿 Ponder 1.0.82 文本多出的10tick寿命并留出净间隔。 */
    private static void note(CreateSceneBuilder scene, Selection target, Vec3 point, int duration, String text) {
        scene.overlay().showOutlineWithText(target, duration).text(text)
                .attachKeyFrame().pointAt(point).placeNearTarget();
        scene.idle(duration + 20);
    }

    /** 用支持小数坐标的 AABB 高亮框与独立区段同步，并沿用普通正文的阅读与净空时长。 */
    private static void noteWithBounds(CreateSceneBuilder scene, AABB bounds, int duration, String text) {
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, new Object(), bounds, duration);
        scene.overlay().showText(duration).text(text)
                .attachKeyFrame().pointAt(bounds.getCenter()).placeNearTarget();
        scene.idle(duration + 20);
    }

    /** 仅改写 Ponder 临时 Create 储罐，展示流体种类与累计量，不执行正式汽轮机交易。 */
    private static void setTankFluid(CreateSceneBuilder scene, BlockPos position, FluidStack fluid) {
        scene.world().modifyBlockEntity(position, FluidTankBlockEntity.class, tank -> {
            tank.getTankInventory().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            if (!fluid.isEmpty())
                tank.getTankInventory().fill(fluid.copy(), IFluidHandler.FluidAction.EXECUTE);
        });
    }
}
