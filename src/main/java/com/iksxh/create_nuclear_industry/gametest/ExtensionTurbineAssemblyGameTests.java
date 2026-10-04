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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 01C 玩家逐件放壳路径：辅助落点、扣料、局部外观与完整运行门。 */
@GameTestHolder("create_nuclear_industry_turbine")
@PrefixGameTestTemplate(false)
public final class ExtensionTurbineAssemblyGameTests {
    private static final BlockPos FRONT = new BlockPos(5, 5, 1);
    private ExtensionTurbineAssemblyGameTests() {}

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
}
