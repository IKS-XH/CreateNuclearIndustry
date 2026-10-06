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
        if (!origin.current() || !origin.canTick() || origin.getLevel().isClientSide || origin.inBoiler()) return null;
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
            member.ledger().setSettings(HeatExchangerConfig.settings());
            member.ledger().setCondensationSettings(HeatExchangerConfig.condensationSettings());
            if (members.size() > maxLength) return null;
            cursor = cursor.relative(facing);
        }
        if (!level.hasChunkAt(cursor) || members.isEmpty()) return null;
        if (members.size() > 16 && members.stream().anyMatch(m -> m.ledger().activeMode() == HeatExchangerMode.CONDENSATION))
            return null;
        return new HeatExchangerLine(members, facing);
    }

    private static boolean matches(Level level, BlockPos pos, Direction facing) {
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof NuclearHeatExchangerBlock
                && state.getValue(NuclearHeatExchangerBlock.FACING) == facing
                && (!(level.getBlockEntity(pos) instanceof NuclearHeatExchangerBlockEntity machine) || !machine.inBoiler());
    }

    boolean contains(NuclearHeatExchangerBlockEntity machine) { return members.contains(machine); }
    int count() { return members.size(); }
    /** 非空成员必须同工质，空机保留的历史模式不造成假冲突。 */
    HeatExchangerMode mode() {
        HeatExchangerMode selected = HeatExchangerMode.EMPTY;
        for (var member : members) {
            var active = member.ledger().activeMode();
            if (active == HeatExchangerMode.EMPTY) continue;
            if (selected != HeatExchangerMode.EMPTY && selected != active) return null;
            selected = active;
        }
        return selected;
    }
    boolean conflict() { return mode() == null; }
    HeatExchangerMode displayMode() {
        var active = mode();
        return active == null || active == HeatExchangerMode.EMPTY ? members.getFirst().ledger().mode() : active;
    }
    boolean accepts(HeatExchangerMode candidate) {
        var selected = mode();
        return selected != null && (selected == HeatExchangerMode.EMPTY || selected == candidate)
                && (candidate != HeatExchangerMode.CONDENSATION || members.size() <= 16)
                && members.stream().allMatch(m -> m.ledger().acceptsMode(candidate)
                && (candidate == HeatExchangerMode.CONDENSATION ? m.ledger().condensationSettings().valid() : m.ledger().settings().valid()));
    }
    void adoptMode(HeatExchangerMode candidate) { members.forEach(m -> m.ledger().selectMode(candidate)); }
    int capacity(HeatExchangerMode candidate, boolean input) {
        return members.stream().mapToInt(m -> m.ledger().capacityFor(candidate, input)).sum();
    }
    int hotCapacity() { return capacity(displayMode(), true); }
    int coldCapacity() { return capacity(displayMode(), false); }
    int inputSpace(HeatExchangerMode candidate) {
        return members.stream().mapToInt(m -> Math.max(0, m.ledger().capacityFor(candidate, true) - m.ledger().hot())).sum();
    }
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
    @Override public int coldSpace() {
        // 各机缩容后的超额存量不抵销另一台真实空位；只能加入容量内仍有空位的成员。
        return members.stream().mapToInt(m -> Math.max(0, m.ledger().capacityFor(displayMode(), false) - m.ledger().cold())).sum();
    }

    /** 执行期重新读取各台空位；模拟只返回可接收量，不预约真实库存。 */
    int fillHot(int amount, boolean simulate) {
        return fillInput(amount, HeatExchangerMode.NUCLEAR, simulate);
    }
    int fillInput(int amount, HeatExchangerMode candidate, boolean simulate) {
        if (!accepts(candidate)) return 0;
        int space = inputSpace(candidate);
        int accepted = Math.min(Math.max(0, amount), space);
        if (simulate) return accepted;
        if (accepted == 0) return 0;
        adoptMode(candidate);
        int remaining = accepted;
        for (var member : members) {
            int filled = member.ledger().fillInput(remaining, candidate, false);
            remaining -= filled;
            if (remaining == 0) break;
        }
        if (accepted != remaining) notifyInventoryChanged();
        return accepted - remaining;
    }

    int drainCold(int amount, boolean simulate) {
        if (conflict()) return 0;
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
        if (conflict() || displayMode() != HeatExchangerMode.NUCLEAR) return 0;
        int moved = Math.min(Math.max(0, amount), Math.min(totalHot(), coldSpace()));
        return transfer(moved, moved);
    }

    /** 整列汽→水预检后在服务端无回调提交；输入量和产出量分别记账。 */
    @Override public int condense(int input, int output) {
        if (conflict() || displayMode() != HeatExchangerMode.CONDENSATION
                || input < 0 || output < 0 || input > totalHot() || output > coldSpace()) return 0;
        return transfer(input, output);
    }

    private int transfer(int moved, int output) {
        int remaining = moved;
        for (var member : members) {
            int drawn = member.ledger().takeHotForConversion(remaining);
            remaining -= drawn;
            if (remaining == 0) break;
        }
        if (remaining != 0) throw new IllegalStateException("换热直列热液预检与提交不一致");
        remaining = output;
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
