package com.iksxh.create_nuclear_industry.ponder;

import com.iksxh.create_nuclear_industry.blockentity.ControlRodDriveBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * 管理员两幕只讲相邻接入、燃烧室上的操作和现有运行保护，坐标单位为方块格。
 * 仅更新Ponder临时方块与客户端滑块显示；不打开菜单、发送命令或写入管理员状态。
 */
public final class ReactorAdministratorPonderScenes {
    private static final BlockPos PORT = new BlockPos(5, 2, 4);
    private static final BlockPos BURNER = new BlockPos(5, 2, 3);
    private static final BlockPos INSTRUMENT = new BlockPos(6, 3, 4);
    private static final BlockPos DRIVE = new BlockPos(6, 5, 6);
    private static final BlockPos TORCH = new BlockPos(6, 3, 2);

    private ReactorAdministratorPonderScenes() {}

    /** 从接口任职到燃烧室空手操作；红石反相路径与仪表端口均在正面可见。 */
    public static void setup(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = prepare(builder, util, "reactor_administrator_setup", "反应堆管理员：接入与操作");
        note(scene, util.select().position(PORT), util.vector().centerOf(PORT), 100,
                "在反应堆侧面非棱边的位置安装自动化接口。");
        scene.world().showSection(util.select().position(BURNER), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(BURNER), Pointing.DOWN, 70).rightClick().withItem(new ItemStack(Items.COAL));
        scene.world().setBlock(BURNER, AllBlocks.BLAZE_BURNER.getDefaultState()
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED), false);
        note(scene, util.select().position(BURNER), util.vector().topOf(BURNER), 115,
                "紧邻接口放置烈焰人燃烧室任职；冷启动先给它燃料，并接通反应堆冷却回路。");
        Selection circuit = circuit(util);
        scene.world().showSection(circuit, Direction.DOWN);
        scene.idle(20);
        note(scene, circuit.add(util.select().position(INSTRUMENT)), util.vector().centerOf(TORCH), 126,
                "把燃烧室的运行许可信号经红石反相后接到仪表急停：失去许可时，仪表收到停堆信号。");
        scene.overlay().showControls(util.vector().topOf(BURNER), Pointing.DOWN, 70).rightClick();
        note(scene, util.select().position(BURNER), util.vector().centerOf(BURNER), 100,
                "空手右击任职燃烧室打开三页界面；手持燃料右击仍用于补料。");
        note(scene, util.select().position(BURNER), util.vector().topOf(BURNER), 138,
                "总览页查看模式、目标与实际发热、棒位和冷热库存。模式与数值先编辑，再按应用提交。");
        scene.overlay().showOutline(net.createmod.ponder.api.PonderPalette.GREEN, new Object(), util.select().position(DRIVE), 100);
        note(scene, util.select().position(BURNER), util.vector().centerOf(BURNER), 132,
                "应用不会代替启动。准备就绪后单独按启动；停堆在三页都可用，关界面不会暂停反应堆。");
        // 转动至更高的顶面视角，指向可见的顶帽；不靠穿壳文字假装显示界面网格。
        scene.rotateCameraY(35);
        note(scene, util.select().fromTo(5, 5, 5, 7, 5, 7), util.vector().topOf(DRIVE), 138,
                "堆芯页按真实截面只读查看燃料、完整度、积热和卡死；装换料与钢板维修仍在世界端口操作。");
        note(scene, util.select().position(BURNER), util.vector().centerOf(BURNER), 100,
                "保护页查看联锁原因与复位条件，保护阈值不能在界面关闭；退出会丢弃尚未应用的编辑。");
        scene.markAsFinished();
    }

    /** 两种现行模式、核热值班、缺料与故障保持；运行许可仅用红石画面表达。 */
    public static void operation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = prepare(builder, util, "reactor_administrator_operation", "反应堆管理员：运行与保护");
        scene.world().showSection(util.select().position(BURNER).add(circuit(util)), Direction.DOWN);
        scene.world().setBlock(BURNER, AllBlocks.BLAZE_BURNER.getDefaultState()
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED), false);
        signal(scene, util, true);
        scene.idle(20);
        showDepth(scene, 70);
        scene.overlay().showScrollInput(util.vector().topOf(DRIVE), Direction.UP, 70);
        note(scene, util.select().position(DRIVE).add(util.select().position(BURNER)), util.vector().topOf(DRIVE), 110,
                "统一深度模式把已应用的目标交给全堆可移动控制棒；运行时可在安全校验允许下逐棒精调。");
        showDepth(scene, 45);
        note(scene, util.select().position(DRIVE).add(util.select().position(BURNER)), util.vector().topOf(DRIVE), 149,
                "恒定发热模式按HU/t目标调棒，观察实际与可达发热；该模式和安全停机状态下，逐棒控件只读。");
        signal(scene, util, true);
        scene.world().setBlock(BURNER, AllBlocks.BLAZE_BURNER.getDefaultState()
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED), false);
        note(scene, util.select().position(BURNER), util.vector().topOf(BURNER), 105,
                "反应堆产热可以维持管理员普通燃烧，默认自耗1 HU/t；供能不足仍会失去运行许可。");
        note(scene, util.select().fromTo(5, 5, 5, 7, 5, 7), util.vector().topOf(DRIVE), 126,
                "部分燃料列枯竭或为空时，只要还有可用燃料，就不因这一原因停整堆；可达发热会随之变化。");
        note(scene, util.select().position(INSTRUMENT), util.vector().centerOf(INSTRUMENT), 126,
                "冷却不足或积热过高时，保护优先于模式目标；燃料全部耗尽、缺少控制棒覆盖、卡死、失联和急停也需处理。");
        signal(scene, util, false);
        showDepth(scene, 100);
        scene.effects().indicateRedstone(INSTRUMENT);
        note(scene, util.select().position(BURNER).add(util.select().position(INSTRUMENT)), util.vector().centerOf(BURNER), 143,
                "故障或急停后保持停机。排除原因并满足健康稳定条件，再人工确认复位；不要只撤红石就等待自行提棒。");
        note(scene, util.select().position(INSTRUMENT), util.vector().centerOf(INSTRUMENT), 115,
                "正常保存重进会重新核对安全条件，续接原模式和棒位；人工停堆、故障和急停仍保持停机。");
        scene.markAsFinished();
    }

    /** 地台和机组先完整显露，接口与燃烧室在正面，反相电路由后续步骤独立揭示。 */
    private static CreateSceneBuilder prepare(SceneBuilder builder, SceneBuildingUtil util, String id, String title) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title(id, title);
        scene.configureBasePlate(0, 0, 13);
        scene.scaleSceneView(0.78F);
        scene.setSceneOffsetY(-0.5F);
        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(4, 1, 4, 8, 5, 8), Direction.DOWN);
        scene.idle(20);
        return scene;
    }

    private static Selection circuit(SceneBuildingUtil util) {
        return util.select().fromTo(5, 1, 2, 6, 3, 2).add(util.select().fromTo(6, 2, 3, 6, 3, 3));
    }

    /** 仅更改临时红石方块画面，绝不读取或写入管理员许可账本。 */
    private static void signal(CreateSceneBuilder scene, SceneBuildingUtil util, boolean permitted) {
        scene.world().modifyBlock(new BlockPos(5, 2, 2), s -> s.setValue(BlockStateProperties.POWER, permitted ? 15 : 0), false);
        scene.world().modifyBlock(TORCH, s -> s.setValue(BlockStateProperties.LIT, !permitted), false);
        scene.world().modifyBlock(new BlockPos(6, 3, 3), s -> s.setValue(BlockStateProperties.POWER, permitted ? 0 : 15), false);
    }

    /** 复用原反应堆Ponder的客户端滑块显示入口，不调用服务端调棒方法。 */
    private static void showDepth(CreateSceneBuilder scene, int percent) {
        scene.world().modifyBlockEntity(DRIVE, ControlRodDriveBlockEntity.class, drive -> {
            if (drive.slider() != null) drive.slider().setClientDisplayedValue(percent);
        });
    }

    /** 正文覆盖额外10tick淡出后，再留10tick净空，所有段落严格串行。 */
    private static void note(CreateSceneBuilder scene, Selection selection, Vec3 target, int duration, String text) {
        scene.overlay().showOutlineWithText(selection, duration).text(text).attachKeyFrame().pointAt(target).placeNearTarget();
        scene.idle(duration + 20);
    }
}
