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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

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

        // 冻结的占位结构不提供完整横向边界，因此显式选择 5×4×5 主体，最后再单独展示顶层。
        scene.world().showSection(util.select().fromTo(0, 0, 0, 4, 3, 4), Direction.DOWN);
        scene.idle(10);
        // 十段说明分别挂接懒关键帧，玩家可在时间轴上手动前后选择教学段落。
        scene.overlay().showText(80)
                .text("A fixed 5x5x5 reactor starts with its casing and valid side interfaces.")
                .attachKeyFrame()
                .pointAt(util.vector().centerOf(2, 0, 2))
                .placeNearTarget();
        scene.idle(90);

        openTeachingCutaway(scene, util);
        scene.idle(10);

        // 顶层八个换料端口分别对应八根燃料列，中心控制棒驱动器不属于换料端口。
        var refuelingPorts = util.select().fromTo(1, 4, 1, 3, 4, 3)
                .substract(util.select().position(CONTROL_ROD));
        scene.world().showSection(refuelingPorts, Direction.DOWN);
        scene.idle(10);
        scene.overlay().showOutlineWithText(refuelingPorts, 165)
                .text("In this temporary teaching cutaway, each refueling port caps three fuel-rod blocks; the control-rod drive caps an air column, with no placeable control-rod block. After inspection, restore the walls and seal the roof with refueling ports, the drive, and casing before formation.")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(100);

        restoreTeachingCutaway(scene);
        scene.idle(15);
        scene.world().showSection(util.select().fromTo(0, 4, 0, 4, 4, 4), Direction.DOWN);
        scene.idle(50);
        scene.overlay().showText(100)
                .text("The single instrument port owns reactor state. A Create wrench requests one server-side formation diagnostic: exactly one instrument port, at least one valid cold port and hot port, plus the casing and column layout; it does not change reactor state.")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(INSTRUMENT_PORT))
                .placeNearTarget();
        scene.overlay().showControls(util.vector().topOf(INSTRUMENT_PORT), Pointing.DOWN, 60)
                .withItem(AllItems.WRENCH.asStack())
                .rightClick();
        scene.idle(120);

        scene.overlay().showText(120)
                .text("With Create engineer goggles, the instrument shows formed size, fuel and control-rod columns, cold and hot port counts, and configured total cold-plus-hot buffer capacity in mB—not current stock. Runtime heat and per-column data come later.")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(INSTRUMENT_PORT))
                .placeNearTarget();
        scene.overlay().showControls(util.vector().topOf(INSTRUMENT_PORT), Pointing.DOWN, 70)
                .withItem(AllItems.GOGGLES.asStack())
                .rightClick();
        scene.idle(140);

        scene.overlay().showOutlineWithText(util.select().position(CONTROL_ROD), 90)
                .text("A newly formed reactor sets every movable control rod's target and actual depth to fully inserted. The drive caps an empty column; no fake control-rod blocks are placed below it.")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(110);

        scene.overlay().showScrollInput(util.vector().topOf(CONTROL_ROD), Direction.UP, 80);
        scene.overlay().showText(120)
                .text("Every player may adjust each drive with its own server-validated Create-style slider. The client previews continuously; the server commits the target, and redstone does not adjust the drive.")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(CONTROL_ROD))
                .placeNearTarget();
        scene.idle(135);

        // 冷热端口位于背面与右侧，先转动镜头确保两个物理端口都能被玩家看见。
        scene.rotateCameraY(90.0F);
        scene.idle(20);
        scene.overlay().showOutlineWithText(
                        util.select().position(COLD_PORT).add(util.select().position(SECOND_COLD_PORT)), 110)
                .text("Multiple cold ports feed one shared cold buffer and coolant ledger. Each physical port has its configured per-tick quota; adding a port increases input throughput without creating another inventory.")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(130);

        scene.overlay().showOutlineWithText(
                        util.select().position(HOT_PORT).add(util.select().position(SECOND_HOT_PORT)), 110)
                .text("Multiple hot ports draw from one shared hot buffer and independently provide output capacity. There is no extra whole-reactor flow cap; conversion still depends on both sides' throughput, stock, capacity, heat, and backpressure.")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(135);

        // SCRAM 仍由正面的仪表端口触发，端口教学结束后把镜头恢复到初始方向。
        scene.rotateCameraY(-90.0F);
        scene.idle(20);
        scene.effects().indicateRedstone(INSTRUMENT_PORT);
        scene.world().toggleRedstonePower(util.select().position(INSTRUMENT_PORT));
        scene.overlay().showText(120)
                .text("Holding a high redstone signal at the instrument port requests SCRAM, saves the earlier targets, and commands every still-movable control rod fully inserted. This request does not promise success for every layout or a jammed rod.")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(INSTRUMENT_PORT))
                .placeNearTarget();
        scene.idle(135);

        scene.world().toggleRedstonePower(util.select().position(INSTRUMENT_PORT));
        scene.overlay().showText(120)
                .text("Removing the signal clears SCRAM and restores the saved pre-SCRAM target depths. Fuel and residual heat are never deleted by the redstone request.")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(INSTRUMENT_PORT))
                .placeNearTarget();
        scene.idle(140);

        scene.markAsFinished();
    }

    /**
     * 使用与服务端结构扫描器相同的标准契约填充虚拟 Ponder 世界。
     * 即使某个 Ponder 资源管理器没有解码结构资源，故事线仍能保持稳定；所有修改都限制在临时世界内。
     * 必须通过 WorldInstructions 写入，确保 Ponder 已建立的世界区段收到重绘通知。
     */
    private static void materializeCanonicalStructure(SceneBuilder scene) {
        // 冻结的占位结构可能小于正式反应堆；仅扩展临时 Ponder 世界边界，允许写入完整 5×5×5 模板。
        scene.addInstruction(ponderScene -> ponderScene.getWorld().setBounds(new BoundingBox(
                0,
                0,
                0,
                ReactorStructureDefinition.SIZE - 1,
                ReactorStructureDefinition.SIZE - 1,
                ReactorStructureDefinition.SIZE - 1
        )));
        ReactorStructureDefinition.canonicalTemplate().forEach((position, blockId) -> {
            if (!ReactorStructureDefinition.AIR_ID.equals(blockId)) {
                scene.world().setBlock(
                        new BlockPos(position.x(), position.y(), position.z()),
                        stateFor(blockId),
                        false
                );
            }
        });
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

    /**
     * 恢复教学切面被移除的两面外墙，并按正式结构模板还原观察窗与仪表端口。
     * 该恢复只作用于 Ponder 临时世界，避免后续红石和端口说明停留在伪造的缺墙结构上。
     */
    private static void restoreTeachingCutaway(SceneBuilder scene) {
        for (int x = 0; x < ReactorStructureDefinition.SIZE; x++) {
            restoreCanonicalBlock(scene, new ReactorStructureDefinition.LocalPosition(x, 1, 0));
            restoreCanonicalBlock(scene, new ReactorStructureDefinition.LocalPosition(x, 2, 0));
            restoreCanonicalBlock(scene, new ReactorStructureDefinition.LocalPosition(x, 3, 0));
        }
        for (int z = 0; z < ReactorStructureDefinition.SIZE; z++) {
            restoreCanonicalBlock(scene, new ReactorStructureDefinition.LocalPosition(4, 1, z));
            restoreCanonicalBlock(scene, new ReactorStructureDefinition.LocalPosition(4, 2, z));
            restoreCanonicalBlock(scene, new ReactorStructureDefinition.LocalPosition(4, 3, z));
        }

        // 标准模板恢复会把教学额外端口覆盖为外壳，因此最后重新放回它们供后续多端口说明使用。
        scene.world().setBlock(SECOND_COLD_PORT, P1Blocks.REACTOR_COLD_PORT.get().defaultBlockState(), false);
        scene.world().setBlock(SECOND_HOT_PORT, P1Blocks.REACTOR_HOT_PORT.get().defaultBlockState(), false);
    }

    /** 按结构定义恢复单个被切面的方块，确保窗口和仪表端口不会被统一外壳覆盖。 */
    private static void restoreCanonicalBlock(SceneBuilder scene,
                                              ReactorStructureDefinition.LocalPosition position) {
        String blockId = ReactorStructureDefinition.canonicalTemplate().get(position);
        if (blockId != null && !ReactorStructureDefinition.AIR_ID.equals(blockId)) {
            scene.world().setBlock(new BlockPos(position.x(), position.y(), position.z()),
                    stateFor(blockId), false);
        }
    }
}
