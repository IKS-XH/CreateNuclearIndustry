package com.iksxh.create_nuclear_industry.ponder;

import com.iksxh.create_nuclear_industry.accident.ReactorCoriumBlock;
import com.iksxh.create_nuclear_industry.accident.ReactorCoriumBlockEntity;
import com.iksxh.create_nuclear_industry.content.AccidentContent;
import com.iksxh.create_nuclear_industry.content.SpentFuelStorageContent;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** 事故与回收的客户端用法故事线；只摆静态危险图示和临时载荷，不启动事故或真实伤害。 */
public final class ReactorSafetyPonderScenes {
    private static final BlockPos INSTRUMENT = new BlockPos(6, 3, 5);
    private static final BlockPos DRIVE = new BlockPos(6, 5, 7);
    private static final BlockPos SOLID = new BlockPos(6, 1, 7);
    private static final BlockPos MOLTEN = new BlockPos(6, 3, 7);
    private static final BlockPos RACK = new BlockPos(9, 1, 3);

    private ReactorSafetyPonderScenes() {}

    /** 先讲预防/安全停机，再切入热残骸、持续下烧、凝固冷却和封装回收。 */
    public static void safety(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("experimental_reactor_safety", "反应堆：事故与回收");
        scene.configureBasePlate(0, 0, 13);
        scene.scaleSceneView(0.76F);
        scene.setSceneOffsetY(-0.5F);
        scene.showBasePlate();
        Selection reactor = util.select().fromTo(4, 1, 5, 8, 5, 9);
        scene.world().showSection(reactor, Direction.DOWN);
        scene.idle(20);
        note(scene, util.select().position(INSTRUMENT), util.vector().centerOf(INSTRUMENT), 155,
                "失冷和积热会损伤燃料列与控制棒。用护目镜观察仪表，停堆后继续循环冷却剂，直到余热消退。");
        scene.overlay().showControls(util.vector().topOf(DRIVE), Pointing.DOWN, 70).withItem(new ItemStack(Items.IRON_PICKAXE));
        note(scene, util.select().position(DRIVE), util.vector().topOf(DRIVE), 143,
                "融毁倒计时结束会引发事故，危险时拆开结构也会直接触发事故；先停堆冷却，再检查拆除条件。");
        scene.world().showSection(util.select().position(10, 1, 7), Direction.DOWN);
        scene.idle(20);
        note(scene, util.select().position(10, 1, 7).add(util.select().fromTo(8, 1, 5, 8, 5, 9)), util.vector().centerOf(8, 3, 7), 149,
                "TNT或苦力怕实际炸坏仍危险运行的反应堆成员，也会触发事故；附近爆炸或冷空堆不会一概触发。");
        scene.world().hideSection(reactor.add(util.select().position(10, 1, 7)), Direction.UP);
        scene.idle(20);
        scene.world().setBlock(SOLID, AccidentContent.REACTOR_CORIUM.get().defaultBlockState().setValue(ReactorCoriumBlock.HOT, true), false);
        scene.world().setBlock(MOLTEN, AccidentContent.MOLTEN_CORIUM.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(SOLID).add(util.select().position(MOLTEN)), Direction.DOWN);
        payload(scene, SOLID);
        scene.world().setBlock(new BlockPos(8, 1, 7), AccidentContent.ACCIDENT_FIRE.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(8, 1, 7), Direction.DOWN);
        var moltenPayload = scene.world().createItemEntity(util.vector().topOf(MOLTEN), Vec3.ZERO,
                new ItemStack(AccidentContent.ACCIDENT_FUEL_WASTE.get()));
        scene.world().modifyEntity(moltenPayload, entity -> {
            entity.setNoGravity(true);
            if (entity instanceof net.minecraft.world.entity.item.ItemEntity item) {
                item.noPhysics = true;
                item.setPickUpDelay(Integer.MAX_VALUE);
            }
        });
        scene.idle(20);
        note(scene, util.select().position(SOLID).add(util.select().position(MOLTEN)), util.vector().centerOf(MOLTEN), 149,
                "高温残骸与熔融物接触会热伤并点燃。火抗与辐射防护分别生效；防护服不能代替防烫措施。");
        scene.overlay().showControls(util.vector().topOf(8, 1, 7), Pointing.DOWN, 70).leftClick();
        scene.world().setBlock(new BlockPos(8, 1, 7), Blocks.AIR.defaultBlockState(), false);
        note(scene, util.select().position(SOLID), util.vector().centerOf(SOLID), 110,
                "现场火点可以直接扑灭；灭火不代表残骸已经冷却，也不会消除废料辐射。");
        scene.world().setBlock(MOLTEN, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(MOLTEN.below(), AccidentContent.MOLTEN_CORIUM.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(MOLTEN.below()), Direction.DOWN);
        scene.world().modifyEntity(moltenPayload, entity -> entity.setPos(6.5, 3, 7.5));
        scene.idle(20);
        note(scene, util.select().position(MOLTEN.below()), util.vector().centerOf(MOLTEN.below()), 115,
                "熔融物会继续向下移动和溶蚀，无法用桶或管泵回收；等待它凝固，卸载期间不会继续冷却。");
        scene.world().setBlock(new BlockPos(5, 1, 7), Blocks.WATER.defaultBlockState(), false);
        scene.world().setBlock(new BlockPos(5, 2, 7), Blocks.WATER.defaultBlockState(), false);
        scene.world().showSection(util.select().fromTo(5, 1, 7, 5, 2, 7), Direction.DOWN);
        note(scene, util.select().position(5, 1, 7).add(util.select().position(SOLID)), util.vector().centerOf(SOLID), 120,
                "邻水会加快熔融物凝固和固体冷却。凝固后仍可能高温；冷却只降低热危害，不会消除废料辐射。");
        // 同一熔融位置先凝固为热残骸，并把已展示的那份废料载荷投影到新固体，不凭空删掉一团。
        scene.world().setBlock(MOLTEN.below(), AccidentContent.REACTOR_CORIUM.get().defaultBlockState().setValue(ReactorCoriumBlock.HOT, true), false);
        payload(scene, MOLTEN.below());
        scene.world().modifyEntity(moltenPayload, net.minecraft.world.entity.Entity::discard);
        scene.idle(25);
        scene.world().modifyBlock(MOLTEN.below(), s -> s.setValue(ReactorCoriumBlock.HOT, false), false);
        scene.world().modifyBlock(SOLID, s -> s.setValue(ReactorCoriumBlock.HOT, false), false);
        scene.overlay().showControls(util.vector().topOf(SOLID), Pointing.DOWN, 75).withItem(new ItemStack(Items.IRON_PICKAXE));
        note(scene, util.select().position(SOLID), util.vector().topOf(SOLID), 120,
                "凝固冷却后用普通镐回收残骸里的事故废料；空残骸不产生废料或辐射。");
        scene.world().setBlock(SOLID, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(MOLTEN.below(), Blocks.AIR.defaultBlockState(), false);
        scene.overlay().showControls(util.vector().topOf(SOLID), Pointing.DOWN, 80).withItem(new ItemStack(AccidentContent.ACCIDENT_FUEL_WASTE.get()));
        scene.world().showSection(util.select().position(RACK), Direction.DOWN);
        note(scene, util.select().position(RACK), util.vector().centerOf(RACK), 155,
                "用屏蔽装配台封装事故废料，再把核废料桶存入贮存架；回收或屏蔽辐射源后，现场场强会下降。");
        scene.overlay().showControls(util.vector().topOf(RACK), Pointing.DOWN, 70).withItem(new ItemStack(SpentFuelStorageContent.SEALED_SPENT_FUEL_CASK.get()));
        scene.idle(85);
        scene.markAsFinished();
    }

    /** 仅读入临时残骸的合法单件显示载荷；不调用accept/acceptMolten或创建事故SavedData。 */
    private static void payload(CreateSceneBuilder scene, BlockPos pos) {
        scene.world().modifyBlockEntity(pos, ReactorCoriumBlockEntity.class, corium -> {
            if (corium.getLevel() == null || !corium.getLevel().isClientSide) return;
            CompoundTag tag = new CompoundTag();
            tag.put("Payload", new ItemStack(AccidentContent.ACCIDENT_FUEL_WASTE.get()).save(corium.getLevel().registryAccess()));
            corium.loadWithComponents(tag, corium.getLevel().registryAccess());
        });
    }

    /** 每次只显示一段正文，补偿10tick淡出后另留10tick净空。 */
    private static void note(CreateSceneBuilder scene, Selection selection, Vec3 target, int duration, String text) {
        scene.overlay().showOutlineWithText(selection, duration).text(text).attachKeyFrame().pointAt(target).placeNearTarget();
        scene.idle(duration + 20);
    }
}
