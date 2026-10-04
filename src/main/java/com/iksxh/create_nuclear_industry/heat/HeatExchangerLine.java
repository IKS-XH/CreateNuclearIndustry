package com.iksxh.create_nuclear_industry.heat;

import com.iksxh.create_nuclear_industry.config.HeatExchangerConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * 服务端已加载水平直列的瞬时视图；每机仍拥有自己的库存和 HU，视图不持久化。
 * 任一成员无法确认可 tick、边界区块未知或长度超过服务端配置上限时，整列拒绝交易与供热。
 */
final class HeatExchangerLine implements HeatExchangerState.Exchange {
    private final List<NuclearHeatExchangerBlockEntity> members;
    private final Direction facing;
    private final Object identity;
    private final List<Object> memberIdentities;

    private HeatExchangerLine(List<NuclearHeatExchangerBlockEntity> members, Direction facing) {
        this.members = List.copyOf(members);
        this.facing = facing;
        this.identity = members.getFirst().inventoryIdentity();
        this.memberIdentities = members.stream().map(NuclearHeatExchangerBlockEntity::inventoryIdentity).toList();
    }

    /** 扫描前后各端，先查区块再查方块实体，不触发区块加载。 */
    static HeatExchangerLine find(NuclearHeatExchangerBlockEntity origin) {
        if (!origin.current() || !origin.canTick() || origin.getLevel().isClientSide) return null;
        Level level = origin.getLevel();
        Direction facing = origin.getBlockState().getValue(NuclearHeatExchangerBlock.FACING);
        List<NuclearHeatExchangerBlockEntity> members = new ArrayList<>();
        BlockPos cursor = origin.getBlockPos();
        int maxLength = HeatExchangerConfig.settings().maxLineLength();
        for (int distance = 0; distance <= maxLength; distance++) {
            BlockPos prior = cursor.relative(facing.getOpposite());
            if (!level.hasChunkAt(prior)) return null;
            if (!matches(level, prior, facing)) break;
            cursor = prior;
            if (distance == maxLength) return null;
        }
        for (int distance = 0; distance <= maxLength; distance++) {
            if (!level.hasChunkAt(cursor)) return null;
            if (!matches(level, cursor, facing)) break;
            if (!(level.getBlockEntity(cursor) instanceof NuclearHeatExchangerBlockEntity member)
                    || !member.current() || !member.canTick()) return null;
            members.add(member);
            if (members.size() > maxLength) return null;
            cursor = cursor.relative(facing);
        }
        if (!level.hasChunkAt(cursor) || members.isEmpty()) return null;
        return new HeatExchangerLine(members, facing);
    }

    private static boolean matches(Level level, BlockPos pos, Direction facing) {
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof NuclearHeatExchangerBlock
                && state.getValue(NuclearHeatExchangerBlock.FACING) == facing;
    }

    boolean contains(NuclearHeatExchangerBlockEntity machine) { return members.contains(machine); }
    int count() { return members.size(); }
    int hotCapacity() { return members.size() * members.getFirst().ledger().settings().hotCapacityMb(); }
    int coldCapacity() { return members.size() * members.getFirst().ledger().settings().coldCapacityMb(); }
    boolean inlet(NuclearHeatExchangerBlockEntity machine, Direction side) {
        return side == facing.getOpposite() && members.getFirst() == machine;
    }
    boolean outlet(NuclearHeatExchangerBlockEntity machine, Direction side) {
        return side == facing && members.getLast() == machine;
    }
    Object identity() { return identity; }

    /** 同一拓扑的旧句柄仍要逐成员核对身份，避免拆放后作用于新实体。 */
    boolean sameMembers(HeatExchangerLine other) {
        return other != null && facing == other.facing && identity == other.identity
                && members.equals(other.members) && memberIdentities.equals(other.memberIdentities);
    }

    int totalHot() { return members.stream().mapToInt(m -> m.ledger().hot()).sum(); }
    int totalCold() { return members.stream().mapToInt(m -> m.ledger().cold()).sum(); }
    @Override public int hot() { return totalHot(); }
    @Override public int coldSpace() { return Math.max(0, coldCapacity() - totalCold()); }

    /** 执行期重新读取各台空位；模拟只返回可接收量，不预约真实库存。 */
    int fillHot(int amount, boolean simulate) {
        int accepted = Math.min(Math.max(0, amount), Math.max(0, hotCapacity() - totalHot()));
        if (simulate) return accepted;
        int remaining = accepted;
        for (var member : members) {
            int filled = member.ledger().fillHot(remaining, false);
            remaining -= filled;
            if (remaining == 0) break;
        }
        if (accepted != remaining) notifyInventoryChanged();
        return accepted - remaining;
    }

    int drainCold(int amount, boolean simulate) {
        int taken = Math.min(Math.max(0, amount), totalCold());
        if (simulate) return taken;
        int remaining = taken;
        for (var member : members) {
            int drained = member.ledger().drainCold(remaining, false);
            remaining -= drained;
            if (remaining == 0) break;
        }
        if (taken != remaining) notifyInventoryChanged();
        return taken - remaining;
    }

    /**
     * 先用全列实际热量和冷空位定额，再依序从成员热罐扣除、冷罐加入。
     * 服务端单线程提交期间不调用外部回调；两段账本完成后才通知库存变化。
     */
    @Override public int convert(int amount) {
        int moved = Math.min(Math.max(0, amount), Math.min(totalHot(), coldSpace()));
        int remaining = moved;
        for (var member : members) {
            int drawn = member.ledger().takeHotForConversion(remaining);
            remaining -= drawn;
            if (remaining == 0) break;
        }
        if (remaining != 0) throw new IllegalStateException("换热直列热液预检与提交不一致");
        remaining = moved;
        for (var member : members) {
            int added = member.ledger().putColdFromConversion(remaining);
            remaining -= added;
            if (remaining == 0) break;
        }
        if (remaining != 0) throw new IllegalStateException("换热直列冷液预检与提交不一致");
        if (moved > 0) notifyInventoryChanged();
        return moved;
    }

    private void notifyInventoryChanged() {
        int hot = totalHot();
        int cold = totalCold();
        for (var member : members) {
            member.updateLineView(members.size(), hot, cold);
            member.markInventoryChanged();
        }
    }
}
