package com.iksxh.create_nuclear_industry.ponder;

import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.content.P1ContentIds;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.simibubi.create.AllItems;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * P1 客户端教学故事线。
 *
 * <p>本类只修改 Ponder 创建的临时世界，用于展示结构、控制棒、冷却端口和 SCRAM 的关系；
 * 它不读取或写入正式服务端快照、玩家库存或真实世界。</p>
 */
public final class P1PonderScenes {
    /** 结构定义中的仪表端口坐标，也是教学中说明权威状态所有权的锚点。 */
    private static final BlockPos INSTRUMENT_PORT = local(ReactorStructureDefinition.DEFAULT_INSTRUMENT_PORT_POSITION);
    /** 结构定义中的标准冷却剂输入端口坐标。 */
    private static final BlockPos COLD_PORT = local(ReactorStructureDefinition.DEFAULT_COLD_PORT_POSITION);
    /** 结构定义中的标准热冷却剂输出端口坐标。 */
    private static final BlockPos HOT_PORT = local(ReactorStructureDefinition.DEFAULT_HOT_PORT_POSITION);
    /** 教学场景中额外放置的控制棒驱动器坐标。 */
    private static final BlockPos CONTROL_ROD = new BlockPos(2, 4, 2);
    /** 用于展示多端口共享账本的第二个冷却剂输入坐标。 */
    private static final BlockPos SECOND_COLD_PORT = new BlockPos(4, 2, 1);
    /** 用于展示多端口独立输出容量的第二个热端口坐标。 */
    private static final BlockPos SECOND_HOT_PORT = new BlockPos(4, 2, 3);

    private P1PonderScenes() {
    }

    /**
     * P1-PONDER-02：展示结构成形、扳手诊断、控制棒默认位置、滑块输入、SCRAM 和多冷却端口。
     * 故事线不接触服务端状态或真实库存。
     */
    public static void experimentalReactorBasics(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("experimental_reactor", "Experimental Reactor: Formation and Safe Control");
        scene.configureBasePlate(0, 0, ReactorStructureDefinition.SIZE);
        scene.scaleSceneView(0.72F);
        scene.showBasePlate();

        materializeCanonicalStructure(scene);

        // 共享模板的中心空列是合法的控制棒列展示位置，不会遮挡燃料列主体。
        scene.world().setBlock(CONTROL_ROD, P1Blocks.CONTROL_ROD_DRIVE.get().defaultBlockState(), false);
        scene.world().setBlock(SECOND_COLD_PORT, P1Blocks.REACTOR_COLD_PORT.get().defaultBlockState(), false);
        scene.world().setBlock(SECOND_HOT_PORT, P1Blocks.REACTOR_HOT_PORT.get().defaultBlockState(), false);

        scene.world().showSection(util.select().layers(0, 3), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("A fixed 5x5x5 reactor starts with its casing and valid side interfaces.")
                .pointAt(util.vector().centerOf(2, 0, 2))
                .placeNearTarget();
        scene.idle(90);

        openTeachingCutaway(scene, util);
        scene.idle(10);
        scene.overlay().showText(90)
                .text("The interior uses three-high fuel columns, control-rod columns, or empty columns.")
                .pointAt(util.vector().centerOf(2, 2, 2))
                .placeNearTarget();
        scene.idle(100);

        scene.world().showSection(util.select().layer(4), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(100)
                .text("The instrument port is the sole reactor state owner and the required wrench diagnosis point.")
                .pointAt(util.vector().topOf(INSTRUMENT_PORT))
                .placeNearTarget();
        scene.overlay().showControls(util.vector().topOf(INSTRUMENT_PORT), Pointing.DOWN, 60)
                .withItem(AllItems.WRENCH.asStack())
                .rightClick();
        scene.idle(90);

        scene.overlay().showOutlineWithText(util.select().position(CONTROL_ROD), 90)
                .text("A newly formed reactor leaves every movable control rod fully inserted.")
                .placeNearTarget();
        scene.idle(100);

        scene.overlay().showScrollInput(util.vector().topOf(CONTROL_ROD), Direction.UP, 80);
        scene.overlay().showText(100)
                .text("Every player may adjust each drive with its own server-validated Create-style slider.")
                .pointAt(util.vector().topOf(CONTROL_ROD))
                .placeNearTarget();
        scene.idle(105);

        scene.overlay().showOutlineWithText(
                        util.select().position(COLD_PORT).add(util.select().position(SECOND_COLD_PORT)), 100)
                .text("Multiple cold ports may feed one shared coolant ledger; they do not create separate reactor state.")
                .placeNearTarget();
        scene.overlay().showOutlineWithText(
                        util.select().position(HOT_PORT).add(util.select().position(SECOND_HOT_PORT)), 100)
                .text("Multiple hot ports independently accept output capacity, with no hidden whole-reactor flow cap.")
                .placeNearTarget();
        scene.idle(110);

        scene.effects().indicateRedstone(INSTRUMENT_PORT);
        scene.world().toggleRedstonePower(util.select().position(INSTRUMENT_PORT));
        scene.overlay().showText(100)
                .text("A sustained high redstone signal at the instrument port requests SCRAM and inserts movable rods.")
                .pointAt(util.vector().topOf(INSTRUMENT_PORT))
                .placeNearTarget();
        scene.idle(110);

        scene.world().toggleRedstonePower(util.select().position(INSTRUMENT_PORT));
        scene.overlay().showText(110)
                .text("Removing the signal restores the pre-SCRAM target depths; SCRAM never deletes fuel or residual heat.")
                .pointAt(util.vector().topOf(INSTRUMENT_PORT))
                .placeNearTarget();
        scene.idle(120);

        scene.markAsFinished();
    }

    /**
     * 使用与服务端结构扫描器相同的标准契约填充虚拟 Ponder 世界。
     * 即使某个 Ponder 资源管理器没有解码结构资源，故事线仍能保持稳定；所有修改都限制在临时世界内。
     */
    private static void materializeCanonicalStructure(SceneBuilder scene) {
        scene.addInstruction(ponderScene -> ReactorStructureDefinition.canonicalTemplate().forEach((position, blockId) -> {
            if (!ReactorStructureDefinition.AIR_ID.equals(blockId)) {
                ponderScene.getWorld().setBlock(
                        new BlockPos(position.x(), position.y(), position.z()),
                        stateFor(blockId),
                        Block.UPDATE_CLIENTS
                );
            }
        }));
    }

    /** 将结构契约中的方块 ID 映射为已注册方块状态，未知 ID 说明故事线与结构契约失配。 */
    private static BlockState stateFor(String blockId) {
        String path = blockId.substring(blockId.indexOf(':') + 1);
        return switch (path) {
            case P1ContentIds.REACTOR_CASING_ID -> P1Blocks.REACTOR_CASING.get().defaultBlockState();
            case P1ContentIds.REACTOR_WINDOW_ID -> P1Blocks.REACTOR_WINDOW.get().defaultBlockState();
            case P1ContentIds.REACTOR_INSTRUMENT_PORT_ID ->
                    P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState();
            case P1ContentIds.REACTOR_COLD_PORT_ID -> P1Blocks.REACTOR_COLD_PORT.get().defaultBlockState();
            case P1ContentIds.REACTOR_HOT_PORT_ID -> P1Blocks.REACTOR_HOT_PORT.get().defaultBlockState();
            case P1ContentIds.REACTOR_REFUELING_PORT_ID ->
                    P1Blocks.REACTOR_REFUELING_PORT.get().defaultBlockState();
            case P1ContentIds.REACTOR_FUEL_ROD_ID -> P1Blocks.REACTOR_FUEL_ROD.get().defaultBlockState();
            case P1ContentIds.CONTROL_ROD_DRIVE_ID -> P1Blocks.CONTROL_ROD_DRIVE.get().defaultBlockState();
            default -> throw new IllegalArgumentException("Unsupported Ponder structure block: " + blockId);
        };
    }

    /** 将结构定义的本地三维坐标转换为 Ponder 使用的方块坐标。 */
    private static BlockPos local(ReactorStructureDefinition.LocalPosition position) {
        return new BlockPos(position.x(), position.y(), position.z());
    }

    /**
     * 正式结构仍是封闭的 5×5×5 外壳；临时 Ponder 剖面移除两块外墙以展示内部列，
     * 并在随后把教学所需端口恢复到临时世界中。该剖面不代表可用于服务端扫描的实际结构。
     */
    private static void openTeachingCutaway(SceneBuilder scene, SceneBuildingUtil util) {
        scene.world().replaceBlocks(
                util.select().fromTo(0, 1, 0, 4, 3, 0),
                Blocks.AIR.defaultBlockState(),
                false
        );
        scene.world().replaceBlocks(
                util.select().fromTo(4, 1, 0, 4, 3, 4),
                Blocks.AIR.defaultBlockState(),
                false
        );

        scene.world().setBlock(INSTRUMENT_PORT,
                P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState(), false);
        scene.world().setBlock(SECOND_COLD_PORT,
                P1Blocks.REACTOR_COLD_PORT.get().defaultBlockState(), false);
        scene.world().setBlock(SECOND_HOT_PORT,
                P1Blocks.REACTOR_HOT_PORT.get().defaultBlockState(), false);
    }
}
