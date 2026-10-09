package com.iksxh.create_nuclear_industry.ponder;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlock;
import com.iksxh.create_nuclear_industry.production.FuelSinteringBlock;
import com.iksxh.create_nuclear_industry.production.FuelSinteringBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 烧结炉两幕客户端教学；坐标单位为方块格，工时快照单位为有效tick。
 * 仅改写Ponder临时世界的既有NBT和渲染库存，不运行服务端加工、供热或物流事务。
 */
public final class FuelSinteringPonderScenes {
    private static final BlockPos FURNACE = new BlockPos(3, 2, 3);
    private static final BlockPos HEATER = FURNACE.below();
    private static final BlockPos CHUTE = FURNACE.above();
    private static final BlockPos RECEIVER = new BlockPos(2, 1, 2);
    private static final BlockPos HOT_TANK = new BlockPos(0, 1, 3);
    private static final BlockPos COLD_TANK = new BlockPos(6, 1, 3);

    private FuelSinteringPonderScenes() {}

    /** 炉体与底部热源先显露，再按真实账本展示一进一出及断热保留工时。 */
    public static void fuelSinteringOperation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = setup(builder, "fuel_sintering_operation", "燃料烧结炉：烧结与供热");
        Selection machine = util.select().position(FURNACE);
        scene.world().showSection(machine.add(util.select().position(HEATER)), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showControls(util.vector().topOf(FURNACE), Pointing.DOWN, 35)
                .rightClick().withItem(green());
        note(scene, machine, util.vector().topOf(FURNACE), 65,
                "在底部供热，从顶部投入生芯块。");
        snapshot(scene, util, 1, 0, 0);
        snapshot(scene, util, 1, 0, 200);
        scene.idle(25);
        snapshot(scene, util, 0, 1, 0);
        scene.overlay().showControls(util.vector().centerOf(FURNACE).add(0, 0, -0.55), Pointing.RIGHT, 35)
                .withItem(sintered());
        note(scene, machine, util.vector().centerOf(FURNACE), 65,
                "每个生芯块烧结为一个烧结芯块。");

        snapshot(scene, util, 1, 1, 200);
        burner(scene, false);
        lit(scene, false);
        note(scene, machine.add(util.select().position(HEATER)), util.vector().centerOf(HEATER), 75,
                "断热会暂停烧结，恢复加热后继续。");
        // 停热期间保持200有效tick，恢复后才展示新的工时和成品快照。
        burner(scene, true);
        lit(scene, true);
        snapshot(scene, util, 1, 1, 300);
        scene.idle(25);
        snapshot(scene, util, 0, 2, 0);
        scene.idle(20);
        scene.markAsFinished();
    }

    /** 顶部溜槽输入，北面漏斗取出到承接漏斗和桶；随后替换唯一底部热源为核换热器。 */
    public static void fuelSinteringAutomation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = setup(builder, "fuel_sintering_automation", "燃料烧结炉：自动化与核热");
        Selection machine = util.select().position(FURNACE);
        Selection logistics = util.select().fromTo(3, 3, 3, 3, 4, 3)
                .add(util.select().fromTo(3, 1, 2, 3, 2, 2)).add(util.select().position(RECEIVER));
        scene.world().showSection(machine.add(util.select().position(HEATER)), Direction.DOWN);
        scene.idle(15);
        scene.world().showSection(logistics, Direction.DOWN);
        scene.idle(15);
        scene.world().modifyBlockEntity(new BlockPos(3, 4, 3), BarrelBlockEntity.class,
                barrel -> barrel.setItem(0, green()));
        scene.world().modifyBlockEntity(CHUTE, ChuteBlockEntity.class, chute -> chute.setItem(green(), 0.6F));
        note(scene, util.select().position(CHUTE), util.vector().centerOf(CHUTE), 65,
                "通过顶部溜槽送入生芯块。");
        scene.world().modifyBlockEntity(CHUTE, ChuteBlockEntity.class, chute -> chute.setItem(ItemStack.EMPTY));
        scene.world().modifyBlockEntity(new BlockPos(3, 4, 3), BarrelBlockEntity.class,
                barrel -> barrel.setItem(0, ItemStack.EMPTY));
        snapshot(scene, util, 1, 0, 200);
        scene.idle(20);
        snapshot(scene, util, 0, 1, 0);
        note(scene, util.select().position(3, 2, 2).add(util.select().position(RECEIVER)),
                util.vector().centerOf(3, 2, 2), 75,
                "四个水平面都能取出烧结芯块；用漏斗取出，送入承接容器。");
        snapshot(scene, util, 0, 0, 0);
        // 物品从北面漏斗向北落到其正下方承接漏斗；随后写入该漏斗西侧的桶。
        var product = scene.world().createItemEntity(new Vec3(3.5, 2.2, 2.3),
                new Vec3(0, -0.1, -0.03), sintered());
        scene.idle(12);
        scene.world().modifyEntity(product, Entity::discard);
        scene.world().modifyBlockEntity(RECEIVER, BarrelBlockEntity.class, barrel -> barrel.setItem(0, sintered()));
        scene.idle(15);

        // 冷热回路沿东西轴，避开北侧出料；隐藏物流后炉体和换热器同时可辨识。
        scene.world().hideSection(logistics, Direction.UP);
        scene.idle(20);
        scene.world().setBlock(HEATER, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState()
                .setValue(NuclearHeatExchangerBlock.FACING, Direction.EAST)
                .setValue(NuclearHeatExchangerBlock.LIT, false), false);
        lit(scene, false);
        Selection loop = util.select().fromTo(0, 1, 3, 2, 1, 3)
                .add(util.select().fromTo(4, 1, 3, 6, 1, 3));
        scene.world().showSection(loop, Direction.DOWN);
        scene.idle(15);
        tank(scene, HOT_TANK, new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 1000));
        tank(scene, COLD_TANK, FluidStack.EMPTY);
        scene.world().modifyBlock(HEATER, state -> state.setValue(NuclearHeatExchangerBlock.LIT, true), false);
        lit(scene, true);
        note(scene, machine.add(util.select().position(HEATER)).add(loop), util.vector().centerOf(HEATER), 80,
                "核换热器也能从底部供热：后侧接入热液，前侧排出等量冷液。保持热液供应和冷液出口畅通。");
        tank(scene, HOT_TANK, new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 800));
        tank(scene, COLD_TANK, new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 200));
        snapshot(scene, util, 1, 1, 200);
        scene.idle(20);
        scene.markAsFinished();
    }

    /** 七格紧凑地台留出上下控件空间；最低热源y=1，炉体y=2。 */
    private static CreateSceneBuilder setup(SceneBuilder builder, String id, String title) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title(id, title);
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.85F);
        scene.setSceneOffsetY(-0.5F);
        scene.showBasePlate();
        return scene;
    }

    /** 通过既有FuelSintering NBT载入数量和有效工时，不调用正式加工引擎。 */
    private static void snapshot(CreateSceneBuilder scene, SceneBuildingUtil util, int input, int output, int progress) {
        scene.world().modifyBlockEntityNBT(util.select().position(FURNACE), FuelSinteringBlockEntity.class, tag -> {
            CompoundTag state = new CompoundTag();
            state.putInt("Input", input);
            state.putInt("Output", output);
            state.putInt("Progress", progress);
            tag.put("FuelSintering", state);
        });
    }

    private static ItemStack green() { return new ItemStack(FuelProcessingContent.GREEN_FUEL_PELLET.get()); }
    private static ItemStack sintered() { return new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get()); }

    /** 燃烧室heat级kindled满足正式HEATED条件，smouldering则停止供热。 */
    private static void burner(CreateSceneBuilder scene, boolean heated) {
        scene.world().setBlock(HEATER, AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL,
                heated ? BlazeBurnerBlock.HeatLevel.KINDLED : BlazeBurnerBlock.HeatLevel.SMOULDERING), false);
    }

    private static void lit(CreateSceneBuilder scene, boolean lit) {
        scene.world().modifyBlock(FURNACE, state -> state.setValue(FuelSinteringBlock.LIT, lit), false);
    }

    /** 只改临时Create储罐可见库存；热液减少与冷液增加使用同一mB数量。 */
    private static void tank(CreateSceneBuilder scene, BlockPos pos, FluidStack fluid) {
        scene.world().modifyBlockEntity(pos, FluidTankBlockEntity.class, tank -> {
            tank.getTankInventory().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            if (!fluid.isEmpty()) tank.getTankInventory().fill(fluid.copy(), IFluidHandler.FluidAction.EXECUTE);
        });
    }

    /** Ponder1.0.82正文含额外10tick淡出，等待duration+20保证完全退出后至少10tick。 */
    private static void note(CreateSceneBuilder scene, Selection target, Vec3 point, int duration, String text) {
        scene.overlay().showOutlineWithText(target, duration).text(text).attachKeyFrame().pointAt(point).placeNearTarget();
        scene.idle(duration + 20);
    }
}
