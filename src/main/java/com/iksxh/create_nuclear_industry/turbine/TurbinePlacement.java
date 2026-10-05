package com.iksxh.create_nuclear_industry.turbine;

import com.iksxh.create_nuclear_industry.content.TurbineContent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Create 原生 placement helper：只返回玩家可触及的一个合法空位，实际放置、权限与扣料
 * 交给 PlacementOffset.placeInWorld 的 BlockItem 路径；潜行时回到普通单块放置。
 */
public final class TurbinePlacement implements IPlacementHelper {
    private static final int HELPER_ID = PlacementHelpers.register(new TurbinePlacement());

    private TurbinePlacement() {}

    /** 模组装配时提前注册，使 Create 客户端的世界内预览能发现本 helper。 */
    public static void register() { /* 类初始化已注册 helper。 */ }

    @Override public Predicate<ItemStack> getItemPredicate() {
        return stack -> stack.is(TurbineContent.CASING_ITEM.get())
                || stack.is(TurbineContent.WINDOW_ITEM.get());
    }

    @Override public Predicate<BlockState> getStatePredicate() {
        return state -> state.is(TurbineContent.ROTOR.get())
                || state.is(TurbineContent.CASING.get()) || state.is(TurbineContent.WINDOW.get())
                || state.is(TurbineContent.OUTPUT_SHAFT.get());
    }

    public static ItemInteractionResult useOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (player == null || player.isShiftKeyDown())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        IPlacementHelper helper = PlacementHelpers.get(HELPER_ID);
        if (!helper.matchesItem(stack) || !helper.matchesState(state))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        PlacementOffset offset = helper.getOffset(player, level, state, pos, hit, stack);
        if (!offset.isSuccessful()) {
            // 已定位机组上的失败必须终止本次物品交互，否则原版会在点击面意外落块并扣料。
            TurbineAssembly.Match match = TurbineAssembly.lookup(level, pos);
            if (match.kind() == TurbineAssembly.MatchKind.NONE)
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable(
                        "gui.create_nuclear_industry.turbine.issue."
                                + (match.kind() == TurbineAssembly.MatchKind.AMBIGUOUS
                                ? "unlocated_axis" : "blocked")), true);
            return ItemInteractionResult.FAIL;
        }
        return offset.placeInWorld(level, (BlockItem) stack.getItem(), player, hand, hit);
    }

    @Override public PlacementOffset getOffset(Player player, Level level, BlockState state,
                                               BlockPos pos, BlockHitResult hit) {
        return getOffset(player, level, state, pos, hit,
                player == null ? ItemStack.EMPTY : player.getMainHandItem());
    }

    @Override public PlacementOffset getOffset(Player player, Level level, BlockState state,
                                               BlockPos pos, BlockHitResult hit, ItemStack stack) {
        if (player == null || player.isShiftKeyDown() || !matchesItem(stack))
            return PlacementOffset.fail();
        TurbineAssembly.Layout layout = TurbineAssembly.at(level, pos);
        if (layout == null) return PlacementOffset.fail();
        boolean window = stack.is(TurbineContent.WINDOW_ITEM.get());
        List<BlockPos> possible = new ArrayList<>();
        int z = layout.axial(pos);
        if (z < 0 || z >= layout.length()) return PlacementOffset.fail();
        if (state.is(TurbineContent.CASING.get()) || state.is(TurbineContent.WINDOW.get())) {
            TurbineGeometry.Piece piece = TurbineGeometry.piece(state.getValue(TurbinePartBlock.PIECE));
            if (piece == null || piece.diameter() != layout.diameter()) return PlacementOffset.fail();
            for (int neighborZ : new int[]{z - 1, z + 1})
                addIfShell(level, layout, piece.x(), piece.y(), neighborZ, window, possible);
        } else {
            TurbineGeometry.Section section = z == 0 ? TurbineGeometry.Section.FRONT
                    : z == layout.length() - 1 ? TurbineGeometry.Section.REAR
                    : TurbineGeometry.Section.MIDDLE;
            for (TurbineGeometry.Piece piece : TurbineGeometry.pieces()) {
                if (piece.diameter() != layout.diameter() || piece.section() != section) continue;
                addIfShell(level, layout, piece.x(), piece.y(), z, window, possible);
            }
        }
        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + .5;
        Vec3 eye = player.getEyePosition();
        possible.removeIf(target -> eye.distanceToSqr(Vec3.atCenterOf(target)) > reach * reach);
        possible.sort(Comparator.comparingDouble(target ->
                Vec3.atCenterOf(target).distanceToSqr(hit.getLocation())));
        if (possible.isEmpty()) return PlacementOffset.fail();
        BlockPos target = possible.getFirst();
        int x = layout.cross(target), y = target.getY() - layout.front().getY();
        int targetZ = layout.axial(target);
        TurbineGeometry.Section targetSection = targetZ == 0 ? TurbineGeometry.Section.FRONT
                : targetZ == layout.length() - 1 ? TurbineGeometry.Section.REAR
                : TurbineGeometry.Section.MIDDLE;
        int piece = TurbineGeometry.pieceId(layout.diameter(), targetSection, x, y);
        var transform = (java.util.function.Function<BlockState, BlockState>) initial -> initial
                .setValue(TurbinePartBlock.LOCATED, true)
                .setValue(TurbinePartBlock.MACHINE_FACING, layout.facing())
                .setValue(TurbinePartBlock.PIECE, piece);
        BlockState ghost = transform.apply(window ? TurbineContent.WINDOW.get().defaultBlockState()
                : TurbineContent.CASING.get().defaultBlockState());
        return PlacementOffset.success(target, transform).withGhostState(ghost);
    }

    private static void addIfShell(Level level, TurbineAssembly.Layout layout, int x, int y,
                                   int z, boolean window, List<BlockPos> targets) {
        if (z < 0 || z >= layout.length() || x == 0 && y == 0) return;
        TurbineGeometry.Section section = z == 0 ? TurbineGeometry.Section.FRONT
                : z == layout.length() - 1 ? TurbineGeometry.Section.REAR
                : TurbineGeometry.Section.MIDDLE;
        if (TurbineGeometry.pieceId(layout.diameter(), section, x, y) == 0
                || window && (section != TurbineGeometry.Section.MIDDLE
                || !TurbineGeometry.windowSlot(layout.diameter(), x, y))) return;
        BlockPos target = layout.at(x, y, z);
        if (level.hasChunkAt(target) && level.getBlockState(target).canBeReplaced())
            targets.add(target);
    }
}
