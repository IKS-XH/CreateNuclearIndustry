package com.iksxh.create_nuclear_industry.ponder;

import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.storage.DryStorageBlock;
import com.iksxh.create_nuclear_industry.storage.DryStorageBlockEntity;
import com.iksxh.create_nuclear_industry.storage.SpentFuelPayload;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.api.PonderPalette;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 干式贮存架的单页客户端用法教学，坐标单位为方块格，容量单位为桶。
 * Ponder临时世界只读入教学快照；正式服务端库存事务及客户端权限边界保持不变。
 */
public final class DryStoragePonderScenes {
    private static final BlockPos RACK = new BlockPos(2, 2, 2);
    private static final BlockPos INPUT = new BlockPos(3, 2, 2);
    private static final BlockPos SOURCE = new BlockPos(3, 3, 2);
    private static final BlockPos OUTPUT = new BlockPos(2, 1, 2);
    private static final BlockPos RECEIVER = new BlockPos(2, 1, 3);

    private DryStoragePonderScenes() {}

    /** 一页依次展示手动存取、侧面输入、底部输出、占用与满架等待。 */
    public static void usage(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("dry_storage_usage", "干式贮存架：存取与自动化");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.9F);
        scene.setSceneOffsetY(-0.5F);
        scene.showBasePlate();
        // 原生GUI只反转Y；默认Y=145°观察北/西面，转至325°显露南/东面。
        scene.rotateCameraY(180);
        scene.idle(20);
        scene.world().showSection(util.select().position(RACK), Direction.DOWN);
        scene.overlay().showControls(util.vector().topOf(RACK), Pointing.DOWN, 35).rightClick().withItem(cask());
        snapshot(scene, 1);
        note(scene, util.select().position(RACK), util.vector().centerOf(RACK), 80,
                "手持封装乏燃料桶右击贮存架，每次存入一桶。");

        scene.overlay().showControls(util.vector().topOf(RACK), Pointing.DOWN, 35).rightClick();
        snapshot(scene, 0);
        note(scene, util.select().position(RACK), util.vector().centerOf(RACK), 85,
                "空手右击取出一桶；背包满时，桶仍留在架内。");

        Selection input = util.select().position(INPUT).add(util.select().position(SOURCE));
        Selection output = util.select().position(OUTPUT).add(util.select().position(RECEIVER));
        scene.world().showSection(input.add(output), Direction.DOWN);
        scene.idle(20);
        scene.world().modifyBlockEntity(SOURCE, BarrelBlockEntity.class, barrel -> barrel.setItem(0, ItemStack.EMPTY));
        waiting(scene, true);
        scene.idle(15);
        snapshot(scene, 1);
        waiting(scene, false);
        scene.overlay().showLine(PonderPalette.GREEN, util.vector().centerOf(INPUT), util.vector().centerOf(RACK), 100);
        scene.overlay().showLine(PonderPalette.BLUE, util.vector().centerOf(OUTPUT), util.vector().centerOf(RECEIVER), 100);
        note(scene, util.select().position(INPUT).add(output), util.vector().centerOf(INPUT), 110,
                "顶部和四侧可输入，各面可输出；底部只可输出。东侧漏斗送入，底部漏斗向南取出。");

        // 每次把一件上游桶移入独立槽；槽数与外观档位随同一快照更新。
        for (int used = 2; used <= 16; used++) {
            waiting(scene, true);
            scene.idle(5);
            snapshot(scene, used);
            waiting(scene, false);
            scene.idle(5);
        }
        scene.overlay().showControls(util.vector().topOf(RACK), Pointing.DOWN, 45).withItem(AllItems.GOGGLES.asStack());
        note(scene, util.select().position(RACK), util.vector().centerOf(RACK), 100,
                "占用随存入增加，护目镜查看已用/容量。默认16桶，容量可配置；当前已用16/16。");

        waiting(scene, true);
        snapshot(scene, 16);
        note(scene, util.select().position(INPUT).add(util.select().position(RACK)), util.vector().centerOf(INPUT), 85,
                "满架不再接收，额外的一桶留在上游漏斗等待。");

        snapshot(scene, 15);
        scene.world().modifyBlockEntity(OUTPUT, HopperBlockEntity.class, hopper -> hopper.setItem(0, cask()));
        scene.idle(20);
        scene.world().modifyBlockEntity(OUTPUT, HopperBlockEntity.class, hopper -> hopper.setItem(0, ItemStack.EMPTY));
        scene.world().modifyBlockEntity(RECEIVER, BarrelBlockEntity.class, barrel -> barrel.setItem(0, cask()));
        snapshot(scene, 16);
        waiting(scene, false);
        caskIcon(scene, new Vec3(2.5, 2.2, 3.5));
        note(scene, util.select().position(RACK).add(util.select().position(INPUT)).add(output), util.vector().centerOf(OUTPUT), 90,
                "取出一桶腾出空位后，上游继续存入；取出的桶留在南侧承接桶中。");
        scene.idle(15);
        scene.markAsFinished();
    }

    /** 从真实组件封装出独立单件桶，不以空壳或一个多件栈代替完整载荷。 */
    private static ItemStack cask() {
        return SpentFuelPayload.seal(new ItemStack(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get()));
    }

    /** 仅更新教学世界的已有客户端读入边界；不调用受current()保护的服务端事务。 */
    private static void snapshot(CreateSceneBuilder scene, int used) {
        scene.world().modifyBlockEntity(RACK, DryStorageBlockEntity.class, rack -> {
            var registries = rack.getLevel().registryAccess();
            rack.loadWithComponents(inventorySnapshot(used, (CompoundTag) cask().save(registries)), registries);
        });
        scene.world().modifyBlock(RACK, state -> state.setValue(DryStorageBlock.STORAGE_LEVEL,
                used == 0 ? 0 : (used + 3) / 4), false);
    }

    /** 产生0..16个独立单件槽的只读教学输入；容量为本页默认值，不修改正式配置。 */
    static CompoundTag inventorySnapshot(int used, CompoundTag sealed) {
        if (used < 0 || used > 16) throw new IllegalArgumentException("教学占用超出默认容量");
        CompoundTag content = new CompoundTag();
        for (int slot = 0; slot < used; slot++) content.put("Slot" + slot, sealed.copy());
        CompoundTag snapshot = new CompoundTag();
        snapshot.put("DryStorage", content);
        snapshot.putInt("Capacity", 16);
        return snapshot;
    }

    /** 一件等待桶始终保存在上游，只有完成入架的同帧才清空该教学槽。 */
    private static void waiting(CreateSceneBuilder scene, boolean present) {
        scene.world().modifyBlockEntity(INPUT, HopperBlockEntity.class,
                hopper -> hopper.setItem(0, present ? cask() : ItemStack.EMPTY));
        // 图标只投影同一上游槽，满架期间保留；本页此时没有其他物品图标。
        if (present) caskIcon(scene, new Vec3(3.5, 2.8, 3.15));
        else scene.world().modifyEntities(ItemEntity.class, ItemEntity::discard);
    }

    /** 静止物品图标投影教学库存，不参与碰撞、拾取或正式贮存事务。 */
    private static void caskIcon(CreateSceneBuilder scene, Vec3 point) {
        var icon = scene.world().createItemEntity(point, Vec3.ZERO, cask());
        scene.world().modifyEntity(icon, entity -> {
            if (entity instanceof ItemEntity item) {
                item.noPhysics = true;
                item.setNoGravity(true);
                item.setPickUpDelay(Integer.MAX_VALUE);
            }
        });
    }

    /** 正文显示结束后覆盖10tick淡出，再留10tick净空，避免下一段重叠。 */
    private static void note(CreateSceneBuilder scene, Selection target, Vec3 point, int duration, String text) {
        scene.overlay().showOutlineWithText(target, duration).text(text).attachKeyFrame().pointAt(point).placeNearTarget();
        scene.idle(duration + 20);
    }
}
