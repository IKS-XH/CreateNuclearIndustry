package com.iksxh.create_nuclear_industry.ponder;

import com.iksxh.create_nuclear_industry.content.BasicMaterialContent;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 离心机思索仅操作 Ponder 客户端临时世界，演示外部动力、接管和物料去向。
 * 不读取或调用正式服务端库存、玩家数据、配方执行或维修事务。
 */
public final class CentrifugePonderScenes {
    private static final BlockPos GEARBOX = new BlockPos(4, 1, 4);
    private static final BlockPos MACHINE_LOWER = new BlockPos(4, 2, 4);
    private static final BlockPos MACHINE_UPPER = new BlockPos(4, 3, 4);
    private static final BlockPos SLURRY_TANK = new BlockPos(4, 7, 4);
    private static final BlockPos RECOVERY_TANK = new BlockPos(7, 2, 5);
    private static final BlockPos PUMP = new BlockPos(4, 5, 4);
    private static final BlockPos WATER_PUMP = new BlockPos(6, 2, 5);
    private static final BlockPos LOWER_FUNNEL = new BlockPos(4, 2, 3);
    private static final BlockPos LOWER_RECEIVER = new BlockPos(4, 1, 3);
    private static final BlockPos UPPER_RECEIVER = new BlockPos(5, 2, 4);
    private static final BlockPos UPPER_FUNNEL = new BlockPos(5, 3, 4);

    private CentrifugePonderScenes() {
    }

    /**
     * 展示两段机身、竖直动力轴、顶部泵送示意、配方示例产物、过滤与回水，以及停机排查和维修标记。
     * 动力速度只作为转速演示；场景中的瞬时物料切换不表示配方真实处理耗时。
     */
    public static void enrichmentCentrifugeBasics(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("enrichment_centrifuge", "Uranium Slurry Enrichment Centrifuge");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.68F);
        scene.showBasePlate();

        Selection lowerDrive = util.select().fromTo(1, 1, 4, 4, 2, 4);
        Selection inputPump = util.select().position(PUMP);
        Selection inputPumpDrive = util.select().fromTo(3, 5, 4, 3, 8, 4);
        Selection returnPump = util.select().position(WATER_PUMP);
        Selection returnPumpDrive = util.select().fromTo(6, 2, 4, 8, 2, 4);
        Selection lowerBody = util.select().position(MACHINE_LOWER);
        Selection upperBody = util.select().position(MACHINE_UPPER);
        scene.world().setKineticSpeed(lowerDrive, 128);

        scene.world().showSection(util.select().layer(1), Direction.UP);
        scene.idle(5);
        scene.world().showSection(lowerBody, Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(upperBody, Direction.DOWN);
        scene.idle(10);
        scene.overlay().showOutlineWithText(lowerBody.add(upperBody), 90)
                .text("The centrifuge separates uranium slurry into low-enriched dust, depleted dust, and reusable water.")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(100);

        scene.overlay().showControls(util.vector().topOf(new BlockPos(4, 1, 3)), Pointing.DOWN, 70)
                .withItem(new ItemStack(FuelProcessingContent.ENRICHMENT_CENTRIFUGE_ITEM.get()))
                .rightClick();
        scene.overlay().showOutlineWithText(lowerBody.add(upperBody), 90)
                .text("Place one centrifuge item to form both halves. Leave room above the upper section for the pipe.")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(MACHINE_UPPER))
                .placeNearTarget();
        scene.idle(100);

        scene.world().showSection(util.select().fromTo(1, 1, 4, 4, 1, 4), Direction.SOUTH);
        scene.idle(8);
        scene.world().setKineticSpeed(lowerDrive, 128);
        scene.effects().rotationSpeedIndicator(MACHINE_LOWER);
        scene.overlay().showOutlineWithText(util.select().position(GEARBOX), 90)
                .text("Drive the lower shaft. Any nonzero speed stable for 20 ticks can run it; 128 RPM is an example. Creative motors only illustrate power.")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(GEARBOX))
                .placeNearTarget();
        scene.idle(100);

        scene.world().showSection(util.select().fromTo(4, 4, 4, 4, 7, 4), Direction.DOWN);
        scene.world().showSection(inputPumpDrive, Direction.WEST);
        scene.idle(10);
        setTankFluid(scene, SLURRY_TANK, new FluidStack(FuelProcessingContent.URANIUM_SLURRY.get(), 1000));
        scene.world().setKineticSpeed(inputPump, 128);
        scene.world().setKineticSpeed(inputPumpDrive, -128);
        scene.effects().rotationSpeedIndicator(PUMP);
        scene.overlay().showBigLine(
                net.createmod.ponder.api.PonderPalette.BLUE,
                util.vector().topOf(SLURRY_TANK),
                util.vector().topOf(MACHINE_UPPER),
                70
        );
        scene.overlay().showOutlineWithText(inputPump.add(inputPumpDrive).add(
                        util.select().position(new BlockPos(4, 6, 4)).add(util.select().position(SLURRY_TANK))), 100)
                .text("The top is the sole slurry inlet. A mechanical pump feeds slurry from the tank through the pipe; pipes alone do not pump.")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(PUMP))
                .placeNearTarget();
        scene.idle(110);

        scene.overlay().showOutlineWithText(upperBody, 105)
                .text("Current recipe example: 1000 mB slurry yields 1 low-enriched dust, 7 depleted dust, and 1000 mB water. The visual is illustrative; actual time varies with speed.")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(MACHINE_UPPER))
                .placeNearTarget();
        scene.idle(80);
        scene.world().modifyBlockEntity(SLURRY_TANK, FluidTankBlockEntity.class, tank ->
                tank.getTankInventory().drain(1000, IFluidHandler.FluidAction.EXECUTE));
        scene.world().showSection(util.select().position(LOWER_RECEIVER).add(util.select().position(LOWER_FUNNEL)), Direction.UP);
        scene.world().showSection(util.select().position(UPPER_RECEIVER).add(util.select().position(UPPER_FUNNEL)), Direction.UP);
        scene.world().showSection(util.select().fromTo(4, 2, 5, 5, 2, 5), Direction.WEST);
        scene.world().showSection(returnPumpDrive, Direction.WEST);
        scene.world().showSection(returnPump, Direction.WEST);
        scene.world().showSection(util.select().position(RECOVERY_TANK), Direction.DOWN);
        scene.idle(10);
        scene.world().setKineticSpeed(returnPump, 128);
        scene.world().setKineticSpeed(returnPumpDrive, -128);
        scene.effects().rotationDirectionIndicator(WATER_PUMP.north());
        scene.world().setFilterData(util.select().position(LOWER_FUNNEL), FunnelBlockEntity.class,
                new ItemStack(FuelProcessingContent.DEPLETED_URANIUM_DUST.get()));
        scene.world().setFilterData(util.select().position(UPPER_FUNNEL), FunnelBlockEntity.class,
                new ItemStack(FuelProcessingContent.LOW_ENRICHED_URANIUM_DUST.get()));
        scene.world().flapFunnel(LOWER_FUNNEL, false);
        scene.world().flapFunnel(UPPER_FUNNEL, false);
        scene.world().createItemEntity(
                util.vector().centerOf(LOWER_FUNNEL).add(0, -0.45, -0.25),
                util.vector().of(0, -0.06, 0),
                new ItemStack(FuelProcessingContent.DEPLETED_URANIUM_DUST.get(), 7));
        scene.world().createItemEntity(
                util.vector().centerOf(UPPER_FUNNEL).add(0.25, -0.45, 0),
                util.vector().of(0, -0.06, 0),
                new ItemStack(FuelProcessingContent.LOW_ENRICHED_URANIUM_DUST.get()));
        setTankFluid(scene, RECOVERY_TANK, new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000));
        scene.overlay().showOutlineWithText(util.select().fromTo(4, 1, 3, 8, 3, 5).add(returnPumpDrive), 150)
                .text("The top is the sole slurry inlet. Either side of both halves can output either powder or water; external filters choose the material. This example drops filtered powders into hoppers and pumps water to its tank.")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(RECOVERY_TANK))
                .placeNearTarget();
        scene.idle(120);

        scene.world().setKineticSpeed(lowerDrive, 0);
        scene.world().setKineticSpeed(inputPump, 0);
        scene.world().setKineticSpeed(inputPumpDrive, 0);
        scene.world().setKineticSpeed(returnPump, 0);
        scene.world().setKineticSpeed(returnPumpDrive, 0);
        scene.effects().indicateRedstone(MACHINE_LOWER);
        scene.overlay().showOutlineWithText(lowerBody.add(upperBody), 105)
                .text("Power loss, unstable speed, or blocked outputs pauses processing. Check power, input, and outputs with engineer goggles; an unfinished batch is kept.")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(MACHINE_LOWER))
                .placeNearTarget();
        scene.idle(115);

        scene.overlay().showOutlineWithText(lowerBody, 100)
                .text("Stop the machine when bearing wear is exhausted, then repair the lower section with one heavy bearing. This scene is an action marker only.")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(MACHINE_LOWER))
                .placeNearTarget();
        scene.overlay().showControls(util.vector().topOf(MACHINE_LOWER), Pointing.DOWN, 75)
                .withItem(new ItemStack(BasicMaterialContent.HEAVY_BEARING.get()))
                .rightClick();
        scene.idle(110);

        scene.markAsFinished();
    }

    /** 在思索临时方块实体中直接填充演示液体，不经过机器流体事务或服务端逻辑。 */
    private static void setTankFluid(CreateSceneBuilder scene, BlockPos position, FluidStack fluid) {
        scene.world().modifyBlockEntity(position, FluidTankBlockEntity.class, tank -> {
            tank.getTankInventory().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            if (!fluid.isEmpty())
                tank.getTankInventory().fill(fluid.copy(), IFluidHandler.FluidAction.EXECUTE);
        });
    }
}
