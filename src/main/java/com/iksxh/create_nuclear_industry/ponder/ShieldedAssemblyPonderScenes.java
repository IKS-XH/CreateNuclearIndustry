package com.iksxh.create_nuclear_industry.ponder;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 装配台搭建与接口的客户端用法教学，坐标单位为方块格，转速单位为RPM。
 * 只在Ponder临时世界显露设备与接口，不执行正式生产或服务端物流事务。
 */
public final class ShieldedAssemblyPonderScenes {
    private static final BlockPos MASTER = new BlockPos(3, 2, 3);
    private static final BlockPos RECEIVER = new BlockPos(3, 1, 2);


    private ShieldedAssemblyPonderScenes() {}

    /** 整台八格同时显露；唯一底轴、水平物流和顶部只进料分段说明。 */
    public static void placement(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = setup(builder, "placement", "屏蔽装配台：搭建与接口");
        Selection machine = machine(util);
        scene.overlay().showControls(util.vector().topOf(MASTER), Pointing.DOWN, 35).rightClick()
                .withItem(new ItemStack(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get()));
        scene.world().showSection(machine, Direction.DOWN);
        note(scene, machine, util.vector().centerOf(MASTER), 70, "预留2×2×2空间，一件装配台放出整台设备。");
        scene.world().showSection(util.select().position(MASTER.below()), Direction.DOWN);
        speed(scene, util, 64);
        note(scene, util.select().position(MASTER.below()), util.vector().centerOf(MASTER.below()), 65,
                "只从主控底部动力轴驱动，至少32RPM。");
        showLogistics(scene, util);
        scene.world().showSection(util.select().position(4, 4, 4), Direction.DOWN);
        note(scene, logistics(util).add(util.select().position(4, 4, 4)), util.vector().centerOf(3, 3, 2), 80,
                "四周可输入材料或取出成品；顶部只进料，底面不通物品。");
        scene.idle(15);
        scene.markAsFinished();
    }

    /** 七格地台包住中央机体和物流，设备最低y=1，保留标题及底部控件空间。 */
    private static CreateSceneBuilder setup(SceneBuilder builder, String suffix, String title) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("shielded_assembly_" + suffix, title);
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.85F);
        scene.setSceneOffsetY(-0.5F);
        scene.showBasePlate();
        return scene;
    }

    private static Selection machine(SceneBuildingUtil util) { return util.select().fromTo(3, 2, 3, 4, 3, 4); }
    private static Selection logistics(SceneBuildingUtil util) {
        return util.select().fromTo(3, 2, 2, 3, 3, 2).add(util.select().fromTo(2, 2, 3, 2, 3, 3))
                .add(util.select().position(4, 2, 2)).add(util.select().position(4, 1, 2))
                .add(util.select().position(RECEIVER));
    }
    private static void showLogistics(CreateSceneBuilder scene, SceneBuildingUtil util) {
        scene.world().showSection(logistics(util), Direction.DOWN);
        scene.idle(15);
    }
    /** 临时世界转速只驱动轴动画，不运行正式加工工时。 */
    private static void speed(CreateSceneBuilder scene, SceneBuildingUtil util, float rpm) {
        scene.world().setKineticSpeed(util.select().position(MASTER).add(util.select().position(MASTER.below())), rpm);
    }

    /** Ponder1.0.82正文额外淡出10tick，duration+20使下一段至少再留10tick净空。 */
    private static void note(CreateSceneBuilder scene, Selection target, Vec3 point, int duration, String text) {
        scene.overlay().showOutlineWithText(target, duration).text(text).attachKeyFrame().pointAt(point).placeNearTarget();
        scene.idle(duration + 20);
    }
}
