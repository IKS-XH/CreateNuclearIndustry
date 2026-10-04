package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.config.TurbineConfig;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.iksxh.create_nuclear_industry.turbine.TurbineAssembly;
import com.iksxh.create_nuclear_industry.turbine.TurbineGeometry;
import com.iksxh.create_nuclear_industry.turbine.TurbinePartBlock;
import com.iksxh.create_nuclear_industry.turbine.TurbineShaftBlock;
import com.iksxh.create_nuclear_industry.turbine.TurbineState;
import com.iksxh.create_nuclear_industry.turbine.TurbineStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 01C 玩家逐件放壳路径：辅助落点、扣料、局部外观与完整运行门。 */
@GameTestHolder("create_nuclear_industry_turbine")
@PrefixGameTestTemplate(false)
public final class ExtensionTurbineAssemblyGameTests {
    private static final BlockPos FRONT = new BlockPos(5, 5, 1);
    private ExtensionTurbineAssemblyGameTests() {}

    /** 用实际放置上下文核对六向端面、前轴外露侧及控制器两层网格的选取/碰撞。 */
    @GameTest(template = "turbine_empty", timeoutTicks = 30)
    public static void ordinaryPortsFaceViewerAndControllerShapeMatchesVisibleThickness(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(5, 5, 5));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + 1.5);
        Direction[] expected = {Direction.NORTH, Direction.EAST, Direction.SOUTH,
                Direction.WEST, Direction.DOWN, Direction.UP};
        float[][] angles = {{0, 0}, {90, 0}, {180, 0}, {270, 0}, {0, -89}, {0, 89}};
        for (int index = 0; index < angles.length; index++) {
            player.setYRot(angles[index][0]);
            player.setYHeadRot(angles[index][0]);
            player.setXRot(angles[index][1]);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            BlockPlaceContext context = new BlockPlaceContext(
                    new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            for (var port : new TurbinePartBlock[]{TurbineContent.INLET.get(),
                    TurbineContent.EXHAUST.get()}) {
                BlockState placed = port.getStateForPlacement(context);
                require(helper, placed != null && placed.getValue(TurbinePartBlock.OUTWARD)
                        == expected[index], "普通汽口外端没有面向玩家的六向视线：" + index
                        + " expected=" + expected[index] + " actual="
                        + (placed == null ? "null" : placed.getValue(TurbinePartBlock.OUTWARD))
                        + " nearest=" + context.getNearestLookingDirection()
                        + " yaw=" + player.getYRot() + " pitch=" + player.getXRot());
            }
            if (index < 4) {
                BlockState shaft = TurbineContent.OUTPUT_SHAFT.get().getStateForPlacement(context);
                require(helper, shaft != null && shaft.getValue(TurbinePartBlock.MACHINE_FACING)
                        == expected[index]
                        && shaft.getValue(TurbineShaftBlock.END) == TurbineShaftBlock.End.FRONT
                        && TurbineContent.OUTPUT_SHAFT.get().hasShaftTowards(helper.getLevel(),
                        pos, shaft, expected[index]), "前端轴普通放置方向与外露 Create 轴不一致");
            }
        }
        BlockState controller = TurbineContent.CONTROLLER.get().defaultBlockState();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockState unlocated = controller.setValue(TurbinePartBlock.MACHINE_FACING, facing);
            var ordinaryShape = unlocated.getShape(helper.getLevel(), pos);
            var ordinary = ordinaryShape.bounds();
            var ordinaryCollision = unlocated.getCollisionShape(helper.getLevel(), pos).bounds();
            require(helper, ordinary.equals(ordinaryCollision), "普通控制器选取与碰撞不一致");
            double thickness = facing.getAxis() == Direction.Axis.Z
                    ? ordinary.maxZ - ordinary.minZ : ordinary.maxX - ordinary.minX;
            require(helper, Math.abs(thickness - .4) < 1e-6,
                    "普通控制器未采用可见模型的 0.4 格厚度");
            require(helper, !containsLocal(ordinaryShape, facing, .05, .05, .2)
                    && containsLocal(ordinaryShape, facing, .5, .5, .2),
                    "普通控制器边缘后方应为空而中心盒体应有碰撞");
            for (TurbinePartBlock.Side side : TurbinePartBlock.Side.values()) {
                BlockState located = unlocated.setValue(TurbinePartBlock.LOCATED, true)
                        .setValue(TurbinePartBlock.SIDE, side);
                var locatedShape = located.getShape(helper.getLevel(), pos);
                var shape = locatedShape.bounds();
                var collision = located.getCollisionShape(helper.getLevel(), pos).bounds();
                require(helper, shape.equals(collision), "定位控制器选取与碰撞不一致");
                double sideThickness = side == TurbinePartBlock.Side.UP
                        || side == TurbinePartBlock.Side.DOWN
                        ? shape.maxY - shape.minY
                        : facing.getAxis() == Direction.Axis.Z
                        ? shape.maxX - shape.minX : shape.maxZ - shape.minZ;
                require(helper, Math.abs(sideThickness - .4) < 1e-6,
                        "定位控制器厚度与 .4 格模型不符");
                double[] edge = switch (side) {
                    case UP -> new double[]{.05, .8, .05};
                    case DOWN -> new double[]{.05, .2, .05};
                    case LEFT -> new double[]{.2, .05, .05};
                    case RIGHT -> new double[]{.8, .05, .05};
                };
                double[] body = switch (side) {
                    case UP -> new double[]{.5, .8, .5};
                    case DOWN -> new double[]{.5, .2, .5};
                    case LEFT -> new double[]{.2, .5, .5};
                    case RIGHT -> new double[]{.8, .5, .5};
                };
                require(helper, !containsLocal(locatedShape, facing, edge[0], edge[1], edge[2])
                        && containsLocal(locatedShape, facing, body[0], body[1], body[2]),
                        "定位控制器边缘后方应为空而中心盒体应有碰撞");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "turbine_empty", timeoutTicks = 40)
    public static void crossingCompleteAxesRejectAssistedPlacementWithoutConsumingItem(GameTestHelper helper) {
        BlockPos center = new BlockPos(5, 5, 5);
        for (int offset = -2; offset <= 2; offset++) {
            helper.setBlock(center.offset(offset, 0, 0), offset == -2 || offset == 2
                    ? TurbineContent.OUTPUT_SHAFT.get().defaultBlockState()
                    : TurbineContent.ROTOR.get().defaultBlockState());
            helper.setBlock(center.offset(0, 0, offset), offset == -2 || offset == 2
                    ? TurbineContent.OUTPUT_SHAFT.get().defaultBlockState()
                    : TurbineContent.ROTOR.get().defaultBlockState());
        }
        helper.runAfterDelay(2, () -> {
            BlockPos absolute = helper.absolutePos(center);
            require(helper, TurbineAssembly.lookup(helper.getLevel(), absolute).kind()
                    == TurbineAssembly.MatchKind.AMBIGUOUS,
                    "交叉双轴应显式标为歧义而非普通未定位");
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(absolute.getX() + .5, absolute.getY() + 1, absolute.getZ() + 1.5);
            ItemStack stack = new ItemStack(TurbineContent.CASING_ITEM.get(), 4);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute),
                    Direction.UP, absolute, false);
            var result = helper.getLevel().getBlockState(absolute).useItemOn(stack,
                    helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
            require(helper, result == ItemInteractionResult.FAIL && stack.getCount() == 4,
                    "歧义轴列未阻断普通落块回退或错误扣料");
            helper.succeed();
        });
    }

    @GameTest(template = "turbine_empty", timeoutTicks = 45)
    public static void controllerShowsLocatedTierAndHighlightsLegalMissingPortSlot(GameTestHelper helper) {
        TurbineState.Tier tier = TurbineConfig.settings().shortTier();
        ExtensionTurbineGameTests.build(helper, FRONT, tier.rotorCount(), false);
        BlockPos inlet = ExtensionTurbineGameTests.inlet(FRONT, tier);
        BlockPos exhaust = FRONT.offset(1, 0, 1);
        BlockPos controller = ExtensionTurbineGameTests.controller(FRONT, tier);
        helper.runAfterDelay(8, () -> {
            helper.setBlock(inlet, TurbineContent.CASING.get().defaultBlockState());
            var owner = (com.iksxh.create_nuclear_industry.turbine.TurbineControllerBlockEntity)
                    helper.getLevel().getBlockEntity(helper.absolutePos(controller));
            TurbineStructure.Issue missingInlet = TurbineStructure.issue(helper.getLevel(),
                    helper.absolutePos(controller), TurbineConfig.settings());
            require(helper, missingInlet != null && missingInlet.reason().equals("inlet")
                    && !missingInlet.pos().equals(helper.absolutePos(controller))
                    && TurbineAssembly.at(helper.getLevel(), missingInlet.pos()) != null,
                    "缺进汽口应标记当前档位可替换的中央侧槽");
            require(helper, owner.diagnostic().getContents() instanceof TranslatableContents text
                    && text.getKey().equals("gui.create_nuclear_industry.turbine.located"),
                    "控制器诊断应先显示已定位尺寸，再显示缺件类别和坐标");
            helper.setBlock(inlet, TurbineContent.INLET.get().defaultBlockState());
            helper.setBlock(exhaust, TurbineContent.CASING.get().defaultBlockState());
            TurbineStructure.Issue missingExhaust = TurbineStructure.issue(helper.getLevel(),
                    helper.absolutePos(controller), TurbineConfig.settings());
            require(helper, missingExhaust != null && missingExhaust.reason().equals("exhaust")
                    && !missingExhaust.pos().equals(helper.absolutePos(controller))
                    && TurbineAssembly.at(helper.getLevel(), missingExhaust.pos()) != null,
                    "缺排汽口应标记当前档位可替换的两端侧槽");
            helper.succeed();
        });
    }

    @GameTest(template = "turbine_empty", timeoutTicks = 30)
    public static void independentPanelUsesClickedWorldFaceAndWrenchRotatesItsCollision(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 5, 5);
        BlockPos absolute = helper.absolutePos(pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(TurbineContent.CASING_ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute),
                Direction.EAST, absolute, false);
        BlockState placed = TurbineContent.CASING.get().getStateForPlacement(
                new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)));
        require(helper, placed != null && placed.getValue(TurbinePartBlock.PIECE)
                == TurbinePartBlock.independentPiece(Direction.EAST), "独立板未记住玩家点击的世界面");
        helper.setBlock(pos, placed);
        var eastShape = helper.getBlockState(pos).getShape(helper.getLevel(), absolute).bounds();
        require(helper, eastShape.minX >= 13D / 16 && eastShape.maxX == 1,
                "独立东面板选取与顶部模型不一致");
        TurbineContent.CASING.get().onWrenched(helper.getBlockState(pos),
                new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        require(helper, TurbinePartBlock.independentFace(helper.getBlockState(pos)
                .getValue(TurbinePartBlock.PIECE)) == Direction.SOUTH,
                "独立板扳手未更新世界朝向");
        var southShape = helper.getBlockState(pos).getShape(helper.getLevel(), absolute).bounds();
        require(helper, southShape.minZ >= 13D / 16 && southShape.maxZ == 1,
                "独立板扳手后模型面与碰撞面不同");
        helper.succeed();
    }

    @GameTest(template = "turbine_empty", timeoutTicks = 100)
    public static void playerWrapsCoreOneCasingAtATimeAndMissingPartCannotRun(GameTestHelper helper) {
        TurbineState.Tier tier = TurbineConfig.settings().shortTier();
        int length = tier.length();
        for (int z = 0; z < length; z++) {
            BlockPos pos = FRONT.offset(0, 0, z);
            BlockState state = z == 0 || z == length - 1
                    ? TurbineContent.OUTPUT_SHAFT.get().defaultBlockState()
                    .setValue(TurbinePartBlock.MACHINE_FACING, Direction.NORTH)
                    .setValue(TurbineShaftBlock.END, z == 0
                            ? TurbineShaftBlock.End.FRONT : TurbineShaftBlock.End.REAR)
                    : TurbineContent.ROTOR.get().defaultBlockState();
            helper.setBlock(pos, state);
        }
        BlockPos controller = FRONT.offset(0, -1, length / 2);
        BlockPos inlet = FRONT.offset(-1, 0, length / 2);
        BlockPos exhaust = FRONT.offset(1, 0, 1);
        helper.setBlock(controller, TurbineContent.CONTROLLER.get().defaultBlockState());
        helper.setBlock(inlet, TurbineContent.INLET.get().defaultBlockState());
        helper.setBlock(exhaust, TurbineContent.EXHAUST.get().defaultBlockState());
        BlockPos blocked = FRONT.offset(1, 1, length - 1);
        helper.setBlock(blocked, Blocks.STONE);

        helper.runAfterDelay(3, () -> {
            BlockPos axis = FRONT.offset(0, 0, 1);
            require(helper, TurbineAssembly.at(helper.getLevel(), helper.absolutePos(axis)) != null,
                    "完整双轴与连续转子未定位小型档位");
            require(helper, helper.getBlockState(axis).getValue(TurbinePartBlock.LOCATED)
                    && !helper.getBlockState(axis).getValue(TurbinePartBlock.FORMED),
                    "缺壳时转子应有正确局部外观且不能运行");
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack stack = new ItemStack(TurbineContent.CASING_ITEM.get(), 64);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            int placed = 0;
            for (int z = 0; z < length; z++) {
                BlockPos anchor = FRONT.offset(0, 0, z);
                BlockPos absolute = helper.absolutePos(anchor);
                player.setPos(absolute.getX() + .5, absolute.getY() + 1,
                        absolute.getZ() + 1.5);
                for (int attempt = 0; attempt < 12; attempt++) {
                    int before = stack.getCount();
                    BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute),
                            Direction.UP, absolute, false);
                    var result = helper.getLevel().getBlockState(absolute).useItemOn(stack,
                            helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
                    if (stack.getCount() == before) break;
                    require(helper, result.consumesAction() && stack.getCount() == before - 1,
                            "辅助放置未做到每块只扣一件");
                    placed++;
                }
            }
            require(helper, placed == 36, "小型机器真实交互补壳数量错误：" + placed);
            require(helper, stack.getCount() == 28
                    && helper.getBlockState(blocked).is(Blocks.STONE),
                    "被阻挡的格子被覆盖或额外扣料");
            BlockPos rear = FRONT.offset(0, 0, length - 1);
            BlockPos absoluteRear = helper.absolutePos(rear);
            player.setPos(absoluteRear.getX() + .5, absoluteRear.getY() + 1,
                    absoluteRear.getZ() + 1.5);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absoluteRear),
                    Direction.UP, absoluteRear, false);
            var rejected = helper.getLevel().getBlockState(absoluteRear).useItemOn(stack,
                    helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
            require(helper, rejected == ItemInteractionResult.FAIL && stack.getCount() == 28,
                    "已定位机组辅助落点受阻时必须拦截普通放置且不扣料");
            require(helper, helper.getLevel().getBlockEntity(helper.absolutePos(controller))
                    instanceof com.iksxh.create_nuclear_industry.turbine.TurbineControllerBlockEntity owner
                    && owner.currentForm() == null, "缺一块时不应授予完整运行 Form");
            helper.setBlock(blocked, Blocks.AIR);
            helper.getLevel().getBlockState(absoluteRear).useItemOn(stack, helper.getLevel(),
                    player, InteractionHand.MAIN_HAND, hit);
            require(helper, stack.getCount() == 27
                    && helper.getBlockState(blocked).is(TurbineContent.CASING.get()),
                    "解除阻挡后未通过物品交互补齐最后一件");
        });
        helper.runAfterDelay(25, () -> {
            var owner = (com.iksxh.create_nuclear_industry.turbine.TurbineControllerBlockEntity)
                    helper.getLevel().getBlockEntity(helper.absolutePos(controller));
            require(helper, owner.currentForm() != null, "真实物品包壳补齐后没有完整成型");
            BlockPos removed = FRONT.offset(-1, 1, 2);
            helper.setBlock(removed, Blocks.AIR);
            require(helper, owner.currentForm() == null, "拆一件未立即撤销运行 Form");
        });
        helper.runAfterDelay(30, () -> {
            BlockPos remains = FRONT.offset(1, 1, 2);
            require(helper, helper.getBlockState(remains).getValue(TurbinePartBlock.LOCATED)
                    && !helper.getBlockState(remains).getValue(TurbinePartBlock.FORMED)
                    && TurbineGeometry.piece(helper.getBlockState(remains)
                    .getValue(TurbinePartBlock.PIECE)) != null,
                    "拆壳后剩余机壳应保留正确piece与局部外观");
            helper.succeed();
        });
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }

    /** 把局部北向的测试点转到世界方向，用于比对二层模型内部的实际占据体积。 */
    private static boolean containsLocal(VoxelShape shape, Direction facing,
                                         double x, double y, double z) {
        Vec3 point = switch (facing) {
            case NORTH -> new Vec3(x, y, z);
            case EAST -> new Vec3(1 - z, y, x);
            case SOUTH -> new Vec3(1 - x, y, 1 - z);
            case WEST -> new Vec3(z, y, 1 - x);
            default -> throw new IllegalArgumentException("仅水平朝向");
        };
        return shape.toAabbs().stream().anyMatch(box -> box.contains(point));
    }
}
