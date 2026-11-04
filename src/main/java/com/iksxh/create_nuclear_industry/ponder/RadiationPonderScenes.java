package com.iksxh.create_nuclear_industry.ponder;

import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.RadiationContent;
import com.iksxh.create_nuclear_industry.radiation.FuelUseHistory;
import com.iksxh.create_nuclear_industry.storage.DryStorageBlock;
import com.iksxh.create_nuclear_industry.storage.DryStorageBlockEntity;
import com.iksxh.create_nuclear_industry.storage.SpentFuelPayload;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 辐射防护和屏蔽封存的客户端教学；所有物品均为临时展示副本，读数单位与仪表说明一致。
 * 不采样真实源、不改玩家剂量/装备，不注入HUD缓存；状态示意仅限临时贮存架的读入边界。
 */
public final class RadiationPonderScenes {
    private static final BlockPos FRESH = new BlockPos(6, 2, 1);
    private static final BlockPos USED = new BlockPos(6, 2, 3);
    private static final BlockPos SPENT = new BlockPos(6, 2, 5);
    private static final BlockPos CONTAINER = new BlockPos(9, 1, 3);
    private static final BlockPos ASSEMBLY = new BlockPos(7, 2, 7);
    private static final BlockPos RACK = new BlockPos(3, 1, 8);

    private RadiationPonderScenes() {}

    /** 先识别源与防护，再比较容器、屏蔽墙、合法封装和完整贮存架。 */
    public static void protection(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("radiation_protection", "辐射：防护与屏蔽封存");
        scene.configureBasePlate(0, 0, 13);
        scene.scaleSceneView(0.77F);
        scene.setSceneOffsetY(-0.5F);
        scene.showBasePlate();
        Selection samples = util.select().fromTo(6, 1, 1, 6, 2, 5);
        scene.world().showSection(samples, Direction.DOWN);
        scene.idle(20);
        scene.world().createItemOnBeltLike(FRESH, Direction.UP, new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get()));
        scene.world().createItemOnBeltLike(USED, Direction.UP,
                FuelUseHistory.markUsedCopy(new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get())));
        scene.world().createItemOnBeltLike(SPENT, Direction.UP, new ItemStack(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get()));
        note(scene, samples, util.vector().centerOf(USED), 149,
                "新组件、铀原料、贫化铀材料与装备不放射；燃料开始消耗后，组件即使还有耐久也会产生辐射。");
        note(scene, util.select().position(SPENT), util.vector().topOf(SPENT), 105,
                "枯竭铀燃料组件和事故废料也是辐射源；冷却不会把它们变成无辐射物品。");
        scene.overlay().showControls(util.vector().topOf(USED), Pointing.DOWN, 80).withItem(new ItemStack(RadiationContent.METER.get()));
        note(scene, util.select().position(USED), util.vector().topOf(USED), 138,
                "主手或副手持辐射计，查看场强、累计剂量和实际吸收强度；靠近源、搬运或布置屏蔽时都要留意读数。");
        scene.overlay().showControls(util.vector().topOf(FRESH), Pointing.DOWN, 90).withItem(new ItemStack(RadiationContent.HELMET.get()));
        scene.overlay().showControls(util.vector().topOf(USED), Pointing.DOWN, 90).withItem(new ItemStack(RadiationContent.CHESTPLATE.get()));
        scene.overlay().showControls(util.vector().topOf(SPENT), Pointing.DOWN, 90).withItem(new ItemStack(RadiationContent.LEGGINGS.get()));
        scene.overlay().showControls(util.vector().topOf(SPENT).add(1, 0, 0), Pointing.DOWN, 90).withItem(new ItemStack(RadiationContent.BOOTS.get()));
        note(scene, samples, util.vector().topOf(USED), 166,
                "防护服必须穿在对应部位，才能削减自身吸收；头罩兼具工程师护目镜功能。穿戴不会降低携带源对他人的辐射。");
        scene.world().showSection(util.select().position(CONTAINER), Direction.DOWN);
        scene.idle(20);
        note(scene, util.select().position(CONTAINER), util.vector().centerOf(CONTAINER), 105,
                "普通箱桶不会额外屏蔽核废料。把源丢在普通容器里，仍要考虑周围人员的暴露。");
        Selection wall = util.select().fromTo(3, 1, 2, 3, 3, 4);
        scene.world().showSection(wall, Direction.DOWN);
        scene.idle(20);
        scene.overlay().showBigLine(PonderPalette.RED, util.vector().centerOf(USED), util.vector().centerOf(3, 2, 3), 90);
        scene.overlay().showBigLine(PonderPalette.GREEN, util.vector().centerOf(3, 2, 3), util.vector().centerOf(1, 2, 3), 90);
        note(scene, wall, util.vector().centerOf(3, 2, 3), 120,
                "屏蔽混凝土可削减穿过它的辐射，完整反应堆外壳也能屏蔽内部源；不要把任意装饰块当成屏蔽材料。");
        scene.world().hideSection(wall.add(samples).add(util.select().position(CONTAINER)), Direction.UP);
        scene.world().removeItemsFromBelt(FRESH);
        scene.world().removeItemsFromBelt(USED);
        scene.world().removeItemsFromBelt(SPENT);
        scene.idle(20);
        Selection assembly = util.select().fromTo(7, 1, 7, 8, 3, 8);
        scene.world().showSection(assembly, Direction.DOWN);
        scene.world().setKineticSpeed(util.select().position(ASSEMBLY).add(util.select().position(ASSEMBLY.below())), 64);
        scene.world().showSection(util.select().position(RACK), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(ASSEMBLY), Pointing.DOWN, 80).withItem(new ItemStack(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get()));
        note(scene, assembly, util.vector().centerOf(ASSEMBLY), 120,
                "用屏蔽装配台封装乏燃料或事故废料，再把核废料桶存入贮存架。");
        ItemStack cask = SpentFuelPayload.seal(new ItemStack(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get()));
        scene.overlay().showControls(util.vector().topOf(RACK), Pointing.DOWN, 80).rightClick().withItem(cask);
        note(scene, util.select().position(RACK), util.vector().centerOf(RACK), 143,
                "封装桶默认削减95%向外辐射，存入完整贮存架后架内桶完全屏蔽；取出后仍只有桶自身的屏蔽。");
        rackSnapshot(scene, cask);
        note(scene, util.select().position(RACK), util.vector().centerOf(RACK), 138,
                "远离或屏蔽源后，累计剂量会逐步恢复。冷却、防烫与辐射防护分别处理，不要用低温外观判断辐射安全。");
        scene.markAsFinished();
    }

    /** 仅沿既有客户端快照载入一桶，演示占用外观；不调用贮存服务端插入或辐射采样。 */
    private static void rackSnapshot(CreateSceneBuilder scene, ItemStack cask) {
        scene.world().modifyBlockEntity(RACK, DryStorageBlockEntity.class, rack -> {
            if (rack.getLevel() == null || !rack.getLevel().isClientSide) return;
            var registries = rack.getLevel().registryAccess();
            rack.loadWithComponents(DryStoragePonderScenes.inventorySnapshot(1, (CompoundTag) cask.save(registries)), registries);
        });
        scene.world().modifyBlock(RACK, s -> s.setValue(DryStorageBlock.STORAGE_LEVEL, 1), false);
    }

    /** 正文额外10tick淡出后再留10tick净空，不在此构造真实仪表读数。 */
    private static void note(CreateSceneBuilder scene, Selection selection, Vec3 target, int duration, String text) {
        scene.overlay().showOutlineWithText(selection, duration).text(text).attachKeyFrame().pointAt(target).placeNearTarget();
        scene.idle(duration + 20);
    }
}
